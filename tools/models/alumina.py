"""The Bayer refinery and its helpers at true size (docs/multiblocks/bayer-refinery.md, alumina-plant.md), each part drawn facing
north from its own main cell (the footprints match AluminaPartBlock's). Inlets come in at the north face of the front cells, outlets
leave the south face of the back cells; steam, gas, caustic and HF come in on the east; the 480 V box is on the west of the main
cell (the calciner's is 4.16 kV):
- ball_mill, 4 x 4 x 8: the mill shell on its trunnions, girth gear and motor, the bauxite feed chute;
- digester, 5 x 8 x 5: the insulated digester vessel and its agitator drive, steam in on the east;
- flash_tanks, 3 x 8 x 6: three flash vessels stepping down, the vapour line to the feed heaters;
- settler and mud_washer, 10 x 4 x 10: a thickener tank, its bridge and rake drive, the mud line east;
- residue_filter_press, 4 x 4 x 8: the plate pack between its heads, hydraulic cylinder, cake chute under it;
- polishing_filter, 3 x 5 x 6: two pressure leaf filters and the feed pump;
- precipitator, 12 x 18 x 12: the draft-tube tank, its drive on the roof bridge;
- hydrate_filter, 4 x 4 x 10: the horizontal pan filter, its vacuum receiver; spent liquor out on the east;
- calciner, 6 x 20 x 6: the cyclone preheater tower, the furnace and the cooler cyclones;
- evaporator, 4 x 12 x 8: three falling-film effects and the barometric condenser, the liquor storage tank;
- causticizer, 3 x 5 x 3: the lime slaker and its bin;
- package_boiler, 4 x 5 x 9: the water-tube boiler casing, its burner front, stack and steam drum;
- lime_kiln, 3 x 14 x 3: the vertical shaft kiln with its skip hoist and top hopper;
- alf3_reactor, 3 x 10 x 3: the fluid-bed reactor, its cyclone and the hydrate hopper;
- bagging_station, 2 x 4 x 2: the FIBC filling head on its frame;
- chlorine_filler, 3 x 4 x 3: the refrigeration skid, the liquefier and the filling scale with a ton container."""
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
	"""The same octagon facing along z: a mill's end."""
	a, b = r * 0.92, r * 0.38
	M.box((cx - b, cy - a, z0), (cx + b, cy + a, z1), tex)
	M.box((cx - a, cy - b, z0), (cx + a, cy + b, z1), tex)
	M.box((cx - r * 0.65, cy - r * 0.65, z0), (cx + r * 0.65, cy + r * 0.65, z1), tex)


def tank(M, cx, cz, r, y0, y1, tex, sides=28, lid=None):
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


# ---- the ball mill ------------------------------------------------------------------------------------------------------------
BM = model("ball_mill", 4, 4, 8, "blue")
BM.box((0.0, 0.0, 0.0), (4.0, 0.3, 8.0), "concrete")
for z in (0.8, 6.6):
	BM.box((0.8, 0.3, z), (3.2, 1.6, z + 0.6), "concrete")                  # the trunnion piers
BM.ring("z", (2.0, 2.0), 1.6, 1.0, 6.6, 0.1, "blue", sides=20)            # the mill shell
for z in (1.0, 6.5):
	disc_z(BM, 2.0, 2.0, 1.6, z, z + 0.1, "blue")                           # its heads
BM.ring("z", (2.0, 2.0), 1.75, 4.6, 4.9, 0.15, "iron", sides=20)           # the girth gear
BM.box((0.4, 0.3, 4.4), (1.2, 1.5, 5.2), "engine")                          # the pinion and the motor
BM.box((1.5, 2.6, 0.0), (2.5, 3.9, 0.9), "galv")                            # the bauxite feed chute
inlet(BM, 3.2)
outlet(BM, 2.0, 8.0)
box480(BM, 0.5, 0.3)
BM.collide((0.4, 0.0, 0.4), (3.6, 3.9, 7.6))

# ---- the digester --------------------------------------------------------------------------------------------------------------
DG = model("digester", 5, 8, 5, "lagging")
DG.box((0.0, 0.0, 0.0), (5.0, 0.3, 5.0), "concrete")
tank(DG, 2.5, 2.5, 2.1, 0.8, 7.0, "lagging", 20)                            # the insulated vessel
DG.ring("y", (2.5, 2.5), 1.6, 0.3, 0.8, 0.1, "steel", sides=16)            # its skirt
DG.box((2.0, 7.0, 2.0), (3.0, 7.9, 3.0), "engine")                          # the agitator drive
inlet(DG, 1.2)
east(DG, 5.0, 2.5, 0.5, "lagging")                                          # process steam in
box480(DG, 0.5, 0.3)
DG.collide((0.4, 0.0, 0.4), (4.6, 7.9, 4.6))

