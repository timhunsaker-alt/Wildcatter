package dev.wildcatter.plc.lang;

import dev.wildcatter.plc.lang.Ast.Arg;
import dev.wildcatter.plc.lang.Ast.Expr;
import dev.wildcatter.plc.lang.Ast.Stmt;
import dev.wildcatter.plc.lang.Ast.VarKind;
import dev.wildcatter.plc.lang.Lexer.Kind;
import dev.wildcatter.plc.lang.Lexer.Token;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Structured text to the program tree. An error is recorded and parsing resumes at the next statement or declaration, as an IDE does. */
final class Parser {
	private static final Set<String> STATEMENT_ENDS = Set.of("END_IF", "ELSIF", "ELSE", "END_CASE", "END_FOR", "END_WHILE", "UNTIL", "END_REPEAT", "END_PROGRAM", "END_FUNCTION", "END_FUNCTION_BLOCK");

	private final List<Token> tokens;
	private final List<CompileError> errors;
	private int at;

	private Parser(List<Token> tokens, List<CompileError> errors) {
		this.tokens = tokens;
		this.errors = errors;
	}

	static Ast.Unit parse(String source, List<CompileError> errors) {
		try {
			return new Parser(Lexer.tokens(source), errors).unit();
		} catch (CompileException e) {
			errors.add(new CompileError(e.line, e.column, e.getMessage()));
			return new Ast.Unit(List.of(), List.of(), List.of(), List.of(), List.of());
		}
	}

	private Token peek() {
		return tokens.get(at);
	}

	private Token peek(int ahead) {
		return tokens.get(Math.min(at + ahead, tokens.size() - 1));
	}

	private Token advance() {
		Token token = peek();
		if (token.kind() != Kind.END) {
			at++;
		}
		return token;
	}

	private boolean accept(String text) {
		if (peek().is(text)) {
			advance();
			return true;
		}
		return false;
	}

	private CompileException error(Token token, String message) {
		return new CompileException(token.line(), token.column(), message);
	}

	private Token expect(String text) {
		if (!peek().is(text)) {
			throw error(peek(), "expected " + text + " but found " + peek().text());
		}
		return advance();
	}

	private String ident(String what) {
		if (peek().kind() != Kind.IDENT) {
			throw error(peek(), "expected " + what + " but found " + peek().text());
		}
		return advance().text();
	}

	private void record(CompileException e) {
		errors.add(new CompileError(e.line, e.column, e.getMessage()));
	}

	/** Resumes after the next semicolon, without passing the end of the block. */
	private void recover() {
		while (peek().kind() != Kind.END && !STATEMENT_ENDS.contains(peek().text())) {
			if (advance().is(";")) {
				return;
			}
		}
	}

	private Ast.Unit unit() {
		List<Ast.TypeDecl> types = new ArrayList<>();
		List<Ast.VarDecl> globals = new ArrayList<>();
		List<Ast.Pou> pous = new ArrayList<>();
		List<Ast.TaskDecl> tasks = new ArrayList<>();
		List<Ast.ProgramInstance> instances = new ArrayList<>();
		while (peek().kind() != Kind.END) {
			Token start = peek();
			try {
				switch (start.text()) {
					case "TYPE" -> typeBlock(types);
					case "VAR_GLOBAL" -> globals.addAll(varBlock());
					case "PROGRAM" -> pous.add(pou(Ast.PouKind.PROGRAM, "END_PROGRAM"));
					case "FUNCTION_BLOCK" -> pous.add(pou(Ast.PouKind.FUNCTION_BLOCK, "END_FUNCTION_BLOCK"));
					case "FUNCTION" -> pous.add(pou(Ast.PouKind.FUNCTION, "END_FUNCTION"));
					case "CONFIGURATION" -> configuration(globals, tasks, instances);
					default -> throw error(start, "expected a PROGRAM, FUNCTION_BLOCK, FUNCTION, TYPE, VAR_GLOBAL or CONFIGURATION, but found " + start.text());
				}
			} catch (CompileException e) {
				record(e);
				while (peek().kind() != Kind.END && !Set.of("TYPE", "VAR_GLOBAL", "PROGRAM", "FUNCTION_BLOCK", "FUNCTION", "CONFIGURATION").contains(peek().text())) {
					advance();
				}
			}
		}
		return new Ast.Unit(types, globals, pous, tasks, instances);
	}

