package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.kinetic.BeamLinkage;
import dev.wildcatter.kinetic.Kinetic;
import dev.wildcatter.kinetic.RotorBlock;
import dev.wildcatter.kinetic.Spin;
import dev.wildcatter.multiblock.BigPartEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;

/**
 * The pumpjack's insides (docs/multiblocks/well-site.md). Oil does not always flow by itself, so the pumpjack lifts it from the
 * field under its {@link OilWellheadBlock} into a tank (standing in for a flowline and stock tank until Phase 5). The tank can be
 * drained with buckets, and crude goes along steel pipe from the wellhead to hungry fireboxes and refineries. Its 480 V
 * three-phase motor runs only while there is oil and tank room, and the pump slows as the field loses pressure. The crank turns at
 * {@link Tuning#PUMPJACK_SPM} and the beam, pitmans and polished rod follow it through {@link BeamLinkage}.
 */
public class PumpjackBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable, BigPartEntity, Kinetic, dev.wildcatter.kinetic.Sounding {
	/** The carrier bar's height with the beam level: 2.1 m, so at the bottom of the stroke it clears the stuffing box. */
	private static final float CARRIER_Y = 2.1F;

	/** Its induction motor (480 V three-phase); it turns only while there is work. */
	private final dev.wildcatter.circuit.Motor motor = new dev.wildcatter.circuit.Motor(true, Voltage.INDUSTRIAL, Tuning.PUMPJACK_WATTS);
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private final Spin spin = new Spin();
	/** Set by the machine's own logic: whether it has work for the motor. */
	private boolean wanted;
	private double volts;
	private double amps;
	private double tank;
	private double carry;
	private boolean grown;
	private String status = "starting";

