package dev.wildcatter.plc.lang;

import dev.wildcatter.plc.lang.Ast.Expr;
import dev.wildcatter.plc.lang.Ast.Stmt;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A ladder diagram (IEC 61131-3 LD): rungs of contacts and boxes in series and parallel, driving coils. It compiles to the same tree as
 * structured text, one assignment an element, into a variable of its own: those are what the screen reads to draw the power flow, so what
 * it shows is what the CPU computed.
 */
public final class Ladder {
	public sealed interface Node {
		int id();

		void id(int id);
	}

	public static final class Contact implements Node {
		public String var;
		public boolean negated;
		private int id;

		public Contact(String var, boolean negated) {
			this.var = var;
			this.negated = negated;
		}

		@Override
		public int id() {
			return id;
		}

		@Override
		public void id(int id) {
			this.id = id;
		}
	}

	/** A function block, function or operator box. Power goes in at its first BOOL input, or its enable, and out of its first BOOL output. */
	public static final class Box implements Node {
		public String type;
		public String instance;
		/** Pin to expression: inputs are read, outputs written. */
		public final Map<String, String> pins = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		private int id;

		public Box(String type, String instance) {
			this.type = type;
			this.instance = instance;
		}

		@Override
		public int id() {
			return id;
		}

		@Override
		public void id(int id) {
			this.id = id;
		}
	}

	public static final class Series implements Node {
		public final List<Node> items = new ArrayList<>();
		private int id;

		@Override
		public int id() {
			return id;
		}

		@Override
		public void id(int id) {
			this.id = id;
		}
	}

	public static final class Parallel implements Node {
		public final List<Node> items = new ArrayList<>();
		private int id;

		@Override
		public int id() {
			return id;
		}

		@Override
		public void id(int id) {
			this.id = id;
		}
	}

	/** What a rung drives: a coil on a variable, or, at the rung's end, a jump to a labelled rung ({@code var} is the label) or a return. */
	public enum CoilKind {
		COIL, NOT, SET, RESET, JUMP, RETURN;

		/** A jump or return moves the scan on; it writes no variable. */
		public boolean flow() {
			return this == JUMP || this == RETURN;
		}
	}

	public static final class Coil {
		public CoilKind kind;
		/** The variable driven; for a jump, the label it goes to; empty for a return. */
		public String var;

		public Coil(CoilKind kind, String var) {
			this.kind = kind;
			this.var = var;
		}
	}

	public static final class Rung {
		public String comment = "";
		/** The label a jump goes to; empty for none. */
		public String label = "";
		public final Series body = new Series();
		public final List<Coil> coils = new ArrayList<>();
	}

	public String kind = "PROGRAM";
	public String name = "MAIN";
	/** A FUNCTION's result type, as written; empty for a program or function block. */
	public String returns = "";
	/** The VAR blocks, in structured text. */
	public String declarations = "VAR\nEND_VAR";
	public final List<Rung> rungs = new ArrayList<>();

	public static String powerVar(int id) {
		return "__P" + id;
	}

	/** Numbers every element, the same way on the server and in the screen. */
	public void number() {
		int[] next = {1};
		for (Rung rung : rungs) {
			number(rung.body, next);
		}
	}

	private static void number(Node node, int[] next) {
		node.id(next[0]++);
		switch (node) {
			case Series s -> s.items.forEach(n -> number(n, next));
			case Parallel p -> p.items.forEach(n -> number(n, next));
			default -> {
			}
		}
	}

	// ---- the saved form --------------------------------------------------------------------------------------------------------------

	public String write() {
		StringBuilder out = new StringBuilder();
		out.append(headLine(kind, name, returns)).append('\n').append(declarations).append("\n---\n");
		for (Rung rung : rungs) {
			List<Object> coils = new ArrayList<>();
			coils.add("C");
			for (Coil coil : rung.coils) {
				coils.add(Sexpr.list(coil.kind.name(), coil.var));
			}
			List<Object> parts = new ArrayList<>(List.of("R", rung.comment, tree(rung.body), Sexpr.list(coils.toArray())));
			if (rung.label != null && !rung.label.isBlank()) {
				parts.add(rung.label);   // after the coils, so a ladder saved before labels reads the same
			}
			out.append(Sexpr.list(parts.toArray())).append('\n');
		}
		return out.toString();
	}

