# The centrifuge cascade (APPROVED 2026-11-01 with the Phase 7e plan)

Natural UF6 from the conversion plant's 48Y cylinders (conversion.md) is enriched in a cascade of gas centrifuges:
- product: **4.95 % U-235**, in 30B cylinders;
- tails: **0.25 %**, in 48Y cylinders.

The plant follows the National Enrichment Facility (Urenco USA, Eunice NM). The cascade's output is set by counting its centrifuges. This is the written exception to "never tile identical blocks" (ROADMAP, Phase 7), because a real cascade is exactly that.

Decided with Tim on 2026-11-01:
- **Machine:** a Urenco **TC-21 class** centrifuge.
- **Scale:** each machine does **1000×** its real separative work, at the real **50 kWh a SWU**. That makes it about 571 kW a machine, and the cascade a big MV load.

Sources:
- [N] NRC NUREG-1827, the SER for the National Enrichment Facility:
  - 3 MSWU a year;
  - 3 separations modules of 2 cascade halls of 8 cascades;
  - solid feed stations that heat 48Ys to sublime the UF6 without liquefying it;
  - product and tails take-offs that desublime it at sub-atmospheric pressure;
  - each year about 690 feed 48Ys, 350 product 30Bs and 625 tails 48Ys.
- [W] Wikipedia, *Zippe-type centrifuge*: the TC-21 rotor is 20 cm across, more than 5 m long, and runs at 770 m/s.
- [G] Glaser's separation factor of **1.74** for a 5 m TC-21.
- [E] 10 CFR 110 App. A/B: frequency changers for centrifuge stators, 600-2,000 Hz.
- [S] The SWU literature: 50-60 kWh/SWU for gas centrifuge plants. 100 SWU a year for a TC-21 is an informal figure *(unverified)*.
- NRC package certificates for the 30B: **2,277 kg** (5,020 lb) at no more than 5 wt % U-235, about 635 kg tare (1,400 lb), 760 mm x 2,070 mm.

A figure marked *(unverified)* is textbook, an estimate or a proposal.

```
48Y natural (hopper) -> FEED STATION ==UF6 header (steel pipe)==> CENTRIFUGES x n (top cells on the header)
                         480 V                 ||                    ^ 1,225 Hz along the header
                                                ||               FREQUENCY CONVERTER (12.47 kV)
                                                ++==> PRODUCT TAKE-OFF -> 30B of 4.95 % (hopper under it)
                                                ++==> TAILS TAKE-OFF   -> 48Y of 0.25 % (hopper under it)
```

## Numbers (`fluid/Enrichment.java`)
- **Value function:** V(x) = (2x-1) ln(x/(1-x)).
- **Feed:** F/P = (4.95-0.25)/(0.711-0.25) = **10.20 kg of feed U a kg of product U**. The tails are 9.20 kg.
- **Separative work:** **7.81 SWU a kg of product U**.
- **Stages:** with a stage factor of the square root of 1.74 [G], the cascade has 7.2 enriching and 3.8 stripping stages, so **11 stages**.
  - A cascade needs **at least 11 driven machines**, one a stage.
  - More machines widen the stages and add separative work.
- **A machine:** 100 SWU a year × 1000 is **11.4 SWU an hour**. At 50 kWh a SWU, that is **571 kW**.
- **The smallest cascade, 11 machines:**
  - 126 SWU an hour, 6.3 MW;
  - **16.1 kg of product U an hour**: a 30B (1,539 kg of U) in **about 96 hours**;
  - 164 kg of feed U an hour, about 60 % of one conversion plant: a 48Y of feed (8,453 kg of U) in about 51 hours;
  - 148 kg of tails U an hour: a tails 48Y in about 57 hours.
- **Motor frequency:** 770 m/s at a 0.1 m radius is 1,225 revolutions a second, so a two-pole motor wants **1,225 Hz** (derived). That is inside [E]'s band.
  - It is the machine's own frequency, made by the converter. It is not a grid frequency and does not come from the world's NA or IEC standard.
- **Feed heat:** UF6 sublimes at 48 kJ/mol, about 137 kJ/kg. The smallest cascade's 243 kg of UF6 an hour needs 9 kW.

