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


def copper(img, d):
	noise(img, (196, 118, 72), 10)
	for y in (4, 9, 13):
		d.line([0, y, 15, y], fill=(150, 80, 48))


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
tex("copper_cable", copper)
tex("electric_lamp", lamp(False))
tex("electric_lamp_lit", lamp(True))


def insulated(img, d):
	noise(img, (84, 90, 100), 6)
	for y in (3, 8, 12):
		d.line([0, y, 15, y], fill=(235, 190, 40))


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


tex("power_meter", meter, "item")
tex("medium_cable", insulated)
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


save_json(f"{ASSETS}/models/block/cable_core.json", {
	"textures": {"cable": f"{ns}:block/copper_cable", "particle": f"{ns}:block/copper_cable"},
	"elements": [cuboid([6, 6, 6], [10, 10, 10])]})
save_json(f"{ASSETS}/models/block/cable_arm.json", {  # points north; rotated for the other sides in the blockstate
	"textures": {"cable": f"{ns}:block/copper_cable", "particle": f"{ns}:block/copper_cable"},
	"elements": [cuboid([6, 6, 0], [10, 10, 6])]})
save_json(f"{ASSETS}/models/block/cable_inventory.json", {  # the item: a short piece lying across the middle
	"parent": "minecraft:block/block",
	"display": {"gui": {"rotation": [30, 225, 0], "scale": [0.9, 0.9, 0.9]}, "ground": {"scale": [0.8, 0.8, 0.8]},
		"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.8, 0.8, 0.8]}, "thirdperson_righthand": {"scale": [0.8, 0.8, 0.8]}},
	"textures": {"cable": f"{ns}:block/copper_cable", "particle": f"{ns}:block/copper_cable"},
	"elements": [cuboid([0, 6, 6], [16, 10, 10]), cuboid([6, 6, 0], [10, 10, 16])]})

save_json(f"{ASSETS}/blockstates/boiler.json", {"variants": {
	"lit=false": {"model": f"{ns}:block/boiler"}, "lit=true": {"model": f"{ns}:block/boiler_lit"}}})
save_json(f"{ASSETS}/blockstates/steam_turbine.json", {"variants": {
	"running=false": {"model": f"{ns}:block/steam_turbine"}, "running=true": {"model": f"{ns}:block/steam_turbine_running"}}})
save_json(f"{ASSETS}/blockstates/electric_lamp.json", {"variants": {
	"lit=false": {"model": f"{ns}:block/electric_lamp"}, "lit=true": {"model": f"{ns}:block/electric_lamp_lit"}}})

arms = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
multipart = [{"apply": {"model": f"{ns}:block/cable_core"}}]
for side, rot in arms.items():
	multipart.append({"when": {side: "true"}, "apply": {"model": f"{ns}:block/cable_arm", **rot}})
save_json(f"{ASSETS}/blockstates/copper_cable.json", {"multipart": multipart})
for part in ("core", "arm"):
	m = json.load(open(f"{ASSETS}/models/block/cable_{part}.json"))
	m["textures"] = {"cable": f"{ns}:block/medium_cable", "particle": f"{ns}:block/medium_cable"}
	save_json(f"{ASSETS}/models/block/medium_cable_{part}.json", m)
save_json(f"{ASSETS}/blockstates/medium_cable.json", {"multipart": [
	{"apply": {"model": f"{ns}:block/medium_cable_core"}}] + [
	{"when": {side: "true"}, "apply": {"model": f"{ns}:block/medium_cable_arm", **rot}} for side, rot in arms.items()]})
inv = json.load(open(f"{ASSETS}/models/block/cable_inventory.json"))
inv["textures"] = {"cable": f"{ns}:block/medium_cable", "particle": f"{ns}:block/medium_cable"}
save_json(f"{ASSETS}/models/block/medium_cable_inventory.json", inv)

# --- items --------------------------------------------------------------------------------------
for name, model in (("boiler", "block/boiler"), ("steam_turbine", "block/steam_turbine"), ("electric_lamp", "block/electric_lamp"),
		("copper_cable", "block/cable_inventory"), ("medium_cable", "block/medium_cable_inventory"), ("transformer", "block/transformer"), ("substation_transformer", "block/substation_transformer"), ("power_meter", "item/power_meter")):
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:{model}"}})
save_json(f"{ASSETS}/models/item/power_meter.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:item/power_meter"}})

# --- names --------------------------------------------------------------------------------------
save_json(f"{ASSETS}/lang/en_us.json", {
	"block.wildcatter.boiler": "Boiler",
	"block.wildcatter.steam_turbine": "Steam Turbine",
	"block.wildcatter.copper_cable": "Copper Cable",
	"block.wildcatter.electric_lamp": "Electric Lamp",
	"block.wildcatter.medium_cable": "Medium-Voltage Line",
	"block.wildcatter.transformer": "Transformer (110 V / 4.16 kV)",
	"block.wildcatter.substation_transformer": "Substation Transformer (4.16 kV / 138 kV)",
	"item.wildcatter.power_meter": "Power Meter",
})

# --- loot: every block drops itself ---------------------------------------------------------------
ALL = ("boiler", "steam_turbine", "copper_cable", "medium_cable", "transformer", "substation_transformer", "electric_lamp")
for name in ALL:
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {
		"type": "minecraft:block",
		"pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
			"conditions": [{"condition": "minecraft:survives_explosion"}]}]})

save_json(f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{ns}:{n}" for n in ALL]})
save_json(f"{ROOT}/data/minecraft/tags/block/needs_stone_tool.json", {"values": [f"{ns}:{n}" for n in ("boiler", "steam_turbine", "transformer", "substation_transformer")]})

# --- recipes (early game: iron, copper, redstone, glass) ------------------------------------------------
def shaped(name, pattern, key, result, count=1):
	save_json(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "redstone",
		"pattern": pattern, "key": key, "result": {"id": f"{ns}:{result}", "count": count}})


shaped("boiler", ["III", "IWI", "III"], {"I": "minecraft:iron_ingot", "W": "minecraft:water_bucket"}, "boiler")
shaped("steam_turbine", ["ICI", "CRC", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "steam_turbine")
shaped("copper_cable", ["CCC"], {"C": "minecraft:copper_ingot"}, "copper_cable", 6)
shaped("medium_cable", ["CCC", "RRR", "CCC"], {"C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, "medium_cable", 4)
shaped("transformer", ["ICI", "ICI", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"}, "transformer")
shaped("substation_transformer", ["IDI", "TCT", "IDI"], {"I": "minecraft:iron_ingot", "D": "minecraft:diamond", "T": "wildcatter:transformer", "C": "minecraft:copper_block"}, "substation_transformer")
shaped("electric_lamp", [" G ", "GRG", " C "], {"G": "minecraft:glass_pane", "R": "minecraft:redstone", "C": "minecraft:copper_ingot"}, "electric_lamp")
shaped("power_meter", [" C ", "IRI", " I "], {"C": "minecraft:copper_ingot", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "power_meter")
print("assets written")
