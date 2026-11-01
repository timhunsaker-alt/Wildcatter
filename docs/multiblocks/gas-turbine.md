# The 16 MW gas turbine and the 13.8 kV tier (APPROVED 2026-10-31 with the Phase 6a plan)

An industrial gas turbine generator set of the Solar Titan 130 class, built from distinct true-size parts that form a unit by
touching, with the turbine package as controller. It makes 13.8 kV (11 kV on IEC worlds), the generator-terminal level of
ELECTRICAL_MODEL.md, which steps up to 138 kV through a 25 MVA GSU. Values are approximate, drawn from typical equipment of this
class. The combined cycle (HRSG and steam turbine) is in combined-cycle.md.

## The 13.8 kV tier
- `Voltage.GENERATOR`: 13.8 kV (11 kV IEC), read from the standard like every tier.
- `bus_duct_gen`: non-segregated phase bus duct, 15 kV class, 1,200 A (a generator this size makes about 860 A, more than one cable
  carries). It joins like the substation's station bus.
- `gen_breaker`, 2 x 3 x 2: the generator circuit breaker, a 15 kV vacuum breaker, 1,200 A, 40 kA, in its own enclosure against the
  generator's terminal end. It is the generator's synchronizing breaker: it closes when the generator has synchronized and opens
  when it trips or stops. Its front faces the generator; the bus duct leaves its back.
- `gsu_25`, 5 x 5 x 4: a 25 MVA generator step-up, 13.8 kV delta / 138 kV grounded wye, 8 %Z, ONAN/ONAF. The 13.8 kV comes in
  low on its back; the 138 kV bushings stand on its cover.

## Gas turbine parts (each drawn facing north; x east, z south; main cell at its north-west bottom corner)
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `gas_turbine` (controller) | 4 x 4 x 10 | The package: acoustic enclosure over the gas generator and power turbine, and the reduction gearbox (power turbine about 11,000 rpm, output 1,800 rpm). Its starter, a 300 hp (224 kW) 4.16 kV motor, is on this skid: its terminal box on the west face of the cell at x 0, z 2. Fuel comes in on the west face of the cell at x 0, z 6. The air inlet is at its front (north) end, the exhaust at its back (south) end, the drive at its west side |
| `gt_inlet_house` | 4 x 6 x 4 | Air filter house and inlet silencer, touching the turbine |
| `gt_generator` | 4 x 4 x 6 | 20.6 MVA, 13.8 kV, 0.8 PF, four-pole (1,800 rpm; 1,500 IEC), touching the turbine. Its terminals are the south face of its back bottom cells |
| `gt_exhaust_stack` | 3 x 12 x 3 | Simple-cycle exhaust stack, touching the turbine. An HRSG in its place makes combined cycle |
| `load_bank` | 3 x 3 x 6 | Resistive load bank at 13.8 kV, 0 to 10 MW in 1 MW steps (right-click up, sneak and right-click down). Its terminals are the north face of its front bottom cells |

Forming: the inlet house, the generator and a stack or HRSG touch the turbine; the turbine's readout names what is missing.

## Values
- 16.5 MW (ISO) at a heat rate of 10,230 kJ/kWh (35 %): 46.9 MW of fuel at full load, 1.23 m3/s of pipeline gas or 1.3 L/s of
  diesel or kerosene (36 MJ/L). At no load it burns 30 % of its full-load fuel, rising in a straight line to full.
- Exhaust 50 kg/s at about 495 C.
- Fuel: pipeline gas, diesel or kerosene, off steel pipe at its fuel nozzle. Separator gas or wet gas reads "needs pipeline gas".
- Start: right-click the turbine. The starter (4.16 kV the whole way) purges it at a quarter speed for 60 s, it lights on fuel,
  and it runs up to full speed in 120 s; the starter drops out at 60 % speed. The real start takes about ten minutes; this is
  shortened for play. With no 4.16 kV at the starter it says STARTER: no 4.16 kV and stops.
- Governor 4 % droop, inertia H = 3 s. It synchronizes and closes its generator breaker the way the gensets do, onto a dead bus at
  once or onto a live one in step.

## Added while building (6a)
- The generator breaker carries the bus's voltage transformers (wye-grounded, 1 MOhm a phase), as metal-clad switchgear does. Without
  a ground reference the 13.8 kV bus and the GSU's delta floated while the generator was off, and the circuit could not be solved.
- At part load the plant is less efficient (33 % at 10 MW), as a gas turbine is; the test checks the 40 %+ near full load on two
  load banks.
- No ramp limit yet: the turbine takes load as fast as the governor shares it.
- The no-load fuel, 30 % of full-load fuel, was not in the plan: it is a typical industrial gas turbine's part-load curve, and it is
  what makes part load inefficient. Wet gas on the fuel nozzle shows WRONG GAS in the readout; the test does not check it.

## Game test
`gasTurbine`: a diesel set and a backward unit substation give the starter 4.16 kV; kerosene from a stock tank; the turbine
purges, lights, runs up, closes onto a dead 13.8 kV bus duct and energizes the GSU's 138 kV side; a load bank steps to 10 MW and
it carries it at 60 Hz, burning fuel at its heat rate. Without 4.16 kV the start fails; wet gas is refused.
