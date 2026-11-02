package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The hyperbolic natural-draft cooling tower's controller (docs/multiblocks/natural-draft-tower.md): the basin's outlet structure
 * and hot-water riser at the basin's edge, facing the tower, whose centre lies 34 m in front of it. Materials by a hopper on top;
 * the tower rises from them stage by stage and lift by lift.
 */
public class NaturalDraftTowerBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public static final Footprint FOOTPRINT = Footprint.box(4, 3, 4, 0, 0);

	public NaturalDraftTowerBlock(Properties properties) {
		super(properties, FOOTPRINT, "natural_draft_tower");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new NaturalDraftTowerBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, WildcatterBlocks.NATURAL_DRAFT_TOWER_ENTITY,
				level.isClientSide() ? NaturalDraftTowerBlockEntity::clientTick : NaturalDraftTowerBlockEntity::tick);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof NaturalDraftTowerBlockEntity tower) {
			player.sendOverlayMessage(tower.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof NaturalDraftTowerBlockEntity tower) {
			tower.demolish(level);
			player.sendOverlayMessage(Component.literal("Cooling tower: demolished"));
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** A hopper on top works its material bins. */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
