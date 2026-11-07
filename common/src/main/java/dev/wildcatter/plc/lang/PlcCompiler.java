package dev.wildcatter.plc.lang;

import dev.wildcatter.plc.CpuSpec;
import dev.wildcatter.plc.Elementary;
import dev.wildcatter.plc.IoAddress;
import dev.wildcatter.plc.Op;
import dev.wildcatter.plc.PlcProgram;
import dev.wildcatter.plc.Tag;
import dev.wildcatter.plc.lang.Ast.Expr;
import dev.wildcatter.plc.lang.Ast.Stmt;
import dev.wildcatter.plc.lang.Ast.VarKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiles a project to the CPU's bytecode. Functions cannot recurse (the standard forbids it), so every variable has a fixed place: a
 * function's frame is one static block, and a function block's or program's variables are offsets from its instance.
 */
public final class PlcCompiler {
	public record Result(PlcProgram program, List<CompileError> errors) {
		public boolean ok() {
			return errors.isEmpty();
		}
	}

	/** Compiles one source. */
	public static Result compile(CpuSpec spec, String source) {
		return new PlcCompiler(spec).run(List.of(source));
	}

	private static final Ast.Unit STANDARD = parseStandard();

	private static Ast.Unit parseStandard() {
		List<CompileError> errors = new ArrayList<>();
		Ast.Unit unit = Parser.parse(StdLib.SOURCE, errors);
		if (!errors.isEmpty()) {
			throw new IllegalStateException("the standard library does not parse: " + errors);
		}
		return unit;
	}

	// ---- types -------------------------------------------------------------------------------------------------------------------------

	private sealed interface Type {
		int slots();

		String label();
	}

	private record Scalar(Elementary kind) implements Type {
		@Override
		public int slots() {
			return 1;
		}

		@Override
		public String label() {
			return kind.name();
		}
	}

	private record ArrayT(Type element, int[] low, int[] high) implements Type {
		int count() {
			int n = 1;
			for (int i = 0; i < low.length; i++) {
				n *= high[i] - low[i] + 1;
			}
			return n;
		}

		@Override
		public int slots() {
			return count() * element.slots();
		}

		@Override
		public String label() {
			return "ARRAY OF " + element.label();
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ArrayT a && element.equals(a.element) && Arrays.equals(low, a.low) && Arrays.equals(high, a.high);
		}

		@Override
		public int hashCode() {
			return element.hashCode() * 31 + Arrays.hashCode(low) + Arrays.hashCode(high);
		}
	}

	private static final class StructT implements Type {
		final String name;
		final Map<String, Field> fields = new LinkedHashMap<>();
		int size;

		StructT(String name) {
			this.name = name;
		}

		@Override
		public int slots() {
			return size;
		}

		@Override
		public String label() {
			return name;
		}
	}

	private record Field(String name, Type type, int offset, Ast.Init init) {
	}

	private record EnumT(String name, List<String> values) implements Type {
		@Override
		public int slots() {
			return 1;
		}

		@Override
		public String label() {
			return name;
		}
	}

	private static final class FbT implements Type {
		final Pou pou;

		FbT(Pou pou) {
			this.pou = pou;
		}

		@Override
		public int slots() {
			return pou.size;
		}

		@Override
		public String label() {
			return pou.name;
		}
	}

	private static final Scalar BOOL = new Scalar(Elementary.BOOL);
	private static final Scalar INT = new Scalar(Elementary.INT);
	private static final Scalar DINT = new Scalar(Elementary.DINT);
	private static final Scalar LINT = new Scalar(Elementary.LINT);
	private static final Scalar LREAL = new Scalar(Elementary.LREAL);
	private static final Scalar REAL = new Scalar(Elementary.REAL);
	private static final Scalar TIME = new Scalar(Elementary.TIME);
	private static final Scalar STRING = new Scalar(Elementary.STRING);
	private static final Scalar UNTYPED_INT = new Scalar(Elementary.UNTYPED_INT);
	private static final Scalar UNTYPED_REAL = new Scalar(Elementary.UNTYPED_REAL);

	private static final class Var {
		final Ast.VarDecl decl;
		final Type type;
		int offset;
		IoAddress at;

		Var(Ast.VarDecl decl, Type type) {
			this.decl = decl;
			this.type = type;
		}
	}

	private static final class Pou {
		final Ast.Pou ast;
		final String name;
		final int index;
		final Map<String, Var> vars = new LinkedHashMap<>();
		final Set<String> calls = new HashSet<>();
		int size;
		int frame = -1;
		int tempStart;
		int tempCount;
		int hiddenNext;
		boolean laidOut;
		boolean laying;
		Type returns;

		Pou(Ast.Pou ast, int index) {
			this.ast = ast;
			this.name = ast.name();
			this.index = index;
		}

		boolean isFunction() {
			return ast.kind() == Ast.PouKind.FUNCTION;
		}
	}

	// ---- state ------------------------------------------------------------------------------------------------------------------------

	private final CpuSpec spec;
	private final List<CompileError> errors = new ArrayList<>();
	private final Map<String, Ast.TypeDecl> typeDecls = new HashMap<>();
	private final Map<String, Type> types = new HashMap<>();
	private final Set<String> resolving = new HashSet<>();
	private final Map<String, Pou> pous = new LinkedHashMap<>();
	private final Map<String, Var> globals = new LinkedHashMap<>();
	private final Map<String, Ast.VarDecl> globalDecls = new HashMap<>();
	private final Map<String, EnumT> enumValues = new HashMap<>();
	private final Map<String, Boolean> ambiguousEnums = new HashMap<>();

	private long[] init = new long[1024];
	private String[] initStr = new String[1024];
	private boolean[] retained = new boolean[1024];
	private int top;

	private int[] code = new int[4096];
	private int[] lines = new int[4096];
	private int size;
	private int line;
	private final List<Long> constants = new ArrayList<>();
	private final Map<Long, Integer> constantIndex = new HashMap<>();
	private final List<String> strings = new ArrayList<>();
	private final Map<String, Integer> stringIndex = new HashMap<>();

	private Pou cur;
	private final List<List<Integer>> exits = new ArrayList<>();
	private final List<List<Integer>> continues = new ArrayList<>();

	private PlcCompiler(CpuSpec spec) {
		this.spec = spec;
	}

	private CompileException error(int atLine, String message) {
		return new CompileException(atLine, 1, message);
	}

	private void record(CompileException e) {
		errors.add(new CompileError(e.line, e.column, e.getMessage()));
	}

	// ---- the passes -------------------------------------------------------------------------------------------------------------------

	private Result run(List<String> sources) {
		List<Ast.Unit> units = new ArrayList<>();
		units.add(STANDARD);
		for (String source : sources) {
			units.add(Parser.parse(source, errors));
		}
		for (Ast.Unit unit : units) {
			for (Ast.TypeDecl decl : unit.types()) {
				if (typeDecls.putIfAbsent(decl.name(), decl) != null) {
					record(error(decl.line(), "the type " + decl.name() + " is declared twice"));
				}
			}
			for (Ast.Pou pou : unit.pous()) {
				if (elementary(pou.name()) != null) {
					record(error(pou.line(), pou.name() + " is the name of a type"));
				} else if (pous.containsKey(pou.name())) {
					record(error(pou.line(), pou.name() + " is already declared" + (units.getFirst().pous().stream().anyMatch(p -> p.name().equals(pou.name())) ? " in the standard library" : "")));
				} else {
					pous.put(pou.name(), new Pou(pou, pous.size()));
				}
			}
			for (Ast.VarDecl decl : unit.globals()) {
				globalDecls.put(decl.name(), decl);
			}
		}
		attempt(() -> {
			for (Ast.TypeDecl decl : typeDecls.values()) {
				attempt(() -> resolveNamed(decl.name(), decl.line()));
			}
		});
		for (Pou pou : pous.values()) {
			attempt(() -> layout(pou));
		}
		for (Ast.Unit unit : units) {
			for (Ast.VarDecl decl : unit.globals()) {
				attempt(() -> declareGlobal(decl));
			}
		}
		for (Pou pou : pous.values()) {
			if (pou.isFunction() && pou.laidOut) {
				attempt(() -> {
					pou.frame = alloc(pou.size, pou.ast.line());
					cur = pou;
					initMembers(pou, pou.frame, null, false);
					cur = null;
				});
			}
		}
		Map<String, Integer> instances = new LinkedHashMap<>();
		List<PlcProgram.Task> tasks = tasks(units, instances);
		if (errors.isEmpty()) {
			for (Pou pou : pous.values()) {
				generate(pou);
			}
			checkRecursion();
		}
		if (!errors.isEmpty()) {
			errors.sort((a, b) -> a.line() != b.line() ? a.line() - b.line() : a.column() - b.column());
			return new Result(null, List.copyOf(errors));
		}
		int[] entries = new int[pous.size()];
		String[] names = new String[pous.size()];
		for (Pou pou : pous.values()) {
			entries[pou.index] = entryOf[pou.index];
			names[pou.index] = pou.name;
		}
		long[] k = new long[constants.size()];
		for (int i = 0; i < k.length; i++) {
			k[i] = constants.get(i);
		}
		Map<String, Tag> tags = new HashMap<>();
		for (Map.Entry<String, Var> global : globals.entrySet()) {
			addTags(tags, global.getKey(), global.getValue().type, global.getValue().offset, global.getValue().at);
		}
		for (Map.Entry<String, Integer> instance : instances.entrySet()) {
			Pou pou = pous.get(instanceOf.get(instance.getKey()));
			addVarTags(tags, instance.getKey(), pou, instance.getValue(), true);
		}
		return new Result(new PlcProgram(Arrays.copyOf(code, size), Arrays.copyOf(lines, size), k, strings.toArray(String[]::new), Arrays.copyOf(init, top),
				Arrays.copyOf(initStr, top), Arrays.copyOf(retained, top), entries, names, tasks, Map.copyOf(tags)), List.of());
	}

