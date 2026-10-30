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
import java.util.ArrayList;
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
import org.jspecify.annotations.Nullable;

/**
 * A generator. Alone it is a 110 V set; its output sags as the current rises (it has some resistance of its own), and
 * it only draws as much steam from the boiler as the circuit is actually using. If the boiler cannot keep up, the
 * voltage falls: a brownout, not a blackout.
 *
 * A 3x3x3 cube of 27 turbines becomes one medium voltage (4.16 kV) generator. The lowest corner turbine is the
 * master: it feeds the steam of the whole cube and holds the shared "how much steam did we get" number. Every
 * turbine in the cube still makes its share of the power and has terminals on all sides, so a wire on any face
 * of the cube is connected to the whole generator.
 */
public class SteamTurbineBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	private static final int CUBE = Tuning.BIG_SIZE * Tuning.BIG_SIZE * Tuning.BIG_SIZE;

	private int hot;
	private int neutral;
	private int source;
	private double volts;
	private double amps;
	/** What share of the demanded steam the boiler is managing to supply (1 = all of it). Shared by a whole cube. */
	private double satisfaction = 1.0;
	/** Ticks since it last had a load; the rotor keeps spinning for a second so the block doesn't flicker. */
	private int idleTicks = 100;
	private @Nullable BlockPos master;
	private boolean isMaster;
	private double cubeWatts;
	/** Water condensed from the steam used, waiting to go out through the drain. */
	private double drainWater;
	private boolean drained;
	/** A cooling tower on the drain's pipes is running. Without one the turbine needs more steam per joule. */
	private boolean cooled;

	public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STEAM_TURBINE_ENTITY, pos, state);
	}

	public double watts() {
		return Math.max(0, volts * amps);
	}

	/** True if there is voltage on its terminals to bite with. */
	public boolean isLive() {
		return volts > Tuning.SHOCK_MIN_VOLTS;
	}

	public Voltage voltage() {
		return master != null ? Voltage.MEDIUM : Voltage.LOW;
	}

	private boolean isSlave() {
		return master != null && !isMaster;
	}

	private @Nullable SteamTurbineBlockEntity root() {
		if (!isSlave()) {
			return this;
		}
		return level != null && level.getBlockEntity(master) instanceof SteamTurbineBlockEntity m && m.isMaster ? m : null;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == voltage();
	}

	@Override
	public void build(Circuit circuit) {
		SteamTurbineBlockEntity r = root();
		double sat = r != null ? r.satisfaction : 1.0;
		double ohms = master != null ? Tuning.MV_GENERATOR_OHMS * CUBE : Tuning.TURBINE_INTERNAL_OHMS;   // 27 in parallel
		hot = circuit.node();
		neutral = circuit.node();
		int internal = circuit.node();
		source = circuit.source(internal, neutral, voltage().nominal() * sat);
		circuit.resistor(internal, hot, ohms);
		circuit.ground(neutral);
		for (Direction face : Direction.values()) {
			circuit.port(worldPosition, face, Conductor.A, voltage(), hot, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, face, Conductor.N, voltage(), neutral, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = circuit.across(hot, neutral);
		amps = circuit.sourceCurrent(source);
	}

	/** Every tick: take the steam the circuit is using this tick from the boilers touching it. */
	public static void tick(Level level, BlockPos pos, BlockState state, SteamTurbineBlockEntity turbine) {
		long phase = level.getGameTime() + pos.hashCode();
		if (turbine.isSlave()) {
			SteamTurbineBlockEntity m = turbine.root();
			if (m == null || !m.contains(pos)) {
				turbine.release(level);
				return;
			}
			boolean running = m.getBlockState().getValue(SteamTurbineBlock.RUNNING);
			if (state.getValue(SteamTurbineBlock.RUNNING) != running) {
				level.setBlock(pos, state.setValue(SteamTurbineBlock.RUNNING, running), 3);
			}
			return;
		}
		if (phase % Tuning.STRUCTURE_CHECK_TICKS == 0) {
			if (turbine.isMaster) {
				if (!turbine.stillValid(level)) {
					turbine.dissolve(level);
				}
			} else {
				turbine.tryForm(level);
			}
			state = turbine.getBlockState();
		}

		double watts = turbine.isMaster ? turbine.sumWatts(level) : turbine.watts();
		turbine.cubeWatts = watts;
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
		if (running && phase % 60 == 0) {
			level.playSound(null, pos, WildcatterSounds.TURBINE_HUM, SoundSource.BLOCKS, turbine.isMaster ? 1.5F : 0.6F, 1.0F);
		}
	}

	/**
	 * The drain: a pipe touching the turbine (or, for a cube, any turbine of it) carries the condensed steam back
	 * to the boilers on the same run of pipe. With no pipe the water just spills away, so the boiler slowly dries out.
	 */
	private void drain(Level level) {
		List<BlockPos> cells = new ArrayList<>();
		int n = isMaster ? Tuning.BIG_SIZE : 1;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					cells.add(worldPosition.offset(x, y, z));
				}
			}
		}
		Set<BlockPos> pipes = Plumbing.network(level, cells);
		drained = !pipes.isEmpty();
		cooled = false;
		for (CoolingTowerBlockEntity tower : Plumbing.machines(level, cells, pipes, false, CoolingTowerBlockEntity.class)) {
			cooled |= tower.isCooling();
		}
		if (drainWater >= 1) {
			if (drained) {
				drainWater -= Plumbing.deliver(Plumbing.boilers(level, cells, pipes, false), (int) drainWater);
			} else {
				drainWater = 0;   // it just spills
			}
		}
	}

	/** Takes steam from every boiler touching this turbine (or, for a cube, touching any turbine in it). */
	private int draw(Level level, int wanted) {
		int got = 0;
		if (!isMaster) {
			for (Direction side : Direction.values()) {
				if (got < wanted && level.getBlockEntity(worldPosition.relative(side)) instanceof BoilerBlockEntity boiler) {
					got += boiler.takeSteam(wanted - got);
				}
			}
			return got;
		}
		int n = Tuning.BIG_SIZE;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					BlockPos at = worldPosition.offset(x, y, z);
					for (Direction side : Direction.values()) {
						BlockPos next = at.relative(side);
						if (got < wanted && !contains(next) && level.getBlockEntity(next) instanceof BoilerBlockEntity boiler) {
							got += boiler.takeSteam(wanted - got);
						}
					}
				}
			}
		}
		return got;
	}

	private double sumWatts(Level level) {
		double total = 0;
		int n = Tuning.BIG_SIZE;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					if (level.getBlockEntity(worldPosition.offset(x, y, z)) instanceof SteamTurbineBlockEntity t) {
						total += t.watts();
					}
				}
			}
		}
		return total;
	}

	private boolean contains(BlockPos pos) {
		return isMaster && pos.getX() >= worldPosition.getX() && pos.getX() < worldPosition.getX() + Tuning.BIG_SIZE
				&& pos.getY() >= worldPosition.getY() && pos.getY() < worldPosition.getY() + Tuning.BIG_SIZE
				&& pos.getZ() >= worldPosition.getZ() && pos.getZ() < worldPosition.getZ() + Tuning.BIG_SIZE;
	}

	/** Looks for a 3x3x3 cube of free turbines with this one as its lowest corner. */
	private void tryForm(Level level) {
		int n = Tuning.BIG_SIZE;
		BlockPos o = worldPosition;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					if (!(level.getBlockEntity(o.offset(x, y, z)) instanceof SteamTurbineBlockEntity t) || t.master != null) {
						return;
					}
				}
			}
		}
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					BlockPos at = o.offset(x, y, z);
					SteamTurbineBlockEntity t = (SteamTurbineBlockEntity) level.getBlockEntity(at);
					t.master = o;
					t.setChanged();
					level.setBlock(at, level.getBlockState(at).setValue(SteamTurbineBlock.FORMED, true), 3);
				}
			}
		}
		isMaster = true;
		satisfaction = 1.0;
		level.playSound(null, o.offset(1, 1, 1), WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.5F, 0.8F);
		CircuitManager.markDirty(level);
	}

	private boolean stillValid(Level level) {
		int n = Tuning.BIG_SIZE;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					if (!(level.getBlockEntity(worldPosition.offset(x, y, z)) instanceof SteamTurbineBlockEntity t) || !worldPosition.equals(t.master)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void dissolve(Level level) {
		int n = Tuning.BIG_SIZE;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					if (level.getBlockEntity(worldPosition.offset(x, y, z)) instanceof SteamTurbineBlockEntity t && worldPosition.equals(t.master)) {
						t.release(level);
					}
				}
			}
		}
	}

	private void release(Level level) {
		master = null;
		isMaster = false;
		setChanged();
		BlockState state = getBlockState();
		if (state.is(WildcatterBlocks.STEAM_TURBINE)) {
			level.setBlock(worldPosition, state.setValue(SteamTurbineBlock.FORMED, false), 3);
		}
		CircuitManager.markDirty(level);
	}

	@Override
	public Component inspect() {
		SteamTurbineBlockEntity r = root();
		String earth = level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you";
		if (r == null) {
			return Component.literal("Steam turbine: its generator has lost its master");
		}
		String drain = (r.drained ? ", drain connected" : ", no drain pipe (condensate is lost)")
				+ (r.cooled ? ", condensate cooled" : ", NO COOLING TOWER (needs " + Math.round(100 / Tuning.UNCOOLED_EFFICIENCY - 100) + "% more steam)");
		String starved = r.satisfaction < 0.98 ? ", STARVED of steam (" + Math.round(r.satisfaction * 100) + "%)" : "";
		if (master != null) {
			return Component.literal("Medium voltage generator (27 turbines, " + Voltage.MEDIUM.label() + "): " + String.format("%.0f V, %.0f W of %.0f W", volts, r.cubeWatts,
					CUBE * Tuning.BOILER_STEAM_PER_TICK * Tuning.JOULES_PER_STEAM * 20) + starved + drain + "; " + earth);
		}
		return Component.literal("Steam turbine (" + Voltage.LOW.label() + "): " + String.format("%.0f V, %.1f A, %.0f W", volts, amps, watts()) + starved + drain + "; " + earth);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("satisfaction", satisfaction);
		output.putBoolean("is_master", isMaster);
		if (master != null) {
			output.putInt("master_x", master.getX());
			output.putInt("master_y", master.getY());
			output.putInt("master_z", master.getZ());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		satisfaction = input.getDoubleOr("satisfaction", 1.0);
		isMaster = input.getBooleanOr("is_master", false);
		master = input.getInt("master_x").isPresent()
				? new BlockPos(input.getIntOr("master_x", 0), input.getIntOr("master_y", 0), input.getIntOr("master_z", 0)) : null;
	}
}
