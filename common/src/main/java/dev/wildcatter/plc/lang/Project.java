package dev.wildcatter.plc.lang;

import java.util.List;

/** A project: program units in any of the languages, each its own source (docs/multiblocks/plc.md). */
public final class Project {
	public enum Language {
		ST, LD, FBD, SFC
	}

	public record Source(String name, Language language, String text) {
	}

	private Project() {
	}

	/** A project as one string, to go over a network in pieces. */
	public static String pack(List<Source> sources) {
		StringBuilder out = new StringBuilder();
		for (Source source : sources) {
			out.append(source.name()).append('\n').append(source.language().name()).append('\n').append(source.text().length()).append('\n').append(source.text());
		}
		return out.toString();
	}

	/** The reverse of {@link #pack}; null if the string is not one. */
	public static List<Source> unpack(String packed) {
		List<Source> out = new java.util.ArrayList<>();
		try {
			int at = 0;
			while (at < packed.length()) {
				int a = packed.indexOf('\n', at);
				int b = packed.indexOf('\n', a + 1);
				int c = packed.indexOf('\n', b + 1);
				int length = Integer.parseInt(packed.substring(b + 1, c));
				out.add(new Source(packed.substring(at, a), Language.valueOf(packed.substring(a + 1, b)), packed.substring(c + 1, c + 1 + length)));
				at = c + 1 + length;
			}
		} catch (RuntimeException e) {
			return null;
		}
		return out;
	}

	/** A project of one structured text source, as 13b's were. */
	public static List<Source> of(String text) {
		return List.of(new Source("MAIN", Language.ST, text));
	}
}
