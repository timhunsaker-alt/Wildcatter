# Soft starter: 480 V cabinet, MCC bucket, 4.16 kV cabinet (Phase 10a; values approved 2026-11-02)

## Real-world counterpart
A **solid-state reduced-voltage starter**:
- **Power path:** each phase passes through a pair of inverse-parallel SCRs (thyristors). Firing them later in each half cycle
  lowers the voltage the motor sees.
- **Current limit:** the controller holds the line current at a set multiple of the motor's full-load amps while the motor
  accelerates.
- **Bypass:** once the motor is near speed, a bypass contactor closes across the SCRs so they carry nothing while it runs.
- **Overload relay:** the same class 10 relay as a combination starter.

Counterparts:
- **480 V:** Allen-Bradley SMC-50, ABB PSTX, Siemens 3RW55: a floor-standing combination enclosure about 0.6 m wide and
  1.8-2.0 m tall, or a 2-space MCC bucket.
- **4.16 kV:** Allen-Bradley SMC Flex MV (1503E), Motortronics MVC4: a 36 in (0.91 m) wide, 90 in (2.29 m) tall cabinet. Inside it:
  - a load-break isolation switch and power fuses;
  - three SCR stacks;
  - a vacuum bypass contactor;
  - the control door.

A soft starter lowers the starting current but also the torque, which goes with the square of the voltage. A motor started at
3.5 x full-load amps (instead of about 6 x locked-rotor) gets (3.5/6)² = 34 % of its locked-rotor torque, so it takes about 3 times
as long to reach speed.

## Values *(proposed; approved with the 10a plan, 2026-11-02)*
| Item | Value | Basis |
|---|---|---|
| Current limit dial | 2, 2.5, 3, 3.5 (default), 4, 4.5, 5 x full-load amps | SMC-50 and SMC Flex current-limit start: 50-600 %, 350 % typical |
| Bypass | closes when the line current falls to 1.25 x full-load amps (the motor is up to speed) | at-speed detection |
| Full-load amps | the overload dial, as on the combination starter | |
| Overload relay | class 10, the combination starter's | |
| Short-circuit protection | 480 V: the motor circuit protector at 13 x the setting; 4.16 kV: power fuses, modelled as a trip at 13 x | NEC 430.52 |
| 480 V overload dial | the starter's 1.5-63 A, plus 96, 124, 156, 180, 240 and 302 A on the cabinet (not the bucket) | NEC 430.250 full-load current at 460 V: 75, 100, 125, 150, 200, 250 hp |
| 4.16 kV overload dial | 10, 16, 25, 32, 40, 50, 63, 80, 100, 125 A | IEC R10 steps; the 250 hp charge pump is 32 A and a 500 kW coolant pump 87 A |
| SCR losses | none modelled (a bypassed SCR carries nothing; about 1 V drop while ramping) | |

## How it works in the circuit
1. **Stopped:** the contactor is open.
2. **Ramping:** the line connects to the motor through one series impedance per phase. Each tick the starter changes that impedance
   so the measured current stays at the dial setting. The motor's start progress goes with (V/Vn)² while the starter ramps, so it takes longer
   at the lower voltage (`circuit/Motor`; since 10d1 a direct start goes with V² too).
3. **Bypassed:** once the current falls to 1.25 x, the bypass closes (the closed-contact resistance), and the readout says
   BYPASSED.
4. **Overload:** the relay heats as on the combination starter. A start that never ends (a stalled or overloaded machine) trips
   it.
5. **Lost supply:** the starter drops out and waits for a new start.

## Parts
| Char | Block id | Real part | Cells (W x H x D) |
|---|---|---|---|
| `s` | `wildcatter:soft_starter` | 480 V soft starter in a floor-standing enclosure | 1 x 2 x 1 |
| (item) | `wildcatter:mcc_soft_starter_bucket` | 480 V soft-starter bucket, clicked into an MCC starter section's empty space | one space |
| `S` | `wildcatter:soft_starter_mv` | 4.16 kV soft-starter cabinet | 1 x 3 x 1 |

Each is one part, not a multiblock. The 4.16 kV cabinet draws its isolation switch, SCR stacks and vacuum contactor behind the
door's window.

## Layout (seen from the front; 1 block = 1 m)
```
480 V cabinet      4.16 kV cabinet
   s                  S      <- top cell: line in (top face, or back)
   s                  S
                      S      <- bottom cell: motor out (back)
```
- **480 V cabinet:**
  - line in (A, B, C, G, from a 480 V cable) on the top cell's top and back faces;
  - motor out on the bottom cell's back face;
  - the door, keypad and dial on the front.
- **480 V MCC bucket:** the line comes from the MCC bus and the motor leads are on the back of its own cell, as for a starter bucket.
- **4.16 kV cabinet:** line in (from a 4.16 kV cable) on the top cell's top and back faces; motor out on the bottom cell's back face.

## Controls
- Right-click the front to start and stop it, or reset a trip.
- Redstone runs it, as on the combination starter.
- Sneak and right-click steps the overload dial.
- Sneak and right-click with the Power Meter steps the current-limit dial.
- The readouts:
  - "RAMPING, limit 3.5 x 32 A = 113 A, 112 A";
  - "BYPASSED, running 31 A";
  - "TRIPPED (...)".

## Rotation, forming, removal
- Four horizontal facings. There is nothing to form.
- Breaking a cabinet drops it, and its motor stops.
- Breaking an MCC section drops its soft-starter buckets as their own item.

## Game test (`drives`)
- **4.16 kV:** a 250 hp charge pump on one 500 kW set:
  - direct on line, it drops the island (as today);
  - behind the 4.16 kV soft starter, it starts with the line current held at the dial, then BYPASSED.
- **480 V:** an MCC soft-starter bucket starts a 480 V motor and bypasses.
