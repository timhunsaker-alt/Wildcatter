#!/usr/bin/env python3
"""Turns the Ludo images in art/source into the 32x32 block textures in art/final.

Ludo drew one image per face. The lit / running / MV versions are made here from those, so every state of a
block comes from the same drawing and nothing jumps when it switches on. Run from the repo root with Pillow
installed: python3 tools/process_art.py. Then python3 tools/make_assets.py copies art/final into the mod.
"""
import colorsys, os
from PIL import Image, ImageEnhance, ImageFilter, ImageChops

SRC, OUT, SIZE = "art/source", "art/final", 32
os.makedirs(OUT, exist_ok=True)


def load(name):
	img = Image.open(f"{SRC}/{name}.webp").convert("RGB")
	# Box-filter down to the block size, then a touch of contrast so the details survive.
	img = img.resize((SIZE, SIZE), Image.BOX)
	return ImageEnhance.Contrast(img).enhance(1.15)


def save(name, img):
	img.convert("RGBA").save(f"{OUT}/{name}.png")


def hue_shift(img, to_hue, sat=1.0):
	"""Recolours the greys of an image toward a hue (for the blue substation transformer)."""
	px = img.load()
	out = Image.new("RGB", img.size)
	o = out.load()
	for x in range(img.width):
		for y in range(img.height):
			r, g, b = px[x, y]
			h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
			if s < 0.25:  # only the steel; leave brass, copper and red alone
				h, s = to_hue, min(1.0, 0.45 * sat + s)
			o[x, y] = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(h, s, v))
	return out


def glow(img, color, strength, where=None):
	"""Adds a coloured glow. `where` is a function (x, y) -> 0..1 that says where it is strongest."""
	out = img.copy()
	px, o = img.load(), out.load()
	for x in range(img.width):
		for y in range(img.height):
			k = strength * (where(x / img.width, y / img.height) if where else 1.0)
			r, g, b = px[x, y]
			o[x, y] = tuple(min(255, int(c + (t - c) * k * 0.55 + t * k * 0.25)) for c, t in zip((r, g, b), color))
	return out


def brighten_hot(img, strength):
	"""Makes the orange/copper pixels glow (heating coils, element bars) and leaves the steel alone."""
	out = img.copy()
	px, o = img.load(), out.load()
	for x in range(img.width):
		for y in range(img.height):
			r, g, b = px[x, y]
			h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
			if 0.0 <= h <= 0.12 and s > 0.35:
				v = min(1.0, v + strength)
				s = max(0.0, s - strength * 0.4)
			o[x, y] = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(h, s, v))
	return out


def dim_hot(img):
	"""The opposite: a cold element (heater off) is a dull dark red-brown."""
	out = img.copy()
	px, o = img.load(), out.load()
	for x in range(img.width):
		for y in range(img.height):
			r, g, b = px[x, y]
			h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
			if 0.0 <= h <= 0.14 and s > 0.35:
				v *= 0.45
				s *= 0.6
			o[x, y] = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(h, s, v))
	return out


def spin_blur(img, turns=6, degrees=18):
	"""A rotor at speed: the face averaged over a few small rotations."""
	acc = [img.rotate(-degrees * i / turns, resample=Image.BICUBIC) for i in range(turns + 1)]
	out = acc[0]
	for i, frame in enumerate(acc[1:], start=1):
		out = Image.blend(out, frame, 1.0 / (i + 1))
	return out


def darker(img, k):
	return ImageEnhance.Brightness(img).enhance(k)


# ---- boiler ---------------------------------------------------------------------------------------------
side = load("boiler_side_2")
top = load("boiler_top_2")
save("boiler_side", side)
save("boiler_side_lit", glow(side, (255, 140, 40), 0.9, lambda x, y: (0.25 + 0.75 * y) * (1 - abs(x - 0.5))))
save("boiler_top", top)
save("boiler_bottom", darker(side, 0.55))

# the coil boiler (27 boilers over 9 fireboxes): copper heating coils; lit, they glow
coil = load("coil_side_0")
save("boiler_formed_side", coil)
save("boiler_formed_side_lit", brighten_hot(coil, 0.3))

# ---- firebox, pump and pipe ------------------------------------------------------------------------------
fire = load("firebox")
save("firebox_side", dim_hot(fire))
save("firebox_side_lit", brighten_hot(fire, 0.3))
save("firebox_top", darker(side, 0.7))
save("pump", load("pump"))
save("pipe", load("pipe"))

