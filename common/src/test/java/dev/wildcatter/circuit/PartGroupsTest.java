package dev.wildcatter.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * The circuit groups' walk must not touch a block in an unloaded chunk: on a server that read loads the chunk, and doing it on every
 * solve kept a released test area (or a player's old base) loading and unloading for ever, which hung the game test server and ran
 * it out of heap.
 */
class PartGroupsTest {
	/** A level of 16 x 16 chunks: a read of a block in an unloaded chunk "loads" it, as ServerLevel.getBlockState does. */
	static final class FakeWorld implements PartGroups.World {
		final Set<Long> loadedChunks = new HashSet<>();
		/** Big parts: every cell to its main cell. */
		final Map<BlockPos, BlockPos> owners = new HashMap<>();
		final Map<BlockPos, List<BlockPos>> links = new HashMap<>();
		final List<BlockPos> loadsCaused = new ArrayList<>();

		static long chunk(BlockPos pos) {
			return ((long) (pos.getX() >> 4) << 32) ^ ((pos.getZ() >> 4) & 0xffffffffL);
		}

		void read(BlockPos pos) {
			if (loadedChunks.add(chunk(pos))) {
				loadsCaused.add(pos);
			}
		}

		@Override
		public boolean loaded(BlockPos pos) {
			return loadedChunks.contains(chunk(pos));
		}

		@Override
		public List<BlockPos> cellsAt(BlockPos pos) {
			read(pos);
			BlockPos main = owners.getOrDefault(pos, pos);
			List<BlockPos> cells = new ArrayList<>();
			owners.forEach((cell, m) -> {
				if (m.equals(main)) {
					cells.add(cell);
				}
			});
			return cells.isEmpty() ? List.of(pos) : cells;
		}

		@Override
		public BlockPos owner(BlockPos pos) {
			read(pos);
			return owners.getOrDefault(pos, pos);
		}

		@Override
		public Collection<BlockPos> links(BlockPos pos) {
			read(pos);
			return links.getOrDefault(pos, List.of());
		}
	}

	@Test
	void neverReadsIntoAnUnloadedChunk() {
		FakeWorld world = new FakeWorld();
		world.loadedChunks.add(FakeWorld.chunk(BlockPos.ZERO));   // chunk 0,0 loaded; chunk 1,0 (x 16-31) unloaded
		Set<BlockPos> registered = new LinkedHashSet<>();
		for (int x = 0; x < 16; x++) {
			registered.add(new BlockPos(x, 64, 0));   // a cable run to the chunk's east edge, which carries on into the unloaded chunk
		}
		BlockPos stale = new BlockPos(20, 64, 0);
		registered.add(stale);   // a part still registered while its chunk unloads
		for (int pass = 0; pass < 50; pass++) {
			List<List<BlockPos>> groups = PartGroups.of(registered, world);
			assertEquals(2, groups.size(), "the loaded run is one group and the part on its way out another: " + groups);
		}
		assertTrue(world.loadsCaused.isEmpty(), "the walk loaded chunks: " + world.loadsCaused);
		assertEquals(1, world.loadedChunks.size());
	}

	@Test
	void groupsTouchingPartsBigPartsAndSpans() {
		FakeWorld world = new FakeWorld();
		for (int cx = -2; cx <= 2; cx++) {
			for (int cz = -2; cz <= 2; cz++) {
				world.loadedChunks.add(FakeWorld.chunk(new BlockPos(cx * 16, 0, cz * 16)));
			}
		}
		// a three-cell big part (main at 15,64,0) over a chunk border, a cable at each end, a span from one cable to a far insulator
		BlockPos main = new BlockPos(15, 64, 0);
		world.owners.put(main, main);
		world.owners.put(new BlockPos(16, 64, 0), main);
		world.owners.put(new BlockPos(17, 64, 0), main);
		BlockPos west = new BlockPos(14, 64, 0);
		BlockPos east = new BlockPos(18, 64, 0);
		BlockPos far = new BlockPos(-20, 70, 30);
		world.links.put(east, List.of(far));
		world.links.put(far, List.of(east));
		BlockPos alone = new BlockPos(0, 64, -20);
		Set<BlockPos> registered = new LinkedHashSet<>(List.of(west, main, east, far, alone));
		List<List<BlockPos>> groups = PartGroups.of(registered, world);
		assertEquals(2, groups.size(), groups.toString());
		assertEquals(Set.of(west, main, east, far), new HashSet<>(groups.get(0)));
		assertEquals(List.of(alone), groups.get(1));
		assertTrue(world.loadsCaused.isEmpty(), "everything was loaded: " + world.loadsCaused);
	}
}
