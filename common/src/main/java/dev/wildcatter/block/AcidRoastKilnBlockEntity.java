package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The acid roast kiln (docs/multiblocks/lithium.md): a pug mill and an indirectly fired kiln at 250 C, the beta-spodumene roasted with
 * sulfuric acid (30 % excess) to soluble lithium sulfate. Acid on the east face of its back cell (z 7, past the leach), 0.2 MW of gas on the
 * south face of its back cells. 15 kW at the world's industrial voltage, the box on the north face of its main cell (the decrepitation kiln
 * covers its west). The lithium leach runs it.
 */
public class AcidRoastKilnBlockEntity extends UnitMotorBlockEntity {
	String unit = "idle: the leach is not running it";

	public AcidRoastKilnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ACID_ROAST_KILN_ENTITY, pos, state);
	}

	List<Lines.Face> acidIn() {
		return faces(at -> at[0] == 2 && at[2] == 7 && at[1] == 0, Direction.EAST);
	}

	List<Lines.Face> gasIn() {
		return faces(at -> at[2] == 7 && at[1] == 0, Direction.SOUTH);
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
		return Component.literal(String.format("Acid roast kiln (%.0f kW, %s): ", Tuning.ACID_ROAST_KILN_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit + "; " + describe());
	}
}
