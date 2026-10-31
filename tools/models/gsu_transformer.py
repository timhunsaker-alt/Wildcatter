"""Generator step-up, 1,500 kVA, 4.16 kV delta / 12.47 kV grounded wye (docs/multiblocks/gsu.md), at true size.

Drawn from a typical liquid-filled substation-type unit of this rating: tank about 1.5 m wide, 1.2 m deep and 1.6 m tall on
skid channels; a bank of panel radiators on each side (about 2.2 m across them); three 15 kV class porcelain bushings
(95 kV BIL, four sheds) on the cover over the front half, with the H0 neutral bushing beside them; the 4.16 kV delta comes
in through an air-terminal cable throat on the back. Fittings: liquid-level gauge, dial thermometer, pressure-vacuum gauge,
pressure relief device, drain valve, nameplate, lifting lugs and copper ground pads.

Footprint 3 across x 2 deep x 3 high; the main cell is the back of the bottom middle, so the part spans x -1..2, z -1..1.
"""
import mlib

M = mlib.Model("gsu_transformer", mlib.footprint_box(3, 3, 2, 1, 1), {
	"paint": "block/mat/mat_paint_ansi70",
	"dark": "block/mat/mat_oil_dark",
	"porcelain": "block/mat/mat_porcelain_brown",
	"brass": "block/mat/mat_brass",
	"copper": "block/mat/mat_copper",
	"galv": "block/mat/mat_galvanized",
	"steel": "block/mat/mat_stainless",
	"rubber": "block/mat/mat_rubber",
	"nameplate": "block/mat/decal_nameplate",
	"danger": "block/mat/decal_danger_hv",
	"gauge": "block/mat/decal_gauge",
}, "block/mat/mat_paint_ansi70")

CX = 0.5                         # the tank's centre line across
X1, X2 = CX - 0.75, CX + 0.75    # tank 1.5 m wide
Z1, Z2 = -0.6, 0.6               # 1.2 m deep, front toward -z
BASE = 0.15                      # skid channels
TOP = BASE + 1.6                 # the cover

# skid: two channels running front to back, with lifting holes in the ends
for x in (X1 + 0.1, X2 - 0.25):
	M.box((x, 0, Z1 - 0.05), (x + 0.15, BASE, Z2 + 0.05), "dark")
# tank walls, the cover and its bolted lip
M.box((X1, BASE, Z1), (X2, TOP, Z2), "paint")
M.box((X1 - 0.03, TOP, Z1 - 0.03), (X2 + 0.03, TOP + 0.04, Z2 + 0.03), "paint")
M.box((X1 + 0.05, TOP + 0.04, Z1 + 0.05), (X2 - 0.05, TOP + 0.06, Z2 - 0.05), "paint")
for k in range(10):   # the cover bolts round the lip
	x = X1 + 0.05 + k * (X2 - X1 - 0.1) / 9
	for z in (Z1 - 0.015, Z2 + 0.015):
		M.box((x - 0.015, TOP + 0.04, z - 0.015), (x + 0.015, TOP + 0.06, z + 0.015), "galv")
# vertical stiffeners on the front and back walls
for k in range(4):
	x = X1 + 0.2 + k * (X2 - X1 - 0.4) / 3
	M.box((x - 0.03, BASE + 0.05, Z1 - 0.05), (x + 0.03, TOP - 0.05, Z1), "paint")
	M.box((x - 0.03, BASE + 0.05, Z2), (x + 0.03, TOP - 0.05, Z2 + 0.05), "paint")

# panel radiators: top and bottom headers off the tank wall, eight panels each side
for side, (xa, xb) in ((-1, (X1 - 0.42, X1)), (1, (X2, X2 + 0.42))):
	for y in (0.45, 1.5):
		for z in (-0.45, 0.45):
			M.cylinder("x", (y, z), 0.05, xa, xb, "paint", sides=8)
	edge = xa if side < 0 else xb - 0.32
	M.fins((edge, 0.4, -0.52), (edge + 0.32, 1.55, 0.52), "z", 8, 0.035, "paint")
	# the panels' outer flanges
	M.box((edge, 0.4, -0.52), (edge + 0.32, 0.44, 0.52), "paint")
	M.box((edge, 1.51, -0.52), (edge + 0.32, 1.55, 0.52), "paint")

# 12.47 kV bushings on the cover over the front half: porcelain with four sheds, a brass stud and a spade terminal
def bushing(x, z, height, sheds, r_core, r_shed, sides=12):
	y0 = TOP + 0.06
	M.cylinder("y", (x, z), r_shed + 0.03, y0, y0 + 0.05, "galv", sides)            # mounting flange
	M.cylinder("y", (x, z), r_core, y0 + 0.05, y0 + height, "porcelain", sides)
	for k in range(sheds):
		y = y0 + 0.1 + k * (height - 0.14) / max(1, sheds - 1)
		M.cylinder("y", (x, z), r_shed, y, y + 0.025, "porcelain", sides)
	M.cylinder("y", (x, z), r_core * 0.75, y0 + height, y0 + height + 0.03, "galv", sides)   # top cap
	M.cylinder("y", (x, z), 0.012, y0 + height + 0.03, y0 + height + 0.1, "brass", 8)      # stud
	M.box((x - 0.03, y0 + height + 0.06, z - 0.006), (x + 0.03, y0 + height + 0.12, z + 0.006), "copper")   # spade
	return y0 + height + 0.12

