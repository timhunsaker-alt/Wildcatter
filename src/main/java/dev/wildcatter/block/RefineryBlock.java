package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A 110 V refinery. Crude goes in by bucket or from a pumpjack on its gas lines; diesel comes out (by bucket, or down the
 * gas lines to a diesel generator) along with plastic sheets: right-click it with an empty hand to collect those.
 */
public class RefineryBlock extends BaseEntityBlock {
	public RefineryBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RefineryBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, WildcatterBlocks.REFINERY_ENTITY, level.isClientSide() ? (l, p, st, be) -> dev.wildcatter.kinetic.RunningSound.clientTick(be) : RefineryBlockEntity::tick);
	}

	private static void give(Player player, InteractionHand hand, ItemStack used, ItemStack reward) {
		used.shrink(1);
		if (used.isEmpty()) {
			player.setItemInHand(hand, reward);
		} else if (!player.getInventory().add(reward)) {
			net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), reward);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		if (!(level.getBlockEntity(pos) instanceof RefineryBlockEntity refinery)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (stack.is(WildcatterItems.CRUDE_OIL_BUCKET)) {
			if (!level.isClientSide() && refinery.addCrude(Tuning.CRUDE_BUCKET_UNITS)) {
				give(player, hand, stack, new ItemStack(Items.BUCKET));
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.BUCKET)) {
			if (!level.isClientSide() && refinery.takeDiesel(Tuning.DIESEL_BUCKET_UNITS)) {
				give(player, hand, stack, new ItemStack(WildcatterItems.DIESEL_BUCKET));
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RefineryBlockEntity refinery) {
			int sheets = refinery.takePlastic();
			if (sheets > 0) {
				ItemStack plastic = new ItemStack(WildcatterItems.PLASTIC_SHEET, sheets);
				if (!player.getInventory().add(plastic)) {
					net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), plastic);
				}
			} else if (!Shock.touch((ServerLevel) level, pos, player, Voltage.LOW, refinery.isLive())) {
				player.sendOverlayMessage(refinery.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
