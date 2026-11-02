package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The substation's relay panel at true size, an outdoor relay cabinet 2 x 2 x 1 north of the power transformer
 * (docs/multiblocks/substation.md). It holds the 51 and 50 overcurrent relays, the lockout and the meters (their logic runs in
 * the transformer); right-click reads them, and after a trip resets the lockout and closes both breakers again.
 */
public class RelayPanelBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(2, 2, 1, 0, 0);

	public RelayPanelBlock(Properties properties) {
		super(properties, FOOTPRINT, "relay_panel");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new BigPartBlockEntity(pos, state);
	}

	/** The power transformer of the yard this panel stands in: where the yard's layout puts it from the panel's place. */
	public static PowerTransformerBlockEntity transformer(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof RelayPanelBlock panel)) {
			return null;
		}
		BlockPos main = BigParts.main(pos, state);
		BlockPos t = SubstationLayout.layout().controllerFrom(WildcatterBlocks.RELAY_PANEL, main, panel.facing(state), 0);
		return level.getBlockEntity(t) instanceof PowerTransformerBlockEntity transformer ? transformer : null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			PowerTransformerBlockEntity t = transformer(level, pos);
			if (t == null) {
				player.sendOverlayMessage(Component.literal("Relay panel: it must stand in a substation yard, north of the power transformer"));
			} else {
				t.resetRelays();
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, 0.9F);
				player.sendOverlayMessage(t.inspect());
				CircuitManager.markDirty(level);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
