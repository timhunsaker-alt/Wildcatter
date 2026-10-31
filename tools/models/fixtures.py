"""Gear wired on every face at true size (docs/multiblocks/building-gear.md): the receptacles and their plugs, the lamp, the
1,500 W wall heater and the 15 kW unit heater, each on a wall (drawn on the north wall; the blockstate turns it), on the floor
and on the ceiling. Receptacle and plug faces are their existing face textures."""
import gearlib as g

PLUGS = ["nema_5_15", "nema_5_20", "nema_14_50", "cee_7", "iec_60309_blue", "nema_l21_30", "nema_l22_30", "iec_60309_red_32", "iec_60309_red_63"]
# plate width and height, box depth: a duplex plate is 0.07 x 0.115 m; single 50 A and twist-lock plates 0.115 square; an IEC
# 60309 wall socket's housing about 0.1 x 0.14 x 0.09 m
PLATE = {"nema_5_15": (0.07, 0.115, 0.05), "nema_5_20": (0.07, 0.115, 0.05), "nema_14_50": (0.115, 0.115, 0.06), "cee_7": (0.08, 0.08, 0.045),
	"nema_l21_30": (0.115, 0.115, 0.06), "nema_l22_30": (0.115, 0.115, 0.06), "iec_60309_blue": (0.1, 0.14, 0.09),
	"iec_60309_red_32": (0.1, 0.14, 0.09), "iec_60309_red_63": (0.12, 0.16, 0.1)}
WHERE = ("wall", "floor", "ceiling")
WALL_V = 0.4   # receptacles sit lower on a wall than switches


def receptacle(kind, where):
	M = g.model(f"receptacle_{kind}_{where}", {"face": f"block/receptacle_{kind}"}, "steel")
	F = g.Face(M, where)
	w, h, d = PLATE[kind]
	v = WALL_V if where == "wall" else 0.5
	if where == "floor":   # a floor box: a brass plate on a flush box
		F.box((0.5 - w / 2 - 0.03, v - h / 2 - 0.03, 0.0), (0.5 + w / 2 + 0.03, v + h / 2 + 0.03, 0.012), "brass")
		F.decal((0.5 - w / 2, v - h / 2, 0.012), (0.5 + w / 2, v + h / 2, 0.016), "face", "brass")
	else:
		F.box((0.5 - w / 2 + 0.005, v - h / 2 + 0.005, 0.0), (0.5 + w / 2 - 0.005, v + h / 2 - 0.005, d - 0.004), "galv")   # the box
		F.decal((0.5 - w / 2, v - h / 2, d - 0.004), (0.5 + w / 2, v + h / 2, d), "face", "steel")                  # the plate and its face
		if kind.startswith("iec"):   # the hinged lid, up
			F.box((0.5 - w / 2, v + h / 2 - 0.01, d), (0.5 + w / 2, v + h / 2 + 0.005, d + 0.06), "polymer")
	F.collide((0.5 - w / 2, v - h / 2, 0.0), (0.5 + w / 2, v + h / 2, max(d, 0.02)))
	return M


def plug(kind, where):
	"""The plug in its receptacle's face (the receptacle is beyond this cell's face), its cord running out across the cell."""
	M = g.model(f"plug_{kind}_{where}", {"face": f"block/plug_{kind}"}, "rubber")
	F = g.Face(M, where)
	w, h, _ = PLATE[kind]
	v = WALL_V if where == "wall" else 0.5
	r = min(w, h) * 0.38
	F.cylinder(0.5, v, r, 0.0, 0.07, "rubber", 12)                     # the plug body
	F.decal((0.5 - r * 0.7, v - r * 0.7, 0.07), (0.5 + r * 0.7, v + r * 0.7, 0.074), "face", "rubber")
	F.cylinder(0.5, v, r * 0.45, 0.074, 0.12, "rubber", 8)             # strain relief
	F.cylinder(0.5, v, 0.006 if w < 0.1 else 0.01, 0.12, 1.0, "rubber", 6)   # the cord
	F.collide((0.5 - r, v - r, 0.0), (0.5 + r, v + r, 0.12))
	return M


def lamp(where, lit):
	"""A keyless porcelain lampholder with a 60 W A19 lamp in a vapour-tight glass jar and its cast guard."""
	M = g.model(f"electric_lamp_{where}{'_lit' if lit else ''}")
	F = g.Face(M, where)
	v = 0.6 if where == "wall" else 0.5
	glass = "bulb" if lit else "polymer"
	F.cylinder(0.5, v, 0.065, 0.0, 0.03, "iron", 12)                   # the cast box cover
	F.cylinder(0.5, v, 0.04, 0.03, 0.06, "porcelain", 12)              # the lampholder
	F.cylinder(0.5, v, 0.014, 0.06, 0.08, "brass", 8)
	for w1, w2, rr in ((0.08, 0.1, 0.022), (0.1, 0.13, 0.03), (0.13, 0.16, 0.03), (0.16, 0.175, 0.022)):   # the A19 bulb, 60 mm across
		F.cylinder(0.5, v, rr, w1, w2, glass, 10)
	for du, dv in ((-0.05, 0), (0.05, 0), (0, 0.05), (0, -0.05)):      # the guard's bars over the jar
		F.box((0.5 + du - 0.005, v + dv - 0.005, 0.03), (0.5 + du + 0.005, v + dv + 0.005, 0.2), "iron")
	F.cylinder(0.5, v, 0.055, 0.195, 0.205, "iron", 10)
	F.collide((0.43, v - 0.07, 0.0), (0.57, v + 0.07, 0.2))
	return M


