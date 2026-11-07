package dev.wildcatter.plc;

/** IEC 61131-3's elementary types. The two UNTYPED kinds are literals the compiler has not yet given a type. */
public enum Elementary {
	BOOL(1, false), SINT(8, true), USINT(8, false), INT(16, true), UINT(16, false), DINT(32, true), UDINT(32, false), LINT(64, true),
	BYTE(8, false), WORD(16, false), DWORD(32, false), LWORD(64, false),
	REAL(32, true), LREAL(64, true), TIME(64, true), STRING(0, false), UNTYPED_INT(64, true), UNTYPED_REAL(64, true);

	public final int bits;
	public final boolean signed;

	Elementary(int bits, boolean signed) {
		this.bits = bits;
		this.signed = signed;
	}

	public boolean isInt() {
		return switch (this) {
			case SINT, USINT, INT, UINT, DINT, UDINT, LINT, UNTYPED_INT -> true;
			default -> false;
		};
	}

	public boolean isBits() {
		return this == BYTE || this == WORD || this == DWORD || this == LWORD;
	}

	public boolean isReal() {
		return this == REAL || this == LREAL || this == UNTYPED_REAL;
	}

	public boolean isNumeric() {
		return isInt() || isReal();
	}

	/** What the standard lets pass without a written conversion: only what loses nothing. */
	public boolean widensTo(Elementary to) {
		if (this == to) {
			return true;
		}
		return switch (this) {
			case SINT -> to == INT || to == DINT || to == LINT || to == REAL || to == LREAL;
			case USINT -> to == UINT || to == UDINT || to == INT || to == DINT || to == LINT || to == REAL || to == LREAL;
			case INT -> to == DINT || to == LINT || to == REAL || to == LREAL;
			case UINT -> to == UDINT || to == DINT || to == LINT || to == REAL || to == LREAL;
			case DINT, UDINT -> to == LINT || to == LREAL;
			case REAL -> to == LREAL;
			case BYTE -> to == WORD || to == DWORD || to == LWORD;
			case WORD -> to == DWORD || to == LWORD;
			case DWORD -> to == LWORD;
			default -> false;
		};
	}

	/** {@code value} held in this type's range, for checking a literal. */
	public boolean holds(long value) {
		if (bits >= 64) {
			return true;
		}
		return signed ? value >= -(1L << (bits - 1)) && value < (1L << (bits - 1)) : value >= 0 && value < (1L << bits);
	}
}
