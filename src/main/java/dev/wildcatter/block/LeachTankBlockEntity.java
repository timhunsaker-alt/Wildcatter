package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Mill;
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
import org.jspecify.annotations.Nullable;

/**
 * The leach tank (56 kW agitator, 480 V) (docs/multiblocks/uranium-mill.md): ore slurry in on the north face of its front cells,
 * sulfuric acid on the east face of its bottom cells, and pyrolusite by a hopper on top, which turns the uranium(IV) to uranium(VI)
 * so it dissolves. Leach slurry out of the south face of its back cells: 93 % of the uranium in its liquor, the rest left in the
 * solids for the tailings.
 */
public class LeachTankBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double mno2;
	private String unit = "starting";

	public LeachTankBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LEACH_TANK_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.PYROLUSITE));
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> acid() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 7 && at[1] == 0 && side == Direction.SOUTH ? Product.LEACH_SLURRY : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.MILL_ORE_KGS / Tuning.MILL_SLURRY_SOLIDS * 1.1 * Tuning.PLANT_HOLD_SECONDS;
	}

	@Override
	protected void work(Level level) {
		double waiting = Lines.available(level, inlet(), Product.ORE_SLURRY, Pull.MACHINE, this);
		if (waiting < 0.001) {
			unit = "idle: no ore slurry at its front (north face)";
			return;
		}
		double acidPerSlurry = Mill.acidPerOre() * Tuning.MILL_SLURRY_SOLIDS / Tuning.ACID_KG_PER_L;   // litres a kg of slurry
		double acidWaiting = Lines.available(level, acid(), Product.SULFURIC_ACID, Pull.MACHINE, this);
		if (acidWaiting < 0.001) {
			unit = "NO ACID: pipe sulfuric acid to its east side";
			return;
		}
		double oxidantPerSlurry = Mill.mno2PerOre() * Tuning.MILL_SLURRY_SOLIDS;
		if (mno2 < oxidantPerSlurry * waiting && fed() > 0) {
			useOne();
			mno2 += Ore.PYROLUSITE.kgPerBlock();
		}
		if (mno2 <= 0.0001) {
			unit = "NO OXIDANT: put a hopper of pyrolusite on the tank (uranium(IV) will not dissolve without it)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double max = Math.min(Tuning.MILL_ORE_KGS / Tuning.MILL_SLURRY_SOLIDS * 1.1 * seconds() * strength, Math.min(waiting, acidWaiting / acidPerSlurry));
		max = Math.min(Math.min(max, mno2 / oxidantPerSlurry), room(Product.LEACH_SLURRY) / (1 + acidPerSlurry * Tuning.ACID_KG_PER_L));
		double[] in = new double[5];
		double got = max > 0.001 ? pullMixed(level, inlet(), Product.ORE_SLURRY, max, in) : 0;
		if (got <= 0) {
			unit = "idle: its leach slurry has nowhere to go (pipe its back end to a CCD thickener)";
			return;
		}
		double solids = got * in[1];
		double acidL = Lines.pull(level, acid(), Product.SULFURIC_ACID, solids * Mill.acidPerOre() / Tuning.ACID_KG_PER_L, Pull.MACHINE, this);
		mno2 = Math.max(0, mno2 - solids * Mill.mno2PerOre());
		double out = got + acidL * Tuning.ACID_KG_PER_L;
		double dissolved = got * in[0] * Tuning.MILL_RECOVERY;
		addMixed(Product.LEACH_SLURRY, out, new double[] {dissolved / out, solids / out, 0, 0, 0});
		unit = String.format("leaching %.1f kg/s of ore with %.2f L/s of acid: %.3f kg/s of U3O8 dissolved (%.0f %%)", solids / seconds(), acidL / seconds(),
				dissolved / seconds(), Tuning.MILL_RECOVERY * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal("Leach tank: " + unit + String.format("; pyrolusite %,.0f kg and %d blocks; ", mno2, fed()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("mno2", mno2);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		mno2 = input.getDoubleOr("mno2", 0);
	}
}
