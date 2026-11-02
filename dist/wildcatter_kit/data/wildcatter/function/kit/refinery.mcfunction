# Wildcatter refinery kit: /function wildcatter:kit/refinery
# Early topping unit: the column, with the heater and the skid against it. Late crude unit: the column, heater, strippers and condenser against it, the charge pump against the heater, the desalter against the pump.
give @s wildcatter:topping_column
give @s wildcatter:topping_heater
give @s wildcatter:topping_skid
give @s wildcatter:atmospheric_column
give @s wildcatter:crude_heater
give @s wildcatter:side_strippers
give @s wildcatter:overhead_condenser
give @s wildcatter:crude_charge_pump
give @s wildcatter:desalter
give @s wildcatter:stock_tank 6
give @s wildcatter:lpg_bullet
give @s wildcatter:gas_line 128
give @s wildcatter:cable_industrial 64
give @s wildcatter:cable_mv 32
tellraw @s {"text":"Wildcatter refinery kit given. Crude into the skid (or desalter); products off the column, strippers and condenser. Read the column for what is missing.","color":"gold"}