	/** A diagram's first saved line: {@code PROGRAM MAIN}, {@code FUNCTION_BLOCK X}, or {@code FUNCTION F : INT}. */
	static String headLine(String kind, String name, String returns) {
		return kind + ' ' + name + (kind.equals("FUNCTION") && returns != null && !returns.isBlank() ? " : " + returns : "");
	}

	/** The first saved line read back: kind, name and a function's result type. */
	static String[] readHead(String line) {
		String text = line.trim();
		String returns = "";
		int colon = text.indexOf(':');
		if (colon >= 0) {
			returns = text.substring(colon + 1).trim().toUpperCase(Locale.ROOT);
			text = text.substring(0, colon).trim();
		}
		String[] first = text.split("\\s+");
		String kind = first[0].isEmpty() ? "PROGRAM" : first[0].toUpperCase(Locale.ROOT);
		String name = first.length > 1 ? first[1].toUpperCase(Locale.ROOT) : "MAIN";
		return new String[] {kind, name, returns};
	}

	private static Sexpr tree(Node node) {
		return switch (node) {
			case Contact c -> Sexpr.list(c.negated ? "NC" : "NO", c.var);
			case Box b -> {
				List<Object> parts = new ArrayList<>(List.of("B", b.type, b.instance));
				b.pins.forEach((pin, value) -> parts.add(Sexpr.list(pin, value)));
				yield Sexpr.list(parts.toArray());
			}
			case Series s -> {
				List<Object> parts = new ArrayList<>(List.of((Object) "S"));
				s.items.forEach(n -> parts.add(tree(n)));
				yield Sexpr.list(parts.toArray());
			}
			case Parallel p -> {
				List<Object> parts = new ArrayList<>(List.of((Object) "P"));
				p.items.forEach(n -> parts.add(tree(n)));
				yield Sexpr.list(parts.toArray());
			}
		};
	}

	public static Ladder parse(String text) {
		Ladder out = new Ladder();
		int cut = text.indexOf("\n---\n");
		String head = cut < 0 ? text : text.substring(0, cut);
		int newline = head.indexOf('\n');
		String[] first = readHead(newline < 0 ? head : head.substring(0, newline));
		out.kind = first[0];
		out.name = first[1];
		out.returns = first[2];
		out.declarations = newline < 0 ? "VAR\nEND_VAR" : head.substring(newline + 1);
		if (cut >= 0) {
			for (String line : text.substring(cut + 5).split("\n")) {
				if (!line.isBlank()) {
					out.rungs.add(rung(Sexpr.parse(line)));
				}
			}
		}
		return out;
	}

	private static Rung rung(Sexpr form) {
		Rung rung = new Rung();
		rung.comment = form.text(1);
		rung.label = form.text(4);
		for (Sexpr item : form.items.get(2).items.subList(1, form.items.get(2).items.size())) {
			rung.body.items.add(node(item));
		}
		for (Sexpr item : form.items.get(3).items.subList(1, form.items.get(3).items.size())) {
			rung.coils.add(new Coil(CoilKind.valueOf(item.text(0)), item.text(1)));
		}
		return rung;
	}

	private static Node node(Sexpr form) {
		switch (form.head()) {
			case "NO", "NC":
				return new Contact(form.text(1), form.head().equals("NC"));
			case "B":
				Box box = new Box(form.text(1), form.text(2));
				for (Sexpr pin : form.items.subList(3, form.items.size())) {
					box.pins.put(pin.text(0), pin.text(1));
				}
				return box;
			case "S":
				Series series = new Series();
				form.items.subList(1, form.items.size()).forEach(n -> series.items.add(node(n)));
				return series;
			case "P":
				Parallel parallel = new Parallel();
				form.items.subList(1, form.items.size()).forEach(n -> parallel.items.add(node(n)));
				return parallel;
			default:
				throw new IllegalArgumentException("the saved ladder is damaged: " + form);
		}
	}

	// ---- to the tree ------------------------------------------------------------------------------------------------------------------

