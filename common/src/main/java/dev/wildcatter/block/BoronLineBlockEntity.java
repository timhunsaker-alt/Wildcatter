package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Boron;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A part of the boron plant (docs/multiblocks/boron.md):
 * - the borax dissolver (borax by hopper, sulfuric acid off its east pipe, pumped water), making boric acid liquor at 90 C;
 * - the boric acid crystallizer against it, bagging 25 kg of boric acid to a hopper under it;
 * - the B4C furnace (boric acid and coal by hopper), 1 kg trays of pellets out below;
 * - the RCCA bench (trays and steel by hopper), building an RCCA of 24 rodlets out below.
 * Slots: 0 and 1 in from the hopper on top, 2 and 3 out to the hopper under it.
 */
public class BoronLineBlockEntity extends UnitMotorBlockEntity implements HopperFed, WaterUser {
	private static final double WATER_HOLD = 200.0;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(4, ItemStack.EMPTY);
	private double borax;      // kg in the dissolver
	private double liquor;     // kg of boric acid dissolved, for the crystallizer
	private double water;      // L
	private double acid;       // kg of boric acid in the furnace
	private double carbon;     // kg in the furnace
	private double steel;      // kg on the bench
	private double progress;   // kg of the bag or tray, or seconds of the RCCA
	private boolean building;  // the bench has an RCCA on it
	private double sulfate, co;
	private String unit = "starting";