	public PumpjackBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PUMPJACK_ENTITY, pos, state);
	}

	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
	}

	/** The cell its horsehead hangs over. */
	public BlockPos wellhead() {
		return PumpjackBlock.wellhead(worldPosition, getBlockState());
	}

	private boolean overWell() {
		return level != null && level.getBlockState(wellhead()).is(WildcatterBlocks.OIL_WELLHEAD);
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

	/** Lets a refinery on the steel pipe draw crude from the tank: returns how much it got. */
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

	// ---- the motor's terminal box: the back, left and right faces of the bottom cell at the back ---------------------------------
	private List<Direction> boxFaces() {
		Direction facing = getBlockState().getValue(PumpjackBlock.FACING);
		return List.of(facing.getOpposite(), facing.getClockWise(), facing.getCounterClockWise());
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(PumpjackBlock.terminalBox(worldPosition, getBlockState())) && boxFaces().contains(face)
				&& voltage == motor.voltage() && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, PumpjackBlock.terminalBox(worldPosition, getBlockState()), boxFaces());
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, wanted);
		volts = motor.volts();
		amps = motor.amps();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PumpjackBlockEntity jack) {
		jack.running.update(jack, jack.motor.output() > 0);   // the client plays its loop while it runs
		// the crank turns at its strokes a minute while the motor runs, backward with its leads swapped
		boolean turning = jack.motor.state() == dev.wildcatter.circuit.Motor.State.RUNNING && !jack.motor.stalled();
		jack.spin.setRpm(turning ? (float) (Tuning.PUMPJACK_SPM * (jack.motor.reversed() ? -1 : 1)) : 0);
		if (jack.spin.needsSync()) {
			jack.spin.markSynced();
			level.sendBlockUpdated(pos, state, state, 3);
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0 || !(level instanceof ServerLevel server)) {
			return;
		}
		BlockPos well = jack.wellhead();
		Reservoir field = jack.overWell() ? Reservoir.at(ReservoirData.seed(server), well.getX(), well.getZ()) : null;
		ReservoirData data = ReservoirData.get(server);
		jack.wanted = field != null && data.remaining(field) > 0 && jack.tank < Tuning.PUMPJACK_TANK;
		double strength = jack.motor.output();
		if (!jack.overWell()) {
			jack.status = "NOT OVER A WELL: set it with its horsehead over an oil wellhead";
		} else if (field == null || data.remaining(field) <= 0) {
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
		if (jack.tank > 0 && jack.overWell()) {   // crude leaves through the wellhead's flowline
			List<BlockPos> cell = List.of(well);
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
		if (level instanceof ServerLevel server && overWell()) {
			Reservoir reservoir = Reservoir.at(ReservoirData.seed(server), wellhead().getX(), wellhead().getZ());
			if (reservoir != null) {
				ReservoirData data = ReservoirData.get(server);
				field = Math.round(data.pressure(reservoir) * 100) + "% pressure, about " + data.remaining(reservoir) + " units left in the field; ";
			}
		}
		return Component.literal("Pumpjack (" + String.format("%.1f", Tuning.PUMPJACK_WATTS / 1000) + " kW motor, " + Voltage.INDUSTRIAL.label() + " three-phase, "
				+ Math.round(Math.abs(spin.rpm())) + " strokes a minute): " + field + "tank " + (int) tank + "/" + Tuning.PUMPJACK_TANK + " (a bucket is " + Tuning.CRUDE_BUCKET_UNITS + "), " + status + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	// ---- moving parts ---------------------------------------------------------------------------------------------------------
	@Override
	public List<Rotor> rotors() {
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		Vector3f crank = new Vector3f(0.5F, (float) BeamLinkage.CRANK_Y, (float) BeamLinkage.CRANK_Z);
		Vector3f saddle = new Vector3f(0.5F, (float) BeamLinkage.SADDLE_Y, (float) BeamLinkage.SADDLE_Z);
		Vector3f carrier = new Vector3f(0.5F, CARRIER_Y, -0.5F);
		return List.of(
				new Rotor(rotor.part(RotorBlock.Part.PUMPJACK_CRANK), crank, Direction.Axis.X, 1.0F),
				new Rotor(rotor.part(RotorBlock.Part.PUMPJACK_PITMAN_LOW), crank, Direction.Axis.X, 1.0F, (m, deg) -> pitman(m, deg, 0)),
				new Rotor(rotor.part(RotorBlock.Part.PUMPJACK_PITMAN_HIGH), crank, Direction.Axis.X, 1.0F, (m, deg) -> pitman(m, deg, (float) BeamLinkage.PITMAN)),
				new Rotor(rotor.part(RotorBlock.Part.PUMPJACK_BEAM_FRONT), saddle, Direction.Axis.X, 1.0F, (m, deg) -> beam(m, deg, -1.6F)),
				new Rotor(rotor.part(RotorBlock.Part.PUMPJACK_BEAM_MID), saddle, Direction.Axis.X, 1.0F, (m, deg) -> beam(m, deg, 0)),
				new Rotor(rotor.part(RotorBlock.Part.PUMPJACK_BEAM_BACK), saddle, Direction.Axis.X, 1.0F, (m, deg) -> beam(m, deg, 1.6F)),
				new Rotor(rotor.part(RotorBlock.Part.PUMPJACK_ROD), carrier, Direction.Axis.Y, 1.0F, (m, deg) -> rod(m, deg, 0)),
				new Rotor(rotor.part(RotorBlock.Part.PUMPJACK_ROD_LOW), carrier, Direction.Axis.Y, 1.0F, (m, deg) -> rod(m, deg, -1.5F)),
				new Rotor(rotor.part(RotorBlock.Part.PUMPJACK_BRIDLE), carrier, Direction.Axis.Y, 1.0F, (m, deg) -> rod(m, deg, 1.0F)));
	}

	/** A pitman half: to the crank pin, tilted along the pitman, and {@code up} along it. */
	private static void pitman(org.joml.Matrix4f m, float degrees, float up) {
		double crank = Math.toRadians(degrees);
		double[] pin = BeamLinkage.pin(crank);
		m.translate(0, (float) (pin[1] - BeamLinkage.CRANK_Y), (float) (pin[0] - BeamLinkage.CRANK_Z))
				.rotateX((float) BeamLinkage.pitman(crank)).translate(0, up, 0);
	}

	/** A piece of the walking beam {@code along} metres back from the saddle, the beam tilted tail up. */
	private static void beam(org.joml.Matrix4f m, float degrees, float along) {
		m.rotateX((float) -BeamLinkage.beam(Math.toRadians(degrees))).translate(0, 0, along);
	}

	/** The carrier bar, polished rod and bridle, {@code up} metres above the carrier bar, riding the horsehead. */
	private static void rod(org.joml.Matrix4f m, float degrees, float up) {
		m.translate(0, (float) BeamLinkage.rod(Math.toRadians(degrees)) + up, 0);
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(PumpjackBlock.FACING);
	}

	@Override
	public Spin spin() {
		return spin;
	}

	/** About 9 strokes a minute: slow enough to show at its real speed. */
	@Override
	public float displayDivisor() {
		return 1.0F;
	}

	// ---- BigPartEntity ---------------------------------------------------------------------------------------------------------
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
		output.putDouble("tank", tank);
		output.putBoolean("leadsSwapped", motor.leadsSwapped());
		output.putDouble("carry", carry);
		output.putBoolean("grown", grown);
		output.putFloat("rpm", spin.rpm());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		tank = input.getDoubleOr("tank", 0);
		motor.setLeadsSwapped(input.getBooleanOr("leadsSwapped", false));
		carry = input.getDoubleOr("carry", 0);
		grown = input.getBooleanOr("grown", grown);
		spin.setRpm(input.getFloatOr("rpm", 0));
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
		net.minecraft.nbt.CompoundTag tag = running.tag();
		tag.putFloat("rpm", spin.rpm());
		return tag;
	}
}