	private static final class Build {
		final Map<String, Ast.Pou> pous;
		final List<CompileError> errors;
		final String source;
		final List<Ast.VarDecl> hidden = new ArrayList<>();
		final Map<String, String> instances = new LinkedHashMap<>();
		final Ast.Pou header;
		int line;

		Build(Ast.Pou header, Map<String, Ast.Pou> pous, List<CompileError> errors, String source) {
			this.header = header;
			this.pous = pous;
			this.errors = errors;
			this.source = source;
		}

		void error(String message) {
			errors.add(new CompileError(source, line, 1, message));
		}

		Expr flag(String name) {
			hidden.add(new Ast.VarDecl(line, 1, name, Ast.VarKind.VAR, false, false, null, new Ast.NamedType(line, "BOOL"), null));
			return new Ast.Name(line, name);
		}

		Expr expr(String text, String what) {
			if (text == null || text.isBlank()) {
				errors.add(new CompileError(source, line, 1, what + " has nothing on it"));
				return new Ast.BoolLit(line, false);
			}
			List<CompileError> found = new ArrayList<>();
			Expr out = Parser.parseExpression(text, line, found);
			for (CompileError error : found) {
				errors.add(new CompileError(source, line, 1, what + " '" + text + "': " + error.message()));
			}
			return out == null ? new Ast.BoolLit(line, false) : out;
		}

		Expr keep(int id, Expr value, List<Stmt> out) {
			String name = powerVar(id);
			hidden.add(new Ast.VarDecl(line, 1, name, Ast.VarKind.VAR, false, false, null, new Ast.NamedType(line, "BOOL"), null));
			out.add(new Ast.Assign(line, new Ast.Name(line, name), value));
			return new Ast.Name(line, name);
		}

		Expr and(Expr power, Expr value) {
			return power instanceof Ast.BoolLit lit && lit.value() ? value : new Ast.Binary(line, "AND", power, value);
		}

		Expr power(Node node, Expr in, List<Stmt> out) {
			switch (node) {
				case Contact c -> {
					Expr var = expr(c.var, "a contact");
					return keep(c.id(), and(in, c.negated ? new Ast.Unary(line, "NOT", var) : var), out);
				}
				case Series s -> {
					Expr at = in;
					for (Node item : s.items) {
						at = power(item, at, out);
					}
					return keep(s.id(), at, out);
				}
				case Parallel p -> {
					Expr any = null;
					for (Node item : p.items) {
						Expr branch = power(item, in, out);
						any = any == null ? branch : new Ast.Binary(line, "OR", any, branch);
					}
					return keep(p.id(), any == null ? in : any, out);
				}
				case Box b -> {
					return box(b, in, out);
				}
			}
		}

		Expr box(Box b, Expr in, List<Stmt> out) {
			BlockLibrary.Pins pins = BlockLibrary.pins(b.type, pous);
			if (pins == null) {
				errors.add(new CompileError(source, line, 1, "there is no block " + b.type));
				return keep(b.id(), in, out);
			}
			Ast.Pou pou = pous.get(b.type);
			if (pou != null && pou.kind() == Ast.PouKind.FUNCTION_BLOCK) {
				return functionBlock(b, pou, pins, in, out);
			}
			List<Expr> args = new ArrayList<>();
			for (String pin : pins.inputs()) {
				args.add(expr(b.pins.get(pin), b.type + "'s " + pin));
			}
			Expr value = BlockLibrary.call(line, b.type, args);
			if (BlockLibrary.isCompare(b.type)) {
				return keep(b.id(), and(in, value), out);
			}
			// an operator with an enable: it runs only with power, and passes the power on
			String target = b.pins.get("OUT");
			if (target != null && !target.isBlank()) {
				out.add(new Ast.If(line, List.of(new Ast.Branch(in, List.of(new Ast.Assign(line, expr(target, b.type + "'s OUT"), value)))), List.of()));
			}
			return keep(b.id(), in, out);
		}

