package dev.wildcatter.plc.lang;

/** One compile error, as a real IDE lists it: which program unit, where, and what. In a ladder the line is the rung; in an FBD, the block. */
public record CompileError(String source, int line, int column, String message) {
	public CompileError(int line, int column, String message) {
		this("", line, column, message);
	}

	@Override
	public String toString() {
		return (source.isEmpty() ? "" : source + " ") + "line " + line + ":" + column + ": " + message;
	}
}
