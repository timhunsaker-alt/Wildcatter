package dev.wildcatter.control;

import com.mojang.serialization.Codec;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.platform.Platform;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * An HMI panel: its screen's layout, its alarms with their ISA-18.2 states and history, and its trends. It reads and writes any PLC on its
 * network by tag (docs/multiblocks/plc.md). A layout row is {@code KIND|label|tag|low|high|x|y}; an alarm row {@code tag|op|limit|priority|message}.
 */
public class HmiPanelBlockEntity extends BlockEntity implements Inspectable, PlcRequests {
	public enum AlarmState {
		NORMAL, UNACKED, ACKED, RETURNED_UNACKED, SHELVED
	}

	/** One sample a second, kept for ten minutes (proposed). */
	private static final int TREND_SAMPLES = 600;
	private static final int HISTORY = 100;
	private static final int MOST_ROWS = 128;
	/** A shelved alarm comes back by itself, as ISA-18.2 asks: ten minutes (proposed). */
	private static final int SHELF_SECONDS = 600;

	private final List<String> layout = new ArrayList<>();
	private final List<String> alarms = new ArrayList<>();
	private final Map<Integer, AlarmState> states = new HashMap<>();
	private final Map<Integer, Integer> shelved = new HashMap<>();
	private final Deque<String> history = new ArrayDeque<>();
	private final Map<String, Deque<Float>> trends = new HashMap<>();
	private final Set<UUID> watchers = new HashSet<>();
	private int seconds;

