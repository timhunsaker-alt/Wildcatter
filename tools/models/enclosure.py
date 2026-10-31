"""The in-line building gear at true size (docs/multiblocks/building-gear.md): every enclosed breaker, the safety switch, the
frequency relay, the fused disconnect and the combination starter, each on a wall and on a Unistrut stand, in each of its states.
Drawn facing north (the line from the south face in to the north face out), the wall to the west, the door east."""
import gearlib as g

# block: (width along the line, height, depth, states, what is on its door)
GEAR = {
	"breaker": (0.15, 0.25, 0.1, ("on", "off", "tripped"), "toggle"),
	"switch": (0.2, 0.35, 0.12, ("on", "off", "tripped"), "side_lever"),
	"ufls_relay": (0.25, 0.3, 0.15, ("on", "off", "tripped"), "relay"),
	"fuse": (0.15, 0.2, 0.1, ("false", "true"), "pullout"),
	"breaker_3p_lv": (0.3, 0.5, 0.15, ("on", "off", "tripped"), "toggle3"),
	"breaker_3p_industrial": (0.4, 0.8, 0.2, ("on", "off", "tripped"), "rotary"),
	"motor_starter": (0.4, 0.75, 0.25, ("stopped", "running", "tripped"), "starter"),
}
STATE_KEY = {"fuse": "blown", "motor_starter": "run"}


def door(M, kind, state, x, z1, z2, y0, y1):
	"""What shows on the door: the handle in its position, the relay's face, the starter's buttons and lights."""
	zc, yc = (z1 + z2) / 2, (y0 + y1) / 2
	tilt = {"on": 30.0, "off": -30.0, "tripped": 0.0}.get(state, 0.0)
	if kind in ("toggle", "toggle3"):
		half = 0.012 if kind == "toggle" else 0.05
		M.box((x, yc - 0.03, zc - half - 0.01), (x + 0.012, yc + 0.03, zc + half + 0.01), "dark")          # the escutcheon
		M.box((x + 0.012, yc - 0.008, zc - half), (x + 0.045, yc + 0.008, zc + half), "rubber", rot=("z", tilt, (x + 0.012, yc, zc)))
		M.decal((x, y1 - 0.07, z1 + 0.02), (x + 0.002, y1 - 0.02, z2 - 0.02), "nameplate", "east", "steel")
	elif kind == "side_lever":
		up = state == "on"
		M.box((x - 0.06, yc - 0.02, z1 - 0.012), (x - 0.02, yc + 0.02, z1), "dark")                         # the hub on the side
		M.box((x - 0.05, yc, z1 - 0.022), (x - 0.03, yc + (0.12 if up else -0.12), z1 - 0.012), "rubber")
		M.decal((x, yc - 0.06, z1 + 0.03), (x + 0.002, yc + 0.06, z2 - 0.03), "danger", "east", "grey")
	elif kind == "relay":
		M.decal((x, y1 - 0.15, z1 + 0.02), (x + 0.002, y1 - 0.02, z2 - 0.02), "relays", "east", "grey")
		M.box((x, y0 + 0.05, zc - 0.02), (x + 0.012, y0 + 0.11, zc + 0.02), "dark")
		M.box((x + 0.012, y0 + 0.07, zc - 0.01), (x + 0.04, y0 + 0.09, zc + 0.01), "rubber", rot=("z", tilt, (x + 0.012, y0 + 0.08, zc)))
	elif kind == "pullout":
		out = 0.04 if state == "true" else 0.0
		M.box((x, yc - 0.05, zc - 0.04), (x + 0.02 + out, yc + 0.05, zc + 0.04), "dark")                   # the pullout
		M.box((x + 0.02 + out, yc - 0.01, zc - 0.03), (x + 0.035 + out, yc + 0.01, zc + 0.03), "rubber")    # its grip
		if state == "true":
			M.box((x, yc - 0.05, zc + 0.045), (x + 0.004, yc - 0.01, zc + 0.06), "copper")                 # the blown indicator
	elif kind == "rotary":
		M.box((x, yc - 0.05, zc - 0.05), (x + 0.015, yc + 0.05, zc + 0.05), "dark")
		if state == "off":
			M.box((x + 0.015, yc - 0.012, zc - 0.11), (x + 0.04, yc + 0.012, zc + 0.02), "rubber")
		else:
			M.box((x + 0.015, yc - 0.02, zc - 0.012), (x + 0.04, yc + 0.11, zc + 0.012), "rubber", rot=("x", 0.0 if state == "on" else 40.0, (x, yc, zc)))
		M.decal((x, y1 - 0.12, z1 + 0.04), (x + 0.002, y1 - 0.04, z2 - 0.04), "nameplate", "east", "steel")
		M.decal((x, y0 + 0.05, z1 + 0.05), (x + 0.002, y0 + 0.2, z2 - 0.05), "danger", "east", "grey")
	elif kind == "starter":
		M.decal((x, yc - 0.12, zc - 0.07), (x + 0.002, yc + 0.2, zc + 0.07), "pushbuttons", "east", "grey")
		run, trip = state == "running", state == "tripped"
		M.box((x, yc + 0.22, zc - 0.05), (x + 0.012, yc + 0.25, zc - 0.02), "bulb" if run else "dark")      # RUN light
		M.box((x, yc + 0.22, zc + 0.02), (x + 0.012, yc + 0.25, zc + 0.05), "brass" if trip else "dark")    # OVERLOAD light
		M.box((x, y0 + 0.06, z1 + 0.03), (x + 0.02, y0 + 0.16, z1 + 0.09), "dark")                          # the disconnect handle
		M.box((x + 0.02, y0 + 0.1, z1 + 0.035), (x + 0.04, y0 + 0.2 if state != "stopped" else y0 + 0.13, z1 + 0.085), "rubber")


def look(block, mount, state):
	w, h, d, states, kind = GEAR[block]
	M = g.model(f"{block}_{mount}_{state}")
	wall = mount == "wall"
	y0 = g.MOUNT_Y - h / 2 if wall else min(0.15, 0.95 - h)
	x, z1, z2, y0, y1 = g.enclosure(M, w, h, d, y0, wall)
	door(M, kind, state, x, z1, z2, y0, y1)
	g.collide_enclosure(M, w, h, d, y0, wall)
	return M


LOOKS = []
for block, (_, _, _, states, _) in GEAR.items():
	LOOKS += g.single([look(block, mount, state) for mount in ("floor", "wall") for state in states], block)
M = LOOKS[0]
EXPORT = LOOKS[0].export_args
EXTRA = LOOKS[1:]


def after():
	for block, (_, _, _, states, _) in GEAR.items():
		key = STATE_KEY.get(block, "position")
		g.save_blockstate(block, {f"facing={facing},{key}={state},mount={mount}": {"model": f"wildcatter:block/{block}_{mount}_{state}/p0", **({"y": y} if y else {})}
			for facing, y in g.ROT.items() for state in states for mount in ("floor", "wall")})
