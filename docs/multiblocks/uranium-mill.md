# The uranium mill (APPROVED 2026-11-01 with the Phase 7b plan)

A conventional **acid-leach uranium mill** that makes yellowcake in 208 L drums from uranium ore and pitchblende (ores.md). The
flowsheet follows Cominak (Niger) and White Mesa (Utah):
- crush and grind;
- leach in sulfuric acid with pyrolusite as the oxidant (as at Rössing and Ranger);
- wash in counter-current decantation;
- solvent extraction with an amine in kerosene, stripped with salt water;
- precipitate with **magnesia**, dry, and drum.

It is a chain of distinct true-size parts on steel pipe, like the cracker. The crusher feeds the grinding mill by touching, and
the precipitation tank, dryer and drum filler form a unit by touching.

Sources:
- IAEA TRS-359, *Uranium extraction technology* (1994): flowsheets and equipment for White Mesa, Rabbit Lake, Cominak, Rössing and Key Lake.
- WNTI *Standard for the packaging and transport of uranium concentrates* V4 (2025).
- ASTM C967 (uranium ore concentrate).

A figure marked *(unverified)* is a textbook value, a scaled value, or a proposal.

```
ore (hopper) -> JAW CRUSHER -> SAG MILL -> ore slurry -> LEACH TANK -> leach slurry -> CCD THICKENER -> pregnant liquor
                 (touching)      ^ water               ^ acid, pyrolusite             ^ wash water \-> tailings
pregnant liquor -> SX CIRCUIT -> strip liquor -> PRECIPITATION TANK -> YELLOWCAKE DRYER -> DRUM FILLER -> yellowcake drums
                   ^ kerosene, rock salt          ^ magnesia            ^ gas  (all three touching)  ^ empty drums
```

## Size
- **Ore:** sized on White Mesa (1,800 t/d rated), **20.8 kg/s of ore** at full rate.
- **Uranium:** SX and precipitation are sized for the game's 2 % ore, **0.42 kg/s of U3O8 in the feed**. That is about 12,000 t a year, Key Lake class: the ten-times grade makes this a large uranium plant.
- **Pitchblende** (60 %): the crusher feeds slower so the U3O8 in the feed never passes 0.42 kg/s. Pitchblende saves digging, not time. Real mills blend high-grade ore down too.
- **Hold-up:** the real leach takes 10-24 h. Here each part holds a few seconds of its rated flow, as every process part in the mod does; the rates are real.
- **Recovery:** 93 % overall (Rabbit Lake; the range is 90-95 %). It is all taken in the leach. The later stages pass on what they get.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `jaw_crusher` | 3 x 4 x 4 | Primary jaw crusher, C80 class *(unverified)*: 75 kW, 480 V. Uranium ore or pitchblende in by a hopper on top. It touches the SAG mill, which drives it |
| `sag_mill` | 7 x 7 x 5 | White Mesa's semi-autogenous mill: 5.5 m across by 1.8 m, 522 kW, 4.16 kV. Grinds to -0.5 mm (Key Lake) in water at 55 % solids: 0.82 L of water a kg of ore, 17 L/s. Water off a water pipe |
| `leach_tank` | 8 x 9 x 8 | White Mesa leach tank, 7.6 m across by 8.2 m, with a 56 kW, 480 V agitator.<br>• Acid: **70 kg of H2SO4 a tonne of ore** (Rabbit Lake), 1.46 kg/s, about 0.80 L/s of 98.5 % acid.<br>• Pyrolusite (MnO2) as the oxidant: **1.5 kg a tonne** (Rössing, 0.7-1.5), by a hopper on top. A pyrolusite block (2,560 kg) lasts about 23 h.<br>• Real leaches run at 40-55 C; the heating is left out. |
| `ccd_thickener` | 12 x 4 x 12 | White Mesa high-rate thickener, 12 m across by 3 m, standing for the CCD train. Its rake drive is 7.5 kW at 480 V *(unverified)*.<br>• Wash ratio 2.5 (Rabbit Lake): 2.5 kg of wash water a kg of solids, 52 L/s off a water pipe.<br>• Underflow at 50 % solids goes to the tailings, counted in its readout.<br>• The overflow is the pregnant liquor. |
| `sx_circuit` | 6 x 3 x 16 | Mixer-settlers *(size unverified)*: Alamine 336 amine in **kerosene**, stripped with **1.5 M NaCl** (Cominak, White Mesa). Mixer motors 30 kW at 480 V *(unverified)*.<br>• Kerosene: a first charge of **10,000 L**, then a loss of 0.1 L for every m3 of pregnant liquor *(both proposed)*.<br>• Strip liquor carries **25 g/L of U3O8** (TRS-359).<br>• The strip (87.7 g/L NaCl) is mostly recycled. Its make-up is 10 % *(proposed)*, about 0.14 kg/s, so a rock-salt block (2,117 kg) lasts about 4 h.<br>• The raffinate goes back to the CCD wash in a real mill; here it is not counted. |
| `precipitation_tank` | 4 x 5 x 4 | Precipitation with milk of magnesia to pH 7 over 4 h (Cominak). Agitator 15 kW at 480 V *(unverified)*.<br>• MgO: **0.3 kg a kg of U3O8** *(proposed: 0.14 is the stoichiometric amount; real use is higher because it also takes up the free acid)*.<br>• A Magnesia item (1,250 kg) lasts about 3 h. It comes in by a hopper on top. |
| `yellowcake_dryer` | 3 x 8 x 3 | Spray dryer, as at Cominak, fired on pipeline gas or LPG: **0.5 MW** *(proposed)*. It touches the tank and the filler |
| `drum_filler` | 2 x 4 x 2 | Drum filling station, 5 kW at 480 V *(unverified)*. Empty drums come in by a hopper on top. Full drums come out to a hopper under it, or to an empty hand |

