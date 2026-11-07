package dev.wildcatter.plc.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** What the graphical languages can place, and its pins: the project's function blocks and functions, and the standard's operators. */
public final class BlockLibrary {
	/** A block's pins. */
	public record Pins(List<String> inputs, List<String> outputs) {
	}

	private static final Set<String> COMPARE = Set.of("GT", "GE", "LT", "LE", "EQ", "NE");
	private static final Set<String> MATH = Set.of("ADD", "SUB", "MUL", "DIV", "MOD", "AND", "OR", "XOR");
	private static final Set<String> ONE = Set.of("NOT", "NEG", "ABS", "SQRT", "LN", "LOG", "EXP", "SIN", "COS", "TAN", "ASIN", "ACOS", "ATAN", "TRUNC", "MOVE");
	private static final Map<String, String> SYMBOL = Map.of("GT", ">", "GE", ">=", "LT", "<", "LE", "<=", "EQ", "=", "NE", "<>", "ADD", "+", "SUB", "-", "MUL", "*", "DIV", "/");
	/** What the screens offer first. */
	public static final List<String> COMMON = List.of("TON", "TOF", "TP", "CTU", "CTD", "CTUD", "R_TRIG", "F_TRIG", "SR", "RS", "PID", "ALARM_HL", "TOTALIZER", "MOTOR_CTRL", "VALVE_CTRL", "ADD", "SUB", "MUL", "DIV", "MOD", "AND", "OR", "XOR", "NOT", "GT", "GE", "LT", "LE", "EQ", "NE",
			"MOVE", "SEL", "MUX", "MIN", "MAX", "LIMIT", "ABS", "SQRT", "SCALE");

	private BlockLibrary() {
	}

	public static boolean isCompare(String type) {
		return COMPARE.contains(type);
	}

	/** The operator a two-input block compiles to, or null. */
	static String operator(String type) {
		if (SYMBOL.containsKey(type)) {
			return SYMBOL.get(type);
		}
		return MATH.contains(type) ? type : null;
	}

	/** {@code type}'s pins, from the project's POUs; null if there is no such block. */
	public static Pins pins(String type, Map<String, Ast.Pou> pous) {
		if (COMPARE.contains(type) || MATH.contains(type) || type.equals("MIN") || type.equals("MAX")) {
			return new Pins(List.of("IN1", "IN2"), List.of("OUT"));
		}
		if (ONE.contains(type) || type.contains("_TO_")) {
			return new Pins(List.of("IN"), List.of("OUT"));
		}
		switch (type) {
			case "SEL":
				return new Pins(List.of("G", "IN0", "IN1"), List.of("OUT"));
			case "LIMIT":
				return new Pins(List.of("MN", "IN", "MX"), List.of("OUT"));
			case "MUX":
				return new Pins(List.of("K", "IN0", "IN1", "IN2", "IN3"), List.of("OUT"));
			default:
				break;
		}
		Ast.Pou pou = pous.get(type);
		if (pou == null || pou.kind() == Ast.PouKind.PROGRAM) {
			return null;
		}
		List<String> in = new ArrayList<>();
		List<String> out = new ArrayList<>();
		for (Ast.VarDecl decl : pou.vars()) {
			if (decl.kind() == Ast.VarKind.INPUT || decl.kind() == Ast.VarKind.IN_OUT) {
				in.add(decl.name());
			} else if (decl.kind() == Ast.VarKind.OUTPUT) {
				out.add(decl.name());
			}
		}
		if (pou.kind() == Ast.PouKind.FUNCTION) {
			out.add(0, "OUT");
		}
		return new Pins(in, out);
	}

	/** True for a function block, which needs an instance. */
	public static boolean needsInstance(String type, Map<String, Ast.Pou> pous) {
		Ast.Pou pou = pous.get(type);
		return pou != null && pou.kind() == Ast.PouKind.FUNCTION_BLOCK;
	}

	/** The first BOOL input and output of a function block: where a ladder's power goes in and comes out. */
	static String firstBool(Ast.Pou pou, Ast.VarKind kind) {
		for (Ast.VarDecl decl : pou.vars()) {
			if (decl.kind() == kind && decl.type() instanceof Ast.NamedType named && named.name().equals("BOOL")) {
				return decl.name();
			}
		}
		return null;
	}

	/** A function or operator block's value. */
	static Ast.Expr call(int line, String type, List<Ast.Expr> args) {
		String op = operator(type);
		if (op != null && args.size() == 2) {
			return new Ast.Binary(line, op, args.get(0), args.get(1));
		}
		if (type.equals("NOT") && args.size() == 1) {
			return new Ast.Unary(line, "NOT", args.getFirst());
		}
		List<Ast.Arg> list = new ArrayList<>();
		for (Ast.Expr arg : args) {
			list.add(new Ast.Arg(null, arg, false));
		}
		return new Ast.Call(line, type, new Ast.Name(line, type), list);
	}
}
