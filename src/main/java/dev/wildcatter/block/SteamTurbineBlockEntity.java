package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The small 120 V turbine-generator of the starter plant (a campfire boiler and this): its output sags as the current rises (it
 * has some resistance of its own), and it only draws as much steam from the boiler as the circuit is actually using. If the boiler
 * cannot keep up, the voltage falls: a brownout, not a blackout. Big power comes from the 1 MW steam plant (TurbineGeneratorBlockEntity).
 */
public class SteamTurbineBlockEntity extends BlockEntity implements CircuitPart, dev.wildcatter.circuit.Generator, Inspectable, Bondable, dev.wildcatter.kinetic.Sounding {
	private final dev.wildcatter.circuit.ThreePhaseSource phases = new dev.wildcatter.circuit.ThreePhaseSource();
	private double volts;
	private double amps;
	private double outWatts;
	/** What share of the demanded steam the boiler is managing to supply (1 = all of it). */
	private double satisfaction = 1.0;
	/** Ticks since it last had a load; the rotor keeps spinning for a second so the block doesn't flicker. */
	private int idleTicks = 100;
	/** Water condensed from the steam used, waiting to go out through the drain. */
	private double drainWater;
	private boolean drained;
	/** Frequency protection shut this generator down. */
	private boolean tripped;
	private double tripHertz;
	/** The frequency of its island, as of the last solve. */
	private double hertz = dev.wildcatter.energy.ElectricalStandard.active().hertz();
	/** A cooling tower on the drain's pipes is running. Without one the turbine needs more steam per joule. */
	private boolean cooled;

	public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STEAM_TURBINE_ENTITY, pos, state);
	}

	public double watts() {
		return Math.max(0, outWatts);
	}

	/** True if there is voltage on its terminals to bite with. */
	public boolean isLive() {
		return volts > Tuning.SHOCK_MIN_VOLTS;
	}

	public Voltage voltage() {
		return Voltage.LOW;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == voltage();
	}

	@Override
	public void build(Circuit circuit) {
		// three phases, each able to carry the full rating on its own (a small single-phase set); the governor and frequency limit the total
		phases.build(circuit, worldPosition, voltage(), tripped ? 0 : voltage().nominal() * satisfaction, Tuning.TURBINE_INTERNAL_OHMS);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = phases.volts(circuit);
		amps = phases.amps(circuit);
		outWatts = phases.watts(circuit);
		hertz = circuit.hertz();
	}

	/** Every tick: take the steam the circuit is using this tick from the boilers touching it. */
	public static void tick(Level level, BlockPos pos, BlockState state, SteamTurbineBlockEntity turbine) {
		long phase = level.getGameTime() + pos.hashCode();

		double watts = turbine.watts();
		double joules = watts / 20.0;
		double perSteam = Tuning.JOULES_PER_STEAM * (turbine.cooled ? 1.0 : Tuning.UNCOOLED_EFFICIENCY);
		int wanted = (int) Math.ceil(joules / perSteam);
		double share = 1.0;
		if (wanted > 0) {
			int got = turbine.draw(level, wanted);
			turbine.drainWater = Math.min(Tuning.DRAIN_TANK, turbine.drainWater + got * Tuning.CONDENSATE_RETURN / Tuning.STEAM_PER_WATER);
			share = Math.min(1.0, got / (joules / perSteam));
			turbine.idleTicks = 0;
		} else {
			turbine.idleTicks = Math.min(turbine.idleTicks + 1, 100);
		}
		turbine.satisfaction += (share - turbine.satisfaction) * Tuning.TURBINE_STEAM_SMOOTHING;

		if (phase % Tuning.PLUMBING_INTERVAL == 0) {
			turbine.drain(level);
		}

		boolean running = turbine.idleTicks < 20;
		if (state.getValue(SteamTurbineBlock.RUNNING) != running) {
			level.setBlock(pos, state.setValue(SteamTurbineBlock.RUNNING, running), 3);
		}
	}

	/**
	 * The drain: a pipe touching the turbine carries the condensed steam back
	 * to the boilers on the same run of pipe. With no pipe the water just spills away, so the boiler slowly dries out.
	 */
	private void drain(Level level) {
		List<BlockPos> cells = List.of(worldPosition);
		Set<BlockPos> pipes = Plumbing.network(level, cells);
		drained = !pipes.isEmpty();
		cooled = false;
		for (TowerBasinBlockEntity basin : Plumbing.machines(level, cells, pipes, false, TowerBasinBlockEntity.class)) {
			cooled |= basin.cooling();
		}
		if (drainWater >= 1) {
			if (drained) {
				drainWater -= Plumbing.deliver(Plumbing.boilers(level, cells, pipes, false), (int) drainWater);
			} else {
				drainWater = 0;   // it just spills
			}
		}
	}

	/** Takes steam from every boiler touching this turbine. */
	private int draw(Level level, int wanted) {
		int got = 0;
		for (Direction side : Direction.values()) {
			if (got < wanted && level.getBlockEntity(worldPosition.relative(side)) instanceof BoilerBlockEntity boiler) {
				got += boiler.takeSteam(wanted - got);
			}
		}
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(inspectText().getString() + frequencyText());
	}


	// ---- Generator: the island's frequency, and frequency protection ----------------------------------------------------
	@Override
	public double ratedWatts() {
		return Tuning.BOILER_STEAM_PER_TICK * Tuning.JOULES_PER_STEAM * 20.0;
	}

	@Override
	public double inertiaSeconds() {
		return Tuning.INERTIA_STEAM;
	}

	@Override
	public double deliveredWatts() {
		return outWatts;   // signed: a machine being driven by the others counts against them
	}

	@Override
	public boolean online() {
		return !tripped;
	}

	@Override
	public void tripOnFrequency(double frequency) {
		tripped = true;
		tripHertz = frequency;
		setChanged();
	}

	public boolean isTripped() {
		return tripped;
	}

	/** Right-click after a trip: it starts up again. */
	public void resetTrip() {
		tripped = false;
		setChanged();
		CircuitManager.markDirty(level);
	}

	private String frequencyText() {
		return tripped ? String.format(", TRIPPED on frequency (%.1f Hz): fix the overload, then right-click to restart", tripHertz) : String.format(", %.1f Hz", hertz);
	}

	private Component inspectText() {
		String earth = level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you";
		String drain = (drained ? ", drain connected" : ", no drain pipe (condensate is lost)")
				+ (cooled ? ", condensate cooled" : ", NO COOLING TOWER (needs " + Math.round(100 / Tuning.UNCOOLED_EFFICIENCY - 100) + "% more steam)");
		String starved = satisfaction < 0.98 ? ", STARVED of steam (" + Math.round(satisfaction * 100) + "%)" : "";
		return Component.literal("Steam turbine (" + Voltage.LOW.label() + "): " + String.format("%.0f V, %.1f A, %.0f W", volts, amps, watts()) + starved + drain + "; " + earth);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("satisfaction", satisfaction);
		output.putBoolean("tripped", tripped);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		satisfaction = input.getDoubleOr("satisfaction", 1.0);
		tripped = input.getBooleanOr("tripped", false);
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return getBlockState().getValue(SteamTurbineBlock.RUNNING) ? WildcatterSounds.TURBINE_WHINE : null;
	}

	@Override
	public float soundPitch() {
		return 1.4F;   // a small machine sings higher
	}

	@Override
	public float soundVolume() {
		return 0.35F;
	}
}
