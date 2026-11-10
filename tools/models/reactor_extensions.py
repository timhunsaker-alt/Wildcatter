"""Phase 19's reactor extensions at true size (docs/multiblocks/reactor-extensions.md), each part drawn facing north from its own main
cell (the footprints match ContainmentPartBlock's):
- equipment_hatch, 4 x 4 x 1: the round bolted hatch in its sleeve, set into the containment wall;
- personnel_airlock, 2 x 3 x 3: the steel barrel with its inner and outer doors;
- hydrogen_recombiner, 1 x 2 x 1: a passive autocatalytic recombiner, a louvred box of catalyst plates;
- containment_monitor, 1 x 2 x 1: the panel that reads the building;
- pressurizer_relief_tank, 2 x 2 x 6: the horizontal tank on its saddles, its rupture disc on top."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


# ---- the equipment hatch ----------------------------------------------------------------------------------------------------------
EH = model("equipment_hatch", 4, 4, 1, "steel")
for x in range(4):
	for y in range(4):
		EH.box((x, y, 0.45), (x + 1, y + 1, 1.0), "concrete")                # the sleeve's embedment, cell by cell
		EH.box((x + 0.02, y + 0.02, 0.12), (x + 0.98, y + 0.98, 0.45), "steel")   # the bolted cover's plate
for x in (0.15, 3.65):
	for y in range(4):
		EH.box((x, y + 0.1, 0.05), (x + 0.2, y + 0.9, 0.12), "iron")        # its bolting flange, the sides
for y in (0.15, 3.65):
	for x in range(4):
		EH.box((x + 0.1, y, 0.05), (x + 0.9, y + 0.2, 0.12), "iron")        # and top and bottom
EH.cylinder("z", (2.0, 2.0), 0.45, 0.0, 0.12, "galv", 12)                    # the lifting lug's pad
EH.collide((0.0, 0.0, 0.0), (4.0, 4.0, 1.0))

# ---- the personnel airlock ----------------------------------------------------------------------------------------------------------
PA = model("personnel_airlock", 2, 3, 3, "steel")
PA.cylinder("z", (1.0, 1.35), 0.95, 0.15, 2.85, "steel", 18)                  # the barrel
for z0, z1 in ((0.0, 0.15), (2.85, 3.0)):
	PA.cylinder("z", (1.0, 1.35), 0.98, z0, z1, "iron", 18)                  # the bulkheads
for z in (0.0, 2.99):
	PA.box((0.6, 0.35, z), (1.4, 2.25, z + 0.01), "grey")                     # the doors
PA.box((0.15, 0.0, 0.3), (1.85, 0.4, 2.7), "concrete")                        # its floor and support
PA.collide((0.0, 0.0, 0.0), (2.0, 2.4, 3.0))

# ---- the passive autocatalytic recombiner ------------------------------------------------------------------------------------------
PR = model("hydrogen_recombiner", 1, 2, 1, "galv")
PR.box((0.12, 0.25, 0.25), (0.88, 1.75, 0.75), "galv")                        # the housing
for y in (0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6):
	PR.box((0.15, y, 0.24), (0.85, y + 0.06, 0.25), "iron")                  # the inlet and outlet louvres
for x in (0.2, 0.8):
	PR.box((x - 0.04, 0.0, 0.46), (x + 0.04, 0.25, 0.54), "iron")            # its legs
PR.collide((0.12, 0.0, 0.25), (0.88, 1.75, 0.75))

# ---- the containment monitor --------------------------------------------------------------------------------------------------------
CM = model("containment_monitor", 1, 2, 1, "grey")
CM.box((0.08, 0.0, 0.35), (0.92, 1.9, 0.95), "grey")                          # the cabinet
CM.decal((0.15, 1.1, 0.34), (0.85, 1.75, 0.35), "controller", "north", "grey")
CM.decal((0.2, 0.6, 0.34), (0.8, 1.0, 0.35), "gauge", "north", "grey")
CM.collide((0.08, 0.0, 0.35), (0.92, 1.9, 0.95))

# ---- the pressurizer relief tank ----------------------------------------------------------------------------------------------------
RT = model("pressurizer_relief_tank", 2, 2, 6, "steel")
for z in (0.8, 5.2):
	RT.box((0.25, 0.0, z - 0.2), (1.75, 0.45, z + 0.2), "concrete")          # the saddles
RT.cylinder("z", (1.0, 1.05), 0.9, 0.25, 5.75, "steel", 18)                   # the shell
for z0, z1 in ((0.05, 0.25), (5.75, 5.95)):
	RT.cylinder("z", (1.0, 1.05), 0.7, z0, z1, "steel", 14)                  # the heads
RT.cylinder("y", (1.0, 4.5), 0.18, 1.85, 2.0, "blue", 10)                     # the rupture disc's holder
RT.pipe([(1.0, 1.9, 1.5), (1.0, 2.0, 1.5)], 6, "steel")                       # the PORVs' discharge line in
RT.collide((0.1, 0.0, 0.05), (1.9, 1.95, 5.95))

M = EH
EXTRA = [PA, PR, CM, RT]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
