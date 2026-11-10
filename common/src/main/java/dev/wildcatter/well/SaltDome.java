package dev.wildcatter.well;

import dev.wildcatter.Tuning;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.jspecify.annotations.Nullable;

/**
 * A Gulf Coast salt dome on a seed grid of its own (docs/salt-dome-oil.md): a column of rock salt from y -60 to its crest under a
 * calcite caprock (docs/ores.md), and the oil it traps: one to three FLANK fields, arcs of a ring sealed against the salt's flank,
 * and in some domes a CAPROCK pool under the crest. Like the oil fields it depends only on the world seed; whether a cell's dome is
 * really there is its centre's biome, which the caller checks ({@link ReservoirData#site}).
 *
 * @param centerX the salt column's centre column (the axis runs up through its middle, x + 0.5)
 * @param radius the salt's radius at its waist; a little wider low down, rounding over at the crest
 * @param crestY the top of the salt; the caprock is the {@link Tuning#DOME_CAPROCK} blocks above it
 */
public record SaltDome(int cellX, int cellZ, int centerX, int centerZ, int radius, int crestY, List<Reservoir> flanks, @Nullable Reservoir caprockPool) {
	public String key() {
		return "dome:" + cellX + "," + cellZ;
	}

	/** The salt's radius at height {@code y} (blocks from the axis): wider low down, rounding over in its top 3 blocks. */
	public double saltRadius(int y) {
		double r = radius + Math.max(0, crestY - 12 - y) * 0.04;
		if (y > crestY - 3) {
			r -= (y - (crestY - 3)) * 0.6;
		}
		return r;
	}

	/** The caprock's radius at height {@code y} (crest + 1 to crest + caprock): one block wider than the salt, rounding over. */
	public double capRadius(int y) {
		return radius + 1 - (y - crestY - 1) * 0.5;
	}

	public int capTop() {
		return crestY + Tuning.DOME_CAPROCK;
	}

	private double fromAxis(double x, double z) {
		return Math.hypot(x + 0.5 - (centerX + 0.5), z + 0.5 - (centerZ + 0.5));
	}

	/** True if this block is the dome's rock salt. */
	public boolean inSalt(int x, int y, int z) {
		return y >= Tuning.DOME_BOTTOM_Y && y <= crestY && fromAxis(x, z) <= saltRadius(y);
	}

	/** True if this block is the dome's caprock. */
	public boolean inCap(int x, int y, int z) {
		return y > crestY && y <= capTop() && fromAxis(x, z) <= capRadius(y);
	}

	/** True if a hole at this column goes down the dome's middle (the caprock's footprint): caprock, then salt, then no oil. */
	public boolean overMiddle(int x, int z) {
		return fromAxis(x, z) <= radius + 1;
	}

	/** Height of the low mound over the dome at this column: 0 off it, up to {@link Tuning#MOUND_MAX_HEIGHT} over its middle. */
	public int moundHeight(int x, int z) {
		double r = fromAxis(x, z), edge = radius + 1;
		return r > edge ? 0 : r <= edge / 2 ? Tuning.MOUND_MAX_HEIGHT : 1;
	}

