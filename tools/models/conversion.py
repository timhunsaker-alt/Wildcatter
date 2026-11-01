"""The conversion plant at true size (docs/multiblocks/conversion.md), each part drawn facing north from its own main cell (the
footprints match ConversionPartBlock's):
- reduction_reactor, 3 x 10 x 3: the fluid-bed reducer, its drum tipper on top, the hydrogen line on the east;
- hydrofluorinator, 3 x 10 x 6: two fluid beds in series and their KOH scrubber;
- fluorinator, 3 x 10 x 3: the fluid bed with its CaF2 bed and filters;
- cold_traps, 3 x 4 x 6: three jacketed traps on a skid with the refrigeration unit (480 V box west);
- distillation_columns, 3 x 12 x 3: the low and high boiler columns, the reboiler (480 V box west);
- cylinder_filler, 3 x 3 x 6: a 48Y cylinder on its scale under the fill hood (480 V box west);
- steam_reformer, 3 x 8 x 4: the packaged reformer's firebox and stack, the shift vessel, the PSA beds;
- hf_kiln, 3 x 4 x 10: the pre-reactor and the brick-lined rotary kiln on its tyres and rollers, its drive (480 V box west);
- hf_column, 2 x 10 x 2: the scrubbing and purification column with its condenser;
- rectifier, 3 x 4 x 4: the rectifier transformer and the diode cubicle, bus bars on its east (4.16 kV box west);
- fluorine_cell_house, 6 x 4 x 12: the cell house, its roof vents, the HF line in front and the fluorine line out the back."""
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


# ---- the reduction reactor ------------------------------------------------------------------------------------------------------
RR = model("reduction_reactor", 3, 10, 3, "steel")
RR.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
vessel(RR, 1.5, 1.5, 0.9, 0.3, 7.5)
RR.cylinder("y", (1.5, 1.5), 1.2, 7.5, 8.6, "steel", 16)                    # the expanded disengaging section
RR.box((0.7, 8.6, 0.7), (2.3, 9.9, 2.3), "galv")                            # the drum tipper and feed hopper
RR.pipe([(3.0, 0.5, 1.5), (2.4, 0.5, 1.5)], 2, "steel")                     # hydrogen in, the east
RR.ladder(0.2, 0.2, 0.3, 9.5, -1, "galv")
RR.collide((0.6, 0.0, 0.6), (2.4, 9.9, 2.4))

# ---- the hydrofluorinator -------------------------------------------------------------------------------------------------------
HF = model("hydrofluorinator", 3, 10, 6, "steel")
HF.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "concrete")
for z in (1.5, 4.5):
	vessel(HF, 1.5, z, 0.85, 0.3, 8.8)                                       # the two beds in series
HF.pipe([(1.5, 9.1, 1.5), (1.5, 9.5, 1.5), (1.5, 9.5, 4.5), (1.5, 9.1, 4.5)], 4, "steel")
HF.cylinder("y", (2.5, 3.0), 0.3, 0.3, 4.0, "padgreen", 10)                 # its KOH scrubber
HF.pipe([(3.0, 0.5, 3.0), (2.8, 0.5, 3.0)], 2, "steel")                     # HF in, the east
HF.collide((0.6, 0.0, 0.6), (2.4, 9.1, 5.4))

# ---- the fluorinator --------------------------------------------------------------------------------------------------------------
FL = model("fluorinator", 3, 10, 3, "steel")
FL.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
vessel(FL, 1.5, 1.5, 0.95, 0.3, 7.0)
for x in (0.9, 2.1):
	FL.cylinder("y", (x, 1.5), 0.35, 7.3, 9.6, "steel", 12)                 # its sintered metal filters
FL.pipe([(3.0, 0.5, 1.5), (2.45, 0.5, 1.5)], 2, "steel")                    # fluorine in, the east
FL.collide((0.5, 0.0, 0.5), (2.5, 9.6, 2.5))

# ---- the cold traps ---------------------------------------------------------------------------------------------------------------
CT = model("cold_traps", 3, 4, 6, "lagging")
CT.box((0.05, 0.0, 0.05), (2.95, 0.2, 5.95), "iron")
for z in (1.0, 2.6, 4.2):
	CT.cylinder("y", (1.7, z), 0.6, 0.2, 3.4, "lagging", 14)                # primary, secondary, tertiary
CT.box((0.3, 0.2, 5.0), (2.7, 1.6, 5.9), "blue")                            # the refrigeration unit
box480(CT, 0.5, 0.3)
CT.collide((0.3, 0.0, 0.3), (2.8, 3.4, 5.9))

# ---- the distillation columns -------------------------------------------------------------------------------------------------------
DC = model("distillation_columns", 3, 12, 3, "lagging")
DC.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
DC.cylinder("y", (1.0, 1.0), 0.45, 0.3, 11.5, "lagging", 14)                # the low boiler column
DC.cylinder("y", (2.1, 2.0), 0.45, 0.3, 10.5, "lagging", 14)                # the high boiler column
DC.box((1.8, 0.3, 0.3), (2.8, 1.2, 1.2), "steel")                           # the reboiler
box480(DC, 0.5, 1.8)
DC.collide((0.5, 0.0, 0.5), (2.6, 11.5, 2.5))

# ---- the cylinder filler ------------------------------------------------------------------------------------------------------------
CF = model("cylinder_filler", 3, 3, 6, "grey")
CF.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "checker")                         # the scale deck
CF.cylinder("z", (1.5, 0.95), 0.61, 1.0, 5.0, "padgreen", 16)               # a 48Y cylinder, 48 in across, on its cradles
for z in (1.6, 4.4):
	CF.box((0.8, 0.3, z - 0.2), (2.2, 0.5, z + 0.2), "iron")