	private void typeBlock(List<Ast.TypeDecl> into) {
		expect("TYPE");
		while (!accept("END_TYPE")) {
			int line = peek().line();
			String name = ident("a type name");
			expect(":");
			Ast.TypeSpec spec = typeSpec();
			Ast.Init init = accept(":=") ? init() : null;
			accept(";");
			into.add(new Ast.TypeDecl(line, name, spec, init));
		}
	}

	private Ast.TypeSpec typeSpec() {
		int line = peek().line();
		if (accept("ARRAY")) {
			expect("[");
			List<Expr> low = new ArrayList<>();
			List<Expr> high = new ArrayList<>();
			do {
				low.add(expr());
				expect("..");
				high.add(expr());
			} while (accept(","));
			expect("]");
			expect("OF");
			return new Ast.ArrayType(line, low, high, typeSpec());
		}
		if (accept("STRUCT")) {
			List<Ast.VarDecl> fields = new ArrayList<>();
			while (!accept("END_STRUCT")) {
				fields.addAll(declaration(VarKind.VAR, false, false));
			}
			return new Ast.StructType(line, fields);
		}
		if (accept("(")) {
			List<String> values = new ArrayList<>();
			do {
				values.add(ident("an enumerated value"));
			} while (accept(","));
			expect(")");
			return new Ast.EnumType(line, values);
		}
		String name = ident("a type");
		if (name.equals("STRING") && accept("[")) {
			expr();
			expect("]");
		}
		return new Ast.NamedType(line, name);
	}

	private Ast.Init init() {
		if (accept("[")) {
			List<Ast.Init> items = new ArrayList<>();
			List<Integer> counts = new ArrayList<>();
			do {
				if (peek().kind() == Kind.INT && peek(1).is("(")) {
					int count = (int) advance().whole();
					expect("(");
					items.add(init());
					expect(")");
					counts.add(count);
				} else {
					items.add(init());
					counts.add(1);
				}
			} while (accept(","));
			expect("]");
			return new Ast.ArrayInit(items, counts);
		}
		if (peek().is("(") && peek(1).kind() == Kind.IDENT && peek(2).is(":=")) {
			advance();
			List<String> names = new ArrayList<>();
			List<Ast.Init> values = new ArrayList<>();
			do {
				names.add(ident("a member"));
				expect(":=");
				values.add(init());
			} while (accept(","));
			expect(")");
			return new Ast.StructInit(names, values);
		}
		return new Ast.ExprInit(expr());
	}

	private List<Ast.VarDecl> varBlock() {
		VarKind kind = switch (advance().text()) {
			case "VAR_INPUT" -> VarKind.INPUT;
			case "VAR_OUTPUT" -> VarKind.OUTPUT;
			case "VAR_IN_OUT" -> VarKind.IN_OUT;
			case "VAR_TEMP" -> VarKind.TEMP;
			case "VAR_GLOBAL" -> VarKind.GLOBAL;
			case "VAR_EXTERNAL" -> VarKind.EXTERNAL;
			default -> VarKind.VAR;
		};
		boolean constant = false;
		boolean retain = false;
		while (peek().is("CONSTANT") || peek().is("RETAIN") || peek().is("NON_RETAIN")) {
			String word = advance().text();
			constant |= word.equals("CONSTANT");
			retain |= word.equals("RETAIN");
		}
		List<Ast.VarDecl> out = new ArrayList<>();
		while (!accept("END_VAR")) {
			if (peek().kind() == Kind.END) {
				throw error(peek(), "this variable block has no END_VAR");
			}
			try {
				out.addAll(declaration(kind, constant, retain));
			} catch (CompileException e) {
				record(e);
				while (peek().kind() != Kind.END && !peek().is(";") && !peek().is("END_VAR")) {
					advance();
				}
				accept(";");
			}
		}
		return out;
	}

	private List<Ast.VarDecl> declaration(VarKind kind, boolean constant, boolean retain) {
		List<Token> names = new ArrayList<>();
		do {
			if (peek().kind() != Kind.IDENT) {
				throw error(peek(), "expected a variable name but found " + peek().text());
			}
			names.add(advance());
		} while (accept(","));
		String located = null;
		if (accept("AT")) {
			if (peek().kind() != Kind.DIRECT) {
				throw error(peek(), "AT needs a direct address such as %IX0.3");
			}
			located = advance().text();
		}
		expect(":");
		Ast.TypeSpec type = typeSpec();
		Ast.Init init = accept(":=") ? init() : null;
		expect(";");
		List<Ast.VarDecl> out = new ArrayList<>();
		for (Token name : names) {
			out.add(new Ast.VarDecl(name.line(), name.column(), name.text(), kind, constant, retain, located, type, init));
		}
		return out;
	}

