package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The HP feedwater heaters (docs/multiblocks/turbine-island.md), against a feed pump: they bring the feedwater to 227 C on extraction
 * steam and give it on the south face of their back bottom cells, piped to the steam generators.
 */
public class HpHeaterBlockEntity extends PlantUnitBlockEntity {
	public HpHeaterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HP_HEATER_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 7 && at[1] == 0 && side == Direction.SOUTH ? Product.FEEDWATER : null;
	}

	@Override
	protected double hold(Product product) {
		return 2 * Tuning.MAIN_FEED_PUMP_KGS * 2;
	}

	public double roomForFeed() {
		return room(Product.FEEDWATER);
	}

	public void feed(double kg) {
		add(Product.FEEDWATER, kg);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("HP feedwater heaters: feedwater at %.0f C, %.0f kg ready for the steam generators", Tuning.FEEDWATER_C,
				held(Product.FEEDWATER)));
	}
}
