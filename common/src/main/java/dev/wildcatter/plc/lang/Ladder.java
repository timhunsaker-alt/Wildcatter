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

	public enum CoilKind {
		COIL, NOT, SET, RESET
	}

	public static final class Coil {
		public CoilKind kind;
		public String var;

		public Coil(CoilKind kind, String var) {
			this.kind = kind;
			this.var = var;
		}
	}

	public static final class Rung {
		public String comment = "";
		public final Series body = new Series();
		public final List<Coil> coils = new ArrayList<>();
	}

	public String kind = "PROGRAM";
	public String name = "MAIN";
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
		out.append(kind).append(' ').append(name).append('\n').append(declarations).append("\n---\n");
		for (Rung rung : rungs) {
			List<Object> coils = new ArrayList<>();
			coils.add("C");
			for (Coil coil : rung.coils) {
				coils.add(Sexpr.list(coil.kind.name(), coil.var));
			}
			out.append(Sexpr.list("R", rung.comment, tree(rung.body), Sexpr.list(coils.toArray()))).append('\n');
		}
		return out.toString();
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
		String[] first = (newline < 0 ? head : head.substring(0, newline)).trim().split(" ");
		out.kind = first[0].isEmpty() ? "PROGRAM" : first[0].toUpperCase(Locale.ROOT);
		out.name = first.length > 1 ? first[1].toUpperCase(Locale.ROOT) : "MAIN";
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
		int line;

		Build(Map<String, Ast.Pou> pous, List<CompileError> errors, String source) {
			this.pous = pous;
			this.errors = errors;
			this.source = source;
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
			}
		}
	}

	/** This ladder as a POU of the tree. {@code header} is its declared POU, with the variables. */
	Ast.Pou toPou(Ast.Pou header, Map<String, Ast.Pou> pous, List<CompileError> errors, String source) {
		number();
		Build build = new Build(pous, errors, source);
		List<Stmt> body = new ArrayList<>();
		for (int i = 0; i < rungs.size(); i++) {
			Rung rung = rungs.get(i);
			build.line = i + 1;
			Expr power = build.power(rung.body, new Ast.BoolLit(i + 1, true), body);
			for (Coil coil : rung.coils) {
				build.coil(coil, power, body);
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
