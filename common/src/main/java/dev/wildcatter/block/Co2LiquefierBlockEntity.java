package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The CO2 liquefier (docs/multiblocks/isr-operations.md, 16d1): kiln-gas CO2 recovery, a scrubber, a two-stage compressor, a dryer and
 * a refrigerated condenser at about 20 bar and -25 C. Against a lime kiln it takes the kiln's CO2 (0.785 kg a kg of quicklime, about
 * 327 kg/h) up to 150 kg/h, and puts liquid CO2 out of the south face of its back cells for the CO2 storage tank. Its compressor and
 * refrigeration: 75 kW on the industrial supply, box on the west.
 */
public class Co2LiquefierBlockEntity extends UnitMotorBlockEntity {
	private double made;
	private double rate;
	private String unit = "starting";

	public Co2LiquefierBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CO2_LIQUEFIER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 5 && at[1] == 0 && side == Direction.SOUTH ? Product.CO2 : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.CO2 ? Tuning.CO2_LIQUEFIER_KGH / 3600 * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	/** kg an hour it liquefied, its last interval. */
	public double kgh() {
		return rate * 3600;
	}

	@Override
	protected void work(Level level) {
		LimeKilnBlockEntity kiln = Touching.find(level, worldPosition, LimeKilnBlockEntity.class, k -> true);
		rate = 0;
		if (kiln == null) {
			unit = "NOT BUILT: set it against a LIME KILN";
			return;
		}
		double room = room(Product.CO2);
		if (room <= 1e-6) {
			unit = "FULL: its liquid CO2 has nowhere to go (pipe its back to a CO2 storage tank)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the compressor: " + describe();
			return;
		}
		double kg = kiln.takeKilnGas(Math.min(room, Tuning.CO2_LIQUEFIER_KGH / 3600 * seconds() * strength));
		add(Product.CO2, kg);
		made += kg;
		rate = kg / seconds();
		unit = kg > 1e-9 ? String.format("liquefying %.0f kg/h of kiln CO2 at 20 bar, -25 C", rate * 3600) : "idle: the kiln is not burning (no CO2 in its gas)";
	}

	@Override
	public Component inspect() {
		return Component.literal("CO2 liquefier (" + Math.round(Tuning.CO2_LIQUEFIER_WATTS / 1000) + " kW, " + Voltage.INDUSTRIAL.label() + "): " + unit
				+ String.format("; %,.0f kg liquefied, %.0f kg held; ", made, held(Product.CO2)) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("made", made);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		made = input.getDoubleOr("made", 0);
	}
}
