# Wave A (2026-11-19): decisions taken building Tim's answers C18-C23 and the faults short's findings

Tim's answers: C18 (HOA overlay), C19 (SFC reset and pause), C20 (memory card), C21 (a PLC may start a crane lift), C22 (one manual
check of the NeoForge editor), C23 (one Ludo art pass). The faults short asked for the breaker to be seen opening, with its arc and
sound, and a reclose onto a standing fault to be seen. Branch `w-wave-a`. Format as in `README.md` here. Each entry was **decided on
recommendation; for Tim**: none needs him before it can stand, but each can be changed.

### 1. The HOA overlay: what it shows, how far, and in which colours (C18)
- **Chosen:** while the control key is in either hand, each selector within 16 blocks is labelled over its device: HAND amber, OFF red,
  AUTO green (switchgear and relay-operated breakers read LOCAL and REMOTE, amber and green). A true-size part's label sits over its
  top; an MCC's buckets each get one out from the bucket's front. The server sends the list twice a second; the client drops it 1.5 s
  after the last one, so putting the key away clears the labels.
- **How another mod's devices join:** `ControlDevice.hoaPositions()` (a default method, empty). Every Wildcatter device with a selector
  answers it. **Tailings' six selector devices need a two-line `hoaPositions()` each to show** (their `cycleHoa` is theirs): until
  then they show nothing, as before.
- **Why:** a keyswitch's legend plate is the real thing; colouring the three positions as stop / caution / run reads at a glance. 16 blocks
  covers a plant room without sending a whole base. The server sends it because a device's selector lives in its own server data
  (`Remote`), which the client never had.
- **Alternatives:** only the device under the crosshair (less clutter, but a row of starters needs looking at one by one); a block
  state on each device (the option Tim did not pick).

### 2. SFC_INIT and SFC_PAUSE, where they live and what they do (C19)
- **Chosen:** both are BOOL variables every chart gets unless it declares them itself. In a FUNCTION_BLOCK chart they are inputs, set by
  the caller (`fill(SFC_INIT := reset)`); in a PROGRAM chart they are its own variables, which the chart may declare with an AT (a
  pushbutton or a marker) or as VAR_EXTERNAL to a global, or which the editor writes online. SFC_INIT, while on: every step off but the
  initial ones (each at T#0s), every action's control reset (a stored S action cleared), boolean actions off, the chart not moving;
  released, it runs from its initial steps. SFC_PAUSE, while on: no transition fires, the active steps' times stand still, and every
  action holds what it was doing (its variables keep their last values).
- **Why:** this is what the vendors' implicit variables do (SFCInit and SFCPause in common IEC editors). A program's variables are its
  own in this mod (another unit reaches one through a global), so the program chart's flags follow the same rule.
- **Alternatives:** `<chart>.SFC_INIT` written from another program (would break the "a program's variables are its own" rule);
  SFC_PAUSE also stopping timed qualifiers (L, D, SD, DS, SL) inside each action: they keep running in the action control, as a vendor's
  pause does not reach into function block timers either.

### 3. The memory card's retained values and boot (C20)
- **Chosen:** TO CARD writes the project and every RETAIN variable's value, by name and type. FROM CARD downloads the project (CPU in
  STOP, as before) and puts each value back into the RETAIN variable of the same name and type; anything renamed or retyped starts at its
  initial value, as an online change does. A CPU with no project and a card in its door, when its 24 V supply comes on, loads the card,
  takes its values and goes to RUN (a warm restart, so the values stand). A CPU that already has a project ignores the card at power-up.
- **Why:** real CPUs set to "load from card at power-up" do exactly this, and a replacement CPU in a dead cabinet is the case it serves.
  Matching by name is what keeps a card usable across a small program change.
- **Alternatives:** the card's whole memory image (refused on any change); boot to STOP (a person must start it; safer, but a
  replacement CPU then sits idle until someone opens the editor).

