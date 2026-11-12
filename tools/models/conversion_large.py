"""16b2, the full-size conversion plant's larger main line (docs/multiblocks/conversion-extensions.md), each part drawn facing north
from its own main cell, twice the height of its 7d part and four times its rating:
- reduction_reactor_large, 3 x 20 x 3: a wider, taller fluid-bed reducer, its disengaging section and drum tipper; hydrogen east;
- hydrofluorinator_large, 3 x 20 x 6: two taller beds in series and a bigger KOH scrubber; HF east;
- fluorinator_large, 3 x 20 x 3: the taller fluid-bed fluorinator and four sintered metal filters; fluorine east;
- cold_traps_large, 3 x 8 x 6: three tall traps in a double row and two refrigeration units (480 V box west);
- distillation_columns_large, 3 x 24 x 3: the low and high boiler columns at twice the height and two reboilers (480 V box west)."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


def vessel(M, cx, cz, r, y0, y1, tex="steel"):
	M.cylinder("y", (cx, cz), r, y0, y1, tex, 16)
	M.cylinder("y", (cx, cz), r * 0.6, y1, y1 + 0.3, tex, 12)


def platforms(M, ys, x0, z0, x1, z1):
	for y in ys:
		M.box((x0, y, z0), (x1, y + 0.08, z1), "grating")


# ---- the large reduction reactor --------------------------------------------------------------------------------------------------
RR = model("reduction_reactor_large", 3, 20, 3, "steel")
RR.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
vessel(RR, 1.5, 1.5, 1.1, 0.3, 15.0)
RR.cylinder("y", (1.5, 1.5), 1.4, 15.0, 17.2, "steel", 16)                   # the expanded disengaging section
RR.box((0.6, 17.2, 0.6), (2.4, 19.8, 2.4), "galv")                           # the drum tipper and feed hopper
for y in (4.0, 8.0, 12.0):
	RR.ring("y", (1.5, 1.5), 1.12, y, y + 0.12, 0.05, "galv", sides=16)
RR.pipe([(3.0, 0.5, 1.5), (2.5, 0.5, 1.5)], 3, "steel")                      # hydrogen in, the east
RR.ladder(0.15, 0.15, 0.3, 19.0, -1, "galv")
RR.collide((0.4, 0.0, 0.4), (2.6, 19.8, 2.6))

# ---- the large hydrofluorinator ---------------------------------------------------------------------------------------------------
HF = model("hydrofluorinator_large", 3, 20, 6, "steel")
HF.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "concrete")
for z in (1.5, 4.5):
	vessel(HF, 1.4, z, 1.1, 0.3, 17.6)                                        # the two beds in series
HF.pipe([(1.4, 17.9, 1.5), (1.4, 18.6, 1.5), (1.4, 18.6, 4.5), (1.4, 17.9, 4.5)], 6, "steel")
HF.cylinder("y", (2.6, 3.0), 0.35, 0.3, 8.0, "padgreen", 10)                 # its KOH scrubber
HF.pipe([(3.0, 0.5, 3.0), (2.9, 0.5, 3.0)], 3, "steel")                      # HF in, the east
platforms(HF, (6.0, 12.0), 0.1, 0.1, 2.9, 5.9)
HF.collide((0.3, 0.0, 0.3), (2.6, 18.6, 5.7))

# ---- the large fluorinator --------------------------------------------------------------------------------------------------------
FL = model("fluorinator_large", 3, 20, 3, "steel")
FL.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
vessel(FL, 1.5, 1.5, 1.2, 0.3, 14.0)
for x, z in ((0.8, 0.8), (2.2, 0.8), (0.8, 2.2), (2.2, 2.2)):
	FL.cylinder("y", (x, z), 0.35, 14.3, 19.4, "steel", 12)                  # its sintered metal filters
FL.pipe([(3.0, 0.5, 1.5), (2.7, 0.5, 1.5)], 3, "steel")                      # fluorine in, the east
FL.ladder(0.1, 2.6, 0.3, 14.0, 1, "galv")
FL.collide((0.3, 0.0, 0.3), (2.7, 19.4, 2.7))

# ---- the large cold traps ---------------------------------------------------------------------------------------------------------
CT = model("cold_traps_large", 3, 8, 6, "lagging")
CT.box((0.05, 0.0, 0.05), (2.95, 0.2, 5.95), "iron")
for z in (0.9, 2.4, 3.9):
	CT.cylinder("y", (1.7, z), 0.65, 0.2, 7.0, "lagging", 14)                # primary, secondary, tertiary, twice the height
	CT.ring("y", (1.7, z), 0.67, 3.5, 3.6, 0.04, "galv", sides=14)
CT.box((0.3, 0.2, 4.9), (2.7, 1.6, 5.9), "blue")                             # the two refrigeration units
CT.box((0.3, 1.7, 4.9), (2.7, 3.1, 5.9), "blue")
box480(CT, 0.5, 0.3)
CT.collide((0.3, 0.0, 0.2), (2.8, 7.0, 5.9))

# ---- the large distillation columns ------------------------------------------------------------------------------------------------
DC = model("distillation_columns_large", 3, 24, 3, "lagging")
DC.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
DC.cylinder("y", (1.0, 1.0), 0.55, 0.3, 23.5, "lagging", 14)                 # the low boiler column
DC.cylinder("y", (2.1, 2.0), 0.55, 0.3, 21.5, "lagging", 14)                 # the high boiler column
for y in (6.0, 12.0, 18.0):
	DC.ring("y", (1.0, 1.0), 0.57, y, y + 0.12, 0.04, "galv", sides=14)
DC.box((1.8, 0.3, 0.2), (2.9, 1.2, 1.2), "steel")                            # the reboilers
DC.box((0.2, 0.3, 1.9), (1.2, 1.2, 2.9), "steel")
box480(DC, 0.5, 0.9)
DC.collide((0.4, 0.0, 0.4), (2.7, 23.5, 2.6))


M = RR
EXTRA = [HF, FL, CT, DC]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
