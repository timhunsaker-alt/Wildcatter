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
 * A sequential function chart (IEC 61131-3 SFC): steps and transitions, wired as a graph. A transition with several steps before or after it
 * is a parallel branch; several transitions leaving a step are a choice, taken left to right. It compiles to the same tree as the other
 * languages: every step is a {@code SFC_STEP} variable with its {@code .X} and {@code .T}, and every action has an {@code ACTION_CONTROL}.
 */
public final class Sfc {
	public static final List<String> QUALIFIERS = List.of("N", "R", "S", "L", "D", "P", "P1", "P0", "SD", "DS", "SL");
	private static final Set<String> TIMED = Set.of("L", "D", "SD", "DS", "SL");
	/** An action body's lines are numbered from here, a thousand for each action in turn, so an error can say where it is. */
	private static final int ACTION_LINES = 1000;

	/** One step's use of an action. */
	public static final class Association {
		public String qualifier;
		public String action;
		public String time;

		public Association(String qualifier, String action, String time) {
			this.qualifier = qualifier;
			this.action = action;
			this.time = time;
		}
	}

	public static final class Step {
		public final int id;
		public String name;
		public boolean initial;
		public int x;
		public int y;
		public final List<Association> actions = new ArrayList<>();

		public Step(int id, String name, boolean initial, int x, int y) {
			this.id = id;
			this.name = name;
			this.initial = initial;
			this.x = x;
			this.y = y;
		}
	}

	public static final class Transition {
		public final int id;
		public String condition;
		public int x;
		public int y;
		public final List<Integer> from = new ArrayList<>();
		public final List<Integer> to = new ArrayList<>();

		public Transition(int id, String condition, int x, int y) {
			this.id = id;
			this.condition = condition;
			this.x = x;
			this.y = y;
		}
	}

	public String kind = "PROGRAM";
	public String name = "MAIN";
	public String declarations = "VAR\nEND_VAR";
	public final List<Step> steps = new ArrayList<>();
	public final List<Transition> transitions = new ArrayList<>();
	/** Named actions' bodies, in structured text. */
	public final Map<String, String> bodies = new LinkedHashMap<>();

	public int nextId() {
		int most = 0;
		for (Step step : steps) {
			most = Math.max(most, step.id);
		}
		for (Transition transition : transitions) {
			most = Math.max(most, transition.id);
		}
		return most + 1;
	}

	public Step step(int id) {
		for (Step step : steps) {
			if (step.id == id) {
				return step;
			}
		}
		return null;
	}

	/** What the screen reads to show a condition: true when it holds, whether or not its step is active. */
	public static String conditionVar(int transition) {
		return "__C" + transition;
	}

	/** The instance holding the action's Q and A: the action's own name for a named one, as its body reads {@code NAME.Q}. */
	public String controlOf(String action) {
		String upper = action.toUpperCase(Locale.ROOT);
		if (bodies.containsKey(upper)) {
			return upper;
		}
		return "__A" + Integer.toHexString(upper.hashCode() & 0x7fffffff);
	}

	/** Where an error's line is in a chart, in words: an element's id or a line of an action's body. */
	public String where(int line) {
		if (line < ACTION_LINES) {
			for (Step step : steps) {
				if (step.id == line) {
					return "step " + step.name;
				}
			}
			for (Transition transition : transitions) {
				if (transition.id == line) {
					return "transition " + transition.id;
				}
			}
			return "";
		}
		List<String> names = new ArrayList<>(bodies.keySet());
		int index = line / ACTION_LINES - 1;
		return index < names.size() ? "action " + names.get(index) + " line " + (line % ACTION_LINES) : "";
	}

	/** The body's first line, for {@link #where} to find: used by the screen to open it at an error. */
	public static int actionOfLine(int line) {
		return line < ACTION_LINES ? -1 : line / ACTION_LINES - 1;
	}

	// ---- the saved form --------------------------------------------------------------------------------------------------------------

