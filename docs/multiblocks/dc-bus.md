# DC tier: rectifiers, bus bars and cells on the solver (Phase 10b1; values approved with the 10b1 plan, 2026-11-03)

## Real-world counterpart
Electrolysis runs on DC: fluorine cells, magnesium cells, chlor-alkali cells and, in 10b3, aluminium potlines.
- **The rectifier:**
  - a regulating transformer (on-load taps, plus saturable reactors for the fine steps) feeds a diode bridge;
  - it holds the **line current** at a setpoint, whatever the number of cells in series, as a potline rectifier does;
  - the voltage is whatever the cells need, up to its maximum.
- **The bus bars:** bare copper (aluminium on a potline) on standoff insulators, carried from the rectifier's DC terminals
  through the cells in series and back.
- **Grounding:** the DC system is **ungrounded**. A ground-fault detector ties each pole to earth through a balancing resistor:
  - with no fault, each pole sits at about half the voltage from earth;
  - when one pole faults to earth, the other pole is at the full voltage, and the detector alarms and names the faulted pole.
- **Shorts:** a bar across the two poles is a short. The rectifier trips on DC overcurrent.

The counterparts:
- copper bus bars of standard 100 x 10 mm and 200 x 10 mm flat bar, run in parallel bars;
- a 12-pulse diode rectifier with an on-load tap changer and saturable reactors (the potline and chlor-alkali kind);
- a DC ground-fault relay with balancing resistors to earth.

## Values *(proposed; approved with the 10b1 plan, 2026-11-03)*
| Item | Value | Basis |
|---|---|---|
| 4 kA bus bar | 4 x 100 x 10 mm copper, **4 kA**, **5.2 µΩ/m** at 75 °C | about 1 A/mm² for a bare bar in air |
| 16 kA bus bar | 8 x 200 x 10 mm copper, **16 kA**, **1.3 µΩ/m** at 75 °C | as above |
| Rectifier setpoint dial | 1, 2, 2.8, 4, 6, 8, 10, 12, 14, 16 kA | covers today's cells: 2.8 kA chlor-alkali, 6 kA fluorine, 14 kA magnesium |
| Rectifier maximum DC volts | **150 V**, so 16 kA x 150 V = 2.4 MW | a mid-size electrolysis rectifier |
| Rectifier internal resistance | 0.1 mΩ | its regulation, about 1 % of 150 V at 16 kA; its losses are in the 97 % efficiency |
| Rectifier ramp | at most 15 V a second (10 % of its maximum) | an on-load tap changer steps in seconds |
| Rectifier AC side | 97 % efficient, PF 0.95, on 4.16 kV (unchanged from 7d) | |
| DC overcurrent trip | at 1.25 x setpoint for 1 s, or at once at 2 x with its terminals below half its EMF (a bolted short) | rectifier DC overcurrent relay |
| Bus bar joint | 2 µΩ (bolted) | a bolted copper joint, a few µΩ |
| Ground-fault detector | 10 kΩ from each pole to earth; alarm when a pole's share of the voltage leaves 30-70 % | DC ground-fault relay |
| DC shock | none below **120 V DC**; above that, the 480 V tier's shock, scaled by volts / 600 V and halved | IEC 61201 ripple-free ELV limit 120 V DC; IEC 60479-1: DC is about 2-4 x less dangerous than AC at the same current |

Cells as DC loads: a back-EMF in series with a resistance that gives the rated volts at the rated current.

| Cell | Back-EMF | At rated | Basis |
|---|---|---|---|
| Fluorine cell house, 12 cells in series | 2.9 V a cell | 9 V a cell at 6 kA (108 V) | KF·2HF decomposition 2.9 V |
| Magnesium cell | 2.5 V | 5 V at 14 kA | MgCl2 decomposition at 700 °C about 2.5 V |
| Chlor-alkali cell, 6 cells in series | 2.2 V a cell | 3.1 V a cell at 2.8 kA (18.6 V) | membrane cell, reversible about 2.2 V |

A cell's output goes with the current through it: Faraday's law and its current efficiency, as before. At the rated current
it makes exactly what it did in 7d and 7f.

## Parts
| Id | Real part | Cells (W x H x D) |
|---|---|---|
| `wildcatter:dc_bus_4k` | 4 kA copper bus bar on standoffs | 1 x 1 x 1 |
| `wildcatter:dc_bus_16k` | 16 kA copper bus bar on standoffs | 1 x 1 x 1 |
| `wildcatter:rectifier` | today's rectifier (conversion.md), now a DC source on the solver | 3 x 4 x 4 |

A bus bar is one block, 1 m of bar, placed along an axis like a log (X, Y or Z). It has a terminal on all six faces: it joins
any bus bar or DC terminal it touches, whatever its axis. Corners are made by placing a bar of the other axis.

