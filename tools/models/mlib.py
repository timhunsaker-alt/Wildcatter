"""The model tool for true-scale parts (docs/multiblocks/README.md, "Big parts").

A part is drawn in metres, facing north, with its main cell at [0, 1] on every axis (x east, y up, z south; the part's
front is toward -z). Shapes: box, cylinder, flange, bolt_circle, pipe, fins, ladder, handrail, grating, plate. export()
cuts the drawing into the part's cells and writes, for the mod:
  models/block/<part>/p<k>.json   one block model per piece (boxes may reach one block past their cell: -16..32)
  blockstates/<part>.json         every facing x piece
  models/block/<part>/item.json   the whole part shrunk to fit, for the item
  shapes/<part>.json              each piece's collision boxes, in sixteenths, facing north (BigParts reads it)
It refuses a box outside -16..32 of its cell, a box outside the footprint, and a face with no texture.
preview() draws the part from three sides to a PNG, so a model can be checked without starting the game.
"""
import json, math, os

NS = "wildcatter"
ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets", NS)
FACES = ("north", "south", "west", "east", "up", "down")
# NPS pipe outside diameters in metres (ASME B36.10)
NPS_OD = {0.5: 0.0213, 0.75: 0.0267, 1: 0.0334, 1.5: 0.0483, 2: 0.0603, 3: 0.0889, 4: 0.1143, 6: 0.1683, 8: 0.2191, 10: 0.2730, 12: 0.3239}


class ModelError(Exception):
	pass


def footprint_box(width, height, depth, main_x, main_z):
	"""The cells of Footprint.box, in the same order: the main cell first, then bottom layer up, north to south, west to east."""
	cells = [(0, 0, 0)]
	for y in range(height):
		for z in range(depth):
			for x in range(width):
				if (y, x, z) != (0, main_x, main_z):
					cells.append((x - main_x, y, z - main_z))
	return cells


class Element:
	def __init__(self, frm, to, tex, faces=None, rot=None, uv_full=(), shade=True):
		self.frm = list(frm)
		self.to = list(to)
		# tex: one texture name for every face, or {face: name}
		self.tex = tex if isinstance(tex, dict) else {f: tex for f in (faces or FACES)}
		if faces is not None:
			self.tex = {f: t for f, t in self.tex.items() if f in faces}
		self.rot = rot          # (axis, degrees, origin in metres) or None
		self.uv_full = set(uv_full)   # faces that show their whole texture (decals)
		self.shade = shade

	def corners(self):
		"""The eight corners in metres, after the rotation."""
		pts = []
		for x in (self.frm[0], self.to[0]):
			for y in (self.frm[1], self.to[1]):
				for z in (self.frm[2], self.to[2]):
					pts.append(rotate((x, y, z), self.rot))
		return pts


def rotate(p, rot):
	if not rot:
		return p
	axis, deg, o = rot
	a = math.radians(deg)
	c, s = math.cos(a), math.sin(a)
	x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
	if axis == "x":
		y, z = y * c - z * s, y * s + z * c
	elif axis == "y":
		# Minecraft turns about y the other way round from the right-hand rule
		x, z = x * c + z * s, -x * s + z * c
	else:
		x, y = x * c - y * s, x * s + y * c
	return (x + o[0], y + o[1], z + o[2])


