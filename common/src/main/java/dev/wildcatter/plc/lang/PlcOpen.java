package dev.wildcatter.plc.lang;

import dev.wildcatter.plc.lang.Project.Language;
import dev.wildcatter.plc.lang.Project.Source;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * A project as PLCopen XML (TC6 v2.01, the form IEC 61131-10 standardises), so one can be written outside the game. Reading is all or nothing:
 * a file with anything a program unit here cannot hold is refused with the reasons, rather than dropping part of a program.
 */
public final class PlcOpen {
	private static final String NAMESPACE = "http://www.plcopen.org/xml/tc6_0201";
	private static final Set<String> ELEMENTARY = Set.of("BOOL", "SINT", "INT", "DINT", "LINT", "USINT", "UINT", "UDINT", "ULINT", "REAL", "LREAL", "TIME", "BYTE", "WORD", "DWORD", "LWORD", "DATE", "DT", "TOD");
	private static final Map<String, String> POU_TYPE = Map.of("PROGRAM", "program", "FUNCTION_BLOCK", "functionBlock", "FUNCTION", "function");
	private static final Map<String, String> KEYWORD = Map.of("inputVars", "VAR_INPUT", "outputVars", "VAR_OUTPUT", "inOutVars", "VAR_IN_OUT", "externalVars", "VAR_EXTERNAL", "localVars", "VAR", "tempVars", "VAR_TEMP",
			"globalVars", "VAR_GLOBAL");

	/** The file, or why not. */
	public record Exported(String xml, List<String> problems) {
	}

	/** The units read, or why not: never both. */
	public record Imported(List<Source> sources, List<String> problems) {
	}

	private PlcOpen() {
	}

	// ---- writing ---------------------------------------------------------------------------------------------------------------------

	/** A small XML tree: enough for this format, and it escapes everything in one place. */
	private static final class X {
		final String name;
		final Map<String, String> attrs = new LinkedHashMap<>();
		final List<X> children = new ArrayList<>();
		String text;

		X(String name) {
			this.name = name;
		}

		X attr(String key, Object value) {
			attrs.put(key, String.valueOf(value));
			return this;
		}

		X add(X child) {
			children.add(child);
			return this;
		}

		X child(String childName) {
			X out = new X(childName);
			children.add(out);
			return out;
		}

		void write(StringBuilder out, int depth) {
			out.append("  ".repeat(depth)).append('<').append(name);
			attrs.forEach((k, v) -> out.append(' ').append(k).append("=\"").append(escape(v)).append('"'));
			if (text != null) {
				out.append('>').append(escape(text)).append("</").append(name).append(">\n");
			} else if (children.isEmpty()) {
				out.append("/>\n");
			} else {
				out.append(">\n");
				children.forEach(c -> c.write(out, depth + 1));
				out.append("  ".repeat(depth)).append("</").append(name).append(">\n");
			}
		}
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static X st(String text) {
		X body = new X("ST");
		X p = body.child("xhtml:p");
		p.text = text;
		return body;
	}

	private static X position(int x, int y) {
		return new X("position").attr("x", Math.max(0, x)).attr("y", Math.max(0, y));
	}

	private static X into(int ref, String formal) {
		X connection = new X("connection").attr("refLocalId", ref);
		if (formal != null && !formal.isEmpty()) {
			connection.attr("formalParameter", formal);
		}
		return connection;
	}

	private static String print(Ast.Expr expr) {
		return switch (expr) {
			case Ast.IntLit v -> (v.type() == null ? "" : v.type() + "#") + v.value();
			case Ast.RealLit v -> (v.type() == null ? "" : v.type() + "#") + v.value();
			case Ast.BoolLit v -> v.value() ? "TRUE" : "FALSE";
			case Ast.TimeLit v -> "T#" + v.ms() + "ms";
			case Ast.StringLit v -> "'" + v.value().replace("'", "$'") + "'";
			case Ast.EnumLit v -> (v.type() == null ? "" : v.type() + "#") + v.value();
			case Ast.Name v -> v.name();
			case Ast.Member v -> print(v.base()) + "." + v.field();
			case Ast.BitOf v -> print(v.base()) + "." + v.bit();
			case Ast.Index v -> print(v.base()) + "[" + String.join(", ", v.index().stream().map(PlcOpen::print).toList()) + "]";
			case Ast.Direct v -> v.address();
			case Ast.Unary v -> v.op() + (v.op().equals("NOT") ? " " : "") + print(v.value());
			case Ast.Binary v -> "(" + print(v.left()) + " " + v.op() + " " + print(v.right()) + ")";
			case Ast.Call v -> v.name() + "(" + String.join(", ", v.args().stream().map(a -> (a.name() == null ? "" : a.name() + (a.output() ? " => " : " := ")) + print(a.value())).toList()) + ")";
		};
	}

	private static X typeXml(Ast.TypeSpec spec) {
		switch (spec) {
			case Ast.NamedType named -> {
				if (ELEMENTARY.contains(named.name())) {
					return new X(named.name());
				}
				if (named.name().equals("STRING")) {
					return new X("string");
				}
				return new X("derived").attr("name", named.name());
			}
			case Ast.ArrayType array -> {
				X out = new X("array");
				for (int i = 0; i < array.low().size(); i++) {
					out.child("dimension").attr("lower", print(array.low().get(i))).attr("upper", print(array.high().get(i)));
				}
				out.child("baseType").add(typeXml(array.element()));
				return out;
			}
			case Ast.StructType struct -> {
				X out = new X("struct");
				struct.fields().forEach(f -> out.add(variable(f)));
				return out;
			}
			case Ast.EnumType en -> {
				X out = new X("enum");
				X values = out.child("values");
				en.values().forEach(v -> values.child("value").attr("name", v));
				return out;
			}
			case Ast.AutoType auto -> throw new IllegalStateException("an inferred type is never declared");
		}
	}

	private static X valueXml(Ast.Init init) {
		switch (init) {
			case Ast.ExprInit e -> {
				return new X("simpleValue").attr("value", print(e.value()));
			}
			case Ast.ArrayInit a -> {
				X out = new X("arrayValue");
				for (int i = 0; i < a.items().size(); i++) {
					X value = out.child("value");
					if (a.counts().get(i) != 1) {
						value.attr("repetitionValue", a.counts().get(i));
					}
					value.add(valueXml(a.items().get(i)));
				}
				return out;
			}
			case Ast.StructInit s -> {
				X out = new X("structValue");
				for (int i = 0; i < s.names().size(); i++) {
					out.child("value").attr("member", s.names().get(i)).add(valueXml(s.values().get(i)));
				}
				return out;
			}
		}
	}

	private static X variable(Ast.VarDecl decl) {
		X out = new X("variable").attr("name", decl.name());
		if (decl.at() != null) {
			out.attr("address", decl.at());
		}
		out.child("type").add(typeXml(decl.type()));
		if (decl.init() != null) {
			out.child("initialValue").add(valueXml(decl.init()));
		}
		return out;
	}

	/** Declarations as var blocks, a new one wherever the kind or a qualifier changes. */
	private static void varBlocks(List<Ast.VarDecl> vars, X into) {
		X block = null;
		Ast.VarDecl last = null;
		for (Ast.VarDecl decl : vars) {
			if (last == null || last.kind() != decl.kind() || last.constant() != decl.constant() || last.retain() != decl.retain()) {
				String tag = switch (decl.kind()) {
					case INPUT -> "inputVars";
					case OUTPUT -> "outputVars";
					case IN_OUT -> "inOutVars";
					case TEMP -> "tempVars";
					case GLOBAL -> "globalVars";
					case EXTERNAL -> "externalVars";
					case VAR -> "localVars";
				};
				block = into.child(tag);
				if (decl.constant()) {
					block.attr("constant", "true");
				}
				if (decl.retain()) {
					block.attr("retain", "true");
				}
			}
			block.add(variable(decl));
			last = decl;
		}
	}

