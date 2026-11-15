# Phase 21, the quest book: decisions taken while building (2026-11-14)

Plan: `/Users/timhunsaker/.claude/plans/elegant-swinging-starfish.md` (approved by Tim 2026-11-14). Spec: `docs/quests.md`. Format as in
`docs/questions/README.md`. Each entry was **decided on recommendation; for Tim**: none needs him before it can stand, but each can be
changed. Tim went through 1-15 on 2026-11-14 (plan `elegant-swinging-starfish.md`, "Tim's answers"); 16-20 were decided while building
those answers.

## 21.1: the engine

### 1. Advancement tasks are read in the 20-tick check, not on an event
- **Accepted by Tim 2026-11-14.**
- **Chosen:** the tracker reads a player's advancements with their inventory every 20 ticks, instead of hooking the advancement-done event
  and a check on join.
- **Why:** Fabric has no advancement event, and a mixin for one would be loader code for something the check already does. Reading an
  advancement's progress is a map lookup; the check also covers advancements earned before the book existed, which is what the join check
  was for.
- **Alternatives:** NeoForge's `AdvancementEarnEvent` plus a Fabric mixin on `PlayerAdvancements.award` (done within a tick instead of
  within a second).

### 2. A quest is done when its tasks are met, whether or not its prerequisites are
- **Accepted by Tim 2026-11-14.**
- **Chosen:** prerequisites only decide whether a quest is marked **next**; a quest whose tasks are met is done even if the quests before
  it are not.
