package dev.wildcatter.solver;

/**
 * The frequency of one electrical island (a set of connected generators and loads). No Minecraft in here.
 *
 * Generators spin together, so there is one frequency. It falls when the load takes more electrical power than the
 * turbines supply and rises when they supply more (the swing equation, 2H df/dt = Pmech - Pelec, per unit on the generators'
 * rating). The governor opens the valve as the frequency falls (droop: a 4% fall in frequency takes a generator from no load to
 * full load) up to the generator's rating. A slow secondary control (AGC) then moves the governor's setpoint to the load so the
 * frequency returns to nominal.
 */
public final class FrequencyIsland {
	private final double nominal;
	private final double droop;
	private final double agcSeconds;
	private double hertz;
	/** The governor's setpoint as a fraction of rated power. */
	private double reference;
	private boolean started;

	public FrequencyIsland(double nominal, double droop, double agcSeconds) {
		this.nominal = nominal;
		this.droop = droop;
		this.agcSeconds = agcSeconds;
		this.hertz = nominal;
	}

	public double hertz() {
		return hertz;
	}

	/**
	 * Advances {@code dt} seconds.
	 * @param ratedWatts total rating of the generators online
	 * @param inertiaSeconds their combined inertia constant H (weighted by rating)
	 * @param electricalWatts what the load is taking from them
	 */
	public void step(double dt, double ratedWatts, double inertiaSeconds, double electricalWatts) {
		if (ratedWatts <= 0 || dt <= 0) {
			return;
		}
		double pe = electricalWatts / ratedWatts;
		if (!started) {
			reference = Math.max(0, Math.min(1, pe));   // the governor is synchronised at the load it finds
			started = true;
		}
		double x = (hertz - nominal) / nominal;
		double twoH = 2.0 * Math.max(0.1, inertiaSeconds);
		// implicit in the droop term, so the step is stable however long it is
		double next = (x + dt / twoH * (reference - pe)) / (1.0 + dt / (twoH * droop));
		double pm = reference - next / droop;
		if (pm < 0 || pm > 1) {                      // the valve is shut or fully open: no more help from the governor
			pm = Math.max(0, Math.min(1, pm));
			next = x + dt / twoH * (pm - pe);
		}
		hertz = Math.max(nominal * 0.5, Math.min(nominal * 1.5, nominal * (1.0 + next)));
		reference += (pe - reference) * Math.min(1.0, dt / agcSeconds);
		reference = Math.max(0, Math.min(1, reference));
	}
}
