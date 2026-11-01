"""The 45 MW aeroderivative peaker and the 60 MVA transformers at true size (docs/multiblocks/aeroderivative.md), each part drawn facing
north from its own main cell (the footprints match TurbinePartBlock's and Gsu60TransformerBlock's):
- aero_turbine, 5 x 5 x 14: the package on its base: the enclosure with its doors and vent fans, the air inlet plenum at the front,
  the exhaust collector at the back, the auxiliary skid and the hydraulic starter's 300 hp motor with its 4.16 kV box on the west
  side, the fuel skid and its nozzle on the west side;
- aero_inlet_house, 6 x 8 x 5: the filter house on its structure, its evaporative cooler and silencer, the duct down;
- aero_generator, 4 x 4 x 7: the 56.25 MVA two-pole generator, its coolers on top and the line-side terminal box at the back;
- aero_exhaust_stack, 4 x 14 x 4: the exhaust transition and silencer, the stack, the blank SCR housing;
- gen_breaker_3000, 2 x 3 x 2: the 3,000 A generator breaker's metal-clad enclosure;
- gsu_60 and substation_transformer_60, 6 x 6 x 5: a 60 MVA tank with radiators and fans on both sides, the conservator, three
  138 kV bushings on the cover over the front, the other winding's throat low on the back."""
import mlib
import plantlib as g

TEX = dict(g.TEX)
TEX.update({"porcelain": "block/mat/mat_porcelain_brown"})


def model(name, w, h, d, particle="grey", main=(0, 0)):
	return mlib.Model(name, mlib.footprint_box(w, h, d, main[0], main[1]), dict(TEX), TEX[particle])


# ---- the aeroderivative package -----------------------------------------------------------------------------------------------
AT = model("aero_turbine", 5, 5, 14)
AT.box((0.0, 0.0, 0.0), (5.0, 0.4, 14.0), "iron")                          # the base
AT.box((0.6, 0.4, 0.0), (4.4, 3.8, 2.4), "grey")                           # the inlet plenum
AT.box((0.3, 0.4, 2.4), (4.7, 4.4, 11.4), "louvre")                        # the enclosure
for z in (3.2, 5.2, 7.2, 9.2):
	AT.box((0.25, 0.4, z), (0.3, 4.4, z + 0.06), "iron")
	AT.box((4.7, 0.4, z), (4.75, 4.4, z + 0.06), "iron")
for z in (4.0, 8.0):
	AT.box((4.75, 0.6, z), (4.8, 2.6, z + 1.0), "dark")                    # doors, east
for z in (5.0, 8.5):
	AT.cylinder("y", (2.5, z), 0.45, 4.4, 4.75, "grey", 12)                # vent fans
AT.box((0.8, 0.4, 11.4), (4.2, 4.0, 14.0), "lagging")                      # the exhaust collector
AT.box((0.3, 0.4, 1.6), (1.4, 1.8, 3.6), "engine")                         # the auxiliary skid and hydraulic start pump
AT.cylinder("z", (0.85, 1.3), 0.4, 2.0, 3.4, "blue", 14)                   # its 300 hp motor
AT.box((0.0, 0.3, 2.2), (0.3, 1.2, 2.8), "grey")                           # its 4.16 kV box, west face of x 0, z 2
AT.decal((-0.01, 0.4, 2.3), (0.0, 1.0, 2.7), "danger", "west", "grey")
AT.box((0.05, 0.4, 5.8), (0.3, 1.5, 7.2), "dark")                          # the fuel skid, west face of x 0, z 6
AT.pipe([(0.0, 0.7, 6.5), (0.3, 0.7, 6.5)], 4, "steel")
AT.decal((4.8, 3.0, 6.0), (4.81, 3.5, 7.0), "nameplate", "east", "grey")
AT.decal((4.7, 0.8, 5.8), (4.71, 2.0, 6.9), "annunciator", "east", "grey")   # its local control panel
AT.collide((0.0, 0.0, 0.0), (5.0, 4.4, 14.0))

AI = model("aero_inlet_house", 6, 8, 5)
for x in (0.2, 5.6):
	for z in (0.2, 4.6):
		AI.box((x, 0.0, z), (x + 0.2, 3.4, z + 0.2), "galv")
AI.box((0.0, 3.4, 0.0), (6.0, 7.6, 5.0), "filter")                         # the filter house
for y in (4.0, 5.2, 6.4):
	AI.box((0.0, y, -0.3), (6.0, y + 0.1, 0.0), "grey")                    # weather hoods
AI.box((0.0, 3.4, -0.32), (6.0, 3.9, -0.3), "padgreen")                    # the evaporative cooler's sump
AI.box((1.5, 0.4, 1.0), (4.5, 3.4, 4.0), "grey")                           # the duct down
AI.collide((0.0, 3.4, 0.0), (6.0, 7.6, 5.0))
AI.collide((1.5, 0.0, 1.0), (4.5, 3.4, 4.0))

