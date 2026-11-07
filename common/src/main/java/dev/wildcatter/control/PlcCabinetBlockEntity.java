package dev.wildcatter.control;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.plc.Cpu;
import dev.wildcatter.plc.CpuSpec;
import dev.wildcatter.plc.Diagnostic;
import dev.wildcatter.plc.IoAddress;
import dev.wildcatter.plc.lang.CompileError;
import dev.wildcatter.plc.lang.PlcCompiler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A PLC cabinet: the rail of modules, the project, the terminals, and the {@link Cpu} they run on. Each tick, with 24 V, it reads every landed
 * input, scans once and writes every landed output (docs/multiblocks/plc.md).
 */
public class PlcCabinetBlockEntity extends BlockEntity implements Inspectable, BigPartEntity {
	/** The CPU's slot, then the module slots. */
	public static final int MODULE_SLOTS = 12;
	/** Each module slot owns this many bytes of the input and of the output area. */
	public static final int BYTES_PER_SLOT = 16;
	private static final int WALK_TICKS = 10;
	private static final int STATE_TICKS = 10;

	/** One signal landed on one channel. */
	public record Landing(BlockPos pos, int point, int slot, int channel) {
	}

	private PlcModuleItem.@org.jspecify.annotations.Nullable Type cpuModule;
	private final PlcModuleItem.Type[] modules = new PlcModuleItem.Type[MODULE_SLOTS];
	private String source = "";
	private final List<Landing> landings = new ArrayList<>();
	private final Map<BlockPos, String> tags = new HashMap<>();
	private Cpu cpu;
	private String compileText = "";
	private boolean grown;
	private ControlNet.Found net = new ControlNet.Found(List.of(), false, null);
	private boolean powered;
	private Cpu.State loaded;
	private boolean loadedRunning;
	private final Set<UUID> watchers = new HashSet<>();
	private final Map<UUID, List<String>> watching = new HashMap<>();