	private void attempt(Runnable step) {
		try {
			step.run();
		} catch (CompileException e) {
			record(e);
		}
	}

	private final Map<String, String> instanceOf = new HashMap<>();
	private int[] entryOf = new int[0];

	private List<PlcProgram.Task> tasks(List<Ast.Unit> units, Map<String, Integer> instances) {
		Map<String, Ast.TaskDecl> declared = new LinkedHashMap<>();
		List<Ast.ProgramInstance> asked = new ArrayList<>();
		for (Ast.Unit unit : units) {
			for (Ast.TaskDecl task : unit.tasks()) {
				if (task.intervalMs() % dev.wildcatter.plc.Cpu.SCAN_MS != 0) {
					record(error(task.line(), "task " + task.name() + "'s interval must be a whole number of " + dev.wildcatter.plc.Cpu.SCAN_MS + " ms scans"));
				}
				declared.put(task.name(), task);
			}
			asked.addAll(unit.instances());
		}
		if (asked.isEmpty()) {
			for (Pou pou : pous.values()) {
				if (pou.ast.kind() == Ast.PouKind.PROGRAM) {
					asked.add(new Ast.ProgramInstance(pou.ast.line(), pou.name, null, pou.name));
				}
			}
		}
		Map<String, List<PlcProgram.Run>> runs = new LinkedHashMap<>();
		for (Ast.ProgramInstance instance : asked) {
			attempt(() -> {
				Pou pou = pous.get(instance.program());
				if (pou == null || pou.ast.kind() != Ast.PouKind.PROGRAM) {
					throw error(instance.line(), instance.program() + " is not a PROGRAM");
				}
				if (instance.task() != null && !declared.containsKey(instance.task())) {
					throw error(instance.line(), "there is no task " + instance.task());
				}
				if (!pou.laidOut) {
					return;
				}
				int slot = alloc(pou.size, instance.line());
				cur = pou;
				initMembers(pou, slot, null, false);
				cur = null;
				instances.put(instance.instance(), slot);
				instanceOf.put(instance.instance(), pou.name);
				runs.computeIfAbsent(instance.task() == null ? "" : instance.task(), k -> new ArrayList<>()).add(new PlcProgram.Run(instance.instance(), pou.index, slot));
			});
		}
		List<PlcProgram.Task> out = new ArrayList<>();
		runs.forEach((name, list) -> {
			Ast.TaskDecl task = declared.get(name);
			out.add(new PlcProgram.Task(name, task == null ? 0 : task.intervalMs(), task == null ? Integer.MAX_VALUE : task.priority(), List.copyOf(list)));
		});
		out.sort((a, b) -> a.priority() - b.priority());
		return out;
	}

	private void checkRecursion() {
		for (Pou pou : pous.values()) {
			if (reaches(pou, pou.name, new HashSet<>())) {
				record(error(pou.ast.line(), pou.name + " calls itself, which the standard forbids"));
			}
		}
	}

	private boolean reaches(Pou from, String target, Set<String> seen) {
		for (String called : from.calls) {
			if (called.equals(target) || (seen.add(called) && reaches(pous.get(called), target, seen))) {
				return true;
			}
		}
		return false;
	}

	// ---- declarations -----------------------------------------------------------------------------------------------------------------

