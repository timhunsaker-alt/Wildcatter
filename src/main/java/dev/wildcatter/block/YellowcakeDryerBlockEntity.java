package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Mill;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The yellowcake spray dryer (docs/multiblocks/uranium-mill.md): it dries the precipitation tank's precipitate, touching both the
 * tank and the drum filler, on a 0.5 MW gas burner fed on the east face of its bottom cells. The dry concentrate is 80 % U3O8.
 */
public class YellowcakeDryerBlockEntity extends PlantUnitBlockEntity {
	public YellowcakeDryerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.YELLOWCAKE_DRYER_ENTITY, pos, state);
	}

	private List<Lines.Face> gas() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, YellowcakeDryerBlockEntity d) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		PrecipitationTankBlockEntity tank = Touching.find(level, pos, PrecipitationTankBlockEntity.class, t -> true);
		DrumFillerBlockEntity filler = Touching.find(level, pos, DrumFillerBlockEntity.class, f -> true);
		if (tank == null || filler == null) {
			d.status = "NOT BUILT: set it against a PRECIPITATION TANK and a DRUM FILLER";
		} else {
			double max = Math.min(Tuning.MILL_U3O8_KGS * seconds(), filler.room() / Mill.yellowcakePerU3o8());   // its burner's rating
			double u = max > 0.0001 ? tank.takePrecipitate(max) : 0;
			if (u <= 0) {
				d.status = max <= 0.0001 ? "idle: the drum filler is full (empty drums in its hopper, full ones out under it)" : "idle: no precipitate in the tank";
			} else {
				double need = Tuning.DRYER_WATTS * seconds() * u / (Tuning.MILL_U3O8_KGS * seconds());
				double got = GasFiring.fire(level, d.gas(), need, d);
				double dried = u * Math.min(1, need > 0 ? got / need : 0);
				tank.takePrecipitate(-(u - dried));   // what it could not dry goes back
				filler.addYellowcake(dried * Mill.yellowcakePerU3o8());
				d.status = dried > 0 ? String.format("drying %.3f kg/s of yellowcake (%.0f %% U3O8) on %.0f kW of gas", dried * Mill.yellowcakePerU3o8() / seconds(),
						Tuning.YELLOWCAKE_U3O8 * 100, got / seconds() / 1000) : "NO GAS: pipe pipeline gas to its east side";
			}
		}
		d.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Yellowcake dryer: " + status);
	}
}
