"""The potline at true size (docs/multiblocks/potline.md), each part drawn facing north from its own main cell (the footprints match
PotlinePartBlock's). Motor boxes are on the west of the main cell (the main fan's is 4.16 kV); the rectiformer's 138 kV terminal is
there too, its DC terminals on the east face at z 2 (+ at y 2 over - at y 0); the pot's anode riser is on its west face and its
cathode collector bars on its east face at y 1, z 5, its main duct across the top at y 5:
- rectiformer, 8 x 7 x 6: the regulating transformer's tank, radiators and conservator, the 138 kV bushings, the diode cubicles;
- pot, 6 x 6 x 12: the shell on its cradles, the hoods, the superstructure with its anode beam and rods, the feeders, the duct;
- fume_treatment_centre, 8 x 12 x 16: the injection reactor, the bag filter house over its hoppers, the stack;
- ftc_fan, 4 x 5 x 6: the centrifugal main fan, its 250 kW motor;
- casting_machine, 3 x 3 x 12: the pour box, the mould chain under its sprays, the stacker."""
import mlib
import plantlib as g

TEX = dict(g.TEX)
TEX["glaze"] = "block/mat/mat_porcelain_brown"


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


# ---- the rectiformer -----------------------------------------------------------------------------------------------------------
RT = model("rectiformer", 8, 7, 6, "blue")
RT.box((0.0, 0.0, 0.0), (8.0, 0.3, 6.0), "concrete")
RT.box((0.6, 0.3, 1.0), (4.6, 4.6, 5.0), "blue")                            # the regulating transformer's tank
for z in (0.3, 5.0):
	RT.fins((0.9, 0.6, z), (4.3, 4.2, z + 0.7), "x", 9, 0.06, "radfin")    # radiators, north and south
RT.cylinder("x", (5.3, 1.5), 0.45, 0.8, 4.4, "blue", 12)                    # the conservator
for x in (1.2, 2.4, 3.6):
	RT.cylinder("y", (x, 2.0), 0.18, 4.6, 6.6, "glaze", 8)   # 138 kV bushings
RT.box((0.0, 0.0, 0.0), (0.6, 1.0, 1.0), "grey")                            # the 138 kV terminal box
RT.decal((-0.01, 0.2, 0.2), (0.0, 0.8, 0.8), "danger", "west", "grey")
RT.box((4.6, 1.2, 1.6), (5.2, 2.6, 4.4), "grey")                            # the saturable reactors
RT.box((5.2, 0.3, 0.6), (7.6, 3.6, 5.4), "grey")                            # the diode cubicles
RT.decal((5.4, 1.0, 0.59), (7.4, 3.0, 0.6), "louvre", "north", "grey")
RT.box((7.6, 2.0, 1.6), (8.0, 3.0, 3.4), "hipaint")                         # DC + terminal
RT.box((7.6, 0.3, 1.6), (8.0, 1.0, 3.4), "hipaint")                         # DC - terminal
RT.collide((0.0, 0.0, 0.0), (8.0, 4.6, 6.0))

# ---- the pot --------------------------------------------------------------------------------------------------------------------
PT = model("pot", 6, 6, 12, "iron")
PT.box((0.0, 0.0, 0.0), (6.0, 0.2, 12.0), "concrete")                       # the potroom floor
PT.box((0.9, 0.2, 0.75), (5.1, 1.6, 11.25), "iron")                         # the steel shell
for z in [0.75 + i * 1.05 for i in range(11)]:
	PT.box((0.8, 0.2, z), (5.2, 1.7, z + 0.15), "dark")                     # its cradles
PT.box((1.0, 1.6, 0.9), (5.0, 2.7, 11.1), "galv")                           # the hood panels
PT.box((2.5, 2.7, 0.4), (3.5, 4.7, 11.6), "blue")                           # the superstructure and anode beam
for z in [1.3 + i * 1.05 for i in range(9)]:
	for x in (1.6, 4.2):
		PT.box((x, 2.7, z), (x + 0.2, 4.2, z + 0.15), "hipaint")            # the anode rods, 18
