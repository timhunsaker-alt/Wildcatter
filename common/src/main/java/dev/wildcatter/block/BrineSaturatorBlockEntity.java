package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.ChlorAlkaliPlant;
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
 * The brine saturator (docs/multiblocks/chlor-alkali-plant.md): a salt dissolver making saturated brine (26 % NaCl) from rock salt by a
 * hopper on top and the cell room's depleted brine, made up with water off a pump; or it takes brine from a brine wellhead on the north
 * face of its front cells. 22 kW on the world's industrial voltage, its box on the west face of its main cell. The brine treatment it
 * stands against draws on it.
 */
public class BrineSaturatorBlockEntity extends UnitMotorBlockEntity implements HopperFed, WaterUser {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double salt;    // kg of NaCl dissolving
	private double water;   // litres of make-up water, and the water well brine brought
	private double fromWell; // kg of brine taken from a well
	String unit = "idle: no brine treatment drawing on it";

	public BrineSaturatorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BRINE_SATURATOR_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.HALITE));
	}

	private List<Lines.Face> brineIn() {
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

	@Override
	public double waterLevel() {
		return water / Tuning.BRINE_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.BRINE_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	/** kg of brine taken from a brine well since placed. */
	public double fromWell() {
		return fromWell;
	}

	/** Each interval it tops up its salt from its hopper and a brine well, whether or not a treatment is drawing on it. */
	@Override
	protected void work(Level level) {
		refill(level);
	}

	private void refill(Level level) {
		if (salt < Tuning.BRINE_SALT_HOLD_KG - Ore.HALITE.kgPerBlock() && fed() > 0) {
			useOne();
			salt += Ore.HALITE.kgPerBlock();
		}
		double room = Math.min((Tuning.BRINE_SALT_HOLD_KG - salt) / Tuning.BRINE_NACL, (Tuning.BRINE_WATER_HOLD - water) / (1 - Tuning.BRINE_NACL));
		if (room > 1e-6) {
			double got = Lines.pull(level, brineIn(), Product.BRINE, room, Pull.MACHINE, this);
			salt += got * Tuning.BRINE_NACL;
			water += got * (1 - Tuning.BRINE_NACL);
			fromWell += got;
		}
	}

	double salt() {
		return salt;
	}

	double water() {
		return water;
	}

	/** Draws salt and water as brine for the treatment. */
	void draw(double kgSalt, double litres) {
		salt = Math.max(0, salt - kgSalt);
		water = Math.max(0, water - litres);
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Brine saturator (%.0f kW, %s): ", Tuning.BRINE_SATURATOR_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; salt %,.0f kg and %d blocks; water %,.0f/%,.0f L; %,.0f kg of well brine taken (%.0f %% NaCl); ", salt, fed(), water,
						Tuning.BRINE_WATER_HOLD, fromWell, Tuning.BRINE_NACL * 100) + describe());
	}

	/** kg of brine a kg of salt makes, for a readout. */
	static double brinePerSalt() {
		return ChlorAlkaliPlant.brinePerSalt();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("salt", salt);
		output.putDouble("water", water);
		output.putDouble("fromWell", fromWell);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		salt = input.getDoubleOr("salt", 0);
		water = input.getDoubleOr("water", 0);
		fromWell = input.getDoubleOr("fromWell", 0);
	}
}
