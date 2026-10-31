#!/usr/bin/env python3
"""Draws the placeholder textures and writes the model/blockstate/lang/recipe/loot JSON.

Placeholders only: real art comes from Ludo once the shapes settle. Run from the repo root: python3 tools/make_assets.py
"""
import json, os, random, shutil
from PIL import Image, ImageDraw

ROOT = "src/main/resources"
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
tex("mv_breaker_body", tinted_breaker((60, 90, 170)))
tex("dist_breaker_body", tinted_breaker((60, 150, 90)))
tex("hv_breaker_body", tinted_breaker((170, 60, 60)))


def wood(base, grain):
	def draw(img, d):
		noise(img, base, 8)
		for x in (2, 5, 9, 12):
			d.line([x, 0, x, 15], fill=grain)
	return draw


tex("pole", wood((110, 78, 48), (80, 55, 34)))
tex("insulator", plain((200, 200, 210)))
tex("breaker_lever_on", lever((60, 190, 90)))
tex("breaker_lever_off", lever((120, 120, 126)))
tex("breaker_lever_tripped", lever((220, 60, 50)))
for name, t in (("transformer", ((96, 102, 112), (225, 225, 215))), ("substation", ((70, 90, 130), (200, 215, 240))), ("distribution", ((60, 115, 85), (205, 235, 215))), ("pole_tx", ((112, 118, 124), (232, 232, 224)))):
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
		kind = "item" if f in ("power_meter.png", "field_manual.png", "seismograph.png", "crude_oil_bucket.png", "plastic_sheet.png", "diesel_bucket.png", "steel_ingot.png",
		"engine_block_casting.png", "piston.png", "crankshaft.png", "cylinder_head.png", "fuel_injector.png", "spark_plug.png", "stator_winding.png", "rotor_field.png", "exciter.png") else "block"
		shutil.copy(f"art/final/{f}", f"{ASSETS}/textures/{kind}/{f}")

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
for cube in ("drilling_rig", "pumpjack", "gas_wellhead", "casing", "refinery", "refinery_column", "cooling_tower", "cooling_fill"):
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

for tname, t in (("transformer", "transformer"), ("substation_transformer", "substation"), ("pole_transformer", "pole_tx")):
	save_json(f"{ASSETS}/models/block/{tname}.json", {"parent": "minecraft:block/orientable", "textures": {
		"front": f"{ns}:block/{t}_front", "side": f"{ns}:block/{t}_side", "top": f"{ns}:block/{t}_top", "bottom": f"{ns}:block/{t}_top"}})
	save_json(f"{ASSETS}/blockstates/{tname}.json", {"variants": {
		"facing=north": {"model": f"{ns}:block/{tname}"}, "facing=east": {"model": f"{ns}:block/{tname}", "y": 90},
		"facing=south": {"model": f"{ns}:block/{tname}", "y": 180}, "facing=west": {"model": f"{ns}:block/{tname}", "y": 270}}})


def cuboid(frm, to, texture="#cable"):
	faces = {f: {"uv": [0, 0, 16, 16], "texture": texture} for f in ("north", "south", "east", "west", "up", "down")}
	return {"from": frm, "to": to, "faces": faces}


# The breaker: a box with a lever. On leans one way, off the other, tripped sits in the middle. The 4.16 kV and
# 138 kV ones are bigger (a wider, taller body and a longer lever) and tinted blue and red.
for prefix, wide in (("", 0), ("mv_", 1), ("dist_", 1), ("hv_", 2)):
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
save_json(f"{ASSETS}/blockstates/ufls_relay_dist.json", {"variants": {
	f"facing={facing},position={position}": {"model": f"{ns}:block/relay_{'on' if position == 'on' else 'off'}", "y": y}
	for facing, y in rotations.items() for position in ("on", "off", "tripped")}})
# the fused cutout: a porcelain body with a fuse tube hanging in a hinged holder; when the link blows the tube drops open
for state, angle in (("good", 0), ("blown", -75)):
	tube = cuboid([6.5, 1, 7], [9.5, 15, 9], "#tube")
	tube["rotation"] = {"origin": [8, 14, 8], "axis": "x", "angle": angle}
	save_json(f"{ASSETS}/models/block/cutout_{state}.json", {
		"textures": {"body": f"{ns}:block/cutout_body", "tube": f"{ns}:block/cutout_tube", "particle": f"{ns}:block/cutout_body"},
		"elements": [cuboid([5, 0, 3], [11, 16, 6], "#body"), cuboid([6, 13, 6], [10, 16, 10], "#body"), tube]})
save_json(f"{ASSETS}/blockstates/fused_cutout.json", {"variants": {
	f"blown={blown},facing={facing}": {"model": f"{ns}:block/cutout_{'blown' if blown == 'true' else 'good'}", "y": y}
	for facing, y in rotations.items() for blown in ("false", "true")}})
save_json(f"{ASSETS}/blockstates/ufls_relay.json", {"variants": {
	f"facing={facing},position={position}": {"model": f"{ns}:block/relay_{'on' if position == 'on' else 'off'}", "y": y}
	for facing, y in rotations.items() for position in ("on", "off", "tripped")}})
save_json(f"{ASSETS}/blockstates/fuse.json", {"variants": {
	f"blown={blown},facing={facing}": {"model": f"{ns}:block/fuse_{'blown' if blown == 'true' else 'good'}", "y": y}
	for facing, y in rotations.items() for blown in ("false", "true")}})
