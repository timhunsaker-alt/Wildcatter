"""The sulfuric acid plant at true size (docs/multiblocks/acid-plant.md), each part drawn facing north from its own main cell (the
footprints match AcidPlantPartBlock's):
- sulfur_melter, 4 x 3 x 8: the concrete melting pit with its grating, the steam coil headers, the receiving shed over the front,
  the submerged sulfur pump at the back (molten sulfur out the back), start-up gas on the east;
- drying_tower, 3 x 12 x 3: the brick-lined packed tower and its acid pump tank;
- main_blower, 3 x 3 x 5: the centrifugal blower, its inlet duct and the 250 kW two-pole motor (its 4.16 kV box on the west);
- sulfur_furnace, 3 x 3 x 10: the brick-lined horizontal furnace on saddles, the sulfur burner gun at its front;
- waste_heat_boiler, 3 x 4 x 8: the fire-tube boiler and its steam drum, the silencer; burner gas out the back;
- converter, 5 x 15 x 5: the four-bed stainless converter with its gas nozzles; burner gas in the front;
- interpass_absorber, 3 x 12 x 3: the brick-lined absorption tower and its acid cooler;
- final_absorber, 3 x 20 x 3: the final tower with the stack on top; acid out the south of its foot."""
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


def tower(M, cx, cz, r, top, tex="grey", pad=True):
	if pad:
		M.box((cx - r - 0.2, 0.0, cz - r - 0.2), (cx + r + 0.2, 0.3, cz + r + 0.2), "concrete")
	M.cylinder("y", (cx, cz), r, 0.3, top, tex, 16)
	M.cylinder("y", (cx, cz), r * 0.6, top, top + 0.3, tex, 12)
	for y in range(3, int(top), 4):
		M.ring("y", (cx, cz), r + 0.2, y, y + 0.06, 0.2, "checker", sides=16)


# ---- the sulfur melter ------------------------------------------------------------------------------------------------------------
SM = model("sulfur_melter", 4, 3, 8, "concrete")
SM.box((0.0, 0.0, 0.0), (4.0, 1.1, 8.0), "concrete")                        # the pit's walls, above grade
SM.box((0.3, 1.1, 0.3), (3.7, 1.15, 7.7), "grating")                        # its grating
for x in (0.8, 2.0, 3.2):
	SM.pipe([(x, 1.25, 0.6), (x, 1.25, 7.4)], 2, "lagging")                 # the steam coil headers
SM.box((0.2, 1.1, 0.2), (3.8, 2.9, 2.6), "galv")                            # the receiving shed over the front
SM.box((0.1, 2.9, 0.1), (3.9, 2.98, 2.7), "grey")
SM.cylinder("y", (2.0, 6.9), 0.3, 1.15, 2.2, "blue", 12)                    # the sulfur pump's motor
SM.pipe([(2.0, 1.2, 6.9), (2.0, 1.2, 7.6), (2.0, 0.5, 7.6), (2.0, 0.5, 8.0)], 3, "lagging")   # molten sulfur out, the back
SM.pipe([(4.0, 0.5, 4.0), (3.7, 0.5, 4.0)], 2, "steel")                    # start-up gas, the east side
SM.collide((0.0, 0.0, 0.0), (4.0, 1.15, 8.0))
SM.collide((0.2, 1.1, 0.2), (3.8, 2.98, 2.7))

# ---- the drying tower -------------------------------------------------------------------------------------------------------------
DT = model("drying_tower", 3, 12, 3, "grey")
tower(DT, 1.5, 1.5, 1.1, 11.3)
DT.ladder(0.2, 0.15, 0.3, 11.0, -1, "galv")
DT.collide((0.4, 0.0, 0.4), (2.6, 11.6, 2.6))

# ---- the main blower --------------------------------------------------------------------------------------------------------------
MB = model("main_blower", 3, 3, 5, "blue")
MB.box((0.0, 0.0, 0.0), (3.0, 0.25, 5.0), "concrete")
MB.box((0.4, 0.25, 0.3), (2.6, 2.6, 2.0), "grey")                           # the blower's scroll casing
MB.cylinder("z", (1.5, 1.4), 0.9, 0.0, 0.3, "grey", 16)                     # its inlet, the drying tower's duct
MB.box((1.0, 2.6, 0.6), (2.0, 2.95, 1.6), "grey")                           # the discharge to the furnace
MB.box((1.2, 0.25, 2.0), (1.8, 0.9, 2.6), "iron")
MB.cylinder("z", (1.5, 1.1), 0.75, 2.6, 4.7, "blue", 16)                    # the 250 kW motor
MB.fins((0.85, 0.4, 2.8), (2.15, 1.8, 4.5), "z", 10, 0.02, "blue")
MB.box((0.0, 0.6, 0.3), (0.35, 1.3, 0.9), "grey")                           # its 4.16 kV box, west
MB.decal((-0.01, 0.75, 0.4), (0.0, 1.15, 0.8), "danger", "west", "grey")
MB.collide((0.4, 0.0, 0.0), (2.6, 2.6, 4.7))

