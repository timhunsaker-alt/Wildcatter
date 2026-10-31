"""138 kV three-pole dead-tank SF6 breaker, 2 x 5 x 9 (docs/multiblocks/substation.md), at true size.

Three grounded tanks along the line on a galvanized frame, one per phase (3 m apart), each with its two bushings rising to
about 5 m (the line side west, the load side east) and the current transformers at their feet; the gas-density gauges, the
spring operating mechanism's cabinet at the south end, its control conduit, and the ground pads.
"""
import yardlib as y

M = y.model("hv_breaker_3p", 2, 5, 9, "grey")
TY = 2.3   # the tanks' axis

# the frame: legs at the corners, beams along both sides and across under each tank
for x in (0.2, 1.8):
	for z in (0.4, 8.6):
		M.box((x - 0.08, 0.0, z - 0.08), (x + 0.08, 1.8, z + 0.08), "galv")
		M.box((x - 0.15, 0.0, z - 0.15), (x + 0.15, 0.04, z + 0.15), "galv")
	M.box((x - 0.06, 1.7, 0.3), (x + 0.06, 1.82, 8.7), "galv")
for z in y.HV_PHASES:
	M.box((0.1, 1.7, z - 0.06), (1.9, 1.82, z + 0.06), "galv")
for z in y.HV_PHASES:
	# the tank on its saddles, its dished ends
	for x in (0.45, 1.55):
		M.box((x - 0.06, 1.82, z - 0.3), (x + 0.06, TY - 0.3, z + 0.3), "grey")
	M.cylinder("x", (TY, z), 0.42, 0.2, 1.8, "grey", 16)
	M.cylinder("x", (TY, z), 0.3, 0.12, 0.2, "grey", 16)
	M.cylinder("x", (TY, z), 0.3, 1.8, 1.88, "grey", 16)
	for x in (0.6, 1.0, 1.4):
		M.ring("x", (TY, z), 0.43, x, x + 0.025, 0.02, "grey", sides=16)
	# the bushings, line side and load side, with their current transformers and terminal pads
	for x in (0.45, 1.55):
		M.cylinder("y", (x, z), 0.2, TY + 0.38, TY + 0.62, "grey", 12)   # CT housing
		top = y.bushing(M, x, z, TY + 0.62, 1.9, 0.08, 14, 0.15)
		M.box((x - 0.08, top, z - 0.02), (x + 0.08, top + 0.12, z + 0.02), "steel")
	# gas-density gauge on the tank
	M.cylinder("z", (1.0, TY - 0.15), 0.05, z - 0.48, z - 0.42, "galv", 10)
	M.decal((0.96, TY - 0.19, z - 0.485), (1.04, TY - 0.11, z - 0.48), "gauge", "north", "galv")
# the operating mechanism cabinet at the south end, and its conduit to each pole
M.box((0.25, 0.4, 8.35), (1.75, 1.65, 8.95), "grey")
M.box((0.95, 0.5, 8.95), (1.05, 1.55, 8.97), "dark")                      # the door seam
M.box((0.3, 1.0, 8.95), (0.36, 1.15, 8.99), "dark")                       # handle
M.decal((1.15, 1.2, 8.95), (1.65, 1.45, 8.955), "nameplate", "south", "steel")
M.cylinder("x", (1.55, 8.65), 0.03, 0.4, 1.6, "galv", 6)                   # the open/close indicator shaft
M.pipe([(1.0, 1.65, 8.5), (1.0, 1.95, 8.5), (1.0, 1.95, 1.5)], 1, "galv")
M.decal((0.5, 0.8, 8.345), (1.5, 1.2, 8.35), "danger", "north", "grey")
# ground pads on two legs
for z in (0.4, 8.6):
	M.box((0.05, 0.3, z - 0.05), (0.12, 0.4, z + 0.05), "copper")

M.collide((0.1, 0, 0.3), (1.9, 1.82, 8.7))
for z in y.HV_PHASES:
	M.collide((0.12, 1.82, z - 0.42), (1.88, TY + 0.62, z + 0.42))
