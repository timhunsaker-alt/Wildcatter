package dev.wildcatter.control;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import org.jspecify.annotations.Nullable;

/** A remote I/O drop: a cabinet with a communications adapter and no CPU. A module clicked on it seats; an empty hand steps its rack number, and sneaking pulls the last module. */
public class RemoteIoDropBlock extends BigPartBlock {
	public RemoteIoDropBlock(Properties properties, Footprint footprint, String shapeName) {
		super(properties, footprint, shapeName);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new RemoteIoDropBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, dev.wildcatter.WildcatterBlocks.REMOTE_IO_DROP_ENTITY, RemoteIoDropBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		if (stack.getItem() instanceof PlcModuleItem module) {
			if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof RemoteIoDropBlockEntity drop) {
				String refused = drop.seat(module.type());
				if (refused == null) {
					stack.consume(1, player);
				}
				player.sendOverlayMessage(refused != null ? Component.literal(refused) : drop.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof RemoteIoDropBlockEntity drop) {
			if (player.isShiftKeyDown()) {
				ItemStack out = drop.unseatLast();
				if (out.isEmpty()) {
					player.sendOverlayMessage(Component.literal("The rail is empty"));
				} else {
					player.getInventory().placeItemBackInInventory(out, net.minecraft.util.Prediction.SERVER_ONLY);
				}
			} else {
				drop.nextRack();
				player.sendOverlayMessage(drop.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
