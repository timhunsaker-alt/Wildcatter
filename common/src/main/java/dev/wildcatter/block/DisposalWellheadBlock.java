package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.PowerMeterItem;
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
 * A saltwater disposal wellhead (docs/multiblocks/lease.md): casing head, tubing head and the injection wing with its check valve,
 * about 1 m tall. It goes in the hole cell of a dry hole, on the top casing, and takes the produced water an injection pump pushes
 * into it; the rock below takes it all. It has no logic of its own: the pump looks for it on its discharge run.
 */
public class DisposalWellheadBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

	public DisposalWellheadBlock(Properties properties) {
		super(properties.noOcclusion());
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!context.getLevel().getBlockState(context.getClickedPos().below()).is(WildcatterBlocks.CASING)) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.literal("A disposal wellhead goes on a well's casing: the hole of a dry hole, its rig broken away"));
			}
			return null;
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	private static Component describe() {
		return Component.literal("Saltwater disposal wellhead: on the casing of a dry hole. An injection pump's discharge on steel pipe pushes produced water down it");
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (PowerMeterItem.readIfMeter(stack, level, pos, player) != null) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(describe());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(describe());
		}
		return InteractionResult.SUCCESS;
	}
}
