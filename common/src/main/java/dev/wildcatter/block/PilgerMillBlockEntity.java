package dev.wildcatter.block;

import dev.wildcatter.Rules;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Zirconium;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The cold pilger mill (docs/multiblocks/zirconium.md): four passes from the extrusion press's hollows down to the tube, with the
 * vacuum anneal furnace it touches annealing between them (75 kW, 480 V). Its dies make cladding tube (9.50 x 0.57 mm) or guide
 * thimble (12.24 x 0.41 mm), or the strip for a spacer grid (fuel.md); sneak-click to change them, which scraps the tube on the mill. Finished tubes go to the furnace's bins.
 * <p>
 * 17c1 (zirconium-extensions.md items 3 and 6): a fourth die rolls hafnium rodlets (9.7 mm, 3.66 m, solid) on a hafnium campaign. With
 * the rule {@code zirconiumBatches} the mill loses 6 % of the metal and inspection 2 %; with the press's 8 % crop ends it bins them as
 * 25 kg bundles of Zircaloy scrap, out to a hopper under it, for the VAR furnace. Hafnium scrap is counted.
 */
public class PilgerMillBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double bin;        // Zircaloy scrap not yet bundled, kg
	private double hfScrap;    // hafnium scrap, counted
	private int die;           // 0 cladding tube, 1 guide thimble, 2 grid strip, 3 hafnium rodlet
	private double progress;   // kg of the tube on the mill
	private double scrap;
	private String unit = "starting";

	public PilgerMillBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PILGER_MILL_ENTITY, pos, state);
	}

	private double tubeKg() {
		return die == 1 ? Zirconium.thimbleKg() : die == 2 ? Tuning.SPACER_GRID_KG : die == 3 ? Zirconium.hafniumRodletKg() : Zirconium.claddingKg();
	}

	// ---- the scrap bin, out to a hopper under it -------------------------------------------------------------------------------------
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
		return side == Direction.DOWN ? new int[] {0} : new int[0];
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	private void scrap(double kg, boolean hafnium) {
		if (hafnium) {
			hfScrap += kg;
			return;
		}
		bin += kg;
		while (bin >= Tuning.ZR_SCRAP_KG - 1e-9) {
			ItemStack out = slots.get(0);
			if (out.isEmpty()) {
				slots.set(0, new ItemStack(WildcatterItems.ZIRCALOY_SCRAP));
			} else if (out.getCount() < out.getMaxStackSize()) {
				out.grow(1);
			} else {
				return;   // the bin is full: the scrap waits, counted
			}
			bin -= Tuning.ZR_SCRAP_KG;
		}
		setChanged();
	}

	public int scrapBundles() {
		return slots.get(0).getCount();
	}

	/** For the game test: scrap into its bin. */
	public void addScrapForTest(double kg) {
		scrap(kg, false);
	}

	private String tubeName() {
		return die == 1 ? String.format("guide thimble (%.2f x %.2f mm)", Tuning.THIMBLE_OD_MM, Tuning.THIMBLE_WALL_MM)
				: die == 2 ? "spacer grid's strip" : die == 3 ? String.format("hafnium rodlet (%.1f mm, %.2f m)", Tuning.HF_RODLET_OD_MM, Tuning.HF_RODLET_LENGTH_M) : String.format("cladding tube (%.2f x %.2f mm)", Tuning.CLAD_OD_MM, Tuning.CLAD_WALL_MM);
	}

	/** A sneak-click: the next dies, cladding, thimble, grid strip, hafnium rodlet. The tube on the mill is scrapped. */
	public String changeDies() {
		if (Rules.zirconiumBatches() && progress > 0) {
			scrap(progress, die == 3);
		}
		die = (die + 1) % 4;
		scrap += progress;
		progress = 0;
		setChanged();
		return "Pilger mill: dies changed, now making " + tubeName();
	}

	public int die() {
		return die;
	}

	/** For the game test: the tube on the mill brought along by {@code kg}. */
	public void addProgressForTest(double kg) {
		progress = Math.min(tubeKg(), progress + kg);
		setChanged();
	}

	@Override
	protected void work(Level level) {
		ZrStage up = Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("extrusion_press"));
		ZrLineBlockEntity furnace = Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("vacuum_anneal_furnace"));
		if (up == null) {
			unit = "NOT BUILT: set it against the EXTRUSION PRESS";
			return;
		}
		if (furnace == null) {
			unit = "NO ANNEAL FURNACE: set the VACUUM ANNEAL FURNACE against it";
			return;
		}
		if (progress >= tubeKg() - 1e-9) {
			ItemStack tube = new ItemStack(die == 1 ? WildcatterItems.GUIDE_THIMBLE : die == 2 ? WildcatterItems.SPACER_GRID
					: die == 3 ? WildcatterItems.HAFNIUM_RODLET : WildcatterItems.ZIRCALOY_TUBE);
			if (!furnace.deliver(tube)) {
				unit = "FULL: take the finished tubes from the anneal furnace (empty hand, or a hopper under it)";
				return;
			}
			progress = 0;
		}
		boolean batches = Rules.zirconiumBatches();
		if (batches && up instanceof ZrLineBlockEntity press) {
			double ends = press.takeScrap();
			if (ends > 0) {
				scrap(ends, false);
			}
		}
		if (up.zirconium() > 1e-9 && up.hafnium() != (die == 3)) {
			unit = up.hafnium() ? "HAFNIUM HOLLOWS: change its dies to the hafnium rodlet (sneak-click)" : "the hafnium rodlet dies take hafnium hollows only: change its dies";
			furnace.anneal(level, "idle: the pilger mill has the wrong dies");
			return;
		}
		if (up.zirconium() < 1e-9) {
			unit = "waiting for hollows from the extrusion press";
			furnace.anneal(level, "idle: the pilger mill is waiting for hollows");
			return;
		}
		double heat = furnace.anneal(level, "annealing at 675-700 C between the passes");
		double strength = drive(level);
		if (strength <= 0 || heat <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double yield = batches ? Zirconium.pilgerYield() : 1;
		boolean hafnium = up.hafnium();
		double kg = up.takeZirconium(Math.min(Tuning.ZR_LINE_KGS * 1.2 * seconds() * Math.min(strength, heat), (tubeKg() - progress) / yield));
		progress += kg * yield;
		if (batches) {
			scrap(kg * (1 - yield), hafnium);
		}
		unit = String.format("rolling a %s: %.0f of %.0f g (%.0f %%), %.1f kg/h", tubeName(), progress * 1000, tubeKg() * 1000, progress / tubeKg() * 100,
				kg / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		String bins = slots.get(0).isEmpty() && bin < 1e-6 && hfScrap < 1e-6 ? "" : String.format("; scrap bin %d bundles and %.1f kg%s", slots.get(0).getCount(), bin,
				hfScrap > 1e-6 ? String.format(", hafnium scrap %.1f kg", hfScrap) : "");
		return Component.literal(String.format("Pilger mill (4 passes): %s; scrap %.1f kg%s; %s", unit, scrap, bins, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("die", die);
		output.putDouble("progress", progress);
		output.putDouble("scrap", scrap);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("bin", bin);
		output.putDouble("hfScrap", hfScrap);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		die = input.getIntOr("die", input.getBooleanOr("thimble", false) ? 1 : 0);
		progress = input.getDoubleOr("progress", 0);
		scrap = input.getDoubleOr("scrap", 0);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		bin = input.getDoubleOr("bin", 0);
		hfScrap = input.getDoubleOr("hfScrap", 0);
	}
}
