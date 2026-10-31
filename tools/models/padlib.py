"""A three-phase pad-mount transformer (docs/multiblocks/padmount.md), drawn in its 2 x 2 x 2 cells facing north: the concrete pad,
the tank with its cooling fins on the back and sides, the front cabinet (high-voltage compartment on the left as you face
it, low-voltage on the right) with its two doors, sill, penta-head bolt and padlock hasp, the warning and nameplate decals, and
the lifting lugs."""
import linelib as l


def padmount(name, width, height, depth, fins):
	M = l.model(name, l.mlib.footprint_box(2, 2, 2, 0, 0), "padgreen")
	x1, x2 = 1.0 - width / 2, 1.0 + width / 2
	z1, z2 = 1.0 - depth / 2, 1.0 + depth / 2
	pad = 0.1
	M.box((x1 - 0.12, 0, z1 - 0.12), (x2 + 0.12, pad, z2 + 0.12), "concrete")
	cab = z1 + 0.5          # the cabinet's back: the tank behind it
	top = pad + height
	M.box((x1, pad, z1), (x2, top, cab), "padgreen")                                      # cabinet
	M.box((x1 - 0.015, top - 0.02, z1 - 0.025), (x2 + 0.015, top + 0.015, cab), "padgreen")  # its lid lip
	M.box((x1 + 0.08, pad, cab), (x2 - 0.08, top - 0.08, z2 - 0.12), "padgreen")             # tank
	M.box((x1 + 0.06, top - 0.08, cab), (x2 - 0.06, top - 0.05, z2 - 0.1), "padgreen")       # tank cover
	M.fins((x1 + 0.14, pad + 0.12, z2 - 0.12), (x2 - 0.14, top - 0.2, z2), "x", fins, 0.012, "padgreen")
	for x in (x1 + 0.08, x2 - 0.2):
		M.fins((x, pad + 0.12, cab + 0.12), (x + 0.12, top - 0.2, z2 - 0.18), "z", 4, 0.012, "padgreen")
	mid = (x1 + x2) / 2
	M.box((x1 + 0.03, pad + 0.04, z1 - 0.012), (mid - 0.005, top - 0.06, z1), "padgreen")    # the doors
	M.box((mid + 0.005, pad + 0.04, z1 - 0.012), (x2 - 0.03, top - 0.06, z1), "padgreen")
	M.box((mid - 0.03, pad + 0.4, z1 - 0.03), (mid + 0.03, pad + 0.46, z1 - 0.01), "steel")  # penta-head bolt
	M.box((mid + 0.05, pad + 0.55, z1 - 0.04), (mid + 0.09, pad + 0.65, z1 - 0.01), "steel") # padlock hasp
	for xa in (x1 + 0.12, mid + 0.1):
		M.decal((xa, top - 0.45, z1 - 0.014), (xa + 0.35, top - 0.2, z1 - 0.012), "danger", "north", "padgreen")
	M.decal((x2 - 0.24, pad + 0.15, z1 - 0.014), (x2 - 0.06, pad + 0.27, z1 - 0.012), "nameplate", "north", "steel")
	for x in (x1 + 0.2, x2 - 0.24):
		M.box((x, top - 0.05, (cab + z2) / 2 - 0.02), (x + 0.04, top + 0.06, (cab + z2) / 2 + 0.02), "padgreen")   # lifting lugs
	M.collide((x1, 0, z1), (x2, top, z2))
	return M
