package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FluidTest {
	@Test
	void waterCutRisesFromTenToEightyPercentAsTheFieldDrains() {
		assertEquals(0.10, Stream.waterCut(60_000, 60_000), 1e-9, "a new field");
		assertEquals(0.45, Stream.waterCut(30_000, 60_000), 1e-9, "half drained");
		assertEquals(0.80, Stream.waterCut(0, 60_000), 1e-9, "drained");
	}

	@Test
	void anOilWellsStreamSplitsIntoOilWaterAndGas() {
		Stream.Split s = Stream.oilWell(1000, 0.10);
		assertEquals(900, s.oil(), 1e-9);
		assertEquals(100, s.water(), 1e-9);
		assertEquals(90, s.gas(), 1e-9, "100 m3 of gas for each m3 of oil");
	}

	@Test
	void aGasWellsGasCarriesCondensateAndWater() {
		Stream.Split s = Stream.gasWell(28_317);   // a million scf
		assertEquals(10 * 159, s.oil(), 15, "10 bbl of condensate a million scf");
		assertEquals(2 * 159, s.water(), 5, "2 bbl of water a million scf");
		assertEquals(28_317, s.gas(), 1e-9);
	}

	@Test
	void aRunCarriesItsOneSourceProduct() {
		Run run = Run.of(Arrays.asList(Product.CRUDE, null, Product.CRUDE));
		assertEquals(Product.CRUDE, run.product());
		assertFalse(run.blocked());
		assertTrue(run.describe().contains("crude"));
		assertNull(Run.of(Arrays.asList((Product) null)).product(), "an empty tank alone sets nothing");
	}

	@Test
	void twoProductsMeetingBlockTheRun() {
		Run run = Run.of(List.of(Product.CRUDE, Product.PRODUCED_WATER));
		assertTrue(run.blocked());
		assertNull(run.product());
		assertEquals(Set.of(Product.CRUDE, Product.PRODUCED_WATER), run.clash());
		assertTrue(run.describe().startsWith("BLOCKED: crude meets produced water"), run.describe());
	}

	@Test
	void aPullTakesFromTheFirstSourcesFirst() {
		assertArrayEquals(new double[] {50, 30, 0}, Flow.share(80, new double[] {50, 100, 100}), 1e-9);
		assertArrayEquals(new double[] {10, 20, 0}, Flow.share(100, new double[] {10, 20, 0}), 1e-9, "everything there is, and no more");
		assertArrayEquals(new double[] {0, 0}, Flow.share(0, new double[] {10, 20}), 1e-9);
	}
}
