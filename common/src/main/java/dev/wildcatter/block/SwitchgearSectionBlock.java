package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
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
import org.jspecify.annotations.Nullable;

/**
 * A 4.16 kV metal-clad switchgear section at true size, 1 x 3 x 3 (docs/multiblocks/mv-switchgear.md): a draw-out vacuum breaker
 * behind its front door, its relay above, the main bus through the middle and the cable compartment at the back. Sections side by
 * side, facing the same way, join their buses into a lineup with one MAIN, where the source comes in, and FEEDER sections, whose
 * cables leave their tops.
 * <ul>
 * <li>Right-click opens or closes the breaker; on a trip it resets the relay and closes.</li>
 * <li>Sneak and right-click with an empty hand racks the breaker: CONNECTED, TEST, DISCONNECTED.</li>
 * <li>Sneak and right-click with the Power Meter steps the frame on the top cell, the 51 pickup on the others.</li>
 * </ul>
 */
public class SwitchgearSectionBlock extends BigPartBlock {
	/** 1 along the lineup, 3 tall, 3 deep; the main cell is the front bottom, the breaker compartment. */
	public static final Footprint FOOTPRINT = Footprint.box(1, 3, 3, 0, 0);

	private final boolean main;

	public SwitchgearSectionBlock(Properties properties, boolean main, String shapeName) {
		super(properties, FOOTPRINT, shapeName);
		this.main = main;
	}

	/** The MAIN section, where the source comes in; otherwise a FEEDER. */
	public boolean isMain() {
		return main;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new SwitchgearSectionBlockEntity(pos, state);
	}

	private static @Nullable SwitchgearSectionBlockEntity section(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(BigParts.main(pos, state)) instanceof SwitchgearSectionBlockEntity section ? section : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		SwitchgearSectionBlockEntity section = section(level, pos, state);
		if (stack.getItem() instanceof PowerMeterItem && player.isShiftKeyDown() && section != null) {
			if (!level.isClientSide()) {
				if (pos.getY() - BigParts.main(pos, state).getY() == 2) {
					section.nextFrame();   // the top cell: the breaker's frame
				} else {
					section.nextPickup();  // the relay's 51 pickup
				}
				player.sendOverlayMessage(section.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		SwitchgearSectionBlockEntity section = section(level, pos, state);
		if (level.isClientSide() || section == null) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			if (section.rack()) {
				level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, 0.6F);
			}
		} else if (section.operate()) {
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 1.0F, 0.9F);
		}
		player.sendOverlayMessage(section.inspect());
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}
}
