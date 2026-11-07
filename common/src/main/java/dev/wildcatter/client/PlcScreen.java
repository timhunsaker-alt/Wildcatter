package dev.wildcatter.client;

import dev.wildcatter.control.PlcNet;
import dev.wildcatter.platform.Platform;
import dev.wildcatter.plc.CpuSpec;
import dev.wildcatter.plc.TextBuffer;
import dev.wildcatter.plc.lang.CompileError;
import dev.wildcatter.plc.lang.PlcCompiler;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;

/**
 * The engineering station at a PLC cabinet: the project in a real editor, the terminals, online values and forces, and the fault buffer
 * (docs/multiblocks/plc.md). The game keeps running behind it, as a plant does.
 */
public class PlcScreen extends Screen {
	private enum Tab {
		CODE, TERMINALS, ONLINE, DIAGNOSTICS
	}

	private static final int BACK = 0xF01B1E24;
	private static final int PANEL = 0xFF23272F;
	private static final int TEXT = 0xFFD6DAE0;
	private static final int MUTED = 0xFF8B93A0;
	private static final int KEYWORD = 0xFF7FB2F0;
	private static final int TYPE = 0xFF6FD0C0;
	private static final int NUMBER = 0xFFE0B070;
	private static final int STRING = 0xFFA8D080;
	private static final int COMMENT = 0xFF6F7884;
	private static final int ADDRESS = 0xFFD8A0E0;
	private static final int ERROR = 0xFFF07070;
	private static final int GOOD = 0xFF70D080;
	private static final int SELECT = 0xFF34507A;
	private static final int ROW = 11;
	private static final int TOP = 42;
	private static final int ERROR_ROWS = 5;
	private static final Set<String> KEYWORDS = Set.of("PROGRAM", "END_PROGRAM", "FUNCTION", "END_FUNCTION", "FUNCTION_BLOCK", "END_FUNCTION_BLOCK", "VAR", "VAR_INPUT", "VAR_OUTPUT", "VAR_IN_OUT",
			"VAR_TEMP", "VAR_GLOBAL", "VAR_EXTERNAL", "END_VAR", "CONSTANT", "RETAIN", "AT", "IF", "THEN", "ELSIF", "ELSE", "END_IF", "CASE", "OF", "END_CASE", "FOR", "TO", "BY", "DO", "END_FOR", "WHILE",
			"END_WHILE", "REPEAT", "UNTIL", "END_REPEAT", "EXIT", "CONTINUE", "RETURN", "AND", "OR", "XOR", "NOT", "MOD", "TRUE", "FALSE", "TYPE", "END_TYPE", "STRUCT", "END_STRUCT", "ARRAY", "CONFIGURATION",
			"END_CONFIGURATION", "RESOURCE", "END_RESOURCE", "ON", "TASK", "WITH");
	private static final Set<String> TYPES = Set.of("BOOL", "SINT", "USINT", "INT", "UINT", "DINT", "UDINT", "LINT", "BYTE", "WORD", "DWORD", "LWORD", "REAL", "LREAL", "TIME", "STRING", "TON", "TOF", "TP",
			"CTU", "CTD", "CTUD", "R_TRIG", "F_TRIG", "SR", "RS", "PID", "SCALE", "ALARM_HL", "TOTALIZER");

	private final int x;
	private final int y;
	private final int z;
	private final TextBuffer code;
	private final List<String> tagNames = new ArrayList<>();
	private final List<String> watched = new ArrayList<>();
	private Tab tab = Tab.CODE;
	private CompoundTag state;
	private List<CompileError> errors = List.of();
	private final List<String> notes = new ArrayList<>();
	private int scrollLine;
	private int scrollList;
	private int selectedDevice = -1;
	private int selectedPoint = -1;
	private int selectedTag = -1;
	private boolean dragging;
	private int blink;
	private EditBox fieldOne;
	private EditBox fieldTwo;

	private PlcScreen(CompoundTag tag) {
		super(Component.literal("PLC cabinet"));
		x = tag.getIntOr("x", 0);
		y = tag.getIntOr("y", 0);
		z = tag.getIntOr("z", 0);
		code = new TextBuffer(tag.getStringOr("source", ""));
		tag.getListOrEmpty("names").forEach(t -> t.asString().ifPresent(tagNames::add));
		state = tag;
	}

