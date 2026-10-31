package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The genset control panel and output breaker enclosure at true size (docs/multiblocks/genset.md): 1 m along the shaft, 2 m
 * across the skid, 2 m high, at the skid's east end. It is the controller. Its north face carries the readout and the
 * start/stop button (right-click any piece); the set's 480 V output leaves from its east end face, low down, where the
 * cables enter a real breaker box.
 */
public class GensetControlPanelBlock extends BaseEntityBlock implements BigPart {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** 1 along the shaft, 2 high, 2 across; the main cell is the front of the bottom. */
	public static final Footprint FOOTPRINT = Footprint.box(1, 2, 2, 0, 0);

	private final IntegerProperty piece;

	public GensetControlPanelBlock(Properties properties) {
		super(constructing(properties));
		this.piece = (IntegerProperty) getStateDefinition().getProperty("piece");
		BigParts.CONSTRUCTING.remove();
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(piece, 0));
	}

	private static Properties constructing(Properties properties) {
		BigParts.CONSTRUCTING.set(FOOTPRINT);
		return properties.noOcclusion();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, BigParts.pieceProperty(BigParts.CONSTRUCTING.get()));
	}

	/** The output terminals: the east end face (for a set facing north) of both bottom cells. */
	public static List<BlockPos> terminalCells(BlockPos main, BlockState state) {
		return List.of(main, BigParts.cell(main, state, FOOTPRINT.pieceAt(0, 0, 1, 0)));
	}

	public static Direction terminalFace(BlockState state) {
		return state.getValue(FACING).getClockWise();
	}

	// ---- one item, its real size -------------------------------------------------------------------------------------------
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
	public String shapeName() {
		return "genset_control_panel";
	}

	@Override
	public boolean growsOnLoad() {
		return false;   // the skid's layout changed with true size
	}

	@Override
	public BlockPlaceContext snap(BlockPlaceContext context) {
		return GensetSkid.layout().snap(context, this);
	}

	@Override
	public BlockState placementState(BlockPlaceContext context) {
		return GensetSkid.layout().placementState(context, this);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return GensetSkid.layout().placement(context, this);
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

	// ---- a click on any piece is a click on the panel ----------------------------------------------------------------------------
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos main = BigParts.main(pos, state);
		if (!level.isClientSide() && level.getBlockEntity(main) instanceof GensetControlPanelBlockEntity panel) {
			if (player.isShiftKeyDown()) {
				panel.sneakButton();
			} else if (panel.wantsManualClose()) {
				panel.manualClose();
			} else {
				panel.pushButton();
			}
			level.playSound(null, main, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 1.3F);
			player.sendOverlayMessage(panel.inspect());
			CircuitManager.markDirty(level);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(piece) == 0 ? new GensetControlPanelBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || state.getValue(piece) != 0 ? null
				: createTickerHelper(type, WildcatterBlocks.GENSET_CONTROL_PANEL_ENTITY, GensetControlPanelBlockEntity::tick);
	}
}
