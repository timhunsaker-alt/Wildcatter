package dev.wildcatter.energy;

import dev.wildcatter.Tuning;

/**
 * The cells of one battery container or cabinet (docs/multiblocks/grid-batteries.md, 22b): energy, state of charge, state of health,
 * equivalent full cycles, cell temperature and the BMS. No Minecraft in here, so the unit test can drive it.
 * <p>
 * Power is given at the PCS's AC terminals, positive discharging. Discharging takes {@code ac / (PCS x DC)} out of the cells; charging puts
 * {@code ac x PCS x DC} in (each 98.5 % and 97 %); the MV transformer's losses are the transformer's own, in the solver. The DC losses heat
 * the cells; the chiller (COP 3, proposed) holds them at 25 C while it runs, drawing a third of its rating idle and more with the heat.
 * Fade: {@link Tuning#SOH_FADE_PER_CYCLE} an equivalent full cycle at 25 C and 0.5C or less, doubling every 10 C over 25 C and growing
 * with the charge rate over 0.5C (proposed); no calendar fade.
 */
public final class BessCells {
	/** The chiller's heat removed per watt it draws (proposed). */
	public static final double CHILLER_COP = 3.0;
	/** Without its chiller a container loses heat to the air this slowly, s (proposed: insulated walls). */
	public static final double PASSIVE_TAU_SECONDS = 6 * 3600.0;

	private double ratedKwh;
	private final double heatCapacity;
	private final double chillerWatts;
	private double soc;
	private double soh = 1.0;
	private double cycles;
	private double tempC = Tuning.CELL_AMBIENT_C;
	private boolean overtemp;
	/** The last step's chiller draw, W. */
	private double chillerDraw;

	public BessCells(double ratedKwh, double heatCapacity, double chillerWatts) {
		this.ratedKwh = ratedKwh;
		this.heatCapacity = heatCapacity;
		this.chillerWatts = chillerWatts;
	}

	public double ratedKwh() {
		return ratedKwh;
	}

	/**
	 * Modules loaded into an emptier container (or cabinet): its rating grows by theirs, its health the energy-weighted mean of the old
	 * modules' and the new ones' (100 %), its charge held as energy.
	 */
	public void addModules(double kwh) {
		if (kwh <= 0) {
			return;
		}
		double energy = energyKwh();
		double health = ratedKwh * soh + kwh;
		ratedKwh += kwh;
		soh = health / ratedKwh;
		soc = usableKwh() > 0 ? energy / usableKwh() : 0;
	}

	public void setRated(double kwh) {
		ratedKwh = Math.max(0, kwh);
	}

	/** What it holds at 100 % now, its rating times its health, kWh. */
	public double usableKwh() {
		return ratedKwh * soh;
	}

	public double energyKwh() {
		return usableKwh() * soc;
	}

	public double soc() {
		return soc;
	}

	public double soh() {
		return soh;
	}

	public double cycles() {
		return cycles;
	}

	public double tempC() {
		return tempC;
	}

	public boolean overtemp() {
		return overtemp;
	}

	public boolean endOfLife() {
		return soh < Tuning.SOH_END_OF_LIFE;
	}

	public double chillerDraw() {
		return chillerDraw;
	}

	public double chillerRated() {
		return chillerWatts;
	}

	public void set(double soc, double soh, double cycles, double tempC) {
		this.soc = clamp(soc, 0, 1);
		this.soh = clamp(soh, 0, 1);
		this.cycles = Math.max(0, cycles);
		this.tempC = tempC;
	}

	public void setSoc(double value) {
		soc = clamp(value, 0, 1);
	}

	public void setTemp(double value) {
		tempC = value;
	}

	/** The most it may discharge at its AC terminals now, W, of {@code ratedWatts}: tapering over the window's last 3 %, halved hot, none tripped. */
	public double dischargeLimit(double ratedWatts, double socMin) {
		if (overtemp || soh <= 0) {
			return 0;
		}
		double taper = clamp((soc - socMin) / Tuning.SOC_TAPER, 0, 1);
		return ratedWatts * taper * thermalDerate();
	}

	public double chargeLimit(double ratedWatts, double socMax) {
		if (overtemp || soh <= 0) {
			return 0;
		}
		double taper = clamp((socMax - soc) / Tuning.SOC_TAPER, 0, 1);
		return ratedWatts * taper * thermalDerate();
	}

