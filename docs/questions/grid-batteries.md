# Grid batteries (2026-11-19): questions on the spec

Spec: `docs/multiblocks/grid-batteries.md` (APPROVED by Tim 2026-11-19). Format as in `README.md` here. Only real design
choices are here; everything else in the spec is decided on the best recommendation and says so. The recommended option is
**Chosen**.

**Answered by Tim 2026-11-19:** every entry, each with the recommended (Chosen) option. Built as Phase 22 (22a-22c) on branch
`spec-grid-batteries`.

### 1. Sizes
- **Chosen:** two sizes: a **125 kW / 261 kWh commercial cabinet** on the industrial LV tier, and a **2.5 MW / 10 MWh utility block**
  (two containers, a PCS, an MV skid) on distribution, up to four blocks under one plant controller.
- **Why:** both are real products in common use. The cabinet fits the 480 V bases a player has long before distribution (peak shaving
  beside a genset, a small island's black start); the block is the real utility unit and a 4-hour duration is the usual one.
- **Alternatives:** the utility block only (fewer parts, but nothing for a small base); also a 5 MW block of four containers on one
  larger PCS (real too, but the same play at twice the size).
- **Answered by Tim 2026-11-19:** the Chosen option.

### 2. Where the cells come from
- **Chosen:** an **LFP cell plant** of four new parts (graphite line, graphite furnace, LFP kiln, cell line) that makes 52 kWh
  modules, one every 10 minutes, from lithium carbonate, phosphoric acid, iron and graphite schist.
- **Why:** every other product in Wildcatter comes off a real plant; it gives graphite schist a use and puts a 1.4 MW furnace on the
  grid; a container's 3.1 t of lithium carbonate makes the lithium plant matter.
- **Alternatives:** a crafting-table recipe for a module from the same bags (no new plant, much faster to build, far less real);
  both: the crafting recipe for the cabinet's five modules, the plant for containers.
- **Answered by Tim 2026-11-19:** the Chosen option.

### 3. Chemistry
- **Chosen:** **LFP only.**
- **Why:** it is what grid storage is built from now, its inputs are all in the mod (lithium carbonate, phosphoric acid, iron,
  graphite), and it is the safer chemistry.
- **Alternatives:** LFP and NMC (nickel from the smelter, cobalt from cobaltite: gives cobaltite its first use and a denser, hotter,
  more fire-prone container); LFP and sodium-ion (rock salt as the source; no lithium bottleneck; lower energy).
- **Answered by Tim 2026-11-19:** the Chosen option.

### 4. Thermal runaway
- **Chosen:** a world rule **`bessThermalRunaway`**, on in new worlds and off in existing ones, with **no random defects**: only fire,
  explosion damage or an ungrounded lightning strike sets a container off. A container touching a burning one catches; one a block
  apart does not.
- **Why:** real cell failures are about 0.004 a GWh a year (11 % of 0.04): a player's plant would never see one, so a random roll would
  only feel arbitrary. Abuse is what sets real ones off, and spacing is what fire codes require.
- **Alternatives:** a rare random cell defect as well (more danger, less fair); the rule off by default in new worlds too.
- **Answered by Tim 2026-11-19:** the Chosen option.

### 5. Degradation
- **Chosen:** **real LFP cycle fade:** 20 % over 6,000 equivalent full cycles, faster when hot or charged fast; no calendar fade; worn
  containers renewed with new modules.
- **Why:** real, and it rewards keeping the chillers running and not abusing the rate; at a cycle a game day it takes 6,000 days
  (about 83 real days of play) to lose 20 %, so it never punishes normal play.
- **Alternatives:** ten times faster, so a player sees it inside a world's life (less real, more upkeep); none (simpler; SOH removed).
- **Answered by Tim 2026-11-19:** the Chosen option.

### 6. Grid-forming
- **Chosen:** **both modes from the start of the phase**, a switch on the plant controller and a PLC point: grid-following by default,
  grid-forming for islands and black start (wave 22c).
- **Why:** grid-forming is the new thing a battery brings to the grid: it can hold an island with no machine and bring a dead one up.
  Real sites choose the mode in the controller.
- **Alternatives:** grid-following only, with grid-forming as a later upgrade item for the PCS (a progression step); grid-forming only
  (one model, simpler code, but not how most real fleets run today).
- **Answered by Tim 2026-11-19:** the Chosen option.

### 7. Fault current on an inverter island
- **Chosen:** **the real limit, 1.2 x rated:** on an island fed only by inverters, relays must be set below that or they never trip,
  and the block trips on undervoltage after 1 s.
- **Why:** it is the real protection problem of inverter grids, and the mod's relays already have settable pickups and show the fault
  current they see.
- **Alternatives:** give a grid-forming block a machine's 5-6 x fault current, so protection set for gensets keeps working (easier,
  not real).
