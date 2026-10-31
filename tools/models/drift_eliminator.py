"""Cooling tower drift eliminators and hot-water distribution, 4 x 4 x 1 (docs/multiblocks/cooling-tower.md), at true size.

Inside the casing: the hot-water header and its laterals with spray nozzles over the fill, and above them the chevron
drift eliminators that catch the spray. The hot-water riser comes in through the east wall to the header.
"""
import towerlib as t

M = t.model("drift_eliminator", 1, "polymer")

t.casing(M, 0.0, 1.0)
t.posts(M, 0.0, 1.0)
t.ladder(M, 0.0, 1.0)
t.riser(M, 0.0, 0.3)
M.pipe([(3.94, 0.3, 2.0), (3.94, 0.36, 2.0), (2.0, 0.36, 2.0)], 4, "padgreen")   # into the header
M.cylinder("x", (0.36, 2.0), 0.08, 0.3, 3.7, "padgreen", 10)   # the header across the middle
for z in (0.6, 1.2, 1.8, 2.2, 2.8, 3.4):   # laterals with their nozzles
	M.box((0.3, 0.27, z - 0.025), (3.7, 0.32, z + 0.025), "polymer")
	for x in (0.6, 1.2, 1.8, 2.4, 3.0, 3.4):
		M.box((x - 0.02, 0.2, z - 0.02), (x + 0.02, 0.27, z + 0.02), "dark")
# the chevron eliminator packs above
for k in range(16):
	z = 0.25 + k * 0.22
	M.box((0.2, 0.5, z - 0.005), (3.8, 0.85, z + 0.005), "polymer", rot=("x", 30 if k % 2 else -30, (2.0, 0.675, z)))

M.collide((0.06, 0, 0.06), (3.94, 1.0, 3.94))
