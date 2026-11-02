package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.world.level.Level;

/**
 * A gas burner on a process part: an acid plant's melter starting cold, a mill's yellowcake dryer. Any natural gas on its pipe, or
 * LPG, as a cracking furnace fires on it.
 */
final class GasFiring {
	private GasFiring() {
	}

	/** Burns up to {@code joules} of gas from the runs on {@code faces}; returns the joules it got. */
	static double fire(Level level, List<Lines.Face> faces, double joules, Object self) {
		double got = 0;
		for (Lines.Line line : Lines.lines(level, faces)) {
			Product p = line.run().product();
			double per = p == Product.PIPELINE_GAS || p == Product.GAS || p == Product.DRIED_GAS || p == Product.COMPRESSED_GAS ? Tuning.GAS_JOULES_PER_M3
					: p == Product.LPG ? Tuning.LPG_JOULES_PER_L : 0;
			if (per > 0 && got < joules) {
				got += Lines.pull(line, p, (joules - got) / per, FluidNode.Pull.MACHINE, self) * per;
			}
		}
		return got;
	}
}
