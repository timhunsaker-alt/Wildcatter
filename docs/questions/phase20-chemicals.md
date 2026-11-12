# Phase 20, chemicals: decisions taken while Tim was away (2026-11-12)

Plan: `docs/ideas-plan.md`, Phase 20. Built: **20a** the chlor-alkali plant. Specs: `docs/multiblocks/chlor-alkali-plant.md` (and, as
they are built, `titanium-pigment.md`, `lithium.md`); each has an "As built" section. Format as in `docs/questions/README.md`. Every
entry was decided on the best real-world recommendation; none needs Tim before it can stand, but each can be changed.

The rule for this phase: nothing changes what an existing world already does. Every part is new; the one existing part changed (the
injection pump) behaves as before unless its discharge reaches the new brine wellhead.

## 20a: the chlor-alkali plant

### 1. The layout of record
- **Chosen:** the cell room stands facing north like every other part (6 wide, 16 long), against the brine treatment's east side; the
  rectifier_mv stands against the room's north end, its 12.47 kV box on the west face of its main cell; the saturator against the
  treatment's west side. The `chlor_alkali` scene is the layout of record.
- **Why:** the spec's sketch draws the room running east with the rectifier at its east end, but a north-facing part's 16 is its length,
  and at the east end the rectifier's own box would face the room. The spec says the scene is the layout of record.
- **Alternatives:** place the room facing east (then the treatment touches its short end only).

### 2. rectifier_mv is the 4.16 kV rectifier's second size
- **Chosen:** the same 12-pulse rectifier model (setpoint from the touching cell, dial, PLC points, DC overcurrent trips, ground-fault
  detector), on the distribution tier, up to 500 V and 16 kA, PF 0.95, 97 %. Its internal resistance 0.3 milliohm (about 1 % of 500 V at
  16 kA, as the 4.16 kV unit's 1 % of 150 V) and its tap changer 50 V/s (the 4.16 kV unit's 15 V/s scaled to its range): both *proposed*.
  It is a conversion part (it serves 16b2's cell houses too), so the sneak-click steps its setpoint as on the small one.
- **Why:** a real thyristor rectifier transformer differs from a diode one in its regulation, not in what the grid sees; one model keeps
  the DC solver as it is.
- **Alternatives:** model thyristor phase control (a lagging PF that falls with the firing angle) for both sizes.

### 3. Where the water and brine go
- **Chosen:** the brine treatment draws saturated brine from the saturator against it and hands it, treated, to the cell room against
  it. The cell room's water (3 L a kg of chlorine, the spec's figure) comes with that brine, so the saturator is the plant's one water
  user; brine from a well brings its own water (74 % of it). The depleted brine's return is in the balance, not piped.
- **Why:** the spec ties the saturator and the room to the treatment by touching and names the saturator's make-up water; one water
  point is how a real brine circuit is fed.
- **Alternatives:** a separate demineralized water feed to the cell room's catholyte (real plants have one; the spec counts only one).

### 4. The CO2 for the brine's calcium
- **Chosen:** 3.2 g of CO2 a kg of salt, about **5.2 kg/h** at the rating (*proposed*): rock salt with 0.5 % calcium sulfate (*unverified*;
  halite's usual impurity is anhydrite), its calcium precipitated as carbonate with the usual 100 % excess of carbonate.
- **Why:** the spec names CO2 for the calcium but gives no rate.
- **Alternatives:** none needed if Tim prefers calcium left out entirely (then no CO2 pipe).

### 5. The voltage creep and replacing membranes
- **Chosen:** without CO2 the elements' voltage creeps 0.1 V a day of running (the spec's proposal), up to 0.5 V an element (*proposed*:
  where real membranes are blinded and replaced); the extra voltage raises the room's DC volts and so its megawatts at the same chlorine.
  A sneak-click on the room replaces the membranes and the creep goes back to zero, at no cost.
