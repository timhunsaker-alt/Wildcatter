"""The crossbeam of a 138 kV wood H-frame (docs/multiblocks/poles.md): two 5.6 x 7.6 in (143 x 193 mm) timbers about 9 m long,
one either side of the poles, through-bolted to both 75 ft poles 4 m apart, their tops 0.6 m below the pole tops. Under it
three suspension clevises for the strings, 4 m apart (middle and both ends), and a knee brace from each pole. The main cell is
the middle; the poles stand in the cells 2 m either side (centres x -1.5 and 2.5)."""
import linelib as l

CELLS = [(0, 0, 0), (-4, 0, 0), (-3, 0, 0), (-1, 0, 0), (1, 0, 0), (3, 0, 0), (4, 0, 0)]
M = l.model("crossbeam_138", CELLS, "fir")
Y1, Y2 = 0.5, 0.693
for z1, z2 in ((0.28, 0.423), (0.577, 0.72)):
	M.box((-4.0, Y1, z1), (5.0, Y2, z2), "fir")
for px in (-1.5, 2.5):
	M.box((px - 0.04, Y1 + 0.07, 0.22), (px + 0.04, Y1 + 0.12, 0.78), "galv")                  # the through bolts
	for side in (-1, 1):
		l.strut(M, (px + side * 1.2, Y1, 0.5), (px + side * 0.2, Y1 - 0.9, 0.5), 0.06, 0.08, "fir")   # knee braces
for sx in (-3.5, 0.5, 4.5):
	M.box((sx - 0.06, Y1 - 0.03, 0.26), (sx + 0.06, Y1, 0.74), "galv")                           # the hanger plate
	M.box((sx - 0.015, Y1 - 0.09, 0.48), (sx + 0.015, Y1 - 0.03, 0.52), "galv")                  # the clevis
M.collide((-4.0, Y1, 0.28), (5.0, Y2, 0.72))
