"""Soft starters and drives at true size (docs/multiblocks/soft-starter.md, vfd.md), drawn facing north (the doors toward -z).
- The 480 V soft starter and drive cabinets: floor-standing enclosures 0.6 m wide, 2.0 m tall and 0.6 m deep, 1 x 2 x 1, with the
  keypad, the disconnect handle and a filter louvre (the drive's at the top, for its fan).
- The 4.16 kV soft starter: a 36 in (0.91 m) cabinet 2.29 m tall, 1 x 3 x 1: the isolation switch's handle and window high, the
  SCR stacks' door in the middle, the vacuum bypass contactor's door low.
- The 4.16 kV drive lineup, 4.5 x 2.6 x 1.3 m in three sections: the phase-shifting input transformer (2 x 3 x 2, louvred for its
  fans), the power cells (2 x 3 x 2, the cells seen through the door windows), and the output and control section (1 x 3 x 2,
  the keypad and the output cable box).
"""
import gearlib as g
import mlib


def model(name, w, h, d):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(g.TEX), g.TEX["grey"])


def latches(M, x, z, ys):
	for y in ys:
		M.box((x, y, z - 0.008), (x + 0.02, y + 0.03, z), "galv")


def handle(M, x, y, z):
	"""A door-mounted disconnect handle, in the ON position."""
	M.box((x - 0.045, y - 0.07, z - 0.015), (x + 0.045, y + 0.07, z), "dark")
	M.box((x - 0.012, y - 0.01, z - 0.045), (x + 0.012, y + 0.14, z - 0.015), "rubber")


def lv_cabinet(name, drive):
	X1, X2, Z1, Z2, TOP = 0.2, 0.8, 0.2, 0.8, 2.0
	M = model(name, 1, 2, 1)
	M.box((X1, 0.0, Z1 + 0.02), (X2, 0.1, Z2), "iron")                          # the plinth
	M.box((X1, 0.1, Z1 + 0.012), (X2, TOP, Z2), "grey")                         # the enclosure
	M.box((X1 + 0.015, 0.14, Z1), (X2 - 0.015, TOP - 0.04, Z1 + 0.012), "grey")  # its door
	M.box((X1 - 0.004, TOP, Z1), (X2 + 0.004, TOP + 0.025, Z2 + 0.004), "grey")  # the top plate
	M.cylinder("y", (0.5, 0.55), mlib.NPS_OD[3] / 2 + 0.01, TOP + 0.025, TOP + 0.07, "galv", 12)   # the line conduit hub
	M.decal((0.35, 1.45, Z1 - 0.003), (0.65, 1.7, Z1), "controller", "north", "grey")             # the keypad
	M.decal((0.33, 1.25, Z1 - 0.003), (0.67, 1.38, Z1), "nameplate", "north", "steel")
	M.decal((0.32, 0.25, Z1 - 0.003), (0.68, 0.55, Z1), "danger", "north", "grey")
	M.box((0.42, 1.76, Z1 - 0.012), (0.46, 1.8, Z1), "bulb")                                        # READY
	M.box((0.54, 1.76, Z1 - 0.012), (0.58, 1.8, Z1), "dark")                                        # FAULT
	handle(M, X2 - 0.09, 1.1, Z1)
	if drive:
		M.box((X1 + 0.08, 1.82, Z1 - 0.006), (X2 - 0.08, TOP - 0.06, Z1), "louvre")                 # the fan's exhaust filter, high
		M.box((X1 + 0.08, 0.62, Z1 - 0.006), (X2 - 0.08, 0.98, Z1), "louvre")                       # the intake, low
	else:
		M.box((X1 + 0.1, 0.62, Z1 - 0.006), (X2 - 0.1, 0.9, Z1), "louvre")                          # the SCRs' heat sink vent
	latches(M, X1 + 0.03, Z1, (0.4, 1.0, 1.6))
	M.collide((X1, 0.0, Z1), (X2, TOP, Z2))
	return M


