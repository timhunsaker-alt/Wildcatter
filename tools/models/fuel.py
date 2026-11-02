"""The fuel plant at true size (docs/multiblocks/fuel.md), each part drawn facing north from its own main cell (the footprints
match FuelPartBlock's and FuelAssemblyBlock's):
- uf6_autoclave, 3 x 3 x 4: the horizontal autoclave round a 30B, its door and heaters (480 V box west);
- idr_kiln, 3 x 4 x 10: the reaction vessel and the rotary Inconel kiln on its rollers, hydrogen in on the east;
- granulator, 3 x 4 x 4: the pre-compaction press, granulator and blender;
- pellet_press, 3 x 3 x 4: the rotary press in its glovebox;
- sintering_furnace, 3 x 3 x 12: the pusher furnace, its pusher and boats, hydrogen in on the east;
- pellet_grinder, 3 x 3 x 4: the centreless grinder and its coolant tank;
- rod_loader, 3 x 3 x 10: the loading bench with a tube on it;
- rod_welder, 3 x 3 x 6: the pressurized weld chamber and the rod rack;
- assembly_fixture, 3 x 6 x 10: the horizontal fixture with an assembly's skeleton in it;
- helium_recovery_unit, 3 x 6 x 4: the cold box, the PSA vessels and the cylinder rack;
- fuel_assembly, 1 x 5 x 1: the 17x17 assembly standing, 214 mm square, 4.06 m tall."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


# ---- the UF6 autoclave ---------------------------------------------------------------------------------------------------------
AC = model("uf6_autoclave", 3, 3, 4, "steel")
AC.box((0.0, 0.0, 0.0), (3.0, 0.2, 4.0), "concrete")
AC.cylinder("z", (1.5, 1.2), 0.95, 0.3, 3.8, "steel", 16)                   # the autoclave shell
AC.cylinder("z", (1.5, 1.2), 1.0, 0.1, 0.3, "iron", 16)                     # its door
AC.box((2.4, 0.2, 3.2), (2.9, 1.0, 3.8), "grey")                            # the heater panel
box480(AC, 0.5, 2.0)
AC.collide((0.5, 0.0, 0.1), (2.5, 2.2, 3.8))

# ---- the IDR kiln ------------------------------------------------------------------------------------------------------------------
IK = model("idr_kiln", 3, 4, 10, "iron")
IK.cylinder("y", (1.5, 1.0), 0.6, 0.0, 3.6, "steel", 14)                    # the reaction vessel
for z in (3.0, 6.0, 9.0):
	IK.box((0.5, 0.0, z - 0.3), (2.5, 0.8, z + 0.3), "concrete")            # piers and rollers
IK.cylinder("z", (1.5, 1.5), 0.7, 2.0, 9.8, "hipaint", 16)                  # the rotary kiln in its heating shell
for z in (3.0, 6.0, 9.0):
	IK.ring("z", (1.5, 1.5), 0.75, z - 0.12, z + 0.12, 0.06, "iron", sides=16)
IK.pipe([(3.0, 0.4, 1.0), (2.1, 0.4, 1.0)], 2, "steel")                     # hydrogen in, the east
box480(IK, 0.4, 4.0)
IK.collide((0.5, 0.0, 0.3), (2.5, 3.6, 9.8))

# ---- the granulator --------------------------------------------------------------------------------------------------------------
GR = model("granulator", 3, 4, 4, "grey")
GR.box((0.0, 0.0, 0.0), (3.0, 0.2, 4.0), "concrete")
GR.box((0.4, 0.2, 0.4), (2.6, 2.2, 1.8), "blue")                           # the pre-compaction press
GR.cylinder("y", (1.5, 2.9), 0.6, 0.2, 2.6, "steel", 12)                   # the granulator and blender
GR.box((1.1, 2.6, 2.5), (1.9, 3.7, 3.3), "galv")                           # its hopper
box480(GR, 0.5, 0.5)
GR.collide((0.4, 0.0, 0.4), (2.6, 3.7, 3.6))

# ---- the pellet press ---------------------------------------------------------------------------------------------------------------
PP = model("pellet_press", 3, 3, 4, "grey")
PP.box((0.0, 0.0, 0.0), (3.0, 0.2, 4.0), "concrete")
PP.box((0.3, 0.2, 0.3), (2.7, 2.6, 3.7), "polymer")                        # the glovebox
PP.cylinder("y", (1.5, 2.0), 0.7, 0.4, 1.4, "blue", 14)                     # the rotary turret
PP.cylinder("y", (1.5, 2.0), 0.15, 1.4, 2.4, "steel", 8)
box480(PP, 0.5, 0.5)
PP.collide((0.3, 0.0, 0.3), (2.7, 2.6, 3.7))

# ---- the sintering furnace ----------------------------------------------------------------------------------------------------------
SF = model("sintering_furnace", 3, 3, 12, "hipaint")
SF.box((0.0, 0.0, 0.0), (3.0, 0.2, 12.0), "concrete")
SF.box((0.5, 0.2, 1.5), (2.5, 2.2, 11.0), "hipaint")                        # the furnace muffle and its cooling zone
SF.box((0.9, 0.2, 0.2), (2.1, 1.2, 1.5), "blue")                            # the pusher
for z in (11.2, 11.6):
	SF.box((1.0, 0.6, z), (2.0, 0.8, z + 0.3), "steel")                     # molybdenum boats out
SF.pipe([(3.0, 0.4, 6.0), (2.5, 0.4, 6.0)], 2, "steel")                     # hydrogen in, the east
box480(SF, 0.5, 2.0)
SF.collide((0.5, 0.0, 0.2), (2.5, 2.2, 11.9))

# ---- the pellet grinder ---------------------------------------------------------------------------------------------------------------
PG = model("pellet_grinder", 3, 3, 4, "grey")
PG.box((0.0, 0.0, 0.0), (3.0, 0.2, 4.0), "concrete")
PG.box((0.5, 0.2, 0.5), (2.5, 1.6, 3.5), "blue")                            # the grinder bed
PG.cylinder("x", (1.9, 1.5), 0.35, 0.6, 2.4, "steel", 12)                   # its wheel
PG.box((0.6, 0.2, 3.5), (1.6, 0.9, 3.9), "steel")                           # the coolant tank
box480(PG, 0.4, 0.3)
PG.collide((0.5, 0.0, 0.5), (2.5, 2.2, 3.9))

# ---- the rod loader ---------------------------------------------------------------------------------------------------------------------
RL = model("rod_loader", 3, 3, 10, "grey")
RL.box((0.3, 0.0, 0.2), (2.7, 0.9, 9.8), "iron")                            # the bench
RL.cylinder("z", (1.5, 1.0), 0.05, 0.4, 9.6, "steel", 6)                     # a tube on it
RL.box((0.5, 0.9, 0.3), (2.5, 2.0, 1.6), "polymer")                          # the pellet loading glovebox
RL.box((2.1, 0.9, 1.8), (2.6, 1.6, 2.6), "galv")                             # the tube magazine
box480(RL, 0.3, 2.8)
RL.collide((0.3, 0.0, 0.2), (2.7, 2.0, 9.8))

# ---- the rod welder -------------------------------------------------------------------------------------------------------------------
RW = model("rod_welder", 3, 3, 6, "grey")
RW.box((0.0, 0.0, 0.0), (3.0, 0.2, 6.0), "concrete")
RW.cylinder("z", (1.0, 1.2), 0.55, 0.3, 2.4, "steel", 14)                   # the pressurized weld chamber
RW.box((1.8, 0.2, 0.3), (2.7, 2.4, 5.7), "iron")                            # the rod rack
for y in (0.6, 1.0, 1.4, 1.8):
	RW.box((1.85, y, 0.4), (2.65, y + 0.05, 5.6), "steel")                  # racked rods
RW.cylinder("y", (0.6, 4.8), 0.12, 0.2, 1.7, "padgreen", 8)                 # the helium cylinder on its manifold
box480(RW, 0.5, 2.8)
RW.collide((0.4, 0.0, 0.3), (2.7, 2.4, 5.7))

# ---- the assembly fixture ---------------------------------------------------------------------------------------------------------------
AF = model("assembly_fixture", 3, 6, 10, "blue")
AF.box((0.2, 0.0, 0.2), (2.8, 0.8, 9.8), "iron")                            # the fixture bed
AF.box((1.39, 0.8, 0.5), (1.61, 1.02, 4.56), "steel")                       # the skeleton, lying
for i in range(8):
	z = 0.6 + i * 0.55
	AF.box((1.36, 0.78, z), (1.64, 1.05, z + 0.05), "galv")                 # its grids
AF.box((0.2, 0.8, 5.0), (2.8, 5.8, 5.4), "blue")                            # the rod-pulling carriage's gantry
AF.box((0.2, 0.8, 9.0), (2.8, 3.0, 9.8), "blue")                            # its drive
box480(AF, 0.9, 0.3)
AF.collide((0.2, 0.0, 0.2), (2.8, 1.1, 9.8))

# ---- the helium recovery unit -------------------------------------------------------------------------------------------------------------
HE = model("helium_recovery_unit", 3, 6, 4, "galv")
HE.box((0.0, 0.0, 0.0), (3.0, 0.2, 4.0), "concrete")
HE.box((0.3, 0.2, 0.3), (1.5, 5.8, 1.5), "galv")                            # the cold box
for z in (2.2, 3.2):
	HE.cylinder("y", (0.9, z), 0.35, 0.2, 2.6, "steel", 10)                 # the PSA vessels
for z in (1.9, 2.5, 3.1):
	HE.cylinder("y", (2.3, z), 0.12, 0.2, 1.7, "padgreen", 8)               # cylinders on the filling rack
HE.pipe([(1.0, 0.5, 0.0), (1.0, 0.5, 0.3)], 2, "steel")                     # pipeline gas in and out, the north
box480(HE, 0.5, 1.7)
HE.collide((0.3, 0.0, 0.3), (2.7, 5.8, 3.7))

# ---- the 17x17 fuel assembly ---------------------------------------------------------------------------------------------------------------
FA = model("fuel_assembly", 1, 5, 1, "steel")
c0, c1 = 0.5 - 0.107, 0.5 + 0.107                                           # 214 mm square
FA.box((c0, 0.0, c0), (c1, 0.07, c1), "steel")                              # the bottom nozzle
FA.box((c0 + 0.004, 0.07, c0 + 0.004), (c1 - 0.004, 3.94, c1 - 0.004), "galv")   # the rod bundle
for i in range(8):
	y = 0.15 + i * 0.52
	FA.box((c0 - 0.002, y, c0 - 0.002), (c1 + 0.002, y + 0.04, c1 + 0.002), "iron")   # the grids
FA.box((c0, 3.94, c0), (c1, 4.06, c1), "steel")                              # the top nozzle
for x, z in ((c0 + 0.02, c0 + 0.02), (c1 - 0.06, c1 - 0.06)):
	FA.box((x, 4.06, z), (x + 0.04, 4.1, z + 0.04), "steel")                 # its hold-down springs
FA.collide((c0, 0.0, c0), (c1, 4.1, c1))


M = AC
EXTRA = [IK, GR, PP, SF, PG, RL, RW, AF, HE, FA]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
