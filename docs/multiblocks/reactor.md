# The nuclear island: a 9-assembly, two-loop PWR (APPROVED 2026-11-01 with the Phase 8a plan)

Nine 17×17 assemblies from the fuel plant (fuel.md) in a 3×3 core, cooled and moderated by borated water at 15.5 MPa. Two loops, as in a
2-loop Westinghouse plant, carry the core's heat to two U-tube steam generators. 8a goes critical and makes steam with no turbine: it
runs at low power on the auxiliary feed pump and vents its steam through the atmospheric dump valves, as a real plant does in
low-power physics testing. The steam leaves on a pipe for 8b's turbine.

Decided with Tim on 2026-11-01:
- **9 full-height assemblies at real power density**, about 155 MW thermal. The small core goes critical as a full core would: a
  written exception, like the cascade's 1000x.
- **Burnup 100x:** real power, the uranium used 100 times faster.
- **Boric acid in the coolant plus control rods**, the rods of **boron carbide** (boron.md).
- **Two loops.**

Sources:
- **The Westinghouse 17×17 core:**
  - 3,411 MWt in 193 assemblies, so an average linear heat rate of about 17.8 kW/m;
  - Tavg 557 °F (291.7 °C) at no load, about 588 °F (308.9 °C) at full load;
  - 2,250 psia (15.51 MPa).
- **Trips** (Westinghouse standard technical specifications, NUREG-1431):
  - high pressurizer pressure 2,385 psig (16.54 MPa), low pressure about 1,865 psig (12.96 MPa);
  - low flow 90 %; power-range high flux 109 %, the low setpoint 25 %;
  - P-7 and P-10 at 10 %, P-8 at 35 % *(for this plant)*.
- **Rods and boron** (NUREG-1431):
  - magnetic-jack CRDMs at 72 steps a minute over 228 steps, a rod drop under 2.7 s;
  - a shutdown margin of 1.3 % Δk/k;
  - boron worth about −10 pcm/ppm *(textbook, unverified)*.
- **Kinetics:** β = 0.0065 with Keepin's 6 delayed groups for U-235; prompt generation time about 2×10⁻⁵ s.
- **Steam:** IAPWS-IF97. Its saturation line is used exactly; hf and hg come from short fits checked against the tables.

A figure marked *(unverified)* is textbook, an estimate or a proposal.

## Numbers
**The core:**
- **Power:** 264 rods × 3.66 m × 17.8 kW/m = **17.2 MW an assembly, 155 MWt** for 9.
- **Uranium:** 4,136 kg, so a specific power of 37.5 MW/t.
- **Burnup 100x:** 3.75 GWd/t a day at full power.
  - The core is refuelled a third at a time and discharged at about 50 GWd/t.
  - A cycle is about **107 h** at full power, so a new assembly is needed about every 36 h.
- **Reactivity of an assembly** *(proposed, so a cycle ends at 0 ppm)*: **+16,000 pcm − 380 pcm per GWd/t**. The core is the
  average of its 9.
- **Feedback** *(textbook, unverified)*, all from hot zero power (291.7 °C):
  - Doppler: −2.5 pcm/°C of fuel temperature. The fuel runs 600 °C over the coolant at full power.
  - Moderator: −40 pcm/°C at 0 ppm, rising linearly to −5 pcm/°C at 1,500 ppm.
  - Boron: −10 pcm/ppm.
- **The source:** a small intrinsic neutron source, so the source range reads while the core is shut down.

**Control rods: 5 RCCAs** of 24 B4C rodlets each.
- **Positions:** 1 in the centre, 4 on the edges.
- **Worth:** **6,000 pcm** in all *(proposed)*, 1,600 for the centre and 1,100 for each edge rod.
  - Along its travel a rod's worth follows the usual S-curve: little at the ends, most in the middle.
  - With the centre rod stuck out, 4,400 pcm is left. That covers the 1,500 pcm power defect with the required 1,300 pcm to spare.
