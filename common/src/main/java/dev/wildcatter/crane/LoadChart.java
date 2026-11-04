package dev.wildcatter.crane;

/**
 * A crane's load chart (docs/multiblocks/tower-crane.md, crawler-crane.md): tonnes at each radius from the real chart. Between two
 * radii it gives the farther radius's figure, as a crane operator reads a chart; it never interpolates. Plain Java, so it is
 * unit-tested without Minecraft.
 */
public final class LoadChart {
	private final double[] radii;
	private final double[] tonnes;
	private final double maxRadius;
	/** What the hook block itself may carry, t: no figure is read above it. */
	private final double cap;

	/** {@code radii} rising, each with its tonnes; the hook works from the first radius to {@code maxRadius}. */
	public LoadChart(double[] radii, double[] tonnes, double maxRadius) {
		this(radii, tonnes, maxRadius, Double.MAX_VALUE);
	}

	public LoadChart(double[] radii, double[] tonnes, double maxRadius, double cap) {
		if (radii.length == 0 || radii.length != tonnes.length) {
			throw new IllegalArgumentException("a chart needs a figure at each radius");
		}
		this.radii = radii.clone();
		this.tonnes = tonnes.clone();
		this.maxRadius = maxRadius;
		this.cap = cap;
	}

	public double minRadius() {
		return radii[0];
	}

	public double maxRadius() {
		return maxRadius;
	}

	public boolean reaches(double radius) {
		return radius >= radii[0] - 1e-9 && radius <= maxRadius + 1e-9;
	}

	/** Tonnes the hook may carry at {@code radius} m, hook block and rigging included; 0 outside the chart. */
	public double capacity(double radius) {
		return Math.min(chartFigure(radius), cap);
	}

	/** True if the hook block's own limit is below the chart's figure at {@code radius}. */
	public boolean capped(double radius) {
		return chartFigure(radius) > cap;
	}

	/** The chart's own figure at {@code radius}, before the hook block's limit; 0 outside the chart. */
	public double chartFigure(double radius) {
		if (!reaches(radius)) {
			return 0;
		}
		for (int i = 0; i < radii.length; i++) {
			if (radii[i] >= radius - 1e-9) {
				return tonnes[i];
			}
		}
		return tonnes[tonnes.length - 1];
	}

	/** The chart's figure for display: the radius that is read for {@code radius}. */
	public double readAt(double radius) {
		for (double r : radii) {
			if (r >= radius - 1e-9) {
				return r;
			}
		}
		return radii[radii.length - 1];
	}

	// ---- the Liebherr 380 EC-B 16 Litronic, 75 m jib (BKL data sheet, chart LM1) ----------------------------------------------------
	/** From the trolley against the mast at 2.6 m: 16 t out to 19.6 m, then the chart. */
	private static final LoadChart TOWER = new LoadChart(
			new double[] {2.6, 19.6, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75},
			new double[] {16.0, 16.0, 15.66, 12.15, 9.83, 8.19, 6.96, 6.02, 5.26, 4.64, 4.13, 3.69, 3.32, 3.00}, 75.0);

	public static LoadChart tower() {
		return TOWER;
	}

