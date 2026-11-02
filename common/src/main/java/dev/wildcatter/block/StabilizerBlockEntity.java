package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.GasPlant;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The debutanizer column (docs/multiblocks/gas-plant.md): raw NGL in, propane and butane (LPG) off the top through its condenser and
 * reflux drum, natural gasoline (C5+) from the bottom. Y-grade in on the north face of its base, LPG out of the east, gasoline out of
 * the south.
 */
public class StabilizerBlockEntity extends PlantUnitBlockEntity {
	public StabilizerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.STABILIZER_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0) {
			return null;
		}
		return side == Direction.EAST ? Product.LPG : side == Direction.SOUTH ? Product.NATURAL_GASOLINE : null;
	}

	@Override
	protected double hold(Product product) {
		return 50;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StabilizerBlockEntity st) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double want = Math.min(Tuning.STABILIZER_LPS * seconds(), Math.min(st.room(Product.LPG) / Tuning.STABILIZER_LPG,
				st.room(Product.NATURAL_GASOLINE) / (1 - Tuning.STABILIZER_LPG)));
		double got = want > 0.001 ? Lines.pull(level, st.inlet(), Product.Y_GRADE, want, Pull.MACHINE, st) : 0;
		st.add(Product.LPG, GasPlant.lpg(got));
		st.add(Product.NATURAL_GASOLINE, GasPlant.gasoline(got));
		st.status = got > 0 ? String.format("splitting %.2f L/s of raw NGL: %.2f L/s LPG, %.2f L/s natural gasoline", got / seconds(), GasPlant.lpg(got) / seconds(), GasPlant.gasoline(got) / seconds())
				: want <= 0.001 ? "idle: its products have nowhere to go (LPG to a bullet, east; gasoline to a stock tank, south)"
				: "idle: no raw NGL at its base (north face)";
		st.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Stabilizer (debutanizer, 12 m): " + status);
	}
}
