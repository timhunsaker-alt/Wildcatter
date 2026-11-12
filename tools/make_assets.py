#!/usr/bin/env python3
"""Draws the placeholder textures and writes the model/blockstate/lang/recipe/loot JSON.

Placeholders only: real art comes from Ludo once the shapes settle. Run from the repo root: python3 tools/make_assets.py
"""
import json, os, random, shutil
from PIL import Image, ImageDraw

ROOT = "common/src/main/resources"
ASSETS = f"{ROOT}/assets/wildcatter"
DATA = f"{ROOT}/data/wildcatter"
ns = "wildcatter"
random.seed(7)

# Start clean so renamed or removed blocks do not leave stale files behind.
for path in (f"{ASSETS}/blockstates", f"{ASSETS}/models", f"{ASSETS}/items", f"{ASSETS}/textures", DATA):
	shutil.rmtree(path, ignore_errors=True)


def save_json(path, obj):
	os.makedirs(os.path.dirname(path), exist_ok=True)
	with open(path, "w") as f:
		json.dump(obj, f, indent=2)
		f.write("\n")


def tex(name, draw, kind="block"):
	img = Image.new("RGBA", (16, 16))
	draw(img, ImageDraw.Draw(img))
	path = f"{ASSETS}/textures/{kind}/{name}.png"
	os.makedirs(os.path.dirname(path), exist_ok=True)
	img.save(path)


def noise(img, base, spread):
	for x in range(16):
		for y in range(16):
			d = random.randint(-spread, spread)
			img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))


def steel(img, d, base=(110, 118, 126)):
	noise(img, base, 6)
	d.rectangle([0, 0, 15, 15], outline=(70, 76, 84))
	for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
		d.point((x, y), fill=(190, 196, 204))


# ---- machines -------------------------------------------------------------------------------------------
def boiler_side(lit):
	def draw(img, d):
		steel(img, d, (120, 96, 88) if not lit else (130, 100, 90))
		d.rectangle([4, 4, 11, 11], fill=(40, 40, 46), outline=(60, 60, 66))  # gauge window
		if lit:
			d.rectangle([5, 7, 10, 10], fill=(255, 150, 40))
			d.rectangle([6, 8, 9, 10], fill=(255, 220, 90))
		else:
			d.rectangle([5, 7, 10, 10], fill=(70, 50, 40))
	return draw


def boiler_top(img, d):
	steel(img, d, (95, 100, 108))
	d.ellipse([3, 3, 12, 12], outline=(60, 64, 70), fill=(85, 90, 98))
	d.rectangle([7, 7, 8, 8], fill=(180, 186, 194))


def boiler_bottom(img, d):
	steel(img, d, (70, 72, 78))


def turbine_side(running):
	def draw(img, d):
		steel(img, d, (126, 130, 138))
		d.rectangle([1, 6, 14, 9], fill=(70, 74, 82))
		for x in range(2, 14, 3):
			d.line([x, 6, x + 1, 9], fill=(200, 205, 212) if running else (140, 145, 152))
	return draw


def turbine_end(running):
	def draw(img, d):
		steel(img, d, (100, 104, 112))
		d.ellipse([2, 2, 13, 13], fill=(50, 54, 60), outline=(150, 155, 162))
		c = (230, 235, 240) if running else (130, 135, 142)
		for dx, dy in ((0, -4), (4, 0), (0, 4), (-4, 0), (3, -3), (3, 3), (-3, 3), (-3, -3)):
			d.line([7.5, 7.5, 7.5 + dx, 7.5 + dy], fill=c)
		d.rectangle([6, 6, 9, 9], fill=(200, 160, 60))
	return draw


def lamp(lit):
	def draw(img, d):
		noise(img, (250, 232, 150) if lit else (170, 160, 120), 8)
		d.rectangle([0, 0, 15, 15], outline=(110, 90, 50))
		d.line([0, 8, 15, 8], fill=(110, 90, 50))
		d.line([8, 0, 8, 15], fill=(110, 90, 50))
		if lit:
			d.rectangle([4, 4, 11, 11], fill=(255, 250, 200))
	return draw


def heater(lit):
	def draw(img, d):
		steel(img, d, (140, 60, 50))
		d.rectangle([2, 2, 13, 13], fill=(30, 28, 28), outline=(70, 70, 70))
		for y in (4, 7, 10):
			d.line([3, y, 12, y], fill=(255, 140, 40) if lit else (90, 60, 50), width=2)
	return draw


def transformer_front(base, glow):
	def draw(img, d):
		steel(img, d, base)
		for x in (3, 8, 12):  # porcelain insulators
			d.rectangle([x, 3, x + 1, 12], fill=glow, outline=(60, 60, 66))
	return draw


def transformer_side(base):
	def draw(img, d):
		steel(img, d, base)
		for y in (4, 7, 10, 13):  # cooling fins
			d.line([2, y, 13, y], fill=(base[0] - 35, base[1] - 35, base[2] - 35))
	return draw


def transformer_top(base):
	def draw(img, d):
		steel(img, d, base)
		d.rectangle([5, 5, 10, 10], outline=(50, 54, 60), fill=(base[0] - 20, base[1] - 20, base[2] - 20))
	return draw


def plain(base, spread=6):
	def draw(img, d):
		noise(img, base, spread)
	return draw


def breaker_body(img, d):
	noise(img, (70, 72, 78), 5)
	d.rectangle([0, 0, 15, 15], outline=(30, 32, 36))
	d.rectangle([3, 11, 12, 14], fill=(200, 200, 205))  # the rating label


def lever(color):
	def draw(img, d):
		noise(img, color, 8)
	return draw


# ---- wire ---------------------------------------------------------------------------------------------------
HOT = (200, 50, 45)
NEUTRAL = (25, 25, 28)
COPPER = (196, 118, 72)
ALUMINIUM = (170, 176, 184)
STEEL_WIRE = (140, 150, 160)


def bare(metal, band):
	"""Bare metal with a coloured band so you can tell hot from neutral."""
	def draw(img, d):
		noise(img, metal, 8)
		for y in (3, 8, 12):
			d.line([0, y, 15, y], fill=tuple(c - 40 for c in metal))
		d.line([0, 5, 15, 5], fill=band)
		d.line([0, 10, 15, 10], fill=band)
	return draw


def wool(color, wear):
	"""Wool wrap. At wear 1 and 2 the fibres thin out and copper shows through; at 3 it is bare copper again."""
	def draw(img, d):
		noise(img, color, 14)
		holes = {0: 0, 1: 22, 2: 45, 3: 100}[wear]
		for x in range(16):
			for y in range(16):
				if random.randint(0, 99) < holes:
					img.putpixel((x, y), tuple(max(0, min(255, c + random.randint(-12, 12))) for c in COPPER) + (255,))
		if wear == 3:
			d.line([0, 5, 15, 5], fill=color)
			d.line([0, 10, 15, 10], fill=color)
	return draw


def firebox_side(lit):
	def draw(img, d):
		noise(img, (120, 60, 50), 10)
		d.rectangle([3, 4, 12, 14], fill=(30, 28, 30), outline=(50, 50, 55))
		if lit:
			d.rectangle([4, 8, 11, 13], fill=(255, 140, 40))
			d.rectangle([5, 10, 10, 13], fill=(255, 220, 90))
	return draw


tex("firebox_side", firebox_side(False))
tex("firebox_side_lit", firebox_side(True))
tex("firebox_top", boiler_bottom)
tex("pump", plain((50, 90, 160)))
tex("pipe", bare((110, 130, 120), (80, 100, 90)))
tex("boiler_side", boiler_side(False))
tex("boiler_side_lit", boiler_side(True))
tex("boiler_top", boiler_top)
tex("boiler_bottom", boiler_bottom)
tex("steam_turbine_side", turbine_side(False))
tex("steam_turbine_side_running", turbine_side(True))
tex("steam_turbine_end", turbine_end(False))
tex("steam_turbine_end_running", turbine_end(True))
tex("electric_lamp", lamp(False))
tex("electric_lamp_lit", lamp(True))
tex("electric_heater", heater(False))
tex("electric_heater_lit", heater(True))
tex("breaker_body", breaker_body)


def fuse_tube(blown):
	def draw(img, d):
		noise(img, (120, 70, 40) if not blown else (50, 40, 36), 6)
		if not blown:
			d.rectangle([0, 4, 15, 11], fill=(214, 220, 226, 255))
			d.line([0, 8, 15, 8], fill=(190, 120, 70, 255))
		else:
			d.rectangle([0, 4, 15, 11], fill=(70, 66, 64, 255))
			d.line([4, 8, 7, 8], fill=(40, 36, 34, 255))
	return draw


tex("fuse_tube", fuse_tube(False))
tex("fuse_tube_blown", fuse_tube(True))
tex("fuse_cap", plain((196, 130, 70)))
tex("cutout_body", plain((206, 188, 158), 5))
tex("cutout_tube", plain((196, 122, 70), 6))


def tinted_breaker(color):
	def draw(img, d):
		breaker_body(img, d)
		for x in range(16):
			for y in range(16):
				r, g, b, a = img.getpixel((x, y))
				img.putpixel((x, y), (min(255, (r + color[0]) // 2), min(255, (g + color[1]) // 2), min(255, (b + color[2]) // 2), a))
	return draw


tex("switch_body", tinted_breaker((200, 170, 40)))
tex("relay_body", tinted_breaker((150, 70, 190)))


def wood(base, grain):
	def draw(img, d):
		noise(img, base, 8)
		for x in (2, 5, 9, 12):
			d.line([x, 0, x, 15], fill=grain)
	return draw


# the pole and crossarm are creosoted wood, the Ludo material (art wave); the old script grain is still drawn, unsaved, so the noise
# sequence of the textures after it stays put
_scratch = Image.new("RGBA", (16, 16)); wood((110, 78, 48), (80, 55, 34))(_scratch, ImageDraw.Draw(_scratch))
tex("insulator", plain((200, 200, 210)))
tex("breaker_lever_on", lever((60, 190, 90)))
tex("breaker_lever_off", lever((120, 120, 126)))
tex("breaker_lever_tripped", lever((220, 60, 50)))
for name, t in (("distribution", ((60, 115, 85), (205, 235, 215))), ("pole_tx", ((112, 118, 124), (232, 232, 224)))):
	tex(f"{name}_front", transformer_front(*t))
	tex(f"{name}_side", transformer_side(t[0]))
	tex(f"{name}_top", transformer_top(t[0]))

# Every wire is drawn once per conductor: the coloured band says which one it carries (A red, B orange, C blue, N black, G green).
CONDUCTOR_COLORS = {"a": HOT, "b": (230, 140, 40), "c": (50, 90, 200), "n": (25, 25, 28), "g": (40, 150, 70)}


def plastic_sheath(sheath, band):
	def draw(img, d):
		noise(img, sheath, 6)
		d.line([0, 4, 15, 4], fill=tuple(min(255, c + 40) for c in sheath))
		d.line([0, 11, 15, 11], fill=tuple(c - 30 for c in sheath))
		d.line([0, 7, 15, 7], fill=band)
		d.line([0, 8, 15, 8], fill=band)
	return draw


for key, band in CONDUCTOR_COLORS.items():
	tex(f"wire_{key}", bare(COPPER, band))
	tex(f"mv_wire_{key}", bare(ALUMINIUM, band))
	tex(f"dist_wire_{key}", bare((178, 164, 150), band))
	tex(f"hv_wire_{key}", bare(STEEL_WIRE, band))
	tex(f"industrial_wire_{key}", bare((196, 120, 80), band))
	for wear in range(4):
		tex(f"wool_wire_{key}_{wear}", wool(band if key != "n" else (45, 45, 50), wear))
	for prefix, tint in (("", (235, 235, 240)), ("mv_", (225, 230, 245)), ("dist_", (225, 240, 230)), ("hv_", (215, 225, 250)), ("industrial_", (240, 225, 205))):
		tex(f"{prefix}plastic_wire_{key}", plastic_sheath(tint if key != "n" else (60, 60, 66), band))
tex("ground_wire", bare((40, 150, 70), (240, 220, 60)))
tex("ground_rod", bare((205, 125, 80), (140, 80, 50)))


def meter(img, d):
	d.rectangle([3, 2, 12, 13], fill=(60, 64, 70), outline=(30, 32, 36))
	d.rectangle([4, 3, 11, 8], fill=(90, 200, 120))
	d.line([5, 7, 9, 4], fill=(20, 60, 30))
	d.rectangle([5, 10, 6, 11], fill=(220, 70, 60))
	d.rectangle([9, 10, 10, 11], fill=(230, 200, 70))


def manual(img, d):
	d.rectangle([2, 1, 13, 14], fill=(120, 70, 40), outline=(60, 34, 20))
	d.rectangle([3, 2, 4, 13], fill=(90, 50, 30))                       # spine
	d.rectangle([6, 4, 11, 11], fill=(236, 222, 180), outline=(60, 34, 20))
	d.polygon([(9, 4), (7, 8), (9, 8), (8, 11), (11, 7), (9, 7)], fill=(235, 190, 40))  # a lightning bolt


tex("power_meter", meter, "item")
tex("field_manual", manual, "item")

# Real art: the Ludo textures (made by tools/process_art.py) replace the placeholders of the same name.
if os.path.isdir("art/final"):
	for f in os.listdir("art/final"):
		if os.path.isdir(f"art/final/{f}"):
			continue
		kind = "item" if f in ("power_meter.png", "field_manual.png", "seismograph.png", "crude_oil_bucket.png", "plastic_sheet.png", "diesel_bucket.png", "steel_ingot.png",
		"engine_block_casting.png", "piston.png", "crankshaft.png", "cylinder_head.png", "fuel_injector.png", "spark_plug.png", "stator_winding.png", "rotor_field.png", "exciter.png",
		"branch_breaker.png", "gfci_breaker.png", "panelboard_breaker.png", "fuse_class_j.png", "fuse_e.png", "grounding_set.png") else "block"
		shutil.copy(f"art/final/{f}", f"{ASSETS}/textures/{kind}/{f}")
# the materials the true-scale models are drawn in (tools/process_materials.py, tools/models/)
if os.path.isdir("art/final/mat"):
	os.makedirs(f"{ASSETS}/textures/block/mat", exist_ok=True)
	for f in os.listdir("art/final/mat"):
		shutil.copy(f"art/final/mat/{f}", f"{ASSETS}/textures/block/mat/{f}")

# ---- models -------------------------------------------------------------------------------------------
def block_model(name, parent, textures):
	save_json(f"{ASSETS}/models/block/{name}.json", {"parent": parent, "textures": textures})


for lit in (False, True):
	suffix = "_lit" if lit else ""
	block_model(f"boiler{suffix}", "minecraft:block/cube_bottom_top",
		{"top": f"{ns}:block/boiler_top", "bottom": f"{ns}:block/boiler_bottom", "side": f"{ns}:block/boiler_side{suffix}"})
	block_model(f"boiler_formed{suffix}", "minecraft:block/cube_bottom_top",
		{"top": f"{ns}:block/boiler_top", "bottom": f"{ns}:block/boiler_bottom", "side": f"{ns}:block/boiler_formed_side{suffix}"})
for running in (False, True):
	suffix = "_running" if running else ""
	block_model(f"steam_turbine{suffix}", "minecraft:block/cube_column",
		{"end": f"{ns}:block/steam_turbine_end{suffix}", "side": f"{ns}:block/steam_turbine_side{suffix}"})
	block_model(f"steam_turbine_formed{suffix}", "minecraft:block/cube_column",
		{"end": f"{ns}:block/generator_end{suffix}", "side": f"{ns}:block/generator_side{suffix}"})
for suffix in ("", "_lit"):
	block_model(f"firebox{suffix}", "minecraft:block/cube_bottom_top",
		{"top": f"{ns}:block/firebox_top", "bottom": f"{ns}:block/firebox_top", "side": f"{ns}:block/firebox_side{suffix}"})
save_json(f"{ASSETS}/blockstates/firebox.json", {"variants": {
	"lit=false": {"model": f"{ns}:block/firebox"}, "lit=true": {"model": f"{ns}:block/firebox_lit"}}})
block_model("pump", "minecraft:block/cube_all", {"all": f"{ns}:block/pump"})
save_json(f"{ASSETS}/blockstates/pump.json", {"variants": {"": {"model": f"{ns}:block/pump"}}})
for cube in ("casing",):   # the cooling tower parts are true size (4.5c), the well site (4.5g)
	block_model(cube, "minecraft:block/cube_all", {"all": f"{ns}:block/{cube}"})
	save_json(f"{ASSETS}/blockstates/{cube}.json", {"variants": {"": {"model": f"{ns}:block/{cube}"}}})
for load in ("electric_lamp", "electric_heater"):
	block_model(load, "minecraft:block/cube_all", {"all": f"{ns}:block/{load}"})
	block_model(f"{load}_lit", "minecraft:block/cube_all", {"all": f"{ns}:block/{load}_lit"})
	save_json(f"{ASSETS}/blockstates/{load}.json", {"variants": {
		"lit=false": {"model": f"{ns}:block/{load}"}, "lit=true": {"model": f"{ns}:block/{load}_lit"}}})

save_json(f"{ASSETS}/blockstates/boiler.json", {"variants": {
	f"lit={lit}": {"model": f"{ns}:block/boiler{'_lit' if lit == 'true' else ''}"} for lit in ("false", "true")}})
save_json(f"{ASSETS}/blockstates/steam_turbine.json", {"variants": {
	f"running={running}": {"model": f"{ns}:block/steam_turbine{'_running' if running == 'true' else ''}"} for running in ("false", "true")}})

# Phase 4.5e: the pole transformer is drawn at true size in its cell by tools/models/pole_transformer.py; its blockstate is here
for pole_unit in ("pole_transformer", "pole_transformer_mv"):   # 10d2: the 4.16 kV unit (docs/multiblocks/poles.md)
	save_json(f"{ASSETS}/blockstates/{pole_unit}.json", {"variants": {f"facing={facing}": {"model": f"{ns}:block/{pole_unit}/p0", **({"y": y} if y else {})}
		for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})


def cuboid(frm, to, texture="#cable"):
	faces = {f: {"uv": [0, 0, 16, 16], "texture": texture} for f in ("north", "south", "east", "west", "up", "down")}
	return {"from": frm, "to": to, "faces": faces}


# The breaker: a box with a lever. On leans one way, off the other, tripped sits in the middle.
for prefix, wide in (("", 0),):
	for position, angle in (("on", -30), ("off", 30), ("tripped", 0)):
		top = 10 + 3 * wide
		lever_el = cuboid([7, top, 7], [9, top + 5 + wide, 9], "#lever")
		lever_el["rotation"] = {"origin": [8, top, 8], "axis": "x", "angle": angle}
		save_json(f"{ASSETS}/models/block/{prefix}breaker_{position}.json", {
			"textures": {"body": f"{ns}:block/{prefix}breaker_body", "lever": f"{ns}:block/breaker_lever_{position}", "particle": f"{ns}:block/{prefix}breaker_body"},
			"elements": [cuboid([3 - wide, 0, 3 - wide], [13 + wide, top, 13 + wide], "#body"), lever_el]})
for state, texname in (("good", "fuse_tube"), ("blown", "fuse_tube_blown")):
	save_json(f"{ASSETS}/models/block/fuse_{state}.json", {"textures": {"tube": f"{ns}:block/{texname}", "cap": f"{ns}:block/fuse_cap", "particle": f"{ns}:block/fuse_cap"},
		"elements": [cuboid([6, 6, 4], [10, 10, 12], "#tube"), cuboid([5, 5, 2], [11, 11, 4], "#cap"), cuboid([5, 5, 12], [11, 11, 14], "#cap"),
			cuboid([7, 0, 7], [9, 6, 9], "#cap")]})
for model_prefix, body_tex in (("switch", "switch_body"), ("relay", "relay_body")):
	for position, angle in (("on", -30), ("off", 30)):
		lever_el = cuboid([7, 10, 7], [9, 15, 9], "#lever")
		lever_el["rotation"] = {"origin": [8, 10, 8], "axis": "x", "angle": angle}
		save_json(f"{ASSETS}/models/block/{model_prefix}_{position}.json", {
			"textures": {"body": f"{ns}:block/{body_tex}", "lever": f"{ns}:block/breaker_lever_{position}", "particle": f"{ns}:block/{body_tex}"},
			"elements": [cuboid([3, 0, 3], [13, 10, 13], "#body"), lever_el]})
rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
save_json(f"{ASSETS}/blockstates/switch.json", {"variants": {
	f"facing={facing},position={position}": {"model": f"{ns}:block/switch_{'on' if position == 'on' else 'off'}", "y": y}
	for facing, y in rotations.items() for position in ("on", "off", "tripped")}})
# the fused cutout (Phase 4.5e: tools/models/fused_cutout.py draws it at true size): when the link blows the tube drops open
save_json(f"{ASSETS}/blockstates/fused_cutout.json", {"variants": {
	f"blown={blown},facing={facing}": {"model": f"{ns}:block/fused_cutout{'_blown' if blown == 'true' else ''}/p0", "y": y}
	for facing, y in rotations.items() for blown in ("false", "true")}})
save_json(f"{ASSETS}/blockstates/ufls_relay.json", {"variants": {
	f"facing={facing},position={position}": {"model": f"{ns}:block/relay_{'on' if position == 'on' else 'off'}", "y": y}
	for facing, y in rotations.items() for position in ("on", "off", "tripped")}})
save_json(f"{ASSETS}/blockstates/fuse.json", {"variants": {
	f"blown={blown},facing={facing}": {"model": f"{ns}:block/fuse_{'blown' if blown == 'true' else 'good'}", "y": y}
	for facing, y in rotations.items() for blown in ("false", "true")}})
for prefix in ("",):
	save_json(f"{ASSETS}/blockstates/{prefix}breaker.json", {"variants": {
		f"facing={facing},position={position}": {"model": f"{ns}:block/{prefix}breaker_{position}", "y": y}
		for facing, y in rotations.items() for position in ("on", "off", "tripped")}})

# the pole: a thin post. The crossarm: a beam with two insulators.
save_json(f"{ASSETS}/blockstates/pole.json", {"variants": {"": {"model": f"{ns}:block/pole"}}})
save_json(f"{ASSETS}/models/block/pole.json", {"textures": {"wood": f"{ns}:block/mat/mat_wood_creosote", "particle": f"{ns}:block/mat/mat_wood_creosote"},
	"elements": [cuboid([5, 0, 5], [11, 16, 11], "#wood")]})
for axis, (lo, hi) in {"x": ([0, 12, 6], [16, 16, 10]), "z": ([6, 12, 0], [10, 16, 16])}.items():
	save_json(f"{ASSETS}/models/block/crossarm_{axis}.json", {
		"textures": {"wood": f"{ns}:block/mat/mat_wood_creosote", "particle": f"{ns}:block/mat/mat_wood_creosote"},
		"elements": [cuboid([5, 0, 5], [11, 12, 11], "#wood"), cuboid(lo, hi, "#wood")]})   # a short post under the beam joins it to the pole
save_json(f"{ASSETS}/blockstates/crossarm.json", {"variants": {"axis=x": {"model": f"{ns}:block/crossarm_x"}, "axis=z": {"model": f"{ns}:block/crossarm_z"}}})

# ---- insulators and conductor spools -------------------------------------------------------------------------
def insulator_mounts(tier):
	"""True-size insulators, in sixteenths of a metre (docs/multiblocks/poles.md). On an arm the pin stands on the cell's floor and
	the conductor sits 0.26 m up; on a pole-top pin everything is 0.3 m higher, on a strap down the pole; a 138 kV string of nine
	146 mm bells hangs 1.5 m from the crossbeam to its clamp at the bottom of the cell."""
	if tier == "hv":
		els = [cuboid([7, 23.2, 6.5], [9, 24, 9.5], "#pin")]                         # the yoke under the beam's clevis
		for k in range(9):
			top = 22.4 - k * 2.336
			els.append(cuboid([7.3, top - 1.0, 7.3], [8.7, top, 8.7], "#pin"))          # the bell's cap
			els.append(cuboid([5.97, top - 1.6, 5.97], [10.03, top - 1.0, 10.03], "#glaze"))   # its porcelain skirt, 254 mm
		els.append(cuboid([7.6, 1.2, 7.6], [8.4, 2.4, 8.4], "#pin"))                   # the socket clevis
		els.append(cuboid([7.2, 0, 6.0], [8.8, 1.2, 10.0], "#cap"))                    # the suspension clamp, with the conductor in it
		return {"hanging": els}
	def pin(lift):
		if tier == "lv":    # a secondary spool on its pin
			return [cuboid([7.8, lift, 7.8], [8.2, lift + 3, 8.2], "#pin"), cuboid([7.35, lift + 3, 7.35], [8.65, lift + 4.4, 8.65], "#glaze"),
				cuboid([7.25, lift + 3.5, 7.25], [8.75, lift + 3.9, 8.75], "#cap")]
		# an ANSI 55-5 pin insulator, 114 mm across, on its steel pin, the conductor tied in its top groove
		return [cuboid([7.8, lift, 7.8], [8.2, lift + 2.4, 8.2], "#pin"), cuboid([7.1, lift + 2.4, 7.1], [8.9, lift + 2.9, 8.9], "#glaze"),
			cuboid([7.3, lift + 2.9, 7.3], [8.7, lift + 4.0, 8.7], "#glaze"), cuboid([7.4, lift + 4.0, 7.4], [8.6, lift + 4.4, 8.6], "#cap")]
	strap = [cuboid([7.6, -8, 9.35], [8.4, 5.2, 9.7], "#pin"), cuboid([7.6, 4.8, 8.0], [8.4, 5.2, 9.7], "#pin")]   # the pole-top pin's strap
	return {"arm": pin(0), "pole_top": pin(4.8) + strap}



tex("insulator_pin", plain((92, 92, 98)))
for tier, glaze in (("lv", (232, 232, 238)), ("dist", (150, 98, 66)), ("hv", (176, 196, 176))):
	tex(f"insulator_glaze_{tier}", plain(glaze, 5))
for key, color in CONDUCTOR_COLORS.items():
	tex(f"insulator_cap_{key}", plain(color, 4))
for tier, discs in (("lv", [(3, 5)]), ("dist", [(3, 5), (6, 8)]), ("hv", [(2, 4), (5, 7), (8, 10)])):
	wide = 5 if tier != "hv" else 4
	for key in CONDUCTOR_COLORS:
		els = [cuboid([7, 0, 7], [9, 10, 9], "#pin")] + [cuboid([wide, lo, wide], [16 - wide, hi, 16 - wide], "#glaze") for lo, hi in discs] + [cuboid([6, 10, 6], [10, 13, 10], "#cap")]
		save_json(f"{ASSETS}/models/block/insulator_{tier}_{key}.json", {
			"textures": {"pin": f"{ns}:block/insulator_pin", "glaze": f"{ns}:block/insulator_glaze_{tier}", "cap": f"{ns}:block/insulator_cap_{key}",
				"particle": f"{ns}:block/insulator_glaze_{tier}"}, "elements": els})
		# Phase 4.5e, true size (docs/multiblocks/poles.md): on a crossarm, on a pole-top pin, or a 138 kV string hanging from a crossbeam
		for mount, mels in insulator_mounts(tier).items():
			save_json(f"{ASSETS}/models/block/insulator_{tier}_{key}_{mount}.json", {
				"textures": {"pin": f"{ns}:block/mat/mat_galvanized", "glaze": f"{ns}:block/mat/mat_porcelain_brown", "cap": f"{ns}:block/insulator_cap_{key}",
					"particle": f"{ns}:block/mat/mat_porcelain_brown"}, "elements": mels})
	save_json(f"{ASSETS}/blockstates/insulator_{tier}.json", {"variants": {
		f"conductor={key},mount={mount}": {"model": f"{ns}:block/insulator_{tier}_{key}{'' if mount not in insulator_mounts(tier) else '_' + mount}"}
		for key in CONDUCTOR_COLORS for mount in ("legacy", "arm", "pole_top", "hanging")}})


def spool_icon(metal, covered):
	def draw(img, d):
		d.rectangle([2, 1, 13, 3], fill=(122, 82, 50, 255), outline=(80, 52, 30, 255))
		d.rectangle([2, 12, 13, 14], fill=(122, 82, 50, 255), outline=(80, 52, 30, 255))
		d.rectangle([4, 4, 11, 11], fill=metal + (255,))
		for y in (5, 7, 9):
			d.line([4, y, 11, y], fill=tuple(max(0, c - 40) for c in metal) + (255,))
		if covered:
			d.rectangle([4, 4, 11, 11], fill=(236, 236, 242, 255))
			for y in (5, 7, 9):
				d.line([4, y, 11, y], fill=(190, 190, 200, 255))
	return draw


for tier, metal in (("lv", COPPER), ("mv", (186, 150, 120)), ("dist", (178, 164, 150)), ("hv", STEEL_WIRE)):
	tex(f"conductor_spool_{tier}", spool_icon(metal, False), kind="item")
	tex(f"covered_conductor_spool_{tier}", spool_icon(metal, True), kind="item")
# the misc gaps wave: Ludo's reels (art/source/ore/, through tools/process_art.py's icon()) over the drawn ones, drawn first so the noise
# of every later script texture stays the same
for _tier in ("lv", "mv", "dist", "hv"):
	for _name in (f"conductor_spool_{_tier}", f"covered_conductor_spool_{_tier}"):
		if os.path.exists(f"art/final/ore/{_name}.png"):
			shutil.copy(f"art/final/ore/{_name}.png", f"{ASSETS}/textures/item/{_name}.png")

# ---- receptacles and plug adapters ----------------------------------------------------------------------------
PLUG_COLORS = {"nema_5_15": (238, 238, 232), "nema_5_20": (200, 60, 50), "nema_14_50": (46, 46, 52), "cee_7": (246, 246, 246), "iec_60309_blue": (40, 92, 204), "nema_l21_30": (60, 60, 66), "nema_l22_30": (180, 120, 40), "iec_60309_red_32": (196, 40, 36), "iec_60309_red_63": (150, 26, 24)}
PLUG_LABELS = {"nema_5_15": "NEMA 5-15 (120 V, 15 A)", "nema_5_20": "NEMA 5-20 (120 V, 20 A)", "nema_14_50": "NEMA 14-50 (120/240 V, 50 A)",
	"cee_7": "CEE 7 Schuko (230 V, 16 A)", "iec_60309_blue": "IEC 60309 blue (230 V, 32 A)",
	"nema_l21_30": "NEMA L21-30 (208Y/120 V 3-phase, 30 A)", "nema_l22_30": "NEMA L22-30 (480Y/277 V 3-phase, 30 A)",
	"iec_60309_red_32": "IEC 60309 red (400 V 3-phase, 32 A)", "iec_60309_red_63": "IEC 60309 red (400 V 3-phase, 63 A)"}


def socket_face(color):
	def draw(img, d):
		noise(img, color, 5)
		d.rectangle([0, 0, 15, 15], outline=tuple(max(0, c - 60) for c in color))
		dark = (30, 30, 34)
		d.rectangle([4, 5, 5, 10], fill=dark)
		d.rectangle([10, 5, 11, 10], fill=dark)
		d.rectangle([7, 11, 8, 13], fill=dark)
	return draw


def plug_face(color):
	def draw(img, d):
		noise(img, tuple(max(0, c - 70) for c in color), 5)
		d.rectangle([0, 0, 15, 15], outline=(20, 20, 24))
		prong = (200, 200, 206)
		d.rectangle([4, 4, 5, 9], fill=prong)
		d.rectangle([10, 4, 11, 9], fill=prong)
		d.rectangle([7, 10, 8, 12], fill=prong)
	return draw


for pid, color in PLUG_COLORS.items():
	tex(f"receptacle_{pid}", socket_face(color))
	tex(f"plug_{pid}", plug_face(color))
	save_json(f"{ASSETS}/models/block/receptacle_{pid}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{ns}:block/receptacle_{pid}"}, "elements": [cuboid([3, 3, 3], [13, 13, 13], "#all")]})
	save_json(f"{ASSETS}/models/block/plug_{pid}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{ns}:block/plug_{pid}"}, "elements": [cuboid([4, 4, 4], [12, 12, 12], "#all")]})
	save_json(f"{ASSETS}/blockstates/receptacle_{pid}.json", {"variants": {"": {"model": f"{ns}:block/receptacle_{pid}"}}})
	save_json(f"{ASSETS}/blockstates/plug_{pid}.json", {"variants": {"": {"model": f"{ns}:block/plug_{pid}"}}})

# Wire: a thin core with an arm toward each joined neighbour. Wool wire has four wear stages.
arms = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}


