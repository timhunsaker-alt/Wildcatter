package dev.wildcatter;

/**
 * Every number that decides how the machines behave, in one place.
 * Electricity is in real units: volts, amps, ohms and watts. Times are ticks (20 a second).
 */
public final class Tuning {
	private Tuning() {
	}

	// ---- The circuit simulation --------------------------------------------------------------------------------
	public static final int SOLVE_INTERVAL = 4;               // ticks between solves of every circuit (and one at once when something changes)
	public static final int MAX_CIRCUIT_NODES = 800;          // bigger groups are not simulated
	public static final double GROUND_TIE_OHMS = 0.001;       // neutral is bonded to ground where the power comes in
	public static final double SHORT_CONTACT_OHMS = 0.02;     // two bare wires of opposite roles touching: a short
	public static final double CONTACT_OHMS = 0.002;          // a wire meeting a machine's terminal

	// ---- Boiler (heated by a lit campfire underneath) ----------------------------------------------------------
	public static final int BOILER_WATER_CAPACITY = 4000;     // water units; a bucket is 1000
	public static final int BOILER_STEAM_CAPACITY = 400;
	public static final int BOILER_STEAM_PER_TICK = 2;        // from a campfire
	public static final int STEAM_PER_WATER = 20;             // 1 water makes this much steam: a campfire runs 33 minutes on 4 buckets
	public static final int WATER_PER_BUCKET = 1000;

	// ---- Big machines (3x3x3 multiblocks) ------------------------------------------------------------------------
	/** 27 boilers on top of 9 furnaces make a coil boiler. Each lit furnace under it does 1/9 of the work. */
	public static final int BIG_SIZE = 3;
	public static final int STRUCTURE_CHECK_TICKS = 20;       // how often a structure is checked / looked for
	/** 27 steam turbines in a cube make a medium voltage generator: 27 x 2 kW = 54 kW at 4.16 kV. */
	public static final double MV_GENERATOR_OHMS = 16.0;      // its output sags about 5% at full load (13 A)

	// ---- Steam turbine -----------------------------------------------------------------------------------------
	/** Energy in one unit of steam. A campfire boiler (2 steam a tick) therefore makes 2 kW. */
	public static final double JOULES_PER_STEAM = 50.0;
	public static final double TURBINE_INTERNAL_OHMS = 0.5;  // its output sags as the current rises
	public static final double TURBINE_STEAM_SMOOTHING = 0.3; // how fast the output follows the steam supply

	// ---- Wire: resistance per block and the current it can carry before it heats up ------------------------------
	public static final double WIRE_OHMS_LOW = 0.02;
	public static final double WIRE_OHMS_MEDIUM = 0.5;
	public static final double WIRE_OHMS_HIGH = 2.0;
	public static final double WIRE_AMPS_LOW = 20.0;          // about 12 gauge copper
	public static final double WIRE_AMPS_MEDIUM = 200.0;
	public static final double WIRE_AMPS_HIGH = 600.0;
	/** A wire carrying twice its rating burns out after about this long. Hotter, faster. */
	public static final double WIRE_BURN_SECONDS_AT_2X = 6.0;
	public static final double WIRE_COOL_SECONDS = 20.0;

	// ---- Wool insulation: it wears with time, current and weather ------------------------------------------------
	public static final double WOOL_LIFE_TICKS = 72000;       // three game days of ordinary use
	public static final double WOOL_LOAD_WEAR = 4.0;          // extra wear per (current / rating)^2
	public static final double WOOL_WET_WEAR = 3.0;           // in the rain or in water
	public static final int WEAR_STAGES = 3;                  // 0 fresh, 1 and 2 fraying, 3 worn out (bare)

	// ---- Loads ---------------------------------------------------------------------------------------------------
	public static final double LAMP_WATTS = 60.0;
	public static final double HEATER_WATTS = 1500.0;
	public static final double BRIGHT_ABOVE = 0.5;            // a load works if it gets more than this fraction of its voltage
	public static final double BURNOUT_ABOVE = 1.3;           // and burns out if it gets more than this
	public static final int BURNOUT_TICKS = 20;

	// ---- Breaker ---------------------------------------------------------------------------------------------------
	public static final int[] BREAKER_AMPS = {15, 20, 30, 50};
	public static final double BREAKER_MAGNETIC_MULTIPLE = 8; // instant trip at this many times the rating: a short circuit
	public static final double BREAKER_TRIP_SECONDS_AT_2X = 4.0; // a breaker carrying twice its rating trips after about this long
	public static final double BREAKER_COOL_SECONDS = 10.0;
	public static final double BREAKER_CLOSED_OHMS = 0.002;

	// ---- Transformer ------------------------------------------------------------------------------------------------
	/** Winding resistance on the low side; the high side's is this times the turns ratio squared. It is where the heat comes from. */
	public static final double TRANSFORMER_LOW_WINDING_OHMS = 0.2;

	// ---- Electric shock ---------------------------------------------------------------------------------------------
	// Damage is in half-hearts (2 = one heart) at full voltage, and doubles if you are wet (in water or in rain).
	public static final float SHOCK_DAMAGE_LOW = 2.0F;        // 110 V: it hurts
	public static final float SHOCK_DAMAGE_MEDIUM = 8.0F;     // 4.16 kV: four hearts
	public static final float SHOCK_DAMAGE_HIGH = 20.0F;      // 138 kV: no
	public static final float WET_SHOCK_MULTIPLIER = 2.0F;
	public static final int SHOCK_INTERVAL = 10;              // ticks between shocks from a live wire
	public static final double SHOCK_MIN_VOLTS = 30.0;        // under this you feel nothing

	// A bare live wire shocks anyone this close (blocks, measured from the wire to the player's body).
	// Higher voltage arcs further, so bare lines have to be up on tall poles.
	public static final double BARE_REACH_LOW = 0.3;
	public static final double BARE_REACH_MEDIUM = 2.0;
	public static final double BARE_REACH_HIGH = 4.0;

	// Touching a live machine that has no ground connection: the chance it bites.
	public static final double TOUCH_SHOCK_CHANCE_LOW = 0.15;
	public static final double TOUCH_SHOCK_CHANCE_MEDIUM = 0.6;
	public static final double TOUCH_SHOCK_CHANCE_HIGH = 1.0;

	/** How far a ground wire may run from a machine to its ground rod. */
	public static final int GROUND_SEARCH_LIMIT = 64;
}
