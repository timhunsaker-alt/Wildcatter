# Changelog

## v0.6.2 (unreleased)

### Fixes
- **No more see-through holes around small blocks:** thirteen blocks whose models are smaller than a whole block still told the game
  they filled it, so it skipped drawing the faces of the blocks beside them and the world showed through: the steam turbine, firebox,
  pumps, day tank, breakers and switches, fuses and cutouts, pole transformers, lamps and heaters, the three-phase heater, motor
  starters, the air compressor and dryer, and crane rail. They no longer hide their neighbours. Only how they draw changes; nothing in a
  world does. The NeoForge `registers` scene now checks every block's models against how it occludes.

## v0.6.1 (released 2026-11-12)

### Fixes
- **The NeoForge client starts again:** NeoForge 26.3.0.101 wants the client-side handler of a clientbound payload registered on the client
  (`RegisterClientPayloadHandlersEvent`). The PLC payload had only its two-way handler, so a NeoForge client refused to start with
  "Some clientbound payloads are missing client-side handlers: [wildcatter:plc]". Found by Farmings' first NeoForge client launch. The
  Fabric client and dedicated servers were not affected.

## v0.6.0 (released 2026-11-10)

### Fixes
- **The circuit manager no longer loads chunks:** working out which circuit parts touch read blocks next to a part even in unloaded
  chunks, loading them on the server thread every few ticks; a circuit at the edge of the loaded area kept its neighbours loaded and
  could slow the server or run it out of memory. It now stops at the loaded area's edge; solved numbers are unchanged.

### Gas and power quick wins (Phase 14b-14d), the PLC gap wave and the protection gap wave
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
- **The PLC gap wave** (decisions for review: `docs/questions/plc-gaps.md`):
  - **Limits:**
    - A control cable is 50-pair: 98 signal cores, a discrete signal a core, a 4-20 mA loop or pulse a pair. A landing on a full cable is
      refused; lay a second cable beside it or use a remote drop.
    - The 24 V supply's 10 A is enforced. Over it, it hiccups (off for 1 s, then on), and its PLCs restart warm. Horns, lamps and
      solenoids draw from it.
    - A remote rack has one owner. A device output wired to two PLCs ORs their contacts; a 4-20 mA loop driven by two fails.
  - **Drawn:** the modules seated are drawn in the cabinet's and the drop's door, with the CPU's RUN lamp and the memory card.
  - **Ladder:** rung labels, jumps and RETURN. A ladder or FBD can be a FUNCTION. FBD wires go around blocks.
  - **Switchgear:** TRIP 50, TRIP 51, TRIP 87B and TRIP 50BF inputs.
  - **Fixed:**
    - a lamp switched off by its relay stayed lit;
    - a firebox held back by its firing rate read BURNING;
    - the rig in AUTO asked for a console click.
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
- **The plant and crane gap wave** (decisions for review: `docs/questions/plant-crane-gaps.md`):
  - **Cranes:** wind stops them (rain stops a crawler on a long boom, a storm stops all three); a crawler needs gravel, rock or concrete
    under its tracks and the ring crane rock, concrete or rail (SOFT GROUND); a lift whose jib or boom would strike a block or another
    tower crane's mast waits (BLOCKED). New block **Crane Rail**: the ring crane travels along a row of it to bring a site within reach.
  - **Plants:** the cracker's quench tower holds 2,000 L of pyrolysis gasoline and of fuel oil, then stops until they are piped away; the
    gas plant's stabilizer fires its reboiler on piped gas or its own LPG; gas engine sets and gas turbines burn coke oven gas and coker
    gas; each pot's auxiliaries take 3 kW at the industrial voltage on its front, and a pot can be cut out of a live line (sneak-right-click);
    a hopper on any cell of the steam plant's burner feeds it.
- **The misc gaps wave** (decisions for review: `docs/questions/misc-gaps.md`):
  - A formed steam plant draws its feedwater line from the feed pump to the boiler, and its main steam line runs flange to flange from
    boiler to superheater to turbine; a formed land rig draws its cable trays from the drive house to the mud pumps and the drawworks.
  - The 1 MW gas set's alternator is drawn at its real 1.6 m (the 500 kW set's stays 1 m); built sets redraw on their own.
  - A gas set on coke oven gas is held to 864 kW; the aeroderivative refuses coke oven gas (its Wobbe index is 40 % off pipeline
    gas's) and still takes coker gas.
  - The brass texture no longer shows a tile line; the conductor spool icons are new art.

### The ammonia plant and the waves before it
- **The ammonia plant (Phase 14):** a nitrogen generator, syngas compressor and ammonia converter on the steam reformer's hydrogen
  (15.2 kg/h of liquid ammonia), a nitric acid unit on a platinum-rhodium gauze (60 % acid), an ammonium nitrate neutraliser and an
  emulsion blender making bulk emulsion matrix (46 kg/h) for open-pit blasting. New pipe products: nitrogen, ammonia, nitric acid,
  AN solution, emulsion matrix. New items: gauze pack, emulsifier pail. The LPG bullet also stores ammonia.
  Spec: `docs/multiblocks/ammonia-plant.md`; decisions for review: `docs/questions/ammonia-plant.md`.

Earlier releases (v0.2.0 to v0.5.1) are described phase by phase in `PROGRESS.md`.