def wire_models(model_name, texture):
	t = {"cable": f"{ns}:block/{texture}", "particle": f"{ns}:block/{texture}"}
	save_json(f"{ASSETS}/models/block/{model_name}_core.json", {"textures": t, "elements": [cuboid([7, 7, 7], [9, 9, 9])]})
	save_json(f"{ASSETS}/models/block/{model_name}_arm.json", {"textures": t, "elements": [cuboid([7, 7, 0], [9, 9, 7])]})  # points north
	save_json(f"{ASSETS}/models/block/{model_name}_inventory.json", {  # the item: a short piece lying across the middle
		"parent": "minecraft:block/block",
		"display": {"gui": {"rotation": [30, 225, 0], "scale": [1.2, 1.2, 1.2]}, "ground": {"scale": [0.8, 0.8, 0.8]},
			"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.8, 0.8, 0.8]}, "thirdperson_righthand": {"scale": [0.8, 0.8, 0.8]}},
		"textures": t,
		"elements": [cuboid([0, 7, 7], [16, 9, 9]), cuboid([7, 7, 0], [9, 9, 16])]})


BARE_WIRES = ("wire_hot", "wire_neutral", "mv_wire_hot", "mv_wire_neutral", "dist_wire_hot", "dist_wire_neutral", "hv_wire_hot", "hv_wire_neutral", "industrial_wire_hot", "industrial_wire_neutral", "ground_wire")
PLASTIC_WIRES = ("plastic_wire_hot", "plastic_wire_neutral", "mv_plastic_wire_hot", "mv_plastic_wire_neutral", "dist_plastic_wire_hot", "dist_plastic_wire_neutral", "hv_plastic_wire_hot", "hv_plastic_wire_neutral", "industrial_plastic_wire_hot", "industrial_plastic_wire_neutral")
WOOL_WIRES = ("wool_wire_hot", "wool_wire_neutral")

# The ground wire (the bonding wire for machines) is a plain single piece.
wire_models("ground_wire", "ground_wire")
save_json(f"{ASSETS}/blockstates/ground_wire.json", {"multipart": [{"apply": {"model": f"{ns}:block/ground_wire_core"}}] + [
	{"when": {side: "true"}, "apply": {"model": f"{ns}:block/ground_wire_arm", **rot}} for side, rot in arms.items()]})

# Every other wire block can carry any conductor (the blockstate property "conductor"); the block you place starts as A (hot) or N (neutral).
for name in BARE_WIRES[:-1] + PLASTIC_WIRES + WOOL_WIRES:
	base = name.rsplit("_", 1)[0]                      # wire_hot -> wire, mv_plastic_wire_neutral -> mv_plastic_wire
	wool_wire = name.startswith("wool_")
	parts = []
	for key in CONDUCTOR_COLORS:
		for wear in (range(4) if wool_wire else [None]):
			texture = f"{base}_{key}" + (f"_{wear}" if wear is not None else "")
			wire_models(texture, texture)
			cond = {"conductor": key}
			when_core = {**cond, "wear": str(wear)} if wear is not None else cond
			parts.append({"when": when_core, "apply": {"model": f"{ns}:block/{texture}_core"}})
			for side, rot in arms.items():
				parts.append({"when": {"AND": [{side: "true"}, {k: v for k, v in when_core.items()}]}, "apply": {"model": f"{ns}:block/{texture}_arm", **rot}})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"multipart": parts})
	default_key = "a" if name.endswith("_hot") else "n"
	default_texture = f"{base}_{default_key}" + ("_0" if wool_wire else "")
	save_json(f"{ASSETS}/models/block/{name}_inventory.json", json.load(open(f"{ASSETS}/models/block/{default_texture}_inventory.json")))

# the ground rod: one thin post
save_json(f"{ASSETS}/models/block/ground_rod.json", {"textures": {"rod": f"{ns}:block/ground_rod", "particle": f"{ns}:block/ground_rod"},
	"elements": [cuboid([7, 0, 7], [9, 16, 9], "#rod")]})
save_json(f"{ASSETS}/blockstates/ground_rod.json", {"variants": {"": {"model": f"{ns}:block/ground_rod"}}})

# the pipe at true size (4.5g, docs/multiblocks/well-site.md): 2 in NPS (60 mm) water pipe, a welded fitting at the core and a run
# toward each joined neighbour. Drawn square, as wide as the pipe's outside diameter.
def pipe_shapes(od, fitting):
	a, b = 8 - od * 8, 8 + od * 8
	c, d = 8 - fitting * 8, 8 + fitting * 8
	return [cuboid([c, c, c], [d, d, d], "#pipe")], [cuboid([a, a, 0], [b, b, c], "#pipe")]
pt = {"pipe": f"{ns}:block/pipe", "particle": f"{ns}:block/pipe"}
core, arm = pipe_shapes(0.0603, 0.085)
save_json(f"{ASSETS}/models/block/pipe_core.json", {"textures": pt, "elements": core})
save_json(f"{ASSETS}/models/block/pipe_arm.json", {"textures": pt, "elements": arm})
save_json(f"{ASSETS}/models/block/pipe_inventory.json", {
	"parent": "minecraft:block/block",
	"display": {"gui": {"rotation": [30, 225, 0], "scale": [1.0, 1.0, 1.0]}, "ground": {"scale": [0.8, 0.8, 0.8]},
		"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.8, 0.8, 0.8]}, "thirdperson_righthand": {"scale": [0.8, 0.8, 0.8]}},
	"textures": pt, "elements": [cuboid([0, 6, 6], [16, 10, 10], "#pipe"), cuboid([6, 6, 0], [10, 10, 16], "#pipe")]})
save_json(f"{ASSETS}/blockstates/pipe.json", {"multipart": [{"apply": {"model": f"{ns}:block/pipe_core"}}] + [
	{"when": {side: "true"}, "apply": {"model": f"{ns}:block/pipe_arm", **rot}} for side, rot in arms.items()]})

# the steel line for oil and gas: 4 in NPS (114 mm) gathering line, a welded fitting at the core
gt = {"pipe": f"{ns}:block/gas_line", "particle": f"{ns}:block/gas_line"}
core, arm = pipe_shapes(0.1143, 0.15)
save_json(f"{ASSETS}/models/block/gas_line_core.json", {"textures": gt, "elements": core})
save_json(f"{ASSETS}/models/block/gas_line_arm.json", {"textures": gt, "elements": arm})
inv = json.load(open(f"{ASSETS}/models/block/pipe_inventory.json"))
inv["textures"] = gt
save_json(f"{ASSETS}/models/block/gas_line_inventory.json", inv)
save_json(f"{ASSETS}/blockstates/gas_line.json", {"multipart": [{"apply": {"model": f"{ns}:block/gas_line_core"}}] + [
	{"when": {side: "true"}, "apply": {"model": f"{ns}:block/gas_line_arm", **rot}} for side, rot in arms.items()]})

# ---- items --------------------------------------------------------------------------------------------
WIRES = BARE_WIRES + WOOL_WIRES + PLASTIC_WIRES
ITEM_MODELS = {"firebox": "block/firebox", "pump": "block/pump", "pipe": "block/pipe_inventory", "gas_line": "block/gas_line_inventory", "drilling_rig": "block/drilling_rig", "cooling_tower": "block/cooling_tower", "cooling_fill": "block/cooling_fill",
	"pumpjack": "block/pumpjack",
	"gas_wellhead": "block/gas_wellhead", "casing": "block/casing", "boiler": "block/boiler", "steam_turbine": "block/steam_turbine", "electric_lamp": "block/electric_lamp",
	"electric_heater": "block/electric_heater", "breaker": "block/breaker_on", "fuse": "block/fuse_good", "fused_cutout": "block/fused_cutout/item", "pole_transformer": "block/pole_transformer/item", "switch": "block/switch_on", "ufls_relay": "block/relay_on", "pole": "block/pole", "crossarm": "block/crossarm_x", "pole_transformer_mv": "block/pole_transformer_mv/item",
	"ground_rod": "block/ground_rod",
	"power_meter": "item/power_meter", "field_manual": "item/field_manual", "seismograph": "item/seismograph", "crude_oil_bucket": "item/crude_oil_bucket", "plastic_sheet": "item/plastic_sheet", "diesel_bucket": "item/diesel_bucket", "steel_ingot": "item/steel_ingot", "conductor_spool_lv": "item/conductor_spool_lv", "conductor_spool_mv": "item/conductor_spool_mv", "covered_conductor_spool_mv": "item/covered_conductor_spool_mv", "conductor_spool_dist": "item/conductor_spool_dist", "conductor_spool_hv": "item/conductor_spool_hv", "covered_conductor_spool_lv": "item/covered_conductor_spool_lv", "covered_conductor_spool_dist": "item/covered_conductor_spool_dist", "covered_conductor_spool_hv": "item/covered_conductor_spool_hv", "insulator_lv": "block/insulator_lv_a", **{f"receptacle_{p}": f"block/receptacle_{p}" for p in PLUG_COLORS}, **{f"plug_{p}": f"block/plug_{p}" for p in PLUG_COLORS}, "insulator_dist": "block/insulator_dist_a", "insulator_hv": "block/insulator_hv_a"}
ITEM_MODELS.update({w: f"block/{w}_inventory" for w in WIRES})
for name, model in ITEM_MODELS.items():
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:{model}"}})
for item in ("power_meter", "field_manual", "seismograph", "crude_oil_bucket", "plastic_sheet", "diesel_bucket", "steel_ingot", "conductor_spool_lv", "conductor_spool_mv", "covered_conductor_spool_mv", "conductor_spool_dist", "conductor_spool_hv", "covered_conductor_spool_lv", "covered_conductor_spool_dist", "covered_conductor_spool_hv"):
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{item}"}})

# ---- names --------------------------------------------------------------------------------------------
save_json(f"{ASSETS}/lang/en_us.json", {
	"block.wildcatter.boiler": "Boiler",
	"block.wildcatter.firebox": "Firebox",
	"block.wildcatter.pipe": "Water Pipe",
	"block.wildcatter.pump": "Water Pump (LV)",
	"block.wildcatter.gas_line": "Steel Pipe (oil and gas)",
	"block.wildcatter.drilling_rig": "Drilling Rig (LV)",
	"block.wildcatter.casing": "Well Casing (cemented: unbreakable)",
	"block.wildcatter.cooling_tower": "Cooling Tower Fan Deck (LV)",
	"block.wildcatter.cooling_fill": "Cooling Tower Fill",
	"block.wildcatter.plastic_wire_hot": "Plastic Wire, red (LV)",
	"block.wildcatter.plastic_wire_neutral": "Plastic Wire, black (LV)",
	"block.wildcatter.mv_plastic_wire_hot": "Plastic Line, red (MV)",
	"block.wildcatter.mv_plastic_wire_neutral": "Plastic Line, black (MV)",
	"block.wildcatter.hv_plastic_wire_hot": "Plastic Line, red (HV)",
	"block.wildcatter.hv_plastic_wire_neutral": "Plastic Line, black (HV)",
	"block.wildcatter.pumpjack": "Pumpjack (LV)",
	"block.wildcatter.gas_wellhead": "Gas Wellhead",
	"block.wildcatter.steam_turbine": "Steam Turbine",
	"block.wildcatter.wire_hot": "Bare Wire, red (LV)",
	"block.wildcatter.wire_neutral": "Bare Wire, black (LV)",
	"block.wildcatter.wool_wire_hot": "Wool-Insulated Wire, red (LV)",
	"block.wildcatter.wool_wire_neutral": "Wool-Insulated Wire, black (LV)",
	"block.wildcatter.mv_wire_hot": "Bare Line, red (MV)",
	"block.wildcatter.mv_wire_neutral": "Bare Line, black (MV)",
	"block.wildcatter.hv_wire_hot": "Bare Line, red (HV)",
	"block.wildcatter.hv_wire_neutral": "Bare Line, black (HV)",
	"block.wildcatter.ground_rod": "Ground Rod",
	"block.wildcatter.ground_wire": "Ground Wire",
	"block.wildcatter.breaker": "Circuit Breaker (LV)",
	"block.wildcatter.fuse": "Fuse (LV)",
	"block.wildcatter.switch": "Switch (LV)",
	"block.wildcatter.fused_cutout": "Fused Cutout (DIST)",
	"block.wildcatter.ufls_relay": "Under-Frequency Relay (LV)",
	"block.wildcatter.dist_wire_hot": "Bare Line, red (DIST)",
	"block.wildcatter.dist_wire_neutral": "Bare Line, black (DIST)",
	"block.wildcatter.dist_plastic_wire_hot": "Plastic Line, red (DIST)",
	"block.wildcatter.dist_plastic_wire_neutral": "Plastic Line, black (DIST)",
	"block.wildcatter.pole_transformer": "Pole-Mount Transformer (DIST / LV, 25 kVA)",
	"block.wildcatter.insulator_lv": "Insulator (LV, 1 kV)",
	"block.wildcatter.insulator_dist": "Pin Insulator (DIST, 15 kV)",
	"block.wildcatter.insulator_hv": "Suspension Insulator (HV, 138 kV)",
	"item.wildcatter.conductor_spool_lv": "Conductor Spool (LV)",
	"item.wildcatter.conductor_spool_mv": "Conductor Spool (MV)",
	"item.wildcatter.covered_conductor_spool_mv": "Covered Conductor Spool (MV)",
	"item.wildcatter.conductor_spool_dist": "Conductor Spool (DIST)",
	"item.wildcatter.conductor_spool_hv": "Conductor Spool (HV)",
	"item.wildcatter.covered_conductor_spool_lv": "Covered Conductor Spool (LV)",
	"item.wildcatter.covered_conductor_spool_dist": "Covered Conductor Spool (DIST)",
	"item.wildcatter.covered_conductor_spool_hv": "Covered Conductor Spool (HV)",
	**{f"block.wildcatter.receptacle_{p}": f"Receptacle {PLUG_LABELS[p]}" for p in PLUG_COLORS},
	**{f"block.wildcatter.plug_{p}": f"Plug Adapter {PLUG_LABELS[p]}" for p in PLUG_COLORS},
	"block.wildcatter.pole": "Utility Pole",
	"block.wildcatter.crossarm": "Pole Crossarm",
	"block.wildcatter.electric_lamp": "Electric Lamp (60 W)",
	"block.wildcatter.electric_heater": "Electric Heater (1500 W)",
	"subtitles.wildcatter.turbine_hum": "Turbine hums",
	"subtitles.wildcatter.boiler_hiss": "Boiler hisses",
	"subtitles.wildcatter.breaker_trip": "Breaker trips",
	"subtitles.wildcatter.breaker_click": "Breaker clicks",
	"subtitles.wildcatter.zap": "Electric shock",
	"subtitles.wildcatter.structure_formed": "Machine powers up",
	"item.wildcatter.power_meter": "Power Meter",
	"item.wildcatter.field_manual": "Wildcatter Field Manual",
	"item.wildcatter.seismograph": "Seismograph",
	"item.wildcatter.plastic_sheet": "Plastic Sheet",
	"item.wildcatter.steel_ingot": "Steel Ingot",
	"item.wildcatter.diesel_bucket": "Bucket of Diesel",
	"item.wildcatter.crude_oil_bucket": "Bucket of Crude Oil",
})

# ---- loot: every block drops itself ---------------------------------------------------------------
BLOCKS = ("cooling_tower", "cooling_fill", "boiler", "firebox", "pipe", "gas_line", "drilling_rig", "pumpjack", "gas_wellhead", "pump", "steam_turbine", "electric_lamp", "electric_heater", "breaker", "fuse", "fused_cutout", "switch", "ufls_relay", "pole_transformer", "pole_transformer_mv", "pole", "crossarm", "insulator_lv", "insulator_dist", "insulator_hv", "ground_rod") + tuple(f"receptacle_{p}" for p in PLUG_COLORS) + tuple(f"plug_{p}" for p in PLUG_COLORS) + WIRES
for name in BLOCKS:
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {
		"type": "minecraft:block",
		"pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
			"conditions": [{"condition": "minecraft:survives_explosion"}]}]})

save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{ns}:{n}" for n in BLOCKS if n not in ("pole", "crossarm")]})
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/axe.json", {"values": [f"{ns}:pole", f"{ns}:crossarm"]})
save_json(f"{ROOT}/data/minecraft/tags/block/climbable.json", {"values": [f"{ns}:pole"]})
save_json(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json", {"values": [f"{ns}:{n}" for n in ("boiler", "firebox", "pole_transformer", "pole_transformer_mv", "pump", "drilling_rig", "pumpjack", "gas_wellhead", "cooling_tower", "cooling_fill", "steam_turbine")]})

# ---- recipes (early game: iron, copper, redstone, glass, dye, wool) ----------------------------------------
def shaped(name, pattern, key, result, count=1):
	save_json(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "redstone",
		"pattern": pattern, "key": key, "result": {"id": f"{ns}:{result}", "count": count}})


def shapeless(name, ingredients, result, count=1):
	save_json(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "redstone",
		"ingredients": ingredients, "result": {"id": f"{ns}:{result}", "count": count}})