	public BoronLineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BORON_LINE_ENTITY, pos, state);
	}

	// ---- hoppers and water ------------------------------------------------------------------------------------------------------
	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private Item boraxItem() {
		return OreBlocks.MINERALS.get(Ore.BORAX);
	}

	private boolean takes(int slot, ItemStack stack) {
		return switch (kind()) {
			case "borax_dissolver" -> slot == 0 && stack.is(boraxItem());
			case "b4c_furnace" -> slot == 0 && stack.is(WildcatterItems.BORIC_ACID) || slot == 1 && (stack.is(Items.COAL) || stack.is(Items.CHARCOAL));
			case "rcca_bench" -> slot == 0 && stack.is(WildcatterItems.B4C_PELLETS) || slot == 1 && stack.is(WildcatterItems.STEEL_INGOT);
			default -> false;
		};
	}

	@Override
	public boolean takes(ItemStack stack) {
		return takes(0, stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1} : side == Direction.DOWN ? new int[] {2, 3} : new int[0];
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
		return slot >= 2 && side == Direction.DOWN;
	}

	@Override
	public double waterLevel() {
		return kind().equals("borax_dissolver") ? water / WATER_HOLD : 1;
	}

	@Override
	public int addWater(int litres) {
		if (!kind().equals("borax_dissolver")) {
			return 0;
		}
		int fits = (int) Math.max(0, Math.min(litres, WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	private List<Lines.Face> sulfuric() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	/** Puts {@code item} in the first free out slot; false when both are full. */
	private boolean out(Item item) {
		for (int i = 2; i <= 3; i++) {
			ItemStack s = slots.get(i);
			if (s.isEmpty()) {
				slots.set(i, new ItemStack(item));
				return true;
			}
			if (s.is(item) && s.getCount() < s.getMaxStackSize()) {
				s.grow(1);
				return true;
			}
		}
		return false;
	}

	public double liquor() {
		return liquor;
	}

	public double takeLiquor(double kg) {
		double got = Math.max(0, Math.min(kg, liquor));
		liquor -= got;
		setChanged();
		return got;
	}

	// ---- its work -------------------------------------------------------------------------------------------------------------
	@Override
	protected void work(Level level) {
		switch (kind()) {
			case "borax_dissolver" -> dissolve(level);
			case "boric_acid_crystallizer" -> crystallize(level);
			case "b4c_furnace" -> furnace(level);
			case "rcca_bench" -> bench(level);
			default -> {
			}
		}
	}

	private void dissolve(Level level) {
		double rate = Tuning.BORIC_ACID_KGS * seconds();
		if (borax < rate * Boron.boraxPerAcid() && slots.get(0).getCount() > 0) {
			useOne();
			borax += Tuning.BORAX_KG;
		}
		if (borax < 1e-6) {
			unit = "NO BORAX: put borax in its hopper";
			return;
		}
		double h2so4 = Lines.available(level, sulfuric(), Product.SULFURIC_ACID, Pull.MACHINE, this);
		if (h2so4 <= 1e-9) {
			unit = "NO ACID: pipe sulfuric acid to the east face of its bottom cells";
			return;
		}
		if (water <= 1e-6) {
			unit = "NO WATER: pump water to it";
			return;
		}
		if (liquor >= Tuning.DISSOLVER_HOLD_KG - 1e-9) {
			unit = "FULL: the crystallizer is not taking its liquor";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the agitator and heaters: " + describe();
			return;
		}
		double kg = Math.min(Math.min(rate * strength, Tuning.DISSOLVER_HOLD_KG - liquor), Math.min(borax / Boron.boraxPerAcid(),
				Math.min(h2so4 / Boron.sulfuricLitresPerAcid(), water / Tuning.DISSOLVER_WATER_PER_KG)));
		Lines.pull(level, sulfuric(), Product.SULFURIC_ACID, kg * Boron.sulfuricLitresPerAcid(), Pull.MACHINE, this);
		borax -= kg * Boron.boraxPerAcid();
		water = Math.max(0, water - kg * Tuning.DISSOLVER_WATER_PER_KG);
		liquor += kg;
		sulfate += kg * Boron.sulfatePerAcid();
		unit = String.format("dissolving borax in sulfuric acid at 90 C: %.0f kg/h of boric acid; liquor %.1f kg", kg / seconds() * 3600, liquor);
	}

	private void crystallize(Level level) {
		BoronLineBlockEntity up = Touching.find(level, worldPosition, BoronLineBlockEntity.class, b -> b.kind().equals("borax_dissolver"));
		if (up == null) {
			unit = "NOT BUILT: set it against the BORAX DISSOLVER";
			return;
		}
		if (progress >= Tuning.BORIC_ACID_BAG_KG - 1e-9) {
			if (!out(WildcatterItems.BORIC_ACID)) {
				unit = "FULL: take the bags of boric acid (a hopper under it)";
				return;
			}
			progress = 0;
		}
		if (up.liquor() < 1e-9) {
			unit = "waiting for liquor from the dissolver";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the centrifuge: " + describe();
			return;
		}
		double kg = up.takeLiquor(Math.min(Tuning.BORIC_ACID_KGS * 1.2 * seconds() * strength, Tuning.BORIC_ACID_BAG_KG - progress));
		progress += kg;
		unit = String.format("crystallizing, centrifuging and drying: %.0f kg/h; bag %.1f of %.0f kg", kg / seconds() * 3600, progress, Tuning.BORIC_ACID_BAG_KG);
	}

	private void furnace(Level level) {
		double rate = Tuning.B4C_KGS * seconds();
		if (acid < rate * Boron.acidPerB4c() && slots.get(0).getCount() > 0) {
			slots.get(0).shrink(1);
			acid += Tuning.BORIC_ACID_BAG_KG;
		}
		if (carbon < rate * Boron.carbonPerB4c() && slots.get(1).getCount() > 0) {
			slots.get(1).shrink(1);
			carbon += Tuning.COAL_KG * Tuning.COAL_CARBON;
		}
		if (progress >= 1 - 1e-9) {
			if (!out(WildcatterItems.B4C_PELLETS)) {
				unit = "FULL: take the trays of B4C pellets (a hopper under it)";
				return;
			}
			progress = 0;
		}
		if (acid < 1e-6) {
			unit = "NO BORIC ACID: put bags of boric acid in its hopper";
			return;
		}
		if (carbon < 1e-6) {
			unit = "NO COAL: put coal in its hopper";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the furnace transformer: " + describe();
			return;
		}
		double kg = Math.min(Math.min(rate * strength, 1 - progress), Math.min(acid / Boron.acidPerB4c(), carbon / Boron.carbonPerB4c()));
		acid -= kg * Boron.acidPerB4c();
		carbon -= kg * Boron.carbonPerB4c();
		co += kg * Boron.coPerB4c();
		progress += kg;
		unit = String.format("reducing B2O3 with carbon at about 2,000 C: %.1f kg/h of B4C; tray %.0f %%", kg / seconds() * 3600, progress * 100);
	}

	private void bench(Level level) {
		if (steel < Tuning.RCCA_STEEL_KG && slots.get(1).getCount() > 0) {
			slots.get(1).shrink(1);
			steel += Tuning.STEEL_INGOT_KG;
		}
		if (building && progress >= Tuning.RCCA_SECONDS - 1e-9) {
			if (!out(WildcatterItems.RCCA)) {
				unit = "FULL: take the RCCA (a hopper under it)";
				return;
			}
			building = false;
			progress = 0;
		}
		if (!building) {
			int trays = (int) Math.round(Tuning.B4C_ROD_KG);
			if (slots.get(0).getCount() < trays) {
				unit = String.format("NO PELLETS: it needs %d trays of B4C pellets in its hopper (%d there)", trays, slots.get(0).getCount());
				return;
			}
			if (steel < Tuning.RCCA_STEEL_KG) {
				unit = "NO STEEL: put steel ingots in its hopper (the rodlet cladding, plugs and spider)";
				return;
			}
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		if (!building) {
			slots.get(0).shrink((int) Math.round(Tuning.B4C_ROD_KG));
			steel -= Tuning.RCCA_STEEL_KG;
			building = true;
		}
		progress += seconds() * strength;
		unit = String.format("loading and welding 24 B4C rodlets onto the spider: %.0f %%", progress / Tuning.RCCA_SECONDS * 100);
	}

	/** For the game test: the bag, tray or RCCA on the part brought nearly to its end. */
	public void setNearlyDoneForTest() {
		switch (kind()) {
			case "boric_acid_crystallizer" -> progress = Tuning.BORIC_ACID_BAG_KG - 0.01;
			case "b4c_furnace" -> progress = 0.999;
			case "rcca_bench" -> {
				if (building) {
					progress = Tuning.RCCA_SECONDS - 1;
				}
			}
			default -> {
			}
		}
		setChanged();
	}

	@Override
	public Component inspect() {
		String label = switch (kind()) {
			case "borax_dissolver" -> String.format("Borax dissolver: %s; borax %.0f kg, water %.0f/%.0f L; Na2SO4 %.0f kg", unit, borax, water, WATER_HOLD, sulfate);
			case "boric_acid_crystallizer" -> "Boric acid crystallizer: " + unit;
			case "b4c_furnace" -> String.format("B4C furnace (Acheson): %s; boric acid %.0f kg, carbon %.0f kg; CO %.0f kg", unit, acid, carbon, co);
			case "rcca_bench" -> String.format("RCCA bench: %s; trays %d, steel %.0f kg", unit, slots.get(0).getCount(), steel);
			default -> kind();
		};
		return Component.literal(label + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("borax", borax);
		output.putDouble("liquor", liquor);
		output.putDouble("water", water);
		output.putDouble("acid", acid);
		output.putDouble("carbon", carbon);
		output.putDouble("steel", steel);
		output.putDouble("progress", progress);
		output.putBoolean("building", building);
		output.putDouble("sulfate", sulfate);
		output.putDouble("co", co);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		borax = input.getDoubleOr("borax", 0);
		liquor = input.getDoubleOr("liquor", 0);
		water = input.getDoubleOr("water", 0);
		acid = input.getDoubleOr("acid", 0);
		carbon = input.getDoubleOr("carbon", 0);
		steel = input.getDoubleOr("steel", 0);
		progress = input.getDoubleOr("progress", 0);
		building = input.getBooleanOr("building", false);
		sulfate = input.getDoubleOr("sulfate", 0);
		co = input.getDoubleOr("co", 0);
	}
}