## Terminals
Each part is written facing north (x east, z south, from its main cell).

```
rectifier, east face (x = 2), z = 2:      y = 2  +   (upper bar)
                                            y = 0  -   (lower bar)
fluorine cell house, west face (x = 0), z = 6:   y = 2 +, y = 0 -
magnesium cell, east face (x = 2), z = 3:        y = 2 +, y = 0 -
chlor-alkali cell, east face (x = 2), z = 3:     y = 2 +, y = 0 -
```
- + and - are a cell apart, so the first bars off them don't touch.
- **Polarity is the circuit's, not the bar's:** every bus bar is the same block, and the voltage along a run is whatever the
  cells make it. Two bars at different voltages that touch are joined, which is a short across what lies between them, as a
  shorting bar across a pot.
- **Touching still works:**
  - a cell set against its rectifier, as in 7d and 7f, is joined to it directly, as if by short internal bars;
  - the rectifier then takes that cell's rated current as its setpoint;
  - bus bars are only needed to reach a cell further away, or to run cells in series.
- **A bus bar against bare ground** (a ground wire) is a ground fault on that point.

## How it runs (`circuit/DcSupply`)
1. **Solved with everything else:** the DC side is solved in the same group as the rectifier's AC side, by the same solver. All
   DC voltages are real numbers at 0°.
2. **Regulation:** each solve, the rectifier moves its EMF toward the voltage that drives its setpoint current, estimating the
   load's slope from its last two readings. It rises at most 15 V a second, up to 150 V, and comes down at once (phase control is
   fast). A cell closing onto the open-circuit voltage overshoots for a moment and is pulled back; it is not a short.
3. **No reverse current:** while its EMF is below the load's own voltage, the rectifier is off (a diode bridge cannot drive
   current backward).
4. **AC side:** it takes the DC watts of the last solve / 0.97 at PF 0.95. On an AC undervoltage it stops.
5. **Protection:** DC overcurrent trips it (right-click to reset). The ground-fault detector alarms but does not trip, as on a
   real ungrounded system.
6. **An idle cell** (no feed, or nowhere for its product to go) opens its circuit, so it draws nothing. With nothing drawing,
   the rectifier's voltage rises to its 150 V maximum, the open-circuit voltage.

## Controls and readouts
- **Rectifier:**
  - sneak and right-click steps the setpoint dial;
  - right-click resets a trip;
  - readouts:
    - "DC 108 V, 6.00 kA (setpoint 6 kA), ground OK";
    - "GROUND FAULT on -: + at 108 V to ground";
    - "TRIPPED (DC overcurrent 13.1 kA): right-click to reset".
- **Bus bar:** "DC bus bar (16 kA copper): +54 V to ground, 6.00 kA of 16 kA allowed".
- **Power Meter on a bus bar:** the same.

## Shock
- A bare bus bar at **120 V DC or more to ground** shocks whoever stands within its reach (the 480 V tier's reach and touch chance).
- **Today's cells:**
  - the 108 V fluorine house can't shock: its poles sit at ±54 V, and even with a ground fault the other pole is at 108 V;
  - an open rectifier at 150 V with one pole faulted puts the other at 150 V, which shocks.
- **10b3's potline:** about 170 V for 40 pots. It shocks once one pole is faulted to ground.

## Rotation, placing, removal
- Bus bars take an axis when placed (from the face clicked), like a log.
- Breaking a bus bar opens the run there.
- A bar carrying more than its ampacity heats, and burns open as a wire does (the same heating rule).

## Game test (`dcTier`)
1. **On bus bars:**
   - setup: a magnesium cell 8 m from its rectifier over 16 kA bus, the rectifier on a diesel set through a backward unit
     substation to 4.16 kV, its dial stepped to 14 kA;
   - the rectifier holds 14 kA at about 5 V, and the cell makes magnesium;
   - the detector reads the + pole at half the voltage, ground OK.
2. **A ground fault:** a ground wire against the − bus. The detector alarms on −, and the rectifier does not trip.
3. **A burned bar:** a 4 kA bar in the − run, carrying the cell's 14 kA, burns open.
4. **Open, and a shock:** with the run open, the rectifier rises to 150 V. With its − pole faulted, the + bus is at 150 V to
   earth and shocks a pig beside it.
5. **A short:** a bar bridging the + and − runs trips the rectifier on DC overcurrent: at 150 V through its
   0.1 mΩ, far past twice its setpoint, with its terminals collapsed.
6. **Touching:** a magnesium cell set against its rectifier, with no bus. The setpoint follows to 14 kA, and it makes magnesium.

The fluorine cell house is not in this scene: its anhydrous HF can't be stocked in a tank. The `conversion` scene runs it on
its touching rectifier.
