package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Zirconium;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A powered stage of the zirconium line (docs/multiblocks/zirconium.md), each against the one before it: the mineral separator
 * (heavy minerals by hopper), the hafnium column, the vacuum distillation (excess Mg back to the magnesium cell), the VAR furnace
 * (tin, iron and chromite by hopper), the extrusion press (4.16 kV), and the vacuum anneal furnace, which the pilger mill runs and
 * whose finished tubes go out to a hopper under it.
 */
public class ZrLineBlockEntity extends UnitMotorBlockEntity implements ZrStage, HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(3, ItemStack.EMPTY);
	private double metal;    // kg held for the next stage
	private double feed;     // the separator's heavy minerals, kg
	private double tin, iron, chromium;   // the VAR furnace's additions, kg
	private double ilmenite, rutile, monazite, hafnium;
	private String unit = "starting";

	public ZrLineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ZR_LINE_ENTITY, pos, state);
	}

	// ---- the metal it holds ---------------------------------------------------------------------------------------------------
	@Override
	public double zirconium() {
		return metal;
	}

	@Override
	public double takeZirconium(double kg) {
		double got = Math.max(0, Math.min(kg, metal));
		metal -= got;
		setChanged();
		return got;
	}

	@Override
	public void addZirconium(double kg) {
		metal = Math.min(Tuning.ZR_STAGE_HOLD_KG, metal + kg);
		setChanged();
	}

	// ---- hoppers --------------------------------------------------------------------------------------------------------------
	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return kind().equals("mineral_separator") && stack.is(OreBlocks.MINERALS.get(Ore.HEAVY_MINERAL));
	}

	private boolean takes(int slot, ItemStack stack) {
		return switch (kind()) {
			case "mineral_separator" -> slot == 0 && takes(stack);
			case "var_furnace" -> slot == 0 && stack.is(OreBlocks.INGOTS.get("tin")) || slot == 1 && stack.is(Items.IRON_INGOT)
					|| slot == 2 && stack.is(OreBlocks.MINERALS.get(Ore.CHROMITE));
			default -> false;
		};
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return switch (kind()) {
			case "mineral_separator" -> side == Direction.UP ? new int[] {0} : new int[0];
			case "var_furnace" -> side == Direction.UP ? new int[] {0, 1, 2} : new int[0];
			case "vacuum_anneal_furnace" -> side == Direction.DOWN ? new int[] {1, 2} : new int[0];
			default -> new int[0];
		};
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takes(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takes(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return kind().equals("vacuum_anneal_furnace") && slot > 0 && side == Direction.DOWN;
	}

	private ItemStack useFrom(int slot) {
		ItemStack stack = slots.get(slot);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack one = stack.split(1);
		setChanged();
		return one;
	}

	// ---- the anneal furnace's finished tubes ----------------------------------------------------------------------------------
	/** A finished tube from the pilger mill into its out bins; false if they are full. */
	public boolean deliver(ItemStack tube) {
		for (int i = 1; i <= 2; i++) {
			ItemStack in = slots.get(i);
			if (in.isEmpty()) {
				slots.set(i, tube);
				setChanged();
				return true;
			}
			if (ItemStack.isSameItemSameComponents(in, tube) && in.getCount() < in.getMaxStackSize()) {
				in.grow(1);
				setChanged();
				return true;
			}
		}
		return false;
	}

	public int finished() {
		return slots.get(1).getCount() + slots.get(2).getCount();
	}

	public void giveAll(Player player) {
		for (int i = 1; i <= 2; i++) {
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

	/** The pilger mill runs its anneals; returns how hard the furnace is running. */
	public double anneal(Level level, String what) {
		unit = what;
		return drive(level);
	}

	// ---- its work -------------------------------------------------------------------------------------------------------------
	private double rate() {
		return Tuning.ZR_LINE_KGS * 1.2 * seconds();
	}

	private double room() {
		return Math.max(0, Tuning.ZR_STAGE_HOLD_KG - metal);
	}

	@Override
	protected void work(Level level) {
		switch (kind()) {
			case "mineral_separator" -> separate(level);
			case "hafnium_column" -> pass(level, Touching.find(level, worldPosition, ChlorinatorBlockEntity.class, c -> true), "CHLORINATOR", 1 - Tuning.ZR_HF_FRACTION,
					"distilling the hafnium out");
			case "vacuum_distillation" -> {
				MagnesiumCellBlockEntity cell = Touching.find(level, worldPosition, MagnesiumCellBlockEntity.class, c -> true);
				if (cell == null) {
					unit = "NOT BUILT: set it against the MAGNESIUM CELL, which takes back the magnesium it boils off";
					return;
				}
				double before = metal;
				pass(level, Touching.find(level, worldPosition, KrollReactorBlockEntity.class, k -> true), "KROLL REACTOR", 1, "boiling off the magnesium");
				cell.addMg((metal - before) * Zirconium.magnesiumPerZr() * (Tuning.KROLL_MG_EXCESS - 1));
			}
			case "var_furnace" -> melt(level);
			case "extrusion_press" -> pass(level, Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("var_furnace")), "VAR FURNACE", 1, "extruding hollows");
			default -> {
			}
		}
	}

	/** Takes metal from {@code up} into its own hold, {@code factor} of it staying in the metal. */
	private void pass(Level level, @Nullable ZrStage up, String upName, double factor, String verb) {
		if (up == null) {
			unit = "NOT BUILT: set it against the " + upName;
			return;
		}
		if (up.zirconium() < 1e-9) {
			unit = "waiting for metal from the " + upName.toLowerCase();
			return;
		}
		if (room() <= 1e-9) {
			unit = "FULL: the next stage is not taking its metal";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = up.takeZirconium(Math.min(rate() * strength, room() / factor));
		metal += kg * factor;
		hafnium += kind().equals("hafnium_column") ? kg * (1 - factor) : 0;
		unit = String.format("%s: %.1f kg/h; %.2f kg held", verb, kg * factor / seconds() * 3600, metal);
	}

	private void separate(Level level) {
		if (feed <= 1e-6 && fed() > 0) {
			useOne();
			feed += Ore.HEAVY_MINERAL.kgPerBlock();
		}
		if (feed <= 1e-6) {
			unit = "NO HEAVY MINERALS: put a hopper of heavy mineral sand on top";
			return;
		}
		if (room() <= 1e-9) {
			unit = "FULL: the chlorinator is not taking its zircon";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double perHm = Tuning.HM_ZIRCON * Tuning.ZIRCON_RECOVERY * Tuning.ZR_IN_ZIRCON;   // kg of Zr a kg of heavy minerals
		double zr = Math.min(Math.min(rate() * strength, room()), feed * perHm);
		double hm = zr / perHm;
		feed -= hm;
		metal += zr;
		ilmenite += hm * Tuning.HM_ILMENITE;
		rutile += hm * Tuning.HM_RUTILE;
		monazite += hm * Tuning.HM_MONAZITE;
		unit = String.format("separating %.0f kg/h of heavy minerals: %.1f kg/h of zircon on; %,.0f kg left in the bin", hm / seconds() * 3600,
				zr * Zirconium.zirconPerZr() / seconds() * 3600, feed);
	}

	private void melt(Level level) {
		ZrStage up = Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("vacuum_distillation"));
		if (up == null) {
			unit = "NOT BUILT: set it against the VACUUM DISTILLATION";
			return;
		}
		if (up.zirconium() < 1e-9) {
			unit = "waiting for sponge from the vacuum distillation";
			return;
		}
		double alloy = 1 - Tuning.ZIRCALOY_SN - Tuning.ZIRCALOY_FE - Tuning.ZIRCALOY_CR;   // Zr in a kg of Zircaloy-4
		double want = Math.min(rate(), room());
		if (tin < want * Tuning.ZIRCALOY_SN && !useFrom(0).isEmpty()) {
			tin += Ore.CASSITERITE.kgPerBlock() * 118.71 / 150.71;
		}
		if (iron < want * Tuning.ZIRCALOY_FE && !useFrom(1).isEmpty()) {
			iron += Tuning.IRON_INGOT_KG;
		}
		if (chromium < want * Tuning.ZIRCALOY_CR && !useFrom(2).isEmpty()) {
			chromium += Ore.CHROMITE.kgPerBlock() * 104.0 / 223.84;
		}
		String missing = tin <= 1e-9 ? "NO TIN: put tin ingots in its hopper" : iron <= 1e-9 ? "NO IRON: put iron ingots in its hopper"
				: chromium <= 1e-9 ? "NO CHROMIUM: put chromite in its hopper" : null;
		if (missing != null) {
			unit = missing;
			return;
		}
		if (room() <= 1e-9) {
			unit = "FULL: the extrusion press is not taking its ingot";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double out = Math.min(want * strength, Math.min(Math.min(tin / Tuning.ZIRCALOY_SN, iron / Tuning.ZIRCALOY_FE), chromium / Tuning.ZIRCALOY_CR));
		double zr = up.takeZirconium(out * alloy);
		out = zr / alloy;
		tin -= out * Tuning.ZIRCALOY_SN;
		iron -= out * Tuning.ZIRCALOY_FE;
		chromium -= out * Tuning.ZIRCALOY_CR;
		metal += out;
		unit = String.format("remelting into Zircaloy-4 (%.1f %% Sn, %.2f %% Fe, %.2f %% Cr): %.1f kg/h; tin %.0f kg, iron %.0f kg, chromium %.0f kg on hand",
				Tuning.ZIRCALOY_SN * 100, Tuning.ZIRCALOY_FE * 100, Tuning.ZIRCALOY_CR * 100, out / seconds() * 3600, tin, iron, chromium);
	}

	@Override
	public Component inspect() {
		String label = switch (kind()) {
			case "mineral_separator" -> String.format("Mineral separator: %s; ilmenite %,.0f kg, rutile %,.0f kg, monazite %,.0f kg", unit, ilmenite, rutile, monazite);
			case "hafnium_column" -> String.format("Hafnium column (KAlCl4 extractive distillation): %s; HfCl4 %.1f kg of hafnium", unit, hafnium);
			case "vacuum_distillation" -> "Vacuum distillation: " + unit;
			case "var_furnace" -> "VAR furnace: " + unit;
			case "extrusion_press" -> "Extrusion press: " + unit;
			case "vacuum_anneal_furnace" -> String.format("Vacuum anneal furnace: %s; %d finished", unit, finished());
			default -> kind();
		};
		return Component.literal(label + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("metal", metal);
		output.putDouble("feed", feed);
		output.putDouble("tin", tin);
		output.putDouble("iron", iron);
		output.putDouble("chromium", chromium);
		output.putDouble("ilmenite", ilmenite);
		output.putDouble("rutile", rutile);
		output.putDouble("monazite", monazite);
		output.putDouble("hafnium", hafnium);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		metal = input.getDoubleOr("metal", 0);
		feed = input.getDoubleOr("feed", 0);
		tin = input.getDoubleOr("tin", 0);
		iron = input.getDoubleOr("iron", 0);
		chromium = input.getDoubleOr("chromium", 0);
		ilmenite = input.getDoubleOr("ilmenite", 0);
		rutile = input.getDoubleOr("rutile", 0);
		monazite = input.getDoubleOr("monazite", 0);
		hafnium = input.getDoubleOr("hafnium", 0);
	}
}
