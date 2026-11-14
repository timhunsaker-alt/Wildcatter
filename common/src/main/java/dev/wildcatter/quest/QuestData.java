package dev.wildcatter.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildcatter.Wildcatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Each player's quest progress (docs/quests.md), on the overworld, keyed by the player's UUID: the {@code DoseData} pattern. */
public class QuestData extends SavedData {
	/** One player's progress: quest ids done, quest ids whose rewards were claimed, milestone keys reached. */
	public static final class Progress {
		public final Set<String> done = new LinkedHashSet<>();
		public final Set<String> claimed = new LinkedHashSet<>();
		public final Set<String> milestones = new LinkedHashSet<>();

		static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.listOf().fieldOf("done").forGetter(p -> new ArrayList<>(p.done)),
				Codec.STRING.listOf().fieldOf("claimed").forGetter(p -> new ArrayList<>(p.claimed)),
				Codec.STRING.listOf().fieldOf("milestones").forGetter(p -> new ArrayList<>(p.milestones))).apply(i, Progress::new));

		public Progress() {
		}

		private Progress(List<String> done, List<String> claimed, List<String> milestones) {
			this.done.addAll(done);
			this.claimed.addAll(claimed);
			this.milestones.addAll(milestones);
		}
	}

	public static final Codec<QuestData> CODEC = Codec.unboundedMap(Codec.STRING, Progress.CODEC).xmap(QuestData::new, d -> d.players);
	public static final SavedDataType<QuestData> TYPE = new SavedDataType<>(Wildcatter.id("quests"), QuestData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Progress> players = new HashMap<>();

	public QuestData() {
	}

	private QuestData(Map<String, Progress> in) {
		players.putAll(in);
	}

	public static QuestData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** The player's progress (by UUID string), made empty the first time. */
	public Progress of(String player) {
		return players.computeIfAbsent(player, k -> new Progress());
	}

	public void changed() {
		setDirty();
	}
}
