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
tex("breaker_lever_on", lever((60, 190, 90)))
tex("breaker_lever_off", lever((120, 120, 126)))
tex("breaker_lever_tripped", lever((220, 60, 50)))
for name, t in (("transformer", ((96, 102, 112), (225, 225, 215))), ("substation", ((70, 90, 130), (200, 215, 240)))):
	tex(f"{name}_front", transformer_front(*t))
	tex(f"{name}_side", transformer_side(t[0]))
	tex(f"{name}_top", transformer_top(t[0]))

tex("wire_hot", bare(COPPER, HOT))
tex("wire_neutral", bare(COPPER, NEUTRAL))
tex("mv_wire_hot", bare(ALUMINIUM, HOT))
tex("mv_wire_neutral", bare(ALUMINIUM, NEUTRAL))
tex("hv_wire_hot", bare(STEEL_WIRE, HOT))
tex("hv_wire_neutral", bare(STEEL_WIRE, NEUTRAL))
for wear in range(4):
	tex(f"wool_wire_hot_{wear}", wool(HOT, wear))
	tex(f"wool_wire_neutral_{wear}", wool((45, 45, 50), wear))
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

# ---- models -------------------------------------------------------------------------------------------
def block_model(name, parent, textures):
	save_json(f"{ASSETS}/models/block/{name}.json", {"parent": parent, "textures": textures})


block_model("boiler", "minecraft:block/cube_bottom_top",
	{"top": f"{ns}:block/boiler_top", "bottom": f"{ns}:block/boiler_bottom", "side": f"{ns}:block/boiler_side"})
block_model("boiler_lit", "minecraft:block/cube_bottom_top",
	{"top": f"{ns}:block/boiler_top", "bottom": f"{ns}:block/boiler_bottom", "side": f"{ns}:block/boiler_side_lit"})
block_model("steam_turbine", "minecraft:block/cube_column",
	{"end": f"{ns}:block/steam_turbine_end", "side": f"{ns}:block/steam_turbine_side"})
block_model("steam_turbine_running", "minecraft:block/cube_column",
	{"end": f"{ns}:block/steam_turbine_end_running", "side": f"{ns}:block/steam_turbine_side_running"})
for load in ("electric_lamp", "electric_heater"):
	block_model(load, "minecraft:block/cube_all", {"all": f"{ns}:block/{load}"})
	block_model(f"{load}_lit", "minecraft:block/cube_all", {"all": f"{ns}:block/{load}_lit"})
	save_json(f"{ASSETS}/blockstates/{load}.json", {"variants": {
		"lit=false": {"model": f"{ns}:block/{load}"}, "lit=true": {"model": f"{ns}:block/{load}_lit"}}})

save_json(f"{ASSETS}/blockstates/boiler.json", {"variants": {
	"lit=false": {"model": f"{ns}:block/boiler"}, "lit=true": {"model": f"{ns}:block/boiler_lit"}}})
save_json(f"{ASSETS}/blockstates/steam_turbine.json", {"variants": {
	"running=false": {"model": f"{ns}:block/steam_turbine"}, "running=true": {"model": f"{ns}:block/steam_turbine_running"}}})

for tname, t in (("transformer", "transformer"), ("substation_transformer", "substation")):
	save_json(f"{ASSETS}/models/block/{tname}.json", {"parent": "minecraft:block/orientable", "textures": {
		"front": f"{ns}:block/{t}_front", "side": f"{ns}:block/{t}_side", "top": f"{ns}:block/{t}_top", "bottom": f"{ns}:block/{t}_top"}})
	save_json(f"{ASSETS}/blockstates/{tname}.json", {"variants": {
		"facing=north": {"model": f"{ns}:block/{tname}"}, "facing=east": {"model": f"{ns}:block/{tname}", "y": 90},
		"facing=south": {"model": f"{ns}:block/{tname}", "y": 180}, "facing=west": {"model": f"{ns}:block/{tname}", "y": 270}}})


def cuboid(frm, to, texture="#cable"):
	faces = {f: {"uv": [0, 0, 16, 16], "texture": texture} for f in ("north", "south", "east", "west", "up", "down")}
	return {"from": frm, "to": to, "faces": faces}


