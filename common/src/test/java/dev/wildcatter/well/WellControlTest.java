package dev.wildcatter.well;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** Phase 23a-23b (docs/multiblocks/oilfield-follow-ons.md): the shoe, the MAASP, ECD and losses, the kill sheet, and a kill through the choke. */
class WellControlTest {
	private static final double FT = Tuning.FEET_PER_M;

	@Test
	void theShoesFractureGradient() {
		int top = -40;
		assertEquals(15.4, WellControl.shoeFg(top + 6, top), 1e-9, "a shoe 6 blocks above a deep field's top is in its transition zone");
		assertEquals(15.4, WellControl.shoeFg(top + 12, top), 1e-9);
		assertEquals(14.0, WellControl.shoeFg(top + 13, top), 1e-9, "set too high, the shoe is the open hole's 14.0");
		assertEquals(14.0, WellControl.shoeFg(10, Integer.MIN_VALUE), 1e-9, "no deep field: 14.0");
		// kill weight with the drilling ECD breaks down the open hole under surface casing, not a good intermediate shoe
		double drilling = Tuning.DEEP_PORE_PPG + Tuning.TRIP_MARGIN_PPG + Tuning.ECD_DRILLING_PPG;
		assertEquals(14.1, drilling, 1e-9);
		assertEquals(WellControl.Losses.PARTIAL, WellControl.losses(drilling, 14.0));
		assertEquals(WellControl.Losses.NONE, WellControl.losses(drilling, 15.4));
		assertEquals(WellControl.Losses.TOTAL, WellControl.losses(16.0 + Tuning.ECD_DRILLING_PPG, 15.4));
		assertEquals(Tuning.LOSS_PARTIAL_M3S * 0.2, WellControl.partialLossRate(14.1, 14.0), 1e-9, "a tenth of a ppg over: a fifth of the full rate");
	}

	@Test
	void maaspAndTheKillSheet() {
		double shoe = 88 * FT, tvd = 100 * FT;
		assertEquals((15.4 - 13.8) * 0.052 * shoe, WellControl.maasp(15.4, 13.8, shoe), 1e-9);
		assertEquals(24.0, WellControl.maasp(15.4, 13.8, shoe), 0.1, "a small margin at game depths");
		WellControl.KillSheet sheet = WellControl.killSheet(12.0, 14.0, 12.8, tvd, shoe, 15.4);
		assertEquals(12.8 + 12.0 / (0.052 * tvd), sheet.kmwExact(), 1e-9);
		assertEquals(sheet.kmwExact() + 0.3, sheet.kmw(), 1e-9);
		assertEquals(12.0 + Tuning.SCR_PSI, sheet.icp(), 1e-9, "ICP = SIDPP + SCR pressure");
		assertEquals(Tuning.SCR_PSI * sheet.kmw() / 12.8, sheet.fcp(), 1e-9, "FCP = SCR pressure x KMW / OMW");
		assertEquals(47, sheet.strokesToBit(), "750 L of string at 15.9 L a stroke");
		assertEquals(sheet.icp(), sheet.schedule(0), 1e-9);
		assertEquals(sheet.fcp(), sheet.schedule(sheet.strokesToBit()), 1e-9);
		assertEquals((sheet.icp() + sheet.fcp()) / 2, sheet.schedule(sheet.strokesToBit() / 2.0), 0.2);
		// a circulation at SCR: about 7 minutes in a 100 m hole
		double minutes = sheet.strokesCirculation() / Tuning.SCR_SPM;
		assertEquals(7.0, minutes, 0.6);
	}