shaped("boiler", ["III", "IWI", "III"], {"I": "minecraft:iron_ingot", "W": "minecraft:water_bucket"}, "boiler")
shaped("firebox", ["IBI", "BFB", "IBI"], {"I": "minecraft:iron_ingot", "B": "minecraft:bricks", "F": "minecraft:furnace"}, "firebox")
shaped("pipe", ["III", "   ", "III"], {"I": "minecraft:iron_ingot"}, "pipe", 6)
shaped("pump", ["III", "CBC", "IRI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "B": "minecraft:bucket", "R": "minecraft:redstone"}, "pump")
shaped("gas_line", ["III", "   ", "III"], {"I": f"{ns}:steel_ingot"}, "gas_line", 6)
# steel: iron smelted in a blast furnace with coal as the fuel (real steel is iron with carbon)
save_json(f"{DATA}/recipe/steel_ingot.json", {"type": "minecraft:blasting", "ingredient": "minecraft:iron_ingot", "cookingtime": 200, "experience": 0.7, "result": {"id": f"{ns}:steel_ingot"}})
shaped("drilling_rig", ["IPI", "CMC", "IBI"], {"I": "minecraft:iron_ingot", "P": "minecraft:iron_pickaxe", "C": "minecraft:copper_ingot", "M": "minecraft:piston", "B": "minecraft:iron_block"}, "drilling_rig")
shaped("pumpjack", ["III", "CPC", "IRI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "P": "minecraft:piston", "R": "minecraft:redstone"}, "pumpjack")
shaped("gas_wellhead", ["III", "CBC", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "B": "minecraft:iron_block"}, "gas_wellhead")
shaped("seismograph", ["IPI", "RCR", "III"], {"I": "minecraft:iron_ingot", "P": "minecraft:paper", "R": "minecraft:redstone", "C": "minecraft:compass"}, "seismograph")
shaped("cooling_tower", ["IBI", "CFC", "III"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_bars", "C": "minecraft:copper_ingot", "F": "minecraft:blast_furnace"}, "cooling_tower")
shaped("cooling_fill", ["IBI", "IBI", "IBI"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_bars"}, "cooling_fill")
shaped("steam_turbine", ["ICI", "CRC", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "steam_turbine")
# Wire is coloured with dye so you can tell the two conductors apart: red is hot, black is neutral.
for role, dye in (("hot", "minecraft:red_dye"), ("neutral", "minecraft:black_dye")):
	shapeless(f"wire_{role}", ["minecraft:copper_ingot"] * 3 + [dye], f"wire_{role}", 6)
	shapeless(f"wool_wire_{role}", [f"{ns}:wire_{role}"] * 4 + ["#minecraft:wool"], f"wool_wire_{role}", 4)
	shapeless(f"dist_wire_{role}", ["minecraft:iron_ingot"] * 2 + ["minecraft:copper_ingot"] * 3 + ["minecraft:gold_ingot", dye], f"dist_wire_{role}", 4)
	shapeless(f"mv_wire_{role}", ["minecraft:iron_ingot"] * 2 + ["minecraft:copper_ingot"] * 3 + [dye], f"mv_wire_{role}", 4)
	shapeless(f"hv_wire_{role}", ["minecraft:iron_ingot"] * 4 + ["minecraft:copper_ingot"] * 2 + [dye], f"hv_wire_{role}", 4)
shaped("ground_rod", ["C", "C", "C"], {"C": "minecraft:copper_ingot"}, "ground_rod", 2)
shapeless("ground_wire", [f"{ns}:wire_neutral", "minecraft:green_dye"], "ground_wire")
shaped("fused_cutout", ["BCB", "NFN", " I "], {"B": "minecraft:brick", "C": "minecraft:copper_ingot", "N": "minecraft:iron_nugget", "F": f"{ns}:fuse", "I": "minecraft:iron_ingot"}, "fused_cutout")
shaped("fuse", ["GGG", "ICI", "GGG"], {"G": "minecraft:glass_pane", "I": "minecraft:iron_nugget", "C": "minecraft:copper_ingot"}, "fuse", 2)
shaped("ufls_relay", ["ICI", "RBR", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:comparator", "R": "minecraft:redstone", "B": f"{ns}:breaker"}, "ufls_relay")
shaped("switch", ["I I", "ICI", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "switch")
shaped("breaker", ["ICI", "IRI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "breaker")
for tier, wire, sheets in (("lv", "wire_hot", 1), ("mv", "mv_wire_hot", 2), ("dist", "dist_wire_hot", 2), ("hv", "hv_wire_hot", 3)):
	shaped(f"conductor_spool_{tier}", ["WWW", "WSW", "WWW"], {"W": f"{ns}:{wire}", "S": "minecraft:stick"}, f"conductor_spool_{tier}")
	shapeless(f"covered_conductor_spool_{tier}", [f"{ns}:conductor_spool_{tier}"] + [f"{ns}:plastic_sheet"] * sheets, f"covered_conductor_spool_{tier}")
shaped("insulator_lv", ["B", "N"], {"B": "minecraft:brick", "N": "minecraft:iron_nugget"}, "insulator_lv", 2)
shaped("insulator_dist", ["B", "B", "N"], {"B": "minecraft:brick", "N": "minecraft:iron_nugget"}, "insulator_dist", 2)
shaped("insulator_hv", ["BBB", "BNB", "BIB"], {"B": "minecraft:brick", "N": "minecraft:iron_nugget", "I": "minecraft:iron_ingot"}, "insulator_hv", 2)
for pid, dye, copper in (("nema_5_15", "minecraft:white_dye", 1), ("nema_5_20", "minecraft:red_dye", 1), ("nema_14_50", "minecraft:black_dye", 2), ("cee_7", "minecraft:yellow_dye", 1), ("iec_60309_blue", "minecraft:blue_dye", 2), ("nema_l21_30", "minecraft:gray_dye", 3), ("nema_l22_30", "minecraft:orange_dye", 3), ("iec_60309_red_32", "minecraft:red_dye", 3), ("iec_60309_red_63", "minecraft:red_dye", 4)):
	shapeless(f"receptacle_{pid}", ["minecraft:iron_ingot"] + ["minecraft:copper_ingot"] * copper + [dye], f"receptacle_{pid}", 2)
	shapeless(f"plug_{pid}", [f"{ns}:receptacle_{pid}", "minecraft:redstone", "minecraft:iron_ingot"], f"plug_{pid}")
shaped("pole", ["L", "L", "L"], {"L": "#minecraft:logs"}, "pole", 3)
shaped("crossarm", ["PPP", " N "], {"P": "#minecraft:planks", "N": "minecraft:iron_nugget"}, "crossarm", 2)
shaped("pole_transformer_mv", ["ICI", "ICI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "pole_transformer_mv")
shaped("pole_transformer", ["ICI", "CTC", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "T": f"{ns}:pole_transformer_mv"}, "pole_transformer")
shaped("electric_lamp", [" G ", "GRG", " C "], {"G": "minecraft:glass_pane", "R": "minecraft:redstone", "C": "minecraft:copper_ingot"}, "electric_lamp")
shaped("electric_heater", ["III", "CRC", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "electric_heater")
# Plastic wire: the same copper as ordinary wire, wrapped in plastic sheet.
for role, dye in (("hot", "minecraft:red_dye"), ("neutral", "minecraft:black_dye")):
	shapeless(f"plastic_wire_{role}", [f"{ns}:wire_{role}"] * 4 + [f"{ns}:plastic_sheet"], f"plastic_wire_{role}", 4)
	shapeless(f"dist_plastic_wire_{role}", [f"{ns}:dist_wire_{role}"] * 4 + [f"{ns}:plastic_sheet"] * 2, f"dist_plastic_wire_{role}", 4)
	shapeless(f"mv_plastic_wire_{role}", [f"{ns}:mv_wire_{role}"] * 4 + [f"{ns}:plastic_sheet"] * 2, f"mv_plastic_wire_{role}", 4)
	shapeless(f"hv_plastic_wire_{role}", [f"{ns}:hv_wire_{role}"] * 4 + [f"{ns}:plastic_sheet"] * 3, f"hv_plastic_wire_{role}", 4)
shapeless("field_manual", ["minecraft:book", "minecraft:copper_ingot"], "field_manual")
shaped("power_meter", [" C ", "IRI", " I "], {"C": "minecraft:copper_ingot", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "power_meter")

# ---- Phase 3: industrial tier, three-phase cable, three-pole breakers, pad-mounts, three-phase heater -------------
# Drawn by script as placeholders; the Ludo art in art/final (tools/process_art.py) takes over a texture when it exists.
def big_part_loot(name):
	"""A true-size part drops one item, from its main cell (piece 0) only: breaking any piece breaks the whole part."""
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}, {"condition": "minecraft:block_state_property", "block": f"{ns}:{name}", "properties": {"piece": "0"}}]}]})


def art(name):
	return name if os.path.exists(f"art/final/{name}.png") else f"{name}_placeholder"


def cable_sheath(sheath):
	def draw(img, d):
		noise(img, sheath, 5)
		for i, key in enumerate(("a", "b", "c", "n", "g")):
			d.line([0, 3 + 2 * i, 15, 3 + 2 * i], fill=CONDUCTOR_COLORS[key] if key != "n" else (235, 235, 235))
	return draw


CABLES = {"cable_lv": (40, 40, 44), "cable_industrial": (70, 52, 40), "cable_mv": (40, 46, 60), "cable_dist": (30, 50, 40), "cable_hv": (24, 24, 30)}
for cable, sheath in CABLES.items():
	tex(f"{cable}_placeholder", cable_sheath(sheath))
	t = {"cable": f"{ns}:block/{art(cable)}", "particle": f"{ns}:block/{art(cable)}"}
	save_json(f"{ASSETS}/models/block/{cable}_core.json", {"textures": t, "elements": [cuboid([6, 6, 6], [10, 10, 10])]})
	save_json(f"{ASSETS}/models/block/{cable}_arm.json", {"textures": t, "elements": [cuboid([6, 6, 0], [10, 10, 6])]})
	save_json(f"{ASSETS}/models/block/{cable}_inventory.json", {"parent": "minecraft:block/block",
		"display": {"gui": {"rotation": [30, 225, 0], "scale": [1.1, 1.1, 1.1]}, "ground": {"scale": [0.8, 0.8, 0.8]},
			"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.8, 0.8, 0.8]}, "thirdperson_righthand": {"scale": [0.8, 0.8, 0.8]}},
		"textures": t, "elements": [cuboid([0, 6, 6], [16, 10, 10]), cuboid([6, 6, 0], [10, 10, 16])]})
	save_json(f"{ASSETS}/blockstates/{cable}.json", {"multipart": [{"apply": {"model": f"{ns}:block/{cable}_core"}}] + [
		{"when": {side: "true"}, "apply": {"model": f"{ns}:block/{cable}_arm", **rot}} for side, rot in arms.items()]})
	ITEM_MODELS[cable] = f"block/{cable}_inventory"

# three-pole breakers: a wide body with three levers tied by a handle bar
tex("breaker_3p_lv_body_placeholder", tinted_breaker((150, 150, 158)))
tex("breaker_3p_industrial_body_placeholder", tinted_breaker((210, 130, 50)))
for name in ("breaker_3p_lv", "breaker_3p_industrial"):
	for position, angle in (("on", -30), ("off", 30), ("tripped", 0)):
		els = [cuboid([1, 0, 3], [15, 11, 13], "#body")]
		for x in (3, 7, 11):
			lever_el = cuboid([x, 11, 7], [x + 2, 16, 9], "#lever")
			lever_el["rotation"] = {"origin": [8, 11, 8], "axis": "x", "angle": angle}
			els.append(lever_el)
		bar = cuboid([3, 14, 7], [13, 15, 9], "#lever")
		bar["rotation"] = {"origin": [8, 11, 8], "axis": "x", "angle": angle}
		els.append(bar)
		save_json(f"{ASSETS}/models/block/{name}_{position}.json", {
			"textures": {"body": f"{ns}:block/{art(name + '_body')}", "lever": f"{ns}:block/breaker_lever_{position}", "particle": f"{ns}:block/{art(name + '_body')}"},
			"elements": els})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {
		f"facing={facing},position={position}": {"model": f"{ns}:block/{name}_{position}", "y": y}
		for facing, y in rotations.items() for position in ("on", "off", "tripped")}})
	ITEM_MODELS[name] = f"block/{name}_on"

# pad-mounts and the unit substation: true size since Phase 4.5e (tools/models/padmount_208.py, padmount_480.py, unit_substation.py)

# the three-phase heater
tex("three_phase_heater_placeholder", heater(False))
tex("three_phase_heater_lit_placeholder", heater(True))
block_model("three_phase_heater", "minecraft:block/cube_all", {"all": f"{ns}:block/{art('three_phase_heater')}"})
block_model("three_phase_heater_lit", "minecraft:block/cube_all", {"all": f"{ns}:block/{art('three_phase_heater_lit')}"})
save_json(f"{ASSETS}/blockstates/three_phase_heater.json", {"variants": {
	"lit=false": {"model": f"{ns}:block/three_phase_heater"}, "lit=true": {"model": f"{ns}:block/three_phase_heater_lit"}}})
ITEM_MODELS["three_phase_heater"] = "block/three_phase_heater"

# motor starters and the motor control center: steel cabinets; the front shows a green (running), grey (stopped) or red (tripped) lamp
def cabinet_front(lamp, handle=False):
	def draw(img, d):
		noise(img, (128, 134, 140), 5)
		d.rectangle([1, 1, 14, 14], outline=(80, 84, 90))
		d.rectangle([3, 3, 12, 7], fill=(60, 64, 70))
		d.rectangle([5, 9, 7, 11], fill=lamp)
		d.rectangle([9, 9, 11, 11], fill=(40, 40, 44) if not handle else (200, 160, 40))
	return draw


def cabinet_side(img, d):
	noise(img, (118, 124, 130), 5)
	d.line([0, 0, 15, 0], fill=(90, 94, 100))
	d.line([0, 15, 15, 15], fill=(90, 94, 100))


tex("cabinet_side_placeholder", cabinet_side)
LAMPS = {"running": (60, 200, 90), "stopped": (110, 110, 116), "tripped": (220, 50, 40), "on": (60, 200, 90), "off": (110, 110, 116)}
for name in ("motor_starter", "mcc_bucket", "mcc_incoming"):
	states = ("on", "off", "tripped") if name == "mcc_incoming" else ("running", "stopped", "tripped")
	prop = "position" if name == "mcc_incoming" else "run"
	for st in states:
		tex(f"{name}_front_{st}_placeholder", cabinet_front(LAMPS[st], name == "mcc_incoming"))
		save_json(f"{ASSETS}/models/block/{name}_{st}.json", {"parent": "minecraft:block/orientable", "textures": {
			"front": f"{ns}:block/{art(f'{name}_front_{st}')}", "side": f"{ns}:block/{art('cabinet_side')}", "top": f"{ns}:block/{art('cabinet_side')}", "bottom": f"{ns}:block/{art('cabinet_side')}"}})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {
		f"facing={facing},{prop}={st}": {"model": f"{ns}:block/{name}_{st}", "y": y} for facing, y in rotations.items() for st in states}})
	ITEM_MODELS[name] = f"block/{name}_{states[1]}"

PHASE3_BLOCKS = ("motor_starter", "mcc_bucket", "mcc_incoming") + tuple(CABLES) + ("breaker_3p_lv", "breaker_3p_industrial", "padmount_208", "padmount_480", "unit_substation", "three_phase_heater")
for name in PHASE3_BLOCKS:
	if name in ITEM_MODELS:   # the pad-mounts and unit substation get theirs from the model tool (Phase 4.5e)
		save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:{ITEM_MODELS[name]}"}})
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
pick = json.load(open(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json"))
pick["values"] += [f"{ns}:{n}" for n in PHASE3_BLOCKS]
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", pick)
stone = json.load(open(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json"))
stone["values"] += [f"{ns}:{n}" for n in ("breaker_3p_industrial", "padmount_208", "padmount_480", "unit_substation", "motor_starter", "mcc_incoming", "mcc_bucket")]
save_json(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json", stone)

lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({
	"block.wildcatter.industrial_wire_hot": "Bare Wire, red (480 V)",
	"block.wildcatter.industrial_wire_neutral": "Bare Wire, black (480 V)",
	"block.wildcatter.industrial_plastic_wire_hot": "Plastic Wire, red (480 V)",
	"block.wildcatter.industrial_plastic_wire_neutral": "Plastic Wire, black (480 V)",
	"block.wildcatter.cable_lv": "Three-Phase Cable (LV, A B C N G)",
	"block.wildcatter.cable_industrial": "Three-Phase Cable (480 V, A B C N G)",
	"block.wildcatter.cable_mv": "MV Cable (4.16 kV, A B C N G)",
	"block.wildcatter.cable_dist": "Underground Primary Cable (DIST, A B C N G)",
	"block.wildcatter.cable_hv": "High-Voltage Cable (HV, A B C N G)",
	"block.wildcatter.breaker_3p_lv": "Three-Pole Breaker (LV)",
	"block.wildcatter.breaker_3p_industrial": "Three-Pole Breaker (480 V)",
	"block.wildcatter.padmount_208": "Pad-Mount Transformer (DIST / 208Y/120 V, 75 kVA)",
	"block.wildcatter.padmount_480": "Pad-Mount Transformer (DIST / 480Y/277 V, 300 kVA)",
	"block.wildcatter.unit_substation": "Unit Substation (MV / 480Y/277 V, 1000 kVA)",
	"block.wildcatter.three_phase_heater": "Three-Phase Heater (480 V, 15 kW)",
	"block.wildcatter.motor_starter": "Combination Motor Starter (480 V)",
	"block.wildcatter.mcc_incoming": "MCC Incoming Section (480 V)",
	"block.wildcatter.mcc_bucket": "MCC Starter Bucket (480 V)",
})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# recipes: industrial copper is heavier; cable is three phase wires, a neutral and a ground in plastic
for role, dye in (("hot", "minecraft:red_dye"), ("neutral", "minecraft:black_dye")):
	shapeless(f"industrial_wire_{role}", ["minecraft:copper_ingot"] * 4 + ["minecraft:iron_ingot", dye], f"industrial_wire_{role}", 4)
	shapeless(f"industrial_plastic_wire_{role}", [f"{ns}:industrial_wire_{role}"] * 4 + [f"{ns}:plastic_sheet"], f"industrial_plastic_wire_{role}", 4)
for cable, (wire_hot, wire_neutral) in {"cable_lv": ("wire_hot", "wire_neutral"), "cable_industrial": ("industrial_wire_hot", "industrial_wire_neutral"), "cable_mv": ("mv_wire_hot", "mv_wire_neutral"), "cable_dist": ("dist_wire_hot", "dist_wire_neutral"), "cable_hv": ("hv_wire_hot", "hv_wire_neutral")}.items():
	shaped(cable, ["PHP", "HNH", "PGP"], {"P": f"{ns}:plastic_sheet", "H": f"{ns}:{wire_hot}", "N": f"{ns}:{wire_neutral}", "G": f"{ns}:ground_wire"}, cable, 4)
shaped("breaker_3p_lv", ["BBB", "IRI"], {"B": f"{ns}:breaker", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "breaker_3p_lv")
shaped("breaker_3p_industrial", ["BBB", "SRS"], {"B": f"{ns}:breaker", "S": f"{ns}:steel_ingot", "R": "minecraft:redstone_block"}, "breaker_3p_industrial")
shaped("padmount_208", ["SSS", "TCT", "SSS"], {"S": f"{ns}:steel_ingot", "T": f"{ns}:pole_transformer_mv", "C": "minecraft:copper_block"}, "padmount_208")
shaped("padmount_480", ["SDS", "TCT", "SSS"], {"S": f"{ns}:steel_ingot", "D": "minecraft:diamond", "T": f"{ns}:pole_transformer_mv", "C": "minecraft:copper_block"}, "padmount_480")
shaped("unit_substation", ["SDS", "TCT", "SDS"], {"S": f"{ns}:steel_ingot", "D": "minecraft:diamond", "T": f"{ns}:pole_transformer_mv", "C": "minecraft:copper_block"}, "unit_substation")
shaped("motor_starter", ["SBS", "CRC", "SSS"], {"S": f"{ns}:steel_ingot", "B": f"{ns}:breaker_3p_industrial", "C": "minecraft:copper_ingot", "R": "minecraft:repeater"}, "motor_starter")
shaped("mcc_bucket", ["SMS", "S S"], {"S": f"{ns}:steel_ingot", "M": f"{ns}:motor_starter"}, "mcc_bucket")
shaped("mcc_incoming", ["SBS", "CCC", "SSS"], {"S": f"{ns}:steel_ingot", "B": f"{ns}:breaker_3p_industrial", "C": "minecraft:copper_block"}, "mcc_incoming")
shaped("three_phase_heater", ["SSS", "HHH", "SRS"], {"S": f"{ns}:steel_ingot", "H": f"{ns}:electric_heater", "R": "minecraft:redstone"}, "three_phase_heater")
# ---- Phase 4.1: busway, and conductor sizes (docs/phase4-conductors.md) --------------------------------------------
def busway_side(img, d):
	noise(img, (150, 154, 160), 4)
	d.rectangle([0, 0, 15, 15], outline=(96, 100, 108))
	for x in (4, 8, 12):
		d.line([x, 2, x, 13], fill=(184, 115, 51))   # copper bars seen through the vent slots


tex("busway_placeholder", busway_side)
for bus in ("busway_lv", "busway_industrial", "bus_duct_gen"):
	t = {"cable": f"{ns}:block/{art('busway')}", "particle": f"{ns}:block/{art('busway')}"}
	save_json(f"{ASSETS}/models/block/{bus}_core.json", {"textures": t, "elements": [cuboid([4, 4, 4], [12, 12, 12])]})
	save_json(f"{ASSETS}/models/block/{bus}_arm.json", {"textures": t, "elements": [cuboid([4, 4, 0], [12, 12, 4])]})
	save_json(f"{ASSETS}/models/block/{bus}_inventory.json", {"parent": "minecraft:block/block",
		"display": {"gui": {"rotation": [30, 225, 0], "scale": [0.9, 0.9, 0.9]}, "ground": {"scale": [0.6, 0.6, 0.6]}},
		"textures": t, "elements": [cuboid([0, 4, 4], [16, 12, 12])]})
	save_json(f"{ASSETS}/blockstates/{bus}.json", {"multipart": [{"apply": {"model": f"{ns}:block/{bus}_core"}}] + [
		{"when": {side: "true"}, "apply": {"model": f"{ns}:block/{bus}_arm", **rot}} for side, rot in arms.items()]})
	save_json(f"{ASSETS}/items/{bus}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{bus}_inventory"}})
	shaped(bus, ["SCS", "CCC", "SCS"], {"S": f"{ns}:steel_ingot", "C": "minecraft:copper_block"}, bus, 4)
pick = json.load(open(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json"))
pick["values"] += [f"{ns}:busway_lv", f"{ns}:busway_industrial", f"{ns}:bus_duct_gen"]
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", pick)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.busway_lv": "Busway (208Y/120 V, A B C N G)", "block.wildcatter.busway_industrial": "Busway (480 V, A B C N G)",
	"block.wildcatter.bus_duct_gen": "Generator Bus Duct (13.8 kV, 1,200 A, A B C N G)",
	"itemGroup.wildcatter": "Wildcatter"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 4b: gensets (docs/multiblocks/genset.md) and the turning parts (docs/kinetics-and-sound.md) ------------------
def box(frm, to, texture):
	"""A model element whose faces take their UVs from where they sit in the block (no squashing)."""
	return {"from": frm, "to": to, "faces": {f: {"texture": texture} for f in ("north", "south", "east", "west", "up", "down")}}


def paint(base, stripes=None, bolts=True):
	def draw(img, d):
		noise(img, base, 6)
		d.rectangle([0, 0, 15, 15], outline=tuple(max(0, c - 40) for c in base))
		for y, color in (stripes or []):
			d.line([1, y, 14, y], fill=color)
		if bolts:
			for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
				d.point((x, y), fill=(200, 200, 200))
	return draw


def grille(img, d):
	noise(img, (60, 64, 70), 4)
	for y in range(1, 15, 2):
		d.line([1, y, 14, y], fill=(150, 156, 164))


def panel_face(img, d):
	noise(img, (120, 126, 132), 4)
	d.rectangle([2, 2, 13, 7], fill=(30, 40, 30))
	for x in (4, 8, 12):
		d.ellipse([x - 1, 3, x + 1, 5], fill=(230, 230, 200))
	d.rectangle([3, 10, 5, 12], fill=(60, 200, 90))
	d.rectangle([7, 10, 9, 12], fill=(220, 50, 40))
	d.rectangle([11, 9, 13, 13], fill=(40, 40, 44))


def rotor_tex(img, d):
	noise(img, (70, 74, 80), 5)
	d.line([0, 7, 15, 7], fill=(230, 190, 40))   # a painted stripe, so the turning shows
	d.line([0, 8, 15, 8], fill=(230, 190, 40))


tex("genset_engine_placeholder", paint((196, 160, 40), [(5, (90, 90, 96)), (11, (90, 90, 96))]))     # engine yellow, rocker covers
tex("genset_gas_engine_placeholder", paint((60, 110, 160), [(5, (90, 90, 96)), (11, (90, 90, 96))]))
tex("alternator_placeholder", paint((70, 90, 120), [(3, (40, 50, 70)), (12, (40, 50, 70))]))
tex("radiator_core_placeholder", grille)
tex("radiator_frame_placeholder", paint((80, 84, 90), bolts=False))
tex("fuel_tank_placeholder", paint((90, 96, 90), [(8, (60, 64, 60))]))
tex("exhaust_stack_placeholder", paint((60, 56, 52), bolts=False))
tex("genset_panel_front_placeholder", panel_face)
tex("genset_panel_side_placeholder", paint((120, 126, 132)))
tex("rotor_placeholder", rotor_tex)
G = {n: f"{ns}:block/{art(n)}" for n in ("genset_engine", "genset_gas_engine", "alternator", "radiator_core", "radiator_frame", "fuel_tank",
	"exhaust_stack", "genset_panel_front", "genset_panel_side", "rotor")}
# Phase 4.5b: every genset part is true size. Its blockstate, piece models, item model and collision come from
# tools/models/<part>.py (run at the end of this script); here only its loot (piece 0 drops the item).
for name in ("genset_diesel_engine", "genset_gas_engine", "alternator", "radiator", "fuel_tank", "exhaust_stack", "genset_control_panel",
		"genset_base_frame", "exhaust_silencer_large"):
	big_part_loot(name)
# the genset's turning parts are drawn by tools/models/rotors.py; the tower fan and disconnect blades below still use this texture
r = {"rotor": G["rotor"], "particle": G["rotor"]}

# crafted parts (items) and recipes: steel and copper into parts, parts into the engine and alternator
PART_COLORS = {"engine_block_casting": (80, 80, 86), "piston": (170, 174, 180), "crankshaft": (120, 124, 130), "cylinder_head": (150, 140, 120),
	"fuel_injector": (180, 160, 60), "spark_plug": (230, 230, 230), "stator_winding": (184, 115, 51), "rotor_field": (150, 90, 50), "exciter": (90, 110, 150)}
for name, color in PART_COLORS.items():
	tex(f"{name}_placeholder", lambda img, d, c=color: (d.rectangle([3, 3, 12, 12], fill=c + (255,), outline=(40, 40, 40, 255)), d.line([3, 8, 12, 8], fill=(40, 40, 40, 255))), "item")
	save_json(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{art(name)}"}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{name}"}})
S_, I_, C_ = f"{ns}:steel_ingot", "minecraft:iron_ingot", "minecraft:copper_ingot"
shaped("engine_block_casting", ["SSS", "SBS", "SSS"], {"S": S_, "B": "minecraft:iron_block"}, "engine_block_casting")
shapeless("piston", [S_, S_, "minecraft:iron_nugget"], "piston", 2)
shaped("crankshaft", ["S  ", " S ", "  S"], {"S": S_}, "crankshaft")
shaped("cylinder_head", ["SSS", "I I"], {"S": S_, "I": I_}, "cylinder_head")
shapeless("fuel_injector", [S_, "minecraft:iron_nugget", "minecraft:redstone"], "fuel_injector", 2)
shapeless("spark_plug", ["minecraft:iron_nugget", "minecraft:quartz", C_], "spark_plug", 2)
shaped("stator_winding", ["CCC", "CSC", "CCC"], {"C": C_, "S": S_}, "stator_winding")
shaped("rotor_field", ["CSC", "SIS", "CSC"], {"C": C_, "S": S_, "I": "minecraft:iron_block"}, "rotor_field")
shaped("exciter", ["CRC", "SDS"], {"C": C_, "R": "minecraft:redstone", "S": S_, "D": "minecraft:comparator"}, "exciter")
shaped("genset_diesel_engine", ["IHI", "PCP", "PBP"], {"I": f"{ns}:fuel_injector", "H": f"{ns}:cylinder_head", "P": f"{ns}:piston", "C": f"{ns}:crankshaft", "B": f"{ns}:engine_block_casting"}, "genset_diesel_engine")
shaped("genset_gas_engine", ["IHI", "PCP", "PBP"], {"I": f"{ns}:spark_plug", "H": f"{ns}:cylinder_head", "P": f"{ns}:piston", "C": f"{ns}:crankshaft", "B": f"{ns}:engine_block_casting"}, "genset_gas_engine")
shaped("alternator", ["SWS", "WRW", "SXS"], {"S": S_, "W": f"{ns}:stator_winding", "R": f"{ns}:rotor_field", "X": f"{ns}:exciter"}, "alternator")
shaped("radiator", ["SBS", "BCB", "SBS"], {"S": S_, "B": "minecraft:iron_bars", "C": "minecraft:copper_block"}, "radiator")
shaped("fuel_tank", ["SSS", "S S", "SSS"], {"S": S_}, "fuel_tank")
shaped("exhaust_stack", ["S", "S", "S"], {"S": S_}, "exhaust_stack")
shaped("exhaust_silencer_large", ["SSS", "S S", "SSS"], {"S": f"{ns}:exhaust_stack"}, "exhaust_silencer_large")
shaped("genset_base_frame", ["SIS", "SIS"], {"S": S_, "I": "minecraft:iron_block"}, "genset_base_frame")
shaped("genset_control_panel", ["SGS", "RBR", "SCS"], {"S": S_, "G": "minecraft:glass_pane", "R": "minecraft:redstone", "B": f"{ns}:breaker_3p_industrial", "C": "minecraft:comparator"}, "genset_control_panel")
GENSET_BLOCKS = ("genset_diesel_engine", "genset_gas_engine", "alternator", "radiator", "fuel_tank", "exhaust_stack", "genset_control_panel",
	"genset_base_frame", "exhaust_silencer_large")
pick = json.load(open(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json"))
pick["values"] += [f"{ns}:{n}" for n in GENSET_BLOCKS]
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", pick)
stone = json.load(open(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json"))
stone["values"] += [f"{ns}:{n}" for n in GENSET_BLOCKS]
save_json(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json", stone)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({
	"block.wildcatter.genset_diesel_engine": "Genset Diesel Engine (500 kW)", "block.wildcatter.genset_gas_engine": "Genset Gas Engine (1 MW)",
	"block.wildcatter.alternator": "Alternator (480 V)", "block.wildcatter.radiator": "Radiator", "block.wildcatter.fuel_tank": "Sub-Base Fuel Tank (2,000 L)",
	"block.wildcatter.exhaust_stack": "Exhaust Silencer (500 kW)", "block.wildcatter.exhaust_silencer_large": "Exhaust Silencer (1 MW)",
	"block.wildcatter.genset_base_frame": "Genset Base Frame (1 MW gas set)", "block.wildcatter.genset_control_panel": "Genset Control Panel", "block.wildcatter.rotor": "Rotor",
	"item.wildcatter.engine_block_casting": "Engine Block Casting", "item.wildcatter.piston": "Piston and Rod", "item.wildcatter.crankshaft": "Crankshaft",
	"item.wildcatter.cylinder_head": "Cylinder Head", "item.wildcatter.fuel_injector": "Fuel Injector", "item.wildcatter.spark_plug": "Spark Plug",
	"item.wildcatter.stator_winding": "Stator Winding", "item.wildcatter.rotor_field": "Rotor Field", "item.wildcatter.exciter": "Exciter and Regulator",
	"subtitles.wildcatter.diesel_idle": "Diesel engine idles", "subtitles.wildcatter.diesel_load": "Diesel engine labours", "subtitles.wildcatter.gas_engine": "Gas engine runs",
	"subtitles.wildcatter.alternator_whine": "Alternator whines", "subtitles.wildcatter.fan": "Fan whirs", "subtitles.wildcatter.engine_crank": "Engine cranks", "subtitles.wildcatter.sync_bang": "Generator slams out of step"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 4d: the generator step-up (docs/multiblocks/gsu.md). Phase 4.5: a true-size part; its blockstate, models and
# item model come from tools/models/gsu_transformer.py, which runs at the end of this script.
big_part_loot("gsu_transformer")
shaped("gsu_transformer", ["SDS", "TCT", "SDS"], {"S": f"{ns}:steel_ingot", "D": "minecraft:diamond", "T": f"{ns}:unit_substation", "C": "minecraft:copper_block"}, "gsu_transformer")
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"].append(f"{ns}:gsu_transformer")
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang["block.wildcatter.gsu_transformer"] = "Generator Step-Up Transformer (MV delta / DIST wye, 1500 kVA)"
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 4d: the cooling tower as a column (docs/multiblocks/cooling-tower.md) --------------------------------------
def basin(img, d):
	noise(img, (150, 150, 146), 5)
	d.rectangle([2, 2, 13, 9], fill=(60, 110, 150))   # the cold water
	d.rectangle([0, 0, 15, 15], outline=(110, 110, 106))


def chevrons(img, d):
	noise(img, (200, 200, 196), 4)
	for y in range(0, 16, 4):
		d.line([0, y + 3, 7, y], fill=(120, 124, 128))
		d.line([8, y, 15, y + 3], fill=(120, 124, 128))


# Phase 4.5c: the tower's parts are true size: blockstates, piece models, items and collision come from tools/models/<part>.py
# (run at the end of this script); here only their loot (piece 0 drops the item).
for name in ("tower_basin", "cooling_fill", "drift_eliminator", "cooling_tower"):
	big_part_loot(name)
shaped("tower_basin", ["S S", "SWS", "SSS"], {"S": "minecraft:smooth_stone", "W": "minecraft:water_bucket"}, "tower_basin")
shaped("drift_eliminator", ["P P", " P ", "P P"], {"P": f"{ns}:plastic_sheet"}, "drift_eliminator")
for tag in ("mineable/pickaxe",):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:tower_basin", f"{ns}:drift_eliminator"]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.tower_basin": "Cooling Tower Basin (4 x 4 m)", "block.wildcatter.drift_eliminator": "Drift Eliminators (4 x 4 m)",
	"block.wildcatter.cooling_fill": "Cooling Tower Fill (4 x 4 m)", "block.wildcatter.cooling_tower": "Cooling Tower Fan Deck (7.5 kW, 480 V)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 4d: the 1 MW steam plant (docs/multiblocks/steam-plant.md), dressed in the old steam machines' Ludo art -------------
def condenser_face(img, d):
	noise(img, (96, 104, 110), 5)
	for y in range(2, 15, 3):
		d.line([1, y, 14, y], fill=(150, 160, 168))   # the tube bundle seen through the shell
	d.rectangle([0, 0, 15, 15], outline=(60, 66, 72))


B = lambda n: f"{ns}:block/{n}"
# Phase 4.5c: the plant's parts are true size (tools/models/<part>.py); here only their loot
for name in ("boiler_burner", "boiler_drum", "superheater", "turbine_casing", "condenser", "feed_pump", "turbine_generator"):
	big_part_loot(name)
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in ("boiler_burner", "boiler_drum", "superheater", "turbine_casing", "condenser", "feed_pump", "turbine_generator")]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
S_ = f"{ns}:steel_ingot"
shaped("boiler_burner", ["SBS", "BFB", "SBS"], {"S": S_, "B": "minecraft:bricks", "F": "minecraft:blast_furnace"}, "boiler_burner")
shaped("boiler_drum", ["SSS", "SWS", "SSS"], {"S": S_, "W": "minecraft:water_bucket"}, "boiler_drum")
shaped("superheater", ["SCS", "CSC", "SCS"], {"S": S_, "C": "minecraft:copper_ingot"}, "superheater")
shaped("turbine_casing", ["SSS", "BCB", "SSS"], {"S": S_, "B": "minecraft:iron_bars", "C": f"{ns}:crankshaft"}, "turbine_casing")
shaped("turbine_generator", ["SWS", "WRW", "SXS"], {"S": S_, "W": f"{ns}:stator_winding", "R": f"{ns}:rotor_field", "X": f"{ns}:exciter"}, "turbine_generator")
shaped("condenser", ["SPS", "PCP", "SPS"], {"S": S_, "P": f"{ns}:pipe", "C": "minecraft:copper_block"}, "condenser")
shaped("feed_pump", ["SPS", "MCM"], {"S": S_, "P": f"{ns}:pump", "M": "minecraft:piston", "C": "minecraft:copper_ingot"}, "feed_pump")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.boiler_burner": "Boiler Burner", "block.wildcatter.boiler_drum": "Package Boiler (D-type, 4.5 t/h)", "block.wildcatter.superheater": "Superheater",
	"block.wildcatter.turbine_casing": "Steam Turbine (1 MW)", "block.wildcatter.condenser": "Condenser", "block.wildcatter.feed_pump": "Boiler Feed Pump (480 V)",
	"block.wildcatter.turbine_generator": "Turbine Generator (4.16 kV, 1,250 kVA)",
	"subtitles.wildcatter.turbine_whine": "Turbine whines", "subtitles.wildcatter.burner_roar": "Burner roars", "subtitles.wildcatter.motor_hum": "Motor hums"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 4e: the distribution substation (docs/multiblocks/substation.md) -------------------------------------------
def tube(img, d):
	noise(img, (190, 192, 196), 4)
	for y in (4, 8, 12):
		d.line([0, y, 15, y], fill=(150, 152, 158))


def insulator_post(img, d):
	noise(img, (110, 70, 50), 4)
	for y in range(1, 16, 3):
		d.line([2, y, 13, y], fill=(150, 100, 70))   # the porcelain sheds


# Phase 4.5d: the yard's parts are true size: blockstates, piece models, items and collision come from tools/models/<part>.py
# (run at the end of this script); here only their loot (piece 0 drops the item).
SUB = ("power_transformer", "relay_panel", "hv_breaker_3p", "dist_breaker_3p", "disconnect_hv", "disconnect_dist", "bus_hv", "bus_dist")
for name in SUB:
	big_part_loot(name)
save_json(f"{ASSETS}/models/block/rotor_disconnect_blade.json", {"textures": r, "elements": [   # hinged at its south end (the model's centre)
	box([5, 7.5, -3], [6, 8.5, 8], "#rotor"), box([10, 7.5, -3], [11, 8.5, 8], "#rotor"), box([5, 7.5, -3], [11, 8.5, -2], "#rotor")]})
# the yard's fence (decoration): chain-link fabric and the barbed-wire top, joined like iron bars, see-through
for name, texture in (("chain_link_fence", "fence_chain_link"), ("barbed_wire_top", "fence_barbed_wire")):
	t = f"{ns}:block/mat/{texture}"
	for part in ("post_ends", "post", "cap", "cap_alt", "side", "side_alt"):
		save_json(f"{ASSETS}/models/block/{name}_{part}.json", {"parent": f"minecraft:block/template_bars_{part}", "textures": {"bars": t, "edge": t, "particle": t}})
	m = lambda part, y=0: {"model": f"{ns}:block/{name}_{part}", **({"y": y} if y else {})}
	alone = {d: "false" for d in ("north", "east", "south", "west")}
	only = lambda d: {**alone, d: "true"}
	save_json(f"{ASSETS}/blockstates/{name}.json", {"multipart": [   # as panes and bars join: a post alone, caps at a lone end, sides
		{"apply": m("post_ends")}, {"when": alone, "apply": m("post")},
		{"when": only("north"), "apply": m("cap")}, {"when": only("east"), "apply": m("cap", 90)},
		{"when": only("south"), "apply": m("cap_alt")}, {"when": only("west"), "apply": m("cap_alt", 90)},
		{"when": {"north": "true"}, "apply": m("side")}, {"when": {"east": "true"}, "apply": m("side", 90)},
		{"when": {"south": "true"}, "apply": m("side_alt")}, {"when": {"west": "true"}, "apply": m("side_alt", 90)}]})
	save_json(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": t}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{name}"}})
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
shaped("chain_link_fence", ["III", "III"], {"I": "minecraft:iron_bars"}, "chain_link_fence", 6)
shaped("barbed_wire_top", ["I I", " I ", "I I"], {"I": "minecraft:iron_nugget"}, "barbed_wire_top", 6)
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in SUB + ("chain_link_fence", "barbed_wire_top")]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
shaped("power_transformer", ["SDS", "TCT", "SDS"], {"S": S_, "D": "minecraft:diamond_block", "T": f"{ns}:substation_transformer_10", "C": "minecraft:copper_block"}, "power_transformer")
shaped("hv_breaker_3p", ["SCS", "DRD", "SCS"], {"S": S_, "C": "minecraft:copper_block", "D": "minecraft:diamond", "R": "minecraft:comparator"}, "hv_breaker_3p")
shaped("dist_breaker_3p", ["SCS", "IRI", "SCS"], {"S": S_, "C": "minecraft:copper_block", "I": "minecraft:iron_ingot", "R": "minecraft:comparator"}, "dist_breaker_3p")
shaped("disconnect_hv", ["I I", "ISI"], {"I": f"{ns}:insulator_hv", "S": S_}, "disconnect_hv")
shaped("disconnect_dist", ["I I", "ISI"], {"I": f"{ns}:insulator_dist", "S": S_}, "disconnect_dist")
shaped("bus_hv", ["AAA", "I I"], {"A": "minecraft:iron_ingot", "I": f"{ns}:insulator_hv"}, "bus_hv")
shaped("bus_dist", ["AAA", "I I"], {"A": "minecraft:iron_ingot", "I": f"{ns}:insulator_dist"}, "bus_dist")
shaped("relay_panel", ["SGS", "CRC", "SSS"], {"S": S_, "G": "minecraft:glass_pane", "C": "minecraft:comparator", "R": "minecraft:redstone_block"}, "relay_panel")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for k in [k for k in lang if "distribution_transformer" in k]:
	del lang[k]
lang.update({"block.wildcatter.power_transformer": "Substation Power Transformer (HV delta / DIST wye, 20 MVA)", "block.wildcatter.relay_panel": "Relay Panel (50/51)",
	"block.wildcatter.hv_breaker_3p": "Substation Breaker (HV, three-pole)", "block.wildcatter.dist_breaker_3p": "Feeder Breaker (DIST, three-pole)",
	"block.wildcatter.disconnect_hv": "Disconnect Switch (HV)", "block.wildcatter.disconnect_dist": "Disconnect Switch (DIST)",
	"block.wildcatter.bus_hv": "Station Bus (HV)", "block.wildcatter.bus_dist": "Station Bus (DIST)",
	"block.wildcatter.chain_link_fence": "Chain-Link Fence", "block.wildcatter.barbed_wire_top": "Barbed-Wire Fence Top", "subtitles.wildcatter.disconnect_arc": "Disconnect arcs",
	"subtitles.wildcatter.hv_breaker": "Power breaker operates", "subtitles.wildcatter.fuse_pop": "Fuse blows", "subtitles.wildcatter.flashover": "Insulator flashes over"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 4.5e: the overhead line at true size (docs/multiblocks/poles.md, padmount.md) -------------------------------------
# blockstates, piece models, items and collision come from tools/models/<part>.py; here loot, names, tags and recipes. The
# older one-block pole and crossarm stay (their spans keep working) but are no longer crafted.
LINE = ("wood_pole_30", "wood_pole_40", "wood_pole_75", "crossarm_dist", "crossbeam_138", "x_brace_138")
for name in LINE + ("padmount_208", "padmount_480", "unit_substation"):
	big_part_loot(name)
for name in ("pole", "crossarm"):
	if os.path.exists(f"{DATA}/recipe/{name}.json"):
		os.remove(f"{DATA}/recipe/{name}.json")
shapeless("wood_pole_30", ["#minecraft:logs"] * 4 + ["minecraft:coal"], "wood_pole_30")        # coal tar for the creosote
shapeless("wood_pole_40", ["#minecraft:logs"] * 5 + ["minecraft:coal"], "wood_pole_40")
shapeless("wood_pole_75", ["#minecraft:logs"] * 8 + ["minecraft:coal"], "wood_pole_75")
shaped("crossarm_dist", ["PPP", "N N"], {"P": "#minecraft:planks", "N": "minecraft:iron_nugget"}, "crossarm_dist")
shaped("crossbeam_138", ["PPP", "PPP", "NIN"], {"P": "#minecraft:planks", "N": "minecraft:iron_nugget", "I": "minecraft:iron_ingot"}, "crossbeam_138")
shaped("x_brace_138", ["P P", " N ", "P P"], {"P": "#minecraft:planks", "N": "minecraft:iron_nugget"}, "x_brace_138")
t = json.load(open(f"{ROOT}/data/minecraft/tags/block/mineable/axe.json"))
t["values"] += [f"{ns}:{n}" for n in LINE]
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/axe.json", t)
save_json(f"{ROOT}/data/minecraft/tags/block/climbable.json", {"values": [f"{ns}:pole", f"{ns}:wood_pole_30", f"{ns}:wood_pole_40", f"{ns}:wood_pole_75"]})
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.wood_pole_30": "Wood Pole (30 ft, class 5)", "block.wildcatter.wood_pole_40": "Wood Pole (40 ft, class 4)",
	"block.wildcatter.wood_pole_75": "Wood Pole (75 ft, class 2)", "block.wildcatter.crossarm_dist": "Crossarm (8 ft)",
	"block.wildcatter.crossbeam_138": "H-Frame Crossbeam (HV)", "block.wildcatter.x_brace_138": "H-Frame X-Brace (HV)",
	"block.wildcatter.pole": "Utility Pole Section (old)", "block.wildcatter.crossarm": "Pole Crossarm (old)",
	"block.wildcatter.insulator_hv": "Suspension Insulator String (HV, 138 kV)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 4.5f: building gear and the starter plant at true size (docs/multiblocks/building-gear.md, motor-control-center.md) --
# blockstates, models and items come from tools/models/enclosure.py, fixtures.py, mcc.py and starter_plant.py; here loot, names,
# tags and recipes. The MCC bucket is an item now (it clicks into a starter section); an older world's bucket block drops it.
for name in ("mcc_incoming", "mcc_section", "boiler"):
	big_part_loot(name)
save_json(f"{ASSETS}/models/item/mcc_bucket.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:block/mat/decal_pushbuttons"}})
save_json(f"{ASSETS}/items/mcc_bucket.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/mcc_bucket"}})
shaped("mcc_section", ["SBS", "S S", "SBS"], {"S": S_, "B": "minecraft:iron_bars"}, "mcc_section")
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:mcc_section"]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.mcc_section": "MCC Starter Section (480 V, three buckets)", "item.wildcatter.mcc_bucket": "MCC Bucket (combination starter)",
	"block.wildcatter.mcc_incoming": "MCC Incoming Section (480 V, main breaker)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10a: soft starters and drives (docs/multiblocks/soft-starter.md, vfd.md) -------------------------------------
# blockstates, models and items come from tools/models/drives.py; here loot, names, tags, the bucket items and recipes.
DRIVE_PARTS = ("soft_starter", "drive_cabinet", "soft_starter_mv", "drive_mv_transformer", "drive_mv_cells", "drive_mv_control")
for name in DRIVE_PARTS:
	big_part_loot(name)
for name, decal in (("mcc_soft_starter_bucket", "decal_relays"), ("mcc_drive_bucket", "decal_controller")):
	save_json(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:block/mat/{decal}"}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{name}"}})
shaped("soft_starter", ["SCS", "QMQ", "SSS"], {"S": S_, "C": "minecraft:comparator", "Q": "minecraft:quartz", "M": f"{ns}:motor_starter"}, "soft_starter")
shaped("drive_cabinet", ["SCS", "QMQ", "SRS"], {"S": S_, "C": "minecraft:comparator", "Q": "minecraft:quartz_block", "M": f"{ns}:motor_starter",
	"R": "minecraft:repeater"}, "drive_cabinet")
shaped("soft_starter_mv", ["SIS", "SMS", "SCS"], {"S": S_, "I": "minecraft:iron_block", "M": f"{ns}:soft_starter", "C": "minecraft:copper_block"}, "soft_starter_mv")
shaped("drive_mv_transformer", ["SCS", "CTC", "SSS"], {"S": S_, "C": "minecraft:copper_block", "T": f"{ns}:pole_transformer_mv"}, "drive_mv_transformer")
shaped("drive_mv_cells", ["SQS", "QDQ", "SQS"], {"S": S_, "Q": "minecraft:quartz_block", "D": f"{ns}:drive_cabinet"}, "drive_mv_cells")
shaped("drive_mv_control", ["SCS", "SDS", "SSS"], {"S": S_, "C": "minecraft:comparator", "D": f"{ns}:drive_cabinet"}, "drive_mv_control")
shaped("mcc_soft_starter_bucket", ["QMQ", " C "], {"Q": "minecraft:quartz", "M": f"{ns}:mcc_bucket", "C": "minecraft:comparator"}, "mcc_soft_starter_bucket")
shaped("mcc_drive_bucket", ["QMQ", "RCR"], {"Q": "minecraft:quartz_block", "M": f"{ns}:mcc_bucket", "C": "minecraft:comparator", "R": "minecraft:repeater"},
	"mcc_drive_bucket")
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in DRIVE_PARTS]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.soft_starter": "Soft Starter (480 V)", "block.wildcatter.drive_cabinet": "Variable-Frequency Drive (480 V)",
	"block.wildcatter.soft_starter_mv": "Soft Starter (4.16 kV)", "block.wildcatter.drive_mv_transformer": "MV Drive: Input Transformer Section (4.16 kV)",
	"block.wildcatter.drive_mv_cells": "MV Drive: Power Cell Section (4.16 kV)", "block.wildcatter.drive_mv_control": "MV Drive: Output and Control Section (4.16 kV)",
	"item.wildcatter.mcc_soft_starter_bucket": "MCC Bucket (soft starter)", "item.wildcatter.mcc_drive_bucket": "MCC Bucket (drive, up to 100 hp)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10b1: DC bus bars (docs/multiblocks/dc-bus.md) ------------------------------------------------------------------
# A metre of bare copper flat bar on two porcelain standoffs, drawn along z; the blockstate turns it to x (y 90) or up (x 90).
# 4 kA: 4 bars of 100 x 10 mm; 16 kA: 8 bars of 200 x 10 mm (1.6 and 3.2 sixteenths tall, a sixteenth wide).
DC_BUSES = {"dc_bus_4k": (4, 1.6), "dc_bus_16k": (8, 3.2)}
for name, (bars, tall) in DC_BUSES.items():
	els = []
	for z in (3, 11):
		els.append(cuboid([7, 0, z], [9, 5, z + 2], "#glaze"))          # the porcelain standoff
		els.append(cuboid([4, 5, z], [12, 6, z + 2], "#steel"))        # its bar clamp
	pitch = 7.0 / bars
	for k in range(bars):
		x = 4.5 + k * pitch
		els.append(cuboid([round(x, 3), 6, 0], [round(x + pitch * 0.6, 3), round(6 + tall, 3), 16], "#copper"))
	save_json(f"{ASSETS}/models/block/{name}.json", {"textures": {"glaze": f"{ns}:block/mat/mat_porcelain_brown", "steel": f"{ns}:block/mat/mat_galvanized",
		"copper": f"{ns}:block/mat/mat_copper", "particle": f"{ns}:block/mat/mat_copper"}, "elements": els})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {"axis=z": {"model": f"{ns}:block/{name}"}, "axis=x": {"model": f"{ns}:block/{name}", "y": 90},
		"axis=y": {"model": f"{ns}:block/{name}", "x": 90}}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{name}"}})
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
shaped("dc_bus_4k", ["CCC", "B B"], {"C": "minecraft:copper_ingot", "B": "minecraft:brick"}, "dc_bus_4k", 4)
shaped("dc_bus_16k", ["KKK", "B B"], {"K": "minecraft:copper_block", "B": "minecraft:brick"}, "dc_bus_16k", 4)
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in DC_BUSES if f"{ns}:{n}" not in t["values"]]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.dc_bus_4k": "DC Bus Bar (4 kA copper)", "block.wildcatter.dc_bus_16k": "DC Bus Bar (16 kA copper)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# Every conductor item and the sizes it comes in (the Java side: energy/ConductorSize).
BUILDING = ["awg_12", "awg_10", "awg_6", "awg_2", "awg_4_0", "kcmil_500"]
ACSR = ["acsr_sparrow", "acsr_raven", "acsr_penguin", "acsr_linnet", "acsr_drake"]
MV_CABLE = ["mv_1_0", "mv_4_0", "mv_500"]
# how many of one size make one of the next: copper roughly in proportion to the cross-section
UPSIZE = {"awg_10": 2, "awg_6": 3, "awg_2": 3, "awg_4_0": 3, "kcmil_500": 2, "acsr_raven": 2, "acsr_penguin": 2, "acsr_linnet": 2, "acsr_drake": 2, "mv_4_0": 2, "mv_500": 2}
FAMILY = {}
for role in ("hot", "neutral"):
	for prefix in ("", "wool_", "plastic_", "industrial_", "industrial_plastic_"):
		FAMILY[f"{prefix}wire_{role}"] = BUILDING
	for prefix in ("mv_", "mv_plastic_", "dist_", "dist_plastic_", "hv_", "hv_plastic_"):
		FAMILY[f"{prefix}wire_{role}"] = ACSR
FAMILY.update({"cable_lv": BUILDING, "cable_industrial": BUILDING, "cable_mv": MV_CABLE, "cable_dist": MV_CABLE, "cable_hv": MV_CABLE})
for tier, fam in (("lv", BUILDING), ("mv", ACSR), ("dist", ACSR), ("hv", ACSR)):
	FAMILY[f"conductor_spool_{tier}"] = fam
	FAMILY[f"covered_conductor_spool_{tier}"] = fam
SIZE_KEY = f"{ns}:conductor_size"
SPOOLS = tuple(n for n in FAMILY if "spool" in n)


def sized(item_id, size):
	return {"fabric:type": "fabric:components", "base": item_id, "components": {SIZE_KEY: size}}


def conductor_of(ingredient):
	if isinstance(ingredient, str) and ingredient.startswith(f"{ns}:") and ingredient[len(ns) + 1:] in FAMILY:
		return ingredient[len(ns) + 1:]
	return None


recipe_dir = f"{DATA}/recipe"
for fname in sorted(os.listdir(recipe_dir)):
	if "__" in fname:
		continue
	path = f"{recipe_dir}/{fname}"
	recipe = json.load(open(path))
	result = recipe.get("result", {}).get("id", "")
	name = result[len(ns) + 1:] if result.startswith(f"{ns}:") else ""
	if name not in FAMILY:
		continue
	family = FAMILY[name]
	slots = recipe["key"] if "key" in recipe else None
	ingredients = list(slots.values()) if slots is not None else recipe["ingredients"]
	same = [i for i in ingredients if conductor_of(i) and FAMILY[conductor_of(i)] is family]
	if not same:
		recipe["result"]["components"] = {SIZE_KEY: family[0]}   # from raw materials: the smallest size
		save_json(path, recipe)
		continue
	# made from conductors of the same family (plastic from bare, cable from wire, spool from wire): one recipe per size
	os.remove(path)
	for size in family:
		variant = json.loads(json.dumps(recipe))
		constrain = lambda i: sized(i, size) if conductor_of(i) and FAMILY[conductor_of(i)] is family else i
		if slots is not None:
			variant["key"] = {k: constrain(v) for k, v in slots.items()}
		else:
			variant["ingredients"] = [constrain(i) for i in recipe["ingredients"]]
		variant["result"]["components"] = {SIZE_KEY: size}
		save_json(f"{recipe_dir}/{fname[:-5]}__{size}.json", variant)
# a bigger size from several of the next smaller one (wire and cable; spools are wound from the size you want)
for name, family in FAMILY.items():
	if name in SPOOLS:
		continue
	for small, big in zip(family, family[1:]):
		shapeless(f"{name}__{big}_from_{small}", [sized(f"{ns}:{name}", small)] * UPSIZE[big], name)
		path = f"{recipe_dir}/{name}__{big}_from_{small}.json"
		recipe = json.load(open(path))
		recipe["result"]["components"] = {SIZE_KEY: big}
		save_json(path, recipe)
# Sized ingredients are loader data: Fabric's "fabric:components" and NeoForge's "neoforge:components" ingredients (Phase 9b).
# The recipes that use one leave the shared data for a copy in each loader's resources.
LOADER_RECIPES = {"fabric": "fabric/src/main/resources/data/wildcatter/recipe", "neoforge": "neoforge/src/main/resources/data/wildcatter/recipe"}
for d in LOADER_RECIPES.values():
	if os.path.isdir(d):
		shutil.rmtree(d)
def _neoforge_ingredient(i):
	if isinstance(i, dict) and i.get("fabric:type") == "fabric:components":
		return {"neoforge:ingredient_type": "neoforge:components", "items": i["base"], "components": i["components"], "strict": False}
	return i
for fname in sorted(os.listdir(recipe_dir)):
	path = f"{recipe_dir}/{fname}"
	if "fabric:type" not in open(path).read():
		continue
	recipe = json.load(open(path))
	save_json(f"{LOADER_RECIPES['fabric']}/{fname}", recipe)
	if "key" in recipe:
		recipe["key"] = {k: _neoforge_ingredient(v) for k, v in recipe["key"].items()}
	else:
		recipe["ingredients"] = [_neoforge_ingredient(i) for i in recipe["ingredients"]]
	save_json(f"{LOADER_RECIPES['neoforge']}/{fname}", recipe)
	os.remove(path)
# conductor blocks drop themselves; CableBlock and MultiCableBlock put the size on the drop
for name in [n for n in FAMILY if n not in SPOOLS] + ["busway_lv", "busway_industrial", "bus_duct_gen"]:
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
		"entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],   # the block adds the size (CableBlock.getDrops)
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
print("assets written")

# ---- Phase 4.5g: the well site at true size (docs/multiblocks/well-site.md) ---------------------------------------------------
# blockstates, models and items come from tools/models/drilling_rig.py, pumpjack.py and wellheads.py; here loot, names, tags and
# the oil wellhead's recipe, and the rotor blockstate's new parts.
for name in ("drilling_rig", "pumpjack", "gas_wellhead"):
	big_part_loot(name)
save_json(f"{DATA}/loot_table/blocks/oil_wellhead.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:oil_wellhead"}],
	"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
shaped("oil_wellhead", ["SCS", " S ", "SBS"], {"S": S_, "C": "minecraft:copper_ingot", "B": "minecraft:iron_block"}, "oil_wellhead")
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:oil_wellhead"]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.drilling_rig": "Drilling Rig (small mast rig, 40 hp)", "block.wildcatter.pumpjack": "Pumpjack (API 80-119-54, 10 hp)",
	"block.wildcatter.gas_wellhead": "Gas Wellhead (Christmas tree)", "block.wildcatter.oil_wellhead": "Oil Wellhead (rod pumping)",
	"block.wildcatter.gas_line": "Steel Pipe (4 in, oil and gas)", "block.wildcatter.pipe": "Water Pipe (2 in)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)
ROTOR_PARTS = ("radiator_fan", "crank_pulley", "coupling", "tower_fan", "disconnect_blade", "alternator_fan", "turbine_coupling", "pump_coupling",
	"disconnect_arm_hv", "disconnect_arm_hv_back", "disconnect_blade_dist", "pumpjack_crank", "pumpjack_pitman_low", "pumpjack_pitman_high",
	"pumpjack_beam_front", "pumpjack_beam_mid", "pumpjack_beam_back", "pumpjack_rod", "pumpjack_rod_low", "pumpjack_bridle",
	"rig_carriage", "rig_spindle", "rig_pipe", "starter_coupling", "flyball_governor", "starter_pump_coupling")
save_json(f"{ASSETS}/blockstates/rotor.json", {"variants": {f"part={p}": {"model": f"{ns}:block/rotor_{p}"} for p in ROTOR_PARTS}})

# ---- Phase 4.5h: the 1,000 hp land rig (docs/multiblocks/land-rig.md) -----------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/land_rig.py; here loot, names, tags, recipes, the top drive's item
# and the rotor blockstate's new parts.
RIG_PARTS = ("rig_substructure", "rig_mast", "rig_drawworks", "mud_pump", "mud_tanks", "rig_drive_house")
for name in RIG_PARTS:
	big_part_loot(name)
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in RIG_PARTS]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
shaped("rig_substructure", ["SSS", "B B", "S S"], {"S": S_, "B": "minecraft:iron_block"}, "rig_substructure")
shaped("rig_mast", ["S S", "SBS", "S S"], {"S": S_, "B": "minecraft:iron_bars"}, "rig_mast")
shaped("rig_drawworks", ["SCS", "CXC", "SSS"], {"S": S_, "C": C_, "X": f"{ns}:crankshaft"}, "rig_drawworks")
shaped("mud_pump", ["SPS", "CXC", "SSS"], {"S": S_, "P": "minecraft:piston", "C": C_, "X": f"{ns}:crankshaft"}, "mud_pump")
shaped("mud_tanks", ["S S", "S S", "SSS"], {"S": S_}, "mud_tanks")
shaped("rig_drive_house", ["SSS", "CRC", "SSS"], {"S": S_, "C": C_, "R": "minecraft:redstone_block"}, "rig_drive_house")
shaped("top_drive", ["SCS", "CXC", "SPS"], {"S": S_, "C": C_, "X": f"{ns}:crankshaft", "P": f"{ns}:gas_line"}, "top_drive")
save_json(f"{ASSETS}/models/item/top_drive.json", {"parent": f"{ns}:block/rotor_top_drive", "display": {
	"gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.45, 0.45, 0.45]}, "ground": {"scale": [0.3, 0.3, 0.3]},
	"fixed": {"scale": [0.5, 0.5, 0.5]}, "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.3, 0.3, 0.3]},
	"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.35, 0.35, 0.35]}, "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.35, 0.35, 0.35]}}})
save_json(f"{ASSETS}/items/top_drive.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/top_drive"}})
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.rig_substructure": "Land Rig Substructure (the controller, 9 x 9 m)", "block.wildcatter.rig_mast": "Land Rig Mast (43 m)",
	"block.wildcatter.rig_drawworks": "Drawworks (1,000 hp)", "block.wildcatter.mud_pump": "Mud Pump (1,000 hp triplex)",
	"block.wildcatter.mud_tanks": "Mud Tanks (60 m3)", "block.wildcatter.rig_drive_house": "Rig Drive House (4.16 kV, 2,500 kVA)",
	"item.wildcatter.top_drive": "Top Drive (250-ton, 400 hp)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)
ROTOR_PARTS += ("top_drive", "top_drive_quill", "mud_pump_sheave", "mud_pump_bull_wheel", "drawworks_drum")
save_json(f"{ASSETS}/blockstates/rotor.json", {"variants": {f"part={p}": {"model": f"{ns}:block/rotor_{p}"} for p in ROTOR_PARTS}})

# ---- Phase 5a: the lease (docs/multiblocks/lease.md) ------------------------------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/lease.py; here loot, names, tags, recipes and the coupling rotor.
LEASE_PARTS = ("separator", "stock_tank", "water_tank", "flare_stack", "injection_pump")
for name in LEASE_PARTS:
	big_part_loot(name)
save_json(f"{DATA}/loot_table/blocks/disposal_wellhead.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:disposal_wellhead"}],
	"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in LEASE_PARTS + ("disposal_wellhead",)]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
shaped("separator", ["SSS", "PGP", "SSS"], {"S": S_, "P": f"{ns}:gas_line", "G": "minecraft:glass"}, "separator")
shaped("stock_tank", ["SSS", "S S", "SSS"], {"S": S_}, "stock_tank")
shaped("water_tank", ["PPP", "P P", "PPP"], {"P": f"{ns}:plastic_sheet"}, "water_tank")
shaped("flare_stack", [" F ", " P ", " P "], {"F": "minecraft:flint_and_steel", "P": f"{ns}:gas_line"}, "flare_stack")
shaped("injection_pump", ["SPS", "CXC", "SSS"], {"S": S_, "P": f"{ns}:gas_line", "C": "minecraft:copper_ingot", "X": f"{ns}:crankshaft"}, "injection_pump")
shaped("disposal_wellhead", ["SPS", " S ", "SBS"], {"S": S_, "P": f"{ns}:gas_line", "B": "minecraft:iron_block"}, "disposal_wellhead")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.separator": "Three-Phase Separator (36 in x 10 ft)", "block.wildcatter.stock_tank": "Stock Tank (400 bbl steel)",
	"block.wildcatter.water_tank": "Water Tank (400 bbl fiberglass)", "block.wildcatter.flare_stack": "Flare Stack (10 m)",
	"block.wildcatter.injection_pump": "Injection Pump (H-pump, 100 hp)", "block.wildcatter.disposal_wellhead": "Disposal Wellhead (saltwater disposal)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)
ROTOR_PARTS += ("injection_pump_coupling",)
save_json(f"{ASSETS}/blockstates/rotor.json", {"variants": {f"part={p}": {"model": f"{ns}:block/rotor_{p}"} for p in ROTOR_PARTS}})

# ---- Phase 5b: the gas plant (docs/multiblocks/gas-plant.md) ------------------------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/gas_plant.py; here loot, names, tags, recipes and the rotors.
GAS_PLANT_PARTS = ("gas_compressor", "teg_dehydrator", "ngl_chiller", "refrigeration_compressor", "stabilizer", "lpg_bullet")
for name in GAS_PLANT_PARTS:
	big_part_loot(name)
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in GAS_PLANT_PARTS]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
shaped("gas_compressor", ["SPS", "CXC", "SBS"], {"S": S_, "P": f"{ns}:gas_line", "C": "minecraft:copper_block", "X": f"{ns}:crankshaft", "B": "minecraft:iron_block"}, "gas_compressor")
shaped("teg_dehydrator", ["SPS", "SGS", "SFS"], {"S": S_, "P": f"{ns}:gas_line", "G": "minecraft:glass", "F": "minecraft:furnace"}, "teg_dehydrator")
shaped("ngl_chiller", ["SPS", "IWI", "SPS"], {"S": S_, "P": f"{ns}:gas_line", "I": "minecraft:packed_ice", "W": "minecraft:white_wool"}, "ngl_chiller")
shaped("refrigeration_compressor", ["SCS", "CXC", "SPS"], {"S": S_, "C": "minecraft:copper_ingot", "X": f"{ns}:crankshaft", "P": f"{ns}:gas_line"}, "refrigeration_compressor")
shaped("stabilizer", ["SPS", "S S", "SFS"], {"S": S_, "P": f"{ns}:gas_line", "F": "minecraft:furnace"}, "stabilizer")
shaped("lpg_bullet", ["SSS", "SPS", "SSS"], {"S": "minecraft:iron_block", "P": f"{ns}:gas_line"}, "lpg_bullet")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.gas_compressor": "Gas Compressor (400 hp, 4.16 kV)", "block.wildcatter.teg_dehydrator": "TEG Dehydrator (3 MMscfd)",
	"block.wildcatter.ngl_chiller": "NGL Chiller", "block.wildcatter.refrigeration_compressor": "Refrigeration Compressor (150 hp)",
	"block.wildcatter.stabilizer": "Stabilizer (debutanizer, 12 m)", "block.wildcatter.lpg_bullet": "LPG Bullet (18,000 gal)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)
