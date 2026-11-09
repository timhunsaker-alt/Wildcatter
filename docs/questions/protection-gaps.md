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
