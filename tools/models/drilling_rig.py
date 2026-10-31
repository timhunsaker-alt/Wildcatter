"""The small drilling rig at true size (docs/multiblocks/well-site.md): a skid-mounted top-head-drive rig of the class used for
water wells and shallow oil wells, drawn facing north with the hole under its main cell at the north end. 3 x 10 x 6 cells.

Still: the 2.4 x 6 m skid with its checker-plate deck and the opening over the hole, the slips bowl and conductor, the 9 m mast
(two legs, rungs and braces, the feed cylinder, the crown), the driller's console, the pipe rack with three 6 m joints, the
mud pump, and the 40 hp power unit with its louvres and terminal box at the back.
Turning (DrillingRigBlockEntity.rotors()): the top head's carriage, which only slides down the mast; the spindle with the saver sub,
which turns; and the drill pipe below it in 2 m pieces. Each is drawn centred on its own block."""
import mlib
import plantlib as g

CELLS = ([(0, 0, 0)] + [(x, 0, z) for z in range(6) for x in (-1, 0, 1) if (x, z) != (0, 0)] + [(0, y, 0) for y in range(1, 10)]
	+ [(-1, 1, z) for z in (1, 2, 3)] + [(1, 1, 1)] + [(x, 1, z) for z in (4, 5) for x in (-1, 0, 1)])
TEX = dict(g.TEX)
PAINT = "engine"
DECK = 0.36
TOP = 9.0

M = mlib.Model("drilling_rig", CELLS, TEX, TEX["engine"])
# the skid: two beams and the checker-plate deck, open over the hole
for x in (-0.7, 1.5):
	M.box((x, 0.0, 0.05), (x + 0.2, 0.3, 5.95), PAINT)
for z in (0.05, 1.5, 3.0, 4.5, 5.85):
	M.box((-0.5, 0.05, z), (1.5, 0.28, z + 0.1), "iron")
M.box((-0.7, 0.3, 0.75), (1.7, DECK, 5.95), "checker")
M.box((-0.7, 0.3, 0.0), (0.25, DECK, 0.75), "checker")
M.box((0.75, 0.3, 0.0), (1.7, DECK, 0.75), "checker")
M.box((0.25, 0.3, 0.0), (0.75, DECK, 0.25), "checker")
M.ring("y", (0.5, 0.5), 0.22, 0.24, DECK + 0.08, 0.06, "iron", sides=12)   # the slips bowl
M.cylinder("y", (0.5, 0.5), mlib.NPS_OD[10] / 2, -0.3, 0.24, "steel", 12, caps=False)   # the conductor pipe into the ground
# the mast behind the hole: two legs, rungs, braces, the feed cylinder and the crown
for x in (0.12, 0.8):
	M.box((x, DECK, 0.78), (x + 0.08, TOP, 0.92), PAINT)
y = DECK + 0.6
while y < TOP - 0.3:
	M.box((0.2, y, 0.82), (0.8, y + 0.06, 0.88), PAINT)
	M.box((0.2, y + 0.3 - 0.03, 0.83), (0.8, y + 0.3 + 0.03, 0.87), PAINT, rot=("z", 26.6, (0.5, y + 0.3, 0.85)))
	y += 0.6
M.cylinder("y", (0.5, 0.95), 0.05, DECK + 0.2, TOP - 0.4, "steel", 10)    # the hydraulic feed cylinder
M.box((0.05, TOP, 0.35), (0.95, TOP + 0.15, 0.95), PAINT)                  # the crown
M.cylinder("x", (TOP + 0.3, 0.6), 0.15, 0.4, 0.6, "iron", 12)             # the crown sheave
for x in (0.15, 0.85):   # guide rails for the head's carriage
	M.box((x - 0.02, DECK + 0.5, 0.74), (x + 0.02, TOP - 0.2, 0.78), "steel")
# the driller's console beside the hole, with its levers and gauges
M.box((1.15, DECK, 1.2), (1.65, DECK + 1.0, 1.55), PAINT)
M.box((1.12, DECK + 0.8, 1.15), (1.68, DECK + 1.05, 1.6), "dark")
for k in range(4):
	M.box((1.2 + k * 0.11, DECK + 1.05, 1.3), (1.23 + k * 0.11, DECK + 1.3, 1.33), "rubber")   # the control levers
for k in range(2):
	M.cylinder("z", (1.3 + k * 0.2, DECK + 0.9), 0.05, 1.11, 1.15, "brass", 10)
	M.decal((1.26 + k * 0.2, DECK + 0.86, 1.105), (1.34 + k * 0.2, DECK + 0.94, 1.11), "gauge", "north", "brass")
# the pipe rack: three 6 m joints of 2 3/8 in drill pipe with their tool joints
for x in (-0.65, -0.1):
	for z in (1.0, 3.0, 5.0):
		M.box((x, DECK, z), (x + 0.06, DECK + 0.6, z + 0.06), "iron")
