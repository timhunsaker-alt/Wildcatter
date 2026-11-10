"""The ferroalloy plant at true size (docs/multiblocks/ferroalloys.md), each part drawn facing north from its own main cell (the
footprints match FerroalloyPartBlock's):
- furnace_transformer, 4 x 5 x 5: the 10 MVA tank and its oil-water coolers, the conservator, the on-load tap changer, its
  distribution box on the west face of the main cell, the secondary's copper bus tubes out of its east side to the furnace;
- submerged_arc_furnace, 7 x 8 x 7: the round shell on its foundation, the taphole and launder on its east face, three Soderberg
  electrode columns with their slipping and regulating gear, the fume hood, the charging chutes and the two hoppers on top;
- capacitor_bank, 2 x 3 x 4: the fenced rack of capacitor cans in three tiers, its inrush reactors and its breaker (box west);
- moly_roaster, 3 x 9 x 3: the multiple-hearth roaster's shell and its hearth doors, the rabble drive under it, the drum filter at its
  foot (concentrate in on the east), the off-gas duct down to its south side (box west);
- aluminothermic_vessel, 3 x 3 x 3: the refractory-lined vessel in its sand bed, the hood and its duct to the baghouse, the batch
  hopper (box west)."""
import math
import mlib
import plantlib as g

TEX = dict(g.TEX)
TEX["glaze"] = "block/mat/mat_porcelain_brown"
TEX["fence"] = "block/mat/fence_chain_link"


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def disc(M, centre, r, y0, y1, tex, step=0.32):
	"""A round body too big for turned slabs (they may reach only a block past their cell): strips along z, stepped to the circle."""
	cx, cz = centre
	n = int(math.ceil(2 * r / step))
	for i in range(n):
		x0 = cx - r + i * 2 * r / n
		x1 = x0 + 2 * r / n
		xm = max(abs(x0 - cx), abs(x1 - cx)) if (x0 - cx) * (x1 - cx) > 0 else 0.0
		h = math.sqrt(max(0.0, r * r - xm * xm))
		M.box((x0, y0, cz - h), (x1, y1, cz + h), tex)


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


# ---- the furnace transformer --------------------------------------------------------------------------------------------------
FX = model("furnace_transformer", 4, 5, 5, "blue")
FX.box((0.0, 0.0, 0.0), (4.0, 0.3, 5.0), "concrete")
FX.box((0.6, 0.3, 0.8), (3.2, 3.4, 4.2), "blue")                            # the tank
for z in (0.8, 1.9, 3.0):
	FX.fins((0.75, 0.6, z + 0.1), (3.05, 3.1, z + 0.9), "x", 8, 0.03, "radfin")   # its ribs
FX.box((0.6, 0.3, 4.2), (2.2, 1.6, 4.9), "steel")                           # the oil-water coolers
FX.pipe([(1.0, 1.8, 4.2), (1.0, 1.8, 4.6), (1.0, 0.8, 4.6)], 2, "steel")
FX.pipe([(1.8, 1.8, 4.2), (1.8, 1.8, 4.6), (1.8, 0.8, 4.6)], 2, "steel")
FX.cylinder("x", (4.0, 1.2), 0.35, 0.8, 3.0, "blue", 12)                    # the conservator
FX.box((0.6, 1.2, 0.2), (1.6, 2.6, 0.8), "grey")                            # the on-load tap changer's drive
FX.decal((0.7, 1.4, 0.19), (1.5, 2.4, 0.2), "ratingplate", "north", "grey")
for x in (1.2, 1.9, 2.6):
	FX.cylinder("y", (x, 2.6), 0.1, 3.4, 4.0, "glaze", 8)                   # the distribution bushings
FX.box((0.0, 0.0, 0.0), (0.6, 1.2, 1.0), "grey")                            # the distribution box, west
FX.decal((-0.01, 0.2, 0.2), (0.0, 1.0, 0.8), "danger", "west", "grey")
for y in (1.0, 1.8, 2.6):                                                     # the secondary: three pairs of copper bus tubes east
	FX.cylinder("x", (y, 2.0), 0.12, 3.2, 4.0, "copper", 8)
	FX.cylinder("x", (y, 3.0), 0.12, 3.2, 4.0, "copper", 8)
