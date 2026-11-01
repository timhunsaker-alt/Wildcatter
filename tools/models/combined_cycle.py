"""The combined cycle at true size (docs/multiblocks/combined-cycle.md), each part drawn facing north from its own main cell (the
footprints match TurbinePartBlock's):
- hrsg, 5 x 12 x 12: the inlet duct from the gas turbine (north), the casing over the superheater, evaporator and economizer
  bundles with its access doors and stiffeners, the steam drum across the top with its downcomers, safety valves and gauge glass,
  the stack at the back (south) with its platform and ladder;
- steam_turbine_5, 3 x 3 x 6: the 5 MW turbine casing, its steam chest and trip valve, the gearbox and the exhaust down to the
  condenser;
- st_generator, 3 x 3 x 4: the 6.25 MVA generator, its cooler and the terminal box at the back;
- air_cooled_condenser, 6 x 8 x 6: the steel frame, the A-frame finned bundles, four fans on top (they turn) and their fan
  shrouds, the steam duct, the condensate tank under it;
- hrsg_feed_pump, 2 x 2 x 4: the 60 hp motor and the multistage pump on a baseplate (the coupling turns)."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


# ---- the HRSG -----------------------------------------------------------------------------------------------------------------
HR = model("hrsg", 5, 12, 12)
HR.box((0.0, 0.0, 0.0), (5.0, 0.3, 12.0), "concrete")
HR.box((0.6, 0.3, 0.0), (4.4, 3.4, 2.5), "lagging")                        # the inlet duct from the turbine, widening
HR.box((0.2, 0.3, 2.5), (4.8, 9.0, 9.5), "grey")                           # the casing over the bundles
for z in (3.5, 5.0, 6.5, 8.0, 9.4):
	HR.box((0.15, 0.3, z - 0.06), (0.2, 9.0, z + 0.06), "iron")            # its stiffeners
	HR.box((4.8, 0.3, z - 0.06), (4.85, 9.0, z + 0.06), "iron")
for z in (4.0, 6.0, 8.0):
	HR.box((4.85, 1.0, z), (4.9, 2.4, z + 0.8), "dark")                    # the access doors
HR.cylinder("z", (2.5, 9.9), 0.7, 3.0, 9.0, "padgreen", 16)                # the steam drum across the top
for z in (3.5, 8.5):
	HR.cylinder("y", (1.2, z), 0.12, 9.0, 9.9, "steel", 8)                 # downcomers and risers
	HR.cylinder("y", (3.8, z), 0.12, 9.0, 9.9, "steel", 8)
HR.cylinder("y", (2.0, 5.0), 0.08, 10.5, 11.2, "brass", 8)                 # the safety valves
HR.cylinder("y", (3.0, 5.0), 0.08, 10.5, 11.2, "brass", 8)
HR.box((1.75, 9.6, 2.7), (1.85, 10.4, 2.8), "steel")                       # the gauge glass
HR.pipe([(2.5, 9.9, 9.0), (2.5, 9.9, 9.6), (2.5, 11.4, 9.6)], 8, "steel")  # the main steam line over to the steam turbine
HR.box((1.2, 0.3, 9.5), (3.8, 3.0, 12.0), "grey")                          # the stack's base
HR.cylinder("y", (2.5, 10.8), 1.0, 3.0, 11.9, "grey", 16)                  # the stack
HR.box((1.0, 7.0, 9.5), (4.0, 7.06, 12.0), "checker")
HR.ladder(1.2, 9.48, 0.3, 7.0, -1, "galv")
HR.collide((0.6, 0.0, 0.0), (4.4, 3.4, 2.5))
HR.collide((0.2, 0.0, 2.5), (4.8, 9.0, 9.5))
HR.collide((1.2, 0.0, 9.5), (3.8, 3.0, 12.0))

# ---- the steam turbine and its generator -------------------------------------------------------------------------------------------
STT = model("steam_turbine_5", 3, 3, 6, "grey")
STT.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "iron")
STT.box((0.4, 0.3, 0.3), (2.6, 1.0, 4.4), "iron")                          # the pedestal
STT.cylinder("z", (1.5, 1.6), 0.8, 0.5, 3.6, "lagging", 16)               # the turbine casing, lagged
STT.box((1.1, 2.3, 0.6), (1.9, 2.9, 1.4), "steel")                         # the steam chest
STT.cylinder("y", (1.5, 0.5), 0.15, 2.0, 2.95, "steel", 10)               # the trip and throttle valve
STT.ring("y", (1.5, 0.5), 0.25, 2.95, 2.98, 0.02, "rubber", sides=12)
STT.box((0.8, 0.3, 3.6), (2.2, 1.0, 4.2), "lagging")                      # the exhaust down to the condenser
STT.box((0.6, 0.3, 4.4), (2.4, 1.6, 5.6), "engine")                       # the gearbox
STT.collide((0.0, 0.0, 0.0), (3.0, 2.4, 6.0))

SG = model("st_generator", 3, 3, 4, "blue")
SG.box((0.0, 0.0, 0.0), (3.0, 0.3, 4.0), "iron")
SG.box((0.4, 0.3, 0.3), (2.6, 2.2, 3.3), "blue")
SG.fins((0.45, 0.5, 0.5), (2.55, 2.0, 3.1), "z", 10, 0.03, "blue")
SG.box((0.6, 2.2, 0.6), (2.4, 2.8, 2.9), "grey")                          # its cooler
SG.box((0.5, 0.3, 3.3), (2.5, 2.0, 4.0), "grey")                          # the terminal box, back
SG.collide((0.0, 0.0, 0.0), (3.0, 2.8, 4.0))

# ---- the air-cooled condenser -------------------------------------------------------------------------------------------------------
AC = model("air_cooled_condenser", 6, 8, 6, "galv")
for x in (0.2, 2.9, 5.6):
	for z in (0.2, 2.9, 5.6):
		AC.box((x, 0.0, z), (x + 0.2, 5.0, z + 0.2), "galv")               # the frame's columns
AC.box((0.0, 5.0, 0.0), (6.0, 5.2, 6.0), "checker")                        # the fan deck
for k in range(4):                                                          # the A-frame bundles, stepped
	y = 5.4 + k * 0.5
	AC.box((0.3 + k * 0.6, y, 0.2), (0.9 + k * 0.6, y + 0.5, 5.8), "steel")
	AC.box((5.1 - k * 0.6, y, 0.2), (5.7 - k * 0.6, y + 0.5, 5.8), "steel")
AC.cylinder("z", (3.0, 7.6), 0.35, 0.1, 5.9, "lagging", 12)               # the steam header along the ridge
for x in (1.5, 4.5):
	for z in (1.5, 4.5):
		AC.ring("y", (x, z), 1.0, 5.2, 5.6, 0.04, "grey", sides=16)         # the fan shrouds under the bundles
AC.cylinder("z", (3.0, 1.2), 0.6, 0.6, 5.4, "padgreen", 14)                # the condensate tank under it
AC.box((0.0, 0.3, 0.2), (0.4, 1.0, 0.8), "grey")                          # its 480 V box, west face of the main cell
AC.collide((0.0, 5.0, 0.0), (6.0, 7.5, 6.0))
AC.collide((2.4, 0.0, 0.6), (3.6, 1.8, 5.4))

# ---- the feed pump ------------------------------------------------------------------------------------------------------------------
FP = model("hrsg_feed_pump", 2, 2, 4, "blue")
FP.box((0.1, 0.0, 0.1), (1.9, 0.25, 3.9), "iron")
FP.cylinder("z", (1.0, 0.7), 0.35, 0.2, 1.5, "blue", 14)                  # the 60 hp motor
FP.box((0.05, 0.3, 0.3), (0.4, 0.9, 0.8), "grey")                         # its 480 V box, west
FP.ring("z", (1.0, 0.7), 0.18, 1.5, 1.9, 0.02, "engine", sides=12, skip=(1, 2, 4, 5, 7, 8, 10, 11))
FP.cylinder("z", (1.0, 0.7), 0.22, 1.9, 3.7, "padgreen", 12)              # the multistage pump
for z in (2.3, 2.8, 3.3):
	FP.ring("z", (1.0, 0.7), 0.24, z - 0.03, z + 0.03, 0.03, "iron", sides=12)
FP.pipe([(1.0, 0.92, 3.4), (1.0, 1.9, 3.4)], 3, "steel")
FP.collide((0.1, 0.0, 0.1), (1.9, 1.1, 3.9))

M = HR
EXTRA = [STT, SG, AC, FP]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
