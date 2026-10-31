"""Shared by the building gear (docs/multiblocks/building-gear.md): materials, enclosures, conduit, Unistrut, and drawing on any
face of a cell.

In-line gear (in at the back, out at the front) is drawn facing north: its line runs from the south face (in) to the north face
(out). On a wall the wall is to the west (x = 0) and the door faces east; on the floor it stands on a Unistrut stand, its door
also east. Gear wired on every face is drawn on the north wall (the blockstate turns it to the other walls), on the floor, or on
the ceiling, through Face, which maps u (across), v (up the wall) and w (out of the face) onto x, y and z.
"""
import json, os
import mlib
import plantlib as p

TEX = dict(p.TEX)
TEX.update({
	"porcelain": "block/mat/mat_porcelain_brown",
	"relays": "block/mat/decal_relays",
	"pushbuttons": "block/mat/decal_pushbuttons",
	"bulb": "block/mat/mat_bulb_lit",
})

CONDUIT = 0.5   # 1/2 in EMT, drawn at its trade size's outside diameter
MOUNT_Y = 0.55  # the middle of wall gear in its cell


def model(name, tex=None, particle="grey"):
	t = dict(TEX)
	t.update(tex or {})
	return mlib.Model(name, [(0, 0, 0)], t, t[particle])


def single(models, item):
	"""Export arguments for a set of looks of one block: the first look also draws the block's item."""
	for i, m in enumerate(models):
		m.export_args = {"blockstate": False, "item": item if i == 0 else False}
	return models


def save_blockstate(name, variants):
	path = os.path.join(mlib.ASSETS, "blockstates", f"{name}.json")
	with open(path, "w") as f:
		json.dump({"variants": variants}, f, indent=1)


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def enclosure(M, w, h, d, y0, wall, tex="grey"):
	"""An enclosure w wide (along the line, z), h high and d deep (out from the wall, x), its bottom at y0: against the west wall,
	or on a Unistrut stand in the middle of the cell. Its door faces east. Returns (x of the door's face, z1, z2, y0, y1)."""
	z1, z2 = 0.5 - w / 2, 0.5 + w / 2
	x1 = 0.0 if wall else 0.5 - d / 2
	x2 = x1 + d
	y1 = y0 + h
	M.box((x1, y0, z1), (x2 - 0.012, y1, z2), tex)                                   # the box
	M.box((x2 - 0.012, y0 + 0.01, z1 + 0.01), (x2, y1 - 0.01, z2 - 0.01), tex)        # its door
	M.box((x2 - 0.015, y1 - 0.06, z2 - 0.012), (x2 + 0.004, y1 - 0.02, z2), "galv")   # hinges, on the out side
	M.box((x2 - 0.015, y0 + 0.02, z2 - 0.012), (x2 + 0.004, y0 + 0.06, z2), "galv")
	if not wall:
		for z in (z1 + 0.03, z2 - 0.07):   # two Unistrut channels from the floor
			M.box((x1 - 0.042, 0.0, z), (x1, y1 + 0.05, z + 0.042), "galv")
		M.box((x1 - 0.06, 0.0, z1 - 0.02), (x1 + 0.02, 0.008, z2 + 0.02), "galv")      # base plate
	# conduit from each end to its terminal face, with a fitting at the box
	r = mlib.NPS_OD[CONDUIT] / 2
	cx, cy = x1 + d / 2, y0 + h * 0.5
	M.cylinder("z", (cx, cy), r, 0.0, z1, "galv", 8)
	M.cylinder("z", (cx, cy), r, z2, 1.0, "galv", 8)
	for z in (z1 - 0.025, z2):
		M.cylinder("z", (cx, cy), r + 0.006, z, z + 0.025, "galv", 8)
	return x2, z1, z2, y0, y1


def collide_enclosure(M, w, h, d, y0, wall):
	z1, z2 = 0.5 - w / 2, 0.5 + w / 2
	x1 = 0.0 if wall else 0.5 - d / 2
	M.collide((x1, y0, z1), (x1 + d, y0 + h, z2))


class Face:
	"""Drawing on one face of the cell: 'wall' (the north wall, out toward +z), 'floor' (out up) or 'ceiling' (out down)."""

	def __init__(self, M, where):
		self.M = M
		self.where = where

	def p(self, u, v, w):
		if self.where == "wall":
			return (u, v, w)
		if self.where == "floor":
			return (u, w, 1.0 - v)
		return (u, 1.0 - w, v)

	FRONT = {"wall": "south", "floor": "up", "ceiling": "down"}

	def box(self, a, b, tex):
		return self.M.box(self.p(*a), self.p(*b), tex)

	def decal(self, a, b, tex, back):
		"""A thin plate showing its whole picture on the face pointing out of the cell's face."""
		return self.M.decal(self.p(*a), self.p(*b), tex, self.FRONT[self.where], back)

	def cylinder(self, u, v, radius, w1, w2, tex, sides=12):
		"""A cylinder standing out of the face, its axis at (u, v)."""
		if self.where == "wall":
			self.M.cylinder("z", (u, v), radius, w1, w2, tex, sides)
		elif self.where == "floor":
			self.M.cylinder("y", (u, 1.0 - v), radius, w1, w2, tex, sides)
		else:
			self.M.cylinder("y", (u, v), radius, 1.0 - w2, 1.0 - w1, tex, sides)

	def collide(self, a, b):
		self.M.collide(self.p(*a), self.p(*b))