### 4. A PLC starts a crane lift: the interlocks (C21)
- **Chosen:** as `docs/multiblocks/crane-plc-lift.md`. LIFT START's rising edge in AUTO starts one lift; the crane's selector in AUTO is
  the operator's permissive. The interlocks are the crane's own: load chart and hook load (TOO HEAVY), reach and height (OUT OF REACH,
  TOO HIGH), the slew path (BLOCKED), ground bearing, wind, and a swing area clear of people. A start that finds a lift held mid-way
  (its RUN ENABLE dropped) finishes it. Readings: HOOK LOAD (0 to the chart's figure at its least radius), RADIUS (0 to its largest), WIND
  (0-50 m/s), AREA CLEAR, LIFT REFUSED.
- **The swing area:** a circle about the slewing centre of the lift's largest hook radius plus 3 m, from 2 m under the base to 2 m over
  the boom head; any player not in spectator mode inside it refuses a start, and holds a PLC-started lift on its brakes until it is clear.
- **Why:** a rated capacity limiter, an anemometer and a ground check already stood in the crane; the missing real interlocks were the
  permissive and the people. 3 m is the common barricade distance beyond the load and counterweight swing. The area check is limited to
  PLC-started lifts so a crane working under RUN ENABLE or in HAND behaves exactly as before (its program can read AREA CLEAR).
- **Alternatives:** a separate permissive input wired from an operator's station (the PLC could not fake it either way, since every input
  reaches the crane through the PLC's own outputs); a smaller area (the load path only), which real sites do with zoning systems.
- **Test hook:** `CraneBlockEntity.testSpeedup` now also speeds the lift steps (it was the ring crane's travel only), so the scene's four
  starts fit; it is 1 outside game tests.

### 5. The NeoForge editor check (C22)
- **Chosen:** a one-time run of the NeoForge client (`runPlcCheck`, a temporary run configuration not kept in the repo) that quick-plays
  the smoke world, places a cabinet with a CPU, DI16 and DO16, downloads a seal-in rung in ST and opens the editor; the game saved a
  screenshot itself (F2's mechanism, as window capture is not permitted). See PROGRESS.md for what it showed.

### 6. The breaker drawn open, its arc and its sound (faults short)
- **Chosen:**
  - The 12.47 kV and 138 kV yard breakers and the metal-clad switchgear sections carry their position on every cell, and their
    mechanism's position flag (the switchgear's also its position lamp) is drawn **red when closed, green when open** (the ANSI
    convention; IEC panels often use the reverse, but the flag's legend is what a person reads). Opening or tripping changes the flag
    at once; the breaker's trip sound plays as before.
  - The **arc is drawn at the fault**: grounding clamps flash and crack each time fault current starts through them (more than 10 A), so a
    reclose onto a standing fault flashes there again. No flash is drawn at the breaker: a vacuum or SF6 interrupter's arc is inside
    its sealed bottle or tank and is never seen.
- **Why:** this keeps what is shown true to the equipment. The flag is what a person on site looks at to know a breaker's state.
- **Alternatives:** a flash at the breaker's bushings (reads better in a short, but no real breaker of these kinds shows one); the
  IEC colours.

### 7. A reclose onto a standing fault is seen (faults short)
- **Chosen:** the breaker relay cabinet's 50 element picks up on the solve the fault appears in, and the breaker opens after
  `FAULT_CLEARING_CYCLES` (5 cycles: the relay's 1-2 and the vacuum breaker's 3), 83 ms at 60 Hz, 100 ms at 50 Hz, instead of within
  the same solve. The fault current flows for those solves (the clamps' heating already counted 5 cycles), so the breaker is seen closed
  for about 3 ticks (with the tick its close takes to reach the solver) before it opens again. The 51 curve, 81U and every other relay
  are unchanged; the switchgear sections' 50 still opens in the solve (no short films them).
- **What the lamps do:** a lamp beyond a bolted fault stays dark in those ticks, because the fault holds the line's voltage near zero; what
  is seen is the breaker's flag going red and back to green, the clamps' flash and the trip sound. The faults short's "lamps flash on"
  would need a fault with impedance (an arcing fault through a tree, say), which the grounding set is not.
- **Alternatives:** keep 50 in the solve and only hold the drawing closed (shows a state the solver never had); hold it longer than the
  real operate time.

### 8. The wells scene's gas well (client scene fix)
- **Chosen:** the scene draws its boiler's steam (90 a tick, a turbine's draw) for the second before it reads the wellhead.
- **Why:** since the 90 kW firebox rating (Tim's C3, kept), a firebox boiler fills its 400 steam in about 5 ticks; with nothing drawing
  steam the fire banks and the gas well's choke shuts, which is correct, so the old check ("flowing" after 10 s with no load) was wrong.
- **Alternatives:** a real steam turbine and load on the boiler (a longer scene).
