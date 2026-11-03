package dev.wildcatter.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/**
 * The DC tier (docs/multiblocks/dc-bus.md) on a bare circuit: a 4.16 kV source, a rectifier's {@link DcSupply}, and fluorine cells
 * in series on its poles. Each step is one solve 4 ticks apart, as in the game.
 */
class DcTest {
	private static final BlockPos SOURCE = new BlockPos(0, 0, 0);
	private static final BlockPos RECTIFIER = new BlockPos(1, 0, 0);
	private static final double SIX_KA = 6000;

	/** What the circuit holds on the DC side. */
	private enum Load { CELLS, SHORT, OPEN }

	private static DcCell f2(int cells) {
		return new DcCell(cells, Tuning.F2_CELL_BACK_EMF, Tuning.F2_CELL_VOLTS, Tuning.F2_CELL_KA * 1000);
	}

	/** Runs the rectifier {@code seconds} on {@code load}; {@code faultMinus} grounds its - pole. */
	private static void run(DcSupply dc, DcCell cells, Load load, boolean faultMinus, double seconds) {
		ThreePhaseSource source = new ThreePhaseSource();
		for (double t = 0; t < seconds; t += 0.2) {
			Circuit circuit = new Circuit();
			source.build(circuit, SOURCE, Voltage.MEDIUM, Voltage.MEDIUM.nominal(), 0.05);
			dc.build(circuit, RECTIFIER, java.util.List.of(Direction.WEST), true);
			cells.build(circuit, dc.plus(), dc.minus(), load == Load.CELLS);
			switch (load) {
				case CELLS -> { }
				case SHORT -> circuit.resistor(dc.plus(), dc.minus(), 1e-5);
				case OPEN -> { }
			}
			if (faultMinus) {
				circuit.resistor(dc.minus(), 0, Tuning.SHORT_CONTACT_OHMS);
			}
			assertTrue(circuit.solve());
			dc.apply(circuit, 4, SIX_KA, true);
			cells.apply(circuit, dc.plus(), dc.minus());
		}
	}

	@Test
	void itHoldsItsSetpointWhateverTheCellsInSeries() {
		for (int n : new int[] {1, 4, 12}) {
			DcSupply dc = new DcSupply(Voltage.MEDIUM);
			DcCell cells = f2(n);
			run(dc, cells, Load.CELLS, false, 60);
			assertEquals(SIX_KA, cells.amps(), SIX_KA * 0.01, n + " cells: 6 kA");
			assertEquals(n * Tuning.F2_CELL_VOLTS, cells.volts(), 0.05 * n, n + " cells: 9 V each");
			assertEquals(1.0, cells.share(), 0.01);
			assertTrue(dc.tripped().isEmpty(), "no trip while it climbs: " + dc.tripped());
		}
	}

	@Test
	void itStopsAtOneHundredFiftyVolts() {
		DcSupply dc = new DcSupply(Voltage.MEDIUM);
		DcCell cells = f2(20);   // 20 x 9 V = 180 V wanted
		run(dc, cells, Load.CELLS, false, 60);
		assertEquals(Tuning.RECTIFIER_MAX_DC_VOLTS, dc.emf(), 1e-6);
		assertTrue(cells.amps() < SIX_KA * 0.8, "short of its setpoint: " + cells.amps());
	}

	@Test
	void theDetectorSplitsThePolesAndNamesAFault() {
		DcSupply dc = new DcSupply(Voltage.MEDIUM);
		DcCell cells = f2(12);
		run(dc, cells, Load.CELLS, false, 40);
		assertEquals("", dc.groundFault());
		assertEquals(dc.volts() / 2, dc.plusToEarth(), 1.0, "+ at half the line voltage");
		run(dc, cells, Load.CELLS, true, 1);
		assertEquals("-", dc.groundFault());
		assertEquals(dc.volts(), dc.plusToEarth(), 1.0, "+ at the full line voltage with - at earth");
		assertTrue(dc.tripped().isEmpty(), "a ground fault alarms but does not trip");
	}

	@Test
	void openCircuitRisesToItsMaximum() {
		DcSupply dc = new DcSupply(Voltage.MEDIUM);
		run(dc, f2(1), Load.OPEN, false, 20);
		assertEquals(Tuning.RECTIFIER_MAX_DC_VOLTS, dc.volts(), 0.5);
	}

	@Test
	void aCellClosingOntoTheOpenCircuitVoltageDoesNotTripIt() {
		DcSupply dc = new DcSupply(Voltage.MEDIUM);
		DcCell cells = new DcCell(Tuning.CA_CELLS, Tuning.CA_CELL_BACK_EMF, Tuning.CA_CELL_VOLTS, Tuning.CA_CELL_KA * 1000);
		run(dc, cells, Load.OPEN, false, 20);   // idle: up at 150 V
		run(dc, cells, Load.CELLS, false, 20);  // the 18.6 V cells close onto it
		assertTrue(dc.tripped().isEmpty(), "an inrush into cells is not a short: " + dc.tripped());
		assertEquals(SIX_KA, cells.amps(), SIX_KA * 0.01, "pulled back to its setpoint");
	}

	@Test
	void aShortTripsIt() {
		DcSupply dc = new DcSupply(Voltage.MEDIUM);
		DcCell cells = f2(12);
		run(dc, cells, Load.CELLS, false, 40);
		run(dc, cells, Load.SHORT, false, 1);
		assertTrue(dc.tripped().startsWith("DC overcurrent"), "tripped: " + dc.tripped());
		run(dc, cells, Load.CELLS, false, 1);
		assertEquals(0, cells.amps(), 1e-6, "it stays off until reset");
		dc.reset();
		run(dc, cells, Load.CELLS, false, 40);
		assertEquals(SIX_KA, cells.amps(), SIX_KA * 0.01, "back at 6 kA after a reset");
	}
}
