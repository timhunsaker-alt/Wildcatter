package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/** The ISR wellfield's 5-spot rule (docs/multiblocks/isr-wellfield.md). Plain Java, so it is unit-tested without Minecraft. */
public final class Isr {
	private Isr() {
	}

	/**
	 * mg/L of U3O8 in a producer's water: a full 5-spot's {@link Tuning#ISR_HEAD_GRADE_MG_L} for its share of four injectors, the
	 * oxygen they carried, the ore at the well against the front's richest, and what is left to win.
	 */
	public static double headGrade(int injectors, double oxygen, double grade, double left) {
		return Tuning.ISR_HEAD_GRADE_MG_L * Math.min(injectors, 4) / 4.0 * oxygen * grade / Tuning.ROLL_PEAK_GRADE * left;
	}
}