# ---- the flash tanks -------------------------------------------------------------------------------------------------------------
FT = model("flash_tanks", 3, 8, 6, "lagging")
FT.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "concrete")
for z, top in ((1.0, 7.6), (3.0, 6.4), (5.0, 5.2)):
	FT.cylinder("y", (1.5, z), 0.8, 0.3, top, "lagging", 14)                # three flash vessels, each a stage down
FT.pipe([(1.5, 7.6, 1.0), (1.5, 7.8, 1.0), (0.3, 7.8, 1.0), (0.3, 7.8, 0.0)], 2, "lagging")   # vapour to the feed heaters
FT.box((2.2, 0.3, 5.3), (2.9, 1.0, 5.9), "blue")                            # the slurry pump
outlet(FT, 1.5, 6.0)
box480(FT, 0.5, 0.3)
FT.collide((0.6, 0.0, 0.2), (2.4, 7.6, 5.8))


def thickener(name):
	T = model(name, 10, 4, 10, "concrete")
	T.box((0.0, 0.0, 0.0), (10.0, 0.3, 10.0), "concrete")
	T.ring("y", (5.0, 5.0), 4.8, 0.3, 3.0, 0.15, "concrete", sides=28)     # the tank
	disc(T, 5.0, 5.0, 4.8, 2.8, 2.85, "dark")                              # the liquor under its bridge
	T.box((0.3, 3.0, 4.6), (9.7, 3.3, 5.4), "grating")                      # the bridge
	T.box((4.4, 3.3, 4.4), (5.6, 3.9, 5.6), "engine")                       # the rake drive
	T.cylinder("y", (5.0, 5.0), 0.15, 0.6, 3.0, "steel", 8)                 # its shaft
	inlet(T, 5.0)
	outlet(T, 5.0, 10.0, 2.6)
	T.pipe([(9.4, 0.5, 5.0), (10.0, 0.5, 5.0)], 2, "steel")                 # the mud underflow, east
	box480(T, 0.5, 0.3)
	T.collide((0.4, 0.0, 0.4), (9.6, 3.0, 9.6))
	return T


ST = thickener("settler")
MW = thickener("mud_washer")
MW.pipe([(8.0, 3.0, 9.0), (8.0, 3.6, 9.0)], 2, "galv")                      # the wash water header

# ---- the residue filter press ---------------------------------------------------------------------------------------------------
FP = model("residue_filter_press", 4, 4, 8, "blue")
FP.box((0.0, 0.0, 0.0), (4.0, 0.3, 8.0), "concrete")
for x in (0.3, 3.4):
	FP.box((x, 0.3, 0.6), (x + 0.3, 1.4, 7.4), "concrete")                  # the cake chute walls
FP.box((0.4, 1.4, 0.6), (3.6, 3.6, 1.3), "blue")                            # the fixed head
FP.box((0.4, 1.4, 6.0), (3.6, 3.6, 6.7), "blue")                            # the follower head
for i in range(14):
	z = 1.5 + i * 0.32
	FP.box((0.6, 1.6, z), (3.4, 3.4, z + 0.24), "dark")                     # the filter plates
FP.cylinder("z", (2.0, 2.5), 0.3, 6.7, 7.7, "steel", 10)                    # the hydraulic cylinder
inlet(FP, 2.0, 2.5)
box480(FP, 0.5, 0.3)
FP.collide((0.3, 0.0, 0.6), (3.7, 3.6, 7.7))

# ---- the polishing filters ------------------------------------------------------------------------------------------------------
PF = model("polishing_filter", 3, 5, 6, "grey")
PF.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "concrete")
for z in (1.5, 4.0):
	PF.cylinder("y", (1.5, z), 1.0, 0.8, 4.4, "steel", 14)                   # pressure leaf filters
	PF.cylinder("y", (1.5, z), 0.6, 0.3, 0.8, "iron", 10)
PF.box((0.2, 0.3, 0.2), (0.9, 1.0, 0.8), "blue")                            # the feed pump
inlet(PF, 1.5)
outlet(PF, 1.5, 6.0)
box480(PF, 1.2, 0.3)
PF.collide((0.5, 0.0, 0.5), (2.5, 4.4, 5.0))

# ---- the precipitator -------------------------------------------------------------------------------------------------------------
PR = model("precipitator", 12, 18, 12, "grey")
PR.box((0.0, 0.0, 0.0), (12.0, 0.3, 12.0), "concrete")
tank(PR, 6.0, 6.0, 5.8, 0.3, 17.1, "grey", 32, "iron")                      # the tank and its roof
PR.box((0.6, 17.1, 5.4), (11.4, 17.4, 6.6), "grating")                      # the roof bridge
PR.box((5.3, 17.4, 5.3), (6.7, 17.9, 6.7), "engine")                        # the draft-tube agitator drive
inlet(PR, 6.0, 16.0)
outlet(PR, 6.0, 12.0)
box480(PR, 0.5, 0.3)
PR.collide((0.5, 0.0, 0.5), (11.5, 17.1, 11.5))

