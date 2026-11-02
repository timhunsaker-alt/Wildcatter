package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The starter plant's boiler at true size (docs/multiblocks/building-gear.md): a small vertical fire-tube boiler about 0.6 m
 * across and 1.4 m tall with its stack, 1 x 2 x 1. It stands on a lit campfire or a firebox; fill it with water. Its pipes and its
 * fire attach at its bottom (main) cell, and a turbine beside that cell draws its steam.
 */
public class BoilerBlock extends BigPartBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final Footprint FOOTPRINT = Footprint.box(1, 2, 1, 0, 0);

	public BoilerBlock(Properties properties) {
		super(properties, FOOTPRINT, "boiler");
		registerDefaultState(defaultBlockState().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new BoilerBlockEntity(pos, state);
	}

	/** An older world's one-block boiler grows upward where it has room. */
	@Override
	public boolean growsOnLoad() {
		return true;
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.BOILER_ENTITY, BoilerBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.is(Items.WATER_BUCKET) && level.getBlockEntity(BigParts.main(pos, state)) instanceof BoilerBlockEntity boiler) {
			if (!level.isClientSide() && boiler.addWater(Tuning.WATER_PER_BUCKET) == Tuning.WATER_PER_BUCKET) {
				if (!player.getAbilities().instabuild) {
					player.setItemInHand(hand, new ItemStack(Items.BUCKET));
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(BigParts.main(pos, state)) instanceof BoilerBlockEntity boiler) {
			player.sendOverlayMessage(boiler.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
