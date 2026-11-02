# Wildcatter lease kit: /function wildcatter:kit/lease
# Pipe a wellhead to the separator's front end; gas, oil and water leave its back end on their own pipes.
give @s wildcatter:separator
give @s wildcatter:stock_tank 2
give @s wildcatter:water_tank
give @s wildcatter:flare_stack
give @s wildcatter:injection_pump
give @s wildcatter:disposal_wellhead
give @s wildcatter:gas_line 128
give @s wildcatter:pumpjack
give @s wildcatter:cable_industrial 64
give @s wildcatter:power_meter
tellraw @s {"text":"Wildcatter lease kit given. One product per steel pipe run: keep oil, gas and water apart. The disposal wellhead goes on a dry hole's casing.","color":"gold"}
