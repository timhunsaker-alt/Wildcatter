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
import dev.wildcatter.kinetic.Kinetic;
import dev.wildcatter.kinetic.RotorBlock;
import dev.wildcatter.kinetic.Sounding;
import dev.wildcatter.kinetic.Spin;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A steam plant part. It remembers its turbine generator (sent to the client for the turning parts and sounds). The burner holds
 * its fuel (one stack, fed by hand or hopper, plus the heat already taken from it); the feed pump is a 480 V three-phase motor.
 */
public class SteamPlantPartBlockEntity extends BlockEntity implements CircuitPart, Kinetic, Sounding, Inspectable, Container, dev.wildcatter.multiblock.BigPartEntity {
	private static final Spin STILL = new Spin();

	private @Nullable BlockPos controller;
	/** Burner: the fuel waiting to burn, and the heat already taken from it, in joules. */
	private ItemStack fuel = ItemStack.EMPTY;
	private double joules;
	/** Feed pump: its motor, whether the plant wants it, and its shaft. */
	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, Tuning.FEED_PUMP_WATTS);
	private boolean wanted;
	private final Spin pumpSpin = new Spin();
	/** Grown into its true-size cells (see BigParts). */
	private boolean grown;

	public SteamPlantPartBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STEAM_PLANT_PART_ENTITY, pos, state);
	}

	public SteamPlantPartBlock.Part part() {
		return ((SteamPlantPartBlock) getBlockState().getBlock()).part();
	}

	public @Nullable TurbineGeneratorBlockEntity controller() {
		return controller != null && level != null && level.getBlockEntity(controller) instanceof TurbineGeneratorBlockEntity g ? g : null;
	}

	void bind(BlockPos generator) {
		if (!generator.equals(controller)) {
			controller = generator.immutable();
			changed();
		}
	}

	void unbind() {
		if (controller != null) {
			controller = null;
			changed();
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	// ---- the burner ------------------------------------------------------------------------------------------------
	/** The heat in an item of solid fuel: a coal block is a cubic metre of coal (docs/multiblocks/steam-plant.md). */
	public static double joulesOf(ItemStack stack) {
		if (stack.is(Items.COAL)) {
			return Tuning.COAL_JOULES;
		}
		if (stack.is(Items.CHARCOAL)) {
			return Tuning.CHARCOAL_JOULES;
		}
		if (stack.is(Items.COAL_BLOCK)) {
			return Tuning.COAL_JOULES * 9;
		}
		return 0;
	}

	/** Puts fuel in the burner's stack; returns what did not fit. */
	public ItemStack insertFuel(ItemStack stack) {
		if (part() != SteamPlantPartBlock.Part.BURNER || joulesOf(stack) <= 0) {
			return stack;
		}
		if (fuel.isEmpty()) {
			fuel = stack.split(Math.min(stack.getCount(), stack.getMaxStackSize()));
		} else if (ItemStack.isSameItemSameComponents(fuel, stack)) {
			int room = fuel.getMaxStackSize() - fuel.getCount();
			fuel.grow(stack.split(Math.min(room, stack.getCount())).getCount());
		}
		setChanged();
		return stack;
	}

	/** Takes up to {@code wanted} joules of heat, burning items from the stack as needed; returns what it gave. */
	double burn(double wanted) {
		while (joules < wanted && !fuel.isEmpty()) {
			joules += joulesOf(fuel);
			fuel.shrink(1);
			setChanged();
		}
		double got = Math.min(joules, wanted);
		joules -= got;
		return got;
	}

	/** Adds heat from piped gas. */
	void addHeat(double j) {
		joules += j;
	}

	public double heatJoules() {
		return joules + fuel.getCount() * joulesOf(fuel);
	}

	/** Test hook: fills the burner with heat, in joules. */
	public void setHeatForTest(double j) {
		joules = j;
		setChanged();
	}

	// ---- the feed pump ----------------------------------------------------------------------------------------------
	void setWanted(boolean value) {
		wanted = value;
	}

	/** 0 to 1: how hard the feed pump is pumping. */
	double pumpOutput() {
		return part() == SteamPlantPartBlock.Part.FEED_PUMP ? motor.output() : 0;
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

	/** The feed pump's motor terminal box is on the side of the motor away from the generator (south, for a plant facing north). */
	private Direction motorEnd() {
		return facing().getOpposite();
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return part() == SteamPlantPartBlock.Part.FEED_PUMP && face == motorEnd() && voltage == Voltage.INDUSTRIAL && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		if (part() == SteamPlantPartBlock.Part.FEED_PUMP) {
			motor.build(circuit, worldPosition, List.of(motorEnd()));
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (part() != SteamPlantPartBlock.Part.FEED_PUMP) {
			return;
		}
		motor.apply(circuit, ticks, wanted);
		pumpSpin.setRpm((float) (motor.output() * 120.0 * ElectricalStandard.active().hertz() / 4 * 0.97));   // a 4-pole motor at 3 % slip
		if (pumpSpin.needsSync() && level != null) {
			pumpSpin.markSynced();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public String pumpState() {
		return motor.describe();
	}

	// ---- turning parts and sound ------------------------------------------------------------------------------------
	@Override
	public List<Rotor> rotors() {
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		return switch (part()) {
			// in the main cell's frame for a plant facing north (tools/models/rotors.py): the generator is west of the turbine, the
			// shaft 0.85 m up along the middle of the 2 m skid; the pump's motor is the west half of its 2 m skid
			case TURBINE -> List.of(new Rotor(rotor.part(RotorBlock.Part.TURBINE_COUPLING), new Vector3f(0.1F, 0.85F, 1.0F), Direction.Axis.X, 1.0F));
			case FEED_PUMP -> List.of(new Rotor(rotor.part(RotorBlock.Part.PUMP_COUPLING), new Vector3f(1.0F, 0.32F, 0.5F), Direction.Axis.X, 1.0F));
			default -> List.of();
		};
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(SteamPlantPartBlock.FACING);
	}

	@Override
	public Spin spin() {
		if (part() == SteamPlantPartBlock.Part.FEED_PUMP) {
			return pumpSpin;
		}
		TurbineGeneratorBlockEntity g = controller();
		return g != null ? g.spin() : STILL;
	}

	@Override
	public @Nullable SoundEvent loop() {
		TurbineGeneratorBlockEntity g = controller();
		return switch (part()) {
			case TURBINE -> g != null && g.spin().rpm() > 0 ? WildcatterSounds.TURBINE_WHINE : null;
			case BURNER -> g != null && g.firing() > 0.01 ? WildcatterSounds.BURNER_ROAR : null;
			case FEED_PUMP -> pumpSpin.rpm() > 0 ? WildcatterSounds.MOTOR_HUM : null;
			default -> null;
		};
	}

	@Override
	public float soundPitch() {
		TurbineGeneratorBlockEntity g = controller();
		return switch (part()) {
			case TURBINE -> g == null ? 1 : (float) (g.spin().rpm() / g.ratedRpm());
			case BURNER -> g == null ? 1 : (float) (0.8 + 0.4 * g.firing());
			default -> 1;
		};
	}

	@Override
	public float soundVolume() {
		TurbineGeneratorBlockEntity g = controller();
		return switch (part()) {
			case TURBINE -> g == null ? 0 : (float) (0.4 + 0.6 * g.loadShare());
			case BURNER -> g == null ? 0 : (float) (0.3 + 0.7 * g.firing());
			case FEED_PUMP -> 0.4F;
			default -> 0;
		};
	}

	/** Client side: keeps the running sounds going; flame flickers in a lit burner. */
	public static void clientTick(Level level, BlockPos pos, BlockState state, SteamPlantPartBlockEntity part) {
		if (level.getGameTime() % 10 == 0 && part.loop() != null) {
			Sounding.Hook.start.accept(part);
		}
		TurbineGeneratorBlockEntity g = part.controller();
		if (part.part() == SteamPlantPartBlock.Part.BURNER && g != null && g.firing() > 0.01 && level.getRandom().nextFloat() < 0.5F) {
			level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.2 + 0.6 * level.getRandom().nextDouble(), pos.getY() + 0.3, pos.getZ() + 0.2 + 0.6 * level.getRandom().nextDouble(), 0, 0.02, 0);
		}
	}

	@Override
	public Component inspect() {
		TurbineGeneratorBlockEntity g = controller();
		String name = switch (part()) {
			case BURNER -> String.format("Burner: %s, %.1f GJ of heat in hand", fuel.isEmpty() ? "no solid fuel" : fuel.getCount() + " x " + fuel.getHoverName().getString(), heatJoules() / 1e9);
			case DRUM -> "Steam drum";
			case SUPERHEATER -> "Superheater";
			case TURBINE -> "Turbine";
			case CONDENSER -> "Condenser";
			case FEED_PUMP -> "Feed pump (" + Math.round(Tuning.FEED_PUMP_WATTS / 1000 * 10) / 10.0 + " kW, " + Voltage.INDUSTRIAL.label() + " three-phase): " + motor.describe();
		};
		return Component.literal(name + (g == null ? " (not part of a plant: build it round a turbine generator)" : "; the plant reads: ")).append(g == null ? Component.empty() : g.inspect());
	}

	// ---- Container: hoppers feed the burner ---------------------------------------------------------------------------
	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return fuel.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return fuel;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack out = fuel.split(count);
		setChanged();
		return out;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack out = fuel;
		fuel = ItemStack.EMPTY;
		return out;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		fuel = stack;
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return part() == SteamPlantPartBlock.Part.BURNER && joulesOf(stack) > 0;
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		fuel = ItemStack.EMPTY;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		if (controller != null) {
			tag.putLong("controller", controller.asLong());
		}
		tag.putFloat("rpm", pumpSpin.rpm());
		return tag;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (controller != null) {
			output.putLong("controller", controller.asLong());
		}
		output.putDouble("joules", joules);
		output.putBoolean("grown", grown);
		if (!fuel.isEmpty()) {
			output.store("fuel", ItemStack.CODEC, fuel);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		controller = input.getLong("controller").map(BlockPos::of).orElse(null);
		joules = input.getDoubleOr("joules", 0);
		grown = input.getBooleanOr("grown", false);
		fuel = input.read("fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		pumpSpin.setRpm(input.getFloatOr("rpm", pumpSpin.rpm()));
	}
}
