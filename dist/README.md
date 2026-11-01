# Wildcatter test build

`wildcatter-0.1.0.jar` is the latest build for testers. It is updated with each commit that changes the game.

## Installing
Put the jar in your instance's `mods` folder next to:
- Minecraft 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API
- Java 25

## What's in it
- Power from a 120 V starter turbine up to a 138 kV substation.
- Gensets, a 1 MW steam plant with its cooling tower, oil wells and a refinery.
- The gensets, the steam plant, the cooling tower, the substation yard, the generator step-up transformer, the overhead line (wood
  poles, crossarms, insulators, H-frames), the pole transformer, the pad-mounts, the unit substation, the motor control center, the
  boiler and starter turbine, the drilling rig, the pumpjack and the wellheads are built at their real size (1 block = 1 m). The
  pumpjack's beam rocks and the rig's drill pipe turns while they work. A late-game 1,000 hp land rig, fed 4.16 kV, is the only
  rig that reaches the new deep fields; well casing can't be broken. Breakers, receptacles, lamps and heaters are drawn at
  their real size and mount on the wall, floor or ceiling you click. Overhead spans are measured at 1 m per block.
- The lease: wells now bring up oil, water and gas together. A separator splits them; oil goes to stock tanks, gas to engines,
  burners and a flare, and produced water to a water tank, an injection pump and a disposal well on a dry hole. Each run of steel
  pipe carries one product; the Power Meter on a pipe says what, or BLOCKED where two meet.
- The gas plant: a 4.16 kV gas compressor, a glycol dehydrator, an NGL chiller with its refrigeration compressor and a
  stabilizer turn separator gas into pipeline gas, LPG (in an LPG bullet) and natural gasoline. The steam plant burner now
  needs pipeline gas.
- Refining: a 1,000 bbl/day topping unit (early) and a 20,000 bbl/day crude unit (late) split crude into LPG, gasoline,
  kerosene, diesel and residue. A steam cracker and a polyethylene reactor make plastic sheets from LPG or gasoline. The old
  one-block refinery is gone: in an older world it disappears.
- A 16.5 MW gas turbine at 13.8 kV, with a heat recovery steam generator and a 5 MW steam turbine for combined cycle, a 25 MVA
  step-up to 138 kV, generator bus duct and breakers, and a load bank for commissioning.
- The Field Manual item explains how to build each machine.

## Older worlds
A world made with an older build loses some machines' parts when it loads in this one. Their layouts changed when they
went to real size, so their parts drop as items. A chat message says where. Rebuild those machines. Old one-block poles and crossarms keep working with their spans, but new lines use the true-size poles. Old one-block MCC buckets drop as bucket items: click them into a starter section. An old pumpjack becomes an oil wellhead
and drops as an item: click the wellhead with it to set it over the well. Since 5a a pumpjack's stream needs a separator before a
refinery, and gas gensets need separated gas (a gas well's wet gas is refused); fireboxes still burn a well's stream directly.
