# The quest book (Phase 21)

A guide from punching trees to a reactor on line, with every quest visible and the ones whose prerequisites are done marked
**next**. Quests are found by themselves: what the player carries, the vanilla advancements they have, and machines they stand near
when they run. Nothing changes an existing world: the book is an item, progress starts empty and fills in from what the player
already has and runs.

Plan: `/Users/timhunsaker/.claude/plans/elegant-swinging-starfish.md` (approved by Tim 2026-11-14). Decisions taken while building:
`docs/questions/phase21-quests.md`.

## The book
- `wildcatter:quest_book`: a book, a copper ingot and paper (shapeless). Every player is given one on their first join (the player tag
  `wildcatter.quest_book_given`, so a second join, a death or a relog gives nothing). Use it to open the quest screen.
- The screen: chapters down the left with how many of their quests are done; open a chapter to list its quests, each with its icon and
  marked done (a tick), next (its prerequisites are done) or later (faint). The right page shows the chosen quest: its text, each task with
  its icon and a tick, the quests it needs (click one to go to it), and its rewards with a **Claim** button once it is done. A search box
  finds quests by title or text across every chapter.

## Detection (server side, `dev.wildcatter.quest.QuestTracker`)
- Every 20 ticks each player's open quests are checked:
  - an **item** task is met while the player carries the count (any components; cables carry their size as a component);
  - an **advancement** task is met once the player has the vanilla advancement (so advancements earned before the book existed count);
  - a **milestone** task is met once a machine near the player has fired the milestone (below). Milestones are kept with the player.
- A quest is done when all its tasks are met at once, whether or not its prerequisites are done (an existing world fills in at once).
  Once done it stays done. A quest with no tasks is done when the quests it needs are done.
- Progress is kept per player in the overworld's saved data `wildcatter:quests` (done quests, claimed rewards, milestones reached).

## Milestones: `QuestEvents.milestone`
```java
dev.wildcatter.quest.QuestEvents.milestone(ServerLevel level, BlockPos at, Identifier key)
dev.wildcatter.quest.QuestEvents.milestone(Level level, BlockPos at, Identifier key)   // does nothing on a client level
```
Awards the milestone `key` to every player within 64 blocks of `at`. A machine may call it every tick while it works: it is counted at
most once every 5 seconds for the same position and key. Add-ons (Tailings, Farmings, any mod) call the same method with their own keys;
it is public API and will not change shape.

The milestone keys Wildcatter fires are listed under "Milestones Wildcatter fires" at the end.

## Chapters: `data/<namespace>/wildcatter_quests/<chapter>.json`
Loaded from every namespace's data, so a mod ships its chapter in its own jar and it appears only when that mod is installed; a data pack
can replace a chapter by the same path. Read when the server starts and again after `/reload`, and sent to each player on join.

```json
{
  "title": "Open pit",
  "icon": "tailings:stamp_mill",
  "order": 110,
  "after": ["wildcatter:getting_started"],
  "quests": [
    {
      "id": "stamp_mill",
      "title": "A stamp mill",
      "icon": "tailings:stamp_mill",
      "text": "Crush ore by the stamp on water power. Build it beside a stream.",
      "requires": ["wildcatter:steel"],
      "tasks": [
        {"item": "tailings:stamp_mill", "count": 1},
        {"milestone": "tailings:stamp_mill_running", "text": "Run the stamp mill"}
      ],
      "rewards": [{"item": "minecraft:bread", "count": 8}]
    }
  ]
}
```

Chapter fields:
- `id` (optional): the chapter id; by default `<namespace>:<file name>`.
- `title`, `icon` (an item id): shown in the list.
- `order` (number): chapters are listed by it, lowest first. Wildcatter's are 10 to 90; put add-on chapters at 100 and up.
- `after` (optional): chapter ids this one follows, shown in its header. Each must exist.
- `quests`: the quests, in the order they are listed.

Quest fields:
- `id`: a bare id takes the chapter's namespace (`stamp_mill` in `data/tailings/...` is `tailings:stamp_mill`); a full id may be given.
- `title`, `icon` (an item id), `text`: a few short lines: what to build, what it needs, where its input comes from.
- `requires` (optional): quest ids, from any namespace. A bare id is in the chapter's namespace.
- `tasks`: all must be met. Each is one of:
  - `{"item": "<item id or #tag>", "count": n}` (count defaults to 1);
  - `{"advancement": "<advancement id>"}`, e.g. `minecraft:story/mine_stone`;
  - `{"milestone": "<key>"}`.
  Any task may carry `"text"`, shown instead of the automatic line (give one for every milestone).
- `rewards` (optional): `[{"item": "<item id>", "count": n}]`, claimed with the button once the quest is done. Small and useful, and never
  past a gate.

## The check: `quests_valid`
A NeoForge game scene (and the server's log at load) checks every chapter it finds:
- every item, tag, icon and advancement resolves;
- every `requires` and `after` names a quest or chapter that exists, and quests have no cycle;
- quest ids are unique;
- every reward is something a player can make by hand (a vanilla item, or an item with a crafting-table, furnace, blast furnace, smoker,
  campfire or stonecutter recipe) and is not on the never-reward list: plastic and polyethylene, any cable or busway, coke, platinum,
  yellowcake, uranium products, zirconium, and the fuel parts (`dev.wildcatter.quest.QuestRules.NEVER_REWARD`).

An add-on can run the same check in its own game test: `dev.wildcatter.quest.QuestRules.problems(server)` returns the list of problems
(empty when every chapter is good).

## Sync: `wildcatter:quest`
One payload, a compound tag either way (as `wildcatter:plc`): the server sends the chapters on join and after a reload, and the player's
progress when it changes; the client sends a claim. Both loaders register it.

## Milestones Wildcatter fires
(Filled in with the chapters, step 2.)
