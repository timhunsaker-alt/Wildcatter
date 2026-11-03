"""The anode carbon plants at true size (docs/multiblocks/delayed-coker.md, coke-ovens.md, anode-plant.md), each part drawn facing
north from its own main cell (the footprints match CarbonPartBlock's). Inlets come in at the north face of the front cells, outlets
leave the south face of the back cells, fuel comes in on the east; the 480 V box is on the west of the main cell (the coke pit's,
the by-product plant's and the paste plant's are 4.16 kV):
- coker_fractionator, 3 x 18 x 3: the column, its trays' manways and the overhead line, the side-draw pumps;
- coker_heater, 4 x 10 x 6: the radiant box and its convection section and stack, the charge pump;
- coke_drum, 3 x 16 x 3: the drum on its skirt, the top head and the drill stem's derrick;
- coke_pit, 6 x 4 x 8: the concrete pit and its sluiceway, the jet pump house;
- coke_kiln, 3 x 3 x 30: the rotary kiln on its tyres and rollers, its girth gear and the firing hood;
- coke_cooler, 2 x 3 x 12: the rotary cooler and its water sprays;
- coal_tower, 4 x 12 x 4: the bunker on its legs and the conveyor gallery;
- coke_oven_battery, 10 x 8 x 16: the brick battery with its oven doors, the charging deck and the collecting main;
- pusher_machine, 4 x 6 x 16: the pusher on its rails, its ram;
- coke_quench_tower, 5 x 20 x 5: the timber-lined tower over the quench car's track;
- byproduct_plant, 5 x 10 x 8: two primary coolers, the tar decanter and the exhauster;
- tar_still, 3 x 12 x 3: the pipe still and the column;
- paste_plant, 5 x 10 x 6: the silos, the preheater and the heated mixer;
- forming_press, 4 x 5 x 4: the vibro-compactor's mould table and weight;
- baking_furnace, 10 x 5 x 24: the sunken pits of the ring furnace and its burner ramps;
- rodding_shop, 4 x 5 x 12: the rod conveyor, the stripping press and the induction furnace."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def disc(M, cx, cz, r, y0, y1, tex):
	"""A round lid too wide for one turned slab: an octagon of three flat boxes inside the radius."""
	a, b = r * 0.92, r * 0.38
	M.box((cx - b, y0, cz - a), (cx + b, y1, cz + a), tex)
	M.box((cx - a, y0, cz - b), (cx + a, y1, cz + b), tex)
	M.box((cx - r * 0.65, y0, cz - r * 0.65), (cx + r * 0.65, y1, cz + r * 0.65), tex)


def disc_z(M, cx, cy, r, z0, z1, tex):
	"""The same octagon facing along z: a kiln's end."""
	a, b = r * 0.92, r * 0.38
	M.box((cx - b, cy - a, z0), (cx + b, cy + a, z1), tex)
	M.box((cx - a, cy - b, z0), (cx + a, cy + b, z1), tex)
	M.box((cx - r * 0.65, cy - r * 0.65, z0), (cx + r * 0.65, cy + r * 0.65, z1), tex)


def tank(M, cx, cz, r, y0, y1, tex, sides=24, lid=None):
	"""A round vessel wider than a turned slab can be: a hoop of plates and a lid."""
	M.ring("y", (cx, cz), r, y0, y1, 0.1, tex, sides=sides)
	disc(M, cx, cz, r, y1 - 0.1, y1, lid or tex)


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


def inlet(M, x, y=0.5):
	M.pipe([(x, y, 0.0), (x, y, 0.6)], 2, "steel")


def outlet(M, x, d, y=0.5):
	M.pipe([(x, y, d - 0.6), (x, y, d)], 2, "steel")


def east(M, w, z, y=0.5, tex="steel"):
	M.pipe([(w - 0.6, y, z), (w, y, z)], 2, tex)