AG = model("aero_generator", 4, 4, 7, "blue")
AG.box((0.0, 0.0, 0.0), (4.0, 0.4, 7.0), "iron")
AG.box((0.3, 0.4, 0.3), (3.7, 3.1, 6.2), "blue")
AG.fins((0.35, 0.7, 0.6), (3.65, 2.9, 6.0), "z", 14, 0.03, "blue")
for z in (0.8, 3.4):
	AG.box((0.6, 3.1, z), (3.4, 3.9, z + 2.2), "grey")                    # the coolers on top
	AG.fins((0.65, 3.2, z + 0.1), (3.35, 3.8, z + 2.1), "x", 12, 0.03, "galv")
AG.cylinder("z", (2.0, 1.7), 0.4, 0.0, 0.3, "steel", 14)
AG.box((0.4, 0.4, 6.2), (3.6, 2.8, 7.0), "grey")                           # the terminal box, back
AG.decal((3.7, 1.2, 2.4), (3.71, 2.2, 4.2), "ratingplate", "east", "blue")
AG.collide((0.0, 0.0, 0.0), (4.0, 3.9, 7.0))

AS = model("aero_exhaust_stack", 4, 14, 4)
AS.box((0.1, 0.0, 0.1), (3.9, 0.3, 3.9), "concrete")
AS.box((0.2, 0.3, 0.2), (3.8, 3.6, 3.8), "lagging")                        # the transition and silencer
AS.box((0.4, 3.6, 0.4), (3.6, 5.6, 3.6), "grey")                           # the SCR housing (blank)
# the stack: an octagon of two crossed boxes (a cylinder this wide would be centred on a cell corner, where its rotated sides
# reach more than a block from the cell that holds them)
AS.box((1.0, 5.6, 0.6), (3.0, 13.8, 3.4), "hipaint")
AS.box((0.6, 5.6, 1.0), (3.4, 13.8, 3.0), "hipaint")
for y in (8.0, 11.0):
	AS.box((0.95, y, 0.55), (3.05, y + 0.08, 3.45), "iron")
	AS.box((0.55, y, 0.95), (3.45, y + 0.08, 3.05), "iron")
AS.collide((0.2, 0.0, 0.2), (3.8, 5.6, 3.8))
AS.collide((0.6, 5.6, 0.6), (3.4, 13.8, 3.4))

GB = model("gen_breaker_3000", 2, 3, 2, "grey")
GB.box((0.05, 0.0, 0.05), (1.95, 2.8, 1.95), "grey")
GB.box((0.15, 0.4, 0.0), (0.95, 2.4, 0.05), "iron")
GB.decal((0.3, 1.6, -0.01), (0.8, 2.1, 0.0), "controller", "north", "iron")
GB.decal((1.1, 1.7, -0.01), (1.6, 2.2, 0.0), "danger", "north", "grey")
GB.decal((1.95, 1.0, 0.5), (1.96, 1.6, 1.3), "arcflash", "east", "grey")
GB.box((0.0, 0.2, 0.0), (2.0, 0.6, 2.0), "iron")
for x in (0.6, 1.4):
	GB.cylinder("y", (x, 1.0), 0.12, 2.8, 2.95, "galv", 8)
GB.collide((0.05, 0.0, 0.05), (1.95, 2.8, 1.95))


# ---- the 60 MVA tank, drawn in the frame of its main cell (it spans x -3..2, z -4..0) -------------------------------------------
def big_transformer(name):
	T = model(name, 6, 6, 5, "blue", main=(3, 4))
	T.box((-2.6, 0.0, -3.6), (2.6, 0.3, 0.6), "iron")
	T.box((-2.3, 0.3, -3.4), (2.3, 4.2, 0.4), "blue")                       # the tank
	for x in (-2.9, 2.3):
		T.fins((x, 0.6, -3.1), (x + 0.6, 3.8, 0.1), "z", 12, 0.04, "radfin")  # radiators
		for z in (-2.4, -0.6):
			T.cylinder("x", (1.0, z), 0.3, x + (0.0 if x < 0 else 0.55), x + (0.05 if x < 0 else 0.6), "dark", 12)   # their fans
	T.cylinder("x", (4.9, -2.8), 0.45, -1.8, 1.8, "blue", 14)              # the conservator
	for x in (-1.6, 1.6):
		T.box((x - 0.1, 4.2, -2.9), (x + 0.1, 4.45, -2.7), "iron")
	for x in (-0.9, 0.5, 1.9):
		T.cylinder("y", (x, -2.6), 0.18, 4.2, 5.6, "porcelain", 10)        # the 138 kV bushings
		for y in (4.5, 4.8, 5.1, 5.4):
			T.ring("y", (x, -2.6), 0.28, y - 0.03, y + 0.03, 0.1, "porcelain", sides=10)
		T.cylinder("y", (x, -2.6), 0.05, 5.6, 5.9, "copper", 8)
	T.box((-0.2, 0.4, 0.4), (1.2, 2.4, 1.0), "grey")                        # the other winding's throat, back
	T.decal((-2.31, 1.6, -2.4), (-2.3, 2.6, -0.8), "ratingplate", "west", "blue")
	T.decal((2.3, 1.6, -2.4), (2.31, 2.6, -0.8), "danger", "east", "blue")
	T.collide((-2.9, 0.0, -3.6), (2.9, 4.2, 0.6))
	return T


G60 = big_transformer("gsu_60")
S60 = big_transformer("substation_transformer_60")

M = AT
EXTRA = [AI, AG, AS, GB, G60, S60]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
