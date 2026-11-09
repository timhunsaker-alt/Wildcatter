package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;
import java.util.ArrayList;
import java.util.List;

/**
 * Which parts act on a solve (protection gap wave): every untimed part, as before, and of the timed ones (breakers and fuses that say
 * how soon they would clear) only the fastest, and any within {@link Tuning#COORDINATION_MARGIN_CYCLES} of it. The circuit is then
 * solved again without the fault, so a slower device never sees it: a fuse or breaker nearer the fault clears it alone when it is
 * faster. A current-limiting fuse that wins lets only its peak through: a device whose instant pickup is under that peak's rms
 * equivalent unlatches anyway and trips with it (a let-through that is not selective).
 */
public final class ProtectionRace {
	private ProtectionRace() {
	}

	public static List<CircuitPart> acting(List<CircuitPart> parts, Circuit circuit) {
		double[] times = new double[parts.size()];
		double fastest = Double.POSITIVE_INFINITY;
		for (int k = 0; k < parts.size(); k++) {
			times[k] = parts.get(k).clearingSeconds(circuit);
			if (!Double.isNaN(times[k])) {
				fastest = Math.min(fastest, times[k]);
			}
		}
		double within = fastest + Tuning.COORDINATION_MARGIN_CYCLES / circuit.hertz();
		double peak = 0;
		for (int k = 0; k < parts.size(); k++) {
			if (!Double.isNaN(times[k]) && times[k] <= within) {
				peak = Math.max(peak, parts.get(k).letThroughPeak(circuit));
			}
		}
		List<CircuitPart> acting = new ArrayList<>(parts.size());
		for (int k = 0; k < parts.size(); k++) {
			CircuitPart part = parts.get(k);
			if (Double.isNaN(times[k]) || times[k] <= within
					|| Double.isFinite(times[k]) && peak > 0 && part.instantPickupAmps() <= peak / Math.sqrt(2)) {
				acting.add(part);
			}
		}
		return acting;
	}
}
