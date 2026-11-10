# Phase 16, uranium routes and plant operations: decisions taken while Tim was away (2026-11-10)

Plan: `docs/ideas-plan.md`, Phase 16. Built: **16a** ADU yellowcake, **16b1** conversion's cathode hydrogen, ash bed and KOH, **16c1**
product assay, spin-up and cooling, **16d1** ISR CO2, the bleed and satellites. Left as written plans: 16b2, 16c2, 16d2 (ROADMAP).
Specs: `docs/multiblocks/adu-yellowcake.md`, `conversion-extensions.md`, `enrichment-operations.md`, `isr-operations.md`; each has an
"As built" section. Format as in `docs/questions/README.md`. Entries marked **needs Tim** wait on him; the rest only need a yes or a
change.

The rule for this phase: nothing may change or slow what an existing world already does. Anything that would alter a plant already built
is behind a default-off setting, or happens only once the player adds the new part.

## 16a: ADU yellowcake

### 1. Does the drum carry its grade, or is the ADU drum its own item?
- **Chosen:** its own item, **U3O8 Drum (96 %)** (`u3o8_drum`), beside the unchanged Yellowcake Drum (magnesium diuranate, 80 %).
- **Why:** an item component would make two drums of different grades refuse to stack in a hopper and need a tooltip hook; two items
  sort and stack on their own, and every drum already in a world stays exactly what it was. There are only two grades.
- **Alternatives:** a grade component on the one drum item (what the spec's wording suggests); worth it only if more grades come
  (uranyl peroxide, 16a's "left out").

### 2. With both ammonia and magnesia at the tank, which wins?
- **Chosen:** ammonia. The two precipitates are held apart in the tank, so a batch is never mixed: the calciner takes only ADU, the dryer
  only magnesium diuranate.
- **Why:** the player who pipes ammonia to a tank wants the better product; a real mill has one reagent line, and switching is a
  decision. Holding them apart is the spec's "never mixes the two in one batch".
- **Alternatives:** magnesia first while its hopper holds any (the old route wins); a sneak-click selector on the tank.

### 3. The calciner's motor box
- **Chosen:** on the north face of its main cell, not the west as on other mill parts.
- **Why:** set as the spec draws it, the calciner's west face is against the tank: a west box could never be cabled.
- **Alternatives:** keep the west and require a gap (then it would not touch the tank).

### 4. The calciner when idle and when stopped
- **Chosen:** at temperature with nothing to do it holds its hearths hot on a fifth of its full gas; without gas, power or a tank it cools
  at a third of its warm-up rate (15 minutes to cold). Five minutes from cold, as the spec proposes.
- **Why:** real multiple-hearth furnaces are kept hot between campaigns; letting it go cold at once would make every gap in ADU cost five
  minutes. A fifth is a typical holding fire (*proposed*).
- **Alternatives:** no holding fire (it cools at once); hold at full fire.

### 5. The fluorinator's ash on U3O8 drums
- **Chosen:** the spec's own basis, "the 4 % that is not U3O8": 0.049 kg of ash a kg of U, against 0.295 for magnesia drums. That is about
  a sixth, not the "about a tenth" the spec's summary says.
- **Why:** the parenthesis states the basis; the summary was an estimate.
- **Alternatives:** a tenth (0.030 kg a kg of U), if the impurities in a real ADU product are counted as lighter fluorides.

### 6. Off-gas and by-product figures
- **Chosen:** the calciner's off-gas counted at 0.37 kg a kg of U3O8 (the ADU's own ammonia and its water of hydration, *proposed*);
  the tank's ammonium salt at 3.1 kg a kg of NH3 (ammonium chloride on the salt strip, 53.5/17). Both only counted.
- **Why:** the spec asks for them counted; nothing uses them yet.
- **Alternatives:** an off-gas scrubber part (the spec leaves out ammonia recovery).

### 7. The spec's "with a dryer against it, an 80 % drum" check
- **Chosen:** the `adu_yellowcake` scene checks NO AMMONIA and that the tank goes back to the magnesia route with magnesia in its hopper;
  the dryer's 80 % drum is left to the `uraniumMill` client scene, whose code is unchanged.
- **Why:** the dryer and the calciner stand in the same place against the tank; the scene would have to break and rebuild a part.
- **Alternatives:** a second scene with a dryer.

There is no `docs/DESIGN.md` in Wildcatter (that file is Tailings'). The design record for each wave is its spec's "As built" section.
