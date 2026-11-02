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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fan deck on top of a mechanical-draft cooling tower cell (docs/multiblocks/cooling-tower.md) and its controller: it checks
 * the column below for the drift eliminators, the fill and the cold-water basin. Built, it runs its 7.5 kW 480 V three-phase fan
 * motor and cools the condensate of the steam machines whose water pipes reach the basin. The 3 m fan turns and whirs.
 */
public class CoolingTowerBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable, dev.wildcatter.kinetic.Kinetic, dev.wildcatter.kinetic.Sounding,
		dev.wildcatter.multiblock.BigPartEntity {
	/** A tower fan turns slowly: about 200 rpm for a fan of this kind. */
	private static final float FAN_RPM = 200.0F;
	/** Its three-phase induction motor; it turns only while there is work. */
	private final dev.wildcatter.circuit.Motor motor = new dev.wildcatter.circuit.Motor(true, Voltage.INDUSTRIAL, Tuning.COOLING_FAN_WATTS);
	/** Grown into its true-size cells (see BigParts). */
	private boolean grown;
	/** Set by the machine's own logic: whether it has work for the motor. */
	private boolean wanted;
	private double volts;
	private double amps;
	private String missing = "";
	private boolean cooling;
	private final dev.wildcatter.kinetic.Spin spin = new dev.wildcatter.kinetic.Spin();

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
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	/** The motor's terminals: the front face of the deck's main cell, where its disconnect is. */
	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return face == facing() && voltage == motor.voltage() && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition, java.util.List.of(facing()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, wanted);
		volts = motor.volts();
		amps = motor.amps();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CoolingTowerBlockEntity tower) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.PLUMBING_INTERVAL != 0) {
			return;
		}
		// down the column: drift eliminators, fill, then the basin, each its main cell under the deck's and turned its way
		tower.missing = TowerColumn.layout().check(level, pos, tower.facing()).missing();
		boolean was = tower.cooling;
		tower.wanted = tower.missing.isEmpty();   // the fan runs once the tower is built
		tower.cooling = tower.wanted && tower.motor.output() > 0;
		if (tower.cooling && !was) {
			level.playSound(null, pos, WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.0F, 1.1F);
		}
		tower.spin.setRpm(tower.cooling ? (float) (FAN_RPM * tower.motor.output()) : 0);
		if (tower.spin.needsSync()) {
			tower.spin.markSynced();
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	/** Client side: keeps the fan's sound going. */
	public static void clientTick(Level level, BlockPos pos, BlockState state, CoolingTowerBlockEntity tower) {
		if (level.getGameTime() % 10 == 0 && tower.spin.rpm() > 0) {
			dev.wildcatter.kinetic.Sounding.Hook.start.accept(tower);
		}
	}

	@Override
	public java.util.List<Rotor> rotors() {
		return java.util.List.of(new Rotor(((dev.wildcatter.kinetic.RotorBlock) WildcatterBlocks.ROTOR).part(dev.wildcatter.kinetic.RotorBlock.Part.TOWER_FAN),
				new org.joml.Vector3f(2.0F, 0.75F, 2.0F), Direction.Axis.Y, 1.0F));   // the middle of the 4 x 4 deck, in its stack
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(BigPartBlock.FACING);
	}

	@Override
	public dev.wildcatter.kinetic.Spin spin() {
		return spin;
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return spin.rpm() > 0 ? WildcatterSounds.FAN : null;
	}

	@Override
	public float soundPitch() {
		return 0.7F * spin.rpm() / FAN_RPM;
	}

	@Override
	public float soundVolume() {
		return spin.rpm() > 0 ? 0.5F : 0;
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putFloat("rpm", spin.rpm());
		return tag;
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		super.saveAdditional(output);
		output.putFloat("rpm", spin.rpm());
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		spin.setRpm(input.getFloatOr("rpm", 0));
		grown = input.getBooleanOr("grown", grown);
	}

	@Override
	public Component inspect() {
		String state = cooling ? "COOLING" : !missing.isEmpty() ? "NOT BUILT: needs " + missing : "fan " + motor.describe();
		return Component.literal("Cooling tower (" + String.format("%.1f", Tuning.COOLING_FAN_WATTS / 1000) + " kW fan motor, " + Voltage.INDUSTRIAL.label() + " three-phase): " + state + (cooling ? ", fan " + motor.describe() : "") + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}
}