# ---- the sulfur furnace -----------------------------------------------------------------------------------------------------------
SF = model("sulfur_furnace", 3, 3, 10, "grey")
for z in (1.0, 5.0, 9.0):
	SF.box((0.4, 0.0, z - 0.3), (2.6, 0.5, z + 0.3), "concrete")            # its saddles
SF.cylinder("z", (1.5, 1.6), 1.25, 0.4, 9.8, "grey", 18)                    # the brick-lined shell
for z in (2.0, 4.0, 6.0, 8.0):
	SF.ring("z", (1.5, 1.6), 1.28, z, z + 0.1, 0.05, "iron", sides=18)
SF.cylinder("z", (1.5, 1.6), 0.25, 0.0, 0.4, "steel", 10)                  # the burner gun
SF.pipe([(3.0, 0.4, 0.6), (2.6, 0.4, 0.6)], 2, "lagging")                # molten sulfur in, the east side of the front
SF.collide((0.25, 0.0, 0.4), (2.75, 2.85, 9.8))

# ---- the waste heat boiler --------------------------------------------------------------------------------------------------------
WB = model("waste_heat_boiler", 3, 4, 8, "lagging")
for z in (1.0, 7.0):
	WB.box((0.3, 0.0, z - 0.3), (2.7, 0.4, z + 0.3), "concrete")
WB.cylinder("z", (1.5, 1.4), 1.1, 0.2, 7.8, "lagging", 18)                  # the fire-tube shell
WB.cylinder("z", (1.5, 2.9), 0.55, 1.0, 7.0, "lagging", 14)                 # the steam drum
WB.pipe([(1.5, 3.4, 4.0), (1.5, 3.95, 4.0)], 6, "steel")
WB.cylinder("y", (2.4, 4.0), 0.3, 2.5, 3.95, "steel", 12)                    # the silencer
WB.pipe([(1.5, 1.4, 7.8), (1.5, 0.5, 7.8), (1.5, 0.5, 8.0)], 12, "steel")   # burner gas out, the back
WB.pipe([(0.0, 0.6, 2.0), (0.4, 0.6, 2.0)], 2, "galv")                     # feedwater
WB.collide((0.4, 0.0, 0.2), (2.6, 3.4, 7.8))

# ---- the converter ----------------------------------------------------------------------------------------------------------------
CV = model("converter", 5, 15, 5, "steel")
CV.box((0.1, 0.0, 0.1), (4.9, 0.4, 4.9), "concrete")
CV.ring("y", (2.5, 2.5), 2.25, 0.4, 13.5, 0.1, "steel", sides=20)                  # the four-bed converter
disc(CV, 2.50, 2.50, 2.25, 13.45, 13.50, "steel")   # its roof
CV.ring("y", (2.5, 2.5), 1.4, 13.5, 14.3, 0.1, "steel", sides=16)
disc(CV, 2.50, 2.50, 1.40, 14.25, 14.30, "steel")   # its roof
for y in (3.5, 6.5, 9.5, 12.5):
	CV.ring("y", (2.5, 2.5), 2.3, y, y + 0.15, 0.05, "iron", sides=20)      # a bed's support ring
for y, x in ((2.0, 0.0), (5.0, 5.0), (8.0, 0.0), (11.0, 5.0)):
	CV.cylinder("x", (y, 2.5), 0.4, min(x, 4.6), max(x, 0.4), "steel", 12)  # gas ducts to and from the absorbers and exchangers
CV.cylinder("z", (2.5, 0.8), 0.4, 0.0, 0.4, "steel", 12)                   # the burner gas duct in, the front
CV.ladder(0.4, 0.4, 0.5, 14.0, -1, "galv")
CV.collide((0.25, 0.0, 0.25), (4.75, 14.3, 4.75))

# ---- the interpass absorber -------------------------------------------------------------------------------------------------------
IA = model("interpass_absorber", 3, 12, 3, "grey")
tower(IA, 1.5, 1.5, 1.1, 11.3)
IA.box((0.0, 0.3, 0.0), (0.9, 1.5, 0.9), "finned")                          # its acid cooler
IA.collide((0.4, 0.0, 0.4), (2.6, 11.6, 2.6))

# ---- the final absorber and stack -------------------------------------------------------------------------------------------------
FA = model("final_absorber", 3, 20, 3, "grey")
tower(FA, 1.5, 1.5, 1.1, 11.3)
FA.cylinder("y", (1.5, 1.5), 0.45, 11.6, 19.9, "hipaint", 14)              # the stack
FA.ring("y", (1.5, 1.5), 0.6, 17.0, 17.08, 0.15, "checker", sides=14)        # the sampling platform
FA.pipe([(1.5, 0.5, 2.6), (1.5, 0.5, 3.0)], 3, "steel")                   # acid out, the south
FA.pipe([(0.0, 0.6, 1.5), (0.4, 0.6, 1.5)], 2, "galv")                     # process water
FA.collide((0.4, 0.0, 0.4), (2.6, 11.6, 2.6))
FA.collide((1.05, 11.6, 1.05), (1.95, 19.9, 1.95))


M = SM
EXTRA = [DT, MB, SF, WB, CV, IA, FA]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
