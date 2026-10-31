"""The 1,000 hp land rig at true size (docs/multiblocks/land-rig.md), each part drawn facing north from its own main cell (the
cell lists match RigPartBlock's footprints). The hole is 4 cells east and south of the substructure's main cell.

- rig_substructure, 9 x 6 x 9: two box-girder side boxes 2 m wide, the drill floor 6 m up with its checker plate, the rotary
  table over the hole, handrails, a ladder, the driller's console;
- rig_mast, 3 x 43 x 3 on the floor: four legs round the top drive's path, girts every 3 m and braces, the torque track at the
  back, the racking board 27 m up on the V-door (north) side, the crown block;
- rig_drawworks, 6 x 3 x 3 on the floor behind the hole: skid, disc brakes, the drum's frame and guard (the drum turns), the
  gear case and the 1,000 hp AC motor;
- mud_pump, 5 x 3 x 3: skid, the fluid end with its manifolds and pulsation dampener, the power end, the AC motor on its stand,
  and on the south side the belts with the motor sheave and the bull wheel (both turn) under an open guard;
- mud_tanks, 3 x 3 x 12: two compartments, the walkway and handrails on top, the shale shaker, agitators;
- rig_drive_house, 12 x 4 x 4: a 40 ft container on skids with its doors, louvres and roof air conditioners, the 4.16 kV cable
  termination box with its danger sign on the west end.
Turning parts (rotors, centred on their blocks): the top drive with its traveling block, the top drive's quill, the mud pump's
motor sheave and bull wheel, the drawworks drum."""
import math
import mlib
import plantlib as g

TEX = dict(g.TEX)
PAINT, FRAME = "engine", "grey"


def model(name, cells):
	return mlib.Model(name, cells, dict(TEX), TEX[PAINT])


def strut(M, a, b, w, tex, axis):
	"""A member from a to b, w square, turned about `axis` ("x": a and b differ in y and z; "z": in x and y), in pieces of a
	metre or less so each fits its cell's model."""
	n = max(1, math.ceil(math.dist(a, b) / 1.0))
	for k in range(n):
		p = [a[i] + (b[i] - a[i]) * k / n for i in range(3)]
		q = [a[i] + (b[i] - a[i]) * (k + 1) / n for i in range(3)]
		m = [(p[i] + q[i]) / 2 for i in range(3)]
		length = math.dist(p, q)
		if axis == "x":
			deg = math.degrees(math.atan2(q[2] - p[2], q[1] - p[1]))
		else:
			deg = -math.degrees(math.atan2(q[0] - p[0], q[1] - p[1]))
		M.box((m[0] - w / 2, m[1] - length / 2, m[2] - w / 2), (m[0] + w / 2, m[1] + length / 2, m[2] + w / 2), tex, rot=(axis, deg, tuple(m)))


# ---- the substructure ------------------------------------------------------------------------------------------------------------
SUB_CELLS = [(0, 0, 0)] + [(x, y, z) for y in range(6) for z in range(9) for x in range(9) if (y == 5 or x <= 1 or x >= 7) and (x, y, z) != (0, 0, 0)]
SUB = model("rig_substructure", SUB_CELLS)
for x0 in (0.0, 7.0):   # the side boxes
	SUB.box((x0, 0.0, 0.0), (x0 + 2.0, 0.8, 9.0), PAINT)                       # the bottom box
	SUB.box((x0, 4.2, 0.0), (x0 + 2.0, 5.0, 9.0), PAINT)                       # the top box under the floor
	for z in (0.0, 2.9, 5.8, 8.7):
		for x in (x0 + 0.05, x0 + 1.65):
			SUB.box((x, 0.8, z), (x + 0.3, 4.2, z + 0.3), PAINT)               # the posts
	outer = x0 + 0.02 if x0 == 0 else x0 + 1.98
	for z1, z2 in ((0.3, 2.9), (3.2, 5.8), (6.1, 8.7)):
		strut(SUB, (outer, 0.8, z1), (outer, 4.2, z2), 0.12, FRAME, "x")       # braces on the outer face
	SUB.ladder(x0 + 1.0, 0.0 if x0 == 0 else 9.0, 0.0, 6.0, -1 if x0 == 0 else 1, "galv")
for z in (0.2, 2.4, 4.5, 6.6, 8.6):
	SUB.box((0.0, 5.0, z - 0.15), (9.0, 5.7, z + 0.15), PAINT)                 # the floor beams
