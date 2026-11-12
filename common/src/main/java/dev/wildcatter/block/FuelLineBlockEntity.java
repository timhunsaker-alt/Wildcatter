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
	private int ifbaSetting;   // 19c2: the loader's IFBA rods an assembly: 0, 32, 64 or 104
	private int rodIndex;      // the rod's place in its assembly's 264
	private boolean benchIfba; // the rod on the bench is coated
	private int ifbaReady;     // coated rods among those ready
	private boolean lastIfba;
	private double hfPending;  // 19c2: the kiln's HF for a recovery condenser against it, kg
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

	/** 19c2: whether the rod the last {@link #takeRod} gave is IFBA-coated. */
	public boolean lastTakenIfba() {
		return lastIfba;
	}

	public int ifbaSetting() {
		return ifbaSetting;
	}

	/** Sneak and right-click on the rod loader: the next IFBA setting (0, 32, 64, 104 rods an assembly). */
	public String nextIfba() {
		int[] s = Tuning.IFBA_SETTINGS;
		int i = 0;
		while (i < s.length && s[i] != ifbaSetting) {
			i++;
		}
		ifbaSetting = s[(i + 1) % s.length];
		setChanged();
		return ifbaSetting == 0 ? "Rod loader: no IFBA rods" : "Rod loader: " + ifbaSetting + " IFBA rods an assembly (the IFBA coater against it coats their pellets)";
	}

	/** 19c2: the HF recovery condenser takes up to {@code kg} of the kiln's HF; returns what it gave. */
	public double takeKilnHf(double kg) {
		double got = Math.max(0, Math.min(kg, hfPending));
		hfPending -= got;
		setChanged();
		return got;
	}

	/** The rod loader's finished rods, for the welder. */
	public int rods() {
		return rods;
	}

	public boolean takeRod() {
		if (rods <= 0) {
			return false;
		}
		lastIfba = ifbaReady > 0;
		if (lastIfba) {
			ifbaReady--;
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
			case "uf6_autoclave" -> slot == 0 && stack.is(WildcatterItems.LEU_CYLINDER_30B)
					&& (!dev.wildcatter.Rules.uf6Cylinders() || dev.wildcatter.item.CylinderTags.sampled(stack));   // 16c2: its certificate first
			case "rod_loader" -> slot == 0 && stack.is(WildcatterItems.ZIRCALOY_TUBE) || slot == 1 && stack.is(WildcatterItems.STEEL_INGOT)
					|| slot == 2 && stack.is(WildcatterItems.NICKEL_CHROMIUM_INGOT);
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
			case "rod_loader" -> side == Direction.UP ? new int[] {0, 1, 2} : new int[0];
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
				// 19c2: with a recovery condenser against it, its HF waits there (five minutes' worth) instead of going to the scrubber
				if (Touching.find(level, worldPosition, HfRecoveryCondenserBlockEntity.class, c -> true) != null) {
					double cap = Tuning.FUEL_LINE_U_KGS * Fuel.hfPerU() * 300;
					double to = Math.min(got * Fuel.hfPerU(), Math.max(0, cap - hfPending));
					hfPending += to;
					hf += got * Fuel.hfPerU() - to;
				} else {
					hf += got * Fuel.hfPerU();
				}
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
		double heel = dev.wildcatter.Rules.uf6Cylinders() ? Tuning.HEEL_30B_KG : 0;   // 16c2: the heel stays in the emptied 30B
		if (loaded && uf6 <= heel + 1e-6) {
			int out = slots.get(1).isEmpty() ? 1 : slots.get(2).isEmpty() ? 2 : -1;
			if (out < 0) {
				unit = "FULL: take the empty 30Bs (a hopper under it)";
				return;
			}
			slots.set(out, dev.wildcatter.item.CylinderTags.withHeel(new ItemStack(WildcatterItems.EMPTY_30B_CYLINDER), Math.min(heel, uf6)));
			uf6 = 0;
			loaded = false;
		}
		if (!loaded && fed() > 0) {
			cylinderAssay = dev.wildcatter.WildcatterComponents.assay(useOne());
			loaded = true;
			uf6 = Tuning.CYLINDER_30B_KG;
		}
		if (!loaded) {
			unit = dev.wildcatter.Rules.uf6Cylinders() ? "NO CYLINDER: put a hopper of SAMPLED 30Bs of LEU on top (the sampling autoclave signs them off)"
					: "NO CYLINDER: put a hopper of 30Bs of LEU on top";
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
		double kg = Math.min(Math.min(rate() * strength, room()), Math.max(0, uf6 - heel) / Tuning.UF6_PER_U);
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
		boolean nickel = dev.wildcatter.Rules.fuelNickelHardware();   // 19c2: its plugs and spring of nickel-chromium alloy
		int plugSlot = nickel ? 2 : 1;
		if (steel < Tuning.ROD_STEEL_KG && !slots.get(plugSlot).isEmpty()) {
			slots.get(plugSlot).shrink(1);
			steel += nickel ? Tuning.ALLOY_INGOT_KG : Tuning.STEEL_INGOT_KG;
		}
		if (progress <= 1e-9 && slots.get(0).isEmpty()) {
			unit = "NO TUBES: put Zircaloy cladding tubes in its hopper";
			return;
		}
		if (steel < Tuning.ROD_STEEL_KG && progress <= 1e-9) {
			unit = nickel ? "NO NICKEL ALLOY: put nickel-chromium alloy ingots in its hopper (the plugs and plenum spring)"
					: "NO STEEL: put steel ingots in its hopper (the plugs and plenum spring)";
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
			benchIfba = ifbaSetting > 0 && rodIndex < ifbaSetting;
			if (benchIfba) {   // 19c2: its pellets coated with ZrB2 first
				IfbaCoaterBlockEntity coater = Touching.find(level, worldPosition, IfbaCoaterBlockEntity.class, c -> true);
				String why = coater == null ? "NO IFBA: set an IFBA coater against it, or sneak-click it to 0 IFBA rods" : coater.coat(level);
				if (why != null) {
					unit = why;
					return;
				}
			}
			rodIndex = (rodIndex + 1) % Tuning.ASSEMBLY_RODS;
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
			if (benchIfba) {
				ifbaReady++;
			}
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
			case "rod_loader" -> String.format("Rod loader: %s; tubes %d, %s %.1f kg%s", unit, slots.get(0).getCount(),
					dev.wildcatter.Rules.fuelNickelHardware() ? "nickel alloy" : "steel", steel,
					ifbaSetting > 0 ? String.format("; %d IFBA rods an assembly (%d coated rods ready); sneak-click to change", ifbaSetting, ifbaReady) : "; sneak-click sets IFBA rods");
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
		output.putInt("ifbaSetting", ifbaSetting);
		output.putInt("rodIndex", rodIndex);
		output.putBoolean("benchIfba", benchIfba);
		output.putInt("ifbaReady", ifbaReady);
		output.putDouble("hfPending", hfPending);
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
		ifbaSetting = input.getIntOr("ifbaSetting", 0);
		rodIndex = input.getIntOr("rodIndex", 0);
		benchIfba = input.getBooleanOr("benchIfba", false);
		ifbaReady = input.getIntOr("ifbaReady", 0);
		hfPending = input.getDoubleOr("hfPending", 0);
		scrap.load(input, "scrap");
	}
}
