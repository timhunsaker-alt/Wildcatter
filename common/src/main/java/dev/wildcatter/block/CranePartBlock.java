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
 * A crane's base at true size (docs/multiblocks/tower-crane.md, crawler-crane.md, ring-crane.md): the tower crane's cross base or the crawler crane's
 * crawlers and carbody, or the ring crane's ring (ring-crane.md). Its main cell holds the crane; the rest above it is drawn by the
 * renderer. Right-click reads it; on the crawler and the ring crane, sneak-right-click steps the boom.
 */
public class CranePartBlock extends BigPartBlock {
	public enum Part {
		TOWER_CRANE("tower_crane", 6, 3, 6, CraneKind.TOWER),
		CRAWLER_CRANE("crawler_crane", 9, 2, 9, CraneKind.CRAWLER),
		/** The ring only: the cells of its 49 x 49 square whose centres lie 20 to 24.25 m out; the main cell is its north cell. */
		RING_CRANE("ring_crane", 49, 1, 49, CraneKind.RING);

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

		public Footprint footprint() {
			return this == RING_CRANE ? ring() : Footprint.box(w, h, l, 0, 0);
		}
	}

	/** The ring crane's ring (docs/multiblocks/ring-crane.md), main cell first, then north to south, west to east. */
	private static Footprint ring() {
		java.util.List<Footprint.Cell> cells = new java.util.ArrayList<>();
		cells.add(new Footprint.Cell(0, 0, 0));
		int main = RING_SIZE / 2;
		for (int z = 0; z < RING_SIZE; z++) {
			for (int x = 0; x < RING_SIZE; x++) {
				if ((x != main || z != 0) && inRing(x, z)) {
					cells.add(new Footprint.Cell(x - main, 0, z));
				}
			}
		}
		return Footprint.of(cells.toArray(Footprint.Cell[]::new));
	}

	public static final int RING_SIZE = 49;
	public static final double RING_INNER = 20.0, RING_OUTER = 24.25;

	private static boolean inRing(int x, int z) {
		double d = Math.hypot(x + 0.5 - RING_SIZE / 2.0, z + 0.5 - RING_SIZE / 2.0);
		return d >= RING_INNER && d <= RING_OUTER;
	}

	private final Part part;

	public CranePartBlock(Properties properties, Part part) {
		super(properties, part.footprint(), part.id);
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
			if (player.isShiftKeyDown() && part.kind.hasBoom()) {
				crane.nextBoom();
			}
			player.sendOverlayMessage(crane.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
