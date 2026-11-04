"""The 10d2 true-size gear, each drawn facing north from its main cell:
- mv_switchgear_main and mv_switchgear_feeder, 1 x 3 x 3 (docs/multiblocks/mv-switchgear.md): a 5 kV metal-clad section 0.9 m wide,
  2.4 m tall and 2.3 m deep, the draw-out vacuum breaker behind its lower front door, the relay in the door above, the bus
  compartment in the middle and the cable compartment at the back. The main's cable throat is low on its back; the feeder's cable
  leaves a box on its roof at the back. Sections stand side by side, so the cubicle fills the whole width;
- breaker_relay_cabinet, 1 x 2 x 1 (breaker-relay-cabinet.md): a 0.6 m relay cabinet on a plinth, the relay behind its window, the
  control switch and lamps, the test switches below;
- substation_transformer_10, 5 x 5 x 4 (power-transformer-10.md): the 10 MVA 4.16/138 kV unit, after the 25 MVA GSU in
  gas_turbine.py, smaller: the tank, its radiators, the conservator, three 138 kV bushings on the cover over the front, and the
  4.16 kV cable throat low on the back."""
import yardlib as y


def section(name, main):
	M = y.model(name, 1, 3, 3, "grey")
	M.box((0.0, 0.0, 0.1), (1.0, 0.1, 2.5), "dark")                            # the channel base
	M.box((0.0, 0.1, 0.1), (1.0, 2.4, 2.5), "grey")                            # the cubicle, 2.3 m deep
	M.box((0.0, 2.4, 0.08), (1.0, 2.47, 2.52), "grey")                         # its roof
	M.box((0.985, 0.1, 0.07), (1.0, 2.4, 0.1), "dark")                         # the joint to the next section, at the front
	M.box((0.06, 0.15, 0.08), (0.94, 1.35, 0.1), "grey")                       # the breaker compartment's door
	M.box((0.06, 1.45, 0.08), (0.94, 2.35, 0.1), "grey")                       # the relay and control door
	M.box((0.82, 0.6, 0.06), (0.88, 0.9, 0.08), "dark")                        # handles
	M.box((0.82, 1.75, 0.06), (0.88, 1.95, 0.08), "dark")
	M.box((0.2, 0.45, 0.075), (0.7, 1.05, 0.08), "dark")                       # the viewing window onto the breaker
	M.box((0.32, 0.6, 0.07), (0.58, 0.9, 0.075), "padgreen")                   # the open/closed flag and the spring indicator
	M.box((0.4, 0.22, 0.06), (0.6, 0.32, 0.08), "steel")                       # the racking port
	M.decal((0.15, 1.75, 0.075), (0.7, 2.25, 0.08), "relays", "north", "grey")
	M.box((0.75, 2.1, 0.07), (0.82, 2.17, 0.08), "padgreen")                   # the open (green) and closed (red) lamps
	M.box((0.75, 1.98, 0.07), (0.82, 2.05, 0.08), "danger")
	M.decal((0.15, 1.5, 0.075), (0.7, 1.68, 0.08), "nameplate", "north", "steel")
	M.box((0.08, 0.2, 2.5), (0.92, 2.3, 2.52), "grey")                         # the bolted rear covers
	M.decal((0.25, 1.4, 2.52), (0.75, 1.9, 2.53), "danger", "south", "grey")
	if main:
		M.box((0.25, 0.15, 2.52), (0.75, 0.85, 3.0), "steel")                  # the incoming cable throat, low on the back
	else:
		M.box((0.3, 2.47, 2.0), (0.7, 3.0, 2.5), "steel")                      # the outgoing cable box on the roof, at the back
	M.collide((0.0, 0.0, 0.08), (1.0, 2.47, 2.52))
	return M


M = section("mv_switchgear_main", True)
F = section("mv_switchgear_feeder", False)

# ---- the breaker relay cabinet ----------------------------------------------------------------------------------------------------------
C = y.model("breaker_relay_cabinet", 1, 2, 1, "grey")
C.box((0.15, 0.0, 0.2), (0.85, 0.1, 0.8), "dark")                              # the plinth
C.box((0.2, 0.1, 0.25), (0.8, 1.85, 0.75), "grey")                             # the cabinet, 0.6 m wide and 1.8 m tall
C.box((0.18, 1.85, 0.23), (0.82, 1.9, 0.77), "grey")
C.box((0.23, 0.15, 0.23), (0.77, 1.8, 0.25), "grey")                           # the door
C.decal((0.28, 1.15, 0.225), (0.72, 1.7, 0.23), "relays", "north", "grey")
C.decal((0.3, 0.9, 0.225), (0.7, 1.05, 0.23), "nameplate", "north", "steel")
C.box((0.3, 0.7, 0.2), (0.42, 0.82, 0.23), "dark")                             # the control switch
C.box((0.52, 0.74, 0.21), (0.58, 0.8, 0.23), "danger")                         # the trip lamp
C.box((0.62, 0.74, 0.21), (0.68, 0.8, 0.23), "padgreen")                       # the closed and healthy lamp
for x in (0.3, 0.4, 0.5, 0.6):
	C.box((x, 0.4, 0.21), (x + 0.06, 0.55, 0.23), "dark")                     # the test switches
C.decal((0.3, 0.18, 0.225), (0.7, 0.33, 0.23), "danger", "north", "grey")
C.collide((0.15, 0.0, 0.2), (0.85, 1.9, 0.8))

# ---- the 10 MVA power transformer ---------------------------------------------------------------------------------------------------------
T = y.p.mlib.Model("substation_transformer_10", y.p.mlib.footprint_box(5, 5, 4, 2, 3), dict(y.TEX), y.TEX["grey"])
T.box((-1.3, 0.0, -2.5), (2.3, 0.3, 0.6), "iron")                              # the skid
T.box((-1.0, 0.3, -2.3), (2.0, 3.0, 0.4), "grey")                              # the tank
for x in (-1.6, 2.0):
	T.fins((x, 0.6, -2.1), (x + 0.6, 2.7, 0.2), "z", 8, 0.04, "radfin")       # the radiators on both sides, ONAN
T.cylinder("x", (3.6, -2.0), 0.3, -0.8, 1.8, "grey", 12)                       # the conservator on its brackets
T.box((-0.6, 3.0, -2.1), (-0.4, 3.35, -1.9), "iron")
T.box((1.4, 3.0, -2.1), (1.6, 3.35, -1.9), "iron")
for x in (-0.4, 0.5, 1.4):
	T.cylinder("y", (x, -1.6), 0.14, 3.0, 4.5, "porcelain", 10)                # the 138 kV bushings
	for yy in (3.3, 3.6, 3.9, 4.2):
		T.ring("y", (x, -1.6), 0.22, yy - 0.03, yy + 0.03, 0.08, "porcelain", sides=10)
	T.cylinder("y", (x, -1.6), 0.05, 4.5, 4.8, "copper", 8)
T.box((0.0, 0.4, 0.4), (1.0, 1.6, 1.0), "steel")                               # the 4.16 kV cable throat, back
T.decal((-1.01, 1.4, -1.6), (-1.0, 2.2, -0.4), "ratingplate", "west", "grey")
T.decal((2.0, 1.4, -1.6), (2.01, 2.2, -0.4), "danger", "east", "grey")
T.collide((-1.6, 0.0, -2.5), (2.6, 3.0, 0.6))

EXTRA = [F, C, T]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
