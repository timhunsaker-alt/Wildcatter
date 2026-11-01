"""The 16 MW gas turbine plant at true size (docs/multiblocks/gas-turbine.md), each part drawn facing north from its own main cell
(the footprints match TurbinePartBlock's; the GSU's is Gsu25TransformerBlock's):
- gas_turbine, 4 x 4 x 10: the package on its base: the acoustic enclosure with its doors, vent fans and lights, the air inlet
  plenum at the front (north), the exhaust collector at the back (south), the reduction gearbox and the 300 hp starter with its
  4.16 kV box on the west side, the fuel skid and its nozzle on the west side;
- gt_inlet_house, 4 x 6 x 4: the filter house on its legs, the weather hoods, the inlet silencer and duct down to the turbine;
- gt_generator, 4 x 4 x 6: the 20.6 MVA generator on its base, its air cooler on top, the line-side terminal box at the back;
- gen_breaker, 2 x 3 x 2: the generator circuit breaker's metal-clad enclosure;
- gt_exhaust_stack, 3 x 12 x 3: the exhaust transition and silencer, the stack;
- load_bank, 3 x 3 x 6: a containerized resistive load bank, its intake louvres and the hot-air outlet on top;
- gsu_25, 5 x 5 x 4: the 25 MVA step-up: the tank, radiators with their fans on both sides, the conservator, three 138 kV bushings
  on the cover and the 13.8 kV bus duct throat low on the back."""
import mlib
import plantlib as g

TEX = dict(g.TEX)
TEX.update({"porcelain": "block/mat/mat_porcelain_brown"})


def model(name, w, h, d, particle="grey", main=(0, 0)):
	return mlib.Model(name, mlib.footprint_box(w, h, d, main[0], main[1]), dict(TEX), TEX[particle])


# ---- the gas turbine package --------------------------------------------------------------------------------------------------
GT = model("gas_turbine", 4, 4, 10)
GT.box((0.0, 0.0, 0.0), (4.0, 0.35, 10.0), "iron")                        # the base
GT.box((0.5, 0.35, 0.0), (3.5, 3.0, 1.8), "grey")                         # the air inlet plenum
GT.box((0.3, 0.35, 1.8), (3.7, 3.6, 8.2), "grey")                         # the enclosure
for z in (2.4, 4.0, 5.6, 7.2):
	GT.box((0.25, 0.35, z), (0.3, 3.6, z + 0.06), "iron")                  # its panel joints
	GT.box((3.7, 0.35, z), (3.75, 3.6, z + 0.06), "iron")
for z in (3.0, 6.2):
	GT.box((3.75, 0.5, z), (3.8, 2.4, z + 0.9), "dark")                   # the doors on the east side
GT.cylinder("y", (1.2, 4.6), 0.35, 3.6, 3.9, "grey", 12)                  # the vent fans
GT.cylinder("y", (2.8, 6.2), 0.35, 3.6, 3.9, "grey", 12)
GT.box((0.6, 0.35, 8.2), (3.4, 3.4, 10.0), "lagging")                     # the exhaust collector, insulated
GT.box((0.3, 0.35, 1.0), (1.3, 1.6, 2.6), "engine")                       # the reduction gearbox at the drive end, west
GT.cylinder("z", (0.75, 1.2), 0.35, 1.9, 3.0, "blue", 14)                 # the 300 hp starter
GT.box((0.0, 0.3, 2.2), (0.3, 1.2, 2.8), "grey")                          # its 4.16 kV terminal box, west face of x 0, z 2
GT.decal((-0.01, 0.4, 2.3), (0.0, 1.0, 2.7), "danger", "west", "grey")
GT.box((0.05, 0.35, 5.8), (0.3, 1.4, 7.0), "dark")                        # the fuel skid and its nozzle, west face of x 0, z 6
GT.pipe([(0.0, 0.6, 6.5), (0.3, 0.6, 6.5)], 3, "steel")
GT.decal((3.8, 2.6, 4.5), (3.81, 3.0, 5.3), "nameplate", "east", "grey")
GT.collide((0.0, 0.0, 0.0), (4.0, 3.6, 10.0))

GI = model("gt_inlet_house", 4, 6, 4)
for x in (0.2, 3.6):
	for z in (0.2, 3.6):
		GI.box((x, 0.0, z), (x + 0.2, 2.6, z + 0.2), "galv")               # the legs
GI.box((0.0, 2.6, 0.0), (4.0, 5.4, 4.0), "grey")                          # the filter house
for y in (3.0, 3.9, 4.8):
	GI.box((0.0, y, -0.25), (4.0, y + 0.08, 0.0), "grey")                 # the weather hoods on the front
	GI.box((0.0, y - 0.6, -0.27), (4.0, y, -0.25), "galv")
GI.box((1.0, 0.4, 1.0), (3.0, 2.6, 3.0), "grey")                          # the silencer and duct down to the turbine
GI.box((0.0, 5.4, 0.0), (4.0, 5.55, 4.0), "iron")
GI.collide((0.0, 2.6, 0.0), (4.0, 5.55, 4.0))
GI.collide((1.0, 0.0, 1.0), (3.0, 2.6, 3.0))

