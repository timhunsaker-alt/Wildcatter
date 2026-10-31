"""Cooling tower fan deck, 4 x 4 x 2 (docs/multiblocks/cooling-tower.md), at true size: the top of the cell and its controller.

The checker-plate deck round the fan opening, the FRP fan stack (3.1 m across, 1.3 m tall) with the 3 m fan turning in it
(tools/models/rotors.py: pivot x 2.0, y 0.75, z 2.0), the right-angle gearbox under the fan, the 7.5 kW motor outside the
stack on the east side and its drive shaft, the motor's disconnect switch at the north edge of the deck (the 480 V cable
connects to the front face of the deck's main cell, under it), the conduit to the motor, and the handrail round the deck.
"""
import towerlib as t

M = t.model("cooling_tower", 2, "checker")
C, SR = 2.0, 1.55   # the fan's centre (x and z), the stack's radius

# the deck round a square opening, on the casing's top
for a, b in (((0, 0, 0), (4, 0.1, C - SR + 0.05)), ((0, 0, C + SR - 0.05), (4, 0.1, 4)),
		((0, 0, C - SR + 0.05), (C - SR + 0.05, 0.1, C + SR - 0.05)), ((C + SR - 0.05, 0, C - SR + 0.05), (4, 0.1, C + SR - 0.05))):
	M.box(a, b, "checker")
t.posts(M, 0.0, 0.1)
# the fan stack
M.ring("y", (C, C), SR, 0.1, 1.4, 0.05, "polymer", sides=24)
M.ring("y", (C, C), SR + 0.03, 1.36, 1.42, 0.06, "polymer", sides=24)   # its rolled lip
for k in range(8):   # stiffeners up the outside
	import math
	a = math.radians(k * 45 + 22.5)
	x, z = C + (SR + 0.04) * math.cos(a), C + (SR + 0.04) * math.sin(a)
	M.box((x - 0.03, 0.1, z - 0.03), (x + 0.03, 1.36, z + 0.03), "polymer")
# gearbox under the fan, on its support beams across the opening
M.box((C - SR + 0.05, 0.12, C - 0.08), (C + SR - 0.05, 0.28, C + 0.08), "iron")
M.box((C - 0.25, 0.28, C - 0.25), (C + 0.25, 0.6, C + 0.25), "iron")
M.cylinder("y", (C, C), 0.08, 0.6, 0.68, "steel", 10)   # the output shaft up to the fan hub
# the motor outside the stack on the east side, and its drive shaft in through the stack wall
M.cylinder("x", (0.4, C), 0.17, 3.62, 3.97, "blue", 12)
for x in (3.68, 3.76, 3.84):
	M.ring("x", (0.4, C), 0.18, x, x + 0.02, 0.02, "blue", sides=12)
M.box((3.66, 0.1, C - 0.15), (3.95, 0.22, C + 0.15), "iron")   # its stand
M.box((3.7, 0.57, C - 0.08), (3.85, 0.7, C + 0.08), "blue")     # terminal box
M.cylinder("x", (0.4, C), 0.035, C + 0.25, 3.62, "steel", 8)    # drive shaft
# the disconnect switch at the north edge of the deck, the conduit along the edge to the motor
M.box((0.3, 0.1, 0.02), (0.75, 0.75, 0.22), "grey")
M.box((0.72, 0.35, 0.06), (0.8, 0.45, 0.12), "brass")           # its handle
M.decal((0.35, 0.5, 0.018), (0.7, 0.7, 0.02), "danger", "north", "grey")
M.pipe([(0.75, 0.15, 0.12), (3.8, 0.15, 0.12), (3.8, 0.15, C - 0.2), (3.8, 0.57, C - 0.2), (3.8, 0.57, C - 0.08)], 1, "galv")
# the handrail round the deck, with a gap where the ladder arrives
M.handrail([(t.LADDER_X - 0.35, 0.05), (0.05, 0.05), (0.05, 3.95), (3.95, 3.95), (3.95, 0.05), (t.LADDER_X + 0.35, 0.05)], 0.1, "padgreen")
M.box((t.LADDER_X - 0.25, 0.1, 0.03), (t.LADDER_X + 0.25, 0.12, 0.1), "galv")   # the ladder's top rung step

M.collide((0, 0, 0), (4, 0.1, 4))
for a, b in (((C - SR, 0.1, C - SR), (C + SR, 1.4, C - SR + 0.08)), ((C - SR, 0.1, C + SR - 0.08), (C + SR, 1.4, C + SR)),
		((C - SR, 0.1, C - SR), (C - SR + 0.08, 1.4, C + SR)), ((C + SR - 0.08, 0.1, C - SR), (C + SR, 1.4, C + SR))):
	M.collide(a, b)
