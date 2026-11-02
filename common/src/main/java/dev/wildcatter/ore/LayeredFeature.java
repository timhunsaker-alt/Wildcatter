package dev.wildcatter.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Flat layers stacked bottom to top, replacing the stone they cut: the chromitite seams with the platinum reef above them
 * (Bushveld), and the bedded rock salt and potash of an evaporite basin (Saskatchewan). A thickness of 0 with no state is
 * a gap of country rock. The edge of the disc is ragged so it does not look stamped.
 */
public record LayeredFeature(List<Layer> layers, int radiusMin, int radiusMax) implements Feature {
	public record Layer(BlockState state, int thickness) {
		public static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockState.CODEC.fieldOf("state").forGetter(Layer::state),
				Codec.INT.fieldOf("thickness").forGetter(Layer::thickness)).apply(i, Layer::new));
	}

	public static final MapCodec<LayeredFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Layer.CODEC.listOf().fieldOf("layers").forGetter(LayeredFeature::layers),
			Codec.INT.fieldOf("radius_min").forGetter(LayeredFeature::radiusMin),
			Codec.INT.fieldOf("radius_max").forGetter(LayeredFeature::radiusMax)).apply(i, LayeredFeature::new));

	@Override
	public MapCodec<LayeredFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int radius = radiusMin + random.nextInt(radiusMax - radiusMin + 1);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		boolean placed = false;
		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dz = -radius - 1; dz <= radius + 1; dz++) {
				double r = radius + (random.nextFloat() - 0.5) * 1.5;
				if (dx * dx + dz * dz > r * r) {
					continue;
				}
				int y = origin.getY();
				for (Layer layer : layers) {
					for (int k = 0; k < layer.thickness(); k++, y++) {
						at.set(origin.getX() + dx, y, origin.getZ() + dz);
						if (!layer.state().isAir() && level.getBlockState(at).is(BlockTags.BASE_STONE_OVERWORLD)) {
							setBlock(level, at, layer.state());
							placed = true;
						}
					}
				}
			}
		}
		return placed;
	}
}
