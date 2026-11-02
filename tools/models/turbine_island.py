"""The turbine island at true size (docs/multiblocks/turbine-island.md), each part drawn facing north from its own main cell (the
footprints match TurbineIslandPartBlock's, TurbinePartBlock's TG_GENERATOR_60 and UatTransformerBlock's):
- turbine_valve_chest, 3 x 4 x 3: the stop and governor valve bodies, their actuators, the steam inlet on top;
- hp_turbine, 4 x 4 x 6: the HP casing on its pedestal, the crossover pipes to the MSR;
- moisture_separator_reheater, 3 x 4 x 10: the horizontal vessel on saddles;
- lp_turbine, 6 x 5 x 8: the double-flow LP casing over its exhaust hood;
- tg_generator_60, 4 x 4 x 8: the hydrogen-cooled generator, its coolers and the terminal box at the back;
- main_condenser, 6 x 5 x 8: the shell, water boxes at each end, the hotwell;
- condensate_pump, 2 x 3 x 2: a vertical can pump and its motor;
- deaerator, 3 x 5 x 8: the storage tank with the deaerating head on top;
- main_feed_pump, 2 x 2 x 5: motor, coupling and barrel pump on a skid;
- hp_feedwater_heater, 2 x 3 x 8: the horizontal U-tube heater shells, feedwater out to the south;
- circulating_water_pump, 3 x 8 x 3: the column and bell down in the intake, the discharge head and motor above;
- unit_auxiliary_transformer, 4 x 4 x 4: the 7.5 MVA tank, radiators, three 13.8 kV bushings over the front."""
import mlib
import plantlib as g

TEX = dict(g.TEX)
TEX.update({"porcelain": "block/mat/mat_porcelain_brown"})


