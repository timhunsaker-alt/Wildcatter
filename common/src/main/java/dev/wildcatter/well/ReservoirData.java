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

	/** 1 when full, falling to 0 as it is emptied. This is the pressure that pushes the oil or gas up. */
	public double pressure(Reservoir field) {
		return remaining(field) / (double) field.units();
	}
}
