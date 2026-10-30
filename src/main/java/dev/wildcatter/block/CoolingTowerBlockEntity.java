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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A 110 V fan. It cools only while it has near-full voltage and enough fill sections stacked above it. */
public class CoolingTowerBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	private int hot;
	private int neutral;
	private int resistor;
	private double volts;
	private double amps;
	private int fill;
	private boolean cooling;

	public CoolingTowerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COOLING_TOWER_ENTITY, pos, state);
	}

	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
	}

	/** True while the tower is actually removing heat. */
	public boolean isCooling() {
		return cooling;
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
		resistor = circuit.resistor(hot, neutral, nominal * nominal / Tuning.COOLING_FAN_WATTS);
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

	public static void tick(Level level, BlockPos pos, BlockState state, CoolingTowerBlockEntity tower) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.PLUMBING_INTERVAL != 0) {
			return;
		}
		int n = 0;
		while (n < Tuning.COOLING_FILL && level.getBlockState(pos.above(n + 1)).is(WildcatterBlocks.COOLING_FILL)) {
			n++;
		}
		tower.fill = n;
		boolean was = tower.cooling;
		tower.cooling = n >= Tuning.COOLING_FILL && tower.volts >= Voltage.LOW.nominal() * Tuning.BRIGHT_ABOVE;
		if (tower.cooling && !was) {
			level.playSound(null, pos.above(), WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.0F, 1.1F);
		}
		if (tower.cooling && (level.getGameTime() / Tuning.PLUMBING_INTERVAL) % 15 == 0) {
			level.playSound(null, pos, WildcatterSounds.TURBINE_HUM, SoundSource.BLOCKS, 0.4F, 0.6F);
		}
	}

	@Override
	public Component inspect() {
		String state = cooling ? "COOLING" : fill < Tuning.COOLING_FILL ? "needs " + Tuning.COOLING_FILL + " fill sections above it (has " + fill + ")"
				: volts < 1.0 ? "NO POWER: wire a hot and a neutral to the fan" : "too little voltage for the fan";
		return Component.literal("Cooling tower (rated " + Math.round(Tuning.COOLING_FAN_WATTS) + " W at " + Voltage.LOW.label() + "): " + String.format("%.0f V, %.2f A", volts, amps)
				+ ", " + state + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}
}
