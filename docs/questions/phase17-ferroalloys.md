# Phase 17a, ferroalloys: decisions taken while Tim was away (2026-11-10)

Spec: `docs/multiblocks/ferroalloys.md` (DRAFT, decided on recommendation; its "As built" section lists every number added). The spec's
own choices are in `docs/questions/specs-wildcatter.md`, items 41-46. Format as in `docs/questions/README.md`. Entries marked
**(needs you)** change something outside this phase or pick between real options.

### 1. Ferrosilicon is a Wildcatter item; Tailings could switch to it (needs you)
- **Chosen:** `wildcatter:ferrosilicon_bag`, a 1 t bulk bag of ferrosilicon 75 from the submerged arc furnace (quartz, coke and iron,
  8.5 MWh a tonne), tagged `c:ferroalloys/ferrosilicon` and `wildcatter:ferroalloys`. Tailings is not changed: its dense-medium plant
  keeps its placeholder `tailings:ferrosilicon` recipe (an iron ingot, sand and coke make four 25 kg bags of 15 % Si FeSi).
- **Why:** the spec makes FeSi 75 in the SAF, and Tailings' own question (its `l7-dms.md`, 8) says the recipe should become FeSi 75
  diluted with steel when the ferroalloy plant exists. Changing Tailings belongs to Tailings' branch.
- **Alternatives for Tailings:** (a) a shapeless recipe, one `wildcatter:ferrosilicon_bag` and iron ingots to forty `tailings:ferrosilicon`
  bags (real milled 15 % FeSi is FeSi 75 or 45 atomised with steel scrap: 200 kg of FeSi 75 and 800 kg of iron make a tonne of FeSi 15);
  (b) keep the placeholder when Wildcatter is absent and offer (a) when it is present (a conditional recipe).

### 2. No solver change, so no setting for existing worlds
- **Chosen:** the furnace and the bank are ordinary circuit parts in the existing phasor solver (an impedance star each: R + jX for the
  furnace, -jX for the bank). No `config/wildcatter.properties` switch and no `Rules` class were needed.
- **Why:** the solver already solves complex power; the parts are new and only change a grid they are built on. The NeoForge server suite
  (every existing electrical scene) gives the same results as on master.
- **Alternatives:** a generator or bus Mvar readout (would change existing readouts; left out); an automatic voltage regulator or a
  substation tap changer (would move existing grid numbers; would need a default-off setting).

### 3. The furnace is a constant-power load
- **Chosen:** at a set tap the furnace takes its tap's power at PF 0.80 whatever the bus volts (its impedance sized from the volts it saw
  last, as the drives are), ramping 10 % a second as the electrodes go down; below 80 % volts its breaker opens.
- **Why:** real SAF electrode regulators hold a power (or impedance) set point. A constant-impedance furnace first tried read 7.50 MW on the
  scene's grid, whose 12.47 kV bus sags to 12.04 kV under 6 Mvar with no tap changer upstream.
