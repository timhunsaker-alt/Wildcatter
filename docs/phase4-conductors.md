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