def soft_starter_mv():
	X1, X2, Z1, Z2, TOP = 0.045, 0.955, 0.05, 0.95, 2.29
	M = model("soft_starter_mv", 1, 3, 1)
	M.box((X1, 0.0, Z1 + 0.02), (X2, 0.08, Z2), "iron")
	M.box((X1, 0.08, Z1 + 0.012), (X2, TOP, Z2), "grey")
	M.box((X1 - 0.005, TOP, Z1), (X2 + 0.005, TOP + 0.03, Z2 + 0.005), "grey")
	doors = ((0.12, 0.86), (0.9, 1.56), (1.6, 2.24))   # the bypass contactor, the SCR stacks, the isolation switch
	for y1, y2 in doors:
		M.box((X1 + 0.02, y1, Z1), (X2 - 0.02, y2, Z1 + 0.012), "grey")
		latches(M, X1 + 0.04, Z1, (y1 + 0.1, y2 - 0.14))
	M.box((0.3, 1.82, Z1 - 0.006), (0.7, 2.12, Z1), "dark")                       # the isolation switch's window
	for x in (0.38, 0.5, 0.62):
		M.box((x - 0.02, 1.86, Z1 - 0.004), (x + 0.02, 2.08, Z1 - 0.002), "copper")   # its blades, seen through it
	handle(M, X2 - 0.12, 1.85, Z1)
	M.box((0.2, 1.0, Z1 - 0.006), (0.8, 1.2, Z1), "louvre")                        # the SCR stacks' vent
	M.decal((0.36, 1.26, Z1 - 0.003), (0.64, 1.5, Z1), "controller", "north", "grey")
	M.decal((0.3, 0.6, Z1 - 0.003), (0.7, 0.78, Z1), "nameplate", "north", "steel")
	M.decal((0.28, 0.2, Z1 - 0.003), (0.72, 0.52, Z1), "danger", "north", "grey")
	M.box((0.62, 1.42, Z1 - 0.012), (0.66, 1.46, Z1), "bulb")
	M.cylinder("y", (0.5, 0.5), mlib.NPS_OD[4] / 2 + 0.01, TOP + 0.03, TOP + 0.08, "galv", 12)
	M.collide((X1, 0.0, Z1), (X2, TOP, Z2))
	return M


# the 4.16 kV drive lineup: the sections butt together; the doors at z = 0.35, the back at 1.65
DZ1, DZ2, DTOP = 0.35, 1.65, 2.6


def lineup_shell(M, x1, x2):
	M.box((x1, 0.0, DZ1 + 0.02), (x2, 0.1, DZ2), "iron")
	M.box((x1, 0.1, DZ1 + 0.012), (x2, DTOP, DZ2), "grey")
	M.box((x1 - 0.004, DTOP, DZ1), (x2 + 0.004, DTOP + 0.03, DZ2 + 0.004), "grey")
	M.collide((x1, 0.0, DZ1), (x2, DTOP, DZ2))


def drive_mv_transformer():
	x1, x2 = 0.1, 2.0
	M = model("drive_mv_transformer", 2, 3, 2)
	lineup_shell(M, x1, x2)
	for a, b in ((x1 + 0.02, 1.04), (1.06, x2 - 0.02)):
		M.box((a, 0.14, DZ1), (b, DTOP - 0.05, DZ1 + 0.012), "grey")                   # two doors
		M.box((a + 0.1, 0.3, DZ1 - 0.006), (b - 0.1, 0.9, DZ1), "louvre")              # the cooling air in, low...
		M.box((a + 0.1, 1.9, DZ1 - 0.006), (b - 0.1, 2.4, DZ1), "louvre")              # ...and out, high
		latches(M, a + 0.03, DZ1, (0.5, 1.3, 2.1))
	M.decal((0.4, 1.25, DZ1 - 0.003), (0.8, 1.45, DZ1), "nameplate", "north", "steel")
	M.decal((1.2, 1.15, DZ1 - 0.003), (1.7, 1.55, DZ1), "danger", "north", "grey")
	for fx in (0.6, 1.5):                                                              # the transformer's roof fans
		M.cylinder("y", (fx, 1.0), 0.3, DTOP + 0.03, DTOP + 0.2, "grey", 16)
		M.box((fx - 0.28, DTOP + 0.2, 0.98), (fx + 0.28, DTOP + 0.22, 1.02), "dark")
		M.box((fx - 0.02, DTOP + 0.2, 0.72), (fx + 0.02, DTOP + 0.22, 1.28), "dark")
	M.box((x1 - 0.06, 0.2, 0.7), (x1, 0.6, 1.3), "grey")                                # the line cable box on the west end
	M.cylinder("x", (0.4, 1.0), mlib.NPS_OD[4] / 2 + 0.01, x1 - 0.1, x1 - 0.06, "galv", 12)
	return M