# ---- the coker fractionator ------------------------------------------------------------------------------------------------------
CF = model("coker_fractionator", 3, 18, 3, "lagging")
CF.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
CF.ring("y", (1.5, 1.5), 0.6, 0.3, 1.2, 0.1, "steel", sides=12)              # the skirt
CF.cylinder("y", (1.5, 1.5), 0.95, 1.2, 17.0, "lagging", 16)                # the column
CF.cylinder("y", (1.5, 1.5), 0.5, 17.0, 17.6, "lagging", 12)                 # the top head
for y in (4.0, 8.0, 12.0, 15.5):
	CF.box((1.3, y, 0.45), (1.7, y + 0.6, 0.55), "steel")                   # manways
	CF.box((0.4, y - 0.1, 0.3), (2.6, y, 2.7), "grating")                   # platforms
CF.pipe([(1.5, 17.6, 1.5), (1.5, 17.9, 1.5), (2.8, 17.9, 1.5), (2.8, 0.5, 1.5), (3.0, 0.5, 1.5)], 2, "steel")   # the gas overhead, east
CF.box((0.2, 0.3, 2.2), (0.8, 0.9, 2.8), "blue")                            # the side-draw pumps
inlet(CF, 1.5)
outlet(CF, 0.5, 3.0)                                                         # gas oil, x 0
outlet(CF, 2.5, 3.0)                                                         # naphtha, x 2
box480(CF, 0.5, 0.3)
CF.collide((0.4, 0.0, 0.4), (2.6, 17.9, 2.6))

# ---- the coker heater ------------------------------------------------------------------------------------------------------------
CH = model("coker_heater", 4, 10, 6, "hipaint")
CH.box((0.0, 0.0, 0.0), (4.0, 0.3, 6.0), "concrete")
for x, z in ((0.3, 0.3), (3.4, 0.3), (0.3, 5.4), (3.4, 5.4)):
	CH.box((x, 0.3, z), (x + 0.3, 1.2, z + 0.3), "iron")                    # the legs
CH.box((0.4, 1.2, 0.5), (3.6, 6.0, 5.5), "hipaint")                         # the radiant box
CH.box((0.8, 6.0, 1.2), (3.2, 7.6, 4.8), "steel")                           # the convection section
CH.cylinder("y", (2.0, 3.0), 0.5, 7.6, 9.9, "steel", 12)                     # the stack
for z in (1.2, 2.4, 3.6, 4.8):
	CH.box((0.9, 1.0, z - 0.15), (3.1, 1.2, z + 0.15), "dark")              # the floor burners
CH.box((0.3, 0.3, 4.0), (1.3, 1.0, 5.0), "blue")                            # the charge pump
east(CH, 4.0, 2.0)                                                           # fuel in
box480(CH, 0.5, 0.3)
CH.collide((0.3, 0.0, 0.3), (3.7, 9.9, 5.7))

# ---- a coke drum ---------------------------------------------------------------------------------------------------------------
CD = model("coke_drum", 3, 16, 3, "lagging")
CD.ring("y", (1.5, 1.5), 1.2, 0.0, 1.5, 0.1, "steel", sides=16)              # the skirt
tank(CD, 1.5, 1.5, 1.45, 1.5, 13.5, "lagging", 20, "steel")                 # the drum
CD.cylinder("y", (1.5, 1.5), 0.7, 13.5, 14.2, "steel", 12)                   # the top head
for x in (0.3, 2.5):
	CD.box((x, 14.2, 0.3), (x + 0.2, 15.9, 2.7), "iron")                    # the drill derrick
CD.box((0.3, 15.7, 0.3), (2.7, 15.9, 2.7), "iron")
CD.cylinder("y", (1.5, 1.5), 0.08, 14.2, 15.7, "steel", 6)                   # the drill stem
CD.pipe([(1.5, 12.8, 0.05), (1.5, 13.0, 0.05)], 4, "lagging")               # the vapour line stub
box480(CD, 0.5, 0.3)
CD.collide((0.1, 0.0, 0.1), (2.9, 15.9, 2.9))

