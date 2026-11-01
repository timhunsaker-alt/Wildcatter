"""The crude units at true size (docs/multiblocks/crude-units.md), each part drawn facing north from its own main cell (the footprints
match RefineryPartBlock's):
- topping_column, 2 x 12 x 2: the 0.9 m column with its two small side strippers, platforms and ladder; kerosene, diesel and
  residue nozzles on its foot (east, south, west);
- topping_heater, 2 x 6 x 2: a vertical cylindrical fired heater: its burner plenum, radiant shell, convection box and stack;
- topping_skid, 3 x 3 x 4: the charge pump and its motor (the coupling turns), the air cooler on its frame with its fan on top
  (it turns), the reflux drum; crude in the north end, gasoline out of the south end, the terminal box on the west;
- atmospheric_column, 3 x 30 x 3: the 3 m trayed column, its skirt, platforms every 6 m and ladder, the residue nozzles at its foot;
- crude_heater, 6 x 14 x 8: a box (cabin) heater: the radiant box on its legs, burners underneath, the convection section, the stack;
- desalter, 3 x 4 x 10: the horizontal vessel on saddles, its grid transformer on top, the mixing valve, crude in the north end;
- crude_charge_pump, 2 x 2 x 5: a 250 hp 4.16 kV motor and a between-bearings pump on a baseplate (the coupling turns);
- side_strippers, 2 x 10 x 1: two 1 m towers on a shared frame;
- overhead_condenser, 4 x 4 x 8: a fin-fan bank on its stand, four fans on top (they turn), the reflux drum under it.
Turning parts: the topping skid's fan and the condenser's fin fans."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def rails(M, w, d, top=0.2):
	for x in (0.1, w - 0.3):
		M.box((x, 0.0, 0.05), (x + 0.2, top, d - 0.05), "iron")
	z = 0.4
	while z < d - 0.3:
		M.box((0.3, 0.02, z), (w - 0.3, top - 0.04, z + 0.1), "iron")
		z += 1.0


def platforms(M, cx, cz, r, ys, h):
	for y in ys:
		M.ring("y", (cx, cz), r + 0.35, y, y + 0.06, 0.35, "checker", sides=16)
		M.ring("y", (cx, cz), r + 0.7, y + 1.0, y + 1.05, 0.03, "galv", sides=16)


# ---- the topping unit ---------------------------------------------------------------------------------------------------------------
TC = model("topping_column", 2, 12, 2, "padgreen")
TC.box((0.1, 0.0, 0.1), (1.9, 0.3, 1.9), "concrete")
TC.cylinder("y", (1.0, 1.0), 0.5, 0.3, 1.0, "iron", 16)                    # the skirt
TC.cylinder("y", (1.0, 1.0), 0.45, 1.0, 11.6, "padgreen", 16)              # the column
TC.cylinder("y", (1.0, 1.0), 0.3, 11.6, 11.75, "padgreen", 12)
for x, y1, y2 in ((1.75, 5.0, 8.0), (0.25, 3.0, 6.0)):                     # its kerosene and diesel side strippers
	TC.cylinder("y", (x, 1.75), 0.18, y1, y2, "padgreen", 10)
	TC.pipe([(x, y2 - 0.3, 1.75), (x, y2 - 0.3, 1.0 + (0.4 if x > 1 else -0.4))], 1, "steel")
platforms(TC, 1.0, 1.0, 0.45, (4.0, 8.0, 11.2), 1.0)
TC.ladder(0.4, 0.05, 0.3, 11.4, -1, "galv")
TC.pipe([(1.45, 0.5, 1.0), (2.0, 0.5, 1.0)], 2, "steel")                   # kerosene, east
TC.pipe([(1.0, 0.5, 1.45), (1.0, 0.5, 2.0)], 2, "steel")                   # diesel, south
TC.pipe([(0.55, 0.5, 1.0), (0.0, 0.5, 1.0)], 3, "steel")                   # residue, west
TC.collide((0.1, 0.0, 0.1), (1.9, 1.0, 1.9))
TC.collide((0.55, 1.0, 0.55), (1.45, 11.75, 1.45))

TH = model("topping_heater", 2, 6, 2, "grey")
TH.box((0.1, 0.0, 0.1), (1.9, 0.6, 1.9), "iron")                         # the legs and burner plenum
for x, z in ((0.2, 0.2), (1.6, 0.2), (0.2, 1.6), (1.6, 1.6)):
	TH.box((x, 0.0, z), (x + 0.2, 1.0, z + 0.2), "iron")
TH.cylinder("y", (1.0, 1.0), 0.85, 1.0, 3.6, "grey", 16)                   # the radiant shell
TH.box((0.25, 3.6, 0.25), (1.75, 4.6, 1.75), "grey")                      # the convection box
TH.cylinder("y", (1.0, 1.0), 0.25, 4.6, 5.95, "dark", 12)                  # the stack
TH.box((0.0, 0.25, 0.85), (0.1, 0.55, 1.15), "dark")                      # the fuel train on its west
TH.collide((0.1, 0.0, 0.1), (1.9, 4.6, 1.9))

TS = model("topping_skid", 3, 3, 4, "blue")
rails(TS, 3, 4)
TS.box((0.05, 0.3, 0.2), (0.4, 0.9, 0.7), "grey")                         # the terminal box, west
TS.decal((0.04, 0.4, 0.25), (0.05, 0.8, 0.65), "danger", "west", "grey")
TS.cylinder("z", (0.9, 0.55), 0.25, 0.2, 0.9, "blue", 12)                  # the charge pump's 10 hp motor
TS.cylinder("z", (0.9, 0.55), 0.2, 1.3, 1.8, "padgreen", 12)              # the pump
TS.pipe([(0.9, 0.55, 0.0), (0.9, 0.55, 0.2)], 3, "steel")                 # crude in, the north end
for x in (0.3, 2.7):                                                        # the air cooler's frame
	for z in (1.9, 3.4):
		TS.box((x - 0.06, 0.2, z - 0.06), (x + 0.06, 2.4, z + 0.06), "galv")
TS.box((0.25, 2.0, 1.85), (2.75, 2.4, 3.45), "galv")                      # its finned bundle
TS.fins((0.3, 2.05, 1.9), (2.7, 2.35, 3.4), "x", 16, 0.02, "steel")
TS.ring("y", (1.5, 2.6), 0.62, 2.4, 2.7, 0.03, "grey", sides=16)           # the fan ring
TS.cylinder("z", (2.2, 0.5), 0.3, 1.0, 3.0, "padgreen", 12)               # the reflux drum
TS.pipe([(2.2, 0.5, 3.0), (2.2, 0.5, 4.0)], 2, "steel")                   # gasoline out, the south end
TS.collide((0.1, 0.0, 0.05), (2.9, 2.4, 3.95))

# ---- the crude unit -----------------------------------------------------------------------------------------------------------------
AC = model("atmospheric_column", 3, 30, 3, "padgreen")
AC.box((0.0, 0.0, 0.0), (3.0, 0.4, 3.0), "concrete")
AC.cylinder("y", (1.5, 1.5), 1.45, 0.4, 2.0, "iron", 20)                   # the skirt
AC.cylinder("y", (1.5, 1.5), 1.4, 2.0, 29.4, "padgreen", 20)               # the column
AC.cylinder("y", (1.5, 1.5), 1.0, 29.4, 29.8, "padgreen", 16)
for y in range(3, 29, 3):
	AC.ring("y", (1.5, 1.5), 1.43, y, y + 0.06, 0.03, "iron", sides=20)   # the tray-section seams
for y in (6.0, 12.0, 18.0, 24.0, 28.6):
	AC.box((0.0, y, -0.05), (3.0, y + 0.06, 0.1), "checker")              # platforms on the ladder side
	AC.box((0.0, y + 1.0, -0.06), (3.0, y + 1.05, -0.03), "galv")
AC.ladder(0.5, -0.05, 0.4, 29.0, -1, "galv")
AC.pipe([(1.5, 0.5, 2.9), (1.5, 0.5, 3.0)], 6, "steel")                  # residue at the foot, south
AC.pipe([(1.5, 29.8, 1.5), (1.5, 29.95, 1.5)], 12, "steel")               # the overhead vapour line
AC.collide((0.0, 0.0, 0.0), (3.0, 2.0, 3.0))
AC.collide((0.1, 2.0, 0.1), (2.9, 29.8, 2.9))

CH = model("crude_heater", 6, 14, 8, "grey")
for x in (0.3, 5.5):
	for z in (0.3, 3.8, 7.5):
		CH.box((x, 0.0, z), (x + 0.2, 2.0, z + 0.2), "iron")               # the legs
CH.box((0.2, 1.8, 0.2), (5.8, 2.0, 7.8), "iron")                          # the floor and its burners below
for x in (1.0, 2.5, 3.5, 5.0):
	for z in (1.5, 4.0, 6.5):
		CH.cylinder("y", (x, z), 0.15, 1.4, 1.8, "dark", 8)
CH.box((0.2, 2.0, 0.2), (5.8, 8.0, 7.8), "grey")                          # the radiant box
for x in (0.2, 2.0, 4.0, 5.8):
	CH.box((x - 0.05, 2.0, 0.15), (x + 0.05, 8.0, 0.2), "iron")
CH.box((1.0, 8.0, 1.5), (5.0, 10.5, 6.5), "grey")                         # the convection section
CH.box((2.0, 10.5, 3.0), (4.0, 11.0, 5.0), "iron")                        # the breeching
CH.cylinder("y", (3.0, 4.0), 0.75, 11.0, 13.95, "dark", 16)               # the stack
CH.pipe([(1.0, 0.5, 0.0), (1.0, 0.5, 0.3), (1.0, 1.6, 0.3)], 3, "steel")  # fuel in, the front
CH.box((0.6, 0.3, 0.05), (1.4, 1.1, 0.3), "dark")                         # the fuel train
CH.collide((0.2, 1.8, 0.2), (5.8, 8.0, 7.8))
CH.collide((1.0, 8.0, 1.5), (5.0, 11.0, 6.5))

DS = model("desalter", 3, 4, 10, "padgreen")
for z in (1.5, 8.0):
	DS.box((0.4, 0.0, z), (2.6, 0.15, z + 0.6), "concrete")
	DS.box((0.5, 0.15, z + 0.1), (2.5, 0.5, z + 0.5), "iron")
DS.cylinder("z", (1.5, 1.55), 1.45, 0.6, 9.4, "padgreen", 20)               # the vessel, 3.6 m drawn 2.9 m inside its cells
DS.cylinder("z", (1.5, 1.55), 1.1, 0.35, 0.6, "padgreen", 16)
DS.cylinder("z", (1.5, 1.55), 1.1, 9.4, 9.65, "padgreen", 16)
DS.box((0.9, 3.0, 4.2), (2.1, 3.6, 5.8), "grey")                        # the grid transformer on top
DS.decal((0.89, 3.1, 4.4), (0.9, 3.5, 5.6), "danger", "west", "grey")
DS.box((0.05, 0.3, 0.2), (0.4, 0.9, 0.7), "grey")                         # its 480 V box on the main cell's west face
DS.pipe([(1.0, 0.5, 0.0), (1.0, 0.5, 0.4), (1.0, 1.0, 0.4)], 6, "steel")  # crude in, the north end
DS.box((0.85, 0.35, 0.05), (1.15, 0.65, 0.3), "dark")                     # the mixing valve
DS.collide((0.05, 0.0, 0.35), (2.95, 3.0, 9.65))

CP = model("crude_charge_pump", 2, 2, 5, "blue")
CP.box((0.1, 0.0, 0.1), (1.9, 0.25, 4.9), "iron")                         # the baseplate
CP.cylinder("z", (1.0, 0.7), 0.42, 0.2, 1.85, "blue", 16)                 # the 250 hp motor
CP.fins((0.62, 0.32, 0.3), (1.38, 1.08, 1.75), "z", 8, 0.02, "blue")
CP.box((0.05, 0.4, 0.3), (0.45, 1.0, 0.8), "grey")                        # its 4.16 kV box, west
CP.decal((0.04, 0.5, 0.35), (0.05, 0.9, 0.75), "danger", "west", "grey")
CP.ring("z", (1.0, 0.7), 0.22, 1.9, 2.3, 0.02, "engine", sides=12, skip=(1, 2, 4, 5, 7, 8, 10, 11))   # the coupling guard
CP.cylinder("z", (1.0, 0.7), 0.35, 2.4, 4.4, "padgreen", 14)              # the between-bearings pump
CP.pipe([(1.0, 1.05, 3.4), (1.0, 1.95, 3.4)], 6, "steel")
CP.collide((0.1, 0.0, 0.1), (1.9, 1.2, 4.9))

SS = model("side_strippers", 2, 10, 1, "padgreen")
SS.box((0.0, 0.0, 0.0), (2.0, 0.25, 1.0), "concrete")
for x in (0.5, 1.5):
	SS.cylinder("y", (x, 0.5), 0.45, 0.25, 9.6, "padgreen", 16)
	SS.cylinder("y", (x, 0.5), 0.3, 9.6, 9.75, "padgreen", 12)
SS.pipe([(0.05, 0.5, 0.5), (0.0, 0.5, 0.5)], 3, "steel")                  # kerosene, west
SS.pipe([(1.95, 0.5, 0.5), (2.0, 0.5, 0.5)], 3, "steel")                  # diesel, east
SS.collide((0.0, 0.0, 0.0), (2.0, 9.75, 1.0))

OC = model("overhead_condenser", 4, 4, 8, "grey")
for x in (0.2, 3.7):
	for z in (0.2, 3.9, 7.7):
		OC.box((x, 0.0, z), (x + 0.15, 3.0, z + 0.15), "galv")               # the stand
OC.box((0.1, 3.0, 0.1), (3.9, 3.35, 7.9), "galv")                         # the bundles
OC.fins((0.15, 3.02, 0.15), (3.85, 3.33, 7.85), "x", 30, 0.02, "steel")
for x in (1.0, 3.0):
	for z in (2.0, 6.0):
		OC.ring("y", (x, z), 1.0, 3.35, 3.75, 0.04, "grey", sides=16)     # the fan rings
OC.cylinder("z", (2.0, 1.0), 0.5, 0.8, 7.2, "padgreen", 16)               # the reflux drum under it
OC.box((0.05, 0.3, 0.2), (0.4, 0.9, 0.7), "grey")                         # its 480 V box, west
OC.pipe([(1.5, 0.5, 7.2), (1.5, 0.5, 8.0)], 4, "steel")                   # gasoline out, back, bottom
OC.pipe([(2.5, 1.5, 7.2), (2.5, 1.5, 8.0)], 3, "steel")                   # LPG out, back, above
OC.collide((0.1, 0.0, 0.1), (3.9, 3.35, 7.9))

# ---- turning parts ----------------------------------------------------------------------------------------------------------------
def rotor(name):
	return mlib.Model("rotor_" + name, [(0, 0, 0)], dict(TEX), TEX["iron"])


SF = rotor("skid_fan")
SF.fan("y", (0.5, 0.5), 0.58, 0.5, 0.04, "dark", blades=6, width=0.18, hub=0.1, hub_tex="iron")
FF = rotor("fin_fan")
FF.fan("y", (0.5, 0.5), 0.95, 0.5, 0.04, "dark", blades=6, width=0.25, hub=0.15, hub_tex="iron")

M = TC
for part in (SF, FF):
	M.rotors[part.name[len("rotor_"):]] = part
EXTRA = [TH, TS, AC, CH, DS, CP, SS, OC]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
