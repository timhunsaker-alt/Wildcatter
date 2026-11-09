# Changelog

## Unreleased (0.7.0): gas and power quick wins (Phase 14b-14d, built 2026-11-09 while Tim was away; not approved)
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

## v0.6.0 (built 2026-11-09; not yet released on GitHub)
- **The ammonia plant (Phase 14):** a nitrogen generator, syngas compressor and ammonia converter on the steam reformer's hydrogen
  (15.2 kg/h of liquid ammonia), a nitric acid unit on a platinum-rhodium gauze (60 % acid), an ammonium nitrate neutraliser and an
  emulsion blender making bulk emulsion matrix (46 kg/h) for open-pit blasting. New pipe products: nitrogen, ammonia, nitric acid,
  AN solution, emulsion matrix. New items: gauze pack, emulsifier pail. The LPG bullet also stores ammonia.
  Spec: `docs/multiblocks/ammonia-plant.md`; decisions for review: `docs/questions/ammonia-plant.md`.

Earlier releases (v0.2.0 to v0.5.1) are described phase by phase in `PROGRESS.md`.
