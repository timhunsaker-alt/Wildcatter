package dev.wildcatter.plc.lang;

/** Unwinds to the nearest statement or declaration, which records the error and carries on. */
final class CompileException extends RuntimeException {
	final int line;
	final int column;

	CompileException(int line, int column, String message) {
		super(message, null, false, false);
		this.line = line;
		this.column = column;
	}
}
