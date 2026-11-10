package dev.wildcatter.client;

import dev.wildcatter.plc.lang.Ladder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws and edits a ladder: rungs between two power rails, the power flow in green from the CPU's own values. */
final class LadderPane {
	private static final int CONTACT_W = 52;
	private static final int ROW_H = 26;
	private static final int COIL_W = 58;
	private static final int WIRE = 0xFF8B93A0;
	private static final int POWER = 0xFF50E070;
	private static final int TEXT = 0xFFD6DAE0;
	private static final int SELECT = 0xFF34507A;
	private static final int BOX_BACK = 0xFF2C3440;
	private static final int RAIL = 0xFFB0B6C0;
	private static final int LABEL = 0xFFE0C060;

	private record Hit(int x0, int y0, int x1, int y1, Object what, Ladder.Rung rung) {
	}

	final Ladder ladder;
	private final Map<Ladder.Node, int[]> sizes = new HashMap<>();
	private final List<Hit> hits = new ArrayList<>();
	Object selected;
	Ladder.Rung selectedRung;
	int scroll;
	int scrollX;
	/** The block library's pin lookup. */
	private final Function<String, List<String>> inputPins;

	LadderPane(Ladder ladder, Function<String, List<String>> inputPins) {
		this.ladder = ladder;
		this.inputPins = inputPins;
		ladder.number();
		if (!ladder.rungs.isEmpty()) {
			selectedRung = ladder.rungs.getFirst();
		}
	}

	/** Every variable the view reads, to ask the CPU for. */
	List<String> watches(String prefix) {
		List<String> out = new ArrayList<>();
		for (Ladder.Rung rung : ladder.rungs) {
			collect(rung.body, prefix, out);
		}
		return out;
	}

	private static void collect(Ladder.Node node, String prefix, List<String> out) {
		out.add(prefix + "." + Ladder.powerVar(node.id()));
		switch (node) {
			case Ladder.Series s -> s.items.forEach(n -> collect(n, prefix, out));
			case Ladder.Parallel p -> p.items.forEach(n -> collect(n, prefix, out));
			default -> {
			}
		}
	}

	private int boxRows(Ladder.Box box) {
		return Math.max(1, inputPins.apply(box.type).size());
	}

	private int[] measure(Ladder.Node node) {
		int[] size = switch (node) {
			case Ladder.Contact c -> new int[] {CONTACT_W, ROW_H};
			case Ladder.Box b -> new int[] {84, 16 + boxRows(b) * 10};
			case Ladder.Series s -> {
				int w = 0;
				int h = ROW_H;
				for (Ladder.Node item : s.items) {
					int[] one = measure(item);
					w += one[0];
					h = Math.max(h, one[1]);
				}
				yield new int[] {Math.max(w, 24), h};
			}
			case Ladder.Parallel p -> {
				int w = 0;
				int h = 0;
				for (Ladder.Node item : p.items) {
					int[] one = measure(item);
					w = Math.max(w, one[0]);
					h += one[1];
				}
				yield new int[] {w + 16, Math.max(h, ROW_H)};
			}
		};
		sizes.put(node, size);
		return size;
	}

	private static boolean on(Map<String, String> values, String prefix, Ladder.Node node) {
		return "true".equals(values.get(prefix + "." + Ladder.powerVar(node.id())));
	}

