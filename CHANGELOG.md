# Changelog

## Unreleased (0.7.0): gas and power quick wins (Phase 14b-14d) and the protection gap wave (built 2026-11-09 while Tim was away; not approved)
- **The sales line (14b):** a gas plant's pipeline gas goes on an off-map transmission line at a receipt meter station and comes off
  anywhere in the dimension at a delivery station, less 2 % kept as compressor fuel. Five true-size parts: meter building (2 kW,
  residential), filter separator, ultrasonic meter run, regulator skid, pipeline tap. Off-spec gas refused (rich, wet, biogas); 8 hours on
  the RTU battery, then METER FAILED; a run ticket in m3, GJ and MMBtu; PLC points. Spec: `docs/multiblocks/sales-gas.md`.
- **The acid plant's steam (14c):** an acid superheater against the converter sends the waste heat boiler's steam to the existing 5 MW
  turbine, about 1.6 MW; the feed pump returns the condensate; the plant can carry its own blower; LOW STEAM TEMPERATURE trip. Spec:
  `docs/multiblocks/acid-plant-steam.md`.
- **Biogas (14d):** a new pipe product, `biogas, 60 % CH4`, for another mod to offer on Wildcatter's steel pipe through `FluidNode`. Gas
  sets burn it derated to 850 kW; fireboxes burn it; big burners and the sales line refuse it. Spec: `docs/biogas.md`.
- **Solver:** a generator or genset closing onto a live bus takes the bus's angle to the nearest 30 degrees, so machines on either side of
  a delta-wye transformer run in parallel without circulating power.
- Decisions for review: `docs/questions/phase14-gas-power.md`.
- **The protection gap wave** (decisions for review: `docs/questions/protection-gaps.md`):
  - Feeder relays (switchgear, breaker relay cabinet, 15 kV feeder breakers) have settable pickup, curve (MI, VI, EI), time dial and
    50, set from the relay with the Power Meter.
  - The breaker relay cabinet recloses (79): up to three shots, then lockout.
  - Switchgear racking takes 15 s a position and can be done remotely from a PLC; breaker failure (50BF) on the switchgear and the
    13.8 kV feeders; a bus differential (87B) on the 4.16 kV lineup, faulted with the grounding set on a section's bus.
  - 87T on the GSUs and the 10 MVA power transformer, 87G on the big generators, each with a lockout reset by hand; the generator bus
    relay panel transfer-trips a GSU's line-side breaker.
  - Grounding clamps carry an I²t rating and burn off on a fault that lasts; the set also goes on a big generator's terminals.
  - A shock trips a GFCI or RCD: the body is a 1,000 ohm path to earth.
  - ATS: factory timers (30 min retransfer, 5 min cooldown) or the short set; open, in-phase or closed transition.
  - Class J and E-rated fuses limit fault current and read out their let-through; only the fastest device clears a fault; breakers
    and fuses read the device upstream and how far they coordinate.
  - The load centre's and panelboard's main handle shows ON, OFF or TRIPPED.
  - Overhead spans and leads heat on their conductor's rating and burn down; 4.16 kV conductor spools; leads from a line insulator to
    a cutout or pole transformer; a panel with a ground rod is the service, its neutral bonded and earthed; an under-rated insulator's
    arc shatters it; a pole transformer or cutout comes down with its pole.

## v0.6.0 (built 2026-11-09; not yet released on GitHub)
- **The ammonia plant (Phase 14):** a nitrogen generator, syngas compressor and ammonia converter on the steam reformer's hydrogen
  (15.2 kg/h of liquid ammonia), a nitric acid unit on a platinum-rhodium gauze (60 % acid), an ammonium nitrate neutraliser and an
  emulsion blender making bulk emulsion matrix (46 kg/h) for open-pit blasting. New pipe products: nitrogen, ammonia, nitric acid,
  AN solution, emulsion matrix. New items: gauze pack, emulsifier pail. The LPG bullet also stores ammonia.
  Spec: `docs/multiblocks/ammonia-plant.md`; decisions for review: `docs/questions/ammonia-plant.md`.

Earlier releases (v0.2.0 to v0.5.1) are described phase by phase in `PROGRESS.md`.
