package dev.wildcatter.block;

import dev.wildcatter.Rules;
import dev.wildcatter.TimeScale;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Zirconium;
import net.minecraft.core.BlockPos;
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
 * The Kroll reactor (docs/multiblocks/zirconium.md): a sealed retort in its electric furnace (100 kW, 480 V) at about 850 C. ZrCl4
 * from the hafnium column it touches is reduced on molten magnesium from the magnesium cell it touches, charged 25 % over the two Mg
 * a Zr: ZrCl4 + 2Mg -> Zr + 2MgCl2. The MgCl2 is tapped back to the cell; the sponge, with the excess Mg in it, waits for the vacuum
 * distillation.
 * <p>
 * 17c1 (zirconium-extensions.md items 3 and 7): with the rule {@code zirconiumBatches} it holds <b>two retorts that alternate</b>: one
 * in the furnace is charged with 50 kg as the ZrCl4 comes and reduced for 5 hours; it then goes to the vacuum distillation it touches
 * for 4 hours while the other is charged, and comes back empty. Both waits go through {@link TimeScale}. Without the rule the line
 * runs as a steady flow, as before; with it, sponge already held from the steady flow is kept and goes on to the distillation.
 * A sneak-click starts or ends a <b>hafnium campaign</b>: HfCl4 drums by a hopper on top in place of the column's ZrCl4.
 */
public class KrollReactorBlockEntity extends UnitMotorBlockEntity implements ZrStage, HopperFed {
	public static final int CHARGING = 0, REDUCED = 1, DISTILLING = 2, EMPTY = 3;

	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double metal;   // sponge from the steady flow, kg
	private boolean metalHf;
	private boolean campaign;   // HfCl4 drums in place of the column's ZrCl4
	private double hfFeed;      // hafnium in the opened drum, kg
	private final double[] retortKg = new double[2];
	private final int[] retortStep = {CHARGING, EMPTY};
	private final double[] retortSeconds = new double[2];
	private final boolean[] retortHf = new boolean[2];
	private int furnace;        // the retort in the furnace
	private String unit = "starting";

