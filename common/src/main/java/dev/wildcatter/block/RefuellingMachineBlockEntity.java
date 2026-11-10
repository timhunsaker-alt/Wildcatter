package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Reactor;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The refuelling machine (docs/multiblocks/fuel-handling.md): the manipulator crane over the vessel's flooded cavity, against the
 * vessel. With the plant cold, depressurized and its pumps stopped, it runs an outage one move a minute: REFUEL takes the 3 most-burned
 * assemblies through the transfer system to the pool, shuffles the rest and loads 3 fresh ones from its hopper; DEFUEL takes the whole
 * core out. Sneak-click cycles REFUEL, DEFUEL and STOP. 30 kW at 480 V.
 */
public class RefuellingMachineBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int TO_POOL = 0, SHUFFLE = 1, FRESH = 2;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private final Deque<Integer> queue = new ArrayDeque<>();
	private String mode = "STOPPED";
	private double progress;
	private double moveSeconds = Tuning.FUEL_MOVE_SECONDS;
	private String unit = "stopped";

	public RefuellingMachineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.REFUELLING_MACHINE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterBlocks.FUEL_ASSEMBLY.asItem());
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takes(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	private @Nullable ReactorVesselBlockEntity vessel(Level level) {
		return Touching.find(level, worldPosition, ReactorVesselBlockEntity.class, v -> true);
	}

	private @Nullable SpentFuelPoolBlockEntity pool(Level level) {
		for (BlockEntity e : Touching.parts(level, worldPosition)) {
			if (e instanceof BigPartBlockEntity && e.getBlockState().getBlock() instanceof FuelHandlingPartBlock b && b.part() == FuelHandlingPartBlock.Part.FUEL_TRANSFER_SYSTEM) {
				SpentFuelPoolBlockEntity pool = Touching.find(level, e.getBlockPos(), SpentFuelPoolBlockEntity.class, p -> true);
				if (pool != null) {
					return pool;
				}
			}
		}
		return null;
	}

	/** A sneak-click: REFUEL, DEFUEL, STOP in turn. */
	public String nextMode(Level level) {
		queue.clear();
		progress = 0;
		ReactorVesselBlockEntity vessel = vessel(level);
		switch (mode) {
			case "STOPPED" -> {
				mode = "REFUEL";
				for (int i = 0; i < Tuning.RELOAD_ASSEMBLIES; i++) {
					queue.add(TO_POOL);
				}
				for (int i = 0; i < Tuning.RELOAD_ASSEMBLIES; i++) {
					queue.add(SHUFFLE);
				}
				for (int i = 0; i < Tuning.RELOAD_ASSEMBLIES; i++) {
					queue.add(FRESH);
				}
			}
			case "REFUEL" -> {
				mode = "DEFUEL";
				int n = vessel == null ? Tuning.CORE_ASSEMBLIES : vessel.reactor().assemblies();
				for (int i = 0; i < n; i++) {
					queue.add(TO_POOL);
				}
			}
			default -> mode = "STOPPED";
		}
		setChanged();
		return "Refuelling machine: " + mode + (queue.isEmpty() ? "" : String.format(" (%d moves)", queue.size()));
	}

	/** For the game test: a move in {@code seconds}. */
	public void setMoveSecondsForTest(double seconds) {
		moveSeconds = seconds;
	}

	public String mode() {
		return mode;
	}

	@Override
	protected void work(Level level) {
		if (queue.isEmpty()) {
			if (!mode.equals("STOPPED")) {
				unit = mode + " done";
				mode = "STOPPED";
			}
			return;
		}
		ReactorVesselBlockEntity vessel = vessel(level);
		SpentFuelPoolBlockEntity pool = pool(level);
		if (vessel == null) {
			unit = "NOT BUILT: set it against the REACTOR VESSEL";
			return;
		}
		if (pool == null) {
			unit = "NOT BUILT: a FUEL TRANSFER SYSTEM against it and the SPENT FUEL POOL";
			return;
		}
		if (!vessel.openForRefuelling()) {
			unit = "HELD: the plant must be at cold shutdown (under 60 C, depressurized, its pumps stopped)";
			return;
		}
		Reactor r = vessel.reactor();
		int move = queue.peek();
		if (move == TO_POOL && (r.assemblies() == 0 || pool.full())) {
			unit = r.assemblies() == 0 ? "the core is empty" : "HELD: the pool's racks are full";
			if (r.assemblies() == 0) {
				queue.poll();
			}
			return;
		}
		if (move == FRESH && slots.get(0).isEmpty()) {
			unit = "HELD: NO NEW FUEL: put fuel assemblies in its hopper";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		progress += seconds() * strength;
		String what = move == TO_POOL ? "an assembly to the pool" : move == SHUFFLE ? "shuffling an assembly" : "a new assembly into the core";
		unit = String.format("%s: %s, %.0f %%; %d moves left", mode, what, progress / moveSeconds * 100, queue.size());
		if (progress >= moveSeconds) {
			progress = 0;
			queue.poll();
			switch (move) {
				case TO_POOL -> pool.store(level, r.removeAssembly(r.mostBurned()));
				case FRESH -> {
					double a = dev.wildcatter.WildcatterComponents.assay(slots.get(0));
					slots.get(0).shrink(1);
					r.loadAssembly(0, a);
				}
				default -> {
				}
			}
			vessel.setChanged();
		}
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Refuelling machine (manipulator crane): %s; %d new assemblies in its hopper; %s", unit, slots.get(0).getCount(), describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putString("mode", mode);
		output.putDouble("progress", progress);
		StringBuilder q = new StringBuilder();
		for (int m : queue) {
			q.append(m);
		}
		output.putString("queue", q.toString());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		mode = input.getStringOr("mode", "STOPPED");
		progress = input.getDoubleOr("progress", 0);
		queue.clear();
		for (char c : input.getStringOr("queue", "").toCharArray()) {
			queue.add(c - '0');
		}
	}
}
