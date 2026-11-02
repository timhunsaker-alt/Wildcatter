package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A substation's three-phase disconnect at true size (docs/multiblocks/substation.md): the 138 kV centre-break (2 x 6 x 9, two
 * arms per phase that swing open) or the 12.47 kV vertical-break (1 x 5 x 3, one blade per phase that lifts). It faces the
 * yard's way; the line runs through it west to east (for a yard facing north), its terminals on the bottom cell of the yard's
 * centre line at each end.
 */
public class BigDisconnectBlock extends DisconnectSwitchBlock implements BigPart {
	private final Footprint footprint;
	private final String shapeName;
	private final int length;
	private final int width;
	private final IntegerProperty piece;
	private final java.util.function.Supplier<List<dev.wildcatter.kinetic.Kinetic.Rotor>> rotors;

	/** {@code length} along the line, {@code height} up, {@code width} across it; the main cell is the north-west bottom corner. */
	public BigDisconnectBlock(Properties properties, Voltage voltage, int length, int height, int width, String shapeName, java.util.function.Supplier<List<dev.wildcatter.kinetic.Kinetic.Rotor>> rotors) {
		super(constructing(properties, Footprint.box(length, height, width, 0, 0)), voltage);
		this.rotors = rotors;
		this.footprint = Footprint.box(length, height, width, 0, 0);
		this.shapeName = shapeName;
		this.length = length;
		this.width = width;
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
	public Direction lineFacing(BlockState state) {
		return state.getValue(FACING).getClockWise();
	}

	/** The 138 kV centre-break's six arms (for the block's registration). */
	public static List<dev.wildcatter.kinetic.Kinetic.Rotor> hvRotors() {
		return DisconnectRotors.hv();
	}

	/** The 12.47 kV vertical-break's three blades (for the block's registration). */
	public static List<dev.wildcatter.kinetic.Kinetic.Rotor> distRotors() {
		return DisconnectRotors.dist();
	}

	/** Its arms or blades, in the main cell's frame for a yard facing north (tools/models/rotors.py). */
	public List<dev.wildcatter.kinetic.Kinetic.Rotor> rotors() {
		return rotors.get();
	}

	@Override
	public BlockPos terminalCell(BlockPos main, BlockState state, Direction face) {
		return BigParts.lineTerminal(main, state, face, length, width);
	}

	// ---- BigPart ------------------------------------------------------------------------------------------------------------
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
	public BlockState placementState(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

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
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(piece) == 0 ? super.newBlockEntity(pos, state) : null;
	}

	/** The main cell checks itself now and then, which is how an older world's one-block part is found. */
	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state,
			net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return level.isClientSide() || state.getValue(piece) != 0 ? null : (l, p, s, e) -> BigParts.checkNow(l, p, s, 100);
	}

	// a click on any piece is a click on the disconnect
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos main = BigParts.main(pos, state);
		return super.useWithoutItem(level.getBlockState(main), level, main, player, hit);
	}
}
