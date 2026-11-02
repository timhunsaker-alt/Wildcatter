# The hyperbolic natural-draft cooling tower (APPROVED 2026-11-01 with the Phase 8c plan)

The turbine island's circulating water (turbine-island.md) goes round a closed loop:
1. The circulating water pumps stand in the tower's own basin.
2. The condenser's warmed water goes up the tower's riser and is sprayed over the fill.
3. Air drawn up the shell by its own draft cools it toward the air's wet-bulb temperature.
4. The water falls back to the basin.

The tower needs no fans and draws no power.

Decided with Tim on 2026-11-01:
- **The shell rises lift by lift** as it is fed, after the basin and raker columns, then the fill goes in.
- **A tenth of the real materials:** about 430 gray concrete blocks and 44 steel ingots in all. The steel is really about a
  hundredth of the real rebar (390 t would be 390 ingots at a tenth), as offered.
- **The wet bulb comes from the biome.**

Sources:
- **Size:** Calder Hall's towers, 88 m tall, one for each 50 MWe unit *(textbook)*. A base diameter of about 60 m *(unverified)*.
- **Shape:** a hyperboloid shell of reinforced concrete about 0.2-0.6 m thick, on raker columns over a cold-water basin. It is
  jump-formed in lifts of 1-2 m, with film fill and water distribution inside *(textbook)*.
- **Wet bulb:** Stull (2011), from the air temperature and relative humidity *(published empirical fit)*.
- **Evaporation:** the heat rejected over 2.45 MJ/kg. Blowdown at 5 cycles of concentration is a quarter of the evaporation
  *(textbook)*.

A figure marked *(unverified)* is textbook, an estimate or a proposal.

## Numbers *(proposed unless marked)*
**The shell:**
- r(y) = 17 √(1 + ((y − 62)/37.1)²) m over the basin floor, from **8 m** (radius 30 m) to the throat at **62 m** (17 m) and the top
  at **88 m** (20.8 m).
- **54 lifts** of 1.5 m, the last one short, to 88 m.
- **Thickness:** 0.40 m at the base, tapering to 0.18 m at the top. That is about 3,150 m³ of shell.
- **Raker columns:** 40 pairs from the rim to the shell's base. The air goes in between them.

**The basin:** radius 31 m, 3 m deep, about 9,000 m³ of water.

**Materials and time (time compressed; real towers take months):**

| Stage | Gray concrete | Steel ingots | Other | Time |
|---|---|---|---|---|
| Basin | 100 | 10 | | 60 s |
| Raker columns | 15 | 2 | | 30 s |
| Shell | about 6 a lift, tapering with the shell (about 317 in all) | about 0.6 a lift (about 32) | | 15 s a lift |
| Fill and distribution | | 4 | 64 plastic sheets | 30 s |

**Thermal:**
- **Design:** 104 MW, a 10 °C range and an **8 °C approach** at a 15 °C wet bulb.
- **Cold water:** wet bulb + 8 × (Q / 104 MW)^0.6 °C, followed with a 2-minute lag.

**Climate from the biome:**
- **Dry bulb:** −5 + 25 × the biome's temperature (plains 15 °C, desert 45 °C, snowy −5 °C).
- **Relative humidity:** 20 % where it never rains or snows (desert, savanna, badlands); 85 % in jungles and swamps; 60 %
  elsewhere.
  - The biome's own downfall number is private to the game, hence this.
- **Wet bulb:** by Stull, for example plains about 11 °C and desert about 25 °C.

**Water:**
- **Evaporation:** Q / 2.45 MJ/kg, **42 kg/s** at full load.
- **Blowdown:** a quarter of that.
- **Make-up:** about **53 kg/s**, from a water pump.
- **Basin under 10 %:** the CW pumps standing in it lose their suction.

**With no tower:** a CW pump in a lake or river gives 20 °C water, as in 8b.

## Parts (each drawn facing north; x east, z south)
| Part | Cells | Real basis |
|---|---|---|
| `natural_draft_tower` | 4 x 3 x 4 | The basin's outlet structure and the hot-water riser at the basin's edge. **The tower's controller.**<br>• It faces the tower: the centre lies 34 m in front of its main cell.<br>• Concrete, steel and plastic sheets by a hopper on top. |
| `tower_shell` | 1 | An invisible, unbreakable block with no item that makes the tower solid. The controller places them along the rim, the raker columns and each finished lift. |

**The tower itself** is drawn by the controller's renderer as one smooth model in concrete: the rim, the raker columns, the shell
up to its last lift, and the fill. While it rejects heat, a white plume rises from the top.

**The pond:** when the basin stage is done, the controller digs the 3 m deep pond inside the rim and fills it with water. CW pumps
can stand in it.

**Breaking the controller** removes the shell blocks and the pond's water. Poured materials are not returned.

## Nozzles
- **Hot water in:** `COOLING_WATER` off a pipe on the controller's top face. The condenser offers its warmed water on the south
  face of its bottom cells.
- **Make-up water:** off a pump, into the controller.
- **Materials:** a hopper on top. Slot 0 takes gray concrete, slot 1 steel ingots, slot 2 plastic sheets.

## How it runs
- **The water's temperature:** `COOLING_WATER` carries it in its mix (slot 0, °C).
  - A CW pump standing in a tower's basin gives water at that tower's cold-water temperature.
  - The condenser counts it for its vacuum, then offers it back warmed.
- **Each interval the tower:**
  - pulls the hot water and takes Q = kg × 4,186 × (T_in − T_cold);
  - moves its cold-water temperature toward wet bulb + approach(Q);
  - loses evaporation and blowdown from the basin, and takes make-up.
- **The readout:**
  - the stage and what it needs;
  - lifts done and height;
  - heat, wet bulb, cold water, evaporation and make-up;
  - basin level.

## Game test
`coolingTower`:
- **Build:** the controller with materials in its hopper. Hooks step it through the stages: the pond fills with water, and the
  shell's blocks stand on the hyperbola at several heights.
- **Water:** a CW pump standing in the basin pumps water at the tower's temperature.
- **Heat:** with 104 MW given by a hook, the cold water settles near wet bulb + 8 °C, make-up is drawn and the plume is on.
- **Screenshot:** the tower from a distance.

## Added while building (8c)
- **Heights** are measured over the ground at the controller's floor; the pond is dug 3 m below it.
- **Cold water** is never colder than **4 °C** *(proposed)*: in snowy biomes the wet bulb is below freezing.
- **Humidity:** the biome's own downfall number is private to the game, so the relative humidity comes from whether the biome
  rains (20 % if not), and 85 % in jungles and swamps.
- **Light:** the renderer draws the shell in full sky light, so it reads as daylit concrete and darkens at night with the sky.
- **Game test:** it pours the basin from the hopper's concrete and steel, and a hook finishes the rest. The screenshot is taken
  about 50 m off, inside the test client's 5-chunk view distance.
