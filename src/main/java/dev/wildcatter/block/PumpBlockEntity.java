package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Role;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A 110 V motor that lifts water from the water touching it and sends it down the pipes to every boiler on the run.
 * It is a plain load on the circuit, so it draws about 7 A whether or not the boilers need water. Water pressure
 * follows the motor: it needs most of its voltage, and delivers with the square of it, like a real pump.
 */
public class PumpBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	private int hot;
	private int neutral;
	private int resistor;
	private double volts;
	private double amps;
	private String status = "idle";

	public PumpBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PUMP_ENTITY, pos, state);
	}

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
		resistor = circuit.resistor(hot, neutral, nominal * nominal / Tuning.PUMP_WATTS);
		for (Direction face : Direction.values()) {
			circuit.port(worldPosition, face, Role.HOT, Voltage.LOW, hot, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, face, Role.NEUTRAL, Voltage.LOW, neutral, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = circuit.across(hot, neutral);
		amps = circuit.current(resistor);
	}

	private boolean touchesWater(Level level) {
		for (Direction side : Direction.values()) {
			if (level.getFluidState(worldPosition.relative(side)).is(FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.PLUMBING_INTERVAL != 0) {
			return;
		}
		double nominal = Voltage.LOW.nominal();
		double strength = pump.volts < nominal * Tuning.BRIGHT_ABOVE ? 0 : Math.min(1.0, Math.pow(pump.volts / nominal, 2));
		if (strength == 0) {
			pump.status = pump.volts < 1.0 ? "NO POWER: wire a hot and a neutral to it" : "too little voltage to turn";
			return;
		}
		if (!pump.touchesWater(level)) {
			pump.status = "DRY: put it against water";
			return;
		}
		List<BlockPos> cell = List.of(pos);
		Set<BlockPos> pipes = Plumbing.network(level, cell);
		List<BoilerBlockEntity> boilers = Plumbing.boilers(level, cell, pipes, true);
		if (boilers.isEmpty()) {
			pump.status = "no boiler on its pipes";
			return;
		}
		int moved = Plumbing.deliver(boilers, (int) Math.round(Tuning.PUMP_WATER_PER_TICK * Tuning.PLUMBING_INTERVAL * strength));
		pump.status = moved > 0 ? "pumping " + Math.round(moved * 20.0 / Tuning.PLUMBING_INTERVAL) + " water/s to " + boilers.size() + " boiler(s)"
				: "running, boilers are full";
		if (moved > 0 && (level.getGameTime() / Tuning.PLUMBING_INTERVAL) % 15 == 0) {
			level.playSound(null, pos, WildcatterSounds.TURBINE_HUM, SoundSource.BLOCKS, 0.4F, 1.5F);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("Water pump (rated " + Math.round(Tuning.PUMP_WATTS) + " W at 110 V): "
				+ String.format("%.0f V, %.2f A", volts, amps) + ", " + status + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}
}
