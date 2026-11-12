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

## 17c2: fumed silica, Pidgeon magnesium, chromium

### 9. Oxygen and hydrochloric acid on pipes
- **Chosen:** the burner takes `OXYGEN` and puts out `HYDROCHLORIC_ACID (32 %)`, the pipe products the titanium pigment and chlor-alkali
  track (w-20, Phase 20) added: on the merge (2026-11-12) this track's identical declarations were dropped for theirs, so the VPSA unit's
  93 % oxygen feeds the burner. The scene takes both gases from stand-in sources.

### 10. Where the fumed silica burner stands
- **Chosen:** against the chlorinator's east face, one cell south of its front, so the chlorine inlet keeps the front cell (the burner,
  3 long, would otherwise cover all three of the chlorinator's east bottom cells). Its 480 V box is on the **north** face of its main cell,
  since its west is against the chlorinator. Hydrogen on the east face of its bottom cells, oxygen one up, 32 % HCl out of the south face
  of its back cells, sacks out below.
- **Why:** the spec's "against its east face" with the box on the west face is not buildable as drawn.

### 11. The chlorinator keeps 20 kg of SiCl4 for a burner
- **Chosen:** the chlorinator keeps up to 20 kg of the SiCl4 it makes for a burner against it *(proposed)*; the rest, and all of it with no
  burner, is counted as before. The burner runs at the line's 18.6 kg/h and a fifth, as every stage does: 6.6 kg/h of fumed silica, 16
  kg/h of HCl (50 kg/h of 32 % acid), 4.9 Nm3/h of hydrogen and 2.45 of oxygen. It stops when its acid has nowhere to go. The water the
  acid is absorbed in is not drawn (simplified).

### 12. Fumed silica is tagged `c:fumed_silica`
- **Chosen:** 25 kg sacks, tagged `c:fumed_silica`; no use in Wildcatter (as the spec says).

### 13. The Pidgeon cycle: magnesium at its rate, ingots at the cycle's end
- **Chosen:** magnesium condenses at 10 kg/h while the furnace runs (dolime, FeSi, fluorspar and gas drawn at that rate); at the end of
  each 12-hour cycle (`TimeScale.scaled`) the crowns are cast into 22.7 kg ingots (the part of an ingot left over waits for the next
  cycle). At 50:1 the cycles are 14.4 minutes and give smaller casts; the rate stays 10 kg/h.
- **Why:** the time scale shortens waits, not rates (docs/time-scale.md). Charging a whole cycle's briquettes at once and releasing
  them in a scaled cycle would make the furnace 50 times faster at 50:1.
- **Alternatives:** a full 120 kg charge at the cycle's start (real retorts are charged at the start).

### 14. Pidgeon numbers
- **Chosen:** 11 t of dolomite a t of Mg (the middle of 10-12), as dolime 5.75 kg a kg of Mg (dolime 52.3 % of dolomite's weight);
  1.0 kg of FeSi 75 a kg; fluorspar 3 % of the briquette, 0.21 kg a kg (the fluorite block's 2,160 kg of CaF2, or a 1 t synthetic
  fluorspar bag); gas on the east face of its bottom cells (the spec names no nozzle), 180 MJ a kg; slag 6 t a t counted.

### 15. Dolime in the lime kiln
- **Chosen:** the lime kiln takes dolomite by the same hopper and burns it first while it has some, at its 10 t a day of product and its
  4.0 GJ a t *(proposed: the same as quicklime)*; CO2 0.913 kg a kg of dolime (the liquefier can take it). A bagging station against it
  fills dolime bags while it holds more dolime than quicklime.

### 16. The chromate kiln
- **Chosen:** a sack (25 kg of Cr2O3) an hour *(proposed; the VAR needs 10 g of Cr an hour)*. Chromite and sulfur by hopper; 32 % caustic
  soda on the north face of its front cells, gas on the east face of its bottom cells (the spec names no nozzles); 0.3 MW of gas.
  Stoichiometry: 4 FeCr2O4 + 16 NaOH + 7 O2 -> 8 Na2CrO4 + 2 Fe2O3 + 8 H2O (1.05 kg of NaOH a kg of Cr2O3); the leach and reduction
  simplified to Na2Cr2O7 + S -> Cr2O3 + Na2SO4 (0.21 kg of S, 0.93 kg of Na2SO4 counted a kg); 90 % of the chromite's chromium
  recovered *(proposed)*.
- **Hexavalent chromium:** it holds up to a sack's worth of chromate between roasting and reduction; broken while it holds any, it leaves
  a poison cloud (vanilla's lingering cloud, Poison II, 4 m, 10 minutes) *(proposed)*.
- **The sodium sulfate** stays counted (19d bags only the borax dissolver's).

### 17. Chromium metal from the aluminothermic vessel
- **Chosen:** a chromium batch is 4 sacks of chromic oxide (100 kg) and 2 aluminium ingots (45 kg; Cr2O3 + 2 Al -> 2 Cr + Al2O3 takes
  35.5 kg), fired as the vessel's other batches: 67 kg of chromium at its 98 %, broken into **10 kg pieces** *(proposed)* out to a hopper
  under the vessel (not a bulk bag: the VAR uses 10 g an hour). The VAR furnace takes a piece in its chromite slot (10 kg of Cr, about
  1,000 hours); chromite still works.
