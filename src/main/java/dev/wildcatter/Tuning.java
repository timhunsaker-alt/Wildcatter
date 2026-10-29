package dev.wildcatter;

/** Every number that decides how the machines behave, in one place. Energy is RF; times are ticks (20 a second). */
public final class Tuning {
	private Tuning() {
	}

	// Boiler (heated by a lit campfire underneath)
	public static final int BOILER_WATER_CAPACITY = 4000;     // water units; a bucket is 1000
	public static final int BOILER_STEAM_CAPACITY = 400;
	public static final int BOILER_STEAM_PER_TICK = 2;        // from a campfire
	public static final int STEAM_PER_WATER = 20;             // 1 water makes this much steam: a campfire runs 33 minutes on 4 buckets
	public static final int WATER_PER_BUCKET = 1000;

	// Steam turbine
	public static final int TURBINE_BUFFER = 4000;
	public static final int TURBINE_OUTPUT = 80;              // most it pushes into the grid each tick
	public static final int TURBINE_RF_PER_STEAM = 4;         // a campfire boiler = 8 RF a tick

	// Copper cable
	public static final int CABLE_BUFFER = 400;
	public static final int CABLE_THROUGHPUT = 200;           // RF a tick, each way

	// Electric lamp
	public static final int LAMP_BUFFER = 40;
	public static final int LAMP_USE = 2;                     // RF a tick while lit

	// Copper cable is the 110 V line; the insulated line carries medium voltage (4.16 kV) and moves far more.
	public static final int MEDIUM_CABLE_BUFFER = 4000;
	public static final int MEDIUM_CABLE_THROUGHPUT = 2000;

	// Transformer: steps between two neighbouring levels. Real ones are about 96% efficient; the rest is heat.
	public static final int TRANSFORMER_BUFFER = 4000;
	public static final int TRANSFORMER_THROUGHPUT = 2000;    // RF a tick, either way
	public static final double TRANSFORMER_EFFICIENCY = 0.96;
}
