package dev.wildcatter.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A Gulf Coast salt dome (docs/ores.md): a column of rock salt pushed up from the deep, under a cap of calcite where bacteria
 * left native sulfur. The column is round and a little wider low down; the cap is one block wider than the salt under it.
 */
public record SaltDomeFeature(int radiusMin, int radiusMax, int bottom, int topMin, int topMax, int caprock, float sulfurChance) implements Feature {
	public static final MapCodec<SaltDomeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("radius_min").forGetter(SaltDomeFeature::radiusMin),
			Codec.INT.fieldOf("radius_max").forGetter(SaltDomeFeature::radiusMax),
			Codec.INT.fieldOf("bottom").forGetter(SaltDomeFeature::bottom),
			Codec.INT.fieldOf("top_min").forGetter(SaltDomeFeature::topMin),
			Codec.INT.fieldOf("top_max").forGetter(SaltDomeFeature::topMax),
			Codec.INT.fieldOf("caprock").forGetter(SaltDomeFeature::caprock),
			Codec.FLOAT.fieldOf("sulfur_chance").forGetter(SaltDomeFeature::sulfurChance)).apply(i, SaltDomeFeature::new));

	@Override
	public MapCodec<SaltDomeFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int radius = radiusMin + random.nextInt(radiusMax - radiusMin + 1);
		int top = topMin + random.nextInt(topMax - topMin + 1);
		BlockState salt = OreBlocks.block(Ore.HALITE).defaultBlockState();
		BlockState sulfur = OreBlocks.block(Ore.SULFUR).defaultBlockState();
		BlockState cap = Blocks.CALCITE.defaultBlockState();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int y = bottom; y <= top + caprock; y++) {
			boolean inCap = y > top;
			// a little wider low down, rounding over at the crest
			double r = inCap ? radius + 1 - (y - top - 1) * 0.5 : radius + Math.max(0, (top - 12 - y)) * 0.04;
			if (!inCap && y > top - 3) {
				r -= (y - (top - 3)) * 0.6;
			}
			int reach = (int) Math.ceil(r);
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					if (dx * dx + dz * dz > r * r) {
						continue;
					}
					at.set(origin.getX() + dx, y, origin.getZ() + dz);
					if (!replaceable(level.getBlockState(at))) {
						continue;
					}
					setBlock(level, at, inCap ? (random.nextFloat() < sulfurChance ? sulfur : cap) : salt);
				}
			}
		}
		return true;
	}

	/** Rock and soil, never air, water, bedrock or a placed block. */
	static boolean replaceable(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty() && !state.is(Blocks.BEDROCK) && !state.hasBlockEntity();
	}
}
