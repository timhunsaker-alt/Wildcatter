"""The nickel smelter and refinery at true size (docs/multiblocks/nickel-smelter.md), each part drawn facing north from its own main cell
(the footprints match NickelPartBlock's):
- fluid_bed_roaster, 4 x 10 x 4: the refractory-lined bed vessel and its freeboard, two cyclones on top, the feed hopper, the
  fluidizing blower at its foot (box west, MV), the concentrate inlet north and the off-gas duct down to its south side;
- nickel_electric_furnace, 6 x 6 x 12: the rectangular shell with its six Soderberg electrodes in line, their slipping gear and
  bus tubes, the matte tapholes east, the slag launder at its far end and the paste hopper;
- peirce_smith_converter, 5 x 5 x 10: the horizontal drum on its riding rings and rollers, the tuyere line and its air header, the
  mouth and hood, the drive at its north end and the blower (box west, MV), the off-gas duct south;
- chlorine_leach_reactor, 3 x 8 x 3: the tall titanium-lined autoclave, its agitator drive, the chlorine sparger line east (box west);
- ni_purification, 4 x 4 x 10: the cementation tank, a row of mixer-settlers and the iron-removal tank (box west);
- nickel_tankhouse, 6 x 4 x 14: the cell line under its anode hoods, the chlorine header, the bus bars to the south end."""
import math
import mlib
import plantlib as g

TEX = dict(g.TEX)
TEX["brick"] = "block/mat/mat_concrete"


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def disc(M, centre, r, y0, y1, tex, step=0.32):
	"""A round body too big for turned slabs: strips along z, stepped to the circle."""
	cx, cz = centre
	n = int(math.ceil(2 * r / step))
	for i in range(n):
		x0 = cx - r + i * 2 * r / n
		x1 = x0 + 2 * r / n
		xm = max(abs(x0 - cx), abs(x1 - cx)) if (x0 - cx) * (x1 - cx) > 0 else 0.0
		h = math.sqrt(max(0.0, r * r - xm * xm))
		M.box((x0, y0, cz - h), (x1, y1, cz + h), tex)


def drum_x(M, centre_yz, r, x0, x1, tex, step=0.3):
	"""A horizontal drum along z (the converter's): strips along z, stepped to the circle in x-y."""
	cy, cx = centre_yz
	n = int(math.ceil(2 * r / step))
	for i in range(n):
		a0 = cx - r + i * 2 * r / n
		a1 = a0 + 2 * r / n
		am = max(abs(a0 - cx), abs(a1 - cx)) if (a0 - cx) * (a1 - cx) > 0 else 0.0
		h = math.sqrt(max(0.0, r * r - am * am))
		M.box((a0, cy - h, x0), (a1, cy + h, x1), tex)


def box480(M, y=0.4, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


def box4160(M, y=0.3, z=0.15):
	M.box((0.0, y, z), (0.4, y + 0.8, z + 0.7), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.6), "arcflash", "west", "grey")


# ---- the fluid-bed roaster ------------------------------------------------------------------------------------------------------
FB = model("fluid_bed_roaster", 4, 10, 4, "iron")
FB.box((0.0, 0.0, 0.0), (4.0, 0.3, 4.0), "concrete")
FB.box((0.5, 0.3, 0.4), (3.5, 1.4, 1.2), "blue")                           # the fluidizing blower and its motor, north
FB.cylinder("x", (0.85, 0.8), 0.35, 1.2, 2.8, "engine", 12)
disc(FB, (2.0, 2.3), 1.5, 1.4, 5.6, "iron")                                # the bed vessel, refractory-lined
for y in (2.4, 3.6, 4.8):
	disc(FB, (2.0, 2.3), 1.56, y, y + 0.1, "dark")                         # its stiffening bands
disc(FB, (2.0, 2.3), 1.7, 5.6, 7.4, "iron")                                # the freeboard, wider
disc(FB, (2.0, 2.3), 1.0, 7.4, 7.8, "dark")                                # its roof
for cx in (1.1, 2.9):
	FB.cylinder("y", (cx, 2.3), 0.38, 7.8, 9.2, "steel", 12)              # the two cyclones
	FB.cylinder("y", (cx, 2.3), 0.16, 9.2, 9.6, "steel", 8)
FB.box((1.6, 9.4, 1.0), (2.4, 10.0, 1.8), "galv")                          # the feed hopper and its mill chute
FB.pipe([(2.0, 9.4, 1.4), (2.0, 7.8, 1.4)], 4, "steel")
FB.pipe([(0.5, 0.6, 0.0), (0.5, 0.6, 0.4)], 3, "steel")                   # concentrate in, north
FB.pipe([(1.5, 9.0, 3.6), (1.5, 0.5, 3.6), (1.5, 0.5, 4.0)], 12, "lagging") # off-gas down to the south: burner gas out
FB.ladder(3.7, 1.0, 0.3, 7.4, 1, "galv")
box4160(FB, 0.3, 1.4)
FB.collide((0.0, 0.0, 0.0), (4.0, 7.8, 4.0))

