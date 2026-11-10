# PLCs: Phase 13 (decided by Tim 2026-11-07; figures still proposed)

Tim, 2026-11-07: PLCs should run "every type of thing you would run off of them", with the programming "as close to real life as possible". In-game names carry no brand.

**Sources:**
- IEC 61131-3 ed. 3 (2013): languages, data types, standard function blocks, tasks.
- IEC 61131-2: 24 V DC input thresholds, output ratings.
- NAMUR NE 43: 4-20 mA signal failure levels.
- ISA-5.1 (instrument tags), ISA-18.2 (alarm management), ISA-88 (sequence and batch structure), ISA-101 (HMI screens).
- IEC 61511: why safety functions stay separate from process control.

A figure marked *(proposed)* is a textbook value or a proposal, for Tim to approve.

## What exists today (survey, 2026-11-07)
- No control wiring, low-voltage control tier, 4-20 mA signal, instrument air or valve block in either mod.
- Redstone input on only four blocks (starter, MCC section, starter cabinet, genset panel), redstone output on one (the transfer switch), none in Tailings.
- Most machines report through `inspect()`, which is text: a transmitter needs a numeric accessor added on each.
- Only Tailings' drill has a real screen, and it is buttons. There is no text editor in either mod.
- Tailings' `mine_switch` has no way to be thrown, despite haulage.md, and its `dewatering_pump` is orphaned. PLC work on the track has to fix the first.
- 43 `PlantUnitBlockEntity` subclasses have not been reviewed one by one; each wave that reaches them does so.

## The rule that governs everything: protection stays separate
As in a real plant, the PLC is process control, not protection (IEC 61511):
- A PLC can **read** every trip and alarm, can **command** a stop or a breaker open, and can **add** trips of its own.
- A PLC can **never block, delay or reset around**: overloads, breaker and fuse curves, protective relays (50/51/81U, 87B/86B/51V), generator and turbine trips, sync-check damage, `ReactorProtection.trips`, the RCP seal permit, the traction DC trip, or one-train-a-section.
- A tripped device needs its hand reset where it has one today.
- A dead PLC is a stopped plant, not a dangerous one: outputs drop out, starters stop, valves go to their fail position.

## Hand-Off-Auto on everything
Every controllable device gains a real HAND / OFF / AUTO selector, or LOCAL / REMOTE on switchgear:
- **HAND:** today's clicks and today's automatic logic, unchanged. A world with no PLC plays exactly as now.
- **AUTO:** the PLC commands it. It reports its selector back as an input.
- A new or existing device starts in HAND, so old worlds are untouched.

## Hardware
| Part | Size | Real-world basis |
|---|---|---|
| `control_power_supply` | 1 x 1 x 1 | 120/230 V AC in from a receptacle circuit, **24 V DC, 10 A out** *(proposed; standard DIN-rail supply)*. Reads the world's standard for its input. Dead supply means a dead PLC |
| `plc_cabinet` | 1 x 2 x 1 | A floor-standing enclosure, about 0.8 x 2 x 0.6 m. A DIN rail with a CPU and **12 module slots** *(proposed)*, keyswitch and status lights on the door, drawn with the modules actually seated |
| `field_junction_box` | 1 x 1 x 1 | A wall box of terminals where a multi-core cable fans out to field devices |
| `control_cable` | run of blocks | Multi-core instrument cable, see DECISION 1 |
| `network_cable` | run of blocks | Industrial Ethernet between cabinets, remote drops and HMIs |
| `hmi_panel` | 1 x 1 x 1 | A door-mounted touch panel, see HMI |
| `remote_io_drop` | 1 x 2 x 1 | A cabinet with a communications adapter and no CPU, as a real remote I/O rack |

**Modules, items seated in a cabinet slot:**
| Module | Real-world basis |
|---|---|
| CPU, small / large | Small: 512 I/O points. Large: 4,096, and more memory *(proposed)* |
| DI16 | 16 digital inputs, 24 V DC, on at 15 V and off at 5 V (IEC 61131-2 type 1) |
| DO16 relay | 16 relay outputs, 2 A *(proposed)* |
| AI8 | 8 analog inputs, 4-20 mA, raw 0-32000 *(proposed scale)*. Reads an open loop: below 3.6 mA or above 21 mA is a channel fault (NE 43) |
| AO4 | 4 analog outputs, 4-20 mA |
| RTD4 | 4 temperature inputs, direct in tenths of a degree |
| HSC2 | 2 high-speed counters, for encoders and belt scales |
| COMM | Network port: remote I/O, PLC-to-PLC, HMI |

**Signals are not put through the circuit solver.** The survey found wired control nodes would join the group's solve (budget 1,000 nodes). So control wiring is its own graph, in plain Java, walked when it changes: continuity, the 24 V source and each point's state. It stays physically honest in what the player sees: cut a cable and inputs read off, analog channels read a channel fault, and outputs lose their devices. It does not model control-circuit shorts, ground faults or voltage drop *(a Known gap, stated)*.

**Timing:** a scan is one game tick, **50 ms**, slower than a real CPU's 1-10 ms. This is the one place the game forces a limit. Tasks run every 1, 2, 5, 10 ... scans, so a fast real task cannot be matched. Electrical readings refresh at the solver's 5 Hz, as an instrument's update rate.

## Field devices
- **Operator:** pushbuttons, selector switches, e-stop (mushroom head, latches, wired as a normally-closed chain), pilot lights, stack lights, horn.
- **Discrete sensors:** limit switch, proximity, level switch, pressure switch, flow switch, zero-speed switch, belt rip and misalignment switches, pull cord.
- **Transmitters (4-20 mA, ranged by the player, tagged per ISA-5.1, e.g. LT-101):**
  - level, pressure, differential pressure, flow, temperature;
  - speed, vibration, position;
  - voltage, current, power, frequency, power factor, as transducers on a CT and PT;
  - belt weigh scale, density gauge, pH, on-stream assay analyser for slurry.
- **Actuators:**
  - on-off valve, control valve with positioner, damper, see DECISION 2;
  - solenoid, sounder, and any starter, breaker or drive below.

Transmitters mount by clamping to a tank, pipe, cable or machine. Each one needs a numeric accessor on what it reads.

## What runs off them
Each row is a wave's checklist. **Start/stop and status** means the run command, running feedback, fault and HOA selector.

