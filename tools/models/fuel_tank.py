"""Sub-base fuel tank, 2,000 L, under the 500 kW diesel skid (docs/multiblocks/genset.md), at true size: 5 x 2 x 1.

A double-wall tank about 0.6 m deep under the whole skid, with the skid's base rails on top and an anti-vibration mount under
each foot of the radiator, engine, alternator and panel. Fittings on the front: a lockable fill cap, a level gauge, a vent
and the leak-detection port for the space between the walls; the supply and return lines rise to the engine.
"""
import gensetlib as g

M = g.model("fuel_tank", 5, 1, "grey")
TOP = 0.6   # the tank's top; the rails sit on it

# the tank: inner wall, outer wall seams, the stiffening ribs round it
M.box((0.04, 0.0, 0.08), (4.96, TOP, 1.92), "grey")
for x in (0.04, 1.0, 2.0, 3.0, 4.0, 4.96):
	M.box((x - 0.025, 0.02, 0.06), (x + 0.025, TOP - 0.02, 0.08), "grey")
	M.box((x - 0.025, 0.02, 1.92), (x + 0.025, TOP - 0.02, 1.94), "grey")
M.box((0.02, 0.0, 0.06), (4.98, 0.06, 1.94), "iron")   # the bottom skid lip
# base rails: two channels the length of the skid, cross members between
for z in (0.22, 1.6):
	M.box((0.0, TOP, z), (5.0, TOP + 0.2, z + 0.18), "iron")
	M.box((0.0, TOP + 0.18, z - 0.03), (5.0, TOP + 0.22, z + 0.21), "iron")
	for x in (0.25, 1.25, 2.25, 3.25, 4.25):   # lifting holes in the rail's web
		M.box((x, TOP + 0.06, z - 0.005), (x + 0.12, TOP + 0.14, z), "dark")
for x in (0.1, 1.0, 2.0, 3.0, 4.0, 4.9):
	M.box((x - 0.05, TOP, 0.4), (x + 0.05, TOP + 0.12, 1.6), "iron")
# anti-vibration mounts under every foot: radiator, engine, alternator, panel (their feet stand on y = 1)
for x in (0.5, 1.4, 2.6, 3.5, 4.5):
	for z in (0.31, 1.69):
		g.mount(M, x, z, top=1.0)
		M.box((x - 0.1, TOP + 0.22, z - 0.1), (x + 0.1, 0.85, z + 0.1), "galv")   # the pad under the mount
# fill cap with its lockable cover, front right; vent pipe with a mushroom cap; level gauge and leak port
M.cylinder("y", (4.6, 0.25), 0.07, TOP, TOP + 0.08, "galv", 12)
M.cylinder("y", (4.6, 0.25), 0.05, TOP + 0.08, TOP + 0.12, "brass", 12)
M.pipe([(4.3, TOP, 0.18), (4.3, 0.95, 0.18)], 1.5, "galv")
M.cylinder("y", (4.3, 0.18), 0.05, 0.95, 0.99, "galv", 8)
M.cylinder("z", (3.9, 0.35), 0.075, 0.03, 0.08, "galv", 12)
M.decal((3.84, 0.29, 0.025), (3.96, 0.41, 0.03), "gauge", "north", "galv")
M.cylinder("y", (0.4, 0.2), 0.03, TOP, TOP + 0.06, "brass", 8)
M.decal((2.2, 0.2, 0.055), (2.8, 0.45, 0.06), "nameplate", "north", "steel")
# supply and return lines up to the engine's fuel pump
M.pipe([(1.6, TOP, 0.75), (1.6, 0.98, 0.75)], 0.5, "steel")
M.pipe([(1.75, TOP, 0.75), (1.75, 0.98, 0.75)], 0.5, "steel")

M.collide((0, 0, 0.06), (5, 1.0, 1.94))