FX.box((3.2, 0.6, 1.6), (3.5, 3.0, 3.4), "grey")                            # the secondary's bushing plate
FX.collide((0.0, 0.0, 0.0), (4.0, 3.4, 5.0))

# ---- the submerged arc furnace ------------------------------------------------------------------------------------------------
SF = model("submerged_arc_furnace", 7, 8, 7, "iron")
C = (3.5, 3.5)
SF.box((0.0, 0.0, 0.0), (7.0, 0.3, 7.0), "concrete")
disc(SF, C, 3.2, 0.3, 2.8, "iron")                                          # the steel shell, lined
for y in (0.8, 1.6, 2.4):
	disc(SF, C, 3.26, y, y + 0.1, "dark")                                   # its cooling bands
disc(SF, C, 3.0, 2.8, 3.0, "dark")                                          # the roof, refractory
SF.box((6.5, 0.3, 3.1), (7.0, 0.9, 3.9), "hipaint")                         # the taphole and its launder, east
SF.box((6.6, 0.85, 3.2), (7.0, 0.9, 3.8), "dark")
SF.box((0.5, 3.4, 0.5), (6.5, 4.9, 6.5), "galv")                            # the fume hood
SF.box((0.6, 4.9, 0.6), (6.4, 5.1, 6.4), "grating")                         # the electrode floor
for k in range(3):
	a = math.radians(90 + 120 * k)
	ex, ez = C[0] + 1.3 * math.cos(a), C[1] + 1.3 * math.sin(a)
	SF.cylinder("y", (ex, ez), 0.55, 2.4, 7.6, "dark", 16)                 # the Soderberg electrode, paste in its casing
	SF.cylinder("y", (ex, ez), 0.62, 5.1, 5.8, "blue", 16)                 # its slipping and regulating gear
	SF.cylinder("y", (ex, ez), 0.62, 3.6, 4.0, "copper", 16)               # its contact clamps
	SF.box((ex - 0.08, 5.1, ez - 0.7), (ex + 0.08, 6.6, ez - 0.62), "steel")   # the hydraulic cylinders
	SF.box((ex - 0.08, 5.1, ez + 0.62), (ex + 0.08, 6.6, ez + 0.7), "steel")
SF.box((0.0, 4.0, 2.8), (0.6, 4.4, 4.2), "copper")                          # the flexible cables in from the transformer, west
for x in (2.0, 5.0):
	SF.box((x - 0.6, 7.0, 2.9), (x + 0.6, 8.0, 4.1), "galv")               # the two charge hoppers on top
	SF.pipe([(x, 7.0, 3.5), (x, 5.1, 3.5)], 3, "steel")                    # their chutes through the hood
SF.cylinder("y", (6.0, 1.0), 0.4, 4.9, 7.8, "galv", 12)                     # the fume duct off the hood
SF.ladder(0.3, 6.6, 0.3, 5.1, -1, "galv")
SF.handrail([(0.6, 0.6), (6.4, 0.6), (6.4, 6.4), (0.6, 6.4), (0.6, 0.6)], 5.1, "galv")
SF.collide((0.3, 0.0, 0.3), (6.7, 5.1, 6.7))

# ---- the capacitor bank ---------------------------------------------------------------------------------------------------------
CB = model("capacitor_bank", 2, 3, 4, "grey")
CB.box((0.0, 0.0, 0.0), (2.0, 0.2, 4.0), "concrete")
for x in (0.3, 1.7):
	for z in (0.8, 3.7):
		CB.box((x - 0.04, 0.2, z - 0.04), (x + 0.04, 2.4, z + 0.04), "galv")   # the rack's posts
