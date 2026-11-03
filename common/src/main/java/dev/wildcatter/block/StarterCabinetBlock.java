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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A cabinet holding one {@link Starter} of its kind at true size: the 480 V soft starter and drive cabinets, the 4.16 kV soft
 * starter (docs/multiblocks/soft-starter.md, vfd.md), and the 4.16 kV drive lineup's output and control section, which checks its
 * lineup (see {@link DriveLineup}). Right-click starts and stops it (or resets a trip), redstone runs it, sneak and right-click
 * steps its full-load amps dial; sneaking with the Power Meter steps a soft starter's current limit.
 */
public class StarterCabinetBlock extends BigPartBlock {
	private final Starter.Kind kind;
	private final Voltage voltage;
	private final double[] dial;
	private final boolean lineup;

	public StarterCabinetBlock(Properties properties, Footprint footprint, String shapeName, Starter.Kind kind, Voltage voltage, double[] dial, boolean lineup) {
		super(properties, footprint, shapeName);
		this.kind = kind;
		this.voltage = voltage;
		this.dial = dial;
		this.lineup = lineup;
	}

	public Starter.Kind kind() {
		return kind;
	}

	public Voltage voltage() {
		return voltage;
	}

	public double[] dial() {
		return dial;
	}

	/** The 4.16 kV drive's control section: its line comes in at the far end of its lineup. */
	public boolean lineup() {
		return lineup;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new StarterCabinetBlockEntity(pos, state);
	}

	private static @Nullable StarterCabinetBlockEntity cabinet(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(BigParts.main(pos, state)) instanceof StarterCabinetBlockEntity cabinet ? cabinet : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		StarterCabinetBlockEntity cabinet = cabinet(level, pos, state);
		if (stack.getItem() instanceof PowerMeterItem && player.isShiftKeyDown() && kind == Starter.Kind.SOFT && cabinet != null) {
			if (!level.isClientSide()) {
				cabinet.nextLimitSetting();
				player.sendOverlayMessage(cabinet.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, BigParts.main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		StarterCabinetBlockEntity cabinet = cabinet(level, pos, state);
		if (level.isClientSide() || cabinet == null) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			cabinet.nextOverloadSetting();
		} else {
			cabinet.pushButton();
			level.playSound(null, pos, WildcatterSounds.BREAKER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
		}
		player.sendOverlayMessage(cabinet.inspect());
		CircuitManager.markDirty(level);
		return InteractionResult.SUCCESS;
	}

	/** A redstone signal on any of its cells starts it; losing the signal stops it. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		StarterCabinetBlockEntity cabinet = cabinet(level, pos, state);
		if (!level.isClientSide() && cabinet != null) {
			cabinet.redstone(level.hasNeighborSignal(pos));
		}
		CircuitManager.markDirty(level);
	}
}
