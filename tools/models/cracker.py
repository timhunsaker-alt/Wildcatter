"""The cracker and polyethylene train at true size (docs/multiblocks/cracker.md), each part drawn facing north from its own main cell
(the footprints match PetrochemPartBlock's):
- cracking_furnace, 4 x 14 x 6: the radiant box on its legs with the burners under it, the coil outlets, the convection section,
  the stack; feed in the front, fuel gas on the east, water on the west, hot cracked gas out the back;
- quench_tower, 2 x 12 x 2: the water quench tower; cracked gas in the north, out the south, pyrolysis gasoline east, fuel oil west;
- cracked_gas_compressor, 4 x 4 x 8: the compressor package (the coupling and the cooler fan turn), its 4.16 kV box on the west;
- cold_box, 3 x 10 x 3: the insulated cold box with the demethanizer and C2 splitter rising out of it; in the north, its three
  products out of the south (LPG low, ethylene, fuel gas high);
- polyethylene_reactor, 3 x 16 x 3: the fluidized-bed reactor with its expanded top, the cycle gas cooler and compressor at its
  foot (the coupling turns);
- pelletizer, 2 x 3 x 6: the sheet bin, the motor (the coupling turns), the gearbox, the extruder barrel with its heater bands, the
  die and cutter."""
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


# ---- the cracking furnace ---------------------------------------------------------------------------------------------------------
CF = model("cracking_furnace", 4, 14, 6, "grey")
for x in (0.2, 3.6):
	for z in (0.3, 2.9, 5.5):
		CF.box((x, 0.0, z), (x + 0.2, 1.2, z + 0.2), "iron")               # the legs
for x in (1.0, 2.0, 3.0):
	for z in (1.2, 3.0, 4.8):
		CF.cylinder("y", (x, z), 0.12, 0.8, 1.2, "dark", 8)                 # the floor burners
CF.box((0.2, 1.2, 0.3), (3.8, 7.5, 5.7), "hipaint")                          # the radiant box
for z in (0.3, 2.0, 4.0, 5.7):
	CF.box((0.15, 1.2, z - 0.05), (0.2, 7.5, z + 0.05), "iron")
	CF.box((3.8, 1.2, z - 0.05), (3.85, 7.5, z + 0.05), "iron")
CF.box((0.8, 7.5, 1.0), (3.2, 10.5, 5.0), "hipaint")                         # the convection section
CF.box((1.5, 10.5, 2.5), (2.5, 11.0, 3.5), "iron")
CF.cylinder("y", (2.0, 3.0), 0.5, 11.0, 13.95, "hipaint", 14)                # the stack
CF.pipe([(1.0, 0.4, 0.0), (1.0, 0.4, 0.3), (1.0, 1.4, 0.3)], 3, "steel")  # feed in, the front
CF.pipe([(4.0, 0.4, 2.0), (3.85, 0.4, 2.0)], 3, "steel")                  # fuel gas, the east side
CF.pipe([(0.0, 0.4, 2.0), (0.15, 0.4, 2.0)], 2, "galv")                   # water, the west side
CF.pipe([(2.0, 1.4, 5.7), (2.0, 0.5, 5.7), (2.0, 0.5, 6.0)], 6, "steel")  # hot cracked gas out, the back
CF.collide((0.2, 1.2, 0.3), (3.8, 7.5, 5.7))
CF.collide((0.8, 7.5, 1.0), (3.2, 11.0, 5.0))

# ---- the quench tower -------------------------------------------------------------------------------------------------------------
QT = model("quench_tower", 2, 12, 2, "padgreen")
QT.box((0.1, 0.0, 0.1), (1.9, 0.3, 1.9), "concrete")
QT.cylinder("y", (1.0, 1.0), 0.75, 0.3, 11.4, "padgreen", 16)
QT.cylinder("y", (1.0, 1.0), 0.5, 11.4, 11.6, "padgreen", 14)
for y in (4.0, 8.0):
	QT.ring("y", (1.0, 1.0), 0.95, y, y + 0.06, 0.2, "checker", sides=16)