# ---- the hydrate filter ----------------------------------------------------------------------------------------------------------
HF = model("hydrate_filter", 4, 4, 10, "blue")
HF.box((0.0, 0.0, 0.0), (4.0, 0.3, 10.0), "concrete")
for z in (1.0, 9.0):
	HF.box((0.6, 0.3, z - 0.4), (3.4, 1.6, z + 0.4), "iron")                # the pan supports
HF.box((0.3, 1.6, 0.4), (3.7, 2.1, 9.6), "blue")                            # the pan filter
for i in range(8):
	z = 0.8 + i * 1.1
	HF.box((0.4, 2.1, z), (3.6, 2.3, z + 0.9), "filter")                    # its pans
HF.cylinder("y", (3.2, 5.0), 0.5, 0.3, 1.6, "steel", 12)                    # the vacuum receiver
inlet(HF, 2.0, 2.3)
east(HF, 4.0, 1.5)                                                          # spent liquor out, the east
box480(HF, 0.5, 0.3)
HF.collide((0.3, 0.0, 0.4), (3.7, 2.3, 9.6))

# ---- the calciner -----------------------------------------------------------------------------------------------------------------
CC = model("calciner", 6, 20, 6, "hipaint")
CC.box((0.0, 0.0, 0.0), (6.0, 0.3, 6.0), "concrete")
for x, z in ((0.4, 0.4), (5.2, 0.4), (0.4, 5.2), (5.2, 5.2)):
	CC.box((x, 0.3, z), (x + 0.4, 18.0, z + 0.4), "iron")                   # the tower steel
for y in (5.0, 10.0, 15.0):
	CC.box((0.4, y, 0.4), (5.6, y + 0.2, 5.6), "grating")                   # its floors
CC.cylinder("y", (3.0, 3.0), 0.98, 1.0, 9.0, "hipaint", 16)                  # the furnace
for x, z, y in ((1.8, 1.8, 10.2), (4.2, 4.2, 10.2), (1.8, 4.2, 15.2), (4.2, 1.8, 15.2)):
	CC.cylinder("y", (x, z), 0.8, y, y + 4.0, "steel", 12)                  # preheater and cooler cyclones
CC.cylinder("y", (3.0, 3.0), 0.5, 15.2, 19.8, "steel", 10)                  # the stack to the ID fan
CC.box((0.2, 0.3, 4.4), (1.4, 1.6, 5.6), "engine")                          # the ID fan
CC.box((0.0, 0.6, 0.3), (0.3, 1.4, 0.9), "grey")                            # its 4.16 kV box, west
east(CC, 6.0, 1.5)                                                          # gas in
CC.collide((0.4, 0.0, 0.4), (5.6, 19.8, 5.6))

# ---- the evaporator ---------------------------------------------------------------------------------------------------------------
EV = model("evaporator", 4, 12, 8, "lagging")
EV.box((0.0, 0.0, 0.0), (4.0, 0.3, 8.0), "concrete")
for z in (1.2, 3.2, 5.2):
	EV.cylinder("y", (1.4, z), 0.75, 0.3, 11.0, "lagging", 14)             # the three falling-film effects
EV.cylinder("y", (3.2, 1.2), 0.4, 6.0, 11.8, "steel", 10)                   # the barometric condenser
EV.cylinder("y", (2.6, 6.8), 1.0, 0.3, 4.0, "grey", 16)                     # the liquor storage tank
east(EV, 4.0, 2.5, 0.5, "lagging")                                          # process steam in
east(EV, 4.0, 4.5)                                                          # caustic soda in, its own run
inlet(EV, 1.4)
outlet(EV, 2.6, 8.0)
box480(EV, 0.5, 0.3)
EV.collide((0.4, 0.0, 0.4), (3.6, 11.8, 7.8))

# ---- the causticizer -------------------------------------------------------------------------------------------------------------
CZ = model("causticizer", 3, 5, 3, "grey")
CZ.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
CZ.cylinder("y", (1.5, 1.6), 1.1, 0.3, 2.8, "grey", 16)                     # the causticization tank
CZ.box((0.8, 2.8, 0.9), (2.2, 4.6, 2.3), "galv")                            # the lime bin and slaker
CZ.box((1.2, 4.6, 1.3), (1.8, 4.9, 1.9), "engine")
box480(CZ, 0.5, 0.3)
CZ.collide((0.4, 0.0, 0.4), (2.6, 4.9, 2.7))

