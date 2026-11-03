package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.CircuitManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A metre of bare copper DC bus bar on standoff insulators (docs/multiblocks/dc-bus.md), laid along an axis like a log. It joins
 * any bus bar or DC terminal it touches on any face; it carries no polarity of its own.
 */
public class DcBusBarBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
	// the model is drawn along z, its standoffs on the floor; the blockstate turns it to x, or stands it up against a wall to the south
	private static final VoxelShape X = Block.box(0, 0, 4, 16, 10, 12);
	private static final VoxelShape Y = Block.box(4, 0, 6, 12, 16, 16);
	private static final VoxelShape Z = Block.box(4, 0, 0, 12, 10, 16);

	private final double amps;
	private final double ohmsPerMeter;
	private final String label;

	public DcBusBarBlock(Properties properties, double amps, double ohmsPerMeter, String label) {
		super(properties);
		this.amps = amps;
		this.ohmsPerMeter = ohmsPerMeter;
		this.label = label;
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	/** Its ampacity: past it the bar heats. */
	public double amps() {
		return amps;
	}

	public double ohmsPerMeter() {
		return ohmsPerMeter;
	}

	public String label() {
		return label;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> X;
			case Y -> Y;
			case Z -> Z;
		};
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		CircuitManager.markDirty(level);   // a bar or ground wire set against it may join or fault it
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DcBusBarBlockEntity bar) {
			player.sendOverlayMessage(bar.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DcBusBarBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.DC_BUS_ENTITY, DcBusBarBlockEntity::tick);
	}
}
