package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPartEntity;
import java.util.ArrayList;
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
import org.jspecify.annotations.Nullable;

/**
 * The 16.5 MW industrial gas turbine package and its controller (docs/multiblocks/gas-turbine.md): the inlet house, the generator and
 * an exhaust stack or HRSG touch it. Right-click starts it: its 300 hp 4.16 kV starter purges it at a quarter speed for a minute, it
 * lights off, and it runs up to full speed; at speed its generator synchronizes. It burns pipeline gas, diesel or kerosene off its
 * fuel nozzle at its heat rate, and hands its exhaust heat to an HRSG.
 */
public class GasTurbineBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity, dev.wildcatter.kinetic.Sounding {
	public enum Phase {
		STOPPED, PURGE, RUNUP, RUNNING
	}

	private static final double FULL_FUEL_WATTS = Tuning.GT_WATTS * Tuning.GT_HEAT_RATE / 3600.0;   // 46.9 MW

	private final Motor starter = new Motor(true, Voltage.MEDIUM, Tuning.GT_STARTER_WATTS);
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private boolean grown;
	private Phase phase = Phase.STOPPED;
	private boolean wantRun;
	private boolean starterWanted;
	private double speed;
	private double timer;
	private double fuelJoules;
	private double fuelWatts;
	private String fuel = "";
	private String missing = "";
	private String trip = "";
	private double volts;

