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
	"hv_breaker_3p", "disconnect_hv", "bus_hv", "power_transformer", "bus_dist", "disconnect_dist", "dist_breaker_3p", "relay_panel",
	"wood_pole_30", "wood_pole_40", "wood_pole_75", "crossarm_dist", "crossbeam_138", "x_brace_138", "pole_transformer", "fused_cutout",
	"padmount_208", "padmount_480", "unit_substation", "enclosure", "fixtures", "mcc", "starter_plant",
	"drilling_rig", "pumpjack", "wellheads", "land_rig", "lease", "gas_plant", "crude_units", "cracker", "gas_turbine", "combined_cycle", "aeroderivative"]


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
		module = importlib.import_module(name)
		model = module.M
		model.export(**getattr(module, "EXPORT", {}))   # a one-cell part with other states writes its own blockstate
		model.preview(os.path.join(previews, name + ".png"), colours(model))
		for extra in getattr(module, "EXTRA", []):      # another look of the same block (the blown cutout)
			extra.export(**getattr(extra, "export_args", {"blockstate": False, "item": False}))
			extra.preview(os.path.join(previews, extra.name + ".png"), colours(extra))
		if hasattr(module, "after"):                    # a block with more states than facing and piece writes its own blockstate
			module.after()
	if not names or "rotors" in names:
		for model in importlib.import_module("rotors").ROTORS:
			model.export_single(model.name[len("rotor_"):])
			model.preview(os.path.join(previews, model.name + ".png"), colours(model))


if __name__ == "__main__":
	main(sys.argv[1:])
