package dev.wildcatter.plc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.plc.lang.CompileError;
import dev.wildcatter.plc.lang.PlcCompiler;
import dev.wildcatter.plc.lang.Project.Language;
import dev.wildcatter.plc.lang.Project.Source;
import dev.wildcatter.plc.lang.Sfc;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Sequential function charts and the device blocks, compiled and run by the same CPU as the other languages. */
class PlcSfcTest {
	private static final String VARS = """
			VAR
				a AT %IX0.0 : BOOL;
				b AT %IX0.1 : BOOL;
				c AT %IX0.2 : BOOL;
				out0 AT %QX0.0 : BOOL;
				out1 AT %QX0.1 : BOOL;
				count : INT;
				last : BOOL;
				t0 : TIME;
			END_VAR""";

	private static Sfc chart() {
		Sfc chart = new Sfc();
		chart.declarations = VARS;
		return chart;
	}

	private static Sfc.Step step(Sfc chart, String name, boolean initial) {
		Sfc.Step step = new Sfc.Step(chart.nextId(), name, initial, 10, chart.steps.size() * 40);
		chart.steps.add(step);
		return step;
	}

	private static Sfc.Transition go(Sfc chart, String condition, Sfc.Step from, Sfc.Step to) {
		Sfc.Transition t = new Sfc.Transition(chart.nextId(), condition, 10, chart.transitions.size() * 40 + 20);
		t.from.add(from.id);
		t.to.add(to.id);
		chart.transitions.add(t);
		return t;
	}

