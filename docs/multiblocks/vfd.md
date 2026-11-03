# Variable-frequency drive: 480 V cabinet, MCC bucket, 4.16 kV lineup (Phase 10a; values approved 2026-11-02)

## Real-world counterpart
A **variable-frequency drive (VFD)** has three stages:
- **Rectifier:** turns the line into DC on a capacitor bus.
- **Inverter:** builds a new three-phase supply from the DC for the motor, at whatever frequency and voltage it is told.
- **Ramp:** to start, it ramps the frequency and voltage up together (volts per hertz held). The motor runs at a small slip all the
  way up, so it never draws locked-rotor current: it accelerates at about its rated current.

The line sees only the real power the motor turns, at the rectifier's power factor (about 0.95 for a 6-pulse diode front end).
Because a drive regulates its output, it takes constant power: when the line voltage sags, it draws more current. Below about
90 % voltage its DC bus is too low and it stops.

Counterparts:
- **480 V:** ABB ACS880-07, Allen-Bradley PowerFlex 755: a cabinet about 0.6-0.8 m wide, 2.0 m tall and 0.65 m deep, or an MCC
  drive bucket up to about 100 hp.
- **4.16 kV:** Siemens Perfect Harmony GH180, ABB ACS580MV: a lineup about 4-5 m wide, 2.6 m tall and 1.3 m deep, in sections:
  - a phase-shifting input transformer (its many secondaries feed the cells and cancel the line harmonics);
  - the power cells (each a small rectifier, DC bus and H-bridge, stacked in series on each phase);
  - an output and control section.

## Values *(proposed; approved with the 10a plan, 2026-11-02)*
| Item | Value | Basis |
|---|---|---|
| Line power factor | 0.95 | 6-pulse diode front end; the multi-pulse MV drive is about 0.96 |
| Efficiency | 97 % | typical LV and MV drives at rated load: 96.5-98 % |
| Ramp | 0 to full frequency and voltage in 10 s (volts per hertz held) | a common default accel time |
| Current limit | 1.5 x the motor's full-load amps; held for longer than 60 s it trips on overcurrent | heavy-duty rating, 150 % for 60 s |
| Undervoltage | stops below 90 % line voltage, starts again when it is back (`Tuning.LAND_RIG_UNDERVOLTAGE`) | DC bus undervoltage, auto restart |
| Full-load amps | the overload dial, as on the soft starter (same steps) | |
| Ratings | MCC bucket up to 124 A (100 hp); 480 V cabinet up to 302 A (250 hp); 4.16 kV lineup up to 125 A (about 900 hp) | |
| Overload | the drive's electronic motor overload, class 10, as the starter's | |

## How it works in the circuit
- **Line side:** a balanced star at PF 0.95 drawing the output watts / 0.97 (from the last tick). Its impedance comes from the
  measured voltage, so the power holds as the voltage sags. This is `circuit/DriveLoad`, which the existing drive houses
  (rig, header house, frequency converter, rectifier) also take constant power from.
- **Motor side:** a three-phase source of its own, marked as a drive output (`Circuit.markDriveOutput`). Its voltage goes from 0 to
  full over the ramp.
- **The motor:** a motor fed from a drive output (`Circuit.driveFed`) draws accelerating current, its rated current at the drive's
  voltage, instead of locked-rotor current. It runs once the drive is at full voltage.
- **Overcurrent:** above the current limit, the drive holds its voltage where it is (the ramp pauses). If it stays there more than
  60 s, it trips.
- **Frequency:** the solver works at one frequency per island. The drive's output frequency is shown (ramp x the standard's), and
  the motor's turning parts follow it, but the motor side is solved at the island's frequency.

## Parts
| Char | Block id | Real part | Cells (W x H x D) |
|---|---|---|---|
| `d` | `wildcatter:drive_cabinet` | 480 V drive cabinet | 1 x 2 x 1 |
| (item) | `wildcatter:mcc_drive_bucket` | 480 V drive bucket, clicked into an MCC starter section's empty space | one space |
| `T` | `wildcatter:drive_mv_transformer` | 4.16 kV drive: phase-shifting input transformer section | 2 x 3 x 2 |
| `P` | `wildcatter:drive_mv_cells` | 4.16 kV drive: power cell section (cells stacked behind the door's window) | 2 x 3 x 2 |
| `C` | `wildcatter:drive_mv_control` | 4.16 kV drive: output and control section; **controller** | 1 x 3 x 2 |

The 4.16 kV drive is three distinct real sections that bolt together side by side, not a tiled block.

## 4.16 kV lineup layout (bottom layer first, facing NORTH; north, the doors' side, is the top row)
Each letter is a part's main cell. Its other cells follow from its size.
```
layer 0      layer 1      layer 2
T.P.C        .....        .....
.....        .....        .....
```
- 5 x 3 x 2 in all. Real: about 4.5 x 2.6 x 1.3 m.
- West to east (facing north, doors to the north): transformer, cells, control.
- **Terminals:**
  - line in (from a 4.16 kV cable) on the transformer section's west end face, bottom cells, or its top;
  - motor out on the control section's east end face, bottom cells.
- **Placing:** each section goes down touching the last, facing the same way. The control section checks the lineup (as the
  genset panel does), and its readout names a section that is missing or turned the wrong way.

## 480 V cabinet and bucket
```
480 V cabinet
   d     <- top cell: line in (top face, or back)
   d     <- bottom cell: motor out (back)
```
The MCC drive bucket takes the MCC bus, and its motor leads are on the back of its own cell.

## Controls
- Right-click the front (the control section's door on the 4.16 kV drive) to start and stop it, or reset a trip.
- Redstone runs it.
- Sneak and right-click steps the full-load amps dial.
- The readouts:
  - "RAMPING 42.0 Hz (70 %), motor 31 A, line 18 A at 0.95 PF";
  - "RUNNING 60.0 Hz, ...";
  - "UNDERVOLTAGE: stopped, 3,610 V";
  - "TRIPPED (overcurrent ...)".

## Rotation, forming, removal
- Four horizontal facings.
- The 4.16 kV lineup forms when all three sections are in place, facing the same way.
- Breaking any section drops that section, and the drive stops and reports it missing.
- Breaking a 480 V cabinet drops it.
- Breaking an MCC section drops its drive buckets as their own item.

## Game test (`drives`)
- **The charge pump:** a 250 hp charge pump on one 500 kW set through the 4.16 kV lineup:
  - line current stays at or below 1.5 x full-load amps;
  - line PF ≥ 0.94;
  - the pump runs at full output once the ramp ends.
- **Two coolant pumps:** two 500 kW coolant pumps on one three-set island, each on its own lineup, both run.
- **480 V:** an MCC drive bucket runs a 480 V motor.
- **Breaking:** breaking the cell section stops the drive and names the cells.
- **Constant power:** a rig drive house at about 95 % voltage draws about 1/0.95 of its full-voltage current.
