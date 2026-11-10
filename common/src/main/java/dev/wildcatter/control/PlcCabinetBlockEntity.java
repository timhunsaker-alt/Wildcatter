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
import dev.wildcatter.plc.lang.Project;
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
public class PlcCabinetBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, PlcRequests {
	private String station = "PLC";
	private List<RemoteIoDropBlockEntity> drops = List.of();
	private List<RemoteIoDropBlockEntity> reachedDrops = List.of();
	private final Map<Integer, String> foreignRacks = new java.util.TreeMap<>();
	/** Landings past their cable's cores: they carry nothing (docs/questions/plc-gaps.md). */
	private final Set<Landing> overCores = new HashSet<>();
	private String coreProblem = "";
	/** The 24 V this cabinet and its racks draw, at the last scan. */
	private double amps;
	private String loopFight = "";
	private List<PlcCabinetBlockEntity> peers = List.of();
	/** Tag exchange: rows of {@code LOCAL = STATION/REMOTE}, as a real controller's produced and consumed data. */
	private final List<String> exchange = new ArrayList<>();
	private final Set<String> exchangeFailed = new HashSet<>();
	private static final int EXCHANGE_TICKS = 10;

	public void addExchange(String row) {
		exchange.add(row);
		setChanged();
	}

	public List<String> exchange() {
		return exchange;
	}

	public Set<String> exchangeFailed() {
		return exchangeFailed;
	}

	public List<RemoteIoDropBlockEntity> drops() {
		return drops;
	}

	/** The module in a slot: its own for the first 12, then each rack's, 12 at a time; null if none, or if the rack is not on the network. */
	private PlcModuleItem.@org.jspecify.annotations.Nullable Type moduleAt(int slot) {
		if (slot < MODULE_SLOTS) {
			return modules[slot];
		}
		int rack = (slot - MODULE_SLOTS) / MODULE_SLOTS + 1;
		for (RemoteIoDropBlockEntity drop : drops) {
			if (drop.rack() == rack) {
				return drop.module((slot - MODULE_SLOTS) % MODULE_SLOTS);
			}
		}
		return null;
	}

	private int slotsInUse() {
		int most = MODULE_SLOTS;
		for (RemoteIoDropBlockEntity drop : drops) {
			most = Math.max(most, MODULE_SLOTS + drop.rack() * MODULE_SLOTS);
		}
		return most;
	}

	private Set<BlockPos> reachable() {
		Set<BlockPos> out = new HashSet<>(net.devices());
		for (RemoteIoDropBlockEntity drop : drops) {
			if (drop.live()) {
				out.addAll(drop.devices());
			}
		}
		return out;
	}

	private void walkNetwork(Level level) {
		List<BlockPos> cells = BigParts.cells(worldPosition, getBlockState());
		reachedDrops = hasComm() ? NetworkNet.all(level, cells, RemoteIoDropBlockEntity.class) : List.of();
		claimDrops(level.getGameTime());
		peers = hasComm() ? NetworkNet.plcs(level, cells).stream().filter(p -> p != this).toList() : List.of();
	}

	/** Copies each exchanged tag over, every half second, only while this CPU runs. A dead peer's tags hold their last value and are named as failed. */
	private void runExchange() {
		exchangeFailed.clear();
		for (String row : exchange) {
			int cut = row.indexOf('=');
			if (cut <= 0) {
				continue;
			}
			String local = row.substring(0, cut).trim().toUpperCase(java.util.Locale.ROOT);
			Object value = NetworkNet.read(peers, row.substring(cut + 1).trim());
			if (value == null || !cpu.write(local, value)) {
				exchangeFailed.add(row);
			}
		}
	}

	/** The racks on the network that this PLC owns; another PLC's are named, not scanned (a rack has one owner). */
	private void claimDrops(long now) {
		List<RemoteIoDropBlockEntity> owned = new ArrayList<>();
		foreignRacks.clear();
		for (RemoteIoDropBlockEntity drop : reachedDrops) {
			if (drop.isRemoved()) {
				continue;
			}
			if (powered && cpuModule != null && drop.claim(worldPosition, station, now)) {
				owned.add(drop);
			} else if (!drop.owner(now).isEmpty() && !drop.owner(now).equals(station)) {
				foreignRacks.put(drop.rack(), drop.owner(now));
			}
		}
		drops = owned;
	}

