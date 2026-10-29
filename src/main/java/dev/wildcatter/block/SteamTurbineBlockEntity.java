package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A 110 V generator. Its output sags as the current rises (it has some resistance of its own), and it only draws as
 * much steam from the boiler as the circuit is actually using. If the boiler cannot keep up, the voltage falls:
 * a brownout, not a blackout.
 */
public class SteamTurbineBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	private int hot;
	private int neutral;
	private int source;
	private double volts;
	private double amps;
	/** What share of the demanded steam the boiler is managing to supply (1 = all of it). */
	private double satisfaction = 1.0;
	/** Ticks since it last had a load; the rotor keeps spinning for a second so the block doesn't flicker. */
	private int idleTicks = 100;

	public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STEAM_TURBINE_ENTITY, pos, state);
	}

	public double watts() {
		return Math.max(0, volts * amps);
	}

	/** True if there is voltage on its terminals to bite with. */
	public boolean isLive() {
		return volts > Tuning.SHOCK_MIN_VOLTS;
	}

	@Override
	public boolean hasPort(Direction face, Role role, Voltage voltage) {
		return voltage == Voltage.LOW;
	}

	@Override
	public void build(Circuit circuit) {
		hot = circuit.node();
		neutral = circuit.node();
		int internal = circuit.node();
		source = circuit.source(internal, neutral, Voltage.LOW.nominal() * satisfaction);
		circuit.resistor(internal, hot, Tuning.TURBINE_INTERNAL_OHMS);
		circuit.ground(neutral);
		for (Direction face : Direction.values()) {
			circuit.port(worldPosition, face, Role.HOT, Voltage.LOW, hot, Tuning.CONTACT_OHMS / 2, false);
			circuit.port(worldPosition, face, Role.NEUTRAL, Voltage.LOW, neutral, Tuning.CONTACT_OHMS / 2, false);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = circuit.across(hot, neutral);
		amps = circuit.sourceCurrent(source);
	}

	/** Every tick: take the steam the circuit is using this tick from the boilers touching it. */
	public static void tick(Level level, BlockPos pos, BlockState state, SteamTurbineBlockEntity turbine) {
		double joules = turbine.watts() / 20.0;
		int wanted = (int) Math.ceil(joules / Tuning.JOULES_PER_STEAM);
		double share = 1.0;
		if (wanted > 0) {
			int got = 0;
			for (Direction side : Direction.values()) {
				if (got < wanted && level.getBlockEntity(pos.relative(side)) instanceof BoilerBlockEntity boiler) {
					got += boiler.takeSteam(wanted - got);
				}
			}
			share = Math.min(1.0, got / (joules / Tuning.JOULES_PER_STEAM));
			turbine.idleTicks = 0;
		} else {
			turbine.idleTicks = Math.min(turbine.idleTicks + 1, 100);
		}
		turbine.satisfaction += (share - turbine.satisfaction) * Tuning.TURBINE_STEAM_SMOOTHING;

		boolean running = turbine.idleTicks < 20;
		if (state.getValue(SteamTurbineBlock.RUNNING) != running) {
			level.setBlock(pos, state.setValue(SteamTurbineBlock.RUNNING, running), 3);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("Steam turbine (110 V): " + String.format("%.0f V, %.1f A, %.0f W", volts, amps, watts())
				+ (satisfaction < 0.98 ? ", STARVED of steam (" + Math.round(satisfaction * 100) + "%)" : "")
				+ "; " + (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("satisfaction", satisfaction);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		satisfaction = input.getDoubleOr("satisfaction", 1.0);
	}
}
