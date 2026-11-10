package dev.wildcatter.circuit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Splits a level's registered circuit parts into the groups that touch, each solved on its own. The walk only looks at blocks in
 * chunks that are already loaded: reading a block in an unloaded chunk loads it on the server thread, and a circuit manager that did
 * that on every solve kept reloading the chunks round a released area (a game test batch's, or a player's who walked away) as fast
 * as the server unloaded them, which stalled the tick and filled the heap. A part over the edge of the loaded area is not
 * registered anyway (its chunk is not loaded), so the group simply ends there, as the solve already did.
 */
public final class PartGroups {
	/** What the walk needs from the level. Only {@link #loaded} is asked about a position whose chunk may not be loaded. */
	public interface World {
		/** Whether the chunk holding {@code pos} is loaded, without loading it. */
		boolean loaded(BlockPos pos);

		/** The cells of whatever stands at {@code pos} (all of a big part's, or just {@code pos}); {@code pos} is loaded. */
		List<BlockPos> cellsAt(BlockPos pos);

		/** The main cell of whatever stands at {@code pos}; {@code pos} is loaded. */
		BlockPos owner(BlockPos pos);

		/** The far ends (spans, leads) of the part at {@code pos}, or none; {@code pos} is loaded. */
		Collection<BlockPos> links(BlockPos pos);
	}

	private PartGroups() {
	}

	public static List<List<BlockPos>> of(Collection<BlockPos> registered, World world) {
		Set<BlockPos> parts = registered instanceof Set<BlockPos> set ? set : new HashSet<>(registered);
		Set<BlockPos> seen = new HashSet<>();
		List<List<BlockPos>> groups = new ArrayList<>();
		for (BlockPos start : new ArrayList<>(registered)) {
			if (seen.contains(start)) {
				continue;
			}
			List<BlockPos> group = new ArrayList<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start);
			seen.add(start);
			while (!queue.isEmpty()) {
				BlockPos here = queue.poll();
				group.add(here);
				if (!world.loaded(here)) {
					continue;   // registered, but its chunk is on its way out: the solve skips it too
				}
				// a big part touches its neighbours through every one of its cells, and a neighbour that is a piece of one means its main cell
				for (BlockPos cell : world.cellsAt(here)) {
					for (Direction side : Direction.values()) {
						BlockPos beside = cell.relative(side);
						if (!world.loaded(beside)) {
							continue;
						}
						BlockPos next = world.owner(beside);
						if (parts.contains(next) && seen.add(next)) {
							queue.add(next);
						}
					}
				}
				for (BlockPos far : world.links(here)) {
					if (parts.contains(far) && seen.add(far)) {
						queue.add(far);
					}
				}
			}
			groups.add(group);
		}
		return groups;
	}
}
