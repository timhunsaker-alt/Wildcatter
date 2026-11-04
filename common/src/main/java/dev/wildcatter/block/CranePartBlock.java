package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.crane.CraneKind;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
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
 * A crane's base at true size (docs/multiblocks/tower-crane.md, crawler-crane.md): the tower crane's cross base or the crawler crane's
 * crawlers and carbody. Its main cell holds the crane; the rest above it is drawn by the renderer. Right-click reads it; on the
 * crawler, sneak-right-click steps the boom.
 */
public class CranePartBlock extends BigPartBlock {
	public enum Part {
		TOWER_CRANE("tower_crane", 6, 3, 6, CraneKind.TOWER),
		CRAWLER_CRANE("crawler_crane", 9, 2, 9, CraneKind.CRAWLER);

		private final String id;
		public final int w, h, l;
		public final CraneKind kind;

		Part(String id, int w, int h, int l, CraneKind kind) {
			this.id = id;
			this.w = w;
			this.h = h;
			this.l = l;
			this.kind = kind;
		}

		public String id() {
			return id;
		}
	}

	private final Part part;

	public CranePartBlock(Properties properties, Part part) {
		super(properties, Footprint.box(part.w, part.h, part.l, 0, 0), part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new CraneBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.CRANE_ENTITY, CraneBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof CraneBlockEntity crane) {
			if (player.isShiftKeyDown() && part.kind == CraneKind.CRAWLER) {
				crane.nextBoom();
			}
			player.sendOverlayMessage(crane.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
