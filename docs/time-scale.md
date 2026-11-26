# Time scale: one world setting for the long waits (2026-11-10)

Tim (2026-11-10): "Some of these we should have in new world setup like if the player wants to reduce the time of things we could
do a 1:1 5:1 10:1 20:1 50:1 presets."

Status: **built for Wildcatter** on `w-timescale` (from `w-next`, for 0.7.0). Tim answered the four questions below (2026-11-10): waits
only, the World Rules screen, leave the old shortcuts, the five presets only. Tailings' side (its rules on the screen, the paste cure
scaled) waits for Wildcatter 0.7.0.

## What it is
A world setting `timeScale` with five presets: **1:1, 5:1, 10:1, 20:1, 50:1**. At 10:1 a wait that takes 10 hours in the real plant
takes 1 hour of play. It is chosen on the Create World screen with the world rules, and changed later by command.

## What it scales: waits, not rates
The scale shortens the time a thing *waits* before it is ready. It does not change how fast anything runs: a pump's flow, a mill's
tonnes an hour, a fuel's heat, a motor's start and every electrical number stay real.
- **Why:** a rate scaled 10x makes the whole plant 10x bigger per block, and fuel and feed run out 10x faster. That changes the
  balance of everything already tuned. A wait scaled 10x only changes how long the player stands and watches. This is what Tim asked
  for ("reduce the time of things").

| Wait | Real time | At 10:1 | At 50:1 |
|---|---|---|---|
| Reactor heat-up, rule `reactorRealHeatup` (55 C an hour) | about 5 h | 30 min | 6 min |
| Reactor cooldown to 120 C (55 C an hour) | about 3 h | 19 min | 4 min |
| Coke oven charge, rule `realResidenceTimes` | 18 h | 1.8 h | 22 min |
| Bayer digesters, `realResidenceTimes` | 1 h | 6 min | 72 s |
| Bayer precipitators, `realResidenceTimes` | 36 h | 3.6 h | 43 min |
| Dry cask cooling, for eligibility (already counted 100x) | years | 10x more | 50x more |
| Tailings paste cure | 1 game day | 2.4 min | 29 s |

