package dev.wildcatter.well;

import com.mojang.serialization.Codec;
import dev.wildcatter.Wildcatter;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** How much U3O8 (kg) the wells have taken out of each roll-front: every producer on a front draws from the same pool. */
public class RollFrontData extends SavedData {
	public static final Codec<RollFrontData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).xmap(RollFrontData::new, data -> data.taken);
	public static final SavedDataType<RollFrontData> TYPE = new SavedDataType<>(Wildcatter.id("roll_fronts"), RollFrontData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Double> taken;

	public RollFrontData() {
		this.taken = new HashMap<>();
	}

	private RollFrontData(Map<String, Double> taken) {
		this.taken = new HashMap<>(taken);
	}

	public static RollFrontData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** 1 while none is taken, falling to 0 when the recoverable uranium is gone: the head grade follows it. */
	public double left(RollFront front) {
		return Math.max(0, 1 - taken.getOrDefault(front.key(), 0.0) / front.recoverableKg());
	}

	/** Takes up to {@code kg} of U3O8 out of the front; returns what came out. */
	public double draw(RollFront front, double kg) {
		double got = Math.min(kg, left(front) * front.recoverableKg());
		if (got > 0) {
			taken.merge(front.key(), got, Double::sum);
			setDirty();
		}
		return got;
	}
}
