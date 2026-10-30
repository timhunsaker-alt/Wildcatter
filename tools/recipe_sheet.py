"""Render every Wildcatter recipe as a crafting-grid picture: python tools/recipe_sheet.py out_dir"""
import glob, io, json, os, sys, zipfile
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAR = glob.glob(f"{ROOT}/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/*/*.jar")[0]
zf = zipfile.ZipFile(JAR)
MOD = f"{ROOT}/src/main/resources/assets/wildcatter/textures"
S = 48
FONT = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 15)
SMALL = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 12)

def tex(item_id):
	ns, name = item_id.split(":")
	if ns == "wildcatter" and name == "crossarm":
		name = "pole"
	cands = []
	if ns == "wildcatter":
		for d in ("item", "block"):
			cands += [f"{MOD}/{d}/{name}.png", f"{MOD}/{d}/{name}_side.png", f"{MOD}/{d}/{name}_front.png", f"{MOD}/{d}/{name}_top.png"]
		for d in ("item", "block"):
			cands += sorted(glob.glob(f"{MOD}/{d}/{name}*.png"))
		for c in cands:
			if os.path.exists(c):
				im = Image.open(c).convert("RGBA")
				return im.crop((0, 0, im.width, im.width))
	else:
		for d in ("item", "block"):
			for suf in ("", "_side", "_front", "_top"):
				p = f"assets/minecraft/textures/{d}/{name}{suf}.png"
				if p in zf.namelist():
					im = Image.open(io.BytesIO(zf.read(p))).convert("RGBA")
					return im.crop((0, 0, im.width, im.width))
	return None

def icon(item_id, count=1):
	im = Image.new("RGBA", (S, S), (139, 139, 139, 255))
	if item_id.startswith("#"):
		item_id = {"#minecraft:wool": "minecraft:white_wool", "#minecraft:logs": "minecraft:oak_log", "#minecraft:planks": "minecraft:oak_planks"}.get(item_id, "minecraft:barrier")
		tag = True
	else:
		tag = False
	t = tex(item_id)
	d = ImageDraw.Draw(im)
	if t:
		im.alpha_composite(t.resize((S - 8, S - 8), Image.NEAREST), (4, 4))
	else:
		d.text((3, S // 2 - 6), item_id.split(":")[1][:7], font=SMALL, fill=(0, 0, 0))
	if tag:
		d.text((S - 12, 0), "*", font=FONT, fill=(255, 255, 0))
	if count > 1:
		d.text((S - 16, S - 18), str(count), font=FONT, fill=(255, 255, 255), stroke_width=1, stroke_fill=(0, 0, 0))
	return im

def render(path):
	r = json.load(open(path))
	name = os.path.basename(path)[:-5].replace("_", " ").upper()
	if r["type"].endswith("shaped"):
		cells = [[None] * 3 for _ in range(3)]
		for y, row in enumerate(r["pattern"]):
			for x, ch in enumerate(row):
				if ch != " ":
					cells[y][x] = r["key"][ch]
		ing = cells
	else:
		flat = (r["ingredients"] + [None] * 9)[:9]
		ing = [flat[0:3], flat[3:6], flat[6:9]]
	W, H = 3 * S + 2 * 6 + 60 + S + 40, 3 * S + 56
	im = Image.new("RGBA", (W, H), (198, 198, 198, 255))
	d = ImageDraw.Draw(im)
	d.text((8, 6), name + ("" if r["type"].endswith("shaped") else "  (shapeless)"), font=FONT, fill=(30, 30, 30))
	for y in range(3):
		for x in range(3):
			ox, oy = 8 + x * (S + 2), 32 + y * (S + 2)
			d.rectangle([ox - 1, oy - 1, ox + S, oy + S], fill=(139, 139, 139, 255), outline=(85, 85, 85, 255))
			if ing[y][x]:
				im.alpha_composite(icon(ing[y][x]), (ox, oy))
	ax = 8 + 3 * (S + 2) + 10
	d.polygon([(ax, 32 + 70), (ax + 30, 32 + 70), (ax + 30, 32 + 58), (ax + 52, 32 + 76), (ax + 30, 32 + 94), (ax + 30, 32 + 82), (ax, 32 + 82)], fill=(90, 90, 90))
	im.alpha_composite(icon(r["result"]["id"], r["result"].get("count", 1)), (ax + 62, 32 + 52))
	# key legend
	return im

def main(out):
	os.makedirs(out, exist_ok=True)
	files = sorted(glob.glob(f"{ROOT}/src/main/resources/data/wildcatter/recipe/*.json"))
	ims = [render(f) for f in files]
	w, h = ims[0].size
	cols = 3
	rows = (len(ims) + cols - 1) // cols
	per = 5
	pages = []
	for p in range(0, len(ims), cols * per):
		chunk = ims[p:p + cols * per]
		r = (len(chunk) + cols - 1) // cols
		sheet = Image.new("RGBA", (cols * (w + 8) + 8, r * (h + 8) + 8), (60, 60, 60, 255))
		for i, im in enumerate(chunk):
			sheet.alpha_composite(im, (8 + (i % cols) * (w + 8), 8 + (i // cols) * (h + 8)))
		fn = f"{out}/recipes_{p // (cols * per) + 1}.png"
		sheet.convert("RGB").save(fn)
		pages.append(fn)
	print("\n".join(pages))

main(sys.argv[1])
