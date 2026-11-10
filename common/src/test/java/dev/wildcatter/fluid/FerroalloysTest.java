package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import dev.wildcatter.fluid.Ferroalloys.Batch;
import dev.wildcatter.fluid.Ferroalloys.Charge;
import dev.wildcatter.fluid.Ferroalloys.Recipe;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.solver.Complex;
import dev.wildcatter.solver.Network;
import org.junit.jupiter.api.Test;

/** The ferroalloy plant (docs/multiblocks/ferroalloys.md): the spec's Numbers follow from its MWh a tonne, its tap and its chemistry. */
class FerroalloysTest {
	@Test
	void eightMegawattsMakeTheSpecsTonnes() {
		assertEquals(2.1, Recipe.FECR.tonnesPerHour(Tuning.SAF_WATTS), 0.01);
		assertEquals(50, Recipe.FECR.tonnesPerHour(Tuning.SAF_WATTS) * 24, 1, "50 t a day: a small real furnace");
		assertEquals(3.2, Recipe.FEMN.tonnesPerHour(Tuning.SAF_WATTS), 0.01);
		assertEquals(0.94, Recipe.FESI.tonnesPerHour(Tuning.SAF_WATTS), 0.01);
	}

	@Test
	void theChromititeGivesTheAlloysChromium() {
		double chromite = Recipe.FECR.kgPerTonne(Charge.CHROMITE) * Ore.CHROMITE.grade;
		double cr = chromite * 2 * 52.00 / (55.85 + 2 * 52.00 + 4 * 16.00) * 0.85;   // FeCr2O4, 85 % recovery
		assertEquals(650, cr, 10, "0.62 of a block of chromitite makes a tonne of 65 % Cr");
		assertEquals(0, Recipe.FESI.kgPerTonne(Charge.CHROMITE));
		assertEquals(1800, Recipe.FESI.kgPerTonne(Charge.QUARTZ));
		assertTrue(Recipe.FECR.lack(Charge.CHROMITE).startsWith("NO ORE"));
		assertTrue(Recipe.FECR.lack(Charge.QUARTZ).startsWith("NO FLUX"));
		assertTrue(Recipe.FESI.lack(Charge.QUARTZ).startsWith("NO ORE"));
		assertTrue(Recipe.FEMN.lack(Charge.COKE).startsWith("NO COKE"));
	}

	@Test
	void theTapChangerRunsTheFurnaceFromHalfToFullPower() {
		assertEquals(Tuning.SAF_WATTS, Ferroalloys.tapWatts(Tuning.FURNACE_TAPS), 1e-6);
		assertEquals(Tuning.SAF_WATTS * 0.49, Ferroalloys.tapWatts(1), 1e-3);
		assertEquals(Tuning.FURNACE_TAPS, Ferroalloys.tapFor(Tuning.SAF_WATTS));
		assertEquals(1, Ferroalloys.tapFor(0));
		assertEquals(38.5, Ferroalloys.electrodeAmps(Tuning.SAF_WATTS, Tuning.FURNACE_TAPS) / 1000, 0.1, "about 38 kA a phase at full tap");
	}

	@Test
	void threeMegavarsLiftTheFeederToAboutPointNineFour() {
		double q = Ferroalloys.vars(Tuning.SAF_WATTS, Tuning.SAF_POWER_FACTOR);
		assertEquals(6.0e6, q, 1);
		assertEquals(0.80, Ferroalloys.powerFactor(Tuning.SAF_WATTS, q), 1e-9);
		assertEquals(0.94, Ferroalloys.powerFactor(Tuning.SAF_WATTS, q - Tuning.CAP_BANK_VAR), 0.005, "8 / 8.54 MVA");
		assertEquals(51.8, Ferroalloys.capacitorOhms(12_470, Tuning.CAP_BANK_VAR), 0.1);
	}

