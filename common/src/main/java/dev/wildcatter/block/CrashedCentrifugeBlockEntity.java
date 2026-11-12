package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 16c2 (docs/multiblocks/enrichment-operations.md item 4): what a centrifuge leaves when its rotor breaks up in its casing. It turns no
 * more and drops out of the cascade's count; broken, it gives scrap: steel and a carbon-fibre rotor shard.
 */
public class CrashedCentrifugeBlockEntity extends BlockEntity implements Inspectable {
	private String reason = "";

	public CrashedCentrifugeBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CRASHED_CENTRIFUGE_ENTITY, pos, state);
	}

	public void setReason(String reason) {
		this.reason = reason;
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Gas centrifuge: CRASHED" + (reason.isEmpty() ? "" : ": " + reason) + "; break it for scrap");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("reason", reason);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		reason = input.getStringOr("reason", "");
	}
}