# ---- the package boiler ----------------------------------------------------------------------------------------------------------
PB = model("package_boiler", 4, 5, 9, "blue")
PB.box((0.0, 0.0, 0.0), (4.0, 0.3, 9.0), "concrete")
PB.box((0.4, 0.3, 1.2), (3.6, 3.6, 8.4), "blue")                            # the water-tube casing
PB.cylinder("z", (2.0, 4.0), 0.55, 1.2, 8.4, "steel", 12)                   # the steam drum
PB.box((0.8, 0.6, 0.3), (3.2, 3.0, 1.2), "grey")                            # the burner front and FD fan
PB.cylinder("y", (3.0, 7.6), 0.4, 3.6, 4.9, "steel", 10)                    # the stack
east(PB, 4.0, 2.0)                                                          # gas in
PB.pipe([(2.0, 4.0, 8.4), (2.0, 4.0, 8.7), (2.0, 0.5, 8.7), (2.0, 0.5, 9.0)], 2, "lagging")   # steam out, the back
box480(PB, 0.5, 0.3)
PB.collide((0.4, 0.0, 0.3), (3.6, 4.9, 8.4))

# ---- the lime kiln -------------------------------------------------------------------------------------------------------------
LK = model("lime_kiln", 3, 14, 3, "hipaint")
LK.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
LK.cylinder("y", (1.5, 1.5), 1.1, 0.3, 11.0, "hipaint", 16)                 # the refractory-lined shaft
LK.box((0.9, 11.0, 0.9), (2.1, 13.9, 2.1), "galv")                          # the top hopper
LK.box((0.2, 0.3, 2.5), (0.5, 12.0, 2.8), "iron")                           # the skip hoist rail
LK.box((0.6, 0.3, 0.2), (1.4, 1.2, 0.6), "engine")                          # the blower
east(LK, 3.0, 1.5)                                                          # gas in
box480(LK, 1.4, 0.3)
LK.collide((0.4, 0.0, 0.4), (2.6, 13.9, 2.6))

# ---- the AlF3 reactor ------------------------------------------------------------------------------------------------------------
AR = model("alf3_reactor", 3, 10, 3, "grey")
AR.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
AR.cylinder("y", (1.4, 1.6), 0.95, 0.3, 7.0, "hipaint", 16)                 # the fluid bed
AR.cylinder("y", (2.4, 0.6), 0.35, 4.0, 8.8, "steel", 10)                    # its cyclone
AR.box((0.8, 7.0, 1.0), (2.0, 9.8, 2.2), "galv")                            # the hydrate hopper
east(AR, 3.0, 0.5)                                                          # anhydrous HF in
east(AR, 3.0, 2.5)                                                          # gas in, its own run
box480(AR, 0.5, 0.3)
AR.collide((0.4, 0.0, 0.4), (2.6, 9.8, 2.6))

# ---- the bagging station ----------------------------------------------------------------------------------------------------------
BS = model("bagging_station", 2, 4, 2, "galv")
for x, z in ((0.1, 0.1), (1.7, 0.1), (0.1, 1.7), (1.7, 1.7)):
	BS.box((x, 0.0, z), (x + 0.2, 3.6, z + 0.2), "iron")                    # the frame
BS.box((0.1, 3.4, 0.1), (1.9, 3.9, 1.9), "galv")                            # the feed hopper
BS.cylinder("y", (1.0, 1.0), 0.2, 2.6, 3.4, "steel", 10)                    # the filling spout
BS.box((0.3, 0.2, 0.3), (1.7, 1.8, 1.7), "polymer")                         # a bag on the scale
BS.box((0.2, 0.0, 0.2), (1.8, 0.2, 1.8), "checker")
box480(BS, 0.5, 0.3)
BS.collide((0.1, 0.0, 0.1), (1.9, 3.9, 1.9))

# ---- the chlorine filler -----------------------------------------------------------------------------------------------------------
CF = model("chlorine_filler", 3, 4, 3, "grey")
CF.box((0.0, 0.0, 0.0), (3.0, 0.2, 3.0), "concrete")
CF.box((0.3, 0.2, 0.3), (1.6, 1.6, 1.4), "blue")                            # the refrigeration compressor
CF.cylinder("y", (2.3, 0.8), 0.4, 0.2, 3.2, "steel", 12)                    # the liquefier
CF.box((0.3, 0.2, 1.8), (2.7, 0.4, 2.8), "checker")                         # the filling scale
CF.cylinder("x", (1.0, 2.3), 0.42, 0.5, 2.5, "padgreen", 14)                # a ton container on it
inlet(CF, 2.3)
box480(CF, 0.5, 1.5)
CF.collide((0.3, 0.0, 0.3), (2.7, 3.2, 2.8))


M = BM
EXTRA = [DG, FT, ST, MW, FP, PF, PR, HF, CC, EV, CZ, PB, LK, AR, BS, CF]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
