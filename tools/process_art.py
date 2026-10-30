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
save("gas_line", hue_shift(load("pipe"), 0.07, 1.4))   # the same pipe, painted orange

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
if os.path.exists(f"{SRC}/breaker.webp"):
	save("breaker_body", load("breaker"))
print("art processed:", len(os.listdir(OUT)), "textures")


# ---- item icons (Ludo drew them on a transparent background) --------------------------------------------
def icon(name):
	img = Image.open(f"{SRC}/{name}.webp").convert("RGBA")
	if img.getpixel((0, 0))[3] == 255:   # drawn on a plain background: make that transparent
		from PIL import ImageDraw
		for corner in ((0, 0), (img.width - 1, 0), (0, img.height - 1), (img.width - 1, img.height - 1)):
			ImageDraw.floodfill(img, corner, (0, 0, 0, 0), thresh=40)
	img = img.crop(img.getbbox())
	side = max(img.size)
	square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
	square.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
	return square.resize((SIZE, SIZE), Image.BOX)


for name in ("power_meter", "field_manual", "seismograph", "crude_oil_bucket"):
	icon(name).save(f"{OUT}/{name}.png")
print("icons done")
