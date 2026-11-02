package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.GasPlant;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Run;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A packaged triethylene glycol dehydrator (docs/multiblocks/gas-plant.md): compressed gas rises through the contactor against lean
 * glycol, which takes its water; the reboiler boils the water back out of the glycol, burning 0.5 % of the gas. A glycol-powered pump
 * circulates it, so it needs no electricity. Wet compressed gas in at the front, dried gas out of the back.
 */
public class DehydratorBlockEntity extends PlantUnitBlockEntity {
	private double dried;

	public DehydratorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DEHYDRATOR_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 4 && at[1] == 0 && side == Direction.SOUTH ? Product.DRIED_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.DEHYDRATOR_M3S * Tuning.PLANT_HOLD_SECONDS;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DehydratorBlockEntity d) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double want = Math.min(Tuning.DEHYDRATOR_M3S * seconds(), d.room(Product.DRIED_GAS) / (1 - Tuning.GLYCOL_FUEL));
		double got = want > 0.01 ? Lines.pull(level, d.inlet(), Product.COMPRESSED_GAS, want, Pull.MACHINE, d) : 0;
		d.dried = GasPlant.dried(got);
		d.add(Product.DRIED_GAS, d.dried);
		Run wrong = got > 0 ? null : Lines.wrong(level, d.inlet(), Product.COMPRESSED_GAS);
		d.status = got > 0 ? String.format("drying %.2f m3/s (the reboiler burns %.1f%%)", d.dried / seconds(), Tuning.GLYCOL_FUEL * 100)
				: want <= 0.01 ? "idle: its dry gas has nowhere to go (pipe the back end to a chiller)"
				: wrong != null ? "idle: its inlet is " + wrong.describe() + " (it takes compressed gas)"
				: "idle: no compressed gas at its front end";
		d.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("TEG dehydrator (3 MMscfd, contactor 7.5 m): " + status);
	}
}
