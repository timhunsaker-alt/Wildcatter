# Phase 19a and 19b, the reactor extensions (2026-11-10): decisions taken while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest". This wave builds 19a (containment and protection) and
19b (dry casks, contamination, fresh-fuel inspection) of `docs/multiblocks/reactor-extensions.md`; 19c and 19d stay plans in ROADMAP.
Branch `phase19`. Format as in `README.md` here. **Needs Tim** marks the entries that wait on him.

The rule for existing worlds: nothing may change or slow what an existing reactor already does. Every rule that would is a key in
`config/wildcatter.properties`, off unless it says `true`, read when the server starts (as `standard` and `wattsPerFe` are). The parts
that only add (the containment and its parts, the relief tank, casks, the inspection stand, the personnel monitor, the washer) need no
key: a reactor without them runs as before.

**Waiting on Tim:** 1-4, 6, 7 (each rule: default on for new worlds?); 5 (the OP-delta-T constants, unverified); 8 (keys per server or
per world); 15 (the contamination numbers); 18 (dry casks against real decay heat: count cooling time 100x for eligibility?).

## The gated rules (each: default on for new worlds?)

### 1. Default on for new worlds? `reactorLtop` (the P-T curve, LTOP and the vessel's failure) — **Needs Tim**
- **Chosen:** off by default. On, the console alarms P-T LIMIT over the curve (3.4 MPa up to 100 C, a straight line to 15.5 MPa at 290 C)
  and counts each time on the vessel; LTOP NOT ARMED below 150 C while not armed; a sneak-click on the console's lower face arms LTOP,
  which lifts the PORVs at 3.2 MPa below 150 C; over twice the curve the vessel fails (a LOCA at 260 kg/s, the PORVs' rate times ten).
  With the rule on, the pressurizer's heaters also hold the pressure under 90 % of the curve, so a heat-up from cold follows it.
- **Why:** an existing plant heated from cold today runs its pressurizer to 15.4 MPa with the loop at 30 C; with the rule on that is
  over twice the curve and would fail the vessel. A world already built must never meet that. The heaters' 90 % hold is what an
  operator does by hand; without it the rule would fail every first heat-up.
- **Alternatives:** on for worlds created after Phase 19 (a world-stored flag beside the electrical standard); or on everywhere
  with the failure left out (alarms only). The curve above 290 C is not binding here (the safety valves cover it); a real curve keeps
  rising.
- **Tim (2026-11-10):** on in new worlds, off in existing ones (answer 23): the world rule `reactorLtop`.

### 2. Default on for new worlds? `reactorHeatupLimit` (the 55 C/h alarm and the fatigue count) — **Needs Tim**
- **Chosen:** off by default. On, the console works out the heat-up rate in plant time (the game's rate over the 80x compression),
  averaged over 30 s, and alarms HEAT-UP RATE over 55 C/h while the loop is under no-load temperature; each excursion adds one to a
  fatigue count on the vessel. It does no damage.
- **Why:** it changes the readout of every existing reactor and adds an alarm to an existing heat-up. Restricted to below no-load,
  because the power ascension's Tavg program (291.7 to 308.9 C in minutes) would alarm on every load change.
- **Alternatives:** on for new worlds; a cool-down alarm too (8d's cool-down already holds 55 C/h on its own).
- **Tim (2026-11-10):** on in new worlds, off in existing ones (answer 23): the world rule `reactorHeatupLimit`.

### 3. Default on for new worlds? `reactorRealHeatup` (the 80x compression removed) — **Needs Tim**
- **Chosen:** off by default, as the spec says. On, cold to hot standby takes the real 8-12 hours (the RCS, steam generators, pressurizer
  and the cool-down all at real rate).
- **Why:** it slows every existing heat-up 80 times.
- **Alternatives:** the spec calls it a world setting; it is a server-wide config key here, as the others (a per-world flag would need a
  Create World option). Tim to say whether it should move to the Create World screen.
- **Tim (2026-11-10):** on in new worlds, off in existing ones: "including `reactorRealHeatup` (the reactor heats up in real hours)" (answer 23). A world rule, set by `/wildcatter rule`, not a Create World option.

### 4. Default on for new worlds? `reactorDeltaTTrips` (the full OT-delta-T and OP-delta-T trips) — **Needs Tim**
- **Chosen:** off by default. On, both setpoints are worked out each step and trip the reactor naming OT-DELTA-T or OP-DELTA-T, ahead
  of the simplified 343 C check, which stays as a backstop. OT: K1 1.1265, K2 0.0221 /C, K3 0.129 /MPa, the lead-lag 30 s over 4 s,
  T' 308.9 C, P' 15.51 MPa, delta-T0 33 C; f(delta-I) left out (the model has no axial shape). The lead-lag is worked out exactly over a
  step, and starts from the present Tavg after loading or switching the rule on (no false transient).
- **Why:** a reactor that runs today could trip on a fast Tavg rise or on natural circulation at a few per cent (delta-T 55 C at 5 %
  with no pumps); a real plant would, an existing world must not start to.
- **Alternatives:** on for new worlds.
- **Tim (2026-11-10):** on in new worlds, off in existing ones (answer 23): the world rule `reactorDeltaTTrips`.

### 5. The OP-delta-T constants — **Needs Tim**
- **Chosen:** K4 1.0864, K5 0.02 /F (0.036 /C) on rising Tavg only with a 10 s rate filter, K6 0.00277 /F (0.0050 /C) above T''. These
  are from memory of the same H. B. Robinson 2 note and are **unverified**; the spec left them "not yet found".
- **Why:** at full power they give a setpoint of 35.8 C against the 33 C delta-T, 108 % power before a trip, which is how real plants
  sit; they only matter with rule 4 on.
- **Alternatives:** read Note 2 in full (NRC ADAMS; the accession number was not recorded either) and replace them.

### 6. Default on for new worlds? `reactorContamination` (coolant activity, contamination, pick-up) — **Needs Tim**
- **Chosen:** off by default (19b; see 14-16).
- **Why:** it puts contamination round a part broken in an existing plant, and doses players who walk there.
- **Alternatives:** on for new worlds.
- **Tim (2026-11-10):** off even in new worlds (answer 23): the world rule `reactorContamination`, set by `/wildcatter rule`.

### 7. Default on for new worlds? `fuelHandlingDamage` (bent grids) — **Needs Tim**
- **Chosen:** off by default (19b; see 17).
- **Why:** with it on an assembly moved into the refuelling machine may get a bent grid and, loaded uninspected, a leaking rod: an
  existing outage would change.
- **Alternatives:** on for new worlds; on only with the inspection stand built.
- **Tim (2026-11-10):** on in new worlds, off in existing ones (answer 23): the world rule `fuelHandlingDamage`.

### 8. Server-wide keys, not per world — **Needs Tim**
- **Chosen:** each rule is one key in `config/wildcatter.properties`, read at server start, as the brief asked. A new config file is
  written with the keys listed in a comment; an existing one is never rewritten.
- **Why:** it is the mechanism `standard` and `wattsPerFe` already use, and a key off by default cannot touch an existing world.
- **Alternatives:** store each rule with the world when it is created (as the electrical standard is), so "on for new worlds" becomes
  possible without touching old ones. That is the natural next step if Tim answers yes to any of 1-7.
- **Tim (2026-11-10):** "one pattern for every opt-in rule: per-world rules, on in new worlds and off in existing ones, changeable by command" (answer 23). Built: the rules are saved with the world (`RulesData`), `/wildcatter rule <name> [true|false]`; the config file keeps only `standard` and `wattsPerFe`. How an existing world is told from a new one: `world-rules.md` 1.

## 19a: containment and protection

### 9. How the building closes
- **Chosen:** the monitor flood-fills from the cell in front of it every 10 s (or on a sneak-click), through every block except
  containment concrete, a shut equipment hatch and the airlock. Machines inside are passed through; air cells count as free volume.
  Over 60,000 m3, or more than 150,000 cells, or into unloaded or out-of-height space, it reads OPEN with the farthest cell reached.
  So the floor must be containment concrete too (a basemat). The lower compartment is the 5,000 m3 nearest the vessel (half the building
  if smaller); "lower" is below 13 blocks over the vessel's base, for placing recombiners and igniters.
- **Why:** it needs no extra marker blocks and finds the vessel, the recombiners and the igniters on the way. A building of any other
  block reads OPEN, which keeps "containment concrete" meaningful.
- **Alternatives:** a declared box (corners marked by the player); counting walls instead of air.

### 10. The airlock and the hatch
- **Chosen:** the airlock's two doors are interlocked, so it always seals; a click takes the player to its other end. The hatch opens
  and shuts on a sneak-click; open, the building is OPEN. At 2.5 times design pressure (1.0 MPa) the hatch's seals fail and it stays
  open until a sneak-click fits new seals.
- **Why:** a real airlock is never open both ends; teleporting through is the simplest way to cross a solid true-size part.
- **Alternatives:** door states drawn open (the blocks become passable) with the interlock enforced.

### 11. Hydrogen, recombiners and igniters
- **Chosen:** a closed building takes the reactor's hydrogen (8d's) into its lower compartment instead of 8d's notional 5,000 m3, and
  mixes it into the dome with a one-hour time constant. A recombiner removes 1.5 kg/h at 4 % (in proportion, up to 1.5 times at 6 %)
  above 1 %. An igniter is a 160 W glow plug, a residential-tier load like the lamp; powered and in a compartment at 4 %, it burns that
  compartment's hydrogen down to 0.5 % with no blast (flame particles). At 4 % with no powered igniter in it, the compartment blasts
  (8d's power 6). Without a closed building, 8d's blast at 18 kg is unchanged.
- **Why:** the spec's values; 160 W is about a real igniter's 1.3 A at 120 V *(unverified)*. The recombiners' rate is small against a
  core's Zircaloy fire, as real PARs are; the igniters do the work.
- **Alternatives:** igniters burning continuously from about 5 % (a real lean burn); recombiner capacity in proportion to the building.

### 12. Building pressure
- **Chosen:** steam let into a closed building (the PORVs' discharge with no relief tank or past a burst disc, and a failed vessel's
  coolant) is counted as an ideal gas at 100 C over the free volume; it condenses on the liner with a 30-minute time constant. The
  monitor alarms OVER DESIGN above 0.4 MPa gauge. Containment spray and fan coolers are left out, as the spec says.
- **Why:** simplified, as the spec allows; only a big LOCA in a small building reaches the design pressure.
- **Alternatives:** a real steam-air mixture with saturation; containment spray (an Idea).

### 13. The relief tank
- **Chosen:** 30 m3 at 30 C; what the PORVs let go (the 8d boil-off and the pressurizer's lift, as kg and enthalpy) condenses in it; its
  pressure is its water's saturation pressure; the disc bursts at 0.7 MPa (about 165 C) and the rest goes on to the building. Its spray
  draws 10 kg/s of the CVCS's makeup water. A sneak-click fits a new disc once the water is under 100 C. Found touching the vessel or the
  pressurizer.
- **Why:** the spec's values; the tank's nitrogen cover is left out, so the disc bursts on the water's temperature alone.
- **Alternatives:** a cover gas, a drain to a reactor coolant drain tank, and the tank's level.

## 19b: dry casks, contamination, fresh-fuel inspection

### 14. Coolant activity
- **Chosen:** with `reactorContamination`, the coolant carries an activity in µCi/g dose-equivalent I-131, the tech spec's unit (limit 1.0,
  NUREG-1431 LCO 3.4.16; COOLANT ACTIVITY alarms over it). Sources: activated corrosion products, about 0.02 µCi/g at full power, and each
  leaking assembly, about 0.2 µCi/g at full power, with half a leaker's source as an iodine spike while decay heat is high after a trip.
  Removal: I-131's decay and the CVCS's letdown through its resin (0.5 kg/s of 30 t, a decontamination factor of 10), worked out exactly
  over a step. The CVCS reads it with the count of leakers.
- **Why:** the spec's "rises with leaking rods and falls with the demineralizer's resin", in the unit a real plant uses. All values
  *(proposed)*; leakers come from bent grids now and would come from 19c1's rod defects later.
- **Alternatives:** separate iodine, noble gas and corrosion product nuclides; leakers raising the activity in proportion to their burnup.

### 15. Where contamination gets out, and how much — **Needs Tim** (the numbers)
- **Chosen:** each release contaminates the top surface of every column within a radius (falling to half at the edge), as a share of
  "fully contaminated" (1 mSv/h at 1 m): a primary part (vessel, steam generator, coolant pump, pressurizer, CVCS) broken by a player with
  water in the RCS, 8 m at the coolant's strength (its activity over the limit, at least 5 %); the relief tank's disc, 6 m at that
  strength; core damage, 32 m at full strength (16 m inside a closed containment); the hatch failing, 32 m (full with a damaged core,
  20 % without); a spent fuel pool drained below its fuel, 8 m at 50 %. Stored sparsely by block, decaying with a 30-day half-life in
  game time. Dose: the contaminated blocks within 16 m, inverse square, no shielding counted (they are surfaces).
- **Why:** the spec's ranges (3 to 32 blocks by what failed); surface-only keeps a release to a few thousand entries.
- **Alternatives:** contaminating blocks through a volume, spreading by water flow, or line of sight in the dose.

### 16. On a player; the monitor and the washer
- **Chosen:** each second a player stands on a contaminated block they pick up 2 % of it (the block loses it), and carry it as a dose
  of up to 2 mSv/h until washed; it decays like the blocks'. The personnel monitor is a 1 x 3 x 1 booth on the residential tier
  (single-phase, 1.5 kW): a click reads the player and washes them clean, while powered (a player cannot step into a solid true-size
  part). The washer is an item: it works within 4 m of a live receptacle and with a water bucket in the inventory; it cleans 90 % a click.
- **Why:** the spec's behaviour with the simplest interaction. **Not modelled:** the washer's 2 kW is not drawn from the receptacle
  (an item is not on the circuit) and its water is not used up.
- **Alternatives:** the washer as a placed skid with a hose (a real circuit load); a walk-through portal monitor block.

### 17. Bent grids and the inspection stand
- **Chosen:** with `fuelHandlingDamage`, an assembly gets one roll, 1 %, at its first move by hopper into the inspection stand or by the
  refuelling machine's crane into the core; the result rides on the item (`handled`, `bentGrid`, `inspected` in its custom data, shown in
  its tooltip once inspected). The stand (2 x 6 x 2, 5 kW at 480 V, hopper in on top and out below, 60 s an assembly) marks it INSPECTED and
  finds a bent grid. The refuelling machine and the vessel refuse an inspected bent assembly; the assembly fixture straightens one put in
  its hopper (a new repair bin; 10 minutes; no sneak-click needed), and it goes out uninspected. Loaded unseen, a bent grid becomes a
  leaking rod once the assembly has burned a third of a cycle. An assembly loaded by hand gets no roll.
- **Why:** one roll per assembly keeps inspection meaningful (an inspected assembly cannot be bent again on the way in).
- **Alternatives:** a roll on every move (the spec's wording), so a crane move after inspection could still bend it; a sneak-click on
  the fixture to start the repair, as the spec says.

### 18. The dry cask limits against the model's decay heat — **Needs Tim**
- **Chosen:** the spec's limits, 1.5 kW an assembly and 12 kW a canister, against the model's real (uncompressed) decay heat. With 8d's
  decay fit, an assembly from a full-power run falls to 1.5 kW about 160 days of world time after discharge; a pool of 36 fills after
  about 53 days at full power (3 assemblies a 107-hour cycle). So in play a full pool cannot yet empty into casks.
- **Why:** the limit is the canister's real thermal design (cladding under 400 C); burnup is compressed 100x but decay heat is not.
- **Alternatives (for Tim):** count cooling time 100x for cask eligibility, as burnup is; raise the per-assembly limit (about 40 kW
  would take fuel about a month old); or a bigger pool. Recommended: count cooling time 100x for eligibility only.
- **Tim (2026-11-10):** "count cooling 100x for cask eligibility only" (answer 20). Built: each record keeps its time since discharge and the station's limits read its decay heat at 100x that time; the heat carried stays real. Scene `cask_cooling`.

### 19. Linking a module; one canister each
- **Chosen:** a sneak-click on the station with the Power Meter links the nearest empty storage module within 64 m. A module holds one
  canister; after a transfer the link clears and the next canister needs another module. The transfer cask is abstract (10 minutes).
  The pool's controller takes a sneak-click for LOAD CASK (it had none before; a click still reads it). The module is unbreakable while
  loaded (no destroy progress) and has blast resistance 1,200.
- **Why:** the spec's link and limits; the nearest-empty rule needs no second click on the module.
- **Alternatives:** a click on the module then the station; a crawler transporter vehicle.

### 20. Additive only
- **Chosen:** the casks, the module's dose, the inspection stand, the personnel monitor and the washer need no rule: with none built,
  nothing changes. Contamination and its dose, the coolant activity and bent grids are behind rules 6 and 7. The CVCS's readout adds the
  activity only with the rule on.
- **Why:** the rule for existing worlds.
- **Alternatives:** none needed.

## Testing

### 21. How each rule is tested
- **Chosen:** unit tests (`ReactorLimitsTest`) switch each rule on and hand it back after each test, and check the same transient
  without the rule changes nothing. The NeoForge scene `reactor_limits` switches LTOP, the heat-up limit and the delta-T trips on in turn
  and hands them back (also before any failed check); it runs in a test environment of its own, so no other scene is in its batch. The
  OT-delta-T trip in the scene is at 8 % on natural circulation with the power pinned by a test hook (`pinPowerForTest`), since a
  scene without coolant pumps cannot hold a critical core.
- **Why:** a rule left on would change other scenes' reactors.
- **Alternatives:** a full critical startup in the scene (the Fabric `reactor` scene's island, 15 minutes of client time).
