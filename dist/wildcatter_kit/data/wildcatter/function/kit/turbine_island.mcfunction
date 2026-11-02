# 8b: the turbine island (docs/multiblocks/turbine-island.md): /function wildcatter:kit/turbine_island
# In a line: valve chest, HP turbine (the MSR beside it), LP turbine, the 60 MVA generator, its 3,000 A breaker and bus duct to
# the GSU. The condenser against the LP turbine with two condensate pumps against it, the deaerator against one, two feed pumps
# against the deaerator, the HP heater against a feed pump. The CW pumps stand in a lake with their bottom 3 cells in the water.
give @s wildcatter:turbine_valve_chest
give @s wildcatter:hp_turbine
give @s wildcatter:moisture_separator_reheater
give @s wildcatter:lp_turbine
give @s wildcatter:tg_generator_60
give @s wildcatter:gen_breaker_3000
give @s wildcatter:gsu_60
give @s wildcatter:main_condenser
give @s wildcatter:condensate_pump 2
give @s wildcatter:deaerator
give @s wildcatter:main_feed_pump 2
give @s wildcatter:hp_feedwater_heater
give @s wildcatter:circulating_water_pump 2
give @s wildcatter:unit_auxiliary_transformer
give @s wildcatter:unit_substation
give @s wildcatter:bus_duct_gen 64
give @s wildcatter:cable_mv 64
give @s wildcatter:busway_industrial 32
give @s wildcatter:gas_line 128
tellraw @s {"text":"Wildcatter turbine island kit given. Start on outside power (a grid line or another generator), then the pumps one by one.","color":"gold"}
