# Wildcatter gas turbine kit: /function wildcatter:kit/gas_turbine
# Against the gas turbine: inlet house, generator, stack or HRSG. Breaker against the generator's back; bus duct to the GSU. Combined cycle: steam turbine against the HRSG, its generator and the condenser against it, feed pump against the HRSG.
give @s wildcatter:gas_turbine
give @s wildcatter:gt_inlet_house
give @s wildcatter:gt_generator
give @s wildcatter:gen_breaker 2
give @s wildcatter:gt_exhaust_stack
give @s wildcatter:hrsg
give @s wildcatter:steam_turbine_5
give @s wildcatter:st_generator
give @s wildcatter:air_cooled_condenser
give @s wildcatter:hrsg_feed_pump
give @s wildcatter:gsu_25
give @s wildcatter:load_bank 2
give @s wildcatter:bus_duct_gen 64
give @s wildcatter:cable_mv 32
give @s wildcatter:cable_industrial 64
give @s wildcatter:unit_substation
give @s wildcatter:stock_tank
give @s wildcatter:gas_line 32
tellraw @s {"text":"Wildcatter gas turbine kit given. The starter needs 4.16 kV at the west side; fuel (pipeline gas, diesel or kerosene) into the west side further back.","color":"gold"}