# ---- oil and gas wells ---------------------------------------------------------------------------------
save("drilling_rig", load("drilling_rig"))
save("pumpjack", load("pumpjack"))
save("gas_wellhead", load("gas_wellhead"))
save("casing", load("casing"))
save("cooling_tower", load("cooling_tower"))
save("cooling_fill", load("cooling_fill"))
de = load("diesel_engine")   # the old 120 V engine's drawing now dresses the genset engines
save("genset_engine", de)
save("genset_gas_engine", hue_shift(de, 0.58))
save("gas_line", load("steel_pipe"))   # steel pipe for oil and gas (the block id stays gas_line)

# ---- turbine and generator ------------------------------------------------------------------------------
tside = load("turbine_side_1")
tend = load("turbine_side_2")  # Ludo drew the rotor face on this one
save("steam_turbine_side", tside)
save("steam_turbine_side_running", tside)
save("steam_turbine_end", tend)
save("steam_turbine_end_running", spin_blur(tend))
gen = load("gen_side_1")
gend = hue_shift(tend, 0.60, 1.0)  # a blue-steel rotor face for the medium voltage generator
save("generator_side", gen)
save("generator_side_running", brighten_hot(gen, 0.25))
save("generator_end", gend)
save("generator_end_running", spin_blur(gend))

# ---- loads ----------------------------------------------------------------------------------------------
lamp = load("lamp_2")
save("electric_lamp", lamp)
save("electric_lamp_lit", glow(lamp, (255, 238, 150), 0.85))
heater = load("heater_1")
save("electric_heater", dim_hot(heater))
save("electric_heater_lit", brighten_hot(heater, 0.2))

# ---- transformers and breaker --------------------------------------------------------------------------
tf, ts = load("xfmr_front_2"), load("xfmr_side_2")
save("transformer_front", tf)
save("transformer_side", ts)
save("transformer_top", darker(ts, 0.85))
save("substation_front", hue_shift(tf, 0.60))
save("substation_side", hue_shift(ts, 0.60))
save("substation_top", darker(hue_shift(ts, 0.60), 0.85))

# ---- Phase 3: cable, three-pole breakers, pad-mounts, unit substation, heater, starters and the MCC ------------
def tint(img, color, k):
	"""Blends a drawing toward a colour (cable jackets, the industrial breaker's orange case)."""
	return Image.blend(img, Image.new("RGB", img.size, color), k)


def lamp(img, color):
	"""Paints the pilot light (upper left of the door) lit in a colour, with a little glow round it."""
	out = glow(img, color, 0.35, lambda x, y: max(0.0, 1 - ((x - 0.2) ** 2 + (y - 0.2) ** 2) * 18))
	px = out.load()
	for x in range(5, 9):
		for y in range(5, 9):
			px[x, y] = color if 5 < x < 8 or 5 < y < 8 else tuple(int(c * 0.7) for c in color)
	return out


if os.path.exists(f"{SRC}/cable_sheath.webp"):
	sheath = load("cable_sheath")
	for cable, (color, k) in {"cable_lv": ((40, 40, 44), 0.0), "cable_industrial": ((90, 60, 40), 0.25), "cable_dist": ((30, 70, 45), 0.25), "cable_hv": ((10, 10, 16), 0.35)}.items():
		save(cable, tint(sheath, color, k))
if os.path.exists(f"{SRC}/breaker_3p.webp"):
	b3 = load("breaker_3p")
	save("breaker_3p_lv_body", b3)
	save("breaker_3p_industrial_body", tint(b3, (210, 120, 40), 0.35))
if os.path.exists(f"{SRC}/padmount_front.webp"):
	pf, ps = load("padmount_front"), load("padmount_side")
	for name, k in (("padmount_208", 1.0), ("padmount_480", 0.85)):   # the 480 V unit a shade darker
		save(f"{name}_front", darker(pf, k))
		save(f"{name}_side", darker(ps, k))
		save(f"{name}_top", darker(ps, k * 0.8))
if os.path.exists(f"{SRC}/unit_substation_front.webp"):
	uf, us = load("unit_substation_front"), load("unit_substation_side")
	save("unit_substation_front", uf)
	save("unit_substation_side", us)
	save("unit_substation_top", darker(us, 0.8))
if os.path.exists(f"{SRC}/three_phase_heater.webp"):
	h3 = load("three_phase_heater")
	save("three_phase_heater", dim_hot(h3))
	save("three_phase_heater_lit", brighten_hot(h3, 0.25))
