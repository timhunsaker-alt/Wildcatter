package dev.wildcatter.well;

import com.mojang.serialization.Codec;
import dev.wildcatter.Wildcatter;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** How much has been taken out of each field so far: every well on a field draws from the same pool. */
public class ReservoirData extends SavedData {
	public static final Codec<ReservoirData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(ReservoirData::new, data -> data.taken);
	public static final SavedDataType<ReservoirData> TYPE = new SavedDataType<>(Wildcatter.id("reservoirs"), ReservoirData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Integer> taken;

	public ReservoirData() {
		this.taken = new HashMap<>();
	}

	private ReservoirData(Map<String, Integer> taken) {
		this.taken = new HashMap<>(taken);
	}

	/** One store for the whole world, kept with the overworld. */
	public static ReservoirData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** The game test sets this to put a field where it needs one. Null in play. */
	public static volatile Long testSeed;

	public static long seed(ServerLevel level) {
		return testSeed != null ? testSeed : level.getServer().overworld().getSeed();
	}

	/** A game test's own seed over its own area, so scenes running side by side each keep the field they forced. */
	public record TestRegion(int minX, int minZ, int maxX, int maxZ, long seed) {
		boolean has(int x, int z) {
			return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
		}
	}

	public static final java.util.List<TestRegion> TEST_REGIONS = new java.util.concurrent.CopyOnWriteArrayList<>();

	/** The seed the fields at this column come from: a test region's, the test seed, or the world's. */
	public static long seed(net.minecraft.world.level.LevelReader level, int x, int z) {
		for (TestRegion r : TEST_REGIONS) {
			if (r.has(x, z)) {
				return r.seed();
			}
		}
		if (testSeed != null) {
			return testSeed;
		}
		return level instanceof ServerLevel server ? server.getServer().overworld().getSeed()
				: level instanceof net.minecraft.world.level.WorldGenLevel gen ? gen.getSeed() : 0;
	}

	/** The game test sets this to stand a grid dome wherever it needs one, whatever the biome. Null in play. */
	public static volatile Boolean testDomeBiome;

	/** True for the biomes a salt dome stands in (docs/ores.md): plains, swamps and beaches, the Gulf Coast's. */
	public static boolean domeBiome(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
		return biome.is(net.minecraft.world.level.biome.Biomes.PLAINS) || biome.is(net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS)
				|| biome.is(net.minecraft.world.level.biome.Biomes.SWAMP) || biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP)
				|| biome.is(net.minecraft.tags.BiomeTags.IS_BEACH);
	}

	/** Whether a grid dome is really there: the world's rule, then its centre's biome (docs/salt-dome-oil.md). Works for unloaded chunks too. */
	public static java.util.function.Predicate<SaltDome> site(net.minecraft.world.level.LevelReader level) {
		// with the world rule saltDomeGrid192 off (a world from before 15a) there are no grid domes: its fields are as they were
		return dome -> dev.wildcatter.Rules.saltDomeGrid() && (testDomeBiome != null ? testDomeBiome : domeBiome(level.getBiome(new net.minecraft.core.BlockPos(dome.centerX(), 64, dome.centerZ()))));
	}

	/** The field a hole at this column reaches first in this world, salt domes included (docs/salt-dome-oil.md). */
	public static @org.jspecify.annotations.Nullable Reservoir field(ServerLevel level, int x, int z) {
		return Reservoir.at(seed(level, x, z), x, z, site(level));
	}

	/** The grid dome under this column in this world (its middle or flanks), or null. */
	public static @org.jspecify.annotations.Nullable SaltDome dome(ServerLevel level, int x, int z) {
		SaltDome dome = SaltDome.near(seed(level, x, z), x, z);
		return dome != null && site(level).test(dome) ? dome : null;
	}

	public int remaining(Reservoir field) {
		return Math.max(0, field.units() - taken.getOrDefault(field.key(), 0));
	}

	/** Takes up to {@code amount} units out of the field and returns how many came out. */
	public double draw(Reservoir field, double amount) {
		int left = remaining(field);
		int got = (int) Math.min(left, Math.floor(amount));
		if (got > 0) {
			taken.merge(field.key(), got, Integer::sum);
			setDirty();
		}
		return got;
	}

	/**
	 * 1 when full, falling to 0 as it is emptied. This is the pressure that pushes the oil or gas up. A dome's caprock pool holds
	 * its full pressure for its first {@link dev.wildcatter.Tuning#CAPROCK_FLUSH} and then falls fast (docs/salt-dome-oil.md): a
	 * big first week and a short life, as the Lucas gusher's.
	 */
	public double pressure(Reservoir field) {
		double left = remaining(field) / (double) field.units();
		if (field.source() == Reservoir.Source.CAPROCK) {
			double knee = 1 - dev.wildcatter.Tuning.CAPROCK_FLUSH;
			return left >= knee ? 1 : dev.wildcatter.Tuning.CAPROCK_AFTER * left / knee;
		}
		return left;
	}

	/** Game test hook: as if {@code units} had been taken out of the field. */
	public void testTake(Reservoir field, int units) {
		taken.merge(field.key(), units, Integer::sum);
		setDirty();
	}
}
