package dev.wildcatter.plc.lang;

import dev.wildcatter.plc.lang.Ast.Expr;
import dev.wildcatter.plc.lang.Ast.Stmt;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A function block diagram (IEC 61131-3 FBD): blocks wired output to input, between variables. Execution follows the wiring, left to right
 * and top to bottom where the wiring leaves a choice; a loop reads last scan's value, as the standard's feedback does. Each function's result
 * goes in a variable of its own, for the screen to read.
 */
public final class Fbd {
	/** A value coming in: a variable or constant, with one output pin. */
	public static final String IN = "IN_VAR";
	/** A variable written, with one input pin. */
	public static final String OUT = "OUT_VAR";
	private static final String WIRE = "@";

	public static final class Block {
		public final int id;
		public String type;
		/** A function block's instance; for {@link #IN} and {@link #OUT}, the variable. */
		public String name;
		public int x;
		public int y;
		/** Pin to its source: an expression, or a wire as made by {@link #wire}. */
		public final Map<String, String> inputs = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		public Block(int id, String type, String name, int x, int y) {
			this.id = id;
			this.type = type;
			this.name = name;
			this.x = x;
			this.y = y;
		}
	}

	public String kind = "PROGRAM";
	public String name = "MAIN";
	/** A FUNCTION's result type, as written; empty for a program or function block. */
	public String returns = "";
	public String declarations = "VAR\nEND_VAR";
	public final List<Block> blocks = new ArrayList<>();

	public static String wire(int block, String pin) {
		return WIRE + block + "." + pin;
	}

	public static boolean isWire(String source) {
		return source != null && source.startsWith(WIRE);
	}

	public static int wireBlock(String source) {
		return Integer.parseInt(source.substring(1, source.indexOf('.')));
	}

	public static String wirePin(String source) {
		return source.substring(source.indexOf('.') + 1);
	}

	public static String resultVar(int block) {
		return "__B" + block;
	}

	public int nextId() {
		int most = 0;
		for (Block block : blocks) {
			most = Math.max(most, block.id);
		}
		return most + 1;
	}

	public Block block(int id) {
		for (Block block : blocks) {
			if (block.id == id) {
				return block;
			}
		}
		return null;
	}

	public String write() {
		StringBuilder out = new StringBuilder();
		out.append(Ladder.headLine(kind, name, returns)).append('\n').append(declarations).append("\n---\n");
		for (Block block : blocks) {
			List<Object> parts = new ArrayList<>(List.of("B", Integer.toString(block.id), block.type, block.name == null ? "" : block.name, Integer.toString(block.x), Integer.toString(block.y)));
			block.inputs.forEach((pin, source) -> parts.add(Sexpr.list(pin, source)));
			out.append(Sexpr.list(parts.toArray())).append('\n');
		}
		return out.toString();
	}

	public static Fbd parse(String text) {
		Fbd out = new Fbd();
		int cut = text.indexOf("\n---\n");
		String head = cut < 0 ? text : text.substring(0, cut);
		int newline = head.indexOf('\n');
		String[] first = Ladder.readHead(newline < 0 ? head : head.substring(0, newline));
		out.kind = first[0];
		out.name = first[1];
		out.returns = first[2];
		out.declarations = newline < 0 ? "VAR\nEND_VAR" : head.substring(newline + 1);
		if (cut >= 0) {
			for (String line : text.substring(cut + 5).split("\n")) {
				if (!line.isBlank()) {
					Sexpr form = Sexpr.parse(line);
					Block block = new Block(Integer.parseInt(form.text(1)), form.text(2), form.text(3), Integer.parseInt(form.text(4)), Integer.parseInt(form.text(5)));
					for (Sexpr pin : form.items.subList(6, form.items.size())) {
						block.inputs.put(pin.text(0), pin.text(1));
					}
					out.blocks.add(block);
				}
			}
		}
		return out;
	}

	/** The execution order: each block after what it reads, except across a feedback loop. */
	public List<Block> order() {
		List<Block> left = new ArrayList<>(blocks);
		left.sort(Comparator.<Block>comparingInt(b -> b.x).thenComparingInt(b -> b.y).thenComparingInt(b -> b.id));
		Set<Integer> done = new HashSet<>();
		List<Block> out = new ArrayList<>();
		while (!left.isEmpty()) {
			Block next = null;
			for (Block block : left) {
				boolean ready = true;
				for (String source : block.inputs.values()) {
					if (isWire(source) && !done.contains(wireBlock(source)) && block(wireBlock(source)) != null && wireBlock(source) != block.id) {
						ready = false;
					}
				}
				if (ready) {
					next = block;
					break;
				}
			}
			if (next == null) {
				next = left.getFirst();   // a feedback loop: its first block reads last scan's value
			}
			left.remove(next);
			done.add(next.id);
			out.add(next);
		}
		return out;
	}