- **Banks:**
  - The 4 edge rods are the **shutdown bank**, all the way out before startup.
  - The centre rod is **control bank D**: automatic rod control moves it to hold the setpoint.
- **Speed:** 72 steps a minute, 228 steps (3.2 min for full travel). They drop in 2.0 s.

**The primary loop, at full power:**
- **Core ΔT:** 33 °C.
- **Flow:** 862 kg/s, **431 kg/s a loop**.
  - With the pumps stopped, natural circulation carries 3 % *(proposed)*.
- **Reactor coolant pump:** **500 kW at 4.16 kV** *(proposed)*, about 85 m of head.
  - It needs **2.3 MPa** for its seals.
  - Its flywheel halves the flow in about 10 s once its power is lost.
  - 90 % of its power heats the water.
- **Water:** **30 t** of coolant. The coolant and metal hold **300 MJ/K** *(proposed)*.
  - The RCS fills at **10 kg/s** off the CVCS's makeup pump, blended to the boron target.
- **Pressurizer:** **8 m³**, **150 kW of heaters at 480 V** *(proposed)*.
  - Its water and steam hold 20 MJ/K *(proposed)*.
  - Heaters come on below 15.41 MPa; spray comes on above 15.65 MPa and needs a pump running.
  - The PORVs hold 16.2 MPa.
  - A rise in Tavg pushes water in and lifts its temperature 0.3 °C a degree *(proposed)*.

**The steam generators** (each):
- **Heat:** 77.5 MW at full load.
- **Heat transfer:** UA 3.2 MW/K at full flow, scaled by the loop flow to the 0.8 power.
- **Steam:** saturated, 6.9 MPa at full load, 7.6 MPa (Tsat = Tavg) at no load.
  - **43 kg/s** each off feedwater at 227 °C (1.796 MJ/kg).
  - The feedwater is counted at 227 °C, the aux feed's included *(simplified)*.
- **Secondary:** 20 t of water and 120 MJ/K *(proposed)*. It sits filled to its normal level from the start, in wet lay-up.
  - The low-low level trip is at 40 %.
- **Atmospheric dump valve:** it opens above 7.6 MPa and vents up to 4.3 kg/s, 10 % of full steam, counted.

**Heat-up:**
- Below the no-load temperature, the RCS, the steam generators and the pressurizer heat and cool as if they held 1/80 of their heat
  *(proposed)*.
- Cold to hot standby takes **about 30 min** on the pumps' own heat. Real plants take 8 to 12 hours.

**CVCS:**
- **Flow:** 0.5 kg/s of letdown and charging *(proposed)*, with a **15 kW** charging pump at 480 V.
- **Boron:** it borates from its **7,000 ppm** boric acid tank, or dilutes with makeup water, toward its target.
  - Diluting from 1,500 ppm loses about 1.5 ppm a minute; borating gains about 5 ppm a minute.
  - The letdown's boron is recovered to the tank, as by a boron recycle evaporator.
- **Bags:** a 25 kg bag of boric acid is 4.37 kg of boron. 1,500 ppm in 30 t is 45 kg of boron, about **10 bags**.

**Aux feed pump:** 5 kg/s at about 8 MPa, **56 kW (75 hp) at 480 V**. Enough for about 6 % power.