	// ---- the Liebherr LR 1300, main boom No. 2821.xx, 104 t counterweight and 57 t carbody counterweight -----------------------------
	/** The chart's boom lengths, m. */
	public static final int[] CRAWLER_BOOMS = {20, 23, 26, 32, 38, 44, 50, 56, 62, 68, 74, 80, 86, 92, 95};
	private static final double[] CRAWLER_RADII = {4.3, 5, 6, 7, 8, 9, 10, 16, 20, 24, 26, 32, 38, 44, 50, 55, 60, 65, 70, 75, 80};
	private static final double N = Double.NaN;
	/** Tonnes, one row a radius, one column a boom; NaN where the boom does not work. */
	private static final double[][] CRAWLER = {
			{286.2, N, N, N, N, N, N, N, N, N, N, N, N, N, N},
			{286.2, 300.5, 296.8, N, N, N, N, N, N, N, N, N, N, N, N},
			{258.4, 259.3, 257.5, 245.6, 237.4, 212.6, N, N, N, N, N, N, N, N, N},
			{225.6, 225.6, 222.7, 216.1, 207.6, 198.5, 189.6, 169.5, N, N, N, N, N, N, N},
			{196.1, 196.0, 195.7, 191.4, 185.2, 179.2, 169.5, 162.2, 150.4, 127.8, N, N, N, N, N},
			{173.1, 173.0, 172.7, 172.4, 166.5, 161.0, 153.6, 145.9, 138.9, 125.7, 109.1, 97.2, 83.6, N, N},
			{154.6, 154.6, 154.3, 154.0, 151.6, 144.3, 137.4, 131.1, 125.1, 118.3, 104.7, 96.5, 83.6, 73.1, 67.8},
			{89.3, 89.5, 89.4, 89.5, 88.7, 85.6, 82.5, 79.6, 76.8, 74.1, 71.4, 68.9, 66.4, 62.2, 59.0},
			{65.4, 65.7, 65.7, 65.7, 65.4, 65.2, 64.1, 62.0, 59.9, 57.9, 55.9, 54.0, 52.1, 50.3, 49.4},
			{N, 51.0, 51.1, 51.2, 50.9, 50.6, 50.2, 49.8, 48.4, 46.8, 45.2, 43.7, 42.1, 40.6, 39.8},
			{N, N, 45.7, 45.9, 45.6, 45.3, 44.8, 44.5, 44.0, 42.5, 41.0, 39.6, 38.1, 36.7, 36.0},
			{N, N, N, 34.2, 34.0, 33.8, 33.3, 32.9, 32.4, 31.9, 31.4, 30.2, 29.0, 27.8, 27.2},
			{N, N, N, N, 26.3, 26.2, 25.8, 25.4, 24.8, 24.4, 23.8, 23.3, 22.5, 21.5, 21.0},
			{N, N, N, N, N, 20.7, 20.4, 20.0, 19.5, 19.0, 18.5, 18.0, 17.4, 16.8, 16.2},
			{N, N, N, N, N, N, 16.3, 16.0, 15.5, 15.1, 14.6, 14.1, 13.5, 13.0, 12.7},
			{N, N, N, N, N, N, N, 13.4, 13.0, 12.5, 12.0, 11.5, 10.9, 10.4, 10.1},
			{N, N, N, N, N, N, N, N, 10.7, 10.3, 9.8, 9.3, 8.7, 8.2, 7.9},
			{N, N, N, N, N, N, N, N, N, 8.5, 8.0, 7.5, 6.9, 6.4, 6.1},
			{N, N, N, N, N, N, N, N, N, N, 6.4, 5.9, 5.3, 4.8, 4.6},
			{N, N, N, N, N, N, N, N, N, N, N, 4.6, 4.0, 3.4, 3.0},
			{N, N, N, N, N, N, N, N, N, N, N, N, 2.6, N, N}};

	/** The boom foot: this far ahead of the slewing centre and this high, m (proposed, from the dimension drawing). */
	public static final double BOOM_FOOT_OUT = 1.5, BOOM_FOOT_UP = 2.7;
	/** The boom's lowest working angle, degrees. */
	public static final double BOOM_MIN_DEGREES = 15;

	private static final LoadChart[] CRAWLER_CHARTS = new LoadChart[CRAWLER_BOOMS.length];

	static {
		for (int b = 0; b < CRAWLER_BOOMS.length; b++) {
			java.util.List<double[]> rows = new java.util.ArrayList<>();
			for (int i = 0; i < CRAWLER_RADII.length; i++) {
				if (!Double.isNaN(CRAWLER[i][b])) {
					rows.add(new double[] {CRAWLER_RADII[i], CRAWLER[i][b]});
				}
			}
			double[] r = new double[rows.size()];
			double[] t = new double[rows.size()];
			for (int i = 0; i < r.length; i++) {
				r[i] = rows.get(i)[0];
				t[i] = rows.get(i)[1];
			}
			double reach = BOOM_FOOT_OUT + CRAWLER_BOOMS[b] * Math.cos(Math.toRadians(BOOM_MIN_DEGREES));
			CRAWLER_CHARTS[b] = new LoadChart(r, t, Math.min(r[r.length - 1], reach));
		}
	}

	/** The crawler's chart on boom {@code index} of {@link #CRAWLER_BOOMS}. */
	public static LoadChart crawler(int index) {
		return CRAWLER_CHARTS[index];
	}

	// ---- the Sarens SGC-250, main boom configurations (SGC brochure sgr.d.marc.0180) ------------------------------------------------
	/** The main booms, m. */
	public static final double[] RING_BOOMS = {118, 160.5};
	/** The main hook's safe working load, t: the chart is capped here (docs/multiblocks/ring-crane.md, proposed). */
	public static final double RING_HOOK_SWL = 3_200;
	private static final LoadChart[] RING_CHARTS = {
			new LoadChart(new double[] {40, 50, 60, 70, 80, 90, 100, 110, 120},
					new double[] {5000, 4362, 3831, 3327, 2746, 2321, 2001, 1756, 1554}, 120, RING_HOOK_SWL),
			new LoadChart(new double[] {45, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170},
					new double[] {3114, 3017, 2850, 2694, 2349, 2089, 1866, 1621, 1424, 1260, 1126, 1009, 913, 838}, 170, RING_HOOK_SWL)};

	/** The ring crane's chart on boom {@code index} of {@link #RING_BOOMS}. */
	public static LoadChart ring(int index) {
		return RING_CHARTS[index];
	}
}