- **Alternatives:** constant impedance (power falls as V², and a capacitor bank's lift would raise it).

### 4. The tap changer
- **Chosen:** 15 steps, secondary 105 V to 150 V; power goes as the square of the volts, so tap 1 is 49 % (3.9 MW) and tap 15 is 8 MW.
  Sneak-click on the transformer steps it; a PLC power set point picks the nearest tap.
- **Why:** the spec gives 15 steps and 150 V at full tap only; a 70 % voltage range is typical of small SAF transformers (unverified).
- **Alternatives:** a wider range; set points between taps (real regulators trim with the electrodes).

### 5. Where the feeder's power factor is read
- **Chosen:** the furnace transformer meters its furnace (MW, Mvar, PF, electrode kA) and, adding every capacitor bank touching it, the
  feeder's MW, Mvar and PF. A bank touching it is taken as on its primary bus, as in the spec's layout; it must still be wired to that bus.
  Transmitters read WATTS, VOLTS, AMPS and POWER_FACTOR (the feeder's) off the transformer.
- **Why:** the spec's test asks for "PF above 0.93" with the bank closed; at the furnace the PF stays 0.80 by physics. A real plant meters
  its incoming feeder. Power on one bus adds, so the sum is exact.
- **Alternatives:** a feeder meter part on the cable; Mvar on generator readouts (changes existing readouts).

### 6. The capacitor bank
- **Chosen:** 3 Mvar at its tier's nominal volts, an ungrounded wye, 0.2 W a kvar of losses, its reactance following the frequency; its
  vars go as V² (2.85 Mvar at the scene's 12.17 kV, giving the feeder PF 0.935). Sneak-click opens or closes its breaker; PLC points
  CLOSE, CLOSED, AUTO, MVAR 4-20 mA; box on the west face of its main cell.
- **Why:** spec values; ungrounded wye is the usual distribution bank. Over-compensation is real: at half power the feeder reads PF 1.00.
- **Left out:** an automatic var or PF controller, switching inrush, harmonic resonance, fuses on the cans.

### 7. Soderberg paste is the anode plant's green anodes (needs you)
- **Chosen:** the furnace's paste bin takes **green anodes** (1,275 kg each), broken into the electrode columns (2,000 kg) whenever one
  fits; 20 kg a tonne is burned; empty columns read ELECTRODES SHORT and the furnace runs at half power.
- **Why:** the spec wants paste as a second product of the anode plant's mixer. A green anode is that paste (calcined coke and pitch)
  pressed and unbaked; using it adds no item and changes nothing in the anode plant, whose paste plant feeds only its forming press.
- **Alternatives:** a new `soderberg_paste` item made by the paste plant into a hopper under it (changes the paste plant's behaviour for
  anyone with a hopper under one: would need a setting); a crafting recipe.

### 8. Charge weights
- **Chosen:** an ore's mineral item is one block of rock (chromitite 3,300 kg, manganese ore 3,200 kg, limestone 2,600 kg), a nether quartz
  100 kg, coke 112.5 kg, an iron ingot 100 kg. Per tonne: FeCr 0.62 chromitite block, 550 kg coke, 200 kg quartz; FeMn 1,600 kg manganese
  ore, 400 kg coke, 150 kg limestone; FeSi 1,800 kg quartz, 900 kg coke, 250 kg iron (ingots).
- **Why:** the spec's numbers; the mod already counts an iron ingot as 100 kg. Quartz's 100 kg is proposed.
- **Alternatives:** charcoal for FeSi (the spec allows it); iron ore instead of ingots.

### 9. Hearth, taps and recipe changes
- **Chosen:** the hearth holds 8 t; it taps every 2 hours or when full, or on the PLC's TAP; up to 10 t of tapped alloy waits for bags,
  past that HEARTH FULL stops it. A recipe change runs at tap power for an hour making nothing, then taps and switches. Slag is counted:
  1.2 t a tonne of FeCr, 0.6 of FeMn, 0.03 of FeSi (proposed).
- **Why:** the spec's 2-hour tap and 1-hour change; 8 t is a little under 4 hours of FeCr. A visible tap: lava, flame and smoke at the
  taphole for 20 seconds, with lava and fire sounds.
- **Alternatives:** a slag item for the cement or road-base that real plants sell.

### 10. Freezing
- **Chosen:** once it has run and holds metal, 4 hours without power (under 2 % of 8 MW) freezes the hearth: FROZEN, nothing works. Breaking
  and placing it again gives a fresh furnace (the hearth and bins lost), as the spec says.
- **Alternatives:** a frozen-furnace drop, as the frozen pot's spent pot lining.

### 11. The roaster's SO2 on the pipe
- **Chosen:** the roaster puts on the pipe the mass of burner gas (the sulfur burner's 11 % SO2 basis) that carries its SO2, so the
  existing converter makes the right acid with no change. 0.25 t/h of oxide gives 194 kg/h of SO2.
- **Why:** real roaster gas is about 6 % SO2; the converter counts burner gas at one strength. Changing the converter would change acid
  plants in existing worlds.
- **Alternatives:** a second product, roaster gas, that the converter learns to take (a converter change).

### 12. Scheelite reaches the vessel through the roaster
- **Chosen:** a concentrate carrying scheelite (`wildcatter:scheelite` in its assay) is filtered and dried in the roaster, no roasting
  and no SO2, into 1 t bags of scheelite concentrate at 65 % WO3 (807 kg of scheelite a bag).
- **Why:** the spec routes both concentrates through the roaster's drum filter but says only how moly oxide is made.
- **Alternatives:** a separate dryer part; the vessel taking slurry.

### 13. The roaster's power and rate
- **Chosen:** 22 kW in all at LV for the rabble drive and fans; 0.25 t/h of oxide; 98 % of the molybdenum to the oxide.
- **Why:** the spec's figures; "22 kW rabble drive and fans" is read as one load.

### 14. The vessel's batches (needs you)
- **Chosen:** a batch is one bag: moly oxide, 4 iron ingots and 20 aluminium ingots make 860 kg of ferromolybdenum (65 % Mo); scheelite
  concentrate, 1 iron ingot and 8 aluminium ingots make 648 kg of ferrotungsten (78 % W). 10 minutes a firing; the alloy waits for the
  bagging station's 1 t bags (two FeMo batches fill a bag). A firing with the fan not yet running reads NO FAN, but still burns.
- **Why:** the aluminium is the spec's 0.53 t a tonne of FeMo and 0.3 t a 1.04 t of WO3. Iron ingots stand in for the spec's "iron ore or
  mill scale" (the mod has no mill scale); the spec gives no iron for FeW, and 1 ingot makes it 78 % W.
- **Alternatives:** batches of 1.15 bags so one firing is a tonne; refuse to fire without the fan.

### 15. Chromium metal is not built
- **Chosen:** left out. **Why:** its chromic oxide comes from zirconium-extensions (17c2), not built.

### 16. Layout
- **Chosen:** the transformer stands directly against the furnace's west face (the spec's sketch shows a gap cell); the bank against the
  transformer's south face; the bagging station at the taphole on the furnace's east face (z 3 of 7). All parts rotate four ways.
- **Why:** the furnace finds its transformer by touching, as every two-part unit in the mod does.

### 17. The test grid
- **Chosen:** the `ferroalloys` scene uses the 45 MW aeroderivative, its 60 MVA GSU and the 60 MVA substation transformer (the client
  scene `peaker`'s yard) rather than the 16.5 MW turbine and a smaller unit.
- **Why:** the spec names a 13.8 kV gas turbine, its GSU and a 138/12.47 kV substation transformer; the 60 MVA unit is the mod's only
  138/12.47 kV transformer.

### 18. Art
- **Chosen:** the five parts are true-size models in the mod's materials (`tools/models/ferroalloys.py`); the seven bags' icons are the
  Ludo bulk bag with a swatch drawn by script, named `*_placeholder`. No Ludo credits spent.

### 19. Ore tooltips
- **Chosen:** chromitite, manganese ore and limestone add the furnace to their uses; molybdenite and scheelite are no longer "No use yet".
