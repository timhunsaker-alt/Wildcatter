package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Fuel;
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
 * A powered stage of the fuel plant's line (docs/multiblocks/fuel.md), each against the one before it: the UF6 autoclave (a 30B by
 * hopper, the empty out below), the IDR kiln (hydrogen off its east pipe, pumped water), the granulator, the pellet press, the
 * sintering furnace (hydrogen off its east pipe), the pellet grinder and the rod loader (cladding tubes and steel by hopper), whose
 * loaded rods wait for the rod welder.
 */
public class FuelLineBlockEntity extends UnitMotorBlockEntity implements FuelStage, HopperFed, WaterUser {
	private static final double WATER_HOLD = 200.0;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(3, ItemStack.EMPTY);
	private double u;          // kg of U held for the next stage
	private boolean loaded;    // the autoclave's 30B
	private double uf6;        // kg left in it
	private double water;      // the kiln's, L
	private double steel;      // the loader's, kg
	private double progress;   // the loader's rod on the bench, kg of U
	private int rods;          // the loader's finished rods
	private double hf, swarf;
	private String unit = "starting";

	public FuelLineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FUEL_LINE_ENTITY, pos, state);
	}

	// ---- uranium it holds -----------------------------------------------------------------------------------------------------
	@Override
	public double fuelU() {
		return u;
	}

	@Override
	public double takeFuelU(double kg) {
		double got = Math.max(0, Math.min(kg, u));
		u -= got;
		setChanged();
		return got;
	}

	@Override
	public void addFuelU(double kg) {
		if (kind().equals("rod_loader")) {
			progress += kg;
			while (progress >= Fuel.rodU()) {
				progress -= Fuel.rodU();
				rods++;
			}
		} else {
			u = Math.min(Tuning.FUEL_STAGE_HOLD_KG, u + kg);
		}
		setChanged();
	}

	/** The rod loader's finished rods, for the welder. */
	public int rods() {
		return rods;
	}

	public boolean takeRod() {
		if (rods <= 0) {
			return false;
		}
		rods--;
		setChanged();
		return true;
	}

	// ---- hoppers and water ------------------------------------------------------------------------------------------------------
	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return takes(0, stack) || takes(1, stack);
	}

	private boolean takes(int slot, ItemStack stack) {
		return switch (kind()) {
			case "uf6_autoclave" -> slot == 0 && stack.is(WildcatterItems.LEU_CYLINDER_30B);
			case "rod_loader" -> slot == 0 && stack.is(WildcatterItems.ZIRCALOY_TUBE) || slot == 1 && stack.is(WildcatterItems.STEEL_INGOT);
			default -> false;
		};
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return switch (kind()) {
			case "uf6_autoclave" -> side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {1, 2} : new int[0];
			case "rod_loader" -> side == Direction.UP ? new int[] {0, 1} : new int[0];
			default -> new int[0];
		};
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takes(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takes(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return kind().equals("uf6_autoclave") && slot > 0 && side == Direction.DOWN;
	}

	@Override
	public double waterLevel() {
		return kind().equals("idr_kiln") ? water / WATER_HOLD : 1;
	}

	@Override
	public int addWater(int litres) {
		if (!kind().equals("idr_kiln")) {
			return 0;
		}
		int fits = (int) Math.max(0, Math.min(litres, WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	private List<Lines.Face> hydrogen() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	// ---- its work -------------------------------------------------------------------------------------------------------------
	private double rate() {
		return Tuning.FUEL_LINE_U_KGS * 1.2 * seconds();
	}

	private double room() {
		return Math.max(0, Tuning.FUEL_STAGE_HOLD_KG - u);
	}

	private @Nullable FuelStage up(Level level, String kind) {
		return Touching.find(level, worldPosition, FuelLineBlockEntity.class, f -> f.kind().equals(kind));
	}

	@Override
	protected void work(Level level) {
		switch (kind()) {
			case "uf6_autoclave" -> vaporize(level);
			case "idr_kiln" -> {
				double h2 = Lines.available(level, hydrogen(), Product.HYDROGEN, Pull.MACHINE, this);
				if (h2 <= 1e-9) {
					unit = "NO HYDROGEN: pipe hydrogen to the east face of its bottom cells";
					return;
				}
				if (water <= 1e-6) {
					unit = "NO WATER: pump water to it for its steam";
					return;
				}
				double max = Math.min(h2 / Fuel.hydrogenPerU(), water / Fuel.waterPerU());
				double got = pass(level, up(level, "uf6_autoclave"), "UF6 AUTOCLAVE", max, "converting UF6 to UO2 at 500-800 C");
				Lines.pull(level, hydrogen(), Product.HYDROGEN, got * Fuel.hydrogenPerU(), Pull.MACHINE, this);
				water = Math.max(0, water - got * Fuel.waterPerU());
				hf += got * Fuel.hfPerU();
			}
			case "granulator" -> pass(level, up(level, "idr_kiln"), "IDR KILN", Double.MAX_VALUE, "granulating the powder");
			case "pellet_press" -> pass(level, up(level, "granulator"), "GRANULATOR", Double.MAX_VALUE, "pressing green pellets");
			case "sintering_furnace" -> {
				double want = Tuning.SINTER_H2_M3H / 3600 * seconds();
				double h2 = Lines.available(level, hydrogen(), Product.HYDROGEN, Pull.MACHINE, this);
				if (h2 < want * 0.5) {
					unit = "NO HYDROGEN: pipe hydrogen to the east face of its bottom cells for its atmosphere";
					return;
				}
				double got = pass(level, up(level, "pellet_press"), "PELLET PRESS", Double.MAX_VALUE, "sintering at 1,750 C in hydrogen");
				if (got > 0) {
					Lines.pull(level, hydrogen(), Product.HYDROGEN, want, Pull.MACHINE, this);
				}
			}
			case "pellet_grinder" -> {
				double before = u;
				pass(level, up(level, "sintering_furnace"), "SINTERING FURNACE", Double.MAX_VALUE, "grinding to " + Tuning.PELLET_DIAMETER_MM + " mm");
				double ground = u - before;
				u -= ground * Tuning.GRIND_SWARF;
				swarf += ground * Tuning.GRIND_SWARF;
			}
			case "rod_loader" -> load(level);
			default -> {
			}
		}
	}

	/** Takes up to {@code max} kg of U from {@code up} at the line rate; returns what came. */
	private double pass(Level level, @Nullable FuelStage up, String upName, double max, String verb) {
		if (up == null) {
			unit = "NOT BUILT: set it against the " + upName;
			return 0;
		}
		if (up.fuelU() < 1e-9) {
			unit = "waiting for uranium from the " + upName.toLowerCase();
			return 0;
		}
		if (room() <= 1e-9) {
			unit = "FULL: the next stage is not taking it";
			return 0;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return 0;
		}
		double kg = up.takeFuelU(Math.min(Math.min(rate() * strength, room()), max));
		u += kg;
		unit = String.format("%s: %.1f kg of U an hour; %.2f kg held", verb, kg / seconds() * 3600, u);
		return kg;
	}

	private void vaporize(Level level) {
		if (loaded && uf6 <= 1e-6) {
			int out = slots.get(1).isEmpty() ? 1 : slots.get(2).isEmpty() ? 2 : -1;
			if (out < 0) {
				unit = "FULL: take the empty 30Bs (a hopper under it)";
				return;
			}
			slots.set(out, new ItemStack(WildcatterItems.EMPTY_30B_CYLINDER));
			loaded = false;
		}
		if (!loaded && fed() > 0) {
			useOne();
			loaded = true;
			uf6 = Tuning.CYLINDER_30B_KG;
		}
		if (!loaded) {
			unit = "NO CYLINDER: put a hopper of 30Bs of LEU on top";
			return;
		}
		if (room() <= 1e-9) {
			unit = "FULL: the kiln is not taking its UF6";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the heaters: " + describe();
			return;
		}
		double kg = Math.min(Math.min(rate() * strength, room()), uf6 / Tuning.UF6_PER_U);
		uf6 = Math.max(0, uf6 - kg * Tuning.UF6_PER_U);
		u += kg;
		unit = String.format("vaporizing the 30B's UF6: %.0f kg/h; %,.0f kg left in it (%.1f %%)", kg * Tuning.UF6_PER_U / seconds() * 3600, uf6,
				uf6 / Tuning.CYLINDER_30B_KG * 100);
	}

	/** For the game test: the 30B in the autoclave with only {@code kg} left. */
	public void setUf6ForTest(double kg) {
		loaded = true;
		uf6 = kg;
		setChanged();
	}

	private void load(Level level) {
		FuelStage up = up(level, "pellet_grinder");
		if (up == null) {
			unit = "NOT BUILT: set it against the PELLET GRINDER";
			return;
		}
		if (steel < Tuning.ROD_STEEL_KG && !slots.get(1).isEmpty()) {
			slots.get(1).shrink(1);
			steel += Tuning.STEEL_INGOT_KG;
		}
		if (progress <= 1e-9 && slots.get(0).isEmpty()) {
			unit = "NO TUBES: put Zircaloy cladding tubes in its hopper";
			return;
		}
		if (steel < Tuning.ROD_STEEL_KG && progress <= 1e-9) {
			unit = "NO STEEL: put steel ingots in its hopper (the plugs and plenum spring)";
			return;
		}
		if (rods >= 20) {
			unit = "FULL: the rod welder is not taking its rods";
			return;
		}
		if (up.fuelU() < 1e-9) {
			unit = "waiting for pellets from the pellet grinder";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		if (progress <= 1e-9) {   // a new rod: a tube on the bench, its bottom plug welded on
			slots.get(0).shrink(1);
			steel -= Tuning.ROD_STEEL_KG;
		}
		double kg = up.takeFuelU(Math.min(rate() * strength, Fuel.rodU() - progress));
		progress += kg;
		if (progress >= Fuel.rodU() - 1e-9) {
			progress = 0;
			rods++;
		}
		unit = String.format("loading rod pellets: %.0f of %d in the rod on the bench; %d rods ready", progress / Fuel.rodU() * Fuel.pelletsPerRod(), Fuel.pelletsPerRod(), rods);
	}

	@Override
	public Component inspect() {
		String label = switch (kind()) {
			case "uf6_autoclave" -> "UF6 autoclave: " + unit;
			case "idr_kiln" -> String.format("IDR kiln: %s; water %.0f/%.0f L; HF %.0f kg to the scrubber", unit, water, WATER_HOLD, hf);
			case "granulator" -> "Granulator: " + unit;
			case "pellet_press" -> "Pellet press: " + unit;
			case "sintering_furnace" -> "Sintering furnace (pusher): " + unit;
			case "pellet_grinder" -> String.format("Pellet grinder (centreless): %s; swarf %.1f kg of U", unit, swarf);
			case "rod_loader" -> String.format("Rod loader: %s; tubes %d, steel %.1f kg", unit, slots.get(0).getCount(), steel);
			default -> kind();
		};
		return Component.literal(label + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("u", u);
		output.putBoolean("loaded", loaded);
		output.putDouble("uf6", uf6);
		output.putDouble("water", water);
		output.putDouble("steel", steel);
		output.putDouble("progress", progress);
		output.putInt("rods", rods);
		output.putDouble("hf", hf);
		output.putDouble("swarf", swarf);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		u = input.getDoubleOr("u", 0);
		loaded = input.getBooleanOr("loaded", false);
		uf6 = input.getDoubleOr("uf6", 0);
		water = input.getDoubleOr("water", 0);
		steel = input.getDoubleOr("steel", 0);
		progress = input.getDoubleOr("progress", 0);
		rods = input.getIntOr("rods", 0);
		hf = input.getDoubleOr("hf", 0);
		swarf = input.getDoubleOr("swarf", 0);
	}
}
