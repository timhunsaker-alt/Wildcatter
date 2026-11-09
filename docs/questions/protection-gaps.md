# The protection gap wave: decisions taken while Tim was away (2026-11-09)

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back ...
close all known gaps". This wave closes the Known gaps of Phase 10d1, 10d2, 10d3 and Phase 2's distribution. Format as in
`docs/questions/README.md`.

### 1. Which inverse-time curves can a relay be set to, and on what dial?
- **Chosen:** the three IEEE C37.112 curves, MI (A 0.0515, B 0.114, p 0.02), VI (19.61, 0.491, 2) and EI (28.2, 0.1217, 2), with
  time dial steps 0.5, 1, 2, 3, 4, 5, 7, 10 and 15, and 50 at off, 4, 6, 8, 10, 12, 16 or 20 x the pickup. A new relay starts where
  the fixed one was: MI, TD 1, 50 at 8 x. The 51V on a generator keeps its own very inverse (3.922, 0.0982) as built in 10d1.
- **Why:** these are the curves every North American feeder relay (SEL-751, SEL-351) offers by name, and the dial range is the
  standard's. One curve family keeps the dial meaning the same whichever curve is picked.
- **Alternatives:** the IEC 60255 curves (SI, VI, EI, LTI on a 0.05-1 TMS) too, for IEC worlds (real relays offer both; it doubles the
  settings to step through); the U.S. U1-U5 set.

### 2. How does a player set a relay?
- **Chosen:** from the relay's front panel with the Power Meter: right-click on the relay door (the upper cells) reads the relay and moves
  its cursor to the next setting (pickup, curve, dial, 50); sneak and right-click steps the setting the cursor is on. The readout shows
  the selected one in brackets. Elsewhere the old gestures stay (the switchgear's top cell steps its frame).
- **Why:** a real relay is set from its front-panel arrows the same way, one setting at a time; no new screen or packets are needed, and
  the old gesture (sneak-click steps the pickup) still works on a new relay, whose cursor starts on the pickup.
- **Alternatives:** a settings screen (new client screen and network packets on both loaders); setting through the PLC's HMI.

### 3. Pickup steps for the 15 kV feeders
- **Chosen:** 2,000 and 3,000 A added to the pickup steps (5 to 3,000 A), capped at each breaker's rating.
- **Why:** the 13.8 kV feeder breakers come in 2,000 and 3,000 A, and their relay used to pick up at the rating.
- **Alternatives:** a per-relay CT ratio and tap (more real, more settings).

### 4. How long does racking take, and what is remote racking?
- **Chosen:** 15 s a position (CONNECTED to TEST, TEST to DISCONNECTED), during which the breaker cannot close. Remote racking is two
  PLC command points on each section, RACK IN and RACK OUT, with the selector on REMOTE; each moves the truck one position.
- **Why:** a 5 kV draw-out breaker takes about 20 turns of the racking handle between positions; a remote racking device (a motor
  on the racking shaft, worked from outside the arc-flash boundary) does the same thing more slowly or at the same pace. The PLC is the
  mod's existing remote; no new block is needed.
- **Alternatives:** a portable remote racking device item; an arc-flash hazard when racking live gear by hand (the reason remote racking
  exists, not modelled).

### 5. What makes a breaker fail to open (for 50BF to have work)?
- **Chosen:** being asked to interrupt more than its interrupting rating (41 kA for the 5 kV switchgear, 350 MVA class; 50 kA for the
  15 kV generator-bus feeders), or a stuck mechanism (a test hook only; nothing in play sticks a mechanism yet). A hand opens a stuck
  breaker afterwards.
- **Why:** these are the real causes; the rating is a published value (IEEE C37.06). Random failures would make play feel arbitrary.
- **Alternatives:** mechanism wear after a number of fault operations (vacuum interrupters are rated for about 30 to 100 full-fault
  operations); loss of DC trip power (the mod has no station battery).

### 6. 50BF settings and what it trips
- **Chosen:** a current detector at 2 A and a timer of 0.15 s (62BF). On the switchgear it trips every other breaker in the lineup and
  locks them out at the main (86BF); on the 13.8 kV bus a failed feeder trips the whole bus as 87B does. The breaker relay cabinet has
  no 50BF (it knows no breaker above it).
- **Why:** 50BF is normally set below the minimum fault and timed at 8-10 cycles; a failed feeder breaker is cleared by opening every
  source to its bus.
- **Alternatives:** transfer-tripping the transformer's primary breaker when a main fails (needs a zone search through the
  transformer); 50BF on the cabinet by searching upstream.

### 7. The 4.16 kV lineup's bus differential (87B)
- **Chosen:** carried by the main section, always in service; minimum pickup half the main's 51 pickup, slope 0.3 of the current through
  the lineup's breakers; trips every breaker in the lineup with 86B lockout, reset at the main. Faults on the bus are made with the
  grounding clamp set on any section's bus compartment.
- **Why:** a real lineup's 87B (SEL-487B class) sits in the main's or a dedicated relay compartment and trips every source and feeder.
  Basing its floor on the main's pickup keeps it sensitive whatever source feeds the lineup (a frame-based floor of 240 A would miss a
  small genset's bus fault). The real ground-and-test device racks into a cubicle; clamps on the bus compartment stand in for it.
