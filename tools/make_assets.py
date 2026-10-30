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
	for wear in range(4):
		tex(f"wool_wire_{key}_{wear}", wool(band if key != "n" else (45, 45, 50), wear))
	for prefix, tint in (("", (235, 235, 240)), ("mv_", (225, 230, 245)), ("dist_", (225, 240, 230)), ("hv_", (215, 225, 250))):
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
		kind = "item" if f in ("power_meter.png", "field_manual.png", "seismograph.png", "crude_oil_bucket.png", "plastic_sheet.png", "diesel_bucket.png", "steel_ingot.png") else "block"
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
for engine in ("diesel_engine", "gas_turbine"):
	block_model(engine, "minecraft:block/cube_all", {"all": f"{ns}:block/{engine}"})
	block_model(f"{engine}_running", "minecraft:block/cube_all", {"all": f"{ns}:block/{engine}_running"})
	save_json(f"{ASSETS}/blockstates/{engine}.json", {"variants": {
		"lit=false": {"model": f"{ns}:block/{engine}"}, "lit=true": {"model": f"{ns}:block/{engine}_running"}}})
for load in ("electric_lamp", "electric_heater"):
	block_model(load, "minecraft:block/cube_all", {"all": f"{ns}:block/{load}"})
	block_model(f"{load}_lit", "minecraft:block/cube_all", {"all": f"{ns}:block/{load}_lit"})
	save_json(f"{ASSETS}/blockstates/{load}.json", {"variants": {
		"lit=false": {"model": f"{ns}:block/{load}"}, "lit=true": {"model": f"{ns}:block/{load}_lit"}}})

save_json(f"{ASSETS}/blockstates/boiler.json", {"variants": {
	f"lit={lit},formed={formed}": {"model": f"{ns}:block/boiler{'_formed' if formed == 'true' else ''}{'_lit' if lit == 'true' else ''}"}
	for lit in ("false", "true") for formed in ("false", "true")}})
save_json(f"{ASSETS}/blockstates/steam_turbine.json", {"variants": {
	f"running={running},formed={formed}": {"model": f"{ns}:block/steam_turbine{'_formed' if formed == 'true' else ''}{'_running' if running == 'true' else ''}"}
	for running in ("false", "true") for formed in ("false", "true")}})

for tname, t in (("transformer", "transformer"), ("substation_transformer", "substation"), ("distribution_transformer", "distribution"), ("pole_transformer", "pole_tx")):
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


BARE_WIRES = ("wire_hot", "wire_neutral", "mv_wire_hot", "mv_wire_neutral", "dist_wire_hot", "dist_wire_neutral", "hv_wire_hot", "hv_wire_neutral", "ground_wire")
PLASTIC_WIRES = ("plastic_wire_hot", "plastic_wire_neutral", "mv_plastic_wire_hot", "mv_plastic_wire_neutral", "dist_plastic_wire_hot", "dist_plastic_wire_neutral", "hv_plastic_wire_hot", "hv_plastic_wire_neutral")
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
	"diesel_engine": "block/diesel_engine", "gas_turbine": "block/gas_turbine", "pumpjack": "block/pumpjack",
	"gas_wellhead": "block/gas_wellhead", "casing": "block/casing", "boiler": "block/boiler", "steam_turbine": "block/steam_turbine", "electric_lamp": "block/electric_lamp",
	"electric_heater": "block/electric_heater", "breaker": "block/breaker_on", "fuse": "block/fuse_good", "fused_cutout": "block/cutout_good", "dist_breaker": "block/dist_breaker_on", "distribution_transformer": "block/distribution_transformer", "pole_transformer": "block/pole_transformer", "switch": "block/switch_on", "ufls_relay": "block/relay_on", "mv_breaker": "block/mv_breaker_on", "hv_breaker": "block/hv_breaker_on", "pole": "block/pole", "crossarm": "block/crossarm_x", "transformer": "block/transformer",
	"substation_transformer": "block/substation_transformer", "ground_rod": "block/ground_rod",
	"power_meter": "item/power_meter", "field_manual": "item/field_manual", "seismograph": "item/seismograph", "crude_oil_bucket": "item/crude_oil_bucket", "plastic_sheet": "item/plastic_sheet", "diesel_bucket": "item/diesel_bucket", "steel_ingot": "item/steel_ingot", "conductor_spool_lv": "item/conductor_spool_lv", "conductor_spool_dist": "item/conductor_spool_dist", "conductor_spool_hv": "item/conductor_spool_hv", "covered_conductor_spool_lv": "item/covered_conductor_spool_lv", "covered_conductor_spool_dist": "item/covered_conductor_spool_dist", "covered_conductor_spool_hv": "item/covered_conductor_spool_hv", "insulator_lv": "block/insulator_lv_a", "insulator_dist": "block/insulator_dist_a", "insulator_hv": "block/insulator_hv_a"}
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
	"block.wildcatter.cooling_tower": "Cooling Tower Fan (LV)",
	"block.wildcatter.cooling_fill": "Cooling Tower Fill",
	"block.wildcatter.diesel_engine": "Diesel Engine (LV)",
	"block.wildcatter.gas_turbine": "Gas Turbine (LV)",
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
	"block.wildcatter.mv_breaker": "Circuit Breaker (MV)",
	"block.wildcatter.dist_breaker": "Circuit Breaker (DIST)",
	"block.wildcatter.dist_wire_hot": "Bare Line, red (DIST)",
	"block.wildcatter.dist_wire_neutral": "Bare Line, black (DIST)",
	"block.wildcatter.dist_plastic_wire_hot": "Plastic Line, red (DIST)",
	"block.wildcatter.dist_plastic_wire_neutral": "Plastic Line, black (DIST)",
	"block.wildcatter.distribution_transformer": "Distribution Transformer (HV / DIST)",
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
BLOCKS = ("refinery", "refinery_column", "cooling_tower", "cooling_fill", "diesel_engine", "gas_turbine", "boiler", "firebox", "pipe", "gas_line", "drilling_rig", "pumpjack", "gas_wellhead", "pump", "steam_turbine", "transformer", "substation_transformer", "electric_lamp", "electric_heater", "breaker", "fuse", "fused_cutout", "switch", "ufls_relay", "mv_breaker", "dist_breaker", "distribution_transformer", "pole_transformer", "hv_breaker", "pole", "crossarm", "insulator_lv", "insulator_dist", "insulator_hv", "ground_rod") + WIRES
for name in BLOCKS:
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {
		"type": "minecraft:block",
		"pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
			"conditions": [{"condition": "minecraft:survives_explosion"}]}]})

