"""Boiler burner, 1 x 3 x 3 (docs/multiblocks/steam-plant.md): the packaged burner on the boiler's front, at true size.

About 1.2 m deep and 2.5 m tall: the burner housing and windbox on the furnace's axis (the furnace is the north half of the
D-type, its centre 1.4 m up and 0.9 m from the north side), the forced-draft fan's scroll and its motor below to the south,
the air duct up to the windbox, the gas train along the front (shut-off valves, regulator), the flame scanner, and the
burner management panel on its stand.
"""
import plantlib as p

M = p.model("boiler_burner", 1, 3, 3, "blue")
FY, FZ = 1.4, 0.9   # the furnace's axis

# the housing on the furnace axis, bolted to the boiler's front plate
M.cylinder("x", (FY, FZ), 0.48, 0.42, 0.98, "blue", 16)
M.flange("x", (FY, FZ), 0.56, 0.96, 0.04, "blue", bolts=12, bolt_tex="galv")
M.cylinder("x", (FY, FZ), 0.3, 0.3, 0.42, "blue", 12)          # the back cover
M.cylinder("x", (FY, FZ), 0.08, 0.24, 0.3, "steel", 8)         # the gun's end
M.cylinder("y", (0.7, FZ - 0.25), 0.04, FY + 0.38, FY + 0.6, "steel", 8)   # flame scanner, looking down the fire
M.box((0.66, FY + 0.6, FZ - 0.29), (0.74, FY + 0.66, FZ - 0.21), "dark")
# forced-draft fan: its scroll, inlet and motor, low on the south side, and the duct up to the windbox
M.box((0.12, 0.25, 1.55), (0.85, 1.25, 2.45), "blue")
M.cylinder("z", (0.48, 0.75), 0.36, 2.45, 2.52, "blue", 16)
M.cylinder("z", (0.48, 0.75), 0.26, 2.52, 2.55, "galv", 16)   # inlet screen
for k in range(-3, 4):
	M.box((0.48 + k * 0.07 - 0.006, 0.5, 2.55), (0.48 + k * 0.07 + 0.006, 1.0, 2.57), "galv")
M.cylinder("z", (0.48, 0.75), 0.17, 2.57, 2.93, "blue", 12)   # the fan motor
for z in (2.65, 2.73, 2.81):
	M.ring("z", (0.48, 0.75), 0.18, z, z + 0.02, 0.02, "blue", sides=12)
M.box((0.36, 0.92, 2.62), (0.6, 1.06, 2.82), "blue")   # its terminal box
M.box((0.4, 1.25, 1.2), (0.8, 1.65, 1.6), "blue")       # duct up to the windbox
M.box((0.42, 1.0, 1.32), (0.78, FY + 0.1, 1.38), "blue")
# gas train along the front: from the north side, a manual valve, two safety shut-off valves, the regulator, into the gun
M.pipe([(0.15, 0.0 + 0.04, 0.12), (0.15, 0.55, 0.12), (0.15, 0.55, 0.65), (0.27, 0.55, 0.65), (0.27, FY, 0.65), (0.3, FY, 0.65)], 2, "padgreen")
M.cylinder("z", (0.15, 0.55), 0.06, 0.24, 0.32, "brass", 10)
M.box((0.08, 0.65, 0.26), (0.22, 0.69, 0.3), "rubber")   # its lever
for z, tex in ((0.4, "padgreen"), (0.5, "padgreen")):
	M.box((0.08, 0.48, z - 0.04), (0.22, 0.68, z + 0.04), tex)   # the two shut-off valves
	M.box((0.1, 0.68, z - 0.03), (0.2, 0.8, z + 0.03), "dark")    # their actuators
M.cylinder("y", (0.27, 0.65), 0.1, 0.85, 1.0, "padgreen", 12)   # regulator
M.cylinder("y", (0.27, 0.65), 0.06, 1.0, 1.08, "padgreen", 10)
# burner management panel on its stand, at the north front corner
M.box((0.06, 0.0, 0.06), (0.1, 1.0, 0.1), "iron")
M.box((0.02, 1.0, 0.04), (0.3, 1.7, 0.5), "grey")
M.decal((0.018, 1.25, 0.1), (0.022, 1.6, 0.44), "controller", "west", "grey")
M.decal((0.018, 1.08, 0.18), (0.022, 1.2, 0.36), "nameplate", "west", "steel")
# the stand under the housing
for z in (FZ - 0.35, FZ + 0.35):
	M.box((0.55, 0.0, z - 0.05), (0.65, FY - 0.45, z + 0.05), "iron")
M.box((0.5, 0.0, FZ - 0.45), (0.7, 0.05, FZ + 0.45), "iron")

M.collide((0.25, 0, 0.35), (1.0, 1.95, 1.45))
M.collide((0.1, 0, 1.45), (0.9, 1.7, 2.95))
M.collide((0.02, 0, 0.04), (0.3, 1.7, 0.5))