# ---- the nickel electric furnace ---------------------------------------------------------------------------------------------------
EF = model("nickel_electric_furnace", 6, 6, 12, "iron")
EF.box((0.0, 0.0, 0.0), (6.0, 0.3, 12.0), "concrete")
EF.box((0.4, 0.3, 0.4), (5.6, 2.6, 11.6), "iron")                          # the rectangular shell, lined
for z in (1.0, 3.0, 5.0, 7.0, 9.0, 11.0):
	EF.box((0.3, 0.3, z - 0.1), (5.7, 2.7, z + 0.1), "dark")               # its buckstays
EF.box((0.6, 2.6, 0.6), (5.4, 2.9, 11.4), "dark")                          # the roof
for k in range(6):
	z = 1.2 + k * 1.92
	EF.cylinder("y", (3.0, z), 0.45, 2.1, 5.8, "dark", 14)                # a Soderberg electrode in its casing
	EF.cylinder("y", (3.0, z), 0.52, 3.9, 4.4, "blue", 14)                # its slipping and regulating gear
	EF.cylinder("y", (3.0, z), 0.52, 3.0, 3.3, "copper", 14)              # its contact clamps
	EF.box((3.5, 3.1, z - 0.06), (5.9, 3.2, z + 0.06), "copper")          # the bus tubes in from the west transformer side
EF.box((0.0, 3.0, 0.4), (3.0, 3.3, 11.6), "copper")                        # the flexibles from the furnace transformer, west
EF.box((0.5, 4.4, 0.5), (5.5, 4.6, 11.5), "grating")                       # the electrode floor
EF.handrail([(0.5, 0.5), (5.5, 0.5), (5.5, 11.5), (0.5, 11.5), (0.5, 0.5)], 4.6, "galv")
for z in (4.0, 8.0):
	EF.box((5.6, 0.6, z - 0.3), (6.0, 1.0, z + 0.3), "hipaint")           # the matte tapholes, east
EF.box((2.0, 0.6, 11.6), (4.0, 1.0, 12.0), "hipaint")                      # the slag launder, south end
EF.box((4.6, 4.6, 5.4), (5.6, 5.6, 6.6), "galv")                           # the paste hopper
EF.ladder(0.3, 0.2, 0.3, 4.6, -1, "galv")
EF.collide((0.3, 0.0, 0.3), (5.7, 4.6, 11.7))

# ---- the Peirce-Smith converter -----------------------------------------------------------------------------------------------------
PS = model("peirce_smith_converter", 5, 5, 10, "iron")
PS.box((0.0, 0.0, 0.0), (5.0, 0.3, 10.0), "concrete")
for z in (2.0, 8.0):
	PS.box((0.9, 0.3, z - 0.4), (4.1, 0.9, z + 0.4), "concrete")          # the roller piers
	PS.cylinder("x", (0.95, z), 0.25, 1.4, 1.8, "steel", 10)              # the riding rollers
	PS.cylinder("x", (0.95, z), 0.25, 3.2, 3.6, "steel", 10)
drum_x(PS, (2.6, 2.5), 1.7, 1.0, 9.0, "iron")                              # the drum, refractory-lined
for z in (2.0, 8.0):
	drum_x(PS, (2.6, 2.5), 1.8, z - 0.25, z + 0.25, "dark")                # its riding rings
PS.box((0.6, 1.6, 1.2), (0.8, 1.8, 8.8), "steel")                          # the tuyere line, west
PS.pipe([(0.4, 2.4, 1.2), (0.4, 2.4, 8.8)], 8, "steel")                    # its air header
PS.box((1.8, 4.3, 4.2), (3.2, 4.4, 5.8), "dark")                           # the mouth
PS.box((1.6, 4.4, 4.0), (3.4, 5.0, 6.0), "galv")                           # the hood
PS.box((0.6, 0.3, 0.1), (4.4, 2.2, 1.0), "blue")                           # the tilting drive, north
PS.box((0.5, 0.3, 9.2), (1.8, 1.4, 10.0), "blue")                          # the air blower
PS.pipe([(2.5, 4.7, 6.0), (2.5, 4.7, 9.5), (2.5, 0.5, 9.5), (2.5, 0.5, 10.0)], 12, "lagging")   # off-gas to the south: burner gas out
box4160(PS, 0.3, 0.15)
PS.collide((0.0, 0.0, 0.0), (5.0, 4.4, 10.0))

