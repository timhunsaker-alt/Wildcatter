package dev.wildcatter.kinetic;

import java.util.List;
import net.minecraft.core.Direction;

/**
 * A formed machine that draws the pipes and cables running between its parts (docs/questions/misc-gaps.md, 1 and 4): the runs
 * belong to no one part, so the controller draws them once the parts are all in place. Points are in metres in the controller's own
 * frame drawn facing north (x east, y up, z south, from its main cell's north-west bottom corner, as the part models are); the
 * renderer turns them with {@link #facing()}.
 */
public interface DrawnRuns {
	/** What a run is drawn as. */
	enum Kind {
		/** A round pipe of radius {@code size} along the points, with a flange at each end. */
		PIPE,
		/** A ladder cable tray {@code size} wide along the points, with three cables laid in it. */
		TRAY,
		/** A bundle of three cables of radius {@code size} along the points (a drop from a tray to a motor's terminal box). */
		CABLES,
		/** A square post of half-width {@code size} from each point down to the ground (y 0). */
		POSTS
	}

	/** One run: its look, its texture (a block atlas path, "block/mat/..."), its size in metres and its points. */
	record Run(Kind kind, String texture, double size, List<double[]> points) {
		public static Run of(Kind kind, String texture, double size, double[]... points) {
			return new Run(kind, texture, size, List.of(points));
		}
	}

	/** The runs to draw now: none while the machine is not formed. */
	List<Run> drawnRuns();

	/** The way the machine faces. */
	Direction facing();
}