	private Ast.Pou pou(Ast.PouKind kind, String end) {
		int line = advance().line();
		String name = ident("a name");
		Ast.TypeSpec returns = null;
		if (kind == Ast.PouKind.FUNCTION) {
			expect(":");
			returns = typeSpec();
		}
		List<Ast.VarDecl> vars = new ArrayList<>();
		while (peek().text().startsWith("VAR") && peek().kind() == Kind.IDENT) {
			vars.addAll(varBlock());
		}
		List<Stmt> body = statements();
		expect(end);
		return new Ast.Pou(line, kind, name, returns, vars, body);
	}

	private void configuration(List<Ast.VarDecl> globals, List<Ast.TaskDecl> tasks, List<Ast.ProgramInstance> instances) {
		expect("CONFIGURATION");
		ident("a configuration name");
		while (!accept("END_CONFIGURATION")) {
			if (peek().is("VAR_GLOBAL")) {
				globals.addAll(varBlock());
			} else if (accept("RESOURCE")) {
				ident("a resource name");
				expect("ON");
				ident("a processor name");
				while (!accept("END_RESOURCE")) {
					if (peek().is("VAR_GLOBAL")) {
						globals.addAll(varBlock());
					} else {
						taskOrProgram(tasks, instances);
					}
				}
			} else {
				taskOrProgram(tasks, instances);
			}
		}
	}

	private void taskOrProgram(List<Ast.TaskDecl> tasks, List<Ast.ProgramInstance> instances) {
		int line = peek().line();
		if (accept("TASK")) {
			String name = ident("a task name");
			expect("(");
			long interval = 0;
			int priority = 0;
			do {
				String field = ident("INTERVAL or PRIORITY");
				expect(":=");
				if (field.equals("INTERVAL")) {
					if (peek().kind() != Kind.TIME) {
						throw error(peek(), "a task's INTERVAL is a time such as T#100ms");
					}
					interval = advance().whole();
				} else if (field.equals("PRIORITY")) {
					priority = (int) advance().whole();
				} else {
					throw error(peek(), "a task takes INTERVAL and PRIORITY, not " + field);
				}
			} while (accept(","));
			expect(")");
			expect(";");
			tasks.add(new Ast.TaskDecl(line, name, interval, priority));
			return;
		}
		expect("PROGRAM");
		String instance = ident("a program instance name");
		String task = accept("WITH") ? ident("a task name") : null;
		expect(":");
		String program = ident("a program");
		if (accept("(")) {
			throw error(peek(), "passing variables to a program instance is not built");
		}
		expect(";");
		instances.add(new Ast.ProgramInstance(line, instance, task, program));
	}

	private List<Stmt> statements() {
		List<Stmt> out = new ArrayList<>();
		while (peek().kind() != Kind.END && !STATEMENT_ENDS.contains(peek().text())) {
			try {
				Stmt stmt = statement();
				if (stmt != null) {
					out.add(stmt);
				}
			} catch (CompileException e) {
				record(e);
				recover();
			}
		}
		return out;
	}

	private Stmt statement() {
		Token start = peek();
		int line = start.line();
		if (accept(";")) {
			return null;
		}
		if (start.kind() == Kind.IDENT) {
			switch (start.text()) {
				case "IF" -> {
					return ifStatement();
				}
				case "CASE" -> {
					return caseStatement();
				}
				case "FOR" -> {
					return forStatement();
				}
				case "WHILE" -> {
					advance();
					Expr condition = expr();
					expect("DO");
					List<Stmt> body = statements();
					expect("END_WHILE");
					accept(";");
					return new Ast.While(line, condition, body);
				}
				case "REPEAT" -> {
					advance();
					List<Stmt> body = statements();
					expect("UNTIL");
					Expr until = expr();
					expect("END_REPEAT");
					accept(";");
					return new Ast.Repeat(line, body, until);
				}
				case "EXIT" -> {
					advance();
					expect(";");
					return new Ast.Exit(line);
				}
				case "CONTINUE" -> {
					advance();
					expect(";");
					return new Ast.Continue(line);
				}
				case "RETURN" -> {
					advance();
					expect(";");
					return new Ast.Return(line);
				}
				default -> {
				}
			}
		}
		Expr target = suffixed();
		if (accept(":=")) {
			Expr value = expr();
			expect(";");
			return new Ast.Assign(line, target, value);
		}
		if (target instanceof Ast.Call call) {
			expect(";");
			return new Ast.CallStmt(line, call);
		}
		throw error(peek(), "expected := or a call, but found " + peek().text());
	}

