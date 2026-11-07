package dev.wildcatter.plc.lang;

import java.util.List;

/** The program tree. Structured text parses to it, and the graphical languages will be drawn down to it, so one compiler serves all. */
public final class Ast {
	private Ast() {
	}

	public sealed interface Expr {
		int line();
	}

	public record IntLit(int line, long value, String type) implements Expr {
	}

	public record RealLit(int line, double value, String type) implements Expr {
	}

	public record BoolLit(int line, boolean value) implements Expr {
	}

	public record TimeLit(int line, long ms) implements Expr {
	}

	public record StringLit(int line, String value) implements Expr {
	}

	public record EnumLit(int line, String type, String value) implements Expr {
	}

	public record Name(int line, String name) implements Expr {
	}

	public record Member(int line, Expr base, String field) implements Expr {
	}

	public record BitOf(int line, Expr base, int bit) implements Expr {
	}

	public record Index(int line, Expr base, List<Expr> index) implements Expr {
	}

	public record Direct(int line, String address) implements Expr {
	}

	public record Unary(int line, String op, Expr value) implements Expr {
	}

	public record Binary(int line, String op, Expr left, Expr right) implements Expr {
	}

	public record Call(int line, String name, Expr target, List<Arg> args) implements Expr {
	}

	/** {@code name := value}, {@code name => value} when {@code output}, or a positional value when {@code name} is null. */
	public record Arg(String name, Expr value, boolean output) {
	}

	public sealed interface Stmt {
		int line();
	}

	public record Assign(int line, Expr target, Expr value) implements Stmt {
	}

	public record CallStmt(int line, Call call) implements Stmt {
	}

	public record If(int line, List<Branch> branches, List<Stmt> otherwise) implements Stmt {
	}

	public record Branch(Expr condition, List<Stmt> body) {
	}

	public record Case(int line, Expr selector, List<CaseArm> arms, List<Stmt> otherwise) implements Stmt {
	}

	public record CaseArm(List<CaseLabel> labels, List<Stmt> body) {
	}

	/** One value, or a range when {@code high} is not null. */
	public record CaseLabel(Expr low, Expr high) {
	}

	public record For(int line, Expr variable, Expr from, Expr to, Expr by, List<Stmt> body) implements Stmt {
	}

	public record While(int line, Expr condition, List<Stmt> body) implements Stmt {
	}

	public record Repeat(int line, List<Stmt> body, Expr until) implements Stmt {
	}

	public record Exit(int line) implements Stmt {
	}

	public record Continue(int line) implements Stmt {
	}

	public record Return(int line) implements Stmt {
	}

	public sealed interface TypeSpec {
	}

	public record NamedType(int line, String name) implements TypeSpec {
	}

	public record ArrayType(int line, List<Expr> low, List<Expr> high, TypeSpec element) implements TypeSpec {
	}

	public record StructType(int line, List<VarDecl> fields) implements TypeSpec {
	}

	public record EnumType(int line, List<String> values) implements TypeSpec {
	}

	/** The type of what {@code of} computes: a graphical language's result, which has no written type. */
	public record AutoType(int line, Expr of) implements TypeSpec {
	}

	public sealed interface Init {
	}

	public record ExprInit(Expr value) implements Init {
	}

	/** {@code [1, 2, 3(0)]}: each item with its repeat count. */
	public record ArrayInit(List<Init> items, List<Integer> counts) implements Init {
	}

	/** {@code (PT := T#5s, IN := TRUE)}. */
	public record StructInit(List<String> names, List<Init> values) implements Init {
	}

	public enum VarKind {
		VAR, INPUT, OUTPUT, IN_OUT, TEMP, GLOBAL, EXTERNAL
	}

	public record VarDecl(int line, int column, String name, VarKind kind, boolean constant, boolean retain, String at, TypeSpec type, Init init) {
	}

	public enum PouKind {
		PROGRAM, FUNCTION_BLOCK, FUNCTION
	}

	public record Pou(int line, PouKind kind, String name, TypeSpec returns, List<VarDecl> vars, List<Stmt> body) {
	}

	public record TypeDecl(int line, String name, TypeSpec type, Init init) {
	}

	public record TaskDecl(int line, String name, long intervalMs, int priority) {
	}

	public record ProgramInstance(int line, String instance, String task, String program) {
	}

	/** One source's worth of declarations; a project is any number of these. */
	public record Unit(List<TypeDecl> types, List<VarDecl> globals, List<Pou> pous, List<TaskDecl> tasks, List<ProgramInstance> instances) {
	}
}
