package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SlurryTest {
	private static final String GALENA = "wildcatter:galena";
	private static final String GOLD = "minecraft:gold_ingot";
	private static final String ZIRCON = "tailings:zircon";

	@Test
	void theSlurriesAreLiquidsCountedInKgThatNoTankHolds() {
		for (Product p : new Product[] {Product.MILL_DISCHARGE, Product.FINES_SLURRY, Product.CONDITIONED_SLURRY, Product.CONCENTRATE_SLURRY,
				Product.THICKENED_CONCENTRATE, Product.TAILS_SLURRY}) {
			assertFalse(p.gas(), p.label());
			assertEquals("kg", p.unit(), p.label());
			assertFalse(p.tanked(), p.label());
		}
	}

	@Test
	void blendingAveragesEachMineralByMass() {
		SlurryAssay a = SlurryAssay.of(Map.of(GALENA, 0.04));
		SlurryAssay b = SlurryAssay.of(Map.of(GALENA, 0.01, ZIRCON, 0.02));
		SlurryAssay mix = SlurryAssay.blend(a, 300, b, 100);
		assertEquals((0.04 * 300 + 0.01 * 100) / 400, mix.get(GALENA), 1e-12);
		assertEquals(0.02 * 100 / 400, mix.get(ZIRCON), 1e-12);
		assertEquals(0.0325 * 1000, mix.kg(GALENA, 1000), 1e-9);
	}

	@Test
	void anEmptySourceDilutesAndNothingBlendedChangesNothing() {
		SlurryAssay a = SlurryAssay.of(Map.of(GOLD, 0.000004));
		assertEquals(0.000002, SlurryAssay.blend(a, 50, SlurryAssay.EMPTY, 50).get(GOLD), 1e-15, "water halves it");
		assertEquals(a, SlurryAssay.blend(a, 50, SlurryAssay.of(Map.of(ZIRCON, 0.5)), 0), "zero kg of the other");
		assertEquals(a, SlurryAssay.blend(SlurryAssay.EMPTY, 0, a, 10), "into nothing");
		assertTrue(SlurryAssay.blend(SlurryAssay.EMPTY, 0, SlurryAssay.EMPTY, 0).isEmpty());
	}

	@Test
	void sharesAreClampedAndNeverAddPastOne() {
		SlurryAssay a = SlurryAssay.of(Map.of(GALENA, -0.2, ZIRCON, 1.5));
		assertEquals(0, a.get(GALENA));
		assertEquals(1, a.get(ZIRCON), 1e-12);
		SlurryAssay b = SlurryAssay.of(Map.of(GALENA, 0.8, ZIRCON, 0.8));
		assertEquals(0.5, b.get(GALENA), 1e-12);
		assertEquals(0.5, b.get(ZIRCON), 1e-12);
	}

	@Test
	void anAssaySavesAndLoads() {
		SlurryAssay a = SlurryAssay.of(Map.of(GALENA, 0.035, GOLD, 0.0000031, ZIRCON, 0.002));
		assertEquals(a, SlurryAssay.load(a.save()));
		assertTrue(SlurryAssay.load("").isEmpty());
		assertTrue(SlurryAssay.load("nonsense;=3;x=y").isEmpty(), "a bad string loads as empty");
		assertEquals(0.1, SlurryAssay.load("wildcatter:galena=0.1;junk").get(GALENA), 1e-12, "the good part is kept");
	}

	@Test
	void theMeterNamesTheRichestMinerals() {
		SlurryAssay a = SlurryAssay.of(Map.of(GALENA, 0.035, GOLD, 0.0001, ZIRCON, 0.002, "wildcatter:sphalerite", 0.05));
		assertEquals("sphalerite 5.00 %, galena 3.50 %, zircon 0.20 %", a.describe());
	}

	@Test
	void aHoldBlendsWhatIsOfferedAndGivesItBack() {
		SlurryHold hold = new SlurryHold();
		hold.offer(Product.FINES_SLURRY, 1000, SlurryAssay.of(Map.of(GALENA, 0.04)));
		hold.offer(Product.FINES_SLURRY, 1000, SlurryAssay.of(Map.of(GALENA, 0.02)));
		assertEquals(2000, hold.held(Product.FINES_SLURRY), 1e-9);
		assertEquals(0.03, hold.assayOf(Product.FINES_SLURRY).get(GALENA), 1e-12);
		assertEquals(500, hold.give(Product.FINES_SLURRY, 500), 1e-9);
		assertEquals(0.03, hold.assayOf(Product.FINES_SLURRY).get(GALENA), 1e-12, "what stays keeps its assay");
		hold.offer(Product.TAILS_SLURRY, 10, SlurryAssay.of(Map.of(ZIRCON, 0.001)));

		SlurryHold back = new SlurryHold();
		back.load(hold.save());
		assertEquals(1500, back.held(Product.FINES_SLURRY), 1e-9);
		assertEquals(hold.assayOf(Product.FINES_SLURRY), back.assayOf(Product.FINES_SLURRY));
		assertEquals(hold.assayOf(Product.TAILS_SLURRY), back.assayOf(Product.TAILS_SLURRY));

		assertEquals(1500, hold.give(Product.FINES_SLURRY, 9999), 1e-9);
		assertEquals(0, hold.held(Product.FINES_SLURRY));
		assertTrue(hold.assayOf(Product.FINES_SLURRY).isEmpty());
		back.load("NOT_A_PRODUCT|5|\nFINES_SLURRY|x|");
		assertEquals(0, back.held(Product.FINES_SLURRY));
	}
}
