package dev.wildcatter.block;

import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The common block of a true-size part (docs/multiblocks/README.md, "Big parts"): it fills its footprint when placed, breaks
 * whole, drops one item, has the model tool's collision, and only its main cell holds a block entity. A subclass names its
 * block entity and its clicks; if it belongs to a {@link PartLayout} a part clicked on top of its machine snaps into its slot.
 * The parts of this wave's machines all drop instead of growing in an older world, since their layouts changed.
 */
public abstract class BigPartBlock extends BaseEntityBlock implements BigPart {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final Footprint footprint;
	private final String shapeName;
	private final IntegerProperty piece;

	protected BigPartBlock(Properties properties, Footprint footprint, String shapeName) {
		super(constructing(properties, footprint));
		this.footprint = footprint;
		this.shapeName = shapeName;
		this.piece = (IntegerProperty) getStateDefinition().getProperty("piece");
		BigParts.CONSTRUCTING.remove();
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(piece, 0));
	}

	private static Properties constructing(Properties properties, Footprint footprint) {
		BigParts.CONSTRUCTING.set(footprint);
		return properties.noOcclusion();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, BigParts.pieceProperty(BigParts.CONSTRUCTING.get()));
	}

	/** The machine this part snaps into, or null for a part that goes where it is clicked. */
	protected @Nullable PartLayout layout() {
		return null;
	}

	/** The main cell's block entity. */
	protected abstract BlockEntity createMain(BlockPos pos, BlockState state);

	/** The main cell's own ticker, on this side; null for none. */
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return null;
	}

	/** The main cell of the part this piece belongs to: clicks on any piece go there. */
	protected static BlockPos main(BlockPos pos, BlockState state) {
		return BigParts.main(pos, state);
	}

	// ---- BigPart --------------------------------------------------------------------------------------------------------------
	@Override
	public Footprint footprint() {
		return footprint;
	}

	@Override
	public IntegerProperty piece() {
		return piece;
	}

	@Override
	public Direction facing(BlockState state) {
		return state.getValue(FACING);
	}

	@Override
	public String shapeName() {
		return shapeName;
	}

	@Override
	public boolean growsOnLoad() {
		return false;
	}

	@Override
	public BlockPlaceContext snap(BlockPlaceContext context) {
		return layout() != null ? layout().snap(context, this) : context;
	}

	@Override
	public BlockState placementState(BlockPlaceContext context) {
		return layout() != null ? layout().placementState(context, this)
				: defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	// ---- one item, its real size -------------------------------------------------------------------------------------------
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return layout() != null ? layout().placement(context, this) : BigParts.placement(context, placementState(context));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		BigParts.placed(level, pos, state);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		BigParts.onPlace(level, pos, state, oldState);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BigParts.check(level, pos, state);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return BigParts.updateShape(state, level, ticks, pos, neighborPos, neighborState);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BigParts.playerBreaks(level, pos, state, player);
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return BigParts.shape(state);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		return BigParts.drops(state, super.getDrops(state, params));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(piece) == 0 ? createMain(pos, state) : null;
	}

	/** The main cell ticks: on the server it also checks itself now and then, which is how an older world's lone cell is found. */
	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (state.getValue(piece) != 0) {
			return null;
		}
		BlockEntityTicker<T> own = mainTicker(level, state, type);
		if (level.isClientSide()) {
			return own;
		}
		return (l, p, s, entity) -> {
			BigParts.checkNow(l, p, s, 100);
			if (own != null && !entity.isRemoved()) {
				own.tick(l, p, s, entity);
			}
		};
	}
}
