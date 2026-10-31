"""An 8 ft distribution crossarm (docs/multiblocks/poles.md): a 3.5 x 4.5 in (89 x 114 mm) Douglas fir arm 2.44 m long, its
top 0.3 m below the pole top (flush with the cell above's floor, where the pin insulators stand), through-bolted to the pole
with a 5/8 in bolt and held by two flat galvanized braces 0.7 m long down to the pole. Its two cells are either side of the
pole's (the pole is at x 1.5); the arm is drawn through the pole's cell by the cells beside it."""
import linelib as l

M = l.model("crossarm_dist", [(0, 0, 0), (2, 0, 0)], "fir")
PX, PZ = 1.5, 0.5
TOP = 1.0
M.box((PX - 1.22, TOP - 0.114, PZ - 0.045), (PX + 1.22, TOP, PZ + 0.045), "fir")
M.box((PX - 0.11, TOP - 0.07, PZ - 0.06), (PX + 0.11, TOP - 0.05, PZ + 0.06), "galv")        # the through bolt
for side in (-1, 1):
	l.strut(M, (PX + side * 0.62, TOP - 0.11, PZ + 0.055), (PX + side * 0.1, TOP - 0.62, PZ + 0.055), 0.04, 0.008, "galv", pieces=1)
	M.box((PX + side * 0.62 - 0.02, TOP - 0.13, PZ + 0.045), (PX + side * 0.62 + 0.02, TOP - 0.1, PZ + 0.065), "galv")   # carriage bolt
	for k in (0.3, 0.95):
		pin = PX + side * k
		M.box((pin - 0.03, TOP - 0.005, PZ - 0.03), (pin + 0.03, TOP + 0.003, PZ + 0.03), "galv")   # pin washers
M.collide((0.28, TOP - 0.12, PZ - 0.05), (1.0, TOP, PZ + 0.05))
M.collide((2.0, TOP - 0.12, PZ - 0.05), (2.72, TOP, PZ + 0.05))
