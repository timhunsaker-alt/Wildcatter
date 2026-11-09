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
	public static final long SOLVE_BUDGET_NANOS = 10_000_000L; // per level per tick: circuit groups past it wait for the next tick (one always runs); docs/grid-performance.md
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
	public static final double PROCESS_PUMP_WATTS = 37_000.0;  // 480 V three-phase (docs/multiblocks/tailings-cell.md, proposed)
	public static final double MILL_TAILS_SOLIDS = 0.50;       // the thickener's underflow (proposed)
	public static final double CELL_KG_PER_BLOCK = 2_200.0;    // settled tails, dry (proposed)
	public static final double CELL_SLURRY_SOLIDS = 0.40;      // what the tower takes tails as (proposed)
	public static final double CELL_RECLAIM = 0.75;            // of the slurry's water, back on the water pipe (proposed)
	public static final int CELL_MAX_AREA = 4096;              // cells of open floor one tower can serve (proposed)
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
	public static final double TRANSFORMER_I2T_PU2S = 1250.0;   // IEEE C57.109 through-fault curve: (I^2 - 1) t to failure, per unit (25 x for 2 s)
	public static final double TRANSFORMER_COOL_SECONDS = 600.0;   // back from its limit to cold at or below rating (proposed; the oil's own time constant is hours)
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
	// 7a: the gamma scintillometer (docs/ores.md). Range is exaggerated, as the seismograph's is; background is proposed.
	public static final int SCINT_RANGE = 16;                 // blocks it hears uranium over (real gamma: about 0.3 m of rock)
	public static final double SCINT_BACKGROUND_CPS = 50;      // counts per second over barren rock
	public static final double SCINT_CPS_PER_U3O8 = 50_000;   // a block of pure U3O8 at 1 m; pitchblende (60 %) at 1 m reads 30,000 cps
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
	public static final int SMALL_RIG_MAX_DEPTH = 110;        // blocks below the small rig: a balance choice (docs/multiblocks/land-rig.md)

	// ---- The land rig and deep fields (docs/multiblocks/land-rig.md, approved 2026-10-31) -------------------------------
	public static final double LAND_RIG_DRILL_WATTS = 1_900_000.0;      // top drive 300 kW + two 750 kW (1,000 hp) mud pumps + 100 kW auxiliaries
	public static final double LAND_RIG_CONNECTION_WATTS = 850_000.0;   // the 750 kW (1,000 hp) drawworks hoisting + auxiliaries
	public static final double LAND_RIG_IDLE_WATTS = 100_000.0;         // auxiliaries: lights, shakers, mixing, the house's cooling
	public static final double LAND_RIG_POWER_FACTOR = 0.95;            // a VFD house's front end
	public static final double LAND_RIG_FREQUENCY_FLOOR = 0.992;        // the power limit holds the island at 59.5 Hz (IEC 49.6 Hz)
	public static final double LAND_RIG_UNDERVOLTAGE = 0.9;             // the drives stop below 90 % voltage
	public static final int LAND_RIG_TICKS_PER_BLOCK = 4;               // at full power: five times the small rig
	public static final int LAND_RIG_STAND_BLOCKS = 27;                 // a stand of three 9 m (range 2) joints
	public static final int LAND_RIG_CONNECTION_TICKS = 300;            // 15 s to hoist and make up the next stand
	public static final int LAND_RIG_MUD_PER_BLOCK = 80;                // litres of water a metre: an 8.5 in hole is 37 L/m, plus surface losses
	public static final int LAND_RIG_MUD_TANK = 60_000;                 // litres: a 60 m3 (380 bbl) active system
	public static final double TOP_DRIVE_RPM = 120.0;                   // drilling rotary speed
	public static final double MUD_PUMP_SHEAVE_RPM = 480.0;             // 120 strokes a minute, the motor sheave about four times the crank
	public static final double DRAWWORKS_DRUM_RPM = 380.0;              // hoisting 27 m in 15 s on ten lines: 18 m/s of drill line on a 0.9 m drum
	public static final int DEEP_FIELD_CELL = 128;
	public static final double DEEP_FIELD_CHANCE = 0.5;
	public static final int DEEP_FIELD_MIN_RADIUS = 16;
	public static final int DEEP_FIELD_MAX_RADIUS = 24;
	public static final int DEEP_FIELD_MIN_Y = -58;                     // above the bedrock noise (-64 to -60), below every shallow field
	public static final int DEEP_FIELD_MAX_Y = -50;
	public static final int DEEP_OIL_MIN_UNITS = 400_000;
	public static final int DEEP_OIL_MAX_UNITS = 1_000_000;
	public static final int DEEP_GAS_MIN_UNITS = 1_000_000;
	public static final int DEEP_GAS_MAX_UNITS = 2_500_000;
	public static final double GAS_UNITS_PER_TICK = 1.5;      // a free-flowing well at full pressure
	/** Fuel a firebox will take from a well before it is full, in ticks of fire. */
	public static final double FIREBOX_FEED_CAP = 200.0;
	public static final int WELL_INTERVAL = 4;                // ticks between a well's transfers

	// ---- The lease (docs/multiblocks/lease.md, approved 2026-10-31) --------------------------------------------------------
	public static final double WATER_CUT_NEW = 0.10;               // a new oil field's water cut
	public static final double WATER_CUT_DEPLETED = 0.80;          // a drained one's (real fields run past 90 %)
	public static final double GAS_OIL_RATIO = 0.1;                // m3 of gas per litre of oil: 100 m3/m3, about 560 scf/bbl, a light oil
	public static final double CONDENSATE_PER_M3 = 0.056;          // litres per m3 of a gas well's gas: 10 bbl per million scf
	public static final double GAS_WELL_WATER_PER_M3 = 0.0112;     // litres per m3: 2 bbl per million scf
	public static final double SEPARATOR_LIQUID_LPS = 20.0;        // a 36 in x 10 ft three-phase separator, four pumpjacks
	public static final double SEPARATOR_GAS_M3S = 1.5;            // about 4.5 million scf a day at 125 psi
	public static final double SEPARATOR_OIL_HOLD = 800.0;         // litres held in the vessel
	public static final double SEPARATOR_WATER_HOLD = 800.0;
	public static final double SEPARATOR_GAS_HOLD = 10.0;          // m3 (standard) in its gas space at 125 psi
	public static final double SEPARATOR_GAS_WELLS_BELOW = 0.5;    // it takes a gas well's gas only while its gas side is under half full
	public static final double SEPARATOR_FLARE_ABOVE = 0.75;       // the back-pressure valve to the flare opens above three quarters
	public static final double STOCK_TANK_LITERS = 64_000.0;       // a 400 bbl API 12F tank, 12 ft x 20 ft
	public static final double INJECTION_PUMP_WATTS = 75_000.0;    // a 100 hp H-pump: 10 L/s at 6 MPa is 60 kW of water power at 80 %
	public static final double INJECTION_PUMP_LPS = 10.0;          // at full voltage
	public static final double INJECTION_PUMP_START = 2_000.0;     // litres waiting: the water tank's level switch, about 0.2 m up the tank
	public static final double INJECTION_PUMP_SLIP = 0.011;        // its two-pole motor: 3,560 rpm at 60 Hz, 2,967 at 50 Hz

	// ---- The gas plant (docs/multiblocks/gas-plant.md, approved 2026-10-31) -----------------------------------------------
	public static final double GAS_COMPRESSOR_WATTS = 300_000.0;   // 400 hp, 4.16 kV: two stages, 125 to 1,000 psi at 3 MMscfd
	public static final double GAS_COMPRESSOR_M3S = 1.0;           // 3 million scf a day at full voltage
	public static final int GAS_COMPRESSOR_POLES = 6;              // 1,190 rpm at 60 Hz, 990 at 50 Hz
	public static final double GAS_COMPRESSOR_SLIP = 0.008;
	public static final double GAS_COMPRESSOR_UNLOADED_SECONDS = 15.0;   // with nothing to do it runs unloaded this long, then stops
	public static final double DEHYDRATOR_M3S = 1.0;               // a 3 MMscfd packaged TEG unit
	public static final double GLYCOL_FUEL = 0.005;                // the reboiler burns this share of the gas
	public static final double CHILLER_M3S = 1.0;
	public static final double CHILLER_RESIDUE = 0.955;            // m3 of pipeline gas per m3 in: the gas shrinks 4.5 %
	public static final double CHILLER_NGL_PER_M3 = 0.20;          // litres of Y-grade per m3: about 1.5 gallons per Mcf
	public static final double REFRIGERATION_WATTS = 112_000.0;    // a 150 hp propane screw compressor, 480 V
	public static final double STABILIZER_LPS = 0.5;               // litres of Y-grade a second
	public static final double STABILIZER_LPG = 0.80;              // of the Y-grade: propane and butane overhead, the rest C5+
	public static final double LPG_BULLET_LITERS = 58_000.0;       // an 18,000 gallon (68 m3) bullet at its 85 % fill
	public static final double PLANT_HOLD_SECONDS = 5.0;           // each process part holds this much of its output

	// ---- The crude units (docs/multiblocks/crude-units.md, approved 2026-10-31) -------------------------------------------
	public static final double TOPPING_LPS = 1.84;                 // 1,000 bbl a day at full voltage
	public static final double TOPPING_HEATER_WATTS = 0.88e6;      // 3 MMBtu/h fired
	public static final double TOPPING_SKID_WATTS = 11_200.0;      // a 10 hp charge pump and a 5 hp air-cooler fan, 480 V
	public static final double CRUDE_UNIT_LPS = 36.8;              // 20,000 bbl a day
	public static final double CRUDE_HEATER_WATTS = 17.6e6;        // 60 MMBtu/h fired
	public static final double CHARGE_PUMP_WATTS = 187_000.0;      // 250 hp, 4.16 kV: 37 L/s at about 400 psi
	public static final double DESALTER_WATTS = 40_000.0;          // its grid transformer, 480 V
	public static final double CONDENSER_FAN_WATTS = 75_000.0;     // four 25 hp fans, 480 V
	public static final double FUEL_OIL_JOULES_PER_L = 38.0e6;     // residue burned as fuel oil
	public static final double LPG_JOULES_PER_L = 25.0e6;
	public static final double GASOLINE_FUEL_TICKS_PER_UNIT = 15.0;   // a litre in a firebox, beside crude's 20 and diesel's 16
	public static final double KEROSENE_FUEL_TICKS_PER_UNIT = 16.0;
	public static final double RESIDUE_FUEL_TICKS_PER_UNIT = 20.0;
	public static final double CONDENSATE_FUEL_TICKS_PER_UNIT = 16.0;

	// ---- The cracker and polyethylene (docs/multiblocks/cracker.md, approved 2026-10-31) ----------------------------------
	public static final double CRACKER_FEED_LPS = 1.0;             // litres of LPG or gasoline a second at full rate
	public static final double LPG_KG_PER_L = 0.51;
	public static final double GASOLINE_KG_PER_L = 0.72;
	public static final double ETHYLENE_KG_PER_M3 = 1.18;
	public static final double FUEL_GAS_KG_PER_M3 = 0.75;          // methane and hydrogen
	public static final double CRACKER_FUEL_WATTS_LPG = 7.5e6;
	public static final double CRACKER_FUEL_WATTS_GASOLINE = 14.0e6;
	public static final double CRACKER_WATER_LPG = 0.15;           // litres of water a second: dilution steam, 0.3 kg a kg of feed
	public static final double CRACKER_WATER_GASOLINE = 0.36;      // 0.5 kg a kg of feed
	public static final double CRACKED_GAS_COMPRESSOR_WATTS = 224_000.0;   // 300 hp, 4.16 kV
	public static final double COLD_BOX_KGS = 0.6;                 // kg of cracked gas a second, through the compressor and the cold box
	public static final int CRACKED_GAS_COMPRESSOR_POLES = 4;      // 1,780 rpm at 60 Hz
	public static final double QUENCH_KGS = 1.0;                   // kg of hot cracked gas a second
	public static final double RESIDUE_KG_PER_L = 0.95;            // fuel oil
	public static final double PE_REACTOR_KGS = 0.25;              // kg of ethylene a second: sized for the cracker
	public static final double PE_REACTOR_WATTS = 75_000.0;        // its cycle gas compressor, 480 V
	public static final double PE_CONVERSION = 0.98;
	public static final double PELLETIZER_WATTS = 187_000.0;       // 250 hp extruder, 4.16 kV
	public static final double PE_KG_PER_SHEET = 1.0;
	public static final int PELLETIZER_BIN = 256;

	// ---- The sulfuric acid plant (docs/multiblocks/acid-plant.md, approved 2026-11-01) --------------------------------------
	public static final double ACID_PLANT_TPD = 150.0;             // tonnes of H2SO4 a day: a small sulfur-burning DCDA plant
	public static final double ACID_STRENGTH = 0.985;              // the product, by weight
	public static final double ACID_KG_PER_L = 1.84;               // 98.5 % acid
	public static final double MOLTEN_SULFUR_KG_PER_L = 1.79;      // at about 140 C
	public static final double BURNER_SO2_VOLUME = 0.11;           // burner gas, 11 % SO2 (BREF 10.5-11.8 %)
	public static final double ACID_CONVERSION_DOUBLE = 0.998;     // four beds with interpass absorption (BREF 99.7-99.9 %)
	public static final double ACID_CONVERSION_SINGLE = 0.95;      // without it (BREF: 85-95 % before the intermediate absorber)
	public static final double ACID_BLOWER_WATTS = 250_000.0;      // the main blower, 4.16 kV, two-pole
	public static final double ACID_STEAM_PER_ACID = 1.2;          // kg of 40 bar steam a kg of acid from the waste heat boiler
	public static final double MELTER_START_WATTS = 100_000.0;     // gas firing a cold melter: 0.57 kg/s at about 140 kJ/kg
	public static final double ACID_PLANT_WATER_HOLD = 400.0;      // litres a boiler or absorber keeps from its pump

	// ---- The uranium mill (docs/multiblocks/uranium-mill.md, approved 2026-11-01) --------------------------------------------
	public static final double MILL_ORE_KGS = 20.8;                // kg of ore a second: White Mesa, 1,800 t/d
	public static final double MILL_U3O8_KGS = 0.416;              // the most U3O8 in the feed SX and precipitation take: 2 % ore at full rate
	public static final double MILL_RECOVERY = 0.93;               // overall (Rabbit Lake; 90-95 %), all taken in the leach
	public static final double MILL_SLURRY_SOLIDS = 0.55;          // the SAG mill's discharge, by weight
	public static final double LEACH_ACID_KG_PER_T = 70.0;         // kg of H2SO4 a tonne of ore (Rabbit Lake)
	public static final double LEACH_MNO2_KG_PER_T = 1.5;          // pyrolusite, the oxidant (Rössing 0.7-1.5)
	public static final double CCD_WASH_RATIO = 2.5;               // kg of wash water a kg of solids (Rabbit Lake)
	public static final double CCD_UNDERFLOW_SOLIDS = 0.5;         // the underflow to the tailings, by weight
	public static final double SX_STRIP_U3O8_G_PER_L = 25.0;       // the strip liquor (TRS-359)
	public static final double SX_KEROSENE_CHARGE = 10_000.0;      // litres of diluent its settlers hold (proposed)
	public static final double SX_KEROSENE_LOSS_L_PER_M3 = 0.1;    // lost a m3 of pregnant liquor (proposed)
	public static final double SX_NACL_G_PER_L = 87.7;             // the 1.5 M NaCl strip
	public static final double SX_NACL_MAKEUP = 0.1;               // of the strip's salt made up each pass, the rest recycled (proposed)
	public static final double PRECIP_MGO_PER_U3O8 = 0.3;          // kg of MgO a kg of U3O8 (proposed; 0.14 stoichiometric)
	public static final double MGO_IN_MGCO3 = 0.478;               // caustic calcining: the CO2 leaves
	public static final double YELLOWCAKE_U3O8 = 0.80;             // magnesium diuranate concentrate (proposed; ASTM C967 needs 77 %)
	public static final double DRUM_NET_KG = 400.0;                // a 208 L drum (WNTI: up to 450 kg gross)
	public static final double DRYER_WATTS = 500_000.0;            // the spray dryer's gas burner (proposed)
	public static final double JAW_CRUSHER_WATTS = 75_000.0;       // 480 V
	public static final double SAG_MILL_WATTS = 522_000.0;         // White Mesa, 4.16 kV
	public static final double LEACH_AGITATOR_WATTS = 56_000.0;    // White Mesa, 480 V
	public static final double THICKENER_RAKE_WATTS = 7_500.0;     // 480 V
	public static final double SX_MIXER_WATTS = 30_000.0;          // 480 V
	public static final double PRECIP_AGITATOR_WATTS = 15_000.0;   // 480 V
	public static final double DRUM_FILLER_WATTS = 5_000.0;        // 480 V
	public static final double MILL_WATER_HOLD = 1_000.0;          // litres the SAG mill or the thickener keeps from its pump

	// ---- Roll-front deposits and the ISR wellfield (docs/multiblocks/isr-wellfield.md, approved 2026-11-01) ------------------
	public static final int ROLL_CELL = 256;                       // blocks: a grid of its own, at most one front a cell
	public static final double ROLL_CHANCE = 0.5;
	public static final double ROLL_MIN_RADIUS = 40, ROLL_MAX_RADIUS = 80;   // the arc's ring
	public static final double ROLL_ARC_DEGREES = 120;
	public static final double ROLL_MIN_WIDTH = 15, ROLL_MAX_WIDTH = 30;     // the band across the front (real 8-30 m)
	public static final int ROLL_MIN_TOP_Y = -45, ROLL_MAX_TOP_Y = -10;
	public static final int ROLL_MIN_THICKNESS = 3, ROLL_MAX_THICKNESS = 6;
	public static final double ROLL_PEAK_GRADE = 0.01;             // eU3O8 at the band's middle: 10x a real 0.1 %
	public static final double ROLL_EDGE_GRADE = 0.003;            // at its edges
	public static final double ROLL_ROCK_KG_PER_M3 = 2_300.0;      // sandstone
	public static final double ROLL_RECOVERY = 0.70;               // of the uranium in place (WNA: 60-80 %)
	public static final double GAMMA_CUTOFF_GT = 0.6;              // m x % eU3O8: 10x a real 0.2 ft x %
	public static final double ISR_HEAD_GRADE_MG_L = 500.0;        // a full 5-spot: 10x a real 50 mg/L U3O8
	public static final double ISR_PRODUCER_LPS = 2.5;             // 40 gpm (Inkai 35-46 gpm)
	public static final double ISR_INJECTOR_LPS = 1.24;            // a producer's flow less bleed, over about two injectors
	public static final int ISR_PATTERN_RANGE = 25;                // blocks from a producer its injectors may stand (real 12-50 m)
	public static final double ISR_BLEED = 0.01;                   // over-production that draws the wellfield inward (Lost Creek 0.5-1.5 %)
	public static final double ISR_PUMP_WATTS = 5_600.0;           // a 7.5 hp, 4 in submersible at 480 V
	public static final int HEADER_HOUSE_PRODUCERS = 25;           // Lost Creek: about 25 producers and 53 injectors a house
	public static final double O2_DOSE_KG_PER_L = 0.3e-3;          // 300 mg/L (100-500 mg/L, unverified)
	public static final double O2_GENERATOR_NM3H = 50.0;           // a PSA skid (proposed)
	public static final double O2_GENERATOR_WATTS = 25_000.0;      // about 0.5 kWh/Nm3, 480 V (proposed)
	public static final double O2_KG_PER_NM3 = 1.429;
	public static final double IX_CAPTURE = 0.95;                  // GEIS: over 95 %, barren under 5 mg/L
	public static final double IX_MAX_LPS = 63.0;                  // 1,000 gpm: one header house (proposed)
	public static final double IX_RESIN_L = 44_000.0;              // the lead column's resin (Crow Butte, 44 m3)
	public static final double IX_RESIN_KG_PER_L = 0.080;          // U3O8 a litre of loaded resin (about 5 lb/ft3)
	public static final double IX_ELUATE_G_PER_L = 15.0;           // U3O8 in the eluate (GEIS: 8-20 g/L U)
	public static final double IX_ELUATE_ACID_G_PER_L = 10.0;      // sulfuric acid to break the carbonate complex (proposed)

	// ---- Conversion to UF6 (docs/multiblocks/conversion.md, approved 2026-11-01) ---------------------------------------------
	public static final int F2_CELLS = 12;                         // KF.2HF cells in series: the quarter-size plant
	public static final double F2_CELL_KA = 6.0;                   // kA through each
	public static final double F2_CELL_VOLTS = 9.0;                // 8-10 V a cell
	public static final double F2_CURRENT_EFFICIENCY = 0.95;
	public static final double F2_KG_PER_KAH = 0.709;              // Faraday: 38 g of F2 for two moles of electrons
	public static final double F2_HF_PER_F2 = 1.053;               // kg of HF a kg of F2
	public static final double F2_COOLING_LPS = 0.19;              // make-up water: two thirds of 650 kW evaporated
	public static final double RECTIFIER_EFFICIENCY = 0.97;        // (unverified)
	public static final double RECTIFIER_POWER_FACTOR = 0.95;      // 12-pulse diode bridge (unverified)

	// ---- DC tier (docs/multiblocks/dc-bus.md, approved with the 10b1 plan 2026-11-03; all proposed) ------------------------------
	public static final double DC_BUS_4K_AMPS = 4000;              // 4 x 100 x 10 mm copper, about 1 A/mm2 bare in air
	public static final double DC_BUS_4K_OHMS_PER_M = 5.2e-6;      // at 75 C
	public static final double DC_BUS_16K_AMPS = 16000;            // 8 x 200 x 10 mm copper
	public static final double DC_BUS_16K_OHMS_PER_M = 1.3e-6;
	public static final double[] RECTIFIER_SETPOINTS_KA = {1, 2, 2.8, 4, 6, 8, 10, 12, 14, 16};
	public static final int RECTIFIER_SETPOINT_DEFAULT = 4;        // 6 kA, the fluorine cell house's
	public static final double RECTIFIER_MAX_DC_VOLTS = 150.0;     // 16 kA x 150 V = 2.4 MW
	public static final double RECTIFIER_DC_OHMS = 0.1e-3;         // its regulation: about 1 % of 150 V at 16 kA (its losses are the 97 %)
	public static final double RECTIFIER_RAMP_VOLTS_PER_S = 15.0;  // an on-load tap changer steps in seconds
	public static final double RECTIFIER_OVERCURRENT = 1.25;       // x setpoint for RECTIFIER_OVERCURRENT_SECONDS: it trips
	public static final double RECTIFIER_OVERCURRENT_SECONDS = 1.0;
	public static final double RECTIFIER_OVERCURRENT_INSTANT = 2.0; // x setpoint with its terminals collapsed (a bolted short): it trips at once
	public static final double RECTIFIER_SHORT_VOLTS = 0.5;         // terminals below this share of its EMF: collapsed
	public static final double DC_JOINT_OHMS = 2e-6;               // a bolted bus bar joint, a few micro-ohms
	public static final double DC_DETECTOR_OHMS = 10_000;          // each pole to earth through the ground-fault detector
	public static final double DC_DETECTOR_ALARM = 0.30;           // a pole's share of the voltage outside 30-70 %: GROUND FAULT
	public static final double DC_SHOCK_MIN_VOLTS = 120.0;         // IEC 61201 ripple-free DC ELV limit
	public static final double DC_SHOCK_FULL_VOLTS = 600.0;        // the 480 V tier's shock at this, halved (IEC 60479-1)
	public static final double DC_SHOCK_FACTOR = 0.5;
	public static final double F2_CELL_BACK_EMF = 2.9;             // KF.2HF decomposition
	public static final double MG_CELL_BACK_EMF = 2.5;             // MgCl2 at 700 C
	public static final double CA_CELL_BACK_EMF = 2.2;             // membrane cell, reversible

	// ---- The Bayer refinery, lime kiln and AlF3 (docs/multiblocks/bayer-refinery.md, alumina-plant.md, 2026-11-03; proposed) ----
	public static final double BAYER_ALUMINA_KGS = 105_000.0 / 86_400;   // 105 t a day, for a 40-pot 180 kA line (Tim, 2026-11-03)
	public static final double BAYER_CATCH_UP = 1.2;               // a part runs up to this times the plant rate while it catches up
	public static final double AL2O3_IN_GIBBSITE = 101.96 / 156.0; // Al2O3 in 2 Al(OH)3
	public static final double BAYER_EXTRACTION = 0.93;            // of the gibbsite, at low-temperature digestion
	public static final double BAYER_PRODUCTIVITY = 80.0;          // kg of Al2O3 a m3 of liquor (70-90)
	public static final double BAYER_LIQUOR_KG_PER_L = 1.30;
	public static final double MUD_UNDERFLOW_SOLIDS = 0.35;        // settler and washer underflow
	public static final double RED_MUD_CAKE_SOLIDS = 0.70;         // filter press cake for dry stacking (65-75 %)
	public static final double RED_MUD_BLOCK_KG = 1_800.0;         // cake in a placed block of red mud
	public static final double BAYER_WASH_PER_ALUMINA = 2.0;       // kg of wash water a kg of alumina (2 m3 a t)
	public static final double BAYER_NAOH_PER_ALUMINA = 0.060;     // soda loss, kg of NaOH a kg (40-80 kg a t)
	public static final double CAUSTIC_STRENGTH = 0.32;            // the chlor-alkali cell's caustic soda, NaOH by weight
	public static final double BAYER_LIME_PER_ALUMINA = 0.040;     // kg of CaO a kg (30-50 kg a t)
	public static final double DIGEST_STEAM_PER_ALUMINA = 1.5;     // kg of 10 bar steam a kg, after the flash heat recovery
	public static final double EVAPORATOR_STEAM_PER_WATER = 0.4;   // three effects
	public static final double CALCINER_J_PER_KG = 3.0e6;          // gas-suspension calciner, a kg of alumina
	public static final double BAYER_LIQUOR_CHARGE_SECONDS = 600;  // strong liquor in the evaporator's storage when it is built (the first fill waived)
	public static final double PROCESS_BOILER_KGS = 10_000.0 / 3600; // a 10 t/h package boiler
	public static final double PROCESS_STEAM_J_PER_KG = 2.34e6;    // 10 bar saturated from 105 C feedwater
	public static final double PROCESS_BOILER_EFFICIENCY = 0.85;
	public static final double BAYER_WATER_HOLD = 2_000.0;         // litres the washer and the boiler keep from their pumps
	public static final double BALL_MILL_WATTS = 110_000.0;        // about 10 kWh a t of bauxite
	public static final double DIGESTER_WATTS = 75_000.0;          // agitator
	public static final double FLASH_PUMP_WATTS = 37_000.0;
	public static final double SETTLER_RAKE_WATTS = 7_500.0;
	public static final double FILTER_PRESS_WATTS = 30_000.0;
	public static final double POLISHING_FILTER_WATTS = 45_000.0;
	public static final double PRECIPITATOR_WATTS = 75_000.0;
	public static final double HYDRATE_FILTER_WATTS = 90_000.0;    // vacuum pump
	public static final double CALCINER_FAN_WATTS = 250_000.0;     // 4.16 kV
	public static final double EVAPORATOR_WATTS = 75_000.0;
	public static final double CAUSTICIZER_WATTS = 15_000.0;
	public static final double PROCESS_BOILER_WATTS = 45_000.0;    // feed pump and FD fan
	public static final double[] HYDRATE_TO_BAGS = {0, 0.10, 0.50, 1.0};
	public static final double BULK_BAG_KG = 1_000.0;              // an FIBC
	public static final double CAO_PER_CACO3 = 56.08 / 100.09;
	public static final double LIME_KILN_KGS = 10_000.0 / 86_400;  // 10 t of quicklime a day
	public static final double LIME_KILN_J_PER_KG = 4.0e6;         // a vertical shaft kiln (3.6-4.5 GJ a t)
	public static final double LIME_KILN_WATTS = 45_000.0;
	public static final double ALF3_KGS = 50.0 / 3600;            // 50 kg an hour
	public static final double HF_PER_ALF3 = 3 * 20.006 / 83.977;
	public static final double HYDRATE_PER_ALF3 = 78.004 / 83.977;
	public static final double ALF3_J_PER_KG = 2.0e6;              // drying and heating the hydrate
	public static final double ALF3_WATTS = 55_000.0;
	public static final double BAGGING_WATTS = 5_000.0;
	public static final double CL2_TON_KG = 907.0;                 // a ton container (The Chlorine Institute)
	public static final double CL2_TON_TARE = 680.0;
	public static final double CL2_FILLER_KGS = 360.0 / 3600;
	public static final double CL2_FILLER_WATTS = 30_000.0;        // refrigeration, about 100 kWh a t
	public static final double CAUSTIC_HOLD_KG = 500.0;            // 32 % caustic a chlor-alkali cell holds

	// ---- Coke, pitch and anodes (docs/multiblocks/delayed-coker.md, coke-ovens.md, anode-plant.md, 2026-11-03; proposed) -------
	public static final double CARBON_CATCH_UP = 1.2;              // a part runs up to this times its plant rate while it catches up
	public static final double COKER_RESIDUE_KGS = 169_000.0 / 86_400;   // 169 t a day of residue, for a 40-pot line's coke (Tim, 2026-11-03)
	public static final double COKER_COKE_YIELD = 0.15;            // of atmospheric residue
	public static final double COKER_GAS_OIL_YIELD = 0.65;
	public static final double COKER_NAPHTHA_YIELD = 0.13;
	public static final double COKER_GAS_YIELD = 0.07;
	public static final double COKER_GAS_OIL_KG_PER_L = 0.90;
	public static final double COKER_GAS_KG_PER_M3 = 1.0;          // methane to butane and hydrogen
	public static final double COKER_GAS_JOULES_PER_M3 = 45.0e6;
	public static final double COKER_HEATER_J_PER_KG = 1.1e6;      // residue to 495 C
	public static final double COKE_DRUM_SWITCH_KG = 1_000.0;      // a drum switches once it holds this (a real one fills 12-24 h)
	public static final double COKE_DRUM_MAX_KG = 40_000.0;
	public static final double COKE_CUT_KGS = 10_000.0 / 3600;     // hydraulic decoking, 10 t an hour
	public static final double COKE_CUT_WATER_PER_KG = 0.3;        // litres made up a kg of coke cut
	public static final double GREEN_COKE_KG = 1_000.0;            // a piece of green petroleum coke out of the pit
	public static final double COKE_CALCINE_YIELD = 0.78;          // moisture and volatiles off (20-25 %)
	public static final double COKE_KILN_J_PER_KG = 1.5e6;         // gas a kg of calcined coke, besides its own volatiles
	public static final double CARBON_WATER_HOLD = 2_000.0;        // litres the pit and the quench tower keep from their pumps
	public static final double CARBON_HOLD_KG = 2_000.0;           // solids a kiln, cooler or press keeps for the next part
	public static final double COKER_PUMP_WATTS = 75_000.0;        // the heater's charge pump
	public static final double COKER_FRACTIONATOR_WATTS = 45_000.0;
	public static final double COKE_DRUM_WATTS = 15_000.0;         // unheading hydraulics
	public static final double COKE_CUTTER_WATTS = 400_000.0;      // 4.16 kV, about 200 bar
	public static final double COKE_KILN_WATTS = 75_000.0;
	public static final double COKE_COOLER_WATTS = 30_000.0;
	public static final double COKE_OVEN_COAL_KGS = 240_000.0 / 86_400;   // 240 t of coal a day
	public static final double COKE_OVEN_COKE_YIELD = 0.75;
	public static final double COKE_OVEN_GAS_PER_KG = 0.40;        // m3 of raw gas a kg of coal
	public static final double COKE_OVEN_UNDERFIRE = 0.40;         // the share of the gas that heats the battery
	public static final double COKE_OVEN_GAS_JOULES_PER_M3 = 18.0e6;
	public static final double COKE_OVEN_TAR_PER_KG = 0.035;       // kg of tar a kg of coal
	public static final double QUENCH_WATER_PER_KG = 0.45;         // litres evaporated a kg of coke
	public static final double COAL_TOWER_HOLD_KG = 10_000.0;
	public static final double MET_COKE_KG = 150.0 * 0.75;         // a piece of coke: one from each coal (COAL_KG)
	public static final double MET_COKE_CARBON = 0.88;
	public static final double MET_COKE_JOULES = MET_COKE_KG * 29.0e6;
	public static final double PITCH_PER_TAR = 0.55;
	public static final double TAR_OIL_KG_PER_L = 1.05;
	public static final double TAR_STILL_J_PER_KG = 1.0e6;
	public static final double COAL_TOWER_WATTS = 15_000.0;
	public static final double LARRY_CAR_WATTS = 30_000.0;
	public static final double PUSHER_WATTS = 75_000.0;
	public static final double QUENCH_PUMP_WATTS = 55_000.0;
	public static final double EXHAUSTER_WATTS = 300_000.0;        // 4.16 kV
	public static final double TAR_STILL_WATTS = 15_000.0;
	public static final double ANODES_PER_SECOND = 25.0 / 86_400;  // 25 anodes a day, for 40 pots
	public static final double BAKED_ANODE_KG = 1_213.0;           // 1.6 x 0.8 x 0.6 m at 1.58 t/m3
	public static final double GREEN_ANODE_KG = 1_275.0;           // the pitch's volatiles burn off in baking
	public static final double PASTE_COKE = 0.65;                  // of the green paste
	public static final double PASTE_BUTTS = 0.20;
	public static final double PASTE_PITCH = 0.15;
	public static final double BUTT_SHARE = 0.25;                  // of a baked anode left when it is changed
	public static final double BUTT_KG = BAKED_ANODE_KG * BUTT_SHARE;
	public static final double BAKING_J_PER_KG = 2.4e6;            // a ring furnace, a kg of baked anode
	public static final int BAKING_FURNACE_PITS = 16;              // green anodes it holds
	public static final double RODDING_SECONDS = 60.0;             // an anode a minute
	public static final double PASTE_PLANT_WATTS = 200_000.0;      // 4.16 kV
	public static final double FORMING_PRESS_WATTS = 110_000.0;
	public static final double BAKING_FAN_WATTS = 75_000.0;
	public static final double RODDING_WATTS = 200_000.0;          // the induction furnace
	// 10b3b: the potline (docs/multiblocks/potline.md), all proposed
	public static final double POT_AMPS = 180_000.0;               // AP18-class prebake
	public static final double POT_BACK_EMF = 1.65;                // decomposition plus anode overvoltage
	public static final double POT_CELL_VOLTS = 4.0;               // across the cell at 180 kA
	public static final double POT_RISER_OHMS = 0.3 / 180_000;     // 0.3 V at 180 kA, pot to pot
	public static final double FARADAY_KG_PER_KAH = 0.3356;        // kg of aluminium a kA.h
	public static final double POT_CURRENT_EFFICIENCY = 0.94;
	public static final double POT_OVERDUE_PENALTY = 0.02;         // current efficiency off for each overdue anode
	public static final double POT_NO_ALF3_PENALTY = 0.04;         // while its AlF3 bin is empty
	public static final double POT_ALUMINA_PER_AL = 1.93;
	public static final double POT_ALF3_TREATED = 0.020;           // kg a kg of aluminium, its gas to a fume treatment centre
	public static final double POT_ALF3_VENTED = 0.045;            // without
	public static final double POT_CARBON_PER_AL = 0.41;           // net anode carbon
	public static final int POT_ANODES = 18;
	public static final double POT_AE_VOLTS = 30.0;                // across the cell in an anode effect, at 180 kA
	public static final double POT_AE_CF4_KG_PER_MIN = 0.143 * 1.363;   // IPCC Tier 2 slope, kg CF4 a t Al per AE-minute a pot-day, x a pot-day's metal
	public static final double POT_AE_CLEAR_SECONDS = 120.0;
	public static final double POT_FREEZE_SECONDS = 4 * 3600.0;
	public static final double POT_FREEZE_AMPS = 18_000.0;         // 10 % of 180 kA: below it the pot loses its heat
	public static final double POT_METAL_HOLD_KG = 2_726.0;        // 2 days of metal over its pad
	public static final double POT_ALUMINA_BIN_KG = 2_000.0;
	public static final double POT_ALF3_BIN_KG = 1_000.0;
	public static final double POT_GAS_M3H = 5_000.0;              // Nm3/h a pot to the duct
	public static final double POT_GAS_HOLD_S = 10.0;              // seconds of a row's gas its end pot holds
	public static final double RECTIFORMER_AMPS = 60_000.0;
	public static final double RECTIFORMER_MAX_DC_VOLTS = 220.0;   // 40 x 4.3 V, the bus, and an anode effect
	public static final double RECTIFORMER_DC_OHMS = 2.2 / 60_000; // 1 % of 220 V at 60 kA
	public static final double RECTIFORMER_RAMP_VOLTS_PER_S = 22.0;
	public static final double RECTIFORMER_EFFICIENCY = 0.985;
	public static final double[] POTLINE_SETPOINTS_KA = {60, 90, 120, 150, 180};
	public static final int POTLINE_SETPOINT_DEFAULT = 4;          // 180 kA
	public static final double POTLINE_JOINT_OHMS = 0.2e-6;        // a welded or clamped aluminium joint
	public static final double DC_BUS_180K_AMPS = 180_000.0;
	public static final double DC_BUS_180K_AL_OHMS_PER_M = 0.063e-6;   // 0.55 m2 of aluminium at 90 C
	public static final double DC_BUS_180K_CU_OHMS_PER_M = 0.12e-6;    // 0.18 m2 of copper
	public static final double FTC_FAN_WATTS = 250_000.0;          // 4.16 kV
	public static final double FTC_FAN_M3H = 100_000.0;            // Nm3/h of pot gas a fan
	public static final int FTC_FANS = 2;
	public static final double FTC_GAS_PER_BAG = 1_000.0 / (109.6 / 5_000.0);   // Nm3 treated for a reacted bag: 109.6 kg/h of alumina for 5,000 Nm3/h
	public static final double CRUCIBLE_KG = 8_000.0;
	public static final double CASTER_KGS = 10_000.0 / 3600;       // 10 t/h
	public static final double CASTER_HOLD_KG = 8_000.0;
	public static final double CASTER_WATTS = 15_000.0;
	public static final double AL_INGOT_KG = 22.7;                 // 50 lb
	// 10c1: blueprints (docs/blueprints.md), proposed
	public static final double PALLET_CAP_TONNES = 16.0;           // the tower crane's most: a pallet never weighs more
	public static final int SITE_CHECK_TICKS = 40;                 // a site marker counts what stands this often
	public static final int GHOST_BLOCKS = 4096;                   // ordinary blocks a ghost draws at most
	public static final double GHOST_RANGE = 32.0;                 // m from the player
	public static final double CONV_F2_PER_U = 0.18;               // kg of F2 a kg of U (0.160 ideal)
	public static final double CONV_H2_PER_U = 8.47e-3;            // kg of H2 a kg of U: U(VI) to U(IV)
	public static final double H2_KG_PER_M3 = 0.0899;
	public static final double CONV_HF_PER_U = 0.43 + 0.084;       // kg of HF a kg of U: 25-30 % excess, and the MgF2
	public static final double UF6_PER_U = 1.479;                  // kg of UF6 a kg of U
	public static final double CYLINDER_48Y_KG = 12_501.0;         // UF6 (ANSI N14.1)
	public static final double CYLINDER_48Y_TARE = 2_359.0;
	public static final double SMR_M3H = 30.0;                     // H2 the packaged reformer makes (proposed)
	public static final double SMR_GAS_PER_H2 = 0.45;              // m3 of natural gas a m3 of H2, feed and fuel
	public static final double SMR_WATER_PER_H2 = 0.015;           // litres of water a m3 of H2: steam:carbon 3
	public static final double HF_KILN_KGS = 0.06;                 // anhydrous HF a second (proposed)
	public static final double HF_FLUORSPAR_PER_HF = 2.165;        // kg of CaF2 a kg of HF (Buss ChemTech)
	public static final double HF_ACID_PER_HF = 2.6;               // kg of H2SO4 a kg of HF
	public static final double HF_FUEL_J_PER_KG = 5.0e6;
	public static final double HF_ANHYDRITE_PER_HF = 3.6;          // kg of CaSO4 a kg of HF (unverified)
	public static final double AHF_KG_PER_L = 0.99;
	public static final double HF_KILN_WATTS = 15_000.0;           // its drive, 480 V
	public static final double COLD_TRAP_WATTS = 30_000.0;         // refrigeration, 480 V (unverified)
	public static final double REBOILER_WATTS = 20_000.0;          // electric, 480 V (unverified)

	// The ammonia plant (docs/multiblocks/ammonia-plant.md): one steam reformer's hydrogen to ammonia, nitric acid, ammonium nitrate
	// and emulsion matrix for blasting. Every rate follows from SMR_M3H through fluid/AmmoniaPlant.
	public static final double N2_GENERATOR_NM3H = 10.0;           // the PSA nitrogen generator, 99.9 % N2: the loop's 1:3
	public static final double N2_GENERATOR_WATTS = 3_000.0;       // about 0.3 kWh/Nm3 (unverified), 480 V
	public static final double SYNGAS_COMPRESSOR_WATTS = 40_000.0; // reciprocating, to 150 bar (proposed), 480 V
	public static final double SYNGAS_LOOP_BAR = 150.0;            // the synthesis loop (proposed; real loops 100-250 bar)
	public static final double AMMONIA_CONVERTER_WATTS = 10_000.0; // its recycle circulator and ammonia chiller (proposed), 480 V
	public static final double AMMONIA_PER_PASS = 0.15;            // converted a pass through the iron catalyst (unverified)
	public static final double AMMONIA_KG_PER_L = 0.61;            // liquid ammonia at about 20 C under its own pressure (8.6 bar)
	public static final double NITRIC_CONVERSION = 0.94;           // of the ammonia burned on the gauze, to acid (BREF 93-98 %)
	public static final double NITRIC_STRENGTH = 0.60;             // the absorption tower's acid (BREF 50-68 %)
	public static final double NITRIC_KG_PER_L = 1.37;             // 60 % nitric acid at 20 C
	public static final double NITRIC_UNIT_WATTS = 15_000.0;       // its air blower (proposed), 480 V
	public static final double GAUZE_PT_G = 20.0;                  // platinum-rhodium in one gauze pack (proposed)
	public static final double GAUZE_LOSS_G_PER_T = 0.15;          // g of it lost a tonne of HNO3 (BREF 0.05-0.3)
	public static final double GAUZE_WORN_SHARE = 0.25;            // a pack is changed when it has lost this share (proposed)
	public static final double AN_STRENGTH = 0.85;                 // the neutraliser's AN solution (BREF)
	public static final double MATRIX_AN = 0.75;                   // the emulsion matrix, by weight (proposed; real 70-80 % AN)
	public static final double MATRIX_WATER = 0.18;
	public static final double MATRIX_DIESEL = 0.06;
	public static final double MATRIX_EMULSIFIER = 0.01;
	public static final double EMULSION_BLENDER_WATTS = 5_500.0;   // its high-shear mixer and pumps (proposed), 480 V
	public static final double EMULSIFIER_PAIL_KG = 20.0;          // one pail of emulsifier (proposed)
	public static final double DIESEL_KG_PER_L = 0.84;
	public static final double AMMONIA_PLANT_WATER_HOLD = 200.0;   // litres a nitric acid unit or blender holds off its pump
	public static final double BLAST_POWDER_FACTOR_KG_PER_T = 0.37; // Tailings' open pit: kg of explosive a tonne of rock
	public static final double CYLINDER_FILLER_WATTS = 5_000.0;    // 480 V (unverified)

	// ---- The centrifuge cascade (docs/multiblocks/enrichment.md, approved 2026-11-01) ------------------------------------------
	public static final double ASSAY_FEED = 0.00711;               // natural U-235
	public static final double ASSAY_PRODUCT = 0.0495;             // LEU for the 17x17 fuel
	public static final double ASSAY_TAILS = 0.0025;
	public static final double CENTRIFUGE_SEPARATION_FACTOR = 1.74;   // a 5 m TC-21 (Glaser)
	public static final double CENTRIFUGE_REAL_SWU_YEAR = 100.0;   // a TC-21 (informal, unverified)
	public static final double CENTRIFUGE_SWU_SCALE = 1000.0;      // decided with Tim: 1000x its real separative work
	public static final double KWH_PER_SWU = 50.0;                 // gas centrifuge plants: 50-60 kWh/SWU
	public static final double CENTRIFUGE_TIP_SPEED = 770.0;       // m/s, TC-21
	public static final double CENTRIFUGE_ROTOR_RADIUS = 0.10;     // m: a 20 cm rotor
	public static final int CENTRIFUGE_MOTOR_POLES = 2;
	/** The machines' own frequency, made by the converter from the grid: a property of the rotor, not a grid standard. */
	public static final double CENTRIFUGE_HERTZ = CENTRIFUGE_TIP_SPEED / (2 * Math.PI * CENTRIFUGE_ROTOR_RADIUS) * CENTRIFUGE_MOTOR_POLES / 2;
	public static final int CONVERTER_MACHINES = 12;               // machines a frequency converter drives (proposed)
	public static final double CONVERTER_POWER_FACTOR = 0.95;      // its front end, as the land rig's drives
	public static final double CYLINDER_30B_KG = 2_277.0;          // UF6 at up to 5 % (NRC package certificates)
	public static final double CYLINDER_30B_TARE = 635.0;          // about 1,400 lb
	public static final double UF6_SUBLIMATION_J_PER_KG = 137_000.0;   // 48 kJ/mol
	public static final double FEED_STATION_WATTS = 30_000.0;      // its heater, 480 V (unverified)
	public static final double PRODUCT_TAKEOFF_WATTS = 15_000.0;   // refrigeration, 480 V (unverified)
	public static final double TAILS_TAKEOFF_WATTS = 30_000.0;     // refrigeration, 480 V (unverified)

	// ---- The zirconium plant (docs/multiblocks/zirconium.md, approved 2026-11-01) ---------------------------------------------
	public static final double ZR_LINE_KGS = 10.0 / 3600;          // Zircaloy-4 a second: a small line (proposed)
	public static final double ZR_STAGE_HOLD_KG = 50.0;            // metal a stage holds for the next
	public static final double ZIRCON_RECOVERY = 0.90;             // of the zircon, through the separator (proposed)
	public static final double HM_ZIRCON = 0.25;                   // of the heavy minerals (docs/ores.md)
	public static final double HM_ILMENITE = 0.55;
	public static final double HM_RUTILE = 0.05;
	public static final double HM_MONAZITE = 0.03;
	public static final double ZR_IN_ZIRCON = 91.22 / 183.31;      // ZrSiO4
	public static final double ZR_HF_FRACTION = 0.02;              // of the metal in zircon (1-3 %, unverified)
	public static final double ZR_CARBON_EXCESS = 1.10;
	public static final double KROLL_MG_EXCESS = 1.25;             // Mg charged over the 2 Mg a Zr (proposed)
	public static final double ZIRCALOY_SN = 0.015;                // Zircaloy-4: 1.2-1.7 % Sn
	public static final double ZIRCALOY_FE = 0.0021;               // 0.18-0.24 % Fe
	public static final double ZIRCALOY_CR = 0.0010;               // 0.07-0.13 % Cr
	public static final double IRON_INGOT_KG = 100.0;              // an iron ingot, for the VAR furnace (proposed)
	public static final double COAL_KG = 150.0;                    // a piece of coal: a ninth of a 1,350 kg coal block (proposed)
	public static final double COAL_CARBON = 0.80;
	public static final double ZIRCALOY_DENSITY = 6.56;            // g/cm3
	public static final double CLAD_OD_MM = 9.50;                  // 17x17 fuel rod cladding
	public static final double CLAD_WALL_MM = 0.57;
	public static final double CLAD_LENGTH_M = 3.85;
	public static final double THIMBLE_OD_MM = 12.24;              // 17x17 guide thimble
	public static final double THIMBLE_WALL_MM = 0.41;
	public static final double THIMBLE_LENGTH_M = 4.0;
	public static final double MG_CELL_KA = 14.0;                  // a monopolar MgCl2 cell (unverified)
	public static final double MG_CELL_VOLTS = 5.0;
	public static final double MG_CURRENT_EFFICIENCY = 0.90;
	public static final double MG_KG_PER_KAH = 0.4534;             // Faraday: 24.3 g for two moles of electrons
	public static final double MG_CELL_HOLD_KG = 400.0;            // MgCl2 it holds; Mg the same in metal
	public static final double MGCL2_CHLORINATOR_KGH = 30.0;       // the magnesia chlorinator (proposed)
	public static final int CA_CELLS = 6;                          // a small bipolar membrane electrolyser
	public static final double CA_CELL_KA = 2.8;
	public static final double CA_CELL_VOLTS = 3.1;
	public static final double CA_CURRENT_EFFICIENCY = 0.96;
	public static final double CL2_KG_PER_KAH = 1.3227;            // Faraday: 70.9 g for two moles of electrons
	public static final double CA_SALT_PER_CL2 = 1.65;             // kg of NaCl a kg of Cl2
	public static final double CA_WATER_PER_CL2 = 3.0;             // litres a kg of Cl2, making 32 % caustic (unverified)
	public static final double NAOH_PER_CL2 = 1.128;
	public static final double CHLOR_ALKALI_HOLD_KG = 50.0;        // salt and water it holds, in kg and L
	public static final double MINERAL_SEPARATOR_WATTS = 30_000.0; // 480 V (unverified)
	public static final double CHLORINATOR_WATTS = 15_000.0;       // its fluidizing blower
	public static final double HAFNIUM_COLUMN_WATTS = 50_000.0;
	public static final double KROLL_WATTS = 100_000.0;
	public static final double VACUUM_DISTILLATION_WATTS = 100_000.0;
	public static final double VAR_WATTS = 20_000.0;               // 1 kWh/kg a melt, twice
	public static final double EXTRUSION_PRESS_WATTS = 250_000.0;  // its hydraulic pumps, 4.16 kV
	public static final double PILGER_WATTS = 75_000.0;
	public static final double ANNEAL_FURNACE_WATTS = 100_000.0;
	public static final double SPACER_GRID_KG = 0.9;               // a Zircaloy mid grid (proposed)

	// ---- The fuel plant (docs/multiblocks/fuel.md, approved 2026-11-01) --------------------------------------------------------
	public static final double FUEL_LINE_U_KGS = 50.0 / 3600;      // kg of U a second: a small line (proposed)
	public static final double FUEL_STAGE_HOLD_KG = 100.0;         // U a stage holds for the next
	public static final double PELLET_DIAMETER_MM = 8.19;          // 17x17
	public static final double PELLET_LENGTH_MM = 9.83;
	public static final double UO2_THEORETICAL_DENSITY = 10.96;    // g/cm3
	public static final double PELLET_DENSITY_FRACTION = 0.95;
	public static final double PELLET_DISH_CHAMFER = 0.015;        // of the cylinder's volume
	public static final double ROD_STACK_M = 3.66;                 // 144 in active length
	public static final int ASSEMBLY_RODS = 264;
	public static final int ASSEMBLY_THIMBLES = 25;                // 24 guide thimbles and 1 instrument tube
	public static final int ASSEMBLY_MID_GRIDS = 6;                // and 2 end grids
	public static final double ASSEMBLY_STEEL_KG = 20.0;           // nozzles, end grids, springs (unverified)
	public static final double ROD_STEEL_KG = 2.0 / 264;           // a rod's spring and plugs
	public static final double STEEL_INGOT_KG = 100.0;             // (proposed, as the iron ingot)
	public static final double U_IN_UO2 = 238.03 / 270.03;
	public static final double IDR_H2_EXCESS = 1.5;                // (proposed)
	public static final double IDR_WATER_EXCESS = 3.0;             // (proposed)
	public static final double SINTER_H2_M3H = 15.0;               // the pusher furnace's atmosphere (proposed)
	public static final double GRIND_SWARF = 0.005;                // (proposed)
	public static final double ROD_HELIUM_L = 0.4;                 // at STP: about 20 cm3 at 2 MPa (estimate)
	public static final double HELIUM_CYLINDER_M3 = 8.5;           // 300 cf at 2,400 psi
	public static final double GAS_HELIUM_FRACTION = 0.004;        // Hugoton-Panhandle gas holds 0.3-0.8 % (proposed)
	public static final double HELIUM_UNIT_M3S = 1.0;              // pipeline gas it can treat (proposed)
	public static final double AUTOCLAVE_WATTS = 30_000.0;         // all 480 V (unverified)
	public static final double IDR_KILN_WATTS = 150_000.0;
	public static final double GRANULATOR_WATTS = 30_000.0;
	public static final double PELLET_PRESS_WATTS = 30_000.0;
	public static final double SINTERING_FURNACE_WATTS = 250_000.0;
	public static final double PELLET_GRINDER_WATTS = 20_000.0;
	public static final double ROD_LOADER_WATTS = 15_000.0;
	public static final double ROD_WELDER_WATTS = 30_000.0;
	public static final double ASSEMBLY_FIXTURE_WATTS = 10_000.0;
	public static final double HELIUM_UNIT_WATTS = 75_000.0;

	// ---- The nuclear island (docs/multiblocks/reactor.md, approved 2026-11-01) -------------------------------------------------
	public static final int CORE_ASSEMBLIES = 9;                   // 3 x 3 (Tim, 2026-11-01)
	public static final double LINEAR_HEAT_KW_M = 17.8;            // the 4-loop 17x17 average
	public static final double BURNUP_SPEEDUP = 100.0;             // (Tim, 2026-11-01)
	public static final double ASSEMBLY_RHO_FRESH = 0.16;          // +16,000 pcm (proposed)
	public static final double ASSEMBLY_RHO_PER_GWD = 0.0038;      // per GWd/t (proposed)
	public static final double DISCHARGE_GWD_T = 50.0;
	public static final double BETA = 0.0065;                      // U-235
	public static final double GENERATION_TIME = 2.0e-5;           // s
	public static final double NEUTRON_SOURCE = 1.0e-10;           // of full power a second, through the generation time
	public static final double DOPPLER_PER_C = -2.5e-5;            // (textbook, unverified)
	public static final double FUEL_RISE_C = 600.0;                // fuel over coolant at full power (unverified)
	public static final double MTC_AT_0_PPM = -40e-5;              // per C (textbook, unverified)
	public static final double MTC_AT_1500_PPM = -5e-5;
	public static final double BORON_PER_PPM = -10e-5;             // (textbook, unverified)
	public static final int RCCAS = 5;
	public static final double ROD_WORTH_CENTRE = 0.016;           // (proposed)
	public static final double ROD_WORTH_EDGE = 0.011;
	public static final int ROD_STEPS = 228;
	public static final double ROD_STEPS_PER_S = 72.0 / 60;
	public static final double ROD_DROP_S = 2.0;
	public static final double SHUTDOWN_MARGIN = 0.013;            // NUREG-1431
	public static final double TAVG_NO_LOAD = 291.7;               // 557 F
	public static final double TAVG_FULL_LOAD = 308.9;             // 588 F
	public static final double RCS_MPA = 15.51;                    // 2,250 psia
	public static final double CORE_DT = 33.0;
	public static final double LOOP_FLOW_KGS = 431.0;
	public static final double NATURAL_CIRCULATION = 0.03;         // of full flow (proposed)
	public static final double RCS_WATER_KG = 30_000.0;            // (proposed)
	public static final double RCS_HEAT_J_PER_C = 300e6;           // coolant and metal (proposed)
	public static final double RCS_FILL_KGS = 10.0;                // the makeup pump filling (proposed)
	public static final double RCS_LOSS_W = 300_000.0;             // insulation loss when hot (proposed)
	public static final double HEATUP_SPEEDUP = 80.0;              // below no-load temperature (proposed)
	public static final double RCP_WATTS = 500_000.0;              // 4.16 kV (proposed)
	public static final double RCP_HEAT = 0.9;                     // of its power into the water
	public static final double RCP_SEAL_MPA = 2.3;
	public static final double RCP_COASTDOWN_S = 10.0;             // the flow halves in 10 s: 1 / (1 + t / T) (proposed)
	public static final double PZR_HEATER_WATTS = 150_000.0;       // 480 V (proposed)
	public static final double PZR_HEAT_J_PER_C = 20e6;            // (proposed)
	public static final double PZR_LOSS_W = 30_000.0;              // when hot (proposed)
	public static final double PZR_SURGE = 0.3;                    // C of the pressurizer a C of Tavg (proposed)
	public static final double PZR_HEATERS_ON_MPA = 15.41;
	public static final double PZR_SPRAY_MPA = 15.65;
	public static final double PZR_SPRAY_C_PER_S = 0.3;            // (proposed)
	public static final double PORV_MPA = 16.2;
	public static final double TRIP_HIGH_MPA = 16.54;              // 2,385 psig
	public static final double TRIP_LOW_MPA = 12.96;               // 1,865 psig
	public static final double TRIP_FLUX = 1.09;
	public static final double TRIP_FLUX_LOW = 0.25;
	public static final double TRIP_LOW_FLOW = 0.90;
	public static final double P7 = 0.10;                          // P-7 and P-10
	public static final double P8 = 0.35;                          // (for this plant)
	public static final double TRIP_THOT_C = 343.0;                // (simplified from OT delta-T)
	public static final double SG_UA_W_PER_C = 3.2e6;              // each, at full flow
	public static final double SG_WATER_KG = 20_000.0;             // each (proposed)
	public static final double SG_HEAT_J_PER_C = 120e6;            // (proposed)
	public static final double SG_LOW_LOW = 0.40;
	public static final double SG_ADV_MPA = 7.6;
	public static final double SG_ADV_KGS = 4.3;                   // each, 10 % of full steam
	public static final double FEEDWATER_C = 227.0;
	public static final double SG_STEAM_HOLD_S = 2.0;              // seconds of full steam a steam generator offers its pipe
	public static final double CVCS_KGS = 0.5;                     // letdown and charging (proposed)
	public static final double BORIC_ACID_TANK_PPM = 7000.0;
	public static final double BORIC_ACID_BAG_KG = 25.0;
	public static final double B_IN_BORIC_ACID = 10.81 / 61.83;
	public static final double CVCS_BORIC_ACID_KG = 500.0;         // its tank (proposed)
	public static final double CVCS_WATTS = 15_000.0;
	public static final double CRDM_WATTS = 25_000.0;
	public static final double AFW_KGS = 5.0;                      // (proposed)
	public static final double AFW_WATTS = 56_000.0;               // 75 hp
	public static final double LOADING_MAX_C = 60.0;
	public static final double SUR_WITHDRAW_DPM = 1.0;
	public static final double SUR_INSERT_DPM = 1.5;

	// ---- The turbine island (docs/multiblocks/turbine-island.md, approved 2026-11-01) -------------------------------------------
	public static final double STEAM_KG_PER_KWH_PWR = 6.1;         // saturated steam, HP, MSR and LP (textbook, unverified)
	public static final double TG60_WATTS = 51_000_000.0;          // 60 MVA at 0.85 PF
	public static final double CW_INLET_C = 20.0;
	public static final double CW_RISE_C = 10.0;                   // at full flow
	public static final double CONDENSER_TTD_C = 5.0;              // across the tubes (unverified)
	public static final double VACUUM_LOSS_PER_KPA = 0.01;         // of output, over the design vacuum (unverified)
	public static final double LOW_VACUUM_TRIP_KPA = 25.0;
	public static final double CW_PUMP_KGS = 1250.0;               // each (proposed)
	public static final double CW_PUMP_WATTS = 300_000.0;          // 4.16 kV (proposed)
	public static final int CW_INTAKE_WATER_BLOCKS = 12;
	public static final double CONDENSATE_PUMP_KGS = 43.0;         // each, 50 % (proposed)
	public static final double CONDENSATE_PUMP_WATTS = 150_000.0;  // 480 V
	public static final double MAIN_FEED_PUMP_KGS = 43.0;          // each, 50 %
	public static final double MAIN_FEED_PUMP_WATTS = 500_000.0;   // 4.16 kV
	public static final double DEAERATOR_KG = 30_000.0;
	public static final double HOTWELL_KG_PWR = 10_000.0;
	public static final double DEAERATOR_LOW = 0.10;               // a turbine trip under it
	public static final double TURBINE_ROLL_SECONDS = 120.0;
	public static final double STEAM_HEADER_MPA = 7.0;
	public static final double STEAM_DUMP = 0.40;                  // of full steam, to the condenser (proposed)
	public static final double P9 = 0.50;
	public static final double UAT_VA = 7_500_000.0;               // 13.8/4.16 kV (proposed)
	public static final double UAT_PERCENT_Z = 7.0;

	// ---- After the trip: decay heat, xenon, RHR, core damage (docs/multiblocks/reactor.md, 8d, approved 2026-11-01) --------------
	public static final double[] DECAY_LAMBDA = {3.0, 0.3, 0.03, 3e-3, 3e-4, 3e-5, 3e-6, 3e-7};   // /s, a fit to Way-Wigner
	public static final double[] DECAY_SHARE = {0.068467, 0.023723, 0.015488, 0.009964, 0.005755, 0.004623, 0.000871, 0.005296};
	public static final double IODINE_LAMBDA = Math.log(2) / (6.57 * 3600);
	public static final double XENON_LAMBDA = Math.log(2) / (9.14 * 3600);
	public static final double XENON_BURNOUT = 6.0e-5;             // /s at full power: sigma-phi at about 2.3e13 n/cm2s (proposed)
	public static final double XENON_DIRECT = 0.24 / (6.39 + 0.24);  // of xenon made directly, not by iodine
	public static final double XENON_WORTH = 0.028;                // at full-power equilibrium (textbook)
	public static final double COOLDOWN_C_PER_H = 55.0;            // the operators' cooldown on the dump valves, compressed as the heat-up
	public static final double COOLDOWN_FLOOR_C = 120.0;
	public static final double PZR_SUBCOOLING_C = 30.0;           // the pressure held over saturation while cooling down
	public static final double RHR_MAX_C = 177.0;                  // 350 F
	public static final double RHR_MAX_MPA = 2.9;                  // 425 psig
	public static final double RHR_HOLD_C = 50.0;                  // cold shutdown
	public static final double RHR_WATTS = 200_000.0;              // 4.16 kV (proposed)
	public static final double RHR_UA_W_PER_C = 60_000.0;          // (proposed)
	public static final double RHR_CW_KGS = 200.0;                 // circulating water through its exchanger (proposed)
	public static final double RCS_LATENT_J_PER_KG = 1.0e6;        // near 15.5 MPa
	public static final double CORE_UNCOVERED = 0.60;              // of the RCS inventory
	public static final double FUEL_HEAT_J_PER_C = 1.8e6;          // 4.7 t of UO2 and 1.05 t of Zircaloy
	public static final double FUEL_STEAM_COOLING_W_PER_C = 1_000.0;     // what the steam round uncovered fuel takes (proposed)
	public static final double CLAD_FAIL_C = 1200.0;
	public static final double CORE_ZR_KG = 1050.0;
	public static final double ZR_OXIDATION_J_PER_KG = 6.42e6;     // 586 kJ/mol
	public static final double H2_PER_ZR = 0.044;
	public static final double H2_IGNITE_KG = 18.0;                // 4 % of 5,000 m3 of the island's building air (proposed)
	public static final float H2_BLAST_POWER = 6.0F;               // (proposed)

	// ---- Fuel handling, the spent fuel pool and radiation (docs/multiblocks/fuel-handling.md, approved 2026-11-02) ----------
	public static final double FUEL_MOVE_SECONDS = 60.0;           // compressed: real moves take 1-2 h
	public static final int RELOAD_ASSEMBLIES = 3;
	public static final int POOL_RACKS = 36;
	public static final int POOL_SIDE = 6;
	public static final int POOL_DEPTH = 12;
	public static final double POOL_WATER_KG = POOL_SIDE * POOL_SIDE * POOL_DEPTH * 1000.0;
	public static final double POOL_UA_W_PER_C = 80_000.0;         // (proposed)
	public static final double POOL_CW_KGS = 100.0;
	public static final double POOL_LATENT_J_PER_KG = 2.26e6;
	public static final double POOL_FUEL_TOP_M = 4.1;              // the racks' top over the floor
	public static final double REFUELLING_WATTS = 30_000.0;
	public static final double POOL_PUMP_WATTS = 75_000.0;
	public static final double SV_H_PER_MW_AT_1M = 1000.0;         // an irradiated assembly, by its decay heat (proposed)
	public static final double VESSEL_SV_H_AT_1M = 0.010;          // at full power, inside its primary shield (Tim, 2026-11-02)
	public static final double DAMAGED_CORE_SV_H_AT_1M = 1.0;
	public static final double ORE_USV_H_PER_CPS = 0.002;          // (proposed)
	public static final double WATER_BLOCK_SHIELD = 1.0 / 300;     // a block: about 8 half-value layers of ~1 MeV gamma
	public static final double SOLID_BLOCK_SHIELD = 1.0 / 1000;
	public static final double DOSE_RANGE = 64.0;
	public static final double ACUTE_HALF_LIFE_S = 24 * 3600.0;    // repair (proposed)
	public static final double SV_NAUSEA = 1.0, SV_SICK = 3.0, SV_WITHER = 6.0, SV_LETHAL = 8.0;
	public static final double ALARM_RATE_SV_H = 1e-3;
	public static final double ALARM_DOSE_SV = 0.020;              // the occupational year (ICRP)

	// ---- The natural-draft cooling tower (docs/multiblocks/natural-draft-tower.md, approved 2026-11-01) --------------------------
	public static final double TOWER_SHELL_BASE_Y = 8.0;           // over the basin floor; the raker columns below
	public static final double TOWER_THROAT_Y = 62.0;
	public static final double TOWER_THROAT_R = 17.0;
	public static final double TOWER_SHAPE_B = 37.1;               // the hyperbola's: 30 m at the base, 20.8 m at the top
	public static final double TOWER_TOP_Y = 88.0;                 // Calder Hall's 88 m (textbook)
	public static final double TOWER_LIFT_M = 1.5;
	public static final double TOWER_SHELL_BASE_T = 0.40;          // m of concrete, tapering to the top's (unverified)
	public static final double TOWER_SHELL_TOP_T = 0.18;
	public static final double TOWER_BASIN_R = 31.0;
	public static final double TOWER_BASIN_DEPTH = 3.0;
	public static final double TOWER_CENTRE = 34.0;                // from the controller's main cell, in front of it
	public static final double TOWER_CONCRETE_SHARE = 0.10;        // a block for each 10 real m3 (Tim, 2026-11-01)
	public static final double TOWER_REBAR_KG_PER_M3 = 1.0;        // a hundredth of 100 kg/m3 (Tim's 44 ingots)
	public static final int TOWER_BASIN_CONCRETE = 100;
	public static final int TOWER_BASIN_STEEL = 10;
	public static final int TOWER_COLUMNS_CONCRETE = 15;
	public static final int TOWER_COLUMNS_STEEL = 2;
	public static final int TOWER_FILL_STEEL = 4;
	public static final int TOWER_FILL_PLASTIC = 64;
	public static final double TOWER_BASIN_SECONDS = 60.0;         // compressed: real towers take months
	public static final double TOWER_COLUMNS_SECONDS = 30.0;
	public static final double TOWER_LIFT_SECONDS = 15.0;
	public static final double TOWER_FILL_SECONDS = 30.0;
	public static final double TOWER_DESIGN_W = 104e6;
	public static final double TOWER_APPROACH_C = 8.0;             // at design (unverified)
	public static final double TOWER_LAG_S = 120.0;
	public static final double LATENT_J_PER_KG = 2.45e6;
	public static final double TOWER_CYCLES = 5.0;                 // of concentration: blowdown a quarter of evaporation
	public static final double LAKE_C = 20.0;
	public static final double TOWER_MIN_COLD_C = 4.0;            // no colder: the basin would freeze (proposed)

	// ---- The boron plant (docs/multiblocks/boron.md, approved 2026-11-01) ------------------------------------------------------
	public static final double BORIC_ACID_KGS = 50.0 / 3600;       // (proposed)
	public static final double B4C_KGS = 5.0 / 3600;               // (proposed)
	public static final double BORAX_KG = 1680.0;                  // in a borax ore item (ores.md)
	public static final double DISSOLVER_HOLD_KG = 100.0;          // of boric acid in its liquor
	public static final double DISSOLVER_WATER_PER_KG = 0.3;       // litres a kg of boric acid (proposed)
	public static final double B4C_ROD_KG = 9.0;                   // an RCCA: 24 rodlets of 0.37 kg
	public static final double RCCA_STEEL_KG = 20.0;               // (unverified)
	public static final double RCCA_SECONDS = 3600.0;              // (proposed)
	public static final double DISSOLVER_WATTS = 30_000.0;
	public static final double CRYSTALLIZER_WATTS = 40_000.0;
	public static final double B4C_FURNACE_WATTS = 150_000.0;
	public static final double RCCA_BENCH_WATTS = 30_000.0;

	// ---- The gas turbine and combined cycle (docs/multiblocks/gas-turbine.md, combined-cycle.md, approved 2026-10-31) -------
	public static final double GT_WATTS = 16_500_000.0;            // ISO rating, Solar Titan 130 class
	public static final double GT_HEAT_RATE = 10_230.0;            // kJ/kWh: 35 %
	public static final double GT_NO_LOAD_FUEL = 0.30;             // of full-load fuel at no load
	public static final double GT_STARTER_WATTS = 224_000.0;       // 300 hp, 4.16 kV
	public static final double GT_PURGE_SECONDS = 60.0;            // at a quarter speed, before light-off
	public static final double GT_RUNUP_SECONDS = 120.0;           // light-off to full speed (a real start is about ten minutes)
	public static final double GT_STARTER_DROPOUT = 0.6;           // of full speed: the turbine runs on alone above it
	public static final int GT_GENERATOR_POLES = 4;                // 1,800 rpm at 60 Hz, 1,500 at 50 Hz
	public static final double LIQUID_FUEL_JOULES_PER_L = 36.0e6;  // diesel and kerosene
	public static final double HRSG_RECOVERY = 0.65;               // of the exhaust's heat above stack temperature, to steam
	public static final double STEAM_CYCLE_EFFICIENCY = 0.27;      // steam turbine power out of HRSG heat: 5.2 MW at full load
	public static final double HRSG_WARMUP_SECONDS = 120.0;        // its steam follows the exhaust with this time constant
	public static final double ST_WATTS = 5_000_000.0;             // 6.25 MVA at 0.8 PF
	public static final double ACC_FAN_WATTS = 149_000.0;          // four 50 hp fans, 480 V
	public static final double HRSG_FEED_PUMP_WATTS = 45_000.0;    // 60 hp, 480 V: 6 L/s at 5 MPa
	public static final double GSU25_VA = 25_000_000.0;            // 13.8 kV delta / 138 kV grounded wye
	public static final double GSU25_PERCENT_Z = 8.0;
	public static final double LOAD_BANK_STEP_WATTS = 1_000_000.0; // 1 MW steps, 0 to 10 MW
	public static final int LOAD_BANK_STEPS = 10;
	public static final double GT_RAMP_PER_MINUTE = 0.10;          // of its rating a minute after it closes its breaker

	// ---- The 45 MW aeroderivative peaker (docs/multiblocks/aeroderivative.md, approved 2026-11-01) --------------------------
	public static final double AERO_WATTS = 45_000_000.0;          // LM6000 class
	public static final double AERO_HEAT_RATE = 8_800.0;           // kJ/kWh: 41 %
	public static final double AERO_NO_LOAD_FUEL = 0.25;
	public static final double AERO_PURGE_SECONDS = 30.0;
	public static final double AERO_RUNUP_SECONDS = 60.0;
	public static final double AERO_RAMP_PER_MINUTE = 0.25;        // full load about five minutes after the button
	public static final double GSU60_VA = 60_000_000.0;            // 13.8 kV delta / 138 kV grounded wye
	public static final double GSU60_PERCENT_Z = 10.0;
	public static final double SUBSTATION60_VA = 60_000_000.0;     // 138 kV delta / 12.47 kV grounded wye
	public static final double SUBSTATION60_PERCENT_Z = 10.0;

	// ---- Refining and engines ----------------------------------------------------------------------------------------
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

	// ---- Steam turbine -----------------------------------------------------------------------------------------
	/** Energy in one unit of steam. A campfire boiler (2 steam a tick) therefore makes 2 kW. */
	public static final double JOULES_PER_STEAM = 50.0;
	public static final double TURBINE_INTERNAL_OHMS = 0.5;  // its output sags as the current rises
	public static final double TURBINE_STEAM_SMOOTHING = 0.3; // how fast the output follows the steam supply

	// ---- Wire heating (the sizes, their resistance and ampacity are in energy/ConductorSize) ---------------------
	/**
	 * A conductor's temperature rise follows its current with this time constant (energy/WireHeat): copper and its insulation take
	 * minutes to heat, so a motor's two-second start passes and a sustained overload does not. Approved by Tim, 2026-10-31.
	 */
	public static final double WIRE_HEAT_SECONDS = 120.0;
	/** It burns out when its temperature rise reaches this many times its rated rise (75 C wire in 30 C air: about 110 C). */
	public static final double WIRE_BURN_RISE = 1.8;

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
	// Soft starters and drives (docs/multiblocks/soft-starter.md, vfd.md; proposed, approved with the 10a plan 2026-11-02)
	public static final double[] SOFT_START_LIMITS = {2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0};   // current limit, x full-load amps
	public static final int SOFT_START_LIMIT_DEFAULT = 3;          // 3.5 x
	public static final double SOFT_START_BYPASS = 1.25;           // the bypass closes when the current falls to this x full-load amps
	public static final double SOFT_START_NO_MOTOR_SECONDS = 2.0;  // no inrush seen this long: nothing to ramp, it bypasses
	/** The 480 V cabinets' dial: the starter's, then NEC 430.250 full-load current at 460 V for 75, 100, 125, 150, 200 and 250 hp. */
	public static final double[] CABINET_SETTINGS_LV = {1.5, 3, 5, 8, 11, 16, 22, 32, 45, 63, 96, 124, 156, 180, 240, 302};
	/** The 480 V drive bucket's dial: up to 100 hp. */
	public static final double[] DRIVE_BUCKET_SETTINGS = {1.5, 3, 5, 8, 11, 16, 22, 32, 45, 63, 96, 124};
	/** The 4.16 kV cabinets' dial (IEC R10 steps): the 250 hp charge pump is 32 A, a 500 kW coolant pump 87 A. */
	public static final double[] CABINET_SETTINGS_MV = {10, 16, 25, 32, 40, 50, 63, 80, 100, 125};
	public static final double DRIVE_LINE_POWER_FACTOR = 0.95;     // a 6-pulse diode front end
	public static final double DRIVE_EFFICIENCY = 0.97;
	public static final double DRIVE_RAMP_SECONDS = 10.0;          // 0 to full frequency and voltage, volts per hertz held
	public static final double DRIVE_CURRENT_LIMIT = 1.5;          // x full-load amps, heavy duty...
	public static final double DRIVE_LIMIT_SECONDS = 60.0;         // ...for this long, then it trips on overcurrent
	// ---- PLCs: analog, valves, air (docs/multiblocks/plc.md, all proposed) ----
	public static final double DRIVE_MIN_SPEED = 0.2;              // a drive under a speed reference runs no slower than this share of full speed
	public static final double PUMP_STATIC_HEAD_SPEED = 0.3;       // a centrifugal pump against head delivers nothing below this speed
	public static final double VALVE_ACTUATOR_WATTS = 370.0;       // a small multi-turn actuator
	public static final double CONTROL_VALVE_STROKE_SECONDS = 5.0;
	public static final double ON_OFF_VALVE_STROKE_SECONDS = 3.0;
	public static final double MOV_STROKE_SECONDS = 30.0;
	public static final double AIR_COMPRESSOR_WATTS = 7_500.0;
	public static final double AIR_CUT_IN_BAR = 6.0;
	public static final double AIR_CUT_OUT_BAR = 8.0;
	public static final double AIR_MIN_BAR = 3.0;                  // under this an actuator's spring wins
	public static final double AIR_CHARGE_BAR_PER_SECOND = 0.5;
	public static final double AIR_LEAK_BAR_PER_SECOND = 0.01;
	public static final double DAY_TANK_LITRES = 1000.0;
	public static final double DAY_TANK_OUTLET_LITRES_PER_SECOND = 100.0;   // full open, full tank
	public static final double DRIVE_AT_SPEED = 0.98;              // a drive-fed motor runs once the drive is at this share of full voltage
	public static final double DRIVE_OUTPUT_OHMS = 0.001;          // the inverter's output impedance: stiff
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
	// the generator bus's relay panel (docs/multiblocks/gen-bus-protection.md; proposed settings)
	public static final double BUS_87_MIN_PICKUP = 0.2;            // 87B floor: a share of the largest breaker's rating
	public static final double BUS_87_SLOPE = 0.3;                 // 87B restraint: a share of the current through the bus's boundary
	public static final double GEN_51V_PICKUP = 1.5;               // 51V: 150 % of full-load amps at full voltage (IEEE C37.102)
	public static final double GEN_51V_FLOOR = 0.25;               // restrained down to 25 % of that at 25 % voltage and below
	public static final double GEN_51V_TIME_DIAL = 1.0;            // IEEE C37.112 very inverse
	public static final double FEEDER_51_PICKUP = 1.0;             // feeder 51: at the breaker's rating, moderately inverse, time dial 1
	public static final double FEEDER_50_MULTIPLE = 8.0;           // feeder 50: instantaneous at 8 x the breaker's rating
	public static final double GROUNDING_SET_OHMS = 0.001;         // a grounding cluster: each phase to earth through 2/0 copper (ASTM F855)
	// 10d2: the 4.16 kV switchgear and the breaker relay cabinet (docs/multiblocks/mv-switchgear.md, breaker-relay-cabinet.md)
	public static final int[] SWITCHGEAR_FRAME_AMPS = {1200, 2000, 3000};   // 5 kV metal-clad vacuum breakers (VCP-W 5 class)
	public static final int[] RELAY_PICKUP_STEPS = {5, 10, 25, 50, 100, 200, 400, 600, 1200, 2000, 3000};   // 51 pickup in amps (proposed: 50:5 CT, 0.5 A tap at the bottom; 2,000 and 3,000 A for the 15 kV feeders, gap wave)
	public static final int RELAY_PICKUP_START = 4;                // a new relay starts at 100 A
	public static final double RELAY_50_OF_PICKUP = 8.0;           // 50 at 8 x the 51 pickup
	// the protection gap wave (docs/questions/protection-gaps.md): settable curve, dial and 50 on every feeder relay
	public static final double[] RELAY_DIAL_STEPS = {0.5, 1, 2, 3, 4, 5, 7, 10, 15};   // IEEE C37.112 time dial, 0.5 to 15
	public static final int RELAY_DIAL_START = 1;                  // time dial 1, as before
	public static final double[] RELAY_50_STEPS = {0, 4, 6, 8, 10, 12, 16, 20};       // 50 as a multiple of the pickup; 0 = off
	public static final int RELAY_50_START = 3;                    // 8 x, as before
	// the 10 MVA 4.16/138 kV power transformer (docs/multiblocks/power-transformer-10.md)
	public static final double STEP_UP_10_VA = 10_000_000.0;       // 4.16 kV delta / 138 kV grounded wye, ONAN
	public static final double STEP_UP_10_PERCENT_Z = 7.5;
	// ---- 10d3: panels and switches (docs/multiblocks/panelboards.md, safety-switch.md, load-interrupter.md, ats.md) -------------
	public static final int LOAD_CENTRE_MAIN_AMPS = 200;            // a 200 A, 40-space main-breaker load centre
	public static final int LOAD_CENTRE_SPACES = 40;
	public static final int PANELBOARD_MAIN_AMPS = 225;             // a 225 A, 42-circuit 480Y/277 V lighting panelboard
	public static final int PANELBOARD_SPACES = 42;
	public static final double CLASS_J_SECONDS_AT_2X = 80.0;        // class J time-delay: carries 500 % for 10 s (UL 248-8)
	public static final double CLASS_J_INSTANT_MULTIPLE = 15.0;     // current-limiting above this (proposed)
	public static final double E_FUSE_SECONDS_AT_2X = 384.0;        // E-rated: melts in 300 s at 220 % (ANSI C37.46, up to 100E)
	public static final double E_FUSE_SECONDS_AT_2X_LARGE = 768.0;  // 600 s above 100E
	public static final double E_FUSE_INSTANT_MULTIPLE = 20.0;      // current-limiting above this (proposed)
	public static final int LIS_LOAD_BREAK_AMPS = 600;             // a 5 kV metal-enclosed load-interrupter (IEEE C37.20.3)
	public static final int ATS_AMPS = 400;                         // a 400 A open-transition ATS (UL 1008)
	public static final double ATS_DROPOUT = 0.85;                  // normal fails below this share of its nominal on any phase
	public static final double ATS_PICKUP = 0.90;                   // normal restored, and emergency acceptable, at this
	public static final double ATS_EMERGENCY_HERTZ = 0.95;          // emergency acceptable at this share of nominal frequency
	public static final double ATS_TDES_SECONDS = 3.0;              // engine-start delay (3 s)
	public static final double ATS_TDNE_SECONDS = 1.0;              // transfer to emergency (0 s; proposed 1 s)
	public static final double ATS_NEUTRAL_SECONDS = 1.0;           // the neutral position, open transition (proposed)
	public static final double ATS_TDEN_SECONDS = 30.0;             // retransfer to normal (30 min, scaled for the game)
	public static final double ATS_TDEC_SECONDS = 30.0;             // engine cooldown (5 min, scaled for the game)
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
	public static final int[] BREAKER_AMPS_GENERATOR = {1200, 2000, 3000};   // 15 kV metal-clad feeder breakers (VCP-W 15 class)
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
			case MEDIUM, DISTRIBUTION, GENERATOR -> SPAN_MAX_DISTRIBUTION;
			case HIGH -> SPAN_MAX_HIGH;
		};
	}
	// ---- protection gap wave, group A: relays, switchgear, breaker failure, reclosing ----
	public static final double RACK_SECONDS = 15.0;                // a draw-out truck cranked one position (about 20 turns of the racking handle)
	public static final double BF_SECONDS = 0.15;                  // 62BF: the breaker failure timer, about 9 cycles
	public static final double BF_DETECTOR_AMPS = 2.0;             // 50BF's current detector: still carrying after the timer means it failed
	public static final double SWITCHGEAR_INTERRUPTING_AMPS = 41_000;   // 5 kV, 350 MVA class vacuum breaker (IEEE C37.06): 41 kA
	public static final double GEN_FEEDER_INTERRUPTING_AMPS = 50_000;   // 15 kV generator-bus feeder breakers, 50 kA class
	public static final double SWITCHGEAR_87_OF_MAIN_PICKUP = 0.5;   // the lineup's 87B minimum pickup: half the main's 51 pickup
	public static final double BREAKER_INTERRUPT_SECONDS = 0.05;   // a vacuum breaker's interrupting time: 3 cycles
	public static final double[] RECLOSE_INTERVALS = {2, 15, 30};  // 79: the open intervals before shots 1, 2 and 3, in seconds
	public static final double RECLOSE_RESET_SECONDS = 60;         // 79: held closed this long after a shot, the sequence resets
	public static final int RECLOSE_SHOTS_DISTRIBUTION = 3;        // a 12.47 kV feeder: three shots, four trips to lockout
	public static final int RECLOSE_SHOTS_HIGH = 1;                // a 138 kV line: one shot

	// ---- protection gap wave, group B: differentials, back-feed, grounding clamps ----
	public static final double XFMR_87_MIN_VA = 10_000_000;       // 87T on a transformer of 10 MVA and up (IEEE C37.91 practice)
	public static final double XFMR_87_MIN_PU = 0.3;               // 87T minimum pickup, per unit of rated current (SEL-787 class O87P)
	public static final double XFMR_87_SLOPE = 0.25;               // 87T restraint slope (SLP1 25 %)
	public static final double GEN_87_MIN_PU = 0.1;                // 87G minimum pickup, per unit of rated current (SEL-700G class)
	public static final double GEN_87_SLOPE = 0.1;                 // 87G restraint slope (10 %): a generator has no magnetizing inrush or taps
	public static final double CLAMP_ULTIMATE_I2T = 35_000.0 * 35_000.0 * 0.25;   // ASTM F855 grade 3 (2/0 Cu): 35 kA for 15 cycles, ultimate
	public static final double CLAMP_CONTINUOUS_AMPS = 300;        // 2/0 copper in free air: what it carries without heating toward failure
	public static final double CLAMP_COOL_SECONDS = 300;           // a fully heated cluster cools back over 5 minutes
	public static final double FAULT_CLEARING_CYCLES = 5;          // relay (1-2 cycles) plus a vacuum breaker's 3-cycle interrupting time

	// ---- protection gap wave, group C: panels, GFCI, ATS, fuses, coordination ----
	public static final double BODY_OHMS = 1000.0;                 // IEC 60479-1: hand to feet at 120-230 V, about 1 kohm (the 50 % value, dry, large contact)
	public static final int BODY_CONTACT_TICKS = 20;               // a body stays across the conductor this long after a shock
	// the ATS's factory timers (UL 1008 controllers' defaults); the SHORT set is the scaled 10d3 one (ATS_*_SECONDS above)
	public static final double ATS_FACTORY_TDES = 3.0;             // engine start delay
	public static final double ATS_FACTORY_TDNE = 0.0;             // transfer to emergency
	public static final double ATS_FACTORY_TDEN = 1800.0;          // retransfer to normal: 30 min
	public static final double ATS_FACTORY_TDEC = 300.0;           // engine cooldown: 5 min
	public static final double ATS_IN_PHASE_DEGREES = 15.0;        // in-phase monitor: transfer between live sources within 15 degrees
	public static final double ATS_IN_PHASE_MAX_HZ = 2.0;          // ... and under 2 Hz apart
	public static final double ATS_FAST_NEUTRAL_SECONDS = 0.1;     // an in-phase transfer's own contact travel (about 6 cycles), no programmed delay
	public static final double ATS_CLOSED_DEGREES = 5.0;           // closed transition sync check: within 5 degrees,
	public static final double ATS_CLOSED_HZ = 0.2;                // 0.2 Hz
	public static final double ATS_CLOSED_VOLTS = 0.05;            // and 5 % voltage
	public static final double ATS_OVERLAP_SECONDS = 0.1;          // closed transition: both closed at most 100 ms
	public static final double ATS_SYNC_WAIT_SECONDS = 30.0;       // closed transition that finds no sync in this long falls back to open
	// the protection race and the coordination study (circuit/TimeCurrent)
	public static final double MAGNETIC_CLEAR_CYCLES = 1.0;        // a molded-case breaker's magnetic trip, or a fuse's instant melt: about a cycle
	public static final double CL_CLEAR_CYCLES = 0.5;              // a current-limiting fuse above its threshold: inside the first half cycle
	public static final double COORDINATION_MARGIN_CYCLES = 0.25;  // a device a quarter cycle slower than the one that cleared the fault holds
	public static final double GFCI_CLEAR_CYCLES = 1.5;            // UL 943: a GFCI on a heavy ground fault opens in about 25 ms
	public static final double INTERRUPT_LOAD_CENTRE = 10_000;     // a load centre and its plug-on breakers: 10 kA (panelboards.md)
	public static final double INTERRUPT_PANELBOARD = 14_000;      // a 480Y/277 V panelboard and its bolt-on breakers: 14 kA
	public static final double INTERRUPT_MCCB = 10_000;            // the one-block breakers: 10 kA, the UL 489 floor (proposed)
	public static final double INTERRUPT_CLASS_J = 200_000;        // class J fuses: 200 kA (UL 248-8)
	public static final double INTERRUPT_E_RATED = 50_000;         // 5 kV E-rated current-limiting fuses: 50 kA symmetrical (proposed, ANSI C37.46 class)
	public static final double INTERRUPT_FUSE = 10_000;            // a plug fuse or an expulsion K link: 10 kA (proposed)

	// ---- protection gap wave, group D: distribution, spans, poles ----
	/**
	 * An overhead span heats on its conductor's rating (the ACSR table's 75 C in 25 C air, a 50 K rise) with a bare conductor's longer
	 * time constant, about 10 minutes for 4/0 ACSR in still air (IEEE 738); it parts when its rise reaches 3 x the rated rise, about
	 * 175 C, well past ACSR's 100-150 C emergency limit, where the aluminium strands anneal and the span drops.
	 */
	public static final double SPAN_HEAT_SECONDS = 600.0;
	public static final double SPAN_BURN_RISE = 3.0;
	/**
	 * The power-follow arc over a flashed-over insulator shatters its porcelain once it has passed this many amp-seconds (a few kA for
	 * a few cycles; proposed). A fault the relays clear in the same solve still arced for about 5 cycles (a feeder relay's 50 and a
	 * vacuum breaker: 1 to 1.5 cycles of relay and 3 of breaker).
	 */
	public static final double INSULATOR_SHATTER_AMP_SECONDS = 50.0;
	public static final double ARC_CLEARING_CYCLES = 5.0;
	/** A grounding electrode at a service: a single driven rod is taken at NEC 250.53(A)(2)'s 25 ohm limit. */
	public static final double GROUND_ELECTRODE_OHMS = 25.0;
	/** A lead (jumper) from a line insulator down to a cutout or a pole transformer's H1 bushing is a few metres at most. */
	public static final int LEAD_MAX_BLOCKS = 6;
}
