"""Phase 20 at true size (docs/multiblocks/chlor-alkali-plant.md), each part drawn facing north from its own main cell (the footprints
match ChemPartBlock's and ConversionPartBlock's). The motor box is on the west face of the main cell, or on its north face where a part
against it covers the west:
- brine_saturator, 4 x 5 x 4: the salt dissolver tank, its feed chute and agitator, the brine pumps; well brine in on the north;
- brine_treatment, 4 x 6 x 8: the reactor tanks, the clarifier, the polishing filters and the two chelating IX columns; CO2 in south;
- membrane_cell_room, 6 x 4 x 16: the cell room building over two filter-press electrolysers, its bus bars on the east (z 8), and the
  chlorine, hydrogen and caustic headers out of the south;
- rectifier_mv, 3 x 4 x 4: a 12.47 kV rectifier transformer with its thyristor cubicle and DC bus stubs on the east;
- chlorine_train, 4 x 6 x 10: the cooler, two drying towers, the compressor (medium-voltage box west) and the liquefier (480 V box north);
- caustic_evaporator, 3 x 10 x 3: two falling-film effects, their separators and the condenser;
- hcl_synthesis_unit, 2 x 8 x 2: the graphite burner and absorber column, its tail tower on top."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.4, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


def box_mv(M, y=0.3, z=0.15):
	M.box((0.0, y, z), (0.4, y + 0.8, z + 0.7), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.6), "arcflash", "west", "grey")


def box_north(M, y=0.4, x=0.2):
	M.box((x, y, 0.0), (x + 0.5, y + 0.6, 0.3), "grey")
	M.decal((x + 0.1, y + 0.1, -0.01), (x + 0.4, y + 0.4, 0.0), "danger", "north", "grey")


def vessel(M, cx, cz, r, y0, y1, tex="steel", sides=16):
	M.cylinder("y", (cx, cz), r, y0, y1, tex, sides)
	M.cylinder("y", (cx, cz), r * 0.6, y1, y1 + 0.2, tex, 12)


def stub(M, a, b, nps=2, tex="steel"):
	M.pipe([a, b], nps, tex)


def drive(M, cx, cz, y, tex="blue"):
	M.box((cx - 0.3, y, cz - 0.3), (cx + 0.3, y + 0.35, cz + 0.3), "engine")
	M.cylinder("y", (cx, cz), 0.18, y + 0.35, y + 0.95, tex, 12)


# ---- the brine saturator --------------------------------------------------------------------------------------------------------
BS = model("brine_saturator", 4, 5, 4, "polymer")
BS.box((0.0, 0.0, 0.0), (4.0, 0.3, 4.0), "concrete")
vessel(BS, 1.5, 1.5, 1.4, 0.3, 3.6, "polymer", 18)                              # the dissolver, FRP
BS.ring("y", (1.5, 1.5), 1.42, 2.0, 2.12, 0.05, "galv", sides=18)
drive(BS, 1.5, 1.5, 3.8)
BS.box((0.5, 3.8, 0.4), (1.4, 4.8, 1.3), "galv")                               # the salt feed chute under its hopper
BS.pipe([(0.95, 3.8, 0.85), (0.95, 3.4, 0.85)], 6, "galv")
BS.box((2.9, 0.3, 3.0), (3.8, 1.0, 3.8), "blue")                                # the brine pumps
BS.pipe([(3.35, 1.0, 3.4), (3.35, 1.6, 3.4), (3.35, 1.6, 2.9)], 3, "polymer")
stub(BS, (2.0, 0.5, 0.0), (2.0, 0.5, 0.4), 3, "polymer")                         # well brine in, the north
box480(BS, 0.4, 2.2)
BS.collide((0.2, 0.0, 0.2), (3.8, 3.8, 3.8))

# ---- the brine treatment --------------------------------------------------------------------------------------------------------
BT = model("brine_treatment", 4, 6, 8, "polymer")
BT.box((0.0, 0.0, 0.0), (4.0, 0.3, 8.0), "concrete")
for cz in (1.0, 2.6):
	vessel(BT, 1.0, cz, 0.7, 0.3, 2.4, "polymer", 14)                           # the reactor tanks: caustic, then carbonate
	drive(BT, 1.0, cz, 2.6)
vessel(BT, 2.8, 2.5, 1.1, 0.3, 1.6, "concrete", 24)                              # the clarifier
BT.box((1.7, 1.6, 2.4), (3.9, 1.75, 2.6), "galv")                               # its bridge
for cz in (4.3, 5.3):
	BT.cylinder("x", (1.0, cz), 0.4, 0.4, 3.6, "steel", 12)                     # the polishing filters
for cx in (1.0, 2.8):
	vessel(BT, cx, 6.8, 0.6, 0.3, 5.4, "rubber", 14)                             # the two chelating IX columns, rubber-lined
BT.pipe([(1.0, 5.6, 6.8), (1.0, 5.8, 6.8), (2.8, 5.8, 6.8), (2.8, 5.6, 6.8)], 2, "polymer")
BT.box((0.2, 0.3, 7.6), (1.0, 1.0, 7.95), "blue")                               # the IX regeneration pumps
stub(BT, (2.0, 0.5, 7.6), (2.0, 0.5, 8.0), 2, "steel")                           # liquid CO2 in, the south
box_north(BT, 0.4, 0.1)
BT.collide((0.2, 0.0, 0.2), (3.8, 2.4, 7.8))

# ---- the membrane cell room -----------------------------------------------------------------------------------------------------
CR = model("membrane_cell_room", 6, 4, 16, "grey")
CR.box((0.0, 0.0, 0.0), (6.0, 0.3, 16.0), "concrete")
for z0, z1 in ((0.6, 7.4), (8.6, 15.4)):
	CR.box((1.0, 0.3, z0), (5.0, 0.6, z1), "iron")                               # the electrolyser's frame
	CR.box((1.3, 0.6, z0 + 0.2), (4.7, 2.2, z1 - 0.2), "dark")                   # the stack of elements
	for k in range(13):
		z = z0 + 0.4 + k * (z1 - z0 - 0.8) / 12
		CR.box((1.2, 0.6, z), (4.8, 2.3, z + 0.08), "polymer")                   # its element frames
	CR.box((1.1, 2.3, z0), (4.9, 2.5, z1), "iron")                               # the tie rods' top beam
for x, tex in ((1.6, "padgreen"), (3.0, "steel"), (4.4, "blue")):
	CR.pipe([(x, 2.7, 0.8), (x, 2.7, 15.6)], 3, tex)                             # the chlorine, hydrogen and caustic headers
CR.box((0.0, 0.3, 0.0), (6.0, 3.6, 0.15), "louvre")                              # the building's end walls
CR.box((0.0, 3.6, 0.0), (6.0, 3.8, 16.0), "grey")                                # its roof
for z in (2.0, 6.0, 10.0, 14.0):
	CR.box((2.6, 3.8, z), (3.4, 3.98, z + 0.8), "louvre")                        # roof vents
for x in (0.0, 5.85):
	for z in (0.0, 4.0, 8.0, 12.0, 15.85):
		CR.box((x, 0.3, z), (x + 0.15, 3.6, z + 0.15), "iron")                   # the columns
CR.box((5.2, 0.4, 7.7), (6.0, 0.7, 8.3), "copper")                               # DC bus bars, - at y 0
CR.box((5.2, 2.4, 7.7), (6.0, 2.7, 8.3), "copper")                               # and + at y 2, the east face
CR.pipe([(1.6, 2.7, 15.6), (0.5, 2.7, 15.6), (0.5, 0.5, 15.6), (0.5, 0.5, 16.0)], 3, "padgreen")   # wet chlorine out, x 0
CR.pipe([(3.0, 2.7, 15.6), (2.5, 2.7, 15.6), (2.5, 0.5, 15.6), (2.5, 0.5, 16.0)], 3, "steel")      # hydrogen, x 2
CR.pipe([(4.4, 2.7, 15.6), (4.5, 2.7, 15.6), (4.5, 0.5, 15.6), (4.5, 0.5, 16.0)], 2, "blue")       # caustic, x 4
CR.decal((5.99, 1.0, 9.0), (6.0, 1.6, 9.8), "danger", "east", "grey")
CR.collide((0.0, 0.0, 0.0), (6.0, 3.8, 16.0))

# ---- the 12.47 kV rectifier ----------------------------------------------------------------------------------------------------
RM = model("rectifier_mv", 3, 4, 4, "grey")
RM.box((0.0, 0.0, 0.0), (3.0, 0.2, 4.0), "concrete")
RM.box((0.3, 0.2, 0.2), (2.3, 2.8, 2.2), "grey")                                # the rectifier transformer, oil-filled
RM.fins((0.25, 0.4, 0.3), (0.3, 2.6, 2.1), "z", 9, 0.02, "radfin")
RM.fins((0.4, 0.4, 0.15), (2.2, 2.6, 0.2), "x", 8, 0.02, "radfin")
for x in (0.8, 1.3, 1.8):
	RM.cylinder("y", (x, 0.6), 0.09, 2.8, 3.6, "polymer", 10)                    # its 12.47 kV bushings
RM.cylinder("y", (1.5, 1.6), 0.35, 2.8, 3.2, "steel", 12)                       # the conservator
RM.box((0.3, 0.2, 2.4), (2.6, 3.2, 3.8), "grey")                                # the thyristor cubicle
RM.decal((0.29, 1.2, 2.7), (0.3, 1.8, 3.4), "danger", "west", "grey")
RM.box((2.6, 0.3, 1.7), (3.0, 0.6, 2.3), "copper")                              # DC -, the east at z 2
RM.box((2.6, 2.3, 1.7), (3.0, 2.6, 2.3), "copper")                              # DC +
box_mv(RM, 0.6, 0.6)
RM.collide((0.3, 0.0, 0.2), (2.6, 3.2, 3.8))

# ---- the chlorine train ---------------------------------------------------------------------------------------------------------
CT = model("chlorine_train", 4, 6, 10, "steel")
CT.box((0.0, 0.0, 0.0), (4.0, 0.3, 10.0), "concrete")
CT.cylinder("z", (2.0, 1.2), 0.5, 0.4, 2.4, "padgreen", 12)                     # the chlorine cooler (titanium tubes)
for cz in (3.4, 5.2):
	vessel(CT, 1.6, cz, 0.7, 0.3, 5.5, "polymer", 14)                            # the two drying towers
CT.box((2.6, 0.3, 2.8), (3.8, 1.2, 4.0), "steel")                               # the acid coolers and circulation pumps
CT.box((0.4, 0.3, 6.6), (2.6, 1.6, 8.0), "engine")                              # the compressor
CT.box((0.4, 0.3, 8.2), (1.6, 1.4, 9.6), "blue")                                # its motor
CT.box((2.8, 0.3, 6.4), (3.8, 2.2, 9.6), "hipaint")                             # the refrigerated liquefier
CT.pipe([(1.6, 5.7, 3.4), (1.6, 5.9, 3.4), (1.6, 5.9, 5.2), (1.6, 5.7, 5.2)], 3, "padgreen")
CT.pipe([(2.0, 1.2, 2.4), (2.0, 1.2, 2.8)], 4, "padgreen")
stub(CT, (2.0, 0.5, 0.0), (2.0, 0.5, 0.7), 4, "padgreen")                        # wet chlorine in, the north
stub(CT, (4.0, 0.5, 1.5), (3.6, 0.5, 1.5), 2, "steel")                           # acid in, the east
stub(CT, (2.0, 0.5, 9.6), (2.0, 0.5, 10.0), 3, "padgreen")                       # dry chlorine out, the south
box_mv(CT, 0.4, 0.1)
box_north(CT, 0.4, 0.6)
CT.ladder(0.15, 4.5, 0.3, 5.5, 1, "galv")
CT.collide((0.2, 0.0, 0.2), (3.8, 2.4, 9.8))

# ---- the caustic evaporator -----------------------------------------------------------------------------------------------------
CE = model("caustic_evaporator", 3, 10, 3, "steel")
CE.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
CE.cylinder("y", (0.8, 0.8), 0.45, 0.3, 7.0, "steel", 14)                       # the first effect (nickel tubes)
CE.cylinder("y", (2.2, 0.8), 0.45, 0.3, 6.0, "steel", 14)                       # the second
CE.cylinder("y", (0.8, 2.2), 0.55, 0.3, 3.0, "steel", 14)                       # the separators
CE.cylinder("y", (2.2, 2.2), 0.55, 0.3, 2.6, "steel", 14)
CE.cylinder("y", (1.5, 1.5), 0.3, 7.2, 9.8, "steel", 12)                        # the surface condenser
CE.pipe([(0.8, 7.0, 0.8), (0.8, 7.5, 0.8), (1.5, 7.5, 0.8), (1.5, 7.5, 1.5)], 4, "steel")
CE.pipe([(2.2, 6.0, 0.8), (2.2, 6.5, 0.8), (2.2, 6.5, 2.2), (2.2, 2.6, 2.2)], 4, "lagging")
stub(CE, (1.5, 0.5, 0.0), (1.5, 0.5, 0.35), 2, "blue")                           # 32 % in, the north
stub(CE, (3.0, 0.5, 1.5), (2.6, 0.5, 1.5), 3, "steel")                           # steam in, the east
stub(CE, (1.5, 0.5, 2.6), (1.5, 0.5, 3.0), 2, "blue")                            # 50 % out, the south
CE.ladder(0.1, 1.5, 0.3, 7.0, -1, "galv")
CE.collide((0.3, 0.0, 0.3), (2.7, 7.0, 2.7))

# ---- the HCl synthesis unit -----------------------------------------------------------------------------------------------------
HC = model("hcl_synthesis_unit", 2, 8, 2, "dark")
HC.box((0.0, 0.0, 0.0), (2.0, 0.3, 2.0), "concrete")
HC.cylinder("y", (1.0, 1.0), 0.6, 0.3, 4.0, "dark", 16)                          # the graphite burner and absorber
HC.ring("y", (1.0, 1.0), 0.62, 1.5, 1.6, 0.04, "iron", sides=16)
HC.ring("y", (1.0, 1.0), 0.62, 3.0, 3.1, 0.04, "iron", sides=16)
HC.cylinder("y", (1.0, 1.0), 0.35, 4.0, 7.6, "polymer", 12)                      # the tail tower
HC.box((0.1, 0.3, 0.1), (0.5, 0.9, 0.5), "rubber")                               # the burner's sight glass and igniter
stub(HC, (1.0, 0.5, 0.0), (1.0, 0.5, 0.4), 2, "steel")                           # hydrogen in, the north
stub(HC, (2.0, 0.5, 1.5), (1.6, 0.5, 1.5), 2, "padgreen")                        # chlorine in, the east
stub(HC, (0.5, 0.5, 1.6), (0.5, 0.5, 2.0), 2, "polymer")                         # acid out, the south
stub(HC, (0.0, 0.8, 1.5), (0.4, 0.8, 1.5), 2, "steel")                           # hydrogen on, the west
HC.collide((0.4, 0.0, 0.4), (1.6, 7.6, 1.6))

# ---- the brine wellhead (one cell) ----------------------------------------------------------------------------------------------
BW = mlib.Model("brine_wellhead", [(0, 0, 0)], dict(TEX), TEX["padgreen"])
BW.cylinder("y", (0.5, 0.5), 0.14, -0.2, 0.05, "steel", 12, caps=False)         # the casing
BW.cylinder("y", (0.5, 0.5), 0.17, 0.05, 0.4, "padgreen", 14)                   # the casing head: brine up the annulus
BW.box((0.6, 0.15, 0.42), (1.0, 0.31, 0.58), "polymer")                         # the brine wing, out of the side it faces (north)
BW.pipe([(0.5, 0.23, 0.42), (0.5, 0.23, 0.0)], 3, "polymer")
BW.cylinder("y", (0.5, 0.5), 0.1, 0.4, 0.75, "padgreen", 12)                     # the tubing head: water down the inner string
BW.box((0.42, 0.75, 0.42), (0.58, 0.95, 0.58), "steel")
BW.pipe([(0.42, 0.85, 0.5), (0.0, 0.85, 0.5)], 3, "steel")                     # the water wing
BW.cylinder("y", (0.5, 0.5), 0.06, 0.95, 1.04, "iron", 10)
BW.collide((0.3, 0.0, 0.3), (0.7, 1.04, 0.7))


M = BS
EXTRA = [BT, CR, RM, CT, CE, HC]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
BW.export_args = {"blockstate": False, "item": True}
EXTRA.append(BW)


def after():
	import gearlib
	gearlib.save_blockstate("brine_wellhead", {f"facing={facing}": {"model": "wildcatter:block/brine_wellhead/p0", **({"y": y} if y else {})} for facing, y in gearlib.ROT.items()})