ROTOR_PARTS += ("gas_compressor_coupling", "gas_compressor_fan", "refrigeration_coupling")
save_json(f"{ASSETS}/blockstates/rotor.json", {"variants": {f"part={p}": {"model": f"{ns}:block/rotor_{p}"} for p in ROTOR_PARTS}})

# ---- Phase 5c and 5d: the crude units and the cracker (docs/multiblocks/crude-units.md, cracker.md) ---------------------------------
# blockstates, models, shapes and items come from tools/models/crude_units.py and cracker.py; here loot, names, tags, recipes, rotors.
REFINING_PARTS = ("topping_column", "topping_heater", "topping_skid", "atmospheric_column", "crude_heater", "desalter", "crude_charge_pump",
	"side_strippers", "overhead_condenser", "cracking_furnace", "quench_tower", "cracked_gas_compressor", "cold_box", "polyethylene_reactor", "pelletizer")
for name in REFINING_PARTS:
	big_part_loot(name)
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] = [v for v in t["values"] if v not in (f"{ns}:refinery", f"{ns}:refinery_column")] + [f"{ns}:{n}" for n in REFINING_PARTS]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
P_, F_ = f"{ns}:gas_line", "minecraft:blast_furnace"
shaped("topping_column", ["SPS", "S S", "SPS"], {"S": S_, "P": P_}, "topping_column")
shaped("topping_heater", ["SSS", "SFS", "SPS"], {"S": S_, "F": F_, "P": P_}, "topping_heater")
shaped("topping_skid", ["SCS", "PXP", "SSS"], {"S": S_, "C": "minecraft:copper_ingot", "P": P_, "X": "minecraft:piston"}, "topping_skid")
shaped("atmospheric_column", ["BPB", "B B", "BPB"], {"B": "minecraft:iron_block", "P": P_}, "atmospheric_column")
shaped("crude_heater", ["BBB", "BFB", "BPB"], {"B": "minecraft:iron_block", "F": F_, "P": P_}, "crude_heater")
shaped("desalter", ["SCS", "PRP", "SSS"], {"S": S_, "C": "minecraft:copper_block", "P": P_, "R": "minecraft:redstone_block"}, "desalter")
shaped("crude_charge_pump", ["SCS", "PXP", "SBS"], {"S": S_, "C": "minecraft:copper_block", "P": P_, "X": f"{ns}:crankshaft", "B": "minecraft:iron_block"}, "crude_charge_pump")
shaped("side_strippers", ["SPS", "S S", "SPS"], {"S": S_, "P": P_}, "side_strippers", 1)
shaped("overhead_condenser", ["SCS", "GXG", "SPS"], {"S": S_, "C": "minecraft:copper_block", "G": "minecraft:iron_bars", "X": "minecraft:piston", "P": P_}, "overhead_condenser")
shaped("cracking_furnace", ["BBB", "BFB", "PFP"], {"B": "minecraft:iron_block", "F": F_, "P": P_}, "cracking_furnace")
shaped("quench_tower", ["SPS", "SWS", "SPS"], {"S": S_, "P": P_, "W": "minecraft:water_bucket"}, "quench_tower")
shaped("cracked_gas_compressor", ["SPS", "CXC", "SBS"], {"S": S_, "P": P_, "C": "minecraft:copper_block", "X": f"{ns}:crankshaft", "B": "minecraft:iron_block"}, "cracked_gas_compressor")
shaped("cold_box", ["SIS", "IWI", "SPS"], {"S": S_, "I": "minecraft:blue_ice", "W": "minecraft:white_wool", "P": P_}, "cold_box")
shaped("polyethylene_reactor", ["SPS", "S S", "SXS"], {"S": S_, "P": P_, "X": "minecraft:piston"}, "polyethylene_reactor")
shaped("pelletizer", ["SCS", "XHX", "SSS"], {"S": S_, "C": "minecraft:copper_block", "X": f"{ns}:crankshaft", "H": "minecraft:hopper"}, "pelletizer")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for old in ("block.wildcatter.refinery", "block.wildcatter.refinery_column"):
	lang.pop(old, None)
lang.update({"block.wildcatter.topping_column": "Topping Column (1,000 bbl/day, controller)", "block.wildcatter.topping_heater": "Topping Heater (3 MMBtu/h)",
	"block.wildcatter.topping_skid": "Topping Pump and Air-Cooler Skid (480 V)", "block.wildcatter.atmospheric_column": "Atmospheric Column (20,000 bbl/day, controller)",
	"block.wildcatter.crude_heater": "Crude Heater (60 MMBtu/h)", "block.wildcatter.desalter": "Electrostatic Desalter",
	"block.wildcatter.crude_charge_pump": "Crude Charge Pump (250 hp, 4.16 kV)", "block.wildcatter.side_strippers": "Side Strippers (kerosene and diesel)",
	"block.wildcatter.overhead_condenser": "Overhead Fin-Fan Condenser", "block.wildcatter.cracking_furnace": "Cracking Furnace",
	"block.wildcatter.quench_tower": "Quench Tower", "block.wildcatter.cracked_gas_compressor": "Cracked Gas Compressor (300 hp, 4.16 kV)",
	"block.wildcatter.cold_box": "Cold Box (ethylene recovery)", "block.wildcatter.polyethylene_reactor": "Polyethylene Reactor",
	"block.wildcatter.pelletizer": "Pelletizer (250 hp, 4.16 kV)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)
ROTOR_PARTS += ("skid_fan", "fin_fan")
save_json(f"{ASSETS}/blockstates/rotor.json", {"variants": {f"part={p}": {"model": f"{ns}:block/rotor_{p}"} for p in ROTOR_PARTS}})

# ---- Phase 6a: the gas turbine and combined cycle (docs/multiblocks/gas-turbine.md, combined-cycle.md) -------------------------
# blockstates, models, shapes and items come from tools/models/gas_turbine.py and combined_cycle.py; here loot, names, tags, recipes.
TURBINE_PARTS = ("gas_turbine", "gt_inlet_house", "gt_generator", "gen_breaker", "gt_exhaust_stack", "hrsg", "steam_turbine_5", "st_generator",
	"air_cooled_condenser", "hrsg_feed_pump", "load_bank", "gsu_25")
for name in TURBINE_PARTS:
	big_part_loot(name)
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in TURBINE_PARTS]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
B_, CB_ = "minecraft:iron_block", "minecraft:copper_block"
shaped("gas_turbine", ["BCB", "XTX", "BPB"], {"B": B_, "C": CB_, "X": f"{ns}:crankshaft", "T": f"{ns}:turbine_casing", "P": f"{ns}:gas_line"}, "gas_turbine")
shaped("gt_inlet_house", ["SWS", "SWS", "S S"], {"S": S_, "W": "minecraft:white_wool"}, "gt_inlet_house")
shaped("gt_generator", ["BCB", "CXC", "BCB"], {"B": B_, "C": CB_, "X": f"{ns}:crankshaft"}, "gt_generator")
shaped("gen_breaker", ["SCS", "SRS", "SCS"], {"S": S_, "C": CB_, "R": "minecraft:redstone_block"}, "gen_breaker")
shaped("gt_exhaust_stack", ["S S", "S S", "SBS"], {"S": S_, "B": B_}, "gt_exhaust_stack")
shaped("hrsg", ["BPB", "BWB", "BFB"], {"B": B_, "P": f"{ns}:pipe", "W": "minecraft:water_bucket", "F": "minecraft:blast_furnace"}, "hrsg")
shaped("steam_turbine_5", ["SCS", "XTX", "SSS"], {"S": S_, "C": CB_, "X": f"{ns}:crankshaft", "T": f"{ns}:turbine_casing"}, "steam_turbine_5")
shaped("st_generator", ["SCS", "CXC", "SCS"], {"S": S_, "C": CB_, "X": f"{ns}:crankshaft"}, "st_generator")
shaped("air_cooled_condenser", ["SGS", "GXG", "S S"], {"S": S_, "G": "minecraft:iron_bars", "X": "minecraft:piston"}, "air_cooled_condenser")
shaped("hrsg_feed_pump", ["SCS", "PXP", "SSS"], {"S": S_, "C": "minecraft:copper_ingot", "P": f"{ns}:pipe", "X": "minecraft:piston"}, "hrsg_feed_pump")
shaped("load_bank", ["SRS", "RCR", "SRS"], {"S": S_, "R": "minecraft:redstone", "C": CB_}, "load_bank")
shaped("gsu_25", ["SDS", "TCT", "SDS"], {"S": S_, "D": "minecraft:diamond", "T": f"{ns}:gsu_transformer", "C": CB_}, "gsu_25")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.gas_turbine": "Gas Turbine (16.5 MW, controller)", "block.wildcatter.gt_inlet_house": "Gas Turbine Inlet House",
	"block.wildcatter.gt_generator": "Gas Turbine Generator (20.6 MVA, 13.8 kV)", "block.wildcatter.gen_breaker": "Generator Circuit Breaker (15 kV, 1,200 A)",
	"block.wildcatter.gt_exhaust_stack": "Gas Turbine Exhaust Stack", "block.wildcatter.hrsg": "Heat Recovery Steam Generator (HRSG)",
	"block.wildcatter.steam_turbine_5": "Steam Turbine (5 MW)", "block.wildcatter.st_generator": "Steam Turbine Generator (6.25 MVA, 13.8 kV)",
	"block.wildcatter.air_cooled_condenser": "Air-Cooled Condenser", "block.wildcatter.hrsg_feed_pump": "HRSG Feed Pump (60 hp)",
	"block.wildcatter.load_bank": "Load Bank (13.8 kV, 10 MW)", "block.wildcatter.gsu_25": "Generator Step-Up Transformer (25 MVA, 13.8/138 kV)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 6b: the aeroderivative peaker and the 60 MVA transformers (docs/multiblocks/aeroderivative.md) -----------------------
# blockstates, models, shapes and items come from tools/models/aeroderivative.py; here loot, names, tags, recipes.
AERO_PARTS = ("aero_turbine", "aero_inlet_house", "aero_generator", "aero_exhaust_stack", "gen_breaker_3000", "gsu_60",
	"substation_transformer_60")
for name in AERO_PARTS:
	big_part_loot(name)
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in AERO_PARTS]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
shaped("aero_turbine", ["BTB", "XGX", "BPB"], {"B": B_, "T": f"{ns}:turbine_casing", "X": f"{ns}:crankshaft", "G": f"{ns}:gas_turbine", "P": f"{ns}:gas_line"}, "aero_turbine")
shaped("aero_inlet_house", ["SWS", "WHW", "S S"], {"S": S_, "W": "minecraft:white_wool", "H": f"{ns}:gt_inlet_house"}, "aero_inlet_house")
shaped("aero_generator", ["BCB", "CGC", "BCB"], {"B": B_, "C": CB_, "G": f"{ns}:gt_generator"}, "aero_generator")
shaped("aero_exhaust_stack", ["S S", "SES", "SBS"], {"S": S_, "E": f"{ns}:gt_exhaust_stack", "B": B_}, "aero_exhaust_stack")
shaped("gen_breaker_3000", ["SCS", "CGC", "SCS"], {"S": S_, "C": CB_, "G": f"{ns}:gen_breaker"}, "gen_breaker_3000")
shaped("gsu_60", ["SDS", "TCT", "SDS"], {"S": S_, "D": "minecraft:diamond", "T": f"{ns}:gsu_25", "C": CB_}, "gsu_60")
shaped("substation_transformer_60", ["SDS", "TCT", "SDS"], {"S": S_, "D": "minecraft:diamond", "T": f"{ns}:substation_transformer_10", "C": CB_},
	"substation_transformer_60")
# the 3,000 A bus duct: three of the 1,200 A duct (any size) and a copper block
shapeless("bus_duct_gen__bus_duct_3000", [f"{ns}:bus_duct_gen"] * 3 + ["minecraft:copper_block"], "bus_duct_gen")
recipe = json.load(open(f"{DATA}/recipe/bus_duct_gen__bus_duct_3000.json"))
recipe["result"]["components"] = {SIZE_KEY: "bus_duct_3000"}
save_json(f"{DATA}/recipe/bus_duct_gen__bus_duct_3000.json", recipe)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.aero_turbine": "Aeroderivative Gas Turbine (45 MW, controller)",
	"block.wildcatter.aero_inlet_house": "Aeroderivative Inlet House", "block.wildcatter.aero_generator": "Aeroderivative Generator (56.25 MVA, 13.8 kV)",
	"block.wildcatter.aero_exhaust_stack": "Aeroderivative Exhaust Stack", "block.wildcatter.gen_breaker_3000": "Generator Circuit Breaker (15 kV, 3,000 A)",
	"block.wildcatter.gsu_60": "Generator Step-Up Transformer (60 MVA, 13.8/138 kV)",
	"block.wildcatter.substation_transformer_60": "Substation Transformer (60 MVA, 138/12.47 kV)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 7a: the ores (docs/ores.md) ---------------------------------------------------------------------------------
# The ore list (ids, names, tool) is read from Ore.java so the two never disagree; the veins (size, count, height, host rock)
# are here. Textures and icons are the Ludo pictures processed into art/final/ore by tools/process_art.py.
import re
_ore_src = open("common/src/main/java/dev/wildcatter/ore/Ore.java").read()
ORES = [(m.group(1), m.group(2), m.group(3), m.group(4)) for m in
	re.finditer(r'^\t[A-Z_]+\("(\w+)", "([^"]+)", "([^"]+)",.*?Tool\.(\w+)\)[,;]', _ore_src, re.M | re.S)]
assert len(ORES) == 28, len(ORES)
ORE_TOOL_TAG = {"SHOVEL": None, "STONE": "needs_stone_tool", "IRON": "needs_iron_tool"}
ORE_ART = "art/final/ore"
os.makedirs(f"{ASSETS}/textures/block/ore", exist_ok=True)
os.makedirs(f"{ASSETS}/textures/item/ore", exist_ok=True)
_ore_tags = {"mineable/pickaxe": [], "mineable/shovel": [], "needs_stone_tool": [], "needs_iron_tool": []}
for key, block_name, mineral_name, tool in ORES:
	block = f"{key}_ore"
	if os.path.exists(f"{ORE_ART}/{block}.png"):
		shutil.copy(f"{ORE_ART}/{block}.png", f"{ASSETS}/textures/block/ore/{block}.png")
		block_tex = f"{ns}:block/ore/{block}"
	else:
		tex(f"{block}_placeholder", lambda img, d: noise(img, (128, 124, 120), 18))
		block_tex = f"{ns}:block/{block}_placeholder"
	if os.path.exists(f"{ORE_ART}/{key}.png"):
		shutil.copy(f"{ORE_ART}/{key}.png", f"{ASSETS}/textures/item/ore/{key}.png")
		item_tex = f"{ns}:item/ore/{key}"
	else:
		tex(f"{key}_placeholder", lambda img, d: noise(img, (150, 140, 130), 30), "item")
		item_tex = f"{ns}:item/{key}_placeholder"
	block_model(block, "minecraft:block/cube_all", {"all": block_tex})
	save_json(f"{ASSETS}/blockstates/{block}.json", {"variants": {"": {"model": f"{ns}:block/{block}"}}})
	save_json(f"{ASSETS}/items/{block}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{block}"}})
	save_json(f"{ASSETS}/models/item/{key}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{key}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{key}"}})
	# silk touch gives the ore block; otherwise its mineral, more with Fortune, as vanilla ores do
	save_json(f"{DATA}/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
		{"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"{ns}:{block}"},
		{"type": "minecraft:item", "name": f"{ns}:{key}", "modifier": [{"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
			{"type": "minecraft:explosion_decay"}]}]}]}], "random_sequence": f"{ns}:blocks/{block}"})
	_ore_tags["mineable/shovel" if tool == "SHOVEL" else "mineable/pickaxe"].append(f"{ns}:{block}")
	if ORE_TOOL_TAG[tool]:
		_ore_tags[ORE_TOOL_TAG[tool]].append(f"{ns}:{block}")
	lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
	lang.update({f"block.{ns}.{block}": block_name, f"item.{ns}.{key}": mineral_name})
	save_json(f"{ASSETS}/lang/en_us.json", lang)
for tag, values in _ore_tags.items():
	path = f"{ROOT}/data/minecraft/tags/block/{tag}.json"
	# pickaxe and stone-tool tags were written fresh above; the shovel and iron-tool tags are only ours, so start them clean
	t = json.load(open(path)) if tag in ("mineable/pickaxe", "needs_stone_tool") else {"replace": False, "values": []}
	t["values"] += values
	save_json(path, t)
# the furnace smelts only what really smelts simply: galena, cassiterite, sphalerite and acanthite
INGOTS = {"lead": "galena", "tin": "cassiterite", "zinc": "sphalerite", "silver": "acanthite"}
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for metal, mineral in INGOTS.items():
	if os.path.exists(f"{ORE_ART}/{metal}_ingot.png"):
		shutil.copy(f"{ORE_ART}/{metal}_ingot.png", f"{ASSETS}/textures/item/ore/{metal}_ingot.png")
		item_tex = f"{ns}:item/ore/{metal}_ingot"
	else:
		tex(f"{metal}_ingot_placeholder", lambda img, d: noise(img, (170, 172, 180), 10), "item")
		item_tex = f"{ns}:item/{metal}_ingot_placeholder"
	save_json(f"{ASSETS}/models/item/{metal}_ingot.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{metal}_ingot.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{metal}_ingot"}})
	for kind, time in (("smelting", 200), ("blasting", 100)):
		save_json(f"{DATA}/recipe/{metal}_ingot_from_{kind}_{mineral}.json", {"type": f"minecraft:{kind}", "cookingtime": time, "experience": 0.7,
			"group": f"{metal}_ingot", "ingredient": f"{ns}:{mineral}", "result": {"id": f"{ns}:{metal}_ingot"}})
	lang[f"item.{ns}.{metal}_ingot"] = f"{metal.capitalize()} Ingot"