	private double thermalDerate() {
		return tempC > Tuning.CELL_DERATE_C ? 0.5 : 1.0;
	}

	/**
	 * Runs {@code seconds} at {@code acWatts} (positive discharging) with the chiller on or off. Returns the kWh taken out of (negative) or
	 * put into the cells.
	 */
	public double step(double acWatts, double seconds, boolean chiller) {
		if (seconds <= 0) {
			return 0;
		}
		double dcWatts;
		double heat;
		if (acWatts >= 0) {
			double fromCells = acWatts / (Tuning.PCS_EFFICIENCY * Tuning.BATTERY_DC_EFFICIENCY);
			dcWatts = -fromCells;
			heat = fromCells * (1 - Tuning.BATTERY_DC_EFFICIENCY);
		} else {
			double atCells = -acWatts * Tuning.PCS_EFFICIENCY;
			dcWatts = atCells * Tuning.BATTERY_DC_EFFICIENCY;
			heat = atCells * (1 - Tuning.BATTERY_DC_EFFICIENCY);
		}
		double usableJ = usableKwh() * 3.6e6;
		double joules = dcWatts * seconds;
		double before = soc;
		if (usableJ > 0) {
			soc = clamp(soc + joules / usableJ, 0, 1);
		}
		double movedKwh = (soc - before) * usableKwh();
		// heat
		tempC += heat * seconds / heatCapacity;
		if (chiller) {
			double capacity = chillerWatts * CHILLER_COP * seconds;
			double over = Math.max(0, (tempC - Tuning.CELL_AMBIENT_C) * heatCapacity);
			double removed = Math.min(capacity, over);
			tempC -= removed / heatCapacity;
			double duty = clamp(heat / (chillerWatts * CHILLER_COP), 0, 1);
			chillerDraw = chillerWatts * (Tuning.CHILLER_IDLE_SHARE + (1 - Tuning.CHILLER_IDLE_SHARE) * duty);
		} else {
			tempC += (Tuning.CELL_AMBIENT_C - tempC) * Math.min(1, seconds / PASSIVE_TAU_SECONDS);
			chillerDraw = 0;
		}
		// the BMS: contactors open over 55 C, back once cooled under the derate
		if (tempC > Tuning.CELL_TRIP_C) {
			overtemp = true;
		} else if (overtemp && tempC < Tuning.CELL_DERATE_C) {
			overtemp = false;
		}
		// cycles and fade
		double dCycles = ratedKwh > 0 ? Math.abs(movedKwh) / (2 * ratedKwh) : 0;
		cycles += dCycles;
		double thermal = tempC > Tuning.CELL_AMBIENT_C ? Math.pow(2, (tempC - Tuning.CELL_AMBIENT_C) / 10.0) : 1.0;
		double cRate = ratedKwh > 0 ? Math.abs(acWatts) / 1000.0 / ratedKwh : 0;
		double rate = cRate > 0.5 ? 1 + 2 * (cRate - 0.5) : 1.0;
		soh = Math.max(0, soh - Tuning.SOH_FADE_PER_CYCLE * dCycles * thermal * rate);
		return movedKwh;
	}

	/** Cycles forced through, for the game test: {@code n} full cycles at 25 C and 0.5C or less. */
	public void testCycles(double n, double atTempC) {
		double thermal = atTempC > Tuning.CELL_AMBIENT_C ? Math.pow(2, (atTempC - Tuning.CELL_AMBIENT_C) / 10.0) : 1.0;
		cycles += n;
		soh = Math.max(0, soh - Tuning.SOH_FADE_PER_CYCLE * n * thermal);
	}

	/** New modules: back to 100 %, the count kept. */
	public void renew() {
		soh = 1.0;
	}

	public String describe() {
		return String.format("SOC %.1f %% of %,.0f kWh (SOH %.2f %%, %.0f cycles), cells %.1f C%s%s", soc * 100, usableKwh(), soh * 100, cycles, tempC,
				overtemp ? ", BMS OVERTEMP" : "", endOfLife() ? ", END OF LIFE" : "");
	}

	private static double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(hi, v));
	}
}
