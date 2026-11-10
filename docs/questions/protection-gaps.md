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

### 15. How hot can an overhead span run before it fails?
- **Chosen:** a span (and a lead) heats on its conductor's ampacity (75 C conductor in 25 C air, a 50 K rise) with a 600 s time constant,
  and parts and falls at 3 x its rated rise (about 175 C): twice the rating lasts about 11 minutes, five times about 50 s.
- **Why:** bare 4/0 ACSR in still air has a thermal time constant of about 10 minutes (IEEE 738); ACSR's emergency limit is 100-150 C,
  and above it the aluminium anneals and the span sags and parts. Building wire's 2-minute constant would burn spans far too fast.
- **Alternatives:** the building-wire model (120 s, 1.8 x); annealing as a permanent loss of strength.

### 16. What shatters an under-rated insulator?
- **Chosen:** the charge of the power-follow arc, read before any relay acts: the arc current over the solve interval, or over 5 cycles
  at the island's frequency if a relay cleared it at once; it shatters at 50 A·s. The old 3 s above 1 A rule stays.
- **Why:** a flashover arc at fault currents cracks porcelain within cycles; before this a fault cleared at once never shattered
  anything, so the path could not happen.
- **Alternatives:** shatter on any flashover at once; the arc's I²t.

### 17. How is the house's neutral earthed?
- **Chosen:** a load centre or panelboard with a ground rod (touching it, or by ground wire) is the service: its main bonding jumper ties
  N to G, and the rod earths it at 25 ohm. A panel with no rod is a sub-panel, N and G kept apart. The readout gives the bonding jumper's
  current, or "neutral not earthed here".
- **Why:** NEC 250.24 bonds N to G only at the service disconnect, with a grounding electrode there; NEC 250.53(A)(2) allows 25 ohm for
  a single rod. The rod is how a player marks "this is the service".
- **Alternatives:** a meter-base block that does the bonding (closer to real, one more part); bonding every load centre (wrong for
  sub-panels).

### 18. Leads (jumpers) from the line down to a cutout or pole transformer
- **Chosen:** strung with the line's own conductor spool from an insulator to a cutout's line contact or a pole unit's H1: at most 6 m,
  two per insulator and one per part, voltage matching. A real circuit link, drawn slack. Wire blocks to the faces still work.
- **Why:** a real primary lead is a short piece of line conductor clamped from the line to the cutout or bushing, so no new item; 6 m
  reaches from the arm to a unit hung a few metres below.
- **Alternatives:** a separate lead-wire item; leads between any two parts (cutout to transformer), which needs a renderer on the cutout.

### 19. What comes down when a pole is broken?
- **Chosen:** a pole transformer or fused cutout that has hung beside a pole (latched the first time it is solved there) drops as an
  item when no pole is left beside it, on top of the arms and insulators that already fell.
- **Why:** pole equipment is bolted to its pole. The latch keeps unit substations, yard cutouts and older builds safe.
- **Alternatives:** an "on pole" block-state flag set at placement (misses units placed by command and older worlds).

### 20. Guy wires (BUILT 2026-11-10 on Tim's answer)
- **Chosen:** not built in this wave. The plan: a `guy_anchor` block and a guy strung from the pole top with a `guy_strand` item (3/8 in
  EHS, 15,400 lbf breaking, NESC strength factor 0.9). Each pole sums its spans' horizontal pull at about 25 % of rated breaking strength
  (Sparrow 2,850 lbf, Raven 4,380, Penguin 8,350, Linnet 14,100, Drake 31,200) against its ANSI O5.1 class load at the top (30 ft class 5:
  1,900 lbf; 40 ft class 4: 2,400; 75 ft class 2: 3,700) plus its guys' horizontal share. The readout warns "UNGUYED: N lbf of line pull
  on a class 4 pole rated 2,400 lbf". Ludo art for the anchor.
- **Why not built:** a new block, item, block entity, renderer, art and loading model, and the effect is a design call: a pole that
  fails would bring down every existing world's unguyed line ends and several client scenes.
- **Question:** should an overloaded, unguyed pole only warn, lean, or break after some minutes?
- **Alternatives:** guys as decoration only; a guy required before a dead-end span can be strung.
- **Tim (2026-11-10):** "an overloaded pole with no guy reads UNGUYED, then leans after a while. It never breaks on its own." Built on
  branch `w-grid` (`guy_anchor`, `guy_strand`, block/PoleLoad): see `docs/questions/answers-grid.md` 2.

### 21. A pole transformer placed by command (BUILT 2026-11-10 on Tim's answer)
- **Chosen:** not changed yet. The fix is a rule in `TransformerBlock.onPlace`: a pole unit with a pole beside it, but not behind it,
  turns its back to that pole (player placement already does this).
- **Why not done:** the facing decides the terminals (front and top high side, back and bottom leg A, sides leg B), so turning a unit
  rewires it. Seven client scenes set units by command wired to their current facing (`transformerChain`, `poleTransformer`,
  `serviceDrop`, `panels`, `feederRelay`, the scene near line 6337 of the client test, the cutout in `lineParts`); they must be re-laid
  and run on the client, one at a time, by the lead.
- **Alternatives:** re-lay those scenes (pole behind each unit, leg-A loads to the bottom face) in a wave where the client suite runs; a
  drawing-only bracket property (drawn bushings would then not match the terminals).
