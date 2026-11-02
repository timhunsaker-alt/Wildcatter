package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A thin wire that carries one conductor (A, B, C, N or G). The block you place starts as red (A) or black (N), and a right-click
 * with an empty hand steps it through the others; the coloured band shows which. A circuit needs its conductors running out
 * to the load and back. Grows an arm toward every neighbour with a terminal of the same conductor and voltage.
 *
 * Bare wire is cheap and live to the touch. Wool-wrapped wire is safe until the wool wears through (WEAR 3),
 * and then it is bare wire again.
 */
public class CableBlock extends BaseEntityBlock {
	/** 0 fresh, up to 3 worn out. Only wool-insulated wire ever leaves 0. */
	public static final IntegerProperty WEAR = IntegerProperty.create("wear", 0, 3);
	/** Which conductor this piece of wire carries. */
	public static final EnumProperty<Conductor> CONDUCTOR = EnumProperty.create("conductor", Conductor.class);

	private final Voltage voltage;
	private final boolean wool;

	private final boolean plastic;

	public CableBlock(Properties properties, Voltage voltage, Conductor conductor, boolean wool) {
		this(properties, voltage, conductor, wool, false);
	}

	/** Plastic insulation never wears out, at any voltage. */
	public CableBlock(Properties properties, Voltage voltage, Conductor conductor, boolean wool, boolean plastic) {
		super(properties);
		this.plastic = plastic;
		this.voltage = voltage;
		this.wool = wool;
		registerDefaultState(WireShape.noArms(stateDefinition.any()).setValue(WEAR, 0).setValue(CONDUCTOR, conductor));
	}

	public Voltage voltage() {
		return voltage;
	}

	/** The conductor of a placed piece of wire. */
	public static Conductor conductorOf(BlockState state) {
		return state.getValue(CONDUCTOR);
	}

	public boolean plastic() {
		return plastic;
	}

	public boolean wool() {
		return wool;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		WireShape.ARMS.values().forEach(builder::add);
		builder.add(WEAR, CONDUCTOR);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return withArms(defaultBlockState(), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			@Nullable Orientation orientation, boolean movedByPiston) {
		BlockState updated = withArms(state, level, pos);
		if (updated != state) {
			level.setBlock(pos, updated, 2);
		}
		CircuitManager.markDirty(level);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		neighborChanged(state, level, pos, this, null, movedByPiston);
	}

	private BlockState withArms(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			boolean joins = CircuitPart.portAt(level, pos.relative(side), side.getOpposite(), state.getValue(CONDUCTOR), voltage);
			state = state.setValue(WireShape.ARMS.get(side), joins);
		}
		return state;
	}

	/** Right-click with an empty hand: the wire carries the next conductor. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			Conductor next = state.getValue(CONDUCTOR).next();
			BlockState changed = withArms(state.setValue(CONDUCTOR, next), level, pos);
			level.setBlock(pos, changed, 3);
			CircuitManager.markDirty(level);
			player.sendOverlayMessage(Component.literal(next.label() + " (" + next.color() + " band)"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return WireShape.of(state);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WireBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.WIRE_ENTITY, WireBlockEntity::tick);
	}
	/** A broken conductor drops with its size (the size lives in the block entity, not the block state). */
	@Override
	protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		java.util.List<net.minecraft.world.item.ItemStack> drops = super.getDrops(state, params);
		net.minecraft.world.level.block.entity.BlockEntity be = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
		dev.wildcatter.energy.ConductorSize size = be instanceof WireBlockEntity wire ? wire.size() : be instanceof MultiCableBlockEntity cable ? cable.size() : null;
		if (size != null) {
			for (net.minecraft.world.item.ItemStack drop : drops) {
				if (drop.is(asItem())) {
					drop.set(dev.wildcatter.WildcatterComponents.CONDUCTOR_SIZE, size);
				}
			}
		}
		return drops;
	}
}