- **Answered by Tim 2026-11-19:** the Chosen option.

### 8. Where in the progression
- **Chosen:** **after the lithium plant** (Phase 20c) and the fertilizer plant's phosphoric acid (18a): late game, a new quest chapter
  after `chemicals`.
- **Why:** that is where its materials are; and storage is most useful once a player has a grid worth stabilising.
- **Alternatives:** earlier, with the cabinet's cells crafted from materials a player reaches at the 480 V stage (lithium carbonate from
  spodumene by a small crafting route), so a small base can have storage before chemicals (less real, more useful early).
- **Answered by Tim 2026-11-19:** the Chosen option.

## Decided while building (22a-22c, 2026-11-19): for Tim

Each was **decided on recommendation**; none needs Tim before it can stand, but each can be changed.

### 9. The graphite furnace's supply
- **Chosen:** its own furnace transformer on the distribution tier (12.47 kV, 11 kV IEC), not industrial MV.
- **Why:** a 1.4 MW resistance furnace is fed as the submerged arc furnace is, from a distribution feeder through its furnace transformer; on
  the industrial MV tier it would have needed a unit substation run backward, and the 4.16 kV parts have no source that size.
- **Alternatives:** industrial MV, as the spec first said.

### 10. Line to furnace
- **Chosen:** spherical graphite travels in bulk bags (a new `spherical_graphite_bag`), the flotation tails counted.
- **Why:** every other plant's solid intermediates go in bags; touching would tie the two parts' layout together.
- **Alternatives:** the furnace touching the line and drawing from it; the tails as slurry to a lined cell.

### 11. Loading modules
- **Chosen:** a container or cabinet is crafted empty and loaded by right-clicking modules into it (96, or 5). A full worn one takes 96 new
  ones and gives the worn ones back as spent modules.
- **Why:** a crafting grid holds one item a slot, so it cannot take 96 modules; real containers are racked on site.
- **Alternatives:** a module "rack" item of 8 modules (12 make a container), crafted.

### 12. The MV skid's windings
- **Chosen:** wye-wye, as the pad-mounts are, so no 30-degree shift; the PCS joins its low side directly.
- **Why:** the solver's delta-wye shift is only followed by machines that synchronise; a grid-following PCS would then read its bus 30 degrees
  off for nothing a player sees.
- **Alternatives:** delta on the low side (the real winding), with the PCS taking the shift as a genset does.

### 13. What peak shaving watches
- **Chosen:** what the island's machines (generators that are not storage) delivered at the last solve.
- **Why:** it needs no new current transformer part, and on a genset island (where a cabinet earns its place) that is the peak to shave.
- **Alternatives:** a CT on a named feeder (a new part, or a transmitter landed on the PLC).

### 14. A grid-forming block's share
- **Chosen:** it shares an island by its 5 % sag, as a genset does; its P setpoint applies only while following; it trips EMPTY at the bottom
  of its window.
