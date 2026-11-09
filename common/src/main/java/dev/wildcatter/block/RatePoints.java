package dev.wildcatter.block;

import dev.wildcatter.control.ControlDevice.Point;
import dev.wildcatter.control.ControlDevice.Signal;
import java.util.List;

/**
 * The PLC points of the ammonia converter and the nitric acid unit (docs/multiblocks/ammonia-plant.md): RUN (the run enable, as on
 * every plant unit), RUNNING, AUTO, and the rate as 4-20 mA over 0 to its rating.
 */
final class RatePoints {
	private RatePoints() {
	}

	static final List<Point> POINTS = List.of(new Point("RUN", true), new Point("RUNNING", false), new Point("AUTO", false), new Point("RATE 4-20 mA", false, Signal.MA));

	static boolean read(int point, boolean running, boolean auto) {
		return point == 1 ? running : point == 2 && auto;
	}

	static double ma(double rate, double rated) {
		return 4 + 16 * Math.min(1.03, Math.max(0, rate / rated));
	}
}
