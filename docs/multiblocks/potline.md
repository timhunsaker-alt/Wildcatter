# The potline (Phase 10b3b, approved with the 10b3 plan 2026-11-03)

A Hall–Héroult potline:
- **pots** in series dissolve alumina in a molten cryolite bath at about 960 °C and pass a direct current through it.
  - **The current:** carbon anodes hang in the bath, and the cathode is the carbon-lined steel shell under a pad of metal.
  - **What it makes:** the current splits the alumina into aluminium, which settles on the pad, and oxygen, which burns the
    anodes to CO2.
- **rectiformers** on the 138 kV grid hold the line current.
- **aluminium bus** carries the current from the rectiformers down the rows of pots and back.
- **a fume treatment centre** catches the pots' fluoride on fresh alumina, which then goes to the pots.
- **a vacuum tapping crucible** takes the metal to the cast house, where **a casting machine** casts it into ingots.

**Decided with Tim on 2026-11-03:**
- **Line:**
  - up to 40 pots, about 180 V and 33 MVA;
  - 4 x 60 kA rectiformers on 138 kV, three carrying the line and one spare.
- **Bus:** pots set side by side join in series through their own risers. Bus is only placed at the row ends, the crossovers
  and the rectifier run.
- **Pots:**
  - they have anode effects;
  - they freeze after about 4 h without current;
  - they have a fume treatment centre.
- **Metal:** a vacuum tapping crucible and a casting machine making 22.7 kg ingots.

**Sources:**
- **The pot:**
  - Prebake pots of the 180 kA class (Pechiney AP18) run at about 4.2-4.4 V a pot, with 93-95 % current efficiency.
  - Faraday's law gives **0.3356 kg of aluminium a kA·h**.
  - Net anode carbon is about 0.40-0.42 kg a kg of aluminium, and alumina 1.92-1.93 kg (Grjotheim and Kvande, *Introduction to
    Aluminium Electrolysis*, 1993).
- **The pot's voltage:**
  - The reversible decomposition voltage with carbon anodes is about 1.2 V. With the anode overvoltage, the back-EMF is about
    1.65 V.
  - The rest is ohmic: the bath, the anode, the cathode and the external bus.
- **AlF3:** about 15-25 kg a t of aluminium with dry scrubbing, and twice that or more without (IAI).
- **Anode effects:**
  - When the bath's alumina falls below about 1-2 %, a gas film forms under the anodes, and the pot voltage jumps to 20-50 V.
  - It makes CF4 and C2F6. The IPCC Tier 2 slope for point-feed prebake pots is **0.143 kg of CF4 a t of aluminium for each
    anode-effect minute a pot-day** (IPCC 2006, vol. 3, ch. 4.4; the 2019 refinement keeps it).
- **Dry scrubbing:** a fume treatment centre injects fresh alumina into the pot gas and catches it in bag filters. It catches
  more than 99 % of the HF, and the reacted alumina goes to the pots (Alcoa A398, Fläkt ABART).
- **The rectiformers:** a potline's rectifiers are regulating transformers, with on-load taps and saturable reactors, each
  feeding a diode bridge. The units are phase-shifted so that four of them make 48 pulses. The line current is held by
  regulating them together (ABB, Siemens potline rectifier literature).
- **Casting:** standard primary ingots are **22.7 kg** (50 lb). A small ingot casting machine pours 10-15 t an hour.

A figure marked *(proposed)* is an estimate or a proposal.

## The balance for 40 pots *(proposed values, from the 10b3 plan)*
| Item | A pot | 40 pots, a day | How |
|---|---|---|---|
| Aluminium | **56.8 kg/h** | 54.5 t | 180 kA x 0.3356 kg/kA·h x 94 % |
| Alumina | 109.6 kg/h | 105 t | 1.93 kg a kg of aluminium |
| AlF3 | 1.14 kg/h | 1.1 t | 20 kg a t of aluminium with the fume treatment centre, 45 without |
| Net anode carbon | 23.3 kg/h | 22.4 t | 0.41 kg a kg of aluminium |
| Anode changes | one every 39 h | 25 | 18 anodes, each burning 910 kg net (1,213 kg less a 303 kg butt): 29 days each |
| Pot gas | 5,000 Nm³/h | | 200,000 Nm³/h for the line |
| DC power | 774 kW | 31 MW | 4.3 V x 180 kA, plus the bus |