	private static Elementary elementary(String name) {
		try {
			Elementary kind = Elementary.valueOf(name);
			return kind == Elementary.UNTYPED_INT || kind == Elementary.UNTYPED_REAL ? null : kind;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private Type resolveNamed(String name, int atLine) {
		Elementary kind = elementary(name);
		if (kind != null) {
			return new Scalar(kind);
		}
		Type known = types.get(name);
		if (known != null) {
			return known;
		}
		Ast.TypeDecl decl = typeDecls.get(name);
		if (decl != null) {
			if (!resolving.add(name)) {
				throw error(atLine, "the type " + name + " contains itself");
			}
			Type type = resolve(decl.type(), name);
			resolving.remove(name);
			types.put(name, type);
			return type;
		}
		Pou pou = pous.get(name);
		if (pou != null && pou.ast.kind() == Ast.PouKind.FUNCTION_BLOCK) {
			layout(pou);
			return new FbT(pou);
		}
		if (pou != null) {
			throw error(atLine, name + " is not a type or function block");
		}
		throw error(atLine, "there is no type " + name);
	}

	private Type resolve(Ast.TypeSpec spec, String named) {
		return switch (spec) {
			case Ast.NamedType n -> resolveNamed(n.name(), n.line());
			case Ast.ArrayType a -> {
				int[] low = new int[a.low().size()];
				int[] high = new int[a.low().size()];
				for (int i = 0; i < low.length; i++) {
					low[i] = (int) constInt(a.low().get(i));
					high[i] = (int) constInt(a.high().get(i));
					if (high[i] < low[i]) {
						throw error(a.line(), "an array's upper bound cannot be below its lower bound");
					}
				}
				yield new ArrayT(resolve(a.element(), null), low, high);
			}
			case Ast.StructType s -> {
				StructT out = new StructT(named == null ? "STRUCT" : named);
				for (Ast.VarDecl field : s.fields()) {
					Type type = resolve(field.type(), null);
					if (out.fields.containsKey(field.name())) {
						throw error(field.line(), "the member " + field.name() + " is declared twice");
					}
					out.fields.put(field.name(), new Field(field.name(), type, out.size, effectiveInit(field)));
					out.size += type.slots();
				}
				yield out;
			}
			case Ast.EnumType e -> {
				EnumT out = new EnumT(named == null ? "ENUM" : named, List.copyOf(e.values()));
				for (String value : e.values()) {
					if (enumValues.putIfAbsent(value, out) != null) {
						ambiguousEnums.put(value, true);
					}
				}
				yield out;
			}
		};
	}

	/** A variable with no initial value of its own takes its named type's. */
	private Ast.Init effectiveInit(Ast.VarDecl decl) {
		if (decl.init() != null) {
			return decl.init();
		}
		if (decl.type() instanceof Ast.NamedType named && typeDecls.containsKey(named.name())) {
			return typeDecls.get(named.name()).init();
		}
		return null;
	}

	private void layout(Pou pou) {
		if (pou.laidOut) {
			return;
		}
		if (pou.laying) {
			throw error(pou.ast.line(), "the function block " + pou.name + " contains itself");
		}
		pou.laying = true;
		Pou was = cur;
		cur = pou;
		int offset = 0;
		if (pou.isFunction()) {
			pou.returns = resolve(pou.ast.returns(), null);
			Ast.VarDecl self = new Ast.VarDecl(pou.ast.line(), 1, pou.name, VarKind.VAR, false, false, null, pou.ast.returns(), null);
			Var ret = new Var(self, pou.returns);
			ret.offset = 0;
			pou.vars.put(pou.name, ret);
			offset = pou.returns.slots();
		}
		List<Var> temps = new ArrayList<>();
		for (Ast.VarDecl decl : pou.ast.vars()) {
			if (decl.kind() == VarKind.EXTERNAL) {
				if (!globalDecls.containsKey(decl.name())) {
					record(error(decl.line(), decl.name() + " is VAR_EXTERNAL, but no VAR_GLOBAL declares it"));
				}
				continue;
			}
			try {
				if (pou.vars.containsKey(decl.name())) {
					throw error(decl.line(), decl.name() + " is declared twice in " + pou.name);
				}
				Var var = new Var(decl, resolve(decl.type(), null));
				if (decl.at() != null) {
					if (pou.ast.kind() != Ast.PouKind.PROGRAM) {
						throw error(decl.line(), "AT can only be used in a PROGRAM or VAR_GLOBAL, not a " + pou.ast.kind());
					}
					var.at = located(decl.at(), decl.line(), var.type);
				}
				if (decl.kind() == VarKind.TEMP) {
					temps.add(var);
				} else {
					var.offset = offset;
					offset += decl.kind() == VarKind.IN_OUT ? 1 : var.type.slots();
				}
				pou.vars.put(decl.name(), var);
			} catch (CompileException e) {
				record(e);
			}
		}
		pou.tempStart = offset;
		for (Var temp : temps) {
			temp.offset = offset;
			offset += temp.type.slots();
		}
		pou.tempCount = offset - pou.tempStart;
		pou.hiddenNext = offset;
		offset += hiddenSlots(pou.ast.body());
		pou.size = offset;
		pou.laying = false;
		pou.laidOut = true;
		cur = was;
	}

	private int hiddenSlots(List<Stmt> body) {
		int n = 0;
		for (Stmt stmt : body) {
			n += switch (stmt) {
				case Ast.For f -> 2 + hiddenSlots(f.body());
				case Ast.Case c -> {
					int k = 1 + hiddenSlots(c.otherwise());
					for (Ast.CaseArm arm : c.arms()) {
						k += hiddenSlots(arm.body());
					}
					yield k;
				}
				case Ast.If i -> {
					int k = hiddenSlots(i.otherwise());
					for (Ast.Branch branch : i.branches()) {
						k += hiddenSlots(branch.body());
					}
					yield k;
				}
				case Ast.While w -> hiddenSlots(w.body());
				case Ast.Repeat r -> hiddenSlots(r.body());
				default -> 0;
			};
		}
		return n;
	}

	private IoAddress located(String text, int atLine, Type type) {
		IoAddress io = ioAddress(text, atLine);
		if (!(type instanceof Scalar scalar) || scalar.kind() == Elementary.STRING || scalar.kind() == Elementary.TIME || scalar.kind().bits != (io.size() == 'X' ? 1 : io.bytes() * 8)) {
			throw error(atLine, text + " holds " + (io.size() == 'X' ? "1 bit" : io.bytes() * 8 + " bits") + ", which does not fit a " + type.label());
		}
		return io;
	}

	private IoAddress ioAddress(String text, int atLine) {
		IoAddress io = IoAddress.parse(text);
		if (io == null) {
			throw error(atLine, "'" + text + "' is not a direct address: write it as %IX0.3, %QW2 or %MD8");
		}
		int limit = io.area() == 'I' ? spec.inputBytes() : io.area() == 'Q' ? spec.outputBytes() : spec.markerBytes();
		if (io.byteAddress() + io.bytes() > limit) {
			throw error(atLine, text + " is beyond the " + limit + " bytes of this " + spec.name());
		}
		return io;
	}

	private void declareGlobal(Ast.VarDecl decl) {
		if (globals.containsKey(decl.name())) {
			throw error(decl.line(), "the global " + decl.name() + " is declared twice");
		}
		Var var = new Var(decl, resolve(decl.type(), null));
		if (decl.at() != null) {
			var.at = located(decl.at(), decl.line(), var.type);
		}
		var.offset = alloc(var.type.slots(), decl.line());
		initAt(var.offset, var.type, effectiveInit(decl), decl.retain(), decl.line());
		globals.put(decl.name(), var);
	}

	private int alloc(int slots, int atLine) {
		if (top + slots > spec.memorySlots()) {
			throw error(atLine, "the project needs more than the " + spec.memorySlots() + " memory slots of this " + spec.name());
		}
		int at = top;
		top += slots;
		while (top > init.length) {
			init = Arrays.copyOf(init, init.length * 2);
			initStr = Arrays.copyOf(initStr, initStr.length * 2);
			retained = Arrays.copyOf(retained, retained.length * 2);
		}
		return at;
	}

	private void initMembers(Pou pou, int base, Ast.StructInit override, boolean retain) {
		for (Var var : pou.vars.values()) {
			if (var.decl.kind() == VarKind.IN_OUT) {
				continue;
			}
			Ast.Init given = pou.isFunction() && var.decl.name().equals(pou.name) ? null : effectiveInit(var.decl);
			initAt(base + var.offset, var.type, overrideOf(override, var.decl.name(), given), retain || var.decl.retain(), var.decl.line());
		}
	}

	private static Ast.Init overrideOf(Ast.StructInit override, String name, Ast.Init fallback) {
		if (override != null) {
			int at = override.names().indexOf(name);
			if (at >= 0) {
				return override.values().get(at);
			}
		}
		return fallback;
	}

	private void initAt(int at, Type type, Ast.Init given, boolean retain, int atLine) {
		switch (type) {
			case Scalar scalar -> {
				retained[at] = retain;
				initStr[at] = scalar.kind() == Elementary.STRING ? "" : null;
				if (given != null) {
					if (!(given instanceof Ast.ExprInit e)) {
						throw error(atLine, "a " + scalar.label() + " takes one value");
					}
					Object value = constant(e.value());
					if (scalar.kind() == Elementary.STRING) {
						if (!(value instanceof String s)) {
							throw error(atLine, "a STRING takes a string");
						}
						initStr[at] = s;
					} else {
						init[at] = rawFor(value, scalar.kind(), atLine);
					}
				}
			}
			case EnumT en -> {
				retained[at] = retain;
				if (given != null) {
					Object value = given instanceof Ast.ExprInit e ? constant(e.value()) : null;
					if (!(value instanceof Long l)) {
						throw error(atLine, "an enumeration takes one of its values");
					}
					init[at] = l;
				}
			}
			case ArrayT array -> {
				int count = array.count();
				int stride = array.element().slots();
				List<Ast.Init> items = new ArrayList<>();
				if (given instanceof Ast.ArrayInit list) {
					for (int i = 0; i < list.items().size(); i++) {
						for (int k = 0; k < list.counts().get(i); k++) {
							items.add(list.items().get(i));
						}
					}
					if (items.size() > count) {
						throw error(atLine, "this array holds " + count + " values, but " + items.size() + " are given");
					}
				} else if (given != null) {
					throw error(atLine, "an array takes [ ... ]");
				}
				for (int i = 0; i < count; i++) {
					initAt(at + i * stride, array.element(), i < items.size() ? items.get(i) : null, retain, atLine);
				}
			}
			case StructT struct -> {
				Ast.StructInit override = structInit(given, atLine, struct.name);
				if (override != null) {
					for (String name : override.names()) {
						if (!struct.fields.containsKey(name)) {
							throw error(atLine, struct.name + " has no member " + name);
						}
					}
				}
				for (Field field : struct.fields.values()) {
					initAt(at + field.offset(), field.type(), overrideOf(override, field.name(), field.init()), retain, atLine);
				}
			}
			case FbT fb -> {
				Ast.StructInit override = structInit(given, atLine, fb.pou.name);
				if (override != null) {
					for (String name : override.names()) {
						if (!fb.pou.vars.containsKey(name)) {
							throw error(atLine, fb.pou.name + " has no input " + name);
						}
					}
				}
				Pou was = cur;
				cur = fb.pou;
				initMembers(fb.pou, at, override, retain);
				cur = was;
			}
		}
	}

	private Ast.StructInit structInit(Ast.Init given, int atLine, String name) {
		if (given == null) {
			return null;
		}
		if (given instanceof Ast.StructInit s) {
			return s;
		}
		throw error(atLine, "a " + name + " takes ( member := value, ... )");
	}

	private long rawFor(Object value, Elementary kind, int atLine) {
		if (kind == Elementary.BOOL) {
			if (value instanceof Boolean b) {
				return b ? 1 : 0;
			}
			throw error(atLine, "a BOOL takes TRUE or FALSE");
		}
		if (kind.isReal()) {
			if (value instanceof Number n) {
				return Double.doubleToRawLongBits(kind == Elementary.REAL ? (float) n.doubleValue() : n.doubleValue());
			}
			throw error(atLine, "a " + kind + " takes a number");
		}
		if (value instanceof TimeValue t) {
			if (kind == Elementary.TIME) {
				return t.ms;
			}
			throw error(atLine, "a " + kind + " cannot take a time");
		}
		if (value instanceof Long l) {
			if (kind == Elementary.TIME) {
				throw error(atLine, "a TIME takes a time such as T#5s");
			}
			if (!kind.holds(l)) {
				throw error(atLine, l + " does not fit a " + kind);
			}
			return l;
		}
		throw error(atLine, "that value does not fit a " + kind);
	}

	private record TimeValue(long ms) {
	}

	private long constInt(Expr e) {
		Object value = constant(e);
		if (value instanceof Long l) {
			return l;
		}
		throw error(e.line(), "this must be a constant whole number");
	}

	/** A constant's value: Long, Double, Boolean, String or TimeValue. */
	private Object constant(Expr e) {
		return switch (e) {
			case Ast.IntLit l -> l.value();
			case Ast.RealLit r -> r.value();
			case Ast.BoolLit b -> b.value();
			case Ast.TimeLit t -> new TimeValue(t.ms());
			case Ast.StringLit s -> s.value();
			case Ast.EnumLit en -> enumIndex(en);
			case Ast.Name n -> {
				Ast.VarDecl decl = null;
				if (cur != null) {
					for (Ast.VarDecl local : cur.ast.vars()) {
						if (local.name().equals(n.name())) {
							decl = local;
						}
					}
				}
				if (decl == null) {
					decl = globalDecls.get(n.name());
				}
				if (decl != null && decl.constant() && decl.init() instanceof Ast.ExprInit init) {
					yield constant(init.value());
				}
				EnumT en = enumValues.get(n.name());
				if (en != null) {
					yield (long) en.values().indexOf(n.name());
				}
				throw error(e.line(), n.name() + " is not a constant");
			}
			case Ast.Unary u -> {
				Object value = constant(u.value());
				if (u.op().equals("NOT") && value instanceof Boolean b) {
					yield !b;
				}
				if (u.op().equals("-") && value instanceof Long l) {
					yield -l;
				}
				if (u.op().equals("-") && value instanceof Double d) {
					yield -d;
				}
				if (u.op().equals("+") && value instanceof Number) {
					yield value;
				}
				throw error(e.line(), "this must be a constant");
			}
			case Ast.Binary b -> {
				Object l = constant(b.left());
				Object r = constant(b.right());
				if (l instanceof Long x && r instanceof Long y) {
					yield switch (b.op()) {
						case "+" -> x + y;
						case "-" -> x - y;
						case "*" -> x * y;
						case "/" -> {
							if (y == 0) {
								throw error(e.line(), "division by zero");
							}
							yield x / y;
						}
						default -> throw error(e.line(), "this must be a constant");
					};
				}
				if (l instanceof Number x && r instanceof Number y) {
					yield switch (b.op()) {
						case "+" -> x.doubleValue() + y.doubleValue();
						case "-" -> x.doubleValue() - y.doubleValue();
						case "*" -> x.doubleValue() * y.doubleValue();
						case "/" -> x.doubleValue() / y.doubleValue();
						default -> throw error(e.line(), "this must be a constant");
					};
				}
				throw error(e.line(), "this must be a constant");
			}
			default -> throw error(e.line(), "this must be a constant");
		};
	}

	private long enumIndex(Ast.EnumLit literal) {
		Type type = resolveNamed(literal.type(), literal.line());
		if (type instanceof EnumT en) {
			int at = en.values().indexOf(literal.value());
			if (at >= 0) {
				return at;
			}
			throw error(literal.line(), en.name() + " has no value " + literal.value());
		}
		throw error(literal.line(), literal.type() + " is not an enumeration");
	}

	private void addVarTags(Map<String, Tag> tags, String path, Pou pou, int base, boolean located) {
		for (Var var : pou.vars.values()) {
			if (var.decl.kind() != VarKind.IN_OUT) {
				addTags(tags, path + "." + var.decl.name(), var.type, base + var.offset, located ? var.at : null);
			}
		}
	}

	private void addTags(Map<String, Tag> tags, String path, Type type, int at, IoAddress io) {
		switch (type) {
			case Scalar s -> tags.put(path, io != null ? new Tag(path, s.kind(), -1, io.area(), io.byteAddress(), io.bit()) : new Tag(path, s.kind(), at, (char) 0, 0, -1));
			case EnumT e -> tags.put(path, new Tag(path, Elementary.INT, at, (char) 0, 0, -1));
			case StructT s -> s.fields.values().forEach(f -> addTags(tags, path + "." + f.name(), f.type(), at + f.offset(), null));
			case FbT f -> addVarTags(tags, path, f.pou, at, false);
			case ArrayT a -> {
				if (a.low().length == 1) {
					for (int i = a.low()[0]; i <= a.high()[0]; i++) {
						addTags(tags, path + "[" + i + "]", a.element(), at + (i - a.low()[0]) * a.element().slots(), null);
					}
				}
			}
		}
	}

	// ---- emitting ---------------------------------------------------------------------------------------------------------------------

	private void put(int value) {
		if (size == code.length) {
			code = Arrays.copyOf(code, size * 2);
			lines = Arrays.copyOf(lines, size * 2);
		}
		lines[size] = line;
		code[size++] = value;
	}

	private void op(int op) {
		put(op);
	}

	private void op(int op, int a) {
		put(op);
		put(a);
	}

	private void op(int op, int a, int b) {
		put(op);
		put(a);
		put(b);
	}

	private void pushLong(long value) {
		if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
			op(Op.PUSHI, (int) value);
		} else {
			op(Op.PUSH, constantIndex.computeIfAbsent(value, v -> {
				constants.add(v);
				return constants.size() - 1;
			}));
		}
	}

	private void pushString(String value) {
		op(Op.SPUSH, stringIndex.computeIfAbsent(value, v -> {
			strings.add(v);
			return strings.size() - 1;
		}));
	}

	private int jump(int op) {
		put(op);
		put(-1);
		return size - 1;
	}

	private void land(int patch) {
		code[patch] = size;
	}

	private void land(List<Integer> patches) {
		for (int patch : patches) {
			code[patch] = size;
		}
	}

	private void wrap(Elementary kind) {
		if (kind == Elementary.REAL) {
			op(Op.FSINGLE);
		} else if (kind.bits < 64 && (kind.isInt() || kind.isBits())) {
			op(Op.WRAP, kind.ordinal());
		}
	}

	// ---- a POU's code -----------------------------------------------------------------------------------------------------------------

	private void generate(Pou pou) {
		if (entryOf.length < pous.size()) {
			entryOf = new int[pous.size()];
		}
		entryOf[pou.index] = size;
		cur = pou;
		line = pou.ast.line();
		if (!pou.isFunction() && pou.tempCount > 0) {
			op(Op.COPYINITB, pou.tempStart, pou.tempCount);
		}
		statements(pou.ast.body());
		line = pou.ast.line();
		op(Op.RET);
		cur = null;
	}

	private void statements(List<Stmt> body) {
		for (Stmt stmt : body) {
			line = stmt.line();
			try {
				statement(stmt);
			} catch (CompileException e) {
				record(e);
			}
		}
	}

	private void statement(Stmt stmt) {
		switch (stmt) {
			case Ast.Assign a -> assign(a);
			case Ast.CallStmt c -> callStatement(c.call());
			case Ast.If i -> ifStatement(i);
			case Ast.Case c -> caseStatement(c);
			case Ast.For f -> forStatement(f);
			case Ast.While w -> {
				int top = size;
				loopOpen();
				gen(w.condition(), BOOL);
				int out = jump(Op.JZ);
				statements(w.body());
				land(continues.getLast());
				op(Op.JMP, top);
				land(out);
				loopClose();
			}
			case Ast.Repeat r -> {
				int top = size;
				loopOpen();
				statements(r.body());
				land(continues.getLast());
				line = r.until().line();
				gen(r.until(), BOOL);
				op(Op.JZ, top);
				loopClose();
			}
			case Ast.Exit e -> {
				if (exits.isEmpty()) {
					throw error(e.line(), "EXIT belongs in a loop");
				}
				exits.getLast().add(jump(Op.JMP));
			}
			case Ast.Continue e -> {
				if (continues.isEmpty()) {
					throw error(e.line(), "CONTINUE belongs in a loop");
				}
				continues.getLast().add(jump(Op.JMP));
			}
			case Ast.Return r -> op(Op.RET);
		}
	}

	private void loopOpen() {
		exits.add(new ArrayList<>());
		continues.add(new ArrayList<>());
	}

	private void loopClose() {
		land(exits.removeLast());
		continues.removeLast();
	}

	private void assign(Ast.Assign a) {
		Place place = writable(a.target());
		place = prepareStore(place);
		gen(a.value(), place.type);
		store(place);
	}

	/** A place to write: not a constant, an input, or an output of something else. */
	private Place writable(Expr target) {
		if (target instanceof Ast.Name n) {
			Var var = lookup(n.name());
			if (var != null) {
				if (var.decl.constant()) {
					throw error(n.line(), n.name() + " is a constant");
				}
				if (var.decl.kind() == VarKind.INPUT) {
					throw error(n.line(), n.name() + " is an input of " + cur.name + ", so only its caller can set it");
				}
			}
		}
		if (target instanceof Ast.Member m && typeOf(m.base()) instanceof FbT fb) {
			Var var = fb.pou.vars.get(m.field());
			if (var != null && var.decl.kind() == VarKind.OUTPUT) {
				throw error(m.line(), m.field() + " is an output of " + fb.pou.name + ", so it cannot be written from outside");
			}
		}
		if (target instanceof Ast.Direct d && IoAddress.parse(d.address()) != null && IoAddress.parse(d.address()).area() == 'I') {
			throw error(d.line(), d.address() + " is an input, which a program can only read");
		}
		Place place = place(target);
		if (place.kind == IO && place.area == Op.AREA_I) {
			throw error(target.line(), "that variable is on an input, which a program can only read");
		}
		return place;
	}

	private void ifStatement(Ast.If i) {
		List<Integer> ends = new ArrayList<>();
		for (Ast.Branch branch : i.branches()) {
			line = branch.condition().line();
			gen(branch.condition(), BOOL);
			int next = jump(Op.JZ);
			statements(branch.body());
			ends.add(jump(Op.JMP));
			land(next);
		}
		statements(i.otherwise());
		land(ends);
	}

	private void caseStatement(Ast.Case c) {
		Type selector = typeOf(c.selector());
		boolean whole = selector instanceof EnumT || selector instanceof Scalar s && (s.kind().isInt() || s.kind().isBits());
		if (!whole) {
			throw error(c.line(), "CASE takes a whole number or enumeration, not a " + selector.label());
		}
		int hidden = cur.hiddenNext++;
		gen(c.selector(), selector instanceof EnumT ? selector : LINT);
		storeLocal(hidden);
		List<Integer> ends = new ArrayList<>();
		for (Ast.CaseArm arm : c.arms()) {
			List<Integer> into = new ArrayList<>();
			for (Ast.CaseLabel label : arm.labels()) {
				long low = caseValue(label.low(), selector);
				if (label.high() == null) {
					loadLocal(hidden);
					pushLong(low);
					op(Op.IEQ);
				} else {
					loadLocal(hidden);
					pushLong(low);
					op(Op.IGE);
					loadLocal(hidden);
					pushLong(caseValue(label.high(), selector));
					op(Op.ILE);
					op(Op.AND);
				}
				into.add(jump(Op.JNZ));
			}
			int next = jump(Op.JMP);
			land(into);
			statements(arm.body());
			ends.add(jump(Op.JMP));
			land(next);
		}
		statements(c.otherwise());
		land(ends);
	}

	private long caseValue(Expr label, Type selector) {
		if (selector instanceof EnumT en && label instanceof Ast.Name n) {
			int at = en.values().indexOf(n.name());
			if (at >= 0) {
				return at;
			}
			throw error(label.line(), en.name() + " has no value " + n.name());
		}
		return constInt(label);
	}

	private void forStatement(Ast.For f) {
		Type type = typeOf(f.variable());
		if (!(type instanceof Scalar s) || !s.kind().isInt()) {
			throw error(f.line(), "a FOR loop's variable must be a whole number");
		}
		Place variable = writable(f.variable());
		if (variable.kind != ABS && variable.kind != BASE) {
			throw error(f.line(), "a FOR loop's variable must be a plain variable");
		}
		int end = cur.hiddenNext++;
		int step = cur.hiddenNext++;
		gen(f.from(), type);
		store(variable);
		gen(f.to(), LINT);
		storeLocal(end);
		if (f.by() == null) {
			pushLong(1);
		} else {
			gen(f.by(), LINT);
		}
		storeLocal(step);
		int top = size;
		loopOpen();
		loadLocal(step);
		pushLong(0);
		op(Op.ILT);
		int negative = jump(Op.JNZ);
		load(variable);
		loadLocal(end);
		op(Op.ILE);
		int outUp = jump(Op.JZ);
		int body = jump(Op.JMP);
		land(negative);
		load(variable);
		loadLocal(end);
		op(Op.IGE);
		int outDown = jump(Op.JZ);
		land(body);
		statements(f.body());
		land(continues.getLast());
		line = f.line();
		load(variable);
		loadLocal(step);
		op(Op.IADD);
		wrap(s.kind());
		store(variable);
		op(Op.JMP, top);
		land(outUp);
		land(outDown);
		loopClose();
	}

	private void loadLocal(int offset) {
		if (cur.isFunction()) {
			op(Op.LOADA, cur.frame + offset);
		} else {
			op(Op.LOADB, offset);
		}
	}

	private void storeLocal(int offset) {
		if (cur.isFunction()) {
			op(Op.STOREA, cur.frame + offset);
		} else {
			op(Op.STOREB, offset);
		}
	}

	// ---- places -----------------------------------------------------------------------------------------------------------------------

	private static final int ABS = 0, BASE = 1, STACK = 2, IO = 3, BIT = 4;

	private static final class Place {
		final int kind;
		final int a;
		final Type type;
		int area;
		char size;
		int byteAt;
		int bit;

		Place(int kind, int a, Type type) {
			this.kind = kind;
			this.a = a;
			this.type = type;
		}
	}

	private Var lookup(String name) {
		Var var = cur == null ? null : cur.vars.get(name);
		return var != null ? var : globals.get(name);
	}

	private Place ioPlace(IoAddress io, Type type) {
		Place place = new Place(IO, 0, type);
		place.area = io.area() == 'I' ? Op.AREA_I : io.area() == 'Q' ? Op.AREA_Q : Op.AREA_M;
		place.size = io.size();
		place.byteAt = io.byteAddress();
		place.bit = Math.max(io.bit(), 0);
		return place;
	}

	private void address(Place p) {
		switch (p.kind) {
			case ABS -> op(Op.PUSHI, p.a);
			case BASE -> op(Op.ADDRB, p.a);
			case STACK -> {
			}
			default -> throw new CompileException(line, 1, "that has no address in memory: it is on an I/O module or is one bit");
		}
	}

	/** Resolves an expression to where it lives. A dynamic address, from an index or IN_OUT, is left on the stack. */
	private Place place(Expr e) {
		switch (e) {
			case Ast.Name n -> {
				Var var = lookup(n.name());
				if (var == null) {
					throw error(n.line(), n.name() + " is not declared");
				}
				if (var.at != null) {
					return ioPlace(var.at, var.type);
				}
				if (cur != null && cur.vars.get(n.name()) == var) {
					if (var.decl.kind() == VarKind.IN_OUT) {
						loadLocal(var.offset);
						return new Place(STACK, 0, var.type);
					}
					return cur.isFunction() ? new Place(ABS, cur.frame + var.offset, var.type) : new Place(BASE, var.offset, var.type);
				}
				return new Place(ABS, var.offset, var.type);
			}
			case Ast.Direct d -> {
				IoAddress io = ioAddress(d.address(), d.line());
				return ioPlace(io, new Scalar(io.type()));
			}
			case Ast.Member m -> {
				Place base = place(m.base());
				int offset;
				Type type;
				switch (base.type) {
					case StructT s -> {
						Field field = s.fields.get(m.field());
						if (field == null) {
							throw error(m.line(), s.name + " has no member " + m.field());
						}
						offset = field.offset();
						type = field.type();
					}
					case FbT fb -> {
						Var var = fb.pou.vars.get(m.field());
						if (var == null) {
							throw error(m.line(), fb.pou.name + " has no " + m.field());
						}
						if (var.decl.kind() != VarKind.INPUT && var.decl.kind() != VarKind.OUTPUT) {
							throw error(m.line(), m.field() + " is private to " + fb.pou.name);
						}
						offset = var.offset;
						type = var.type;
					}
					default -> throw error(m.line(), "a " + base.type.label() + " has no members");
				}
				return member(base, offset, type);
			}
			case Ast.Index x -> {
				Place base = place(x.base());
				if (!(base.type instanceof ArrayT array)) {
					throw error(x.line(), "a " + base.type.label() + " is not an array");
				}
				if (x.index().size() != array.low().length) {
					throw error(x.line(), "this array has " + array.low().length + " dimension(s), and " + x.index().size() + " are given");
				}
				address(base);
				int stride = array.element().slots();
				int[] strides = new int[array.low().length];
				for (int i = strides.length - 1; i >= 0; i--) {
					strides[i] = stride;
					stride *= array.high()[i] - array.low()[i] + 1;
				}
				for (int i = 0; i < strides.length; i++) {
					gen(x.index().get(i), LINT);
					op(Op.BOUNDS, array.low()[i], array.high()[i]);
					pushLong(strides[i]);
					op(Op.IMUL);
					op(Op.IADD);
				}
				return new Place(STACK, 0, array.element());
			}
			case Ast.BitOf b -> {
				Place base = place(b.base());
				if (!(base.type instanceof Scalar s) || !(s.kind().isInt() || s.kind().isBits()) || b.bit() < 0 || b.bit() >= s.kind().bits) {
					throw error(b.line(), "bit " + b.bit() + " does not exist on a " + base.type.label());
				}
				if (base.kind == IO) {
					throw error(b.line(), "write the bit's own address, such as %IX0.3");
				}
				address(base);
				Place place = new Place(BIT, b.bit(), BOOL);
				return place;
			}
			default -> throw error(e.line(), "this is a value, not a variable");
		}
	}

	private Place member(Place base, int offset, Type type) {
		return switch (base.kind) {
			case ABS -> new Place(ABS, base.a + offset, type);
			case BASE -> new Place(BASE, base.a + offset, type);
			case STACK -> {
				if (offset != 0) {
					pushLong(offset);
					op(Op.IADD);
				}
				yield new Place(STACK, 0, type);
			}
			default -> throw new CompileException(line, 1, "that has no members");
		};
	}

	private static boolean composite(Type type) {
		return type instanceof ArrayT || type instanceof StructT || type instanceof FbT;
	}

	private static boolean isString(Type type) {
		return type instanceof Scalar s && s.kind() == Elementary.STRING;
	}

	/** Strings and composites write through an address, which has to be on the stack before their value. */
	private Place prepareStore(Place p) {
		if ((composite(p.type) || isString(p.type)) && (p.kind == ABS || p.kind == BASE)) {
			address(p);
			return new Place(STACK, 0, p.type);
		}
		return p;
	}

	private void load(Place p) {
		if (composite(p.type)) {
			address(p);
			return;
		}
		if (isString(p.type)) {
			address(p);
			op(Op.SLOAD);
			return;
		}
		switch (p.kind) {
			case ABS -> op(Op.LOADA, p.a);
			case BASE -> op(Op.LOADB, p.a);
			case STACK -> op(Op.LOADI);
			case BIT -> {
				op(Op.LOADI);
				op(Op.GETBIT, p.a);
			}
			default -> {
				put(Op.LOADIO);
				put(p.area);
				put(p.size);
				put(p.byteAt);
				put(p.bit);
				if (p.type instanceof Scalar s) {
					if (s.kind() == Elementary.REAL) {
						op(Op.BITS2F);
					} else if (s.kind() != Elementary.LREAL && s.kind() != Elementary.BOOL) {
						wrap(s.kind());
					}
				}
			}
		}
	}

	private void store(Place p) {
		if (composite(p.type)) {
			op(Op.COPY, p.type.slots());
			return;
		}
		if (isString(p.type)) {
			op(Op.SSTORE);
			return;
		}
		switch (p.kind) {
			case ABS -> op(Op.STOREA, p.a);
			case BASE -> op(Op.STOREB, p.a);
			case STACK -> op(Op.STOREI);
			case BIT -> op(Op.SETBIT, p.a);
			default -> {
				if (p.type instanceof Scalar s && s.kind() == Elementary.REAL) {
					op(Op.F2BITS);
				}
				put(Op.STOREIO);
				put(p.area);
				put(p.size);
				put(p.byteAt);
				put(p.bit);
			}
		}
	}

	// ---- expression types -------------------------------------------------------------------------------------------------------------

	private Type typeOf(Expr e) {
		return switch (e) {
			case Ast.IntLit l -> l.type() != null ? literalType(l.type(), l.line()) : UNTYPED_INT;
			case Ast.RealLit r -> r.type() != null ? literalType(r.type(), r.line()) : UNTYPED_REAL;
			case Ast.BoolLit b -> BOOL;
			case Ast.TimeLit t -> TIME;
			case Ast.StringLit s -> STRING;
			case Ast.EnumLit en -> resolveNamed(en.type(), en.line());
			case Ast.Name n -> {
				if (n.name().equals("_FIRST_SCAN")) {
					yield BOOL;
				}
				Var var = lookup(n.name());
				if (var != null) {
					yield var.type;
				}
				EnumT en = enumValues.get(n.name());
				if (en != null) {
					if (ambiguousEnums.containsKey(n.name())) {
						throw error(n.line(), n.name() + " is in more than one enumeration: write it as Type#" + n.name());
					}
					yield en;
				}
				throw error(n.line(), n.name() + " is not declared");
			}
			case Ast.Member m -> {
				Type base = typeOf(m.base());
				if (base instanceof StructT s && s.fields.containsKey(m.field())) {
					yield s.fields.get(m.field()).type();
				}
				if (base instanceof FbT fb && fb.pou.vars.containsKey(m.field())) {
					yield fb.pou.vars.get(m.field()).type;
				}
				throw error(m.line(), "a " + base.label() + " has no member " + m.field());
			}
			case Ast.Index x -> {
				Type base = typeOf(x.base());
				if (base instanceof ArrayT a) {
					yield a.element();
				}
				throw error(x.line(), "a " + base.label() + " is not an array");
			}
			case Ast.BitOf b -> BOOL;
			case Ast.Direct d -> new Scalar(ioAddress(d.address(), d.line()).type());
			case Ast.Unary u -> {
				Type value = typeOf(u.value());
				if (u.op().equals("NOT") && value instanceof Scalar s && (s.kind() == Elementary.BOOL || s.kind().isBits())) {
					yield value;
				}
				if (!u.op().equals("NOT") && value instanceof Scalar s && (s.kind().isNumeric() || s.kind() == Elementary.TIME)) {
					yield value;
				}
				throw error(u.line(), u.op() + " does not apply to a " + value.label());
			}
			case Ast.Binary b -> binaryType(b);
			case Ast.Call c -> callType(c);
		};
	}

	private Scalar literalType(String name, int atLine) {
		Elementary kind = elementary(name);
		if (kind == null) {
			throw error(atLine, name + " is not an elementary type");
		}
		return new Scalar(kind);
	}

	private Type binaryType(Ast.Binary b) {
		Type l = typeOf(b.left());
		Type r = typeOf(b.right());
		switch (b.op()) {
			case "AND", "OR", "XOR" -> {
				Type c = common(l, r, b.line());
				if (c instanceof Scalar s && (s.kind() == Elementary.BOOL || s.kind().isBits() || s.kind() == Elementary.UNTYPED_INT)) {
					return c;
				}
				throw error(b.line(), b.op() + " takes BOOL or bit strings, not a " + c.label());
			}
			case "=", "<>", "<", ">", "<=", ">=" -> {
				Type c = common(l, r, b.line());
				if (!(c instanceof Scalar) && !(c instanceof EnumT)) {
					throw error(b.line(), "a " + c.label() + " cannot be compared");
				}
				return BOOL;
			}
			default -> {
				if (l.equals(TIME) || r.equals(TIME)) {
					return timeArithmetic(b, l, r);
				}
				Type c = common(l, r, b.line());
				if (!(c instanceof Scalar s) || !s.kind().isNumeric()) {
					throw error(b.line(), b.op() + " takes numbers, not a " + c.label());
				}
				if (b.op().equals("MOD") && s.kind().isReal()) {
					throw error(b.line(), "MOD takes whole numbers");
				}
				if (b.op().equals("**") && !s.kind().isReal()) {
					return c == UNTYPED_INT ? LREAL : LREAL;
				}
				return c;
			}
		}
	}

	private Type timeArithmetic(Ast.Binary b, Type l, Type r) {
		boolean bothTime = l.equals(TIME) && r.equals(TIME);
		boolean scaled = (l.equals(TIME) && r instanceof Scalar s && s.kind().isNumeric()) || (r.equals(TIME) && b.op().equals("*") && l instanceof Scalar t && t.kind().isNumeric());
		if ((b.op().equals("+") || b.op().equals("-")) && bothTime) {
			return TIME;
		}
		if ((b.op().equals("*") || b.op().equals("/")) && scaled) {
			return TIME;
		}
		throw error(b.line(), "a TIME cannot do " + b.op() + " with a " + (l.equals(TIME) ? r : l).label());
	}

	private Type common(Type a, Type b, int atLine) {
		if (a.equals(b) || a == b) {
			return a;
		}
		if (a instanceof Scalar x && b instanceof Scalar y) {
			Elementary p = x.kind();
			Elementary q = y.kind();
			if (p == Elementary.UNTYPED_INT && (q.isNumeric() || q.isBits())) {
				return b;
			}
			if (q == Elementary.UNTYPED_INT && (p.isNumeric() || p.isBits())) {
				return a;
			}
			if (p == Elementary.UNTYPED_REAL && q.isReal()) {
				return b;
			}
			if (q == Elementary.UNTYPED_REAL && p.isReal()) {
				return a;
			}
			if (p == Elementary.UNTYPED_REAL && q.isInt()) {
				return q.widensTo(Elementary.REAL) ? REAL : LREAL;
			}
			if (q == Elementary.UNTYPED_REAL && p.isInt()) {
				return p.widensTo(Elementary.REAL) ? REAL : LREAL;
			}
			if (p.widensTo(q)) {
				return b;
			}
			if (q.widensTo(p)) {
				return a;
			}
		}
		throw error(atLine, "a " + a.label() + " and a " + b.label() + " do not mix: convert one with a function such as " + a.label() + "_TO_" + b.label());
	}

	/** A call on a variable: a function block instance. Inside a function its own name is its return variable, but a call to that name is a call. */
	private boolean instanceCall(Ast.Call c) {
		return c.name() == null || (lookup(c.name()) != null && !(cur != null && cur.isFunction() && c.name().equals(cur.name)));
	}

	private Type callType(Ast.Call c) {
		if (!instanceCall(c)) {
			Pou pou = pous.get(c.name());
			if (pou != null && pou.isFunction()) {
				return pou.returns;
			}
			if (pou != null) {
				throw error(c.line(), c.name() + " is a " + pou.ast.kind() + ": declare an instance and call that");
			}
			Scalar conversionTarget = conversionTarget(c.name());
			if (conversionTarget != null) {
				return conversionTarget;
			}
			return genericType(c);
		}
		throw error(c.line(), "a function block's call has no value: read its output, as fb.Q");
	}

	private Scalar conversionTarget(String name) {
		int at = name.indexOf("_TO_");
		if (at <= 0) {
			return null;
		}
		Elementary from = elementary(name.substring(0, at));
		Elementary to = elementary(name.substring(at + 4));
		return from == null || to == null ? null : new Scalar(to);
	}

	private Type genericType(Ast.Call c) {
		List<Ast.Arg> args = c.args();
		switch (c.name()) {
			case "_NOW" -> {
				return TIME;
			}
			case "ABS", "NEG", "MOVE" -> {
				return args(c, 1, 1).getFirst();
			}
			case "SQRT", "LN", "LOG", "EXP", "SIN", "COS", "TAN", "ASIN", "ACOS", "ATAN" -> {
				return realResult(args(c, 1, 1).getFirst());
			}
			case "EXPT" -> {
				return realResult(args(c, 2, 2).getFirst());
			}
			case "ADD", "SUB", "MUL", "DIV", "MOD", "MIN", "MAX" -> {
				List<Type> types = args(c, 2, 64);
				Type out = types.getFirst();
				for (Type t : types) {
					out = common(out, t, c.line());
				}
				return out;
			}
			case "LIMIT" -> {
				List<Type> types = args(c, 3, 3);
				return common(common(types.get(0), types.get(1), c.line()), types.get(2), c.line());
			}
			case "SEL" -> {
				List<Type> types = args(c, 3, 3);
				return common(types.get(1), types.get(2), c.line());
			}
			case "MUX" -> {
				List<Type> types = args(c, 2, 64);
				Type out = types.get(1);
				for (Type t : types.subList(1, types.size())) {
					out = common(out, t, c.line());
				}
				return out;
			}
			case "TRUNC" -> {
				args(c, 1, 1);
				return DINT;
			}
			case "SHL", "SHR", "ROL", "ROR" -> {
				return args(c, 2, 2).getFirst();
			}
			case "LEN", "FIND" -> {
				args(c, 1, 2);
				return INT;
			}
			case "LEFT", "RIGHT", "MID", "CONCAT", "INSERT", "DELETE", "REPLACE" -> {
				args(c, 1, 64);
				return STRING;
			}
			default -> throw error(c.line(), "there is no function " + c.name());
		}
	}

	private Type realResult(Type argument) {
		return argument.equals(REAL) ? REAL : argument.equals(UNTYPED_REAL) || argument.equals(UNTYPED_INT) ? UNTYPED_REAL : LREAL;
	}

	private List<Type> args(Ast.Call c, int least, int most) {
		if (c.args().size() < least || c.args().size() > most) {
			throw error(c.line(), c.name() + " takes " + (least == most ? least : least + " to " + most) + " value(s), and " + c.args().size() + " are given");
		}
		List<Type> out = new ArrayList<>();
		for (Ast.Arg arg : c.args()) {
			if (arg.output()) {
				throw error(c.line(), c.name() + " has no outputs");
			}
			out.add(typeOf(arg.value()));
		}
		return out;
	}

	// ---- expression code --------------------------------------------------------------------------------------------------------------

	/** Emits {@code e} and converts it to {@code want}, or to its own type when {@code want} is null. */
	private Type gen(Expr e, Type want) {
		Type actual = emit(e, want);
		if (want == null) {
			return actual;
		}
		coerce(actual, want, e.line());
		return want;
	}

	private void coerce(Type from, Type to, int atLine) {
		if (from.equals(to) || from == to) {
			return;
		}
		if (from instanceof Scalar x && to instanceof Scalar y && x.kind().widensTo(y.kind())) {
			if (x.kind().isInt() && y.kind().isReal()) {
				op(Op.I2F);
				wrap(y.kind());
			}
			return;
		}
		String conversion = from.label() + "_TO_" + to.label();
		throw error(atLine, "a " + from.label() + " does not fit a " + to.label() + (from instanceof Scalar && to instanceof Scalar ? ": convert it with " + conversion : ""));
	}

	private Type emit(Expr e, Type want) {
		switch (e) {
			case Ast.IntLit l -> {
				return intLiteral(l, want);
			}
			case Ast.RealLit r -> {
				Elementary kind = r.type() != null ? literalType(r.type(), r.line()).kind() : want instanceof Scalar s && s.kind().isReal() ? s.kind() : Elementary.LREAL;
				if (!kind.isReal()) {
					throw error(r.line(), "a real number does not fit a " + kind);
				}
				pushLong(Double.doubleToRawLongBits(kind == Elementary.REAL ? (float) r.value() : r.value()));
				return new Scalar(kind);
			}
			case Ast.BoolLit b -> {
				pushLong(b.value() ? 1 : 0);
				return BOOL;
			}
			case Ast.TimeLit t -> {
				pushLong(t.ms());
				return TIME;
			}
			case Ast.StringLit s -> {
				pushString(s.value());
				return STRING;
			}
			case Ast.EnumLit en -> {
				pushLong(enumIndex(en));
				return resolveNamed(en.type(), en.line());
			}
			case Ast.Name n when n.name().equals("_FIRST_SCAN") -> {
				op(Op.FIRST);
				return BOOL;
			}
			case Ast.Name n when lookup(n.name()) == null -> {
				EnumT en = (EnumT) typeOf(n);
				pushLong(en.values().indexOf(n.name()));
				return en;
			}
			case Ast.Unary u -> {
				return unary(u, want);
			}
			case Ast.Binary b -> {
				return binary(b, want);
			}
			case Ast.Call c -> {
				return call(c, want);
			}
			default -> {
				Place place = place(e);
				load(place);
				return place.type;
			}
		}
	}

	private Type intLiteral(Ast.IntLit l, Type want) {
		if (l.type() != null) {
			Scalar typed = literalType(l.type(), l.line());
			if (typed.kind().isReal()) {
				pushLong(Double.doubleToRawLongBits((double) l.value()));
				return typed;
			}
			if (typed.kind() == Elementary.BOOL && (l.value() == 0 || l.value() == 1)) {
				pushLong(l.value());
				return BOOL;
			}
			if (!typed.kind().holds(l.value())) {
				throw error(l.line(), l.value() + " does not fit a " + typed.kind());
			}
			pushLong(l.value());
			return typed;
		}
		if (want instanceof Scalar s && s.kind().isReal()) {
			pushLong(Double.doubleToRawLongBits((double) l.value()));
			return want;
		}
		if (want instanceof Scalar s && (s.kind().isInt() || s.kind().isBits())) {
			if (!s.kind().holds(l.value())) {
				throw error(l.line(), l.value() + " does not fit a " + s.kind());
			}
			pushLong(l.value());
			return want;
		}
		pushLong(l.value());
		return Elementary.DINT.holds(l.value()) ? DINT : LINT;
	}

	private Type unary(Ast.Unary u, Type want) {
		if (u.op().equals("-") && u.value() instanceof Ast.IntLit l && l.type() == null) {
			return intLiteral(new Ast.IntLit(l.line(), -l.value(), null), want);
		}
		Type type = typeOf(u.value());
		Type target = settle(type, want);
		if (u.op().equals("+")) {
			return gen(u.value(), target);
		}
		gen(u.value(), target);
		Elementary kind = ((Scalar) target).kind();
		if (u.op().equals("NOT")) {
			if (kind == Elementary.BOOL) {
				op(Op.LNOT);
			} else {
				op(Op.BNOT);
				wrap(kind);
			}
		} else if (kind.isReal()) {
			op(Op.FNEG);
		} else {
			op(Op.INEG);
			wrap(kind);
		}
		return target;
	}

	/** An untyped literal expression takes the type wanted, if a number is wanted; otherwise DINT or LREAL. */
	private Type settle(Type type, Type want) {
		if (type == UNTYPED_INT || type.equals(UNTYPED_INT)) {
			return want instanceof Scalar s && (s.kind().isNumeric() || s.kind().isBits()) ? want : DINT;
		}
		if (type == UNTYPED_REAL || type.equals(UNTYPED_REAL)) {
			return want instanceof Scalar s && s.kind().isReal() ? want : LREAL;
		}
		return type;
	}

	private Type binary(Ast.Binary b, Type want) {
		Type l = typeOf(b.left());
		Type r = typeOf(b.right());
		String opName = b.op();
		switch (opName) {
			case "AND", "OR", "XOR" -> {
				Type c = settle(binaryType(b), want);
				gen(b.left(), c);
				gen(b.right(), c);
				op(opName.equals("AND") ? Op.AND : opName.equals("OR") ? Op.OR : Op.XOR);
				return c;
			}
			case "=", "<>", "<", ">", "<=", ">=" -> {
				Type c = settle(common(l, r, b.line()), null);
				gen(b.left(), c);
				gen(b.right(), c);
				compare(opName, c);
				return BOOL;
			}
			default -> {
			}
		}
		Type result = binaryType(b);
		if (result.equals(TIME)) {
			return timeBinary(b, l, r);
		}
		Type c = settle(result, want);
		Elementary kind = ((Scalar) c).kind();
		if (opName.equals("**")) {
			gen(b.left(), c);
			gen(b.right(), c);
			op(Op.FEXPT);
			wrap(kind);
			return c;
		}
		gen(b.left(), c);
		gen(b.right(), c);
		arithmetic(opName, kind, b.line());
		return c;
	}

	private void arithmetic(String name, Elementary kind, int atLine) {
		if (kind.isReal()) {
			op(switch (name) {
				case "+", "ADD" -> Op.FADD;
				case "-", "SUB" -> Op.FSUB;
				case "*", "MUL" -> Op.FMUL;
				case "/", "DIV" -> Op.FDIV;
				default -> throw error(atLine, name + " takes whole numbers");
			});
		} else {
			op(switch (name) {
				case "+", "ADD" -> Op.IADD;
				case "-", "SUB" -> Op.ISUB;
				case "*", "MUL" -> Op.IMUL;
				case "/", "DIV" -> Op.IDIV;
				default -> Op.IMOD;
			});
		}
		wrap(kind);
	}

	private Type timeBinary(Ast.Binary b, Type l, Type r) {
		if (b.op().equals("+") || b.op().equals("-")) {
			gen(b.left(), TIME);
			gen(b.right(), TIME);
			op(b.op().equals("+") ? Op.IADD : Op.ISUB);
			return TIME;
		}
		Expr time = l.equals(TIME) ? b.left() : b.right();
		Expr factor = l.equals(TIME) ? b.right() : b.left();
		gen(time, TIME);
		op(Op.I2F);
		gen(factor, LREAL);
		op(b.op().equals("*") ? Op.FMUL : Op.FDIV);
		op(Op.F2I);
		return TIME;
	}

	private void compare(String name, Type type) {
		if (isString(type)) {
			op(Op.SCMP);
			pushLong(0);
			intCompare(name);
			return;
		}
		if (type instanceof Scalar s && s.kind().isReal()) {
			op(switch (name) {
				case "<" -> Op.FLT;
				case "<=" -> Op.FLE;
				case ">" -> Op.FGT;
				case ">=" -> Op.FGE;
				case "=" -> Op.FEQ;
				default -> Op.FNE;
			});
			return;
		}
		intCompare(name);
	}

	private void intCompare(String name) {
		op(switch (name) {
			case "<" -> Op.ILT;
			case "<=" -> Op.ILE;
			case ">" -> Op.IGT;
			case ">=" -> Op.IGE;
			case "=" -> Op.IEQ;
			default -> Op.INE;
		});
	}

	// ---- calls ------------------------------------------------------------------------------------------------------------------------

	private void callStatement(Ast.Call call) {
		if (instanceCall(call)) {
			fbCall(call);
			return;
		}
		Type type = call(call, null);
		if (isString(type)) {
			op(Op.SPOP);
		} else {
			op(Op.POP);
		}
	}

	private Type call(Ast.Call c, Type want) {
		if (instanceCall(c)) {
			throw error(c.line(), "a function block's call has no value: read its output, as fb.Q");
		}
		Pou pou = pous.get(c.name());
		if (pou != null) {
			if (!pou.isFunction()) {
				throw error(c.line(), c.name() + " is a " + pou.ast.kind() + ": declare an instance and call that");
			}
			return functionCall(c, pou);
		}
		if (conversionTarget(c.name()) != null) {
			return conversion(c);
		}
		return generic(c, want);
	}

	private List<Ast.Arg> ordered(Ast.Call c, Pou pou) {
		for (Ast.Arg arg : c.args()) {
			if (arg.name() != null && !pou.vars.containsKey(arg.name())) {
				throw error(c.line(), pou.name + " has no " + arg.name());
			}
		}
		List<Var> inputs = new ArrayList<>();
		for (Var var : pou.vars.values()) {
			if (var.decl.kind() == VarKind.INPUT || var.decl.kind() == VarKind.IN_OUT) {
				inputs.add(var);
			}
		}
		List<Ast.Arg> out = new ArrayList<>();
		int position = 0;
		for (Ast.Arg arg : c.args()) {
			if (arg.name() != null) {
				out.add(arg);
			} else {
				if (position >= inputs.size()) {
					throw error(c.line(), pou.name + " takes " + inputs.size() + " input(s), and more are given");
				}
				out.add(new Ast.Arg(inputs.get(position++).decl.name(), arg.value(), false));
			}
		}
		return out;
	}

	private Type functionCall(Ast.Call c, Pou pou) {
		cur.calls.add(pou.name);
		List<Ast.Arg> args = ordered(c, pou);
		List<Var> stored = new ArrayList<>();
		for (Ast.Arg arg : args) {
			if (arg.output()) {
				continue;
			}
			Var var = pou.vars.get(arg.name());
			if (var.decl.kind() == VarKind.IN_OUT) {
				inOutAddress(arg, var, c.line());
			} else if (var.decl.kind() == VarKind.INPUT) {
				gen(arg.value(), var.type);
			} else {
				throw error(c.line(), arg.name() + " is not an input of " + pou.name);
			}
			stored.add(var);
		}
		op(Op.COPYINIT, pou.frame, pou.size);
		for (int i = stored.size() - 1; i >= 0; i--) {
			Var var = stored.get(i);
			int slot = pou.frame + var.offset;
			if (var.decl.kind() == VarKind.IN_OUT) {
				op(Op.STOREA, slot);
			} else if (isString(var.type)) {
				op(Op.PUSHI, slot);
				op(Op.SSTORE);
			} else if (composite(var.type)) {
				op(Op.PUSHI, slot);
				op(Op.SWAP);
				op(Op.COPY, var.type.slots());
			} else {
				op(Op.STOREA, slot);
			}
		}
		op(Op.CALL, pou.index);
		for (Ast.Arg arg : args) {
			if (arg.output()) {
				Var var = pou.vars.get(arg.name());
				Place to = prepareStore(writable(arg.value()));
				load(new Place(ABS, pou.frame + var.offset, var.type));
				coerce(var.type, to.type, c.line());
				store(to);
			}
		}
		load(new Place(ABS, pou.frame, pou.returns));
		return pou.returns;
	}

	private void inOutAddress(Ast.Arg arg, Var var, int atLine) {
		Place place = writable(arg.value());
		if (!place.type.equals(var.type) && place.type != var.type) {
			throw error(atLine, arg.name() + " is IN_OUT, so it takes a " + var.type.label() + " exactly, not a " + place.type.label());
		}
		address(place);
	}

	private void fbCall(Ast.Call c) {
		Place place = place(c.target());
		if (!(place.type instanceof FbT fb)) {
			throw error(c.line(), "a " + place.type.label() + " cannot be called");
		}
		Pou pou = fb.pou;
		cur.calls.add(pou.name);
		boolean onStack = place.kind == STACK;
		List<Ast.Arg> args = ordered(c, pou);
		for (Ast.Arg arg : args) {
			if (arg.output()) {
				continue;
			}
			Var var = pou.vars.get(arg.name());
			if (var.decl.kind() != VarKind.INPUT && var.decl.kind() != VarKind.IN_OUT) {
				throw error(c.line(), arg.name() + " is not an input of " + pou.name);
			}
			if (onStack) {
				op(Op.DUP);
				if (var.offset != 0) {
					pushLong(var.offset);
					op(Op.IADD);
				}
			} else if (isString(var.type) || composite(var.type)) {
				address(member(place, var.offset, var.type));
			}
			if (var.decl.kind() == VarKind.IN_OUT) {
				inOutAddress(arg, var, c.line());
				if (onStack) {
					op(Op.STOREI);
				} else {
					store(member(place, var.offset, LINT));
				}
			} else {
				gen(arg.value(), var.type);
				if (onStack || isString(var.type) || composite(var.type)) {
					store(new Place(STACK, 0, var.type));
				} else {
					store(member(place, var.offset, var.type));
				}
			}
		}
		if (onStack) {
			for (Ast.Arg arg : args) {
				if (arg.output()) {
					throw error(c.line(), "a function block in an array cannot use =>: read its output after the call, as fbs[i]." + arg.name());
				}
			}
			op(Op.CALLFB, pou.index);
			return;
		}
		address(place);
		op(Op.CALLFB, pou.index);
		for (Ast.Arg arg : args) {
			if (arg.output()) {
				Var var = pou.vars.get(arg.name());
				if (var.decl.kind() != VarKind.OUTPUT) {
					throw error(c.line(), arg.name() + " is not an output of " + pou.name);
				}
				Place to = prepareStore(writable(arg.value()));
				load(member(place, var.offset, var.type));
				coerce(var.type, to.type, c.line());
				store(to);
			}
		}
	}

	private Type conversion(Ast.Call c) {
		args(c, 1, 1);
		int at = c.name().indexOf("_TO_");
		Elementary from = elementary(c.name().substring(0, at));
		Elementary to = elementary(c.name().substring(at + 4));
		gen(c.args().getFirst().value(), new Scalar(from));
		if (from == Elementary.STRING) {
			if (to.isReal()) {
				op(Op.STOF);
				wrap(to);
			} else {
				op(Op.STOI);
				wrap(to);
			}
		} else if (to == Elementary.STRING) {
			op(from.isReal() ? Op.FTOS : Op.ITOS);
		} else if (from.isReal() && to.isReal()) {
			wrap(to);
		} else if (from.isReal()) {
			op(Op.F2I);
			wrap(to);
		} else if (to.isReal()) {
			op(Op.I2F);
			wrap(to);
		} else {
			wrap(to);
		}
		return new Scalar(to);
	}

	private Type generic(Ast.Call c, Type want) {
		Type type = genericType(c);
		List<Expr> args = new ArrayList<>();
		for (Ast.Arg arg : c.args()) {
			args.add(arg.value());
		}
		Type settled = settle(type, want);
		Elementary kind = settled instanceof Scalar s ? s.kind() : null;
		switch (c.name()) {
			case "_NOW" -> op(Op.NOW);
			case "MOVE" -> gen(args.getFirst(), settled);
			case "ABS" -> {
				gen(args.getFirst(), settled);
				if (kind.isReal()) {
					op(Op.FMATH, Op.MATH_ABS);
				} else {
					op(Op.IABS);
					wrap(kind);
				}
			}
			case "NEG" -> {
				gen(args.getFirst(), settled);
				op(kind.isReal() ? Op.FNEG : Op.INEG);
				wrap(kind);
			}
			case "SQRT", "LN", "LOG", "EXP", "SIN", "COS", "TAN", "ASIN", "ACOS", "ATAN" -> {
				gen(args.getFirst(), settled);
				op(Op.FMATH, switch (c.name()) {
					case "SQRT" -> Op.MATH_SQRT;
					case "LN" -> Op.MATH_LN;
					case "LOG" -> Op.MATH_LOG;
					case "EXP" -> Op.MATH_EXP;
					case "SIN" -> Op.MATH_SIN;
					case "COS" -> Op.MATH_COS;
					case "TAN" -> Op.MATH_TAN;
					case "ASIN" -> Op.MATH_ASIN;
					case "ACOS" -> Op.MATH_ACOS;
					default -> Op.MATH_ATAN;
				});
				wrap(kind);
			}
			case "EXPT" -> {
				gen(args.get(0), settled);
				gen(args.get(1), settled);
				op(Op.FEXPT);
				wrap(kind);
			}
			case "ADD", "SUB", "MUL", "DIV", "MOD" -> {
				gen(args.getFirst(), settled);
				for (Expr arg : args.subList(1, args.size())) {
					gen(arg, settled);
					arithmetic(c.name(), kind, c.line());
				}
			}
			case "MIN", "MAX" -> {
				gen(args.getFirst(), settled);
				for (Expr arg : args.subList(1, args.size())) {
					gen(arg, settled);
					minMax(c.name().equals("MIN"), kind);
				}
			}
			case "LIMIT" -> {
				gen(args.get(1), settled);
				gen(args.get(0), settled);
				minMax(false, kind);
				gen(args.get(2), settled);
				minMax(true, kind);
			}
			case "SEL" -> {
				gen(args.get(0), BOOL);
				gen(args.get(1), settled);
				gen(args.get(2), settled);
				op(Op.SELECT);
			}
			case "MUX" -> {
				gen(args.get(0), LINT);
				for (Expr arg : args.subList(1, args.size())) {
					gen(arg, settled);
				}
				op(Op.MUX, args.size() - 1);
			}
			case "TRUNC" -> {
				gen(args.getFirst(), LREAL);
				op(Op.F2TRUNC);
				wrap(Elementary.DINT);
			}
			case "SHL", "SHR", "ROL", "ROR" -> {
				gen(args.get(0), settled);
				gen(args.get(1), LINT);
				if (!kind.isBits() && !kind.isInt()) {
					throw error(c.line(), c.name() + " takes bit strings");
				}
				op(switch (c.name()) {
					case "SHL" -> Op.SHL;
					case "SHR" -> Op.SHR;
					case "ROL" -> Op.ROL;
					default -> Op.ROR;
				}, kind.bits);
			}
			case "LEN" -> {
				gen(args.getFirst(), STRING);
				op(Op.STROP, Op.STR_LEN);
			}
			case "LEFT", "RIGHT" -> stringOp(c, args, 2, 1, c.name().equals("LEFT") ? Op.STR_LEFT : Op.STR_RIGHT);
			case "MID" -> stringOp(c, args, 3, 1, Op.STR_MID);
			case "DELETE" -> stringOp(c, args, 3, 1, Op.STR_DELETE);
			case "INSERT" -> stringOp(c, args, 3, 2, Op.STR_INSERT);
			case "REPLACE" -> stringOp(c, args, 4, 2, Op.STR_REPLACE);
			case "FIND" -> stringOp(c, args, 2, 2, Op.STR_FIND);
			case "CONCAT" -> {
				gen(args.getFirst(), STRING);
				for (Expr arg : args.subList(1, args.size())) {
					gen(arg, STRING);
					op(Op.STROP, Op.STR_CONCAT);
				}
			}
			default -> throw error(c.line(), "there is no function " + c.name());
		}
		return settled;
	}

	private void stringOp(Ast.Call c, List<Expr> args, int count, int strings, int which) {
		if (args.size() != count) {
			throw error(c.line(), c.name() + " takes " + count + " values");
		}
		for (int i = 0; i < count; i++) {
			gen(args.get(i), i < strings ? STRING : LINT);
		}
		op(Op.STROP, which);
	}

	private void minMax(boolean min, Elementary kind) {
		if (kind.isReal()) {
			op(min ? Op.FMIN : Op.FMAX);
		} else {
			op(min ? Op.IMIN : Op.IMAX);
		}
	}
}
