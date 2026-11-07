package dev.wildcatter.plc.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** IEC 61131-3 structured text tokens. Identifiers and keywords are case-insensitive, so both are upper-cased here. */
final class Lexer {
	enum Kind {
		IDENT, INT, REAL, STRING, TIME, TYPED, DIRECT, SYMBOL, END
	}

	record Token(Kind kind, String text, long whole, double real, int line, int column) {
		boolean is(String symbolOrWord) {
			return (kind == Kind.SYMBOL || kind == Kind.IDENT) && text.equals(symbolOrWord);
		}
	}

	private final String src;
	private int at;
	private int line = 1;
	private int lineStart;

	private Lexer(String src) {
		this.src = src;
	}

	static List<Token> tokens(String src) {
		Lexer lexer = new Lexer(src);
		List<Token> out = new ArrayList<>();
		Token token;
		do {
			token = lexer.next();
			out.add(token);
		} while (token.kind != Kind.END);
		return out;
	}

	private char peek(int ahead) {
		return at + ahead < src.length() ? src.charAt(at + ahead) : '\0';
	}

	private int column() {
		return at - lineStart + 1;
	}

	private CompileException error(String message) {
		return new CompileException(line, column(), message);
	}

	private void skipSpace() {
		while (at < src.length()) {
			char c = src.charAt(at);
			if (c == '\n') {
				line++;
				at++;
				lineStart = at;
			} else if (Character.isWhitespace(c)) {
				at++;
			} else if (c == '/' && peek(1) == '/') {
				while (at < src.length() && src.charAt(at) != '\n') {
					at++;
				}
			} else if (c == '(' && peek(1) == '*') {
				comment();
			} else {
				return;
			}
		}
	}

	/** Block comments, which may not nest in the standard; an unclosed one is an error. */
	private void comment() {
		int fromLine = line;
		at += 2;
		while (!(peek(0) == '*' && peek(1) == ')')) {
			if (at >= src.length()) {
				throw new CompileException(fromLine, 1, "this comment is never closed");
			}
			if (src.charAt(at) == '\n') {
				line++;
				lineStart = at + 1;
			}
			at++;
		}
		at += 2;
	}

	private Token token(Kind kind, String text, int column) {
		return new Token(kind, text, 0, 0, line, column);
	}

	private Token next() {
		skipSpace();
		int column = column();
		if (at >= src.length()) {
			return token(Kind.END, "end of the source", column);
		}
		char c = src.charAt(at);
		if (Character.isDigit(c)) {
			return number(column);
		}
		if (Character.isLetter(c) || c == '_') {
			return word(column);
		}
		if (c == '\'') {
			return string(column);
		}
		if (c == '%') {
			return direct(column);
		}
		for (String symbol : new String[] {":=", "=>", "<>", "<=", ">=", "**", ".."}) {
			if (src.startsWith(symbol, at)) {
				at += symbol.length();
				return token(Kind.SYMBOL, symbol, column);
			}
		}
		if ("()[],;:.+-*/=<>&".indexOf(c) >= 0) {
			at++;
			return token(Kind.SYMBOL, String.valueOf(c), column);
		}
		throw error("'" + c + "' does not belong in structured text");
	}

	private Token word(int column) {
		int from = at;
		while (Character.isLetterOrDigit(peek(0)) || peek(0) == '_') {
			at++;
		}
		String text = src.substring(from, at).toUpperCase(Locale.ROOT);
		if (peek(0) != '#') {
			return token(Kind.IDENT, text, column);
		}
		at++;
		if (text.equals("T") || text.equals("TIME") || text.equals("LT") || text.equals("LTIME")) {
			return time(column);
		}
		return token(Kind.TYPED, text, column);
	}

