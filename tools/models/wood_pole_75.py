"""A 75 ft class 2 wood pole (docs/multiblocks/poles.md): set 9.5 ft deep it stands 20 m, drawn 20 m to its top cell plus the
0.3 m above it; top 0.20 m and ground line 0.38 m across (ANSI O5.1 class 2: 25 in and about 47 in round). Two make a 138 kV
H-frame."""
import linelib as l

M = l.model("wood_pole_75", l.mlib.footprint_box(1, 20, 1, 0, 0))
l.wood_pole(M, 0.5, 0.5, 20, 0.20, 0.38)
