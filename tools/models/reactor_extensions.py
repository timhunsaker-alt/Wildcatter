"""Phase 19's reactor extensions at true size (docs/multiblocks/reactor-extensions.md), each part drawn facing north from its own main
cell (the footprints match ContainmentPartBlock's):
- equipment_hatch, 4 x 4 x 1: the round bolted hatch in its sleeve, set into the containment wall;
- personnel_airlock, 2 x 3 x 3: the steel barrel with its inner and outer doors;
- hydrogen_recombiner, 1 x 2 x 1: a passive autocatalytic recombiner, a louvred box of catalyst plates;
- containment_monitor, 1 x 2 x 1: the panel that reads the building;
- pressurizer_relief_tank, 2 x 2 x 6: the horizontal tank on its saddles, its rupture disc on top;
- cask_loading_station, 3 x 4 x 4: the cask pit with a canister in it, the welding machine on its gantry, the drying skid (19b);
- storage_module, 4 x 6 x 6: a horizontal concrete storage module, 1 m walls, its door and air vents (19b);
- new_fuel_inspection_stand, 2 x 6 x 2: the stand's columns, its lights and camera (19b);
- personnel_monitor, 1 x 3 x 1: the whole-body contamination monitor and shower booth (19b)."""
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

# ---- 19b: the cask loading station ----------------------------------------------------------------------------------------------------
CS = model("cask_loading_station", 3, 4, 4, "concrete")
for x in range(3):
	for z in range(4):
		CS.box((x, 0.0, z), (x + 1, 0.3, z + 1), "concrete")                 # its slab
for x, z in ((0, 0), (1, 0), (0, 1), (1, 1)):
	CS.box((x, 0.3, z), (x + 1, 1.2, z + 1), "concrete")                     # the cask pit's walls
CS.cylinder("y", (1.0, 1.0), 0.7, 0.3, 3.2, "steel", 16)                      # the canister in its transfer cask
CS.cylinder("y", (1.0, 1.0), 0.75, 3.2, 3.4, "iron", 16)                      # its lid, being welded
for x in (0.1, 1.8):
	CS.box((x, 0.3, 0.2), (x + 0.15, 3.9, 0.35), "iron")                     # the gantry's legs
	CS.box((x, 0.3, 1.65), (x + 0.15, 3.9, 1.8), "iron")
CS.box((0.1, 3.75, 0.2), (1.95, 3.9, 1.8), "iron")                            # its beam
CS.box((0.75, 3.4, 0.75), (1.25, 3.75, 1.25), "blue")                         # the welding head
CS.box((2.1, 0.3, 0.2), (2.9, 1.6, 3.8), "blue")                              # the vacuum drying and helium skid
CS.cylinder("y", (2.5, 2.9), 0.3, 1.6, 2.6, "galv", 10)                       # its helium bottles
CS.box((0.2, 0.3, 2.3), (1.8, 1.4, 3.8), "grey")                              # the welder's power cabinet
CS.collide((0.0, 0.0, 0.0), (3.0, 3.9, 4.0))

# ---- 19b: the horizontal storage module ---------------------------------------------------------------------------------------------
SM = model("storage_module", 4, 6, 6, "concrete")
for x in range(4):
	for y in range(6):
		for z in range(6):
			if x in (0, 3) or y in (0, 5) or z in (0, 5):
				SM.box((x, y, z), (x + 1, y + 1, z + 1), "concrete")              # the 1 m walls, roof and floor
SM.cylinder("z", (2.0, 2.6), 0.85, 1.0, 5.0, "steel", 16)                     # the canister on its rails, inside
for z in (0.0, 5.99):
	SM.box((1.0, 1.0, z), (3.0, 4.0, z + 0.01), "iron")                      # the door and the back plate
for x in (0.3, 3.3):
	SM.box((x, 5.0, 2.0), (x + 0.4, 5.25, 4.0), "grating")                   # the roof's air outlets
SM.collide((0.0, 0.0, 0.0), (4.0, 6.0, 6.0))

# ---- 19b: the new-fuel inspection stand ---------------------------------------------------------------------------------------------
IS = model("new_fuel_inspection_stand", 2, 6, 2, "galv")
for x, z in ((0.1, 0.1), (1.75, 0.1), (0.1, 1.75), (1.75, 1.75)):
	for y in range(6):
		IS.box((x, y, z), (x + 0.15, y + 1, z + 0.15), "galv")              # the columns
for y in (0.0, 5.85):
	for x in range(2):
		IS.box((x, y, 0.1), (x + 1, y + 0.15, 1.9), "grating")              # the base and the top frame
IS.box((0.85, 0.15, 0.85), (1.15, 4.2, 1.15), "steel")                        # the assembly standing in it
for y in (1.0, 2.5, 4.0):
	IS.box((0.25, y, 0.2), (0.45, y + 0.2, 0.4), "blue")                     # the cameras
	IS.box((1.55, y, 1.6), (1.75, y + 0.2, 1.8), "grey")                     # the lights
IS.collide((0.1, 0.0, 0.1), (1.9, 6.0, 1.9))

# ---- 19b: the personnel monitor -------------------------------------------------------------------------------------------------------
PM = model("personnel_monitor", 1, 3, 1, "grey")
for y in range(3):
	PM.box((0.05, y, 0.05), (0.95, y + (0.9 if y == 2 else 1), 0.25), "grey")   # the detector wall
	PM.box((0.05, y, 0.25), (0.15, y + (0.9 if y == 2 else 1), 0.95), "grey")   # its side panels
	PM.box((0.85, y, 0.25), (0.95, y + (0.9 if y == 2 else 1), 0.95), "grey")
PM.box((0.15, 2.7, 0.25), (0.85, 2.9, 0.95), "grey")                          # the roof and its shower head
PM.decal((0.25, 1.3, 0.26), (0.75, 1.8, 0.27), "controller", "south", "grey")  # the readout
PM.collide((0.05, 0.0, 0.05), (0.95, 2.9, 0.95))

M = EH
EXTRA = [PA, PR, CM, RT, CS, SM, IS, PM]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
