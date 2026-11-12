"""Phase 20b at true size (docs/multiblocks/titanium-pigment.md), each part drawn facing north from its own main cell (the footprints
match ChemPartBlock's). The motor box is on the west face of the main cell, or on its north face where a part against it covers the west:
- ti_chlorinator, 4 x 12 x 4: the fluid-bed chlorinator, its feed bins on top, the cyclone and the blower; chlorine in on the south;
- ticl4_purification, 3 x 12 x 3: the condensers and the distillation column with its reboiler (box north);
- ticl4_oxidiser, 3 x 4 x 10: the preheaters, the oxygen reactor and the long flue cooler; oxygen north, gas east, chlorine back west;
- vpsa_oxygen, 3 x 4 x 6: two adsorber vessels, the blower and the vacuum pump, the oxygen receiver; oxygen out south;
- pigment_finishing, 4 x 5 x 10: the wet mills, the coating tanks, the filter, the spray dryer and the micronizer; steam east."""
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


def stub(M, a, b, nps=2, tex="steel"):
	M.pipe([a, b], nps, tex)


# ---- the titanium chlorinator ---------------------------------------------------------------------------------------------------
TC = model("ti_chlorinator", 4, 12, 4, "hipaint")
TC.box((0.0, 0.0, 0.0), (4.0, 0.3, 4.0), "concrete")
vessel(TC, 1.5, 1.5, 1.3, 0.3, 9.0, "hipaint", 18)                              # the chlorinator shell, brick-lined
for y in (3.0, 6.0):
	TC.ring("y", (1.5, 1.5), 1.32, y, y + 0.12, 0.05, "iron", sides=18)
TC.box((0.7, 9.3, 0.7), (2.3, 11.6, 2.3), "galv")                               # the feed bins (slag or rutile, coke)
TC.cylinder("y", (3.4, 3.4), 0.45, 2.5, 8.0, "steel", 12)                       # the cyclone
TC.pipe([(3.4, 8.0, 3.4), (3.4, 9.4, 3.4), (2.0, 9.4, 3.4), (2.0, 9.4, 2.4)], 4, "steel")
TC.box((2.9, 0.3, 0.2), (3.8, 1.2, 1.2), "blue")                                # the fluidizing blower
stub(TC, (1.5, 0.5, 3.6), (1.5, 0.5, 4.0), 3, "padgreen")                        # chlorine in, the south
TC.ladder(0.15, 3.0, 0.3, 9.2, 1, "galv")
box480(TC, 0.4, 2.8)
TC.collide((0.2, 0.0, 0.2), (2.8, 11.6, 2.8))

# ---- the TiCl4 purification -----------------------------------------------------------------------------------------------------
TP = model("ticl4_purification", 3, 12, 3, "lagging")
TP.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
TP.cylinder("y", (1.0, 1.8), 0.55, 0.3, 11.6, "lagging", 14)                     # the distillation column
for y in (3.0, 6.0, 9.0):
	TP.ring("y", (1.0, 1.8), 0.57, y, y + 0.1, 0.04, "galv", sides=14)
TP.cylinder("x", (8.0, 2.4), 0.3, 1.2, 2.9, "steel", 12)                         # the condensers
TP.cylinder("x", (5.0, 2.4), 0.3, 1.2, 2.9, "steel", 12)
TP.box((1.7, 0.3, 0.3), (2.8, 1.3, 1.3), "steel")                                # the reboiler
TP.pipe([(1.0, 11.6, 1.8), (1.0, 11.9, 1.8), (2.4, 11.9, 1.8), (2.4, 8.3, 1.8)], 3, "steel")
box_north(TP, 0.4, 0.2)
TP.collide((0.4, 0.0, 0.4), (2.9, 11.6, 2.6))