for k, x in enumerate((CX - 0.33, CX, CX + 0.33)):   # H1 H2 H3
	bushing(x, -0.3, 0.42, 4, 0.045, 0.08)
	M.collide((x - 0.09, TOP, -0.39), (x + 0.09, TOP + 0.55, -0.21))
bushing(X2 - 0.12, -0.42, 0.16, 2, 0.025, 0.045, sides=8)   # H0, the wye neutral, bonded to the tank
M.box((X2 - 0.135, TOP + 0.2, -0.43), (X2 - 0.105, TOP + 0.24, Z1 - 0.04), "copper")   # neutral strap down to the ground pad
M.box((X2 - 0.135, BASE + 0.1, Z1 - 0.07), (X2 - 0.105, TOP + 0.24, Z1 - 0.04), "copper")

# pressure relief device and the fill plug on the cover, lifting lugs at the corners
M.cylinder("y", (CX - 0.45, 0.3), 0.07, TOP + 0.06, TOP + 0.16, "galv", 12)
M.cylinder("y", (CX - 0.45, 0.3), 0.045, TOP + 0.16, TOP + 0.2, "rubber", 12)
M.cylinder("y", (CX + 0.4, 0.35), 0.035, TOP + 0.06, TOP + 0.12, "galv", 8)
for x in (X1 + 0.02, X2 - 0.08):
	for z in (Z1 - 0.06, Z2 + 0.0):
		M.box((x, TOP - 0.18, z), (x + 0.06, TOP + 0.03, z + 0.06), "paint")

# the 4.16 kV air-terminal throat on the back, low, with its bolted cover and the cable coming in from behind
M.box((CX - 0.3, BASE + 0.2, Z2), (CX + 0.3, BASE + 0.95, Z2 + 0.32), "paint")
M.box((CX - 0.33, BASE + 0.17, Z2 + 0.32), (CX + 0.33, BASE + 0.98, Z2 + 0.35), "paint")
for k in range(6):
	y = BASE + 0.22 + k * 0.14
	for x in (CX - 0.31, CX + 0.29):
		M.box((x, y, Z2 + 0.35), (x + 0.02, y + 0.02, Z2 + 0.36), "galv")
M.cylinder("z", (CX, 0.5), 0.09, Z2 + 0.35, 1.0, "dark", 12)   # cable entry to the back face of the main cell
M.decal((CX - 0.12, BASE + 0.7, Z2 + 0.35), (CX + 0.12, BASE + 0.9, Z2 + 0.355), "danger", "south", "paint")
M.collide((CX - 0.33, BASE + 0.17, Z2), (CX + 0.33, BASE + 0.98, Z2 + 0.36))

# front wall fittings: liquid-level gauge, thermometer, pressure-vacuum gauge, nameplate, danger sign, drain valve
for x, y in ((CX - 0.4, TOP - 0.3), (CX - 0.1, TOP - 0.3), (CX + 0.2, TOP - 0.3)):
	M.cylinder("z", (x, y), 0.07, Z1 - 0.06, Z1, "galv", 12)
	M.decal((x - 0.055, y - 0.055, Z1 - 0.065), (x + 0.055, y + 0.055, Z1 - 0.06), "gauge", "north", "galv")
M.decal((CX + 0.35, TOP - 0.55, Z1 - 0.012), (CX + 0.65, TOP - 0.3, Z1 - 0.004), "nameplate", "north", "steel")
M.decal((CX - 0.2, BASE + 0.65, Z1 - 0.008), (CX + 0.2, BASE + 0.95, Z1 - 0.003), "danger", "north", "paint")
M.cylinder("z", (CX - 0.5, BASE + 0.15), 0.035, Z1 - 0.12, Z1, "brass", 8)    # drain valve body
M.cylinder("z", (CX - 0.5, BASE + 0.15), 0.06, Z1 - 0.15, Z1 - 0.13, "rubber", 12)   # its handwheel
# copper ground pads at two corners of the base
for x, z in ((X1 + 0.02, Z1 - 0.04), (X2 - 0.1, Z2 - 0.04)):
	M.box((x, BASE + 0.05, z), (x + 0.08, BASE + 0.15, z + 0.04), "copper")

# collision: the tank, the radiator banks, the skid
M.collide((X1, 0, Z1), (X2, TOP + 0.06, Z2))
M.collide((X1 - 0.42, 0.4, -0.52), (X1, 1.55, 0.52))
M.collide((X2, 0.4, -0.52), (X2 + 0.42, 1.55, 0.52))
