"""138 kV three-phase centre-break disconnect, 2 x 6 x 9 (docs/multiblocks/substation.md), at true size.

A galvanized lattice structure about 4 m high across the line; on its beam, per phase, two rotating insulator stacks 1.4 m
apart whose arms meet in the middle (tools/models/rotors.py: the arms turn about their stacks, pivots at x 0.3 and 1.7,
5.55 m up). The interphase linkage, the vertical operating pipe down to the handle at the foot of the north column, and the
terminal pads at each end where the bus tube joins.
"""
import yardlib as y

M = y.model("disconnect_hv", 2, 6, 9, "galv")
TOP = 4.0

for z in (0.35, 8.65):
	y.column(M, 1.0, z, 0.0, TOP)
y.beam(M, 0.15, 1.85, 0.2, 8.8, TOP - 0.4, TOP)
for z in y.HV_PHASES:
	for x in (0.3, 1.7):
		M.cylinder("y", (x, z), 0.13, TOP, TOP + 0.08, "galv", 12)   # the rotating base bearing
		y.hv_post(M, x, z, TOP + 0.08)
		M.cylinder("y", (x, z), 0.06, TOP + 1.58, y.HV_Y - 0.03, "galv", 10)   # the arm's hub sits on this
	# terminal pads at each end, where the bus tube joins
	for x, xx in ((0.0, 0.3), (1.7, 2.0)):
		y.tube(M, "x", (y.HV_Y, z), x + (0.0 if x == 0 else 0.05), xx - (0.05 if x == 0 else 0.0), 0.04)
# interphase linkage: a pipe along the beam turning every phase's west stack
M.box((0.22, TOP + 0.02, 0.4), (0.28, TOP + 0.06, 8.6), "galv")
# the operating pipe down the north column to the handle, and the ground mat lead
M.cylinder("y", (0.6, 0.6), 0.03, 0.9, TOP - 0.4, "galv", 6)
M.box((0.5, 0.8, 0.55), (0.75, 0.9, 0.65), "galv")
M.box((0.55, 0.85, 0.62), (0.6, 0.95, 0.9), "rubber")   # the handle
M.box((0.45, 0.4, 0.5), (0.75, 0.8, 0.7), "grey")       # its padlock box
M.decal((0.46, 0.55, 0.495), (0.74, 0.75, 0.5), "danger", "north", "grey")
M.box((0.95, 0.0, 0.3), (1.05, 0.05, 0.4), "copper")

for z in (0.35, 8.65):
	M.collide((0.8, 0, z - 0.2), (1.2, TOP, z + 0.2))
M.collide((0.15, TOP - 0.4, 0.2), (1.85, TOP, 8.8))