	/** From the cabinet: opens the screen, or updates the one open on it. */
	public static void fromServer(CompoundTag tag) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gui.screen() instanceof PlcScreen open && open.x == tag.getIntOr("x", 0) && open.y == tag.getIntOr("y", 0) && open.z == tag.getIntOr("z", 0)) {
			open.state = tag;
			tag.getListOrEmpty("messages").forEach(t -> t.asString().ifPresent(open.notes::add));
		} else if (tag.getBooleanOr("open", false)) {
			minecraft.gui.setScreen(new PlcScreen(tag));
		}
	}

	private void ask(String action, Consumer<CompoundTag> fill) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("x", x);
		tag.putInt("y", y);
		tag.putInt("z", z);
		tag.putString("action", action);
		fill.accept(tag);
		Platform.INSTANCE.sendToServer(new PlcNet.Payload(tag));
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

	private Button button(int bx, int by, int w, String label, Runnable press) {
		return addRenderableWidget(Button.builder(Component.literal(label), b -> press.run()).bounds(bx, by, w, 16).build());
	}

	@Override
	protected void init() {
		fieldOne = null;
		fieldTwo = null;
		int bx = 8;
		for (Tab each : Tab.values()) {
			int w = font.width(each.name()) + 12;
			Button b = button(bx, 22, w, each.name(), () -> {
				tab = each;
				scrollList = 0;
				rebuildWidgets();
			});
			b.active = each != tab;
			bx += w + 2;
		}
		int right = width - 8;
		button(right - 44, 22, 44, "COLD", () -> ask("cold", t -> {
		}));
		button(right - 88, 22, 42, "STOP", () -> ask("key", t -> t.putBoolean("run", false)));
		button(right - 130, 22, 40, "RUN", () -> ask("key", t -> t.putBoolean("run", true)));
		if (tab == Tab.CODE) {
			button(right - 64, height - 26, 60, "DOWNLOAD", this::download);   // in the corner of the message pane: the top row is full at a small window
			button(right - 108, height - 26, 42, "CHECK", this::check);
		} else if (tab == Tab.TERMINALS) {
			fieldOne = addRenderableWidget(new EditBox(font, width - 208, TOP + 14, 140, 16, Component.literal("address")));
			fieldOne.setHint(Component.literal("%IX0.0"));
			button(width - 64, TOP + 14, 56, "LAND", () -> landing(fieldOne.getValue()));
			button(width - 64, TOP + 32, 56, "CLEAR", () -> landing(""));
			fieldTwo = addRenderableWidget(new EditBox(font, width - 208, TOP + 66, 140, 16, Component.literal("tag")));
			fieldTwo.setHint(Component.literal("PB-101"));
			button(width - 64, TOP + 66, 56, "SET TAG", () -> {
				CompoundTag device = device(selectedDevice);
				if (device != null) {
					ask("tag", t -> {
						copyPos(device, t);
						t.putString("tag", fieldTwo.getValue());
					});
				}
			});
		} else if (tab == Tab.ONLINE) {
			fieldOne = addRenderableWidget(new EditBox(font, width - 208, TOP + 14, 200, 16, Component.literal("tag or address")));
			fieldOne.setHint(Component.literal("MAIN.PUMP or %IX0.0"));
			fieldOne.setMaxLength(128);
			fieldTwo = addRenderableWidget(new EditBox(font, width - 208, TOP + 50, 200, 16, Component.literal("value")));
			fieldTwo.setHint(Component.literal("TRUE, 42, 1.5"));
			button(width - 208, TOP + 70, 48, "WATCH", () -> {
				String name = fieldOne.getValue().toUpperCase(Locale.ROOT);
				if (!name.isBlank() && !watched.contains(name)) {
					watched.add(name);
					sendWatch();
				}
			});
			button(width - 158, TOP + 70, 48, "WRITE", () -> ask("write", t -> {
				t.putString("tag", fieldOne.getValue());
				t.putString("value", fieldTwo.getValue());
			}));
			button(width - 108, TOP + 70, 48, "FORCE", () -> ask("force", t -> {
				t.putString("tag", fieldOne.getValue());
				t.putString("value", fieldTwo.getValue());
			}));
			button(width - 58, TOP + 70, 50, "RELEASE", () -> ask("release", t -> t.putString("tag", fieldOne.getValue())));
			sendWatch();
		}
	}

	private void sendWatch() {
		ask("watch", t -> {
			ListTag names = new ListTag();
			watched.forEach(n -> names.add(StringTag.valueOf(n)));
			t.put("names", names);
		});
	}

	private void landing(String address) {
		CompoundTag device = device(selectedDevice);
		if (device != null && selectedPoint >= 0) {
			ask("land", t -> {
				copyPos(device, t);
				t.putInt("point", selectedPoint);
				t.putString("address", address);
			});
		}
	}

	private static void copyPos(CompoundTag device, CompoundTag to) {
		to.putInt("dx", device.getIntOr("x", 0));
		to.putInt("dy", device.getIntOr("y", 0));
		to.putInt("dz", device.getIntOr("z", 0));
	}

	private CompoundTag device(int index) {
		ListTag devices = state.getListOrEmpty("devices");
		return index >= 0 && index < devices.size() ? devices.getCompoundOrEmpty(index) : null;
	}

	/** Compiles here, touching nothing on the CPU. */
	public void check() {
		CpuSpec spec = state.getStringOr("cpu", "").equals(CpuSpec.LARGE.name()) ? CpuSpec.LARGE : CpuSpec.SMALL;
		errors = PlcCompiler.compile(spec, code.text()).errors();
		notes.clear();
		notes.add(errors.isEmpty() ? "No errors." : errors.size() + " error(s).");
	}

	/** Sends the project to the cabinet, if it compiles here too. */
	public void download() {
		check();
		if (errors.isEmpty()) {
			notes.clear();
			ask("download", t -> t.putString("source", code.text()));
		}
	}

	/** For the game test: what a paste does. */
	public void replaceForTest(String text) {
		code.setText(text);
	}

	public List<CompileError> errorsForTest() {
		return errors;
	}

	public void showTabForTest(String name) {
		tab = Tab.valueOf(name);
		rebuildWidgets();
	}

	// ---- drawing ----------------------------------------------------------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		graphics.fill(0, 0, width, height, BACK);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		super.extractRenderState(graphics, mouseX, mouseY, partial);
		String status = state.getStringOr("status", "");
		int colour = status.startsWith("RUN") ? GOOD : status.startsWith("STOP") ? 0xFFE0C060 : ERROR;
		graphics.text(font, state.getStringOr("cpu", "No CPU"), 8, 8, TEXT, false);
		graphics.text(font, status, 8 + font.width(state.getStringOr("cpu", "No CPU")) + 12, 8, colour, false);
		if (state.getIntOr("budget", 0) > 0 && status.startsWith("RUN")) {
			String load = String.format("scan %d / %d instructions", state.getIntOr("instructions", 0), state.getIntOr("budget", 0));
			graphics.text(font, load, width - 8 - font.width(load), 8, MUTED, false);
		}
		switch (tab) {
			case CODE -> drawCode(graphics);
			case TERMINALS -> drawTerminals(graphics);
			case ONLINE -> drawOnline(graphics);
			case DIAGNOSTICS -> drawDiagnostics(graphics);
		}
	}

	private int codeBottom() {
		return height - 8 - ERROR_ROWS * ROW - 4;
	}

	private int codeRows() {
		return (codeBottom() - TOP - 4) / ROW;
	}

	private static String expand(String text) {
		return text.replace("\t", "    ");
	}

	private int gutter() {
		return 8 + font.width("0000") + 8;
	}

	private void drawCode(GuiGraphicsExtractor graphics) {
		int bottom = codeBottom();
		graphics.fill(8, TOP, width - 8, bottom, PANEL);
		graphics.enableScissor(8, TOP, width - 8, bottom);
		int rows = codeRows();
		int[] selection = code.selection();
		for (int i = 0; i < rows && scrollLine + i < code.lineCount(); i++) {
			int at = scrollLine + i;
			int rowY = TOP + 2 + i * ROW;
			String row = code.line(at);
			boolean bad = errors.stream().anyMatch(e -> e.line() == at + 1);
			graphics.text(font, Integer.toString(at + 1), 8 + 2, rowY, bad ? ERROR : COMMENT, false);
			if (selection != null && at >= selection[0] && at <= selection[2]) {
				int from = at == selection[0] ? selection[1] : 0;
				int to = at == selection[2] ? selection[3] : row.length();
				int left = gutter() + font.width(expand(row.substring(0, from)));
				int right = gutter() + font.width(expand(row.substring(0, to))) + (at < selection[2] ? 4 : 0);
				graphics.fill(left, rowY - 1, right, rowY + ROW - 1, SELECT);
			}
			drawLine(graphics, row, gutter(), rowY);
			if (at == code.caretLine() && blink / 10 % 2 == 0) {
				int cx = gutter() + font.width(expand(row.substring(0, code.caretColumn())));
				graphics.fill(cx, rowY - 1, cx + 1, rowY + ROW - 2, TEXT);
			}
		}
		graphics.disableScissor();
		int noteY = bottom + 4;
		graphics.fill(8, bottom + 2, width - 8, height - 6, PANEL);
		for (CompileError error : errors.stream().limit(ERROR_ROWS).toList()) {
			graphics.text(font, error.toString(), 12, noteY, ERROR, false);
			noteY += ROW;
		}
		String compile = state.getStringOr("compile", "");
		for (String note : notes) {
			if (noteY < height - 8) {
				graphics.text(font, note, 12, noteY, note.startsWith("Download refused") || note.startsWith("line") ? ERROR : GOOD, false);
				noteY += ROW;
			}
		}
		if (errors.isEmpty() && notes.isEmpty() && !compile.isEmpty()) {
			graphics.text(font, compile.lines().findFirst().orElse(""), 12, noteY, ERROR, false);
		}
	}

	/** One line of code, coloured by token. */
	private void drawLine(GuiGraphicsExtractor graphics, String row, int startX, int rowY) {
		int cx = startX;
		int i = 0;
		boolean comment = false;
		while (i < row.length()) {
			int from = i;
			char c = row.charAt(i);
			int colour = TEXT;
			if (comment || row.startsWith("//", i) || row.startsWith("(*", i)) {
				int end = row.indexOf("*)", i);
				i = row.startsWith("//", i) || end < 0 ? row.length() : end + 2;
				colour = COMMENT;
				comment = false;
			} else if (c == '\'') {
				i++;
				while (i < row.length() && row.charAt(i) != '\'') {
					i++;
				}
				i = Math.min(row.length(), i + 1);
				colour = STRING;
			} else if (c == '%') {
				i++;
				while (i < row.length() && (Character.isLetterOrDigit(row.charAt(i)) || row.charAt(i) == '.')) {
					i++;
				}
				colour = ADDRESS;
			} else if (Character.isDigit(c)) {
				while (i < row.length() && (Character.isLetterOrDigit(row.charAt(i)) || row.charAt(i) == '#' || row.charAt(i) == '.' || row.charAt(i) == '_')) {
					i++;
				}
				colour = NUMBER;
			} else if (Character.isLetter(c) || c == '_') {
				while (i < row.length() && (Character.isLetterOrDigit(row.charAt(i)) || row.charAt(i) == '_')) {
					i++;
				}
				String word = row.substring(from, i).toUpperCase(Locale.ROOT);
				if (i < row.length() && row.charAt(i) == '#') {
					while (i < row.length() && !Character.isWhitespace(row.charAt(i)) && ";,)".indexOf(row.charAt(i)) < 0) {
						i++;
					}
					colour = NUMBER;
				} else {
					colour = KEYWORDS.contains(word) ? KEYWORD : TYPES.contains(word) ? TYPE : TEXT;
				}
			} else {
				i++;
			}
			String piece = expand(row.substring(from, i));
			graphics.text(font, piece, cx, rowY, colour, false);
			cx += font.width(piece);
		}
	}

	private void drawTerminals(GuiGraphicsExtractor graphics) {
		int split = width - 216;
		graphics.fill(8, TOP, split, height - 8, PANEL);
		graphics.text(font, "Rail: " + state.getStringOr("rail", ""), 12, TOP + 3, MUTED, false);
		graphics.enableScissor(8, TOP + 14, split, height - 8);
		int rowY = TOP + 16 - scrollList * ROW;
		ListTag devices = state.getListOrEmpty("devices");
		if (devices.isEmpty()) {
			graphics.text(font, "No devices reached. Run control cable from the cabinet to them.", 12, TOP + 16, MUTED, false);
		}
		for (int d = 0; d < devices.size(); d++) {
			CompoundTag device = devices.getCompoundOrEmpty(d);
			String tagged = device.getStringOr("tag", "");
			graphics.text(font, (tagged.isEmpty() ? "" : tagged + "  ") + device.getStringOr("name", "") + "  (" + device.getIntOr("x", 0) + ", " + device.getIntOr("y", 0) + ", " + device.getIntOr("z", 0) + ")", 12, rowY, TYPE, false);
			rowY += ROW;
			ListTag points = device.getListOrEmpty("points");
			for (int p = 0; p < points.size(); p++) {
				CompoundTag point = points.getCompoundOrEmpty(p);
				if (d == selectedDevice && p == selectedPoint) {
					graphics.fill(10, rowY - 1, split - 2, rowY + ROW - 1, SELECT);
				}
				graphics.text(font, font.plainSubstrByWidth((point.getBooleanOr("output", false) ? "out  " : "in   ") + point.getStringOr("name", ""), split - 76 - 24 - 4), 24, rowY, TEXT, false);
				String address = point.getStringOr("address", "");
				graphics.text(font, address.isEmpty() ? "not landed" : address, split - 76, rowY, address.isEmpty() ? MUTED : ADDRESS, false);
				rowY += ROW;
			}
			rowY += 3;
		}
		graphics.disableScissor();
		graphics.text(font, "Land the selected signal on:", width - 208, TOP + 3, TEXT, false);
		graphics.text(font, "The selected device's tag:", width - 208, TOP + 55, TEXT, false);
		drawNotes(graphics, width - 208, TOP + 90);
	}

	private void drawNotes(GuiGraphicsExtractor graphics, int nx, int ny) {
		int rowY = ny;
		for (String note : notes.subList(Math.max(0, notes.size() - 6), notes.size())) {
			for (var line : font.split(Component.literal(note), width - nx - 8)) {
				graphics.text(font, line, nx, rowY, 0xFFE0C060, false);
				rowY += ROW;
			}
		}
	}

	private void drawOnline(GuiGraphicsExtractor graphics) {
		int split = width - 216;
		graphics.fill(8, TOP, split, height - 8, PANEL);
		graphics.text(font, "Watched tags: click a name on the right, then WATCH", 12, TOP + 3, MUTED, false);
		ListTag values = state.getListOrEmpty("values");
		int rowY = TOP + 16;
		for (int i = 0; i < values.size(); i++) {
			String value = values.getStringOr(i, "");
			int cut = value.indexOf('=');
			graphics.text(font, value.substring(0, Math.max(cut, 0)), 12, rowY, TEXT, false);
			graphics.text(font, value.substring(cut + 1), split - 140, rowY, value.contains("FORCED") ? ERROR : NUMBER, false);
			rowY += ROW;
		}
		graphics.text(font, "Tag or address:", width - 208, TOP + 3, TEXT, false);
		graphics.text(font, "Value:", width - 208, TOP + 39, TEXT, false);
		int listTop = TOP + 92;
		String filter = fieldOne == null ? "" : fieldOne.getValue().toUpperCase(Locale.ROOT);
		int row = 0;
		graphics.enableScissor(width - 208, listTop, width - 8, height - 8);
		for (String name : tagNames) {
			if (name.contains(filter)) {
				if (row >= scrollList) {
					int drawY = listTop + (row - scrollList) * ROW;
					graphics.text(font, name, width - 206, drawY, row == selectedTag ? ADDRESS : MUTED, false);
				}
				row++;
			}
		}
		graphics.disableScissor();
		if (state.getBooleanOr("forcing", false)) {
			graphics.text(font, "FORCE ON", split - 60, TOP + 3, ERROR, false);
		}
	}

	private void drawDiagnostics(GuiGraphicsExtractor graphics) {
		graphics.fill(8, TOP, width - 8, height - 8, PANEL);
		ListTag lines = state.getListOrEmpty("diagnostics");
		int rowY = TOP + 3;
		for (int i = lines.size() - 1; i >= 0 && rowY < height - 16; i--) {
			String line = lines.getStringOr(i, "");
			graphics.text(font, line, 12, rowY, line.contains("FAULT") ? ERROR : TEXT, false);
			rowY += ROW;
		}
	}

	// ---- input ------------------------------------------------------------------------------------------------------------------------

	@Override
	public void tick() {
		blink++;
	}

	private boolean editing() {
		return tab == Tab.CODE;
	}

	private void showCaret() {
		int rows = codeRows();
		if (code.caretLine() < scrollLine) {
			scrollLine = code.caretLine();
		} else if (code.caretLine() >= scrollLine + rows) {
			scrollLine = code.caretLine() - rows + 1;
		}
		blink = 0;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!editing() || event.key() == 256) {
			return super.keyPressed(event);
		}
		boolean shift = (event.modifiers() & 1) != 0;
		boolean command = (event.modifiers() & (2 | 8)) != 0;
		switch (event.key()) {
			case 257, 335 -> code.newline();
			case 258 -> code.insert("\t");
			case 259 -> code.backspace();
			case 261 -> code.delete();
			case 262 -> code.right(shift);
			case 263 -> code.left(shift);
			case 264 -> code.moveTo(code.caretLine() + 1, code.caretColumn(), shift);
			case 265 -> code.moveTo(code.caretLine() - 1, code.caretColumn(), shift);
			case 266 -> code.moveTo(code.caretLine() - codeRows(), code.caretColumn(), shift);
			case 267 -> code.moveTo(code.caretLine() + codeRows(), code.caretColumn(), shift);
			case 268 -> code.moveTo(code.caretLine(), 0, shift);
			case 269 -> code.moveTo(code.caretLine(), Integer.MAX_VALUE, shift);
			case 65 -> {
				if (!command) {
					return true;
				}
				code.selectAll();
			}
			case 67, 88 -> {
				if (!command) {
					return true;
				}
				if (code.hasSelection()) {
					minecraft.keyboardHandler.setClipboard(code.selectedText());
					if (event.key() == 88) {
						code.cutSelection();
					}
				}
			}
			case 86 -> {
				if (!command) {
					return true;
				}
				code.insert(minecraft.keyboardHandler.getClipboard());
			}
			case 90 -> {
				if (!command) {
					return true;
				}
				if (shift) {
					code.redo();
				} else {
					code.undo();
				}
			}
			default -> {
				return true;
			}
		}
		errors = List.of();
		showCaret();
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!editing()) {
			return super.charTyped(event);
		}
		if (event.isAllowedChatCharacter()) {
			code.insert(event.codepointAsString());
			errors = List.of();
			showCaret();
		}
		return true;
	}

	private void placeCaret(double mouseX, double mouseY, boolean extend) {
		int line = scrollLine + (int) ((mouseY - TOP - 2) / ROW);
		line = Math.max(0, Math.min(code.lineCount() - 1, line));
		String row = code.line(line);
		int column = 0;
		while (column < row.length() && gutter() + font.width(expand(row.substring(0, column + 1))) - 2 < mouseX) {
			column++;
		}
		code.moveTo(line, column, extend);
		blink = 0;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		if (super.mouseClicked(event, doubled)) {
			return true;
		}
		double mouseX = event.x();
		double mouseY = event.y();
		if (tab == Tab.CODE) {
			setFocused(null);
			if (mouseY >= TOP && mouseY < codeBottom()) {
				placeCaret(mouseX, mouseY, false);
				dragging = true;
				return true;
			}
			int row = (int) ((mouseY - codeBottom() - 4) / ROW);
			if (mouseY > codeBottom() && row >= 0 && row < errors.size() && row < ERROR_ROWS) {
				code.moveTo(errors.get(row).line() - 1, 0, false);   // an error in the list jumps to its line
				showCaret();
				return true;
			}
		} else if (tab == Tab.TERMINALS && mouseX < width - 216) {
			int rowY = TOP + 16 - scrollList * ROW;
			ListTag devices = state.getListOrEmpty("devices");
			for (int d = 0; d < devices.size(); d++) {
				rowY += ROW;
				ListTag points = devices.getCompoundOrEmpty(d).getListOrEmpty("points");
				for (int p = 0; p < points.size(); p++) {
					if (mouseY >= rowY && mouseY < rowY + ROW) {
						selectedDevice = d;
						selectedPoint = p;
						if (fieldOne != null) {
							fieldOne.setValue(points.getCompoundOrEmpty(p).getStringOr("address", ""));
						}
						if (fieldTwo != null) {
							fieldTwo.setValue(devices.getCompoundOrEmpty(d).getStringOr("tag", ""));
						}
						return true;
					}
					rowY += ROW;
				}
				rowY += 3;
			}
		} else if (tab == Tab.ONLINE && mouseX >= width - 208 && mouseY >= TOP + 92 && fieldOne != null) {
			int want = scrollList + (int) ((mouseY - TOP - 92) / ROW);
			String filter = fieldOne.getValue().toUpperCase(Locale.ROOT);
			int row = 0;
			for (String name : tagNames) {
				if (name.contains(filter)) {
					if (row == want) {
						fieldOne.setValue(name);
						selectedTag = 0;
						return true;
					}
					row++;
				}
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (tab == Tab.CODE && dragging) {
			placeCaret(event.x(), event.y(), true);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (tab == Tab.CODE) {
			scrollLine = Math.max(0, Math.min(Math.max(0, code.lineCount() - 1), scrollLine - (int) Math.signum(scrollY) * 3));
		} else {
			scrollList = Math.max(0, scrollList - (int) Math.signum(scrollY) * 3);
		}
		return true;
	}
}
