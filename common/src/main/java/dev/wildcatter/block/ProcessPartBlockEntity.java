package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A process part with no motor of its own in its spec (the caustic evaporator on steam, the HCl burner): it works each interval, held
 * by a PLC's hold as any plant unit is.
 */
public abstract class ProcessPartBlockEntity extends PlantUnitBlockEntity {
	protected ProcessPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ProcessPartBlockEntity part) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		if (part.plcHold(level)) {
			return;
		}
		part.work(level);
		part.setChanged();
	}

	protected abstract void work(Level level);
}
