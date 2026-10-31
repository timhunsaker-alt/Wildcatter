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
 * The generator step-up at true size (docs/multiblocks/gsu.md): one oil-filled tank 3 m across, 2 m deep and 3 m to the bushing
 * tops, with its radiators on the two sides. Facing is the 12.47 kV side, away from whoever places it.
 * <ul>
 * <li>The 4.16 kV delta comes in through the cable box low on the back, at the main cell's back face.</li>
 * <li>The three 12.47 kV bushings stand on the cover over the front half: their terminals are the top and front faces of the
 * top cell in the middle of the front row.</li>
 * </ul>
 */
public class GsuTransformerBlock extends TransformerBlock implements BigPart {
	/** 3 across, 3 up, 2 deep, the main cell at the back of the bottom middle. */
	public static final Footprint FOOTPRINT = Footprint.box(3, 3, 2, 1, 1);

	private final IntegerProperty piece;

	public GsuTransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ) {
		super(constructing(properties), low, high, ratedVa, percentZ, true, true, false);
		this.piece = (IntegerProperty) getStateDefinition().getProperty("piece");
		BigParts.CONSTRUCTING.remove();
		registerDefaultState(defaultBlockState().setValue(piece, 0));
	}

	private static Properties constructing(Properties properties) {
		BigParts.CONSTRUCTING.set(FOOTPRINT);
		return properties.noOcclusion();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BigParts.pieceProperty(BigParts.CONSTRUCTING.get()));
	}

	@Override
	public Footprint footprint() {
		return FOOTPRINT;
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
		return "gsu_transformer";
	}

	@Override
	public List<Terminal> terminals(BlockPos main, BlockState state) {
		Direction front = state.getValue(FACING);
		BlockPos bushings = BigParts.cell(main, state, FOOTPRINT.pieceAt(0, 2, -1, 0));   // the top of the front row's middle
		return List.of(
				new Terminal(main, front.getOpposite(), false),
				new Terminal(bushings, Direction.UP, true),
				new Terminal(bushings, front, true));
	}

	// ---- one item, its real size -------------------------------------------------------------------------------------------
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return BigParts.placement(context, super.getStateForPlacement(context));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide()) {
			BigParts.grow(level, pos, state);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (state.getValue(piece) == 0 && !oldState.is(this)) {
			level.scheduleTick(pos, this, 1);   // set by a command or found alone: grow on the next tick if there is room
		}
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