# ---- the coke pit -----------------------------------------------------------------------------------------------------------
CP = model("coke_pit", 6, 4, 8, "concrete")
for x0, x1 in ((0.0, 0.4), (5.6, 6.0)):
	CP.box((x0, 0.0, 0.0), (x1, 3.8, 8.0), "concrete")                      # the pit walls
CP.box((0.4, 0.0, 7.6), (5.6, 3.8, 8.0), "concrete")
CP.box((0.4, 3.6, 0.0), (5.6, 4.0, 3.2), "concrete")                        # the drum deck
CP.box((0.4, 0.0, 0.0), (5.6, 0.6, 7.6), "dark")                            # the coke in the pit
CP.box((0.6, 0.6, 3.6), (2.6, 1.6, 6.0), "dark")
CP.box((3.4, 0.0, 4.2), (5.4, 2.4, 7.4), "blue")                            # the jet pump house
CP.box((0.0, 0.5, 0.3), (0.3, 1.4, 0.9), "grey")                            # its 4.16 kV box, west
CP.collide((0.0, 0.0, 0.0), (6.0, 4.0, 8.0))

# ---- the coke kiln -----------------------------------------------------------------------------------------------------------
CK = model("coke_kiln", 3, 3, 30, "hipaint")
CK.box((0.0, 0.0, 0.0), (3.0, 0.2, 30.0), "concrete")
for z in (4.0, 15.0, 26.0):
	CK.box((0.5, 0.2, z - 0.5), (2.5, 0.7, z + 0.5), "concrete")            # the piers and rollers
	CK.ring("z", (1.5, 1.6), 1.3, z - 0.3, z + 0.3, 0.12, "iron", sides=18) # the tyres
CK.ring("z", (1.5, 1.6), 1.2, 1.2, 28.8, 0.1, "hipaint", sides=18)          # the shell
CK.ring("z", (1.5, 1.6), 1.32, 10.0, 10.4, 0.12, "iron", sides=18)          # the girth gear
CK.box((0.2, 0.2, 9.5), (0.9, 1.0, 10.9), "engine")                         # the drive
CK.box((0.2, 0.2, 0.0), (2.8, 2.9, 1.2), "steel")                           # the feed hood
CK.box((0.2, 0.2, 28.8), (2.8, 2.9, 30.0), "hipaint")                       # the firing hood
CK.box((1.0, 2.9, 0.2), (2.0, 3.0, 1.0), "galv")                            # the feed chute under the hopper
east(CK, 3.0, 29.4)                                                         # fuel in at the firing end
box480(CK, 0.4, 0.3)
CK.collide((0.2, 0.0, 0.0), (2.8, 3.0, 30.0))

# ---- the coke cooler -----------------------------------------------------------------------------------------------------------
CO = model("coke_cooler", 2, 3, 12, "steel")
CO.box((0.0, 0.0, 0.0), (2.0, 0.2, 12.0), "concrete")
for z in (2.0, 10.0):
	CO.box((0.3, 0.2, z - 0.4), (1.7, 0.6, z + 0.4), "concrete")
CO.cylinder("z", (1.0, 1.4), 0.85, 0.5, 11.5, "steel", 14)                  # the cooler drum
CO.pipe([(1.0, 2.5, 1.0), (1.0, 2.5, 11.0)], 1, "galv")                     # the water sprays
CO.box((0.2, 0.2, 5.4), (0.8, 0.9, 6.6), "engine")
box480(CO, 0.4, 0.3)
CO.collide((0.1, 0.0, 0.0), (1.9, 2.6, 12.0))

# ---- the coal tower -----------------------------------------------------------------------------------------------------------
CT = model("coal_tower", 4, 12, 4, "concrete")
for x, z in ((0.2, 0.2), (3.4, 0.2), (0.2, 3.4), (3.4, 3.4)):
	CT.box((x, 0.0, z), (x + 0.4, 6.0, z + 0.4), "concrete")                # the legs
