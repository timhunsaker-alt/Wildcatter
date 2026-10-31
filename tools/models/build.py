#!/usr/bin/env python3
"""Builds every true-scale part model (tools/models/<part>.py) and a preview of each in build/model-previews/.

Run from the repo root: python3 tools/models/build.py [part ...]. Each part script makes a module-level Model called M.
"""
import importlib, os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mlib

PARTS = ["gsu_transformer", "fuel_tank", "genset_base_frame", "radiator", "genset_diesel_engine", "genset_gas_engine", "alternator",
	"genset_control_panel", "exhaust_stack", "exhaust_silencer_large",
	"boiler_burner", "boiler_drum", "superheater", "turbine_casing", "turbine_generator", "condenser", "feed_pump",
	"tower_basin", "cooling_fill", "drift_eliminator", "cooling_tower",
	"hv_breaker_3p", "disconnect_hv", "bus_hv", "power_transformer", "bus_dist", "disconnect_dist", "dist_breaker_3p", "relay_panel"]


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
	for name in [n for n in names if n != "rotors"] or (PARTS if not names else []):
		model = importlib.import_module(name).M
		model.export()
		model.preview(os.path.join(previews, name + ".png"), colours(model))
	if not names or "rotors" in names:
		for model in importlib.import_module("rotors").ROTORS:
			model.export_single(model.name[len("rotor_"):])
			model.preview(os.path.join(previews, model.name + ".png"), colours(model))


if __name__ == "__main__":
	main(sys.argv[1:])
