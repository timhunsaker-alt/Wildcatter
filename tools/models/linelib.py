"""Shared by the overhead line's parts (docs/multiblocks/poles.md) and the pad-mounts (docs/multiblocks/padmount.md): materials,
tapered wood poles, timbers and struts at any angle.

Every part is drawn facing north in its own frame (x east, y up, z south), its main cell at [0, 1] on every axis. A pole stands
in the middle of its cell; its top is 0.3 m into the cell above its top cell, so a crossarm in the top cell's row sits 0.3 m
below the pole top with its top flush with the cell above's floor.
"""
import math
import mlib
import plantlib as p

TEX = dict(p.TEX)
TEX.update({
	"porcelain": "block/mat/mat_porcelain_brown",
	"creosote": "block/mat/mat_wood_creosote",
	"fir": "block/mat/mat_wood_fir",
})

POLE_TOP_ABOVE = 0.3   # the pole top stands this far into the cell above its top cell


def model(name, cells, particle="creosote"):
	return mlib.Model(name, cells, dict(TEX), TEX[particle])


def wood_pole(M, x, z, cells, top_d, ground_d, steps=True):
	"""A tapered wood pole standing `cells` cells, its top POLE_TOP_ABOVE into the cell above: one many-sided section a metre,
	narrowing from its ground-line diameter to its top diameter. Pole steps from 2.5 m, the brand tag at about 3 m, the ground
	wire moulding down the north side, and the roof cap."""
	top = cells + POLE_TOP_ABOVE
	r = lambda y: (ground_d + (top_d - ground_d) * y / top) / 2
	y = 0.0
	while y < top - 1e-6:
		y2 = min(top, math.floor(y) + 1.0)
		M.cylinder("y", (x, z), (r(y) + r(y2)) / 2, y, y2, "creosote", 10)
		y = y2
	M.cylinder("y", (x, z), top_d / 2 + 0.01, top - 0.02, top + 0.005, "galv", 10)   # the galvanized roof cap
	M.box((x - 0.012, 0.0, z - r(0) - 0.012), (x + 0.012, top - 0.3, z - r(top) + 0.004), "copper")   # ground wire moulding
	M.decal((x - 0.04, 2.9, z + r(2.9) - 0.002), (x + 0.04, 3.0, z + r(2.9) + 0.004), "nameplate", "south", "galv")   # brand tag
	if steps:
		k = 0
		y = 2.5
		while y < top - 1.2:
			side = 1 if k % 2 else -1
			rr = r(y)
			M.box((x + side * rr, y, z - 0.012), (x + side * (rr + 0.17), y + 0.025, z + 0.012), "galv")
			M.box((x + side * (rr + 0.15), y, z - 0.012), (x + side * (rr + 0.17), y + 0.07, z + 0.012), "galv")
			y += 0.45
			k += 1
	M.collide((x - 0.15, 0.0, z - 0.15), (x + 0.15, cells, z + 0.15))


def strut(M, a, b, width, depth, tex, axis="z", pieces=None):
	"""A straight timber or brace from a to b in one plane (axis z: in the x-y plane, axis x: in the z-y plane), cut into
	pieces under a metre so each is drawn by the cell it lies in. width across the plane, depth along the axis."""
	if axis == "z":
		du, dv = b[0] - a[0], b[1] - a[1]
	else:
		du, dv = b[2] - a[2], b[1] - a[1]
	length = math.hypot(du, dv)
	deg = math.degrees(math.atan2(dv, du))
	n = pieces or max(1, math.ceil(length / 0.9))
	for k in range(n):
		t = (k + 0.5) / n
		c = [a[i] + (b[i] - a[i]) * t for i in range(3)]
		h = length / n / 2 + 0.002
		if axis == "z":
			M.box((c[0] - h, c[1] - width / 2, c[2] - depth / 2), (c[0] + h, c[1] + width / 2, c[2] + depth / 2), tex, rot=("z", deg, tuple(c)))
		else:
			M.box((c[0] - depth / 2, c[1] - width / 2, c[2] - h), (c[0] + depth / 2, c[1] + width / 2, c[2] + h), tex, rot=("x", -deg, tuple(c)))
