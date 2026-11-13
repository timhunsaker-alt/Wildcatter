# Phase 20, chemicals: decisions taken while Tim was away (2026-11-12)

**Tim accepted every decision below as decided on 2026-11-13** (the 0.7.0 review).

Plan: `docs/ideas-plan.md`, Phase 20. Built: **20a** the chlor-alkali plant, **20b** titanium pigment, **20c** lithium hydroxide. Specs: `docs/multiblocks/chlor-alkali-plant.md` (and, as
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

## 20b: titanium pigment

### 15. The mineral separator in an existing world
- **Chosen:** the separator keeps counting its ilmenite and rutile exactly as before. Only while a **hopper stands under it** does it also
  put them out, as 500 kg items (*proposed*, the spec's "500 kg each"), into two out bins the hopper empties. What it counted before is not
  turned into items. The hopper is the part the player adds; a separator never had anything to give downward, so no built plant has a
  reason to have one there.
- **Why:** Tim's rule: nothing changes what an existing plant does until the player adds the new part.
- **Alternatives:** a world rule (`separatorTitanium`), off in old worlds; or put the items out always.

### 16. The furnace's bins
- **Chosen:** the submerged arc furnace keeps its 17a slots (six charges, then the paste in slot 6); ilmenite gets slot 7 and the pig iron
  an out slot 8, which a hopper under the furnace empties. The sneak-click recipe cycle is FeCr, FeMn, FeSi, Ti slag.
- **Why:** a slot moved would load an existing furnace's paste into the ilmenite bin.
- **Alternatives:** none.

### 17. The Ti slag numbers
- **Chosen:** ilmenite 50 % TiO2 (the spec, *unverified*) to 85 % slag: 1.7 t of ilmenite a tonne of slag; its iron 40 % of the ilmenite
  (derived from the spec's 0.12 t/h of pig iron with 0.18 t/h of slag), 98 % of it to pig iron (*proposed*), cast as iron ingots of 100 kg
  (the mod's ingot); coke **0.12 kg a kg of ilmenite** (*unverified*; real smelters use anthracite at about that). 2.2 MWh a tonne of slag (the
  spec). The slag is the product, in 1 t bags; no waste slag is counted.
- **Why:** the spec gives no coke rate; 0.12 covers the iron oxide's reduction with the usual excess.
- **Alternatives:** a coke rate from Richards Bay or Sorel data when Tim has it.

### 18. The chlorinator's feed
- **Chosen:** one bin takes Ti slag bags or rutile (and a second, coke); whichever is there is chlorinated, its TiO2 counted (85 % or 95 %).
  The spec's 60/40 blend is how a player would feed it, not a rule. All the feed's TiO2 goes to TiCl4.
- **Why:** a fixed blend would stop the plant when one feed runs out.
- **Alternatives:** two bins with a fixed blend.

### 19. The chlorine loop
- **Chosen:** the oxidiser's chlorine and the make-up (from a chlor-alkali plant) share the chlorinator's one chlorine line; the
  chlorinator takes the oxidiser's first when the oxidiser is on that line, then the make-up: 1.78 kg back and 0.4 kg of make-up a kg of
  TiO2. In the scene, 641 kg/h in and 523 kg/h back.
- **Why:** the spec's "takes it first and the make-up after"; a pipe carries one product, and both are chlorine.
- **Alternatives:** two inlets.

### 20. What is counted
- **Chosen:** the waste chlorides as FeCl2 from the make-up chlorine (0.71 kg a kg of TiO2, *proposed*), and SiCl4 at 0.5 % of the crude
  TiCl4 (*proposed*). VOCl3 is in the readout, not counted.
- **Why:** the spec says counted but gives no amounts.
- **Alternatives:** a feed assay that carries the impurities.

### 21. TiCl4 between the parts
- **Chosen:** handed by touching (chlorinator, purification, oxidiser, finishing), not a pipe product.
- **Why:** the spec's layout has them touching and the controller finding them that way.
- **Alternatives:** a TiCl4 pipe product and a stock tank.

### 22. The oxidiser
- **Chosen:** no motor (the spec gives none); its preheaters burn 0.2 MW of pipeline gas or LPG on the east face of its front; oxygen on the
  north face of its front; chlorine back out of the west face of its back cell. NO OXYGEN, NO GAS, FULL.
- **Why:** the spec's numbers.
- **Alternatives:** none.

### 23. Oxygen
- **Chosen:** a new pipe product, **oxygen (93 %)**, in Nm3. The VPSA unit makes 100 Nm3/h while it is drawn. The alloy melt shop's AOD
  is not in the mod, so nothing else takes it yet.
- **Why:** the spec pipes it.
- **Alternatives:** none.

### 24. The finishing
- **Chosen:** alumina hydrate from the Bayer refinery's 1 t hydrate bags in its hopper (30 kg a tonne), process steam on the east face of
  its front (2 t a tonne), 75 kW; no water (the spec gives none). Pigment to a bagging station against it.
- **Why:** the spec's numbers.
- **Alternatives:** a water feed for the wet milling.

### 25. Pigment's use
- **Chosen:** a bag crafts into 64 white dye; tagged `c:pigments/titanium_dioxide` for other mods.
- **Why:** the spec (and decision 66 in `specs-wildcatter.md`).
- **Alternatives:** a tag other mods already read (`c:dyes/white` would make a 1 t bag a dye in every recipe).

### 26. Recipes
- **Chosen:** steel, gas line, bricks, a blast furnace for the chlorinator, a furnace for the oxidiser, pistons (*proposed*).
- **Why:** none are in the spec.
- **Alternatives:** any.

## 20c: lithium hydroxide

### 27. A block of spodumene ore
- **Chosen:** the mod's block of spodumene pegmatite, 2,700 kg (density 2.7, `docs/ores.md`), at the spec's 6 % Li2O: 162 kg of Li2O, 409 kg
  of LiOH.H2O at 90 % recovery; 1.22 blocks an hour at the rating (3.3 t/h of ore).
- **Why:** the spec's 2,900 kg is marked unverified; the ore table is the mod's record.
- **Alternatives:** a density of 2.9 for spodumene (174 kg of Li2O, the spec's figure).

### 28. Where things go on the parts
- **Chosen:** set out as the spec's sketch, the roast kiln covers the leach's west face and the decrepitation kiln the roast kiln's, so: the
  leach's residue and CO2 on the south face of its back cells; the roast kiln's acid on the east face of its back cell and its gas on its
  south face; the decrepitation kiln's gas on the east face of its back end; the leach's, roast kiln's and crystallizer's boxes on their
  north faces. The roast kiln and the decrepitation kiln can share one gas line.
- **Why:** the spec's "west face" for the residue is covered by the roast kiln.
- **Alternatives:** a gap between the parts (then they would not touch).

### 29. CO2 for the calcium
- **Chosen:** 2 kg a tonne of ore (*proposed*: about 3 m3 of leach liquor a tonne at 0.6 g/L of calcium, as carbonate).
- **Why:** the spec names the step but gives no rate.
- **Alternatives:** none needed if the lime is dosed to leave no calcium.

### 30. Two bagging stations on one crystallizer
- **Chosen:** the station against the crystallizer's north face bags sodium sulfate; one against any other face, lithium hydroxide. A part
  that bags one product (every earlier one) is unchanged: the bagging station asks for the bag "for this station", which by default is the
  one bag.
- **Why:** the spec's "each with its own bagging station".
- **Alternatives:** a sneak-click switch on the station.

### 31. The carbonator's feed
- **Chosen:** lithium hydroxide **bags** by a hopper on top, dissolved and carbonated with liquid CO2 (east face of its main cell), up to
  100 kg of Li2CO3 an hour (*proposed*); 1 t bags at a station against it. It need not touch the crystallizer.
- **Why:** the crystallizer's faces are all taken (box, caustic, steam, two stations); a pipe product for LiOH solution would be one more
  product for one small part. The chemistry is the same: 1.14 kg of LiOH.H2O and 0.60 kg of CO2 a kg of Li2CO3.
- **Alternatives:** a LiOH solution pipe product from the crystallizer.

### 32. Lithium in the pot (the potline)
- **Chosen:** a lithium carbonate bag in a **pot's own hopper** goes into its bath (1 t); while the bath holds any the pot's back-EMF is
  0.1 V lower, so it runs 0.1 V lower at the same current (the scene: 1.728 V to 1.628 V at 6 kA), using 1.5 kg a tonne of aluminium. Only a
  pot given a bag changes; its 17a slots keep their numbers.
- **Why:** the spec's game test puts the bag in a pot's hopper; its Uses section says the fume treatment centre's hopper. A pot does not
  know which centre its gas reaches, and a bag at the pot is how the test reads.
- **Alternatives:** the bag in the fume treatment centre, shared by every pot whose duct reaches it.

### 33. No water at the leach
- **Chosen:** the leach takes no water: the spec gives none (the acid and caustic carry the liquor).
- **Why:** no invented numbers.
- **Alternatives:** a water feed and a NO WATER readout.

### 34. Sodium sulfate and tags
- **Chosen:** sodium sulfate bags have no use (decision 71 in `specs-wildcatter.md`); lithium hydroxide and carbonate bags are tagged
  `c:dusts/lithium_hydroxide` and `c:dusts/lithium_carbonate` for other mods. Recipes for the five parts are *proposed*.
- **Why:** the spec ("tagged for other mods").
- **Alternatives:** a tailings cell for the sulfate (19d plans that).

