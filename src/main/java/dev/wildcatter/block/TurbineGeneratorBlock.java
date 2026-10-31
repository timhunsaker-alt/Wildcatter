package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * The steam plant's generator at true size, 1,250 kVA two-pole, 3 x 2 x 2 (docs/multiblocks/steam-plant.md): the controller.
 * Right-click lights the burner and starts the plant, or stops it. Its 4.16 kV terminals are on its west end face (for a plant
 * facing north), the non-drive end away from the turbine, on both bottom cells.
 */
public class TurbineGeneratorBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(3, 2, 2, 0, 0);
	/** For older code that names the facing through this class. */
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING = BigPartBlock.FACING;

	public TurbineGeneratorBlock(Properties properties) {
		super(properties, FOOTPRINT, "turbine_generator");
	}

	/** The output terminals: the west end face (for a plant facing north) of both bottom cells. */
	public static List<BlockPos> terminalCells(BlockPos main, BlockState state) {
		return List.of(main, BigParts.cell(main, state, FOOTPRINT.pieceAt(0, 0, 1, 0)));
	}

	public static Direction terminalFace(BlockState state) {
		return state.getValue(FACING).getCounterClockWise();
	}

	@Override
	protected PartLayout layout() {
		return SteamPlantLayout.layout();
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new TurbineGeneratorBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.TURBINE_GENERATOR_ENTITY, TurbineGeneratorBlockEntity::tick);
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
		if (!level.isClientSide() && level.getBlockEntity(main) instanceof TurbineGeneratorBlockEntity panel) {
			panel.pushButton();
			level.playSound(null, main, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 1.3F);
			player.sendOverlayMessage(panel.inspect());
			CircuitManager.markDirty(level);
		}
		return InteractionResult.SUCCESS;
	}
}
