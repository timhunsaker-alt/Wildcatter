"""Genset diesel engine, about 15 L inline six (docs/multiblocks/genset.md), at true size: 2 x 2 x 2.

About 1.9 m long and 1.5 m tall on the skid: oil pan, crankcase and block, the cylinder head and three rocker covers (two
cylinders each), the injection pump and the six steel injector lines on the north side, the exhaust manifold and
turbocharger on the north side with its outlet rising to the silencer, the intake manifold and charge-air pipe on the south,
the oil filters, the starter at the flywheel housing, the timing cover, the fan drive and belt at the front. The crank
pulley turns (tools/models/rotors.py, pivot at x 0.06, y 0.55, z 1.0). Two stands at the back carry the silencer above.
"""
import gensetlib as g

M = g.model("genset_diesel_engine", 2, 2, "engine")
Y, Z = g.SHAFT_Y, g.SHAFT_Z
X1, X2 = 0.18, 1.82        # front (radiator end) and rear faces of the block
CYL = [X1 + 0.14 + k * (X2 - X1 - 0.28) / 5 for k in range(6)]

# bottom end: oil pan, crankcase skirt, block
M.box((X1 + 0.08, 0.1, 0.62), (X2 - 0.12, 0.32, 1.38), "engine")
M.box((X1 + 0.04, 0.3, 0.56), (X2 - 0.02, 0.36, 1.44), "engine")   # pan rail
M.box((X1, 0.36, 0.52), (X2, 0.78, 1.48), "engine")                # crankcase
M.box((X1 + 0.02, 0.78, 0.58), (X2 - 0.02, 1.12, 1.42), "engine")   # block
for x in CYL:   # the water jacket's core plugs and the main-bearing bulges along both sides
	for z in (0.575, 1.425):
		M.cylinder("z", (x, 0.95), 0.025, z - 0.01, z + 0.01, "galv", 8)
	M.box((x - 0.06, 0.4, 0.5), (x + 0.06, 0.72, 0.52), "engine")
	M.box((x - 0.06, 0.4, 1.48), (x + 0.06, 0.72, 1.5), "engine")
# top end: head, three rocker covers, the breather
M.box((X1 + 0.04, 1.12, 0.6), (X2 - 0.04, 1.3, 1.4), "engine")
for k in range(3):
	xa = X1 + 0.08 + k * (X2 - X1 - 0.16) / 3
	xb = xa + (X2 - X1 - 0.16) / 3 - 0.04
	M.box((xa, 1.3, 0.66), (xb, 1.42, 1.34), "iron")
	M.box((xa + 0.03, 1.42, 0.7), (xb - 0.03, 1.45, 1.3), "iron")
	for x in (xa + 0.06, xb - 0.06):
		M.cylinder("y", (x, 1.0), 0.012, 1.45, 1.47, "galv", 6)
M.pipe([(1.0, 1.45, 1.1), (1.0, 1.55, 1.1), (1.0, 1.55, 1.46)], 1.5, "rubber")   # crankcase breather hose
# front: timing cover, crank nose, fan drive on a bracket above it and its belt
M.box((X1 - 0.08, 0.32, 0.62), (X1, 1.15, 1.38), "engine")
M.cylinder("x", (Y, Z), 0.06, 0.04, X1 - 0.08, "iron", 8)
M.box((0.1, 0.95, 0.9), (X1 - 0.08, 1.15, 1.1), "engine")
M.cylinder("x", (1.0, Z), 0.12, 0.03, 0.09, "iron", 12)   # fan drive pulley (its shaft goes to the fan)
for dz in (-0.1, 0.1):   # the belt, crank pulley to fan drive
	M.box((0.04, Y, Z + dz - 0.015), (0.08, 1.0, Z + dz + 0.015), "rubber")
# north side: injection pump and the six injector lines, exhaust manifold, turbocharger
M.box((0.45, 0.62, 0.38), (1.0, 0.82, 0.52), "iron")
for k, x in enumerate(CYL):
	z = 0.4 + 0.015 * k
	M.pipe([(0.5 + 0.08 * k, 0.82, z), (0.5 + 0.08 * k, 1.18, z), (x, 1.18, z), (x, 1.18, 0.6)], 0.5, "steel")
