"""A 15 kV fused cutout (docs/multiblocks/poles.md), at true size in its one cell: the porcelain body (about 0.45 m, 110 kV BIL)
leaning out on its mounting bracket, the upper and lower contacts with their hooks, and the fibreglass fuse tube in front of
it, hinged at the bottom. When the link blows the tube drops open: it hangs swung down and out (EXTRA, the blown model).
Facing north the line runs in at the back and out at the front, as the block's terminals do."""
import linelib as l


def cutout(name, blown):
	M = l.model(name, [(0, 0, 0)], "porcelain")
	cz = 0.6
	M.box((0.46, 0.45, 0.82), (0.54, 0.55, 1.0), "galv")                         # the bracket to the arm behind
	M.box((0.44, 0.47, cz + 0.03), (0.56, 0.53, 0.84), "galv")
	M.cylinder("y", (0.5, cz), 0.035, 0.22, 0.78, "porcelain", 10)                 # the body
	for k in range(6):
		M.cylinder("y", (0.5, cz), 0.06, 0.27 + k * 0.085, 0.29 + k * 0.085, "porcelain", 10)
	for y in (0.17, 0.8):
		M.box((0.46, y, 0.36), (0.54, y + 0.05, cz + 0.02), "galv")               # the contacts
		M.box((0.485, y - 0.02, 0.34), (0.515, y + 0.07, 0.38), "copper")
	hinge = (0.5, 0.22, 0.4)
	rot = ("x", -45.0, hinge) if blown else None
	M.box((0.47, 0.22, 0.375), (0.53, 0.78, 0.425), "polymer", rot=rot)            # the fuse tube
	M.box((0.465, 0.74, 0.37), (0.535, 0.8, 0.43), "brass", rot=rot)               # its cap
	M.box((0.49, 0.8, 0.39), (0.51, 0.86, 0.41), "galv", rot=rot)                 # the pull ring eye
	return M


M = cutout("fused_cutout", False)
EXPORT = dict(blockstate=False)
EXTRA = [cutout("fused_cutout_blown", True)]
