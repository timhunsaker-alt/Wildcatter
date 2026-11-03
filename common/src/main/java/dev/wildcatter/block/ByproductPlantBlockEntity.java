package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Carbon;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The by-product plant (docs/multiblocks/coke-ovens.md): primary coolers, tar decanter and exhauster, 300 kW at 4.16 kV. Raw coke
 * oven gas in at the north face of its front cells; 40 % goes back to underfire the battery, the rest leaves as coke oven gas out of
 * the south face of its back cells, and the tar it condenses out of the east face of its bottom cells.
 */
public class ByproductPlantBlockEntity extends UnitMotorBlockEntity {
	private String unit = "starting";

	public ByproductPlantBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BYPRODUCT_PLANT_ENTITY, pos, state);
	}

	private static double rawRate() {
		return Tuning.COKE_OVEN_COAL_KGS * Tuning.CARBON_CATCH_UP * Tuning.COKE_OVEN_GAS_PER_KG;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0) {
			return null;
		}
		return side == Direction.SOUTH && at[2] == 7 ? Product.COKE_OVEN_GAS : side == Direction.EAST && at[0] == 4 ? Product.COAL_TAR : null;
	}

	@Override
	protected double hold(Product product) {
		return rawRate() * (product == Product.COAL_TAR ? Carbon.tarPerRawGas() : Carbon.cleanPerRawGas()) * Tuning.PLANT_HOLD_SECONDS;
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected void work(Level level) {
		double raw = Lines.available(level, inlet(), Product.RAW_COKE_OVEN_GAS, Pull.MACHINE, this);
		if (raw < 1e-4) {
			unit = "idle: no raw gas at its front (north face)";
			return;
		}
		double max = Math.min(Math.min(rawRate() * seconds(), raw), Math.min(room(Product.COKE_OVEN_GAS) / Carbon.cleanPerRawGas(),
				room(Product.COAL_TAR) / Carbon.tarPerRawGas()));
		if (max <= 1e-7) {
			unit = "idle: its gas or tar has nowhere to go";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double m3 = Lines.pull(level, inlet(), Product.RAW_COKE_OVEN_GAS, max * strength, Pull.MACHINE, this);
		add(Product.COKE_OVEN_GAS, m3 * Carbon.cleanPerRawGas());
		add(Product.COAL_TAR, m3 * Carbon.tarPerRawGas());
		unit = String.format("cleaning %.2f m3/s of raw gas: %.2f m3/s of coke oven gas out (%.2f back to the underfiring), %.0f kg/h of tar", m3 / seconds(),
				m3 * Carbon.cleanPerRawGas() / seconds(), m3 * Tuning.COKE_OVEN_UNDERFIRE / seconds(), m3 * Carbon.tarPerRawGas() / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal("By-product plant: " + unit + heldReadout() + "; " + describe());
	}
}
