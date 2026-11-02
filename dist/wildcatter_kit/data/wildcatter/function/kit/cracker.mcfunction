# Wildcatter cracker kit: /function wildcatter:kit/cracker
# LPG -> cracking furnace -> quench tower -> cracked gas compressor -> cold box (refrigeration compressor against it) -> polyethylene reactor + pelletizer -> plastic sheets.
give @s wildcatter:cracking_furnace
give @s wildcatter:quench_tower
give @s wildcatter:cracked_gas_compressor
give @s wildcatter:cold_box
give @s wildcatter:refrigeration_compressor
give @s wildcatter:polyethylene_reactor
give @s wildcatter:pelletizer
give @s wildcatter:lpg_bullet
give @s wildcatter:pump
give @s wildcatter:pipe 32
give @s wildcatter:gas_line 128
give @s minecraft:hopper
tellraw @s {"text":"Wildcatter cracker kit given. Fuel gas from the cold box back to the furnace's east side; water pipe from a pump to its west side.","color":"gold"}
