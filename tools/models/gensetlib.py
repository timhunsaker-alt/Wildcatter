"""Shared by the genset skid parts (docs/multiblocks/genset.md): the materials, and the shaft line they all line up on.

Every part is drawn facing north in its own frame, with its main cell at the radiator end of the front row's bottom. The
shaft runs along x at z = 1.0 (the middle of the 2 m skid) and 0.55 m above the base, which is y = 0.55 in the parts that
stand on the base (their frame starts at the top of the base, 1 m up).
"""
import mlib

TEX = {
	"engine": "block/mat/mat_paint_engine",
	"gasgreen": "block/mat/mat_paint_pad_green",
	"grey": "block/mat/mat_paint_ansi61",
	"blue": "block/mat/mat_paint_ansi70",
	"iron": "block/mat/mat_cast_iron",
	"dark": "block/mat/mat_oil_dark",
	"galv": "block/mat/mat_galvanized",
	"steel": "block/mat/mat_stainless",
	"rubber": "block/mat/mat_rubber",
	"copper": "block/mat/mat_copper",
	"brass": "block/mat/mat_brass",
	"lagging": "block/mat/mat_aluminium_lagging",
	"checker": "block/mat/mat_checker_plate",
	"nameplate": "block/mat/decal_nameplate",
	"danger": "block/mat/decal_danger_hv",
	"gauge": "block/mat/decal_gauge",
	"controller": "block/mat/decal_controller",
	"engineplate": "block/mat/decal_engine_plate",
}

SHAFT_Y = 0.55
SHAFT_Z = 1.0


def model(name, length, height, particle="grey", width=2):
	return mlib.Model(name, mlib.footprint_box(length, height, width, 0, 0), dict(TEX), TEX[particle])


def mount(M, x, z, top=0.0):
	"""An anti-vibration mount: steel plates with a rubber block between, its top at `top` (the part's foot)."""
	M.box((x - 0.09, top - 0.15, z - 0.09), (x + 0.09, top - 0.12, z + 0.09), "galv")
	M.box((x - 0.07, top - 0.12, z - 0.07), (x + 0.07, top - 0.03, z + 0.07), "rubber")
	M.box((x - 0.09, top - 0.03, z - 0.09), (x + 0.09, top, z + 0.09), "galv")
	M.cylinder("y", (x, z), 0.02, top - 0.06, top - 0.002, "galv", 6)


def feet(M, xs, zs, top, tex):
	"""Cast feet from a part's body down to its frame's top (y = 0)."""
	for x in xs:
		for z in zs:
			M.box((x - 0.08, 0, z - 0.06), (x + 0.08, top, z + 0.06), tex)
			M.box((x - 0.11, 0, z - 0.09), (x + 0.11, 0.03, z + 0.09), tex)
