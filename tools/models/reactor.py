"""The nuclear island at true size (docs/multiblocks/reactor.md), each part drawn facing north from its own main cell (the footprints
match ReactorPartBlock's):
- reactor_vessel, 3 x 10 x 3: the vessel on its skirt, its hot and cold leg nozzles north and south, the closure flange;
- crdm_head, 3 x 3 x 3: the closure head with five CRDM housings and their rod position indicator coils;
- reactor_console, 3 x 2 x 1: the main control board;
- steam_generator, 3 x 12 x 3: the U-tube steam generator, its channel head, lower and upper shells, steam nozzle and dump valve;
- reactor_coolant_pump, 2 x 5 x 2: the pump casing, motor stand, 4.16 kV motor and flywheel;
- pressurizer, 2 x 8 x 2: the vessel on its skirt, its relief and safety valves on top;
- cvcs_skid, 3 x 3 x 6: the volume control tank, boric acid tank, demineralizer, exchangers and charging pump;
- aux_feed_pump, 2 x 2 x 4: the motor, coupling and pump on a skid, the feedwater out of its south end."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


# ---- the reactor vessel ---------------------------------------------------------------------------------------------------------------
RV = model("reactor_vessel", 3, 10, 3, "steel")
RV.cylinder("y", (1.5, 1.5), 1.2, 0.0, 0.8, "concrete", 16)                 # the support skirt
RV.cylinder("y", (1.5, 1.5), 1.35, 0.8, 9.3, "steel", 16)                   # the vessel shell
RV.ring("y", (1.5, 1.5), 1.45, 9.3, 9.9, 0.15, "iron", sides=16)            # the closure flange and studs
for z0, z1 in ((0.0, 0.3), (2.7, 3.0)):
	RV.cylinder("z", (1.0, 7.6), 0.38, z0, z1, "steel", 12)                  # the hot legs
	RV.cylinder("z", (2.1, 7.6), 0.35, z0, z1, "steel", 12)                  # the cold legs
RV.collide((0.15, 0.0, 0.15), (2.85, 9.9, 2.85))

# ---- the CRDM head ----------------------------------------------------------------------------------------------------------------------
CH = model("crdm_head", 3, 3, 3, "steel")
CH.cylinder("y", (1.5, 1.5), 1.35, 0.0, 0.35, "steel", 16)                  # the closure head
CH.cylinder("y", (1.5, 1.5), 1.0, 0.35, 0.6, "steel", 16)
for x, z in ((1.5, 1.5), (1.5, 0.75), (1.5, 2.25), (0.75, 1.5), (2.25, 1.5)):
	CH.cylinder("y", (x, z), 0.1, 0.6, 2.8, "galv", 8)                       # a CRDM housing
	CH.cylinder("y", (x, z), 0.16, 1.2, 1.9, "blue", 8)                      # its coil stack and position indicator
CH.box((0.2, 2.8, 0.2), (2.8, 2.95, 2.8), "grating")                        # the missile shield and cable support
box480(CH, 0.4, 1.2)
CH.collide((0.15, 0.0, 0.15), (2.85, 2.95, 2.85))

# ---- the console ---------------------------------------------------------------------------------------------------------------------
CO = model("reactor_console", 3, 2, 1, "grey")
CO.box((0.05, 0.0, 0.25), (2.95, 0.85, 0.95), "grey")                       # the desk
CO.box((0.05, 0.85, 0.6), (2.95, 1.9, 0.95), "grey")                        # the vertical board
for x in (0.2, 1.1, 2.0):
	CO.decal((x, 1.05, 0.59), (x + 0.75, 1.7, 0.6), "controller", "north", "grey")
	CO.decal((x, 0.86, 0.3), (x + 0.75, 0.87, 0.55), "gauge", "up", "grey")
CO.collide((0.05, 0.0, 0.25), (2.95, 1.9, 0.95))

# ---- the steam generator -----------------------------------------------------------------------------------------------------------
SG = model("steam_generator", 3, 12, 3, "steel")
for x, z in ((0.5, 0.5), (2.5, 0.5), (0.5, 2.5), (2.5, 2.5)):
	SG.box((x - 0.12, 0.0, z - 0.12), (x + 0.12, 0.6, z + 0.12), "iron")    # its support columns
SG.cylinder("y", (1.5, 1.5), 0.95, 0.4, 1.1, "iron", 16)                    # the channel head
SG.cylinder("y", (1.5, 1.5), 0.9, 1.1, 7.2, "steel", 16)                    # the lower shell, round the tube bundle
SG.cylinder("y", (1.5, 1.5), 1.1, 7.2, 7.8, "steel", 16)                    # the transition cone
SG.cylinder("y", (1.5, 1.5), 1.35, 7.8, 11.3, "steel", 16)                  # the upper shell, the separators and dryers
SG.cylinder("y", (1.5, 1.5), 0.9, 11.3, 11.7, "steel", 16)                  # the top head
SG.cylinder("y", (1.5, 1.5), 0.3, 11.7, 12.0, "steel", 10)                  # the steam nozzle
SG.cylinder("y", (2.4, 2.3), 0.09, 11.3, 11.95, "iron", 8)                  # the dump valve
SG.pipe([(3.0, 0.5, 1.5), (2.3, 0.5, 1.5)], 3, "steel")                     # feedwater in, the east
SG.collide((0.15, 0.0, 0.15), (2.85, 12.0, 2.85))

# ---- the reactor coolant pump --------------------------------------------------------------------------------------------------------
RP = model("reactor_coolant_pump", 2, 5, 2, "blue")
RP.cylinder("y", (1.0, 1.0), 0.8, 0.0, 1.3, "steel", 14)                    # the pump casing
RP.cylinder("y", (1.0, 1.0), 0.45, 1.3, 2.2, "iron", 12)                    # the motor stand and seal housing
RP.cylinder("y", (1.0, 1.0), 0.85, 2.2, 4.3, "blue", 14)                    # the 4.16 kV motor
RP.cylinder("y", (1.0, 1.0), 0.7, 4.3, 4.8, "iron", 14)                     # the flywheel cover
box480(RP, 2.5, 0.3)
RP.collide((0.15, 0.0, 0.15), (1.85, 4.8, 1.85))

# ---- the pressurizer ------------------------------------------------------------------------------------------------------------------
PZ = model("pressurizer", 2, 8, 2, "steel")
PZ.cylinder("y", (1.0, 1.0), 0.7, 0.0, 0.6, "concrete", 12)                 # the skirt
PZ.cylinder("y", (1.0, 1.0), 0.8, 0.6, 7.4, "steel", 14)                    # the shell
PZ.cylinder("y", (1.0, 1.0), 0.55, 7.4, 7.7, "steel", 12)                   # the top head
for x in (0.6, 1.0, 1.4):
	PZ.cylinder("y", (x, 1.0), 0.07, 7.7, 7.98, "iron", 6)                   # the PORVs and safety valves
box480(PZ, 0.8, 0.3)
PZ.collide((0.15, 0.0, 0.15), (1.85, 7.98, 1.85))

# ---- the CVCS skid --------------------------------------------------------------------------------------------------------------------
CV = model("cvcs_skid", 3, 3, 6, "grey")
CV.box((0.0, 0.0, 0.0), (3.0, 0.2, 6.0), "concrete")
CV.cylinder("y", (0.9, 1.0), 0.55, 0.2, 2.6, "steel", 12)                   # the volume control tank
CV.cylinder("y", (2.1, 1.1), 0.65, 0.2, 2.8, "galv", 12)                    # the boric acid tank
CV.cylinder("y", (0.9, 2.8), 0.35, 0.2, 2.2, "steel", 10)                   # the demineralizer
CV.cylinder("z", (2.1, 0.8), 0.3, 2.4, 4.6, "steel", 10)                    # the letdown and regenerative exchangers
CV.box((0.4, 0.2, 4.4), (1.6, 0.9, 5.8), "blue")                            # the charging pump
box480(CV, 0.4, 3.6)
CV.collide((0.2, 0.0, 0.2), (2.8, 2.8, 5.8))

# ---- the aux feed pump ---------------------------------------------------------------------------------------------------------------
AF = model("aux_feed_pump", 2, 2, 4, "blue")
g.skid(AF, 0.2, 1.8, 0.2, 3.8)
AF.cylinder("z", (1.0, 0.65), 0.38, 0.3, 1.9, "blue", 12)                   # the 75 hp motor
AF.cylinder("z", (1.0, 0.65), 0.12, 1.9, 2.3, "steel", 8)                   # the coupling
AF.cylinder("z", (1.0, 0.65), 0.42, 2.3, 3.1, "iron", 12)                   # the multistage pump
AF.pipe([(1.0, 0.5, 3.1), (1.0, 0.5, 4.0)], 3, "steel")                     # feedwater out, the south
box480(AF, 0.3, 0.4)
AF.collide((0.2, 0.0, 0.2), (1.8, 1.1, 3.9))

M = RV
EXTRA = [CH, CO, SG, RP, PZ, CV, AF]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