if os.path.exists(f"{SRC}/cabinet_front.webp"):
	cf = load("cabinet_front")
	save("cabinet_side", load("cabinet_side"))
	lamps = {"running": (60, 220, 90), "on": (60, 220, 90), "stopped": (90, 90, 96), "off": (90, 90, 96), "tripped": (235, 50, 40)}
	for name, states in (("motor_starter", ("running", "stopped", "tripped")), ("mcc_bucket", ("running", "stopped", "tripped")), ("mcc_incoming", ("on", "off", "tripped"))):
		door = cf if name != "mcc_incoming" else darker(cf, 0.9)
		for st in states:
			save(f"{name}_front_{st}", lamp(door, lamps[st]) if lamps[st] != (90, 90, 96) else door)

# ---- Phase 4b: genset parts ----------------------------------------------------------------------------------------
if os.path.exists(f"{SRC}/alternator.webp"):
	save("alternator", load("alternator"))
	save("radiator_core", load("radiator_core"))
	save("radiator_frame", darker(load("exhaust_stack"), 0.8))
	save("fuel_tank", load("fuel_tank"))
	save("exhaust_stack", load("exhaust_stack"))
	save("genset_panel_front", load("genset_panel_front"))
	save("genset_panel_side", load("cabinet_side"))

# ---- Phase 4d and 4e: plant, tower, busway, substation ----------------------------------------------------------------
for name in ("busway", "condenser", "tower_basin", "drift_eliminator", "station_bus", "disconnect_post"):
	if os.path.exists(f"{SRC}/{name}.webp"):
		save(name, load(name))

# ---- Phase 10d4: wires, ground, the one-block breaker, switch, relay and fuse, and the 4.16 kV cable -------------------
# Every wire is one of a few Ludo materials (bare copper, aluminium or steel strand, braided wool, PVC jacket), banded in its
# conductor's colour as the script drawing always was (A red, B orange, C blue, N black, G green). Same names as make_assets.py
# draws, so these replace them.
CONDUCTOR_COLORS = {"a": (200, 50, 45), "b": (230, 140, 40), "c": (50, 90, 200), "n": (25, 25, 28), "g": (40, 150, 70)}


def banded(img, band, rows):
	out = img.copy()
	px = out.load()
	for y in rows:
		for x in range(SIZE):
			px[x, y] = band
	return out


def holes(base, through, share, seed):
	"""Wool worn through: about {share} of the pixels show the copper under it (a fixed seed, so every run is the same)."""
	import random
	rng = random.Random(seed)
	out = base.copy()
	px, cu = out.load(), through.load()
	for x in range(SIZE):
		for y in range(SIZE):
			if rng.random() < share:
				px[x, y] = cu[x, y]
	return out


if os.path.exists(f"{SRC}/wire_copper.webp"):
	copper, alu, steel = load("wire_copper"), load("wire_aluminium"), load("wire_steel")
	wool_base = load("wire_wool")
	# the jacket: its faint extrusion lines brought up, and lit like a round wire (light along the top, shadow along the bottom)
	pvc = ImageEnhance.Contrast(load("wire_pvc")).enhance(2.5)
	pvc = glow(pvc, (255, 255, 255), 0.35, lambda x, y: max(0.0, 1 - y * 4))
	pvc = Image.composite(darker(pvc, 0.75), pvc, Image.linear_gradient("L").resize((SIZE, SIZE)).point(lambda v: 255 if v > 215 else 0))
	metals = {"": copper, "industrial_": darker(copper, 0.92), "mv_": alu, "dist_": tint(alu, (178, 164, 150), 0.25), "hv_": steel}
	tiers = {"": (235, 235, 240), "mv_": (225, 230, 245), "dist_": (225, 240, 230), "hv_": (215, 225, 250), "industrial_": (240, 225, 205)}
	for key, band in CONDUCTOR_COLORS.items():
		for prefix, metal in metals.items():
			save(f"{prefix}wire_{key}", banded(metal, band, (10, 11, 20, 21)))
		dye = band if key != "n" else (45, 45, 50)
		cloth = ImageChops.multiply(wool_base, Image.new("RGB", wool_base.size, tuple(min(255, int(c * 0.85 + 60)) for c in dye)))   # dyed, the braid kept
		for wear, share in ((0, 0.0), (1, 0.22), (2, 0.45)):
			save(f"wool_wire_{key}_{wear}", holes(cloth, copper, share, "abcng".index(key) * 10 + wear))
		save(f"wool_wire_{key}_3", banded(copper, band if key != "n" else (45, 45, 50), (10, 11, 20, 21)))   # worn to bare copper
		for prefix, jacket in tiers.items():
			sheath = ImageChops.multiply(pvc, Image.new("RGB", pvc.size, jacket)) if key != "n" else tint(pvc, (60, 60, 66), 0.8)
			save(f"{prefix}plastic_wire_{key}", banded(sheath, band, (14, 15, 16, 17)))
	# the ground wire: green-and-yellow insulated (as IEC marks it, and as the game has always shown G); the rod copper-clad steel
	save("ground_wire", banded(tint(pvc, (40, 150, 70), 0.7), (240, 220, 60), (8, 9, 22, 23)))