# ---- the chlorine leach reactor -----------------------------------------------------------------------------------------------------
LR = model("chlorine_leach_reactor", 3, 8, 3, "steel")
LR.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
for x, z in ((0.4, 0.4), (2.6, 0.4), (0.4, 2.6), (2.6, 2.6)):
	LR.box((x - 0.08, 0.3, z - 0.08), (x + 0.08, 1.2, z + 0.08), "galv")   # its legs
LR.cylinder("y", (1.5, 1.5), 1.2, 1.0, 6.4, "steel", 18)                    # the autoclave, titanium-lined
LR.cylinder("y", (1.5, 1.5), 0.9, 6.4, 6.8, "steel", 14)                    # its head
LR.box((1.2, 6.8, 1.2), (1.8, 7.2, 1.8), "engine")                          # the agitator drive
LR.cylinder("y", (1.5, 1.5), 0.18, 7.2, 7.9, "blue", 10)
LR.pipe([(1.5, 0.5, 0.0), (1.5, 0.5, 0.3), (1.5, 1.5, 0.3)], 3, "steel")    # chlorine in, north
LR.box((0.6, 7.2, 0.5), (1.0, 7.6, 0.9), "galv")                            # the matte feed hopper
LR.ladder(0.3, 2.85, 0.3, 6.8, 1, "galv")
box480(LR, 0.4, 0.3)
LR.collide((0.2, 0.0, 0.2), (2.8, 6.8, 2.8))

# ---- the purification circuit ---------------------------------------------------------------------------------------------------------
PU = model("ni_purification", 4, 4, 10, "grey")
PU.box((0.0, 0.0, 0.0), (4.0, 0.2, 10.0), "concrete")
disc(PU, (2.0, 1.4), 1.1, 0.2, 3.2, "steel")                    # the copper cementation tank
PU.box((1.7, 3.2, 1.1), (2.3, 3.6, 1.7), "engine")
for k in range(4):
	z = 3.0 + k * 1.3
	PU.box((0.6, 0.2, z), (3.4, 1.3, z + 1.1), "polymer")                  # a mixer-settler of the cobalt SX
	PU.box((0.8, 1.3, z + 0.1), (1.4, 1.7, z + 0.5), "engine")             # its mixer drive
disc(PU, (2.0, 8.7), 0.9, 0.2, 2.8, "steel")                     # the iron-removal tank
PU.pipe([(4.0, 0.5, 4.0), (3.6, 0.5, 4.0)], 3, "steel")                      # kerosene in, east
PU.box((3.0, 0.2, 0.2), (3.8, 1.0, 0.8), "galv")                             # the copper casting box, its ingots below
PU.box((0.2, 0.3, 0.0), (0.7, 0.9, 0.3), "grey")                           # its box, north (its west against the leach)
PU.decal((0.3, 0.4, -0.01), (0.6, 0.8, 0.0), "danger", "north", "grey")
PU.collide((0.0, 0.0, 0.0), (4.0, 3.2, 10.0))

# ---- the nickel tankhouse -------------------------------------------------------------------------------------------------------------
NT = model("nickel_tankhouse", 6, 4, 14, "polymer")
NT.box((0.0, 0.0, 0.0), (6.0, 0.3, 14.0), "concrete")
for k in range(18):
	row, i = divmod(k, 9)
	x0 = 0.6 + row * 2.6
	z = 0.6 + i * 1.4
	NT.box((x0, 0.3, z), (x0 + 2.2, 1.4, z + 1.2), "polymer")             # an electrowinning cell
	NT.box((x0 + 0.2, 1.4, z + 0.2), (x0 + 2.0, 1.9, z + 1.0), "steel")   # its titanium anode hood
	NT.cylinder("y", (x0 + 1.1, z + 0.6), 0.07, 1.9, 2.4, "steel", 6)     # its chlorine off-take
NT.pipe([(1.7, 2.6, 0.6), (1.7, 2.6, 13.2)], 8, "steel")                    # the chlorine headers
NT.pipe([(4.3, 2.6, 0.6), (4.3, 2.6, 13.2)], 8, "steel")
NT.pipe([(1.7, 2.6, 0.4), (4.3, 2.6, 0.4)], 8, "steel")
NT.box((2.85, 1.4, 0.6), (3.15, 1.6, 13.2), "copper")                       # the intercell bus bar
NT.box((1.0, 2.0, 13.4), (1.6, 2.6, 14.0), "copper")                        # the + terminal, south end
NT.box((1.0, 0.3, 13.4), (1.6, 0.9, 14.0), "copper")                        # the - terminal
NT.collide((0.0, 0.0, 0.0), (6.0, 2.6, 14.0))

