package dev.wildcatter.well;

import dev.wildcatter.Tuning;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A small well model for a shut-in kick and its kill through the choke (docs/multiblocks/oilfield-follow-ons.md, 23b), in oilfield units.
 * The drill string and the annulus hold columns of mud by volume, each segment with its own weight; the influx is one gas slug in the
 * annulus that rises with the circulation and expands as the pressure on it falls (Boyle). Bottomhole pressure is the annulus
 * hydrostatic (mud and gas) plus annular friction plus the choke's back pressure; the choke's back pressure goes with flow squared over
 * its opening squared. Drill pipe pressure is bottomhole pressure less the string's hydrostatic plus the string and bit friction; the
 * gauge shows it 2 s per 1,000 ft late. Plain Java ({@code WellControlTest}).
 */
public final class KillModel {
	/** One column of mud: litres and ppg. */
	private record Seg(double litres, double ppg) {
	}

	public final double depthM;
	public final double shoeM;
	public final double fgPpg;
	public final double porePpg;
	public final double omw;
	/** Top to bottom. */
	private final List<Seg> string = new ArrayList<>();
	/** Liquid only, bottom to top, in liquid volume; the gas sits at {@link #gasPos} litres of liquid up. */
	private final List<Seg> annulus = new ArrayList<>();
	/** The gas slug's volume at bottomhole pressure, litres. */
	private double gasBh;
	private double gasPos;
	private double gasNow;
	private double choke;
	private double spm;
	private double strokes;
	private double casing;
	private double bhp;
	private double dpp;
	private double shoePsi;
	private double influxL;
	private double lossLps;
	private double ventedL;
	private double returnsL;
	private final double[] dppHistory;
	private int dppAt;

	public KillModel(double depthM, double shoeM, double fgPpg, double porePpg, double mudPpg, double influxM3) {
		this.depthM = depthM;
		this.shoeM = shoeM;
		this.fgPpg = fgPpg;
		this.porePpg = porePpg;
		this.omw = mudPpg;
		string.add(new Seg(stringL(), mudPpg));
		annulus.add(new Seg(annulusL(), mudPpg));
		gasBh = Math.max(0, influxM3 * 1000);
		gasPos = 0;
		int delay = (int) Math.max(1, Math.round(depthM * Tuning.FEET_PER_M / 1000 * Tuning.DPP_DELAY_S_PER_KFT * 20));
		dppHistory = new double[delay];
		gasNow = gasBh;
		for (int k = 0; k < 3; k++) {   // the influx has pushed its own volume of mud out of the top (the pit gain)
			settle();
			double over = sum(annulus) - (annulusL() - gasNow);
			if (over > 0) {
				takeTop(over);
			}
		}
		settle();
		java.util.Arrays.fill(dppHistory, dpp);
	}

	private KillModel(double depthM, double shoeM, double fgPpg, double porePpg, double omw) {
		this.depthM = depthM;
		this.shoeM = shoeM;
		this.fgPpg = fgPpg;
		this.porePpg = porePpg;
		this.omw = omw;
		int delay = (int) Math.max(1, Math.round(depthM * Tuning.FEET_PER_M / 1000 * Tuning.DPP_DELAY_S_PER_KFT * 20));
		dppHistory = new double[delay];
	}

	public double stringL() {
		return depthM * Tuning.STRING_L_PER_M;
	}

	public double annulusL() {
		return depthM * Tuning.ANNULUS_L_PER_M;
	}

	private double feet(double metres) {
		return metres * Tuning.FEET_PER_M;
	}

	public double porePsi() {
		return WellControl.psi(porePpg, feet(depthM));
	}

	private static double sum(List<Seg> segs) {
		double v = 0;
		for (Seg s : segs) {
			v += s.litres;
		}
		return v;
	}

	/** Hydrostatic of the string, psi. */
	public double stringPsi() {
		double psi = 0;
		for (Seg s : string) {
			psi += WellControl.psi(s.ppg, feet(s.litres / Tuning.STRING_L_PER_M));
		}
		return psi;
	}

	/** The string's average mud weight. */
	public double stringPpg() {
		return WellControl.emw(stringPsi(), feet(depthM));
	}

