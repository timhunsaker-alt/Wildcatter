# Ores and worldgen (APPROVED 2026-11-01 with the Phase 7a plan)

Phase 7 starts with rock. Tim asked for "all ore types from real world with exaggerated numbers": every ore that matters to
industry and that vanilla doesn't already have. Each ore is one block drawn as its mineral in its real host rock, found where
the real ore is found. Mining it gives its mineral (Fortune gives more; Silk Touch gives the block).

Vanilla already has coal, iron, copper, gold, redstone, lapis, diamond, emerald and quartz, so those are not repeated. Cadmium
and indium are not mined in the real world: they are by-products of refining zinc, so they come from sphalerite later.

## Grades
The game grade is about **ten times a typical mined grade, capped at 80 % of the ore mineral**. Ores already richer than that
keep their real grade. A block is 1 m3 of rock at its host's density. The list lives in `ore/Ore.java`, and
`tools/make_assets.py` reads its ids and names from there.

| Ore block | Mineral | Real grade | Game grade (per block) | Real setting |
|---|---|---|---|---|
| Uranium Ore | uraninite UO2 | 0.05-0.4 % U3O8 | 2 % U3O8 (46 kg) | roll-front sandstone, Powder River Basin |
| Pitchblende | uraninite | 7-20 % U3O8 (McArthur River 6.9 %) | 60 % U3O8 (1,500 kg) | Athabasca unconformity |
| Native Sulfur | S | 20-40 % (textbook) | 80 % | salt-dome caprock, Gulf Coast |
| Rock Salt | halite NaCl | 95-99 % | 98 % (2,117 kg) | Goderich, Avery Island |
| Potash | sylvite KCl | 20-30 % K2O | 80 % KCl | Saskatchewan |
| Fluorspar | fluorite CaF2 | 20-60 % | 80 % | Illinois-Kentucky |
| Heavy Mineral Sand | zircon, ilmenite, rutile, monazite | 2-10 % heavy minerals | 50 % (of those: 25 % zircon, 55 % ilmenite, 5 % rutile, 3 % monazite) | Jacinth-Ambrosia, Richards Bay |
| Bauxite | gibbsite | 40-60 % Al2O3 | 80 % gibbsite | Weipa, Boke |
| Galena Ore | PbS | about 2 % Pb | 23 % PbS | Mississippi Valley type, Viburnum Trend |
| Sphalerite Ore | ZnS | about 6 % Zn | 80 % ZnS | Tri-State, Pine Point |
| Tin Ore | cassiterite SnO2 | 0.5-1.5 % Sn | 13 % SnO2 | Cornwall |
| Nickel Ore | pentlandite | about 2 % Ni | 60 % nickel sulfides | Sudbury |
| Chromitite | chromite FeCr2O4 | 40-46 % Cr2O3 | 80 % | Bushveld |
| Platinum Reef | Pt-Pd sulfides | 6.3 g/t (Pt+Pd+Rh+Au) | 63 g/t | Merensky Reef |
| Molybdenum Ore | molybdenite MoS2 | 0.2-0.5 % MoS2 | 3 % | Climax, Henderson |
| Magnesite | MgCO3 | about 90 % | 90 % | Haicheng |
| Dolomite | CaMg(CO3)2 | about 95 % | 95 % | platform carbonates |
| Graphite Schist | flake graphite | 5-20 % C | 80 % | Balama |
| Borax Ore | borax | about 25 % B2O3 (textbook) | 80 % | Boron, California |
| Spodumene Pegmatite | spodumene | 1-2 % Li2O | 80 % | Greenbushes, Kings Mountain |
| Barite Ore | BaSO4 | 50-90 % | 90 % | Battle Mountain |
| Bentonite | montmorillonite | over 80 % | 85 % | Black Hills |
| Silver Ore | acanthite Ag2S | 100-1,000 g/t Ag | 3,000 g/t | Comstock, Potosi |
| Tungsten Ore | scheelite CaWO4 | 0.3-1 % WO3 | 6.5 % WO3 | Pine Creek |
| Cobalt Ore | cobaltite CoAsS | 0.1-0.5 % Co | 9 % | Cobalt, Ontario |
| Manganese Ore | pyrolusite MnO2 | 30-48 % Mn (textbook) | 80 % | Kalahari |
| Phosphate Rock | francolite | 18-40 % P2O5, 70-200 ppm U | 80 %, 1,300 ppm U | Bone Valley, Khouribga |

"Textbook" marks a value from standard geology references that the research could not check against a source.

**Sources:** WNA (uranium mining, ISL, phosphates); USGS deposit models (MVT OFR 2009-1213, heavy-mineral sands SIR 2010-5070-L);
ISGS Circular 604 (fluorspar); Freeport 10-K (Climax, Henderson); Implats 2020 fact sheet (Merensky); Geology Ontario MDI
(Sudbury); mining-technology.com (Jacinth-Ambrosia); USGS Mineral Commodity Summaries.

