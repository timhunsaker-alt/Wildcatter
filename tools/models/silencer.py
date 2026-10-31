"""Critical-grade exhaust silencers for the genset skids (docs/multiblocks/genset.md), at true size.

A drum with dished ends on two saddles, which stand on the engine's stands below. The exhaust comes in at the bottom from
the turbocharger's riser and leaves by a tail pipe at the radiator end, turned up, with a rain flap. The panel's smoke
comes out of that tail pipe (GensetPartBlockEntity, 0.1 m from the west end).
"""
import gensetlib as g


def silencer(name, length, radius, inlet_x, saddles, inlet_nps):
	M = g.model(name, length, 1, "iron", width=1)
	Y, Z = max(0.36, radius + 0.04), 0.5
	x1, x2 = 0.22, length - 0.12
	M.cylinder("x", (Y, Z), radius, x1, x2, "iron", 16)
	for x in (x1, x2):   # the seams at the dished ends
		M.ring("x", (Y, Z), radius + 0.008, x - 0.01, x + 0.01, 0.02, "iron", sides=16)
	M.cylinder("x", (Y, Z), radius * 0.7, x1 - 0.05, x1, "iron", 16)
	M.cylinder("x", (Y, Z), radius * 0.7, x2, x2 + 0.05, "iron", 16)
	for x in saddles:   # saddles and their straps
		M.box((x - 0.05, 0.0, Z - radius * 0.8), (x + 0.05, Y - radius * 0.6, Z + radius * 0.8), "iron")
		M.ring("x", (Y, Z), radius + 0.012, x - 0.03, x + 0.03, 0.015, "galv", sides=16)
	r = g.mlib.NPS_OD[inlet_nps] / 2
	top = 0.96 - r   # the tail pipe's end, with room for its flap inside the cell
	M.cylinder("y", (inlet_x, Z), r, 0.0, Y - radius + 0.05, "steel", 8)   # the inlet stub, down to the riser's bellows
	M.flange("y", (inlet_x, Z), g.mlib.NPS_OD[inlet_nps] / 2 + 0.04, 0.03, 0.02, "steel", bolts=8, bolt_tex="galv")
	# the tail pipe: out of the west end, then up, with its rain flap
	M.pipe([(x1 - 0.05, Y, Z), (0.13, Y, Z), (0.13, top, Z)], inlet_nps, "steel")
	M.box((0.13 - r - 0.01, top + r - 0.005, Z - r - 0.01), (0.13 + r + 0.01, top + r + 0.008, Z + r + 0.01), "steel", rot=("z", -10, (0.13 + r, top + r, Z)))
	M.box((0.13 + r, top + r - 0.03, Z - 0.02), (0.13 + r + 0.015, top + r + 0.01, Z + 0.02), "galv")   # the flap's hinge
	M.collide((0.0, 0.0, Z - radius), (length, Y + radius, Z + radius))
	return M
