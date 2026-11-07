package dev.wildcatter.plc.lang;

import java.util.ArrayList;
import java.util.List;

/** The graphical languages' saved form: parenthesised lists of quoted strings, one a line of text. */
final class Sexpr {
	final String atom;
	final List<Sexpr> items;

	private Sexpr(String atom, List<Sexpr> items) {
		this.atom = atom;
		this.items = items;
	}

	static Sexpr atom(String text) {
		return new Sexpr(text, null);
	}

	static Sexpr list(Object... parts) {
		List<Sexpr> out = new ArrayList<>();
		for (Object part : parts) {
			out.add(part instanceof Sexpr s ? s : atom(String.valueOf(part)));
		}
		return new Sexpr(null, out);
	}

	boolean isList() {
		return items != null;
	}

	String head() {
		return isList() && !items.isEmpty() && items.getFirst().atom != null ? items.getFirst().atom : "";
	}

	String text(int at) {
		return isList() && at < items.size() && items.get(at).atom != null ? items.get(at).atom : "";
	}

	@Override
	public String toString() {
		if (!isList()) {
			return '"' + atom.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + '"';
		}
		StringBuilder out = new StringBuilder("(");
		for (int i = 0; i < items.size(); i++) {
			out.append(i > 0 ? " " : "").append(items.get(i));
		}
		return out.append(')').toString();
	}

	static Sexpr parse(String text) {
		int[] at = {0};
		Sexpr out = read(text, at);
		return out;
	}

	private static Sexpr read(String s, int[] at) {
		while (at[0] < s.length() && Character.isWhitespace(s.charAt(at[0]))) {
			at[0]++;
		}
		if (at[0] >= s.length()) {
			throw new IllegalArgumentException("the saved diagram ends early");
		}
		char c = s.charAt(at[0]);
		if (c == '(') {
			at[0]++;
			List<Sexpr> items = new ArrayList<>();
			while (true) {
				while (at[0] < s.length() && Character.isWhitespace(s.charAt(at[0]))) {
					at[0]++;
				}
				if (at[0] >= s.length()) {
					throw new IllegalArgumentException("the saved diagram ends early");
				}
				if (s.charAt(at[0]) == ')') {
					at[0]++;
					return new Sexpr(null, items);
				}
				items.add(read(s, at));
			}
		}
		if (c != '"') {
			throw new IllegalArgumentException("the saved diagram is damaged at " + at[0]);
		}
		at[0]++;
		StringBuilder out = new StringBuilder();
		while (at[0] < s.length() && s.charAt(at[0]) != '"') {
			char ch = s.charAt(at[0]++);
			if (ch == '\\' && at[0] < s.length()) {
				char e = s.charAt(at[0]++);
				out.append(e == 'n' ? '\n' : e);
			} else {
				out.append(ch);
			}
		}
		at[0]++;
		return atom(out.toString());
	}
}
