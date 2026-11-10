package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The vacuum crystallizer (docs/multiblocks/fertilizer.md): multi-stage vacuum cooling crystallizers against the potash dissolver,
 * which runs them. KCl crystallizes as the hot brine cools and the brine returns to the dissolver; the potash dryer goes against it.
 * Steam ejectors on process steam at the south face of its bottom cells; 110 kW of pumps on the world's industrial voltage, the box on
 * the north face of its main cell (the dissolver covers its west side).
 */
public class VacuumCrystallizerBlockEntity extends UnitMotorBlockEntity {
	String unit = "idle: no brine from its dissolver";

	public VacuumCrystallizerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.VACUUM_CRYSTALLIZER_ENTITY, pos, state);
	}

	List<Lines.Face> steamIn() {
		return faces(at -> at[2] == 2 && at[1] == 0, Direction.SOUTH);
	}

	/** Its box is on the north face of its main cell: the part it stands against covers its west side. */
	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Vacuum crystallizer (pumps %.0f kW, %s): ", Tuning.VACUUM_CRYSTALLIZER_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit + "; "
				+ describe());
	}
}
