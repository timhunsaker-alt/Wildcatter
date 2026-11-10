# The PLC gap wave: decisions taken while Tim was away (2026-11-09)

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back ...
close all known gaps". This wave closes the "Not yet" items of Phase 13b to 13h in `docs/multiblocks/plc.md`, where they could be done
properly. The rest have a plan in `plc.md` ("The PLC gap wave", "Planned, not built") and stay in PROGRESS.md's Known gaps. Format as in
`docs/questions/README.md`.

### 1. What a control cable holds
- **Chosen:** a `control_cable` is a 50-pair multi-pair instrument cable: 100 cores, two of them its +24 V and 0 V commons, so 98 carry
  signals. A discrete signal takes one core and returns on the common. A 4-20 mA loop or a pulse input takes a twisted pair (two cores).
  - **Counting:** a cabinet counts the cores of every landed signal in each cable run on its way to the device, in the order the signals
    were landed. The landing that finds a cable full is refused: "CABLE FULL at x y z: its 98 signal cores are taken. Lay a second cable
    beside it, or put a remote drop out there".
  - **Parallel runs:** runs side by side between the same two places (cabinet, box group, device) add their cores.
  - **What does not count:** boxes that touch each other, or the cabinet, are one terminal rail with no cable between them.
  - **Shrinking capacity:** if a run is cut so the signals no longer fit, those past the cores carry nothing (inputs read off, outputs
    are not driven), and the cabinet's status names the full cable.
- **Why:** 50-pair is the largest common size of overall-screened instrument cable. Counting cores, not pairs, for discrete signals is
  how multicore control cable is used, and analog loops are always run on twisted pairs.
  - **Tailings fits:** its playthrough lands 54 discrete signals on one trunk, which a 24-pair rule would have broken.
- **Alternatives:**
  - a smaller cable (12, 24 or 36 pair, the common home-run sizes), or several cable items of different sizes;
  - counting pairs for every signal.
  - **Note for Tailings:** a smaller cable would make its playthrough lay a second trunk or a remote drop.

### 2. The 24 V supply's 10 A, and what the 24 V feeds
- **Chosen:** the supply's limit is 10 A (240 W). Over it, the supply goes into hiccup mode: its output turns off for 1 s, then tries
  again. Every PLC on it sees no 24 V meanwhile, and the CPU makes its warm restart when power returns, as on any power loss.
  - **Readouts:** the supply shows its load ("8.35 A of 10 A") and counts overloads. A cabinet without 24 V names an overloaded supply.
  - **Several supplies:** supplies on one net share the load equally, as supplies in parallel do through a redundancy module.
- **The 24 V loads, all proposed:**

  | Load | Draw |
  |---|---|
  | Small CPU | 0.4 A |
  | Large CPU | 0.7 A |
  | Each I/O module's electronics | 0.05 A |
  | COMM module | 0.15 A |
  | A drop's adapter | 0.2 A |
  | A DI channel that is on | 7 mA (IEC 61131-2 type 1) |
  | A relay output that is on | 10 mA coil, plus the field device it powers |
  | An AI channel | its loop current (loop-powered transmitters) |
  | An AO channel | its output current |
  | LED pilot light | 0.02 A |
  | Stack light tier | 0.1 A |
  | 100 dB horn | 0.25 A |
  | On-off valve's solenoid pilot | 0.2 A (4.8 W) |

  - **Not on the 24 V:** devices with control power of their own, such as a starter's contactor coil, a breaker's close and trip coils,
    or a motor-operated valve's motor. The relay contact only switches them, as in a real panel.
  - **API:** a new default method `ControlDevice.outputAmps(point)` gives a device's draw. It returns 0 unless overridden, so Tailings'
    devices compile and draw nothing.
- **Why:** these are typical catalogue figures for the class of part. Hiccup mode is how DIN-rail supplies protect themselves. Counting
  only real 24 V field loads keeps a normal panel well inside 10 A, and makes a panel of horns and lamps the thing that overloads it, as
  in a real one.
- **Alternatives:**
  - constant-current limiting, with the 24 V sagging and inputs dropping out one by one;
  - a "power boost" of 150 % for a few seconds;
  - a larger 20 A supply item.
  - **Not done:** the supply's draw from the mains is still a fixed 100 W, whatever its load. The circuit would have to be rebuilt as the
    load changes. See Planned.

