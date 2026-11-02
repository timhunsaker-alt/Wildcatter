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
 * The substation's power transformer at true size and its controller (docs/multiblocks/substation.md): 138 kV delta to 12.47 kV
 * grounded wye (IEC 132 / 11 kV), 20 MVA, 8 %Z, ONAN; 6 m along the line, 5 m across, 7 m to the bushing tops. It faces the
 * yard's way; the 138 kV terminal is on the west face and the 12.47 kV one on the east face (for a yard facing north), both on
 * the bottom cell of the yard's centre line. It is energized only while the whole yard stands.
 */
public class PowerTransformerBlock extends TransformerBlock implements BigPart {
	/** 6 along the line, 7 up, 5 across; the main cell is the north-west bottom corner. */
	public static final Footprint FOOTPRINT = Footprint.box(6, 7, 5, 0, 0);

	private final IntegerProperty piece;

	public PowerTransformerBlock(Properties properties, double ratedVa, double percentZ) {
		super(constructing(properties), Voltage.DISTRIBUTION, Voltage.HIGH, ratedVa, percentZ, true, false, true);
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
		// like every part of the yard it faces whoever places it (the line then runs to their right)
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(piece, 0);
	}

	@Override
	public String shapeName() {
		return "power_transformer";
	}

	@Override
	public boolean growsOnLoad() {
		return false;   // the yard's layout changed with true size
	}

	@Override
	public List<Terminal> terminals(BlockPos main, BlockState state) {
		Direction facing = state.getValue(FACING);
		return List.of(
				new Terminal(terminalCell(main, state, facing.getCounterClockWise()), facing.getCounterClockWise(), true),
				new Terminal(terminalCell(main, state, facing.getClockWise()), facing.getClockWise(), false));
	}

	@Override
	public BlockPos terminalCell(BlockPos main, BlockState state, Direction face) {
		return BigParts.lineTerminal(main, state, face, 6, 5);
	}

	// ---- one item, its real size -------------------------------------------------------------------------------------------
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return BigParts.placement(context, placementState(context));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		BigParts.placed(level, pos, state);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		BigParts.onPlace(level, pos, state, oldState);   // set by a command or found alone: checked on the next tick
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
		return state.getValue(piece) == 0 ? new PowerTransformerBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || state.getValue(piece) != 0 ? null
				: createTickerHelper(type, dev.wildcatter.WildcatterBlocks.POWER_TRANSFORMER_ENTITY, PowerTransformerBlockEntity::tick);
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