## Products and items
- **New on steel pipe:**
  - ore slurry and leach slurry, counted in kg;
  - pregnant liquor and strip liquor, counted in L.
- **What the streams carry:**
  - Each slurry and liquor carries its uranium along with it, as cracked gas carries what it is made of.
  - No storage tank holds them.
- **Sulfuric acid:** counted in L; a storage tank may hold it.
- **Magnesia:** a furnace or blast furnace turns one magnesite into one Magnesia. This is caustic calcining at about 700-1,000 C *(unverified)*, as limestone becomes lime. MgCO3 is 47.8 % MgO, so 2,610 kg of magnesite gives 1,250 kg.
- **Empty drum:** a 208 L (55 gallon) open-head steel drum, crafted from iron.
- **Yellowcake drum:**
  - **400 kg net** of magnesium diuranate concentrate at **80 % U3O8**: 320 kg of U3O8, 271 kg of uranium. The WNTI allows up to 450 kg gross, and its sample manifest shows about 437 kg net.
  - ASTM C967 asks for at least 65 % uranium (77 % U3O8). The grade of a magnesia concentrate is a proposal; ammonia (ADU) and a calciner give 94-98 %, and that route is in ROADMAP Ideas.
- **Output:**
  - A uranium-ore block (2,300 kg at 2 %) makes about 43 kg of U3O8, so about 7½ blocks fill a drum.
  - At full rate the mill fills about 4.4 drums an hour.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **Motor boxes:** every powered part has its box on the west face of its main cell. That is 4.16 kV for the SAG mill and 480 V for the others.
- **Hoppers:** a hopper on any top cell feeds a part that takes items. The drum filler's full drums come out to a hopper under any of its bottom cells.
- **SAG mill:** ore slurry out of the south face of its back cells (z 4, bottom layer).
- **Leach tank:**
  - ore slurry in on the north face of its front cells (z 0, bottom layer);
  - acid in on the east face of its bottom cells;
  - leach slurry out of the south face of its back cells (z 7, bottom layer).
- **CCD thickener:** leach slurry in on the north face of its front cells (z 0, bottom layer); pregnant liquor out of the south face of its back cells (z 11, bottom layer).
- **SX circuit:**
  - pregnant liquor in on the north face of its front cells (z 0, bottom layer);
  - kerosene in on the east face of its bottom cells;
  - strip liquor out of the south face of its back cells (z 15, bottom layer).
- **Precipitation tank:** strip liquor in on the north face of its front cells (z 0, bottom layer).
- **Dryer:** pipeline gas in on the east face of its bottom cells.
- **Water:** any face of the SAG mill and the thickener, off a water pipe.

## How it forms and runs
- **SAG mill:** it runs while the crusher touching it has ore, water waits and its outlet has room. It drives the crusher.
- **Each motor part** runs its own motor while it has work, and its rate follows the motor's output.
- **Leach tank:**
  - without acid it stops: NO ACID;
  - without pyrolusite it stops: NO OXIDANT (the uranium(IV) will not dissolve).
- **SX circuit:**
  - with less than its first charge of kerosene it reads FILLING;
  - without salt it stops: NO STRIP SALT.
- **Precipitation tank:**
  - it holds the wet precipitate for the dryer;
  - without magnesia it stops: NO MAGNESIA.
- **Dryer:** it dries what the tank holds into the filler, while it has gas and the filler has room.
- **Drum filler:** it fills a drum each time it holds 400 kg and has an empty drum. It holds two drums' worth at most.
- **Removing a part** stops what needs it. Nothing is lost but what each part holds.

## Game test
`uranium_mill`:
- **Power and supplies:**
  - a genset at 4.16 kV with a unit substation to 480 V;
  - uranium ore, pyrolusite, rock salt, magnesia and empty drums in hoppers;
  - acid from a test-filled storage tank;
  - water topped up by a hook, as the cracker test does;
  - the SX's first kerosene charge by a hook;
  - LPG from a bullet for the dryer.
- **Checks:**
  - Slurry, pregnant liquor and strip liquor all flow.
  - The filler is given 380 kg by a hook, so the test does not wait the 14 minutes a first drum takes. The mill's own yellowcake must then fill the drum and drop it into the hopper under the filler.
  - Pitchblende in the crusher: the SAG mill's readout shows the feed held to the 0.42 kg/s U3O8 limit.
- **Screenshot:** the mill.

## Added while building (7b)
- The SAG mill's 522 kW motor, started across the line, needs a strong supply: the game test parallels three 500 kW diesel sets
  on a busway into a backward unit substation. Its small motors have their own set at 480 V. Soft starters and drives remain in
  ROADMAP Ideas.
- The leach tank and the thickener are drawn open-topped under their bridges, as real ones are.
- The dryer never fires above its 0.5 MW rating; a backlog of precipitate waits in the tank.
