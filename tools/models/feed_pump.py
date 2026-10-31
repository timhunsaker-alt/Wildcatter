"""Boiler feed pump: pump, coupling and its 5 hp 480 V motor, 2 x 1 x 1 (docs/multiblocks/steam-plant.md), at true size.

An end-suction pump skid: the motor on the west half (its terminal box on its south side is where the 480 V cable connects),
the coupling under its guard in the middle (it turns: pivot x 1.0, y 0.32, z 0.5), the pump casing on the east half with its
suction from the condenser's hotwell (east) and the discharge rising toward the boiler.
"""
import plantlib as p

M = p.model("feed_pump", 2, 1, 1, "blue")
Y, Z = 0.32, 0.5

M.box((0.02, 0.0, 0.2), (1.98, 0.1, 0.8), "iron")   # the baseplate
# motor: frame with cooling fins, fan cover at the west end, feet, terminal box on its west end
M.cylinder("x", (Y, Z), 0.17, 0.2, 0.75, "blue", 12)
for x in (0.28, 0.38, 0.48, 0.58, 0.68):
	M.ring("x", (Y, Z), 0.18, x, x + 0.02, 0.02, "blue", sides=12)
M.cylinder("x", (Y, Z), 0.16, 0.08, 0.2, "blue", 12)
for x in (0.3, 0.65):
	M.box((x - 0.05, 0.1, Z - 0.15), (x + 0.05, Y - 0.1, Z + 0.15), "blue")
M.box((0.38, 0.22, Z + 0.17), (0.58, 0.44, 0.97), "grey")   # terminal box on the motor's south side
M.cylinder("z", (0.48, 0.27), 0.025, 0.97, 1.0, "brass", 8)    # its cable gland
M.decal((0.35, 0.36, Z - 0.19), (0.6, 0.46, Z - 0.185), "nameplate", "north", "steel")
# the shaft and the coupling guard
M.cylinder("x", (Y, Z), 0.03, 0.75, 1.25, "steel", 8)
p.coupling_guard(M, 0.88, 1.12, (Y, Z), 0.11, "engine")
# the pump: bearing frame, casing, suction (east, from the hotwell) and discharge (up)
M.box((1.2, 0.1, Z - 0.08), (1.4, Y + 0.06, Z + 0.08), "iron")
M.cylinder("x", (Y, Z), 0.2, 1.4, 1.58, "iron", 12)
M.cylinder("x", (Y - 0.02, Z), 0.05, 1.58, 1.98, "steel", 8)
M.flange("x", (Y - 0.02, Z), 0.08, 1.96, 0.02, "steel", bolts=4, bolt_tex="galv")
M.pipe([(1.49, Y + 0.18, Z), (1.49, 0.94, Z)], 1.5, "steel")
M.cylinder("y", (1.49, Z), 0.04, 0.62, 0.7, "brass", 8)   # discharge check valve
M.cylinder("z", (1.49, 0.8), 0.04, Z - 0.1, Z - 0.04, "galv", 8)
M.decal((1.45, 0.76, Z - 0.105), (1.53, 0.84, Z - 0.1), "gauge", "north", "galv")

M.collide((0.02, 0, 0.2), (1.98, 0.55, 0.8))
