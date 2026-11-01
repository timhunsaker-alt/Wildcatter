# The lease: separator, tank battery, flare and water disposal (APPROVED 2026-10-31 with the Phase 5a plan)

What stands between a well and its buyers. Wells now flow a real mixed stream, and the lease splits it, stores the oil, sells or
burns the gas, and puts the water back underground. Each part is a true-size big part (README, "Big parts") placed on its own and
joined by steel pipe: like the well site, the lease is not one machine with a controller, and no part needs the others to work.
Values are approximate, drawn from typical equipment of each class.

## The well stream
| Well | Stream | Values (real basis) |
|---|---|---|
| Oil (pumpjack) | **well fluid**: oil, water and gas | The pumpjack lifts liquid at its old rate (0.5 L a tick at full pressure and voltage). Water cut 10 % when the field is new, rising in a straight line to 80 % as it drains (real fields run past 90 %). Gas-oil ratio 100 m3 of gas per m3 of oil (about 560 scf/bbl, a typical light oil). Only the oil counts against the field. |
| Gas (Christmas tree) | **wet gas**: gas, condensate and water | Condensate 10 bbl per million scf (0.056 L per m3), water 2 bbl per million scf (0.011 L per m3). The tree still flows only while something takes its gas. |

Game wells flow about a hundred times a real stripper well; that rate was already in the game and stays. The ratios are real.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `separator` | 2 x 2 x 4 | Horizontal three-phase production separator, 36 in x 10 ft, 125 psi working pressure, on a skid with its pneumatic level controllers, dump valves, sight glasses, relief valve and gauges. No power: the vessel's pressure and the level controllers' gas do the work. It handles up to 20 L/s of liquid (four pumpjacks) and 1.5 m3/s of gas (about 4.5 million scf a day). It holds 800 L of oil, 800 L of water and 10 m3 of gas. |
| `stock_tank` | 4 x 6 x 4 (drawn round) | 400 bbl API 12F welded steel stock tank, 12 ft across and 20 ft tall: 64,000 L. Thief hatch, gauge board, ladder and walkway on top. Holds one product, set by its first fill (crude, condensate sold as crude, or diesel). |
| `water_tank` | 4 x 6 x 4 (drawn round) | The same 400 bbl tank in fiberglass for produced water: 64,000 L. |
| `flare_stack` | 1 x 10 x 1 | Elevated flare 10 m tall: the knockout pot at its foot, the riser, the flare tip with its pilot. |
| `injection_pump` | 2 x 1 x 6 | Horizontal multistage centrifugal pump (H-pump) for saltwater disposal: a 100 hp (75 kW) 480 V three-phase motor at 3,560 rpm (2,960 on IEC worlds, read from the standard), thrust chamber and the pump barrel. About 10 L/s at 6 MPa (870 psi): 60 kW of water power at 80 % efficiency. |
| `disposal_wellhead` | 1 | Saltwater disposal (SWD) wellhead: casing head, tubing head, the injection wing with its check valve and gauge, about 1 m tall. It stands in the hole cell of a dry hole (on the top casing), and takes water from an injection pump. |

## Footprints and nozzles (facing north; x east, z south; M is the main cell)
Separator, the stream coming in at the front (north) end:
```
layer 0      layer 1        I: inlet, the north face of either front cell, either layer
  z0  M .      I I          G: gas out, the top face of either back cell of layer 1
  z1  . .      . .          O: oil out, the south face of either back cell of layer 1
  z2  . .      . .          W: water out, the south face of either back cell of layer 0
  z3  W W      O O + G on top
```
Stock and water tanks: 4 x 4 on the ground, 6 high. Pipe joins the side faces of the bottom ring. A bucket works on any cell.
Flare: one column 10 high; gas comes in through the side faces of the bottom cell.
Injection pump, the motor at the front:
```
layer 0      T: the motor's terminal box, the north and west faces of the main cell (480 V three-phase)
  z0  M .    z0..z1 motor, z2 thrust chamber, z3..z5 the pump barrel
  z1  . .    D: discharge, the top face of either cell at z3
  z2  . .    S: suction, the south face of either cell at z5
  z3  D D
  z4  . .
  z5  S S
```