QT.ladder(0.3, 0.05, 0.3, 11.2, -1, "galv")
QT.pipe([(1.0, 0.6, 0.0), (1.0, 0.6, 0.25)], 6, "steel")                  # hot cracked gas in, north
QT.pipe([(1.0, 0.5, 1.75), (1.0, 0.5, 2.0)], 6, "steel")                  # cracked gas out, south
QT.pipe([(1.75, 0.4, 1.0), (2.0, 0.4, 1.0)], 2, "steel")                  # pyrolysis gasoline, east
QT.pipe([(0.25, 0.4, 1.0), (0.0, 0.4, 1.0)], 2, "steel")                  # fuel oil, west
QT.collide((0.25, 0.0, 0.25), (1.75, 11.6, 1.75))

# ---- the cracked gas compressor ---------------------------------------------------------------------------------------------------
CG = model("cracked_gas_compressor", 4, 4, 8, "grey")
rails(CG, 4, 8)
CG.cylinder("y", (1.0, 0.8), 0.35, 0.25, 2.4, "padgreen", 14)             # the suction drum
CG.pipe([(1.0, 0.5, 0.0), (1.0, 0.5, 0.45)], 6, "steel")
CG.box((1.2, 0.25, 1.3), (2.8, 1.5, 3.2), "grey")                         # the compressor casing
CG.cylinder("x", (1.0, 2.2), 0.35, 0.3, 1.2, "iron", 14)
CG.cylinder("x", (1.0, 2.2), 0.3, 2.8, 3.7, "iron", 14)
CG.ring("z", (2.0, 1.0), 0.32, 3.3, 3.8, 0.03, "engine", sides=12, skip=(1, 2, 4, 5, 7, 8, 10, 11))
CG.box((1.45, 0.25, 3.85), (2.55, 0.4, 5.95), "iron")
CG.cylinder("z", (2.0, 1.0), 0.6, 3.85, 5.95, "blue", 16)                  # the 300 hp motor
CG.fins((1.45, 0.45, 4.0), (2.55, 1.55, 5.8), "z", 10, 0.02, "blue")
CG.box((0.05, 0.4, 4.15), (0.6, 1.25, 4.85), "grey")                      # its 4.16 kV box, west
CG.decal((0.04, 0.6, 4.25), (0.05, 1.1, 4.75), "danger", "west", "grey")
CG.box((1.9, 1.0, 5.95), (2.1, 2.0, 6.85), "engine")
CG.ring("z", (2.0, 2.0), 1.4, 6.9, 7.5, 0.04, "grey", sides=24)           # the cooler fan's shroud
CG.box((0.2, 0.25, 7.55), (3.8, 3.8, 7.95), "finned")                       # the cooler
CG.fins((0.3, 0.35, 7.58), (3.7, 3.7, 7.92), "x", 18, 0.03, "galv")
CG.pipe([(3.0, 0.5, 7.0), (3.0, 0.5, 8.0)], 4, "steel")                   # discharge, the back
CG.collide((0.1, 0.0, 0.05), (3.9, 0.25, 7.95))
CG.collide((0.6, 0.25, 0.4), (3.85, 2.6, 6.0))
CG.collide((0.2, 0.25, 7.0), (3.8, 3.8, 7.95))

# ---- the cold box -----------------------------------------------------------------------------------------------------------------
CB = model("cold_box", 3, 10, 3, "lagging")
CB.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
CB.box((0.1, 0.3, 0.1), (2.9, 7.0, 2.9), "lagging")                       # the insulated box
for y in (1.5, 3.0, 4.5, 6.0):
	CB.box((0.08, y - 0.04, 0.08), (2.92, y + 0.04, 2.92), "grey")          # its stiffeners
