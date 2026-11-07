package dev.wildcatter.client;

import dev.wildcatter.control.PlcNet;
import dev.wildcatter.platform.Platform;
import dev.wildcatter.plc.CpuSpec;
import dev.wildcatter.plc.TextBuffer;
import dev.wildcatter.plc.lang.Ast;
import dev.wildcatter.plc.lang.BlockLibrary;
import dev.wildcatter.plc.lang.CompileError;
import dev.wildcatter.plc.lang.Fbd;
import dev.wildcatter.plc.lang.Ladder;
import dev.wildcatter.plc.lang.PlcCompiler;
import dev.wildcatter.plc.lang.PlcOpen;
import dev.wildcatter.plc.lang.Project;
import dev.wildcatter.plc.lang.Sfc;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
		CODE, TERMINALS, ONLINE, DIAGNOSTICS, PROJECT
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
	private static final int ERROR_ROWS = 4;
	private static final int PIECE = 8000;
	private static final int LIST_W = 80;
	private static final int SIDE_W = 104;
	private static final Set<String> KEYWORDS = Set.of("PROGRAM", "END_PROGRAM", "FUNCTION", "END_FUNCTION", "FUNCTION_BLOCK", "END_FUNCTION_BLOCK", "VAR", "VAR_INPUT", "VAR_OUTPUT", "VAR_IN_OUT",
			"VAR_TEMP", "VAR_GLOBAL", "VAR_EXTERNAL", "END_VAR", "CONSTANT", "RETAIN", "AT", "IF", "THEN", "ELSIF", "ELSE", "END_IF", "CASE", "OF", "END_CASE", "FOR", "TO", "BY", "DO", "END_FOR", "WHILE",
			"END_WHILE", "REPEAT", "UNTIL", "END_REPEAT", "EXIT", "CONTINUE", "RETURN", "AND", "OR", "XOR", "NOT", "MOD", "TRUE", "FALSE", "TYPE", "END_TYPE", "STRUCT", "END_STRUCT", "ARRAY", "CONFIGURATION",
			"END_CONFIGURATION", "RESOURCE", "END_RESOURCE", "ON", "TASK", "WITH");
	private static final Set<String> TYPES = Set.of("BOOL", "SINT", "USINT", "INT", "UINT", "DINT", "UDINT", "LINT", "BYTE", "WORD", "DWORD", "LWORD", "REAL", "LREAL", "TIME", "STRING", "TON", "TOF", "TP",
			"CTU", "CTD", "CTUD", "R_TRIG", "F_TRIG", "SR", "RS", "PID", "SCALE", "ALARM_HL", "TOTALIZER", "ACTION_CONTROL", "MOTOR_CTRL", "VALVE_CTRL", "SFC_STEP");
	/** A file's limit, as a project's on the server. */
	private static final int MOST_FILE = 4_000_000;
	private static final StringBuilder ARRIVING = new StringBuilder();

	private final int x;
	private final int y;
	private final int z;
	private final List<Unit> units = new ArrayList<>();
	private Unit unit;
	private final Map<String, String> values = new HashMap<>();
	private Map<String, Ast.Pou> library = Map.of();
	private String boxType = "TON";
	private TextBuffer code = new TextBuffer("");
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
	private final List<String> files = new ArrayList<>();
	private String selectedFile = "";
	private String fileName = "project";

	private PlcScreen(CompoundTag tag, List<Project.Source> project) {
		super(Component.literal("PLC cabinet"));
		x = tag.getIntOr("x", 0);
		y = tag.getIntOr("y", 0);
		z = tag.getIntOr("z", 0);
		loadProject(project);
		readValues(tag);
		tag.getListOrEmpty("names").forEach(t -> t.asString().ifPresent(tagNames::add));
		state = tag;
	}

	/** From the cabinet: opens the screen, or updates the one open on it. */
	public static void fromServer(CompoundTag tag) {
		Minecraft minecraft = Minecraft.getInstance();
		if (tag.contains("projectPiece")) {
			// a project arrives in pieces ahead of what opens the screen; if one is open on this cabinet, it is a card's load and replaces what is there
			if (tag.getIntOr("part", 0) == 0) {
				ARRIVING.setLength(0);
			}
			ARRIVING.append(tag.getStringOr("projectPiece", ""));
			if (tag.getIntOr("part", 0) + 1 == tag.getIntOr("parts", 1) && minecraft.gui.screen() instanceof PlcScreen open && open.x == tag.getIntOr("x", 0) && open.y == tag.getIntOr("y", 0) && open.z == tag.getIntOr("z", 0)) {
				List<Project.Source> project = Project.unpack(ARRIVING.toString());
				if (project != null) {
					open.loadProject(project);
					open.rebuildWidgets();
					open.sendWatch();
				}
			}
			return;
		}
		if (minecraft.gui.screen() instanceof PlcScreen open && open.x == tag.getIntOr("x", 0) && open.y == tag.getIntOr("y", 0) && open.z == tag.getIntOr("z", 0)) {
			open.state = tag;
			open.readValues(tag);
			tag.getListOrEmpty("messages").forEach(t -> t.asString().ifPresent(open.notes::add));
		} else if (tag.getBooleanOr("open", false)) {
			List<Project.Source> project = Project.unpack(ARRIVING.toString());
			minecraft.gui.setScreen(new PlcScreen(tag, project == null ? List.of() : project));
		}
	}

	private void loadProject(List<Project.Source> project) {
		units.clear();
		for (Project.Source source : project) {
			units.add(new Unit(source.name(), source.language(), source.text()));
		}
		if (units.isEmpty()) {
			units.add(new Unit("MAIN", Project.Language.ST, "PROGRAM MAIN\nVAR\nEND_VAR\n\nEND_PROGRAM"));
		}
		select(units.getFirst());
	}

	/** One program unit, being edited. */
	private final class Unit {
		String name;
		final Project.Language language;
		TextBuffer text;
		LadderPane ladder;
		FbdPane fbd;
		SfcPane sfc;
		boolean showVars;
		TextBuffer vars;
		/** A chart's action open in the text editor, or null. */
		String openAction;
		TextBuffer actionText;
		String monitor = "";
		final Deque<String> undo = new ArrayDeque<>();

		Unit(String name, Project.Language language, String saved) {
			this.name = name;
			this.language = language;
			load(saved);
		}

		void load(String saved) {
			switch (language) {
				case ST -> text = new TextBuffer(saved);
				case LD -> {
					ladder = new LadderPane(Ladder.parse(saved), type -> {
						BlockLibrary.Pins pins = BlockLibrary.pins(type, library);
						if (pins == null) {
							return List.of();
						}
						List<String> shown = new ArrayList<>(pins.inputs());
						Ast.Pou pou = library.get(type);
						if (pou != null) {
							shown.remove(firstBoolInput(pou));   // the rung's power goes there
						}
						return shown;
					});
					vars = new TextBuffer(ladder.ladder.declarations);
				}
				case FBD -> {
					fbd = new FbdPane(Fbd.parse(saved), type -> BlockLibrary.pins(type, library));
					vars = new TextBuffer(fbd.fbd.declarations);
				}
				case SFC -> {
					sfc = new SfcPane(Sfc.parse(saved));
					vars = new TextBuffer(sfc.sfc.declarations);
					openAction = null;
				}
			}
		}

		String saved() {
			return switch (language) {
				case ST -> text.text();
				case LD -> {
					ladder.ladder.declarations = vars.text();
					yield ladder.ladder.write();
				}
				case FBD -> {
					fbd.fbd.declarations = vars.text();
					yield fbd.fbd.write();
				}
				case SFC -> {
					sfc.sfc.declarations = vars.text();
					if (openAction != null) {
						sfc.sfc.bodies.put(openAction, actionText.text());
					}
					yield sfc.sfc.write();
				}
			};
		}

		String pouName() {
			return switch (language) {
				case LD -> ladder.ladder.name;
				case FBD -> fbd.fbd.name;
				case SFC -> sfc.sfc.name;
				case ST -> name;
			};
		}

		String kind() {
			return switch (language) {
				case LD -> ladder.ladder.kind;
				case FBD -> fbd.fbd.kind;
				case SFC -> sfc.sfc.kind;
				case ST -> "";
			};
		}

		/** Where the CPU holds this unit's variables: a program's name, or the instance chosen for a function block. */
		String prefix() {
			return !monitor.isBlank() ? monitor.toUpperCase(Locale.ROOT) : kind().equals("PROGRAM") ? pouName() : "";
		}

		void touch() {
			undo.push(saved());
			while (undo.size() > 100) {
				undo.removeLast();
			}
		}
	}

	private static String firstBoolInput(Ast.Pou pou) {
		for (Ast.VarDecl decl : pou.vars()) {
			if (decl.kind() == Ast.VarKind.INPUT && decl.type() instanceof Ast.NamedType named && named.name().equals("BOOL")) {
				return decl.name();
			}
		}
		return "";
	}

	private void select(Unit next) {
		unit = next;
		if (next.language == Project.Language.ST) {
			code = next.text;
		} else {
			code = next.openAction != null ? next.actionText : next.vars;
		}
		scrollLine = 0;
		refreshLibrary();
	}

	private void refreshLibrary() {
		library = PlcCompiler.library(sources());
	}

	private List<Project.Source> sources() {
		List<Project.Source> out = new ArrayList<>();
		for (Unit each : units) {
			out.add(new Project.Source(each.name, each.language, each.saved()));
		}
		return out;
	}

	private void readValues(CompoundTag tag) {
		values.clear();
		ListTag list = tag.getListOrEmpty("values");
		for (int i = 0; i < list.size(); i++) {
			String row = list.getStringOr(i, "");
			int cut = row.indexOf('=');
			if (cut > 0) {
				values.put(row.substring(0, cut), row.substring(cut + 1));
			}
		}
	}

	/** True while what is on screen is what the CPU is running. */
	private boolean inSync() {
		int hash = 0;
		for (Project.Source source : sources()) {
			hash = hash * 31 + (source.name() + source.language() + source.text()).hashCode();
		}
		return state.getIntOr("projectHash", 0) == hash;
	}

	private boolean live() {
		return inSync() && state.getBooleanOr("running", false) && !state.getBooleanOr("faulted", false) && state.getBooleanOr("powered", false) && !unit.prefix().isEmpty();
	}

	private boolean graphic() {
		return unit.language != Project.Language.ST && !unit.showVars && unit.openAction == null;
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
		refreshLibrary();
		int bx = 8;
		for (Tab each : Tab.values()) {
			int w = font.width(each.name()) + 8;
			Button b = button(bx, 22, w, each.name(), () -> {
				tab = each;
				scrollList = 0;
				rebuildWidgets();
				sendWatch();
			});
			b.active = each != tab;
			bx += w + 2;
		}
		int right = width - 8;
		button(right - 44, 22, 44, "COLD", () -> ask("cold", t -> {
		}));
		button(right - 88, 22, 42, "STOP", () -> {
			notes.clear();
			ask("key", t -> t.putBoolean("run", false));
		});
		button(right - 130, 22, 40, "RUN", () -> {
			notes.clear();
			ask("key", t -> t.putBoolean("run", true));
		});
		if (tab == Tab.CODE) {
			codeWidgets();
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
			EditBox range = addRenderableWidget(new EditBox(font, width - 208, TOP + 104, 140, 16, Component.literal("range")));
			range.setHint(Component.literal("0 100"));
			button(width - 64, TOP + 104, 56, "SET RANGE", () -> {
				CompoundTag device = device(selectedDevice);
				if (device != null) {
					ask("range", t -> {
						copyPos(device, t);
						t.putString("range", range.getValue());
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
		} else if (tab == Tab.PROJECT) {
			button(width - 208, TOP + 14, 60, "TO CARD", () -> ask("toCard", t -> {
			}));
			button(width - 146, TOP + 14, 60, "FROM CARD", () -> ask("fromCard", t -> {
			}));
			fieldOne = addRenderableWidget(new EditBox(font, width - 208, TOP + 50, 200, 16, Component.literal("file name")));
			fieldOne.setValue(fileName);
			fieldOne.setMaxLength(48);
			fieldOne.setResponder(value -> fileName = value);
			button(width - 208, TOP + 70, 66, "EXPORT XML", this::exportXml);
			button(width - 140, TOP + 70, 60, "EXPORT ST", this::exportText);
			button(width - 208, TOP + 88, 66, "IMPORT", this::importFile);
			button(width - 140, TOP + 88, 60, "REFRESH", this::listFiles);
			listFiles();
		}
	}

	private EditBox labelled(int bx, int by, int w, String label, String value, Consumer<String> change) {
		EditBox box = addRenderableWidget(new EditBox(font, bx, by, w, 14, Component.literal(label)));
		box.setMaxLength(256);
		box.setValue(value == null ? "" : value);
		box.setResponder(change);
		return box;
	}

	private int canvasRight() {
		return graphic() ? width - 8 - SIDE_W : width - 8;
	}

	/** The CODE tab: the list of units, the toolbar for this unit's language, and what is selected in it. */
	private void codeWidgets() {
		int right = width - 8;
		button(right - 64, height - 26, 60, "DOWNLOAD", this::download);
		button(right - 108, height - 26, 42, "CHECK", this::check);
		button(right - 170, height - 26, 60, "ONLINE CHG", this::onlineChange);
		int by = TOP + 14 + units.size() * ROW + 2;
		for (Project.Language language : Project.Language.values()) {
			int bx = 8 + language.ordinal() % 2 * 38;
			button(bx, by + language.ordinal() / 2 * 17, 37, "+" + language.name(), () -> addUnit(language));
		}
		by += 17;
		button(8, by + 18, 76, "DELETE UNIT", this::deleteUnit);
		labelled(8, by + 36, 76, "name", unit.name, value -> {
			unit.name = value;
			if (value.matches("[A-Za-z_][A-Za-z0-9_]*")) {   // a diagram's POU goes by its unit's name; structured text names its own
				String pou = value.toUpperCase(Locale.ROOT);
				switch (unit.language) {
					case LD -> unit.ladder.ladder.name = pou;
					case FBD -> unit.fbd.fbd.name = pou;
					case SFC -> unit.sfc.sfc.name = pou;
					case ST -> {
					}
				}
			}
		});
		if (unit.language != Project.Language.ST) {
			button(LIST_W + 8 + 0, TOP, 30, unit.showVars || unit.openAction != null ? "DRAW" : "VARS", () -> {
				if (unit.openAction != null) {
					closeAction();
				} else {
					unit.showVars = !unit.showVars;
				}
				select(unit);
				rebuildWidgets();
				sendWatch();
			});
		}
		if (!graphic()) {
			return;
		}
		int tx = LIST_W + 8 + 32;
		if (unit.language == Project.Language.LD) {
			for (String[] tool : new String[][] {{"NO", "24"}, {"NC", "24"}, {"BOX", "28"}, {"BRANCH", "44"}, {"COIL", "32"}, {"/", "14"}, {"S", "14"}, {"R", "14"}, {"RUNG", "32"}, {"DEL", "26"}}) {
				String name = tool[0];
				int w = Integer.parseInt(tool[1]);
				button(tx, TOP, w, name, () -> ladderTool(name));
				tx += w + 1;
			}
			ladderSide();
		} else if (unit.language == Project.Language.SFC) {
			for (String[] tool : new String[][] {{"STEP", "32"}, {"TRANS", "36"}, {"ACTION", "42"}, {"INITIAL", "44"}, {"DEL", "26"}}) {
				String name = tool[0];
				int w = Integer.parseInt(tool[1]);
				button(tx, TOP, w, name, () -> sfcTool(name));
				tx += w + 1;
			}
			sfcSide();
		} else {
			for (String[] tool : new String[][] {{"IN", "24"}, {"OUT", "28"}, {"BLOCK", "40"}, {"DEL", "26"}}) {
				String name = tool[0];
				int w = Integer.parseInt(tool[1]);
				button(tx, TOP, w, name, () -> fbdTool(name));
				tx += w + 1;
			}
			fbdSide();
		}
	}

	private void typeChooser(int sx, int sy, Runnable changed) {
		button(sx, sy, 14, "<", () -> {
			int at = BlockLibrary.COMMON.indexOf(boxType);
			boxType = BlockLibrary.COMMON.get(Math.floorMod(at - 1, BlockLibrary.COMMON.size()));
			changed.run();
			rebuildWidgets();
		});
		button(sx + SIDE_W - 22, sy, 14, ">", () -> {
			int at = BlockLibrary.COMMON.indexOf(boxType);
			boxType = BlockLibrary.COMMON.get(Math.floorMod(at + 1, BlockLibrary.COMMON.size()));
			changed.run();
			rebuildWidgets();
		});
		labelled(sx + 15, sy + 1, SIDE_W - 38, "type", boxType, value -> {
			String upper = value.toUpperCase(Locale.ROOT);
			if (BlockLibrary.pins(upper, library) != null && !upper.equals(boxType)) {
				boxType = upper;
				changed.run();
				rebuildWidgets();
			}
		});
	}

	private void ladderSide() {
		int sx = width - 8 - SIDE_W + 4;
		int w = SIDE_W - 8;
		int sy = TOP + 18;
		LadderPane pane = unit.ladder;
		typeChooser(sx, sy + 9, () -> {
			if (pane.selected instanceof Ladder.Box box) {
				unit.touch();
				box.type = boxType;
				box.pins.clear();
				box.instance = BlockLibrary.needsInstance(boxType, library) ? (box.instance.isEmpty() ? autoInstance(boxType) : box.instance) : "";
			}
		});
		sy += 36;
		switch (pane.selected) {
			case Ladder.Contact c -> labelled(sx, sy + 9, w, "variable", c.var, value -> {
				c.var = value;
			});
			case Ladder.Coil c -> labelled(sx, sy + 9, w, "variable", c.var, value -> {
				c.var = value;
			});
			case Ladder.Box box -> {
				if (BlockLibrary.needsInstance(box.type, library)) {
					labelled(sx, sy + 9, w, "instance", box.instance, value -> {
						box.instance = value;
					});
					sy += 26;
				}
				BlockLibrary.Pins pins = BlockLibrary.pins(box.type, library);
				if (pins != null) {
					Ast.Pou pou = library.get(box.type);
					List<String> all = new ArrayList<>(pins.inputs());
					all.addAll(pins.outputs());
					for (String pin : all) {
						if (pou != null && pin.equals(firstBoolInput(pou))) {
							continue;
						}
						labelled(sx, sy + 9, w, pin, box.pins.getOrDefault(pin, ""), value -> {
							box.pins.put(pin, value);
						});
						sy += 26;
					}
				}
			}
			case null, default -> {
				if (pane.selectedRung != null) {
					labelled(sx, sy + 9, w, "comment", pane.selectedRung.comment, value -> {
						pane.selectedRung.comment = value;
					});
					sy += 26;
				}
				labelled(sx, sy + 9, w, "monitor", unit.monitor, value -> {
					unit.monitor = value;
					sendWatch();
				});
			}
		}
	}

	private void fbdSide() {
		int sx = width - 8 - SIDE_W + 4;
		int w = SIDE_W - 8;
		int sy = TOP + 18;
		FbdPane pane = unit.fbd;
		typeChooser(sx, sy + 9, () -> {
		});
		sy += 36;
		Fbd.Block block = pane.selected;
		if (block == null) {
			labelled(sx, sy + 9, w, "monitor", unit.monitor, value -> {
				unit.monitor = value;
				sendWatch();
			});
			return;
		}
		boolean named = block.type.equals(Fbd.IN) || block.type.equals(Fbd.OUT) || BlockLibrary.needsInstance(block.type, library);
		if (named) {
			labelled(sx, sy + 9, w, block.type.equals(Fbd.IN) || block.type.equals(Fbd.OUT) ? "variable or value" : "instance", block.name, value -> {
				block.name = value;
			});
			sy += 26;
		}
		BlockLibrary.Pins pins = BlockLibrary.pins(block.type, library);
		if (pins != null && !block.type.equals(Fbd.IN) && !block.type.equals(Fbd.OUT)) {
			for (String pin : pins.inputs()) {
				String source = block.inputs.get(pin);
				labelled(sx, sy + 9, w, pin, Fbd.isWire(source) ? "(wired)" : source, value -> {
					if (!value.equals("(wired)")) {
						block.inputs.put(pin, value);
					}
				});
				sy += 26;
			}
		}
	}

	private void sfcSide() {
		int sx = width - 8 - SIDE_W + 4;
		int w = SIDE_W - 8;
		int sy = TOP + 18;
		SfcPane pane = unit.sfc;
		if (pane.selectedAction != null) {
			Sfc.Association action = pane.selectedAction;
			labelled(sx, sy + 9, w, "qualifier", action.qualifier, value -> {
				action.qualifier = value.toUpperCase(Locale.ROOT);
			});
			sy += 26;
			labelled(sx, sy + 9, w, "action", action.action, value -> {
				action.action = value;
			});
			sy += 26;
			labelled(sx, sy + 9, w, "time", action.time, value -> {
				action.time = value;
			});
			sy += 26;
			button(sx, sy + 4, w, "BODY", () -> openAction(action.action));
		} else if (pane.selectedStep != null) {
			Sfc.Step step = pane.selectedStep;
			labelled(sx, sy + 9, w, "step name", step.name, value -> {
				step.name = value;
			});
			sy += 26;
		} else if (pane.selectedTransition != null) {
			Sfc.Transition t = pane.selectedTransition;
			labelled(sx, sy + 9, w, "condition", t.condition, value -> {
				t.condition = value;
			});
			sy += 26;
		} else {
			labelled(sx, sy + 9, w, "monitor", unit.monitor, value -> {
				unit.monitor = value;
				sendWatch();
			});
		}
	}

	/** A chart's action body in the text editor. A name with no body yet is given one, which makes it a named action, not a BOOL it drives. */
	private void openAction(String action) {
		String name = action.trim().toUpperCase(Locale.ROOT);
		if (name.isEmpty() || !name.matches("[A-Z_][A-Z0-9_]*")) {
			notes.clear();
			notes.add("line: An action's body needs a plain name first");
			return;
		}
		unit.touch();
		unit.openAction = name;
		unit.actionText = new TextBuffer(unit.sfc.sfc.bodies.getOrDefault(name, ""));
		select(unit);
		rebuildWidgets();
	}

	private void closeAction() {
		String body = unit.actionText.text();
		if (body.isBlank()) {
			unit.sfc.sfc.bodies.remove(unit.openAction);   // no body: back to a BOOL it drives
		} else {
			unit.sfc.sfc.bodies.put(unit.openAction, body);
		}
		unit.openAction = null;
		unit.actionText = null;
	}

	private void sfcTool(String name) {
		SfcPane pane = unit.sfc;
		unit.touch();
		switch (name) {
			case "STEP" -> pane.addStep();
			case "TRANS" -> pane.addTransition();
			case "ACTION" -> {
				if (pane.selectedStep != null) {
					Sfc.Association added = new Sfc.Association("N", "", "");
					pane.selectedStep.actions.add(added);
					pane.selectedAction = added;
				}
			}
			case "INITIAL" -> {
				if (pane.selectedStep != null) {
					pane.selectedStep.initial = !pane.selectedStep.initial;
				}
			}
			case "DEL" -> pane.deleteSelected();
			default -> {
			}
		}
		rebuildWidgets();
		sendWatch();
	}

	private String autoInstance(String type) {
		int n = 1;
		String all = String.join("\n", sources().stream().map(Project.Source::text).toList()).toUpperCase(Locale.ROOT);
		while (all.contains("\"" + type + "_" + n + "\"")) {
			n++;
		}
		return type + "_" + n;
	}

	private void ladderTool(String name) {
		LadderPane pane = unit.ladder;
		unit.touch();
		switch (name) {
			case "NO" -> pane.insertSeries(new Ladder.Contact("", false));
			case "NC" -> pane.insertSeries(new Ladder.Contact("", true));
			case "BOX" -> pane.insertSeries(new Ladder.Box(boxType, BlockLibrary.needsInstance(boxType, library) ? autoInstance(boxType) : ""));
			case "BRANCH" -> pane.insertBranch(new Ladder.Contact("", false));
			case "COIL" -> pane.addCoil(Ladder.CoilKind.COIL);
			case "/" -> pane.addCoil(Ladder.CoilKind.NOT);
			case "S" -> pane.addCoil(Ladder.CoilKind.SET);
			case "R" -> pane.addCoil(Ladder.CoilKind.RESET);
			case "RUNG" -> pane.addRung();
			case "DEL" -> pane.deleteSelected();
			default -> {
			}
		}
		rebuildWidgets();
		sendWatch();
	}

	private void fbdTool(String name) {
		FbdPane pane = unit.fbd;
		unit.touch();
		switch (name) {
			case "IN" -> pane.add(Fbd.IN, "");
			case "OUT" -> pane.add(Fbd.OUT, "");
			case "BLOCK" -> pane.add(boxType, BlockLibrary.needsInstance(boxType, library) ? autoInstance(boxType) : "");
			case "DEL" -> pane.deleteSelected();
			default -> {
			}
		}
		rebuildWidgets();
		sendWatch();
	}

	private void addUnit(Project.Language language) {
		int n = units.size() + 1;
		String name = "POU" + n;
		String text = switch (language) {
			case ST -> "FUNCTION_BLOCK " + name + "\nVAR_INPUT\nEND_VAR\nVAR_OUTPUT\nEND_VAR\n\nEND_FUNCTION_BLOCK";
			case LD, FBD, SFC -> "PROGRAM " + name + "\nVAR\nEND_VAR\n---\n";
		};
		Unit added = new Unit(name, language, text);
		units.add(added);
		select(added);
		rebuildWidgets();
		sendWatch();
	}

	private void deleteUnit() {
		if (units.size() > 1) {
			units.remove(unit);
			select(units.getFirst());
			rebuildWidgets();
			sendWatch();
		}
	}

	private void sendWatch() {
		ask("watch", t -> {
			ListTag names = new ListTag();
			watched.forEach(n -> names.add(StringTag.valueOf(n)));
			if (tab == Tab.CODE && graphic() && !unit.prefix().isEmpty()) {
				List<String> shown = switch (unit.language) {
					case LD -> unit.ladder.watches(unit.prefix());
					case SFC -> unit.sfc.watches(unit.prefix());
					default -> unit.fbd.watches(unit.prefix());
				};
				shown.forEach(n -> names.add(StringTag.valueOf(n)));
			}
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

	// ---- the project tab: files on the player's own machine ---------------------------------------------------------------------------------

	private Path folder() {
		return minecraft.gameDirectory.toPath().resolve("wildcatter").resolve("plc");
	}

	private void listFiles() {
		files.clear();
		try (var entries = Files.list(folder())) {
			entries.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".xml") || n.endsWith(".st")).sorted().forEach(files::add);
		} catch (IOException e) {
			// no folder yet: nothing has been exported
		}
		if (!files.contains(selectedFile)) {
			selectedFile = "";
		}
	}

	/** The file named, within the folder: a name is letters, numbers, - and _, so it cannot walk out of it. */
	private Path exportPath(String extension) {
		String name = fileName.trim();
		if (!name.matches("[A-Za-z0-9_-]+")) {
			say("A file name is letters, numbers, - and _");
			return null;
		}
		return folder().resolve(name + extension);
	}

	private void say(String note) {
		notes.add(note);
		while (notes.size() > 8) {
			notes.removeFirst();
		}
	}

	private void writeFile(Path path, String text) {
		try {
			Files.createDirectories(path.getParent());
			boolean existed = Files.exists(path);
			Files.writeString(path, text, StandardCharsets.UTF_8);
			say((existed ? "Replaced " : "Wrote ") + path.getFileName() + " in " + folder());
			listFiles();
		} catch (IOException e) {
			say("Could not write it: " + e.getMessage());
		}
	}

	/** Writes the editor's project as PLCopen XML. */
	public void exportXml() {
		Path path = exportPath(".xml");
		if (path == null) {
			return;
		}
		PlcOpen.Exported out = PlcOpen.write(sources());
		if (out.xml() == null) {
			say("Not exported: fix these first.");
			out.problems().forEach(this::say);
		} else {
			writeFile(path, out.xml());
		}
	}

	/** Writes the editor's structured text units as one source. */
	public void exportText() {
		Path path = exportPath(".st");
		if (path == null) {
			return;
		}
		List<String> leftOut = new ArrayList<>();
		String text = PlcOpen.writeStructuredText(sources(), leftOut);
		if (text.isEmpty()) {
			say("This project has no structured text. EXPORT XML holds the diagrams.");
			return;
		}
		writeFile(path, text);
		if (!leftOut.isEmpty()) {
			say("Left out, as they are diagrams: " + String.join(", ", leftOut) + ". EXPORT XML holds them.");
		}
	}

	/** Reads the selected file into the editor: same names replaced, others kept, nothing sent to the CPU. */
	public void importFile() {
		if (selectedFile.isEmpty()) {
			say("Pick a file from the list.");
			return;
		}
		try {
			Path path = folder().resolve(selectedFile);
			if (Files.size(path) > MOST_FILE) {
				say("That file is over " + MOST_FILE / 1_000_000 + " million bytes");
				return;
			}
			String text = Files.readString(path, StandardCharsets.UTF_8);
			List<Project.Source> read;
			if (selectedFile.endsWith(".xml")) {
				PlcOpen.Imported imported = PlcOpen.read(text);
				if (!imported.problems().isEmpty()) {
					say("Not imported: nothing was changed.");
					imported.problems().forEach(this::say);
					return;
				}
				read = imported.sources();
			} else {
				read = List.of(new Project.Source(selectedFile.substring(0, selectedFile.length() - 3).toUpperCase(Locale.ROOT), Project.Language.ST, text));
			}
			Unit first = null;
			for (Project.Source source : read) {
				Unit added = new Unit(source.name(), source.language(), source.text());
				int at = -1;
				for (int i = 0; i < units.size(); i++) {
					if (units.get(i).name.equalsIgnoreCase(source.name())) {
						at = i;
					}
				}
				if (at >= 0) {
					units.set(at, added);
				} else {
					units.add(added);
				}
				first = first == null ? added : first;
			}
			if (first != null) {
				select(first);
			}
			say("Imported " + read.size() + " unit(s) from " + selectedFile + ". DOWNLOAD sends them.");
			rebuildWidgets();
		} catch (IOException | RuntimeException e) {
			say("Could not read it: " + e.getMessage());
		}
	}

	/** Compiles here, touching nothing on the CPU. */
	public void check() {
		refreshLibrary();
		CpuSpec spec = state.getStringOr("cpu", "").equals(CpuSpec.LARGE.name()) ? CpuSpec.LARGE : CpuSpec.SMALL;
		errors = PlcCompiler.compile(spec, sources()).errors();
		notes.clear();
		notes.add(errors.isEmpty() ? "No errors." : errors.size() + " error(s).");
	}

	private void send(boolean online) {
		check();
		if (errors.isEmpty()) {
			notes.clear();
			// in pieces, as a client's packet holds 32 KB: 8,000 characters are under that even at their widest
			String packed = Project.pack(sources());
			int total = Math.max(1, (packed.length() + PIECE - 1) / PIECE);
			for (int i = 0; i < total; i++) {
				int index = i;
				String piece = packed.substring(i * PIECE, Math.min(packed.length(), (i + 1) * PIECE));
				ask("download", t -> {
					t.putBoolean("online", online);
					t.putInt("part", index);
					t.putInt("parts", total);
					t.putString("piece", piece);
				});
			}
		}
	}

	/** Sends the project to the cabinet, if it compiles here too. The CPU goes to STOP. */
	public void download() {
		send(false);
	}

	/** Sends the project and keeps the CPU running on it, with its values. */
	public void onlineChange() {
		send(true);
	}

	/** For the game test: what a paste does. */
	public void replaceForTest(String text) {
		code.setText(text);
	}

	/** For the game test: types into a labelled field, as a player does. */
	public boolean setFieldForTest(String label, String value) {
		for (var child : children()) {
			if (child instanceof EditBox box && box.getMessage().getString().equals(label)) {
				box.setValue(value);
				return true;
			}
		}
		return false;
	}

	public void deselectForTest() {
		switch (unit.language) {
			case LD -> unit.ladder.selected = null;
			case SFC -> {
				unit.sfc.selectedStep = null;
				unit.sfc.selectedTransition = null;
				unit.sfc.selectedAction = null;
			}
			case FBD -> unit.fbd.selected = null;
			case ST -> {
			}
		}
		rebuildWidgets();
	}

	public void deleteUnitForTest(String name) {
		units.removeIf(u -> u.name.equals(name) && units.size() > 1);
		select(units.getFirst());
		rebuildWidgets();
	}

	public void undoForTest() {
		if (!unit.undo.isEmpty()) {
			unit.load(unit.undo.pop());
			select(unit);
			rebuildWidgets();
		}
	}

	/** For the game test: selects the first transmitter in the terminals; false if none. */
	public boolean selectTransmitterForTest() {
		ListTag devices = state.getListOrEmpty("devices");
		for (int i = 0; i < devices.size(); i++) {
			if (devices.getCompoundOrEmpty(i).getStringOr("name", "").contains("transmitter")) {
				selectedDevice = i;
				selectedPoint = 0;
				return true;
			}
		}
		return false;
	}

	public void sfcScrollForTest(int scroll) {
		unit.sfc.scrollX = 0;
		unit.sfc.scrollY = scroll;
	}

	public int fbdScrollForTest() {
		return unit.fbd.scrollX;
	}

	public int fbdBlockXForTest(int block) {
		return unit.fbd.fbd.block(block).x;
	}

	public String unitNamesForTest() {
		return String.join(",", units.stream().map(u -> u.name + ":" + u.language).toList());
	}

	public int unitCountForTest() {
		return units.size();
	}

	/** For the game test: a chart's pin on the sheet, in GUI pixels, as a player would click it. */
	public int[] sfcStepPinForTest(String step, boolean bottom) {
		return unit.sfc.stepPin(step, bottom, LIST_W + 8, TOP + 18);
	}

	public int[] sfcTransitionPinForTest(int index, boolean bottom) {
		return unit.sfc.transitionPin(index, bottom, LIST_W + 8, TOP + 18);
	}

	/** For the game test: the middle of an FBD block's pin, in GUI pixels. */
	public int[] fbdPinForTest(int block, String pin, boolean output) {
		return unit.fbd.pinAt(block, pin, output, LIST_W + 8, TOP + 18);
	}

	public List<String> notesForTest() {
		return notes;
	}

	public List<String> filesForTest() {
		return files;
	}

	public void selectFileForTest(String name) {
		selectedFile = name;
	}

	public List<Project.Source> sourcesForTest() {
		return sources();
	}

	public void selectUnitForTest(String name) {
		for (Unit each : units) {
			if (each.name.equals(name)) {
				select(each);
				rebuildWidgets();
				sendWatch();
			}
		}
	}

	public String savedForTest() {
		return unit.saved();
	}

	/** For the game test: a unit laid out by a saved form, as a paste or an import does. */
	public void loadUnitForTest(Project.Language language, String name, String text) {
		units.removeIf(u -> u.name.equals(name));
		units.add(new Unit(name, language, text));
		selectUnitForTest(name);
	}

	/** For the game test: what the power flow is drawn from. */
	public boolean liveForTest() {
		return live();
	}

	public Map<String, String> valuesForTest() {
		return values;
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
		String status = state.getStringOr("status", "");
		int colour = status.startsWith("RUN") ? GOOD : status.startsWith("STOP") ? 0xFFE0C060 : ERROR;
		graphics.text(font, state.getStringOr("cpu", "No CPU"), 8, 8, TEXT, false);
		graphics.text(font, status, 8 + font.width(state.getStringOr("cpu", "No CPU")) + 12, 8, colour, false);
		if (tab == Tab.CODE) {
			String sync = live() ? "ONLINE" : inSync() ? "offline" : "differs from CPU";
			graphics.text(font, sync, 8 + font.width(state.getStringOr("cpu", "No CPU")) + 12 + font.width(status) + 10, 8, live() ? GOOD : MUTED, false);
		}
		if (state.getIntOr("budget", 0) > 0 && status.startsWith("RUN")) {
			String load = String.format("scan %d / %d instructions", state.getIntOr("instructions", 0), state.getIntOr("budget", 0));
			graphics.text(font, load, width - 8 - font.width(load), 8, MUTED, false);
		}
		switch (tab) {
			case CODE -> drawCode(graphics);
			case TERMINALS -> drawTerminals(graphics);
			case ONLINE -> drawOnline(graphics);
			case DIAGNOSTICS -> drawDiagnostics(graphics);
			case PROJECT -> drawProject(graphics);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partial);   // the widgets last, over the panels
	}

	/** For the game test: the middle of a labelled button, in GUI pixels, or null if none is there to click. */
	public int[] buttonForTest(String label) {
		for (var child : children()) {
			if (child instanceof Button b && b.getMessage().getString().equals(label) && b.visible && b.active) {
				return new int[] {b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
			}
		}
		return null;
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
		return LIST_W + 8 + font.width("0000") + 8;
	}

	private void drawCode(GuiGraphicsExtractor graphics) {
		drawUnits(graphics);
		if (graphic()) {
			drawGraphic(graphics);
		} else {
			drawText(graphics);
		}
		drawMessages(graphics);
	}

	private void drawUnits(GuiGraphicsExtractor graphics) {
		graphics.fill(8, TOP, LIST_W + 4, codeBottom(), PANEL);
		graphics.text(font, "Program units", 10, TOP + 2, MUTED, false);
		for (int i = 0; i < units.size(); i++) {
			Unit each = units.get(i);
			int rowY = TOP + 14 + i * ROW;
			if (each == unit) {
				graphics.fill(8, rowY - 1, LIST_W + 4, rowY + ROW - 1, SELECT);
			}
			graphics.text(font, font.plainSubstrByWidth(each.name, LIST_W - 24), 10, rowY, TEXT, false);
			int shade = switch (each.language) {
				case ST -> TYPE;
				case LD -> GOOD;
				case FBD -> NUMBER;
				case SFC -> ADDRESS;
			};
			graphics.text(font, each.language.name(), LIST_W + 2 - font.width(each.language.name()), rowY, shade, false);
		}
	}

	private void drawGraphic(GuiGraphicsExtractor graphics) {
		int top = TOP + 18;
		int left = LIST_W + 8;
		int right = canvasRight();
		int bottom = codeBottom();
		graphics.fill(left, top, right, bottom, PANEL);
		boolean live = live();
		Set<String> forced = new HashSet<>();
		values.forEach((k, v) -> {
			if (v.contains("FORCED")) {
				forced.add(k);
			}
		});
		switch (unit.language) {
			case LD -> unit.ladder.draw(graphics, font, left, top, right, bottom, values, unit.prefix(), live, forced);
			case SFC -> unit.sfc.draw(graphics, font, left, top, right, bottom, values, unit.prefix(), live);
			default -> unit.fbd.draw(graphics, font, left, top, right, bottom, values, unit.prefix(), live);
		}
		int sx = right + 2;
		graphics.fill(sx, TOP, width - 8, bottom, PANEL);
		for (var child : children()) {
			if (child instanceof EditBox box) {
				graphics.text(font, box.getMessage().getString(), box.getX(), box.getY() - 9, MUTED, false);
			}
		}
	}

	private void drawText(GuiGraphicsExtractor graphics) {
		int bottom = codeBottom();
		int top = unit.language == Project.Language.ST ? TOP : TOP + 18;
		graphics.fill(LIST_W + 8, top, width - 8, bottom, PANEL);
		if (unit.openAction != null) {
			graphics.text(font, "Action " + unit.openAction + ": structured text, run while it is active and once more as it ends", LIST_W + 8 + 34, TOP + 4, MUTED, false);
		}
		graphics.enableScissor(LIST_W + 8, top, width - 8, bottom);
		int rows = codeRows();
		int[] selection = code.selection();
		for (int i = 0; i < rows && scrollLine + i < code.lineCount(); i++) {
			int at = scrollLine + i;
			int rowY = top + 2 + i * ROW;
			String row = code.line(at);
			boolean bad = errors.stream().anyMatch(e -> e.line() == at + 1 && (e.source().isEmpty() || e.source().equals(unit.name)));
			graphics.text(font, Integer.toString(at + 1), LIST_W + 10, rowY, bad ? ERROR : COMMENT, false);
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
	}

	private void drawMessages(GuiGraphicsExtractor graphics) {
		int bottom = codeBottom();
		int noteY = bottom + 4;
		graphics.fill(8, bottom + 2, width - 8, height - 6, PANEL);
		for (CompileError error : errors.stream().limit(ERROR_ROWS).toList()) {
			graphics.text(font, font.plainSubstrByWidth(describe(error), width - 24), 12, noteY, ERROR, false);
			noteY += ROW;
		}
		String compile = state.getStringOr("compile", "");
		for (String note : notes) {
			if (noteY < height - 8) {
				graphics.text(font, font.plainSubstrByWidth(note, width - 24), 12, noteY, note.startsWith("Download refused") || note.startsWith("line") ? ERROR : GOOD, false);
				noteY += ROW;
			}
		}
		if (errors.isEmpty() && notes.isEmpty() && !compile.isEmpty()) {
			graphics.text(font, compile.lines().findFirst().orElse(""), 12, noteY, ERROR, false);
		}
	}

	/** An error as it reads: a chart's line is an element or a line of an action, so it says which. */
	private String describe(CompileError error) {
		Unit in = unitOf(error);
		if (in != null && in.language == Project.Language.SFC) {
			String where = in.sfc.sfc.where(error.line());
			if (!where.isEmpty()) {
				return in.name + " " + where + ": " + error.message();
			}
		}
		return error.toString();
	}

	private Unit unitOf(CompileError error) {
		if (error.source().isEmpty()) {
			return units.size() == 1 ? units.getFirst() : null;
		}
		for (Unit each : units) {
			if (each.name.equals(error.source())) {
				return each;
			}
		}
		return null;
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
		graphics.text(font, "Signals, and where each is landed", 12, TOP + 3, MUTED, false);
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
		graphics.text(font, "A transmitter's 4 and 20 mA values:", width - 208, TOP + 93, TEXT, false);
		int railY = height - 8;
		List<net.minecraft.util.FormattedCharSequence> rail = font.split(Component.literal("Rail: " + state.getStringOr("rail", "")), 200);
		for (int i = 0; i < rail.size(); i++) {
			graphics.text(font, rail.get(i), width - 208, railY - (rail.size() - i) * ROW, MUTED, false);   // at the foot, as long as it needs
		}
		drawNotes(graphics, width - 208, TOP + 124);
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
		graphics.text(font, "Watched tags", 12, TOP + 3, MUTED, false);
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

	private void drawProject(GuiGraphicsExtractor graphics) {
		int split = width - 216;
		graphics.fill(8, TOP, split, height - 8, PANEL);
		graphics.text(font, "Files in the game folder, wildcatter/plc", 12, TOP + 3, MUTED, false);
		graphics.enableScissor(8, TOP + 14, split, height - 8);
		if (files.isEmpty()) {
			int hintY = TOP + 16;
			for (var hint : font.split(Component.literal("None yet. EXPORT writes here, and a file put here can be imported."), split - 20)) {
				graphics.text(font, hint, 12, hintY, MUTED, false);
				hintY += ROW;
			}
		}
		for (int i = scrollList; i < files.size(); i++) {
			int rowY = TOP + 16 + (i - scrollList) * ROW;
			String name = files.get(i);
			if (name.equals(selectedFile)) {
				graphics.fill(10, rowY - 1, split - 2, rowY + ROW - 1, SELECT);
			}
			graphics.text(font, name, 12, rowY, TEXT, false);
		}
		graphics.disableScissor();
		String card = state.getStringOr("card", "");
		String shown = card.isEmpty() ? "Card: none in the door. Click a card on the cabinet." : "Card: " + card;
		graphics.text(font, font.plainSubstrByWidth(shown, 200), width - 208, TOP + 3, TEXT, false);
		graphics.text(font, "Export the editor's project as:", width - 208, TOP + 39, TEXT, false);
		drawNotes(graphics, width - 208, TOP + 108);
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
		return tab == Tab.CODE && !graphic() && !(getFocused() instanceof EditBox);
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
		if (tab == Tab.CODE && graphic() && !(getFocused() instanceof EditBox)) {
			boolean command = (event.modifiers() & (2 | 8)) != 0;
			if (event.key() == 261 || event.key() == 259) {
				switch (unit.language) {
					case LD -> ladderTool("DEL");
					case SFC -> sfcTool("DEL");
					default -> fbdTool("DEL");
				}
				return true;
			}
			if (event.key() == 262 || event.key() == 263) {   // the arrow keys scroll a wide sheet
				int by = event.key() == 262 ? 24 : -24;
				switch (unit.language) {
					case LD -> unit.ladder.scrollX = Math.max(0, unit.ladder.scrollX + by);
					case SFC -> unit.sfc.scrollX = Math.max(0, unit.sfc.scrollX + by);
					default -> unit.fbd.scrollX = Math.max(0, unit.fbd.scrollX + by);
				}
				return true;
			}
			if (command && event.key() == 90 && !unit.undo.isEmpty()) {
				unit.load(unit.undo.pop());
				select(unit);
				rebuildWidgets();
				sendWatch();
				return true;
			}
		}
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
			if (mouseX < LIST_W + 4 && mouseY >= TOP + 14) {
				int row = (int) ((mouseY - TOP - 14) / ROW);
				if (row >= 0 && row < units.size()) {
					select(units.get(row));
					rebuildWidgets();
					sendWatch();
					return true;
				}
			}
			int errorRow = (int) ((mouseY - codeBottom() - 4) / ROW);
			if (mouseY > codeBottom() && errorRow >= 0 && errorRow < errors.size() && errorRow < ERROR_ROWS) {
				CompileError error = errors.get(errorRow);
				for (Unit each : units) {
					if (each.name.equals(error.source())) {
						select(each);
						rebuildWidgets();
					}
				}
				if (!graphic()) {
					code.moveTo(error.line() - 1, 0, false);   // an error in the list jumps to its line
					showCaret();
				} else if (unit.language == Project.Language.LD && error.line() - 1 < unit.ladder.ladder.rungs.size()) {
					unit.ladder.selectedRung = unit.ladder.ladder.rungs.get(error.line() - 1);
				} else if (unit.language == Project.Language.SFC) {
					int action = Sfc.actionOfLine(error.line());
					List<String> names = new ArrayList<>(unit.sfc.sfc.bodies.keySet());
					if (action >= 0 && action < names.size()) {
						openAction(names.get(action));
						code.moveTo(error.line() % 1000 - 1, 0, false);
						showCaret();
					} else {
						Sfc.Step step = unit.sfc.sfc.step(error.line());
						unit.sfc.selectedStep = step;
						unit.sfc.selectedAction = null;
						unit.sfc.selectedTransition = unit.sfc.sfc.transitions.stream().filter(t -> t.id == error.line()).findFirst().orElse(null);
						rebuildWidgets();
					}
				}
				return true;
			}
			setFocused(null);
			if (graphic() && mouseY >= TOP + 18 && mouseY < codeBottom() && mouseX >= LIST_W + 8 && mouseX < canvasRight()) {
				if (unit.language == Project.Language.LD) {
					Object before = unit.ladder.selected;
					unit.ladder.click(mouseX, mouseY);
					if (before != unit.ladder.selected) {
						rebuildWidgets();
					}
				} else if (unit.language == Project.Language.SFC) {
					Object[] before = {unit.sfc.selectedStep, unit.sfc.selectedTransition, unit.sfc.selectedAction};
					unit.touch();
					unit.sfc.press(mouseX, mouseY, LIST_W + 8, TOP + 18);
					if (before[0] != unit.sfc.selectedStep || before[1] != unit.sfc.selectedTransition || before[2] != unit.sfc.selectedAction) {
						rebuildWidgets();
					}
				} else {
					Fbd.Block before = unit.fbd.selected;
					unit.touch();
					unit.fbd.press(mouseX, mouseY, LIST_W + 8, TOP + 18);
					if (before != unit.fbd.selected) {
						rebuildWidgets();
					}
				}
				return true;
			}
			if (!graphic() && mouseY >= TOP && mouseY < codeBottom() && mouseX >= LIST_W + 8) {
				placeCaret(mouseX, mouseY, false);
				dragging = true;
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
		} else if (tab == Tab.PROJECT && mouseX < width - 216 && mouseY >= TOP + 14) {
			int row = scrollList + (int) ((mouseY - TOP - 16) / ROW);
			if (row >= 0 && row < files.size()) {
				selectedFile = files.get(row);
				return true;
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
		if (tab == Tab.CODE && graphic() && unit.language == Project.Language.FBD && unit.fbd.drag(event.x(), event.y(), LIST_W + 8, TOP + 18)) {
			return true;
		}
		if (tab == Tab.CODE && graphic() && unit.language == Project.Language.SFC && unit.sfc.drag(event.x(), event.y(), LIST_W + 8, TOP + 18)) {
			return true;
		}
		if (tab == Tab.CODE && dragging) {
			placeCaret(event.x(), event.y(), true);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		if (unit.language == Project.Language.FBD) {
			unit.fbd.release();
		} else if (unit.language == Project.Language.SFC) {
			unit.sfc.release();
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (tab == Tab.CODE && graphic()) {
			int by = (int) Math.signum(scrollY) * 12;
			switch (unit.language) {
				case LD -> unit.ladder.scroll = Math.max(0, unit.ladder.scroll - by);
				case SFC -> unit.sfc.scrollY = Math.max(0, unit.sfc.scrollY - by);
				default -> unit.fbd.scrollY = Math.max(0, unit.fbd.scrollY - by);
			}
		} else if (tab == Tab.CODE) {
			scrollLine = Math.max(0, Math.min(Math.max(0, code.lineCount() - 1), scrollLine - (int) Math.signum(scrollY) * 3));
		} else {
			scrollList = Math.max(0, scrollList - (int) Math.signum(scrollY) * 3);
		}
		return true;
	}
}
