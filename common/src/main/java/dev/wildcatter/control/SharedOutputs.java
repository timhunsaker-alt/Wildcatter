package dev.wildcatter.control;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * One device's output point landed on two PLCs, wired to both: what that does in the field (docs/questions/plc-gaps.md). Two relay contacts
 * on one coil are in parallel, so either one closing works it: a discrete output is the OR of every PLC driving it in that scan. Two current
 * sources in one 4-20 mA loop fight, and the loop reads as failed: the device falls back as it does on a lost signal.
 */
public final class SharedOutputs {
	/** After two PLCs have driven one loop, it stays failed this long after either stops. */
	private static final int FIGHT_TICKS = 20;
	private static final long FORGET_TICKS = 200;

	private record Key(ResourceKey<Level> level, BlockPos pos, int point) {
	}

	private static final class Entry {
		long tick = Long.MIN_VALUE;
		BlockPos by;
		boolean value;
		long fightUntil = Long.MIN_VALUE;
	}

	private static final Map<Key, Entry> ENTRIES = new HashMap<>();
	private static long swept;

	private SharedOutputs() {
	}

	private static Entry entry(Level level, BlockPos pos, int point, long now) {
		if (now - swept > FORGET_TICKS || now < swept) {
			swept = now;
			ENTRIES.values().removeIf(e -> now - e.tick > FORGET_TICKS || e.tick > now);
		}
		return ENTRIES.computeIfAbsent(new Key(level.dimension(), pos.immutable(), point), k -> new Entry());
	}

	/** A discrete output from {@code cabinet}: the value the device should see, ORed with any other PLC's in this scan. */
	public static synchronized boolean bool(Level level, BlockPos pos, int point, BlockPos cabinet, boolean value, long now) {
		Entry e = entry(level, pos, point, now);
		if (e.tick == now && e.by != null && !e.by.equals(cabinet)) {
			value |= e.value;
		}
		e.tick = now;
		e.by = cabinet;
		e.value = value;
		return value;
	}

	/** An analog output from {@code cabinet}: its milliamps, or 0 (a failed loop) while two PLCs drive the same loop. */
	public static synchronized double milliamps(Level level, BlockPos pos, int point, BlockPos cabinet, double milliamps, long now) {
		Entry e = entry(level, pos, point, now);
		if (e.tick >= now - 1 && e.by != null && !e.by.equals(cabinet)) {
			e.fightUntil = now + FIGHT_TICKS;
		}
		e.tick = now;
		e.by = cabinet;
		return now < e.fightUntil ? 0 : milliamps;
	}

	/** True while two PLCs are driving this loop. */
	public static synchronized boolean fighting(Level level, BlockPos pos, int point, long now) {
		Entry e = ENTRIES.get(new Key(level.dimension(), pos, point));
		return e != null && now < e.fightUntil;
	}
}
