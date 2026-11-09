"""The 10d3 panels and switches at true size, each drawn facing north (the front toward -z, the wall at the back, z = 1 of its cell):
- load_centre, 1 x 1 x 1 (docs/multiblocks/panelboards.md): a 200 A main-breaker load centre 0.36 m wide, 1.0 m tall and 0.1 m deep,
  its door, the main's handle through the dead front, the service conduit into its top;
- panelboard_480, 1 x 2 x 1: a 225 A 480Y/277 V panelboard 0.5 m wide, 1.8 m tall and 0.15 m deep, its door and main;
- panel_gutter, 1 x 1 x 1: a wireway cover filling the cell's wall, holding one branch breaker (empty, on, off or tripped); its
  circuit leaves the front in branch cable;
- safety_switch_100 and safety_switch_200 (safety-switch.md): heavy-duty fusible switches 0.3 x 0.6 m and 0.45 x 1.1 m, the handle on
  the right side (up ON, down OFF), conduit into the top and out of the bottom;
- lis_5kv and lis_5kv_fused, 1 x 3 x 2 (load-interrupter.md): a 5 kV metal-enclosed switch 0.9 m wide, 2.3 m tall and 1.5 m deep, the
  operating handle (up CLOSED, down OPEN), the viewing window onto the blades (and the fuses on the fused one), the cable box on the
  roof at the back and the load throat low on the back; wrecked, it is scorched and its door sprung;
- ats_480, 1 x 2 x 1 (ats.md): a 400 A transfer switch 0.75 m wide, 1.9 m tall and 0.5 m deep, its controller in the door, the
  source lamps, conduit into the top and the back and out of the bottom.
after() writes the blockstates of the parts with more states than facing and piece."""
import gearlib as g
import mlib


def wall_box(M, x1, x2, y1, y2, depth, tex="grey"):
	"""A wall-mounted enclosure with its door, the back against z = 1."""
	z1 = 1.0 - depth
	M.box((x1, y1, z1 + 0.012), (x2, y2, 1.0), tex)
	M.box((x1 + 0.01, y1 + 0.01, z1), (x2 - 0.01, y2 - 0.01, z1 + 0.012), tex)            # the door
	M.box((x1 - 0.004, y2 - 0.08, z1 + 0.01), (x1 + 0.008, y2 - 0.03, z1 + 0.03), "galv")  # hinges on the left
	M.box((x1 - 0.004, y1 + 0.03, z1 + 0.01), (x1 + 0.008, y1 + 0.08, z1 + 0.03), "galv")
	M.collide((x1, y1, z1), (x2, y2, 1.0))
	return z1


def conduit(M, x, z, y1, y2):
	r = mlib.NPS_OD[1] / 2
	M.cylinder("y", (x, z), r, y1, y2, "galv", 8)


def handle(M, x, y, z, up, length=0.12, width=0.03):
	"""A handle on the front at (x, y), pointing up (on) or down (off)."""
	M.box((x - 0.03, y - 0.03, z - 0.012), (x + 0.03, y + 0.03, z), "dark")
	if up:
		M.box((x - width / 2, y, z - 0.035), (x + width / 2, y + length, z - 0.012), "rubber")
	else:
		M.box((x - width / 2, y - length, z - 0.035), (x + width / 2, y, z - 0.012), "rubber")


# ---- the load centre and the panelboard -----------------------------------------------------------------------------------------------
def main_handle(M, x, y, z, slot):
	"""The main breaker's handle through a window in the dead front: up ON, down OFF, centred with its flag out TRIPPED."""
	M.box((x - 0.07, y - 0.09, z - 0.004), (x + 0.07, y + 0.09, z), "dark")                 # the window onto the main
	tilt = {"on": 0.035, "off": -0.035, "tripped": 0.0}[slot]
	M.box((x - 0.04, y - 0.02 + tilt, z - 0.03), (x + 0.04, y + 0.02 + tilt, z - 0.004), "rubber")
	if slot == "tripped":
		M.box((x - 0.05, y + 0.055, z - 0.007), (x + 0.05, y + 0.08, z - 0.004), "danger")   # the trip flag