	/**
	 * The pressure profile from the top: returns {annulus hydrostatic to the bottom, at the shoe, at the gas's middle} for a casing
	 * pressure of zero, with the gas at {@code gasL} litres.
	 */
	private double[] profile(double gasL) {
		double liquid = sum(annulus);
		double liquidAbove = Math.max(0, liquid - gasPos);
		double shoeFromTop = shoeM;
		double depth = 0;   // metres from the top
		double psi = 0;
		double atShoe = Double.NaN, atGas = 0;
		// liquid above the gas, walked from the top down
		double walked = 0;
		for (int i = annulus.size() - 1; i >= 0 && walked < liquidAbove; i--) {
			Seg s = annulus.get(i);
			double take = Math.min(s.litres, liquidAbove - walked);
			double m = take / Tuning.ANNULUS_L_PER_M;
			if (Double.isNaN(atShoe) && depth + m >= shoeFromTop) {
				atShoe = psi + WellControl.psi(s.ppg, feet(shoeFromTop - depth));
			}
			psi += WellControl.psi(s.ppg, feet(m));
			depth += m;
			walked += take;
		}
		double gm = gasL / Tuning.ANNULUS_L_PER_M;
		if (Double.isNaN(atShoe) && depth + gm >= shoeFromTop) {
			atShoe = psi + WellControl.psi(Tuning.GAS_GRADIENT_PPG, feet(shoeFromTop - depth));
		}
		atGas = psi + WellControl.psi(Tuning.GAS_GRADIENT_PPG, feet(gm / 2));
		psi += WellControl.psi(Tuning.GAS_GRADIENT_PPG, feet(gm));
		depth += gm;
		// liquid below the gas
		double below = Math.min(gasPos, liquid);
		double segTop = liquid;
		for (int i = annulus.size() - 1; i >= 0; i--) {
			Seg s = annulus.get(i);
			double segBot = segTop - s.litres;   // liquid coordinates, from the bottom
			double top = segTop;
			segTop = segBot;
			if (segBot >= below) {
				continue;
			}
			segBot = Math.max(segBot, 0);
			double hiTop = top;
			double hi = Math.min(hiTop, below), lo = segBot;
			if (hi <= lo) {
				continue;
			}
			double m = (hi - lo) / Tuning.ANNULUS_L_PER_M;
			if (Double.isNaN(atShoe) && depth + m >= shoeFromTop) {
				atShoe = psi + WellControl.psi(s.ppg, feet(shoeFromTop - depth));
			}
			psi += WellControl.psi(s.ppg, feet(m));
			depth += m;
		}
		if (Double.isNaN(atShoe)) {
			atShoe = psi;
		}
		return new double[] {psi, atShoe, atGas};
	}


	private double frictionAnnulus() {
		double r = spm / Tuning.SCR_SPM;
		return WellControl.psi(Tuning.ECD_SCR_PPG * r * r, feet(depthM));
	}

	private double frictionSystem() {
		double r = spm / Tuning.SCR_SPM;
		return Tuning.SCR_PSI * r * r * stringPpg() / omw;
	}

	/** The choke's back pressure for the pump rate and opening, psi. */
	private double chokePsi() {
		double r = spm / Tuning.SCR_SPM;
		return Math.min(5_000, Tuning.CHOKE_K_PSI * r * r / Math.max(1e-6, choke * choke));
	}

	/** Finds the gas's volume, the casing pressure and bottomhole pressure for the state now. */
	private void settle() {
		double atm = 14.7;
		double[] p = profile(gasNow);
		boolean closed = spm < 1e-6 && choke < 1e-6;
		for (int k = 0; k < 4; k++) {
			p = profile(gasNow);
			double hann = p[0];
			if (closed) {
				casing = Math.max(0, porePsi() - hann);
			} else {
				casing = chokePsi();
			}
			bhp = casing + hann + frictionAnnulus();
			double atGas = casing + p[2];
			gasNow = gasBh <= 0 ? 0 : gasBh * (Math.max(bhp, porePsi()) + atm) / (atGas + atm);
		}
		p = profile(gasNow);
		double hann = p[0];
		casing = closed ? Math.max(0, porePsi() - hann) : chokePsi();
		bhp = casing + hann + frictionAnnulus();
		shoePsi = casing + p[1] + frictionAnnulus() * shoeM / Math.max(1, depthM);
		dpp = Math.max(0, bhp - stringPsi() + frictionSystem() - frictionAnnulus());
	}

