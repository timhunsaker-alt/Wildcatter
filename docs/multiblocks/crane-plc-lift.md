# A PLC starts a crane lift (Wave A, Tim's answer C21, 2026-11-19)

Tim chose "allow PLC lift start" over the recommended readings-only answer, with the interlocks specified before building. This is a
signal spec for the three existing cranes (tower, crawler, ring; `docs/multiblocks/tower-crane.md`, `crawler-crane.md`,
`ring-crane.md`), not a new structure: no part, block or recipe is added. Decisions taken on recommendation are in
`docs/questions/wave-a.md`.

## Real counterpart
- A mobile or tower crane's **rated capacity limiter** (RCL, also "load moment indicator"; ASME B30.5 and B30.3, EN 13000 and
  EN 14439): it reads the hook load and the radius (from the boom angle and length, or the trolley position), compares them with the load
  chart, warns at 90 % and cuts out the motions that increase the load moment at 100 %.
- An **anemometer** at the boom head or jib top, read by the crane's control, with the manufacturer's wind limit.
- **Outrigger or ground bearing** checks: the crane is set up on ground (or mats) that bears its pressure before any lift.
- A **swing area** kept clear of people: the area under the load path and the counterweight's swing, barricaded on a real site; on
  automated cranes (container yards, process cranes) a presence or area scanner stops the motions when someone enters.
- An **operator permissive**: an automatic or remote start is allowed only while a qualified person has given the crane over to it, on
  the crane itself (a keyed mode selector at the cab or the remote-control station).
- Typical 4-20 mA outputs of an RCL and an anemometer, as a PLC reads them: hook load (0 to the chart's largest figure), working
  radius (0 to the chart's largest radius), wind speed (a cup anemometer's common 0-50 m/s range).

## The signals (appended to the crane's existing three, so landed signals keep their numbers)
| # | Point | Kind | What it is |
|---|---|---|---|
| 0 | RUN ENABLE | output | as before (13g): in AUTO the crane works its lifts while it is on, and holds when it drops |
| 1 | CARRYING | input | as before |
| 2 | AUTO | input | as before: the selector is in AUTO |
| 3 | LIFT START | output | **new**: in AUTO, its rising edge starts one lift; the crane works it to the end and holds again |
| 4 | LIFT REFUSED | input | **new**: the last LIFT START was refused (cleared by the next one that starts); the crane's readout says why |
| 5 | HOOK LOAD | 4-20 mA input | **new**: the tonnes on the hook (the load and the hook block), 4 mA at 0 t, 20 mA at the chart's figure at its least radius |
| 6 | RADIUS | 4-20 mA input | **new**: the hook's working radius, 4 mA at 0 m, 20 mA at the chart's largest radius (the boom angle's or trolley's measure) |
| 7 | WIND | 4-20 mA input | **new**: the wind at the boom head or jib, 4 mA at 0, 20 mA at 50 m/s |
| 8 | AREA CLEAR | input | **new**: no person in the swing area (below) |

## The interlocks (all must hold for LIFT START, checked when it rises; the crane's own, not the program's)
1. **Operator permissive:** the crane's selector is in **AUTO**. Only a person at the crane with the control key puts it there; in
   HAND or OFF, LIFT START does nothing (and LIFT REFUSED is not set: the PLC has not been given the crane).
2. **Rated load chart:** the lift is within the chart at its radius (the crane's existing TOO HEAVY and OUT OF REACH checks; a part too
   heavy for one crane goes in tandem as before). A started lift never exceeds the chart.
3. **Hook load:** the same chart check uses the load and the hook block's tonnes (`Masses.onHook` plus the hook block), as the RCL does.
4. **Boom and radius:** the lift's pick and set lie between the chart's least and largest radius, and the height under the hook clears
   the site (the existing TOO HIGH check). A boom or jib blocked on its slew path refuses (the existing BLOCKED check).
5. **Ground bearing or outriggers:** the existing check of the ground under the crawlers or ring (`softGround`, plant-crane-gaps 2).
6. **Wind:** at or under the crane's limit at the boom head or jib (`windStop`, plant-crane-gaps 1); over it mid-lift the crane holds,
   as before.
7. **Swing area clear:** no player (other than one in spectator mode) within the lift's swing area: the circle about the crane's slewing
   centre of the lift's largest hook radius plus 3 m (the load's and the tail's swing), from 2 m under the crane's base to 2 m over its
   boom head or jib. Checked when LIFT START rises (the lift is claimed, the circle taken from its plan, and released again if anyone is
   in it), and once a second while a PLC-started lift runs: a person walking in holds the crane where it is, on its brakes, until the area
   is clear again (no motion restarts while anyone is inside).
8. **Power, fuel and a chest:** as for any lift (the existing checks).

A refused start sets LIFT REFUSED and the readout: `LIFT REFUSED: <why>`, where why is the crane's own wording (WIND, SOFT GROUND, TOO
HEAVY, OUT OF REACH, BLOCKED, NO SITE, WAITING ..., or SWING AREA NOT CLEAR: a person N m from the slewing centre).

## What does not change
- RUN ENABLE keeps its meaning; an existing program behaves as before. LIFT START works with RUN ENABLE off (one lift a start) or on
  (the crane is already working; a start then does nothing more).
- HAND keeps the crane its own operator's: it lifts as before, and LIFT START is ignored.
- The swing area hold applies to PLC-started lifts only. A crane working under RUN ENABLE or in HAND does not look for people (as
  before); its program can read AREA CLEAR and drop RUN ENABLE itself.

## Game test
NeoForge server scene `plc_crane_lift` (WaveAScenes): a crawler crane in AUTO, its chest and site as `plc_crane`, under a real PLC:
- in HAND, LIFT START does nothing;
- in AUTO with RUN ENABLE off, a LIFT START makes exactly one lift, then the crane holds (CARRYING true during it, HOOK LOAD reads the
  tonnes on the hook within 0.5 t, RADIUS the hook's radius);
- a test wind over the limit refuses the next start (LIFT REFUSED, WIND in the readout); calm, a start works again;
- a player standing in the swing area refuses the start (SWING AREA NOT CLEAR) and AREA CLEAR reads false.
