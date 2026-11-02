package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * An insulator: it holds one conductor of an overhead line. Right-click with an empty hand to choose the conductor, then string
 * spans between insulators with a conductor spool. Its voltage rating is the tier of line it can safely hold; a line above that
 * flashes over.
 * <p>
 * How it is mounted (docs/multiblocks/poles.md) says where its conductor sits: a pin or spool insulator on a crossarm, or on a
 * pole-top pin; a 138 kV string hangs from an H-frame's crossbeam. An older world's insulators stand on one-block pole
 * sections and crossarms as before.
 */
public class InsulatorBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);
	private static final VoxelShape SMALL = Block.box(5, 0, 5, 11, 7, 11);
	private static final VoxelShape STRING = Block.box(4, 0, 4, 12, 16, 12);

	/** Where it is mounted, and so how high in its cell the conductor sits (from the model, in metres). */
	public enum Mount implements StringRepresentable {
		/** On an older world's one-block pole section or crossarm. */
		LEGACY("legacy", 0.95),
		/** A pin or spool insulator on a crossarm: the arm's top is the cell's floor. */
		ARM("arm", 0.26),
		/** On a pole-top pin: the pole stands 0.3 m into the cell. */
		POLE_TOP("pole_top", 0.56),
		/** A string hanging from a crossbeam, 1.5 m long: the clamp is at the bottom of the cell. */
		HANGING("hanging", 0.04);

		private final String name;
		private final double conductorY;

		Mount(String name, double conductorY) {
			this.name = name;
			this.conductorY = conductorY;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Mount> MOUNT = EnumProperty.create("mount", Mount.class);

	private final Voltage rating;

	public InsulatorBlock(Properties properties, Voltage rating) {
		super(properties);
		this.rating = rating;
		registerDefaultState(stateDefinition.any().setValue(CableBlock.CONDUCTOR, Conductor.A).setValue(MOUNT, Mount.LEGACY));
	}

	public Voltage rating() {
		return rating;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CableBlock.CONDUCTOR, MOUNT);
	}

	/** How an insulator of this rating would be mounted at {@code pos}, or null if nothing there holds it. */
	public @Nullable Mount mountAt(LevelReader level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		if (rating == Voltage.HIGH && level.getBlockState(pos.above()).getBlock() instanceof CrossbeamBlock) {
			return Mount.HANGING;
		}
		if (rating != Voltage.HIGH && below.getBlock() instanceof CrossarmPartBlock) {
			return Mount.ARM;
		}
		if (rating != Voltage.HIGH && WoodPoleBlock.isTop(below)) {
			return Mount.POLE_TOP;
		}
		return below.is(WildcatterBlocks.POLE) || below.is(WildcatterBlocks.CROSSARM) ? Mount.LEGACY : null;
	}

	/** How high in its cell the conductor sits: the point a span is drawn from and its shock reach is measured along. */
	public static double conductorY(BlockState state) {
		return state.getBlock() instanceof InsulatorBlock ? state.getValue(MOUNT).conductorY : Mount.LEGACY.conductorY;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Mount mount = mountAt(context.getLevel(), context.getClickedPos());
		return mount == null ? null : defaultBlockState().setValue(MOUNT, mount);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return mountAt(level, pos) != null;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			@Nullable Orientation orientation, boolean movedByPiston) {
		Mount mount = mountAt(level, pos);
		if (mount == null) {
			level.destroyBlock(pos, true);
		} else if (mount != state.getValue(MOUNT)) {
			level.setBlock(pos, state.setValue(MOUNT, mount), Block.UPDATE_CLIENTS);
		}
		CircuitManager.markDirty(level);
	}

	// what holds it went in a shape update (the far cell of an arm whose main cell dropped): it drops too
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction.getAxis() == Direction.Axis.Y) {
			Mount mount = mountAt(level, pos);
			if (mount == null) {
				return Blocks.AIR.defaultBlockState();
			}
			if (mount != state.getValue(MOUNT)) {
				return state.setValue(MOUNT, mount);
			}
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(MOUNT)) {
			case LEGACY -> SHAPE;
			case HANGING -> STRING;
			default -> SMALL;
		};
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
