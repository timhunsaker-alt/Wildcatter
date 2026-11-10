package dev.wildcatter.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildcatter.Tuning;
import dev.wildcatter.well.ReservoirData;
import dev.wildcatter.well.SaltDome;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A Gulf Coast salt dome (docs/ores.md, salt-dome-oil.md): a column of rock salt pushed up from the deep, under a cap of calcite
 * where bacteria left native sulfur, and a low mound over it at the surface. Where the domes are is a seed grid ({@link SaltDome}),
 * so the oil on their flanks can be found from the seed alone; the feature runs once in every chunk and draws the part of its
 * cell's dome that lies in that chunk, if the dome's centre is in a dome biome. Placed by hand (/place feature), it draws the part
 * of the grid dome in the chunk it is placed in.
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

	/**
	 * With the world rule {@code saltDomeGrid192} on (new worlds), the grid dome; off (worlds from before Phase 15), the dome as it was
	 * placed before: one in about 150 chunks of a dome biome, at a random spot, with the same random numbers ({@link SaltDomePlacement}),
	 * so an existing world's new chunks get the domes they would have had.
	 */
	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		return dev.wildcatter.Rules.saltDomeGrid() ? placeGrid(level, random, origin) : placeLegacy(level, random, origin);
	}

	/** The dome as placed before Phase 15 (master's code, unchanged). */
	private boolean placeLegacy(WorldGenLevel level, RandomSource random, BlockPos origin) {
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

	/** 15a: the part of this chunk's grid dome that lies in the chunk. */
	private boolean placeGrid(WorldGenLevel level, RandomSource random, BlockPos origin) {
		int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
		long seed = ReservoirData.seed(level, x0 + 8, z0 + 8);
		SaltDome dome = SaltDome.inCell(seed, Math.floorDiv(x0 + 8, Tuning.DOME_CELL), Math.floorDiv(z0 + 8, Tuning.DOME_CELL));
		if (dome == null || Math.abs(dome.centerX() - (x0 + 8)) > Tuning.DOME_MAX_RADIUS + 12 || Math.abs(dome.centerZ() - (z0 + 8)) > Tuning.DOME_MAX_RADIUS + 12
				|| !ReservoirData.site(level).test(dome)) {
			return false;
		}
		BlockState salt = OreBlocks.block(Ore.HALITE).defaultBlockState();
		BlockState sulfur = OreBlocks.block(Ore.SULFUR).defaultBlockState();
		BlockState cap = Blocks.CALCITE.defaultBlockState();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		boolean any = false;
		for (int x = x0; x < x0 + 16; x++) {
			for (int z = z0; z < z0 + 16; z++) {
				for (int y = Tuning.DOME_BOTTOM_Y; y <= dome.capTop(); y++) {
					boolean inCap = dome.inCap(x, y, z);
					if (!inCap && !dome.inSalt(x, y, z)) {
						continue;
					}
					at.set(x, y, z);
					if (replaceable(level.getBlockState(at))) {
						level.setBlock(at, inCap ? (random.nextFloat() < sulfurChance ? sulfur : cap) : salt, 2);
						any = true;
					}
				}
				mound(level, dome, at, x, z);
			}
		}
		return any;
	}

	/** The low mound over the dome: the surface raised a block or two, its top block kept (grass, sand, mud). */
	private static void mound(WorldGenLevel level, SaltDome dome, BlockPos.MutableBlockPos at, int x, int z) {
		int h = dome.moundHeight(x, z);
		if (h == 0) {
			return;
		}
		// the first air above the ground (a placed-by-hand dome is in a finished chunk, which keeps no worldgen heightmap)
		int surface = level.getHeight(level instanceof net.minecraft.server.level.ServerLevel ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG, x, z);
		at.set(x, surface - 1, z);
		BlockState top = level.getBlockState(at);
		if (!(top.is(BlockTags.DIRT) || top.is(BlockTags.SAND)) || !level.getBlockState(at.above()).getFluidState().isEmpty()) {
			return;   // only on soil or sand under open air: no mound in a lake, on stone or on a placed block
		}
		BlockState under = top.is(BlockTags.SAND) ? top : Blocks.DIRT.defaultBlockState();
		for (int k = 0; k < h; k++) {
			level.setBlock(at.set(x, surface - 1 + k, z), under, 2);
		}
		level.setBlock(at.set(x, surface - 1 + h, z), top, 2);
	}

	/** Rock and soil, never air, water, bedrock or a placed block. */
	static boolean replaceable(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty() && !state.is(Blocks.BEDROCK) && !state.hasBlockEntity();
	}
}