## A typical lease (not a fixed layout; pipe may run any way)
```
  pumpjack -> [W] ===> separator ==gas==> gas genset / firebox / steam plant burner
                        |   |                \==> flare stack (takes only what is left)
                        |   +==oil==> stock tank(s) ===> refinery
                        +==water==> water tank ===> injection pump ===> disposal wellhead (on a dry hole)
```

## How it works
- **Pull, one product per run.** Each joined run of steel pipe carries one product, the one its sources put on it: wells (well fluid
  or wet gas), separator nozzles, tanks, a refinery's diesel. If two products meet on one run, nothing flows on that run and the
  Power Meter on any of its pipes says BLOCKED and which two met. Machines pull what they need from the run: process outlets first,
  then tanks. A tank fills only from process outlets, so two tanks on one run never trade.
- **Separator.** It pulls well fluid from the pumpjacks on its inlet run, and wet gas from the gas wells there while its gas side is
  under half full. It splits the stream: oil out of the oil nozzle (a gas well's condensate goes this way, as crude), water out of
  the water nozzle, gas out of the top.
- **Gas.** Fireboxes, gas gensets and the steam plant's burner take gas from the gas run. A flare on the run takes only what the
  separator holds above three quarters full: its back-pressure valve to the flare opens on high pressure. With nowhere for the gas
  to go the separator fills and shuts in: SHUT IN, gas has nowhere to go. A shut-in separator takes nothing from its wells, and the
  pumpjacks stop when their flowline backs up.
- **Oil.** It goes to stock tanks, or straight to a refinery. Full tanks shut the separator in the same way. Crude buckets come
  from a stock tank now (and pour into one), not from the pumpjack.
- **Water.** It goes to a water tank. The injection pump runs when there is water on its suction run and a disposal wellhead on its
  discharge run, and pushes 10 L/s at full voltage into the disposal well. The rock takes it all. A level switch starts it with
  2,000 L waiting and stops it when the water is gone.
- **Wet and dry gas.** Gas engines and the steam plant burner need separated gas: on a run of wet gas they say "wet gas: put a
  separator between the well and the engine". Fireboxes burn wet gas or crude straight from a well, as before.
- **Breaking.** Each part keeps what it holds while it stands and loses it when broken (a broken tank spills). Old worlds: crude in a
  pumpjack's tank becomes well fluid in its flowline.

## Added while building (5a)
- The stock and water tanks are drawn 5.8 m tall (19 ft) so the walkway rail stays inside their 6 m footprint; they still hold
  64,000 L, the 400 bbl rating.
- The injection pump's level switch: it starts with 2,000 L waiting (about 0.2 m up a 12 ft tank) and runs the water down, so it
  does not start and stop every few seconds on a trickle. A real SWD pump runs off a high and a low level switch the same way.
- A refinery takes crude on its base's sides and bottom and gives its diesel off the top (a column, or the base's top face), so
  its crude and diesel are two runs.
- Gas wells' water is 0.0112 L per m3 (2 bbl per million scf exactly).

## Game test
`lease`: an oil field. A powered pumpjack's wellhead to a separator; oil to a stock tank, gas to a gas genset and a flare, water to a
water tank, an injection pump and a disposal wellhead on a dry hole's casing. The water cut reads 10 %, the stock tank fills, the gas
genset runs on separator gas, the flare lights when the genset stops, the separator and pumpjack shut in with the flare gone too, the
injection pump keeps the water tank down, a bucket on the stock tank gives crude, and a run joining oil to water reads BLOCKED. The
gas genset on a run straight from a gas well reads wet gas.