	public String write() {
		StringBuilder out = new StringBuilder();
		out.append(kind).append(' ').append(name).append('\n').append(declarations).append("\n---\n");
		for (Step step : steps) {
			List<Object> parts = new ArrayList<>(List.of("S", Integer.toString(step.id), step.name, step.initial ? "1" : "0", Integer.toString(step.x), Integer.toString(step.y)));
			for (Association a : step.actions) {
				parts.add(Sexpr.list(a.qualifier, a.action, a.time == null ? "" : a.time));
			}
			out.append(Sexpr.list(parts.toArray())).append('\n');
		}
		for (Transition t : transitions) {
			List<Object> from = new ArrayList<>(List.of((Object) "F"));
			t.from.forEach(id -> from.add(Integer.toString(id)));
			List<Object> to = new ArrayList<>(List.of((Object) "O"));
			t.to.forEach(id -> to.add(Integer.toString(id)));
			out.append(Sexpr.list("T", Integer.toString(t.id), t.condition, Integer.toString(t.x), Integer.toString(t.y), Sexpr.list(from.toArray()), Sexpr.list(to.toArray()))).append('\n');
		}
		bodies.forEach((action, body) -> out.append(Sexpr.list("A", action, body)).append('\n'));
		return out.toString();
	}

	public static Sfc parse(String text) {
		Sfc out = new Sfc();
		int cut = text.indexOf("\n---\n");
		String head = cut < 0 ? text : text.substring(0, cut);
		int newline = head.indexOf('\n');
		String[] first = (newline < 0 ? head : head.substring(0, newline)).trim().split(" ");
		out.kind = first[0].isEmpty() ? "PROGRAM" : first[0].toUpperCase(Locale.ROOT);
		out.name = first.length > 1 ? first[1].toUpperCase(Locale.ROOT) : "MAIN";
		out.declarations = newline < 0 ? "VAR\nEND_VAR" : head.substring(newline + 1);
		if (cut >= 0) {
			for (String line : text.substring(cut + 5).split("\n")) {
				if (line.isBlank()) {
					continue;
				}
				Sexpr form = Sexpr.parse(line);
				switch (form.head()) {
					case "S" -> {
						Step step = new Step(Integer.parseInt(form.text(1)), form.text(2), form.text(3).equals("1"), Integer.parseInt(form.text(4)), Integer.parseInt(form.text(5)));
						for (Sexpr a : form.items.subList(6, form.items.size())) {
							step.actions.add(new Association(a.text(0), a.text(1), a.text(2)));
						}
						out.steps.add(step);
					}
					case "T" -> {
						Transition t = new Transition(Integer.parseInt(form.text(1)), form.text(2), Integer.parseInt(form.text(3)), Integer.parseInt(form.text(4)));
						Sexpr from = form.items.get(5);
						from.items.subList(1, from.items.size()).forEach(s -> t.from.add(Integer.parseInt(s.atom)));
						Sexpr to = form.items.get(6);
						to.items.subList(1, to.items.size()).forEach(s -> t.to.add(Integer.parseInt(s.atom)));
						out.transitions.add(t);
					}
					case "A" -> out.bodies.put(form.text(1).toUpperCase(Locale.ROOT), form.text(2));
					default -> throw new IllegalArgumentException("the saved chart is damaged: " + line);
				}
			}
		}
		return out;
	}

	// ---- to the tree ------------------------------------------------------------------------------------------------------------------

	private static Expr name(int line, String name) {
		return new Ast.Name(line, name);
	}

	private static Expr member(int line, String base, String field) {
		return new Ast.Member(line, new Ast.Name(line, base), field);
	}

	private static Stmt set(int line, Expr target, Expr value) {
		return new Ast.Assign(line, target, value);
	}

	private static Stmt when(int line, Expr condition, List<Stmt> body) {
		return new Ast.If(line, List.of(new Ast.Branch(condition, body)), List.of());
	}

