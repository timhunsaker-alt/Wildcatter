package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The nuclear island's plant model (docs/multiblocks/reactor.md): a 9-assembly core under point kinetics, its rods and boron, the
 * two loops' heat balance, the pressurizer and the two steam generators' secondary sides. The vessel's block entity steps it each
 * interval with what its parts give (pump flows, heater and charging strength, feedwater in, steam drawn) and reads back what they
 * should do. Temperatures in C, pressures in MPa, masses in kg. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Reactor {
	/** The console's setpoints; the first is TRIP. */
	public static final double[] SETPOINTS = {-1, 0, 0.02, 0.05, 0.10, 0.25, 0.50, 0.75, 1.00};

	/** What the parts give the core this step. Masses are kg over the step. */
	public static final class Inputs {
		public boolean crdm;                          // the CRDM head is powered
		public boolean pressurizer;                   // a pressurizer is connected
		public double heaters;                        // 0 to 1: how hard its heaters run
		public final boolean[] loop = new boolean[2]; // a steam generator on the loop
		public final double[] pump = new double[2];   // 0 to 1: the pump's flow, its flywheel included
		public final double[] pumpPower = new double[2];   // 0 to 1: how hard its motor runs
		public final double[] feed = new double[2];   // feedwater in
		public final double[] steamTaken = new double[2];  // steam drawn by its pipe
		public boolean cvcs;
		public double charging;                       // 0 to 1: the charging pump
		public double boricAcid;                      // kg in the CVCS's tank
		public double water;                          // kg of makeup water the CVCS holds
	}

	// fuel
	private final double[] burnup = new double[Tuning.CORE_ASSEMBLIES];   // GWd/t
	private int assemblies;
	private int rccas;
	// rods
	private double shutdownBank;
	private double bankD;
	private int setpoint;                        // index into SETPOINTS
	private boolean tripped = true;
	private String trip = "shut down";
	private boolean lowRangeBlocked;
	// coolant
	private double waterKg;
	private double ppm;
	private double targetPpm = 1500;
	private double tavg = 20;
	private double thot = 20;
	private double tcold = 20;
	private double pzrC = 20;
	private double flow;
	private final double[] sgC = {20, 20};
	private final double[] sgKg = {Tuning.SG_WATER_KG, Tuning.SG_WATER_KG};
	private final double[] lastSteam = new double[2];
	private final double[] vented = new double[2];
	private final PointKinetics kinetics = new PointKinetics();
	// what the CVCS did this step: boric acid used (negative: recovered) and makeup water used
	private double boricAcidUsed;
	private double waterUsed;
	private String cvcsState = "idle";
	private String held = "";

	// ---- numbers -----------------------------------------------------------------------------------------------------------------
	/** W of heat an assembly makes at full power: its rods' length at the average linear heat rate. */
	public static double assemblyWatts() {
		return Tuning.ASSEMBLY_RODS * Tuning.ROD_STACK_M * Tuning.LINEAR_HEAT_KW_M * 1000;
	}

	/** W of heat the core makes at full power. */
	public static double ratedWatts() {
		return Tuning.CORE_ASSEMBLIES * assemblyWatts();
	}

	/** GWd/t a second at full power, with the burnup speed-up. */
	public static double burnupPerSecond() {
		return assemblyWatts() / 1e6 / (Fuel.assemblyU() / 1000) / 86400 / 1000 * Tuning.BURNUP_SPEEDUP;
	}

	/** Hours at full power of one cycle, a third of the discharge burnup. */
	public static double cycleHours() {
		return Tuning.DISCHARGE_GWD_T / 3 / burnupPerSecond() / 3600;
	}

	/** kg/s of steam a steam generator makes at full power. */
	public static double steamPerGenerator() {
		return ratedWatts() / 2 / (Steam.hg(Steam.psat(Tuning.TAVG_FULL_LOAD - ratedWatts() / 2 / Tuning.SG_UA_W_PER_C)) - Steam.hf(Tuning.FEEDWATER_C));
	}

	/** kg/s of coolant a loop carries for the core's rated delta-T. */
	public static double loopFlow() {
		return ratedWatts() / 2 / (Steam.CP_COOLANT * Tuning.CORE_DT);
	}

	/** The rods' worth with the most reactive one (the centre) stuck out, less the power defect at full power: the shutdown margin. */
	public static double shutdownMargin() {
		double rods = 4 * Tuning.ROD_WORTH_EDGE;
		double defect = -Tuning.DOPPLER_PER_C * Tuning.FUEL_RISE_C - Tuning.MTC_AT_0_PPM * (Tuning.TAVG_FULL_LOAD - Tuning.TAVG_NO_LOAD);
		return rods - defect;
	}

	/** The fraction of a rod's worth still in at {@code steps} withdrawn: the S-curve. */
	static double inserted(double steps) {
		double x = Math.max(0, Math.min(1, steps / Tuning.ROD_STEPS));
		return 1 - (x - Math.sin(2 * Math.PI * x) / (2 * Math.PI));
	}

	/** The moderator temperature coefficient at {@code ppm}, per C: linear between its two points, never positive. */
	static double mtc(double ppm) {
		return Math.min(0, Tuning.MTC_AT_0_PPM + (Tuning.MTC_AT_1500_PPM - Tuning.MTC_AT_0_PPM) * ppm / 1500);
	}

	// ---- state ---------------------------------------------------------------------------------------------------------------------
	public double power() {
		return kinetics.power();
	}

	public double startupRate() {
		return kinetics.startupRate();
	}

	public boolean tripped() {
		return tripped;
	}

	public String trip() {
		return trip;
	}

	public double tavg() {
		return tavg;
	}

	public double thot() {
		return thot;
	}

	public double tcold() {
		return tcold;
	}

	public double pressure() {
		return waterKg < Tuning.RCS_WATER_KG - 1 ? 0.1 : Math.max(0.1, Steam.psat(pzrC));
	}

	public double ppm() {
		return ppm;
	}

	public double targetPpm() {
		return targetPpm;
	}

	public double bankD() {
		return bankD;
	}

	public double shutdownBank() {
		return shutdownBank;
	}

	public int assemblies() {
		return assemblies;
	}

	public int rccas() {
		return rccas;
	}

	public boolean full() {
		return waterKg >= Tuning.RCS_WATER_KG - 1;
	}

	public double waterKg() {
		return waterKg;
	}

	public double sgLevel(int loop) {
		return sgKg[loop] / Tuning.SG_WATER_KG;
	}

	public double sgPressure(int loop) {
		return Steam.psat(sgC[loop]);
	}

	public double vented(int loop) {
		return vented[loop];
	}

	public double burnup(int i) {
		return burnup[i];
	}

	public double setpoint() {
		return SETPOINTS[setpoint];
	}

	public double boricAcidUsed() {
		return boricAcidUsed;
	}

	public double waterUsed() {
		return waterUsed;
	}

	public String cvcsState() {
		return cvcsState;
	}

	/** Why the rods are held in, or empty. */
	public String held() {
		return held;
	}

	/** Whether the pressurizer's heaters should run. */
	public boolean wantsHeaters() {
		return full() && pressure() < Tuning.PZR_HEATERS_ON_MPA;
	}

	/** kg a steam generator wants from its feedwater pipe over a step of {@code seconds}. */
	public double feedWanted(int loop, double seconds) {
		return Math.max(0, Tuning.SG_WATER_KG - sgKg[loop] + lastSteam[loop]);
	}

	/** Whether a steam generator has steam for its pipe. */
	public boolean steaming(int loop) {
		return sgPressure(loop) > 1.0;
	}

	public boolean loadable(boolean pumpsStopped) {
		return tavg < Tuning.LOADING_MAX_C && pressure() < 0.5 && pumpsStopped;
	}

	/** Loads a fresh assembly; false when the core is full. */
	public boolean loadAssembly(double gwd) {
		if (assemblies >= Tuning.CORE_ASSEMBLIES) {
			return false;
		}
		burnup[assemblies++] = gwd;
		return true;
	}

	/** Takes the last assembly back out if it is fresh; returns false otherwise. */
	public boolean unloadFreshAssembly() {
		if (assemblies == 0 || burnup[assemblies - 1] > 0) {
			return false;
		}
		assemblies--;
		return true;
	}

	public boolean loadRcca() {
		if (rccas >= Tuning.RCCAS) {
			return false;
		}
		rccas++;
		return true;
	}

	public boolean unloadRcca() {
		if (rccas == 0 || shutdownBank > 0 || bankD > 0) {
			return false;
		}
		rccas--;
		return true;
	}

	/** A sneak-click on the console: the next setpoint. Leaving TRIP asks for a reset. */
	public double nextSetpoint() {
		setpoint = (setpoint + 1) % SETPOINTS.length;
		if (setpoint == 0) {
			tripNow("MANUAL TRIP");
		}
		return SETPOINTS[setpoint];
	}

	/** A sneak-click on the CVCS: the boron target up 50 ppm, wrapping to 0 past 2,500. */
	public double nextTarget() {
		targetPpm = targetPpm >= 2500 ? 0 : targetPpm + 50;
		return targetPpm;
	}

	public void setTarget(double ppm) {
		targetPpm = ppm;
	}

	public void setSetpoint(double fraction) {
		for (int i = 0; i < SETPOINTS.length; i++) {
			if (Math.abs(SETPOINTS[i] - fraction) < 1e-9) {
				setpoint = i;
				if (i == 0) {
					tripNow("MANUAL TRIP");
				}
			}
		}
	}

	/** For the game test and unit tests: filled and borated, at hot standby with the pressurizer at its pressure. */
	public void setHotStandbyForTest(double ppm) {
		waterKg = Tuning.RCS_WATER_KG;
		this.ppm = ppm;
		targetPpm = ppm;
		tavg = thot = tcold = Tuning.TAVG_NO_LOAD;
		pzrC = Steam.tsat(Tuning.RCS_MPA);
		sgC[0] = sgC[1] = Tuning.TAVG_NO_LOAD - 0.3;
	}

	/** For the game test: the banks at these steps. */
	public void setRodsForTest(double shutdown, double d) {
		shutdownBank = shutdown;
		bankD = d;
	}

	/** For the game test: the core steady at {@code power}. */
	public void holdPowerForTest(double power) {
		kinetics.hold(power);
	}

	private void tripNow(String why) {
		if (!tripped) {
			tripped = true;
			trip = why;
		}
		setpoint = 0;
	}

	// ---- the step ----------------------------------------------------------------------------------------------------------------
	/** Steps the plant {@code dt} seconds. */
	public void step(Inputs in, double dt) {
		boricAcidUsed = 0;
		waterUsed = 0;
		double before = tavg;
		boolean anyPump = in.pump[0] > 0.5 || in.pump[1] > 0.5;
		boolean ready = assemblies == Tuning.CORE_ASSEMBLIES && rccas == Tuning.RCCAS && full() && anyPump && pressure() > Tuning.TRIP_LOW_MPA;
		held = assemblies < Tuning.CORE_ASSEMBLIES ? String.format("LOADING: %d of %d assemblies", assemblies, Tuning.CORE_ASSEMBLIES)
				: rccas < Tuning.RCCAS ? String.format("LOADING: %d of %d RCCAs", rccas, Tuning.RCCAS)
				: !full() ? "RCS NOT FULL" : !anyPump ? "NO COOLANT PUMP" : pressure() <= Tuning.TRIP_LOW_MPA ? "PRESSURE BELOW 12.96 MPa" : "";

		// the rods
		double drop = Tuning.ROD_STEPS / Tuning.ROD_DROP_S * dt;
		double move = Tuning.ROD_STEPS_PER_S * dt;
		if (tripped || !in.crdm) {
			shutdownBank = Math.max(0, shutdownBank - drop);
			bankD = Math.max(0, bankD - drop);
		} else if (!ready) {
			bankD = Math.max(0, bankD - move);
		} else if (shutdownBank < Tuning.ROD_STEPS) {
			shutdownBank = Math.min(Tuning.ROD_STEPS, shutdownBank + move);
		} else {
			double want = SETPOINTS[setpoint];
			double p = power();
			double rate = startupRate();
			if (p > want * 1.02 || rate > Tuning.SUR_INSERT_DPM) {
				bankD = Math.max(0, bankD - move);
			} else if (p < want * 0.98 && rate < Tuning.SUR_WITHDRAW_DPM) {
				bankD = Math.min(Tuning.ROD_STEPS, bankD + move);
			}
		}

		cvcs(in, dt);

		// reactivity and kinetics
		double fuel = 0;
		for (int i = 0; i < assemblies; i++) {
			fuel += Tuning.ASSEMBLY_RHO_FRESH - Tuning.ASSEMBLY_RHO_PER_GWD * burnup[i];
		}
		fuel = assemblies == 0 ? -1 : fuel / Tuning.CORE_ASSEMBLIES - (Tuning.CORE_ASSEMBLIES - assemblies) * 0.05;
		double centre = rccas > 0 ? Tuning.ROD_WORTH_CENTRE : 0;
		double edges = Math.max(0, rccas - 1) * Tuning.ROD_WORTH_EDGE;
		double rods = centre * inserted(bankD) + edges * inserted(shutdownBank);
		double tfuel = tavg + Tuning.FUEL_RISE_C * power();
		double rho = fuel - rods + Tuning.BORON_PER_PPM * ppm + Tuning.DOPPLER_PER_C * (tfuel - Tuning.TAVG_NO_LOAD)
				+ mtc(ppm) * (tavg - Tuning.TAVG_NO_LOAD);
		if (!full()) {
			rho = Math.min(rho, -0.05);   // no moderator
		}
		kinetics.step(rho, dt);
		double p = power();

		// the primary's heat
		// each steam generator's heat over the step (J), worked out as the exponential approach of the two sides so a long step is stable
		double[] f = new double[2];
		double heat = p * ratedWatts();
		double sgHeat = 0;
		double[] q = new double[2];
		double speed = tavg < Tuning.TAVG_NO_LOAD ? Tuning.HEATUP_SPEEDUP : 1;
		int loops = (in.loop[0] ? 1 : 0) + (in.loop[1] ? 1 : 0);
		for (int l = 0; l < 2; l++) {
			f[l] = in.loop[l] ? Math.max(in.pump[l], Tuning.NATURAL_CIRCULATION) : 0;
			heat += in.pumpPower[l] * Tuning.RCP_WATTS * Tuning.RCP_HEAT;
			if (in.loop[l]) {
				double wet = Math.min(1, sgKg[l] / (0.3 * Tuning.SG_WATER_KG));
				double ua = Tuning.SG_UA_W_PER_C * Math.pow(f[l], 0.8) * wet;
				double primary = Tuning.RCS_HEAT_J_PER_C / speed / loops;
				double secondary = Tuning.SG_HEAT_J_PER_C / (sgC[l] < Tuning.TAVG_NO_LOAD ? Tuning.HEATUP_SPEEDUP : 1);
				double both = 1 / primary + 1 / secondary;
				q[l] = (tavg - sgC[l]) * (1 - Math.exp(-ua * both * dt)) / both;
				sgHeat += q[l];
			}
		}
		flow = (f[0] + f[1]) / 2;
		double loss = Tuning.RCS_LOSS_W * (tavg - 20) / (Tuning.TAVG_NO_LOAD - 20);
		if (full()) {
			double joules = (heat - loss) * dt - sgHeat;
			tavg += joules * (joules > 0 ? speed : 1) / Tuning.RCS_HEAT_J_PER_C;   // the heat-up is compressed, a cool-down is not
		}
		double dt33 = p * ratedWatts() / (Math.max(flow, Tuning.NATURAL_CIRCULATION) * 2 * loopFlow() * Steam.CP_COOLANT);
		thot = tavg + dt33 / 2;
		tcold = tavg - dt33 / 2;

		// the steam generators' secondary sides
		for (int l = 0; l < 2; l++) {
			vented[l] = 0;
			if (!in.loop[l]) {
				continue;
			}
			double mpa = Steam.psat(sgC[l]);
			double vent = mpa > Tuning.SG_ADV_MPA ? Math.min(Tuning.SG_ADV_KGS, Tuning.SG_ADV_KGS * (mpa - Tuning.SG_ADV_MPA) / 0.1) * dt : 0;
			vent = Math.min(vent, sgKg[l]);
			vented[l] = vent;
			double out = Math.min(in.steamTaken[l] + vent, sgKg[l]);
			lastSteam[l] = out;
			double latent = Steam.hg(mpa) - Steam.hf(sgC[l]);
			double sub = Steam.hf(sgC[l]) - Steam.hf(Tuning.FEEDWATER_C);
			double sgSpeed = sgC[l] < Tuning.TAVG_NO_LOAD ? Tuning.HEATUP_SPEEDUP : 1;
			double joules = q[l] - out * latent - in.feed[l] * sub;
			sgC[l] += joules * (joules > 0 ? sgSpeed : 1) / Tuning.SG_HEAT_J_PER_C;
			sgKg[l] = Math.max(0, sgKg[l] + in.feed[l] - out);
		}

		// the pressurizer
		if (!in.pressurizer) {
			pzrC = Math.max(Math.min(pzrC, tavg), pzrC - 2 * dt);
		} else if (full()) {
			double pzrSpeed = tavg < Tuning.TAVG_NO_LOAD ? Tuning.HEATUP_SPEEDUP : 1;
			double gain = in.heaters * Tuning.PZR_HEATER_WATTS - Tuning.PZR_LOSS_W * (pzrC - 20) / (344.8 - 20);
			pzrC += gain * dt * (gain > 0 ? pzrSpeed : 1) / Tuning.PZR_HEAT_J_PER_C + Tuning.PZR_SURGE * (tavg - before);
			if (pressure() > Tuning.PZR_SPRAY_MPA && anyPump) {
				pzrC = Math.max(tcold, pzrC - Tuning.PZR_SPRAY_C_PER_S * dt);
			}
			pzrC = Math.min(pzrC, Steam.tsat(Tuning.PORV_MPA));
		} else {
			pzrC = tavg;
		}

		// burnup
		for (int i = 0; i < assemblies; i++) {
			burnup[i] += p * burnupPerSecond() * dt;
		}

		// the trips
		double mpa = pressure();
		if (p > Tuning.P7) {
			lowRangeBlocked = true;
		} else if (p < Tuning.P7 * 0.9) {
			lowRangeBlocked = false;
		}
		if (!in.crdm && (shutdownBank > 0 || bankD > 0 || !tripped)) {
			tripNow("CRDM POWER LOST");
		} else if (p > Tuning.TRIP_FLUX) {
			tripNow("HIGH FLUX (109 %)");
		} else if (!lowRangeBlocked && p > Tuning.TRIP_FLUX_LOW) {
			tripNow("HIGH FLUX, LOW SETPOINT (25 %)");
		} else if (p > Tuning.P7 && flow < Tuning.TRIP_LOW_FLOW) {
			tripNow("LOW RCS FLOW");
		} else if (p > Tuning.P8 && (in.pump[0] < Tuning.TRIP_LOW_FLOW || in.pump[1] < Tuning.TRIP_LOW_FLOW)) {
			tripNow("REACTOR COOLANT PUMP LOST");
		} else if (p > Tuning.P7 && mpa < Tuning.TRIP_LOW_MPA) {
			tripNow("LOW PRESSURIZER PRESSURE");
		} else if (mpa > Tuning.TRIP_HIGH_MPA) {
			tripNow("HIGH PRESSURIZER PRESSURE");
		} else if (full() && (thot > Tuning.TRIP_THOT_C || thot > Steam.tsat(mpa) - 3) && (shutdownBank > 0 || !tripped)) {
			tripNow("OVERTEMPERATURE");
		} else if ((in.loop[0] && sgLevel(0) < Tuning.SG_LOW_LOW) || (in.loop[1] && sgLevel(1) < Tuning.SG_LOW_LOW)) {
			tripNow("STEAM GENERATOR LOW-LOW LEVEL");
		} else if (tripped && setpoint > 0) {
			tripped = false;
			trip = "";
		}
	}

	private void cvcs(Inputs in, double dt) {
		if (!in.cvcs) {
			cvcsState = "NOT BUILT";
			return;
		}
		if (in.charging <= 0) {
			cvcsState = "charging pump stopped";
			return;
		}
		double baPerPpmKg = 1e-6 / Tuning.B_IN_BORIC_ACID;   // kg of boric acid a ppm a kg of water
		if (!full()) {
			double kg = Math.min(Math.min(Tuning.RCS_FILL_KGS * in.charging * dt, in.water), Tuning.RCS_WATER_KG - waterKg);
			double acid = targetPpm * kg * baPerPpmKg;
			if (acid > in.boricAcid) {
				cvcsState = "NO BORIC ACID: filling waits for bags in its hopper";
				return;
			}
			if (kg <= 1e-9) {
				cvcsState = "NO WATER: filling waits for its makeup pump";
				return;
			}
			ppm = (ppm * waterKg + targetPpm * kg) / (waterKg + kg);
			waterKg += kg;
			boricAcidUsed = acid;
			waterUsed = kg;
			cvcsState = String.format("filling the RCS at %.0f kg/s, %.0f ppm: %.0f of %.0f t", kg / dt, targetPpm, waterKg / 1000, Tuning.RCS_WATER_KG / 1000);
			return;
		}
		double kg = Tuning.CVCS_KGS * in.charging * dt;
		if (ppm < targetPpm - 0.5) {
			double gain = Math.min((Tuning.BORIC_ACID_TANK_PPM - ppm) * kg / waterKg, targetPpm - ppm);
			double acid = gain * waterKg * baPerPpmKg;
			if (acid > in.boricAcid) {
				cvcsState = "NO BORIC ACID: put bags in its hopper";
				return;
			}
			ppm += gain;
			boricAcidUsed = acid;
			cvcsState = String.format("borating toward %.0f ppm", targetPpm);
		} else if (ppm > targetPpm + 0.5) {
			if (in.water < kg) {
				cvcsState = "NO WATER: diluting waits for its makeup pump";
				return;
			}
			double loss = Math.min(ppm * kg / waterKg, ppm - targetPpm);
			ppm -= loss;
			boricAcidUsed = -loss * waterKg * baPerPpmKg;   // recovered from the letdown
			waterUsed = kg;
			cvcsState = String.format("diluting toward %.0f ppm", targetPpm);
		} else {
			cvcsState = String.format("holding %.0f ppm", ppm);
		}
	}

	// ---- saving ----------------------------------------------------------------------------------------------------------------
	/** Where the vessel keeps the state. */
	public interface Store {
		void put(String key, double value);

		double get(String key, double fallback);
	}

	public void save(Store out) {
		for (int i = 0; i < burnup.length; i++) {
			out.put("burnup" + i, burnup[i]);
		}
		out.put("assemblies", assemblies);
		out.put("rccas", rccas);
		out.put("shutdownBank", shutdownBank);
		out.put("bankD", bankD);
		out.put("setpoint", setpoint);
		out.put("tripped", tripped ? 1 : 0);
		out.put("waterKg", waterKg);
		out.put("ppm", ppm);
		out.put("targetPpm", targetPpm);
		out.put("tavg", tavg);
		out.put("pzrC", pzrC);
		for (int l = 0; l < 2; l++) {
			out.put("sgC" + l, sgC[l]);
			out.put("sgKg" + l, sgKg[l]);
		}
		out.put("power", kinetics.power());
		for (int i = 0; i < 6; i++) {
			out.put("precursor" + i, kinetics.precursors()[i]);
		}
	}

	public void load(Store in) {
		for (int i = 0; i < burnup.length; i++) {
			burnup[i] = in.get("burnup" + i, 0);
		}
		assemblies = (int) in.get("assemblies", 0);
		rccas = (int) in.get("rccas", 0);
		shutdownBank = in.get("shutdownBank", 0);
		bankD = in.get("bankD", 0);
		setpoint = (int) in.get("setpoint", 0);
		tripped = in.get("tripped", 1) > 0.5;
		trip = tripped ? "shut down" : "";
		waterKg = in.get("waterKg", 0);
		ppm = in.get("ppm", 0);
		targetPpm = in.get("targetPpm", 1500);
		tavg = thot = tcold = in.get("tavg", 20);
		pzrC = in.get("pzrC", 20);
		for (int l = 0; l < 2; l++) {
			sgC[l] = in.get("sgC" + l, 20);
			sgKg[l] = in.get("sgKg" + l, Tuning.SG_WATER_KG);
		}
		double[] c = new double[6];
		for (int i = 0; i < 6; i++) {
			c[i] = in.get("precursor" + i, 0);
		}
		kinetics.load(in.get("power", 0), c);
		if (in.get("precursor0", -1) < 0) {
			kinetics.hold(Tuning.NEUTRON_SOURCE / 0.05);
		}
	}
}
