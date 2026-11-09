"""The cranes' bases at true size (docs/multiblocks/tower-crane.md, crawler-crane.md), each drawn facing north from its own main cell,
the north-west corner (the footprints match CranePartBlock's). The mast, jib, upper works and boom above them are drawn by
client/CraneRenderer. The slewing centre is the middle of each box.
- tower_crane, 6 x 3 x 6: four corner pads with spindle jacks and outrigger arms, the cross frame, the ballast slabs and the mast
  foot to 3 m; the switch cabinet on the main cell, its terminal on the west face;
- crawler_crane, 9 x 2 x 9: the two crawlers (track frames, shoes, idler and drive tumblers) down the west and east sides, the carbody
  between them with its carbody counterweight, and the slewing ring;
- ring_crane (docs/multiblocks/ring-crane.md), the ring only: the 616 cells of a 49 x 49 square whose centres lie 20 to 24.25 m from
  its middle, 1 m high, its main cell the north cell of the middle column. The ring mats, and the two ring beams with their rails,
  drawn cell by cell as straight boxes. The bogies, platforms, counterweight and boom above it turn, so the renderer draws them."""
import math
import mlib
import plantlib as g

TEX = dict(g.TEX)
TEX["yellow"] = "block/mat/mat_paint_crane_yellow"