# ---- the induction furnace ------------------------------------------------------------------------------------------------------------
IF = model("induction_furnace", 3, 3, 3, "blue")
IF.box((0.0, 0.0, 0.0), (3.0, 0.4, 3.0), "concrete")
IF.box((0.4, 0.4, 0.4), (2.6, 1.6, 2.6), "blue")                             # the furnace body: the yoke frame round the coil
for y in (0.6, 0.9, 1.2):
	IF.ring("y", (1.5, 1.5), 1.0, y, y + 0.12, 0.08, "copper", sides=16)     # the water-cooled copper coil turns, seen at its gaps
IF.cylinder("y", (1.5, 1.5), 0.75, 1.6, 1.8, "dark", 16)                     # the crucible's lip, refractory
IF.box((1.3, 1.6, 2.6), (1.7, 1.75, 3.0), "hipaint")                         # the pouring spout, south: to the AOD
IF.box((0.2, 1.8, 0.2), (1.0, 2.9, 1.0), "galv")                             # the charge bucket and its hopper, over the crucible
IF.box((0.0, 0.4, 0.0), (0.4, 1.6, 0.4), "grey")                             # the power cabinet's bus to the coil, west
IF.box((0.0, 0.3, 0.6), (0.4, 1.1, 1.3), "grey")                             # its 4.16 kV box
IF.decal((-0.01, 0.4, 0.7), (0.0, 0.9, 1.2), "arcflash", "west", "grey")
IF.collide((0.0, 0.0, 0.0), (3.0, 1.8, 3.0))

# ---- the AOD converter --------------------------------------------------------------------------------------------------------------------
AD = model("aod_converter", 3, 6, 3, "iron")
AD.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
for x in (0.2, 2.5):
	AD.box((x, 0.3, 1.2), (x + 0.3, 2.4, 1.8), "blue")                       # the trunnion stands
AD.cylinder("x", (2.2, 1.5), 0.25, 0.2, 2.8, "steel", 10)                    # the trunnion ring's pins
AD.cylinder("y", (1.5, 1.5), 1.05, 0.6, 3.6, "iron", 16)                     # the vessel, magnesia-chrome lined
AD.cylinder("y", (1.5, 1.5), 0.7, 3.6, 4.2, "iron", 14)                      # its cone
AD.cylinder("y", (1.5, 1.5), 0.45, 4.2, 4.3, "dark", 12)                     # the mouth
for z in (1.0, 1.5, 2.0):
	AD.cylinder("x", (0.9, z), 0.06, 2.5, 2.9, "copper", 6)                  # the side tuyeres, east
AD.pipe([(2.9, 0.9, 1.0), (2.9, 0.9, 2.0)], 3, "steel")                      # their gas manifold
AD.pipe([(3.0, 0.5, 1.5), (2.9, 0.5, 1.5), (2.9, 0.9, 1.5)], 3, "steel")      # nitrogen in, east
AD.box((0.5, 4.8, 0.5), (2.5, 5.4, 2.5), "galv")                             # the fume hood
AD.cylinder("y", (2.2, 0.8), 0.3, 5.4, 6.0, "galv", 10)                      # its duct
box480(AD, 0.4, 0.3)
AD.collide((0.0, 0.0, 0.0), (3.0, 4.3, 3.0))

# ---- the ingot caster ---------------------------------------------------------------------------------------------------------------------
IC = model("ingot_caster", 2, 3, 6, "iron")
IC.box((0.0, 0.0, 0.0), (2.0, 0.3, 6.0), "concrete")
for x in (0.3, 1.7):
	IC.box((x - 0.05, 0.3, 0.2), (x + 0.05, 0.4, 5.8), "steel")              # the car's rails
IC.box((0.2, 0.4, 0.4), (1.8, 0.6, 5.6), "iron")                             # the mould car
for k in range(10):
	x = 0.55 if k % 2 == 0 else 1.45
	z = 0.8 + (k // 2) * 1.0
	IC.box((x - 0.25, 0.6, z - 0.25), (x + 0.25, 1.5, z + 0.25), "dark")    # a cast-iron ingot mould
IC.cylinder("y", (1.0, 3.0), 0.12, 0.6, 1.9, "hipaint", 8)                   # the trumpet, bottom-pouring through the runners
IC.box((0.7, 1.9, 2.7), (1.3, 2.1, 3.3), "hipaint")
IC.box((0.0, 2.2, 0.2), (2.0, 2.4, 0.6), "steel")                            # the ladle's stand, north
IC.collide((0.0, 0.0, 0.0), (2.0, 1.5, 6.0))


M = FB
EXTRA = [EF, PS, LR, PU, NT, IF, AD, IC]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
