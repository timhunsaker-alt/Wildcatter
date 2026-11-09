"""The ammonia plant at true size (docs/multiblocks/ammonia-plant.md), each part drawn facing north from its own main cell (the
footprints match AmmoniaPartBlock's):
- nitrogen_generator, 2 x 3 x 3: the PSA skid, its air compressor, two carbon sieve beds and the nitrogen receiver (box west);
- syngas_compressor, 3 x 3 x 5: a two-throw reciprocating compressor, its motor, pulsation bottles and the inter-stage cooler (box west);
- ammonia_converter, 3 x 10 x 3: the thick-walled converter, its feed-effluent exchanger, the chiller and separator (box west);
- nitric_acid_unit, 4 x 12 x 4: the burner with its gauze, the cooler-condenser, the absorption tower and the air blower (box west);
- an_neutraliser, 3 x 5 x 3: the neutraliser vessel with its vapour line up and the solution run-down tank;
- emulsion_blender, 3 x 3 x 4: the high-shear blender, its solution and fuel-phase tanks and the matrix pump (box west)."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


def vessel(M, cx, cz, r, y0, y1, tex="steel", sides=16):
	M.cylinder("y", (cx, cz), r, y0, y1, tex, sides)
	M.cylinder("y", (cx, cz), r * 0.6, y1, y1 + 0.25, tex, 12)


# ---- the nitrogen generator ----------------------------------------------------------------------------------------------------
NG = model("nitrogen_generator", 2, 3, 3, "blue")
NG.box((0.05, 0.0, 0.05), (1.95, 0.2, 2.95), "iron")
NG.box((0.3, 0.2, 0.2), (1.7, 1.3, 1.0), "blue")                            # the air compressor package
NG.fins((0.35, 0.4, 0.15), (1.65, 1.2, 0.2), "x", 10, 0.02, "radfin")       # its cooler grille
for x in (0.6, 1.4):
	vessel(NG, x, 1.6, 0.3, 0.2, 2.5, "padgreen", 12)                       # the two carbon molecular sieve beds
NG.cylinder("y", (1.0, 2.5), 0.3, 0.2, 2.2, "steel", 12)                   # the nitrogen receiver
NG.pipe([(0.6, 2.6, 1.6), (1.0, 2.6, 1.6), (1.4, 2.6, 1.6)], 1, "steel")
NG.pipe([(1.0, 0.5, 2.5), (1.0, 0.5, 3.0)], 1, "steel")                     # nitrogen out, the back
NG.box((1.7, 0.6, 1.2), (1.95, 1.4, 1.6), "grey")                           # its controller
NG.decal((1.95, 0.8, 1.25), (1.96, 1.3, 1.55), "controller", "east", "grey")
box480(NG, 0.4, 0.3)
NG.collide((0.05, 0.0, 0.05), (1.95, 2.5, 2.95))

# ---- the syngas compressor -------------------------------------------------------------------------------------------------------
SC = model("syngas_compressor", 3, 3, 5, "engine")
SC.box((0.0, 0.0, 0.0), (3.0, 0.3, 5.0), "concrete")                        # its block foundation
SC.box((0.5, 0.3, 0.3), (2.5, 1.3, 1.6), "blue")                            # the motor
SC.cylinder("z", (1.5, 0.85), 0.2, 1.6, 2.0, "iron", 12)                    # the shaft through to the frame
SC.box((0.6, 0.3, 2.0), (2.4, 1.2, 3.6), "engine")                          # the crankcase frame
for x in (0.4, 2.6):
	SC.cylinder("x", (0.95, 2.8), 0.32, x - 0.4 if x < 1 else x, x + 0.4 if x < 1 else x + 0.35, "engine", 12)   # the two throws' cylinders
	SC.cylinder("z", (x, 0.95), 0.28, 3.6, 4.8, "steel", 12)                 # their pulsation bottles
SC.box((0.9, 0.3, 3.8), (2.1, 1.5, 4.8), "radfin")                          # the inter-stage cooler
SC.pipe([(3.0, 0.5, 1.0), (2.6, 0.5, 1.0)], 2, "steel")                    # hydrogen in, the east
SC.pipe([(3.0, 0.5, 3.0), (2.6, 0.5, 3.0)], 2, "steel")                    # nitrogen in, the east
SC.decal((2.5, 0.6, 0.6), (2.51, 1.0, 1.0), "ratingplate", "east", "blue")
box480(SC, 0.4, 0.3)
SC.collide((0.4, 0.0, 0.2), (2.6, 1.5, 4.8))

# ---- the ammonia converter -------------------------------------------------------------------------------------------------------
AC = model("ammonia_converter", 3, 10, 3, "steel")
AC.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
vessel(AC, 1.1, 1.1, 0.7, 0.3, 9.4, "steel", 18)                            # the converter, its catalyst baskets inside
for y in (1.5, 4.0, 6.5, 9.0):
	AC.ring("y", (1.1, 1.1), 0.72, y, y + 0.12, 0.06, "iron", sides=18)     # its forged shell courses
AC.cylinder("y", (2.4, 0.6), 0.28, 0.3, 4.5, "lagging", 12)                 # the feed-effluent exchanger
AC.box((1.8, 0.3, 1.9), (2.9, 1.4, 2.9), "blue")                            # the ammonia chiller
AC.cylinder("y", (2.4, 1.6), 0.25, 0.3, 2.6, "steel", 12)                   # the separator
AC.pipe([(1.1, 9.65, 1.1), (1.1, 9.8, 1.1), (2.4, 9.8, 1.1), (2.4, 9.8, 0.6), (2.4, 4.5, 0.6)], 2, "steel")
AC.pipe([(1.5, 0.5, 2.6), (1.5, 0.5, 3.0)], 1, "steel")                     # liquid ammonia out, the south
AC.ladder(0.15, 2.4, 0.3, 9.4, 1, "galv")
box480(AC, 0.4, 2.2)
AC.collide((0.4, 0.0, 0.4), (2.9, 9.4, 2.9))

# ---- the nitric acid unit ---------------------------------------------------------------------------------------------------------
NA = model("nitric_acid_unit", 4, 12, 4, "steel")
NA.box((0.0, 0.0, 0.0), (4.0, 0.3, 4.0), "concrete")
NA.box((0.3, 0.3, 0.3), (1.7, 1.4, 1.3), "blue")                            # the air blower
NA.cylinder("y", (1.0, 2.2), 0.5, 0.3, 3.2, "hipaint", 16)                  # the burner, its gauze across the top of the cone
NA.cylinder("y", (1.0, 2.2), 0.65, 3.2, 3.6, "steel", 16)                   # the gauze flange
NA.cylinder("y", (1.0, 2.2), 0.4, 3.6, 4.6, "steel", 14)
NA.cylinder("y", (1.0, 3.4), 0.35, 0.3, 3.0, "steel", 12)                   # the cooler-condenser
vessel(NA, 2.9, 2.0, 0.75, 0.3, 11.3, "steel", 18)                           # the absorption tower, stainless
for y in (3.0, 6.0, 9.0):
	NA.ring("y", (2.9, 2.0), 0.77, y, y + 0.12, 0.06, "galv", sides=18)
NA.pipe([(1.0, 4.6, 2.2), (1.0, 5.2, 2.2), (2.2, 5.2, 2.2)], 3, "steel")   # nitrous gas to the tower
NA.cylinder("y", (2.9, 2.6), 0.12, 11.3, 11.95, "steel", 8)                # the tail gas stack
NA.box((2.55, 11.55, 1.45), (3.25, 11.95, 2.15), "galv")                    # the gauze hatch on the tower head
NA.pipe([(4.0, 0.5, 1.0), (3.6, 0.5, 1.0)], 2, "steel")                    # ammonia in, the east
NA.pipe([(2.9, 0.5, 3.2), (2.9, 0.5, 4.0)], 2, "steel")                    # acid out, the south
NA.ladder(3.75, 2.0, 0.3, 11.3, 1, "galv")
box480(NA, 0.4, 0.4)
NA.collide((0.3, 0.0, 0.3), (3.7, 11.3, 3.7))

# ---- the AN neutraliser -----------------------------------------------------------------------------------------------------------
AN = model("an_neutraliser", 3, 5, 3, "steel")
AN.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
vessel(AN, 1.2, 1.2, 0.75, 0.3, 3.8, "steel", 18)                            # the neutraliser
AN.pipe([(1.2, 4.05, 1.2), (1.2, 4.9, 1.2)], 3, "steel")                   # its vapour line
AN.cylinder("y", (2.4, 2.4), 0.35, 0.3, 1.6, "lagging", 12)                 # the run-down tank, kept hot
AN.pipe([(3.0, 0.5, 0.5), (2.6, 0.5, 0.5)], 2, "steel")                    # ammonia in, the east
AN.pipe([(3.0, 0.5, 1.5), (2.6, 0.5, 1.5)], 2, "steel")                    # acid in, the east
AN.pipe([(1.5, 0.5, 2.6), (1.5, 0.5, 3.0)], 2, "lagging")                  # solution out, the south
AN.collide((0.4, 0.0, 0.4), (2.8, 3.8, 2.8))

# ---- the emulsion blender ---------------------------------------------------------------------------------------------------------
EB = model("emulsion_blender", 3, 3, 4, "grey")
EB.box((0.05, 0.0, 0.05), (2.95, 0.2, 3.95), "iron")
EB.cylinder("y", (0.8, 1.0), 0.5, 0.2, 1.8, "lagging", 14)                  # the hot solution tank
EB.cylinder("y", (2.2, 1.0), 0.4, 0.2, 1.6, "steel", 12)                    # the fuel phase tank, diesel and emulsifier
EB.box((0.9, 0.2, 2.0), (2.1, 1.1, 3.0), "steel")                           # the high-shear blender
EB.box((1.1, 1.1, 2.2), (1.9, 1.7, 2.8), "blue")                            # its motor
EB.box((2.2, 0.2, 2.4), (2.8, 0.7, 3.4), "blue")                            # the matrix pump
EB.box((0.6, 2.4, 0.6), (1.4, 2.9, 1.4), "galv")                            # the pail hopper
EB.pipe([(3.0, 0.5, 0.5), (2.6, 0.5, 0.5)], 2, "lagging")                  # AN solution in, the east
EB.pipe([(3.0, 0.5, 1.6), (2.6, 0.5, 1.6)], 1, "steel")                    # diesel in, the east
EB.pipe([(1.5, 0.5, 3.6), (1.5, 0.5, 4.0)], 2, "steel")                    # matrix out, the back
box480(EB, 0.3, 0.2)
EB.collide((0.05, 0.0, 0.05), (2.95, 1.8, 3.95))


M = NG
EXTRA = [SC, AC, NA, AN, EB]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
