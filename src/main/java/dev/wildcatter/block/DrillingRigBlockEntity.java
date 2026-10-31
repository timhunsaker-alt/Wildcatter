package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Drills straight down, one block at a time, leaving casing behind. How fast depends on the voltage it gets (the
 * square of it, like a motor). It stops at the top of the field under it, or at {@link Tuning#DRY_HOLE_Y} if there
 * is nothing there: a dry hole. A strike turns the rig into a pumpjack (oil) or a gas wellhead.
 */
public class DrillingRigBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable, dev.wildcatter.kinetic.Sounding {
	private enum Phase {
		DRILLING, DRY, BLOCKED
	}

	/** Its 480 V three-phase drive motor; it turns only while drilling. */
	private final dev.wildcatter.circuit.Motor motor = new dev.wildcatter.circuit.Motor(true, Voltage.INDUSTRIAL, Tuning.RIG_WATTS);
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private double volts;
	private double amps;
	private int depth;
	private double progress;
	private Phase phase = Phase.DRILLING;

	public DrillingRigBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DRILLING_RIG_ENTITY, pos, state);
	}

	public boolean isLive() {
		return Math.abs(volts) > Tuning.SHOCK_MIN_VOLTS;
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
		return voltage == Voltage.INDUSTRIAL && role != Conductor.N && phase == Phase.DRILLING;
	}

	@Override
	public void build(Circuit circuit) {
		if (phase != Phase.DRILLING) {
			return;   // a finished rig draws nothing
		}
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (phase != Phase.DRILLING) {
			volts = 0;
			amps = 0;
			return;
		}
		motor.apply(circuit, ticks, true);
		volts = motor.volts();
		amps = motor.amps();
	}

	private double strength() {
		return motor.output();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DrillingRigBlockEntity rig) {
		rig.running.update(rig, rig.motor.output() > 0);   // the client plays its loop while it runs
		if (rig.phase != Phase.DRILLING || !(level instanceof ServerLevel server)) {
			return;
		}
		double strength = rig.strength();
		if (strength == 0) {
			return;
		}
		rig.progress += strength;
		if (level.getGameTime() % 10 == 0) {
			server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0.1, 0.0, 0.1, 0.01);
		}
		if (rig.progress >= Tuning.DRILL_TICKS_PER_BLOCK) {
			rig.progress -= Tuning.DRILL_TICKS_PER_BLOCK;
			rig.advance(server);
		}
	}

	private int goalY(ServerLevel level) {
		Reservoir field = Reservoir.at(ReservoirData.seed(level), worldPosition.getX(), worldPosition.getZ());
		return field != null ? field.topY() : Tuning.DRY_HOLE_Y;
	}

	private void advance(ServerLevel level) {
		BlockPos next = worldPosition.below(depth + 1);
		BlockState rock = level.getBlockState(next);
		if (next.getY() <= level.getMinY() || rock.getDestroySpeed(level, next) < 0) {
			finish(level, Phase.BLOCKED);
			return;
		}
		level.setBlock(next, WildcatterBlocks.CASING.defaultBlockState(), 3);
		depth++;
		setChanged();
		if (next.getY() <= goalY(level)) {
			strike(level);
		}
	}

	/** The hole has reached the depth where the field would be. */
	private void strike(ServerLevel level) {
		ReservoirData data = ReservoirData.get(level);
		Reservoir field = Reservoir.at(ReservoirData.seed(level), worldPosition.getX(), worldPosition.getZ());
		if (field == null || data.remaining(field) <= 0) {
			finish(level, Phase.DRY);
			return;
		}
		BlockState head = (field.kind() == Reservoir.Kind.OIL ? WildcatterBlocks.PUMPJACK : WildcatterBlocks.GAS_WELLHEAD).defaultBlockState();
		level.setBlock(worldPosition, head, 3);
		CircuitManager.markDirty(level);
		level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.7F, 0.8F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 30, 0.5, 0.3, 0.5, 0.05);
		Component news = Component.literal("STRIKE! " + (field.kind() == Reservoir.Kind.OIL ? "Oil" : "Gas") + " at " + depth + " blocks down. "
				+ (field.kind() == Reservoir.Kind.OIL ? "Wire the pumpjack to bring it up." : "Run a gas line to a firebox."));
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 48 * 48) {
				player.sendOverlayMessage(news);
			}
		}
	}

	private void finish(ServerLevel level, Phase result) {
		phase = result;
		setChanged();
		CircuitManager.markDirty(level);
		level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 0.5F);
	}

	@Override
	public Component inspect() {
		String state = switch (phase) {
			case DRILLING -> strength() == 0 ? motor.describe() : "drilling, " + (int) Math.round(strength() * 100) + "% speed, " + motor.describe();
			case DRY -> "DRY HOLE: nothing there. Break the rig and drill somewhere else";
			case BLOCKED -> "STOPPED: hit rock it cannot cut. Break the rig and drill somewhere else";
		};
		return Component.literal("Drilling rig (" + Math.round(Tuning.RIG_WATTS / 1000) + " kW motor, " + Voltage.INDUSTRIAL.label() + " three-phase): " + depth + " blocks down, " + state + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("depth", depth);
		output.putDouble("progress", progress);
		output.putInt("phase", phase.ordinal());
		output.putBoolean("leadsSwapped", motor.leadsSwapped());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		depth = input.getIntOr("depth", 0);
		progress = input.getDoubleOr("progress", 0);
		phase = Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, input.getIntOr("phase", 0)))];
		motor.setLeadsSwapped(input.getBooleanOr("leadsSwapped", false));
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return running.on() ? WildcatterSounds.MOTOR_HUM : null;
	}

	@Override
	public float soundPitch() {
		return 0.6F;
	}

	@Override
	public float soundVolume() {
		return running.on() ? 0.7F : 0;
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
