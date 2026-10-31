"""The starter plant at true size (docs/multiblocks/building-gear.md), drawn facing north:
- the boiler, 1 x 2 x 1: a small vertical fire-tube boiler about 0.6 m across and 1.4 m tall on its base ring, its stack, the water
  gauge glass and pressure gauge on the front, the safety valve and steam outlet on top, the feed check valve and blowdown valve;
  lit, its fire door glows;
- the 2 kW turbine-generator, one cell: the single-stage turbine (back) and the generator (front) on one skid, the coupling guard
  between, the steam inlet on top, the governor, the terminal box;
- the 750 W close-coupled centrifugal pump, one cell: the motor (front) and the volute (back), suction out the back, discharge up;
- the coal firebox, one cell: cast iron, its fire door and ash-pit door on the front; lit, the door's draught slots glow."""
import gearlib as g
import mlib


def boiler(lit):
	M = mlib.Model(f"boiler{'_lit' if lit else ''}", mlib.footprint_box(1, 2, 1, 0, 0), dict(g.TEX), g.TEX["dark"])
	M.cylinder("y", (0.5, 0.5), 0.33, 0.0, 0.12, "iron", 16)                     # the base ring over the fire
	M.box((0.42, 0.02, 0.15), (0.58, 0.1, 0.17), "bulb" if lit else "dark")       # the fire door's slot
	M.cylinder("y", (0.5, 0.5), 0.3, 0.12, 1.3, "dark", 16)                       # the shell
	for y in (0.3, 0.7, 1.1):
		M.cylinder("y", (0.5, 0.5), 0.305, y, y + 0.02, "steel", 16)             # lagging bands
	M.cylinder("y", (0.5, 0.5), 0.26, 1.3, 1.36, "dark", 16)                      # the crown
	M.cylinder("y", (0.5, 0.5), 0.08, 1.36, 1.85, "dark", 10)                     # the stack
	M.cylinder("y", (0.5, 0.5), 0.1, 1.83, 1.87, "iron", 10)
	M.box((0.36, 0.55, 0.17), (0.4, 1.0, 0.21), "brass")                          # the water gauge glass and its cocks
	M.box((0.37, 0.6, 0.165), (0.39, 0.95, 0.172), "polymer")
	M.cylinder("z", (0.62, 1.1), 0.06, 0.17, 0.2, "brass", 12)                    # the pressure gauge
	M.decal((0.57, 1.05, 0.165), (0.67, 1.15, 0.17), "gauge", "north", "brass")
	M.cylinder("y", (0.35, 0.6), 0.03, 1.36, 1.5, "brass", 8)                     # the safety valve
	M.cylinder("y", (0.35, 0.6), 0.045, 1.46, 1.5, "brass", 8)
	M.pipe([(0.65, 1.33, 0.5), (0.65, 1.45, 0.5), (1.0, 1.45, 0.5)], 1, "steel")   # steam out toward the turbine's side
	M.pipe([(0.0, 0.5, 0.5), (0.2, 0.5, 0.5)], 0.75, "steel")                     # feedwater in, its check valve
	M.box((0.12, 0.46, 0.46), (0.2, 0.54, 0.54), "brass")
	M.pipe([(0.5, 0.18, 0.8), (0.5, 0.18, 0.95)], 0.75, "steel")                  # the blowdown
	M.box((0.46, 0.14, 0.9), (0.54, 0.22, 0.94), "brass")
	M.collide((0.2, 0.0, 0.2), (0.8, 1.36, 0.8))
	M.collide((0.42, 1.36, 0.42), (0.58, 1.87, 0.58))
	return M


