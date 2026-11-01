package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A three-phase transformer at true size (docs/multiblocks/README.md, "Big parts"): one item that fills its real size, breaks
 * whole and drops one item. A subclass says where its bushings are ({@link #terminals}). An older world's one-block unit grows
 * into its cells where there is room.
 */
public abstract class BigTransformerBlock extends TransformerBlock implements BigPart {
	private final Footprint footprint;
	private final String shapeName;
	private final IntegerProperty piece;

	protected BigTransformerBlock(Properties properties, Footprint footprint, String shapeName, Voltage low, Voltage high, double ratedVa, double percentZ, boolean deltaLow) {
		this(properties, footprint, shapeName, low, high, ratedVa, percentZ, deltaLow, false);
	}

	/** The same with the delta on either side: a generator step-up's on its low side, a substation transformer's on its high side. */
	protected BigTransformerBlock(Properties properties, Footprint footprint, String shapeName, Voltage low, Voltage high, double ratedVa, double percentZ,
			boolean deltaLow, boolean deltaHigh) {
		super(constructing(properties, footprint), low, high, ratedVa, percentZ, true, deltaLow, deltaHigh);
		this.footprint = footprint;
		this.shapeName = shapeName;
		this.piece = (IntegerProperty) getStateDefinition().getProperty("piece");
		BigParts.CONSTRUCTING.remove();
		registerDefaultState(defaultBlockState().setValue(piece, 0));
	}

	private static Properties constructing(Properties properties, Footprint footprint) {
		BigParts.CONSTRUCTING.set(footprint);
		return properties.noOcclusion();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BigParts.pieceProperty(BigParts.CONSTRUCTING.get()));
	}

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
	public BlockState placementState(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(piece, 0);
	}

	@Override
	public String shapeName() {
		return shapeName;
	}

	@Override
	public abstract List<Terminal> terminals(BlockPos main, BlockState state);

	// ---- one item, its real size -------------------------------------------------------------------------------------------
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return BigParts.placement(context, super.getStateForPlacement(context));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		BigParts.placed(level, pos, state);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		BigParts.onPlace(level, pos, state, oldState);   // set by a command or found alone: grow on the next tick if there is room
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
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return true;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(piece) == 0 ? super.newBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return state.getValue(piece) == 0 ? super.getTicker(level, state, type) : null;
	}

	// a click on any piece is a click on the transformer
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos main = BigParts.main(pos, state);
		if (!level.isClientSide() && level.getBlockEntity(main) instanceof TransformerBlockEntity transformer) {
			if (!Shock.touch((ServerLevel) level, main, player, transformer.topVoltage(), transformer.isLive())) {
				player.sendOverlayMessage(transformer.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
