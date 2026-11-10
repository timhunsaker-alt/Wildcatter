package dev.wildcatter.control;

import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What a cabinet's control cables reach. Signals are not put through the circuit solver, whose groups they would swell: this is continuity
 * only, walked over loaded blocks (docs/multiblocks/plc.md).
 * <p>
 * A walk also finds each cable's run (its cells from one box, cabinet or device to the next) and the way to each device, run by run, so a
 * cabinet can count the cores its signals take in every cable they pass through.
 */
public final class ControlNet {
	/** The most cable and box cells one walk follows: about 8 km of cable. */
	private static final int MOST_CELLS = 8192;
	/**
	 * The cores of one control cable: a 50-pair multi-pair instrument cable (docs/questions/plc-gaps.md). Two are its +24 V and 0 V commons, so
	 * {@link #SIGNAL_CORES} carry signals.
	 */
	public static final int CORES = 100;
	public static final int SIGNAL_CORES = CORES - 2;

	private ControlNet() {
	}

	/** One link on the way to a device: one or more cable runs side by side between two places, or a box or device touching another (no cable). */
	public record Link(BlockPos from, BlockPos to, int cores, @Nullable BlockPos cable) {
		public boolean cabled() {
			return cable != null;
		}
	}

	/**
	 * What a walk found. {@code badJoint} is a cable cell that touches more than two things, or null. {@code supplies} are the 24 V supplies
	 * reached; {@code routes} each device's links from the walk's start, in order.
	 */
	public record Found(List<BlockPos> devices, boolean supplyLive, @Nullable BlockPos badJoint, List<BlockPos> supplies, Map<BlockPos, List<Link>> routes) {
		public Found(List<BlockPos> devices, boolean supplyLive, @Nullable BlockPos badJoint) {
			this(devices, supplyLive, badJoint, List.of(), Map.of());
		}

		/** The links to {@code device}; empty if it touches the start or was not reached. */
		public List<Link> route(BlockPos device) {
			return routes.getOrDefault(device, List.of());
		}
	}

	/** The device at {@code pos}, looking through a big part's cell to its main cell. */
	public static @Nullable ControlDevice deviceAt(BlockGetter level, BlockPos pos) {
		return BigParts.entity(level, pos) instanceof ControlDevice device && !device.points().isEmpty() ? device : null;   // one with no signals is no device: an MCC bucket, an unpowered part
	}

	public static BlockPos mainOf(BlockGetter level, BlockPos pos) {
		var entity = BigParts.entity(level, pos);   // any kind of true-size part, as not all are a BigPartBlock
		return entity != null ? entity.getBlockPos() : pos;
	}

	/** True if a control cable next to {@code pos} would join it. */
	public static boolean joins(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof ControlCableBlock || state.getBlock() instanceof JunctionBoxBlock || state.getBlock() instanceof ControlSupplyBlock
				|| state.getBlock() instanceof PlcCabinetBlock || deviceAt(level, pos) != null;
	}

	private static int touching(Level level, BlockPos cable) {
		int n = 0;
		for (Direction side : Direction.values()) {
			if (joins(level, cable.relative(side))) {
				n++;
			}
		}
		return n;
	}

	/** Walks out from a cabinet's (or a drop's) cells. */
	public static Found walk(Level level, List<BlockPos> cabinet) {
		Set<BlockPos> seen = new HashSet<>(cabinet);
		Deque<BlockPos> queue = new ArrayDeque<>(cabinet);
		Set<BlockPos> devices = new java.util.LinkedHashSet<>();
		Set<BlockPos> cables = new HashSet<>();
		Set<BlockPos> boxes = new HashSet<>();
		Map<BlockPos, BlockPos> deviceCells = new HashMap<>();
		List<BlockPos> supplies = new ArrayList<>();
		boolean live = false;
		BlockPos bad = null;
		int cells = 0;   // cable and boxes followed: the blocks looked at beside them do not count, or a long trunk stops a fifth of the way
		while (!queue.isEmpty() && cells < MOST_CELLS) {
			BlockPos at = queue.poll();
			for (Direction side : Direction.values()) {
				BlockPos next = at.relative(side);
				if (!level.isLoaded(next) || !seen.add(next)) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (state.getBlock() instanceof ControlCableBlock) {
					if (touching(level, next) > 2) {
						bad = bad == null ? next : bad;
					} else {
						queue.add(next);
						cables.add(next);
						cells++;
					}
				} else if (state.getBlock() instanceof JunctionBoxBlock) {
					queue.add(next);
					boxes.add(next);
					cells++;
				} else if (level.getBlockEntity(next) instanceof ControlSupplyBlockEntity supply) {
					live |= supply.live();
					supplies.add(next);
				} else if (!(state.getBlock() instanceof PlcCabinetBlock) && deviceAt(level, next) != null) {
					BlockPos main = mainOf(level, next);
					devices.add(main);
					deviceCells.put(next, main);
				}
			}
		}
		return new Found(new ArrayList<>(devices), live, bad, supplies, routes(cabinet, cables, boxes, deviceCells));
	}

