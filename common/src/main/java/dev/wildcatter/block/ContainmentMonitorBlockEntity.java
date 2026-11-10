package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The containment monitor (docs/multiblocks/reactor-extensions.md, 19a), the building's controller. Every 10 s it flood-fills the air
 * from the cell in front of it, through everything but containment concrete, a shut equipment hatch and the airlock: kept in, it reads
 * CLOSED and the free volume; got out (over 60,000 m3, or into open ground), OPEN and near where. Closed round a reactor vessel, it
 * takes the vessel's hydrogen into the lower compartment (the 5,000 m3 nearest the vessel, as 8d's building air), mixes it into the
 * dome over an hour, lets the recombiners and powered igniters in each compartment work on it, and blasts a compartment that reaches
 * 4 % with no powered igniter. The steam let into it raises its pressure (ideal gas, simplified); at 2.5 times design its hatches fail.
 */
public class ContainmentMonitorBlockEntity extends PlantUnitBlockEntity {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> MONITORS = new HashMap<>();

	public static void clearAll() {
		MONITORS.clear();
	}

	/** The closed containment round {@code vessel}, or null. */
	public static @Nullable ContainmentMonitorBlockEntity round(Level level, BlockPos vessel) {
		for (BlockPos p : Set.copyOf(MONITORS.getOrDefault(level.dimension(), Set.of()))) {
			if (level.isLoaded(p) && level.getBlockEntity(p) instanceof ContainmentMonitorBlockEntity m && m.closed && m.vessels.contains(vessel)) {
				return m;
			}
		}
		return null;
	}

	// the last fill
	private boolean closed;
	private boolean filled;
	private double volume;
	private String leak = "";
	private int topY;
	private int lowerTopY;
	private double centreX;
	private double centreZ;
	private final Set<BlockPos> vessels = new HashSet<>();
	private final List<BlockPos> recombiners = new ArrayList<>();
	private final List<BlockPos> igniters = new ArrayList<>();
	private final List<BlockPos> hatches = new ArrayList<>();
	// the air
	private double lowerKg;
	private double domeKg;
	private double steamKg;
	private double pendingH2;
	private double pendingSteam;
	private int burns;
	private boolean blasted;
	private String event = "";

	public ContainmentMonitorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CONTAINMENT_MONITOR_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public boolean closed() {
		return closed;
	}

	public double volume() {
		return volume;
	}

	public String leak() {
		return leak;
	}

	private double lowerM3() {
		return Math.min(Tuning.CONTAINMENT_LOWER_M3, volume / 2);
	}

	private double domeM3() {
		return Math.max(1, volume - lowerM3());
	}

	/** Hydrogen, of the lower compartment's air. */
	public double lowerH2() {
		return volume <= 0 ? 0 : lowerKg / Tuning.H2_KG_PER_M3 / lowerM3();
	}

	public double domeH2() {
		return volume <= 0 ? 0 : domeKg / Tuning.H2_KG_PER_M3 / domeM3();
	}

	/** MPa over the atmosphere's, from the steam in it (ideal gas at 100 C, simplified). */
	public double pressure() {
		return volume <= 0 ? 0 : steamKg / 0.018 * 8.314 * 373.15 / volume / 1e6;
	}

	public int burns() {
		return burns;
	}

	public boolean blasted() {
		return blasted;
	}

	/** The vessel's hydrogen made this step goes into the lower compartment. */
	public void addHydrogen(double kg) {
		pendingH2 += Math.max(0, kg);
	}

	/** Steam let into the building (the PORVs with no relief tank, its burst disc, a failed vessel). */
	public void addSteam(double kg) {
		pendingSteam += Math.max(0, kg);
	}

	/** For the game test: hydrogen straight into the lower compartment. */
	public void addHydrogenForTest(double kg) {
		lowerKg += kg;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ContainmentMonitorBlockEntity monitor) {
		MONITORS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
		long t = level.getGameTime() + pos.hashCode();
		if (!monitor.filled || t % Tuning.CONTAINMENT_FILL_TICKS == 0) {
			monitor.fill(level);
		}
		if (t % Tuning.WELL_INTERVAL == 0) {
			monitor.step((ServerLevel) level, seconds());
			monitor.setChanged();
		}
	}

