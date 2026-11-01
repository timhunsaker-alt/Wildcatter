package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The crude unit's side strippers (docs/multiblocks/crude-units.md): two 1 m x 10 m towers that steam the light ends out of the
 * kerosene and diesel the column draws off its side. Kerosene leaves the west face of the west tower's foot, diesel the east face of
 * the east tower's.
 */
public class StripperBlockEntity extends PlantUnitBlockEntity {
	public StripperBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STRIPPER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0) {
			return null;
		}
		return at[0] == 0 && side == Direction.WEST ? Product.KEROSENE : at[0] == 1 && side == Direction.EAST ? Product.DIESEL : null;
	}

	@Override
	protected double hold(Product product) {
		return 200;
	}

	public double roomFor(Product product) {
		return room(product);
	}

	public void give(Product product, double amount) {
		add(product, amount);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Side strippers (kerosene and diesel, 10 m): holding kerosene %.0f L, diesel %.0f L", held(Product.KEROSENE), held(Product.DIESEL)));
	}
}
