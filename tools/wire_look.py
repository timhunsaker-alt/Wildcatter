#!/usr/bin/env python3
"""Wires, cables and pipes that read as connected (Tim, 2026-11-27): wire and cable drawn thicker in four steps by conductor size
(the `gauge` blockstate, energy/ConductorSize#gauge), the single-phase cable (hot, neutral, ground), and pipes with a flange pair at a
machine's nozzle and a welded fitting only where a run turns, branches or ends (straight runs stay plain).

The terminal lugs and cable glands at machines are drawn by client/TerminalRenderer, not here.

Run from the repo root: python3 tools/wire_look.py. tools/make_assets.py runs it at its end, so a full asset rebuild keeps these files.
"""
import json, os, random
from PIL import Image, ImageDraw

ROOT = "common/src/main/resources"
ASSETS = f"{ROOT}/assets/wildcatter"
DATA = f"{ROOT}/data/wildcatter"
ns = "wildcatter"
LOADER_RECIPES = {"fabric": "fabric/src/main/resources/data/wildcatter/recipe", "neoforge": "neoforge/src/main/resources/data/wildcatter/recipe"}

GAUGES = ("small", "medium", "large", "very_large")
SUFFIX = {"small": "", "medium": "_medium", "large": "_large", "very_large": "_very_large"}
WIRE_WIDTH = (2, 3, 4, 5)       # pixels: 12 and 10 AWG keep the old 2-pixel wire (block/WireShape)
CABLE_WIDTH = (4, 5, 6, 7)      # three-phase cable keeps its old 4 pixels at 12 and 10 AWG (block/MultiCableBlock)
SINGLE_WIDTH = (3, 4, 5, 6)     # single-phase cable, a step thinner
ARMS = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
CONDUCTORS = ("a", "b", "c", "n", "g")
BUILDING = ["awg_12", "awg_10", "awg_6", "awg_2", "awg_4_0", "kcmil_500"]
UPSIZE = {"awg_10": 2, "awg_6": 3, "awg_2": 3, "awg_4_0": 3, "kcmil_500": 2}


def save_json(path, obj):
	os.makedirs(os.path.dirname(path), exist_ok=True)
	with open(path, "w") as f:
		json.dump(obj, f, indent=2)
		f.write("\n")


def cuboid(frm, to, texture="#cable"):
	faces = {f: {"uv": [0, 0, 16, 16], "texture": texture} for f in ("north", "south", "east", "west", "up", "down")}
	return {"from": frm, "to": to, "faces": faces}


def square(width, z0, z1, texture="#cable"):
	a, b = 8 - width / 2, 8 + width / 2
	return cuboid([a, a, z0], [b, b, z1], texture)


def shapes(kind, widths):
	"""Parent models for one kind of conductor: its core and its north arm at each gauge, textured by #cable."""
	for gauge, w in zip(GAUGES, widths):
		a, b = 8 - w / 2, 8 + w / 2
		save_json(f"{ASSETS}/models/block/conductor/{kind}_core_{gauge}.json", {"elements": [cuboid([a, a, a], [b, b, b])]})
		save_json(f"{ASSETS}/models/block/conductor/{kind}_arm_{gauge}.json", {"elements": [cuboid([a, a, 0], [b, b, a])]})


def textured(name, parent, texture):
	save_json(f"{ASSETS}/models/block/{name}.json", {"parent": f"{ns}:block/conductor/{parent}", "textures": {"cable": texture, "particle": texture}})


def conductor_parts(model_base, kind, texture, when):
	"""Multipart entries for one look (one texture) at every gauge, each under `when`."""
	parts = []
	for gauge in GAUGES:
		core, arm = f"{model_base}_core{SUFFIX[gauge]}", f"{model_base}_arm{SUFFIX[gauge]}"
		textured(core, f"{kind}_core_{gauge}", texture)
		textured(arm, f"{kind}_arm_{gauge}", texture)
		cond = dict(when, gauge=gauge)
		parts.append({"when": cond, "apply": {"model": f"{ns}:block/{core}"}})
		for side, rot in ARMS.items():
			parts.append({"when": {"AND": [{side: "true"}] + [{k: v} for k, v in cond.items()]}, "apply": {"model": f"{ns}:block/{arm}", **rot}})
	return parts


def wires():
	shapes("wire", WIRE_WIDTH)
	bare = ("wire", "mv_wire", "dist_wire", "hv_wire", "industrial_wire")
	plastic = ("plastic_wire", "mv_plastic_wire", "dist_plastic_wire", "hv_plastic_wire", "industrial_plastic_wire")
	for base in bare + plastic + ("wool_wire",):
		wool = base == "wool_wire"
		parts = []
		for key in CONDUCTORS:
			for wear in (range(4) if wool else [None]):
				look = f"{base}_{key}" + (f"_{wear}" if wear is not None else "")
				when = {"conductor": key, **({"wear": str(wear)} if wear is not None else {})}
				parts += conductor_parts(look, "wire", f"{ns}:block/{look}", when)
		for role in ("hot", "neutral"):
			save_json(f"{ASSETS}/blockstates/{base}_{role}.json", {"multipart": parts})


