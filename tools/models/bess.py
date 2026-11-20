"""Phase 22b grid batteries at true size (docs/multiblocks/grid-batteries.md), each drawn facing north from its main cell:
- bess_container, 3 x 3 x 6: a 20 ft container (6.06 x 2.44 x 2.90 m), corrugated walls, its chiller on the south end, roof vent panels;
- bess_pcs, 3 x 3 x 3: the inverter skid's cabinets with filter louvres and DC entry;
- bess_mv_skid, 3 x 3 x 3: the oil-immersed step-up transformer with its radiators and the ring main unit; low side west, high side east;
- bess_plant_controller, 3 x 4 x 3: the control house with its door, air conditioner and antenna;
- bess_cabinet, 2 x 3 x 2: the commercial outdoor cabinet, its cable box on the west face of the main cell."""
import mlib
import plantlib as g

TEX = dict(g.TEX)
TEX["container"] = "block/mat/mat_container_corrugated"


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


# ---- the battery container ------------------------------------------------------------------------------------------------------
BC = model("bess_container", 3, 3, 6, "container")
BC.box((0.1, 0.0, 0.0), (2.9, 0.15, 6.0), "concrete")                           # its footings
BC.box((0.28, 0.15, 0.0), (2.72, 2.85, 5.6), "container")                        # the box
for x in (0.25, 2.65):
	for z in (0.0, 5.5):
		BC.box((x, 0.15, z), (x + 0.1, 2.9, z + 0.1), "grey")                   # corner castings
BC.box((0.6, 0.3, 5.6), (2.4, 2.5, 6.0), "louvre")                               # the chiller on its end
BC.box((0.6, 0.4, -0.02), (1.4, 2.5, 0.0), "grey")                               # the doors, north
BC.box((1.6, 0.4, -0.02), (2.4, 2.5, 0.0), "grey")
BC.decal((1.0, 1.6, -0.03), (1.3, 1.9, -0.02), "danger", "north", "grey")
BC.decal((1.7, 1.6, -0.03), (2.0, 1.9, -0.02), "flammable", "north", "grey")
for z in (1.0, 2.6, 4.2):
	BC.box((1.0, 2.85, z), (2.0, 2.95, z + 0.8), "galv")                         # deflagration vent panels
BC.box((2.72, 0.4, 2.6), (2.9, 1.2, 3.4), "grey")                                # the sprinkler connection, east
BC.collide((0.25, 0.0, 0.0), (2.75, 2.95, 6.0))

# ---- the PCS -------------------------------------------------------------------------------------------------------------------
PC = model("bess_pcs", 3, 3, 3, "grey")
PC.box((0.0, 0.0, 0.0), (3.0, 0.2, 3.0), "concrete")
PC.box((0.2, 0.2, 0.3), (2.8, 2.5, 2.7), "grey")                                 # the inverter cabinets
for x in (0.35, 1.25, 2.15):
	PC.box((x, 0.4, 0.28), (x + 0.5, 2.2, 0.3), "grey")                         # cabinet doors
	PC.box((x + 0.05, 1.7, 0.27), (x + 0.45, 2.1, 0.28), "louvre")              # filter louvres
PC.decal((0.5, 1.0, 0.26), (0.8, 1.3, 0.27), "arcflash", "north", "grey")
PC.box((0.4, 2.5, 0.5), (2.6, 2.8, 2.5), "louvre")                               # roof fans
PC.collide((0.2, 0.0, 0.3), (2.8, 2.8, 2.7))

# ---- the MV skid --------------------------------------------------------------------------------------------------------------
MV = model("bess_mv_skid", 3, 3, 3, "padgreen")
MV.box((0.0, 0.0, 0.0), (3.0, 0.15, 3.0), "concrete")
MV.box((0.5, 0.15, 0.6), (2.0, 2.0, 2.4), "padgreen")                            # the transformer tank
MV.fins((0.5, 0.4, 2.4), (2.0, 1.8, 2.8), "x", 10, 0.03, "padgreen")             # radiators
MV.cylinder("y", (1.25, 1.5), 0.25, 2.0, 2.4, "padgreen", 10)                    # conservator stub
MV.box((2.1, 0.15, 0.3), (2.9, 2.2, 2.7), "grey")                                # the ring main unit, east (high side)
MV.box((0.05, 0.3, 0.3), (0.5, 1.6, 1.2), "grey")                                # the low-voltage throat, west
MV.decal((2.89, 1.3, 1.0), (2.9, 1.7, 1.4), "danger", "east", "grey")
MV.collide((0.05, 0.0, 0.3), (2.9, 2.4, 2.8))

# ---- the plant controller ----------------------------------------------------------------------------------------------------
PL = model("bess_plant_controller", 3, 4, 3, "grey")
PL.box((0.0, 0.0, 0.0), (3.0, 0.2, 3.0), "concrete")
PL.box((0.2, 0.2, 0.2), (2.8, 2.9, 2.8), "grey")                                 # the house
PL.box((0.1, 2.9, 0.1), (2.9, 3.05, 2.9), "galv")                                # its roof
PL.box((0.4, 0.2, 0.18), (1.3, 2.3, 0.2), "steel")                               # the door, north
PL.box((1.7, 1.2, 0.0), (2.6, 2.0, 0.2), "louvre")                               # the air conditioner
PL.cylinder("y", (2.4, 2.4), 0.04, 3.05, 3.9, "galv", 6)                         # the antenna mast
PL.decal((1.5, 2.3, 0.17), (1.8, 2.6, 0.18), "controller", "north", "grey")
PL.collide((0.2, 0.0, 0.2), (2.8, 3.05, 2.8))

# ---- the cabinet --------------------------------------------------------------------------------------------------------------
CB = model("bess_cabinet", 2, 3, 2, "grey")
CB.box((0.0, 0.0, 0.0), (2.0, 0.15, 2.0), "concrete")
CB.box((0.25, 0.15, 0.3), (1.85, 2.4, 1.7), "grey")                              # the enclosure
CB.box((0.3, 0.3, 0.28), (1.05, 2.3, 0.3), "grey")                               # its doors
CB.box((1.1, 0.3, 0.28), (1.8, 2.3, 0.3), "grey")
CB.box((0.5, 1.6, 1.7), (1.6, 2.2, 1.9), "louvre")                               # the chiller, south
CB.box((0.0, 0.3, 0.3), (0.25, 0.9, 0.8), "grey")                                # the cable box, west of its main cell
CB.decal((-0.01, 0.4, 0.4), (0.0, 0.8, 0.7), "danger", "west", "grey")
CB.decal((1.3, 1.5, 0.27), (1.6, 1.8, 0.28), "flammable", "north", "grey")
CB.collide((0.0, 0.0, 0.3), (1.85, 2.4, 1.9))

M = BC
EXTRA = [PC, MV, PL, CB]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
