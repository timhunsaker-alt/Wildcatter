# 8a: the boron plant (docs/multiblocks/boron.md): /function wildcatter:kit/boron
# The dissolver (borax on top, sulfuric acid to the east side of its bottom, pumped water) with the crystallizer against it; the
# B4C furnace (boric acid and coal on top) and the RCCA bench (9 trays and steel on top) stand on their own. Hoppers under each.
give @s wildcatter:borax_dissolver
give @s wildcatter:boric_acid_crystallizer
give @s wildcatter:b4c_furnace
give @s wildcatter:rcca_bench
give @s wildcatter:borax 2
give @s minecraft:coal 16
give @s wildcatter:steel_ingot 4
give @s minecraft:hopper 8
give @s wildcatter:busway_industrial 32
give @s wildcatter:gas_line 32
tellraw @s {"text":"Wildcatter boron kit given. About 250 kW at 480 V; sulfuric acid from the acid plant.","color":"gold"}
