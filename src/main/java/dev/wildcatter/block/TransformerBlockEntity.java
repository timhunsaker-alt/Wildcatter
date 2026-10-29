package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.EnergyBuffer;
import dev.wildcatter.energy.EnergyNet;
import dev.wildcatter.energy.EnergyNode;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Two buffers, one per voltage. Each tick it moves energy from the fuller side to the emptier one, losing a few percent. */
public class TransformerBlockEntity extends BlockEntity implements EnergyNode, Inspectable, Bondable {
	private final EnergyBuffer lowSide = new EnergyBuffer(Tuning.TRANSFORMER_BUFFER, Tuning.TRANSFORMER_THROUGHPUT, Tuning.TRANSFORMER_THROUGHPUT);
	private final EnergyBuffer highSide = new EnergyBuffer(Tuning.TRANSFORMER_BUFFER, Tuning.TRANSFORMER_THROUGHPUT, Tuning.TRANSFORMER_THROUGHPUT);
	private int lost;

	public TransformerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TRANSFORMER_ENTITY, pos, state);
	}

	private TransformerBlock block() {
		return (TransformerBlock) getBlockState().getBlock();
	}

	private Direction highDirection() {
		return getBlockState().getValue(TransformerBlock.FACING);
	}

	@Override
	public EnergyBuffer energy() {
		return lowSide;
	}

	@Override
	public EnergyBuffer energyAt(Direction side) {
		return side == highDirection() ? highSide : lowSide;
	}

	@Override
	public Voltage voltageAt(Direction side) {
		if (side == highDirection()) {
			return block().high();
		}
		return side == highDirection().getOpposite() ? block().low() : null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TransformerBlockEntity t) {
		// Move energy across toward the emptier side.
		EnergyBuffer from = t.lowSide.fraction() > t.highSide.fraction() ? t.lowSide : t.highSide;
		EnergyBuffer to = from == t.lowSide ? t.highSide : t.lowSide;
		int taken = from.extract(Math.min(from.available(), (int) (to.room() / Tuning.TRANSFORMER_EFFICIENCY)));
		int delivered = to.insert((int) (taken * Tuning.TRANSFORMER_EFFICIENCY));
		t.lost = taken - delivered;

		Direction high = t.highDirection();
		EnergyNet.push(level, pos, t.highSide, t.block().high(), high);
		EnergyNet.push(level, pos, t.lowSide, t.block().low(), high.getOpposite());
		if (taken > 0) {
			t.setChanged();
		}
	}

	/** True if there is power in it to bite with. */
	public boolean isLive() {
		return lowSide.stored() > 0 || highSide.stored() > 0;
	}

	/** The highest voltage in here: that is the one that decides how bad a shock is. */
	public Voltage topVoltage() {
		return block().high();
	}

	@Override
	public Component inspect() {
		return Component.literal("Transformer " + block().low().label() + " / " + block().high().label()
				+ ": " + lowSide.stored() + " RF on the " + block().low().label() + " side, "
				+ highSide.stored() + " RF on the " + block().high().label() + " side, losing " + lost + " RF/t as heat; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("low", lowSide.stored());
		output.putInt("high", highSide.stored());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lowSide.set(input.getIntOr("low", 0));
		highSide.set(input.getIntOr("high", 0));
	}
}
