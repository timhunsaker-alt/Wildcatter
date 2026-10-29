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

	// High-voltage line (138 kV): the long haul. Bare on tall poles, or insulated and buried.
	public static final int HIGH_CABLE_BUFFER = 20000;
	public static final int HIGH_CABLE_THROUGHPUT = 10000;

	// Electric shock. Damage is in half-hearts (2 = one heart) and doubles if you are wet (in water or in rain).
	public static final float SHOCK_DAMAGE_LOW = 2.0F;        // 110 V: it hurts
	public static final float SHOCK_DAMAGE_MEDIUM = 8.0F;     // 4.16 kV: four hearts
	public static final float SHOCK_DAMAGE_HIGH = 20.0F;      // 138 kV: no
	public static final float WET_SHOCK_MULTIPLIER = 2.0F;
	public static final int SHOCK_INTERVAL = 10;              // ticks between shocks from a live wire

	// A bare live wire shocks anyone this close (blocks, measured from the wire to the player's body).
	// Higher voltage arcs further, so bare lines have to be up on tall poles: 4 blocks of clearance is not enough at 138 kV.
	public static final double BARE_REACH_LOW = 0.3;          // you have to touch it
	public static final double BARE_REACH_MEDIUM = 2.0;
	public static final double BARE_REACH_HIGH = 4.0;

	// Touching a live machine that has no ground connection: the chance it bites.
	public static final double TOUCH_SHOCK_CHANCE_LOW = 0.15;
	public static final double TOUCH_SHOCK_CHANCE_MEDIUM = 0.6;
	public static final double TOUCH_SHOCK_CHANCE_HIGH = 1.0;

	/** How far a ground wire may run from a machine to its ground rod (blocks of wire). */
	public static final int GROUND_SEARCH_LIMIT = 64;

	/** A device (lamp, machine) needs this many separate cables joined to it to run: one to feed it, one to return. */
	public static final int DEVICE_MIN_CABLES = 2;
}