def wall_heater(where, lit):
	"""A 1,500 W fan-forced wall heater, about 0.4 x 0.5 x 0.1 m, its grille the heater's face; on the floor it stands on feet."""
	M = g.model(f"electric_heater_{where}{'_lit' if lit else ''}", {"face": "block/electric_heater_lit" if lit else "block/electric_heater"}, "blue")
	if where == "floor":   # upright on its feet, its face to the south
		M.box((0.3, 0.06, 0.4), (0.7, 0.56, 0.5), "blue")
		M.decal((0.32, 0.08, 0.5), (0.68, 0.54, 0.504), "face", "south", "blue")
		for x in (0.32, 0.62):
			M.box((x, 0.0, 0.33), (x + 0.06, 0.06, 0.57), "dark")
		M.collide((0.3, 0.0, 0.33), (0.7, 0.56, 0.57))
		return M
	F = g.Face(M, where)
	v = 0.45 if where == "wall" else 0.5
	F.box((0.3, v - 0.25, 0.0), (0.7, v + 0.25, 0.1), "blue")
	F.decal((0.32, v - 0.23, 0.1), (0.68, v + 0.23, 0.104), "face", "blue")
	F.box((0.62, v + 0.18, 0.1), (0.66, v + 0.22, 0.12), "dark")      # the thermostat knob
	F.collide((0.3, v - 0.25, 0.0), (0.7, v + 0.25, 0.1))
	return M


def unit_heater(where, lit):
	"""A 15 kW 480 V industrial unit heater, about 0.6 x 0.6 x 0.45 m: louvres in front, the fan guard behind, on its bracket."""
	M = g.model(f"three_phase_heater_{where}{'_lit' if lit else ''}", {"face": "block/three_phase_heater_lit" if lit else "block/three_phase_heater"}, "grey")
	F = g.Face(M, where)
	v = 0.5
	gap = 0.06 if where != "floor" else 0.3   # off its bracket, or up on its stand
	F.box((0.2, v - 0.3, gap), (0.8, v + 0.3, gap + 0.45), "grey")
	F.decal((0.22, v - 0.28, gap + 0.45), (0.78, v + 0.28, gap + 0.454), "face", "grey")
	F.cylinder(0.5, v, 0.24, gap - 0.03, gap, "galv", 16)              # the fan guard
	for u in (0.3, 0.7):
		F.box((u - 0.02, v - 0.02, 0.0), (u + 0.02, v + 0.02, gap), "galv")   # bracket arms or stand legs
	if where == "floor":
		F.box((0.25, v - 0.3, 0.0), (0.75, v + 0.3, 0.01), "galv")
	F.collide((0.2, v - 0.3, 0.0), (0.8, v + 0.3, gap + 0.45))
	return M


LOOKS = []
for kind in PLUGS:
	LOOKS += g.single([receptacle(kind, w) for w in WHERE], f"receptacle_{kind}")
	LOOKS += g.single([plug(kind, w) for w in WHERE], f"plug_{kind}")
for name, make in (("electric_lamp", lamp), ("electric_heater", wall_heater), ("three_phase_heater", unit_heater)):
	LOOKS += g.single([make(w, lit) for lit in (False, True) for w in WHERE], name)
M = LOOKS[0]
EXPORT = LOOKS[0].export_args
EXTRA = LOOKS[1:]


def attached(name, suffix=""):
	"""attach=<face> to its look: the four walls turn the north-wall look; the floor and ceiling have their own."""
	out = {}
	for face in ("north", "east", "south", "west", "down", "up"):
		where = {"down": "floor", "up": "ceiling"}.get(face, "wall")
		v = {"model": f"wildcatter:block/{name}_{where}{suffix}/p0"}
		if where == "wall" and g.ROT[face]:
			v["y"] = g.ROT[face]
		out[face] = v
	return out


def after():
	for kind in PLUGS:
		for name in (f"receptacle_{kind}", f"plug_{kind}"):
			g.save_blockstate(name, {f"attach={face}": v for face, v in attached(name).items()})
	for name in ("electric_lamp", "electric_heater", "three_phase_heater"):
		g.save_blockstate(name, {f"attach={face},lit={lit}": v for lit in ("false", "true") for face, v in attached(name, "_lit" if lit == "true" else "").items()})
