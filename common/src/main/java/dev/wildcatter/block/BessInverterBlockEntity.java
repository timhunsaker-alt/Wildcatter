package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Generator;
import dev.wildcatter.circuit.Synchronizer;
import dev.wildcatter.circuit.ThreePhaseSource;
import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.control.Remote;
import dev.wildcatter.energy.BessCells;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.solver.Complex;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A battery's power conversion system on the AC grid (docs/multiblocks/grid-batteries.md, 22b and 22c): the utility block's PCS and the
 * commercial cabinet. Its AC side is on the world's industrial LV tier, read from the standard.
 * <ul>
 * <li><b>Grid-following</b> (the default): a signed constant-power star at its terminals, as a drive's front end but either way: the
 * impedance of each phase is {@code V^2 / S*} for the power it is told to take, so discharging is a negative resistance. On a dead bus it
 * builds nothing and reads LOSS OF GRID after 2 s; it comes back 5 minutes after the bus is good. Its power is the setpoint (or the peak
 * shaver's) plus primary frequency response outside the standard's deadband at 4 % droop, ramped over a second.</li>
 * <li><b>Grid-forming</b>: a voltage source behind a sag resistance, as a genset, and a {@link Generator} with synthetic inertia. It
 * synchronises (or closes onto a dead bus, ramping its voltage over 2 s: a black start), limits its current to 1.2 per unit by raising its
 * virtual impedance, and trips UNDERVOLTAGE after 1 s under half voltage.</li>
 * </ul>
 * The cells (containers, or the cabinet's own) are stepped at each solve with what it delivered plus its chillers' draw.
 */
public abstract class BessInverterBlockEntity extends BlockEntity implements CircuitPart, Generator, Inspectable, BigPartEntity, ControlDevice, Measurable {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	/** What the controller's face (or the plant controller) runs it at, in HAND. */
	public enum Preset {
		STOPPED("STOPPED"), IDLE("FOLLOWING, idle: frequency response only"), CHARGE("FOLLOWING, charging at full power"),
		DISCHARGE("FOLLOWING, discharging at full power"), PEAK_SHAVE("FOLLOWING, peak shaving"), FORMING("GRID FORMING");

		final String label;

		Preset(String label) {
			this.label = label;
		}

		public Preset next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	// ---- settings ----------------------------------------------------------------------------------------------------------------
	protected Preset preset = Preset.STOPPED;
	/** The P setpoint, W at the AC terminals, discharge positive (the presets set it; the PLC and the plant controller may). */
	protected double pSet;
	protected double qSet;
	protected boolean frequencyResponse = true;
	/** Peak shaving: hold the island's machines under this, W. */
	protected double peakLimit;
	protected double socMin = Tuning.SOC_MIN;
	protected double socMax = Tuning.SOC_MAX;
	/** A plant controller is running it: its own face's presets and PLC points are ignored until it lets go. */
	protected long plantUntil = -1;
	/** Its share of a plant's peak shaving: each PCS corrects its share of the error. */
	protected double shaveShare = 1.0;

	// ---- state ---------------------------------------------------------------------------------------------------------------------
	protected final Remote remote = new Remote(8);
	protected String tripped = "";
	protected double lossSeconds;
	protected double goodSeconds;
	/** Grid-forming: its breaker. */
	protected boolean closed;
	protected long rampStart = -1;
	protected double uvSeconds;
	protected double zFactor = 1.0;
	protected final Synchronizer sync = new Synchronizer();
	private final ThreePhaseSource phases = new ThreePhaseSource();
	private boolean built;
	private boolean formingBuilt;
	private boolean ptOnly;
	// grid-following's star
	private final int[] hot = new int[3];
	private int star;
	private final int[] branch = {-1, -1, -1};
	// the last solve
	protected double volts;
	protected double amps;
	/** Delivered to the grid at its terminals, W (negative charging). */
	protected double gridWatts;
	protected double gridVars;
	protected double hertz;
	/** Frequency response and peak shaving, ramped. */
	protected double pResponse;
	protected double pShave;
	/** What it was told to deliver at the last build, W. */
	protected double commanded;
	private boolean grown;

	protected BessInverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	// ---- what a subclass says --------------------------------------------------------------------------------------------------------
	/** The PCS's rating, W. */
	public abstract double ratedWatts();

	protected abstract double ratedVa();

	/** Its cells, now. */
	protected abstract List<BessCells> cells(Level level);

	/** Its AC terminals: the cell each port is placed on and the face it looks out of. Empty: it is not formed. */
	protected abstract List<Lines.Face> acPorts(Level level);

	/** Why it cannot run (not formed, a part missing), or null. */
	protected abstract String unformed(Level level);

	protected abstract String partName();

	/** A burning or runaway container: it trips and stays tripped. */
	protected boolean onFire(Level level) {
		return false;
	}

	// ---- settings from outside -----------------------------------------------------------------------------------------------------------
	public Preset preset() {
		return preset;
	}

	public void setPreset(Preset value) {
		preset = value;
		switch (value) {
			case CHARGE -> pSet = -ratedWatts();
			case DISCHARGE -> pSet = ratedWatts();
			case IDLE, PEAK_SHAVE, STOPPED -> pSet = 0;
			case FORMING -> { }
		}
		if (value == Preset.PEAK_SHAVE && peakLimit <= 0) {
			peakLimit = ratedWatts();
		}
		if (value != Preset.FORMING) {
			openBreaker();
		}
		setChanged();
		if (level != null) {
			CircuitManager.markDirty(level);
		}
	}

	public void setSetpoint(double watts) {
		pSet = Math.max(-ratedWatts(), Math.min(ratedWatts(), watts));
		setChanged();
	}

	public void setReactive(double vars) {
		qSet = vars;
	}

	public void setPeakLimit(double watts) {
		peakLimit = Math.max(0, watts);
	}

	public void setFrequencyResponse(boolean on) {
		frequencyResponse = on;
	}

	public void setWindow(double min, double max) {
		socMin = Math.max(0, Math.min(min, max));
		socMax = Math.min(1, Math.max(min, max));
	}

	/** A plant controller runs it for the next second (it keeps calling). */
	public void runFromPlant(long now, Preset value, double setpoint, double limit, boolean response, double share) {
		plantUntil = now + 20;
		shaveShare = share;
		if (value != preset) {
			setPreset(value);
		}
		pSet = Math.max(-ratedWatts(), Math.min(ratedWatts(), setpoint));
		peakLimit = limit;
		frequencyResponse = response;
	}

	public boolean forming() {
		return preset == Preset.FORMING;
	}

	public boolean running() {
		return preset != Preset.STOPPED && tripped.isEmpty();
	}

	public String tripped() {
		return tripped;
	}

	public double gridWatts() {
		return gridWatts;
	}

	public double amps() {
		return amps;
	}

	public double volts() {
		return volts;
	}

	public boolean breakerClosed() {
		return closed;
	}

	/** Clears a trip (a sneak-click, or the PLC's RUN rising). */
	public void reset() {
		tripped = "";
		lossSeconds = 0;
		uvSeconds = 0;
		zFactor = 1.0;
		setChanged();
	}

	protected void trip(String why) {
		if (!tripped.isEmpty()) {
			return;
		}
		tripped = why;
		openBreaker();
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, 0.7F);
			CircuitManager.markDirty(server);
		}
		setChanged();
	}

	private void openBreaker() {
		if (closed) {
			closed = false;
			rampStart = -1;
			sync.reset(ElectricalStandard.active().hertz());
		}
	}

	// ---- the cells -----------------------------------------------------------------------------------------------------------------------
	public double soc(Level level) {
		double e = 0;
		double u = 0;
		for (BessCells c : cells(level)) {
			e += c.energyKwh();
			u += c.usableKwh();
		}
		return u > 0 ? e / u : 0;
	}

	public double soh(Level level) {
		double s = 0;
		List<BessCells> all = cells(level);
		for (BessCells c : all) {
			s += c.soh();
		}
		return all.isEmpty() ? 0 : s / all.size();
	}

	public double energyKwh(Level level) {
		double e = 0;
		for (BessCells c : cells(level)) {
			e += c.energyKwh();
		}
		return e;
	}

	/** The most it may discharge (positive) or charge (as a positive number) at its terminals now, W. */
	public double availableDischarge(Level level) {
		List<BessCells> all = cells(level);
		double sum = 0;
		for (BessCells c : all) {
			sum += c.dischargeLimit(ratedWatts() / all.size(), socMin);
		}
		return all.isEmpty() ? 0 : sum;
	}

	public double availableCharge(Level level) {
		List<BessCells> all = cells(level);
		double sum = 0;
		for (BessCells c : all) {
			sum += c.chargeLimit(ratedWatts() / all.size(), socMax);
		}
		return all.isEmpty() ? 0 : sum;
	}

	protected double chillerWatts(Level level) {
		double w = 0;
		for (BessCells c : cells(level)) {
			w += c.chillerDraw() > 0 ? c.chillerDraw() : c.chillerRated() * Tuning.CHILLER_IDLE_SHARE;
		}
		return w;
	}

	private boolean anyOvertemp(Level level) {
		for (BessCells c : cells(level)) {
			if (c.overtemp()) {
				return true;
			}
		}
		return false;
	}

	// ---- the circuit ---------------------------------------------------------------------------------------------------------------------
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		if (voltage != Voltage.INDUSTRIAL || role == Conductor.G || level == null) {
			return false;
		}
		for (Lines.Face f : acPorts(level)) {
			if (f.cell().equals(cell) && f.side() == face) {
				return true;
			}
		}
		return false;
	}

	/** What it is told to deliver at its terminals now, W, within what its cells allow; its chillers come on top. */
	private double command(Level level) {
		double p = preset == Preset.PEAK_SHAVE ? pShave : pSet;
		if (frequencyResponse) {
			p += pResponse;
		}
		p = Math.max(-availableCharge(level), Math.min(availableDischarge(level), p));
		return p;
	}

	@Override
	public void build(Circuit circuit) {
		sync.beginBuild();
		built = false;
		formingBuilt = false;
		ptOnly = false;
		java.util.Arrays.fill(branch, -1);
		if (level == null) {
			return;
		}
		List<Lines.Face> ports = acPorts(level);
		if (ports.isEmpty()) {
			return;
		}
		built = true;
		Lines.Face first = ports.get(0);
		double nominal = Voltage.INDUSTRIAL.nominal();
		if (forming() && tripped.isEmpty() && unformed(level) == null) {
			if (!closed) {
				sync.buildTransformers(circuit, List.of(first.cell()), List.of(first.side()), Voltage.INDUSTRIAL);
				ptOnly = true;
				return;
			}
			double ratedAmps = ratedVa() / (3 * nominal);
			double ohms = nominal * Tuning.BESS_SAG / ratedAmps * zFactor;
			double ramp = rampStart < 0 ? 1.0 : Math.min(1.0, (level.getGameTime() - rampStart) / (Tuning.BESS_SOFT_START_SECONDS * 20));
			phases.build(circuit, List.of(first.cell()), List.of(first.side()), Voltage.INDUSTRIAL, nominal * Math.max(0.02, ramp), ohms, sync.angle());
			for (int i = 1; i < ports.size(); i++) {
				for (int k = 0; k < 3; k++) {
					circuit.port(ports.get(i).cell(), ports.get(i).side(), PHASES[k], Voltage.INDUSTRIAL, phases.hot(k), Tuning.CONTACT_OHMS / 2, false);
				}
			}
			formingBuilt = true;
			return;
		}
		for (int k = 0; k < 3; k++) {
			hot[k] = circuit.node();
		}
		commanded = 0;
		boolean able = running() && !forming() && unformed(level) == null && volts > Voltage.INDUSTRIAL.lineToLine() * 0.5;
		double chill = chillerWatts(level);
		double p = able ? command(level) : 0;
		commanded = p;
		double consumeP = chill - p;
		double consumeQ = able ? -qSet : 0;
		double s2 = consumeP * consumeP + consumeQ * consumeQ;
		if (s2 > 1 && volts > Voltage.INDUSTRIAL.lineToLine() * 0.5) {
			double v2 = volts * volts;
			double r = v2 * consumeP / s2;
			double x = v2 * consumeQ / s2;
			star = circuit.node();
			for (int k = 0; k < 3; k++) {
				branch[k] = circuit.impedance(hot[k], star, r, x);
			}
		}
		for (Lines.Face f : ports) {
			for (int k = 0; k < 3; k++) {
				circuit.port(f.cell(), f.side(), PHASES[k], Voltage.INDUSTRIAL, hot[k], Tuning.CONTACT_OHMS / 2, false);
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		double seconds = ticks / 20.0;
		hertz = circuit.hertz();
		if (level == null) {
			return;
		}
		if (!built) {
			volts = amps = gridWatts = gridVars = 0;
			stepCells(seconds, 0);
			return;
		}
		if (ptOnly) {
			sync.read(circuit);
			volts = amps = gridWatts = gridVars = 0;
			stepCells(seconds, 0);
			return;
		}
		double ratedAmps = ratedVa() / (3 * Voltage.INDUSTRIAL.nominal());
		if (formingBuilt) {
			volts = phases.volts(circuit) * Math.sqrt(3);
			amps = phases.amps(circuit);
			gridWatts = phases.watts(circuit);
			// the current limit: a virtual impedance raised until it holds 1.2 per unit
			double limit = ratedAmps * Tuning.BESS_CURRENT_LIMIT;
			if (amps > limit * 1.01) {
				zFactor *= amps / limit;
				CircuitManager.markDirty(level);
			} else if (zFactor > 1.0 && amps < limit * 0.8) {
				zFactor = Math.max(1.0, zFactor * 0.7);
			}
			boolean ramping = rampStart >= 0 && level.getGameTime() - rampStart < Tuning.BESS_SOFT_START_SECONDS * 20;
			if (!ramping && volts < Voltage.INDUSTRIAL.lineToLine() * Tuning.BESS_UV_PU) {
				uvSeconds += seconds;
				if (uvSeconds >= Tuning.BESS_UV_SECONDS) {
					trip(String.format("UNDERVOLTAGE: %.0f V for %.1f s at its current limit (%.0f A)", volts, uvSeconds, amps));
				}
			} else {
				uvSeconds = 0;
			}
			stepCells(seconds, gridWatts + chillerWatts(level));
			if (gridWatts > 0 && availableDischarge(level) <= 1 && soc(level) <= socMin + 1e-3) {
				trip("EMPTY: at the bottom of its state-of-charge window");
			}
			return;
		}
		Complex ab = circuit.acrossPhasor(hot[0], hot[1]);
		Complex bc = circuit.acrossPhasor(hot[1], hot[2]);
		Complex ca = circuit.acrossPhasor(hot[2], hot[0]);
		volts = (ab.abs() + bc.abs() + ca.abs()) / 3.0;
		double consumed = 0;
		double consumedQ = 0;
		amps = 0;
		for (int k = 0; k < 3; k++) {
			if (branch[k] >= 0) {
				Complex i = circuit.currentPhasor(branch[k]);
				Complex s = circuit.acrossPhasor(hot[k], star).times(i.conjugate());
				consumed += s.re();
				consumedQ += s.im();
				amps = Math.max(amps, i.abs());
			}
		}
		gridWatts = -consumed;
		gridVars = -consumedQ;
		double chill = chillerWatts(level);
		stepCells(seconds, branch[0] >= 0 ? gridWatts + chill : 0);
		// grid-following: needs a grid
		boolean good = volts >= Voltage.INDUSTRIAL.lineToLine() * 0.9 && hertz >= ElectricalStandard.active().generatorLowHertz()
				&& hertz <= ElectricalStandard.active().generatorHighHertz();
		if (running() && !forming()) {
			if (volts < Voltage.INDUSTRIAL.lineToLine() * 0.5) {
				lossSeconds += seconds;
				if (lossSeconds >= Tuning.BESS_LOSS_OF_GRID_SECONDS) {
					trip("LOSS OF GRID: no voltage at its terminals (it follows a grid; it does not make one)");
					goodSeconds = 0;
				}
			} else {
				lossSeconds = 0;
			}
		}
		if (tripped.startsWith("LOSS OF GRID")) {
			goodSeconds = good ? goodSeconds + seconds : 0;
			if (goodSeconds >= Tuning.BESS_RECLOSE_SECONDS) {
				reset();
				CircuitManager.markDirty(level);
			}
		}
		// frequency response, ramped
		double nominal = ElectricalStandard.active().hertz();
		double df = hertz - nominal;
		double band = ElectricalStandard.active().frequencyResponseDeadbandHz();
		double dfEff = Math.abs(df) <= band ? 0 : df - Math.signum(df) * band;
		double target = -(dfEff / nominal) / Tuning.BESS_DROOP * ratedWatts();
		double step = ratedWatts() / Tuning.BESS_RAMP_SECONDS * seconds;
		double before = pResponse;
		pResponse += Math.max(-step, Math.min(step, target - pResponse));
		// peak shaving: the island's machines held under the limit
		double beforeShave = pShave;
		if (preset == Preset.PEAK_SHAVE && running()) {
			double machines = circuit.machineWatts();
			pShave += Tuning.PEAK_SHAVE_GAIN * shaveShare * (machines - peakLimit);
			pShave = Math.max(-ratedWatts() * 0.5, Math.min(ratedWatts(), pShave));
		} else {
			pShave = 0;
		}
		if (Math.abs(pResponse - before) > ratedWatts() * 0.002 || Math.abs(pShave - beforeShave) > ratedWatts() * 0.002) {
			CircuitManager.markDirty(level);
		}
		if (anyOvertemp(level) && running()) {
			trip("BMS OVERTEMP: cells over " + (int) Tuning.CELL_TRIP_C + " C, contactors open");
		}
	}

	private void stepCells(double seconds, double acWatts) {
		List<BessCells> all = cells(level);
		boolean chiller = built && volts > Voltage.INDUSTRIAL.lineToLine() * 0.5;
		for (BessCells c : all) {
			c.step(acWatts / Math.max(1, all.size()), seconds, chiller);
		}
		if (!all.isEmpty()) {
			cellsChanged();
		}
	}

	/** The cells moved: save them where they live. */
	protected void cellsChanged() {
		setChanged();
	}

	// ---- Generator (grid-forming) --------------------------------------------------------------------------------------------------------
	@Override
	public double inertiaSeconds() {
		return Tuning.BESS_SYNTHETIC_H;
	}

	@Override
	public double deliveredWatts() {
		return gridWatts;
	}

	@Override
	public boolean online() {
		return forming() && closed && tripped.isEmpty();
	}

	@Override
	public boolean storage() {
		return true;
	}

	@Override
	public void tripOnFrequency(double hz) {
		trip(String.format("FREQUENCY: %.2f Hz outside its band", hz));
	}

	// ---- the tick ------------------------------------------------------------------------------------------------------------------------
	public static void tick(Level level, BlockPos pos, BlockState state, BessInverterBlockEntity inv) {
		inv.serverTick(level);
	}

	protected void serverTick(Level level) {
		long now = level.getGameTime();
		if (plantUntil < now) {
			shaveShare = 1.0;
			applyRemote(now);
		}
		if (onFire(level) && !tripped.startsWith("FIRE")) {
			tripped = "";
			trip("FIRE: a container is in thermal runaway or burning");
		}
		if (forming() && tripped.isEmpty() && unformed(level) == null && !closed) {
			String why = sync.step(ElectricalStandard.active().hertz(), Voltage.INDUSTRIAL);
			boolean permitted = why != null && (!remote.auto() || plantUntil >= now || remote.on(BLACK_START, now) || !why.startsWith("onto a dead"));
			if (permitted) {
				boolean dead = why.startsWith("onto a dead");
				if (!dead || availableDischarge(level) > 0) {
					closed = true;
					rampStart = dead ? now : -1;
					sync.closed(dead ? "black start: onto a dead bus, voltage ramped over 2 s" : why);
					if (level instanceof ServerLevel server) {
						server.playSound(null, worldPosition, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, 0.9F);
						if (dead) {
							dev.wildcatter.quest.QuestEvents.milestone(server, worldPosition, dev.wildcatter.quest.Milestones.BESS_BLACK_START);
						}
					}
					CircuitManager.markDirty(level);
					setChanged();
				}
			}
		}
		if (rampStart >= 0 && now - rampStart <= Tuning.BESS_SOFT_START_SECONDS * 20 + 4 && now % 4 == 0) {
			CircuitManager.markDirty(level);
		}
		if (gridWatts >= 1e6 && level instanceof ServerLevel server && now % 20 == 0) {
			dev.wildcatter.quest.QuestEvents.milestone(server, worldPosition, dev.wildcatter.quest.Milestones.BESS_DISCHARGING);
		}
	}

	// ---- PLC points ----------------------------------------------------------------------------------------------------------------------
	static final int RUN = 0, FORMING = 1, BLACK_START = 2, PEAK_SHAVE = 3, FREQ_RESPONSE = 4, P_SET = 5, Q_SET = 6, PEAK_LIMIT = 7;

	@Override
	public String deviceName() {
		return partName();
	}

	/** The points of a PCS, a cabinet and a plant controller. */
	public static final List<Point> POINTS = List.of(
				new Point("RUN", true), new Point("GRID FORMING", true), new Point("BLACK START permit", true), new Point("PEAK SHAVE", true),
				new Point("FREQUENCY RESPONSE", true), new Point("P SETPOINT 4-20 mA (-rated to +rated)", true, Signal.MA),
				new Point("Q SETPOINT 4-20 mA (-rated to +rated)", true, Signal.MA), new Point("PEAK LIMIT 4-20 mA (0 to 4 x rated)", true, Signal.MA),
				new Point("SOC 4-20 mA", false, Signal.MA), new Point("SOH 4-20 mA", false, Signal.MA), new Point("P 4-20 mA", false, Signal.MA),
				new Point("Q 4-20 mA", false, Signal.MA), new Point("AVAILABLE DISCHARGE 4-20 mA", false, Signal.MA),
				new Point("AVAILABLE CHARGE 4-20 mA", false, Signal.MA), new Point("RUNNING", false), new Point("FORMING", false),
				new Point("ON GRID", false), new Point("BMS FAULT", false), new Point("OVERTEMP", false), new Point("LOSS OF GRID", false),
				new Point("UNDERVOLTAGE", false), new Point("END OF LIFE", false), new Point("THERMAL RUNAWAY", false), new Point("AUTO", false));

	@Override
	public List<Point> points() {
		return POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		if (level == null) {
			return false;
		}
		return switch (point) {
			case 14 -> running();
			case 15 -> forming() && closed;
			case 16 -> volts > Voltage.INDUSTRIAL.lineToLine() * 0.9;
			case 17 -> !tripped.isEmpty();
			case 18 -> anyOvertemp(level) || tripped.startsWith("BMS OVERTEMP");
			case 19 -> tripped.startsWith("LOSS OF GRID");
			case 20 -> tripped.startsWith("UNDERVOLTAGE");
			case 21 -> soh(level) < Tuning.SOH_END_OF_LIFE;
			case 22 -> onFire(level);
			case 23 -> remote.auto();
			default -> false;
		};
	}

	@Override
	public double readMa(int point) {
		if (level == null) {
			return 4;
		}
		double r = ratedWatts();
		return switch (point) {
			case 8 -> 4 + 16 * soc(level);
			case 9 -> 4 + 16 * soh(level);
			case 10 -> 12 + 8 * Math.max(-1, Math.min(1, gridWatts / r));
			case 11 -> 12 + 8 * Math.max(-1, Math.min(1, gridVars / r));
			case 12 -> 4 + 16 * Math.min(1, availableDischarge(level) / r);
			case 13 -> 4 + 16 * Math.min(1, availableCharge(level) / r);
			default -> 4;
		};
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		boolean rose = remote.command(point, on, gameTime);
		if (point == RUN && rose && !tripped.isEmpty() && !onFire(level)) {
			reset();
		}
	}

	@Override
	public void commandMa(int point, double milliamps, long gameTime) {
		remote.commandMa(point, milliamps, gameTime);
	}

	/** In AUTO, the PLC's points run it. */
	private void applyRemote(long now) {
		if (!remote.auto()) {
			return;
		}
		Preset want;
		if (!remote.on(RUN, now)) {
			want = Preset.STOPPED;
		} else if (remote.on(FORMING, now)) {
			want = Preset.FORMING;
		} else if (remote.on(PEAK_SHAVE, now)) {
			want = Preset.PEAK_SHAVE;
		} else {
			want = Preset.IDLE;
		}
		if (want != preset) {
			setPreset(want);
		}
		if (want == Preset.IDLE) {
			pSet = (remote.fraction(P_SET, now, 0.5) - 0.5) * 2 * ratedWatts();
		}
		qSet = (remote.fraction(Q_SET, now, 0.5) - 0.5) * 2 * ratedWatts();
		peakLimit = remote.fraction(PEAK_LIMIT, now, peakLimit / (4 * ratedWatts())) * 4 * ratedWatts();
		frequencyResponse = remote.on(FREQ_RESPONSE, now) || !remote.fresh(FREQ_RESPONSE, now);
	}

	@Override
	public java.util.Map<BlockPos, String> hoaPositions() {
		return java.util.Map.of(worldPosition, remote.hoa().name());
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	@Override
	public double measure(Quantity quantity) {
		return switch (quantity) {
			case WATTS -> gridWatts;
			case VOLTS -> volts;
			case AMPS -> amps;
			case HERTZ -> hertz;
			case LEVEL -> level == null ? Double.NaN : soc(level) * 100;
			default -> Double.NaN;
		};
	}

	// ---- readout -------------------------------------------------------------------------------------------------------------------------
	/** The sneak-click on its face: the next preset (in HAND), or a reset when tripped. */
	public Component sneakClick() {
		if (!tripped.isEmpty() && !(level != null && onFire(level))) {
			reset();
			return Component.literal(partName() + ": reset");
		}
		if (plantUntil >= (level == null ? 0 : level.getGameTime())) {
			return Component.literal(partName() + ": run by its plant controller");
		}
		if (remote.auto()) {
			return Component.literal(partName() + ": in AUTO, run by its PLC");
		}
		setPreset(preset.next());
		return Component.literal(partName() + ": " + preset.label);
	}

	protected String state() {
		if (level == null) {
			return "";
		}
		String why = unformed(level);
		if (why != null) {
			return why;
		}
		String mode = preset.label + (remote.auto() ? " (AUTO)" : plantUntil >= level.getGameTime() ? " (plant controller)" : "");
		String run;
		if (!tripped.isEmpty()) {
			run = "TRIPPED " + tripped + " (sneak-click to reset)";
		} else if (forming() && !closed) {
			run = sync.busRead() && sync.busLive(Voltage.INDUSTRIAL) ? "synchronising: " + sync.scope() : "waiting to close";
		} else if (preset == Preset.STOPPED) {
			run = "stopped";
		} else {
			run = String.format("%s %.0f kW, %.0f kvar", gridWatts >= 0 ? "delivering" : "charging", Math.abs(gridWatts) / 1000, gridVars / 1000);
		}
		return String.format("%s: %s; %s; %.0f V, %.0f A, %.2f Hz; %s", mode, run, describeCells(), volts, amps, hertz,
				forming() ? String.format("H %.0f s, current limit %.1f pu%s", Tuning.BESS_SYNTHETIC_H, Tuning.BESS_CURRENT_LIMIT, zFactor > 1.01 ? " (LIMITING)" : "")
						: String.format("droop %.0f %%, deadband %.0f mHz%s", Tuning.BESS_DROOP * 100, ElectricalStandard.active().frequencyResponseDeadbandHz() * 1000,
								preset == Preset.PEAK_SHAVE ? String.format(", holding machines under %.0f kW", peakLimit / 1000) : ""));
	}

	protected String describeCells() {
		List<BessCells> all = cells(level);
		if (all.isEmpty()) {
			return "no cells";
		}
		return String.format("SOC %.1f %% (window %.0f-%.0f %%), %,.0f of %,.0f kWh, SOH %.2f %%", soc(level) * 100, socMin * 100, socMax * 100,
				energyKwh(level), all.stream().mapToDouble(BessCells::usableKwh).sum(), soh(level) * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("%s (%.0f kW, %.0f kVA, %s): ", partName(), ratedWatts() / 1000, ratedVa() / 1000, Voltage.INDUSTRIAL.label()) + state());
	}

	// ---- save ----------------------------------------------------------------------------------------------------------------------------
	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		remote.save(output);
		sync.save(output);
		output.putBoolean("grown", grown);
		output.putString("preset", preset.name());
		output.putDouble("pSet", pSet);
		output.putDouble("qSet", qSet);
		output.putDouble("peakLimit", peakLimit);
		output.putBoolean("freqResponse", frequencyResponse);
		output.putDouble("socMin", socMin);
		output.putDouble("socMax", socMax);
		output.putString("tripped", tripped);
		output.putBoolean("closed", closed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		remote.load(input);
		sync.load(input);
		grown = input.getBooleanOr("grown", grown);
		try {
			preset = Preset.valueOf(input.getStringOr("preset", Preset.STOPPED.name()));
		} catch (IllegalArgumentException e) {
			preset = Preset.STOPPED;
		}
		pSet = input.getDoubleOr("pSet", 0);
		qSet = input.getDoubleOr("qSet", 0);
		peakLimit = input.getDoubleOr("peakLimit", 0);
		frequencyResponse = input.getBooleanOr("freqResponse", true);
		socMin = input.getDoubleOr("socMin", Tuning.SOC_MIN);
		socMax = input.getDoubleOr("socMax", Tuning.SOC_MAX);
		tripped = input.getStringOr("tripped", "");
		closed = input.getBooleanOr("closed", false);
	}

	/** Every port cell and face of a list, for a subclass. */
	protected static List<Lines.Face> outside(List<dev.wildcatter.block.TransformerBlock.Terminal> terminals) {
		List<Lines.Face> out = new ArrayList<>();
		for (var t : terminals) {
			out.add(new Lines.Face(t.cell().relative(t.face()), t.face().getOpposite()));
		}
		return out;
	}
}