class Model:
	def __init__(self, name, cells, textures, particle):
		"""cells: the footprint (footprint_box); textures: {short name: texture path under textures/}."""
		self.name = name
		self.cells = cells
		self.index = {c: i for i, c in enumerate(cells)}
		self.textures = textures
		self.particle = particle
		self.elements = []
		self.collision = []
		self.rotors = {}

	# ---- shapes, in metres ---------------------------------------------------------------------------------------------
	def box(self, frm, to, tex, faces=None, uv_full=(), rot=None):
		frm, to = [min(a, b) for a, b in zip(frm, to)], [max(a, b) for a, b in zip(frm, to)]
		if any(b - a <= 0 for a, b in zip(frm, to)):
			return None
		e = Element(frm, to, tex, faces, rot, uv_full)
		self.elements.append(e)
		return e

	def cylinder(self, axis, centre, radius, start, end, tex, sides=12, caps=True, cap_tex=None):
		"""A many-sided round part: sides/2 slabs turned about the axis. centre is the other two coordinates (for y: x, z)."""
		if sides % 2:
			raise ModelError("a cylinder needs an even number of sides")
		w = 2 * radius * math.tan(math.pi / sides)
		for k in range(sides // 2):
			deg = k * 360.0 / sides
			eps = 0.0015 * k   # the caps overlap; pull each in a little so they do not fight
			if axis == "y":
				cx, cz = centre
				frm, to = (cx - w / 2, start + eps, cz - radius), (cx + w / 2, end - eps, cz + radius)
				radial, capf, o = ("north", "south"), ("up", "down"), (cx, (start + end) / 2, cz)
			elif axis == "x":
				cy, cz = centre
				frm, to = (start + eps, cy - w / 2, cz - radius), (end - eps, cy + w / 2, cz + radius)
				radial, capf, o = ("north", "south"), ("west", "east"), ((start + end) / 2, cy, cz)
			else:
				cx, cy = centre
				frm, to = (cx - w / 2, cy - radius, start + eps), (cx + w / 2, cy + radius, end - eps)
				radial, capf, o = ("up", "down"), ("north", "south"), (cx, cy, (start + end) / 2)
			faces = {f: tex for f in radial}
			if caps:
				faces.update({f: cap_tex or tex for f in capf})
			self._slab(frm, to, faces, axis, deg, o)

	def _slab(self, frm, to, faces, axis, deg, origin):
		"""A slab turned deg about axis; kept within -45..45 by swapping its two cross dimensions."""
		deg = deg % 180.0
		if deg > 45.0 and deg <= 135.0:
			deg -= 90.0
			frm, to, faces = _swap_cross(frm, to, faces, axis, origin)
		elif deg > 135.0:
			deg -= 180.0
		e = Element(frm, to, faces, None, (axis, deg, origin) if abs(deg) > 1e-6 else None)
		self.elements.append(e)

	def ring(self, axis, centre, radius, start, end, thickness, tex, sides=12, skip=()):
		"""A hoop of sides plates round the axis (a fan shroud, a guard band, a cooling rib). skip leaves gaps (window k)."""
		i = "xyz".index(axis)
		first = [j for j in range(3) if j != i][0]   # the plates' radial direction before they are turned
		o = [0.0, 0.0, 0.0]
		o[i] = (start + end) / 2
		cross = [j for j in range(3) if j != i]
		o[cross[0]], o[cross[1]] = centre
		tangent = 2 * radius * math.tan(math.pi / sides) + 0.004
		for k in range(sides):
			if k in skip:
				continue
			deg = k * 360.0 / sides
			c0 = list(o)
			c0[first] += radius
			c = rotate(c0, (axis, deg, o))
			half = [0.0, 0.0, 0.0]
			half[i] = (end - start) / 2
			half[first] = thickness / 2
			half[cross[1]] = tangent / 2
			frm = [c[j] - half[j] for j in range(3)]
			to = [c[j] + half[j] for j in range(3)]
			self._slab(frm, to, {f: tex for f in FACES}, axis, deg, tuple(c))

	def fan(self, axis, centre, radius, at, thickness, tex, blades=6, width=0.16, hub=0.08, hub_tex=None):
		"""Blades through the hub in pairs (each pair one slab through the centre), so any count of even blades turns cleanly."""
		for k in range(blades // 2):
			deg = k * 360.0 / blades
			if axis == "x":
				cy, cz = centre
				frm, to = (at - thickness / 2, cy - width / 2, cz - radius), (at + thickness / 2, cy + width / 2, cz + radius)
				o = (at, cy, cz)
			elif axis == "y":
				cx, cz = centre
				frm, to = (cx - width / 2, at - thickness / 2, cz - radius), (cx + width / 2, at + thickness / 2, cz + radius)
				o = (cx, at, cz)
			else:
				cx, cy = centre
				frm, to = (cx - width / 2, cy - radius, at - thickness / 2), (cx + width / 2, cy + radius, at + thickness / 2)
				o = (cx, cy, at)
			self._slab(frm, to, {f: tex for f in FACES}, axis, deg, o)
		self.cylinder(axis, centre, hub, at - thickness * 1.5, at + thickness * 1.5, hub_tex or tex, 8)

	def flange(self, axis, centre, radius, at, thickness, tex, bolts=8, bolt_tex=None, sides=16):
		self.cylinder(axis, centre, radius, at - thickness / 2, at + thickness / 2, tex, sides)
		self.bolt_circle(axis, centre, radius * 0.82, at + thickness / 2, bolts, bolt_tex or tex)

	def bolt_circle(self, axis, centre, radius, face_at, count, tex, head=0.03, height=0.02):
		for k in range(count):
			a = 2 * math.pi * (k + 0.5) / count
			u, v = centre[0] + radius * math.cos(a), centre[1] + radius * math.sin(a)
			h = head / 2
			if axis == "y":
				self.box((u - h, face_at, v - h), (u + h, face_at + height, v + h), tex)
			elif axis == "x":
				self.box((face_at, u - h, v - h), (face_at + height, u + h, v + h), tex)
			else:
				self.box((u - h, v - h, face_at), (u + h, v + h, face_at + height), tex)

	def pipe(self, points, nps, tex, sides=8):
		"""A pipe of real outside diameter through straight runs along the axes."""
		r = NPS_OD[nps] / 2
		for a, b in zip(points, points[1:]):
			d = [i for i in range(3) if abs(a[i] - b[i]) > 1e-9]
			if len(d) != 1:
				raise ModelError("pipe runs must follow an axis")
			i = d[0]
			axis = "xyz"[i]
			lo, hi = min(a[i], b[i]) - r, max(a[i], b[i]) + r
			centre = tuple(a[j] for j in range(3) if j != i)
			self.cylinder(axis, centre, r, lo, hi, tex, sides)

	def fins(self, frm, to, along, count, thickness, tex):
		"""count parallel plates filling the box frm..to, spaced along axis `along` (cooling fins, radiator panels)."""
		i = "xyz".index(along)
		span = to[i] - frm[i]
		for k in range(count):
			at = frm[i] + (k + 0.5) * span / count
			a, b = list(frm), list(to)
			a[i], b[i] = at - thickness / 2, at + thickness / 2
			self.box(a, b, tex)

	def plate(self, frm, to, tex, faces=None):
		return self.box(frm, to, tex, faces)

	def decal(self, frm, to, tex, face, back):
		"""A thin plate showing its whole picture on one face (nameplate, sign, gauge); the other faces are `back`."""
		return self.box(frm, to, {f: tex if f == face else back for f in FACES}, uv_full=(face,))

	def ladder(self, x, z, y1, y2, facing_z, tex, width=0.45):
		"""A ladder on a north or south wall, its rails at x +- width/2, standing off the wall at z."""
		for side in (-1, 1):
			self.box((x + side * width / 2 - 0.025, y1, z), (x + side * width / 2 + 0.025, y2, z + facing_z * 0.05), tex)
		y = y1 + 0.15
		while y < y2:
			self.box((x - width / 2, y - 0.012, z + facing_z * 0.01), (x + width / 2, y + 0.012, z + facing_z * 0.04), tex)
			y += 0.3

	def handrail(self, points, y, tex, height=1.07):
		"""OSHA 1910.29: a top rail at about 1.07 m with a mid rail and posts."""
		for a, b in zip(points, points[1:]):
			for h in (height, height / 2):
				lo = [min(a[0], b[0]) - 0.02, y + h - 0.02, min(a[1], b[1]) - 0.02]
				hi = [max(a[0], b[0]) + 0.02, y + h + 0.02, max(a[1], b[1]) + 0.02]
				self.box(lo, hi, tex)
		for p in points:
			self.box((p[0] - 0.025, y, p[1] - 0.025), (p[0] + 0.025, y + height, p[1] + 0.025), tex)

	def collide(self, frm, to):
		"""A collision box in metres (kept coarse: walls, tanks, decks)."""
		self.collision.append(([min(a, b) for a, b in zip(frm, to)], [max(a, b) for a, b in zip(frm, to)]))

	# ---- export ----------------------------------------------------------------------------------------------------------
	def _cell_of(self, p):
		return tuple(math.floor(v + 1e-9) for v in p)

	def _split(self, e):
		"""Cuts a box at cell boundaries (a turned one only along its turning axis), so each piece is lit by its own cell."""
		axes = [0, 1, 2] if not e.rot else ["xyz".index(e.rot[0])]
		parts = [e]
		for i in axes:
			out = []
			for p in parts:
				cuts = [c for c in range(math.floor(p.frm[i]) + 1, math.ceil(p.to[i])) if p.frm[i] < c < p.to[i]]
				edges = [p.frm[i]] + cuts + [p.to[i]]
				for a, b in zip(edges, edges[1:]):
					q = Element(p.frm, p.to, {}, None, p.rot, p.uv_full, p.shade)
					q.frm[i], q.to[i] = a, b
					q.tex = {f: t for f, t in p.tex.items() if not _inner_face(f, i, a, b, p)}
					out.append(q)
			parts = out
		return parts

	def export(self):
		for e in self.elements:
			for f, t in e.tex.items():
				if t not in self.textures:
					raise ModelError(f"{self.name}: face {f} uses unknown texture {t!r}")
		pieces = {i: [] for i in range(len(self.cells))}
		for e in self.elements:
			for part in self._split(e):
				if not part.tex:
					continue
				centre = [(a + b) / 2 for a, b in zip(part.frm, part.to)]
				if part.rot:
					centre = list(rotate(centre, part.rot))
				cell = self._cell_of(centre)
				if cell not in self.index:
					raise ModelError(f"{self.name}: a box centred at {centre} is outside the footprint")
				for p in part.corners():
					for v, c in zip(p, cell):
						if (v - c) * 16 < -16 - 1e-6 or (v - c) * 16 > 32 + 1e-6:
							raise ModelError(f"{self.name}: a box reaches {p}, more than a block past its cell {cell}")
				pieces[self.index[cell]].append(self._json(part, cell))
		out = os.path.join(ASSETS, "models/block", self.name)
		os.makedirs(out, exist_ok=True)
		textures = {k: f"{NS}:{v}" for k, v in self.textures.items()}
		textures["particle"] = f"{NS}:{self.particle}"
		for i, elements in pieces.items():
			_save(os.path.join(out, f"p{i}.json"), {"textures": textures, "elements": elements})
		variants = {}
		for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
			for i in range(len(self.cells)):
				v = {"model": f"{NS}:block/{self.name}/p{i}"}
				if y:
					v["y"] = y
				variants[f"facing={facing},piece={i}"] = v
		_save(os.path.join(ASSETS, "blockstates", f"{self.name}.json"), {"variants": variants})
		self._export_item(textures)
		self._export_shapes()
		for rotor, model in self.rotors.items():
			model.export_single(rotor)
		count = sum(len(v) for v in pieces.values())
		print(f"{self.name}: {count} boxes in {len(self.cells)} pieces")
		return count

	def _json(self, e, cell):
		frm = [round((v - c) * 16, 4) for v, c in zip(e.frm, cell)]
		to = [round((v - c) * 16, 4) for v, c in zip(e.to, cell)]
		j = {"from": frm, "to": to, "faces": {}}
		if e.rot:
			axis, deg, o = e.rot
			j["rotation"] = {"origin": [round((v - c) * 16, 4) for v, c in zip(o, cell)], "axis": axis, "angle": round(deg, 4)}
		for f, t in e.tex.items():
			j["faces"][f] = {"texture": f"#{t}", "uv": [0, 0, 16, 16] if f in e.uv_full else _uv(f, frm, to)}
		if not e.shade:
			j["shade"] = False
		return j

	def _export_item(self, textures):
		"""The whole part, shrunk to fit a model's -16..32 room, with the display scaled so it reads as a block-sized icon."""
		lo = [min(min(p[i] for p in e.corners()) for e in self.elements) for i in range(3)]
		hi = [max(max(p[i] for p in e.corners()) for e in self.elements) for i in range(3)]
		size = max(h - l for l, h in zip(lo, hi)) * 16
		s = min(1.0, 46.0 / size)
		mid = [(l + h) / 2 for l, h in zip(lo, hi)]
		def tr(p):
			return [round((v - m) * 16 * s + 8, 4) for v, m in zip(p, mid)]
		elements = []
		for e in self.elements:
			if not e.tex:
				continue
			frm, to = tr(e.frm), tr(e.to)
			j = {"from": frm, "to": to, "faces": {}}
			if e.rot:
				j["rotation"] = {"origin": tr(e.rot[2]), "axis": e.rot[0], "angle": round(e.rot[1], 4)}
			for f, t in e.tex.items():
				j["faces"][f] = {"texture": f"#{t}", "uv": [0, 0, 16, 16] if f in e.uv_full else _uv(f, [v % 16 for v in frm], [v % 16 + (b - a) for v, a, b in zip(frm, frm, to)])}
			elements.append(j)
		k = 16.0 / (size * s)
		display = {
			"gui": {"rotation": [30, 225, 0], "scale": [0.625 * k] * 3},
			"ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25 * k] * 3},
			"fixed": {"rotation": [0, 0, 0], "scale": [0.5 * k] * 3},
			"thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375 * k] * 3},
			"firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4 * k] * 3},
			"firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4 * k] * 3},
		}
		_save(os.path.join(ASSETS, "models/block", self.name, "item.json"), {"textures": textures, "elements": elements, "display": display})
		_save(os.path.join(ASSETS, "items", f"{self.name}.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:block/{self.name}/item"}})

	def _export_shapes(self):
		pieces = [[] for _ in self.cells]
		for frm, to in self.collision:
			for cell, i in self.index.items():
				a = [max(f, c) for f, c in zip(frm, cell)]
				b = [min(t, c + 1) for t, c in zip(to, cell)]
				if all(y - x > 1e-6 for x, y in zip(a, b)):
					pieces[i].append([round((v - c) * 16, 3) for v, c in zip(a, cell)] + [round((v - c) * 16, 3) for v, c in zip(b, cell)])
		os.makedirs(os.path.join(ASSETS, "shapes"), exist_ok=True)
		_save(os.path.join(ASSETS, "shapes", f"{self.name}.json"), {"pieces": pieces})

	def export_single(self, name):
		"""A rotor: one block model, drawn centred on its block (KineticRenderer turns it about its pivot)."""
		elements = []
		for e in self.elements:
			for part in [e]:
				for p in part.corners():
					if min(p) * 16 < -16 - 1e-6 or max(p) * 16 > 32 + 1e-6:
						raise ModelError(f"rotor {name}: a box reaches {p}, more than a block past its cell")
				elements.append(self._json(part, (0, 0, 0)))
		textures = {k: f"{NS}:{v}" for k, v in self.textures.items()}
		textures["particle"] = f"{NS}:{self.particle}"
		_save(os.path.join(ASSETS, "models/block", f"rotor_{name}.json"), {"textures": textures, "elements": elements})

	# ---- preview -------------------------------------------------------------------------------------------------------------
	def preview(self, path, colours, size=900):
		"""Three views (front-left, back-right, top) drawn flat-shaded with each texture's average colour."""
		from PIL import Image, ImageDraw
		img = Image.new("RGB", (size * 3, size), (40, 44, 52))
		views = [(35, -30), (215, -30), (35, -75)]
		for vi, (yaw, pitch) in enumerate(views):
			polys = []
			for e in self.elements:
				for f, t in e.tex.items():
					quad = [rotate(p, e.rot) for p in _face_quad(e, f)]
					normal = _turn_vector(NORMALS[f], e.rot)
					polys.append((quad, colours.get(t, (200, 0, 200)), normal))
			lo = [min(min(p[i] for p in e.corners()) for e in self.elements) for i in range(3)]
			hi = [max(max(p[i] for p in e.corners()) for e in self.elements) for i in range(3)]
			mid = [(a + b) / 2 for a, b in zip(lo, hi)]
			scale = size * 0.75 / max(1e-6, max(b - a for a, b in zip(lo, hi)) * 1.5)
			draw = ImageDraw.Draw(img)
			items = []
			light = _norm((0.4, 1.0, -0.6))
			for quad, colour, normal in polys:
				vq = [_view([p[i] - mid[i] for i in range(3)], yaw, pitch) for p in quad]
				vn = _view(normal, yaw, pitch)
				if vn[2] > 0:   # facing away from the camera
					continue
				depth = sum(p[2] for p in vq) / 4
				k = 0.45 + 0.55 * max(0.0, sum(a * b for a, b in zip(normal, light)))
				items.append((depth, [(vi * size + size / 2 + p[0] * scale, size / 2 - p[1] * scale) for p in vq], tuple(int(c * k) for c in colour)))
			for _, pts, col in sorted(items, key=lambda t: -t[0]):
				draw.polygon(pts, fill=col, outline=tuple(max(0, c - 25) for c in col))
		img.save(path)


def _swap_cross(frm, to, faces, axis, o):
	"""The same slab turned a quarter more: its two cross dimensions swap about the origin, and so do its radial faces."""
	i = "xyz".index(axis)
	a, b = [j for j in range(3) if j != i]
	frm, to = list(frm), list(to)
	ha, hb = (to[a] - frm[a]) / 2, (to[b] - frm[b]) / 2
	frm[a], to[a] = o[a] - hb, o[a] + hb
	frm[b], to[b] = o[b] - ha, o[b] + ha
	names = {0: ("west", "east"), 1: ("down", "up"), 2: ("north", "south")}
	fa, fb = names[a], names[b]
	swap = {fa[0]: fb[0], fa[1]: fb[1], fb[0]: fa[0], fb[1]: fa[1]}
	return frm, to, {swap.get(f, f): t for f, t in faces.items()}


def _inner_face(face, i, a, b, p):
	"""After a cut along axis i, the cut faces are inside the original box: drop them."""
	lo_face, hi_face = {0: ("west", "east"), 1: ("down", "up"), 2: ("north", "south")}[i]
	if face == lo_face and a > p.frm[i] + 1e-9:
		return True
	if face == hi_face and b < p.to[i] - 1e-9:
		return True
	return False


def _uv(face, frm, to):
	"""Vanilla's own UV for a face, brought into 0..16 (a face wider than a block shows the texture once, stretched)."""
	x1, y1, z1 = frm
	x2, y2, z2 = to
	u, v = {
		"north": ((16 - x2, 16 - x1), (16 - y2, 16 - y1)),
		"south": ((x1, x2), (16 - y2, 16 - y1)),
		"west": ((z1, z2), (16 - y2, 16 - y1)),
		"east": ((16 - z2, 16 - z1), (16 - y2, 16 - y1)),
		"up": ((x1, x2), (z1, z2)),
		"down": ((x1, x2), (16 - z2, 16 - z1)),
	}[face]
	return [round(c, 4) for c in (_fit(u)[0], _fit(v)[0], _fit(u)[1], _fit(v)[1])]


def _fit(r):
	a, b = r
	span = b - a
	if span >= 16:
		return (0.0, 16.0)
	k = math.floor(a / 16)
	a, b = a - 16 * k, b - 16 * k
	if b > 16:
		a, b = 16 - span, 16.0
	return (a, b)


def _face_quad(e, f):
	(x1, y1, z1), (x2, y2, z2) = e.frm, e.to
	return {
		"north": [(x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1)],
		"south": [(x1, y1, z2), (x1, y2, z2), (x2, y2, z2), (x2, y1, z2)],
		"west": [(x1, y1, z1), (x1, y2, z1), (x1, y2, z2), (x1, y1, z2)],
		"east": [(x2, y1, z1), (x2, y1, z2), (x2, y2, z2), (x2, y2, z1)],
		"up": [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)],
		"down": [(x1, y1, z1), (x1, y1, z2), (x2, y1, z2), (x2, y1, z1)],
	}[f]


NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def _turn_vector(n, rot):
	if not rot:
		return n
	return rotate(n, (rot[0], rot[1], (0, 0, 0)))


def _norm(n):
	l = math.sqrt(sum(c * c for c in n)) or 1.0
	return tuple(c / l for c in n)


def _view(p, yaw, pitch):
	"""Camera space: x right, y up, z into the screen."""
	a, b = math.radians(yaw), math.radians(pitch)
	x, y, z = p
	x, z = x * math.cos(a) - z * math.sin(a), x * math.sin(a) + z * math.cos(a)
	y, z = y * math.cos(b) - z * math.sin(b), y * math.sin(b) + z * math.cos(b)
	return (x, y, z)


def _save(path, obj):
	os.makedirs(os.path.dirname(path), exist_ok=True)
	with open(path, "w") as f:
		json.dump(obj, f, indent=1)