**Wildcatter**
| Equipment | PLC commands | PLC reads |
|---|---|---|
| Starters, MCC buckets, soft starters | Start/stop | Running, overload trip, amps, HOA |
| VFDs | Start/stop, **speed reference 4-20 mA** (new: today's drive has a fixed ramp and no speed input), ramp time | Output frequency, amps, fault |
| Water, injection, charge pumps, pumpjacks, compressors | Start/stop, speed where on a drive | Level, pressure, flow via transmitters |
| New valves | Open/close, position 0-100 % | Position, limit switches |
| Breakers, switchgear, load interrupters | Close and trip coil, with remote/local | Closed, tripped, amps, relay flags |
| ATS | Test, inhibit transfer | Stage, source available |
| Gensets | Start/stop, speed and voltage trim, close permissive, so a PLC can do load sharing and load shedding | Rpm, Hz, kW, share, fuel, slip angle, trips |
| Boilers, steam and gas turbines, HRSG | Firing rate, drum level (three-element), valve position, start sequence, load setpoint | Speed, MW, level, pressure, temperatures, trips |
| Reactor | Rod bank and boron demand, RCP start/stop, feedwater, setpoints. **Never the trips** | Power, Tavg, pressure, ppm, SG levels, rods, xenon |
| Refinery, gas plant, cracker, coker, acid plant, uranium, zirconium, fuel chain | Unit run enable, feed valves, sequences | Levels, flows, temperatures, unit status |
| Rectifiers, potline, cells | Current setpoint, start/stop | Amps, volts, EMF, ground fault |
| Rig, lease, wellfield | Pump and drawworks enable, well choke | Depth, flow, pressure |
| Cranes | Run enable, lift start | Status, hook load, boom |
| Lights, load banks, heaters | On/off, steps | Watts |

**Tailings**
| Equipment | PLC commands | PLC reads |
|---|---|---|
| Crushers, mills, screens, feeders, belts | Start/stop, conveyor start-up sequencing, feeder rate | Running, amps, fill, weigh scale, pull cord |
| Flotation, leach, thickener, filter, cells | Run enable, reagent dosing rate, air | Levels, assay, status |
| Water and slurry pumps, lined cell, decant | Start/stop, speed | Level, flow, cell state |
| Ore sorter | Routing by program, replacing clicks | Pocket contents |
| TBM | Go, stop, target, drill program | Advance, floor, cable left, tank, status |
| Train signals, switches, locomotives | Throw switch, hold or release at a signal, dispatch. **Never one-train-a-section** | Occupancy, speed, load |

## Programming: IEC 61131-3
**All four languages a current standard keeps, in game, mixed in one project and calling each other:**
- **Ladder (LD):** rungs, contacts, coils, branches, drawn graphically.
- **Function block diagram (FBD):** wired blocks, graphically.
- **Structured text (ST):** a real text editor with the standard's syntax.
- **Sequential function chart (SFC):** steps, transitions, actions, parallel branches.
- Instruction list is left out: IEC deprecated it in the 2013 edition.

**The rest of the standard:**
- **Program units:** PROGRAM, FUNCTION_BLOCK, FUNCTION, with instances.
- **Data:** BOOL, INT, DINT, UINT, REAL, LREAL, TIME, STRING, arrays, structs, enums, RETAIN variables.
- **Addressing:** direct (`%IX0.3`, `%QX1.0`, `%IW4`, `%QW2`, `%MW10`) and symbolic tags.
- **Standard blocks:** TON, TOF, TP, CTU, CTD, CTUD, R_TRIG, F_TRIG, SR, RS, and the standard math, compare, select and conversion functions.
- **Tasks:** cyclic with priorities, plus a first-scan flag.
- **Beyond the standard, as every real vendor adds:**
  - PID with auto/manual, bumpless transfer, anti-windup and setpoint ramp;
  - scaling, alarm and totalizer blocks;
  - motor and valve device blocks with failure-to-start timers.

**The CPU behaves as a real one:**
- The scan: read inputs into the image, run tasks, write outputs, house-keep. A program never sees an input change mid-scan.
- RUN / STOP / REMOTE on a keyswitch. STOP drops outputs to their configured state.
- **Watchdog:** a scan over its instruction budget faults the CPU, which stops, sets its fault light and logs it. This is also what protects the server.
- A diagnostic buffer, naming the fault, its unit and line.
- Online work: live power flow on rungs and live values, forcing of I/O with a lit FORCE warning, downloading, uploading, online edits, retained values across power cycles.
- Project on a memory card item, so it can be copied to other cabinets.
- Import and export as text: ST source, and PLCopen XML (IEC 61131-10) for the graphical languages, so a project can be written outside the game.

**Under the hood:** a compiler to a bounded bytecode, run by an interpreter in `dev.wildcatter.plc` in plain Java with no Minecraft imports, as the solver is. Player programs never become JVM code, and each has an instruction limit, so a multiplayer server is safe from them. Unit tests can run real programs without Minecraft.

## HMI and networks
- **HMI panel:** screens the player lays out from tags: readouts, buttons, bar graphs, valve and motor symbols, in ISA-101 style.
- **Alarms:** ISA-18.2 states (active, acknowledged, shelved), priorities, a history.
- **Trends:** tags charted over time, stored in the HMI.
- **Networks:** remote I/O drops, PLC-to-PLC tag exchange, an HMI reading any PLC on its network. Modbus-style register addressing *(proposed)*.
- **A SCADA room:** a desk of several HMIs is the same parts, with nothing new.

## Waves
Each wave ships as usual: its spec section, models on a contact sheet, code, tests, docs, Field Manual page, and film.

| Wave | Contents | Proves it |
|---|---|---|
| 13a | Language core: ST compiler, bytecode, scan, tasks, standard blocks, PID | Unit tests running real programs, no Minecraft |
| 13b | Cabinet, supply, modules, control cable, junction box, operator devices, HOA on starters. Text editor | A pushbutton starting a real motor from an ST program, a cut cable stopping it |
| 13c | Ladder and FBD editors, online monitoring, forcing | The same program in ladder, with live power flow |
| 13d | SFC, device blocks, memory card, import and export | A conveyor start-up sequence |
| 13e | Transmitters, AI/AO, VFD speed reference, valves | A PID holding a tank level on a real drive-fed pump |
| 13f | Power equipment: breakers, gensets, turbines, boilers, rectifiers. HMI, alarms, trends | A PLC starting a set, closing it and shedding load, on an HMI |
| 13g | Process plants: the 43 plant units, reactor control | The reactor's rods and boron run by a PLC, its trips still beating it |
| 13h | Networks, remote I/O, PLC-to-PLC | A remote drop across a site |
| 13i | Wildcatter release; Tailings: every machine, track, TBM. Full client suite | Tailings' playthrough run by a PLC, nothing force-run |

Tailings compiles against a Wildcatter release, so it comes after 13h's release. Its ROADMAP line limiting PLCs to four things, with "Wildcatter's controls stay fixed hardware", is replaced by this.

## Known gaps, stated now
- A 50 ms scan, not 1-10 ms.
- Control wiring does not model shorts, earth faults or voltage drop.
- No real vendor's dialect or file format: the standard, and PLCopen XML.
- No real network traffic, timing or cyber-security model.
- Safety PLCs are left out: protection stays in the existing fixed relays and trips.

## Decided by Tim, 2026-11-07
1. **Wiring:** multi-core control cable, fanning out at junction boxes, with each signal landed on a module channel in a terminal screen.
2. **Valves:** both. Air-driven control valves (compressor, dryer, air tubing, fail open or closed on lost air), and electric motor-operated valves for isolation (stay put on lost power).
3. **Role:** optional, with real gains. No machine needs a PLC and old worlds play on, while a PLC gives what built-in logic cannot: true speed control, held levels and pressures, load shedding, a whole site on one HMI.
4. **Scope:** all nine waves, in this order, including all four languages and PLCopen XML.

## As built: 13a (2026-11-07)
`dev.wildcatter.plc` and `dev.wildcatter.plc.lang`, plain Java with no Minecraft imports. Structured text compiles to a tree (`Ast`) and then to bytecode, and the graphical languages will be drawn down to the same tree. The standard blocks are themselves structured text (`StdLib`), so they cost instructions as a real CPU's do.
- **Built:** PROGRAM, FUNCTION_BLOCK, FUNCTION; every statement; structs, enums, multi-dimension arrays, typed and based literals; direct addresses and AT; RETAIN and CONSTANT; VAR_IN_OUT, VAR_TEMP, VAR_EXTERNAL; CONFIGURATION, RESOURCE, TASK; TON, TOF, TP, CTU, CTD, CTUD, R_TRIG, F_TRIG, SR, RS, PID, SCALE, ALARM_HL, TOTALIZER; the scan, tasks, watchdog, diagnostic buffer, forcing, keyswitch, power loss, warm and cold restart, saving.
- **Stricter than many IDEs, as the standard asks:** no implicit narrowing, and `%MW` is a WORD, so an INT needs `INT_TO_WORD`.
- **Measured:** a large CPU's whole budget of 100,000 instructions takes about 0.35 ms on this Mac, so the proposed budgets hold.
- **A fault:** integer and real division by zero, and an array index out of bounds, stop the CPU and name the POU and line. MOD 0 gives 0, as the standard says.
- **Left out:** date types (DATE, TOD, DT), ULINT, the 2013 edition's object-oriented methods and interfaces, and passing variables to a program instance in the configuration. A function block in an array cannot use `=>`: read its output after the call, as `fbs[i].Q`.
- **Errors carry a line, not yet a file:** a project is compiled from one source for now; 13b's editor brings POUs in their own files.

## How 13b plays *(proposed where marked)*
- **Slots and addresses, by position, as a real rack's:** the cabinet's CPU sits in its own slot, and 12 module slots follow. Slot *n* (from 0) owns 16 bytes from `n x 16` *(proposed)*, so channel 3 of a DI16 in slot 1 is `%IX16.3`. An input and an output module each read and write their own area.
- **Seating a module:** click the cabinet holding it. Sneak with an empty hand pulls the last one seated.
- **Wiring:** `control_cable` joins the cabinet, junction boxes, the supply and devices. A cable makes one run, so **a cable cell touching more than two things is a bad joint**: the signal stops there and the cabinet names the block. Runs fan out at a junction box, which takes any number.
- **Terminals:** click the cabinet with an empty hand, and the screen's TERMINALS tab lists every device its cables reach, each with its signals. Choosing a channel lands a signal. A device's tag, such as `PB-101`, is set there too.
- **The HAND / OFF / AUTO selector:** the new `control_key` item, clicked on a device, steps its selector. A keyed selector is a real fitting, and the key does not take over any click a device already has. In OFF or AUTO a device's buttons and redstone do nothing, except a trip's hand reset. Every device starts in HAND.
- **Losing the PLC:** a device in AUTO stops if it has heard no command for half a second *(proposed)*. A cut cable, a dead supply, STOP, a CPU fault and an unloaded cabinet all look the same to it.
- **Starters:** every direct, soft and drive starter, in its own box, an MCC bucket or a cabinet, has four signals: RUN (output), RUNNING, TRIPPED, AUTO (inputs).
- **Operator devices, wall-mounted, 24 V:** pushbutton (momentary, on for half a second a click), selector (maintained), e-stop (latched, **normally closed**, so a cut wire reads as a press), pilot light, stack light (red, amber, green), horn.
- **The supply** draws **100 W** *(proposed)* from a 120 V circuit (or the world's standard). Below 85 % voltage *(proposed)* it gives no 24 V. Its 10 A rating is not enforced yet.
- **The editor:** line numbers, syntax colours, selection, copy and paste, undo, and a compile that lists errors by line and jumps to them. CHECK compiles without touching the CPU. DOWNLOAD compiles on the server and replaces the project, leaving the CPU in STOP, as a real download does. A DIAGNOSTICS tab shows the fault buffer, and an ONLINE tab reads and writes tags and forces I/O.

## As built: 13b (2026-11-07)
`dev.wildcatter.control`, `client/PlcScreen`, and the `plc` game scene, on the Fabric client.
- **Proved in the world:** a pushbutton starts a pumpjack's real 480 V motor through a real program. In AUTO the starter's own button does nothing, the e-stop drops it and refuses a start, a cut cable stops it, a T in a cable is named as a bad joint, a runaway program faults and drops it, a save and load keeps the project and the latch, and losing 24 V stops it. The repeated direct-on-line starts tripped the starter's real overload at 67.5 A inrush on the way, and the hand reset worked in AUTO.
- **Not yet:**
  - ~~the modules seated are not drawn in the cabinet's window~~ *(closed in the gap wave, below)*;
  - ~~a cable's core count: any number of signals may share a run~~ *(closed in the gap wave, below)*;
  - ~~the supply's 10 A rating is not enforced~~ *(closed in the gap wave, below)*;
  - `control_key` steps a selector with no drawn switch position on the device: its readout shows it;
  - ~~a project is one source, so errors carry a line and no POU~~ (closed in 13c: each unit is its own source, and errors name it);
  - the horn borrows a vanilla note block sound;
  - module and key icons borrow existing art, and the recipes are proposed;
  - the NeoForge jar compiles with its packet registered, but has not opened this screen in a world.

## As built: 13c (2026-11-07)
`plc.lang.Ladder`, `Fbd`, `BlockLibrary`, `Project`; `client/LadderPane`, `FbdPane`. A project is now program units in any language, calling each other.
- **Ladder:** rungs of contacts and boxes in series and parallel, driving coils, negated coils, set and reset. Function blocks take the rung's power at their first BOOL input and give it out of their first BOOL output; comparison boxes pass power when true; other operators and functions run with power and pass it on.
- **FBD:** blocks wired output to input, with variables, constants and expressions on pins. Execution follows the wiring whatever the layout, and a block's number shows its place. A loop reads last scan's value. A function's result takes its type from what it computes.
- **Live view:** every ladder element and function result has its own variable, so what is drawn is what the CPU computed, not a second evaluation. It goes live only while the editor matches the CPU's project. A function block unit is watched by giving it an instance path in its `monitor` field.
- **Online change:** the project is replaced with the CPU running; variables of the same name and type keep their values, and nothing misses a scan.
- **Large projects:** a download goes in pieces, as a client's packet holds 32 KB, up to 4 million characters *(proposed)*. A 150 KB project is tested.
- **Proved in the world:** the same motor from a ladder drawn with the real toolbar and mouse: running from the button, the live view showing the seal branch powered and the start branch not, an online change without a stop, an FBD beside it lighting the stack's green lamp after its timer, a force from the ONLINE tab starting the motor with its light on, and a release.
- **Not yet:** ~~ladder jumps and labels; a function or a ladder as a FUNCTION (a ladder or FBD is a PROGRAM or FUNCTION_BLOCK); a ladder's `Coil` on a located output of another unit; FBD wires routed around blocks, as they are drawn straight through~~ (all four closed in the gap wave, below).

- **A regression, fixed:** running the asset generator for 13b deleted Phase 12's lined cell and process water pump assets, and 13b's commits carried that. No release had it. They are back, and `art/hand_assets` and a check at the start of the client suite keep it from recurring.

## How 13d plays *(proposed where marked)*
- **Sequential function chart:** a fourth language beside the others, a PROGRAM or FUNCTION_BLOCK.
  - **Steps and transitions** are drawn on a sheet and wired pin to pin. Several transitions leaving one step are a choice, taken left to right. A transition leaving or entering several steps is a parallel branch, drawn with the double bar.
  - **The scan, as the standard asks:** all transitions are read against the steps active at the start, the ones that clear all fire together, then the actions run. A token moves one step a scan at most.
  - **Each step has its flags,** `S1.X` (active) and `S1.T` (time active), readable anywhere in the unit.
  - **Actions** take the standard's qualifiers: N, R, S, L, D, P, P1, P0, SD, DS, SL, through the standard's `ACTION_CONTROL` block. An action is a BOOL variable it drives, or a named body of structured text that runs while active and once more as it ends, so it can tidy up by reading its own `.Q`.
  - **Refused:** no initial step, two in one chart, a transition with nothing before or after it or no condition, a timed qualifier with no time, one action given two times.
- **Device blocks, beyond the standard as real vendors' are:**
  - `MOTOR_CTRL`: START demand, PERMIT, RUNNING, TRIPPED and AUTO feedback, RESET; RUN out; FAIL_START, FAIL_STOP and FAIL_RUN latch FAULT until RESET. **15 s** to see RUNNING and to see it drop *(proposed, both set on pins)*. Outside AUTO it raises no alarm and asks for nothing.
  - `VALVE_CTRL`: OPEN demand, PERMIT, OPENED and CLOSED limits, AUTO, RESET; OUT for a spring-return solenoid held open, CMD_OPEN and CMD_CLOSE for a motor-operated valve, driven to the limit; FAIL_OPEN, FAIL_CLOSE and BAD_LIMIT (both limits made at once). **30 s** of travel *(proposed, on a pin)*. A fault drops OUT, so an air valve goes to its fail position, and stops both drive commands, so a motor-operated valve stays put. 13e brings the valves themselves.
  - These are process control only. Neither block can hold off a trip.
- **Memory card:** the new `memory_card` item holds a project, up to **256,000 characters** *(proposed: real cards hold far more; this bound is what an item's data can carry to every player who sees it)*.
  - Click a cabinet with a card to seat it in the door. Sneak with an empty hand pulls the card before any module.
  - The new PROJECT tab has TO CARD and FROM CARD. FROM CARD is a download: it compiles first, and leaves the CPU in STOP.
  - A card holds the project, not the terminals or the values, so it copies a program and not a site's wiring.
- **Import and export, from the PROJECT tab, on the player's own machine:** files live in `<game folder>/wildcatter/plc/`.
  - **EXPORT XML** writes the whole project as PLCopen XML (TC6 v2.01, the IEC 61131-10 form). **EXPORT ST** writes the structured text units as one `.st` source, and names any graphical unit it left out.
  - **IMPORT** reads the file picked from the list into the editor. Units of the same name are replaced and others stay; nothing reaches the CPU until a download. A file it cannot represent is refused whole, with the reasons, rather than dropping part of a program.
- **The proof:** a conveyor start-up sequence on three real starters, as a real plant starts belts: horn and amber light first, then the last belt in the line, each next belt once the one after it is running, and a stop in the reverse order. A starter that fails stops the belts feeding it. Three pumpjack motors stand in for belts here, as Wildcatter has none; Tailings' real belts come with 13i.

## As built: 13d (2026-11-07)
`plc.lang.Sfc`, `PlcOpen`; `client/SfcPane`; `control/MemoryCardItem`; the PROJECT tab; `ACTION_CONTROL`, `MOTOR_CTRL`, `VALVE_CTRL`; and the `plcSfc` game scene.
- **Proved in the world, with a chart drawn with the real toolbar and mouse:** from the start button, horn and amber together in a parallel branch for 3 s with nothing moving; then three real 480 V starters and motors in order, last belt first, 2 s apart; green with the line up; a stop in reverse order; the jump back to the start; a belt's feedback forced off, so `MOTOR_CTRL` latches FAIL_RUN and the chart stops the rest and shows red; a selector out of AUTO raising no fault.
- **Also in the world:** a memory card to a second cabinet through a save and load, blank and missing cards refused; a real `belts.xml` written, the chart deleted, imported back as a chart, downloaded, and the same sequence run from it; an unsafe file name refused. FBD wires, a drag and undo by real mouse, which 13c left untested.
- **PLCopen:** every exported test project validates against PLCopen's own TC6 v2.01 schema, when `PLCOPEN_XSD` points to a copy, as that schema is PLCopen's and is not in this repository. CI skips that check. A file laid out as another tool might (shared rails, a named transition, an inline action) is tested, but this was hand-written: **no real vendor's export has been tried.**
- **A second network in a chart** is allowed, as the standard does: the scene's device blocks run from an always-active step.
- **Found and fixed:** a cabinet holding a unit over about 65 KB could not open its screen, as a string in a packet is limited, since 13c allowed such projects. A project now reaches a screen in pieces, and the `plc` scene reopens a 150 KB one.
- **Not yet:**
  - conditions and action bodies are structured text only, not ladder or FBD; a chart can call a ladder function block from a body;
  - a chart's safety (a token stranded or doubled by odd wiring) is not checked, nor are macro steps;
  - no SFC-wide reset or pause: a sequence stops through its own transitions;
  - ~~`VALVE_CTRL` has unit tests only; of the motor block's faults, only FAIL_RUN has run in the world~~: VALVE_CTRL on steel-pipe valves and MOTOR_CTRL's FAIL_START and FAIL_STOP now run in the world *(closed in the gap wave, below)*;
  - the card holds no retained values and does not load itself into a blank CPU;
  - comments in variable blocks are lost through XML (a body's stay), and imported STRING lengths are ignored, as the CPU ignores them;
  - import replaces a unit of the same name with no undo, so unsaved edits to that unit are lost;
  - a jump's wire can only be removed by clicking its target's head pin, which takes all wires into that step;
  - `memory_card`'s recipe is proposed (its icon is Ludo art since the 2026-11-09 art wave).

## As built: 13e (2026-11-07)
`control/ValveBlock`, `TransmitterBlock`, `AirCompressorBlock`, `AirDryerBlock`, `Measurable`; `block/DayTankBlock`; analog points on `ControlDevice`; `DRIVE_POINTS` on `Starter`; and the `plcAnalog` game scene.
- **Proved in the world:** a PID in a real program, reading a real tank through a transmitter and an AI8, writing a real drive's speed reference through an AO4, on a 37 kW pump:
  - at setpoint 50 % the tank held at 47.5 %, the drive at 32 % speed drawing 1.2 kW of a 37 kW motor, as the cube law gives;
  - with a bigger draw it held 49.4 % by speeding up to 35 %;
  - at setpoint 70 % it held 70.1 %.
- **Also in the world:** a transmitter removed reads 32767 and the program stops the pump, and restarts it when it is back; air cut and a control valve fails shut and returns; power cut and a motor-operated valve stays put while `VALVE_CTRL` latches FAIL_CLOSE, then closes after a reset; a CPU in STOP stops the drive and shuts the air valve; flow totalled on an HSC2; a transmitter's range set through the real screen.
- **Regression:** `drives`, `motorStarter`, `tailingsCell`, `lease`, `landRig` and `plcSfc` all pass against the changed motor.
- **New figures, all proposed:** a drive's minimum speed 20 %; a pump's static head at 30 % speed; a valve's stroke of 5 s (control), 3 s (on-off), 30 s (motor-operated); a 370 W actuator; a 7.5 kW compressor cutting in at 6 bar and out at 8, 0.5 bar/s to charge and 0.01 bar/s of leakage; air fails under 3 bar; a 1,000 L day tank whose outlet passes 100 L/s full open and full.
- **Not yet:**
  - **dampers are not built:** nothing has a duct to put one on, so they wait for boiler firing in 13f;
  - **vibration has no transmitter:** nothing in the mod models it, and a made-up number would be worse than none;
  - **pressure** is read only on an air compressor so far: water and product pipes have no pressure model at all, so a pipe has no pressure to transmit;
  - temperature, belt scale, density, pH and assay have their kinds but no machine gives them yet: 13g and 13i bring them, and `RTD4` is untested until then;
  - transmitters read tanks, boilers, pumps, drives, starters, valves and the compressor. Cables, and the CT and PT transducers, are not done: 13f;
  - a throttled valve caps the whole run it is on, as pipes have no flow model: parallel branches are not told apart;
  - a soft starter or an MCC bucket's drive has no speed reference signals: only the drive cabinet and lineup control section has them, and the MCC's drive buckets wait for its point numbering to change;
  - ~~valves on steel pipe have not run in the world~~: an on-off and a motor-operated steel valve feed two units under a PLC *(closed in the gap wave, below)*;
  - a valve's air use per stroke is not modelled, only leakage.

## As built: 13f (2026-11-07)
`control/Remote`, `HmiPanelBlock`, `NetworkNet`, `client/HmiScreen`; signals on breakers, switchgear, the transfer switch, gensets, the steam plant's turbine generator, gas turbines, fireboxes, rectifiers, load banks and lamps and heaters; `network_cable`, `comm_module`; and the `plcPower` game scene.
- **Proved in the world:** two real 500 kW diesel sets, a real program and a real HMI laid out through its editor. The HMI's button starts them; the first closes onto the dead bus, and the second is held by its permit until the first is closed, then closes in sync at 1 degree. The PLC closes feeders one by one; the third takes the load past its 100 kW limit, and after 3 s it opens that feeder and holds it off, at 89 kW. A trim moves 29 kW from one set to the other. The shed raises an ISA-18.2 alarm, acknowledged and shelved by real clicks, with its history and a trend. A tripped breaker refuses its close coil. The button stops both sets.
- **Found:** control cables could not reach a true-size part that is not a `BigPartBlock` by class, such as the genset panel. Fixed in `ControlNet`.
- **Figures, all proposed:** trims of 2 % (speed) and 5 % (voltage); kW read on 125 % of rating; a shelf of 10 minutes; 100 history rows; a sample a second kept 10 minutes; a firebox's burner modulating by time over 10 s.
- **A stated shortcut:** a set's share of the load moves with its source voltage here, as its sag is a resistance standing in for droop. In a real set speed moves kW and voltage moves VARs. Both trims shift load in this model.
- **Not yet:**
  - ~~only breakers, gensets and the HMI have run in the world under a PLC~~: the transfer switch, switchgear, steam and gas turbines, fireboxes, rectifiers, load banks and lamps now run under a real PLC in server scenes *(closed in the gap wave, below)*;
  - boiler pressure and three-element drum control: boilers report only their level, and the steam plant has no setpoint to take; the HRSG only follows its gas turbine;
  - a gas turbine's load setpoint and a steam turbine's own valve: both still follow the grid;
  - dampers, still, as nothing has a duct;
  - CT and PT transducers are a transmitter on a cable, reading amps only; a cable has no voltage reading yet;
  - ~~relay flags (50/51, 87B) are read only as TRIPPED~~: the switchgear has TRIP 50, 51, 87B and 50BF *(closed in the gap wave, below)*;
  - the HMI's symbols use a device block's STATE; there are no piping graphics, and HMI screens are one page.

## As built: 13g (2026-11-07)
A selector and a run enable on every plant unit, in `PlantUnitBlockEntity`; rod demand, boron target, power setpoint and a manual trip on the reactor; the coolant pumps' start; the rig's drill enable and the crane's run enable.
- **Proved in the world, in the `reactor` scene:** a real program, at 2 % power on a real reactor. Its demand starts where the rods are. Bank D went from 104 to 86 steps on the PLC's demand, power falling to 1.08 %, and the boron target to the PLC's 1,450 ppm. An RTD input read Tavg in tenths. **With the PLC demanding every rod out, a power excursion tripped it on LOW RCS FLOW and dropped them all,** its demand still standing; a manual trip from the PLC worked; only the console's reset cleared a trip; out of AUTO the reactor's own control took the rods back.
- **Also, in the `toppingUnit` scene:** a real column enabled by a real program; off, it holds and says so and tells its PLC; on, it goes on; a cut cable holds it.
- **The review of the 43 units:** 25 have a tick of their own and take the gate at its top. Motor-driven units take it in `UnitMotorBlockEntity.drive`, where 51 subclasses meet. **Left out on purpose:** the reactor vessel (its protection lives in its tick), the natural draft tower and the frequency converter (a heat sink and a grid tie, not a process run). The units with no tick are parts their controller runs, so they stop with it.
- **Not yet:**
  - ~~only one process unit has run under a PLC in the world~~: the nitric acid unit (motor-driven) and the AN neutraliser (its own tick) now do too *(closed in the gap wave, below)*;
  - ~~the rig, crane and coolant pump signals compile and have not been exercised~~: each runs under a real PLC *(closed in the gap wave, below)*;
  - **a level is the fullest thing a unit holds;** a unit's own flows and temperatures have no reading yet;
  - **feedwater** is not on a signal, nor a well's choke beyond a valve in its line: the valve is real, the scene has not run one;
  - the crane has no lift-start signal, only a hold; its load and boom are not read.

## As built: 13h (2026-11-07)
`control/RemoteIoDropBlock`, tag exchange and a NET tab on the cabinet, stations on `NetworkNet`, and the `plcNetwork` game scene.
- **Proved in the world:** a button and a starter on a drop 60 blocks from its PLC, across a real network cable; a second PLC taking two tags by rows entered in its real NET tab, lighting its own lamp from one; an HMI's network seeing both, reading by station, and writing a marker as a register; a cut network stopping the remote starter and showing the exchange as FAILED with its last values held; a save and load keeping the rows and name.
- **Addressing:** rack 1's slot 0 is slot 12, so `%IX192.0`, and each rack takes 12 more *(proposed)*. A small CPU's 512 bytes reach rack 1 and part of rack 2; a large CPU's 4,096 reach all four.
- **Not yet:**
  - ~~nothing stops two PLCs landing signals on one rack~~: a rack has one owner, and a point wired to two PLCs does what it would in the field *(closed in the gap wave, below)*;
  - no traffic, timing, or lost-packet model, as the spec says;
  - the exchange copies each row every half second, one way; a row's types must match or it shows FAILED;
  - ~~a drop's modules are not drawn, as a cabinet's are not~~ *(closed in the gap wave, below)*.

## As built: 13i (2026-11-07)
Wildcatter v0.5.0, holding everything above, and Tailings on it.
- **Found by the full suite:** since 13c, a world could not be saved with a program unit over 64 KB in a cabinet: the unit went to disk as one string, and a save writes no string past 65,535 bytes. Units are now saved in pieces of 20,000 characters, as they already went to the screen; a world saved before loads them the old way. The `plc` scene now writes a cabinet holding a 150 KB unit to bytes, as a save does.
- **Found by Tailings' playthrough (v0.5.1):** a cabinet's walk along its cables counted the blocks beside each cable toward its limit of 8,192, so a trunk stopped after about 1,600 cables. Only cable and boxes count now: about 8 km of cable.
- **Tailings:** its plant machines, mine switches, signals and the TBM's cable junction are devices on Wildcatter's control cable. See Tailings' `DESIGN.md` and its `plc_*` scenes.

## The PLC gap wave (2026-11-09; built while Tim was away; decisions: `docs/questions/plc-gaps.md`)
Closes the "Not yet" items above that could be done properly. The rest are planned below and stay in PROGRESS.md's Known gaps.

**Built, all figures proposed:**
- **Cable cores (13b):** a control cable is a 50-pair multi-pair instrument cable, 98 signal cores after its two commons.
  - **What a signal takes:** a discrete signal takes a core; a 4-20 mA loop or a pulse input a twisted pair.
  - **Counting:** a walk finds each run of cable between two places (the cabinet, a group of touching boxes, a device). The cabinet counts
    every landed signal's cores in each run on its way, in landing order. Runs side by side between the same places add their cores.
  - **When a cable is full:** the landing that finds it full is refused, naming the cable. A signal left past the cores by a cut carries
    nothing, and the status says "CABLE FULL at ...". `ControlNet.Found` carries the routes and supplies; its old constructor stays for
    Tailings.
- **The 24 V supply's 10 A (13b):** each scan, a cabinet reports its 24 V load to the supplies on its net, shared among them; a rack's
  load goes to the rack's own supplies.
  - **What it counts:** the CPU 0.4 A small or 0.7 A large; 0.05 A a module; COMM 0.15 A; a drop's adapter 0.2 A; 7 mA an input that is
    on; 10 mA a relay that is on, plus the field device it powers (`ControlDevice.outputAmps`: pilot light 0.02 A, stack light tier 0.1 A,
    horn 0.25 A, on-off valve solenoid 0.2 A); each analog loop its current.
  - **Over 10 A:** the supply goes into hiccup, off for 1 s and on again. Its PLCs lose 24 V and make their warm restart.
- **The modules drawn (13b, 13h):** `client/PlcRackRenderer` draws the rail through the door's window, synced to clients.
  - **The rails:** two DIN rails, the CPU with its RUN lamp (green RUN, amber STOP, red FAULT) and its card, then each module in its slot,
    its label strip coloured by kind.
  - **The drop** is drawn the same way, with its adapter in the CPU's place.
- **One owner a rack (13h):** the first PLC with a CPU and 24 V to reach a drop owns it; another's landing on it is refused, naming the
  owner. When the owner's connection has been silent for 1 s, the next PLC takes it.
  - **A device point wired to two PLCs:** discrete outputs OR, as relay contacts in parallel. A loop driven by both reads as failed (two
    sources fight), named on both cabinets (`SharedOutputs`).
- **Ladder and FBD (13c):**
  - **Jumps:** rung labels, jump coils (forward only) and RETURN, in the editor, the compiler, the live view and PLCopen.
  - **FUNCTIONs:** a ladder or FBD can be a FUNCTION, called from every language.
  - **Other units' outputs:** a coil reaches one by the standard's ways (a direct address, or VAR_GLOBAL AT plus VAR_EXTERNAL). Another
    program's variable is refused with that advice.
  - **Wires:** FBD wires route around blocks (`plc.lang.WireRouter`).
- **Relay flags (13f):** each switchgear section adds TRIP 50, TRIP 51, TRIP 87B and TRIP 50BF.
- **Bugs found and fixed:** a lamp or heater on an open relay stayed lit; a firebox held back by its firing rate read BURNING.

**Proved in the world, NeoForge server scenes with a real PLC each (`PlcLimitScenes`, `PlcPowerScenes`):**
- `plc_supply`:
  - 30 horns draw 8.35 A and sound;
  - the program adds 14 more, taking it to 11.99 A, and the supply hiccups;
  - the warm restart brings it back at 8.35 A.
- `plc_cores`:
  - 34 stack lights behind one cable: the 99th signal is refused as CABLE FULL;
  - a second cable to the same box takes the rest, and every green lights;
  - with that cable cut, the last light goes dark and the status names the full cable.
- `plc_owners`:
  - rack 1 is PLC-A's, and PLC-B's landing on it is refused;
  - a lamp on both lights from either PLC;
  - a control valve's loop on both fights;
  - cutting A's network hands the rack to B, which lights the rack's lamp.
- `plc_switchgear`, a 4.16 kV feeder on REMOTE:
  - its trip coil opens it, not as a relay trip;
  - RACK OUT takes it to TEST in 15 s, and RACK IN brings it back;
  - the close coil closes it;
  - its amps are read on 4-20 mA;
  - a 5 A pickup trips it, read as TRIP 51 and only 51, and the close coil cannot close over the trip.
- `plc_ats`:
  - TEST starts the standby set;
  - with INHIBIT TRANSFER it stays on normal with emergency OK;
  - released, it transfers;
  - TEST off, it goes back to normal.
- `plc_gas_turbine`: RUN starts the 16.5 MW unit's sequence; RUNNING reads it up; RUN off stops it.
- `plc_loads`:
  - a lamp switched by the PLC;
  - a 13.8 kV load bank stepped by 4-20 mA to one 1 MW step, its watts read back (883 kW, the 1 MW gas set's sag).
- `plc_steam`:
  - a coal firebox at 0 % does not burn, and at 50 % burns about half of each period, as BURNING reads;
  - the 1 MW steam plant is started by RUN, read RUNNING at 3,600 rpm, and stopped by RUN off.
- `plc_rectifier`:
  - a rectifier on a magnesium cell is run by RUN and holds the PLC's 8 kA, then 12 kA, setpoint;
  - its amps (8.00 kA) and volts are read back;
  - RUN off stops it.

**Proved in the world, plant scenes (`PlcPlantScenes`):**
- `plc_units`:
  - **The nitric acid unit:** its ammonia comes through a motor-operated valve on steel pipe, driven by VALVE_CTRL's CMD_OPEN and
    CMD_CLOSE. It reads HELD with no enable, and makes 26.6 kg/h of HNO3 once enabled, its RATE read back.
  - **The AN neutraliser:** its ammonia comes through an on-off steel valve on instrument air, and it is enabled only while VALVE_CTRL
    proves the valve open. It runs at 34.6 kg/h. Air cut, the valve springs shut, VALVE_CTRL latches FAIL_OPEN and the unit is held. Air
    back and a RESET, it runs again.
- `plc_crane`, a crawler crane in AUTO:
  - with no RUN ENABLE it does not start;
  - enabled, it lifts, and CARRYING reads true (14.9 t on the hook);
  - with the enable withdrawn mid-lift it holds, still carrying, for 5 s;
  - enabled again, it goes on.
- `plc_motor_faults`, a 37 kW pump on a drive under MOTOR_CTRL, running proved by the drive's speed signal:
  - a welded contactor (a test hook) and a stop latch FAIL_STOP;
  - a dead supply and a start latch FAIL_START;
  - supply back and RESET, it runs.
- `plc_rig`, the full land rig on a 500 kW set:
  - in AUTO the driller's console is refused;
  - with no DRILL ENABLE it stands at 0 m;
  - enabled, it drills, with DRILLING, MUD PUMPS and IN A CONNECTION read;
  - enable withdrawn, it stops at 27 m.
- `plc_rcp`, a one-loop reactor at hot standby:
  - the coolant pump's START runs it to 100 % flow at 452 kW on 4.16 kV, RUNNING and FLOW read back;
  - START dropped, it stops, and its flywheel carries the flow down to 67 % after 5 s.
- **Found and fixed:** in AUTO or OFF the rig's readout said "right-click the driller's console", which does nothing there. It now says
  its PLC is not enabling it, or that its selector is in OFF.
- **Found, stated:**
  - a starter's RUNNING is its contactor, which closes on command even with no power, so a program proves running by speed or current,
    as a real one does;
  - nothing in play welds a contactor, so FAIL_STOP comes only from a test hook.

**Planned, not built** (questions in `docs/questions/plc-gaps.md`, 12):
- **The selector's position on each device:** a `hoa` block-state property (HAND, OFF, AUTO) drawn as a keyswitch on the face of each
  controllable device, set by `Remote.cycle`. Or a client overlay while the control key is held.
- **The horn's sound:** a `wildcatter:horn` sound event from Ludo, a 100 dB motor horn at about 400 Hz, replacing the note block.
- **Icons:** Ludo art for the nine module items and the control key (`art/source/PROMPTS.md`). The rack renderer could then draw each
  module's front from its item model.
- **The NeoForge screen:** one manual check of the PLC screen opening on the NeoForge jar; a NeoForge client scene would need its own
  harness.
- **SFC (13d):**
  - ladder or FBD transition conditions (IEC's form: a transition names a rung or network);
  - macro steps;
  - an unsafe-chart check (a token stranded or doubled) as a compile warning;
  - `SFC_INIT` and `SFC_PAUSE` implicit inputs per chart.
- **The card (13d):** retained values on the card (the CPU's RETAIN area as bytes); a blank CPU loading the card at power-up.
- **PLCopen (13d):** variable comments kept through XML; import with an undo.
- **The supply's mains draw:** it follows its load (idle 5 W plus the 24 V load at 90 % efficiency). This needs a solver hook for a load
  that changes between rebuilds.
- **The crane (13g):** a LIFT START point and HOOK LOAD and BOOM ANGLE 4-20 mA readings (`CraneBlockEntity` knows both). This needs
  a decision on what a PLC may start: a crane lift is a person's job.
- **Feedwater and a well's choke (13g):** a well choke on a signal; feedwater as a valve in the feed line under a three-element program.
  This waits on drum pressure.
- **A unit's own flows and temperatures (13g):** a `Measurable` on each of the 43 units, one by one, as the waves reach them.
- **Contactor wear:** a welded contactor after many starts under load, so that FAIL_STOP happens in play.
- **Carried from 13e and 13f, each needing a process model first:**
  - pipe pressure;
  - vibration;
  - dampers on ducts;
  - CT and PT voltage on a cable;
  - speed references on soft starters and MCC drive buckets;
  - a valve's air per stroke (an actuator volume against a receiver);
  - boiler pressure and three-element drum control;
  - a gas turbine's load setpoint and a steam turbine's own valve;
  - HMI piping graphics and several pages.

## The plan for 13e to 13i (surveyed 2026-11-07; Tim: "let's finish out the rest of 13")
What the survey found that the waves above did not say, and how each is met. All figures *(proposed)*.
- **13e:**
  - **Analog signals:** `ControlDevice` points are on/off only, so a point gains an analog kind carrying milliamps. AI8 and AO4 read and write words at the slot's address, 4 mA = 0, 20 mA = 32000; an open loop reads 32767, the channel fault. RTD4 reads tenths of a degree. HSC2 counts.
  - **Readings:** most machines report only text, so a `Measurable` interface gives numbers, machine by machine.
  - **Speed:** a drive's ramp always heads to 1.0, a motor on a drive under 98 % never counts as running, and `Motor.output()` is 0 at or below 50 % and goes as V squared above. A speed reference changes all three: the ramp heads to the reference, a drive at its reference is running, and a centrifugal load's flow follows speed and its power speed cubed, down to a minimum speed of 20 %.
  - **Valves:** none exist, and pipes hold no state: the walkers in `Plumbing` and `Lines` learn to stop at a shut valve, and a pull path gains a cap for a throttled one. Instrument air is new: compressor, dryer, tubing.
- **13f:** no voltage trim, rectifier setpoint setter, boiler firing rate or genset hertz getter exists, so each is added, behind a selector. A network cable arrives here for the HMI, and 13h extends it.
- **13g:** `PlantUnitBlockEntity` has no tick and no gate, and 29 of its 43 direct subclasses tick themselves. The selector and points go in the base, and the gate wraps the unit's ticker, so a unit not enabled does not run and says so. Units on `UnitMotorBlockEntity` also pass through its `drive`. Rods have no direct demand, only the automatic bank from a setpoint, so a demand is added that the trips still beat. Wells have no choke: an inline control valve is one.
- **13h:** as above.
- **13i:** Tailings needs a Wildcatter release holding `ControlDevice`, so v0.5.0 is released first, after the full client suite. `PlantBlockEntity` is one hook for most of the plant, through `want()`. Exceptions: belts have no start or stop and no pull cord, the sorter is clicks only, and `mine_switch` cannot be thrown, while `TrainPath.broken` ignores a throw under a built path. The TBM moves, so its signals go through the fixed cable junction, with a new link back to the machine. Sorter routing and rates need word signals, as AO does, not bits.

## Figures for 13a *(proposed)*
| Figure | Small CPU | Large CPU | Basis |
|---|---|---|---|
| Instructions a scan | 20,000 | 100,000 | A real CPU's 1-10 ms at tens of instructions a microsecond, cut to what a server can spare at 20 ticks a second |
| Memory, slots | 16,384 | 262,144 | A small and a large controller's data memory, in 8-byte slots here |
| Input, output and marker bytes | 512 / 512 / 4,096 | 4,096 / 4,096 / 65,536 | Follows the I/O counts above |