CT.box((0.2, 6.0, 0.2), (3.8, 11.0, 3.8), "concrete")                       # the bunker
CT.box((1.2, 4.0, 1.2), (2.8, 6.0, 2.8), "iron")                            # the outlet gates
CT.box((0.8, 11.0, 0.8), (3.2, 11.9, 3.2), "galv")                          # the conveyor head house
box480(CT, 0.5, 0.6)
CT.collide((0.2, 0.0, 0.2), (3.8, 11.9, 3.8))

# ---- the coke oven battery -----------------------------------------------------------------------------------------------------
CB = model("coke_oven_battery", 10, 8, 16, "concrete")
CB.box((0.0, 0.0, 0.0), (10.0, 1.0, 16.0), "concrete")                      # the battery's foundation and regenerators
CB.box((0.4, 1.0, 0.2), (9.6, 7.0, 15.8), "hipaint")                        # the oven brickwork
for i in range(13):
	z = 0.6 + i * 1.15
	for x in (0.3, 9.6):
		CB.box((x, 1.2, z), (x + 0.1, 6.8, z + 0.5), "iron")                # the oven doors, both sides
	CB.box((4.6, 7.0, z), (5.4, 7.2, z + 0.5), "iron")                      # the charging holes' lids
CB.box((0.4, 7.0, 0.2), (9.6, 7.05, 15.8), "checker")                       # the charging deck
CB.cylinder("z", (8.6, 7.6), 0.35, 0.2, 15.8, "steel", 10)                   # the gas collecting main
CB.box((1.0, 7.05, 6.0), (3.6, 7.9, 9.0), "galv")                           # the larry car
CB.pipe([(8.6, 7.6, 15.8), (8.6, 7.6, 16.0)], 4, "steel")
CB.pipe([(5.0, 0.5, 15.4), (5.0, 0.5, 16.0)], 4, "steel")                   # raw gas out, the back
box480(CB, 0.3, 0.3)
CB.collide((0.0, 0.0, 0.0), (10.0, 7.9, 16.0))

# ---- the pusher machine -------------------------------------------------------------------------------------------------------
PM = model("pusher_machine", 4, 6, 16, "blue")
for x in (0.6, 3.2):
	PM.box((x, 0.0, 0.0), (x + 0.2, 0.15, 16.0), "iron")                    # the rails
PM.box((0.4, 0.15, 5.0), (3.6, 1.0, 11.0), "iron")                          # the bogies
PM.box((0.4, 1.0, 5.0), (3.6, 5.6, 11.0), "blue")                           # the machine
PM.box((3.6, 2.6, 7.6), (4.0, 3.4, 8.4), "steel")                           # the pusher ram's head
PM.box((0.6, 5.6, 5.4), (2.0, 5.9, 6.8), "grey")                            # the cab roof
box480(PM, 0.5, 0.3)
PM.collide((0.4, 0.0, 5.0), (4.0, 5.9, 11.0))

# ---- the coke quench tower ---------------------------------------------------------------------------------------------------
QT = model("coke_quench_tower", 5, 20, 5, "concrete")
for x in (0.6, 4.2):
	QT.box((x, 0.0, 0.0), (x + 0.2, 0.15, 5.0), "iron")                     # the quench car track
QT.box((0.5, 0.15, 0.8), (4.5, 2.2, 4.2), "iron")                           # the quench car
QT.box((0.7, 1.2, 1.0), (4.3, 2.0, 4.0), "dark")                            # its coke
for x, z in ((0.0, 0.0), (4.6, 0.0), (0.0, 4.6), (4.6, 4.6)):
	QT.box((x, 0.0, z), (x + 0.4, 6.0, z + 0.4), "concrete")
