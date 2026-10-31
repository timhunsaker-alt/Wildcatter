"""Shared by the cooling tower cell's parts (docs/multiblocks/cooling-tower.md): its FRP casing, corner posts, the ladder up
the north face and the hot-water riser up the east face. Each part draws its own height of them, so they line up."""
import plantlib as p

W = 4.0          # the cell, 4 x 4 m
IN = 0.12        # the casing stands this far in from the cell's edge
LADDER_X = 3.4
RISER = (3.94, 2.0)   # x, z of the riser on the east face (NPS 4)


def model(name, height, particle="polymer"):
	return p.model(name, 4, height, 4, particle)


def posts(M, y1, y2):
	for x in (IN, W - IN):
		for z in (IN, W - IN):
			M.box((x - 0.06, y1, z - 0.06), (x + 0.06, y2, z + 0.06), "galv")


def casing(M, y1, y2, open_sides=()):
	"""FRP panels on the four sides, with ribs every 0.4 m; open_sides ('north', 'south') are left for the louvres."""
	t = 0.03
	walls = {
		"north": ((IN, y1, IN - t), (W - IN, y2, IN)),
		"south": ((IN, y1, W - IN), (W - IN, y2, W - IN + t)),
		"west": ((IN - t, y1, IN), (IN, y2, W - IN)),
		"east": ((W - IN, y1, IN), (W - IN + t, y2, W - IN)),
	}
	for side, (a, b) in walls.items():
		if side in open_sides:
			continue
		M.box(a, b, "polymer")
		for k in range(1, 10):
			u = IN + k * (W - 2 * IN) / 10
			if side == "north":
				M.box((u - 0.015, y1, IN - t - 0.02), (u + 0.015, y2, IN - t), "polymer")
			elif side == "south":
				M.box((u - 0.015, y1, W - IN + t), (u + 0.015, y2, W - IN + t + 0.02), "polymer")
			elif side == "west":
				M.box((IN - t - 0.02, y1, u - 0.015), (IN - t, y2, u + 0.015), "polymer")
			else:
				M.box((W - IN + t, y1, u - 0.015), (W - IN + t + 0.02, y2, u + 0.015), "polymer")


def ladder(M, y1, y2):
	M.ladder(LADDER_X, IN - 0.05, y1, y2, -1, "galv")


def riser(M, y1, y2):
	M.cylinder("y", RISER, p.mlib.NPS_OD[4] / 2, y1, y2, "padgreen", 8)


def louvres(M, z_wall, y1, y2, outward):
	"""Air-inlet louvres: slats along x, tilted to shed the water back in."""
	y = y1 + 0.06
	while y < y2 - 0.05:
		c = (W / 2, y, z_wall + outward * 0.02)
		M.box((IN, y - 0.005, z_wall - 0.06), (W - IN, y + 0.005, z_wall + 0.06), "polymer", rot=("x", 35 * outward, c))
		y += 0.11
	for x in (IN, W / 2, W - IN):
		M.box((x - 0.03, y1, z_wall - 0.03), (x + 0.03, y2, z_wall + 0.03), "galv")
