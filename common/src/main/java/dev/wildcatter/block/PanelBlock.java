package dev.wildcatter.block;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A load centre (120/240 V, 1 x 1 x 1) or a 480Y/277 V panelboard (1 x 2 x 1) on a wall, at true size
 * (docs/multiblocks/panelboards.md). It holds the main breaker and the bus; the branch breakers clip into gutter blocks set beside it.
 * The service lands on its top face or its back face. Right-click works the main.
 */
public class PanelBlock extends BigPartBlock {
	/** What the main's handle shows through the dead front: ON (up), OFF (down) or TRIPPED (centre, its flag out). EMPTY is unused. */
	public static final EnumProperty<PanelGutterBlock.Slot> MAIN = EnumProperty.create("main", PanelGutterBlock.Slot.class,
			PanelGutterBlock.Slot.ON, PanelGutterBlock.Slot.OFF, PanelGutterBlock.Slot.TRIPPED);

	private final Voltage voltage;
	private final int mainAmps;
	private final int spaces;

	public PanelBlock(Properties properties, Voltage voltage, int height, int mainAmps, int spaces, String shapeName) {
		super(properties, Footprint.box(1, height, 1, 0, 0), shapeName);
		this.voltage = voltage;
		this.mainAmps = mainAmps;
		this.spaces = spaces;
		registerDefaultState(defaultBlockState().setValue(MAIN, PanelGutterBlock.Slot.ON));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(MAIN);
	}

	/** {@link Voltage#LOW} for a load centre, {@link Voltage#INDUSTRIAL} for a panelboard. */
	public Voltage voltage() {
		return voltage;
	}

	public int mainAmps() {
		return mainAmps;
	}

	/** Its space count: the poles its branch breakers may add up to. */
	public int spaces() {
		return spaces;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new PanelBlockEntity(pos, state);
	}

	private static @Nullable PanelBlockEntity panel(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(BigParts.main(pos, state)) instanceof PanelBlockEntity panel ? panel : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		PanelBlockEntity panel = panel(level, pos, state);
		if (level.isClientSide() || panel == null) {
			return InteractionResult.SUCCESS;
		}
		boolean closing = !panel.closed();
		panel.setMain(closing);
		level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.9F, closing ? 0.9F : 0.75F);
		player.sendOverlayMessage(panel.inspect());
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		CircuitManager.markDirty(level);
	}
}