def model(name, w, h, d, particle="grey", main=(0, 0)):
	return mlib.Model(name, mlib.footprint_box(w, h, d, main[0], main[1]), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


VC = model("turbine_valve_chest", 3, 4, 3, "steel")
VC.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
for x in (0.8, 2.2):
	VC.cylinder("y", (x, 1.5), 0.5, 0.3, 2.6, "steel", 12)                  # a stop valve and a governor valve body
	VC.cylinder("y", (x, 1.5), 0.2, 2.6, 3.4, "blue", 8)                    # its hydraulic actuator
VC.cylinder("y", (1.5, 1.5), 0.35, 3.4, 4.0, "steel", 10)                   # the steam inlet
VC.collide((0.2, 0.0, 0.2), (2.8, 4.0, 2.8))

HP = model("hp_turbine", 4, 4, 6, "blue")
HP.box((0.0, 0.0, 0.0), (4.0, 1.0, 6.0), "concrete")                        # the pedestal
HP.cylinder("z", (2.5, 2.5), 1.3, 0.3, 5.7, "blue", 16)                     # the HP casing
HP.cylinder("z", (2.5, 2.5), 0.3, 0.0, 0.3, "steel", 10)                    # the shaft to the governor end
for z in (1.5, 4.5):
	HP.pipe([(3.6, 2.8, z), (4.0, 2.8, z)], 10, "steel")                    # the crossovers to the MSR
HP.collide((0.3, 0.0, 0.2), (3.7, 3.6, 5.8))

MS = model("moisture_separator_reheater", 3, 4, 10, "steel")
for z in (1.0, 5.0, 9.0):
	MS.box((0.4, 0.0, z - 0.3), (2.6, 1.0, z + 0.3), "concrete")            # its saddles
MS.cylinder("z", (1.5, 2.5), 1.4, 0.2, 9.8, "steel", 16)                    # the vessel
MS.collide((0.1, 0.0, 0.2), (2.9, 3.7, 9.8))

LP = model("lp_turbine", 6, 5, 8, "blue")
LP.box((0.0, 0.0, 0.0), (6.0, 1.8, 8.0), "grey")                            # the exhaust hood down to the condenser
LP.box((0.4, 1.8, 0.6), (5.6, 3.6, 7.4), "blue")                            # the outer casing
LP.cylinder("z", (2.5, 3.5), 1.45, 1.2, 6.8, "blue", 18)                     # the double-flow casing
LP.cylinder("z", (2.5, 2.5), 0.3, 0.0, 1.2, "steel", 10)                    # the shaft
LP.cylinder("z", (2.5, 2.5), 0.3, 6.8, 8.0, "steel", 10)
LP.collide((0.0, 0.0, 0.0), (6.0, 4.9, 8.0))

TG = model("tg_generator_60", 4, 4, 8, "blue")
TG.box((0.0, 0.0, 0.0), (4.0, 0.4, 8.0), "iron")
TG.cylinder("z", (2.5, 1.5), 1.4, 0.3, 7.2, "blue", 18)                     # the stator frame, hydrogen-tight
for z in (1.2, 4.2):
	TG.box((0.5, 3.0, z), (3.5, 3.95, z + 2.4), "grey")                     # the hydrogen coolers on top
	TG.fins((0.55, 3.05, z + 0.1), (3.45, 3.9, z + 2.3), "x", 12, 0.03, "galv")
TG.cylinder("z", (2.5, 1.5), 0.35, 0.0, 0.3, "steel", 10)                   # the coupling end
TG.box((0.4, 0.4, 7.2), (3.6, 2.6, 8.0), "grey")                            # the terminal box, back
TG.decal((3.6, 1.0, 2.5), (3.61, 2.0, 4.5), "ratingplate", "east", "blue")
TG.collide((0.0, 0.0, 0.0), (4.0, 3.95, 8.0))

CD = model("main_condenser", 6, 5, 8, "steel")
CD.box((0.0, 0.0, 0.0), (6.0, 0.4, 8.0), "concrete")
CD.box((0.3, 0.4, 0.8), (5.7, 4.6, 7.2), "steel")                           # the shell under the LP exhaust
for z0, z1 in ((0.1, 0.8), (7.2, 7.9)):
	CD.box((0.6, 0.6, z0), (5.4, 4.0, z1), "blue")                          # the water boxes
CD.box((1.0, 0.0, 2.0), (5.0, 0.4, 6.0), "dark")                            # the hotwell
CD.collide((0.1, 0.0, 0.1), (5.9, 4.6, 7.9))

CP = model("condensate_pump", 2, 3, 2, "blue")
CP.cylinder("y", (1.0, 1.0), 0.55, 0.0, 1.2, "steel", 12)                   # the can and discharge head
CP.cylinder("y", (1.0, 1.0), 0.45, 1.2, 2.8, "blue", 12)                    # its motor
box480(CP, 0.6, 0.3)
CP.collide((0.3, 0.0, 0.3), (1.7, 2.8, 1.7))

DA = model("deaerator", 3, 5, 8, "steel")
for z in (1.0, 4.0, 7.0):
	DA.box((0.4, 0.0, z - 0.3), (2.6, 1.0, z + 0.3), "concrete")
DA.cylinder("z", (1.5, 2.5), 1.3, 0.2, 7.8, "steel", 16)                    # the storage tank
DA.cylinder("y", (1.5, 2.0), 0.6, 3.4, 4.9, "steel", 12)                    # the deaerating head
DA.collide((0.2, 0.0, 0.2), (2.8, 4.9, 7.8))

FP = model("main_feed_pump", 2, 2, 5, "blue")
g.skid(FP, 0.1, 1.9, 0.1, 4.9)
FP.cylinder("z", (1.0, 0.75), 0.5, 0.3, 2.4, "blue", 12)                    # the 4.16 kV motor
FP.cylinder("z", (1.0, 0.75), 0.15, 2.4, 2.8, "steel", 8)                   # the coupling
FP.cylinder("z", (1.0, 0.75), 0.5, 2.8, 4.7, "iron", 12)                    # the barrel pump
box480(FP, 0.3, 0.6)
FP.collide((0.1, 0.0, 0.1), (1.9, 1.3, 4.9))

HX = model("hp_feedwater_heater", 2, 3, 8, "steel")
for z in (1.0, 6.5):
	HX.box((0.2, 0.0, z - 0.25), (1.8, 0.6, z + 0.25), "concrete")
for y in (0.9, 2.1):
	HX.cylinder("z", (1.0, y), 0.55, 0.3, 7.6, "steel", 14)                 # two heater shells, one over the other
HX.pipe([(1.0, 0.5, 7.6), (1.0, 0.5, 8.0)], 6, "steel")                     # feedwater out, the south
HX.collide((0.2, 0.0, 0.2), (1.8, 2.7, 7.8))

CW = model("circulating_water_pump", 3, 8, 3, "blue")
CW.cylinder("y", (1.5, 1.5), 0.9, 0.0, 0.6, "iron", 14)                     # the suction bell
CW.cylinder("y", (1.5, 1.5), 0.6, 0.6, 3.0, "iron", 12)                     # the column, in the water
CW.box((0.2, 3.0, 0.2), (2.8, 3.4, 2.8), "concrete")                        # the pump floor
CW.cylinder("y", (1.5, 1.5), 0.75, 3.4, 4.6, "steel", 12)                   # the discharge head
CW.pipe([(2.2, 3.9, 1.5), (3.0, 3.9, 1.5)], 12, "steel")                    # circulating water out, the east
CW.cylinder("y", (1.5, 1.5), 0.85, 4.6, 7.6, "blue", 14)                    # the 4.16 kV motor
box480(CW, 3.5, 0.3)
CW.collide((0.2, 0.0, 0.2), (2.8, 7.6, 2.8))

UA = model("unit_auxiliary_transformer", 4, 4, 4, "blue", main=(1, 3))
UA.box((-0.9, 0.0, -2.9), (2.9, 0.3, 0.9), "iron")
UA.box((-0.6, 0.3, -2.6), (2.6, 2.9, 0.6), "blue")                          # the tank
for x in (-1.0, 2.6):
	UA.fins((x, 0.6, -2.3), (x + 0.4, 2.6, 0.3), "z", 8, 0.04, "radfin")    # radiators
for x in (0.0, 1.0, 2.0):
	UA.cylinder("y", (x, -2.1), 0.13, 2.9, 3.8, "porcelain", 8)             # the 13.8 kV bushings
	UA.cylinder("y", (x, -2.1), 0.04, 3.8, 3.95, "copper", 6)
UA.box((-0.2, 0.4, 0.6), (1.0, 1.8, 0.95), "grey")                          # the 4.16 kV throat, back
UA.decal((2.6, 1.0, -1.8), (2.61, 1.8, -0.6), "ratingplate", "east", "blue")
UA.collide((-1.0, 0.0, -2.9), (3.0, 2.9, 0.95))

M = HP
EXTRA = [VC, MS, LP, TG, CD, CP, DA, FP, HX, CW, UA]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
