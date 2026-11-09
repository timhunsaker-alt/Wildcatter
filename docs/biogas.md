# Biogas as an engine fuel

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **14c**. Decisions are in
> `docs/questions/specs-wildcatter.md` (items 8-11). No Wildcatter change until Tim approves (ROADMAP Ideas, 2026-11-07).

Farmings (the sister mod) proposes a manure **methane digester** whose biogas runs a Wildcatter gas genset (Farmings
`docs/waste.md`, Route B). This spec is the Wildcatter side: a **biogas** pipe product, the gas set burning it at a **derated
rating**, and a small, stable way for another mod to put biogas on Wildcatter's steel pipe. It is a system change, not a new
multiblock: the gas set (genset.md) is unchanged in its parts.

Sources:
- Caterpillar CG260 data sheets: the biogas rating assumes **60 % CH4** at a minimum LHV of **18.0 MJ/Nm3**; the CG260-12 gives
  **2,530 kWe on biogas against 3,000 kWe on natural gas** (84 %). The two ratings also assume different methane numbers, so this is
  not a pure heating-value derate ([CG260 data sheet](https://www.scribd.com/document/599282338/CG260)).
- Engine makers publish derate curves as multiplied factors for methane number, heating value, altitude and temperature
  ([industrial-monitor note](https://industrialmonitordirect.com/blogs/knowledgebase/gas-engine-derating-for-low-btu-fuel-gas-quality),
  a secondary source).
- Farmings `docs/waste.md`: dairy manure gives about **23 m3 of biogas a wet tonne**, about **48 kWh of electricity a tonne**
  ([Ontario OMAFA energy-yield table](https://www.ontario.ca/files/2025-09/omafa-energy-yields-farm-based-anaerobic-digestion-system-25-015-en-2025-06-04.pdf)).
- Methane's heating value, 35.8 MJ/m3 LHV and 39.8 MJ/m3 HHV *(textbook)*.

## The fuel
- **`BIOGAS`**, a new steel-pipe product (m3, a gas): **60 % methane, 40 % carbon dioxide**, already dried and stripped of hydrogen
  sulfide by whoever makes it (Farmings' gas conditioning). Wildcatter does not model H2S.
- **Heating value:** **23 MJ/m3** on the same basis as the mod's pipeline gas (38 MJ/m3): 60 % of it *(derived)*.
- **Who burns it:**
  - the **gas set** (genset.md), derated as below;
  - **fireboxes**, which take any gas, at 23 MJ/m3;
  - **not** the steam plant's burner, the gas turbines, the reformer or the dryers, which need pipeline gas (as today), nor the sales
    line (sales-gas.md: OFF SPEC: CO2).

## The gas set on biogas
| Item | Pipeline gas (today) | Biogas *(proposed)* | Basis |
|---|---|---|---|
| Engine rating | 1,000 kW | **850 kW** | CG260-12: 84 % on biogas |
| Alternator | 1,250 kVA, 0.8 PF | unchanged | the alternator does not care |
| Heat rate at full load | 9.5 MJ/kWh | **11.0 MJ/kWh** *(proposed)* | lean biogas burns less efficiently; this makes 23 m3 give 48 kWh, Farmings' OMAFA figure |
| Fuel at full load | 265 m3/h | **407 m3/h** | 850 kW x 11.0 / 23 |
| Fuel at no load | 15 % of full | 15 % of full: **61 m3/h** | genset.md's part-load line |

- The engine draws, in order, **separator gas, then pipeline gas, then biogas** from the runs on its cells. A run of steel pipe carries
  one product (Phase 5 rule), so a set on biogas has only biogas.
- **Derate:** the governor holds the set to 850 kW; asked for more, it droops as a set at its limit does. The readout reads
  **BIOGAS: 850 kW MAX**.
- **What a farm makes:** 5 t of manure a day (about a hundred cows) is 115 m3 a day, under 5 m3/h. That would idle a 1 MW set for
  about 5 minutes an hour. A farm digester holds its gas in a gas holder and runs the engine in bursts, which the set's redstone
  remote start (ats.md) already allows. Farmings' own numbers note one barn "feeds a few lights and fans, not a grid".
  A small biogas set is proposed as a follow-on (questions file).

## How another mod supplies it
- **The contract:** a block entity of the other mod implements `dev.wildcatter.block.FluidNode` and **offers `Product.BIOGAS`** on a
  port face. Wildcatter's steel pipe carries it; the gas set pulls it through `Lines.pull`, as it pulls separator gas from a separator.
- **Nothing else** is needed from Farmings: no Wildcatter code calls Farmings, and Wildcatter has no digester.
- **Versioning:** `Product.BIOGAS` is added in a minor release (v0.6.0 *proposed*). Farmings declares Wildcatter v0.6.0 or later.
- **The Power Meter** on a biogas pipe reads "biogas, 60 % CH4".

## Left out
- Upgrading biogas to biomethane (membrane or amine CO2 removal) for the sales line.
- Hydrogen sulfide, siloxanes and the engine damage they cause.
- Combined heat and power: a real farm set heats its digester from the jacket water. Farmings can model the digester's heat.

## Game tests
- `biogas`: a test node (a stand-in block that offers `BIOGAS` at a set rate, as the slurry tests' stand-in sources) on a steel pipe
  to a 1 MW gas set:
  - the set starts and carries a load bank of 1 MW, holding **850 kW** and reading BIOGAS: 850 kW MAX;
  - its fuel draw at 850 kW is **407 m3/h ± 5 %**;
  - a firebox on the same product burns it;
  - the steam plant burner on a biogas run reads NEEDS PIPELINE GAS.
- No screenshot: nothing new is drawn.

## Proposed wave
**14c**: one product, one engine rule, one test stand-in. Small; it can ride with 14a.
