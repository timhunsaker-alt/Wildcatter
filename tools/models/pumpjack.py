"""The pumpjack at true size (docs/multiblocks/well-site.md): a conventional beam pumping unit, API size 80-119-54, drawn facing
north with its horsehead over the well one cell in front of the main cell (the well's centre is z = -0.5). 3 x 5 x 7 cells.

Still: the base (two wide-flange rails and cross members), the Samson post (an A-frame each side), the saddle bearing, the gear
reducer with its crank shaft, the 10 hp motor on its slide rails with the belt guard and terminal box, the brake lever, a ladder
up the post and the nameplate.
Turning (KineticRenderer, PumpjackBlockEntity.rotors(), kinetic/BeamLinkage): the crank arms and counterweights; the pitman arms
(in two halves); the walking beam with the horsehead and the equalizer (in three pieces); the carrier bar and polished rod, and
the bridle wires. Each is drawn centred on its own block, at the beam level and the crank at zero (the pins toward the back).
The geometry is BeamLinkage's: saddle 4.3 m up at z 2.3, crank centre 1.5 m up at z 4.7, a 0.59 m crank, a 2.9 m pitman."""
import math
import mlib
import plantlib as g

SADDLE = (4.3, 2.3)      # (y, z)
CRANK = (1.5, 4.7)
R, PITMAN, FRONT, BACK = 0.59, 2.9, 2.8, 2.4

# the bottom cells either side of the motor's terminal box (z 5) are left open, so it can be wired from three sides
CELLS = [(0, 0, 0)] + [(x, y, z) for y in range(5) for z in range(6) for x in (-1, 0, 1) if (x, y, z) != (0, 0, 0) and not (y == 0 and z == 5 and x != 0)] + [(0, y, -1) for y in range(1, 5)]
TEX = dict(g.TEX)
FRAME, HEAD = "grey", "engine"


def strut(M, x1, x2, a, b, w, tex):
	"""A member from a = (y, z) to b = (y, z), w thick in z, between x1 and x2 (a box along y turned about x)."""
	(ya, za), (yb, zb) = a, b
	deg = math.degrees(math.atan2(zb - za, yb - ya))
	n = max(1, math.ceil(math.hypot(yb - ya, zb - za) / 1.0))   # in pieces of a metre or less, so each fits its cell's model
	for k in range(n):
		y1, z1 = ya + (yb - ya) * k / n, za + (zb - za) * k / n
		y2, z2 = ya + (yb - ya) * (k + 1) / n, za + (zb - za) * (k + 1) / n
		length = math.hypot(y2 - y1, z2 - z1)
		ym, zm = (y1 + y2) / 2, (z1 + z2) / 2
		M.box((x1, ym - length / 2, zm - w / 2), (x2, ym + length / 2, zm + w / 2), tex, rot=("x", deg, ((x1 + x2) / 2, ym, zm)))


M = mlib.Model("pumpjack", CELLS, TEX, TEX["iron"])
# the base: two W10 rails 1.8 m apart and cross members, on the ground
for x in (-0.4, 1.25):
	M.box((x, 0.0, 0.0), (x + 0.15, 0.25, 5.0), "iron")
for z in (0.1, 1.8, 3.0, 4.4):
	M.box((-0.25, 0.05, z), (1.25, 0.2, z + 0.12), "iron")
for x in (0.05, 0.8):   # the motor's slide rails run on back past the base
	M.box((x, 0.0, 4.9), (x + 0.15, 0.25, 6.0), "iron")
# the Samson post: an A-frame on each side, its legs meeting under the saddle bearing
for x in (-0.3, 1.15):
	strut(M, x, x + 0.15, (0.25, 1.2), (SADDLE[0] - 0.25, SADDLE[1] - 0.12), 0.14, FRAME)
	strut(M, x, x + 0.15, (0.25, 3.4), (SADDLE[0] - 0.25, SADDLE[1] + 0.12), 0.14, FRAME)
	strut(M, x, x + 0.15, (1.6, 1.62), (1.6, 3.0), 0.08, FRAME)   # a brace across the legs
