import dev.wildcatter.circuit.Solver;

public class SolverCheck {
	static int failures = 0;

	static void near(String what, double got, double want, double tol) {
		boolean ok = Math.abs(got - want) <= tol;
		System.out.printf("%s %-46s got %.6f want %.6f%n", ok ? "ok  " : "FAIL", what, got, want);
		if (!ok) failures++;
	}

	public static void main(String[] a) {
		// 1. Ohm's law: 110 V across a 201.67 ohm lamp (60 W) draws 0.5455 A.
		Solver s = new Solver();
		int hot = s.node();
		s.source(hot, Solver.GROUND, 110);
		int lamp = s.resistor(hot, Solver.GROUND, 110.0 * 110.0 / 60.0);
		s.solve();
		near("lamp current (A)", s.current(lamp), 60.0 / 110.0, 1e-6);
		near("lamp voltage (V)", s.voltage(hot), 110, 1e-6);

		// 2. Series: 12 V across 4 ohm + 8 ohm: 1 A, 4 V then 8 V.
		s = new Solver();
		int n1 = s.node(), n2 = s.node();
		s.source(n1, Solver.GROUND, 12);
		int r1 = s.resistor(n1, n2, 4);
		s.resistor(n2, Solver.GROUND, 8);
		s.solve();
		near("series current (A)", s.current(r1), 1.0, 1e-6);
		near("series middle node (V)", s.voltage(n2), 8.0, 1e-6);

		// 3. Parallel: three 100 ohm loads on 100 V draw 3 A together.
		s = new Solver();
		int h = s.node();
		int src = s.source(h, Solver.GROUND, 100);
		for (int i = 0; i < 3; i++) s.resistor(h, Solver.GROUND, 100);
		s.solve();
		near("parallel source current (A)", s.sourceCurrent(src), 3.0, 1e-6);

		// 4. A source with internal resistance sags under load: 110 V, 0.5 ohm, 20 A load (5.5 ohm) -> 100 V at the load.
		s = new Solver();
		int in = s.node(), out = s.node();
		s.source(in, Solver.GROUND, 110);
		s.resistor(in, out, 0.5);
		int load = s.resistor(out, Solver.GROUND, 5.0);
		s.solve();
		near("sagging load voltage (V)", s.voltage(out), 100.0, 1e-6);
		near("sagging load current (A)", s.current(load), 20.0, 1e-6);

		// 5. A dead short: 110 V through 0.5 ohm of source and 0.01 ohm of wire = 215.7 A.
		s = new Solver();
		in = s.node();
		out = s.node();
		s.source(in, Solver.GROUND, 110);
		s.resistor(in, out, 0.5);
		int shortCircuit = s.resistor(out, Solver.GROUND, 0.01);
		s.solve();
		near("short-circuit current (A)", s.current(shortCircuit), 110.0 / 0.51, 1e-6);

		// 6. Ideal transformer 4160 V : 110 V feeding a 100 ohm load. Secondary 110 V, 1.1 A; primary draws 1.1 / 37.82 A.
		s = new Solver();
		int pHot = s.node(), sHot = s.node();
		int srcP = s.source(pHot, Solver.GROUND, 4160);
		double ratio = 4160.0 / 110.0;
		s.transformer(pHot, Solver.GROUND, sHot, Solver.GROUND, ratio);
		int sLoad = s.resistor(sHot, Solver.GROUND, 100);
		s.solve();
		near("transformer secondary volts", s.voltage(sHot), 110.0, 1e-4);
		near("transformer secondary amps", s.current(sLoad), 1.1, 1e-6);
		near("transformer primary amps", s.sourceCurrent(srcP), 1.1 / ratio, 1e-6);
		near("transformer power in = power out (W)", 4160 * s.sourceCurrent(srcP), 110 * 1.1, 1e-3);

		// 7. A floating circuit with no source does not blow up.
		s = new Solver();
		int f1 = s.node(), f2 = s.node();
		s.resistor(f1, f2, 10);
		near("floating circuit solves", s.solve() ? 1 : 0, 1, 0);

		System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
		System.exit(failures == 0 ? 0 : 1);
	}
}
