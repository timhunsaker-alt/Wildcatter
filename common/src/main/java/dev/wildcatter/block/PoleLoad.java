package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wood pole's line pull and its guys (protection-gaps 20; Tim, 2026-11-10: "an overloaded pole with no guy reads UNGUYED, then leans
 * after a while. It never breaks on its own"). Each span on an insulator the pole carries pulls toward its far end at a quarter of its
 * conductor's rated breaking strength; the pulls add as vectors (a tangent pole's cancel), each taken to the pole top in proportion to its
 * height. Guys take what pulls away from their anchors, up to their strand's strength. What is left is set against the pole's ANSI O5.1
 * class load at the top. Past it, a pole placed from 0.7.0 on leans a step every {@link Tuning#POLE_LEAN_SECONDS}; a pole from an older
 * save only reads it. A guy that holds the pull draws a leaning pole plumb again.
 */
public final class PoleLoad {
	private PoleLoad() {
	}

	/** The game test shortens the time between lean steps to this many seconds; null in play. */
	public static Double testLeanSeconds;

	/** How often a pole looks again, ticks. */
	public static final int CHECK_TICKS = 40;

	/** One check: the line pull at the top (lbf, x and z), what the guys took, what is left, and how many guys. */
	public record Reading(double lineX, double lineZ, double leftX, double leftZ, int guys, double guyLbf) {
		public double line() {
			return Math.hypot(lineX, lineZ);
		}

		public double left() {
			return Math.hypot(leftX, leftZ);
		}
	}

	/** The share of a pull at this height above the pole's foot that reaches the class load's point, 0.6 m (2 ft) below the top. */
	static double heightShare(BlockPos foot, int height, double y) {
		return Math.max(0, Math.min(1.2, (y - foot.getY()) / (height - 0.6)));
	}

	/** A guy's horizontal strength at the pole top, lbf: its strand's working strength, by its lead over its length and its height. */
	public static double guyLbf(BlockPos foot, int height, BlockPos attach, BlockPos anchor) {
		double lead = Math.hypot(anchor.getX() - attach.getX(), anchor.getZ() - attach.getZ());
		double length = Math.sqrt(attach.distSqr(anchor));
		if (lead < 1e-6 || length < 1e-6) {
			return 0;
		}
		return Tuning.GUY_STRAND_LBF * Tuning.GUY_STRENGTH_FACTOR * lead / length * heightShare(foot, height, attach.getY() + 0.8);
	}

	/** The wood pole columns within reach of an insulator at {@code pos}: an H-frame's middle insulator is shared by its two poles. */
	private static int sharedBy(Level level, BlockPos pos) {
		Set<Long> columns = new HashSet<>();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 0; dy >= -2; dy--) {
					if (level.getBlockState(pos.offset(dx, dy, dz)).getBlock() instanceof WoodPoleBlock) {
						columns.add(BlockPos.asLong(pos.getX() + dx, 0, pos.getZ() + dz));
						break;
					}
				}
			}
		}
		return Math.max(1, columns.size());
	}

	/** The pull of the spans on the insulators this pole carries, at its top, and what its guys leave of it. */
	public static Reading read(Level level, BlockPos foot, WoodPoleBlock pole, List<GuyAnchorBlockEntity> guys) {
		int height = pole.height();
		double x = 0, z = 0;
		for (BlockPos at : BlockPos.betweenClosed(foot.offset(-2, 1, -2), foot.offset(2, height + 1, 2))) {
			if (!(level.getBlockEntity(at) instanceof InsulatorBlockEntity insulator) || insulator.spans().isEmpty()) {
				continue;
			}
			double share = heightShare(foot, height, at.getY() + 0.5) / sharedBy(level, at);
			for (InsulatorBlockEntity.Span span : insulator.spans()) {
				double dx = span.other().getX() - at.getX();
				double dz = span.other().getZ() - at.getZ();
				double run = Math.hypot(dx, dz);
				if (run < 1e-6) {
					continue;
				}
				double pull = span.size().breakingLbf() * Tuning.SPAN_TENSION_SHARE * share;
				x += pull * dx / run;
				z += pull * dz / run;
			}
		}
		double leftX = x, leftZ = z, held = 0;
		for (GuyAnchorBlockEntity guy : guys) {
			BlockPos attach = guy.attach();
			if (attach == null) {
				continue;
			}
			double gx = guy.getBlockPos().getX() - attach.getX();
			double gz = guy.getBlockPos().getZ() - attach.getZ();
			double lead = Math.hypot(gx, gz);
			if (lead < 1e-6) {
				continue;
			}
			gx /= lead;
			gz /= lead;
			double strength = guyLbf(foot, height, attach, guy.getBlockPos());
			held += strength;
			// the guy takes the share of the pull that points away from its anchor, up to its strength
			double take = Math.max(0, Math.min(strength, -(leftX * gx + leftZ * gz)));
			leftX += take * gx;
			leftZ += take * gz;
		}
		return new Reading(x, z, leftX, leftZ, guys.size(), held);
	}

	/** The anchors guyed to this pole that still hold their guy to it; the others are let go. */
	static List<GuyAnchorBlockEntity> guys(Level level, BlockPos foot, BigPartBlockEntity entity) {
		List<GuyAnchorBlockEntity> out = new ArrayList<>();
		boolean dropped = entity.guyAnchors().removeIf(anchor -> {
			if (!level.isLoaded(anchor)) {
				return false;
			}
			if (level.getBlockEntity(anchor) instanceof GuyAnchorBlockEntity guy && foot.equals(guy.pole())) {
				out.add(guy);
				return false;
			}
			return true;
		});
		if (dropped) {
			entity.setChanged();
		}
		return out;
	}

	/** The pole's server tick (its main cell's): looks again every two seconds. */
	public static void tick(Level level, BlockPos foot, BlockState state, BigPartBlockEntity entity) {
		if (!(level instanceof ServerLevel server) || !(state.getBlock() instanceof WoodPoleBlock pole) || (level.getGameTime() + foot.hashCode()) % CHECK_TICKS != 0) {
			return;
		}
		Reading reading = read(server, foot, pole, guys(server, foot, entity));
		entity.setPoleReading(reading);
		int lean = state.getValue(WoodPoleBlock.LEAN);
		double rated = pole.classLoadLbf();
		if (reading.left() > rated) {
			entity.addOverloadSeconds(CHECK_TICKS / 20.0);
			double step = testLeanSeconds != null ? testLeanSeconds : Tuning.POLE_LEAN_SECONDS;
			int want = Math.min(WoodPoleBlock.MAX_LEAN, (int) Math.floor(entity.overloadSeconds() / step));
			if (entity.leans() && want > lean) {
				setLean(server, foot, state, pole, want, toward(reading.leftX(), reading.leftZ()));
			}
		} else {
			entity.clearOverload();
			if (lean > 0 && reading.guys() > 0) {
				setLean(server, foot, state, pole, 0, null);   // the guy is pulled up: the pole stands plumb again
			}
		}
	}

	/** The side a horizontal pull points to. */
	static Direction toward(double x, double z) {
		return Math.abs(x) >= Math.abs(z) ? (x >= 0 ? Direction.EAST : Direction.WEST) : (z >= 0 ? Direction.SOUTH : Direction.NORTH);
	}

	/**
	 * Leans every cell of the pole {@code steps} steps toward {@code toward}, or stands it plumb (null). A pole's model leans toward the
	 * side counter-clockwise of its facing, so the facing is turned to suit; the cells all change together, without shape updates.
	 */
	private static void setLean(ServerLevel level, BlockPos foot, BlockState state, WoodPoleBlock pole, int steps, Direction toward) {
		Direction facing = toward == null || !toward.getAxis().isHorizontal() ? state.getValue(BigPartBlock.FACING) : toward.getClockWise();
		for (int piece = 0; piece < pole.height(); piece++) {
			BlockPos cell = BigParts.cell(foot, state, piece);
			BlockState there = level.getBlockState(cell);
			if (there.getBlock() == pole) {
				level.setBlock(cell, there.setValue(WoodPoleBlock.LEAN, steps).setValue(BigPartBlock.FACING, facing), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			}
		}
		dev.wildcatter.Wildcatter.LOGGER.info("The pole at {} {}", foot, steps > 0 ? "leans " + Tuning.POLE_LEAN_DEGREES[steps] + " degrees: overloaded and unguyed" : "is guyed plumb");
	}

	/** The Power Meter's reading of a pole. */
	public static String describe(Level level, BlockPos foot, BlockState state, WoodPoleBlock pole, BigPartBlockEntity entity) {
		Reading r = read(level, foot, pole, guys(level, foot, entity));
		int lean = state.getValue(WoodPoleBlock.LEAN);
		String name = pole.label() + " wood pole";
		String guys = r.guys() == 0 ? "" : String.format("; %d %s, holding up to %,.0f lbf", r.guys(), r.guys() == 1 ? "guy" : "guys", r.guyLbf());
		String state2;
		if (r.left() > pole.classLoadLbf()) {
			state2 = String.format("%s: %,.0f lbf of line pull on a class %d pole rated %,.0f lbf", r.guys() == 0 ? "UNGUYED" : "UNDER-GUYED", r.left(),
					pole.poleClass(), pole.classLoadLbf());
		} else if (r.line() < 1) {
			state2 = String.format("no line pull (rated %,.0f lbf at the top)", pole.classLoadLbf());
		} else {
			state2 = String.format("%,.0f lbf of line pull, %,.0f lbf of it on the pole (rated %,.0f lbf): holding", r.line(), r.left(), pole.classLoadLbf());
		}
		String leaning = lean > 0 ? String.format("; LEANING %.1f degrees", Tuning.POLE_LEAN_DEGREES[lean]) : "";
		return name + ": " + state2 + guys + leaning;
	}
}
