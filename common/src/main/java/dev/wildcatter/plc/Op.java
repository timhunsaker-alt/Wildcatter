package dev.wildcatter.plc;

/** The CPU's instruction set: a stack machine over 8-byte memory slots. Plain ints, so the interpreter's switch is a table. */
public final class Op {
	private Op() {
	}

	public static final int PUSH = 1, PUSHI = 2, SPUSH = 3, LOADA = 4, STOREA = 5, LOADB = 6, STOREB = 7, ADDRB = 8, LOADI = 9, STOREI = 10, SLOAD = 11, SSTORE = 12,
			COPY = 13, COPYINIT = 14, COPYINITB = 15, DUP = 16, POP = 17, SWAP = 18, SPOP = 19,
			IADD = 20, ISUB = 21, IMUL = 22, IDIV = 23, IMOD = 24, INEG = 25, IABS = 26, IMIN = 27, IMAX = 28,
			FADD = 30, FSUB = 31, FMUL = 32, FDIV = 33, FNEG = 34, FEXPT = 35, FMIN = 36, FMAX = 37, FMATH = 38,
			ILT = 40, ILE = 41, IGT = 42, IGE = 43, IEQ = 44, INE = 45,
			FLT = 46, FLE = 47, FGT = 48, FGE = 49, FEQ = 50, FNE = 51, SCMP = 52,
			AND = 53, OR = 54, XOR = 55, LNOT = 56, BNOT = 57, WRAP = 58, FSINGLE = 59, I2F = 60, F2I = 61, F2TRUNC = 62,
			SHL = 63, SHR = 64, ROL = 65, ROR = 66, SELECT = 67, MUX = 68,
			JMP = 70, JZ = 71, JNZ = 72, CALL = 73, CALLFB = 74, RET = 75, BOUNDS = 76,
			LOADIO = 77, STOREIO = 78, BITS2F = 79, F2BITS = 80, GETBIT = 81, SETBIT = 82, NOW = 83, FIRST = 84, STROP = 85, ITOS = 86, FTOS = 87, STOI = 88, STOF = 89;

	public static final int MATH_ABS = 0, MATH_SQRT = 1, MATH_LN = 2, MATH_LOG = 3, MATH_EXP = 4, MATH_SIN = 5, MATH_COS = 6, MATH_TAN = 7, MATH_ASIN = 8, MATH_ACOS = 9, MATH_ATAN = 10;
	public static final int STR_LEN = 0, STR_LEFT = 1, STR_RIGHT = 2, STR_MID = 3, STR_CONCAT = 4, STR_FIND = 5, STR_INSERT = 6, STR_DELETE = 7, STR_REPLACE = 8;
	public static final int AREA_I = 0, AREA_Q = 1, AREA_M = 2;
}
