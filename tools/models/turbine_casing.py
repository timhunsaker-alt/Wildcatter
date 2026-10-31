"""Single-stage condensing steam turbine, 1 MW, 2 x 2 x 2 (docs/multiblocks/steam-plant.md), at true size.

About 2 m long on its skid: the horizontally split casing (lagged, with its bolted joint), the steam chest on top with the
governor valve and its actuator, the trip-and-throttle valve on the inlet that comes down from the superheater to the
north, the bearing pedestals, the exhaust trunk out of the south side into the condenser beside it, and the lube-oil
console. The generator is to the west: the coupling there turns behind an open guard (pivot x 0.1, y 0.85, z 1.0).
"""
import plantlib as p

M = p.model("turbine_casing", 2, 2, 2, "lagging")
Y, Z = p.SHAFT_Y, p.SHAFT_Z
IX = 1.45   # the inlet, under the superheater's stop valve

p.skid(M, 0.0, 2.0, 0.15, 1.85)
# the casing: lagged barrel with the horizontal joint, the exhaust end wider
M.cylinder("x", (Y, Z), 0.5, 0.55, 1.75, "lagging", 16)
M.cylinder("x", (Y, Z), 0.38, 0.4, 0.55, "lagging", 16)
M.box((0.42, Y - 0.03, Z - 0.56), (1.75, Y + 0.03, Z + 0.56), "iron")   # the split flange
for x in [0.5 + k * 0.12 for k in range(11)]:
	for z in (Z - 0.54, Z + 0.54):
		M.box((x - 0.015, Y + 0.03, z - 0.015), (x + 0.015, Y + 0.06, z + 0.015), "galv")
# bearing pedestals and the shaft ends
for x1, x2 in ((0.22, 0.4), (1.75, 1.95)):
	M.box((x1, 0.22, Z - 0.28), (x2, Y + 0.18, Z + 0.28), "iron")
	M.cylinder("x", (Y, Z), 0.16, x1, x2, "iron", 12)
M.cylinder("x", (Y, Z), 0.07, 0.0, 0.22, "steel", 10)                # the shaft to the coupling
p.coupling_guard(M, 0.0, 0.22, (Y, Z), 0.3, "blue")
M.cylinder("y", (1.85, Z + 0.2), 0.03, Y + 0.18, Y + 0.28, "brass", 8)   # speed pickup
M.box((1.8, Y + 0.18, Z - 0.3), (1.9, Y + 0.3, Z - 0.18), "dark")    # overspeed trip
# steam chest and governor valve on top, the actuator above it
M.box((1.15, Y + 0.45, Z - 0.3), (1.65, Y + 0.75, Z + 0.3), "iron")
M.cylinder("y", (1.4, Z), 0.08, Y + 0.75, Y + 0.9, "steel", 10)
M.box((1.28, Y + 0.9, Z - 0.12), (1.52, 1.98, Z + 0.12), "blue")
# inlet from the superheater above the north side: down at IX, through the trip-and-throttle valve, into the chest
M.pipe([(IX, 1.75, 0.095), (IX, 1.75, Z - 0.3)], 6, "lagging")
M.flange("z", (IX, 1.75), 0.15, 0.1, 0.03, "steel", bolts=8, bolt_tex="galv")
M.box((IX - 0.13, 1.6, 0.35), (IX + 0.13, 1.9, 0.6), "steel")   # trip-and-throttle valve
M.cylinder("y", (IX, 0.47), 0.03, 1.9, 1.97, "steel", 8)
M.cylinder("y", (IX, 0.47), 0.11, 1.97, 1.99, "rubber", 12)       # its handwheel
# exhaust trunk out of the south side, down into the condenser beside it
M.box((0.75, 0.3, Z + 0.35), (1.55, 1.05, 1.98), "iron")
M.box((0.72, 0.27, 1.96), (1.58, 1.08, 2.0), "iron")             # its flange
# lube-oil console on the north side: tank, pump, filters, the oil lines to the bearings
M.box((0.1, 0.22, 0.12), (0.75, 0.62, 0.42), "padgreen")
M.cylinder("y", (0.25, 0.27), 0.06, 0.62, 0.82, "steel", 10)
M.cylinder("y", (0.45, 0.27), 0.06, 0.62, 0.82, "steel", 10)
M.box((0.55, 0.62, 0.17), (0.72, 0.78, 0.37), "blue")             # oil pump motor
M.pipe([(0.31, 0.62, 0.27), (0.31, 0.62, 0.5), (0.31, Y + 0.1, 0.5), (0.31, Y + 0.1, Z - 0.28)], 0.5, "steel")
M.decal((0.3, 0.3, 0.115), (0.55, 0.5, 0.12), "gauge", "north", "padgreen")
M.decal((0.75, 0.25, 0.145), (1.05, 0.4, 0.15), "nameplate", "north", "steel")   # on the skid's front channel

M.collide((0.2, 0, 0.15), (1.95, 1.95, 1.85))
