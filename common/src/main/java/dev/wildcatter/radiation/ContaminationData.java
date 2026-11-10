package dev.wildcatter.radiation;

import com.mojang.serialization.Codec;
import dev.wildcatter.Tuning;
import dev.wildcatter.Wildcatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Contamination (docs/multiblocks/reactor-extensions.md, 19b), stored sparsely with each level: by block, a share of a fully
 * contaminated block (1 mSv/h at 1 m), and by player, a share of a fully contaminated person. Each entry keeps its value and the game
 * time it was set, and decays with a 30-day half-life when read. Only the contamination rule (reactorContamination, dev.wildcatter.Rules) puts any here.
 */
public class ContaminationData extends SavedData {
	public static final Codec<ContaminationData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE.listOf()).xmap(ContaminationData::new, ContaminationData::lists);
	public static final SavedDataType<ContaminationData> TYPE = new SavedDataType<>(Wildcatter.id("contamination"), ContaminationData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<Long, double[]> blocks = new HashMap<>();
	private final Map<String, double[]> players = new HashMap<>();
	private final Map<Long, Set<Long>> byChunk = new HashMap<>();

	public ContaminationData() {
	}

	private ContaminationData(Map<String, List<Double>> in) {
		in.forEach((k, v) -> {
			double[] e = {v.get(0), v.get(1)};
			if (k.startsWith("b:")) {
				long pos = Long.parseLong(k.substring(2));
				blocks.put(pos, e);
				index(pos);
			} else if (k.startsWith("p:")) {
				players.put(k.substring(2), e);
			}
		});
	}

	private Map<String, List<Double>> lists() {
		Map<String, List<Double>> out = new HashMap<>();
		blocks.forEach((k, v) -> out.put("b:" + k, new ArrayList<>(List.of(v[0], v[1]))));
		players.forEach((k, v) -> out.put("p:" + k, new ArrayList<>(List.of(v[0], v[1]))));
		return out;
	}

	public static ContaminationData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	private static long chunk(int cx, int cz) {
		return ((long) cx << 32) ^ (cz & 0xffffffffL);
	}

	private void index(long pos) {
		byChunk.computeIfAbsent(chunk(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4), k -> new HashSet<>()).add(pos);
	}

	private static double decayed(double[] e, long now) {
		return e[0] * Math.pow(0.5, Math.max(0, now - e[1]) / Tuning.CONTAMINATION_HALF_LIFE_TICKS);
	}

	/** A block's contamination now, 0 to 1. */
	public double at(BlockPos pos, long now) {
		double[] e = blocks.get(pos.asLong());
		return e == null ? 0 : decayed(e, now);
	}

	/** Sets a block's contamination (0 to 1); under 1e-4 it is forgotten. */
	public void set(BlockPos pos, double value, long now) {
		long key = pos.asLong();
		if (value < 1e-4) {
			if (blocks.remove(key) != null) {
				java.util.Optional.ofNullable(byChunk.get(chunk(pos.getX() >> 4, pos.getZ() >> 4))).ifPresent(s -> s.remove(key));
				setDirty();
			}
			return;
		}
		if (!blocks.containsKey(key)) {
			index(key);
		}
		blocks.put(key, new double[] {Math.min(1, value), now});
		setDirty();
	}

	public void add(BlockPos pos, double amount, long now) {
		set(pos, at(pos, now) + amount, now);
	}

	/** The contaminated blocks in the chunks within {@code range} of ({@code x}, {@code z}). */
	public List<BlockPos> near(double x, double z, double range) {
		List<BlockPos> out = new ArrayList<>();
		int r = (int) Math.ceil(range / 16) + 1;
		int cx = (int) Math.floor(x) >> 4, cz = (int) Math.floor(z) >> 4;
		for (int i = cx - r; i <= cx + r; i++) {
			for (int j = cz - r; j <= cz + r; j++) {
				Set<Long> set = byChunk.get(chunk(i, j));
				if (set != null) {
					for (long p : set) {
						out.add(BlockPos.of(p));
					}
				}
			}
		}
		return out;
	}

	public int size() {
		return blocks.size();
	}

	/** A player's own contamination now, 0 to 1. */
	public double skin(String player, long now) {
		double[] e = players.get(player);
		return e == null ? 0 : decayed(e, now);
	}

	public void setSkin(String player, double value, long now) {
		if (value < 1e-4) {
			if (players.remove(player) != null) {
				setDirty();
			}
			return;
		}
		players.put(player, new double[] {Math.min(1, value), now});
		setDirty();
	}
}