	private Stmt ifStatement() {
		int line = advance().line();
		List<Ast.Branch> branches = new ArrayList<>();
		Expr condition = expr();
		expect("THEN");
		branches.add(new Ast.Branch(condition, statements()));
		while (accept("ELSIF")) {
			condition = expr();
			expect("THEN");
			branches.add(new Ast.Branch(condition, statements()));
		}
		List<Stmt> otherwise = accept("ELSE") ? statements() : List.of();
		expect("END_IF");
		accept(";");
		return new Ast.If(line, branches, otherwise);
	}

	private Stmt caseStatement() {
		int line = advance().line();
		Expr selector = expr();
		expect("OF");
		List<Ast.CaseArm> arms = new ArrayList<>();
		List<Stmt> otherwise = List.of();
		while (!accept("END_CASE")) {
			if (accept("ELSE")) {
				otherwise = caseBody();
				expect("END_CASE");
				break;
			}
			List<Ast.CaseLabel> labels = new ArrayList<>();
			do {
				Expr low = expr();
				labels.add(new Ast.CaseLabel(low, accept("..") ? expr() : null));
			} while (accept(","));
			expect(":");
			arms.add(new Ast.CaseArm(labels, caseBody()));
		}
		accept(";");
		return new Ast.Case(line, selector, arms, otherwise);
	}

	/** A CASE arm's statements run until the next label, which looks like an expression followed by ":" or ".." or ",". */
	private List<Stmt> caseBody() {
		List<Stmt> out = new ArrayList<>();
		while (peek().kind() != Kind.END && !peek().is("END_CASE") && !peek().is("ELSE") && !atCaseLabel()) {
			try {
				Stmt stmt = statement();
				if (stmt != null) {
					out.add(stmt);
				}
			} catch (CompileException e) {
				record(e);
				recover();
			}
		}
		return out;
	}

	private boolean atCaseLabel() {
		int i = at;
		if (tokens.get(i).is("-")) {
			i++;
		}
		Token first = tokens.get(i);
		boolean value = first.kind() == Kind.INT || first.kind() == Kind.IDENT;
		if (first.kind() == Kind.TYPED) {
			i++;
		}
		if (!value && first.kind() != Kind.TYPED) {
			return false;
		}
		Token after = tokens.get(Math.min(i + 1, tokens.size() - 1));
		return after.is(":") || after.is("..") || after.is(",");
	}

	private Stmt forStatement() {
		int line = advance().line();
		Expr variable = suffixed();
		expect(":=");
		Expr from = expr();
		expect("TO");
		Expr to = expr();
		Expr by = accept("BY") ? expr() : null;
		expect("DO");
		List<Stmt> body = statements();
		expect("END_FOR");
		accept(";");
		return new Ast.For(line, variable, from, to, by, body);
	}

	// Expressions, in the standard's order: OR, XOR, AND, comparison, equality, add, multiply, power, unary, primary.

	Expr expr() {
		Expr left = xor();
		while (peek().is("OR")) {
			int line = advance().line();
			left = new Ast.Binary(line, "OR", left, xor());
		}
		return left;
	}

	private Expr xor() {
		Expr left = and();
		while (peek().is("XOR")) {
			int line = advance().line();
			left = new Ast.Binary(line, "XOR", left, and());
		}
		return left;
	}

	private Expr and() {
		Expr left = equality();
		while (peek().is("AND") || peek().is("&")) {
			int line = advance().line();
			left = new Ast.Binary(line, "AND", left, equality());
		}
		return left;
	}

	private Expr equality() {
		Expr left = comparison();
		while (peek().is("=") || peek().is("<>")) {
			Token op = advance();
			left = new Ast.Binary(op.line(), op.text(), left, comparison());
		}
		return left;
	}

