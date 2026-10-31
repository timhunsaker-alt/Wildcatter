package dev.wildcatter.kinetic;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * A block entity with parts that visibly turn (docs/kinetics-and-sound.md): a flywheel, a fan, a shaft. The server knows the real
 * speed and sends it to the client when it changes by more than {@link Spin#SYNC_CHANGE}; the client turns each rotor between updates.
 */
public interface Kinetic {
	/**
	 * One turning part: the block state whose model is drawn, the point it turns about and its axis (both in the block's own frame,
	 * drawn for a machine facing north; the renderer turns them with {@link #facing()}), and its speed as a multiple of the shaft's.
	 */
	record Rotor(BlockState model, Vector3f pivot, Direction.Axis axis, float ratio) {
	}

	List<Rotor> rotors();

	/** The way the machine faces. */
	Direction facing();

	/** Shaft speed and the client's turning angle. */
	Spin spin();
}
