"""Substation relay panel, an outdoor relay cabinet 2 x 2 x 1 (docs/multiblocks/substation.md), at true size: NEMA 3R cabinet
on its plinth north of the transformer, the relays behind the window in its door (the 50/51 overcurrent relays, the lockout and
the meters), the drip roof, and the control cables down into the trench."""
import yardlib as y

M = y.model("relay_panel", 2, 2, 1, "grey")

M.box((0.1, 0.0, 0.2), (1.9, 0.15, 0.9), "concrete")
M.box((0.15, 0.15, 0.25), (1.85, 1.85, 0.85), "grey")
M.box((0.1, 1.85, 0.2), (1.9, 1.9, 0.9), "grey")
M.box((0.2, 0.25, 0.23), (1.8, 1.78, 0.25), "grey")                 # the door
M.decal((0.3, 0.75, 0.225), (1.7, 1.65, 0.23), "relays", "north", "grey")
M.decal((0.3, 0.4, 0.225), (0.9, 0.65, 0.23), "danger", "north", "grey")
M.decal((1.1, 0.45, 0.225), (1.6, 0.6, 0.23), "nameplate", "north", "steel")
for y0 in (0.45, 1.55):
	M.box((1.82, y0, 0.22), (1.86, y0 + 0.12, 0.25), "galv")       # hinges
M.box((0.22, 1.0, 0.2), (0.26, 1.2, 0.23), "dark")                  # handle
M.pipe([(1.0, 0.15, 0.6), (1.0, 0.02 + 0.017, 0.6)], 1, "galv")

M.collide((0.1, 0, 0.2), (1.9, 1.9, 0.9))
