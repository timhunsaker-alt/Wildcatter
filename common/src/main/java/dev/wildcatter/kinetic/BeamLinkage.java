package dev.wildcatter.kinetic;

/**
 * The pumpjack's four-bar linkage (docs/multiblocks/well-site.md): the crank turns about its centre, the pitman joins the crank pin
 * to the tail of the walking beam, and the beam rocks on the saddle bearing so the horsehead lifts the polished rod. Plain math in
 * the pumpjack's own frame, facing north: z toward the back (south), y up, metres from the corner of its main cell; the well is at
 * z = -0.5. Plain Java, so it is unit-tested without Minecraft.
 */
public final class BeamLinkage {
	/** Saddle bearing to the horsehead face (the front arm). */
	public static final double FRONT_ARM = 2.8;
	/** Saddle bearing to the equalizer at the tail (the back arm). */
	public static final double BACK_ARM = 2.4;
	public static final double CRANK_RADIUS = 0.59;
	public static final double PITMAN = 2.9;
	public static final double SADDLE_Y = 4.3;
	public static final double SADDLE_Z = -0.5 + FRONT_ARM;   // over the well, a front arm back
	public static final double CRANK_Y = 1.5;
	public static final double CRANK_Z = SADDLE_Z + BACK_ARM;

	private BeamLinkage() {
	}

	/** The crank pin {z, y} with the crank at {@code crank} radians (a crank turned about x by that angle, drawn pin-back at zero). */
	public static double[] pin(double crank) {
		return new double[] {CRANK_Z + CRANK_RADIUS * Math.cos(crank), CRANK_Y - CRANK_RADIUS * Math.sin(crank)};
	}

	/** The equalizer at the beam's tail {z, y}: where the circle of the back arm about the saddle meets the pitman's about the pin. */
	public static double[] tail(double crank) {
		double[] p = pin(crank);
		double dz = p[0] - SADDLE_Z, dy = p[1] - SADDLE_Y;
		double d = Math.hypot(dz, dy);
		double a = (BACK_ARM * BACK_ARM - PITMAN * PITMAN + d * d) / (2 * d);
		double h = Math.sqrt(Math.max(0, BACK_ARM * BACK_ARM - a * a));
		double mz = SADDLE_Z + a * dz / d, my = SADDLE_Y + a * dy / d;
		double z1 = mz + h * dy / d, y1 = my - h * dz / d;
		double z2 = mz - h * dy / d, y2 = my + h * dz / d;
		return z1 >= z2 ? new double[] {z1, y1} : new double[] {z2, y2};   // the tail is the one behind the saddle
	}

	/** The beam's tilt in radians, tail up positive. */
	public static double beam(double crank) {
		double[] t = tail(crank);
		return Math.atan2(t[1] - SADDLE_Y, t[0] - SADDLE_Z);
	}

	/** How far the polished rod is above where it hangs with the beam level: the horsehead's arc, so the front arm times the tilt. */
	public static double rod(double crank) {
		return -FRONT_ARM * beam(crank);
	}

	/** The pitman's angle about x from straight up, in radians, for a pitman drawn standing up from the crank pin. */
	public static double pitman(double crank) {
		double[] p = pin(crank), t = tail(crank);
		return Math.atan2(t[0] - p[0], t[1] - p[1]);
	}
}
