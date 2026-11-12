package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** 19c2's IFBA rods in the reactor (docs/multiblocks/fuel-fabrication-extensions.md, item 5). */
class ReactorIfbaTest {
	@Test
	void aFreshAssemblyWith104IfbaRodsStarts3000PcmLower() {
		Reactor r = new Reactor();
		r.loadAssembly(0, Tuning.ASSAY_PRODUCT, false, false, 0);
		r.loadAssembly(0, Tuning.ASSAY_PRODUCT, false, false, 104);
		assertEquals(0.03, r.assemblyRho(0) - r.assemblyRho(1), 1e-12);
		assertEquals(104, r.ifba(1));
	}

	@Test
	void theAbsorberBurnsOutByTwelveGwdPerTonne() {
		assertEquals(0.03, Reactor.ifbaRho(104, 0), 1e-12);
		assertEquals(0.015, Reactor.ifbaRho(104, 6), 1e-12);
		assertEquals(0, Reactor.ifbaRho(104, 12), 1e-12);
		assertEquals(0, Reactor.ifbaRho(104, 20), 1e-12);
		assertEquals(0.03 * 32 / 104, Reactor.ifbaRho(32, 0), 1e-12);
	}

	@Test
	void aWholeCoreOfIfbaFuelNeedsAbout300PpmLessBoron() {
		double ppm = Tuning.IFBA_RHO_104 / Math.abs(Tuning.BORON_PER_PPM);
		assertEquals(300, ppm, 1);
	}
}
