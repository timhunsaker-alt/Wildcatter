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
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Oil does not always flow by itself, so a 110 V pumpjack lifts it into a tank. The tank can be drained with buckets,
 * and oil is also sent along gas lines to hungry fireboxes. The pump draws its power all the time, full tank or not,
 * and slows as the field loses pressure.
 */
public class PumpjackBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable {
	private int hot;
	private int neutral;
	private int resistor;
	private double volts;
	private double amps;
	private double tank;
	private double carry;
	private String status = "starting";

	public PumpjackBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PUMPJACK_ENTITY, pos, state);
	}

	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
	}

	/** Takes oil out of the tank for a bucket; false if there is not enough. */
	public boolean takeUnits(int units) {
		if (tank < units) {
			return false;
		}
		tank -= units;
		setChanged();
		return true;
	}

	/** Lets a refinery on the gas lines draw crude from the tank: returns how much it got. */
	public double take(double max) {
		double got = Math.min(max, tank);
		if (got > 0) {
			tank -= got;
			setChanged();
		}
		return got;
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
		resistor = circuit.resistor(hot, neutral, nominal * nominal / Tuning.PUMPJACK_WATTS);
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

	public static void tick(Level level, BlockPos pos, BlockState state, PumpjackBlockEntity jack) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0 || !(level instanceof ServerLevel server)) {
			return;
		}
		Reservoir field = Reservoir.at(ReservoirData.seed(server), pos.getX(), pos.getZ());
		ReservoirData data = ReservoirData.get(server);
		double nominal = Voltage.LOW.nominal();
		double strength = jack.volts < nominal * Tuning.BRIGHT_ABOVE ? 0 : Math.min(1.0, Math.pow(jack.volts / nominal, 2));
		if (field == null || data.remaining(field) <= 0) {
			jack.status = "DEPLETED: the well has run dry";
		} else if (strength == 0) {
			jack.status = jack.volts < 1.0 ? "NO POWER: wire a hot and a neutral to it" : "too little voltage to turn";
		} else if (jack.tank >= Tuning.PUMPJACK_TANK) {
			jack.status = "tank full: empty it with buckets or burn it";
		} else {
			double pressure = Math.max(0.1, Math.min(1.0, data.pressure(field) * 1.25));
			double want = Math.min(Tuning.PUMPJACK_TANK - jack.tank, Tuning.PUMPJACK_UNITS_PER_TICK * Tuning.WELL_INTERVAL * strength * pressure);
			jack.carry += want;
			int whole = (int) jack.carry;
			if (whole > 0) {
				jack.tank += data.draw(field, whole);
				jack.carry -= whole;
			}
			jack.status = "pumping at " + Math.round(strength * pressure * 100) + "% of full rate";
			if ((level.getGameTime() / Tuning.WELL_INTERVAL) % 10 == 0) {
				level.playSound(null, pos, WildcatterSounds.TURBINE_HUM, SoundSource.BLOCKS, 0.5F, 0.7F);
			}
			jack.setChanged();
		}
		if (jack.tank > 0) {
			List<BlockPos> cell = List.of(pos);
			Set<BlockPos> lines = Plumbing.network(level, cell, PipeBlock.Kind.GAS);
			List<FireboxBlockEntity> fireboxes = Plumbing.fireboxes(level, cell, lines, true);
			double sent = Plumbing.feedFireboxes(fireboxes, jack.tank * Tuning.OIL_FUEL_TICKS_PER_UNIT);
			if (sent > 0) {
				jack.tank -= sent / Tuning.OIL_FUEL_TICKS_PER_UNIT;
				jack.setChanged();
			}
		}
	}

	@Override
	public Component inspect() {
		String field = "";
		if (level instanceof ServerLevel server) {
			Reservoir reservoir = Reservoir.at(ReservoirData.seed(server), worldPosition.getX(), worldPosition.getZ());
			if (reservoir != null) {
				ReservoirData data = ReservoirData.get(server);
				field = Math.round(data.pressure(reservoir) * 100) + "% pressure, about " + data.remaining(reservoir) + " units left in the field; ";
			}
		}
		return Component.literal("Pumpjack (rated " + Math.round(Tuning.PUMPJACK_WATTS) + " W at 110 V): " + String.format("%.0f V, %.2f A", volts, amps)
				+ ", " + field + "tank " + (int) tank + "/" + Tuning.PUMPJACK_TANK + " (a bucket is " + Tuning.CRUDE_BUCKET_UNITS + "), " + status + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("tank", tank);
		output.putDouble("carry", carry);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tank = input.getDoubleOr("tank", 0);
		carry = input.getDoubleOr("carry", 0);
	}
}
