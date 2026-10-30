package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.energy.Inspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A fuel store and a fire. The fire only burns down while the boiler above wants heat, so a full steam tank costs
 * no fuel. A 3x3 floor of fireboxes becomes one big firebox: the lowest corner is the master, its nine slots are
 * the fuel store of all nine, and a hopper or a click on any of them reaches it.
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
	private @Nullable BlockPos master;
	private boolean isMaster;

	public FireboxBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FIREBOX_ENTITY, pos, state);
	}

	private boolean isSlave() {
		return master != null && !isMaster;
	}

	private @Nullable FireboxBlockEntity root() {
		if (!isSlave()) {
			return this;
		}
		return level != null && level.getBlockEntity(master) instanceof FireboxBlockEntity m && m.isMaster ? m : null;
	}

	/** True while the fire is alight. A bank is alight or not as a whole. */
	public boolean isBurning() {
		FireboxBlockEntity r = root();
		return r != null && r.burnLeft > 0;
	}

	/** The block that holds this bank's fuel: the master, or the firebox itself if it is alone. */
	public BlockPos rootPos() {
		FireboxBlockEntity r = root();
		return r != null ? r.worldPosition : worldPosition;
	}

	/**
	 * Fuel piped in from a well: tops the fire up with up to {@code ticks} of burn, but only while a boiler above wants
	 * heat and the fire is low. Returns how many ticks it took.
	 */
	public double feed(double ticks) {
		FireboxBlockEntity r = root();
		if (r == null || level == null || !r.wantsHeat(level) || r.burnLeft >= Tuning.FIREBOX_FEED_CAP) {
			return 0;
		}
		double taken = Math.min(ticks, Tuning.FIREBOX_FEED_CAP - r.burnLeft);
		r.burnLeft += taken;
		r.setChanged();
		return taken;
	}

	/** Can this firebox use more fuel right now? */
	public boolean hungry() {
		FireboxBlockEntity r = root();
		return r != null && level != null && r.burnLeft < Tuning.FIREBOX_FEED_CAP && r.wantsHeat(level);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.wildcatter.firebox");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		FireboxBlockEntity r = root();
		return r != null ? r.items : items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		FireboxBlockEntity r = root();
		(r != null ? r : this).items = items;
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

	private int cells() {
		return isMaster ? Tuning.BIG_SIZE * Tuning.BIG_SIZE : 1;
	}

	/** Does any boiler above this firebox (or bank) want heat? */
	private boolean wantsHeat(Level level) {
		int n = isMaster ? Tuning.BIG_SIZE : 1;
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				if (level.getBlockEntity(worldPosition.offset(x, 1, z)) instanceof BoilerBlockEntity boiler && boiler.wantsHeat()) {
					return true;
				}
			}
		}
		return false;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FireboxBlockEntity firebox) {
		long phase = level.getGameTime() + pos.hashCode();
		boolean lit;
		if (firebox.isSlave()) {
			FireboxBlockEntity m = firebox.root();
			if (m == null || !m.contains(pos)) {
				firebox.release(level);
				return;
			}
			lit = m.burnLeft > 0;
		} else {
			if (phase % Tuning.STRUCTURE_CHECK_TICKS == 0) {
				if (firebox.isMaster) {
					if (!firebox.stillValid(level)) {
						firebox.dissolve(level);
					}
				} else {
					firebox.tryForm(level);
				}
			}
			boolean wants = firebox.wantsHeat(level);
			if (firebox.burnLeft <= 0 && wants) {
				firebox.light((ServerLevel) level);
			}
			if (firebox.burnLeft > 0 && wants) {
				firebox.burnLeft = Math.max(0, firebox.burnLeft - Tuning.FIREBOX_FUEL_PER_TICK * firebox.cells());
				if (firebox.burnLeft <= 0) {
					firebox.light((ServerLevel) level);   // straight on to the next piece of fuel
				}
				firebox.setChanged();
			}
			lit = firebox.burnLeft > 0;
		}
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

	private boolean contains(BlockPos pos) {
		return isMaster && pos.getX() >= worldPosition.getX() && pos.getX() < worldPosition.getX() + Tuning.BIG_SIZE
				&& pos.getY() == worldPosition.getY()
				&& pos.getZ() >= worldPosition.getZ() && pos.getZ() < worldPosition.getZ() + Tuning.BIG_SIZE;
	}

	/** Looks for a 3x3 floor of free fireboxes with this one as its lowest corner. */
	private void tryForm(Level level) {
		int n = Tuning.BIG_SIZE;
		BlockPos o = worldPosition;
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				if (!(level.getBlockEntity(o.offset(x, 0, z)) instanceof FireboxBlockEntity f) || f.master != null) {
					return;
				}
			}
		}
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				FireboxBlockEntity f = (FireboxBlockEntity) level.getBlockEntity(o.offset(x, 0, z));
				if (f != this) {
					// Their fuel moves into the shared store, what does not fit is dropped on top.
					for (ItemStack stack : f.items) {
						ItemStack left = stack;
						for (int i = 0; i < items.size() && !left.isEmpty(); i++) {
							ItemStack have = items.get(i);
							if (have.isEmpty()) {
								items.set(i, left.copy());
								left = ItemStack.EMPTY;
							} else if (ItemStack.isSameItemSameComponents(have, left)) {
								int move = Math.min(left.getCount(), have.getMaxStackSize() - have.getCount());
								have.grow(move);
								left.shrink(move);
							}
						}
						if (!left.isEmpty()) {
							net.minecraft.world.Containers.dropItemStack(level, o.getX() + 0.5, o.getY() + 1.0, o.getZ() + 0.5, left);
						}
					}
					f.items = NonNullList.withSize(Tuning.FIREBOX_SLOTS, ItemStack.EMPTY);
					burnLeft = Math.max(burnLeft, f.burnLeft);
					f.burnLeft = 0;
					f.master = o;
					f.setChanged();
				}
			}
		}
		master = o;
		isMaster = true;
		setChanged();
		level.playSound(null, o.offset(1, 1, 1), WildcatterSounds.STRUCTURE_FORMED, SoundSource.BLOCKS, 1.2F, 1.2F);
	}

	private boolean stillValid(Level level) {
		int n = Tuning.BIG_SIZE;
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				if (!(level.getBlockEntity(worldPosition.offset(x, 0, z)) instanceof FireboxBlockEntity f) || !worldPosition.equals(f.master)) {
					return false;
				}
			}
		}
		return true;
	}

	private void dissolve(Level level) {
		int n = Tuning.BIG_SIZE;
		for (int x = 0; x < n; x++) {
			for (int z = 0; z < n; z++) {
				if (level.getBlockEntity(worldPosition.offset(x, 0, z)) instanceof FireboxBlockEntity f && worldPosition.equals(f.master)) {
					f.release(level);
				}
			}
		}
	}

	private void release(Level level) {
		master = null;
		isMaster = false;
		setChanged();
	}

	/** Breaking a slave must not spill the master's fuel; the master's own items drop when the master breaks. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (!isSlave()) {
			super.preRemoveSideEffects(pos, state);
		}
	}

	@Override
	public Component inspect() {
		FireboxBlockEntity r = root();
		if (r == null) {
			return Component.literal("Firebox: its bank has lost its master");
		}
		int stored = 0;
		for (ItemStack stack : r.items) {
			stored += stack.getCount();
		}
		String what = r.isMaster ? "Big firebox (9 fireboxes)" : "Firebox";
		return Component.literal(what + ": " + (r.burnLeft > 0 ? "burning" : "cold") + ", " + stored + " fuel items stored"
				+ (r.burnLeft <= 0 && stored == 0 ? " (add coal or wood, by hand or with a hopper)" : ""));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putDouble("burn_left", burnLeft);
		output.putBoolean("is_master", isMaster);
		if (master != null) {
			output.putInt("master_x", master.getX());
			output.putInt("master_y", master.getY());
			output.putInt("master_z", master.getZ());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(Tuning.FIREBOX_SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		burnLeft = input.getDoubleOr("burn_left", 0);
		isMaster = input.getBooleanOr("is_master", false);
		master = input.getInt("master_x").isPresent()
				? new BlockPos(input.getIntOr("master_x", 0), input.getIntOr("master_y", 0), input.getIntOr("master_z", 0)) : null;
	}
}
