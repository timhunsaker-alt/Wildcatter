package dev.wildcatter.block;

import dev.wildcatter.Rules;
import dev.wildcatter.TimeScale;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
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
 * <p>
 * 17c1 (zirconium-extensions.md): the hafnium column's HfCl4 goes out to a hopper under it as drums of 50 kg of hafnium. Each stage
 * follows a hafnium campaign: it holds zirconium or hafnium, and takes the other only once empty; the VAR furnace melts hafnium
 * unalloyed. With the rule {@code zirconiumBatches}: the vacuum distillation takes the Kroll reactor's retorts (it drives them), the VAR
 * furnace melts 100 kg ingots twice, an hour a melt (through {@link TimeScale}), with up to 30 % Zircaloy scrap by hopper, and the
 * extrusion press loses 8 % of the metal as crop ends, which the pilger mill against it bins as scrap.
 */
public class ZrLineBlockEntity extends UnitMotorBlockEntity implements ZrStage, HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(4, ItemStack.EMPTY);
	private double metal;    // kg held for the next stage
	private boolean hf;      // the metal it holds is hafnium (a hafnium campaign)
	private double scrap;    // the extrusion press's crop ends, kg, for the pilger mill's bin; the VAR furnace's scrap on hand
	private double hfScrap;  // the extrusion press's hafnium crop ends, counted
	private double charge, chargeScrap;   // the VAR furnace's next ingot, kg, and the scrap in it
	private boolean chargeHf;
	private int melt;        // 0 charging, 1 and 2 the melts
	private double meltSeconds;
	private long distilled = -1000;   // when the Kroll reactor last ran a retort in it
	private double feed;     // the separator's heavy minerals, kg
	private double tin, iron, chromium;   // the VAR furnace's additions, kg
	private double ilmenite, rutile, monazite, hafnium;
	private double ilmeniteOut, rutileOut;   // 20b: kg toward the next 500 kg item, made only while a hopper stands under the separator
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
	public boolean hafnium() {
		return hf;
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
					|| slot == 2 && (stack.is(OreBlocks.MINERALS.get(Ore.CHROMITE)) || stack.is(WildcatterItems.CHROMIUM_METAL))
					|| slot == 3 && stack.is(WildcatterItems.ZIRCALOY_SCRAP);
			default -> false;
		};
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return switch (kind()) {
			case "mineral_separator" -> side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {1, 2} : new int[0];
			case "var_furnace" -> side == Direction.UP ? new int[] {0, 1, 2, 3} : new int[0];
			case "hafnium_column" -> side == Direction.DOWN ? new int[] {1} : new int[0];
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
		return side == Direction.DOWN && ((kind().equals("vacuum_anneal_furnace") || kind().equals("mineral_separator")) && slot > 0 && slot < 3
				|| kind().equals("hafnium_column") && slot == 1);
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
			case "hafnium_column" -> {
				pass(level, Touching.find(level, worldPosition, ChlorinatorBlockEntity.class, c -> true), "CHLORINATOR", 1 - Tuning.ZR_HF_FRACTION,
						"distilling the hafnium out");
				drums();
			}
			case "vacuum_distillation" -> {
				MagnesiumCellBlockEntity cell = Touching.find(level, worldPosition, MagnesiumCellBlockEntity.class, c -> true);
				if (cell == null) {
					unit = "NOT BUILT: set it against the MAGNESIUM CELL, which takes back the magnesium it boils off";
					return;
				}
				KrollReactorBlockEntity kroll = Touching.find(level, worldPosition, KrollReactorBlockEntity.class, k -> true);
				if (Rules.zirconiumBatches() && kroll != null && kroll.zirconium() < 1e-9) {
					if (level.getGameTime() - distilled > 40) {
						unit = "idle: no retort in it (the Kroll reactor sends one when its batch is reduced)";
					}
					return;   // the Kroll reactor runs its retorts in it
				}
				double before = metal;
				pass(level, kroll, "KROLL REACTOR", 1, "boiling off the magnesium");
				cell.addMg((metal - before) * (hf ? Zirconium.magnesiumPerHf() : Zirconium.magnesiumPerZr()) * (Tuning.KROLL_MG_EXCESS - 1));
			}
			case "var_furnace" -> {
				if (Rules.zirconiumBatches()) {
					meltIngots(level);
				} else {
					melt(level);
				}
			}
			case "extrusion_press" -> {
				double before = metal;
				double factor = Rules.zirconiumBatches() ? Zirconium.extrusionYield() : 1;
				pass(level, Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("var_furnace")), "VAR FURNACE", factor, "extruding hollows");
				double lost = (metal - before) / factor * (1 - factor);
				if (hf) {
					hfScrap += lost;
				} else {
					scrap += lost;
				}
			}
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
		if (metal > 1e-9 && up.hafnium() != hf) {
			unit = String.format("holding %.1f kg of %s: the next stage must take it before the %s behind it", metal, hf ? "hafnium" : "zirconium",
					hf ? "zirconium" : "hafnium");
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
		boolean upHf = up.hafnium();
		double kg = up.takeZirconium(Math.min(rate() * strength, room() / factor));
		metal += kg * factor;
		hf = upHf;
		hafnium += kind().equals("hafnium_column") ? kg * (1 - factor) : 0;
		unit = String.format("%s%s: %.1f kg/h; %.2f kg held", verb, hf ? " (HAFNIUM)" : "", kg * factor / seconds() * 3600, metal);
	}

	/** For the game test (20b): ilmenite as if separated toward the next item, with a hopper under it. */
	public void testIlmeniteOut(double kg) {
		ilmeniteOut = kg;
		setChanged();
	}

	/** kg of ilmenite the separator has counted, all told. */
	public double ilmeniteCounted() {
		return ilmenite;
	}

	/** 20b: a hopper under any cell of the separator (the part the player adds for the titanium plant). */
	private boolean hopperUnder(Level level) {
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 6; z++) {
				if (level.getBlockState(LeasePartBlock.at(worldPosition, getBlockState(), x, -1, z)).getBlock() instanceof net.minecraft.world.level.block.HopperBlock) {
					return true;
				}
			}
		}
		return false;
	}

	/** Whole 500 kg items of {@code item} into slot {@code slot} while it has room; returns the kg left toward the next. */
	private double bag(int slot, net.minecraft.world.item.Item item, double kg) {
		while (kg >= Tuning.TI_MINERAL_ITEM_KG) {
			ItemStack out = slots.get(slot);
			if (out.isEmpty()) {
				slots.set(slot, new ItemStack(item));
			} else if (out.is(item) && out.getCount() < out.getMaxStackSize()) {
				out.grow(1);
			} else {
				break;
			}
			kg -= Tuning.TI_MINERAL_ITEM_KG;
			setChanged();
		}
		return kg;
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
		if (hopperUnder(level)) {   // 20b: with a hopper under it, its ilmenite and rutile go out as items instead of only being counted
			ilmeniteOut += hm * Tuning.HM_ILMENITE;
			rutileOut += hm * Tuning.HM_RUTILE;
			ilmeniteOut = bag(1, dev.wildcatter.WildcatterItems.ILMENITE, ilmeniteOut);
			rutileOut = bag(2, dev.wildcatter.WildcatterItems.RUTILE, rutileOut);
		}
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
		if (up.hafnium() || hf && metal > 1e-9) {
			meltHafnium(level, up);
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
		if (chromium < want * Tuning.ZIRCALOY_CR) {
			chromium += chromiumFrom(useFrom(2));
		}
		String missing = tin <= 1e-9 ? "NO TIN: put tin ingots in its hopper" : iron <= 1e-9 ? "NO IRON: put iron ingots in its hopper"
				: chromium <= 1e-9 ? "NO CHROMIUM: put chromium metal or chromite in its hopper" : null;
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

	/** kg of chromium in an item from the VAR's chromium bin: a chromite block (the 7f stand-in) or a piece of chromium metal (17c2). */
	private static double chromiumFrom(ItemStack item) {
		return item.isEmpty() ? 0 : item.is(WildcatterItems.CHROMIUM_METAL) ? Tuning.CHROMIUM_PIECE_KG : Ore.CHROMITE.kgPerBlock() * 104.0 / 223.84;
	}

	/** The steady flow's VAR on a hafnium campaign: hafnium sponge melted unalloyed. */
	private void meltHafnium(Level level, ZrStage up) {
		if (metal > 1e-9 && !hf) {
			unit = "holding its Zircaloy-4 until the extrusion press takes it; hafnium sponge behind it";
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
		double out = up.takeZirconium(Math.min(rate() * strength, room()));
		metal += out;
		hf = true;
		unit = String.format("remelting hafnium unalloyed (HAFNIUM CAMPAIGN): %.1f kg/h", out / seconds() * 3600);
	}

	/** The VAR furnace's ingots (the rule zirconiumBatches): 100 kg charged, then melted twice, an hour a melt. */
	private void meltIngots(Level level) {
		ZrStage up = Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("vacuum_distillation"));
		if (up == null) {
			unit = "NOT BUILT: set it against the VACUUM DISTILLATION";
			return;
		}
		double ingot = Tuning.VAR_INGOT_KG;
		if (melt == 0) {
			if (!slots.get(3).isEmpty() && scrap < ingot * Tuning.VAR_SCRAP_SHARE) {
				useFrom(3);
				scrap += Tuning.ZR_SCRAP_KG;
			}
			if (charge < 1e-9) {
				chargeHf = up.zirconium() > 1e-9 && up.hafnium();
			}
			double want = Math.min(rate(), ingot - charge);
			String missing = null;
			if (!chargeHf && ingot - charge > 1e-9) {
				// clean scrap, up to 30 % of the ingot, welded into the electrode as it comes: it is already Zircaloy-4
				double s = Math.min(Math.min(scrap, ingot * Tuning.VAR_SCRAP_SHARE - chargeScrap), ingot - charge);
				if (s > 1e-9) {
					scrap -= s;
					chargeScrap += s;
					charge += s;
					want = Math.min(want, ingot - charge);
				}
				double alloy = 1 - Tuning.ZIRCALOY_SN - Tuning.ZIRCALOY_FE - Tuning.ZIRCALOY_CR;
				if (want > 1e-9 && up.zirconium() > 1e-9 && !up.hafnium()) {
					if (tin < want * Tuning.ZIRCALOY_SN && !useFrom(0).isEmpty()) {
						tin += Ore.CASSITERITE.kgPerBlock() * 118.71 / 150.71;
					}
					if (iron < want * Tuning.ZIRCALOY_FE && !useFrom(1).isEmpty()) {
						iron += Tuning.IRON_INGOT_KG;
					}
					if (chromium < want * Tuning.ZIRCALOY_CR) {
						chromium += chromiumFrom(useFrom(2));
					}
					missing = tin <= 1e-9 ? "NO TIN: put tin ingots in its hopper" : iron <= 1e-9 ? "NO IRON: put iron ingots in its hopper"
							: chromium <= 1e-9 ? "NO CHROMIUM: put chromium metal or chromite in its hopper" : null;
					if (missing == null) {
						double out = Math.min(want, Math.min(Math.min(tin / Tuning.ZIRCALOY_SN, iron / Tuning.ZIRCALOY_FE), chromium / Tuning.ZIRCALOY_CR));
						out = up.takeZirconium(out * alloy) / alloy;
						tin -= out * Tuning.ZIRCALOY_SN;
						iron -= out * Tuning.ZIRCALOY_FE;
						chromium -= out * Tuning.ZIRCALOY_CR;
						charge += out;
					}
				}
			} else if (want > 1e-9 && up.zirconium() > 1e-9 && up.hafnium()) {
				charge += up.takeZirconium(want);
			}
			if (charge >= ingot - 1e-6) {
				melt = 1;
				meltSeconds = 0;
				setChanged();
			} else {
				unit = missing != null ? missing : String.format("charging the next ingot for remelting into %s: %.1f of %.0f kg (%.1f kg of it scrap)",
						chargeHf ? "hafnium (HAFNIUM CAMPAIGN)" : "Zircaloy-4", charge, ingot, chargeScrap);
				return;
			}
		}
		double hold = TimeScale.scaled(Tuning.VAR_MELT_S);
		if (melt <= Tuning.VAR_MELTS) {
			double strength = drive(level);
			if (strength <= 0) {
				unit = "starting: " + describe();
				return;
			}
			meltSeconds += seconds() * strength;
			if (meltSeconds >= hold - 1e-6) {
				melt++;
				meltSeconds = 0;
				setChanged();
			}
			if (melt <= Tuning.VAR_MELTS) {
				unit = String.format("remelting a %.0f kg %s ingot, melt %d of %d: %.0f %% of its hour%s", ingot, chargeHf ? "hafnium" : "Zircaloy-4", melt, Tuning.VAR_MELTS,
						meltSeconds / hold * 100, TimeScale.note(Tuning.VAR_MELT_S));
				return;
			}
		}
		if (metal > 1e-9 && (hf != chargeHf || metal + charge > ingot + 1e-6)) {
			unit = String.format("FULL: the %.0f kg ingot is melted; the extrusion press has not taken the last one", charge);
			return;
		}
		metal += charge;
		hf = chargeHf;
		charge = 0;
		chargeScrap = 0;
		melt = 0;
		setChanged();
		unit = String.format("an ingot of %s melted twice, %.0f kg held for the extrusion press", hf ? "hafnium" : "Zircaloy-4", metal);
	}

	/** The hafnium column's drums: each 50 kg of hafnium it has taken out goes out to a hopper under it as a HfCl4 drum. */
	private void drums() {
		while (hafnium >= Tuning.HFCL4_DRUM_HF_KG - 1e-3) {   // to the drum's gram
			ItemStack out = slots.get(1);
			if (out.isEmpty()) {
				slots.set(1, new ItemStack(WildcatterItems.HFCL4_DRUM));
			} else if (out.is(WildcatterItems.HFCL4_DRUM) && out.getCount() < out.getMaxStackSize()) {
				out.grow(1);
			} else {
				return;   // the drums wait for a hopper; the rest is counted
			}
			hafnium -= Tuning.HFCL4_DRUM_HF_KG;
			setChanged();
		}
	}

	public int drumsOut() {
		return kind().equals("hafnium_column") && slots.get(1).is(WildcatterItems.HFCL4_DRUM) ? slots.get(1).getCount() : 0;
	}

	/** For the game test: the column's hafnium brought along by {@code kg}. */
	public void addHafniumForTest(double kg) {
		hafnium += kg;
		setChanged();
	}

	/** The Kroll reactor runs a retort in the vacuum distillation: the furnace's strength, 0 to 1. */
	public double distillRetort(Level level, String what) {
		distilled = level.getGameTime();
		double strength = drive(level);
		unit = strength > 0 ? what : "starting: " + describe();
		return strength;
	}

	/** A distilled retort's sponge into its hold; false if it holds other metal or has no room. */
	public boolean receiveBatch(double kg, boolean hafnium) {
		if (metal > 1e-9 && (hafnium != hf || metal + kg > Math.max(Tuning.ZR_STAGE_HOLD_KG, kg) + 1e-6)) {
			return false;
		}
		metal += kg;
		hf = hafnium;
		setChanged();
		return true;
	}

	/** The extrusion press's crop ends, for the pilger mill against it to bin. */
	public double takeScrap() {
		double got = scrap;
		scrap = 0;
		if (got > 0) {
			setChanged();
		}
		return got;
	}

	public double charge() {
		return charge;
	}

	public int meltStep() {
		return melt;
	}

	public double chargeScrap() {
		return chargeScrap;
	}

	/** For the game test: the VAR furnace's next ingot charged and its melts all but done. */
	public void finishIngotForTest() {
		if (melt == 0) {
			charge = Tuning.VAR_INGOT_KG;
			melt = 1;
		}
		melt = Tuning.VAR_MELTS;
		meltSeconds = TimeScale.scaled(Tuning.VAR_MELT_S) - 1;
		setChanged();
	}

	/** For the game test: metal of either kind put straight into it. */
	public void addMetalForTest(double kg, boolean hafnium) {
		metal = Math.min(Math.max(Tuning.ZR_STAGE_HOLD_KG, Tuning.VAR_INGOT_KG), metal + kg);
		hf = hafnium;
		setChanged();
	}

	@Override
	public Component inspect() {
		String label = switch (kind()) {
			case "mineral_separator" -> String.format("Mineral separator: %s; ilmenite %,.0f kg, rutile %,.0f kg, monazite %,.0f kg", unit, ilmenite, rutile, monazite);
			case "hafnium_column" -> String.format("Hafnium column (KAlCl4 extractive distillation): %s; HfCl4 %.1f kg of hafnium%s", unit, Math.max(0, hafnium),
					drumsOut() > 0 ? String.format(", %d drums for a hopper under it", drumsOut()) : "");
			case "vacuum_distillation" -> "Vacuum distillation: " + unit;
			case "var_furnace" -> "VAR furnace: " + unit + (scrap > 1e-6 || !slots.get(3).isEmpty() ? String.format("; scrap %.0f kg and %d bundles on hand", scrap, slots.get(3).getCount()) : "");
			case "extrusion_press" -> "Extrusion press: " + unit + (hfScrap > 1e-6 ? String.format("; hafnium crop ends %.1f kg", hfScrap) : "");
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
		output.putDouble("ilmeniteOut", ilmeniteOut);
		output.putDouble("rutileOut", rutileOut);
		output.putDouble("monazite", monazite);
		output.putDouble("hafnium", hafnium);
		output.putBoolean("hf", hf);
		output.putDouble("scrap", scrap);
		output.putDouble("hfScrap", hfScrap);
		output.putDouble("charge", charge);
		output.putDouble("chargeScrap", chargeScrap);
		output.putBoolean("chargeHf", chargeHf);
		output.putInt("melt", melt);
		output.putDouble("meltS", meltSeconds);
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
		ilmeniteOut = input.getDoubleOr("ilmeniteOut", 0);
		rutileOut = input.getDoubleOr("rutileOut", 0);
		monazite = input.getDoubleOr("monazite", 0);
		hafnium = input.getDoubleOr("hafnium", 0);
		hf = input.getBooleanOr("hf", false);
		scrap = input.getDoubleOr("scrap", 0);
		hfScrap = input.getDoubleOr("hfScrap", 0);
		charge = input.getDoubleOr("charge", 0);
		chargeScrap = input.getDoubleOr("chargeScrap", 0);
		chargeHf = input.getBooleanOr("chargeHf", false);
		melt = input.getIntOr("melt", 0);
		meltSeconds = input.getDoubleOr("meltS", 0);
	}
}