		Expr functionBlock(Box b, Ast.Pou pou, BlockLibrary.Pins pins, Expr in, List<Stmt> out) {
			if (header.kind() == Ast.PouKind.FUNCTION) {
				error("rung " + line + ": " + functionHoldsBlock(header.name(), b.type));
				return keep(b.id(), in, out);
			}
			if (b.instance == null || b.instance.isBlank()) {
				errors.add(new CompileError(source, line, 1, "a " + b.type + " box needs an instance name"));
				return keep(b.id(), in, out);
			}
			String prior = instances.putIfAbsent(b.instance.toUpperCase(Locale.ROOT), b.type);
			if (prior != null && !prior.equals(b.type)) {
				errors.add(new CompileError(source, line, 1, b.instance + " is a " + prior + " elsewhere, so it cannot be a " + b.type));
			}
			String powerIn = BlockLibrary.firstBool(pou, Ast.VarKind.INPUT);
			String powerOut = BlockLibrary.firstBool(pou, Ast.VarKind.OUTPUT);
			List<Ast.Arg> args = new ArrayList<>();
			for (String pin : pins.inputs()) {
				if (pin.equals(powerIn) && !b.pins.containsKey(pin)) {
					args.add(new Ast.Arg(pin, in, false));
				} else if (b.pins.containsKey(pin) && !b.pins.get(pin).isBlank()) {
					args.add(new Ast.Arg(pin, expr(b.pins.get(pin), b.type + "'s " + pin), false));
				}
			}
			for (String pin : pins.outputs()) {
				if (b.pins.containsKey(pin) && !b.pins.get(pin).isBlank()) {
					args.add(new Ast.Arg(pin, expr(b.pins.get(pin), b.type + "'s " + pin), true));
				}
			}
			String instance = b.instance.toUpperCase(Locale.ROOT);
			out.add(new Ast.CallStmt(line, new Ast.Call(line, instance, new Ast.Name(line, instance), args)));
			return keep(b.id(), powerOut == null ? in : new Ast.Member(line, new Ast.Name(line, instance), powerOut), out);
		}

		void coil(Coil coil, Expr power, List<Stmt> out) {
			Expr target = expr(coil.var, "a coil");
			switch (coil.kind) {
				case COIL -> out.add(new Ast.Assign(line, target, power));
				case NOT -> out.add(new Ast.Assign(line, target, new Ast.Unary(line, "NOT", power)));
				case SET -> out.add(new Ast.If(line, List.of(new Ast.Branch(power, List.of(new Ast.Assign(line, target, new Ast.BoolLit(line, true))))), List.of()));
				case RESET -> out.add(new Ast.If(line, List.of(new Ast.Branch(power, List.of(new Ast.Assign(line, target, new Ast.BoolLit(line, false))))), List.of()));
				case JUMP, RETURN -> throw new IllegalStateException("a jump or return writes no variable");
			}
		}
	}

	/** Why a function cannot hold a function block box: the standard's rule, said so it can be fixed. */
	static String functionHoldsBlock(String function, String type) {
		return "the function " + function + " cannot hold a " + type + ": a function block keeps its state from scan to scan, and a function keeps none; make " + function + " a FUNCTION_BLOCK";
	}

	private static void ids(Node node, List<Integer> into) {
		into.add(node.id());
		switch (node) {
			case Series s -> s.items.forEach(n -> ids(n, into));
			case Parallel p -> p.items.forEach(n -> ids(n, into));
			default -> {
			}
		}
	}

	private static String key(String label) {
		return label == null ? "" : label.trim().toUpperCase(Locale.ROOT);
	}

	/** A jump with power: the rungs after its own, up to the labelled one, are skipped this scan. */
	private record Jump(int from, int to, String flag) {
	}

