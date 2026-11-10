"""Phase 18 at true size (docs/multiblocks/fertilizer.md, uranium-from-phosphate.md), each part drawn facing north from its own main
cell (the footprints match PhosphatePartBlock's). The motor box is on the west face of the main cell (4.16 kV on the mill and the
filter, 480 V on the rest), or on its north face where a part against it covers the west:
- phosphate_mill, 3 x 4 x 6: a wet ball mill on its trunnions, girth gear and motor; rock slurry out of the south;
- phosphoric_reactor, 8 x 6 x 8: the concrete attack tank with four agitators and the flash cooler; slurry in north, acid in south;
- tilting_pan_filter, 6 x 4 x 10: the ring of tilting pans on its frame, the vacuum receivers and pump; acid south, gypsum west;
- acid_evaporator, 3 x 10 x 3: the heat exchanger, the flash chamber, the circulation pump and the barometric condenser;
- granulation_plant, 4 x 5 x 12: the pre-neutraliser, the granulation drum, the dryer drum with its burner, the screens and stack;
- potash_dissolver, 4 x 5 x 4: the hot leach tank with its agitator and feed chute; steam south, salt tails west;
- vacuum_crystallizer, 3 x 10 x 3: the crystallizer body with its steam ejectors and condenser; steam south;
- potash_dryer, 8 x 4 x 3: the fluid-bed dryer, its air heater, the cyclone and the compactor; gas south;
- acid_pretreatment, 3 x 6 x 6: the clarifier, the acid cooler and the air oxidiser column; acid in north, out south;
- depa_topo_circuit, 16 x 3 x 6: two rows of mixer-settlers, their mixer drives and the organic tank; kerosene north, acid south."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.4, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


def box4160(M, y=0.3, z=0.15):
	"""A medium-voltage motor terminal box: bigger, with its arc-flash label."""
	M.box((0.0, y, z), (0.4, y + 0.8, z + 0.7), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.6), "arcflash", "west", "grey")


def box_north(M, y=0.4, x=0.2, mv=False):
	"""The motor box on the north face of the main cell, where a part against it covers the west."""
	w, h, tex = (0.7, 0.8, "arcflash") if mv else (0.5, 0.6, "danger")
	M.box((x, y, 0.0), (x + w, y + h, 0.3 if not mv else 0.4), "grey")
	M.decal((x + 0.1, y + 0.1, -0.01), (x + w - 0.1, y + h - 0.2, 0.0), tex, "north", "grey")


def vessel(M, cx, cz, r, y0, y1, tex="steel", sides=16):
	M.cylinder("y", (cx, cz), r, y0, y1, tex, sides)
	M.cylinder("y", (cx, cz), r * 0.6, y1, y1 + 0.2, tex, 12)


def stub(M, a, b, nps=2, tex="steel"):
	M.pipe([a, b], nps, tex)


def drive(M, cx, cz, y, tex="blue"):
	"""An agitator drive: the gearbox and motor on top, its shaft down."""
	M.box((cx - 0.3, y, cz - 0.3), (cx + 0.3, y + 0.35, cz + 0.3), "engine")
	M.cylinder("y", (cx, cz), 0.18, y + 0.35, y + 0.95, tex, 12)


# ---- the phosphate mill ----------------------------------------------------------------------------------------------------------
PM = model("phosphate_mill", 3, 4, 6, "iron")
PM.box((0.0, 0.0, 0.0), (3.0, 0.4, 6.0), "concrete")                         # its foundation
for z in (0.6, 4.4):
	PM.box((0.6, 0.4, z), (2.4, 1.4, z + 0.6), "concrete")                    # the trunnion piers
	PM.cylinder("z", (1.5, 1.7), 0.35, z - 0.2, z + 0.8, "iron", 12)           # the trunnions
PM.cylinder("z", (1.5, 1.7), 1.15, 1.2, 4.4, "engine", 18)                    # the shell
for z in (1.6, 2.6, 3.6):
	PM.ring("z", (1.5, 1.7), 1.17, z, z + 0.1, 0.05, "iron", sides=18)        # its liner bolt rows
PM.ring("z", (1.5, 1.7), 1.3, 3.9, 4.15, 0.12, "brass", sides=18)              # the girth gear
PM.box((0.7, 0.4, 5.1), (2.3, 1.6, 5.9), "blue")                               # the motor
PM.cylinder("z", (1.5, 1.0), 0.12, 4.4, 5.1, "iron", 8)                        # its pinion shaft
PM.box((1.0, 2.9, 0.1), (2.0, 3.8, 1.0), "galv")                               # the feed chute and rock hopper
PM.pipe([(1.5, 2.9, 0.6), (1.5, 1.7, 0.6)], 6, "galv")
PM.pipe([(3.0, 0.6, 2.0), (2.6, 0.6, 2.0), (2.6, 3.1, 2.0), (2.6, 3.1, 0.6), (2.0, 3.1, 0.6)], 1, "steel")   # water to the feed
PM.box((1.0, 0.4, 4.6), (2.0, 1.1, 5.0), "padgreen")                           # the discharge sump
stub(PM, (1.5, 0.5, 5.0), (1.5, 0.5, 6.0), 3, "rubber")                         # rock slurry out, the south
PM.decal((2.31, 0.7, 5.3), (2.32, 1.2, 5.7), "ratingplate", "east", "blue")
box4160(PM, 0.4, 0.1)
PM.collide((0.0, 0.0, 0.0), (3.0, 2.9, 6.0))

# ---- the phosphoric reactor ------------------------------------------------------------------------------------------------------
PR = model("phosphoric_reactor", 8, 6, 8, "concrete")
PR.box((0.0, 0.0, 0.0), (8.0, 0.3, 8.0), "concrete")
for x0, x1, z0, z1 in ((0.2, 7.8, 0.2, 0.6), (0.2, 7.8, 7.4, 7.8), (0.2, 0.6, 0.6, 7.4), (7.4, 7.8, 0.6, 7.4), (3.8, 4.2, 0.6, 7.4), (0.6, 7.4, 3.8, 4.2)):
	PR.box((x0, 0.3, z0), (x1, 3.4, z1), "concrete")                            # the tank walls and its four compartments
PR.box((0.6, 0.3, 0.6), (7.4, 2.9, 7.4), "padgreen")                            # the slurry inside, rubber-lined
PR.box((0.2, 3.4, 0.2), (7.8, 3.5, 7.8), "grating")                             # the top deck
for cx, cz in ((2.0, 2.0), (6.0, 2.0), (2.0, 6.0)):
	drive(PR, cx, cz, 3.5)
PR.box((4.6, 3.5, 4.6), (7.4, 3.8, 7.4), "iron")                                # the flash cooler's stand
vessel(PR, 5.5, 5.5, 1.1, 3.8, 5.6, "steel", 18)                                # the flash cooler
PR.pipe([(5.5, 5.8, 5.5), (5.5, 5.95, 5.5), (2.0, 5.95, 5.5), (2.0, 4.5, 5.5)], 4, "steel")   # its vapour to the condenser
PR.cylinder("y", (2.0, 5.5), 0.4, 3.5, 4.5, "steel", 12)                        # the condenser
PR.handrail([(0.2, 0.2), (7.8, 0.2), (7.8, 7.8), (0.2, 7.8), (0.2, 0.2)], 3.5, "galv")
stub(PR, (2.0, 0.6, 0.0), (2.0, 0.6, 0.2), 3, "rubber")                         # rock slurry in, the north
stub(PR, (4.0, 0.6, 7.8), (4.0, 0.6, 8.0), 2, "steel")                          # sulfuric acid in, the south
PR.ladder(0.05, 4.5, 0.3, 3.5, -1, "galv")
box480(PR, 0.4, 0.8)
PR.collide((0.0, 0.0, 0.0), (8.0, 3.5, 8.0))

# ---- the tilting pan filter ------------------------------------------------------------------------------------------------------
TF = model("tilting_pan_filter", 6, 4, 10, "steel")
TF.box((0.0, 0.0, 0.0), (6.0, 0.3, 10.0), "concrete")
for x in (0.6, 5.4):
	for z in (1.0, 4.0, 7.0):
		TF.box((x - 0.15, 0.3, z - 0.15), (x + 0.15, 1.6, z + 0.15), "iron")    # the frame's columns
TF.box((0.4, 1.6, 0.8), (5.6, 1.75, 7.2), "iron")                               # its ring girder
for (a, b) in ((0.6, 2.6), (3.4, 5.4)):
	for (c, d) in ((1.0, 3.9), (4.1, 7.0)):
		TF.box((a, 1.75, c), (b, 2.05, d), "steel")                              # the pans, round the ring
		TF.box((a + 0.1, 2.0, c + 0.1), (b - 0.1, 2.06, d - 0.1), "dark")         # the cake on them
TF.cylinder("y", (3.0, 4.0), 0.5, 1.75, 2.6, "engine", 12)                      # the central valve and drive
for x in (1.2, 4.8):
	TF.cylinder("y", (x, 8.4), 0.45, 0.3, 2.6, "steel", 14)                      # the vacuum receivers (filtrate and wash)
TF.box((2.0, 0.3, 8.0), (4.0, 1.5, 9.6), "blue")                                 # the vacuum pump
TF.pipe([(1.2, 2.6, 8.4), (1.2, 2.9, 8.4), (1.2, 2.9, 5.0), (2.0, 2.9, 5.0), (2.0, 2.06, 5.0)], 3, "steel")
TF.pipe([(4.8, 2.6, 8.4), (4.8, 2.9, 8.4), (4.8, 2.9, 5.0), (4.0, 2.9, 5.0), (4.0, 2.06, 5.0)], 3, "steel")
TF.box((0.4, 0.3, 7.4), (1.6, 1.2, 8.8), "padgreen")                             # the gypsum repulp tank
TF.box((0.6, 2.9, 0.4), (2.4, 3.3, 0.9), "galv")                                 # the wash headers' manifold
stub(TF, (3.0, 0.5, 9.6), (3.0, 0.5, 10.0), 2, "steel")                         # filter acid out, the south
stub(TF, (0.0, 0.5, 9.0), (0.4, 0.5, 9.0), 4, "rubber")                         # gypsum tails out, the west
box_north(TF, 0.3, 0.1, True)
TF.collide((0.0, 0.0, 0.0), (6.0, 2.1, 10.0))

# ---- the acid evaporator ---------------------------------------------------------------------------------------------------------
AE = model("acid_evaporator", 3, 10, 3, "steel")
AE.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
AE.cylinder("y", (0.7, 0.7), 0.4, 0.3, 4.2, "steel", 14)                        # the graphite-tube heat exchanger
vessel(AE, 1.6, 1.6, 1.1, 4.4, 8.4, "rubber", 18)                               # the flash chamber, rubber-lined
AE.ring("y", (1.6, 1.6), 1.12, 6.2, 6.32, 0.05, "galv", sides=18)
AE.pipe([(0.7, 4.2, 0.7), (0.7, 4.8, 0.7), (0.7, 4.8, 1.2)], 6, "steel")          # heated acid up into the chamber
AE.pipe([(2.4, 4.4, 2.4), (2.4, 1.2, 2.4)], 6, "rubber")                         # the downcomer
AE.box((1.6, 0.3, 1.9), (2.8, 1.2, 2.9), "blue")                                 # the circulation pump
AE.cylinder("y", (1.6, 1.6), 0.3, 8.6, 9.9, "steel", 12)                         # the barometric condenser
AE.pipe([(1.6, 9.5, 1.6), (2.7, 9.5, 1.6), (2.7, 0.6, 1.6)], 3, "steel")          # its tail leg
stub(AE, (1.0, 0.5, 0.0), (1.0, 0.5, 0.4), 2, "steel")                           # acid in, the north
stub(AE, (3.0, 0.5, 0.8), (2.7, 0.5, 0.8), 3, "steel")                           # steam in, the east
AE.pipe([(0.7, 0.5, 1.2), (0.7, 0.5, 2.6), (1.0, 0.5, 2.6), (1.0, 0.5, 3.0)], 2, "steel")   # merchant acid out, the south
AE.ladder(0.2, 2.6, 0.3, 8.4, -1, "galv")
box480(AE, 0.4, 1.4)
AE.collide((0.2, 0.0, 0.2), (2.8, 8.4, 2.8))

# ---- the granulation plant -------------------------------------------------------------------------------------------------------
GP = model("granulation_plant", 4, 5, 12, "engine")
GP.box((0.0, 0.0, 0.0), (4.0, 0.3, 12.0), "concrete")
vessel(GP, 2.0, 1.0, 0.6, 0.3, 2.4, "steel", 14)                                # the pre-neutraliser
drive(GP, 2.0, 1.0, 2.6)
for z0, z1, r, tex in ((2.0, 5.6, 1.1, "engine"), (6.4, 10.4, 1.15, "hipaint")):
	GP.cylinder("z", (1.5, 1.6), r, z0, z1, tex, 18)                             # the granulation drum, then the dryer drum
	for z in (z0 + 0.4, z1 - 0.6):
		GP.ring("z", (1.5, 1.6), r + 0.06, z, z + 0.2, 0.08, "iron", sides=18)   # their riding tyres
		GP.box((0.5, 0.3, z - 0.1), (2.5, 0.5, z + 0.3), "iron")                  # and trunnion rollers
GP.ring("z", (1.5, 1.6), 1.25, 3.6, 3.8, 0.1, "brass", sides=18)                # the drum's girth gear
GP.box((2.8, 0.3, 3.2), (3.8, 1.0, 4.2), "blue")                                 # its drive
GP.box((0.7, 0.3, 5.7), (2.3, 2.4, 6.3), "hipaint")                              # the dryer's burner and combustion chamber
GP.box((0.4, 0.3, 10.6), (3.6, 2.2, 11.8), "galv")                               # the screens and cooler
GP.cylinder("y", (0.9, 11.2), 0.3, 2.2, 4.9, "steel", 12)                        # the stack
GP.pipe([(1.5, 2.0, 5.6), (1.5, 2.0, 6.4)], 6, "steel")
for z, tex in ((1.0, "rubber"), (2.6, "steel"), (5.0, "steel"), (8.5, "steel")):
	stub(GP, (4.0, 0.5, z), (3.6, 0.5, z), 2, tex)                               # east: rock slurry, merchant acid, ammonia, gas
box480(GP, 0.4, 0.2)
GP.collide((0.0, 0.0, 0.0), (4.0, 2.8, 12.0))

# ---- the potash dissolver --------------------------------------------------------------------------------------------------------
PD = model("potash_dissolver", 4, 5, 4, "steel")
PD.box((0.0, 0.0, 0.0), (4.0, 0.3, 4.0), "concrete")
vessel(PD, 1.5, 1.5, 1.35, 0.3, 3.4, "lagging", 18)                              # the hot leach tank, insulated
drive(PD, 1.5, 1.5, 3.6)
PD.box((0.6, 3.6, 0.4), (1.6, 4.6, 1.4), "galv")                                 # the ore feed chute
PD.pipe([(1.1, 3.6, 0.9), (1.1, 3.3, 0.9)], 6, "galv")
PD.box((2.6, 0.3, 0.2), (3.6, 1.0, 0.9), "blue")                                 # the brine pump
stub(PD, (2.0, 0.5, 3.4), (2.0, 0.5, 4.0), 3, "steel")                           # steam in, the south
stub(PD, (0.0, 0.5, 2.5), (0.6, 0.5, 2.5), 3, "rubber")                          # salt tails out, the west
box480(PD, 0.4, 0.2)
PD.collide((0.2, 0.0, 0.2), (3.8, 3.6, 3.8))

# ---- the vacuum crystallizer -----------------------------------------------------------------------------------------------------
VC = model("vacuum_crystallizer", 3, 10, 3, "steel")
VC.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
VC.cylinder("y", (1.5, 1.5), 0.5, 0.3, 2.0, "steel", 14)                         # the cone and its draw-off
vessel(VC, 1.5, 1.5, 1.2, 2.0, 7.6, "steel", 18)                                 # the crystallizer body
for y in (3.5, 5.5):
	VC.ring("y", (1.5, 1.5), 1.22, y, y + 0.12, 0.05, "galv", sides=18)
VC.cylinder("y", (1.5, 1.5), 0.35, 7.8, 8.8, "steel", 12)                        # the vapour head
VC.cylinder("x", (9.1, 1.5), 0.15, 0.6, 2.6, "steel", 10)                        # the steam ejectors
VC.cylinder("y", (2.5, 1.5), 0.25, 7.5, 9.9, "steel", 10)                        # the condenser
VC.box((2.0, 0.3, 0.2), (2.9, 1.0, 1.0), "blue")                                 # the circulation pump
stub(VC, (1.5, 0.5, 2.6), (1.5, 0.5, 3.0), 3, "steel")                           # steam in, the south
VC.ladder(0.15, 0.4, 0.3, 7.6, 1, "galv")
box_north(VC, 0.4, 0.1)
VC.collide((0.2, 0.0, 0.2), (2.8, 7.6, 2.8))

# ---- the potash dryer ------------------------------------------------------------------------------------------------------------
PY = model("potash_dryer", 8, 4, 3, "grey")
PY.box((0.0, 0.0, 0.0), (8.0, 0.3, 3.0), "concrete")
PY.box((0.3, 0.3, 0.5), (5.2, 1.8, 2.5), "grey")                                 # the fluid-bed dryer
PY.box((0.4, 1.8, 0.7), (5.1, 2.0, 2.3), "louvre")                               # its hood
PY.box((5.4, 0.3, 0.4), (6.6, 1.4, 1.4), "engine")                                # the compactor rolls
PY.cylinder("x", (0.9, 1.6), 0.25, 5.4, 6.6, "iron", 10)
PY.box((6.6, 0.3, 0.6), (7.8, 2.0, 2.6), "hipaint")                              # the gas air heater
PY.cylinder("y", (2.0, 1.5), 0.45, 2.0, 3.0, "steel", 12)                        # the cyclone
PY.cylinder("y", (2.0, 1.5), 0.2, 3.0, 3.9, "steel", 10)                         # and its stack
PY.box((5.5, 0.3, 1.8), (6.4, 1.0, 2.8), "blue")                                 # the fluidizing fan
stub(PY, (7.2, 0.5, 2.6), (7.2, 0.5, 3.0), 2, "steel")                           # gas in, the south
box_north(PY, 0.4, 0.1)
PY.collide((0.0, 0.0, 0.0), (8.0, 2.0, 3.0))

# ---- the acid pretreatment -------------------------------------------------------------------------------------------------------
AP = model("acid_pretreatment", 3, 6, 6, "steel")
AP.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "concrete")
vessel(AP, 1.5, 1.5, 1.3, 0.3, 2.4, "rubber", 18)                                # the clarifier
AP.box((0.2, 2.4, 1.4), (2.8, 2.6, 1.6), "galv")                                 # its bridge
drive(AP, 1.5, 1.5, 2.6)
AP.box((0.4, 0.3, 3.0), (2.6, 1.6, 3.8), "radfin")                               # the acid cooler
AP.cylinder("y", (1.5, 4.9), 0.5, 0.3, 5.6, "steel", 14)                          # the air oxidiser column
AP.box((0.3, 0.3, 4.2), (0.9, 0.9, 4.8), "blue")                                 # its blower
stub(AP, (1.5, 0.5, 0.0), (1.5, 0.5, 0.3), 2, "steel")                           # filter acid in, the north
stub(AP, (1.5, 0.5, 5.4), (1.5, 0.5, 6.0), 2, "steel")                           # bypass out, the south
box480(AP, 0.4, 2.6)
AP.collide((0.2, 0.0, 0.2), (2.8, 2.6, 5.8))

# ---- the DEPA-TOPO circuit -------------------------------------------------------------------------------------------------------
DT = model("depa_topo_circuit", 16, 3, 6, "steel")
DT.box((0.0, 0.0, 0.0), (16.0, 0.3, 6.0), "concrete")
for row, z0 in enumerate((0.5, 3.3)):
	for k in range(5):
		x = 0.6 + k * 3.0
		DT.box((x, 0.3, z0), (x + 0.8, 1.5, z0 + 0.8), "polymer")                 # a mixer box
		drive(DT, x + 0.4, z0 + 0.4, 1.5, "blue")
		DT.box((x + 0.9, 0.3, z0), (x + 2.8, 1.1, z0 + 2.2), "polymer")           # its settler
		DT.box((x + 0.9, 1.1, z0), (x + 2.8, 1.15, z0 + 2.2), "grating")
DT.cylinder("y", (15.4, 2.9), 0.5, 0.3, 2.6, "steel", 14)                        # the organic tank
DT.pipe([(0.4, 1.3, 2.9), (15.0, 1.3, 2.9)], 3, "steel")                         # the organic header between the rows
stub(DT, (1.0, 0.5, 0.0), (1.0, 0.5, 0.5), 2, "steel")                           # kerosene in, the north
stub(DT, (14.0, 0.5, 5.6), (14.0, 0.5, 6.0), 2, "steel")                         # treated acid out, the south
box_north(DT, 0.4, 0.1)
DT.collide((0.0, 0.0, 0.0), (16.0, 1.5, 6.0))


M = PM
EXTRA = [PR, TF, AE, GP, PD, VC, PY, AP, DT]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