M.box((-0.68, DECK + 0.6, 0.98), (-0.02, DECK + 0.64, 1.08), "iron")
M.box((-0.68, DECK + 0.6, 2.98), (-0.02, DECK + 0.64, 3.08), "iron")
M.box((-0.68, DECK + 0.6, 4.98), (-0.02, DECK + 0.64, 5.08), "iron")
for k in range(3):
	x = -0.55 + k * 0.17
	M.cylinder("z", (x, DECK + 0.68), mlib.NPS_OD[2] / 2, 0.0, 6.0, "steel", 8)
	M.cylinder("z", (x, DECK + 0.68), 0.043, 0.0, 0.2, "steel", 8)
	M.cylinder("z", (x, DECK + 0.68), 0.043, 5.8, 6.0, "steel", 8)
# the mud pump on the deck, its suction and discharge
M.box((1.0, DECK, 2.2), (1.6, DECK + 0.55, 3.2), "blue")
M.cylinder("z", (1.3, DECK + 0.3), 0.12, 3.2, 3.5, "iron", 12)
M.pipe([(1.3, DECK + 0.55, 2.5), (1.3, DECK + 0.9, 2.5), (1.3, DECK + 0.9, 0.95), (0.95, DECK + 0.9, 0.95)], 2, "steel")   # discharge to the swivel's hose
# the 40 hp power unit: motor, hydraulic pumps and the reservoir in an enclosure, louvres on the sides
M.box((-0.6, DECK, 4.1), (1.6, DECK + 1.25, 5.9), PAINT)
M.box((-0.62, DECK + 1.25, 4.08), (1.62, DECK + 1.3, 5.92), "iron")
for side in (-0.61, 1.6):
	M.fins((side, DECK + 0.3, 4.3), (side + 0.01, DECK + 1.0, 5.7), "z", 9, 0.03, "dark")
M.box((0.3, DECK + 0.25, 5.9), (0.7, DECK + 0.6, 5.98), "grey")     # the terminal box, low on the back
M.decal((0.75, DECK + 0.7, 5.9), (1.25, DECK + 1.0, 5.91), "nameplate", "south", PAINT)
M.decal((-0.3, DECK + 0.7, 5.9), (0.2, DECK + 1.0, 5.91), "danger", "south", PAINT)
M.collide((-0.7, 0.0, 0.75), (1.7, DECK, 5.95))
M.collide((-0.7, 0.0, 0.0), (0.25, DECK, 0.75))
M.collide((0.75, 0.0, 0.0), (1.7, DECK, 0.75))
M.collide((0.1, DECK, 0.74), (0.9, TOP + 0.15, 0.95))
M.collide((-0.6, DECK, 4.1), (1.6, DECK + 1.3, 5.9))
M.collide((1.15, DECK, 1.2), (1.65, DECK + 1.05, 1.55))
M.collide((-0.68, DECK, 0.98), (-0.02, DECK + 0.72, 5.08))


def rotor(name):
	return mlib.Model("rotor_" + name, [(0, 0, 0)], dict(TEX), TEX["iron"])


CARRIAGE = rotor("rig_carriage")   # the top head's body on its carriage: slides, does not turn (the spindle at the block's centre)
CARRIAGE.box((0.22, 0.25, 0.25), (0.78, 0.65, 0.75), PAINT)          # the gearbox
CARRIAGE.cylinder("y", (0.5, 0.42), 0.12, 0.65, 0.95, "dark", 12)   # the hydraulic motor on top
CARRIAGE.box((0.1, 0.2, 0.72), (0.9, 0.7, 0.8), "iron")             # the carriage plate on the guides
CARRIAGE.cylinder("z", (0.75, 0.6), 0.04, 0.3, 0.72, "rubber", 8)   # the mud hose to the swivel
SPINDLE = rotor("rig_spindle")     # turns: the spindle, the saver sub and the first 1.3 m of pipe
SPINDLE.cylinder("y", (0.5, 0.5), 0.06, 0.05, 0.25, "steel", 10)
SPINDLE.box((0.43, 0.1, 0.56), (0.47, 0.2, 0.6), "brass")            # a mark, so it shows turning
SPINDLE.cylinder("y", (0.5, 0.5), 0.043, -0.1, 0.05, "steel", 8)     # the tool joint
SPINDLE.cylinder("y", (0.5, 0.5), mlib.NPS_OD[2] / 2, -1.0, -0.1, "steel", 8)
PIPE = rotor("rig_pipe")           # 2 m of drill pipe, turning
PIPE.cylinder("y", (0.5, 0.5), mlib.NPS_OD[2] / 2, -0.5, 1.5, "steel", 8)
PIPE.box((0.52, 1.2, 0.48), (0.535, 1.3, 0.52), "brass")
for part in (CARRIAGE, SPINDLE, PIPE):
	M.rotors[part.name[len("rotor_"):]] = part