SUB.box((0.0, 5.75, 0.0), (9.0, 5.95, 3.8), "checker")                         # the drill floor, open round the rotary table
SUB.box((0.0, 5.75, 5.2), (9.0, 5.95, 9.0), "checker")
SUB.box((0.0, 5.75, 3.8), (3.8, 5.95, 5.2), "checker")
SUB.box((5.2, 5.75, 3.8), (9.0, 5.95, 5.2), "checker")
SUB.ring("y", (4.5, 4.5), 0.7, 5.6, 6.0, 0.4, "iron", sides=16)               # the rotary table and its master bushing
SUB.ring("y", (4.5, 4.5), 0.3, 6.0, 6.05, 0.06, "brass", sides=12)
SUB.box((7.3, 5.95, 6.4), (8.4, 6.9, 7.3), "dark")                             # the driller's console
SUB.box((7.35, 6.9, 6.45), (8.35, 6.95, 7.25), "checker")
for k in range(3):
	SUB.cylinder("x", (6.6, 6.6 + k * 0.3), 0.06, 7.25, 7.3, "brass", 10)
	SUB.decal((7.24, 6.55, 6.55 + k * 0.3), (7.25, 6.65, 6.65 + k * 0.3), "gauge", "west", "brass")
SUB.handrail([(0.1, 0.1), (0.1, 8.9), (8.9, 8.9), (8.9, 0.1), (6.0, 0.1)], 5.95, "galv", height=1.0)   # within reach of the floor cells
SUB.decal((2.02, 2.0, 3.5), (2.03, 3.0, 5.5), "danger", "east", PAINT)          # on the inner face of the west box, at the cellar
SUB.collide((0.0, 0.0, 0.0), (2.0, 5.0, 9.0))
SUB.collide((7.0, 0.0, 0.0), (9.0, 5.0, 9.0))
SUB.collide((0.0, 5.0, 0.0), (9.0, 6.0, 9.0))

# ---- the mast ------------------------------------------------------------------------------------------------------------------------
MAST_CELLS = [(0, 0, 0)] + [(lx, y, lz) for y in range(42) for lx, lz in ((0, 0), (2, 0), (0, 2), (2, 2)) if (y, lx, lz) != (0, 0, 0)] + [(x, 42, z) for z in range(3) for x in range(3)]
MAST = model("rig_mast", MAST_CELLS)
LEGS = ((0.2, 0.2), (2.55, 0.2), (0.2, 2.55), (2.55, 2.55))
for lx, lz in LEGS:
	MAST.box((lx, 0.0, lz), (lx + 0.25, 42.0, lz + 0.25), PAINT)
	MAST.box((lx - 0.1, 0.0, lz - 0.1), (lx + 0.35, 0.1, lz + 0.35), "iron")   # the shoe on the floor
for y in range(3, 42, 3):   # girts on every face, and a brace across each face below them
	for z in (0.3, 2.65):
		MAST.box((0.45, y - 0.08, z - 0.06), (2.55, y + 0.08, z + 0.06), PAINT)
		strut(MAST, (0.45, y - 3 + 0.1, z), (2.55, y - 0.1, z), 0.08, FRAME, "z")
	for x in (0.3, 2.65):
		MAST.box((x - 0.06, y - 0.08, 0.45), (x + 0.06, y + 0.08, 2.55), PAINT)
		if x == 0.3:   # braces on the west face; the east face is left open for the racking fingers' view
			strut(MAST, (x, y - 3 + 0.1, 0.45), (x, y - 0.1, 2.55), 0.08, FRAME, "x")
for y in (1.0, 10.0, 20.0, 30.0, 40.0):   # the torque track brackets
	MAST.box((1.3, y, 2.3), (1.7, y + 0.15, 2.55), "iron")
MAST.box((1.4, 0.5, 2.25), (1.6, 41.5, 2.32), "steel")                       # the torque track the top drive rides
MAST.box((0.0, 27.0, -0.9), (3.0, 27.12, 0.3), "checker")                    # the racking board, 27 m up on the V-door side
MAST.box((0.05, 27.12, -0.9), (0.1, 28.1, 0.3), "galv")
MAST.box((2.9, 27.12, -0.9), (2.95, 28.1, 0.3), "galv")
MAST.box((0.05, 27.12, -0.9), (2.95, 28.1, -0.85), "galv")
MAST.box((0.0, 42.0, 0.0), (3.0, 42.3, 3.0), PAINT)                           # the crown deck
for z in (1.0, 1.6, 2.2):
	MAST.cylinder("x", (42.75, z), 0.45, 0.7, 2.3, "iron", 14)                # the crown's sheaves
