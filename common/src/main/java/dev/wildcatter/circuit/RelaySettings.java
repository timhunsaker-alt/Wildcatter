package dev.wildcatter.circuit;

import dev.wildcatter.Tuning;

/**
 * The settings of one overcurrent relay (a feeder relay of the SEL-751 or SEL-351 class): its 51 pickup, its inverse-time curve and
 * time dial, and its 50 instantaneous multiple, with a cursor for setting them one at a time from the relay's front panel (the Power
 * Meter: right-click on the relay door moves to the next setting, sneak and right-click steps the one selected). Plain Java, so it is
 * unit-tested without Minecraft. Saved as small integers by the block entity that owns it.
 */
public final class RelaySettings {
	/** The IEEE C37.112 inverse-time curves: {@code t = TD x (A / (M^p - 1) + B)}. */
	public enum Curve {
		MI("moderately inverse", 0.0515, 0.114, 0.02),
		VI("very inverse", 19.61, 0.491, 2.0),
		EI("extremely inverse", 28.2, 0.1217, 2.0);

		public final String label;
		private final double a;
		private final double b;
		private final double p;

		Curve(String label, double a, double b, double p) {
			this.label = label;
			this.a = a;
			this.b = b;
			this.p = p;
		}

		/** Seconds to trip at {@code m} times the pickup and time dial {@code dial}; infinite at or below the pickup. */
		public double seconds(double m, double dial) {
			return m <= 1.0 ? Double.POSITIVE_INFINITY : dial * (a / (Math.pow(m, p) - 1.0) + b);
		}
	}

	/** Which setting the front-panel cursor is on. */
	public enum Field { PICKUP, CURVE, DIAL, INSTANTANEOUS, RECLOSE }

	private int pickupIndex;
	private Curve curve;
	private int dialIndex;
	private int instIndex;
	private Field cursor = Field.PICKUP;
	/** True on a relay with a 79 reclosing element (the breaker relay cabinet): its shots are one more setting. */
	private boolean reclosing;
	private int shots;

	public RelaySettings() {
		this(Tuning.RELAY_PICKUP_START, Curve.MI, Tuning.RELAY_DIAL_START);
	}

	/** Gives the relay a 79 element with {@code shots} reclosing shots (0 = off, at most {@link Tuning#RECLOSE_INTERVALS}' length). */
	public RelaySettings withReclosing(int shots) {
		this.reclosing = true;
		this.shots = Math.clamp(shots, 0, Tuning.RECLOSE_INTERVALS.length);
		return this;
	}

	/** 79's shots: how many times it recloses after a trip before it locks out; 0 without a 79. */
	public int shots() {
		return reclosing ? shots : 0;
	}

	public void setShots(int shots) {
		this.shots = Math.clamp(shots, 0, Tuning.RECLOSE_INTERVALS.length);
	}

	public RelaySettings(int pickupIndex, Curve curve, int dialIndex) {
		this.pickupIndex = pickupIndex;
		this.curve = curve;
		this.dialIndex = dialIndex;
		this.instIndex = Tuning.RELAY_50_START;
	}

	/** The pickup in amps, capped at {@code ceiling} (the breaker's rating or frame; 0 for none). */
	public int pickup(int ceiling) {
		int step = Tuning.RELAY_PICKUP_STEPS[Math.clamp(pickupIndex, 0, Tuning.RELAY_PICKUP_STEPS.length - 1)];
		return ceiling > 0 ? Math.min(step, ceiling) : step;
	}

	public Curve curve() {
		return curve;
	}

	public double dial() {
		return Tuning.RELAY_DIAL_STEPS[Math.clamp(dialIndex, 0, Tuning.RELAY_DIAL_STEPS.length - 1)];
	}

	/** The 50 element's multiple of the pickup; 0 means 50 is off. */
	public double instantaneous() {
		return Tuning.RELAY_50_STEPS[Math.clamp(instIndex, 0, Tuning.RELAY_50_STEPS.length - 1)];
	}

	public Field cursor() {
		return cursor;
	}

	/** Moves the front-panel cursor to the next setting. */
	public Field nextField() {
		cursor = Field.values()[(cursor.ordinal() + 1) % Field.values().length];
		if (cursor == Field.RECLOSE && !reclosing) {
			cursor = Field.PICKUP;
		}
		return cursor;
	}