	/**
	 * One step of {@code seconds}: the pump puts {@code tankPpg} mud in the string at {@code newSpm} and the choke is at {@code newChoke}
	 * (0 shut, 1 open). Returns nothing; read the gauges and the flows after.
	 */
	public void step(double seconds, double tankPpg, double newSpm, double newChoke) {
		spm = Math.max(0, newSpm);
		choke = Math.max(0, Math.min(1, newChoke));
		double dv = spm / 60 * Tuning.PUMP_L_PER_STROKE * seconds;
		strokes += spm / 60 * seconds;
		returnsL = 0;
		ventedL = 0;
		if (dv > 0) {
			string.addFirst(new Seg(dv, tankPpg));
			double out = take(string, dv, false);
			annulus.addFirst(new Seg(out, lastPpg));
			merge(annulus);
			gasPos += out;
		}
		settle();
		// influx while bottomhole pressure is under the pore pressure
		double under = porePsi() - bhp;
		if (under > 0 && !(spm < 1e-6 && choke < 1e-6)) {
			double litres = Math.min(Tuning.KICK_GAIN_M3S, Tuning.KICK_INFLUX_M3S_PER_PSI * under) * 1000 * seconds;
			influxL += litres;
			if (gasBh <= 0) {
				gasPos = 0;
			}
			gasBh += litres * (porePsi() + 14.7) / (bhp + 14.7);
			settle();
		}
		// losses at the shoe
		double shoeEmw = WellControl.emw(shoePsi, feet(shoeM));
		lossLps = WellControl.losses(shoeEmw, fgPpg) == WellControl.Losses.NONE ? 0 : WellControl.partialLossRate(shoeEmw, fgPpg) * 1000;
		// the liquid over what the annulus holds goes out the top to the pits; gas at the top vents through the choke
		double liquid = sum(annulus);
		double room = annulusL() - gasNow;
		if (gasPos >= liquid - 1e-6 && gasNow > 0) {
			// the gas is at the surface: what will not fit vents
			double over = gasPos + gasNow - annulusL();
			if (over > 0 || dv > 0) {
				double vent = Math.max(over, 0);
				double frac = Math.min(1, vent / Math.max(1e-9, gasNow));
				ventedL = vent * (casing + 14.7) / 14.7;   // at surface conditions
				gasBh *= 1 - frac;
				if (gasBh < 1e-3) {
					gasBh = 0;
				}
			}
			room = annulusL() - Math.min(gasNow, annulusL() - gasPos);
		}
		if (liquid > room) {
			returnsL = liquid - room;
			takeTop(returnsL);
			gasPos = Math.min(gasPos, sum(annulus));
		}
		settle();
		dppHistory[dppAt] = dpp;
		dppAt = (dppAt + 1) % dppHistory.length;
	}

	private double lastPpg;

	/** Takes {@code litres} off the string's bottom; returns the litres and sets {@link #lastPpg} to their mixed weight. */
	private double take(List<Seg> segs, double litres, boolean top) {
		double got = 0, mass = 0;
		while (got < litres - 1e-9 && !segs.isEmpty()) {
			int i = top ? 0 : segs.size() - 1;
			Seg s = segs.get(i);
			double t = Math.min(s.litres, litres - got);
			got += t;
			mass += t * s.ppg;
			if (t >= s.litres - 1e-9) {
				segs.remove(i);
			} else {
				segs.set(i, new Seg(s.litres - t, s.ppg));
			}
		}
		lastPpg = got > 0 ? mass / got : omw;
		merge(segs);
		return got;
	}

	private void takeTop(double litres) {
		double got = 0;
		while (got < litres - 1e-9 && !annulus.isEmpty()) {
			int i = annulus.size() - 1;
			Seg s = annulus.get(i);
			double t = Math.min(s.litres, litres - got);
			got += t;
			if (t >= s.litres - 1e-9) {
				annulus.remove(i);
			} else {
				annulus.set(i, new Seg(s.litres - t, s.ppg));
			}
		}
		merge(annulus);
	}

	/** Joins neighbouring segments of nearly the same weight, so the lists stay short. */
	private static void merge(List<Seg> segs) {
		while (segs.size() > 48) {   // too many: join the closest pair in weight
			int best = 1;
			for (int i = 2; i < segs.size(); i++) {
				if (Math.abs(segs.get(i).ppg - segs.get(i - 1).ppg) < Math.abs(segs.get(best).ppg - segs.get(best - 1).ppg)) {
					best = i;
				}
			}
			Seg a = segs.get(best - 1), b = segs.get(best);
			double v = a.litres + b.litres;
			segs.set(best - 1, new Seg(v, (a.litres * a.ppg + b.litres * b.ppg) / v));
			segs.remove(best);
		}
		for (int i = segs.size() - 1; i > 0; i--) {
			Seg a = segs.get(i - 1), b = segs.get(i);
			if (Math.abs(a.ppg - b.ppg) < 0.02) {
				double v = a.litres + b.litres;
				segs.set(i - 1, new Seg(v, v > 0 ? (a.litres * a.ppg + b.litres * b.ppg) / v : a.ppg));
				segs.remove(i);
			}
		}
	}

