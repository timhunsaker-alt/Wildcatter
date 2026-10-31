"""Genset control panel and output breaker enclosure (docs/multiblocks/genset.md), at true size: 1 x 2 x 2.

A floor-standing steel enclosure at the east end of the skid, on the base's mounts. The north face is the controller: its
display and buttons in the upper door, with the breaker's rotary operating handle and the emergency stop below. The output
cables leave through the gland plate low on the east end, where the 480Y/277 V terminals are. Vents near the roof.
"""
import gensetlib as g

M = g.model("genset_control_panel", 1, 2, "grey")
X1, X2, Z1, Z2, TOP = 0.08, 0.96, 0.1, 1.95, 1.9

M.box((X1, 0.08, Z1), (X2, TOP, Z2), "grey")
M.box((X1 - 0.02, TOP, Z1 - 0.02), (X2 + 0.02, TOP + 0.03, Z2 + 0.02), "grey")   # drip roof
M.box((X1, 0.0, Z1 + 0.05), (X2, 0.08, Z2 - 0.05), "iron")                        # plinth
# north face: two doors with hinges and handles, the controller in the upper one
M.box((X1 + 0.02, 1.0, Z1 - 0.012), (X2 - 0.02, 1.02, Z1), "dark")   # the seam between the doors
for y0, y1 in ((0.12, 0.98), (1.04, TOP - 0.04)):
	for y in (y0 + 0.1, y1 - 0.1):
		M.box((X1 - 0.01, y - 0.04, Z1 - 0.02), (X1 + 0.02, y + 0.04, Z1), "galv")   # hinges
	M.box((X2 - 0.09, (y0 + y1) / 2 - 0.08, Z1 - 0.04), (X2 - 0.06, (y0 + y1) / 2 + 0.08, Z1), "dark")   # handle
M.decal((X1 + 0.1, 1.2, Z1 - 0.012), (X2 - 0.14, 1.72, Z1 - 0.004), "controller", "north", "grey")
M.decal((X1 + 0.18, 1.76, Z1 - 0.008), (X2 - 0.22, 1.84, Z1 - 0.003), "nameplate", "north", "steel")
# lower door: the breaker's rotary handle and the emergency stop
M.box((X1 + 0.25, 0.48, Z1 - 0.01), (X1 + 0.55, 0.78, Z1), "dark")
M.cylinder("z", (X1 + 0.4, 0.63), 0.08, Z1 - 0.04, Z1 - 0.01, "dark", 12)
M.box((X1 + 0.38, 0.55, Z1 - 0.07), (X1 + 0.42, 0.71, Z1 - 0.04), "rubber")
M.box((X1 + 0.6, 0.78, Z1 - 0.012), (X1 + 0.76, 0.94, Z1), "brass")              # yellow guard
M.cylinder("z", (X1 + 0.68, 0.86), 0.05, Z1 - 0.05, Z1 - 0.012, "rubber", 12)       # emergency stop
M.decal((X1 + 0.2, 0.2, Z1 - 0.008), (X2 - 0.2, 0.4, Z1 - 0.003), "danger", "north", "grey")
# east end: the gland plate where the output cables leave, low (the terminals), and a danger sign
M.box((X2, 0.12, 0.3), (X2 + 0.03, 0.75, 1.7), "galv")
for z in (0.55, 0.85, 1.15, 1.45):
	M.cylinder("x", (0.4, z), 0.05, X2 + 0.03, 1.0, "brass", 10)
for y in (0.15, 0.72):
	for z in (0.33, 0.8, 1.2, 1.67):
		M.box((X2 + 0.03, y - 0.012, z - 0.012), (X2 + 0.04, y + 0.012, z + 0.012), "galv")
M.decal((X2, 1.3, 0.75), (X2 + 0.005, 1.6, 1.25), "danger", "east", "grey")
# vents under the roof on both long sides
for z, face in ((Z1 - 0.01, 0), (Z2, 1)):
	for k in range(4):
		y = 1.6 + k * 0.05
		M.box((X1 + 0.3, y, z), (X2 - 0.3, y + 0.02, z + 0.01), "dark")
# the cables in from the alternator, through the west side
for z in (0.82, 0.94, 1.06, 1.18):
	M.cylinder("x", (1.15, z), 0.035, 0.0, X1, "rubber", 8)

M.collide((X1, 0, Z1), (1.0, TOP + 0.03, Z2))
