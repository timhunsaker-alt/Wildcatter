package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The cold-water basin at the foot of a cooling tower. Steam machines find the tower through it, on their water pipes. */
public class TowerBasinBlockEntity extends BlockEntity {
	public TowerBasinBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TOWER_BASIN_ENTITY, pos, state);
	}

	/** True while the tower above is built, its fan has power, and it is cooling. */
	public boolean cooling() {
		return level != null && level.getBlockEntity(worldPosition.above(3)) instanceof CoolingTowerBlockEntity tower && tower.isCooling();
	}
}
