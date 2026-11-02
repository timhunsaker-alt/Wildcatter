package dev.wildcatter.fluid;

/**
 * Water and steam at saturation (docs/multiblocks/reactor.md), from IAPWS-IF97: the saturation line is its region 4 equation, exact;
 * the enthalpies are read from the IF97 tables between their points. Temperatures in C, pressures in MPa, enthalpies in J/kg.
 * Plain Java, so it is unit-tested without Minecraft.
 */
public final class Steam {
	private Steam() {
	}

	private static final double[] N = {0, 0.11670521452767e4, -0.72421316703206e6, -0.17073846940092e2, 0.12020824702470e5,
			-0.32325550322333e7, 0.14915108613530e2, -0.48232657361591e4, 0.40511340542057e6, -0.23855557567849, 0.65017534844798e3};

	/** The specific heat of the reactor's coolant near 15.5 MPa and 290-325 C, J/kg C. */
	public static final double CP_COOLANT = 5450.0;

	/** The saturation pressure at {@code c} C (IF97 region 4), MPa; from 1 to 373.9 C. */
	public static double psat(double c) {
		double t = Math.max(1, Math.min(373.9, c)) + 273.15;
		double th = t + N[9] / (t - N[10]);
		double a = th * th + N[1] * th + N[2];
		double b = N[3] * th * th + N[4] * th + N[5];
		double cc = N[6] * th * th + N[7] * th + N[8];
		return Math.pow(2 * cc / (-b + Math.sqrt(b * b - 4 * a * cc)), 4);
	}

	/** The saturation temperature at {@code mpa}, C (IF97 region 4); from 0.0007 to 22 MPa. */
	public static double tsat(double mpa) {
		double beta = Math.pow(Math.max(0.000_7, Math.min(22.0, mpa)), 0.25);
		double e = beta * beta + N[3] * beta + N[6];
		double f = N[1] * beta * beta + N[4] * beta + N[7];
		double g = N[2] * beta * beta + N[5] * beta + N[8];
		double d = 2 * g / (-f - Math.sqrt(f * f - 4 * e * g));
		return (N[10] + d - Math.sqrt((N[10] + d) * (N[10] + d) - 4 * (N[9] + N[10] * d))) / 2 - 273.15;
	}

	private static final double[] HF_C = {0, 20, 50, 100, 150, 200, 225, 250, 275, 300, 320, 340, 350, 370};
	private static final double[] HF = {0.0, 83.9, 209.3, 419.2, 632.2, 852.3, 966.8, 1085.8, 1210.7, 1344.8, 1461.5, 1594.6, 1670.9, 1888.4};
	private static final double[] HG_MPA = {0.1, 0.5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 18};
	private static final double[] HG = {2674.9, 2748.1, 2777.1, 2798.3, 2803.2, 2800.8, 2794.2, 2784.6, 2772.6, 2758.7, 2742.9, 2725.5, 2685.4, 2610.9, 2509.1};

	/** The enthalpy of saturated (or compressed, near enough) water at {@code c} C, J/kg. */
	public static double hf(double c) {
		return table(HF_C, HF, c) * 1000;
	}

	/** The enthalpy of saturated steam at {@code mpa}, J/kg. */
	public static double hg(double mpa) {
		return table(HG_MPA, HG, mpa) * 1000;
	}

	private static double table(double[] x, double[] y, double at) {
		if (at <= x[0]) {
			return y[0];
		}
		for (int i = 1; i < x.length; i++) {
			if (at <= x[i]) {
				return y[i - 1] + (y[i] - y[i - 1]) * (at - x[i - 1]) / (x[i] - x[i - 1]);
			}
		}
		return y[y.length - 1];
	}
}
