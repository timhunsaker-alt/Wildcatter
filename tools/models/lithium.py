"""Phase 20c at true size (docs/multiblocks/lithium.md), each part drawn facing north from its own main cell (the footprints match
ChemPartBlock's). The 480 V box is on the west face of the main cell, or on its north face where a part against it covers the west:
- decrepitation_kiln, 3 x 4 x 12: the rotary kiln on its tyres, its burner hood, the cooler and the feed chute; gas east at the back;
- acid_roast_kiln, 3 x 3 x 8: the pug mill and the indirectly fired kiln with its muffle; acid east, gas south (box north);
- lithium_leach, 4 x 5 x 6: the leach tanks and their agitators, the belt filter, the lime silo; residue and CO2 south (box north);
- lioh_crystallizer, 3 x 10 x 4: the causticiser, the Glauber's salt chiller and the evaporative crystallizer; caustic east, steam south;
- lithium_carbonator, 2 x 4 x 3: the carbonation reactor, its CO2 sparger and the centrifuge; CO2 east."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.4, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


def box_north(M, y=0.4, x=0.2):
	M.box((x, y, 0.0), (x + 0.5, y + 0.6, 0.3), "grey")
	M.decal((x + 0.1, y + 0.1, -0.01), (x + 0.4, y + 0.4, 0.0), "danger", "north", "grey")


def vessel(M, cx, cz, r, y0, y1, tex="steel", sides=16):
	M.cylinder("y", (cx, cz), r, y0, y1, tex, sides)
	M.cylinder("y", (cx, cz), r * 0.6, y1, y1 + 0.2, tex, 12)


def drive(M, cx, cz, y, tex="blue"):
	M.box((cx - 0.3, y, cz - 0.3), (cx + 0.3, y + 0.35, cz + 0.3), "engine")
	M.cylinder("y", (cx, cz), 0.18, y + 0.35, y + 0.95, tex, 12)


def stub(M, a, b, nps=2, tex="steel"):
	M.pipe([a, b], nps, tex)


# ---- the decrepitation kiln -----------------------------------------------------------------------------------------------------
DK = model("decrepitation_kiln", 3, 4, 12, "hipaint")
DK.box((0.0, 0.0, 0.0), (3.0, 0.3, 12.0), "concrete")
DK.cylinder("z", (1.5, 1.6), 1.0, 1.0, 9.0, "hipaint", 16)                     # the rotary kiln
for z in (2.0, 6.5):
	DK.ring("z", (1.5, 1.6), 1.08, z, z + 0.3, 0.08, "iron", sides=16)         # its riding tyres
	DK.box((0.6, 0.3, z - 0.1), (2.4, 0.6, z + 0.4), "iron")                    # and rollers
DK.ring("z", (1.5, 1.6), 1.15, 4.4, 4.6, 0.1, "brass", sides=16)               # the girth gear
DK.box((2.2, 0.3, 4.2), (2.9, 1.0, 4.9), "blue")                               # its drive
DK.box((0.4, 0.3, 9.0), (2.6, 2.8, 10.2), "hipaint")                           # the burner hood
DK.cylinder("z", (1.5, 1.1), 0.7, 10.2, 11.9, "steel", 14)                     # the cooler
DK.box((0.9, 2.7, 0.1), (2.1, 3.9, 1.0), "galv")                               # the feed chute under its hopper
stub(DK, (3.0, 0.5, 9.5), (2.6, 0.5, 9.5), 2, "steel")                          # gas in, the east at the back
box480(DK, 0.4, 0.2)
DK.collide((0.2, 0.0, 0.0), (2.8, 2.8, 12.0))

# ---- the acid roast kiln ---------------------------------------------------------------------------------------------------------
AR = model("acid_roast_kiln", 3, 3, 8, "steel")
AR.box((0.0, 0.0, 0.0), (3.0, 0.3, 8.0), "concrete")
AR.box((0.4, 0.3, 0.3), (2.6, 1.3, 1.8), "polymer")                            # the pug mill
AR.cylinder("z", (1.5, 1.4), 0.8, 2.0, 7.4, "lagging", 14)                     # the kiln and its muffle
AR.box((2.0, 0.3, 2.5), (2.8, 0.8, 3.3), "blue")                               # the drive
stub(AR, (3.0, 0.5, 7.5), (2.6, 0.5, 7.5), 2, "steel")                          # acid in, the east at the back
stub(AR, (1.5, 0.5, 7.4), (1.5, 0.5, 8.0), 2, "steel")                          # gas in, the south
box_north(AR, 0.4, 0.2)
AR.collide((0.3, 0.0, 0.3), (2.7, 2.2, 7.6))

# ---- the lithium leach -----------------------------------------------------------------------------------------------------------
LL = model("lithium_leach", 4, 5, 6, "polymer")
LL.box((0.0, 0.0, 0.0), (4.0, 0.3, 6.0), "concrete")
for cx, cz in ((1.0, 1.0), (3.0, 1.0)):
	vessel(LL, cx, cz, 0.8, 0.3, 2.6, "polymer", 14)                           # the leach and neutralisation tanks
	drive(LL, cx, cz, 2.8)
LL.box((0.3, 0.3, 2.4), (3.7, 1.2, 4.6), "rubber")                             # the belt filter
LL.box((0.4, 1.2, 2.6), (3.6, 1.3, 4.4), "dark")
LL.cylinder("y", (3.2, 5.2), 0.5, 0.3, 4.6, "galv", 12)                         # the lime silo under its hopper
stub(LL, (1.5, 0.5, 5.6), (1.5, 0.5, 6.0), 3, "rubber")                         # residue out, the south (x 1)
stub(LL, (3.5, 0.5, 5.7), (3.5, 0.5, 6.0), 2, "steel")                          # CO2 in, the south (x 3)
box_north(LL, 0.4, 1.8)
LL.collide((0.2, 0.0, 0.2), (3.8, 2.6, 5.8))

# ---- the LiOH crystallizer -------------------------------------------------------------------------------------------------------
LC = model("lioh_crystallizer", 3, 10, 4, "steel")
LC.box((0.0, 0.0, 0.0), (3.0, 0.3, 4.0), "concrete")
vessel(LC, 0.9, 0.9, 0.6, 0.3, 3.0, "polymer", 14)                              # the causticiser
drive(LC, 0.9, 0.9, 3.2)
LC.cylinder("y", (2.2, 0.9), 0.55, 0.3, 5.0, "lagging", 14)                     # the Glauber's salt chiller
vessel(LC, 1.5, 2.8, 0.9, 0.3, 8.6, "steel", 16)                                # the evaporative crystallizer
LC.cylinder("y", (1.5, 2.8), 0.3, 8.8, 9.9, "steel", 12)                        # its vapour line and condenser
LC.box((0.2, 0.3, 3.5), (0.9, 1.0, 3.95), "blue")                               # the centrifuges
stub(LC, (3.0, 0.5, 3.5), (2.4, 0.5, 3.5), 2, "steel")                          # caustic in, the east at the back
stub(LC, (1.5, 0.5, 3.7), (1.5, 0.5, 4.0), 3, "steel")                          # steam in, the south
LC.ladder(0.1, 2.0, 0.3, 8.6, -1, "galv")
box_north(LC, 0.4, 0.2)
LC.collide((0.2, 0.0, 0.2), (2.8, 8.6, 3.8))

# ---- the lithium carbonator ------------------------------------------------------------------------------------------------------
CA = model("lithium_carbonator", 2, 4, 3, "steel")
CA.box((0.0, 0.0, 0.0), (2.0, 0.3, 3.0), "concrete")
vessel(CA, 1.0, 1.0, 0.7, 0.3, 2.6, "steel", 14)                                # the carbonation reactor
drive(CA, 1.0, 1.0, 2.8)
CA.box((0.3, 0.3, 2.0), (1.7, 1.2, 2.9), "blue")                                # the centrifuge
stub(CA, (2.0, 0.5, 0.5), (1.6, 0.5, 0.5), 2, "steel")                          # CO2 in, the east of its main cell
box480(CA, 0.4, 0.2)
CA.collide((0.2, 0.0, 0.2), (1.8, 2.6, 2.9))


M = DK
EXTRA = [AR, LL, LC, CA]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
