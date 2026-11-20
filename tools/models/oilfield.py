"""Phase 23 at true size (docs/multiblocks/oilfield-follow-ons.md), each drawn facing north from its own main cell (the footprints match
OilfieldPartBlock's), in the mod's existing materials. Motor terminal boxes are on the west face of the main cell (UnitMotorBlockEntity):
- calcium_chloride_unit, 2 x 4 x 3: the rubber-lined reactor with its agitator and the calcite feed chute (a hopper on top), the acid
  inlet on the north of its front bottom cells, the steam evaporator and its condenser leg, the flaker drum and the flake chute at the
  back, process steam in on the east of the bottom cells, the CO2 vent on top.
- choke_manifold, 2 x 2 x 4: a skid of gate valves on a cross, the hydraulic adjustable choke and the manual choke with its handwheel,
  the gauges, the buffer chamber; the choke line in at its north end, the line to the separator on its east;
- remote_choke_panel, 1 x 2 x 1: the console on its pedestal, the gauges and the choke lever, the hydraulic pump below;
- mud_gas_separator, 1 x 6 x 1: the vertical vessel on its legs, the inlet from the manifold low on its west, the liquid seal's
  U-bend under it, the vent line off its top.
- storage_compressor, 3 x 3 x 6: the medium-voltage motor (its box on the west of the main cell), the frame with two stages of
  cylinders and their pulsation bottles, the scrubber and the aftercooler; gas in at the north of the front, out at the south of the back;
- glycol_dehydrator, 2 x 6 x 3: the TEG contactor tower, the still column and its reboiler firebox (fuel gas on the east), the surge
  tank and pumps; wet gas in at the north, dry gas out at the south;
- storage_wellhead (one cell): the casing head, the master and wing valves, the subsurface safety valve's control line, the brine
  wing out of the side it faces."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.3, z=0.2):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


# ---- 23a: the calcium chloride unit ---------------------------------------------------------------------------------------------------
CC = model("calcium_chloride_unit", 2, 4, 3, "polymer")
CC.box((0.0, 0.0, 0.0), (2.0, 0.2, 3.0), "concrete")
CC.cylinder("y", (1.0, 0.85), 0.7, 0.2, 2.2, "polymer", 16)                       # the rubber-lined reactor
CC.ring("y", (1.0, 0.85), 0.72, 1.2, 1.3, 0.04, "galv", sides=16)
CC.cylinder("y", (1.0, 0.85), 0.4, 2.2, 2.4, "polymer", 12)
CC.box((0.75, 2.4, 0.6), (1.25, 2.75, 1.1), "engine")                              # the agitator drive
CC.box((0.2, 2.2, 0.15), (0.75, 3.95, 0.7), "galv")                               # the calcite feed chute, under a hopper on top
CC.pipe([(1.0, 0.4, 0.0), (1.0, 0.4, 0.3)], 3, "polymer")                         # the acid in, north of the front
CC.decal((1.2, 0.3, -0.01), (1.6, 0.7, 0.0), "corrosive", "north", "polymer") if "corrosive" in TEX else None
CC.pipe([(1.0, 2.6, 0.85), (1.0, 3.7, 0.85)], 4, "steel")                         # the CO2 vent
CC.cylinder("y", (1.0, 2.0), 0.45, 0.2, 3.2, "steel", 14)                          # the evaporator body
CC.cylinder("y", (1.0, 2.0), 0.25, 3.2, 3.6, "steel", 10)                          # its vapour head
CC.pipe([(1.45, 3.4, 2.0), (1.8, 3.4, 2.0), (1.8, 0.6, 2.0)], 3, "steel")         # the condenser leg down the east side
CC.pipe([(2.0, 0.45, 2.1), (1.55, 0.45, 2.1)], 3, "steel")                         # process steam in, the east of the bottom cells
CC.cylinder("x", (0.75, 2.65), 0.35, 0.3, 1.7, "steel", 14)                       # the flaker drum
CC.box((0.4, 0.2, 2.35), (1.6, 0.4, 3.0), "iron")                                 # its frame and the flake chute
CC.box((0.8, 0.2, 2.8), (1.2, 0.6, 3.0), "galv")
box480(CC)
CC.decal((1.99, 1.2, 0.3), (2.0, 1.7, 0.9), "nameplate", "east", "polymer")
CC.collide((0.0, 0.0, 0.0), (2.0, 3.6, 3.0))

# ---- 23b: the choke manifold ---------------------------------------------------------------------------------------------------------
CM = model("choke_manifold", 2, 2, 4, "steel")
CM.box((0.0, 0.0, 0.0), (2.0, 0.15, 4.0), "iron")                                  # the skid
CM.pipe([(1.0, 0.6, 0.0), (1.0, 0.6, 3.6)], 4, "steel")                           # the choke line in at the north, down the middle
CM.pipe([(0.4, 0.6, 1.2), (1.6, 0.6, 1.2)], 3, "steel")                           # the cross
for z in (0.6, 2.2, 3.2):
	CM.box((0.75, 0.3, z - 0.2), (1.25, 0.95, z + 0.2), "blue")                    # gate valves
	CM.cylinder("y", (1.0, z), 0.04, 0.95, 1.3, "steel", 6)
	CM.cylinder("y", (1.0, z), 0.16, 1.3, 1.34, "red" if "red" in TEX else "engine", 10)   # handwheels
CM.box((0.2, 0.25, 1.6), (0.7, 1.1, 2.4), "blue")                                  # the hydraulic adjustable choke, its actuator
CM.cylinder("x", (0.7, 2.0), 0.18, 0.05, 0.25, "engine", 10)
CM.box((1.3, 0.25, 1.6), (1.8, 0.9, 2.4), "steel")                                 # the manual choke
CM.cylinder("x", (0.6, 2.0), 0.2, 1.8, 1.86, "red" if "red" in TEX else "engine", 12)
CM.box((0.3, 0.2, 3.5), (1.7, 1.2, 3.95), "steel")                                 # the buffer chamber
CM.pipe([(1.7, 0.7, 3.7), (2.0, 0.7, 3.7)], 4, "steel")                           # out to the separator, east
CM.decal((0.3, 1.2, 1.0), (0.6, 1.5, 1.01), "gauge", "south", "steel")
CM.decal((1.4, 1.2, 1.0), (1.7, 1.5, 1.01), "gauge", "south", "steel")
CM.box((0.4, 1.0, 0.95), (1.7, 1.2, 1.0), "steel")
CM.collide((0.0, 0.0, 0.0), (2.0, 1.4, 4.0))

# ---- 23b: the remote choke panel -----------------------------------------------------------------------------------------------------
CP = model("remote_choke_panel", 1, 2, 1, "grey")
CP.box((0.1, 0.0, 0.1), (0.9, 0.6, 0.9), "engine")                                 # the hydraulic pump and its tank
CP.box((0.2, 0.6, 0.3), (0.8, 1.0, 0.7), "grey")                                   # the pedestal
CP.box((0.05, 1.0, 0.15), (0.95, 1.75, 0.85), "grey")                              # the console
CP.decal((0.15, 1.3, 0.14), (0.45, 1.6, 0.15), "gauge", "north", "grey")
CP.decal((0.55, 1.3, 0.14), (0.85, 1.6, 0.15), "gauge", "north", "grey")
CP.box((0.42, 1.05, 0.05), (0.58, 1.25, 0.15), "red" if "red" in TEX else "engine")   # the choke lever
CP.box((0.1, 1.75, 0.2), (0.9, 1.85, 0.8), "grey")
CP.collide((0.05, 0.0, 0.1), (0.95, 1.85, 0.9))

# ---- 23b: the mud-gas separator ------------------------------------------------------------------------------------------------------
MG = model("mud_gas_separator", 1, 6, 1, "hipaint")
for dx, dz in ((0.15, 0.15), (0.85, 0.15), (0.15, 0.85), (0.85, 0.85)):
	MG.box((dx - 0.05, 0.0, dz - 0.05), (dx + 0.05, 0.9, dz + 0.05), "iron")      # its legs
MG.cylinder("y", (0.5, 0.5), 0.45, 0.9, 5.4, "hipaint", 16)                         # the vessel, 0.9 m across
MG.cylinder("y", (0.5, 0.5), 0.3, 5.4, 5.6, "hipaint", 12)
MG.pipe([(0.0, 1.6, 0.5), (0.1, 1.6, 0.5)], 4, "steel")                            # the inlet from the manifold, west
MG.pipe([(0.5, 0.9, 0.5), (0.5, 0.3, 0.5), (0.9, 0.3, 0.5), (0.9, 0.0, 0.5)], 4, "steel")   # the liquid seal's leg to the tanks
MG.pipe([(0.5, 5.6, 0.5), (0.5, 6.0, 0.5)], 4, "steel")                            # the vent line off its top
MG.ladder(0.95, 0.5, 0.9, 5.4, 1, "galv") if hasattr(MG, "ladder") else None
MG.collide((0.05, 0.0, 0.05), (0.95, 6.0, 0.95))

# ---- 23c: the storage compressor ---------------------------------------------------------------------------------------------------
SC = model("storage_compressor", 3, 3, 6, "engine")
SC.box((0.0, 0.0, 0.0), (3.0, 0.2, 6.0), "concrete")
SC.box((0.3, 0.2, 0.2), (1.5, 1.4, 1.8), "engine")                                # the motor
SC.box((0.0, 0.4, 0.3), (0.4, 1.2, 1.0), "grey")                                  # its medium-voltage box, west
SC.decal((-0.01, 0.55, 0.4), (0.0, 1.05, 0.9), "arcflash", "west", "grey")
SC.box((0.4, 0.2, 1.8), (2.6, 1.0, 4.8), "iron")                                  # the frame and crankcase
for z in (2.3, 3.3, 4.3):
	SC.cylinder("x", (1.2, z), 0.32, 0.0, 0.45, "steel", 12)                       # the cylinders, both sides
	SC.cylinder("x", (1.2, z), 0.32, 2.55, 3.0, "steel", 12)
SC.cylinder("z", (2.3, 2.2), 0.35, 1.8, 4.8, "steel", 12)                         # the pulsation bottles
SC.cylinder("y", (0.8, 5.3), 0.4, 0.2, 2.6, "steel", 12)                          # the inlet scrubber
SC.box((1.6, 1.0, 4.9), (2.9, 2.6, 5.9), "galv")                                   # the aftercooler
SC.pipe([(1.5, 0.5, 0.0), (1.5, 0.5, 0.2)], 4, "steel")                           # gas in, north
SC.pipe([(1.5, 0.5, 5.8), (1.5, 0.5, 6.0)], 4, "steel")                           # gas out, south
SC.collide((0.0, 0.0, 0.0), (3.0, 2.6, 6.0))

# ---- 23c: the glycol dehydrator ------------------------------------------------------------------------------------------------------
GD = model("glycol_dehydrator", 2, 6, 3, "steel")
GD.box((0.0, 0.0, 0.0), (2.0, 0.2, 3.0), "concrete")
GD.cylinder("y", (0.6, 0.7), 0.45, 0.2, 5.8, "steel", 14)                         # the TEG contactor
GD.cylinder("y", (1.5, 2.2), 0.18, 1.6, 4.0, "steel", 10)                         # the still column
GD.box((1.0, 0.2, 1.7), (2.0, 1.6, 2.8), "hipaint")                               # the reboiler and its firebox
GD.cylinder("y", (1.7, 2.6), 0.08, 1.6, 3.2, "hipaint", 8)                        # its stack
GD.cylinder("x", (0.6, 2.2), 0.3, 0.1, 0.9, "steel", 12)                          # the surge tank
GD.box((0.1, 0.2, 1.3), (0.9, 0.6, 1.7), "blue")                                  # the glycol pumps
GD.box((0.0, 0.3, 0.1), (0.3, 0.9, 0.6), "grey")                                  # the 480 V box, west
GD.decal((-0.01, 0.4, 0.2), (0.0, 0.8, 0.5), "danger", "west", "grey")
GD.pipe([(0.6, 0.5, 0.0), (0.6, 0.5, 0.25)], 4, "steel")                          # wet gas in, north
GD.pipe([(0.6, 0.5, 2.75), (0.6, 0.5, 3.0)], 4, "steel")                          # dry gas out, south
GD.pipe([(2.0, 0.4, 2.2), (1.9, 0.4, 2.2)], 2, "steel")                           # fuel gas, east
GD.ladder(1.05, 0.7, 0.2, 5.8, 1, "galv") if hasattr(GD, "ladder") else None
GD.collide((0.0, 0.0, 0.0), (2.0, 5.8, 3.0))

# ---- 23c: the storage wellhead (one cell) -----------------------------------------------------------------------------------------
SW = mlib.Model("storage_wellhead", [(0, 0, 0)], dict(TEX), TEX["steel"])
SW.cylinder("y", (0.5, 0.5), 0.14, -0.2, 0.05, "steel", 12, caps=False)
SW.cylinder("y", (0.5, 0.5), 0.18, 0.05, 0.35, "steel", 14)                       # the casing head
SW.box((0.4, 0.35, 0.4), (0.6, 0.8, 0.6), "red" if "red" in TEX else "engine")     # the master valves
SW.pipe([(0.5, 0.2, 0.4), (0.5, 0.2, 0.0)], 3, "steel")                           # the brine wing, the side it faces (north)
SW.cylinder("y", (0.5, 0.5), 0.1, 0.8, 0.95, "steel", 10)
SW.pipe([(0.6, 0.6, 0.5), (1.0, 0.6, 0.5)], 3, "steel")                           # the gas wing
SW.pipe([(0.4, 0.5, 0.5), (0.0, 0.5, 0.5)], 3, "steel")
SW.box((0.42, 0.95, 0.42), (0.58, 1.0, 0.58), "brass" if "brass" in TEX else "steel")   # the safety valve's control line fitting
SW.collide((0.3, 0.0, 0.3), (0.7, 1.0, 0.7))

M = CC
EXTRA = [CM, CP, MG, SC, GD]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
SW.export_args = {"blockstate": False, "item": True}
EXTRA.append(SW)


def after():
	import gearlib
	for name in ("storage_wellhead",):
		gearlib.save_blockstate(name, {f"facing={facing}": {"model": f"wildcatter:block/{name}/p0", **({"y": y} if y else {})} for facing, y in gearlib.ROT.items()})