- **Why:** Tim asked that an existing world fill in from what the player already has. A player who already runs a genset but never carried
  a campfire boiler would otherwise see the genset quest stuck. A quest with no tasks (a chapter's summary) is done when the quests it needs
  are done.
- **Alternatives:** lock a quest until its prerequisites are done (a strict ladder; an existing world would fill in over several checks, and
  some quests could never be reached without going back).

### 3. Joining and reloading are seen from the server tick
- **Accepted by Tim 2026-11-14.**
- **Chosen:** the tracker notices a new player in the player list (book given once, chapters sent) and a new resource manager after
  `/reload` (chapters read again and sent to everyone) on its own tick, the way `ZirconFeed` notices a reload.
- **Why:** no new loader events, so common needs nothing more from `Platform`; the delay is at most one tick for a join.
- **Alternatives:** each loader's join and reload events wired to common (four more hooks to keep in step).

### 4. Quest text is plain English in the chapter files
- **Accepted by Tim 2026-11-14.**
- **Chosen:** titles and text are literal strings in the JSON; the screen's own labels are translated.
- **Why:** a chapter is data any mod or pack can ship; translation keys would split each quest across the chapter file and a lang file in
  each mod. Farmings' manual pages are literal text the same way.
- **Alternatives:** translation keys with the literal text as the fallback.

### 5. What counts as "never past a gate" for a reward
- **Accepted by Tim 2026-11-14.**
- **Chosen:** the check (`QuestRules`) refuses a reward whose id contains a word on the never list (plastic, polyethylene, cable, busway,
  coke, platinum, yellowcake, uranium, UF6, enriched, fuel parts, RCCA, zirconium, hafnium, helium, boron, ammonia, aluminium, titanium,
  lithium, nickel, ferroalloys, chromium, spent), and any non-vanilla reward that no hand recipe (crafting table, furnaces, campfire,
  stonecutter) makes, so nothing only a machine makes can be given.
- **Why:** the plan names plastic, cables, coke, platinum, yellowcake and "anything only a machine makes"; the recipe rule catches the last
  without a list that must grow with every plant. The claim refuses never-list items too, so a pack's chapter cannot hand them out.
- **Alternatives:** an explicit allow-list of reward items.

### 6. Milestones: 64 blocks, at most once in 5 seconds per machine and key
- **Accepted by Tim 2026-11-14.**
- **Chosen:** the plan's 64-block radius; a repeat of the same position and key within 100 ticks is ignored.
- **Why:** machines may then call the hook from their tick without counting; 5 s is short enough that a player walking up to a running
  machine gets it almost at once.
- **Alternatives:** a longer repeat (fewer lookups) or a shorter one.

### 7. A chat line when a quest is done
- **Accepted by Tim 2026-11-14.**
- **Chosen:** "Quest done: <title>" in gold for up to three quests at once; more than three (an existing world on the first check) give one
  line, "N quests done: open the quest book".
- **Why:** the player should know a reward is waiting without a toast class on each loader; one line keeps the first join from flooding chat.
- **Alternatives:** a toast (client code on both loaders), or no message.

### 8. A chapter's `after` is shown and checked, and locks nothing
- **Accepted by Tim 2026-11-14.**
- **Chosen:** `after` names the chapters a chapter follows; the check makes sure they exist. The quests' own `requires` decide what is next.
- **Why:** with every quest visible (Tim's choice), a chapter lock would only grey out quests whose `requires` already say the same.
- **Alternatives:** grey a whole chapter until the chapters it follows are done.

### 9. The book's open hook is set by `WildcatterClient.init`
- **Accepted by Tim 2026-11-14.**
- **Chosen:** `QuestBookItem.open` is set in `WildcatterClient.init()`, which each loader's client entry point calls.
- **Why:** the plan's "a client hook set by each loader's client entry" without the same line in two places; Farmings sets its hook in
  each loader because it has no shared client init.
- **Alternatives:** set it in `WildcatterFabricClient` and `WildcatterNeoForgeClient` separately.

## 21.2: Wildcatter's chapters and the milestone calls

### 10. The first 480 V supply comes after the drilling rig in the plan's order
- **Answered by Tim 2026-11-14:** a step-up transformer fed from firebox steam turbines. Built as the 75 kVA dry-type transformer
  (`docs/dry-type-transformer.md`) and the quest "Step up to 480 V" in Three-phase power, which the rig's quest now requires; the
  rig's text points to it (entries 16-19).
- **Chosen:** the plan's order is kept (Oil and gas, then Three-phase power). The rig quest's text says its drive needs 480 V three-phase and
  points to Three-phase power; a quest is done whenever its tasks are met (entry 2), so the order does not block anyone.
- **Why:** the rig and pumpjack are 480 V three-phase machines (Phase 3), and the first gensets burn gas from a well or diesel from the crude
  unit. Reordering the chapters would not remove the loop; that needs a design answer, not a quest.
- **For Tim:** how should a new world get its first 480 V three-phase before it has gas or diesel? (A diesel set with a starting tank of
  fuel, a small LV-to-480 V transformer for the steam turbines, or a rig drive on the LV tier.)

### 11. A formed multiblock fires its milestone on every structure check, not only when it forms
- **Accepted by Tim 2026-11-14.**
- **Chosen:** `Multiblock.formed` is called by the controllers each time they find their structure complete: the genset panel, the steam
  plant's turbine generator, the substation transformer, the cooling tower (once its fan runs), the reactor vessel (its island built), the HP
  turbine (its island built) and the natural-draft tower (raised). The milestone's 5 s limit keeps it cheap.
- **Why:** a structure built before the book counts as soon as a player comes near, which is what Tim asked for existing worlds.
- **Alternatives:** fire only on the change to formed (a player away at that moment would never get it).

### 12. The motor milestone reaches the level through the circuit solve
- **Accepted by Tim 2026-11-14.**
- **Chosen:** `Motor` keeps the position it was built at, and `CircuitManager` exposes the level whose circuits it is applying; a running,
  unstalled three-phase motor fires `wildcatter:motor_running` from there.
- **Why:** every three-phase motor (pumps, fans, the rig, plant motors) goes through `Motor`, so one call covers them all; the motor holds
  no level of its own.
- **Alternatives:** a call in each machine that owns a motor (some thirty places).

### 13. 57 quests rather than about 70
- **Accepted by Tim 2026-11-14.**
- **Chosen:** every bullet of the plan's quest line has a quest; where the plan lists the parts of one build (breaker, fuse, ground rod, power
  meter; separator, flare, injection, disposal) they are one quest with a task each.
- **Why:** a quest per part would be a checklist, not a guide; the tasks still tick one by one in the screen.
- **Alternatives:** split the grouped quests (about 75).

### 14. Wells, the rig and the lease are found by their items, not by milestones
- **Answered by Tim 2026-11-14:** add milestones for the oilfield. `wildcatter:pumpjack_running` and `wildcatter:well_struck` are
  fired and the Oil and gas quests ask for them (entries 18-19).
- **Chosen:** only the machines the plan lists fire milestones; the pumpjack, the rig and the lease are item tasks.
- **Why:** the plan's hook list is the list of places that already know a machine is working; adding more is cheap later.
- **Alternatives:** a `pumpjack_running` and `well_struck` milestone.

### 15. The rewards
- **Accepted by Tim 2026-11-14.**
- **Chosen:** a handful of small things per chapter, all hand-made: apples, torches, coal, bread, iron and copper ingots, paper, a water
  bucket, wool wire (16), ground rods, fuses, spark plugs, pushbuttons, a compass, steel (16 at the rig, as the plan suggests), steel pipe,
  buckets, pin insulators, 480 V wire, redstone. Nothing after the plastic gate is rewarded.
- **Why:** the plan: "small, useful, never past a gate".
- **Alternatives:** more or fewer.

## Tim's answers of 2026-11-14: the dry-type step-up and the oilfield milestones

### 16. The dry-type is grounded wye on both sides
- **Decided on recommendation; for Tim.**
- **Chosen:** `TransformerBlock.threePhase` (no delta), as the pad-mounts, LOW tier to INDUSTRIAL tier.
- **Why:** a real general-purpose unit is 480 V delta to 208Y/120 V; reverse-fed, its 480 V side would be an ungrounded delta. The rig and
  the pumpjack use A, B and C only, so a 480 V neutral changes nothing they do, and an ungrounded delta fed from the low side would float
  in the solve. The scene showed the reverse feed works with no change to the transformer code.
- **Alternatives:** the delta-high constructor (the substation's), with a grounding reference added for its 480 V side.

### 17. It takes 34 turbines to start the rig, each between two firebox boilers
- **Decided on recommendation; for Tim.**
- **Chosen:** the scene `dry_type_runs_rig` measured it (the table in `docs/dry-type-transformer.md`): 33 small turbines trip on frequency
  during the rig's direct-on-line start, 34 start it; with one boiler each the turbines nearest the dry-type run short of steam. The quest
  and the Field Manual say "at least 34, 36 to be safe, each between two firebox boilers". Nothing in the turbine, the rig or the
  frequency model was changed.
- **Why:** the plan: "If the turbines can't carry the start, the scene shows how many turbines are needed, and the quest text says it."
  Each small turbine is rated by one boiler's steam (2 kW), and the start is about 53 kW.
- **For Tim:** 34 turbines and 68 boilers is a big first build. Ways to make it smaller, none built: start the rig through a soft starter
  (its start current held at 2-5 x; the soft starter is a Phase 3 part, not checked against the plastic gate), rate a small turbine above
  one boiler's steam, or let the first rig run on a drive.

### 18. The oil well asks for the pumpjack running, not for a wellhead and a pumpjack
- **Decided on recommendation; for Tim.**
- **Chosen:** "An oil well": `pumpjack_running` alone. "A gas well": `well_struck` and 16 steel pipe. The rig keeps its item tasks
  (two pistons and the rig) and now needs "Step up to 480 V".
- **Why:** the plan says the Oil and gas quests ask for the milestones instead of carried parts; a wellhead is set by the rig and nobody
  carries one, and a pumpjack that runs has been built. `well_struck` fires on oil strikes too, so an oil strike also ticks the gas
  well's strike task; the steel pipe stays as the gas well's own task.
- **Alternatives:** a separate `gas_struck` key; keep the pumpjack as an item task as well.

### 19. Where the milestones fire
- **Decided on recommendation; for Tim.**
- **Chosen:** `pumpjack_running` in the pumpjack's well tick, on each lift (its motor running and fluid coming up); `well_struck` in the
  small rig's strike and the land rig's strike, both for oil and gas. A dry hole, a too-deep stop and an ISR well do not fire it.
- **Why:** the places that already know; the 5 s limit per position keeps it cheap.

### 20. The client screenshot builds the plant by command
- **Decided on recommendation; for Tim.**
- **Chosen:** `dryType` in the client test builds 36 turbines with two firebox boilers each on a platform 40 blocks up (so the rig has
  rock to drill), the player standing on the ground below while it warms up, then on the platform for the two shots.
- **Why:** a player left in the air falls and dies, and a dead player ticks nothing, so the fireboxes never lit.

