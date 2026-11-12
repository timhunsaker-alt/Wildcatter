package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.MeltShop;
import dev.wildcatter.fluid.MeltShop.Grade;
import dev.wildcatter.fluid.Product;
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
import org.jspecify.annotations.Nullable;

/**
 * The ingot caster (docs/multiblocks/nickel-smelter.md, 17b2): bottom-poured ingot moulds on a car. It takes the AOD's finished heat, pours
 * it in about 10 minutes into twenty 100 kg ingots, and strips them to a hopper under it. No power: the ladle is tilted by the shop's crane.
 */
public class IngotCasterBlockEntity extends PlantUnitBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private @Nullable Grade pouring;
	private double pourLeft;   // s
	private int cast;          // ingots, all told

	public IngotCasterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.INGOT_CASTER_ENTITY, pos, state);
	}

	public static Item ingotFor(Grade g) {
		return switch (g) {
			case S304 -> WildcatterItems.STAINLESS_304_INGOT;
			case S316 -> WildcatterItems.STAINLESS_316_INGOT;
			case NICR -> WildcatterItems.NICKEL_CHROMIUM_INGOT;
		};
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[] {0, 1} : new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	public boolean pouring() {
		return pouring != null;
	}

	/** For the game test: the pour's minutes skipped. */
	public void testFinishPour() {
		if (pouring != null) {
			pourLeft = 0.001;
		}
	}

	private boolean room(Item ingot, int n) {
		int fits = 0;
		for (ItemStack s : slots) {
			fits += s.isEmpty() ? ingot.getDefaultMaxStackSize() : s.is(ingot) ? s.getMaxStackSize() - s.getCount() : 0;
		}
		return fits >= n;
	}

	private void put(Item ingot, int n) {
		for (int i = 0; i < slots.size() && n > 0; i++) {
			ItemStack s = slots.get(i);
			if (s.isEmpty()) {
				int k = Math.min(n, ingot.getDefaultMaxStackSize());
				slots.set(i, new ItemStack(ingot, k));
				n -= k;
			} else if (s.is(ingot)) {
				int k = Math.min(n, s.getMaxStackSize() - s.getCount());
				s.grow(k);
				n -= k;
			}
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, IngotCasterBlockEntity c) {
		if (level.isClientSide() || (level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		c.work(level);
		c.setChanged();
	}

	private void work(Level level) {
		int n = MeltShop.ingotsPerHeat();
		if (pouring == null) {
			AodConverterBlockEntity aod = Touching.find(level, worldPosition, AodConverterBlockEntity.class, a -> true);
			if (aod == null) {
				status = "NOT BUILT: set it against the AOD converter";
				return;
			}
			if (!aod.finished()) {
				status = "waiting for a finished heat from the AOD";
				return;
			}
			Grade next = aod.pour();
			if (next == null) {
				return;
			}
			pouring = next;
			pourLeft = Tuning.CASTER_POUR_SECONDS;
		}
		if (!room(ingotFor(pouring), n)) {
			status = "FULL: take its ingots (a hopper under it)";
			return;
		}
		pourLeft -= seconds();
		if (pourLeft > 0) {
			status = String.format("pouring %s into %d moulds: %.0f min left", pouring.label, n, pourLeft / 60);
			return;
		}
		put(ingotFor(pouring), n);
		cast += n;
		status = String.format("cast %d ingots of %s", n, pouring.label);
		pouring = null;
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
	public Component inspect() {
		int waiting = slots.get(0).getCount() + slots.get(1).getCount();
		return Component.literal(String.format("Ingot caster (bottom-poured, %.0f kg ingots): %s; %d ingots waiting, %d cast", Tuning.ALLOY_INGOT_KG, status, waiting, cast));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putString("pouring", pouring == null ? "" : pouring.name());
		output.putDouble("pourLeft", pourLeft);
		output.putInt("cast", cast);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		String p = input.getStringOr("pouring", "");
		pouring = p.isEmpty() ? null : InductionFurnaceBlockEntity.parse(p, null);
		pourLeft = input.getDoubleOr("pourLeft", 0);
		cast = input.getIntOr("cast", 0);
	}
}
