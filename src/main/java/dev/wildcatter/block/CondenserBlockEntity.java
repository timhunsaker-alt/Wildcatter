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

	private double inC = dev.wildcatter.Tuning.LAKE_C;

	/** The warmed circulating water goes back out on the south face of its bottom cells, to a cooling tower's riser (8c). */
	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 7 && at[1] == 0 && side == Direction.SOUTH ? Product.COOLING_WATER : null;
	}

	@Override
	protected double hold(Product product) {
		return 2 * 2 * dev.wildcatter.Tuning.CW_PUMP_KGS;
	}

	/** The temperature the circulating water came in at, C. */
	public double inC() {
		return inC;
	}

	/** Offers {@code kg} of warmed water at {@code c} C back out; what no pipe takes in time is discharged. */
	public void giveBack(double kg, double c) {
		double fits = Math.min(kg, room(Product.COOLING_WATER));
		if (fits > 0) {
			addMixed(Product.COOLING_WATER, fits, new double[] {c, 0, 0, 0, 0});
		}
		discharged += kg - fits;
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[1] == 0, Direction.NORTH);
	}

	/** Pulls up to {@code kg} of circulating water through its tubes; returns what came. */
	public double pullCooling(Level level, double kg) {
		double[] mix = new double[5];
		double got = kg <= 1e-9 ? 0 : pullMixed(level, inlet(), Product.COOLING_WATER, kg, mix);
		if (got > 0) {
			inC = mix[0] > 0.1 ? mix[0] : dev.wildcatter.Tuning.LAKE_C;
		}
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
