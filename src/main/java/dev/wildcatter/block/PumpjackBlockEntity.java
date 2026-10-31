package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
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
 * and oil is also sent along gas lines to hungry fireboxes. Its 480 V three-phase motor runs only while there is oil and tank room,
 * and the pump slows as the field loses pressure.
 */
public class PumpjackBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable, dev.wildcatter.kinetic.Sounding {
	/** Its induction motor (480 V three-phase); it turns only while there is work. */
	private final dev.wildcatter.circuit.Motor motor = new dev.wildcatter.circuit.Motor(true, Voltage.INDUSTRIAL, Tuning.PUMPJACK_WATTS);
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	/** Set by the machine's own logic: whether it has work for the motor. */
	private boolean wanted;
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

	/** Game test hook: fills the tank. */
	public void testFill(double units) {
		tank = Math.min(Tuning.PUMPJACK_TANK, tank + units);
		setChanged();
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

	public dev.wildcatter.circuit.Motor motor() {
		return motor;
	}

	/** Sneak and right-click with an empty hand: swap two leads at the motor's terminal box, which reverses its rotation. */
	public Component swapLeads() {
		motor.setLeadsSwapped(!motor.leadsSwapped());
		setChanged();
		return Component.literal("Swapped leads B and C at the motor: it will now turn " + (motor.leadsSwapped() ? "the other way" : "the way it was wired"));
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == motor.voltage() && (motor.threePhase() ? role != Conductor.N : true);
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, wanted);
		volts = motor.volts();
		amps = motor.amps();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PumpjackBlockEntity jack) {
		jack.running.update(jack, jack.motor.output() > 0);   // the client plays its loop while it runs
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0 || !(level instanceof ServerLevel server)) {
			return;
		}
		Reservoir field = Reservoir.at(ReservoirData.seed(server), pos.getX(), pos.getZ());
		ReservoirData data = ReservoirData.get(server);
		jack.wanted = field != null && data.remaining(field) > 0 && jack.tank < Tuning.PUMPJACK_TANK;
		double strength = jack.motor.output();
		if (field == null || data.remaining(field) <= 0) {
			jack.status = "DEPLETED: the well has run dry";
		} else if (jack.tank >= Tuning.PUMPJACK_TANK) {
			jack.status = "tank full, motor stopped: empty it with buckets or burn it";
		} else if (strength == 0) {
			jack.status = jack.motor.reversed() ? "lifting nothing, " + jack.motor.describe() : jack.motor.describe();
		} else {
			double pressure = Math.max(0.1, Math.min(1.0, data.pressure(field) * 1.25));
			double want = Math.min(Tuning.PUMPJACK_TANK - jack.tank, Tuning.PUMPJACK_UNITS_PER_TICK * Tuning.WELL_INTERVAL * strength * pressure);
			jack.carry += want;
			int whole = (int) jack.carry;
			if (whole > 0) {
				jack.tank += data.draw(field, whole);
				jack.carry -= whole;
			}
			jack.status = "pumping at " + Math.round(strength * pressure * 100) + "% of full rate, " + jack.motor.describe();
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
		return Component.literal("Pumpjack (" + String.format("%.1f", Tuning.PUMPJACK_WATTS / 1000) + " kW motor, " + Voltage.INDUSTRIAL.label() + " three-phase): " + field + "tank " + (int) tank + "/" + Tuning.PUMPJACK_TANK + " (a bucket is " + Tuning.CRUDE_BUCKET_UNITS + "), " + status + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("tank", tank);
		output.putBoolean("leadsSwapped", motor.leadsSwapped());
		output.putDouble("carry", carry);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		tank = input.getDoubleOr("tank", 0);
		motor.setLeadsSwapped(input.getBooleanOr("leadsSwapped", false));
		carry = input.getDoubleOr("carry", 0);
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? WildcatterSounds.MOTOR_HUM : null;
	}

	@Override
	public float soundPitch() {
		return 0.8F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.5F : 0;
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return running.tag();
	}
}
