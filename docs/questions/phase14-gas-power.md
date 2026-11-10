# Phase 14b-14e, gas and power quick wins: decisions taken while Tim was away (2026-11-09)

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back ...
lets get all ideas done or planned out". Specs: `docs/multiblocks/sales-gas.md` (14b), `docs/multiblocks/acid-plant-steam.md` (14c),
`docs/biogas.md` (14d), `docs/mekanism-check.md` (14e, still a plan). The spec-level choices are items 1-13 of
`docs/questions/specs-wildcatter.md`; these are the ones taken while building. Format as in `docs/questions/README.md`.

### 1. How are the waves numbered, with Phase 14 already the ammonia plant?
- **Chosen:** the ammonia plant is 14a; the sales line 14b, the acid plant's steam 14c, biogas 14d, the Mekanism check 14e. The specs
  and `docs/ideas-plan.md` called them 14a-14d; they are renumbered there too.
- **Why:** ROADMAP already had Phase 14 as the ammonia plant, and these share its theme (gas and power, built while Tim was away).
- **Alternatives:** a new Phase 15 (and every later proposed phase moves up one).

### 2. The meter station's layout of record
- **Chosen:** facing north, the building x 0-2 z 0-3; the meter run (1 x 2 x 8) along its east side, x 3 z 0-7; the filter separator
  (receipt) or the regulator skid (delivery) east of the run's north end, x 4-5 from z 0; the tap east of the run's south end, x 4 z 7. The
  filter separator's inlet is the **north** face of its bottom front cells (the spec said west, which is where the meter run touches it).
- **Why:** the spec's sketch did not fit its own part sizes (it drew the 8-long run 5 long and two wide); the game test is the layout of record.
- **Alternatives:** the spec's sketch with the run shortened.

### 3. The meter building's 2 kW at residential voltage
- **Chosen:** a single-phase load on the world's low tier (120 V NA, 230 V IEC), modelled with the mod's motor (PF 0.8), hot and neutral on
  the west face of its main cell. Fed in the scene from a pole transformer.
- **Why:** it is mostly heaters and analysers; the motor model is what every plant part uses and it reads the supply's voltage.
- **Alternatives:** a resistive heater load (PF 1.0, closer to the truth, a new load type).

### 4. How much gas the regulator skid holds
- **Chosen:** one interval at the station's rating (1 m3, 0.2 s at 5 m3/s).
- **Why:** it is the gas in the skid's own piping; any more and a delivery station with an empty account would run a set for minutes.
- **Alternatives:** a few seconds of rating (line pack).

### 5. "Today" on the run ticket
- **Chosen:** a game day of running (24,000 ticks of world time), not the clock.
- **Why:** the world's day time is not reachable from a block entity on this version without a loader call; game time is.
- **Alternatives:** reset at dawn.

### 6. Off-spec gas
- **Chosen:** separator, compressed and dried gas OFF SPEC: RICH; wet gas OFF SPEC: WATER; biogas OFF SPEC: CO2 (docs/biogas.md); anything
  else NOT GAS. A refused receipt books nothing and the tap's valve reads CLOSED. The chromatograph reads 38 MJ/m3 for pipeline gas and
  23 MJ/m3 for biogas.
- **Why:** specs-wildcatter items 3 and 9.
- **Alternatives:** accept dry gas at its heating value.

