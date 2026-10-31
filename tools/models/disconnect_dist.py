"""12.47 kV three-phase vertical-break disconnect, 1 x 5 x 3 (docs/multiblocks/substation.md), at true size.

A galvanized structure; per phase a hinge insulator at the west and a jaw insulator at the east with the blade between them
(tools/models/rotors.py: it lifts about its hinge, pivot x 0.15, 4.3 m up). The operating pipe down to the handle at the foot,
and the jumpers down to the bus's height at each end.
"""
import yardlib as y

M = y.model("disconnect_dist", 1, 5, 3, "galv")
TOP = 3.6

for z in (0.15, 2.85):
	y.column(M, 0.5, z, 0.0, TOP, width=0.2)
y.beam(M, 0.05, 0.95, 0.05, 2.95, TOP - 0.25, TOP)
for z in y.DIST_PHASES:
	for x in (0.15, 0.85):
		top = y.dist_post(M, x, z, TOP)
		M.box((x - 0.06, top, z - 0.04), (x + 0.06, top + 0.05, z + 0.04), "galv")
	M.box((0.8, 4.05, z - 0.05), (0.9, 4.3, z - 0.035), "copper")   # the jaw's fingers
	M.box((0.8, 4.05, z + 0.035), (0.9, 4.3, z + 0.05), "copper")
	for x, xa, xb in ((0.03, 0.0, 0.15), (0.97, 0.85, 1.0)):
		M.cylinder("y", (x, z), 0.02, y.DIST_Y, 4.06, "steel", 6)
		M.cylinder("x", (4.06, z), 0.02, xa, xb, "steel", 6)
	M.cylinder("x", (y.DIST_Y, z), 0.035, 0.0, 0.06, "steel", 6)    # the bus tube's end fittings at each side
	M.cylinder("x", (y.DIST_Y, z), 0.035, 0.94, 1.0, "steel", 6)
M.box((0.12, TOP + 0.02, 0.4), (0.18, TOP + 0.05, 2.6), "galv")   # the linkage turning the hinges
M.cylinder("y", (0.3, 0.3), 0.025, 0.9, TOP - 0.25, "galv", 6)    # the operating pipe
M.box((0.2, 0.8, 0.25), (0.45, 0.9, 0.35), "galv")
M.box((0.25, 0.85, 0.32), (0.3, 0.95, 0.55), "rubber")

for z in (0.15, 2.85):
	M.collide((0.38, 0, z - 0.12), (0.62, TOP, z + 0.12))
M.collide((0.05, TOP - 0.25, 0.05), (0.95, TOP, 2.95))
