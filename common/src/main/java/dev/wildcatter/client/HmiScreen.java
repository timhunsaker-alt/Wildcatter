package dev.wildcatter.client;

import dev.wildcatter.control.PlcNet;
import dev.wildcatter.platform.Platform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;

/**
 * An operator's HMI screen, in the ISA-101 manner: greys for normal, colour kept for what is abnormal. RUN shows the layout; EDIT lays it out from tags;
 * ALARMS holds the live list, its acknowledgements and shelving, and the history (docs/multiblocks/plc.md).
 */
public class HmiScreen extends Screen {
	private enum Tab {
		RUN, EDIT, ALARMS
	}

	private static final List<String> KINDS = List.of("VALUE", "BAR", "LAMP", "BUTTON", "MOTOR", "VALVE", "TREND");
	private static final int BACK = 0xF0303438;
	private static final int PANEL = 0xFF3C4147;
	private static final int TEXT = 0xFFE4E6E8;
	private static final int MUTED = 0xFFA0A6AC;
	private static final int FILL = 0xFF7E868E;
	private static final int ALARM = 0xFFE05050;
	private static final int WARN = 0xFFE0B040;
	private static final int TOP = 24;

	private final int x;
	private final int y;
	private final int z;
	private final List<String> layout = new ArrayList<>();
	private final List<String> alarms = new ArrayList<>();
	private final List<String> values = new ArrayList<>();
	private final List<String> states = new ArrayList<>();
	private final List<String> history = new ArrayList<>();
	private final Map<String, float[]> trends = new HashMap<>();
	private final List<String> notes = new ArrayList<>();
	private int plcs;
	private Tab tab = Tab.RUN;
	private String kind = "VALUE";
	private int selected = -1;
	private int dragging = -1;
	private EditBox label;
	private EditBox tag;
	private EditBox low;
	private EditBox high;
	private EditBox alarmTag;
	private EditBox alarmOp;
	private EditBox alarmLimit;
	private EditBox alarmPriority;
	private EditBox alarmText;

	private HmiScreen(CompoundTag state) {
		super(Component.literal("HMI"));
		x = state.getIntOr("x", 0);
		y = state.getIntOr("y", 0);
		z = state.getIntOr("z", 0);
		read(state, true);
	}

