package dev.wildcatter.block;

import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Hardware bolted to poles (docs/multiblocks/poles.md): a crossarm on one pole, an H-frame's crossbeam or X-brace between two.
 * Clicked on a pole it moves to its place there; it needs its poles, and drops when one goes. Its insulators then lose their
 * footing and drop too.
 */
public abstract class PoleHardwareBlock extends BigPartBlock {
	protected PoleHardwareBlock(Properties properties, Footprint footprint, String shapeName) {
		super(properties, footprint, shapeName);
	}

	/** The pole cells this part, with its main cell at {@code main} and facing {@code facing}, is bolted to. */
	protected abstract List<BlockPos> poles(BlockPos main, Direction facing);

	/** Where the main cell goes for a click on {@code pole}, a pole cell, facing {@code facing}; null to go where clicked. */
	protected abstract @Nullable BlockPos mainFor(BlockGetter level, BlockPos pole, Direction facing);

	/** Said when the part has no poles where it would go. */
	protected abstract String needs();

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new BigPartBlockEntity(pos, state);
	}

	/** New parts: one set by a command grows into its cells. */
	@Override
	public boolean growsOnLoad() {
		return true;
	}

	@Override
	public BlockPlaceContext snap(BlockPlaceContext context) {
		BlockPos pole = context.getClickedPos().relative(context.getClickedFace().getOpposite());
		if (!WoodPoleBlock.isPole(context.getLevel().getBlockState(pole))) {
			return context;
		}
		BlockPos main = mainFor(context.getLevel(), pole, context.getHorizontalDirection().getOpposite());
		return main == null || main.equals(context.getClickedPos()) ? context : BlockPlaceContext.at(context, main, Direction.UP);
	}

	private boolean onPoles(LevelReader level, BlockPos main, Direction facing) {
		for (BlockPos pole : poles(main, facing)) {
			if (!WoodPoleBlock.isPole(level.getBlockState(pole))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = placementState(context);
		if (!onPoles(context.getLevel(), context.getClickedPos(), facing(state))) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.literal(needs()));
			}
			return null;
		}
		return BigParts.placement(context, state);
	}

	// a pole beside it changed: look again on the next tick
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState out = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		if (!out.isAir() && !WoodPoleBlock.isPole(neighborState) && poles(BigParts.main(pos, state), facing(state)).contains(neighborPos)) {
			ticks.scheduleTick(pos, this, 1);
		}
		return out;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockPos main = BigParts.main(pos, state);
		BlockState at = level.getBlockState(main);
		if (!at.is(this)) {
			// a cell across a gap from its main cell (an arm's far end) is not told when the part breaks: it goes now
			level.removeBlock(pos, false);
			return;
		}
		if (!onPoles(level, main, facing(at))) {
			level.destroyBlock(main, true);
			return;
		}
		if (pos.equals(main)) {
			super.tick(state, level, pos, random);
		}
	}
}