	// ---- reads -----------------------------------------------------------------------------------------------------------------

	public double casingPsi() {
		return casing;
	}

	public double bhpPsi() {
		return bhp;
	}

	/** The drill pipe gauge: what the drill pipe pressure was 2 s per 1,000 ft ago. */
	public double dppGauge() {
		return dppHistory[dppAt];
	}

	/** The drill pipe pressure now (the gauge shows it late). */
	public double dppNow() {
		return dpp;
	}

	public double shoeEmw() {
		return WellControl.emw(shoePsi, feet(shoeM));
	}

	public double strokes() {
		return strokes;
	}

	public double spm() {
		return spm;
	}

	public double choke() {
		return choke;
	}

	/** Litres of gas in the hole now, at its own pressure. */
	public double gasL() {
		return gasNow;
	}

	public boolean gasInHole() {
		return gasBh > 0;
	}

	/** The gas's top, metres below the surface (0 at the surface, depth with no gas). */
	public double gasTopM() {
		if (gasBh <= 0) {
			return depthM;
		}
		double liquid = sum(annulus);
		return Math.max(0, (liquid - gasPos) / Tuning.ANNULUS_L_PER_M);
	}

	/** Litres of new influx taken since the model began. */
	public double influxL() {
		return influxL;
	}

	/** Litres a second of mud lost at the shoe, the last step. */
	public double lossLps() {
		return lossLps;
	}

	/** Gas vented at the surface the last step, litres at atmospheric pressure. */
	public double ventedL() {
		return ventedL;
	}

	public double returnsL() {
		return returnsL;
	}

	/** The annulus's average mud weight (liquid only). */
	public double annulusPpg() {
		double v = 0, m = 0;
		for (Seg s : annulus) {
			v += s.litres;
			m += s.litres * s.ppg;
		}
		return v > 0 ? m / v : omw;
	}

	/** The lightest mud in the hole, string or annulus. */
	public double lightestPpg() {
		double light = Double.MAX_VALUE;
		for (Seg s : string) {
			light = Math.min(light, s.ppg);
		}
		for (Seg s : annulus) {
			light = Math.min(light, s.ppg);
		}
		return light;
	}

	/** The shut-in pressures with the pump stopped and the choke closed: {SIDPP, SICP}. */
	public double[] shutInPressures() {
		double s = spm, c = choke;
		spm = 0;
		choke = 0;
		settle();
		double[] out = {Math.max(0, bhp - stringPsi()), casing};
		spm = s;
		choke = c;
		settle();
		return out;
	}

	// ---- saving ----------------------------------------------------------------------------------------------------------------

	public String save() {
		StringBuilder out = new StringBuilder();
		out.append(String.format(Locale.ROOT, "%f,%f,%f,%f,%f;", depthM, shoeM, fgPpg, porePpg, omw));
		out.append(String.format(Locale.ROOT, "%f,%f,%f,%f,%f,%f;", gasBh, gasPos, choke, spm, strokes, influxL));
		for (Seg s : string) {
			out.append(String.format(Locale.ROOT, "%f:%f,", s.litres, s.ppg));
		}
		out.append(';');
		for (Seg s : annulus) {
			out.append(String.format(Locale.ROOT, "%f:%f,", s.litres, s.ppg));
		}
		return out.toString();
	}

	public static KillModel load(String text) {
		try {
			String[] parts = text.split(";", -1);
			String[] g = parts[0].split(",");
			KillModel m = new KillModel(Double.parseDouble(g[0]), Double.parseDouble(g[1]), Double.parseDouble(g[2]), Double.parseDouble(g[3]), Double.parseDouble(g[4]));
			String[] s = parts[1].split(",");
			m.gasBh = Double.parseDouble(s[0]);
			m.gasPos = Double.parseDouble(s[1]);
			m.choke = Double.parseDouble(s[2]);
			m.spm = Double.parseDouble(s[3]);
			m.strokes = Double.parseDouble(s[4]);
			m.influxL = Double.parseDouble(s[5]);
			for (int k = 2; k <= 3; k++) {
				for (String seg : parts[k].split(",")) {
					if (seg.isBlank()) {
						continue;
					}
					String[] lp = seg.split(":");
					(k == 2 ? m.string : m.annulus).add(new Seg(Double.parseDouble(lp[0]), Double.parseDouble(lp[1])));
				}
			}
			m.settle();
			java.util.Arrays.fill(m.dppHistory, m.dpp);
			return m;
		} catch (RuntimeException e) {
			return null;
		}
	}
}
