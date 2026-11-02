# Grid performance

How the circuit solve is scheduled, what it costs, and where the limits are.

## When a grid is solved

`CircuitManager` groups every loaded circuit part into connected groups and solves each group with the banded phasor solver.
A level starts a **pass** over all of its groups:

- at once when something changes (a part placed, loaded or removed, a switch or breaker, a wire: `CircuitManager.markDirty`), and
- every `Tuning.SOLVE_INTERVAL` ticks (4) otherwise, because motor loads, generator governors and island frequency move on their own.

On the other ticks nothing is solved. A breaker trip re-solves only its own group, up to 4 times in that pass.

## The per-tick budget

A pass normally finishes in the tick it starts. Once a level has spent `Tuning.SOLVE_BUDGET_NANOS` (10 ms) solving in a tick,
the rest of its groups wait for the next tick. At least one group runs each tick, so a pass always finishes. A deferred group's
simulated time runs from its own last solve, so generators, wire heating and frequency see the true elapsed time. While a pass is
unfinished no new one starts. A change made during it starts the next pass as soon as this one ends.

A single group is never split, so one huge group can still take longer than the budget in its tick. The node cap
(`Tuning.MAX_CIRCUIT_NODES`, 4,000) bounds that. `CircuitManager.deferredTicks` counts the ticks on which work was deferred.

## Measured cost

Here is `PhasorSolver.solve()` alone on synthetic grids, best of 7 runs after warm-up. The setup was JDK 25 on an 8-core cloud VM
(`./gradlew test --tests '*GridSolveBenchmark' -Dwildcatter.bench=true`). Building the circuit from block entities and
finding the groups are not included.

| nodes | three-phase feeders (ms) | square mesh (ms) |
|------:|-------------------------:|-----------------:|
|   100 | 0.2 | 0.2 |
|   250 | 0.3–0.4 | 0.3–0.6 |
|   500 | 0.2–0.3 | 0.7–1.3 |
| 1,000 | 0.3–0.6 | 1.5–1.6 |
| 2,000 | 1.0–1.7 | 6.9–8.8 |
| 4,000 | 1.1–1.2 | 24–28 |
| 8,000 | 2–3 | 80–82 |

The same benchmark on the development Mac (Apple M3 Pro, JDK 25, 2026-11-02):

| nodes | three-phase feeders (ms) | square mesh (ms) |
|------:|-------------------------:|-----------------:|
|   100 | 0.15 | 0.17 |
|   250 | 0.26 | 0.42 |
|   500 | 0.21 | 0.72 |
| 1,000 | 0.33 | 1.2 |
| 2,000 | 0.48 | 4.4 |
| 4,000 | 0.74 | 17 |
| 8,000 | 1.1 | 60 |

There, twenty 2,000-node meshes took 79 ms in one tick before the budget, and 13 ms in the worst tick after, over 7 ticks.

Radial feeders, the usual shape of a Wildcatter grid, stay cheap well past the node cap because the matrix band stays narrow. A
dense mesh grows much faster. At the 4,000-node cap one solve takes about 25 ms, half of a 50 ms tick.

With twenty 2,000-node meshes on one level, a pass used to take **112–118 ms in one tick**. With the budget the worst tick is
**13–15 ms** and the pass is spread over 10 ticks, so each group is solved about every 10 ticks instead of every 4.

## Practical limits

- Up to a few thousand nodes of ordinary feeders: no measurable load.
- Large meshed grids (lots of cross-ties between busses) are the expensive shape. Keep each connected group under about 2,000
  nodes, or split it with open disconnects, if its updates should stay at every 4 ticks.
- Raising `MAX_CIRCUIT_NODES` lets one group cost more than a whole tick. Raise `SOLVE_BUDGET_NANOS` only on a fast,
  dedicated server.
