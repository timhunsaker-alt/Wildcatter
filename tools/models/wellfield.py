"""The ISR wellfield at true size (docs/multiblocks/isr-wellfield.md), each part drawn facing north from its own main cell (the
footprints match WellfieldPartBlock's):
- header_house, 4 x 4 x 10: the insulated wellfield building on its pad, the producers' pipes into its west side, the injection
  header out of its east, the trunklines to the IX columns out of its back, its 480 V box on the north end;
- oxygen_generator, 3 x 3 x 6: the PSA skid: an air compressor, the receiver, two molecular-sieve beds and the oxygen buffer tank;
- ix_columns, 8 x 6 x 4: the lead and lag resin columns on their skid with the elution tank and the acid day tank;
- isr_wellhead, one cell: the PVC casing up out of the ground and its well cap; with the pump, a producer's pitless head with
  its power cable conduit and a flow meter on the line;
- co2_liquefier, 3 x 4 x 6 (16d1): the kiln-gas scrubber, the two-stage compressor, the dryer and the refrigerated condenser (480 V box
  west), liquid CO2 out the back;
- co2_storage_tank, 3 x 3 x 8 (16d1): the 30 t insulated horizontal tank on its saddles with its vaporiser;
- resin_trailer, 2 x 3 x 6 (16d1): a trailer carrying a 10 m3 resin vessel, its axles, wheels and kingpin."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


# ---- the header house ---------------------------------------------------------------------------------------------------------
HH = model("header_house", 4, 4, 10, "grey")
HH.box((0.0, 0.0, 0.0), (4.0, 0.2, 10.0), "concrete")                     # its pad
HH.box((0.2, 0.2, 0.2), (3.8, 3.2, 9.8), "louvre")                          # the insulated building
HH.box((0.1, 3.2, 0.1), (3.9, 3.35, 9.9), "grey")                           # the roof, pitched a little
HH.box((1.0, 3.35, 0.1), (3.0, 3.5, 9.9), "grey")
HH.box((1.4, 0.2, 9.8), (2.6, 2.4, 9.85), "iron")                           # its door, the back end
HH.box((1.5, 0.6, 0.0), (2.5, 1.4, 0.2), "grey")                            # its 480 V box, the north end
HH.decal((1.6, 0.7, -0.01), (2.4, 1.3, 0.0), "danger", "north", "grey")
for z in (1.5, 3.5, 5.5, 7.5):
	HH.pipe([(0.0, 0.4, z), (0.2, 0.4, z)], 3, "polymer")                   # producers' lines in, the west
	HH.pipe([(3.8, 0.4, z), (4.0, 0.4, z)], 3, "polymer")                   # injection header out, the east
HH.pipe([(1.0, 0.4, 9.8), (1.0, 0.4, 10.0)], 8, "polymer")                  # pregnant trunk to the IX columns
HH.pipe([(3.0, 0.4, 9.8), (3.0, 0.4, 10.0)], 8, "polymer")                  # barren trunk back
HH.collide((0.2, 0.0, 0.2), (3.8, 3.5, 9.8))

# ---- the oxygen generator ---------------------------------------------------------------------------------------------------------
OG = model("oxygen_generator", 3, 3, 6, "blue")
OG.box((0.05, 0.0, 0.05), (2.95, 0.2, 5.95), "iron")                        # the skid
OG.box((0.3, 0.2, 0.3), (1.6, 1.6, 2.2), "blue")                            # the air compressor in its canopy
OG.fins((0.35, 0.4, 0.28), (1.55, 1.4, 0.3), "x", 8, 0.02, "galv")
OG.box((0.0, 0.5, 0.3), (0.3, 1.1, 0.8), "grey")                            # its 480 V box, the west
OG.cylinder("y", (2.3, 1.2), 0.45, 0.2, 2.2, "steel", 14)                   # the air receiver
for z in (3.0, 4.0):
	OG.cylinder("y", (1.0, z), 0.38, 0.2, 2.8, "padgreen", 14)              # the two molecular-sieve beds
OG.cylinder("y", (2.3, 4.6), 0.45, 0.2, 2.4, "steel", 14)                   # the oxygen buffer tank
OG.pipe([(1.0, 2.6, 3.0), (1.0, 2.6, 4.0)], 2, "steel")
OG.pipe([(1.0, 2.3, 4.0), (2.3, 2.3, 4.0), (2.3, 2.3, 4.6)], 1, "copper")
OG.collide((0.05, 0.0, 0.05), (2.95, 2.4, 5.95))

# ---- the IX columns ---------------------------------------------------------------------------------------------------------------
IX = model("ix_columns", 8, 6, 4, "steel")
IX.box((0.0, 0.0, 0.0), (8.0, 0.25, 4.0), "concrete")
for x in (2.0, 6.0):
	IX.ring("y", (x, 2.0), 1.75, 0.6, 5.2, 0.1, "steel", sides=20)          # a resin column, 3.5 m across by 4.6 m
	IX.box((x - 1.0, 5.15, 1.0), (x + 1.0, 5.25, 3.0), "steel")             # its dished head
	IX.box((x - 1.6, 5.15, 1.6), (x + 1.6, 5.2, 2.4), "steel")
	IX.box((x - 0.3, 5.25, 1.7), (x + 0.3, 5.7, 2.3), "steel")              # its top manway
	for dx in (-1.2, 1.2):
		IX.box((x + dx - 0.1, 0.25, 1.9), (x + dx + 0.1, 0.6, 2.1), "iron")  # its legs
IX.pipe([(2.0, 0.5, 0.0), (2.0, 0.5, 0.3)], 8, "polymer")                   # pregnant lixiviant in, the front
IX.pipe([(2.0, 0.4, 3.7), (2.0, 0.4, 4.0)], 3, "steel")                     # eluate out, the back (west)
IX.pipe([(6.0, 0.4, 3.7), (6.0, 0.4, 4.0)], 8, "polymer")                   # barren out, the back (east)
IX.pipe([(8.0, 0.4, 2.0), (7.75, 0.4, 2.0)], 2, "steel")                    # acid in, the east
IX.pipe([(2.0, 5.5, 2.0), (2.0, 5.8, 2.0), (6.0, 5.8, 2.0), (6.0, 5.5, 2.0)], 6, "steel")   # lead to lag over the top
IX.collide((0.25, 0.0, 0.25), (7.75, 5.7, 3.75))


# ---- the ISR wellhead: injector, and producer with its pump ----------------------------------------------------------------------
def wellhead(name, pump):
	M = mlib.Model(name, [(0, 0, 0)], TEX, TEX["polymer"])
	M.box((0.2, 0.0, 0.2), (0.8, 0.08, 0.8), "concrete")                    # the well pad
	M.cylinder("y", (0.5, 0.5), 0.085, 0.0, 0.55, "polymer", 12)           # 6 in PVC casing
	M.cylinder("y", (0.5, 0.5), 0.1, 0.55, 0.65, "padgreen" if pump else "blue", 12)   # the well cap
	M.pipe([(0.5, 0.35, 0.5), (1.0, 0.35, 0.5)], 2, "polymer")             # its line, east
	if pump:
		M.cylinder("y", (0.5, 0.5), 0.03, 0.65, 0.75, "steel", 8)          # the pitless head's lifting eye
		M.pipe([(0.5, 0.6, 0.5), (0.5, 0.6, 0.15), (0.5, 0.08, 0.15)], 1, "grey")   # the pump's power cable conduit
		M.box((0.7, 0.29, 0.43), (0.84, 0.41, 0.57), "brass")              # a flow meter on the line
	M.collide((0.38, 0.0, 0.38), (0.62, 0.65, 0.62))
	return M


ISR = wellhead("isr_wellhead", False)
ISR.export_args = {"blockstate": False, "item": True}
ISR_PUMP = wellhead("isr_wellhead_pump", True)
ISR_PUMP.export_args = {"blockstate": False, "item": False}

# ---- 16d1: the CO2 liquefier ----------------------------------------------------------------------------------------------------
CL = model("co2_liquefier", 3, 4, 6, "grey")
CL.box((0.0, 0.0, 0.0), (3.0, 0.2, 6.0), "concrete")
CL.cylinder("y", (0.8, 0.9), 0.45, 0.2, 3.8, "steel", 14)                   # the kiln-gas scrubber
CL.box((1.5, 0.2, 0.4), (2.8, 1.6, 2.6), "blue")                            # the two-stage compressor
CL.cylinder("z", (2.15, 1.0), 0.35, 2.6, 3.2, "blue", 12)                   # its motor
CL.cylinder("y", (0.8, 3.3), 0.3, 0.2, 2.6, "steel", 12)                    # the dryer
CL.box((0.3, 0.2, 4.0), (2.7, 2.2, 5.6), "louvre")                          # the refrigeration unit and condenser
CL.box((0.5, 2.2, 4.2), (2.5, 2.6, 5.4), "radfin")                          # its condenser fans
CL.pipe([(0.8, 3.8, 0.9), (0.8, 3.95, 0.9), (2.15, 3.95, 0.9), (2.15, 1.6, 0.9)], 4, "steel")   # gas from the scrubber to the compressor
CL.pipe([(1.5, 0.5, 5.6), (1.5, 0.5, 6.0)], 2, "steel")                     # liquid CO2 out, the back
CL.box((0.0, 0.5, 0.3), (0.3, 1.1, 0.8), "grey")                            # its 480 V box, the west
CL.decal((-0.01, 0.6, 0.4), (0.0, 1.0, 0.7), "danger", "west", "grey")
CL.collide((0.2, 0.0, 0.2), (2.8, 3.8, 5.8))

# ---- 16d1: the CO2 storage tank -------------------------------------------------------------------------------------------------
CT = model("co2_storage_tank", 3, 3, 8, "lagging")
CT.box((0.0, 0.0, 0.0), (3.0, 0.2, 8.0), "concrete")
for z in (1.2, 6.8):
	CT.box((0.4, 0.2, z - 0.2), (2.6, 1.0, z + 0.2), "iron")                 # a saddle
CT.cylinder("z", (1.5, 1.6), 1.2, 0.5, 7.5, "lagging", 20)                 # the insulated tank, 2.4 m across
CT.cylinder("z", (1.5, 1.6), 0.9, 0.3, 0.5, "lagging", 16)                 # its dished heads
CT.cylinder("z", (1.5, 1.6), 0.9, 7.5, 7.7, "lagging", 16)
CT.box((2.3, 0.2, 0.1), (2.9, 1.4, 0.9), "finned")                         # the vaporiser
CT.pipe([(1.5, 0.5, 0.0), (1.5, 0.5, 0.3)], 2, "steel")                     # liquid CO2 in, the front
CT.collide((0.2, 0.0, 0.2), (2.8, 2.8, 7.8))

# ---- 16d1: the resin trailer ----------------------------------------------------------------------------------------------------
RT = model("resin_trailer", 2, 3, 6, "padgreen")
RT.box((0.15, 0.7, 0.2), (1.85, 0.85, 5.9), "iron")                        # the deck
for z in (4.3, 5.2):
	for x in (0.15, 1.6):
		RT.cylinder("x", (0.45, z), 0.45, x, x + 0.25, "rubber", 14)         # its wheels on two axles
	RT.cylinder("x", (0.45, z), 0.06, 0.4, 1.6, "iron", 8)
RT.box((0.9, 0.4, 0.3), (1.1, 0.7, 0.5), "iron")                            # the landing leg
RT.box((0.9, 0.85, 0.2), (1.1, 1.0, 0.4), "iron")                           # the kingpin plate
RT.cylinder("z", (1.0, 1.75), 0.85, 1.0, 5.6, "padgreen", 18)               # the 10 m3 resin vessel
RT.cylinder("y", (1.0, 1.6), 0.2, 2.55, 2.85, "steel", 10)                  # its fill and transfer nozzles
RT.cylinder("y", (1.0, 4.6), 0.2, 2.55, 2.85, "steel", 10)
RT.collide((0.1, 0.0, 0.2), (1.9, 2.6, 5.9))

# ---- 16d2: the monitor wellhead and the RO unit ---------------------------------------------------------------------------------
MW = mlib.Model("monitor_wellhead", [(0, 0, 0)], TEX, TEX["polymer"])
MW.box((0.25, 0.0, 0.25), (0.75, 0.08, 0.75), "concrete")                  # the well pad
MW.cylinder("y", (0.5, 0.5), 0.06, 0.0, 0.7, "polymer", 10)                # 4 in PVC casing
MW.cylinder("y", (0.5, 0.5), 0.08, 0.7, 0.8, "danger", 10)                 # its locked cap, painted
MW.box((0.62, 0.08, 0.35), (0.9, 0.5, 0.65), "grey")                       # the sampling pump's control box
MW.pipe([(0.5, 0.35, 0.5), (0.0, 0.35, 0.5)], 1, "polymer")                # its sample line, west
MW.collide((0.4, 0.0, 0.4), (0.6, 0.8, 0.6))
MW.export_args = {"blockstate": False, "item": True}   # one cell with no states: after() writes its blockstate

RO = model("ro_unit", 3, 3, 8, "grey")
RO.box((0.0, 0.0, 0.0), (3.0, 0.2, 8.0), "concrete")
RO.box((0.3, 0.2, 0.3), (1.3, 1.6, 1.5), "steel")                          # cartridge filters
RO.box((1.6, 0.2, 0.3), (2.7, 1.3, 1.8), "blue")                           # the high-pressure pumps
for y in (0.6, 1.2, 1.8, 2.4):
	RO.cylinder("z", (0.9, y), 0.18, 2.0, 7.6, "polymer", 10)             # spiral-wound membrane vessels
	RO.cylinder("z", (2.1, y), 0.18, 2.0, 7.6, "polymer", 10)
RO.box((0.4, 0.2, 2.0), (2.6, 0.35, 7.6), "iron")                          # their rack
RO.pipe([(1.5, 0.5, 0.0), (1.5, 0.5, 0.3)], 4, "steel")                    # feed in, the front
RO.box((0.0, 0.5, 0.3), (0.3, 1.1, 0.8), "grey")                            # its 480 V box, the west
RO.decal((-0.01, 0.6, 0.4), (0.0, 1.0, 0.7), "danger", "west", "grey")
RO.collide((0.2, 0.0, 0.2), (2.8, 2.7, 7.8))

M = HH
EXTRA = [OG, IX, ISR, ISR_PUMP, CL, CT, RT, MW, RO]
for extra in (OG, IX, CL, CT, RT, RO):
	extra.export_args = {"blockstate": True, "item": True}


def after():
	import gearlib
	gearlib.save_blockstate("isr_wellhead", {"pump=false": {"model": "wildcatter:block/isr_wellhead/p0"},
		"pump=true": {"model": "wildcatter:block/isr_wellhead_pump/p0"}})
	gearlib.save_blockstate("monitor_wellhead", {"": {"model": "wildcatter:block/monitor_wellhead/p0"}})
