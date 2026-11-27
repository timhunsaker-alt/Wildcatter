# Phase 4.1: conductor sizes (APPROVED 2026-10-30 with the Phase 4 plan)

Tim asked for wire gauges. ELECTRICAL_MODEL already says "conductor ampacity depends on size and material", and a 500 kW genset makes
about 750 A at 480 V, far past the single 100 A wire that tier had.

## Decisions (Tim, 2026-10-30)
- The practical set of sizes below, not the whole NEC table.
- **Real resistance per metre.** A wire block is 1 m of conductor; a span block is 4 m, as before.
- **Each size is its own item.** The size rides on the stack (a `wildcatter:conductor_size` component), goes into the placed block,
  and comes back when it is broken. The smallest size of each kind keeps the old recipe. A bigger size is crafted from several of the
  next smaller one, so the copper goes roughly with the cross-section.

## Sizes
| Size | Ampacity | Resistance | Basis |
|---|---|---|---|
| 12 AWG | 20 A | 6.50 mOhm/m | NEC 310.16 75 C copper, limited by 240.4(D); NEC Ch. 9 Table 8 at 75 C |
| 10 AWG | 30 A | 4.07 mOhm/m | same |
| 6 AWG | 65 A | 1.61 mOhm/m | same |
| 2 AWG | 115 A | 0.636 mOhm/m | same |
| 4/0 AWG | 230 A | 0.199 mOhm/m | same |
| 500 kcmil | 380 A | 0.0846 mOhm/m | same |
| 800 A busway | 800 A | about 0.02 mOhm/m per phase | standard 800 A copper feeder busway |
| #2 ACSR (Sparrow) | 180 A | 1.05 mOhm/m | standard ACSR, 75 C conductor |
| 1/0 ACSR (Raven) | 240 A | 0.66 mOhm/m | same |
| 4/0 ACSR (Penguin) | 340 A | 0.36 mOhm/m | same |
| 336.4 kcmil ACSR (Linnet) | 530 A | 0.205 mOhm/m | same |
| 795 kcmil ACSR (Drake) | 900 A | 0.087 mOhm/m | same |
| 1/0 MV cable | 195 A | 0.40 mOhm/m | NEC 310.60(C) one-circuit duct tables, 90 C copper (to verify) |
| 4/0 MV cable | 290 A | 0.199 mOhm/m | same (to verify) |
| 500 kcmil MV cable | 450 A | 0.0846 mOhm/m | same (450 A for 500 kcmil at 90 C, from 310.60(C)(79)) |

## Where each set is used
- Building wire and three-phase cable at 120/208 V and 480 V: 12 AWG to 500 kcmil.
- Wire and spans above 480 V: ACSR. Spans: #2 and 1/0 reach the tier's longest span; 4/0 is 10 % shorter, 336.4 kcmil 20 %, 795 kcmil 30 %
  (heavier conductors sag more on the same poles).
- 12.47 kV and 138 kV cable: MV cable sizes.
- Busway: 208Y/120 V and 480Y/277 V, 800 A.

## Existing worlds
A wire, cable or span saved before sizes takes the size nearest its old rating: 120 V wire 12 AWG, 480 V wire 2 AWG, 4.16 kV #2 ACSR,
12.47 kV 4/0 ACSR, 138 kV 336.4 kcmil ACSR, MV cable 500 kcmil. Resistances are now real, so voltage drop is smaller and fault currents larger than before.

# How conductors look and the single-phase cable (approved by Tim 2026-11-27, branch `wire-look`)

Tim: "cables and plugs don't quite look right; the power cables don't actually look like they connect to anything; pipes don't actually
look like they connect, they are just there."

## Thickness by size
Size lives in the block entity, so a `gauge` blockstate (small, medium, large, very_large) is set from the stack when a wire or cable is
placed, and kept in step with the size by the block entity (a wire from an older world, or one sized by a blueprint or command, takes
its gauge on its first solve). To keep the state count down, only wool wire has the `wear` property now (bare and plastic wire never
wear), so the gauge costs nothing for them; wool wire has four times its states.