CB.cylinder("y", (0.9, 0.9), 0.4, 7.0, 9.7, "lagging", 14)                 # the demethanizer
CB.cylinder("y", (2.1, 2.1), 0.45, 7.0, 9.9, "lagging", 14)                # the C2 splitter
CB.pipe([(1.5, 0.5, 0.0), (1.5, 0.5, 0.1)], 4, "steel")                   # compressed cracked gas in, north
for y, nps in ((0.5, 2), (1.5, 3), (2.5, 4)):
	CB.pipe([(1.5, y, 2.9), (1.5, y, 3.0)], nps, "steel")                  # LPG, ethylene, fuel gas out, south
CB.collide((0.1, 0.0, 0.1), (2.9, 7.0, 2.9))

# ---- the polyethylene reactor -----------------------------------------------------------------------------------------------------
PR = model("polyethylene_reactor", 3, 16, 3, "steel")
PR.box((0.0, 0.0, 0.0), (3.0, 0.25, 3.0), "concrete")
PR.cylinder("y", (1.2, 1.5), 0.95, 0.25, 1.2, "iron", 16)                  # the skirt
PR.cylinder("y", (1.2, 1.5), 0.85, 1.2, 11.0, "steel", 16)                 # the fluidized bed
PR.cylinder("y", (1.2, 1.5), 1.15, 11.0, 14.5, "steel", 18)                # the expanded top
PR.cylinder("y", (1.2, 1.5), 0.7, 14.5, 15.2, "steel", 14)
for y in (5.0, 10.5, 14.4):
	PR.ring("y", (1.2, 1.5), 1.3, y, y + 0.06, 0.25, "checker", sides=16)
PR.pipe([(1.2, 15.2, 1.5), (1.2, 15.6, 1.5), (2.55, 15.6, 1.5), (2.55, 1.2, 1.5)], 8, "steel")   # the cycle gas loop down the east side
PR.cylinder("z", (2.55, 0.55), 0.3, 0.1, 1.2, "blue", 12)                  # the cycle gas compressor's motor
PR.cylinder("z", (2.55, 0.55), 0.25, 1.8, 2.8, "padgreen", 12)             # the compressor
PR.box((0.05, 0.3, 0.1), (0.35, 0.8, 0.5), "grey")                        # its 480 V box, west
PR.pipe([(0.0, 0.5, 1.5), (0.35, 0.5, 1.5)], 4, "steel")                  # ethylene in, west
PR.collide((0.25, 0.0, 0.55), (2.15, 15.2, 2.45))

# ---- the pelletizer ---------------------------------------------------------------------------------------------------------------
PE = model("pelletizer", 2, 3, 6, "blue")
PE.box((0.05, 0.0, 0.05), (1.95, 0.2, 5.95), "iron")
PE.box((0.05, 0.2, 0.05), (0.95, 1.3, 1.4), "galv")                       # the sheet bin
PE.box((0.1, 1.3, 0.1), (0.9, 1.35, 1.35), "dark")
PE.box((0.0, 1.5, 0.3), (0.05, 1.9, 0.7), "grey")                         # its 4.16 kV box, west
PE.cylinder("z", (1.5, 0.9), 0.4, 0.2, 1.4, "blue", 16)                   # the 250 hp motor
PE.ring("z", (1.5, 0.9), 0.2, 1.4, 1.8, 0.02, "engine", sides=12, skip=(1, 2, 4, 5, 7, 8, 10, 11))
PE.box((1.1, 0.2, 1.8), (1.9, 1.4, 2.6), "grey")                          # the gearbox
PE.cylinder("z", (1.5, 0.9), 0.22, 2.6, 5.0, "steel", 12)                 # the extruder barrel
for z in (3.0, 3.6, 4.2, 4.8):
	PE.ring("z", (1.5, 0.9), 0.25, z - 0.08, z + 0.08, 0.03, "copper", sides=12)   # its heater bands
PE.box((1.1, 0.4, 5.0), (1.9, 1.5, 5.8), "grey")                          # the die and cutter
PE.box((0.1, 1.6, 2.6), (0.9, 2.4, 3.4), "padgreen")                      # the feed hopper
PE.collide((0.05, 0.0, 0.05), (1.95, 1.5, 5.95))


M = CF
EXTRA = [QT, CG, CB, PR, PE]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