	@Test
	void theShutInWell() {
		KillModel well = new KillModel(100, 88, 15.4, 13.5, 12.8, 0.05);
		double[] shut = well.shutInPressures();
		assertEquals(0.052 * 0.7 * 100 * FT, shut[0], 0.05, "SIDPP from the underbalance");
		assertTrue(shut[1] > shut[0], "the gas makes SICP read over SIDPP: " + shut[1]);
		assertEquals(well.porePsi(), well.bhpPsi(), 0.01, "shut in, bottomhole pressure balances the formation");
		assertTrue(well.shoeEmw() < 15.4, "a small kick leaves the shoe whole: " + well.shoeEmw());
		// a big kick from far underbalanced mud under only the surface casing breaks the open hole at shut-in, as it would
		KillModel bad = new KillModel(100, Tuning.SURFACE_CASING_M, 14.0, 13.5, 8.6, 1.5);
		assertTrue(bad.shoeEmw() > 14.5, "8.6 ppg into geopressure with 1.5 m3 in the hole, no intermediate casing: " + bad.shoeEmw());
	}

	/** A choke operator holding the drill pipe gauge on the schedule with a PI loop, as a PLC would. */
	private static double[] kill(double kp, double bias) {
		KillModel well = new KillModel(100, 88, 15.4, 13.5, 12.8, 0.05);
		double[] shut = well.shutInPressures();
		WellControl.KillSheet sheet = WellControl.killSheet(shut[0], shut[1], 12.8, 100 * FT, 88 * FT, 15.4);
		double choke = 0, spm = 0, worstShoe = 0, minBhp = Double.MAX_VALUE;
		double dt = 0.05;
		double casingStart = shut[1];
		int ticks = 0;
		for (; ticks < 20 * 60 * 15; ticks++) {
			double target;
			if (spm < Tuning.SCR_SPM - 1e-6) {
				// bring the pump up holding casing pressure at SICP
				spm = Math.min(Tuning.SCR_SPM, spm + Tuning.PUMP_SPM_PER_S * dt);
				double r = spm / Tuning.SCR_SPM;   // open the choke as the pump comes up, holding casing pressure at SICP
				choke = Math.max(0.001, Math.min(1, Math.sqrt(Tuning.CHOKE_K_PSI * r * r / Math.max(1, casingStart))));
			} else {
				target = sheet.schedule(well.strokes()) + bias;
				double err = well.dppGauge() - target;   // open on high drill pipe pressure, close on low, as fast as the hydraulic choke goes
				double next = choke * Math.exp(kp * err * dt);
				next = Math.max(choke - Tuning.HYDRAULIC_CHOKE_PER_S * dt, Math.min(choke + Tuning.HYDRAULIC_CHOKE_PER_S * dt, next));
				choke = Math.max(0.001, Math.min(1, next));
			}
			well.step(dt, sheet.kmw(), spm, choke);
			if (well.strokes() > 10) {
				worstShoe = Math.max(worstShoe, well.shoeEmw());
				minBhp = Math.min(minBhp, well.bhpPsi() - well.porePsi());
			}
			if (well.strokes() > sheet.strokesCirculation() + 5 && !well.gasInHole()) {
				break;
			}
		}
		well.step(dt, sheet.kmw(), 0, 0);
		double[] dead = well.shutInPressures();
		return new double[] {ticks / 20.0 / 60, worstShoe, minBhp, well.influxL(), dead[0], dead[1], well.annulusPpg(), sheet.kmw()};
	}

	@Test
	void aKillHeldOnScheduleKillsTheWell() {
		double[] r = kill(0.02, 0);
		assertTrue(r[0] < 12, "the kill within 12 minutes: " + r[0]);
		assertTrue(r[1] < 15.4, "the shoe never over its FG: " + r[1]);
		assertTrue(r[3] < 500, "no second kick: " + r[3] + " L of new influx");
		assertEquals(0, r[4], 0.5, "dead: SIDPP zero");
		assertEquals(0, r[5], 0.5, "dead: SICP zero");
		assertEquals(r[7], r[6], 0.1, "kill mud all round");
	}

	@Test
	void heldTooFarClosedTheShoeBreaks() {
		double[] r = kill(0.02, 60);
		assertTrue(r[1] > 15.4, "60 psi over the schedule puts the shoe over its FG: " + r[1]);
	}

	@Test
	void heldTooFarOpenTheWellFlows() {
		double[] r = kill(0.02, -40);
		assertTrue(r[2] < 0 && r[3] > 50, "40 psi under the schedule lets the formation flow: " + r[2] + " psi, " + r[3] + " L");
	}
}
