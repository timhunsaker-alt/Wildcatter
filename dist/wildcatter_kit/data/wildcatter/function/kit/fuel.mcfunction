# 7g: the fuel plant (docs/multiblocks/fuel.md): /function wildcatter:kit/fuel
# The line, each part against the next: autoclave, IDR kiln, granulator, pellet press, sintering furnace, pellet grinder, rod loader,
# rod welder, assembly fixture. Hydrogen (a steam reformer or the chlor-alkali cell) to the east sides of the kiln and the furnace.
# The helium recovery unit goes on a gas plant's pipeline gas, its front against the pipe.
give @s wildcatter:uf6_autoclave
give @s wildcatter:idr_kiln
give @s wildcatter:granulator
give @s wildcatter:pellet_press
give @s wildcatter:sintering_furnace
give @s wildcatter:pellet_grinder
give @s wildcatter:rod_loader
give @s wildcatter:rod_welder
give @s wildcatter:assembly_fixture
give @s wildcatter:helium_recovery_unit
give @s wildcatter:steam_reformer
give @s wildcatter:busway_industrial 64
give @s wildcatter:cable_industrial[wildcatter:conductor_size="kcmil_500"] 16
give @s wildcatter:gas_line 64
give @s minecraft:hopper 8
give @s wildcatter:leu_cylinder_30b 1
give @s wildcatter:zircaloy_tube 64
give @s wildcatter:guide_thimble 25
give @s wildcatter:spacer_grid 6
give @s wildcatter:steel_ingot 4
give @s wildcatter:helium_cylinder 1
give @s wildcatter:empty_helium_cylinder 2
give @s wildcatter:fuel_assembly 1
tellraw @s {"text":"Wildcatter fuel kit given. About 0.6 MW at 480 V: start the sets before connecting the line.","color":"gold"}
