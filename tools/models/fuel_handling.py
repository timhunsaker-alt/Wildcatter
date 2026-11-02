"""Fuel handling at true size (docs/multiblocks/fuel-handling.md), each part drawn facing north from its own main cell (the footprints
match FuelHandlingPartBlock's):
- refuelling_machine, 4 x 4 x 4: the manipulator crane's bridge on its rails, the trolley and the mast reaching down;
- fuel_transfer_system, 2 x 2 x 6: the upender frame and the transfer tube with its carriage;
- spent_fuel_pool, 4 x 3 x 4: the pool's cooling pump, its exchanger and the cleanup skid at the pool's edge (the pool itself is dug
  in front of it and its racks drawn by SpentFuelPoolRenderer)."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


RM = model("refuelling_machine", 4, 4, 4, "blue")
for x in (0.2, 3.5):
	RM.box((x, 0.0, 0.0), (x + 0.3, 3.2, 4.0), "iron")                       # the bridge's legs on their rails
RM.box((0.0, 3.2, 0.2), (4.0, 3.7, 3.8), "blue")                            # the bridge
RM.box((1.2, 3.7, 1.0), (2.8, 3.95, 3.0), "grey")                           # the trolley
RM.cylinder("y", (2.0, 2.0), 0.18, 0.4, 3.2, "steel", 8)                     # the mast, down into the cavity
RM.box((1.7, 0.2, 1.7), (2.3, 0.4, 2.3), "steel")                           # its gripper
box480(RM, 1.0, 0.3)
RM.collide((0.0, 0.0, 0.0), (4.0, 3.95, 4.0))

FT = model("fuel_transfer_system", 2, 2, 6, "steel")
FT.box((0.1, 0.0, 0.1), (1.9, 1.6, 1.0), "blue")                            # the upender
FT.cylinder("z", (1.0, 0.8), 0.45, 1.0, 6.0, "steel", 12)                   # the transfer tube
FT.box((0.5, 0.0, 0.0), (1.5, 0.3, 6.0), "iron")                            # its supports
FT.collide((0.1, 0.0, 0.0), (1.9, 1.6, 6.0))

SP = model("spent_fuel_pool", 4, 3, 4, "concrete")
SP.box((0.0, 0.0, 0.0), (4.0, 0.2, 4.0), "concrete")
SP.cylinder("y", (1.0, 3.0), 0.45, 0.2, 1.4, "blue", 12)                    # the cooling pump
SP.cylinder("x", (2.0, 0.9), 0.5, 1.6, 3.8, "steel", 12)                    # its exchanger
SP.box((0.4, 0.2, 0.3), (1.6, 2.4, 1.5), "grey")                            # the cleanup skid's demineralizer
SP.box((0.0, 0.0, -0.01), (4.0, 0.6, 0.0), "galv")                          # the pool's curb, north
box480(SP, 0.5, 2.0)
SP.collide((0.0, 0.0, 0.0), (4.0, 2.4, 4.0))

M = RM
EXTRA = [FT, SP]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
