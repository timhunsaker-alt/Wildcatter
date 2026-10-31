"""Cooling tower fill with its casing, 4 x 4 x 2 (docs/multiblocks/cooling-tower.md), at true size.

The lower metre is the air inlet: louvres on the north and south faces, with the PVC film fill visible behind them; above,
the closed FRP casing over the rest of the fill pack. Corner posts, the ladder and the hot-water riser carry on up.
"""
import towerlib as t

M = t.model("cooling_fill", 2, "polymer")

M.box((0.2, 0.05, 0.2), (3.8, 1.95, 3.8), "dark")   # the fill pack's core
M.fins((0.2, 0.05, 0.16), (3.8, 1.0, 0.2), "x", 24, 0.012, "polymer")   # film sheets seen through the louvres
M.fins((0.2, 0.05, 3.8), (3.8, 1.0, 3.84), "x", 24, 0.012, "polymer")
t.louvres(M, t.IN, 0.0, 1.0, -1)
t.louvres(M, 4 - t.IN, 0.0, 1.0, 1)
t.casing(M, 0.0, 1.0, open_sides=("north", "south"))
t.casing(M, 1.0, 2.0)
t.posts(M, 0.0, 2.0)
t.ladder(M, 0.0, 2.0)
t.riser(M, 0.0, 2.0)

M.collide((0.06, 0, 0.06), (3.94, 2.0, 3.94))
