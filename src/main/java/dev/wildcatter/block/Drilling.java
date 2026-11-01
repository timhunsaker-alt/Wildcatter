package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What both rigs do to the hole (docs/multiblocks/well-site.md, land-rig.md): find the field under it, case it a block at a
 * time, and set the wellhead on a strike.
 */
public final class Drilling {
	private Drilling() {
	}

	/** The field a hole at this column reaches first, or null. */
	public static @Nullable Reservoir field(ServerLevel level, BlockPos hole) {
		return Reservoir.at(ReservoirData.seed(level), hole.getX(), hole.getZ());
	}

	/**
	 * The roll-front under this column, or null (docs/multiblocks/isr-wellfield.md). Only desert and badlands sandstone carries
	 * one; the biome at the hole decides.
	 */
	public static dev.wildcatter.well.@Nullable RollFront rollFront(ServerLevel level, BlockPos hole) {
		dev.wildcatter.well.RollFront front = dev.wildcatter.well.RollFront.at(ReservoirData.seed(level), hole.getX(), hole.getZ());
		if (front == null) {
			return null;
		}
		net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(hole);
		return biome.is(net.minecraft.world.level.biome.Biomes.DESERT) || biome.is(net.minecraft.tags.BiomeTags.IS_BADLANDS) ? front : null;
	}

	/** True if a hole here reaches the roll-front before any oil or gas field: the rig completes it as an ISR well. */
	public static boolean isrFirst(ServerLevel level, BlockPos hole) {
		dev.wildcatter.well.RollFront front = rollFront(level, hole);
		Reservoir field = field(level, hole);
		return front != null && (field == null || front.bottomY() > field.topY());
	}

	/** The height the hole stops at: through the base of a roll-front, the top of its field, or {@code dryY} with neither. */
	public static int goalY(ServerLevel level, BlockPos hole, int dryY) {
		if (isrFirst(level, hole)) {
			return rollFront(level, hole).bottomY();
		}
		Reservoir field = field(level, hole);
		return field != null ? field.topY() : dryY;
	}

	/** What a gamma log of the hole shows: the ore zone of a roll-front, or background to the bottom. */
	public static String gammaLog(ServerLevel level, BlockPos hole, int bottomY) {
		dev.wildcatter.well.RollFront front = rollFront(level, hole);
		return front != null && bottomY <= front.topY() ? front.gammaLog(hole.getX(), hole.getZ())
				: "GAMMA LOG: background to y " + bottomY + ", no uranium";
	}

	/** Completes the hole as an ISR well through a roll-front: the wellhead on the casing, the gamma log in it. */
	public static void setIsrWell(ServerLevel level, BlockPos hole, String log) {
		level.setBlock(hole, WildcatterBlocks.ISR_WELLHEAD.defaultBlockState(), 3);
		if (level.getBlockEntity(hole) instanceof IsrWellheadBlockEntity well) {
			well.setLog(log);
		}
	}

	/** Cases the next block down, {@code depth} blocks already cased below {@code hole}; false if the rock there cannot be cut. */
	public static boolean caseNext(ServerLevel level, BlockPos hole, int depth) {
		BlockPos next = hole.below(depth + 1);
		BlockState rock = level.getBlockState(next);
		if (next.getY() <= level.getMinY() || rock.getDestroySpeed(level, next) < 0) {
			return false;
		}
		level.setBlock(next, WildcatterBlocks.CASING.defaultBlockState(), 3);
		return true;
	}

	/** Sets the wellhead for this kind of field on the hole: an oil wellhead, or a Christmas tree grown into its two cells. */
	public static void setWellhead(ServerLevel level, BlockPos hole, Reservoir.Kind kind, Direction facing) {
		if (kind == Reservoir.Kind.OIL) {
			level.setBlock(hole, WildcatterBlocks.OIL_WELLHEAD.defaultBlockState().setValue(OilWellheadBlock.FACING, facing), 3);
		} else {
			BlockState tree = WildcatterBlocks.GAS_WELLHEAD.defaultBlockState().setValue(GasWellheadBlock.FACING, facing);
			level.setBlock(hole, tree, 3);
			dev.wildcatter.multiblock.BigParts.grow(level, hole, tree);
		}
	}

	/** The bang and smoke of a strike, and the news to everyone near. */
	public static void announce(ServerLevel level, BlockPos at, Component news) {
		level.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.7F, 0.8F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 30, 0.5, 0.3, 0.5, 0.05);
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(at.getX(), at.getY(), at.getZ()) < 64 * 64) {
				player.sendOverlayMessage(news);
			}
		}
	}

	/** The small rig's last block: it stops this far down. */
	public static boolean tooDeepForSmallRig(int depth) {
		return depth >= Tuning.SMALL_RIG_MAX_DEPTH;
	}
}
