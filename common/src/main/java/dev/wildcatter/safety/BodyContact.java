package dev.wildcatter.safety;

import dev.wildcatter.Tuning;
import dev.wildcatter.circuit.CircuitManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * A body across a live conductor and the earth (protection gap wave, docs/questions/protection-gaps.md): when someone is shocked by a
 * part, that part puts the body's resistance from its live conductor to earth into the circuit for the next solves, so the current
 * through the person is real current to ground. A GFCI or RCD upstream sees it as imbalance and trips (UL 943 Class A at 5 mA, an RCD at
 * 30 mA). The body is IEC 60479-1's hand-to-feet impedance at mains voltage, {@link Tuning#BODY_OHMS}.
 */
public final class BodyContact {
	private BodyContact() {
	}

	private static final Map<ResourceKey<Level>, Map<Long, Long>> TOUCHING = new HashMap<>();

	/** Someone is touching the live part at {@code pos} now: its body path stays in the circuit for {@link Tuning#BODY_CONTACT_TICKS}. */
	public static void touched(ServerLevel level, BlockPos pos) {
		long now = level.getGameTime();
		Map<Long, Long> here = TOUCHING.computeIfAbsent(level.dimension(), k -> new HashMap<>());
		here.values().removeIf(until -> until < now);
		Long before = here.put(pos.asLong(), now + Tuning.BODY_CONTACT_TICKS);
		if (before == null) {
			CircuitManager.markDirty(level);   // a new path to earth: solve again
		}
	}

	/** True if a body is across the part at {@code pos}; its part then adds {@link Tuning#BODY_OHMS} from its live node to earth. */
	public static boolean on(Level level, BlockPos pos) {
		Map<Long, Long> here = TOUCHING.get(level.dimension());
		Long until = here == null ? null : here.get(pos.asLong());
		return until != null && until >= level.getGameTime();
	}

	/** Forgets every contact (the server stopping). */
	public static void clearAll() {
		TOUCHING.clear();
	}
}