	/** The project as PLCopen XML. Only what does not parse stops it: a project with other errors exports as it stands. */
	public static Exported write(List<Source> sources) {
		List<String> problems = new ArrayList<>();
		X types = new X("dataTypes");
		X pous = new X("pous");
		List<Ast.TaskDecl> tasks = new ArrayList<>();
		List<Ast.ProgramInstance> instances = new ArrayList<>();
		List<Ast.VarDecl> globals = new ArrayList<>();
		Map<String, Ast.Pou> library = PlcCompiler.library(sources);
		for (Source source : sources) {
			try {
				switch (source.language()) {
					case ST -> structuredText(source, types, pous, tasks, instances, globals, problems);
					case LD -> ladder(Ladder.parse(source.text()), library, pous, problems, source.name());
					case FBD -> fbd(Fbd.parse(source.text()), library, pous, problems, source.name());
					case SFC -> sfc(Sfc.parse(source.text()), pous, problems, source.name());
				}
			} catch (RuntimeException e) {
				problems.add(source.name() + " is damaged: " + e.getMessage());
			}
		}
		if (!problems.isEmpty()) {
			return new Exported(null, problems);
		}
		X project = new X("project").attr("xmlns", NAMESPACE).attr("xmlns:xhtml", "http://www.w3.org/1999/xhtml");
		project.child("fileHeader").attr("companyName", "Wildcatter").attr("productName", "Wildcatter PLC").attr("productVersion", "1").attr("creationDateTime", java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
		X header = project.child("contentHeader").attr("name", "project");
		X coordinates = header.child("coordinateInfo");
		for (String language : List.of("fbd", "ld", "sfc")) {
			coordinates.child(language).child("scaling").attr("x", 1).attr("y", 1);
		}
		project.child("types").add(types).add(pous);
		X configurations = project.child("instances").child("configurations");
		if (!tasks.isEmpty() || !instances.isEmpty() || !globals.isEmpty()) {
			X configuration = configurations.child("configuration").attr("name", "CONFIG");
			X resource = configuration.child("resource").attr("name", "RES");
			Map<String, X> taskXml = new LinkedHashMap<>();
			for (Ast.TaskDecl task : tasks) {
				X element = resource.child("task").attr("name", task.name()).attr("priority", task.priority());
				if (task.intervalMs() > 0) {
					element.attr("interval", "T#" + task.intervalMs() + "ms");
				}
				taskXml.put(task.name(), element);
			}
			if (!globals.isEmpty()) {
				varBlocks(globals, configuration);
			}
			for (Ast.ProgramInstance instance : instances) {
				X parent = instance.task() == null || !taskXml.containsKey(instance.task()) ? resource : taskXml.get(instance.task());
				parent.child("pouInstance").attr("name", instance.instance()).attr("typeName", instance.program());
			}
		}
		StringBuilder out = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		project.write(out, 0);
		return new Exported(out.toString(), List.of());
	}

	private static void structuredText(Source source, X types, X pous, List<Ast.TaskDecl> tasks, List<Ast.ProgramInstance> instances, List<Ast.VarDecl> globals, List<String> problems) {
		List<CompileError> errors = new ArrayList<>();
		Parser.Spanned parsed = Parser.parseSpanned(source.text(), errors);
		for (CompileError error : errors) {
			problems.add(source.name() + " line " + error.line() + ": " + error.message());
		}
		if (!errors.isEmpty()) {
			return;
		}
		for (Ast.TypeDecl decl : parsed.unit().types()) {
			X type = types.child("dataType").attr("name", decl.name());
			type.child("baseType").add(typeXml(decl.type()));
			if (decl.init() != null) {
				type.child("initialValue").add(valueXml(decl.init()));
			}
		}
		tasks.addAll(parsed.unit().tasks());
		instances.addAll(parsed.unit().instances());
		globals.addAll(parsed.unit().globals());
		String[] lines = source.text().split("\n", -1);
		int[] starts = new int[lines.length + 2];
		for (int i = 0; i < lines.length; i++) {
			starts[i + 1] = starts[i] + lines[i].length() + 1;
		}
		for (int i = 0; i < parsed.unit().pous().size(); i++) {
			Ast.Pou pou = parsed.unit().pous().get(i);
			int[] span = parsed.bodies().get(i);
			int from = starts[span[0] - 1] + span[1] - 1;
			int to = starts[span[2] - 1] + span[3] - 1;
			X element = pou(pou.name(), pou.kind().name(), pous);
			X face = element.child("interface");
			if (pou.returns() != null) {
				face.child("returnType").add(typeXml(pou.returns()));
			}
			varBlocks(pou.vars(), face);
			element.child("body").add(st(source.text().substring(from, to).strip()));
		}
	}

	private static X pou(String name, String kind, X pous) {
		return pous.child("pou").attr("name", name).attr("pouType", POU_TYPE.getOrDefault(kind, "program"));
	}

	private static X graphicalFace(String kind, String name, String returns, String declarations, X element, List<String> problems, String unit) {
		List<CompileError> errors = new ArrayList<>();
		if (kind.equals("FUNCTION") && (returns == null || returns.isBlank())) {
			problems.add(unit + " is a function with no result type");
		}
		Ast.Unit parsed = Parser.parse(PlcCompiler.headerText(kind, name, returns, declarations), errors);
		for (CompileError error : errors) {
			problems.add(unit + "'s variables, line " + Math.max(1, error.line() - 1) + ": " + error.message());
		}
		X face = element.child("interface");
		if (!parsed.pous().isEmpty()) {
			Ast.Pou pou = parsed.pous().getFirst();
			if (pou.returns() != null) {
				face.child("returnType").add(typeXml(pou.returns()));
			}
			varBlocks(pou.vars(), face);
		}
		return face;
	}

	// ---- ladder, out ------------------------------------------------------------------------------------------------------------------

	private static final class LdOut {
		final X body = new X("LD");
		final Map<String, Ast.Pou> library;
		int next = 1;
		int x;
		int y;

		LdOut(Map<String, Ast.Pou> library) {
			this.library = library;
		}

		int id() {
			return next++;
		}
	}

	/** What feeds an element: each a local id, with its output's formal parameter when it is a block's. */
	private record Feed(int id, String formal) {
	}

	private static void ladder(Ladder ladder, Map<String, Ast.Pou> library, X pous, List<String> problems, String unit) {
		X element = pou(ladder.name, ladder.kind, pous);
		graphicalFace(ladder.kind, ladder.name, ladder.returns, ladder.declarations, element, problems, unit);
		LdOut out = new LdOut(library);
		int row = 0;
		for (Ladder.Rung rung : ladder.rungs) {
			out.y = 20 + row * 80;
			out.x = 40;
			if (rung.comment != null && !rung.comment.isBlank()) {
				X comment = out.body.child("comment").attr("localId", out.id()).attr("height", 12).attr("width", 300);
				comment.add(position(40, Math.max(0, out.y - 14)));
				comment.child("content").child("xhtml:p").text = rung.comment;
			}
			if (rung.label != null && !rung.label.isBlank()) {
				// a label stands before its rung, as the standard draws it
				out.body.child("label").attr("localId", out.id()).attr("label", rung.label.trim().toUpperCase(Locale.ROOT)).add(position(0, Math.max(0, out.y - 10)));
			}
			int rail = out.id();
			X left = out.body.child("leftPowerRail").attr("localId", rail).add(position(10, out.y));
			left.child("connectionPointOut").attr("formalParameter", "");
			List<Feed> power = ladderNode(rung.body, List.of(new Feed(rail, "")), out);
			List<Feed> ends = new ArrayList<>();
			for (Ladder.Coil coil : rung.coils) {
				int id = out.id();
				if (coil.kind.flow()) {
					// a jump or return ends its rung: it has no output, and the right rail does not take it
					X flow = out.body.child(coil.kind == Ladder.CoilKind.JUMP ? "jump" : "return").attr("localId", id);
					if (coil.kind == Ladder.CoilKind.JUMP) {
						flow.attr("label", coil.var.trim().toUpperCase(Locale.ROOT));
					}
					flow.add(position(out.x + 40, out.y));
					connect(flow.child("connectionPointIn"), power);
					continue;
				}
				X element2 = out.body.child("coil").attr("localId", id).attr("negated", coil.kind == Ladder.CoilKind.NOT ? "true" : "false");
				if (coil.kind == Ladder.CoilKind.SET) {
					element2.attr("storage", "set");
				} else if (coil.kind == Ladder.CoilKind.RESET) {
					element2.attr("storage", "reset");
				}
				element2.add(position(out.x + 40, out.y));
				connect(element2.child("connectionPointIn"), power);
				element2.child("connectionPointOut");
				element2.child("variable").text = coil.var;
				ends.add(new Feed(id, ""));
			}
			if (ends.isEmpty() && rung.coils.stream().noneMatch(c -> c.kind.flow())) {
				ends = power;
			}
			X right = out.body.child("rightPowerRail").attr("localId", out.id()).add(position(out.x + 90, out.y));
			if (ends.isEmpty()) {
				right.child("connectionPointIn");
			} else {
				connect(right.child("connectionPointIn"), ends);
			}
			row++;
		}
		element.child("body").add(out.body);
	}

	private static void connect(X point, List<Feed> feeds) {
		for (Feed feed : feeds) {
			point.add(into(feed.id(), feed.formal()));
		}
	}

	private static List<Feed> ladderNode(Ladder.Node node, List<Feed> in, LdOut out) {
		switch (node) {
			case Ladder.Contact c -> {
				int id = out.id();
				X element = out.body.child("contact").attr("localId", id).attr("negated", c.negated ? "true" : "false");
				out.x += 40;
				element.add(position(out.x, out.y));
				connect(element.child("connectionPointIn"), in);
				element.child("connectionPointOut");
				element.child("variable").text = c.var;
				return List.of(new Feed(id, ""));
			}
			case Ladder.Series s -> {
				List<Feed> at = in;
				for (Ladder.Node item : s.items) {
					at = ladderNode(item, at, out);
				}
				return at;
			}
			case Ladder.Parallel p -> {
				Set<Feed> ends = new LinkedHashSet<>();
				int startX = out.x;
				int endX = out.x;
				int startY = out.y;
				for (Ladder.Node item : p.items) {
					out.x = startX;
					ends.addAll(ladderNode(item, in, out));
					endX = Math.max(endX, out.x);
					out.y += 30;
				}
				out.x = endX;
				out.y = startY;
				return List.copyOf(ends);
			}
			case Ladder.Box b -> {
				return ladderBox(b, in, out);
			}
		}
	}

	private static List<Feed> ladderBox(Ladder.Box b, List<Feed> in, LdOut out) {
		BlockLibrary.Pins pins = BlockLibrary.pins(b.type, out.library);
		Ast.Pou pou = out.library.get(b.type);
		boolean fb = pou != null && pou.kind() == Ast.PouKind.FUNCTION_BLOCK;
		String powerIn = fb ? BlockLibrary.firstBool(pou, Ast.VarKind.INPUT) : "EN";
		String powerOut = fb ? BlockLibrary.firstBool(pou, Ast.VarKind.OUTPUT) : BlockLibrary.isCompare(b.type) ? "OUT" : "ENO";
		if (powerOut == null) {
			powerOut = "ENO";
		}
		int id = out.id();
		X inputs = new X("inputVariables");
		X inOuts = new X("inOutVariables");
		X outputs = new X("outputVariables");
		out.x += 40;
		int px = out.x;
		int py = out.y;
		List<String> inNames = pins == null ? List.copyOf(b.pins.keySet()) : pins.inputs();
		if (powerIn != null && !(fb && b.pins.containsKey(powerIn) && !b.pins.get(powerIn).isBlank())) {
			X variable = inputs.child("variable").attr("formalParameter", powerIn);
			connect(variable.child("connectionPointIn"), in);
		}
		for (String pin : inNames) {
			String value = b.pins.get(pin);
			if (value == null || value.isBlank()) {
				continue;
			}
			int source = out.id();
			X variable = out.body.child("inVariable").attr("localId", source).add(position(px - 30, py + 30));
			variable.child("connectionPointOut");
			variable.child("expression").text = value;
			inputs.child("variable").attr("formalParameter", pin).child("connectionPointIn").add(into(source, ""));
		}
		if (!fb) {
			// a function's EN is not in its library pins, and neither is ENO: both are the standard's own
			outputs.child("variable").attr("formalParameter", powerOut).child("connectionPointOut");
			if (!powerOut.equals("OUT")) {
				outputs.child("variable").attr("formalParameter", "OUT").child("connectionPointOut");
			}
		} else if (pins != null) {
			for (String pin : pins.outputs()) {
				outputs.child("variable").attr("formalParameter", pin).child("connectionPointOut");
			}
		}
		if (pins != null) {
			for (String pin : pins.outputs()) {
				String target = b.pins.get(pin);
				if (target != null && !target.isBlank()) {
					int sink = out.id();
					X variable = out.body.child("outVariable").attr("localId", sink).add(position(px + 30, py + 30));
					variable.child("connectionPointIn").add(into(id, pin));
					variable.child("expression").text = target;
				}
			}
		}
		X block = out.body.child("block").attr("localId", id).attr("typeName", b.type);
		if (b.instance != null && !b.instance.isBlank()) {
			block.attr("instanceName", b.instance);
		}
		block.add(position(px, py)).add(inputs).add(inOuts).add(outputs);
		return List.of(new Feed(id, powerOut));
	}

	// ---- function block diagram, out ----------------------------------------------------------------------------------------------------

	private static void fbd(Fbd fbd, Map<String, Ast.Pou> library, X pous, List<String> problems, String unit) {
		X element = pou(fbd.name, fbd.kind, pous);
		graphicalFace(fbd.kind, fbd.name, fbd.returns, fbd.declarations, element, problems, unit);
		X body = new X("FBD");
		int[] next = {fbd.nextId()};
		List<Fbd.Block> order = fbd.order();
		for (Fbd.Block block : fbd.blocks) {
			int orderId = order.indexOf(block) + 1;
			if (block.type.equals(Fbd.IN)) {
				X variable = body.child("inVariable").attr("localId", block.id).add(position(block.x, block.y));
				variable.child("connectionPointOut");
				variable.child("expression").text = block.name == null ? "" : block.name;
			} else if (block.type.equals(Fbd.OUT)) {
				X variable = body.child("outVariable").attr("localId", block.id).attr("executionOrderId", orderId).add(position(block.x, block.y));
				fbdInput(block.inputs.get("IN"), block, variable.child("connectionPointIn"), body, next);
				variable.child("expression").text = block.name == null ? "" : block.name;
			} else {
				BlockLibrary.Pins pins = BlockLibrary.pins(block.type, library);
				X inputs = new X("inputVariables");
				X outputs = new X("outputVariables");
				for (String pin : pins == null ? List.copyOf(block.inputs.keySet()) : pins.inputs()) {
					String source = block.inputs.get(pin);
					if (source != null && !source.isBlank()) {
						fbdInput(source, block, inputs.child("variable").attr("formalParameter", pin).child("connectionPointIn"), body, next);
					}
				}
				for (String pin : pins == null ? List.of("OUT") : pins.outputs()) {
					outputs.child("variable").attr("formalParameter", pin).child("connectionPointOut");
				}
				X out = body.child("block").attr("localId", block.id).attr("typeName", block.type);
				if (block.name != null && !block.name.isBlank()) {
					out.attr("instanceName", block.name);
				}
				out.attr("executionOrderId", orderId).add(position(block.x, block.y)).add(inputs).add(new X("inOutVariables")).add(outputs);
			}
		}
		element.child("body").add(body);
	}

	private static void fbdInput(String source, Fbd.Block block, X point, X body, int[] next) {
		if (Fbd.isWire(source)) {
			String pin = Fbd.wirePin(source);
			point.add(into(Fbd.wireBlock(source), pin.equals("OUT") ? "" : pin));
		} else if (source != null && !source.isBlank()) {
			int id = next[0]++;
			X variable = body.child("inVariable").attr("localId", id).add(position(block.x - 40, block.y));
			variable.child("connectionPointOut");
			variable.child("expression").text = source;
			point.add(into(id, ""));
		}
	}

	// ---- chart, out -------------------------------------------------------------------------------------------------------------------

	private static void sfc(Sfc sfc, X pous, List<String> problems, String unit) {
		X element = pou(sfc.name, sfc.kind, pous);
		graphicalFace(sfc.kind, sfc.name, "", sfc.declarations, element, problems, unit);
		if (!sfc.bodies.isEmpty()) {
			X actions = element.child("actions");
			sfc.bodies.forEach((name, text) -> {
				X action = actions.child("action").attr("name", name);
				action.child("body").add(st(text));
			});
		}
		X body = new X("SFC");
		int[] next = {sfc.nextId()};
		Map<Integer, List<Sfc.Transition>> in = new HashMap<>();
		Map<Integer, List<Sfc.Transition>> out = new HashMap<>();
		for (Sfc.Transition t : sfc.transitions) {
			t.from.forEach(id -> out.computeIfAbsent(id, k -> new ArrayList<>()).add(t));
			t.to.forEach(id -> {
				Sfc.Step step = sfc.step(id);
				if (step != null && step.y > t.y) {   // a way back up is a jump, as a chart is drawn
					in.computeIfAbsent(id, k -> new ArrayList<>()).add(t);
				}
			});
		}
		// the divergences, so a wire can come out of them
		Map<Integer, Integer> simultaneous = new HashMap<>();
		for (Sfc.Transition t : sfc.transitions) {
			if (t.to.size() > 1) {
				int id = next[0]++;
				simultaneous.put(t.id, id);
				X node = body.child("simultaneousDivergence").attr("localId", id).add(position(t.x, t.y + 14));
				node.child("connectionPointIn").add(into(t.id, ""));
				for (int i = 0; i < t.to.size(); i++) {
					node.child("connectionPointOut").attr("formalParameter", Integer.toString(i));
				}
			}
		}
		Map<Integer, Integer> selection = new HashMap<>();
		for (Sfc.Step step : sfc.steps) {
			List<Sfc.Transition> leaving = out.getOrDefault(step.id, List.of());
			if (leaving.size() > 1) {
				int id = next[0]++;
				selection.put(step.id, id);
				X node = body.child("selectionDivergence").attr("localId", id).add(position(step.x, step.y + 20));
				node.child("connectionPointIn").add(into(step.id, ""));
				for (int i = 0; i < leaving.size(); i++) {
					node.child("connectionPointOut").attr("formalParameter", Integer.toString(i));
				}
			}
		}
		for (Sfc.Step step : sfc.steps) {
			X element2 = body.child("step").attr("localId", step.id).attr("name", step.name).attr("initialStep", step.initial ? "true" : "false").add(position(step.x, step.y));
			List<Sfc.Transition> arriving = in.getOrDefault(step.id, List.of());
			if (arriving.size() == 1) {
				element2.child("connectionPointIn").add(fromTransition(arriving.getFirst(), step, simultaneous));
			} else if (arriving.size() > 1) {
				int id = next[0]++;
				X join = body.child("selectionConvergence").attr("localId", id).add(position(step.x, step.y - 6));
				for (Sfc.Transition t : arriving) {
					join.child("connectionPointIn").add(fromTransition(t, step, simultaneous));
				}
				join.child("connectionPointOut");
				element2.child("connectionPointIn").add(into(id, ""));
			}
			element2.child("connectionPointOut").attr("formalParameter", "");
			if (!step.actions.isEmpty()) {
				X block = body.child("actionBlock").attr("localId", next[0]++).add(position(step.x + 90, step.y));
				block.child("connectionPointIn").add(into(step.id, ""));
				for (Sfc.Association a : step.actions) {
					X action = block.child("action").attr("localId", next[0]++).attr("qualifier", a.qualifier.isBlank() ? "N" : a.qualifier);
					if (a.time != null && !a.time.isBlank()) {
						action.attr("duration", a.time);
					}
					action.child("relPosition").attr("x", 0).attr("y", 0);
					action.child("reference").attr("name", a.action);
				}
			}
		}
		for (Sfc.Transition t : sfc.transitions) {
			X element2 = body.child("transition").attr("localId", t.id).add(position(t.x, t.y));
			X point = element2.child("connectionPointIn");
			if (t.from.size() == 1) {
				point.add(fromStep(t.from.getFirst(), t, out, selection));
			} else if (t.from.size() > 1) {
				int id = next[0]++;
				X join = body.child("simultaneousConvergence").attr("localId", id).add(position(t.x, t.y - 6));
				for (int step : t.from) {
					join.child("connectionPointIn").add(fromStep(step, t, out, selection));
				}
				join.child("connectionPointOut");
				point.add(into(id, ""));
			}
			element2.child("connectionPointOut");
			element2.child("condition").child("inline").attr("name", "").add(st(t.condition));
			for (int id : t.to) {
				Sfc.Step step = sfc.step(id);
				if (step != null && step.y <= t.y) {
					X jump = body.child("jumpStep").attr("localId", next[0]++).attr("targetName", step.name).add(position(t.x, t.y + 20));
					jump.child("connectionPointIn").add(fromTransition(t, step, simultaneous));
				}
			}
		}
		element.child("body").add(body);
	}

	private static X fromTransition(Sfc.Transition t, Sfc.Step to, Map<Integer, Integer> simultaneous) {
		Integer split = simultaneous.get(t.id);
		return split == null ? into(t.id, "") : into(split, Integer.toString(t.to.indexOf(to.id)));
	}

	private static X fromStep(int step, Sfc.Transition t, Map<Integer, List<Sfc.Transition>> out, Map<Integer, Integer> selection) {
		Integer split = selection.get(step);
		return split == null ? into(step, "") : into(split, Integer.toString(out.get(step).indexOf(t)));
	}

	// ---- reading ---------------------------------------------------------------------------------------------------------------------

	private static List<Element> children(Element parent) {
		List<Element> out = new ArrayList<>();
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element) {
				out.add(element);
			}
		}
		return out;
	}

