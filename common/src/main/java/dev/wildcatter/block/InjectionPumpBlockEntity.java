package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.ElectricalStandard;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.kinetic.Kinetic;
import dev.wildcatter.kinetic.RotorBlock;
import dev.wildcatter.kinetic.Spin;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;

/**
 * A saltwater disposal H-pump (docs/multiblocks/lease.md): a 100 hp (75 kW) 480 V three-phase two-pole motor, the thrust chamber and
 * a multistage pump barrel, about 10 L/s at 6 MPa. It draws produced water at its suction (the back end) and pushes it out of its
 * discharge (on top, near the thrust chamber) into a disposal wellhead. The motor runs only while there is water and a disposal well.
 */
public class InjectionPumpBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable, BigPartEntity, Kinetic, dev.wildcatter.kinetic.Sounding {
	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, Tuning.INJECTION_PUMP_WATTS);
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private final Spin spin = new Spin();
	private boolean wanted;
	/** The level switch: it starts with a tank's worth waiting and runs the water down. */
	private boolean called;
	private double volts;
	private double amps;
	private double pumped;
	private String status = "starting";
	private boolean grown;

	public InjectionPumpBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.INJECTION_PUMP_ENTITY, pos, state);
	}

	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
	}

	public Motor motor() {
		return motor;
	}

	private List<Lines.Face> suction() {
		return LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[2] == 5, Direction.SOUTH);
	}

	private List<Lines.Face> discharge() {
		return LeasePartBlock.faces(worldPosition, getBlockState(), at -> at[2] == 3, Direction.UP);
	}

	// ---- the motor's terminal box: the front and west faces of the main cell ---------------------------------------------------
	private List<Direction> boxFaces() {
		return List.of(LeasePartBlock.world(getBlockState(), Direction.NORTH), LeasePartBlock.world(getBlockState(), Direction.WEST));
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && boxFaces().contains(face) && voltage == motor.voltage() && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition, boxFaces());
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, wanted);
		volts = motor.volts();
		amps = motor.amps();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, InjectionPumpBlockEntity pump) {
		pump.running.update(pump, pump.motor.output() > 0);
		boolean turning = pump.motor.state() == Motor.State.RUNNING && !pump.motor.stalled();
		pump.spin.setRpm(turning ? (float) (120.0 * ElectricalStandard.active().hertz() / 2 * (1 - Tuning.INJECTION_PUMP_SLIP) * (pump.motor.reversed() ? -1 : 1)) : 0);
		if (pump.spin.needsSync()) {
			pump.spin.markSynced();
			level.sendBlockUpdated(pos, state, state, 3);
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		boolean well = false;
		for (Lines.Line line : Lines.lines(level, pump.discharge())) {
			well |= line.touches(level, DisposalWellheadBlock.class);
		}
		double waiting = Lines.available(level, pump.suction(), Product.PRODUCED_WATER, FluidNode.Pull.MACHINE, pump);
		pump.called = waiting >= Tuning.INJECTION_PUMP_START || (pump.called && waiting >= 1);
		pump.wanted = well && pump.called;
		double strength = pump.motor.output();
		pump.pumped = 0;
		if (!well) {
			pump.status = "NO DISPOSAL WELL: pipe its discharge (on top, by the thrust chamber) to a disposal wellhead";
		} else if (!pump.called) {
			pump.status = String.format("idle: %.0f L of produced water at its suction, it starts at %.0f L (the back end)", waiting, Tuning.INJECTION_PUMP_START);
		} else if (strength == 0) {
			pump.status = pump.motor.describe();
		} else if (pump.motor.reversed()) {
			pump.status = "pumping nothing: " + pump.motor.describe();
		} else {
			pump.pumped = Lines.pull(level, pump.suction(), Product.PRODUCED_WATER, Tuning.INJECTION_PUMP_LPS * Tuning.WELL_INTERVAL / 20.0 * strength,
					FluidNode.Pull.MACHINE, pump);
			pump.status = String.format("injecting %.1f L/s, %s", pump.pumped * 20.0 / Tuning.WELL_INTERVAL, pump.motor.describe());
		}
		pump.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Injection pump (H-pump, " + Math.round(Tuning.INJECTION_PUMP_WATTS / 1000) + " kW / 100 hp, " + Voltage.INDUSTRIAL.label()
				+ " three-phase): " + status + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	// ---- moving parts ---------------------------------------------------------------------------------------------------------
	@Override
	public List<Rotor> rotors() {
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		return List.of(new Rotor(rotor.part(RotorBlock.Part.INJECTION_PUMP_COUPLING), new Vector3f(1.0F, 0.55F, 2.15F), Direction.Axis.Z, 1.0F));
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(LeasePartBlock.FACING);
	}

	@Override
	public Spin spin() {
		return spin;
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? WildcatterSounds.MOTOR_HUM : null;
	}

	@Override
	public float soundPitch() {
		return 1.1F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.6F : 0;
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("leadsSwapped", motor.leadsSwapped());
		output.putBoolean("grown", grown);
		output.putBoolean("called", called);
		output.putFloat("rpm", spin.rpm());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		motor.setLeadsSwapped(input.getBooleanOr("leadsSwapped", false));
		grown = input.getBooleanOr("grown", grown);
		called = input.getBooleanOr("called", false);
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
