"""Genset alternator, 480Y/277 V, 4-pole brushless (docs/multiblocks/genset.md), at true size: 1 x 2 x 2.

The SAE adapter bolted to the engine's flywheel housing, open at its guard windows so the coupling shows turning; the fan
behind a screen at the drive end; the main frame with its cooling ribs and feet; the exciter cover at the far end; and the
terminal box on top, with the output cables running on to the breaker in the control panel. The coupling and the fan turn
(tools/models/rotors.py: pivots at x 0.1 and x 0.3, y 0.55, z 1.0).
"""
import gensetlib as g

M = g.model("alternator", 1, 2, "blue")
Y, Z = g.SHAFT_Y, g.SHAFT_Z

# SAE adapter with four guard windows: two rings and the bars between them
M.ring("x", (Y, Z), 0.46, 0.0, 0.03, 0.04, "blue", sides=16)
M.ring("x", (Y, Z), 0.46, 0.19, 0.22, 0.04, "blue", sides=16)
M.ring("x", (Y, Z), 0.46, 0.03, 0.19, 0.04, "blue", sides=16, skip=(1, 2, 3, 5, 6, 7, 9, 10, 11, 13, 14, 15))
# fan screen at the drive end: bars round the fan
M.ring("x", (Y, Z), 0.46, 0.22, 0.25, 0.04, "blue", sides=16)
M.ring("x", (Y, Z), 0.46, 0.39, 0.42, 0.04, "blue", sides=16)
M.ring("x", (Y, Z), 0.465, 0.25, 0.39, 0.012, "galv", sides=32, skip=tuple(range(1, 32, 2)))
# the main frame with cooling ribs, the exciter cover
M.cylinder("x", (Y, Z), 0.45, 0.42, 0.9, "blue", 16)
for x in (0.5, 0.6, 0.7, 0.8):
	M.ring("x", (Y, Z), 0.46, x, x + 0.025, 0.03, "blue", sides=16)
M.cylinder("x", (Y, Z), 0.34, 0.9, 0.98, "blue", 16)
M.cylinder("x", (Y, Z), 0.12, 0.98, 1.0, "galv", 10)
for z in (0.6, 1.4):   # air outlet louvres low on the frame
	M.box((0.5, 0.2, z - 0.1), (0.85, 0.3, z + 0.1), "dark")
# feet down to the base's mounts
for z, (za, zb) in ((0.31, (0.22, 0.75)), (1.69, (1.25, 1.78))):
	M.box((0.38, 0.0, z - 0.1), (0.62, 0.05, z + 0.1), "blue")
	M.box((0.44, 0.05, za), (0.56, 0.3, zb), "blue")
# terminal box on top, with its bolted lid, nameplate and the output cables to the panel
M.box((0.45, 0.95, 0.66), (0.9, 1.3, 1.34), "blue")
M.box((0.43, 1.3, 0.64), (0.92, 1.33, 1.36), "blue")
for x in (0.46, 0.89):
	for z in (0.66, 1.34):
		M.box((x - 0.012, 1.33, z - 0.012), (x + 0.012, 1.345, z + 0.012), "galv")
M.decal((0.52, 1.05, 0.654), (0.82, 1.22, 0.66), "nameplate", "north", "steel")
for z in (0.82, 0.94, 1.06, 1.18):   # A, B, C and N cables to the breaker
	M.cylinder("x", (1.15, z), 0.035, 0.9, 1.0, "rubber", 8)
	M.cylinder("x", (1.15, z), 0.045, 0.9, 0.93, "brass", 8)   # cable glands

M.collide((0, 0, 0.52), (1.0, 1.35, 1.48))
