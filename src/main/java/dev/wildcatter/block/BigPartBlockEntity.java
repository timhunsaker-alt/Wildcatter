package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The block entity of a true-size part that does nothing itself (a tower's fill or drift eliminators): it only remembers that
 * the part has grown into its cells, so a grown part with a piece gone is broken rather than taken for an old lone block.
 */
public class BigPartBlockEntity extends BlockEntity implements BigPartEntity {
	private boolean grown;

	public BigPartBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BIG_PART_ENTITY, pos, state);
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
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
	}
}
