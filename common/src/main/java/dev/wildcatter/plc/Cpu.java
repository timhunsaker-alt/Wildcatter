package dev.wildcatter.plc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A CPU module, in plain Java with no Minecraft imports, as the circuit solver is. Each {@link #scan} does what a real one does: reads the
 * inputs into the image, runs the tasks that are due against that image, writes the outputs. A program never sees an input change mid-scan.
 *
 * <p>Player programs never become JVM code: they run in this interpreter under an instruction budget, the CPU's watchdog, so one cannot
 * hold a server up.
 */
public final class Cpu {
	/** One game tick. A real CPU's scan is 1-10 ms; the game cannot go below this (plc.md, Known gaps). */
	public static final int SCAN_MS = 50;
	private static final int STACK = 1024;
	private static final int STRING_STACK = 64;
	private static final int CALLS = 128;
	private static final int DIAGNOSTICS = 64;

	public enum Mode {
		STOP, RUN
	}

	public enum Restart {
		/** RETAIN variables survive, everything else goes to its initial value. */
		WARM,
		/** Everything goes to its initial value. */
		COLD
	}

	private static final class Fault extends RuntimeException {
		Fault(String message) {
			super(message, null, false, false);
		}
	}

	private final CpuSpec spec;
	private PlcProgram program;
	private long[] mem;
	private String[] str;
	private final byte[] inputField;
	private final byte[] inputImage;
	private final byte[] outputImage;
	private final byte[] outputField;
	private final byte[] markers;
	private final byte[] forceInMask;
	private final byte[] forceInValue;
	private final byte[] forceOutMask;
	private final byte[] forceOutValue;
	private final Map<String, String> forced = new HashMap<>();
	private final Deque<Diagnostic> diagnostics = new ArrayDeque<>();
	private Mode mode = Mode.STOP;
	private boolean faulted;
	private boolean firstScan;
	private long timeMs;
	private long scans;
	private int lastInstructions;
	private int pc;

	private final long[] st = new long[STACK];
	private final String[] sst = new String[STRING_STACK];
	private final int[] returnPc = new int[CALLS];
	private final int[] returnBase = new int[CALLS];

	public Cpu(CpuSpec spec, PlcProgram program) {
		this.spec = spec;
		inputField = new byte[spec.inputBytes()];
		inputImage = new byte[spec.inputBytes()];
		outputImage = new byte[spec.outputBytes()];
		outputField = new byte[spec.outputBytes()];
		markers = new byte[spec.markerBytes()];
		forceInMask = new byte[spec.inputBytes()];
		forceInValue = new byte[spec.inputBytes()];
		forceOutMask = new byte[spec.outputBytes()];
		forceOutValue = new byte[spec.outputBytes()];
		download(program);
	}

	/** A new project replaces the old one, in STOP, with every variable at its initial value. */
	public void download(PlcProgram next) {
		program = next;
		mem = next.initMem().clone();
		str = next.initStr().clone();
		Arrays.fill(markers, (byte) 0);
		mode = Mode.STOP;
		faulted = false;
		dropOutputs();
		log("project downloaded", null, 0);
	}

	/**
	 * An online change: the new project takes over without a stop, each variable of the same name and type keeping its value, as an engineering
	 * station's does. New variables start at their initial values. Outputs hold through it, as no scan is missed.
	 */
	public void onlineChange(PlcProgram next) {
		long[] newMem = next.initMem().clone();
		String[] newStr = next.initStr().clone();
		for (Map.Entry<String, Tag> entry : next.tags().entrySet()) {
			Tag was = program.tags().get(entry.getKey());
			Tag now = entry.getValue();
			if (was != null && was.type() == now.type() && was.area() == 0 && now.area() == 0) {
				newMem[now.slot()] = mem[was.slot()];
				newStr[now.slot()] = str[was.slot()];
			}
		}
		program = next;
		mem = newMem;
		str = newStr;
		log("online change", null, 0);
	}

	public CpuSpec spec() {
		return spec;
	}

	public PlcProgram program() {
		return program;
	}

	public Mode mode() {
		return mode;
	}

	public boolean faulted() {
		return faulted;
	}

	public long timeMs() {
		return timeMs;
	}

	/** Instructions the last scan used, for the watchdog's margin. */
	public int lastInstructions() {
		return lastInstructions;
	}

	public List<Diagnostic> diagnostics() {
		return new ArrayList<>(diagnostics);
	}

	/** The FORCE light. */
	public boolean forcing() {
		return !forced.isEmpty();
	}

	public Map<String, String> forces() {
		return Map.copyOf(forced);
	}

	private void log(String event, String pou, int line) {
		if (diagnostics.size() == DIAGNOSTICS) {
			diagnostics.removeFirst();
		}
		diagnostics.addLast(new Diagnostic(timeMs, event, pou, line));
	}

	/** The keyswitch. Going to RUN clears a fault and makes a warm restart. Going to STOP drops the outputs. */
	public void setMode(Mode next) {
		if (next == mode && !(next == Mode.RUN && faulted)) {
			return;
		}
		if (next == Mode.RUN) {
			start(Restart.WARM);
		} else {
			mode = Mode.STOP;
			dropOutputs();
			log("STOP", null, 0);
		}
	}

	public void start(Restart restart) {
		for (int i = 0; i < mem.length; i++) {
			if (restart == Restart.COLD || !program.retain()[i]) {
				mem[i] = program.initMem()[i];
				str[i] = program.initStr()[i];
			}
		}
		Arrays.fill(markers, (byte) 0);
		mode = Mode.RUN;
		faulted = false;
		firstScan = true;
		log(restart == Restart.COLD ? "RUN, cold restart" : "RUN, warm restart", null, 0);
	}

	/** Supply lost: outputs drop out at once. When it comes back the CPU makes a warm restart, if its key was in RUN. */
	public void powerLost() {
		dropOutputs();
		log("power lost", null, 0);
	}

	public void powerRestored() {
		log("power restored", null, 0);
		if (mode == Mode.RUN && !faulted) {
			start(Restart.WARM);
		}
	}

	private void dropOutputs() {
		Arrays.fill(outputImage, (byte) 0);
		Arrays.fill(outputField, (byte) 0);
	}

	public void scan() {
		timeMs += SCAN_MS;
		if (mode != Mode.RUN || faulted) {
			lastInstructions = 0;
			return;
		}
		for (int i = 0; i < inputImage.length; i++) {
			inputImage[i] = (byte) (inputField[i] & ~forceInMask[i] | forceInValue[i] & forceInMask[i]);
		}
		int budget = spec.instructionsPerScan();
		try {
			for (PlcProgram.Task task : program.tasks()) {
				if (task.intervalMs() == 0 || scans * SCAN_MS % task.intervalMs() == 0) {
					for (PlcProgram.Run run : task.runs()) {
						budget = run(program.entries()[run.pou()], run.slot(), budget);
					}
				}
			}
		} catch (Fault e) {
			fault(e.getMessage());
			return;
		} catch (ArrayIndexOutOfBoundsException e) {
			fault("stack overflow");
			return;
		}
		lastInstructions = spec.instructionsPerScan() - budget;
		firstScan = false;
		scans++;
		for (int i = 0; i < outputField.length; i++) {
			outputField[i] = (byte) (outputImage[i] & ~forceOutMask[i] | forceOutValue[i] & forceOutMask[i]);
		}
	}

	private void fault(String message) {
		faulted = true;
		dropOutputs();
		int entry = -1;
		for (int i = 0; i < program.entries().length; i++) {
			if (program.entries()[i] <= pc && (entry < 0 || program.entries()[i] > program.entries()[entry])) {
				entry = i;
			}
		}
		log("CPU FAULT: " + message, entry < 0 ? null : program.pouNames()[entry], pc >= 0 && pc < program.lines().length ? program.lines()[pc] : 0);
	}

	// ---- the field side: what input modules write and output modules read ----------------------------------------------------------

	public void setInputBit(int byteAddress, int bit, boolean on) {
		if (on) {
			inputField[byteAddress] |= (byte) (1 << bit);
		} else {
			inputField[byteAddress] &= (byte) ~(1 << bit);
		}
	}

	/** A 16-bit input channel, as an analog module writes it. */
	public void setInputWord(int byteAddress, int value) {
		inputField[byteAddress] = (byte) value;
		inputField[byteAddress + 1] = (byte) (value >> 8);
	}

	public boolean outputBit(int byteAddress, int bit) {
		return (outputField[byteAddress] >> bit & 1) != 0;
	}

	public int outputWord(int byteAddress) {
		return (short) (outputField[byteAddress] & 0xFF | outputField[byteAddress + 1] << 8);
	}

	// ---- forcing --------------------------------------------------------------------------------------------------------------------

	/** Forces a direct input or output, such as {@code %IX0.3} := "TRUE" or {@code %IW4} := "16000". The CPU ignores the field for it until released. */
	public void force(String address, String value) {
		IoAddress io = IoAddress.parse(address);
		if (io == null || io.area() == 'M') {
			throw new IllegalArgumentException(address + " cannot be forced: only inputs and outputs can");
		}
		boolean input = io.area() == 'I';
		byte[] mask = input ? forceInMask : forceOutMask;
		byte[] val = input ? forceInValue : forceOutValue;
		if (io.size() == 'X') {
			boolean on = value.equalsIgnoreCase("TRUE") || value.equals("1");
			mask[io.byteAddress()] |= (byte) (1 << io.bit());
			val[io.byteAddress()] = (byte) (on ? val[io.byteAddress()] | 1 << io.bit() : val[io.byteAddress()] & ~(1 << io.bit()));
		} else {
			long raw = Long.parseLong(value);
			for (int i = 0; i < io.bytes(); i++) {
				mask[io.byteAddress() + i] = (byte) 0xFF;
				val[io.byteAddress() + i] = (byte) (raw >> 8 * i);
			}
		}
		forced.put(address.toUpperCase(Locale.ROOT), value);
		log("forced " + address + " to " + value, null, 0);
	}

	public void release(String address) {
		IoAddress io = IoAddress.parse(address);
		if (io == null || forced.remove(address.toUpperCase(Locale.ROOT)) == null) {
			return;
		}
		byte[] mask = io.area() == 'I' ? forceInMask : forceOutMask;
		if (io.size() == 'X') {
			mask[io.byteAddress()] &= (byte) ~(1 << io.bit());
		} else {
			for (int i = 0; i < io.bytes(); i++) {
				mask[io.byteAddress() + i] = 0;
			}
		}
		log("released " + address, null, 0);
	}

	// ---- tags: online monitoring ----------------------------------------------------------------------------------------------------

	/** A variable's value by name, such as {@code MAIN.T1.ET}: a Boolean, Long, Double or String; null if there is no such tag. */
	public Object read(String path) {
		IoAddress direct = path.startsWith("%") ? IoAddress.parse(path) : null;
		if (direct != null) {
			int limit = direct.area() == 'I' ? spec.inputBytes() : direct.area() == 'Q' ? spec.outputBytes() : spec.markerBytes();
			if (direct.byteAddress() + direct.bytes() > limit) {
				return null;
			}
			long raw = readIo(areaOf(direct.area()), direct.size(), direct.byteAddress(), direct.bit());
			return direct.size() == 'X' ? (Object) (raw != 0) : (Object) raw;
		}
		Tag tag = program.tags().get(path.toUpperCase(Locale.ROOT));
		if (tag == null) {
			return null;
		}
		if (tag.type() == Elementary.STRING) {
			return str[tag.slot()];
		}
		long raw;
		if (tag.area() != 0) {
			raw = readIo(areaOf(tag.area()), tag.bit() >= 0 ? 'X' : sizeFor(tag.type()), tag.byteAddress(), tag.bit());
			if (tag.type() == Elementary.REAL) {
				raw = Double.doubleToRawLongBits(Float.intBitsToFloat((int) raw));
			} else if (tag.type() != Elementary.LREAL) {
				raw = wrap(raw, tag.type());
			}
		} else {
			raw = mem[tag.slot()];
		}
		return switch (tag.type()) {
			case BOOL -> raw != 0;
			case REAL, LREAL -> Double.longBitsToDouble(raw);
			default -> raw;
		};
	}

	/** Writes a variable online, as an engineering station does. Located tags cannot be written here: force them. */
	public boolean write(String path, Object value) {
		Tag tag = program.tags().get(path.toUpperCase(Locale.ROOT));
		if (tag == null || tag.area() != 0) {
			return false;
		}
		switch (value) {
			case Boolean b -> mem[tag.slot()] = b ? 1 : 0;
			case Number n when tag.type().isReal() -> mem[tag.slot()] = Double.doubleToRawLongBits(n.doubleValue());
			case Number n -> mem[tag.slot()] = n.longValue();
			case String s when tag.type() == Elementary.STRING -> str[tag.slot()] = s;
			default -> {
				return false;
			}
		}
		return true;
	}

	private static int areaOf(char area) {
		return area == 'I' ? Op.AREA_I : area == 'Q' ? Op.AREA_Q : Op.AREA_M;
	}

	private static char sizeFor(Elementary type) {
		return switch (type.bits) {
			case 8 -> 'B';
			case 16 -> 'W';
			case 32 -> 'D';
			case 64 -> 'L';
			default -> 'X';
		};
	}

	// ---- saving with the world ------------------------------------------------------------------------------------------------------

	/** What a world save has to keep for a CPU, beside its project's source: all memory, as a save is no power cycle. */
	public record State(long[] mem, String[] str, byte[] markers, byte[] outputImage, byte[] inputField, Mode mode, boolean faulted, long timeMs, long scans, Map<String, String> forced) {
	}

	public State save() {
		return new State(mem.clone(), str.clone(), markers.clone(), outputImage.clone(), inputField.clone(), mode, faulted, timeMs, scans, Map.copyOf(forced));
	}

	/** Restores a save onto the same compiled project; a size mismatch means the project changed, and the save is refused. */
	public boolean restore(State state) {
		if (state.mem().length != mem.length) {
			return false;
		}
		System.arraycopy(state.mem(), 0, mem, 0, mem.length);
		System.arraycopy(state.str(), 0, str, 0, str.length);
		System.arraycopy(state.markers(), 0, markers, 0, markers.length);
		System.arraycopy(state.outputImage(), 0, outputImage, 0, outputImage.length);
		System.arraycopy(state.inputField(), 0, inputField, 0, inputField.length);
		mode = state.mode();
		faulted = state.faulted();
		timeMs = state.timeMs();
		scans = state.scans();
		state.forced().forEach(this::force);
		return true;
	}

	// ---- the interpreter ------------------------------------------------------------------------------------------------------------

	private long readIo(int area, char size, int at, int bit) {
		byte[] from = area == Op.AREA_I ? inputImage : area == Op.AREA_Q ? outputImage : markers;
		return switch (size) {
			case 'X' -> from[at] >> bit & 1;
			case 'B' -> from[at] & 0xFFL;
			case 'W' -> from[at] & 0xFFL | (from[at + 1] & 0xFFL) << 8;
			case 'D' -> from[at] & 0xFFL | (from[at + 1] & 0xFFL) << 8 | (from[at + 2] & 0xFFL) << 16 | (from[at + 3] & 0xFFL) << 24;
			default -> {
				long out = 0;
				for (int i = 0; i < 8; i++) {
					out |= (from[at + i] & 0xFFL) << 8 * i;
				}
				yield out;
			}
		};
	}

	private void writeIo(int area, char size, int at, int bit, long value) {
		byte[] to = area == Op.AREA_Q ? outputImage : markers;
		if (size == 'X') {
			to[at] = (byte) (value != 0 ? to[at] | 1 << bit : to[at] & ~(1 << bit));
			return;
		}
		int bytes = size == 'B' ? 1 : size == 'W' ? 2 : size == 'D' ? 4 : 8;
		for (int i = 0; i < bytes; i++) {
			to[at + i] = (byte) (value >> 8 * i);
		}
	}

	static long wrap(long v, Elementary type) {
		return switch (type) {
			case SINT -> (byte) v;
			case USINT, BYTE -> v & 0xFFL;
			case INT -> (short) v;
			case UINT, WORD -> v & 0xFFFFL;
			case DINT -> (int) v;
			case UDINT, DWORD -> v & 0xFFFFFFFFL;
			case BOOL -> v != 0 ? 1 : 0;
			default -> v;
		};
	}

	private static final Elementary[] TYPES = Elementary.values();

	private static double f(long raw) {
		return Double.longBitsToDouble(raw);
	}

	private static long b(double value) {
		return Double.doubleToRawLongBits(value);
	}

	private static long mask(int width) {
		return width >= 64 ? -1L : (1L << width) - 1;
	}

	private int run(int entry, int instance, int budgetIn) {
		final int[] c = program.code();
		final long[] m = mem;
		final long[] s = st;
		final long[] k = program.constants();
		int budget = budgetIn;
		int p = entry;
		int sp = 0;
		int ss = 0;
		int base = instance;
		int depth = 0;
		while (true) {
			pc = p;
			if (--budget < 0) {
				throw new Fault("watchdog: the scan took more than " + spec.instructionsPerScan() + " instructions");
			}
			switch (c[p++]) {
				case Op.PUSH -> s[sp++] = k[c[p++]];
				case Op.PUSHI -> s[sp++] = c[p++];
				case Op.SPUSH -> sst[ss++] = program.strings()[c[p++]];
				case Op.LOADA -> s[sp++] = m[c[p++]];
				case Op.STOREA -> m[c[p++]] = s[--sp];
				case Op.LOADB -> s[sp++] = m[base + c[p++]];
				case Op.STOREB -> m[base + c[p++]] = s[--sp];
				case Op.ADDRB -> s[sp++] = base + c[p++];
				case Op.LOADI -> s[sp - 1] = m[(int) s[sp - 1]];
				case Op.STOREI -> {
					sp -= 2;
					m[(int) s[sp]] = s[sp + 1];
				}
				case Op.SLOAD -> {
					String v = str[(int) s[--sp]];
					sst[ss++] = v == null ? "" : v;
				}
				case Op.SSTORE -> str[(int) s[--sp]] = sst[--ss];
				case Op.COPY -> {
					int n = c[p++];
					sp -= 2;
					int to = (int) s[sp];
					int from = (int) s[sp + 1];
					System.arraycopy(m, from, m, to, n);
					System.arraycopy(str, from, str, to, n);
					budget -= n;
				}
				case Op.COPYINIT, Op.COPYINITB -> {
					boolean relative = c[p - 1] == Op.COPYINITB;
					int at = c[p++] + (relative ? base : 0);
					int n = c[p++];
					System.arraycopy(program.initMem(), at, m, at, n);
					System.arraycopy(program.initStr(), at, str, at, n);
					budget -= n;
				}
				case Op.DUP -> {
					s[sp] = s[sp - 1];
					sp++;
				}
				case Op.POP -> sp--;
				case Op.SPOP -> ss--;
				case Op.SWAP -> {
					long t = s[sp - 1];
					s[sp - 1] = s[sp - 2];
					s[sp - 2] = t;
				}
				case Op.IADD -> {
					sp--;
					s[sp - 1] += s[sp];
				}
				case Op.ISUB -> {
					sp--;
					s[sp - 1] -= s[sp];
				}
				case Op.IMUL -> {
					sp--;
					s[sp - 1] *= s[sp];
				}
				case Op.IDIV -> {
					sp--;
					if (s[sp] == 0) {
						throw new Fault("division by zero");
					}
					s[sp - 1] /= s[sp];
				}
				case Op.IMOD -> {
					sp--;
					s[sp - 1] = s[sp] == 0 ? 0 : s[sp - 1] % s[sp];
				}
				case Op.INEG -> s[sp - 1] = -s[sp - 1];
				case Op.IABS -> s[sp - 1] = Math.abs(s[sp - 1]);
				case Op.IMIN -> {
					sp--;
					s[sp - 1] = Math.min(s[sp - 1], s[sp]);
				}
				case Op.IMAX -> {
					sp--;
					s[sp - 1] = Math.max(s[sp - 1], s[sp]);
				}
				case Op.FADD -> {
					sp--;
					s[sp - 1] = b(f(s[sp - 1]) + f(s[sp]));
				}
				case Op.FSUB -> {
					sp--;
					s[sp - 1] = b(f(s[sp - 1]) - f(s[sp]));
				}
				case Op.FMUL -> {
					sp--;
					s[sp - 1] = b(f(s[sp - 1]) * f(s[sp]));
				}
				case Op.FDIV -> {
					sp--;
					if (f(s[sp]) == 0) {
						throw new Fault("division by zero");
					}
					s[sp - 1] = b(f(s[sp - 1]) / f(s[sp]));
				}
				case Op.FNEG -> s[sp - 1] = b(-f(s[sp - 1]));
				case Op.FEXPT -> {
					sp--;
					s[sp - 1] = b(Math.pow(f(s[sp - 1]), f(s[sp])));
				}
				case Op.FMIN -> {
					sp--;
					s[sp - 1] = b(Math.min(f(s[sp - 1]), f(s[sp])));
				}
				case Op.FMAX -> {
					sp--;
					s[sp - 1] = b(Math.max(f(s[sp - 1]), f(s[sp])));
				}
				case Op.FMATH -> {
					double v = f(s[sp - 1]);
					s[sp - 1] = b(switch (c[p++]) {
						case Op.MATH_ABS -> Math.abs(v);
						case Op.MATH_SQRT -> Math.sqrt(v);
						case Op.MATH_LN -> Math.log(v);
						case Op.MATH_LOG -> Math.log10(v);
						case Op.MATH_EXP -> Math.exp(v);
						case Op.MATH_SIN -> Math.sin(v);
						case Op.MATH_COS -> Math.cos(v);
						case Op.MATH_TAN -> Math.tan(v);
						case Op.MATH_ASIN -> Math.asin(v);
						case Op.MATH_ACOS -> Math.acos(v);
						default -> Math.atan(v);
					});
				}
				case Op.ILT -> {
					sp--;
					s[sp - 1] = s[sp - 1] < s[sp] ? 1 : 0;
				}
				case Op.ILE -> {
					sp--;
					s[sp - 1] = s[sp - 1] <= s[sp] ? 1 : 0;
				}
				case Op.IGT -> {
					sp--;
					s[sp - 1] = s[sp - 1] > s[sp] ? 1 : 0;
				}
				case Op.IGE -> {
					sp--;
					s[sp - 1] = s[sp - 1] >= s[sp] ? 1 : 0;
				}
				case Op.IEQ -> {
					sp--;
					s[sp - 1] = s[sp - 1] == s[sp] ? 1 : 0;
				}
				case Op.INE -> {
					sp--;
					s[sp - 1] = s[sp - 1] != s[sp] ? 1 : 0;
				}
				case Op.FLT -> {
					sp--;
					s[sp - 1] = f(s[sp - 1]) < f(s[sp]) ? 1 : 0;
				}
				case Op.FLE -> {
					sp--;
					s[sp - 1] = f(s[sp - 1]) <= f(s[sp]) ? 1 : 0;
				}
				case Op.FGT -> {
					sp--;
					s[sp - 1] = f(s[sp - 1]) > f(s[sp]) ? 1 : 0;
				}
				case Op.FGE -> {
					sp--;
					s[sp - 1] = f(s[sp - 1]) >= f(s[sp]) ? 1 : 0;
				}
				case Op.FEQ -> {
					sp--;
					s[sp - 1] = f(s[sp - 1]) == f(s[sp]) ? 1 : 0;
				}
				case Op.FNE -> {
					sp--;
					s[sp - 1] = f(s[sp - 1]) != f(s[sp]) ? 1 : 0;
				}
				case Op.SCMP -> {
					ss -= 2;
					s[sp++] = Integer.signum(sst[ss].compareTo(sst[ss + 1]));
				}
				case Op.AND -> {
					sp--;
					s[sp - 1] &= s[sp];
				}
				case Op.OR -> {
					sp--;
					s[sp - 1] |= s[sp];
				}
				case Op.XOR -> {
					sp--;
					s[sp - 1] ^= s[sp];
				}
				case Op.LNOT -> s[sp - 1] = s[sp - 1] == 0 ? 1 : 0;
				case Op.BNOT -> s[sp - 1] = ~s[sp - 1];
				case Op.WRAP -> s[sp - 1] = wrap(s[sp - 1], TYPES[c[p++]]);
				case Op.FSINGLE -> s[sp - 1] = b((float) f(s[sp - 1]));
				case Op.I2F -> s[sp - 1] = b((double) s[sp - 1]);
				case Op.F2I -> s[sp - 1] = (long) Math.rint(f(s[sp - 1]));
				case Op.F2TRUNC -> s[sp - 1] = (long) f(s[sp - 1]);
				case Op.SHL -> {
					int w = c[p++];
					sp--;
					s[sp - 1] = s[sp] >= w ? 0 : s[sp - 1] << s[sp] & mask(w);
				}
				case Op.SHR -> {
					int w = c[p++];
					sp--;
					s[sp - 1] = s[sp] >= w ? 0 : (s[sp - 1] & mask(w)) >>> s[sp];
				}
				case Op.ROL, Op.ROR -> {
					boolean left = c[p - 1] == Op.ROL;
					int w = c[p++];
					sp--;
					int n = (int) Math.floorMod(s[sp], (long) w);
					int l = left ? n : (w - n) % w;
					long v = s[sp - 1] & mask(w);
					s[sp - 1] = l == 0 ? v : (v << l | v >>> w - l) & mask(w);
				}
				case Op.SELECT -> {
					sp -= 2;
					s[sp - 1] = s[sp - 1] != 0 ? s[sp + 1] : s[sp];
				}
				case Op.MUX -> {
					int n = c[p++];
					sp -= n;
					long which = s[sp - 1];
					if (which < 0 || which >= n) {
						throw new Fault("MUX has no input " + which);
					}
					s[sp - 1] = s[sp + (int) which];
				}
				case Op.JMP -> p = c[p];
				case Op.JZ -> p = s[--sp] == 0 ? c[p] : p + 1;
				case Op.JNZ -> p = s[--sp] != 0 ? c[p] : p + 1;
				case Op.CALL -> {
					int target = c[p++];
					returnPc[depth] = p;
					returnBase[depth++] = base;
					p = program.entries()[target];
				}
				case Op.CALLFB -> {
					int target = c[p++];
					returnPc[depth] = p;
					returnBase[depth++] = base;
					base = (int) s[--sp];
					p = program.entries()[target];
				}
				case Op.RET -> {
					if (depth == 0) {
						return budget;
					}
					p = returnPc[--depth];
					base = returnBase[depth];
				}
				case Op.BOUNDS -> {
					int low = c[p++];
					int high = c[p++];
					long i = s[sp - 1];
					if (i < low || i > high) {
						throw new Fault("array index " + i + " is outside " + low + ".." + high);
					}
					s[sp - 1] = i - low;
				}
				case Op.LOADIO -> {
					s[sp++] = readIo(c[p], (char) c[p + 1], c[p + 2], c[p + 3]);
					p += 4;
				}
				case Op.STOREIO -> {
					writeIo(c[p], (char) c[p + 1], c[p + 2], c[p + 3], s[--sp]);
					p += 4;
				}
				case Op.BITS2F -> s[sp - 1] = b(Float.intBitsToFloat((int) s[sp - 1]));
				case Op.F2BITS -> s[sp - 1] = Float.floatToRawIntBits((float) f(s[sp - 1]));
				case Op.GETBIT -> s[sp - 1] = s[sp - 1] >>> c[p++] & 1;
				case Op.SETBIT -> {
					int bit = c[p++];
					sp -= 2;
					int at = (int) s[sp];
					m[at] = s[sp + 1] != 0 ? m[at] | 1L << bit : m[at] & ~(1L << bit);
				}
				case Op.NOW -> s[sp++] = timeMs;
				case Op.FIRST -> s[sp++] = firstScan ? 1 : 0;
				case Op.STROP -> {
					switch (c[p++]) {
						case Op.STR_LEN -> s[sp++] = sst[--ss].length();
						case Op.STR_LEFT -> {
							String v = sst[ss - 1];
							sst[ss - 1] = v.substring(0, clamp(s[--sp], v.length()));
						}
						case Op.STR_RIGHT -> {
							String v = sst[ss - 1];
							sst[ss - 1] = v.substring(v.length() - clamp(s[--sp], v.length()));
						}
						case Op.STR_MID -> {
							sp -= 2;
							String v = sst[ss - 1];
							int from = clamp(s[sp + 1] - 1, v.length());
							sst[ss - 1] = v.substring(from, from + clamp(s[sp], v.length() - from));
						}
						case Op.STR_CONCAT -> {
							ss--;
							sst[ss - 1] = sst[ss - 1] + sst[ss];
							budget -= sst[ss - 1].length();
						}
						case Op.STR_FIND -> {
							ss -= 2;
							s[sp++] = sst[ss].indexOf(sst[ss + 1]) + 1;
						}
						case Op.STR_INSERT -> {
							ss--;
							String v = sst[ss - 1];
							int at = clamp(s[--sp], v.length());
							sst[ss - 1] = v.substring(0, at) + sst[ss] + v.substring(at);
						}
						case Op.STR_DELETE -> {
							sp -= 2;
							String v = sst[ss - 1];
							int from = clamp(s[sp + 1] - 1, v.length());
							sst[ss - 1] = v.substring(0, from) + v.substring(from + clamp(s[sp], v.length() - from));
						}
						default -> {
							sp -= 2;
							ss--;
							String v = sst[ss - 1];
							int from = clamp(s[sp + 1] - 1, v.length());
							sst[ss - 1] = v.substring(0, from) + sst[ss] + v.substring(from + clamp(s[sp], v.length() - from));
						}
					}
				}
				case Op.ITOS -> sst[ss++] = Long.toString(s[--sp]);
				case Op.FTOS -> sst[ss++] = Double.toString(f(s[--sp]));
				case Op.STOI -> {
					try {
						s[sp++] = Long.parseLong(sst[--ss].trim());
					} catch (NumberFormatException e) {
						s[sp - 1] = 0;
					}
				}
				case Op.STOF -> {
					try {
						s[sp++] = b(Double.parseDouble(sst[--ss].trim()));
					} catch (NumberFormatException e) {
						s[sp - 1] = b(0);
					}
				}
				default -> throw new Fault("bad instruction " + c[p - 1]);
			}
		}
	}

	private static int clamp(long v, int max) {
		return (int) Math.max(0, Math.min(v, max));
	}
}
