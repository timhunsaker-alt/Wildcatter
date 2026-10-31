"""Genset gas engine, 1 MW lean-burn V20 (docs/multiblocks/genset.md), at true size: 4 x 2 x 2.

About 3.8 m long: a deep crankcase and oil pan, two banks of ten cylinders in a V with their head covers, the ignition
harness rail and coil leads along each bank, the intake manifold in the V, insulated exhaust manifolds outboard of both
banks, twin turbochargers and the charge-air cooler at the rear, the exhaust riser to the silencer, and the gas train on
the north side (the pipeline connects to any cell of the engine). The crank pulley turns (pivot x 0.06, y 0.55, z 1.0).
"""
import gensetlib as g

M = g.model("genset_gas_engine", 4, 2, "gasgreen")
Y, Z = g.SHAFT_Y, g.SHAFT_Z
X1, X2 = 0.18, 3.82
CYL = [X1 + 0.22 + k * (X2 - X1 - 0.44) / 9 for k in range(10)]

# bottom end
M.box((X1 + 0.1, 0.08, 0.6), (X2 - 0.1, 0.3, 1.4), "gasgreen")
M.box((X1, 0.3, 0.48), (X2, 0.82, 1.52), "gasgreen")
for x in CYL:   # main-bearing bulges
	M.box((x - 0.07, 0.36, 0.46), (x + 0.07, 0.76, 0.48), "gasgreen")
	M.box((x - 0.07, 0.36, 1.52), (x + 0.07, 0.76, 1.54), "gasgreen")
	for z in (0.455, 1.545):   # crankcase door covers
		M.cylinder("z", (x, 0.56), 0.06, z - 0.01, z + 0.01, "iron", 10)
# the V: two banks, each block leaning out, with its heads and covers
for side, (za, zb) in ((-1, (0.42, 0.9)), (1, (1.1, 1.58))):
	M.box((X1 + 0.04, 0.82, za + 0.04), (X2 - 0.04, 1.08, zb - 0.04), "gasgreen")
	for x in CYL:
		M.box((x - 0.15, 1.08, za), (x + 0.15, 1.24, zb), "gasgreen")          # head
		M.box((x - 0.12, 1.24, za + 0.05), (x + 0.12, 1.33, zb - 0.05), "iron")   # head cover
		zp = (za + zb) / 2
		M.cylinder("y", (x, zp), 0.018, 1.33, 1.42, "rubber", 6)                # coil on the spark plug
	zr = za + 0.06 if side < 0 else zb - 0.06
	M.box((X1 + 0.1, 1.42, zr - 0.02), (X2 - 0.1, 1.46, zr + 0.02), "galv")      # ignition harness rail
	for x in CYL:
		M.box((x - 0.006, 1.38, min(zr, (za + zb) / 2)), (x + 0.006, 1.42, max(zr, (za + zb) / 2)), "rubber")
	# insulated exhaust manifold outboard of the bank
	zm = za - 0.12 if side < 0 else zb + 0.12
	M.cylinder("x", (1.0, zm), 0.1, X1 + 0.1, X2 - 0.2, "lagging", 10)
	for x in CYL:
		M.box((x - 0.04, 0.96, min(zm, za)), (x + 0.04, 1.06, max(zm, za) if side < 0 else max(zm, zb)), "lagging")
# intake manifold in the V, the charge-air cooler and twin turbos at the rear
M.box((X1 + 0.15, 1.08, 0.9), (X2 - 0.5, 1.3, 1.1), "galv")
M.box((3.2, 1.08, 0.75), (3.75, 1.45, 1.25), "grey")   # charge-air cooler
for x in (3.3, 3.65):
	M.cylinder("y", (x, 1.0), 0.035, 1.45, 1.52, "galv", 8)   # its water connections
for zt in (0.3, 1.7):
	M.cylinder("x", (1.32, zt), 0.15, 3.25, 3.45, "iron", 12)   # turbine housing
	M.cylinder("x", (1.32, zt), 0.14, 3.45, 3.62, "steel", 12)  # compressor housing
	M.cylinder("x", (1.32, zt), 0.07, 3.62, 3.75, "steel", 8)   # air inlet
	M.pipe([(3.53, 1.46, zt), (3.53, 1.55, zt), (3.53, 1.55, 1.0), (3.53, 1.45, 1.0)], 4, "galv")
# exhaust riser: both turbines into one pipe that rises to the silencer above (inlet at x 3.5, z 1.5)
M.pipe([(3.2, 1.32, 0.3), (3.1, 1.32, 0.3)], 6, "lagging")
M.pipe([(3.1, 1.32, 0.3), (3.1, 1.62, 0.3), (3.1, 1.62, 1.5), (3.5, 1.62, 1.5)], 8, "lagging")
M.pipe([(3.2, 1.32, 1.7), (3.1, 1.32, 1.7), (3.1, 1.5, 1.7), (3.1, 1.5, 1.5)], 6, "lagging")
M.cylinder("y", (3.5, 1.5), 0.14, 1.7, 1.98, "steel", 12)
for y in (1.76, 1.84, 1.92):
	M.cylinder("y", (3.5, 1.5), 0.155, y, y + 0.02, "steel", 12)
# front: vibration damper housing and timing cover, the fan drive, the belt
M.box((X1 - 0.08, 0.3, 0.6), (X1, 1.1, 1.4), "gasgreen")
M.cylinder("x", (Y, Z), 0.06, 0.04, X1 - 0.08, "iron", 8)
M.box((0.1, 0.95, 0.9), (X1 - 0.08, 1.15, 1.1), "gasgreen")
M.cylinder("x", (1.0, Z), 0.12, 0.03, 0.09, "iron", 12)
for dz in (-0.1, 0.1):
	M.box((0.04, Y, Z + dz - 0.015), (0.08, 1.0, Z + dz + 0.015), "rubber")
# gas train on the north side: shut-off valve, regulator, the gas mixer feed to the intake
M.pipe([(0.6, 0.2, 0.12), (0.6, 0.7, 0.12), (1.6, 0.7, 0.12)], 3, "galv")
M.cylinder("x", (0.7, 0.12), 0.07, 0.9, 1.05, "brass", 10)       # shut-off valve
M.box((0.95, 0.77, 0.07), (1.0, 0.9, 0.17), "rubber")            # its handle
M.cylinder("y", (1.3, 0.12), 0.12, 0.62, 0.9, "grey", 12)        # regulator
M.cylinder("y", (1.3, 0.12), 0.07, 0.9, 1.0, "grey", 10)
M.pipe([(1.6, 0.7, 0.12), (1.6, 1.2, 0.12), (1.6, 1.2, 0.9)], 3, "galv")
M.decal((2.0, 0.5, 0.455), (2.3, 0.7, 0.46), "engineplate", "north", "gasgreen")
# rear: flywheel housing; feet; the silencer's stands
M.cylinder("x", (Y, Z), 0.5, X2, 4.0, "gasgreen", 16)
M.bolt_circle("x", (Y, Z), 0.46, X2 - 0.02, 14, "galv")
g.feet(M, (0.5, 1.5, 2.5, 3.5), (0.31, 1.69), 0.32, "gasgreen")
for x in (1.3, 2.4):
	M.box((x - 0.04, 1.3, 1.46), (x + 0.04, 2.0, 1.54), "iron")
	M.box((x - 0.06, 1.24, 1.42), (x + 0.06, 1.3, 1.58), "iron")

M.collide((0.1, 0, 0.3), (4.0, 1.46, 1.7))
M.collide((3.1, 1.46, 0.15), (3.8, 1.75, 1.85))
