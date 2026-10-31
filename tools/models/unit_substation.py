"""A 1,000 kVA unit substation, 4.16 kV to 480Y/277 V, 5.75 %Z (docs/multiblocks/padmount.md): one shipped lineup 2.3 m high and
1.7 m deep on its base channels. From the main cell's end (west when facing north): the 4.16 kV fused load-break switch section
(0.9 m) with its viewing window and operating handle; the liquid-filled transformer (2.0 m) with tank-wall radiators front and
back, its gauges and the throats into both sections; the 480 V section (0.9 m) with the main breaker behind its door and the
meters."""
import linelib as l

M = l.model("unit_substation", l.mlib.footprint_box(4, 3, 2, 0, 0), "grey")
Z1, Z2 = 0.15, 1.85
H = 2.3
M.box((0.05, 0, Z1 + 0.05), (3.85, 0.1, Z2 - 0.05), "iron")                 # base channels
# the 4.16 kV switch section
S1, S2 = 0.05, 0.95
M.box((S1, 0.1, Z1), (S2, H, Z2), "grey")
M.box((S1 - 0.02, H, Z1 - 0.03), (S2 + 0.02, H + 0.04, Z2 + 0.02), "grey")
M.box((S1 + 0.05, 0.15, Z1 - 0.012), (S2 - 0.05, H - 0.08, Z1), "grey")       # door
M.box((S1 + 0.3, 1.3, Z1 - 0.016), (S2 - 0.3, 1.6, Z1 - 0.012), "dark")       # the viewing window
M.decal((S1 + 0.15, 1.75, Z1 - 0.016), (S2 - 0.15, 2.05, Z1 - 0.013), "danger", "north", "grey")
M.decal((S1 + 0.3, 0.9, Z1 - 0.016), (S2 - 0.3, 1.05, Z1 - 0.013), "nameplate", "north", "steel")
M.box((S1 - 0.04, 0.9, Z1 + 0.25), (S1, 1.1, Z1 + 0.35), "dark")               # the operating handle's hub on the end
l.strut(M, (S1 - 0.06, 1.0, Z1 + 0.3), (S1 - 0.06, 1.55, Z1 + 0.62), 0.04, 0.03, "dark", axis="x", pieces=1)
# the transformer
T1, T2 = 0.95, 2.95
M.box((T1 + 0.2, 0.1, 0.45), (T2 - 0.2, 1.95, 1.55), "grey")
M.box((T1 + 0.15, 1.95, 0.4), (T2 - 0.15, 2.0, 1.6), "grey")
for za, zb in ((0.17, 0.45), (1.55, 1.83)):
	M.fins((T1 + 0.3, 0.25, za), (T2 - 0.3, 1.8, zb), "x", 16, 0.02, "grey")
	M.box((T1 + 0.3, 1.75, za), (T2 - 0.3, 1.8, zb), "grey")                   # radiator headers
	M.box((T1 + 0.3, 0.25, za), (T2 - 0.3, 0.3, zb), "grey")
M.box((T1, 1.4, 0.7), (T1 + 0.2, 1.9, 1.3), "grey")                           # throats into the sections
M.box((T2 - 0.2, 1.4, 0.7), (T2, 1.9, 1.3), "grey")
for x in (1.5, 1.9, 2.3):
	M.decal((x, 1.3, 0.165), (x + 0.2, 1.5, 0.17), "gauge", "north", "grey")
M.decal((2.1, 0.9, 0.165), (2.45, 1.1, 0.17), "nameplate", "north", "steel")
M.box((1.25, 0.2, 0.42), (1.35, 0.3, 0.45), "brass")                          # drain valve
for x in (T1 + 0.3, T2 - 0.4):
	M.box((x, 2.0, 0.95), (x + 0.1, 2.12, 1.05), "grey")                        # lifting lugs
# the 480 V section
B1, B2 = 2.95, 3.85
M.box((B1, 0.1, Z1), (B2, H, Z2), "grey")
M.box((B1 - 0.02, H, Z1 - 0.03), (B2 + 0.02, H + 0.04, Z2 + 0.02), "grey")
M.box((B1 + 0.05, 0.15, Z1 - 0.012), (B2 - 0.05, 1.15, Z1), "grey")           # the lower door
M.box((B1 + 0.05, 1.2, Z1 - 0.012), (B2 - 0.05, H - 0.08, Z1), "grey")         # the breaker door
M.box((B1 + 0.2, 1.45, Z1 - 0.03), (B2 - 0.2, 1.95, Z1 - 0.012), "dark")       # the main breaker's face
M.box((B1 + 0.4, 1.6, Z1 - 0.05), (B2 - 0.4, 1.7, Z1 - 0.03), "rubber")        # its handle
for x in (B1 + 0.12, B1 + 0.5):
	M.decal((x, 0.75, Z1 - 0.016), (x + 0.28, 1.0, Z1 - 0.013), "gauge", "north", "grey")
M.decal((B1 + 0.15, 0.35, Z1 - 0.016), (B2 - 0.15, 0.6, Z1 - 0.013), "danger", "north", "grey")
for x in (S1 + 0.1, B2 - 0.2):
	M.box((x, 0.03, Z1 + 0.1), (x + 0.1, 0.1, Z1 + 0.2), "copper")             # ground pads
M.collide((0.05, 0, Z1), (3.85, H, Z2))
