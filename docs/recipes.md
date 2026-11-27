# Recipes

Wildcatter's crafting recipes are written by `tools/make_assets.py` (and `tools/oilfield_assets.py`, which it runs) into
`common/src/main/resources/data/wildcatter/recipe/`; recipes with a sized conductor are copied into each loader's resources. A recipe
changed by hand is changed in both places, the script's `shaped(...)` or `shapeless(...)` line and its JSON, so the next full run of the
script writes the same file.

## One grid, one result
The crafting table makes only one recipe for a grid. Two recipes that take the same grid (with the same items, or a tag and an item in
it, or one the mirror image of the other left to right, or a shapeless recipe whose ingredients a shaped grid holds) leave one of the two
items impossible to craft. Every grid must give one thing, the game's own recipes included.

- **Check:** the NeoForge scene `recipes_unique` lays every grid each Wildcatter crafting recipe takes and asks every crafting recipe
  loaded whether it matches; it fails on any two that give different things, and logs each pair with the grid
  (`WILDCATTER_ONLY=recipes_unique ./gradlew :neoforge:runGameTestServer`).
- **A new recipe:** real parts for what it makes; costs in line with its neighbours; materials no later than the chapter whose quests
  ask for it. When it collides, change the less-used item (keep a quest item; keep the one more of the mod uses), and change one
  ingredient or the place of a pipe rather than the whole grid.

## Grids changed for uniqueness (Built From Scratch ep 3)
The stock tank is eight steel round a ladder (it shared eight steel round an empty middle with the dry storage canister, the fuel tank
and the 48Y cylinder). The others below, each with the grid it shared and why, are in `docs/questions/bfs-fixes2.md`. Grids read top row
to bottom; `·` is an empty slot.

