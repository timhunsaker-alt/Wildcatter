#!/usr/bin/env python3
"""Draws the placeholder textures and writes the model/blockstate/lang/recipe/loot JSON.

Placeholders only: real art comes from Ludo once the shapes settle. Run from the repo root: python3 tools/make_assets.py
"""
import json, os, random
from PIL import Image, ImageDraw

ROOT = "src/main/resources"
ASSETS = f"{ROOT}/assets/wildcatter"
DATA = f"{ROOT}/data/wildcatter"
random.seed(7)


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


def wire(base, strand=None, stripe=None):
	"""A wire texture: a base colour, optional twisted-strand shading, optional coloured stripe."""
	def draw(img, d):
		noise(img, base, 8)
		if strand:
			for y in (3, 8, 12):
				d.line([0, y, 15, y], fill=strand)
		if stripe:
			d.line([0, 5, 15, 5], fill=stripe)
			d.line([0, 10, 15, 10], fill=stripe)
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


def meter(img, d):
	d.rectangle([3, 2, 12, 13], fill=(60, 64, 70), outline=(30, 32, 36))
	d.rectangle([4, 3, 11, 8], fill=(90, 200, 120))
	d.line([5, 7, 9, 4], fill=(20, 60, 30))
	d.rectangle([5, 10, 6, 11], fill=(220, 70, 60))
	d.rectangle([9, 10, 10, 11], fill=(230, 200, 70))


tex("boiler_side", boiler_side(False))
tex("boiler_side_lit", boiler_side(True))
tex("boiler_top", boiler_top)
tex("boiler_bottom", boiler_bottom)
tex("steam_turbine_side", turbine_side(False))
tex("steam_turbine_side_running", turbine_side(True))
tex("steam_turbine_end", turbine_end(False))
tex("steam_turbine_end_running", turbine_end(True))
tex("copper_cable", wire((196, 118, 72), strand=(150, 80, 48)))                 # bare copper
tex("insulated_cable", wire((30, 30, 34), stripe=(190, 190, 196)))              # black sheath
tex("medium_line", wire((170, 176, 184), strand=(120, 126, 134)))                # bare aluminium
tex("insulated_medium_cable", wire((60, 64, 72), stripe=(235, 190, 40)))         # grey sheath, yellow stripe
tex("high_line", wire((140, 150, 160), strand=(90, 98, 108)))                    # steel-cored aluminium
tex("insulated_high_cable", wire((26, 32, 52), stripe=(210, 60, 50)))            # dark blue sheath, red stripe
tex("ground_wire", wire((40, 150, 70), stripe=(240, 220, 60)))                   # green with a yellow stripe
tex("ground_rod", wire((205, 125, 80), strand=(140, 80, 50)))
tex("electric_lamp", lamp(False))
tex("electric_lamp_lit", lamp(True))


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


def manual(img, d):
	d.rectangle([2, 1, 13, 14], fill=(120, 70, 40), outline=(60, 34, 20))
	d.rectangle([3, 2, 4, 13], fill=(90, 50, 30))                       # spine
	d.rectangle([6, 4, 11, 11], fill=(236, 222, 180), outline=(60, 34, 20))
	d.polygon([(9, 4), (7, 8), (9, 8), (8, 11), (11, 7), (9, 7)], fill=(235, 190, 40))  # a lightning bolt


tex("power_meter", meter, "item")
tex("field_manual", manual, "item")
tex("transformer_front", transformer_front((96, 102, 112), (225, 225, 215)))
tex("transformer_side", transformer_side((96, 102, 112)))
tex("transformer_top", transformer_top((96, 102, 112)))
tex("transformer_back", transformer_side((120, 100, 80)))
tex("substation_front", transformer_front((70, 90, 130), (200, 215, 240)))
tex("substation_side", transformer_side((70, 90, 130)))
tex("substation_top", transformer_top((70, 90, 130)))
tex("substation_back", transformer_side((100, 100, 130)))

# --- models -------------------------------------------------------------------------------------
ns = "wildcatter"


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
block_model("electric_lamp", "minecraft:block/cube_all", {"all": f"{ns}:block/electric_lamp"})
block_model("electric_lamp_lit", "minecraft:block/cube_all", {"all": f"{ns}:block/electric_lamp_lit"})


