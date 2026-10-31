"""A 30 ft class 5 wood pole (docs/multiblocks/poles.md): set 5 ft deep it stands 7.6 m, drawn 8 m to its top cell plus the
0.3 m above it; top 0.15 m and ground line 0.23 m across (ANSI O5.1 class 5: 19 in and about 28 in round)."""
import linelib as l

M = l.model("wood_pole_30", l.mlib.footprint_box(1, 8, 1, 0, 0))
l.wood_pole(M, 0.5, 0.5, 8, 0.154, 0.23)
