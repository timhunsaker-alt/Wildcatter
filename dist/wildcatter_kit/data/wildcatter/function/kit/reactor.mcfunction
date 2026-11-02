# 8a: the nuclear island (docs/multiblocks/reactor.md): /function wildcatter:kit/reactor
# The vessel with the CRDM head on top; a steam generator against its north and south faces, a coolant pump against each; the
# pressurizer against a steam generator; the CVCS against the vessel's west face (boric acid on top, pumped water); the console
# against the vessel. Aux feed pump piped to the east side of each steam generator's bottom. Pumps 4.16 kV, the rest 480 V.
give @s wildcatter:reactor_vessel
give @s wildcatter:crdm_head
give @s wildcatter:reactor_console
give @s wildcatter:steam_generator 2
give @s wildcatter:reactor_coolant_pump 2
give @s wildcatter:pressurizer
give @s wildcatter:cvcs_skid
give @s wildcatter:aux_feed_pump
give @s wildcatter:fuel_assembly 9
give @s wildcatter:rcca 5
give @s wildcatter:boric_acid 12
give @s wildcatter:unit_substation
give @s wildcatter:cable_mv 64
give @s wildcatter:busway_industrial 32
give @s wildcatter:cable_industrial 32
give @s wildcatter:gas_line 32
give @s minecraft:hopper 2
tellraw @s {"text":"Wildcatter reactor kit given. Load fuel and RCCAs while cold, fill through the CVCS, heat on the pumps, then set a power on the console.","color":"gold"}
