"""The boron plant at true size (docs/multiblocks/boron.md), each part drawn facing north from its own main cell (the footprints match
BoronPartBlock's):
- borax_dissolver, 3 x 4 x 3: the heated, agitated tank, its agitator drive on top and the acid nozzle east;
- boric_acid_crystallizer, 3 x 6 x 3: the cooling crystallizer, the pusher centrifuge and the dryer;
- b4c_furnace, 3 x 4 x 6: the Acheson-type furnace bed between its graphite electrodes, and its furnace transformer;
- rcca_bench, 3 x 3 x 8: the bench with a spider and its 24 rodlets, the loading glovebox."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


def box480(M, y=0.5, z=0.3):
	M.box((0.0, y, z), (0.3, y + 0.6, z + 0.5), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.5, z + 0.4), "danger", "west", "grey")


DI = model("borax_dissolver", 3, 4, 3, "steel")
DI.box((0.0, 0.0, 0.0), (3.0, 0.2, 3.0), "concrete")
DI.cylinder("y", (1.5, 1.5), 1.2, 0.2, 3.2, "steel", 16)                    # the tank and its steam jacket
DI.cylinder("y", (1.5, 1.5), 0.3, 3.2, 3.8, "blue", 10)                     # the agitator drive
DI.pipe([(3.0, 0.5, 1.5), (2.6, 0.5, 1.5)], 2, "steel")                     # sulfuric acid in, the east
box480(DI, 0.5, 0.3)
DI.collide((0.3, 0.0, 0.3), (2.7, 3.8, 2.7))

CR = model("boric_acid_crystallizer", 3, 6, 3, "steel")
CR.box((0.0, 0.0, 0.0), (3.0, 0.2, 3.0), "concrete")
CR.cylinder("y", (1.1, 1.1), 0.25, 0.2, 1.4, "steel", 10)                   # the cone and its outlet
CR.cylinder("y", (1.1, 1.1), 0.85, 1.4, 5.6, "steel", 14)                   # the cooling crystallizer
CR.box((1.9, 0.2, 1.8), (2.8, 1.3, 2.8), "blue")                            # the pusher centrifuge
CR.cylinder("z", (2.35, 1.9), 0.35, 0.3, 1.7, "galv", 10)                   # the dryer
box480(CR, 0.5, 2.2)
CR.collide((0.25, 0.0, 0.25), (2.8, 5.6, 2.8))

FU = model("b4c_furnace", 3, 4, 6, "concrete")
FU.box((0.3, 0.0, 0.5), (2.7, 1.4, 5.5), "concrete")                        # the furnace bed
FU.box((0.6, 1.4, 0.8), (2.4, 1.7, 5.2), "dark")                            # the charge, banked over the core
for z in (0.25, 5.75):
	FU.cylinder("z", (1.5, 0.9), 0.25, z - 0.25, z + 0.25, "dark", 10)       # a graphite electrode
FU.box((0.3, 0.0, 0.0), (1.3, 1.6, 0.5), "grey")                            # the furnace transformer
FU.box((0.5, 1.6, 0.1), (1.1, 2.0, 0.4), "copper")                          # its bus bars
box480(FU, 0.3, 0.05)
FU.collide((0.3, 0.0, 0.0), (2.7, 2.0, 6.0))

RB = model("rcca_bench", 3, 3, 8, "grey")
RB.box((0.3, 0.0, 0.2), (2.7, 0.9, 7.8), "iron")                            # the bench
RB.box((1.3, 0.9, 0.4), (1.7, 1.3, 0.6), "steel")                           # the spider
for i in range(6):
	RB.cylinder("z", (0.95 + i * 0.22, 1.05), 0.05, 0.6, 7.4, "steel", 6)    # rodlets
RB.box((0.5, 0.9, 6.0), (2.5, 2.2, 7.6), "polymer")                          # the loading glovebox
box480(RB, 0.3, 2.8)
RB.collide((0.3, 0.0, 0.2), (2.7, 2.2, 7.8))

M = DI
EXTRA = [CR, FU, RB]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
