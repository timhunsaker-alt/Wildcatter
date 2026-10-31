package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

/** A 110 V drilling rig. Wire it up and it sinks a cased hole straight down; if it finds a field it becomes the wellhead. */
public class DrillingRigBlock extends BaseEntityBlock {
	public DrillingRigBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DrillingRigBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, WildcatterBlocks.DRILLING_RIG_ENTITY, level.isClientSide() ? (l, p, st, be) -> dev.wildcatter.kinetic.RunningSound.clientTick(be) : DrillingRigBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DrillingRigBlockEntity rig) {
			if (player.isShiftKeyDown()) {
				player.sendOverlayMessage(rig.swapLeads());
				dev.wildcatter.circuit.CircuitManager.markDirty(level);
			} else if (!Shock.touch((ServerLevel) level, pos, player, Voltage.INDUSTRIAL, rig.isLive())) {
				player.sendOverlayMessage(rig.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
