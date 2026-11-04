package dev.wildcatter.blueprint;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A plant written down (docs/blueprints.md): its blocks and its true-size parts in a box, facing north from the box's north-west
 * bottom corner (x east, y up, z south), with the lift list the cranes build it from. The block states are strings; turning them is
 * the Minecraft side's job. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Blueprint {
	/** A true-size part: its id, its facing (quarter turns clockwise from north), its main cell and the box of all its cells. */
	public record Part(String id, int turns, int x, int y, int z, int x0, int y0, int z0, int x1, int y1, int z1) {
		public int width() {
			return x1 - x0 + 1;
		}

		public int height() {
			return y1 - y0 + 1;
		}

		public int length() {
			return z1 - z0 + 1;
		}
	}

	/** An ordinary block: where, and its palette entry. */
	public record Block(int x, int y, int z, int state) {
	}

	/** One lift: a part (its index in {@link #parts()}), or a pallet of ordinary blocks (their indices in {@link #blocks()}). */
	public record Lift(int part, List<Integer> blocks, double tonnes, int y) {
		public boolean isPart() {
			return part >= 0;
		}
	}

	public static final int PALLET_BLOCKS = 64;
	public static final int PALLET_AREA = 5;   // m: a pallet's blocks lie in one 5 x 5 m area of one layer

	private final String name;
	private final int sx, sy, sz;
	private final List<String> palette;
	private final List<Block> blocks;
	private final List<Part> parts;
	private final Map<String, String> sizes;   // "x,y,z" -> a conductor size's name

	public Blueprint(String name, int sx, int sy, int sz, List<String> palette, List<Block> blocks, List<Part> parts, Map<String, String> sizes) {
		this.name = name;
		this.sx = sx;
		this.sy = sy;
		this.sz = sz;
		this.palette = List.copyOf(palette);
		this.blocks = List.copyOf(blocks);
		this.parts = List.copyOf(parts);
		this.sizes = Map.copyOf(sizes);
	}

	public String name() {
		return name;
	}

	public int sizeX() {
		return sx;
	}

	public int sizeY() {
		return sy;
	}

	public int sizeZ() {
		return sz;
	}

	public List<String> palette() {
		return palette;
	}

	public List<Block> blocks() {
		return blocks;
	}

	public List<Part> parts() {
		return parts;
	}

	/** The conductor size of the wire or cable at x, y, z, or null. */
	public String size(int x, int y, int z) {
		return sizes.get(x + "," + y + "," + z);
	}

	public Map<String, String> sizes() {
		return sizes;
	}

	// ---- turning -----------------------------------------------------------------------------------------------------------
	/** The same plant turned {@code turns} quarter turns clockwise, its origin again at its north-west bottom corner. */
	public Blueprint turned(int turns) {
		int t = Math.floorMod(turns, 4);
		if (t == 0) {
			return this;
		}
		Blueprint b = this;
		for (int i = 0; i < t; i++) {
			b = b.quarter();
		}
		return b;
	}

	/** A quarter turn clockwise seen from above: north goes to east, so (x, z) goes to (sz - 1 - z, x). */
	private Blueprint quarter() {
		List<Block> nb = new ArrayList<>(blocks.size());
		for (Block k : blocks) {
			nb.add(new Block(sz - 1 - k.z(), k.y(), k.x(), k.state()));
		}
		List<Part> np = new ArrayList<>(parts.size());
		for (Part p : parts) {
			np.add(new Part(p.id(), (p.turns() + 1) % 4, sz - 1 - p.z(), p.y(), p.x(), sz - 1 - p.z1(), p.y0(), p.x0(), sz - 1 - p.z0(), p.y1(), p.x1()));
		}
		Map<String, String> ns = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : sizes.entrySet()) {
			String[] at = e.getKey().split(",");
			ns.put((sz - 1 - Integer.parseInt(at[2])) + "," + at[1] + "," + at[0], e.getValue());
		}
		return new Blueprint(name, sz, sy, sx, palette, nb, np, ns);
	}

	// ---- the lift list -----------------------------------------------------------------------------------------------------
	/**
	 * The lifts, bottom up: on each layer the parts first, then the ordinary blocks in pallets of at most {@value #PALLET_BLOCKS}
	 * in one {@value #PALLET_AREA} m square, west to east then north to south, each at most {@code palletCap} tonnes. A part that
	 * rests on another starts higher, so it comes after it. {@code stateTonnes} gives each palette entry's mass, {@code partTonnes}
	 * each part's.
	 */
	public List<Lift> lifts(double[] stateTonnes, double[] partTonnes, double palletCap) {
		List<Lift> out = new ArrayList<>();
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < parts.size(); i++) {
			order.add(i);
		}
		order.sort(Comparator.comparingInt((Integer i) -> parts.get(i).y0()).thenComparingInt(i -> parts.get(i).z0()).thenComparingInt(i -> parts.get(i).x0()));
		Map<Integer, List<Integer>> byLayer = new java.util.TreeMap<>();
		for (int i = 0; i < blocks.size(); i++) {
			byLayer.computeIfAbsent(blocks.get(i).y(), k -> new ArrayList<>()).add(i);
		}
		int p = 0;
		int top = Math.max(sy, 1);
		for (int y = 0; y < top; y++) {
			while (p < order.size() && parts.get(order.get(p)).y0() <= y) {
				int i = order.get(p++);
				out.add(new Lift(i, List.of(), partTonnes[i], parts.get(i).y0()));
			}
			List<Integer> layer = byLayer.getOrDefault(y, List.of());
			if (layer.isEmpty()) {
				continue;
			}
			// one 5 x 5 square at a time, west to east then north to south
			Map<Long, List<Integer>> squares = new java.util.TreeMap<>();
			for (int i : layer) {
				Block k = blocks.get(i);
				squares.computeIfAbsent((long) (k.z() / PALLET_AREA) * 100_000 + k.x() / PALLET_AREA, s -> new ArrayList<>()).add(i);
			}
			for (List<Integer> square : squares.values()) {
				List<Integer> pallet = new ArrayList<>();
				double tonnes = 0;
				for (int i : square) {
					double t = stateTonnes[blocks.get(i).state()];
					if (!pallet.isEmpty() && (pallet.size() >= PALLET_BLOCKS || tonnes + t > palletCap)) {
						out.add(new Lift(-1, List.copyOf(pallet), tonnes, y));
						pallet.clear();
						tonnes = 0;
					}
					pallet.add(i);
					tonnes += t;
				}
				if (!pallet.isEmpty()) {
					out.add(new Lift(-1, List.copyOf(pallet), tonnes, y));
				}
			}
		}
		while (p < order.size()) {   // a part reaching past the box's top layer (it cannot, but never lose one)
			int i = order.get(p++);
			out.add(new Lift(i, List.of(), partTonnes[i], parts.get(i).y0()));
		}
		return out;
	}

	/** A lift's box, in blueprint cells: x0, y0, z0, x1, y1, z1. */
	public int[] box(Lift lift) {
		if (lift.isPart()) {
			Part p = parts.get(lift.part());
			return new int[] {p.x0(), p.y0(), p.z0(), p.x1(), p.y1(), p.z1()};
		}
		int[] out = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
		for (int i : lift.blocks()) {
			Block k = blocks.get(i);
			out[0] = Math.min(out[0], k.x());
			out[1] = Math.min(out[1], k.y());
			out[2] = Math.min(out[2], k.z());
			out[3] = Math.max(out[3], k.x());
			out[4] = Math.max(out[4], k.y());
			out[5] = Math.max(out[5], k.z());
		}
		return out;
	}

	/**
	 * For each lift, the earlier lifts it rests on (docs/blueprints.md, "Sharing and tandem lifts"): their x-z boxes overlap and its
	 * bottom is above theirs. A crane working ahead never takes a lift before the lifts it rests on are set.
	 */
	public List<int[]> restsOn(List<Lift> lifts) {
		List<int[]> boxes = new ArrayList<>(lifts.size());
		for (Lift l : lifts) {
			boxes.add(box(l));
		}
		List<int[]> out = new ArrayList<>(lifts.size());
		for (int a = 0; a < lifts.size(); a++) {
			int[] A = boxes.get(a);
			List<Integer> under = new ArrayList<>();
			for (int b = 0; b < a; b++) {
				int[] B = boxes.get(b);
				if (A[1] > B[1] && A[0] <= B[3] && B[0] <= A[3] && A[2] <= B[5] && B[2] <= A[5]) {
					under.add(b);
				}
			}
			out.add(under.stream().mapToInt(Integer::intValue).toArray());
		}
		return out;
	}

	// ---- JSON and the item's compressed form ---------------------------------------------------------------------------------
	public String toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("name", name);
		JsonArray size = new JsonArray();
		size.add(sx);
		size.add(sy);
		size.add(sz);
		o.add("size", size);
		JsonArray pal = new JsonArray();
		palette.forEach(pal::add);
		o.add("palette", pal);
		JsonArray bl = new JsonArray();
		for (Block k : blocks) {
			bl.add(k.x());
			bl.add(k.y());
			bl.add(k.z());
			bl.add(k.state());
		}
		o.add("blocks", bl);
		JsonArray pa = new JsonArray();
		for (Part p : parts) {
			JsonObject j = new JsonObject();
			j.addProperty("id", p.id());
			j.addProperty("turns", p.turns());
			JsonArray at = new JsonArray();
			for (int v : new int[] {p.x(), p.y(), p.z(), p.x0(), p.y0(), p.z0(), p.x1(), p.y1(), p.z1()}) {
				at.add(v);
			}
			j.add("at", at);
			pa.add(j);
		}
		o.add("parts", pa);
		JsonObject si = new JsonObject();
		sizes.forEach(si::addProperty);
		o.add("sizes", si);
		return new Gson().toJson(o);
	}

	public static Blueprint fromJson(String json) {
		JsonObject o = JsonParser.parseString(json).getAsJsonObject();
		JsonArray size = o.getAsJsonArray("size");
		List<String> palette = new ArrayList<>();
		o.getAsJsonArray("palette").forEach(e -> palette.add(e.getAsString()));
		JsonArray bl = o.getAsJsonArray("blocks");
		List<Block> blocks = new ArrayList<>(bl.size() / 4);
		for (int i = 0; i + 3 < bl.size(); i += 4) {
			blocks.add(new Block(bl.get(i).getAsInt(), bl.get(i + 1).getAsInt(), bl.get(i + 2).getAsInt(), bl.get(i + 3).getAsInt()));
		}
		List<Part> parts = new ArrayList<>();
		o.getAsJsonArray("parts").forEach(e -> {
			JsonObject j = e.getAsJsonObject();
			JsonArray at = j.getAsJsonArray("at");
			int[] v = new int[9];
			for (int i = 0; i < 9; i++) {
				v[i] = at.get(i).getAsInt();
			}
			parts.add(new Part(j.get("id").getAsString(), j.get("turns").getAsInt(), v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]));
		});
		Map<String, String> sizes = new LinkedHashMap<>();
		if (o.has("sizes")) {
			o.getAsJsonObject("sizes").entrySet().forEach(e -> sizes.put(e.getKey(), e.getValue().getAsString()));
		}
		return new Blueprint(o.get("name").getAsString(), size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt(), palette, blocks, parts, sizes);
	}

	/** The item's form: the JSON, gzipped and in base64. */
	public String compressed() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream gz = new GZIPOutputStream(bytes)) {
			gz.write(toJson().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return Base64.getEncoder().encodeToString(bytes.toByteArray());
	}

	public static Blueprint decompress(String data) {
		try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)))) {
			return fromJson(new String(gz.readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalArgumentException("not a blueprint", e);
		}
	}
}