	/** The solver, with no Minecraft: the furnace's star and the bank's star on a 12.47 kV supply; the bank cuts the supply's current. */
	@Test
	void theSolverSeesTheBankCutTheFeederCurrent() {
		double vll = 12_470;
		double[] degrees = {0, -120, 120};
		double[] amps = new double[2];
		double[] pf = new double[2];
		for (int withBank = 0; withBank < 2; withBank++) {
			Network net = new Network(0.01, 0.01);
			int neutral = net.node();
			net.ground(neutral);
			int[] hot = new int[3];
			int[] src = new int[3];
			for (int k = 0; k < 3; k++) {
				hot[k] = net.node();
				src[k] = net.source(hot[k], neutral, vll / Math.sqrt(3), degrees[k]);
			}
			double z = vll * vll / (Tuning.SAF_WATTS / Tuning.SAF_POWER_FACTOR);
			int star = net.node();
			for (int k = 0; k < 3; k++) {
				net.impedance(hot[k], star, new Complex(z * Tuning.SAF_POWER_FACTOR, z * Math.sqrt(1 - Tuning.SAF_POWER_FACTOR * Tuning.SAF_POWER_FACTOR)));
			}
			if (withBank == 1) {
				double x = Ferroalloys.capacitorOhms(vll, Tuning.CAP_BANK_VAR);
				int capStar = net.node();
				for (int k = 0; k < 3; k++) {
					net.impedance(hot[k], capStar, new Complex(x * Tuning.CAP_BANK_LOSS, -x));
				}
			}
			assertTrue(net.solve());
			Complex s = Complex.ZERO;
			for (int k = 0; k < 3; k++) {
				s = s.plus(net.voltagePhasor(hot[k]).times(net.sourceCurrentPhasor(src[k]).conjugate()));
			}
			amps[withBank] = net.sourceCurrent(src[0]);
			pf[withBank] = Math.abs(s.re()) / s.abs();
		}
		assertEquals(463, amps[0], 5, "10 MVA at 12.47 kV is about 460 A");
		assertEquals(0.80, pf[0], 0.005);
		assertEquals(0.94, pf[1], 0.01);
		assertEquals(396, amps[1], 6, "8.54 MVA");
	}

	@Test
	void theRoasterMakesMolyOxideAndBurnerGas() {
		assertEquals(1.08, Ferroalloys.mos2PerOxide() / 0.90, 0.03, "about 1.1 t of 90 % concentrate a tonne of oxide");
		assertEquals(0.78, Ferroalloys.so2PerOxide(), 0.01);
		assertEquals(Ferroalloys.so2PerOxide(), Ferroalloys.burnerGasPerOxide() * AcidPlant.so2InGas(), 1e-9, "the gas on the pipe carries the roaster's SO2");
		assertEquals(807, Ferroalloys.scheelitePerBag(), 1);
	}

	@Test
	void theVesselsBatchesBalance() {
		Batch femo = Batch.FERROMOLY;
		assertEquals(860, femo.alloyKg(), 5);
		assertTrue(femo.aluminiumKg() >= femo.aluminiumNeededKg(), "enough aluminium for the oxide");
		assertEquals(0.53, femo.aluminiumKg() / femo.alloyKg(), 0.01, "0.53 t of aluminium a tonne of FeMo");
		assertTrue(femo.alloyKg() * (1 - Tuning.FERROMOLY_MO) <= femo.iron * Tuning.IRON_INGOT_KG, "the iron covers the alloy's");
		Batch few = Batch.FERROTUNGSTEN;
		assertEquals(648, few.alloyKg(), 5);
		assertTrue(few.aluminiumKg() >= few.aluminiumNeededKg());
		assertEquals(0.30, few.aluminiumKg() / (Tuning.BULK_BAG_KG * Tuning.SCHEELITE_BAG_WO3) * 1.04, 0.02, "0.3 t of aluminium a 1.04 t of WO3");
		assertTrue(femo.slagKg() > 800 && few.slagKg() > 300, "a block of alumina slag each: " + femo.slagKg() + ", " + few.slagKg());
	}
}
