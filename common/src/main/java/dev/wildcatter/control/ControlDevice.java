package dev.wildcatter.control;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/** Something with signals a PLC's terminals can land: what a control cable reaches (docs/multiblocks/plc.md). */
public interface ControlDevice {
	/** One signal. An output is driven by the PLC; an input is read by it. */
	record Point(String name, boolean output) {
	}

	String deviceName();

	List<Point> points();

	/** An input's state, for the module to read. */
	boolean readPoint(int point);

	/** An output's state, from the module, every scan. A device must fall back when these stop coming. */
	void commandPoint(int point, boolean on, long gameTime);

	/** Steps the HAND / OFF / AUTO selector of the part of this device at {@code cell}; what it now reads, or null if it has none. */
	default @Nullable String cycleHoa(BlockPos cell) {
		return null;
	}
}