M.box((-0.3, SADDLE[0] - 0.3, SADDLE[1] - 0.2), (1.3, SADDLE[0] - 0.2, SADDLE[1] + 0.2), FRAME)   # the post's cap
M.box((0.3, SADDLE[0] - 0.22, SADDLE[1] - 0.15), (0.7, SADDLE[0] - 0.12, SADDLE[1] + 0.15), "iron")   # the saddle bearing housing
M.ladder(0.5, SADDLE[1] - 0.5, 0.25, SADDLE[0] - 0.4, -1, "galv")
# the gear reducer and its crank shaft
M.box((0.18, 0.25, CRANK[1] - 0.5), (0.82, CRANK[0] + 0.25, CRANK[1] + 0.45), FRAME)
M.box((0.15, 0.25, CRANK[1] - 0.55), (0.85, 0.4, CRANK[1] + 0.5), "iron")
M.cylinder("x", CRANK, 0.12, -0.05, 1.05, "steel", 12)
M.decal((0.83, CRANK[0] - 0.2, CRANK[1] - 0.3), (0.84, CRANK[0], CRANK[1] + 0.1), "nameplate", "east", FRAME)
# the 10 hp motor on its slide rails behind, the belt guard on the west side, the terminal box
M.box((0.1, 0.25, 5.3), (0.9, 0.35, 5.95), "iron")
M.cylinder("x", (0.65, 5.62), 0.2, 0.15, 0.75, "blue", 14)
M.fins((0.17, 0.48, 5.44), (0.73, 0.82, 5.8), "x", 7, 0.012, "blue")
M.box((0.75, 0.55, 5.5), (0.83, 0.75, 5.75), "grey")
M.box((0.02, 0.4, CRANK[1] - 0.25), (0.16, CRANK[0] + 0.2, 5.85), "galv")   # the belt guard, motor sheave to the reducer
M.box((0.85, 0.35, 4.2), (0.9, 1.4, 4.26), "dark")   # the brake lever
M.box((0.84, 1.3, 4.18), (0.92, 1.42, 4.28), "rubber")
M.collide((-0.4, 0.0, 0.0), (1.4, 0.25, 5.0))
M.collide((0.05, 0.0, 5.0), (0.95, 0.25, 6.0))
M.collide((0.15, 0.25, CRANK[1] - 0.55), (0.85, CRANK[0] + 0.25, 5.95))
M.collide((-0.3, 0.25, 1.2), (1.3, SADDLE[0] - 0.2, 3.4))


def rotor(name):
	return mlib.Model("rotor_" + name, [(0, 0, 0)], dict(TEX), TEX["iron"])


C = (0.5, 0.5)   # a rotor's centre, (y, z)

crank = rotor("pumpjack_crank")   # about x through the crank shaft; the pins toward +z, the counterweights toward -z
for x, out in ((-0.2, -1), (1.06, 1)):   # a crank each side of the reducer, just inside the base rails
	crank.box((x + 0.02, 0.38, 0.4), (x + 0.12, 0.62, 0.5 + R + 0.08), "iron")   # the crank arm
	crank.cylinder("x", (0.5, 0.5 + R), 0.05, x + (0.0 if out < 0 else 0.02), x + 0.14, "steel", 10)   # its wrist pin
	crank.box((x, 0.12, -0.45), (x + 0.14, 0.88, 0.25), "dark")                  # the counterweight
	crank.box((x + (0.0 if out < 0 else 0.135), 0.15, -0.42), (x + (0.005 if out < 0 else 0.14), 0.85, 0.22), "brass")   # its painted edge
	crank.cylinder("x", C, 0.13, x + 0.02, x + 0.12, "iron", 12)                 # the hub

def pitman(name, y1, y2, top):
	P = rotor(name)
	for x in (-0.17, 1.09):
		P.box((x, y1, 0.45), (x + 0.08, y2, 0.55), FRAME)   # standing up from the wrist pin at the block's centre
	if not top:
		for x in (-0.19, 1.07):
			P.cylinder("x", C, 0.07, x, x + 0.12, "iron", 10)   # the bearing on the wrist pin
	return P

