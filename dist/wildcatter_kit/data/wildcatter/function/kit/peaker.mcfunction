# Wildcatter peaker kit: /function wildcatter:kit/peaker
# Against the aero turbine: inlet house (front), generator (east side), exhaust stack (back). 3,000 A breaker against the generator's back; 3,000 A bus duct to the 60 MVA GSU; 138 kV line to the 60 MVA substation transformer.
give @s wildcatter:aero_turbine
give @s wildcatter:aero_inlet_house
give @s wildcatter:aero_generator
give @s wildcatter:aero_exhaust_stack
give @s wildcatter:gen_breaker_3000
give @s wildcatter:gsu_60
give @s wildcatter:substation_transformer_60
give @s wildcatter:load_bank 4
give @s wildcatter:bus_duct_gen[wildcatter:conductor_size="bus_duct_3000"] 64
give @s wildcatter:cable_hv 32
give @s wildcatter:cable_dist 16
give @s wildcatter:cable_mv 32
give @s wildcatter:unit_substation
give @s wildcatter:stock_tank
give @s wildcatter:gas_line 32
tellraw @s {"text":"Wildcatter peaker kit given. The starter needs 4.16 kV at the west side near the front; fuel (pipeline gas, diesel or kerosene) into the west side further back.","color":"gold"}
