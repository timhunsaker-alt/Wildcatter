package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Zirconium;
import net.minecraft.core.BlockPos;
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
 */
public class PilgerMillBlockEntity extends UnitMotorBlockEntity {
	private int die;           // 0 cladding tube, 1 guide thimble, 2 grid strip
	private double progress;   // kg of the tube on the mill
	private double scrap;
	private String unit = "starting";

	public PilgerMillBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PILGER_MILL_ENTITY, pos, state);
	}

	private double tubeKg() {
		return die == 1 ? Zirconium.thimbleKg() : die == 2 ? Tuning.SPACER_GRID_KG : Zirconium.claddingKg();
	}

	private String tubeName() {
		return die == 1 ? String.format("guide thimble (%.2f x %.2f mm)", Tuning.THIMBLE_OD_MM, Tuning.THIMBLE_WALL_MM)
				: die == 2 ? "spacer grid's strip" : String.format("cladding tube (%.2f x %.2f mm)", Tuning.CLAD_OD_MM, Tuning.CLAD_WALL_MM);
	}

	/** A sneak-click: the next dies, cladding, thimble, grid strip. The tube on the mill is scrapped. */
	public String changeDies() {
		die = (die + 1) % 3;
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
			ItemStack tube = new ItemStack(die == 1 ? WildcatterItems.GUIDE_THIMBLE : die == 2 ? WildcatterItems.SPACER_GRID : WildcatterItems.ZIRCALOY_TUBE);
			if (!furnace.deliver(tube)) {
				unit = "FULL: take the finished tubes from the anneal furnace (empty hand, or a hopper under it)";
				return;
			}
			progress = 0;
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
		double kg = up.takeZirconium(Math.min(Tuning.ZR_LINE_KGS * 1.2 * seconds() * Math.min(strength, heat), tubeKg() - progress));
		progress += kg;
		unit = String.format("rolling a %s: %.0f of %.0f g (%.0f %%), %.1f kg/h", tubeName(), progress * 1000, tubeKg() * 1000, progress / tubeKg() * 100,
				kg / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Pilger mill (4 passes): %s; scrap %.1f kg; %s", unit, scrap, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("die", die);
		output.putDouble("progress", progress);
		output.putDouble("scrap", scrap);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		die = input.getIntOr("die", input.getBooleanOr("thimble", false) ? 1 : 0);
		progress = input.getDoubleOr("progress", 0);
		scrap = input.getDoubleOr("scrap", 0);
	}
}
