package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Nickel;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
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
 * The fluid-bed roaster (docs/multiblocks/nickel-smelter.md): nickel sulfide feed roasted at about 700 C in a bed fluidized by its blower
 * (110 kW at MV, box west), with its own feed mill and cyclones. Nickel ore by a hopper on top (one ore item is about 4 t of feed at 12 %
 * Ni), or thickened concentrate carrying pentlandite on the north face of its bottom cells. 2 t of feed an hour; 70 % of the sulfur goes
 * up as SO2, out its south side as burner gas for an acid plant's converter (NO ACID PLANT otherwise: a roaster must not vent). The
 * calcine waits for the electric furnace it touches.
 */
public class FluidBedRoasterBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	public static final String PENTLANDITE = "wildcatter:pentlandite";

	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double feed;        // kg of feed opened into its feed mill
	private double feedNi;      // kg of Ni in it
	private double calcine;     // kg of calcine for the furnace
	private double calcineNi;   // kg of Ni in it
	private double making;      // kg/h of feed roasted, the last interval
	private double roasted;     // t, all told
	private String unit = "starting";

	public FluidBedRoasterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FLUID_BED_ROASTER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.PENTLANDITE)) || stack.is(OreBlocks.block(Ore.PENTLANDITE).asItem());
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[1] == 0 && at[2] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[0] == 1 && at[1] == 0 && at[2] == 3 && side == Direction.SOUTH ? Product.BURNER_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.BURNER_GAS ? Tuning.NI_ROASTER_FEED_KGH / 3600 * Nickel.burnerGasPerFeed() * Tuning.PLANT_HOLD_SECONDS : 0;
	}

	public double making() {
		return making;
	}

	public double calcine() {
		return calcine;
	}

	/** The furnace takes up to {@code kg} of calcine; returns the kg of Ni that came with what it gave (the kg given in {@code got[0]}). */
	public double takeCalcine(double kg, double[] got) {
		double give = Math.max(0, Math.min(kg, calcine));
		double ni = calcine > 0 ? calcineNi * give / calcine : 0;
		calcine -= give;
		calcineNi -= ni;
		got[0] = give;
		setChanged();
		return ni;
	}

	/** For the game test: calcine put straight into it. */
	public void testAddCalcine(double kg) {
		calcine += kg;
		calcineNi += kg * Tuning.NICKEL_FEED_NI;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double dt = seconds();
		double want = Tuning.NI_ROASTER_FEED_KGH / 3600 * dt;
		// feed: an ore item opened into the mill, or concentrate in from the north
		if (feed < want && !slots.get(0).isEmpty()) {
			slots.get(0).shrink(1);
			feed += Tuning.NICKEL_ORE_KG;
			feedNi += Tuning.NICKEL_ORE_KG * Tuning.NICKEL_FEED_NI;
		}
		if (feed < Tuning.NI_ROASTER_SLURRY_HOLD_KG * 0.05) {
			Slurry.Taken got = Slurry.take(level, inlet(), Product.THICKENED_CONCENTRATE, 50, this);
			double pn = got.assay().kg(PENTLANDITE, got.kg());
			feed += pn;   // its pentlandite counted as feed at the concentrate grade (questions file)
			feedNi += pn * Tuning.NICKEL_FEED_NI;
		}
		if (feed <= 1e-6) {
			unit = "NO FEED: put nickel ore in a hopper on its top, or pipe thickened concentrate carrying pentlandite to its north side";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		if (calcine >= Tuning.NI_ROASTER_CALCINE_HOLD_KG) {
			unit = Touching.find(level, worldPosition, NickelFurnaceBlockEntity.class, f -> true) == null
					? "FULL: set the nickel electric furnace against it to take its calcine" : "FULL: the furnace is not taking its calcine";
			return;
		}
		double kg = Math.min(want, feed);
		double gas = kg * Nickel.burnerGasPerFeed();
		if (room(Product.BURNER_GAS) < gas) {
			unit = "NO ACID PLANT: pipe its burner gas (south side) to an acid plant's converter; a roaster must not vent";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the fluidizing blower: " + describe();
			return;
		}
		kg *= strength;
		double ni = feedNi * kg / feed;
		feed -= kg;
		feedNi -= ni;
		add(Product.BURNER_GAS, kg * Nickel.burnerGasPerFeed());
		calcine += kg * Nickel.calcinePerFeed();
		calcineNi += ni;
		roasted += kg / 1000;
		making = kg / dt * 3600;
		unit = String.format("roasting at 700 C: %.2f t/h of feed, %.0f kg/h of sulfur to the acid plant as SO2", making / 1000, making * Nickel.roasterSulfurPerFeed());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Fluid-bed roaster (%.0f kW blower, %s): ", Tuning.NI_ROASTER_WATTS / 1000, Voltage.MEDIUM.label()) + unit
				+ String.format("; feed %.1f t in the mill, %d ore in its bin; calcine %.1f t for the furnace (%.0f kg of Ni); %.1f t roasted; ", feed / 1000,
						slots.get(0).getCount(), calcine / 1000, calcineNi, roasted)
				+ describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, making > 0, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(making, Tuning.NI_ROASTER_FEED_KGH);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("feed", feed);
		output.putDouble("feedNi", feedNi);
		output.putDouble("calcine", calcine);
		output.putDouble("calcineNi", calcineNi);
		output.putDouble("roasted", roasted);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		feed = input.getDoubleOr("feed", 0);
		feedNi = input.getDoubleOr("feedNi", 0);
		calcine = input.getDoubleOr("calcine", 0);
		calcineNi = input.getDoubleOr("calcineNi", 0);
		roasted = input.getDoubleOr("roasted", 0);
	}
}
