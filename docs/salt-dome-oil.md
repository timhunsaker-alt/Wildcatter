# Salt domes that trap oil on their flanks (worldgen)

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed phase **15a**. Decisions are in
> `docs/questions/specs-wildcatter.md` (items 14-17). A worldgen spec, like `docs/ores.md`; it has no multiblock.

The 7a salt domes (ores.md) are a column of rock salt under a calcite caprock with native sulfur. Real Gulf Coast domes are oil traps:
the rising salt bends the sand beds up around it and seals them against its impermeable flank, and the caprock itself can hold oil.
ores.md left the link "for a later wave". This is that wave.

Sources:
- Spindletop, Texas: the 1901 Lucas gusher came from the **caprock**, at about 1,000-1,139 ft; caprock wells on other domes were dry.
  In 1925 the McFaddin No. 2 found oil on the dome's **southeast flank** at about **5,400 ft**, and "in eight years the new field
  produced more oil than the caprock at Spindletop had produced in its entire existence"; the early flank wells were in Miocene sands
  that **pinch out against the salt wall**
  ([TSHA Handbook, *Spindletop Oilfield*](https://webarchive.library.unt.edu/web/20141113221107/http://www.tshaonline.org/handbook/online/articles/dos03)).
- Salt is impermeable, so the rock around a dome's perimeter is an ideal trap; the caprock, arched up as the dome grows, can itself be
  a reservoir; the rising salt faults the rock above it ([Wikipedia, *Salt dome*](https://en.wikipedia.org/wiki/Salt_dome)).
- Domes were found by the torsion balance and refraction seismic, from the 1920s *(textbook, unverified)*: salt is lighter and faster
  than the sediment around it.

## What changes in the world
### 1. Domes move onto a seed grid
Today a dome is a vanilla feature (about 1 in 150 chunks). A reservoir in Wildcatter depends **only on the world seed**
(`well/Reservoir.java`: "nothing is stored until a well starts to empty one"), so a flank field must know where its dome is from the
seed alone. Domes therefore move to a **seed grid of their own**, as the roll-fronts did (7c):
- **Grid:** cells of **192 x 192 blocks** (144 chunks); a dome in **60 %** of the cells whose centre biome is plains, swamp, mangrove
  or beach *(proposed: about the same density as now in those biomes)*.
- **Shape:** unchanged (ores.md): salt 8 to 14 blocks across, a little wider low down, from y -60 to a crest at y 0 to 20, the 4-block
  caprock one block wider, 30 % of it native sulfur.
- **Placement:** the dome's feature places itself where the grid says, in whichever chunks are being generated. Old chunks keep what
  they have.
- **Old domes** (placed by the old feature) keep their salt and sulfur and have no oil. A grid dome whose chunks were generated
  before this wave has its oil but no salt drawn *(proposed: harmless, and it keeps reservoirs a pure function of the seed)*.

### 2. Flank fields
Each grid dome gets **1 to 3 flank fields** *(proposed)*:
- **Shape:** an arc of a ring round the dome's axis: from the salt's edge out **4 to 12 blocks**, over **60 to 150 degrees**
  (sand beds sealed on one side of the dome, as Spindletop's southeast flank). Fields on one dome do not overlap.
- **Depth:** its top at **y -50 to -20**, deeper than the caprock and above the deep fields (land-rig.md, y -58 to -50), so the small
  rig (well-site.md) reaches the shallower ones.
- **Kind:** **80 % oil** *(proposed: Gulf Coast domes are oil provinces)*, the rest gas.
- **Size:** **150,000 to 400,000 units** of oil (a bucket is 1,000 units), up to twice a shallow field's 60,000 to 200,000, over a
  smaller area *(proposed: Spindletop's flank out-produced its caprock; domes are rich)*. Gas flank fields 300,000 to 800,000.

### 3. The caprock pool
Each grid dome has a **caprock pool** in **30 %** of cases *(proposed)*:
- in the caprock's calcite, under the crest: the dome's own footprint;
- **oil, 20,000 to 60,000 units**, small and shallow, at the caprock's depth;
- it flows at the **full-pressure rate for its first 10 % and then falls fast** *(proposed)*: the Lucas gusher's story, a big first
  week and a short life.

## Finding them
- **The seismograph** (Phase 0) reads a dome under it as **SALT DOME, crest at y N** (real: salt is fast and light; a refraction shot
  and a gravity low show it). It reads flank fields and caprock pools as it reads any field.
- **On the surface:** a low mound over each grid dome, 1 to 2 blocks high and the caprock's width *(proposed)*, as Spindletop Hill
  stood about 15 ft over the prairie *(unverified)*. It is drawn only in newly generated chunks.
- **A hole down the dome's middle** finds caprock, then salt, and no oil below the caprock pool.

## Drilling through salt
A hole on a flank field can pass through the salt's edge. With **drilling mud** (drilling-mud.md), salt needs a **salt-saturated
mud**, as real wells drilled through salt do, or the hole washes out (WASHOUT: the rig loses its hole). Until drilling mud is built,
the rig drills salt as it drills stone.

## Code that changes *(for the plan, not a design decision)*
- `well/Reservoir.java`: a second shape, an arc of a ring with its dome's centre, inner and outer radius and angles; `at(seed, x, z)`
  checks dome fields first.
- A new `well/SaltDome.java`: the grid, as `RollFront.java` is.
- The salt dome feature reads `SaltDome` for its position.
- `tools/make_assets.py`: the dome's placement moves from a vanilla count to the grid (both loaders' biome modifiers).
- `OreUses` and the JEI page for rock salt and native sulfur: "domes trap oil on their flanks".

## Left out
- **Frasch sulfur:** melting the caprock's sulfur in place with superheated water (it is mined as ore).
- **Brine wells and storage caverns** in the salt: the chlor-alkali plant (chlor-alkali-plant.md) takes rock salt by hopper; caverns for
  gas storage could follow the sales line.
- Faults round the dome splitting a flank field into blocks.

## Game tests
- `saltDome` (a forced test seed with a grid dome under plains, as `isrWellfield` forces its seed):
  - the seismograph over the dome reads SALT DOME and its crest;
  - the small rig drilling over a flank field's middle strikes oil, and a pumpjack lifts it;
  - a hole down the dome's centre finds no oil below the caprock;
  - where the seed gives a caprock pool, a well on the crest flows at full rate and falls to under half of it after its first 10 %
    (the field drained by hook);
  - the dome's feature placed in a stone box (`/place feature`, as the `ores` scene does) puts its salt where `SaltDome` says.
- **Screenshot:** a cut through a dome with a flank well beside it.

## Proposed wave
**15a**, with drilling mud (15b) after it, so salt can wash out.
