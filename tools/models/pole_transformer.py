"""The 25 kVA single-phase pole-mount transformers (docs/multiblocks/poles.md), at true size in their one cell:
- pole_transformer, 7.2 kV to 120/240 V: a round tank about 0.46 m across and 0.84 m high with its cover, the H1 bushing (15 kV
  class, five sheds) on the cover with its bird guard;
- pole_transformer_mv (10d2), 2.4 kV to 120/240 V: the same rating on an older 4.16 kV line, a little smaller, with a 5 kV class
  H1 bushing of three sheds.
Each has a surge arrester on the tank's bracket, the X1, X2 and X3 secondary bushings low on the front, the nameplate and the
tank ground lug, and the two hanger brackets on the back that bolt it to the pole (the pole is the next cell south when it faces
north)."""
import linelib as l


def pole_unit(name, R, top, sheds):
	M = l.model(name, [(0, 0, 0)], "blue")
	CX, CZ = 0.5, 0.48
	M.cylinder("y", (CX, CZ), R, 0.06, top, "blue", 16)
	M.cylinder("y", (CX, CZ), R + 0.015, top, top + 0.04, "blue", 16)            # the cover and its band
	M.cylinder("y", (CX, CZ), R + 0.012, 0.06, 0.09, "blue", 16)                 # the bottom rim
	M.cylinder("y", (CX, CZ), R - 0.04, top + 0.04, top + 0.07, "blue", 16)      # the domed lid
	# H1 on the cover, a little toward the front, with its bird guard and terminal
	base = top + 0.07
	M.cylinder("y", (CX, CZ - 0.08), 0.045, base, base + 0.03, "grey", 8)
	for k in range(sheds):
		M.cylinder("y", (CX, CZ - 0.08), 0.055 if k % 2 == 0 else 0.04, base + 0.03 + k * 0.035, base + 0.055 + k * 0.035, "porcelain", 10)
	head = base + 0.03 + sheds * 0.035
	M.cylinder("y", (CX, CZ - 0.08), 0.015, head, head + 0.05, "brass", 6)
	M.cylinder("y", (CX, CZ - 0.08), 0.06, head + 0.03, head + 0.04, "rubber", 10)   # the bird guard
	# the surge arrester on its bracket, beside H1
	M.box((CX + R - 0.03, top - 0.12, CZ - 0.03), (CX + R + 0.09, top - 0.1, CZ + 0.03), "galv")
	M.cylinder("y", (CX + R + 0.06, CZ), 0.035, top - 0.1, top + 0.16, "polymer", 8)
	for k in range(4):
		M.cylinder("y", (CX + R + 0.06, CZ), 0.05, top - 0.06 + k * 0.055, top - 0.045 + k * 0.055, "polymer", 8)
	M.cylinder("y", (CX + R + 0.06, CZ), 0.012, top + 0.16, top + 0.2, "brass", 6)
	# X1, X2, X3: the secondary bushings low on the front
	for x in (CX - 0.12, CX, CX + 0.12):
		M.cylinder("z", (x, 0.66 * top / 0.86), 0.025, CZ - R - 0.06, CZ - R + 0.01, "dark", 8)
		M.cylinder("z", (x, 0.66 * top / 0.86), 0.012, CZ - R - 0.1, CZ - R - 0.06, "brass", 6)
	M.decal((CX - 0.07, 0.4 * top / 0.86, CZ - R - 0.004), (CX + 0.07, 0.5 * top / 0.86, CZ - R + 0.002), "nameplate", "north", "blue")
	M.box((CX - 0.15, 0.12, CZ - R - 0.03), (CX - 0.11, 0.16, CZ - R + 0.01), "copper")   # the tank ground lug
	# the hanger brackets to the pole behind it
	for yy in (0.2, top - 0.16):
		M.box((CX - 0.16, yy, CZ + R - 0.02), (CX + 0.16, yy + 0.06, CZ + R + 0.02), "galv")
		for x in (CX - 0.14, CX + 0.12):
			M.box((x, yy, CZ + R), (x + 0.02, yy + 0.06, 1.34), "galv")
		M.box((CX - 0.14, yy, 1.32), (CX + 0.14, yy + 0.06, 1.36), "galv")
	return M


M = pole_unit("pole_transformer", 0.23, 0.86, 5)
EXPORT = dict(blockstate=False)
MV = pole_unit("pole_transformer_mv", 0.21, 0.76, 3)
MV.export_args = {"blockstate": False, "item": True}
EXTRA = [MV]
