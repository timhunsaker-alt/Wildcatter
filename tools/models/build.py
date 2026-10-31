#!/usr/bin/env python3
"""Builds every true-scale part model (tools/models/<part>.py) and a preview of each in build/model-previews/.

Run from the repo root: python3 tools/models/build.py [part ...]. Each part script makes a module-level Model called M.
"""
import importlib, os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mlib

PARTS = ["gsu_transformer"]


def colours(model):
	out = {}
	for short, path in model.textures.items():
		png = os.path.join(mlib.ASSETS, "textures", path + ".png")
		if os.path.exists(png):
			img = Image.open(png).convert("RGB").resize((1, 1), Image.BOX)
			out[short] = img.getpixel((0, 0))
	return out


def main(names):
	previews = os.path.join(mlib.ROOT, "build/model-previews")
	os.makedirs(previews, exist_ok=True)
	for name in names or PARTS:
		model = importlib.import_module(name).M
		model.export()
		model.preview(os.path.join(previews, name + ".png"), colours(model))


if __name__ == "__main__":
	main(sys.argv[1:])
