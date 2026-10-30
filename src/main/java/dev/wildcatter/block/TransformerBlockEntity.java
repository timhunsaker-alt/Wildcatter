package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Two circuits at different voltages, joined only by magnetism: the power that goes in one side comes out the other,
 * with the volts and amps traded against each other (a 4.16 kV side carries 1/38th of the current of the 110 V side).
 * The windings have a little resistance, which is where the heat and the few percent of loss come from.
 * The neutral on each side is tied to ground, as it is at a real service entrance.
 */
public class TransformerBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	private int lowHotPort;
	private int highHotPort;
	private int lowNeutral;
	private int lowBPort;
	private int lowBWinding = -1;
	private double lowBVolts;
	private double lowBAmps;
	private boolean split;
	private int highNeutral;
	private int lowWinding = -1;
	private int highWinding = -1;
	private double lowVolts;
	private double highVolts;
	private double lowAmps;
	private double highAmps;
	private double lossWatts;

	public TransformerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TRANSFORMER_ENTITY, pos, state);
	}

	private TransformerBlock block() {
		return (TransformerBlock) getBlockState().getBlock();
	}

	private Direction highFace() {
		return getBlockState().getValue(TransformerBlock.FACING);
	}

	public boolean isLive() {
		return Math.max(Math.abs(lowVolts), Math.abs(highVolts)) > Tuning.SHOCK_MIN_VOLTS;
	}

	public Voltage topVoltage() {
		return block().high();
	}

	/** The high-voltage terminals are on the front and the top (where real bushings are); the low side is the other four faces, so a split-phase secondary has room for A, B and N. */
	private boolean isHighFace(Direction face) {
		return face == highFace() || face == Direction.UP;
	}

	private boolean isLowFace(Direction face) {
		return !isHighFace(face);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		if (isHighFace(face)) {
			return voltage == block().high();
		}
		return isLowFace(face) && voltage == block().low();
	}

	@Override
	public void build(Circuit circuit) {
		TransformerBlock block = block();
		double ratio = block.high().nominal() / block.low().nominal();

		lowHotPort = circuit.node();
		highHotPort = circuit.node();
		lowNeutral = circuit.node();
		highNeutral = circuit.node();
		int lowCoil = circuit.node();
		int highCoil = circuit.node();

		double lowOhms = Tuning.TRANSFORMER_LOW_WINDING_OHMS;
		lowWinding = circuit.resistor(lowHotPort, lowCoil, lowOhms);
		highWinding = circuit.resistor(highHotPort, highCoil, lowOhms * ratio * ratio);
		circuit.transformer(highCoil, highNeutral, lowCoil, lowNeutral, ratio);
		// North American service: the low side is centre-tapped. Winding two runs the other way round, so leg B is 180 degrees from leg A.
		split = block.low() == Voltage.LOW && dev.wildcatter.energy.ElectricalStandard.active().splitPhase();
		lowBPort = circuit.node();
		if (split) {
			int lowBCoil = circuit.node();
			lowBWinding = circuit.resistor(lowBPort, lowBCoil, lowOhms);
			circuit.transformer(highCoil, highNeutral, lowNeutral, lowBCoil, ratio);
		} else {
			lowBWinding = -1;
		}
		circuit.ground(lowNeutral);
		circuit.ground(highNeutral);

		// Each conductor can attach to either face of a side, so each wire gets a face of its own.
		for (Direction face : Direction.values()) {
			if (isHighFace(face)) {
				circuit.port(worldPosition, face, Conductor.A, block.high(), highHotPort, Tuning.CONTACT_OHMS / 2, false);
				circuit.port(worldPosition, face, Conductor.N, block.high(), highNeutral, Tuning.CONTACT_OHMS / 2, false);
			} else if (isLowFace(face)) {
				boolean side = face != highFace().getOpposite() && face != Direction.DOWN;
				if (!side) {
					// back and bottom: leg A and the neutral, as a single-phase low side always had
					circuit.port(worldPosition, face, Conductor.A, block.low(), lowHotPort, Tuning.CONTACT_OHMS / 2, false);
					circuit.port(worldPosition, face, Conductor.N, block.low(), lowNeutral, Tuning.CONTACT_OHMS / 2, false);
				} else if (split) {
					// the sides: leg B and the neutral, so a machine touching a side runs on leg B and never bridges the two legs
					circuit.port(worldPosition, face, Conductor.B, block.low(), lowBPort, Tuning.CONTACT_OHMS / 2, false);
					circuit.port(worldPosition, face, Conductor.N, block.low(), lowNeutral, Tuning.CONTACT_OHMS / 2, false);
				}
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		lowVolts = circuit.across(lowHotPort, lowNeutral);
		highVolts = circuit.across(highHotPort, highNeutral);
		lowAmps = circuit.current(lowWinding);
		lowBVolts = split ? circuit.across(lowBPort, lowNeutral) : 0;
		lowBAmps = split ? circuit.current(lowBWinding) : 0;
		highAmps = circuit.current(highWinding);
		double ratio = block().high().nominal() / block().low().nominal();
		lossWatts = lowAmps * lowAmps * Tuning.TRANSFORMER_LOW_WINDING_OHMS + highAmps * highAmps * Tuning.TRANSFORMER_LOW_WINDING_OHMS * ratio * ratio;
	}

	@Override
	public Component inspect() {
		String legB = split ? String.format("leg B %.0f V %.1f A (A to B %.0f V), ", Math.abs(lowBVolts), Math.abs(lowBAmps), Math.abs(lowVolts - lowBVolts)) : "";
		return Component.literal("Transformer " + block().low().label() + " / " + block().high().label() + ": " + legB
				+ String.format("%.0f V %.1f A on the %s side, %.0f V %.2f A on the %s side, losing %.0f W as heat; ",
						lowVolts, Math.abs(lowAmps), block().low().label(), highVolts, Math.abs(highAmps), block().high().label(), lossWatts)
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}
}
