"""Package D-type watertube boiler, 4.5 t/h (10,000 lb/h), 4 x 3 x 4 (docs/multiblocks/steam-plant.md), at true size.

About 4 m of the 5 m package (the burner and the rear outlet section are their own parts): the casing on its skid, the
furnace on the north side (the burner's opening in the front plate), the convection bank on the south side, and the steam
drum along the top over the bank with its dished heads and manway. On the drum: two safety valves, the water column with its
gauge glass and try cocks, and the steam outlet to the superheater. The feedwater line with its check and stop valves, the
blowdown valves on the mud drum, sight ports, access doors, the nameplate and a ladder to the drum.
"""
import plantlib as p

M = p.model("boiler_drum", 4, 4, 3, "blue")
TOP = 3.15                      # the casing's roof
DY, DZ, DR = 3.32, 1.85, 0.42   # the steam drum

p.skid(M, 0.0, 4.0, 0.1, 2.9)
M.box((0.0, 0.22, 0.12), (4.0, TOP, 2.88), "blue")                      # the casing
M.box((0.0, TOP, 0.1), (4.0, TOP + 0.04, 2.9), "blue")                  # the roof plate
for x in (0.03, 0.6, 1.2, 1.8, 2.4, 3.0, 3.6, 3.97):                    # casing stiffeners on both sides
	for z in (0.1, 2.88):
		M.box((x - 0.025, 0.25, z), (x + 0.025, TOP - 0.02, z + 0.02), "blue")
for y in (0.9, 1.9):
	for z in (0.1, 2.88):
		M.box((0.0, y - 0.025, z), (4.0, y + 0.025, z + 0.02), "blue")
# the front plate: the burner opening on the furnace's axis (the burner bolts to it)
M.ring("x", (1.4, 0.9), 0.52, 0.0, 0.03, 0.06, "iron", sides=16)
M.cylinder("x", (1.4, 0.9), 0.48, 0.0, 0.01, "dark", 16)
# steam drum along the top, its heads and the manway on the front head
M.cylinder("x", (DY, DZ), DR, 0.15, 3.85, "grey", 16)
for x in (0.15, 3.85):
	M.ring("x", (DY, DZ), DR + 0.01, x - 0.015, x + 0.015, 0.02, "grey", sides=16)
M.cylinder("x", (DY, DZ), DR * 0.75, 0.08, 0.15, "grey", 16)
M.cylinder("x", (DY, DZ), DR * 0.75, 3.85, 3.92, "grey", 16)
M.box((0.04, DY - 0.14, DZ - 0.1), (0.08, DY + 0.14, DZ + 0.1), "iron")   # the manway cover and its dogs
M.bolt_circle("x", (DY, DZ), 0.13, 0.03, 6, "galv")
# two safety valves on top, with their discharge elbows
for x in (1.2, 1.7):
	M.cylinder("y", (x, DZ), 0.06, DY + DR - 0.02, DY + DR + 0.08, "steel", 10)
	M.cylinder("y", (x, DZ), 0.075, DY + DR + 0.08, DY + DR + 0.11, "steel", 10)
	M.cylinder("y", (x, DZ), 0.045, DY + DR + 0.11, 3.96, "brass", 8)
	M.box((x - 0.02, 3.88, DZ - 0.12), (x + 0.02, 3.92, DZ), "steel")   # lifting lever
# the water column on the front of the drum: its body, the gauge glass, try cocks and the drain
M.pipe([(0.25, DY + 0.3, DZ - 0.35), (0.25, DY + 0.3, DZ - 0.6)], 1, "steel")
M.pipe([(0.25, DY - 0.3, DZ - 0.35), (0.25, DY - 0.3, DZ - 0.6)], 1, "steel")
M.cylinder("y", (0.25, DZ - 0.68), 0.06, DY - 0.45, DY + 0.45, "steel", 10)
M.box((0.17, DY - 0.25, DZ - 0.7), (0.19, DY + 0.25, DZ - 0.66), "polymer")   # the gauge glass
for y in (DY - 0.15, DY, DY + 0.15):
	M.box((0.17, y - 0.015, DZ - 0.76), (0.2, y + 0.015, DZ - 0.62), "brass")   # try cocks
M.pipe([(0.25, DY - 0.45, DZ - 0.68), (0.25, 0.3, DZ - 0.68)], 0.75, "steel")
M.cylinder("x", (0.6, DZ - 0.68), 0.05, 0.12, 0.22, "brass", 8)
# steam outlet to the superheater at the rear, lagged, to its flange at the boiler's end, where the superheater's inlet bolts on
M.pipe([(3.5, DY + DR - 0.05, DZ), (3.5, 3.85, DZ), (4.0, 3.85, DZ)], 6, "lagging")
M.flange("x", (3.85, DZ), 0.15, 3.965, 0.03, "steel", bolts=8, bolt_tex="galv")
# feedwater line: in from the feed pump over the generator (the plant draws that run, docs/questions/misc-gaps.md, 1) to its
# flange on the south edge 2.1 m up, along the south side through the stop valve and check valve, and up into the drum
FY = 2.1
M.pipe([(1.49, FY, 3.0), (1.49, FY, 2.93), (2.4, FY, 2.93), (2.4, DY, 2.93), (2.4, DY, DZ + DR - 0.05)], 2, "steel")
M.flange("z", (1.49, FY), 0.07, 2.965, 0.03, "steel", bolts=4, bolt_tex="galv")
M.box((1.72, FY - 0.07, 2.88), (1.88, FY + 0.07, 2.98), "steel")   # stop valve body, its stem and handwheel up
M.cylinder("y", (1.8, 2.93), 0.015, FY + 0.07, FY + 0.22, "steel", 6)
M.cylinder("y", (1.8, 2.93), 0.08, FY + 0.22, FY + 0.24, "rubber", 12)
M.box((2.04, FY - 0.06, 2.89), (2.24, FY + 0.06, 2.97), "steel")   # check valve
M.box((2.11, FY + 0.06, 2.9), (2.17, FY + 0.1, 2.96), "steel")     # its cover
# blowdown valves on the mud drum, low on the front
for z in (2.0, 2.3):
	M.pipe([(0.0 + 0.05, 0.32, z), (0.2, 0.32, z)], 1.5, "steel")
	M.cylinder("x", (0.32, z), 0.06, 0.0, 0.04, "brass", 10)
# sight ports on the front and access doors on the north side
for y in (0.7, 2.1):
	M.cylinder("x", (y, 2.4), 0.07, 0.0, 0.03, "galv", 10)
	M.cylinder("x", (y, 2.4), 0.04, 0.0, 0.005, "dark", 8)
for x in (1.0, 2.6):
	M.box((x - 0.3, 0.6, 0.08), (x + 0.3, 1.4, 0.12), "blue")
	for xx in (x - 0.26, x + 0.26):
		for y in (0.66, 1.34):
			M.box((xx - 0.015, y - 0.015, 0.06), (xx + 0.015, y + 0.015, 0.08), "galv")
M.decal((0.002, 1.05, 1.6), (0.004, 1.3, 2.1), "nameplate", "west", "steel")
# a ladder up the north side to the drum
M.ladder(3.6, 0.1, 0.22, TOP + 0.6, -1, "galv")

M.collide((0, 0, 0.1), (4, TOP + 0.04, 2.9))
M.collide((0.1, TOP, DZ - DR), (3.9, DY + DR, DZ + DR))
