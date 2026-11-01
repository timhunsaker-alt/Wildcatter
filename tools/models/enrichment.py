"""The centrifuge cascade at true size (docs/multiblocks/enrichment.md), each part drawn facing north from its own main cell (the
footprints match EnrichmentPartBlock's):
- centrifuge, 1 x 6 x 1: a TC-21 class machine, its 0.4 m vacuum casing on a plinth over the motor, the header fittings on top;
- frequency_converter, 3 x 3 x 6: the input transformer and the rectifier and inverter cubicles (12.47 kV box west);
- feed_station, 3 x 3 x 5: the heated cabinet around a 48Y, its header valve out the back (480 V box west);
- product_takeoff, 3 x 3 x 4: a 30B on its scale under the chilled desublimers (480 V box west);
- tails_takeoff, 3 x 3 x 5: a 48Y on its scale under the desublimers (480 V box west)."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


# ---- the centrifuge -------------------------------------------------------------------------------------------------------------
CE = model("centrifuge", 1, 6, 1, "galv")
CE.box((0.1, 0.0, 0.1), (0.9, 0.15, 0.9), "concrete")                      # its plinth
CE.cylinder("y", (0.5, 0.5), 0.26, 0.15, 0.6, "blue", 14)                   # the motor housing at its foot
CE.cylinder("y", (0.5, 0.5), 0.2, 0.6, 5.6, "galv", 14)                     # the vacuum casing round the 5 m rotor
for y in (1.6, 3.1, 4.6):
	CE.ring("y", (0.5, 0.5), 0.21, y, y + 0.08, 0.03, "iron", sides=14)     # its casing flanges
CE.cylinder("y", (0.5, 0.5), 0.24, 5.6, 5.75, "iron", 14)                   # the top cover and magnetic bearing
for x, z in ((0.5, 0.3), (0.32, 0.62), (0.68, 0.62)):
	CE.cylinder("y", (x, z), 0.04, 5.75, 6.0, "steel", 6)                   # feed, product and tails take-offs to the header
CE.collide((0.25, 0.0, 0.25), (0.75, 6.0, 0.75))

# ---- the frequency converter -------------------------------------------------------------------------------------------------------
FC = model("frequency_converter", 3, 3, 6, "grey")
FC.box((0.0, 0.0, 0.0), (3.0, 0.2, 6.0), "concrete")
FC.box((0.3, 0.2, 0.3), (2.6, 2.4, 2.1), "grey")                            # the input transformer
FC.fins((2.6, 0.4, 0.4), (2.7, 2.2, 2.0), "z", 8, 0.02, "radfin")
FC.box((0.3, 0.2, 2.4), (2.6, 2.6, 5.8), "grey")                            # the rectifier and inverter cubicles
for z in (2.7, 3.5, 4.3, 5.1):
	FC.decal((2.6, 0.6, z), (2.61, 2.2, z + 0.6), "louvre", "east", "grey")
FC.decal((2.6, 2.3, 2.5), (2.61, 2.5, 3.0), "danger", "east", "grey")
FC.box((0.0, 0.6, 0.4), (0.3, 1.4, 1.0), "grey")                            # its 12.47 kV box, west
FC.box((1.2, 0.3, 5.8), (1.8, 0.9, 6.0), "grey")                            # its output to the header, the back
FC.collide((0.3, 0.0, 0.3), (2.7, 2.6, 5.8))

# ---- the feed station ------------------------------------------------------------------------------------------------------------
FS = model("feed_station", 3, 3, 5, "louvre")
FS.box((0.0, 0.0, 0.0), (3.0, 0.2, 5.0), "concrete")
FS.box((0.15, 0.2, 0.15), (2.85, 2.7, 4.55), "louvre")                      # the heated cabinet round its 48Y
FS.box((0.1, 2.7, 0.1), (2.9, 2.85, 4.6), "grey")
FS.decal((0.4, 0.4, 0.14), (2.6, 2.5, 0.15), "grey", "north", "grey")       # its door
FS.pipe([(1.5, 0.5, 4.55), (1.5, 0.5, 5.0)], 2, "steel")                    # UF6 out to the header, the back
box480(FS, 0.5, 0.5)
FS.collide((0.15, 0.0, 0.15), (2.85, 2.85, 4.6))

# ---- the take-offs -----------------------------------------------------------------------------------------------------------------
def takeoff(name, length, r):
	M = model(name, 3, 3, length, "grey")
	M.box((0.0, 0.0, 0.0), (3.0, 0.3, length), "checker")                   # the scale deck
	M.cylinder("z", (1.5, 0.35 + r), r, 0.5, length - 0.5, "padgreen", 16)  # the cylinder on its cradles
	for z in (0.9, length - 0.9):
		M.box((0.8, 0.3, z - 0.2), (2.2, 0.45, z + 0.2), "iron")
	M.box((0.3, 2.0, 0.2), (2.7, 2.9, 1.4), "lagging")                     # the chilled desublimers
	M.box((2.2, 0.3, 0.1), (2.9, 1.9, 0.9), "blue")                         # their refrigeration unit
	M.box((0.3, 0.3, 0.4), (0.5, 2.0, 0.6), "iron")                         # and their stand
	M.pipe([(1.5, 0.5, 0.0), (1.5, 0.5, 0.4)], 2, "steel")                  # the header in, the front
	box480(M, 0.5, 1.6)
	M.collide((0.0, 0.0, 0.0), (3.0, 0.4 + 2 * r, length))
	return M


PT = takeoff("product_takeoff", 4, 0.38)   # a 30B: 30 in across, 2.07 m long
TT = takeoff("tails_takeoff", 5, 0.61)     # a 48Y: 48 in across, 3.8 m long


M = CE
EXTRA = [FC, FS, PT, TT]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
