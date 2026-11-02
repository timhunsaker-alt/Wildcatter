package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The cold-water basin at the foot of a cooling tower. Steam machines find the tower through it, on their water pipes. */
public class TowerBasinBlockEntity extends BlockEntity implements dev.wildcatter.multiblock.BigPartEntity {
	private boolean grown;

	public TowerBasinBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TOWER_BASIN_ENTITY, pos, state);
	}

	/** True while the tower above is built, its fan has power, and it is cooling. */
	public boolean cooling() {
		return level != null && level.getBlockEntity(worldPosition.above(TowerColumn.DECK_HEIGHT)) instanceof CoolingTowerBlockEntity tower && tower.isCooling();
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