# the scintillometer
ITEM_ICON = f"{ns}:item/ore/scintillometer" if os.path.exists(f"{ORE_ART}/scintillometer.png") else f"{ns}:item/scintillometer_placeholder"
if os.path.exists(f"{ORE_ART}/scintillometer.png"):
	shutil.copy(f"{ORE_ART}/scintillometer.png", f"{ASSETS}/textures/item/ore/scintillometer.png")
else:
	tex("scintillometer_placeholder", lambda img, d: noise(img, (220, 190, 40), 10), "item")
save_json(f"{ASSETS}/models/item/scintillometer.json", {"parent": "minecraft:item/generated", "textures": {"layer0": ITEM_ICON}})
save_json(f"{ASSETS}/items/scintillometer.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/scintillometer"}})
lang[f"item.{ns}.scintillometer"] = "Scintillometer"
save_json(f"{ASSETS}/lang/en_us.json", lang)
shaped("scintillometer", ["IGI", "RCR", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "R": "minecraft:redstone", "C": "minecraft:amethyst_shard"}, "scintillometer")

# worldgen: (vein size, veins per chunk, lowest y, highest y, host rule); a count below 1 is one vein in every 1/count chunks
STONE = {"predicate_type": "minecraft:tag_match", "tag": "minecraft:base_stone_overworld"}
def any_of(*rules):
	return {"predicate_type": "minecraft:any_of", "rules": list(rules)}
def tag(t):
	return {"predicate_type": "minecraft:tag_match", "tag": t}
def block_is(b):
	return {"predicate_type": "minecraft:block_match", "block": b}
VEINS = {
	"uraninite": (8, 6, 0, 64, any_of(STONE, block_is("minecraft:sandstone"))),
	"pitchblende": (4, 0.5, -64, -40, STONE),
	"fluorite": (8, 4, -16, 48, STONE),
	"heavy_mineral": (20, 4, 55, 70, any_of(tag("minecraft:sand"), block_is("minecraft:sandstone"))),
	"bauxite": (20, 4, 55, 80, any_of(STONE, tag("minecraft:dirt"))),
	"galena": (9, 4, -16, 64, STONE),
	"sphalerite": (9, 5, -16, 64, STONE),
	"cassiterite": (7, 6, 0, 80, STONE),
	"pentlandite": (9, 3, -64, -16, STONE),
	"molybdenite": (16, 2, 0, 80, STONE),
	"magnesite": (12, 2, 0, 64, STONE),
	"dolomite": (48, 1, 0, 100, STONE),
	"graphite": (8, 4, -32, 32, STONE),
	"borax": (16, 3, 50, 80, any_of(STONE, tag("minecraft:sand"), block_is("minecraft:sandstone"), tag("minecraft:terracotta"))),
	"spodumene": (10, 3, 0, 64, STONE),
	"barite": (10, 3, 0, 48, STONE),
	"bentonite": (16, 2, 40, 70, any_of(STONE, tag("minecraft:dirt"), tag("minecraft:terracotta"))),
	"acanthite": (6, 4, -16, 48, STONE),
	"scheelite": (7, 3, -16, 48, STONE),
	"cobaltite": (6, 3, -32, 32, STONE),
	"pyrolusite": (12, 2, 0, 48, STONE),
	"phosphate": (14, 3, 30, 60, any_of(STONE, tag("minecraft:sand"), block_is("minecraft:gravel"))),
	"limestone": (48, 1, 40, 90, any_of(STONE, tag("minecraft:dirt"))),   # 10b2: thick beds (docs/ores.md)
}
WG = f"{DATA}/worldgen"
def placed(name, count, lo, hi, square=True):
	place = [{"type": "minecraft:count", "count": int(count)} if count >= 1 else {"type": "minecraft:rarity_filter", "chance": round(1 / count)}]
	if square:
		place.append({"type": "minecraft:in_square"})
	place.append({"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}})
	place.append({"type": "minecraft:biome"})
	save_json(f"{WG}/placed_feature/{name}.json", {"feature": f"{ns}:{name}", "placement": place})
for key, (size, count, lo, hi, rule) in VEINS.items():
	save_json(f"{WG}/feature/ore_{key}.json", {"type": "minecraft:ore", "discard_chance_on_air_exposure": 0.0, "size": size,
		"targets": [{"state": f"{ns}:{key}_ore", "target": rule}]})
	placed(f"ore_{key}", count, lo, hi)
# salt domes: a rock-salt column 8 to 14 blocks across from y -60 to a crest at y 0-20, under a 4-block calcite cap with 30 % sulfur.
# 15a (docs/salt-dome-oil.md): where they stand is a seed grid (SaltDome.java, its sizes in Tuning), so the feature runs once in every
# overworld chunk and draws its cell's dome where it falls in that chunk; the dome checks its own centre's biome.
# With the world rule saltDomeGrid192 off (worlds from before 15a) the domes are placed exactly as before: SaltDomePlacement runs the old
# rarity 1/150, in_square, y 0 and biome check itself on the same random numbers, and the feature keeps its old sizes for that case.
save_json(f"{WG}/feature/salt_dome.json", {"type": f"{ns}:salt_dome", "radius_min": 4, "radius_max": 7, "bottom": -60, "top_min": 0, "top_max": 20,
	"caprock": 4, "sulfur_chance": 0.3})
save_json(f"{WG}/placed_feature/salt_dome.json", {"feature": f"{ns}:salt_dome", "placement": [{"type": f"{ns}:salt_dome_placement"}]})
AIR = "minecraft:air"
def layer(state, thickness):
	return {"state": state, "thickness": thickness}
# bedded evaporites under the desert: rock salt, a potash bed, rock salt
save_json(f"{WG}/feature/evaporite_beds.json", {"type": f"{ns}:layered", "radius_min": 8, "radius_max": 14,
	"layers": [layer(f"{ns}:halite_ore", 3), layer(f"{ns}:sylvite_ore", 1), layer(f"{ns}:halite_ore", 2)]})
placed("evaporite_beds", 1 / 6, -32, 0)
# the Bushveld: two chromitite seams, then a gap of country rock, then the platinum reef
save_json(f"{WG}/feature/bushveld_reef.json", {"type": f"{ns}:layered", "radius_min": 8, "radius_max": 14,
	"layers": [layer(f"{ns}:chromite_ore", 2), layer(AIR, 1), layer(f"{ns}:chromite_ore", 1), layer(AIR, 2), layer(f"{ns}:platinum_ore", 1)]})
placed("bushveld_reef", 1 / 8, -64, -38)

# ---- Phase 7b: the sulfuric acid plant and the uranium mill (docs/multiblocks/acid-plant.md, uranium-mill.md) ---------------
# blockstates, models, shapes and items come from tools/models/acid_plant.py and uranium_mill.py; here loot, names, tags, recipes.
FUEL_PARTS = ("sulfur_melter", "drying_tower", "main_blower", "sulfur_furnace", "waste_heat_boiler", "converter", "interpass_absorber", "final_absorber",
	"jaw_crusher", "sag_mill", "leach_tank", "ccd_thickener", "sx_circuit", "precipitation_tank", "yellowcake_dryer", "drum_filler")
for name in FUEL_PARTS:
	big_part_loot(name)
# a placed assembly keeps its assay and its handling record (Tim, answer 21): the block entity holds the item's components; the drop copies them back
_fa = json.load(open(f"{DATA}/loot_table/blocks/fuel_assembly.json"))
_fa["pools"][0]["entries"][0]["modifier"] = {"type": "minecraft:copy_components", "source": "block_entity", "include": [f"{ns}:assay", "minecraft:custom_data"]}
save_json(f"{DATA}/loot_table/blocks/fuel_assembly.json", _fa)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	t["values"] = [v for v in t["values"] if v not in [f"{ns}:{n}" for n in FUEL_PARTS]] + [f"{ns}:{n}" for n in FUEL_PARTS]
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
S_, P_, B_ = f"{ns}:steel_ingot", f"{ns}:gas_line", "minecraft:iron_block"
shaped("sulfur_melter", ["CPC", "CFC", "CCC"], {"C": "minecraft:smooth_stone", "P": P_, "F": "minecraft:furnace"}, "sulfur_melter")
shaped("drying_tower", ["SPS", "SBS", "SPS"], {"S": S_, "P": P_, "B": "minecraft:bricks"}, "drying_tower")
shaped("main_blower", ["SCS", "XFX", "SSS"], {"S": S_, "C": "minecraft:copper_block", "X": f"{ns}:crankshaft", "F": "minecraft:iron_bars"}, "main_blower")
shaped("sulfur_furnace", ["BBB", "PFB", "BBB"], {"B": "minecraft:bricks", "P": P_, "F": "minecraft:blast_furnace"}, "sulfur_furnace")
shaped("waste_heat_boiler", ["SSS", "PWP", "SSS"], {"S": S_, "P": P_, "W": "minecraft:water_bucket"}, "waste_heat_boiler")
shaped("converter", ["BPB", "BVB", "BPB"], {"B": B_, "P": P_, "V": "minecraft:lapis_block"}, "converter")
shaped("interpass_absorber", ["SPS", "SBS", "SCS"], {"S": S_, "P": P_, "B": "minecraft:bricks", "C": "minecraft:copper_ingot"}, "interpass_absorber")
shaped("final_absorber", ["SPS", "SBS", "BPB"], {"S": S_, "P": P_, "B": "minecraft:bricks"}, "final_absorber")
shaped("jaw_crusher", ["B B", "BXB", "SCS"], {"B": B_, "X": f"{ns}:crankshaft", "S": S_, "C": "minecraft:copper_block"}, "jaw_crusher")
shaped("sag_mill", ["BBB", "XCX", "BBB"], {"B": B_, "X": f"{ns}:crankshaft", "C": "minecraft:copper_block"}, "sag_mill")
shaped("leach_tank", ["SXS", "S S", "SPS"], {"S": S_, "X": "minecraft:piston", "P": P_}, "leach_tank")
shaped("ccd_thickener", ["SXS", "SSS", " P "], {"S": S_, "X": "minecraft:piston", "P": P_}, "ccd_thickener")
shaped("sx_circuit", ["SXS", "PGP", "SSS"], {"S": S_, "X": "minecraft:piston", "P": P_, "G": "minecraft:glass"}, "sx_circuit")
shaped("precipitation_tank", ["SXS", "SCS", "SPS"], {"S": S_, "X": "minecraft:piston", "C": "minecraft:cauldron", "P": P_}, "precipitation_tank")
shaped("yellowcake_dryer", ["SFS", "S S", "SHS"], {"S": S_, "F": "minecraft:blast_furnace", "H": "minecraft:hopper"}, "yellowcake_dryer")
shaped("drum_filler", ["SHS", "S S", "SXS"], {"S": S_, "H": "minecraft:hopper", "X": "minecraft:piston"}, "drum_filler")
shaped("empty_drum", ["S S", "S S", "SSS"], {"S": "minecraft:iron_ingot"}, "empty_drum", 2)
for kind, time in (("smelting", 200), ("blasting", 100)):
	save_json(f"{DATA}/recipe/magnesia_from_{kind}_magnesite.json", {"type": f"minecraft:{kind}", "cookingtime": time, "experience": 0.3,
		"group": "magnesia", "ingredient": f"{ns}:magnesite", "result": {"id": f"{ns}:magnesia"}})
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"magnesia": ("Magnesia", (235, 235, 228)), "empty_drum": ("Empty Drum (208 L)", (60, 90, 140)),
		"yellowcake_drum": ("Yellowcake Drum", (220, 190, 40))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.sulfur_melter": "Sulfur Melter", f"block.{ns}.drying_tower": "Drying Tower",
	f"block.{ns}.main_blower": "Main Blower (250 kW, 4.16 kV)", f"block.{ns}.sulfur_furnace": "Sulfur Furnace (acid plant front)",
	f"block.{ns}.waste_heat_boiler": "Waste Heat Boiler", f"block.{ns}.converter": "Converter (four V2O5 beds)",
	f"block.{ns}.interpass_absorber": "Interpass Absorber", f"block.{ns}.final_absorber": "Final Absorber and Stack",
	f"block.{ns}.jaw_crusher": "Jaw Crusher (75 kW, 480 V)", f"block.{ns}.sag_mill": "SAG Mill (522 kW, 4.16 kV)",
	f"block.{ns}.leach_tank": "Leach Tank (56 kW agitator)", f"block.{ns}.ccd_thickener": "CCD Thickener",
	f"block.{ns}.sx_circuit": "SX Circuit (mixer-settlers)", f"block.{ns}.precipitation_tank": "Precipitation Tank",
	f"block.{ns}.yellowcake_dryer": "Yellowcake Dryer", f"block.{ns}.drum_filler": "Drum Filler"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 7c: the ISR wellfield (docs/multiblocks/isr-wellfield.md) -----------------------------------------------------
# blockstates, models, shapes and items come from tools/models/wellfield.py; here loot, names, tags, recipes and the pump's icon.
WELLFIELD_PARTS = ("header_house", "oxygen_generator", "ix_columns")
for name in WELLFIELD_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in WELLFIELD_PARTS + ("isr_wellhead",)]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
# a well drops its cap, and its pump if it has one
save_json(f"{DATA}/loot_table/blocks/isr_wellhead.json", {"type": "minecraft:block", "pools": [
	{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:isr_wellhead"}]},
	{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:submersible_pump"}],
		"conditions": [{"condition": "minecraft:block_state_property", "block": f"{ns}:isr_wellhead", "properties": {"pump": "true"}}]}],
	"random_sequence": f"{ns}:blocks/isr_wellhead"})
shaped("header_house", ["SCS", "PXP", "SBS"], {"S": S_, "C": "minecraft:copper_block", "P": P_, "X": "minecraft:comparator", "B": "minecraft:iron_block"}, "header_house")
shaped("oxygen_generator", ["SPS", "XZX", "SSS"], {"S": S_, "P": P_, "X": "minecraft:piston", "Z": "minecraft:clay_ball"}, "oxygen_generator")
shaped("ix_columns", ["SPS", "SRS", "SPS"], {"S": S_, "P": P_, "R": "minecraft:slime_ball"}, "ix_columns")
shaped("submersible_pump", [" C ", "SXS", " S "], {"C": "minecraft:copper_ingot", "S": S_, "X": f"{ns}:crankshaft"}, "submersible_pump")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
if os.path.exists(f"{ORE_ART}/submersible_pump.png"):
	shutil.copy(f"{ORE_ART}/submersible_pump.png", f"{ASSETS}/textures/item/ore/submersible_pump.png")
	pump_tex = f"{ns}:item/ore/submersible_pump"
else:
	tex("submersible_pump_placeholder", lambda img, d: noise(img, (190, 195, 200), 10), "item")
	pump_tex = f"{ns}:item/submersible_pump_placeholder"
save_json(f"{ASSETS}/models/item/submersible_pump.json", {"parent": "minecraft:item/generated", "textures": {"layer0": pump_tex}})
save_json(f"{ASSETS}/items/submersible_pump.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/submersible_pump"}})
lang.update({f"item.{ns}.submersible_pump": "Submersible Pump (7.5 hp, 480 V)", f"block.{ns}.isr_wellhead": "ISR Well",
	f"block.{ns}.header_house": "Header House (25 producers, 480 V)", f"block.{ns}.oxygen_generator": "Oxygen Generator (PSA, 50 Nm3/h)",
	f"block.{ns}.ix_columns": "IX Columns (lead and lag)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 7d: conversion to UF6 (docs/multiblocks/conversion.md) --------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/conversion.py; here loot, names, tags, recipes and the cylinders.
CONVERSION_PARTS = ("reduction_reactor", "hydrofluorinator", "fluorinator", "cold_traps", "distillation_columns", "cylinder_filler",
	"steam_reformer", "hf_kiln", "hf_column", "rectifier", "fluorine_cell_house")
for name in CONVERSION_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in CONVERSION_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("reduction_reactor", ["SHS", "SBS", "SPS"], {"S": S_, "H": "minecraft:hopper", "B": "minecraft:blast_furnace", "P": P_}, "reduction_reactor")
shaped("hydrofluorinator", ["SPS", "BSB", "SPS"], {"S": S_, "P": P_, "B": "minecraft:blast_furnace"}, "hydrofluorinator")
shaped("fluorinator", ["SPS", "SBS", "SIS"], {"S": S_, "P": P_, "B": "minecraft:blast_furnace", "I": "minecraft:iron_bars"}, "fluorinator")
shaped("cold_traps", ["SPS", "IXI", "SSS"], {"S": S_, "P": P_, "I": "minecraft:packed_ice", "X": "minecraft:piston"}, "cold_traps")
shaped("distillation_columns", ["SPS", "S S", "SFS"], {"S": S_, "P": P_, "F": "minecraft:furnace"}, "distillation_columns")
shaped("cylinder_filler", ["SHS", "SXS", "BBB"], {"S": S_, "H": "minecraft:hopper", "X": "minecraft:piston", "B": "minecraft:iron_block"}, "cylinder_filler")
shaped("steam_reformer", ["SBS", "PFP", "SSS"], {"S": S_, "B": "minecraft:bricks", "P": P_, "F": "minecraft:blast_furnace"}, "steam_reformer")
shaped("hf_kiln", ["BBB", "PFP", "SXS"], {"B": "minecraft:bricks", "P": P_, "F": "minecraft:blast_furnace", "S": S_, "X": f"{ns}:crankshaft"}, "hf_kiln")
shaped("hf_column", ["SPS", "S S", "SPS"], {"S": "minecraft:iron_ingot", "P": P_}, "hf_column")
shaped("rectifier", ["CRC", "CSC", "CRC"], {"C": "minecraft:copper_block", "R": "minecraft:redstone_block", "S": S_}, "rectifier")
shaped("fluorine_cell_house", ["SCS", "GXG", "SPS"], {"S": S_, "C": "minecraft:copper_block", "G": "minecraft:coal_block", "X": "minecraft:cauldron", "P": P_}, "fluorine_cell_house")
shaped("empty_48y_cylinder", ["SSS", "S S", "SSS"], {"S": S_}, "empty_48y_cylinder")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"empty_48y_cylinder": ("Empty 48Y Cylinder", (200, 200, 195)), "uf6_cylinder_48y": ("48Y Cylinder of UF6 (natural)", (210, 200, 120))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.reduction_reactor": "Reduction Reactor (fluid bed)", f"block.{ns}.hydrofluorinator": "Hydrofluorinator (two fluid beds)",
	f"block.{ns}.fluorinator": "Fluorinator (fluid bed)", f"block.{ns}.cold_traps": "Cold Traps (30 kW, 480 V)",
	f"block.{ns}.distillation_columns": "UF6 Distillation Columns", f"block.{ns}.cylinder_filler": "48Y Cylinder Filler",
	f"block.{ns}.steam_reformer": "Steam Reformer (30 Nm3/h hydrogen)", f"block.{ns}.hf_kiln": "HF Kiln (rotary)",
	f"block.{ns}.hf_column": "HF Column", f"block.{ns}.rectifier": "Rectifier (12-pulse, 4.16 kV)",
	f"block.{ns}.fluorine_cell_house": "Fluorine Cell House (12 cells)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 7e: the centrifuge cascade (docs/multiblocks/enrichment.md) ------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/enrichment.py; here loot, names, tags, recipes and the cylinders.
ENRICHMENT_PARTS = ("centrifuge", "frequency_converter", "feed_station", "product_takeoff", "tails_takeoff")
for name in ENRICHMENT_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in ENRICHMENT_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("centrifuge", ["SPS", "SIS", "SRS"], {"S": S_, "P": P_, "I": "minecraft:iron_block", "R": "minecraft:redstone"}, "centrifuge")
shaped("frequency_converter", ["CRC", "SXS", "CRC"], {"C": "minecraft:copper_block", "R": "minecraft:redstone_block", "S": S_, "X": "minecraft:comparator"}, "frequency_converter")
shaped("feed_station", ["SHS", "PFP", "SSS"], {"S": S_, "H": "minecraft:hopper", "P": P_, "F": "minecraft:furnace"}, "feed_station")
shaped("product_takeoff", ["SHS", "PIP", "BBB"], {"S": S_, "H": "minecraft:hopper", "P": P_, "I": "minecraft:packed_ice", "B": "minecraft:iron_block"}, "product_takeoff")
shaped("tails_takeoff", ["SHS", "PIP", "SBS"], {"S": S_, "H": "minecraft:hopper", "P": P_, "I": "minecraft:packed_ice", "B": "minecraft:iron_block"}, "tails_takeoff")
shaped("empty_30b_cylinder", ["S S", "S S", "SSS"], {"S": S_}, "empty_30b_cylinder")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"empty_30b_cylinder": ("Empty 30B Cylinder", (200, 200, 195)), "leu_cylinder_30b": ("30B Cylinder of UF6 (4.95 % LEU)", (220, 190, 90)),
		"tails_cylinder_48y": ("48Y Cylinder of UF6 (0.25 % tails)", (150, 150, 140))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.centrifuge": "Gas Centrifuge (TC-21 class)", f"block.{ns}.frequency_converter": "Frequency Converter (12.47 kV, 1,225 Hz)",
	f"block.{ns}.feed_station": "Feed Station (48Y)", f"block.{ns}.product_takeoff": "Product Take-off (30B)",
	f"block.{ns}.tails_takeoff": "Tails Take-off (48Y)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 7f: the zirconium plant (docs/multiblocks/zirconium.md) ----------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/zirconium.py; here loot, names, tags, recipes and the tubes.
ZIRCONIUM_PARTS = ("mineral_separator", "chlorinator", "hafnium_column", "kroll_reactor", "vacuum_distillation", "var_furnace", "extrusion_press",
	"pilger_mill", "vacuum_anneal_furnace", "magnesium_cell", "magnesia_chlorinator", "chlor_alkali_cell")
for name in ZIRCONIUM_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in ZIRCONIUM_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("mineral_separator", ["SHS", "IRI", "SSS"], {"S": S_, "H": "minecraft:hopper", "I": "minecraft:iron_block", "R": "minecraft:redstone"}, "mineral_separator")
shaped("chlorinator", ["SHS", "BFB", "SPS"], {"S": S_, "H": "minecraft:hopper", "B": "minecraft:bricks", "F": "minecraft:blast_furnace", "P": P_}, "chlorinator")
shaped("hafnium_column", ["SPS", "S S", "SFS"], {"S": S_, "P": P_, "F": "minecraft:furnace"}, "hafnium_column")
shaped("kroll_reactor", ["SPS", "SBS", "SSS"], {"S": S_, "P": P_, "B": "minecraft:blast_furnace"}, "kroll_reactor")
shaped("vacuum_distillation", ["SGS", "SBS", "SSS"], {"S": S_, "G": "minecraft:glass", "B": "minecraft:blast_furnace"}, "vacuum_distillation")
shaped("var_furnace", ["SHS", "CBC", "SSS"], {"S": S_, "H": "minecraft:hopper", "C": "minecraft:copper_block", "B": "minecraft:blast_furnace"}, "var_furnace")
shaped("extrusion_press", ["SXS", "IAI", "SXS"], {"S": S_, "X": "minecraft:piston", "I": "minecraft:iron_block", "A": "minecraft:anvil"}, "extrusion_press")
shaped("pilger_mill", ["SXS", "IRI", "SSS"], {"S": S_, "X": "minecraft:piston", "I": "minecraft:iron_block", "R": f"{ns}:crankshaft"}, "pilger_mill")
shaped("vacuum_anneal_furnace", ["SSS", "GFG", "SSS"], {"S": S_, "G": "minecraft:glass", "F": "minecraft:furnace"}, "vacuum_anneal_furnace")
shaped("magnesium_cell", ["CBC", "SXS", "SSS"], {"C": "minecraft:copper_block", "B": "minecraft:bricks", "S": S_, "X": "minecraft:cauldron"}, "magnesium_cell")
shaped("magnesia_chlorinator", ["SHS", "BFB", "SPS"], {"S": S_, "H": "minecraft:hopper", "B": "minecraft:bricks", "F": "minecraft:furnace", "P": P_}, "magnesia_chlorinator")
shaped("chlor_alkali_cell", ["CPC", "SXS", "SHS"], {"C": "minecraft:copper_block", "P": P_, "S": S_, "X": "minecraft:cauldron", "H": "minecraft:hopper"}, "chlor_alkali_cell")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"zircaloy_tube": ("Zircaloy-4 Cladding Tube", (190, 190, 185)), "guide_thimble": ("Zircaloy-4 Guide Thimble", (175, 175, 170))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.mineral_separator": "Mineral Separator (magnetic, electrostatic)", f"block.{ns}.chlorinator": "Chlorinator (fluid bed)",
	f"block.{ns}.hafnium_column": "Hafnium Column (extractive distillation)", f"block.{ns}.kroll_reactor": "Kroll Reactor",
	f"block.{ns}.vacuum_distillation": "Vacuum Distillation Furnace", f"block.{ns}.var_furnace": "VAR Furnace (vacuum arc remelting)",
	f"block.{ns}.extrusion_press": "Extrusion Press", f"block.{ns}.pilger_mill": "Pilger Mill", f"block.{ns}.vacuum_anneal_furnace": "Vacuum Anneal Furnace",
	f"block.{ns}.magnesium_cell": "Magnesium Cell (14 kA)", f"block.{ns}.magnesia_chlorinator": "Magnesia Chlorinator",
	f"block.{ns}.chlor_alkali_cell": "Chlor-alkali Cell (membrane)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10b2: the Bayer refinery, lime kiln, AlF3, bags and chlorine (docs/multiblocks/bayer-refinery.md, alumina-plant.md) ----
# blockstates, models, shapes and items come from tools/models/alumina.py; here loot, names, tags, recipes, the bags and red mud.
ALUMINA_PARTS = ("ball_mill", "digester", "flash_tanks", "settler", "mud_washer", "residue_filter_press", "polishing_filter", "precipitator",
	"hydrate_filter", "calciner", "evaporator", "causticizer", "package_boiler", "lime_kiln", "alf3_reactor", "bagging_station", "chlorine_filler")
for name in ALUMINA_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in ALUMINA_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("ball_mill", ["BBB", "XCX", "SHS"], {"B": B_, "X": f"{ns}:crankshaft", "C": "minecraft:iron_block", "S": S_, "H": "minecraft:hopper"}, "ball_mill")
shaped("digester", ["SXS", "SWS", "SPS"], {"S": S_, "X": "minecraft:piston", "W": "minecraft:white_wool", "P": P_}, "digester")
shaped("flash_tanks", ["SPS", "SWS", "SPS"], {"S": S_, "P": P_, "W": "minecraft:white_wool"}, "flash_tanks")
shaped("settler", ["SXS", "CCC", " P "], {"S": S_, "X": "minecraft:piston", "C": "minecraft:smooth_stone", "P": P_}, "settler")
shaped("mud_washer", ["SXS", "CWC", " P "], {"S": S_, "X": "minecraft:piston", "C": "minecraft:smooth_stone", "W": "minecraft:water_bucket", "P": P_}, "mud_washer")
shaped("residue_filter_press", ["SPS", "XLX", "SSS"], {"S": S_, "P": P_, "X": "minecraft:piston", "L": "minecraft:paper"}, "residue_filter_press")
shaped("polishing_filter", ["SPS", "SLS", "SPS"], {"S": S_, "P": P_, "L": "minecraft:paper"}, "polishing_filter")
shaped("precipitator", ["SXS", "S S", "SPS"], {"S": "minecraft:iron_block", "X": "minecraft:piston", "P": P_}, "precipitator")
shaped("hydrate_filter", ["SLS", "PXP", "SSS"], {"S": S_, "L": "minecraft:paper", "P": P_, "X": "minecraft:piston"}, "hydrate_filter")
shaped("calciner", ["SPS", "BFB", "SXS"], {"S": S_, "P": P_, "B": "minecraft:bricks", "F": "minecraft:blast_furnace", "X": f"{ns}:crankshaft"}, "calciner")
shaped("evaporator", ["SPS", "WCW", "SPS"], {"S": S_, "P": P_, "W": "minecraft:white_wool", "C": "minecraft:copper_block"}, "evaporator")
shaped("causticizer", ["SHS", "SXS", "SSS"], {"S": S_, "H": "minecraft:hopper", "X": "minecraft:piston"}, "causticizer")
shaped("package_boiler", ["SPS", "BWB", "SFS"], {"S": S_, "P": P_, "B": "minecraft:bricks", "W": "minecraft:water_bucket", "F": "minecraft:furnace"}, "package_boiler")
shaped("lime_kiln", ["SHS", "BFB", "BBB"], {"S": S_, "H": "minecraft:hopper", "B": "minecraft:bricks", "F": "minecraft:blast_furnace"}, "lime_kiln")
shaped("alf3_reactor", ["SHS", "BFB", "SPS"], {"S": S_, "H": "minecraft:hopper", "B": "minecraft:bricks", "F": "minecraft:furnace", "P": P_}, "alf3_reactor")
shaped("bagging_station", ["SHS", "S S", "IXI"], {"S": S_, "H": "minecraft:hopper", "I": "minecraft:iron_bars", "X": "minecraft:piston"}, "bagging_station")
shaped("chlorine_filler", ["SPS", "CXC", "SSS"], {"S": S_, "P": P_, "C": "minecraft:copper_ingot", "X": "minecraft:piston"}, "chlorine_filler")
shaped("empty_bulk_bag", ["SWS", "W W", "SWS"], {"S": "minecraft:string", "W": "minecraft:white_wool"}, "empty_bulk_bag", 2)
shaped("empty_ton_container", ["SSS", "V V", "SSS"], {"S": S_, "V": "minecraft:lever"}, "empty_ton_container")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"empty_bulk_bag": ("Empty Bulk Bag (FIBC, 1 t)", (225, 225, 215)), "alumina_bag": ("Bulk Bag of Alumina", (240, 240, 236)),
		"hydrate_bag": ("Bulk Bag of Hydrate (Al(OH)3)", (232, 228, 214)), "alf3_bag": ("Bulk Bag of AlF3", (214, 222, 230)),
		"quicklime_bag": ("Bulk Bag of Quicklime", (238, 236, 226)), "empty_ton_container": ("Empty Chlorine Ton Container", (120, 130, 125)),
		"chlorine_ton_container": ("Chlorine Ton Container (907 kg)", (150, 170, 90))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
# red mud: a plain block of filter cake for a dry stack
if os.path.exists(f"{ORE_ART}/red_mud.png"):
	shutil.copy(f"{ORE_ART}/red_mud.png", f"{ASSETS}/textures/block/ore/red_mud.png")
	mud_tex = f"{ns}:block/ore/red_mud"
else:
	tex("red_mud_placeholder", lambda img, d: noise(img, (150, 62, 40), 16))
	mud_tex = f"{ns}:block/red_mud_placeholder"
block_model("red_mud", "minecraft:block/cube_all", {"all": mud_tex})
save_json(f"{ASSETS}/blockstates/red_mud.json", {"variants": {"": {"model": f"{ns}:block/red_mud"}}})
save_json(f"{ASSETS}/items/red_mud.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/red_mud"}})
save_json(f"{DATA}/loot_table/blocks/red_mud.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:red_mud"}],
	"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