	private static List<Element> children(Element parent, String name) {
		return children(parent).stream().filter(e -> name.equals(local(e))).toList();
	}

	private static Element child(Element parent, String name) {
		return parent == null ? null : children(parent).stream().filter(e -> name.equals(local(e))).findFirst().orElse(null);
	}

	private static String local(Element element) {
		return element.getLocalName() != null ? element.getLocalName() : element.getTagName();
	}

	private static String code(Element body) {
		if (body == null) {
			return "";
		}
		return body.getTextContent().strip();
	}

	private static int number(String text, int otherwise) {
		try {
			return (int) Math.round(Double.parseDouble(text));
		} catch (NumberFormatException e) {
			return otherwise;
		}
	}

	private static int[] at(Element element) {
		Element position = child(element, "position");
		return position == null ? new int[] {0, 0} : new int[] {number(position.getAttribute("x"), 0), number(position.getAttribute("y"), 0)};
	}

	private record Wire(int ref, String formal) {
	}

	private static List<Wire> wires(Element point) {
		List<Wire> out = new ArrayList<>();
		if (point != null) {
			for (Element connection : children(point, "connection")) {
				out.add(new Wire(number(connection.getAttribute("refLocalId"), -1), connection.getAttribute("formalParameter")));
			}
		}
		return out;
	}

