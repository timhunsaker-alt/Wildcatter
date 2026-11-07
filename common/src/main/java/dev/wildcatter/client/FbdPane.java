package dev.wildcatter.client;

import dev.wildcatter.plc.lang.BlockLibrary;
import dev.wildcatter.plc.lang.Fbd;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws and edits a function block diagram: blocks on a sheet, a wire from an output pin to an input pin, live values from the CPU. */
final class FbdPane {
	private static final int BLOCK_W = 76;
	private static final int ROW = 11;
	private static final int WIRE = 0xFF8B93A0;
	private static final int POWER = 0xFF50E070;
	private static final int TEXT = 0xFFD6DAE0;
	private static final int BACK = 0xFF2C3440;
	private static final int SELECT = 0xFF34507A;
	private static final int PIN = 0xFF7FB2F0;

	final Fbd fbd;
	Fbd.Block selected;
	/** A wire being drawn: its source. */
	String pendingWire;
	int scrollX;
	int scrollY;
	private Fbd.Block dragging;
	private int dragDX;
	private int dragDY;
	private boolean panning;
	private double panX;
	private double panY;
	private final Function<String, BlockLibrary.Pins> pins;

	FbdPane(Fbd fbd, Function<String, BlockLibrary.Pins> pins) {
		this.fbd = fbd;
		this.pins = pins;
	}

	private BlockLibrary.Pins pinsOf(Fbd.Block block) {
		if (block.type.equals(Fbd.IN)) {
			return new BlockLibrary.Pins(List.of(), List.of("OUT"));
		}
		if (block.type.equals(Fbd.OUT)) {
			return new BlockLibrary.Pins(List.of("IN"), List.of());
		}
		BlockLibrary.Pins found = pins.apply(block.type);
		return found == null ? new BlockLibrary.Pins(List.of(), List.of()) : found;
	}

	private int height(Fbd.Block block) {
		BlockLibrary.Pins p = pinsOf(block);
		return 14 + Math.max(1, Math.max(p.inputs().size(), p.outputs().size())) * ROW;
	}

	private int inY(Fbd.Block block, int index) {
		return block.y + 14 + index * ROW + ROW / 2;
	}

	/** The variable holding a block's output, for the CPU to report; null if the block has none to read. */
	String valueTag(Fbd.Block block, String pin, String prefix) {
		if (block.type.equals(Fbd.IN)) {
			String name = block.name == null ? "" : block.name.toUpperCase(java.util.Locale.ROOT);
			return name.startsWith("%") ? name : name.matches("[A-Z_][A-Z0-9_.]*") ? prefix + "." + name : null;
		}
		if (block.type.equals(Fbd.OUT)) {
			return null;
		}
		return block.name != null && !block.name.isBlank() ? prefix + "." + block.name.toUpperCase(java.util.Locale.ROOT) + "." + pin : prefix + "." + Fbd.resultVar(block.id);
	}

	List<String> watches(String prefix) {
		List<String> out = new ArrayList<>();
		for (Fbd.Block block : fbd.blocks) {
			for (String pin : pinsOf(block).outputs()) {
				String tag = valueTag(block, pin, prefix);
				if (tag != null) {
					out.add(tag);
				}
			}
		}
		return out;
	}

	private String value(Fbd.Block block, String pin, Map<String, String> values, String prefix) {
		String tag = valueTag(block, pin, prefix);
		return tag == null ? null : values.get(tag);
	}

