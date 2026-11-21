# Shorts batch 4 (2026-11-21): decisions taken filming turbine and cascade

Two TikTok shorts were filmed: a gas turbine start onto a live bus, and the enrichment cascade. Branch `films-batch4`. The format is as
in `README.md` here. Each entry was **decided on recommendation; for Tim**: none needs him before it can stand, but each can be changed.

### 1. The gas turbine's exhaust seen at its stack
- **Chosen:** while the turbine burns fuel, its exhaust stack's top shows it: at light-off 3 s of smoke (vanilla large smoke and
  campfire smoke), then a thin white haze that grows with the fuel it burns (white smoke every 4 ticks, now and then a campfire wisp; faint on camera). Nothing shows with an HRSG in
  the stack's place (the HRSG takes the exhaust). The particles are sent to players up to 512 blocks away, like the ones drawn far.
- **Why:** before this a start showed nothing at all: the turbine has no moving part on its outside. A liquid-fuel light-off does throw a
  puff out of a real stack; a running one shows a shimmer of hot gas, which the game cannot draw, so a thin haze stands in for it.
- **Alternatives:** no haze, the light-off puff only (a running unit then looks stopped); a heat-shimmer shader (no such thing in the
  game's particles).

### 2. The exhaust on the films' card: heat, not temperature
- **Chosen:** the card reads the turbine's exhaust heat in MW (`exhaustWatts`: the fuel it burns less what it makes), not an exhaust
  temperature.
- **Why:** the turbine models no exhaust temperature. The 16 MW unit's spec gives one figure (50 kg/s at about 495 C at full load) and
  the aeroderivative's none; a temperature curve over the start would be a new mechanic with new numbers.
- **Alternatives:** model exhaust temperature from the fuel burnt, a mass flow and a heat capacity (needs the aeroderivative's flow,
  and Tim's approval as a new mechanic).

### 3. The synchronizer seen sweeping into phase
- **Chosen:** the film puts the starting unit's synchronizer 150 degrees behind the bus by a test hook as it reaches full speed; from
  there the slip (0.05 Hz fast), the sweep and the close inside the 10 degree window are the synchronizer's own. A public
  `GeneratorPartBlockEntity.synchronizer()` lets the card read the slip and phase.
- **Why:** the game starts every synchronizer's slip at 0 degrees, so a machine closes one tick after it reaches speed and no sweep is
  ever seen. A real machine comes up at an arbitrary phase to the bus.
- **Open for Tim:** whether the game itself should start the slip at a random angle (a generator would then take up to 20 s to close
  at the auto-synchronizer's 0.05 Hz). Not changed here: it changes every generator's timing and its scenes.

### 4. The cascade's stages on the card
- **Chosen:** the cascade film's card walks the eleven stages, each raising the U-235 abundance ratio by the same factor from the tails'
  0.25 % to the product's 4.95 % (`Enrichment.stages()` and the assays in `Tuning`), one machine a stage as the spec has it.
- **Why:** the cascade's product is a set assay; nothing in it climbs over time. The stage by stage climb is the real reason a cascade
  needs its machines, and it is derived from the mod's own numbers.