## Parts (cells W x H x L, sizes and watts *proposed*; each drawn facing north, its main cell at its north-west bottom corner)
| Part | Cells | Real basis |
|---|---|---|
| `reactor_vessel` | 3 x 10 x 3 | The vessel, core barrel and baffle, the 9 assemblies and their RCCAs, the upper internals, and 2 hot and 2 cold leg nozzles. **The island's controller.**<br>• Load fuel assemblies and RCCAs by right-click, only when it is cold (under 60 °C), depressurized and its pumps are stopped.<br>• A fresh assembly comes back out to an empty sneak-click. One with burnup on it waits for 8c's refuelling machine. |
| `crdm_head` | 3 x 3 x 3 | The closure head on the vessel: 5 magnetic-jack CRDMs and their rod position indicators. **25 kW at 480 V**. Losing its power drops the rods. |
| `reactor_console` | 3 x 2 x 1 | The main control board, against the vessel. A click reads the plant.<br>• A sneak-click steps the setpoint: TRIP, 0, 2, 5, 10, 25, 50, 75, 100 %.<br>• TRIP is the manual trip. Leaving it resets the trip once its cause has cleared. |
| `steam_generator` (2) | 3 x 12 x 3 | A U-tube steam generator against the vessel (its hot leg).<br>• Feedwater in on the east face of its bottom cells; steam (`STEAM`) out of its top face.<br>• Its atmospheric dump valve on top. |
| `reactor_coolant_pump` (2) | 2 x 5 x 2 | A shaft-seal pump with its 4.16 kV motor and flywheel on top, against its steam generator.<br>• **500 kW at 4.16 kV**.<br>• A sneak-click switches it to stop or to start. |
| `pressurizer` | 2 x 8 x 2 | Heaters, spray, PORVs and safety valves, against the vessel or a steam generator (its surge line). **150 kW at 480 V**. |
| `cvcs_skid` | 3 x 3 x 6 | The regenerative and letdown exchangers, demineralizer, volume control tank, charging pump, boric acid tank and blender, against the vessel.<br>• Boric acid bags by a hopper on top; makeup water off a pump.<br>• A sneak-click steps the boron target by 50 ppm, from 0 to 2,500. **15 kW at 480 V**. |
| `aux_feed_pump` | 2 x 2 x 4 | A motor-driven auxiliary feedwater pump. Water off a pump; `FEEDWATER` out of the south face of its back bottom cells. **56 kW at 480 V**. |

## Layout (all facing north; x east, z south; the vessel's main cell at 0,0,0)
Ground (y 0):
```
x:   -3-2-1 0 1 2 3 4 5
z-5   . . . R R . . . .      R  loop A's reactor coolant pump (y 0-4)
z-4   . . . R R . . . .
z-3   . P P A A A f . .      A  loop A's steam generator (y 0-11); f its feedwater nozzle faces
z-2   . P P A A A f . .      P  the pressurizer (y 0-7), against A
z-1   . . . A A A f . .
z0    C C C V V V . . .      V  the reactor vessel (y 0-9); H, the CRDM head, on it at y 10-12
z1    C C C V V V K K K      K  the console (y 0-1)
z2    C C C V V V . . .      C  the CVCS skid (y 0-2)
z3    C C C B B B f . .      B  loop B's steam generator (y 0-11)
z4    C C C B B B f . .
z5    C C C B B B f . .
z6    . . . Q Q . . . .      Q  loop B's reactor coolant pump (y 0-4)
z7    . . . Q Q . . . .
```
- **Layers:**
  - y 0 to 9: the vessel;
  - y 10 to 12: the CRDM head over it;
  - y 0 to 11: the steam generators. Steam leaves their top faces at y 12.
- **The aux feed pump** stands anywhere, piped to the steam generators' feedwater faces.
- **Rotation:** any of the four. Each part is placed on its own and they work by touching.

## Nozzles
- **480 V and 4.16 kV boxes:** the west face of each powered part's main cell. These are the pumps, the CRDM head, the pressurizer, the CVCS and the aux feed pump.
- **Steam:** out of the top face of each steam generator.
- **Feedwater:** in on the east face of each steam generator's bottom cells, and out of the south face of the aux feed pump's back bottom cells.
- **Water:** the CVCS and the aux feed pump take water off a pump.
- **Boric acid:** bags by a hopper on top of the CVCS.

## How it forms
- The vessel finds, by touching:
  - the CRDM head (on top);
  - the console, the CVCS and the two steam generators;
  - the pressurizer, against the vessel or a steam generator.