	private static Expr now(int line) {
		return new Ast.Call(line, "_NOW", new Ast.Name(line, "_NOW"), List.of());
	}

	private static Expr and(int line, Expr a, Expr b) {
		return a == null ? b : new Ast.Binary(line, "AND", a, b);
	}

	private static Expr or(int line, Expr a, Expr b) {
		return a == null ? b : new Ast.Binary(line, "OR", a, b);
	}

	private static List<Stmt> activate(int line, String step) {
		return List.of(set(line, member(line, step, "X"), new Ast.BoolLit(line, true)), set(line, member(line, step, "T"), new Ast.TimeLit(line, 0)), set(line, member(line, step, "START"), now(line)));
	}

	private static String upper(String text) {
		return text == null ? "" : text.trim().toUpperCase(Locale.ROOT);
	}

	/** The chart's own rules, which the tree cannot check. */
	private void validate(List<CompileError> errors, String source) {
		if (!kind.equals("PROGRAM") && !kind.equals("FUNCTION_BLOCK")) {
			errors.add(new CompileError(source, 1, 1, "a chart is a PROGRAM or FUNCTION_BLOCK"));
		}
		if (steps.isEmpty()) {
			errors.add(new CompileError(source, 1, 1, "a chart needs a step"));
			return;
		}
		Set<String> seen = new HashSet<>();
		for (Step step : steps) {
			if (upper(step.name).isEmpty()) {
				errors.add(new CompileError(source, step.id, 1, "a step needs a name"));
			} else if (!seen.add(upper(step.name))) {
				errors.add(new CompileError(source, step.id, 1, "there are two steps named " + upper(step.name)));
			}
		}
		// one initial step a network, as the standard asks: a network is what the wires join
		Map<Integer, Integer> group = new HashMap<>();
		steps.forEach(s -> group.put(s.id, s.id));
		for (Transition t : transitions) {
			List<Integer> joined = new ArrayList<>(t.from);
			joined.addAll(t.to);
			for (int i = 1; i < joined.size(); i++) {
				if (group.containsKey(joined.get(0)) && group.containsKey(joined.get(i))) {
					int a = group.get(joined.get(0));
					int b = group.get(joined.get(i));
					group.replaceAll((k, v) -> v == b ? a : v);
				}
			}
		}
		Map<Integer, Integer> initials = new HashMap<>();
		for (Step step : steps) {
			if (step.initial) {
				initials.merge(group.get(step.id), 1, Integer::sum);
			}
		}
		for (Step step : steps) {
			int count = initials.getOrDefault(group.get(step.id), 0);
			if (count == 0) {
				errors.add(new CompileError(source, step.id, 1, "the chart holding step " + step.name + " has no initial step"));
				initials.put(group.get(step.id), -1);
			} else if (count > 1 && step.initial) {
				errors.add(new CompileError(source, step.id, 1, "the chart holding step " + step.name + " has " + count + " initial steps; it may have one"));
				initials.put(group.get(step.id), -1);
			}
		}
		for (Transition t : transitions) {
			if (t.from.isEmpty()) {
				errors.add(new CompileError(source, t.id, 1, "this transition has no step before it"));
			}
			if (t.to.isEmpty()) {
				errors.add(new CompileError(source, t.id, 1, "this transition has no step after it"));
			}
			if (upper(t.condition).isEmpty()) {
				errors.add(new CompileError(source, t.id, 1, "this transition has no condition"));
			}
			for (int id : t.from) {
				if (step(id) == null) {
					errors.add(new CompileError(source, t.id, 1, "this transition comes from a step that is gone"));
				}
			}
			for (int id : t.to) {
				if (step(id) == null) {
					errors.add(new CompileError(source, t.id, 1, "this transition leads to a step that is gone"));
				}
			}
		}
	}

