"""Genset alternator, 480Y/277 V, 4-pole brushless (docs/multiblocks/genset.md), at true size: 1 x 2 x 2, in two frames.

kw500 (M): the 500 kW diesel set's, a 1 m frame. mw1 (EXTRA): the 1 MW gas set's, about 1.6 m (a 1 MW 4-pole frame is about
1.6 m from adapter to exciter end), so its adapter and fan end reach 0.6 m back into the gas engine's last cell, under its
turbochargers and over its flywheel housing, which the engine leaves free (docs/questions/misc-gaps.md, 2). The block's frame
property picks the look; the control panel sets it when the set forms.

The SAE adapter bolted to the engine's flywheel housing, open at its guard windows so the coupling shows turning; the fan
behind a screen at the drive end; the main frame with its cooling ribs and feet; the exciter cover at the far end; and the
terminal box on top, with the output cables running on to the breaker in the control panel. The coupling and the fan turn
(tools/models/rotors.py: pivots at x 0.1 and x 0.3, y 0.55, z 1.0; 0.6 m further west on the 1 MW frame).
"""
import gearlib
import gensetlib as g

Y, Z = g.SHAFT_Y, g.SHAFT_Z


def alternator(name, back, radius):
	"""back: how far the frame reaches west of its cell (0 or 0.6 m); radius: the frame's."""
	M = g.model(name, 1, 2, "blue")
	a = -back   # the adapter's face, bolted to the engine's flywheel housing
	r = radius + 0.01
	# SAE adapter with four guard windows: two rings and the bars between them
	M.ring("x", (Y, Z), r, a, a + 0.03, 0.04, "blue", sides=16)
	M.ring("x", (Y, Z), r, a + 0.19, a + 0.22, 0.04, "blue", sides=16)
	M.ring("x", (Y, Z), r, a + 0.03, a + 0.19, 0.04, "blue", sides=16, skip=(1, 2, 3, 5, 6, 7, 9, 10, 11, 13, 14, 15))
	# fan screen at the drive end: bars round the fan
	M.ring("x", (Y, Z), r, a + 0.22, a + 0.25, 0.04, "blue", sides=16)
	M.ring("x", (Y, Z), r, a + 0.39, a + 0.42, 0.04, "blue", sides=16)
	M.ring("x", (Y, Z), r + 0.005, a + 0.25, a + 0.39, 0.012, "galv", sides=32, skip=tuple(range(1, 32, 2)))
	# the main frame with cooling ribs, the exciter cover
	M.cylinder("x", (Y, Z), radius, a + 0.42, 0.9, "blue", 16)
	ribs = int((0.85 - (a + 0.5)) / 0.1) + 1
	for k in range(ribs):
		x = a + 0.5 + k * 0.1
		M.ring("x", (Y, Z), r, x, x + 0.025, 0.03, "blue", sides=16)
	M.cylinder("x", (Y, Z), 0.34, 0.9, 0.98, "blue", 16)
	M.cylinder("x", (Y, Z), 0.12, 0.98, 1.0, "galv", 10)
	for z in (0.6, 1.4):   # air outlet louvres low on the frame
		M.box((max(a + 0.5, 0.5 - back), 0.2, z - 0.1), (0.85, 0.3, z + 0.1), "dark")
	# feet down to the base's mounts
	for fx in ((0.5,) if back == 0 else (a + 0.6, 0.6)):
		for z, (za, zb) in ((0.31, (0.22, 0.75)), (1.69, (1.25, 1.78))):
			M.box((fx - 0.12, 0.0, z - 0.1), (fx + 0.12, 0.05, z + 0.1), "blue")
			M.box((fx - 0.06, 0.05, za), (fx + 0.06, 0.3, zb), "blue")
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
	M.collide((0, 0, 0.52), (1.0, 1.35, 1.48))   # its own cell: the engine's cell is solid where the 1 MW frame reaches
	return M


M = alternator("alternator", 0.0, 0.45)
EXPORT = dict(blockstate=True)   # its shapes and item; the blockstate is rewritten in after() with the frame
LARGE = alternator("alternator_mw1", 0.6, 0.48)
EXTRA = [LARGE]


def after():
	gearlib.save_blockstate("alternator", {f"facing={facing},frame={frame},piece={piece}": {"model": f"wildcatter:block/{'alternator' if frame == 'kw500' else 'alternator_mw1'}/p{piece}", **({"y": y} if y else {})}
		for facing, y in gearlib.ROT.items() for piece in range(len(M.cells)) for frame in ("kw500", "mw1")})