	public GasTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GAS_TURBINE_ENTITY, pos, state);
	}

	/** Start, stop, or reset after a trip. */
	public Component pushButton() {
		GeneratorPartBlockEntity gen = level == null ? null : generator(level);
		if (!trip.isEmpty() || gen != null && gen.tripped()) {
			trip = "";
			if (gen != null) {
				gen.reset();
			}
			return Component.literal("Gas turbine reset");
		}
		wantRun = !wantRun;
		setChanged();
		return Component.literal(wantRun ? "Gas turbine starting: purge, light-off, run-up" : "Gas turbine stopping");
	}

	public void setWantRun(boolean value) {
		wantRun = value;
		setChanged();
	}

	/** Game test hook: once purging, skip the rest of the start (three minutes) to full speed. */
	public void skipStartForTest() {
		if (phase == Phase.PURGE || phase == Phase.RUNUP) {
			speed = 1.0;
			phase = Phase.RUNNING;
			setChanged();
		}
	}

	public Phase phase() {
		return phase;
	}

	public double speed() {
		return speed;
	}

	/** The heat in its exhaust above the stack's temperature, in watts: what it burns less what it makes. */
	public double exhaustWatts() {
		GeneratorPartBlockEntity gen = level == null ? null : generator(level);
		double electric = gen == null ? 0 : Math.max(0, gen.deliveredWatts());
		return Math.max(0, fuelWatts - electric);
	}

	public double fuelWatts() {
		return fuelWatts;
	}

	// ---- its parts, by touching --------------------------------------------------------------------------------------------------
	private @Nullable GeneratorPartBlockEntity generator(Level level) {
		return Touching.find(level, worldPosition, GeneratorPartBlockEntity.class, g -> !g.getBlockState().is(WildcatterBlocks.ST_GENERATOR));
	}

	private boolean has(Level level, TurbinePartBlock.Part part) {
		return Touching.find(level, worldPosition, BlockEntity.class, e -> e.getBlockState().getBlock() instanceof TurbinePartBlock t && t.part() == part) != null;
	}

	private @Nullable HrsgBlockEntity hrsg(Level level) {
		return Touching.find(level, worldPosition, HrsgBlockEntity.class, h -> true);
	}

	private List<String> missing(Level level) {
		List<String> out = new ArrayList<>();
		if (!has(level, TurbinePartBlock.Part.GT_INLET_HOUSE)) {
			out.add("an INLET HOUSE");
		}
		if (generator(level) == null) {
			out.add("a GENERATOR");
		}
		if (!has(level, TurbinePartBlock.Part.GT_EXHAUST_STACK) && hrsg(level) == null) {
			out.add("an EXHAUST STACK or HRSG");
		}
		return out;
	}

	// ---- fuel off its nozzle: the west face of the cell at x 0, z 6 -------------------------------------------------------------
	private List<Lines.Face> fuelFaces() {
		return LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[0] == 0 && at[1] == 0 && at[2] == 6, Direction.WEST);
	}

	private static double joules(Product p) {
		return switch (p) {
			case PIPELINE_GAS -> Tuning.GAS_JOULES_PER_M3;
			case DIESEL, KEROSENE -> Tuning.LIQUID_FUEL_JOULES_PER_L;
			default -> 0;
		};
	}

	private void refuel(Level level) {
		double room = FULL_FUEL_WATTS * 5 - fuelJoules;   // five seconds at full load
		if (room <= 0) {
			return;
		}
		for (Lines.Line line : Lines.lines(level, fuelFaces())) {
			Product p = line.run().product();
			double per = p == null ? 0 : joules(p);
			if (per > 0) {
				fuelJoules += Lines.pull(line, p, room / per, FluidNode.Pull.MACHINE, this) * per;
				fuel = p.label();
				return;
			}
			if (p != null && p.gas()) {
				fuel = "WRONG GAS: " + p.label() + " - it needs pipeline gas (process it in a gas plant)";
			}
		}
	}

	// ---- every tick ------------------------------------------------------------------------------------------------------------
	public static void tick(Level level, BlockPos pos, BlockState state, GasTurbineBlockEntity gt) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		double dt = 1.0 / 20.0;
		if ((level.getGameTime() + pos.hashCode()) % 20 == 0) {
			gt.missing = String.join(", ", gt.missing(level));
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL == 0) {
			gt.refuel(level);
		}
		GeneratorPartBlockEntity gen = gt.generator(level);
		boolean formed = gt.missing.isEmpty() && gen != null;
		boolean go = gt.wantRun && formed && gt.trip.isEmpty() && (gen == null || !gen.tripped());
		double burn = 0;
		switch (gt.phase) {
			case STOPPED -> {
				gt.starterWanted = false;
				gt.speed = Math.max(0, gt.speed - dt / 60.0);
				if (go) {
					gt.phase = Phase.PURGE;
					gt.timer = 0;
				}
			}
			case PURGE -> {
				gt.starterWanted = true;
				gt.timer += dt;
				if (gt.starter.output() > 0) {
					gt.speed = Math.min(0.25, gt.speed + dt / 20.0);
				} else if (gt.timer > 4) {
					gt.stop(server, "STARTER: no 4.16 kV at its terminal box (" + gt.starter.describe() + ")");
				}
				if (!go) {
					gt.phase = Phase.STOPPED;
				} else if (gt.timer >= Tuning.GT_PURGE_SECONDS) {
					if (gt.fuelJoules < FULL_FUEL_WATTS * Tuning.GT_NO_LOAD_FUEL) {
						gt.stop(server, "NO FUEL at light-off: pipe pipeline gas, diesel or kerosene to its fuel nozzle (west side)");
					} else {
						gt.phase = Phase.RUNUP;
						server.playSound(null, pos, WildcatterSounds.BURNER_ROAR, SoundSource.BLOCKS, 1.5F, 1.4F);
					}
				}
			}
			case RUNUP -> {
				gt.starterWanted = gt.speed < Tuning.GT_STARTER_DROPOUT;
				if (gt.starterWanted && gt.starter.output() <= 0) {
					gt.stop(server, "STARTER: lost its 4.16 kV during the run-up");
				}
				gt.speed = Math.min(1.0, gt.speed + 0.75 * dt / Tuning.GT_RUNUP_SECONDS);
				burn = FULL_FUEL_WATTS * Tuning.GT_NO_LOAD_FUEL * gt.speed;
				if (!go) {
					gt.phase = Phase.STOPPED;
				} else if (gt.speed >= 1.0) {
					gt.phase = Phase.RUNNING;
				}
			}
			case RUNNING -> {
				gt.starterWanted = false;
				double load = gen == null ? 0 : Math.max(0, Math.min(1, gen.deliveredWatts() / Tuning.GT_WATTS));
				burn = FULL_FUEL_WATTS * (Tuning.GT_NO_LOAD_FUEL + (1 - Tuning.GT_NO_LOAD_FUEL) * load);
				if (!go) {
					gt.phase = Phase.STOPPED;
				}
			}
		}
		if (burn > 0) {
			if (gt.fuelJoules < burn * dt) {
				gt.stop(server, "FLAMEOUT: out of fuel");
				burn = 0;
			} else {
				gt.fuelJoules -= burn * dt;
			}
		}
		gt.fuelWatts = burn;
		if (gen != null) {
			gen.drive(level, gt.phase == Phase.RUNNING, Tuning.GT_WATTS);
		}
		gt.running.update(gt, gt.phase != Phase.STOPPED || gt.speed > 0.05);
		gt.setChanged();
	}

	private void stop(ServerLevel level, String why) {
		trip = why;
		phase = Phase.STOPPED;
		wantRun = false;
		starterWanted = false;
		level.playSound(null, worldPosition, WildcatterSounds.BREAKER_TRIP, SoundSource.BLOCKS, 1.0F, 0.8F);
		CircuitManager.markDirty(level);
	}

	// ---- the starter: a 4.16 kV motor, its terminal box on the west face of the cell at x 0, z 2 -----------------------------------
	private BlockPos starterBox() {
		return LeasePartBlock.at(worldPosition, getBlockState(), 0, 0, 2);
	}

	private Direction starterFace() {
		return LeasePartBlock.world(getBlockState(), Direction.WEST);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(starterBox()) && face == starterFace() && voltage == Voltage.MEDIUM && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		starter.build(circuit, starterBox(), List.of(starterFace()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		starter.apply(circuit, ticks, starterWanted);
		volts = starter.volts();
	}

	/** The plant's efficiency, with the steam turbine if an HRSG feeds one. */
	private double plantWatts(Level level) {
		GeneratorPartBlockEntity gen = generator(level);
		double out = gen == null ? 0 : Math.max(0, gen.deliveredWatts());
		HrsgBlockEntity h = hrsg(level);
		if (h != null) {
			GeneratorPartBlockEntity st = h.steamGenerator(level);
			out += st == null ? 0 : Math.max(0, st.deliveredWatts());
		}
		return out;
	}

	@Override
	public Component inspect() {
		String name = "Gas turbine (" + Math.round(Tuning.GT_WATTS / 1e5) / 10.0 + " MW, " + Voltage.GENERATOR.label() + ")";
		if (level == null) {
			return Component.literal(name);
		}
		if (!missing.isEmpty()) {
			return Component.literal(name + ": NOT BUILT, needs " + missing + " against it");
		}
		GeneratorPartBlockEntity gen = generator(level);
		String state = !trip.isEmpty() ? "STOPPED: " + trip + " (right-click to reset)"
				: switch (phase) {
					case STOPPED -> speed > 0.01 ? String.format("coasting down, %.0f%% speed", speed * 100) : "STOPPED: right-click to start";
					case PURGE -> String.format("PURGE: the starter turns it at %.0f%% speed (%.0f s), %s", speed * 100, timer, starter.describe());
					case RUNUP -> String.format("RUN-UP: lit, %.0f%% speed%s", speed * 100, starterWanted ? ", starter on" : ", starter off");
					case RUNNING -> "RUNNING at full speed";
				};
		String fuelLine = fuelWatts > 0 ? String.format("; burning %.1f MW of %s", fuelWatts / 1e6, fuel) : fuel.startsWith("WRONG") ? "; " + fuel : "";
		double plant = plantWatts(level);
		String eff = fuelWatts > 0 && plant > 1e5 ? String.format("; plant %.1f MW, %.0f%% efficient", plant / 1e6, plant / fuelWatts * 100) : "";
		return Component.literal(name + ": " + state + "; " + (gen == null ? "" : gen.status()) + fuelLine + eff);
	}

	// ---- sound -----------------------------------------------------------------------------------------------------------------
	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? WildcatterSounds.TURBINE_WHINE : null;
	}

	@Override
	public float soundPitch() {
		return 1.3F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 1.0F : 0;
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
		output.putString("phase", phase.name());
		output.putBoolean("wantRun", wantRun);
		output.putDouble("speed", speed);
		output.putDouble("fuel", fuelJoules);
		output.putString("trip", trip);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		grown = input.getBooleanOr("grown", grown);
		try {
			phase = Phase.valueOf(input.getStringOr("phase", "STOPPED"));
		} catch (IllegalArgumentException e) {
			phase = Phase.STOPPED;
		}
		wantRun = input.getBooleanOr("wantRun", false);
		speed = input.getDoubleOr("speed", 0);
		fuelJoules = input.getDoubleOr("fuel", 0);
		trip = input.getStringOr("trip", "");
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return running.tag();
	}
}
