package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A branch-circuit cable (docs/multiblocks/panelboards.md): armored cable (type MC; SWA under IEC) carrying A, B, C, N and G, laid
 * along one axis like a conduit run. It joins only at its two ends, so the runs out of neighbouring panel gutters never touch.
 * Placed, it runs along the axis of the face it was put against.
 */
public class BranchCableBlock extends MultiCableBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
	private static final VoxelShape X = Block.box(0, 6.5, 6.5, 16, 9.5, 9.5);
	private static final VoxelShape Y = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);
	private static final VoxelShape Z = Block.box(6.5, 6.5, 0, 9.5, 9.5, 16);

	public BranchCableBlock(Properties properties, Voltage voltage) {
		super(properties, voltage, false);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(AXIS, context.getClickedFace().getAxis());
	}

	/** It joins only at its two ends. */
	@Override
	public boolean joins(BlockState state, Direction face) {
		return face.getAxis() == state.getValue(AXIS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> X;
			case Y -> Y;
			case Z -> Z;
		};
	}
}