MAST.box((0.5, 42.3, 0.5), (2.5, 42.4, 2.5), "dark")
MAST.handrail([(0.1, 0.1), (0.1, 2.9), (2.9, 2.9), (2.9, 0.1), (0.1, 0.1)], 42.3, "galv")
for lx, lz in LEGS:
	MAST.collide((lx, 0.0, lz), (lx + 0.25, 42.0, lz + 0.25))
MAST.collide((0.0, 42.0, 0.0), (3.0, 42.4, 3.0))

# ---- the drawworks -------------------------------------------------------------------------------------------------------------------
DW = model("rig_drawworks", mlib.footprint_box(6, 3, 3, 0, 0))
DW.box((0.0, 0.0, 0.2), (6.0, 0.3, 2.8), "iron")                              # the skid
for x in (0.9, 4.1):
	DW.box((x, 0.3, 0.4), (x + 0.2, 2.0, 2.6), PAINT)                         # the drum's frame ends
DW.box((1.1, 2.0, 0.4), (4.1, 2.1, 2.6), PAINT)                               # the drum guard's top
DW.box((1.1, 0.3, 2.5), (4.1, 2.0, 2.6), PAINT)                               # its back
for x in (0.3, 0.6):
	DW.cylinder("x", (1.3, 1.5), 0.75, x, x + 0.06, "steel", 16)              # the disc brakes
	DW.box((x - 0.05, 1.9, 1.3), (x + 0.11, 2.2, 1.7), "dark")                # their callipers
DW.box((4.3, 0.3, 0.6), (4.8, 1.9, 2.4), PAINT)                               # the gear case
DW.cylinder("x", (1.2, 1.5), 0.55, 4.8, 5.9, "blue", 16)                     # the 1,000 hp AC motor
DW.fins((4.9, 0.62, 0.92), (5.8, 1.78, 2.08), "x", 8, 0.02, "blue")
DW.box((5.0, 1.75, 1.2), (5.5, 2.05, 1.8), "grey")                           # its terminal box
DW.decal((4.81, 1.0, 0.9), (4.82, 1.4, 1.5), "nameplate", "east", PAINT)
DW.collide((0.0, 0.0, 0.2), (6.0, 2.2, 2.8))

# ---- a mud pump ----------------------------------------------------------------------------------------------------------------------
MP = model("mud_pump", mlib.footprint_box(5, 3, 3, 0, 0))
MP.box((0.0, 0.0, 0.2), (5.0, 0.3, 2.8), "iron")                              # the skid
MP.box((0.2, 0.3, 0.4), (1.5, 1.5, 2.6), "dark")                              # the fluid end: three modules
for z in (0.8, 1.5, 2.2):
	MP.cylinder("x", (0.95, z), 0.18, 0.05, 0.2, "steel", 10)                 # the valve covers
MP.pipe([(0.2, 0.45, 0.2), (0.2, 0.45, 2.8)], 8, "iron")                      # the suction manifold
MP.pipe([(0.85, 1.5, 1.5), (0.85, 1.75, 1.5)], 4, "steel")                    # discharge up to the dampener
MP.cylinder("y", (0.85, 1.5), 0.3, 1.75, 2.35, "padgreen", 14)                 # the pulsation dampener
MP.box((1.5, 0.3, 0.3), (3.6, 2.0, 2.7), PAINT)                               # the power end
MP.box((1.45, 2.0, 0.3), (3.65, 2.08, 2.7), "iron")
MP.box((3.7, 0.3, 0.6), (4.9, 0.9, 2.4), "iron")                             # the motor's stand
MP.cylinder("z", (4.3, 1.5), 0.55, 0.4, 2.7, "blue", 16)                     # the 1,000 hp AC motor, its shaft along z
MP.fins((3.8, 1.0, 0.5), (4.8, 2.0, 2.6), "z", 9, 0.02, "blue")
MP.box((4.6, 1.95, 1.0), (5.0, 2.2, 1.5), "grey")
for x1, x2 in ((2.4, 4.3),):   # the belts, top and bottom runs, between the sheaves on the south side
	MP.box((x1, 2.3, 2.92), (x2, 2.38, 3.02), "rubber")
	MP.box((x1, 0.7, 2.92), (x2, 0.78, 3.02), "rubber")
