# 8e: fuel handling (docs/multiblocks/fuel-handling.md): /function wildcatter:kit/fuel_handling
# The refuelling machine against the vessel, the transfer system against the machine, the pool's controller against that (its
# pool is dug 6 x 6 x 12 in front of it). New assemblies in the machine's hopper; sneak-click it for REFUEL or DEFUEL at cold shutdown.
give @s wildcatter:refuelling_machine
give @s wildcatter:fuel_transfer_system
give @s wildcatter:spent_fuel_pool
give @s wildcatter:fuel_assembly 3
give @s wildcatter:dosimeter
give @s minecraft:hopper 2
tellraw @s {"text":"Wildcatter fuel handling kit given. Carry the dosimeter in your hotbar.","color":"gold"}
