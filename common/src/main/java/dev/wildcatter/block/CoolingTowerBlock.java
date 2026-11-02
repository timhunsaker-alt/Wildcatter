package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * The fan deck of a cooling tower cell at true size, 4 x 4 x 2 (docs/multiblocks/cooling-tower.md): the deck, the 3 m fan in
 * its stack, the 7.5 kW 480 V motor and gearbox, and the handrail. It is the top of the column and its controller; built and
 * powered, the tower cools the condensate of every steam machine piped to its basin. The motor's terminals are on the front
 * face of the deck's main cell.
 */
public class CoolingTowerBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(4, 2, 4, 0, 0);

	public CoolingTowerBlock(Properties properties) {
		super(properties, FOOTPRINT, "cooling_tower");
	}

	@Override
	protected PartLayout layout() {
		return TowerColumn.layout();
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new CoolingTowerBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, WildcatterBlocks.COOLING_TOWER_ENTITY, level.isClientSide() ? CoolingTowerBlockEntity::clientTick : CoolingTowerBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos main = main(pos, state);
		if (!level.isClientSide() && level.getBlockEntity(main) instanceof CoolingTowerBlockEntity tower
				&& !Shock.touch((ServerLevel) level, main, player, Voltage.INDUSTRIAL, tower.isLive())) {
			player.sendOverlayMessage(tower.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