	/** This ladder as a POU of the tree. {@code header} is its declared POU, with the variables. */
	Ast.Pou toPou(Ast.Pou header, Map<String, Ast.Pou> pous, List<CompileError> errors, String source) {
		number();
		Build build = new Build(header, pous, errors, source);
		// the labels, each on one rung
		Map<String, Integer> labels = new LinkedHashMap<>();
		for (int i = 0; i < rungs.size(); i++) {
			String label = key(rungs.get(i).label);
			build.line = i + 1;
			if (label.isEmpty()) {
				continue;
			}
			if (!label.matches("[A-Z_][A-Z0-9_]*")) {
				build.error("rung " + (i + 1) + "'s label " + rungs.get(i).label.trim() + " is not a name: letters, digits and _, not starting with a digit");
			} else if (labels.containsKey(label)) {
				build.error("rung " + (i + 1) + "'s label " + label + " is already on rung " + (labels.get(label) + 1) + " of " + header.name());
			} else {
				labels.put(label, i);
			}
		}
		// the jumps: forward only, so a scan runs each rung at most once
		List<Jump> jumps = new ArrayList<>();
		int firstSkippable = rungs.size();
		for (int i = 0; i < rungs.size(); i++) {
			build.line = i + 1;
			int n = 0;
			for (Coil coil : rungs.get(i).coils) {
				if (coil.kind == CoilKind.RETURN) {
					firstSkippable = Math.min(firstSkippable, i + 1);
				}
				if (coil.kind != CoilKind.JUMP) {
					continue;
				}
				n++;
				String target = key(coil.var);
				Integer to = labels.get(target);
				if (target.isEmpty()) {
					build.error("rung " + (i + 1) + "'s jump has no label to go to");
				} else if (to == null) {
					build.error("rung " + (i + 1) + " jumps to " + target + ", but no rung of " + header.name() + " has that label");
				} else if (to <= i) {
					build.error("rung " + (i + 1) + " jumps back to " + target + " on rung " + (to + 1) + ": a jump here goes forward only, as going back would run rungs twice in one scan; use a loop in structured text");
				} else {
					jumps.add(new Jump(i, to, "__J" + (i + 1) + "_" + n));
					firstSkippable = Math.min(firstSkippable, i + 1);
				}
			}
		}
		List<Stmt> body = new ArrayList<>();
		// a rung a jump or return can pass over shows no power while it is passed over: its elements start each scan off
		for (int k = firstSkippable; k < rungs.size(); k++) {
			List<Integer> ids = new ArrayList<>();
			ids(rungs.get(k).body, ids);
			for (int id : ids) {
				body.add(new Ast.Assign(k + 1, new Ast.Name(k + 1, powerVar(id)), new Ast.BoolLit(k + 1, false)));
			}
		}
		for (Jump jump : jumps) {
			build.line = jump.from() + 1;
			body.add(new Ast.Assign(build.line, build.flag(jump.flag()), new Ast.BoolLit(build.line, false)));
		}
		for (int i = 0; i < rungs.size(); i++) {
			Rung rung = rungs.get(i);
			build.line = i + 1;
			List<Stmt> out = new ArrayList<>();
			Expr power = build.power(rung.body, new Ast.BoolLit(i + 1, true), out);
			int n = 0;
			for (Coil coil : rung.coils) {
				if (coil.kind == CoilKind.JUMP) {
					String flag = "__J" + (i + 1) + "_" + ++n;
					if (jumps.stream().anyMatch(j -> j.flag().equals(flag))) {
						out.add(new Ast.Assign(build.line, new Ast.Name(build.line, flag), power));
					}
				} else if (coil.kind != CoilKind.RETURN) {
					build.coil(coil, power, out);
				}
			}
			for (Coil coil : rung.coils) {
				if (coil.kind == CoilKind.RETURN) {
					out.add(new Ast.If(build.line, List.of(new Ast.Branch(power, List.of(new Ast.Return(build.line)))), List.of()));
				}
			}
			// the jumps that pass over this rung: it runs only while none of them has power
			Expr skipped = null;
			for (Jump jump : jumps) {
				if (jump.from() < i && i < jump.to()) {
					Expr flag = new Ast.Name(build.line, jump.flag());
					skipped = skipped == null ? flag : new Ast.Binary(build.line, "OR", skipped, flag);
				}
			}
			if (skipped == null) {
				body.addAll(out);
			} else {
				body.add(new Ast.If(build.line, List.of(new Ast.Branch(new Ast.Unary(build.line, "NOT", skipped), out)), List.of()));
			}
		}
		List<Ast.VarDecl> vars = new ArrayList<>(header.vars());
		build.instances.forEach((name, type) -> {
			if (vars.stream().noneMatch(v -> v.name().equals(name))) {
				vars.add(new Ast.VarDecl(1, 1, name, Ast.VarKind.VAR, false, false, null, new Ast.NamedType(1, type), null));
			}
		});
		vars.addAll(build.hidden);
		return new Ast.Pou(header.line(), header.kind(), header.name(), header.returns(), vars, body);
	}
}
