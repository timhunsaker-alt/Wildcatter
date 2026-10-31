"""Shared by the substation yard's parts (docs/multiblocks/substation.md): materials, post insulators and bushings by voltage
class, and galvanized structures.

Every part is drawn facing north in its own frame: x along the line (138 kV west, 12.47 kV east), z across it, its main cell at
the north-west bottom corner. The 138 kV parts are 9 m across with their phases at z 1.5, 4.5 and 7.5 (3 m spacing); the
12.47 kV parts 3 m across with their phases at z 0.5, 1.5 and 2.5; the transformer's bushings are 1 m apart. The 138 kV
conductors run 5.55 m up, the 12.47 kV ones 3.75 m up.
"""
import math
import plantlib as p

TEX = dict(p.TEX)
TEX.update({"porcelain": "block/mat/mat_porcelain_brown", "relays": "block/mat/decal_relays"})

HV_PHASES = (1.5, 4.5, 7.5)
DIST_PHASES = (0.5, 1.5, 2.5)
HV_Y = 5.55
DIST_Y = 3.75


def model(name, length, height, width, particle="galv"):
	return p.mlib.Model(name, p.mlib.footprint_box(length, height, width, 0, 0), dict(TEX), TEX[particle])


def post_insulator(M, x, z, y0, height, radius, sheds, shed_r, tex="porcelain"):
	"""A station post insulator: its base flange, the core, the sheds and the cap. Returns the top's height."""
	M.cylinder("y", (x, z), radius + 0.03, y0, y0 + 0.04, "galv", 12)
	M.cylinder("y", (x, z), radius, y0 + 0.04, y0 + height - 0.04, tex, 12)
	for k in range(sheds):
		y = y0 + 0.08 + k * (height - 0.16) / max(1, sheds - 1)
		M.cylinder("y", (x, z), shed_r, y, y + 0.025, tex, 12)
	M.cylinder("y", (x, z), radius + 0.02, y0 + height - 0.04, y0 + height, "galv", 12)
	return y0 + height


def hv_post(M, x, z, y0):
	"""A 138 kV station post (650 kV BIL): about 1.5 m with 12 sheds."""
	return post_insulator(M, x, z, y0, 1.5, 0.07, 12, 0.13)


def dist_post(M, x, z, y0):
	"""A 15 kV station post (110 kV BIL): about 0.4 m with 4 sheds."""
	return post_insulator(M, x, z, y0, 0.4, 0.05, 4, 0.09)


def column(M, x, z, y0, y1, width=0.3):
	"""A galvanized lattice column: four corner angles with cross braces every 0.6 m, on its base plate."""
	h = width / 2
	M.box((x - h - 0.05, y0, z - h - 0.05), (x + h + 0.05, y0 + 0.04, z + h + 0.05), "galv")
	for dx in (-h, h - 0.04):
		for dz in (-h, h - 0.04):
			M.box((x + dx, y0, z + dz), (x + dx + 0.04, y1, z + dz + 0.04), "galv")
	y = y0 + 0.3
	while y < y1 - 0.1:
		for a, b in (((x - h, y, z - h), (x + h, y + 0.03, z - h + 0.02)), ((x - h, y, z + h - 0.02), (x + h, y + 0.03, z + h)),
				((x - h, y, z - h), (x - h + 0.02, y + 0.03, z + h)), ((x + h - 0.02, y, z - h), (x + h, y + 0.03, z + h))):
			M.box(a, b, "galv")
		y += 0.6


def beam(M, x1, x2, z1, z2, y0, y1):
	"""A galvanized box truss: top and bottom chords and verticals."""
	for y in (y0, y1 - 0.06):
		for x in (x1, x2 - 0.06):
			M.box((x, y, z1), (x + 0.06, y + 0.06, z2), "galv")
	k = int((z2 - z1) / 0.5)
	for i in range(k + 1):
		z = z1 + i * (z2 - z1 - 0.04) / max(1, k)
		for x in (x1, x2 - 0.04):
			M.box((x, y0 + 0.06, z), (x + 0.04, y1 - 0.06, z + 0.04), "galv")
		M.box((x1, y1 - 0.06, z), (x2, y1 - 0.02, z + 0.04), "galv")


def tube(M, axis, centre, start, end, radius=0.055):
	"""Aluminium bus tube (4 in. schedule 40 is about 0.11 m across)."""
	M.cylinder(axis, centre, radius, start, end, "steel", 8)


def bushing(M, x, z, y0, height, radius, sheds, shed_r):
	"""A transformer or breaker bushing: its mounting flange and current-transformer pocket, porcelain with sheds, the terminal."""
	M.cylinder("y", (x, z), radius + 0.08, y0, y0 + 0.12, "grey", 12)
	return post_insulator(M, x, z, y0 + 0.12, height - 0.12, radius, sheds, shed_r)
