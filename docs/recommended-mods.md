# Recommended mods to play with

Quality-of-life mods that work alongside Wildcatter on Minecraft 26.3. None of them is required. Every one has a 26.3 build on
Modrinth (checked 2026-11-03). The Fabric set ran the `coker` and `anodes` game-test scenes alongside Wildcatter: both passed,
and the plants rendered the same with Sodium as without it. The NeoForge set is the matching list but has not been played yet.

| Mod | What it gives Wildcatter | Fabric | NeoForge | Needs |
|---|---|---|---|---|
| **Performance** | | | | |
| [Sodium](https://modrinth.com/mod/sodium) | faster chunk rendering around the big plants | yes | yes | |
| [Lithium](https://modrinth.com/mod/lithium) | faster server ticks | yes | yes | |
| [FerriteCore](https://modrinth.com/mod/ferrite-core) | less memory for block states | yes | yes | |
| [EntityCulling](https://modrinth.com/mod/entityculling) | skips block entities you cannot see | yes | yes | |
| [ImmediatelyFast](https://modrinth.com/mod/immediatelyfast) | faster HUD and text drawing | yes | yes | |
| **Building** | | | | |
| [WorldEdit](https://modrinth.com/plugin/worldedit) | clear and level ground for the true-size plants | yes | yes | |
| [Litematica](https://modrinth.com/mod/litematica) | save a plant layout as a schematic and build from its ghost | yes | no | MaLiLib |
| **Information and maps** | | | | |
| [Jade](https://modrinth.com/mod/jade) | names the part you look at | yes | yes | |
| [JourneyMap](https://modrinth.com/mod/journeymap) | minimap and waypoints for wells, ores and plants | yes | yes | |
| [Mod Menu](https://modrinth.com/mod/modmenu) | the mods list and their settings | yes | built in | Placeholder API |
| [Controlling](https://modrinth.com/mod/controlling) | search key bindings | yes | yes | Searchables |
| [JEI](https://modrinth.com/mod/jei) | recipes and item lookup | yes | yes | Mezz Config |
| **Comfort** | | | | |
| [Mouse Tweaks](https://modrinth.com/mod/mouse-tweaks) | drag and scroll items between slots | yes | yes | |
| [AppleSkin](https://modrinth.com/mod/appleskin) | food values | yes | yes | Cloth Config (for its settings) |
| [Inventory Profiles Next](https://modrinth.com/mod/inventory-profiles-next) | inventory sorting | yes | yes | libIPN; on Fabric also Fabric Language Kotlin |
| [Shulker Box Tooltip](https://modrinth.com/mod/shulkerboxtooltip) | shows a shulker box's contents | yes | yes | |
| [Waystones](https://modrinth.com/mod/waystones) | travel between plants that are far apart | yes | yes | Balm, Shogi |
| [Chunky](https://modrinth.com/mod/chunky) | pre-generate the world | yes | yes | |

**Notes:**
- Fabric also needs Fabric API 0.165.2 or newer.
- Install mods only while Minecraft is closed. Replacing a jar under a running game breaks world loading until you restart.
- Iris shaders (needs Sodium) and Distant Horizons also have 26.3 builds. They are heavy and were left out.
