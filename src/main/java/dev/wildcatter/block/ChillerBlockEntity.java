package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.GasPlant;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Run;
import dev.wildcatter.multiblock.BigParts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The NGL chiller skid (docs/multiblocks/gas-plant.md): the gas-gas exchanger, the propane chiller and the cold separator. Chilled to
 * about -20 F, dried gas drops its heavier parts as raw NGL (Y-grade) and leaves as pipeline gas. It needs a running refrigeration
 * compressor whose skid touches it. Gas still wet freezes in its exchanger (hydrates), so it takes dried gas only.
 */
public class ChillerBlockEntity extends PlantUnitBlockEntity {
	private double processed;

	public ChillerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CHILLER_ENTITY, pos, state);
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[2] != 6 || side != Direction.SOUTH) {
			return null;
		}
		return at[1] == 1 ? Product.PIPELINE_GAS : at[1] == 0 ? Product.Y_GRADE : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.Y_GRADE ? 50 : Tuning.CHILLER_M3S * Tuning.PLANT_HOLD_SECONDS;
	}

	/** The refrigeration compressor whose skid touches this one, or null. */
	private @Nullable GasCompressorBlockEntity refrigeration(Level level) {
		List<BlockPos> cells = BigParts.cells(worldPosition, getBlockState());
		for (BlockPos cell : cells) {
			for (Direction side : Direction.values()) {
				BlockPos next = cell.relative(side);
				if (!cells.contains(next) && level.getBlockState(next).getBlock() instanceof GasPlantPartBlock part
						&& part.part() == GasPlantPartBlock.Part.REFRIGERATION_COMPRESSOR && BigParts.entity(level, next) instanceof GasCompressorBlockEntity c) {
					return c;
				}
			}
		}
		return null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ChillerBlockEntity ch) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		ch.processed = 0;
		List<Lines.Face> inlet = ch.inlet();
		Run wrong = Lines.wrong(level, inlet, Product.DRIED_GAS);
		if (wrong != null && wrong.carries(Product.COMPRESSED_GAS)) {
			ch.status = "HYDRATES: wet gas is freezing in the exchanger. Dry it in a TEG dehydrator first";
			ch.setChanged();
			return;
		}
		double waiting = Lines.available(level, inlet, Product.DRIED_GAS, Pull.MACHINE, ch);
		GasCompressorBlockEntity cold = ch.refrigeration(level);
		if (waiting < 0.01) {
			ch.status = wrong != null ? "idle: its inlet is " + wrong.describe() + " (it takes dried gas)" : "idle: no dried gas at its front end";
		} else if (cold == null) {
			ch.status = "NO REFRIGERATION: set a refrigeration compressor against its skid";
		} else if (!cold.cool(level)) {
			ch.status = "NO REFRIGERATION: its refrigeration compressor is not running (" + cold.inspect().getString() + ")";
		} else {
			double want = Math.min(Tuning.CHILLER_M3S * seconds() * cold.cooling(),
					Math.min(ch.room(Product.PIPELINE_GAS) / Tuning.CHILLER_RESIDUE, ch.room(Product.Y_GRADE) / Tuning.CHILLER_NGL_PER_M3));
			double got = want > 0.001 ? Lines.pull(level, inlet, Product.DRIED_GAS, want, Pull.MACHINE, ch) : 0;
			ch.processed = got;
			ch.add(Product.PIPELINE_GAS, GasPlant.residue(got));
			ch.add(Product.Y_GRADE, GasPlant.ngl(got));
			ch.status = got > 0 ? String.format("chilling %.2f m3/s: %.2f m3/s pipeline gas, %.2f L/s raw NGL", got / seconds(), GasPlant.residue(got) / seconds(), GasPlant.ngl(got) / seconds())
					: "idle: its outlets are full (pipe the back end on: pipeline gas above, NGL below)";
		}
		ch.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("NGL chiller (mechanical refrigeration, to -20 F): " + status);
	}
}