def cable_texture(cable):
	"""The texture a cable is drawn in now (Ludo art where there is some, else its placeholder)."""
	core = f"{ASSETS}/models/block/{cable}_core.json"
	t = json.load(open(core)).get("textures", {}) if os.path.exists(core) else {}
	return t.get("cable", f"{ns}:block/{cable}")


def single_phase_texture():
	"""Placeholder (only while the Ludo art in art/final is missing): an ivory thermoplastic sheath with its printed legend stripe."""
	if os.path.exists("art/final/single_phase_cable.png"):
		return
	random.seed(11)
	img = Image.new("RGBA", (16, 16))
	d = ImageDraw.Draw(img)
	for x in range(16):
		for y in range(16):
			n = random.randint(-5, 5)
			img.putpixel((x, y), (226 + n, 220 + n, 200 + n, 255))
	d.line([0, 7, 15, 7], fill=(196, 188, 164))
	for x in range(1, 16, 4):
		d.line([x, 10, x + 1, 10], fill=(120, 112, 96))
	img.save(f"{ASSETS}/textures/block/single_phase_cable_placeholder.png")


def SINGLE_TEXTURE():
	"""The single-phase cable's sheath: Ludo art (process_art.py) when it has been made, else the placeholder."""
	return f"{ns}:block/single_phase_cable" if os.path.exists("art/final/single_phase_cable.png") else f"{ns}:block/single_phase_cable_placeholder"