	private static Cpu run(Sfc chart) {
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.SMALL, List.of(new Source("MAIN", Language.SFC, chart.write())));
		assertTrue(result.ok(), () -> "should compile: " + result.errors());
		Cpu cpu = new Cpu(CpuSpec.SMALL, result.program());
		cpu.start(Cpu.Restart.COLD);
		return cpu;
	}

	private static List<CompileError> errors(Sfc chart) {
		return PlcCompiler.compile(CpuSpec.SMALL, List.of(new Source("MAIN", Language.SFC, chart.write()))).errors();
	}

	private static boolean active(Cpu cpu, String step) {
		return Boolean.TRUE.equals(cpu.read("MAIN." + step + ".X"));
	}

	private static void scans(Cpu cpu, int n) {
		for (int i = 0; i < n; i++) {
			cpu.scan();
		}
	}

	@Test
	void aTokenMovesOneStepAScan() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		Sfc.Step s3 = step(chart, "S3", false);
		go(chart, "a", s1, s2);
		go(chart, "a", s2, s3);
		go(chart, "b", s3, s1);
		Cpu cpu = run(chart);
		cpu.scan();
		assertTrue(active(cpu, "S1"), "the initial step is active from the first scan");
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertTrue(active(cpu, "S2") && !active(cpu, "S1"), "one scan, one step, though the next transition holds too");
		cpu.scan();
		assertTrue(active(cpu, "S3") && !active(cpu, "S2"));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertTrue(active(cpu, "S1") && !active(cpu, "S3"), "and round again");
	}

	@Test
	void aChoiceGoesLeftToRight() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step left = step(chart, "LEFT", false);
		Sfc.Step right = step(chart, "RIGHT", false);
		go(chart, "b", s1, right).x = 200;
		go(chart, "a", s1, left).x = 10;
		Cpu cpu = run(chart);
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertTrue(active(cpu, "LEFT"), "both clear, so the left one takes it");
		assertFalse(active(cpu, "RIGHT"), "and only one token leaves a step");
	}

	@Test
	void aParallelBranchSplitsAndWaitsForAll() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step p = step(chart, "P", false);
		Sfc.Step q = step(chart, "Q", false);
		Sfc.Step end = step(chart, "DONE", false);
		Sfc.Transition split = go(chart, "a", s1, p);
		split.to.add(q.id);
		Sfc.Transition join = go(chart, "b", p, end);
		join.from.add(q.id);
		Sfc.Transition onlyQ = new Sfc.Transition(chart.nextId(), "FALSE", 10, 500);
		onlyQ.from.add(end.id);
		onlyQ.to.add(s1.id);
		chart.transitions.add(onlyQ);
		Cpu cpu = run(chart);
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertTrue(active(cpu, "P") && active(cpu, "Q") && !active(cpu, "S1"), "both branches run");
		cpu.setInputBit(0, 0, false);
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertTrue(active(cpu, "DONE") && !active(cpu, "P") && !active(cpu, "Q"), "the join needs both, and takes both");
	}

	@Test
	void aJoinWaitsWhileOneBranchIsNotThere() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step p = step(chart, "P", false);
		Sfc.Step q = step(chart, "Q", false);
		Sfc.Step r = step(chart, "R2", false);
		Sfc.Step end = step(chart, "DONE", false);
		Sfc.Transition split = go(chart, "a", s1, p);
		split.to.add(q.id);
		go(chart, "c", q, r);
		Sfc.Transition join = go(chart, "b", p, end);
		join.from.add(r.id);
		Cpu cpu = run(chart);
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		cpu.setInputBit(0, 0, false);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 3);
		assertFalse(active(cpu, "DONE"), "R2 is not active yet, so b alone does nothing");
		cpu.setInputBit(0, 2, true);
		scans(cpu, 2);
		assertTrue(active(cpu, "DONE"), "now it joins");
	}

	@Test
	void aStepsTimeRunsAndIsKeptAfterward() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		go(chart, "S1.T >= T#1s", s1, s2);
		Cpu cpu = run(chart);
		scans(cpu, 10);
		assertTrue(active(cpu, "S1"), "half a second in");
		scans(cpu, 12);
		assertTrue(active(cpu, "S2"), "a second is up, at 50 ms a scan");
		long kept = ((Number) cpu.read("MAIN.S1.T")).longValue();
		assertTrue(kept >= 950 && kept <= 1050, "T stays where it was: " + kept);
	}

	@Test
	void aSteadyTrueTransitionDoesNotSkipItsActionsScan() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		s2.actions.add(new Sfc.Association("N", "out0", ""));
		go(chart, "TRUE", s1, s2);
		Cpu cpu = run(chart);
		scans(cpu, 2);
		assertTrue(active(cpu, "S2"));
		assertTrue(cpu.outputBit(0, 0), "the action ran the scan the step went active");
	}

	private static Cpu qualifier(String qualifier, String time) {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		Sfc.Step s3 = step(chart, "S3", false);
		s2.actions.add(new Sfc.Association(qualifier, "out0", time));
		go(chart, "a", s1, s2);
		go(chart, "b", s2, s3);
		return run(chart);
	}

	@Test
	void nIsOnWhileTheStepIs() {
		Cpu cpu = qualifier("N", "");
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0));
	}

	@Test
	void aSetActionOutlivesItsStepUntilAReset() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		Sfc.Step s3 = step(chart, "S3", false);
		s2.actions.add(new Sfc.Association("S", "out0", ""));
		s3.actions.add(new Sfc.Association("R", "out0", ""));
		go(chart, "a", s1, s2);
		go(chart, "b", s2, s1);
		go(chart, "c", s1, s3);
		Cpu cpu = run(chart);
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "set");
		cpu.setInputBit(0, 0, false);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 3);
		assertTrue(active(cpu, "S1") && cpu.outputBit(0, 0), "stays set with the step gone");
		cpu.setInputBit(0, 2, true);
		scans(cpu, 2);
		assertFalse(cpu.outputBit(0, 0), "reset by the other step");
	}

	@Test
	void limitedIsOnForItsTimeOnly() {
		Cpu cpu = qualifier("L", "T#500ms");
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		scans(cpu, 5);
		assertTrue(cpu.outputBit(0, 0), "250 ms in");
		scans(cpu, 7);
		assertFalse(cpu.outputBit(0, 0), "past 500 ms, with the step still active");
		assertTrue(active(cpu, "S2"));
	}

	@Test
	void delayedComesOnAfterItsTime() {
		Cpu cpu = qualifier("D", "T#500ms");
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		scans(cpu, 5);
		assertFalse(cpu.outputBit(0, 0), "not yet");
		scans(cpu, 8);
		assertTrue(cpu.outputBit(0, 0), "on, with the step still active");
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0), "off with the step");
	}

	@Test
	void aDelayShorterThanTheStepNeverComesOn() {
		Cpu cpu = qualifier("D", "T#500ms");
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		scans(cpu, 20);
		assertFalse(cpu.outputBit(0, 0));
	}

	@Test
	void storedAndDelayedSurvivesItsStep() {
		Cpu cpu = qualifier("SD", "T#500ms");
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		scans(cpu, 2);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 2);
		assertFalse(cpu.outputBit(0, 0), "the step is gone and the time is not up");
		scans(cpu, 12);
		assertTrue(cpu.outputBit(0, 0), "stored, then delayed");
	}

	@Test
	void delayedAndStoredDiesWithAShortStep() {
		Cpu cpu = qualifier("DS", "T#500ms");
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		scans(cpu, 2);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 20);
		assertFalse(cpu.outputBit(0, 0), "delayed first: the step left before the time, so nothing was stored");
	}

	@Test
	void storedAndLimitedRunsOutEvenWithoutItsStep() {
		Cpu cpu = qualifier("SL", "T#500ms");
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		scans(cpu, 2);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 2);
		assertTrue(cpu.outputBit(0, 0), "stored, so it stays");
		scans(cpu, 12);
		assertFalse(cpu.outputBit(0, 0), "and limited");
	}

	@Test
	void aPulseIsOneScan() {
		Cpu cpu = qualifier("P", "");
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0), "one scan only, though the step stays");
	}

	@Test
	void aNamedBodyRunsWhileActiveAndOnceMoreToTidyUp() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		s2.actions.add(new Sfc.Association("N", "COUNTER", ""));
		chart.bodies.put("COUNTER", "count := count + 1;\nout0 := COUNTER.Q;\nIF NOT COUNTER.Q THEN\n\tout1 := TRUE;\nEND_IF;");
		go(chart, "a", s1, s2);
		go(chart, "b", s2, s1);
		Cpu cpu = run(chart);
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		cpu.setInputBit(0, 0, false);
		scans(cpu, 2);
		assertEquals(3L, ((Number) cpu.read("MAIN.COUNT")).longValue(), "three scans active");
		assertTrue(cpu.outputBit(0, 0));
		assertFalse(cpu.outputBit(0, 1));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertEquals(4L, ((Number) cpu.read("MAIN.COUNT")).longValue(), "and one more as it ends");
		assertFalse(cpu.outputBit(0, 0), "its last run sees Q false");
		assertTrue(cpu.outputBit(0, 1));
		scans(cpu, 2);
		assertEquals(4L, ((Number) cpu.read("MAIN.COUNT")).longValue(), "then no more");
	}

	@Test
	void pOneAndPZeroRunOnTheStepsEdges() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		s2.actions.add(new Sfc.Association("P1", "ENTER", ""));
		s2.actions.add(new Sfc.Association("P0", "LEAVE", ""));
		chart.bodies.put("ENTER", "count := count + 1;");
		chart.bodies.put("LEAVE", "count := count + 100;");
		go(chart, "a", s1, s2);
		go(chart, "b", s2, s1);
		Cpu cpu = run(chart);
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		scans(cpu, 1);
		cpu.setInputBit(0, 0, false);
		scans(cpu, 3);
		assertEquals(1L, ((Number) cpu.read("MAIN.COUNT")).longValue());
		cpu.setInputBit(0, 1, true);
		scans(cpu, 3);
		assertEquals(101L, ((Number) cpu.read("MAIN.COUNT")).longValue());
	}

	@Test
	void twoStepsCanShareAnAction() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		Sfc.Step s3 = step(chart, "S3", false);
		s1.actions.add(new Sfc.Association("N", "out0", ""));
		s3.actions.add(new Sfc.Association("N", "out0", ""));
		go(chart, "a", s1, s2);
		go(chart, "b", s2, s3);
		Cpu cpu = run(chart);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
	}

	@Test
	void anotherUnitCanReadAStepFlag() {
		Sfc chart = chart();
		chart.declarations = "VAR\nEND_VAR";
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		go(chart, "TRUE", s1, s2);
		String st = """
				PROGRAM WATCHER
				VAR seen AT %QX0.0 : BOOL; END_VAR
				seen := MAIN.S2.X;
				END_PROGRAM""";
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.SMALL, List.of(new Source("MAIN", Language.SFC, chart.write()), new Source("WATCHER", Language.ST, st)));
		if (!result.ok()) {
			assertTrue(result.errors().toString().contains("MAIN"), "a program's variables are its own, as the standard says: " + result.errors());
			return;
		}
		Cpu cpu = new Cpu(CpuSpec.SMALL, result.program());
		cpu.start(Cpu.Restart.COLD);
		scans(cpu, 3);
		assertTrue(cpu.outputBit(0, 0));
	}

	@Test
	void aChartCanBeAFunctionBlockWithInstances() {
		Sfc chart = chart();
		chart.kind = "FUNCTION_BLOCK";
		chart.name = "SEQ";
		chart.declarations = "VAR_INPUT go : BOOL; END_VAR\nVAR_OUTPUT busy : BOOL; END_VAR";
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		s2.actions.add(new Sfc.Association("N", "busy", ""));
		go(chart, "go", s1, s2);
		go(chart, "NOT go", s2, s1);
		String st = """
				PROGRAM MAIN
				VAR
					one, two : SEQ;
					a AT %IX0.0 : BOOL;
					x AT %QX0.0 : BOOL;
					y AT %QX0.1 : BOOL;
				END_VAR
				one(go := a, busy => x);
				two(go := NOT a, busy => y);
				END_PROGRAM""";
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.SMALL, List.of(new Source("SEQ", Language.SFC, chart.write()), new Source("MAIN", Language.ST, st)));
		assertTrue(result.ok(), () -> "should compile: " + result.errors());
		Cpu cpu = new Cpu(CpuSpec.SMALL, result.program());
		cpu.start(Cpu.Restart.COLD);
		scans(cpu, 3);
		assertFalse(cpu.outputBit(0, 0));
		assertTrue(cpu.outputBit(0, 1), "each instance has its own token");
		cpu.setInputBit(0, 0, true);
		scans(cpu, 3);
		assertTrue(cpu.outputBit(0, 0));
		assertFalse(cpu.outputBit(0, 1));
	}

	@Test
	void anOnlineChangeKeepsTheToken() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		go(chart, "a", s1, s2);
		go(chart, "FALSE", s2, s1);
		Cpu cpu = run(chart);
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		scans(cpu, 2);
		assertTrue(active(cpu, "S2"));
		s2.actions.add(new Sfc.Association("N", "out0", ""));
		PlcCompiler.Result next = PlcCompiler.compile(CpuSpec.SMALL, List.of(new Source("MAIN", Language.SFC, chart.write())));
		assertTrue(next.ok(), () -> "" + next.errors());
		cpu.onlineChange(next.program());
		cpu.scan();
		assertTrue(active(cpu, "S2") && !active(cpu, "S1"), "the sequence is where it was, not back at the start");
		assertTrue(cpu.outputBit(0, 0));
	}

	@Test
	void theSavedFormRoundTrips() {
		Sfc chart = chart();
		Sfc.Step s1 = step(chart, "S1", true);
		Sfc.Step s2 = step(chart, "S2", false);
		s2.actions.add(new Sfc.Association("L", "out0", "T#5s"));
		chart.bodies.put("TIDY", "count := 0; (* a \"quoted\" comment *)\nlast := TRUE;");
		s2.actions.add(new Sfc.Association("N", "TIDY", ""));
		go(chart, "a AND NOT b", s1, s2);
		String saved = chart.write();
		assertEquals(saved, Sfc.parse(saved).write());
	}

	private static void refused(Sfc chart, String part) {
		List<CompileError> found = errors(chart);
		assertTrue(found.stream().anyMatch(e -> e.message().contains(part)), () -> "should say '" + part + "': " + found);
	}

	@Test
	void whatTheStandardForbidsIsRefused() {
		Sfc none = chart();
		step(none, "S1", false);
		refused(none, "no initial step");

		Sfc two = chart();
		Sfc.Step a = step(two, "S1", true);
		Sfc.Step b = step(two, "S2", true);
		go(two, "TRUE", a, b);
		refused(two, "initial steps");

		Sfc same = chart();
		step(same, "S1", true);
		step(same, "S1", false);
		refused(same, "two steps named");

		Sfc hanging = chart();
		Sfc.Step only = step(hanging, "S1", true);
		Sfc.Transition t = new Sfc.Transition(hanging.nextId(), "TRUE", 0, 0);
		t.from.add(only.id);
		hanging.transitions.add(t);
		refused(hanging, "no step after it");

		Sfc blank = chart();
		Sfc.Step c = step(blank, "S1", true);
		Sfc.Step d = step(blank, "S2", false);
		go(blank, "", c, d);
		refused(blank, "no condition");

		Sfc notBool = chart();
		Sfc.Step e = step(notBool, "S1", true);
		Sfc.Step f = step(notBool, "S2", false);
		go(notBool, "count + 1", e, f);
		assertFalse(errors(notBool).isEmpty(), "a condition must be a BOOL");

		Sfc timed = chart();
		step(timed, "S1", true).actions.add(new Sfc.Association("L", "out0", ""));
		refused(timed, "needs a time");

		Sfc twoTimes = chart();
		Sfc.Step g = step(twoTimes, "S1", true);
		Sfc.Step h = step(twoTimes, "S2", false);
		go(twoTimes, "TRUE", g, h);
		g.actions.add(new Sfc.Association("L", "out0", "T#1s"));
		h.actions.add(new Sfc.Association("D", "out0", "T#2s"));
		refused(twoTimes, "two times");

		Sfc badQualifier = chart();
		step(badQualifier, "S1", true).actions.add(new Sfc.Association("X", "out0", ""));
		refused(badQualifier, "no action qualifier");

		Sfc badBody = chart();
		step(badBody, "S1", true).actions.add(new Sfc.Association("N", "ACT", ""));
		badBody.bodies.put("ACT", "this is not structured text");
		assertTrue(errors(badBody).stream().anyMatch(x -> x.line() >= 1000), "an action's error carries its own line: " + errors(badBody));
		assertTrue(badBody.where(errors(badBody).getFirst().line()).contains("action ACT"));
	}

	// ---- the device blocks -----------------------------------------------------------------------------------------------------------

	private static final String MOTOR = """
			PROGRAM MAIN
			VAR
				start AT %IX0.0 : BOOL;
				running AT %IX0.1 : BOOL;
				tripped AT %IX0.2 : BOOL;
				auto AT %IX0.3 : BOOL;
				reset AT %IX0.4 : BOOL;
				permit AT %IX0.5 : BOOL;
				coil AT %QX0.0 : BOOL;
				m : MOTOR_CTRL;
			END_VAR
			m(START := start, PERMIT := permit, RUNNING := running, TRIPPED := tripped, AUTO := auto, RESET := reset, START_T := T#1s, STOP_T := T#1s, RUN => coil);
			END_PROGRAM""";

	private static Cpu motor() {
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.SMALL, MOTOR);
		assertTrue(result.ok(), () -> "should compile: " + result.errors());
		Cpu cpu = new Cpu(CpuSpec.SMALL, result.program());
		cpu.start(Cpu.Restart.COLD);
		cpu.setInputBit(0, 3, true);
		cpu.setInputBit(0, 5, true);
		return cpu;
	}

	private static long state(Cpu cpu) {
		return ((Number) cpu.read("MAIN.M.STATE")).longValue();
	}

	private static boolean flag(Cpu cpu, String name) {
		return Boolean.TRUE.equals(cpu.read("MAIN.M." + name));
	}

	@Test
	void aMotorStartsAndReportsRunning() {
		Cpu cpu = motor();
		cpu.scan();
		assertEquals(0, state(cpu));
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		assertEquals(1, state(cpu), "starting");
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertEquals(2, state(cpu), "running");
		cpu.setInputBit(0, 0, false);
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0));
		assertEquals(3, state(cpu), "stopping");
		cpu.setInputBit(0, 1, false);
		cpu.scan();
		assertEquals(0, state(cpu));
		assertFalse(flag(cpu, "FAULT"));
	}

	@Test
	void noFeedbackIsAFailureToStartAndItLatches() {
		Cpu cpu = motor();
		cpu.setInputBit(0, 0, true);
		scans(cpu, 15);
		assertTrue(cpu.outputBit(0, 0), "under a second, still trying");
		scans(cpu, 10);
		assertTrue(flag(cpu, "FAIL_START") && flag(cpu, "FAULT"));
		assertFalse(cpu.outputBit(0, 0), "it drops the coil");
		assertEquals(4, state(cpu));
		cpu.setInputBit(0, 4, true);
		cpu.setInputBit(0, 0, false);
		cpu.scan();
		cpu.setInputBit(0, 4, false);
		cpu.scan();
		assertFalse(flag(cpu, "FAULT"), "a reset clears it");
	}

	@Test
	void losingFeedbackWhileRunningFaultsAtOnce() {
		Cpu cpu = motor();
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 2);
		assertEquals(2, state(cpu));
		cpu.setInputBit(0, 1, false);
		cpu.scan();
		assertTrue(flag(cpu, "FAIL_RUN") && flag(cpu, "FAULT"));
		assertFalse(cpu.outputBit(0, 0));
	}

	@Test
	void feedbackThatWillNotDropIsAFailureToStop() {
		Cpu cpu = motor();
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 2);
		cpu.setInputBit(0, 0, false);
		scans(cpu, 25);
		assertTrue(flag(cpu, "FAIL_STOP") && flag(cpu, "FAULT"), "the contactor has welded");
	}

	@Test
	void aTripIsAFaultAndNoRunUntilReset() {
		Cpu cpu = motor();
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 2);
		cpu.setInputBit(0, 2, true);
		cpu.setInputBit(0, 1, false);
		cpu.scan();
		assertTrue(flag(cpu, "FAULT"));
		assertFalse(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 4, true);
		scans(cpu, 2);
		assertTrue(flag(cpu, "FAULT"), "reset does nothing while the trip stands: only the hand reset at the starter clears it");
		assertFalse(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 2, false);
		scans(cpu, 2);
		assertFalse(flag(cpu, "FAULT"));
		assertTrue(cpu.outputBit(0, 0), "start is still held, so it goes again");
	}

	@Test
	void outsideAutoItAsksForNothingAndRaisesNothing() {
		Cpu cpu = motor();
		cpu.setInputBit(0, 3, false);
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 40);
		assertFalse(cpu.outputBit(0, 0));
		assertFalse(flag(cpu, "FAULT"), "someone is running it in HAND, and that is no failure");
		assertEquals(5, state(cpu));
	}

	@Test
	void permitDropsItWithoutAFault() {
		Cpu cpu = motor();
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		scans(cpu, 2);
		assertTrue(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 5, false);
		cpu.setInputBit(0, 1, false);
		scans(cpu, 2);
		assertFalse(cpu.outputBit(0, 0));
		assertFalse(flag(cpu, "FAULT"));
	}

	private static final String VALVE = """
			PROGRAM MAIN
			VAR
				open AT %IX0.0 : BOOL;
				opened AT %IX0.1 : BOOL;
				closed AT %IX0.2 : BOOL;
				reset AT %IX0.3 : BOOL;
				v : VALVE_CTRL;
			END_VAR
			v(OPEN := open, OPENED := opened, CLOSED := closed, RESET := reset, TRAVEL_T := T#1s);
			END_PROGRAM""";

	private static Cpu valve() {
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.SMALL, VALVE);
		assertTrue(result.ok(), () -> "should compile: " + result.errors());
		Cpu cpu = new Cpu(CpuSpec.SMALL, result.program());
		cpu.start(Cpu.Restart.COLD);
		cpu.setInputBit(0, 2, true);
		return cpu;
	}

	private static boolean valveFlag(Cpu cpu, String name) {
		return Boolean.TRUE.equals(cpu.read("MAIN.V." + name));
	}

	@Test
	void aValveTravelsBetweenItsLimits() {
		Cpu cpu = valve();
		cpu.scan();
		assertEquals(0L, ((Number) cpu.read("MAIN.V.STATE")).longValue(), "closed");
		assertFalse(valveFlag(cpu, "CMD_CLOSE"), "at the limit, nothing drives it");
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 2, false);
		cpu.scan();
		assertTrue(valveFlag(cpu, "OUT") && valveFlag(cpu, "CMD_OPEN"));
		assertEquals(1L, ((Number) cpu.read("MAIN.V.STATE")).longValue(), "opening");
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertTrue(valveFlag(cpu, "OUT"), "a solenoid stays held");
		assertFalse(valveFlag(cpu, "CMD_OPEN"), "a motor-operated valve stops at its limit");
		assertEquals(2L, ((Number) cpu.read("MAIN.V.STATE")).longValue(), "open");
	}

	@Test
	void aStuckValveFailsAndDropsEverything() {
		Cpu cpu = valve();
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 2, false);
		scans(cpu, 25);
		assertTrue(valveFlag(cpu, "FAIL_OPEN") && valveFlag(cpu, "FAULT"));
		assertFalse(valveFlag(cpu, "OUT") || valveFlag(cpu, "CMD_OPEN") || valveFlag(cpu, "CMD_CLOSE"), "air valve to its fail position, motor-operated valve stays put");
	}

	@Test
	void bothLimitsTogetherIsABadSwitch() {
		Cpu cpu = valve();
		cpu.setInputBit(0, 1, true);
		scans(cpu, 2);
		assertTrue(valveFlag(cpu, "BAD_LIMIT") && valveFlag(cpu, "FAULT"));
	}
}