## Values *(proposed)*
| Item | Value | Basis |
|---|---|---|
| Pot back-EMF | **1.65 V** | above |
| Pot at 180 kA | **4.0 V** across its cell (1.65 V plus 13.1 µΩ), **0.3 V** in its risers (1.67 µΩ): **4.3 V a pot** | AP18 class |
| Current efficiency | **94 %** | |
| An overdue anode | **2 points** off the current efficiency each, up to 18 | a stub burned through drops its current onto the others and the bath gets hot |
| No AlF3 | the current efficiency falls by **4 points** while the AlF3 bin is empty | the bath's ratio drifts and it runs hot |
| Anode effect | the pot reads **30 V**: its back-EMF rises to 27.65 V. It makes no metal and burns no carbon, and it makes **0.195 kg of CF4 a minute** | 0.143 kg/t x 1.363 t a pot-day |
| Anode effect clears | **2 minutes** after alumina is back in its bin | point feeders restore the bath and the gas film breaks |
| Freeze | after **4 h** below **18 kA** (10 % of 180 kA) | a pot holds its heat for some hours |
| Metal it can hold | **2,726 kg** over its pad, 2 days of metal | |
| Alumina bin | 2,000 kg, two bags | |
| AlF3 bin | 1,000 kg, one bag | |
| Rectiformer | **60 kA**, up to **220 V** (13.2 MW DC). **98.5 %** efficient at PF 0.95: **14.1 MVA**. On 138 kV | 40 pots x 4.3 V, plus the bus and one anode effect's 26 V |
| Rectiformer internal resistance | **37 µΩ** (1 % of 220 V at 60 kA) | as the 10b1 rectifier |
| Rectiformer ramp | **22 V a second** | 10 % of its maximum, as the 10b1 rectifier |
| Line setpoint dial | 60, 90, 120, 150, **180** kA | |
| Rectiformer DC terminal joint | **0.2 µΩ** | a welded or clamped aluminium joint |
| Fume treatment centre | bag filter house and injection reactor. **200,000 Nm³/h** with both fans, **100,000** with one | |
| Main fan | **250 kW at 4.16 kV**, 100,000 Nm³/h | |
| Reacted alumina | a reacted bag for each **45,600 Nm³** of pot gas treated | 109.6 kg/h of alumina for 5,000 Nm³/h a pot |
| Tapping crucible | holds **8,000 kg** | a small vacuum crucible |
| Casting machine | **10 t/h**, **15 kW at 480 V**, ingots of **22.7 kg**; holds 8,000 kg | |

## Parts (cells W x H x L; each a true-size big part)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `rectiformer` | 8 x 7 x 6 | A potline rectiformer: the regulating transformer in its tank with its tap changer, saturable reactors and radiators, and the diode cubicles beside it.<br>• Its **138 kV** terminal is on the west face of its main cell (x 0, y 0, z 0).<br>• Its DC terminals are on the east face, z 2: **+ at y 2 over − at y 0**, as on the 10b1 rectifier. |
| `pot` | 6 x 6 x 12 | A 180 kA prebake pot, its 10.5 x 4 m shell set across the line. The part's width is the 6 m pot pitch, so pots in a row touch. It has a superstructure with anode beam and hoods, and its stretch of the row's main gas duct on top.<br>• Its anode (+) riser is on the west face at y 1, z 5.<br>• Its cathode (−) collector bars are on the east face at y 1, z 5.<br>• Its duct ends are on the west and east faces at y 5, z 5. |
| `fume_treatment_centre` | 8 x 12 x 16 | A dry scrubber: the alumina injection reactor and the bag filter house.<br>• The pot gas comes in at the west face of its bottom cells (z 0).<br>• Fresh alumina bags go in by a hopper on top.<br>• Reacted alumina bags go out to a hopper under it, or to an empty hand.<br>• The clean gas goes up its stack. |
| `ftc_fan` | 4 x 5 x 6 | One of the centre's two main exhaust fans: **250 kW at 4.16 kV**, its box on the west face of its main cell. Set it against the centre. |
| `casting_machine` | 3 x 3 x 12 | An ingot casting machine: a tilting pour box over a chain of moulds, with a water spray and a stacker. **15 kW at 480 V**, its box on the west face of its main cell.<br>• It holds what the crucible pours.<br>• Ingots go out to a hopper under it, or to an empty hand. |
| `dc_bus_180k` | 1 x 1 x 1 | 180 kA aluminium bus (dc-bus.md) |
| `dc_bus_180k_copper` | 1 x 1 x 1 | 180 kA copper bus (dc-bus.md) |

### The pot (drawn facing north; x east, z south; the main cell at the north-west bottom corner)
Layers, bottom (y 0) to top (y 5). The whole box is the part. It shows where its terminals and hopper cells lie:

```
y 0, 1 (the shell, cathode and bath)   y 2, 3 (anode beam and hoods)   y 4 (superstructure)   y 5 (main duct, hopper deck)
x 012345                               x 012345                        x 012345               x 012345
z 0  ######                            ######                          ######                 ######
...                                    ...                             ...                    ...
z 5  +####-   (y 1)                    ######                          ######                 D####D
...                                    ...                             ...                    ...
z 11 ######                            ######                          ######                 ######
```
Legend:
- `#`: the part (`wildcatter:pot`).
- `+`: the anode riser (the + terminal, west face).
- `-`: the cathode collector (the − terminal, east face).
- `D`: a duct end (west and east faces).
- **Hopper on top:** any top cell, to the main cell's bins.
- **Hopper below:** under any bottom cell, for the spent anodes.

