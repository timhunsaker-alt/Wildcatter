package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.kinetic.Kinetic;
import dev.wildcatter.kinetic.RotorBlock;
import dev.wildcatter.kinetic.Spin;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The gas plant's two motor-driven packages (docs/multiblocks/gas-plant.md).
 * <ul>
 * <li>The gas compressor: a two-stage reciprocating compressor on a 400 hp 4.16 kV six-pole motor. It draws separator gas at its
 * suction (the front end) and gives compressed gas out of its discharge (the back end), 1.0 m3/s at full voltage. The motor runs
 * only while gas waits and the discharge has room; the cooler's fan, driven off the motor, turns with it.</li>
 * <li>The refrigeration compressor: a 150 hp 480 V propane screw package with no pipe of its own. It runs while a chiller its skid
 * touches asks for cold.</li>
 * </ul>
 */
public class GasCompressorBlockEntity extends PlantUnitBlockEntity implements CircuitPart, Bondable, Kinetic, dev.wildcatter.kinetic.Sounding {
	private @Nullable Motor motor;
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private final Spin spin = new Spin();
	private boolean wanted;
	private double volts;
	private double compressed;
	/** Intervals the gas compressor has had nothing to do: it unloads and keeps turning, and stops after {@link Tuning#GAS_COMPRESSOR_UNLOADED_SECONDS}. */
	private int unloaded;
	/** For the refrigeration compressor: the game time a chiller last asked for cold. */
	private long called = -1000;

