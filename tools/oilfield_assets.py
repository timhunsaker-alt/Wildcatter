#!/usr/bin/env python3
"""Phase 23, the oilfield follow-ons (docs/multiblocks/oilfield-follow-ons.md): loot, tags, recipes, names and item icons for its parts
and items. The part models come from tools/models/oilfield.py. Run from the repo root (make_assets.py runs it too); it draws its
placeholders with its own random numbers, so the rest of make_assets.py's noise is unchanged. An item whose Ludo icon is in
art/final/ore (tools/process_art.py's icon()) takes it in place of the placeholder."""
import json, os, random, shutil
from PIL import Image, ImageDraw

ROOT = "common/src/main/resources"
ASSETS = f"{ROOT}/assets/wildcatter"
DATA = f"{ROOT}/data/wildcatter"
ORE_ART = "art/final/ore"
ns = "wildcatter"
rng = random.Random(23)


def save_json(path, obj):
	os.makedirs(os.path.dirname(path), exist_ok=True)
	with open(path, "w") as f:
		json.dump(obj, f, indent=2)
		f.write("\n")


def shaped(name, pattern, key, result, count=1):
	save_json(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "redstone",
		"pattern": pattern, "key": key, "result": {"id": f"{ns}:{result}", "count": count}})


def shapeless(name, ingredients, result, count=1):
	save_json(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "redstone",
		"ingredients": ingredients, "result": {"id": f"{ns}:{result}", "count": count}})


def big_part_loot(name):
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}, {"condition": "minecraft:block_state_property", "block": f"{ns}:{name}", "properties": {"piece": "0"}}]}]})


def simple_loot(name):
	save_json(f"{DATA}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ns}:{name}"}],
		"conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def placeholder(name, colour, mark=None):
	img = Image.new("RGBA", (16, 16))
	d = ImageDraw.Draw(img)
	for x in range(16):
		for y in range(16):
			n = rng.randint(-10, 10)
			img.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in colour) + (255,))
	if mark:
		mark(d)
	path = f"{ASSETS}/textures/item/{name}_placeholder.png"
	os.makedirs(os.path.dirname(path), exist_ok=True)
	img.save(path)


def item(name, label, colour, mark=None):
	placeholder(name, colour, mark)   # drawn always, so the random sequence does not depend on which icons exist
	if os.path.exists(f"{ORE_ART}/{name}.png"):
		os.makedirs(f"{ASSETS}/textures/item/ore", exist_ok=True)
		shutil.copy(f"{ORE_ART}/{name}.png", f"{ASSETS}/textures/item/ore/{name}.png")
		layer = f"{ns}:item/ore/{name}"
	else:
		layer = f"{ns}:item/{name}_placeholder"
	save_json(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": layer}})
	save_json(f"{ASSETS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/{name}"}})
	LANG[f"item.{ns}.{name}"] = label


def tags(names):
	for t_name in ("mineable/pickaxe", "needs_stone_tool"):
		path = f"{ROOT}/data/minecraft/tags/block/{t_name}.json"
		t = json.load(open(path))
		wanted = [f"{ns}:{n}" for n in names]
		t["values"] = [v for v in t["values"] if v not in wanted] + wanted
		save_json(path, t)


LANG = json.load(open(f"{ASSETS}/lang/en_us.json"))
S_, P_ = f"{ns}:steel_ingot", f"{ns}:gas_line"

# ---- 23a: casing, cement, LCM, oil mud and the calcium chloride unit ------------------------------------------------------------
PARTS_23A = ("calcium_chloride_unit",)
for name in PARTS_23A:
	big_part_loot(name)
tags(PARTS_23A)
shaped("calcium_chloride_unit", ["SHS", "PCP", "SXS"], {"S": S_, "H": "minecraft:hopper", "P": P_, "C": "minecraft:cauldron", "X": "minecraft:piston"}, "calcium_chloride_unit")
LANG[f"block.{ns}.calcium_chloride_unit"] = "Calcium Chloride Unit (0.5 t/h flake)"
item("casing_joint", "Casing Joint (9-5/8 in, 9 m)", (120, 126, 132), lambda d: d.rectangle([6, 0, 9, 15], fill=(80, 86, 92, 255)))
item("oilwell_cement", "Bag of Oilwell Cement (class H)", (176, 176, 170))
item("lcm_bag", "Bulk Bag of LCM (sized calcium carbonate)", (226, 222, 210))
item("cacl2_bag", "Bulk Bag of Calcium Chloride Flake (77 %)", (236, 236, 236))
item("obm_package", "Bulk Bag of OBM Package (emulsifier, organoclay, lime)", (150, 120, 80))
shaped("casing_joint", ["S", "S", "S"], {"S": S_}, "casing_joint", 2)
# cement: quicklime and clay (the gypsum-free variant, plain clay doubled; the mod has no gypsum item: decisions 11)
shapeless("oilwell_cement", [f"{ns}:quicklime_bag", "minecraft:clay_ball", "minecraft:clay_ball", "minecraft:clay_ball", "minecraft:clay_ball"], "oilwell_cement")

