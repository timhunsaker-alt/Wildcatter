package dev.wildcatter.client;

import dev.wildcatter.plc.lang.Sfc;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws and edits a sequential function chart: steps and transitions on a sheet, wired pin to pin, the active steps and clear conditions from the CPU. */
final class SfcPane {
	private static final int W = 84;
	private static final int STEP_H = 18;
	private static final int TRANS_H = 10;
	private static final int ROW = 11;
	private static final int GAP = 12;
	/** Sideways, one branch from the next. */
	private static final int BRANCH = 180;
	private static final int WIRE = 0xFF8B93A0;
	private static final int POWER = 0xFF50E070;
	private static final int TEXT = 0xFFD6DAE0;
	private static final int BACK = 0xFF2C3440;
	private static final int ACTIVE = 0xFF2F6B45;
	private static final int SELECT = 0xFF34507A;
	private static final int PIN = 0xFF7FB2F0;
	private static final int HELD = 0xFFE0C060;

	final Sfc sfc;
	Sfc.Step selectedStep;
	Sfc.Transition selectedTransition;
	Sfc.Association selectedAction;
	/** A wire being drawn: the step or transition it leaves. */
	Sfc.Step pendingStep;
	Sfc.Transition pendingTransition;
	int scrollX;
	int scrollY;
	private Sfc.Step dragStep;
	private Sfc.Transition dragTransition;
	private int dragDX;
	private int dragDY;
	private boolean panning;
	private double panX;
	private double panY;

	SfcPane(Sfc sfc) {
		this.sfc = sfc;
	}

	List<String> watches(String prefix) {
		List<String> out = new ArrayList<>();
		for (Sfc.Step step : sfc.steps) {
			String name = step.name.toUpperCase(Locale.ROOT);
			if (!name.isEmpty()) {
				out.add(prefix + "." + name + ".X");
				out.add(prefix + "." + name + ".T");
			}
			for (Sfc.Association a : step.actions) {
				if (!a.action.isBlank()) {
					out.add(prefix + "." + sfc.controlOf(a.action) + ".Q");
				}
			}
		}
		for (Sfc.Transition t : sfc.transitions) {
			out.add(prefix + "." + Sfc.conditionVar(t.id));
		}
		return out;
	}

