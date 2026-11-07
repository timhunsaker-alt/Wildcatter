package dev.wildcatter.plc.lang;

/** One compile error, as a real IDE lists it: where, and what. */
public record CompileError(int line, int column, String message) {
	@Override
	public String toString() {
		return "line " + line + ":" + column + ": " + message;
	}
}
