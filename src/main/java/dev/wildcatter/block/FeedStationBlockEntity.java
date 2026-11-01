package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Enrichment;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A solid feed station (docs/multiblocks/enrichment.md): a heated cabinet (30 kW, 480 V) around a 48Y of natural UF6, subliming it
 * onto the cascade header at the south face of its back cells. It runs the cascade: it counts the driven centrifuges on its header,
 * at least one a stage, works out their separative work and hands the product to the product take-off and the tails to the tails
 * take-off on the same header. Without both it idles the cascade on total reflux. The emptied 48Y goes out to a hopper under it.
 */
public class FeedStationBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int OUT = 2;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1 + OUT, ItemStack.EMPTY);
	private boolean loaded;
	private double uf6;   // kg left in the cylinder in the cabinet
	private double swuDone;
	private String unit = "starting";

	public FeedStationBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FEED_STATION_ENTITY, pos, state);
	}

	private List<Lines.Face> header() {
		return faces(at -> at[2] == 4 && at[1] == 0, Direction.SOUTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 4 && at[1] == 0 && side == Direction.SOUTH ? Product.UF6 : null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.UF6_CYLINDER_48Y);
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

	public double uf6() {
		return uf6;
	}

	/** For the game test: the cylinder in the cabinet with only {@code kg} left. */
	public void setUf6ForTest(double kg) {
		loaded = true;
		uf6 = kg;
		setChanged();
	}

	public int emptied() {
		int n = 0;
		for (int i = 1; i <= OUT; i++) {
			n += slots.get(i).getCount();
		}
		return n;
	}

	public void giveAll(Player player) {
		for (int i = 1; i <= OUT; i++) {
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

	private int freeOut() {
		for (int i = 1; i <= OUT; i++) {
			if (slots.get(i).isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	@Override
	protected void work(Level level) {
		if (loaded && uf6 <= 1e-6) {
			int out = freeOut();
			if (out < 0) {
				unit = "FULL: take the emptied 48Ys (empty hand, or a hopper under it)";
				return;
			}
			slots.set(out, new ItemStack(WildcatterItems.EMPTY_48Y_CYLINDER));
			loaded = false;
			uf6 = 0;
		}
		if (!loaded && fed() > 0) {
			useOne();   // the next 48Y in the bin goes into the cabinet
			loaded = true;
			uf6 = Tuning.CYLINDER_48Y_KG;
		}
		// the cascade on its header
		List<CentrifugeBlockEntity> machines = FrequencyConverterBlockEntity.centrifuges(level, header());
		int driven = 0;
		double swu = 0;
		for (CentrifugeBlockEntity c : machines) {
			if (c.speed() > 0) {
				driven++;
				swu += c.speed() * Enrichment.machineSwuPerSecond() * seconds();
			}
		}
		TakeoffBlockEntity product = null, tails = null;
		for (Lines.Line line : Lines.lines(level, header())) {
			for (Lines.Port port : line.ports()) {
				if (port.node() instanceof TakeoffBlockEntity t) {
					if (t.product() && product == null) {
						product = t;
					} else if (!t.product() && tails == null) {
						tails = t;
					}
				}
			}
		}
		String cascade = String.format("%d machines driven of %d on the header, %.1f SWU/h", driven, machines.size(), swu / seconds() * 3600);
		if (!loaded) {
			unit = "NO FEED: put a hopper of natural UF6 48Ys on top; " + cascade;
			return;
		}
		if (machines.isEmpty()) {
			unit = "NO MACHINES: set centrifuges on its header (the pipe at the south face of its back)";
			return;
		}
		if (driven == 0) {
			unit = "NO DRIVE: set a FREQUENCY CONVERTER on the header; " + cascade;
			return;
		}
		if (driven < Enrichment.stages()) {
			unit = String.format("TOO FEW STAGES (%d of %d): a cascade needs a machine for each stage; %s", driven, Enrichment.stages(), cascade);
			return;
		}
		if (product == null || tails == null || product.room() <= 0 || tails.room() <= 0) {
			String why = product == null ? "no PRODUCT TAKE-OFF on the header" : tails == null ? "no TAILS TAKE-OFF on the header"
					: product.room() <= 0 ? "the product take-off has no cylinder ready" : "the tails take-off has no cylinder ready";
			unit = "TOTAL REFLUX: " + why + ", nothing is withdrawn; " + cascade;
			return;
		}
		double heat = drive(level);
		if (heat <= 0) {
			unit = "starting the heater: " + describe();
			return;
		}
		// separative work into product, limited by the feed left and the cylinders' room
		double p = swu / Enrichment.swuPerProduct();   // kg of product U
		p = Math.min(p, uf6 / Tuning.UF6_PER_U / Enrichment.feedPerProduct());
		p = Math.min(p, product.room() / Tuning.UF6_PER_U);
		p = Math.min(p, tails.room() / Tuning.UF6_PER_U / Enrichment.tailsPerProduct());
		p = Math.max(0, p);
		uf6 = Math.max(0, uf6 - p * Enrichment.feedPerProduct() * Tuning.UF6_PER_U);
		product.addUf6(p * Tuning.UF6_PER_U);
		tails.addUf6(p * Enrichment.tailsPerProduct() * Tuning.UF6_PER_U);
		swuDone += p * Enrichment.swuPerProduct();
		unit = String.format("feeding %.0f kg of UF6 an hour; %,.0f kg left in the 48Y (%.1f %%); %s; %.1f kg of %.2f %% product an hour; %,.0f SWU done",
				p * Enrichment.feedPerProduct() * Tuning.UF6_PER_U / seconds() * 3600, uf6, uf6 / Tuning.CYLINDER_48Y_KG * 100, cascade,
				p * Tuning.UF6_PER_U / seconds() * 3600, Tuning.ASSAY_PRODUCT * 100, swuDone);
	}

	@Override
	public Component inspect() {
		return Component.literal("Feed station: " + unit + String.format("; %d emptied, %d waiting; ", emptied(), fed()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putBoolean("loaded", loaded);
		output.putDouble("uf6", uf6);
		output.putDouble("swu", swuDone);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		loaded = input.getBooleanOr("loaded", false);
		uf6 = input.getDoubleOr("uf6", 0);
		swuDone = input.getDoubleOr("swu", 0);
	}
}
