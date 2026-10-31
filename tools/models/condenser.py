"""Side-exhaust surface condenser, 3 x 2 x 2 (docs/multiblocks/steam-plant.md), at true size.

Beside the turbine on its south side (Tim chose this over a condenser under a raised turbine deck): the shell, about 3 m
long and 1.2 m across, on its saddles, the exhaust inlet from the turbine's trunk on its north side, the water boxes at both
ends with the cooling-water nozzles (the water pipe connects to any cell), the hotwell under the shell with its level glass,
the condensate line west to the feed pump, and the air ejector on top.
"""
import plantlib as p

M = p.model("condenser", 3, 2, 2, "grey")
Y, Z, R = 1.05, 1.0, 0.6

# saddles and the shell
for x in (0.6, 2.4):
	M.box((x - 0.08, 0.0, Z - 0.5), (x + 0.08, Y - 0.35, Z + 0.5), "iron")
	M.box((x - 0.12, 0.0, Z - 0.55), (x + 0.12, 0.05, Z + 0.55), "iron")
M.cylinder("x", (Y, Z), R, 0.3, 2.7, "grey", 16)
for x in (0.3, 2.7):
	M.ring("x", (Y, Z), R + 0.01, x - 0.02, x + 0.02, 0.03, "grey", sides=16)
# water boxes, the cooling-water nozzles down to the floor at the west end
for x1, x2 in ((0.08, 0.3), (2.7, 2.92)):
	M.cylinder("x", (Y, Z), R - 0.04, x1, x2, "blue", 16)
	M.flange("x", (Y, Z), R + 0.02, x2 if x1 < 1 else x1, 0.03, "blue", bolts=16, bolt_tex="galv", sides=16)
M.pipe([(0.19, Y - 0.25, Z - 0.3), (0.19, 0.1 + 0.11, Z - 0.3)], 8, "blue")   # cooling water in
M.pipe([(0.19, Y + 0.25, Z + 0.3), (0.19, 1.85, Z + 0.3)], 8, "blue")          # and out, up and over
# the exhaust inlet from the turbine's trunk on the north side
M.box((1.72, 0.27, 0.0), (2.58, 1.08, 0.04), "iron")
M.box((1.75, 0.3, 0.04), (2.55, 1.05, Z - R + 0.15), "iron")
# the hotwell under the shell, its level glass and the condensate line west to the feed pump
M.box((0.75, 0.15, Z - 0.35), (2.25, Y - R + 0.05, Z + 0.35), "grey")
M.box((1.5, 0.18, Z - 0.37), (1.54, 0.42, Z - 0.35), "polymer")
M.pipe([(0.75, 0.3, 0.5), (0.05, 0.3, 0.5)], 3, "steel")
M.flange("x", (0.3, 0.5), 0.08, 0.02, 0.02, "steel", bolts=4, bolt_tex="galv")
M.pipe([(0.75, 0.3, 0.5), (0.9, 0.3, 0.5), (0.9, 0.3, Z - 0.3)], 3, "steel")
# the steam-jet air ejector on top, and the vacuum gauge
M.cylinder("x", (Y + R + 0.12, Z), 0.08, 1.0, 1.9, "steel", 10)
M.pipe([(1.45, Y + R - 0.05, Z), (1.45, Y + R + 0.05, Z)], 2, "steel")
M.cylinder("z", (2.0, Y + 0.1), 0.07, Z - R - 0.03, Z - R + 0.02, "galv", 10)
M.decal((1.945, Y + 0.045, Z - R - 0.035), (2.055, Y + 0.155, Z - R - 0.03), "gauge", "north", "galv")
M.decal((0.9, Y - 0.1, Z + R + 0.0), (1.4, Y + 0.12, Z + R + 0.005), "nameplate", "south", "steel")

M.collide((0.08, 0, Z - R), (2.92, Y + R + 0.2, Z + R))
M.collide((1.72, 0.27, 0.0), (2.58, 1.08, Z - R))
