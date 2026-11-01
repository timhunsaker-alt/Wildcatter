package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Generator;
import dev.wildcatter.circuit.StatorHeating;
import dev.wildcatter.circuit.Synchronizer;
import dev.wildcatter.circuit.ThreePhaseSource;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A 13.8 kV generator at true size (docs/multiblocks/gas-turbine.md, combined-cycle.md): the gas turbine's 20.6 MVA unit or the
 * steam turbine's 6.25 MVA one. Its prime mover drives it each tick ({@link #drive}); at speed it synchronizes and closes its
 * generator breaker (the {@code gen_breaker} against its terminal end), onto a dead bus at once or onto a live one in step, and it
 * trips on stator overcurrent or frequency. Its terminals are the south face of its back bottom cells.
 */
public class GeneratorPartBlockEntity extends BlockEntity implements CircuitPart, Generator, Inspectable, BigPartEntity {
	private final ThreePhaseSource phases = new ThreePhaseSource();
	private final StatorHeating stator = new StatorHeating();
	private final Synchronizer sync = new Synchronizer();
	private boolean grown;
	private boolean ready;
	private double rated;
	private long driven = -1000;
	private boolean breakerClosed;
	private boolean tripped;
	private String tripReason = "";
	private double volts;
	private double amps;
	private double outWatts;
	private double hertz = ElectricalStandard.active().hertz();

	public GeneratorPartBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GENERATOR_PART_ENTITY, pos, state);
	}

	private boolean steam() {
		return getBlockState().getBlock() instanceof BigPart part && part.shapeName().equals("st_generator");
	}

	private String kind() {
		return getBlockState().getBlock() instanceof BigPart part ? part.shapeName() : "";
	}

	/** Its nameplate: 16.5 MW (20.6 MVA), 5 MW (6.25 MVA) or 45 MW (56.25 MVA). */
	public double nameplateWatts() {
		return switch (kind()) {
			case "st_generator" -> Tuning.ST_WATTS;
			case "aero_generator" -> Tuning.AERO_WATTS;
			default -> Tuning.GT_WATTS;
		};
	}

	/** The prime mover, each tick: whether it is at speed, and what it can deliver now. */
	public void drive(Level level, boolean atSpeed, double canDeliver) {
		driven = level.getGameTime();
		if (atSpeed && !ready) {
			sync.reset(ElectricalStandard.active().hertz());   // just up to speed: start synchronizing
		}
		ready = atSpeed;
		rated = Math.max(0, Math.min(nameplateWatts(), canDeliver));
	}

	public boolean live() {
		return ready && breakerClosed && !tripped && rated > 1000;
	}

	public boolean breakerClosed() {
		return breakerClosed && live();
	}

	public boolean tripped() {
		return tripped;
	}

	public String tripReason() {
		return tripReason;
	}

	public double hertz() {
		return hertz;
	}

	/** Reset after a trip: the prime mover's button. */
	public void reset() {
		tripped = false;
		tripReason = "";
		stator.reset();
		setChanged();
	}

	public void trip(String why) {
		tripped = true;
		tripReason = why;
		breakerClosed = false;
		if (level instanceof ServerLevel server) {
			server.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.2F, 0.7F);
			CircuitManager.markDirty(server);
		}
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, GeneratorPartBlockEntity g) {
		boolean was = g.live();
		if (level.getGameTime() - g.driven > 10) {
			g.ready = false;
		}
		if (!g.ready || g.tripped) {
			g.breakerClosed = false;
		} else if (!g.breakerClosed) {
			String why = g.sync.step(ElectricalStandard.active().hertz(), Voltage.GENERATOR);
			if (why != null) {
				g.breakerClosed = true;
				g.sync.closed(why);
				level.playSound(null, pos, WildcatterSounds.HV_BREAKER, SoundSource.BLOCKS, 1.0F, 1.1F);
				CircuitManager.markDirty(level);
			}
		}
		if (g.live() != was) {
			CircuitManager.markDirty(level);
		}
		g.setChanged();
	}

	// ---- the circuit -----------------------------------------------------------------------------------------------------------
	private int length() {
		return switch (kind()) {
			case "st_generator" -> 4;
			case "aero_generator" -> 7;
			default -> 6;
		};
	}

	List<Lines.Face> terminals() {
		return LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[2] == length() - 1 && at[1] == 0, Direction.SOUTH);
	}

	private List<BlockPos> terminalCells() {
		return terminals().stream().map(Lines.Face::cell).toList();
	}

	private Direction terminalFace() {
		return LeasePartBlock.world(getBlockState(), Direction.SOUTH);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.GENERATOR && face == terminalFace() && terminalCells().contains(cell);
	}

	@Override
	public void build(Circuit circuit) {
		sync.beginBuild();
		if (!live()) {
			if (ready && !breakerClosed && !tripped) {
				sync.buildTransformers(circuit, terminalCells(), List.of(terminalFace()), Voltage.GENERATOR);
			}
			return;
		}
		double nominal = Voltage.GENERATOR.nominal();
		double ratedAmps = rated / Tuning.GENERATOR_RATED_PF / (3 * nominal);
		phases.build(circuit, terminalCells(), List.of(terminalFace()), Voltage.GENERATOR, nominal, nominal * Tuning.ENGINE_SAG / ratedAmps);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (!live()) {
			volts = amps = outWatts = 0;
			sync.read(circuit);
			return;
		}
		volts = phases.volts(circuit);
		amps = phases.amps(circuit);
		outWatts = phases.watts(circuit);
		hertz = circuit.hertz();
		if (stator.step(phases.phaseAmps(circuit), StatorHeating.ratedPhaseAmps(nameplateWatts(), Voltage.GENERATOR.nominal()), ticks / 20.0)) {
			trip(String.format("on stator overcurrent (phase %s at %.1fx its rating)", "ABC".charAt(stator.hottestPhase()), stator.worstRatio()));
		}
	}

	// ---- Generator ---------------------------------------------------------------------------------------------------------------
	@Override
	public double ratedWatts() {
		return rated;
	}

	@Override
	public double inertiaSeconds() {
		return Tuning.INERTIA_STEAM;   // H = 3 s, for the gas turbine and the steam turbine alike
	}

	@Override
	public double deliveredWatts() {
		return outWatts;
	}

	@Override
	public boolean online() {
		return live();
	}

	@Override
	public void tripOnFrequency(double frequency) {
		trip(String.format("on frequency (%.1f Hz)", frequency));
	}

	/** One line for the prime mover's readout. */
	public String status() {
		if (tripped) {
			return "generator TRIPPED " + tripReason;
		}
		if (!ready) {
			return "generator breaker open";
		}
		if (!breakerClosed) {
			return "synchronizing" + (sync.busLive(Voltage.GENERATOR) ? " (" + sync.scope() + ")" : "");
		}
		return String.format("breaker CLOSED, %.1f Hz, %.0f V, %.2f MW", hertz, volts * Math.sqrt(3), outWatts / 1e6);
	}

	@Override
	public Component inspect() {
		String name = switch (kind()) {
			case "st_generator" -> "Steam turbine generator (6.25 MVA, ";
			case "aero_generator" -> "Aeroderivative generator (56.25 MVA, ";
			default -> "Gas turbine generator (20.6 MVA, ";
		};
		return Component.literal(name + Voltage.GENERATOR.label() + "): " + status());
	}

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
		output.putBoolean("grown", grown);
		output.putBoolean("tripped", tripped);
		output.putString("tripReason", tripReason);
		sync.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", grown);
		tripped = input.getBooleanOr("tripped", false);
		tripReason = input.getStringOr("tripReason", "");
		sync.load(input);
	}
}
