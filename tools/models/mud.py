"""Phase 15b1 parts at true size (docs/multiblocks/drilling-mud.md), each drawn facing north from its own main cell (the footprints match
MudPartBlock's), all in the mod's existing materials. Each motor's terminal box is on the west face of the main cell (UnitMotorBlockEntity):
- barite_mill, 3 x 6 x 4: the roller mill on its gearbox, its 150 kW motor, the whizzer classifier on top, the duct to the pulse-jet
  baghouse at the back with its rotary valve, the feed bin on the front for a hopper;
- bentonite_dryer, 3 x 4 x 8: the rotary dryer drum on its tyres and trunnion rollers with its girth gear, the burner and gas train at the
  north end (fuel on the east of the bottom cells), the discharge breeching and cyclone at the south end, the mill and fan;
- mud_hopper, 2 x 3 x 3: the jet mixing hopper (funnel, table and venturi) on its skid, the 37 kW centrifugal mixing pump, and the
  discharge to the mud tanks out of its east side (set it facing south against the tanks' east face, so its box faces away).
15b2's well control:
- bop_stack, 2 x 4 x 2: nippled up on the wellhead cell under the rotary table: the drilling spool with its choke and kill line valves,
  the double ram body (blind-shear and pipe rams, their bonnets and operators out to the sides), the annular preventer on top, the bell
  nipple to the floor;
- bop_accumulator, 2 x 2 x 4: the accumulator unit's skid of nitrogen-charged bottles in two rows, the triplex pump and its 15 kW motor
  (box on the west of the main cell), the manifold and the remote panel."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


# ---- the barite mill ----------------------------------------------------------------------------------------------------------------
BM = model("barite_mill", 3, 6, 4, "grey")
BM.box((0.0, 0.0, 0.0), (3.0, 0.25, 4.0), "concrete")                         # its foundation
BM.box((0.4, 0.25, 0.3), (2.6, 0.9, 2.3), "engine")                           # the gearbox
BM.cylinder("y", (1.5, 1.3), 1.0, 0.9, 2.4, "grey", 16)                       # the mill housing round the rollers
BM.ring("y", (1.5, 1.3), 1.02, 1.5, 1.6, 0.04, "iron", sides=16)
BM.cylinder("y", (1.5, 1.3), 0.75, 2.4, 3.1, "grey", 14)                      # the whizzer classifier
BM.cylinder("y", (1.5, 1.3), 0.35, 3.1, 3.6, "engine", 10)                    # its drive
BM.box((0.0, 0.3, 0.2), (0.35, 1.1, 0.9), "engine")                            # the main motor, its box on the west
BM.decal((-0.01, 0.45, 0.3), (0.0, 0.85, 0.8), "danger", "west", "engine")
BM.box((1.1, 2.4, 0.0), (1.9, 3.0, 0.4), "galv")                               # the feed bin and its screw, the front
BM.box((1.05, 3.0, 0.0), (1.95, 3.2, 0.45), "iron")
BM.pipe([(1.5, 3.4, 1.3), (1.5, 4.6, 1.3), (1.5, 4.6, 2.6)], 10, "galv")      # the product duct to the baghouse
BM.box((0.4, 0.25, 2.6), (2.6, 0.8, 3.9), "iron")                              # the baghouse's legs and hopper base
BM.box((0.6, 0.8, 2.7), (2.4, 2.0, 3.8), "grey")                               # its hopper
BM.box((0.3, 2.0, 2.5), (2.7, 5.4, 3.95), "grey")                              # the pulse-jet baghouse
BM.box((0.25, 5.4, 2.45), (2.75, 5.6, 4.0), "galv")                            # its plenum and blowpipes
BM.cylinder("x", (5.75, 3.2), 0.12, 0.4, 2.6, "steel", 8)                     # the compressed-air header
BM.cylinder("z", (1.5, 0.55), 0.18, 3.85, 4.0, "steel", 8)                     # the rotary valve's discharge to the bagging station
BM.ladder(2.85, 3.0, 0.25, 5.4, 1, "galv")
BM.decal((2.7, 3.0, 3.0), (2.71, 3.6, 3.6), "nameplate", "east", "grey")
BM.collide((0.3, 0.0, 0.2), (2.7, 5.6, 4.0))

# ---- the bentonite dryer ------------------------------------------------------------------------------------------------------------
BD = model("bentonite_dryer", 3, 4, 8, "grey")
BD.box((0.0, 0.0, 0.0), (3.0, 0.2, 8.0), "concrete")
for z in (1.6, 5.4):
	BD.box((0.5, 0.2, z - 0.25), (2.5, 0.55, z + 0.25), "iron")                 # the trunnion roller bases
	BD.cylinder("z", (0.75, 0.75), 0.22, z - 0.18, z + 0.18, "steel", 10)       # the trunnion rollers
	BD.cylinder("z", (2.25, 0.75), 0.22, z - 0.18, z + 0.18, "steel", 10)
	BD.cylinder("z", (1.5, 1.75), 1.0, z - 0.15, z + 0.15, "iron", 18)          # the riding tyres
BD.cylinder("z", (1.5, 1.75), 0.92, 1.0, 6.6, "grey", 18)                      # the drum
BD.cylinder("z", (1.5, 1.75), 1.08, 3.4, 3.6, "engine", 20)                    # the girth gear
BD.box((2.3, 0.2, 3.0), (2.9, 0.8, 4.0), "engine")                             # its pinion drive
BD.box((0.2, 0.2, 0.0), (2.8, 3.2, 1.0), "hipaint")                            # the burner housing and combustion chamber, north
BD.cylinder("y", (1.5, 0.5), 0.2, 3.2, 3.9, "hipaint", 10)                     # its stack
BD.pipe([(3.0, 0.4, 0.5), (2.8, 0.4, 0.5)], 2, "steel")                        # gas in, the east of the bottom cells
BD.box((2.8, 0.25, 0.15), (3.0, 0.9, 0.85), "blue")                             # the gas train's valves
BD.decal((2.99, 1.2, 0.2), (3.0, 1.7, 0.8), "flammable", "east", "blue")
BD.box((0.3, 0.2, 6.6), (2.7, 2.8, 7.4), "grey")                               # the discharge breeching, south
BD.cylinder("y", (1.5, 7.6), 0.35, 1.8, 3.6, "grey", 12)                      # the cyclone
BD.cylinder("y", (1.5, 7.6), 0.12, 1.2, 1.8, "grey", 8)                       # its cone and valve
BD.box((0.0, 0.2, 0.0), (0.35, 0.9, 0.9), "engine")                            # the mill and fan motor, its box on the west
BD.box((0.4, 0.2, 7.3), (1.1, 1.2, 8.0), "engine")                             # the mill
BD.decal((-0.01, 0.35, 0.15), (0.0, 0.75, 0.75), "danger", "west", "engine")
BD.collide((0.2, 0.0, 0.0), (2.8, 2.8, 8.0))

# ---- the mud hopper -----------------------------------------------------------------------------------------------------------------
MH = model("mud_hopper", 2, 3, 3, "grey")
MH.box((0.0, 0.0, 0.0), (2.0, 0.15, 3.0), "iron")                              # the skid
MH.box((0.0, 0.15, 0.2), (0.4, 0.8, 0.9), "engine")                            # the pump motor, its box on the west
MH.decal((-0.01, 0.3, 0.3), (0.0, 0.7, 0.8), "danger", "west", "engine")
MH.cylinder("x", (0.55, 0.55), 0.3, 0.4, 0.9, "blue", 12)                     # the centrifugal mixing pump
MH.pipe([(0.9, 0.55, 0.55), (1.3, 0.55, 0.55), (1.3, 1.1, 0.55), (1.3, 1.1, 1.6)], 6, "steel")   # discharge up to the venturi
for x, z in ((0.6, 1.2), (1.8, 1.2), (0.6, 2.8), (1.8, 2.8)):
	MH.box((x - 0.05, 0.15, z - 0.05), (x + 0.05, 1.4, z + 0.05), "galv")      # the table's legs
MH.box((0.5, 1.4, 1.1), (1.9, 1.5, 2.9), "checker")                            # the table, where bags are cut
MH.box((0.85, 1.5, 1.45), (1.55, 2.3, 2.15), "galv")                           # the hopper funnel
MH.box((0.75, 2.3, 1.35), (1.65, 2.45, 2.25), "galv")                          # its rim and bag-breaker grid
MH.cylinder("y", (1.2, 1.8), 0.12, 1.1, 1.5, "steel", 8)                      # the venturi under it
MH.pipe([(1.2, 1.1, 1.8), (1.2, 1.1, 2.6), (2.0, 1.1, 2.6)], 6, "steel")      # mud out to the tanks, the east
MH.handrail([(0.5, 1.1), (0.5, 2.9)], 1.5, "galv")
MH.decal((1.89, 0.4, 1.4), (1.9, 0.9, 2.0), "nameplate", "east", "grey")
MH.collide((0.0, 0.0, 0.1), (2.0, 2.45, 3.0))


# ---- the BOP stack -----------------------------------------------------------------------------------------------------------------
BS = model("bop_stack", 2, 4, 2, "blue")
BS.cylinder("y", (1.0, 1.0), 0.32, 0.0, 0.35, "steel", 12)                    # the casing head flange it sits on
BS.box((0.55, 0.35, 0.55), (1.45, 1.0, 1.45), "blue")                          # the drilling spool
for x, d in ((0.0, 1), (2.0, -1)):
	BS.pipe([(x + d * 0.05, 0.65, 1.0), (1.0 - d * 0.45, 0.65, 1.0)], 3, "steel")   # the choke line (east) and the kill line (west)
	BS.cylinder("y", (x + d * 0.3, 1.0), 0.11, 0.45, 0.85, "steel", 8)         # their gate valves
BS.box((0.45, 1.0, 0.4), (1.55, 1.75, 1.6), "blue")                            # the double ram body
for z in (0.0, 1.6):
	BS.box((0.6, 1.05, z), (1.4, 1.35, z + 0.4), "blue")                        # the blind-shear ram bonnets
	BS.box((0.6, 1.4, z), (1.4, 1.7, z + 0.4), "blue")                          # the pipe ram bonnets
	BS.cylinder("z", (1.0, 1.2), 0.08, z - 0.0 if z == 0 else z + 0.3, z + 0.1 if z == 0 else z + 0.4, "steel", 8)
BS.cylinder("y", (1.0, 1.0), 0.55, 1.75, 2.9, "blue", 16)                      # the annular preventer
BS.cylinder("y", (1.0, 1.0), 0.6, 2.4, 2.55, "steel", 16)                      # its body flange
BS.cylinder("y", (1.0, 1.0), 0.28, 2.9, 4.0, "steel", 12)                      # the bell nipple up to the floor
BS.decal((0.44, 1.1, 0.7), (0.45, 1.6, 1.3), "nameplate", "west", "blue")
BS.collide((0.4, 0.0, 0.4), (1.6, 4.0, 1.6))

# ---- the accumulator unit ------------------------------------------------------------------------------------------------------------
BA = model("bop_accumulator", 2, 2, 4, "grey")
BA.box((0.0, 0.0, 0.0), (2.0, 0.15, 4.0), "iron")                               # the skid
for i in range(5):
	for x in (0.45, 1.55):
		BA.cylinder("y", (x, 1.6 + i * 0.45), 0.18, 0.15, 1.45, "red" if "red" in TEX else "engine", 10)   # the nitrogen bottles
		BA.cylinder("y", (x, 1.6 + i * 0.45), 0.08, 1.45, 1.55, "steel", 6)
BA.cylinder("x", (0.55, 1.6), 0.06, 0.2, 1.8, "steel", 8)                     # the bottle header
BA.box((0.0, 0.15, 0.1), (0.4, 0.75, 0.8), "engine")                            # the triplex pump's motor, its box on the west
BA.decal((-0.01, 0.3, 0.2), (0.0, 0.65, 0.7), "danger", "west", "engine")
BA.box((0.45, 0.15, 0.15), (1.5, 0.7, 0.85), "grey")                            # the triplex pump
BA.box((0.2, 0.15, 1.0), (1.8, 0.55, 1.3), "steel")                             # the manifold and its four-way valves
for x in (0.45, 0.85, 1.25, 1.65):
	BA.cylinder("y", (x, 1.15), 0.05, 0.55, 0.85, "brass", 6)                   # the valve handles
BA.box((0.6, 0.15, 3.85), (1.4, 1.6, 3.95), "grey")                             # the remote panel
BA.decal((0.65, 0.9, 3.95), (1.35, 1.4, 3.96), "gauge", "south", "grey")
BA.collide((0.0, 0.0, 0.0), (2.0, 1.6, 4.0))


M = BM
EXTRA = [BD, MH, BS, BA]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