def load_centre(slot):
	L = mlib.Model(f"load_centre{'' if slot == 'on' else '_' + slot}", mlib.footprint_box(1, 1, 1, 0, 0), dict(g.TEX), g.TEX["grey"])
	z = wall_box(L, 0.32, 0.68, 0.0, 0.98, 0.1)
	L.decal((0.36, 0.78, z - 0.002), (0.64, 0.9, z), "nameplate", "north", "grey")
	L.decal((0.36, 0.08, z - 0.002), (0.64, 0.22, z), "danger", "north", "grey")
	L.box((0.6, 0.45, z - 0.012), (0.63, 0.55, z), "galv")                                   # the door latch
	main_handle(L, 0.47, 0.62, z, slot)
	conduit(L, 0.5, 0.95, 0.98, 1.0)
	return L


def panelboard(slot):
	P = mlib.Model(f"panelboard_480{'' if slot == 'on' else '_' + slot}", mlib.footprint_box(1, 2, 1, 0, 0), dict(g.TEX), g.TEX["grey"])
	z = wall_box(P, 0.25, 0.75, 0.1, 1.9, 0.15)
	P.decal((0.3, 1.55, z - 0.002), (0.7, 1.75, z), "nameplate", "north", "grey")
	P.decal((0.3, 0.2, z - 0.002), (0.7, 0.4, z), "danger", "north", "grey")
	P.box((0.68, 0.9, z - 0.012), (0.72, 1.1, z), "galv")
	main_handle(P, 0.5, 1.32, z, slot)
	conduit(P, 0.5, 0.92, 1.9, 2.0)
	return P


LOAD_CENTRES = [load_centre(s) for s in ("on", "off", "tripped")]
PANELBOARDS = [panelboard(s) for s in ("on", "off", "tripped")]
L = LOAD_CENTRES[0]
P = PANELBOARDS[0]


# ---- the gutter -----------------------------------------------------------------------------------------------------------------------
def gutter(slot):
	M = mlib.Model(f"panel_gutter{'' if slot == 'empty' else '_' + slot}", mlib.footprint_box(1, 1, 1, 0, 0), dict(g.TEX), g.TEX["grey"])
	M.box((0.0, 0.0, 0.9), (1.0, 1.0, 1.0), "grey")                                        # the trough
	M.box((0.02, 0.02, 0.888), (0.98, 0.98, 0.9), "grey")                                  # its cover
	for x, y in ((0.06, 0.06), (0.92, 0.06), (0.06, 0.92), (0.92, 0.92)):
		M.box((x, y, 0.882), (x + 0.02, y + 0.02, 0.888), "galv")                          # the cover's screws
	if slot == "empty":
		M.box((0.4, 0.42, 0.884), (0.6, 0.58, 0.888), "steel")                             # the blank in the breaker opening
	else:
		M.box((0.38, 0.3, 0.86), (0.62, 0.7, 0.888), "dark")                               # the breaker
		tilt = {"on": 0.06, "off": -0.06, "tripped": 0.0}[slot]
		M.box((0.46, 0.47 + tilt, 0.835), (0.54, 0.53 + tilt, 0.86), "rubber")             # its handle: up on, down off, centre tripped
		if slot == "tripped":
			M.box((0.43, 0.62, 0.857), (0.57, 0.66, 0.86), "danger")                       # the trip flag
		M.decal((0.4, 0.33, 0.858), (0.6, 0.42, 0.86), "nameplate", "north", "dark")
	M.collide((0.0, 0.0, 0.88), (1.0, 1.0, 1.0))
	return M


GUTTERS = [gutter(s) for s in ("empty", "on", "off", "tripped")]


