package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A resin trailer set down (16d1): the kg of U3O8 on its resin. */
public class ResinTrailerBlockEntity extends BlockEntity implements BigPartEntity {
	private boolean grown;
	private double u3o8;

	public ResinTrailerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RESIN_TRAILER_ENTITY, pos, state);
	}

	public double u3o8() {
		return u3o8;
	}

	public double room() {
		return Math.max(0, Tuning.RESIN_TRAILER_U3O8_KG - u3o8);
	}

	public void setU3o8(double kg) {
		u3o8 = Math.max(0, Math.min(Tuning.RESIN_TRAILER_U3O8_KG, kg));
		setChanged();
	}

	public String describe() {
		return describe(u3o8);
	}

	public static String describe(double kg) {
		return kg < 0.5 ? "Resin trailer (10 m3 of IX resin): eluted, ready for a satellite's columns"
				: String.format("Resin trailer (10 m3 of IX resin): loaded, %.0f of %.0f kg of U3O8 (%.0f %%)%s", kg, Tuning.RESIN_TRAILER_U3O8_KG,
						kg / Tuning.RESIN_TRAILER_U3O8_KG * 100, kg >= Tuning.RESIN_TRAILER_U3O8_KG - 0.5 ? ": full, for the central plant's columns" : "");
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putDouble("u3o8", u3o8);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		u3o8 = input.getDoubleOr("u3o8", 0);
	}
}