for prefix in ("", "mv_", "dist_", "hv_"):
	save_json(f"{ASSETS}/blockstates/{prefix}breaker.json", {"variants": {
		f"facing={facing},position={position}": {"model": f"{ns}:block/{prefix}breaker_{position}", "y": y}
		for facing, y in rotations.items() for position in ("on", "off", "tripped")}})

# the pole: a thin post. The crossarm: a beam with two insulators.
save_json(f"{ASSETS}/blockstates/pole.json", {"variants": {"": {"model": f"{ns}:block/pole"}}})
save_json(f"{ASSETS}/models/block/pole.json", {"textures": {"wood": f"{ns}:block/pole", "particle": f"{ns}:block/pole"},
	"elements": [cuboid([5, 0, 5], [11, 16, 11], "#wood")]})
for axis, (lo, hi) in {"x": ([0, 12, 6], [16, 16, 10]), "z": ([6, 12, 0], [10, 16, 16])}.items():
	save_json(f"{ASSETS}/models/block/crossarm_{axis}.json", {
		"textures": {"wood": f"{ns}:block/pole", "particle": f"{ns}:block/pole"},
		"elements": [cuboid([5, 0, 5], [11, 12, 11], "#wood"), cuboid(lo, hi, "#wood")]})   # a short post under the beam joins it to the pole
save_json(f"{ASSETS}/blockstates/crossarm.json", {"variants": {"axis=x": {"model": f"{ns}:block/crossarm_x"}, "axis=z": {"model": f"{ns}:block/crossarm_z"}}})

# ---- insulators and conductor spools -------------------------------------------------------------------------
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
	save_json(f"{ASSETS}/blockstates/insulator_{tier}.json", {"variants": {
		f"conductor={key}": {"model": f"{ns}:block/insulator_{tier}_{key}"} for key in CONDUCTOR_COLORS}})


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


for tier, metal in (("lv", COPPER), ("dist", (178, 164, 150)), ("hv", STEEL_WIRE)):
	tex(f"conductor_spool_{tier}", spool_icon(metal, False), kind="item")
	tex(f"covered_conductor_spool_{tier}", spool_icon(metal, True), kind="item")

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

