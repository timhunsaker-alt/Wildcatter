# The dry-type step-up transformer (Phase 21 follow-up, Tim 2026-11-14)

Tim's answer to "how does a new world get its first 480 V three-phase before it has gas or diesel?": a step-up transformer fed
from firebox steam turbines.

## Real counterpart
A 75 kVA general-purpose three-phase dry-type transformer, NEMA ventilated enclosure, about 5 % impedance: the box that hangs on the
wall of a shop and makes 208Y/120 V from a 480 V service. Fed the other way round, from its 208Y/120 V side, it gives 480 V: that is
how a small site with only a 208 V service runs one 480 V motor. Manufacturers allow it on units of this size; the inrush is higher
when energised from the low-voltage side.

| Value | Used | Basis |
|---|---|---|
| Rating | 75 kVA | the common general-purpose size; the same rating as `padmount_208` |
| Impedance | 5 %Z, X/R about 5 | a ventilated dry-type of 75 kVA is 4.5 to 6 % |
| Low side (fed) | the LOW tier, three-phase: 208Y/120 V (NA), 400Y/230 V (IEC) | the steam turbines' tier |
| High side (out) | the INDUSTRIAL tier: 480Y/277 V (NA), 400Y/230 V (IEC) | the rig's and the pumpjack's tier |
| Connection | grounded wye on both sides, as the pad-mounts | see "Simplified" below |
| Size | one block: about 0.76 W x 1.0 H x 0.6 D m | a 75 kVA ventilated enclosure (about 30 x 40 x 24 in) |
| Overload | IEEE C57.109, as every rated transformer | docs/ELECTRICAL_MODEL.md, "Short-time rule" |

The voltages come from the world's standard and are never written in code: the block joins `Voltage.LOW` and `Voltage.INDUSTRIAL`.
Under IEC both tiers are 400Y/230 V, so there it is a 1:1 isolation transformer; it still joins the two tiers, and the Field Manual
says so.

### Simplified
- A real general-purpose unit is delta on its 480 V side and grounded wye on its 208Y/120 V side. Here it is grounded wye on both,
  as the pad-mounts are: the rig and the pumpjack use A, B and C only, so the 480 V neutral changes nothing they do, and an
  ungrounded delta fed from the low side would leave its 480 V side floating in the solve.
- The higher inrush on energising from the low side is not modelled (no transformer's magnetising inrush is).

## The block
- `wildcatter:dry_type_transformer`, a `TransformerBlock` built with its three-phase constructor (`TransformerBlock.threePhase`:
  rated VA, %Z, no delta) and the existing `TransformerBlockEntity`.
- One block, facing the way the player faces when placing it. As every one-block transformer: the INDUSTRIAL side on its front and
  top, the LOW side on the other four faces. Each side carries A, B, C and N.
- Power flows either way through every transformer in the solve (an ideal transformer behind its series impedance), so feeding the
  low side and taking power from the high side needs no new code. The `dry_type_runs_rig` scene checks it.
- Fires `wildcatter:transformer_energised` when its high side is live, as every transformer does.

## Recipe (by hand, before the plastic gate)
```
S W S      S  steel ingot
C I C      W  wool-wrapped wire (red)
S W S      C  block of copper (the windings)
           I  block of iron (the core)
```
No pole transformer and no cable in it.

## A turbine on a firebox is rated by its fire (Tim, 2026-11-14: "Rate turbine by its fire")
Answer to decision 17. Written before the code.

**Real counterpart:** a small packaged steam turbine-generator, single-stage and back-pressure, about 100 kW, three-phase at the low
tier, on a skid with its governor: the size a sawmill or a plant's waste-heat boiler drives.

| Value | Used | Basis |
|---|---|---|
| Firebox heat | 0.36 MW (`Tuning.FIREBOX_HEAT_WATTS`, unchanged) | its coal fire |
| Fuel to electricity | 25 % (`Tuning.FIREBOX_ELECTRIC_SHARE`) | the figure the steam plant uses: "25 % overall from fuel to electricity" (docs/multiblocks/steam-plant.md, Build values) |
| Rating of a turbine on a firebox boiler | 0.36 MW x 25 % = 90 kW (`Tuning.FIREBOX_TURBINE_WATTS`) | the fire it can turn into power |
| Steam a firebox boiler makes, at most | 90 a tick (`Tuning.FIREBOX_STEAM_PER_TICK` = 90 kW / 50 J / 20) | only what the heat rate gives; at 50 J a steam that is 90 kW |
| Turbine on a campfire boiler | 2 kW, 2 steam a tick: unchanged | a campfire |
| Internal resistance | 0.5 ohm x 2 kW / rating: 0.011 ohm at 90 kW | the same per-unit impedance (2.3 %) as the 2 kW set |

- A turbine is rated 90 kW when any boiler it touches stands on a firebox (the nameplate: what is built, not whether it is lit); its
  island's frequency, its stator and its sag all use that rating.
- **Fuel follows the load.** A firebox burns only while the boiler above wants heat (water in it and room for steam). Making up to 90 a
  tick, a lightly loaded boiler is full after a tick of fire and the fire waits until the turbine draws it down, so a firebox at today's
  2 kW load burns about 1/45 of the fuel it burned before, and an idle one none. Steam drawn still follows the power used.
- **Water:** 20 steam a unit of water, unchanged; at the full 90 kW a boiler of 4,000 uses its water in 45 s unless its drain and a pump
  refill it. At loads of 2 kW and less nothing changes.
- **Existing worlds:** a firebox set only gains headroom. At its present load it makes the same power from the same steam; its voltage sags
  less (0.06 V instead of 2.8 V at 2 kW) and its fire burns less often. Campfire sets are unchanged.

## How many sets (the `dry_type_runs_rig` scene)
The rig is a 30 kW drive motor started direct on line, drawing six times its current at PF 0.35 for its start: about 57 kW at the motor,
with the 480 V side near 410 V. Running, it takes 28.4 kW at 467 V. An island whose load stays above its generators' rating falls in
frequency and trips them (docs/ELECTRICAL_MODEL.md, "Frequency and supply/demand").

With the turbines rated by their fire (above), measured in the scene, each set a turbine with one boiler beside it:

| Sets | Boiler on | Result |
|---|---|---|
| 1, 2 | a firebox | start the rig 3 s after the switch closes and hold it at 95 % speed (59.8 Hz); one set's boiler uses about 2 units of water a tick |
| 1, 4 | a campfire | trip on frequency within 2 s of the start |

- **One firebox set starts the rig and keeps it drilling.** The scene builds one firebox set (drills) and one campfire set (trips).
- Before Tim's answer, when every small turbine was rated 2 kW, it took 34 turbines with two firebox boilers each (decision 17).
- The quest text and the Field Manual say this.

## Quest
"Step up to 480 V" in Three-phase power: the dry-type transformer and `wildcatter:transformer_energised`. The drilling rig's quest
requires it.
