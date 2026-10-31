"""Substation power transformer, 20 MVA 138/12.47 kV delta-wye, ONAN, 6 x 7 x 5 (docs/multiblocks/substation.md), at true size.

On its concrete pad with the oil-containment curb: the main tank, banks of panel radiators along both long sides, the three
138 kV bushings on the cover at the west (1 m apart, about 6.5 m to the terminals) with their surge arresters on brackets and
the jumpers from the bus, the three 12.47 kV bushings and the X0 neutral at the east with the jumpers to the low-side bus, the
conservator on its stand with the Buchholz relay in its pipe, the tap changer's compartment and the control cabinet on the
east end, the nameplate, the gauges and the ground pads.
"""
import yardlib as y

M = y.model("power_transformer", 6, 7, 5, "grey")
X1, X2, Z1, Z2 = 1.2, 4.8, 1.0, 4.0     # the tank
BASE, COVER = 0.3, 3.5
HV = (1.5, 2.5, 3.5)                    # bushing positions across (z)

# the pad and its containment curb
M.box((0.0, 0.0, 0.0), (6.0, 0.15, 5.0), "concrete")
for a, b in (((0.0, 0.15, 0.0), (6.0, 0.3, 0.12)), ((0.0, 0.15, 4.88), (6.0, 0.3, 5.0)), ((0.0, 0.15, 0.12), (0.12, 0.3, 4.88)), ((5.88, 0.15, 0.12), (6.0, 0.3, 4.88))):
	M.box(a, b, "concrete")
# the tank, its base, cover and stiffeners
M.box((X1 - 0.1, 0.15, Z1 - 0.1), (X2 + 0.1, BASE, Z2 + 0.1), "iron")
M.box((X1, BASE, Z1), (X2, COVER, Z2), "grey")
M.box((X1 - 0.05, COVER, Z1 - 0.05), (X2 + 0.05, COVER + 0.08, Z2 + 0.05), "grey")
for x in (X1 + 0.6, X1 + 1.8, X1 + 3.0):
	for z in (Z1 - 0.06, Z2):
		M.box((x - 0.05, BASE, z), (x + 0.05, COVER - 0.1, z + 0.06), "grey")
# radiator banks on both long sides
for z1, z2, zh in ((0.15, Z1, Z1 - 0.06), (Z2, 4.85, Z2 + 0.06)):
	for yh in (0.75, 3.0):
		M.cylinder("z", (2.0, yh), 0.07, min(z1, zh), max(z2, zh), "grey", 8)
		M.cylinder("z", (4.0, yh), 0.07, min(z1, zh), max(z2, zh), "grey", 8)
	M.fins((1.4, 0.6, z1 + 0.05), (4.6, 3.15, z2 - 0.05), "x", 22, 0.03, "grey")
# the 138 kV bushings at the west, their arresters on brackets outside the tank, the jumpers in from the bus
for z in HV:
	top = y.bushing(M, 1.5, z, COVER + 0.08, 2.75, 0.09, 18, 0.16)
	M.box((1.42, top, z - 0.02), (1.58, top + 0.1, z + 0.02), "steel")
	M.box((0.55, 2.4, z - 0.06), (X1, 2.5, z + 0.06), "grey")                 # arrester bracket
	y.post_insulator(M, 0.6, z, 2.5, 1.6, 0.07, 10, 0.12, tex="polymer")     # the arrester
	M.cylinder("y", (0.1, z), 0.025, y.HV_Y, top + 0.05, "steel", 6)          # jumper up from the bus
	M.cylinder("x", (top + 0.05, z), 0.025, 0.1, 1.5, "steel", 6)            # and across to the bushing
	M.cylinder("x", (y.HV_Y, z), 0.04, 0.0, 0.12, "steel", 6)                 # the bus's end fitting
# the 12.47 kV bushings and X0 at the east, the jumpers out to the low-side bus
for z in HV:
	top = y.bushing(M, 4.5, z, COVER + 0.08, 0.75, 0.06, 5, 0.1)
	M.cylinder("x", (top + 0.03, z), 0.025, 4.5, 5.9, "steel", 6)
	M.cylinder("y", (5.9, z), 0.025, y.DIST_Y, top + 0.03, "steel", 6)
	M.cylinder("x", (y.DIST_Y, z), 0.035, 5.9, 6.0, "steel", 6)
y.bushing(M, 4.5, 4.0 - 0.15, COVER + 0.08, 0.5, 0.05, 3, 0.08)              # X0, the neutral
M.box((4.45, BASE + 0.1, 3.95), (4.55, COVER + 0.6, 4.0), "copper")          # its strap to the ground pad
# conservator on its stand, the Buchholz relay in the pipe to the cover
for z in (1.3, 3.7):
	M.box((3.75, COVER + 0.08, z - 0.05), (3.85, 4.4, z + 0.05), "grey")
M.cylinder("z", (3.8, 4.7), 0.35, 0.9, 4.1, "grey", 16)
M.pipe([(3.8, 4.35, 2.5), (3.8, COVER + 0.4, 2.5), (3.4, COVER + 0.4, 2.5), (3.4, COVER + 0.08, 2.5)], 2, "grey")
M.box((3.5, COVER + 0.3, 2.42), (3.7, COVER + 0.5, 2.58), "iron")          # the Buchholz relay
M.cylinder("y", (3.8, 4.25), 0.06, 4.95, 5.1, "galv", 8)                    # breather
# tap changer compartment and control cabinet on the east end, gauges and nameplate
M.box((X2, 0.5, 1.15), (5.55, 2.4, 2.35), "grey")
M.box((X2, 0.5, 2.65), (5.55, 2.2, 3.85), "grey")
M.decal((5.55, 1.6, 2.8), (5.555, 2.05, 3.7), "nameplate", "east", "steel")
M.decal((5.55, 0.7, 2.9), (5.555, 1.1, 3.6), "danger", "east", "grey")
for x in (2.2, 2.8, 3.4):
	M.cylinder("z", (x, 2.9), 0.09, Z1 - 0.08, Z1, "galv", 12)
	M.decal((x - 0.07, 2.83, Z1 - 0.085), (x + 0.07, 2.97, Z1 - 0.08), "gauge", "north", "galv")
for x, z in ((X1 + 0.1, Z1 - 0.12), (X2 - 0.2, Z2 + 0.1)):
	M.box((x, BASE + 0.05, z), (x + 0.1, BASE + 0.15, z + 0.02), "copper")

M.collide((0.0, 0.0, 0.0), (6.0, 0.3, 5.0))
M.collide((X1, BASE, 0.15), (X2, COVER + 0.08, 4.85))
M.collide((X2, 0.5, 1.15), (5.55, 2.4, 3.85))
