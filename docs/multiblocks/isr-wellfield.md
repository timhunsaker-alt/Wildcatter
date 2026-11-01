# The ISR wellfield (APPROVED 2026-11-01 with the Phase 7c plan)

**In-situ recovery** of uranium from a roll-front deposit, the way it is done in Wyoming (Smith Ranch, Lost Creek, Crow Butte):
- no mining;
- wells pump oxygenated groundwater through the ore;
- ion-exchange resin takes the uranium out at the surface.

It is the late-game uranium source. Mill-made and ISR uranium meet at the mill's precipitation tank (uranium-mill.md).

Decided with Tim on 2026-11-01:
- **Chemistry:** alkaline, the Wyoming way: groundwater with oxygen added.
- **Rate:** 10× the real head grade at the real flow per well.

Sources:
- NRC NUREG-1910 GEIS ch. 2 (the NRC's generic environmental review of ISR);
- EPA Dewey-Burdock Class III permit fact sheet;
- Lost Creek technical reports (2022, 2026);
- Cameco Inkai technical report (2024);
- WNA, *In situ leach mining of uranium*;
- Cameco / ASEG on gamma logging.

A figure marked *(unverified)* is textbook, scaled or a proposal.

```
 injectors <- barren lixiviant + O2 <- HEADER HOUSE <- barren <- IX COLUMNS -> eluate -> PRECIPITATION TANK (mill)
 producers -> pregnant lixiviant  -> HEADER HOUSE -> pregnant -> IX COLUMNS     ^ salt (hopper), acid (pipe)
                                      ^ OXYGEN GENERATOR (touching), 480 V for the producers' pumps
```

## Roll-fronts (`well/RollFront.java`)
- **The deposit:** a crescent band of ore in desert and badlands sandstone. It is found only by drilling: the scintillometer can't hear it, because real gamma gets through about 0.3 m of rock.
- **Placement:** its own 256-block seed grid, with a front in about half the cells *(proposed)*.
- **Shape:** an arc of a ring, radius 40-80 blocks over 120°. The band is 15-30 blocks wide (real 8-30 m) and 3-6 blocks thick (real 1.5-8 m).
- **Depth:** top at y -10 to -45, which is 60-120 m under a desert surface (real 100-300 m), compressed as the oil fields are.
- **Grade:** 1 % eU3O8 along the middle of the band, falling to 0.3 % at its edges. That is 10× a real 0.1 %; GEIS gives 0.05-0.25 %.
- **Recovery:** 70 % of the uranium in place can be won (WNA: 60-80 %). The producers on a front draw from one pool, kept in `RollFrontData`.

## Gamma log
- **On a strike:** when a rig's hole passes through a front, the rig completes it as an ISR well and announces a gamma log:
  - the ore zone's top and bottom y;
  - its thickness;
  - its eU3O8;
  - its grade-thickness (GT, m·%) against the cut-off of **0.6 m·%**. That is 10× a real 0.2 ft·% (Lost Creek), which is about 0.06 m·%.
- **On a miss:** a hole that misses logs "background".
- **Where the log stays:** the well keeps its log in its readout.
- **When the rig completes an ISR well:** a front above any oil or gas field in the column; the rig drills through the front's base.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `isr_wellhead` | 1 block | The casing's cap. PVC casing 4.5-6 in (Dewey-Burdock). The rig sets it on the casing; a wellhead off casing reads NOT A WELL. **Injector** without a pump; **producer** with a submersible pump clicked in. |
| `submersible_pump` (item) | - | A 4 in submersible, **7.5 hp (5.6 kW), 480 V**: 2.5 L/s against about 150 m of head is 3.7 kW of water power at about 65 % wire-to-water *(unverified)*. |
| `header_house` | 4 x 4 x 10 *(unverified)* | The wellfield building: manifolds, a meter and valve per well, the drives for up to **25 producers** (Lost Creek MU1: about 25 producers and 53 injectors a house), and oxygen on the injection header. Its 480 V load is the drives: 5.6 kW a running pump, a variable load like the land rig's drive house (drives in the header house are *unverified*). Power reaches each producer along its pipe, as real cables are trenched with the lines. It holds back **1 %** as bleed (Lost Creek 0.5-1.5 %; GEIS 1-3 %), counted, so the wellfield draws water inward. |
| `oxygen_generator` | 3 x 3 x 6 *(unverified)* | A PSA oxygen skid against the header house: **50 Nm3/h, 25 kW at 480 V** (about 0.5 kWh/Nm3, *unverified*). Dose **300 mg/L** (100-500, *unverified*): 25 producers at 2.5 L/s take 47 Nm3/h. |
| `ix_columns` | 8 x 6 x 4 *(unverified)* | A lead-lag pair of pressurized resin columns, each 3.5 m across by 4.6 m (Crow Butte).<br>• It takes **95 %** of the uranium (GEIS), up to **63 L/s** (1,000 gpm, one header house; *proposed*).<br>• The lead column's 44 m3 of resin holds 80 g of U3O8 a litre (about 5 lb/ft3), 3,520 kg.<br>• Elution with NaCl brine, rock salt by hopper, made up at 10 % *(proposed, as the mill's SX)*.<br>• The eluate is acidified with **10 g/L** of sulfuric acid *(proposed)*: below pH 2 the carbonate complex breaks (Nichols Ranch), and magnesia can bring the uranium down (US 2,900,227, pH 6.8-7.1).<br>• Eluate out at **15 g/L U3O8** (GEIS 8-20 g/L U). |

