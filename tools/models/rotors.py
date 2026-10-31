"""The genset's turning parts at true size (docs/kinetics-and-sound.md), each drawn centred on its block with its axis along x.
KineticRenderer turns them about the pivots in GensetPartBlockEntity.rotors().

- radiator_fan: 1.2 m pusher fan, six blades (genset radiators run 1.1 to 1.3 m fans)
- crank_pulley: the crank pulley with its two belt grooves and a timing mark, 0.3 m across
- coupling: the flexible coupling and flywheel ring seen through the adapter's windows, 0.8 m across
- alternator_fan: the alternator's drive-end cooling fan, 0.8 m across
- tower_fan: the cooling tower's 3 m fan, six blades, turning about y (4.5c)
- turbine_coupling: the steam turbine's flexible disc coupling to its generator, 0.45 m across
- pump_coupling: the feed pump's jaw coupling, 0.14 m across
- disconnect_arm_hv, disconnect_arm_hv_back: a 138 kV centre-break disconnect's arms, 0.7 m from their stacks to the middle,
  pointing east and west (4.5d); they turn about y on their stacks
- disconnect_blade_dist: a 12.47 kV vertical-break disconnect's blade, 0.7 m from its hinge, lifting about z
"""
import mlib
import plantlib as g

C = (0.5, 0.5)   # the block's centre, (y, z)


def rotor(name):
	return mlib.Model("rotor_" + name, [(0, 0, 0)], dict(g.TEX), g.TEX["iron"])


FAN = rotor("radiator_fan")
FAN.fan("x", C, 0.58, 0.5, 0.025, "dark", blades=6, width=0.17, hub=0.09, hub_tex="galv")
FAN.cylinder("x", C, 0.04, 0.38, 0.62, "steel", 8)   # the fan shaft

PULLEY = rotor("crank_pulley")
PULLEY.cylinder("x", C, 0.15, 0.45, 0.55, "iron", 16)
for x in (0.47, 0.51):
	PULLEY.ring("x", C, 0.152, x, x + 0.015, 0.01, "dark", sides=16)
PULLEY.cylinder("x", C, 0.06, 0.42, 0.45, "steel", 10)   # the damper's centre bolt
PULLEY.box((0.43, 0.6, 0.48), (0.46, 0.64, 0.52), "brass")   # a timing mark, so it shows turning

COUPLING = rotor("coupling")
COUPLING.cylinder("x", C, 0.4, 0.46, 0.54, "iron", 16)   # flywheel ring
COUPLING.cylinder("x", C, 0.3, 0.42, 0.58, "rubber", 16)  # the flexible element
COUPLING.bolt_circle("x", C, 0.34, 0.54, 12, "galv", head=0.04, height=0.025)
COUPLING.box((0.4, 0.86, 0.45), (0.6, 0.9, 0.55), "brass")   # a painted mark on the ring

ALT_FAN = rotor("alternator_fan")
ALT_FAN.fan("x", C, 0.4, 0.5, 0.06, "galv", blades=8, width=0.1, hub=0.16, hub_tex="blue")
ALT_FAN.ring("x", C, 0.4, 0.47, 0.53, 0.02, "galv", sides=16)

TOWER_FAN = rotor("tower_fan")
TOWER_FAN.fan("y", (0.5, 0.5), 1.45, 0.5, 0.04, "polymer", blades=6, width=0.3, hub=0.22, hub_tex="galv")
TOWER_FAN.cylinder("y", (0.5, 0.5), 0.12, 0.5 + 0.06, 0.5 + 0.16, "galv", 10)   # the hub's cap

TURBINE_COUPLING = rotor("turbine_coupling")
TURBINE_COUPLING.cylinder("x", C, 0.22, 0.44, 0.48, "steel", 16)   # the two hubs' flanges
TURBINE_COUPLING.cylinder("x", C, 0.22, 0.52, 0.56, "steel", 16)
TURBINE_COUPLING.cylinder("x", C, 0.17, 0.48, 0.52, "galv", 16)    # the disc pack
TURBINE_COUPLING.bolt_circle("x", C, 0.18, 0.56, 8, "galv", head=0.03, height=0.02)
TURBINE_COUPLING.box((0.43, 0.68, 0.47), (0.57, 0.72, 0.53), "brass")   # a painted mark

PUMP_COUPLING = rotor("pump_coupling")
PUMP_COUPLING.cylinder("x", C, 0.07, 0.42, 0.49, "iron", 12)
PUMP_COUPLING.cylinder("x", C, 0.07, 0.51, 0.58, "iron", 12)
PUMP_COUPLING.fan("x", C, 0.065, 0.5, 0.04, "rubber", blades=4, width=0.03, hub=0.02)   # the spider between the jaws
PUMP_COUPLING.box((0.44, 0.565, 0.485), (0.56, 0.575, 0.515), "brass")

def arm(name, sign):
	"""An arm from its hub at the block's centre, 0.7 m toward the middle (east for +1, west for -1), its contact at the end."""
	M = rotor(name)
	M.cylinder("y", (0.5, 0.5), 0.07, 0.42, 0.5, "galv", 10)   # the hub on the rotating stack
	a, b = (0.5, 1.2) if sign > 0 else (-0.2, 0.5)
	M.cylinder("x", (0.52, 0.5), 0.035, a, b, "steel", 8)
	end = 1.2 if sign > 0 else -0.2
	M.box((end - 0.06 if sign > 0 else end, 0.47, 0.44), (end if sign > 0 else end + 0.06, 0.58, 0.56), "copper")   # the contact
	return M


ARM = arm("disconnect_arm_hv", 1)
ARM_BACK = arm("disconnect_arm_hv_back", -1)
BLADE = rotor("disconnect_blade_dist")
BLADE.box((0.47, 0.47, 0.47), (0.55, 0.55, 0.53), "galv")   # the hinge
BLADE.box((0.5, 0.49, 0.48), (1.2, 0.53, 0.52), "copper")    # the blade
BLADE.box((1.1, 0.53, 0.47), (1.2, 0.6, 0.53), "galv")       # its pull ring

ROTORS = [FAN, PULLEY, COUPLING, ALT_FAN, TOWER_FAN, TURBINE_COUPLING, PUMP_COUPLING, ARM, ARM_BACK, BLADE]
