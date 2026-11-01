package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The pumpjack at true size (docs/multiblocks/well-site.md): a conventional beam pumping unit, API size 80-119-54, with its 10 hp
 * 480 V three-phase motor. 3 x 5 x 7: its base 6 m long behind the well and its horsehead over the {@link OilWellheadBlock} one
 * cell in front of its main cell. Clicked on a wellhead it snaps into place, facing the way the player faces. Its motor's
 * terminal box is on the bottom cell at the back.
 */
public class PumpjackBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = cells();

	public PumpjackBlock(Properties properties) {
		super(properties, FOOTPRINT, "pumpjack");
	}

	private static Footprint cells() {
		List<Footprint.Cell> cells = new ArrayList<>();
		cells.add(new Footprint.Cell(0, 0, 0));
		for (int y = 0; y < 5; y++) {
			for (int z = 0; z < 6; z++) {
				for (int x = -1; x <= 1; x++) {
					if ((x != 0 || y != 0 || z != 0) && !(y == 0 && z == 5 && x != 0)) {   // open beside the terminal box
						cells.add(new Footprint.Cell(x, y, z));
					}
				}
			}
		}
		for (int y = 1; y < 5; y++) {
			cells.add(new Footprint.Cell(0, y, -1));   // the horsehead and bridle over the well
		}
		return Footprint.of(cells.toArray(Footprint.Cell[]::new));
	}

	/** Where the well is: the cell in front of the main cell. */
	public static BlockPos wellhead(BlockPos main, BlockState state) {
		return main.relative(state.getValue(FACING));
	}

	/** The motor's terminal box: the bottom cell at the back. */
	public static BlockPos terminalBox(BlockPos main, BlockState state) {
		return main.relative(state.getValue(FACING).getOpposite(), 5);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new PumpjackBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, WildcatterBlocks.PUMPJACK_ENTITY, level.isClientSide() ? (l, p, st, be) -> dev.wildcatter.kinetic.RunningSound.clientTick(be) : PumpjackBlockEntity::tick);
	}

	/** The front faces the way the player faces: they stand behind it looking at the well. */
	@Override
	public BlockState placementState(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	/** Clicked on an oil wellhead: the horsehead goes over it. */
	@Override
	public BlockPlaceContext snap(BlockPlaceContext context) {
		BlockPos well = context.getClickedPos().relative(context.getClickedFace().getOpposite());
		if (!context.getLevel().getBlockState(well).is(WildcatterBlocks.OIL_WELLHEAD)) {
			return context;
		}
		BlockPos main = well.relative(context.getHorizontalDirection().getOpposite());
		return main.equals(context.getClickedPos()) ? context : BlockPlaceContext.at(context, main, Direction.UP);
	}

	/** An older world's one-block pumpjack stood on its cased hole: the hole gets a wellhead and the pumpjack drops to be set over it. */
	@Override
	public boolean replaceLegacy(ServerLevel level, BlockPos main, BlockState state) {
		if (!level.getBlockState(main.below()).is(WildcatterBlocks.CASING)) {
			return false;
		}
		level.removeBlockEntity(main);
		level.setBlock(main, WildcatterBlocks.OIL_WELLHEAD.defaultBlockState(), Block.UPDATE_ALL);
		Block.popResource(level, main.above(), new ItemStack(WildcatterBlocks.PUMPJACK));
		String news = "Pumpjack at " + main.getX() + " " + main.getY() + " " + main.getZ() + " is now true size (" + BigParts.size(FOOTPRINT)
				+ "): its well has an oil wellhead now, and the pumpjack dropped as an item. Click the wellhead with it to set it over the well (the crude in its tank was lost)";
		Wildcatter.LOGGER.info(news);
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().closerThan(main, 128)) {
				player.sendSystemMessage(Component.literal(news));
			}
		}
		return true;
	}

	/** A bucket on the pumpjack or its wellhead: the stream is oil, water and gas together, so crude comes from a separator or a stock tank. */
	static void fillBucket(PumpjackBlockEntity jack, ItemStack stack, Player player, InteractionHand hand, Level level) {
		player.sendOverlayMessage(Component.literal("The well flows oil, water and gas together: pipe the wellhead to a separator, and fill buckets at its oil side or a stock tank"));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		if (read != null) {
			return read;
		}
		if (stack.is(Items.BUCKET) && level.getBlockEntity(main(pos, state)) instanceof PumpjackBlockEntity jack) {
			if (!level.isClientSide()) {
				fillBucket(jack, stack, player, hand, level);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof PumpjackBlockEntity jack) {
			if (player.isShiftKeyDown()) {
				player.sendOverlayMessage(jack.swapLeads());
				dev.wildcatter.circuit.CircuitManager.markDirty(level);
			} else if (!Shock.touch((ServerLevel) level, pos, player, Voltage.INDUSTRIAL, jack.isLive())) {
				player.sendOverlayMessage(jack.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