- **Alternatives:** an 87B setting on its own; a separate bus differential section; a ground-and-test device item.

### 8. Reclosing (79) on the breaker relay cabinet
- **Chosen:** up to 3 shots after open intervals of 2, 15 and 30 s; a 60 s reset time; lockout after the last shot, reset by hand. A
  12.47 kV cabinet starts with 3 shots, a 138 kV one with 1, and the shots are a setting (0 turns it off). Only 50/51 trips reclose.
- **Why:** typical utility practice: overhead distribution feeders reclose three times (most faults are temporary flashovers), and
  transmission lines once. Never reclosing an 81U trip is how load shedding is done.
- **Alternatives:** an instantaneous first shot (0.3-0.5 s, common on feeders with fuse-saving); off by default, so old worlds behave as
  before.

### 9. Where the 13.8 kV feeder relays' settings live
- **Chosen:** on each 15 kV feeder breaker (its upper cells are the relay and control compartment), read and set with the Power Meter.
  Until set they follow the breaker's rating as in 10d1 (pickup at the rating, MI, TD 1, 50 at 8 x). The 51V on the generator and the
  87B slope stay fixed.
- **Why:** each feeder has its own relay; setting it at the breaker is where a technician would find it, and the panel stays the
  place for the bus-wide elements.
- **Alternatives:** settings at the bus relay panel, one page per feeder; a settable 51V dial.

### 10. Which transformers carry 87T, and where does the relay live?
- **Chosen:** every true-size, three-phase rated transformer of 10 MVA and up except the substation yard's 20 MVA unit: the 25 and
  60 MVA GSUs and the 10 MVA 4.16/138 kV unit. The relay is built into the transformer; its readout and its 86T reset are there.
- **Why:** IEEE C37.91 practice puts differential protection on power transformers from about 10 MVA; pad-mounts and unit
  substations are protected by fuses or overcurrent relays. The yard unit already has its relay panel. No new block to place.
- **Alternatives:** a separate transformer relay panel block (truer to a station, one more part); 87T on the yard transformer too,
  inside its relay panel.

### 11. The 87T zone and its settings
- **Chosen:** an overall differential on the breakers' CTs: the transformer plus its cable, busway, bus duct, station bus and
  disconnects up to the first breaker on each side (yard breaker, 15 kV feeder breaker, switchgear section, generator breaker). Minimum
  pickup 0.3 per unit of rated current, slope 25 %; the difference is the current into grounds in the zone, as 87B measures it. It opens
  every bounding breaker (a generator breaker means a unit trip); 86T holds them, and a breaker closed while locked out opens again.
- **Why:** typical SEL-787 class settings (O87P 0.3, SLP1 25 %). Breaker CTs put the transformer's leads inside the zone, as in a station.
- **Alternatives:** bushing CTs (faults on the leads left to the bus or line relays); a second slope, an unrestrained 87U and harmonic
  restraint (the model has no inrush harmonics, so they would do nothing).

### 12. 87G settings, and how a fault inside a generator is made
- **Chosen:** the grounding set goes on the big generator itself (grounds on its terminals, between its neutral CTs and its breaker's);
  minimum pickup 0.1 per unit of full-load current, slope 10 %; a unit trip with 86G, released by the prime mover's reset.
- **Why:** the generator touches its breaker directly, so there is no lead to clamp. Generator differentials are set more sensitively
  than transformer ones (no inrush, no tap changer; SEL-700G class).
- **Alternatives:** internal winding faults from stator overheating (a new failure mode that changes existing scenes); 87G on the
  gensets too.

### 13. A 138 kV line back-feeding a GSU onto a generator-bus fault
- **Chosen:** when a GSU stands straight off the bus, 86B also opens the breakers bounding the GSU's 138 kV side (transfer trip), and
  the panel's reset closes them. The bus is also inside that GSU's 87T zone, so 86T may lock out as well; then both are reset.
- **Why:** this is how a unit-connected station does it: the bus lockout trips the GSU's high-side breaker.
- **Alternatives:** requiring a 15 kV feeder breaker between the bus and the GSU.

### 14. When do the grounding clamps burn?
- **Chosen:** ASTM F855 grade 3 (2/0 copper), ultimate rating 35 kA for 15 cycles: I²t 3.06 x 10^8 A²s. A fault cleared on the solve it
  appeared in still counts 5 cycles (relay plus a 3-cycle vacuum breaker); a fault left on counts as long as it lasts, only the current
  above 300 A (the cable's continuous rating). A heated set cools back over 5 minutes. Past the rating the clamps burn off: a flash, the
  cell reads UNGROUNDED, and the item is not used up.
- **Why:** the set's cable is 2/0, which is F855 grade 3; the adiabatic melting limit of 2/0 copper is about 3.7 x 10^8 A²s, close to it.
  A 40 kA bus fault cleared by 87B leaves the set whole; 10 kA that nothing clears burns it in about 3 s.
- **Alternatives:** the withstand rating (27 kA for 15 cycles, 1.8 x 10^8) as the burn point; the item consumed on a burn.