	/** This chart as a POU of the tree. */
	Ast.Pou toPou(Ast.Pou header, List<CompileError> errors, String source) {
		validate(errors, source);
		List<Ast.VarDecl> vars = new ArrayList<>(header.vars());
		List<Stmt> body = new ArrayList<>();
		for (Step step : steps) {
			String stepName = upper(step.name);
			if (!stepName.isEmpty()) {
				vars.add(new Ast.VarDecl(step.id, 1, stepName, Ast.VarKind.VAR, false, false, null, new Ast.NamedType(step.id, "SFC_STEP"), null));
			}
		}
		vars.add(new Ast.VarDecl(1, 1, "__UP", Ast.VarKind.VAR, false, false, null, new Ast.NamedType(1, "BOOL"), null));
		// every transition against the steps active now; a choice goes left to right
		List<Transition> order = new ArrayList<>(transitions);
		order.sort(Comparator.<Transition>comparingInt(t -> t.x).thenComparingInt(t -> t.y).thenComparingInt(t -> t.id));
		List<Transition> earlier = new ArrayList<>();
		for (Transition t : order) {
			int line = t.id;
			String conditionVar = conditionVar(t.id);
			String fire = "__F" + t.id;
			vars.add(new Ast.VarDecl(line, 1, conditionVar, Ast.VarKind.VAR, false, false, null, new Ast.NamedType(line, "BOOL"), null));
			vars.add(new Ast.VarDecl(line, 1, fire, Ast.VarKind.VAR, false, false, null, new Ast.NamedType(line, "BOOL"), null));
			List<CompileError> found = new ArrayList<>();
			Expr condition = upper(t.condition).isEmpty() ? new Ast.BoolLit(line, false) : Parser.parseExpression(t.condition, line, found);
			found.forEach(e -> errors.add(new CompileError(source, line, 1, "condition '" + t.condition + "': " + e.message())));
			body.add(set(line, name(line, conditionVar), condition == null ? new Ast.BoolLit(line, false) : condition));
			Expr ready = null;
			for (int id : t.from) {
				Step before = step(id);
				if (before != null && !upper(before.name).isEmpty()) {
					ready = and(line, ready, member(line, upper(before.name), "X"));
				}
			}
			for (Transition other : earlier) {
				if (other.from.stream().anyMatch(t.from::contains)) {
					ready = and(line, ready, new Ast.Unary(line, "NOT", name(line, "__F" + other.id)));
				}
			}
			Expr clears = ready == null ? new Ast.BoolLit(line, false) : and(line, ready, name(line, conditionVar));
			body.add(set(line, name(line, fire), clears));
			earlier.add(t);
		}
		// all leave, then all arrive, so a step both left and entered ends active
		for (Transition t : order) {
			List<Stmt> leave = new ArrayList<>();
			for (int id : t.from) {
				Step before = step(id);
				if (before != null && !upper(before.name).isEmpty()) {
					leave.add(set(t.id, member(t.id, upper(before.name), "X"), new Ast.BoolLit(t.id, false)));
				}
			}
			body.add(when(t.id, name(t.id, "__F" + t.id), leave));
		}
		for (Transition t : order) {
			List<Stmt> arrive = new ArrayList<>();
			for (int id : t.to) {
				Step after = step(id);
				if (after != null && !upper(after.name).isEmpty()) {
					arrive.addAll(activate(t.id, upper(after.name)));
				}
			}
			body.add(when(t.id, name(t.id, "__F" + t.id), arrive));
		}
		List<Stmt> start = new ArrayList<>();
		start.add(set(1, name(1, "__UP"), new Ast.BoolLit(1, true)));
		for (Step step : steps) {
			if (step.initial && !upper(step.name).isEmpty()) {
				start.addAll(activate(step.id, upper(step.name)));
			}
		}
		body.add(when(1, new Ast.Unary(1, "NOT", name(1, "__UP")), start));
		for (Step step : steps) {
			String stepName = upper(step.name);
			if (!stepName.isEmpty()) {
				body.add(when(step.id, member(step.id, stepName, "X"), List.of(set(step.id, member(step.id, stepName, "T"), new Ast.Binary(step.id, "-", now(step.id), member(step.id, stepName, "START"))))));
			}
		}
		actions(vars, body, errors, source);
		return new Ast.Pou(header.line(), header.kind(), header.name(), header.returns(), vars, body);
	}

