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
 * The tar distillery (docs/multiblocks/coke-ovens.md): a continuous column and pipe still, pumps 15 kW at 480 V, firing 1.0 MJ a kg
 * of tar of gas, coke oven gas or LPG at the east face of its bottom cells. Tar in at the north face of its front cells; 55 % comes
 * out as pitch (the south face of its back cells at x 0), the tar oils onto the residue line (at x 2).
 */
public class TarStillBlockEntity extends UnitMotorBlockEntity {
	private String unit = "starting";

	public TarStillBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TAR_STILL_ENTITY, pos, state);
	}

	private static double tarRate() {
		return Tuning.COKE_OVEN_COAL_KGS * Tuning.CARBON_CATCH_UP * Tuning.COKE_OVEN_TAR_PER_KG;
	}

	private static double perTar(Product product) {
		return product == Product.PITCH ? Tuning.PITCH_PER_TAR : Carbon.tarOilLitres();
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0 || side != Direction.SOUTH || at[2] != 2) {
			return null;
		}
		return at[0] == 0 ? Product.PITCH : at[0] == 2 ? Product.RESIDUE : null;
	}

	@Override
	protected double hold(Product product) {
		return Math.max(20, tarRate() * perTar(product) * Tuning.PLANT_HOLD_SECONDS);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected void work(Level level) {
		double tar = Lines.available(level, inlet(), Product.COAL_TAR, Pull.MACHINE, this);
		if (tar < 1e-4) {
			unit = "NO TAR: pipe coal tar from the by-product plant to its front (north face)";
			return;
		}
		double max = Math.min(Math.min(tarRate() * seconds(), tar), Math.min(room(Product.PITCH) / perTar(Product.PITCH), room(Product.RESIDUE) / perTar(Product.RESIDUE)));
		if (max <= 1e-7) {
			unit = "idle: its pitch or tar oils have nowhere to go";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = max * strength;
		double need = kg * Tuning.TAR_STILL_J_PER_KG;
		double got = GasFiring.fire(level, east(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "NO FUEL: pipe gas, coke oven gas or LPG to its east side";
			return;
		}
		kg = Lines.pull(level, inlet(), Product.COAL_TAR, kg, Pull.MACHINE, this);
		add(Product.PITCH, kg * Tuning.PITCH_PER_TAR);
		add(Product.RESIDUE, kg * Carbon.tarOilLitres());
		unit = String.format("distilling %.0f kg/h of tar: %.0f kg/h of pitch, %.0f L/h of tar oils", kg / seconds() * 3600, kg * Tuning.PITCH_PER_TAR / seconds() * 3600,
				kg * Carbon.tarOilLitres() / seconds() * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal("Tar still: " + unit + heldReadout() + "; " + describe());
	}
}