CF.box((1.1, 1.6, 1.0), (1.9, 2.9, 1.8), "grey")                            # the fill hood over its valve
box480(CF, 0.5, 0.3)
CF.collide((0.0, 0.0, 0.0), (3.0, 1.6, 6.0))

# ---- the steam reformer ------------------------------------------------------------------------------------------------------------
SR = model("steam_reformer", 3, 8, 4, "hipaint")
SR.box((0.05, 0.0, 0.05), (2.95, 0.2, 3.95), "iron")
SR.box((0.2, 0.2, 0.2), (1.6, 3.5, 1.8), "hipaint")                         # the firebox and its tubes
SR.cylinder("y", (0.9, 1.0), 0.25, 3.5, 7.8, "hipaint", 12)                 # its stack
SR.cylinder("y", (2.3, 0.9), 0.35, 0.2, 3.0, "steel", 12)                   # the shift vessel
for z in (2.4, 3.3):
	SR.cylinder("y", (0.9, z), 0.35, 0.2, 2.8, "padgreen", 12)              # the PSA beds
SR.pipe([(3.0, 0.5, 2.0), (2.6, 0.5, 2.0)], 2, "steel")                     # fuel in, the east
SR.pipe([(1.5, 0.5, 3.6), (1.5, 0.5, 4.0)], 2, "steel")                     # hydrogen out, the back
SR.collide((0.2, 0.0, 0.2), (2.8, 3.5, 3.8))

# ---- the HF kiln ----------------------------------------------------------------------------------------------------------------
HK = model("hf_kiln", 3, 4, 10, "iron")
for z in (1.5, 5.0, 8.5):
	HK.box((0.4, 0.0, z - 0.4), (2.6, 0.9, z + 0.4), "concrete")            # its piers and rollers
HK.cylinder("z", (1.5, 1.8), 1.0, 1.0, 9.6, "hipaint", 18)                  # the rotary kiln, in its heating jacket
for z in (2.5, 5.0, 7.5):
	HK.ring("z", (1.5, 1.8), 1.05, z, z + 0.25, 0.08, "iron", sides=18)     # its tyres
HK.box((0.6, 0.9, 0.0), (2.4, 3.6, 1.0), "steel")                           # the pre-reactor
HK.box((2.3, 0.9, 4.0), (2.9, 1.5, 5.0), "blue")                            # the drive
HK.pipe([(3.0, 0.4, 3.0), (2.5, 0.4, 3.0)], 2, "steel")                     # acid and fuel in, the east
box480(HK, 0.4, 0.2)
HK.collide((0.4, 0.0, 0.0), (2.6, 3.6, 9.6))

# ---- the HF column --------------------------------------------------------------------------------------------------------------
HC = model("hf_column", 2, 10, 2, "steel")
HC.box((0.0, 0.0, 0.0), (2.0, 0.25, 2.0), "concrete")
HC.cylinder("y", (1.0, 1.0), 0.5, 0.25, 9.4, "steel", 14)
HC.box((0.2, 7.5, 0.2), (0.6, 9.0, 0.6), "finned")                          # its condenser
HC.pipe([(1.0, 0.4, 1.5), (1.0, 0.4, 2.0)], 2, "steel")                     # anhydrous HF out, the south
HC.collide((0.45, 0.0, 0.45), (1.55, 9.4, 1.55))

# ---- the rectifier --------------------------------------------------------------------------------------------------------------
RC = model("rectifier", 3, 4, 4, "grey")
RC.box((0.0, 0.0, 0.0), (3.0, 0.2, 4.0), "concrete")
RC.box((0.3, 0.2, 0.3), (2.0, 2.4, 2.0), "grey")                            # the rectifier transformer
RC.fins((0.25, 0.4, 0.3), (0.3, 2.2, 2.0), "z", 8, 0.02, "radfin")
RC.box((0.3, 0.2, 2.2), (2.6, 3.0, 3.8), "grey")                            # the diode cubicle
RC.decal((0.29, 1.2, 2.6), (0.3, 1.8, 3.2), "danger", "west", "grey")
for y in (0.8, 1.4):
	RC.box((2.6, y, 2.6), (3.0, y + 0.12, 3.4), "copper")                   # the DC bus bars, east
RC.box((0.0, 0.6, 0.4), (0.3, 1.4, 1.0), "grey")                            # its 4.16 kV box, west
RC.collide((0.3, 0.0, 0.3), (2.6, 3.0, 3.8))

# ---- the fluorine cell house ----------------------------------------------------------------------------------------------------------
CH = model("fluorine_cell_house", 6, 4, 12, "louvre")
CH.box((0.0, 0.0, 0.0), (6.0, 0.2, 12.0), "concrete")
CH.box((0.2, 0.2, 0.2), (5.8, 3.4, 11.8), "louvre")                         # the cell house
CH.box((0.1, 3.4, 0.1), (5.9, 3.55, 11.9), "grey")
for z in (2.0, 5.0, 8.0, 11.0):
	CH.box((2.6, 3.55, z - 0.4), (3.4, 3.95, z + 0.4), "galv")              # its roof vents
for y in (0.8, 1.4):
	CH.box((0.0, y, 5.6), (0.2, y + 0.12, 6.4), "copper")                   # the DC bus bars in, west
CH.pipe([(3.0, 0.5, 0.0), (3.0, 0.5, 0.2)], 2, "steel")                     # anhydrous HF in, the front
CH.pipe([(3.0, 0.5, 11.8), (3.0, 0.5, 12.0)], 2, "steel")                   # fluorine out, the back
CH.collide((0.2, 0.0, 0.2), (5.8, 3.55, 11.8))


M = RR
EXTRA = [HF, FL, CT, DC, CF, SR, HK, HC, RC, CH]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
