"""138 kV station bus, 2 x 6 x 9 (docs/multiblocks/substation.md), at true size.

Aluminium tube on 138 kV post insulators on a galvanized structure: each phase comes in from the disconnect at the west
(3 m spacing) and is gathered across the bus to the transformer's bushings at the east (1 m spacing): the outer phases turn
along the structure's beam toward the middle.
"""
import yardlib as y

M = y.model("bus_hv", 2, 6, 9, "galv")
TOP = 4.0
IN = (3.5, 4.5, 5.5)   # where the phases leave toward the transformer's bushings

for z in (0.35, 8.65):
	y.column(M, 1.0, z, 0.0, TOP)
y.beam(M, 0.15, 1.85, 0.2, 8.8, TOP - 0.4, TOP)
for zp, zt in zip(y.HV_PHASES, IN):
	y.hv_post(M, 0.5, zp, TOP)                        # a post under each phase where it comes in
	y.tube(M, "x", (y.HV_Y, zp), 0.0, 0.55)
	if zp != zt:
		y.hv_post(M, 1.0, (zp + zt) / 2, TOP)
		M.cylinder("z", (0.5, y.HV_Y), 0.055, min(zp, zt) - 0.05, max(zp, zt) + 0.05, "steel", 8)
	y.hv_post(M, 1.5, zt, TOP)
	M.cylinder("x", (y.HV_Y, zt), 0.055, 0.45, 2.0, "steel", 8)
	for x in (0.5, 1.5):
		M.box((x - 0.07, y.HV_Y - 0.08, zt - 0.07 if x > 1 else zp - 0.07), (x + 0.07, y.HV_Y - 0.03, (zt if x > 1 else zp) + 0.07), "galv")   # clamps
M.box((0.95, 0.0, 0.3), (1.05, 0.05, 0.4), "copper")

for z in (0.35, 8.65):
	M.collide((0.8, 0, z - 0.2), (1.2, TOP, z + 0.2))
M.collide((0.15, TOP - 0.4, 0.2), (1.85, TOP, 8.8))