# ---- 23b: the choke manifold, the remote choke panel and the mud-gas separator ----------------------------------------------------
PARTS_23B = ("choke_manifold", "remote_choke_panel", "mud_gas_separator")
for name in PARTS_23B:
	big_part_loot(name)
tags(PARTS_23B)
shaped("choke_manifold", ["SVS", "PVP", "SVS"], {"S": S_, "V": "minecraft:lever", "P": P_}, "choke_manifold")
shaped("remote_choke_panel", [" G ", "SCS", "SPS"], {"G": "minecraft:glass_pane", "S": S_, "C": "minecraft:comparator", "P": "minecraft:piston"}, "remote_choke_panel")
shaped("mud_gas_separator", ["SPS", "SIS", "SPS"], {"S": S_, "P": P_, "I": "minecraft:iron_bars"}, "mud_gas_separator")
LANG.update({f"block.{ns}.choke_manifold": "Choke Manifold (5,000 psi)", f"block.{ns}.remote_choke_panel": "Remote Choke Panel",
	f"block.{ns}.mud_gas_separator": "Mud-Gas Separator"})

# ---- 23c: gas storage --------------------------------------------------------------------------------------------------------------
PARTS_23C = ("storage_compressor", "glycol_dehydrator")
for name in PARTS_23C:
	big_part_loot(name)
simple_loot("storage_wellhead")
tags(PARTS_23C + ("storage_wellhead",))
shaped("storage_compressor", ["SPS", "XMX", "SIS"], {"S": S_, "P": P_, "X": "minecraft:piston", "M": f"{ns}:motor_mv" if False else "minecraft:copper_block", "I": "minecraft:iron_block"}, "storage_compressor")
shaped("glycol_dehydrator", ["SPS", "SBS", "SPS"], {"S": S_, "P": P_, "B": "minecraft:blast_furnace"}, "glycol_dehydrator")
shaped("storage_wellhead", [" P ", "SVS", " S "], {"S": S_, "P": P_, "V": "minecraft:iron_door"}, "storage_wellhead")
shaped("sonar_tool", [" G ", "SRS", " S "], {"G": "minecraft:glass_pane", "S": S_, "R": "minecraft:note_block"}, "sonar_tool")
LANG.update({f"block.{ns}.storage_compressor": "Storage Compressor (5 Sm3/s, 1.0 MW)", f"block.{ns}.glycol_dehydrator": "Glycol Dehydrator (TEG)",
	f"block.{ns}.storage_wellhead": "Gas Storage Wellhead"})
item("sonar_tool", "Sonar Survey Tool", (60, 90, 120), lambda d: d.ellipse([4, 4, 11, 11], outline=(200, 230, 255, 255)))

# ---- 23d: Frasch sulfur ------------------------------------------------------------------------------------------------------------
PARTS_23D = ("frasch_water_heater", "lift_air_compressor", "sulfur_vat")
for name in PARTS_23D:
	big_part_loot(name)
simple_loot("frasch_wellhead")
tags(PARTS_23D + ("frasch_wellhead",))
shaped("frasch_water_heater", ["SBS", "PFP", "SBS"], {"S": S_, "B": "minecraft:bricks", "P": P_, "F": "minecraft:blast_furnace"}, "frasch_water_heater")
shaped("lift_air_compressor", ["SPS", "SXS", "SIS"], {"S": S_, "P": P_, "X": "minecraft:piston", "I": "minecraft:iron_block"}, "lift_air_compressor")
shaped("sulfur_vat", ["B B", "BSB", "BBB"], {"B": "minecraft:bricks", "S": S_}, "sulfur_vat")
shaped("frasch_wellhead", ["PPP", "SVS", " S "], {"S": S_, "P": P_, "V": "minecraft:lever"}, "frasch_wellhead")
LANG.update({f"block.{ns}.frasch_water_heater": "Frasch Water Heater (165 C, 2.8 MPa)", f"block.{ns}.lift_air_compressor": "Lift Air Compressor",
	f"block.{ns}.sulfur_vat": "Sulfur Vat", f"block.{ns}.frasch_wellhead": "Frasch Wellhead"})

save_json(f"{ASSETS}/lang/en_us.json", LANG)
print("oilfield assets written")
