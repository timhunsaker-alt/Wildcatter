package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GasPlantTest {
	@Test
	void aMillionScfGivesAboutOneAndAHalfGallonsOfNglPerMcf() {
		double m3 = 28_317;   // a million scf
		double gallonsPerMcf = GasPlant.ngl(m3) / 3.785 / 1000;
		assertEquals(1.5, gallonsPerMcf, 0.05);
		assertEquals(0.955 * m3, GasPlant.residue(m3), 1e-6, "the gas shrinks 4.5 %");
	}

	@Test
	void theStabilizersSplitAddsUp() {
		assertEquals(100, GasPlant.lpg(100) + GasPlant.gasoline(100), 1e-9);
		assertEquals(80, GasPlant.lpg(100), 1e-9);
	}

	@Test
	void theReboilerBurnsHalfAPercent() {
		assertEquals(995, GasPlant.dried(1000), 1e-9);
	}
}
