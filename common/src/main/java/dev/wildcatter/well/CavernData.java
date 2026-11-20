package dev.wildcatter.well;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildcatter.Wildcatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The salt caverns of a world (docs/multiblocks/oilfield-follow-ons.md, 23c), saved per hole (by its wellhead's place): the rock salt
 * blocks leached out of it, the gas in it, how much of its brine has been pushed out, and its breach. A brine wellhead and the storage
 * wellhead set on the same hole after it share the record; breaking a storage wellhead leaves its gas here.
 */
public final class CavernData extends SavedData {
	/** One cavern. */
	public static final class Hole {
		public final Set<Long> blocks = new LinkedHashSet<>();
		public double gas;          // Sm3
		public double debrined;     // m3 of brine pushed out by gas
		public boolean dry;
		public boolean breached;
		public long lowSince = -1;
		public long heaveAt = -1;

		public int volume() {
			return blocks.size();
		}

		public boolean contains(BlockPos pos) {
			return blocks.contains(pos.asLong());
		}

		private static final Codec<Hole> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.LONG.listOf().fieldOf("blocks").forGetter(h -> new ArrayList<>(h.blocks)),
				Codec.DOUBLE.optionalFieldOf("gas", 0.0).forGetter(h -> h.gas),
				Codec.DOUBLE.optionalFieldOf("debrined", 0.0).forGetter(h -> h.debrined),
				Codec.BOOL.optionalFieldOf("dry", false).forGetter(h -> h.dry),
				Codec.BOOL.optionalFieldOf("breached", false).forGetter(h -> h.breached),
				Codec.LONG.optionalFieldOf("lowSince", -1L).forGetter(h -> h.lowSince),
				Codec.LONG.optionalFieldOf("heaveAt", -1L).forGetter(h -> h.heaveAt)).apply(i, (blocks, gas, debrined, dry, breached, low, heave) -> {
					Hole h = new Hole();
					h.blocks.addAll(blocks);
					h.gas = gas;
					h.debrined = debrined;
					h.dry = dry;
					h.breached = breached;
					h.lowSince = low;
					h.heaveAt = heave;
					return h;
				}));
	}

	public static final Codec<CavernData> CODEC = Codec.unboundedMap(Codec.STRING, Hole.CODEC).xmap(CavernData::new, d -> d.holes);
	public static final SavedDataType<CavernData> TYPE = new SavedDataType<>(Wildcatter.id("caverns"), CavernData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Hole> holes;

	private CavernData() {
		this(new HashMap<>());
	}

	private CavernData(Map<String, Hole> holes) {
		this.holes = new HashMap<>(holes);
	}

	public static CavernData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The cavern under the wellhead at {@code head}, made if it has none yet. */
	public Hole at(BlockPos head) {
		setDirty();
		return holes.computeIfAbsent(Long.toString(head.asLong()), k -> new Hole());
	}

	public List<BlockPos> positions(Hole hole) {
		List<BlockPos> out = new ArrayList<>();
		for (long l : hole.blocks) {
			out.add(BlockPos.of(l));
		}
		return out;
	}
}
