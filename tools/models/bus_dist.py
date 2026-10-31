"""12.47 kV station bus, 1 x 4 x 3 (docs/multiblocks/substation.md), at true size: aluminium tube on 15 kV post insulators on a
galvanized structure, the phases 1 m apart (15 kV class), carrying the transformer's low side east to the disconnect."""
import yardlib as y

M = y.model("bus_dist", 1, 4, 3, "galv")
TOP = 3.2

for z in (0.15, 2.85):
	y.column(M, 0.5, z, 0.0, TOP, width=0.2)
y.beam(M, 0.25, 0.75, 0.05, 2.95, TOP - 0.25, TOP)
for z in y.DIST_PHASES:
	y.dist_post(M, 0.5, z, TOP)
	y.tube(M, "x", (y.DIST_Y, z), 0.0, 1.0, 0.035)
	M.box((0.44, y.DIST_Y - 0.06, z - 0.05), (0.56, y.DIST_Y - 0.03, z + 0.05), "galv")
M.box((0.45, 0.0, 0.05), (0.55, 0.05, 0.12), "copper")

for z in (0.15, 2.85):
	M.collide((0.38, 0, z - 0.12), (0.62, TOP, z + 0.12))
M.collide((0.25, TOP - 0.25, 0.05), (0.75, TOP, 2.95))