def model(name, w, h, d, particle="yellow"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


# ---- the tower crane's cross base -----------------------------------------------------------------------------------------------------
TC = model("tower_crane", 6, 3, 6)
for x, z in ((0.0, 0.0), (5.0, 0.0), (0.0, 5.0), (5.0, 5.0)):
	TC.box((x + 0.1, 0.0, z + 0.1), (x + 0.9, 0.25, z + 0.9), "concrete")       # the corner pads
	TC.cylinder("y", (x + 0.5, z + 0.5), 0.12, 0.25, 0.75, "galv", 8)           # the spindle jacks
	TC.box((x + 0.3, 0.75, z + 0.3), (x + 0.7, 0.95, z + 0.7), "yellow")        # their heads
# the outrigger arms from the corners in to the frame, an L of box beams each (a turned beam reaches too far past its cell)
for x, z in ((0.5, 0.5), (5.5, 0.5), (0.5, 5.5), (5.5, 5.5)):
	ix = 1.2 if x < 3 else 4.8
	iz = 1.2 if z < 3 else 4.8
	TC.box((min(x, ix) - 0.2, 0.75, z - 0.2), (max(x, ix) + 0.2, 1.05, z + 0.2), "yellow")
	TC.box((ix - 0.2, 0.75, min(z, iz) - 0.2), (ix + 0.2, 1.05, max(z, iz) + 0.2), "yellow")
# the cross frame: a square of box beams, and the cross under the mast
TC.box((1.0, 0.6, 1.0), (5.0, 1.0, 1.4), "yellow")
TC.box((1.0, 0.6, 4.6), (5.0, 1.0, 5.0), "yellow")
TC.box((1.0, 0.6, 1.4), (1.4, 1.0, 4.6), "yellow")
TC.box((4.6, 0.6, 1.4), (5.0, 1.0, 4.6), "yellow")
TC.box((1.4, 0.6, 2.8), (4.6, 1.0, 3.2), "yellow")
TC.box((2.8, 0.6, 1.4), (3.2, 1.0, 4.6), "yellow")
# the ballast slabs, about 112 t of concrete, two courses round the mast foot
for x0, z0, x1, z1 in ((1.1, 1.1, 4.9, 1.85), (1.1, 4.15, 4.9, 4.9), (1.1, 1.85, 1.85, 4.15), (4.15, 1.85, 4.9, 4.15)):
	TC.box((x0, 1.0, z0), (x1, 1.75, z1), "concrete")
	TC.box((x0 + 0.05, 1.75, z0 + 0.05), (x1 - 0.05, 2.5, z1 - 0.05), "concrete")
# the mast foot: the first 355 HC section, 2.3 m square, its four chords and its bracing, to 3 m
lo, hi = 1.85, 4.15
for x in (lo, hi - 0.2):
	for z in (lo, hi - 0.2):
		TC.box((x, 1.0, z), (x + 0.2, 3.0, z + 0.2), "yellow")
for y in (1.0, 2.9):
	TC.box((lo, y, lo), (hi, y + 0.1, lo + 0.12), "yellow")
	TC.box((lo, y, hi - 0.12), (hi, y + 0.1, hi), "yellow")
	TC.box((lo, y, lo), (lo + 0.12, y + 0.1, hi), "yellow")
	TC.box((hi - 0.12, y, lo), (hi, y + 0.1, hi), "yellow")
for y in (1.95,):   # a mid rail on each face (diagonals would be turned boxes, reaching too far past their cells)
	TC.box((lo, y, lo), (hi, y + 0.08, lo + 0.08), "yellow")
	TC.box((lo, y, hi - 0.08), (hi, y + 0.08, hi), "yellow")
# the switch cabinet on the main cell, its supply on the west face
TC.box((0.05, 0.25, 0.25), (0.6, 1.6, 0.85), "grey")
TC.decal((0.04, 0.6, 0.35), (0.05, 1.2, 0.75), "danger", "west", "grey")
TC.collide((0.0, 0.0, 0.0), (6.0, 1.0, 6.0))
TC.collide((1.1, 1.0, 1.1), (4.9, 2.5, 4.9))
TC.collide((lo, 2.5, lo), (hi, 3.0, hi))
TC.collide((0.05, 1.0, 0.25), (0.6, 1.6, 0.85))

# ---- the crawler crane's crawlers and carbody -------------------------------------------------------------------------------------------
CC = model("crawler_crane", 9, 2, 9, "dark")
for x0 in (0.0, 7.6):
	x1 = x0 + 1.4
	CC.box((x0, 0.0, 0.2), (x1, 0.15, 8.8), "dark")                             # the shoes on the ground
	for z in [0.4 + i * 0.4 for i in range(21)]:
		CC.box((x0, 0.15, z), (x1, 0.22, z + 0.1), "iron")                      # the shoes' grousers
	CC.box((x0 + 0.15, 0.22, 0.7), (x1 - 0.15, 1.15, 8.3), "yellow")            # the track frame
	CC.cylinder("x", (0.65, 0.65), 0.55, x0, x1, "iron", 12)                    # the idler tumbler, north
	CC.cylinder("x", (0.65, 8.35), 0.55, x0, x1, "iron", 12)                    # the drive tumbler, south
	CC.box((x0, 1.15, 0.6), (x1, 1.3, 8.4), "dark")                             # the shoes over the top
	CC.cylinder("x", (0.85, 7.6), 0.35, x1 - 0.05 if x0 else x0 - 0.3, x1 + 0.25 if x0 else x0 + 0.05, "grey", 10)   # the drive motor
# the carbody across, with its 57 t of carbody counterweight north and south
CC.box((1.4, 0.4, 3.0), (7.6, 1.6, 6.0), "yellow")
CC.box((2.2, 0.5, 1.6), (6.8, 1.5, 3.0), "iron")
CC.box((2.2, 0.5, 6.0), (6.8, 1.5, 7.4), "iron")
# the slewing ring
for a, b in ((1.7, 0.7), (0.7, 1.7), (1.2, 1.2)):   # an octagon of straight boxes: a turned slab would reach too far past its cell
	CC.box((4.5 - a, 1.6, 4.5 - b), (4.5 + a, 1.85, 4.5 + b), "galv")
	CC.box((4.5 - a + 0.2, 1.85, 4.5 - b + 0.2), (4.5 + a - 0.2, 2.0, 4.5 + b - 0.2), "dark")
CC.collide((0.0, 0.0, 0.2), (1.4, 1.3, 8.8))
CC.collide((7.6, 0.0, 0.2), (9.0, 1.3, 8.8))
CC.collide((1.4, 0.4, 1.6), (7.6, 1.6, 7.4))
CC.collide((2.8, 1.6, 2.8), (6.2, 2.0, 6.2))

# ---- the ring crane's ring --------------------------------------------------------------------------------------------------------------
RING, MAIN = 49, 24
INNER, OUTER = 20.0, 24.25
BEAMS = (21.2, 23.4)   # the two ring beams' centre lines, m from the middle (the bogies run on them)


def in_ring(x, z):
	return INNER <= math.hypot(x + 0.5 - RING / 2, z + 0.5 - RING / 2) <= OUTER


RING_CELLS = [(0, 0, 0)] + [(x - MAIN, 0, z) for z in range(RING) for x in range(RING) if (x, z) != (MAIN, 0) and in_ring(x, z)]
RC = mlib.Model("ring_crane", RING_CELLS, dict(TEX), TEX["yellow"])
for (cx, _, cz) in RING_CELLS:
	x, z = cx + MAIN, cz
	d = math.hypot(x + 0.5 - RING / 2, z + 0.5 - RING / 2)
	RC.box((cx, 0.0, cz), (cx + 1, 0.12, cz + 1), "dark")                                    # the steel ring spreader mats
	for beam in BEAMS:
		if abs(d - beam) < 0.62:
			RC.box((cx + 0.05, 0.12, cz + 0.05), (cx + 0.95, 0.75, cz + 0.95), "yellow")   # the ring beam, a box girder
			RC.box((cx + 0.3, 0.75, cz + 0.3), (cx + 0.7, 0.85, cz + 0.7), "iron")          # its rail
	RC.collide((cx, 0.0, cz), (cx + 1, 0.85 if any(abs(d - b) < 0.62 for b in BEAMS) else 0.12, cz + 1))

M = TC
EXTRA = [CC, RC]
CC.export_args = {"blockstate": True, "item": True}
RC.export_args = {"blockstate": True, "item": True}