- Each steam generator finds its pump by touching.
- **NOT BUILT** names what is missing.
- It holds the rods in (the readout says why) until all of these hold:
  - 9 assemblies and 5 RCCAs are loaded;
  - the RCS is full;
  - a pump runs;
  - the pressure is over 12.96 MPa.
- **Removing a part:**
  - the head drops the rods;
  - a pump coasts down;
  - a steam generator stops its loop's heat;
  - without the pressurizer, the pressure falls and it trips.

## How it runs
**Each interval** the vessel steps the plant (fluid/Reactor.java):
1. The rods move: the shutdown bank out once reset, bank D under automatic control.
2. The CVCS moves the boron.
3. Point kinetics give the power.
4. The heat balance gives Tavg, Thot and Tcold.
5. The steam generators take UA·(Tavg − Tsat).

**Automatic rod control:**
- Bank D withdraws while the power is under its setpoint and the startup rate is under 1 decade a minute.
- It inserts above the setpoint, or above 1.5 dpm.

**Steam:**
- Each steam generator offers the steam it has to its pipe. What a pipe draws carries heat away.
- The dump valve vents what holds the pressure over 7.6 MPa.
- It pulls feedwater to hold its normal level.

**Trips:**
- **Causes:**
  - manual;
  - high flux at 109 %, and 25 % below P-10;
  - low flow at 90 %, above P-7;
  - a pump lost above P-8 (35 %); below it, the plant runs on one loop;
  - pressurizer pressure low (12.96 MPa, above P-7) or high (16.54 MPa);
  - overtemperature: the core outlet over 343 °C, or within 3 °C of boiling *(simplified from OTΔT)*;
  - steam generator low-low level;
  - CRDM power lost.
- **What happens:** all rods drop in 2 s, and the readout names the first cause.
- **After a trip:** the power falls on the delayed neutrons. Decay heat is 8c.

**The readout** shows:
- power and startup rate;
- the source-range count while shut down;
- rod steps and boron;
- Tavg, Thot and Tcold;
- pressurizer pressure;
- each steam generator's level and pressure;
- each pump's state and the trip.

## Game test
`reactor`:
- **Power:** 4.16 kV for the pumps from a diesel set and a unit substation run backward; 480 V on a busway.
- **Set up by hook:** the RCS filled and borated to 1,500 ppm; 9 fresh assemblies and 5 RCCAs loaded; the plant at hot standby.
- **Startup:** the setpoint at 2 %.
- **Checks:**
  - the shutdown bank comes out, and the reactor goes critical with a positive startup rate;
  - it holds about 2 % on bank D;
  - steam vents at the dump valves while the aux feed pump holds the levels;
  - a pump stopped at 2 % leaves it running on one loop;
  - a manual trip drops the power under 0.1 % in seconds, with all rods at 0;
  - after a reset, cutting the CRDM head's power trips it again.
- **Screenshot:** the island.

## Added while building (8a)
- **The heat-up speed-up** (1/80 of the heat capacity) applies only while the plant gains heat. A cool-down runs at real speed, so a
  hot plant left without its pumps or heaters cools slowly, as a real one does.
- **The moderator temperature coefficient** is never positive: at very high boron the linear figure is held at 0.
- **When the rods are held** (no pump, low pressure, the RCS not full, the core not loaded), bank D is driven in; the shutdown bank
  stays where it is.
- **Burnup on the assembly item:** an irradiated assembly cannot leave the vessel in 8a, so its burnup stays with the vessel. It
  goes onto the item with the refuelling machine in 8c.
- **Game test:**
  - the console sits against the vessel's east face three blocks up, so the feedwater pipe runs under it;
  - the aux feed pump has a diesel set of its own;
  - the pressurizer and aux feed stubs are 500 kcmil, as the zirconium and fuel plants' stubs are.