	void draw(GuiGraphicsExtractor g, Font font, int left, int top, int right, int bottom, Map<String, String> values, String prefix, boolean live) {
		g.enableScissor(left, top, right, bottom);
		int ox = left + 4 - scrollX;
		int oy = top + 4 - scrollY;
		List<Fbd.Block> order = fbd.order();
		for (Fbd.Block block : fbd.blocks) {
			List<String> inputs = pinsOf(block).inputs();
			for (int i = 0; i < inputs.size(); i++) {
				String source = block.inputs.get(inputs.get(i));
				if (Fbd.isWire(source)) {
					Fbd.Block from = fbd.block(Fbd.wireBlock(source));
					if (from != null) {
						int oi = Math.max(0, pinsOf(from).outputs().indexOf(Fbd.wirePin(source)));
						String v = live ? value(from, Fbd.wirePin(source), values, prefix) : null;
						int colour = "true".equals(v) ? POWER : WIRE;
						int x0 = ox + from.x + BLOCK_W;
						int y0 = oy + inY(from, oi);
						int x1 = ox + block.x;
						int y1 = oy + inY(block, i);
						int mid = x1 > x0 ? (x0 + x1) / 2 : x0 + 6;
						g.fill(x0, y0, mid, y0 + 1, colour);
						g.fill(mid, Math.min(y0, y1), mid + 1, Math.max(y0, y1) + 1, colour);
						g.fill(Math.min(mid, x1), y1, Math.max(mid, x1), y1 + 1, colour);
					}
				}
			}
		}
		for (Fbd.Block block : fbd.blocks) {
			int x = ox + block.x;
			int y = oy + block.y;
			int h = height(block);
			if (block == selected) {
				g.fill(x - 2, y - 2, x + BLOCK_W + 2, y + h + 2, SELECT);
			}
			g.fill(x, y, x + BLOCK_W, y + h, BACK);
			String title = block.type.equals(Fbd.IN) ? "IN" : block.type.equals(Fbd.OUT) ? "OUT" : block.type;
			g.text(font, font.plainSubstrByWidth(title, BLOCK_W - 18), x + 3, y + 2, 0xFF6FD0C0, false);
			String number = Integer.toString(order.indexOf(block) + 1);
			g.text(font, number, x + BLOCK_W - font.width(number) - 2, y + 2, 0xFF6F7884, false);   // its place in the execution order
			BlockLibrary.Pins p = pinsOf(block);
			if (block.type.equals(Fbd.IN) || block.type.equals(Fbd.OUT)) {
				g.text(font, font.plainSubstrByWidth(block.name == null || block.name.isEmpty() ? "?" : block.name, BLOCK_W - 6), x + 3, y + 14, TEXT, false);
			} else if (block.name != null && !block.name.isBlank()) {
				g.text(font, font.plainSubstrByWidth(block.name, BLOCK_W - 6), x + 3, y - 9, 0xFFA0A8B4, false);
			}
			for (int i = 0; i < p.inputs().size(); i++) {
				String pin = p.inputs().get(i);
				int py = y + 14 + i * ROW;
				g.fill(x - 2, py + ROW / 2 - 1, x, py + ROW / 2 + 2, PIN);
				if (!block.type.equals(Fbd.OUT)) {
					String source = block.inputs.get(pin);
					String shown = pin + (source != null && !Fbd.isWire(source) && !source.isBlank() ? " " + source : "");
					g.text(font, font.plainSubstrByWidth(shown, BLOCK_W / 2 + 12), x + 2, py + 1, 0xFFA0A8B4, false);
				}
			}
			for (int i = 0; i < p.outputs().size(); i++) {
				String pin = p.outputs().get(i);
				int py = y + 14 + i * ROW;
				String pending = Fbd.wire(block.id, pin);
				g.fill(x + BLOCK_W, py + ROW / 2 - 1, x + BLOCK_W + 2, py + ROW / 2 + 2, pending.equals(pendingWire) ? 0xFFE0C060 : PIN);
				String v = live ? value(block, pin, values, prefix) : null;
				String label = v == null ? pin : v;
				int colour = "true".equals(v) ? POWER : v != null ? 0xFFE0B070 : 0xFFA0A8B4;
				if (!(block.type.equals(Fbd.IN) && v == null)) {
					String cut = font.plainSubstrByWidth(label, BLOCK_W / 2);
					int width = font.width(cut);
					g.text(font, cut, x + BLOCK_W - width - 1, block.type.equals(Fbd.IN) ? py + 11 : py + 1, colour, false);
				}
			}
		}
		g.disableScissor();
	}

	/** A press on the sheet: a pin starts or ends a wire, a block is selected and dragged. */
	boolean press(double mx, double my, int left, int top) {
		int sx = (int) mx - (left + 4 - scrollX);
		int sy = (int) my - (top + 4 - scrollY);
		for (int b = fbd.blocks.size() - 1; b >= 0; b--) {
			Fbd.Block block = fbd.blocks.get(b);
			BlockLibrary.Pins p = pinsOf(block);
			for (int i = 0; i < p.outputs().size(); i++) {
				int py = inY(block, i);
				if (Math.abs(sx - (block.x + BLOCK_W)) <= 5 && Math.abs(sy - py) <= 5) {
					pendingWire = Fbd.wire(block.id, p.outputs().get(i));
					selected = block;
					return true;
				}
			}
			for (int i = 0; i < p.inputs().size(); i++) {
				int py = inY(block, i);
				if (Math.abs(sx - block.x) <= 5 && Math.abs(sy - py) <= 5) {
					if (pendingWire != null) {
						block.inputs.put(p.inputs().get(i), pendingWire);
						pendingWire = null;
					} else {
						block.inputs.remove(p.inputs().get(i));   // a click on an input with nothing in hand takes its wire off
					}
					selected = block;
					return true;
				}
			}
			if (sx >= block.x && sx < block.x + BLOCK_W && sy >= block.y && sy < block.y + height(block)) {
				selected = block;
				dragging = block;
				dragDX = sx - block.x;
				dragDY = sy - block.y;
				pendingWire = null;
				return true;
			}
		}
		selected = null;
		pendingWire = null;
		panning = true;   // the empty sheet drags to pan
		panX = mx;
		panY = my;
		return true;
	}

	boolean drag(double mx, double my, int left, int top) {
		if (panning) {
			scrollX = Math.max(0, scrollX - (int) (mx - panX));
			scrollY = Math.max(0, scrollY - (int) (my - panY));
			panX = mx;
			panY = my;
			return true;
		}
		if (dragging == null) {
			return false;
		}
		dragging.x = Math.max(0, (int) mx - (left + 4 - scrollX) - dragDX);
		dragging.y = Math.max(0, (int) my - (top + 4 - scrollY) - dragDY);
		return true;
	}

	void release() {
		dragging = null;
		panning = false;
	}

	Fbd.Block add(String type, String name) {
		int y = 8;
		for (Fbd.Block block : fbd.blocks) {
			y = Math.max(y, block.y + height(block) + 12);
		}
		int x = type.equals(Fbd.IN) ? 4 : type.equals(Fbd.OUT) ? 210 : 100;
		Fbd.Block block = new Fbd.Block(fbd.nextId(), type, name, x, y);
		fbd.blocks.add(block);
		selected = block;
		return block;
	}

	void deleteSelected() {
		if (selected == null) {
			return;
		}
		fbd.blocks.remove(selected);
		String prefix = "@" + selected.id + ".";
		for (Fbd.Block block : fbd.blocks) {
			block.inputs.values().removeIf(s -> s.startsWith(prefix));
		}
		selected = null;
	}
}
