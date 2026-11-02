# Wildcatter gas plant kit: /function wildcatter:kit/gas_plant
# Separator gas -> compressor -> dehydrator -> chiller (refrigeration compressor against it) -> stabilizer. In at the front of each, out at the back.
give @s wildcatter:gas_compressor
give @s wildcatter:teg_dehydrator
give @s wildcatter:ngl_chiller
give @s wildcatter:refrigeration_compressor
give @s wildcatter:stabilizer
give @s wildcatter:lpg_bullet
give @s wildcatter:stock_tank
give @s wildcatter:flare_stack
give @s wildcatter:gas_line 128
give @s wildcatter:unit_substation
give @s wildcatter:cable_mv 32
give @s wildcatter:cable_industrial 64
tellraw @s {"text":"Wildcatter gas plant kit given. The compressor takes 4.16 kV (MV cable to its motor's side); the refrigeration compressor 480 V. Wet gas freezes in the chiller: dry it first.","color":"gold"}
