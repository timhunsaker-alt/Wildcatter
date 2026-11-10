package dev.wildcatter.control;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/** Something with signals a PLC's terminals can land: what a control cable reaches (docs/multiblocks/plc.md). */
public interface ControlDevice {
	/** What travels on a signal's pair of wires. */
	enum Signal {
		/** 24 V on or off. */
		BOOL,
		/** A 4-20 mA loop. */
		MA,
		/** A pulse train, for a high-speed counter. */
		PULSE
	}

	/** One signal. An output is driven by the PLC; an input is read by it. */
	record Point(String name, boolean output, Signal signal) {
		public Point(String name, boolean output) {
			this(name, output, Signal.BOOL);
		}
	}

	String deviceName();

	List<Point> points();

	/** An input's state, for the module to read. */
	boolean readPoint(int point);

	/** An output's state, from the module, every scan. A device must fall back when these stop coming. */
	void commandPoint(int point, boolean on, long gameTime);

	/**
	 * What an output point draws from the PLC's 24 V supply while it is on, in amps: a field device powered from the 24 V, as a lamp, a horn or a
	 * solenoid. Zero for one whose coil has control power of its own (a starter's contactor, a breaker's close and trip coils), which the
	 * module's relay contact only switches.
	 */
	default double outputAmps(int point) {
		return 0;
	}

	/** An analog input's loop current in milliamps; under 3.6 is a failed loop (NAMUR NE 43). */
	default double readMa(int point) {
		return 0;
	}

	/** An analog output's loop current, from the module, every scan while the CPU runs. A device must fall back when these stop coming. */
	default void commandMa(int point, double milliamps, long gameTime) {
	}

	/** A pulse input's running count. */
	default long readPulses(int point) {
		return 0;
	}

	/** A temperature input for an RTD module, in tenths of a degree, or NaN if this signal is not one. */
	default double readTenths(int point) {
		return Double.NaN;
	}

	/** Sets a ranged device's 4 and 20 mA values; why not, or null. Unranged devices have nothing to set. */
	default @Nullable String setRange(double low, double high) {
		return deviceName() + " has no range to set";
	}

	/** Steps the HAND / OFF / AUTO selector of the part of this device at {@code cell}; what it now reads, or null if it has none. */
	default @Nullable String cycleHoa(BlockPos cell) {
		return null;
	}
}