**Pots in a row:**
- Two pots set side by side, both facing the same way, at the same z, put one's − face against the next one's + face. They join
  through the risers, with 1.67 µΩ between them, and their ducts join too.
- A row turned to face south runs the other way, so a crossover bus joins the end of one row to the start of the next.
- A row's first and last pots need bus to their free terminals.

## How the parts link
- **138 kV:** a rectiformer's terminal takes 138 kV three-phase cable, such as the GSU 60's high side. It draws the DC watts of
  its last solve / 0.985, at PF 0.95.
- **The rectifier yard:**
  - rectiformers set side by side (touching) form one yard, and their DC terminals are joined inside;
  - bus may leave from any of them;
  - the yard holds **one line setpoint**: sneak and right-click on any unit steps the dial for the whole yard;
  - each unit online takes **the setpoint / the number online, at most 60 kA**: with 4 online, 45 kA each; when one trips,
    the other three carry 60 kA each; with two left, the line falls to 120 kA;
  - a unit is online when it is not tripped and its 138 kV is present.
- **DC:** + from the yard, through bus to the first pot's + face, along the row, crossover to the next row, and back from the
  last pot's − face to the yard's −. The pots carry whatever the yard drives. A pot is never opened, except a frozen one.
- **Pot gas:**
  - the pots in a row pass their gas along their joined ducts;
  - a pipe from a duct end at either end of the row carries the row's **POT_GAS** to the fume treatment centre's inlet;
  - a row whose gas has nowhere to go vents, and its pots use 45 kg of AlF3 a t;
  - a row with its gas going away uses 20 kg.
- **The main fans** are 250 kW motors: start them through a 4.16 kV soft starter or drive (10a), off the smelter's 138 kV by a
  step-down, as a smelter's auxiliaries are fed. A small diesel set cannot start one.
- **The fume treatment centre** runs while a fan touching it runs. It takes up to 100,000 Nm³/h a fan running and converts fresh
  alumina bags into reacted ones as it treats the gas. The pots take reacted bags first.
- **Metal:**
  - right-click a pot with the **tapping crucible** to siphon its metal into the crucible, up to 8 t;
  - right-click the casting machine with it to pour it all in, as far as the machine has room.

## How it runs
1. **A pot each tick:**
   - **What it makes:** with current I, it makes aluminium at I x 0.3356 kg/kA·h x its current efficiency. For that it takes
     1.93 kg of alumina a kg, the AlF3 rate a kg, and burns 0.41 kg of carbon a kg.
   - **Its bins:** it opens a bag when its bin has room for one.
   - **Short supply:**
     - with no AlF3, it runs on, 4 points down;
     - with its alumina bin empty and no alumina bag in its hopper, it goes to an **ANODE EFFECT**.
2. **Anodes:**
   - each 910 kg of carbon burned, an anode is due;
   - if a **rodded anode** is in its hopper, the pot changes it at once: the new anode goes in, and a **spent anode** goes out
     below;
   - if none is there, the due anode counts as **overdue**, and the pot reads ANODE CHANGE OVERDUE (n);
   - a rodded anode put in later is changed first for an overdue one;
   - a new pot starts with 18 anodes of staggered ages, the next due in 39 h. This stands in for the pot tending machine until
     10c's cranes.
3. **Anode effect:**
   - the pot's back-EMF goes to 27.65 V, so it reads about 30 V at the line current;
   - it makes no metal, burns no carbon and counts CF4;
   - the rectifiers ramp up to hold the line current;
   - it clears 2 minutes after alumina is back.
4. **Full:** at 2,726 kg of metal it reads **FULL: TAP IT** and makes no metal. It still carries the line current; a pot in
   series cannot open.
5. **Freeze:**
   - below 18 kA its clock runs, and it reads NO CURRENT: FREEZES IN 3 h 12 min;
   - at 4 h the bath is solid. It reads **FROZEN** and its circuit opens, so the line stops until it is cut out;
   - to cut it out, break it and close the gap with bus bars across its 6 m, at its terminal height;
   - breaking a frozen pot drops **spent pot lining** instead of the pot;
   - current back before 4 h stops and resets the clock.
6. **New pot:** a placed pot starts hot and filled, with its bath and pad (the start-up is waived). Its days-long bake-in is
   not modelled.
7. **Rectiformers:**
   - they regulate as the 10b1 rectifier does, each toward its share of the setpoint;
   - DC overcurrent trips one at 1.25 x its share for 1 s, or at once on a bolted short;
   - the ground-fault detector is on every unit.

