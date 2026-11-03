package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Bayer;
import dev.wildcatter.fluid.Product;
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
 * The AlF3 reactor (docs/multiblocks/alumina-plant.md): a dry fluid bed at about 550 C, Al(OH)3 + 3 HF -> AlF3 + 3 H2O, 50 kg of AlF3
 * an hour; fan 55 kW at 480 V. Hydrate bags by a hopper on top; anhydrous HF and gas or LPG at the east face of its bottom cells, each
 * on its own run; the AlF3 to the bagging station it touches.
 */
public class Alf3ReactorBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double hydrate;
	private double alf3;
	private String unit = "starting";

	public Alf3ReactorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ALF3_REACTOR_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.HYDRATE_BAG);
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.ALF3_BAG;
	}

	@Override
	public double bagReady() {
		return alf3;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, alf3);
		alf3 -= got;
		setChanged();
		return got;
	}

	/** For the game test: AlF3 as if made. */
	public void addAlf3(double kg) {
		alf3 += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		double max = Tuning.ALF3_KGS * seconds();
		if (hydrate < max * Tuning.HYDRATE_PER_ALF3 && fed() > 0) {
			useOne();
			hydrate += Tuning.BULK_BAG_KG;
		}
		if (hydrate < 0.001) {
			unit = "NO HYDRATE: put hydrate bags in its hopper";
			return;
		}
		double hfLitres = Lines.available(level, east(), Product.ANHYDROUS_HF, Pull.MACHINE, this);
		if (hfLitres < 1e-4) {
			unit = "NO HF: pipe anhydrous HF from an HF column to its east side, on its own run";
			return;
		}
		double hfPerAlf3 = Tuning.HF_PER_ALF3 / Tuning.AHF_KG_PER_L;   // litres a kg
		max = Math.min(Math.min(max, hydrate / Tuning.HYDRATE_PER_ALF3), Math.min(hfLitres / hfPerAlf3, 2 * Tuning.BULK_BAG_KG - alf3));
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
		double need = kg * Tuning.ALF3_J_PER_KG;
		double got = GasFiring.fire(level, east(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "NO FUEL: pipe gas or LPG to its east side";
			return;
		}
		Lines.pull(level, east(), Product.ANHYDROUS_HF, kg * hfPerAlf3, Pull.MACHINE, this);
		hydrate -= kg * Tuning.HYDRATE_PER_ALF3;
		alf3 += kg;
		unit = String.format("fluorinating at 550 C: %.1f kg/h of AlF3 on %.1f kg/h of HF, %.0f kg/h of water vapour", kg / seconds() * 3600,
				kg * Tuning.HF_PER_ALF3 / seconds() * 3600, kg * Bayer.waterPerAlf3() / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("AlF3 reactor (fluid bed): %s; hydrate %,.0f kg and %d bags; %.0f kg of AlF3 held; ", unit, hydrate, fed(), alf3) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("hydrate", hydrate);
		output.putDouble("alf3", alf3);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		hydrate = input.getDoubleOr("hydrate", 0);
		alf3 = input.getDoubleOr("alf3", 0);
	}
}
