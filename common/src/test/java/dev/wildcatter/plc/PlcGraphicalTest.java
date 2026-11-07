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

	@Test
	void structuredTextProjectsStillCompile() {
		assertTrue(PlcCompiler.compile(CpuSpec.SMALL, Project.of("PROGRAM MAIN END_PROGRAM")).ok());
	}
}
