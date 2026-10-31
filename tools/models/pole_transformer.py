"""A 25 kVA single-phase pole-mount transformer, 7.2 kV to 120/240 V (docs/multiblocks/poles.md), at true size in its one cell:
a round tank about 0.46 m across and 0.84 m high with its cover, the H1 bushing on the cover with its bird guard, a surge
arrester on the tank's bracket, the X1, X2 and X3 secondary bushings low on the front, the nameplate and the tank ground lug,
and the two hanger brackets on the back that bolt it to the pole (the pole is the next cell south when it faces north)."""
import linelib as l

M = l.model("pole_transformer", [(0, 0, 0)], "blue")
EXPORT = dict(blockstate=False)
CX, CZ, R = 0.5, 0.48, 0.23
M.cylinder("y", (CX, CZ), R, 0.06, 0.86, "blue", 16)
M.cylinder("y", (CX, CZ), R + 0.015, 0.86, 0.9, "blue", 16)                # the cover and its band
M.cylinder("y", (CX, CZ), R + 0.012, 0.06, 0.09, "blue", 16)               # the bottom rim
M.cylinder("y", (CX, CZ), R - 0.04, 0.9, 0.93, "blue", 16)                 # the domed lid
# H1 on the cover, a little toward the front, with its bird guard and terminal
M.cylinder("y", (CX, CZ - 0.08), 0.045, 0.93, 0.96, "grey", 8)
for k in range(5):
	M.cylinder("y", (CX, CZ - 0.08), 0.055 if k % 2 == 0 else 0.04, 0.96 + k * 0.035, 0.985 + k * 0.035, "porcelain", 10)
M.cylinder("y", (CX, CZ - 0.08), 0.015, 1.13, 1.18, "brass", 6)
M.cylinder("y", (CX, CZ - 0.08), 0.06, 1.16, 1.17, "rubber", 10)           # the bird guard
# the surge arrester on its bracket, beside H1
M.box((CX + 0.2, 0.74, CZ - 0.03), (CX + 0.32, 0.76, CZ + 0.03), "galv")
M.cylinder("y", (CX + 0.29, CZ), 0.035, 0.76, 1.02, "polymer", 8)
for k in range(4):
	M.cylinder("y", (CX + 0.29, CZ), 0.05, 0.8 + k * 0.055, 0.815 + k * 0.055, "polymer", 8)
M.cylinder("y", (CX + 0.29, CZ), 0.012, 1.02, 1.06, "brass", 6)
# X1, X2, X3: the secondary bushings low on the front
for x in (CX - 0.12, CX, CX + 0.12):
	M.cylinder("z", (x, 0.66), 0.025, CZ - R - 0.06, CZ - R + 0.01, "dark", 8)
	M.cylinder("z", (x, 0.66), 0.012, CZ - R - 0.1, CZ - R - 0.06, "brass", 6)
M.decal((CX - 0.07, 0.4, CZ - R - 0.004), (CX + 0.07, 0.5, CZ - R + 0.002), "nameplate", "north", "blue")
M.box((CX - 0.15, 0.12, CZ - R - 0.03), (CX - 0.11, 0.16, CZ - R + 0.01), "copper")   # the tank ground lug
# the hanger brackets to the pole behind it
for y in (0.2, 0.7):
	M.box((CX - 0.16, y, CZ + R - 0.02), (CX + 0.16, y + 0.06, CZ + R + 0.02), "galv")
	for x in (CX - 0.14, CX + 0.12):
		M.box((x, y, CZ + R), (x + 0.02, y + 0.06, 1.34), "galv")
	M.box((CX - 0.14, y, 1.32), (CX + 0.14, y + 0.06, 1.36), "galv")
