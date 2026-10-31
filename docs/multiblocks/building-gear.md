# Building gear and the starter plant at true size (Phase 4.5f)

**Status: APPROVED by Tim on 2026-10-31 with the 4.5f plan.** 1 block = 1 m. Small gear is one cell, drawn at its real size and
mounted where it is clicked; ratings, terminals and behaviour are unchanged (phase 1 to 3 docs). The MCC is in
motor-control-center.md.

## In-line gear: in at the back, out at the front
Each gets a `mount`: `wall` when clicked against the side of a block, otherwise `floor`.
- On a wall it hangs on it; the line runs along the wall, in from the left and out to the right as you face it (facing north
  the wall is on the west). Conduit stubs run from the enclosure to the two terminal faces.
- On the floor the enclosure stands on a Unistrut stand, conduit stubs to the two terminal faces.

| Block | Real-world counterpart | Size (W × H × D) |
|---|---|---|
| `breaker` | 1-pole enclosed circuit breaker (NEMA 1); its handle shows on, off or tripped | about 0.15 × 0.25 × 0.1 m |
| `switch` | 30 A general-duty safety switch with its side handle | about 0.2 × 0.35 × 0.12 m |
| `ufls_relay` | frequency relay over a shunt-trip breaker in one enclosure | about 0.25 × 0.3 × 0.15 m |
| `fuse` | fused pullout disconnect; the cartridge shows blown | about 0.15 × 0.2 × 0.1 m |
| `breaker_3p_lv` | 3-pole enclosed breaker, 240 V class | about 0.3 × 0.5 × 0.15 m |
| `breaker_3p_industrial` | 3-pole enclosed breaker, 480 V class | about 0.4 × 0.8 × 0.2 m |
| `motor_starter` | combination starter, NEMA 12: disconnect handle, start/stop buttons, run and trip lights | about 0.4 × 0.75 × 0.25 m |

The single-pole MV, DIST and HV breakers and the DIST under-frequency relay stay one-block stand-ins (Phase 6).

## Gear wired on every face
Each gets an `attach`: the face of its cell it is mounted on (a wall, the floor or the ceiling).

| Block | Real-world counterpart |
|---|---|
| receptacles | NEMA 5-15 and 5-20 duplex on a steel box (0.07 × 0.11 m plate); NEMA 14-50, L21-30 and L22-30 on a 0.1 × 0.1 m box; CEE 7 on a round box; IEC 60309 wall sockets (about 0.1 × 0.15 m) with a hinged lid, blue (230 V) or red (400 V). On the floor, a floor box. |
| plug adapters | the matching plug on a cord stub, pointing at its receptacle |
| `electric_lamp` | 60 W lamp in a keyless lampholder with a glass jar guard |
| `electric_heater` | 1,500 W wall fan-forced heater, about 0.4 × 0.5 × 0.1 m, grille and thermostat knob; on the floor it stands on feet |
| `three_phase_heater` | 15 kW 480 V industrial unit heater, about 0.6 × 0.6 × 0.45 m, fan and louvres, on a wall or ceiling bracket; on the floor on a stand |

## The starter plant
| Block | Cells | Real-world counterpart |
|---|---|---|
| `boiler` | 1 × 2 × 1 on a campfire or firebox | small vertical fire-tube boiler, about 0.6 m across and 1.4 m tall with its stack, water gauge glass, safety valve and pressure gauge (about 20 kW of steam for the 2 kW set) |
| `steam_turbine` | 1 | 2 kW single-stage steam turbine and generator on one skid, about 0.8 × 0.5 × 0.4 m; its shaft runs along its facing |
| `pump` | 1 | 750 W close-coupled centrifugal pump, about 0.55 × 0.3 × 0.3 m |
| `firebox` | 1 | cast-iron coal firebox with its fire door and grate |

The boiler's pipes and fire still attach at its bottom cell; an older world's one-block boiler grows upward where it has room.

## Game test
`buildingGear`: a breaker clicked on a wall takes the wall mount and runs along it, a receptacle and a lamp take the faces they
are clicked on, a lamp lights through a wall breaker; screenshots of a wall of gear and the starter plant.
