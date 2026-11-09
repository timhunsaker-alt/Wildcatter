# Plant and crane gaps (2026-11-09): decisions taken while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back
... close all known gaps". This wave takes the Known gaps of the cranes (10c1-10c3), the Bayer, carbon and potline plants (10b1-10b3b)
and the oilfield and plant leftovers. Format as in `README.md` here. Built on branch `plant-crane-gaps`.

## Built

### 1. How hard does the wind blow, and when does a crane stop?
- **Chosen:** the world's weather sets the mean wind 10 m up: 4 m/s fair, 8 m/s in rain, 18 m/s in a thunderstorm (Beaufort 3, 5 and
  8), blended by the rain and thunder levels as the weather fades in. It rises with height by the 1/7 power law to the jib (tower
  crane, 63 m) or the boom head (crawler and ring). Limits: tower crane **20 m/s** (the 72 km/h of tower-crane.md), crawler
  **9 m/s** (the LR 1300 charts' usual EN 13000 reference wind; *proposed*), ring crane **14 m/s** (the SGC-250 brochure). Over its
  limit a crane starts no lift (`WIND: 21.5 m/s at its boom head, over its 9 m/s limit`) and holds one under way on its brakes.
- **Why:** the three limits are in the specs; Minecraft has no wind, so the weather is the only signal a player sees. With these
  figures rain stops a crawler on a long boom, never the tower or the ring crane, and a thunderstorm stops all three, much as a site
  supervisor would call it.
- **Alternatives:** a wind that varies by biome or at random gusts (no signal for the player to read); landing the load rather
  than holding it (a real crane lands it if it can; here the hold is the same as a power loss, which the cranes already do); the
  tower crane weathervaning (it would only show in the drawing).

### 2. What does the ground bear?
- **Chosen:** each block under a crawler's tracks or the ring's mats is read as a ground with IBC Table 1806.2's presumptive bearing,
  psf converted to t/m2: rock, concrete, bricks and crane rail **58.6** (12,000 psf); sandstone, cobble, terracotta and any other
  solid block **19.5** (4,000 psf, sedimentary rock or fill); gravel **14.6** (3,000 psf); sand **9.8**; dirt, grass, clay, mud and
  snow **7.3**; air, fluid or a block with no top **0**. The crawler needs **14.5 t/m2** (its 1.45 kg/cm2 with mats), the ring crane
  **30 t/m2** (its mats). Every cell must bear it, or it reads `SOFT GROUND: 81 of 81 m2 under its crawlers bear less than its
  14.5 t/m2 (dirt or clay, 7.3 t/m2): set it on gravel, rock or concrete` and takes no lift. The tower crane is not checked: its
  cross base stands on its own 112 t of ballast slabs, as on a designed foundation.
- **Why:** the specs give both pressures; a published table of allowable bearing turns blocks into soils honestly. Gravel just
  carries a crawler, as a crane pad of compacted stone does; the ring crane needs rock or a built pad, as Hinkley Point C's did.
- **Alternatives:** averaging over the footprint (one hole under a mat would pass); scaling pressure with load and slew (needs the
  real outrigger and track pressure charts, not published for the LR 1300's 104 t chart in a usable form).

### 3. What may a boom or jib not pass through?
- **Chosen:** before a lift starts the crane sweeps its jib (the tower crane: 61.2 to 63.1 m up, from the counter-jib's 22 m to the
  jib's 76.8 m tip) or its boom (from 2 m past its foot to its head, at the angle the radius needs) along both slews of the lift, a
  sample at least every metre at the tip. Any block with a collision shape in that path, or another tower crane's mast (1.15 m half
  width, plus 1 m) below the swept height, stops it: `BLOCKED: its boom would strike minecraft:obsidian at x, y, z as it slews the
  load`. It looks again every 5 s.
- **Why:** the gap said "at least stop a jib passing through blocks". Real cranes are planned so, and sites with several tower
  cranes use anti-collision systems; a mast is the one crane part that is always where it is drawn.
- **Alternatives:** slewing the long way round when the short way is blocked; jib against jib (needs both cranes' slews in time:
  left out, see 12); drawing the mast as blocks (it would then be broken by players and explosions).

### 4. How does the ring crane relocate?
- **Chosen:** a new block, **Crane Rail** (`crane_rail`: a heavy crane rail on its steel beam and timber mat, flush with the ground;
  six from three steel ingots over three planks). A row of it under the ring's slewing centre is its track. With nothing it can lift
  (NO SITE, or every lift OUT OF REACH), the crane looks along the rails for the stop that brings the nearest site with work left
  closest to **its chart's least radius plus 10 m** (50 m on the 118 m boom), and only if that is at least 2 m better than where it
  stands. It then skids along the rails at **10 m/h** (*proposed*: a hydraulic skid system's pace), burning diesel as its 600 kW
  slewing drives would, one metre at a time; a block in the new footprint stops it (`TRAVEL BLOCKED`), and the end of the rails ends
  the move. The whole crane, with its tank, boom and PLC settings, moves with it.
- **Why:** the SGC-250 is moved between lifting positions on rails without being dismantled (Sarens; Hinkley Point C). One row of
  rail keeps it cheap to lay; the rails count as rock for its 30 t/m2.
- **Alternatives:** two rails under the ring beams (twice the blocks, no change to play); a player command to travel (the crane
  already knows where its work is); the crawler crane's travel at 1.3 km/h (not built: it would need a path over the ground and
  its own bearing check on the move).

### 5. The cracker's pyrolysis gasoline and fuel oil
- **Chosen:** the quench tower's sump holds **2,000 L of each** (0.5 m of liquid in its 2 x 2 m foot), and when either is full it
  stops: `FULL: its sump is full; pipe its pyrolysis gasoline (east face) and fuel oil (west face) away`. Its nozzles were already
  there; before, past 20 L the products were lost.
- **Why:** a real quench tower must have its bottoms drawn off; hours of sump before it backs up leaves short runs and tests
  untouched.
- **Alternatives:** keep losing them (the gap); a pyrolysis gasoline product of its own (it is gasoline-range; the mod's gasoline
  stands in, as before).

### 6. The gas plant's stabilizer reboiler
- **Chosen:** the debutanizer's fired reboiler needs **450 kJ a litre of Y-grade** (*proposed*: the LPG overhead, 0.43 kg a litre,
  boiled up at 1.5 reflux at about 370 kJ/kg, plus sensible heat) at **80 %** efficiency: 281 kW at its full 0.5 L/s. It burns gas,
  LPG, coker gas or coke oven gas piped to the **west face of its base**; with none it burns its own LPG off the reflux drum (about
  2.8 % of the LPG). The dehydrator keeps its 0.5 %.
- **Why:** real NGL plants fire stabilizer reboilers on residue gas or a hot-oil system; drawing its own product when none is piped
  keeps every gas plant built before this running, with a reason to pipe fuel.
- **Alternatives:** stop with no fuel (would break every existing gas plant until fuel is piped); a hot-oil heater as a new part.

### 7. Coke oven gas and coker gas in engines and turbines
- **Chosen:** a gas engine set takes coke oven gas (18 MJ/m3) and coker gas (45 MJ/m3) on its gas line as well as natural gas,
  counted by heat against natural gas's 38 MJ/m3; its readout names the fuel. The gas turbines take both on their fuel nozzle.
- **Why:** lean-gas engines on coke oven gas (Jenbacher's special-gas engines) and heavy-duty gas turbines on steel-mill gases are
  common in coke and steel works.
- **Alternatives:** derating the engine on lean gas (real engines on coke oven gas are derated 10-20 %; left out, as the mod's engine
  has one rating); blocking the aeroderivative (real aeroderivatives need a cleaner, richer gas: left for Tim).

### 8. The pots' auxiliaries
- **Chosen:** each pot's auxiliaries (its pot controller, beam jacks, point feeders and crust breakers, and its share of the
  potroom's compressed air) take **3 kW** (*proposed*) at the world's industrial voltage, on the **front face of its main cell**.
  Without it the feeders stop: the pot runs on the alumina dissolved in its bath, **60 kg** (*proposed*: about 0.8 % of an 8 t bath
  above the anode-effect level, half an hour), then goes to an anode effect. Its readout says `NO AUX POWER`.
- **Why:** real pots are fed alumina by pneumatic point feeders under a pot controller on the potroom's AC; a lost auxiliary supply
  ends in anode effects across the line.
- **Alternatives:** draw the auxiliaries as a load on the rectiformer yard (no wiring, but no failure to see); one feed per row
  through the row's touching pots (fewer cables; a potroom's distribution does run along the row; can be added).

### 9. A frozen pot cut out live
- **Chosen:** sneak-right-click a pot to put its shorting bars in (1 µΩ, *proposed*: 0.18 V at 180 kA): the line's current goes past
  it and it makes nothing (`CUT OUT`). A frozen pot can be cut out and stays so; a running one can be cut out and back in. Broken, a
  frozen pot still leaves spent pot lining.
- **Why:** smelters take a pot out of a running line with shorting wedges or a cut-out switch, without stopping the line, to reline
  or after a freeze.
- **Alternatives:** lower the line current while cutting out (real practice on some lines; the yard's setpoint can be stepped down by
  hand first).

### 10. A hopper on the steam plant's burner
- **Chosen:** a hopper against any cell of the burner feeds it, not only one at its main cell.

## Planned, not built (narrowed in Known gaps)

### 11. Residence times as real holds (coking 18 h, drum fill 12-24 h, precipitation 36 h, digestion 1 h)
- **Chosen:** not built in this wave; this is for Tim to decide.
- **Why:** a real hold delays each plant's first product by its residence time in real time: a new coke oven battery's first coke
  18 h after its first charge, a coker's first cut after a 16 h fill, a Bayer plant's first alumina 37 h after start. At steady state
  the rates are the same as today. That changes how the game paces, which CLAUDE.md leaves to Tim.
- **The plan if wanted:** a plain-Java `Holdup` (a FIFO of time buckets, a minute wide for the digester, half an hour for the
  precipitators and ovens), saved with each part; feed in at the front, product out when its bucket comes due; the readout shows
  what is in process and the next product's hour. The coke drum switches on its fill time (16 h) instead of at a tonne, and its
  coke is cut out after steaming and quenching (about 8 h). Each game test gets an `age` hook to run the hold forward.
- **Alternatives:** scaled-down holds (minutes standing for hours: not real, and CLAUDE.md asks for real values).

### 12. The cranes' remaining gaps
- **Erection and assembly:** a tower crane erected by a mobile crane in sections (about 2 days), a crawler's boom assembled with an
  assist crane (hours), the ring crane over weeks. Plan: a site-marker blueprint of the crane itself, built by another crane, the
  last lift turning it into the working crane. Large; not built.
- **The crawler's luffing jib, superlift and derrick, and its travel:** the LR 1300's charts for these are not published in a usable
  form (crawler-crane.md); not built.
- **The ring crane's heavy-duty jib and 5,000 t hook arrangement:** as before, its chart is only partly published.
- **Jib against jib between tower cranes:** needs both slews in time (an anti-collision zoning system). Plan: each tower crane
  reserves the sector its lift sweeps at its jib height; another tower crane whose jib height is within 3 m waits for a sector that
  does not cross it.
- **A tandem lift turning the part; a world saved mid-lift:** unchanged.

### 13. Spent pot lining
- **Chosen:** planned, not built. SPL is a listed hazardous waste (US EPA K088). Real treatment: the Reynolds/Alcoa thermal process
  (a rotary kiln with limestone and sand, leaving an inert residue), Rio Tinto's LCL&L (leaching, recovering sodium fluoride), or
  co-firing in cement kilns as fuel and fluoride. The mod has none of these plants.
- **The plan:** an SPL treatment kiln (a rotary kiln on gas, 480 V drive) taking SPL and limestone, giving inert residue for the
  lined tailings cell and fluoride back to the AlF3 plant. Needs its own spec.
- **Alternatives:** burning SPL in the steam plant's burner (real boilers do not take it: it gives off fluoride and cyanide).

### 14. The blowout preventer stack
- **Chosen:** left to Phase 15b2 of `docs/multiblocks/drilling-mud.md` (DRAFT, not approved), as that spec places it.
- **Why:** a stack with no kicks, shut-in or accumulator is a model only; the spec makes the rig read NO BOP without it, which would
  change the land rig the moment it is built. It belongs with well control.

### 15. Drawing-only gaps
- **The land rig's cables from the drive house to its motors, the steam plant's main steam and feedwater lines between parts, the
  alternator's 1 m length (a real 1 MW frame is about 1.6 m), the brass swatch's tile line, the potroom building, pumpjack and rig
  collision on their moving parts:** rendering work, left for an art or model wave. Plan for the rig cables: cable trays drawn from
  the drive house's east end to each pump and the drawworks, as a part renderer on the drive house, no new blocks. Plan for the steam
  lines: a pipe renderer joining each part's nozzle to the next part's along the plant's layout.
- **Crane paint:** already Ludo art since the art wave (`mat_paint_crane_yellow`); nothing to do.

### 16. Potroom magnetics, heat balance, bath chemistry, cathode life, bake-in
- Very large; unchanged. A plan needs its own spec (MHD stability against the busbar layout, a pot heat balance with ledge freeze,
  ratio and alumina concentration, cathode wear to a pot age of about 2,000 days).

## Not reached in this wave
- 10c1's blueprint gaps (block contents and machine settings, the marker in its own corner, the ghost's coloured cubes and its range).
- 10b1's bus bar standoffs in the air; 10b2's liquor first fill, fixed stream shares and the ton containers' and chlorine's buyers;
  10b3a's metallurgical coke users and the by-product plant's ammonia, light oil and desulfurizing, and the vacuum unit; 10b3b's
  anode change by crane, the crucible's transport and cooling, start-up and C2F6.
