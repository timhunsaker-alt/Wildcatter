# Checking the plugs against real Mekanism machines

> **DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed wave **14d**, to run **when
> Mekanism for 26.3 is released**. Decisions are in `docs/questions/specs-wildcatter.md` (items 12-13).

Phase 9c made plugs feed NeoForge's energy capability, checked against a stand-in `SimpleEnergyHandler` because Mekanism for 26.3 was
not out (PROGRESS.md, Known gaps, 9a and 9c). This is the plan for the real check.

## Is Mekanism for 26.3 out? (checked 2026-11-09)
**No.**
- The official repository's latest release is **v1.21.1-10.7.19.85** (Minecraft 1.21.1); no release targets any 26.x version
  ([Mekanism releases](https://github.com/mekanism/Mekanism/releases)).
- The repository's main branch is now named **26.3** and its README reads "Mekanism for Minecraft 26.3", so the port is in progress
  ([Mekanism on GitHub](https://github.com/mekanism/Mekanism)). The ROADMAP's "10.8.0 on its dev branch" (2026-11-02) is not visible
  on the page any more; the version it will ship as is unknown.
- An unofficial fork, "Mekanism: Version Locked", targets 26.1.2 and says it will not move past it
  ([modpackindex](https://www.modpackindex.com/mod/205712/mekanism-tools-version-locked)). It is not a target: Wildcatter is 26.3.

**Recommendation:** keep the item in Ideas; re-check the releases page at the start of each phase. Do not build against a dev
snapshot of the 26.3 branch: its API can move under us.

## What to check, once it is out
Each check is a NeoForge GameTest smoke scene (Phase 9's rule: NeoForge gets smoke scenes; the Fabric client suite stays the full one),
with Mekanism as a **test-only runtime dependency** of `neoforge/` (never a compile dependency: no code needs it, as 9c decided).

| # | Check | Pass |
|---|---|---|
| 1 | **A Mekanism machine beside a plug draws.** An Energized Smelter (a small, steady draw) next to a 120 V plug on a 20 A breaker, smelting. | It smelts; the plug reads real amps; the meter's watts match the machine's FE/t at 1 W = 1 FE/t. |
| 2 | **It stops drawing when full.** The same machine idle with a full buffer. | The plug reads 0 A, as with the stand-in. |
| 3 | **A buffer fills at the plug's limit, not the machine's.** A Basic Energy Cube beside a plug: it accepts far more than a 20 A, 120 V plug gives. | The plug delivers up to its circuit; the breaker trips if the plug is set above its rating, as the "plug overload" decision says. |
| 4 | **A cable network as the consumer.** Universal cable from a plug to three machines. | The plug sees the network's simulated insert and supplies what the three draw. |
| 5 | **A big draw on three-phase.** A Digital Miner or an Electrolytic Separator run hard, on an L21-30 (NA) or IEC 60309 red plug. | Real current on all three phases; no phase imbalance beyond the plug's own (it is a balanced load). |
| 6 | **No power comes back.** A Mekanism generator (a solar or a gas-burning generator) cabled to a plug. | Nothing flows into the Wildcatter grid: power leaves Wildcatter only through plugs (VISION, Interop). |
| 7 | **Mekanism's unit.** Mekanism shows Joules and converts them from FE by its own setting *(1 FE = 2.5 J by default, unverified)*. | The plug's `wattsPerFe` setting alone decides watts to FE; Mekanism's display is its own business. Document the conversion in the Field Manual's plugs page. |
| 8 | **The transaction semantics of 26.3's energy API.** The plug's demand is a simulated insert in a root transaction never committed (9c). | No machine keeps energy from a simulated insert (check with a machine that has an internal buffer and an upgrade slot). |
| 9 | **Chunk unload.** The plug's chunk unloaded and reloaded while a machine draws. | No duplicated or lost energy; the plug's circuit part comes back with the chunk (the 9b `LevelChunk` mixin). |

## What is not checked
- **Fabric:** Mekanism is NeoForge-only; Fabric keeps Team Reborn Energy (9a).
- Mekanism's own balance (how much FE its machines should cost against Wildcatter's watts): 1 W = 1 FE/t is settled
  (ELECTRICAL_MODEL, decisions table) and is not reopened.

## Test machine list (by draw, from small to large; draws from Mekanism's defaults, *unverified* until checked in its 26.3 config)
Energized Smelter, Enrichment Chamber, Metallurgic Infuser (small); Electrolytic Separator, Digital Miner (large); a Basic Energy Cube
as a buffer.

## Done when
- The 9 checks pass as smoke scenes on NeoForge with the released Mekanism jar.
- PROGRESS.md's 9a and 9c known gaps are cleared; the Field Manual's plugs page names Mekanism as tried.
- The Idea is struck from ROADMAP.md.