	void draw(GuiGraphicsExtractor g, Font font, int left, int top, int right, int bottom, Map<String, String> values, String prefix, boolean live, Set<String> forced) {
		hits.clear();
		g.enableScissor(left, top, right, bottom);
		int y = top + 4 - scroll;
		int railLeft = left + 6 - scrollX;
		// the coils line up on the right, past the widest rung; a sheet wider than the pane scrolls sideways
		int widest = 0;
		for (Ladder.Rung rung : ladder.rungs) {
			widest = Math.max(widest, measure(rung.body)[0]);
		}
		int railRight = Math.max(right - 6 - scrollX, railLeft + 2 + widest + 6 + COIL_W + 8);
		for (int r = 0; r < ladder.rungs.size(); r++) {
			Ladder.Rung rung = ladder.rungs.get(r);
			int[] size = measure(rung.body);
			int height = Math.max(size[1], rung.coils.size() * ROW_H) + 12;
			int headX = railLeft + 3;
			if (!rung.label.isBlank()) {
				// a jump's label stands at the rung's left, as the standard draws it
				String tag = rung.label.trim().toUpperCase(java.util.Locale.ROOT) + ":";
				g.text(font, tag, headX, y, LABEL, false);
				headX += font.width(tag) + 4;
			}
			g.text(font, "Rung " + (r + 1) + (rung.comment.isEmpty() ? "" : "  " + rung.comment), headX, y, rung == selectedRung ? 0xFF7FB2F0 : 0xFF6F7884, false);
			hits.add(new Hit(left, y, right, y + 10, rung, rung));
			int rowTop = y + 10;
			int midY = rowTop + ROW_H / 2;
			g.fill(railLeft, rowTop - 2, railLeft + 2, rowTop + height - 8, live ? POWER : RAIL);
			g.fill(railRight - 2, rowTop - 2, railRight, rowTop + height - 8, RAIL);
			int coilLeft = railRight - COIL_W - 6;
			drawNode(g, font, rung.body, railLeft + 2, rowTop, size[0], live, values, prefix, rung, live, forced);
			boolean out = live && on(values, prefix, rung.body);
			int reach = railLeft + 2 + size[0];
			g.fill(reach, midY, coilLeft, midY + 1, out ? POWER : WIRE);
			int coilY = rowTop;
			for (Ladder.Coil coil : rung.coils) {
				String label = switch (coil.kind) {
					case COIL -> "( )";
					case NOT -> "(/)";
					case SET -> "(S)";
					case RESET -> "(R)";
					case JUMP -> ">>" + (coil.var.isBlank() ? "?" : coil.var.trim().toUpperCase(java.util.Locale.ROOT));
					case RETURN -> "<RETURN>";
				};
				int cy = coilY + ROW_H / 2;
				if (coil == selected) {
					g.fill(coilLeft, coilY, railRight - 2, coilY + ROW_H, SELECT);
				}
				g.fill(coilLeft, midY, coilLeft + 1, cy + 1, out ? POWER : WIRE);
				if (coil.kind.flow()) {
					// a jump or return ends the rung: no wire on to the right rail
					g.fill(coilLeft, cy, coilLeft + 3, cy + 1, out ? POWER : WIRE);
					g.text(font, font.plainSubstrByWidth(label, railRight - 2 - coilLeft - 4), coilLeft + 4, cy - 4, out ? POWER : LABEL, false);
				} else {
					g.fill(coilLeft, cy, railRight - 2, cy + 1, out ? POWER : WIRE);
					g.fill(coilLeft + 6, cy - 5, railRight - 8, cy + 6, 0xFF23272F);
					g.text(font, label, coilLeft + 8, cy - 4, out ? POWER : TEXT, false);
					g.text(font, font.plainSubstrByWidth(coil.var, COIL_W - 4), coilLeft + 4, coilY + 2 - 1, TEXT, false);
				}
				hits.add(new Hit(coilLeft, coilY, railRight, coilY + ROW_H, coil, rung));
				coilY += ROW_H;
			}
			y = rowTop + height;
		}
		g.disableScissor();
	}