	public boolean hasComm() {
		return java.util.Arrays.asList(modules).contains(PlcModuleItem.Type.COMM);
	}

	/** Its name on the network. */
	public String station() {
		return station;
	}

	public void setStation(String name) {
		station = name;
		setChanged();
	}

	/** The CPU's slot, then the module slots. */
	public static final int MODULE_SLOTS = 12;
	/** Each module slot owns this many bytes of the input and of the output area. */
	public static final int BYTES_PER_SLOT = 16;
	private static final int WALK_TICKS = 10;
	private static final int STATE_TICKS = 5;

	/** One signal landed on one channel. */
	public record Landing(BlockPos pos, int point, int slot, int channel) {
	}

	private PlcModuleItem.@org.jspecify.annotations.Nullable Type cpuModule;
	private final PlcModuleItem.Type[] modules = new PlcModuleItem.Type[MODULE_SLOTS];
	private List<Project.Source> project = new ArrayList<>();
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
	private final Map<UUID, StringBuilder> downloads = new HashMap<>();
	/** A project's limit, against a player filling a server's memory. */
	private static final int MOST_PROJECT = 4_000_000;
	/** A string in a packet holds 65,535 bytes, so a project opening in a screen goes in pieces of this many characters. */
	private static final int OPEN_PIECE = 20_000;
	private ItemStack card = ItemStack.EMPTY;
	/** What an AI8 channel reads with no loop on it. */
	public static final int OPEN_LOOP = 32767;

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