## Parts (cells W x H x L, all sizes *unverified*)
| Part | Cells | Real basis and numbers |
|---|---|---|
| `centrifuge` | 1 x 6 x 1 | A TC-21 class machine [W]: a 20 cm rotor over 5 m in its vacuum casing, a magnetic bearing at the top and a two-pole motor at the foot.<br>• Its header connection is a steel pipe on any side of its **top** cell, where real machines meet their headers.<br>• It spins only while a frequency converter on its header drives it.<br>• Readout: its speed (m/s and rev/s), the Hz it is fed, its SWU an hour. |
| `frequency_converter` | 3 x 3 x 6 | An MV frequency changer lineup: input transformer, rectifier and inverter at **1,225 Hz**.<br>• Fed at **12.47 kV**; its box is on the west face of its main cell.<br>• It drives up to **12 machines** (6.85 MW) on the header at the south face of its back cells *(proposed)*. Their power runs along the header, as the ISR producers' does along theirs.<br>• It is the land rig's drive load at 0.95 power factor, with the same power management. It wants 571 kW a machine, and their speed follows the share it gets.<br>• More machines need another converter on the same header. Each machine follows one converter at a time. |
| `feed_station` | 3 x 3 x 5 | A solid feed station [N]: a heated cabinet around a 48Y.<br>• A natural UF6 48Y (conversion.md) goes in by a hopper on top. It is heated to sublime its 12,501 kg without liquefying it.<br>• The UF6 goes onto the header at the south face of its back cells.<br>• The emptied 48Y goes out to a hopper under it, ready for the tails take-off or the conversion filler.<br>• Its heater draws **30 kW at 480 V** *(unverified)*.<br>• It runs the cascade: it counts the driven machines on its header and meters the feed. |
| `product_takeoff` | 3 x 3 x 4 | The product take-off [N]: chilled desublimers fill a 30B under vacuum.<br>• Empty 30Bs go in by a hopper on top. Full ones, **2,277 kg** of 4.95 % UF6, go out to a hopper under it or to an empty hand.<br>• Refrigeration draws **15 kW at 480 V** *(unverified)*.<br>• The header comes in on the north face of its front cells. |
| `tails_takeoff` | 3 x 3 x 5 | The tails take-off [N]: the same, into a 48Y, **12,501 kg** of 0.25 % UF6.<br>• Empty 48Ys go in by hopper; full ones come out below.<br>• Refrigeration draws **30 kW at 480 V** *(unverified)*.<br>• The header comes in on the north face of its front cells. |

## Products and items
- **UF6:** the header's product, a gas in kg. No stock tank holds it, because real UF6 is a solid in its cylinders at room temperature.
- **Empty 30B cylinder:** 30 in steel, 635 kg tare, crafted from steel.
- **30B of LEU:** 2,277 kg of UF6 at 4.95 % U-235, 1,539 kg of U (0.6761 kg of U a kg of UF6).
- **48Y of tails:** 12,501 kg of depleted UF6 at 0.25 %, 8,453 kg of U.

## Nozzles (each part drawn facing north; x east, z south; its main cell at the north-west bottom corner)
- **Centrifuge:** the header on any side of its top cell.
- **Feed station:** the header on the south face of its back cells (z 4, bottom).
- **Converter:** the header on the south face of its back cells (z 5, bottom).
- **Take-offs:** the header on the north face of their front cells (z 0, bottom).
- **12.47 kV box:** the west face of the converter's main cell.
- **480 V boxes:** the west face of the main cell of the feed station and of each take-off.
- **Hoppers:** cylinders go in from on top; full or emptied ones come out to a hopper under the part.

## How it runs
- **The header:** one joined run of steel pipe carrying UF6, from the feed station's nozzle. It joins the feed station, the two take-offs, the machines and the converters.
- **Before it feeds,** the feed station checks:
  - its 480 V;
  - a cylinder with UF6 left: otherwise **NO FEED**;
  - at least 11 driven machines: otherwise **TOO FEW STAGES** (n of 11);
  - a product take-off and a tails take-off on the header, each with a cylinder and room: otherwise **TOTAL REFLUX**. The machines spin, nothing is withdrawn and no feed goes in, as a real cascade idles.
- **What it makes:**
  - SWU = the sum of each machine's speed share × 11.4 SWU an hour;
  - product = SWU / 7.81;
  - feed = product × 10.20, limited by what its cylinder holds.
- **Where it goes:** the product UF6 goes to the product take-off and the tails UF6 to the tails take-off.
- **The converter:**
  - with none on the header, the machines read **NO DRIVE**, 0 Hz;
  - a converter short of power slows its machines, and their separative work follows.
- **Removing a part** stops what needs it. A broken machine drops out of the count.

## Left out (ROADMAP Ideas)
- Spin-up and coast-down, which take hours on real machines.
- Machine crashes.
- Cooling water to the casings.
- The product sampling autoclave and blending [N].
- The heels left in emptied cylinders.
- Choosing the product assay.

## Game test
`cascade`:
- **Power:** the peaker scene's chain: an aeroderivative, its 60 MVA GSU, and a 60 MVA substation transformer down to 12.47 kV for the converter. A diesel set for the 480 V stations, and one for the turbine's starter.
- **The cascade:** the feed station, the two take-offs and the converter on one header, with ten centrifuges at first.
- **Checks:**
  - ten machines read TOO FEW STAGES (10 of 11), and the 11th starts it;
  - the converter reads about 6.3 MW at 1,225 Hz;
  - both take-offs fill;
  - a 30B and a tails 48Y that a hook has filled almost full are finished and drop into their hoppers;
  - a feed 48Y that a hook has almost emptied comes out empty below;
  - breaking the product take-off gives TOTAL REFLUX, and breaking the converter gives NO DRIVE.
- **Screenshot:** the hall.

## Added while building (7e)
- The converter draws a fixed impedance, as the land rig's drives do. At 12,258 V on the peaker's substation transformer it gets
  6.07 of its 6.28 MW, so the machines run at 1,184 Hz and do 121 SWU/h rather than 126. The game test checks for more than
  1,150 Hz.
- The take-offs' refrigeration runs whenever a cylinder is on the scale, so a take-off is ready before the cascade feeds.
