package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Role;
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
 * A diesel engine or gas turbine set. Alone it is a 110 V generator; a 2x2x2 cube of the same kind joins into one
 * 4.16 kV generator (the lowest corner is the master and holds the cube's fuel). Like the steam turbine, its output
 * sags as the current rises and it burns only the fuel the circuit is using. Its tank is topped up from a refinery
 * (diesel) or a gas well (gas) on its gas lines, or by hand with a bucket of diesel. With no fuel it makes nothing.
 */
public class EngineBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	private static final int CELLS = Tuning.ENGINE_CUBE * Tuning.ENGINE_CUBE * Tuning.ENGINE_CUBE;

	private int hot;
	private int neutral;
	private int source;
	private double volts;
	private double amps;
	private double tank;
	/** Share of the demanded fuel the tank could supply; smoothed so the voltage does not jump. Shared by a cube. */
	private double satisfaction;
	private int idleTicks = 100;
	private double cubeWatts;
	private String supply = "no gas line to a fuel source";
	private @Nullable BlockPos master;
	private boolean isMaster;

	public EngineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ENGINE_ENTITY, pos, state);
	}

	private EngineBlock block() {
		return (EngineBlock) getBlockState().getBlock();
	}

	private boolean diesel() {
		return block().fuel() == EngineBlock.Fuel.DIESEL;
	}

	private boolean isSlave() {
		return master != null && !isMaster;
	}

	private @Nullable EngineBlockEntity root() {
		if (!isSlave()) {
			return this;
		}
		return level != null && level.getBlockEntity(master) instanceof EngineBlockEntity m && m.isMaster ? m : null;
	}

	public Voltage voltage() {
		return master != null ? Voltage.MEDIUM : Voltage.LOW;
	}

	private double unitWatts() {
		return diesel() ? Tuning.DIESEL_ENGINE_WATTS : Tuning.GAS_TURBINE_WATTS;
	}

	private double tankSize() {
		return (diesel() ? Tuning.DIESEL_TANK : Tuning.GAS_TURBINE_TANK) * (isMaster ? CELLS : 1);
	}

	private double joulesPerUnit() {
		return diesel() ? Tuning.DIESEL_JOULES_PER_UNIT : Tuning.GAS_JOULES_PER_UNIT;
	}

	public double watts() {
		return Math.max(0, volts * amps);
	}

	public boolean isLive() {
		return volts > Tuning.SHOCK_MIN_VOLTS;
	}

	/** Pours fuel in by hand (a diesel bucket). */
	public boolean addFuel(double units) {
		EngineBlockEntity r = root();
		if (r == null || r.tank + units > r.tankSize()) {
			return false;
		}
		r.tank += units;
		r.setChanged();
		return true;
	}

	@Override
	public boolean hasPort(Direction face, Role role, Voltage voltage) {
		return voltage == voltage();
	}

	@Override
	public void build(Circuit circuit) {
		EngineBlockEntity r = root();
		double sat = r != null ? r.satisfaction : 0;
		double nominal = voltage().nominal();
		int cells = master != null ? CELLS : 1;
		double ohms = nominal * Tuning.ENGINE_SAG / (unitWatts() * cells / nominal) * cells;   // sags ENGINE_SAG at full load, cells in parallel
		hot = circuit.node();
		neutral = circuit.node();
		int internal = circuit.node();
		source = circuit.source(internal, neutral, nominal * sat);
		circuit.resistor(internal, hot, ohms);
		circuit.ground(neutral);
		for (Direction face : Direction.values()) {
			circuit.port(worldPosition, face, Role.HOT, voltage(), hot, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, face, Role.NEUTRAL, voltage(), neutral, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = circuit.across(hot, neutral);
		amps = circuit.sourceCurrent(source);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, EngineBlockEntity engine) {
		long phase = level.getGameTime() + pos.hashCode();
		if (engine.isSlave()) {
			EngineBlockEntity m = engine.root();
			if (m == null || !m.contains(pos)) {
				engine.release(level);
				return;
			}
			boolean running = m.getBlockState().getValue(EngineBlock.RUNNING);
			if (state.getValue(EngineBlock.RUNNING) != running) {
				level.setBlock(pos, state.setValue(EngineBlock.RUNNING, running), 3);
			}
			return;
		}
		if (phase % Tuning.STRUCTURE_CHECK_TICKS == 0) {
			if (engine.isMaster) {
				if (!engine.stillValid(level)) {
					engine.dissolve(level);
				}
			} else {
				engine.tryForm(level);
			}
			state = engine.getBlockState();
		}
		if (phase % Tuning.WELL_INTERVAL == 0) {
			engine.refill(level);
		}
		double watts = engine.isMaster ? engine.sumWatts(level) : engine.watts();
		engine.cubeWatts = watts;
		double units = watts / 20.0 / engine.joulesPerUnit();
		double share = 1.0;
		if (units > 0) {
			share = engine.tank >= units ? 1.0 : engine.tank / units;
			engine.tank = Math.max(0, engine.tank - units);
			engine.idleTicks = 0;
		} else {
			engine.idleTicks = Math.min(engine.idleTicks + 1, 100);
		}
		double target = engine.tank <= 0 ? 0 : share;
		double before = engine.satisfaction;
		engine.satisfaction += (target - engine.satisfaction) * Tuning.TURBINE_STEAM_SMOOTHING;
		if (Math.abs(engine.satisfaction - before) > 0.002) {
			CircuitManager.markDirty(level);   // the source's voltage follows the fuel
		}
		boolean running = engine.idleTicks < 20 && engine.satisfaction > 0.05;
		if (state.getValue(EngineBlock.RUNNING) != running) {
			level.setBlock(pos, state.setValue(EngineBlock.RUNNING, running), 3);
		}
		if (running && phase % 40 == 0) {
			level.playSound(null, pos, WildcatterSounds.TURBINE_HUM, SoundSource.BLOCKS, engine.isMaster ? 1.5F : 0.8F, engine.diesel() ? 0.6F : 1.6F);
		}
	}

	private List<BlockPos> cells() {
		List<BlockPos> cells = new ArrayList<>();
		int n = isMaster ? Tuning.ENGINE_CUBE : 1;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					cells.add(worldPosition.offset(x, y, z));
				}
			}
		}
		return cells;
	}

	/** Tops the tank up from whatever fuel source is on its gas lines (or touching it). */
	private void refill(Level level) {
		double room = tankSize() - tank;
		if (room < 1) {
			return;
		}
		List<BlockPos> cells = cells();
		Set<BlockPos> lines = Plumbing.network(level, cells, PipeBlock.Kind.GAS);
		double got = 0;
		if (diesel()) {
			for (RefineryBlockEntity refinery : Plumbing.machines(level, cells, lines, true, RefineryBlockEntity.class)) {
				got += refinery.drawDiesel(room - got);
			}
			supply = got > 0 ? "diesel from a refinery" : "no diesel on its gas lines: pour in a bucket, or run a line to a refinery";
		} else {
			for (GasWellheadBlockEntity head : Plumbing.machines(level, cells, lines, true, GasWellheadBlockEntity.class)) {
				got += head.offer(room - got);
			}
			supply = got > 0 ? "gas from a well" : "no gas: run a gas line to a gas wellhead";
		}
		if (got > 0) {
			tank += got;
			setChanged();
		}
	}

	private double sumWatts(Level level) {
		double total = 0;
		for (BlockPos at : cells()) {
			if (level.getBlockEntity(at) instanceof EngineBlockEntity e) {
				total += e.watts();
			}
		}
		return total;
	}

	private boolean contains(BlockPos pos) {
		int n = Tuning.ENGINE_CUBE;
		return isMaster && pos.getX() >= worldPosition.getX() && pos.getX() < worldPosition.getX() + n
				&& pos.getY() >= worldPosition.getY() && pos.getY() < worldPosition.getY() + n
				&& pos.getZ() >= worldPosition.getZ() && pos.getZ() < worldPosition.getZ() + n;
	}

	/** Looks for a 2x2x2 cube of free engines of this kind with this one as its lowest corner. */
	private void tryForm(Level level) {
		int n = Tuning.ENGINE_CUBE;
		BlockPos o = worldPosition;
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					if (!(level.getBlockEntity(o.offset(x, y, z)) instanceof EngineBlockEntity e) || e.master != null || e.block() != block()) {
						return;
					}
				}
			}
		}
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				for (int z = 0; z < n; z++) {
					EngineBlockEntity e = (EngineBlockEntity) level.getBlockEntity(o.offset(x, y, z));
					if (e != this) {
						tank += e.tank;     // their fuel joins the shared tank
						e.tank = 0;
					}
					e.master = o;
					e.setChanged();
				}
			}
		}
		isMaster = true;
		satisfaction = 0;
		level.playSound(null, o.offset(1, 1, 1), WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.5F, 0.8F);
		CircuitManager.markDirty(level);
	}

	private boolean stillValid(Level level) {
		for (BlockPos at : cells()) {
			if (!(level.getBlockEntity(at) instanceof EngineBlockEntity e) || !worldPosition.equals(e.master)) {
				return false;
			}
		}
		return true;
	}

	private void dissolve(Level level) {
		for (BlockPos at : cells()) {
			if (level.getBlockEntity(at) instanceof EngineBlockEntity e && worldPosition.equals(e.master)) {
				e.release(level);
			}
		}
	}

	private void release(Level level) {
		master = null;
		isMaster = false;
		setChanged();
		CircuitManager.markDirty(level);
	}

	@Override
	public Component inspect() {
		EngineBlockEntity r = root();
		String earth = level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you";
		if (r == null) {
			return Component.literal("Engine: its generator has lost its master");
		}
		String name = diesel() ? "Diesel" : "Gas turbine";
		String state = r.tank <= 0 ? "OUT OF FUEL (" + r.supply + ")"
				: r.satisfaction < 0.98 ? "fuel starved (" + Math.round(r.satisfaction * 100) + "%)" : "running";
		String fuel = ", fuel " + (int) r.tank + "/" + (int) r.tankSize() + " units, " + state + "; " + earth;
		if (master != null) {
			return Component.literal(name + " generator (8 engines, " + Voltage.MEDIUM.label() + ", rated " + Math.round(unitWatts() * CELLS / 1000) + " kW): "
					+ String.format("%.0f V, %.0f W", volts, r.cubeWatts) + fuel);
		}
		return Component.literal(name + " engine (" + Voltage.LOW.label() + ", rated " + Math.round(unitWatts() / 1000.0 * 10) / 10.0 + " kW; build a 2x2x2 cube of them for " + Voltage.MEDIUM.label() + "): "
				+ String.format("%.0f V, %.1f A, %.0f W", volts, amps, watts()) + fuel);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("tank", tank);
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
		tank = input.getDoubleOr("tank", 0);
		satisfaction = input.getDoubleOr("satisfaction", 0);
		isMaster = input.getBooleanOr("is_master", false);
		master = input.getInt("master_x").isPresent()
				? new BlockPos(input.getIntOr("master_x", 0), input.getIntOr("master_y", 0), input.getIntOr("master_z", 0)) : null;
	}
}