save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{ns}:{n}" for n in BLOCKS + ("casing",) if n not in ("pole", "crossarm")]})
save_json(f"{ROOT}/data/minecraft/tags/block/mineable/axe.json", {"values": [f"{ns}:pole", f"{ns}:crossarm"]})
save_json(f"{ROOT}/data/minecraft/tags/block/climbable.json", {"values": [f"{ns}:pole"]})
save_json(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json", {"values": [f"{ns}:{n}" for n in ("boiler", "firebox", "mv_breaker", "dist_breaker", "distribution_transformer", "pole_transformer", "hv_breaker", "pump", "drilling_rig", "pumpjack", "gas_wellhead", "refinery", "refinery_column", "cooling_tower", "cooling_fill", "diesel_engine", "gas_turbine", "steam_turbine", "transformer", "substation_transformer")]})

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
shaped("diesel_engine", ["IPI", "CFC", "IRI"], {"I": "minecraft:iron_ingot", "P": "minecraft:piston", "C": "minecraft:copper_ingot", "F": "minecraft:furnace", "R": "minecraft:redstone"}, "diesel_engine")
shaped("gas_turbine", ["IDI", "CRC", "IDI"], {"I": "minecraft:iron_ingot", "D": "minecraft:diamond", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "gas_turbine")
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
shaped("mv_breaker", ["IBI", "CRC", "IBI"], {"I": "minecraft:iron_ingot", "B": f"{ns}:breaker", "C": "minecraft:copper_ingot", "R": "minecraft:redstone_block"}, "mv_breaker")
shaped("dist_breaker", ["IBI", "GRG", "IBI"], {"I": "minecraft:iron_ingot", "B": f"{ns}:mv_breaker", "G": "minecraft:gold_ingot", "R": "minecraft:redstone_block"}, "dist_breaker")
shaped("hv_breaker", ["DBD", "IRI", "DBD"], {"I": "minecraft:iron_block", "B": f"{ns}:mv_breaker", "D": "minecraft:diamond", "R": "minecraft:redstone_block"}, "hv_breaker")
for tier, wire, sheets in (("lv", "wire_hot", 1), ("dist", "dist_wire_hot", 2), ("hv", "hv_wire_hot", 3)):
	shaped(f"conductor_spool_{tier}", ["WWW", "WSW", "WWW"], {"W": f"{ns}:{wire}", "S": "minecraft:stick"}, f"conductor_spool_{tier}")
	shapeless(f"covered_conductor_spool_{tier}", [f"{ns}:conductor_spool_{tier}"] + [f"{ns}:plastic_sheet"] * sheets, f"covered_conductor_spool_{tier}")
shaped("insulator_lv", ["B", "N"], {"B": "minecraft:brick", "N": "minecraft:iron_nugget"}, "insulator_lv", 2)
shaped("insulator_dist", ["B", "B", "N"], {"B": "minecraft:brick", "N": "minecraft:iron_nugget"}, "insulator_dist", 2)
shaped("insulator_hv", ["BBB", "BNB", "BIB"], {"B": "minecraft:brick", "N": "minecraft:iron_nugget", "I": "minecraft:iron_ingot"}, "insulator_hv", 2)
shaped("pole", ["L", "L", "L"], {"L": "#minecraft:logs"}, "pole", 3)
shaped("crossarm", ["PPP", " N "], {"P": "#minecraft:planks", "N": "minecraft:iron_nugget"}, "crossarm", 2)
shaped("transformer", ["ICI", "ICI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "transformer")
shaped("pole_transformer", ["ICI", "CTC", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "T": f"{ns}:transformer"}, "pole_transformer")
shaped("distribution_transformer", ["IDI", "SCS", "IDI"], {"I": "minecraft:iron_ingot", "D": "minecraft:diamond", "S": f"{ns}:substation_transformer", "C": "minecraft:copper_block"}, "distribution_transformer")
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
print("assets written")
