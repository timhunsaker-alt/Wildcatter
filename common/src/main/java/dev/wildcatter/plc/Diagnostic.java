package dev.wildcatter.plc;

/** One line of the CPU's diagnostic buffer. {@code pou} and {@code line} say where, when a program caused it. */
public record Diagnostic(long timeMs, String event, String pou, int line) {
	@Override
	public String toString() {
		return pou == null ? event : event + " in " + pou + " at line " + line;
	}
}
