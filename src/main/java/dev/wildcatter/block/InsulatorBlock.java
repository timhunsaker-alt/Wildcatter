package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * An insulator: it sits on top of a pole or a crossarm and holds one conductor of an overhead line. Right-click with an empty
 * hand to choose the conductor, then string spans between insulators with a conductor spool. Its voltage rating is the tier
 * of line it can safely hold; a line above that flashes over.
 */
public class InsulatorBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

	private final Voltage rating;

	public InsulatorBlock(Properties properties, Voltage rating) {
		super(properties);
		this.rating = rating;
		registerDefaultState(stateDefinition.any().setValue(CableBlock.CONDUCTOR, Conductor.A));
	}

	public Voltage rating() {
		return rating;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CableBlock.CONDUCTOR);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(WildcatterBlocks.POLE) || below.is(WildcatterBlocks.CROSSARM);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			@Nullable Orientation orientation, boolean movedByPiston) {
		if (!canSurvive(state, level, pos)) {
			level.destroyBlock(pos, true);
		}
		CircuitManager.markDirty(level);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof InsulatorBlockEntity insulator) {
			if (player.isShiftKeyDown()) {
				player.sendOverlayMessage(insulator.inspect());
			} else if (insulator.spanCount() > 0) {
				player.sendOverlayMessage(Component.literal("Take its spans down before changing its conductor. ").append(insulator.inspect()));
			} else {
				Conductor next = state.getValue(CableBlock.CONDUCTOR).next();
				level.setBlock(pos, state.setValue(CableBlock.CONDUCTOR, next), 3);
				CircuitManager.markDirty(level);
				player.sendOverlayMessage(Component.literal(next.label() + " (" + next.color() + " band)"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new InsulatorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.INSULATOR_ENTITY, InsulatorBlockEntity::tick);
	}
}