def cables():
	shapes("cable", CABLE_WIDTH)
	shapes("single_cable", SINGLE_WIDTH)
	for cable in ("cable_lv", "cable_industrial", "cable_mv", "cable_dist", "cable_hv", "single_phase_cable"):
		single = cable == "single_phase_cable"
		texture = SINGLE_TEXTURE() if single else cable_texture(cable)
		parts = conductor_parts(cable, "single_cable" if single else "cable", texture, {})
		save_json(f"{ASSETS}/blockstates/{cable}.json", {"multipart": parts})
	w = SINGLE_WIDTH[0]
	t = {"cable": SINGLE_TEXTURE(), "particle": SINGLE_TEXTURE()}
	a, b = 8 - w / 2, 8 + w / 2
	save_json(f"{ASSETS}/models/block/single_phase_cable_inventory.json", {"parent": "minecraft:block/block",
		"display": {"gui": {"rotation": [30, 225, 0], "scale": [1.1, 1.1, 1.1]}, "ground": {"scale": [0.8, 0.8, 0.8]},
			"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.8, 0.8, 0.8]}, "thirdperson_righthand": {"scale": [0.8, 0.8, 0.8]}},
		"textures": t, "elements": [cuboid([0, a, a], [16, b, b]), cuboid([a, a, 0], [b, b, 16])]})
	save_json(f"{ASSETS}/items/single_phase_cable.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/single_phase_cable_inventory"}})
	save_json(f"{DATA}/loot_table/blocks/single_phase_cable.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
		"entries": [{"type": "minecraft:item", "name": f"{ns}:single_phase_cable"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def single_phase_recipes():
	"""One hot (red) wire, one neutral (black) wire and one ground wire of a size make a metre of single-phase cable of that size; a
	bigger size from several of the next smaller one, as for every conductor. Sized ingredients are loader data (Phase 9b)."""
	def fabric(item, size):
		return {"fabric:type": "fabric:components", "base": f"{ns}:{item}", "components": {f"{ns}:conductor_size": size}}

	def neoforge(item, size):
		return {"neoforge:ingredient_type": "neoforge:components", "items": f"{ns}:{item}", "components": {f"{ns}:conductor_size": size}, "strict": False}

	for loader, sized in (("fabric", fabric), ("neoforge", neoforge)):
		out = LOADER_RECIPES[loader]
		for name in os.listdir(out) if os.path.isdir(out) else []:
			if name.startswith("single_phase_cable"):
				os.remove(f"{out}/{name}")
		for insulation in ("wool_", "plastic_"):
			for size in BUILDING:
				save_json(f"{out}/single_phase_cable__{insulation}{size}.json", {"type": "minecraft:crafting_shapeless", "category": "redstone",
					"ingredients": [sized(f"{insulation}wire_hot", size), sized(f"{insulation}wire_neutral", size), f"{ns}:ground_wire"],
					"result": {"id": f"{ns}:single_phase_cable", "count": 1, "components": {f"{ns}:conductor_size": size}}})
		for small, big in zip(BUILDING, BUILDING[1:]):
			save_json(f"{out}/single_phase_cable__{big}_from_{small}.json", {"type": "minecraft:crafting_shapeless", "category": "redstone",
				"ingredients": [sized("single_phase_cable", small)] * UPSIZE[big],
				"result": {"id": f"{ns}:single_phase_cable", "count": 1, "components": {f"{ns}:conductor_size": big}}})


def pipe_set(name, texture, od, fitting, flange_od, flange_t):
	"""A pipe's run core (its own diameter), its welded fitting, its arm, and a flange pair at a nozzle, in pixels from metres."""
	t = {"pipe": texture, "particle": texture}
	p = lambda m: m * 16
	save_json(f"{ASSETS}/models/block/{name}_run.json", {"textures": t, "elements": [square(p(od), 8 - p(od) / 2, 8 + p(od) / 2, "#pipe")]})
	c = p(fitting)
	save_json(f"{ASSETS}/models/block/{name}_core.json", {"textures": t, "elements": [cuboid([8 - c / 2] * 3, [8 + c / 2] * 3, "#pipe")]})
	save_json(f"{ASSETS}/models/block/{name}_arm.json", {"textures": t, "elements": [square(p(od), 0, 8 - p(od) / 2, "#pipe")]})
	ft = max(p(flange_t), 0.45)   # each flange, a little thicker than life so it reads
	save_json(f"{ASSETS}/models/block/{name}_flange.json", {"textures": t, "elements": [
		square(p(flange_od), 0, ft, "#pipe"),                                    # the machine's nozzle flange
		square(p(flange_od) - 0.3, ft + 0.12, 2 * ft + 0.12, "#pipe"),            # the pipe's flange, bolted to it
		square(p(od) * 1.35, 2 * ft + 0.12, 2 * ft + 0.12 + max(p(od) * 0.5, 0.6), "#pipe")]})   # its weld-neck hub


def not_straight():
	"""Multipart OR: every pattern of arms that is not a straight run (exactly two opposite arms)."""
	sides = ("north", "south", "east", "west", "up", "down")
	straight = ({"north", "south"}, {"east", "west"}, {"up", "down"})
	out = []
	for mask in range(64):
		on = {s for i, s in enumerate(sides) if mask >> i & 1}
		if on in straight:
			continue
		out.append({s: ("!false" if s in on else "false") for s in sides})
	return {"OR": out}


def pipe_blockstate(name, core_model):
	parts = [{"apply": {"model": f"{ns}:block/{name}_run"}}, {"when": not_straight(), "apply": {"model": f"{ns}:block/{core_model}"}}]
	for side, rot in ARMS.items():
		parts.append({"when": {side: "true|flange"}, "apply": {"model": f"{ns}:block/{name}_arm", **rot}})
		parts.append({"when": {side: "flange"}, "apply": {"model": f"{ns}:block/{name}_flange", **rot}})
	return {"multipart": parts}


def pipes():
	# 2 in NPS water pipe (60.3 mm) with a 152 mm ASME B16.5 class 150 flange; 4 in NPS steel line (114 mm), 229 mm flange; the 600 mm
	# slurry pipe and air header, 813 mm flanges
	galv = f"{ns}:block/mat/mat_galvanized"
	rubber = f"{ns}:block/mat/mat_rubber"
	for name, texture, od, fitting, flange_od, flange_t in (("pipe", f"{ns}:block/pipe", 0.0603, 0.085, 0.152, 0.018),
			("gas_line", f"{ns}:block/gas_line", 0.1143, 0.15, 0.229, 0.024), ("slurry_pipe", rubber, 0.61, 0.75, 0.813, 0.06),
			("air_header", galv, 0.61, 0.75, 0.813, 0.06)):
		pipe_set(name, texture, od, fitting, flange_od, flange_t)
		save_json(f"{ASSETS}/blockstates/{name}.json", pipe_blockstate(name, f"{name}_core"))
	# valves: their body in place of the fitting, arms to pipe or flange, a flange pair where one sits on a machine
	for valve, pipe in (("control_valve", "pipe"), ("on_off_valve", "pipe"), ("motor_operated_valve", "pipe"), ("control_valve_steel", "gas_line"),
			("on_off_valve_steel", "gas_line"), ("motor_operated_valve_steel", "gas_line")):
		path = f"{ASSETS}/blockstates/{valve}.json"
		parts = [p for p in json.load(open(path))["multipart"] if "when" not in p]   # the valve's own body
		for side, rot in ARMS.items():
			parts.append({"when": {side: "true|flange"}, "apply": {"model": f"{ns}:block/{pipe}_arm", **rot}})
			parts.append({"when": {side: "flange"}, "apply": {"model": f"{ns}:block/{pipe}_flange", **rot}})
		save_json(path, {"multipart": parts})


def lang():
	path = f"{ASSETS}/lang/en_us.json"
	names = json.load(open(path))
	names["block.wildcatter.single_phase_cable"] = "Single-Phase Cable (LV, hot, neutral, ground)"
	save_json(path, names)


def tags():
	"""The First power quest's wiring: either separate wool wires or single-phase cable."""
	for tag, wire in (("lamp_hot_wiring", "wool_wire_hot"), ("lamp_neutral_wiring", "wool_wire_neutral")):
		save_json(f"{DATA}/tags/item/{tag}.json", {"values": [f"{ns}:{wire}", f"{ns}:single_phase_cable"]})


def run():
	wires()
	single_phase_texture()
	cables()
	single_phase_recipes()
	pipes()
	lang()
	tags()


if __name__ == "__main__":
	run()
