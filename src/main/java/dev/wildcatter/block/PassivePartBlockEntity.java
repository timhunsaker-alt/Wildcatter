package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * An acid plant part with no work of its own (docs/multiblocks/acid-plant.md): the drying tower, which dries the blower's air, and
 * the interpass absorber, which lets the converter's last bed finish. What touches them reads them.
 */
public class PassivePartBlockEntity extends PlantUnitBlockEntity {
	public PassivePartBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ACID_PASSIVE_ENTITY, pos, state);
	}

	public String kind() {
		return getBlockState().getBlock() instanceof BigPart part ? part.shapeName() : "";
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction localSide) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	public Component inspect() {
		return Component.literal(kind().equals("drying_tower") ? "Drying tower: dries the main blower's air with 98 % acid. It works against a main blower."
				: "Interpass absorber: takes the SO3 out after the converter's third bed, so the fourth reaches 99.8 %. It works against a converter.");
	}
}
