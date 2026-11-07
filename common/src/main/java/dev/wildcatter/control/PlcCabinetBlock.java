package dev.wildcatter.control;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** A floor-standing PLC cabinet. A module clicked on it seats; an empty hand opens its screen, and sneaking pulls the last module seated. */
public class PlcCabinetBlock extends BigPartBlock {

	public PlcCabinetBlock(Properties properties, Footprint footprint, String shapeName) {
		super(properties, footprint, shapeName);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new PlcCabinetBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, dev.wildcatter.WildcatterBlocks.PLC_CABINET_ENTITY, PlcCabinetBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		if (stack.getItem() instanceof PlcModuleItem module) {
			if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof PlcCabinetBlockEntity cabinet) {
				String refused = cabinet.seat(module.type());
				if (refused == null) {
					stack.consume(1, player);
				}
				player.sendOverlayMessage(Component.literal(refused != null ? refused : cabinet.inspect().getString()));
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof PlcCabinetBlockEntity cabinet) {
			if (player.isShiftKeyDown()) {
				ItemStack out = cabinet.unseatLast();
				if (out.isEmpty()) {
					player.sendOverlayMessage(Component.literal("The rail is empty"));
				} else {
					player.getInventory().placeItemBackInInventory(out, net.minecraft.util.Prediction.SERVER_ONLY);
				}
			} else if (player instanceof ServerPlayer server) {
				cabinet.open(server);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
