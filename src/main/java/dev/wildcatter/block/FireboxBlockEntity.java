package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Stream;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.jspecify.annotations.Nullable;

/**
 * A fuel store and a fire under a starter boiler. The fire only burns down while the boiler above wants heat, so a full steam
 * tank costs no fuel. (The 1 MW steam plant has its own burner: docs/multiblocks/steam-plant.md.)
 */
public class FireboxBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, Inspectable {
	private static final int[] ALL_SLOTS = new int[Tuning.FIREBOX_SLOTS];

	static {
		for (int i = 0; i < ALL_SLOTS.length; i++) {
			ALL_SLOTS[i] = i;
		}
	}

	private NonNullList<ItemStack> items = NonNullList.withSize(Tuning.FIREBOX_SLOTS, ItemStack.EMPTY);
	/** Ticks of fire left in the fuel that is burning. */
	private double burnLeft;

	public FireboxBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FIREBOX_ENTITY, pos, state);
	}

	/** True while the fire is alight. */
	public boolean isBurning() {
		return burnLeft > 0;
	}

	/** The block that holds its fuel (pipes use it to count each firebox once). */
	public BlockPos rootPos() {
		return worldPosition;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.wildcatter.firebox");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int getContainerSize() {
		return Tuning.FIREBOX_SLOTS;
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, this, 1);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return !(level instanceof ServerLevel server) || burnTicks(server, stack) > 0;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return ALL_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	/** Hoppers only take out what is not fuel (an empty bucket left by lava, say), never the coal. */
	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return !canPlaceItem(slot, stack);
	}

	/** How many ticks a piece of fuel burns, or 0 if it is not fuel. */
	private static int burnTicks(ServerLevel level, ItemStack stack) {
		if (stack.is(dev.wildcatter.WildcatterItems.CRUDE_OIL_BUCKET)) {
			return (int) (Tuning.CRUDE_BUCKET_UNITS * Tuning.OIL_FUEL_TICKS_PER_UNIT);
		}
		if (stack.is(dev.wildcatter.WildcatterItems.DIESEL_BUCKET)) {
			return (int) (Tuning.DIESEL_BUCKET_UNITS * Tuning.DIESEL_FUEL_TICKS_PER_UNIT);
		}
		if (stack.isEmpty() || stack.get(DataComponents.COOKING_FUEL) == null) {
			return 0;
		}
		LootContext context = new LootContext.Builder(new LootParams.Builder(level).create(LootContextParamSets.EMPTY)).create(Optional.empty());
		return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
	}

	/** Does the boiler above want heat? */
	private boolean wantsHeat(Level level) {
		return level.getBlockEntity(worldPosition.above()) instanceof BoilerBlockEntity boiler && boiler.wantsHeat();
	}

	/**
	 * Fuel off its steel pipe (docs/multiblocks/lease.md): whatever its runs carry that burns. A well's stream burns as it comes:
	 * a pumpjack's oil and gas (the water boils off), a gas well's wet gas.
	 */
	private void pipeFuel(Level level) {
		for (Lines.Line line : Lines.lines(level, Lines.faces(java.util.List.of(worldPosition)))) {
			double need = Tuning.FIREBOX_FEED_CAP - burnLeft;
			Product product = line.run().product();
			if (need <= 0 || product == null) {
				continue;
			}
			double ticks = switch (product) {
				case WELL_FLUID -> {
					double sum = 0;
					for (Lines.Port port : Lines.sources(line, product, FluidNode.Pull.MACHINE, this)) {
						if (port.node() instanceof WellSource well && sum < need) {
							Stream.Split got = well.drain((need - sum) / Tuning.OIL_FUEL_TICKS_PER_UNIT);
							sum += got.oil() * Tuning.OIL_FUEL_TICKS_PER_UNIT + got.gas() * Tuning.GAS_FUEL_TICKS_PER_UNIT;
						}
					}
					yield sum;
				}
				default -> {
					double perUnit = fuelTicks(product);
					yield perUnit > 0 ? Lines.pull(line, product, need / perUnit, FluidNode.Pull.MACHINE, this) * perUnit : 0;
				}
			};
			if (ticks > 0) {
				burnLeft += ticks;
				setChanged();
			}
		}
	}

	/** Ticks of fire in a unit (litre or m3) of what a line carries, or 0 if a firebox does not burn it. */
	private static double fuelTicks(Product product) {
		return switch (product) {
			case WET_GAS, GAS, COMPRESSED_GAS, DRIED_GAS, PIPELINE_GAS -> Tuning.GAS_FUEL_TICKS_PER_UNIT;
			case CRUDE -> Tuning.OIL_FUEL_TICKS_PER_UNIT;
			case DIESEL -> Tuning.DIESEL_FUEL_TICKS_PER_UNIT;
			case GASOLINE, NATURAL_GASOLINE -> Tuning.GASOLINE_FUEL_TICKS_PER_UNIT;
			case KEROSENE -> Tuning.KEROSENE_FUEL_TICKS_PER_UNIT;
			case RESIDUE -> Tuning.RESIDUE_FUEL_TICKS_PER_UNIT;
			case CONDENSATE -> Tuning.CONDENSATE_FUEL_TICKS_PER_UNIT;
			default -> 0;
		};
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FireboxBlockEntity firebox) {
		boolean wants = firebox.wantsHeat(level);
		if (wants && (level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL == 0 && firebox.burnLeft < Tuning.FIREBOX_FEED_CAP) {
			firebox.pipeFuel(level);
		}
		if (firebox.burnLeft <= 0 && wants) {
			firebox.light((ServerLevel) level);
		}
		if (firebox.burnLeft > 0 && wants) {
			firebox.burnLeft = Math.max(0, firebox.burnLeft - Tuning.FIREBOX_FUEL_PER_TICK);
			if (firebox.burnLeft <= 0) {
				firebox.light((ServerLevel) level);   // straight on to the next piece of fuel
			}
			firebox.setChanged();
		}
		boolean lit = firebox.burnLeft > 0;
		BlockState now = firebox.getBlockState();
		if (now.getValue(FireboxBlock.LIT) != lit) {
			level.setBlock(pos, now.setValue(FireboxBlock.LIT, lit), 3);
		}
	}

	/** Takes one piece of fuel from the store and starts it burning. */
	private void light(ServerLevel level) {
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			int ticks = burnTicks(level, stack);
			if (ticks > 0) {
				burnLeft = ticks;
				ItemStackTemplate remainder = stack.getCraftingRemainder();
				stack.shrink(1);
				if (stack.isEmpty() && remainder != null) {
					items.set(i, remainder.create());
				}
				setChanged();
				return;
			}
		}
	}

	@Override
	public Component inspect() {
		int stored = 0;
		for (ItemStack stack : items) {
			stored += stack.getCount();
		}
		return Component.literal("Firebox: " + (burnLeft > 0 ? "burning" : "cold") + ", " + stored + " fuel items stored"
				+ (burnLeft <= 0 && stored == 0 ? " (add coal or wood, by hand or with a hopper)" : ""));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putDouble("burn_left", burnLeft);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(Tuning.FIREBOX_SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		burnLeft = input.getDoubleOr("burn_left", 0);
	}
}
