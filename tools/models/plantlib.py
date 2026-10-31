"""Shared by the steam plant and cooling tower parts (docs/multiblocks/steam-plant.md, cooling-tower.md): the materials.

Every part is drawn facing north in its own frame, its main cell at its north-west bottom corner (x east, y up, z south).
The plant's turbine-generator shaft runs along x, 0.85 m up, along the middle of their 2 m skids (z = 1.0).
"""
import mlib
import gensetlib

TEX = dict(gensetlib.TEX)
TEX.update({
	"concrete": "block/mat/mat_concrete",
	"polymer": "block/mat/mat_polymer_grey",
	"padgreen": "block/mat/mat_paint_pad_green",
})

SHAFT_Y = 0.85
SHAFT_Z = 1.0


def model(name, length, height, width, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(length, height, width, 0, 0), dict(TEX), TEX[particle])


def skid(M, x1, x2, z1, z2, top=0.22):
	"""A steel base frame: two channels along x and cross members."""
	for z in (z1, z2 - 0.16):
		M.box((x1, 0, z), (x2, top, z + 0.16), "iron")
	k = int((x2 - x1) / 0.7) + 1
	for i in range(k + 1):
		x = x1 + 0.05 + i * (x2 - x1 - 0.1) / max(1, k)
		M.box((x - 0.05, 0.02, z1 + 0.16), (x + 0.05, top - 0.02, z2 - 0.16), "iron")


def coupling_guard(M, x1, x2, centre, radius, tex="engine"):
	"""An open guard over a shaft coupling: two hoops and four bars, so the coupling shows turning."""
	M.ring("x", centre, radius, x1, x1 + 0.03, 0.025, tex, sides=16)
	M.ring("x", centre, radius, x2 - 0.03, x2, 0.025, tex, sides=16)
	M.ring("x", centre, radius, x1 + 0.03, x2 - 0.03, 0.025, tex, sides=16, skip=tuple(k for k in range(16) if k % 4))
