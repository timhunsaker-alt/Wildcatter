package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The residual heat removal exchanger (docs/multiblocks/reactor.md, 8d), vertical, against the RHR pump: circulating water in on the
 * north face of its bottom cells, out warmed on the south face (to a cooling tower's riser, or discharged). The vessel works out the
 * heat; this pulls and gives back the water.
 */
public class RhrHeatExchangerBlockEntity extends PlantUnitBlockEntity {
	private double inC = Tuning.LAKE_C;
	private double discharged;
	private String unit = "idle";

	public RhrHeatExchangerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RHR_HX_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 1 && at[1] == 0 && side == Direction.SOUTH ? Product.COOLING_WATER : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.RHR_CW_KGS * 2;
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	/** Pulls up to {@code kg} of circulating water; returns what came (its temperature in {@link #inC()}). */
	public double pull(Level level, double kg) {
		double[] mix = new double[5];
		double got = kg <= 1e-9 ? 0 : pullMixed(level, inlet(), Product.COOLING_WATER, kg, mix);
		if (got > 0) {
			inC = mix[0] > 0.1 ? mix[0] : Tuning.LAKE_C;
		}
		return got;
	}

	public double inC() {
		return inC;
	}

	public void giveBack(double kg, double c, String status) {
		double fits = Math.min(kg, room(Product.COOLING_WATER));
		if (fits > 0) {
			addMixed(Product.COOLING_WATER, fits, new double[] {c, 0, 0, 0, 0});
		}
		discharged += kg - fits;
		unit = status;
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("RHR heat exchanger: " + unit);
	}
}
