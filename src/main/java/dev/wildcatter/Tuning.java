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
	public static final int[] BREAKER_AMPS_GENERATOR = {600, 1200};          // 13.8 kV: generator circuit breakers
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
}