QT.box((0.0, 6.0, 0.0), (5.0, 19.0, 5.0), "concrete")                       # the tower
QT.box((0.6, 19.0, 0.6), (4.4, 19.9, 4.4), "steel")                         # the baffles at its top
QT.box((0.0, 0.5, 0.3), (0.3, 1.1, 0.8), "grey")
box480(QT, 2.5, 0.3)
QT.collide((0.0, 0.0, 0.0), (5.0, 19.9, 5.0))

# ---- the by-product plant ----------------------------------------------------------------------------------------------------
BP = model("byproduct_plant", 5, 10, 8, "grey")
BP.box((0.0, 0.0, 0.0), (5.0, 0.3, 8.0), "concrete")
for z in (1.5, 4.0):
	BP.cylinder("y", (1.5, z), 0.95, 0.3, 9.4, "grey", 16)                   # the primary coolers
BP.cylinder("z", (3.8, 1.2), 0.9, 0.6, 4.6, "dark", 14)                      # the tar decanter
BP.box((2.6, 0.3, 5.4), (4.6, 1.9, 7.6), "blue")                            # the exhauster
BP.pipe([(1.5, 9.4, 1.5), (1.5, 9.7, 1.5), (1.5, 9.7, 4.0), (1.5, 9.4, 4.0)], 4, "steel")
BP.box((0.0, 0.6, 0.3), (0.3, 1.4, 0.9), "grey")                            # its 4.16 kV box, west
inlet(BP, 1.5)
outlet(BP, 3.6, 8.0)
east(BP, 5.0, 1.2)                                                          # tar out, the east
BP.collide((0.4, 0.0, 0.4), (4.6, 9.7, 7.6))

# ---- the tar still -------------------------------------------------------------------------------------------------------------
TS = model("tar_still", 3, 12, 3, "dark")
TS.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
TS.box((0.3, 0.3, 0.3), (2.7, 2.6, 1.6), "hipaint")                         # the pipe still
TS.cylinder("y", (1.5, 2.1), 0.7, 0.3, 11.6, "dark", 14)                     # the column
TS.cylinder("y", (1.5, 2.1), 0.35, 11.6, 11.9, "dark", 10)
east(TS, 3.0, 1.0)                                                          # fuel in
inlet(TS, 1.5)
outlet(TS, 0.5, 3.0)                                                        # pitch, x 0
outlet(TS, 2.5, 3.0)                                                        # tar oils, x 2
box480(TS, 0.5, 0.3)
TS.collide((0.3, 0.0, 0.3), (2.7, 11.9, 2.9))

# ---- the paste plant ------------------------------------------------------------------------------------------------------------
PP = model("paste_plant", 5, 10, 6, "grey")
PP.box((0.0, 0.0, 0.0), (5.0, 0.3, 6.0), "concrete")
for x, z in ((1.2, 1.4), (3.6, 1.4)):
	PP.cylinder("y", (x, z), 0.95, 4.0, 9.0, "galv", 14)                      # the coke and butt silos
	PP.cylinder("y", (x, z), 0.4, 3.0, 4.0, "galv", 10)
	PP.box((x - 0.8, 0.3, z - 0.8), (x - 0.6, 4.0, z - 0.6), "iron")
	PP.box((x + 0.6, 0.3, z + 0.6), (x + 0.8, 4.0, z + 0.8), "iron")
PP.box((0.6, 9.0, 0.6), (4.4, 9.8, 2.4), "galv")                            # the feed gallery under the hopper
PP.cylinder("z", (2.5, 1.5), 0.8, 3.0, 5.6, "hipaint", 14)                   # the preheater and the heated mixer
PP.box((3.4, 0.3, 3.6), (4.6, 1.4, 5.4), "engine")
PP.box((0.0, 0.6, 0.3), (0.3, 1.4, 0.9), "grey")                            # its 4.16 kV box, west
east(PP, 5.0, 4.5, 0.5, "lagging")                                          # pitch in, traced
PP.collide((0.3, 0.0, 0.3), (4.7, 9.8, 5.7))

