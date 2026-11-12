package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Bayer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The lime kiln (docs/multiblocks/alumina-plant.md): a small vertical shaft kiln, CaCO3 -> CaO + CO2, 10 t of quicklime a day on
 * 4.0 GJ a t, gas or LPG at the east face of its bottom cells; blower 45 kW at 480 V. Limestone by a hopper on top; the quicklime to
 * the bagging station it touches. 17c2 (zirconium-extensions.md item 4): dolomite in its hopper burns to dolime (CaO.MgO) for the Pidgeon
 * furnace, in its own bags; dolomite first while it has both.
 */
public class LimeKilnBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double caco3;
	private double quicklime;
	private double dolomite, dolime;   // 17c2: kg of CaMg(CO3)2 on hand, kg of dolime for bags
	private double co2;   // t sent out
	private double kilnGas;   // 16d1: kg of CO2 a liquefier against it may take, a few seconds' worth; the rest goes up the stack
	private String unit = "starting";

	public LimeKilnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LIME_KILN_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.LIMESTONE)) || stack.is(OreBlocks.MINERALS.get(Ore.DOLOMITE));
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	public Item bagItem() {
		return dolime > quicklime ? WildcatterItems.DOLIME_BAG : WildcatterItems.QUICKLIME_BAG;
	}

	@Override
	public double bagReady() {
		return Math.max(quicklime, dolime);
	}

	@Override
	public double takeForBag(double kg) {
		if (dolime > quicklime) {
			double got = Math.min(kg, dolime);
			dolime -= got;
			setChanged();
			return got;
		}
		double got = Math.min(kg, quicklime);
		quicklime -= got;
		setChanged();
		return got;
	}

	/** For the game test: dolime as if burnt. */
	public void addDolimeForTest(double kg) {
		dolime += kg;
		setChanged();
	}

	public double dolime() {
		return dolime;
	}

	/** For the game test: quicklime as if burnt. */
	public void addQuicklime(double kg) {
		quicklime += kg;
		setChanged();
	}

	/** 16d1: a CO2 liquefier takes up to {@code kg} of the kiln's CO2; returns what it got. */
	public double takeKilnGas(double kg) {
		double got = Math.min(kg, kilnGas);
		kilnGas -= got;
		return got;
	}

	/** For the game test: kiln gas as if it were burning. */
	public void addKilnGasForTest(double kg) {
		kilnGas += kg;
	}

	@Override
	protected void work(Level level) {
		double max = Tuning.LIME_KILN_KGS * seconds();
		if (dolomite >= 0.01 || slots.get(0).is(OreBlocks.MINERALS.get(Ore.DOLOMITE))) {
			burnDolomite(level, max);
			return;
		}
		if (caco3 < max / Tuning.CAO_PER_CACO3 && fed() > 0) {
			useOne();
			caco3 += Ore.LIMESTONE.kgPerBlock();
		}
		if (caco3 < 0.01) {
			unit = "NO LIMESTONE: put a hopper of limestone on it";
			return;
		}
		max = Math.min(Math.min(max, caco3 * Tuning.CAO_PER_CACO3), 2 * Tuning.BULK_BAG_KG - quicklime);
		if (max <= 1e-7) {
			unit = "FULL: set a bagging station against it";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = max * strength;
		double need = kg * Tuning.LIME_KILN_J_PER_KG;
		double got = GasFiring.fire(level, east(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "NO FUEL: pipe gas or LPG to its east side";
			return;
		}
		caco3 -= kg / Tuning.CAO_PER_CACO3;
		quicklime += kg;
		co2 += kg * Bayer.co2PerLime() / 1000;
		kilnGas = Math.min(Tuning.LIME_KILN_KGS * Bayer.co2PerLime() * Tuning.PLANT_HOLD_SECONDS, kilnGas + kg * Bayer.co2PerLime());
		unit = String.format("burning at 1,000 C: %.0f kg/h of quicklime, %.0f kW of gas", kg / seconds() * 3600, got / seconds() / 1000);
	}

	/** 17c2: dolomite to dolime at the kiln's rate of product, CaMg(CO3)2 -> CaO.MgO + 2 CO2. */
	private void burnDolomite(Level level, double max) {
		if (dolomite < max / Tuning.DOLIME_PER_DOLOMITE && slots.get(0).is(OreBlocks.MINERALS.get(Ore.DOLOMITE))) {
			useOne();
			dolomite += Ore.DOLOMITE.kgPerBlock();
		}
		max = Math.min(Math.min(max, dolomite * Tuning.DOLIME_PER_DOLOMITE), 2 * Tuning.BULK_BAG_KG - dolime);
		if (max <= 1e-7) {
			unit = dolomite < 0.01 ? "NO DOLOMITE" : "FULL: set a bagging station against it";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = max * strength;
		double need = kg * Tuning.LIME_KILN_J_PER_KG;
		double got = GasFiring.fire(level, east(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "NO FUEL: pipe gas or LPG to its east side";
			return;
		}
		double co2PerDolime = 2 * 44.01 / 96.38;
		dolomite -= kg / Tuning.DOLIME_PER_DOLOMITE;
		dolime += kg;
		co2 += kg * co2PerDolime / 1000;
		kilnGas = Math.min(Tuning.LIME_KILN_KGS * Bayer.co2PerLime() * Tuning.PLANT_HOLD_SECONDS, kilnGas + kg * co2PerDolime);
		unit = String.format("burning dolomite at 1,000 C: %.0f kg/h of dolime, %.0f kW of gas", kg / seconds() * 3600, got / seconds() / 1000);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Lime kiln (shaft): %s; CaCO3 %,.0f kg and %d blocks; %.0f kg of quicklime held%s; %.1f t of CO2 out; ", unit, caco3, fed(),
				quicklime, dolime > 0 || dolomite > 0 ? String.format(", dolomite %,.0f kg, %.0f kg of dolime held", dolomite, dolime) : "", co2) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("caco3", caco3);
		output.putDouble("quicklime", quicklime);
		output.putDouble("co2", co2);
		output.putDouble("dolomite", dolomite);
		output.putDouble("dolime", dolime);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		caco3 = input.getDoubleOr("caco3", 0);
		quicklime = input.getDoubleOr("quicklime", 0);
		co2 = input.getDoubleOr("co2", 0);
		dolomite = input.getDoubleOr("dolomite", 0);
		dolime = input.getDoubleOr("dolime", 0);
	}
}
