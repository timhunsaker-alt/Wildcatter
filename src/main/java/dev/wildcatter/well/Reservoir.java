package dev.wildcatter.well;

import dev.wildcatter.Tuning;
import java.util.SplittableRandom;
import org.jspecify.annotations.Nullable;

/**
 * A pocket of oil or gas hiding in the rock. Where the fields are depends only on the world seed, so nothing is
 * stored until a well starts to empty one. The world is cut into square cells; a cell may hold one field.
 */
public record Reservoir(int cellX, int cellZ, double centerX, double centerZ, double radius, int topY, Kind kind, int units) {
	public enum Kind {
		OIL, GAS
	}

	public String key() {
		return cellX + "," + cellZ;
	}

	/** True if a hole drilled at this column reaches it. */
	public boolean covers(int x, int z) {
		double dx = x + 0.5 - centerX;
		double dz = z + 0.5 - centerZ;
		return dx * dx + dz * dz <= radius * radius;
	}

	/** Distance from the column to the edge of the field (0 or less means you are over it). */
	public double edgeDistance(double x, double z) {
		return Math.hypot(x - centerX, z - centerZ) - radius;
	}

	/** The field in this cell, or null if the ground there is barren. */
	public static @Nullable Reservoir inCell(long seed, int cellX, int cellZ) {
		long mix = seed * 0x9E3779B97F4A7C15L ^ (cellX * 0xC2B2AE3D27D4EB4FL) ^ (cellZ * 0x165667B19E3779F9L);
		SplittableRandom random = new SplittableRandom(mix);
		if (random.nextDouble() >= Tuning.FIELD_CHANCE) {
			return null;
		}
		double radius = Tuning.FIELD_MIN_RADIUS + random.nextDouble() * (Tuning.FIELD_MAX_RADIUS - Tuning.FIELD_MIN_RADIUS);
		double margin = Tuning.FIELD_MAX_RADIUS;
		double room = Tuning.FIELD_CELL - 2 * margin;
		double cx = cellX * (double) Tuning.FIELD_CELL + margin + random.nextDouble() * room;
		double cz = cellZ * (double) Tuning.FIELD_CELL + margin + random.nextDouble() * room;
		int topY = Tuning.FIELD_MIN_Y + random.nextInt(Tuning.FIELD_MAX_Y - Tuning.FIELD_MIN_Y + 1);
		Kind kind = random.nextDouble() < Tuning.OIL_FRACTION ? Kind.OIL : Kind.GAS;
		int min = kind == Kind.OIL ? Tuning.OIL_MIN_UNITS : Tuning.GAS_MIN_UNITS;
		int max = kind == Kind.OIL ? Tuning.OIL_MAX_UNITS : Tuning.GAS_MAX_UNITS;
		return new Reservoir(cellX, cellZ, cx, cz, radius, topY, kind, min + random.nextInt(max - min + 1));
	}

	/** The field a hole at this column would reach, or null. */
	public static @Nullable Reservoir at(long seed, int x, int z) {
		Reservoir field = inCell(seed, Math.floorDiv(x, Tuning.FIELD_CELL), Math.floorDiv(z, Tuning.FIELD_CELL));
		return field != null && field.covers(x, z) ? field : null;
	}

	/** The field whose edge is nearest to this point, within {@code range} blocks, or null. */
	public static @Nullable Reservoir nearest(long seed, double x, double z, double range) {
		int cx = Math.floorDiv((int) Math.floor(x), Tuning.FIELD_CELL);
		int cz = Math.floorDiv((int) Math.floor(z), Tuning.FIELD_CELL);
		int reach = (int) Math.ceil(range / Tuning.FIELD_CELL) + 1;
		Reservoir best = null;
		double bestDistance = range;
		for (int i = -reach; i <= reach; i++) {
			for (int j = -reach; j <= reach; j++) {
				Reservoir field = inCell(seed, cx + i, cz + j);
				if (field != null && field.edgeDistance(x, z) < bestDistance) {
					best = field;
					bestDistance = field.edgeDistance(x, z);
				}
			}
		}
		return best;
	}
}
