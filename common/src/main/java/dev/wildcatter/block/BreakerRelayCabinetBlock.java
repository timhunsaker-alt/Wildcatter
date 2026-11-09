package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A standalone breaker's relay cabinet at true size, 1 x 2 x 1 (docs/multiblocks/breaker-relay-cabinet.md): 50/51 on the 12.47 kV or
 * 138 kV yard breaker it touches, and 81U on a 12.47 kV one. Right-click reads it, and after a trip resets it and recloses the
 * breaker; sneak and right-click with the Power Meter steps the relay's selected setting, and right-click with it on the upper cell
 * moves the relay's cursor to the next setting (pickup, curve, dial, 50, 79 shots).
 */
public class BreakerRelayCabinetBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(1, 2, 1, 0, 0);

	public BreakerRelayCabinetBlock(Properties properties) {
		super(properties, FOOTPRINT, "breaker_relay_cabinet");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new BreakerRelayCabinetBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof PowerMeterItem && player.isShiftKeyDown() && level.getBlockEntity(main(pos, state)) instanceof BreakerRelayCabinetBlockEntity cabinet) {
			if (!level.isClientSide()) {
				cabinet.nextPickup();
				player.sendOverlayMessage(cabinet.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.getItem() instanceof PowerMeterItem && pos.getY() > main(pos, state).getY() && level.getBlockEntity(main(pos, state)) instanceof BreakerRelayCabinetBlockEntity cabinet) {
			if (!level.isClientSide()) {
				cabinet.nextSetting();   // the relay in the upper cell: its front-panel cursor
				player.sendOverlayMessage(cabinet.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof BreakerRelayCabinetBlockEntity cabinet) {
			if (cabinet.hasTripped()) {
				cabinet.reset();
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, 0.9F);
			}
			player.sendOverlayMessage(cabinet.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
