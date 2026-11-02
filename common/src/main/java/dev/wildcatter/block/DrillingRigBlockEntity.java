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
 * Drills straight down from under its main cell, one block at a time, leaving casing behind. How fast depends on the voltage it
 * gets (the square of it, like a motor). It stops at the top of the field under it, or at {@link Tuning#DRY_HOLE_Y} if there
 * is nothing there: a dry hole. On a strike the rig rigs down: it drops as its item and leaves an {@link OilWellheadBlock} or a
 * gas wellhead (Christmas tree) on the hole (docs/multiblocks/well-site.md). While drilling its top head turns at
 * {@link Tuning#RIG_ROTARY_RPM} and slides down the mast through each 6 m joint.
 */
public class DrillingRigBlockEntity extends BlockEntity implements CircuitPart, Inspectable, Bondable, dev.wildcatter.kinetic.Sounding,
		dev.wildcatter.multiblock.BigPartEntity, dev.wildcatter.kinetic.Kinetic {
	/** The top head's carriage at the top of the mast, and how far it travels down through one joint. */
	private static final float HEAD_TOP = 8.3F;

	private enum Phase {
		DRILLING, DRY, BLOCKED, TOO_DEEP
	}

	/** Its 480 V three-phase drive motor; it turns only while drilling. */
	private final dev.wildcatter.circuit.Motor motor = new dev.wildcatter.circuit.Motor(true, Voltage.INDUSTRIAL, Tuning.RIG_WATTS);
	private final dev.wildcatter.kinetic.RunningSound running = new dev.wildcatter.kinetic.RunningSound();
	private double volts;
	private double amps;
	private int depth;
	private String log = "";
	private double progress;
	private Phase phase = Phase.DRILLING;
	private boolean grown;
	private final dev.wildcatter.kinetic.Spin spin = new dev.wildcatter.kinetic.Spin();
	/** Client side: the head's drawn height, eased toward where the depth puts it. */
	private float shownHead = Float.NaN;
	private long shownAt;

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

	// ---- the power unit's terminal box: the back face of the bottom cell at the back (the skid is on either side) ---------------
	private java.util.List<Direction> boxFaces() {
		return java.util.List.of(facing().getOpposite());
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(DrillingRigBlock.terminalBox(worldPosition, getBlockState())) && boxFaces().contains(face)
				&& voltage == Voltage.INDUSTRIAL && role != Conductor.N && phase == Phase.DRILLING;
	}

	@Override
	public void build(Circuit circuit) {
		if (phase != Phase.DRILLING) {
			return;   // a finished rig draws nothing
		}
		motor.build(circuit, DrillingRigBlock.terminalBox(worldPosition, getBlockState()), boxFaces());
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
		rig.spin.setRpm(rig.phase == Phase.DRILLING && rig.motor.output() > 0 ? (float) Tuning.RIG_ROTARY_RPM : 0);
		if (rig.spin.needsSync()) {
			rig.spin.markSynced();
			level.sendBlockUpdated(pos, state, state, 3);
		}
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

	/** Game test hook: as if it had already drilled (and cased) this far. */
	public void testDepth(int blocks) {
		depth = blocks;
		setChanged();
	}

	private void advance(ServerLevel level) {
		if (Drilling.tooDeepForSmallRig(depth)) {
			finish(level, Phase.TOO_DEEP);
			return;
		}
		if (!Drilling.caseNext(level, worldPosition, depth)) {
			finish(level, Phase.BLOCKED);
			return;
		}
		depth++;
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);   // the head moves down the mast
		if (worldPosition.getY() - depth <= Drilling.goalY(level, worldPosition, Tuning.DRY_HOLE_Y)) {
			strike(level);
		}
	}

	/** The hole has reached the depth where the field would be, or the base of a roll-front. */
	private void strike(ServerLevel level) {
		log = Drilling.gammaLog(level, worldPosition, worldPosition.getY() - depth);
		if (Drilling.isrFirst(level, worldPosition)) {
			BlockPos hole = worldPosition;
			String news = log;
			rigDown(level, null);
			Drilling.setIsrWell(level, hole, news);
			CircuitManager.markDirty(level);
			Drilling.announce(level, hole, Component.literal("ISR WELL completed through a roll-front, " + depth + " blocks down. " + news
					+ ". The rig has rigged down and dropped; a submersible pump makes the well a producer."));
			return;
		}
		ReservoirData data = ReservoirData.get(level);
		Reservoir field = Drilling.field(level, worldPosition);
		if (field == null || data.remaining(field) <= 0) {
			finish(level, Phase.DRY);
			return;
		}
		BlockPos hole = worldPosition;
		rigDown(level, field.kind());
		CircuitManager.markDirty(level);
		Drilling.announce(level, hole, Component.literal("STRIKE! " + (field.kind() == Reservoir.Kind.OIL ? "Oil" : "Gas") + " at " + depth + " blocks down. "
				+ "The rig has rigged down and dropped; "
				+ (field.kind() == Reservoir.Kind.OIL ? "click the oil wellhead with a pumpjack and wire it to bring the oil up." : "run steel pipe from the Christmas tree to a firebox.")));
	}

	/** The rig leaves the hole: every cell goes, it drops as its item, and the wellhead for the field (if any) stands on the hole. */
	private void rigDown(ServerLevel level, Reservoir.@org.jspecify.annotations.Nullable Kind kind) {
		BlockPos hole = worldPosition;
		BlockState state = getBlockState();
		java.util.List<BlockPos> cells = dev.wildcatter.multiblock.BigParts.cells(hole, state);
		for (int i = cells.size() - 1; i >= 1; i--) {
			level.setBlock(cells.get(i), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
					net.minecraft.world.level.block.Block.UPDATE_ALL | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS);
		}
		level.removeBlockEntity(hole);
		Direction facing = state.getValue(DrillingRigBlock.FACING);
		if (kind != null) {
			Drilling.setWellhead(level, hole, kind, facing);
		}
		net.minecraft.world.level.block.Block.popResource(level, hole.relative(facing.getOpposite()).above(), new net.minecraft.world.item.ItemStack(WildcatterBlocks.DRILLING_RIG));
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
			case DRY -> "DRY HOLE: nothing there. Break the rig and drill somewhere else" + (log.isEmpty() ? "" : "; " + log);
			case TOO_DEEP -> "TOO DEEP: this rig reaches " + Tuning.SMALL_RIG_MAX_DEPTH + " m. A land rig goes deeper";
			case BLOCKED -> "STOPPED: hit rock it cannot cut. Break the rig and drill somewhere else";
		};
		return Component.literal("Drilling rig (" + Math.round(Tuning.RIG_WATTS / 1000) + " kW motor, " + Voltage.INDUSTRIAL.label() + " three-phase): " + depth + " blocks down, " + state + "; "
				+ (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("depth", depth);
		output.putString("log", log);
		output.putDouble("progress", progress);
		output.putInt("phase", phase.ordinal());
		output.putBoolean("leadsSwapped", motor.leadsSwapped());
		output.putBoolean("grown", grown);
		output.putFloat("rpm", spin.rpm());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		running.load(input);
		depth = input.getIntOr("depth", 0);
		log = input.getStringOr("log", "");
		progress = input.getDoubleOr("progress", 0);
		phase = Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, input.getIntOr("phase", 0)))];
		motor.setLeadsSwapped(input.getBooleanOr("leadsSwapped", false));
		grown = input.getBooleanOr("grown", grown);
		spin.setRpm(input.getFloatOr("rpm", 0));
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

	// ---- moving parts: the carriage slides, the spindle and drill pipe turn and slide with it ------------------------------------
	@Override
	public java.util.List<Rotor> rotors() {
		dev.wildcatter.kinetic.RotorBlock rotor = (dev.wildcatter.kinetic.RotorBlock) WildcatterBlocks.ROTOR;
		float head = head();
		org.joml.Vector3f hole = new org.joml.Vector3f(0.5F, 0, 0.5F);
		java.util.List<Rotor> out = new java.util.ArrayList<>();
		out.add(new Rotor(rotor.part(dev.wildcatter.kinetic.RotorBlock.Part.RIG_CARRIAGE), hole, Direction.Axis.Y, 1.0F, (m, deg) -> m.translate(0, head, 0)));
		out.add(new Rotor(rotor.part(dev.wildcatter.kinetic.RotorBlock.Part.RIG_SPINDLE), hole, Direction.Axis.Y, 1.0F,
				(m, deg) -> m.translate(0, head, 0).rotateY((float) Math.toRadians(deg))));
		for (int k = 0; k < 4; k++) {   // 2 m pieces of pipe down from the spindle, into the hole
			float down = -2.5F - 2.0F * k;
			out.add(new Rotor(rotor.part(dev.wildcatter.kinetic.RotorBlock.Part.RIG_PIPE), hole, Direction.Axis.Y, 1.0F,
					(m, deg) -> m.translate(0, head + down, 0).rotateY((float) Math.toRadians(deg))));
		}
		return out;
	}

	/** Client side: the carriage's height, easing toward where the depth drilled puts it in the current joint. */
	private float head() {
		float target = HEAD_TOP - (phase == Phase.DRILLING ? depth % Tuning.RIG_JOINT_BLOCKS : 0);
		long now = System.nanoTime();
		if (Float.isNaN(shownHead)) {
			shownHead = target;
		} else {
			float step = Math.min(0.25F, (now - shownAt) / 1e9F) * (target > shownHead ? 3.0F : 1.2F);   // up for the next joint faster
			shownHead = Math.abs(target - shownHead) <= step ? target : shownHead + Math.signum(target - shownHead) * step;
		}
		shownAt = now;
		return shownHead;
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(DrillingRigBlock.FACING);
	}

	@Override
	public dev.wildcatter.kinetic.Spin spin() {
		return spin;
	}

	/** 80 rpm: slow enough to show at its real speed. */
	@Override
	public float displayDivisor() {
		return 1.0F;
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
		net.minecraft.nbt.CompoundTag tag = running.tag();
		tag.putFloat("rpm", spin.rpm());
		tag.putInt("depth", depth);
		tag.putInt("phase", phase.ordinal());
		return tag;
	}
}