def turbine(running):
	M = g.model(f"steam_turbine{'_running' if running else ''}", particle="iron")
	M.box((0.3, 0.0, 0.1), (0.7, 0.08, 0.9), "iron")                              # the skid
	M.cylinder("z", (0.5, 0.25), 0.15, 0.55, 0.85, "steel", 14)                   # the turbine casing
	M.box((0.38, 0.08, 0.6), (0.62, 0.13, 0.8), "iron")
	M.cylinder("z", (0.5, 0.25), 0.14, 0.12, 0.45, "blue", 14)                    # the generator
	M.fins((0.36, 0.13, 0.15), (0.64, 0.38, 0.42), "z", 7, 0.012, "blue")
	M.cylinder("z", (0.5, 0.25), 0.07, 0.45, 0.55, "galv", 10)                    # the coupling guard
	M.pipe([(0.5, 0.4, 0.75), (0.5, 0.6, 0.75), (0.5, 0.6, 1.0)], 1, "steel")     # steam in from behind
	M.box((0.45, 0.55, 0.7), (0.55, 0.66, 0.8), "brass")                          # the governor valve
	M.pipe([(0.65, 0.25, 0.7), (1.0, 0.25, 0.7)], 1.5, "steel")                   # the exhaust
	M.box((0.62, 0.3, 0.2), (0.7, 0.42, 0.34), "grey")                           # the terminal box
	M.decal((0.4, 0.02, 0.098), (0.6, 0.07, 0.1), "nameplate", "north", "steel")         # the nameplate on the skid
	M.box((0.47, 0.42, 0.25), (0.53, 0.45, 0.31), "bulb" if running else "dark")  # the run light
	M.collide((0.3, 0.0, 0.1), (0.7, 0.45, 0.9))
	return M


def pump():
	M = g.model("pump", particle="blue")
	M.box((0.3, 0.0, 0.15), (0.7, 0.05, 0.85), "iron")                            # base
	M.cylinder("z", (0.5, 0.2), 0.09, 0.15, 0.45, "blue", 12)                     # the motor
	M.fins((0.41, 0.11, 0.17), (0.59, 0.29, 0.43), "z", 6, 0.01, "blue")
	M.box((0.56, 0.24, 0.3), (0.64, 0.32, 0.4), "grey")                           # its terminal box
	M.cylinder("z", (0.5, 0.2), 0.05, 0.45, 0.52, "galv", 10)                     # the bracket
	M.cylinder("z", (0.5, 0.2), 0.13, 0.52, 0.62, "iron", 14)                     # the volute
	M.pipe([(0.5, 0.2, 0.62), (0.5, 0.2, 1.0)], 1.5, "steel")                     # suction out the back
	M.pipe([(0.5, 0.33, 0.57), (0.5, 1.0, 0.57)], 1, "steel")                     # discharge up
	M.collide((0.3, 0.0, 0.15), (0.7, 0.35, 0.85))
	return M


def firebox(lit):
	M = g.model(f"firebox{'_lit' if lit else ''}", particle="iron")
	M.box((0.1, 0.0, 0.1), (0.9, 0.85, 0.9), "iron")
	M.box((0.08, 0.85, 0.08), (0.92, 0.9, 0.92), "iron")                          # the top flange the boiler sits on
	M.box((0.3, 0.38, 0.08), (0.7, 0.72, 0.1), "iron")                            # the fire door
	for x in (0.36, 0.46, 0.56):
		M.box((x, 0.5, 0.075), (x + 0.06, 0.6, 0.08), "bulb" if lit else "dark")  # its draught slots
	M.box((0.3, 0.08, 0.08), (0.7, 0.25, 0.1), "iron")                            # the ash-pit door
	M.box((0.66, 0.53, 0.05), (0.69, 0.57, 0.08), "brass")                        # the latch
	M.collide((0.1, 0.0, 0.1), (0.9, 0.9, 0.9))
	return M


BOILERS = [boiler(False), boiler(True)]
BOILERS[1].export_args = {"blockstate": False, "item": False}
OTHERS = g.single([turbine(False), turbine(True)], "steam_turbine") + g.single([pump()], "pump") + g.single([firebox(False), firebox(True)], "firebox")
M = BOILERS[0]
EXTRA = BOILERS[1:] + OTHERS


def after():
	g.save_blockstate("boiler", {f"facing={facing},lit={lit},piece={piece}": {"model": f"wildcatter:block/boiler{'_lit' if lit == 'true' else ''}/p{piece}", **({"y": y} if y else {})}
		for facing, y in g.ROT.items() for piece in range(2) for lit in ("false", "true")})
	r = lambda y: {"y": y} if y else {}
	g.save_blockstate("steam_turbine", {f"facing={facing},running={run}": {"model": f"wildcatter:block/steam_turbine{'_running' if run == 'true' else ''}/p0", **r(y)}
		for facing, y in g.ROT.items() for run in ("false", "true")})
	g.save_blockstate("pump", {f"facing={facing}": {"model": "wildcatter:block/pump/p0", **r(y)} for facing, y in g.ROT.items()})
	g.save_blockstate("firebox", {f"facing={facing},lit={lit}": {"model": f"wildcatter:block/firebox{'_lit' if lit == 'true' else ''}/p0", **r(y)}
		for facing, y in g.ROT.items() for lit in ("false", "true")})
