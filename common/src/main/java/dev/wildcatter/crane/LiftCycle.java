package dev.wildcatter.crane;

import java.util.ArrayList;
import java.util.List;

/**
 * One lift's steps (docs/multiblocks/tower-crane.md, "How it works a site"): the empty hook goes to the pick, the riggers sling
 * the load, it is hoisted clear, slewed and moved out together, lowered and set. Dismantling runs the same steps from the site to
 * the chest. Each step has its real time from the crane's speeds and what its drives draw. Plain Java, so it is unit-tested without
 * Minecraft.
 */
public final class LiftCycle {
	private LiftCycle() {
	}

	public enum Phase {
		TO_PICK("MOVING TO THE PICK"), RIG("RIGGING"), HOIST("HOISTING"), SLEW("SLEWING"), LOWER("LOWERING"), SET("SETTING");

		public final String label;

		Phase(String label) {
			this.label = label;
		}
	}

	/** Where the hook is: slewing angle (degrees), radius (m) and height above the crane's base (m). */
	public record Hook(double degrees, double radius, double height) {
	}

	/** One step: how long it takes, where the hook goes, whether the load is on it, and what the drives draw. */
	public record Step(Phase phase, double seconds, Hook from, Hook to, boolean loaded, double watts) {
	}

	/** The empty hook's own mass, t: the hook block, as the rigging's least (docs/blueprints.md, "Masses"). */
	public static final double EMPTY_HOOK = 0.5;

	/**
	 * The steps of a lift of {@code tonnes} (on the hook) from {@code pick} to {@code set}, starting with the hook at {@code at} and
	 * going over the site at {@code clearance} m. Pick and set heights are where the load's bottom stands.
	 */
	public static List<Step> plan(CraneKind kind, int boom, Hook at, Hook pick, Hook set, double clearance, double tonnes) {
		List<Step> steps = new ArrayList<>();
		double light = kind.hoistMetresPerMinute(EMPTY_HOOK);
		double loaded = kind.hoistMetresPerMinute(tonnes);
		// the empty hook to the pick: up over anything first, across, and down, all at once
		double turn = turn(at.degrees(), pick.degrees());
		double across = Math.max(kind.slewSeconds(turn), kind.outSeconds(boom, at.radius(), pick.radius()));
		double vertical = (Math.abs(clearance - at.height()) + Math.abs(clearance - pick.height())) / light * 60;
		double toPickWatts = (turn != 0 ? kind.slewWatts() : 0) + (at.radius() != pick.radius() ? kind.outWatts() : 0)
				+ kind.hoistWatts(EMPTY_HOOK, light);
		steps.add(new Step(Phase.TO_PICK, Math.max(across, vertical), at, pick, false, toPickWatts));
		steps.add(new Step(Phase.RIG, CraneKind.RIG_SECONDS, pick, pick, false, 0));
		Hook up = new Hook(pick.degrees(), pick.radius(), clearance);
		steps.add(new Step(Phase.HOIST, Math.abs(clearance - pick.height()) / loaded * 60, pick, up, true, kind.hoistWatts(tonnes, loaded)));
		double swing = turn(pick.degrees(), set.degrees());
		Hook over = new Hook(pick.degrees() + swing, set.radius(), clearance);
		double moved = Math.max(kind.slewSeconds(swing), kind.outSeconds(boom, pick.radius(), set.radius()));
		steps.add(new Step(Phase.SLEW, moved, up, over, true, kind.slewWatts() + (pick.radius() != set.radius() ? kind.outWatts() : 0)));
		Hook down = new Hook(over.degrees(), set.radius(), set.height());
		steps.add(new Step(Phase.LOWER, Math.abs(clearance - set.height()) / loaded * 60, over, down, true, 0));
		steps.add(new Step(Phase.SET, CraneKind.RIG_SECONDS, down, down, true, 0));
		return steps;
	}

	/** The shorter way round from {@code from} to {@code to}, degrees, -180 to 180. */
	public static double turn(double from, double to) {
		double d = (to - from) % 360;
		if (d > 180) {
			d -= 360;
		} else if (d < -180) {
			d += 360;
		}
		return d;
	}

	/** The whole lift, s. */
	public static double seconds(List<Step> steps) {
		double s = 0;
		for (Step step : steps) {
			s += step.seconds();
		}
		return s;
	}
}