	public PlcCabinetBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PLC_CABINET_ENTITY, pos, state);
	}

	public Cpu cpu() {
		return cpu;
	}

	public boolean powered() {
		return powered;
	}

	public ControlNet.Found net() {
		return net;
	}

	public String source() {
		return source;
	}

	// ---- the rail --------------------------------------------------------------------------------------------------------------------

	/** Seats a module in the next free slot; why not, or null when it went in. */
	public String seat(PlcModuleItem.Type type) {
		if (type.cpu != null) {
			if (cpuModule != null) {
				return "This cabinet already has a CPU";
			}
			cpuModule = type;
			compile();
			setChanged();
			return null;
		}
		for (int i = 0; i < modules.length; i++) {
			if (modules[i] == null) {
				modules[i] = type;
				setChanged();
				return null;
			}
		}
		return "The rail is full: " + MODULE_SLOTS + " modules";
	}

	public ItemStack unseatLast() {
		for (int i = modules.length - 1; i >= 0; i--) {
			if (modules[i] != null) {
				ItemStack out = item(modules[i]);
				modules[i] = null;
				setChanged();
				return out;
			}
		}
		if (cpuModule != null) {
			ItemStack out = item(cpuModule);
			cpuModule = null;
			cpu = null;
			setChanged();
			return out;
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack item(PlcModuleItem.Type type) {
		for (var item : BuiltInRegistries.ITEM) {
			if (item instanceof PlcModuleItem module && module.type() == type) {
				return new ItemStack(item);
			}
		}
		return ItemStack.EMPTY;
	}

	private void compile() {
		cpu = null;
		compileText = "";
		if (cpuModule == null) {
			return;
		}
		PlcCompiler.Result result = PlcCompiler.compile(cpuModule.cpu, source);
		if (result.ok()) {
			cpu = new Cpu(cpuModule.cpu, result.program());
		} else {
			compileText = result.errors().stream().map(CompileError::toString).collect(Collectors.joining("\n"));
		}
	}

	/** A new project: compiled here, and on success it replaces the old one, the CPU in STOP. The errors, or empty. */
	public List<CompileError> download(String text) {
		CpuSpec spec = cpuModule == null ? CpuSpec.SMALL : cpuModule.cpu;
		PlcCompiler.Result result = PlcCompiler.compile(spec, text);
		if (!result.ok()) {
			return result.errors();
		}
		source = text;
		compileText = "";
		if (cpuModule != null) {
			cpu = new Cpu(spec, result.program());
		}
		setChanged();
		return List.of();
	}

	// ---- terminals -------------------------------------------------------------------------------------------------------------------

	/** The module a channel address needs, and where: why not, or null when it landed. An empty address clears the signal. */
	public String land(BlockPos pos, int point, String address) {
		ControlDevice device = ControlNet.deviceAt(level, pos);
		if (device == null || point < 0 || point >= device.points().size()) {
			return "That device is gone";
		}
		landings.removeIf(l -> l.pos().equals(pos) && l.point() == point);
		setChanged();
		if (address.isBlank()) {
			return null;
		}
		IoAddress io = IoAddress.parse(address);
		boolean output = device.points().get(point).output();
		if (io == null || io.size() != 'X' || io.area() != (output ? 'Q' : 'I')) {
			return address + " is not " + (output ? "an output bit such as %QX16.0" : "an input bit such as %IX0.3") + ", which this signal needs";
		}
		int slot = io.byteAddress() / BYTES_PER_SLOT;
		int channel = (io.byteAddress() % BYTES_PER_SLOT) * 8 + io.bit();
		if (slot >= MODULE_SLOTS || modules[slot] != (output ? PlcModuleItem.Type.DO16 : PlcModuleItem.Type.DI16)) {
			return "Slot " + slot + " has no " + (output ? "DO16" : "DI16") + " module";
		}
		if (channel >= 16) {
			return "A 16-channel module has channels 0 to 15; that is channel " + channel;
		}
		for (Landing other : landings) {
			boolean otherOutput = ControlNet.deviceAt(level, other.pos()) instanceof ControlDevice d && other.point() < d.points().size() && d.points().get(other.point()).output();
			if (other.slot() == slot && other.channel() == channel && otherOutput == output) {
				return address + " already has a signal landed on it";
			}
		}
		landings.add(new Landing(pos, point, slot, channel));
		return null;
	}

	public void setTag(BlockPos pos, String tag) {
		tags.put(pos, tag);
		setChanged();
	}

	private static String address(boolean output, int slot, int channel) {
		return (output ? "%QX" : "%IX") + (slot * BYTES_PER_SLOT + channel / 8) + "." + (channel % 8);
	}

	// ---- the scan --------------------------------------------------------------------------------------------------------------------

	public static void tick(Level level, BlockPos pos, BlockState state, PlcCabinetBlockEntity cabinet) {
		cabinet.run(level);
	}

	private void run(Level level) {
		long now = level.getGameTime();
		if (loaded != null || (cpu == null && cpuModule != null && compileText.isEmpty())) {
			compile();
			if (cpu != null && loaded != null) {
				cpu.restore(loaded);
			}
			loaded = null;
		}
		if (now % WALK_TICKS == 0) {
			net = ControlNet.walk(level, BigParts.cells(worldPosition, getBlockState()));
		}
		boolean wasPowered = powered;
		powered = net.supplyLive();
		if (cpu != null) {
			if (powered && !wasPowered) {
				cpu.powerRestored();
			} else if (!powered && wasPowered) {
				cpu.powerLost();
			}
		}
		if (powered && cpu != null) {
			scan(level, now);
		}
		if (now % STATE_TICKS == 0 && !watchers.isEmpty() && level.getServer() != null) {
			for (UUID id : List.copyOf(watchers)) {
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
				if (player == null || player.blockPosition().distSqr(worldPosition) > 64) {
					watchers.remove(id);
					watching.remove(id);
				} else {
					send(player, false, List.of());
				}
			}
		}
	}

	private void scan(Level level, long now) {
		Set<BlockPos> reachable = new HashSet<>(net.devices());
		for (Landing l : landings) {
			ControlDevice device = reachable.contains(l.pos()) ? ControlNet.deviceAt(level, l.pos()) : null;
			if (modules[l.slot()] == PlcModuleItem.Type.DI16) {
				boolean on = device != null && l.point() < device.points().size() && !device.points().get(l.point()).output() && device.readPoint(l.point());
				cpu.setInputBit(l.slot() * BYTES_PER_SLOT + l.channel() / 8, l.channel() % 8, on);
			}
		}
		cpu.scan();
		for (Landing l : landings) {
			ControlDevice device = reachable.contains(l.pos()) ? ControlNet.deviceAt(level, l.pos()) : null;
			if (device != null && modules[l.slot()] == PlcModuleItem.Type.DO16 && l.point() < device.points().size() && device.points().get(l.point()).output()) {
				device.commandPoint(l.point(), cpu.outputBit(l.slot() * BYTES_PER_SLOT + l.channel() / 8, l.channel() % 8), now);
			}
		}
		setChanged();
	}

	// ---- the screen ------------------------------------------------------------------------------------------------------------------

	public void open(ServerPlayer player) {
		watchers.add(player.getUUID());
		net = ControlNet.walk(level, BigParts.cells(worldPosition, getBlockState()));
		send(player, true, List.of());
	}

	/** What a screen asks. {@code action} says which. */
	public void request(ServerPlayer player, CompoundTag tag) {
		List<String> messages = new ArrayList<>();
		switch (tag.getStringOr("action", "")) {
			case "download" -> {
				List<CompileError> errors = download(tag.getStringOr("source", ""));
				messages.add(errors.isEmpty() ? "Downloaded. The CPU is in STOP." : "Download refused: " + errors.size() + " error(s)");
				errors.forEach(e -> messages.add(e.toString()));
			}
			case "key" -> {
				if (cpu != null) {
					cpu.setMode(tag.getBooleanOr("run", false) ? Cpu.Mode.RUN : Cpu.Mode.STOP);
				}
			}
			case "cold" -> {
				if (cpu != null) {
					cpu.start(Cpu.Restart.COLD);
				}
			}
			case "land" -> {
				BlockPos at = new BlockPos(tag.getIntOr("dx", 0), tag.getIntOr("dy", 0), tag.getIntOr("dz", 0));
				String refused = land(at, tag.getIntOr("point", -1), tag.getStringOr("address", ""));
				if (refused != null) {
					messages.add(refused);
				}
			}
			case "tag" -> setTag(new BlockPos(tag.getIntOr("dx", 0), tag.getIntOr("dy", 0), tag.getIntOr("dz", 0)), tag.getStringOr("tag", ""));
			case "watch" -> {
				List<String> names = new ArrayList<>();
				tag.getListOrEmpty("names").forEach(t -> t.asString().ifPresent(names::add));
				watching.put(player.getUUID(), names);
			}
			case "write" -> {
				if (cpu != null) {
					String path = tag.getStringOr("tag", "");
					String value = tag.getStringOr("value", "");
					Object now = cpu.read(path);
					Object parsed = now instanceof Boolean ? (Object) (value.equalsIgnoreCase("TRUE") || value.equals("1")) : now instanceof Double ? parseDouble(value) : now instanceof Long ? parseLong(value) : value;
					if (parsed == null || !cpu.write(path, parsed)) {
						messages.add("Could not write " + path + ": located variables are forced, not written");
					}
				}
			}
			case "force" -> {
				if (cpu != null) {
					try {
						cpu.force(tag.getStringOr("tag", ""), tag.getStringOr("value", ""));
					} catch (IllegalArgumentException e) {
						messages.add(e.getMessage());
					}
				}
			}
			case "release" -> {
				if (cpu != null) {
					cpu.release(tag.getStringOr("tag", ""));
				}
			}
			case "close" -> {
				watchers.remove(player.getUUID());
				watching.remove(player.getUUID());
				return;
			}
			default -> {
				return;
			}
		}
		send(player, false, messages);
	}

	private static Double parseDouble(String text) {
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long parseLong(String text) {
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void send(ServerPlayer player, boolean open, List<String> messages) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("x", worldPosition.getX());
		tag.putInt("y", worldPosition.getY());
		tag.putInt("z", worldPosition.getZ());
		tag.putBoolean("open", open);
		if (open) {
			tag.putString("source", source);
			ListTag names = new ListTag();
			if (cpu != null) {
				new TreeSet<>(cpu.program().tags().keySet()).stream().filter(n -> !n.contains("[") && !isLibrary(n)).limit(1024).forEach(n -> names.add(StringTag.valueOf(n)));
			}
			tag.put("names", names);
		}
		tag.putString("status", statusText());
		tag.putString("cpu", cpuModule == null ? "" : cpuModule.cpu.name());
		tag.putString("rail", railText());
		tag.putString("compile", compileText);
		tag.putBoolean("powered", powered);
		if (cpu != null) {
			tag.putBoolean("running", cpu.mode() == Cpu.Mode.RUN);
			tag.putBoolean("faulted", cpu.faulted());
			tag.putBoolean("forcing", cpu.forcing());
			tag.putInt("instructions", cpu.lastInstructions());
			tag.putInt("budget", cpu.spec().instructionsPerScan());
		}
		ListTag said = new ListTag();
		messages.forEach(m -> said.add(StringTag.valueOf(m)));
		tag.put("messages", said);
		ListTag diagnostics = new ListTag();
		if (cpu != null) {
			for (Diagnostic d : cpu.diagnostics()) {
				diagnostics.add(StringTag.valueOf(String.format("%8.1f s  %s", d.timeMs() / 1000.0, d)));
			}
		}
		tag.put("diagnostics", diagnostics);
		tag.put("devices", devicesTag());
		ListTag values = new ListTag();
		if (cpu != null) {
			for (String name : watching.getOrDefault(player.getUUID(), List.of())) {
				Object value = name.startsWith("%") ? null : cpu.read(name);
				values.add(StringTag.valueOf(name + "=" + (value == null ? "?" : value)));
			}
			cpu.forces().forEach((k, v) -> values.add(StringTag.valueOf(k + "=" + v + " (FORCED)")));
		}
		tag.put("values", values);
		dev.wildcatter.platform.Platform.INSTANCE.sendToPlayer(player, new PlcNet.Payload(tag));
	}

	/** The standard library's own variables would drown the list. */
	private static boolean isLibrary(String name) {
		return name.matches(".*\\.(START|RUNNING|LAST|LASTU|LASTD|M|INTEGRAL|LAST_PV|DT|GAIN|P|D|RAW|STARTED)$");
	}

	private ListTag devicesTag() {
		ListTag list = new ListTag();
		for (BlockPos pos : net.devices()) {
			ControlDevice device = ControlNet.deviceAt(level, pos);
			if (device == null) {
				continue;
			}
			CompoundTag entry = new CompoundTag();
			entry.putInt("x", pos.getX());
			entry.putInt("y", pos.getY());
			entry.putInt("z", pos.getZ());
			entry.putString("name", device.deviceName());
			entry.putString("tag", tags.getOrDefault(pos, ""));
			ListTag points = new ListTag();
			List<ControlDevice.Point> all = device.points();
			for (int i = 0; i < all.size(); i++) {
				int point = i;
				CompoundTag row = new CompoundTag();
				row.putString("name", all.get(i).name());
				row.putBoolean("output", all.get(i).output());
				row.putString("address", landings.stream().filter(l -> l.pos().equals(pos) && l.point() == point).findFirst()
						.map(l -> address(all.get(point).output(), l.slot(), l.channel())).orElse(""));
				points.add(row);
			}
			entry.put("points", points);
			list.add(entry);
		}
		return list;
	}

	private String railText() {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < modules.length; i++) {
			if (modules[i] != null) {
				out.append(out.length() > 0 ? ", " : "").append(i).append(": ").append(modules[i]).append(" (%").append(modules[i] == PlcModuleItem.Type.DI16 ? "I" : "Q").append("X").append(i * BYTES_PER_SLOT).append(".0)");
			}
		}
		return out.length() == 0 ? "no modules" : out.toString();
	}

	private String statusText() {
		if (net.badJoint() != null) {
			return "BAD JOINT at " + net.badJoint().toShortString() + ": a cable makes one run; fan out at a junction box";
		}
		if (!powered) {
			return "NO 24 V: a live control power supply must reach it by control cable";
		}
		if (cpuModule == null) {
			return "NO CPU: click a CPU module into the cabinet";
		}
		if (cpu == null) {
			return source.isBlank() ? "NO PROJECT" : "The project does not compile: " + compileText;
		}
		return (cpu.faulted() ? "CPU FAULT" : cpu.mode() == Cpu.Mode.RUN ? "RUN" : "STOP") + (cpu.forcing() ? ", FORCING" : "");
	}

	@Override
	public Component inspect() {
		return Component.literal("PLC cabinet, " + (cpuModule == null ? "no CPU" : cpuModule.cpu.name()) + ": " + statusText() + "; " + railText() + "; " + landings.size() + " signals landed, "
				+ net.devices().size() + " devices reached");
	}

	// ---- saving ----------------------------------------------------------------------------------------------------------------------

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null) {
			ItemStack out;
			while (!(out = unseatLast()).isEmpty()) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), out);
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putString("cpuModule", cpuModule == null ? "" : cpuModule.name());
		output.store("modules", Codec.STRING.listOf(), java.util.Arrays.stream(modules).map(m -> m == null ? "" : m.name()).toList());
		output.store("source", Codec.STRING.listOf(), List.of(source.split("\n", -1)));
		int[] packed = new int[landings.size() * 6];
		for (int i = 0; i < landings.size(); i++) {
			Landing l = landings.get(i);
			packed[i * 6] = l.pos().getX();
			packed[i * 6 + 1] = l.pos().getY();
			packed[i * 6 + 2] = l.pos().getZ();
			packed[i * 6 + 3] = l.point();
			packed[i * 6 + 4] = l.slot();
			packed[i * 6 + 5] = l.channel();
		}
		output.putIntArray("landings", packed);
		output.store("tags", Codec.unboundedMap(Codec.STRING, Codec.STRING), tags.entrySet().stream().collect(Collectors.toMap(e -> Long.toString(e.getKey().asLong()), Map.Entry::getValue)));
		if (cpu != null) {
			Cpu.State state = cpu.save();
			output.store("mem", Codec.LONG_STREAM, java.util.Arrays.stream(state.mem()));
			Map<String, String> strings = new HashMap<>();
			for (int i = 0; i < state.str().length; i++) {
				if (state.str()[i] != null && !state.str()[i].isEmpty()) {
					strings.put(Integer.toString(i), state.str()[i]);
				}
			}
			output.store("str", Codec.unboundedMap(Codec.STRING, Codec.STRING), strings);
			output.store("markers", Codec.BYTE_BUFFER, java.nio.ByteBuffer.wrap(state.markers()));
			output.store("images", Codec.BYTE_BUFFER, java.nio.ByteBuffer.wrap(state.outputImage()));
			output.store("field", Codec.BYTE_BUFFER, java.nio.ByteBuffer.wrap(state.inputField()));
			output.putBoolean("run", state.mode() == Cpu.Mode.RUN);
			output.putBoolean("faulted", state.faulted());
			output.putLong("time", state.timeMs());
			output.putLong("scans", state.scans());
			output.store("forced", Codec.unboundedMap(Codec.STRING, Codec.STRING), state.forced());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		String name = input.getStringOr("cpuModule", "");
		cpuModule = name.isEmpty() ? null : PlcModuleItem.Type.valueOf(name);
		List<String> rail = input.read("modules", Codec.STRING.listOf()).orElse(List.of());
		for (int i = 0; i < modules.length && i < rail.size(); i++) {
			modules[i] = rail.get(i).isEmpty() ? null : PlcModuleItem.Type.valueOf(rail.get(i));
		}
		source = String.join("\n", input.read("source", Codec.STRING.listOf()).orElse(List.of()));
		landings.clear();
		int[] packed = input.getIntArray("landings").orElse(new int[0]);
		for (int i = 0; i + 5 < packed.length; i += 6) {
			landings.add(new Landing(new BlockPos(packed[i], packed[i + 1], packed[i + 2]), packed[i + 3], packed[i + 4], packed[i + 5]));
		}
		tags.clear();
		input.read("tags", Codec.unboundedMap(Codec.STRING, Codec.STRING)).orElse(Map.of()).forEach((k, v) -> tags.put(BlockPos.of(Long.parseLong(k)), v));
		long[] mem = input.read("mem", Codec.LONG_STREAM).map(java.util.stream.LongStream::toArray).orElse(null);
		if (mem != null && cpuModule != null) {
			String[] str = new String[mem.length];
			input.read("str", Codec.unboundedMap(Codec.STRING, Codec.STRING)).orElse(Map.of()).forEach((k, v) -> {
				int at = Integer.parseInt(k);
				if (at < str.length) {
					str[at] = v;
				}
			});
			CpuSpec spec = cpuModule.cpu;
			loaded = new Cpu.State(mem, str, bytes(input, "markers", spec.markerBytes()), bytes(input, "images", spec.outputBytes()), bytes(input, "field", spec.inputBytes()),
					input.getBooleanOr("run", false) ? Cpu.Mode.RUN : Cpu.Mode.STOP, input.getBooleanOr("faulted", false), input.getLongOr("time", 0), input.getLongOr("scans", 0),
					input.read("forced", Codec.unboundedMap(Codec.STRING, Codec.STRING)).orElse(Map.of()));
		}
	}

	private static byte[] bytes(ValueInput input, String name, int size) {
		byte[] out = new byte[size];
		input.read(name, Codec.BYTE_BUFFER).ifPresent(buffer -> {
			buffer.get(out, 0, Math.min(size, buffer.remaining()));
		});
		return out;
	}

}
