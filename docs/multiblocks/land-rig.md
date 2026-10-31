# Land rig (APPROVED 2026-10-31 with the Phase 4.5h plan)

The late-game drilling rig: a 1,000 hp AC land rig built from distinct true-size parts (README, "Big parts"), fed 4.16 kV
three-phase. It drills five times faster than the small mast rig (docs/multiblocks/well-site.md) and is the only rig that
reaches the deep fields. Values are approximate, drawn from typical rigs of this class (rated for about 3,000 to 3,700 m).

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `rig_substructure` (controller) | 9 x 6 x 9 | Box-on-box substructure: the drill floor 6 m (20 ft) up on two side boxes 2 m wide, the blowout preventer space between them. The hole is the ground cell under the rotary table, a gap in its footprint. The driller's console is on the floor. |
| `rig_mast` | 3 x 43 x 3, on the floor | 142 ft (43 m) mast, about 165 t (365,000 lb) hook load: four legs, girts and braces, the racking board 27 m up, the crown block |
| `rig_drawworks` | 6 x 3 x 3, on the floor | 1,000 hp (750 kW) AC drawworks: drum, AC motor, disc brakes |
| `top_drive` | an item, clicked into the mast | 250-ton AC top drive, 400 hp (300 kW), with the traveling block and hook |
| `mud_pump` (two) | 5 x 3 x 3 | 1,000 hp (750 kW) triplex mud pump, 120 strokes a minute, its AC motor on top and the belt guard over its sheave |
| `mud_tanks` | 3 x 3 x 12 | Active mud system, 60 m3 (about 380 bbl), the shale shaker on top |
| `rig_drive_house` | 12 x 4 x 4 | VFD house, a 40 ft container: a 2,500 kVA 4.16 kV / 600 V rig transformer and the drives for every motor |

## Layout (facing north; x east, z south; offsets from the hole H, the ground cell under the rotary table)
Each letter is a part's main cell. The V-door (where pipe comes up to the floor) is the north side.
```
ground (y 0):                                         drill floor (y 6, on the substructure):
  x: -6  -4    -1  0  1    3  4    6  8 10    14
z -5   .  .     .  .  .    .  .    T  .  .     .         A at (-1, 6, -1): the mast's north-west leg; its legs stand at
z -4   .  S     .  .  .    .  .    .  .  P     .           x -1 and 1, z -1 and 1, the top drive running down the middle
z  0   .  .     .  H  .    .  .    .  .  Q     .         W at (-3, 6, 2): the drawworks, behind the hole
z  7   D  .     .  .  .    .  .    .  .  .     .
```
- S: substructure, x -4..4, z -4..4; its side boxes x -4..-3 and 3..4 from the ground to y 4, the floor (y 5) across all of it.
- T: mud tanks, x 6..8, z -5..6, 3 high.
- P, Q: the two mud pumps, x 10..14, z -4..-2 and 0..2, 3 high.
- D: drive house, x -6..5, z 7..10, 4 high. Its 4.16 kV terminal is on its west end, the main cell's west face.

## Controller, rotation, forming
- The substructure is the controller and the origin: put it down first, its hole over the place to drill. The machine turns
  with it (four facings).
- The mast and the drawworks snap into their places when clicked on the floor. The ground parts snap into their places when
  clicked on the ground within 16 blocks of the substructure. The substructure's readout names the next missing part.
- The top drive is clicked into the formed mast and is kept by the substructure; breaking the substructure drops it.
- The controller checks every 20 ticks. Removing any part stops the rig; it forms again when the part goes back.

## How it works
- Right-click the driller's console (the substructure) to start or stop drilling.
- The drive house is a balanced three-phase load at 4.16 kV, power factor 0.95 (the drives' front end):
  drilling 1.9 MW (top drive 300 kW, two pumps at 750 kW, auxiliaries 100 kW); a connection 850 kW (drawworks 750 kW and the
  auxiliaries); formed but idle 100 kW.
- Power limit (a rig's power management system): the drives trim their load to keep the island at or above 99.2 % of the
  standard frequency (59.5 Hz, IEC 49.6 Hz). An underpowered rig drills slower instead of tripping the plant. Below 90 % voltage
  the drives stop on undervoltage, and they start again when it comes back.
- Drilling: 4 ticks a block at full power (the small rig takes 20), slower in proportion to the power it gets. It drills a 27 m
  stand (three 9 m joints), then makes a 15 s connection: the pumps stop and the drawworks hoists. Its depth has no limit: it
  drills to the first field under the hole, or to bedrock.
- Mud: each block drilled takes 80 L of water from the 60,000 L tanks (the 8.5 in hole is about 37 L a metre, plus surface
  losses). A pump on water pipe fills the tanks. With no water it stops: NO MUD.
- Strike: WELL COMPLETE. The right wellhead goes into the hole cell under the floor. Break the rig down to move it.

## Added while building (4.5h)
- `cable_mv`: 4.16 kV three-phase cable (A B C N G), the 5 kV class member of the existing MV cable family (1/0, 4/0, 500 kcmil).
  The drive house needs a three-phase 4.16 kV feed and there was none: the 4.16 kV tier had only single wires.
- The well casing is unbreakable, like bedrock (Tim, 2026-10-31): real casing is steel cemented into the rock, and a miner who
  tunnels into one knows it is a well. The fields are not blocks in the rock, so mining past a well takes nothing from it.

## Deep fields
A second grid of fields, separate from the shallow ones so they do not change: 128-block cells, half of them with a field,
16 to 24 blocks across in radius, the top between y -58 and -50. Oil holds 400,000 to 1,000,000 units, gas 1,000,000 to
2,500,000, with the same share of oil and gas. Where a shallow and a deep field overlap, a hole strikes the shallow one first.
The small rig stops 110 m below its main cell (TOO DEEP): every shallow field is in reach from near sea level (y 63 to -45 is
108 m), no deep one is. The seismograph hears deep fields as DEEP ECHO.

## Moving parts
The top drive's quill and the stand below it turn at 120 rpm (real speed); the top drive and traveling block ride down the mast
through each stand and back up in a connection. The drawworks drum turns in a connection. Each mud pump's motor sheave turns at
480 rpm while it pumps (shown at rpm / 20).

## Game test
`landRig`: a deep oil field under the hole; the substructure on a raised pad, each part clicked on and snapping in, the readout
naming what is missing until it forms; the top drive clicked in; a pump filling the mud tanks; a 500 kW genset through a unit
substation run backward to 4.16 kV. The rig power-limits (the frequency stays up), drills, makes a connection, strikes and
leaves an oil wellhead under the floor. Removing a mud pump stops it; with no water it says NO MUD. In `wells`, a small rig
stops TOO DEEP.
