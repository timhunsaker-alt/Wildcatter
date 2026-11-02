# 7f: the zirconium plant (docs/multiblocks/zirconium.md): /function wildcatter:kit/zirconium
# The line, each part against the next: separator, chlorinator, hafnium column, Kroll reactor, vacuum distillation, VAR furnace,
# extrusion press (4.16 kV), pilger mill, anneal furnace. The magnesium cell against the Kroll reactor and the vacuum distillation,
# the magnesia chlorinator against the cell, a rectifier against each cell. One chlorine pipe joins both cells and both chlorinators.
give @s wildcatter:mineral_separator
give @s wildcatter:chlorinator
give @s wildcatter:hafnium_column
give @s wildcatter:kroll_reactor
give @s wildcatter:vacuum_distillation
give @s wildcatter:var_furnace
give @s wildcatter:extrusion_press
give @s wildcatter:pilger_mill
give @s wildcatter:vacuum_anneal_furnace
give @s wildcatter:magnesium_cell
give @s wildcatter:magnesia_chlorinator
give @s wildcatter:chlor_alkali_cell
give @s wildcatter:rectifier 2
give @s wildcatter:gas_line 64
give @s wildcatter:busway_industrial 64
give @s wildcatter:cable_industrial[wildcatter:conductor_size="kcmil_500"] 32
give @s wildcatter:cable_mv 64
give @s minecraft:hopper 8
give @s wildcatter:heavy_mineral 4
give @s minecraft:coal 16
give @s wildcatter:magnesia 2
give @s wildcatter:halite 2
give @s wildcatter:tin_ingot 1
give @s minecraft:iron_ingot 2
give @s wildcatter:chromite 1
tellraw @s {"text":"Wildcatter zirconium kit given. About 0.5 MW at 480 V (use busway down the line) and 0.4 MW at 4.16 kV for the rectifiers and press.","color":"gold"}