# ---- the TiCl4 oxidiser ----------------------------------------------------------------------------------------------------------
TO = model("ticl4_oxidiser", 3, 4, 10, "steel")
TO.box((0.0, 0.0, 0.0), (3.0, 0.3, 10.0), "concrete")
TO.box((0.3, 0.3, 0.3), (2.7, 2.6, 2.0), "hipaint")                              # the TiCl4 and oxygen preheaters
TO.cylinder("y", (1.5, 1.2), 0.25, 2.6, 3.8, "hipaint", 12)                      # their stack
TO.cylinder("z", (1.5, 1.6), 0.6, 2.0, 3.6, "steel", 14)                         # the reactor
TO.cylinder("z", (1.5, 1.6), 0.45, 3.6, 9.6, "radfin", 14)                       # the flue cooler
for z in (4.5, 6.5, 8.5):
	TO.box((0.8, 0.3, z), (2.2, 1.1, z + 0.3), "iron")                           # its supports
stub(TO, (1.5, 0.5, 0.0), (1.5, 0.5, 0.3), 3, "steel")                           # oxygen in, the north
stub(TO, (3.0, 0.5, 1.0), (2.7, 0.5, 1.0), 2, "steel")                           # gas in, the east
TO.pipe([(1.5, 1.6, 9.6), (1.5, 1.6, 9.8), (0.4, 1.6, 9.8), (0.4, 0.5, 9.8), (0.0, 0.5, 9.8)], 3, "padgreen")   # chlorine back, the west
TO.collide((0.3, 0.0, 0.3), (2.7, 2.6, 9.8))

# ---- the VPSA oxygen unit -------------------------------------------------------------------------------------------------------
VO = model("vpsa_oxygen", 3, 4, 6, "steel")
VO.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "concrete")
for z in (1.2, 3.0):
	vessel(VO, 1.5, z, 0.75, 0.3, 3.3, "steel", 16)                              # the two adsorbers
VO.box((0.3, 0.3, 4.2), (1.4, 1.3, 5.6), "blue")                                 # the blower
VO.box((1.6, 0.3, 4.2), (2.7, 1.3, 5.6), "engine")                               # the vacuum pump
VO.cylinder("y", (2.6, 2.1), 0.3, 0.3, 2.6, "padgreen", 10)                      # the oxygen receiver
stub(VO, (1.5, 0.5, 5.6), (1.5, 0.5, 6.0), 2, "steel")                           # oxygen out, the south
box480(VO, 0.4, 2.1)
VO.collide((0.3, 0.0, 0.3), (2.7, 3.3, 5.7))

# ---- the pigment finishing ------------------------------------------------------------------------------------------------------
PF = model("pigment_finishing", 4, 5, 10, "grey")
PF.box((0.0, 0.0, 0.0), (4.0, 0.3, 10.0), "concrete")
for z in (0.5, 2.0):
	PF.cylinder("z", (2.0, 1.0), 0.6, z, z + 1.2, "engine", 14)                  # the wet mills
for z in (4.0, 5.4):
	vessel(PF, 1.2, z, 0.6, 0.3, 2.6, "polymer", 14)                             # the coating tanks
PF.box((2.3, 0.3, 3.6), (3.8, 2.0, 6.0), "grey")                                  # the filter
PF.cylinder("y", (2.0, 7.4), 1.0, 1.0, 4.2, "steel", 16)                          # the spray dryer
PF.cylinder("y", (2.0, 7.4), 0.4, 0.3, 1.0, "steel", 12)
PF.box((0.4, 0.3, 8.6), (2.2, 1.6, 9.8), "steel")                                 # the micronizer
PF.box((2.4, 0.3, 8.6), (3.8, 2.2, 9.8), "galv")                                  # its baghouse
stub(PF, (4.0, 0.5, 1.0), (3.6, 0.5, 1.0), 3, "steel")                            # steam in, the east
box480(PF, 0.4, 3.0)
PF.collide((0.2, 0.0, 0.2), (3.8, 2.6, 9.8))


M = TC
EXTRA = [TP, TO, VO, PF]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
