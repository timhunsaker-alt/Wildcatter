package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A 480 V open-transition automatic transfer switch, 1 x 2 x 1 on a wall (docs/multiblocks/ats.md). NORMAL comes in on the top face of
 * its top cell, EMERGENCY on the back face of its top cell, and the LOAD leaves the bottom face of its main cell. While it wants the
 * standby source it is a redstone source on every side: the engine-start contact.
 * <ul>
 * <li>Right-click reads it.</li>
 * <li>Sneak and right-click with an empty hand starts or ends a test (normal treated as failed).</li>
 * </ul>
 */
public class TransferSwitchBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(1, 2, 1, 0, 0);

	public TransferSwitchBlock(Properties properties) {
		super(properties, FOOTPRINT, "ats_480");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new TransferSwitchBlockEntity(pos, state);
	}

	private static @Nullable TransferSwitchBlockEntity entity(BlockGetter level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(BigParts.main(pos, state)) instanceof TransferSwitchBlockEntity entity ? entity : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		TransferSwitchBlockEntity ats = entity(level, pos, state);
		if (stack.getItem() instanceof PowerMeterItem && player.isShiftKeyDown() && ats != null) {
			if (!level.isClientSide()) {
				if (pos.equals(BigParts.main(pos, state))) {
					ats.nextTimers();       // the bottom cell: the controller's timer set
				} else {
					ats.nextTransition();   // the top cell: open, in-phase or closed transition
				}
				player.sendOverlayMessage(ats.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		TransferSwitchBlockEntity entity = entity(level, pos, state);
		if (level.isClientSide() || entity == null) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			entity.setTest(!entity.test());
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 1.5F);
			CircuitManager.markDirty(level);
		}
		player.sendOverlayMessage(entity.inspect());
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		CircuitManager.markDirty(level);
	}

	// ---- the engine-start contact ------------------------------------------------------------------------------------------------
	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		TransferSwitchBlockEntity entity = entity(level, pos, state);
		return entity != null && entity.engineStart() ? 15 : 0;
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return getSignal(state, level, pos, direction);
	}
}
