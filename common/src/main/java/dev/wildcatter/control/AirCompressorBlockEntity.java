package dev.wildcatter.control;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.PipeBlock;
import dev.wildcatter.block.Plumbing;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Charges its receiver between a cut-in and a cut-out pressure, and the receiver leaks down slowly, as a real system's does. */
public class AirCompressorBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Measurable {
	private final dev.wildcatter.circuit.Motor motor = new dev.wildcatter.circuit.Motor(true, Voltage.INDUSTRIAL, Tuning.AIR_COMPRESSOR_WATTS);
	private double bar;
	private boolean wanted = true;

	public AirCompressorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.AIR_COMPRESSOR_ENTITY, pos, state);
	}

	public double bar() {
		return bar;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, AirCompressorBlockEntity compressor) {
		double was = compressor.bar;
		compressor.bar = Math.max(0, compressor.bar - Tuning.AIR_LEAK_BAR_PER_SECOND / 20 + compressor.motor.output() * Tuning.AIR_CHARGE_BAR_PER_SECOND / 20);
		boolean want = compressor.wanted ? compressor.bar < Tuning.AIR_CUT_OUT_BAR : compressor.bar < Tuning.AIR_CUT_IN_BAR;
		if (want != compressor.wanted) {
			compressor.wanted = want;
			if (level instanceof net.minecraft.server.level.ServerLevel server) {
				dev.wildcatter.circuit.CircuitManager.markDirty(server);
			}
		}
		if (was != compressor.bar) {
			compressor.setChanged();
		}
	}

	/** The pressure in the tubing touching {@code cell}: through a dryer, from a compressor. 0 with none. */
	public static double pressureAt(Level level, BlockPos cell) {
		Set<BlockPos> tubing = Plumbing.network(level, List.of(cell), PipeBlock.Kind.AIR);
		double best = 0;
		for (BlockPos tube : tubing) {
			for (Direction side : Direction.values()) {
				BlockPos dryer = tube.relative(side);
				if (level.getBlockState(dryer).getBlock() instanceof AirDryerBlock) {
					for (Direction out : Direction.values()) {
						if (level.getBlockEntity(dryer.relative(out)) instanceof AirCompressorBlockEntity compressor) {
							best = Math.max(best, compressor.bar);
						}
					}
				}
			}
		}
		return best;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == motor.voltage() && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, wanted);
	}

	@Override
	public double measure(Quantity quantity) {
		return quantity == Quantity.PRESSURE ? bar : quantity == Quantity.AMPS ? motor.amps() : Double.NaN;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format(Locale.ROOT, "Instrument air compressor: %.1f bar in its receiver, %s", bar, motor.describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("bar", bar);
		output.putBoolean("wanted", wanted);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		bar = input.getDoubleOr("bar", 0);
		wanted = input.getBooleanOr("wanted", true);
	}
}
