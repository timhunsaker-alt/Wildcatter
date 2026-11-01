"""The lease at true size (docs/multiblocks/lease.md), each part drawn facing north from its own main cell (the footprints match
LeasePartBlock's):
- separator, 2 x 2 x 4: a 36 in x 10 ft horizontal three-phase separator on its skid and saddles; the inlet at the front (north)
  end, the gas outlet on top at the back, the oil and water outlets out of the back end, its level controllers, dump valves,
  sight glasses, relief valve and gauge;
- stock_tank, 4 x 6 x 4: a 400 bbl API 12F welded steel tank, 12 ft across and 20 ft tall, with its thief hatch, gauge board,
  ladder and walkway, its bottom-ring connections;
- water_tank, 4 x 6 x 4: the same tank in fiberglass, ribbed, for produced water;
- flare_stack, 1 x 10 x 1: the knockout pot, the 8 in riser and the flare tip with its pilot line;
- injection_pump, 2 x 1 x 6: an H-pump skid, the 100 hp motor at the front, the coupling (it turns) under its guard, the thrust
  chamber, the multistage barrel, the suction at the back end and the discharge on top;
- disposal_wellhead, one cell: casing and tubing heads and the injection wing with its check valve and gauge.
Turning part: the injection pump's coupling."""
import math
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def disc(M, cx, cz, r, y1, y2, tex, step=0.25):
	"""A flat round plate made of strips along x (they split at the cells), for a tank's roof or floor."""
	z = cz - r
	while z < cz + r - 1e-6:
		mid = min(z + step / 2, cz + r)
		half = math.sqrt(max(0.0, r * r - (mid - cz) ** 2))
		M.box((cx - half, y1, z), (cx + half, y2, min(z + step, cz + r)), tex)
		z += step


# ---- the separator --------------------------------------------------------------------------------------------------------------
SEP = model("separator", 2, 2, 4)
for x in (0.1, 1.74):   # the skid's two channels along the vessel, cross members between
	SEP.box((x, 0.0, 0.05), (x + 0.16, 0.18, 3.95), "iron")
for z in (0.1, 1.0, 2.0, 3.0, 3.8):
	SEP.box((0.26, 0.02, z), (1.74, 0.16, z + 0.1), "iron")
for z in (0.9, 3.0):   # the saddles
	SEP.box((0.5, 0.18, z), (1.5, 0.62, z + 0.25), "iron")
VY, VR = 1.05, 0.46   # the vessel's centre line and radius: 36 in
SEP.cylinder("z", (1.0, VY), VR, 0.45, 3.55, "padgreen", 16)
SEP.cylinder("z", (1.0, VY), VR * 0.8, 0.33, 0.45, "padgreen", 16)   # the heads
SEP.cylinder("z", (1.0, VY), VR * 0.8, 3.55, 3.67, "padgreen", 16)
SEP.pipe([(1.0, 1.25, 0.0), (1.0, 1.25, 0.4)], 4, "steel")             # the inlet, the north face of the front cells
SEP.flange("z", (1.0, 1.25), 0.11, 0.02, 0.03, "iron", bolts=8, bolt_tex="galv")
SEP.box((0.88, 1.15, 0.12), (1.12, 1.35, 0.3), "dark")                 # the inlet valve
SEP.pipe([(1.0, VY + VR, 3.25), (1.0, 2.0, 3.25)], 3, "steel")          # the gas outlet up through the top of the back cells
SEP.box((0.9, 1.6, 3.15), (1.1, 1.78, 3.35), "dark")                   # the back-pressure valve
SEP.pipe([(1.5, 1.3, 3.5), (1.5, 1.3, 4.0)], 2, "steel")                # the oil outlet out of the back, upper layer
SEP.box((1.4, 1.2, 3.72), (1.6, 1.4, 3.88), "dark")                    # its dump valve
SEP.pipe([(0.5, 0.75, 3.5), (0.5, 0.75, 4.0)], 2, "steel")              # the water outlet, lower layer
SEP.box((0.4, 0.65, 3.72), (0.6, 0.85, 3.88), "dark")
for x, y in ((1.6, 1.2), (1.6, 0.85)):   # the level controllers on the east side, oil over water
	SEP.box((x - 0.05, y - 0.1, 2.6), (x + 0.18, y + 0.12, 2.85), "grey")
	SEP.cylinder("x", (y + 0.02, 2.72), 0.05, x + 0.18, x + 0.2, "brass", 10)
