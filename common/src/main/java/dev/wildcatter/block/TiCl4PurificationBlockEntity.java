package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The TiCl4 purification (docs/multiblocks/titanium-pigment.md): condensers and a distillation column that take out SiCl4 and, with a
 * small oil treatment, VOCl3: pure TiCl4 to the oxidiser it touches. Its reboiler is 40 kW at the world's industrial voltage; its box is
 * on the north face of its main cell (the chlorinator covers its west). The chlorinator runs it.
 */
public class TiCl4PurificationBlockEntity extends UnitMotorBlockEntity {
	double pure;     // kg of pure TiCl4 for the oxidiser
	double sicl4;    // kg of SiCl4 taken off, counted
	String unit = "idle: no crude TiCl4 from the chlorinator";

	public TiCl4PurificationBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TICL4_PURIFICATION_ENTITY, pos, state);
	}

	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("TiCl4 purification (reboiler %.0f kW, %s): ", Tuning.TICL4_PURIFICATION_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; pure TiCl4 held %.0f kg; %.0f kg of SiCl4 taken off; ", pure, sicl4) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("pure", pure);
		output.putDouble("sicl4", sicl4);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		pure = input.getDoubleOr("pure", 0);
		sicl4 = input.getDoubleOr("sicl4", 0);
	}
}