GG = model("gt_generator", 4, 4, 6, "blue")
GG.box((0.0, 0.0, 0.0), (4.0, 0.35, 6.0), "iron")
GG.box((0.4, 0.35, 0.3), (3.6, 2.9, 5.2), "blue")                         # the generator frame
GG.fins((0.45, 0.6, 0.6), (3.55, 2.7, 5.0), "z", 12, 0.03, "blue")
GG.box((0.6, 2.9, 0.8), (3.4, 3.8, 4.6), "grey")                          # the air-to-air cooler on top
GG.fins((0.65, 3.0, 0.9), (3.35, 3.7, 4.5), "x", 14, 0.03, "galv")
GG.cylinder("z", (2.0, 1.6), 0.35, 0.0, 0.3, "steel", 14)                 # the shaft to the gearbox
GG.box((0.5, 0.35, 5.2), (3.5, 2.6, 6.0), "grey")                         # the line-side terminal box, back
GG.decal((3.61, 1.2, 2.0), (3.62, 1.8, 3.2), "nameplate", "east", "blue")
GG.collide((0.0, 0.0, 0.0), (4.0, 3.8, 6.0))

GB = model("gen_breaker", 2, 3, 2, "grey")
GB.box((0.05, 0.0, 0.05), (1.95, 2.6, 1.95), "grey")                       # the metal-clad enclosure
GB.box((0.15, 0.4, 0.0), (0.95, 2.2, 0.05), "iron")                        # its door, front
GB.decal((0.3, 1.5, -0.01), (0.8, 2.0, 0.0), "controller", "north", "iron")
GB.decal((1.1, 1.6, -0.01), (1.6, 2.1, 0.0), "danger", "north", "grey")
GB.box((0.0, 0.2, 0.0), (2.0, 0.6, 2.0), "iron")
GB.cylinder("y", (1.0, 1.0), 0.15, 2.6, 2.9, "galv", 8)                    # its vent
GB.collide((0.05, 0.0, 0.05), (1.95, 2.6, 1.95))

ES = model("gt_exhaust_stack", 3, 12, 3)
ES.box((0.1, 0.0, 0.1), (2.9, 0.3, 2.9), "concrete")
ES.box((0.2, 0.3, 0.2), (2.8, 3.0, 2.8), "lagging")                        # the transition and silencer
ES.cylinder("y", (1.5, 1.5), 1.1, 3.0, 11.8, "grey", 18)                   # the stack
ES.ring("y", (1.5, 1.5), 1.15, 11.6, 11.8, 0.05, "iron", sides=18)
for y in (6.0, 9.0):
	ES.ring("y", (1.5, 1.5), 1.14, y, y + 0.06, 0.03, "iron", sides=18)
ES.collide((0.2, 0.0, 0.2), (2.8, 3.0, 2.8))
ES.collide((0.4, 3.0, 0.4), (2.6, 11.8, 2.6))

LB = model("load_bank", 3, 3, 6, "grey")
LB.box((0.1, 0.0, 0.0), (2.9, 2.6, 6.0), "grey")                          # the container
for z in (1.0, 2.5, 4.0):
	LB.fins((0.08, 0.6, z), (0.1, 2.0, z + 1.0), "y", 8, 0.03, "dark")     # intake louvres
	LB.fins((2.9, 0.6, z), (2.92, 2.0, z + 1.0), "y", 8, 0.03, "dark")
LB.box((0.5, 2.6, 1.0), (2.5, 2.9, 5.0), "galv")                          # the hot-air outlet on top
LB.box((0.4, 0.3, -0.02), (2.6, 1.4, 0.0), "dark")                        # its 13.8 kV terminal compartment, front
LB.decal((0.8, 1.6, -0.02), (2.2, 2.2, 0.0), "danger", "north", "grey")
LB.collide((0.1, 0.0, 0.0), (2.9, 2.6, 6.0))

# ---- the 25 MVA GSU: drawn in the frame of its main cell, the back of the bottom middle (it spans x -2..3, z -3..1) -------------
GS = model("gsu_25", 5, 5, 4, "blue", main=(2, 3))
GS.box((-1.5, 0.0, -2.6), (2.5, 0.3, 0.6), "iron")                         # the skid
GS.box((-1.3, 0.3, -2.4), (2.3, 3.4, 0.4), "blue")                         # the tank
for x in (-1.9, 2.3):
	GS.fins((x, 0.6, -2.2), (x + 0.6, 3.0, 0.2), "z", 10, 0.04, "blue")    # the radiators on both sides
	GS.cylinder("x", (0.9, -1.0), 0.3, x + (0.0 if x < 0 else 0.55), x + (0.05 if x < 0 else 0.6), "dark", 12)   # their fans
GS.cylinder("x", (4.0, -2.0), 0.35, -1.0, 2.0, "blue", 12)                 # the conservator on its brackets
GS.box((-0.8, 3.4, -2.1), (-0.6, 3.65, -1.9), "iron")
GS.box((1.6, 3.4, -2.1), (1.8, 3.65, -1.9), "iron")
for x in (-0.4, 0.5, 1.4):
	GS.cylinder("y", (x, -1.6), 0.15, 3.4, 4.7, "porcelain", 10)           # the 138 kV bushings
	for y in (3.7, 4.0, 4.3, 4.6):
		GS.ring("y", (x, -1.6), 0.24, y - 0.03, y + 0.03, 0.08, "porcelain", sides=10)
	GS.cylinder("y", (x, -1.6), 0.05, 4.7, 4.95, "copper", 8)
GS.box((-0.2, 0.4, 0.4), (1.2, 2.2, 1.0), "grey")                          # the 13.8 kV bus duct throat, back
GS.decal((-1.31, 1.4, -1.6), (-1.3, 2.2, -0.4), "nameplate", "west", "blue")
GS.decal((2.3, 1.4, -1.6), (2.31, 2.2, -0.4), "danger", "east", "blue")
GS.collide((-1.9, 0.0, -2.6), (2.9, 3.4, 0.6))

M = GT
EXTRA = [GI, GG, GB, ES, LB, GS]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