# the pipe: a fat core with a fat arm toward each joined neighbour
pt = {"pipe": f"{ns}:block/pipe", "particle": f"{ns}:block/pipe"}
save_json(f"{ASSETS}/models/block/pipe_core.json", {"textures": pt, "elements": [cuboid([5, 5, 5], [11, 11, 11], "#pipe")]})
save_json(f"{ASSETS}/models/block/pipe_arm.json", {"textures": pt, "elements": [cuboid([5, 5, 0], [11, 11, 5], "#pipe")]})
save_json(f"{ASSETS}/models/block/pipe_inventory.json", {
	"parent": "minecraft:block/block",
	"display": {"gui": {"rotation": [30, 225, 0], "scale": [1.0, 1.0, 1.0]}, "ground": {"scale": [0.8, 0.8, 0.8]},
		"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.8, 0.8, 0.8]}, "thirdperson_righthand": {"scale": [0.8, 0.8, 0.8]}},
	"textures": pt, "elements": [cuboid([0, 5, 5], [16, 11, 11], "#pipe"), cuboid([5, 5, 0], [11, 11, 16], "#pipe")]})
save_json(f"{ASSETS}/blockstates/pipe.json", {"multipart": [{"apply": {"model": f"{ns}:block/pipe_core"}}] + [
	{"when": {side: "true"}, "apply": {"model": f"{ns}:block/pipe_arm", **rot}} for side, rot in arms.items()]})

# the gas line: the same shapes as the pipe, painted orange
gt = {"pipe": f"{ns}:block/gas_line", "particle": f"{ns}:block/gas_line"}
save_json(f"{ASSETS}/models/block/gas_line_core.json", {"textures": gt, "elements": [cuboid([5, 5, 5], [11, 11, 11], "#pipe")]})
save_json(f"{ASSETS}/models/block/gas_line_arm.json", {"textures": gt, "elements": [cuboid([5, 5, 0], [11, 11, 5], "#pipe")]})
inv = json.load(open(f"{ASSETS}/models/block/pipe_inventory.json"))
inv["textures"] = gt
save_json(f"{ASSETS}/models/block/gas_line_inventory.json", inv)
save_json(f"{ASSETS}/blockstates/gas_line.json", {"multipart": [{"apply": {"model": f"{ns}:block/gas_line_core"}}] + [
	{"when": {side: "true"}, "apply": {"model": f"{ns}:block/gas_line_arm", **rot}} for side, rot in arms.items()]})

# ---- items --------------------------------------------------------------------------------------------
WIRES = BARE_WIRES + WOOL_WIRES + PLASTIC_WIRES
ITEM_MODELS = {"firebox": "block/firebox", "pump": "block/pump", "pipe": "block/pipe_inventory", "gas_line": "block/gas_line_inventory", "drilling_rig": "block/drilling_rig", "refinery": "block/refinery", "refinery_column": "block/refinery_column", "cooling_tower": "block/cooling_tower", "cooling_fill": "block/cooling_fill",
	"pumpjack": "block/pumpjack",
	"gas_wellhead": "block/gas_wellhead", "casing": "block/casing", "boiler": "block/boiler", "steam_turbine": "block/steam_turbine", "electric_lamp": "block/electric_lamp",
	"electric_heater": "block/electric_heater", "breaker": "block/breaker_on", "fuse": "block/fuse_good", "fused_cutout": "block/cutout_good", "dist_breaker": "block/dist_breaker_on", "pole_transformer": "block/pole_transformer", "switch": "block/switch_on", "ufls_relay": "block/relay_on", "ufls_relay_dist": "block/relay_on", "mv_breaker": "block/mv_breaker_on", "hv_breaker": "block/hv_breaker_on", "pole": "block/pole", "crossarm": "block/crossarm_x", "transformer": "block/transformer",
	"substation_transformer": "block/substation_transformer", "ground_rod": "block/ground_rod",
	"power_meter": "item/power_meter", "field_manual": "item/field_manual", "seismograph": "item/seismograph", "crude_oil_bucket": "item/crude_oil_bucket", "plastic_sheet": "item/plastic_sheet", "diesel_bucket": "item/diesel_bucket", "steel_ingot": "item/steel_ingot", "conductor_spool_lv": "item/conductor_spool_lv", "conductor_spool_dist": "item/conductor_spool_dist", "conductor_spool_hv": "item/conductor_spool_hv", "covered_conductor_spool_lv": "item/covered_conductor_spool_lv", "covered_conductor_spool_dist": "item/covered_conductor_spool_dist", "covered_conductor_spool_hv": "item/covered_conductor_spool_hv", "insulator_lv": "block/insulator_lv_a", **{f"receptacle_{p}": f"block/receptacle_{p}" for p in PLUG_COLORS}, **{f"plug_{p}": f"block/plug_{p}" for p in PLUG_COLORS}, "insulator_dist": "block/insulator_dist_a", "insulator_hv": "block/insulator_hv_a"}
ITEM_MODELS.update({w: f"block/{w}_inventory" for w in WIRES})
for name, model in ITEM_MODELS.items():
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:{model}"}})
for item in ("power_meter", "field_manual", "seismograph", "crude_oil_bucket", "plastic_sheet", "diesel_bucket", "steel_ingot", "conductor_spool_lv", "conductor_spool_dist", "conductor_spool_hv", "covered_conductor_spool_lv", "covered_conductor_spool_dist", "covered_conductor_spool_hv"):
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{item}"}})

# ---- names --------------------------------------------------------------------------------------------
save_json(f"{ASSETS}/lang/en_us.json", {
	"block.wildcatter.boiler": "Boiler",
	"block.wildcatter.firebox": "Firebox",
	"block.wildcatter.pipe": "Water Pipe",
	"block.wildcatter.pump": "Water Pump (LV)",
	"block.wildcatter.gas_line": "Steel Pipe (oil and gas)",
	"block.wildcatter.drilling_rig": "Drilling Rig (LV)",
	"block.wildcatter.casing": "Well Casing",
	"block.wildcatter.refinery": "Refinery Base (LV)",
	"block.wildcatter.refinery_column": "Distillation Column",
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
	"block.wildcatter.ufls_relay_dist": "Under-Frequency Relay (DIST)",
	"block.wildcatter.mv_breaker": "Circuit Breaker (MV)",
	"block.wildcatter.dist_breaker": "Circuit Breaker (DIST)",
	"block.wildcatter.dist_wire_hot": "Bare Line, red (DIST)",
	"block.wildcatter.dist_wire_neutral": "Bare Line, black (DIST)",
	"block.wildcatter.dist_plastic_wire_hot": "Plastic Line, red (DIST)",
	"block.wildcatter.dist_plastic_wire_neutral": "Plastic Line, black (DIST)",
	"block.wildcatter.pole_transformer": "Pole-Mount Transformer (DIST / LV, 25 kVA)",
	"block.wildcatter.hv_breaker": "Circuit Breaker (HV)",
	"block.wildcatter.insulator_lv": "Insulator (LV, 1 kV)",
	"block.wildcatter.insulator_dist": "Pin Insulator (DIST, 15 kV)",
	"block.wildcatter.insulator_hv": "Suspension Insulator (HV, 138 kV)",
	"item.wildcatter.conductor_spool_lv": "Conductor Spool (LV)",
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
	"block.wildcatter.transformer": "Transformer (LV / MV)",
	"block.wildcatter.substation_transformer": "Substation Transformer (MV / HV)",
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
BLOCKS = ("refinery", "refinery_column", "cooling_tower", "cooling_fill", "boiler", "firebox", "pipe", "gas_line", "drilling_rig", "pumpjack", "gas_wellhead", "pump", "steam_turbine", "transformer", "substation_transformer", "electric_lamp", "electric_heater", "breaker", "fuse", "fused_cutout", "switch", "ufls_relay", "ufls_relay_dist", "mv_breaker", "dist_breaker", "pole_transformer", "hv_breaker", "pole", "crossarm", "insulator_lv", "insulator_dist", "insulator_hv", "ground_rod") + tuple(f"receptacle_{p}" for p in PLUG_COLORS) + tuple(f"plug_{p}" for p in PLUG_COLORS) + WIRES
for name in BLOCKS:
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {
		"type": "minecraft:block",
		"pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
			"conditions": [{"condition": "minecraft:survives_explosion"}]}]})