def drive_mv_cells():
	x1, x2 = 0.0, 2.0
	M = model("drive_mv_cells", 2, 3, 2)
	lineup_shell(M, x1, x2)
	for a, b in ((x1 + 0.02, 0.98), (1.02, x2 - 0.02)):
		M.box((a, 0.14, DZ1), (b, DTOP - 0.05, DZ1 + 0.012), "grey")
		latches(M, a + 0.03, DZ1, (0.5, 1.3, 2.1))
		for row in range(3):                                                           # a window on each phase's cell stack
			y = 0.45 + row * 0.62
			M.box((a + 0.1, y, DZ1 - 0.006), (b - 0.1, y + 0.4, DZ1), "dark")
			for c in range(3):                                                         # three cells to a stack, seen through it
				cx = a + 0.16 + c * (b - a - 0.32) / 3
				M.box((cx, y + 0.06, DZ1 - 0.004), (cx + (b - a - 0.32) / 3 - 0.04, y + 0.34, DZ1 - 0.002), "blue")
				M.box((cx + 0.03, y + 0.28, DZ1 - 0.0045), (cx + 0.07, y + 0.31, DZ1 - 0.004), "bulb")
	M.decal((0.7, 2.3, DZ1 - 0.003), (1.3, 2.45, DZ1), "danger", "north", "grey")
	return M


def drive_mv_control():
	x1, x2 = 0.0, 0.9
	M = model("drive_mv_control", 1, 3, 2)
	lineup_shell(M, x1, x2)
	M.box((x1 + 0.02, 1.3, DZ1), (x2 - 0.02, DTOP - 0.05, DZ1 + 0.012), "grey")    # the control door
	M.box((x1 + 0.02, 0.14, DZ1), (x2 - 0.02, 1.26, DZ1 + 0.012), "grey")          # the output section's door
	M.decal((0.25, 1.75, DZ1 - 0.003), (0.65, 2.1, DZ1), "controller", "north", "grey")
	M.decal((0.22, 1.45, DZ1 - 0.003), (0.68, 1.6, DZ1), "nameplate", "north", "steel")
	M.decal((0.2, 0.4, DZ1 - 0.003), (0.7, 0.8, DZ1), "danger", "north", "grey")
	M.decal((0.3, 2.2, DZ1 - 0.003), (0.6, 2.4, DZ1), "pushbuttons", "north", "grey")
	M.box((0.66, 2.12, DZ1 - 0.012), (0.7, 2.16, DZ1), "bulb")
	latches(M, x1 + 0.04, DZ1, (0.5, 1.6, 2.2))
	M.box((x2, 0.2, 0.7), (x2 + 0.06, 0.6, 1.3), "grey")                                # the motor cable box on the east end
	M.cylinder("x", (0.4, 1.0), mlib.NPS_OD[4] / 2 + 0.01, x2 + 0.06, x2 + 0.1, "galv", 12)
	return M


M = lv_cabinet("soft_starter", False)
def plc_cabinet():
	"""A floor-standing PLC enclosure, 0.8 x 2.0 x 0.6 m: its door, the status lights and keyswitch above the window onto the rail, and the cable gland below."""
	X1, X2, Z1, Z2, TOP = 0.1, 0.9, 0.2, 0.8, 2.0
	M = model("plc_cabinet", 1, 2, 1)
	M.box((X1, 0.0, Z1 + 0.02), (X2, 0.1, Z2), "iron")
	M.box((X1, 0.1, Z1 + 0.012), (X2, TOP, Z2), "grey")
	M.box((X1 + 0.015, 0.14, Z1), (X2 - 0.015, TOP - 0.04, Z1 + 0.012), "grey")
	M.box((X1 - 0.004, TOP, Z1), (X2 + 0.004, TOP + 0.025, Z2 + 0.004), "grey")
	M.decal((0.28, 0.85, Z1 - 0.003), (0.72, 1.45, Z1), "controller", "north", "grey")          # the window on the rail
	M.decal((0.33, 0.68, Z1 - 0.003), (0.67, 0.8, Z1), "nameplate", "north", "steel")
	M.box((0.3, 1.62, Z1 - 0.012), (0.36, 1.68, Z1), "bulb")                                    # RUN
	M.box((0.4, 1.62, Z1 - 0.012), (0.46, 1.68, Z1), "bulb")                                    # FAULT
	M.box((0.6, 1.58, Z1 - 0.02), (0.7, 1.72, Z1), "dark")                                      # the keyswitch
	latches(M, X2 - 0.05, Z1, (0.5, 1.0, 1.6))
	M.cylinder("y", (0.5, 0.5), 0.03, 0.0, 0.1, "galv", 10)                                     # the cable gland
	M.collide((X1, 0.0, Z1), (X2, TOP, Z2))
	return M


EXTRA = [plc_cabinet(), lv_cabinet("drive_cabinet", True), soft_starter_mv(), drive_mv_transformer(), drive_mv_cells(), drive_mv_control()]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