for tname, t in (("transformer", "transformer"), ("substation_transformer", "substation")):
	save_json(f"{ASSETS}/models/block/{tname}.json", {"parent": "minecraft:block/orientable", "textures": {
		"front": f"{ns}:block/{t}_front", "side": f"{ns}:block/{t}_side", "top": f"{ns}:block/{t}_top", "bottom": f"{ns}:block/{t}_top"}})
	save_json(f"{ASSETS}/blockstates/{tname}.json", {"variants": {
		"facing=north": {"model": f"{ns}:block/{tname}"}, "facing=east": {"model": f"{ns}:block/{tname}", "y": 90},
		"facing=south": {"model": f"{ns}:block/{tname}", "y": 180}, "facing=west": {"model": f"{ns}:block/{tname}", "y": 270}}})


def cuboid(frm, to, tex_name="#cable"):
	faces = {f: {"uv": [0, 0, 16, 16], "texture": tex_name} for f in ("north", "south", "east", "west", "up", "down")}
	return {"from": frm, "to": to, "faces": faces}


CABLES = ("copper_cable", "insulated_cable", "medium_line", "insulated_medium_cable", "high_line", "insulated_high_cable", "ground_wire")
for name in CABLES:
	t = {"cable": f"{ns}:block/{name}", "particle": f"{ns}:block/{name}"}
	save_json(f"{ASSETS}/models/block/{name}_core.json", {"textures": t, "elements": [cuboid([7, 7, 7], [9, 9, 9])]})
	save_json(f"{ASSETS}/models/block/{name}_arm.json", {"textures": t, "elements": [cuboid([7, 7, 0], [9, 9, 7])]})  # points north; rotated in the blockstate
	save_json(f"{ASSETS}/models/block/{name}_inventory.json", {  # the item: a short piece lying across the middle
		"parent": "minecraft:block/block",
		"display": {"gui": {"rotation": [30, 225, 0], "scale": [1.2, 1.2, 1.2]}, "ground": {"scale": [0.8, 0.8, 0.8]},
			"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.8, 0.8, 0.8]}, "thirdperson_righthand": {"scale": [0.8, 0.8, 0.8]}},
		"textures": t,
		"elements": [cuboid([0, 7, 7], [16, 9, 9]), cuboid([7, 7, 0], [9, 9, 16])]})

arms = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
for name in CABLES:
	save_json(f"{ASSETS}/blockstates/{name}.json", {"multipart": [{"apply": {"model": f"{ns}:block/{name}_core"}}] + [
		{"when": {side: "true"}, "apply": {"model": f"{ns}:block/{name}_arm", **rot}} for side, rot in arms.items()]})

# the ground rod: one thin post
save_json(f"{ASSETS}/models/block/ground_rod.json", {"textures": {"rod": f"{ns}:block/ground_rod", "particle": f"{ns}:block/ground_rod"},
	"elements": [cuboid([7, 0, 7], [9, 16, 9], "#rod")]})
save_json(f"{ASSETS}/blockstates/ground_rod.json", {"variants": {"": {"model": f"{ns}:block/ground_rod"}}})

save_json(f"{ASSETS}/blockstates/boiler.json", {"variants": {
	"lit=false": {"model": f"{ns}:block/boiler"}, "lit=true": {"model": f"{ns}:block/boiler_lit"}}})
save_json(f"{ASSETS}/blockstates/steam_turbine.json", {"variants": {
	"running=false": {"model": f"{ns}:block/steam_turbine"}, "running=true": {"model": f"{ns}:block/steam_turbine_running"}}})
save_json(f"{ASSETS}/blockstates/electric_lamp.json", {"variants": {
	"lit=false": {"model": f"{ns}:block/electric_lamp"}, "lit=true": {"model": f"{ns}:block/electric_lamp_lit"}}})

# --- items --------------------------------------------------------------------------------------
for name, model in (("boiler", "block/boiler"), ("steam_turbine", "block/steam_turbine"), ("electric_lamp", "block/electric_lamp"),
		("transformer", "block/transformer"), ("substation_transformer", "block/substation_transformer"), ("ground_rod", "block/ground_rod"),
		("power_meter", "item/power_meter"), ("field_manual", "item/field_manual")) + tuple((c, f"block/{c}_inventory") for c in CABLES):
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:{model}"}})
for item in ("power_meter", "field_manual"):
	save_json(f"{ASSETS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/{item}"}})

