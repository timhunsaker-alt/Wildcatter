package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.crane.Siting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a crane's crawlers or ring stand on (docs/questions/plant-crane-gaps.md, 2): each block read as the ground it stands for,
 * with that ground's allowable bearing ({@link Siting.Ground}).
 */
public final class CraneGround {
	private CraneGround() {
	}

	public static Siting.Ground at(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.is(WildcatterBlocks.CRANE_RAIL)) {
			return Siting.Ground.ROCK;
		}
		if (!state.getFluidState().isEmpty() || !state.isFaceSturdy(level, pos, Direction.UP)) {
			return Siting.Ground.NONE;
		}
		if (state.is(Blocks.GRAVEL) || state.is(Blocks.SUSPICIOUS_GRAVEL)) {
			return Siting.Ground.GRAVEL;
		}
		if (state.is(BlockTags.SAND) || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)) {
			return Siting.Ground.SAND;
		}
		if (state.is(BlockTags.DIRT) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT_PATH)
				|| state.is(Blocks.PACKED_MUD)) {
			return Siting.Ground.CLAY;
		}
		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		if (id.contains("sandstone") || id.contains("cobblestone") || id.contains("terracotta") || id.equals("calcite") || id.equals("dripstone_block")
				|| id.equals("netherrack")) {
			return Siting.Ground.SEDIMENTARY;
		}
		if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || (id.contains("concrete") && !id.contains("powder"))
				|| id.contains("stone") || id.contains("brick") || id.contains("deepslate") || id.contains("granite") || id.contains("diorite")
				|| id.contains("andesite") || id.contains("tuff") || id.contains("basalt") || id.contains("obsidian") || id.equals("bedrock")
				|| id.contains("foundation")) {
			return Siting.Ground.ROCK;
		}
		return Siting.Ground.SEDIMENTARY;   // any other solid block: a built surface on compacted fill
	}
}