## Where they generate (`data/wildcatter/worldgen`, written by `tools/make_assets.py`)
Ordinary veins (vanilla `minecraft:ore` features). Count is veins per chunk; "1 in N" is one vein in every N chunks.

| Ore | Vein size | Count | y | Biomes | Replaces |
|---|---|---|---|---|---|
| Uranium Ore | 8 | 6 | 0 to 64 | desert, badlands | stone, sandstone |
| Pitchblende | 4 | 1 in 2 | -64 to -40 | overworld | stone, deepslate |
| Fluorspar | 8 | 4 | -16 to 48 | overworld | stone |
| Heavy Mineral Sand | 20 | 4 | 55 to 70 | beaches | sand, sandstone |
| Bauxite | 20 | 4 | 55 to 80 | jungle, savanna | stone, dirt |
| Galena / Sphalerite | 9 | 4 / 5 | -16 to 64 | overworld | stone |
| Tin Ore | 7 | 6 | 0 to 80 | mountains | stone |
| Nickel Ore | 9 | 3 | -64 to -16 | overworld | deepslate |
| Molybdenum Ore | 16 | 2 | 0 to 80 | mountains | stone |
| Magnesite | 12 | 2 | 0 to 64 | overworld | stone |
| Dolomite | 48 | 1 | 0 to 100 | overworld | stone |
| Graphite Schist | 8 | 4 | -32 to 32 | overworld | stone |
| Borax Ore | 16 | 3 | 50 to 80 | desert, badlands | stone, sand, terracotta |
| Spodumene / Barite | 10 | 3 | 0 to 64 / 0 to 48 | overworld | stone |
| Bentonite | 16 | 2 | 40 to 70 | plains, badlands | stone, dirt, terracotta |
| Silver Ore | 6 | 4 | -16 to 48 | mountains, badlands | stone |
| Tungsten / Cobalt Ore | 7 / 6 | 3 | -16 to 48 / -32 to 32 | overworld | stone |
| Manganese Ore | 12 | 2 | 0 to 48 | overworld | stone |
| Phosphate Rock | 14 | 3 | 30 to 60 | ocean, beaches | stone, sand, gravel |

Three ores need their own shapes:
- **Salt domes** (`wildcatter:salt_dome`, about 1 in 150 chunks in plains, swamp, mangrove and beach):
  - A round column of rock salt 8 to 14 blocks across, a little wider low down, rises from y -60 to a crest at y 0 to 20.
  - On top is a 4-block calcite caprock, one block wider than the salt; about 30 % of its blocks are native sulfur.
  - Real Gulf Coast domes trap oil on their flanks; that link waits for a later wave.
- **Evaporite beds** (`wildcatter:layered`, 1 in 6 chunks under desert and badlands, y -32 to 0):
  - A disc 16 to 28 blocks across: 3 blocks of rock salt, 1 of potash, then 2 more of rock salt.
- **The Bushveld reef** (`wildcatter:layered`, 1 in 8 chunks, y -64 to -38):
  - Two chromitite seams with a parting of country rock, two blocks of country rock, then the 1-block platinum reef.

Worldgen reaches only chunks that have not been generated yet. An old world gets the ores only in new chunks.

## Smelting
The furnace (and the blast furnace, twice as fast) smelts only what really smelts simply:

| Mineral | Ingot |
|---|---|
| galena | lead |
| cassiterite | tin |
| sphalerite | zinc |
| acanthite | silver |
| magnesite | magnesia (MgO), the uranium mill's precipitant: calcining, added in 7b |

Every other mineral waits for its plant:
- 7b: sulfur, uranium ore, pitchblende, pyrolusite and rock salt (uranium-mill.md, acid-plant.md).
- 7d: fluorspar.
- 7f: heavy mineral sand and dolomite.
- Phase 8: borax and silver.
- The rest go to the plants listed under Ideas in ROADMAP.

## Scintillometer
A handheld gamma scintillometer finds uranium.
- **Reading:** a count rate in counts per second (cps). Background is 50 cps (proposed). Each block holding uranium within 16 blocks adds 50,000 cps × its U3O8 fraction ÷ distance² (m, at least 1):
  - pitchblende at 1 m: 30,000 cps
  - uranium ore at 1 m: 1,000 cps
  - phosphate rock at 1 m: about 77 cps
- **Direction:** the meter says which way the count rises: a compass direction, or above or below you.
- **Range:** real gamma gets through only about 0.3 m of rock. The 16-block range is exaggerated, as the seismograph's 96 is.
- **Crafting:** iron, glass, redstone and an amethyst shard (the crystal stands in for the real detector crystal).

Other ores are found by digging, as in vanilla.

## Game test (`ores` scene)
- Each feature is placed with `/place feature` into a stone box, and the box must then hold its ore. The salt dome must have rock salt in its column and sulfur in its cap.
- Breaking each ore block drops its mineral.
- A furnace turns galena into a lead ingot.
- The scintillometer reads background over barren stone, and above background next to a pitchblende pod. The direction it gives points at the pod.
- Screenshots: a wall of all 27 ore blocks, and a cut through a salt dome.
