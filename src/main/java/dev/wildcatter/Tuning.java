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
	public static final int MAX_CIRCUIT_NODES = 4000;         // bigger groups are not simulated (the banded phasor solve stays a few ms well past this)
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
	public static final int STRUCTURE_CHECK_TICKS = 20;       // how often a structure is checked / looked for
	/** 27 steam turbines in a cube make a medium voltage generator: 27 x 2 kW = 54 kW at 4.16 kV. */

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
	public static final double INERTIA_STEAM = 3.0;            // seconds, steam turbine
	public static final double AGC_SECONDS = 30.0;             // secondary control brings the frequency back (assumed; not in the approved table)
	public static final double GENERATOR_TRIP_SECONDS = 1.0;   // outside the safe band this long and a generator trips (assumed delay)
	// ---- Generator stator (docs/phase3-three-phase.md, gap fill) ----
	public static final double GENERATOR_RATED_PF = 0.8;       // nameplate power factor: kVA = rated kW / 0.8
	public static final double STATOR_I2T_SECONDS = 37.5;      // IEEE C50.13: (I^2 - 1) t = 37.5 s per unit
	public static final double STATOR_COOL_SECONDS = 120.0;    // from the limit back to cold below rating (as the overload relay)
	// ---- Overhead spans (docs/phase2-distribution.md) ----
	public static final double SPAN_METERS_PER_BLOCK = 1.0;    // true scale (docs/multiblocks/poles.md); 4 m before Phase 4.5e
	public static final double SPAN_SAG = 0.025;               // drawn sag, a fraction of the span: ACSR at typical tension sags 2 to 3 %
	public static final int SPAN_MAX_LOW = 32;                 // 32 m: a service drop
	public static final int SPAN_MAX_DISTRIBUTION = 40;        // 40 m: a distribution span
	public static final int SPAN_MAX_HIGH = 256;               // 256 m: a transmission span
	public static final int SPOOL_SPAN_BLOCKS = 1024;          // how much span a conductor spool holds: 1,024 m
	public static final int INSULATOR_FLASHOVER_TICKS = 60;    // an under-rated insulator under load lasts this long
	public static final double POLE_TRANSFORMER_VA = 25_000;     // a 25 kVA pole-top unit
	public static final double POLE_TRANSFORMER_FAIL_SECONDS = 10.0;   // at 150 % of its rating it fails in this long
	public static final double POLE_TRANSFORMER_COOL_SECONDS = 30.0;
	public static final double TRANSFORMER_X_OVER_R = 5.0;   // reactance to resistance of a distribution transformer's impedance
	public static final double POLE_TRANSFORMER_PERCENT_Z = 2.5;   // nameplate impedance of a 25 kVA pole-top unit (docs/phase3-three-phase.md)
	public static final double RECEPTACLE_BURN_SECONDS = 30.0;  // sustained 120 % of the plug rating for this long burns the receptacle
	public static final double UNCOOLED_EFFICIENCY = 0.75;
	public static final double COOLING_FAN_WATTS = 7_500;           // 10 hp, 480 V three-phase, through a gearbox: a 3 m tower fan (4.5c)
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
	public static final double RIG_WATTS = 30_000.0;          // a 40 hp, 480 V three-phase drive: about 45 A (docs/phase3-three-phase.md)
	public static final int DRILL_TICKS_PER_BLOCK = 20;       // at full voltage
	public static final double PUMPJACK_WATTS = 7_500.0;      // a 10 hp, 480 V three-phase motor: about 11 A
	public static final double PUMPJACK_UNITS_PER_TICK = 0.5; // at full pressure and voltage
	public static final int PUMPJACK_TANK = 4000;
	public static final double PUMPJACK_SPM = 9.0;            // strokes a minute: the crank's rpm (docs/multiblocks/well-site.md)
	public static final double RIG_ROTARY_RPM = 80.0;         // the small rig's top head while drilling
	public static final int RIG_JOINT_BLOCKS = 6;             // a 6 m joint of drill pipe: the head's travel down the mast
	public static final double STARTER_PUMP_RPM = 3450.0;     // the 750 W pump's two-pole motor at full load
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
	/**
	 * A single engine is a 110 V set. A 2x2x2 cube of eight joins into one 4.16 kV generator, like the steam turbines do:
	 * 8 diesel engines make 100 kW, 8 gas turbines 250 kW. Each engine's share of the fuel tank is on its own.
	 */
	// ---- Gensets (docs/multiblocks/genset.md; Phase 4 plan, approved 2026-10-30) ----
	public static final double GENSET_DIESEL_WATTS = 500_000.0;    // 625 kVA at 0.8 PF
	public static final double GENSET_GAS_WATTS = 1_000_000.0;     // 1,250 kVA at 0.8 PF
	public static final double GENSET_TANK_LITERS = 2_000.0;       // sub-base tank: about 15 h at full load
	public static final double DIESEL_FULL_LOAD_LPH = 130.0;       // a 500 kW set: about 35 gal/h at full load
	public static final double GAS_FULL_LOAD_M3H = 265.0;          // a 1 MW lean-burn set at 9.5 MJ/kWh, gas at 38 MJ/m3
	public static final double GENSET_NO_LOAD_FUEL = 0.15;         // share of full-load fuel at no load (proposed, genset fuel charts)
	public static final double GENSET_START_SECONDS = 10.0;        // NFPA 110 Type 10 (proposed)
	public static final double GENSET_STOP_SECONDS = 10.0;         // coast-down (proposed)
	public static final int GENSET_POLES = 4;                      // 1,800 rpm at 60 Hz, 1,500 rpm at 50 Hz
	public static final double INERTIA_GENSET = 1.0;               // seconds
	// ---- Synchronizing (Phase 4 plan, approved 2026-10-30) ----
	public static final double SYNC_MAX_DEGREES = 10.0;            // sync-check window: phase angle
	public static final double SYNC_MAX_HZ = 0.1;                  // slip frequency
	public static final double SYNC_MAX_VOLTS = 0.05;              // voltage difference, per unit
	public static final double AUTO_SYNC_SLIP_HZ = 0.05;           // an auto-synchronizer closes slightly fast, so the set picks up load
	public static final double ALTERNATOR_XDPP = 0.15;             // subtransient reactance X''d, per unit
	public static final double SYNC_DAMAGE_PU = 3.0;               // an out-of-sync close above this many per unit damages the alternator
	public static final double[] SYNC_TRIM_HZ = {-0.2, -0.1, 0.0, 0.1, 0.2};   // the manual raise/lower speed settings
	public static final double PT_OHMS = 1.0e6;                    // the potential transformers the synchronizer reads the bus through
	public static final double ENGINE_SAG = 0.05;             // output voltage sags this much at full load
	/** A refinery base alone is a pot still: slow, a poor yield of diesel, and no plastic. Two columns on top make a full tower. */
	public static final int REFINERY_COLUMNS = 2;
	public static final double POT_STILL_SPEED = 0.5;
	public static final double POT_STILL_YIELD = 0.4;

	// ---- Steam turbine -----------------------------------------------------------------------------------------
	/** Energy in one unit of steam. A campfire boiler (2 steam a tick) therefore makes 2 kW. */
	public static final double JOULES_PER_STEAM = 50.0;
	public static final double TURBINE_INTERNAL_OHMS = 0.5;  // its output sags as the current rises
	public static final double TURBINE_STEAM_SMOOTHING = 0.3; // how fast the output follows the steam supply

	// ---- Wire heating (the sizes, their resistance and ampacity are in energy/ConductorSize) ---------------------
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
	// Induction motors (docs/phase3-three-phase.md)
	public static final double MOTOR_POWER_FACTOR = 0.80;
	public static final double MOTOR_LOCKED_ROTOR_MULTIPLE = 6.0;      // starting current, NEMA design B
	public static final double MOTOR_LOCKED_ROTOR_POWER_FACTOR = 0.35;
	public static final double MOTOR_START_SECONDS = 2.0;
	public static final double PUMP_START_LEVEL = 0.8;
	// Motor starters and the motor control center (docs/phase3-three-phase.md, docs/multiblocks/motor-control-center.md)
	public static final double[] OVERLOAD_SETTINGS = {1.5, 3, 5, 8, 11, 16, 22, 32, 45, 63};   // the overload relay's full-load-amps dial
	public static final int OVERLOAD_DEFAULT = 4;               // 11 A, a 10 hp motor at 480 V
	public static final double OVERLOAD_PICKUP = 1.15;          // never trips at or below 115 % of the setting
	public static final double OVERLOAD_CLASS_SECONDS = 10.0;   // class 10: trips within 10 s at 6 times the setting
	public static final double OVERLOAD_COOL_SECONDS = 120.0;
	public static final double MCP_MULTIPLE = 13.0;             // the motor circuit protector trips at once at 13 times the setting (NEC 430.52)
	public static final int MCC_MAX_BUCKETS = 6;                 // the water pump starts below this full, and stops when the boilers are full
	public static final double THREE_PHASE_HEATER_WATTS = 15_000.0;   // an industrial duct or unit heater (docs/phase3-three-phase.md, proposed)
	public static final double PADMOUNT_208_VA = 75_000;      // 12.47 kV to 208Y/120 V
	public static final double PADMOUNT_208_PERCENT_Z = 3.0;
	public static final double PADMOUNT_480_VA = 300_000;     // 12.47 kV to 480Y/277 V
	public static final double PADMOUNT_480_PERCENT_Z = 5.0;
	public static final double UNIT_SUBSTATION_VA = 1_000_000;   // 4.16 kV to 480Y/277 V
	public static final double UNIT_SUBSTATION_PERCENT_Z = 5.75;
	public static final double GSU_VA = 1_500_000;               // generator step-up, 4.16 kV delta / 12.47 kV Y (docs/multiblocks/gsu.md)
	public static final double GSU_PERCENT_Z = 5.75;
	// ---- Substation (docs/multiblocks/substation.md; Phase 4 plan, approved 2026-10-30) ----
	public static final double SUBSTATION_VA = 20_000_000;         // 138/12.47 kV delta-wye, ONAN
	public static final double SUBSTATION_PERCENT_Z = 8.0;
	public static final double RELAY_51_PICKUP = 1.5;              // time overcurrent picks up at 150 % of full-load current
	public static final double RELAY_50_MULTIPLE = 8.0;            // instantaneous at 800 %
	public static final double RELAY_TIME_DIAL = 1.0;              // IEEE C37.112 moderately inverse: t = TD x (0.0515 / (M^0.02 - 1) + 0.114)
	public static final double DISCONNECT_ARC_AMPS = 10.0;         // opening a disconnect above this draws an arc (proposed: they break only magnetizing current)
	public static final double ARC_FLASH_REACH = 3.0;              // blocks: who is burned by a disconnect's arc (proposed)
	// ---- Steam plant (docs/multiblocks/steam-plant.md; Phase 4 plan, approved 2026-10-30) ----
	public static final double PLANT_WATTS = 1_000_000.0;          // 1,250 kVA at 0.8 PF, 4.16 kV
	public static final double PLANT_EFFICIENCY = 0.25;            // fuel to electricity, overall
	public static final double PLANT_HEATUP_SECONDS = 300.0;       // cold to full pressure at full fire (real: 30 to 60 min)
	public static final double PLANT_MIN_FIRE = 0.10;              // low fire while hot and idle (proposed)
	public static final double STEAM_KG_PER_KWH = 4.54;            // 10 lb/kWh, a small condensing set: 1.26 kg/s at 1 MW
	public static final double DRUM_WATER_KG = 2_000.0;            // drum and tubes; a low level trips the plant (proposed)
	public static final double HOTWELL_KG = 2_000.0;               // the condenser's hotwell
	public static final double FEED_PUMP_WATTS = 3_700.0;          // 5 hp, 480 V three-phase
	public static final double FEED_PUMP_KG_PER_S = 2.0;           // about 120 % of the most steam the drum makes (proposed)
	public static final double TURBINE_RUNUP_SECONDS = 30.0;       // rolls up to speed once the drum is at pressure (proposed)
	public static final int TURBINE_POLES = 2;                     // 3,600 rpm at 60 Hz, 3,000 rpm at 50 Hz
	public static final double COAL_JOULES = 3.5e9;                // a coal block is 1 m3 = 1,300 kg at 24 MJ/kg; one coal is a ninth
	public static final double CHARCOAL_JOULES = 0.8e9;            // about 250 kg/m3 at 29 MJ/kg, a ninth of a cubic metre
	public static final double GAS_JOULES_PER_M3 = 38.0e6;         // natural gas, 1 unit = 1 m3
	public static final double BRIGHT_ABOVE = 0.5;            // a load works if it gets more than this fraction of its voltage
	public static final double BURNOUT_ABOVE = 1.3;           // and burns out if it gets more than this
	public static final int BURNOUT_TICKS = 20;

	// ---- Breaker ---------------------------------------------------------------------------------------------------
	public static final int[] BREAKER_AMPS = {15, 20, 30, 50};               // 110 V
	public static final int[] BREAKER_AMPS_MEDIUM = {30, 60, 100, 200};      // 4.16 kV: a 100 kW generator makes 24 A
	public static final int[] BREAKER_AMPS_HIGH = {100, 200, 400, 600};      // 138 kV
	public static final int[] BREAKER_AMPS_DISTRIBUTION = {200, 400, 600, 1200};   // feeder breakers
	public static final int[] BREAKER_AMPS_INDUSTRIAL = {20, 30, 60, 100, 225, 400};   // 480 V frames and trips
	public static final double BREAKER_MAGNETIC_MULTIPLE = 8; // instant trip at this many times the rating: a short circuit
	public static final double BREAKER_TRIP_SECONDS_AT_2X = 4.0; // a breaker carrying twice its rating trips after about this long
	public static final double BREAKER_COOL_SECONDS = 10.0;
	public static final int[] CUTOUT_K_LINKS = {6, 10, 15, 25, 40, 65, 100};   // standard K-link ratings (docs/phase2-distribution.md)
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
	public static final float SHOCK_DAMAGE_INDUSTRIAL = 4.0F;   // 480 V: two hearts
	public static final float WET_SHOCK_MULTIPLIER = 2.0F;
	public static final int SHOCK_INTERVAL = 10;              // ticks between shocks from a live wire
	public static final double SHOCK_MIN_VOLTS = 30.0;        // under this you feel nothing

	// A bare live wire shocks anyone this close (blocks, measured from the wire to the player's body).
	// Higher voltage arcs further, so bare lines have to be up on tall poles.
	public static final double BARE_REACH_LOW = 0.3;
	public static final double BARE_REACH_MEDIUM = 2.0;
	public static final double BARE_REACH_DISTRIBUTION = 3.0;
	public static final double BARE_REACH_HIGH = 4.0;
	public static final double BARE_REACH_INDUSTRIAL = 0.5;

	// Touching a live machine that has no ground connection: the chance it bites.
	public static final double TOUCH_SHOCK_CHANCE_LOW = 0.15;
	public static final double TOUCH_SHOCK_CHANCE_MEDIUM = 0.6;
	public static final double TOUCH_SHOCK_CHANCE_DISTRIBUTION = 0.8;
	public static final double TOUCH_SHOCK_CHANCE_HIGH = 1.0;
	public static final double TOUCH_SHOCK_CHANCE_INDUSTRIAL = 0.3;

	/** How far a ground wire may run from a machine to its ground rod. */
	public static final int GROUND_SEARCH_LIMIT = 64;

	/** Resistance of one block of wire at a tier. */
	/** The longest span (in blocks) a line of this tier may have between two insulators. */
	public static int maxSpan(dev.wildcatter.energy.Voltage voltage) {
		return switch (voltage) {
			case LOW, INDUSTRIAL -> SPAN_MAX_LOW;
			case MEDIUM, DISTRIBUTION -> SPAN_MAX_DISTRIBUTION;
			case HIGH -> SPAN_MAX_HIGH;
		};
	}
}
