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
import dev.wildcatter.multiblock.BigPart;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A powered part of a refining or petrochemical unit (docs/multiblocks/crude-units.md, cracker.md): the topping unit's pump and
 * air-cooler skid, the desalter's grid transformer, the crude charge pump, the overhead fin-fan condenser, the polyethylene reactor's
 * cycle gas compressor and the pelletizer's extruder. Its unit's controller asks for it each interval with {@link #drive}; it runs
 * while asked. Its terminal box is on the west face of its main cell.
 */
public class UnitMotorBlockEntity extends PlantUnitBlockEntity implements CircuitPart, Bondable, Kinetic, dev.wildcatter.kinetic.Sounding {
	private @Nullable Motor motor;
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private final Spin spin = new Spin();
	private boolean wanted;
	private double volts;
	private long called = -1000;

	public UnitMotorBlockEntity(BlockPos pos, BlockState state) {
		this(WildcatterBlocks.UNIT_MOTOR_ENTITY, pos, state);
	}

	protected UnitMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** The part's id: what it is. */
	public String kind() {
		return getBlockState().getBlock() instanceof BigPart part ? part.shapeName() : "";
	}

	public Motor motor() {
		if (motor == null) {
			motor = switch (kind()) {
				case "topping_skid" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.TOPPING_SKID_WATTS);
				case "desalter" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.DESALTER_WATTS);
				case "crude_charge_pump" -> new Motor(true, Voltage.MEDIUM, Tuning.CHARGE_PUMP_WATTS);
				case "overhead_condenser" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.CONDENSER_FAN_WATTS);
				case "polyethylene_reactor" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.PE_REACTOR_WATTS);
				case "pelletizer" -> new Motor(true, Voltage.MEDIUM, Tuning.PELLETIZER_WATTS);
				case "air_cooled_condenser" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.ACC_FAN_WATTS);
				case "hrsg_feed_pump" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.HRSG_FEED_PUMP_WATTS);
				case "main_blower" -> new Motor(true, Voltage.MEDIUM, Tuning.ACID_BLOWER_WATTS);
				case "jaw_crusher" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.JAW_CRUSHER_WATTS);
				case "sag_mill" -> new Motor(true, Voltage.MEDIUM, Tuning.SAG_MILL_WATTS);
				case "leach_tank" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.LEACH_AGITATOR_WATTS);
				case "ccd_thickener" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.THICKENER_RAKE_WATTS);
				case "sx_circuit" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.SX_MIXER_WATTS);
				case "precipitation_tank" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.PRECIP_AGITATOR_WATTS);
				case "drum_filler" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.DRUM_FILLER_WATTS);
				case "oxygen_generator" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.O2_GENERATOR_WATTS);
				case "cold_traps" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.COLD_TRAP_WATTS);
				case "distillation_columns" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.REBOILER_WATTS);
				case "cylinder_filler" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.CYLINDER_FILLER_WATTS);
				case "hf_kiln" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.HF_KILN_WATTS);
				case "feed_station" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.FEED_STATION_WATTS);
				case "product_takeoff" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.PRODUCT_TAKEOFF_WATTS);
				case "tails_takeoff" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.TAILS_TAKEOFF_WATTS);
				case "mineral_separator" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.MINERAL_SEPARATOR_WATTS);
				case "chlorinator" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.CHLORINATOR_WATTS);
				case "hafnium_column" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.HAFNIUM_COLUMN_WATTS);
				case "kroll_reactor" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.KROLL_WATTS);
				case "vacuum_distillation" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.VACUUM_DISTILLATION_WATTS);
				case "var_furnace" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.VAR_WATTS);
				case "extrusion_press" -> new Motor(true, Voltage.MEDIUM, Tuning.EXTRUSION_PRESS_WATTS);
				case "pilger_mill" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.PILGER_WATTS);
				case "vacuum_anneal_furnace" -> new Motor(true, Voltage.INDUSTRIAL, Tuning.ANNEAL_FURNACE_WATTS);
				default -> new Motor(true, Voltage.INDUSTRIAL, 1000.0);
			};
		}
		return motor;
	}

	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
	}

	/** The unit's controller asks it to run; returns how hard it is running now, 0 to 1. */
	public double drive(Level level) {
		called = level.getGameTime();
		return motor().reversed() ? 0 : motor().output();
	}

	/** For a readout: the part's name and its motor's state. */
	public String describe() {
		return name() + ": " + motor().describe();
	}

	protected String name() {
		return switch (kind()) {
			case "topping_skid" -> "pump and air-cooler skid";
			case "desalter" -> "desalter";
			case "crude_charge_pump" -> "charge pump";
			case "overhead_condenser" -> "overhead condenser";
			case "polyethylene_reactor" -> "reactor's cycle gas compressor";
			case "pelletizer" -> "pelletizer";
			case "air_cooled_condenser" -> "condenser fans";
			case "hrsg_feed_pump" -> "feed pump";
			case "main_blower" -> "main blower";
			case "jaw_crusher" -> "jaw crusher";
			case "sag_mill" -> "SAG mill";
			case "leach_tank" -> "leach agitator";
			case "ccd_thickener" -> "thickener rake";
			case "sx_circuit" -> "SX mixers";
			case "precipitation_tank" -> "precipitation agitator";
			case "drum_filler" -> "drum filler";
			case "oxygen_generator" -> "oxygen generator's air compressor";
			case "cold_traps" -> "refrigeration";
			case "distillation_columns" -> "reboiler";
			case "cylinder_filler" -> "filler";
			case "hf_kiln" -> "kiln drive";
			case "feed_station" -> "heater";
			case "product_takeoff", "tails_takeoff" -> "refrigeration";
			case "mineral_separator" -> "separators";
			case "chlorinator" -> "fluidizing blower";
			case "hafnium_column", "kroll_reactor", "vacuum_distillation", "vacuum_anneal_furnace" -> "furnace";
			case "var_furnace" -> "arc supply";
			case "extrusion_press" -> "hydraulic pumps";
			case "pilger_mill" -> "mill drive";
			default -> kind();
		};
	}

	// ---- nozzles ----------------------------------------------------------------------------------------------------------------
	/** The feed faces: the topping skid's pump suction and the desalter's inlet, on the north face of the front cells. */
	public List<Lines.Face> feed() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (side != Direction.SOUTH) {
			return null;
		}
		return switch (kind()) {
			case "topping_skid" -> at[2] == 3 && at[1] == 0 ? Product.GASOLINE : null;
			case "overhead_condenser" -> at[2] != 7 ? null : at[1] == 0 ? Product.GASOLINE : at[1] == 1 ? Product.LPG : null;
			default -> null;
		};
	}

	@Override
	protected double hold(Product product) {
		return kind().equals("overhead_condenser") ? 200 : 50;
	}

	/** Adds a product at its nozzle; returns what did not fit. */
	public double give(Product product, double amount) {
		double fits = Math.min(amount, room(product));
		add(product, fits);
		return amount - fits;
	}

	public double roomFor(Product product) {
		return room(product);
	}

	// ---- the motor ---------------------------------------------------------------------------------------------------------------
	private List<Direction> boxFaces() {
		return List.of(LeasePartBlock.world(getBlockState(), Direction.WEST));
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && boxFaces().contains(face) && voltage == motor().voltage() && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		motor().build(circuit, worldPosition, boxFaces());
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor().apply(circuit, ticks, wanted);
		volts = motor().volts();
	}

	/** The motor's speed at full load, read from the standard's frequency. */
	private double rpm() {
		int poles = switch (kind()) {
			case "crude_charge_pump", "polyethylene_reactor", "hrsg_feed_pump", "main_blower" -> 2;
			case "desalter" -> 0;
			default -> 4;
		};
		return poles == 0 ? 0 : 120.0 * ElectricalStandard.active().hertz() / poles * 0.985;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, UnitMotorBlockEntity part) {
		Motor motor = part.motor();
		part.running.update(part, motor.output() > 0);
		boolean turning = motor.state() == Motor.State.RUNNING && !motor.stalled();
		part.spin.setRpm(turning ? (float) (part.rpm() * (motor.reversed() ? -1 : 1)) : 0);
		if (part.spin.needsSync()) {
			part.spin.markSynced();
			level.sendBlockUpdated(pos, state, state, 3);
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL == 0) {
			part.wanted = level.getGameTime() - part.called < 40;
			part.work(level);
			part.setChanged();
		}
	}

	/** A subclass's own work each interval (the reactor, the pelletizer). */
	protected void work(Level level) {
	}

	@Override
	public Component inspect() {
		String label = switch (kind()) {
			case "topping_skid" -> "Topping unit skid (charge pump 10 hp and air cooler 5 hp, " + Voltage.INDUSTRIAL.label() + ")";
			case "desalter" -> "Electrostatic desalter (its grid transformer, " + Math.round(Tuning.DESALTER_WATTS / 1000) + " kW, " + Voltage.INDUSTRIAL.label() + ")";
			case "crude_charge_pump" -> "Crude charge pump (250 hp, " + Voltage.MEDIUM.label() + ")";
			case "overhead_condenser" -> "Overhead fin-fan condenser (four 25 hp fans, " + Voltage.INDUSTRIAL.label() + ")";
			case "air_cooled_condenser" -> "Air-cooled condenser (four 50 hp fans, " + Voltage.INDUSTRIAL.label() + ")";
			case "hrsg_feed_pump" -> "HRSG feed pump (60 hp, " + Voltage.INDUSTRIAL.label() + ")";
			case "main_blower" -> "Main blower (" + Math.round(Tuning.ACID_BLOWER_WATTS / 1000) + " kW, " + Voltage.MEDIUM.label() + ")";
			default -> kind();
		};
		return Component.literal(label + ": " + (wanted ? motor().describe() : "idle: its unit is not running it") + heldReadout() + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	/** What it holds at its nozzles, for a readout. */
	protected String heldReadout() {
		StringBuilder out = new StringBuilder();
		for (var e : held.entrySet()) {
			if (e.getValue() >= 0.5) {
				out.append(", ").append(String.format("%s %.0f %s", e.getKey().label(), e.getValue(), e.getKey().unit()));
			}
		}
		return out.toString();
	}

	// ---- moving parts -------------------------------------------------------------------------------------------------------------
	@Override
	public List<Rotor> rotors() {
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		return switch (kind()) {
			case "topping_skid" -> List.of(new Rotor(rotor.part(RotorBlock.Part.INJECTION_PUMP_COUPLING), new Vector3f(0.9F, 0.55F, 1.1F), Direction.Axis.Z, 1.0F),
					new Rotor(rotor.part(RotorBlock.Part.SKID_FAN), new Vector3f(1.5F, 2.75F, 2.6F), Direction.Axis.Y, 0.3F));
			case "crude_charge_pump" -> List.of(new Rotor(rotor.part(RotorBlock.Part.INJECTION_PUMP_COUPLING), new Vector3f(1.0F, 0.7F, 2.1F), Direction.Axis.Z, 1.0F));
			case "overhead_condenser" -> List.of(
					new Rotor(rotor.part(RotorBlock.Part.FIN_FAN), new Vector3f(1.0F, 3.6F, 2.0F), Direction.Axis.Y, 0.14F),
					new Rotor(rotor.part(RotorBlock.Part.FIN_FAN), new Vector3f(3.0F, 3.6F, 2.0F), Direction.Axis.Y, 0.14F),
					new Rotor(rotor.part(RotorBlock.Part.FIN_FAN), new Vector3f(1.0F, 3.6F, 6.0F), Direction.Axis.Y, 0.14F),
					new Rotor(rotor.part(RotorBlock.Part.FIN_FAN), new Vector3f(3.0F, 3.6F, 6.0F), Direction.Axis.Y, 0.14F));
			case "polyethylene_reactor" -> List.of(new Rotor(rotor.part(RotorBlock.Part.INJECTION_PUMP_COUPLING), new Vector3f(2.55F, 0.55F, 1.5F), Direction.Axis.Z, 1.0F));
			case "pelletizer" -> List.of(new Rotor(rotor.part(RotorBlock.Part.GAS_COMPRESSOR_COUPLING), new Vector3f(1.5F, 0.9F, 1.6F), Direction.Axis.Z, 1.0F));
			case "air_cooled_condenser" -> List.of(
					new Rotor(rotor.part(RotorBlock.Part.FIN_FAN), new Vector3f(1.5F, 5.4F, 1.5F), Direction.Axis.Y, 0.14F),
					new Rotor(rotor.part(RotorBlock.Part.FIN_FAN), new Vector3f(4.5F, 5.4F, 1.5F), Direction.Axis.Y, 0.14F),
					new Rotor(rotor.part(RotorBlock.Part.FIN_FAN), new Vector3f(1.5F, 5.4F, 4.5F), Direction.Axis.Y, 0.14F),
					new Rotor(rotor.part(RotorBlock.Part.FIN_FAN), new Vector3f(4.5F, 5.4F, 4.5F), Direction.Axis.Y, 0.14F));
			case "hrsg_feed_pump" -> List.of(new Rotor(rotor.part(RotorBlock.Part.INJECTION_PUMP_COUPLING), new Vector3f(1.0F, 0.7F, 1.7F), Direction.Axis.Z, 1.0F));
			default -> List.of();
		};
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(BigPartBlock.FACING);
	}

	@Override
	public Spin spin() {
		return spin;
	}

	@Override
	public float displayDivisor() {
		return kind().equals("overhead_condenser") || kind().equals("air_cooled_condenser") ? 2 : 20;   // the fin fans turn slowly enough to show at their own speed
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? (kind().endsWith("condenser") ? WildcatterSounds.FAN : WildcatterSounds.MOTOR_HUM) : null;
	}

	@Override
	public float soundPitch() {
		return kind().equals("desalter") ? 0.5F : 0.9F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.7F : 0;
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
