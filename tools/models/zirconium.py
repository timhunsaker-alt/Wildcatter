"""The zirconium plant at true size (docs/multiblocks/zirconium.md), each part drawn facing north from its own main cell (the
footprints match ZirconiumPartBlock's):
- mineral_separator, 3 x 4 x 6: the feed bin, the magnetic and electrostatic separators in their frame (480 V box west);
- chlorinator, 3 x 10 x 3: the brick-lined fluid bed, its cyclone and condenser, chlorine in on the east;
- hafnium_column, 3 x 12 x 3: the extractive distillation column and its salt reboiler;
- kroll_reactor, 3 x 6 x 3: the furnace pit with the retort's lid and the ZrCl4 sublimer on top;
- vacuum_distillation, 3 x 6 x 3: the vacuum furnace bell, its condenser and pump;
- var_furnace, 3 x 8 x 3: the furnace head, ram and crucible, the arc supply;
- extrusion_press, 3 x 4 x 8: the press frame, its container and ram, the hydraulic pump set (4.16 kV box west);
- pilger_mill, 3 x 3 x 10: the mill stand on its bed, the feed carriage and run-out;
- vacuum_anneal_furnace, 3 x 3 x 8: the horizontal vacuum furnace and its pumps;
- magnesium_cell, 3 x 3 x 6: the refractory cell, its electrodes, the bus bars on the east, the chlorine line on the east front;
- magnesia_chlorinator, 3 x 6 x 3: the shaft furnace with its charging hopper, chlorine in on the north;
- chlor_alkali_cell, 3 x 3 x 6: the bipolar electrolyser stack in its frame, brine tank, bus bars east."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


# ---- the mineral separator ------------------------------------------------------------------------------------------------------
MS = model("mineral_separator", 3, 4, 6, "galv")
MS.box((0.0, 0.0, 0.0), (3.0, 0.2, 6.0), "concrete")
MS.box((0.6, 2.4, 0.3), (2.4, 3.9, 1.5), "galv")                            # the feed bin
for z in (2.0, 3.4, 4.8):
	MS.box((0.4, 0.2, z - 0.5), (2.6, 1.8, z + 0.5), "blue")                # magnetic and electrostatic separators
	MS.cylinder("x", (1.2, z), 0.25, 0.4, 2.6, "steel", 12)                 # their rolls
for x in (0.3, 2.6):
	MS.box((x, 0.2, 0.3), (x + 0.1, 2.4, 5.7), "iron")                      # the frame
box480(MS, 0.5, 0.4)
MS.collide((0.3, 0.0, 0.3), (2.7, 3.9, 5.7))

# ---- the chlorinator -----------------------------------------------------------------------------------------------------------
CL = model("chlorinator", 3, 10, 3, "hipaint")
CL.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
CL.cylinder("y", (1.5, 1.5), 0.95, 0.3, 7.0, "hipaint", 16)                # the brick-lined fluid bed
CL.cylinder("y", (1.5, 1.5), 0.6, 7.0, 8.4, "steel", 14)                   # the disengaging head
CL.cylinder("y", (0.6, 0.6), 0.3, 6.0, 9.6, "steel", 10)                   # the cyclone and the ZrCl4 condenser
CL.box((1.1, 8.4, 1.1), (1.9, 9.8, 1.9), "galv")                            # the coal hopper
CL.pipe([(3.0, 0.5, 1.5), (2.45, 0.5, 1.5)], 2, "steel")                    # chlorine in, the east
box480(CL, 0.5, 1.9)
CL.collide((0.5, 0.0, 0.5), (2.5, 9.8, 2.5))

# ---- the hafnium column --------------------------------------------------------------------------------------------------------
HC = model("hafnium_column", 3, 12, 3, "lagging")
HC.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
HC.cylinder("y", (1.5, 1.5), 0.55, 0.3, 11.6, "lagging", 14)               # the column
HC.box((0.3, 0.3, 0.3), (1.2, 1.6, 1.2), "hipaint")                         # the salt reboiler
HC.box((1.9, 9.5, 1.9), (2.7, 11.0, 2.7), "finned")                         # its condenser
box480(HC, 0.5, 1.9)
HC.collide((0.9, 0.0, 0.9), (2.1, 11.6, 2.1))

# ---- the Kroll reactor ------------------------------------------------------------------------------------------------------
KR = model("kroll_reactor", 3, 6, 3, "grey")
KR.box((0.1, 0.0, 0.1), (2.9, 3.2, 2.9), "hipaint")                        # the furnace
KR.cylinder("y", (1.5, 1.5), 0.85, 3.2, 3.5, "steel", 16)                   # the retort lid
KR.cylinder("y", (1.5, 1.5), 0.45, 3.5, 5.6, "steel", 12)                   # the ZrCl4 sublimer
KR.pipe([(1.5, 5.0, 1.5), (2.6, 5.0, 1.5), (2.6, 3.4, 1.5)], 2, "steel")
box480(KR, 0.5, 0.3)
KR.collide((0.1, 0.0, 0.1), (2.9, 5.6, 2.9))

# ---- the vacuum distillation ----------------------------------------------------------------------------------------------------
VD = model("vacuum_distillation", 3, 6, 3, "grey")
VD.box((0.1, 0.0, 0.1), (2.9, 2.8, 2.9), "hipaint")                        # the furnace
VD.cylinder("y", (1.5, 1.5), 0.9, 2.8, 4.8, "steel", 16)                    # the bell
VD.cylinder("y", (1.5, 1.5), 0.5, 4.8, 5.2, "steel", 12)
VD.box((2.2, 0.0, 2.2), (2.9, 1.2, 2.9), "blue")                            # the vacuum pump
box480(VD, 0.5, 0.3)
VD.collide((0.1, 0.0, 0.1), (2.9, 5.2, 2.9))

# ---- the VAR furnace -------------------------------------------------------------------------------------------------------------
VA = model("var_furnace", 3, 8, 3, "grey")
VA.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
VA.cylinder("y", (1.5, 1.5), 0.6, 0.3, 2.6, "copper", 14)                   # the water-cooled copper crucible
VA.cylinder("y", (1.5, 1.5), 0.75, 2.6, 4.4, "steel", 14)                   # the furnace head
VA.cylinder("y", (1.5, 1.5), 0.15, 4.4, 7.8, "steel", 8)                    # the ram
VA.box((1.2, 7.4, 1.2), (1.8, 7.9, 1.8), "grey")
VA.box((0.2, 0.3, 2.2), (1.2, 2.2, 2.8), "grey")                            # the arc supply
VA.box((2.1, 4.4, 0.2), (2.8, 5.6, 0.9), "galv")                            # the alloy hopper
box480(VA, 0.5, 0.3)
VA.collide((0.75, 0.0, 0.75), (2.25, 7.9, 2.25))

# ---- the extrusion press ---------------------------------------------------------------------------------------------------------
EP = model("extrusion_press", 3, 4, 8, "blue")
EP.box((0.0, 0.0, 0.0), (3.0, 0.3, 8.0), "concrete")
for z in (0.4, 5.6):
	EP.box((0.3, 0.3, z), (2.7, 3.6, z + 0.8), "blue")                      # the platens
for x, y in ((0.5, 0.6), (2.5, 0.6), (0.5, 3.2), (2.5, 3.2)):
	EP.cylinder("z", (x, y), 0.15, 1.2, 5.6, "steel", 8)                    # the tie rods
EP.cylinder("z", (1.5, 1.9), 0.55, 4.2, 5.6, "iron", 14)                    # the container
EP.cylinder("z", (1.5, 1.9), 0.25, 1.2, 4.2, "steel", 10)                   # the ram
EP.box((0.2, 0.3, 6.6), (2.8, 1.8, 7.8), "blue")                            # the hydraulic pump set
EP.box((0.0, 0.6, 0.4), (0.3, 1.4, 1.0), "grey")                            # its 4.16 kV box, west
EP.collide((0.3, 0.0, 0.3), (2.7, 3.6, 7.8))

# ---- the pilger mill ---------------------------------------------------------------------------------------------------------------
PM = model("pilger_mill", 3, 3, 10, "blue")
PM.box((0.2, 0.0, 0.2), (2.8, 0.6, 9.8), "iron")                            # its bed
PM.box((0.6, 0.6, 3.5), (2.4, 2.6, 5.5), "blue")                            # the roll stand on its carriage
PM.cylinder("x", (1.6, 4.5), 0.45, 0.4, 2.6, "steel", 12)                    # the crank
PM.cylinder("z", (1.5, 1.0), 0.06, 0.4, 3.5, "steel", 6)                     # the hollow in
PM.cylinder("z", (1.5, 1.0), 0.05, 5.5, 9.6, "steel", 6)                     # the tube out
box480(PM, 0.6, 0.4)
PM.collide((0.2, 0.0, 0.2), (2.8, 2.6, 9.8))

# ---- the vacuum anneal furnace ----------------------------------------------------------------------------------------------------
AF = model("vacuum_anneal_furnace", 3, 3, 8, "grey")
AF.box((0.0, 0.0, 0.0), (3.0, 0.3, 8.0), "concrete")
AF.cylinder("z", (1.5, 1.3), 0.95, 0.3, 7.2, "hipaint", 16)                 # the furnace vessel
AF.cylinder("z", (1.5, 1.3), 1.0, 0.1, 0.4, "steel", 16)                    # its door
AF.box((0.2, 0.3, 7.2), (1.2, 1.4, 7.9), "blue")                            # the vacuum pumps
box480(AF, 0.5, 0.3)
AF.collide((0.5, 0.0, 0.3), (2.5, 2.3, 7.9))

# ---- the magnesium cell ------------------------------------------------------------------------------------------------------------
MC = model("magnesium_cell", 3, 3, 6, "hipaint")
MC.box((0.1, 0.0, 0.1), (2.9, 1.8, 5.9), "hipaint")                          # the refractory cell
MC.box((0.0, 1.8, 0.0), (3.0, 2.0, 6.0), "grey")                              # its lid
for z in (1.0, 2.5, 4.0, 5.2):
	MC.cylinder("y", (1.0, z), 0.18, 2.0, 2.8, "dark", 8)                    # the graphite anodes
for y in (0.6, 1.2):
	MC.box((2.9, y, 2.5), (3.0, y + 0.12, 3.5), "copper")                    # the bus bars, east
MC.pipe([(2.2, 2.4, 0.5), (2.2, 2.4, 0.4), (2.2, 0.5, 0.4), (3.0, 0.5, 0.4)], 2, "steel")   # chlorine out, the east front
MC.collide((0.1, 0.0, 0.1), (2.9, 2.0, 5.9))

# ---- the magnesia chlorinator --------------------------------------------------------------------------------------------------------
MG = model("magnesia_chlorinator", 3, 6, 3, "hipaint")
MG.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
MG.cylinder("y", (1.5, 1.5), 0.9, 0.3, 4.6, "hipaint", 14)                  # the shaft
MG.box((0.9, 4.6, 0.9), (2.1, 5.9, 2.1), "galv")                            # its charging hopper
MG.pipe([(1.5, 0.5, 0.0), (1.5, 0.5, 0.6)], 2, "steel")                     # chlorine in, the north
MG.collide((0.6, 0.0, 0.6), (2.4, 5.9, 2.4))

# ---- the chlor-alkali cell -------------------------------------------------------------------------------------------------------------
CA = model("chlor_alkali_cell", 3, 3, 6, "grey")
CA.box((0.0, 0.0, 0.0), (3.0, 0.2, 6.0), "concrete")
CA.box((0.4, 0.2, 1.2), (2.4, 2.2, 4.8), "iron")                            # the electrolyser frame
for i in range(7):
	z = 1.4 + i * 0.5
	CA.box((0.5, 0.3, z), (2.3, 2.1, z + 0.3), "padgreen")                  # the bipolar elements
CA.cylinder("y", (1.4, 0.6), 0.4, 0.2, 2.6, "galv", 12)                     # the brine tank and salt hopper
for y in (0.6, 1.2):
	CA.box((2.4, y, 2.6), (3.0, y + 0.12, 3.4), "copper")                    # the bus bars, east
CA.pipe([(1.5, 0.5, 5.2), (1.5, 0.5, 6.0)], 2, "steel")                     # chlorine out, the back
CA.pipe([(2.4, 0.5, 0.0), (2.4, 0.5, 0.4)], 2, "steel")                     # hydrogen out, the front
CA.collide((0.4, 0.0, 0.2), (2.6, 2.6, 5.8))


M = MS
EXTRA = [CL, HC, KR, VD, VA, EP, PM, AF, MC, MG, CA]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
