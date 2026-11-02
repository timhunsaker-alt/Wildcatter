package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Zirconium;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Kroll reactor (docs/multiblocks/zirconium.md): a sealed retort in its electric furnace (100 kW, 480 V) at about 850 C. ZrCl4
 * from the hafnium column it touches is reduced on molten magnesium from the magnesium cell it touches, charged 25 % over the two Mg
 * a Zr: ZrCl4 + 2Mg -> Zr + 2MgCl2. The MgCl2 is tapped back to the cell; the sponge, with the excess Mg in it, waits for the vacuum
 * distillation.
 */
public class KrollReactorBlockEntity extends UnitMotorBlockEntity implements ZrStage {
	private double metal;   // sponge, kg
	private String unit = "starting";

	public KrollReactorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.KROLL_REACTOR_ENTITY, pos, state);
	}

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

	@Override
	protected void work(Level level) {
		ZrStage up = Touching.find(level, worldPosition, ZrLineBlockEntity.class, z -> z.kind().equals("hafnium_column"));
		MagnesiumCellBlockEntity cell = Touching.find(level, worldPosition, MagnesiumCellBlockEntity.class, c -> true);
		if (up == null) {
			unit = "NOT BUILT: set it against the HAFNIUM COLUMN";
			return;
		}
		if (cell == null) {
			unit = "NOT BUILT: set it against the MAGNESIUM CELL";
			return;
		}
		if (up.zirconium() < 1e-9) {
			unit = "waiting for ZrCl4 from the hafnium column";
			return;
		}
		double charge = Zirconium.magnesiumPerZr() * Tuning.KROLL_MG_EXCESS;   // Mg charged a kg of Zr
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
		double zr = Math.min(Math.min(Tuning.ZR_LINE_KGS * 1.2 * seconds() * strength, room),
				Math.min(cell.mg() / charge, cell.mgCl2Room() / Zirconium.mgCl2PerZr()));
		zr = up.takeZirconium(zr);
		cell.takeMg(zr * charge);
		cell.addMgCl2(zr * Zirconium.mgCl2PerZr());
		metal += zr;
		unit = String.format("reducing at about 850 C: %.1f kg/h of zirconium sponge, %.1f kg/h of magnesium; %.2f kg of sponge held", zr / seconds() * 3600,
				zr * charge / seconds() * 3600, metal);
	}

	@Override
	public Component inspect() {
		return Component.literal("Kroll reactor: " + unit + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("metal", metal);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		metal = input.getDoubleOr("metal", 0);
	}
}