### 3. Two PLCs on one rack, and on one device
- **Chosen:**
  - **A rack has one owner,** as a real adapter takes one controller's exclusive-owner connection. The first PLC to reach a drop with
    a CPU and 24 V owns it.
    - Another PLC's landing on that rack is refused: "Rack 1 belongs to PLC-A: a rack has one owner". Its status names the rack's owner.
    - When the owner has not scanned the rack for 1 s (its connection timed out: a cut network, a dead PLC), the next PLC to reach it
      takes it.
  - **A device's output point landed on two PLCs by cable is wired to both,** and does what that does in the field:
    - two relay contacts on one coil are in parallel, so a discrete output is the OR of both PLCs in that scan;
    - two current sources in one 4-20 mA loop fight, so the loop reads as failed (0 mA). The device falls back as on a lost signal, and
      both cabinets show "TWO PLCs ON ONE LOOP: ...".
- **Why:** the first is the real rule for remote I/O. The second keeps the physics honest without refusing a wiring a player may want: two
  PLCs able to start one motor is a real, if unusual, design.
- **Alternatives:**
  - refusing any landing of a point another PLC already lands;
  - allowing a second "listen-only" connection to a rack, for inputs only (real adapters allow this).

### 4. Which relay flags a PLC reads
- **Chosen:** four new inputs on each switchgear section: TRIP 50, TRIP 51, TRIP 87B and TRIP 50BF. Each is the element that tripped it,
  from the relay's own record, as a multifunction relay's target flags. TRIP 50BF also marks the breaker that failed to open. They follow
  the existing points, so landings made before keep their numbers.
- **Why:** this is how a PLC or SCADA tells why a feeder is out without walking to it.
- **Alternatives:** one 4-20 mA or word "last trip code"; flags on the molded-case and generator breakers too (81U on the
  under-frequency shed), which would be the same few lines each.

### 5. How the modules are drawn in the cabinet
- **Chosen:** a block-entity renderer draws the seated rail through the door's window (the picture there before was a fixed decal).
  - **The rails:** two DIN rails.
  - **Top rail:** the CPU on the left, with its RUN lamp (green RUN, amber STOP, red FAULT, dark without 24 V) and the memory card when
    one is seated. A large CPU shows a second port.
  - **The modules:** one in each of the 12 slots, six to a rail. A module is a dark body with a label strip coloured by kind:

    | Kind | Label strip |
    |---|---|
    | DI | green |
    | DO | orange |
    | AI | blue |
    | AO | violet |
    | RTD | yellow |
    | HSC | cyan |
    | COMM | white |

  - **Empty slots** show the bare rail.
  - **The remote drop** is drawn the same way, with its adapter in the CPU's place.
  - **Materials:** the mod's existing ones; no new texture.
- **Why:** the spec asks for "the modules actually seated"; the label colours stand in for the colour bands real modules use.
- **Alternatives:** Ludo art for each module's front, drawn as item models in the slots, when the module icons get their own art
  (they still borrow `decal_relays`).

### 6. Ladder jumps, labels and RETURN
- **Chosen:**
  - **The elements:** a rung may carry a label, plus a jump coil (`>>LABEL`) and a RETURN coil (`<RETURN>`).
  - **Forward only:** a jump goes forward only. A jump back, or to its own rung, is refused with the rung named and the advice to use a
    loop in structured text.
  - **While a jump has power:** the rungs it passes over are not run. Their plain coils keep their last value, and their timers and blocks
    are not called, as the standard has it. A skipped rung's live view reads unpowered, not stale.
  - **Errors** name the unit and the rung: a label used twice, a jump to no label, a label that is not a name.
  - **PLCopen:** `<label>`, `<jump>` and `<return>`, both ways.
  - **Saved ladders:** old ones load unchanged.
- **Why:** a backward jump in one scan risks running into the watchdog, and many real tools restrict it.
- **Alternatives:** lowering a backward jump to a REPEAT loop.

### 7. A ladder or a function block diagram as a FUNCTION
- **Chosen:**
  - **The header:** `FUNCTION NAME : TYPE`. A coil (ladder) or an OUT variable (FBD) on the function's name sets its result.
  - **Callers:** structured text, ladder boxes and FBD blocks can all call one.
  - **Refused:** a function block inside a function, as IEC refuses it ("make F a FUNCTION_BLOCK"), and a chart as a FUNCTION.
  - **The editor:** a kind button cycles PROGRAM, FUNCTION_BLOCK and FUNCTION, with a result type field.
  - **PLCopen:** `pouType="function"` with its `returnType`.
- **Why:** this is the standard. A function has no instance to watch, so it has no live view.
- **Alternatives:** none worth keeping.

### 8. A coil on another unit's located output
- **Chosen:** the standard's ways, now proven in tests:
  - a direct address (`%QX0.1`) in any unit;
  - a `VAR_GLOBAL` with its `AT`, declared `VAR_EXTERNAL` in a ladder function block or a second ladder program.
  - **Another program's variable** (`MAIN.motor`) stays refused, as the standard has a program's variables its own. The error now says
    so and names the way: "MAIN is a program, whose variables are its own: declare MOTOR in VAR_GLOBAL (with its AT) and as VAR_EXTERNAL
    here".