# The breaker: a box with a lever. On leans one way, off the other, tripped sits in the middle.
for position, angle in (("on", -30), ("off", 30), ("tripped", 0)):
	lever_el = cuboid([7, 10, 7], [9, 15, 9], "#lever")
	lever_el["rotation"] = {"origin": [8, 10, 8], "axis": "x", "angle": angle}
	save_json(f"{ASSETS}/models/block/breaker_{position}.json", {
		"textures": {"body": f"{ns}:block/breaker_body", "lever": f"{ns}:block/breaker_lever_{position}", "particle": f"{ns}:block/breaker_body"},
		"elements": [cuboid([3, 0, 3], [13, 10, 13], "#body"), lever_el]})
rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
save_json(f"{ASSETS}/blockstates/breaker.json", {"variants": {
	f"facing={facing},position={position}": {"model": f"{ns}:block/breaker_{position}", "y": y}
	for facing, y in rotations.items() for position in ("on", "off", "tripped")}})

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


BARE_WIRES = ("wire_hot", "wire_neutral", "mv_wire_hot", "mv_wire_neutral", "hv_wire_hot", "hv_wire_neutral", "ground_wire")
for name in BARE_WIRES:
	wire_models(name, name)
	save_json(f"{ASSETS}/blockstates/{name}.json", {"multipart": [{"apply": {"model": f"{ns}:block/{name}_core"}}] + [
		{"when": {side: "true"}, "apply": {"model": f"{ns}:block/{name}_arm", **rot}} for side, rot in arms.items()]})

WOOL_WIRES = ("wool_wire_hot", "wool_wire_neutral")
for name in WOOL_WIRES:
	parts = []
	for wear in range(4):
		wire_models(f"{name}_{wear}", f"{name}_{wear}")
		parts.append({"when": {"wear": str(wear)}, "apply": {"model": f"{ns}:block/{name}_{wear}_core"}})
		for side, rot in arms.items():
			parts.append({"when": {"AND": [{side: "true"}, {"wear": str(wear)}]}, "apply": {"model": f"{ns}:block/{name}_{wear}_arm", **rot}})
	save_json(f"{ASSETS}/blockstates/{name}.json", {"multipart": parts})
	save_json(f"{ASSETS}/models/block/{name}_inventory.json", json.load(open(f"{ASSETS}/models/block/{name}_0_inventory.json")))

# the ground rod: one thin post
save_json(f"{ASSETS}/models/block/ground_rod.json", {"textures": {"rod": f"{ns}:block/ground_rod", "particle": f"{ns}:block/ground_rod"},
	"elements": [cuboid([7, 0, 7], [9, 16, 9], "#rod")]})
save_json(f"{ASSETS}/blockstates/ground_rod.json", {"variants": {"": {"model": f"{ns}:block/ground_rod"}}})

# ---- items --------------------------------------------------------------------------------------------
WIRES = BARE_WIRES + WOOL_WIRES
ITEM_MODELS = {"boiler": "block/boiler", "steam_turbine": "block/steam_turbine", "electric_lamp": "block/electric_lamp",
	"electric_heater": "block/electric_heater", "breaker": "block/breaker_on", "transformer": "block/transformer",
	"substation_transformer": "block/substation_transformer", "ground_rod": "block/ground_rod",
	"power_meter": "item/power_meter", "field_manual": "item/field_manual"}
ITEM_MODELS.update({w: f"block/{w}_inventory" for w in WIRES})
for name, model in ITEM_MODELS.items():
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:{model}"}})
for item in ("power_meter", "field_manual"):
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{item}"}})