PT.box((1.4, 4.0, 1.0), (4.6, 4.2, 11.0), "hipaint")                        # the anode bus beam
for z in (3.0, 6.0, 9.0):
	PT.box((2.7, 4.7, z), (3.3, 5.2, z + 0.6), "grey")                      # the point feeders' hoppers
PT.cylinder("x", (5.3, 5.0), 0.5, 0.0, 6.0, "galv", 12)                     # the row's main duct
PT.box((0.0, 0.3, 4.5), (0.9, 1.8, 5.5), "hipaint")                         # the anode riser (+), west
PT.box((5.1, 0.3, 4.5), (6.0, 1.0, 5.5), "hipaint")                         # the cathode collector bars (-), east
PT.collide((0.0, 0.0, 0.4), (6.0, 5.5, 11.6))

# ---- the fume treatment centre ----------------------------------------------------------------------------------------------------
FT = model("fume_treatment_centre", 8, 12, 16, "grey")
FT.box((0.0, 0.0, 0.0), (8.0, 0.3, 16.0), "concrete")
FT.box((0.0, 0.3, 0.0), (1.5, 1.8, 3.0), "galv")                            # the pot gas inlet duct, west
FT.cylinder("y", (2.5, 2.5), 1.2, 0.3, 8.0, "galv", 12)                     # the injection reactor
for z in (4.5, 7.5, 10.5):
	FT.box((0.8, 0.3, z), (7.2, 2.5, z + 2.6), "dark")                      # the dust hoppers' legs and hoppers
	FT.box((0.6, 2.5, z - 0.2), (7.4, 7.5, z + 2.8), "filter")              # the bag filter compartments
FT.box((0.6, 7.5, 4.3), (7.4, 8.0, 13.7), "grating")                        # the roof walkway
FT.cylinder("y", (6.5, 14.8), 0.8, 0.3, 12.0, "galv", 12)                   # the stack
FT.collide((0.0, 0.0, 0.0), (8.0, 8.0, 16.0))

# ---- the main fan ------------------------------------------------------------------------------------------------------------------
FF = model("ftc_fan", 4, 5, 6, "blue")
FF.box((0.0, 0.0, 0.0), (4.0, 0.3, 6.0), "concrete")
for a, b in ((1.85, 0.75), (0.75, 1.85), (1.3, 1.3)):
	FF.box((1.4, 2.6 - a, 3.0 - b), (2.8, 2.6 + a, 3.0 + b), "blue")        # the scroll casing, an octagon
FF.box((1.4, 0.3, 3.0), (2.8, 4.4, 5.6), "blue")                            # its discharge
FF.box((2.8, 0.3, 1.8), (3.9, 1.8, 4.2), "engine")                          # the 250 kW motor
FF.box((0.0, 0.3, 0.2), (0.4, 1.3, 0.9), "grey")                            # its 4.16 kV box
FF.decal((-0.01, 0.4, 0.3), (0.0, 1.2, 0.8), "danger", "west", "grey")
FF.collide((0.0, 0.0, 0.5), (4.0, 4.6, 6.0))

# ---- the casting machine -----------------------------------------------------------------------------------------------------------
CM = model("casting_machine", 3, 3, 12, "iron")
CM.box((0.0, 0.0, 0.0), (3.0, 0.3, 12.0), "concrete")
CM.box((0.5, 0.3, 0.2), (2.5, 1.9, 1.6), "hipaint")                         # the tilting pour box
for z in [1.8 + i * 0.5 for i in range(18)]:
	CM.box((0.6, 0.9, z), (2.4, 1.15, z + 0.4), "iron")                     # the moulds on their chain
CM.box((0.5, 0.3, 1.7), (2.5, 0.9, 10.9), "dark")                           # the chain's frame
CM.box((0.4, 2.0, 4.0), (2.6, 2.2, 9.0), "galv")                            # the water spray hood
CM.box((0.3, 0.3, 10.9), (2.7, 2.4, 11.9), "blue")                          # the stacker
box480(CM, 0.5, 0.3)
CM.collide((0.3, 0.0, 0.2), (2.7, 2.4, 11.9))


M = RT
EXTRA = [PT, FT, FF, CM]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