	public GasCompressorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GAS_COMPRESSOR_ENTITY, pos, state);
	}

	private boolean refrigeration() {
		return getBlockState().getBlock() instanceof GasPlantPartBlock part && part.part() == GasPlantPartBlock.Part.REFRIGERATION_COMPRESSOR;
	}

	public Motor motor() {
		if (motor == null) {
			motor = refrigeration() ? new Motor(true, Voltage.INDUSTRIAL, Tuning.REFRIGERATION_WATTS) : new Motor(true, Voltage.MEDIUM, Tuning.GAS_COMPRESSOR_WATTS);
		}
		return motor;
	}

	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
	}

	/** A chiller touching the refrigeration compressor asks for cold; true if it is running and cooling. */
	public boolean cool(Level level) {
		called = level.getGameTime();
		return motor().output() > 0 && !motor().reversed();
	}

	/** How hard the refrigeration compressor is cooling, 0 to 1. */
	public double cooling() {
		return motor().reversed() ? 0 : motor().output();
	}

	// ---- nozzles --------------------------------------------------------------------------------------------------------------
	private List<Lines.Face> suction() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return !refrigeration() && at[2] == 8 && at[1] == 0 && side == Direction.SOUTH ? Product.COMPRESSED_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.GAS_COMPRESSOR_M3S * Tuning.PLANT_HOLD_SECONDS;
	}

	// ---- the motor's terminal box: the gas compressor's on the motor's west side, the refrigeration compressor's on its main cell --
	private BlockPos terminal() {
		return refrigeration() ? worldPosition : LeasePartBlock.at(worldPosition, getBlockState(), 0, 0, 4);
	}

	private List<Direction> boxFaces() {
		return List.of(LeasePartBlock.world(getBlockState(), Direction.WEST));
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(terminal()) && boxFaces().contains(face) && voltage == motor().voltage() && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		motor().build(circuit, terminal(), boxFaces());
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor().apply(circuit, ticks, wanted);
		volts = motor().volts();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, GasCompressorBlockEntity c) {
		Motor motor = c.motor();
		c.running.update(c, motor.output() > 0);
		boolean turning = motor.state() == Motor.State.RUNNING && !motor.stalled();
		double rpm = c.refrigeration() ? 120.0 * ElectricalStandard.active().hertz() / 2 * (1 - Tuning.INJECTION_PUMP_SLIP)
				: 120.0 * ElectricalStandard.active().hertz() / Tuning.GAS_COMPRESSOR_POLES * (1 - Tuning.GAS_COMPRESSOR_SLIP);
		c.spin.setRpm(turning ? (float) (rpm * (motor.reversed() ? -1 : 1)) : 0);
		if (c.spin.needsSync()) {
			c.spin.markSynced();
			level.sendBlockUpdated(pos, state, state, 3);
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		if (c.refrigeration()) {
			c.wanted = level.getGameTime() - c.called < 40;
			c.status = !c.wanted ? "idle: no chiller beside it is asking for cold" : motor.output() > 0 && !motor.reversed() ? "cooling the chiller, " + motor.describe() : motor.describe();
			c.setChanged();
			return;
		}
		double waiting = Lines.available(level, c.suction(), Product.GAS, Pull.MACHINE, c);
		double room = c.room(Product.COMPRESSED_GAS);
		boolean work = waiting >= 0.05 && room >= 0.05;
		// it starts with gas waiting and half its discharge free; with nothing to do it unloads its cylinders and keeps turning
		c.unloaded = work ? 0 : c.unloaded + 1;
		if (!c.wanted) {
			c.wanted = waiting >= 0.5 && room >= c.hold(Product.COMPRESSED_GAS) / 2;
		} else if (c.unloaded * seconds() >= Tuning.GAS_COMPRESSOR_UNLOADED_SECONDS) {
			c.wanted = false;
		}
		c.compressed = 0;
		if (!work) {
			String why = waiting < 0.05 ? "no separator gas at its suction (the front end)" : "its discharge is full (pipe the back end to a dehydrator)";
			c.status = c.wanted && motor.output() > 0 ? "running UNLOADED: " + why + ", " + motor.describe() : "idle: " + why;
		} else if (motor.output() == 0) {
			c.status = motor.describe();
		} else if (motor.reversed()) {
			c.status = "compressing nothing: " + motor.describe();
		} else {
			c.compressed = Lines.pull(level, c.suction(), Product.GAS, Math.min(room, Tuning.GAS_COMPRESSOR_M3S * seconds() * motor.output()), Pull.MACHINE, c);
			c.add(Product.COMPRESSED_GAS, c.compressed);
			c.status = String.format("compressing %.2f m3/s, 125 to 1,000 psi, %s", c.compressed / seconds(), motor.describe());
		}
		c.setChanged();
	}

	@Override
	public Component inspect() {
		String name = refrigeration() ? "Refrigeration compressor (propane screw, 150 hp, " + Voltage.INDUSTRIAL.label() + " three-phase)"
				: "Gas compressor (two-stage reciprocating, 400 hp, " + Voltage.MEDIUM.label() + " three-phase)";
		return Component.literal(name + ": " + status + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	// ---- moving parts ---------------------------------------------------------------------------------------------------------
	@Override
	public List<Rotor> rotors() {
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		return refrigeration()
				? List.of(new Rotor(rotor.part(RotorBlock.Part.REFRIGERATION_COUPLING), new Vector3f(1.5F, 1.0F, 2.6F), Direction.Axis.Z, 1.0F))
				: List.of(new Rotor(rotor.part(RotorBlock.Part.GAS_COMPRESSOR_COUPLING), new Vector3f(2.0F, 1.0F, 3.55F), Direction.Axis.Z, 1.0F),
						new Rotor(rotor.part(RotorBlock.Part.GAS_COMPRESSOR_FAN), new Vector3f(2.0F, 2.0F, 7.4F), Direction.Axis.Z, 1.0F));
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(GasPlantPartBlock.FACING);
	}

	@Override
	public Spin spin() {
		return spin;
	}

	@Override
	public float displayDivisor() {
		return refrigeration() ? 20 : 10;   // the six-pole set at rpm / 10, so its blades and coupling read
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? WildcatterSounds.MOTOR_HUM : null;
	}

	@Override
	public float soundPitch() {
		return refrigeration() ? 1.2F : 0.6F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.8F : 0;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("leadsSwapped", motor().leadsSwapped());
		output.putFloat("rpm", spin.rpm());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		motor().setLeadsSwapped(input.getBooleanOr("leadsSwapped", false));
		spin.setRpm(input.getFloatOr("rpm", 0));
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = running.tag();
		tag.putFloat("rpm", spin.rpm());
		return tag;
	}
}
