package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
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
 * The helium recovery unit at the gas plant (docs/multiblocks/gas-plant.md, fuel.md): a nitrogen-rejection cold box with helium PSA
 * (75 kW, 480 V). It takes pipeline gas off the pipe at the north face of its front cells and gives it back there without its
 * helium, 0.4 % of the gas; the run's other users draw through it. It fills 8.5 m3 helium cylinders: empties by a hopper on top,
 * full ones out to a hopper under it.
 */
public class HeliumRecoveryBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(3, ItemStack.EMPTY);
	private boolean cylinder;
	private double helium;   // m3 in the cylinder on its filling rack
	private double recovered;
	private String unit = "starting";

	public HeliumRecoveryBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HELIUM_RECOVERY_ENTITY, pos, state);
	}

	private List<Lines.Face> gas() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 0 && at[1] == 0 && side == Direction.NORTH ? Product.PIPELINE_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.HELIUM_UNIT_M3S * Tuning.PLANT_HOLD_SECONDS;
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.EMPTY_HELIUM_CYLINDER);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {1, 2} : new int[0];
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot > 0 && side == Direction.DOWN;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == 0 && side == Direction.UP && takes(stack);
	}

	/** For the game test: the cylinder on the rack brought to {@code m3}. */
	public void setHeliumForTest(double m3) {
		cylinder = true;
		helium = m3;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		if (cylinder && helium >= Tuning.HELIUM_CYLINDER_M3 - 1e-9) {
			int out = slots.get(1).isEmpty() ? 1 : slots.get(2).isEmpty() ? 2 : -1;
			if (out < 0) {
				unit = "FULL: take the helium cylinders (a hopper under it)";
				return;
			}
			slots.set(out, new ItemStack(WildcatterItems.HELIUM_CYLINDER));
			cylinder = false;
			helium = 0;
		}
		if (!cylinder && fed() > 0) {
			useOne();
			cylinder = true;
		}
		if (!cylinder) {
			unit = "NO CYLINDER: put a hopper of empty helium cylinders on top";
			return;
		}
		double room = room(Product.PIPELINE_GAS);
		if (room <= 1e-6) {
			unit = "idle: the pipeline gas it has treated is not being drawn";
			return;
		}
		double waiting = Lines.available(level, gas(), Product.PIPELINE_GAS, Pull.MACHINE, this);
		if (waiting <= 1e-9) {
			unit = "NO GAS: pipe pipeline gas to the north face of its front";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the cold box: " + describe();
			return;
		}
		double m3 = Lines.pull(level, gas(), Product.PIPELINE_GAS, Math.min(Tuning.HELIUM_UNIT_M3S * seconds() * strength, room), Pull.MACHINE, this);
		double he = Math.min(m3 * Tuning.GAS_HELIUM_FRACTION, Tuning.HELIUM_CYLINDER_M3 - helium);
		helium += he;
		recovered += he;
		add(Product.PIPELINE_GAS, m3 - m3 * Tuning.GAS_HELIUM_FRACTION);
		unit = String.format("treating %.2f m3/s of pipeline gas: %.1f m3/h of helium; cylinder %.2f of %.1f m3", m3 / seconds(), he / seconds() * 3600, helium,
				Tuning.HELIUM_CYLINDER_M3);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Helium recovery unit: %s; %.1f m3 recovered; %s", unit, recovered, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putBoolean("cylinder", cylinder);
		output.putDouble("helium", helium);
		output.putDouble("recovered", recovered);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		cylinder = input.getBooleanOr("cylinder", false);
		helium = input.getDoubleOr("helium", 0);
		recovered = input.getDoubleOr("recovered", 0);
	}
}
