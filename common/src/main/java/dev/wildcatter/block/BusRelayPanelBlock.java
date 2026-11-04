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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The generator bus's relay panel at true size, 1 x 3 x 1 (docs/multiblocks/gen-bus-protection.md): 87B and its lockout, 51V on
 * each generator breaker, 50/51 on each feeder breaker, for the 13.8 kV bus duct it stands against. Right-click reads it; after a
 * trip it resets the lockout and closes again what it opened.
 */
public class BusRelayPanelBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(1, 3, 1, 0, 0);

	public BusRelayPanelBlock(Properties properties) {
		super(properties, FOOTPRINT, "bus_relay_panel");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new BusRelayPanelBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof BusRelayPanelBlockEntity panel) {
			if (panel.hasTripped()) {
				panel.reset();
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, 0.9F);
			}
			player.sendOverlayMessage(panel.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
