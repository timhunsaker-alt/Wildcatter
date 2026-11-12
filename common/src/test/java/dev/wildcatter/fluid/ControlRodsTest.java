package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Rules;
import dev.wildcatter.Tuning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 19d's rods (docs/multiblocks/reactor-extensions.md items 9 and 10), with the rule reactorRodWear switched on and handed back. */
class ControlRodsTest {
	private boolean[] rules;

	@BeforeEach
	void keep() {
		rules = Rules.snapshot();
	}

	@AfterEach
	void handBack() {
		Rules.restore(rules);
	}

	@Test
	void hafniumIsWorth85PercentAndNeverWears() {
		assertEquals(0.85, ControlRods.worthShare(ControlRods.HAFNIUM, 0, true), 1e-9);
		assertEquals(0.85, ControlRods.worthShare(ControlRods.HAFNIUM, 0.9, true), 1e-9);
		assertFalse(ControlRods.replace(ControlRods.HAFNIUM, 0.9));
	}

	@Test
	void b4cReadsReplaceAt30PercentAndLosesWorthPast40() {
		assertFalse(ControlRods.replace(ControlRods.B4C, 0.29));
		assertTrue(ControlRods.replace(ControlRods.B4C, 0.30));
		assertEquals(1.0, ControlRods.worthShare(ControlRods.B4C, 0.40, true), 1e-9);
		assertEquals(0.85, ControlRods.worthShare(ControlRods.B4C, 0.50, true), 1e-9);
		assertEquals(0.70, ControlRods.worthShare(ControlRods.B4C, 0.60, true), 1e-9);
		assertEquals(0.70, ControlRods.worthShare(ControlRods.B4C, 0.80, true), 1e-9);
		// without the rule a worn rod keeps its worth
		assertEquals(1.0, ControlRods.worthShare(ControlRods.B4C, 0.60, false), 1e-9);
	}

	@Test
	void aRodFullyInAtFullPowerReachesReplaceInAbout438Hours() {
		assertEquals(438, ControlRods.hoursToReplace(), 1);
	}

	@Test
	void theShutdownMarginFollowsTheRodsLoaded() {
		Rules.set(Rules.Rule.REACTOR_ROD_WEAR, true);
		Reactor b4c = new Reactor();
		while (b4c.loadRcca()) {
		}
		assertEquals(Reactor.shutdownMargin(), b4c.rodShutdownMargin(), 1e-12);   // the design's
		assertEquals(2212, b4c.rodShutdownMargin() * 1e5, 1);
		Reactor hafnium = new Reactor();
		while (hafnium.loadRcca(ControlRods.HAFNIUM, 0)) {
		}
		assertEquals(0.85 * 4 * Tuning.ROD_WORTH_EDGE - (4 * Tuning.ROD_WORTH_EDGE - Reactor.shutdownMargin()), hafnium.rodShutdownMargin(), 1e-12);
		assertTrue(hafnium.rodShutdownMargin() > Tuning.SHUTDOWN_MARGIN);
		// worn B4C at the edges with a hafnium centre: under 1,300 pcm
		Reactor worn = new Reactor();
		worn.loadRcca(ControlRods.HAFNIUM, 0);
		while (worn.loadRcca(ControlRods.B4C, 0.60)) {
		}
		assertTrue(worn.rodShutdownMargin() < Tuning.SHUTDOWN_MARGIN, "margin " + worn.rodShutdownMargin());
	}

	@Test
	void bankDBurnsItsBoronWhileInsertedAtPowerAndOnlyWithTheRule() {
		Reactor r = new Reactor();
		while (r.loadRcca()) {
		}
		r.loadRcca(ControlRods.HAFNIUM, 0);   // refused: all five are in
		double dt = ControlRods.hoursToReplace() * 3600;
		ReactorNeutronics.burnRods(r, 1.0, dt);
		assertEquals(0, r.rccaB10(0), 1e-12);   // the rule off: nothing burns
		Rules.set(Rules.Rule.REACTOR_ROD_WEAR, true);
		r.setRodsForTest(Tuning.ROD_STEPS, 0);   // the shutdown bank out, bank D all in
		ReactorNeutronics.burnRods(r, 1.0, dt);
		assertEquals(Tuning.B10_REPLACE, r.rccaB10(0), 1e-9);
		assertEquals(0, r.rccaB10(1), 1e-9);
		assertTrue(r.rccaReadout().startsWith("B4C 30 % B-10 REPLACE"), r.rccaReadout());
	}
}
