package dev.wildcatter.control;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A device's HAND / OFF / AUTO selector and what its PLC last told it. A command counts only while they keep coming, so a cut cable, a dead PLC
 * and STOP look alike, as on a starter (docs/multiblocks/plc.md).
 */
public final class Remote {
	private static final int STALE_TICKS = dev.wildcatter.block.Starter.COMMAND_STALE_TICKS;
	private Hoa hoa = Hoa.HAND;
	private final boolean[] on;
	private final boolean[] was;
	private final long[] tick;
	private final double[] ma;
	private final long[] maTick;

	public Remote(int points) {
		on = new boolean[points];
		was = new boolean[points];
		tick = new long[points];
		ma = new double[points];
		maTick = new long[points];
		java.util.Arrays.fill(tick, -1000);
		java.util.Arrays.fill(maTick, -1000);
	}

	public Hoa hoa() {
		return hoa;
	}

	public boolean auto() {
		return hoa == Hoa.AUTO;
	}

	public boolean hand() {
		return hoa == Hoa.HAND;
	}

	public String cycle() {
		hoa = hoa.next();
		return hoa.name();
	}

	/** Records a command; true if it just rose, in AUTO. */
	public boolean command(int point, boolean value, long now) {
		boolean rose = value && !was[point] && auto();
		was[point] = value;
		on[point] = value;
		tick[point] = now;
		return rose;
	}

	public void commandMa(int point, double milliamps, long now) {
		ma[point] = milliamps;
		maTick[point] = now;
	}

	/** True in AUTO while the command is on and fresh. */
	public boolean on(int point, long now) {
		return auto() && on[point] && now - tick[point] <= STALE_TICKS;
	}

	/** True while commands for this point are coming at all. */
	public boolean fresh(int point, long now) {
		return now - tick[point] <= STALE_TICKS;
	}

	/** A loop's value, 0 to 1 of its range, in AUTO while it is fresh; otherwise {@code fallback}. */
	public double fraction(int point, long now, double fallback) {
		return auto() && now - maTick[point] <= STALE_TICKS && ma[point] >= 3.6 ? Math.max(0, Math.min(1, (ma[point] - 4) / 16)) : fallback;
	}

	public void save(ValueOutput output) {
		output.putString("hoa", hoa.name());
	}

	public void load(ValueInput input) {
		hoa = Hoa.valueOf(input.getStringOr("hoa", Hoa.HAND.name()));
	}
}
