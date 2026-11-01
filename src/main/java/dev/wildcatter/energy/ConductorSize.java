package dev.wildcatter.energy;

import java.util.Arrays;
import java.util.List;
import net.minecraft.util.StringRepresentable;

/**
 * A conductor size (docs/phase4-conductors.md). Building wire and cable follow NEC 310.16 (75 C copper, with 240.4(D) for 12 and 10
 * AWG) and NEC Chapter 9 Table 8 resistance at 75 C; overhead lines are standard ACSR; medium-voltage cable follows the NEC 310.60
 * duct tables. A wire block is 1 m of conductor; a span block is {@code SPAN_METERS_PER_BLOCK} metres.
 */
public enum ConductorSize implements StringRepresentable {
	AWG_12("awg_12", "12 AWG", Family.BUILDING, 20, 6.50e-3, 1.0),
	AWG_10("awg_10", "10 AWG", Family.BUILDING, 30, 4.07e-3, 1.0),
	AWG_6("awg_6", "6 AWG", Family.BUILDING, 65, 1.61e-3, 1.0),
	AWG_2("awg_2", "2 AWG", Family.BUILDING, 115, 0.636e-3, 1.0),
	AWG_4_0("awg_4_0", "4/0 AWG", Family.BUILDING, 230, 0.199e-3, 1.0),
	KCMIL_500("kcmil_500", "500 kcmil", Family.BUILDING, 380, 0.0846e-3, 1.0),
	BUSWAY_800("busway_800", "800 A busway", Family.BUSWAY, 800, 0.02e-3, 1.0),
	ACSR_SPARROW("acsr_sparrow", "#2 ACSR (Sparrow)", Family.ACSR, 180, 1.05e-3, 1.0),
	ACSR_RAVEN("acsr_raven", "1/0 ACSR (Raven)", Family.ACSR, 240, 0.66e-3, 1.0),
	ACSR_PENGUIN("acsr_penguin", "4/0 ACSR (Penguin)", Family.ACSR, 340, 0.36e-3, 0.9),
	ACSR_LINNET("acsr_linnet", "336.4 kcmil ACSR (Linnet)", Family.ACSR, 530, 0.205e-3, 0.8),
	ACSR_DRAKE("acsr_drake", "795 kcmil ACSR (Drake)", Family.ACSR, 900, 0.087e-3, 0.7),
	MV_1_0("mv_1_0", "1/0 MV cable", Family.MV_CABLE, 195, 0.40e-3, 1.0),
	MV_4_0("mv_4_0", "4/0 MV cable", Family.MV_CABLE, 290, 0.199e-3, 1.0),
	MV_500("mv_500", "500 kcmil MV cable", Family.MV_CABLE, 450, 0.0846e-3, 1.0),
	STATION_BUS_2000("station_bus_2000", "2000 A station bus", Family.STATION_BUS, 2000, 0.015e-3, 1.0),
	/** 15 kV non-segregated phase bus duct from a generator (docs/multiblocks/gas-turbine.md). */
	BUS_DUCT_1200("bus_duct_1200", "1,200 A generator bus duct", Family.STATION_BUS, 1200, 0.025e-3, 1.0),
	/** The 45 MW peaker's generator bus (docs/multiblocks/aeroderivative.md). */
	BUS_DUCT_3000("bus_duct_3000", "3,000 A generator bus duct", Family.STATION_BUS, 3000, 0.010e-3, 1.0);

	/** Which set of sizes a conductor comes in. */
	public enum Family {
		BUILDING, ACSR, MV_CABLE, BUSWAY, STATION_BUS
	}

	/** What kind of block or item carries the conductor. */
	public enum Kind {
		WIRE, CABLE, BUSWAY
	}

	public static final com.mojang.serialization.Codec<ConductorSize> CODEC = StringRepresentable.fromEnum(ConductorSize::values);

	private final String id;
	private final String label;
	private final Family family;
	private final double amps;
	private final double ohmsPerMeter;
	private final double spanFactor;

	ConductorSize(String id, String label, Family family, double amps, double ohmsPerMeter, double spanFactor) {
		this.id = id;
		this.label = label;
		this.family = family;
		this.amps = amps;
		this.ohmsPerMeter = ohmsPerMeter;
		this.spanFactor = spanFactor;
	}

	@Override
	public String getSerializedName() {
		return id;
	}

	public String label() {
		return label;
	}

	public Family family() {
		return family;
	}

	/** Ampacity: past it the conductor heats. */
	public double amps() {
		return amps;
	}

	public double ohmsPerMeter() {
		return ohmsPerMeter;
	}

	/** The share of the tier's longest span this conductor may have: heavier conductors sag more. */
	public double spanFactor() {
		return spanFactor;
	}

	/** The sizes a conductor of this tier and kind comes in. Building wire up to 480 V; ACSR and MV cable above. */
	public static Family family(Voltage tier, Kind kind) {
		boolean building = tier == Voltage.LOW || tier == Voltage.INDUSTRIAL;
		return switch (kind) {
			case BUSWAY -> building ? Family.BUSWAY : Family.STATION_BUS;
			case WIRE -> building ? Family.BUILDING : Family.ACSR;
			case CABLE -> building ? Family.BUILDING : Family.MV_CABLE;
		};
	}

	public static List<ConductorSize> sizes(Family family) {
		return Arrays.stream(values()).filter(s -> s.family == family).toList();
	}

	public static List<ConductorSize> sizes(Voltage tier, Kind kind) {
		return sizes(family(tier, kind));
	}

	/** The size an unsized conductor of this tier takes: the one nearest the single rating each tier had before sizes. */
	public static ConductorSize standard(Voltage tier, Kind kind) {
		return switch (family(tier, kind)) {
			case BUILDING -> tier == Voltage.INDUSTRIAL ? AWG_2 : AWG_12;
			case ACSR -> switch (tier) {
				case DISTRIBUTION -> ACSR_PENGUIN;
				case HIGH -> ACSR_LINNET;
				default -> ACSR_SPARROW;
			};
			case MV_CABLE -> MV_500;
			case BUSWAY -> BUSWAY_800;
			case STATION_BUS -> tier == Voltage.GENERATOR ? BUS_DUCT_1200 : STATION_BUS_2000;
		};
	}

	/** {@code size} if it belongs with this tier and kind, otherwise the standard one. */
	public static ConductorSize orStandard(ConductorSize size, Voltage tier, Kind kind) {
		return size != null && size.family == family(tier, kind) ? size : standard(tier, kind);
	}

	public static ConductorSize byId(String id) {
		for (ConductorSize size : values()) {
			if (size.id.equals(id)) {
				return size;
			}
		}
		return null;
	}
}
