# 8c: the natural-draft cooling tower (docs/multiblocks/natural-draft-tower.md): /function wildcatter:kit/cooling_tower
# Place the controller facing where the tower should stand: its centre is 34 m in front, and it is 88 m tall and about 64 m across.
# Feed it by a hopper on top. It needs about 430 gray concrete, 44 steel ingots and 64 plastic sheets in all.
give @s wildcatter:natural_draft_tower
give @s minecraft:hopper 2
give @s minecraft:gray_concrete 448
give @s wildcatter:steel_ingot 48
give @s wildcatter:plastic_sheet 64
give @s wildcatter:circulating_water_pump 2
tellraw @s {"text":"Wildcatter cooling tower kit given. The basin, columns, 54 lifts and fill take about 16 minutes once fed.","color":"gold"}