	private static String typeText(Element type, List<String> problems, String where) {
		if (type == null || children(type).isEmpty()) {
			problems.add(where + " has no type");
			return "BOOL";
		}
		Element spec = children(type).getFirst();
		String tag = local(spec);
		switch (tag) {
			case "string", "wstring" -> {
				return "STRING";
			}
			case "derived" -> {
				return spec.getAttribute("name").toUpperCase(Locale.ROOT);
			}
			case "array" -> {
				List<String> ranges = new ArrayList<>();
				for (Element dimension : children(spec, "dimension")) {
					ranges.add(dimension.getAttribute("lower") + ".." + dimension.getAttribute("upper"));
				}
				return "ARRAY [" + String.join(", ", ranges) + "] OF " + typeText(child(spec, "baseType"), problems, where);
			}
			case "struct" -> {
				StringBuilder out = new StringBuilder("STRUCT\n");
				for (Element variable : children(spec, "variable")) {
					out.append('\t').append(variableText(variable, problems, where)).append('\n');
				}
				return out.append("END_STRUCT").toString();
			}
			case "enum" -> {
				List<String> names = new ArrayList<>();
				Element values = child(spec, "values");
				if (values != null) {
					children(values, "value").forEach(v -> names.add(v.getAttribute("name")));
				}
				return "(" + String.join(", ", names) + ")";
			}
			default -> {
				String upper = tag.toUpperCase(Locale.ROOT);
				if (ELEMENTARY.contains(upper) && !Set.of("DATE", "DT", "TOD", "ULINT").contains(upper)) {
					return upper;
				}
				problems.add(where + " has the type " + tag + ", which this CPU does not have");
				return "BOOL";
			}
		}
	}