SEP.box((1.47, 0.8, 1.6), (1.5, 1.3, 1.66), "steel")                   # the sight glasses
SEP.box((1.47, 0.6, 1.9), (1.5, 1.0, 1.96), "steel")
SEP.cylinder("y", (1.0, 2.2), 0.05, VY + VR, 1.75, "galv", 10)        # the relief valve, its outlet turned down
SEP.box((0.94, 1.75, 2.12), (1.06, 1.85, 2.28), "iron")
SEP.cylinder("z", (0.65, 1.55), 0.06, 1.4, 1.44, "brass", 12)          # the pressure gauge on the top
SEP.decal((0.59, 1.49, 1.395), (0.71, 1.61, 1.4), "gauge", "north", "brass")
SEP.decal((0.53, 0.75, 1.3), (0.54, 1.25, 2.1), "nameplate", "west", "padgreen")
SEP.decal((0.53, 0.8, 2.3), (0.54, 1.2, 2.7), "nfpa", "west", "padgreen")
g.pipe_label(SEP, (1.5, 1.3, 3.75), "z", 2, "pipe_crude", 0.35)
g.pipe_label(SEP, (0.5, 0.75, 3.75), "z", 2, "pipe_water", 0.35)
SEP.collide((0.1, 0.0, 0.05), (1.9, 0.62, 3.95))
SEP.collide((0.54, 0.59, 0.33), (1.46, 1.51, 3.67))

# ---- the tanks ------------------------------------------------------------------------------------------------------------------
TR = 1.83   # 12 ft across


def tank(name, tex, ribs):
	T = model(name, 4, 6, 4, "padgreen" if tex == "padgreen" else "polymer")
	disc(T, 2.0, 2.0, TR, 0.0, 0.08, "concrete")                          # the floor on its ring
	T.ring("y", (2.0, 2.0), TR, 0.08, 5.8, 0.04, tex, sides=24)           # the shell
	if ribs:
		for y in (1.0, 2.0, 3.0, 4.0, 5.0):
			T.ring("y", (2.0, 2.0), TR + 0.03, y - 0.04, y + 0.04, 0.04, tex, sides=24)
	else:
		for y in (1.55, 3.05, 4.55):   # the welded seams of the courses
			T.ring("y", (2.0, 2.0), TR + 0.01, y - 0.01, y + 0.01, 0.02, "iron", sides=24)
	disc(T, 2.0, 2.0, TR + 0.04, 5.8, 5.86, tex)                          # the roof, a shallow cone
	disc(T, 2.0, 2.0, 1.0, 5.86, 5.94, tex)
	disc(T, 2.0, 2.0, 0.35, 5.94, 6.0, tex)
	T.box((1.85, 6.0, 1.85), (2.15, 6.15, 2.15), "galv")                  # the thief hatch
	T.cylinder("y", (2.6, 1.3), 0.07, 5.94, 6.3, "galv", 8)               # the vent, its cap
	T.cylinder("y", (2.6, 1.3), 0.12, 6.3, 6.36, "galv", 10)
	T.ladder(1.0, 2.0 - TR - 0.06, 0.0, 5.8, -1, "galv")                   # the ladder up the north side, the walkway over the top
	T.box((0.7, 5.86, 0.12), (1.3, 5.9, 2.0), "checker")
	T.handrail([(0.72, 0.15), (0.72, 1.9)], 5.9, "galv", height=0.9)
	T.handrail([(1.28, 0.15), (1.28, 1.9)], 5.9, "galv", height=0.9)
	T.box((3.0, 0.4, 2.0 - TR - 0.05), (3.2, 5.6, 2.0 - TR - 0.03), "galv")   # the gauge board on the north side
	T.decal((2.2, 1.0, 2.0 - TR - 0.04), (2.8, 1.4, 2.0 - TR - 0.03), "nameplate", "north", tex)
	if name == "stock_tank":   # crude: the NFPA 704 placard
		T.decal((2.3, 1.6, 2.0 - TR - 0.04), (2.7, 2.0, 2.0 - TR - 0.03), "nfpa", "north", tex)
	for side in range(4):   # the bottom-ring connections, a valve on each side
		a = side * math.pi / 2
		x, z = 2.0 + (TR + 0.08) * math.sin(a), 2.0 - (TR + 0.08) * math.cos(a)
		T.box((x - 0.12, 0.3, z - 0.12), (x + 0.12, 0.54, z + 0.12), "dark")
	T.collide((0.17, 0.0, 0.17), (3.83, 5.9, 3.83))
	return T


