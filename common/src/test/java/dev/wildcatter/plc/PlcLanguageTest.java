package dev.wildcatter.plc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.plc.lang.CompileError;
import dev.wildcatter.plc.lang.PlcCompiler;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Real structured text, run by the real CPU, with no Minecraft. */
class PlcLanguageTest {
	private static Cpu cpu(String source) {
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.SMALL, source);
		assertTrue(result.ok(), () -> "should compile: " + result.errors());
		Cpu cpu = new Cpu(CpuSpec.SMALL, result.program());
		cpu.start(Cpu.Restart.COLD);
		return cpu;
	}

	private static void scans(Cpu cpu, int n) {
		for (int i = 0; i < n; i++) {
			cpu.scan();
		}
	}

	private static List<CompileError> errors(String source) {
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.SMALL, source);
		assertFalse(result.ok(), "should not compile");
		return result.errors();
	}

	private static void assertError(String source, String part) {
		List<CompileError> errors = errors(source);
		assertTrue(errors.stream().anyMatch(e -> e.message().contains(part)), () -> "expected an error about '" + part + "', got " + errors);
	}

	/** One PROGRAM with these variables and body, one scan. */
	private static Cpu once(String vars, String body) {
		Cpu cpu = cpu("PROGRAM MAIN VAR " + vars + " END_VAR\n" + body + "\nEND_PROGRAM");
		cpu.scan();
		assertFalse(cpu.faulted(), () -> "faulted: " + cpu.diagnostics());
		return cpu;
	}

	@Test
	void arithmeticWrapsAtItsType() {
		Cpu cpu = once("a : INT := 32767; b : USINT := 255; c : DINT; r : REAL; d : LREAL; m : INT;",
				"a := a + 1; b := b + 1; c := 7 / 2; r := 1.5 * 2.0 + 1; d := 2.0 ** 10.0; m := 17 MOD 5;");
		assertEquals(-32768L, cpu.read("MAIN.A"));
		assertEquals(0L, cpu.read("MAIN.B"));
		assertEquals(3L, cpu.read("MAIN.C"));
		assertEquals(4.0, (Double) cpu.read("MAIN.R"), 1e-6);
		assertEquals(1024.0, (Double) cpu.read("MAIN.D"), 1e-9);
		assertEquals(2L, cpu.read("MAIN.M"));
	}

	@Test
	void precedenceIsTheStandards() {
		Cpu cpu = once("a : INT; b : BOOL; c : BOOL; e : INT;", "a := 2 + 3 * 4; b := TRUE OR FALSE AND FALSE; c := 1 + 1 = 2; e := -(2 + 3) * 2;");
		assertEquals(14L, cpu.read("MAIN.A"));
		assertEquals(true, cpu.read("MAIN.B"));
		assertEquals(true, cpu.read("MAIN.C"));
		assertEquals(-10L, cpu.read("MAIN.E"));
	}

	@Test
	void literalsInEveryForm() {
		Cpu cpu = once("a : WORD; b : BYTE; c : DINT; t : TIME; r : REAL; s : INT;",
				"a := 16#FF00; b := 2#1010_0101; c := 1_000_000; t := T#1h2m3s4ms; r := 1.5E3; s := INT#-5;");
		assertEquals(0xFF00L, cpu.read("MAIN.A"));
		assertEquals(0xA5L, cpu.read("MAIN.B"));
		assertEquals(1_000_000L, cpu.read("MAIN.C"));
		assertEquals(3_723_004L, cpu.read("MAIN.T"));
		assertEquals(1500.0, (Double) cpu.read("MAIN.R"), 1e-6);
		assertEquals(-5L, cpu.read("MAIN.S"));
	}

	@Test
	void controlStatements() {
		Cpu cpu = once("i, sum, down, w, r, c1, c2, c3, skip : INT; b : BOOL;", """
				FOR i := 1 TO 10 DO sum := sum + i; END_FOR;
				FOR i := 10 TO 1 BY -3 DO down := down + i; END_FOR;
				WHILE w < 5 DO w := w + 1; IF w = 3 THEN EXIT; END_IF; END_WHILE;
				REPEAT r := r + 2; UNTIL r >= 5 END_REPEAT;
				FOR i := 1 TO 5 DO IF i = 2 THEN CONTINUE; END_IF; skip := skip + 1; END_FOR;
				CASE sum OF
					1, 2: c1 := 1;
					50..60: c1 := 2;
				ELSE
					c1 := 3;
				END_CASE;
				CASE 7 OF 1: c2 := 1; ELSE c2 := 9; END_CASE;
				IF sum < 0 THEN c3 := 1; ELSIF sum = 55 THEN c3 := 2; ELSE c3 := 3; END_IF;
				""");
		assertEquals(55L, cpu.read("MAIN.SUM"));
		assertEquals(22L, cpu.read("MAIN.DOWN"));
		assertEquals(3L, cpu.read("MAIN.W"));
		assertEquals(6L, cpu.read("MAIN.R"));
		assertEquals(4L, cpu.read("MAIN.SKIP"));
		assertEquals(2L, cpu.read("MAIN.C1"));
		assertEquals(9L, cpu.read("MAIN.C2"));
		assertEquals(2L, cpu.read("MAIN.C3"));
	}

	@Test
	void typesCompose() {
		Cpu cpu = cpu("""
				TYPE Mode : (OFF_, HAND, AUTO_) := HAND; END_TYPE
				TYPE Pump : STRUCT tag : STRING := 'P-101'; run : BOOL; rate : REAL := 2.5; mode : Mode; END_STRUCT; END_TYPE
				VAR_GLOBAL table : ARRAY[1..3] OF INT := [10, 2(20)]; END_VAR
				PROGRAM MAIN
				VAR
					p, q : Pump;
					grid : ARRAY[0..1, 0..2] OF INT;
					pumps : ARRAY[1..2] OF Pump;
					m : Mode;
					x : INT;
					i : INT := 2;
					text : STRING;
					n : INT;
				END_VAR
				p.run := TRUE;
				p.mode := AUTO_;
				q := p;
				grid[1, 2] := 42;
				x := grid[1, 2] + table[i];
				pumps[i].rate := 9.0;
				m := Mode#AUTO_;
				text := CONCAT(q.tag, '/A');
				n := LEN(text) + FIND(text, 'A');
				END_PROGRAM
				""");
		cpu.scan();
		assertFalse(cpu.faulted(), () -> cpu.diagnostics().toString());
		assertEquals(true, cpu.read("MAIN.Q.RUN"));
		assertEquals(2.5, (Double) cpu.read("MAIN.Q.RATE"), 1e-6);
		assertEquals(2L, cpu.read("MAIN.Q.MODE"));
		assertEquals(62L, cpu.read("MAIN.X"));
		assertEquals(9.0, (Double) cpu.read("MAIN.PUMPS[2].RATE"), 1e-6);
		assertEquals("P-101/A", cpu.read("MAIN.TEXT"));
		assertEquals(14L, cpu.read("MAIN.N"));
	}

	@Test
	void functionsAndBlocks() {
		Cpu cpu = cpu("""
				FUNCTION ADD3 : INT
				VAR_INPUT a, b : INT; c : INT := 100; END_VAR
				VAR_OUTPUT twice : INT; END_VAR
				twice := a * 2;
				ADD3 := a + b + c;
				END_FUNCTION

				FUNCTION BUMP : BOOL
				VAR_IN_OUT v : INT; END_VAR
				v := v + 1;
				BUMP := TRUE;
				END_FUNCTION

				FUNCTION_BLOCK COUNTER
				VAR_INPUT step : INT := 1; END_VAR
				VAR_OUTPUT total : INT; END_VAR
				total := total + step;
				END_FUNCTION_BLOCK

				FUNCTION_BLOCK DOUBLE_COUNTER
				VAR_OUTPUT total : INT; END_VAR
				VAR inner : COUNTER := (step := 5); END_VAR
				inner();
				total := inner.total * 2;
				END_FUNCTION_BLOCK

				PROGRAM MAIN
				VAR
					r1, r2, tw, v : INT;
					c1, c2 : COUNTER;
					d : DOUBLE_COUNTER;
					arr : ARRAY[1..2] OF COUNTER;
					i : INT := 2;
					got : INT;
				END_VAR
				r1 := ADD3(1, 2);
				r2 := ADD3(b := 1, a := ADD3(1, 1, 0), c := 0, twice => tw);
				BUMP(v);
				BUMP(v := v);
				c1(step := 3);
				c2();
				c2(total => got);
				d();
				arr[i](step := 7);
				END_PROGRAM
				""");
		cpu.scan();
		assertFalse(cpu.faulted(), () -> cpu.diagnostics().toString());
		assertEquals(103L, cpu.read("MAIN.R1"));
		assertEquals(3L, cpu.read("MAIN.R2"));
		assertEquals(4L, cpu.read("MAIN.TW"));
		assertEquals(2L, cpu.read("MAIN.V"));
		assertEquals(3L, cpu.read("MAIN.C1.TOTAL"));
		assertEquals(2L, cpu.read("MAIN.GOT"));
		assertEquals(10L, cpu.read("MAIN.D.TOTAL"));
		assertEquals(7L, cpu.read("MAIN.ARR[2].TOTAL"));
		cpu.scan();
		assertEquals(6L, cpu.read("MAIN.C1.TOTAL"), "a function block keeps its state between scans");
		assertEquals(103L, cpu.read("MAIN.R1"), "a function does not");
	}

	@Test
	void conversionsAndGenerics() {
		Cpu cpu = once("a : INT; b : DINT; c : REAL; d : BOOL; e : WORD; f : INT; g : REAL; h : INT; i : STRING; j : INT; k : INT;", """
				a := REAL_TO_INT(2.5);
				b := TRUNC(2.9);
				c := INT_TO_REAL(3) / 2;
				d := INT_TO_BOOL(5);
				e := ROL(WORD#16#8001, 1);
				f := LIMIT(0, 200, 100);
				g := SQRT(16.0);
				h := MUX(1, 10, 20, 30);
				i := INT_TO_STRING(42);
				j := STRING_TO_INT('17');
				k := SEL(TRUE, 1, 2);
				""");
		assertEquals(2L, cpu.read("MAIN.A"), "rounds half to even");
		assertEquals(2L, cpu.read("MAIN.B"));
		assertEquals(1.5, (Double) cpu.read("MAIN.C"), 1e-6);
		assertEquals(true, cpu.read("MAIN.D"));
		assertEquals(0x0003L, cpu.read("MAIN.E"));
		assertEquals(100L, cpu.read("MAIN.F"));
		assertEquals(4.0, (Double) cpu.read("MAIN.G"), 1e-6);
		assertEquals(20L, cpu.read("MAIN.H"));
		assertEquals("42", cpu.read("MAIN.I"));
		assertEquals(17L, cpu.read("MAIN.J"));
		assertEquals(2L, cpu.read("MAIN.K"));
	}

	@Test
	void bitAccess() {
		Cpu cpu = once("w : WORD; a, b : BOOL;", "w.3 := TRUE; w.0 := TRUE; w.0 := FALSE; a := w.3; b := w.4;");
		assertEquals(8L, cpu.read("MAIN.W"));
		assertEquals(true, cpu.read("MAIN.A"));
		assertEquals(false, cpu.read("MAIN.B"));
	}

	@Test
	void theScanUsesAnImageOfItsInputs() {
		Cpu cpu = cpu("""
				VAR_GLOBAL level AT %IW4 : INT; END_VAR
				PROGRAM MAIN
				VAR start AT %IX0.3 : BOOL; motor AT %QX1.0 : BOOL; out AT %QW2 : INT; scaled : INT; END_VAR
				motor := start;
				out := level * 2;
				%MW10 := INT_TO_WORD(out);
				scaled := WORD_TO_INT(%MW10);
				END_PROGRAM
				""");
		cpu.setInputBit(0, 3, true);
		cpu.setInputWord(4, 1000);
		assertFalse(cpu.outputBit(1, 0), "nothing changes until a scan");
		cpu.scan();
		assertTrue(cpu.outputBit(1, 0));
		assertEquals(2000, cpu.outputWord(2));
		assertEquals(2000L, cpu.read("MAIN.SCALED"));
		assertEquals(1000L, cpu.read("LEVEL"));
		assertEquals(true, cpu.read("MAIN.START"));
		cpu.setInputBit(0, 3, false);
		cpu.scan();
		assertFalse(cpu.outputBit(1, 0));
	}

	@Test
	void forcingOverridesTheFieldUntilReleased() {
		Cpu cpu = cpu("PROGRAM MAIN VAR a AT %IX0.0 : BOOL; b AT %QX0.0 : BOOL; c AT %QX0.1 : BOOL; v AT %IW2 : INT; w AT %QW2 : INT; END_VAR b := a; c := TRUE; w := v; END_PROGRAM");
		cpu.force("%IX0.0", "TRUE");
		cpu.force("%QX0.1", "FALSE");
		cpu.force("%IW2", "1234");
		assertTrue(cpu.forcing());
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		assertFalse(cpu.outputBit(0, 1));
		assertEquals(1234, cpu.outputWord(2));
		cpu.release("%IX0.0");
		cpu.release("%QX0.1");
		cpu.release("%IW2");
		assertFalse(cpu.forcing());
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0));
		assertTrue(cpu.outputBit(0, 1));
		assertEquals(0, cpu.outputWord(2));
	}

	@Test
	void timersKeepToTheScan() {
		Cpu cpu = cpu("""
				PROGRAM MAIN
				VAR go AT %IX0.0 : BOOL; on_t : TON; off_t : TOF; pulse : TP; END_VAR
				on_t(IN := go, PT := T#200ms);
				off_t(IN := go, PT := T#100ms);
				pulse(IN := go, PT := T#150ms);
				END_PROGRAM
				""");
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertEquals(false, cpu.read("MAIN.ON_T.Q"));
		assertEquals(true, cpu.read("MAIN.OFF_T.Q"));
		assertEquals(true, cpu.read("MAIN.PULSE.Q"));
		scans(cpu, 3);
		assertEquals(false, cpu.read("MAIN.ON_T.Q"), "150 ms in, of 200");
		assertEquals(false, cpu.read("MAIN.PULSE.Q"), "a 150 ms pulse is over");
		cpu.scan();
		assertEquals(true, cpu.read("MAIN.ON_T.Q"));
		assertEquals(200L, cpu.read("MAIN.ON_T.ET"));
		cpu.setInputBit(0, 0, false);
		cpu.scan();
		assertEquals(false, cpu.read("MAIN.ON_T.Q"));
		assertEquals(0L, cpu.read("MAIN.ON_T.ET"));
		assertEquals(true, cpu.read("MAIN.OFF_T.Q"), "the off delay holds");
		scans(cpu, 2);
		assertEquals(false, cpu.read("MAIN.OFF_T.Q"));
	}

	@Test
	void countersAndLatches() {
		Cpu cpu = cpu("""
				PROGRAM MAIN
				VAR clk AT %IX0.0 : BOOL; r AT %IX0.1 : BOOL; up : CTU; down : CTD; ud : CTUD; rt : R_TRIG; ft : F_TRIG; sr1 : SR; rs1 : RS; edges : INT; END_VAR
				up(CU := clk, R := r, PV := 2);
				down(CD := clk, LD := _FIRST_SCAN, PV := 2);
				ud(CU := clk, CD := FALSE, PV := 1);
				rt(CLK := clk);
				ft(CLK := clk);
				IF rt.Q OR ft.Q THEN edges := edges + 1; END_IF;
				sr1(S1 := clk, R := r);
				rs1(S := clk, R1 := r);
				END_PROGRAM
				""");
		for (boolean level : new boolean[] {true, false, true, false}) {
			cpu.setInputBit(0, 0, level);
			cpu.scan();
		}
		assertEquals(2L, cpu.read("MAIN.UP.CV"));
		assertEquals(true, cpu.read("MAIN.UP.Q"));
		assertEquals(1L, cpu.read("MAIN.DOWN.CV"), "the first edge fell on the load scan");
		assertEquals(false, cpu.read("MAIN.DOWN.Q"));
		assertEquals(4L, cpu.read("MAIN.EDGES"));
		assertEquals(true, cpu.read("MAIN.SR1.Q1"), "latched");
		cpu.setInputBit(0, 1, true);
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertEquals(true, cpu.read("MAIN.SR1.Q1"), "SR: set wins");
		assertEquals(false, cpu.read("MAIN.RS1.Q1"), "RS: reset wins");
		assertEquals(0L, cpu.read("MAIN.UP.CV"));
	}

	@Test
	void tasksRunAtTheirIntervalsInPriorityOrder() {
		Cpu cpu = cpu("""
				VAR_GLOBAL order : DINT; END_VAR
				PROGRAM FAST_P VAR n : INT; END_VAR n := n + 1; END_PROGRAM
				PROGRAM SLOW_P VAR n : INT; seen : DINT; END_VAR n := n + 1; END_PROGRAM
				CONFIGURATION C
					RESOURCE R ON CPU
						TASK SLOW (INTERVAL := T#250ms, PRIORITY := 5);
						TASK FAST (INTERVAL := T#50ms, PRIORITY := 1);
						PROGRAM S WITH SLOW : SLOW_P;
						PROGRAM F WITH FAST : FAST_P;
					END_RESOURCE
				END_CONFIGURATION
				""");
		scans(cpu, 10);
		assertEquals(10L, cpu.read("F.N"));
		assertEquals(2L, cpu.read("S.N"));
		assertEquals("FAST", cpu.program().tasks().getFirst().name());
	}

	@Test
	void aTaskFasterThanTheScanIsRefused() {
		assertError("PROGRAM P END_PROGRAM CONFIGURATION C TASK T (INTERVAL := T#10ms, PRIORITY := 1); PROGRAM X WITH T : P; END_CONFIGURATION", "whole number of 50 ms");
	}

	@Test
	void theWatchdogFaultsARunawayProgram() {
		Cpu cpu = cpu("PROGRAM MAIN\nVAR q AT %QX0.0 : BOOL; i : DINT; END_VAR\nq := TRUE;\nWHILE TRUE DO\n\ti := i + 1;\nEND_WHILE;\nEND_PROGRAM");
		long start = System.nanoTime();
		cpu.scan();
		assertTrue(System.nanoTime() - start < 100_000_000L, "a runaway costs the server very little");
		assertTrue(cpu.faulted());
		Diagnostic last = cpu.diagnostics().getLast();
		assertTrue(last.event().contains("watchdog"), last::toString);
		assertEquals("MAIN", last.pou());
		assertTrue(last.line() >= 4 && last.line() <= 6, last::toString);
		assertFalse(cpu.outputBit(0, 0), "a fault drops the outputs");
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0), "it stays stopped");
	}

	@Test
	void runtimeFaultsStopTheCpuAndNameTheLine() {
		Cpu div = cpu("PROGRAM MAIN\nVAR a : INT;\nb : INT; END_VAR\na := 1 / b;\nEND_PROGRAM");
		div.scan();
		assertTrue(div.faulted());
		assertEquals(4, div.diagnostics().getLast().line());
		assertTrue(div.diagnostics().getLast().event().contains("division by zero"));
		Cpu bounds = cpu("PROGRAM MAIN VAR a : ARRAY[1..3] OF INT; i : INT := 9; END_VAR a[i] := 1; END_PROGRAM");
		bounds.scan();
		assertTrue(bounds.faulted());
		assertTrue(bounds.diagnostics().getLast().event().contains("outside 1..3"));
		bounds.write("MAIN.I", 2L);
		bounds.setMode(Cpu.Mode.STOP);
		bounds.setMode(Cpu.Mode.RUN);
		assertFalse(bounds.faulted(), "the keyswitch clears a fault");
	}

	@Test
	void retainSurvivesAWarmRestartOnly() {
		Cpu cpu = cpu("""
				VAR_GLOBAL RETAIN g : INT; END_VAR
				PROGRAM MAIN
				VAR RETAIN kept : INT := 5; t : TOTALIZER; END_VAR
				VAR lost : INT := 5; first : INT; END_VAR
				kept := kept + 1; lost := lost + 1; g := g + 1;
				IF _FIRST_SCAN THEN first := first + 1; END_IF;
				END_PROGRAM
				""");
		scans(cpu, 3);
		assertEquals(8L, cpu.read("MAIN.KEPT"));
		assertEquals(1L, cpu.read("MAIN.FIRST"));
		cpu.powerLost();
		cpu.powerRestored();
		cpu.scan();
		assertEquals(9L, cpu.read("MAIN.KEPT"));
		assertEquals(4L, cpu.read("G"));
		assertEquals(6L, cpu.read("MAIN.LOST"));
		assertEquals(1L, cpu.read("MAIN.FIRST"), "first scan again after a restart");
		cpu.start(Cpu.Restart.COLD);
		cpu.scan();
		assertEquals(6L, cpu.read("MAIN.KEPT"));
	}

	@Test
	void stopDropsOutputsAndPowerLossToo() {
		Cpu cpu = cpu("PROGRAM MAIN VAR q AT %QX0.0 : BOOL; END_VAR q := TRUE; END_PROGRAM");
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		cpu.setMode(Cpu.Mode.STOP);
		assertFalse(cpu.outputBit(0, 0));
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0));
		cpu.setMode(Cpu.Mode.RUN);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		cpu.powerLost();
		assertFalse(cpu.outputBit(0, 0));
	}

	@Test
	void aSaveKeepsEveryVariable() {
		Cpu cpu = cpu("PROGRAM MAIN VAR n : INT; s : STRING; t : TON; END_VAR n := n + 1; s := 'x'; t(IN := TRUE, PT := T#10s); END_PROGRAM");
		scans(cpu, 5);
		Cpu.State state = cpu.save();
		Cpu loaded = new Cpu(CpuSpec.SMALL, cpu.program());
		assertTrue(loaded.restore(state));
		assertEquals(Cpu.Mode.RUN, loaded.mode());
		loaded.scan();
		assertEquals(6L, loaded.read("MAIN.N"));
		assertEquals("x", loaded.read("MAIN.S"));
		assertEquals(250L, loaded.read("MAIN.T.ET"), "timers run on across a save");
	}

	@Test
	void compileErrorsNameTheLine() {
		List<CompileError> found = errors("PROGRAM MAIN\nVAR a : INT; END_VAR\na := b;\na := TRUE;\nEND_PROGRAM");
		assertEquals(2, found.size(), found::toString);
		assertEquals(3, found.get(0).line());
		assertTrue(found.get(0).message().contains("B is not declared"));
		assertEquals(4, found.get(1).line());
	}

	@Test
	void theCompilerHoldsTheStandardsRules() {
		String p = "PROGRAM MAIN VAR a : INT; d : DINT; r : REAL; b : BOOL; c : INT := 1; k : TON; END_VAR ";
		assertError(p + "a := d; END_PROGRAM", "does not fit");
		assertError(p + "a := r; END_PROGRAM", "does not fit");
		assertError(p + "a := 40000; END_PROGRAM", "40000 does not fit");
		assertError(p + "b := 1; END_PROGRAM", "does not fit");
		assertError(p + "a := k; END_PROGRAM", "does not fit");
		assertError(p + "k.Q := TRUE; END_PROGRAM", "is an output");
		assertError(p + "a := k.START; END_PROGRAM", "private");
		assertError(p + "%IX0.0 := TRUE; END_PROGRAM", "is an input");
		assertError(p + "%QW99999 := 1; END_PROGRAM", "is beyond");
		assertError(p + "EXIT; END_PROGRAM", "belongs in a loop");
		assertError("PROGRAM MAIN VAR CONSTANT k : INT := 1; END_VAR k := 2; END_PROGRAM", "is a constant");
		assertError("FUNCTION F : INT VAR_INPUT a : INT; END_VAR a := 1; F := 1; END_FUNCTION", "is an input");
		assertError("FUNCTION F : INT VAR_INPUT a : INT; END_VAR F := F(a); END_FUNCTION", "calls itself");
		assertError("FUNCTION_BLOCK A VAR x : A; END_VAR END_FUNCTION_BLOCK", "contains itself");
		assertError("FUNCTION_BLOCK B VAR x AT %IX0.0 : BOOL; END_VAR END_FUNCTION_BLOCK", "AT can only");
		assertError("PROGRAM MAIN VAR x AT %IW0 : BOOL; END_VAR END_PROGRAM", "does not fit a BOOL");
	}

	@Test
	void anUnclosedCommentAndAnUnknownTypeAreErrorsToo() {
		assertError("PROGRAM MAIN (* never closed END_PROGRAM", "never closed");
		assertError("PROGRAM MAIN VAR a : NOPE; END_VAR END_PROGRAM", "no type NOPE");
	}

	@Test
	void identifiersIgnoreCaseAndCommentsAreSkipped() {
		Cpu cpu = once("Speed : INT; (* a block comment *)", "// a line comment\nSPEED := 3; speed := sPeEd + 1;");
		assertEquals(4L, cpu.read("main.speed"));
	}

	@Test
	void tooMuchMemoryIsACompileError() {
		assertError("PROGRAM MAIN VAR a : ARRAY[1..100000] OF INT; END_VAR END_PROGRAM", "memory slots");
		assertTrue(PlcCompiler.compile(CpuSpec.LARGE, "PROGRAM MAIN VAR a : ARRAY[1..100000] OF INT; END_VAR END_PROGRAM").ok());
	}

	@Test
	void scaleAlarmAndTotalizer() {
		Cpu cpu = cpu("""
				PROGRAM MAIN
				VAR raw AT %IW0 : INT; level : REAL; a : ALARM_HL; t : TOTALIZER; END_VAR
				level := SCALE(INT_TO_REAL(raw), 0.0, 32000.0, 0.0, 100.0);
				a(PV := level, HI := 80.0, LO := 20.0, DB := 5.0);
				t(RATE := 2.0);
				END_PROGRAM
				""");
		cpu.setInputWord(0, 28000);
		scans(cpu, 21);
		assertEquals(87.5, (Double) cpu.read("MAIN.LEVEL"), 1e-3);
		assertEquals(true, cpu.read("MAIN.A.HIGH"));
		assertEquals(2.0, (Double) cpu.read("MAIN.T.TOTAL"), 1e-6, "2 units a second for one second");
		cpu.setInputWord(0, 24800);
		cpu.scan();
		assertEquals(true, cpu.read("MAIN.A.HIGH"), "77.5 %: inside the deadband");
		cpu.setInputWord(0, 22400);
		cpu.scan();
		assertEquals(false, cpu.read("MAIN.A.HIGH"));
	}

	private static Cpu pidLoop(String extra) {
		return cpu("""
				PROGRAM MAIN
				VAR pv, sp, man : REAL; auto : BOOL := TRUE; c : PID; END_VAR
				c(AUTO := auto, PV := pv, SP := sp, MAN := man, KP := 2.0, TI := T#5s%s);
				END_PROGRAM
				""".formatted(extra));
	}

	/** A first-order process: PV lags towards the output. */
	private static double step(Cpu cpu, double pv) {
		cpu.write("MAIN.PV", pv);
		cpu.scan();
		double out = (Double) cpu.read("MAIN.C.OUT");
		return pv + (out - pv) * 0.05 / 10.0;
	}

	@Test
	void aPidHoldsItsSetpoint() {
		Cpu cpu = pidLoop("");
		cpu.write("MAIN.SP", 60.0);
		double pv = 20;
		for (int i = 0; i < 20 * 120; i++) {
			pv = step(cpu, pv);
		}
		assertEquals(60.0, pv, 0.5);
		assertEquals(60.0, (Double) cpu.read("MAIN.C.OUT"), 1.0);
		assertFalse((Boolean) cpu.read("MAIN.C.SAT"));
	}

	@Test
	void aPidDoesNotWindUp() {
		Cpu cpu = pidLoop("");
		cpu.write("MAIN.SP", 500.0);
		double pv = 20;
		for (int i = 0; i < 20 * 300; i++) {
			pv = step(cpu, pv);
		}
		assertEquals(100.0, (Double) cpu.read("MAIN.C.OUT"), 1e-6);
		assertTrue((Boolean) cpu.read("MAIN.C.SAT"));
		cpu.write("MAIN.SP", 50.0);
		int scans = 0;
		while ((Double) cpu.read("MAIN.C.OUT") >= 100.0 && scans < 2000) {
			pv = step(cpu, pv);
			scans++;
		}
		assertTrue(scans < 20, "it comes off the limit at once, in " + scans + " scans");
	}

	@Test
	void aPidGoesToAutoWithoutABump() {
		Cpu cpu = pidLoop("");
		cpu.write("MAIN.AUTO", false);
		cpu.write("MAIN.MAN", 37.0);
		cpu.write("MAIN.SP", 37.0);
		double pv = 37;
		for (int i = 0; i < 40; i++) {
			pv = step(cpu, pv);
		}
		assertEquals(37.0, (Double) cpu.read("MAIN.C.OUT"), 1e-6);
		cpu.write("MAIN.AUTO", true);
		pv = step(cpu, pv);
		assertEquals(37.0, (Double) cpu.read("MAIN.C.OUT"), 0.1);
	}

	@Test
	void aPidRampsItsSetpointAndCanReverse() {
		Cpu ramped = pidLoop(", SP_RAMP := 10.0");
		ramped.write("MAIN.PV", 0.0);
		ramped.scan();
		ramped.write("MAIN.SP", 100.0);
		for (int i = 0; i < 20; i++) {
			ramped.scan();
		}
		assertEquals(10.0, (Double) ramped.read("MAIN.C.SP_ACT"), 0.6, "10 units a second, for one second");
		Cpu reverse = pidLoop(", REVERSE := TRUE");
		reverse.write("MAIN.SP", 10.0);
		reverse.write("MAIN.PV", 50.0);
		reverse.scan();
		reverse.scan();
		assertTrue((Double) reverse.read("MAIN.C.OUT") > 0, "too high, on a reverse loop, drives the output up");
	}

	/** What the watchdog's budget really costs a server, printed for the spec's figures, with a limit far above them so it never flakes. */
	@Test
	void aFullBudgetIsCheap() {
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.LARGE, "PROGRAM MAIN VAR i : DINT; x : REAL; END_VAR WHILE TRUE DO i := i + 1; x := x * 0.5 + 1.0; END_WHILE; END_PROGRAM");
		assertTrue(result.ok(), result.errors()::toString);
		Cpu warm = new Cpu(CpuSpec.LARGE, result.program());
		for (int i = 0; i < 2000; i++) {
			warm.start(Cpu.Restart.WARM);
			warm.scan();
		}
		long start = System.nanoTime();
		int runs = 200;
		for (int i = 0; i < runs; i++) {
			warm.start(Cpu.Restart.WARM);
			warm.scan();
		}
		double ms = (System.nanoTime() - start) / 1e6 / runs;
		System.out.printf("[plc] a large CPU's full budget of %d instructions: %.3f ms%n", CpuSpec.LARGE.instructionsPerScan(), ms);
		assertTrue(ms < 25, "took " + ms + " ms");
	}

	@Test
	void aLargeProgramFitsTheBudget() {
		Cpu cpu = cpu("""
				PROGRAM MAIN
				VAR t : ARRAY[1..100] OF TON; c : ARRAY[1..20] OF PID; i : INT; x : REAL; END_VAR
				FOR i := 1 TO 100 DO t[i](IN := TRUE, PT := T#1s); END_FOR;
				FOR i := 1 TO 20 DO c[i](PV := x, SP := 50.0, KP := 1.0, TI := T#10s); x := c[i].OUT; END_FOR;
				END_PROGRAM
				""");
		scans(cpu, 100);
		assertFalse(cpu.faulted(), () -> cpu.diagnostics().toString());
		assertTrue(cpu.lastInstructions() < CpuSpec.SMALL.instructionsPerScan(), "used " + cpu.lastInstructions());
	}
}
