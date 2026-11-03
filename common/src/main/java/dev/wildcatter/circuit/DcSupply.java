package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A rectifier's DC side (docs/multiblocks/dc-bus.md): an EMF behind its internal resistance, which it moves each solve toward
 * the voltage that drives its setpoint current, as a potline rectifier's tap changer and saturable reactors hold the line current.
 * A diode bridge cannot drive current backward, so it is off while its EMF is below the load's own voltage. The ground-fault
 * detector's two balancing resistors hold the ungrounded system to earth. Its AC side is a constant-power {@link DriveLoad}
 * taking the DC watts of the last solve.
 */
public final class DcSupply {
	private final DriveLoad ac;
	private int plus;
	private int minus;
	private int branch = -1;
	private double emf;
	private double lastEmf = Double.NaN;
	private double lastAmps;
	private double amps;
	private double volts;
	private double plusToEarth;
	private double minusToEarth;
	private double dcWatts;
	private double overSeconds;
	private String trip = "";

	public DcSupply(Voltage acVoltage) {
		this.ac = new DriveLoad(acVoltage, Tuning.RECTIFIER_POWER_FACTOR, 0);
	}

	public DriveLoad ac() {
		return ac;
	}

	public int plus() {
		return plus;
	}

	public int minus() {
		return minus;
	}

	/** Builds the AC front end on {@code acFaces} of {@code pos}, and the DC poles. {@code on}: started and not tripped. */
	public void build(Circuit circuit, BlockPos pos, java.util.List<Direction> acFaces, boolean on) {
		ac.setDemand(on ? dcWatts / Tuning.RECTIFIER_EFFICIENCY : 0);
		ac.build(circuit, pos, acFaces);
		plus = circuit.node();
		minus = circuit.node();
		circuit.resistor(plus, 0, Tuning.DC_DETECTOR_OHMS);
		circuit.resistor(minus, 0, Tuning.DC_DETECTOR_OHMS);
		branch = -1;
		if (on && tripped().isEmpty() && emf > 0) {
			int inside = circuit.node();
			circuit.source(inside, minus, emf);
			branch = circuit.resistor(inside, plus, Tuning.RECTIFIER_DC_OHMS);
		}
	}

	/** Reads the solve and regulates toward {@code setpoint} amps. */
	public void apply(Circuit circuit, int ticks, double setpoint, boolean on) {
		ac.apply(circuit, ticks);
		double seconds = ticks / 20.0;
		plusToEarth = circuit.dcVolts(plus);
		minusToEarth = circuit.dcVolts(minus);
		volts = plusToEarth - minusToEarth;
		amps = branch < 0 ? 0 : circuit.dcAmps(branch);
		dcWatts = Math.max(0, volts * amps);   // what it delivers; its own losses are in its efficiency
		if (!on || !trip.isEmpty() || ac.undervoltage()) {
			emf = 0;
			lastEmf = Double.NaN;
			dcWatts = 0;
			overSeconds = 0;
			return;
		}
		// protection: a bolted short (its terminals collapsed) trips it at once, a lasting overcurrent after a second. A cell closing
		// onto its open-circuit voltage only overshoots until the regulator pulls back, which it does at once, unlimited by the ramp
		if (amps > setpoint * Tuning.RECTIFIER_OVERCURRENT_INSTANT && volts < emf * Tuning.RECTIFIER_SHORT_VOLTS) {
			tripOn(amps);
			return;
		}
		overSeconds = amps > setpoint * Tuning.RECTIFIER_OVERCURRENT ? overSeconds + seconds : 0;
		if (overSeconds >= Tuning.RECTIFIER_OVERCURRENT_SECONDS) {
			tripOn(amps);
			return;
		}
		double step = Tuning.RECTIFIER_RAMP_VOLTS_PER_S * seconds;
		double next;
		if (amps < 1) {
			// not conducting: the load stands at its own voltage, so start from there and climb
			next = Math.max(emf, volts) + step;
		} else {
			double slope = Tuning.RECTIFIER_DC_OHMS * 2;   // until two readings give the load's own slope
			if (!Double.isNaN(lastEmf) && Math.abs(amps - lastAmps) > 1) {
				slope = Math.max(Tuning.RECTIFIER_DC_OHMS, Math.min(1.0, (emf - lastEmf) / (amps - lastAmps)));
			}
			next = emf + Math.min(step, slope * (setpoint - amps));   // up at the tap changer's pace, down at once
		}
		lastEmf = emf;
		lastAmps = amps;
		emf = Math.max(0, Math.min(Tuning.RECTIFIER_MAX_DC_VOLTS, next));
	}

	private void tripOn(double at) {
		trip = String.format("DC overcurrent %.1f kA", at / 1000);
		emf = 0;
		lastEmf = Double.NaN;
		dcWatts = 0;
		overSeconds = 0;
	}

	/** Empty when healthy, else why it tripped. */
	public String tripped() {
		return trip;
	}

	public void reset() {
		trip = "";
	}

	public double amps() {
		return amps;
	}

	public double volts() {
		return volts;
	}

	public double emf() {
		return emf;
	}

	public double plusToEarth() {
		return plusToEarth;
	}

	public double minusToEarth() {
		return minusToEarth;
	}

	/** The ground-fault detector: "" when healthy, "+" or "-" for the pole that has gone to earth. */
	public String groundFault() {
		if (Math.abs(volts) < 1) {
			return "";
		}
		double share = plusToEarth / volts;   // the + pole's share of the line voltage, a half when healthy
		return share < Tuning.DC_DETECTOR_ALARM ? "+" : share > 1 - Tuning.DC_DETECTOR_ALARM ? "-" : "";
	}

	public void save(net.minecraft.world.level.storage.ValueOutput output) {
		output.putDouble("dc_emf", emf);
		output.putDouble("dc_watts", dcWatts);
		output.putString("dc_trip", trip);
	}

	public void load(net.minecraft.world.level.storage.ValueInput input) {
		emf = input.getDoubleOr("dc_emf", 0);
		dcWatts = input.getDoubleOr("dc_watts", 0);
		trip = input.getStringOr("dc_trip", "");
	}
}
