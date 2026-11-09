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

	/** Its reboiler's fuel: gas or LPG piped to the west face of its base. */
	private List<Lines.Face> fuel() {
		return faces(at -> at[1] == 0, Direction.WEST);
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
		if (st.plcHold(level)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double want = Math.min(Tuning.STABILIZER_LPS * seconds(), Math.min(st.room(Product.LPG) / Tuning.STABILIZER_LPG,
				st.room(Product.NATURAL_GASOLINE) / (1 - Tuning.STABILIZER_LPG)));
		double fed = Math.min(want, st.testFeed);
		st.testFeed -= fed;
		double got = fed + (want - fed > 0.001 ? Lines.pull(level, st.inlet(), Product.Y_GRADE, want - fed, Pull.MACHINE, st) : 0);
		// its fired reboiler: fuel piped to its west face, or its own LPG off the reflux drum
		double need = GasPlant.reboilerFuel(got);
		double piped = need > 0 ? GasFiring.fire(level, st.fuel(), need, st) : 0;
		double own = Math.min(GasPlant.lpg(got), (need - piped) / Tuning.LPG_JOULES_PER_L);
		st.add(Product.LPG, GasPlant.lpg(got) - own);
		st.add(Product.NATURAL_GASOLINE, GasPlant.gasoline(got));
		String fired = got <= 0 ? "" : own > 1e-9 ? String.format("; its reboiler burns %.1f kW of its own LPG (pipe fuel gas to its west face to save it)", own * Tuning.LPG_JOULES_PER_L / seconds() / 1000)
				: String.format("; its reboiler burns %.1f kW of piped fuel", piped / seconds() / 1000);
		st.status = got > 0 ? String.format("splitting %.2f L/s of raw NGL: %.2f L/s LPG, %.2f L/s natural gasoline%s", got / seconds(), (GasPlant.lpg(got) - own) / seconds(),
				GasPlant.gasoline(got) / seconds(), fired)
				: want <= 0.001 ? "idle: its products have nowhere to go (LPG to a bullet, east; gasoline to a stock tank, south)"
				: "idle: no raw NGL at its base (north face)";
		st.setChanged();
	}

	/** For the game test: raw NGL as if from a chiller, litres. */
	private double testFeed;

	public void testFeed(double litres) {
		testFeed += litres;
	}

	@Override
	public Component inspect() {
		return Component.literal("Stabilizer (debutanizer, 12 m): " + status);
	}
}
