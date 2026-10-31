package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.kinetic.Kinetic;
import dev.wildcatter.kinetic.RotorBlock;
import dev.wildcatter.kinetic.Spin;
import dev.wildcatter.multiblock.BigPartEntity;
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
 * A land rig part with no logic of its own (docs/multiblocks/land-rig.md): the mast, the drawworks, a mud pump or the mud tanks.
 * The pumps' sheaves turn while the rig pumps mud and the drawworks drum turns in a connection; a click reads the rig.
 */
public class RigPartBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, Kinetic {
	private final Spin spin = new Spin();
	private boolean grown;

	public RigPartBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RIG_PART_ENTITY, pos, state);
	}

	public RigPartBlock.Part part() {
		return ((RigPartBlock) getBlockState().getBlock()).part();
	}

	public RigSubstructureBlockEntity rig() {
		return level == null ? null : RigPartBlock.controller(level, worldPosition, getBlockState());
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RigPartBlockEntity part) {
		if (level.getGameTime() % 10 != 0 || (part.part() != RigPartBlock.Part.MUD_PUMP && part.part() != RigPartBlock.Part.DRAWWORKS)) {
			return;
		}
		RigSubstructureBlockEntity rig = part.rig();
		boolean turning = rig != null && rig.formed() && (part.part() == RigPartBlock.Part.MUD_PUMP ? rig.pumping() : rig.connecting());
		part.spin.setRpm(!turning ? 0 : (float) (part.part() == RigPartBlock.Part.MUD_PUMP ? Tuning.MUD_PUMP_SHEAVE_RPM : Tuning.DRAWWORKS_DRUM_RPM));
		if (part.spin.needsSync()) {
			part.spin.markSynced();
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	@Override
	public Component inspect() {
		RigSubstructureBlockEntity rig = rig();
		String name = switch (part()) {
			case MAST -> "Rig mast (43 m)";
			case DRAWWORKS -> "Drawworks (1,000 hp)";
			case MUD_PUMP -> "Mud pump (1,000 hp triplex)";
			case MUD_TANKS -> "Mud tanks (60 m3)";
			default -> "Rig part";
		};
		return Component.literal(name + ": " + (rig != null ? rig.inspect().getString() : "NOT PART OF A RIG: set it in its place by a substructure"));
	}

	// ---- moving parts ------------------------------------------------------------------------------------------------------
	@Override
	public List<Rotor> rotors() {
		RotorBlock rotor = (RotorBlock) WildcatterBlocks.ROTOR;
		return switch (part()) {
			// the motor's sheave and the bull wheel on the pinion, on the south side under the open belt guard
			case MUD_PUMP -> List.of(new Rotor(rotor.part(RotorBlock.Part.MUD_PUMP_SHEAVE), new Vector3f(4.3F, 1.5F, 3.05F), Direction.Axis.Z, 1.0F),
					new Rotor(rotor.part(RotorBlock.Part.MUD_PUMP_BULL_WHEEL), new Vector3f(2.4F, 1.5F, 3.05F), Direction.Axis.Z, 0.28F));
			case DRAWWORKS -> List.of(new Rotor(rotor.part(RotorBlock.Part.DRAWWORKS_DRUM), new Vector3f(2.6F, 1.3F, 1.5F), Direction.Axis.X, 1.0F));
			default -> List.of();
		};
	}

	@Override
	public Direction facing() {
		return getBlockState().getValue(RigPartBlock.FACING);
	}

	@Override
	public Spin spin() {
		return spin;
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
		output.putBoolean("grown", grown);
		output.putFloat("rpm", spin.rpm());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
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
		tag.putFloat("rpm", spin.rpm());
		return tag;
	}
}