# ---- the safety switches --------------------------------------------------------------------------------------------------------------
def safety(name, height, x1, x2, y1, y2, depth, on):
	M = mlib.Model(f"{name}{'' if on else '_off'}", mlib.footprint_box(1, height, 1, 0, 0), dict(g.TEX), g.TEX["grey"])
	z = wall_box(M, x1, x2, y1, y2, depth)
	M.decal((x1 + 0.04, y2 - 0.16, z - 0.002), (x2 - 0.04, y2 - 0.06, z), "nameplate", "north", "grey")
	M.decal((x1 + 0.04, y1 + 0.05, z - 0.002), (x2 - 0.04, y1 + 0.17, z), "danger", "north", "grey")
	hy = (y1 + y2) / 2                                                                      # the handle on the right side
	M.box((x2, hy - 0.05, 1.0 - depth * 0.6), (x2 + 0.03, hy + 0.05, 1.0 - depth * 0.3), "dark")
	if on:
		M.box((x2 + 0.01, hy, 1.0 - depth * 0.5), (x2 + 0.04, hy + 0.18, 1.0 - depth * 0.4), "danger")
	else:
		M.box((x2 + 0.01, hy - 0.18, 1.0 - depth * 0.5), (x2 + 0.04, hy, 1.0 - depth * 0.4), "danger")
	cz = 1.0 - depth / 2
	conduit(M, (x1 + x2) / 2, cz, y2, height)                                              # the line in at the top
	conduit(M, (x1 + x2) / 2, cz, 0.0, y1)                                                 # the load out of the bottom
	return M


S1 = [safety("safety_switch_100", 1, 0.35, 0.65, 0.2, 0.8, 0.2, on) for on in (True, False)]
S2 = [safety("safety_switch_200", 2, 0.275, 0.725, 0.45, 1.55, 0.22, on) for on in (True, False)]


# ---- the 5 kV load-interrupter switches -----------------------------------------------------------------------------------------------
def lis(name, fused, state):
	M = mlib.Model(f"{name}{'' if state == 'closed' else '_' + state}", mlib.footprint_box(1, 3, 2, 0, 0), dict(g.TEX), g.TEX["grey"])
	Z1, Z2 = 0.25, 1.75
	M.box((0.05, 0.0, Z1 + 0.05), (0.95, 0.1, Z2), "dark")                                  # the base channel
	M.box((0.05, 0.1, Z1 + 0.02), (0.95, 2.3, Z2), "grey")                                  # the enclosure
	M.box((0.04, 2.3, Z1), (0.96, 2.36, Z2 + 0.02), "grey")                                 # its roof
	scorched = state == "wrecked"
	door = (0.1, 0.2, Z1), (0.9, 2.2, Z1 + 0.02)
	if scorched:
		M.box((0.1, 0.2, Z1 - 0.12), (0.12, 2.2, Z1 + 0.02), "grey")                       # the door sprung open on its hinge
		M.box((0.12, 0.25, Z1 + 0.015), (0.88, 2.15, Z1 + 0.02), "dark")                   # the scorched inside
	else:
		M.box(*door, "grey")
		M.box((0.2, 1.2, Z1 - 0.005), (0.7, 1.9, Z1), "dark")                              # the viewing window onto the blades
		M.box((0.25, 1.35 if state == "closed" else 1.6, Z1 - 0.007), (0.65, 1.45 if state == "closed" else 1.7, Z1 - 0.005), "steel")
		if fused:
			for x in (0.27, 0.42, 0.57):
				M.box((x, 0.7, Z1 - 0.005), (x + 0.08, 1.05, Z1), "porcelain")             # the fuses, seen through the lower window
			M.box((0.22, 0.65, Z1 - 0.003), (0.7, 1.1, Z1 - 0.001), "dark")
		M.decal((0.2, 0.3, Z1 - 0.002), (0.7, 0.55, Z1), "danger", "north", "grey")
		M.decal((0.2, 2.0, Z1 - 0.002), (0.7, 2.12, Z1), "nameplate", "north", "steel")
	# the operating handle on the right, its shaft through the front
	hx, hy = 0.82, 1.3
	M.box((hx - 0.05, hy - 0.05, Z1 - 0.03), (hx + 0.05, hy + 0.05, Z1), "dark")
	if state == "closed":
		M.box((hx - 0.02, hy, Z1 - 0.06), (hx + 0.02, hy + 0.35, Z1 - 0.03), "danger")
	else:
		M.box((hx - 0.02, hy - 0.35, Z1 - 0.06), (hx + 0.02, hy, Z1 - 0.03), "danger")
	M.box((0.3, 2.36, 1.2), (0.7, 3.0, 1.7), "steel")                                       # the line cable box on the roof at the back
	M.box((0.25, 0.15, Z2), (0.75, 0.85, 2.0), "steel")                                     # the load throat, low on the back
	M.collide((0.04, 0.0, Z1), (0.96, 2.36, Z2 + 0.02))
	return M


