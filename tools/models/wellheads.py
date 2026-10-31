"""The wellheads at true size (docs/multiblocks/well-site.md), drawn facing north over a well at the centre of the cell:
- oil_wellhead, one cell: a rod-pumping wellhead about 1.1 m tall: the casing head with its two side-outlet valves, the tubing head,
  the pumping tee with its flowline outlet and check valve, and the stuffing box the pumpjack's polished rod runs through;
- gas_wellhead, 1 x 2 x 1: a 2 1/16 in 5,000 psi Christmas tree about 2 m tall: the casing and tubing heads, the lower and upper
  master valves, the flow cross with its wing valve and adjustable choke toward the front, the swab valve and the tree cap with
  its pressure gauge."""
import math
import mlib
import plantlib as g

TEX = dict(g.TEX)
BODY = "padgreen"


def flange(M, y, r=0.17, t=0.05):
	M.cylinder("y", (0.5, 0.5), r, y, y + t, "iron", 16)
	for k in range(8):
		a = math.radians(22.5 + k * 45)
		M.cylinder("y", (0.5 + (r - 0.025) * math.cos(a), 0.5 + (r - 0.025) * math.sin(a)), 0.012, y - 0.02, y + t + 0.02, "galv", 6)


def gate_valve(M, at, y, length, tex=BODY):
	"""A gate valve on a side outlet along x (body, bonnet up, handwheel on top), centred at x = at."""
	M.box((at - length / 2, y - 0.06, 0.44), (at + length / 2, y + 0.06, 0.56), tex)
	M.cylinder("y", (at, 0.5), 0.025, y + 0.06, y + 0.2, "steel", 8)   # the stem
	M.ring("y", (at, 0.5), 0.08, y + 0.2, y + 0.22, 0.012, "rubber", sides=12)


def oil():
	M = mlib.Model("oil_wellhead", [(0, 0, 0)], TEX, TEX[BODY])
	M.cylinder("y", (0.5, 0.5), 0.12, -0.2, 0.05, "steel", 12, caps=False)   # the 7 in casing up out of the ground
	flange(M, 0.05, 0.19)
	M.cylinder("y", (0.5, 0.5), 0.14, 0.1, 0.38, BODY, 14)                  # the casing head
	for side in (-1, 1):   # its side outlets and valves
		M.pipe([(0.5 + side * 0.14, 0.24, 0.5), (0.5 + side * 0.36, 0.24, 0.5)], 2, "steel")
		gate_valve(M, 0.5 + side * 0.3, 0.24, 0.1)
	flange(M, 0.38, 0.16)
	M.cylinder("y", (0.5, 0.5), 0.1, 0.43, 0.62, BODY, 12)                  # the tubing head
	flange(M, 0.62, 0.12, 0.04)
	M.cylinder("y", (0.5, 0.5), 0.06, 0.66, 0.9, "steel", 10)               # the pumping tee
	M.pipe([(0.5, 0.78, 0.5), (1.0, 0.78, 0.5)], 2, "steel")                # the flowline outlet, east
	M.box((0.7, 0.72, 0.45), (0.82, 0.84, 0.55), BODY)                     # its check valve
	M.cylinder("y", (0.5, 0.5), 0.075, 0.9, 1.08, "brass", 12)             # the stuffing box
	M.cylinder("y", (0.5, 0.5), 0.085, 1.04, 1.1, "iron", 12)
	M.cylinder("z", (0.36, 0.8), 0.04, 0.44, 0.47, "brass", 10)             # a pressure gauge on the tee
	M.decal((0.33, 0.77, 0.435), (0.39, 0.83, 0.44), "gauge", "north", "brass")
	M.collide((0.31, 0.0, 0.31), (0.69, 1.1, 0.69))
	return M


def gas():
	M = mlib.Model("gas_wellhead", mlib.footprint_box(1, 2, 1, 0, 0), TEX, TEX[BODY])
	M.cylinder("y", (0.5, 0.5), 0.12, -0.2, 0.05, "steel", 12, caps=False)
	flange(M, 0.05, 0.2)
	M.cylinder("y", (0.5, 0.5), 0.15, 0.1, 0.4, BODY, 14)                   # the casing head and its side valves
	for side in (-1, 1):
		M.pipe([(0.5 + side * 0.15, 0.25, 0.5), (0.5 + side * 0.38, 0.25, 0.5)], 2, "steel")
		gate_valve(M, 0.5 + side * 0.3, 0.25, 0.1)
	flange(M, 0.4, 0.17)
	M.cylinder("y", (0.5, 0.5), 0.11, 0.45, 0.62, BODY, 12)                 # the tubing head
	flange(M, 0.62, 0.13, 0.04)
	for y in (0.66, 0.94):   # the lower and upper master valves, their handwheels to the east
		M.box((0.4, y, 0.4), (0.6, y + 0.24, 0.6), BODY)
		M.cylinder("x", (y + 0.12, 0.5), 0.025, 0.6, 0.78, "steel", 8)
		M.ring("x", (y + 0.12, 0.5), 0.1, 0.78, 0.8, 0.014, "rubber", sides=12)
		flange(M, y + 0.24, 0.11, 0.03)
	M.box((0.42, 1.21, 0.42), (0.58, 1.39, 0.58), "steel")                 # the flow cross
	M.pipe([(0.5, 1.3, 0.42), (0.5, 1.3, 0.0)], 2, "steel")                 # the wing to the front
	M.box((0.42, 1.22, 0.24), (0.58, 1.38, 0.36), BODY)                    # the wing valve
	M.ring("y", (0.5, 0.3), 0.08, 1.42, 1.44, 0.012, "rubber", sides=12)
	M.cylinder("y", (0.5, 0.3), 0.02, 1.38, 1.42, "steel", 8)
	M.box((0.44, 1.24, 0.08), (0.56, 1.36, 0.18), "iron")                  # the adjustable choke
	M.cylinder("y", (0.5, 0.13), 0.03, 1.36, 1.46, "brass", 8)
	M.box((0.4, 1.39, 0.4), (0.6, 1.63, 0.6), BODY)                        # the swab valve
	M.cylinder("x", (1.51, 0.5), 0.025, 0.6, 0.78, "steel", 8)
	M.ring("x", (1.51, 0.5), 0.08, 0.78, 0.8, 0.012, "rubber", sides=12)
	M.cylinder("y", (0.5, 0.5), 0.08, 1.63, 1.78, "iron", 12)              # the tree cap
	M.cylinder("y", (0.5, 0.5), 0.02, 1.78, 1.86, "brass", 6)
	M.cylinder("z", (0.5, 1.92), 0.055, 0.47, 0.53, "brass", 12)           # its pressure gauge
	M.decal((0.45, 1.87, 0.465), (0.55, 1.97, 0.47), "gauge", "north", "brass")
	M.collide((0.3, 0.0, 0.3), (0.7, 1.86, 0.7))
	M.collide((0.42, 1.22, 0.0), (0.58, 1.38, 0.3))
	return M


M = gas()
OIL = oil()
OIL.export_args = {"blockstate": False, "item": True}
EXTRA = [OIL]


def after():
	import gearlib
	gearlib.save_blockstate("oil_wellhead", {f"facing={facing}": {"model": "wildcatter:block/oil_wellhead/p0", **({"y": y} if y else {})} for facing, y in gearlib.ROT.items()})
