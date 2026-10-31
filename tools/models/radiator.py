"""Genset radiator with its 1.2 m pusher fan (docs/multiblocks/genset.md), at true size: 1 x 2 x 2.

A core about 1.8 x 1.8 m at the west end of the skid, top and bottom tanks with the filler cap, side frames, the fan
shroud round the fan and the wire guard over it, and the upper and lower hoses to the engine. The fan itself turns
(tools/models/rotors.py, pivot at x 0.78, y 1.0, z 1.0 in this frame).
"""
import gensetlib as g

M = g.model("radiator", 1, 2, "grey")
FAN_X, FAN_Y, FAN_Z, FAN_R = 0.78, 1.0, 1.0, 0.6

# the core: tubes and fins behind a grille, between side frames
M.box((0.04, 0.2, 0.12), (0.26, 1.8, 1.88), "dark")
M.fins((0.0, 0.2, 0.12), (0.04, 1.8, 1.88), "z", 44, 0.012, "galv")
for z in (0.06, 1.88):
	M.box((0.0, 0.0, z), (0.32, 1.95, z + 0.06), "grey")
M.box((0.0, 1.8, 0.12), (0.32, 1.95, 1.88), "grey")   # top tank
M.box((0.0, 0.05, 0.12), (0.32, 0.2, 1.88), "grey")   # bottom tank
M.cylinder("y", (0.16, 0.4), 0.05, 1.95, 2.0, "brass", 8)   # pressure filler cap
M.cylinder("z", (0.16, 0.12), 0.03, 0.0, 0.06, "brass", 8)  # drain cock
# the shroud: a plate from the core's edge to a ring round the fan, the ring just clear of the blade tips
M.box((0.32, 0.12, 0.12), (0.36, FAN_Y - FAN_R - 0.06, 1.88), "grey")
M.box((0.32, FAN_Y + FAN_R + 0.06, 0.12), (0.36, 1.88, 1.88), "grey")
M.box((0.32, FAN_Y - FAN_R - 0.06, 0.12), (0.36, FAN_Y + FAN_R + 0.06, FAN_Z - FAN_R - 0.06), "grey")
M.box((0.32, FAN_Y - FAN_R - 0.06, FAN_Z + FAN_R + 0.06), (0.36, FAN_Y + FAN_R + 0.06, 1.88), "grey")
M.ring("x", (FAN_Y, FAN_Z), FAN_R + 0.05, 0.36, 0.7, 0.03, "grey", sides=16)
# the wire guard over the fan, on the engine side
for k in range(-4, 5):
	M.box((0.9, FAN_Y - FAN_R, FAN_Z + k * 0.14 - 0.008), (0.915, FAN_Y + FAN_R, FAN_Z + k * 0.14 + 0.008), "galv")
M.ring("x", (FAN_Y, FAN_Z), FAN_R + 0.02, 0.89, 0.92, 0.02, "galv", sides=16)
for z in (0.3, 1.7):   # guard stays back to the shroud
	M.box((0.7, FAN_Y - 0.01, z), (0.9, FAN_Y + 0.01, z + 0.02), "galv")
# hoses to the engine: upper from the top tank, lower from the bottom tank
M.cylinder("x", (1.72, 0.7), 0.05, 0.32, 1.0, "rubber", 8)
M.cylinder("x", (0.3, 1.3), 0.05, 0.32, 1.0, "rubber", 8)
for x in (0.4, 0.92):
	M.cylinder("x", (1.72, 0.7), 0.058, x, x + 0.02, "steel", 8)   # hose clamps
	M.cylinder("x", (0.3, 1.3), 0.058, x, x + 0.02, "steel", 8)
# feet on the base's mounts
for z in (0.31, 1.69):
	M.box((0.42, 0.0, z - 0.1), (0.58, 0.06, z + 0.1), "grey")
	M.box((0.32, 0.0, z - 0.04), (0.58, 0.2, z + 0.04), "grey")

M.collide((0, 0, 0.06), (1.0, 1.95, 1.94))