	public List<Project.Source> project() {
		return project;
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

	/** A memory card into the door; why not, or null when it went in. */
	public String seatCard(ItemStack stack) {
		if (!card.isEmpty()) {
			return "This cabinet already has a card in it";
		}
		card = stack.copyWithCount(1);
		setChanged();
		return null;
	}

	public ItemStack card() {
		return card;
	}

	/** The project to the card; why not, or null. */
	public String toCard() {
		if (card.isEmpty()) {
			return "There is no card in the door";
		}
		if (project.isEmpty()) {
			return "The cabinet has no project to copy";
		}
		String refused = MemoryCardItem.write(card, project);
		setChanged();
		return refused;
	}

	/** Why the card cannot load, or null. */
	public String cardProblem() {
		return card.isEmpty() ? "There is no card in the door" : MemoryCardItem.read(card) == null ? "The card is blank" : null;
	}

	/** The card's project to the cabinet, as a download is: compiled first, and the CPU left in STOP. The errors, or empty. */
	public List<String> fromCard() {
		return download(MemoryCardItem.read(card), false).stream().map(CompileError::toString).toList();
	}

	public ItemStack unseatLast() {
		if (!card.isEmpty()) {
			ItemStack out = card;
			card = ItemStack.EMPTY;
			setChanged();
			return out;
		}
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
		if (project.isEmpty()) {
			return;
		}
		PlcCompiler.Result result = PlcCompiler.compile(cpuModule.cpu, project);
		if (result.ok()) {
			cpu = new Cpu(cpuModule.cpu, result.program());
		} else {
			compileText = result.errors().stream().map(CompileError::toString).collect(Collectors.joining("\n"));
		}
	}

	/** A new project: compiled here, and on success it replaces the old one, the CPU in STOP. The errors, or empty. */
	public List<CompileError> download(String text) {
		return download(Project.of(text), false);
	}

	/** {@code online} keeps the CPU running on the new code with its values, if a project is running; otherwise as a download. */
	public List<CompileError> download(List<Project.Source> next, boolean online) {
		CpuSpec spec = cpuModule == null ? CpuSpec.SMALL : cpuModule.cpu;
		PlcCompiler.Result result = PlcCompiler.compile(spec, next);
		if (!result.ok()) {
			return result.errors();
		}
		project = new ArrayList<>(next);
		compileText = "";
		if (cpuModule != null) {
			if (online && cpu != null) {
				cpu.onlineChange(result.program());
			} else {
				cpu = new Cpu(spec, result.program());
			}
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
		ControlDevice.Point wanted = device.points().get(point);
		boolean output = wanted.output();
		char size = switch (wanted.signal()) {
			case BOOL -> 'X';
			case MA -> 'W';
			case PULSE -> 'D';
		};
		if (io == null || io.size() != size || io.area() != (output ? 'Q' : 'I')) {
			String example = switch (wanted.signal()) {
				case BOOL -> output ? "an output bit such as %QX16.0" : "an input bit such as %IX0.3";
				case MA -> output ? "an output word such as %QW48" : "an input word such as %IW32";
				case PULSE -> "an input double word such as %ID64";
			};
			return address + " is not " + example + ", which this signal needs";
		}
		int slot = io.byteAddress() / BYTES_PER_SLOT;
		int offset = io.byteAddress() % BYTES_PER_SLOT;
		PlcModuleItem.Type module = moduleAt(slot);
		if (cpu != null && io.byteAddress() + io.bytes() > (output ? cpu.spec().outputBytes() : cpu.spec().inputBytes())) {
			return address + " is past this CPU's I/O: a large CPU reaches further racks";
		}
		if (module == null && slot >= MODULE_SLOTS && foreignRacks.containsKey((slot - MODULE_SLOTS) / MODULE_SLOTS + 1)) {
			int rack = (slot - MODULE_SLOTS) / MODULE_SLOTS + 1;
			return "Rack " + rack + " belongs to " + foreignRacks.get(rack) + ": a rack has one owner";
		}
		int channel;
		switch (wanted.signal()) {
			case BOOL -> {
				channel = offset * 8 + io.bit();
				if (module != (output ? PlcModuleItem.Type.DO16 : PlcModuleItem.Type.DI16)) {
					return "Slot " + slot + " has no " + (output ? "DO16" : "DI16") + " module";
				}
				if (channel >= 16) {
					return "A 16-channel module has channels 0 to 15; that is channel " + channel;
				}
			}
			case MA -> {
				if (offset % 2 != 0) {
					return "An analog channel is a whole word, on an even address";
				}
				channel = offset / 2;
				boolean rtd = module == PlcModuleItem.Type.RTD4 && !Double.isNaN(device.readTenths(point));
				if (output ? module != PlcModuleItem.Type.AO4 : module != PlcModuleItem.Type.AI8 && !rtd) {
					return "Slot " + slot + " has no " + (output ? "AO4" : "AI8") + " module" + (module == PlcModuleItem.Type.RTD4 ? ", and this signal is not a temperature for an RTD4" : "");
				}
				if (channel >= (module == PlcModuleItem.Type.AI8 ? 8 : 4)) {
					return "That module has channels 0 to " + ((module == PlcModuleItem.Type.AI8 ? 8 : 4) - 1) + "; that is channel " + channel;
				}
			}
			default -> {
				if (offset % 4 != 0) {
					return "A counter is a double word, on an address divisible by 4";
				}
				channel = offset / 4;
				if (module != PlcModuleItem.Type.HSC2) {
					return "Slot " + slot + " has no HSC2 module";
				}
				if (channel >= 2) {
					return "An HSC2 has counters 0 and 1; that is counter " + channel;
				}
			}
		}
		for (Landing other : landings) {
			boolean otherOutput = ControlNet.deviceAt(level, other.pos()) instanceof ControlDevice d && other.point() < d.points().size() && d.points().get(other.point()).output();
			if (other.slot() == slot && other.channel() == channel && otherOutput == output) {
				return address + " already has a signal landed on it";
			}
		}
		Landing landing = new Landing(pos, point, slot, channel);
		landings.add(landing);
		countCores();
		if (overCores.contains(landing)) {
			landings.remove(landing);
			String full = coreProblem;
			countCores();
			return full + ". Lay a second cable beside it, or put a remote drop out there";
		}
		return null;
	}

	/** The net a landed device is reached through: this cabinet's, or a rack's. */
	private ControlNet.@org.jspecify.annotations.Nullable Found netOf(BlockPos device) {
		if (net.devices().contains(device)) {
			return net;
		}
		for (RemoteIoDropBlockEntity drop : drops) {
			if (drop.devices().contains(device)) {
				return drop.net();
			}
		}
		return null;
	}

	/**
	 * Counts the cores every landed signal takes in each cable on its way, in the order they were landed; any that find a cable full carry
	 * nothing, and the first full cable is named.
	 */
	private void countCores() {
		overCores.clear();
		coreProblem = "";
		Map<ControlNet.Link, int[]> used = new HashMap<>();
		for (Landing l : landings) {
			ControlNet.Found found = netOf(l.pos());
			ControlDevice device = found == null ? null : ControlNet.deviceAt(level, l.pos());
			if (device == null || l.point() >= device.points().size()) {
				continue;
			}
			int cores = ControlNet.coresOf(device.points().get(l.point()));
			List<ControlNet.Link> route = found.route(l.pos());
			ControlNet.Link full = null;
			for (ControlNet.Link link : route) {
				if (link.cabled() && used.computeIfAbsent(link, k -> new int[1])[0] + cores > link.cores()) {
					full = link;
					break;
				}
			}
			if (full != null) {
				overCores.add(l);
				if (coreProblem.isEmpty()) {
					coreProblem = "CABLE FULL at " + full.cable().toShortString() + ": its " + full.cores() + " signal cores are taken";
				}
				continue;
			}
			for (ControlNet.Link link : route) {
				if (link.cabled()) {
					used.computeIfAbsent(link, k -> new int[1])[0] += cores;
				}
			}
		}
	}

	public void setTag(BlockPos pos, String tag) {
		tags.put(pos, tag);
		setChanged();
	}

	private String address(boolean output, int slot, int channel) {
		int base = slot * BYTES_PER_SLOT;
		PlcModuleItem.Type module = moduleAt(slot);
		if (module == PlcModuleItem.Type.HSC2) {
			return "%ID" + (base + channel * 4);
		}
		if (module == PlcModuleItem.Type.AI8 || module == PlcModuleItem.Type.AO4 || module == PlcModuleItem.Type.RTD4) {
			return (output ? "%QW" : "%IW") + (base + channel * 2);
		}
		return (output ? "%QX" : "%IX") + (base + channel / 8) + "." + (channel % 8);
	}

	/** A channel's raw value for {@code milliamps}: 4 mA is 0, 20 mA is 32000. Out of NE 43's bounds, an open or failed loop, is 32767. */
	public static int rawOf(double milliamps) {
		if (milliamps < 3.6 || milliamps > 21) {
			return OPEN_LOOP;
		}
		return (int) Math.round((milliamps - 4) / 16 * 32000);
	}

	/** The loop current a raw output value gives. */
	public static double milliampsOf(int raw) {
		return Math.max(3.8, Math.min(20.5, 4 + (short) raw / 32000.0 * 16));
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
			walkNetwork(level);
			countCores();
		} else if (!reachedDrops.isEmpty()) {
			claimDrops(now);
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
		Set<BlockPos> reachable = reachable();
		int slots = slotsInUse();
		for (int slot = 0; slot < slots; slot++) {
			PlcModuleItem.Type here = moduleAt(slot);
			if ((here == PlcModuleItem.Type.AI8 || here == PlcModuleItem.Type.RTD4) && (slot + 1) * BYTES_PER_SLOT <= cpu.spec().inputBytes()) {
				for (int channel = 0; channel < (here == PlcModuleItem.Type.AI8 ? 8 : 4); channel++) {
					cpu.setInputWord(slot * BYTES_PER_SLOT + channel * 2, OPEN_LOOP);   // an empty channel, or a cut one, reads as an open loop
				}
			}
		}
		for (Landing l : landings) {
			ControlDevice device = reachable.contains(l.pos()) && !overCores.contains(l) ? ControlNet.deviceAt(level, l.pos()) : null;
			if (device != null && l.point() >= device.points().size()) {
				device = null;
			}
			int base = l.slot() * BYTES_PER_SLOT;
			PlcModuleItem.Type module = moduleAt(l.slot());
			if (module == PlcModuleItem.Type.DI16) {
				boolean on = device != null && !device.points().get(l.point()).output() && device.readPoint(l.point());
				cpu.setInputBit(base + l.channel() / 8, l.channel() % 8, on);
			} else if (module == PlcModuleItem.Type.AI8 && device != null) {
				cpu.setInputWord(base + l.channel() * 2, rawOf(device.readMa(l.point())));
			} else if (module == PlcModuleItem.Type.RTD4 && device != null && !Double.isNaN(device.readTenths(l.point()))) {
				cpu.setInputWord(base + l.channel() * 2, (int) Math.round(device.readTenths(l.point())));
			} else if (module == PlcModuleItem.Type.HSC2) {
				long count = device == null ? 0 : device.readPulses(l.point());
				cpu.setInputWord(base + l.channel() * 4, (int) (count & 0xFFFF));
				cpu.setInputWord(base + l.channel() * 4 + 2, (int) (count >>> 16 & 0xFFFF));
			}
		}
		if (now % EXCHANGE_TICKS == 0 && !exchange.isEmpty() && cpu.mode() == Cpu.Mode.RUN) {
			runExchange();
		}
		cpu.scan();
		// a stopped or faulted CPU writes no analog signal, so the loop is dead rather than a false 4 mA
		boolean writesAnalog = cpu.mode() == Cpu.Mode.RUN && !cpu.faulted();
		// the 24 V drawn: the CPU and each module's electronics, then each signal by what it carries (docs/questions/plc-gaps.md)
		double here = CPU_AMPS.getOrDefault(cpuModule, 0.0);
		for (PlcModuleItem.Type module : modules) {
			here += module == null ? 0 : module == PlcModuleItem.Type.COMM ? COMM_AMPS : MODULE_AMPS;
		}
		Map<RemoteIoDropBlockEntity, double[]> rackAmps = new HashMap<>();
		for (RemoteIoDropBlockEntity drop : drops) {
			double rail = ADAPTER_AMPS;
			for (int i = 0; i < MODULE_SLOTS; i++) {
				rail += drop.module(i) == null ? 0 : MODULE_AMPS;
			}
			rackAmps.put(drop, new double[] {rail});
		}
		loopFight = "";
		for (Landing l : landings) {
			ControlDevice device = reachable.contains(l.pos()) && !overCores.contains(l) ? ControlNet.deviceAt(level, l.pos()) : null;
			if (device == null || l.point() >= device.points().size()) {
				continue;
			}
			int base = l.slot() * BYTES_PER_SLOT;
			PlcModuleItem.Type module = moduleAt(l.slot());
			double draw = 0;
			if (!device.points().get(l.point()).output()) {
				if (module == PlcModuleItem.Type.DI16 && device.readPoint(l.point())) {
					draw = DI_AMPS;
				} else if (module == PlcModuleItem.Type.AI8) {
					double ma = device.readMa(l.point());
					draw = ma >= 3.6 && ma <= 21 ? ma / 1000 : 0;   // a loop-powered transmitter takes its loop current from the 24 V
				}
			} else if (module == PlcModuleItem.Type.DO16) {
				boolean mine = cpu.outputBit(base + l.channel() / 8, l.channel() % 8);
				device.commandPoint(l.point(), SharedOutputs.bool(level, l.pos(), l.point(), worldPosition, mine, now), now);
				draw = mine ? RELAY_AMPS + device.outputAmps(l.point()) : 0;   // a contact in parallel with another PLC's carries its share; counted whole here
			} else if (module == PlcModuleItem.Type.AO4 && writesAnalog) {
				double ma = SharedOutputs.milliamps(level, l.pos(), l.point(), worldPosition, milliampsOf(cpu.outputWord(base + l.channel() * 2)), now);
				if (ma == 0 && loopFight.isEmpty()) {
					loopFight = "TWO PLCs ON ONE LOOP: " + device.deviceName() + " " + device.points().get(l.point()).name() + " at " + l.pos().toShortString();
				}
				device.commandMa(l.point(), ma, now);
				draw = ma / 1000;
			}
			RemoteIoDropBlockEntity rack = l.slot() >= MODULE_SLOTS ? rackOf(l.slot()) : null;
			if (rack != null && rackAmps.containsKey(rack)) {
				rackAmps.get(rack)[0] += draw;
			} else {
				here += draw;
			}
		}
		amps = here;
		report(net, here, now);
		rackAmps.forEach((drop, a) -> report(drop.net(), a[0], now));
		setChanged();
	}

	/** A load to the supplies of a net, shared among them as supplies in parallel share it. */
	private void report(ControlNet.Found found, double load, long now) {
		List<ControlSupplyBlockEntity> supplies = new ArrayList<>();
		for (BlockPos at : found.supplies()) {
			if (level.getBlockEntity(at) instanceof ControlSupplyBlockEntity supply && supply.live()) {
				supplies.add(supply);
			}
		}
		for (ControlSupplyBlockEntity supply : supplies) {
			supply.report(worldPosition, load / supplies.size(), now);
		}
	}

	private @org.jspecify.annotations.Nullable RemoteIoDropBlockEntity rackOf(int slot) {
		int rack = (slot - MODULE_SLOTS) / MODULE_SLOTS + 1;
		for (RemoteIoDropBlockEntity drop : drops) {
			if (drop.rack() == rack) {
				return drop;
			}
		}
		return null;
	}

	/** What the cabinet draws from its 24 V at the last scan, its racks' not counted. */
	public double amps() {
		return amps;
	}

	/** Each signal's 24 V, in amps (proposed, docs/questions/plc-gaps.md): a compact and a standard CPU, a module's electronics, a network module and a rack's adapter, an input that is on, a relay's coil. */
	public static final Map<PlcModuleItem.Type, Double> CPU_AMPS = Map.of(PlcModuleItem.Type.CPU_SMALL, 0.4, PlcModuleItem.Type.CPU_LARGE, 0.7);
	public static final double MODULE_AMPS = 0.05;
	public static final double COMM_AMPS = 0.15;
	public static final double ADAPTER_AMPS = 0.2;
	public static final double DI_AMPS = 0.007;
	public static final double RELAY_AMPS = 0.01;

	// ---- the screen ------------------------------------------------------------------------------------------------------------------

	public void open(ServerPlayer player) {
		watchers.add(player.getUUID());
		net = ControlNet.walk(level, BigParts.cells(worldPosition, getBlockState()));
		send(player, true, List.of());
	}

	/** What a screen asks. {@code action} says which. */
	@Override
	public void request(ServerPlayer player, CompoundTag tag) {
		List<String> messages = new ArrayList<>();
		switch (tag.getStringOr("action", "")) {
			case "download" -> {
				int part = tag.getIntOr("part", 0);
				StringBuilder arriving = part == 0 ? new StringBuilder() : downloads.get(player.getUUID());
				if (arriving == null || arriving.length() + tag.getStringOr("piece", "").length() > MOST_PROJECT) {
					downloads.remove(player.getUUID());
					messages.add("Download refused: the project is over " + MOST_PROJECT / 1_000_000 + " million characters");
					break;
				}
				arriving.append(tag.getStringOr("piece", ""));
				downloads.put(player.getUUID(), arriving);
				if (part + 1 < tag.getIntOr("parts", 1)) {
					return;
				}
				downloads.remove(player.getUUID());
				List<Project.Source> next = Project.unpack(arriving.toString());
				if (next == null) {
					messages.add("Download refused: it arrived damaged");
					break;
				}
				boolean online = tag.getBooleanOr("online", false);
				List<CompileError> errors = download(next, online);
				messages.add(errors.isEmpty() ? online ? "Online change made." : "Downloaded. The CPU is in STOP." : "Download refused: " + errors.size() + " error(s)");
				errors.forEach(e -> messages.add(e.toString()));
			}
			case "toCard" -> {
				String refused = toCard();
				messages.add(refused != null ? refused : "Copied to the card: " + MemoryCardItem.describe(card));
			}
			case "fromCard" -> {
				String problem = cardProblem();
				if (problem != null) {
					messages.add(problem);
					break;
				}
				List<String> refused = fromCard();
				if (refused.isEmpty()) {
					messages.add("Loaded from the card. The CPU is in STOP.");
					sendProject(player);
				} else {
					messages.add("Download refused: " + refused.size() + " error(s)");
					messages.addAll(refused);
				}
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
			case "range" -> {
				ControlDevice device = ControlNet.deviceAt(level, new BlockPos(tag.getIntOr("dx", 0), tag.getIntOr("dy", 0), tag.getIntOr("dz", 0)));
				String[] ends = tag.getStringOr("range", "").trim().split("\\s+");
				try {
					String refused = device == null ? "That device is gone" : ends.length != 2 ? "A range is two numbers, such as 0 100" : device.setRange(Double.parseDouble(ends[0]), Double.parseDouble(ends[1]));
					messages.add(refused != null ? refused : "Range set: " + ends[0] + " to " + ends[1]);
				} catch (NumberFormatException e) {
					messages.add("A range is two numbers, such as 0 100");
				}
			}
			case "station" -> setStation(tag.getStringOr("station", "PLC").trim());
			case "exchangeAdd" -> {
				String row = tag.getStringOr("row", "").trim();
				if (row.indexOf('=') > 0 && exchange.size() < 64) {
					addExchange(row);
				} else {
					messages.add("A row is LOCAL = STATION/TAG, such as MAIN.FLOW = PLC-2/MAIN.FLOW");
				}
			}
			case "exchangeDel" -> {
				int index = tag.getIntOr("index", -1);
				if (index >= 0 && index < exchange.size()) {
					exchange.remove(index);
					setChanged();
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
			sendProject(player);
			ListTag names = new ListTag();
			if (cpu != null) {
				new TreeSet<>(cpu.program().tags().keySet()).stream().filter(n -> !n.contains("[") && !n.contains("__") && !isLibrary(n)).limit(1024).forEach(n -> names.add(StringTag.valueOf(n)));
			}
			tag.put("names", names);
		}
		int hash = 0;
		for (Project.Source unit : project) {
			hash = hash * 31 + (unit.name() + unit.language() + unit.text()).hashCode();
		}
		tag.putInt("projectHash", hash);
		tag.putString("status", statusText());
		tag.putString("cpu", cpuModule == null ? "" : cpuModule.cpu.name());
		tag.putString("rail", railText());
		tag.putString("compile", compileText);
		tag.putString("card", card.isEmpty() ? "" : MemoryCardItem.describe(card));
		tag.putString("station", station);
		ListTag rows = new ListTag();
		exchange.forEach(row -> rows.add(StringTag.valueOf((exchangeFailed.contains(row) ? "FAILED  " : "ok  ") + row)));
		tag.put("exchange", rows);
		ListTag others = new ListTag();
		peers.forEach(p -> others.add(StringTag.valueOf(p.station() + ": " + (p.cpu() == null ? "no project" : p.cpu().mode().name()))));
		drops.forEach(d -> others.add(StringTag.valueOf("Rack " + d.rack() + ": " + (d.live() ? "24 V OK, " : "NO 24 V, ") + d.devices().size() + " devices, slot " + (MODULE_SLOTS + (d.rack() - 1) * MODULE_SLOTS) + " up")));
		tag.put("network", others);
		tag.putBoolean("comm", hasComm());
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
				Object value = cpu.read(name);
				values.add(StringTag.valueOf(name + "=" + (value == null ? "?" : value)));
			}
			cpu.forces().forEach((k, v) -> values.add(StringTag.valueOf(k + "=" + v + " (FORCED)")));
		}
		tag.put("values", values);
		dev.wildcatter.platform.Platform.INSTANCE.sendToPlayer(player, new PlcNet.Payload(tag));
	}

	/** The project to a player's screen, in pieces, ahead of the packet that opens it or reloads it. */
	private void sendProject(ServerPlayer player) {
		String packed = Project.pack(project);
		int total = Math.max(1, (packed.length() + OPEN_PIECE - 1) / OPEN_PIECE);
		for (int i = 0; i < total; i++) {
			CompoundTag piece = new CompoundTag();
			piece.putInt("x", worldPosition.getX());
			piece.putInt("y", worldPosition.getY());
			piece.putInt("z", worldPosition.getZ());
			piece.putInt("part", i);
			piece.putInt("parts", total);
			piece.putString("projectPiece", packed.substring(i * OPEN_PIECE, Math.min(packed.length(), (i + 1) * OPEN_PIECE)));
			dev.wildcatter.platform.Platform.INSTANCE.sendToPlayer(player, new PlcNet.Payload(piece));
		}
	}

	/** The standard library's own variables would drown the list. */
	private static boolean isLibrary(String name) {
		return name.matches(".*\\.(START|RUNNING|LAST|LASTU|LASTD|M|INTEGRAL|LAST_PV|DT|GAIN|P|D|RAW|STARTED)$");
	}

	private ListTag devicesTag() {
		ListTag list = new ListTag();
		List<BlockPos> all = new ArrayList<>(net.devices());
		for (RemoteIoDropBlockEntity drop : drops) {
			all.addAll(drop.devices());
		}
		for (BlockPos pos : all) {
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
			List<ControlDevice.Point> pointsHere = device.points();
			for (int i = 0; i < pointsHere.size(); i++) {
				int point = i;
				CompoundTag row = new CompoundTag();
				row.putString("name", pointsHere.get(i).name());
				row.putBoolean("output", pointsHere.get(i).output());
				row.putString("address", landings.stream().filter(l -> l.pos().equals(pos) && l.point() == point).findFirst()
						.map(l -> address(pointsHere.get(point).output(), l.slot(), l.channel())).orElse(""));
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
				String first = switch (modules[i]) {
					case DI16 -> "%IX" + i * BYTES_PER_SLOT + ".0";
					case DO16 -> "%QX" + i * BYTES_PER_SLOT + ".0";
					case AO4 -> "%QW" + i * BYTES_PER_SLOT;
					case HSC2 -> "%ID" + i * BYTES_PER_SLOT;
					case COMM -> "network";
					default -> "%IW" + i * BYTES_PER_SLOT;
				};
				out.append(out.length() > 0 ? ", " : "").append(i).append(": ").append(modules[i]).append(" (").append(first).append(")");
			}
		}
		return out.length() == 0 ? "no modules" : out.toString();
	}

	private String statusText() {
		if (net.badJoint() != null) {
			return "BAD JOINT at " + net.badJoint().toShortString() + ": a cable makes one run; fan out at a junction box";
		}
		if (!powered) {
			for (BlockPos at : net.supplies()) {
				if (level != null && level.getBlockEntity(at) instanceof ControlSupplyBlockEntity supply && supply.overloaded()) {
					return "NO 24 V: its supply is OVERLOADED (over " + (int) ControlSupplyBlockEntity.RATED_AMPS + " A) and trying again";
				}
			}
			return "NO 24 V: a live control power supply must reach it by control cable";
		}
		if (cpuModule == null) {
			return "NO CPU: click a CPU module into the cabinet";
		}
		if (cpu == null) {
			return project.isEmpty() ? "NO PROJECT" : "The project does not compile: " + compileText;
		}
		return (cpu.faulted() ? "CPU FAULT" : cpu.mode() == Cpu.Mode.RUN ? "RUN" : "STOP") + (cpu.forcing() ? ", FORCING" : "") + (coreProblem.isEmpty() ? "" : ", " + coreProblem)
				+ (loopFight.isEmpty() ? "" : ", " + loopFight) + (foreignRacks.isEmpty() ? "" : ", " + foreignRacks.entrySet().stream().map(e -> "RACK " + e.getKey() + " OWNED BY " + e.getValue()).collect(Collectors.joining(", ")))
				+ String.format(", 24 V %.2f A", amps);
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
		output.putString("station", station);
		output.store("exchange", Codec.STRING.listOf(), exchange);
		if (!card.isEmpty()) {
			output.store("card", ItemStack.CODEC, card);
		}
		output.putString("cpuModule", cpuModule == null ? "" : cpuModule.name());
		output.store("modules", Codec.STRING.listOf(), java.util.Arrays.stream(modules).map(m -> m == null ? "" : m.name()).toList());
		output.store("units", Codec.STRING.listOf(), project.stream().map(u -> u.name() + "|" + u.language().name()).toList());
		for (int i = 0; i < project.size(); i++) {
			// in pieces: a saved string holds 65,535 bytes, and one line of a unit can be longer than that
			String text = project.get(i).text();
			List<String> pieces = new ArrayList<>();
			for (int at = 0; at < text.length(); at += 20_000) {
				pieces.add(text.substring(at, Math.min(text.length(), at + 20_000)));
			}
			output.store("unitPieces" + i, Codec.STRING.listOf(), pieces);
		}
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
		station = input.getStringOr("station", "PLC");
		exchange.clear();
		exchange.addAll(input.read("exchange", Codec.STRING.listOf()).orElse(List.of()));
		card = input.read("card", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		String name = input.getStringOr("cpuModule", "");
		cpuModule = name.isEmpty() ? null : PlcModuleItem.Type.valueOf(name);
		List<String> rail = input.read("modules", Codec.STRING.listOf()).orElse(List.of());
		for (int i = 0; i < modules.length && i < rail.size(); i++) {
			modules[i] = rail.get(i).isEmpty() ? null : PlcModuleItem.Type.valueOf(rail.get(i));
		}
		project = new ArrayList<>();
		List<String> units = input.read("units", Codec.STRING.listOf()).orElse(List.of());
		for (int i = 0; i < units.size(); i++) {
			String[] head = units.get(i).split("\\|");
			int unit = i;
			String text = input.read("unitPieces" + i, Codec.STRING.listOf()).map(pieces -> String.join("", pieces))
					.orElseGet(() -> String.join("\n", input.read("unit" + unit, Codec.STRING.listOf()).orElse(List.of())));   // a 13c to 13h save, by lines
			project.add(new Project.Source(head[0], Project.Language.valueOf(head[1]), text));
		}
		input.read("source", Codec.STRING.listOf()).ifPresent(old -> project.add(new Project.Source("MAIN", Project.Language.ST, String.join("\n", old))));   // a 13b save
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
