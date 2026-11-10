package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Phosphate;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The tilting pan filter (docs/multiblocks/fertilizer.md): a tilting-pan vacuum filter with three washes, against the phosphoric
 * reactor's east side, which runs it. Filter acid (28 % P2O5) out of the south face of its back cells; the gypsum, repulped in water off
 * a pump to the tailings cell's 40 % solids, out of the west face of its two back rows as tails slurry that carries a phosphogypsum
 * assay, for a lined tailings cell (docs/multiblocks/tailings-cell.md). Its vacuum pump is 250 kW on the world's medium voltage, the
 * box on the north face of its main cell (the reactor covers its west side).
 */
public class PanFilterBlockEntity extends UnitMotorBlockEntity implements WaterUser, Assayed {
	/** The assay key of the gypsum: a tails mineral, not an item. */
	public static final String GYPSUM = "wildcatter:phosphogypsum";
	private double water;
	String unit = "idle: no slurry from its reactor";

	public PanFilterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TILTING_PAN_FILTER_ENTITY, pos, state);
	}

	/** Its box is on the north face of its main cell: the part it stands against covers its west side. */
	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0) {
			return null;
		}
		if (at[2] == 9 && side == Direction.SOUTH) {
			return Product.FILTER_ACID;
		}
		return at[0] == 0 && at[2] >= 8 && side == Direction.WEST ? Product.TAILS_SLURRY : null;
	}

	@Override
	protected double hold(Product product) {
		return switch (product) {
			case FILTER_ACID -> Phosphate.filterAcidLps() * Tuning.PLANT_HOLD_SECONDS * 4;
			case TAILS_SLURRY -> Phosphate.reactorRockKgs() * Phosphate.gypsumPerRock() * Phosphate.tailsPerGypsum() * Tuning.PLANT_HOLD_SECONDS * 4;
			default -> 0;
		};
	}

	@Override
	public SlurryAssay assayOf(Product product) {
		return product == Product.TAILS_SLURRY ? SlurryAssay.of(Map.of(GYPSUM, Tuning.CELL_SLURRY_SOLIDS)) : SlurryAssay.EMPTY;
	}

	@Override
	public double waterLevel() {
		return water / Tuning.PHOSPHATE_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.PHOSPHATE_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	double water() {
		return water;
	}

	void useWater(double kg) {
		water = Math.max(0, water - kg);
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Tilting pan filter (vacuum pump %.0f kW, %s): ", Tuning.PAN_FILTER_WATTS / 1000, Voltage.MEDIUM.label()) + unit
				+ String.format("; filter acid held %.0f L; gypsum tails held %.0f kg; water %.0f/%.0f L; ", held(Product.FILTER_ACID), held(Product.TAILS_SLURRY), water,
						Tuning.PHOSPHATE_WATER_HOLD)
				+ describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("water", water);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getDoubleOr("water", 0);
	}
}