STOCK = tank("stock_tank", "padgreen", False)
WATER = tank("water_tank", "polymer", True)

# ---- the flare stack ------------------------------------------------------------------------------------------------------------
FL = model("flare_stack", 1, 10, 1)
FL.box((0.1, 0.0, 0.1), (0.9, 0.06, 0.9), "concrete")                     # the foundation
FL.cylinder("y", (0.5, 0.5), 0.36, 0.06, 1.3, "grey", 14)                  # the knockout pot
FL.cylinder("y", (0.5, 0.5), 0.3, 1.3, 1.4, "grey", 14)
FL.pipe([(0.0, 0.5, 0.5), (0.2, 0.5, 0.5)], 4, "steel")                    # the inlet, on the side of the bottom cell
FL.pipe([(0.5, 0.5, 1.0), (0.5, 0.5, 0.8)], 4, "steel")
FL.cylinder("y", (0.5, 0.5), mlib.NPS_OD[8] / 2, 1.4, 9.6, "hipaint", 12)       # the 8 in riser
for y in (3.0, 6.0, 9.0):
	FL.ring("y", (0.5, 0.5), 0.13, y, y + 0.06, 0.02, "iron", sides=12)   # its flanges
FL.cylinder("y", (0.5, 0.5), 0.16, 9.6, 9.95, "iron", 12)                   # the flare tip and its wind shield
FL.ring("y", (0.5, 0.5), 0.2, 9.7, 10.0, 0.02, "steel", sides=12)
FL.cylinder("y", (0.72, 0.5), 0.015, 1.4, 9.9, "steel", 6)                  # the pilot's gas line and its igniter
FL.box((0.68, 9.85, 0.46), (0.76, 9.98, 0.54), "brass")
FL.decal((0.3, 0.5, 0.12), (0.7, 0.8, 0.13), "flammable", "north", "grey")
FL.collide((0.14, 0.0, 0.14), (0.86, 1.4, 0.86))
FL.collide((0.38, 1.4, 0.38), (0.62, 10.0, 0.62))

# ---- the injection pump ---------------------------------------------------------------------------------------------------------
IP = model("injection_pump", 2, 1, 6, "blue")
for x in (0.15, 1.69):   # the skid
	IP.box((x, 0.0, 0.0), (x + 0.16, 0.12, 6.0), "iron")
for z in (0.1, 1.5, 3.0, 4.5, 5.8):
	IP.box((0.31, 0.01, z), (1.69, 0.1, z + 0.1), "iron")
SY = 0.55   # the shaft line
IP.box((0.55, 0.12, 0.2), (1.45, 0.16, 1.85), "iron")                     # the motor's base
IP.cylinder("z", (1.0, SY), 0.4, 0.15, 1.85, "blue", 16)                    # the 100 hp motor
IP.fins((0.62, 0.17, 0.3), (1.38, 0.93, 1.7), "z", 10, 0.02, "blue")
IP.cylinder("z", (1.0, SY), 0.3, 0.05, 0.15, "blue", 14)                     # its fan cover
IP.box((0.35, 0.45, 0.4), (0.6, 0.75, 0.75), "grey")                         # the terminal box, west side at the front
IP.decal((0.34, 0.5, 0.45), (0.35, 0.7, 0.7), "danger", "west", "grey")
IP.ring("z", (1.0, SY), 0.2, 1.95, 2.35, 0.02, "engine", sides=12, skip=(1, 2, 4, 5, 7, 8, 10, 11))   # the open coupling guard
IP.box((0.7, 0.16, 2.35), (1.3, 0.85, 2.9), "padgreen")                     # the thrust chamber
IP.cylinder("z", (1.0, SY), 0.24, 2.9, 5.55, "padgreen", 16)                # the multistage barrel
for z in (3.2, 3.8, 4.4, 5.0):
	IP.ring("z", (1.0, SY), 0.26, z - 0.03, z + 0.03, 0.03, "iron", sides=16)   # its tie-rod bands
for z in (3.0, 5.3):
	IP.box((0.75, 0.12, z), (1.25, SY - 0.2, z + 0.15), "iron")               # the barrel's supports