M.box((X1 + 0.1, 1.0, 0.36), (X2 - 0.1, 1.12, 0.48), "iron")   # exhaust manifold
for x in CYL:
	M.box((x - 0.05, 0.98, 0.48), (x + 0.05, 1.14, 0.58), "iron")
TURBO_X = 1.6
M.cylinder("z", (TURBO_X, 1.3), 0.14, 0.3, 0.46, "iron", 12)     # turbine housing
M.cylinder("z", (TURBO_X, 1.3), 0.13, 0.2, 0.3, "steel", 12)     # compressor housing
M.cylinder("z", (TURBO_X, 1.3), 0.06, 0.12, 0.2, "steel", 8)     # air inlet
M.pipe([(TURBO_X, 1.12, 0.42), (TURBO_X, 1.2, 0.42)], 4, "iron")
# exhaust outlet: up from the turbine, over the head, up to the silencer's inlet above the rear
M.pipe([(TURBO_X, 1.42, 0.42), (TURBO_X, 1.7, 0.42), (TURBO_X, 1.7, 1.5)], 6, "lagging")
M.cylinder("y", (TURBO_X, 1.5), 0.11, 1.7, 1.98, "steel", 12)   # stainless flex bellows
for y in (1.76, 1.84, 1.92):
	M.cylinder("y", (TURBO_X, 1.5), 0.12, y, y + 0.02, "steel", 12)
# south side: charge-air pipe from the compressor round the rear to the intake manifold, oil filters, starter
M.box((X1 + 0.1, 1.0, 1.44), (X2 - 0.1, 1.12, 1.56), "galv")   # intake manifold
M.pipe([(TURBO_X, 1.3, 0.2), (1.86, 1.3, 0.2), (1.86, 1.3, 1.5), (1.75, 1.3, 1.5), (1.75, 1.12, 1.5)], 4, "galv")
for x in (0.55, 0.75):
	M.cylinder("y", (x, 1.56), 0.06, 0.45, 0.72, "steel", 10)
	M.cylinder("y", (x, 1.56), 0.065, 0.72, 0.75, "dark", 10)
M.box((0.45, 0.55, 1.48), (0.85, 0.75, 1.52), "engine")   # filter head
M.cylinder("x", (0.42, 1.55), 0.08, 1.6, 1.9, "dark", 10)   # starter motor
M.cylinder("x", (0.42, 1.55), 0.06, 1.55, 1.6, "galv", 8)
# rear: flywheel housing (SAE 0 bell) bolted to the alternator's adapter
M.cylinder("x", (Y, Z), 0.47, X2, 2.0, "engine", 16)
M.bolt_circle("x", (Y, Z), 0.43, X2 - 0.02, 12, "galv")
# engine feet on the base's mounts, and the oil dipstick
g.feet(M, (0.4, 1.6), (0.31, 1.69), 0.4, "engine")
for x in (0.4, 1.6):
	for z, dz in ((0.31, 0.21), (1.69, -0.21)):
		M.box((x - 0.05, 0.28, min(z, z + dz)), (x + 0.05, 0.4, max(z, z + dz)), "engine")
M.cylinder("y", (1.2, 0.5), 0.01, 0.5, 1.2, "brass", 6)
M.decal((0.9, 0.55, 0.515), (1.2, 0.72, 0.52), "engineplate", "north", "engine")
# the silencer's stands at the back of the engine: they reach up to its saddles
for x in (0.3, 1.7):
	M.box((x - 0.04, 1.3, 1.46), (x + 0.04, 2.0, 1.54), "iron")
	M.box((x - 0.06, 1.12, 1.42), (x + 0.06, 1.3, 1.58), "iron")

M.collide((0.1, 0, 0.36), (2.0, 1.47, 1.64))
M.collide((TURBO_X - 0.15, 1.47, 0.1), (TURBO_X + 0.15, 1.75, 0.6))