	private void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int colour) {
		g.fill(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1) + 1, Math.max(y0, y1) + 1, colour);
	}

	void draw(GuiGraphicsExtractor g, Font font, int left, int top, int right, int bottom, Map<String, String> values, String prefix, boolean live) {
		g.enableScissor(left, top, right, bottom);
		int ox = left + 4 - scrollX;
		int oy = top + 4 - scrollY;
		// the wires first, under everything
		int gutter = 0;
		for (Sfc.Transition t : sfc.transitions) {
			int cx = ox + t.x + W / 2;
			int above = oy + t.y - 5;
			int below = oy + t.y + TRANS_H + 5;
			boolean fromMany = t.from.size() > 1;
			boolean toMany = t.to.size() > 1;
			int fromLow = cx;
			int fromHigh = cx;
			for (int id : t.from) {
				Sfc.Step step = sfc.step(id);
				if (step == null) {
					continue;
				}
				int sx = ox + step.x + W / 2;
				int sy = oy + step.y + STEP_H;
				fromLow = Math.min(fromLow, sx);
				fromHigh = Math.max(fromHigh, sx);
				if (sy > above) {
					// a transition above its step: out, round the side and back
					int side = Math.max(ox + step.x, ox + t.x) + W + 6 + (gutter++ % 6) * 3;
					line(g, sx, sy, sx, sy + 3, WIRE);
					line(g, sx, sy + 3, side, sy + 3, WIRE);
					line(g, side, above, side, sy + 3, WIRE);
					line(g, cx, above, side, above, WIRE);
				} else {
					line(g, sx, sy, sx, above, WIRE);
					line(g, sx, above, cx, above, WIRE);
				}
			}
			line(g, cx, above, cx, oy + t.y, WIRE);
			if (fromMany) {
				line(g, fromLow, above - 2, fromHigh, above - 2, WIRE);
				line(g, fromLow, above, fromHigh, above, WIRE);
			}
			line(g, cx, oy + t.y + TRANS_H, cx, below, WIRE);
			int toLow = cx;
			int toHigh = cx;
			int jumps = 0;
			for (int id : t.to) {
				Sfc.Step step = sfc.step(id);
				if (step == null) {
					continue;
				}
				int sx = ox + step.x + W / 2;
				int sy = oy + step.y;
				if (sy < below) {
					// a way back up is a jump, as a real chart draws it: an arrow and the step's name, not a line across everything
					int jx = cx + jumps++ * 70;
					toHigh = Math.max(toHigh, jx);
					line(g, jx, below, jx, below + 4, WIRE);
					g.fill(jx - 2, below + 5, jx + 3, below + 6, WIRE);
					g.fill(jx - 1, below + 6, jx + 2, below + 7, WIRE);
					g.fill(jx, below + 7, jx + 1, below + 8, WIRE);
					g.text(font, font.plainSubstrByWidth(step.name, 60), jx + 5, below + 2, 0xFFA0A8B4, false);
				} else {
					toLow = Math.min(toLow, sx);
					toHigh = Math.max(toHigh, sx);
					line(g, cx, below, sx, below, WIRE);
					line(g, sx, below, sx, sy, WIRE);
				}
			}
			if (toMany) {
				line(g, toLow, below, toHigh, below, WIRE);
				line(g, toLow, below + 2, toHigh, below + 2, WIRE);
			}
		}
		for (Sfc.Transition t : sfc.transitions) {
			int x = ox + t.x;
			int y = oy + t.y;
			int cx = x + W / 2;
			boolean clear = live && "true".equals(values.get(prefix + "." + Sfc.conditionVar(t.id)));
			if (t == selectedTransition) {
				g.fill(cx - 22, y - 1, cx + 22, y + TRANS_H + 1, SELECT);
			}
			g.fill(cx - 16, y + 3, cx + 16, y + 6, clear ? POWER : TEXT);
			g.fill(cx - 2, y - 2, cx + 3, y + 1, t == pendingTransition ? HELD : PIN);
			g.fill(cx - 2, y + TRANS_H - 1, cx + 3, y + TRANS_H + 2, t == pendingTransition ? HELD : PIN);
			g.text(font, font.plainSubstrByWidth(t.condition == null || t.condition.isBlank() ? "(condition)" : t.condition, 150), cx + 22, y, clear ? POWER : 0xFFA0A8B4, false);
		}
		for (Sfc.Step step : sfc.steps) {
			int x = ox + step.x;
			int y = oy + step.y;
			String name = step.name.toUpperCase(Locale.ROOT);
			boolean active = live && "true".equals(values.get(prefix + "." + name + ".X"));
			if (step == selectedStep && selectedAction == null) {
				g.fill(x - 2, y - 2, x + W + 2, y + STEP_H + 2, SELECT);
			}
			g.fill(x, y, x + W, y + STEP_H, active ? ACTIVE : BACK);
			if (step.initial) {
				// the standard draws an initial step with a double border
				g.fill(x + 1, y + 1, x + W - 1, y + 2, TEXT);
				g.fill(x + 1, y + STEP_H - 2, x + W - 1, y + STEP_H - 1, TEXT);
				g.fill(x + 1, y + 1, x + 2, y + STEP_H - 1, TEXT);
				g.fill(x + W - 2, y + 1, x + W - 1, y + STEP_H - 1, TEXT);
			}
			String shown = step.name.isEmpty() ? "?" : step.name;
			String time = live ? values.get(prefix + "." + name + ".T") : null;
			g.text(font, font.plainSubstrByWidth(shown, time == null ? W - 8 : W / 2), x + 4, y + 5, TEXT, false);
			if (time != null) {
				String label = seconds(time);
				g.text(font, label, x + W - 4 - font.width(label), y + 5, 0xFFE0B070, false);
			}
			g.fill(x + W / 2 - 2, y - 2, x + W / 2 + 3, y + 1, PIN);
			g.fill(x + W / 2 - 2, y + STEP_H - 1, x + W / 2 + 3, y + STEP_H + 2, step == pendingStep ? HELD : PIN);
			for (int i = 0; i < step.actions.size(); i++) {
				Sfc.Association a = step.actions.get(i);
				int ay = y + i * ROW;
				boolean q = live && !a.action.isBlank() && "true".equals(values.get(prefix + "." + sfc.controlOf(a.action) + ".Q"));
				g.fill(x + W, y + 9, x + W + 4, y + 10, WIRE);
				g.fill(x + W + 4, ay, x + W + 92, ay + ROW - 1, a == selectedAction ? SELECT : BACK);
				g.text(font, a.qualifier, x + W + 6, ay + 1, 0xFF6FD0C0, false);
				String named = a.action + (a.time == null || a.time.isBlank() ? "" : " " + a.time);
				g.text(font, font.plainSubstrByWidth(named.isBlank() ? "?" : named, 70), x + W + 22, ay + 1, q ? POWER : TEXT, false);
			}
		}
		g.disableScissor();
	}

	/** A step's time, which the CPU reads in milliseconds. */
	private static String seconds(String ms) {
		try {
			return String.format(Locale.ROOT, "%.1f s", Long.parseLong(ms) / 1000.0);
		} catch (NumberFormatException e) {
			return ms;
		}
	}

	/** A press on the sheet: a pin starts or ends a wire, an action row or element is selected, an element dragged, the empty sheet panned. */
	boolean press(double mx, double my, int left, int top) {
		int sx = (int) mx - (left + 4 - scrollX);
		int sy = (int) my - (top + 4 - scrollY);
		for (int b = sfc.transitions.size() - 1; b >= 0; b--) {
			Sfc.Transition t = sfc.transitions.get(b);
			int cx = t.x + W / 2;
			if (Math.abs(sx - cx) <= 5 && Math.abs(sy - t.y) <= 3) {
				if (pendingStep != null) {
					if (!t.from.contains(pendingStep.id)) {
						t.from.add(pendingStep.id);
					}
					pendingStep = null;
				} else if (!t.from.isEmpty()) {
					t.from.removeLast();   // a click with nothing in hand takes its last wire off
				}
				selectTransition(t);
				return true;
			}
			if (Math.abs(sx - cx) <= 5 && Math.abs(sy - (t.y + TRANS_H)) <= 3) {
				pendingTransition = t;
				pendingStep = null;
				selectTransition(t);
				return true;
			}
		}
		for (int b = sfc.steps.size() - 1; b >= 0; b--) {
			Sfc.Step step = sfc.steps.get(b);
			int cx = step.x + W / 2;
			if (Math.abs(sx - cx) <= 5 && Math.abs(sy - step.y) <= 3) {
				if (pendingTransition != null) {
					if (!pendingTransition.to.contains(step.id)) {
						pendingTransition.to.add(step.id);
					}
					pendingTransition = null;
				} else {
					for (Sfc.Transition t : sfc.transitions) {
						t.to.remove((Integer) step.id);
					}
				}
				selectStep(step);
				return true;
			}
			if (Math.abs(sx - cx) <= 5 && Math.abs(sy - (step.y + STEP_H)) <= 3) {
				pendingStep = step;
				pendingTransition = null;
				selectStep(step);
				return true;
			}
			for (int i = 0; i < step.actions.size(); i++) {
				int ay = step.y + i * ROW;
				if (sx >= step.x + W + 4 && sx < step.x + W + 92 && sy >= ay && sy < ay + ROW) {
					selectStep(step);
					selectedAction = step.actions.get(i);
					return true;
				}
			}
			if (sx >= step.x && sx < step.x + W && sy >= step.y && sy < step.y + STEP_H) {
				selectStep(step);
				dragStep = step;
				dragDX = sx - step.x;
				dragDY = sy - step.y;
				pendingStep = null;
				pendingTransition = null;
				return true;
			}
		}
		for (int b = sfc.transitions.size() - 1; b >= 0; b--) {
			Sfc.Transition t = sfc.transitions.get(b);
			int cx = t.x + W / 2;
			if (Math.abs(sx - cx) <= 22 && sy >= t.y - 1 && sy <= t.y + TRANS_H) {
				selectTransition(t);
				dragTransition = t;
				dragDX = sx - t.x;
				dragDY = sy - t.y;
				pendingStep = null;
				pendingTransition = null;
				return true;
			}
		}
		selectedStep = null;
		selectedTransition = null;
		selectedAction = null;
		pendingStep = null;
		pendingTransition = null;
		panning = true;
		panX = mx;
		panY = my;
		return true;
	}

	private void selectStep(Sfc.Step step) {
		selectedStep = step;
		selectedTransition = null;
		selectedAction = null;
	}

	private void selectTransition(Sfc.Transition t) {
		selectedTransition = t;
		selectedStep = null;
		selectedAction = null;
	}

	boolean drag(double mx, double my, int left, int top) {
		if (panning) {
			scrollX = Math.max(0, scrollX - (int) (mx - panX));
			scrollY = Math.max(0, scrollY - (int) (my - panY));
			panX = mx;
			panY = my;
			return true;
		}
		int sx = Math.max(0, (int) mx - (left + 4 - scrollX) - dragDX);
		int sy = Math.max(0, (int) my - (top + 4 - scrollY) - dragDY);
		if (dragStep != null) {
			dragStep.x = sx;
			dragStep.y = sy;
			return true;
		}
		if (dragTransition != null) {
			dragTransition.x = sx;
			dragTransition.y = sy;
			return true;
		}
		return false;
	}

	void release() {
		dragStep = null;
		dragTransition = null;
		panning = false;
	}

	private String nextName() {
		int n = sfc.steps.size() + 1;
		while (true) {
			String name = "S" + n;
			if (sfc.steps.stream().noneMatch(s -> s.name.equalsIgnoreCase(name))) {
				return name;
			}
			n++;
		}
	}

	private int lowest() {
		int y = -GAP;
		for (Sfc.Step step : sfc.steps) {
			y = Math.max(y, step.y + STEP_H);
		}
		for (Sfc.Transition t : sfc.transitions) {
			y = Math.max(y, t.y + TRANS_H);
		}
		return y;
	}

	/** A new step: after the selected transition, as a parallel branch if it already has one, or else at the foot of the sheet. The chart's first is initial. */
	void addStep() {
		pendingStep = null;
		pendingTransition = null;
		Sfc.Step step;
		if (selectedTransition != null) {
			Sfc.Transition t = selectedTransition;
			step = new Sfc.Step(sfc.nextId(), nextName(), false, t.x + t.to.size() * BRANCH, t.y + TRANS_H + GAP);
			sfc.steps.add(step);
			t.to.add(step.id);
		} else {
			step = new Sfc.Step(sfc.nextId(), nextName(), sfc.steps.isEmpty(), 8, lowest() + GAP * 2);   // clear of a jump's arrow and name above
			sfc.steps.add(step);
		}
		selectStep(step);
	}

	/** A new transition: after the selected step, as another way out if it already has one, or else alone. */
	void addTransition() {
		pendingStep = null;
		pendingTransition = null;
		Sfc.Transition t;
		if (selectedStep != null) {
			Sfc.Step step = selectedStep;
			int ways = (int) sfc.transitions.stream().filter(o -> o.from.contains(step.id)).count();
			int rows = Math.max(0, step.actions.size() * ROW - STEP_H);
			t = new Sfc.Transition(sfc.nextId(), "", step.x + ways * BRANCH, step.y + STEP_H + rows + GAP);
			t.from.add(step.id);
			sfc.transitions.add(t);
		} else {
			t = new Sfc.Transition(sfc.nextId(), "", 8, lowest() + GAP);
			sfc.transitions.add(t);
		}
		selectTransition(t);
	}

	void deleteSelected() {
		if (selectedAction != null && selectedStep != null) {
			selectedStep.actions.remove(selectedAction);
			selectedAction = null;
		} else if (selectedStep != null) {
			int id = selectedStep.id;
			sfc.steps.remove(selectedStep);
			for (Sfc.Transition t : sfc.transitions) {
				t.from.remove((Integer) id);
				t.to.remove((Integer) id);
			}
			selectedStep = null;
		} else if (selectedTransition != null) {
			sfc.transitions.remove(selectedTransition);
			selectedTransition = null;
		}
		pendingStep = null;
		pendingTransition = null;
	}

	// ---- for the game test: where a click must land, in GUI pixels ------------------------------------------------------------------------

	int[] stepPin(String name, boolean bottom, int left, int top) {
		for (Sfc.Step step : sfc.steps) {
			if (step.name.equalsIgnoreCase(name)) {
				scrollX = Math.max(0, step.x - 20);   // panned into view, as a player would
				scrollY = Math.max(0, step.y - 30);
				return new int[] {left + 4 - scrollX + step.x + W / 2, top + 4 - scrollY + step.y + (bottom ? STEP_H : 0)};
			}
		}
		return null;
	}

	int[] transitionPin(int index, boolean bottom, int left, int top) {
		Sfc.Transition t = sfc.transitions.get(index);
		scrollX = Math.max(0, t.x - 20);
		scrollY = Math.max(0, t.y - 30);
		return new int[] {left + 4 - scrollX + t.x + W / 2, top + 4 - scrollY + t.y + (bottom ? TRANS_H : 0)};
	}
}
