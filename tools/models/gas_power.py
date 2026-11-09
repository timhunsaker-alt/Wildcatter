"""Phase 14b-14c parts at true size, each drawn facing north from its own main cell (the footprints match SalesGasPartBlock's and
AcidPlantPartBlock's), all in the mod's existing materials:
- meter_building, 3 x 3 x 4: the prefabricated metering shelter on its skid, the chromatograph's carrier gas bottles, the RTU's solar
  panel and antenna, its residential service box on the west;
- filter_separator, 2 x 4 x 2: the vertical filter separator, its quick-opening closure on top, the liquid sump and its level glass;
- ultrasonic_meter_run, 1 x 2 x 8: the flow conditioner, the 4-path ultrasonic meter body with its transducer housings, the straight
  runs, the thermowell and the check meter port, on pipe supports;
- regulator_skid, 2 x 2 x 5: the indirect-fired line heater and two runs of worker and monitor regulators with relief valves;
  pipeline gas out of the south of its back cells;
- pipeline_tap, 1 x 2 x 1: the hot-tap tee going down into the ground, the mainline block valve and its gas-over-oil operator;
- acid_superheater, 3 x 6 x 3: the brick-lined superheater shell between the converter's beds, its tube bundle headers and the steam
  outlet on top; saturated steam in on the west of its bottom."""
import mlib
import plantlib as g

TEX = dict(g.TEX)


def model(name, w, h, d, particle="grey"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(TEX), TEX[particle])


# ---- the meter building ----------------------------------------------------------------------------------------------------------
MB = model("meter_building", 3, 3, 4, "grey")
MB.box((0.0, 0.0, 0.0), (3.0, 0.15, 4.0), "iron")                           # its skid
MB.box((0.15, 0.15, 0.2), (2.85, 2.5, 3.8), "grey")                         # the shelter
MB.box((0.05, 2.5, 0.1), (2.95, 2.65, 3.9), "galv")                         # the roof
MB.box((1.1, 0.15, 3.8), (1.9, 2.2, 3.82), "galv")                          # the door, south
MB.decal((2.85, 1.2, 1.0), (2.86, 1.8, 1.8), "flammable", "east", "grey")
MB.decal((2.85, 0.6, 2.4), (2.86, 1.0, 3.0), "nameplate", "east", "grey")
for z in (0.45, 0.75):
	MB.cylinder("y", (2.95, z), 0.11, 0.15, 1.5, "steel", 10)                  # the chromatograph's carrier gas bottles
MB.box((0.4, 2.65, 0.4), (1.6, 2.75, 1.6), "dark")                          # the RTU's solar panel
MB.cylinder("y", (2.4, 3.4), 0.03, 2.65, 2.98, "galv", 6)                  # its radio antenna
MB.box((0.0, 0.5, 0.3), (0.15, 1.1, 0.8), "grey")                          # the residential service box, west
MB.decal((-0.01, 0.6, 0.35), (0.0, 1.0, 0.75), "danger", "west", "grey")
MB.collide((0.0, 0.0, 0.0), (3.0, 2.65, 4.0))

# ---- the filter separator --------------------------------------------------------------------------------------------------------
FS = model("filter_separator", 2, 4, 2, "steel")
FS.box((0.1, 0.0, 0.1), (1.9, 0.2, 1.9), "concrete")
FS.cylinder("y", (1.0, 1.0), 0.55, 0.5, 3.4, "grey", 16)                   # the vessel
FS.cylinder("y", (1.0, 1.0), 0.6, 3.4, 3.6, "steel", 16)                   # its quick-opening closure
FS.cylinder("y", (1.0, 1.0), 0.3, 0.2, 0.5, "grey", 12)                    # the sump boot
for x in (0.55, 1.45):
	FS.box((x - 0.05, 0.2, 0.45), (x + 0.05, 0.5, 0.55), "iron")            # its legs
FS.box((1.5, 0.6, 0.95), (1.58, 1.4, 1.05), "glass" if "glass" in TEX else "steel")   # the level glass
FS.pipe([(1.0, 0.45, 0.0), (1.0, 0.45, 0.45)], 4, "steel")                 # gas in, the north
FS.pipe([(0.0, 2.8, 1.0), (0.45, 2.8, 1.0)], 4, "steel")                   # gas out to the meter run, the west
FS.decal((0.44, 2.0, 0.8), (0.45, 2.6, 1.2), "nameplate", "west", "grey")
FS.collide((0.4, 0.0, 0.4), (1.6, 3.6, 1.6))

# ---- the ultrasonic meter run ----------------------------------------------------------------------------------------------------
MR = model("ultrasonic_meter_run", 1, 2, 8, "steel")
for z in (0.5, 3.0, 5.5, 7.5):
	MR.box((0.3, 0.0, z - 0.1), (0.7, 0.6, z + 0.1), "iron")                # pipe supports