t = json.load(open(f"{ROOT}/data/minecraft/tags/block/mineable/shovel.json"))
t["values"] = [v for v in t["values"] if v != f"{ns}:red_mud"] + [f"{ns}:red_mud"]
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/shovel.json", t)
lang.update({f"block.{ns}.ball_mill": "Ball Mill (110 kW, 480 V)", f"block.{ns}.digester": "Digester (145 C)", f"block.{ns}.flash_tanks": "Flash Tanks",
	f"block.{ns}.settler": "Settler (red mud thickener)", f"block.{ns}.mud_washer": "Mud Washer", f"block.{ns}.residue_filter_press": "Residue Filter Press",
	f"block.{ns}.polishing_filter": "Polishing Filters", f"block.{ns}.precipitator": "Precipitator (2,035 m3)", f"block.{ns}.hydrate_filter": "Hydrate Filter (pan)",
	f"block.{ns}.calciner": "Calciner (gas suspension, 4.16 kV fan)", f"block.{ns}.evaporator": "Evaporator (three-effect)", f"block.{ns}.causticizer": "Causticizer",
	f"block.{ns}.package_boiler": "Package Boiler (10 t/h)", f"block.{ns}.lime_kiln": "Lime Kiln (shaft)", f"block.{ns}.alf3_reactor": "AlF3 Reactor (fluid bed)",
	f"block.{ns}.bagging_station": "Bagging Station (FIBC)", f"block.{ns}.chlorine_filler": "Chlorine Filler (ton containers)", f"block.{ns}.red_mud": "Red Mud (filter cake)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10b3a: coke, pitch and anodes (docs/multiblocks/delayed-coker.md, coke-ovens.md, anode-plant.md) -------------------
# blockstates, models, shapes and items come from tools/models/carbon.py; here loot, names, tags, recipes and the items.
CARBON_PARTS = ("coker_fractionator", "coker_heater", "coke_drum", "coke_pit", "coke_kiln", "coke_cooler", "coal_tower", "coke_oven_battery",
	"pusher_machine", "coke_quench_tower", "byproduct_plant", "tar_still", "paste_plant", "forming_press", "baking_furnace", "rodding_shop")
for name in CARBON_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in CARBON_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("coker_fractionator", ["SPS", "SWS", "SPS"], {"S": "minecraft:iron_block", "P": P_, "W": "minecraft:white_wool"}, "coker_fractionator")
shaped("coker_heater", ["SPS", "BFB", "SXS"], {"S": S_, "P": P_, "B": "minecraft:bricks", "F": "minecraft:blast_furnace", "X": "minecraft:piston"}, "coker_heater")
shaped("coke_drum", ["SSS", "S S", "SXS"], {"S": "minecraft:iron_block", "X": "minecraft:piston"}, "coke_drum")
shaped("coke_pit", ["SPS", "C C", "CXC"], {"S": S_, "P": P_, "C": "minecraft:smooth_stone", "X": f"{ns}:crankshaft"}, "coke_pit")
shaped("coke_kiln", ["BBB", "FXF", "BBB"], {"B": "minecraft:bricks", "F": "minecraft:blast_furnace", "X": f"{ns}:crankshaft"}, "coke_kiln")
shaped("coke_cooler", ["SSS", "WXW", "SSS"], {"S": S_, "W": "minecraft:water_bucket", "X": f"{ns}:crankshaft"}, "coke_cooler")
shaped("coal_tower", ["SHS", "C C", "SXS"], {"S": S_, "H": "minecraft:hopper", "C": "minecraft:smooth_stone", "X": "minecraft:piston"}, "coal_tower")
shaped("coke_oven_battery", ["BBB", "BFB", "BBB"], {"B": "minecraft:bricks", "F": "minecraft:blast_furnace"}, "coke_oven_battery")
shaped("pusher_machine", ["SXS", "IRI", "SSS"], {"S": S_, "X": "minecraft:piston", "I": "minecraft:iron_block", "R": "minecraft:rail"}, "pusher_machine")
shaped("coke_quench_tower", ["CWC", "C C", "SRS"], {"C": "minecraft:smooth_stone", "W": "minecraft:water_bucket", "S": S_, "R": "minecraft:rail"}, "coke_quench_tower")
shaped("byproduct_plant", ["SPS", "CXC", "SPS"], {"S": S_, "P": P_, "C": "minecraft:copper_block", "X": f"{ns}:crankshaft"}, "byproduct_plant")
shaped("tar_still", ["SPS", "SFS", "SPS"], {"S": S_, "P": P_, "F": "minecraft:furnace"}, "tar_still")
shaped("paste_plant", ["SHS", "GXG", "SPS"], {"S": S_, "H": "minecraft:hopper", "G": "minecraft:grindstone", "X": f"{ns}:crankshaft", "P": P_}, "paste_plant")
shaped("forming_press", ["SXS", "SIS", "SSS"], {"S": S_, "X": "minecraft:piston", "I": "minecraft:iron_block"}, "forming_press")
shaped("baking_furnace", ["BHB", "BFB", "BBB"], {"B": "minecraft:bricks", "H": "minecraft:hopper", "F": "minecraft:blast_furnace"}, "baking_furnace")
shaped("rodding_shop", ["SXS", "IFI", "SSS"], {"S": S_, "X": "minecraft:piston", "I": "minecraft:iron_block", "F": "minecraft:blast_furnace"}, "rodding_shop")
shaped("anode_rod_copper", ["C", "C", "S"], {"C": "minecraft:copper_ingot", "S": f"{ns}:steel_ingot"}, "anode_rod")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"green_petroleum_coke": ("Green Petroleum Coke (1 t)", (40, 40, 42)), "calcined_coke_bag": ("Bulk Bag of Calcined Coke", (70, 70, 74)),
		"metallurgical_coke": ("Metallurgical Coke", (90, 90, 96)), "green_anode": ("Green Anode", (34, 34, 36)), "baked_anode": ("Baked Anode", (60, 60, 64)),
		"rodded_anode": ("Rodded Anode", (110, 110, 116)), "spent_anode": ("Spent Anode (butt on its rod)", (120, 110, 100)),
		"crushed_butts": ("Crushed Anode Butts", (66, 64, 62)), "anode_rod": ("Anode Rod", (190, 120, 80))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.coker_fractionator": "Coker Fractionator", f"block.{ns}.coker_heater": "Coker Heater (495 C)", f"block.{ns}.coke_drum": "Coke Drum",
	f"block.{ns}.coke_pit": "Coke Pit (hydraulic decoking, 4.16 kV)", f"block.{ns}.coke_kiln": "Coke Calcining Kiln (rotary)", f"block.{ns}.coke_cooler": "Coke Cooler (rotary)",
	f"block.{ns}.coal_tower": "Coal Tower", f"block.{ns}.coke_oven_battery": "Coke Oven Battery (13 slot ovens)", f"block.{ns}.pusher_machine": "Coke Pusher Machine",
	f"block.{ns}.coke_quench_tower": "Coke Quench Tower", f"block.{ns}.byproduct_plant": "By-product Plant (4.16 kV exhauster)", f"block.{ns}.tar_still": "Tar Still",
	f"block.{ns}.paste_plant": "Anode Paste Plant (4.16 kV)", f"block.{ns}.forming_press": "Anode Forming Press (vibro-compactor)",
	f"block.{ns}.baking_furnace": "Ring Baking Furnace", f"block.{ns}.rodding_shop": "Rodding Shop"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10b3b: the potline (docs/multiblocks/potline.md) ---------------------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/potline.py; here loot, names, tags, recipes, the 180 kA bus and the items.
POTLINE_PARTS = ("rectiformer", "pot", "fume_treatment_centre", "ftc_fan", "casting_machine")
for name in POTLINE_PARTS:
	big_part_loot(name)
# the 180 kA bus: a 0.55 m2 aluminium bar (or 0.18 m2 of copper) on two porcelain standoffs, drawn along z like the 10b1 bars
for name, metal in (("dc_bus_180k", "hipaint"), ("dc_bus_180k_copper", "copper")):
	els = []
	for z in (2, 12):
		els.append(cuboid([6, 0, z], [10, 3, z + 2], "#glaze"))
	els.append(cuboid([1, 3, 0], [15, 15, 16], "#bar") if metal == "hipaint" else cuboid([3, 3, 0], [13, 11, 16], "#bar"))
	save_json(f"{ASSETS}/models/block/{name}.json", {"textures": {"glaze": f"{ns}:block/mat/mat_porcelain_brown",
		"bar": f"{ns}:block/mat/{'mat_hitemp_aluminium' if metal == 'hipaint' else 'mat_copper'}", "particle": f"{ns}:block/mat/mat_galvanized"}, "elements": els})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {"axis=z": {"model": f"{ns}:block/{name}"}, "axis=x": {"model": f"{ns}:block/{name}", "y": 90},
		"axis=y": {"model": f"{ns}:block/{name}", "x": 90}}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{name}"}})
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
# the aluminium block: nine ingots
if os.path.exists(f"{ORE_ART}/aluminium_block.png"):
	shutil.copy(f"{ORE_ART}/aluminium_block.png", f"{ASSETS}/textures/block/ore/aluminium_block.png")
	al_tex = f"{ns}:block/ore/aluminium_block"
else:
	tex("aluminium_block_placeholder", lambda img, d: noise(img, (200, 204, 210), 8))
	al_tex = f"{ns}:block/aluminium_block_placeholder"
block_model("aluminium_block", "minecraft:block/cube_all", {"all": al_tex})
save_json(f"{ASSETS}/blockstates/aluminium_block.json", {"variants": {"": {"model": f"{ns}:block/aluminium_block"}}})
save_json(f"{ASSETS}/items/aluminium_block.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/aluminium_block"}})
save_json(f"{DATA}/loot_table/blocks/aluminium_block.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:aluminium_block"}],
	"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in POTLINE_PARTS + ("dc_bus_180k", "dc_bus_180k_copper", "aluminium_block")]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("rectiformer", ["CRC", "SKS", "SSS"], {"C": "minecraft:copper_block", "R": "minecraft:redstone_block", "S": S_, "K": "minecraft:comparator"}, "rectiformer")
shaped("pot", ["IBI", "BKB", "III"], {"I": "minecraft:iron_block", "B": "minecraft:bricks", "K": f"{ns}:calcined_coke_bag"}, "pot")
shaped("fume_treatment_centre", ["SPS", "WHW", "SSS"], {"S": S_, "P": P_, "W": "minecraft:white_wool", "H": "minecraft:hopper"}, "fume_treatment_centre")
shaped("ftc_fan", ["SPS", "SXS", "SSS"], {"S": S_, "P": P_, "X": f"{ns}:crankshaft"}, "ftc_fan")
shaped("casting_machine", ["SCS", "IXI", "SSS"], {"S": S_, "C": "minecraft:cauldron", "I": "minecraft:iron_block", "X": "minecraft:piston"}, "casting_machine")
shaped("tapping_crucible", ["I I", "IBI", " I "], {"I": "minecraft:iron_ingot", "B": "minecraft:bricks"}, "tapping_crucible")
shaped("dc_bus_180k", ["AAA", "AAA", "BAB"], {"A": f"{ns}:aluminium_block", "B": "minecraft:brick"}, "dc_bus_180k")
shaped("dc_bus_180k_copper", ["KKK", "KKK", "BKB"], {"K": "minecraft:copper_block", "B": "minecraft:brick"}, "dc_bus_180k_copper")
shaped("aluminium_block", ["AAA", "AAA", "AAA"], {"A": f"{ns}:aluminium_ingot"}, "aluminium_block")
shapeless("aluminium_ingot_from_block", [f"{ns}:aluminium_block"], "aluminium_ingot", 9)
shaped("anode_rod_aluminium", ["A", "A", "S"], {"A": f"{ns}:aluminium_ingot", "S": f"{ns}:steel_ingot"}, "anode_rod")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"aluminium_ingot": ("Aluminium Ingot (22.7 kg)", (196, 200, 206)), "reacted_alumina_bag": ("Bulk Bag of Reacted Alumina", (226, 222, 210)),
		"spent_pot_lining": ("Spent Pot Lining", (52, 50, 54)), "tapping_crucible": ("Tapping Crucible (8 t)", (150, 76, 60))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.rectiformer": "Rectiformer (60 kA, 138 kV)", f"block.{ns}.pot": "Pot (180 kA prebake)", f"block.{ns}.fume_treatment_centre": "Fume Treatment Centre",
	f"block.{ns}.ftc_fan": "FTC Main Fan (250 kW, 4.16 kV)", f"block.{ns}.casting_machine": "Ingot Casting Machine", f"block.{ns}.aluminium_block": "Block of Aluminium",
	f"block.{ns}.dc_bus_180k": "DC Bus Bar (180 kA aluminium)", f"block.{ns}.dc_bus_180k_copper": "DC Bus Bar (180 kA copper)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10c1: blueprints (docs/blueprints.md) --------------------------------------------------------------------------------
# the site marker: a surveyor's wooden stake with an orange flag
els = [cuboid([7, 0, 7], [9, 12, 9], "#stake"), cuboid([9, 9, 7.5], [13, 12, 8.5], "#flag")]
if os.path.exists(f"{ORE_ART}/site_marker.png"):
	shutil.copy(f"{ORE_ART}/site_marker.png", f"{ASSETS}/textures/block/ore/site_marker.png")
	flag_tex = f"{ns}:block/ore/site_marker"
else:
	tex("site_marker_placeholder", lambda img, d: noise(img, (230, 120, 30), 10))
	flag_tex = f"{ns}:block/site_marker_placeholder"
save_json(f"{ASSETS}/models/block/site_marker.json", {"textures": {"stake": f"{ns}:block/mat/mat_wood_fir", "flag": flag_tex, "particle": f"{ns}:block/mat/mat_wood_fir"},
	"elements": els})
save_json(f"{ASSETS}/blockstates/site_marker.json", {"variants": {"": {"model": f"{ns}:block/site_marker"}}})
save_json(f"{ASSETS}/items/site_marker.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/site_marker"}})
save_json(f"{DATA}/loot_table/blocks/site_marker.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:site_marker"}],
	"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
t = json.load(open(f"{ROOT}/data/minecraft/tags/block/mineable/axe.json")) if os.path.exists(f"{ROOT}/data/minecraft/tags/block/mineable/axe.json") else {"values": []}
t["values"] = [v for v in t["values"] if v != f"{ns}:site_marker"] + [f"{ns}:site_marker"]
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/axe.json", t)
shaped("blueprint", [" B ", "PPP"], {"B": "minecraft:blue_dye", "P": "minecraft:paper"}, "blueprint")
shaped("survey_tool", ["S", "C", "P"], {"S": "minecraft:spyglass", "C": "minecraft:compass", "P": "minecraft:paper"}, "survey_tool")
shaped("site_marker", ["W", "S"], {"W": "minecraft:orange_wool", "S": "minecraft:stick"}, "site_marker", 4)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"blueprint": ("Blueprint", (40, 80, 170)), "survey_tool": ("Survey Tool", (180, 150, 60))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang[f"block.{ns}.site_marker"] = "Site Marker"
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10c2: the tower and crawler cranes (docs/multiblocks/tower-crane.md, crawler-crane.md) --------------------------------
# blockstates, models, shapes and items come from tools/models/cranes.py (run after this, it reads the paint); here the paint, loot,
# names, tags and recipes. The superstructure is drawn by client/CraneRenderer in the same paint.
# the crane paint is Ludo art since the art wave (art/source/mat_paint_crane_yellow.webp, tools/process_materials.py)
CRANE_PARTS = ("tower_crane", "crawler_crane")
for name in CRANE_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in CRANE_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("tower_crane", ["III", "SXS", "SCS"], {"I": "minecraft:iron_block", "S": S_, "X": f"{ns}:crankshaft", "C": "minecraft:iron_chain"}, "tower_crane")
shaped("crawler_crane", ["SCS", "IXI", "TTT"], {"S": S_, "C": "minecraft:iron_chain", "I": "minecraft:iron_block", "X": f"{ns}:crankshaft",
	"T": "minecraft:heavy_weighted_pressure_plate"}, "crawler_crane")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang[f"block.{ns}.tower_crane"] = "Tower Crane (16 t, 75 m jib)"
lang[f"block.{ns}.crawler_crane"] = "Crawler Crane (300 t)"
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10c3: the ring crane (docs/multiblocks/ring-crane.md) ------------------------------------------------------------------------
# its ring's blockstate, models, shapes and item come from tools/models/cranes.py; the upper works are drawn by client/CraneRenderer.
big_part_loot("ring_crane")
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	t["values"] = [v for v in t["values"] if v != f"{ns}:ring_crane"] + [f"{ns}:ring_crane"]
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("ring_crane", ["CCC", "XIX", "III"], {"C": f"{ns}:crawler_crane", "X": f"{ns}:crankshaft", "I": "minecraft:iron_block"}, "ring_crane")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang[f"block.{ns}.ring_crane"] = "Ring Crane (5,000 t)"
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10d2: true-size gear in place of the one-block stand-ins ------------------------------------------------------------------
# the switchgear sections', the cabinet's and the 10 MVA unit's blockstates, models, shapes and items come from tools/models/mv_switchgear.py,
# the 4.16 kV pole unit's from tools/models/pole_transformer.py (its blockstate above); here loot, tags, recipes and names.
D2_PARTS = ("mv_switchgear_main", "mv_switchgear_feeder", "breaker_relay_cabinet", "substation_transformer_10")
for name in D2_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in D2_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("mv_switchgear_main", ["SGS", "SRS", "SCS"], {"S": S_, "G": "minecraft:glass_pane", "R": "minecraft:comparator", "C": "minecraft:copper_block"}, "mv_switchgear_main")
shaped("mv_switchgear_feeder", ["SGS", "SRS", "SCS"], {"S": S_, "G": "minecraft:glass_pane", "R": "minecraft:comparator", "C": "minecraft:copper_ingot"}, "mv_switchgear_feeder")
shaped("breaker_relay_cabinet", ["SGS", "SRS", "SIS"], {"S": S_, "G": "minecraft:glass_pane", "R": "minecraft:comparator", "I": "minecraft:iron_ingot"}, "breaker_relay_cabinet")
shaped("substation_transformer_10", ["IDI", "TCT", "IDI"], {"I": "minecraft:iron_ingot", "D": "minecraft:diamond", "T": f"{ns}:pole_transformer_mv", "C": "minecraft:copper_block"}, "substation_transformer_10")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang[f"block.{ns}.mv_switchgear_main"] = "4.16 kV Switchgear, Main Section"
lang[f"block.{ns}.mv_switchgear_feeder"] = "4.16 kV Switchgear, Feeder Section"
lang[f"block.{ns}.breaker_relay_cabinet"] = "Breaker Relay Cabinet (50/51, 81U)"
lang[f"block.{ns}.pole_transformer_mv"] = "Pole-Mount Transformer (MV / LV, 25 kVA)"
lang[f"block.{ns}.substation_transformer_10"] = "Power Transformer (MV / HV, 10 MVA)"
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 10d1: generator bus protection (docs/multiblocks/gen-bus-protection.md) ---------------------------------------------------
# the feeder breaker's and the panel's blockstates, models, shapes and items come from tools/models/gen_bus.py; here loot, tags, recipes,
# names and the grounding set (its texture a placeholder until 10d4's art).
for name in ("gen_feeder_breaker", "bus_relay_panel"):
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:gen_feeder_breaker", f"{ns}:bus_relay_panel"]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("gen_feeder_breaker", ["SIS", "SXS", "SCS"], {"S": S_, "I": "minecraft:iron_block", "X": f"{ns}:hv_breaker_3p", "C": "minecraft:copper_ingot"}, "gen_feeder_breaker")
shaped("bus_relay_panel", ["SGS", "SRS", "SCS"], {"S": S_, "G": "minecraft:glass_pane", "R": "minecraft:comparator", "C": "minecraft:copper_ingot"}, "bus_relay_panel")
shaped("grounding_set", ["CCC", " I ", " T "], {"C": "minecraft:copper_ingot", "I": "minecraft:iron_ingot", "T": "minecraft:stick"}, "grounding_set")
tex("grounding_set_placeholder", lambda img, d: noise(img, (190, 110, 60), 12), "item")   # drawn so the noise sequence stays put; 10d4's art replaces it
save_json(f"{ASSETS}/models/item/grounding_set.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{art('grounding_set')}"}})
save_json(f"{ASSETS}/items/grounding_set.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/grounding_set"}})
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang[f"block.{ns}.gen_feeder_breaker"] = "15 kV Feeder Breaker (metal-clad, vacuum)"
lang[f"block.{ns}.bus_relay_panel"] = "Generator Bus Relay Panel (87B, 51V, 50/51)"
lang[f"item.{ns}.grounding_set"] = "Grounding Clamp Set"
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- The ammonia plant (docs/multiblocks/ammonia-plant.md) ------------------------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/ammonia.py; here loot, names, tags, recipes and the two items.
AMMONIA_PARTS = ("nitrogen_generator", "syngas_compressor", "ammonia_converter", "nitric_acid_unit", "an_neutraliser", "emulsion_blender")
for name in AMMONIA_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in AMMONIA_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("nitrogen_generator", ["SPS", "GXG", "SSS"], {"S": S_, "P": P_, "G": "minecraft:charcoal", "X": "minecraft:piston"}, "nitrogen_generator")
shaped("syngas_compressor", ["SPS", "XIX", "SSS"], {"S": S_, "P": P_, "X": "minecraft:piston", "I": "minecraft:iron_block"}, "syngas_compressor")
shaped("ammonia_converter", ["SPS", "IFI", "SIS"], {"S": S_, "P": P_, "I": "minecraft:iron_block", "F": "minecraft:blast_furnace"}, "ammonia_converter")
shaped("nitric_acid_unit", ["SPS", "SFS", "SXS"], {"S": S_, "P": P_, "F": "minecraft:blast_furnace", "X": "minecraft:piston"}, "nitric_acid_unit")
shaped("an_neutraliser", ["SPS", "S S", "SPS"], {"S": S_, "P": P_}, "an_neutraliser")
shaped("emulsion_blender", ["SHS", "PXP", "SSS"], {"S": S_, "H": "minecraft:hopper", "P": P_, "X": "minecraft:piston"}, "emulsion_blender")
recipe = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [f"{ns}:platinum"], "result": {"id": f"{ns}:gauze_pack", "count": 1}}
save_json(f"{DATA}/recipe/gauze_pack.json", recipe)
recipe = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [f"{ns}:plastic_sheet", f"{ns}:diesel_bucket", "minecraft:iron_ingot"],
	"result": {"id": f"{ns}:emulsifier_pail", "count": 4}}
save_json(f"{DATA}/recipe/emulsifier_pail.json", recipe)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"gauze_pack": ("Platinum-Rhodium Gauze Pack", (200, 200, 196)), "emulsifier_pail": ("Emulsifier Pail (20 kg)", (180, 150, 60))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):   # the Ludo icon (art/source/ore/), as the other plant items
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.nitrogen_generator": "Nitrogen Generator (PSA, 10 Nm3/h)", f"block.{ns}.syngas_compressor": "Syngas Compressor (150 bar)",
	f"block.{ns}.ammonia_converter": "Ammonia Converter (with its loop)", f"block.{ns}.nitric_acid_unit": "Nitric Acid Unit (Pt-Rh gauze, 60 % acid)",
	f"block.{ns}.an_neutraliser": "Ammonium Nitrate Neutraliser", f"block.{ns}.emulsion_blender": "Emulsion Blender",
	f"block.{ns}.lpg_bullet": "Pressure Bullet (18,000 gal; LPG or ammonia)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 14b and 14c: the sales line's meter stations and the acid plant's superheater -----------------------------------------