Not scaled (unless the world rule `fastProduction` is on, and then only the rates in "Fast production" below): electrical (protection trip times, motor and stator heating and cooling, breaker and clamp cooling), every flow and
throughput, burn times (a fuel bucket still burns an hour), the drill and TBM advance, train and crane speeds, and the waits that are
already compressed for play (the steam plant's 300 s heat-up, `HEATUP_SPEEDUP`). Those keep their present numbers at every preset.

## Fast production: an opt-in rule for filming (Tim, 2026-11-26)
Tim (2026-11-26, Built From Scratch): the very long chains (uranium to fuel, loading the core) are started on camera and then cut to a
world prepared off camera; the shorter long chains are filmed at a faster time scale, "once the time-scale setting also reaches those
production rates". The 2026-11-10 decision above (waits, not rates) still stands for every world that does not ask otherwise, so this
is a world rule of its own, **`fastProduction`, off by default and off in new worlds**.

With it on, these production rates are multiplied by the time scale (`TimeScale.production()`: the scale with the rule on, else 1).
At 1:1 the rule changes nothing.

| Chain (the quest it holds up) | Rates multiplied | Real time to the first product | At 10:1 |
|---|---|---|---|
| Quicklime for oilwell cement (casing_point) | the lime kiln's 10 t a day, and the kiln gas it holds for the CO2 liquefier | about 9.6 h for 4 bags | about 1 h |
| Fertilizer (fertilizer, spreader) | the whole ammonia plant together: steam reformer, nitrogen generator, syngas compressor, converter (15.2 kg/h of NH3) | about 14.4 h a DAP bag | about 1.4 h |
| Battery graphite | the graphite line (400 kg/h) and the graphite furnace (360 kg/h); the nitrogen generator above | about 5.3 h | about 32 min |
| Cathode powder | the lithium plant (its leach, kilns and crystallizer follow the leach's rate), the lithium carbonator (100 kg/h), the CO2 liquefier (150 kg/h), the LFP kiln (700 kg/h); the lime kiln above | about 16 h | under 2 h |
| Zirconium to the first tube | every flow stage of the Zr line (12 kg/h: separator, chlorinator, hafnium column, Kroll fill, distillation, VAR charge, extrusion, pilger); the Kroll and VAR batch holds already follow the scale as waits | about 20 h | about 2 h |

Not touched, rule or no rule: anything electrical (power drawn, protection, motor starts), fuel burn times, and the uranium and fuel
chain (mill, conversion, enrichment, fuel line, RCCAs), which is filmed from a prepared world. The inputs a multiplied machine pulls
(ore, gas, acid, power) are not multiplied, so a faster machine wants its feed to keep up: the lime kiln at 10:1 burns ten times the gas.
The zirconium line also makes the fuel's cladding, so with the rule on the fuel's Zr tubes come faster too.

Left out on purpose: the delayed coker's green coke for the aluminium pot (about 2 h on the crude column) is held by the crude unit's
residue, and the crude unit is not multiplied; the cell line's 10 minutes a module is short already.

Readouts that state a rate (the nitrogen generator's header, each machine's "kg/h" line) read the multiplied rate. The scene
`fast_production` shows the graphite line at 10:1 at its real 400 kg/h with the rule off, 4,000 kg/h with it on, and back again.

## How it is stored and read
- One value per world, saved with the world rules (`data/wildcatter/rules.dat`, `RulesData`). Tailings has Wildcatter as a
  dependency and reads Wildcatter's value, so a world has one scale for both mods.
- Plain Java: `TimeScale.scaled(seconds)` returns `seconds / scale`. Each scaled wait above goes through it, so a new wait opts in
  by one call.
- An existing world loads at **1:1**, so nothing changes in it. A new world gets what was chosen on the Create World screen; the
  default there is 1:1.
- `/wildcatter rule timeScale <1|5|10|20|50>` (operators) changes it at any time; any other number is refused. The reactor follows
  at once. A charge already in a coke oven or a Bayer stage keeps the time it was given when it went in; the next one takes the new
  scale.
- The readouts show both times: "coking at 1,100 C (18 h a charge, 22 min at 50:1)".

## New world setup
- A **World Rules... (time 1:1)** button on the Create World screen's Game tab, under the Grid button, opens a screen with:
  - the time scale as five buttons (1:1 to 50:1);
  - every world rule as a toggle, preset to its new-world default (`reactorContamination`, `cascadeSpinUp`,
    `realResidenceTimes` and `fastProduction` off, the rest on). Another mod adds its rules with `NewWorldSetup.register` (Tailings' after 0.7.0,
    `paste_wall_breach` off).
- The choices go into the new world when it is made, before its first chunk (the same moment the rules are made today). This
  replaces typing `/wildcatter rule` and `/tailings rule` in a new world.
- The choices are held until the player goes back to the title or world list. Coming back from World Rules keeps the Grid choice.
- A dedicated server has no Create World screen. When it makes a new world it reads `config/wildcatter.properties`:
  `newWorld.timeScale=10`, `newWorld.mudGel=false`, and another mod's `newWorld.<mod>.<rule>`. The command covers the rest.
- **Alternative:** vanilla's game rules screen (Create World, then Game Rules). It needs no new screen, but it shows the scale as a
  number field rather than five presets, and the world rules would have to move from `rules.dat` into game rules.

## Game tests
- `TimeScaleTest` (unit): presets only; waits divided; the reactor's real heat-up runs at the scale, the old 80x stands without the
  rule; the new-world choices and another mod's registered rule.
- `world_rules` (NeoForge): the test world loads at 1:1; `/wildcatter rule timeScale 50` sets it and `7` is refused; an hour's wait
  is 72 s; the heat-up speed follows.
- `time_scale_holds` (NeoForge): the residence plant at 50:1; aged a fiftieth of each hold, coke, digested slurry and hydrate come out.
- Fabric client (`createWorldScreen`): the World Rules button, the screen, 20:1 and mudGel off chosen, the Grid choice kept on return
  (screenshots `wildcatter-world-rules`, `-20x`, `wildcatter-create-world-rules-back`).

## Questions for Tim (answered 2026-11-10)
1. **Waits only, or rates too?** The recommendation is waits only (above). **Tim: waits only.**
2. **The Create World screen:** a World Rules screen with five preset buttons (recommended), or vanilla's game rules list? **Tim: the World Rules screen.**
3. **Fold in the existing shortcuts?** The steam plant's 5-minute heat-up and the reactor's `HEATUP_SPEEDUP` are compressed today
   with no rule. With a scale, they could become their real times divided by the scale. That changes them in every world, so the
   recommendation is to leave them as they are. **Tim: leave them.**
4. **Presets only?** Five presets as Tim listed them, or any whole number from 1 to 50 by command? **Tim: the five presets only.**