	public static void fromServer(CompoundTag state) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gui.screen() instanceof HmiScreen open && open.x == state.getIntOr("x", 0) && open.y == state.getIntOr("y", 0) && open.z == state.getIntOr("z", 0)) {
			open.read(state, false);
		} else if (state.getBooleanOr("open", false)) {
			minecraft.gui.setScreen(new HmiScreen(state));
		}
	}

	private static void strings(ListTag from, List<String> into) {
		into.clear();
		from.forEach(t -> t.asString().ifPresent(into::add));
	}

	private void read(CompoundTag state, boolean all) {
		if (all) {
			strings(state.getListOrEmpty("layout"), layout);
			strings(state.getListOrEmpty("alarms"), alarms);
		}
		strings(state.getListOrEmpty("values"), values);
		strings(state.getListOrEmpty("states"), states);
		strings(state.getListOrEmpty("history"), history);
		plcs = state.getIntOr("plcs", 0);
		trends.clear();
		CompoundTag saved = state.getCompoundOrEmpty("trends");
		for (String name : saved.keySet()) {
			String[] parts = saved.getStringOr(name, "").split(",");
			float[] samples = new float[parts.length];
			for (int i = 0; i < parts.length; i++) {
				try {
					samples[i] = parts[i].isEmpty() ? Float.NaN : Float.parseFloat(parts[i]);
				} catch (NumberFormatException e) {
					samples[i] = Float.NaN;
				}
			}
			trends.put(name, samples);
		}
		state.getListOrEmpty("messages").forEach(t -> t.asString().ifPresent(notes::add));
	}

	private void ask(String action, Consumer<CompoundTag> fill) {
		CompoundTag out = new CompoundTag();
		out.putInt("x", x);
		out.putInt("y", y);
		out.putInt("z", z);
		out.putString("action", action);
		fill.accept(out);
		Platform.INSTANCE.sendToServer(new PlcNet.Payload(out));
	}

	private void save() {
		ask("save", t -> {
			t.put("layout", list(layout));
			t.put("alarms", list(alarms));
		});
	}

	private static ListTag list(List<String> from) {
		ListTag out = new ListTag();
		from.forEach(s -> out.add(StringTag.valueOf(s)));
		return out;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		ask("close", t -> {
		});
		super.onClose();
	}

	private Button button(int bx, int by, int w, String text, Runnable press) {
		return addRenderableWidget(Button.builder(Component.literal(text), b -> press.run()).bounds(bx, by, w, 16).build());
	}

	private EditBox field(int bx, int by, int w, String name, String hint) {
		EditBox box = addRenderableWidget(new EditBox(font, bx, by, w, 14, Component.literal(name)));
		box.setHint(Component.literal(hint));
		box.setMaxLength(128);
		return box;
	}

	@Override
	protected void init() {
		int bx = 8;
		for (Tab each : Tab.values()) {
			Button b = button(bx, 4, 44, each.name(), () -> {
				tab = each;
				selected = -1;
				rebuildWidgets();
			});
			b.active = each != tab;
			bx += 46;
		}
		int side = width - 112;
		if (tab == Tab.EDIT) {
			button(side, TOP, 100, kind, () -> {
				kind = KINDS.get((KINDS.indexOf(kind) + 1) % KINDS.size());
				rebuildWidgets();
			});
			String[] row = selected >= 0 && selected < layout.size() ? row(selected) : null;
			label = field(side, TOP + 28, 100, "label", "LT-101");
			tag = field(side, TOP + 54, 100, "tag", "LEVEL.PV");
			low = field(side, TOP + 80, 48, "low", "0");
			high = field(side + 52, TOP + 80, 48, "high", "100");
			if (row != null) {
				label.setValue(row[1]);
				tag.setValue(row[2]);
				low.setValue(row[3]);
				high.setValue(row[4]);
			}
			button(side, TOP + 98, 49, "ADD", this::addWidget);
			button(side + 51, TOP + 98, 49, "DEL", this::deleteWidget);
		} else if (tab == Tab.ALARMS) {
			alarmTag = field(side, TOP + 10, 100, "alarm tag", "LEVEL.PV");
			alarmOp = field(side, TOP + 36, 30, "op", ">");
			alarmLimit = field(side + 32, TOP + 36, 40, "limit", "90");
			alarmPriority = field(side + 74, TOP + 36, 26, "pri", "1");
			alarmText = field(side, TOP + 62, 100, "message", "Tank high");
			button(side, TOP + 80, 100, "ADD ALARM", this::addAlarm);
			for (int i = 0; i < alarms.size() && i < 8; i++) {
				int index = i;
				button(side - 88, TOP + 2 + i * 18, 36, "ACK", () -> ask("ack", t -> t.putInt("index", index)));
				button(side - 50, TOP + 2 + i * 18, 44, "SHELVE", () -> ask("shelve", t -> t.putInt("index", index)));
			}
		}
	}

	private String[] row(int index) {
		String[] out = layout.get(index).split("\\|", -1);
		return out.length >= 7 ? out : java.util.Arrays.copyOf(out, 7);
	}

	private void addWidget() {
		int at = layout.size();
		int wx = 8 + at % 3 * 94;
		int wy = TOP + at / 3 * 40;
		layout.add(String.join("|", kind, label.getValue().replace("|", " "), tag.getValue().replace("|", " "), low.getValue().isEmpty() ? "0" : low.getValue(), high.getValue().isEmpty() ? "100" : high.getValue(), Integer.toString(wx), Integer.toString(wy)));
		save();
		rebuildWidgets();
	}

	private void deleteWidget() {
		if (selected >= 0 && selected < layout.size()) {
			layout.remove(selected);
			selected = -1;
			save();
			rebuildWidgets();
		}
	}

	private void addAlarm() {
		alarms.add(String.join("|", alarmTag.getValue().replace("|", " "), alarmOp.getValue().isEmpty() ? ">" : alarmOp.getValue(), alarmLimit.getValue(), alarmPriority.getValue().isEmpty() ? "2" : alarmPriority.getValue(), alarmText.getValue().replace("|", " ")));
		save();
		rebuildWidgets();
	}

	private static double parse(String text, double otherwise) {
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException | NullPointerException e) {
			return otherwise;
		}
	}

	private String value(int index) {
		return index < values.size() ? values.get(index) : "?";
	}

	private int widgetHeight(String[] row) {
		return "TREND".equals(row[0]) ? 76 : 36;
	}

	private int widgetWidth(String[] row) {
		return "TREND".equals(row[0]) ? 182 : 88;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		graphics.fill(0, 0, width, height, BACK);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		String head = plcs == 0 ? "NO PLC ON THE NETWORK" : plcs + " PLC(s)";
		graphics.text(font, head, width - 8 - font.width(head), 8, plcs == 0 ? ALARM : MUTED, false);
		long up = states.stream().filter(s -> s.equals("UNACKED") || s.equals("RETURNED_UNACKED")).count();
		if (up > 0) {
			graphics.text(font, up + " ALARM(S)", 8 + 3 * 46 + 8, 8, ALARM, false);
		}
		if (tab == Tab.ALARMS) {
			drawAlarms(graphics);
		} else {
			drawLayout(graphics);
			if (tab == Tab.EDIT) {
				graphics.fill(width - 116, TOP - 2, width, height, PANEL);
				for (var child : children()) {
					if (child instanceof EditBox box) {
						graphics.text(font, box.getMessage().getString(), box.getX(), box.getY() - 9, MUTED, false);
					}
				}
			}
		}
		for (int i = Math.max(0, notes.size() - 2); i < notes.size(); i++) {
			graphics.text(font, notes.get(i), 8, height - 12 - (notes.size() - 1 - i) * 10, WARN, false);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partial);
	}

	private void drawLayout(GuiGraphicsExtractor g) {
		for (int i = 0; i < layout.size(); i++) {
			String[] row = row(i);
			int wx = (int) parse(row[5], 8);
			int wy = (int) parse(row[6], TOP);
			int w = widgetWidth(row);
			int h = widgetHeight(row);
			g.fill(wx, wy, wx + w, wy + h, i == selected ? 0xFF4A5A70 : PANEL);
			g.text(font, font.plainSubstrByWidth(row[1] == null ? "" : row[1], w - 6), wx + 3, wy + 2, MUTED, false);
			String value = value(i);
			double number = value.equals("true") ? 1 : value.equals("false") ? 0 : parse(value, Double.NaN);
			double lo = parse(row[3], 0);
			double hi = parse(row[4], 100);
			switch (row[0]) {
				case "VALUE" -> g.text(font, Double.isNaN(number) ? value : String.format(Locale.ROOT, "%.1f", number), wx + 3, wy + 16, value.equals("?") ? ALARM : TEXT, false);
				case "BAR" -> {
					g.fill(wx + 3, wy + 14, wx + w - 3, wy + 22, 0xFF24282C);
					double share = Double.isNaN(number) || hi == lo ? 0 : Math.max(0, Math.min(1, (number - lo) / (hi - lo)));
					g.fill(wx + 3, wy + 14, wx + 3 + (int) ((w - 6) * share), wy + 22, FILL);
					g.text(font, Double.isNaN(number) ? "?" : String.format(Locale.ROOT, "%.1f", number), wx + 3, wy + 25, TEXT, false);
				}
				case "LAMP" -> {
					g.fill(wx + 3, wy + 14, wx + 19, wy + 30, number > 0 ? TEXT : 0xFF24282C);
					g.text(font, number > 0 ? "ON" : "OFF", wx + 24, wy + 18, TEXT, false);
				}
				case "BUTTON" -> {
					g.fill(wx + 3, wy + 13, wx + w - 3, wy + 32, number > 0 ? 0xFF8A929A : 0xFF596068);
					g.text(font, number > 0 ? "ON" : "OFF", wx + w / 2 - 8, wy + 18, TEXT, false);
				}
				case "MOTOR", "VALVE" -> {
					// the state a device block reports: 2 running or open, 4 fault, 5 not in AUTO
					int state = (int) number;
					boolean fault = state == 4;
					g.fill(wx + 4, wy + 14, wx + 20, wy + 30, fault ? ALARM : state == 2 ? 0xFFC8CCD0 : state == 5 ? WARN : 0xFF596068);
					String[] names = row[0].equals("MOTOR") ? new String[] {"STOPPED", "STARTING", "RUNNING", "STOPPING", "FAULT", "NOT AUTO"} : new String[] {"CLOSED", "OPENING", "OPEN", "CLOSING", "FAULT", "NOT AUTO"};
					g.text(font, state >= 0 && state < names.length && !Double.isNaN(number) ? names[state] : "?", wx + 24, wy + 18, fault ? ALARM : TEXT, false);
				}
				case "TREND" -> {
					int top = wy + 12;
					int bottom = wy + h - 3;
					g.fill(wx + 3, top, wx + w - 3, bottom, 0xFF24282C);
					float[] samples = trends.get(row[2] == null ? "" : row[2].toUpperCase(Locale.ROOT));
					if (samples != null && samples.length > 1 && hi != lo) {
						int span = w - 6;
						int lastY = -1;
						for (int s = 0; s < span; s++) {
							int at = samples.length - span + s;
							if (at >= 0 && !Float.isNaN(samples[at])) {
								double share = Math.max(0, Math.min(1, (samples[at] - lo) / (hi - lo)));
								int py = bottom - 1 - (int) ((bottom - top - 2) * share);
								int from = lastY < 0 ? py : lastY;
								g.fill(wx + 3 + s, Math.min(py, from), wx + 4 + s, Math.max(py, from) + 1, TEXT);
								lastY = py;
							}
						}
					}
					g.text(font, Double.isNaN(number) ? "?" : String.format(Locale.ROOT, "%.1f", number), wx + w - 40, wy + 2, TEXT, false);
				}
				default -> {
				}
			}
		}
		if (layout.isEmpty()) {
			g.text(font, tab == Tab.EDIT ? "Choose a kind, fill it in, and ADD." : "Nothing here yet: lay out this screen under EDIT.", 8, TOP + 4, MUTED, false);
		}
	}

	private void drawAlarms(GuiGraphicsExtractor g) {
		int split = width - 112;
		g.fill(8, TOP, split - 90, height - 8, PANEL);
		for (var child : children()) {
			if (child instanceof EditBox box) {
				g.text(font, box.getMessage().getString(), box.getX(), box.getY() - 9, MUTED, false);
			}
		}
		for (int i = 0; i < alarms.size() && i < 8; i++) {
			String[] row = alarms.get(i).split("\\|", -1);
			String state = i < states.size() ? states.get(i) : "NORMAL";
			int colour = state.equals("UNACKED") ? ALARM : state.equals("ACKED") ? WARN : state.equals("RETURNED_UNACKED") ? 0xFFE09050 : state.equals("SHELVED") ? 0xFF8090C0 : MUTED;
			String shown = (row.length > 3 ? "P" + row[3] + " " : "") + (row.length > 4 ? row[4] : "") + "  " + state;
			g.text(font, font.plainSubstrByWidth(shown, split - 104), 12, TOP + 6 + i * 18, colour, false);
		}
		int rowY = TOP + 8 + Math.min(alarms.size(), 8) * 18;
		g.text(font, "History", 12, rowY, MUTED, false);
		rowY += 11;
		for (String event : history) {
			if (rowY > height - 18) {
				break;
			}
			g.text(font, font.plainSubstrByWidth(event, split - 104), 12, rowY, TEXT, false);
			rowY += 10;
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		if (super.mouseClicked(event, doubled)) {
			return true;
		}
		int mx = (int) event.x();
		int my = (int) event.y();
		for (int i = layout.size() - 1; i >= 0 && tab != Tab.ALARMS; i--) {
			String[] row = row(i);
			int wx = (int) parse(row[5], 8);
			int wy = (int) parse(row[6], TOP);
			if (mx >= wx && mx < wx + widgetWidth(row) && my >= wy && my < wy + widgetHeight(row)) {
				if (tab == Tab.EDIT) {
					selected = i;
					dragging = i;
					rebuildWidgets();
				} else if ("BUTTON".equals(row[0])) {
					boolean on = value(i).equals("true");
					ask("write", t -> {
						t.putString("tag", row[2]);
						t.putString("value", on ? "FALSE" : "TRUE");
					});
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging >= 0 && dragging < layout.size()) {
			String[] row = row(dragging);
			row[5] = Integer.toString(Math.max(0, (int) event.x() - 20) / 4 * 4);
			row[6] = Integer.toString(Math.max(TOP, (int) event.y() - 6) / 4 * 4);
			layout.set(dragging, String.join("|", row));
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging >= 0) {
			dragging = -1;
			save();
		}
		return super.mouseReleased(event);
	}

	// ---- for the game test ----------------------------------------------------------------------------------------------------------------

	public int[] buttonForTest(String text) {
		for (var child : children()) {
			if (child instanceof Button b && b.getMessage().getString().equals(text) && b.visible && b.active) {
				return new int[] {b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
			}
		}
		return null;
	}

	public boolean setFieldForTest(String name, String value) {
		for (var child : children()) {
			if (child instanceof EditBox box && box.getMessage().getString().equals(name)) {
				box.setValue(value);
				return true;
			}
		}
		return false;
	}

	public int[] widgetForTest(int index) {
		String[] row = row(index);
		return new int[] {(int) parse(row[5], 8) + 20, (int) parse(row[6], TOP) + 20};
	}

	public List<String> valuesForTest() {
		return values;
	}

	public List<String> statesForTest() {
		return states;
	}

	public void kindForTest(String next) {
		kind = next;
		rebuildWidgets();
	}

	public int layoutSizeForTest() {
		return layout.size();
	}
}