- **Why:** IEC 61131-3 gives no access to a program instance's variables from another POU; vendors that allow it do so as an extension.
- **Alternatives:** allowing `PROGRAM.var` as a vendor extension.

### 9. FBD wires
- **Chosen:** each wire is routed orthogonally around the blocks.
  - **Search:** A* on a grid of the blocks' edges plus a margin, the pins' stubs and the gaps' middles. A bend costs 16 px.
  - **Caching:** routes are kept until the layout changes.
  - **Fallback:** a wire with no way round is drawn straight across, as before.
- **Why:** a wire drawn through a block hides what it connects. Every FBD editor routes around.
- **Alternatives:** a fixed-pitch grid (jogs on pins off the grid, slower).

### 10. Two small bugs the new scenes found
- **Chosen:** both fixed.
  - **A lamp or heater on its relay:** an open relay left it lit. It read full voltage across an open circuit. It now goes dark, so a
    PLC switching a lamp off shows it.
  - **A firebox's BURNING input:** it read true while the fire was held back by its firing rate (banked). It now reads true only while
    it burns.
- **Why:** both made a PLC's view of the plant wrong.
- **Alternatives:** none.

### 11. How a program proves a motor is running, and where FAIL_STOP comes from
- **Chosen:** no change to the starters.
  - **Proving a motor runs:** a starter's RUNNING input is its contactor's auxiliary contact, which closes on command whether or not the
    motor has power. MOTOR_CTRL's FAIL_START therefore needs a real proof. The `plc_motor_faults` scene uses the drive's speed (over 5 %),
    as real programs use a speed switch or a current relay. A program that wants FAIL_START must wire such a proof.
  - **Welded contactors:** nothing in play welds one, so FAIL_STOP comes only from a test hook (`Starter.setWeldedForTest`), as the
    switchgear's stuck mechanism does.
- **Why:** an auxiliary contact really is what RUNNING is, and a welded contactor really is how a motor fails to stop.
- **Alternatives:**
  - RUNNING as "contactor closed and current flowing" (a current-sensing relay built into every starter);
  - contactor wear (a weld after a number of starts under load, AC-3 / AC-4 duty).
- **Scene settings (on the pins, not mod figures):**
  - MOTOR_CTRL START_T and STOP_T of 5 s;
  - the on-off valve's TRAVEL_T of 10 s;
  - the motor-operated valve's TRAVEL_T of 45 s against its 30 s stroke.

### 12. Questions left for Tim (planned, not built)
Each has its plan in `plc.md`, "The PLC gap wave", "Planned, not built".
- **The HOA selector's position drawn on each device.** Today the readout says it. The plan is a `hoa` property drawn as a small
  keyswitch on each device's face.
  - **The question:** is a property on every controllable block (about 40 block classes, Tailings' too) wanted, or a client overlay drawn
    only while the control key is held?
- **The horn's own sound.** It borrows the note block's didgeridoo.
  - **The question:** a Ludo sound of a 100 dB industrial motor horn?
- **Module, key and card icons.** The modules still borrow `decal_relays` and the key borrows the trial key.
  - **The question:** Ludo art for nine modules and the key?
- **The NeoForge screen in a world.** It has not been opened by hand on the NeoForge jar. No client test harness exists for NeoForge.
  - **The question:** is a one-time manual check enough, or should a NeoForge client scene be built?
- **Ladder and FBD conditions in a chart, macro steps and a chart's safety check.** These need a design for how a transition names a
  rung or network.
  - **The question:** is IEC's "a transition is a ladder rung or FBD network" form wanted, or ST-only conditions kept, as most real
    charts use?
- **A chart reset and pause.** The plan is `<chart>.SFC_INIT` and `SFC_PAUSE` implicit inputs, as vendors add them.
  - **The question:** these are not in IEC 61131-3. Add them?
- **The memory card with retained values, and loading itself into a blank CPU at power-up.**
  - **The question:** should a card carry values, and should a blank CPU boot from it, as real ones do?
- **Boiler pressure and three-element drum control, a gas turbine's load setpoint, a steam turbine's own valve.** Each needs a
  process model the plant does not have yet (drum pressure, governor valve). These are larger than a gap.
- **Pipe pressure, vibration, dampers, CT and PT voltage on a cable, the soft starter's and MCC drive's speed reference.** These carry
  over from 13e and 13f, with plans in plc.md.
- **A crane's lift start and its hook load and boom readings.**
  - **The question:** may a PLC start a crane lift? A lift is normally a person's job, with a signaller.
