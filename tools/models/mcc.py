"""The motor control center at true size (docs/multiblocks/motor-control-center.md): NEMA sections 20 in (0.51 m) wide, 90 in
(2.29 m) tall and 20 in deep, each 1 x 3 x 1, drawn facing north (the front toward -z).
- The incoming section: the main breaker's door low (its handle on, off or tripped), the cable pull box at the top with its hub,
  the nameplate and warning label.
- The starter section: three bucket spaces, one per cell, beside the vertical wireway door. An empty space is a blank cover;
  a bucket has its disconnect handle, start/stop buttons and run and overload lights (lit as it runs or trips).
Each look is a model per piece; after() writes the blockstates with the main breaker's position and each cell's bucket."""
import gearlib as g
import mlib

X1, X2 = 0.245, 0.755   # 0.51 m wide
Z1, Z2 = 0.245, 0.755   # 0.51 m deep, the front at Z1
TOP = 2.29


def section(M):
	M.box((X1, 0.0, Z1 + 0.02), (X2, 0.08, Z2), "iron")                        # the floor sill
	M.box((X1, 0.08, Z1 + 0.01), (X2, TOP, Z2), "grey")                        # the section
	M.box((X1 - 0.005, TOP, Z1), (X2 + 0.005, TOP + 0.03, Z2 + 0.005), "grey")  # its top plate
	M.box((X1 + 0.03, TOP + 0.03, 0.45), (X1 + 0.08, TOP + 0.09, 0.55), "galv")   # lifting angle
	M.box((X2 - 0.08, TOP + 0.03, 0.45), (X2 - 0.03, TOP + 0.09, 0.55), "galv")
	M.collide((X1, 0.0, Z1), (X2, TOP, Z2))


def incoming(position):
	M = mlib.Model(f"mcc_incoming{'' if position == 'on' else '_' + position}", mlib.footprint_box(1, 3, 1, 0, 0), dict(g.TEX), g.TEX["grey"])
	section(M)
	M.box((X1 + 0.02, 0.12, Z1), (X2 - 0.02, 1.05, Z1 + 0.01), "grey")          # the main breaker's door
	M.box((X1 + 0.02, 1.1, Z1), (X2 - 0.02, 2.24, Z1 + 0.01), "grey")           # the pull box door
	hub = (0.5, 0.6)
	M.box((hub[0] - 0.07, hub[1] - 0.07, Z1 - 0.015), (hub[0] + 0.07, hub[1] + 0.07, Z1), "dark")
	tilt = {"on": 0.0, "off": 90.0, "tripped": 45.0}[position]
	if tilt == 90.0:
		M.box((hub[0] - 0.02, hub[1] - 0.015, Z1 - 0.045), (hub[0] + 0.16, hub[1] + 0.015, Z1 - 0.015), "rubber")
	else:
		M.box((hub[0] - 0.015, hub[1] - 0.02, Z1 - 0.045), (hub[0] + 0.015, hub[1] + 0.16, Z1 - 0.015), "rubber", rot=("z", -tilt, (hub[0], hub[1], Z1)))
	M.decal((X1 + 0.06, 0.85, Z1 - 0.002), (X2 - 0.06, 0.98, Z1), "nameplate", "north", "steel")
	M.decal((X1 + 0.06, 1.7, Z1 - 0.002), (X2 - 0.06, 2.0, Z1), "danger", "north", "grey")
	M.cylinder("y", (0.5, 0.5), mlib.NPS_OD[3] / 2 + 0.01, TOP + 0.03, TOP + 0.08, "galv", 12)   # the conduit hub on top
	for y in (0.3, 0.9, 1.3, 2.0):
		M.box((X1 + 0.03, y, Z1 - 0.008), (X1 + 0.05, y + 0.03, Z1), "galv")    # door latches
	return M


def starter_section(bucket):
	M = mlib.Model(f"mcc_section{'' if bucket == 'empty' else '_' + bucket}", mlib.footprint_box(1, 3, 1, 0, 0), dict(g.TEX), g.TEX["grey"])
	section(M)
	WX = X2 - 0.12   # the vertical wireway's door, on the right as you face the front
	M.box((WX, 0.12, Z1), (X2 - 0.015, 2.24, Z1 + 0.01), "grey")
	for k in range(3):
		y1, y2 = k + 0.12, k + 0.92
		M.box((X1 + 0.015, y1, Z1), (WX - 0.01, y2, Z1 + 0.01), "grey")          # the bucket's door or a blank cover
		M.box((X1 + 0.03, y1 + 0.02, Z1 - 0.006), (X1 + 0.05, y1 + 0.06, Z1), "galv")
		if bucket == "empty":
			for y in (y1 + 0.05, y2 - 0.07):
				M.box((0.45, y, Z1 - 0.004), (0.47, y + 0.02, Z1), "galv")      # the cover's screws
			continue
		M.decal((0.43, y1 + 0.18, Z1 - 0.003), (0.6, y1 + 0.58, Z1), "pushbuttons", "north", "grey")
		run, trip = bucket == "running", bucket == "tripped"
		M.box((0.44, y1 + 0.62, Z1 - 0.012), (0.48, y1 + 0.66, Z1), "bulb" if run else "dark")     # RUN
		M.box((0.53, y1 + 0.62, Z1 - 0.012), (0.57, y1 + 0.66, Z1), "brass" if trip else "dark")   # OVERLOAD
		hx, hy = X1 + 0.08, y1 + 0.35                                               # the disconnect handle
		M.box((hx - 0.04, hy - 0.06, Z1 - 0.015), (hx + 0.04, hy + 0.06, Z1), "dark")
		M.box((hx - 0.012, hy - 0.01, Z1 - 0.04), (hx + 0.012, hy + 0.12, Z1 - 0.015), "rubber")
		M.decal((0.43, y1 + 0.05, Z1 - 0.003), (0.6, y1 + 0.13, Z1), "nameplate", "north", "steel")
	return M


INCOMING = [incoming(p) for p in ("on", "off", "tripped")]
SECTIONS = [starter_section(b) for b in ("empty", "stopped", "running", "tripped")]
M = INCOMING[0]
INCOMING[1].export_args = SECTIONS[1].export_args = SECTIONS[2].export_args = SECTIONS[3].export_args = {"blockstate": False, "item": False}
INCOMING[2].export_args = {"blockstate": False, "item": False}
SECTIONS[0].export_args = {"blockstate": True, "item": True}   # its shapes and item; its blockstate is rewritten in after()
EXTRA = INCOMING[1:] + SECTIONS


def after():
	g.save_blockstate("mcc_incoming", {f"facing={facing},piece={piece},position={position}": {"model": f"wildcatter:block/mcc_incoming{'' if position == 'on' else '_' + position}/p{piece}", **({"y": y} if y else {})}
		for facing, y in g.ROT.items() for piece in range(3) for position in ("on", "off", "tripped")})
	g.save_blockstate("mcc_section", {f"bucket={bucket},facing={facing},piece={piece}": {"model": f"wildcatter:block/mcc_section{'' if bucket == 'empty' else '_' + bucket}/p{piece}", **({"y": y} if y else {})}
		for facing, y in g.ROT.items() for piece in range(3) for bucket in ("empty", "stopped", "running", "tripped")})
