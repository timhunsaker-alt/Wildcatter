package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The land rig's blowout preventer stack (docs/multiblocks/drilling-mud.md, 15b2): a 13-5/8 in, 5,000 psi stack nippled up on the
 * wellhead in the substructure's BOP space under the rotary table: the annular preventer over the pipe rams and the blind-shear rams,
 * the choke and kill lines off its spool. It has no power of its own: its accumulator closes it. The rig works it.
 */
public class BopStackBlockEntity extends BlockEntity implements Inspectable, BigPartEntity {
	private boolean grown;
	private boolean closed;

	public BopStackBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BOP_STACK_ENTITY, pos, state);
	}

	public boolean closed() {
		return closed;
	}

	void setClosed(boolean value) {
		closed = value;
		setChanged();
	}

	/** The rig whose hole it stands on, or null. */
	public RigSubstructureBlockEntity rig() {
		if (level == null) {
			return null;
		}
		BlockPos origin = LandRig.substructureNear(level, worldPosition);
		return origin != null && level.getBlockEntity(origin) instanceof RigSubstructureBlockEntity rig && rig.hole().above().equals(worldPosition) ? rig : null;
	}

	@Override
	public Component inspect() {
		RigSubstructureBlockEntity rig = rig();
		return Component.literal("BOP stack (13-5/8 in, 5,000 psi; annular, pipe rams, blind-shear rams): " + (closed ? "CLOSED" : "open") + "; "
				+ (rig != null ? rig.inspect().getString() : "NOT ON A RIG: set it under a land rig's rotary table"));
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
		output.putBoolean("closed", closed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		closed = input.getBooleanOr("closed", false);
	}
}
