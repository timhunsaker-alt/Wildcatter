"""Base frame of the 1 MW gas genset (docs/multiblocks/genset.md), at true size: 7 x 2 x 1.

A fabricated steel frame about 0.4 m deep: two I-beam rails the length of the skid and cross members, with pedestals under
the radiator, engine, alternator and panel feet (on anti-vibration mounts, their tops 1 m up), and a lube-oil make-up tank
between the rails under the alternator end, with its sight glass and fill.
"""
import gensetlib as g

M = g.model("genset_base_frame", 7, 1, "iron")
RAIL = 0.4

for z in (0.2, 1.62):
	M.box((0.0, 0.0, z - 0.04), (7.0, 0.04, z + 0.22), "iron")           # bottom flange
	M.box((0.0, 0.04, z + 0.07), (7.0, RAIL - 0.04, z + 0.11), "iron")   # web
	M.box((0.0, RAIL - 0.04, z - 0.04), (7.0, RAIL, z + 0.22), "iron")   # top flange
	for x in (0.3, 1.3, 2.3, 3.3, 4.3, 5.3, 6.3):   # web stiffeners
		M.box((x - 0.012, 0.04, z - 0.03), (x + 0.012, RAIL - 0.04, z + 0.21), "iron")
for x in (0.08, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 6.92):
	M.box((x - 0.06, 0.1, 0.4), (x + 0.06, RAIL - 0.05, 1.62), "iron")
# pedestals and mounts: radiator, the V20's four feet pairs, alternator, panel
for x in (0.5, 1.5, 2.5, 3.5, 4.5, 5.5, 6.5):
	for z in (0.31, 1.73):
		M.box((x - 0.12, RAIL, z - 0.11), (x + 0.12, 0.85, z + 0.11), "grey")
		M.box((x - 0.14, 0.83, z - 0.13), (x + 0.14, 0.85, z + 0.13), "grey")
		g.mount(M, x, z, top=1.0)
# lube-oil make-up tank between the rails, its sight glass on the front and the fill on top
M.box((4.7, 0.08, 0.5), (6.3, 0.36, 1.5), "grey")
M.box((5.0, 0.12, 0.495), (5.06, 0.32, 0.5), "dark")
M.cylinder("y", (6.0, 0.8), 0.05, 0.36, 0.42, "brass", 8)
M.pipe([(5.0, 0.36, 1.0), (5.0, 0.98, 1.0)], 0.75, "steel")   # make-up line up to the engine's sump
M.decal((2.7, 0.12, 0.155), (3.3, 0.32, 0.16), "nameplate", "north", "steel")
# lifting eyes at the four corners
for x in (0.05, 6.85):
	for z in (0.14, 1.86):
		M.box((x, 0.2, z - 0.02), (x + 0.1, 0.36, z + 0.02), "galv")

M.collide((0, 0, 0.16), (7, 1.0, 1.84))
