# 9a: receptacles and plug adapters (ELECTRICAL_MODEL.md): /function wildcatter:kit/plugs
# Wire a receptacle like any load point (hot and neutral), set the plug of the SAME type against it, and put another mod's
# machine, battery or cable beside the plug (Tech Reborn on Fabric). The starter plant: a boiler on a campfire, water by bucket.
give @s wildcatter:receptacle_nema_5_15
give @s wildcatter:plug_nema_5_15
give @s wildcatter:receptacle_nema_5_20
give @s wildcatter:plug_nema_5_20
give @s wildcatter:receptacle_nema_14_50
give @s wildcatter:plug_nema_14_50
give @s wildcatter:receptacle_cee_7
give @s wildcatter:plug_cee_7
give @s wildcatter:receptacle_iec_60309_blue
give @s wildcatter:plug_iec_60309_blue
give @s wildcatter:receptacle_nema_l21_30
give @s wildcatter:plug_nema_l21_30
give @s wildcatter:receptacle_nema_l22_30
give @s wildcatter:plug_nema_l22_30
give @s wildcatter:receptacle_iec_60309_red_32
give @s wildcatter:plug_iec_60309_red_32
give @s wildcatter:receptacle_iec_60309_red_63
give @s wildcatter:plug_iec_60309_red_63
give @s wildcatter:boiler
give @s wildcatter:steam_turbine
give @s minecraft:campfire
give @s minecraft:water_bucket
give @s wildcatter:breaker 2
give @s wildcatter:wool_wire_hot 32
give @s wildcatter:wool_wire_neutral 32
give @s wildcatter:electric_lamp
tellraw @s {"text":"Wildcatter plugs kit given. Plugs feed Team Reborn Energy machines at 1 W = 1 E/t.","color":"gold"}
