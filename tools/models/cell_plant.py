"""Phase 22a at true size (docs/multiblocks/grid-batteries.md), each part drawn facing north from its own main cell (the footprints match
ChemPartBlock's). The 480 V (or industrial MV) box is on the west face of the main cell:
- graphite_line, 3 x 6 x 6: the crusher, a bank of flotation cells and two spheroidising mills, the feed chute under its hopper;
- graphite_furnace, 4 x 5 x 8: the brick-lined furnace with its electrode bus, the furnace transformer, nitrogen east of its main cell;
- lfp_kiln, 3 x 4 x 12: the precipitation tanks and the long electric roller-hearth kiln; acid east of its main cell, nitrogen east at the back;
- cell_line, 4 x 5 x 16: the dry-room building with the coater's oven on its roof; HF and ethylene east of its first two cells."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.4, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


def stub(M, a, b, nps=2, tex="steel"):
	M.pipe([a, b], nps, tex)


# ---- the graphite line ----------------------------------------------------------------------------------------------------------
GL = model("graphite_line", 3, 6, 6, "grey")
GL.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "concrete")
GL.box((0.4, 0.3, 0.3), (2.6, 2.4, 1.8), "blue")                                # the crusher
GL.box((0.9, 2.4, 0.6), (2.1, 5.8, 1.5), "galv")                                # its feed chute under the hopper
for z in (2.1, 3.0):
	GL.box((0.3, 0.3, z), (2.7, 1.5, z + 0.8), "steel")                         # flotation cells
	GL.cylinder("y", (1.5, z + 0.4), 0.12, 1.5, 2.1, "blue", 8)                 # their agitator drives
for z in (4.2, 5.2):
	GL.cylinder("z", (1.5, 1.1), 0.8, z, z + 0.7, "hipaint", 14)                # spheroidising mills
GL.box((0.2, 0.3, 4.0), (2.8, 0.5, 5.9), "iron")
box480(GL, 0.4, 0.2)
GL.collide((0.2, 0.0, 0.2), (2.8, 2.4, 5.9))

# ---- the graphite furnace -------------------------------------------------------------------------------------------------------
GF = model("graphite_furnace", 4, 5, 8, "hipaint")
GF.box((0.0, 0.0, 0.0), (4.0, 0.3, 8.0), "concrete")
GF.box((0.4, 0.3, 1.5), (3.6, 3.2, 7.6), "iron")                                # the brick-lined furnace shell
GF.box((0.6, 3.2, 1.8), (3.4, 3.6, 7.3), "hipaint")                             # its roof
for z in (2.5, 4.5, 6.5):
	GF.box((1.6, 3.6, z), (2.4, 4.6, z + 0.3), "copper")                        # electrode bus
GF.box((0.4, 0.3, 0.2), (3.0, 2.6, 1.3), "padgreen")                            # the furnace transformer
GF.fins((0.5, 0.6, 0.0), (2.9, 2.3, 0.2), "x", 10, 0.02, "padgreen")
stub(GF, (4.0, 0.5, 0.5), (3.4, 0.5, 0.5), 2, "steel")                          # nitrogen in, the east of its main cell
box480(GF, 0.4, 0.2)
GF.collide((0.2, 0.0, 0.2), (3.8, 3.6, 7.8))

# ---- the LFP kiln ---------------------------------------------------------------------------------------------------------------
LK = model("lfp_kiln", 3, 4, 12, "steel")
LK.box((0.0, 0.0, 0.0), (3.0, 0.3, 12.0), "concrete")
for z in (0.8, 2.4):
	LK.cylinder("y", (1.5, z), 0.65, 0.3, 2.6, "steel", 14)                     # precipitation tanks
	LK.cylinder("y", (1.5, z), 0.1, 2.6, 3.4, "blue", 8)
LK.box((0.5, 0.3, 3.6), (2.5, 1.9, 11.7), "hipaint")                            # the roller-hearth kiln
LK.box((0.4, 1.9, 3.5), (2.6, 2.1, 11.8), "grey")
for z in range(4, 12, 2):
	LK.box((0.45, 0.6, z), (0.5, 1.6, z + 0.6), "grey")                         # heater panel boxes
stub(LK, (3.0, 0.5, 0.5), (2.2, 0.5, 0.5), 2, "steel")                          # acid in, east of its main cell
stub(LK, (3.0, 0.5, 11.5), (2.5, 0.5, 11.5), 2, "steel")                        # nitrogen in, east of its back cell
box480(LK, 0.4, 0.2)
LK.collide((0.2, 0.0, 0.2), (2.8, 2.1, 11.8))

# ---- the cell line --------------------------------------------------------------------------------------------------------------
CL = model("cell_line", 4, 5, 16, "grey")
CL.box((0.0, 0.0, 0.0), (4.0, 0.3, 16.0), "concrete")
CL.box((0.2, 0.3, 0.2), (3.8, 3.6, 15.8), "grey")                               # the dry-room building
CL.box((0.1, 3.6, 0.1), (3.9, 3.8, 15.9), "galv")                               # its roof
CL.box((0.8, 3.8, 2.0), (3.2, 4.8, 10.0), "hipaint")                            # the coater's drying oven
for z in (11.0, 12.6, 14.2):
	CL.box((1.0, 3.8, z), (2.0, 4.4, z + 1.0), "louvre")                        # dry-room air handlers
CL.box((1.2, 0.3, 15.8), (2.8, 2.4, 16.0), "steel")                             # the module door, south end
stub(CL, (4.0, 0.5, 0.5), (3.8, 0.5, 0.5), 2, "steel")                          # HF in
stub(CL, (4.0, 0.5, 1.5), (3.8, 0.5, 1.5), 2, "steel")                          # ethylene in
box480(CL, 0.4, 0.6)
CL.collide((0.2, 0.0, 0.2), (3.8, 3.8, 15.8))

M = GL
EXTRA = [GF, LK, CL]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