	private void drawNode(GuiGraphicsExtractor g, Font font, Ladder.Node node, int x, int y, int width, boolean in, Map<String, String> values, String prefix, Ladder.Rung rung, boolean live, Set<String> forced) {
		int[] size = sizes.get(node);
		int mid = y + ROW_H / 2;
		boolean out = live && on(values, prefix, node);
		switch (node) {
			case Ladder.Series s -> {
				int at = x;
				boolean power = in;
				if (s.items.isEmpty()) {
					g.fill(x, mid, x + width, mid + 1, live && power ? POWER : WIRE);
				}
				int used = 0;
				for (Ladder.Node item : s.items) {
					int[] one = sizes.get(item);
					drawNode(g, font, item, at, y, one[0], power, values, prefix, rung, live, forced);
					at += one[0];
					used += one[0];
					power = live && on(values, prefix, item);
				}
				if (used < width) {
					g.fill(at, mid, x + width, mid + 1, power ? POWER : WIRE);
				}
			}
			case Ladder.Parallel p -> {
				int rowY = y;
				int lastMid = mid;
				for (Ladder.Node item : p.items) {
					int[] one = sizes.get(item);
					int branchMid = rowY + ROW_H / 2;
					lastMid = branchMid;
					boolean branch = live && on(values, prefix, item);
					g.fill(x, branchMid, x + 8, branchMid + 1, live && in ? POWER : WIRE);
					drawNode(g, font, item, x + 8, rowY, width - 16, in, values, prefix, rung, live, forced);
					g.fill(x + 8 + one[0], branchMid, x + width, branchMid + 1, branch ? POWER : WIRE);
					rowY += one[1];
				}
				g.fill(x, mid, x + 1, lastMid + 1, live && in ? POWER : WIRE);
				g.fill(x + width - 1, mid, x + width, lastMid + 1, out ? POWER : WIRE);
				if (node == selected) {
					g.fill(x, y - 1, x + width, y, 0xFF7FB2F0);
				}
				hits.add(new Hit(x, y, x + 8, y + size[1], node, rung));
			}
			case Ladder.Contact c -> {
				if (node == selected) {
					g.fill(x, y, x + width, y + ROW_H, SELECT);
				}
				int cx = x + width / 2;
				g.fill(x, mid, cx - 6, mid + 1, live && in ? POWER : WIRE);
				g.fill(cx + 6, mid, x + width, mid + 1, out ? POWER : WIRE);
				int bar = live && out ? POWER : TEXT;
				g.fill(cx - 6, mid - 5, cx - 5, mid + 6, bar);
				g.fill(cx + 5, mid - 5, cx + 6, mid + 6, bar);
				if (c.negated) {
					g.fill(cx - 5, mid - 1, cx + 5, mid + 1, bar);
				}
				boolean isForced = forced.contains(c.var.toUpperCase(java.util.Locale.ROOT));
				g.text(font, font.plainSubstrByWidth(c.var.isEmpty() ? "?" : c.var, width - 2), x + 1, y + 1, isForced ? 0xFFF07070 : TEXT, false);
				hits.add(new Hit(x, y, x + width, y + ROW_H, node, rung));
			}
			case Ladder.Box b -> {
				if (node == selected) {
					g.fill(x + 2, y - 1, x + width - 2, y + size[1], SELECT);
				}
				g.fill(x, mid, x + 8, mid + 1, live && in ? POWER : WIRE);
				g.fill(x + width - 8, mid, x + width, mid + 1, out ? POWER : WIRE);
				g.fill(x + 8, y + 1, x + width - 8, y + size[1] - 1, BOX_BACK);
				g.text(font, font.plainSubstrByWidth(b.type + (b.instance.isEmpty() ? "" : " " + b.instance), width - 18), x + 10, y + 3, 0xFF6FD0C0, false);
				int rowY = y + 13;
				for (String pin : inputPins.apply(b.type)) {
					String value = b.pins.getOrDefault(pin, "");
					g.text(font, font.plainSubstrByWidth(pin + " " + value, width - 18), x + 10, rowY, 0xFFA0A8B4, false);
					rowY += 10;
				}
				hits.add(new Hit(x, y, x + width, y + size[1], node, rung));
			}
		}
	}

	/** Selects what is at the point; true if anything is. */
	boolean click(double mx, double my) {
		Hit best = null;
		for (Hit hit : hits) {
			if (mx >= hit.x0() && mx < hit.x1() && my >= hit.y0() && my < hit.y1()) {
				best = hit;
			}
		}
		if (best == null) {
			return false;
		}
		selected = best.what() instanceof Ladder.Rung ? null : best.what();
		selectedRung = best.rung();
		return true;
	}

	// ---- editing ----------------------------------------------------------------------------------------------------------------------

