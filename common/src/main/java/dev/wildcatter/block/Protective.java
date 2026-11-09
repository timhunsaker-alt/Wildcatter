package dev.wildcatter.block;

import dev.wildcatter.circuit.TripCurve;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A breaker or fuse that takes part in the coordination study (protection gap wave): its time-current curve, its rating, the fault
 * current it can interrupt, and where its line (source) side leaves it, so {@link Coordination} can find the device upstream of it.
 */
public interface Protective extends TripCurve {
	/** The continuous rating its curve is drawn on. */
	double ratingAmps();

	/** The largest fault it is rated to interrupt: the top of its coordination study. */
	double interruptingAmps();

	/** A terminal of its line side: the cell, and the face the supply comes in by. */
	record Terminal(BlockPos cell, Direction face) {
	}

	/** The curve its coordination study runs on: itself, or its fuses' current-limiting curve. */
	default TripCurve studyCurve() {
		return this;
	}

	/** Its line-side terminals; empty if it has no fixed line side. */
	List<Terminal> lineSide();
}
