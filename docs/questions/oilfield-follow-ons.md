# Oilfield follow-ons (2026-11-19): questions on the spec

Spec: `docs/multiblocks/oilfield-follow-ons.md` (APPROVED by Tim 2026-11-19). Format as in `README.md` here. Only real design
choices are here; everything else in the spec is decided on the best recommendation and says so. The recommended option is
**Chosen**.

**Answered by Tim 2026-11-19:** every entry, each with the recommended (Chosen) option. Built as Phase 23 (23a-23d) on branch
`w-oilfield`. Decisions taken while building are numbered on from 9 at the end.

### 1. Cavern pressure
- **Chosen:** the real window, **0.3 to 0.85 psi/ft at the casing shoe**, taken **as if the cavern were at a real storage depth of
  1,000 m**: 68 to 193 bar, about 125 Sm3 of working gas a m3. A 2,000 m3 cavern holds about 250,000 Sm3 (9.5 TJ).
- **Why:** real Gulf Coast caverns are 600-1,800 m deep; a Minecraft dome's salt is 60-130 m down. At the game's depth a cavern would
  hold a tenth of the gas and sit under pipeline pressure, so the compressor would be there for show. Drilling keeps its pressures in
  equivalent mud weights, which do not depend on depth, so it is unaffected.
- **Alternatives:** the real gradient at the game's depth (about 7-20 bar): consistent with the small SIDPP readings, a tenth of the
  storage, and a compressor that is real equipment doing almost nothing.
- **Answered by Tim 2026-11-19:** the Chosen option.

### 2. Cavern breaches and sinkholes
- **Chosen:** a world rule **`cavernStability`**, on in new worlds and off in existing ones: leaching past the walls (2 blocks to the
  flank, 3 under the caprock) or holding gas under the minimum pressure for a game week breaches the cavern: flooded through the flank,
  or a sinkhole at the surface through the roof. The sonar reads WALL THIN, and surface cracks show for a game day first.
- **Why:** it is the real consequence (Bayou Corne, 2012) and the reason caverns are surveyed; with warnings it is a choice, not a trap.
- **Alternatives:** warnings only, leaching stopping at the limits and never breaching (safe, less to think about); leave breaches
  out entirely (the sonar only measures volume).
- **Answered by Tim 2026-11-19:** the Chosen option.

### 3. What caverns store
- **Chosen:** **gas only.**
- **Why:** gas storage is the Idea as written, it gives the sales line and gas plants a buffer, and it needs only the compressor and the
  dehydrator.
- **Alternatives:** gas and liquids: crude and LPG stored by brine displacement (as the strategic petroleum reserve in Gulf Coast
  domes), which needs a brine pond part and a second wellhead mode.
- **Answered by Tim 2026-11-19:** the Chosen option.

### 4. Frasch sulfur
- **Chosen:** **build it, with bleed water:** a hot water plant, a lift air compressor, a three-string wellhead and a pour vat; the
  water put down must leave by a disposal well within 16 blocks, or the rate falls to a third.
- **Why:** it is the real reason a dome's sulfur was worth anything, it ties to Spindletop's history the domes already tell, and the
  molten sulfur feeds the acid plant without digging. Bleed water is what made real Frasch fields work and costs only an existing part.
- **Alternatives:** build it without bleed water (simpler); leave it out: native sulfur stays mined as ore (four fewer parts).
- **Answered by Tim 2026-11-19:** the Chosen option.

### 5. Casing points
- **Chosen:** **SET CASING by the player** on the console, with casing joints and cement bags by hopper, and a **leak-off test** to read
  the shoe's fracture gradient. A deep field needs an intermediate shoe set within 12 blocks above it, or kill-weight mud breaks down
  the open hole (14.1 ppg against 14.0).
- **Why:** choosing the casing point from the DEEP ECHO and testing the shoe is the real decision the fracture gradient makes; it gives
  the choke operator's MAASP a meaning.
- **Alternatives:** the rig sets intermediate casing by itself when it nears a deep field (no new decision, fewer items); no casing at
  all, one fixed shoe FG for every rig (only the losses and the MAASP, no new step). Either is gated by `wellControl` as now.
- **Answered by Tim 2026-11-19:** the Chosen option.

### 6. Oil mud's cost
- **Chosen:** **diesel is lost with the mud** (about 64 L a metre drilled, 48 m3 to fill the system), and oily cuttings are **only
  counted**.
- **Why:** the diesel bill is oil mud's real cost and lands on the refinery; a cuttings plant would be one more part for a waste the
  game has nowhere to put.
- **Alternatives:** a cuttings dryer required to drill on oil mud (real, more parts); oil mud as water mud with no salt washout and
  nothing else (no CaCl2 unit, no package bag).
