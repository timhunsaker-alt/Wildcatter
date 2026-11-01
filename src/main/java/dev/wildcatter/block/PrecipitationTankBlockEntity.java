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
 */
public class PrecipitationTankBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double mgo;
	private double precipitate;   // kg of U3O8 in the wet precipitate
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
		return Component.literal("Precipitation tank: " + unit + String.format("; %.1f kg of U3O8 in its precipitate; MgO %,.0f kg and %d Magnesia; ", precipitate, mgo, fed())
				+ describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("mgo", mgo);
		output.putDouble("precipitate", precipitate);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		mgo = input.getDoubleOr("mgo", 0);
		precipitate = input.getDoubleOr("precipitate", 0);
	}
}
