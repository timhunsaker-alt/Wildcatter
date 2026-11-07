package dev.wildcatter.plc;

/** A direct address such as {@code %IX0.3}, {@code %QW2} or {@code %MD8}. Words are byte-addressed and low byte first. */
public record IoAddress(char area, char size, int byteAddress, int bit) {
	public static IoAddress parse(String text) {
		String t = text.toUpperCase(java.util.Locale.ROOT);
		if (t.length() < 3 || t.charAt(0) != '%' || "IQM".indexOf(t.charAt(1)) < 0) {
			return null;
		}
		int at = 2;
		char size = 'X';
		if ("XBWDL".indexOf(t.charAt(2)) >= 0) {
			size = t.charAt(2);
			at = 3;
		}
		String rest = t.substring(at);
		try {
			int dot = rest.indexOf('.');
			if (size == 'X') {
				if (dot < 0) {
					return null;
				}
				int bit = Integer.parseInt(rest.substring(dot + 1));
				return bit < 0 || bit > 7 ? null : new IoAddress(t.charAt(1), 'X', Integer.parseInt(rest.substring(0, dot)), bit);
			}
			return dot >= 0 ? null : new IoAddress(t.charAt(1), size, Integer.parseInt(rest), -1);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public int bytes() {
		return switch (size) {
			case 'X', 'B' -> 1;
			case 'W' -> 2;
			case 'D' -> 4;
			default -> 8;
		};
	}

	/** The widest elementary type at this size, as a bare direct address reads. */
	public Elementary type() {
		return switch (size) {
			case 'X' -> Elementary.BOOL;
			case 'B' -> Elementary.BYTE;
			case 'W' -> Elementary.WORD;
			case 'D' -> Elementary.DWORD;
			default -> Elementary.LWORD;
		};
	}
}