- **Tim (2026-11-10):** "face the pole for new placements only." Built on `w-grid`: a new unit in the default facing turns its back to its
  pole; a named facing and a unit from an older save are kept (a `faced` marker). No scene is rewired: `answers-grid.md` 3.

### 22. Arc-fault (AFCI) breakers?
- **Chosen:** none built; documented as a limit.
- **Why:** a UL 1699 AFCI trips on an arc's high-frequency signature (kHz noise, shoulders at each zero crossing). The solver is a
  50/60 Hz phasor solve, where an arc is only a resistance like any other fault, so an AFCI could only be a breaker tripping on a made-up
  rule.
- **Alternatives:** treat worn-wool hot-to-neutral contact as an "arcing" contact an AFCI trips on (not backed by the physics, and it
  would change how shorts behave in existing scenes).

### 23. How a shock trips a GFCI
- **Chosen:** for one second after a shock, the part that shocked puts 1,000 ohm from its live conductor to earth (IEC 60479-1
  hand-to-feet, 50th percentile, dry). A GFCI upstream sees about 120 mA and trips (UL 943 Class A at 5 mA; an RCD at 30 mA). The first
  shock still lands: a GFCI limits how long a shock lasts. Bare wires and ungrounded lamps and heaters add the path.
- **Why:** this is how a real GFCI sees a person: current to ground.
- **Alternatives:** body impedance by voltage, wetness and contact area (IEC 60479 tables); the path on every part that shocks,
  including MV (where no GFCI exists).

### 24. ATS timers
- **Chosen:** a new ATS runs the factory defaults (TDES 3 s, TDNE 0 s, TDEN 30 min, TDEC 5 min); sneak and right-click the bottom cell
  with the Power Meter switches to the 10d3 SHORT set (3 s, 1 s, 30 s, 30 s). A switch saved before this wave keeps SHORT.
- **Why:** the UL 1008 controllers' factory defaults; old worlds keep their behaviour.
- **Alternatives:** each timer settable on its own (a PLC or HMI menu); the short set as the default.

### 25. Closed transition and the in-phase monitor on a one-frequency solver
- **Chosen:** between two live sources the ATS does OPEN (1 s neutral position), IN_PHASE (transfers within 15 degrees and under 2 Hz,
  0.1 s contact travel) or CLOSED (make before break for one solve, at most 100 ms, only within 5 degrees, 0.2 Hz and 5 % voltage);
  with no sync after 30 s it falls back to open. Leaving a dead source is always open. The angle is emergency phase A against normal
  phase A, from the solve. Set with the Power Meter (sneak-click the top cell).
- **Why:** usual closed-transition passive-sync settings; UL 1008 limits the overlap to 100 ms.
- **Limit:** the solver gives a connected group one frequency, and the ATS joins both sources into one group, so in play they never
  slip: the angle is only the load-angle difference and both modes find their window at once. Scenes inject slip with a test hook.
- **Alternatives:** solve each side of an open ATS as its own island (a change to how CircuitManager groups circuits and frequency
  islands; a later plan).

### 26. Current-limiting let-through, the protection race, the coordination study
- **Chosen:** class J fuses limit above 15 x their rating, E-rated above 20 x; they clear in 0.5 cycle x (threshold / I)^(1/3), with a
  peak let-through of sqrt(2) x threshold x (I / threshold)^(1/3) (within about 10 % of UL 248-8's maximum peaks at 200 kA for 30-600 A,
  unit-tested) and the I²t of that triangle. On a fault solve only the fastest timed devices act (breaker magnetic and non-limiting fuse
  melt 1 cycle, GFCI on a heavy ground fault 1.5 cycles, current-limiting fuse under half a cycle), with anything within a quarter cycle
  of the fastest, and any device whose instantaneous pickup is under the winning fuse's let-through. Relays are untimed in the race and
  act as before. Interrupting ratings for the study: load centre 10 kA, panelboard 14 kA, one-block breaker 10 kA (proposed, UL 489
  floor), class J 200 kA, E-rated 50 kA (proposed), plug fuse and K link 10 kA (proposed). Series ratings left out.
- **Why:** the cube-root law is the shape of every maker's let-through chart and reproduces the UL limits. Two molded-case breakers both
  in their magnetic range tie and both trip, as real ones do. A series rating is a tested listing, not derivable from curves.
- **Alternatives:** a magnetic clearing time falling with the fault multiple (would make molded-case breakers look selective); vendor
  let-through tables per size; the available fault current from a second solve.

### 27. The coordination readout: where and how
- **Chosen:** appended to the Power Meter readout of each branch breaker, panel main, one-block LV or 480 V breaker or fuse, safety
  switch and fused load-interrupter: "coordination: under the ..., selective only up to N A". The device upstream is the panel main for a
  gutter's breaker; otherwise the first breaker, fuse or relay (switchgear section, relay cabinet) found walking the line side through
  conductors; the walk stops at a transformer or source.
- **Why:** no new tool to craft, and a real study's answer is "selective up to X kA".
- **Alternatives:** a separate coordination tool item; comparing across transformers (needs the turns ratio and the primary protection).

### 28. The panel main's handle
- **Chosen:** a `main` block-state (on, off, tripped) on the load centre and the 480 V panelboard; the handle shows through a window in
  the dead front, up for ON, down for OFF, centred with its flag out for TRIPPED, drawn from existing materials.
- **Why:** it matches the gutters, and real main-breaker panels show the main's handle.
- **Alternatives:** draw it only with the door open.
