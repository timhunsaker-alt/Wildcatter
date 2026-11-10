package dev.wildcatter.plc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.plc.lang.CompileError;
import dev.wildcatter.plc.lang.Fbd;
import dev.wildcatter.plc.lang.Ladder;
import dev.wildcatter.plc.lang.PlcCompiler;
import dev.wildcatter.plc.lang.Project;
import dev.wildcatter.plc.lang.Project.Language;
import dev.wildcatter.plc.lang.Project.Source;
import dev.wildcatter.plc.lang.WireRouter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ladder and function block diagrams, compiled and run by the same CPU as structured text. */
class PlcGraphicalTest {
	private static final String VARS = """
			VAR
				start AT %IX0.0 : BOOL;
				stop AT %IX0.1 : BOOL;
				motor AT %QX0.0 : BOOL;
				other : BOOL;
				n : INT;
				x : INT := 7;
				y : INT;
				late : BOOL;
			END_VAR""";

	private static Cpu run(List<Source> sources) {
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.SMALL, sources);
		assertTrue(result.ok(), () -> "should compile: " + result.errors());
		Cpu cpu = new Cpu(CpuSpec.SMALL, result.program());
		cpu.start(Cpu.Restart.COLD);
		return cpu;
	}

	private static Ladder ladder() {
		Ladder ladder = new Ladder();
		ladder.declarations = VARS;
		return ladder;
	}

	private static Ladder.Rung rung(Ladder ladder, String coil, Ladder.Node... nodes) {
		Ladder.Rung rung = new Ladder.Rung();
		rung.body.items.addAll(List.of(nodes));
		if (coil != null) {
			rung.coils.add(new Ladder.Coil(Ladder.CoilKind.COIL, coil));
		}
		ladder.rungs.add(rung);
		return rung;
	}

	private static Ladder.Contact no(String var) {
		return new Ladder.Contact(var, false);
	}

	private static Ladder.Contact nc(String var) {
		return new Ladder.Contact(var, true);
	}

	private static Ladder.Parallel either(Ladder.Node... nodes) {
		Ladder.Parallel parallel = new Ladder.Parallel();
		parallel.items.addAll(List.of(nodes));
		return parallel;
	}

	private static Source ld(Ladder ladder) {
		return new Source(ladder.name, Language.LD, ladder.write());
	}

	/** The classic: start in parallel with a seal-in, in series with a normally closed stop. */
	@Test
	void aSealInRung() {
		Ladder ladder = ladder();
		rung(ladder, "motor", either(no("start"), no("motor")), nc("stop"));
		Cpu cpu = run(List.of(ld(ladder)));
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "start pulls it in");
		cpu.setInputBit(0, 0, false);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "the seal holds it");
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0), "stop drops it");
		cpu.setInputBit(0, 1, false);
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0), "and it stays out");
	}

	@Test
	void everyElementHasItsPowerToRead() {
		Ladder ladder = ladder();
		rung(ladder, "motor", either(no("start"), no("motor")), nc("stop"));
		ladder.number();
		Cpu cpu = run(List.of(ld(ladder)));
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		Ladder.Parallel branch = (Ladder.Parallel) ladder.rungs.getFirst().body.items.get(0);
		Ladder.Node stop = ladder.rungs.getFirst().body.items.get(1);
		assertEquals(true, cpu.read("MAIN." + Ladder.powerVar(branch.items.get(0).id())), "start's contact passes power");
		assertEquals(false, cpu.read("MAIN." + Ladder.powerVar(branch.items.get(1).id())), "the seal does not");
		assertEquals(true, cpu.read("MAIN." + Ladder.powerVar(branch.id())), "the branch does");
		assertEquals(false, cpu.read("MAIN." + Ladder.powerVar(stop.id())), "the pressed stop breaks it");
	}

	@Test
	void setResetAndNegatedCoils() {
		Ladder ladder = ladder();
		rung(ladder, null, no("start")).coils.add(new Ladder.Coil(Ladder.CoilKind.SET, "other"));
		rung(ladder, null, no("stop")).coils.add(new Ladder.Coil(Ladder.CoilKind.RESET, "other"));
		rung(ladder, null, no("other")).coils.add(new Ladder.Coil(Ladder.CoilKind.NOT, "motor"));
		Cpu cpu = run(List.of(ld(ladder)));
		cpu.scan();
		assertEquals(false, cpu.read("MAIN.OTHER"));
		assertTrue(cpu.outputBit(0, 0), "a negated coil is on with no power");
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		cpu.setInputBit(0, 0, false);
		cpu.scan();
		assertEquals(true, cpu.read("MAIN.OTHER"), "set holds");
		assertFalse(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertEquals(false, cpu.read("MAIN.OTHER"), "reset drops");
	}

	@Test
	void aTimerBoxInARung() {
		Ladder ladder = ladder();
		Ladder.Box timer = new Ladder.Box("TON", "t1");
		timer.pins.put("PT", "T#200ms");
		rung(ladder, "late", no("start"), timer);
		Cpu cpu = run(List.of(ld(ladder)));
		cpu.setInputBit(0, 0, true);
		for (int i = 0; i < 3; i++) {
			cpu.scan();
		}
		assertEquals(false, cpu.read("MAIN.LATE"));
		for (int i = 0; i < 3; i++) {
			cpu.scan();
		}
		assertEquals(true, cpu.read("MAIN.LATE"), "the timer's Q carries the power on");
		assertEquals(200L, cpu.read("MAIN.T1.ET"), "and its instance declares itself");
	}

	@Test
	void compareAndMathBoxes() {
		Ladder ladder = ladder();
		Ladder.Box compare = new Ladder.Box("GT", "");
		compare.pins.put("IN1", "x");
		compare.pins.put("IN2", "5");
		rung(ladder, "other", compare);
		Ladder.Box add = new Ladder.Box("ADD", "");
		add.pins.put("IN1", "x");
		add.pins.put("IN2", "1");
		add.pins.put("OUT", "y");
		rung(ladder, null, no("start"), add);
		Cpu cpu = run(List.of(ld(ladder)));
		cpu.scan();
		assertEquals(true, cpu.read("MAIN.OTHER"), "7 > 5");
		assertEquals(0L, cpu.read("MAIN.Y"), "no power, no add");
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertEquals(8L, cpu.read("MAIN.Y"));
	}

	@Test
	void theSavedFormRoundTrips() {
		Ladder ladder = ladder();
		Ladder.Box timer = new Ladder.Box("TON", "t1");
		timer.pins.put("PT", "T#2s");
		Ladder.Rung rung = rung(ladder, "motor", either(no("start"), nc("say \"hi\"")), timer);
		rung.comment = "the first rung";
		Ladder back = Ladder.parse(ladder.write());
		assertEquals(ladder.write(), back.write());
		assertEquals("the first rung", back.rungs.getFirst().comment);
		assertEquals("T#2s", ((Ladder.Box) back.rungs.getFirst().body.items.get(1)).pins.get("PT"));
	}

	private static Fbd fbd() {
		Fbd fbd = new Fbd();
		fbd.declarations = VARS;
		return fbd;
	}

	private static Fbd.Block add(Fbd fbd, String type, String name, int x, int y) {
		Fbd.Block block = new Fbd.Block(fbd.nextId(), type, name, x, y);
		fbd.blocks.add(block);
		return block;
	}

	private static Source fb(Fbd fbd) {
		return new Source(fbd.name, Language.FBD, fbd.write());
	}

	@Test
	void theSameSealInAsAFunctionBlockDiagram() {
		Fbd fbd = fbd();
		Fbd.Block start = add(fbd, Fbd.IN, "start", 0, 0);
		Fbd.Block seal = add(fbd, Fbd.IN, "motor", 0, 20);
		Fbd.Block stop = add(fbd, Fbd.IN, "stop", 0, 40);
		Fbd.Block or = add(fbd, "OR", "", 40, 0);
		or.inputs.put("IN1", Fbd.wire(start.id, "OUT"));
		or.inputs.put("IN2", Fbd.wire(seal.id, "OUT"));
		Fbd.Block not = add(fbd, "NOT", "", 40, 40);
		not.inputs.put("IN", Fbd.wire(stop.id, "OUT"));
		Fbd.Block and = add(fbd, "AND", "", 80, 20);
		and.inputs.put("IN1", Fbd.wire(or.id, "OUT"));
		and.inputs.put("IN2", Fbd.wire(not.id, "OUT"));
		Fbd.Block coil = add(fbd, Fbd.OUT, "motor", 120, 20);
		coil.inputs.put("IN", Fbd.wire(and.id, "OUT"));
		Cpu cpu = run(List.of(fb(fbd)));
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 0, false);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "the seal holds it");
		assertEquals(true, cpu.read("MAIN." + Fbd.resultVar(and.id)), "a function's result can be read");
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0));
		assertEquals(fbd.write(), Fbd.parse(fbd.write()).write());
	}

	@Test
	void anFbdPlacesBlocksAndTakesConstantsOnPins() {
		Fbd fbd = fbd();
		Fbd.Block start = add(fbd, Fbd.IN, "start", 0, 0);
		Fbd.Block timer = add(fbd, "TON", "t2", 40, 0);
		timer.inputs.put("IN", Fbd.wire(start.id, "OUT"));
		timer.inputs.put("PT", "T#100ms");
		Fbd.Block sum = add(fbd, "ADD", "", 40, 40);
		sum.inputs.put("IN1", "x");
		sum.inputs.put("IN2", "2");
		Fbd.Block result = add(fbd, Fbd.OUT, "y", 80, 40);
		result.inputs.put("IN", Fbd.wire(sum.id, "OUT"));
		Fbd.Block late = add(fbd, Fbd.OUT, "late", 80, 0);
		late.inputs.put("IN", Fbd.wire(timer.id, "Q"));
		Cpu cpu = run(List.of(fb(fbd)));
		cpu.setInputBit(0, 0, true);
		for (int i = 0; i < 4; i++) {
			cpu.scan();
		}
		assertEquals(9L, cpu.read("MAIN.Y"));
		assertEquals(true, cpu.read("MAIN.LATE"));
	}

	@Test
	void anFbdRunsInWiringOrderWhateverItsLayout() {
		Fbd fbd = fbd();
		// drawn right to left: the wiring still decides
		Fbd.Block result = add(fbd, Fbd.OUT, "y", 0, 0);
		Fbd.Block second = add(fbd, "ADD", "", 40, 0);
		Fbd.Block first = add(fbd, "ADD", "", 80, 0);
		first.inputs.put("IN1", "x");
		first.inputs.put("IN2", "1");
		second.inputs.put("IN1", Fbd.wire(first.id, "OUT"));
		second.inputs.put("IN2", "1");
		result.inputs.put("IN", Fbd.wire(second.id, "OUT"));
		Cpu cpu = run(List.of(fb(fbd)));
		cpu.scan();
		assertEquals(9L, cpu.read("MAIN.Y"), "in one scan, not three");
	}

	@Test
	void aFeedbackLoopReadsLastScan() {
		Fbd fbd = fbd();
		Fbd.Block counter = add(fbd, "ADD", "", 0, 0);
		counter.inputs.put("IN1", Fbd.wire(counter.id, "OUT"));
		counter.inputs.put("IN2", "1");
		Fbd.Block out = add(fbd, Fbd.OUT, "n", 40, 0);
		out.inputs.put("IN", Fbd.wire(counter.id, "OUT"));
		// a real function's result type must come from somewhere: a loop on itself has none, so its first input is typed by a constant's wire
		out.inputs.put("IN", Fbd.wire(counter.id, "OUT"));
		PlcCompiler.Result result = PlcCompiler.compile(CpuSpec.SMALL, List.of(fb(fbd)));
		assertFalse(result.ok(), "a result whose type depends only on itself cannot be typed");
	}

	@Test
	void aFeedbackLoopWithATypeWorks() {
		Fbd fbd = fbd();
		Fbd.Block seed = add(fbd, Fbd.IN, "n", 0, 0);
		Fbd.Block plus = add(fbd, "ADD", "", 40, 0);
		plus.inputs.put("IN1", Fbd.wire(seed.id, "OUT"));
		plus.inputs.put("IN2", "1");
		Fbd.Block out = add(fbd, Fbd.OUT, "n", 80, 0);
		out.inputs.put("IN", Fbd.wire(plus.id, "OUT"));
		Cpu cpu = run(List.of(fb(fbd)));
		for (int i = 0; i < 5; i++) {
			cpu.scan();
		}
		assertEquals(5L, cpu.read("MAIN.N"));
	}

	/** The point of the standard: one project, its languages calling each other. */
	@Test
	void theLanguagesCallEachOther() {
		String st = """
				FUNCTION TWICE : INT
				VAR_INPUT v : INT; END_VAR
				TWICE := v * 2;
				END_FUNCTION

				FUNCTION_BLOCK SEAL
				VAR_INPUT start, stop : BOOL; END_VAR
				VAR_OUTPUT q : BOOL; END_VAR
				q := (start OR q) AND NOT stop;
				END_FUNCTION_BLOCK
				""";
		Ladder inner = new Ladder();
		inner.kind = "FUNCTION_BLOCK";
		inner.name = "DEBOUNCE";
		inner.declarations = "VAR_INPUT IN : BOOL; END_VAR\nVAR_OUTPUT Q : BOOL; END_VAR";
		Ladder.Box timer = new Ladder.Box("TON", "t");
		timer.pins.put("PT", "T#100ms");
		rung(inner, "Q", no("IN"), timer);
		Ladder main = ladder();
		Ladder.Box debounce = new Ladder.Box("DEBOUNCE", "d");
		Ladder.Box seal = new Ladder.Box("SEAL", "s");
		seal.pins.put("stop", "stop");
		rung(main, "motor", no("start"), debounce, seal);
		Ladder.Box twice = new Ladder.Box("TWICE", "");
		twice.pins.put("v", "x");
		twice.pins.put("OUT", "y");
		rung(main, null, twice);
		Cpu cpu = run(List.of(new Source("LIB", Language.ST, st), new Source("DEBOUNCE", Language.LD, inner.write()), new Source("MAIN", Language.LD, main.write())));
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertEquals(14L, cpu.read("MAIN.Y"), "ladder calling a structured text function");
		assertFalse(cpu.outputBit(0, 0));
		for (int i = 0; i < 3; i++) {
			cpu.scan();
		}
		assertTrue(cpu.outputBit(0, 0), "ladder calling ladder calling structured text");
		cpu.setInputBit(0, 0, false);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "the structured text block's seal holds it");
	}

	private static void assertError(List<Source> sources, String source, int line, String part) {
		List<CompileError> errors = PlcCompiler.compile(CpuSpec.SMALL, sources).errors();
		assertTrue(errors.stream().anyMatch(e -> e.source().equals(source) && e.line() == line && e.message().contains(part)), () -> "expected " + source + " line " + line + " '" + part + "', got " + errors);
	}

	@Test
	void errorsNameTheUnitAndRung() {
		Ladder bad = ladder();
		bad.name = "SECOND";
		rung(bad, "motor", no("start"));
		rung(bad, "motor", no("nonsense"));
		rung(bad, "motor", no(""));
		Source good = new Source("FIRST", Language.ST, "PROGRAM FIRST VAR a : INT; END_VAR a := 1; END_PROGRAM");
		List<Source> sources = new ArrayList<>(List.of(good, new Source("SECOND", Language.LD, bad.write())));
		assertError(sources, "SECOND", 2, "NONSENSE is not declared");
		assertError(sources, "SECOND", 3, "nothing on it");
		Ladder.Box noName = new Ladder.Box("TON", "");
		Ladder ladder = ladder();
		rung(ladder, null, noName);
		assertError(List.of(ld(ladder)), "", 1, "instance name");
		Ladder wrong = ladder();
		rung(wrong, null, new Ladder.Box("NOPE", "z"));
		assertError(List.of(ld(wrong)), "", 1, "no block NOPE");
		Ladder vars = new Ladder();
		vars.declarations = "VAR a : NOPE; END_VAR";
		assertError(List.of(ld(vars)), "", 2, "no type NOPE");
	}

	@Test
	void aDamagedDiagramIsAnErrorNotACrash() {
		List<CompileError> errors = PlcCompiler.compile(CpuSpec.SMALL, List.of(new Source("X", Language.LD, "PROGRAM X\nVAR\nEND_VAR\n---\n(this is not saved form"))).errors();
		assertFalse(errors.isEmpty());
		assertTrue(errors.getFirst().message().contains("damaged"), errors::toString);
	}

	// ---- jumps, labels and return ------------------------------------------------------------------------------------------------------

	private static Ladder.Coil jump(String label) {
		return new Ladder.Coil(Ladder.CoilKind.JUMP, label);
	}

	private static Ladder.Coil ret() {
		return new Ladder.Coil(Ladder.CoilKind.RETURN, "");
	}

	/** Rung 1 jumps over rung 2 to the rung labelled OVER while start is made. */
	private static Ladder jumpOver() {
		Ladder ladder = ladder();
		rung(ladder, null, no("start")).coils.add(jump("over"));
		rung(ladder, "motor", no("stop"));
		rung(ladder, "late", no("stop")).label = "OVER";
		return ladder;
	}

	@Test
	void aJumpSkipsTheRungsUpToItsLabelWhileItHasPower() {
		Ladder ladder = jumpOver();
		Cpu cpu = run(List.of(ld(ladder)));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "no jump: rung 2 runs");
		assertEquals(true, cpu.read("MAIN.LATE"), "and so does the labelled rung");
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, false);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "a skipped rung's coil keeps what it had");
		assertEquals(false, cpu.read("MAIN.LATE"), "the labelled rung runs: the jump lands on it");
		ladder.number();
		Ladder.Node skippedContact = ladder.rungs.get(1).body.items.getFirst();
		Ladder.Node landed = ladder.rungs.get(2).body.items.getFirst();
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertEquals(false, cpu.read("MAIN." + Ladder.powerVar(skippedContact.id())), "a skipped rung shows no power, whatever its contacts read");
		assertEquals(false, cpu.read("MAIN." + Ladder.powerVar(ladder.rungs.get(1).body.id())));
		assertEquals(true, cpu.read("MAIN." + Ladder.powerVar(landed.id())), "the labelled rung shows its own");
		assertTrue(cpu.outputBit(0, 0), "still held");
		cpu.setInputBit(0, 0, false);
		cpu.setInputBit(0, 1, false);
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0), "the jump released: rung 2 runs again");
	}

	@Test
	void aSkippedTimerStopsWhereItWas() {
		Ladder ladder = ladder();
		rung(ladder, null, no("start")).coils.add(jump("END_TIMING"));
		Ladder.Box timer = new Ladder.Box("TON", "t1");
		timer.pins.put("PT", "T#500ms");
		rung(ladder, "late", no("stop"), timer);
		rung(ladder, "other", no("stop")).label = "end_timing";
		Cpu cpu = run(List.of(ld(ladder)));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		cpu.setInputBit(0, 0, true);
		for (int i = 0; i < 20; i++) {
			cpu.scan();
		}
		assertEquals(false, cpu.read("MAIN.LATE"), "the timer's rung is skipped, so its coil holds");
		assertEquals(true, cpu.read("MAIN.OTHER"), "a label is matched whatever its case");
	}

	@Test
	void overlappingJumpsEachSkipTheirOwnRungs() {
		Ladder ladder = ladder();
		rung(ladder, null, no("start")).coils.add(jump("A"));
		rung(ladder, null, no("stop")).coils.add(jump("B"));
		rung(ladder, "motor", no("stop"));
		rung(ladder, "late", no("stop")).label = "A";
		rung(ladder, "other", no("stop")).label = "B";
		Cpu cpu = run(List.of(ld(ladder)));
		cpu.scan();
		assertEquals(false, cpu.read("MAIN.LATE"));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0), "rung 2's jump to B passes over rung 3");
		assertEquals(false, cpu.read("MAIN.LATE"), "and over A's rung, which is before B");
		assertEquals(true, cpu.read("MAIN.OTHER"), "B's rung runs");
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertEquals(true, cpu.read("MAIN.LATE"), "rung 1's jump to A passes over rung 2, so B's jump is not taken and A's rung runs");
		assertFalse(cpu.outputBit(0, 0), "rung 3 is still passed over");
	}

	@Test
	void aReturnEndsTheUnit() {
		Ladder ladder = ladder();
		rung(ladder, "other", no("start"));
		rung(ladder, null, no("start")).coils.add(ret());
		rung(ladder, "motor", no("stop"));
		Cpu cpu = run(List.of(ld(ladder)));
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertEquals(true, cpu.read("MAIN.OTHER"), "the coil before the return ran");
		assertFalse(cpu.outputBit(0, 0), "the rung after it did not");
		ladder.number();
		assertEquals(false, cpu.read("MAIN." + Ladder.powerVar(ladder.rungs.get(2).body.items.getFirst().id())), "and shows no power");
		cpu.setInputBit(0, 0, false);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "no return: it runs");
	}

	@Test
	void aReturnInALadderFunctionBlockReturnsToItsCaller() {
		Ladder inner = new Ladder();
		inner.kind = "FUNCTION_BLOCK";
		inner.name = "GUARD";
		inner.declarations = "VAR_INPUT trip, run : BOOL; END_VAR\nVAR_OUTPUT q : BOOL; END_VAR";
		rung(inner, null, no("trip")).coils.add(ret());
		rung(inner, "q", no("run"));
		String main = "PROGRAM MAIN VAR g : GUARD; t AT %IX0.0 : BOOL; r AT %IX0.1 : BOOL; q AT %QX0.0 : BOOL; after : INT; END_VAR g(trip := t, run := r, q => q); after := after + 1; END_PROGRAM";
		Cpu cpu = run(List.of(new Source("GUARD", Language.LD, inner.write()), new Source("MAIN", Language.ST, main)));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0));
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, false);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "returned before q's rung: q keeps its value");
		assertEquals(2L, ((Number) cpu.read("MAIN.AFTER")).longValue(), "the caller carried on");
	}

	@Test
	void jumpErrorsNameTheUnitAndRung() {
		Ladder missing = ladder();
		missing.name = "SECOND";
		rung(missing, "motor", no("start"));
		rung(missing, null, no("start")).coils.add(jump("NOWHERE"));
		Source first = new Source("FIRST", Language.ST, "PROGRAM FIRST VAR a : INT; END_VAR a := 1; END_PROGRAM");
		assertError(List.of(first, new Source("SECOND", Language.LD, missing.write())), "SECOND", 2, "rung 2 jumps to NOWHERE, but no rung of SECOND has that label");
		Ladder twice = ladder();
		rung(twice, "motor", no("start")).label = "here";
		rung(twice, "other", no("start")).label = "HERE";
		assertError(List.of(ld(twice)), "", 2, "rung 2's label HERE is already on rung 1");
		Ladder back = ladder();
		rung(back, "motor", no("start")).label = "TOP";
		rung(back, null, no("start")).coils.add(jump("TOP"));
		assertError(List.of(ld(back)), "", 2, "rung 2 jumps back to TOP on rung 1");
		Ladder self = ladder();
		Ladder.Rung loop = rung(self, null, no("start"));
		loop.label = "SPIN";
		loop.coils.add(jump("SPIN"));
		assertError(List.of(ld(self)), "", 1, "jumps back to SPIN");
		Ladder blank = ladder();
		rung(blank, null, no("start")).coils.add(jump(""));
		assertError(List.of(ld(blank)), "", 1, "no label to go to");
		Ladder bad = ladder();
		rung(bad, "motor", no("start")).label = "2ND";
		assertError(List.of(ld(bad)), "", 1, "is not a name");
	}

	@Test
	void labelsJumpsAndReturnsAreSaved() {
		Ladder ladder = jumpOver();
		rung(ladder, null, no("start")).coils.add(ret());
		String saved = ladder.write();
		Ladder read = Ladder.parse(saved);
		assertEquals(saved, read.write());
		assertEquals("OVER", read.rungs.get(2).label);
		assertEquals(Ladder.CoilKind.JUMP, read.rungs.get(0).coils.getFirst().kind);
		assertEquals("over", read.rungs.get(0).coils.getFirst().var);
		assertEquals(Ladder.CoilKind.RETURN, read.rungs.get(3).coils.getFirst().kind);
		// a ladder saved before labels: four items a rung, and it reads as it did
		Ladder old = Ladder.parse("PROGRAM MAIN\n" + VARS + "\n---\n(\"R\" \"a rung\" (\"S\" (\"NO\" \"start\")) (\"C\" (\"COIL\" \"motor\")))\n");
		assertEquals("", old.rungs.getFirst().label);
		assertEquals("a rung", old.rungs.getFirst().comment);
		assertTrue(old.write().endsWith("(\"C\" (\"COIL\" \"motor\")))\n"), "no label is written for a rung without one: " + old.write());
	}

	// ---- a ladder or FBD as a FUNCTION --------------------------------------------------------------------------------------------------

	@Test
	void aLadderFunctionIsCalledFromEveryLanguage() {
		Ladder either = new Ladder();
		either.kind = "FUNCTION";
		either.name = "EITHER";
		either.returns = "BOOL";
		either.declarations = "VAR_INPUT a, b : BOOL; END_VAR";
		rung(either, "EITHER", either(no("a"), no("b")));
		assertEquals("FUNCTION EITHER : BOOL", either.write().lines().findFirst().orElseThrow());
		Ladder read = Ladder.parse(either.write());
		assertEquals("FUNCTION", read.kind);
		assertEquals("BOOL", read.returns);
		// from structured text
		String st = "PROGRAM ST_SIDE VAR s AT %IX0.0 : BOOL; t AT %IX0.1 : BOOL; q AT %QX0.0 : BOOL; END_VAR q := EITHER(s, t); END_PROGRAM";
		// from a ladder box
		Ladder main = ladder();
		Ladder.Box box = new Ladder.Box("EITHER", "");
		box.pins.put("a", "start");
		box.pins.put("b", "stop");
		box.pins.put("OUT", "other");
		rung(main, null, box);
		// from an FBD block
		Fbd fbd = fbd();
		fbd.name = "DIAGRAM";
		Fbd.Block call = add(fbd, "EITHER", "", 40, 0);
		call.inputs.put("a", "%IX0.0");
		call.inputs.put("b", "FALSE");
		Fbd.Block out = add(fbd, Fbd.OUT, "late", 80, 0);
		out.inputs.put("IN", Fbd.wire(call.id, "OUT"));
		Cpu cpu = run(List.of(new Source("EITHER", Language.LD, either.write()), new Source("ST_SIDE", Language.ST, st), ld(main), fb(fbd)));
		cpu.scan();
		assertFalse(cpu.outputBit(0, 0));
		assertEquals(false, cpu.read("MAIN.OTHER"));
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 0), "structured text calls the ladder function");
		assertEquals(true, cpu.read("MAIN.OTHER"), "a ladder box calls it");
		assertEquals(false, cpu.read("DIAGRAM.LATE"));
		cpu.setInputBit(0, 0, true);
		cpu.scan();
		assertEquals(true, cpu.read("DIAGRAM.LATE"), "an FBD block calls it");
	}

	@Test
	void anFbdFunctionSetsItsResultWithAnOutVariable() {
		Fbd scale = new Fbd();
		scale.kind = "FUNCTION";
		scale.name = "TRIPLE";
		scale.returns = "INT";
		scale.declarations = "VAR_INPUT v : INT; END_VAR";
		Fbd.Block v = add(scale, Fbd.IN, "v", 0, 0);
		Fbd.Block mul = add(scale, "MUL", "", 40, 0);
		mul.inputs.put("IN1", Fbd.wire(v.id, "OUT"));
		mul.inputs.put("IN2", "3");
		Fbd.Block result = add(scale, Fbd.OUT, "TRIPLE", 80, 0);
		result.inputs.put("IN", Fbd.wire(mul.id, "OUT"));
		assertEquals(scale.write(), Fbd.parse(scale.write()).write());
		String st = "PROGRAM MAIN VAR y : INT; END_VAR y := TRIPLE(5) + 1; END_PROGRAM";
		Cpu cpu = run(List.of(new Source("TRIPLE", Language.FBD, scale.write()), new Source("MAIN", Language.ST, st)));
		cpu.scan();
		assertEquals(16L, ((Number) cpu.read("MAIN.Y")).longValue());
	}

	@Test
	void aFunctionCannotHoldAFunctionBlock() {
		Ladder ladder = new Ladder();
		ladder.kind = "FUNCTION";
		ladder.name = "LATE";
		ladder.returns = "BOOL";
		ladder.declarations = "VAR_INPUT a : BOOL; END_VAR";
		Ladder.Box timer = new Ladder.Box("TON", "t1");
		timer.pins.put("PT", "T#1s");
		rung(ladder, "LATE", no("a"), timer);
		assertError(List.of(new Source("LATE", Language.LD, ladder.write())), "", 1, "the function LATE cannot hold a TON");
		Fbd fbd = new Fbd();
		fbd.kind = "FUNCTION";
		fbd.name = "EDGE";
		fbd.returns = "BOOL";
		fbd.declarations = "VAR_INPUT a : BOOL; END_VAR";
		Fbd.Block trig = add(fbd, "R_TRIG", "r1", 40, 0);
		trig.inputs.put("CLK", "a");
		assertError(List.of(fb(fbd)), "", trig.id, "the function EDGE cannot hold a R_TRIG");
		Ladder noType = new Ladder();
		noType.kind = "FUNCTION";
		noType.name = "F";
		assertError(List.of(new Source("F", Language.LD, noType.write())), "F", 1, "needs a result type");
	}

	// ---- a coil on a located output, from another unit --------------------------------------------------------------------------------

	@Test
	void aCoilReachesAnOutputFromAnyUnitTheStandardsWay() {
		String globals = "VAR_GLOBAL pump AT %QX0.2 : BOOL; horn AT %QX0.3 : BOOL; END_VAR\nPROGRAM MAIN VAR s : STARTER; go AT %IX0.0 : BOOL; END_VAR s(run := go); END_PROGRAM";
		// a function block ladder: a direct address, and a global through VAR_EXTERNAL
		Ladder starter = new Ladder();
		starter.kind = "FUNCTION_BLOCK";
		starter.name = "STARTER";
		starter.declarations = "VAR_INPUT run : BOOL; END_VAR\nVAR_EXTERNAL pump : BOOL; END_VAR";
		rung(starter, "%QX0.1", no("run"));
		rung(starter, "pump", no("run"));
		// a second program, in ladder, through VAR_EXTERNAL
		Ladder second = new Ladder();
		second.name = "ALARMS";
		second.declarations = "VAR_EXTERNAL horn : BOOL; END_VAR\nVAR alarm AT %IX0.1 : BOOL; END_VAR";
		rung(second, "horn", no("alarm"));
		Cpu cpu = run(List.of(new Source("MAIN", Language.ST, globals), new Source("STARTER", Language.LD, starter.write()), new Source("ALARMS", Language.LD, second.write())));
		cpu.scan();
		assertFalse(cpu.outputBit(0, 1));
		assertFalse(cpu.outputBit(0, 2));
		cpu.setInputBit(0, 0, true);
		cpu.setInputBit(0, 1, true);
		cpu.scan();
		assertTrue(cpu.outputBit(0, 1), "a coil on a direct address in a function block");
		assertTrue(cpu.outputBit(0, 2), "a coil on a located global, from a function block");
		assertTrue(cpu.outputBit(0, 3), "a coil on a located global, from a second program");
	}

	@Test
	void anotherProgramsVariableIsRefusedWithTheWayToDoIt() {
		Ladder second = new Ladder();
		second.name = "SECOND";
		second.declarations = "VAR alarm AT %IX0.1 : BOOL; seen : BOOL; END_VAR";
		rung(second, "MAIN.motor", no("alarm"));
		rung(second, "seen", no("MAIN.motor"));
		Ladder main = ladder();
		rung(main, "motor", no("start"));
		List<Source> sources = List.of(ld(main), new Source("SECOND", Language.LD, second.write()));
		assertError(sources, "SECOND", 1, "MAIN is a program, whose variables are its own: declare MOTOR in VAR_GLOBAL (with its AT) and as VAR_EXTERNAL here");
		assertError(sources, "SECOND", 2, "MAIN is a program");
	}

	// ---- FBD wires routed around blocks --------------------------------------------------------------------------------------------------

	/** A wire's path: at right angles throughout, out of its output to the right and into its input from the left, through no block. */
	private static void assertRouted(int[] path, List<WireRouter.Box> boxes, int x0, int y0, int x1, int y1) {
		String shown = java.util.Arrays.toString(path);
		assertEquals(x0, path[0], shown);
		assertEquals(y0, path[1], shown);
		assertEquals(x1, path[path.length - 2], shown);
		assertEquals(y1, path[path.length - 1], shown);
		for (int k = 0; k + 3 < path.length; k += 2) {
			assertTrue(path[k] == path[k + 2] || path[k + 1] == path[k + 3], "a slanted run: " + shown);
		}
		assertTrue(path[2] > x0 && path[3] == y0, "it leaves its output to the right: " + shown);
		assertTrue(path[path.length - 4] < x1 && path[path.length - 3] == y1, "it comes into its input from the left: " + shown);
		assertTrue(WireRouter.clear(path, boxes), "it runs through a block: " + shown);
	}

	@Test
	void aWireWithNothingInTheWayGoesStraightAcross() {
		List<WireRouter.Box> boxes = List.of(new WireRouter.Box(0, 0, 76, 40), new WireRouter.Box(200, 0, 76, 40));
		int[] path = WireRouter.route(boxes, 76, 20, 200, 20);
		assertRouted(path, boxes, 76, 20, 200, 20);
		assertEquals(4, path.length, "one straight run: " + java.util.Arrays.toString(path));
		int[] step = WireRouter.route(boxes, 76, 20, 200, 30);
		assertRouted(step, boxes, 76, 20, 200, 30);
		assertEquals(8, step.length, "one step down, two bends: " + java.util.Arrays.toString(step));
	}

	@Test
	void aWireGoesAroundABlockInItsWay() {
		List<WireRouter.Box> boxes = List.of(new WireRouter.Box(0, 0, 76, 40), new WireRouter.Box(110, 0, 76, 60), new WireRouter.Box(220, 0, 76, 40));
		int[] path = WireRouter.route(boxes, 76, 20, 220, 20);
		assertRouted(path, boxes, 76, 20, 220, 20);
		// a block straddling the gap between two blocks below it: the vertical run has to find the gap's side
		List<WireRouter.Box> crowd = List.of(new WireRouter.Box(0, 100, 76, 40), new WireRouter.Box(90, 0, 120, 80), new WireRouter.Box(90, 90, 40, 80), new WireRouter.Box(240, 10, 76, 30));
		int[] up = WireRouter.route(crowd, 76, 120, 240, 25);
		assertRouted(up, crowd, 76, 120, 240, 25);
	}

	@Test
	void aFeedbackWireGoesBackAroundItsBlocks() {
		List<WireRouter.Box> boxes = List.of(new WireRouter.Box(100, 50, 76, 40), new WireRouter.Box(0, 50, 76, 40));
		int[] self = WireRouter.route(boxes, 176, 70, 100, 60);
		assertRouted(self, boxes, 176, 70, 100, 60);
		int[] back = WireRouter.route(boxes, 176, 70, 0, 70);
		assertRouted(back, boxes, 176, 70, 0, 70);
	}

	@Test
	void aBigSheetRoutesEveryWireQuickly() {
		List<WireRouter.Box> boxes = new ArrayList<>();
		for (int col = 0; col < 6; col++) {
			for (int row = 0; row < 7; row++) {
				boxes.add(new WireRouter.Box(col * 120 + (row % 2) * 20, row * 60, 76, 40));
			}
		}
		long began = System.nanoTime();
		java.util.Random random = new java.util.Random(7);
		for (int n = 0; n < 60; n++) {
			WireRouter.Box from = boxes.get(random.nextInt(boxes.size()));
			WireRouter.Box to = boxes.get(random.nextInt(boxes.size()));
			int x0 = from.x() + from.w();
			int y0 = from.y() + 10;
			int y1 = to.y() + 25;
			assertRouted(WireRouter.route(boxes, x0, y0, to.x(), y1), boxes, x0, y0, to.x(), y1);
		}
		long ms = (System.nanoTime() - began) / 1_000_000;
		assertTrue(ms < 3000, "sixty wires on a 42-block sheet took " + ms + " ms");
	}

	@Test
	void structuredTextProjectsStillCompile() {
		assertTrue(PlcCompiler.compile(CpuSpec.SMALL, Project.of("PROGRAM MAIN END_PROGRAM")).ok());
	}
}