	private static String valueText(Element value, List<String> problems, String where) {
		switch (local(value)) {
			case "simpleValue" -> {
				return value.getAttribute("value");
			}
			case "arrayValue" -> {
				List<String> items = new ArrayList<>();
				for (Element item : children(value, "value")) {
					String inner = valueText(children(item).getFirst(), problems, where);
					String count = item.getAttribute("repetitionValue");
					items.add(count.isEmpty() ? inner : count + "(" + inner + ")");
				}
				return "[" + String.join(", ", items) + "]";
			}
			case "structValue" -> {
				List<String> items = new ArrayList<>();
				for (Element item : children(value, "value")) {
					items.add(item.getAttribute("member") + " := " + valueText(children(item).getFirst(), problems, where));
				}
				return "(" + String.join(", ", items) + ")";
			}
			default -> {
				problems.add(where + " has an initial value of a form not read here: " + local(value));
				return "0";
			}
		}
	}

	private static String variableText(Element variable, List<String> problems, String where) {
		String name = variable.getAttribute("name");
		String address = variable.getAttribute("address");
		StringBuilder out = new StringBuilder(name);
		if (!address.isEmpty()) {
			out.append(" AT ").append(address);
		}
		out.append(" : ").append(typeText(child(variable, "type"), problems, where + ", variable " + name));
		Element initial = child(variable, "initialValue");
		if (initial != null && !children(initial).isEmpty()) {
			out.append(" := ").append(valueText(children(initial).getFirst(), problems, where + ", variable " + name));
		}
		return out.append(';').toString();
	}

	private static String blockText(Element block, List<String> problems, String where) {
		StringBuilder out = new StringBuilder(KEYWORD.get(local(block)));
		if (block.getAttribute("constant").equals("true")) {
			out.append(" CONSTANT");
		}
		if (block.getAttribute("retain").equals("true")) {
			out.append(" RETAIN");
		}
		out.append('\n');
		for (Element variable : children(block, "variable")) {
			out.append('\t').append(variableText(variable, problems, where)).append('\n');
		}
		return out.append("END_VAR").toString();
	}

	private static String declarations(Element face, List<String> problems, String where) {
		List<String> blocks = new ArrayList<>();
		if (face != null) {
			for (Element block : children(face)) {
				if (KEYWORD.containsKey(local(block))) {
					blocks.add(blockText(block, problems, where));
				}
			}
		}
		return blocks.isEmpty() ? "VAR\nEND_VAR" : String.join("\n", blocks);
	}

