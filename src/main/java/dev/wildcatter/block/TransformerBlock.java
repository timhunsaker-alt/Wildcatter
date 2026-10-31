package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import dev.wildcatter.item.PowerMeterItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Joins two voltage levels. The side it faces is the higher voltage, the opposite side is the lower one, and the
 * four other sides connect to nothing. Power can flow either way.
 */
public class TransformerBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final Voltage low;
	private final Voltage high;
	/** A pole-mount unit has a kVA rating (it heats on overload and fails); the others are not rated. 0 means unrated. */
	private final double ratedVa;
	/** The nameplate impedance in percent (leakage reactance); 0 for the unrated stand-ins. */
	private final double percentZ;
	/** A pad-mount or unit-substation bank: always three-phase, never a pole unit, never split-phase. */
	private final boolean threePhase;
	/** Delta on the low (generator) side, grounded wye on the high side: a generator step-up. The high side leads by 30 degrees. */
	private final boolean deltaLow;
	/** Delta on the high (transmission) side, grounded wye on the low side: a distribution substation. The high side leads by 30 degrees. */
	private final boolean deltaHigh;

	public TransformerBlock(Properties properties, Voltage low, Voltage high) {
		this(properties, low, high, 0);
	}

	/** A pole-mount unit with its kVA rating. */
	public TransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa) {
		this(properties, low, high, ratedVa, ratedVa > 0 ? dev.wildcatter.Tuning.POLE_TRANSFORMER_PERCENT_Z : 0, false);
	}

	/** A three-phase pad-mount or unit substation, sitting on the ground: its kVA rating and nameplate impedance. */
	public static TransformerBlock threePhase(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ) {
		return new TransformerBlock(properties, low, high, ratedVa, percentZ, true, false);
	}

	/** A generator step-up (docs/multiblocks/gsu.md): delta on the generator side, grounded wye on the line side. */
	public static TransformerBlock deltaWye(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ) {
		return new TransformerBlock(properties, low, high, ratedVa, percentZ, true, true);
	}

	private TransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ, boolean threePhase) {
		this(properties, low, high, ratedVa, percentZ, threePhase, false);
	}

	private TransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ, boolean threePhase, boolean deltaLow) {
		this(properties, low, high, ratedVa, percentZ, threePhase, deltaLow, false);
	}

	/** For the substation's power transformer: delta on the high side. */
	protected TransformerBlock(Properties properties, Voltage low, Voltage high, double ratedVa, double percentZ, boolean threePhase, boolean deltaLow, boolean deltaHigh) {
		super(properties);
		this.low = low;
		this.high = high;
		this.ratedVa = ratedVa;
		this.percentZ = percentZ;
		this.threePhase = threePhase;
		this.deltaLow = deltaLow;
		this.deltaHigh = deltaHigh;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public Voltage low() {
		return low;
	}

	public Voltage high() {
		return high;
	}

	public double ratedVa() {
		return ratedVa;
	}

	public double percentZ() {
		return percentZ;
	}

	public boolean threePhase() {
		return threePhase;
	}

	public boolean deltaLow() {
		return deltaLow;
	}

	public boolean deltaHigh() {
		return deltaHigh;
	}

	/** A terminal: a face of one of the transformer's cells, on its high or its low side. */
	public record Terminal(BlockPos cell, Direction face, boolean high) {
	}

	/**
	 * Where the bushings are. A one-block unit has its high side on the front and the top (where real bushings are) and its
	 * low side on the other four faces, so a split-phase secondary has room for A, B and N. A true-size unit says its own.
	 */
	public java.util.List<Terminal> terminals(BlockPos main, BlockState state) {
		Direction front = state.getValue(FACING);
		java.util.List<Terminal> out = new java.util.ArrayList<>(6);
		for (Direction face : Direction.values()) {
			out.add(new Terminal(main, face, face == front || face == Direction.UP));
		}
		return out;
	}

	/** Hangs on a pole: a rated single-phase unit. */
	public boolean poleMount() {
		return ratedVa > 0 && !threePhase;
	}

	/** A pole-mount transformer hangs on a pole: one must stand beside it. */
	@Override
	protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
		if (!poleMount()) {
			return true;
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(pos.relative(side)).is(WildcatterBlocks.POLE)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// The high side faces away from the player, so the near side (where you stand) is the lower voltage.
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TransformerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || ratedVa <= 0 ? null : createTickerHelper(type, WildcatterBlocks.TRANSFORMER_ENTITY, TransformerBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TransformerBlockEntity transformer) {
			if (!Shock.touch((ServerLevel) level, pos, player, transformer.topVoltage(), transformer.isLive())) {
				player.sendOverlayMessage(transformer.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
