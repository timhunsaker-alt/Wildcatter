package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
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

/**
 * The HF kiln (docs/multiblocks/conversion.md): a pre-reactor and indirectly fired rotary kiln, its drive 15 kW at 480 V. Fluorspar
 * by a hopper on top and sulfuric acid on the east face of its bottom cells react to HF gas and anhydrite: CaF2 + H2SO4 -> CaSO4 +
 * 2 HF, fired on gas or LPG (5 MJ a kg of HF) on the same side. The HF column against it takes the gas.
 */
public class HfKilnBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double fluorspar;   // kg of CaF2 tipped in
	private double hf;          // kg of crude HF gas for the column
	private double anhydrite;   // t sent out
	private String unit = "starting";

	public HfKilnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HF_KILN_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.FLUORITE));
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	public double takeHf(double kg) {
		double got = Math.min(kg, hf);
		hf -= got;
		setChanged();
		return got;
	}

	@Override
	protected void work(Level level) {
		double max = Math.min(Tuning.HF_KILN_KGS * seconds(), Tuning.HF_KILN_KGS * Tuning.PLANT_HOLD_SECONDS - hf);
		if (fluorspar < max * Tuning.HF_FLUORSPAR_PER_HF && fed() > 0) {
			useOne();
			fluorspar += Ore.FLUORITE.kgPerBlock();
		}
		double acidPerHf = Tuning.HF_ACID_PER_HF / Tuning.ACID_STRENGTH / Tuning.ACID_KG_PER_L;   // litres of 98.5 % acid a kg of HF
		double acid = Lines.available(level, east(), Product.SULFURIC_ACID, Pull.MACHINE, this);
		double kg = Math.min(Math.min(max, fluorspar / Tuning.HF_FLUORSPAR_PER_HF), acid / acidPerHf);
		if (fluorspar < 0.001) {
			unit = "idle: no fluorspar (put a hopper of fluorspar on it)";
			return;
		}
		if (acid < 0.001) {
			unit = "NO ACID: pipe sulfuric acid to its east side";
			return;
		}
		if (kg <= 1e-7) {
			unit = "idle: the HF column is full";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		kg *= strength;
		double need = kg * Tuning.HF_FUEL_J_PER_KG;
		double got = GasFiring.fire(level, east(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "NO FUEL: pipe gas or LPG to its east side";
			return;
		}
		Lines.pull(level, east(), Product.SULFURIC_ACID, kg * acidPerHf, Pull.MACHINE, this);
		fluorspar -= kg * Tuning.HF_FLUORSPAR_PER_HF;
		hf += kg;
		anhydrite += kg * Tuning.HF_ANHYDRITE_PER_HF / 1000;
		unit = String.format("roasting: %.3f kg/s of HF from %.3f kg/s of fluorspar, %.0f kW fired", kg / seconds(), kg * Tuning.HF_FLUORSPAR_PER_HF / seconds(), got / seconds() / 1000);
	}

	@Override
	public Component inspect() {
		return Component.literal("HF kiln: " + unit + String.format("; fluorspar %,.0f kg and %d blocks; %.1f t of anhydrite out; ", fluorspar, fed(), anhydrite) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("fluorspar", fluorspar);
		output.putDouble("hf", hf);
		output.putDouble("anhydrite", anhydrite);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		fluorspar = input.getDoubleOr("fluorspar", 0);
		hf = input.getDoubleOr("hf", 0);
		anhydrite = input.getDoubleOr("anhydrite", 0);
	}
}
