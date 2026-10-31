package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
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
import net.minecraft.world.item.Items;
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
 * One part of a genset skid at true size (docs/multiblocks/genset.md): the base (sub-base fuel tank or base frame), radiator,
 * engine, alternator or exhaust silencer, each filling its real size. Parts do nothing alone; the control panel finds them and
 * runs them. Placed on top of a base, a part goes to its own slot on the skid. The fuel tank holds diesel (pour in a bucket on
 * any of its cells, or run a line from a refinery).
 */
public class GensetPartBlock extends BaseEntityBlock implements BigPart {
	public enum Part {
		// sizes along the shaft x across x high; every main cell is at the radiator end of the front row's bottom
		DIESEL_ENGINE("genset_diesel_engine", Footprint.box(2, 2, 2, 0, 0)),
		GAS_ENGINE("genset_gas_engine", Footprint.box(4, 2, 2, 0, 0)),
		ALTERNATOR("alternator", Footprint.box(1, 2, 2, 0, 0)),
		RADIATOR("radiator", Footprint.box(1, 2, 2, 0, 0)),
		FUEL_TANK("fuel_tank", Footprint.box(5, 1, 2, 0, 0)),
		BASE_FRAME("genset_base_frame", Footprint.box(7, 1, 2, 0, 0)),
		EXHAUST("exhaust_stack", Footprint.box(2, 1, 1, 0, 0)),
		SILENCER_LARGE("exhaust_silencer_large", Footprint.box(3, 1, 1, 0, 0));

		private final String id;
		private final Footprint footprint;

		Part(String id, Footprint footprint) {
			this.id = id;
			this.footprint = footprint;
		}

		public boolean isBase() {
			return this == FUEL_TANK || this == BASE_FRAME;
		}

		public boolean isEngine() {
			return this == DIESEL_ENGINE || this == GAS_ENGINE;
		}

		public boolean isSilencer() {
			return this == EXHAUST || this == SILENCER_LARGE;
		}
	}

	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final Part part;
	private final IntegerProperty piece;

	public GensetPartBlock(Properties properties, Part part) {
		super(constructing(properties, part));
		this.part = part;
		this.piece = (IntegerProperty) getStateDefinition().getProperty("piece");
		BigParts.CONSTRUCTING.remove();
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(piece, 0));
	}

	private static Properties constructing(Properties properties, Part part) {
		BigParts.CONSTRUCTING.set(part.footprint);
		return properties;
	}

	public Part part() {
		return part;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, BigParts.pieceProperty(BigParts.CONSTRUCTING.get()));
	}

	// ---- one item, its real size -------------------------------------------------------------------------------------------
	@Override
	public Footprint footprint() {
		return part.footprint;
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
		return part.id;
	}

	@Override
	public boolean growsOnLoad() {
		return false;   // the skid's layout changed with true size: an older world's one-block part drops instead
	}

	@Override
	public BlockPlaceContext snap(BlockPlaceContext context) {
		return GensetSkid.snap(context, this);
	}

	/** Facing the player, or the base's way when it goes into a slot on a skid. */
	@Override
	public BlockState placementState(BlockPlaceContext context) {
		GensetSkid.Slot slot = GensetSkid.slotFor(context.getLevel(), context.getClickedPos(), this);
		Direction facing = slot != null ? slot.facing() : context.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		GensetSkid.Slot slot = GensetSkid.slotFor(context.getLevel(), context.getClickedPos(), this);
		if (slot != null && !slot.main().equals(context.getClickedPos())) {
			return null;   // its slot on this skid is taken
		}
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

	// ---- a click on any piece is a click on the part ---------------------------------------------------------------------------
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		BlockPos main = BigParts.main(pos, state);
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main, player);
		if (read != null) {
			return read;
		}
		if (part == Part.FUEL_TANK && stack.is(WildcatterItems.DIESEL_BUCKET) && level.getBlockEntity(main) instanceof GensetPartBlockEntity tank) {
			if (!level.isClientSide()) {
				if (tank.addFuel(Tuning.DIESEL_BUCKET_UNITS)) {
					stack.shrink(1);
					ItemStack empty = new ItemStack(Items.BUCKET);
					if (stack.isEmpty()) {
						player.setItemInHand(hand, empty);
					} else if (!player.getInventory().add(empty)) {
						net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), empty);
					}
				}
				player.sendOverlayMessage(tank.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(BigParts.main(pos, state)) instanceof GensetPartBlockEntity p) {
			player.sendOverlayMessage(p.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(piece) == 0 ? new GensetPartBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (state.getValue(piece) != 0) {
			return null;
		}
		return level.isClientSide() ? createTickerHelper(type, WildcatterBlocks.GENSET_PART_ENTITY, GensetPartBlockEntity::clientTick)
				: createTickerHelper(type, WildcatterBlocks.GENSET_PART_ENTITY, GensetPartBlockEntity::serverTick);
	}
}
