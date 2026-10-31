"""A 40 ft class 4 wood pole (docs/multiblocks/poles.md): set 6 ft deep it stands 10.4 m, drawn 10 m to its top cell plus the
0.3 m above it; top 0.17 m and ground line 0.27 m across (ANSI O5.1 class 4: 21 in and about 33 in round)."""
import linelib as l

M = l.model("wood_pole_40", l.mlib.footprint_box(1, 10, 1, 0, 0))
l.wood_pole(M, 0.5, 0.5, 10, 0.17, 0.27)
