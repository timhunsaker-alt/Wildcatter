"""12.47 kV three-pole feeder breaker, 2 x 4 x 3 (docs/multiblocks/substation.md), at true size: a 15 kV outdoor breaker on its
stand. The tank and mechanism cabinet on four legs, three bushings on the line side (west) and three on the load side (east),
the jumpers from the disconnect's height, the open/close indicator, the nameplate."""
import yardlib as y

M = y.model("dist_breaker_3p", 2, 4, 3, "grey")
B1, B2 = 1.3, 2.6   # the cabinet's bottom and top

for x in (0.3, 1.7):
	for z in (0.35, 2.65):
		M.box((x - 0.05, 0.0, z - 0.05), (x + 0.05, B1, z + 0.05), "galv")
		M.box((x - 0.1, 0.0, z - 0.1), (x + 0.1, 0.03, z + 0.1), "galv")
M.box((0.2, B1, 0.25), (1.8, B2, 2.75), "grey")
M.box((0.18, B2, 0.23), (1.82, B2 + 0.05, 2.77), "grey")
M.box((0.995 - 0.6, 1.4, 0.24), (0.995 + 0.6, 2.5, 0.25), "grey")   # the front door
M.box((1.55, 1.85, 0.21), (1.6, 2.05, 0.24), "dark")
M.decal((0.5, 2.15, 0.235), (1.0, 2.4, 0.24), "nameplate", "north", "steel")
M.box((0.6, 1.6, 0.235), (0.8, 1.75, 0.24), "padgreen")              # the indicator: green, open
for z in y.DIST_PHASES:
	for x in (0.45, 1.55):
		top = y.bushing(M, x, z, B2 + 0.05, 0.75, 0.05, 5, 0.09)
		M.box((x - 0.04, top, z - 0.02), (x + 0.04, top + 0.06, z + 0.02), "steel")
	M.cylinder("x", (y.DIST_Y, z), 0.025, 0.0, 0.45, "steel", 6)       # in from the disconnect
	M.cylinder("x", (y.DIST_Y, z), 0.025, 1.55, 2.0, "steel", 6)       # out to the feeder
M.pipe([(1.0, B1, 2.4), (1.0, 0.1 + 0.017, 2.4)], 1, "galv")         # control conduit down to the trench

M.collide((0.2, 0, 0.25), (1.8, B2 + 0.05, 2.75))
