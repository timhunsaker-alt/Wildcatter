# Shorts batch 3 (2026-11-21): decisions taken filming scram, blowout and arcflash

Three TikTok shorts were filmed: a reactor trip, an uncontrolled well and a reclose onto a standing fault. Each needed something drawn or
heard that the mod did not yet show. Branch `films-batch3`. The format is as in `README.md` here. Each entry was **decided on
recommendation; for Tim**: none needs him before it can stand, but each can be changed.

### 1. The rod drop drawn: rod position indicators on the reactor console
- **Chosen:** the console carries a vertical board on its top, like the rod control section of a real main control board. It has one
  column of 12 green lamps for each RCCA: the four shutdown bank rods, a gap, then bank D. A column is lit up to its rod's position over
  the 228 steps. A red ROD BOTTOM lamp under each column lights when that rod is fully in. A REACTOR TRIP annunciator across the top
  flashes red while the reactor is tripped. The vessel sends the positions to the console each step (every 4 ticks). The client eases
  between updates, so a trip is seen as the columns falling in 2 s.
- **Why:** a real plant shows rod motion on the board's rod position indication (digital RPI), not on the head. A real head's drive rods
  move inside sealed rod travel housings and are never seen. The numbers are the reactor's own; nothing new is modelled.
- **Alternatives:** drive shafts drawn rising out of the CRDM head (reads from further away, but no real head shows them); a screen on
  the console (needs text rendering in the world).

### 2. The trip's alarms
- **Chosen:** a new trip clunks the reactor trip breakers open (the breaker trip sound at the console). The console's horn then sounds a
  blast every 1.5 s for 9 s. A plant that was already tripped when it loaded stays silent.
- **Why:** on a real trip the trip breakers open and the control room's annunciators sound until acknowledged. The mod has no
  acknowledge button, so the horn stops on its own.
- **Alternatives:** a horn that sounds until the trip is reset (very loud in a base); an acknowledge on the console (a new control).

### 3. A blowout seen at the rig floor
- **Chosen:** an unlit blowout throws a spray of mud and oil drops (a new particle, `blowout_spray`, dark brown and near black) up out
  of the rotary table. The spray rises some 10 to 15 m and falls back round the rig. A mist of gas (vanilla clouds) spreads across the
  floor, and the choke manifold's flow loop plays at the hole every 4.5 s as its roar. The black smoke it had is kept but thinned.
  A blowout on fire is unchanged (flames).
- **Why:** before this an unlit blowout showed only dark smoke puffs, which read as fire smoke. A real surface blowout is a column of
  mud, oil and gas over the floor.
- **Alternatives:** vanilla block-crack particles (the game caps their speed, so no jet); a Ludo roar sound (one more asset).

### 4. The arc at the grounding clamps
- **Chosen:** each time fault current starts through grounding clamps, a blue-white glare (a new particle, `arc_flash`, a metre or two
  across at full brightness, 3 to 5 ticks) is drawn there with the sparks. Wave A's flash was the sparks alone.
- **Why:** the 40 small sparks did not read on camera from more than a couple of metres. A bolted fault's arc lights its surroundings
  for the few cycles it lasts.
- **Kept from Wave A:** no flash is drawn at the breaker, as Wave A decided (6): a vacuum interrupter's arc is inside its bottle.
- **Kept from Wave A:** the lamps past a bolted fault stay dark when the breaker recloses onto it (7). The short says so; it does not
  show them flashing on.
