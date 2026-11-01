"""The uranium mill at true size (docs/multiblocks/uranium-mill.md), each part drawn facing north from its own main cell (the
footprints match MillPartBlock's):
- jaw_crusher, 3 x 4 x 4: the jaw crusher on its plinth, its feed hopper, the flywheels and the 75 kW motor;
- sag_mill, 7 x 7 x 5: the 5.5 m semi-autogenous mill on its trunnions, the ring gear and pinion, the 522 kW motor (4.16 kV box on
  the west), the discharge trommel and sump; ore slurry out the back;
- leach_tank, 8 x 9 x 8: the rubber-lined 7.6 m tank, its bridge and the 56 kW agitator drive; slurry in the front, acid on the
  east, leach slurry out the back;
- ccd_thickener, 12 x 4 x 12: the 12 m high-rate thickener on its legs, the bridge and rake drive; liquor out the back;
- sx_circuit, 6 x 3 x 16: a train of covered mixer-settlers, the mixer drives along the west side; strip liquor out the back;
- precipitation_tank, 4 x 5 x 4: the agitated tank and its magnesia feeder;
- yellowcake_dryer, 3 x 8 x 3: the spray dryer's chamber and cone, the burner and the bag house;
- drum_filler, 2 x 4 x 2: the filling head and its dust hood over the drum station."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def disc(M, cx, cz, r, y0, y1, tex):
	"""A round lid or liquid surface too wide for one turned slab: an octagon of three flat boxes inside the radius."""
	a, b = r * 0.92, r * 0.38
	M.box((cx - b, y0, cz - a), (cx + b, y1, cz + a), tex)
	M.box((cx - a, y0, cz - b), (cx + a, y1, cz + b), tex)
	M.box((cx - r * 0.65, y0, cz - r * 0.65), (cx + r * 0.65, y1, cz + r * 0.65), tex)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


# ---- the jaw crusher --------------------------------------------------------------------------------------------------------------
JC = model("jaw_crusher", 3, 4, 4, "padgreen")
JC.box((0.0, 0.0, 0.0), (3.0, 0.8, 4.0), "concrete")                        # the plinth
JC.box((0.6, 0.8, 0.8), (2.4, 2.6, 3.2), "padgreen")                        # the frame
JC.box((0.4, 2.6, 0.4), (2.6, 3.9, 2.6), "iron")                            # the feed hopper
JC.cylinder("x", (2.0, 2.6), 0.75, 0.3, 0.6, "iron", 16)                     # the flywheels
JC.cylinder("x", (2.0, 2.6), 0.75, 2.4, 2.7, "iron", 16)
JC.cylinder("z", (2.4, 1.3), 0.4, 3.2, 3.95, "blue", 12)                     # the 75 kW motor
box480(JC, 0.9, 0.3)
JC.collide((0.3, 0.0, 0.0), (2.7, 3.9, 4.0))

# ---- the SAG mill -----------------------------------------------------------------------------------------------------------------
SG = model("sag_mill", 7, 7, 5, "padgreen")
SG.box((0.0, 0.0, 0.0), (7.0, 0.6, 5.0), "concrete")
SG.box((0.3, 0.6, 0.2), (1.0, 3.4, 4.8), "concrete")                        # the trunnion piers
SG.box((6.0, 0.6, 0.2), (6.7, 3.4, 4.8), "concrete")
SG.ring("x", (3.4, 2.5), 2.75, 1.0, 6.0, 0.1, "padgreen", sides=24)                # the shell, 5.5 m across
SG.box((1.00, 0.87, -0.03), (1.05, 5.93, 5.03), "padgreen")
SG.box((5.95, 0.87, -0.03), (6.00, 5.93, 5.03), "padgreen")
SG.cylinder("x", (3.4, 2.5), 0.9, 0.3, 1.0, "iron", 16)                     # the feed trunnion
SG.ring("x", (3.4, 2.5), 2.9, 4.6, 4.9, 0.12, "iron", sides=24)              # the ring gear
SG.box((4.3, 0.6, 0.3), (5.3, 1.6, 1.0), "iron")                            # the pinion
SG.cylinder("z", (4.8, 1.2), 0.75, 0.0, 0.3, "blue", 16)                    # the 522 kW motor, ahead of the pinion
SG.box((6.0, 0.6, 4.0), (6.9, 1.6, 4.95), "galv")                           # the discharge sump
box480(SG, 0.8, 0.2)
SG.pipe([(3.4, 0.8, 4.6), (3.4, 0.8, 5.0)], 8, "rubber")                    # ore slurry out, the back
SG.collide((0.3, 0.0, 0.0), (6.9, 6.2, 5.0))

# ---- the leach tank ---------------------------------------------------------------------------------------------------------------
LT = model("leach_tank", 8, 9, 8, "grey")
LT.box((0.0, 0.0, 0.0), (8.0, 0.3, 8.0), "concrete")
LT.ring("y", (4.0, 4.0), 3.8, 0.3, 8.5, 0.1, "grey", sides=28)                      # the 7.6 m tank
disc(LT, 4.00, 4.00, 3.80, 8.0, 8.05, "dark")   # the slurry: an open tank under its bridge
LT.box((0.3, 8.5, 3.6), (7.7, 8.7, 4.4), "grating")                         # the bridge
LT.box((3.4, 8.7, 3.5), (4.6, 8.98, 4.5), "blue")                           # the agitator's gearbox and motor
LT.handrail([(0.3, 3.6), (7.7, 3.6)], 8.7, "galv", 0.25)
LT.pipe([(4.0, 0.8, 0.0), (4.0, 0.8, 0.25)], 8, "rubber")                   # ore slurry in, the front
LT.pipe([(8.0, 0.6, 4.0), (7.75, 0.6, 4.0)], 2, "steel")                    # acid in, the east
LT.pipe([(4.0, 0.8, 7.75), (4.0, 0.8, 8.0)], 8, "rubber")                   # leach slurry out, the back
box480(LT, 0.5, 3.0)
LT.collide((0.3, 0.0, 0.3), (7.7, 8.98, 7.7))

# ---- the CCD thickener ------------------------------------------------------------------------------------------------------------
CT = model("ccd_thickener", 12, 4, 12, "grey")
for x in (1.5, 10.5):
	for z in (1.5, 10.5):
		CT.box((x - 0.2, 0.0, z - 0.2), (x + 0.2, 1.0, z + 0.2), "iron")     # its legs
CT.cylinder("y", (6.0, 6.0), 0.95, 0.0, 1.0, "grey", 16)                     # the underflow cone
CT.ring("y", (6.0, 6.0), 5.95, 1.0, 3.2, 0.1, "grey", sides=32)                    # the 12 m tank
disc(CT, 6.00, 6.00, 5.95, 2.9, 2.95, "dark")   # the liquor: an open tank under its bridge
CT.box((0.4, 3.2, 5.6), (11.6, 3.45, 6.4), "grating")                       # the bridge
CT.box((5.5, 3.45, 5.5), (6.5, 3.95, 6.5), "blue")                          # the rake drive
CT.pipe([(6.0, 0.6, 0.0), (6.0, 0.6, 1.0)], 8, "rubber")                    # leach slurry in, the front
CT.pipe([(6.0, 0.5, 11.0), (6.0, 0.5, 12.0)], 8, "steel")                   # pregnant liquor out, the back
box480(CT, 0.4, 5.5)
CT.collide((0.05, 1.0, 0.05), (11.95, 3.45, 11.95))
CT.collide((4.8, 0.0, 4.8), (7.2, 1.0, 7.2))

# ---- the SX circuit ---------------------------------------------------------------------------------------------------------------
SX = model("sx_circuit", 6, 3, 16, "padgreen")
SX.box((0.0, 0.0, 0.0), (6.0, 0.25, 16.0), "concrete")
for z in (0.3, 4.3, 8.3, 12.3):
	SX.box((1.4, 0.25, z), (5.8, 1.5, z + 3.6), "padgreen")                 # a settler
	SX.box((1.3, 1.5, z - 0.05), (5.9, 1.6, z + 3.65), "grey")              # its cover
	SX.box((0.4, 0.25, z + 0.5), (1.4, 1.8, z + 1.5), "padgreen")           # its mixer box
	SX.cylinder("y", (0.9, z + 1.0), 0.25, 1.8, 2.5, "blue", 10)            # the mixer's drive
SX.pipe([(3.0, 0.5, 0.0), (3.0, 0.5, 0.3)], 6, "steel")                    # pregnant liquor in, the front
SX.pipe([(6.0, 0.5, 8.0), (5.8, 0.5, 8.0)], 3, "steel")                    # kerosene in, the east
SX.pipe([(3.0, 0.5, 15.9), (3.0, 0.5, 16.0)], 3, "steel")                  # strip liquor out, the back
box480(SX, 0.4, 0.3)
SX.collide((0.3, 0.0, 0.25), (5.9, 1.6, 15.95))

# ---- the precipitation tank -------------------------------------------------------------------------------------------------------
PT = model("precipitation_tank", 4, 5, 4, "steel")
PT.box((0.0, 0.0, 0.0), (4.0, 0.3, 4.0), "concrete")
PT.ring("y", (2.0, 2.0), 1.6, 0.3, 3.8, 0.1, "steel", sides=20)
disc(PT, 2.00, 2.00, 1.60, 3.75, 3.80, "steel")   # its roof
PT.box((0.3, 3.8, 1.7), (3.7, 4.0, 2.3), "grating")
PT.box((1.6, 4.0, 1.6), (2.4, 4.6, 2.4), "blue")                            # the agitator drive
PT.box((2.6, 3.8, 2.6), (3.6, 4.9, 3.6), "galv")                            # the magnesia feeder
PT.pipe([(2.0, 0.6, 0.0), (2.0, 0.6, 0.4)], 3, "steel")                    # strip liquor in, the front
box480(PT, 0.4, 0.3)
PT.collide((0.4, 0.0, 0.4), (3.6, 4.0, 3.6))

# ---- the yellowcake dryer ---------------------------------------------------------------------------------------------------------
YD = model("yellowcake_dryer", 3, 8, 3, "steel")
for x in (0.3, 2.5):
	for z in (0.3, 2.5):
		YD.box((x, 0.0, z), (x + 0.2, 3.0, z + 0.2), "iron")
YD.cylinder("y", (1.5, 1.5), 0.4, 1.8, 3.0, "steel", 12)                    # the cone's outlet
YD.cylinder("y", (1.5, 1.5), 1.2, 3.0, 7.0, "steel", 18)                    # the drying chamber
YD.cylinder("y", (1.5, 1.5), 0.4, 7.0, 7.8, "hipaint", 12)                  # the burner and atomizer
YD.pipe([(3.0, 0.5, 1.5), (2.6, 0.5, 1.5), (2.6, 7.4, 1.5), (1.9, 7.4, 1.5)], 2, "steel")   # gas in, the east, up to the burner
YD.collide((0.3, 0.0, 0.3), (2.7, 7.8, 2.7))

# ---- the drum filler --------------------------------------------------------------------------------------------------------------
DF = model("drum_filler", 2, 4, 2, "grey")
DF.box((0.0, 0.0, 0.0), (2.0, 0.2, 2.0), "checker")                         # the drum station's scale deck
DF.cylinder("y", (1.0, 1.0), 0.29, 0.2, 1.08, "blue", 14)                   # a 208 L drum on the scale
for x in (0.1, 1.75):
	DF.box((x, 0.2, 0.1), (x + 0.15, 3.0, 1.9), "grey")                     # the frame
DF.box((0.1, 2.0, 0.1), (1.9, 3.0, 1.9), "grey")                            # the dust hood and filling head
DF.cylinder("y", (1.0, 1.0), 0.15, 1.3, 2.0, "steel", 10)
DF.box((0.4, 3.0, 0.4), (1.6, 3.95, 1.6), "galv")                           # the surge bin
box480(DF, 0.6, 0.6)
DF.collide((0.1, 0.0, 0.1), (1.9, 3.95, 1.9))


M = JC
EXTRA = [SG, LT, CT, SX, PT, YD, DF]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