## Controls and readouts
- **Rectiformer:**
  - "Rectiformer (138 kV in): DC 45 V, 45.0 kA of the line's 180 kA (4 online), ground OK";
  - "TRIPPED (DC overcurrent ...): right-click to reset".
- **Pot:**
  - "Pot: 4.00 V, 180 kA, 94 %: 56.8 kg/h of aluminium; metal 1,200 of 2,726 kg; alumina 1,840 kg, AlF3 640 kg; 18 anodes, next
    change in 21 h; gas to the duct";
  - ANODE EFFECT (30 V, CF4 1.2 kg), ANODE CHANGE OVERDUE (2), FULL: TAP IT, NO CURRENT: FREEZES IN ..., FROZEN, NO ALUMINA,
    NO AlF3, VENTING (no duct).
- **Fume treatment centre:** "Fume treatment centre: 50,000 Nm³/h of pot gas, 1 fan of 2; 4 fresh bags; next reacted bag
  71 %"; NO FAN, NO ALUMINA, FULL.
- **Casting machine:** "Casting machine: 3,200 kg of metal, 10 t/h; 141 ingots cast"; NO METAL, FULL: take the ingots.
- **Tapping crucible (tooltip):** "4,100 kg of aluminium of 8,000 kg".

## Items
- **Aluminium ingot:** 22.7 kg. An **aluminium block** is 9 ingots.
- **Reacted alumina bag:** 1,000 kg. It stacks to 16.
- **Pot:** the part's item. Crafted from iron blocks (the shell), bricks (the refractory) and calcined coke bags (the
  cathode blocks, standing in for graphitized cathode carbon).
- **Spent pot lining:** what a frozen pot leaves. It has no use yet.
- **Tapping crucible:** it stacks to 1 and holds up to 8,000 kg of aluminium.
- **Recipes** *(proposed)*:
  - the 180 kA aluminium bar from 7 aluminium blocks over 2 bricks;
  - the copper bar from 7 copper blocks over 2 bricks;
  - an aluminium anode rod from 2 aluminium ingots over a steel ingot (the 10b3a copper recipe stays).

## Rotation, placing, removal
- Every part places facing the player, as the other big parts do.
- Breaking a pot opens the line there. Breaking a rectiformer leaves the yard with one unit fewer.
- The pot's metal, bins and anodes are lost when it is broken.

## Added in the plant-crane-gaps wave (2026-11-09; `docs/questions/plant-crane-gaps.md`, 8 and 9)
- **Pot auxiliaries:** 3 kW (*proposed*) at the world's industrial voltage on the front face of each pot's main cell (the pot control
  cabinet). Without it the point feeders stop: the pot runs on the 60 kg (*proposed*) of alumina dissolved in its bath, then has an
  anode effect. Readout: `NO AUX POWER`.
- **Cutting a pot out live:** sneak-right-click puts its shorting bars in (1 µΩ, *proposed*); the line's current goes past it. A
  frozen pot can be cut out, and stays so; breaking it still leaves spent pot lining.

## Left out (Known gaps)
- Potroom magnetics, heat balance, bath chemistry (ratio, superheat), and cathode life (a pot is relined after 5-7 years).
- **The pot tending machine:** the pot changes its own anodes until 10c's cranes.
- **The crucible transport vehicle:** the tapping crucible item stands in for it. Cooling in the crucible is not modelled.
- **Spent pot lining** has no treatment yet.
- **C2F6** is not counted, only CF4.

## Game test
`potline` (z near −6300):
- **Power:**
  - the aeroderivative through its GSU 60 to 138 kV, to a yard of 4 rectiformers;
  - off the 138 kV run, a 4.16/138 kV step-down and a 4.16 kV soft starter for a main fan;
  - a diesel set's 480 V for the casting machine.
- **Line:** 10 pots in two rows of 5, facing north and south, with a crossover bus and the bus runs back to the yard. That is
  about 45 V and 8 MW.
- **Checks:**
  1. The yard holds **180 kA** at about 45 V, 45 kA a unit. The pots make metal.
  2. Row 1's duct goes to the fume treatment centre. The centre, given almost a bag's worth of gas, drops a reacted alumina bag.
  3. A pot seeded with an anode due and a rodded anode in its hopper drops a spent anode.
  4. A pot seeded nearly full is tapped with the crucible, which is then poured into the casting machine, and ingots drop.
  5. **A unit trips:** one rectiformer is tripped, and the line holds 180 kA on three, 60 kA each.
  6. **An anode effect:** a pot with its alumina taken out reads ANODE EFFECT at about 30 V, and the line still holds 180 kA.
     Alumina back, it clears.
  7. **A freeze:** a pot set off the line, with its clock seeded to almost 4 h, reads FROZEN, and breaking it drops spent pot
     lining.
- **Screenshot:** the potroom.
