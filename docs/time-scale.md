# Time scale: one world setting for the long waits (proposed, 2026-11-10)

Tim (2026-11-10): "Some of these we should have in new world setup like if the player wants to reduce the time of things we could
do a 1:1 5:1 10:1 20:1 50:1 presets."

Status: **spec only, not built.** Planned for 0.7.0 on a branch from `w-next`; the questions at the end are Tim's.

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

Not scaled: electrical (protection trip times, motor and stator heating and cooling, breaker and clamp cooling), every flow and
throughput, burn times (a fuel bucket still burns an hour), the drill and TBM advance, train and crane speeds, and the waits that are
already compressed for play (the steam plant's 300 s heat-up, `HEATUP_SPEEDUP`). Those keep their present numbers at every preset.

## How it is stored and read
- One value per world, saved with the world rules (`data/wildcatter/rules.dat`, `RulesData`). Tailings has Wildcatter as a
  dependency and reads Wildcatter's value, so a world has one scale for both mods.
- Plain Java: `TimeScale.wait(seconds)` returns `seconds / scale`. Each scaled wait above goes through it, so a new wait opts in
  by one call.
- An existing world loads at **1:1**, so nothing changes in it. A new world gets what was chosen on the Create World screen; the
  default there is 1:1.
- `/wildcatter rule timeScale <1|5|10|20|50>` (operators) changes it at any time. A wait already under way finishes at the new scale
  from that moment; what it has done so far stays done.

## New world setup
- A **World Rules** button on the Create World screen's More tab opens a screen with:
  - the time scale as five buttons (1:1 to 50:1);
  - every opt-in rule of both mods as a toggle, preset to its new-world default (`reactorContamination`, `cascadeSpinUp` and
    `paste_wall_breach` off, the rest on).
- The choices go into the new world when it is made, before its first chunk (the same moment the rules are made today). This
  replaces typing `/wildcatter rule` and `/tailings rule` in a new world.
- A dedicated server has no Create World screen. It reads `config/wildcatter-new-world.properties`
  (`timeScale=10`, `mudGel=false`, ...) when it makes a new world; the command covers the rest.
- **Alternative:** vanilla's game rules screen (Create World, then Game Rules). It needs no new screen, but it shows the scale as a
  number field rather than five presets, and the world rules would have to move from `rules.dat` into game rules.

## Game tests
- `time_scale_reactor`: a heat-up at 50:1 reaches no-load temperature in a fiftieth of the 1:1 time.
- `time_scale_holds`: a digester's first product at 10:1 comes out after 6 minutes, not an hour.
- `time_scale_existing`: an existing world loads at 1:1; `/wildcatter rule timeScale 20` sets it, and a bad value is refused.
- Client film `new_world_rules`: the Create World screen, the World Rules button, choosing 20:1, and the rule read back in the world.

## Questions for Tim
1. **Waits only, or rates too?** The recommendation is waits only (above).
2. **The Create World screen:** a World Rules screen with five preset buttons (recommended), or vanilla's game rules list?
3. **Fold in the existing shortcuts?** The steam plant's 5-minute heat-up and the reactor's `HEATUP_SPEEDUP` are compressed today
   with no rule. With a scale, they could become their real times divided by the scale. That changes them in every world, so the
   recommendation is to leave them as they are.
4. **Presets only?** Five presets as Tim listed them, or any whole number from 1 to 50 by command?
