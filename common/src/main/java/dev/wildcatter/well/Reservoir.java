package dev.wildcatter.well;

import dev.wildcatter.Tuning;
import java.util.SplittableRandom;
import org.jspecify.annotations.Nullable;

/**
 * A pocket of oil or gas hiding in the rock. Where the fields are depends only on the world seed, so nothing is
 * stored until a well starts to empty one. The world is cut into square cells; a cell may hold one field. Deep fields
 * (docs/multiblocks/land-rig.md) are a second, coarser grid of their own below the shallow ones, so adding them changed no
 * shallow field: only a land rig reaches them.
 */
public record Reservoir(int cellX, int cellZ, double centerX, double centerZ, double radius, int topY, Kind kind, int units, boolean deep, Source source,
		int index, double inner, double angle, double arc) {
	public enum Kind {
		OIL, GAS
	}

	/**
	 * Where a field lies (docs/salt-dome-oil.md): a round shallow or deep field on its grid; a FLANK field, an arc of a ring round a
	 * salt dome sealed against its flank; or a dome's CAPROCK pool under its crest.
	 */
	public enum Source {
		FIELD, DEEP, FLANK, CAPROCK
	}

	/** A round field on the shallow or deep grid. */
	public Reservoir(int cellX, int cellZ, double centerX, double centerZ, double radius, int topY, Kind kind, int units, boolean deep) {
		this(cellX, cellZ, centerX, centerZ, radius, topY, kind, units, deep, deep ? Source.DEEP : Source.FIELD, 0, 0, 0, 0);
	}

	public String key() {
		return switch (source) {
			case FLANK -> "flank:" + cellX + "," + cellZ + ":" + index;
			case CAPROCK -> "cap:" + cellX + "," + cellZ;
			default -> (deep ? "deep:" : "") + cellX + "," + cellZ;
		};
	}

	/** True for a field round a salt dome: a flank field or a caprock pool. */
	public boolean onDome() {
		return source == Source.FLANK || source == Source.CAPROCK;
	}

	/** True if a hole drilled at this column reaches it. */
	public boolean covers(int x, int z) {
		double dx = x + 0.5 - centerX;
		double dz = z + 0.5 - centerZ;
		if (source != Source.FLANK) {
			return dx * dx + dz * dz <= radius * radius;
		}
		double r = Math.hypot(dx, dz);
		return r >= inner && r <= radius && alongArc(Math.atan2(dz, dx)) <= arc;
	}

	/** How far round the arc an angle lies from its start, 0 to 2 pi. */
	private double alongArc(double a) {
		double t = (a - angle) % (2 * Math.PI);
		return t < 0 ? t + 2 * Math.PI : t;
	}

	/** Distance from the column to the edge of the field (0 or less means you are over it). */
	public double edgeDistance(double x, double z) {
		if (source != Source.FLANK) {
			return Math.hypot(x - centerX, z - centerZ) - radius;
		}
		// the nearest point of the arc's band: the angle clamped to the arc, the radius to the band
		double dx = x - centerX, dz = z - centerZ;
		double t = alongArc(Math.atan2(dz, dx));
		double a = t <= arc ? angle + t : (t - arc < 2 * Math.PI - t ? angle + arc : angle);
		double r = Math.max(inner, Math.min(radius, Math.hypot(dx, dz)));
		double nx = r * Math.cos(a), nz = r * Math.sin(a);
		return t <= arc && Math.hypot(dx, dz) >= inner && Math.hypot(dx, dz) <= radius ? 0 : Math.hypot(dx - nx, dz - nz);
	}

	/** A column on the middle of a flank field's band, halfway round its arc: for the game test. */
	public int[] middle() {
		double a = angle + arc / 2, r = (inner + radius) / 2;
		return new int[] {(int) Math.floor(centerX + r * Math.cos(a)), (int) Math.floor(centerZ + r * Math.sin(a))};
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
		return new Reservoir(cellX, cellZ, cx, cz, radius, topY, kind, min + random.nextInt(max - min + 1), false);
	}

	/** The deep field in this cell of the deep grid, or null. */
	public static @Nullable Reservoir deepInCell(long seed, int cellX, int cellZ) {
		long mix = seed * 0xD6E8FEB86659FD93L ^ (cellX * 0x9FB21C651E98DF25L) ^ (cellZ * 0xA0761D6478BD642FL) ^ 0x5DEECE66DL;
		SplittableRandom random = new SplittableRandom(mix);
		if (random.nextDouble() >= Tuning.DEEP_FIELD_CHANCE) {
			return null;
		}
		double radius = Tuning.DEEP_FIELD_MIN_RADIUS + random.nextDouble() * (Tuning.DEEP_FIELD_MAX_RADIUS - Tuning.DEEP_FIELD_MIN_RADIUS);
		double margin = Tuning.DEEP_FIELD_MAX_RADIUS;
		double room = Tuning.DEEP_FIELD_CELL - 2 * margin;
		double cx = cellX * (double) Tuning.DEEP_FIELD_CELL + margin + random.nextDouble() * room;
		double cz = cellZ * (double) Tuning.DEEP_FIELD_CELL + margin + random.nextDouble() * room;
		int topY = Tuning.DEEP_FIELD_MIN_Y + random.nextInt(Tuning.DEEP_FIELD_MAX_Y - Tuning.DEEP_FIELD_MIN_Y + 1);
		Kind kind = random.nextDouble() < Tuning.OIL_FRACTION ? Kind.OIL : Kind.GAS;
		int min = kind == Kind.OIL ? Tuning.DEEP_OIL_MIN_UNITS : Tuning.DEEP_GAS_MIN_UNITS;
		int max = kind == Kind.OIL ? Tuning.DEEP_OIL_MAX_UNITS : Tuning.DEEP_GAS_MAX_UNITS;
		return new Reservoir(cellX, cellZ, cx, cz, radius, topY, kind, min + random.nextInt(max - min + 1), true);
	}

	/**
	 * The field a hole at this column would reach first, with every grid dome counted (whatever its biome): see
	 * {@link #at(long, int, int, java.util.function.Predicate)}. Game code asks {@link ReservoirData#field}, which checks the dome's biome.
	 */
	public static @Nullable Reservoir at(long seed, int x, int z) {
		return at(seed, x, z, dome -> true);
	}

	/**
	 * The field a hole at this column would reach first: a salt dome's flank field or caprock pool first (docs/salt-dome-oil.md), and
	 * nothing else under a dome's middle, where the salt is; else the shallow field, or else the deep one under it; or null.
	 * {@code site} says whether a grid dome is really there (its centre's biome).
	 */
	public static @Nullable Reservoir at(long seed, int x, int z, java.util.function.Predicate<SaltDome> site) {
		SaltDome dome = SaltDome.inCell(seed, Math.floorDiv(x, Tuning.DOME_CELL), Math.floorDiv(z, Tuning.DOME_CELL));
		if (dome != null && site.test(dome)) {
			for (Reservoir flank : dome.flanks()) {
				if (flank.covers(x, z)) {
					return flank;
				}
			}
			if (dome.overMiddle(x, z)) {
				return dome.caprockPool();
			}
		}
		Reservoir field = inCell(seed, Math.floorDiv(x, Tuning.FIELD_CELL), Math.floorDiv(z, Tuning.FIELD_CELL));
		return field != null && field.covers(x, z) ? field : deepAt(seed, x, z);
	}

	/** The deep field under this column, or null. */
	public static @Nullable Reservoir deepAt(long seed, int x, int z) {
		Reservoir field = deepInCell(seed, Math.floorDiv(x, Tuning.DEEP_FIELD_CELL), Math.floorDiv(z, Tuning.DEEP_FIELD_CELL));
		return field != null && field.covers(x, z) ? field : null;
	}

	/** The field whose edge is nearest to this point, within {@code range} blocks, or null (every grid dome counted). */
	public static @Nullable Reservoir nearest(long seed, double x, double z, double range) {
		return nearest(seed, x, z, range, dome -> true);
	}

	/** The field whose edge is nearest to this point, within {@code range} blocks, or null; {@code site} as for {@link #at}. */
	public static @Nullable Reservoir nearest(long seed, double x, double z, double range, java.util.function.Predicate<SaltDome> site) {
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
		int dx = Math.floorDiv((int) Math.floor(x), Tuning.DEEP_FIELD_CELL);
		int dz = Math.floorDiv((int) Math.floor(z), Tuning.DEEP_FIELD_CELL);
		int deepReach = (int) Math.ceil(range / Tuning.DEEP_FIELD_CELL) + 1;
		for (int i = -deepReach; i <= deepReach; i++) {
			for (int j = -deepReach; j <= deepReach; j++) {
				Reservoir field = deepInCell(seed, dx + i, dz + j);
				if (field != null && field.edgeDistance(x, z) < bestDistance) {
					best = field;
					bestDistance = field.edgeDistance(x, z);
				}
			}
		}
		int ox = Math.floorDiv((int) Math.floor(x), Tuning.DOME_CELL);
		int oz = Math.floorDiv((int) Math.floor(z), Tuning.DOME_CELL);
		int domeReach = (int) Math.ceil(range / Tuning.DOME_CELL) + 1;
		for (int i = -domeReach; i <= domeReach; i++) {
			for (int j = -domeReach; j <= domeReach; j++) {
				SaltDome dome = SaltDome.inCell(seed, ox + i, oz + j);
				if (dome == null || !site.test(dome)) {
					continue;
				}
				java.util.List<Reservoir> fields = new java.util.ArrayList<>(dome.flanks());
				if (dome.caprockPool() != null) {
					fields.add(dome.caprockPool());
				}
				for (Reservoir field : fields) {
					if (field.edgeDistance(x, z) < bestDistance) {
						best = field;
						bestDistance = field.edgeDistance(x, z);
					}
				}
			}
		}
		return best;
	}
}
