package dev.wildcatter.ore;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

/**
 * Where the salt dome feature runs, by the world's rule {@code saltDomeGrid192}. Off (a world from before Phase 15), exactly the
 * placement the dome had before: a rarity filter of 1 in 150, a random spot in the chunk, y 0 and the biome check, run in that order
 * on the same random source, so the same chunks get the same domes they always would have. On, once in every chunk the feature's
 * biomes reach, at the chunk's origin: the grid dome decides itself where it is ({@link SaltDomeFeature}).
 */
public final class SaltDomePlacement implements PlacementModifier {
	public static final SaltDomePlacement INSTANCE = new SaltDomePlacement();
	public static final MapCodec<SaltDomePlacement> CODEC = MapCodec.unit(() -> INSTANCE);

	/** Master's placement of the dome (tools/make_assets.py wrote it as these four, in this order). */
	private static final List<PlacementModifier> LEGACY = List.of(RarityFilter.onAverageOnceEvery(150), InSquarePlacement.spread(),
			HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(0)), BiomeFilter.biome());

	private SaltDomePlacement() {
	}

	@Override
	public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
		if (dev.wildcatter.Rules.saltDomeGrid()) {
			output.accept(origin);
		} else {
			legacy(context, random, origin, 0, output);
		}
	}

	/** As the game's feature placer runs a list of modifiers: each one's positions collected, then each passed on in order. */
	private static void legacy(PlacementContext context, RandomSource random, BlockPos pos, int index, Consumer<BlockPos> output) {
		if (index == LEGACY.size()) {
			output.accept(pos);
			return;
		}
		List<BlockPos> next = new ArrayList<>();
		LEGACY.get(index).modify(context, random, pos, next::add);
		for (BlockPos p : next) {
			legacy(context, random, p, index + 1, output);
		}
	}

	@Override
	public MapCodec<? extends PlacementModifier> codec() {
		return CODEC;
	}
}
