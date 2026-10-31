package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The substation's relay panel, in front of the power transformer (docs/multiblocks/substation.md). It holds the 51 and 50
 * overcurrent relays (their logic runs in the transformer); right-click reads them, and after a trip resets the lockout and
 * closes both breakers again.
 */
public class RelayPanelBlock extends Block {
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

	public RelayPanelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** The power transformer this panel stands in front of. */
	public static PowerTransformerBlockEntity transformer(Level level, BlockPos pos) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (level.getBlockEntity(pos.relative(side)) instanceof PowerTransformerBlockEntity t) {
				return t;
			}
		}
		return null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			PowerTransformerBlockEntity t = transformer(level, pos);
			if (t == null) {
				player.sendOverlayMessage(Component.literal("Relay panel: it must stand in front of a substation's power transformer"));
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