### 7. The scenes' gas source
- **Chosen:** a stand-in block in the NeoForge test mod (and in the Fabric test mod) whose block entity implements `FluidNode` and offers a
  set product at a set rate: pipeline gas for the receipt station (1.0 m3/s, a gas plant's), separator gas, wet gas and biogas for the
  refusals, biogas for the gas set.
- **Why:** the gas plant train is proven in `gasPlant`; the stand-in is also exactly what another mod does to supply biogas, so it
  proves the API from outside Wildcatter.
- **Alternatives:** the full well, separator, compressor, dehydrator and chiller in each scene.

### 8. The delivery station's power in the scenes
- **Chosen:** it runs on its RTU battery (8 game hours) for the scene; the receipt station is on a real residential service.
- **Why:** the battery lasts the scene; the receipt station proves the service and METER FAILED.
- **Alternatives:** feed it from the gas set it supplies, through a pad-mount and a pole transformer (a real city gate's own supply).

### 9. The superheater's steam to the turbine
- **Chosen:** by touching, as the HRSG's is; saturated steam in on a steam pipe at the west face of its bottom cells, from the waste heat
  boiler's top face (a new STEAM nozzle).
- **Why:** the 5 MW turbine already takes the HRSG's steam by touching; the spec put the turbine against the superheater.
- **Alternatives:** a superheated steam pipe from the superheater's top to the turbine.

### 10. LOW STEAM TEMPERATURE
- **Chosen:** if the superheater passes saturated steam (the converter has not converted for 2 s), the turbine trips its generator and
  latches; it resets itself once the turbine has coasted to a stop and the steam is hot again (or gone).
- **Why:** wet steam erodes the last stages; a real trip needs a reset, and the turbine has no button of its own.
- **Alternatives:** a reset on the generator by hand.

### 11. The condenser at a third of its steam
- **Chosen:** all four fans run (149 kW), as in combined cycle; the spec's "two fans" is not built.
- **Why:** the fans are one motor in the model.
- **Alternatives:** split the fan motor into two.

### 12. A machine closing onto a bus behind a delta-wye transformer (solver)
- **Chosen:** when a generator or genset closes onto a live bus, it takes the bus's phase angle **to the nearest 30 degrees** as its own
  frame (`Synchronizer.angle`, saved). A machine with no delta-wye transformer between it and the others stays at 0 and shares load
  exactly as before.
- **Why:** every source was built at 0 degrees, so the acid plant's turbine (13.8 kV, through the delta-wye unit auxiliary transformer)
  paralleled with the diesel sets (480 V, through a wye-wye unit substation) drove about 2 MW of circulating power. A real machine pulls
  into step with the bus it closes onto. Rounding to 30 degrees keeps the small load angle out, so existing load sharing is untouched.
  **This is a change to how generators are built in the circuit, not to the settled table in ELECTRICAL_MODEL.md; Tim should look at it.**
- **Alternatives:** follow the measured angle exactly (changes load sharing everywhere); forbid paralleling across a delta-wye.

### 13. How the gas set holds 850 kW on biogas
- **Chosen:** its rating on the island becomes 850 kW, and its governor's hold lowers its voltage until the load takes no more than
  850 kW (a load's power follows the square of its voltage in this model). The readout says BIOGAS: 850 kW MAX. Against a 1 MW load bank
  it holds about 846 kW at about 447 V.
- **Why:** in the model a set's share of load follows its voltage; a real engine at its limit droops and the load takes what it gets.
- **Alternatives:** let frequency collapse and the set trip on underfrequency (a real overloaded island), or ignore the derate.

### 14. Biogas fuel on the set
- **Chosen:** its own 10 s buffer in m3 of biogas, drawn only when the engine's runs carry no natural gas; burned at 407 m3/h at
  850 kW and 61 m3/h idle (Biogas.fuelM3h). Natural gas left in the line is dropped when it switches.
- **Why:** a run carries one product, so a set is on one fuel or the other.
- **Alternatives:** mix the two by heating value.

### 15. The interface for another mod, and the version
- **Chosen:** the contract is `dev.wildcatter.block.FluidNode` offering `Product.BIOGAS` (unchanged from the spec). It ships in the next
  minor release (0.7.0), not 0.6.0 as the spec proposed: 0.6.0 went out with the ammonia plant. Farmings declares Wildcatter 0.7.0 or later.
- **Why:** 0.6.0 is released and cannot change.
- **Alternatives:** a 0.6.1 patch release.

### 16. Biogas in a firebox
- **Chosen:** by its heating value: 23/38 of a natural gas m3's fire.
- **Why:** the firebox takes any gas (spec).

### 17. Recipes for the new parts
- **Chosen:** steel and gas line with real stand-ins: the meter building from glass, a comparator and a repeater; the filter separator from
  wool and a bucket; the meter run from a comparator; the regulator skid from levers and a furnace; the tap from a lever; the superheater
  from bricks and a copper block.
- **Why:** as the other plant parts' recipes; no new intermediate items.

### 18. Textures
- **Chosen:** all six parts are drawn in the mod's existing materials (tools/models/gas_power.py); no new script-drawn textures.

### 19. 14e, the Mekanism check
- **Chosen:** still a plan: Mekanism for 26.3 is not released (specs-wildcatter item 12). Re-check each phase.

### 20. A firebox on piped gas (found by the lead filming `biogas`, 2026-11-09) — question for Tim
- **Found:** a firebox counts 8 ticks of fire per unit of gas (`Tuning.GAS_FUEL_TICKS_PER_UNIT`), a unit from the early gas wells.
  Pipeline gas and biogas are now in m3 and use the same figure: a m3 of natural gas (38 MJ) burns 0.4 s, a fire of about 95 MW, while
  the same firebox on coal is a few hundred kW. On a digester's 1 m3/s of biogas each draw gives it under one tick of fire, so it
  draws the gas but never shows lit.
- **Chosen for now:** nothing changed in play. The `biogas` client scene checks that the firebox draws the gas (as the server
  scene does) and logs that it is not lit.
- **Recommended:** give the firebox one real heat rate (its coal fire's, about 0.36 MW) and burn every fuel by heating value: natural
  gas about 2,100 ticks a m3, biogas about 1,270, crude by its MJ. Well gas units would need converting to m3 at the same time, so
  gas wells under a firebox would run about 260 times longer per unit.
- **Alternatives:** leave it (gas fireboxes stay an early-game shortcut); or fix only m3 products (pipeline gas, biogas) and leave
  well units alone.
- **Tim (2026-11-10):** "fix it with one real heat rate, in every world." Built on `w-grid`: 0.36 MW, every fuel by its heating value
  (`fluid/FireboxFuel`); the `biogas` scenes now check the firebox stays lit. See `docs/questions/answers-grid.md` 1.