	public KrollReactorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.KROLL_REACTOR_ENTITY, pos, state);
	}

	// ---- the sponge from the steady flow ------------------------------------------------------------------------------------------
	@Override
	public double zirconium() {
		return metal;
	}

	@Override
	public boolean hafnium() {
		return metalHf;
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

	// ---- the hafnium campaign's drums -----------------------------------------------------------------------------------------------
	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.HFCL4_DRUM);
	}

	/** A sneak-click: the hafnium campaign on or off. */
	public String toggleCampaign() {
		campaign = !campaign;
		setChanged();
		return campaign ? "Kroll reactor: HAFNIUM CAMPAIGN: it takes HfCl4 drums by a hopper on top; the line after it follows the hafnium"
				: "Kroll reactor: back on zirconium from the hafnium column";
	}

	public boolean campaign() {
		return campaign;
	}

	/** Where its tetrachloride comes from: the column's ZrCl4, or the campaign's drums. Null with neither. */
	private interface Feed {
		double available();

		double take(double kg);
	}

	private @Nullable Feed feed(@Nullable ZrStage column, boolean hf) {
		if (hf) {
			if (hfFeed < 1e-6 && !useOne().isEmpty()) {
				hfFeed += Tuning.HFCL4_DRUM_HF_KG;
			}
			return new Feed() {
				@Override
				public double available() {
					return hfFeed;
				}

				@Override
				public double take(double kg) {
					double got = Math.max(0, Math.min(kg, hfFeed));
					hfFeed -= got;
					return got;
				}
			};
		}
		return column == null ? null : new Feed() {
			@Override
			public double available() {
				return column.zirconium();
			}

			@Override
			public double take(double kg) {
				return column.takeZirconium(kg);
			}
		};
	}

	// ---- the retorts, for the readout and the game test ----------------------------------------------------------------------------
	public int retortStep(int i) {
		return retortStep[i];
	}

	public double retortKg(int i) {
		return retortKg[i];
	}

	public int furnace() {
		return furnace;
	}

	/** For the game test: the retort in the furnace charged with a whole batch of zirconium, its reduction all but done. */
	public void chargeBatchForTest() {
		retortKg[furnace] = Tuning.KROLL_BATCH_KG;
		retortHf[furnace] = false;
		retortStep[furnace] = CHARGING;
		retortSeconds[furnace] = TimeScale.scaled(Tuning.KROLL_REDUCTION_S) - 1;
		setChanged();
	}

	/** For the game test: the retort in the distillation all but done. */
	public void finishDistillationForTest() {
		for (int i = 0; i < 2; i++) {
			if (retortStep[i] == DISTILLING) {
				retortSeconds[i] = TimeScale.scaled(Tuning.KROLL_DISTILLATION_S) - 1;
			}
		}
		setChanged();
	}

	private static String name(int i) {
		return i == 0 ? "A" : "B";
	}

	private String metalName(boolean hf) {
		return hf ? "hafnium" : "zirconium";
	}

	// ---- its work -------------------------------------------------------------------------------------------------------------------
	@Override
	protected void work(Level level) {
		ZrStage column = Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("hafnium_column"));
		MagnesiumCellBlockEntity cell = Touching.find(level, worldPosition, MagnesiumCellBlockEntity.class, c -> true);
		if (column == null && !campaign) {
			unit = "NOT BUILT: set it against the HAFNIUM COLUMN";
			return;
		}
		if (cell == null) {
			unit = "NOT BUILT: set it against the MAGNESIUM CELL";
			return;
		}
		if (Rules.zirconiumBatches()) {
			batches(level, column, cell);
			return;
		}
		if (metal > 1e-9 && metalHf != campaign) {
			unit = "holding its " + metalName(metalHf) + " sponge until the vacuum distillation takes it";
			return;
		}
		Feed up = feed(column, campaign);
		if (up == null || up.available() < 1e-9) {
			unit = campaign ? "NO HfCl4: put hafnium tetrachloride drums in a hopper on top" : "waiting for ZrCl4 from the hafnium column";
			return;
		}
		if (cell.mg() < 1e-6) {
			unit = "NO MAGNESIUM: the magnesium cell has none yet";
			return;
		}
		double room = Tuning.ZR_STAGE_HOLD_KG - metal;
		if (room <= 1e-9 || cell.mgCl2Room() <= 1e-6) {
			unit = room <= 1e-9 ? "FULL: the vacuum distillation is not taking its sponge" : "FULL: the magnesium cell has no room for its MgCl2";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the furnace: " + describe();
			return;
		}
		double kg = reduce(up, cell, campaign, Math.min(Tuning.ZR_LINE_KGS * 1.2 * seconds() * strength, room));
		metal += kg;
		metalHf = campaign;
		unit = String.format("reducing %s at about 850 C: %.1f kg/h of sponge; %.2f kg of sponge held", campaign ? "HfCl4 (HAFNIUM CAMPAIGN)" : "ZrCl4",
				kg / seconds() * 3600, metal);
	}

	/** Reduces up to {@code want} kg of metal from {@code up} on the cell's magnesium, tapping the MgCl2 back; returns the kg. */
	private double reduce(Feed up, MagnesiumCellBlockEntity cell, boolean hf, double want) {
		double mgPer = hf ? Zirconium.magnesiumPerHf() : Zirconium.magnesiumPerZr();
		double charge = mgPer * Tuning.KROLL_MG_EXCESS;   // Mg charged a kg of metal
		double mgCl2Per = hf ? Zirconium.mgCl2PerHf() : Zirconium.mgCl2PerZr();
		double kg = Math.min(want, Math.min(cell.mg() / charge, cell.mgCl2Room() / mgCl2Per));
		kg = up.take(Math.max(0, kg));
		cell.takeMg(kg * charge);
		cell.addMgCl2(kg * mgCl2Per);
		return kg;
	}

	/** The two retorts (the rule zirconiumBatches). */
	private void batches(Level level, @Nullable ZrStage column, MagnesiumCellBlockEntity cell) {
		ZrLineBlockEntity still = Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("vacuum_distillation"));
		String other = distill(level, still, cell);
		int f = furnace;
		String inFurnace;
		if (retortStep[f] == REDUCED) {
			int o = 1 - f;
			if (retortStep[o] == EMPTY && still != null) {
				retortStep[f] = DISTILLING;
				retortSeconds[f] = 0;
				retortStep[o] = CHARGING;
				retortKg[o] = 0;
				retortSeconds[o] = 0;
				furnace = o;
				setChanged();
				inFurnace = String.format("retort %s out to the vacuum distillation; retort %s in", name(f), name(o));
			} else {
				inFurnace = String.format("retort %s REDUCED (%.0f kg of %s sponge): cooling, waiting for the vacuum distillation", name(f), retortKg[f],
						metalName(retortHf[f]));
			}
		} else {
			inFurnace = charge(level, column, cell, f);
		}
		unit = inFurnace + "; " + other;
	}

	/** The retort in the furnace: charged as the tetrachloride comes, reduced for 5 hours. Returns its readout. */
	private String charge(Level level, @Nullable ZrStage column, MagnesiumCellBlockEntity cell, int f) {
		boolean hf = retortKg[f] > 1e-9 ? retortHf[f] : campaign;
		double need = Tuning.KROLL_BATCH_KG - retortKg[f];
		double hold = TimeScale.scaled(Tuning.KROLL_REDUCTION_S);
		String r = "retort " + name(f);
		if (need > 1e-9) {
			Feed up = feed(column, hf);
			if (up == null || up.available() < 1e-9) {
				return r + (hf ? ": NO HfCl4: put hafnium tetrachloride drums in a hopper on top" : String.format(": waiting for ZrCl4 from the hafnium column (%.1f of %.0f kg charged)",
						retortKg[f], Tuning.KROLL_BATCH_KG));
			}
			if (cell.mg() < 1e-6) {
				return r + ": NO MAGNESIUM: the magnesium cell has none yet";
			}
			if (cell.mgCl2Room() <= 1e-6) {
				return r + ": FULL: the magnesium cell has no room for its MgCl2";
			}
		}
		double strength = drive(level);
		if (strength <= 0) {
			return r + ": starting the furnace: " + describe();
		}
		if (need > 1e-9) {
			Feed up = feed(column, hf);
			double kg = reduce(up, cell, hf, Math.min(Tuning.ZR_LINE_KGS * 1.2 * seconds() * strength, need));
			retortKg[f] += kg;
			retortHf[f] = hf;
		}
		if (retortKg[f] > 1e-9) {
			retortSeconds[f] += seconds() * strength;
		}
		if (retortKg[f] >= Tuning.KROLL_BATCH_KG - 1e-6 && retortSeconds[f] >= hold - 1e-6) {
			retortStep[f] = REDUCED;
			setChanged();
			return String.format("%s REDUCED: %.0f kg of %s sponge for the vacuum distillation", r, retortKg[f], metalName(hf));
		}
		return String.format("%s reducing %s at about 850 C: %.1f of %.0f kg charged, %.0f %% of its %s", r, hf ? "HfCl4 (HAFNIUM CAMPAIGN)" : "ZrCl4", retortKg[f],
				Tuning.KROLL_BATCH_KG, Math.min(100, retortSeconds[f] / hold * 100), (hours(Tuning.KROLL_REDUCTION_S) + TimeScale.note(Tuning.KROLL_REDUCTION_S)));
	}

	/** The retort out at the vacuum distillation: 4 hours, then its sponge into the distillation's hold. Returns its readout. */
	private String distill(Level level, @Nullable ZrLineBlockEntity still, MagnesiumCellBlockEntity cell) {
		for (int i = 0; i < 2; i++) {
			if (retortStep[i] != DISTILLING) {
				continue;
			}
			String r = "retort " + name(i);
			if (still == null) {
				return r + ": NOT BUILT: set the VACUUM DISTILLATION against it";
			}
			double hold = TimeScale.scaled(Tuning.KROLL_DISTILLATION_S);
			if (retortSeconds[i] < hold - 1e-6) {
				double strength = still.distillRetort(level, String.format("distilling retort %s's %.0f kg of %s sponge at 920-980 C, %.0f %% of its %s", r.substring(7), retortKg[i],
						metalName(retortHf[i]), retortSeconds[i] / hold * 100, (hours(Tuning.KROLL_DISTILLATION_S) + TimeScale.note(Tuning.KROLL_DISTILLATION_S))));
				retortSeconds[i] += seconds() * strength;
				return String.format("%s in the vacuum distillation: %.0f %% of its %s", r, retortSeconds[i] / hold * 100, (hours(Tuning.KROLL_DISTILLATION_S) + TimeScale.note(Tuning.KROLL_DISTILLATION_S)));
			}
			if (!still.receiveBatch(retortKg[i], retortHf[i])) {
				still.distillRetort(level, "FULL: retort " + name(i) + " is distilled; the VAR furnace has not taken the last batch");
				return r + " distilled, waiting for room in the vacuum distillation";
			}
			double mgPer = retortHf[i] ? Zirconium.magnesiumPerHf() : Zirconium.magnesiumPerZr();
			cell.addMg(retortKg[i] * mgPer * (Tuning.KROLL_MG_EXCESS - 1));
			retortKg[i] = 0;
			retortSeconds[i] = 0;
			retortStep[i] = EMPTY;
			setChanged();
			return r + " emptied: its sponge is in the vacuum distillation";
		}
		int o = 1 - furnace;
		return "retort " + name(o) + (retortStep[o] == EMPTY ? " empty, waiting for the furnace" : " waiting");
	}

	private static String hours(double seconds) {
		return seconds >= 3600 ? String.format("%.1f h", seconds / 3600) : String.format("%.0f min", seconds / 60);
	}

	@Override
	public Component inspect() {
		String drums = campaign ? String.format("; HAFNIUM CAMPAIGN: %d HfCl4 drums, %.0f kg of Hf open", fed(), hfFeed) : "";
		String held = metal > 1e-9 ? String.format("; %.1f kg of %s sponge from the steady flow", metal, metalName(metalHf)) : "";
		return Component.literal("Kroll reactor: " + unit + held + drums + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("metal", metal);
		output.putBoolean("metalHf", metalHf);
		output.putBoolean("campaign", campaign);
		output.putDouble("hfFeed", hfFeed);
		output.putInt("furnace", furnace);
		for (int i = 0; i < 2; i++) {
			output.putDouble("retortKg" + i, retortKg[i]);
			output.putInt("retortStep" + i, retortStep[i]);
			output.putDouble("retortS" + i, retortSeconds[i]);
			output.putBoolean("retortHf" + i, retortHf[i]);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		metal = input.getDoubleOr("metal", 0);
		metalHf = input.getBooleanOr("metalHf", false);
		campaign = input.getBooleanOr("campaign", false);
		hfFeed = input.getDoubleOr("hfFeed", 0);
		furnace = input.getIntOr("furnace", 0);
		for (int i = 0; i < 2; i++) {
			retortKg[i] = input.getDoubleOr("retortKg" + i, 0);
			retortStep[i] = input.getIntOr("retortStep" + i, i == 0 ? CHARGING : EMPTY);
			retortSeconds[i] = input.getDoubleOr("retortS" + i, 0);
			retortHf[i] = input.getBooleanOr("retortHf" + i, false);
		}
	}
}