for x in (1.45, 5.0):   # the open belt guard: posts and rails
	MP.box((x - 0.05, 0.3, 3.1), (x, 2.5, 3.15), PAINT)
MP.box((1.45, 2.45, 3.1), (5.0, 2.5, 3.15), PAINT)
MP.box((1.45, 0.3, 3.1), (5.0, 0.35, 3.15), PAINT)
MP.decal((3.61, 1.0, 0.8), (3.62, 1.5, 1.6), "nameplate", "east", PAINT)
MP.collide((0.0, 0.0, 0.2), (5.0, 2.35, 2.8))

# ---- the mud tanks -------------------------------------------------------------------------------------------------------------------
MT = model("mud_tanks", mlib.footprint_box(3, 3, 12, 0, 0))
MT.box((0.1, 0.0, 0.0), (2.9, 0.25, 12.0), "iron")                            # the skids
MT.box((0.15, 0.25, 0.05), (2.85, 2.3, 5.95), FRAME)                           # the two compartments
MT.box((0.15, 0.25, 6.05), (2.85, 2.3, 11.95), FRAME)
for z in (1.0, 3.0, 5.0, 7.0, 9.0, 11.0):
	MT.box((0.1, 0.25, z - 0.08), (0.15, 2.3, z + 0.08), PAINT)               # stiffeners
	MT.box((2.85, 0.25, z - 0.08), (2.9, 2.3, z + 0.08), PAINT)
MT.box((0.1, 2.3, 0.0), (2.9, 2.36, 12.0), "checker")                         # the walkway grating on top
MT.handrail([(0.12, 0.1), (0.12, 11.9)], 2.36, "galv")
MT.handrail([(2.88, 0.1), (2.88, 11.9)], 2.36, "galv")
MT.box((0.5, 2.36, 0.4), (2.5, 3.2, 2.4), PAINT)                              # the shale shaker: its basket, screens and motors
MT.box((0.6, 3.0, 0.5), (2.4, 3.05, 2.3), "galv", rot=("x", 6.0, (1.5, 3.0, 1.4)))
for x in (0.45, 2.45):
	MT.cylinder("x", (2.8, 1.4), 0.15, x - 0.1, x + 0.1, "blue", 10)
for z in (4.0, 8.5, 10.5):   # the agitators' motors and gearboxes
	MT.cylinder("y", (1.5, z), 0.25, 2.36, 2.8, "blue", 12)
	MT.box((1.3, 2.8, z - 0.2), (1.7, 3.0, z + 0.2), "grey")
MT.pipe([(0.0, 0.6, 11.0), (0.15, 0.6, 11.0)], 4, "steel")                   # the suction to the pumps
MT.collide((0.1, 0.0, 0.0), (2.9, 2.36, 12.0))

# ---- the drive house -----------------------------------------------------------------------------------------------------------------
DH = model("rig_drive_house", mlib.footprint_box(12, 4, 4, 0, 0))
for z in (0.3, 3.4):
	DH.box((0.0, 0.0, z), (12.0, 0.3, z + 0.3), "iron")                       # the skids
DH.box((0.05, 0.3, 0.25), (11.95, 3.2, 3.75), "blue")                          # the container
DH.box((0.0, 3.2, 0.2), (12.0, 3.3, 3.8), "grey")
for x in (0.5, 1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5, 9.5, 10.5, 11.4):
	DH.box((x - 0.03, 0.3, 0.22), (x + 0.03, 3.2, 0.25), "blue")               # the corrugations, front and back
	DH.box((x - 0.03, 0.3, 3.75), (x + 0.03, 3.2, 3.78), "blue")
for x in (2.0, 5.0, 8.0):
	DH.box((x, 0.4, 0.2), (x + 0.9, 2.5, 0.22), "grey")                       # the doors
	DH.box((x + 0.75, 1.3, 0.17), (x + 0.82, 1.45, 0.2), "galv")
	DH.fins((x - 0.6, 1.8, 3.78), (x - 0.1, 2.6, 3.8), "y", 6, 0.03, "dark")  # the louvres, back
for x in (1.5, 6.0, 10.0):
	DH.box((x, 3.3, 0.8), (x + 1.4, 3.9, 2.4), "grey")                        # the roof air conditioners
	DH.cylinder("y", (x + 0.7, 1.6), 0.4, 3.9, 3.95, "dark", 12)
