# Phase 17c, zirconium extensions: decisions taken while Tim was away (2026-11-12)

Spec: `docs/multiblocks/zirconium-extensions.md` (DRAFT, decided on recommendation; its "As built" section lists what was built). The
spec's own choices are in `docs/questions/specs-wildcatter.md`, items 72-78. Format as in `docs/questions/README.md`. Each entry was
decided on recommendation; for Tim. Entries marked **(needs you)** change something outside this phase or pick between real options.

## 17c1: batches, scrap and hafnium

### 1. Batches and scrap losses are behind a world rule, `zirconiumBatches` (needs you)
- **Chosen:** a world rule `zirconiumBatches` (on in new worlds, off in worlds saved before) covers the Kroll reactor's two retorts, the
  VAR furnace's 100 kg ingots and the line's scrap losses (8 % at the press, 6 % at the mill, 2 % at inspection). Off, the line runs as
  the steady flow of 7f. Turned on in an old world (`/wildcatter rule zirconiumBatches true`), metal already held by the Kroll reactor,
  the vacuum distillation and the VAR furnace is kept and passes on as before; the first retort starts charging at once and is reduced
  when its 50 kg are in.
- **Why:** the brief's rule is that nothing changes a plant already built. The spec says old worlds simply start batching, but lumpy
  output and a 16 % loss would change an existing line's tube count. With the rule the spec's old-world behaviour holds once it is on.
- **Alternatives:** no rule, as the spec reads (every old line loses 16 % of its tube until scrap is recycled); two rules (batches,
  losses).

### 2. A retort is charged as the ZrCl4 comes, then waits out its 5 hours
- **Chosen:** the retort in the furnace takes ZrCl4 from the hafnium column at the line rate (with the fifth more every stage has) on the
  cell's magnesium; it is REDUCED when it holds 50 kg **and** 5 hours of furnace time (through `TimeScale.scaled`) have passed. Then it goes
  to the vacuum distillation when that is free, for 4 hours (scaled), while the other retort takes the furnace. Its sponge then goes into
  the distillation's hold as one 50 kg lump and its excess magnesium back to the cell.
- **Real counterpart:** in a Kroll retort ZrCl4 is sublimed onto molten magnesium over the reduction run (the feed is the reduction);
  the retort is then moved to a distillation furnace.
- **Why:** time scale (docs/time-scale.md) shortens waits, not rates; the ZrCl4 feed is a rate and stays real. At 1:1 the two agree (50
  kg at 12 kg/h is 4.2 h, inside the 5 h).
- **Alternatives:** charge the 50 kg at once (the column holds only 50 kg, so it would wait for the column to fill, then take it all).

### 3. The VAR arc stays at 20 kW (needs you)
- **Chosen:** the VAR furnace's arc supply keeps its 7f rating, 20 kW at 480 V; a melt takes an hour of time at whatever share of it the
  supply gives. It draws nothing while it charges an ingot (the electrode is pressed from sponge compacts and scrap off the arc).
- **Real counterpart and the conflict:** zirconium.md's 1 kWh a kg a melt at 100 kg an hour would be **100 kW** while melting (20 % of the
  time), averaging 20 kW. Raising the motor to 100 kW would change an existing world's load, so it was left.
- **Alternatives:** a 100 kW arc supply in new worlds only (a second rating behind the rule); melts of 5 hours at 20 kW.

### 4. Scrap: shares of the metal, bundled at the pilger mill
- **Chosen:** the press keeps 92 % of what it takes as hollows; the mill keeps 0.84/0.92 of its hollows as tube, so 84 % of the metal
  reaches a tube. The press's crop ends and the mill's losses go into the mill's bin; each 25 kg is a **Zircaloy scrap** item in a slot a
  hopper under the mill takes from (64 bundles, 1.6 t; past that the scrap is counted). The tube on the mill at a die change goes into
  the bin too (with the rule). The VAR furnace takes bundles by its top hopper and welds up to 30 kg into each 100 kg ingot at once
  (it is already Zircaloy-4; no tin, iron or chromium for it).
- **Alternatives:** the press bins its own crop ends into a hopper under it (two scrap hoppers).

### 5. Hafnium losses are counted, not binned
- **Chosen:** on a hafnium campaign the same 16 % is lost and counted as hafnium crop ends (at the press) and hafnium scrap (at the mill);
  there is no hafnium scrap item.
- **Why:** a campaign makes 50 kg a drum, so a few kg a campaign; a second scrap item and a second VAR hopper rule were not worth it.
- **Alternatives:** a hafnium scrap item the VAR furnace takes on a hafnium campaign.

### 6. HfCl4 drums come in every world, with or without the rule
- **Chosen:** the hafnium column drops a **HfCl4 drum** (50 kg of Hf, 79.7 kg of HfCl4) into a slot that a hopper under it takes from,
  for each 50 kg of hafnium it has counted; 16 wait there, then the rest is counted. A column in an existing world turns what it has
  already counted into drums.
- **Why:** the drum is an item only; nothing the plant does changes. 50 kg is about 245 hours of the line.
- **Alternatives:** drums only into a hopper actually under it (counted otherwise).

### 7. The hafnium campaign follows the metal down the line
- **Chosen:** a sneak-click on the Kroll reactor starts or ends a campaign. Each stage after it holds one metal at a time (saved as `hf`):
  it takes the other only once it is empty, and reads which metal it is holding. The Kroll reactor reduces HfCl4 + 2Mg -> Hf + 2MgCl2
  (0.272 kg of Mg reacted and 1.067 kg of MgCl2 a kg of Hf, charged 25 % over, as for zirconium). A retort keeps the metal it started on:
  a campaign begun while a retort holds zirconium starts with the next retort. The VAR furnace melts hafnium unalloyed, 100 kg an
  ingot (two drums). The pilger mill's **fourth die** rolls a **hafnium rodlet**: solid, 9.7 mm, 3.66 m, 3.6 kg (13.31 g/cm3). Hafnium
  hollows on another die read HAFNIUM HOLLOWS; the rodlet dies with zirconium hollows say they take hafnium only. Rodlets go to the
  anneal furnace's bins as tubes do.
- **Real counterpart:** hafnium is made by the same Kroll and VAR steps; hafnium control rods are solid bar.
- **Alternatives:** a campaign switch on every stage.

### 8. Strike item 2 (uses for the caustic) from Ideas
- **Chosen:** the caustic already goes to the Bayer refinery (10b2); nothing was built for it. ROADMAP Ideas no longer lists it.
