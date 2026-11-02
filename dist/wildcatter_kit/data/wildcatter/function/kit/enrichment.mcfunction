# 7e: the centrifuge cascade (docs/multiblocks/enrichment.md): /function wildcatter:kit/enrichment
# One steel-pipe header joins the feed station's back, the converter's back, the take-offs' fronts and the tops of the centrifuges.
# 11 machines at least (one a stage); a converter drives 12 on 12.47 kV at 571 kW each: power it from the peaker kit's substation transformer.
give @s wildcatter:feed_station
give @s wildcatter:frequency_converter
give @s wildcatter:product_takeoff
give @s wildcatter:tails_takeoff
give @s wildcatter:centrifuge 12
give @s wildcatter:gas_line 64
give @s wildcatter:cable_dist[wildcatter:conductor_size="mv_500"] 64
give @s wildcatter:cable_industrial 64
give @s minecraft:hopper 6
give @s wildcatter:uf6_cylinder_48y 1
give @s wildcatter:empty_30b_cylinder 2
give @s wildcatter:empty_48y_cylinder 2
tellraw @s {"text":"Wildcatter cascade kit given. Run it from the peaker's 12.47 kV (kit/peaker); the stations take 480 V on the west of their front corner.","color":"gold"}
