package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The nuclear island's plant model (docs/multiblocks/reactor.md): a 9-assembly core under point kinetics, its rods and boron, the
 * two loops' heat balance, the pressurizer and the two steam generators' secondary sides. The vessel's block entity steps it each
 * interval with what its parts give (pump flows, heater and charging strength, feedwater in, steam drawn) and reads back what they
 * should do. Temperatures in C, pressures in MPa, masses in kg. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Reactor {
	// The state is package-private: ReactorNeutronics, ReactorThermal, ReactorProtection, ReactorCvcs and ReactorSaves step and save it.
	/** The console's setpoints; the first is TRIP. */
	public static final double[] SETPOINTS = {-1, -2, 0, 0.02, 0.05, 0.10, 0.25, 0.50, 0.75, 1.00};
	/** The console's COOLDOWN setpoint: rods in, the dump valves lower the temperature, the RHR takes it to cold shutdown. */
	public static final double COOLDOWN = -2;

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
		public boolean turbineTripped;                // the turbine on its steam has tripped (turbine-island.md)
		public double rhrPump;                        // 0 to 1: the RHR pump (8d)
		public double rhrWaterKgs;                    // circulating water through its exchanger
		public double rhrWaterC = Tuning.LAKE_C;
		public boolean contained;                     // a closed containment takes the hydrogen (19a): no blast of the reactor's own
		public boolean demineralizer;                 // the CVCS's letdown runs through its resin (19b)
	}

	// fuel
	final double[] burnup = new double[Tuning.CORE_ASSEMBLIES];   // GWd/t
	final double[] assay = filled(Tuning.ASSAY_PRODUCT);          // 16c1: each assembly's U-235, from its 30B

	private static double[] filled(double v) {
		double[] a = new double[Tuning.CORE_ASSEMBLIES];
		java.util.Arrays.fill(a, v);
		return a;
	}
	int assemblies;
	int rccas;
	final int[] rccaKind = new int[Tuning.RCCAS];        // 19d: ControlRods.B4C or HAFNIUM, in loading order (the first is bank D's)
	final double[] rccaB10 = new double[Tuning.RCCAS];   // 19d: a B4C RCCA's B-10 burnup at its tip
	// rods
	double shutdownBank;
	double bankD;
	private double rodDemand = -1;
	int setpoint;                        // index into SETPOINTS
	boolean tripped = true;
	String trip = "shut down";
	boolean lowRangeBlocked;
	// coolant
	double waterKg;
	double ppm;
	double targetPpm = 1500;
	double tavg = 20;
	double thot = 20;
	double tcold = 20;
	double pzrC = 20;
	double flow;
	final double[] sgC = {20, 20};
	final double[] sgKg = {Tuning.SG_WATER_KG, Tuning.SG_WATER_KG};
	final double[] lastSteam = new double[2];
	final double[] vented = new double[2];
	final PointKinetics kinetics = new PointKinetics();
	// 8d: decay heat (fractions of full power, by group), iodine and xenon (of their full-power equilibrium), the cooldown, damage
	final double[] decay = new double[Tuning.DECAY_LAMBDA.length];
	double iodine;
	double xenon;
	double cooldownC = Double.NaN;
	double rhrHeat;
	double fuelC = 20;
	double zrOxidized;
	double h2;
	boolean coreDamaged;
	boolean exploded;
	boolean blast;
	// what the CVCS did this step: boric acid used (negative: recovered) and makeup water used
	double boricAcidUsed;
	double waterUsed;
	String cvcsState = "idle";
	String held = "";
	// 19a (each read only with its world rule on, dev.wildcatter.Rules): the heat-up rate in plant time and its excursions; LTOP and the P-T curve;
	// the delta-T trips' filters and setpoints; what the PORVs (and a failed vessel) let go this step
	double heatupRate;
	boolean heatupAlarm;
	int fatigue;
	boolean ltopArmed;
	boolean ptAlarm;
	int ptViolations;
	boolean vesselFailed;
	double otLag = Double.NaN;
	double opLag = Double.NaN;
	double otSetpoint = Double.NaN;
	double opSetpoint = Double.NaN;
	double reliefKg;
	double reliefJ;
	double locaKg;
	// 19b: bent grids and leaking rods by assembly; the coolant's activity, µCi/g dose-equivalent I-131
	final boolean[] bent = new boolean[Tuning.CORE_ASSEMBLIES];
	final int[] ifba = new int[Tuning.CORE_ASSEMBLIES];   // 19c2: each assembly's IFBA rods
	final boolean[] leaking = new boolean[Tuning.CORE_ASSEMBLIES];
	double activity;

	// ---- numbers (ReactorDesign) -----------------------------------------------------------------------------------------------
	public static double assemblyWatts() {
		return ReactorDesign.assemblyWatts();
	}

	public static double ratedWatts() {
		return ReactorDesign.ratedWatts();
	}

	public static double burnupPerSecond() {
		return ReactorDesign.burnupPerSecond();
	}

	public static double cycleHours() {
		return ReactorDesign.cycleHours();
	}

	public static double steamPerGenerator() {
		return ReactorDesign.steamPerGenerator();
	}

	public static double loopFlow() {
		return ReactorDesign.loopFlow();
	}

	public static double shutdownMargin() {
		return ReactorDesign.shutdownMargin();
	}

	static double inserted(double steps) {
		return ReactorDesign.inserted(steps);
	}

	static double mtc(double ppm) {
		return ReactorDesign.mtc(ppm);
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
		return waterKg < 1 ? 0.1 : Math.max(0.1, Steam.psat(pzrC));
	}

	/** The decay heat, a fraction of full power. */
	public double decayHeat() {
		double sum = 0;
		for (double d : decay) {
			sum += d;
		}
		return sum;
	}

	public double xenonPcm() {
		return -Tuning.XENON_WORTH * xenon * 1e5;
	}

	/** Whether the RHR may run: below 177 C and 2.9 MPa. */
	public boolean rhrPermit() {
		return full() && tavg < Tuning.RHR_MAX_C && pressure() < Tuning.RHR_MAX_MPA;
	}

	/** W the RHR exchanger took this step. */
	public double rhrHeat() {
		return rhrHeat;
	}

	public double fuelC() {
		return fuelC;
	}

	public double h2() {
		return h2;
	}

	public boolean coreDamaged() {
		return coreDamaged;
	}

	public boolean exploded() {
		return exploded;
	}

	/** True once, the step the island's hydrogen ignites. */
	public boolean takeBlast() {
		boolean was = blast;
		blast = false;
		return was;
	}

	public boolean coolingDown() {
		return SETPOINTS[setpoint] == COOLDOWN;
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

	/** Whether the pressurizer's heaters should run. With the LTOP rule they hold the pressure under the P-T curve too. */
	public boolean wantsHeaters() {
		double target = coolingDown() ? cooldownMpa() : Tuning.PZR_HEATERS_ON_MPA;
		if (dev.wildcatter.Rules.reactorLtop()) {
			target = Math.min(target, Tuning.LTOP_HEATER_SHARE * ptLimit(tcold));
		}
		return full() && pressure() < target;
	}

	// ---- 19a: the heat-up limit, LTOP and the P-T curve, the delta-T trips (docs/multiblocks/reactor-extensions.md) -----------------
	/** The P-T limit curve (simplified): MPa allowed with the vessel at {@code c} C; no limit from 290 C, where the safety valves cover it. */
	public static double ptLimit(double c) {
		if (c >= Tuning.PT_HIGH_C) {
			return Double.POSITIVE_INFINITY;
		}
		if (c <= Tuning.PT_LOW_C) {
			return Tuning.PT_LOW_MPA;
		}
		return Tuning.PT_LOW_MPA + (Tuning.PT_HIGH_MPA - Tuning.PT_LOW_MPA) * (c - Tuning.PT_LOW_C) / (Tuning.PT_HIGH_C - Tuning.PT_LOW_C);
	}

	/** The heat-up rate in plant time (the real-rate equivalent of the compressed heat-up), C/h, averaged over a minute. */
	public double heatupRate() {
		return heatupRate;
	}

	public boolean heatupAlarm() {
		return heatupAlarm;
	}

	/** Heat-ups over 55 C/h counted on the vessel (they do no damage). */
	public int fatigue() {
		return fatigue;
	}

	public boolean ltopArmed() {
		return ltopArmed;
	}

	/** The console's LTOP ARMED switch. */
	public boolean toggleLtop() {
		ltopArmed = !ltopArmed;
		return ltopArmed;
	}

	/** Below the LTOP enable temperature, filled, and LTOP not armed. */
	public boolean ltopNotArmed() {
		return dev.wildcatter.Rules.reactorLtop() && full() && tcold < Tuning.LTOP_ENABLE_C && !ltopArmed;
	}

	public boolean ptAlarm() {
		return ptAlarm;
	}

	public int ptViolations() {
		return ptViolations;
	}

	public boolean vesselFailed() {
		return vesselFailed;
	}

	/** The core's delta-T, C. */
	public double deltaT() {
		return thot - tcold;
	}

	/** The OT delta-T setpoint, C (NaN without the rule). */
	public double otSetpoint() {
		return otSetpoint;
	}

	public double opSetpoint() {
		return opSetpoint;
	}

	/** kg of steam and water the PORVs (and the safety valves) let go this step, and its enthalpy, J. */
	public double reliefKg() {
		return reliefKg;
	}

	public double reliefJ() {
		return reliefJ;
	}

	/** kg a failed vessel let go into its building this step. */
	public double locaKg() {
		return locaKg;
	}

	/** The building's hydrogen burned explosively (a closed containment works it out, 19a). */
	public void markExploded() {
		exploded = true;
	}

	// ---- 19b: leaking rods and the coolant's activity ----------------------------------------------------------------------------
	/** The coolant's activity, µCi/g dose-equivalent I-131. */
	public double activity() {
		return activity;
	}

	public int leakers() {
		int n = 0;
		for (int i = 0; i < assemblies; i++) {
			n += leaking[i] ? 1 : 0;
		}
		return n;
	}

	public boolean bent(int i) {
		return bent[i];
	}

	public boolean leaking(int i) {
		return leaking[i];
	}

	/** The alarms on the console, each with its rule on; empty when none. */
	public String alarms() {
		StringBuilder out = new StringBuilder();
		if (vesselFailed) {
			out.append("VESSEL FAILED");
		}
		if (heatupAlarm) {
			out.append(out.isEmpty() ? "" : ", ").append("HEAT-UP RATE");
		}
		if (ptAlarm) {
			out.append(out.isEmpty() ? "" : ", ").append("P-T LIMIT");
		}
		if (sdmLow()) {
			out.append(out.isEmpty() ? "" : ", ").append("SDM LOW");
		}
		if (ltopNotArmed()) {
			out.append(out.isEmpty() ? "" : ", ").append("LTOP NOT ARMED");
		}
		if (dev.wildcatter.Rules.reactorContamination() && activity > Tuning.ACTIVITY_LIMIT) {
			out.append(out.isEmpty() ? "" : ", ").append("COOLANT ACTIVITY");
		}
		return out.toString();
	}


	/** The pressure held while cooling down: saturation 30 C over the hot leg, no higher than normal. */
	double cooldownMpa() {
		return Math.min(Tuning.RCS_MPA, Steam.psat(thot + Tuning.PZR_SUBCOOLING_C));
	}

	/** kg a steam generator wants from its feedwater pipe over a step of {@code seconds}. */
	public double feedWanted(int loop, double seconds) {
		return Math.max(0, Tuning.SG_WATER_KG - sgKg[loop] + lastSteam[loop]);
	}

	/** Whether a steam generator has steam for its pipe. */
	public boolean steaming(int loop) {
		return sgPressure(loop) > 1.0;
	}

	/** The share of its steam a steam generator offers its pipe: the turbine's governor valves hold the header at 7.0 MPa. */
	public double headerShare(int loop) {
		return Math.max(0, Math.min(1, (sgPressure(loop) - Tuning.STEAM_HEADER_MPA) / 0.2));
	}

	public boolean loadable(boolean pumpsStopped) {
		return tavg < Tuning.LOADING_MAX_C && pressure() < 0.5 && pumpsStopped;
	}

	/** Loads a fresh assembly; false when the core is full. */
	public boolean loadAssembly(double gwd) {
		return loadAssembly(gwd, Tuning.ASSAY_PRODUCT, false);
	}

	/** 16c1: loads a fresh assembly of this assay; its starting reactivity follows it (Enrichment.freshRho). */
	public boolean loadAssembly(double gwd, double u235) {
		return loadAssembly(gwd, u235, false);
	}

	/** Loads a fresh assembly, with a bent grid nobody found (19b); false when the core is full. */
	public boolean loadAssembly(double gwd, boolean bentGrid) {
		return loadAssembly(gwd, Tuning.ASSAY_PRODUCT, bentGrid);
	}

	/** Loads a fresh assembly of this assay (16c1), with or without a bent grid nobody found (19b); false when the core is full. */
	public boolean loadAssembly(double gwd, double u235, boolean bentGrid) {
		return loadAssembly(gwd, u235, bentGrid, false);
	}

	/** As above, with a rod that leaks from the start (19c1: made without inspection). */
	public boolean loadAssembly(double gwd, double u235, boolean bentGrid, boolean leakingRod) {
		return loadAssembly(gwd, u235, bentGrid, leakingRod, 0);
	}

	/** As above, with {@code ifbaRods} rods carrying the IFBA burnable absorber (19c2). */
	public boolean loadAssembly(double gwd, double u235, boolean bentGrid, boolean leakingRod, int ifbaRods) {
		if (assemblies >= Tuning.CORE_ASSEMBLIES) {
			return false;
		}
		ifba[assemblies] = Math.max(0, ifbaRods);
		assay[assemblies] = u235;
		bent[assemblies] = bentGrid;
		leaking[assemblies] = leakingRod;
		burnup[assemblies++] = gwd;
		return true;
	}

	/** The assay of the last assembly loaded (what a fresh one taken back out carries). */
	public double lastAssay() {
		return assemblies == 0 ? Tuning.ASSAY_PRODUCT : assay[assemblies - 1];
	}

	public double assay(int i) {
		return assay[i];
	}

	public int ifba(int i) {
		return ifba[i];
	}

	/** The IFBA rods of the last assembly loaded (what a fresh one taken back out carries). */
	public int lastIfba() {
		return assemblies == 0 ? 0 : ifba[assemblies - 1];
	}

	/** Assembly {@code i}'s reactivity: its assay's (16c1), less its burnup, less its IFBA while it lasts (19c2). */
	public double assemblyRho(int i) {
		return Enrichment.freshRho(assay[i]) - Tuning.ASSEMBLY_RHO_PER_GWD * burnup[i] - ifbaRho(ifba[i], burnup[i]);
	}

	/** The IFBA's worth: 3,000 pcm for 104 rods, in proportion, burning out in a straight line by 12 GWd/t. */
	public static double ifbaRho(int rods, double gwd) {
		return Tuning.IFBA_RHO_104 * rods / Tuning.IFBA_FULL_RODS * Math.max(0, 1 - gwd / Tuning.IFBA_BURNOUT_GWD);
	}

	/** Takes the last assembly back out if it is fresh; returns false otherwise. */
	public boolean unloadFreshAssembly() {
		if (assemblies == 0 || burnup[assemblies - 1] > 0) {
			return false;
		}
		assemblies--;
		bent[assemblies] = false;
		ifba[assemblies] = 0;
		return true;
	}

	/** Whether the last assembly (the one {@link #unloadFreshAssembly} would take) has a bent grid. */
	public boolean lastBent() {
		return assemblies > 0 && bent[assemblies - 1];
	}

	/** The index of the assembly with the most burnup (the next to go to the pool). */
	public int mostBurned() {
		int best = 0;
		for (int i = 1; i < assemblies; i++) {
			if (burnup[i] > burnup[best]) {
				best = i;
			}
		}
		return best;
	}

	/**
	 * Takes assembly {@code i} out for the pool (8e): its record carries its burnup and its decay heat groups (each assembly's share is
	 * the core's, a fraction of its own power), and whether the core was damaged. Taking the last one out clears a damaged core.
	 */
	public SpentFuel.Assembly removeAssembly(int i) {
		SpentFuel.Assembly out = new SpentFuel.Assembly(burnup[i], decay, coreDamaged);
		for (int k = i; k < assemblies - 1; k++) {
			burnup[k] = burnup[k + 1];
			assay[k] = assay[k + 1];
			ifba[k] = ifba[k + 1];
			bent[k] = bent[k + 1];
			leaking[k] = leaking[k + 1];
		}
		assemblies--;
		burnup[assemblies] = 0;
		assay[assemblies] = Tuning.ASSAY_PRODUCT;
		ifba[assemblies] = 0;
		bent[assemblies] = false;
		leaking[assemblies] = false;
		for (int g = 0; g < decay.length; g++) {
			decay[g] *= assemblies / (assemblies + 1.0);
		}
		if (assemblies == 0 && coreDamaged) {
			clearDamage();
		}
		return out;
	}

	/** A defuelled core: the damage, its hydrogen and the trip's cause cleared. */
	public void clearDamage() {
		coreDamaged = false;
		exploded = false;
		h2 = 0;
		zrOxidized = 0;
		fuelC = tavg;
		trip = "shut down";
	}

	public boolean loadRcca() {
		return loadRcca(ControlRods.B4C, 0);
	}

	/** 19d: loads an RCCA of this kind with this much B-10 burnt; false when all 5 are in. */
	public boolean loadRcca(int kind, double b10) {
		if (rccas >= Tuning.RCCAS) {
			return false;
		}
		rccaKind[rccas] = kind;
		rccaB10[rccas] = b10;
		rccas++;
		return true;
	}

	public int rccaKind(int i) {
		return rccaKind[i];
	}

	public double rccaB10(int i) {
		return rccaB10[i];
	}

	/** 19d: RCCA {@code i}'s worth as a share of a fresh B4C RCCA's. */
	public double rccaWorthShare(int i) {
		return ControlRods.worthShare(rccaKind[i], rccaB10[i], dev.wildcatter.Rules.reactorRodWear());
	}

	/** 19d: the shutdown margin of the RCCAs loaded, the highest-worth one stuck out, less the power defect (fraction; 0.013 is 1,300 pcm). */
	public double rodShutdownMargin() {
		return ReactorNeutronics.rodShutdownMargin(this);
	}

	/** 19d: true when the margin is under 1,300 pcm with a full core and all 5 RCCAs in, the rule reactorRodWear on (or a hafnium RCCA in). */
	public boolean sdmLow() {
		return (dev.wildcatter.Rules.reactorRodWear() || hasHafnium()) && assemblies == Tuning.CORE_ASSEMBLIES && rccas == Tuning.RCCAS && rodShutdownMargin() < Tuning.SHUTDOWN_MARGIN;
	}

	/** 19d: a hafnium RCCA is in: a new part, so its SDM reading needs no rule. */
	public boolean hasHafnium() {
		for (int i = 0; i < rccas; i++) {
			if (rccaKind[i] == ControlRods.HAFNIUM) {
				return true;
			}
		}
		return false;
	}

	/** 19d: the RCCAs, for a readout: "B4C 12 % B-10, Hf, ...", REPLACE after one that needs it. */
	public String rccaReadout() {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < rccas; i++) {
			out.append(i == 0 ? "" : ", ").append(ControlRods.name(rccaKind[i]));
			if (rccaKind[i] == ControlRods.B4C && (rccaB10[i] > 0 || dev.wildcatter.Rules.reactorRodWear())) {
				out.append(String.format(" %.0f %% B-10", rccaB10[i] * 100));
			}
			if (ControlRods.replace(rccaKind[i], rccaB10[i])) {
				out.append(" REPLACE");
			}
		}
		return out.toString();
	}

	/** For the game test: RCCA {@code i}'s B-10 burnup. */
	public void setRccaB10ForTest(int i, double b10) {
		rccaB10[i] = b10;
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

	/** A PLC's demand for bank D, in steps, or negative for the reactor's own control from its setpoint. Never saved: it must keep coming. It moves rods only as far as the trips allow. */
	public void setRodDemand(double steps) {
		rodDemand = steps;
	}

	public double rodDemand() {
		return rodDemand;
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
	/** For the game test: filled and cold (30 C, no pressure), rods in. */
	public void setColdForTest() {
		waterKg = Tuning.RCS_WATER_KG;
		tavg = thot = tcold = pzrC = fuelC = 30;
		sgC[0] = sgC[1] = 30;
	}

	/** For the game test: the assemblies' burnups. */
	public void setBurnupsForTest(double... gwd) {
		System.arraycopy(gwd, 0, burnup, 0, Math.min(gwd.length, assemblies));
	}

	/** For the game test: a damaged core. */
	public void setDamagedForTest() {
		coreDamaged = true;
		tripped = true;
		trip = "CORE DAMAGE";
	}

	/** For the game test and unit tests: a long run at {@code power}, its decay heat groups, iodine and xenon at equilibrium. */
	public void setHistoryForTest(double power) {
		setHistoryForTest(power, 0);
	}

	/** The same, shut down {@code ageSeconds} ago (the decay heat only). */
	public void setHistoryForTest(double power, double ageSeconds) {
		for (int i = 0; i < decay.length; i++) {
			decay[i] = Tuning.DECAY_SHARE[i] * power * Math.exp(-Tuning.DECAY_LAMBDA[i] * ageSeconds);
		}
		iodine = power;
		xenon = xenonEquilibrium(power);
	}

	/** For the game test and unit tests: the RCS holding only {@code kg}. */
	public void setInventoryForTest(double kg) {
		waterKg = kg;
	}

	/** For the game test: the fuel at {@code c} C (uncovered). */
	public void setFuelForTest(double c) {
		fuelC = c;
	}

	/** For the game test: the plant at {@code c} C and {@code mpa}, rods in, cooling down. */
	public void setCooldownForTest(double c, double mpa) {
		tavg = thot = tcold = c;
		sgC[0] = sgC[1] = c;
		pzrC = Steam.tsat(mpa);
		setSetpoint(-1);
		setSetpoint(COOLDOWN);
	}

	/** Xenon at equilibrium at {@code power}, of its full-power level. */
	static double xenonEquilibrium(double power) {
		return ReactorDesign.xenonEquilibrium(power);
	}

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

	/** For the game test and unit tests: the pressurizer at the saturation temperature of {@code mpa}. */
	public void setPressureForTest(double mpa) {
		pzrC = Steam.tsat(mpa);
	}

	/** For the game test and unit tests: the RCS (and the steam generators) at {@code c} C. */
	public void setTemperatureForTest(double c) {
		tavg = thot = tcold = c;
		sgC[0] = sgC[1] = Math.min(c, sgC[0]);
	}

	private double pinnedForTest = -1;

	/** For the game test: the power held at {@code power} each step whatever the rods do (negative lets it go). Never saved. */
	public void pinPowerForTest(double power) {
		pinnedForTest = power;
	}

	/** For the game test: assembly {@code i} with a leaking rod. */
	public void setLeakingForTest(int i) {
		leaking[i] = true;
	}

	/** For the game test: the coolant's activity, µCi/g. */
	public void setActivityForTest(double uciPerG) {
		activity = uciPerG;
	}

	/** For the game test: the core steady at {@code power}. */
	public void holdPowerForTest(double power) {
		kinetics.hold(power);
	}

	void tripNow(String why) {
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
			if (rodDemand >= 0) {
				// a PLC's demand, inside the same startup-rate limits: too fast and the rods insert whatever it asks
				if (rate > Tuning.SUR_INSERT_DPM) {
					bankD = Math.max(0, bankD - move);
				} else if (bankD > rodDemand) {
					bankD = Math.max(rodDemand, bankD - move);
				} else if (bankD < rodDemand && rate < Tuning.SUR_WITHDRAW_DPM) {
					bankD = Math.min(rodDemand, bankD + move);
				}
			} else if (p > want * 1.02 || rate > Tuning.SUR_INSERT_DPM) {
				bankD = Math.max(0, bankD - move);
			} else if (p < want * 0.98 && rate < Tuning.SUR_WITHDRAW_DPM) {
				bankD = Math.min(Tuning.ROD_STEPS, bankD + move);
			}
		}

		ReactorCvcs.step(this, in, dt);

		double p = ReactorNeutronics.kinetics(this, dt);
		if (pinnedForTest >= 0) {
			kinetics.hold(pinnedForTest);
			p = pinnedForTest;
		}

		double prompt = ReactorNeutronics.decayAndXenon(this, p, dt);

		reliefKg = 0;
		reliefJ = 0;
		locaKg = 0;
		ReactorThermal.step(this, in, dt, p, prompt, before, anyPump);

		ReactorNeutronics.burn(this, p, dt);

		ReactorLimits.step(this, in, dt, before);
		ReactorActivity.step(this, in, p, dt);

		ReactorProtection.trips(this, in, p, dt);
	}

	// ---- saving ----------------------------------------------------------------------------------------------------------------
	/** Where the vessel keeps the state. */
	public interface Store {
		void put(String key, double value);

		double get(String key, double fallback);
	}

	public void save(Store out) {
		ReactorSaves.save(this, out);
	}

	public void load(Store in) {
		ReactorSaves.load(this, in);
	}
}
