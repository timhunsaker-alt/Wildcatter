# Built From Scratch quest fixes: judgement calls (2026-11-26, branch `bfs-fixes`, 0.10.2)

From the quest check (`videos/longform/built-from-scratch/QUEST_CHECK.md`, "Fixes, ranked"), which Tim approved with the episode order
that follows the book. Format as in `docs/questions/README.md`.

### 1. Item tasks kept: where and how long
- **Chosen:** a new `items` list in each player's `QuestData.Progress` (`<quest id>#<task index>`), read with a default of empty, so a
  0.10.1 save loads unchanged. An item task met once is added and counted as met from then on; when the quest is done its entries are
  dropped, so the list holds only open quests' tasks.
- **Why:** the report's fix 2. The index is stable while a chapter is unchanged; if a data pack reorders a quest's tasks, a kept entry
  may tick a different line once, which is harmless.
- **Alternatives:** keep the item ids instead of the index (two tasks for the same item in one quest would share one entry).

### 2. `motor_started`: what counts as "a motor started from a starter"
- **Chosen:** a new key fired by the starter (standalone, MCC bucket, starter cabinet) when it is closed, current flows on all three
  lines (none under a fifth of the most loaded), for three motor start times (6 s), and the current is down to twice the overload
  setting or less; a soft starter must be bypassed, a drive at its speed. The motor quest asks for it. `motor_running` is unchanged.
- **Why:** the starter cannot see the motor's speed; current settled from inrush on all three phases, after the start time, is what a
  real operator reads on the starter's ammeter. A lost phase or a stall keeps it from firing.
- **Alternatives:** fire from the motor if anything upstream is a starter (the circuit does not record that); accept that the motor
  quest ticks when its parts are carried (the report's other option).

### 3. `pole_transformer_energised`
- **Chosen:** fired alongside `transformer_energised` when `TransformerBlock.poleMount()` (a rated single-phase unit). The pole
  transformer quest asks for it.

### 4. `requires`: checked against code
Each of the report's items 5 and 6 was checked; all hold, with these notes:
- **aluminium drops `coke`:** the pot's recipe takes a calcined coke bag (delayed coker and coke kiln), and a new pot comes with its 18
  anodes, so the first ingot needs no coke-oven product. Later anode changes (about every 1.5 h of a pot's carbon) take rodded anodes
  whose paste binder is coal-tar pitch from the coke ovens' tar still. Kept out of `requires` (the quest's task is one ingot) but named
  in the quest text.
- **coke loses `acid_plant`:** now `ore_survey` (the chapter's root) and `motor` (the ovens' motors are 480 V). Nothing from the acid
  plant is used.
- **battery_cabinet loses `lfp_module` and `genset`:** now `plc` (the recipe's CPU). The cabinet does nothing without modules, but the
  quest's task is the cabinet itself.
- **spent_fuel loses `critical`:** now `nuclear_island`. Its three items are made from early materials; the pool belongs beside a
  reactor, and the island comes before the 28-50 h heat-up rather than after it.
- **lfp_module:** besides aluminium, the cell line takes anhydrous HF, made by the HF kiln and column (the conversion plant's front end,
  uranium chapter). The report did not list them; they are named in the quest text, not added as a task or a requirement.

### 5. Machines the book never taught: which quest gets each
- **Chosen** (each an item task, and named in the quest's text):
  - land rig (`rig_substructure`, the controller) and lime kiln: casing_point (cement is a quicklime bag and clay);
  - barite mill: choke_kill; bentonite dryer: oil_mud (its second mode makes the OBM package);
  - refrigeration compressor and stabilizer: gas_plant (the chiller will not run without the compressor; the stabilizer takes the raw NGL);
  - quench tower, cracked-gas compressor and cold box: plastic;
  - coke drum and coke kiln (the delayed coker and calciner): aluminium;
  - nitrogen generator: battery_graphite (the first quest that needs nitrogen);
  - lithium leach (the lithium plant's controller), lithium carbonator and CO2 liquefier: cathode_powder (the lime kiln is named, taught
    in casing_point);
  - induction furnace and AOD converter (the alloy melt shop): fuel; the nickel refinery is named in the text.
  - Tailings' quests take the submerged arc furnace (ferrosilicon), the nitric acid unit and emulsion blender (blasting), and the haul
    truck and loader (gyratory): see Tailings' `docs/questions/bfs-fixes.md`.
- **Why one item per machine:** a quest showing every part would run to a dozen lines; the controller or the part that makes the
  product stands for the machine, and the text names the rest.
- The melt shop tasks stand even with `fuelNickelHardware` off (on in new worlds); the text says the hardware needs them with it on.

### 6. `fastProduction`: which rates
- **Chosen:** a world rule, off by default and off in new worlds. With it on, these rates are multiplied by the time scale: the lime
  kiln (and the kiln gas it holds), the ammonia plant as a whole (reformer, nitrogen generator, syngas compressor and converter, each
  sized to the same 20 Nm3/h, so multiplying one alone changes nothing), the graphite line and furnace, the lithium plant's rating, the
  lithium carbonator, the CO2 liquefier, the LFP kiln, and every flow stage of the zirconium line.
- **Why:** the report's chains over about 5 h before the uranium chapter: cement 9.6 h, fertilizer 14.4 h, graphite 5.3 h, cathode
  16 h, zirconium 20 h. Tim named all five.
- **Left out:** the aluminium pot's green coke (about 2 h on the crude column, 7.6 h on the topping unit) is held by the crude unit's
  residue; the crude unit is a refinery rate Tim did not name. The cell line (10 min a module) is short already.
- **Side effects, accepted:** the nitrogen generator also feeds the melt shop's AOD converter and the zirconium line also makes the
  fuel's cladding, so with the rule on those uranium-chapter steps get their nitrogen and Zr tubes faster. Their own rates are not
  touched. The machines' feeds (ore, gas, acid, power) are not multiplied: the lime kiln at 10:1 burns ten times the gas.
- **The scene:** `fast_production` uses the graphite line (it needs only ore and 480 V, so it shows the rate alone).