- **Why:** the spec says "until the membranes are replaced" without saying how. Membranes as an item would be a new part to make.
- **Alternatives:** a membrane kit item (from what?), or a stop of some minutes for the change.

### 6. The treatment's own caustic and HCl
- **Chosen:** the caustic for the magnesium and the HCl and caustic that regenerate the chelating columns are not drawn: they are a
  small share of the plant's own make and the spec gives no rate.
- **Why:** no invented numbers. Real regeneration uses a few kg of each a tonne of chlorine.
- **Alternatives:** draw them from the room's caustic and the HCl unit at a rate Tim gives.

### 7. Hydrogen with nowhere to go
- **Chosen:** vented, as at the small cell; the room does not stop for it.
- **Why:** the spec says "as the small cell", and the small cell vents its hydrogen.
- **Alternatives:** FULL when the hydrogen pipe is not drawn (the spec's list names hydrogen with chlorine and caustic).

### 8. WET CHLORINE
- **Chosen:** the room's chlorine is a new pipe product, **wet chlorine**, which only the chlorine train takes. The room reads WET CHLORINE
  and draws no current while no chlorine train is on its chlorine line.
- **Why:** the spec's behaviour; checking the line for a train gives the readout the spec names rather than a plain FULL.
- **Alternatives:** none.

### 9. The chlorine train's split between pipe and liquefier
- **Chosen:** dry chlorine goes on its pipe; the liquefier condenses what the pipe's users leave above two intervals' make, up to 1.1 t/h
  (*proposed*, the room's rating with a margin), into a chlorine filler it touches. Two supplies, as the spec lists: the compressor at
  medium voltage (west box), the liquefier at 480 V (north box). Drying acid 15 kg a tonne (the spec), counted as spent 75 % acid.
- **Why:** gas users (HCl, 20b's chlorinator) draw from the compressor discharge; storage takes the rest, as at a real plant.
- **Alternatives:** liquefy everything and vaporize for users.

### 10. Parts with no motor
- **Chosen:** the caustic evaporator and the HCl synthesis unit have no electrical load: the spec gives them none (steam and a burner).
- **Why:** no invented ratings.
- **Alternatives:** a few kW of pumps each, if Tim wants them on the grid.

### 11. The evaporator off
- **Chosen:** without steam it passes 32 % caustic through its outlet (after its 50 % hold is drawn, so the two never share the pipe).
- **Why:** the spec's "32 % passes through when it is off"; a pipe carries one product at a time.
- **Alternatives:** a bypass nozzle of its own.

### 12. The HCl unit's inputs
- **Chosen:** hydrogen from the cell room on its north side, dry chlorine on its east, water from a pump; 32 % acid out of the south, the
  unburned hydrogen out of the west face of its back cell. 0.972 kg of chlorine, 0.31 Nm3 of hydrogen and 2.125 L of water a kg of HCl.
- **Why:** stoichiometry; the spec's "the rest of the hydrogen goes on its pipe".
- **Alternatives:** none.

### 13. The brine well
- **Chosen:** the brine wellhead goes on a cased hole's casing. An injection pump whose discharge reaches it takes **fresh water from a
  water pump** on its suction (an injection pump whose discharge reaches no brine wellhead takes none, as before) and pushes it down at up
  to 3 L/s (*proposed*: the plant needs about 1.3). The water comes up saturated (26 %); each 2,117 kg of salt turns the next rock salt block
  into water, found from the hole's bottom upward and outward within 12 blocks (*proposed*). Brine comes out of the side the wellhead faces.
- **Why:** the spec's injection pump; a water pump is how the mod lifts fresh water. Without the wellhead the pump is unchanged.
- **Alternatives:** the wellhead taking water straight from a water pump (no injection pump).

### 14. Recipes
- **Chosen:** steel, gas line, pistons, copper blocks, a cauldron for the treatment, a blast furnace for the HCl burner (*proposed*, in
  the pattern of the other plant parts).
- **Why:** none are in the spec.
- **Alternatives:** any.
