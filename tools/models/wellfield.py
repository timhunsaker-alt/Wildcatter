"""The ISR wellfield at true size (docs/multiblocks/isr-wellfield.md), each part drawn facing north from its own main cell (the
footprints match WellfieldPartBlock's):
- header_house, 4 x 4 x 10: the insulated wellfield building on its pad, the producers' pipes into its west side, the injection
  header out of its east, the trunklines to the IX columns out of its back, its 480 V box on the north end;
- oxygen_generator, 3 x 3 x 6: the PSA skid: an air compressor, the receiver, two molecular-sieve beds and the oxygen buffer tank;
- ix_columns, 8 x 6 x 4: the lead and lag resin columns on their skid with the elution tank and the acid day tank;
- isr_wellhead, one cell: the PVC casing up out of the ground and its well cap; with the pump, a producer's pitless head with
  its power cable conduit and a flow meter on the line."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


# ---- the header house ---------------------------------------------------------------------------------------------------------
HH = model("header_house", 4, 4, 10, "grey")
HH.box((0.0, 0.0, 0.0), (4.0, 0.2, 10.0), "concrete")                     # its pad
HH.box((0.2, 0.2, 0.2), (3.8, 3.2, 9.8), "louvre")                          # the insulated building
HH.box((0.1, 3.2, 0.1), (3.9, 3.35, 9.9), "grey")                           # the roof, pitched a little
HH.box((1.0, 3.35, 0.1), (3.0, 3.5, 9.9), "grey")
HH.box((1.4, 0.2, 9.8), (2.6, 2.4, 9.85), "iron")                           # its door, the back end
HH.box((1.5, 0.6, 0.0), (2.5, 1.4, 0.2), "grey")                            # its 480 V box, the north end
HH.decal((1.6, 0.7, -0.01), (2.4, 1.3, 0.0), "danger", "north", "grey")
for z in (1.5, 3.5, 5.5, 7.5):
	HH.pipe([(0.0, 0.4, z), (0.2, 0.4, z)], 3, "polymer")                   # producers' lines in, the west
	HH.pipe([(3.8, 0.4, z), (4.0, 0.4, z)], 3, "polymer")                   # injection header out, the east
HH.pipe([(1.0, 0.4, 9.8), (1.0, 0.4, 10.0)], 8, "polymer")                  # pregnant trunk to the IX columns
HH.pipe([(3.0, 0.4, 9.8), (3.0, 0.4, 10.0)], 8, "polymer")                  # barren trunk back
HH.collide((0.2, 0.0, 0.2), (3.8, 3.5, 9.8))

# ---- the oxygen generator ---------------------------------------------------------------------------------------------------------
OG = model("oxygen_generator", 3, 3, 6, "blue")
OG.box((0.05, 0.0, 0.05), (2.95, 0.2, 5.95), "iron")                        # the skid
OG.box((0.3, 0.2, 0.3), (1.6, 1.6, 2.2), "blue")                            # the air compressor in its canopy
OG.fins((0.35, 0.4, 0.28), (1.55, 1.4, 0.3), "x", 8, 0.02, "galv")
OG.box((0.0, 0.5, 0.3), (0.3, 1.1, 0.8), "grey")                            # its 480 V box, the west
OG.cylinder("y", (2.3, 1.2), 0.45, 0.2, 2.2, "steel", 14)                   # the air receiver
for z in (3.0, 4.0):
	OG.cylinder("y", (1.0, z), 0.38, 0.2, 2.8, "padgreen", 14)              # the two molecular-sieve beds
OG.cylinder("y", (2.3, 4.6), 0.45, 0.2, 2.4, "steel", 14)                   # the oxygen buffer tank
OG.pipe([(1.0, 2.6, 3.0), (1.0, 2.6, 4.0)], 2, "steel")
OG.pipe([(1.0, 2.3, 4.0), (2.3, 2.3, 4.0), (2.3, 2.3, 4.6)], 1, "copper")
OG.collide((0.05, 0.0, 0.05), (2.95, 2.4, 5.95))

# ---- the IX columns ---------------------------------------------------------------------------------------------------------------
IX = model("ix_columns", 8, 6, 4, "steel")
IX.box((0.0, 0.0, 0.0), (8.0, 0.25, 4.0), "concrete")
for x in (2.0, 6.0):
	IX.ring("y", (x, 2.0), 1.75, 0.6, 5.2, 0.1, "steel", sides=20)          # a resin column, 3.5 m across by 4.6 m
	IX.box((x - 1.0, 5.15, 1.0), (x + 1.0, 5.25, 3.0), "steel")             # its dished head
	IX.box((x - 1.6, 5.15, 1.6), (x + 1.6, 5.2, 2.4), "steel")
	IX.box((x - 0.3, 5.25, 1.7), (x + 0.3, 5.7, 2.3), "steel")              # its top manway
	for dx in (-1.2, 1.2):
		IX.box((x + dx - 0.1, 0.25, 1.9), (x + dx + 0.1, 0.6, 2.1), "iron")  # its legs
IX.pipe([(2.0, 0.5, 0.0), (2.0, 0.5, 0.3)], 8, "polymer")                   # pregnant lixiviant in, the front
IX.pipe([(2.0, 0.4, 3.7), (2.0, 0.4, 4.0)], 3, "steel")                     # eluate out, the back (west)
IX.pipe([(6.0, 0.4, 3.7), (6.0, 0.4, 4.0)], 8, "polymer")                   # barren out, the back (east)
IX.pipe([(8.0, 0.4, 2.0), (7.75, 0.4, 2.0)], 2, "steel")                    # acid in, the east
IX.pipe([(2.0, 5.5, 2.0), (2.0, 5.8, 2.0), (6.0, 5.8, 2.0), (6.0, 5.5, 2.0)], 6, "steel")   # lead to lag over the top
IX.collide((0.25, 0.0, 0.25), (7.75, 5.7, 3.75))


# ---- the ISR wellhead: injector, and producer with its pump ----------------------------------------------------------------------
def wellhead(name, pump):
	M = mlib.Model(name, [(0, 0, 0)], TEX, TEX["polymer"])
	M.box((0.2, 0.0, 0.2), (0.8, 0.08, 0.8), "concrete")                    # the well pad
	M.cylinder("y", (0.5, 0.5), 0.085, 0.0, 0.55, "polymer", 12)           # 6 in PVC casing
	M.cylinder("y", (0.5, 0.5), 0.1, 0.55, 0.65, "padgreen" if pump else "blue", 12)   # the well cap
	M.pipe([(0.5, 0.35, 0.5), (1.0, 0.35, 0.5)], 2, "polymer")             # its line, east
	if pump:
		M.cylinder("y", (0.5, 0.5), 0.03, 0.65, 0.75, "steel", 8)          # the pitless head's lifting eye
		M.pipe([(0.5, 0.6, 0.5), (0.5, 0.6, 0.15), (0.5, 0.08, 0.15)], 1, "grey")   # the pump's power cable conduit
		M.box((0.7, 0.29, 0.43), (0.84, 0.41, 0.57), "brass")              # a flow meter on the line
	M.collide((0.38, 0.0, 0.38), (0.62, 0.65, 0.62))
	return M


ISR = wellhead("isr_wellhead", False)
ISR.export_args = {"blockstate": False, "item": True}
ISR_PUMP = wellhead("isr_wellhead_pump", True)
ISR_PUMP.export_args = {"blockstate": False, "item": False}

M = HH
EXTRA = [OG, IX, ISR, ISR_PUMP]
for extra in (OG, IX):
	extra.export_args = {"blockstate": True, "item": True}


def after():
	import gearlib
	gearlib.save_blockstate("isr_wellhead", {"pump=false": {"model": "wildcatter:block/isr_wellhead/p0"},
		"pump=true": {"model": "wildcatter:block/isr_wellhead_pump/p0"}})
