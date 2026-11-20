# Oilfield follow-ons (2026-11-19): questions on the spec

Spec: `docs/multiblocks/oilfield-follow-ons.md` (DRAFT, Wave B step 1, nothing built). Format as in `README.md` here. Only real
design choices are here; everything else in the spec is decided on the best recommendation and says so. The recommended option is
**Chosen**; nothing is built until Tim has gone through these.

### 1. Cavern pressure
- **Chosen:** the real window, **0.3 to 0.85 psi/ft at the casing shoe**, taken **as if the cavern were at a real storage depth of
  1,000 m**: 68 to 193 bar, about 125 Sm3 of working gas a m3. A 2,000 m3 cavern holds about 250,000 Sm3 (9.5 TJ).
- **Why:** real Gulf Coast caverns are 600-1,800 m deep; a Minecraft dome's salt is 60-130 m down. At the game's depth a cavern would
  hold a tenth of the gas and sit under pipeline pressure, so the compressor would be there for show. Drilling keeps its pressures in
  equivalent mud weights, which do not depend on depth, so it is unaffected.
- **Alternatives:** the real gradient at the game's depth (about 7-20 bar): consistent with the small SIDPP readings, a tenth of the
  storage, and a compressor that is real equipment doing almost nothing.

### 2. Cavern breaches and sinkholes
- **Chosen:** a world rule **`cavernStability`**, on in new worlds and off in existing ones: leaching past the walls (2 blocks to the
  flank, 3 under the caprock) or holding gas under the minimum pressure for a game week breaches the cavern: flooded through the flank,
  or a sinkhole at the surface through the roof. The sonar reads WALL THIN, and surface cracks show for a game day first.
- **Why:** it is the real consequence (Bayou Corne, 2012) and the reason caverns are surveyed; with warnings it is a choice, not a trap.
- **Alternatives:** warnings only, leaching stopping at the limits and never breaching (safe, less to think about); leave breaches
  out entirely (the sonar only measures volume).

### 3. What caverns store
- **Chosen:** **gas only.**
- **Why:** gas storage is the Idea as written, it gives the sales line and gas plants a buffer, and it needs only the compressor and the
  dehydrator.
- **Alternatives:** gas and liquids: crude and LPG stored by brine displacement (as the strategic petroleum reserve in Gulf Coast
  domes), which needs a brine pond part and a second wellhead mode.

### 4. Frasch sulfur
- **Chosen:** **build it, with bleed water:** a hot water plant, a lift air compressor, a three-string wellhead and a pour vat; the
  water put down must leave by a disposal well within 16 blocks, or the rate falls to a third.
- **Why:** it is the real reason a dome's sulfur was worth anything, it ties to Spindletop's history the domes already tell, and the
  molten sulfur feeds the acid plant without digging. Bleed water is what made real Frasch fields work and costs only an existing part.
- **Alternatives:** build it without bleed water (simpler); leave it out: native sulfur stays mined as ore (four fewer parts).

### 5. Casing points
- **Chosen:** **SET CASING by the player** on the console, with casing joints and cement bags by hopper, and a **leak-off test** to read
  the shoe's fracture gradient. A deep field needs an intermediate shoe set within 12 blocks above it, or kill-weight mud breaks down
  the open hole (14.1 ppg against 14.0).
- **Why:** choosing the casing point from the DEEP ECHO and testing the shoe is the real decision the fracture gradient makes; it gives
  the choke operator's MAASP a meaning.
- **Alternatives:** the rig sets intermediate casing by itself when it nears a deep field (no new decision, fewer items); no casing at
  all, one fixed shoe FG for every rig (only the losses and the MAASP, no new step). Either is gated by `wellControl` as now.

### 6. Oil mud's cost
- **Chosen:** **diesel is lost with the mud** (about 64 L a metre drilled, 48 m3 to fill the system), and oily cuttings are **only
  counted**.
- **Why:** the diesel bill is oil mud's real cost and lands on the refinery; a cuttings plant would be one more part for a waste the
  game has nowhere to put.
- **Alternatives:** a cuttings dryer required to drill on oil mud (real, more parts); oil mud as water mud with no salt washout and
  nothing else (no CaCl2 unit, no package bag).

### 7. The choke kill
- **Chosen:** **required under a new world rule `chokeKill`**, on in new worlds and off in existing ones; off, the Phase 15 one-minute
  circulation stays. A PLC may run the choke.
- **Why:** a hand kill on schedule is the follow-on's point, and the rule keeps old worlds' rigs working as they did.
- **Alternatives:** optional: the one-minute kill stays beside it, the choke for players who want it; replace the old kill in every
  world.

### 8. A kick in oil mud
- **Chosen:** **it shows late:** a third of the pit gain until the influx is within 20 blocks of the surface, then all of it as the
  gas breaks out.
- **Why:** gas dissolves in oil mud; it is the real danger of oil mud and makes the flow check matter.
- **Alternatives:** a kick shows the same in oil and water mud (simpler, oil mud only a benefit).
