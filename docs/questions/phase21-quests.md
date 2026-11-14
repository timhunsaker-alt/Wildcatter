# Phase 21, the quest book: decisions taken while building (2026-11-14)

Plan: `/Users/timhunsaker/.claude/plans/elegant-swinging-starfish.md` (approved by Tim 2026-11-14). Spec: `docs/quests.md`. Format as in
`docs/questions/README.md`. Each entry was **decided on recommendation; for Tim**: none needs him before it can stand, but each can be
changed.

## 21.1: the engine

### 1. Advancement tasks are read in the 20-tick check, not on an event
- **Chosen:** the tracker reads a player's advancements with their inventory every 20 ticks, instead of hooking the advancement-done event
  and a check on join.
- **Why:** Fabric has no advancement event, and a mixin for one would be loader code for something the check already does. Reading an
  advancement's progress is a map lookup; the check also covers advancements earned before the book existed, which is what the join check
  was for.
- **Alternatives:** NeoForge's `AdvancementEarnEvent` plus a Fabric mixin on `PlayerAdvancements.award` (done within a tick instead of
  within a second).

### 2. A quest is done when its tasks are met, whether or not its prerequisites are
- **Chosen:** prerequisites only decide whether a quest is marked **next**; a quest whose tasks are met is done even if the quests before
  it are not.
- **Why:** Tim asked that an existing world fill in from what the player already has. A player who already runs a genset but never carried
  a campfire boiler would otherwise see the genset quest stuck. A quest with no tasks (a chapter's summary) is done when the quests it needs
  are done.
- **Alternatives:** lock a quest until its prerequisites are done (a strict ladder; an existing world would fill in over several checks, and
  some quests could never be reached without going back).

### 3. Joining and reloading are seen from the server tick
- **Chosen:** the tracker notices a new player in the player list (book given once, chapters sent) and a new resource manager after
  `/reload` (chapters read again and sent to everyone) on its own tick, the way `ZirconFeed` notices a reload.
- **Why:** no new loader events, so common needs nothing more from `Platform`; the delay is at most one tick for a join.
- **Alternatives:** each loader's join and reload events wired to common (four more hooks to keep in step).

### 4. Quest text is plain English in the chapter files
- **Chosen:** titles and text are literal strings in the JSON; the screen's own labels are translated.
- **Why:** a chapter is data any mod or pack can ship; translation keys would split each quest across the chapter file and a lang file in
  each mod. Farmings' manual pages are literal text the same way.
- **Alternatives:** translation keys with the literal text as the fallback.

### 5. What counts as "never past a gate" for a reward
- **Chosen:** the check (`QuestRules`) refuses a reward whose id contains a word on the never list (plastic, polyethylene, cable, busway,
  coke, platinum, yellowcake, uranium, UF6, enriched, fuel parts, RCCA, zirconium, hafnium, helium, boron, ammonia, aluminium, titanium,
  lithium, nickel, ferroalloys, chromium, spent), and any non-vanilla reward that no hand recipe (crafting table, furnaces, campfire,
  stonecutter) makes, so nothing only a machine makes can be given.
- **Why:** the plan names plastic, cables, coke, platinum, yellowcake and "anything only a machine makes"; the recipe rule catches the last
  without a list that must grow with every plant. The claim refuses never-list items too, so a pack's chapter cannot hand them out.
- **Alternatives:** an explicit allow-list of reward items.

### 6. Milestones: 64 blocks, at most once in 5 seconds per machine and key
- **Chosen:** the plan's 64-block radius; a repeat of the same position and key within 100 ticks is ignored.
- **Why:** machines may then call the hook from their tick without counting; 5 s is short enough that a player walking up to a running
  machine gets it almost at once.
- **Alternatives:** a longer repeat (fewer lookups) or a shorter one.

### 7. A chat line when a quest is done
- **Chosen:** "Quest done: <title>" in gold for up to three quests at once; more than three (an existing world on the first check) give one
  line, "N quests done: open the quest book".
- **Why:** the player should know a reward is waiting without a toast class on each loader; one line keeps the first join from flooding chat.
- **Alternatives:** a toast (client code on both loaders), or no message.

### 8. A chapter's `after` is shown and checked, and locks nothing
- **Chosen:** `after` names the chapters a chapter follows; the check makes sure they exist. The quests' own `requires` decide what is next.
- **Why:** with every quest visible (Tim's choice), a chapter lock would only grey out quests whose `requires` already say the same.
- **Alternatives:** grey a whole chapter until the chapters it follows are done.

### 9. The book's open hook is set by `WildcatterClient.init`
- **Chosen:** `QuestBookItem.open` is set in `WildcatterClient.init()`, which each loader's client entry point calls.
- **Why:** the plan's "a client hook set by each loader's client entry" without the same line in two places; Farmings sets its hook in
  each loader because it has no shared client init.
- **Alternatives:** set it in `WildcatterFabricClient` and `WildcatterNeoForgeClient` separately.
