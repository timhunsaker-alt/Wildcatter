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
 * the bagging station it touches.
 */
public class LimeKilnBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double caco3;
	private double quicklime;
	private double co2;   // t sent out
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
		return stack.is(OreBlocks.MINERALS.get(Ore.LIMESTONE));
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.QUICKLIME_BAG;
	}

	@Override
	public double bagReady() {
		return quicklime;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, quicklime);
		quicklime -= got;
		setChanged();
		return got;
	}

	/** For the game test: quicklime as if burnt. */
	public void addQuicklime(double kg) {
		quicklime += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		double max = Tuning.LIME_KILN_KGS * seconds();
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
		unit = String.format("burning at 1,000 C: %.0f kg/h of quicklime, %.0f kW of gas", kg / seconds() * 3600, got / seconds() / 1000);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Lime kiln (shaft): %s; CaCO3 %,.0f kg and %d blocks; %.0f kg of quicklime held; %.1f t of CO2 out; ", unit, caco3, fed(),
				quicklime, co2) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("caco3", caco3);
		output.putDouble("quicklime", quicklime);
		output.putDouble("co2", co2);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		caco3 = input.getDoubleOr("caco3", 0);
		quicklime = input.getDoubleOr("quicklime", 0);
		co2 = input.getDoubleOr("co2", 0);
	}
}
