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

## How many turbines (the `dry_type_runs_rig` scene)
A small steam turbine is rated by the steam one boiler makes (`Tuning.BOILER_STEAM_PER_TICK`, 2 kW), and an island whose load stays
above its generators' rating falls in frequency and trips them (docs/ELECTRICAL_MODEL.md, "Frequency and supply/demand"). The rig is a 30 kW drive
motor started direct on line, drawing six times its current at PF 0.35 for its start: about 53 kW at the motor while it starts,
with the 480 V side near 390 V (82 %). Running, it takes 27.6 kW at 460 V.

Measured in the scene, turbines in a row, touching (each turbine's own terminals join the next), the dry-type against the last:

| Turbines | Boilers each | Result |
|---|---|---|
| 8, 16, 24, 32 | 1 | trip on frequency within 2 s of the start: the start steps the load from nothing to near or past the turbines' rating, and the frequency falls below 57 Hz for a second |
| 24, 33 | 2 | trip the same way (a second boiler adds steam, not rating) |
| 34, 35, 36, 38 | 2 | start the rig (drilling 4 s after the switch closes), 92 % speed, held for 40 s |
| 40, 48 | 1 | start it, then starve: the turbines nearest the dry-type deliver most of the load and one boiler cannot keep up (40 turbines at 30 % speed after 40 s, 48 at 79 %) |
| 64, 80 | 1 | start it and hold it (89 %, 86 %) |

- **To start the rig and keep it drilling:** at least 34 turbines, each between two firebox boilers; 36 to be safe. The scene runs
  24 (trip) and 36 (drills).
- The quest text and the Field Manual page say this.

## Quest
"Step up to 480 V" in Three-phase power: the dry-type transformer and `wildcatter:transformer_energised`. The drilling rig's quest
requires it.