# --- names --------------------------------------------------------------------------------------
save_json(f"{ASSETS}/lang/en_us.json", {
	"block.wildcatter.boiler": "Boiler",
	"block.wildcatter.steam_turbine": "Steam Turbine",
	"block.wildcatter.copper_cable": "Bare Copper Wire (110 V)",
	"block.wildcatter.insulated_cable": "Insulated Cable (110 V)",
	"block.wildcatter.medium_line": "Bare Medium-Voltage Line (4.16 kV)",
	"block.wildcatter.insulated_medium_cable": "Insulated Medium-Voltage Cable (4.16 kV)",
	"block.wildcatter.high_line": "Bare High-Voltage Line (138 kV)",
	"block.wildcatter.insulated_high_cable": "Insulated High-Voltage Cable (138 kV)",
	"block.wildcatter.ground_rod": "Ground Rod",
	"block.wildcatter.ground_wire": "Ground Wire",
	"block.wildcatter.electric_lamp": "Electric Lamp",
	"block.wildcatter.transformer": "Transformer (110 V / 4.16 kV)",
	"block.wildcatter.substation_transformer": "Substation Transformer (4.16 kV / 138 kV)",
	"item.wildcatter.power_meter": "Power Meter",
	"item.wildcatter.field_manual": "Wildcatter Field Manual",
})

# --- loot: every block drops itself ---------------------------------------------------------------
ALL = ("boiler", "steam_turbine", "transformer", "substation_transformer", "electric_lamp", "ground_rod") + CABLES
for name in ALL:
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {
		"type": "minecraft:block",
		"pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
			"conditions": [{"condition": "minecraft:survives_explosion"}]}]})

save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{ns}:{n}" for n in ALL]})
save_json(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json", {"values": [f"{ns}:{n}" for n in ("boiler", "steam_turbine", "transformer", "substation_transformer")]})

# --- recipes (early game: iron, copper, redstone, glass; slime stands in for rubber) ------------------------
def shaped(name, pattern, key, result, count=1):
	save_json(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "redstone",
		"pattern": pattern, "key": key, "result": {"id": f"{ns}:{result}", "count": count}})


def shapeless(name, ingredients, result, count=1):
	save_json(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "redstone",
		"ingredients": ingredients, "result": {"id": f"{ns}:{result}", "count": count}})


shaped("boiler", ["III", "IWI", "III"], {"I": "minecraft:iron_ingot", "W": "minecraft:water_bucket"}, "boiler")
shaped("steam_turbine", ["ICI", "CRC", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "steam_turbine")
shaped("copper_cable", ["CCC"], {"C": "minecraft:copper_ingot"}, "copper_cable", 6)
shaped("medium_line", ["CCC", "RRR", "CCC"], {"C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "medium_line", 4)
shaped("high_line", ["III", "CCC", "III"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "high_line", 4)
# Insulating a wire takes slime (our rubber): more of it the higher the voltage.
shapeless("insulated_cable", ["wildcatter:copper_cable"] * 4 + ["minecraft:slime_ball"], "insulated_cable", 4)
shapeless("insulated_medium_cable", ["wildcatter:medium_line"] * 4 + ["minecraft:slime_ball"] * 2, "insulated_medium_cable", 4)
shapeless("insulated_high_cable", ["wildcatter:high_line"] * 4 + ["minecraft:slime_ball"] * 4, "insulated_high_cable", 4)
shaped("ground_rod", ["C", "C", "C"], {"C": "minecraft:copper_ingot"}, "ground_rod", 2)
shapeless("ground_wire", ["wildcatter:copper_cable", "minecraft:green_dye"], "ground_wire")
shaped("transformer", ["ICI", "ICI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "transformer")
shaped("substation_transformer", ["IDI", "TCT", "IDI"], {"I": "minecraft:iron_ingot", "D": "minecraft:diamond", "T": "wildcatter:transformer", "C": "minecraft:copper_block"}, "substation_transformer")
shaped("electric_lamp", [" G ", "GRG", " C "], {"G": "minecraft:glass_pane", "R": "minecraft:redstone", "C": "minecraft:copper_ingot"}, "electric_lamp")
shapeless("field_manual", ["minecraft:book", "minecraft:copper_ingot"], "field_manual")
shaped("power_meter", [" C ", "IRI", " I "], {"C": "minecraft:copper_ingot", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "power_meter")
print("assets written")