# ---- names --------------------------------------------------------------------------------------------
save_json(f"{ASSETS}/lang/en_us.json", {
	"block.wildcatter.boiler": "Boiler",
	"block.wildcatter.steam_turbine": "Steam Turbine",
	"block.wildcatter.wire_hot": "Bare Hot Wire (110 V)",
	"block.wildcatter.wire_neutral": "Bare Neutral Wire (110 V)",
	"block.wildcatter.wool_wire_hot": "Wool-Insulated Hot Wire (110 V)",
	"block.wildcatter.wool_wire_neutral": "Wool-Insulated Neutral Wire (110 V)",
	"block.wildcatter.mv_wire_hot": "Bare Hot Line (4.16 kV)",
	"block.wildcatter.mv_wire_neutral": "Bare Neutral Line (4.16 kV)",
	"block.wildcatter.hv_wire_hot": "Bare Hot Line (138 kV)",
	"block.wildcatter.hv_wire_neutral": "Bare Neutral Line (138 kV)",
	"block.wildcatter.ground_rod": "Ground Rod",
	"block.wildcatter.ground_wire": "Ground Wire",
	"block.wildcatter.breaker": "Circuit Breaker (110 V)",
	"block.wildcatter.electric_lamp": "Electric Lamp (60 W)",
	"block.wildcatter.electric_heater": "Electric Heater (1500 W)",
	"block.wildcatter.transformer": "Transformer (110 V / 4.16 kV)",
	"block.wildcatter.substation_transformer": "Substation Transformer (4.16 kV / 138 kV)",
	"item.wildcatter.power_meter": "Power Meter",
	"item.wildcatter.field_manual": "Wildcatter Field Manual",
})

# ---- loot: every block drops itself ---------------------------------------------------------------
BLOCKS = ("boiler", "steam_turbine", "transformer", "substation_transformer", "electric_lamp", "electric_heater", "breaker", "ground_rod") + WIRES
for name in BLOCKS:
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {
		"type": "minecraft:block",
		"pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
			"conditions": [{"condition": "minecraft:survives_explosion"}]}]})

save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{ns}:{n}" for n in BLOCKS]})
save_json(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json", {"values": [f"{ns}:{n}" for n in ("boiler", "steam_turbine", "transformer", "substation_transformer")]})

# ---- recipes (early game: iron, copper, redstone, glass, dye, wool) ----------------------------------------
def shaped(name, pattern, key, result, count=1):
	save_json(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "redstone",
		"pattern": pattern, "key": key, "result": {"id": f"{ns}:{result}", "count": count}})


def shapeless(name, ingredients, result, count=1):
	save_json(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "redstone",
		"ingredients": ingredients, "result": {"id": f"{ns}:{result}", "count": count}})


shaped("boiler", ["III", "IWI", "III"], {"I": "minecraft:iron_ingot", "W": "minecraft:water_bucket"}, "boiler")
shaped("steam_turbine", ["ICI", "CRC", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "steam_turbine")
# Wire is coloured with dye so you can tell the two conductors apart: red is hot, black is neutral.
for role, dye in (("hot", "minecraft:red_dye"), ("neutral", "minecraft:black_dye")):
	shapeless(f"wire_{role}", ["minecraft:copper_ingot"] * 3 + [dye], f"wire_{role}", 6)
	shapeless(f"wool_wire_{role}", [f"{ns}:wire_{role}"] * 4 + ["#minecraft:wool"], f"wool_wire_{role}", 4)
	shapeless(f"mv_wire_{role}", ["minecraft:iron_ingot"] * 2 + ["minecraft:copper_ingot"] * 3 + [dye], f"mv_wire_{role}", 4)
	shapeless(f"hv_wire_{role}", ["minecraft:iron_ingot"] * 4 + ["minecraft:copper_ingot"] * 2 + [dye], f"hv_wire_{role}", 4)
shaped("ground_rod", ["C", "C", "C"], {"C": "minecraft:copper_ingot"}, "ground_rod", 2)
shapeless("ground_wire", [f"{ns}:wire_neutral", "minecraft:green_dye"], "ground_wire")
shaped("breaker", ["ICI", "IRI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "breaker")
shaped("transformer", ["ICI", "ICI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "transformer")
shaped("substation_transformer", ["IDI", "TCT", "IDI"], {"I": "minecraft:iron_ingot", "D": "minecraft:diamond", "T": "wildcatter:transformer", "C": "minecraft:copper_block"}, "substation_transformer")
shaped("electric_lamp", [" G ", "GRG", " C "], {"G": "minecraft:glass_pane", "R": "minecraft:redstone", "C": "minecraft:copper_ingot"}, "electric_lamp")
shaped("electric_heater", ["III", "CRC", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "electric_heater")
shapeless("field_manual", ["minecraft:book", "minecraft:copper_ingot"], "field_manual")
shaped("power_meter", [" C ", "IRI", " I "], {"C": "minecraft:copper_ingot", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "power_meter")
print("assets written")
