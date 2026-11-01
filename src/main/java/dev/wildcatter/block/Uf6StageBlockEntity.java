package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.fluid.Conversion;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The cold traps (30 kW of refrigeration) and the distillation columns (a 20 kW reboiler), both at 480 V
 * (docs/multiblocks/conversion.md): the traps freeze the UF6 out of the fluorinator's gas and melt it out; the columns take the
 * light and heavy impurities off it. Each takes from the stage touching it before it while its motor runs.
 */
public class Uf6StageBlockEntity extends UnitMotorBlockEntity implements UraniumStage {
	private double uranium;
	private String unit = "starting";

	public Uf6StageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private boolean traps() {
		return kind().equals("cold_traps");
	}

	@Override
	public double uranium() {
		return uranium;
	}

	@Override
	public double takeUranium(double kg) {
		double got = Math.min(kg, uranium);
		uranium -= got;
		setChanged();
		return got;
	}

	@Override
	protected void work(Level level) {
		boolean traps = traps();
		UraniumStage up = traps ? Touching.find(level, worldPosition, ConversionStageBlockEntity.class, s -> s.getBlockState().getBlock() instanceof dev.wildcatter.multiblock.BigPart p && p.shapeName().equals("fluorinator"))
				: Touching.find(level, worldPosition, Uf6StageBlockEntity.class, Uf6StageBlockEntity::traps);
		if (up == null) {
			unit = "NOT BUILT: set it against the " + (traps ? "FLUORINATOR" : "COLD TRAPS");
			return;
		}
		if (up.uranium() < 1e-6) {
			unit = "idle: no UF6 coming";
			return;
		}
		double room = Conversion.uraniumKgs() * Tuning.PLANT_HOLD_SECONDS - uranium;
		if (room <= 1e-7) {
			unit = "idle: full";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = up.takeUranium(Math.min(Conversion.uraniumKgs() * 1.2 * seconds() * strength, room));
		uranium += kg;
		unit = String.format("%s %.3f kg/s of UF6", traps ? "trapping" : "distilling", kg * Tuning.UF6_PER_U / seconds());
	}

	@Override
	public Component inspect() {
		return Component.literal((traps() ? "Cold traps (primary at -29 to -18 C): " : "Distillation columns (low and high boilers, 75 C): ") + unit
				+ String.format("; %.1f kg of UF6 held; ", uranium * Tuning.UF6_PER_U) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("uranium", uranium);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		uranium = input.getDoubleOr("uranium", 0);
	}
}
