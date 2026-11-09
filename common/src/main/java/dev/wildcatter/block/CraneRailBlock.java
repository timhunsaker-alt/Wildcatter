package dev.wildcatter.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A metre of heavy crane rail on its steel beam and timber mat (docs/multiblocks/ring-crane.md, "Relocation"), laid flush with the
 * ground. A row of them under the ring crane's slewing centre is the track it travels along between lifting positions. It reads as
 * rock to a crane's ground check.
 */
public class CraneRailBlock extends Block {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	public CraneRailBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
	}
}