IP.pipe([(1.0, SY, 5.55), (1.0, SY, 6.0)], 4, "steel")                       # the suction, the back end
g.pipe_label(IP, (1.0, SY, 5.78), "z", 4, "pipe_water", 0.3)
IP.flange("z", (1.0, SY), 0.12, 5.6, 0.03, "iron", bolts=8, bolt_tex="galv")
IP.pipe([(1.0, SY + 0.24, 3.5), (1.0, 1.0, 3.5)], 3, "steel")                # the discharge, on top by the thrust chamber
IP.cylinder("z", (1.3, 0.9), 0.05, 3.42, 3.46, "brass", 12)                 # its gauge
IP.decal((1.25, 0.85, 3.415), (1.35, 0.95, 3.42), "gauge", "north", "brass")
IP.decal((1.41, 0.4, 0.8), (1.42, 0.7, 1.3), "nameplate", "east", "blue")
IP.collide((0.15, 0.0, 0.0), (1.85, 0.95, 6.0))

# ---- the disposal wellhead ------------------------------------------------------------------------------------------------------
DW = mlib.Model("disposal_wellhead", [(0, 0, 0)], dict(TEX), TEX["padgreen"])
DW.cylinder("y", (0.5, 0.5), 0.12, -0.2, 0.05, "steel", 12, caps=False)    # the casing
DW.flange("y", (0.5, 0.5), 0.19, 0.05, 0.05, "iron", bolts=8, bolt_tex="galv")
DW.cylinder("y", (0.5, 0.5), 0.14, 0.1, 0.36, "padgreen", 14)               # the casing head
DW.flange("y", (0.5, 0.5), 0.16, 0.36, 0.05, "iron", bolts=8, bolt_tex="galv")
DW.cylinder("y", (0.5, 0.5), 0.1, 0.41, 0.6, "padgreen", 12)                # the tubing head
DW.box((0.4, 0.6, 0.4), (0.6, 0.84, 0.6), "padgreen")                      # the master valve
DW.cylinder("x", (0.72, 0.5), 0.025, 0.6, 0.76, "steel", 8)
DW.ring("x", (0.72, 0.5), 0.09, 0.76, 0.78, 0.012, "rubber", sides=12)
DW.box((0.42, 0.84, 0.42), (0.58, 0.98, 0.58), "steel")                    # the injection tee
DW.pipe([(0.42, 0.91, 0.5), (0.0, 0.91, 0.5)], 3, "steel")                 # the injection wing, west
DW.box((0.15, 0.84, 0.43), (0.3, 0.98, 0.57), "dark")                      # its check valve
DW.cylinder("y", (0.5, 0.5), 0.06, 0.98, 1.06, "iron", 10)                 # the cap and its gauge
DW.cylinder("z", (0.5, 1.12), 0.05, 0.47, 0.53, "brass", 12)
DW.decal((0.45, 1.07, 0.465), (0.55, 1.17, 0.47), "gauge", "north", "brass")
DW.collide((0.31, 0.0, 0.31), (0.69, 1.06, 0.69))

# ---- the turning coupling -------------------------------------------------------------------------------------------------------
CP = mlib.Model("rotor_injection_pump_coupling", [(0, 0, 0)], dict(TEX), TEX["iron"])
CP.cylinder("z", (0.5, 0.5), 0.07, 0.3, 0.7, "steel", 10)                  # the shafts
CP.cylinder("z", (0.5, 0.5), 0.15, 0.42, 0.5, "iron", 14)                  # the two hubs and the spacer
CP.cylinder("z", (0.5, 0.5), 0.15, 0.52, 0.6, "iron", 14)
CP.cylinder("z", (0.5, 0.5), 0.1, 0.5, 0.52, "rubber", 12)
CP.box((0.47, 0.63, 0.42), (0.53, 0.67, 0.6), "brass")                     # a mark, so it shows turning

M = SEP
M.rotors["injection_pump_coupling"] = CP
EXTRA = [STOCK, WATER, FL, IP]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
DW.export_args = {"blockstate": False, "item": True}
EXTRA.append(DW)


def after():
	import gearlib
	gearlib.save_blockstate("disposal_wellhead", {f"facing={facing}": {"model": "wildcatter:block/disposal_wellhead/p0", **({"y": y} if y else {})} for facing, y in gearlib.ROT.items()})
