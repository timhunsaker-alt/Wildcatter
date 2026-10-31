"""The X-brace of a 138 kV wood H-frame (docs/multiblocks/poles.md): two 3.6 x 5.6 in (92 x 143 mm) timbers crossed between the
two poles, 3 m high, bolted at the crossing and to each pole. The main cell is the middle of the bottom row; the poles stand
2 m either side (centres x -1.5 and 2.5)."""
import linelib as l

M = l.model("x_brace_138", l.mlib.footprint_box(3, 3, 1, 1, 0), "fir")
L, R = -1.5 + 0.17, 2.5 - 0.17
for a, b in (((L, 2.85, 0.62), (R, 0.15, 0.62)), ((L, 0.15, 0.38), (R, 2.85, 0.38))):
	l.strut(M, a, b, 0.143, 0.092, "fir")
M.box((0.45, 1.45, 0.3), (0.55, 1.55, 0.7), "galv")   # the bolt at the crossing
for x, y in ((L, 2.85), (R, 0.15), (L, 0.15), (R, 2.85)):
	M.box((x - 0.03, y - 0.03, 0.28), (x + 0.03, y + 0.03, 0.72), "galv")