DH.box((-0.25, 0.6, 1.5), (0.05, 1.7, 2.5), "grey")                           # the 4.16 kV termination box on the west end
DH.decal((-0.26, 1.0, 1.6), (-0.25, 1.6, 2.4), "danger", "west", "grey")
DH.cylinder("y", (-0.1, 2.0), 0.08, 0.0, 0.6, "galv", 10)                     # the cable's conduit up into it
DH.decal((4.0, 2.65, 0.19), (5.0, 2.95, 0.2), "nameplate", "north", "blue")
DH.collide((0.0, 0.0, 0.2), (12.0, 3.3, 3.8))

# ---- turning parts -------------------------------------------------------------------------------------------------------------------
def rotor(name):
	return mlib.Model("rotor_" + name, [(0, 0, 0)], dict(TEX), TEX["iron"])


TD = rotor("top_drive")   # its quill's top at the block's centre; the dolly reaches back to the torque track
TD.box((0.15, -0.35, 0.15), (0.85, 0.75, 0.85), PAINT)                         # the gearbox and AC motor housing
TD.cylinder("y", (0.5, 0.5), 0.3, 0.75, 1.1, "blue", 14)                      # the motor's top
TD.box((0.3, 0.75, 1.0), (0.7, 1.05, 1.35), "grey")                           # the guide dolly to the torque track
TD.box((0.4, -0.2, 0.85), (0.6, 0.9, 1.3), "iron")
for x in (0.2, 0.75):
	TD.box((x, 1.1, 0.45), (x + 0.05, 1.5, 0.55), "steel")                    # the bail
TD.box((0.15, 1.5, 0.25), (0.85, 1.95, 0.75), "iron")                         # the traveling block
for x in (0.25, 0.5, 0.75):
	TD.cylinder("x", (1.72, 0.5), 0.2, x - 0.04, x + 0.04, "dark", 12)
TD.cylinder("z", (0.95, 0.5), 0.06, 0.5, 0.95, "rubber", 8)                   # the mud hose's gooseneck
QUILL = rotor("top_drive_quill")   # the quill and the saver sub, down to the stand
QUILL.cylinder("y", (0.5, 0.5), 0.08, -0.6, -0.35, "steel", 10)
QUILL.cylinder("y", (0.5, 0.5), 0.065, -0.95, -0.6, "steel", 10)
QUILL.cylinder("y", (0.5, 0.5), mlib.NPS_OD[4] / 2, -1.0, -0.95, "steel", 10)
QUILL.box((0.42, -0.55, 0.58), (0.46, -0.4, 0.6), "brass")                   # a mark, so it shows turning
SHEAVE = rotor("mud_pump_sheave")   # the motor's sheave, 0.5 m, its shaft along z
SHEAVE.cylinder("z", (0.5, 0.5), 0.25, 0.4, 0.6, "iron", 16)
for z in (0.44, 0.5, 0.56):
	SHEAVE.ring("z", (0.5, 0.5), 0.252, z - 0.01, z + 0.01, 0.01, "dark", sides=16)
SHEAVE.box((0.47, 0.68, 0.38), (0.53, 0.74, 0.62), "brass")
BULL = rotor("mud_pump_bull_wheel")   # the bull wheel on the pinion shaft, 1.7 m, spoked
BULL.ring("z", (0.5, 0.5), 0.85, 0.4, 0.6, 0.12, "iron", sides=24)
BULL.cylinder("z", (0.5, 0.5), 0.2, 0.38, 0.62, "iron", 12)
BULL.fan("z", (0.5, 0.5), 0.75, 0.5, 0.08, "iron", blades=6, width=0.12, hub=0.2)
BULL.box((0.47, 1.25, 0.39), (0.53, 1.33, 0.61), "brass")
DRUM = rotor("drawworks_drum")   # the grooved drum and its flanges, 2.8 m between the flanges, along x
DRUM.cylinder("x", (0.5, 0.5), 0.45, -0.9, 1.9, "iron", 16)
for x in (-0.95, 1.9):
	DRUM.cylinder("x", (0.5, 0.5), 0.62, x, x + 0.05, "steel", 16)
for k in range(9):
	DRUM.ring("x", (0.5, 0.5), 0.47, -0.8 + k * 0.3, -0.7 + k * 0.3, 0.03, "dark", sides=16)   # the wraps of drill line
DRUM.box((-0.2, 0.93, 0.45), (0.0, 0.98, 0.55), "brass")

M = SUB
for part in (TD, QUILL, SHEAVE, BULL, DRUM):
	M.rotors[part.name[len("rotor_"):]] = part
EXTRA = [MAST, DW, MP, MT, DH]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