LIS = [lis("lis_5kv", False, s) for s in ("closed", "open", "wrecked")]
LISF = [lis("lis_5kv_fused", True, s) for s in ("closed", "open", "wrecked")]


# ---- the transfer switch --------------------------------------------------------------------------------------------------------------
A = mlib.Model("ats_480", mlib.footprint_box(1, 2, 1, 0, 0), dict(g.TEX), g.TEX["grey"])
z = wall_box(A, 0.125, 0.875, 0.05, 1.95, 0.5)
A.decal((0.2, 1.2, z - 0.002), (0.6, 1.7, z), "relays", "north", "grey")                   # the controller
A.box((0.68, 1.55, z - 0.012), (0.74, 1.61, z), "padgreen")                                # NORMAL available
A.box((0.68, 1.42, z - 0.012), (0.74, 1.48, z), "danger")                                  # EMERGENCY available
A.box((0.68, 1.29, z - 0.012), (0.74, 1.35, z), "bulb")                                    # the source in use
A.decal((0.2, 0.95, z - 0.002), (0.6, 1.08, z), "nameplate", "north", "steel")
A.decal((0.2, 0.15, z - 0.002), (0.6, 0.35, z), "danger", "north", "grey")
A.box((0.8, 0.9, z - 0.012), (0.84, 1.1, z), "galv")                                       # the door latch
conduit(A, 0.5, 0.75, 1.95, 2.0)                                                           # NORMAL in at the top
conduit(A, 0.5, 0.75, 0.0, 0.05)                                                           # LOAD out of the bottom

M = L
EXTRA = LOAD_CENTRES[1:] + PANELBOARDS + [A] + GUTTERS + S1 + S2 + LIS + LISF
for extra in (A,):
	extra.export_args = {"blockstate": True, "item": True}
for group in (LOAD_CENTRES, PANELBOARDS, GUTTERS, S1, S2, LIS, LISF):
	group[0].export_args = {"blockstate": True, "item": True}   # its shapes and item; its blockstate is rewritten in after()
	for other in group[1:]:
		other.export_args = {"blockstate": False, "item": False}


def after():
	# a one-cell part's piece property still runs 0 to 1 (BigParts.pieceProperty): both show the one cell
	g.save_blockstate("load_centre", {f"facing={facing},main={slot},piece={piece}": {"model": f"wildcatter:block/load_centre{'' if slot == 'on' else '_' + slot}/p0", **({"y": y} if y else {})}
		for facing, y in g.ROT.items() for piece in range(2) for slot in ("on", "off", "tripped")})
	g.save_blockstate("panelboard_480", {f"facing={facing},main={slot},piece={piece}": {"model": f"wildcatter:block/panelboard_480{'' if slot == 'on' else '_' + slot}/p{piece}", **({"y": y} if y else {})}
		for facing, y in g.ROT.items() for piece in range(2) for slot in ("on", "off", "tripped")})
	g.save_blockstate("panel_gutter", {f"facing={facing},piece={piece},slot={slot}": {"model": f"wildcatter:block/panel_gutter{'' if slot == 'empty' else '_' + slot}/p0", **({"y": y} if y else {})}
		for facing, y in g.ROT.items() for piece in range(2) for slot in ("empty", "on", "off", "tripped")})
	for name, height in (("safety_switch_100", 1), ("safety_switch_200", 2)):
		g.save_blockstate(name, {f"facing={facing},on={'true' if on else 'false'},piece={piece}": {"model": f"wildcatter:block/{name}{'' if on else '_off'}/p{min(piece, height - 1)}", **({"y": y} if y else {})}
			for facing, y in g.ROT.items() for piece in range(max(2, height)) for on in (True, False)})
	for name in ("lis_5kv", "lis_5kv_fused"):
		g.save_blockstate(name, {f"facing={facing},piece={piece},state={state}": {"model": f"wildcatter:block/{name}{'' if state == 'closed' else '_' + state}/p{piece}", **({"y": y} if y else {})}
			for facing, y in g.ROT.items() for piece in range(6) for state in ("closed", "open", "wrecked")})
