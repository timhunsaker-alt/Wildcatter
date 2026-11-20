"""Phase 23 at true size (docs/multiblocks/oilfield-follow-ons.md), each drawn facing north from its own main cell (the footprints match
OilfieldPartBlock's), in the mod's existing materials. Motor terminal boxes are on the west face of the main cell (UnitMotorBlockEntity):
- calcium_chloride_unit, 2 x 4 x 3: the rubber-lined reactor with its agitator and the calcite feed chute (a hopper on top), the acid
  inlet on the north of its front bottom cells, the steam evaporator and its condenser leg, the flaker drum and the flake chute at the
  back, process steam in on the east of the bottom cells, the CO2 vent on top."""
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

M = CC
EXTRA = []
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