PITMAN_LOW = pitman("pumpjack_pitman_low", 0.5, 0.5 + PITMAN / 2, False)     # the lower half
PITMAN_HIGH = pitman("pumpjack_pitman_high", -0.95, 0.5, True)               # the upper half, its centre half a pitman up

def beam(name, z1, z2):
	"""A piece of the walking beam, z1..z2 from the piece's own centre (minus is forward), drawn about the block's centre; the
	three pieces sit 1.6 m forward, at the saddle and 1.6 m back, and meet end to end."""
	B = rotor(name)
	B.box((0.38, 0.5 - 0.22, 0.5 + z1), (0.62, 0.5 + 0.12, 0.5 + z2), HEAD)   # the W24 beam
	B.box((0.33, 0.5 + 0.12, 0.5 + z1), (0.67, 0.5 + 0.15, 0.5 + z2), HEAD)   # its top flange
	return B

BEAM_FRONT = beam("pumpjack_beam_front", -1.3, 0.8)   # from just behind the horsehead to the middle piece
for k in range(9):   # the horsehead: an arc of the front arm's radius about the saddle, from 20 degrees below to 12 above
	a1, a2 = math.radians(-20 + k * 32 / 9), math.radians(-20 + (k + 1) * 32 / 9)
	am = (a1 + a2) / 2
	ym, zm = FRONT * math.sin(am), -FRONT * math.cos(am) + 1.6   # from the saddle, then into this piece's frame
	BEAM_FRONT.box((0.32, 0.5 + ym - FRONT * (a2 - a1) / 2, 0.5 + zm), (0.68, 0.5 + ym + FRONT * (a2 - a1) / 2, 0.5 + zm + 0.3), HEAD, rot=("x", math.degrees(am), (0.5, 0.5 + ym, 0.5 + zm)))
BEAM_FRONT.box((0.42, 0.25, 0.5 - 1.2), (0.58, 0.62, 0.5 - 0.3), HEAD)   # the horsehead's web back to the beam
BEAM_MID = beam("pumpjack_beam_mid", -0.8, 0.8)
BEAM_MID.box((0.28, 0.5 - 0.3, 0.3), (0.72, 0.5 - 0.22, 0.7), "iron")     # the saddle on its bearing
BEAM_BACK = beam("pumpjack_beam_back", -0.8, 1.2)                         # from the middle piece to the tail and the equalizer
BEAM_BACK.box((-0.19, 0.5 - 0.15, 0.5 + 0.8 - 0.1), (1.19, 0.5 + 0.1, 0.5 + 0.8 + 0.1), HEAD)   # the equalizer across, over the pitmans
BEAM_BACK.cylinder("x", (0.5 - 0.05, 0.5 + 0.8), 0.06, -0.19, 1.19, "steel", 10)

ROD = rotor("pumpjack_rod")   # the carrier bar at the block's centre, the polished rod through it (its centre at the well)
ROD.box((0.3, 0.45, 0.42), (0.7, 0.55, 0.58), "iron")
ROD.cylinder("y", (0.5, 0.5), 0.019, -0.5, 0.7, "steel", 8)    # a 1 1/2 in polished rod through the carrier bar
ROD_LOW = rotor("pumpjack_rod_low")   # the rest of it, down through the stuffing box (its centre 1.5 m below the carrier bar)
ROD_LOW.cylinder("y", (0.5, 0.5), 0.019, -0.5, 1.5, "steel", 8)
ROD.box((0.45, 0.55, 0.45), (0.55, 0.65, 0.55), "iron")         # the rod clamp
BRIDLE = rotor("pumpjack_bridle")   # the two wires, from the carrier bar up the horsehead's face (its centre 1 m up)
for x in (0.36, 0.62):
	BRIDLE.box((x, -0.5, 0.48), (x + 0.02, 1.7, 0.52), "dark")

for part in (crank, PITMAN_LOW, PITMAN_HIGH, BEAM_FRONT, BEAM_MID, BEAM_BACK, ROD, ROD_LOW, BRIDLE):
	M.rotors[part.name[len("rotor_"):]] = part