## The 5-spot
- **Flow:** a producer pumps **2.5 L/s** (40 gpm; Inkai 35-46 gpm) while its header house runs its pump.
- **Head grade:**
  - **500 mg/L U3O8** (10× a real 50; GEIS about 60, Lost Creek 40),
  - × its share of four injectors injecting within **25 blocks** of it (real 5-spot spacing is 12-50 m),
  - × the share of their lixiviant that carried oxygen,
  - × the ore's grade at the well against the band's richest,
  - × what is left of the front's recoverable uranium.
- **Injectors:** each takes **1.24 L/s**, a producer's flow less bleed over about two injectors (Lost Creek has 2.1 per producer). An injector counts while it took lixiviant in the last 2 s.
- **Output:**
  - One full pattern on the band's middle makes about **4.5 kg of U3O8 an hour**.
  - **Six fill a 400 kg drum in about 12 hours.**

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **ISR well:** its pipe on any side.
- **Header house:**
  - producers' pipe on the west face of its bottom cells;
  - barren lixiviant to the injectors on the east face of its bottom cells;
  - to and from the IX columns on the south face of its back cells (z 9, bottom): pregnant out of the west two (x 0-1), barren in at the east two (x 2-3);
  - its 480 V box on the north face of its main cell.
- **Oxygen generator:** its 480 V box on the west face of its main cell. It works against a header house.
- **IX columns:**
  - pregnant lixiviant in on the north face of its front cells (z 0, bottom);
  - out of the south face of its back cells (z 3, bottom): eluate from the west half (x 0-3), barren lixiviant from the east half (x 4-7);
  - sulfuric acid in on the east face of its bottom cells;
  - rock salt by a hopper on top.

## How it runs
- **Start-up:** the loop starts itself. The producers pump barren groundwater first, which goes through the IX columns and back as lixiviant. Once the injectors are injecting it with oxygen, the uranium comes.
- **Header house problems:**
  - no oxygen generator: NO OXYGEN, and the head grade falls away as the injectors' oxygen runs out;
  - no power: the pumps stop.
- **IX columns:**
  - without salt or acid they stop eluting: NO SALT, NO ACID;
  - the resin loads until it is full, then the uranium passes through.
- **Removing a part** stops what needs it.

## Left out (ROADMAP Ideas)
- **CO2:** bottled CO2 for pH and bicarbonate. The mod has no CO2 source, so the native bicarbonate carries the uranium.
- Monitor-well rings and excursions.
- Groundwater restoration.
- A disposal well for the bleed.
- Satellite IX with trucked resin.

## Game test
`isrWellfield`:
- **Setup:** a forced test seed, the column turned to desert.
- **Drilling:** the small rig drills a hole that must report a gamma log at or above the cut-off and leave an ISR well.
- **The pattern:** four more wells are set in a 5-spot, and a pump is clicked into the middle one.
- **The plant:** header house, oxygen generator and IX columns on a 480 V diesel set; acid from a test-filled stock tank; salt by hopper; the eluate piped to a precipitation tank with its dryer and filler.
- **Checks:**
  - the producer reads 4 injectors and a head grade near its 5-spot value;
  - the IX columns make eluate, and the precipitation tank precipitates from it;
  - with the oxygen generator removed, the head grade falls.
- **Screenshot:** the wellfield.

## Added while building (7c)
- The header house meters the injectors on its east pipe rather than each pulling for itself. Real header houses do: a valve
  and meter on each well. One producer's water is shared evenly among its four injectors (0.62 L/s each), so a lone 5-spot works.
- A producer's head grade also takes the ore's grade at the well against the band's richest, so the edge of a front pays less
  than its middle and exploration matters.
- `fillbiome` is limited to 32,768 blocks, so the game test turns only the 5-spot's columns to desert.
