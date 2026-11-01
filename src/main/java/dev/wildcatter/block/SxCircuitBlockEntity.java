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
 * The solvent extraction circuit's mixer-settlers (30 kW of mixers, 480 V) (docs/multiblocks/uranium-mill.md): an amine in kerosene
 * takes the uranium out of the pregnant liquor (north face of its front cells), and 1.5 M salt water strips it again into a strong
 * strip liquor of 25 g/L (south face of its back cells). Kerosene on the east face of its bottom cells fills its settlers, then makes
 * up what is lost; rock salt by a hopper on top makes up the strip.
 */
public class SxCircuitBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double organic;
	private double salt;
	private String unit = "starting";

	public SxCircuitBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SX_CIRCUIT_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.HALITE));
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> kerosene() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 15 && at[1] == 0 && side == Direction.SOUTH ? Product.STRIP_LIQUOR : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.MILL_U3O8_KGS * Mill.stripPerU3o8() * Tuning.PLANT_HOLD_SECONDS;
	}

	/** For the game test: the settlers' first charge, as if it had come down a pipe. */
	public void testCharge(double litres) {
		organic = Math.min(Tuning.SX_KEROSENE_CHARGE, organic + litres);
		setChanged();
	}

	@Override
	protected void work(Level level) {
		if (organic < Tuning.SX_KEROSENE_CHARGE) {
			organic += Lines.pull(level, kerosene(), Product.KEROSENE, Tuning.SX_KEROSENE_CHARGE - organic, Pull.MACHINE, this);
		}
		if (organic < Tuning.SX_KEROSENE_CHARGE * 0.9) {
			unit = String.format("FILLING: %,.0f/%,.0f L of kerosene in its settlers (pipe kerosene to its east side)", organic, Tuning.SX_KEROSENE_CHARGE);
			return;
		}
		double waiting = Lines.available(level, inlet(), Product.PREGNANT_LIQUOR, Pull.MACHINE, this);
		if (waiting < 0.001) {
			unit = "idle: no pregnant liquor at its front (north face)";
			return;
		}
		if (salt < 1 && fed() > 0) {
			useOne();
			salt += Ore.HALITE.kgPerBlock();
		}
		if (salt < 0.001) {
			unit = "NO STRIP SALT: put a hopper of rock salt on the circuit";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double rated = Tuning.MILL_ORE_KGS / Tuning.MILL_SLURRY_SOLIDS * Mill.liquorPerSlurry(Tuning.MILL_SLURRY_SOLIDS);
		double[] in = new double[5];
		double got = pullMixed(level, inlet(), Product.PREGNANT_LIQUOR, Math.min(waiting, rated * 1.2 * seconds() * strength), in);
		double u = got * in[0];
		double strip = Math.min(u * Mill.stripPerU3o8(), room(Product.STRIP_LIQUOR));
		u = strip / Mill.stripPerU3o8();
		salt = Math.max(0, salt - u * Mill.saltPerU3o8());
		organic = Math.max(0, organic - got / 1000 * Tuning.SX_KEROSENE_LOSS_L_PER_M3);
		addMixed(Product.STRIP_LIQUOR, strip, new double[] {Tuning.SX_STRIP_U3O8_G_PER_L / 1000, 0, 0, 0, 0});
		unit = got > 0 ? String.format("extracting from %.0f L/s of pregnant liquor: %.2f L/s of strip liquor, %.3f kg/s of U3O8", got / seconds(),
				strip / seconds(), u / seconds()) : "idle";
	}

	@Override
	public Component inspect() {
		return Component.literal("SX circuit (mixer-settlers): " + unit + String.format("; kerosene %,.0f L; salt %,.0f kg and %d blocks; ", organic, salt, fed())
				+ describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("organic", organic);
		output.putDouble("salt", salt);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		organic = input.getDoubleOr("organic", 0);
		salt = input.getDoubleOr("salt", 0);
	}
}
