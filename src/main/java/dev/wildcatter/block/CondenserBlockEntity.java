package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The main condenser (docs/multiblocks/turbine-island.md), against the LP turbine: circulating water in on the north face of its
 * bottom cells, out on the south (discharged to the lake, counted). The HP turbine works out its vacuum from the water it gets.
 */
public class CondenserBlockEntity extends PlantUnitBlockEntity {
	private double discharged;   // kg
	private String unit = "NOT BUILT: set it against the LP TURBINE";

	public CondenserBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CONDENSER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[1] == 0, Direction.NORTH);
	}

	/** Pulls up to {@code kg} of circulating water through its tubes; returns what came. */
	public double pullCooling(Level level, double kg) {
		double got = kg <= 1e-9 ? 0 : Lines.pull(level, inlet(), Product.COOLING_WATER, kg, Pull.MACHINE, this);
		discharged += got;
		setChanged();
		return got;
	}

	public void status(String status) {
		unit = status;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Main condenser: %s; %,.0f t of circulating water discharged", unit, discharged / 1000));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("discharged", discharged);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		discharged = input.getDoubleOr("discharged", 0);
	}
}