for y in (0.6, 1.3, 2.0):
	CB.box((0.3, y - 0.05, 0.8), (1.7, y, 3.7), "galv")                    # its tiers
	for z in (1.0, 1.5, 2.0, 2.5, 3.0, 3.5):
		CB.box((0.5, y, z - 0.15), (1.5, y + 0.45, z + 0.15), "grey")     # the capacitor cans, 18
		CB.cylinder("y", (1.0, z), 0.04, y + 0.45, y + 0.6, "glaze", 6)  # their bushings
for z in (1.2, 2.2, 3.2):
	CB.cylinder("y", (1.0, z), 0.2, 2.45, 2.95, "padgreen", 10)              # the inrush reactors, one a phase
CB.box((0.0, 0.2, 0.0), (0.7, 1.6, 0.6), "grey")                            # its breaker and the terminal box, west
CB.decal((-0.01, 0.4, 0.1), (0.0, 1.0, 0.5), "danger", "west", "grey")
CB.box((0.7, 0.2, 0.05), (2.0, 2.3, 0.1), "fence", uv_full=("north", "south"))   # the fence, north
CB.collide((0.0, 0.0, 0.0), (2.0, 2.4, 4.0))

# ---- the moly roaster -----------------------------------------------------------------------------------------------------------
MR = model("moly_roaster", 3, 9, 3, "steel")
MR.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
MR.box((0.6, 0.3, 0.6), (2.4, 1.3, 2.4), "blue")                            # the rabble drive under the shell
MR.cylinder("y", (1.5, 1.5), 1.3, 1.3, 8.2, "steel", 20)                    # the shell, refractory-lined, eight hearths
for k in range(8):
	y = 1.5 + k * 0.85
	MR.ring("y", (1.5, 1.5), 1.32, y, y + 0.08, 0.05, "dark", sides=20)   # the hearths' floors
	MR.box((1.2, y + 0.25, 0.17), (1.8, y + 0.6, 0.2), "iron")            # their doors, north
MR.cylinder("y", (1.5, 1.5), 0.3, 8.2, 8.9, "steel", 10)                    # the feed at the top hearth
MR.cylinder("z", (2.6, 0.8), 0.32, 0.3, 2.2, "galv", 12)                    # the drum filter, east
MR.pipe([(2.6, 0.5, 1.5), (3.0, 0.5, 1.5)], 2, "steel")                    # concentrate in, the east
MR.pipe([(1.5, 8.0, 2.75), (1.5, 0.5, 2.75), (1.5, 0.5, 2.85)], 12, "lagging") # the off-gas duct down to the south: burner gas out
MR.ladder(2.3, 0.42, 1.3, 8.2, -1, "galv")
box480(MR, 0.4, 0.2)
MR.collide((0.2, 0.0, 0.2), (2.8, 8.2, 2.8))

# ---- the aluminothermic vessel ----------------------------------------------------------------------------------------------------
AV = model("aluminothermic_vessel", 3, 3, 3, "iron")
AV.box((0.0, 0.0, 0.0), (3.0, 0.4, 3.0), "concrete")                        # the sand bed
AV.cylinder("y", (1.5, 1.5), 0.95, 0.4, 1.5, "iron", 16)                    # the vessel's steel shell
AV.ring("y", (1.5, 1.5), 0.97, 1.3, 1.45, 0.05, "dark", sides=16)
AV.cylinder("y", (1.5, 1.5), 0.75, 1.45, 1.5, "dark", 16)                   # its refractory lining, seen at the rim
for x in (0.2, 2.8):
	for z in (0.2, 2.8):
		AV.box((x - 0.05, 0.4, z - 0.05), (x + 0.05, 2.2, z + 0.05), "galv")   # the hood's legs
AV.box((0.1, 2.2, 0.1), (2.9, 2.6, 2.9), "galv")                            # the hood
AV.box((1.1, 2.6, 1.1), (1.9, 3.0, 1.9), "galv")                            # the batch hopper
AV.cylinder("z", (2.4, 2.2), 0.25, 2.6, 3.0, "galv", 10)                    # the duct to the baghouse, south
box480(AV, 0.4, 0.3)
AV.collide((0.0, 0.0, 0.0), (3.0, 1.5, 3.0))


M = FX
EXTRA = [SF, CB, MR, AV]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