	private Expr source(Block block, String pin, String text, Map<String, Ast.Pou> pous, List<CompileError> errors, String sourceName) {
		int line = block.id;
		if (text == null || text.isBlank()) {
			errors.add(new CompileError(sourceName, line, 1, block.type + " " + (block.name == null ? "" : block.name) + ": input " + pin + " has nothing on it"));
			return new Ast.BoolLit(line, false);
		}
		if (isWire(text)) {
			Block from = block(wireBlock(text));
			if (from == null) {
				errors.add(new CompileError(sourceName, line, 1, "input " + pin + " is wired to a block that is gone"));
				return new Ast.BoolLit(line, false);
			}
			if (from.type.equals(IN)) {
				return parsed(from, from.name, errors, sourceName);
			}
			if (BlockLibrary.needsInstance(from.type, pous)) {
				return new Ast.Member(line, new Ast.Name(line, from.name.toUpperCase(Locale.ROOT)), wirePin(text));
			}
			return new Ast.Name(line, resultVar(from.id));
		}
		return parsed(block, text, errors, sourceName);
	}

	private Expr parsed(Block block, String text, List<CompileError> errors, String sourceName) {
		if (text == null || text.isBlank()) {
			errors.add(new CompileError(sourceName, block.id, 1, "a variable block has no name"));
			return new Ast.BoolLit(block.id, false);
		}
		List<CompileError> found = new ArrayList<>();
		Expr out = Parser.parseExpression(text, block.id, found);
		for (CompileError error : found) {
			errors.add(new CompileError(sourceName, block.id, 1, "'" + text + "': " + error.message()));
		}
		return out == null ? new Ast.BoolLit(block.id, false) : out;
	}

	Ast.Pou toPou(Ast.Pou header, Map<String, Ast.Pou> pous, List<CompileError> errors, String sourceName) {
		List<Stmt> body = new ArrayList<>();
		List<Ast.VarDecl> extra = new ArrayList<>();
		Map<String, String> instances = new HashMap<>();
		for (Block block : order()) {
			int line = block.id;
			if (block.type.equals(IN)) {
				continue;
			}
			if (block.type.equals(OUT)) {
				Expr value = source(block, "IN", block.inputs.get("IN"), pous, errors, sourceName);
				body.add(new Ast.Assign(line, parsed(block, block.name, errors, sourceName), value));
				continue;
			}
			BlockLibrary.Pins pins = BlockLibrary.pins(block.type, pous);
			if (pins == null) {
				errors.add(new CompileError(sourceName, line, 1, "there is no block " + block.type));
				continue;
			}
			if (BlockLibrary.needsInstance(block.type, pous)) {
				if (header.kind() == Ast.PouKind.FUNCTION) {
					errors.add(new CompileError(sourceName, line, 1, Ladder.functionHoldsBlock(header.name(), block.type)));
					continue;
				}
				if (block.name == null || block.name.isBlank()) {
					errors.add(new CompileError(sourceName, line, 1, "a " + block.type + " block needs an instance name"));
					continue;
				}
				String instance = block.name.toUpperCase(Locale.ROOT);
				instances.put(instance, block.type);
				List<Ast.Arg> args = new ArrayList<>();
				for (String pin : pins.inputs()) {
					if (block.inputs.containsKey(pin)) {
						args.add(new Ast.Arg(pin, source(block, pin, block.inputs.get(pin), pous, errors, sourceName), false));
					}
				}
				body.add(new Ast.CallStmt(line, new Ast.Call(line, instance, new Ast.Name(line, instance), args)));
				continue;
			}
			List<Expr> args = new ArrayList<>();
			for (String pin : pins.inputs()) {
				if (block.type.equals("MUX") && !block.inputs.containsKey(pin)) {
					continue;
				}
				args.add(source(block, pin, block.inputs.get(pin), pous, errors, sourceName));
			}
			Expr value = BlockLibrary.call(line, block.type, args);
			String result = resultVar(block.id);
			extra.add(new Ast.VarDecl(line, 1, result, Ast.VarKind.VAR, false, false, null, new Ast.AutoType(line, value), null));
			body.add(new Ast.Assign(line, new Ast.Name(line, result), value));
		}
		List<Ast.VarDecl> vars = new ArrayList<>(header.vars());
		instances.forEach((name, type) -> {
			if (vars.stream().noneMatch(v -> v.name().equals(name))) {
				vars.add(new Ast.VarDecl(1, 1, name, Ast.VarKind.VAR, false, false, null, new Ast.NamedType(1, type), null));
			}
		});
		// a result's type comes from what it computes, so it must follow the instances it reads and, in wiring order, the results it reads
		vars.addAll(extra);
		return new Ast.Pou(header.line(), header.kind(), header.name(), header.returns(), vars, body);
	}
}
