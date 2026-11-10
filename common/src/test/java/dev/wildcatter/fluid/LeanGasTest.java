package dev.wildcatter.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.Tuning;
import org.junit.jupiter.api.Test;

/** The carbon plants' gases in engines and turbines (docs/questions/misc-gaps.md, 5). */
class LeanGasTest {
	@Test
	void cokeOvenGasDeratesAGasSetToEightySixPercent() {
		assertEquals(0.864, LeanGas.engineDerate(Product.COKE_OVEN_GAS), 0.001);
		assertEquals(864_000, Tuning.GENSET_GAS_WATTS * LeanGas.engineDerate(Product.COKE_OVEN_GAS), 500);
		assertEquals(1.0, LeanGas.engineDerate(Product.COKER_GAS), 1e-9, "coker gas is richer than pipeline gas");
		assertEquals(1.0, LeanGas.engineDerate(Product.PIPELINE_GAS), 1e-9);
	}

	@Test
	void wobbeIndexIsHeatingValueOverRootSpecificGravity() {
		assertEquals(49.1e6, LeanGas.wobbe(Product.PIPELINE_GAS), 0.1e6);   // 38 / sqrt(0.60)
		assertEquals(29.6e6, LeanGas.wobbe(Product.COKE_OVEN_GAS), 0.1e6);  // 18 / sqrt(0.37)
		assertEquals(49.8e6, LeanGas.wobbe(Product.COKER_GAS), 0.1e6);      // 45 / sqrt(0.816)
		assertEquals(-0.40, LeanGas.wobbeShift(Product.COKE_OVEN_GAS), 0.01);
		assertEquals(0.015, LeanGas.wobbeShift(Product.COKER_GAS), 0.01);
	}

	@Test
	void theAeroderivativeTakesOnlyGasNearItsDesignWobbeIndex() {
		assertTrue(LeanGas.aeroTakes(Product.PIPELINE_GAS));
		assertTrue(LeanGas.aeroTakes(Product.COKER_GAS));
		assertFalse(LeanGas.aeroTakes(Product.COKE_OVEN_GAS));
		assertFalse(LeanGas.aeroTakes(Product.BIOGAS), "no specific gravity given: never taken");
		assertFalse(LeanGas.aeroTakes(Product.CRUDE));
	}
}
