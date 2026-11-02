package dev.wildcatter.fluid;

import dev.wildcatter.Tuning;

/**
 * The hyperbolic natural-draft cooling tower's numbers (docs/multiblocks/natural-draft-tower.md): the shell's shape and lifts, the
 * materials each stage takes, the wet bulb (Stull, 2011), the approach and the water it uses. Plain Java, so it is unit-tested
 * without Minecraft.
 */
public final class CoolingTower {
	private CoolingTower() {
	}

	/** The shell's radius at {@code y} m over the basin floor. */
	public static double radius(double y) {
		double u = (y - Tuning.TOWER_THROAT_Y) / Tuning.TOWER_SHAPE_B;
		return Tuning.TOWER_THROAT_R * Math.sqrt(1 + u * u);
	}

	public static int lifts() {
		return (int) Math.ceil((Tuning.TOWER_TOP_Y - Tuning.TOWER_SHELL_BASE_Y) / Tuning.TOWER_LIFT_M - 1e-9);
	}

	/** The bottom of lift {@code i}, m over the basin floor. */
	public static double liftY(int i) {
		return Tuning.TOWER_SHELL_BASE_Y + i * Tuning.TOWER_LIFT_M;
	}

	/** The shell's thickness at {@code y}. */
	public static double thickness(double y) {
		double f = (y - Tuning.TOWER_SHELL_BASE_Y) / (Tuning.TOWER_TOP_Y - Tuning.TOWER_SHELL_BASE_Y);
		return Tuning.TOWER_SHELL_BASE_T + (Tuning.TOWER_SHELL_TOP_T - Tuning.TOWER_SHELL_BASE_T) * f;
	}

	/** The height of lift {@code i}: 1.5 m, the last one to the top. */
	public static double liftHeight(int i) {
		return Math.min(Tuning.TOWER_LIFT_M, Tuning.TOWER_TOP_Y - liftY(i));
	}

	/** Real m3 of concrete in lift {@code i}. */
	public static double liftM3(int i) {
		double mid = liftY(i) + liftHeight(i) / 2;
		return 2 * Math.PI * radius(mid) * thickness(mid) * liftHeight(i);
	}

	public static double shellM3() {
		double sum = 0;
		for (int i = 0; i < lifts(); i++) {
			sum += liftM3(i);
		}
		return sum;
	}

	/** Gray concrete blocks a lift takes (a tenth of its real m3), fractional. */
	public static double liftConcrete(int i) {
		return liftM3(i) * Tuning.TOWER_CONCRETE_SHARE;
	}

	/** kg of rebar (steel) a lift takes. */
	public static double liftSteelKg(int i) {
		return liftM3(i) * Tuning.TOWER_REBAR_KG_PER_M3;
	}

	public static double totalConcrete() {
		return Tuning.TOWER_BASIN_CONCRETE + Tuning.TOWER_COLUMNS_CONCRETE + shellM3() * Tuning.TOWER_CONCRETE_SHARE;
	}

	public static double totalSteelIngots() {
		return Tuning.TOWER_BASIN_STEEL + Tuning.TOWER_COLUMNS_STEEL + Tuning.TOWER_FILL_STEEL + shellM3() * Tuning.TOWER_REBAR_KG_PER_M3 / Tuning.STEEL_INGOT_KG;
	}

	/** The wet-bulb temperature, C, from the air's {@code c} C and {@code rh} % relative humidity (Stull, 2011). */
	public static double wetBulb(double c, double rh) {
		return c * Math.atan(0.151977 * Math.sqrt(rh + 8.313659)) + Math.atan(c + rh) - Math.atan(rh - 1.676331)
				+ 0.00391838 * Math.pow(rh, 1.5) * Math.atan(0.023101 * rh) - 4.686035;
	}

	/** The air's dry bulb, C, from a biome's temperature number. */
	public static double dryBulb(float biomeTemperature) {
		return -5 + 25 * biomeTemperature;
	}

	/** How far over the wet bulb the cold water comes, C, at {@code watts} of heat. */
	public static double approach(double watts) {
		return Tuning.TOWER_APPROACH_C * Math.pow(Math.max(0, watts) / Tuning.TOWER_DESIGN_W, 0.6);
	}

	/** kg/s of water evaporated rejecting {@code watts}. */
	public static double evaporation(double watts) {
		return Math.max(0, watts) / Tuning.LATENT_J_PER_KG;
	}

	/** kg/s of make-up: the evaporation and the blowdown that keeps the cycles of concentration. */
	public static double makeUp(double watts) {
		return evaporation(watts) * Tuning.TOWER_CYCLES / (Tuning.TOWER_CYCLES - 1);
	}

	/** kg of water the full basin holds. */
	public static double basinKg() {
		return Math.PI * Tuning.TOWER_BASIN_R * Tuning.TOWER_BASIN_R * Tuning.TOWER_BASIN_DEPTH * 1000;
	}
}