# (docs/multiblocks/sales-gas.md, acid-plant-steam.md) blockstates, models, shapes and items come from tools/models/gas_power.py; here
# loot, names, tags and recipes.
GAS_POWER_PARTS = ("meter_building", "filter_separator", "ultrasonic_meter_run", "regulator_skid", "pipeline_tap", "acid_superheater")
for name in GAS_POWER_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in GAS_POWER_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("meter_building", ["SGS", "CRC", "SSS"], {"S": S_, "G": "minecraft:glass_pane", "C": "minecraft:comparator", "R": "minecraft:repeater"}, "meter_building")
shaped("filter_separator", ["SPS", "SWS", "SBS"], {"S": S_, "P": P_, "W": "minecraft:white_wool", "B": "minecraft:bucket"}, "filter_separator")
shaped("ultrasonic_meter_run", ["PPP", "SCS", "PPP"], {"S": S_, "P": P_, "C": "minecraft:comparator"}, "ultrasonic_meter_run")
shaped("regulator_skid", ["SPS", "LFL", "SPS"], {"S": S_, "P": P_, "L": "minecraft:lever", "F": "minecraft:furnace"}, "regulator_skid")
shaped("pipeline_tap", [" L ", "SPS", " P "], {"S": S_, "P": P_, "L": "minecraft:lever"}, "pipeline_tap")
shaped("acid_superheater", ["SBS", "PCP", "SBS"], {"S": S_, "B": "minecraft:bricks", "P": P_, "C": "minecraft:copper_block"}, "acid_superheater")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({f"block.{ns}.meter_building": "Meter Building (flow computer, chromatograph, RTU)", f"block.{ns}.filter_separator": "Filter Separator",
	f"block.{ns}.ultrasonic_meter_run": "Ultrasonic Meter Run (4 in, 4-path)", f"block.{ns}.regulator_skid": "Regulator Skid (with line heater)",
	f"block.{ns}.pipeline_tap": "Pipeline Tap (hot tap and block valve)", f"block.{ns}.acid_superheater": "Acid Plant Steam Superheater"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 7g: the fuel plant (docs/multiblocks/fuel.md) -------------------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/fuel.py; here loot, names, tags, recipes and the items.
FUEL_PARTS = ("uf6_autoclave", "idr_kiln", "granulator", "pellet_press", "sintering_furnace", "pellet_grinder", "rod_loader", "rod_welder",
	"assembly_fixture", "helium_recovery_unit", "fuel_assembly")
for name in FUEL_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in FUEL_PARTS if n != "fuel_assembly"]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("uf6_autoclave", ["SHS", "SFS", "SSS"], {"S": S_, "H": "minecraft:hopper", "F": "minecraft:furnace"}, "uf6_autoclave")
shaped("idr_kiln", ["SPS", "BFB", "SXS"], {"S": S_, "P": P_, "B": "minecraft:bricks", "F": "minecraft:blast_furnace", "X": f"{ns}:crankshaft"}, "idr_kiln")
shaped("granulator", ["SHS", "XGX", "SSS"], {"S": S_, "H": "minecraft:hopper", "X": "minecraft:piston", "G": "minecraft:grindstone"}, "granulator")
shaped("pellet_press", ["GXG", "SIS", "SSS"], {"G": "minecraft:glass", "X": "minecraft:piston", "S": S_, "I": "minecraft:iron_block"}, "pellet_press")
shaped("sintering_furnace", ["SSS", "BFB", "SPS"], {"S": S_, "B": "minecraft:bricks", "F": "minecraft:blast_furnace", "P": P_}, "sintering_furnace")
shaped("pellet_grinder", ["SGS", "SXS", "SSS"], {"S": S_, "G": "minecraft:grindstone", "X": "minecraft:piston"}, "pellet_grinder")
shaped("rod_loader", ["GHG", "SXS", "SSS"], {"G": "minecraft:glass", "H": "minecraft:hopper", "S": S_, "X": "minecraft:piston"}, "rod_loader")
shaped("rod_welder", ["SCS", "GRG", "SSS"], {"S": S_, "C": "minecraft:copper_block", "G": "minecraft:glass", "R": "minecraft:redstone_block"}, "rod_welder")
shaped("assembly_fixture", ["SXS", "IAI", "SSS"], {"S": S_, "X": "minecraft:piston", "I": "minecraft:iron_block", "A": "minecraft:anvil"}, "assembly_fixture")
shaped("helium_recovery_unit", ["SPS", "IXI", "SPS"], {"S": S_, "P": P_, "I": "minecraft:packed_ice", "X": "minecraft:piston"}, "helium_recovery_unit")
shaped("empty_helium_cylinder", [" S ", "S S", "SSS"], {"S": S_}, "empty_helium_cylinder")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"spacer_grid": ("Spacer Grid (Zircaloy-4)", (180, 180, 175)), "empty_helium_cylinder": ("Empty Helium Cylinder", (150, 150, 150)),
		"helium_cylinder": ("Helium Cylinder", (140, 120, 90))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.uf6_autoclave": "UF6 Autoclave (30B)", f"block.{ns}.idr_kiln": "IDR Kiln (dry route)", f"block.{ns}.granulator": "Granulator",
	f"block.{ns}.pellet_press": "Pellet Press (rotary)", f"block.{ns}.sintering_furnace": "Sintering Furnace (pusher, 1,750 C)",
	f"block.{ns}.pellet_grinder": "Pellet Grinder (centreless)", f"block.{ns}.rod_loader": "Rod Loader", f"block.{ns}.rod_welder": "Rod Welder (helium, 2 MPa)",
	f"block.{ns}.assembly_fixture": "Assembly Fixture", f"block.{ns}.helium_recovery_unit": "Helium Recovery Unit",
	f"block.{ns}.fuel_assembly": "Fuel Assembly (17x17, 4.95 %)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 8a: the nuclear island and the boron plant (docs/multiblocks/reactor.md, boron.md) -----------------------------
# blockstates, models, shapes and items come from tools/models/reactor.py and boron.py; here loot, names, tags, recipes and the items.
REACTOR_PARTS = ("reactor_vessel", "crdm_head", "reactor_console", "steam_generator", "reactor_coolant_pump", "pressurizer", "cvcs_skid", "aux_feed_pump", "rhr_pump", "rhr_heat_exchanger",
	"borax_dissolver", "boric_acid_crystallizer", "b4c_furnace", "rcca_bench")
for name in REACTOR_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in REACTOR_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("reactor_vessel", ["SIS", "IBI", "SIS"], {"S": S_, "I": "minecraft:iron_block", "B": "minecraft:blast_furnace"}, "reactor_vessel")
shaped("crdm_head", ["CCC", "SRS", "SIS"], {"C": "minecraft:copper_block", "S": S_, "R": "minecraft:redstone_block", "I": "minecraft:iron_block"}, "crdm_head")
shaped("reactor_console", ["GGG", "RCR", "SSS"], {"G": "minecraft:glass_pane", "R": "minecraft:redstone", "C": "minecraft:comparator", "S": S_}, "reactor_console")
shaped("steam_generator", ["SPS", "SIS", "SPS"], {"S": S_, "P": P_, "I": "minecraft:iron_block"}, "steam_generator")
shaped("reactor_coolant_pump", ["SCS", "SXS", "SIS"], {"S": S_, "C": "minecraft:copper_block", "X": f"{ns}:crankshaft", "I": "minecraft:iron_block"}, "reactor_coolant_pump")
shaped("pressurizer", ["SPS", "SRS", "SIS"], {"S": S_, "P": P_, "R": "minecraft:redstone_block", "I": "minecraft:iron_block"}, "pressurizer")
shaped("cvcs_skid", ["SHS", "PXP", "SSS"], {"S": S_, "H": "minecraft:hopper", "P": P_, "X": "minecraft:piston"}, "cvcs_skid")
shaped("aux_feed_pump", ["SPS", "SXS", "SSS"], {"S": S_, "P": P_, "X": "minecraft:piston"}, "aux_feed_pump")
shaped("rhr_pump", ["SCS", "SXS", "SPS"], {"S": S_, "C": "minecraft:copper_block", "X": "minecraft:piston", "P": P_}, "rhr_pump")
shaped("rhr_heat_exchanger", ["SPS", "SCS", "SPS"], {"S": S_, "P": P_, "C": "minecraft:copper_ingot"}, "rhr_heat_exchanger")
shaped("borax_dissolver", ["SHS", "SCS", "SSS"], {"S": S_, "H": "minecraft:hopper", "C": "minecraft:cauldron"}, "borax_dissolver")
shaped("boric_acid_crystallizer", ["SCS", "SXS", "SHS"], {"S": S_, "C": "minecraft:cauldron", "X": "minecraft:piston", "H": "minecraft:hopper"}, "boric_acid_crystallizer")
shaped("b4c_furnace", ["BCB", "BFB", "SSS"], {"B": "minecraft:bricks", "C": "minecraft:copper_block", "F": "minecraft:blast_furnace", "S": S_}, "b4c_furnace")
shaped("rcca_bench", ["GHG", "SAS", "SSS"], {"G": "minecraft:glass", "H": "minecraft:hopper", "S": S_, "A": "minecraft:anvil"}, "rcca_bench")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"boric_acid": ("Boric Acid (25 kg bag)", (235, 235, 225)), "b4c_pellets": ("Boron Carbide Pellets (1 kg tray)", (60, 60, 65)),
		"rcca": ("RCCA (B4C control rods)", (170, 170, 170))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.reactor_vessel": "Reactor Vessel (9 assemblies)", f"block.{ns}.crdm_head": "CRDM Head (5 rod drives)",
	f"block.{ns}.reactor_console": "Reactor Console", f"block.{ns}.steam_generator": "Steam Generator (U-tube)",
	f"block.{ns}.reactor_coolant_pump": "Reactor Coolant Pump (500 kW)", f"block.{ns}.pressurizer": "Pressurizer",
	f"block.{ns}.cvcs_skid": "CVCS Skid (charging, letdown, boron)", f"block.{ns}.aux_feed_pump": "Aux Feed Pump (75 hp)",
	f"block.{ns}.rhr_pump": "RHR Pump (200 kW, 4.16 kV)", f"block.{ns}.rhr_heat_exchanger": "RHR Heat Exchanger",
	f"block.{ns}.borax_dissolver": "Borax Dissolver", f"block.{ns}.boric_acid_crystallizer": "Boric Acid Crystallizer",
	f"block.{ns}.b4c_furnace": "B4C Furnace (Acheson)", f"block.{ns}.rcca_bench": "RCCA Bench"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 8b: the turbine island (docs/multiblocks/turbine-island.md) ---------------------------------------------------
ISLAND_PARTS = ("turbine_valve_chest", "hp_turbine", "moisture_separator_reheater", "lp_turbine", "tg_generator_60", "main_condenser", "condensate_pump",
	"deaerator", "main_feed_pump", "hp_feedwater_heater", "circulating_water_pump", "unit_auxiliary_transformer")
for name in ISLAND_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in ISLAND_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("turbine_valve_chest", ["SPS", "IRI", "SPS"], {"S": S_, "P": P_, "I": "minecraft:iron_block", "R": "minecraft:redstone"}, "turbine_valve_chest")
shaped("hp_turbine", ["SIS", "TXT", "SIS"], {"S": S_, "I": "minecraft:iron_block", "T": f"{ns}:turbine_casing", "X": f"{ns}:crankshaft"}, "hp_turbine")
shaped("moisture_separator_reheater", ["SSS", "PIP", "SSS"], {"S": S_, "P": P_, "I": "minecraft:iron_block"}, "moisture_separator_reheater")
shaped("lp_turbine", ["TIT", "TXT", "SIS"], {"S": S_, "I": "minecraft:iron_block", "T": f"{ns}:turbine_casing", "X": f"{ns}:crankshaft"}, "lp_turbine")
shaped("tg_generator_60", ["CDC", "GCG", "CDC"], {"C": CB_, "D": "minecraft:diamond", "G": f"{ns}:aero_generator"}, "tg_generator_60")
shaped("main_condenser", ["SPS", "PIP", "SPS"], {"S": S_, "P": P_, "I": "minecraft:copper_block"}, "main_condenser")
shaped("condensate_pump", ["S S", "SXS", "SPS"], {"S": S_, "X": "minecraft:piston", "P": P_}, "condensate_pump")
shaped("deaerator", ["SPS", "S S", "SSS"], {"S": S_, "P": P_}, "deaerator")
shaped("main_feed_pump", ["SXS", "PCP", "SSS"], {"S": S_, "X": "minecraft:piston", "P": P_, "C": CB_}, "main_feed_pump")
shaped("hp_feedwater_heater", ["SPS", "SCS", "SPS"], {"S": S_, "P": P_, "C": "minecraft:copper_block"}, "hp_feedwater_heater")
shaped("circulating_water_pump", ["SCS", "SXS", "SPS"], {"S": S_, "C": CB_, "X": "minecraft:piston", "P": P_}, "circulating_water_pump")
shaped("unit_auxiliary_transformer", ["SCS", "CTC", "SCS"], {"S": S_, "C": CB_, "T": f"{ns}:unit_substation"}, "unit_auxiliary_transformer")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({f"block.{ns}.turbine_valve_chest": "Turbine Valve Chest", f"block.{ns}.hp_turbine": "HP Turbine (51 MW plant)",
	f"block.{ns}.moisture_separator_reheater": "Moisture Separator Reheater", f"block.{ns}.lp_turbine": "LP Turbine (double-flow)",
	f"block.{ns}.tg_generator_60": "Turbine Generator (60 MVA, 13.8 kV, H2-cooled)", f"block.{ns}.main_condenser": "Main Condenser",
	f"block.{ns}.condensate_pump": "Condensate Pump (150 kW)", f"block.{ns}.deaerator": "Deaerator", f"block.{ns}.main_feed_pump": "Main Feed Pump (500 kW, 4.16 kV)",
	f"block.{ns}.hp_feedwater_heater": "HP Feedwater Heater", f"block.{ns}.circulating_water_pump": "Circulating Water Pump (300 kW, 4.16 kV)",
	f"block.{ns}.unit_auxiliary_transformer": "Unit Auxiliary Transformer (7.5 MVA, 13.8/4.16 kV)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 8c: the natural-draft cooling tower (docs/multiblocks/natural-draft-tower.md) ------------------------------------
big_part_loot("natural_draft_tower")
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	t["values"] = [v for v in t["values"] if v != f"{ns}:natural_draft_tower"] + [f"{ns}:natural_draft_tower"]
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("natural_draft_tower", ["CCC", "CPC", "CHC"], {"C": "minecraft:gray_concrete", "P": P_, "H": "minecraft:hopper"}, "natural_draft_tower")
save_json(f"{ASSETS}/blockstates/tower_shell.json", {"variants": {"": {"model": "minecraft:block/barrier"}}})   # drawn by the tower's renderer
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({f"block.{ns}.natural_draft_tower": "Natural-Draft Cooling Tower (88 m)", f"block.{ns}.tower_shell": "Cooling Tower Shell"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 8e: fuel handling and the dosimeter (docs/multiblocks/fuel-handling.md) -----------------------------------------
HANDLING_PARTS = ("refuelling_machine", "fuel_transfer_system", "spent_fuel_pool")
for name in HANDLING_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in HANDLING_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("refuelling_machine", ["III", "SXS", "S S"], {"I": "minecraft:iron_block", "S": S_, "X": "minecraft:piston"}, "refuelling_machine")
shaped("fuel_transfer_system", ["SPS", "SXS", "SPS"], {"S": S_, "P": P_, "X": "minecraft:piston"}, "fuel_transfer_system")
shaped("spent_fuel_pool", ["CCC", "CPC", "CHC"], {"C": "minecraft:light_gray_concrete", "P": P_, "H": "minecraft:copper_block"}, "spent_fuel_pool")
shaped("dosimeter", [" G ", "RCR", " I "], {"G": "minecraft:glass_pane", "R": "minecraft:redstone", "C": "minecraft:comparator", "I": "minecraft:iron_ingot"}, "dosimeter")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
item = "dosimeter"
if os.path.exists(f"{ORE_ART}/{item}.png"):
	shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
	item_tex = f"{ns}:item/ore/{item}"
else:
	tex(f"{item}_placeholder", lambda img, d: noise(img, (220, 190, 40), 10), "item")
	item_tex = f"{ns}:item/{item}_placeholder"
save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
lang.update({f"item.{ns}.dosimeter": "Dosimeter (electronic personal)", f"block.{ns}.refuelling_machine": "Refuelling Machine",
	f"block.{ns}.fuel_transfer_system": "Fuel Transfer System", f"block.{ns}.spent_fuel_pool": "Spent Fuel Pool (6 x 6 m, 12 m deep)",
	"death.attack.wildcatter.radiation": "%1$s died of radiation sickness",
	"death.attack.wildcatter.radiation.player": "%1$s died of radiation sickness"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 9b: NeoForge biome modifiers, one per placed ore feature -------------------------------------------------------
# The same table as OreGen.placements() and OreGen.biomes() (Fabric adds the features in code); OreGenTest checks they agree.
NF_MODIFIERS = "neoforge/src/main/resources/data/wildcatter/neoforge/biome_modifier"
ORE_BIOMES = {
	"EVERYWHERE": ["#minecraft:is_overworld"], "DEEP": ["#minecraft:is_overworld"],
	"DESERT_BADLANDS": ["minecraft:desert", "#minecraft:is_badlands"], "EVAPORITE": ["minecraft:desert", "#minecraft:is_badlands"],
	"SALT_DOME": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:swamp", "minecraft:mangrove_swamp", "#minecraft:is_beach"],
	"BEACH": ["#minecraft:is_beach"], "TROPICS": ["#minecraft:is_jungle", "#minecraft:is_savanna"],
	"MOUNTAINS": ["#minecraft:is_mountain"], "MOUNTAINS_BADLANDS": ["#minecraft:is_mountain", "#minecraft:is_badlands"],
	"PLAINS_BADLANDS": ["minecraft:plains", "minecraft:sunflower_plains", "#minecraft:is_badlands"],
	"OCEAN_BEACH": ["#minecraft:is_ocean", "#minecraft:is_beach"],
	"PLAINS_FOREST": ["minecraft:plains", "minecraft:sunflower_plains", "#minecraft:is_forest", "#minecraft:is_river"],
}
_ore_where = {m.group(1): m.group(2) for m in re.finditer(r'^\t[A-Z_]+\("(\w+)",.*?Where\.(\w+), Tool\.\w+\)[,;]', _ore_src, re.M | re.S)}
assert len(_ore_where) == 28, len(_ore_where)
_placements = {f"ore_{k}": w for k, w in _ore_where.items() if k not in ("sulfur", "halite", "sylvite", "chromite", "platinum")}
_placements.update({"salt_dome": "SALT_DOME", "evaporite_beds": "EVAPORITE", "bushveld_reef": "EVERYWHERE"})   # as before 15a; the grid dome checks its biome
def _holder_set(entries):
	tags = [e for e in entries if e.startswith("#")]
	ids = [e for e in entries if not e.startswith("#")]
	parts = tags + ([ids] if ids else [])
	return parts[0] if len(parts) == 1 else {"type": "neoforge:or", "values": parts}
if os.path.isdir(NF_MODIFIERS):
	for f in os.listdir(NF_MODIFIERS):
		os.remove(f"{NF_MODIFIERS}/{f}")
for name, where in _placements.items():
	save_json(f"{NF_MODIFIERS}/{name}.json", {"type": "neoforge:add_features", "biomes": _holder_set(ORE_BIOMES[where]),
		"features": f"{ns}:{name}", "step": "underground_ores"})

# ---- Phase 10d3: panels and switches (docs/multiblocks/panelboards.md, safety-switch.md, load-interrupter.md, ats.md) ----------------
# the parts' blockstates, models, shapes and items come from tools/models/panels.py; here loot, tags, recipes, names and the branch
# breaker and fuse items (their textures placeholders until 10d4's art). Last in the file, so its placeholders shift no other's noise.
D3_PARTS = ("load_centre", "panelboard_480", "panel_gutter", "safety_switch_100", "safety_switch_200", "lis_5kv", "lis_5kv_fused", "ats_480")
for name in D3_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in D3_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("load_centre", ["SIS", "SBS", "SCS"], {"S": S_, "I": "minecraft:iron_ingot", "B": f"{ns}:breaker", "C": "minecraft:copper_ingot"}, "load_centre")
shaped("panelboard_480", ["SBS", "SCS", "SBS"], {"S": S_, "B": f"{ns}:breaker_3p_industrial", "C": "minecraft:copper_block"}, "panelboard_480")
shaped("panel_gutter", ["SSS", "C C", "SSS"], {"S": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "panel_gutter", 4)
shaped("safety_switch_100", ["SLS", "SFS", "SSS"], {"S": S_, "L": "minecraft:lever", "F": f"{ns}:fuse"}, "safety_switch_100")
shaped("safety_switch_200", ["SLS", "SFS", "SFS"], {"S": S_, "L": "minecraft:lever", "F": f"{ns}:fuse"}, "safety_switch_200")
shaped("lis_5kv", ["SLS", "SXS", "SSS"], {"S": S_, "L": "minecraft:lever", "X": "minecraft:copper_block"}, "lis_5kv")
shaped("lis_5kv_fused", ["SLS", "SXS", "SQS"], {"S": S_, "L": "minecraft:lever", "X": "minecraft:copper_block", "Q": "minecraft:quartz_block"}, "lis_5kv_fused")
shaped("ats_480", ["SRS", "SBS", "SPS"], {"S": S_, "R": "minecraft:comparator", "B": f"{ns}:breaker_3p_industrial", "P": "minecraft:repeater"}, "ats_480")
BRANCH = {   # item: (its name, its recipe, its placeholder)
	"branch_breaker_1p_15": ("Branch Breaker, 1-Pole 15 A", (["B", "N"], {"B": f"{ns}:breaker", "N": "minecraft:iron_nugget"}), "branch_breaker"),
	"branch_breaker_1p_20": ("Branch Breaker, 1-Pole 20 A", (["B", "G"], {"B": f"{ns}:breaker", "G": "minecraft:gold_nugget"}), "branch_breaker"),
	"branch_breaker_2p_30": ("Branch Breaker, 2-Pole 30 A", (["BB", "NN"], {"B": f"{ns}:breaker", "N": "minecraft:iron_nugget"}), "branch_breaker"),
	"branch_breaker_2p_50": ("Branch Breaker, 2-Pole 50 A", (["BB", "GG"], {"B": f"{ns}:breaker", "G": "minecraft:gold_nugget"}), "branch_breaker"),
	"gfci_breaker_1p_20": ("GFCI Breaker, 1-Pole 20 A", (["B", "R"], {"B": f"{ns}:breaker", "R": "minecraft:comparator"}), "gfci_breaker"),
	"branch_breaker_277_1p_20": ("Panelboard Breaker, 1-Pole 20 A", (["T", "N"], {"T": f"{ns}:breaker_3p_industrial", "N": "minecraft:iron_nugget"}), "panelboard_breaker"),
	"branch_breaker_480_3p_20": ("Panelboard Breaker, 3-Pole 20 A", (["T", "G"], {"T": f"{ns}:breaker_3p_industrial", "G": "minecraft:gold_nugget"}), "panelboard_breaker"),
	"branch_breaker_480_3p_30": ("Panelboard Breaker, 3-Pole 30 A", (["T", "C"], {"T": f"{ns}:breaker_3p_industrial", "C": "minecraft:copper_ingot"}), "panelboard_breaker"),
	"branch_breaker_480_3p_60": ("Panelboard Breaker, 3-Pole 60 A", (["T", "I"], {"T": f"{ns}:breaker_3p_industrial", "I": "minecraft:iron_ingot"}), "panelboard_breaker"),
	"branch_breaker_480_3p_100": ("Panelboard Breaker, 3-Pole 100 A", (["T", "K"], {"T": f"{ns}:breaker_3p_industrial", "K": "minecraft:copper_block"}), "panelboard_breaker"),
}
FUSE_MID = {10: "minecraft:iron_nugget", 25: "minecraft:gold_nugget", 30: "minecraft:iron_nugget", 50: "minecraft:copper_ingot", 60: "minecraft:gold_nugget",
	100: "minecraft:gold_ingot", 200: "minecraft:copper_block"}
for amps in (30, 60, 100, 200):
	BRANCH[f"fuse_class_j_{amps}"] = (f"Class J Fuse, {amps} A", (["P", "X", "P"], {"P": "minecraft:paper", "X": FUSE_MID[amps if amps != 100 else 100]}), "fuse_class_j")
for amps in (10, 25, 50, 100, 200):
	BRANCH[f"fuse_e_{amps}"] = (f"E-Rated Power Fuse, {amps}E", (["Q", "X", "Q"], {"Q": "minecraft:quartz", "X": FUSE_MID[amps]}), "fuse_e")
PLACEHOLDERS = {"branch_breaker": (40, 42, 46), "gfci_breaker": (220, 220, 214), "panelboard_breaker": (70, 74, 80), "fuse_class_j": (190, 160, 100),
	"fuse_e": (200, 196, 186)}
for kind, colour in PLACEHOLDERS.items():
	if not os.path.exists(f"art/final/{kind}.png"):   # 10d4 drew them all; last in the file, so leaving them out shifts no other noise
		tex(f"{kind}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (title, (pattern, key), kind) in BRANCH.items():
	shaped(item, pattern, key, item, 3 if kind.startswith("fuse") else 1)
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{art(kind)}"}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = title
lang[f"block.{ns}.load_centre"] = "Load Centre (residential, 200 A)"
lang[f"block.{ns}.panelboard_480"] = "Panelboard (industrial LV, 225 A)"
lang[f"block.{ns}.panel_gutter"] = "Panel Gutter (one branch space)"
lang[f"block.{ns}.safety_switch_100"] = "Fused Safety Switch (industrial LV, 100 A)"
lang[f"block.{ns}.safety_switch_200"] = "Fused Safety Switch (industrial LV, 200 A)"
lang[f"block.{ns}.lis_5kv"] = "Load-Interrupter Switch (MV)"
lang[f"block.{ns}.lis_5kv_fused"] = "Fused Load-Interrupter Switch (MV)"
lang[f"block.{ns}.ats_480"] = "Automatic Transfer Switch (industrial LV, 400 A)"
# the branch cable: armored cable along one axis, joined only at its ends; one model, turned for x and y. Industrial is
# jacketed darker so the two tiers can be told apart.
for cable, jacket in (("branch_cable", "block/mat/mat_galvanized"), ("branch_cable_industrial", "block/mat/mat_oil_dark")):
	save_json(f"{ASSETS}/models/block/{cable}.json", {"parent": "minecraft:block/block", "textures": {"particle": f"{ns}:{jacket}", "side": f"{ns}:{jacket}"},
		"elements": [{"from": [6.5, 6.5, 0], "to": [9.5, 9.5, 16], "faces": {f: {"texture": "#side", "uv": [0, 0, 3, 16] if f in ("north", "south") else [0, 0, 16, 3]}
			for f in ("north", "south", "east", "west", "up", "down")}}]})
	save_json(f"{ASSETS}/blockstates/{cable}.json", {"variants": {"axis=z": {"model": f"{ns}:block/{cable}"},
		"axis=x": {"model": f"{ns}:block/{cable}", "y": 90}, "axis=y": {"model": f"{ns}:block/{cable}", "x": 90}}})
	save_json(f"{ASSETS}/items/{cable}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{cable}"}})
	save_json(f"{DATA}/loot_table/blocks/{cable}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{cable}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
shaped("branch_cable", ["I", "C", "I"], {"I": "minecraft:iron_nugget", "C": f"{ns}:cable_lv"}, "branch_cable", 3)
shaped("branch_cable_industrial", ["I", "C", "I"], {"I": "minecraft:iron_nugget", "C": f"{ns}:cable_industrial"}, "branch_cable_industrial", 3)
lang[f"block.{ns}.branch_cable"] = "Branch Cable (armored, residential)"
lang[f"block.{ns}.branch_cable_industrial"] = "Branch Cable (armored, industrial LV)"
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Assets made by hand, not by this script (Phase 12's lined cell and process water pump): kept in art/hand_assets, as this script clears the
# folders it writes. Anything hand-made goes there, or the next run deletes it.
for _dir, _, _files in os.walk("art/hand_assets"):
	for _f in _files:
		_to = os.path.join(ROOT, os.path.relpath(_dir, "art/hand_assets"), _f)
		os.makedirs(os.path.dirname(_to), exist_ok=True)
		shutil.copy(os.path.join(_dir, _f), _to)

lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.process_water_pump": "Process Water Pump", "block.wildcatter.cell_liner": "Cell Liner", "block.wildcatter.decant_tower": "Decant Tower", "block.wildcatter.settled_tails": "Settled Tails",
	"block.wildcatter.crane_rail": "Crane Rail"})   # the plant-crane-gaps wave's rail, made by hand (this script lost it once: misc-gaps)
save_json(f"{ASSETS}/lang/en_us.json", lang)
for tag, names in {"mineable/pickaxe": ["wildcatter:decant_tower", "wildcatter:process_water_pump", "wildcatter:crane_rail"], "needs_stone_tool": ["wildcatter:crane_rail"]}.items():
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [n for n in names if n not in t["values"]]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)

# ---- Phase 13: PLCs (docs/multiblocks/plc.md) --------------------------------------------------------------------------
# Small wall devices, as plain JSON boxes in the mod's materials and vanilla's coloured blocks: no new textures.
def _box(f, to, tex):
	return {"from": f, "to": to, "faces": {d: {"texture": "#" + tex} for d in ("north", "south", "east", "west", "up", "down")}}

def _model(name, elements, textures):
	save_json(f"{ASSETS}/models/block/{name}.json", {"parent": "minecraft:block/block", "textures": {"particle": textures.get("body", next(iter(textures.values()))), **textures}, "elements": elements})

def _item(name, model):
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{model}"}})

def _flat_item(name, layer):
	save_json(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": layer}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{name}"}})

_GREY = f"{ns}:block/mat/mat_polymer_grey"
_BODY = [_box([4, 4, 12], [12, 12, 16], "body")]                      # the enclosure, on the wall behind it, drawn facing north
_YROT = {"north": 0, "east": 90, "south": 180, "west": 270}

def _device(name, variants):
	"""variants: the lamps value's model elements and textures; the highest given holds for the rest."""
	for lamps, (elements, textures) in enumerate(variants):
		_model(f"{name}_{lamps}", _BODY + elements, {"body": _GREY, **textures})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {
		f"facing={facing},lamps={lamps}": {"model": f"{ns}:block/{name}_{min(lamps, len(variants) - 1)}", "y": rot}
		for facing, rot in _YROT.items() for lamps in range(8)}})
	_item(name, f"{name}_0")
	big_part_loot_simple(name)

def big_part_loot_simple(name):
	save_json(f"{ROOT}/data/{ns}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})

_RED, _REDLIT = "minecraft:block/red_terracotta", "minecraft:block/red_concrete"
_AMB, _AMBLIT = "minecraft:block/yellow_terracotta", "minecraft:block/yellow_concrete"
_GRN, _GRNLIT = "minecraft:block/green_terracotta", "minecraft:block/lime_concrete"
_button = lambda z: [_box([6, 6, z], [10, 10, 12], "cap")]
_device("pushbutton", [(_button(9), {"cap": _GRNLIT}), (_button(11), {"cap": _GRNLIT})])
_device("selector_switch", [([_box([6, 7, 10], [10, 9, 12], "cap")], {"cap": "minecraft:block/black_concrete"}), ([_box([7, 6, 10], [9, 10, 12], "cap")], {"cap": "minecraft:block/black_concrete"})])
_device("estop", [([_box([5, 5, 9], [11, 11, 12], "cap")], {"cap": _REDLIT}), ([_box([5, 5, 7], [11, 11, 12], "cap")], {"cap": _REDLIT})])
_device("pull_cord", [([_box([5, 6, 8], [11, 10, 12], "cap"), _box([-8, 7.5, 9.5], [24, 8.5, 10.5], "cap")], {"cap": _REDLIT}), ([_box([5, 6, 8], [11, 10, 12], "cap"), _box([-8, 6.5, 9.5], [24, 7.5, 10.5], "cap")], {"cap": _RED})])
_device("pilot_light", [(_button(10), {"cap": _RED}), (_button(10), {"cap": _REDLIT})])
_device("horn", [([_box([5, 5, 7], [11, 11, 12], "cap")], {"cap": "minecraft:block/gray_concrete"})])
_device("stack_light", [([_box([7, 2, 12], [9, 4, 16], "body"), _box([6, 11, 9], [10, 13, 13], "r"), _box([6, 8, 9], [10, 10, 13], "a"), _box([6, 5, 9], [10, 7, 13], "g")],
	{"r": _REDLIT if lamps & 1 else _RED, "a": _AMBLIT if lamps & 2 else _AMB, "g": _GRNLIT if lamps & 4 else _GRN}) for lamps in range(8)])

_model("junction_box", [_box([3, 3, 3], [13, 13, 13], "body")], {"body": f"{ns}:block/mat/mat_galvanized"})
save_json(f"{ASSETS}/blockstates/junction_box.json", {"variants": {"": {"model": f"{ns}:block/junction_box"}}})
_item("junction_box", "junction_box")
big_part_loot_simple("junction_box")
for on, lamp in (("false", "minecraft:block/green_terracotta"), ("true", "minecraft:block/lime_concrete")):
	_model(f"control_supply_{on}", [_box([3, 0, 4], [13, 12, 12], "body"), _box([10, 9, 3], [12, 11, 4], "lamp")], {"body": _GREY, "lamp": lamp})
save_json(f"{ASSETS}/blockstates/control_supply.json", {"variants": {"on=false": {"model": f"{ns}:block/control_supply_false"}, "on=true": {"model": f"{ns}:block/control_supply_true"}}})
_item("control_supply", "control_supply_true")
big_part_loot_simple("control_supply")
_RUBBER = f"{ns}:block/mat/mat_rubber"
_model("control_cable_core", [_box([7, 7, 7], [9, 9, 9], "body")], {"body": _RUBBER})
_model("control_cable_arm", [_box([7, 7, 0], [9, 9, 7], "body")], {"body": _RUBBER})
_arm = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
save_json(f"{ASSETS}/blockstates/control_cable.json", {"multipart": [{"apply": {"model": f"{ns}:block/control_cable_core"}}] +
	[{"when": {side: "true"}, "apply": {"model": f"{ns}:block/control_cable_arm", **rot}} for side, rot in _arm.items()]})
save_json(f"{ASSETS}/models/item/control_cable.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/string"}})
save_json(f"{ASSETS}/items/control_cable.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/control_cable"}})
big_part_loot_simple("control_cable")
big_part_loot("plc_cabinet")
big_part_loot("remote_io_drop")
for name, icon in (("cpu_small", "decal_controller"), ("cpu_large", "decal_controller"), ("di16_module", "decal_relays"), ("do16_module", "decal_relays")):
	_flat_item(name, f"{ns}:block/mat/{icon}")
_flat_item("control_key", "minecraft:item/trial_key")
if os.path.exists(f"{ORE_ART}/memory_card.png"):   # the art wave's Ludo icon; vanilla paper's before it
	shutil.copy(f"{ORE_ART}/memory_card.png", f"{ASSETS}/textures/item/ore/memory_card.png")
	_flat_item("memory_card", f"{ns}:item/ore/memory_card")
else:
	_flat_item("memory_card", "minecraft:item/paper")
_flat_item("comm_module", f"{ns}:block/mat/decal_controller")
for name, icon in (("ai8_module", "decal_relays"), ("ao4_module", "decal_relays"), ("rtd4_module", "decal_relays"), ("hsc2_module", "decal_controller")):
	_flat_item(name, f"{ns}:block/mat/{icon}")
# 13e: valves are a body on the pipe's core and arms; an actuator on top says which kind
_STEEL, _BLUE = f"{ns}:block/mat/mat_galvanized", "minecraft:block/blue_concrete"
def _valve(name, pipe, actuator, atex):
	_model(f"{name}_core", [_box([5, 5, 5], [11, 11, 11], "body")] + actuator, {"body": _STEEL, "a": atex})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"multipart": [{"apply": {"model": f"{ns}:block/{name}_core"}}] + [
		{"when": {side: "true"}, "apply": {"model": f"{ns}:block/{pipe}_arm", **rot}} for side, rot in _arm.items()]})
	_item(name, f"{name}_core")
	big_part_loot_simple(name)
_DIAPHRAGM = [_box([7, 11, 7], [9, 12, 9], "body"), _box([4, 12, 4], [12, 15, 12], "a")]   # a spring-and-diaphragm top
_SOLENOID = [_box([6, 11, 6], [10, 15, 10], "a")]                                           # a small air cylinder
_ACTUATOR = [_box([7, 11, 7], [9, 12, 9], "body"), _box([4, 12, 5], [12, 16, 11], "a")]     # a motor gearbox
for suffix, pipe in (("", "pipe"), ("_steel", "gas_line")):
	_valve(f"control_valve{suffix}", pipe, _DIAPHRAGM, _GREY)
	_valve(f"on_off_valve{suffix}", pipe, _SOLENOID, "minecraft:block/yellow_concrete")
	_valve(f"motor_operated_valve{suffix}", pipe, _ACTUATOR, _BLUE)
_TUBE = "minecraft:block/copper_block"
_model("air_tubing_core", [_box([7, 7, 7], [9, 9, 9], "body")], {"body": _TUBE})
_model("air_tubing_arm", [_box([7.5, 7.5, 0], [8.5, 8.5, 7], "body")], {"body": _TUBE})
save_json(f"{ASSETS}/blockstates/air_tubing.json", {"multipart": [{"apply": {"model": f"{ns}:block/air_tubing_core"}}] +
	[{"when": {side: "true"}, "apply": {"model": f"{ns}:block/air_tubing_arm", **rot}} for side, rot in _arm.items()]})
save_json(f"{ASSETS}/models/item/air_tubing.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/copper_ingot"}})
save_json(f"{ASSETS}/items/air_tubing.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/air_tubing"}})
big_part_loot_simple("air_tubing")
def _plain(name, elements, textures):
	_model(name, elements, textures)
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {"": {"model": f"{ns}:block/{name}"}}})
	_item(name, name)
	big_part_loot_simple(name)
# a receiver lying on a skid, the compressor on top; a twin-tower dryer; a caged tote tank
_plain("air_compressor", [_box([0, 0, 1], [16, 2, 15], "s"), _box([1, 2, 3], [15, 9, 13], "r"), _box([4, 9, 4], [12, 15, 12], "c")], {"s": _STEEL, "r": _BLUE, "c": _GREY})
_plain("air_dryer", [_box([1, 0, 1], [15, 2, 15], "s"), _box([2, 2, 4], [7, 15, 12], "t"), _box([9, 2, 4], [14, 15, 12], "t"), _box([7, 11, 7], [9, 13, 9], "s")], {"s": _STEEL, "t": _GREY})
_plain("day_tank", [_box([0, 0, 0], [16, 2, 16], "s"), _box([1, 2, 1], [15, 15, 15], "t"), _box([6, 15, 6], [10, 16, 10], "s")], {"s": _STEEL, "t": "minecraft:block/white_concrete"})
# the transmitter: a clamp behind, a round head with its display
_model("transmitter", [_box([7, 7, 12], [9, 9, 16], "c"), _box([5, 4, 8], [11, 12, 12], "h"), _box([6, 5, 7], [10, 11, 8], "d")], {"c": _STEEL, "h": _GREY, "d": "minecraft:block/light_blue_concrete"})
save_json(f"{ASSETS}/blockstates/transmitter.json", {"variants": {f"facing={facing}": {"model": f"{ns}:block/transmitter", "y": rot} for facing, rot in _YROT.items()}})
_item("transmitter", "transmitter")
big_part_loot_simple("transmitter")
# 13f: Ethernet cable on the pipe's arms, thinner; a touch panel on a door
_BLUECAB = "minecraft:block/blue_wool"
_model("network_cable_core", [_box([7, 7, 7], [9, 9, 9], "body")], {"body": _BLUECAB})
_model("network_cable_arm", [_box([7.5, 7.5, 0], [8.5, 8.5, 7], "body")], {"body": _BLUECAB})
save_json(f"{ASSETS}/blockstates/network_cable.json", {"multipart": [{"apply": {"model": f"{ns}:block/network_cable_core"}}] +
	[{"when": {side: "true"}, "apply": {"model": f"{ns}:block/network_cable_arm", **rot}} for side, rot in _arm.items()]})
save_json(f"{ASSETS}/models/item/network_cable.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/string"}})
save_json(f"{ASSETS}/items/network_cable.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/network_cable"}})
big_part_loot_simple("network_cable")
_model("hmi_panel", [_box([1, 2, 13], [15, 14, 16], "body"), _box([2, 3, 12], [14, 13, 13], "s")], {"body": _GREY, "s": "minecraft:block/light_blue_terracotta"})
save_json(f"{ASSETS}/blockstates/hmi_panel.json", {"variants": {f"facing={facing}": {"model": f"{ns}:block/hmi_panel", "y": rot} for facing, rot in _YROT.items()}})
_item("hmi_panel", "hmi_panel")
big_part_loot_simple("hmi_panel")
_PLC_PARTS = ("pull_cord", "remote_io_drop", "network_cable", "hmi_panel", "control_valve", "on_off_valve", "motor_operated_valve", "control_valve_steel", "on_off_valve_steel", "motor_operated_valve_steel", "air_tubing", "air_compressor", "air_dryer", "day_tank", "transmitter", "control_cable", "junction_box", "control_supply", "plc_cabinet", "pushbutton", "selector_switch", "estop", "pilot_light", "stack_light", "horn")
_PLC_STONE = ("junction_box", "control_supply", "plc_cabinet", "remote_io_drop")
for tag, names in (("mineable/pickaxe", _PLC_PARTS), ("needs_stone_tool", _PLC_STONE)):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in names]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
# proposed recipes (plc.md): real parts where the mod has them
shaped("control_cable", ["WWW", "RRR", "WWW"], {"W": "minecraft:black_wool", "R": "minecraft:redstone"}, "control_cable", 8)
shaped("junction_box", ["SSS", "SRS", "SSS"], {"S": S_, "R": "minecraft:repeater"}, "junction_box")
shaped("control_supply", ["SQS", "RCR", "SSS"], {"S": S_, "Q": "minecraft:quartz", "R": "minecraft:redstone", "C": "minecraft:comparator"}, "control_supply")
shaped("plc_cabinet", ["SSS", "SRS", "SIS"], {"S": S_, "R": "minecraft:repeater", "I": "minecraft:iron_block"}, "plc_cabinet")
shaped("cpu_small", ["QCQ", "RGR", "QCQ"], {"Q": "minecraft:quartz", "C": "minecraft:comparator", "R": "minecraft:redstone", "G": "minecraft:gold_ingot"}, "cpu_small")
shaped("cpu_large", ["DCD", "CPC", "DCD"], {"D": "minecraft:diamond", "C": "minecraft:comparator", "P": f"{ns}:cpu_small"}, "cpu_large")
shaped("di16_module", ["QRQ", "RCR", "QRQ"], {"Q": "minecraft:quartz", "R": "minecraft:redstone", "C": "minecraft:comparator"}, "di16_module")
shaped("do16_module", ["QRQ", "RCR", "QTQ"], {"Q": "minecraft:quartz", "R": "minecraft:redstone", "C": "minecraft:comparator", "T": "minecraft:repeater"}, "do16_module")
shaped("control_key", [" I", "I "], {"I": "minecraft:iron_nugget"}, "control_key")
shaped("ai8_module", ["QRQ", "RCR", "QGQ"], {"Q": "minecraft:quartz", "R": "minecraft:redstone", "C": "minecraft:comparator", "G": "minecraft:gold_nugget"}, "ai8_module")
shaped("ao4_module", ["QGQ", "RCR", "QRQ"], {"Q": "minecraft:quartz", "R": "minecraft:redstone", "C": "minecraft:comparator", "G": "minecraft:gold_nugget"}, "ao4_module")
shaped("rtd4_module", ["QRQ", "GCG", "QRQ"], {"Q": "minecraft:quartz", "R": "minecraft:redstone", "C": "minecraft:comparator", "G": "minecraft:copper_ingot"}, "rtd4_module")
shaped("hsc2_module", ["QTQ", "RCR", "QTQ"], {"Q": "minecraft:quartz", "R": "minecraft:redstone", "C": "minecraft:comparator", "T": "minecraft:repeater"}, "hsc2_module")
shaped("control_valve", ["S", "P"], {"S": S_, "P": f"{ns}:pipe"}, "control_valve")
shaped("on_off_valve", ["R", "P"], {"R": "minecraft:redstone", "P": f"{ns}:pipe"}, "on_off_valve")
shaped("motor_operated_valve", ["C", "P"], {"C": "minecraft:comparator", "P": f"{ns}:pipe"}, "motor_operated_valve")
shaped("control_valve_steel", ["S", "P"], {"S": S_, "P": f"{ns}:gas_line"}, "control_valve_steel")
shaped("on_off_valve_steel", ["R", "P"], {"R": "minecraft:redstone", "P": f"{ns}:gas_line"}, "on_off_valve_steel")
shaped("motor_operated_valve_steel", ["C", "P"], {"C": "minecraft:comparator", "P": f"{ns}:gas_line"}, "motor_operated_valve_steel")
shaped("air_tubing", ["CCC"], {"C": "minecraft:copper_ingot"}, "air_tubing", 8)
shaped("air_compressor", ["SPS", "SIS"], {"S": S_, "P": "minecraft:piston", "I": "minecraft:iron_block"}, "air_compressor")
shaped("air_dryer", ["SSS", "SDS", "SSS"], {"S": S_, "D": "minecraft:sponge"}, "air_dryer")
shaped("day_tank", ["III", "I I", "III"], {"I": "minecraft:iron_ingot"}, "day_tank")
shaped("transmitter", ["G", "C", "S"], {"G": "minecraft:glass_pane", "C": "minecraft:comparator", "S": S_}, "transmitter")
shaped("remote_io_drop", ["SSS", "SCS", "SIS"], {"S": S_, "C": "minecraft:comparator", "I": "minecraft:iron_block"}, "remote_io_drop")
shaped("pull_cord", ["S", "T"], {"S": S_, "T": "minecraft:string"}, "pull_cord")
shaped("comm_module", ["QRQ", "ECE", "QRQ"], {"Q": "minecraft:quartz", "R": "minecraft:redstone", "C": "minecraft:comparator", "E": "minecraft:ender_pearl"}, "comm_module")
shaped("network_cable", ["WWW", "CCC", "WWW"], {"W": "minecraft:blue_wool", "C": "minecraft:copper_ingot"}, "network_cable", 8)
shaped("hmi_panel", ["SGS", "SRS"], {"S": S_, "G": "minecraft:glass_pane", "R": "minecraft:comparator"}, "hmi_panel")
shaped("memory_card", ["Q", "P", "R"], {"Q": "minecraft:quartz", "P": "minecraft:paper", "R": "minecraft:redstone"}, "memory_card")
shaped("pushbutton", ["S", "B"], {"S": S_, "B": "minecraft:stone_button"}, "pushbutton")
shaped("selector_switch", ["S", "L"], {"S": S_, "L": "minecraft:lever"}, "selector_switch")
shaped("estop", ["R", "S", "B"], {"R": "minecraft:red_dye", "S": S_, "B": "minecraft:stone_button"}, "estop")
shaped("pilot_light", ["G", "S"], {"G": "minecraft:glowstone_dust", "S": S_}, "pilot_light")
shaped("stack_light", ["R", "G", "S"], {"R": "minecraft:redstone_lamp", "G": "minecraft:glass_pane", "S": S_}, "stack_light")
shaped("horn", ["N", "S"], {"N": "minecraft:note_block", "S": S_}, "horn")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.control_cable": "Control Cable (multi-core)", "block.wildcatter.junction_box": "Field Junction Box", "block.wildcatter.control_supply": "Control Power Supply (24 V DC)",
	"block.wildcatter.plc_cabinet": "PLC Cabinet", "block.wildcatter.pushbutton": "Pushbutton", "block.wildcatter.selector_switch": "Selector Switch", "block.wildcatter.estop": "E-Stop",
	"block.wildcatter.pilot_light": "Pilot Light", "block.wildcatter.stack_light": "Stack Light", "block.wildcatter.horn": "Horn",
	"item.wildcatter.cpu_small": "CPU Module (small)", "item.wildcatter.cpu_large": "CPU Module (large)", "item.wildcatter.di16_module": "Digital Input Module (16, 24 V DC)",
	"item.wildcatter.do16_module": "Relay Output Module (16, 2 A)", "item.wildcatter.control_key": "Selector Key (HAND / OFF / AUTO)", "item.wildcatter.memory_card": "PLC Memory Card", "item.wildcatter.comm_module": "Communications Module", "block.wildcatter.network_cable": "Network Cable", "block.wildcatter.hmi_panel": "HMI Panel", "block.wildcatter.remote_io_drop": "Remote I/O Drop", "block.wildcatter.pull_cord": "Pull Cord Switch",
	"item.wildcatter.ai8_module": "Analog Input Module (8, 4-20 mA)", "item.wildcatter.ao4_module": "Analog Output Module (4, 4-20 mA)", "item.wildcatter.rtd4_module": "RTD Input Module (4)", "item.wildcatter.hsc2_module": "High-Speed Counter Module (2)",
	"block.wildcatter.control_valve": "Control Valve (air, 2 in water)", "block.wildcatter.on_off_valve": "On-Off Valve (air, 2 in water)", "block.wildcatter.motor_operated_valve": "Motor-Operated Valve (480 V, 2 in water)",
	"block.wildcatter.control_valve_steel": "Control Valve (air, 4 in steel)", "block.wildcatter.on_off_valve_steel": "On-Off Valve (air, 4 in steel)", "block.wildcatter.motor_operated_valve_steel": "Motor-Operated Valve (480 V, 4 in steel)",
	"block.wildcatter.air_tubing": "Instrument Air Tubing", "block.wildcatter.air_compressor": "Instrument Air Compressor", "block.wildcatter.air_dryer": "Air Dryer", "block.wildcatter.day_tank": "Water Day Tank (1 m3)", "block.wildcatter.transmitter": "Transmitter (4-20 mA)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 16: uranium routes and plant operations (docs/ideas-plan.md) --------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/; here loot, names, tags, recipes and the items' placeholders.
