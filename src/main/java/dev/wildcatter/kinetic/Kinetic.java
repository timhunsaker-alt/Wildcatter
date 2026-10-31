package dev.wildcatter.kinetic;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A block entity with parts that visibly turn (docs/kinetics-and-sound.md): a flywheel, a fan, a shaft. The server knows the real
 * speed and sends it to the client when it changes by more than {@link Spin#SYNC_CHANGE}; the client turns each rotor between updates.
 */
public interface Kinetic {
	/**
	 * How a part on a linkage moves with the shaft (a pumpjack's beam rocks, its polished rod slides): sets {@code pose}, about the
	 * rotor's pivot, for the shaft at {@code shaftDegrees}.
	 */
	@FunctionalInterface
	interface Motion {
		void pose(Matrix4f pose, float shaftDegrees);
	}

	/**
	 * One turning part: the block state whose model is drawn, the point it turns about and its axis (both in the block's own frame,
	 * drawn for a machine facing north; the renderer turns them with {@link #facing()}), and its speed as a multiple of the shaft's.
	 * A part with a {@link Motion} moves by it instead of turning about its axis.
	 */
	record Rotor(BlockState model, Vector3f pivot, Direction.Axis axis, float ratio, @Nullable Motion motion) {
		public Rotor(BlockState model, Vector3f pivot, Direction.Axis axis, float ratio) {
			this(model, pivot, axis, ratio, null);
		}
	}

	List<Rotor> rotors();

	/** The way the machine faces. */
	Direction facing();

	/** Shaft speed and the client's turning angle. */
	Spin spin();

	/** Real rpm is shown divided by this: {@link Spin#DISPLAY_DIVISOR} for fast machines, 1 for slow ones shown at real speed. */
	default float displayDivisor() {
		return Spin.DISPLAY_DIVISOR;
	}
}
