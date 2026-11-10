package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.SpentFuel;
import java.util.ArrayList;
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
 * The cask loading station (docs/multiblocks/reactor-extensions.md, 19b): the pool's cask pit, the canister welder and the vacuum
 * drying and helium backfill skid, against the spent fuel pool's controller; 50 kW at 480 V. Empty canisters by a hopper on top. With
 * the pool on LOAD CASK it takes the coolest assemblies, a move a minute, up to 12 a canister (at most 1.5 kW each and 12 kW in all),
 * then drains, dries and backfills (20 minutes), welds (10) and sends the canister in a transfer cask to its linked storage module (10).
 */
public class CaskLoadingStationBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private final List<SpentFuel.Assembly> canister = new ArrayList<>();
	private String stage = "IDLE";
	private double progress;
	private @Nullable BlockPos link;
	private int sent;
	private String unit = "idle";
	private final double[] times = {Tuning.FUEL_MOVE_SECONDS, Tuning.CASK_DRY_SECONDS, Tuning.CASK_WELD_SECONDS, Tuning.CASK_TRANSFER_SECONDS};

	public CaskLoadingStationBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CASK_STATION_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.DRY_STORAGE_CANISTER);
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

	/** For the game test: a move, the drying, the welding and the transfer in these seconds. */
	public void setTimesForTest(double move, double dry, double weld, double transfer) {
		times[0] = move;
		times[1] = dry;
		times[2] = weld;
		times[3] = transfer;
	}

	public String stage() {
		return stage;
	}

	public int sent() {
		return sent;
	}

	/** A sneak-click with the Power Meter: links the nearest empty storage module within 64 m. */
	public String link(Level level) {
		StorageModuleBlockEntity best = null;
		double bestD = Tuning.CASK_LINK_BLOCKS * Tuning.CASK_LINK_BLOCKS;
		for (BlockPos p : java.util.Set.copyOf(StorageModuleBlockEntity.modules(level))) {
			if (level.getBlockEntity(p) instanceof StorageModuleBlockEntity m && m.records().isEmpty() && p.distSqr(worldPosition) <= bestD) {
				best = m;
				bestD = p.distSqr(worldPosition);
			}
		}
		if (best == null) {
			return String.format("Cask loading station: no empty storage module within %.0f m", Tuning.CASK_LINK_BLOCKS);
		}
		link = best.getBlockPos();
		setChanged();
		return String.format("Cask loading station: linked to the storage module at %d %d %d (%.0f m)", link.getX(), link.getY(), link.getZ(), Math.sqrt(bestD));
	}

	private @Nullable StorageModuleBlockEntity module(Level level) {
		if (link == null || !level.isLoaded(link) || link.distSqr(worldPosition) > Tuning.CASK_LINK_BLOCKS * Tuning.CASK_LINK_BLOCKS) {
			return null;
		}
		return level.getBlockEntity(link) instanceof StorageModuleBlockEntity m ? m : null;
	}

	private double canisterWatts() {
		double w = 0;
		for (SpentFuel.Assembly a : canister) {
			w += a.watts();
		}
		return w;
	}

	/** The coolest rack that still fits the canister's limits, or null. */
	private SpentFuel.@Nullable Assembly coolest(SpentFuelPoolBlockEntity pool) {
		double total = canisterWatts();
		SpentFuel.Assembly best = null;
		for (SpentFuel.Assembly a : pool.racks()) {
			double w = a.watts();
			if (w <= Tuning.CANISTER_MAX_W_EACH && total + w <= Tuning.CANISTER_MAX_W && (best == null || w < best.watts())) {
				best = a;
			}
		}
		return best;
	}

	@Override
	protected void work(Level level) {
		double dt = seconds();
		for (SpentFuel.Assembly a : canister) {
			a.decay(dt);
		}
		SpentFuelPoolBlockEntity pool = Touching.find(level, worldPosition, SpentFuelPoolBlockEntity.class, p -> true);
		if (pool == null) {
			unit = "NOT BUILT: set it against the SPENT FUEL POOL's controller";
			return;
		}
		StorageModuleBlockEntity module = module(level);
		if (stage.equals("IDLE")) {
			if (!pool.caskMode()) {
				unit = "idle: set the pool's controller to LOAD CASK (sneak-click it)";
				return;
			}
			if (module == null || !module.records().isEmpty()) {
				unit = "HELD: NO MODULE: link an empty storage module within 64 m (sneak-click this station with the Power Meter)";
				return;
			}
			if (slots.get(0).isEmpty()) {
				unit = "HELD: NO CANISTER: put empty canisters in its hopper";
				return;
			}
			slots.get(0).shrink(1);
			stage = "LOADING";
			progress = 0;
			setChanged();
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = stage + ", starting: " + describe();
			return;
		}
		progress += dt * strength;
		switch (stage) {
			case "LOADING" -> {
				unit = String.format("LOADING: %d of %d assemblies, %.2f kW; next move %.0f %%", canister.size(), Tuning.CANISTER_ASSEMBLIES, canisterWatts() / 1000,
						progress / times[0] * 100);
				if (progress < times[0]) {
					return;
				}
				progress = 0;
				SpentFuel.Assembly next = pool.caskMode() ? coolest(pool) : null;
				if (next != null) {
					pool.takeRack(level, next);
					canister.add(next);
				}
				if (canister.size() >= Tuning.CANISTER_ASSEMBLIES || next == null && !canister.isEmpty()) {
					stage = "DRYING";
				} else if (next == null) {
					double coolest = pool.racks().stream().mapToDouble(SpentFuel.Assembly::watts).min().orElse(0);
					unit = pool.racks().isEmpty() ? "HELD: the pool is empty" : String.format("HELD: no assembly cool enough for a canister (the coolest %.1f kW; the limit %.1f kW)",
							coolest / 1000, Tuning.CANISTER_MAX_W_EACH / 1000);
				}
			}
			case "DRYING" -> {
				unit = String.format("DRAINING, VACUUM DRYING AND HELIUM BACKFILL: %d assemblies, %.0f %%", canister.size(), progress / times[1] * 100);
				if (progress >= times[1]) {
					stage = "WELDING";
					progress = 0;
				}
			}
			case "WELDING" -> {
				unit = String.format("WELDING THE LID: %.0f %%", progress / times[2] * 100);
				if (progress >= times[2]) {
					stage = "TRANSFER";
					progress = 0;
				}
			}
			case "TRANSFER" -> {
				if (module == null || !module.records().isEmpty()) {
					unit = "HELD: TRANSFER: no empty linked storage module";
					progress = 0;
					return;
				}
				unit = String.format("TRANSFER CASK to the storage module: %.0f %%", progress / times[3] * 100);
				if (progress >= times[3]) {
					module.receive(level, new ArrayList<>(canister));
					canister.clear();
					sent++;
					stage = "IDLE";
					progress = 0;
					link = null;   // the module is full: the next canister needs a new one
					unit = "canister sent to its storage module";
				}
			}
			default -> stage = "IDLE";
		}
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Cask loading station: %s; %d empty canisters in its hopper; %d sent; %s", unit, slots.get(0).getCount(), sent, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putString("stage", stage);
		output.putDouble("progress", progress);
		output.putInt("sent", sent);
		if (link != null) {
			output.putLong("link", link.asLong());
		}
		SpentFuelPoolBlockEntity.saveRecords(output, "c", canister);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		stage = input.getStringOr("stage", "IDLE");
		progress = input.getDoubleOr("progress", 0);
		sent = input.getIntOr("sent", 0);
		long l = input.getLongOr("link", Long.MIN_VALUE);
		link = l == Long.MIN_VALUE ? null : BlockPos.of(l);
		SpentFuelPoolBlockEntity.loadRecords(input, "c", canister);
	}
}
