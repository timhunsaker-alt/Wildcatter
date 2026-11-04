"""The generator bus's protection (docs/multiblocks/gen-bus-protection.md), at true size, each drawn facing north from its main cell,
the north-west bottom corner:
- gen_feeder_breaker, 3 x 3 x 1 (3 along the line, west to east): a 15 kV metal-clad cubicle, the draw-out vacuum breaker behind
  its lower door, the relay and control compartment above it, the bus compartment's bolted rear covers, the bus duct entering low
  at each end;
- bus_relay_panel, 1 x 3 x 1: a free-standing relay panel, its relays behind the swing frame's window, the 86B lockout handle,
  the trip and close lamps, the meters, on a channel base."""
import yardlib as y

# ---- the 15 kV feeder breaker cubicle -----------------------------------------------------------------------------------------------
M = y.model("gen_feeder_breaker", 3, 3, 1, "grey")
M.box((0.0, 0.0, 0.05), (3.0, 0.1, 0.95), "dark")                       # the channel base
M.box((0.05, 0.1, 0.08), (2.95, 2.4, 0.92), "grey")                      # the cubicle, 2.4 m tall
M.box((0.03, 2.4, 0.06), (2.97, 2.47, 0.94), "grey")                     # its roof
for x0, x1 in ((0.1, 1.45), (1.55, 2.9)):
	M.box((x0, 0.15, 0.06), (x1, 1.35, 0.08), "grey")                     # the breaker compartment's doors
	M.box((x0, 1.45, 0.06), (x1, 2.35, 0.08), "grey")                     # the relay and control doors
	M.box((x1 - 0.12, 0.6, 0.04), (x1 - 0.06, 0.9, 0.06), "dark")          # handles
	M.box((x1 - 0.12, 1.75, 0.04), (x1 - 0.06, 1.95, 0.06), "dark")
M.box((0.35, 0.4, 0.055), (1.2, 1.1, 0.06), "dark")                       # the viewing window onto the breaker
M.box((0.55, 0.55, 0.05), (1.0, 0.95, 0.055), "padgreen")                # the open/closed flag and the charging indicator
M.decal((1.75, 1.6, 0.055), (2.7, 2.25, 0.06), "relays", "north", "grey")
M.box((0.4, 2.0, 0.05), (0.55, 2.15, 0.06), "padgreen")                   # the open (green) and closed (red) lamps
M.box((0.7, 2.0, 0.05), (0.85, 2.15, 0.06), "danger")
M.decal((0.35, 1.6, 0.055), (1.25, 1.85, 0.06), "nameplate", "north", "steel")
M.decal((1.75, 0.5, 0.055), (2.7, 1.0, 0.06), "danger", "north", "grey")
for x in (0.0, 2.92):                                                    # the bus duct's flanges at each end
	M.box((x, 0.2, 0.15), (x + 0.08, 0.85, 0.85), "steel")
M.collide((0.0, 0.0, 0.05), (3.0, 2.47, 0.95))

# ---- the bus relay panel --------------------------------------------------------------------------------------------------------------
P = y.model("bus_relay_panel", 1, 3, 1, "grey")
P.box((0.05, 0.0, 0.15), (0.95, 0.1, 0.85), "dark")                       # the channel base
P.box((0.1, 0.1, 0.2), (0.9, 2.3, 0.8), "grey")                          # the panel, 0.8 m wide and 2.3 m tall
P.box((0.08, 2.3, 0.18), (0.92, 2.36, 0.82), "grey")
P.box((0.14, 0.2, 0.18), (0.86, 2.2, 0.2), "grey")                        # the swing frame
P.decal((0.18, 1.2, 0.175), (0.82, 2.1, 0.18), "relays", "north", "grey")
P.decal((0.2, 0.85, 0.175), (0.8, 1.1, 0.18), "nameplate", "north", "steel")
P.box((0.25, 0.55, 0.15), (0.45, 0.75, 0.18), "danger")                   # the 86B lockout handle
P.box((0.55, 0.62, 0.16), (0.63, 0.7, 0.18), "danger")                    # the trip lamp
P.box((0.68, 0.62, 0.16), (0.76, 0.7, 0.18), "padgreen")                  # the healthy lamp
P.decal((0.2, 0.25, 0.175), (0.8, 0.45, 0.18), "danger", "north", "grey")
P.collide((0.05, 0.0, 0.15), (0.95, 2.36, 0.85))

EXTRA = [P]
P.export_args = {"blockstate": True, "item": True}
