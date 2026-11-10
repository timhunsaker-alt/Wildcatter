package dev.wildcatter.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Every ore's tooltip says where it is found, what mines it and what uses it (docs/ores.md, "Tooltips and JEI"). */
class OreUsesTest {
	/** The ores no plant takes yet: a wave that gives one a user moves it out of this set and into OreUses.uses. */
	private static final Set<Ore> NO_USE_YET = EnumSet.of(Ore.SYLVITE, Ore.PENTLANDITE, Ore.PLATINUM, Ore.DOLOMITE,
			Ore.GRAPHITE, Ore.SPODUMENE, Ore.BARITE, Ore.BENTONITE, Ore.COBALTITE, Ore.PHOSPHATE);

	@Test
	void everyOreHasFoundToolAndUses() {
		for (Ore ore : Ore.values()) {
			List<String> lines = OreUses.lines(ore);
			assertTrue(lines.get(0).startsWith("Found: ") && lines.get(0).length() > 10, ore + ": " + lines);
			assertTrue(lines.get(1).startsWith("Mined with "), ore + ": " + lines);
			assertEquals(NO_USE_YET.contains(ore), OreUses.uses(ore).isEmpty(), ore + ": " + lines);
			assertTrue(NO_USE_YET.contains(ore) == lines.contains("No use yet"), ore + ": " + lines);
		}
	}

	@Test
	void theFurnaceOresSayFurnace() {
		for (Ore ore : new Ore[] {Ore.GALENA, Ore.SPHALERITE, Ore.CASSITERITE, Ore.ACANTHITE, Ore.MAGNESITE}) {
			assertTrue(OreUses.uses(ore).stream().anyMatch(u -> u.startsWith("a furnace")), ore.name());
		}
		assertFalse(OreUses.uses(Ore.HALITE).isEmpty());
		assertEquals("an iron pickaxe or better", OreUses.tool(Ore.PITCHBLENDE));
		assertEquals("a shovel", OreUses.tool(Ore.HEAVY_MINERAL));
	}
}