if os.path.exists(f"{SRC}/ground_rod.webp"):
	save("ground_rod", load("ground_rod"))
if os.path.exists(f"{SRC}/breaker.webp"):
	# Ludo drew the breaker on white: crop to the breaker itself, so its face fills the block's
	face = Image.open(f"{SRC}/breaker.webp").convert("RGB")
	dark = face.convert("L").point(lambda v: 255 if v < 200 else 0)
	body = ImageEnhance.Contrast(face.crop(dark.getbbox()).resize((SIZE, SIZE), Image.BOX)).enhance(1.15)
	save("breaker_body", body)
	save("switch_body", tint(body, (200, 170, 40), 0.5))   # the plain switch yellow, the UFLS relay purple, as before
	save("relay_body", tint(body, (150, 70, 190), 0.5))
if os.path.exists(f"{SRC}/fuse_cartridge.webp"):
	full = Image.open(f"{SRC}/fuse_cartridge.webp").convert("RGB")
	w, h = full.size

	def part(box):
		return ImageEnhance.Contrast(full.crop(box).resize((SIZE, SIZE), Image.BOX)).enhance(1.15)

	tube = part((int(w * 0.3), int(h * 0.2), int(w * 0.7), int(h * 0.8)))   # the fibre tube, from its middle
	save("fuse_tube", tube)
	save("fuse_tube_blown", tint(darker(tube, 0.45), (30, 24, 20), 0.35))   # scorched dark when the element has melted
	save("fuse_cap", part((0, int(h * 0.2), int(w * 0.09), int(h * 0.8))))   # a brass end cap
if os.path.exists(f"{SRC}/cable_sheath.webp"):
	save("cable_mv", tint(load("cable_sheath"), (40, 46, 60), 0.25))   # the 4.16 kV cable, between the LV and the distribution jacket

print("art processed:", len(os.listdir(OUT)), "textures")


# ---- item icons (Ludo drew them on a transparent background) --------------------------------------------
def icon(name, src=SRC):
	img = Image.open(f"{src}/{name}.webp").convert("RGBA")
	if img.getpixel((0, 0))[3] == 255:   # drawn on a plain background: make that transparent
		from PIL import ImageDraw
		for corner in ((0, 0), (img.width - 1, 0), (0, img.height - 1), (img.width - 1, img.height - 1)):
			ImageDraw.floodfill(img, corner, (0, 0, 0, 0), thresh=40)
	img = img.crop(img.getbbox())
	side = max(img.size)
	square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
	square.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
	return square.resize((SIZE, SIZE), Image.BOX)


for name in ("power_meter", "field_manual", "seismograph", "crude_oil_bucket", "plastic_sheet", "diesel_bucket", "steel_ingot"):
	icon(name).save(f"{OUT}/{name}.png")
GRID_ICONS = ("branch_breaker", "gfci_breaker", "panelboard_breaker", "fuse_class_j", "fuse_e", "grounding_set")   # 10d4
for name in GRID_ICONS:
	if os.path.exists(f"{SRC}/{name}.webp"):
		icon(name).save(f"{OUT}/{name}.png")
GENSET_PART_ICONS = ("engine_block_casting", "piston", "crankshaft", "cylinder_head", "fuel_injector", "spark_plug", "stator_winding", "rotor_field", "exciter")
for name in GENSET_PART_ICONS:
	if os.path.exists(f"{SRC}/{name}.webp"):
		icon(name).save(f"{OUT}/{name}.png")
# 7a: mineral, ingot and scintillometer icons (art/source/ore/<name>.webp, not the *_ore block pictures)
if os.path.isdir(f"{SRC}/ore"):
	os.makedirs(f"{OUT}/ore", exist_ok=True)
	for f in sorted(os.listdir(f"{SRC}/ore")):
		name, ext = os.path.splitext(f)
		if ext == ".webp" and not name.endswith("_ore"):
			icon(name, f"{SRC}/ore").save(f"{OUT}/ore/{name}.png")
print("icons done")
