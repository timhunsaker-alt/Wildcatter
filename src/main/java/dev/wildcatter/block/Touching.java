package dev.wildcatter.block;

import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Parts that work by touching (docs/multiblocks/crude-units.md, cracker.md): a heater against its column, a pump against its heater,
 * a refrigeration compressor against its chiller. The part next to any cell of another counts once.
 */
public final class Touching {
	private Touching() {
	}

	/** The block entities (main cells) of the other big parts touching the part whose main cell is {@code main}. */
	public static List<BlockEntity> parts(Level level, BlockPos main) {
		List<BlockPos> cells = BigParts.cellsAt(level, main);
		Map<BlockPos, BlockEntity> found = new LinkedHashMap<>();
		for (BlockPos cell : cells) {
			for (Direction side : Direction.values()) {
				BlockPos next = cell.relative(side);
				if (cells.contains(next)) {
					continue;
				}
				BlockPos owner = BigParts.owner(level, next);
				if (!owner.equals(main) && !found.containsKey(owner)) {
					BlockEntity entity = level.getBlockEntity(owner);
					if (entity != null) {
						found.put(owner, entity);
					}
				}
			}
		}
		return new ArrayList<>(found.values());
	}

	/** The first touching part of this kind that {@code test} accepts, or null. */
	public static <T> T find(Level level, BlockPos main, Class<T> kind, java.util.function.Predicate<T> test) {
		for (BlockEntity entity : parts(level, main)) {
			if (kind.isInstance(entity) && test.test(kind.cast(entity))) {
				return kind.cast(entity);
			}
		}
		return null;
	}
}
