package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Christmas tree on a gas well at true size (docs/multiblocks/well-site.md): a 2 1/16 in 5,000 psi tree about 2 m tall, 1 x 2 x 1.
 * The gas comes up under its own pressure and flows to fireboxes touching its bottom cell or its steel pipe.
 */
public class GasWellheadBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(1, 2, 1, 0, 0);

	public GasWellheadBlock(Properties properties) {
		super(properties, FOOTPRINT, "gas_wellhead");
	}

	/** An older world's one-block wellhead grows upward where it has room. */
	@Override
	public boolean growsOnLoad() {
		return true;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new GasWellheadBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.GAS_WELLHEAD_ENTITY, GasWellheadBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof GasWellheadBlockEntity head) {
			player.sendOverlayMessage(head.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