	/** The dome in this cell of the dome grid, or null (the biome is the caller's to check). */
	public static @Nullable SaltDome inCell(long seed, int cellX, int cellZ) {
		long mix = seed * 0xE7037ED1A0B428DBL ^ (cellX * 0x8EBC6AF09C88C6E3L) ^ (cellZ * 0x589965CC75374CC3L) ^ 0x1D8E4E27C47D124FL;
		SplittableRandom random = new SplittableRandom(mix);
		if (random.nextDouble() >= Tuning.DOME_CHANCE) {
			return null;
		}
		int radius = Tuning.DOME_MIN_RADIUS + random.nextInt(Tuning.DOME_MAX_RADIUS - Tuning.DOME_MIN_RADIUS + 1);
		int crest = Tuning.DOME_MIN_CREST_Y + random.nextInt(Tuning.DOME_MAX_CREST_Y - Tuning.DOME_MIN_CREST_Y + 1);
		int room = Tuning.DOME_CELL - 2 * Tuning.DOME_MARGIN;
		int cx = cellX * Tuning.DOME_CELL + Tuning.DOME_MARGIN + random.nextInt(room);
		int cz = cellZ * Tuning.DOME_CELL + Tuning.DOME_MARGIN + random.nextInt(room);
		SaltDome shape = new SaltDome(cellX, cellZ, cx, cz, radius, crest, List.of(), null);
		// the flank fields: arcs round the axis, one after another with a gap between, so no two overlap
		int count = Tuning.FLANK_MIN_FIELDS + random.nextInt(Tuning.FLANK_MAX_FIELDS - Tuning.FLANK_MIN_FIELDS + 1);
		double[] arcs = new double[count];
		double total = 0;
		for (int i = 0; i < count; i++) {
			arcs[i] = Tuning.FLANK_MIN_ARC + random.nextDouble() * (Tuning.FLANK_MAX_ARC - Tuning.FLANK_MIN_ARC);
			total += arcs[i] + Tuning.FLANK_GAP;
		}
		if (total > 360) {
			double scale = (360 - count * Tuning.FLANK_GAP) / (total - count * Tuning.FLANK_GAP);
			for (int i = 0; i < count; i++) {
				arcs[i] *= scale;
			}
			total = 360;
		}
		double slack = 360 - total;
		double at = random.nextDouble() * 360;
		List<Reservoir> flanks = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			double gap = slack > 0 ? random.nextDouble() * slack / count : 0;
			at += gap;
			int topY = Tuning.FLANK_MIN_Y + random.nextInt(Tuning.FLANK_MAX_Y - Tuning.FLANK_MIN_Y + 1);
			// its inner edge a block inside the salt's flank at its top: the sands pinch out under the salt's edge, so a hole on its
			// inner ring goes down through rock salt (and needs salt-saturated mud, docs/multiblocks/drilling-mud.md)
			double inner = Math.max(1.5, shape.saltRadius(topY) - 1);
			double outer = inner + Tuning.FLANK_MIN_WIDTH + random.nextDouble() * (Tuning.FLANK_MAX_WIDTH - Tuning.FLANK_MIN_WIDTH);
			Reservoir.Kind kind = random.nextDouble() < Tuning.FLANK_OIL_FRACTION ? Reservoir.Kind.OIL : Reservoir.Kind.GAS;
			int min = kind == Reservoir.Kind.OIL ? Tuning.FLANK_OIL_MIN_UNITS : Tuning.FLANK_GAS_MIN_UNITS;
			int max = kind == Reservoir.Kind.OIL ? Tuning.FLANK_OIL_MAX_UNITS : Tuning.FLANK_GAS_MAX_UNITS;
			flanks.add(new Reservoir(cellX, cellZ, cx + 0.5, cz + 0.5, outer, topY, kind, min + random.nextInt(max - min + 1), false, Reservoir.Source.FLANK, i, inner,
					Math.toRadians(at % 360), Math.toRadians(arcs[i])));
			at += arcs[i] + Tuning.FLANK_GAP;
		}
		Reservoir pool = null;
		if (random.nextDouble() < Tuning.CAPROCK_POOL_CHANCE) {
			int units = Tuning.CAPROCK_OIL_MIN_UNITS + random.nextInt(Tuning.CAPROCK_OIL_MAX_UNITS - Tuning.CAPROCK_OIL_MIN_UNITS + 1);
			pool = new Reservoir(cellX, cellZ, cx + 0.5, cz + 0.5, radius + 1, crest + Tuning.DOME_CAPROCK / 2, Reservoir.Kind.OIL, units, false, Reservoir.Source.CAPROCK, 0, 0, 0, 0);
		}
		return new SaltDome(cellX, cellZ, cx, cz, radius, crest, List.copyOf(flanks), pool);
	}

	/** The grid dome whose middle or flanks lie under this column, or null (the biome is the caller's to check). */
	public static @Nullable SaltDome near(long seed, int x, int z) {
		SaltDome dome = inCell(seed, Math.floorDiv(x, Tuning.DOME_CELL), Math.floorDiv(z, Tuning.DOME_CELL));
		return dome != null && Math.hypot(x - dome.centerX, z - dome.centerZ) < Tuning.DOME_MARGIN ? dome : null;
	}
}