	private void actions(List<Ast.VarDecl> vars, List<Stmt> body, List<CompileError> errors, String source) {
		Map<String, Map<String, Expr>> inputs = new LinkedHashMap<>();
		Map<String, String> times = new HashMap<>();
		Map<String, Integer> lines = new HashMap<>();
		for (Step step : steps) {
			String stepName = upper(step.name);
			for (Association a : step.actions) {
				String action = upper(a.action);
				String qualifier = upper(a.qualifier).isEmpty() ? "N" : upper(a.qualifier);
				if (action.isEmpty()) {
					errors.add(new CompileError(source, step.id, 1, "step " + stepName + " has an action with no name"));
					continue;
				}
				if (!QUALIFIERS.contains(qualifier)) {
					errors.add(new CompileError(source, step.id, 1, "step " + stepName + ": there is no action qualifier " + qualifier));
					continue;
				}
				boolean timed = TIMED.contains(qualifier);
				String time = upper(a.time);
				if (timed && time.isEmpty()) {
					errors.add(new CompileError(source, step.id, 1, "step " + stepName + ": " + qualifier + " on " + action + " needs a time"));
				} else if (!timed && !time.isEmpty()) {
					errors.add(new CompileError(source, step.id, 1, "step " + stepName + ": " + qualifier + " on " + action + " takes no time"));
				}
				if (timed && !time.isEmpty()) {
					String prior = times.putIfAbsent(action, time);
					if (prior != null && !prior.equals(time)) {
						errors.add(new CompileError(source, step.id, 1, action + " is given two times, " + prior + " and " + time + "; an action has one"));
					}
				}
				lines.putIfAbsent(action, step.id);
				// S0 because S is a word the block keeps for the set-reset inside it
				inputs.computeIfAbsent(action, k -> new LinkedHashMap<>()).merge(qualifier.equals("S") ? "S0" : qualifier, member(step.id, stepName, "X"), (x, y) -> or(step.id, x, y));
			}
		}
		List<String> names = new ArrayList<>(bodies.keySet());
		inputs.forEach((action, in) -> {
			int line = lines.get(action);
			String control = controlOf(action);
			vars.add(new Ast.VarDecl(line, 1, control, Ast.VarKind.VAR, false, false, null, new Ast.NamedType(line, "ACTION_CONTROL"), null));
			List<Ast.Arg> args = new ArrayList<>();
			in.forEach((pin, value) -> args.add(new Ast.Arg(pin, value, false)));
			if (times.containsKey(action)) {
				List<CompileError> found = new ArrayList<>();
				Expr time = Parser.parseExpression(times.get(action), line, found);
				found.forEach(e -> errors.add(new CompileError(source, line, 1, action + "'s time: " + e.message())));
				if (time != null) {
					args.add(new Ast.Arg("T", time, false));
				}
			}
			body.add(new Ast.CallStmt(line, new Ast.Call(line, control, name(line, control), args)));
			String text = bodies.get(action);
			if (text != null) {
				int base = ACTION_LINES * (names.indexOf(action) + 1);
				List<CompileError> found = new ArrayList<>();
				List<Stmt> statements = Parser.parseStatements(text, base + 1, found);
				found.forEach(e -> errors.add(new CompileError(source, e.line(), e.column(), e.message())));
				body.add(when(line, member(line, control, "A"), statements));
			} else {
				List<CompileError> found = new ArrayList<>();
				Expr target = Parser.parseExpression(action, line, found);
				found.forEach(e -> errors.add(new CompileError(source, line, 1, "action '" + action + "': " + e.message())));
				if (target != null) {
					body.add(set(line, target, member(line, control, "Q")));
				}
			}
		});
	}
}