	private Token time(int column) {
		int from = at;
		while (Character.isLetterOrDigit(peek(0)) || peek(0) == '_' || peek(0) == '.' || peek(0) == '-') {
			at++;
		}
		String text = src.substring(from, at).toLowerCase(Locale.ROOT).replace("_", "");
		long sign = 1;
		if (text.startsWith("-")) {
			sign = -1;
			text = text.substring(1);
		}
		if (text.isEmpty()) {
			throw error("a time needs a value, such as T#5s");
		}
		double ms = 0;
		int i = 0;
		while (i < text.length()) {
			int start = i;
			while (i < text.length() && (Character.isDigit(text.charAt(i)) || text.charAt(i) == '.')) {
				i++;
			}
			int unitStart = i;
			while (i < text.length() && Character.isLetter(text.charAt(i))) {
				i++;
			}
			if (start == unitStart || unitStart == i) {
				throw error("'" + text + "' is not a time: write it as T#1h2m3s4ms");
			}
			double value = Double.parseDouble(text.substring(start, unitStart));
			ms += value * switch (text.substring(unitStart, i)) {
				case "d" -> 86_400_000.0;
				case "h" -> 3_600_000.0;
				case "m" -> 60_000.0;
				case "s" -> 1000.0;
				case "ms" -> 1.0;
				default -> throw error("'" + text.substring(unitStart, i) + "' is not a time unit: use d, h, m, s or ms");
			};
		}
		return new Token(Kind.TIME, text, sign * Math.round(ms), 0, line, column);
	}

	private Token string(int column) {
		StringBuilder out = new StringBuilder();
		at++;
		while (peek(0) != '\'') {
			if (at >= src.length() || peek(0) == '\n') {
				throw new CompileException(line, column, "this string is never closed");
			}
			char c = src.charAt(at++);
			if (c == '$') {
				char e = peek(0);
				at++;
				out.append(switch (e) {
					case 'N', 'n', 'L', 'l' -> '\n';
					case 'T', 't' -> '\t';
					case 'R', 'r' -> '\r';
					case '$' -> '$';
					case '\'' -> '\'';
					default -> throw error("'$" + e + "' is not an escape a string can hold");
				});
			} else {
				out.append(c);
			}
		}
		at++;
		return token(Kind.STRING, out.toString(), column);
	}

	/** {@code %IX0.3}, {@code %QW2}, {@code %MD8}. A size left out means a bit. */
	private Token direct(int column) {
		int from = at;
		at++;
		while (Character.isLetterOrDigit(peek(0)) || peek(0) == '.') {
			at++;
		}
		return token(Kind.DIRECT, src.substring(from, at).toUpperCase(Locale.ROOT), column);
	}

	private Token number(int column) {
		int from = at;
		while (Character.isDigit(peek(0)) || peek(0) == '_') {
			at++;
		}
		String digits = src.substring(from, at).replace("_", "");
		if (peek(0) == '#') {
			int radix = Integer.parseInt(digits);
			if (radix != 2 && radix != 8 && radix != 16) {
				throw error("a based number is 2#, 8# or 16#, not " + radix + "#");
			}
			at++;
			int start = at;
			while (Character.isLetterOrDigit(peek(0)) || peek(0) == '_') {
				at++;
			}
			try {
				return new Token(Kind.INT, src.substring(from, at), Long.parseUnsignedLong(src.substring(start, at).replace("_", ""), radix), 0, line, column);
			} catch (NumberFormatException e) {
				throw error("'" + src.substring(from, at) + "' is not a number");
			}
		}
		boolean real = false;
		if (peek(0) == '.' && Character.isDigit(peek(1))) {
			real = true;
			at++;
			while (Character.isDigit(peek(0)) || peek(0) == '_') {
				at++;
			}
		}
		if ((peek(0) == 'E' || peek(0) == 'e') && (Character.isDigit(peek(1)) || ((peek(1) == '-' || peek(1) == '+') && Character.isDigit(peek(2))))) {
			real = true;
			at += 2;
			while (Character.isDigit(peek(0))) {
				at++;
			}
		}
		String text = src.substring(from, at).replace("_", "");
		if (real) {
			return new Token(Kind.REAL, text, 0, Double.parseDouble(text), line, column);
		}
		try {
			return new Token(Kind.INT, text, Long.parseLong(text), 0, line, column);
		} catch (NumberFormatException e) {
			throw error("'" + text + "' is too large");
		}
	}
}
