package dev.wildcatter.fluid;

import java.util.Map;
import java.util.TreeMap;

/**
 * What a plant part holds in process for its residence time (docs/questions/plant-crane-gaps.md #11; the world rule
 * {@code realResidenceTimes}): a first-in first-out of time buckets. Feed goes in with the time it comes due; product comes out of the
 * buckets that are due, oldest first. The buckets are {@code width} seconds wide (a minute for the digester, half an hour for the
 * precipitators and the ovens), so a part saves a few dozen numbers, not one per interval. Plain Java, so it is unit-tested alone.
 */
public final class Holdup {
	private final double width;
	private final TreeMap<Long, Double> buckets = new TreeMap<>();

	public Holdup(double widthSeconds) {
		this.width = widthSeconds;
	}

	/** {@code amount} in at time {@code now} (s), due {@code hold} seconds later (rounded up to its bucket). */
	public void add(double now, double hold, double amount) {
		if (amount <= 0) {
			return;
		}
		buckets.merge((long) Math.ceil((now + hold) / width), amount, Double::sum);
	}

	/** What is due at {@code now}. */
	public double due(double now) {
		double sum = 0;
		for (Map.Entry<Long, Double> e : buckets.entrySet()) {
			if (e.getKey() * width > now) {
				break;
			}
			sum += e.getValue();
		}
		return sum;
	}

	/** Takes up to {@code max} of what is due at {@code now}, oldest first; returns what it took. */
	public double take(double now, double max) {
		double got = 0;
		while (got < max && !buckets.isEmpty()) {
			Map.Entry<Long, Double> first = buckets.firstEntry();
			if (first.getKey() * width > now) {
				break;
			}
			double t = Math.min(first.getValue(), max - got);
			got += t;
			if (first.getValue() - t <= 1e-9) {
				buckets.pollFirstEntry();
			} else {
				buckets.put(first.getKey(), first.getValue() - t);
			}
		}
		return got;
	}

	/** Takes up to {@code max} whether it is due or not (the rule turned off: what was in process comes out). */
	public double takeAny(double max) {
		return take(Double.MAX_VALUE, max);
	}

	/** Everything in process. */
	public double inProcess() {
		double sum = 0;
		for (double v : buckets.values()) {
			sum += v;
		}
		return sum;
	}

	public boolean isEmpty() {
		return buckets.isEmpty();
	}

	/** Seconds from {@code now} until the next bucket comes due (0 if one is due), or -1 with nothing in process. */
	public double untilNext(double now) {
		return buckets.isEmpty() ? -1 : Math.max(0, buckets.firstKey() * width - now);
	}

	/** Game test hook: as if {@code seconds} had passed for what is in process. */
	public void age(double seconds) {
		long shift = (long) Math.ceil(seconds / width);
		TreeMap<Long, Double> moved = new TreeMap<>();
		buckets.forEach((k, v) -> moved.merge(k - shift, v, Double::sum));
		buckets.clear();
		buckets.putAll(moved);
	}

	/** "k:v;k:v", for a block entity's save. */
	public String save() {
		StringBuilder out = new StringBuilder();
		buckets.forEach((k, v) -> out.append(out.isEmpty() ? "" : ";").append(k).append(':').append(v));
		return out.toString();
	}

	public void load(String saved) {
		buckets.clear();
		if (saved == null || saved.isBlank()) {
			return;
		}
		for (String pair : saved.split(";")) {
			String[] kv = pair.split(":");
			try {
				buckets.put(Long.parseLong(kv[0]), Double.parseDouble(kv[1]));
			} catch (RuntimeException e) {
				// a damaged entry is dropped
			}
		}
	}
}
