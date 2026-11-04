package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The prebake pot's balance (docs/multiblocks/potline.md): Faraday's law and the current efficiency, the alumina, AlF3 and carbon a
 * kg of aluminium, the anodes' life and the anode effect's back-EMF and CF4. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Potline {
	private Potline() {
	}

	/** The current efficiency with {@code overdue} anodes past their change and the AlF3 bin empty or not. */
	public static double currentEfficiency(int overdue, boolean noAlf3) {
		double ce = Tuning.POT_CURRENT_EFFICIENCY - Math.min(overdue, Tuning.POT_ANODES) * Tuning.POT_OVERDUE_PENALTY;
		return Math.max(0, noAlf3 ? ce - Tuning.POT_NO_ALF3_PENALTY : ce);
	}

	/** kg of aluminium a second at {@code amps} and current efficiency {@code ce}. */
	public static double aluminiumKgs(double amps, double ce) {
		return amps / 1000 * Tuning.FARADAY_KG_PER_KAH * ce / 3600;
	}

	/** kg of AlF3 a kg of aluminium, the pot's gas treated or vented. */
	public static double alf3PerAl(boolean treated) {
		return treated ? Tuning.POT_ALF3_TREATED : Tuning.POT_ALF3_VENTED;
	}

	/** kg of carbon an anode burns before it is changed: the baked block less its butt. */
	public static double carbonPerAnode() {
		return Tuning.BAKED_ANODE_KG - Tuning.BUTT_KG;
	}

	/** Days an anode lasts at the rated current. */
	public static double anodeLifeDays() {
		double carbonKgs = aluminiumKgs(Tuning.POT_AMPS, Tuning.POT_CURRENT_EFFICIENCY) * Tuning.POT_CARBON_PER_AL;
		return Tuning.POT_ANODES * carbonPerAnode() / carbonKgs / 86_400;
	}

	/** The back-EMF that makes the cell read {@link Tuning#POT_AE_VOLTS} at the rated current: its resistance stays. */
	public static double anodeEffectBackEmf() {
		return Tuning.POT_AE_VOLTS - (Tuning.POT_CELL_VOLTS - Tuning.POT_BACK_EMF);
	}

	/** Volts a pot reads at the rated current, its cell and its risers. */
	public static double potVolts() {
		return Tuning.POT_CELL_VOLTS + Tuning.POT_RISER_OHMS * Tuning.POT_AMPS;
	}

	/** m3 of pot gas a second at {@code amps}. */
	public static double gasM3s(double amps) {
		return Tuning.POT_GAS_M3H / 3600 * Math.min(1, amps / Tuning.POT_AMPS);
	}

	/** A rectiformer's share of the line setpoint with {@code online} units online, at most its rating. */
	public static double unitShare(double lineSetpoint, int online) {
		return online <= 0 ? 0 : Math.min(Tuning.RECTIFORMER_AMPS, lineSetpoint / online);
	}
}
