package dev.wildcatter.well;

import dev.wildcatter.Tuning;
import java.util.SplittableRandom;
import org.jspecify.annotations.Nullable;

/**
 * A roll-front uranium deposit (docs/multiblocks/isr-wellfield.md): a crescent band of ore in a confined sandstone, where
 * oxygenated groundwater met reduced rock and left its uranium. Like an oil field it depends only on the world seed, on a grid of
 * its own; what is taken is kept in {@link RollFrontData}. Here it is an arc of a ring {@code width} blocks across, its ore
 * richest along the middle of the band. Only desert and badlands carry one: the caller checks the biome.
 *
 * @param angle where the arc starts, in radians; it runs {@link Tuning#ROLL_ARC_DEGREES} from there
 * @param topY the top of the ore; it runs {@code thickness} blocks down
 */
public record RollFront(int cellX, int cellZ, double centerX, double centerZ, double radius, double angle, double width, int topY, int thickness) {
	public String key() {
		return "roll:" + cellX + "," + cellZ;
	}

	public int bottomY() {
		return topY - thickness + 1;
	}

	/** How far across the band this column is: 0 on its middle line, 1 at its edge, above 1 outside it or off the arc's ends. */
	public double across(double x, double z) {
		double dx = x + 0.5 - centerX, dz = z + 0.5 - centerZ;
		double a = Math.atan2(dz, dx) - angle;
		a = (a % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
		if (a > Math.toRadians(Tuning.ROLL_ARC_DEGREES)) {
			return Double.MAX_VALUE;
		}
		return Math.abs(Math.hypot(dx, dz) - radius) / (width / 2);
	}

	public boolean covers(int x, int z) {
		return across(x, z) <= 1;
	}

	/** eU3O8 of the ore at this column (a fraction), or 0 off the front. */
	public double grade(int x, int z) {
		double t = across(x, z);
		return t > 1 ? 0 : Tuning.ROLL_PEAK_GRADE - (Tuning.ROLL_PEAK_GRADE - Tuning.ROLL_EDGE_GRADE) * t;
	}

	/** kg of U3O8 in the whole front: its band, its thickness, the sandstone, the grade across it on average. */
	public double inPlaceKg() {
		double area = radius * Math.toRadians(Tuning.ROLL_ARC_DEGREES) * width;
		return area * thickness * Tuning.ROLL_ROCK_KG_PER_M3 * (Tuning.ROLL_PEAK_GRADE + Tuning.ROLL_EDGE_GRADE) / 2;
	}

	/** kg of U3O8 the wells can win from it. */
	public double recoverableKg() {
		return inPlaceKg() * Tuning.ROLL_RECOVERY;
	}

	/** The front in this cell, or null. */
	public static @Nullable RollFront inCell(long seed, int cellX, int cellZ) {
		long mix = seed * 0xBF58476D1CE4E5B9L ^ (cellX * 0x94D049BB133111EBL) ^ (cellZ * 0x2545F4914F6CDD1DL) ^ 0x7A3C9E1FL;
		SplittableRandom random = new SplittableRandom(mix);
		if (random.nextDouble() >= Tuning.ROLL_CHANCE) {
			return null;
		}
		double radius = Tuning.ROLL_MIN_RADIUS + random.nextDouble() * (Tuning.ROLL_MAX_RADIUS - Tuning.ROLL_MIN_RADIUS);
		double width = Tuning.ROLL_MIN_WIDTH + random.nextDouble() * (Tuning.ROLL_MAX_WIDTH - Tuning.ROLL_MIN_WIDTH);
		double margin = Tuning.ROLL_MAX_RADIUS + Tuning.ROLL_MAX_WIDTH / 2;
		double room = Tuning.ROLL_CELL - 2 * margin;
		double cx = cellX * (double) Tuning.ROLL_CELL + margin + random.nextDouble() * room;
		double cz = cellZ * (double) Tuning.ROLL_CELL + margin + random.nextDouble() * room;
		double angle = random.nextDouble() * 2 * Math.PI;
		int topY = Tuning.ROLL_MIN_TOP_Y + random.nextInt(Tuning.ROLL_MAX_TOP_Y - Tuning.ROLL_MIN_TOP_Y + 1);
		int thickness = Tuning.ROLL_MIN_THICKNESS + random.nextInt(Tuning.ROLL_MAX_THICKNESS - Tuning.ROLL_MIN_THICKNESS + 1);
		return new RollFront(cellX, cellZ, cx, cz, radius, angle, width, topY, thickness);
	}

	/** The front under this column, or null (the biome is the caller's to check). */
	public static @Nullable RollFront at(long seed, int x, int z) {
		RollFront front = inCell(seed, Math.floorDiv(x, Tuning.ROLL_CELL), Math.floorDiv(z, Tuning.ROLL_CELL));
		return front != null && front.covers(x, z) ? front : null;
	}

	/** A column on the middle line of the front's arc, a third of the way along: for the game test. */
	public int[] middle() {
		double a = angle + Math.toRadians(Tuning.ROLL_ARC_DEGREES) / 3;
		return new int[] {(int) Math.floor(centerX + radius * Math.cos(a)), (int) Math.floor(centerZ + radius * Math.sin(a))};
	}

	/**
	 * The gamma log of a hole through this front at this column (m = blocks): the ore zone's depth, thickness, eU3O8 and its
	 * grade-thickness against the cut-off.
	 */
	public String gammaLog(int x, int z) {
		double grade = grade(x, z);
		double gt = thickness * grade * 100;
		return String.format("GAMMA LOG: ore from y %d to %d, %d m at %.2f %% eU3O8, GT %.1f m%% (cut-off %.1f): %s", topY, bottomY(), thickness, grade * 100, gt,
				Tuning.GAMMA_CUTOFF_GT, gt >= Tuning.GAMMA_CUTOFF_GT ? "ORE" : "below cut-off");
	}
}
