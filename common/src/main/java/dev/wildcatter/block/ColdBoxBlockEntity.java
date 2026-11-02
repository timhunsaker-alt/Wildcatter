package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The cracker's cold box (docs/multiblocks/cracker.md): the chilling train, demethanizer and C2 splitter. Compressed cracked gas in
 * on the north face of its front cells; out of the south face of its back cells, LPG (the C3s and C4s) at the bottom, ethylene above
 * it and fuel gas (methane and hydrogen) at the top. It needs a running refrigeration compressor touching it.
 */
public class ColdBoxBlockEntity extends PlantUnitBlockEntity {
	public ColdBoxBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COLD_BOX_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[2] != 2 || side != Direction.SOUTH) {
			return null;
		}
		return at[1] == 0 ? Product.LPG : at[1] == 1 ? Product.ETHYLENE : at[1] == 2 ? Product.PIPELINE_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.LPG ? 20 : 5;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ColdBoxBlockEntity c) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double waiting = Lines.available(level, c.inlet(), Product.COMPRESSED_CRACKED_GAS, Pull.MACHINE, c);
		GasCompressorBlockEntity cold = Touching.find(level, pos, GasCompressorBlockEntity.class, GasCompressorBlockEntity::isRefrigeration);
		if (waiting < 0.001) {
			c.status = "idle: no compressed cracked gas at its front";
		} else if (cold == null) {
			c.status = "NO REFRIGERATION: set a refrigeration compressor against it";
		} else if (!cold.cool(level)) {
			c.status = "NO REFRIGERATION: its refrigeration compressor is not running";
		} else {
			// no more than every product has room for, whatever the gas is made of
			double max = Math.min(Tuning.COLD_BOX_KGS * seconds() * cold.cooling(), Math.min(c.room(Product.ETHYLENE) * Tuning.ETHYLENE_KG_PER_M3,
					Math.min(c.room(Product.PIPELINE_GAS) * Tuning.FUEL_GAS_KG_PER_M3, c.room(Product.LPG) * Tuning.LPG_KG_PER_L)));
			double[] in = new double[5];
			double got = c.pullMixed(level, c.inlet(), Product.COMPRESSED_CRACKED_GAS, max, in);
			c.add(Product.ETHYLENE, got * in[0] / Tuning.ETHYLENE_KG_PER_M3);
			c.add(Product.PIPELINE_GAS, got * in[1] / Tuning.FUEL_GAS_KG_PER_M3);
			c.add(Product.LPG, got * in[2] / Tuning.LPG_KG_PER_L);
			c.status = got > 0 ? String.format("separating %.2f kg/s: %.3f m3/s ethylene, %.3f m3/s fuel gas, %.3f L/s LPG", got / seconds(),
					got * in[0] / Tuning.ETHYLENE_KG_PER_M3 / seconds(), got * in[1] / Tuning.FUEL_GAS_KG_PER_M3 / seconds(), got * in[2] / Tuning.LPG_KG_PER_L / seconds())
					: "idle: its products have nowhere to go (ethylene, fuel gas and LPG out of its south face)";
		}
		c.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Cold box (chilling train, demethanizer, C2 splitter): " + status);
	}
}