save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{ns}:{n}" for n in BLOCKS + ("casing",) if n not in ("pole", "crossarm")]})
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/axe.json", {"values": [f"{ns}:pole", f"{ns}:crossarm"]})
save_json(f"{ROOT}/data/minecraft/tags/block/climbable.json", {"values": [f"{ns}:pole"]})
save_json(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json", {"values": [f"{ns}:{n}" for n in ("boiler", "firebox", "mv_breaker", "dist_breaker", "pole_transformer", "hv_breaker", "pump", "drilling_rig", "pumpjack", "gas_wellhead", "refinery", "refinery_column", "cooling_tower", "cooling_fill", "steam_turbine", "transformer", "substation_transformer")]})

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
shaped("refinery", ["IPI", "CBC", "III"], {"I": "minecraft:iron_ingot", "P": "minecraft:copper_ingot", "C": "minecraft:cauldron", "B": "minecraft:blast_furnace"}, "refinery")
shaped("cooling_tower", ["IBI", "CFC", "III"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_bars", "C": "minecraft:copper_ingot", "F": "minecraft:blast_furnace"}, "cooling_tower")
shaped("cooling_fill", ["IBI", "IBI", "IBI"], {"I": "minecraft:iron_ingot", "B": "minecraft:iron_bars"}, "cooling_fill", 2)
shaped("refinery_column", ["ICI", "ICI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "refinery_column")
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
shaped("ufls_relay_dist", ["ICI", "RBR", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:comparator", "R": "minecraft:redstone", "B": f"{ns}:dist_breaker"}, "ufls_relay_dist")
shaped("ufls_relay", ["ICI", "RBR", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:comparator", "R": "minecraft:redstone", "B": f"{ns}:breaker"}, "ufls_relay")
shaped("switch", ["I I", "ICI", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "switch")
shaped("breaker", ["ICI", "IRI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "breaker")
shaped("mv_breaker", ["IBI", "CRC", "IBI"], {"I": "minecraft:iron_ingot", "B": f"{ns}:breaker", "C": "minecraft:copper_ingot", "R": "minecraft:redstone_block"}, "mv_breaker")
shaped("dist_breaker", ["IBI", "GRG", "IBI"], {"I": "minecraft:iron_ingot", "B": f"{ns}:mv_breaker", "G": "minecraft:gold_ingot", "R": "minecraft:redstone_block"}, "dist_breaker")
shaped("hv_breaker", ["DBD", "IRI", "DBD"], {"I": "minecraft:iron_block", "B": f"{ns}:mv_breaker", "D": "minecraft:diamond", "R": "minecraft:redstone_block"}, "hv_breaker")
for tier, wire, sheets in (("lv", "wire_hot", 1), ("dist", "dist_wire_hot", 2), ("hv", "hv_wire_hot", 3)):
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
shaped("transformer", ["ICI", "ICI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "transformer")
shaped("pole_transformer", ["ICI", "CTC", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "T": f"{ns}:transformer"}, "pole_transformer")
shaped("substation_transformer", ["IDI", "TCT", "IDI"], {"I": "minecraft:iron_ingot", "D": "minecraft:diamond", "T": "wildcatter:transformer", "C": "minecraft:copper_block"}, "substation_transformer")
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
def art(name):
	return name if os.path.exists(f"art/final/{name}.png") else f"{name}_placeholder"


def cable_sheath(sheath):
	def draw(img, d):
		noise(img, sheath, 5)
		for i, key in enumerate(("a", "b", "c", "n", "g")):
			d.line([0, 3 + 2 * i, 15, 3 + 2 * i], fill=CONDUCTOR_COLORS[key] if key != "n" else (235, 235, 235))
	return draw


CABLES = {"cable_lv": (40, 40, 44), "cable_industrial": (70, 52, 40), "cable_dist": (30, 50, 40), "cable_hv": (24, 24, 30)}
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

# pad-mounts and the unit substation: orientable cabinets
for tname, t in (("padmount_208", ((70, 110, 80), (210, 230, 210))), ("padmount_480", ((60, 100, 70), (230, 215, 160))), ("unit_substation", ((120, 124, 130), (220, 220, 210)))):
	tex(f"{tname}_front_placeholder", transformer_front(*t))
	tex(f"{tname}_side_placeholder", transformer_side(t[0]))
	tex(f"{tname}_top_placeholder", transformer_top(t[0]))
	save_json(f"{ASSETS}/models/block/{tname}.json", {"parent": "minecraft:block/orientable", "textures": {
		"front": f"{ns}:block/{art(tname + '_front')}", "side": f"{ns}:block/{art(tname + '_side')}", "top": f"{ns}:block/{art(tname + '_top')}", "bottom": f"{ns}:block/{art(tname + '_top')}"}})
	save_json(f"{ASSETS}/blockstates/{tname}.json", {"variants": {
		"facing=north": {"model": f"{ns}:block/{tname}"}, "facing=east": {"model": f"{ns}:block/{tname}", "y": 90},
		"facing=south": {"model": f"{ns}:block/{tname}", "y": 180}, "facing=west": {"model": f"{ns}:block/{tname}", "y": 270}}})
	ITEM_MODELS[tname] = f"block/{tname}"

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
for cable, (wire_hot, wire_neutral) in {"cable_lv": ("wire_hot", "wire_neutral"), "cable_industrial": ("industrial_wire_hot", "industrial_wire_neutral"), "cable_dist": ("dist_wire_hot", "dist_wire_neutral"), "cable_hv": ("hv_wire_hot", "hv_wire_neutral")}.items():
	shaped(cable, ["PHP", "HNH", "PGP"], {"P": f"{ns}:plastic_sheet", "H": f"{ns}:{wire_hot}", "N": f"{ns}:{wire_neutral}", "G": f"{ns}:ground_wire"}, cable, 4)
shaped("breaker_3p_lv", ["BBB", "IRI"], {"B": f"{ns}:breaker", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "breaker_3p_lv")
shaped("breaker_3p_industrial", ["BBB", "SRS"], {"B": f"{ns}:breaker", "S": f"{ns}:steel_ingot", "R": "minecraft:redstone_block"}, "breaker_3p_industrial")
shaped("padmount_208", ["SSS", "TCT", "SSS"], {"S": f"{ns}:steel_ingot", "T": f"{ns}:transformer", "C": "minecraft:copper_block"}, "padmount_208")
shaped("padmount_480", ["SDS", "TCT", "SSS"], {"S": f"{ns}:steel_ingot", "D": "minecraft:diamond", "T": f"{ns}:transformer", "C": "minecraft:copper_block"}, "padmount_480")
shaped("unit_substation", ["SDS", "TCT", "SDS"], {"S": f"{ns}:steel_ingot", "D": "minecraft:diamond", "T": f"{ns}:transformer", "C": "minecraft:copper_block"}, "unit_substation")
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
for bus in ("busway_lv", "busway_industrial"):
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
pick["values"] += [f"{ns}:busway_lv", f"{ns}:busway_industrial"]
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", pick)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.busway_lv": "Busway (208Y/120 V, A B C N G)", "block.wildcatter.busway_industrial": "Busway (480 V, A B C N G)",
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
# parts are drawn for a set facing north: the shaft runs west (radiator) to east (panel); the turning parts sit at the west of each block
for name, key in (("genset_diesel_engine", "genset_engine"), ("genset_gas_engine", "genset_gas_engine")):
	t = {"body": G[key], "particle": G[key]}
	save_json(f"{ASSETS}/models/block/{name}.json", {"textures": t, "elements": [
		box([3, 0, 1], [16, 12, 15], "#body"),          # crankcase and block
		box([4, 12, 3], [15, 15, 13], "#body"),         # heads and rocker covers
		box([0, 2, 6], [3, 4, 10], "#body")]})          # crank nose under the pulley
save_json(f"{ASSETS}/models/block/alternator.json", {"textures": {"body": G["alternator"], "particle": G["alternator"]}, "elements": [
	box([3, 1, 1], [16, 15, 15], "#body"), box([0, 7, 7], [3, 9, 9], "#body")]})
save_json(f"{ASSETS}/models/block/radiator.json", {"textures": {"core": G["radiator_core"], "frame": G["radiator_frame"], "particle": G["radiator_frame"]}, "elements": [
	box([0, 0, 0], [5, 16, 16], "#core"),              # the core, facing west
	box([5, 0, 0], [12, 2, 16], "#frame"), box([5, 14, 0], [12, 16, 16], "#frame"),   # fan shroud, open at the sides
	box([12, 0, 0], [16, 16, 16], "#frame")]})
save_json(f"{ASSETS}/models/block/fuel_tank.json", {"parent": "minecraft:block/cube_all", "textures": {"all": G["fuel_tank"]}})
save_json(f"{ASSETS}/models/block/exhaust_stack.json", {"textures": {"pipe": G["exhaust_stack"], "particle": G["exhaust_stack"]}, "elements": [
	box([5, 0, 5], [11, 7, 11], "#pipe"), box([6, 7, 6], [10, 16, 10], "#pipe")]})
save_json(f"{ASSETS}/models/block/genset_control_panel.json", {"parent": "minecraft:block/orientable", "textures": {
	"front": G["genset_panel_front"], "side": G["genset_panel_side"], "top": G["genset_panel_side"], "bottom": G["genset_panel_side"]}})
for name in ("genset_diesel_engine", "genset_gas_engine", "alternator", "radiator", "fuel_tank", "exhaust_stack", "genset_control_panel"):
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {
		"facing=north": {"model": f"{ns}:block/{name}"}, "facing=east": {"model": f"{ns}:block/{name}", "y": 90},
		"facing=south": {"model": f"{ns}:block/{name}", "y": 180}, "facing=west": {"model": f"{ns}:block/{name}", "y": 270}}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{name}"}})
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
# the turning parts, centred on the block with their axis along X
r = {"rotor": G["rotor"], "particle": G["rotor"]}
save_json(f"{ASSETS}/models/block/rotor_radiator_fan.json", {"textures": r, "elements": [
	box([7, 7, 7], [9, 9, 9], "#rotor"),
	box([7.5, 9, 6.5], [8.5, 15, 9.5], "#rotor"), box([7.5, 1, 6.5], [8.5, 7, 9.5], "#rotor"),
	box([7.5, 6.5, 9], [8.5, 9.5, 15], "#rotor"), box([7.5, 6.5, 1], [8.5, 9.5, 7], "#rotor")]})
save_json(f"{ASSETS}/models/block/rotor_crank_pulley.json", {"textures": r, "elements": [
	box([6.5, 4, 5], [9.5, 12, 11], "#rotor"), box([6.5, 5, 4], [9.5, 11, 12], "#rotor"),
	box([6, 7, 10], [10, 9, 12.5], "#rotor")]})   # a timing mark that shows it turning
save_json(f"{ASSETS}/models/block/rotor_coupling.json", {"textures": r, "elements": [
	box([6.5, 3, 5], [9.5, 13, 11], "#rotor"), box([6.5, 5, 3], [9.5, 11, 13], "#rotor"),
	box([6, 11, 7], [10, 13, 9], "#rotor"), box([6, 3, 7], [10, 5, 9], "#rotor")]})   # coupling bolts
save_json(f"{ASSETS}/blockstates/rotor.json", {"variants": {f"part={p}": {"model": f"{ns}:block/rotor_{p}"} for p in ("radiator_fan", "crank_pulley", "coupling")}})

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
shaped("genset_control_panel", ["SGS", "RBR", "SCS"], {"S": S_, "G": "minecraft:glass_pane", "R": "minecraft:redstone", "B": f"{ns}:breaker_3p_industrial", "C": "minecraft:comparator"}, "genset_control_panel")
GENSET_BLOCKS = ("genset_diesel_engine", "genset_gas_engine", "alternator", "radiator", "fuel_tank", "exhaust_stack", "genset_control_panel")
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
	"block.wildcatter.exhaust_stack": "Exhaust Stack", "block.wildcatter.genset_control_panel": "Genset Control Panel", "block.wildcatter.rotor": "Rotor",
	"item.wildcatter.engine_block_casting": "Engine Block Casting", "item.wildcatter.piston": "Piston and Rod", "item.wildcatter.crankshaft": "Crankshaft",
	"item.wildcatter.cylinder_head": "Cylinder Head", "item.wildcatter.fuel_injector": "Fuel Injector", "item.wildcatter.spark_plug": "Spark Plug",
	"item.wildcatter.stator_winding": "Stator Winding", "item.wildcatter.rotor_field": "Rotor Field", "item.wildcatter.exciter": "Exciter and Regulator",
	"subtitles.wildcatter.diesel_idle": "Diesel engine idles", "subtitles.wildcatter.diesel_load": "Diesel engine labours", "subtitles.wildcatter.gas_engine": "Gas engine runs",
	"subtitles.wildcatter.alternator_whine": "Alternator whines", "subtitles.wildcatter.fan": "Fan whirs", "subtitles.wildcatter.engine_crank": "Engine cranks", "subtitles.wildcatter.sync_bang": "Generator slams out of step"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 4d: the generator step-up (docs/multiblocks/gsu.md): the transformer drawing, greyed ---------------------------
save_json(f"{ASSETS}/models/block/gsu_transformer.json", {"parent": "minecraft:block/orientable", "textures": {
	"front": f"{ns}:block/transformer_front", "side": f"{ns}:block/transformer_side", "top": f"{ns}:block/transformer_top", "bottom": f"{ns}:block/transformer_top"}})
save_json(f"{ASSETS}/blockstates/gsu_transformer.json", {"variants": {
	"facing=north": {"model": f"{ns}:block/gsu_transformer"}, "facing=east": {"model": f"{ns}:block/gsu_transformer", "y": 90},
	"facing=south": {"model": f"{ns}:block/gsu_transformer", "y": 180}, "facing=west": {"model": f"{ns}:block/gsu_transformer", "y": 270}}})
save_json(f"{ASSETS}/items/gsu_transformer.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/gsu_transformer"}})
save_json(f"{DATA}/loot_table/blocks/gsu_transformer.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:gsu_transformer"}],
	"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
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


tex("tower_basin_placeholder", basin)
tex("drift_eliminator_placeholder", chevrons)
block_model("tower_basin", "minecraft:block/cube_all", {"all": f"{ns}:block/{art('tower_basin')}"})
block_model("drift_eliminator", "minecraft:block/cube_all", {"all": f"{ns}:block/{art('drift_eliminator')}"})
deck = f"{ns}:block/cooling_tower"
save_json(f"{ASSETS}/models/block/cooling_tower.json", {"textures": {"deck": deck, "particle": deck}, "elements": [
	box([0, 0, 0], [16, 8, 16], "#deck"),                                    # the fan deck and the motor under the fan
	box([0, 8, 0], [16, 16, 2], "#deck"), box([0, 8, 14], [16, 16, 16], "#deck"),   # the fan stack, open at the top
	box([0, 8, 2], [2, 16, 14], "#deck"), box([14, 8, 2], [16, 16, 14], "#deck")]})
for name in ("tower_basin", "drift_eliminator"):
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {"": {"model": f"{ns}:block/{name}"}}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{name}"}})
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
save_json(f"{ASSETS}/models/block/rotor_tower_fan.json", {"textures": r, "elements": [   # blades in the horizontal plane, turning about Y
	box([7, 7, 7], [9, 9, 9], "#rotor"),
	box([9, 7.5, 6.5], [14, 8.5, 9.5], "#rotor"), box([2, 7.5, 6.5], [7, 8.5, 9.5], "#rotor"),
	box([6.5, 7.5, 9], [9.5, 8.5, 14], "#rotor"), box([6.5, 7.5, 2], [9.5, 8.5, 7], "#rotor")]})
save_json(f"{ASSETS}/blockstates/rotor.json", {"variants": {f"part={p}": {"model": f"{ns}:block/rotor_{p}"} for p in ("radiator_fan", "crank_pulley", "coupling", "tower_fan")}})
shaped("tower_basin", ["S S", "SWS", "SSS"], {"S": "minecraft:smooth_stone", "W": "minecraft:water_bucket"}, "tower_basin")
shaped("drift_eliminator", ["P P", " P ", "P P"], {"P": f"{ns}:plastic_sheet"}, "drift_eliminator", 2)
for tag in ("mineable/pickaxe",):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:tower_basin", f"{ns}:drift_eliminator"]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
lang.update({"block.wildcatter.tower_basin": "Cooling Tower Basin", "block.wildcatter.drift_eliminator": "Drift Eliminators"})
save_json(f"{ASSETS}/lang/en_us.json", lang)

# ---- Phase 4d: the 1 MW steam plant (docs/multiblocks/steam-plant.md), dressed in the old steam machines' Ludo art -------------
def condenser_face(img, d):
	noise(img, (96, 104, 110), 5)
	for y in range(2, 15, 3):
		d.line([1, y, 14, y], fill=(150, 160, 168))   # the tube bundle seen through the shell
	d.rectangle([0, 0, 15, 15], outline=(60, 66, 72))


tex("condenser_placeholder", condenser_face)
B = lambda n: f"{ns}:block/{n}"
block_model("boiler_burner", "minecraft:block/cube_bottom_top", {"side": B("firebox_side_lit"), "top": B("firebox_top"), "bottom": B("firebox_top")})
block_model("boiler_drum", "minecraft:block/cube_bottom_top", {"side": B("boiler_side"), "top": B("boiler_top"), "bottom": B("boiler_bottom")})
block_model("superheater", "minecraft:block/cube_bottom_top", {"side": B("boiler_formed_side"), "top": B("boiler_top"), "bottom": B("boiler_bottom")})
block_model("condenser", "minecraft:block/cube_all", {"all": B(art("condenser"))})
save_json(f"{ASSETS}/models/block/turbine_casing.json", {"textures": {"side": B("steam_turbine_side"), "end": B("steam_turbine_end"), "particle": B("steam_turbine_side")}, "elements": [
	{"from": [0, 1, 1], "to": [12, 15, 15], "faces": {"north": {"texture": "#side"}, "south": {"texture": "#side"}, "up": {"texture": "#side"}, "down": {"texture": "#side"},
		"west": {"texture": "#end"}, "east": {"texture": "#end"}}},
	box([12, 7, 7], [16, 9, 9], "#side")]})   # the shaft out to the generator; the coupling turns on it
save_json(f"{ASSETS}/models/block/turbine_generator.json", {"parent": "minecraft:block/orientable", "textures": {
	"front": B("generator_end"), "side": B("generator_side"), "top": B("generator_side"), "bottom": B("generator_side")}})
save_json(f"{ASSETS}/models/block/feed_pump.json", {"textures": {"body": B("pump"), "particle": B("pump")}, "elements": [
	box([0, 2, 3], [6, 12, 13], "#body"),     # the pump
	box([10, 1, 2], [16, 13, 14], "#body"),   # the motor
	box([6, 6, 7], [10, 8, 9], "#body"),      # the shaft; the coupling turns on it
	box([0, 0, 1], [16, 2, 15], "#body")]})   # the base plate
for name in ("boiler_burner", "boiler_drum", "superheater", "turbine_casing", "condenser", "feed_pump", "turbine_generator"):
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {
		"facing=north": {"model": f"{ns}:block/{name}"}, "facing=east": {"model": f"{ns}:block/{name}", "y": 90},
		"facing=south": {"model": f"{ns}:block/{name}", "y": 180}, "facing=west": {"model": f"{ns}:block/{name}", "y": 270}}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{name}"}})
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
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
lang.update({"block.wildcatter.boiler_burner": "Boiler Burner", "block.wildcatter.boiler_drum": "Steam Drum", "block.wildcatter.superheater": "Superheater",
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


tex("station_bus_placeholder", tube)
tex("disconnect_post_placeholder", insulator_post)
save_json(f"{ASSETS}/models/block/power_transformer.json", {"parent": "minecraft:block/orientable", "textures": {
	"front": B("substation_front"), "side": B("substation_side"), "top": B("substation_top"), "bottom": B("substation_top")}})
save_json(f"{ASSETS}/models/block/relay_panel.json", {"parent": "minecraft:block/orientable", "textures": {
	"front": B("mcc_incoming_front_on"), "side": B("cabinet_side"), "top": B("cabinet_side"), "bottom": B("cabinet_side")}})
for name in ("power_transformer", "relay_panel"):
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {
		"facing=north": {"model": f"{ns}:block/{name}"}, "facing=east": {"model": f"{ns}:block/{name}", "y": 90},
		"facing=south": {"model": f"{ns}:block/{name}", "y": 180}, "facing=west": {"model": f"{ns}:block/{name}", "y": 270}}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{name}"}})
# relay-operated three-pole breakers: the three-pole model, tinted red (138 kV) and green (12.47 kV)
for name, body in (("hv_breaker_3p", "breaker_3p_industrial_body"), ("dist_breaker_3p", "breaker_3p_lv_body")):
	for position, angle in (("on", -30), ("off", 30), ("tripped", 0)):
		els = [cuboid([0, 0, 2], [16, 13, 14], "#body")]
		for x in (3, 7, 11):
			lever_el = cuboid([x, 13, 7], [x + 2, 16, 9], "#lever")
			lever_el["rotation"] = {"origin": [8, 13, 8], "axis": "x", "angle": angle}
			els.append(lever_el)
		save_json(f"{ASSETS}/models/block/{name}_{position}.json", {
			"textures": {"body": f"{ns}:block/{body}", "lever": f"{ns}:block/breaker_lever_{position}", "particle": f"{ns}:block/{body}"}, "elements": els})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {
		f"facing={facing},position={position}": {"model": f"{ns}:block/{name}_{position}", "y": y}
		for facing, y in rotations.items() for position in ("on", "off", "tripped")}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{name}_on"}})
# disconnects: a base and two insulator posts; the blade is a rotor that swings up about its hinge
post = f"{ns}:block/{art('disconnect_post')}"
for name in ("disconnect_hv", "disconnect_dist"):
	save_json(f"{ASSETS}/models/block/{name}.json", {"textures": {"post": post, "base": B("pipe"), "particle": post}, "elements": [
		box([1, 0, 1], [15, 2, 15], "#base"), box([6, 2, 1], [10, 12, 4], "#post"), box([6, 2, 12], [10, 12, 15], "#post")]})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"variants": {
		f"facing={facing},open={o}": {"model": f"{ns}:block/{name}", "y": y} for facing, y in rotations.items() for o in ("false", "true")}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{name}"}})
save_json(f"{ASSETS}/models/block/rotor_disconnect_blade.json", {"textures": r, "elements": [   # hinged at its south end (the model's centre)
	box([5, 7.5, -3], [6, 8.5, 8], "#rotor"), box([10, 7.5, -3], [11, 8.5, 8], "#rotor"), box([5, 7.5, -3], [11, 8.5, -2], "#rotor")]})
save_json(f"{ASSETS}/blockstates/rotor.json", {"variants": {f"part={p}": {"model": f"{ns}:block/rotor_{p}"} for p in ("radiator_fan", "crank_pulley", "coupling", "tower_fan", "disconnect_blade")}})
# station buses: tubular bus on the busway shape
for bus in ("bus_hv", "bus_dist"):
	t = {"cable": f"{ns}:block/{art('station_bus')}", "particle": f"{ns}:block/{art('station_bus')}"}
	save_json(f"{ASSETS}/models/block/{bus}_core.json", {"textures": t, "elements": [cuboid([4, 4, 4], [12, 12, 12])]})
	save_json(f"{ASSETS}/models/block/{bus}_arm.json", {"textures": t, "elements": [cuboid([4, 4, 0], [12, 12, 4])]})
	save_json(f"{ASSETS}/models/block/{bus}_inventory.json", {"parent": "minecraft:block/block", "textures": t, "elements": [cuboid([0, 4, 4], [16, 12, 12])]})
	save_json(f"{ASSETS}/blockstates/{bus}.json", {"multipart": [{"apply": {"model": f"{ns}:block/{bus}_core"}}] + [
		{"when": {side: "true"}, "apply": {"model": f"{ns}:block/{bus}_arm", **rot}} for side, rot in arms.items()]})
	save_json(f"{ASSETS}/items/{bus}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:block/{bus}_inventory"}})
SUB = ("power_transformer", "relay_panel", "hv_breaker_3p", "dist_breaker_3p", "disconnect_hv", "disconnect_dist", "bus_hv", "bus_dist")
for name in SUB:
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for tag in ("mineable/pickaxe", "needs_stone_tool"):
	t = json.load(open(f"{ROOT}/data/minecraft/tags/block/{tag}.json"))
	t["values"] += [f"{ns}:{n}" for n in SUB]
	save_json(f"{ROOT}/data/minecraft/tags/block/{tag}.json", t)
shaped("power_transformer", ["SDS", "TCT", "SDS"], {"S": S_, "D": "minecraft:diamond_block", "T": f"{ns}:substation_transformer", "C": "minecraft:copper_block"}, "power_transformer")
shapeless("hv_breaker_3p", [f"{ns}:hv_breaker"] * 3 + ["minecraft:comparator"], "hv_breaker_3p")
shapeless("dist_breaker_3p", [f"{ns}:dist_breaker"] * 3 + ["minecraft:comparator"], "dist_breaker_3p")
shaped("disconnect_hv", ["I I", "ISI"], {"I": f"{ns}:insulator_hv", "S": S_}, "disconnect_hv")
shaped("disconnect_dist", ["I I", "ISI"], {"I": f"{ns}:insulator_dist", "S": S_}, "disconnect_dist")
shaped("bus_hv", ["AAA", "I I"], {"A": "minecraft:iron_ingot", "I": f"{ns}:insulator_hv"}, "bus_hv", 4)
shaped("bus_dist", ["AAA", "I I"], {"A": "minecraft:iron_ingot", "I": f"{ns}:insulator_dist"}, "bus_dist", 4)
shaped("relay_panel", ["SGS", "CRC", "SSS"], {"S": S_, "G": "minecraft:glass_pane", "C": "minecraft:comparator", "R": "minecraft:redstone_block"}, "relay_panel")
lang = json.load(open(f"{ASSETS}/lang/en_us.json"))
for k in [k for k in lang if "distribution_transformer" in k]:
	del lang[k]
lang.update({"block.wildcatter.power_transformer": "Substation Power Transformer (HV delta / DIST wye, 20 MVA)", "block.wildcatter.relay_panel": "Relay Panel (50/51)",
	"block.wildcatter.hv_breaker_3p": "Substation Breaker (HV, three-pole)", "block.wildcatter.dist_breaker_3p": "Feeder Breaker (DIST, three-pole)",
	"block.wildcatter.disconnect_hv": "Disconnect Switch (HV)", "block.wildcatter.disconnect_dist": "Disconnect Switch (DIST)",
	"block.wildcatter.bus_hv": "Station Bus (HV)", "block.wildcatter.bus_dist": "Station Bus (DIST)", "subtitles.wildcatter.disconnect_arc": "Disconnect arcs",
	"subtitles.wildcatter.hv_breaker": "Power breaker operates", "subtitles.wildcatter.fuse_pop": "Fuse blows", "subtitles.wildcatter.flashover": "Insulator flashes over"})
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
FAMILY.update({"cable_lv": BUILDING, "cable_industrial": BUILDING, "cable_dist": MV_CABLE, "cable_hv": MV_CABLE})
for tier, fam in (("lv", BUILDING), ("dist", ACSR), ("hv", ACSR)):
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
# conductor blocks drop themselves; CableBlock and MultiCableBlock put the size on the drop
for name in [n for n in FAMILY if n not in SPOOLS] + ["busway_lv", "busway_industrial"]:
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
		"entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],   # the block adds the size (CableBlock.getDrops)
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})
print("assets written")
