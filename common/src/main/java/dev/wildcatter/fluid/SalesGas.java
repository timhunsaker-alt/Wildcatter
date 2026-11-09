package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;
import org.jspecify.annotations.Nullable;

/**
 * The sales line's arithmetic (docs/multiblocks/sales-gas.md): which gas a receipt station takes, the energy it books, the share the
 * line keeps as compressor fuel and what a delivery station may draw against the account. Energy in GJ. Plain Java, so it is
 * unit-tested without Minecraft.
 */
public final class SalesGas {
	private SalesGas() {
	}

	/** What the chromatograph and the moisture analyser make of the gas at a receipt station. */
	public enum Spec {
		/** Pipeline gas: on spec. */
		ON_SPEC("on spec"),
		/** Separator, compressed or dried gas: its NGL put it over the heating value limit and would drop liquids in the line. */
		RICH("OFF SPEC: RICH (its NGL put it over " + Math.round(Tuning.SALES_BTU_SCF_MAX) + " Btu/scf and would drop liquids in the line: process it in a gas plant)"),
		/** Wet gas: water over the tariff's 7 lb a million scf. */
		WATER("OFF SPEC: WATER (wet gas: separate and dry it, then process it)"),
		/** Biogas: 40 % CO2, far under the heating value limit (docs/biogas.md). */
		CO2("OFF SPEC: CO2 (biogas is 40 % CO2, under " + Math.round(Tuning.SALES_BTU_SCF_MIN) + " Btu/scf)"),
		/** A liquid or a gas the line does not carry at all. */
		NOT_GAS("NOT GAS: the line takes only pipeline gas"),
		/** Nothing on the inlet. */
		NONE("no gas at its inlet");

		private final String reading;

		Spec(String reading) {
			this.reading = reading;
		}

		public String reading() {
			return reading;
		}
	}

	/** The verdict on what an inlet run carries (null: nothing). */
	public static Spec check(@Nullable Product product) {
		if (product == null) {
			return Spec.NONE;
		}
		return switch (product) {
			case PIPELINE_GAS -> Spec.ON_SPEC;
			case GAS, COMPRESSED_GAS, DRIED_GAS -> Spec.RICH;
			case WET_GAS -> Spec.WATER;
			case BIOGAS -> Spec.CO2;
			default -> Spec.NOT_GAS;
		};
	}

	/** GJ in {@code m3} of pipeline gas at the mod's heating value. */
	public static double gj(double m3) {
		return m3 * Tuning.GAS_JOULES_PER_M3 / 1e9;
	}

	/** MMBtu in {@code gj}. */
	public static double mmbtu(double gj) {
		return gj * 1e9 / (Tuning.JOULES_PER_BTU * 1e6);
	}

	/** Btu a standard cubic foot for a heating value in J/m3: 38 MJ/m3 is about 1,020. */
	public static double btuPerScf(double joulesPerM3) {
		return joulesPerM3 / Tuning.JOULES_PER_BTU * Tuning.M3_PER_SCF;
	}

	/** What the line keeps of a receipt as compressor fuel. */
	public static double retained(double gj) {
		return gj * Tuning.SALES_FUEL_RETENTION;
	}

	/** What a receipt adds to the account. */
	public static double credited(double gj) {
		return gj - retained(gj);
	}

	/** GJ taken from the account to put {@code m3} out of a delivery station: the gas and its line heater's share. */
	public static double debit(double m3) {
		return gj(m3) * (1 + Tuning.SALES_LINE_HEATER_SHARE);
	}

	/** How many m3 a delivery station may put out of {@code wanted}, with {@code balance} GJ on the account: never more than it holds. */
	public static double deliverable(double balance, double wanted) {
		if (balance <= 0 || wanted <= 0) {
			return 0;
		}
		return Math.min(wanted, balance / debit(1));
	}

	/** A station's rating in m3 over {@code seconds}. */
	public static double rating(double seconds) {
		return Tuning.SALES_STATION_M3S * seconds;
	}
}
