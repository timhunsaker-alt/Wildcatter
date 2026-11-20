package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import dev.wildcatter.well.CavernData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * A salt cavern in the world (docs/multiblocks/oilfield-follow-ons.md, 23c): which rock salt block the water reaches next, the walls it
 * must keep (2 blocks of salt to the dome's flank, 3 under the caprock), the sonar's record, a breach and the sinkhole a roof breach opens.
 * The walls are read off the blocks themselves, so a placed salt column and a dome's salt are alike.
 */
public final class Caverns {
	private Caverns() {
	}

	static boolean salt(BlockState state) {
		return state.is(OreBlocks.block(Ore.HALITE));
	}

	/** Salt, or this cavern: what a wall may be made of. */
	private static boolean wall(Level level, CavernData.Hole hole, BlockPos at) {
		return hole.contains(at) || salt(level.getBlockState(at)) || level.getBlockState(at).is(WildcatterBlocks.CASING);   // the well's own casing string
	}

	/** True if leaching {@code at} keeps 2 blocks of salt to the flank all round it. */
	static boolean flankOk(Level level, CavernData.Hole hole, BlockPos at) {
		int w = Tuning.CAVERN_FLANK_WALL;
		for (int dx = -w; dx <= w; dx++) {
			for (int dz = -w; dz <= w; dz++) {
				if (!wall(level, hole, at.offset(dx, 0, dz))) {
					return false;
				}
			}
		}
		return true;
	}

	/** True if leaching {@code at} keeps 3 blocks of salt over it (the roof under the caprock). */
	static boolean roofOk(Level level, CavernData.Hole hole, BlockPos at) {
		for (int dy = 1; dy <= Tuning.CAVERN_ROOF_WALL; dy++) {
			if (!wall(level, hole, at.above(dy))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The next rock salt block the water reaches: the hole's bottom, then a salt block against the cavern, upward first without a blanket
	 * and outward first with one, nearest the hole's axis. One that would thin a wall past its limit is taken only with the rule
	 * {@code cavernStability} on and nothing else left (and then breaches the cavern); with the rule off the cavern stops at its limits.
	 */
	static @Nullable BlockPos next(Level level, CavernData.Hole hole, BlockPos start, boolean blanket) {
		if (hole.breached) {
			return null;
		}
		if (hole.blocks.isEmpty()) {
			return salt(level.getBlockState(start)) ? start : null;
		}
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		boolean bestOk = false;
		boolean stability = dev.wildcatter.Rules.cavernStability();
		for (long l : hole.blocks) {
			BlockPos c = BlockPos.of(l);
			for (Direction d : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
				BlockPos n = c.relative(d);
				if (hole.contains(n) || !salt(level.getBlockState(n)) || n.distManhattan(start) > 24) {
					continue;
				}
				boolean ok = flankOk(level, hole, n) && roofOk(level, hole, n);
				if (!ok && !stability) {
					continue;
				}
				double axis = Math.hypot(n.getX() - start.getX(), n.getZ() - start.getZ());
				int dy = n.getY() - c.getY();
				// without a blanket the fresh water rises and eats the roof first; with one it spreads out under the blanket
				double score = (blanket ? (dy == 0 ? 0 : dy < 0 ? 40 : 80) : (dy > 0 ? 0 : dy == 0 ? 40 : 80)) + axis + (blanket ? 0 : -(n.getY() - start.getY()) * 0.5);
				if ((ok && !bestOk) || (ok == bestOk && score < bestScore)) {
					best = n;
					bestScore = score;
					bestOk = ok;
				}
			}
		}
		return best;
	}

	/** {volume m3, height, width, least salt to the flank, least salt over the roof} for the sonar and the readouts. */
	static int[] record(Level level, CavernData.Hole hole) {
		if (hole.blocks.isEmpty()) {
			return new int[] {0, 0, 0, 0, 0};
		}
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE, minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		int flank = 9, roof = 9;
		for (long l : hole.blocks) {
			BlockPos c = BlockPos.of(l);
			minY = Math.min(minY, c.getY());
			maxY = Math.max(maxY, c.getY());
			minX = Math.min(minX, c.getX());
			maxX = Math.max(maxX, c.getX());
			minZ = Math.min(minZ, c.getZ());
			maxZ = Math.max(maxZ, c.getZ());
			for (Direction d : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
				int k = 0;
				BlockPos at = c.relative(d);
				while (k < 9 && !hole.contains(at) && salt(level.getBlockState(at))) {
					k++;
					at = at.relative(d);
				}
				if (!hole.contains(at)) {
					flank = Math.min(flank, k);
				}
			}
			int k = 0;
			BlockPos at = c.above();
			if (level.getBlockState(at).is(WildcatterBlocks.CASING)) {
				continue;   // under the well's own casing string
			}
			while (k < 9 && !hole.contains(at) && salt(level.getBlockState(at))) {
				k++;
				at = at.above();
			}
			if (!hole.contains(at)) {
				roof = Math.min(roof, k);
			}
		}
		return new int[] {hole.volume(), maxY - minY + 1, Math.max(maxX - minX, maxZ - minZ) + 1, flank, roof};
	}

	static String recordText(Level level, CavernData.Hole hole) {
		int[] r = record(level, hole);
		if (r[0] == 0) {
			return "no cavern yet";
		}
		boolean thin = r[3] < Tuning.CAVERN_THIN_WALL || r[4] < Tuning.CAVERN_THIN_WALL;
		return String.format("cavern %,d m3, %d m high, %d m across, salt %d m to the flank and %d m over the roof%s%s", r[0], r[1], r[2], Math.min(r[3], 9), Math.min(r[4], 9),
				thin ? ": WALL THIN" : "", hole.breached ? ": BREACHED" : "");
	}

	/** A flank breach: formation water floods the cavern and its gas is lost. */
	static void floodBreach(ServerLevel level, CavernData.Hole hole, BlockPos head) {
		hole.breached = true;
		hole.gas = 0;
		level.playSound(null, head, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 0.4F);
		Drilling.announce(level, head, net.minecraft.network.chat.Component.literal("CAVERN BREACHED through the flank: formation water floods it, its gas is lost"));
	}

	/** A roof heaving: cracks at the surface over the cavern for a game day, then the sinkhole. */
	static void heave(ServerLevel level, CavernData.Hole hole, BlockPos head) {
		if (hole.heaveAt < 0) {
			hole.heaveAt = level.getGameTime() + Tuning.CAVERN_HEAVE_TICKS;
			Drilling.announce(level, head, net.minecraft.network.chat.Component.literal("THE CAVERN'S ROOF IS HEAVING: cracks at the surface; it will fall within a day"));
		}
	}

	/** Each second: the cracks over a heaving roof, and its fall. */
	static void tickHeave(ServerLevel level, CavernData.Hole hole, BlockPos head) {
		if (hole.heaveAt < 0 || hole.breached) {
			return;
		}
		List<BlockPos> cols = columns(hole);
		for (int i = 0; i < Math.min(6, cols.size()); i++) {
			BlockPos c = cols.get(level.getRandom().nextInt(cols.size()));
			BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, c);
			level.sendParticles(ParticleTypes.POOF, top.getX() + 0.5, top.getY() + 0.1, top.getZ() + 0.5, 3, 0.3, 0.05, 0.3, 0.01);
		}
		if (level.getGameTime() >= hole.heaveAt) {
			sinkhole(level, hole, head);
		}
	}

	private static List<BlockPos> columns(CavernData.Hole hole) {
		List<BlockPos> out = new ArrayList<>();
		java.util.Set<Long> seen = new java.util.HashSet<>();
		for (long l : hole.blocks) {
			BlockPos c = BlockPos.of(l);
			if (seen.add(BlockPos.asLong(c.getX(), 0, c.getZ()))) {
				out.add(c);
			}
		}
		return out;
	}

	/** The roof falls: the ground over the cavern drops into it and water pools in the hole it leaves. The well is lost. */
	static void sinkhole(ServerLevel level, CavernData.Hole hole, BlockPos head) {
		hole.breached = true;
		hole.gas = 0;
		hole.heaveAt = -1;
		for (BlockPos c : columns(hole)) {
			int top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, c).getY();
			for (int y = c.getY(); y < top; y++) {
				BlockPos at = new BlockPos(c.getX(), y, c.getZ());
				level.removeBlockEntity(at);
				level.setBlock(at, Blocks.WATER.defaultBlockState(), 2 | 16);
			}
		}
		level.playSound(null, head, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 3.0F, 0.3F);
		Drilling.announce(level, head, net.minecraft.network.chat.Component.literal("SINKHOLE: the cavern's roof has fallen in; the ground over it has dropped and the well is lost"));
	}

	/** The sonar's outline: a particle in each leached block. */
	static void outline(ServerLevel level, CavernData.Hole hole) {
		int n = 0;
		for (long l : hole.blocks) {
			if (n++ > 600) {
				break;
			}
			BlockPos c = BlockPos.of(l);
			level.sendParticles(ParticleTypes.END_ROD, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0);
		}
	}

	/** The block under the hole's casing below a wellhead at {@code head}, or null with no casing under it. */
	static @Nullable BlockPos bottom(Level level, BlockPos head) {
		BlockPos at = head.below();
		if (!level.getBlockState(at).is(WildcatterBlocks.CASING)) {
			return null;
		}
		while (level.getBlockState(at).is(WildcatterBlocks.CASING) && at.getY() > level.getMinY()) {
			at = at.below();
		}
		return at;
	}
}
