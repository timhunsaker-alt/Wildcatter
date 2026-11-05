# Automatic transfer switch (Phase 10d3, proposed 2026-11-04)

A 480 V (400 V IEC) automatic transfer switch: it feeds a load from its normal source and, when that fails, starts a genset and moves
the load onto it, then back when normal returns.

**Decided with Tim on 2026-11-04:**
- **Open transition:** break before make; the two sources are never paralleled.
- **480 V, 3-pole.**
- **Genset start by redstone:** the ATS gives a redstone signal while it wants the standby source (the engine-start contact); the
  genset control panel gets a redstone remote start.

A figure marked *(proposed)* is a setting or estimate, not a published rating.

## Real-world counterparts
| Part | Counterpart | Values |
|---|---|---|
| `ats_480` | A 400 A, 3-pole, open-transition ATS (UL 1008) | 0.75 m wide, 1.9 m tall; 3-pole, solid neutral |

## Parts
1 x 2 x 1, wall-mounted (the back to the wall). Its main cell is the bottom one. All four facings.
```
 facing it:   T    top cell: NORMAL on its top face, EMERGENCY on its back face
              M    main cell: the LOAD leaves its bottom face
```
- A, B and C switch; N is solid (normal, emergency and load neutrals joined), and G runs through.
- **Redstone:** it is a redstone source on every side while it calls for the genset (strength 15).

## Sensing (from the standard's nominal, never in volts)
| Setting | Value | Counterpart |
|---|---|---|
| Normal fails | any phase below 85 % | the usual undervoltage dropout |
| Normal restored | every phase at or above 90 % | pickup |
| Emergency acceptable | every phase at or above 90 %, and the island at 95 % of nominal frequency | the usual emergency pickups |

## Sequence (*proposed* timers, scaled for the game; the factory defaults in brackets)
1. **NORMAL:** the normal contacts closed.
2. Normal fails: **TDES**, the engine-start delay, 3 s (3 s). If normal returns inside it, back to NORMAL.
3. **Start:** the redstone signal goes on. It waits for emergency to be acceptable.
4. **TDNE**, the transfer delay, 1 s (0 s).
5. The normal contacts open; a **neutral position** of 1 s *(proposed)* lets motors' residual voltage decay.
6. **EMERGENCY:** the emergency contacts closed.
7. Normal restored: **TDEN**, the retransfer delay, 30 s (30 min). If normal fails again inside it, the timer starts over.
8. The emergency contacts open; the neutral position, 1 s; the normal contacts close.
9. **TDEC**, the engine cooldown, 30 s (5 min); then the redstone signal goes off.
- If emergency fails while on EMERGENCY and normal is there, it transfers back at once (no TDEN).
- The two sets of contacts are never closed together.

## Controls
- **Right-click:** the readout.
- **Sneak and right-click with an empty hand:** a test: it acts as though normal had failed, until pressed again (then TDEN and TDEC
  run as usual).

## Readout
"Transfer switch 480 V, 400 A, open transition: on EMERGENCY, 41 A; normal 487 V 60.0 Hz (restored, retransfer in 22 s); emergency
482 V; engine start ON".

## The genset's remote start
- A redstone signal at the genset control panel runs the set; the signal dropping stops it (after the set's own cooldown, if any).
- With no redstone, the start button works as before. A panel started by its button isn't stopped by a redstone signal that was
  never there.

## Breaking
Drops itself. The redstone signal goes with it.

## Left out (Known gaps)
- No closed transition, no in-phase monitor, no load shed before transfer.
- The solver gives one frequency to a connected group, so with both sources wired in the ATS sees the same frequency on both; it
  checks the frequency only for emergency acceptance.
- The timers are scaled down from the real defaults.

## Game test (`ats`)
1. Genset 1, through a breaker, is the normal source; genset 2 is the emergency source, its control panel next to the ATS's
   redstone; the load is two 15 kW heaters.
2. Stop genset 1: the ATS signals, genset 2 starts, the load transfers; it reads EMERGENCY.
3. Restart genset 1: after TDEN the load retransfers; after TDEC the signal drops and genset 2 stops.
4. At no point were both sets of contacts closed.
