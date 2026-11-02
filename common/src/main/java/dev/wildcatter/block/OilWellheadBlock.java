package dev.wildcatter.block;

import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A rod-pumping wellhead on an oil well (docs/multiblocks/well-site.md): casing head, tubing head, pumping tee and stuffing box,
 * about 1.1 m tall. A drilling rig leaves one when it strikes oil; a pumpjack clicked on it snaps over it and pumps the field
 * below. It has no logic of its own: steel pipe joins its flowline outlet on any side, and buckets and pipes reach the pumpjack's
 * tank through it.
 */
public class OilWellheadBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

	public OilWellheadBlock(Properties properties) {
		super(properties.noOcclusion());
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** The pumpjack whose horsehead is over this wellhead, or null. */
	public static @Nullable PumpjackBlockEntity pumpjack(BlockGetter level, BlockPos wellhead) {
		return BigParts.entity(level, wellhead.above()) instanceof PumpjackBlockEntity jack && wellhead.equals(jack.wellhead()) ? jack : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		PumpjackBlockEntity jack = pumpjack(level, pos);
		if (jack != null && stack.is(net.minecraft.world.item.Items.BUCKET)) {
			if (!level.isClientSide()) {
				PumpjackBlock.fillBucket(jack, stack, player, hand, level);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			PumpjackBlockEntity jack = pumpjack(level, pos);
			player.sendOverlayMessage(jack != null ? jack.inspect()
					: Component.literal("Oil wellhead: no pumpjack over it. Click it with a pumpjack to set one over the well"));
		}
		return InteractionResult.SUCCESS;
	}
}