- **Why:** the island frequency model was left as it is; a forming block behaves as one more machine, which is how real ones are tuned.
- **Alternatives:** a power setpoint while forming (a voltage trim, as the gensets' PLC trims).

### 15. The plant controller's power
- **Chosen:** none drawn (its station battery and the PCS's auxiliaries).
- **Why:** 3 kW at residential voltage would only add a cable a player must run before the plant does anything.
- **Alternatives:** a 3 kW residential load, as the meter building has.

### 16. What sets a container off
- **Chosen:** fire or lava touching it for 30 s, or a burning container touching it for 2 minutes. Not yet explosions or lightning.
- **Why:** both are seen and avoidable; an explosion already breaks a container's blocks, and the mod has no lightning hook yet.
- **Alternatives:** add explosions and lightning (spec item 4's list).
- **Superseded in 0.10.1 by 21:** explosions and lightning now set one off too.

### 17. The cabinet's quest
- **Chosen:** in the new `grid_storage` chapter, after the first module.
- **Why:** its modules come from the cell plant, which is after chemicals; a quest in `three_phase` would wait a long time.
- **Alternatives:** an optional quest in `three_phase`, as the spec said.

### 18. The cell line's two pipes
- **Chosen:** HF on its first cell's east face, ethylene on its third's.
- **Why:** pipes on neighbouring cells touch and join into one line, which mixes them.
- **Alternatives:** HF on the east face, ethylene on another face.

### 19. Sounds
- **Chosen:** the DC contactors' clunk when a PCS or cabinet leaves STOPPED, and the inverter's whine every 4 s while it moves over a tenth of
  its rating (a one-shot repeated, not a loop).
- **Why:** a looped sound needs the client to know the PCS is running, which these parts do not send yet.
- **Alternatives:** a client-side running loop, as the motors have.
- **Superseded in 0.10.1 by 22:** the whine is a client-side running loop.

### 20. Burned-out containers
- **Chosen:** the container is gone; 16 iron ingots of scrap are left; its modules are lost.
- **Why:** the spec's "scrap steel back, modules lost", in items the player has.
- **Alternatives:** a burned container block to clear.

## 0.10.1 (2026-11-20, decided on the best recommendation while Tim is away; for his review)

### 21. Explosions and lightning set a container off
- **Chosen:** under `bessThermalRunaway`, an explosion that would break any cell of a container sets it off instead (its blocks are
  spared: it burns out to scrap as any runaway does); lightning striking its roof sets it off unless a ground rod stands within 4 blocks
  of any of its cells (a cube, the rod need not be wired to it). The struck block is the one under the bolt, as the game's lightning rod
  reads it; the container looks for a bolt every other tick (a bolt lives at least 3). A wind charge (it only triggers blocks) and a blast
  that keeps blocks (a creeper with mob griefing off) do nothing. With the rule off nothing changes: a blast breaks it as before and
  lightning is only lightning.
- **Why:** the spec's list (item 4), with the least new machinery: the game already tells a block it is in a blast, and polling for a bolt
  near a container needs no loader hook. Sparing the blocks keeps the runaway visible: a container blown apart could not burn. The mod's
  own blasts that break blocks (the containment's hydrogen, the vessel's hydrogen) now count; the blowout fire counts as fire already.
- **Alternatives:** let the blast break it and set the containers touching it off; a lightning hook on each loader (an entity-join event);
  the rod counting only when wired to the container by ground wire (the bonding rule `Grounding` uses).

### 22. The PCS's whine is a loop
- **Chosen:** the PCS and the cabinet send the client how hard they switch, in tenths of their rating (0 under a tenth: silent), checked
  every half second and sent when it changes; the client plays `inverter_whine` as a running loop (the motors' `MachineSound`), its pitch
  from 0.84 at a tenth of rating to 1.2 at full power and its volume from 0.3 to 0.75, fading out when it stops. The repeated one-shot is gone.
- **Why:** the spec's "rises with power"; tenths keep the updates rare while frequency response wanders the power a little.
- **Alternatives:** the exact power sent every tick; a fixed loop with no pitch change.

