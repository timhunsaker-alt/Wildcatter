package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.kinetic.Kinetic;
import dev.wildcatter.kinetic.RotorBlock;
import dev.wildcatter.kinetic.Spin;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.well.Reservoir;
import dev.wildcatter.well.ReservoirData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The land rig's controller, in its substructure (docs/multiblocks/land-rig.md). It checks the rig every 20 ticks, keeps the top
 * drive, the mud water, the depth and the stand, tells the drive house how much power it wants, and drills as fast as the power
 * it gets allows: {@link Tuning#LAND_RIG_TICKS_PER_BLOCK} a block at full power, a 15 s connection after each 27 m stand. On a
 * strike it sets the wellhead in the hole cell under its floor and stops.
 */
public class RigSubstructureBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, Kinetic {
	/** The top drive's quill at the top of a stand, above the hole (the floor's top is 6 m up). */
	private static final float TOP_DRIVE_TOP = 6.0F + Tuning.LAND_RIG_STAND_BLOCKS + 3.0F;

	public enum Phase {
		IDLE, DRILLING, COMPLETE, DRY
	}

	private Phase phase = Phase.IDLE;
	private boolean formed;
	private String missing = "";
	private boolean topDrive;
	private int water;
	private int depth;
	/** Blocks drilled in the current stand, and progress toward the next block in ticks of full power. */
	private int stand;
	private double progress;
	/** Ticks of full power left in the connection under way (0: drilling). */
	private double connection;
	private boolean grown;
	private String status = "";
	private final Spin spin = new Spin();
	/** Client side: the top drive's drawn height, eased toward where the stand puts it. */
	private float shownHead = Float.NaN;
	private long shownAt;

	public RigSubstructureBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RIG_SUBSTRUCTURE_ENTITY, pos, state);
	}

	public Direction facing() {
		return getBlockState().getValue(RigPartBlock.FACING);
	}

	public BlockPos hole() {
		return LandRig.hole(worldPosition, facing());
	}

	public boolean formed() {
		return formed;
	}

	public boolean drilling() {
		return phase == Phase.DRILLING && formed && topDrive;
	}

	/** In a connection: the drawworks hoisting, the pumps stopped. */
	public boolean connecting() {
		return drilling() && connection > 0;
	}

	/** The mud pumps run while it drills between connections, with water in the tanks. */
	public boolean pumping() {
		return drilling() && connection <= 0 && water >= Tuning.LAND_RIG_MUD_PER_BLOCK;
	}

	private @Nullable RigDriveHouseBlockEntity driveHouse() {
		return level != null && level.getBlockEntity(LandRig.driveHouse(worldPosition, facing())) instanceof RigDriveHouseBlockEntity house ? house : null;
	}

	/** The power the drive house should draw for what the rig is doing now. */
	public double demand() {
		if (!formed) {
			return 0;
		}
		if (connecting()) {
			return Tuning.LAND_RIG_CONNECTION_WATTS;
		}
		return pumping() ? Tuning.LAND_RIG_DRILL_WATTS : Tuning.LAND_RIG_IDLE_WATTS;
	}

	public boolean installTopDrive() {
		if (topDrive || !formed) {
			return false;
		}
		topDrive = true;
		setChanged();
		sync();
		return true;
	}

	/** The driller's console: start or stop drilling. */
	public Component toggle() {
		if (phase == Phase.IDLE) {
			phase = Phase.DRILLING;
		} else if (phase == Phase.DRILLING) {
			phase = Phase.IDLE;
		}
		setChanged();
		return inspect();
	}

	/** Water from a pump's pipe into the mud tanks; returns how much fitted. */
	public int addWater(int amount) {
		int took = Math.max(0, Math.min(amount, Tuning.LAND_RIG_MUD_TANK - water));
		water += took;
		if (took > 0) {
			setChanged();
		}
		return took;
	}

	public double waterLevel() {
		return water / (double) Tuning.LAND_RIG_MUD_TANK;
	}

	/** Game test hook: empties or fills the mud tanks. */
	public void testWater(int litres) {
		water = litres;
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RigSubstructureBlockEntity rig) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % 20 == 0) {
			PartLayout.Found found = LandRig.layout().check(level, pos, rig.facing());
			boolean was = rig.formed;
			rig.formed = found.parts() != null;
			rig.missing = found.missing();
			if (was != rig.formed) {
				rig.setChanged();
				dev.wildcatter.circuit.CircuitManager.markDirty(level);
			}
		}
		RigDriveHouseBlockEntity house = rig.driveHouse();
		double power = house != null && rig.formed ? house.delivered() : 0;
		float rpm = 0;
		if (!rig.drilling()) {
			rig.status = !rig.formed ? "NOT BUILT: needs the " + rig.missing : !rig.topDrive ? "NO TOP DRIVE: click one into the mast"
					: rig.phase == Phase.COMPLETE ? "WELL COMPLETE: break the rig down to move it" : rig.phase == Phase.DRY ? "DRY HOLE: nothing under it down to bedrock"
					: "stopped: right-click the driller's console to drill";
		} else if (power <= 0.01) {
			rig.status = house != null && house.undervoltage() ? "drives stopped on UNDERVOLTAGE" : "NO POWER at the drive house";
		} else if (rig.connection > 0) {
			rig.connection -= power;
			rig.status = "making a connection: the drawworks hoisting the next stand";
			if (rig.connection <= 0) {
				rig.connection = 0;
				rig.sync();
			}
		} else if (rig.water < Tuning.LAND_RIG_MUD_PER_BLOCK) {
			rig.status = "NO MUD: pipe water from a pump to the mud tanks";
		} else {
			rpm = (float) Tuning.TOP_DRIVE_RPM;
			rig.progress += power;
			rig.status = "drilling at " + Math.round(power * 100) + "% of full rate";
			while (rig.progress >= Tuning.LAND_RIG_TICKS_PER_BLOCK && rig.drilling() && rig.connection <= 0) {
				rig.progress -= Tuning.LAND_RIG_TICKS_PER_BLOCK;
				rig.advance(server);
			}
		}
		rig.spin.setRpm(rpm);
		if (rig.spin.needsSync()) {
			rig.spin.markSynced();
			rig.sync();
		}
	}

	private void advance(ServerLevel level) {
		BlockPos hole = hole();
		Reservoir field = Drilling.field(level, hole);
		if (!Drilling.caseNext(level, hole, depth)) {
			phase = field == null ? Phase.DRY : Phase.COMPLETE;   // bedrock: with a field above it, the hole has already struck
			if (field == null) {
				level.playSound(null, hole, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 0.5F);
			}
			setChanged();
			sync();
			return;
		}
		depth++;
		water -= Tuning.LAND_RIG_MUD_PER_BLOCK;
		stand++;
		dev.wildcatter.well.RollFront front = Drilling.isrFirst(level, hole) ? Drilling.rollFront(level, hole) : null;
		if (front != null && hole.getY() - depth <= front.bottomY()) {
			phase = Phase.COMPLETE;   // through a roll-front: an ISR well, its gamma log in the wellhead
			String log = Drilling.gammaLog(level, hole, hole.getY() - depth);
			if (level.getBlockState(hole).canBeReplaced()) {
				Drilling.setIsrWell(level, hole, log);
			}
			Drilling.announce(level, hole, Component.literal("ISR WELL completed through a roll-front, " + depth + " m down. " + log));
		} else if (field != null && hole.getY() - depth <= field.topY()) {
			strike(level, field);
		} else if (stand >= Tuning.LAND_RIG_STAND_BLOCKS) {
			stand = 0;
			connection = Tuning.LAND_RIG_CONNECTION_TICKS;
		}
		setChanged();
		sync();
	}

	private void strike(ServerLevel level, Reservoir field) {
		phase = Phase.COMPLETE;
		ReservoirData data = ReservoirData.get(level);
		BlockPos hole = hole();
		if (data.remaining(field) <= 0) {
			phase = Phase.DRY;
			return;
		}
		if (level.getBlockState(hole).canBeReplaced()) {
			Drilling.setWellhead(level, hole, field.kind(), facing());
		}
		Drilling.announce(level, hole, Component.literal("STRIKE! " + (field.deep() ? "Deep " : "") + (field.kind() == Reservoir.Kind.OIL ? "oil" : "gas") + " at "
				+ depth + " m. The wellhead is set in the cellar under the rig floor: break the rig down to move it."));
	}

	private void sync() {
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	public Component inspect() {
		RigDriveHouseBlockEntity house = driveHouse();
		return Component.literal("Land rig (1,000 hp, " + Voltage.MEDIUM.label() + " three-phase): " + status + "; " + depth + " m down, stand "
				+ stand + "/" + Tuning.LAND_RIG_STAND_BLOCKS + ", mud water " + water / 1000 + " of " + Tuning.LAND_RIG_MUD_TANK / 1000 + " m3, top drive "
				+ (topDrive ? "in" : "MISSING") + (house != null && formed ? "; drive house " + house.load().describe() : ""));
	}

	// ---- moving parts: the top drive rides down the stand, its quill and the pipe below turning --------------------------------
	@Override
	public List<Rotor> rotors() {
		List<Rotor> out = new ArrayList<>();
		if (!topDrive) {
			return out;
		}
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		float head = head();
		// the hole is 4 cells east and south of the main cell, facing north
		Vector3f hole = new Vector3f(4.5F, 0, 4.5F);
		out.add(new Rotor(rotor.part(RotorBlock.Part.TOP_DRIVE), hole, Direction.Axis.Y, 1.0F, (m, deg) -> m.translate(0, head, 0)));
		out.add(new Rotor(rotor.part(RotorBlock.Part.TOP_DRIVE_QUILL), hole, Direction.Axis.Y, 1.0F,
				(m, deg) -> m.translate(0, head, 0).rotateY((float) Math.toRadians(deg))));
		for (int k = 0; k < 16; k++) {   // 2 m pieces of the stand down through the rotary table into the hole
			float down = -2.5F - 2.0F * k;
			out.add(new Rotor(rotor.part(RotorBlock.Part.RIG_PIPE), hole, Direction.Axis.Y, 1.0F,
					(m, deg) -> m.translate(0, head + down, 0).rotateY((float) Math.toRadians(deg))));
		}
		return out;
	}

	/** Client side: the top drive's height, easing toward where the stand puts it (back to the top in a connection). */
	private float head() {
		float target = connection > 0 || phase != Phase.DRILLING ? TOP_DRIVE_TOP : TOP_DRIVE_TOP - stand;
		long now = System.nanoTime();
		if (Float.isNaN(shownHead)) {
			shownHead = target;
		} else {
			float step = Math.min(0.25F, (now - shownAt) / 1e9F) * (target > shownHead ? 2.0F : 8.0F);
			shownHead = Math.abs(target - shownHead) <= step ? target : shownHead + Math.signum(target - shownHead) * step;
		}
		shownAt = now;
		return shownHead;
	}

	@Override
	public Spin spin() {
		return spin;
	}

	/** 120 rpm: shown at its real speed. */
	@Override
	public float displayDivisor() {
		return 1.0F;
	}

	// ---- saving, and the top drive dropping with the substructure ------------------------------------------------------------
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && topDrive) {
			Containers.dropItemStack(level, pos.getX(), pos.getY() + 6, pos.getZ(), new ItemStack(WildcatterItems.TOP_DRIVE));
		}
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
		output.putInt("phase", phase.ordinal());
		output.putBoolean("topDrive", topDrive);
		output.putInt("water", water);
		output.putInt("depth", depth);
		output.putInt("stand", stand);
		output.putDouble("progress", progress);
		output.putDouble("connection", connection);
		output.putBoolean("grown", grown);
		output.putFloat("rpm", spin.rpm());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		phase = Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, input.getIntOr("phase", 0)))];
		topDrive = input.getBooleanOr("topDrive", false);
		water = input.getIntOr("water", 0);
		depth = input.getIntOr("depth", 0);
		stand = input.getIntOr("stand", 0);
		progress = input.getDoubleOr("progress", 0);
		connection = input.getDoubleOr("connection", 0);
		grown = input.getBooleanOr("grown", grown);
		spin.setRpm(input.getFloatOr("rpm", 0));
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putInt("phase", phase.ordinal());
		tag.putBoolean("topDrive", topDrive);
		tag.putInt("stand", stand);
		tag.putDouble("connection", connection);
		tag.putFloat("rpm", spin.rpm());
		return tag;
	}
}
