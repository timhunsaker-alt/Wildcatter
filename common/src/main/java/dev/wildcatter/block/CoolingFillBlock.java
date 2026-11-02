package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A part of the cooling tower cell that does nothing on its own (docs/multiblocks/cooling-tower.md): the fill (PVC film,
 * 4 x 4 x 2) or the drift eliminators with the hot-water distribution (4 x 4 x 1). The fan deck checks for them.
 */
public class CoolingFillBlock extends BigPartBlock {
	public CoolingFillBlock(Properties properties, Footprint footprint, String shapeName) {
		super(properties, footprint, shapeName);
	}

	@Override
	protected PartLayout layout() {
		return TowerColumn.layout();
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new BigPartBlockEntity(pos, state);
	}

	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.phys.BlockHitResult hit) {
		// a click on the tower's fill or eliminators reads the fan deck above
		BlockPos deck = BigParts.main(pos, state).above(TowerColumn.DECK_HEIGHT - (state.is(WildcatterBlocks.COOLING_FILL) ? 1 : 3));
		if (!level.isClientSide() && level.getBlockEntity(deck) instanceof CoolingTowerBlockEntity tower) {
			player.sendOverlayMessage(tower.inspect());
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}
}