- **Answered by Tim 2026-11-19:** the Chosen option.

### 7. The choke kill
- **Chosen:** **required under a new world rule `chokeKill`**, on in new worlds and off in existing ones; off, the Phase 15 one-minute
  circulation stays. A PLC may run the choke.
- **Why:** a hand kill on schedule is the follow-on's point, and the rule keeps old worlds' rigs working as they did.
- **Alternatives:** optional: the one-minute kill stays beside it, the choke for players who want it; replace the old kill in every
  world.
- **Answered by Tim 2026-11-19:** the Chosen option.

### 8. A kick in oil mud
- **Chosen:** **it shows late:** a third of the pit gain until the influx is within 20 blocks of the surface, then all of it as the
  gas breaks out.
- **Why:** gas dissolves in oil mud; it is the real danger of oil mud and makes the flow check matter.
- **Alternatives:** a kick shows the same in oil and water mud (simpler, oil mud only a benefit).
- **Answered by Tim 2026-11-19:** the Chosen option.

## Decided while building (Phase 23, 2026-11-19), on the best recommendation

### 9. Small numbers the spec left open (23a)
- **Chosen:** every rig has its **surface casing at 30 m**; the open hole below it is the weakest point until intermediate casing is
  set. A kick's influx rises at **1 m/s** (the drilling circulation rate) for oil mud's late show. Under `chokeKill` a kick flows with its
  underbalance (0.0006 m3/s a psi, so Phase 15's 0.05 m3/s at about 84 psi), so a small underbalance gives a small kick.
- **Why:** surface casing is real on every well and gives the MAASP a depth before an intermediate string; a fixed kick rate whatever the
  underbalance made every kick as big as the worst.
- **Alternatives:** surface casing at a share of the hole's depth; the Phase 15 fixed rate kept.

### 10. SCR pressure (23b)
- **Chosen:** **150 psi at 30 spm** in a game hole (most of it the bit's nozzles); it goes with the square of the rate and with the
  string's mud weight, so FCP = 150 x KMW / OMW.
- **Why:** real SCR pressures of 400-800 psi come from kilometres of drill pipe; a 100 m hole has little but the bit.
- **Alternatives:** a real-size 500 psi (the schedule's numbers larger, its shape the same).

### 11. Oilwell cement's recipe (23a)
- **Chosen:** the spec's **gypsum-free variant only**: a quicklime bag and four clay balls make a bag of class H cement.
- **Why:** the mod has no phosphogypsum or gypsum item (the fertilizer plant only counts its phosphogypsum), and the spec allows the
  gypsum-free variant so casing does not wait for the fertilizer plant.
- **Alternatives:** a phosphogypsum item from the pan filter, and the spec's first recipe with it.

### 12. Which rule holds the fracture gradient (23a)
- **Chosen:** the shoe's fracture gradient, lost circulation and the underground blowout are under **`chokeKill`** with the choke kill,
  not `wellControl`.
- **Why:** `wellControl` is on in existing worlds that already have Phase 15; putting losses under it would change what their rigs do
  (13.8 ppg mud with no intermediate casing now loses circulation). The MAASP is what the choke operator works against, so the two
  belong together. SET CASING, the LOT, oil mud and the CaCl2 unit work in every world.
- **Alternatives:** a third rule `fractureGradient`; losses under `wellControl`.

### 13. The console's buttons and the rack (23a)
- **Chosen:** casing joints and cement bags go on a **rack in the substructure** (click them in, or a hopper on the rig floor). With no
  kick, **sneak-clicking the stopped console** is SET CASING when no intermediate string is set, then the LOT once 3 m of new hole is
  drilled; in a kick it is SHUT IN as before. One intermediate string a rig (liners and more strings are Left out).
- **Why:** the land rig has no pipe rack part; the console's sneak-click is already its second button.
- **Alternatives:** a pipe rack part beside the substructure; a screen on the console.

### 14. Switching and dumping the mud tanks (23a)
- **Chosen:** **sneak-click the mud tanks**: empty, they switch WATER / OIL MUD; with mud in them the first sneak-click asks and a second
  within 5 s DUMPs it to the reserve pit (lost, counted as diesel lost).
- **Why:** a dump throws away 48 m3 of diesel; a second click stops it happening by accident.
- **Alternatives:** a DUMP by a PLC point only.

### 15. The OBM package and the water in oil mud (23a)
- **Chosen:** the emulsion holds (**ES ok**) with at least **15 lb/bbl** of the package; under it the readout says ES LOW and the mud
  behaves as water mud (washes out salt, LOW GEL by its bentonite, no lubricity, the kick shows at once). On OIL MUD the water pump
  tops the water up only to 80/20 against the diesel; diesel comes off a steel pipe at the tanks at up to 20 L/s.
- **Why:** gives the package its job (real invert emulsions break without emulsifier) and keeps a pump from flooding the oil mud.
- **Alternatives:** the package optional; the OWR left to the player.

### 16. LCM, the dryer's second recipe and the shut-in shoe (23a)
- **Chosen:** an LCM bag pumped with no partial losses is screened out at the shakers (lost); the cure leaves the weakest point
  0.5 ppg stronger once. The bentonite dryer makes the OBM package when it has a quicklime bag (its second hopper slot) and ammonia on
  the north face of its front bottom cells; the barite mill grinds calcite or limestone to LCM, changing over only when empty. A kick
  whose shut-in pressure puts the shoe over 0.5 ppg past its FG is an **underground blowout** at once.
- **Why:** each follows the spec's rule for what the part does, with the smallest new behaviour.
- **Alternatives:** LCM kept in the mud; strengthening adding up bag by bag.

### 17. The kill model and the panel (23b)
- **Chosen:** a column model of the well (`well/KillModel`): mud segments in the string and annulus, one gas slug that rises with the
  circulation and expands (Boyle), the shut-in annulus a closed volume; back pressure from the choke equation for the pump's flow
  (1 psi at SCR full open, as (rate/opening)^2), and never under the shut-in pressure bled off as the choke opens to 10 %; drill pipe
  pressure 2 s per 1,000 ft late. The influx flows with the underbalance; **0.5 m3 of new influx in a kill is a second kick**: the rig
  closes the choke and stops the pump, and the kill sheet is worked again for the new SIDPP (the kill weight stays unless SIDPP asks for
  more). The panel is a screen (kill sheet, four gauges, the drill pipe chart against its schedule and casing pressure), the choke held
  open or closed with the mouse or O and C. **TOTAL losses with the well underbalanced** are the underground blowout in a kill (total
  losses alone only lose mud).
- **Why:** the smallest model that makes the real choke operator's job: casing pressure climbing as gas nears the surface, drill pipe
  pressure as the truth, a schedule that only holds while the choke is right.
- **Alternatives:** a scripted schedule with no hydraulics (simpler, but nothing to work); a full transient model with gas migration
  in a closed well (more real, more cost).

### 18. GAS ON THE FLOOR and the separator (23b)
- **Chosen:** gas reaching the surface goes to the separator, which offers it on its top face for a flare stack on its vent line to burn
  (vented cold at the top with no flare). With no separator, or with casing pressure over **200 psi while the choke opens faster than
  5 % a second**, it is GAS ON THE FLOOR; a fire, lava or lit campfire within 16 blocks ignites it as a blowout.
- **Why:** the spec's "a few hundred psi on a fast choke opening"; a fast opening is what blows a real liquid seal.
- **Alternatives:** a separator rating in gas flow.

### 19. Finding the parts (23b)
- **Chosen:** the rig finds the choke manifold by a steel pipe run touching its BOP stack and the manifold (within 12 blocks of the
  substructure, 21 of its main cell); the separator touching the manifold; the panel on the rig floor (standing on the substructure or
  within reach). The panel's hydraulic pump is a residential single-phase load; without power the choke moves at the manual 2 %/s.
- **Why:** the same ways the rig already finds its accumulator and the mud hopper its tanks.
- **Alternatives:** a choke line part.

### 20. Caverns: walls, blanket, debrining (23c)
- **Chosen:** the walls are read off the blocks: a rock salt block may be leached only if every block within 2 horizontally is salt (or
  the cavern, or the well's casing) and the 3 over it are; the well's own casing counts as wall. Without a blanket the water takes the
  highest salt block against the cavern first, with one the level ones; the blanket is diesel or nitrogen on a pipe at any other side of
  the brine wellhead (a trickle, 0.01 L/s). Debrining fills the brine's volume with gas at the cavern's minimum pressure (the cushion),
  so a cavern is DRY with its cushion in. Withdrawal falls off in proportion to the pressure over the minimum (full above half the
  window) and never goes under it, so the low-pressure breach of question 2 comes only from gas lost some other way. A brine well
  leached before 23c takes the water joined to its hole's bottom as its cavern.
- **Why:** works for a dome's salt and a placed column alike; real debrining fills the cavern with gas as the brine goes.
- **Alternatives:** the dome's geometry from `SaltDome` for the walls (only for grid domes).

### 21. The storage compressor in the scene (23c)
- **Chosen:** the `gas_storage` scene runs the 1 MW medium-voltage compressor by a test hook (`testDriven`): the scenes' 500 kW diesel
  set cannot start it, and a peaker and a 12.47/4.16 kV step-down is a scene of its own. The dehydrator runs on its own 480 V set.
- **Alternatives:** the peaker of the chemical scenes and a distribution step-down.
