#!/usr/bin/env python3
"""Turns the Ludo material swatches (art/source/mat_*.webp) and decals (art/source/decal_*.webp) into the 32x32 textures
the true-scale models are drawn in (art/final/mat/). Run from the repo root, then tools/make_assets.py copies them in.

A swatch is 32 px per metre, the same density as every other Wildcatter texture. Ludo often draws a material as a grid of
tiles with grout lines between them, so a swatch is the cleanest window of the picture (the one with the least sharp edges
in it); then its edges are cross-faded so it tiles. Decals use the crop in CROP, or the whole picture.
"""
import os
from PIL import Image, ImageEnhance

SRC, OUT, SIZE = "art/source", "art/final/mat", 32
os.makedirs(OUT, exist_ok=True)

# decals: the part of each picture to use, as fractions (left, top, right, bottom); the rest use the whole picture
CROP = {"decal_nameplate": (0.0, 0.0, 0.5, 0.5)}
WINDOW = 0.34   # a material swatch is the cleanest window this big (a fraction of the picture)
DECALS = ("decal_",)


def cleanest(img):
	"""The window of the picture with the least edge energy: no grout lines, no tile borders."""
	from PIL import ImageFilter
	small = img.resize((96, 96), Image.BOX).convert("L").filter(ImageFilter.FIND_EDGES)
	px = small.load()
	w = int(96 * WINDOW)
	best, at = None, (0, 0)
	for y in range(2, 96 - w - 2, 2):
		for x in range(2, 96 - w - 2, 2):
			e = sum(px[i, j] for i in range(x, x + w, 2) for j in range(y, y + w, 2))
			if best is None or e < best:
				best, at = e, (x, y)
	k = img.width / 96
	return img.crop((int(at[0] * k), int(at[1] * k), int((at[0] + w) * k), int((at[1] + w) * k)))


def tileable(img, band=4):
	"""Cross-fades each edge with the opposite one so the swatch repeats without a seam."""
	px = img.load()
	w, h = img.size
	out = img.copy()
	o = out.load()
	for y in range(h):
		for i in range(band):
			k = 0.5 * (1 - i / band)
			a, b = px[i, y], px[w - 1 - i, y]
			o[i, y] = tuple(int(x * (1 - k) + z * k) for x, z in zip(a, b))
			o[w - 1 - i, y] = tuple(int(z * (1 - k) + x * k) for x, z in zip(a, b))
	img = out.copy()
	px = img.load()
	for x in range(w):
		for i in range(band):
			k = 0.5 * (1 - i / band)
			a, b = px[x, i], px[x, h - 1 - i]
			o[x, i] = tuple(int(p * (1 - k) + q * k) for p, q in zip(a, b))
			o[x, h - 1 - i] = tuple(int(q * (1 - k) + p * k) for p, q in zip(a, b))
	return out


for f in sorted(os.listdir(SRC)):
	name, ext = os.path.splitext(f)
	if ext != ".webp" or not name.startswith(("mat_", "decal_")):
		continue
	img = Image.open(f"{SRC}/{f}").convert("RGB")
	if name.startswith(DECALS):
		l, t, r, b = CROP.get(name, (0.0, 0.0, 1.0, 1.0))
		img = img.crop((int(l * img.width), int(t * img.height), int(r * img.width), int(b * img.height)))
	else:
		img = cleanest(img)
	img = img.resize((SIZE, SIZE), Image.BOX)
	img = ImageEnhance.Contrast(img).enhance(1.1)
	if not name.startswith(DECALS):
		img = tileable(img)
	img.convert("RGBA").save(f"{OUT}/{name}.png")
	print(name)
