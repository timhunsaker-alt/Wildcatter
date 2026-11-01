"""The gas plant at true size (docs/multiblocks/gas-plant.md), each part drawn facing north from its own main cell (the footprints
match GasPlantPartBlock's):
- gas_compressor, 4 x 4 x 9: a two-stage reciprocating package: the suction scrubber at the front, the frame and its two cylinders,
  the coupling (it turns), the 400 hp 4.16 kV motor with its terminal box on the west side, the interstage scrubber, and the cooler
  at the back with its fan (it turns) driven off the motor; suction in the front, discharge out the back;
- teg_dehydrator, 3 x 8 x 5: the glycol contactor tower with its ladder, the reboiler with its firetube stack and still column,
  the glycol pump; wet gas in the front, dry gas out the back;
- ngl_chiller, 3 x 3 x 7: the gas-gas exchanger, the insulated propane chiller and cold separator, refrigerant lines on the east
  side; gas in the front, pipeline gas (upper) and NGL (lower) out the back;
- refrigeration_compressor, 3 x 3 x 6: the 150 hp motor, the coupling (it turns), the screw compressor, the oil separator and the
  control panel;
- stabilizer, 2 x 12 x 2: the debutanizer column with its platforms and ladder, the overhead line down to the reflux drum;
- lpg_bullet, 3 x 3 x 12: the 18,000 gallon pressure tank on its saddles with its relief valves and gauges.
Turning parts: the gas compressor's coupling and cooler fan, the refrigeration compressor's coupling."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def rails(M, w, d, top=0.25):
	"""A skid: two channels along z and cross members."""
	for x in (0.15, w - 0.35):
		M.box((x, 0.0, 0.05), (x + 0.2, top, d - 0.05), "iron")
	z = 0.4
	while z < d - 0.3:
		M.box((0.35, 0.02, z), (w - 0.35, top - 0.04, z + 0.12), "iron")
		z += 1.2


# ---- the gas compressor -----------------------------------------------------------------------------------------------------------
GC = model("gas_compressor", 4, 4, 9)
rails(GC, 4, 9)
GC.cylinder("y", (1.0, 0.8), 0.35, 0.25, 2.6, "padgreen", 14)              # the suction scrubber
GC.cylinder("y", (1.0, 0.8), 0.25, 2.6, 2.75, "padgreen", 12)
GC.pipe([(1.0, 0.5, 0.0), (1.0, 0.5, 0.45)], 6, "steel")                    # the suction, front end
g.pipe_label(GC, (1.0, 0.5, 0.25), "z", 6, "pipe_gas", 0.3)
GC.box((1.2, 0.25, 1.3), (2.8, 1.45, 3.2), "grey")                          # the frame (crankcase)
GC.box((1.15, 1.45, 1.25), (2.85, 1.55, 3.25), "iron")
for x1, x2, r in ((0.3, 1.2, 0.4), (2.8, 3.7, 0.3)):                        # the first- and second-stage cylinders across the frame
	GC.cylinder("x", (1.0, 2.25), r, x1, x2, "iron", 14)
	GC.cylinder("y", ((x1 + x2) / 2, 2.25), 0.1, 1.4, 1.9, "steel", 8)      # their valves' bottles
GC.pipe([(1.0, 2.7, 0.8), (1.0, 2.7, 2.25), (0.75, 2.7, 2.25)], 4, "steel")  # scrubber to the first stage
GC.ring("z", (2.0, 1.0), 0.32, 3.3, 3.8, 0.03, "engine", sides=12, skip=(1, 2, 4, 5, 7, 8, 10, 11))   # the coupling's open guard
GC.box((1.45, 0.25, 3.85), (2.55, 0.4, 5.95), "iron")                      # the motor's base
GC.cylinder("z", (2.0, 1.0), 0.6, 3.85, 5.95, "blue", 16)                   # the 400 hp, 4.16 kV motor
GC.fins((1.45, 0.45, 4.0), (2.55, 1.55, 5.8), "z", 10, 0.02, "blue")
GC.box((0.05, 0.4, 4.15), (0.6, 1.25, 4.85), "grey")                       # its MV terminal box on the west side
GC.decal((0.04, 0.6, 4.25), (0.05, 1.1, 4.75), "danger", "west", "grey")
GC.pipe([(0.6, 0.8, 4.5), (1.4, 0.8, 4.5)], 3, "galv")                     # the conduit into the motor
GC.cylinder("y", (3.3, 4.6), 0.3, 0.25, 2.5, "padgreen", 14)               # the interstage scrubber
GC.box((1.9, 1.0, 5.95), (2.1, 2.0, 7.0), "engine")                        # the fan drive's guard, motor to fan
GC.box((0.2, 0.25, 7.7), (3.8, 3.8, 8.4), "finned")                        # the cooler: its frame and core
GC.fins((0.3, 0.35, 7.75), (3.7, 3.7, 8.35), "x", 18, 0.03, "galv")
GC.ring("z", (2.0, 2.0), 1.4, 7.1, 7.7, 0.04, "grey", sides=24)            # the fan shroud
GC.pipe([(3.7, 2.25, 3.0), (3.7, 2.25, 7.6)], 4, "steel")                  # to and from the cooler
GC.pipe([(1.0, 0.5, 8.4), (1.0, 0.5, 9.0)], 4, "steel")                    # the discharge, back end
GC.decal((2.56, 0.6, 4.6), (2.57, 0.9, 5.2), "nameplate", "east", "blue")
GC.decal((2.8, 0.5, 1.6), (2.81, 1.0, 2.4), "flammable", "east", "grey")
GC.collide((0.15, 0.0, 0.05), (3.85, 0.25, 8.95))
GC.collide((0.6, 0.25, 0.4), (3.85, 2.7, 6.0))
GC.collide((0.2, 0.25, 7.0), (3.8, 3.8, 8.4))

# ---- the TEG dehydrator -----------------------------------------------------------------------------------------------------------
TD = model("teg_dehydrator", 3, 8, 5, "padgreen")
rails(TD, 3, 5)
TD.cylinder("y", (0.7, 0.7), 0.32, 0.25, 7.6, "padgreen", 14)               # the contactor
TD.cylinder("y", (0.7, 0.7), 0.22, 7.6, 7.75, "padgreen", 12)
TD.ladder(0.7, 0.33, 0.25, 7.5, -1, "galv")
TD.box((0.2, 5.0, 0.2), (1.5, 5.06, 1.4), "grating")                       # its platform
TD.handrail([(0.25, 1.35), (1.45, 1.35), (1.45, 0.25)], 5.06, "galv", height=0.9)
TD.pipe([(0.7, 0.5, 0.0), (0.7, 0.5, 0.4)], 4, "steel")                    # wet gas in, front
TD.pipe([(0.7, 7.5, 0.95), (0.7, 7.5, 1.3), (0.7, 0.5, 1.3), (0.7, 0.5, 5.0)], 4, "steel")   # dry gas off the top, out the back
TD.cylinder("z", (2.1, 0.95), 0.45, 1.4, 4.6, "grey", 16)                   # the reboiler
TD.cylinder("y", (2.1, 4.3), 0.12, 1.3, 4.6, "iron", 10)                    # its firetube stack
TD.cylinder("y", (2.1, 2.2), 0.15, 1.35, 3.6, "steel", 10)                  # the still column
TD.box((1.0, 0.25, 2.5), (1.5, 0.75, 3.2), "dark")                         # the glycol pump (Kimray type)
TD.cylinder("z", (2.1, 0.45), 0.2, 1.5, 4.5, "padgreen", 12)                # the surge tank under the reboiler
TD.pipe([(1.0, 0.6, 2.85), (0.95, 0.6, 2.85), (0.95, 3.5, 2.85), (0.95, 3.5, 0.95)], 1, "steel")   # lean glycol up to the contactor
TD.decal((2.56, 0.8, 2.2), (2.57, 1.1, 2.8), "nameplate", "east", "grey")
TD.collide((0.15, 0.0, 0.05), (2.85, 1.4, 4.95))
TD.collide((0.38, 1.4, 0.38), (1.02, 7.75, 1.02))

# ---- the NGL chiller --------------------------------------------------------------------------------------------------------------
CH = model("ngl_chiller", 3, 3, 7)
rails(CH, 3, 7)
CH.cylinder("z", (0.8, 1.0), 0.35, 0.4, 4.2, "grey", 14)                    # the gas-gas exchanger
CH.cylinder("z", (2.1, 1.15), 0.55, 1.4, 5.2, "lagging", 16)                # the propane chiller (kettle), insulated
CH.cylinder("y", (1.5, 6.0), 0.4, 0.25, 2.6, "lagging", 14)                 # the cold separator, insulated
CH.cylinder("y", (1.5, 6.0), 0.3, 2.6, 2.75, "lagging", 12)
CH.pipe([(0.8, 0.5, 0.0), (0.8, 0.5, 0.3), (0.8, 1.0, 0.3)], 4, "steel")   # gas in, front
CH.pipe([(0.8, 1.0, 4.2), (0.8, 1.0, 4.6), (2.1, 1.0, 4.6)], 4, "lagging")   # exchanger to the chiller
CH.pipe([(2.1, 1.15, 5.2), (2.1, 1.15, 5.6), (1.5, 1.15, 5.6)], 4, "lagging")   # chiller to the cold separator
CH.pipe([(1.5, 2.6, 6.0), (1.5, 2.85, 6.0), (0.4, 2.85, 6.0), (0.4, 1.5, 6.0), (0.4, 1.5, 7.0)], 4, "steel")   # pipeline gas, back upper
g.pipe_label(CH, (0.4, 1.5, 6.6), "z", 4, "pipe_gas", 0.4)
CH.pipe([(1.5, 0.45, 6.4), (1.5, 0.45, 7.0)], 2, "steel")                  # raw NGL, back lower
CH.pipe([(2.6, 1.7, 3.0), (3.0, 1.7, 3.0)], 3, "lagging")                  # the refrigerant lines to the east side
CH.pipe([(2.6, 0.7, 3.6), (3.0, 0.7, 3.6)], 2, "steel")
CH.cylinder("z", (2.3, 2.0), 0.05, 2.0, 2.04, "brass", 12)                 # a temperature gauge
CH.decal((2.25, 1.95, 1.995), (2.35, 2.05, 2.0), "gauge", "north", "brass")
CH.collide((0.15, 0.0, 0.05), (2.85, 1.7, 6.95))

# ---- the refrigeration compressor -------------------------------------------------------------------------------------------------
RC = model("refrigeration_compressor", 3, 3, 6, "blue")
rails(RC, 3, 6)
RC.box((0.05, 0.4, 0.3), (0.45, 1.1, 0.8), "grey")                         # the terminal box, west side of the main cell
RC.decal((0.04, 0.5, 0.35), (0.05, 1.0, 0.75), "danger", "west", "grey")
RC.cylinder("z", (1.5, 1.0), 0.45, 0.3, 2.3, "blue", 16)                    # the 150 hp motor
RC.fins((1.1, 0.6, 0.4), (1.9, 1.4, 2.2), "z", 8, 0.02, "blue")
RC.ring("z", (1.5, 1.0), 0.22, 2.35, 2.85, 0.02, "engine", sides=12, skip=(1, 2, 4, 5, 7, 8, 10, 11))   # the coupling guard
RC.box((1.0, 0.4, 2.85), (2.0, 1.5, 4.0), "grey")                          # the screw compressor
RC.cylinder("z", (1.5, 2.1), 0.4, 2.6, 5.7, "padgreen", 16)                 # the oil separator over it
RC.box((0.15, 0.25, 4.4), (0.7, 2.0, 5.6), "grey")                         # the control panel
RC.decal((0.71, 1.2, 4.6), (0.72, 1.8, 5.4), "controller", "east", "grey")
RC.pipe([(2.0, 1.7, 3.0), (3.0, 1.7, 3.0)], 3, "lagging")                  # the refrigerant suction, out of its east side
RC.collide((0.15, 0.0, 0.05), (2.85, 2.5, 5.95))

# ---- the stabilizer ---------------------------------------------------------------------------------------------------------------
ST = model("stabilizer", 2, 12, 2)
ST.box((0.1, 0.0, 0.1), (1.9, 0.3, 1.9), "concrete")                      # the foundation
ST.cylinder("y", (1.0, 1.0), 0.5, 0.3, 0.9, "iron", 16)                     # the skirt
ST.cylinder("y", (1.0, 1.0), 0.45, 0.9, 11.6, "padgreen", 16)               # the column
ST.cylinder("y", (1.0, 1.0), 0.32, 11.6, 11.75, "padgreen", 12)
for y in (4.0, 8.0, 11.3):
	ST.ring("y", (1.0, 1.0), 0.75, y, y + 0.06, 0.25, "grating", sides=16)  # the platforms
	ST.ring("y", (1.0, 1.0), 0.95, y + 1.0, y + 1.05, 0.03, "galv", sides=16)
ST.ladder(0.4, 0.05, 0.3, 11.4, -1, "galv")
ST.pipe([(1.0, 11.75, 1.0), (1.0, 11.9, 1.0), (1.8, 11.9, 1.0), (1.8, 1.6, 1.0)], 3, "steel")   # the overhead line down to the condenser
ST.cylinder("z", (1.75, 1.3), 0.22, 0.2, 1.8, "grey", 12)                  # the reflux drum
ST.pipe([(1.0, 0.6, 0.0), (1.0, 0.6, 0.55)], 2, "steel")                   # raw NGL in, north
ST.pipe([(1.75, 0.5, 1.4), (2.0, 0.5, 1.4)], 2, "steel")                   # LPG out, east
ST.pipe([(0.6, 0.4, 1.4), (0.6, 0.4, 2.0)], 2, "steel")                    # natural gasoline out, south
ST.collide((0.1, 0.0, 0.1), (1.9, 0.9, 1.9))
ST.collide((0.55, 0.9, 0.55), (1.45, 11.75, 1.45))

# ---- the LPG bullet ---------------------------------------------------------------------------------------------------------------
LB = model("lpg_bullet", 3, 3, 12, "steel")
for z in (1.5, 10.0):                                                       # the saddles on their piers
	LB.box((0.4, 0.0, z), (2.6, 0.35, z + 0.6), "concrete")
	LB.box((0.5, 0.35, z + 0.1), (2.5, 0.9, z + 0.5), "iron")
LB.cylinder("z", (1.5, 1.6), 1.4, 0.8, 11.2, "steel", 24)                   # the shell, 10 ft across (drawn 2.8 m to stay inside its cells)
LB.cylinder("z", (1.5, 1.6), 1.1, 0.55, 0.8, "steel", 20)                   # the heads
LB.cylinder("z", (1.5, 1.6), 1.1, 11.2, 11.45, "steel", 20)
for z in (3.0, 9.0):                                                        # the relief valves on top
	LB.cylinder("y", (1.5, z), 0.07, 3.0, 3.45, "brass", 8)
	LB.box((1.4, 3.45, z - 0.1), (1.6, 3.55, z + 0.1), "iron")
LB.cylinder("z", (2.2, 2.5), 0.07, 0.62, 0.66, "brass", 12)                # a pressure gauge on the front head
LB.decal((2.13, 2.43, 0.615), (2.27, 2.57, 0.62), "gauge", "north", "brass")
LB.decal((1.0, 1.6, 0.53), (2.0, 2.1, 0.55), "danger", "north", "steel")
LB.decal((0.45, 1.6, 0.53), (0.95, 2.1, 0.55), "nfpa", "north", "steel")
for x, z in ((0.0, 6.0), (3.0, 6.0)):                                       # the bottom connections on both sides
	LB.pipe([(1.5, 0.4, z), (x, 0.4, z)], 3, "steel")
LB.collide((0.1, 0.0, 0.55), (2.9, 3.0, 11.45))

# ---- turning parts ----------------------------------------------------------------------------------------------------------------
def rotor(name):
	return mlib.Model("rotor_" + name, [(0, 0, 0)], dict(TEX), TEX["iron"])


GCC = rotor("gas_compressor_coupling")
GCC.cylinder("z", (0.5, 0.5), 0.09, 0.25, 0.75, "steel", 10)
GCC.cylinder("z", (0.5, 0.5), 0.22, 0.38, 0.48, "iron", 16)
GCC.cylinder("z", (0.5, 0.5), 0.22, 0.52, 0.62, "iron", 16)
GCC.cylinder("z", (0.5, 0.5), 0.14, 0.48, 0.52, "rubber", 12)
GCC.box((0.46, 0.7, 0.38), (0.54, 0.74, 0.62), "brass")
FAN = rotor("gas_compressor_fan")
FAN.fan("z", (0.5, 0.5), 1.3, 0.5, 0.05, "dark", blades=8, width=0.32, hub=0.18, hub_tex="iron")
RCC = rotor("refrigeration_coupling")
RCC.cylinder("z", (0.5, 0.5), 0.07, 0.3, 0.7, "steel", 10)
RCC.cylinder("z", (0.5, 0.5), 0.17, 0.42, 0.5, "iron", 14)
RCC.cylinder("z", (0.5, 0.5), 0.17, 0.52, 0.6, "iron", 14)
RCC.cylinder("z", (0.5, 0.5), 0.11, 0.5, 0.52, "rubber", 12)
RCC.box((0.47, 0.64, 0.42), (0.53, 0.68, 0.6), "brass")

M = GC
for part in (GCC, FAN, RCC):
	M.rotors[part.name[len("rotor_"):]] = part
EXTRA = [TD, CH, RC, ST, LB]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
