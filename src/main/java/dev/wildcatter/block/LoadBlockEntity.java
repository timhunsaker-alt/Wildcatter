package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Role;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A resistor between hot and neutral. Works if it gets enough voltage, and burns out if it gets far too much. */
public class LoadBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	private int hot;
	private int neutral;
	private int resistor;
	private double volts;
	private double amps;
	private int overVoltageTicks;

	public LoadBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LOAD_ENTITY, pos, state);
	}

	private double watts() {
		return ((LoadBlock) getBlockState().getBlock()).watts();
	}

	/** True if there is voltage across it to bite with. */
	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
	}

	@Override
	public boolean hasPort(Direction face, Role role, Voltage voltage) {
		return voltage == Voltage.LOW;
	}

	@Override
	public void build(Circuit circuit) {
		hot = circuit.node();
		neutral = circuit.node();
		double nominal = Voltage.LOW.nominal();
		resistor = circuit.resistor(hot, neutral, nominal * nominal / watts()); // P = V^2 / R
		for (Direction face : Direction.values()) {
			circuit.port(worldPosition, face, Role.HOT, Voltage.LOW, hot, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, face, Role.NEUTRAL, Voltage.LOW, neutral, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		volts = circuit.across(hot, neutral);
		amps = circuit.current(resistor);
		double nominal = Voltage.LOW.nominal();

		boolean working = volts > nominal * Tuning.BRIGHT_ABOVE;
		BlockState state = getBlockState();
		if (state.getValue(LoadBlock.LIT) != working) {
			server.setBlock(worldPosition, state.setValue(LoadBlock.LIT, working), 3);
		}

		overVoltageTicks = volts > nominal * Tuning.BURNOUT_ABOVE ? overVoltageTicks + ticks : 0;
		if (overVoltageTicks >= Tuning.BURNOUT_TICKS) {
			server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
			server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.4F);
			server.destroyBlock(worldPosition, false);
			CircuitManager.markDirty(server);
		}
	}

	@Override
	public Component inspect() {
		String what = ((LoadBlock) getBlockState().getBlock()).watts() >= 1000 ? "Electric heater" : "Electric lamp";
		String state;
		if (volts < 1.0) {
			state = "NO POWER: check that both a hot and a neutral wire reach it, and that the breaker is on";
		} else if (volts < Voltage.LOW.nominal() * Tuning.BRIGHT_ABOVE) {
			state = "too little voltage to work";
		} else {
			state = "running";
		}
		return Component.literal(what + " (rated " + Math.round(watts()) + " W at 110 V): " + String.format("%.0f V, %.2f A, %.0f W", volts, amps, volts * amps)
				+ ", " + state + "; " + (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}
}