# ---- the forming press --------------------------------------------------------------------------------------------------------
FP = model("forming_press", 4, 5, 4, "blue")
FP.box((0.0, 0.0, 0.0), (4.0, 0.3, 4.0), "concrete")
FP.box((0.4, 0.3, 0.4), (3.6, 1.4, 3.6), "blue")                            # the vibrating table
FP.box((1.2, 1.4, 1.6), (2.8, 2.0, 2.4), "dark")                            # a green anode in the mould
for x, z in ((0.5, 0.5), (3.2, 0.5), (0.5, 3.2), (3.2, 3.2)):
	FP.box((x, 1.4, z), (x + 0.3, 4.6, z + 0.3), "iron")                    # the press frame
FP.box((0.5, 4.0, 0.5), (3.5, 4.8, 3.5), "blue")                            # the top weight's cylinders
FP.box((1.0, 2.0, 1.4), (3.0, 3.2, 2.6), "iron")                            # the top weight
box480(FP, 0.5, 0.6)
FP.collide((0.4, 0.0, 0.4), (3.6, 4.8, 3.6))

# ---- the ring baking furnace ----------------------------------------------------------------------------------------------------
RF = model("baking_furnace", 10, 5, 24, "concrete")
RF.box((0.0, 0.0, 0.0), (10.0, 2.6, 24.0), "concrete")                      # the furnace's pit structure
for i in range(8):
	z = 0.6 + i * 2.9
	RF.box((0.6, 2.6, z), (9.4, 2.7, z + 2.5), "hipaint")                   # the sections' covers
	for x in (2.4, 4.8, 7.2):
		RF.box((x, 2.7, z + 0.3), (x + 0.2, 2.9, z + 2.2), "iron")          # the flue walls' peep holes
for z in (3.5, 12.2):
	RF.box((0.4, 2.7, z), (9.6, 3.6, z + 0.8), "steel")                     # the burner ramps
	RF.box((0.4, 3.6, z + 0.2), (9.6, 3.8, z + 0.6), "galv")
RF.box((0.2, 2.6, 18.0), (9.8, 4.6, 20.0), "blue")                          # the overhead tending crane's bridge
RF.cylinder("y", (8.6, 22.6), 0.6, 2.6, 4.9, "steel", 12)                    # the exhaust fan's stack
east(RF, 10.0, 1.0)                                                         # fuel in
box480(RF, 0.6, 0.3)
RF.collide((0.0, 0.0, 0.0), (10.0, 3.8, 24.0))

# ---- the rodding shop ---------------------------------------------------------------------------------------------------------
RS = model("rodding_shop", 4, 5, 12, "grey")
RS.box((0.0, 0.0, 0.0), (4.0, 0.3, 12.0), "concrete")
RS.box((1.6, 4.0, 0.2), (2.4, 4.3, 11.8), "iron")                           # the overhead rod conveyor
for z in (1.0, 3.0, 5.0, 7.0, 9.0, 11.0):
	RS.box((1.9, 1.6, z - 0.05), (2.1, 4.0, z + 0.05), "copper")            # rods hanging from it
RS.box((0.3, 0.3, 1.0), (3.7, 2.2, 3.2), "blue")                            # the butt stripping press
RS.box((0.3, 0.3, 4.4), (1.6, 1.6, 6.2), "dark")                            # the butt crusher
RS.box((2.4, 0.3, 7.0), (3.7, 1.8, 9.0), "hipaint")                         # the induction furnace
RS.box((0.6, 0.3, 9.6), (3.4, 1.0, 11.6), "iron")                           # the rodding station
box480(RS, 0.5, 0.3)
RS.collide((0.3, 0.0, 0.2), (3.7, 4.3, 11.8))


M = CF
EXTRA = [CH, CD, CP, CK, CO, CT, CB, PM, QT, BP, TS, PP, FP, RF, RS]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
