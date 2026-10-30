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

/** A 110 V pumpjack on top of an oil well. Right-click it with a bucket to fill one with crude. */
public class PumpjackBlock extends BaseEntityBlock {
	public PumpjackBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PumpjackBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.PUMPJACK_ENTITY, PumpjackBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		if (stack.is(Items.BUCKET) && level.getBlockEntity(pos) instanceof PumpjackBlockEntity jack) {
			if (!level.isClientSide() && jack.takeUnits(Tuning.CRUDE_BUCKET_UNITS)) {
				ItemStack crude = new ItemStack(WildcatterItems.CRUDE_OIL_BUCKET);
				stack.shrink(1);
				if (stack.isEmpty()) {
					player.setItemInHand(hand, crude);
				} else if (!player.getInventory().add(crude)) {
					net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), crude);
				}
			} else if (!level.isClientSide()) {
				player.sendOverlayMessage(jack.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PumpjackBlockEntity jack) {
			if (!Shock.touch((ServerLevel) level, pos, player, Voltage.LOW, jack.isLive())) {
				player.sendOverlayMessage(jack.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