	/** Steps the selected setting on, wrapping; a pickup step above {@code ceiling} is skipped. */
	public void step(int ceiling) {
		switch (cursor) {
			case PICKUP -> {
				int start = pickupIndex;
				do {
					pickupIndex = (pickupIndex + 1) % Tuning.RELAY_PICKUP_STEPS.length;
				} while (ceiling > 0 && Tuning.RELAY_PICKUP_STEPS[pickupIndex] > ceiling && pickupIndex != 0 && pickupIndex != start);
			}
			case CURVE -> curve = Curve.values()[(curve.ordinal() + 1) % Curve.values().length];
			case DIAL -> dialIndex = (dialIndex + 1) % Tuning.RELAY_DIAL_STEPS.length;
			case INSTANTANEOUS -> instIndex = (instIndex + 1) % Tuning.RELAY_50_STEPS.length;
			case RECLOSE -> shots = (shots + 1) % (Tuning.RECLOSE_INTERVALS.length + 1);
		}
	}

	/** Sets the pickup to the highest step at or under {@code amps}. */
	public void setPickup(int amps) {
		for (int k = 0; k < Tuning.RELAY_PICKUP_STEPS.length; k++) {
			if (Tuning.RELAY_PICKUP_STEPS[k] <= amps) {
				pickupIndex = k;
			}
		}
	}

	public void setCurve(Curve curve) {
		this.curve = curve;
	}

	/** Sets the dial to the step nearest {@code dial}. */
	public void setDial(double dial) {
		int best = 0;
		for (int k = 0; k < Tuning.RELAY_DIAL_STEPS.length; k++) {
			if (Math.abs(Tuning.RELAY_DIAL_STEPS[k] - dial) < Math.abs(Tuning.RELAY_DIAL_STEPS[best] - dial)) {
				best = k;
			}
		}
		dialIndex = best;
	}

	/** Seconds the 51 element takes at {@code amps} (infinite at or below the pickup); 50 counts as instant. */
	public double secondsAt(double amps, int ceiling) {
		int pickup = pickup(ceiling);
		if (instantaneous() > 0 && amps > instantaneous() * pickup) {
			return 0;
		}
		return curve.seconds(amps / pickup, dial());
	}

	/** "51 pickup 100 A (MI, TD 1), 50 at 800 A", with the selected setting in brackets when {@code showCursor}. */
	public String describe(int ceiling, boolean showCursor) {
		int pickup = pickup(ceiling);
		String p = String.format("51 pickup %,d A", pickup);
		String c = curve.name();
		String d = String.format("TD %s", trim(dial()));
		String i = instantaneous() > 0 ? String.format("50 at %,.0f A", instantaneous() * pickup) : "50 off";
		if (showCursor) {
			switch (cursor) {
				case PICKUP -> p = "[" + p + "]";
				case CURVE -> c = "[" + c + "]";
				case DIAL -> d = "[" + d + "]";
				case INSTANTANEOUS -> i = "[" + i + "]";
				default -> { }
			}
		}
		String text = p + " (" + c + ", " + d + "), " + i;
		if (reclosing) {
			String r = shots == 0 ? "79 off" : String.format("79 %d shot%s", shots, shots == 1 ? "" : "s");
			text += ", " + (showCursor && cursor == Field.RECLOSE ? "[" + r + "]" : r);
		}
		return text;
	}

	private static String trim(double v) {
		return v == Math.rint(v) ? String.format("%.0f", v) : String.valueOf(v);
	}

	// ---- saving, as plain integers ---------------------------------------------------------------------------------------------------
	public int[] save() {
		return new int[] {pickupIndex, curve.ordinal(), dialIndex, instIndex, cursor.ordinal(), shots};
	}

	/** Loads what {@link #save} gave; a missing entry keeps its default. */
	public void load(int[] saved) {
		if (saved.length >= 5) {
			load(saved[0], saved[1], saved[2], saved[3], saved[4]);
		}
		if (saved.length >= 6) {
			setShots(saved[5]);
		}
		if (cursor == Field.RECLOSE && !reclosing) {
			cursor = Field.PICKUP;
		}
	}

	public void load(int pickup, int curveOrdinal, int dial, int inst, int field) {
		pickupIndex = Math.max(0, pickup);
		curve = Curve.values()[Math.clamp(curveOrdinal, 0, Curve.values().length - 1)];
		dialIndex = Math.clamp(dial, 0, Tuning.RELAY_DIAL_STEPS.length - 1);
		instIndex = Math.clamp(inst, 0, Tuning.RELAY_50_STEPS.length - 1);
		cursor = Field.values()[Math.clamp(field, 0, Field.values().length - 1)];
	}

	/** An inverse-time element on these settings: runs toward its trip while over pickup, resets at once below it (a digital relay). */
	public static final class Element {
		private double timer;

		/** Runs for {@code seconds} at {@code amps}; true once it trips. */
		public boolean step(double amps, int pickup, Curve curve, double dial, double seconds) {
			double m = pickup <= 0 ? 0 : amps / pickup;
			if (m <= 1.0) {
				timer = 0;
				return false;
			}
			timer += seconds / curve.seconds(m, dial);
			return timer >= 1.0;
		}

		public double progress() {
			return timer;
		}

		public void reset() {
			timer = 0;
		}
	}
}
