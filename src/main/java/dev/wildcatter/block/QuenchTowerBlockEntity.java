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
 * The water quench tower (docs/multiblocks/cracker.md): hot cracked gas in on the north face of its foot, cooled; the pyrolysis
 * gasoline (east face) and fuel oil (west face) drop out, and the cracked gas leaves the south face.
 */
public class QuenchTowerBlockEntity extends PlantUnitBlockEntity {
	public QuenchTowerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.QUENCH_TOWER_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[1] != 0) {
			return null;
		}
		return side == Direction.SOUTH ? Product.CRACKED_GAS : side == Direction.EAST ? Product.GASOLINE : side == Direction.WEST ? Product.RESIDUE : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.CRACKED_GAS ? Tuning.QUENCH_KGS * Tuning.PLANT_HOLD_SECONDS : 20;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, QuenchTowerBlockEntity q) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double max = Math.min(Tuning.QUENCH_KGS * seconds(), q.room(Product.CRACKED_GAS));
		double[] in = new double[5];
		double got = max > 0.001 ? q.pullMixed(level, q.inlet(), Product.HOT_CRACKED_GAS, max, in) : 0;
		double gas = in[0] + in[1] + in[2];
		if (got > 0 && gas > 0) {
			q.addMixed(Product.CRACKED_GAS, got * gas, new double[] {in[0] / gas, in[1] / gas, in[2] / gas, 0, 0});
			q.add(Product.GASOLINE, got * in[3] / Tuning.GASOLINE_KG_PER_L);
			q.add(Product.RESIDUE, got * in[4] / Tuning.RESIDUE_KG_PER_L);
		}
		q.status = got > 0 ? String.format("quenching %.2f kg/s: %.2f kg/s of cracked gas on", got / seconds(), got * gas / seconds())
				: max <= 0.001 ? "idle: its cracked gas has nowhere to go (pipe the south face to the cracked gas compressor)"
				: "idle: no hot cracked gas at its foot (north face)";
		q.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Quench tower: " + status + String.format("; pyrolysis gasoline %.1f L, fuel oil %.1f L held", held(Product.GASOLINE), held(Product.RESIDUE)));
	}
}
