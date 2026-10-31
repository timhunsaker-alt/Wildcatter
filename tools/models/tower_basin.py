"""Cooling tower cold-water basin, concrete, 4 x 4 x 1 (docs/multiblocks/cooling-tower.md), at true size.

The basin under the cell: floor and walls about 0.65 m high, the cold water in it, the sump with its outlet on the north side
(the water pipe connects to any cell of the basin), the casing's corner posts on their base plates, the foot of the ladder
and of the hot-water riser.
"""
import towerlib as t

M = t.model("tower_basin", 1, "concrete")
H = 0.65

M.box((0, 0, 0), (4, 0.15, 4), "concrete")
for a, b in (((0, 0.15, 0), (4, H, 0.18)), ((0, 0.15, 3.82), (4, H, 4)), ((0, 0.15, 0.18), (0.18, H, 3.82)), ((3.82, 0.15, 0.18), (4, H, 3.82))):
	M.box(a, b, "concrete")
M.box((0.18, 0.15, 0.18), (3.82, 0.5, 3.82), "dark")   # the cold water
M.box((1.6, 0.0, 0.0), (2.4, 0.2, 0.3), "concrete")     # the sump
M.cylinder("z", (2.0, 0.35), 0.12, 0.0, 0.05, "padgreen", 12)   # its outlet flange
for x in (t.IN, 4 - t.IN):
	for z in (t.IN, 4 - t.IN):
		M.box((x - 0.1, H, z - 0.1), (x + 0.1, H + 0.02, z + 0.1), "galv")
t.posts(M, H + 0.02, 1.0)
t.ladder(M, 0.2, 1.0)
t.riser(M, 0.0, 1.0)

M.collide((0, 0, 0), (4, H, 4))
