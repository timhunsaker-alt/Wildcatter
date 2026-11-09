package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.AmmoniaPlant;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The emulsion blender (docs/multiblocks/ammonia-plant.md): a high-shear blender making the emulsion matrix (75 % AN, 18 % water,
 * 6 % diesel, 1 % emulsifier) from hot AN solution and diesel on the east face of its bottom cells, emulsifier pails by a hopper on
 * top or by hand, and make-up water off a pump. The matrix leaves the south face of its back cells for a stock tank or a bulk truck.
 * It is an oxidiser, not an explosive until it is gassed in the hole. Its mixer is its load (5.5 kW at the industrial voltage), its
 * terminal box on the west face of its main cell.
 */
public class EmulsionBlenderBlockEntity extends UnitMotorBlockEntity implements HopperFed, WaterUser {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double emulsifier;   // kg tipped in
	private double water;
	private double making;       // kg/h of matrix, the last interval
	private String unit = "starting";

	public EmulsionBlenderBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.EMULSION_BLENDER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.EMULSIFIER_PAIL);
	}

	private List<Lines.Face> inlets() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 3 && at[1] == 0 && side == Direction.SOUTH ? Product.EMULSION_MATRIX : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.EMULSION_MATRIX ? AmmoniaPlant.matrixKgh() / 3600 * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	@Override
	public double waterLevel() {
		return water / Tuning.AMMONIA_PLANT_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.AMMONIA_PLANT_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	/** kg/h of matrix over the last interval. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double max = Math.min(AmmoniaPlant.matrixKgh() / 3600 * seconds(), room(Product.EMULSION_MATRIX));
		if (emulsifier < max * Tuning.MATRIX_EMULSIFIER && fed() > 0) {
			useOne();
			emulsifier += Tuning.EMULSIFIER_PAIL_KG;
		}
		double solution = Lines.available(level, inlets(), Product.AN_SOLUTION, Pull.MACHINE, this);
		double diesel = Lines.available(level, inlets(), Product.DIESEL, Pull.MACHINE, this);
		if (solution < 1e-9) {
			unit = "NO AN SOLUTION: pipe a neutraliser's solution to its east side";
			return;
		}
		if (diesel < 1e-9) {
			unit = "NO DIESEL: pipe diesel to its east side";
			return;
		}
		if (emulsifier < 1e-9) {
			unit = "NO EMULSIFIER: put emulsifier pails in a hopper on its top, or click one in";
			return;
		}
		if (water < 1e-6) {
			unit = "NO WATER: pump water to it";
			return;
		}
		double kg = Math.min(max, solution / AmmoniaPlant.solutionPerMatrix());
		kg = Math.min(kg, diesel / AmmoniaPlant.dieselLPerMatrix());
		kg = Math.min(kg, emulsifier / Tuning.MATRIX_EMULSIFIER);
		kg = Math.min(kg, water / AmmoniaPlant.blenderWaterPerMatrix());
		if (kg <= 1e-9) {
			unit = "idle: FULL, its matrix has nowhere to go (pipe its south side to a stock tank)";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		kg *= strength;
		kg = Lines.pull(level, inlets(), Product.AN_SOLUTION, kg * AmmoniaPlant.solutionPerMatrix(), Pull.MACHINE, this) / AmmoniaPlant.solutionPerMatrix();
		kg = Math.min(kg, Lines.pull(level, inlets(), Product.DIESEL, kg * AmmoniaPlant.dieselLPerMatrix(), Pull.MACHINE, this) / AmmoniaPlant.dieselLPerMatrix());
		emulsifier = Math.max(0, emulsifier - kg * Tuning.MATRIX_EMULSIFIER);
		water = Math.max(0, water - kg * AmmoniaPlant.blenderWaterPerMatrix());
		add(Product.EMULSION_MATRIX, kg);
		making = kg / seconds() * 3600;
		unit = String.format("blending %.1f kg/h of emulsion matrix (%.0f %% AN, %.0f %% water, %.0f %% diesel, %.0f %% emulsifier)", making,
				Tuning.MATRIX_AN * 100, Tuning.MATRIX_WATER * 100, Tuning.MATRIX_DIESEL * 100, Tuning.MATRIX_EMULSIFIER * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Emulsion blender (%.1f kW, %s): ", Tuning.EMULSION_BLENDER_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; emulsifier %.1f kg and %d pails; water %.0f/%.0f L; matrix held %.2f kg; ", emulsifier, fed(), water, Tuning.AMMONIA_PLANT_WATER_HOLD,
						held(Product.EMULSION_MATRIX)) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("emulsifier", emulsifier);
		output.putDouble("water", water);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		emulsifier = input.getDoubleOr("emulsifier", 0);
		water = input.getDoubleOr("water", 0);
	}
}