| Gauge | Building wire and cable | ACSR | MV cable | Wire | Three-phase cable | Single-phase cable | Span line |
|---|---|---|---|---|---|---|---|
| small | 12, 10 AWG | #2 Sparrow | 1/0 | 2 px | 4 px | 3 px | 1 x |
| medium | 6, 2 AWG | 1/0 Raven, 4/0 Penguin | 4/0 | 3 px | 5 px | 4 px | 1.35 x |
| large | 4/0 AWG | 336.4 Linnet | 500 kcmil | 4 px | 6 px | 5 px | 1.7 x |
| very large | 500 kcmil | 795 Drake | | 5 px | 7 px | 6 px | 2.1 x |

Busway and bus duct keep their housings. The readout names the size, as before.

## Undersized
When a wire or cable (or the worst span on an insulator) carries more than its ampacity, its readout, and so the Power Meter on it,
starts with `UNDERSIZED: carrying 48 A; needs 6 AWG or bigger`: the smallest size of its own family that carries the present current
(`ConductorSize#undersized`). Past the family's largest it says no single conductor of that kind carries it. Wires never resize
themselves; the heating and burn-out are unchanged.

## Fittings
- **Wire and cable at a machine** (`WireShape#terminals`: an arm whose neighbour is not wire, cable, busway, ground wire or an
  insulator): a wire lands on a terminal block in a brass lug with its clamp screw; a cable goes through a cable gland (locknut, gland
  body, sealing nut). They are drawn by `client/TerminalRenderer` (a block entity renderer that draws nothing, and is skipped, for any
  piece with no machine beside it, so long runs cost one check). A machine drawn smaller than its cell is reached: the conductor is
  carried on to its surface on the conductor's line (its shape, ray-clipped) and the fitting sits there.
- **Wire and cable at wall, floor or ceiling gear** (receptacle, lamp, heater: `AttachedGear`): the conductor is carried across the gear's
  cell to its box and enters it through a connector, from above if the run comes down to it, otherwise from below or the side.
- Arms only go where the machine has a terminal of that conductor (`CircuitPart#hasPortAt`), as before: a machine with defined terminal
  faces (a breaker's line and load faces, a multiblock's terminal box) is only reached there; one with none takes the run on any face.
- **Pipes**: each side of a pipe is `false` (no arm), `true` (to a pipe) or `flange` (to a machine's nozzle, drawn as a bolted flange
  pair and weld-neck hub). Old saves load as before, `true` meaning an arm. A welded fitting is drawn only where a run turns, branches or
  ends; a straight run is plain pipe. Valves on a machine get the same flange. Tubing and network cable just plug in.
- **Receptacle and plug**: the 5-15 and 5-20 sit in a 4 in square box; every receptacle and plug is drawn at 1.8 times true size so it
  reads next to its (much exaggerated) wire. A plug adapter facing the receptacle's front (the receptacle on the far wall, floor or ceiling
  of the next cell) is `seated`: its plug is pushed into the receptacle's face and its cord runs back to the adapter's moulded box.

A pre-existing bug made runs set by command or blueprint look broken: a wire, cable or pipe that set its own arms while being placed
stopped the game's neighbour update for that placement, so the older neighbour never grew its arm back. `onPlace` now sets the arms
with a neighbour update.

## Single-phase cable
`wildcatter:single_phase_cable`, low tier only: one insulated block carrying hot (A), neutral (N) and ground (G), as the sheathed
non-metallic cable of house wiring is. It is the three-phase cable block with three conductors: it joins conductor by conductor (a
lamp, turbine, receptacle or breaker on the conductors each has; a single wire only on its own; never on B or C). Its ground is tied
to earth at every piece, as a ground wire is. Sizes are the building wire's, 12 AWG to 500 kcmil.
- **Recipe:** one hot wire, one neutral wire (both wool-wrapped or both plastic, of one size) and one ground wire make one cable of that
  size; bigger sizes from the next smaller, as for every conductor.
- **Breakers and in-line fuses** now pass the neutral and ground straight through (unswitched) on a one-pole breaker as on a
  three-pole one, so a cable's neutral and ground carry on past a breaker in its run. A separate neutral wire that ends on a breaker's
  line or load face now joins through it.
- **First power:** Light a lamp takes four hot and four neutral wires or four single-phase cables (`#wildcatter:lamp_hot_wiring`,
  `#wildcatter:lamp_neutral_wiring`). Saves keep their progress (tasks are kept by position).

Proof: NeoForge `single_phase_cable_lamp`, `single_phase_cable_one_conductor`, `wire_undersized`, `fittings_on_placement`; Fabric client
`wireLook` (screenshots).
