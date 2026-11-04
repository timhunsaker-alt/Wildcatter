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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The potline's fume treatment centre (docs/multiblocks/potline.md): a dry scrubber, its alumina injection reactor and bag filter
 * house, catching the pots' HF on fresh alumina. The pot gas comes in at the west face of its bottom cells (z 0), fresh alumina bags
 * by a hopper on top (slot 0), and reacted alumina bags go out below, one for each 45,600 Nm3 of gas treated. It runs on the main
 * fans set against it, 100,000 Nm3/h each.
 */
public class FumeTreatmentCentreBlockEntity extends PlantUnitBlockEntity implements HopperFed {
	private static final int OUT = 4;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1 + OUT, ItemStack.EMPTY);
	private double treated;   // m3 toward the next reacted bag
	private double rate;      // m3/s, for the readout
	private int fans;
	private int bags;

	public FumeTreatmentCentreBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FUME_TREATMENT_CENTRE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.ALUMINA_BAG);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {1, 2, 3, 4} : new int[0];
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot >= 1 && side == Direction.DOWN;
	}

	public int outCount() {
		int n = 0;
		for (int i = 1; i < slots.size(); i++) {
			n += slots.get(i).getCount();
		}
		return n;
	}

	public void giveAll(Player player) {
		for (int i = 1; i < slots.size(); i++) {
			ItemStack stack = slots.get(i);
			if (!stack.isEmpty()) {
				if (!player.getInventory().add(stack.copy())) {
					net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy());
				}
				slots.set(i, ItemStack.EMPTY);
			}
		}
		setChanged();
	}

	private boolean roomOut() {
		for (int i = 1; i < slots.size(); i++) {
			ItemStack in = slots.get(i);
			if (in.isEmpty() || in.is(WildcatterItems.REACTED_ALUMINA_BAG) && in.getCount() < in.getMaxStackSize()) {
				return true;
			}
		}
		return false;
	}

	private void putOut() {
		for (int i = 1; i < slots.size(); i++) {
			ItemStack in = slots.get(i);
			if (in.isEmpty()) {
				slots.set(i, new ItemStack(WildcatterItems.REACTED_ALUMINA_BAG));
				return;
			}
			if (in.is(WildcatterItems.REACTED_ALUMINA_BAG) && in.getCount() < in.getMaxStackSize()) {
				in.grow(1);
				return;
			}
		}
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[1] == 0 && at[2] == 0, Direction.WEST);
	}

	/** For the game test: gas as if treated. */
	public void addTreated(double m3) {
		treated += m3;
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FumeTreatmentCentreBlockEntity ftc) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		ftc.work(level);
		ftc.setChanged();
	}

	private void work(Level level) {
		double s = seconds();
		rate = 0;
		if (slots.get(0).isEmpty()) {
			status = "NO ALUMINA: put fresh alumina bags in its hopper; the pots' gas is not being taken";
			return;
		}
		if (!roomOut()) {
			status = "FULL: take the reacted alumina bags (empty hand, or a hopper under it)";
			return;
		}
		double strength = 0;
		fans = 0;
		for (BlockEntity next : Touching.parts(level, worldPosition)) {
			if (next instanceof UnitMotorBlockEntity fan && fan.kind().equals("ftc_fan") && fans < Tuning.FTC_FANS) {
				fans++;
				strength += fan.drive(level);
			}
		}
		if (fans == 0) {
			status = "NO FAN: set a main fan against it";
			return;
		}
		if (strength <= 0) {
			status = "starting: its main fans";
			return;
		}
		double got = Lines.pull(level, inlet(), Product.POT_GAS, Tuning.FTC_FAN_M3H / 3600 * strength * s, Pull.MACHINE, this);
		rate = got / s;
		treated += got;
		while (treated >= Tuning.FTC_GAS_PER_BAG && !slots.get(0).isEmpty() && roomOut()) {
			treated -= Tuning.FTC_GAS_PER_BAG;
			slots.get(0).shrink(1);
			putOut();
			bags++;
		}
		status = got > 1e-6 ? String.format("%,.0f Nm3/h of pot gas, %d of %d fans", rate * 3600, fans, Tuning.FTC_FANS)
				: String.format("no pot gas: pipe a pot row's duct end to its west side (%d of %d fans)", fans, Tuning.FTC_FANS);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Fume treatment centre: %s; %d fresh bags; next reacted bag %.0f %%; %d made, %d waiting", status, slots.get(0).getCount(),
				Math.min(100, treated / Tuning.FTC_GAS_PER_BAG * 100), bags, outCount()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("treated", treated);
		output.putInt("bags", bags);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		treated = input.getDoubleOr("treated", 0);
		bags = input.getIntOr("bags", 0);
	}
}
