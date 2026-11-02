package dev.wildcatter.radiation;

import com.mojang.serialization.Codec;
import dev.wildcatter.Wildcatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Each player's radiation dose (docs/multiblocks/fuel-handling.md): the lifetime dose and the acute dose that repairs, in sieverts. */
public class DoseData extends SavedData {
	public static final Codec<DoseData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE.listOf()).xmap(DoseData::new, DoseData::lists);
	public static final SavedDataType<DoseData> TYPE = new SavedDataType<>(Wildcatter.id("dose"), DoseData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, double[]> doses = new HashMap<>();

	public DoseData() {
	}

	private DoseData(Map<String, List<Double>> in) {
		in.forEach((k, v) -> doses.put(k, new double[] {v.get(0), v.get(1)}));
	}

	private Map<String, List<Double>> lists() {
		Map<String, List<Double>> out = new HashMap<>();
		doses.forEach((k, v) -> out.put(k, new ArrayList<>(List.of(v[0], v[1]))));
		return out;
	}

	public static DoseData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** {lifetime, acute} for a player. */
	public double[] of(String player) {
		return doses.computeIfAbsent(player, k -> new double[2]);
	}

	public void changed() {
		setDirty();
	}
}
