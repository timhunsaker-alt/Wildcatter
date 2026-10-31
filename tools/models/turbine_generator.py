"""Steam plant generator, 1,250 kVA two-pole, 4.16 kV, 3 x 2 x 2 (docs/multiblocks/steam-plant.md), at true size.

About 2.5 m long on its skid, west of the turbine: the stator frame with its feet, the cooling-air hood and filters on top,
the bearing pedestals, the drive-end shaft to the coupling (east), the brushless exciter at the non-drive end (west), and
the line terminal box on that end with its cable glands: the 4.16 kV terminals are on this west face, low down.
"""
import plantlib as p

M = p.model("turbine_generator", 3, 2, 2, "blue")
Y, Z = p.SHAFT_Y, p.SHAFT_Z

p.skid(M, 0.0, 3.0, 0.15, 1.85)
# stator frame and feet
M.cylinder("x", (Y, Z), 0.6, 0.45, 2.55, "blue", 16)
for x in (0.55, 1.2, 1.85, 2.45):
	M.ring("x", (Y, Z), 0.61, x, x + 0.04, 0.03, "blue", sides=16)
for x in (0.7, 2.3):
	for z1, z2 in ((Z - 0.65, Z - 0.3), (Z + 0.3, Z + 0.65)):
		M.box((x - 0.15, 0.22, z1), (x + 0.15, Y - 0.2, z2), "blue")
# cooling-air hood and its inlet filters on top
M.box((0.65, Y + 0.52, Z - 0.45), (2.35, 1.72, Z + 0.45), "grey")
for x in (0.9, 1.5, 2.1):
	M.box((x - 0.22, 1.25, Z - 0.47), (x + 0.22, 1.6, Z - 0.45), "dark")   # filter frames on the north side
	M.fins((x - 0.2, 1.27, Z - 0.48), (x + 0.2, 1.58, Z - 0.47), "x", 8, 0.01, "galv")
M.box((0.6, 1.72, Z - 0.5), (2.4, 1.76, Z + 0.5), "grey")
# bearing pedestals and the shaft to the coupling at the east end
for x1, x2 in ((0.3, 0.45), (2.55, 2.72)):
	M.box((x1, 0.22, Z - 0.26), (x2, Y + 0.16, Z + 0.26), "iron")
	M.cylinder("x", (Y, Z), 0.14, x1, x2, "iron", 12)
M.cylinder("x", (Y, Z), 0.07, 2.72, 3.0, "steel", 10)
p.coupling_guard(M, 2.75, 3.0, (Y, Z), 0.3, "blue")
# the exciter at the non-drive end, and the terminal box on that end with its glands
M.cylinder("x", (Y, Z), 0.34, 0.3, 0.45, "blue", 16)
M.box((0.02, 0.24, 0.4), (0.3, 1.25, 1.6), "grey")
M.box((0.0, 0.26, 0.42), (0.02, 1.23, 1.58), "grey")   # its bolted cover
for y in (0.3, 1.18):
	for z in (0.46, 1.0, 1.54):
		M.box((0.0, y - 0.015, z - 0.015), (0.005, y + 0.015, z + 0.015), "galv")
for z in (0.6, 0.85, 1.15, 1.4):   # glands for the three phases and the neutral, low on the west face
	M.cylinder("x", (0.4, z), 0.045, 0.0, 0.02, "brass", 10)
M.decal((0.0, 0.85, 0.75), (0.002, 1.1, 1.25), "danger", "west", "grey")
M.decal((1.6, Y - 0.1, Z - 0.62), (2.0, Y + 0.12, Z - 0.6), "nameplate", "north", "steel")
M.box((1.0, 0.25, Z + 0.62), (1.3, 0.5, Z + 0.7), "grey")   # space-heater box on the south side

M.collide((0.0, 0, 0.3), (3.0, 1.76, 1.7))
