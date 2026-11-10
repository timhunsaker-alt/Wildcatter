package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
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
 * The precipitation tank (15 kW agitator, 480 V) (docs/multiblocks/uranium-mill.md): milk of magnesia, from Magnesia by a hopper on
 * top, takes the strip liquor (north face of its front cells), or an ISR wellfield's acidified eluate, to pH 7 and the uranium comes down as magnesium diuranate. It holds the
 * wet precipitate for the yellowcake dryer touching it.
 * <p>16a (docs/multiblocks/adu-yellowcake.md): with liquid ammonia piped to the east face of its bottom cells it precipitates
 * ammonium diuranate instead, at pH 7.5 and 50 C, for a uranium calciner touching it. The two never share a batch: each is held
 * apart, the ADU for the calciner and the magnesium diuranate for the dryer. Without ammonia it runs on magnesia as it always has.
 */
public class PrecipitationTankBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double mgo;
	private double precipitate;   // kg of U3O8 in the wet precipitate
	private double adu;           // kg of U3O8 in the wet ammonium diuranate (16a)
	private double ammoniaSalt;   // kg of ammonium chloride or sulfate left in the barren liquor, counted
	private boolean aduRoute;     // its last batch was ADU: without ammonia now it reads NO AMMONIA
	private String unit = "starting";

	public PrecipitationTankBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PRECIPITATION_TANK_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.MAGNESIA);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	/** 16a: the ammonia nozzle, the east face of its bottom cells. */
	private List<Lines.Face> ammonia() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	private static double precipitateHold() {
		return Tuning.MILL_U3O8_KGS * Tuning.PLANT_HOLD_SECONDS * 4;
	}

	/** The dryer takes up to {@code kg} of U3O8 in precipitate; returns what it got. */
	public double takePrecipitate(double kg) {
		double got = Math.min(kg, precipitate);
		precipitate -= got;
		setChanged();
		return got;
	}

	/** The calciner takes up to {@code kg} of U3O8 in ADU; returns what it got (a negative amount puts some back). */
	public double takeAdu(double kg) {
		double got = Math.min(kg, adu);
		adu -= got;
		setChanged();
		return got;
	}

	public double adu() {
		return adu;
	}

	/** For the game test: ADU put straight into the tank. */
	public void addAduForTest(double kg) {
		adu += kg;
		setChanged();
	}

	/** Litres of liquid ammonia a kg of U3O8 precipitated as ADU. */
	public static double ammoniaLitresPerU3o8() {
		return Tuning.ADU_NH3_PER_U3O8 / Tuning.AMMONIA_KG_PER_L;
	}

	@Override
	protected void work(Level level) {
		// strip liquor from the mill's SX, or acidified eluate from an ISR wellfield's IX columns (docs/multiblocks/isr-wellfield.md)
		Product feed = Product.STRIP_LIQUOR;
		double waiting = Lines.available(level, inlet(), feed, Pull.MACHINE, this);
		if (waiting < 0.001) {
			feed = Product.ELUATE;
			waiting = Lines.available(level, inlet(), feed, Pull.MACHINE, this);
		}
		if (waiting < 0.001) {
			unit = "idle: no strip liquor or eluate at its front (north face)";
			return;
		}
		double nh3 = Lines.available(level, ammonia(), Product.AMMONIA, Pull.MACHINE, this);
		if (nh3 > 1e-6) {
			aduRoute = true;
			double room = precipitateHold() - adu;
			if (room < 0.001) {
				unit = "FULL: the ADU waits for a uranium calciner against the tank";
				return;
			}
			double strength = drive(level);
			if (strength <= 0) {
				unit = "starting: " + describe();
				return;
			}
			double perLitre = (feed == Product.ELUATE ? Tuning.IX_ELUATE_G_PER_L : Tuning.SX_STRIP_U3O8_G_PER_L) / 1000;
			double max = Math.min(Math.min(waiting, Tuning.MILL_U3O8_KGS * 1.2 * seconds() * strength / perLitre), room / perLitre);
			max = Math.min(max, nh3 / ammoniaLitresPerU3o8() / perLitre);
			double[] in = new double[5];
			double got = pullMixed(level, inlet(), feed, max, in);
			double u = got * in[0];
			double litres = u * ammoniaLitresPerU3o8();
			Lines.pull(level, ammonia(), Product.AMMONIA, litres, Pull.MACHINE, this);
			adu += u;
			ammoniaSalt += litres * Tuning.AMMONIA_KG_PER_L * Tuning.ADU_BYPRODUCT_PER_NH3;
			String calciner = Touching.find(level, worldPosition, UraniumCalcinerBlockEntity.class, c -> true) == null ? "NO CALCINER (the ADU waits in the tank); " : "";
			unit = calciner + String.format("precipitating ADU (pH 7.5, 50 C): %.3f kg/s of U3O8 from %.2f L/s of %s with %.1f L/h of ammonia", u / seconds(), got / seconds(),
					feed.label(), litres / seconds() * 3600);
			return;
		}
		if (aduRoute && (mgo < 1 && fed() == 0)) {
			unit = "NO AMMONIA: pipe liquid ammonia to its east side, or put a hopper of magnesia on it for the magnesia route";
			return;
		}
		aduRoute = false;
		if (mgo < 1 && fed() > 0) {
			useOne();
			mgo += dev.wildcatter.ore.Ore.MAGNESITE.kgPerBlock() * Tuning.MGO_IN_MGCO3;
		}
		if (mgo < 0.001) {
			unit = "NO MAGNESIA: put a hopper of magnesia on the tank (calcine magnesite in a furnace)";
			return;
		}
		double room = precipitateHold() - precipitate;
		if (room < 0.001) {
			unit = "FULL: the precipitate waits for a yellowcake dryer against the tank";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double perLitre = (feed == Product.ELUATE ? Tuning.IX_ELUATE_G_PER_L : Tuning.SX_STRIP_U3O8_G_PER_L) / 1000;
		double max = Math.min(Math.min(waiting, Tuning.MILL_U3O8_KGS * 1.2 * seconds() * strength / perLitre), room / perLitre);
		max = Math.min(max, mgo / Tuning.PRECIP_MGO_PER_U3O8 / perLitre);
		double[] in = new double[5];
		double got = pullMixed(level, inlet(), feed, max, in);
		double u = got * in[0];
		mgo = Math.max(0, mgo - u * Tuning.PRECIP_MGO_PER_U3O8);
		precipitate += u;
		unit = String.format("precipitating %.3f kg/s of U3O8 from %.2f L/s of %s with %.3f kg/s of MgO", u / seconds(), got / seconds(), feed.label(),
				u * Tuning.PRECIP_MGO_PER_U3O8 / seconds());
	}

	@Override
	public Component inspect() {
		String route = adu > 0.05 || aduRoute ? String.format("; %.1f kg of U3O8 as ADU; %,.0f kg of ammonium salt to the barren liquor", adu, ammoniaSalt) : "";
		return Component.literal("Precipitation tank: " + unit + String.format("; %.1f kg of U3O8 in its precipitate; MgO %,.0f kg and %d Magnesia", precipitate, mgo, fed())
				+ route + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("mgo", mgo);
		output.putDouble("precipitate", precipitate);
		output.putDouble("adu", adu);
		output.putDouble("ammoniaSalt", ammoniaSalt);
		output.putBoolean("aduRoute", aduRoute);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		mgo = input.getDoubleOr("mgo", 0);
		precipitate = input.getDoubleOr("precipitate", 0);
		adu = input.getDoubleOr("adu", 0);
		ammoniaSalt = input.getDoubleOr("ammoniaSalt", 0);
		aduRoute = input.getBooleanOr("aduRoute", false);
	}
}