def phase16_parts(parts):
	for name in parts:
		big_part_loot(name)
	for t_name in ("mineable/pickaxe", "needs_stone_tool"):
		t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
		wanted = [f"{ns}:{n}" for n in parts]
		t["values"] = [v for v in t["values"] if v not in wanted] + wanted
		save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)


def phase16_items(items):
	lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
	for item, (name, colour) in items.items():
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{item}_placeholder"}})
		save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
		lang[f"item.{ns}.{item}"] = name
	save_json(f"{ASSETS}/lang/en_us.json", lang)


def phase16_names(names):
	lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
	lang.update({f"block.{ns}.{k}": v for k, v in names.items()})
	save_json(f"{ASSETS}/lang/en_us.json", lang)


S_, P_ = f"{ns}:steel_ingot", f"{ns}:gas_line"
# 16a: ADU yellowcake (docs/multiblocks/adu-yellowcake.md)
phase16_parts(("uranium_calciner",))
shaped("uranium_calciner", ["SFS", "BXB", "SPS"], {"S": S_, "F": "minecraft:blast_furnace", "B": "minecraft:bricks", "X": "minecraft:piston", "P": P_}, "uranium_calciner")
phase16_items({"u3o8_drum": ("U3O8 Drum (96 %)", (60, 56, 50))})
phase16_names({"uranium_calciner": "Uranium Calciner (six hearths, 0.6 MW)"})
# 16b1: cathode hydrogen, the ash bed, KOH (docs/multiblocks/conversion-extensions.md)
phase16_parts(("cathode_gas_scrubber", "koh_scrubber"))
shaped("cathode_gas_scrubber", ["SPS", "GCG", "SSS"], {"S": S_, "P": P_, "G": "minecraft:glass", "C": "minecraft:copper_ingot"}, "cathode_gas_scrubber")
shaped("koh_scrubber", ["SPS", "SCS", "SXS"], {"S": S_, "P": P_, "C": "minecraft:cauldron", "X": "minecraft:piston"}, "koh_scrubber")
phase16_items({"fluorinator_ash_drum": ("Fluorinator Ash Drum", (120, 110, 100)), "synthetic_fluorspar": ("Synthetic Fluorspar (1 t bag)", (200, 190, 220))})
phase16_parts(("cascade_cooling_skid",))
shaped("cascade_cooling_skid", ["SPS", "XWX", "SSS"], {"S": S_, "P": P_, "X": "minecraft:piston", "W": "minecraft:water_bucket"}, "cascade_cooling_skid")
phase16_names({"cascade_cooling_skid": "Cascade Cooling Skid (55 kW)"})
# 16d1: ISR CO2, the bleed, satellites (docs/multiblocks/isr-operations.md)
phase16_parts(("co2_liquefier", "co2_storage_tank", "resin_trailer"))
shaped("co2_liquefier", ["SPS", "XIX", "SGS"], {"S": S_, "P": P_, "X": "minecraft:piston", "I": "minecraft:iron_block", "G": "minecraft:glass"}, "co2_liquefier")
shaped("co2_storage_tank", ["SSS", "W W", "SPS"], {"S": S_, "W": "minecraft:white_wool", "P": P_}, "co2_storage_tank")
shaped("resin_trailer", ["SBS", "S S", "C C"], {"S": S_, "B": "minecraft:barrel", "C": "minecraft:minecart"}, "resin_trailer")
phase16_names({"co2_liquefier": "CO2 Liquefier (150 kg/h, 75 kW)", "co2_storage_tank": "CO2 Storage Tank (30 t)", "resin_trailer": "Resin Trailer (10 m3 of IX resin)"})
phase16_names({"cathode_gas_scrubber": "Cathode Gas Scrubber (NaF beds, 10 kW)", "koh_scrubber": "KOH Scrubber (with lime regeneration, 15 kW)"})
# 17c1: zirconium scrap and hafnium (docs/multiblocks/zirconium-extensions.md)
phase16_items({"zircaloy_scrap": ("Zircaloy Scrap (25 kg)", (150, 150, 140)), "hfcl4_drum": ("HfCl4 Drum (50 kg of Hf)", (200, 196, 180)),
	"hafnium_rodlet": ("Hafnium Rodlet", (120, 124, 132))})
# 19d: hafnium RCCAs and sodium sulfate bags (docs/multiblocks/reactor-extensions.md)
phase16_items({"hafnium_rcca": ("Hafnium RCCA", (110, 116, 126)), "sodium_sulfate_bag": ("Sodium Sulfate (1 t bag)", (236, 232, 220))})
save_json(f"{ROOT}/data/c/tags/item/sodium_sulfate.json", {"replace": False, "values": [f"{ns}:sodium_sulfate_bag"]})
# 17c2: fumed silica, Pidgeon magnesium, chromium (docs/multiblocks/zirconium-extensions.md)
phase16_parts(("fumed_silica_burner", "pidgeon_furnace", "chromate_kiln"))
shaped("fumed_silica_burner", ["SPS", "GFG", "SBS"], {"S": S_, "P": P_, "G": "minecraft:glass", "F": "minecraft:blast_furnace", "B": "minecraft:bucket"}, "fumed_silica_burner")
shaped("pidgeon_furnace", ["BPB", "FXF", "BSB"], {"B": "minecraft:bricks", "P": P_, "F": "minecraft:blast_furnace", "X": "minecraft:piston", "S": S_}, "pidgeon_furnace")
shaped("chromate_kiln", ["SPS", "BFB", "SXS"], {"S": S_, "P": P_, "B": "minecraft:bricks", "F": "minecraft:blast_furnace", "X": "minecraft:cauldron"}, "chromate_kiln")
phase16_names({"fumed_silica_burner": "Fumed Silica Burner (15 kW)", "pidgeon_furnace": "Pidgeon Furnace (retorts, 0.5 MW of gas)", "chromate_kiln": "Chromate Kiln (0.3 MW of gas)"})
phase16_items({"fumed_silica": ("Fumed Silica (25 kg sack)", (236, 238, 240)), "dolime_bag": ("Dolime (1 t bag)", (222, 218, 206)), "magnesium_ingot": ("Magnesium Ingot", (196, 200, 204)),
	"chromic_oxide": ("Chromic Oxide (25 kg sack)", (70, 120, 60)), "chromium_metal": ("Chromium Metal (10 kg)", (180, 186, 196))})
save_json(f"{ROOT}/data/c/tags/item/fumed_silica.json", {"replace": False, "values": [f"{ns}:fumed_silica"]})
# 16c2: crashes, sampling, heels (docs/multiblocks/enrichment-operations.md); crashed_centrifuge's loot (4 steel and a rotor shard) is written by hand
phase16_parts(("sampling_autoclave",))
shaped("sampling_autoclave", ["SPS", "SCS", "SXS"], {"S": S_, "P": P_, "C": "minecraft:cauldron", "X": "minecraft:furnace"}, "sampling_autoclave")
phase16_names({"sampling_autoclave": "Sampling Autoclave (50 kW)", "crashed_centrifuge": "Crashed Centrifuge"})
phase16_items({"rotor_shard": ("Rotor Shard (carbon fibre)", (40, 40, 44))})
# ---- Phase 17a: ferroalloys (docs/multiblocks/ferroalloys.md) ---------------------------------------------------------------------
# blockstates, models, shapes and items come from tools/models/ferroalloys.py; here loot, names, tags, recipes and the bags. Each bag's
# icon is the Ludo bulk bag with a swatch of what it holds drawn on it by this script: a placeholder until it has art of its own.
FERRO_PARTS = ("furnace_transformer", "submerged_arc_furnace", "capacitor_bank", "moly_roaster", "aluminothermic_vessel")
for name in FERRO_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in FERRO_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("furnace_transformer", ["SCS", "CIC", "SCS"], {"S": S_, "C": "minecraft:copper_block", "I": "minecraft:iron_block"}, "furnace_transformer")
shaped("submerged_arc_furnace", ["SHS", "BFB", "SBS"], {"S": S_, "H": "minecraft:hopper", "B": "minecraft:bricks", "F": "minecraft:blast_furnace"}, "submerged_arc_furnace")
shaped("capacitor_bank", ["CCC", "SRS", "SIS"], {"C": "minecraft:copper_ingot", "S": S_, "R": "minecraft:redstone_block", "I": "minecraft:iron_bars"}, "capacitor_bank")
shaped("moly_roaster", ["SBS", "SFS", "SBS"], {"S": S_, "B": "minecraft:bricks", "F": "minecraft:furnace"}, "moly_roaster")
shaped("aluminothermic_vessel", ["SHS", "BCB", "SSS"], {"S": S_, "H": "minecraft:hopper", "B": "minecraft:bricks", "C": "minecraft:cauldron"}, "aluminothermic_vessel")
FERRO_BAGS = {"ferrochrome_bag": ("Bulk Bag of HC Ferrochrome", (150, 154, 160)), "ferromanganese_bag": ("Bulk Bag of HC Ferromanganese", (120, 112, 104)),
	"ferrosilicon_bag": ("Bulk Bag of Ferrosilicon 75", (170, 176, 186)), "moly_oxide_bag": ("Bulk Bag of Technical Moly Oxide", (196, 206, 150)),
	"scheelite_concentrate_bag": ("Bulk Bag of Scheelite Concentrate", (226, 214, 170)), "ferromolybdenum_bag": ("Bulk Bag of Ferromolybdenum", (140, 146, 156)),
	"ferrotungsten_bag": ("Bulk Bag of Ferrotungsten", (100, 104, 112))}
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in FERRO_BAGS.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		bag = Image.open(f"{ORE_ART}/empty_bulk_bag.png").convert("RGBA")
		d = ImageDraw.Draw(bag)
		d.rectangle([10, 13, 21, 22], fill=colour + (255,), outline=(60, 60, 60, 255))   # a swatch of what it holds
		d.rectangle([12, 15, 14, 17], fill=tuple(min(255, c + 40) for c in colour) + (255,))
		bag.save(f"{ASSETS}/textures/item/{item}_placeholder.png")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.furnace_transformer": "Furnace Transformer (10 MVA, 150 V)", f"block.{ns}.submerged_arc_furnace": "Submerged Arc Furnace (8 MW)",
	f"block.{ns}.capacitor_bank": "Capacitor Bank (3 Mvar)", f"block.{ns}.moly_roaster": "Moly Roaster (multiple hearth)",
	f"block.{ns}.aluminothermic_vessel": "Aluminothermic Vessel"})
save_json(f"{ASSETS}/lang/en_us.json", lang)
# the alloys for other mods: a tag of them all, and the common tags another mod looks for
save_json(f"{DATA}/tags/item/ferroalloys.json", {"values": [f"{ns}:{b}" for b in ("ferrochrome_bag", "ferromanganese_bag", "ferrosilicon_bag", "ferromolybdenum_bag", "ferrotungsten_bag")]})
for metal in ("ferrochrome", "ferromanganese", "ferrosilicon", "ferromolybdenum", "ferrotungsten"):
	save_json(f"{ROOT}/data/c/tags/item/ferroalloys/{metal}.json", {"values": [f"{ns}:{metal}_bag"]})

# ---- Phase 18: fertilizer and uranium from phosphoric acid (docs/multiblocks/fertilizer.md, uranium-from-phosphate.md) ----------
# blockstates, models, shapes and items come from tools/models/phosphate.py; here loot, names, tags, recipes and the items. Drawn
# last, so the placeholders before it keep their noise.
PHOSPHATE_PARTS = ("phosphate_mill", "phosphoric_reactor", "tilting_pan_filter", "acid_evaporator", "granulation_plant", "potash_dissolver",
	"vacuum_crystallizer", "potash_dryer", "acid_pretreatment", "depa_topo_circuit")
for name in PHOSPHATE_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in PHOSPHATE_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("phosphate_mill", ["SHS", "IXI", "SSS"], {"S": S_, "H": "minecraft:hopper", "I": "minecraft:iron_block", "X": "minecraft:piston"}, "phosphate_mill")
shaped("phosphoric_reactor", ["SXS", "CPC", "CCC"], {"S": S_, "X": "minecraft:piston", "C": "minecraft:smooth_stone", "P": P_}, "phosphoric_reactor")
shaped("tilting_pan_filter", ["SLS", "PXP", "SIS"], {"S": S_, "L": "minecraft:paper", "P": P_, "X": "minecraft:piston", "I": "minecraft:iron_block"}, "tilting_pan_filter")
shaped("acid_evaporator", ["SPS", "SCS", "SXS"], {"S": S_, "P": P_, "C": "minecraft:copper_block", "X": "minecraft:piston"}, "acid_evaporator")
shaped("granulation_plant", ["SPS", "IFI", "SXS"], {"S": S_, "P": P_, "I": "minecraft:iron_block", "F": "minecraft:blast_furnace", "X": "minecraft:piston"}, "granulation_plant")
shaped("potash_dissolver", ["SHS", "SXS", "SPS"], {"S": S_, "H": "minecraft:hopper", "X": "minecraft:piston", "P": P_}, "potash_dissolver")
shaped("vacuum_crystallizer", ["SPS", "S S", "SXS"], {"S": S_, "P": P_, "X": "minecraft:piston"}, "vacuum_crystallizer")
shaped("potash_dryer", ["SPS", "BFB", "SXS"], {"S": S_, "P": P_, "B": "minecraft:bricks", "F": "minecraft:furnace", "X": "minecraft:piston"}, "potash_dryer")
shaped("acid_pretreatment", ["SPS", "SXS", "SSS"], {"S": S_, "P": P_, "X": "minecraft:piston"}, "acid_pretreatment")
shaped("depa_topo_circuit", ["PXP", "SSS"], {"S": S_, "P": P_, "X": "minecraft:piston"}, "depa_topo_circuit")
# the extractant's stand-in (uranium-from-phosphate.md): phosphorus from the rock, an organic from diesel, iron, in a drum
recipe = {"type": "minecraft:crafting_shapeless", "category": "misc",
	"ingredients": [f"{ns}:empty_drum", f"{ns}:phosphate", f"{ns}:diesel_bucket", "minecraft:iron_ingot"], "result": {"id": f"{ns}:extractant_drum", "count": 1}}
save_json(f"{DATA}/recipe/extractant_drum.json", recipe)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"tsp_bag": ("Bulk Bag of TSP (0-46-0)", (196, 196, 180)), "dap_bag": ("Bulk Bag of DAP (18-46-0)", (170, 150, 120)),
		"mop_bag": ("Bulk Bag of Muriate of Potash (0-0-60)", (200, 120, 110)), "extractant_drum": ("Extractant Drum (DEPA-TOPO)", (60, 90, 130))}.items():
	tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{item}_placeholder"}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.phosphate_mill": "Phosphate Mill (wet ball mill, 200 kW)", f"block.{ns}.phosphoric_reactor": "Phosphoric Acid Reactor (dihydrate)",
	f"block.{ns}.tilting_pan_filter": "Tilting Pan Filter", f"block.{ns}.acid_evaporator": "Phosphoric Acid Evaporator",
	f"block.{ns}.granulation_plant": "Granulation Plant (TSP or DAP)", f"block.{ns}.potash_dissolver": "Potash Dissolver",
	f"block.{ns}.vacuum_crystallizer": "Vacuum Crystallizer", f"block.{ns}.potash_dryer": "Potash Dryer and Compactor",
	f"block.{ns}.acid_pretreatment": "Acid Pretreatment (uranium)", f"block.{ns}.depa_topo_circuit": "DEPA-TOPO Circuit"})
save_json(f"{ASSETS}/lang/en_us.json", lang)
# the convention tag another mod's crops can read; Wildcatter itself gives the bags no use on crops (fertilizer.md)
save_json(f"{ROOT}/data/c/tags/item/fertilizers.json", {"replace": False, "values": [f"{ns}:tsp_bag", f"{ns}:dap_bag", f"{ns}:mop_bag"]})

# ---- Phase 19a: containment and protection (docs/multiblocks/reactor-extensions.md) --------------------------------------
# The big parts' blockstates, models and shapes come from tools/models/reactor_extensions.py; the concrete and the igniter are drawn
# in the materials already in art/final/mat (no new textures).
P19A_PARTS = ("equipment_hatch", "personnel_airlock", "hydrogen_recombiner", "containment_monitor", "pressurizer_relief_tank")
for name in P19A_PARTS:
	big_part_loot(name)
for name in ("containment_concrete", "hydrogen_igniter"):
	big_part_loot_simple(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in P19A_PARTS + ("containment_concrete", "hydrogen_igniter")]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
block_model("containment_concrete", "minecraft:block/cube_all", {"all": f"{ns}:block/mat/mat_concrete"})
save_json(f"{ASSETS}/blockstates/containment_concrete.json", {"variants": {"": {"model": f"{ns}:block/containment_concrete"}}})
save_json(f"{ASSETS}/items/containment_concrete.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/containment_concrete"}})
for lit, glow in (("", f"{ns}:block/mat/mat_galvanized"), ("_lit", "minecraft:block/orange_concrete")):
	save_json(f"{ASSETS}/models/block/hydrogen_igniter{lit}.json", {"parent": "minecraft:block/block", "textures": {"body": f"{ns}:block/mat/mat_galvanized", "tip": glow,
		"particle": f"{ns}:block/mat/mat_galvanized"}, "elements": [
		{"from": [5, 5, 5], "to": [11, 11, 11], "faces": {f: {"texture": "#body"} for f in ("north", "south", "east", "west", "up", "down")}},
		{"from": [7, 7, 11], "to": [9, 9, 14], "faces": {f: {"texture": "#tip"} for f in ("north", "south", "east", "west", "up", "down")}}]})
save_json(f"{ASSETS}/blockstates/hydrogen_igniter.json", {"variants": {"lit=false": {"model": f"{ns}:block/hydrogen_igniter"}, "lit=true": {"model": f"{ns}:block/hydrogen_igniter_lit"}}})
save_json(f"{ASSETS}/items/hydrogen_igniter.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/hydrogen_igniter"}})
shaped("containment_concrete", ["CCC", "CSC", "CCC"], {"C": "minecraft:gray_concrete", "S": f"{ns}:steel_ingot"}, "containment_concrete", 8)
shaped("hydrogen_igniter", [" R ", "SCS"], {"R": "minecraft:redstone", "S": f"{ns}:steel_ingot", "C": "minecraft:copper_ingot"}, "hydrogen_igniter", 2)
shaped("equipment_hatch", ["SSS", "SIS", "SSS"], {"S": f"{ns}:steel_ingot", "I": "minecraft:iron_block"}, "equipment_hatch")
shaped("personnel_airlock", ["SDS", "S S", "SDS"], {"S": f"{ns}:steel_ingot", "D": "minecraft:iron_door"}, "personnel_airlock")
shaped("hydrogen_recombiner", ["SBS", "SGS", "SBS"], {"S": f"{ns}:steel_ingot", "B": "minecraft:iron_bars", "G": "minecraft:gold_ingot"}, "hydrogen_recombiner")
shaped("containment_monitor", ["GCG", "SRS", "SSS"], {"G": "minecraft:glass_pane", "C": "minecraft:comparator", "R": "minecraft:redstone", "S": f"{ns}:steel_ingot"}, "containment_monitor")
shaped("pressurizer_relief_tank", ["SPS", "SBS", "SPS"], {"S": f"{ns}:steel_ingot", "P": f"{ns}:gas_line", "B": "minecraft:bucket"}, "pressurizer_relief_tank")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({f"block.{ns}.containment_concrete": "Containment Concrete (1 m, steel-lined)", f"block.{ns}.hydrogen_igniter": "Hydrogen Igniter (glow plug)",
	f"block.{ns}.equipment_hatch": "Equipment Hatch", f"block.{ns}.personnel_airlock": "Personnel Airlock", f"block.{ns}.hydrogen_recombiner": "Hydrogen Recombiner (passive)",
	f"block.{ns}.containment_monitor": "Containment Monitor", f"block.{ns}.pressurizer_relief_tank": "Pressurizer Relief Tank (30 m3)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 19b: dry casks, contamination, fresh-fuel inspection (docs/multiblocks/reactor-extensions.md) --------------------------
P19B_PARTS = ("cask_loading_station", "storage_module", "new_fuel_inspection_stand", "personnel_monitor")
for name in P19B_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in P19B_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("cask_loading_station", ["SHS", "BWB", "SSS"], {"S": f"{ns}:steel_ingot", "H": "minecraft:hopper", "B": "minecraft:iron_block", "W": "minecraft:anvil"}, "cask_loading_station")
shaped("storage_module", ["CCC", "C C", "CCC"], {"C": f"{ns}:containment_concrete"}, "storage_module")
shaped("new_fuel_inspection_stand", ["SGS", "S S", "SLS"], {"S": f"{ns}:steel_ingot", "G": "minecraft:glass_pane", "L": "minecraft:redstone_lamp"}, "new_fuel_inspection_stand")
shaped("personnel_monitor", ["SGS", "SRS", "SBS"], {"S": f"{ns}:steel_ingot", "G": "minecraft:glass_pane", "R": "minecraft:comparator", "B": "minecraft:bucket"}, "personnel_monitor")
shaped("dry_storage_canister", ["SSS", "S S", "SSS"], {"S": f"{ns}:steel_ingot"}, "dry_storage_canister")
shaped("decon_washer", [" P ", "SXS", " B "], {"P": f"{ns}:pipe", "S": f"{ns}:steel_ingot", "X": "minecraft:piston", "B": "minecraft:bucket"}, "decon_washer")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for item, (name, colour) in {"dry_storage_canister": ("Dry Storage Canister (empty, 12 assemblies)", (150, 156, 162)),
		"decon_washer": ("Decontamination Washer (2 kW)", (210, 180, 40))}.items():
	tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{item}_placeholder"}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
lang.update({f"block.{ns}.cask_loading_station": "Cask Loading Station", f"block.{ns}.storage_module": "Dry Cask Storage Module (horizontal)",
	f"block.{ns}.new_fuel_inspection_stand": "New-Fuel Inspection Stand", f"block.{ns}.personnel_monitor": "Personnel Contamination Monitor"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 15b1: the drilling mud plant and the rig's mixing hopper (docs/multiblocks/drilling-mud.md) ------------------------------
# blockstates, models, shapes and items come from tools/models/mud.py; here loot, tags, recipes, names and the two bags. Last before
# the models, so its placeholders shift no other's noise.
MUD_PARTS = ("barite_mill", "bentonite_dryer", "mud_hopper", "bop_stack", "bop_accumulator")
for name in MUD_PARTS:
	big_part_loot(name)
for t_name in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{t_name}.json"))
	wanted = [f"{ns}:{n}" for n in MUD_PARTS]
	t["values"] = [v for v in t["values"] if v not in wanted] + wanted
	save_json(f"{ROOT}/data/minecraft/tags/block/{t_name}.json", t)
shaped("barite_mill", ["SHS", "XCX", "SFS"], {"S": S_, "H": "minecraft:hopper", "X": "minecraft:piston", "C": f"{ns}:crankshaft", "F": "minecraft:white_wool"}, "barite_mill")
shaped("bentonite_dryer", ["SHS", "BPB", "SXS"], {"S": S_, "H": "minecraft:hopper", "B": "minecraft:bricks", "P": P_, "X": "minecraft:blast_furnace"}, "bentonite_dryer")
shaped("mud_hopper", [" H ", "SPS", "SXS"], {"S": S_, "H": "minecraft:hopper", "P": P_, "X": "minecraft:piston"}, "mud_hopper")
shaped("bop_stack", ["SXS", "XIX", "SPS"], {"S": S_, "X": "minecraft:piston", "I": "minecraft:iron_block", "P": P_}, "bop_stack")
shaped("bop_accumulator", ["BBB", "BPB", "SXS"], {"B": "minecraft:bucket", "P": P_, "S": S_, "X": "minecraft:piston"}, "bop_accumulator")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({f"block.{ns}.barite_mill": "Barite Mill (roller, 5 t/h)", f"block.{ns}.bentonite_dryer": "Bentonite Dryer (rotary, 3 t/h)",
	f"block.{ns}.mud_hopper": "Mud Hopper (jet mixing)", f"block.{ns}.bop_stack": "BOP Stack (13-5/8 in, 5,000 psi)",
	f"block.{ns}.bop_accumulator": "BOP Accumulator Unit"})
for item, (name, colour) in {"barite_bag": ("Bulk Bag of Barite (API 13A 4.1)", (200, 196, 190)), "bentonite_bag": ("Bulk Bag of Bentonite (API 13A)", (205, 190, 150))}.items():
	if os.path.exists(f"{ORE_ART}/{item}.png"):
		shutil.copy(f"{ORE_ART}/{item}.png", f"{ASSETS}/textures/item/ore/{item}.png")
		item_tex = f"{ns}:item/ore/{item}"
	else:
		tex(f"{item}_placeholder", lambda img, d, c=colour: noise(img, c, 10), "item")
		item_tex = f"{ns}:item/{item}_placeholder"
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": item_tex}})
	save_json(f"{ASSETS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{item}"}})
	lang[f"item.{ns}.{item}"] = name
save_json(f"{ASSETS}/lang/en_us.json", lang)


# ---- Tim's answers of 2026-11-10 (branch w-grid; docs/questions/answers-grid.md) ------------------------------------------------
# The 600 mm slurry pipe (Tailings p7 #27) and flotation air header (p7 #36): the pipe's core and arms at 0.61 m OD with a 0.75 m flange,
# in the mod's materials. Rubber-lined steel slurry pipe painted black; the air header galvanized.
_GALV = f"{ns}:block/mat/mat_galvanized"
for _name, _tex in (("slurry_pipe", _RUBBER), ("air_header", _GALV)):
	_pt = {"pipe": _tex, "particle": _tex}
	_core, _armel = pipe_shapes(0.61, 0.75)
	save_json(f"{ASSETS}/models/block/{_name}_core.json", {"textures": _pt, "elements": _core})
	save_json(f"{ASSETS}/models/block/{_name}_arm.json", {"textures": _pt, "elements": _armel})
	save_json(f"{ASSETS}/models/block/{_name}_inventory.json", {"parent": "minecraft:block/block",
		"display": {"gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]}, "ground": {"scale": [0.5, 0.5, 0.5]},
			"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.5, 0.5, 0.5]}, "thirdperson_righthand": {"scale": [0.5, 0.5, 0.5]}},
		"textures": _pt, "elements": [cuboid([0, 3, 3], [16, 13, 13], "#pipe"), cuboid([2, 2, 2], [14, 14, 14], "#pipe")]})
	save_json(f"{ASSETS}/blockstates/{_name}.json", {"multipart": [{"apply": {"model": f"{ns}:block/{_name}_core"}}] +
		[{"when": {side: "true"}, "apply": {"model": f"{ns}:block/{_name}_arm", **rot}} for side, rot in _arm.items()]})
	_item(_name, f"{_name}_inventory")
	big_part_loot_simple(_name)
shaped("slurry_pipe", ["SSS", "K K", "SSS"], {"S": S_, "K": "minecraft:dried_kelp_block"}, "slurry_pipe", 4)   # the lining
shaped("air_header", ["S S", "S S", "S S"], {"S": S_}, "air_header", 4)
# the guy anchor (protection-gaps 20): a screw anchor's rod and eye standing out of the ground, a bolt plate under it
_model("guy_anchor", [_box([5, 0, 5], [11, 1, 11], "body"), _box([7.4, 0, 7.4], [8.6, 4, 8.6], "body"), _box([7, 4, 6.5], [9, 6, 9.5], "body")], {"body": _GALV})
save_json(f"{ASSETS}/blockstates/guy_anchor.json", {"variants": {"": {"model": f"{ns}:block/guy_anchor"}}})
_item("guy_anchor", "guy_anchor")
big_part_loot_simple("guy_anchor")
shaped("guy_anchor", [" I ", " S ", "SSS"], {"I": "minecraft:iron_ingot", "S": S_}, "guy_anchor", 2)
# the guy strand: a coil of galvanized 7-wire strand (placeholder icon, drawn here)
def _coil(img, d):
	for r, c in ((6, (150, 156, 162)), (5, (110, 116, 122)), (4, (170, 176, 182))):
		d.ellipse([8 - r, 8 - r, 7 + r, 7 + r], outline=c + (255,))
tex("guy_strand_placeholder", _coil, "item")
_flat_item("guy_strand", f"{ns}:item/guy_strand_placeholder")
shaped("guy_strand", ["NSN", "S S", "NSN"], {"N": "minecraft:iron_nugget", "S": S_}, "guy_strand")
# the chlorinator's zircon feed (answer 9): empty here; Tailings (or a data pack) adds its zircon concentrate and a kg file (block/ZirconFeed)
save_json(f"{DATA}/tags/item/zircon_feed.json", {"values": []})
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.slurry_pipe": "Slurry Pipe (600 mm)", "block.wildcatter.air_header": "Air Header (600 mm)",
	"block.wildcatter.guy_anchor": "Guy Anchor", "item.wildcatter.guy_strand": "Guy Strand (3/8 in EHS)"})
save_json(f"{ASSETS}/lang/en_us.json", lang)
for tag in ("mineable/pickaxe",):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in ("slurry_pipe", "air_header", "guy_anchor") if f"{ns}:{n}" not in t["values"]]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)

# ---- Phase 4.5: the true-size part models (tools/models/), drawn in the materials in art/final/mat ----------------------
import subprocess, sys
subprocess.run([sys.executable, "tools/models/build.py"], check=True)

# ---- Placeholders the Ludo art has replaced stay out of the jar --------------------------------------------------------
# A placeholder is drawn before this script knows whether its art exists (and some are drawn only to keep the noise sequence);
# any *_placeholder texture no model, blockstate or Java class names is deleted here.
import glob, re
_named = set()
for _f in glob.glob(f"{ASSETS}/**/*.json", recursive=True) + glob.glob("common/src/main/java/**/*.java", recursive=True):
	_named.update(re.findall(r"[a-z0-9_/]+_placeholder", open(_f).read()))
_gone = 0
for _png in glob.glob(f"{ASSETS}/textures/**/*_placeholder.png", recursive=True):
	_name = os.path.relpath(_png, f"{ASSETS}/textures")[:-4]   # e.g. block/rotor_placeholder
	if not any(n.endswith(_name) or n == os.path.basename(_name) for n in _named):
		os.remove(_png)
		_gone += 1
print(f"{_gone} replaced placeholders left out")
