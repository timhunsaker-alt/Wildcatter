# Grid batteries (2026-11-19): questions on the spec

Spec: `docs/multiblocks/grid-batteries.md` (DRAFT, Wave B step 1, nothing built). Format as in `README.md` here. Only real design
choices are here; everything else in the spec is decided on the best recommendation and says so. The recommended option is
**Chosen**; nothing is built until Tim has gone through these.

### 1. Sizes
- **Chosen:** two sizes: a **125 kW / 261 kWh commercial cabinet** on the industrial LV tier, and a **2.5 MW / 10 MWh utility block**
  (two containers, a PCS, an MV skid) on distribution, up to four blocks under one plant controller.
- **Why:** both are real products in common use. The cabinet fits the 480 V bases a player has long before distribution (peak shaving
  beside a genset, a small island's black start); the block is the real utility unit and a 4-hour duration is the usual one.
- **Alternatives:** the utility block only (fewer parts, but nothing for a small base); also a 5 MW block of four containers on one
  larger PCS (real too, but the same play at twice the size).

### 2. Where the cells come from
- **Chosen:** an **LFP cell plant** of four new parts (graphite line, graphite furnace, LFP kiln, cell line) that makes 52 kWh
  modules, one every 10 minutes, from lithium carbonate, phosphoric acid, iron and graphite schist.
- **Why:** every other product in Wildcatter comes off a real plant; it gives graphite schist a use and puts a 1.4 MW furnace on the
  grid; a container's 3.1 t of lithium carbonate makes the lithium plant matter.
- **Alternatives:** a crafting-table recipe for a module from the same bags (no new plant, much faster to build, far less real);
  both: the crafting recipe for the cabinet's five modules, the plant for containers.

### 3. Chemistry
- **Chosen:** **LFP only.**
- **Why:** it is what grid storage is built from now, its inputs are all in the mod (lithium carbonate, phosphoric acid, iron,
  graphite), and it is the safer chemistry.
- **Alternatives:** LFP and NMC (nickel from the smelter, cobalt from cobaltite: gives cobaltite its first use and a denser, hotter,
  more fire-prone container); LFP and sodium-ion (rock salt as the source; no lithium bottleneck; lower energy).

### 4. Thermal runaway
- **Chosen:** a world rule **`bessThermalRunaway`**, on in new worlds and off in existing ones, with **no random defects**: only fire,
  explosion damage or an ungrounded lightning strike sets a container off. A container touching a burning one catches; one a block
  apart does not.
- **Why:** real cell failures are about 0.004 a GWh a year (11 % of 0.04): a player's plant would never see one, so a random roll would
  only feel arbitrary. Abuse is what sets real ones off, and spacing is what fire codes require.
- **Alternatives:** a rare random cell defect as well (more danger, less fair); the rule off by default in new worlds too.

### 5. Degradation
- **Chosen:** **real LFP cycle fade:** 20 % over 6,000 equivalent full cycles, faster when hot or charged fast; no calendar fade; worn
  containers renewed with new modules.
- **Why:** real, and it rewards keeping the chillers running and not abusing the rate; at a cycle a game day it takes 6,000 days
  (about 83 real days of play) to lose 20 %, so it never punishes normal play.
- **Alternatives:** ten times faster, so a player sees it inside a world's life (less real, more upkeep); none (simpler; SOH removed).

### 6. Grid-forming
- **Chosen:** **both modes from the start of the phase**, a switch on the plant controller and a PLC point: grid-following by default,
  grid-forming for islands and black start (wave 22c).
- **Why:** grid-forming is the new thing a battery brings to the grid: it can hold an island with no machine and bring a dead one up.
  Real sites choose the mode in the controller.
- **Alternatives:** grid-following only, with grid-forming as a later upgrade item for the PCS (a progression step); grid-forming only
  (one model, simpler code, but not how most real fleets run today).

### 7. Fault current on an inverter island
- **Chosen:** **the real limit, 1.2 x rated:** on an island fed only by inverters, relays must be set below that or they never trip,
  and the block trips on undervoltage after 1 s.
- **Why:** it is the real protection problem of inverter grids, and the mod's relays already have settable pickups and show the fault
  current they see.
- **Alternatives:** give a grid-forming block a machine's 5-6 x fault current, so protection set for gensets keeps working (easier,
  not real).

### 8. Where in the progression
- **Chosen:** **after the lithium plant** (Phase 20c) and the fertilizer plant's phosphoric acid (18a): late game, a new quest chapter
  after `chemicals`.
- **Why:** that is where its materials are; and storage is most useful once a player has a grid worth stabilising.
- **Alternatives:** earlier, with the cabinet's cells crafted from materials a player reaches at the 480 V stage (lithium carbonate from
  spodumene by a small crafting route), so a small base can have storage before chemicals (less real, more useful early).
