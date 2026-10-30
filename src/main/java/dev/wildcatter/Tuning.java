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

	// ---- Firebox, pipes, pump and drain -----------------------------------------------------------------------------
	/** A firebox burns this many ticks of fuel per tick, but only while the boiler above it wants heat. A 3x3 bank burns 9 cells' worth. */
	public static final double FIREBOX_FUEL_PER_TICK = 1.0;
	public static final int FIREBOX_SLOTS = 9;
	public static final double PUMP_WATTS = 750.0;            // a 110 V motor: 6.8 A
	public static final int PUMP_WATER_PER_TICK = 40;         // at full voltage
	public static final int PLUMBING_INTERVAL = 4;            // ticks between pump / drain transfers
	public static final int PIPE_NETWORK_LIMIT = 512;
	/** Share of the steam a turbine uses that comes back as water through its drain. The rest is lost, so a loop needs some makeup. */
	public static final double CONDENSATE_RETURN = 0.9;
	/** Steam turbines need more steam for the same power when their condensate is not cooled (back pressure). */
	// ---- Frequency (docs/ELECTRICAL_MODEL.md; values approved by Tim 2026-10-30) ----
	public static final double GOVERNOR_DROOP = 0.04;          // a 4% frequency fall takes a generator from no load to full load
	public static final double INERTIA_DIESEL = 1.0;           // seconds, small diesel set
	public static final double INERTIA_STEAM = 3.0;            // seconds, steam turbine
	public static final double INERTIA_GAS_TURBINE = 3.0;      // seconds (assumed like a steam turbine; not in the approved table)
	public static final double AGC_SECONDS = 30.0;             // secondary control brings the frequency back (assumed; not in the approved table)
	public static final double GENERATOR_TRIP_SECONDS = 1.0;   // outside the safe band this long and a generator trips (assumed delay)
	// ---- Overhead spans (docs/phase2-distribution.md) ----
	public static final double SPAN_METERS_PER_BLOCK = 4.0;    // one block of span is 4 m: a 12 m pole is 3 blocks
	public static final int SPAN_MAX_LOW = 8;                  // 32 m: a service drop
	public static final int SPAN_MAX_DISTRIBUTION = 10;        // 40 m: a distribution span
	public static final int SPAN_MAX_HIGH = 64;                // 256 m: a transmission span
	public static final int SPOOL_SPAN_BLOCKS = 256;           // how much span a conductor spool holds
	public static final int INSULATOR_FLASHOVER_TICKS = 60;    // an under-rated insulator under load lasts this long
	public static final double POLE_TRANSFORMER_VA = 25_000;     // a 25 kVA pole-top unit
	public static final double POLE_TRANSFORMER_FAIL_SECONDS = 10.0;   // at 150 % of its rating it fails in this long
	public static final double POLE_TRANSFORMER_COOL_SECONDS = 30.0;
	public static final double UNCOOLED_EFFICIENCY = 0.75;
	public static final double COOLING_FAN_WATTS = 750;
	public static final int COOLING_FILL = 2;                 // fill sections a tower needs above its fan
	public static final double DRAIN_TANK = 100;              // water a drain can hold before it spills

	// ---- Oil and gas wells -------------------------------------------------------------------------------------------
	/** The ground is divided into square cells of this many blocks; each may hold one hidden reservoir. */
	public static final int FIELD_CELL = 64;
	public static final double FIELD_CHANCE = 0.4;
	public static final int FIELD_MIN_RADIUS = 8;
	public static final int FIELD_MAX_RADIUS = 14;
	public static final int FIELD_MIN_Y = -45;                // the top of a reservoir lies between these heights
	public static final int FIELD_MAX_Y = -12;
	public static final int DRY_HOLE_Y = -50;                 // a rig that finds nothing gives up at this height
	public static final int SURVEY_RANGE = 96;                // how far a seismograph can hear a field (blocks)
	public static final double OIL_FRACTION = 0.55;           // of the fields; the rest are gas
	public static final int OIL_MIN_UNITS = 60_000;           // a bucket of crude is 1000 units
	public static final int OIL_MAX_UNITS = 200_000;
	public static final int GAS_MIN_UNITS = 150_000;
	public static final int GAS_MAX_UNITS = 400_000;
	/** Fire in a firebox per unit: crude is 20 ticks a unit (a bucket burns 20000 ticks, like lava); gas is 8. */
	public static final double OIL_FUEL_TICKS_PER_UNIT = 20.0;
	public static final double GAS_FUEL_TICKS_PER_UNIT = 8.0;
	public static final int CRUDE_BUCKET_UNITS = 1000;
	public static final double RIG_WATTS = 2000.0;            // a 110 V drive: 18 A, so it needs a 20 A breaker and good wire
	public static final int DRILL_TICKS_PER_BLOCK = 20;       // at full voltage
	public static final double PUMPJACK_WATTS = 1500.0;       // 13.6 A
	public static final double PUMPJACK_UNITS_PER_TICK = 0.5; // at full pressure and voltage
	public static final int PUMPJACK_TANK = 4000;
	public static final double GAS_UNITS_PER_TICK = 1.5;      // a free-flowing well at full pressure
	/** Fuel a firebox will take from a well before it is full, in ticks of fire. */
	public static final double FIREBOX_FEED_CAP = 200.0;
	public static final int WELL_INTERVAL = 4;                // ticks between a well's transfers

	// ---- Refining and engines ----------------------------------------------------------------------------------------
	public static final double REFINERY_WATTS = 2000.0;       // 110 V, 18 A
	public static final double REFINERY_CRUDE_PER_TICK = 0.5; // at full voltage
	public static final int REFINERY_TANK = 4000;             // crude in, and diesel out
	public static final double DIESEL_YIELD = 0.7;            // units of diesel per unit of crude
	public static final int CRUDE_PER_PLASTIC = 250;          // crude refined for each plastic sheet
	public static final int PLASTIC_BIN = 64;                 // the refinery stops when this many sheets are waiting
	public static final int DIESEL_BUCKET_UNITS = 1000;
	public static final double DIESEL_FUEL_TICKS_PER_UNIT = 16.0;   // burning it in a firebox
	/** Electrical energy in one unit of fuel, after the engine's losses: 100 kW of diesel uses 2.5 units a second. */
	public static final double DIESEL_JOULES_PER_UNIT = 40000.0;
	public static final double GAS_JOULES_PER_UNIT = 15000.0;
	/**
	 * A single engine is a 110 V set. A 2x2x2 cube of eight joins into one 4.16 kV generator, like the steam turbines do:
	 * 8 diesel engines make 100 kW, 8 gas turbines 250 kW. Each engine's share of the fuel tank is on its own.
	 */
	public static final int ENGINE_CUBE = 2;
	public static final double DIESEL_ENGINE_WATTS = 12_500.0;
	public static final double GAS_TURBINE_WATTS = 31_250.0;
	public static final double ENGINE_SAG = 0.05;             // output voltage sags this much at full load
	public static final double DIESEL_TANK = 125.0;           // each engine's own fuel tank, in units
	public static final double GAS_TURBINE_TANK = 12.5;
	/** A refinery base alone is a pot still: slow, a poor yield of diesel, and no plastic. Two columns on top make a full tower. */
	public static final int REFINERY_COLUMNS = 2;
	public static final double POT_STILL_SPEED = 0.5;
	public static final double POT_STILL_YIELD = 0.4;

	// ---- Steam turbine -----------------------------------------------------------------------------------------
	/** Energy in one unit of steam. A campfire boiler (2 steam a tick) therefore makes 2 kW. */
	public static final double JOULES_PER_STEAM = 50.0;
	public static final double TURBINE_INTERNAL_OHMS = 0.5;  // its output sags as the current rises
	public static final double TURBINE_STEAM_SMOOTHING = 0.3; // how fast the output follows the steam supply

	// ---- Wire: resistance per block and the current it can carry before it heats up ------------------------------
	public static final double WIRE_OHMS_LOW = 0.02;
	public static final double WIRE_OHMS_MEDIUM = 0.5;
	public static final double WIRE_OHMS_HIGH = 2.0;
	public static final double WIRE_OHMS_DISTRIBUTION = 0.8;   // between 4.16 kV and 138 kV wire (docs/phase2-distribution.md)
	public static final double WIRE_AMPS_LOW = 20.0;          // about 12 gauge copper
	public static final double WIRE_AMPS_MEDIUM = 200.0;
	public static final double WIRE_AMPS_HIGH = 600.0;
	public static final double WIRE_AMPS_DISTRIBUTION = 400.0;
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
	public static final int[] BREAKER_AMPS = {15, 20, 30, 50};               // 110 V
	public static final int[] BREAKER_AMPS_MEDIUM = {30, 60, 100, 200};      // 4.16 kV: a 100 kW generator makes 24 A
	public static final int[] BREAKER_AMPS_HIGH = {100, 200, 400, 600};      // 138 kV
	public static final int[] BREAKER_AMPS_DISTRIBUTION = {200, 400, 600, 1200};   // feeder breakers
	public static final double BREAKER_MAGNETIC_MULTIPLE = 8; // instant trip at this many times the rating: a short circuit
	public static final double BREAKER_TRIP_SECONDS_AT_2X = 4.0; // a breaker carrying twice its rating trips after about this long
	public static final double BREAKER_COOL_SECONDS = 10.0;
	public static final double FUSE_INSTANT_MULTIPLE = 5;    // a fuse melts at once at this many times its rating (faster than a breaker)
	public static final double FUSE_SECONDS_AT_2X = 2.0;
	public static final double BREAKER_CLOSED_OHMS = 0.002;

	// ---- Transformer ------------------------------------------------------------------------------------------------
	/** Winding resistance on the low side; the high side's is this times the turns ratio squared. It is where the heat comes from. */
	public static final double TRANSFORMER_LOW_WINDING_OHMS = 0.2;

	// ---- Electric shock ---------------------------------------------------------------------------------------------
	// Damage is in half-hearts (2 = one heart) at full voltage, and doubles if you are wet (in water or in rain).
	public static final float SHOCK_DAMAGE_LOW = 2.0F;        // 110 V: it hurts
	public static final float SHOCK_DAMAGE_MEDIUM = 8.0F;     // 4.16 kV: four hearts
	public static final float SHOCK_DAMAGE_DISTRIBUTION = 12.0F;   // 12.47 kV: six hearts
	public static final float SHOCK_DAMAGE_HIGH = 20.0F;      // 138 kV: no
	public static final float WET_SHOCK_MULTIPLIER = 2.0F;
	public static final int SHOCK_INTERVAL = 10;              // ticks between shocks from a live wire
	public static final double SHOCK_MIN_VOLTS = 30.0;        // under this you feel nothing

	// A bare live wire shocks anyone this close (blocks, measured from the wire to the player's body).
	// Higher voltage arcs further, so bare lines have to be up on tall poles.
	public static final double BARE_REACH_LOW = 0.3;
	public static final double BARE_REACH_MEDIUM = 2.0;
	public static final double BARE_REACH_DISTRIBUTION = 3.0;
	public static final double BARE_REACH_HIGH = 4.0;

	// Touching a live machine that has no ground connection: the chance it bites.
	public static final double TOUCH_SHOCK_CHANCE_LOW = 0.15;
	public static final double TOUCH_SHOCK_CHANCE_MEDIUM = 0.6;
	public static final double TOUCH_SHOCK_CHANCE_DISTRIBUTION = 0.8;
	public static final double TOUCH_SHOCK_CHANCE_HIGH = 1.0;

	/** How far a ground wire may run from a machine to its ground rod. */
	public static final int GROUND_SEARCH_LIMIT = 64;

	/** Resistance of one block of wire at a tier. */
	public static double wireOhms(dev.wildcatter.energy.Voltage voltage) {
		return switch (voltage) {
			case LOW -> WIRE_OHMS_LOW;
			case MEDIUM -> WIRE_OHMS_MEDIUM;
			case DISTRIBUTION -> WIRE_OHMS_DISTRIBUTION;
			case HIGH -> WIRE_OHMS_HIGH;
		};
	}

	/** The longest span (in blocks) a line of this tier may have between two insulators. */
	public static int maxSpan(dev.wildcatter.energy.Voltage voltage) {
		return switch (voltage) {
			case LOW -> SPAN_MAX_LOW;
			case MEDIUM, DISTRIBUTION -> SPAN_MAX_DISTRIBUTION;
			case HIGH -> SPAN_MAX_HIGH;
		};
	}
}
