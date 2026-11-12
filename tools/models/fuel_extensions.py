"""Fuel fabrication's added parts at true size (docs/multiblocks/fuel-fabrication-extensions.md), each drawn facing north from its own
main cell (the footprints match FuelPartBlock's):
- pellet_inspection, 2 x 3 x 4: the pellet conveyor through a laser-gauge head and a camera hood, the reject chute down to a scrap can
  (box west);
- rod_inspection, 3 x 3 x 10: the rod conveyor through the weld X-ray cabinet, the vacuum chamber with its helium leak detector and the
  passive gamma scanner, the reject saw (box west);
- scrap_oxidizer, 2 x 3 x 4: the tray furnace, its air fan and the can hopper on top (box north, its west side against the granulator)."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.4, z=0.3, x=0.0, face="west"):
	if face == "west":
		M.box((x, y, z), (x + 0.3, y + 0.6, z + 0.5), "grey")
		M.decal((x - 0.01, y + 0.1, z + 0.1), (x, y + 0.5, z + 0.4), "danger", "west", "grey")
	else:
		M.box((x - 0.3, y, z), (x, y + 0.6, z + 0.5), "grey")
		M.decal((x, y + 0.1, z + 0.1), (x + 0.01, y + 0.5, z + 0.4), "danger", "east", "grey")


# ---- the pellet inspection bench --------------------------------------------------------------------------------------------------
PI = model("pellet_inspection", 2, 3, 4, "grey")
PI.box((0.0, 0.0, 0.0), (2.0, 0.2, 4.0), "concrete")
PI.box((0.3, 0.2, 0.0), (1.7, 0.9, 4.0), "grey")                              # the bench
PI.box((0.8, 0.9, 0.0), (1.2, 1.0, 4.0), "rubber")                            # the pellet conveyor, grinder to loader
PI.box((0.5, 1.0, 1.0), (1.5, 1.6, 1.6), "steel")                             # the laser-gauge head
PI.box((0.4, 1.0, 2.2), (1.6, 2.0, 3.2), "polymer")                           # the camera hood
PI.cylinder("y", (1.0, 2.7), 0.12, 2.0, 2.4, "grey", 8)                        # its light
PI.pipe([(1.6, 0.95, 3.4), (1.8, 0.95, 3.4), (1.8, 0.6, 3.4)], 3, "steel")   # the reject chute, down to the can
PI.box((1.6, 0.2, 3.2), (2.0, 0.6, 3.7), "galv")                              # the scrap can
box480(PI, 0.3, 0.3)
PI.collide((0.0, 0.0, 0.0), (2.0, 1.0, 4.0))

# ---- the rod inspection line ------------------------------------------------------------------------------------------------------
RI = model("rod_inspection", 3, 3, 10, "grey")
RI.box((0.0, 0.0, 0.0), (3.0, 0.2, 10.0), "concrete")
RI.box((1.2, 0.2, 0.0), (1.8, 0.8, 10.0), "grey")                             # the rod conveyor's frame
RI.box((1.25, 0.8, 0.0), (1.75, 0.85, 10.0), "rubber")
RI.box((0.4, 0.2, 0.6), (2.6, 2.4, 2.8), "hipaint")                           # the weld X-ray cabinet, lead-lined
RI.decal((0.39, 1.4, 1.2), (0.4, 2.0, 2.2), "danger", "west", "hipaint")
RI.cylinder("z", (1.0, 1.5), 0.55, 3.4, 6.6, "steel", 14)                      # the vacuum chamber of the helium leak test
RI.box((0.4, 0.2, 4.4), (1.0, 1.2, 5.4), "blue")                              # its vacuum pumps
RI.box((2.1, 0.2, 4.4), (2.7, 1.6, 5.4), "grey")                              # the helium mass-spectrometer
RI.box((0.7, 0.2, 7.2), (2.3, 2.0, 8.6), "polymer")                           # the passive gamma scanner
RI.cylinder("x", (1.3, 9.3), 0.25, 1.9, 2.0, "steel", 12)                      # the reject saw
box480(RI, 0.3, 0.1)
RI.collide((0.0, 0.0, 0.0), (3.0, 2.4, 10.0))

# ---- the scrap oxidizer -------------------------------------------------------------------------------------------------------------
SO = model("scrap_oxidizer", 2, 3, 4, "iron")
SO.box((0.0, 0.0, 0.0), (2.0, 0.2, 4.0), "concrete")
SO.box((0.2, 0.2, 0.3), (1.8, 1.8, 3.4), "iron")                              # the tray furnace
for z in (0.8, 1.6, 2.4):
	SO.box((0.18, 0.5, z), (0.2, 1.5, z + 0.6), "dark")                       # its tray doors, west to the granulator's side
SO.box((0.5, 1.8, 1.2), (1.5, 2.6, 2.2), "galv")                              # the can hopper
SO.cylinder("z", (1.2, 3.7), 0.3, 3.4, 4.0, "blue", 10)                        # the air fan, south
SO.cylinder("y", (1.4, 0.6), 0.15, 1.8, 2.8, "galv", 8)                        # the vent
SO.box((0.2, 0.3, 0.0), (0.7, 0.9, 0.3), "grey")                              # its box, north
SO.decal((0.3, 0.4, -0.01), (0.6, 0.8, 0.0), "danger", "north", "grey")
SO.collide((0.0, 0.0, 0.0), (2.0, 1.8, 4.0))

# ---- the HF recovery condenser (19c2) ------------------------------------------------------------------------------------------------
HR = model("hf_recovery_condenser", 2, 6, 2, "grey")
HR.box((0.0, 0.0, 0.0), (2.0, 0.2, 2.0), "concrete")
HR.cylinder("y", (0.7, 1.0), 0.4, 0.2, 5.6, "dark", 12)                         # the graphite condenser stack
HR.cylinder("y", (1.5, 1.2), 0.25, 0.2, 4.2, "polymer", 10)                     # the small column, lined
HR.pipe([(0.0, 5.0, 1.0), (0.7, 5.0, 1.0)], 6, "steel")                        # the off-gas in from the kiln, west
HR.pipe([(1.5, 0.5, 1.2), (1.5, 0.5, 2.0)], 3, "polymer")                      # aqueous HF out, south
HR.box((0.2, 0.3, 0.0), (0.7, 0.9, 0.3), "grey")                               # its box, north
HR.decal((0.3, 0.4, -0.01), (0.6, 0.8, 0.0), "danger", "north", "grey")
HR.ladder(1.75, 0.1, 0.2, 5.6, -1, "galv")
HR.collide((0.0, 0.0, 0.0), (2.0, 5.6, 2.0))

# ---- the IFBA coater (19c2) -------------------------------------------------------------------------------------------------------------
IB = model("ifba_coater", 3, 3, 6, "grey")
IB.box((0.0, 0.0, 0.0), (3.0, 0.2, 6.0), "concrete")
IB.cylinder("z", (1.2, 1.5), 0.9, 0.6, 5.0, "steel", 16)                        # the sputter chamber
for z in (1.6, 3.0, 4.4):
	IB.cylinder("x", (1.2, z), 0.25, 2.3, 2.9, "copper", 10)                   # the ZrB2 magnetron targets
IB.box((0.3, 0.2, 5.1), (2.7, 1.6, 5.9), "blue")                              # the vacuum pumps
IB.box((1.1, 2.1, 0.8), (1.9, 2.9, 1.6), "galv")                              # the hopper for tubes and boric acid
IB.box((0.2, 0.3, 0.0), (0.7, 0.9, 0.3), "grey")                               # its box, north (its west against the loader)
IB.decal((0.3, 0.4, -0.01), (0.6, 0.8, 0.0), "danger", "north", "grey")
IB.collide((0.0, 0.0, 0.0), (3.0, 2.1, 6.0))


M = PI
EXTRA = [RI, SO, HR, IB]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