	/**
	 * The way to each device, as links between places: the start (the cabinet's cells, and any box touching them, as one place), each group of
	 * touching boxes, and each device. Runs of cable side by side between the same two places add their cores.
	 */
	private static Map<BlockPos, List<Link>> routes(List<BlockPos> start, Set<BlockPos> cables, Set<BlockPos> boxes, Map<BlockPos, BlockPos> deviceCells) {
		BlockPos root = start.getFirst();
		Map<BlockPos, BlockPos> place = new HashMap<>();   // a cell to the place it is part of
		for (BlockPos cell : start) {
			place.put(cell, root);
		}
		// boxes that touch each other, or the start, are one place: terminals on one rail
		for (BlockPos box : boxes) {
			if (place.containsKey(box)) {
				continue;
			}
			BlockPos name = box;
			Deque<BlockPos> group = new ArrayDeque<>(List.of(box));
			List<BlockPos> members = new ArrayList<>();
			Set<BlockPos> grouped = new HashSet<>(List.of(box));
			while (!group.isEmpty()) {
				BlockPos at = group.poll();
				members.add(at);
				for (Direction side : Direction.values()) {
					BlockPos next = at.relative(side);
					if (place.get(next) == root) {
						name = root;
					} else if (boxes.contains(next) && grouped.add(next)) {
						group.add(next);
					}
				}
			}
			for (BlockPos member : members) {
				place.put(member, name);
			}
		}
		deviceCells.forEach(place::put);
		// the links: each cable run between two places, and each device touching a place
		Map<BlockPos, Map<BlockPos, int[]>> cores = new HashMap<>();
		Map<String, BlockPos> sample = new HashMap<>();
		Set<BlockPos> walked = new HashSet<>();
		for (BlockPos cable : cables) {
			if (!walked.add(cable)) {
				continue;
			}
			Deque<BlockPos> run = new ArrayDeque<>(List.of(cable));
			Set<BlockPos> ends = new java.util.LinkedHashSet<>();
			while (!run.isEmpty()) {
				BlockPos at = run.poll();
				for (Direction side : Direction.values()) {
					BlockPos next = at.relative(side);
					if (cables.contains(next)) {
						if (walked.add(next)) {
							run.add(next);
						}
					} else if (place.containsKey(next)) {
						ends.add(place.get(next));
					}
				}
			}
			if (ends.size() == 2) {
				BlockPos a = ends.iterator().next();
				BlockPos b = ends.stream().skip(1).findFirst().orElseThrow();
				cores.computeIfAbsent(a, k -> new HashMap<>()).computeIfAbsent(b, k -> new int[1])[0] += SIGNAL_CORES;
				cores.computeIfAbsent(b, k -> new HashMap<>()).computeIfAbsent(a, k -> new int[1])[0] += SIGNAL_CORES;
				sample.putIfAbsent(key(a, b), cable);
			}
		}
		for (Map.Entry<BlockPos, BlockPos> device : deviceCells.entrySet()) {
			for (Direction side : Direction.values()) {
				BlockPos owner = place.get(device.getKey().relative(side));
				if (owner != null && !owner.equals(device.getValue()) && !deviceCells.containsKey(device.getKey().relative(side))) {
					cores.computeIfAbsent(owner, k -> new HashMap<>()).put(device.getValue(), new int[] {Integer.MAX_VALUE});
					cores.computeIfAbsent(device.getValue(), k -> new HashMap<>()).put(owner, new int[] {Integer.MAX_VALUE});
				}
			}
		}
		// out from the start, the shortest way, never through a device
		Set<BlockPos> isDevice = new HashSet<>(deviceCells.values());
		Map<BlockPos, List<Link>> out = new LinkedHashMap<>();
		Map<BlockPos, List<Link>> way = new HashMap<>();
		way.put(root, List.of());
		Deque<BlockPos> queue = new ArrayDeque<>(List.of(root));
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			for (Map.Entry<BlockPos, int[]> next : cores.getOrDefault(at, Map.of()).entrySet()) {
				if (way.containsKey(next.getKey())) {
					continue;
				}
				List<Link> path = new ArrayList<>(way.get(at));
				int n = next.getValue()[0];
				path.add(new Link(at, next.getKey(), n, n == Integer.MAX_VALUE ? null : sample.get(key(at, next.getKey()))));
				way.put(next.getKey(), path);
				if (isDevice.contains(next.getKey())) {
					out.put(next.getKey(), path);
				} else {
					queue.add(next.getKey());
				}
			}
		}
		return out;
	}

	private static String key(BlockPos a, BlockPos b) {
		return a.asLong() < b.asLong() ? a.asLong() + ":" + b.asLong() : b.asLong() + ":" + a.asLong();
	}

	/** The cores a signal takes: a discrete one a core, returning on the cable's common; a 4-20 mA loop or a pulse input a twisted pair. */
	public static int coresOf(ControlDevice.Point point) {
		return point.signal() == ControlDevice.Signal.BOOL ? 1 : 2;
	}
}