MR.pipe([(0.5, 0.75, 0.0), (0.5, 0.75, 8.0)], 4, "steel")                  # the straight runs
MR.flange("z", (0.5, 0.75), 0.17, 1.2, 0.04, "steel")                      # the flow conditioner's flanges
MR.flange("z", (0.5, 0.75), 0.17, 1.3, 0.04, "steel")
MR.box((0.25, 0.5, 3.4), (0.75, 1.0, 4.4), "blue")                          # the 4-path meter body
for z in (3.55, 4.25):
	for x in (0.18, 0.82):
		MR.cylinder("x", (0.88, z), 0.05, x - 0.05, x + 0.05, "steel", 8)      # its transducer housings
MR.box((0.35, 1.0, 3.6), (0.65, 1.3, 4.2), "grey")                          # the meter's electronics head
MR.cylinder("y", (0.5, 5.6), 0.03, 0.85, 1.1, "brass", 6)                  # the thermowell
MR.cylinder("y", (0.5, 6.4), 0.05, 0.85, 1.05, "steel", 6)                 # the check meter port
MR.collide((0.3, 0.0, 0.0), (0.7, 1.3, 8.0))

# ---- the regulator skid -------------------------------------------------------------------------------------------------------------
RS = model("regulator_skid", 2, 2, 5, "grey")
RS.box((0.0, 0.0, 0.0), (2.0, 0.15, 5.0), "iron")
RS.cylinder("z", (0.5, 0.75), 0.4, 0.2, 2.2, "grey", 14)                   # the indirect-fired line heater's shell
RS.cylinder("y", (0.5, 0.35), 0.08, 1.1, 1.95, "steel", 8)                 # its stack
for x in (1.2, 1.7):
	RS.pipe([(x, 0.5, 0.0), (x, 0.5, 5.0)], 2, "steel")                    # the two runs
	RS.box((x - 0.13, 0.5, 2.6), (x + 0.13, 0.9, 2.9), "blue")              # the monitor regulator
	RS.cylinder("y", (x, 2.75), 0.15, 0.9, 1.05, "blue", 10)
	RS.box((x - 0.13, 0.5, 3.4), (x + 0.13, 0.9, 3.7), "blue")              # the worker regulator
	RS.cylinder("y", (x, 3.55), 0.15, 0.9, 1.05, "blue", 10)
	RS.cylinder("y", (x, 4.3), 0.05, 0.5, 1.3, "steel", 6)                  # its relief valve
RS.pipe([(0.5, 0.5, 4.5), (0.5, 0.5, 5.0)], 4, "steel")                    # pipeline gas out, the south
RS.decal((0.09, 0.5, 1.0), (0.1, 0.9, 1.6), "flammable", "west", "grey")
RS.collide((0.0, 0.0, 0.0), (2.0, 1.2, 5.0))

# ---- the pipeline tap ---------------------------------------------------------------------------------------------------------------
PT = model("pipeline_tap", 1, 2, 1, "steel")
PT.box((0.0, 0.0, 0.0), (1.0, 0.1, 1.0), "concrete")
PT.cylinder("y", (0.5, 0.5), 0.2, 0.0, 0.6, "padgreen", 12)                # the hot-tap tee going down to the line
PT.box((0.25, 0.6, 0.25), (0.75, 1.0, 0.75), "padgreen")                    # the block valve
PT.cylinder("y", (0.5, 0.5), 0.07, 1.0, 1.2, "steel", 8)                   # its stem
PT.box((0.2, 1.2, 0.3), (0.8, 1.5, 0.7), "blue")                            # the gas-over-oil operator
PT.cylinder("x", (1.35, 0.5), 0.1, 0.1, 0.9, "steel", 10)                  # its oil tanks
PT.pipe([(0.0, 0.8, 0.5), (0.25, 0.8, 0.5)], 4, "steel")                   # to the meter run, the west
PT.collide((0.2, 0.0, 0.2), (0.8, 1.5, 0.8))

# ---- the acid plant's superheater --------------------------------------------------------------------------------------------------
SH = model("acid_superheater", 3, 6, 3, "hipaint")
SH.box((0.0, 0.0, 0.0), (3.0, 0.3, 3.0), "concrete")
SH.box((0.3, 0.3, 0.3), (2.7, 5.2, 2.7), "hipaint")                         # the lined shell
for y in (1.4, 2.6, 3.8):
	SH.box((0.25, y, 0.25), (2.75, y + 0.12, 2.75), "iron")                 # its stiffener bands
SH.box((0.6, 0.3, 0.0), (2.4, 4.8, 0.3), "steel")                           # the gas duct against the converter, north
SH.cylinder("z", (1.5, 4.9), 0.18, 0.4, 2.6, "lagging", 12)                # the outlet header
SH.cylinder("y", (1.5, 1.5), 0.15, 5.2, 6.0, "lagging", 10)                # main steam out, the top
SH.cylinder("z", (0.45, 0.6), 0.15, 0.4, 2.6, "lagging", 10)                # the inlet header
SH.pipe([(0.0, 0.6, 1.5), (0.3, 0.6, 1.5)], 4, "lagging")                  # saturated steam in, the west
SH.ladder(2.85, 1.5, 0.3, 5.2, 1, "galv")
SH.collide((0.3, 0.0, 0.3), (2.7, 5.2, 2.7))


M = MB
EXTRA = [FS, MR, RS, PT, SH]
for extra in EXTRA:
	extra.export_args = {"blockstate": True, "item": True}
