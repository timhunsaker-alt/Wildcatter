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
	private static final int SCRAP_OUT = 3;   // 19c1: the press's and the grinder's scrap cans, for a hopper under them
	private final NonNullList<ItemStack> slots = NonNullList.withSize(4, ItemStack.EMPTY);
	private final ScrapCans scrap = new ScrapCans();
	private double lubricant;  // the press's zinc stearate, kg in its feeder
	private double u;          // kg of U held for the next stage
	private boolean loaded;    // the autoclave's 30B
	private double uf6;        // kg left in it
	private double water;      // the kiln's, L
	private double steel;      // the loader's, kg
	private double progress;   // the loader's rod on the bench, kg of U
	private int rods;          // the loader's finished rods
	private double hf, swarf;
	private double assay = Tuning.ASSAY_PRODUCT;       // 16c1: of the uranium it holds (the loader: of its rods)
	private double cylinderAssay = Tuning.ASSAY_PRODUCT;   // the autoclave's 30B
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
	public double fuelAssay() {
		return assay;
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
			case "pellet_press" -> slot == 0 && stack.is(WildcatterItems.ZINC_STEARATE);
			default -> false;
		};
	}

	private boolean scrapping() {
		return kind().equals("pellet_press") || kind().equals("pellet_grinder");
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return switch (kind()) {
			case "uf6_autoclave" -> side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {1, 2} : new int[0];
			case "rod_loader" -> side == Direction.UP ? new int[] {0, 1} : new int[0];
			case "pellet_press" -> side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {SCRAP_OUT} : new int[0];
			case "pellet_grinder" -> side == Direction.DOWN ? new int[] {SCRAP_OUT} : new int[0];
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
		return kind().equals("uf6_autoclave") && slot > 0 && slot < SCRAP_OUT && side == Direction.DOWN || scrapping() && slot == SCRAP_OUT && side == Direction.DOWN;
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
			case "granulator" -> {
				double got = pass(level, up(level, "idr_kiln"), "IDR KILN", Double.MAX_VALUE, "granulating the powder");
				// 19c1: the scrap oxidizer's U3O8 blended in, at most a tenth of the feed: the pore former
				ScrapOxidizerBlockEntity oxidizer = Touching.find(level, worldPosition, ScrapOxidizerBlockEntity.class, o -> true);
				if (oxidizer != null && got > 0 && oxidizer.oxideU() > 0) {
					double a = oxidizer.assay();
					double ox = oxidizer.takeOxide(Math.min(got * Tuning.SCRAP_SHARE_MAX / (1 - Tuning.SCRAP_SHARE_MAX), room()));
					assay = FuelStage.blend(u, assay, ox, a);
					u += ox;
					unit += String.format("; %.1f kg/h of recycled U3O8 blended in", ox / seconds() * 3600);
				}
			}
			case "pellet_press" -> {
				double before = u;
				pass(level, up(level, "granulator"), "GRANULATOR", Double.MAX_VALUE, "pressing green pellets");
				double pressed = u - before;
				// 19c1: zinc stearate, 2.5 g a kg of UO2; without it, with the line defects rule, a tenth of the green pellets crack
				double need = pressed / Tuning.U_IN_UO2 * Tuning.ZINC_STEARATE_G_PER_KG_UO2 / 1000;
				if (lubricant < need && !slots.get(0).isEmpty()) {
					slots.get(0).shrink(1);
					lubricant += Tuning.ZINC_STEARATE_SACK_KG;
				}
				if (lubricant >= need) {
					lubricant -= need;
				} else if (dev.wildcatter.Rules.fuelLineDefects()) {
					double cracked = pressed * Tuning.CRACKED_WITHOUT_LUBRICANT;
					u -= cracked;
					scrap.add(cracked, assay);
					unit = "NO LUBRICANT: put zinc stearate in a hopper on its top; " + Math.round(Tuning.CRACKED_WITHOUT_LUBRICANT * 100) + " % of its green pellets crack to scrap; " + unit;
				}
				scrap.emit(slots, SCRAP_OUT);
			}
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
				if (dev.wildcatter.Rules.fuelLineDefects()) {   // 19c1: the swarf goes out in scrap cans for the oxidizer
					scrap.add(ground * Tuning.GRIND_SWARF, assay);
					scrap.emit(slots, SCRAP_OUT);
				} else {
					swarf += ground * Tuning.GRIND_SWARF;
				}
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
		double a = up.fuelAssay();
		double kg = up.takeFuelU(Math.min(Math.min(rate() * strength, room()), max));
		assay = FuelStage.blend(u, assay, kg, a);
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
			cylinderAssay = dev.wildcatter.WildcatterComponents.assay(useOne());
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
		assay = FuelStage.blend(u, assay, kg, cylinderAssay);
		u += kg;
		unit = String.format("vaporizing the 30B's UF6: %.0f kg/h; %,.0f kg left in it (%.1f %%)", kg * Tuning.UF6_PER_U / seconds() * 3600, uf6,
				uf6 / Tuning.CYLINDER_30B_KG * 100);
	}

	/** For the game test: kg of U cracked or ground to scrap here (19c1). */
	public double scrapTotal() {
		return scrap.total();
	}

	/** For the game test: the 30B in the autoclave with only {@code kg} left. */
	public void setUf6ForTest(double kg) {
		loaded = true;
		uf6 = kg;
		setChanged();
	}

	private void load(Level level) {
		FuelStage up = Touching.find(level, worldPosition, PelletInspectionBlockEntity.class, i -> true);   // 19c1: inspected pellets first
		if (up == null) {
			up = up(level, "pellet_grinder");
		}
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
		double a = up.fuelAssay();
		double kg = up.takeFuelU(Math.min(rate() * strength, Fuel.rodU() - progress));
		assay = FuelStage.blend(rods * Fuel.rodU() + progress, assay, kg, a);
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
			case "pellet_press" -> String.format("Pellet press: %s; zinc stearate %.2f kg in its feeder, %d sacks; scrap %.2f kg of U cracked, %d cans", unit, lubricant,
					slots.get(0).getCount(), scrap.total(), slots.get(SCRAP_OUT).getCount());
			case "sintering_furnace" -> "Sintering furnace (pusher): " + unit;
			case "pellet_grinder" -> String.format("Pellet grinder (centreless): %s; swarf %.1f kg of U%s", unit, swarf + scrap.total(),
					scrap.total() > 0 ? String.format(" (%d scrap cans)", slots.get(SCRAP_OUT).getCount()) : "");
			case "rod_loader" -> String.format("Rod loader: %s; tubes %d, steel %.1f kg", unit, slots.get(0).getCount(), steel);
			default -> kind();
		};
		return Component.literal(label + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("assay", assay);
		output.putDouble("cylinderAssay", cylinderAssay);
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
		output.putDouble("lubricant", lubricant);
		scrap.save(output, "scrap");
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		assay = input.getDoubleOr("assay", Tuning.ASSAY_PRODUCT);
		cylinderAssay = input.getDoubleOr("cylinderAssay", Tuning.ASSAY_PRODUCT);
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
		lubricant = input.getDoubleOr("lubricant", 0);
		scrap.load(input, "scrap");
	}
}