	/** Reads PLCopen XML into program units. Any version of the TC6 namespace is read by its element names. No DTDs: a file is data. */
	public static Imported read(String xml) {
		List<String> problems = new ArrayList<>();
		Document document;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			var builder = factory.newDocumentBuilder();
			builder.setErrorHandler(null);
			document = builder.parse(new InputSource(new StringReader(xml)));
		} catch (Exception e) {
			return new Imported(List.of(), List.of("this is not an XML file: " + e.getMessage()));
		}
		Element project = document.getDocumentElement();
		if (!"project".equals(local(project)) || !project.getNamespaceURI().contains("plcopen")) {
			return new Imported(List.of(), List.of("this is not a PLCopen project"));
		}
		List<Source> sources = new ArrayList<>();
		Element types = child(project, "types");
		StringBuilder dataTypes = new StringBuilder();
		Element typeList = child(types, "dataTypes");
		if (typeList != null) {
			for (Element type : children(typeList, "dataType")) {
				String name = type.getAttribute("name");
				dataTypes.append('\t').append(name).append(" : ").append(typeText(child(type, "baseType"), problems, "type " + name));
				Element initial = child(type, "initialValue");
				if (initial != null && !children(initial).isEmpty()) {
					dataTypes.append(" := ").append(valueText(children(initial).getFirst(), problems, "type " + name));
				}
				dataTypes.append(";\n");
			}
		}
		if (dataTypes.length() > 0) {
			sources.add(new Source("TYPES", Language.ST, "TYPE\n" + dataTypes + "END_TYPE"));
		}
		Element pouList = child(types, "pous");
		if (pouList != null) {
			for (Element pou : children(pouList, "pou")) {
				try {
					Source source = readPou(pou, problems);
					if (source != null) {
						sources.add(source);
					}
				} catch (RuntimeException e) {
					problems.add("POU " + pou.getAttribute("name") + " could not be read: " + e);
				}
			}
		}
		String configuration = configurationText(child(child(project, "instances"), "configurations"), problems);
		if (!configuration.isEmpty()) {
			sources.add(new Source("CONFIG", Language.ST, configuration));
		}
		return problems.isEmpty() ? new Imported(sources, List.of()) : new Imported(List.of(), problems);
	}

	private static String configurationText(Element configurations, List<String> problems) {
		if (configurations == null) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (Element configuration : children(configurations, "configuration")) {
			String name = configuration.getAttribute("name");
			out.append("CONFIGURATION ").append(name).append('\n');
			for (Element block : children(configuration, "globalVars")) {
				out.append(blockText(block, problems, "configuration " + name)).append('\n');
			}
			for (Element resource : children(configuration, "resource")) {
				out.append("RESOURCE ").append(resource.getAttribute("name")).append(" ON CPU\n");
				for (Element block : children(resource, "globalVars")) {
					out.append(blockText(block, problems, "resource " + resource.getAttribute("name"))).append('\n');
				}
				StringBuilder programs = new StringBuilder();
				for (Element task : children(resource, "task")) {
					String taskName = task.getAttribute("name");
					List<String> fields = new ArrayList<>();
					if (!task.getAttribute("interval").isEmpty()) {
						fields.add("INTERVAL := " + task.getAttribute("interval"));
					}
					fields.add("PRIORITY := " + (task.getAttribute("priority").isEmpty() ? "0" : task.getAttribute("priority")));
					out.append("TASK ").append(taskName).append('(').append(String.join(", ", fields)).append(");\n");
					for (Element instance : children(task, "pouInstance")) {
						programs.append("PROGRAM ").append(instance.getAttribute("name")).append(" WITH ").append(taskName).append(" : ").append(instance.getAttribute("typeName")).append(";\n");
					}
				}
				for (Element instance : children(resource, "pouInstance")) {
					programs.append("PROGRAM ").append(instance.getAttribute("name")).append(" : ").append(instance.getAttribute("typeName")).append(";\n");
				}
				out.append(programs).append("END_RESOURCE\n");
			}
			out.append("END_CONFIGURATION\n");
		}
		return out.toString();
	}

	private static Source readPou(Element pou, List<String> problems) {
		String name = pou.getAttribute("name").toUpperCase(Locale.ROOT);
		String where = "POU " + name;
		String kind = switch (pou.getAttribute("pouType")) {
			case "function" -> "FUNCTION";
			case "functionBlock" -> "FUNCTION_BLOCK";
			default -> "PROGRAM";
		};
		Element face = child(pou, "interface");
		String vars = declarations(face, problems, where);
		Element body = child(pou, "body");
		if (body == null || children(body).isEmpty()) {
			problems.add(where + " has no body");
			return null;
		}
		Element language = children(body).getFirst();
		String tag = local(language);
		if (!tag.equals("SFC") && (child(pou, "actions") != null && !children(child(pou, "actions")).isEmpty() || child(pou, "transitions") != null && !children(child(pou, "transitions")).isEmpty())) {
			problems.add(where + " has actions or transitions outside a chart, which are not built");
		}
		if (tag.equals("SFC") && kind.equals("FUNCTION")) {
			problems.add(where + " is a function in SFC; a chart here is a PROGRAM or FUNCTION_BLOCK");
			return null;
		}
		String returns = kind.equals("FUNCTION") ? typeText(child(face, "returnType"), problems, where + "'s result") : "";
		switch (tag) {
			case "ST" -> {
				String head = kind + " " + name;
				if (kind.equals("FUNCTION")) {
					head += " : " + returns;
				}
				return new Source(name, Language.ST, head + "\n" + vars + "\n" + code(language) + "\nEND_" + kind);
			}
			case "LD" -> {
				Ladder ladder = readLadder(language, problems, where);
				ladder.kind = kind;
				ladder.name = name;
				ladder.returns = returns;
				ladder.declarations = vars;
				return new Source(name, Language.LD, ladder.write());
			}
			case "FBD" -> {
				Fbd fbd = readFbd(language, problems, where);
				fbd.kind = kind;
				fbd.name = name;
				fbd.returns = returns;
				fbd.declarations = vars;
				return new Source(name, Language.FBD, fbd.write());
			}
			case "SFC" -> {
				Sfc sfc = readSfc(pou, language, problems, where);
				sfc.kind = kind;
				sfc.name = name;
				sfc.declarations = vars;
				return new Source(name, Language.SFC, sfc.write());
			}
			default -> {
				problems.add(where + " is in " + tag + ", which is not one of this CPU's languages (instruction list left the standard in 2013)");
				return null;
			}
		}
	}

	// ---- function block diagram, in ------------------------------------------------------------------------------------------------------

	private static Fbd readFbd(Element body, List<String> problems, String where) {
		Fbd out = new Fbd();
		for (Element element : children(body)) {
			int id = number(element.getAttribute("localId"), -1);
			int[] p = at(element);
			switch (local(element)) {
				case "inVariable" -> {
					refuseNegated(element, problems, where);
					out.blocks.add(new Fbd.Block(id, Fbd.IN, code(child(element, "expression")), p[0], p[1]));
				}
				case "outVariable" -> {
					refuseNegated(element, problems, where);
					Fbd.Block block = new Fbd.Block(id, Fbd.OUT, code(child(element, "expression")), p[0], p[1]);
					List<Wire> feed = wires(child(element, "connectionPointIn"));
					if (!feed.isEmpty()) {
						block.inputs.put("IN", Fbd.wire(feed.getFirst().ref(), feed.getFirst().formal().isEmpty() ? "OUT" : feed.getFirst().formal()));
					}
					out.blocks.add(block);
				}
				case "block" -> {
					Fbd.Block block = new Fbd.Block(id, element.getAttribute("typeName").toUpperCase(Locale.ROOT), element.getAttribute("instanceName"), p[0], p[1]);
					Element inputs = child(element, "inputVariables");
					if (inputs != null) {
						for (Element variable : children(inputs, "variable")) {
							if (variable.getAttribute("negated").equals("true") || !variable.getAttribute("edge").isEmpty()) {
								problems.add(where + ": block " + id + " has a negated or edge input, which is not built");
							}
							List<Wire> feed = wires(child(variable, "connectionPointIn"));
							if (feed.size() > 1) {
								problems.add(where + ": block " + id + "'s " + variable.getAttribute("formalParameter") + " has several wires");
							}
							if (!feed.isEmpty()) {
								block.inputs.put(variable.getAttribute("formalParameter").toUpperCase(Locale.ROOT), Fbd.wire(feed.getFirst().ref(), feed.getFirst().formal().isEmpty() ? "OUT" : feed.getFirst().formal().toUpperCase(Locale.ROOT)));
							}
						}
					}
					Element inOuts = child(element, "inOutVariables");
					if (inOuts != null && !children(inOuts).isEmpty()) {
						problems.add(where + ": block " + id + " has in-out pins, which an FBD here does not wire");
					}
					out.blocks.add(block);
				}
				case "comment" -> {
				}
				default -> problems.add(where + ": " + local(element) + " is not built in an FBD here");
			}
		}
		return out;
	}

	private static void refuseNegated(Element element, List<String> problems, String where) {
		if (element.getAttribute("negated").equals("true") || !element.getAttribute("edge").isEmpty()) {
			problems.add(where + ": element " + element.getAttribute("localId") + " is negated or edge-triggered, which is not built on a variable here");
		}
	}

	// ---- chart, in --------------------------------------------------------------------------------------------------------------------

	private static Sfc readSfc(Element pou, Element body, List<String> problems, String where) {
		Sfc out = new Sfc();
		Map<Integer, Element> elements = new HashMap<>();
		Map<String, Sfc.Step> byName = new HashMap<>();
		for (Element element : children(body)) {
			if (element.hasAttribute("localId")) {
				elements.put(number(element.getAttribute("localId"), -1), element);
			}
		}
		Element actionList = child(pou, "actions");
		if (actionList != null) {
			for (Element action : children(actionList, "action")) {
				Element code = child(child(action, "body"), "ST");
				if (code == null) {
					problems.add(where + ": action " + action.getAttribute("name") + " is not in structured text, which is all a chart's action is here");
				} else {
					out.bodies.put(action.getAttribute("name").toUpperCase(Locale.ROOT), code(code));
				}
			}
		}
		Map<String, String> named = new HashMap<>();
		Element transitionList = child(pou, "transitions");
		if (transitionList != null) {
			for (Element transition : children(transitionList, "transition")) {
				String name = transition.getAttribute("name");
				Element code = child(child(transition, "body"), "ST");
				if (code == null) {
					problems.add(where + ": transition " + name + " is not in structured text");
				} else {
					Matcher matcher = Pattern.compile("^\\s*" + Pattern.quote(name) + "\\s*:=\\s*(.*?);?\\s*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(code(code));
					named.put(name.toUpperCase(Locale.ROOT), matcher.matches() ? matcher.group(1) : code(code).replaceAll(";\\s*$", ""));
				}
			}
		}
		for (Element element : children(body)) {
			int id = number(element.getAttribute("localId"), -1);
			int[] p = at(element);
			switch (local(element)) {
				case "step" -> {
					Sfc.Step step = new Sfc.Step(id, element.getAttribute("name").toUpperCase(Locale.ROOT), element.getAttribute("initialStep").equals("true"), p[0], p[1]);
					out.steps.add(step);
					byName.put(step.name, step);
				}
				case "transition" -> {
					Element condition = child(element, "condition");
					String text = "";
					if (condition != null && child(condition, "inline") != null) {
						text = code(child(child(condition, "inline"), "ST"));
						if (child(child(condition, "inline"), "ST") == null) {
							problems.add(where + ": transition " + id + "'s condition is not in structured text");
						}
					} else if (condition != null && child(condition, "reference") != null) {
						String name = child(condition, "reference").getAttribute("name").toUpperCase(Locale.ROOT);
						text = named.getOrDefault(name, name);
					} else if (condition != null) {
						problems.add(where + ": transition " + id + "'s condition is wired, in ladder or FBD; here a condition is structured text");
					}
					out.transitions.add(new Sfc.Transition(id, text.replaceAll(";\\s*$", ""), p[0], p[1]));
				}
				case "selectionDivergence", "selectionConvergence", "simultaneousDivergence", "simultaneousConvergence", "jumpStep", "actionBlock", "comment" -> {
				}
				default -> problems.add(where + ": " + local(element) + " is not built in a chart here");
			}
		}
		for (Sfc.Transition t : out.transitions) {
			stepsBefore(number(String.valueOf(t.id), -1), elements, t.from, new HashSet<>(), problems, where);
		}
		for (Sfc.Step step : out.steps) {
			Set<Integer> before = new LinkedHashSet<>();
			transitionsBefore(elements.get(step.id), elements, before, new HashSet<>(), problems, where);
			for (int id : before) {
				Sfc.Transition t = transition(out, id);
				if (t != null && !t.to.contains(step.id)) {
					t.to.add(step.id);
				}
			}
		}
		for (Element jump : children(body, "jumpStep")) {
			Sfc.Step target = byName.get(jump.getAttribute("targetName").toUpperCase(Locale.ROOT));
			if (target == null) {
				problems.add(where + ": a jump goes to " + jump.getAttribute("targetName") + ", and there is no such step");
				continue;
			}
			Set<Integer> before = new LinkedHashSet<>();
			transitionsBefore(jump, elements, before, new HashSet<>(), problems, where);
			for (int id : before) {
				Sfc.Transition t = transition(out, id);
				if (t != null && !t.to.contains(target.id)) {
					t.to.add(target.id);
				}
			}
		}
		for (Element block : children(body, "actionBlock")) {
			List<Wire> feed = wires(child(block, "connectionPointIn"));
			Sfc.Step step = feed.isEmpty() ? null : out.step(feed.getFirst().ref());
			if (step == null) {
				problems.add(where + ": an action block is not on a step");
				continue;
			}
			for (Element action : children(block, "action")) {
				Element reference = child(action, "reference");
				String name;
				if (reference != null) {
					name = reference.getAttribute("name");
				} else if (child(action, "inline") != null && child(child(action, "inline"), "ST") != null) {
					name = "INLINE_" + block.getAttribute("localId") + "_" + step.actions.size();
					out.bodies.put(name, code(child(child(action, "inline"), "ST")));
				} else {
					problems.add(where + ": step " + step.name + " has an action not in structured text");
					continue;
				}
				String qualifier = action.getAttribute("qualifier");
				step.actions.add(new Sfc.Association(qualifier.isEmpty() ? "N" : qualifier.toUpperCase(Locale.ROOT), name, action.getAttribute("duration")));
			}
		}
		return out;
	}

	private static Sfc.Transition transition(Sfc sfc, int id) {
		for (Sfc.Transition t : sfc.transitions) {
			if (t.id == id) {
				return t;
			}
		}
		return null;
	}

	/** Walks back from a transition through the bars to the steps before it. */
	private static void stepsBefore(int transitionId, Map<Integer, Element> elements, List<Integer> into, Set<Integer> seen, List<String> problems, String where) {
		Element element = elements.get(transitionId);
		if (element == null) {
			return;
		}
		walkBack(element, elements, (id, kind) -> {
			if (kind.equals("step")) {
				if (!into.contains(id)) {
					into.add(id);
				}
				return true;
			}
			if (kind.equals("transition")) {
				problems.add(where + ": transition " + id + " leads straight into transition " + transitionId);
				return true;
			}
			return false;
		}, seen, problems, where);
	}

	private static void transitionsBefore(Element element, Map<Integer, Element> elements, Set<Integer> into, Set<Integer> seen, List<String> problems, String where) {
		if (element == null) {
			return;
		}
		walkBack(element, elements, (id, kind) -> {
			if (kind.equals("transition")) {
				into.add(id);
				return true;
			}
			if (kind.equals("step")) {
				problems.add(where + ": step " + id + " leads straight into a step");
				return true;
			}
			return false;
		}, seen, problems, where);
	}

	private interface Visit {
		boolean found(int id, String kind);
	}

	private static void walkBack(Element from, Map<Integer, Element> elements, Visit visit, Set<Integer> seen, List<String> problems, String where) {
		for (Element point : children(from, "connectionPointIn")) {
			for (Wire wire : wires(point)) {
				Element source = elements.get(wire.ref());
				if (source == null) {
					problems.add(where + ": a wire comes from element " + wire.ref() + ", which is not there");
					continue;
				}
				if (!visit.found(wire.ref(), local(source)) && seen.add(wire.ref())) {
					walkBack(source, elements, visit, seen, problems, where);
				}
			}
		}
	}

	// ---- ladder, in -------------------------------------------------------------------------------------------------------------------

	private static final class Nets {
		final Map<String, String> parent = new HashMap<>();

		String find(String key) {
			String at = parent.computeIfAbsent(key, k -> k);
			while (!at.equals(parent.get(at))) {
				at = parent.get(at);
			}
			return at;
		}

		void join(String a, String b) {
			parent.put(find(a), find(b));
		}
	}

	private record Edge(String from, String to, java.util.function.Supplier<Ladder.Node> node, int index, boolean box, int id) {
	}

	private static String in(int id) {
		return "i" + id;
	}

	private static String out(int id) {
		return "o" + id;
	}

	private static Ladder readLadder(Element body, List<String> problems, String where) {
		Ladder out = new Ladder();
		Map<Integer, Element> elements = new LinkedHashMap<>();
		Map<Integer, Integer> index = new HashMap<>();
		int counter = 0;
		for (Element element : children(body)) {
			counter++;
			if (element.hasAttribute("localId")) {
				int id = number(element.getAttribute("localId"), -1);
				elements.put(id, element);
				index.put(id, counter);
			}
			switch (local(element)) {
				case "leftPowerRail", "rightPowerRail", "contact", "coil", "block", "inVariable", "outVariable", "comment", "jump", "return", "label" -> {
				}
				default -> problems.add(where + ": " + local(element) + " is not built in a ladder here");
			}
		}
		Nets nets = new Nets();
		String source = "RAIL";
		nets.find(source);
		Set<String> ends = new LinkedHashSet<>();
		Set<Integer> consumed = new HashSet<>();
		Map<Integer, String> boxPowerOut = new HashMap<>();
		Map<Integer, Map<String, String>> boxPins = new HashMap<>();
		// who is fed by what: what a block's output feeds tells its power from its bindings
		for (var entry : elements.entrySet()) {
			Element element = entry.getValue();
			int id = entry.getKey();
			switch (local(element)) {
				case "contact", "coil", "jump", "return" -> {
					if (local(element).equals("contact") && !element.getAttribute("edge").isEmpty()) {
						problems.add(where + ": contact " + id + " is edge-triggered, which is not built; use an R_TRIG box");
					}
					feed(wires(child(element, "connectionPointIn")), in(id), elements, nets, consumed, boxPowerOut, problems, where);
				}
				case "rightPowerRail" -> {
					for (Element point : children(element, "connectionPointIn")) {
						for (Wire wire : wires(point)) {
							Element from = elements.get(wire.ref());
							if (from != null && !local(from).equals("coil")) {
								ends.add(out(wire.ref()));
								if (local(from).equals("block")) {
									boxPowerOut.put(wire.ref(), wire.formal());
								}
								consumed.add(wire.ref());
							}
						}
					}
				}
				case "outVariable" -> {
					List<Wire> feed = wires(child(element, "connectionPointIn"));
					if (!feed.isEmpty() && elements.get(feed.getFirst().ref()) != null && local(elements.get(feed.getFirst().ref())).equals("block")) {
						boxPins.computeIfAbsent(feed.getFirst().ref(), k -> new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER)).put(feed.getFirst().formal().toUpperCase(Locale.ROOT), code(child(element, "expression")));
					} else {
						problems.add(where + ": a variable " + code(child(element, "expression")) + " is on a rung; only a box's pins take one");
					}
				}
				default -> {
				}
			}
		}
		for (var entry : elements.entrySet()) {
			if (local(entry.getValue()).equals("leftPowerRail")) {
				nets.join(out(entry.getKey()), source);
			}
		}
		for (var entry : elements.entrySet()) {
			if (!local(entry.getValue()).equals("block")) {
				continue;
			}
			int id = entry.getKey();
			Element inputs = child(entry.getValue(), "inputVariables");
			boolean power = false;
			if (inputs != null) {
				for (Element variable : children(inputs, "variable")) {
					String formal = variable.getAttribute("formalParameter").toUpperCase(Locale.ROOT);
					for (Wire wire : wires(child(variable, "connectionPointIn"))) {
						Element from = elements.get(wire.ref());
						if (from != null && local(from).equals("inVariable")) {
							if (!formal.equals("EN")) {
								boxPins.computeIfAbsent(id, k -> new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER)).put(formal, code(child(from, "expression")));
							}
						} else {
							feed(List.of(wire), in(id), elements, nets, consumed, boxPowerOut, problems, where);
							power = true;
						}
					}
				}
			}
			if (!power) {
				nets.join(in(id), source);
			}
		}
		// the coils: each pair of a rung's end, and a series coil passes power on
		Map<String, List<Ladder.Coil>> coils = new LinkedHashMap<>();
		for (var entry : elements.entrySet()) {
			if (local(entry.getValue()).equals("coil")) {
				nets.join(in(entry.getKey()), out(entry.getKey()));
			}
		}
		Map<String, Integer> coilIndex = new HashMap<>();
		for (var entry : elements.entrySet()) {
			Element element = entry.getValue();
			if (local(element).equals("coil")) {
				String storage = element.getAttribute("storage");
				boolean negated = element.getAttribute("negated").equals("true");
				if (negated && !storage.isEmpty() && !storage.equals("none")) {
					problems.add(where + ": coil " + entry.getKey() + " is both negated and stored, which is not built");
				}
				Ladder.CoilKind kind = storage.equals("set") ? Ladder.CoilKind.SET : storage.equals("reset") ? Ladder.CoilKind.RESET : negated ? Ladder.CoilKind.NOT : Ladder.CoilKind.COIL;
				String net = nets.find(in(entry.getKey()));
				coils.computeIfAbsent(net, k -> new ArrayList<>()).add(new Ladder.Coil(kind, code(child(element, "variable"))));
				coilIndex.merge(net, index.get(entry.getKey()), Math::min);
			} else if (local(element).equals("jump") || local(element).equals("return")) {
				boolean jump = local(element).equals("jump");
				String net = nets.find(in(entry.getKey()));
				coils.computeIfAbsent(net, k -> new ArrayList<>()).add(new Ladder.Coil(jump ? Ladder.CoilKind.JUMP : Ladder.CoilKind.RETURN, jump ? element.getAttribute("label").toUpperCase(Locale.ROOT) : ""));
				coilIndex.merge(net, index.get(entry.getKey()), Math::min);
			}
		}
		// the elements with power through them are edges between nets
		List<Edge> edges = new ArrayList<>();
		Map<Integer, Ladder.Box> boxes = new HashMap<>();
		for (var entry : elements.entrySet()) {
			Element element = entry.getValue();
			int id = entry.getKey();
			if (local(element).equals("contact")) {
				String variable = code(child(element, "variable"));
				boolean negated = element.getAttribute("negated").equals("true");
				edges.add(new Edge(nets.find(in(id)), nets.find(out(id)), () -> new Ladder.Contact(variable, negated), index.get(id), false, id));
				if (!consumed.contains(id)) {
					ends.add(out(id));
				}
			} else if (local(element).equals("block")) {
				Ladder.Box box = new Ladder.Box(element.getAttribute("typeName").toUpperCase(Locale.ROOT), element.getAttribute("instanceName"));
				boxPins.getOrDefault(id, Map.of()).forEach((pin, value) -> {
					if (!pin.equals(boxPowerOut.get(id)) && !pin.equals("ENO")) {
						box.pins.put(pin, value);
					}
				});
				boxes.put(id, box);
				edges.add(new Edge(nets.find(in(id)), nets.find(out(id)), () -> box, index.get(id), true, id));
				if (!consumed.contains(id)) {
					ends.add(out(id));
				}
			}
		}
		Set<String> targets = new LinkedHashSet<>(coils.keySet());
		for (String end : ends) {
			targets.add(nets.find(end));
		}
		String start = nets.find(source);
		List<Rung2> built = new ArrayList<>();
		Map<Integer, Integer> used = new HashMap<>();
		for (String target : targets) {
			Rung2 rung = rung(edges, start, target, used, problems, where);
			if (rung != null) {
				rung.rung.coils.addAll(coils.getOrDefault(target, List.of()));
				rung.first = Math.min(rung.first, coilIndex.getOrDefault(target, Integer.MAX_VALUE));
				built.add(rung);
			}
		}
		for (Edge edge : edges) {
			if (!used.containsKey(edge.id())) {
				problems.add(where + ": element " + edge.id() + " has no path from the rail to a coil");
			} else if (edge.box() && used.get(edge.id()) > 1) {
				problems.add(where + ": box " + edge.id() + " feeds two different rungs; a box here sits on one");
			}
		}
		built.sort(java.util.Comparator.comparingInt(r -> r.first));
		for (var entry : elements.entrySet()) {
			if (local(entry.getValue()).equals("comment")) {
				int where2 = index.get(entry.getKey());
				for (Rung2 rung : built) {
					if (rung.first > where2 && rung.rung.comment.isEmpty()) {
						rung.rung.comment = code(child(entry.getValue(), "content"));
						break;
					}
				}
			}
		}
		// a label stands before the rung it names
		for (var entry : elements.entrySet()) {
			if (local(entry.getValue()).equals("label")) {
				int where2 = index.get(entry.getKey());
				Rung2 named = null;
				for (Rung2 rung : built) {
					if (rung.first > where2 && rung.rung.label.isEmpty()) {
						named = rung;
						break;
					}
				}
				if (named == null) {
					problems.add(where + ": label " + entry.getValue().getAttribute("label") + " has no rung after it");
				} else {
					named.rung.label = entry.getValue().getAttribute("label").toUpperCase(Locale.ROOT);
				}
			}
		}
		built.forEach(r -> out.rungs.add(r.rung));
		return out;
	}

	private static final class Rung2 {
		final Ladder.Rung rung = new Ladder.Rung();
		int first = Integer.MAX_VALUE;
	}

	private static void feed(List<Wire> wires, String into, Map<Integer, Element> elements, Nets nets, Set<Integer> consumed, Map<Integer, String> boxPowerOut, List<String> problems, String where) {
		for (Wire wire : wires) {
			Element from = elements.get(wire.ref());
			if (from == null) {
				problems.add(where + ": a wire comes from element " + wire.ref() + ", which is not there");
				continue;
			}
			if (local(from).equals("inVariable")) {
				problems.add(where + ": a variable " + code(child(from, "expression")) + " feeds a rung; only a box's pins take one");
				continue;
			}
			consumed.add(wire.ref());
			if (local(from).equals("block")) {
				String formal = wire.formal().toUpperCase(Locale.ROOT);
				String prior = boxPowerOut.put(wire.ref(), formal);
				if (prior != null && !prior.isEmpty() && !prior.equals(formal)) {
					problems.add(where + ": box " + wire.ref() + " has wires from two of its outputs; here a box gives power out of one");
				}
			}
			nets.join(into, out(wire.ref()));
		}
	}

	/** The part of the graph between the rail and {@code target}, reduced to series and parallel; null if it has none. A bridge cannot be drawn, so it is refused. */
	private static Rung2 rung(List<Edge> all, String start, String target, Map<Integer, Integer> used, List<String> problems, String where) {
		Set<String> ahead = new HashSet<>(Set.of(start));
		Set<String> behind = new HashSet<>(Set.of(target));
		for (boolean changed = true; changed;) {
			changed = false;
			for (Edge edge : all) {
				if (ahead.contains(edge.from())) {
					changed |= ahead.add(edge.to());
				}
				if (behind.contains(edge.to())) {
					changed |= behind.add(edge.from());
				}
			}
		}
		Rung2 out = new Rung2();
		record Live(String from, String to, Ladder.Node node) {
		}
		List<Live> live = new ArrayList<>();
		for (Edge edge : all) {
			if (ahead.contains(edge.from()) && behind.contains(edge.to())) {
				live.add(new Live(edge.from(), edge.to(), edge.node().get()));
				used.merge(edge.id(), 1, Integer::sum);
				out.first = Math.min(out.first, edge.index());
			}
		}
		boolean changed = true;
		while (changed && live.size() > 1) {
			changed = false;
			// parallel: the same two ends
			outer:
			for (int a = 0; a < live.size(); a++) {
				for (int b = a + 1; b < live.size(); b++) {
					if (live.get(a).from().equals(live.get(b).from()) && live.get(a).to().equals(live.get(b).to())) {
						Ladder.Parallel parallel = new Ladder.Parallel();
						addFlat(parallel, live.get(a).node());
						addFlat(parallel, live.get(b).node());
						Live merged = new Live(live.get(a).from(), live.get(a).to(), parallel);
						live.remove(b);
						live.set(a, merged);
						changed = true;
						break outer;
					}
				}
			}
			if (changed) {
				continue;
			}
			// series: a net with one way in and one way out
			for (int a = 0; a < live.size() && !changed; a++) {
				String net = live.get(a).to();
				if (net.equals(target) || net.equals(start)) {
					continue;
				}
				int ins = 0;
				int outs = 0;
				int next = -1;
				for (int b = 0; b < live.size(); b++) {
					if (live.get(b).to().equals(net)) {
						ins++;
					}
					if (live.get(b).from().equals(net)) {
						outs++;
						next = b;
					}
				}
				if (ins == 1 && outs == 1) {
					Ladder.Series series = new Ladder.Series();
					addFlat(series, live.get(a).node());
					addFlat(series, live.get(next).node());
					Live merged = new Live(live.get(a).from(), live.get(next).to(), series);
					int low = Math.min(a, next);
					int high = Math.max(a, next);
					live.remove(high);
					live.remove(low);
					live.add(low, merged);
					changed = true;
				}
			}
		}
		if (live.size() > 1) {
			problems.add(where + ": a rung here is a bridge, not series and parallel, which a ladder here cannot draw");
			return null;
		}
		if (live.isEmpty()) {
			return start.equals(target) ? out : null;
		}
		addFlat(out.rung.body, live.getFirst().node());
		return out;
	}

	private static void addFlat(Ladder.Series into, Ladder.Node node) {
		if (node instanceof Ladder.Series series) {
			into.items.addAll(series.items);
		} else {
			into.items.add(node);
		}
	}

	private static void addFlat(Ladder.Parallel into, Ladder.Node node) {
		if (node instanceof Ladder.Parallel parallel) {
			into.items.addAll(parallel.items);
		} else {
			into.items.add(node);
		}
	}

	/** The project's structured text units as one source, and the names of those it could not include. */
	public static String writeStructuredText(List<Source> sources, List<String> leftOut) {
		StringBuilder out = new StringBuilder();
		for (Source source : sources) {
			if (source.language() == Language.ST) {
				out.append("(* ").append(source.name()).append(" *)\n").append(source.text()).append("\n\n");
			} else {
				leftOut.add(source.name());
			}
		}
		return out.toString();
	}
}
