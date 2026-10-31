"""Superheater and rear outlet section, 1 x 3 x 4 (docs/multiblocks/steam-plant.md), at true size.

The package boiler's rear section: the casing over the superheater bank where the flue gas turns out, the breeching and
the stack stub on top, the superheater's outlet header with its own safety valve, and the main steam stop valve. The main
steam line, lagged, runs down the south side into the turbine below it (its inlet at x 0.4 here, 1.75 m up).
"""
import plantlib as p

M = p.model("superheater", 1, 4, 3, "blue")
TOP = 3.15
HX, HY = 0.45, 3.4   # the outlet header

p.skid(M, 0.0, 1.0, 0.1, 2.9)
M.box((0.0, 0.22, 0.12), (0.88, TOP, 2.88), "blue")
M.box((0.0, TOP, 0.1), (0.9, TOP + 0.04, 2.9), "blue")
for z in (0.12, 1.0, 2.0, 2.86):
	M.box((0.86, 0.25, z), (0.9, TOP - 0.02, z + 0.02), "blue")   # rear-wall stiffeners
M.box((0.88, 0.6, 1.1), (0.92, 1.5, 1.9), "blue")                 # rear access door
for z in (1.14, 1.86):
	for y in (0.66, 1.44):
		M.box((0.92, y - 0.015, z - 0.015), (0.94, y + 0.015, z + 0.015), "galv")
# breeching and stack stub, with its rain cap
M.box((0.08, TOP + 0.04, 0.3), (0.82, TOP + 0.3, 1.3), "iron")
M.cylinder("y", (0.45, 0.8), 0.32, TOP + 0.3, 3.9, "iron", 16)
M.ring("y", (0.45, 0.8), 0.33, 3.6, 3.64, 0.02, "iron", sides=16)
M.cylinder("y", (0.45, 0.8), 0.36, 3.94, 3.98, "iron", 16)
# steam in from the drum, the outlet header, its safety valve and thermometer
M.pipe([(0.09, 3.85, 1.85), (HX, 3.85, 1.85), (HX, HY + 0.1, 1.85)], 6, "lagging")
M.cylinder("z", (HX, HY), 0.12, 1.5, 2.65, "lagging", 12)
M.cylinder("y", (HX, 2.3), 0.05, HY + 0.1, HY + 0.3, "steel", 8)
M.cylinder("y", (HX, 2.3), 0.035, HY + 0.3, 3.95, "brass", 8)
M.cylinder("x", (HY, 2.0), 0.05, HX + 0.12, HX + 0.2, "galv", 10)
M.decal((HX + 0.2, HY - 0.04, 1.96), (HX + 0.205, HY + 0.04, 2.04), "gauge", "east", "galv")
# the main steam stop valve and the line down the south side into the turbine below
M.pipe([(HX, HY, 2.65), (HX, HY, 2.75), (HX, 1.75, 2.75), (HX, 1.75, 2.9)], 6, "lagging")
M.box((HX - 0.14, 2.6, 2.62), (HX + 0.14, 2.9, 2.88), "steel")   # stop valve body
M.cylinder("x", (2.75, 2.75), 0.04, HX + 0.14, HX + 0.4, "steel", 8)   # its stem
M.cylinder("x", (2.75, 2.75), 0.14, HX + 0.4, HX + 0.43, "rubber", 12) # handwheel
M.flange("z", (HX, 1.75), 0.15, 2.92, 0.03, "steel", bolts=8, bolt_tex="galv")

M.collide((0, 0, 0.1), (0.92, TOP + 0.04, 2.9))
M.collide((0.08, TOP, 0.3), (0.82, 3.98, 1.3))
