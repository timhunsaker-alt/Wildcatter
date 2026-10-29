# Wildcatter

A Fabric mod for Minecraft Java 26.3 about building a real electric grid, meant to double as a teaching tool for how circuits and breakers actually work.

## What's in it

- **Steam power:** campfire boiler and steam turbine (110 V).
- **Real closed loops:** red hot and black neutral wires; every wire is a resistor and each circuit is solved with Ohm's law. A load with only one wire stays dark.
- **Amps and breakers:** thermal and magnetic breakers (15/20/30/50 A). Overloads and shorts trip them; with no breaker, wire burns out. A breaker only protects what comes after it.
- **Wire ratings:** 110 V, 4.16 kV and 138 kV wire, each with resistance and ampacity.
- **Insulation:** bare wire shocks. Wool-insulated wire wears with time, load and rain, and shorts or shocks when worn out (plastics later).
- **Voltage levels and transformers:** step 110 V up to 4.16 kV and back down.
- **Shocks and grounding:** ungrounded live machines can bite; ground rods and ground wire fix it.
- **Big machines:** 27 boilers on 9 furnaces form a 54 kW coil boiler; a 3x3x3 cube of 27 steam turbines forms a 4.16 kV medium voltage generator. Both assemble themselves.
- **Art and sound:** textures and sound effects made with Ludo (`art/`, `tools/process_art.py`, `tools/process_sounds.py`).
- **Power Meter** and an in-game **Field Manual** that grows with the mod. JEI shows the recipes.

## Planned

Oil and gas wells, drilling rigs, refinery, plastics, gas and diesel turbines, medium/high-voltage breakers, and a Mekanism (RF) bridge with an easy NeoForge port.

## Build

JDK 25 required.

```bash
./gradlew build              # jar in build/libs/
./gradlew runClientGameTest  # automated in-game tests
```

To redo the art: `python3 tools/process_art.py && python3 tools/make_assets.py` (needs Pillow).

Tuning numbers are all in `Tuning.java`. The solver has a standalone check in `tools/solvercheck/`.
