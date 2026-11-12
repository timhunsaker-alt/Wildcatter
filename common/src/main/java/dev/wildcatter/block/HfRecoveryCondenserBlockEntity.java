package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Fuel;
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
 * The HF recovery condenser (docs/multiblocks/fuel-fabrication-extensions.md, 19c2): graphite condensers and a small column on the IDR
 * kiln's off-gas, set against the kiln; 10 kW at LV (box on the north face of its main cell). The kiln's HF (0.504 kg a kg of U) comes out
 * as 70 % aqueous HF on the south face of its bottom cells (x 1), for the conversion plant's HF column. With nowhere for it to go, the
 * kiln's HF goes to its scrubber as before.
 */
public class HfRecoveryCondenserBlockEntity extends UnitMotorBlockEntity {
	private double made;       // kg of aqueous HF, all told
	private double making;     // kg/h
	private String unit = "starting";

	public HfRecoveryCondenserBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HF_RECOVERY_CONDENSER_ENTITY, pos, state);
	}

	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[0] == 1 && at[1] == 0 && at[2] == 1 && side == Direction.SOUTH ? Product.AQUEOUS_HF : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.AQUEOUS_HF ? Tuning.FUEL_LINE_U_KGS * Fuel.hfPerU() / Tuning.AQUEOUS_HF_STRENGTH * 60 : 0;
	}

	public double making() {
		return making;
	}

	public double made() {
		return made;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		FuelLineBlockEntity kiln = Touching.find(level, worldPosition, FuelLineBlockEntity.class, f -> f.kind().equals("idr_kiln"));
		if (kiln == null) {
			unit = "NOT BUILT: set it against the IDR KILN";
			return;
		}
		double room = room(Product.AQUEOUS_HF) * Tuning.AQUEOUS_HF_STRENGTH;
		if (room <= 1e-9) {
			unit = "FULL: pipe its aqueous HF (south side) to a tank or the conversion plant's HF column";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double hf = kiln.takeKilnHf(Math.min(room, Tuning.FUEL_LINE_U_KGS * Fuel.hfPerU() * 2 * seconds() * strength));
		add(Product.AQUEOUS_HF, hf / Tuning.AQUEOUS_HF_STRENGTH);
		made += hf / Tuning.AQUEOUS_HF_STRENGTH;
		making = hf / Tuning.AQUEOUS_HF_STRENGTH / seconds() * 3600;
		unit = making > 0 ? String.format("condensing the kiln's HF: %.1f kg/h of 70 %% aqueous HF", making) : "waiting for the kiln's off-gas";
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("HF recovery condenser (%.0f kW, %s): %s; %.1f kg of aqueous HF held, %.1f kg made; ", Tuning.HF_CONDENSER_WATTS / 1000,
				Voltage.INDUSTRIAL.label(), unit, held(Product.AQUEOUS_HF), made) + describe());
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
