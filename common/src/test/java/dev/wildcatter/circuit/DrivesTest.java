package dev.wildcatter.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import dev.wildcatter.block.Starter;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/**
 * Soft starters and drives (docs/multiblocks/soft-starter.md, vfd.md) on a bare circuit: a 4.16 kV source behind some impedance,
 * a starter of each kind, and the 250 hp charge pump's motor. Each step is one solve 4 ticks apart, as in the game.
 */
class DrivesTest {
	private static final BlockPos SOURCE = new BlockPos(0, 0, 0);
	private static final BlockPos STARTER = new BlockPos(1, 0, 0);
	private static final BlockPos MOTOR = new BlockPos(2, 0, 0);
	private static final double PUMP = Tuning.CHARGE_PUMP_WATTS;

	/** What a run saw. */
	record Run(double peakLineAmps, double seconds, Motor motor, Starter starter, double fla) {
	}

	/** Starts the pump through {@code starter} on a source of {@code sourceOhms} and runs it for {@code seconds}. */
	private static Run run(Starter starter, double sourceOhms, double seconds) {
		Motor motor = new Motor(true, Voltage.MEDIUM, PUMP);
		ThreePhaseSource source = new ThreePhaseSource();
		starter.pushButton();
		double peak = 0;
		double startedAt = -1;
		int[] line = new int[3];
		for (double t = 0; t < seconds; t += 0.2) {
			Circuit circuit = new Circuit();
			source.build(circuit, SOURCE, Voltage.MEDIUM, Voltage.MEDIUM.nominal(), sourceOhms);
			int ground = circuit.node();
			for (int k = 0; k < 3; k++) {
				line[k] = circuit.node();
			}
			starter.build(circuit, line, true);
			Starter.linePort(circuit, STARTER, Direction.WEST, Voltage.MEDIUM, line, ground);
			starter.motorPort(circuit, STARTER, Direction.EAST, ground);
			motor.build(circuit, MOTOR, java.util.List.of(Direction.WEST));
			assertTrue(circuit.solve());
			starter.protect(circuit);
			starter.apply(circuit, 4);
			motor.apply(circuit, 4, true);
			peak = Math.max(peak, starter.lineAmps());
			if (startedAt < 0 && motor.state() == Motor.State.RUNNING) {
				startedAt = t;
			}
		}
		return new Run(peak, startedAt, motor, starter, motor.fullLoadAmps());
	}

	@Test
	void directOnLineDrawsLockedRotorCurrent() {
		Run run = run(new Starter(Starter.Kind.DIRECT, Voltage.MEDIUM, Tuning.CABINET_SETTINGS_MV), 0.01, 5);
		assertTrue(run.peakLineAmps() > 5.5 * run.fla(), "about 6 x full-load amps: " + run.peakLineAmps() + " / " + run.fla());
		assertEquals(Motor.State.RUNNING, run.motor().state());
	}

	@Test
	void softStarterHoldsItsLimitThenBypasses() {
		Starter soft = new Starter(Starter.Kind.SOFT, Voltage.MEDIUM, Tuning.CABINET_SETTINGS_MV);   // 32 A, 3.5 x
		Run run = run(soft, 0.01, 20);
		double limit = soft.limitMultiple() * soft.overloadAmps();
		assertTrue(run.peakLineAmps() < limit * 1.05, "held at the limit: " + run.peakLineAmps() + " / " + limit);
		assertTrue(run.peakLineAmps() > limit * 0.85, "near the limit while ramping: " + run.peakLineAmps() + " / " + limit);
		assertTrue(soft.bypassed(), "bypassed once up to speed: " + soft.state());
		assertEquals(Motor.State.RUNNING, run.motor().state());
		assertTrue(run.seconds() > 4 && run.seconds() < 10, "about 6 s at (3.5/6)^2 torque: " + run.seconds());
		assertTrue(run.motor().output() > 0.95, "full output bypassed: " + run.motor().output());
	}

	@Test
	void driveStartsAtRatedCurrentAndHighPowerFactor() {
		Starter drive = new Starter(Starter.Kind.DRIVE, Voltage.MEDIUM, Tuning.CABINET_SETTINGS_MV);
		Run run = run(drive, 0.01, 15);
		assertTrue(run.peakLineAmps() <= Tuning.DRIVE_CURRENT_LIMIT * run.fla(), "line current within 1.5 x: " + run.peakLineAmps() + " / " + run.fla());
		assertTrue(run.seconds() > 8 && run.seconds() < 12, "runs once the 10 s ramp ends: " + run.seconds());
		assertEquals(Motor.State.RUNNING, run.motor().state());
		assertTrue(run.motor().output() > 0.95, "full output on the drive: " + run.motor().output());
		assertTrue(drive.linePowerFactor() > 0.94, "the front end's power factor: " + drive.linePowerFactor());
		// the line carries the motor's power over the drive's efficiency
		assertEquals(run.motor().watts() / Tuning.DRIVE_EFFICIENCY, drive.lineWatts(), run.motor().watts() * 0.03);
	}

	@Test
	void aDirectStartTakesItsTimeWhateverTheVoltage() {
		Run stiff = run(new Starter(Starter.Kind.DIRECT, Voltage.MEDIUM, Tuning.CABINET_SETTINGS_MV), 0.01, 6);
		Run weak = run(new Starter(Starter.Kind.DIRECT, Voltage.MEDIUM, Tuning.CABINET_SETTINGS_MV), 6.0, 6);
		assertEquals(stiff.seconds(), weak.seconds(), 0.21, "only a soft start goes with V squared (Tim, 2026-11-02)");
	}

	@Test
	void driveLoadTakesConstantPower() {
		double[] amps = new double[2];
		double[] watts = new double[2];
		double[] volts = {1.0, 0.95};
		for (int i = 0; i < 2; i++) {
			DriveLoad load = new DriveLoad(Voltage.INDUSTRIAL, Tuning.LAND_RIG_POWER_FACTOR, 0);
			load.setDemand(300_000);
			ThreePhaseSource source = new ThreePhaseSource();
			for (int step = 0; step < 100; step++) {
				Circuit circuit = new Circuit();
				source.build(circuit, SOURCE, Voltage.INDUSTRIAL, Voltage.INDUSTRIAL.nominal() * volts[i], 0.001);
				load.build(circuit, STARTER, java.util.List.of(Direction.WEST));
				assertTrue(circuit.solve());
				load.apply(circuit, 4);
			}
			amps[i] = load.amps();
			watts[i] = load.watts();
		}
		assertEquals(watts[0], watts[1], watts[0] * 0.02, "the same power at 95 % voltage");
		assertEquals(1 / 0.95, amps[1] / amps[0], 0.02, "more current as the voltage sags");
	}
}
