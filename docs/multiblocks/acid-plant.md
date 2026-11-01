# The sulfuric acid plant (APPROVED 2026-11-01 with the Phase 7b plan)

A sulfur-burning **double contact, double absorption** (DCDA) plant: what feeds the uranium mill's leach (uranium-mill.md). It is
rated **150 t/day of H2SO4**, a small real plant: Riverton, Wyoming ran 250 t/d beside a 550 t/d uranium mill, and Ranger about
280 t/d. It is a chain of distinct true-size parts, like the cracker (cracker.md). The melter, the furnace and the converter
work through what touches them; the gas goes between them on steel pipe.

Sources: EU BREF *Large Volume Inorganic Chemicals: ammonia, acids and fertilisers* (2007) §4.4; MECS start-up procedures. A
figure marked *(unverified)* is a textbook or scaled value.

```
sulfur (hopper) -> SULFUR MELTER -> molten sulfur -> SULFUR FURNACE <- MAIN BLOWER <- DRYING TOWER
                                                      | (touching)
                                                WASTE HEAT BOILER (steam to the melter's coils)
                                                      | burner gas
                                                  CONVERTER <-> INTERPASS ABSORBER (touching)
                                                      | (touching)
                                                FINAL ABSORBER and stack -> sulfuric acid -> storage tank
```

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `sulfur_melter` | 4 x 3 x 8 | Concrete melting pit with steam coils, holding sulfur at 135-145 C (it melts at 119 C). Sulfur in by a hopper on top. Its coils take steam from a waste heat boiler touching it; from cold, before there is steam, it fires pipeline gas or LPG (100 kW: 0.57 kg/s at about 140 kJ/kg to heat and melt, derived) |
| `drying_tower` | 3 x 12 x 3 | Packed tower that dries the air with 98 % acid before the blower. It touches the blower *(size unverified)* |
| `main_blower` | 3 x 3 x 5 | Main air blower, about 13,000 Nm3/h (2,100 Nm3 of air a tonne of acid, derived from 11 % SO2). 250 kW, 4.16 kV, two-pole. The BREF gives 35-50 kWh/t for the whole plant, mostly the blower |
| `sulfur_furnace` | 3 x 3 x 10 | Brick-lined horizontal burner, about 1,100 C. Burner gas is 11 % SO2 (BREF: 10.5-11.8 %): 9.2 kg of gas a kg of sulfur. It touches the blower and the boiler *(size unverified)* |
| `waste_heat_boiler` | 3 x 4 x 8 | Fire-tube boiler: cools the burner gas to about 420 C for the first bed and raises about 1.2 t of 40 bar steam a tonne of acid (BREF 3.1-3.4 GJ/t), 2.1 kg/s. Feedwater off a water pipe. The melter takes its steam; the rest goes up its silencer |
| `converter` | 5 x 15 x 5 | Four beds of V2O5 catalyst (3+1), 4.5 m across *(unverified)*, about 200 L of catalyst a t/d (30 m3). It touches the two absorbers |
| `interpass_absorber` | 3 x 12 x 3 | Absorbs the SO3 after the third bed so the fourth can finish: 99.8 % conversion. Without it the plant runs single-absorption at 95 % (BREF: 85-95 % before the intermediate absorption) and the stack shows the difference |
| `final_absorber` | 3 x 20 x 3 | The final absorption tower with the stack above it. 98.5 % acid; its process water off a water pipe |

## Numbers
- Sulfur: 32.06 / 98.08 = 0.327 kg a kg of acid at full conversion: **0.57 kg/s** at 150 t/d. A sulfur block (2,160 kg) lasts
  about an hour.
- Acid: 1.74 kg/s of H2SO4, made as **98.5 % acid** (1.84 kg/L): about **0.96 L/s**. It is counted in litres.
- Water: the SO3 takes its water (18/80 by weight) and the 1.5 % dilution, about 0.34 L/s; the boiler about 2.1 L/s.
- The stack: the SO2 the converter did not convert, in kg/h.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- Melter: molten sulfur out of the south face of its back cells (z 7, bottom layer); start-up gas in on the east face of its
  bottom cells. Hopper on any top cell.
- Blower: its 4.16 kV box on the west face of its main cell.
- Furnace: molten sulfur in on the east face of its front cells (z 0, bottom layer), the burner gun's side: the blower is at its front.
- Boiler: burner gas out of the south face of its back cells (z 7, bottom layer). Water pipe on any face.
- Converter: burner gas in on the north face of its front cells (z 0, bottom layer).
- Final absorber: sulfuric acid out of the south face of its bottom cells (z 2). Water pipe on any face.

## Layout (the game test's, facing north, plan view; each letter a cell of the bottom layer)
```
 D D D B B B F F F           D drying tower    B main blower   F sulfur furnace (10 long, south)
 D D D B B B F F F           W waste heat boiler (under the furnace's south end, touching it)
 D D D B B B F F F           M sulfur melter (touching the boiler)
       B B B F F F
       B B B F F F   ...     C converter, I interpass absorber, A final absorber: C with I on its west, A on its east
```
The grid is a sketch; the game test is the layout of record.

## How it forms and runs
- The **furnace** is the front of the plant. It runs while it has molten sulfur at its front, a blower touching it with a drying
  tower against the blower, and a waste heat boiler touching it with water. It drives the blower; its rate follows the blower's
  output. It hands its burner gas to the boiler, which needs room at its outlet.
  - No blower: NO AIR. No drying tower: WET AIR (real plants stop: wet gas makes acid mist that eats the steel). No boiler or a dry
    boiler: NO BOILER.
- The **melter** melts while it has heat: steam from a boiler that is making it, or start-up gas.
- The **converter** runs while burner gas waits and a final absorber touching it has water and room for acid. With an interpass
  absorber also touching it, it converts 99.8 %; without, 95 %.
- Removing a part stops what needs it; nothing is lost but what each part holds.

## Game test
`acid_plant`: a genset steps up to 4.16 kV for the blower; sulfur in a hopper on the melter, start-up LPG from a bullet, water topped up by a hook (as the
cracker test does). Acid reaches a storage tank. Then the interpass absorber is broken and the converter reads single
absorption with a stack SO2 at least ten times higher. A screenshot of the plant.

## Added while building (7b)
- The furnace takes its molten sulfur on the east side of its front, the burner gun's side, because the blower stands against its
  front.
- The melter's start-up burner, and the mill's dryer, fire LPG as well as gas, as the cracking furnace does.
- A 250 kW blower started across the line takes a lone 500 kW diesel set down on frequency, as it would in real life: the game
  test parallels two sets on a busway before the plant starts.