	Ladder.Rung addRung() {
		Ladder.Rung rung = new Ladder.Rung();
		int at = selectedRung == null ? ladder.rungs.size() : ladder.rungs.indexOf(selectedRung) + 1;
		ladder.rungs.add(at, rung);
		selectedRung = rung;
		selected = null;
		ladder.number();
		return rung;
	}

	private Ladder.Rung rungForEdit() {
		if (selectedRung == null) {
			addRung();
		}
		return selectedRung;
	}

	private Ladder.Node parentOf(Ladder.Node root, Ladder.Node child) {
		List<Ladder.Node> items = switch (root) {
			case Ladder.Series s -> s.items;
			case Ladder.Parallel p -> p.items;
			default -> List.of();
		};
		for (Ladder.Node item : items) {
			if (item == child) {
				return root;
			}
			Ladder.Node found = parentOf(item, child);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/** Puts {@code node} in series after the selection, or at the end of the rung. */
	void insertSeries(Ladder.Node node) {
		Ladder.Rung rung = rungForEdit();
		if (selected instanceof Ladder.Node at && at != rung.body) {
			Ladder.Node parent = parentOf(rung.body, at);
			if (parent instanceof Ladder.Series s) {
				s.items.add(s.items.indexOf(at) + 1, node);
			} else if (parent instanceof Ladder.Parallel p) {
				if (at instanceof Ladder.Parallel) {
					p.items.add(node);   // clicking a branch's rail and adding puts it on a new branch
				} else {
					Ladder.Series wrap = new Ladder.Series();
					wrap.items.add(at);
					wrap.items.add(node);
					p.items.set(p.items.indexOf(at), wrap);
				}
			}
		} else {
			rung.body.items.add(node);
		}
		selected = node;
		ladder.number();
	}

	/** Wraps the selection in a parallel branch, with {@code node} on the new branch. */
	void insertBranch(Ladder.Node node) {
		Ladder.Rung rung = rungForEdit();
		if (selected instanceof Ladder.Parallel p) {
			p.items.add(node);
		} else if (selected instanceof Ladder.Node at && at != rung.body) {
			Ladder.Node parent = parentOf(rung.body, at);
			Ladder.Parallel wrap = new Ladder.Parallel();
			wrap.items.add(at);
			wrap.items.add(node);
			if (parent instanceof Ladder.Series s) {
				s.items.set(s.items.indexOf(at), wrap);
			} else if (parent instanceof Ladder.Parallel p) {
				p.items.set(p.items.indexOf(at), wrap);
			}
		} else {
			Ladder.Parallel wrap = new Ladder.Parallel();
			wrap.items.add(node);
			rung.body.items.add(wrap);
		}
		selected = node;
		ladder.number();
	}

	void addCoil(Ladder.CoilKind kind) {
		Ladder.Coil coil = new Ladder.Coil(kind, "");
		rungForEdit().coils.add(coil);
		selected = coil;
	}

	void deleteSelected() {
		if (selected instanceof Ladder.Coil coil) {
			if (selectedRung != null) {
				selectedRung.coils.remove(coil);
			}
		} else if (selected instanceof Ladder.Node node && selectedRung != null) {
			Ladder.Node parent = parentOf(selectedRung.body, node);
			if (parent instanceof Ladder.Series s) {
				s.items.remove(node);
			} else if (parent instanceof Ladder.Parallel p) {
				p.items.remove(node);
				if (p.items.size() == 1) {
					Ladder.Node only = p.items.getFirst();
					Ladder.Node grand = parentOf(selectedRung.body, p);
					if (grand instanceof Ladder.Series s) {
						s.items.set(s.items.indexOf(p), only);
					} else if (grand instanceof Ladder.Parallel q) {
						q.items.set(q.items.indexOf(p), only);
					}
				}
			}
		} else if (selectedRung != null) {
			int at = ladder.rungs.indexOf(selectedRung);
			ladder.rungs.remove(selectedRung);
			selectedRung = ladder.rungs.isEmpty() ? null : ladder.rungs.get(Math.min(at, ladder.rungs.size() - 1));
		}
		selected = null;
		ladder.number();
	}
}