| Item | Grid |
|---|---|
| LiOH Crystallizer (`lioh_crystallizer`) | `SPS` / `SCS` / `SHS` (S steel, P steel pipe, C copper block, H hopper) |
| Aux Feed Pump (75 hp) (`aux_feed_pump`) | `SPS` / `SXS` / `SCS` (S steel, P steel pipe, X piston, C copper) |
| LFP Kiln (700 C) (`lfp_kiln`) | `SHS` / `BFB` / `SBS` (S steel, H hopper, B bricks, F furnace) |
| Magnesia Chlorinator (`magnesia_chlorinator`) | `SHS` / `BFB` / `PSP` (S steel, H hopper, B bricks, F furnace, P steel pipe) |
| Turbine Generator (4.16 kV, 1,250 kVA) (`turbine_generator`) | `SWS` / `WRW` / `PXP` (S steel, W stator winding, R rotor field, X exciter, P steel pipe) |
| Mud-Gas Separator (`mud_gas_separator`) | `SPS` / `SIS` / `SPS` (S steel, P steel pipe, I iron bars) |
| Side Strippers (kerosene and diesel) (`side_strippers`) | `PSP` / `S·S` / `PSP` (S steel, P steel pipe) |
| Ammonium Nitrate Neutraliser (`an_neutraliser`) | `SPS` / `SKS` / `SPS` (S steel, P steel pipe, K cauldron) |
| Potash Dissolver (`potash_dissolver`) | `SHS` / `SXS` / `SCS` (S steel, H hopper, X piston, C copper) |
| Busway (480 V, A B C N G) (`busway_industrial`) | `SCS` / `CSC` / `SCS` (S steel, C copper block) |
| Generator Bus Duct (13.8 kV, 1,200 A, A B C N G) (`bus_duct_gen`) | `BCB` / `CCC` / `BCB` (B brick, C copper block) |
| Generator Bus Relay Panel (87B, 51V, 50/51) (`bus_relay_panel`) | `SGS` / `SRS` / `SDS` (S steel, G glass pane, R comparator, D redstone) |
| IDR Kiln (dry route) (`idr_kiln`) | `PSP` / `BFB` / `SXS` (S steel, P steel pipe, B bricks, F blast furnace, X crankshaft) |
| Exhaust Silencer (500 kW) (`exhaust_stack`) | `S` / `B` / `S` (S steel, B iron bars) |
| Caustic Evaporator (to 50 %) (`caustic_evaporator`) | `SPS` / `SCS` / `PSP` (S steel, P steel pipe, C copper block) |
| TiCl4 Purification (`ticl4_purification`) | `SPS` / `SCS` / `SFS` (S steel, P steel pipe, C copper block, F furnace) |
| Titanium Chlorinator (controller) (`ti_chlorinator`) | `SHS` / `BFB` / `PSP` (S steel, H hopper, B bricks, F blast furnace, P steel pipe) |
| RHR Pump (200 kW, 4.16 kV) (`rhr_pump`) | `SCS` / `SXS` / `PSP` (S steel, C copper block, X piston, P steel pipe) |
| Cracked Gas Compressor (300 hp, 4.16 kV) (`cracked_gas_compressor`) | `PSP` / `CXC` / `SBS` (S steel, P steel pipe, C copper block, X crankshaft, B iron block) |
| Emulsion Blender (`emulsion_blender`) | `SHS` / `PXP` / `SCS` (S steel, H hopper, P steel pipe, X piston, C copper) |
| UF6 Distillation Columns (`distillation_columns`) | `SPS` / `SPS` / `SFS` (S steel, P steel pipe, F furnace) |
| Hafnium Column (extractive distillation) (`hafnium_column`) | `SPS` / `S·S` / `BFB` (S steel, P steel pipe, F furnace, B bricks) |
| Empty 48Y Cylinder (`empty_48y_cylinder`) | `SSS` / `S·S` / `SCS` (S steel, C copper) |
| Sub-Base Fuel Tank (2,000 L) (`fuel_tank`) | `SSS` / `S·S` / `SPS` (S steel, P steel pipe) |
| Empty 30B Cylinder (`empty_30b_cylinder`) | `SCS` / `S·S` / `SSS` (S steel, C copper) |
| Equipment Hatch (`equipment_hatch`) | `SSS` / `SDS` / `SSS` (S steel, D iron door) |
| Submerged Arc Furnace (8 MW) (`submerged_arc_furnace`) | `CHC` / `BFB` / `SBS` (S steel, C copper, H hopper, B bricks, F blast furnace) |
| HCl Synthesis Unit (`hcl_synthesis_unit`) | `PSP` / `SBS` / `SPS` (S steel, P steel pipe, B blast furnace) |
| Rod Welder (helium, 2 MPa) (`rod_welder`) | `SCS` / `GRG` / `SPS` (S steel, C copper block, G glass, R redstone block, P steel pipe) |
| VPSA Oxygen Unit (100 Nm3/h) (`vpsa_oxygen`) | `PSP` / `SXS` / `SIS` (S steel, P steel pipe, X piston, I iron block) |
| Nickel Electric Furnace (1 MW, six-in-line) (`nickel_electric_furnace`) | `CHC` / `BFB` / `BBB` (C copper, H hopper, B bricks, F blast furnace) |
| Vacuum Crystallizer (`vacuum_crystallizer`) | `SPS` / `S·S` / `SHS` (S steel, P steel pipe, H hopper) |
| Top Drive (250-ton, 400 hp) (`top_drive`) | `SNS` / `CXC` / `SPS` (S steel, N chain, C copper, X crankshaft, P steel pipe) |
| Empty Drum (208 L) (`empty_drum`) | `SNS` / `S·S` / `SSS` (S iron, N iron nugget) |
| Ground Rod (`ground_rod`) | `C` / `I` / `C` (C copper, I iron) |
| Receptacle IEC 60309 red (400 V 3-phase, 63 A) (`receptacle_iec_60309_red_63`) | shapeless: 2 iron, 4 copper, 1 red dye |

## Single-phase cable (2026-11-27)
Shapeless, per size, in each loader's resources (written by `tools/wire_look.py`, which `tools/make_assets.py` runs at its end): one hot
wire, one neutral wire and one ground wire make one single-phase cable of the wires' size, with both wires wool-wrapped or both plastic;
two to three of a size make one of the next, as for every conductor. `recipes_unique` checks them.