	private Expr comparison() {
		Expr left = additive();
		while (peek().is("<") || peek().is(">") || peek().is("<=") || peek().is(">=")) {
			Token op = advance();
			left = new Ast.Binary(op.line(), op.text(), left, additive());
		}
		return left;
	}

	private Expr additive() {
		Expr left = multiplicative();
		while (peek().is("+") || peek().is("-")) {
			Token op = advance();
			left = new Ast.Binary(op.line(), op.text(), left, multiplicative());
		}
		return left;
	}

	private Expr multiplicative() {
		Expr left = power();
		while (peek().is("*") || peek().is("/") || peek().is("MOD")) {
			Token op = advance();
			left = new Ast.Binary(op.line(), op.text(), left, power());
		}
		return left;
	}

	private Expr power() {
		Expr left = unary();
		while (peek().is("**")) {
			int line = advance().line();
			left = new Ast.Binary(line, "**", left, unary());
		}
		return left;
	}

	private Expr unary() {
		if (peek().is("-") || peek().is("+") || peek().is("NOT")) {
			Token op = advance();
			return new Ast.Unary(op.line(), op.text(), unary());
		}
		return suffixed();
	}

	private Expr suffixed() {
		Expr out = primary();
		while (true) {
			Token next = peek();
			if (next.is(".")) {
				advance();
				if (peek().kind() == Kind.INT) {
					out = new Ast.BitOf(next.line(), out, (int) advance().whole());
				} else {
					out = new Ast.Member(next.line(), out, ident("a member"));
				}
			} else if (next.is("[")) {
				advance();
				List<Expr> index = new ArrayList<>();
				do {
					index.add(expr());
				} while (accept(","));
				expect("]");
				out = new Ast.Index(next.line(), out, index);
			} else if (next.is("(") && (out instanceof Ast.Name || out instanceof Ast.Member || out instanceof Ast.Index)) {
				advance();
				List<Arg> args = args();
				out = new Ast.Call(next.line(), out instanceof Ast.Name name ? name.name() : null, out, args);
			} else {
				return out;
			}
		}
	}

	private List<Arg> args() {
		List<Arg> args = new ArrayList<>();
		if (accept(")")) {
			return args;
		}
		do {
			if (peek().kind() == Kind.IDENT && (peek(1).is(":=") || peek(1).is("=>"))) {
				String name = advance().text();
				boolean output = advance().is("=>");
				args.add(new Arg(name, output ? suffixed() : expr(), output));
			} else {
				args.add(new Arg(null, expr(), false));
			}
		} while (accept(","));
		expect(")");
		return args;
	}

	private Expr primary() {
		Token token = advance();
		int line = token.line();
		switch (token.kind()) {
			case INT:
				return new Ast.IntLit(line, token.whole(), null);
			case REAL:
				return new Ast.RealLit(line, token.real(), null);
			case TIME:
				return new Ast.TimeLit(line, token.whole());
			case STRING:
				return new Ast.StringLit(line, token.text());
			case DIRECT:
				return new Ast.Direct(line, token.text());
			case TYPED:
				return typedLiteral(token);
			case IDENT:
				if (token.text().equals("TRUE") || token.text().equals("FALSE")) {
					return new Ast.BoolLit(line, token.text().equals("TRUE"));
				}
				return new Ast.Name(line, token.text());
			default:
				if (token.is("(")) {
					Expr inner = expr();
					expect(")");
					return inner;
				}
				throw error(token, "expected a value but found " + token.text());
		}
	}

	private Expr typedLiteral(Token type) {
		int line = type.line();
		boolean negative = accept("-");
		Token value = advance();
		return switch (value.kind()) {
			case INT -> new Ast.IntLit(line, negative ? -value.whole() : value.whole(), type.text());
			case REAL -> new Ast.RealLit(line, negative ? -value.real() : value.real(), type.text());
			case IDENT -> {
				if (type.text().equals("BOOL") && (value.text().equals("TRUE") || value.text().equals("FALSE"))) {
					yield new Ast.BoolLit(line, value.text().equals("TRUE"));
				}
				yield new Ast.EnumLit(line, type.text(), value.text());
			}
			default -> throw error(value, "expected a value after " + type.text() + "#");
		};
	}
}
