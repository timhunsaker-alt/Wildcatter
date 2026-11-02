# Wildcatter land rig kit: /function wildcatter:kit/land_rig
# The rig: put the substructure down first over the hole, then click the rest on its floor or the ground nearby.
give @s wildcatter:rig_substructure
give @s wildcatter:rig_mast
give @s wildcatter:rig_drawworks
give @s wildcatter:mud_pump 2
give @s wildcatter:mud_tanks
give @s wildcatter:rig_drive_house
give @s wildcatter:top_drive
# 4.16 kV to the drive house's west end
give @s wildcatter:cable_mv 64
# Power: a 500 kW diesel set (480 V), a unit substation run backward to 4.16 kV, cable between them
give @s wildcatter:fuel_tank
give @s wildcatter:radiator
give @s wildcatter:genset_diesel_engine
give @s wildcatter:alternator
give @s wildcatter:genset_control_panel
give @s wildcatter:exhaust_stack
give @s wildcatter:diesel_bucket 4
give @s wildcatter:unit_substation
give @s wildcatter:cable_industrial 32
# Mud water: a pump in water, piped to the mud tanks
give @s wildcatter:pump
give @s wildcatter:pipe 64
# Finding and reading
give @s wildcatter:seismograph
give @s wildcatter:power_meter
give @s wildcatter:field_manual
tellraw @s {"text":"Wildcatter land rig kit given. Find a DEEP ECHO with the seismograph, put the substructure over it, then the rest.","color":"gold"}