	/** A sneak-click: fill it again now. */
	public String refill(Level level) {
		fill(level);
		return inspect().getString();
	}

	// ---- the fill ----------------------------------------------------------------------------------------------------------------
	private boolean seals(Level level, BlockPos p, BlockState s) {
		Block b = s.getBlock();
		if (b == WildcatterBlocks.CONTAINMENT_CONCRETE) {
			return true;
		}
		if (b == WildcatterBlocks.EQUIPMENT_HATCH || b == WildcatterBlocks.PERSONNEL_AIRLOCK) {
			return BigParts.entity(level, p) instanceof ContainmentDoorBlockEntity door && door.seals();
		}
		return false;
	}

	void fill(Level level) {
		filled = true;
		vessels.clear();
		recombiners.clear();
		igniters.clear();
		hatches.clear();
		Direction f = getBlockState().getValue(BigPartBlock.FACING);
		BlockPos start = worldPosition.relative(f);
		Set<Long> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(start.asLong());
		queue.add(start);
		int air = 0;
		int minY = level.getMinY(), maxY = level.getMaxY();
		BlockPos far = start;
		String out = null;
		int top = start.getY();
		double sx = 0, sz = 0;
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			BlockState s = level.getBlockState(p);
			if (s.isAir()) {
				air++;
				sx += p.getX();
				sz += p.getZ();
			}
			top = Math.max(top, p.getY());
			Block b = s.getBlock();
			if (b == WildcatterBlocks.REACTOR_VESSEL) {
				vessels.add(BigParts.owner(level, p));
			} else if (b == WildcatterBlocks.HYDROGEN_RECOMBINER) {
				BlockPos o = BigParts.owner(level, p);
				if (!recombiners.contains(o)) {
					recombiners.add(o);
				}
			} else if (b == WildcatterBlocks.HYDROGEN_IGNITER) {
				igniters.add(p.immutable());
			}
			if (p.distSqr(start) > far.distSqr(start)) {
				far = p;
			}
			if (seen.size() > Tuning.CONTAINMENT_MAX_CELLS || air > Tuning.CONTAINMENT_MAX_M3) {
				out = String.format("over %,d m3: the air gets out near %d %d %d", (int) Tuning.CONTAINMENT_MAX_M3, far.getX(), far.getY(), far.getZ());
				break;
			}
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (!seen.add(n.asLong())) {
					continue;
				}
				if (n.getY() < minY || n.getY() > maxY || !level.isLoaded(n)) {
					out = String.format("the air gets out near %d %d %d", p.getX(), p.getY(), p.getZ());
					break;
				}
				BlockState ns = level.getBlockState(n);
				if (seals(level, n, ns)) {
					if (ns.getBlock() == WildcatterBlocks.EQUIPMENT_HATCH) {
						BlockPos o = BigParts.owner(level, n);
						if (!hatches.contains(o)) {
							hatches.add(o);
						}
					}
					continue;
				}
				queue.add(n);
			}
			if (out != null) {
				break;
			}
		}
		boolean was = closed;
		closed = out == null && air > 0;
		leak = out == null ? "" : out;
		volume = closed ? air : 0;
		topY = top;
		centreX = air > 0 ? sx / air + 0.5 : worldPosition.getX() + 0.5;
		centreZ = air > 0 ? sz / air + 0.5 : worldPosition.getZ() + 0.5;
		int vesselY = vessels.stream().mapToInt(BlockPos::getY).min().orElse(start.getY());
		lowerTopY = vesselY + 13;   // the vessel, its head and the steam generators' height: the lower compartment
		if (was && !closed) {
			lowerKg = domeKg = steamKg = 0;   // opened: the building air mixes with the outside
		}
		setChanged();
	}

	// ---- the air -------------------------------------------------------------------------------------------------------------------
	private boolean lower(BlockPos p) {
		return p.getY() < lowerTopY;
	}

	private void step(ServerLevel level, double dt) {
		if (!closed) {
			pendingH2 = pendingSteam = 0;
			return;
		}
		lowerKg += pendingH2;
		steamKg += pendingSteam;
		pendingH2 = pendingSteam = 0;
		// the lower compartment mixes into the dome
		double vl = lowerM3(), vd = domeM3();
		double move = (lowerKg / vl - domeKg / vd) * (vl * vd / (vl + vd)) * (1 - Math.exp(-dt / Tuning.CONTAINMENT_MIX_S));
		lowerKg -= move;
		domeKg += move;
		// the recombiners, each in its compartment
		for (BlockPos par : recombiners) {
			boolean low = lower(par);
			double c = low ? lowerH2() : domeH2();
			if (c > Tuning.PAR_START) {
				double kg = Tuning.PAR_KG_H / 3600 * Math.min(1.5, c / Tuning.H2_FLAMMABLE) * dt;
				if (low) {
					lowerKg = Math.max(0, lowerKg - kg);
				} else {
					domeKg = Math.max(0, domeKg - kg);
				}
			}
		}
		// the powered igniters burn a compartment's hydrogen at 4 % in a small deliberate burn
		boolean lowerLit = false, domeLit = false;
		for (BlockPos ig : igniters) {
			BlockState s = level.getBlockState(ig);
			if (s.getBlock() == WildcatterBlocks.HYDROGEN_IGNITER && s.getValue(LoadBlock.LIT)) {
				if (lower(ig)) {
					lowerLit = true;
				} else {
					domeLit = true;
				}
			}
		}
		if (lowerLit && lowerH2() >= Tuning.H2_FLAMMABLE) {
			lowerKg = Tuning.IGNITER_BURN_TO * Tuning.H2_KG_PER_M3 * vl;
			burn(level, true);
		}
		if (domeLit && domeH2() >= Tuning.H2_FLAMMABLE) {
			domeKg = Tuning.IGNITER_BURN_TO * Tuning.H2_KG_PER_M3 * vd;
			burn(level, false);
		}
		// with no powered igniter there, 4 % is a blast (8d's, inside the building)
		if (lowerH2() >= Tuning.H2_FLAMMABLE || domeH2() >= Tuning.H2_FLAMMABLE) {
			boolean low = lowerH2() >= Tuning.H2_FLAMMABLE;
			BlockPos vessel = vessels.isEmpty() ? worldPosition : vessels.iterator().next();
			double y = low ? vessel.getY() + 12 : topY - 2;
			double x = low ? vessel.getX() + 0.5 : centreX;
			double z = low ? vessel.getZ() + 0.5 : centreZ;
			level.explode(null, x, y, z, Tuning.H2_BLAST_POWER, Level.ExplosionInteraction.BLOCK);
			if (low) {
				lowerKg = 0;
			} else {
				domeKg = 0;
			}
			blasted = true;
			event = "HYDROGEN BLAST in the " + (low ? "lower compartment" : "dome");
			for (BlockPos v : vessels) {
				if (level.getBlockEntity(v) instanceof ReactorVesselBlockEntity rv) {
					rv.reactor().markExploded();
					rv.setChanged();
				}
			}
		}
		// the steam condenses on the liner; its pressure
		steamKg *= Math.exp(-dt / Tuning.CONTAINMENT_CONDENSE_S);
		if (pressure() > Tuning.CONTAINMENT_DESIGN_MPA * Tuning.CONTAINMENT_FAIL_FACTOR) {
			boolean damaged = vessels.stream().anyMatch(v -> level.getBlockEntity(v) instanceof ReactorVesselBlockEntity rv && rv.reactor().coreDamaged());
			for (BlockPos h : hatches) {
				if (level.getBlockEntity(h) instanceof ContainmentDoorBlockEntity door) {
					door.fail();
					dev.wildcatter.radiation.Contamination.splash(level, h, 32, damaged ? 1.0 : 0.2);   // 19b, with its rule
				}
			}
			event = String.format("THE EQUIPMENT HATCH FAILED at %.2f MPa", pressure());
			fill(level);
		}
	}

	private void burn(ServerLevel level, boolean low) {
		burns++;
		event = "igniters burned the " + (low ? "lower compartment's" : "dome's") + " hydrogen (no blast)";
		for (BlockPos ig : igniters) {
			if (lower(ig) == low) {
				level.sendParticles(ParticleTypes.FLAME, ig.getX() + 0.5, ig.getY() + 0.5, ig.getZ() + 0.5, 30, 1.5, 1.0, 1.5, 0.02);
			}
		}
	}

	// ---- the PLC and the readout --------------------------------------------------------------------------------------------------
	@Override
	public List<Point> points() {
		return List.of(new Point("CLOSED", false), new Point("H2 HIGH (2 %)", false), new Point("PRESSURE HIGH (design)", false),
				new Point("H2 LOWER 4-20 mA, 10 %", false, Signal.MA), new Point("H2 DOME 4-20 mA, 10 %", false, Signal.MA), new Point("PRESSURE 4-20 mA, 1 MPa", false, Signal.MA));
	}

	@Override
	public boolean readPoint(int point) {
		return switch (point) {
			case 0 -> closed;
			case 1 -> Math.max(lowerH2(), domeH2()) > 0.02;
			case 2 -> pressure() > Tuning.CONTAINMENT_DESIGN_MPA;
			default -> false;
		};
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
	}

	@Override
	public double readMa(int point) {
		double share = switch (point) {
			case 3 -> lowerH2() / 0.10;
			case 4 -> domeH2() / 0.10;
			case 5 -> pressure();
			default -> 0;
		};
		return 4 + 16 * Math.max(0, Math.min(1.03, share));
	}

	@Override
	public Component inspect() {
		int lit = 0;
		if (level != null) {
			for (BlockPos ig : igniters) {
				BlockState s = level.getBlockState(ig);
				lit += s.getBlock() == WildcatterBlocks.HYDROGEN_IGNITER && s.getValue(LoadBlock.LIT) ? 1 : 0;
			}
		}
		double dose = level instanceof ServerLevel sl ? dev.wildcatter.radiation.RadiationTicker.rateAt(sl, net.minecraft.world.phys.Vec3.atCenterOf(worldPosition.relative(
				getBlockState().getValue(BigPartBlock.FACING)).above()), null) : 0;
		String building = closed ? String.format("CLOSED, %,.0f m3 free (lower compartment %,.0f m3)", volume, lowerM3()) : "OPEN: " + (leak.isEmpty() ? "no air in front of it" : leak);
		return Component.literal(String.format("Containment monitor: %s; %.3f MPa gauge (design %.2f)%s; hydrogen lower %.1f %%, dome %.1f %%; %d recombiners, %d igniters (%d powered), %d burns; "
				+ "%d reactor vessels inside; dose %s%s", building, pressure(), Tuning.CONTAINMENT_DESIGN_MPA, pressure() > Tuning.CONTAINMENT_DESIGN_MPA ? " OVER DESIGN" : "",
				lowerH2() * 100, domeH2() * 100, recombiners.size(), igniters.size(), lit, burns, vessels.size(), dev.wildcatter.fluid.Radiation.rateText(dose),
				event.isEmpty() ? "" : "; " + event));
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && !level.isClientSide()) {
			java.util.Optional.ofNullable(MONITORS.get(level.dimension())).ifPresent(set -> set.remove(worldPosition));
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("lowerKg", lowerKg);
		output.putDouble("domeKg", domeKg);
		output.putDouble("steamKg", steamKg);
		output.putInt("burns", burns);
		output.putBoolean("blasted", blasted);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lowerKg = input.getDoubleOr("lowerKg", 0);
		domeKg = input.getDoubleOr("domeKg", 0);
		steamKg = input.getDoubleOr("steamKg", 0);
		burns = input.getIntOr("burns", 0);
		blasted = input.getBooleanOr("blasted", false);
	}
}
