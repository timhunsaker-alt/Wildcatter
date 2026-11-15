"""75 kVA three-phase dry-type transformer, NEMA ventilated (docs/dry-type-transformer.md), at true size in its one cell: a grey
enclosure about 0.76 W x 1.0 H x 0.6 D m on two floor channels, louvred low on the front and sides (air in) and high (air out),
its drip-hood top, the nameplate and the danger label on the front. Facing north, the front is the 480 V side (the block's
high side: front and top), so a conduit hub stands on the top and another low on the back for the 208Y/120 V feed."""
import linelib as l

M = l.model("dry_type_transformer", [(0, 0, 0)], "grey")
x1, x2, z1, z2 = 0.12, 0.88, 0.2, 0.8
M.box((x1 + 0.02, 0.0, z1 + 0.04), (x1 + 0.08, 0.05, z2 - 0.04), "galv")       # the floor channels
M.box((x2 - 0.08, 0.0, z1 + 0.04), (x2 - 0.02, 0.05, z2 - 0.04), "galv")
M.box((x1, 0.05, z1), (x2, 0.95, z2), "grey")                                   # the enclosure
M.box((x1 - 0.02, 0.95, z1 - 0.03), (x2 + 0.02, 0.99, z2 + 0.03), "grey")       # the drip hood
M.box((x1 + 0.1, 0.99, z1 + 0.1), (x2 - 0.1, 1.0, z2 - 0.1), "grey")
# louvres: low on the front and both sides, high on the sides
M.decal((x1 + 0.08, 0.1, z1 - 0.004), (x2 - 0.08, 0.3, z1), "louvre", "north", "grey")
for x, face in ((x1, "west"), (x2, "east")):
	sign = -1 if face == "west" else 1
	a, b = sorted((x, x + sign * 0.004))
	M.decal((a, 0.1, z1 + 0.08), (b, 0.3, z2 - 0.08), "louvre", face, "grey")
	M.decal((a, 0.7, z1 + 0.08), (b, 0.88, z2 - 0.08), "louvre", face, "grey")
M.decal((x1 + 0.1, 0.62, z1 - 0.004), (x1 + 0.36, 0.86, z1), "danger", "north", "grey")
M.decal((x2 - 0.3, 0.66, z1 - 0.004), (x2 - 0.1, 0.8, z1), "nameplate", "north", "steel")
M.box((x2 - 0.12, 0.42, z1 - 0.02), (x2 - 0.08, 0.5, z1), "dark")               # the cover's quarter-turn latch
M.box((x1 + 0.08, 0.42, z1 - 0.02), (x1 + 0.12, 0.5, z1), "dark")
M.cylinder("y", (0.5, 0.42), 0.04, 1.0, 1.06, "galv", 10)                       # 480 V conduit hub on the top
M.cylinder("z", (0.5, 0.2), 0.045, z2, z2 + 0.06, "galv", 10)                   # 208Y/120 V conduit hub low on the back
M.box((x1 + 0.04, 0.06, z2), (x1 + 0.1, 0.1, z2 + 0.02), "copper")             # the ground lug
EXPORT = dict(blockstate=False)