	public HmiPanelBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HMI_PANEL_ENTITY, pos, state);
	}

	private List<PlcCabinetBlockEntity> plcs() {
		return NetworkNet.plcs(level, List.of(worldPosition));
	}

	public AlarmState alarmState(int index) {
		return states.getOrDefault(index, AlarmState.NORMAL);
	}

	public List<String> historyForTest() {
		return new ArrayList<>(history);
	}

	public int trendSamples(String tag) {
		Deque<Float> trend = trends.get(tag.toUpperCase(Locale.ROOT));
		return trend == null ? 0 : trend.size();
	}

	private static double number(Object value) {
		return value instanceof Boolean b ? (b ? 1 : 0) : value instanceof Number n ? n.doubleValue() : Double.NaN;
	}

	private static String[] fields(String row, int count) {
		String[] out = row.split("\\|", -1);
		return out.length >= count ? out : java.util.Arrays.copyOf(out, count);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, HmiPanelBlockEntity hmi) {
		long now = level.getGameTime();
		if (now % 20 == pos.hashCode() % 20 || now % 20 == -(pos.hashCode() % 20)) {
			hmi.second();
		}
		if (now % 10 == 0 && !hmi.watchers.isEmpty() && level.getServer() != null) {
			for (UUID id : List.copyOf(hmi.watchers)) {
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
				if (player == null || player.blockPosition().distSqr(pos) > 64) {
					hmi.watchers.remove(id);
				} else {
					hmi.send(player, false, List.of());
				}
			}
		}
	}

	private void second() {
		seconds++;
		List<PlcCabinetBlockEntity> plcs = plcs();
		for (int i = 0; i < alarms.size(); i++) {
			String[] row = fields(alarms.get(i), 5);
			double value = number(NetworkNet.read(plcs, row[0] == null ? "" : row[0]));
			double limit;
			try {
				limit = row[2] == null || row[2].isEmpty() ? 0 : Double.parseDouble(row[2]);
			} catch (NumberFormatException e) {
				limit = 0;
			}
			boolean active = !Double.isNaN(value) && switch (row[1] == null ? "" : row[1]) {
				case ">" -> value > limit;
				case "<" -> value < limit;
				default -> value != 0;
			};
			AlarmState was = states.getOrDefault(i, AlarmState.NORMAL);
			AlarmState next = was;
			switch (was) {
				case NORMAL -> next = active ? AlarmState.UNACKED : was;
				case UNACKED -> next = active ? was : AlarmState.RETURNED_UNACKED;
				case ACKED -> next = active ? was : AlarmState.NORMAL;
				case RETURNED_UNACKED -> next = active ? AlarmState.UNACKED : was;
				case SHELVED -> {
					if (seconds >= shelved.getOrDefault(i, 0)) {
						next = active ? AlarmState.UNACKED : AlarmState.NORMAL;
					}
				}
			}
			if (next != was) {
				states.put(i, next);
				note(next + ": " + row[4]);
			}
		}
		Set<String> wanted = new HashSet<>();
		for (String widget : layout) {
			String[] row = fields(widget, 7);
			if ("TREND".equals(row[0]) && row[2] != null) {
				wanted.add(row[2].toUpperCase(Locale.ROOT));
			}
		}
		trends.keySet().retainAll(wanted);
		for (String tag : wanted) {
			double value = number(NetworkNet.read(plcs, tag));
			Deque<Float> trend = trends.computeIfAbsent(tag, t -> new ArrayDeque<>());
			trend.addLast(Double.isNaN(value) ? Float.NaN : (float) value);
			while (trend.size() > TREND_SAMPLES) {
				trend.removeFirst();
			}
		}
		setChanged();
	}

	private void note(String event) {
		history.addFirst(String.format(Locale.ROOT, "%d s  %s", seconds, event));
		while (history.size() > HISTORY) {
			history.removeLast();
		}
	}

	public void open(ServerPlayer player) {
		watchers.add(player.getUUID());
		send(player, true, List.of());
	}

	@Override
	public void request(ServerPlayer player, CompoundTag tag) {
		List<String> messages = new ArrayList<>();
		switch (tag.getStringOr("action", "")) {
			case "save" -> {
				layout.clear();
				alarms.clear();
				tag.getListOrEmpty("layout").forEach(t -> t.asString().ifPresent(s -> {
					if (layout.size() < MOST_ROWS) {
						layout.add(s);
					}
				}));
				tag.getListOrEmpty("alarms").forEach(t -> t.asString().ifPresent(s -> {
					if (alarms.size() < MOST_ROWS) {
						alarms.add(s);
					}
				}));
				states.keySet().removeIf(i -> i >= alarms.size());
				setChanged();
			}
			case "write" -> {
				String name = tag.getStringOr("tag", "");
				Object now = NetworkNet.read(plcs(), name);
				String value = tag.getStringOr("value", "");
				Object parsed = now instanceof Boolean ? (Object) (value.equalsIgnoreCase("TRUE") || value.equals("1")) : now instanceof Double ? parse(value) : now instanceof Long ? (Object) Math.round(parse(value)) : value;
				if (now == null || !NetworkNet.write(plcs(), name, parsed)) {
					messages.add("Could not write " + name);
				}
			}
			case "ack" -> {
				int index = tag.getIntOr("index", -1);
				AlarmState state = states.getOrDefault(index, AlarmState.NORMAL);
				if (state == AlarmState.UNACKED || state == AlarmState.RETURNED_UNACKED) {
					states.put(index, state == AlarmState.UNACKED ? AlarmState.ACKED : AlarmState.NORMAL);
					note("ACKNOWLEDGED: " + fields(alarms.get(index), 5)[4]);
				}
			}
			case "shelve" -> {
				int index = tag.getIntOr("index", -1);
				if (index >= 0 && index < alarms.size()) {
					states.put(index, AlarmState.SHELVED);
					shelved.put(index, seconds + SHELF_SECONDS);
					note("SHELVED: " + fields(alarms.get(index), 5)[4]);
				}
			}
			case "close" -> {
				watchers.remove(player.getUUID());
				return;
			}
			default -> {
				return;
			}
		}
		setChanged();
		send(player, false, messages);
	}

	private static double parse(String text) {
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void send(ServerPlayer player, boolean open, List<String> messages) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("hmi", true);
		tag.putInt("x", worldPosition.getX());
		tag.putInt("y", worldPosition.getY());
		tag.putInt("z", worldPosition.getZ());
		tag.putBoolean("open", open);
		tag.put("layout", strings(layout));
		tag.put("alarms", strings(alarms));
		List<PlcCabinetBlockEntity> plcs = plcs();
		tag.putInt("plcs", plcs.size());
		ListTag values = new ListTag();
		for (String widget : layout) {
			String name = fields(widget, 7)[2];
			Object value = name == null ? null : NetworkNet.read(plcs, name);
			values.add(StringTag.valueOf(value == null ? "?" : value.toString()));
		}
		tag.put("values", values);
		List<String> shown = new ArrayList<>();
		for (int i = 0; i < alarms.size(); i++) {
			shown.add(states.getOrDefault(i, AlarmState.NORMAL).name());
		}
		tag.put("states", strings(shown));
		tag.put("history", strings(new ArrayList<>(history)));
		CompoundTag trendTag = new CompoundTag();
		trends.forEach((name, trend) -> {
			StringBuilder out = new StringBuilder();
			trend.forEach(v -> out.append(String.format(Locale.ROOT, "%.3g,", v)));
			trendTag.putString(name, out.toString());
		});
		tag.put("trends", trendTag);
		tag.put("messages", strings(messages));
		Platform.INSTANCE.sendToPlayer(player, new PlcNet.Payload(tag));
	}

	private static ListTag strings(List<String> list) {
		ListTag out = new ListTag();
		list.forEach(s -> out.add(StringTag.valueOf(s)));
		return out;
	}

	@Override
	public Component inspect() {
		long up = alarms.isEmpty() ? 0 : states.values().stream().filter(s -> s == AlarmState.UNACKED || s == AlarmState.RETURNED_UNACKED).count();
		return Component.literal("HMI panel: " + plcs().size() + " PLC(s) on its network, " + layout.size() + " items on its screen, " + up + " alarm(s) to acknowledge");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("layout", Codec.STRING.listOf(), layout);
		output.store("alarms", Codec.STRING.listOf(), alarms);
		output.store("history", Codec.STRING.listOf(), new ArrayList<>(history));
		output.putInt("seconds", seconds);
		Map<String, String> savedStates = new HashMap<>();
		states.forEach((k, v) -> savedStates.put(Integer.toString(k), v.name()));
		output.store("states", Codec.unboundedMap(Codec.STRING, Codec.STRING), savedStates);
		Map<String, String> savedTrends = new HashMap<>();
		trends.forEach((k, v) -> {
			StringBuilder out = new StringBuilder();
			v.forEach(f -> out.append(f).append(','));
			savedTrends.put(k, out.toString());
		});
		output.store("trends", Codec.unboundedMap(Codec.STRING, Codec.STRING), savedTrends);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		layout.clear();
		layout.addAll(input.read("layout", Codec.STRING.listOf()).orElse(List.of()));
		alarms.clear();
		alarms.addAll(input.read("alarms", Codec.STRING.listOf()).orElse(List.of()));
		history.clear();
		history.addAll(input.read("history", Codec.STRING.listOf()).orElse(List.of()));
		seconds = input.getIntOr("seconds", 0);
		input.read("states", Codec.unboundedMap(Codec.STRING, Codec.STRING)).orElse(Map.of()).forEach((k, v) -> states.put(Integer.parseInt(k), AlarmState.valueOf(v)));
		input.read("trends", Codec.unboundedMap(Codec.STRING, Codec.STRING)).orElse(Map.of()).forEach((k, v) -> {
			Deque<Float> trend = new ArrayDeque<>();
			for (String sample : v.split(",")) {
				if (!sample.isEmpty()) {
					trend.addLast(Float.parseFloat(sample));
				}
			}
			trends.put(k, trend);
		});
	}
}
