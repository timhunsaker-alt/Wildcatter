package dev.wildcatter.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.safety.Groundable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A differential relay's zone on one side of a machine (87T, 87G): the leads from the machine's terminals (cable, busway, bus duct,
 * a station bus and its disconnects) up to the breakers whose CTs bound it. Found by walking cell to cell from the terminals; it stops
 * at any breaker (the boundary) and at anything that is not a lead (a load, another transformer, an insulator).
 */
public final class ProtectionZone {
	/** The lead cells in the zone: a grounding set on one of them is a fault in the zone. */
	public final List<BlockPos> leads = new ArrayList<>();
	/** The main cells of the breakers that bound it: yard breakers, 15 kV feeder breakers, switchgear sections, generator breakers. */
	public final List<BlockPos> breakers = new ArrayList<>();

	private ProtectionZone() {
	}

	private static final int MOST = 512;

	/** Walks from the cells across {@code starts} (each a terminal cell and the face it leaves by). */
	public static ProtectionZone walk(Level level, Collection<Lines.Face> starts) {
		ProtectionZone zone = new ProtectionZone();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		for (Lines.Face f : starts) {
			seen.add(f.cell());
		}
		for (Lines.Face f : starts) {
			BlockPos n = f.cell().relative(f.side());
			if (seen.add(n)) {
				queue.add(n);
			}
		}
		Set<BlockPos> mains = new HashSet<>();
		while (!queue.isEmpty() && zone.leads.size() < MOST) {
			BlockPos here = queue.poll();
			BlockEntity e = BigParts.entity(level, here);
			if (e == null) {
				continue;
			}
			if (e instanceof BreakerBlockEntity || e instanceof SwitchgearSectionBlockEntity || e instanceof GenBreakerBlockEntity) {
				if (mains.add(e.getBlockPos())) {
					zone.breakers.add(e.getBlockPos());
				}
				continue;
			}
			List<BlockPos> cells;
			if (e instanceof MultiCableBlockEntity) {
				zone.leads.add(here);
				cells = List.of(here);
			} else if (e instanceof StationBusBlockEntity || e instanceof DisconnectSwitchBlockEntity) {
				if (!mains.add(e.getBlockPos())) {
					continue;
				}
				cells = BigParts.cells(e.getBlockPos(), e.getBlockState());
				seen.addAll(cells);
			} else {
				continue;
			}
			for (BlockPos cell : cells) {
				for (Direction d : Direction.values()) {
					BlockPos n = cell.relative(d);
					if (seen.add(n)) {
						queue.add(n);
					}
				}
			}
		}
		return zone;
	}

	/** The current into grounds on the zone's leads, the largest phase's of each set, summed: the zone's differential current. */
	public double faultAmps(Level level, Circuit circuit) {
		double sum = 0;
		for (BlockPos at : leads) {
			if (level.getBlockEntity(at) instanceof MultiCableBlockEntity cable && cable.grounded()) {
				sum += cable.groundAmps(circuit);
			}
		}
		return sum;
	}

	/** True if any breaker bounding it is closed (for a lockout to hold them open). */
	public static boolean anyClosed(Level level, Collection<BlockPos> breakers) {
		for (BlockPos at : breakers) {
			if (closed(level, at)) {
				return true;
			}
		}
		return false;
	}

	public static boolean closed(Level level, BlockPos at) {
		BlockEntity e = level.getBlockEntity(at);
		if (e instanceof BreakerBlockEntity b) {
			return b.closed();
		}
		if (e instanceof SwitchgearSectionBlockEntity s) {
			return s.closed();
		}
		if (e instanceof GenBreakerBlockEntity g) {
			GeneratorPartBlockEntity gen = g.generator();
			return gen != null && gen.breakerClosed();
		}
		return false;
	}

	/** Opens the breaker at {@code at} by a relay; a generator breaker trips its generator (a unit trip). True if it was closed. */
	public static boolean open(Level level, BlockPos at, String why) {
		if (!closed(level, at)) {
			return false;
		}
		BlockEntity e = level.getBlockEntity(at);
		if (e instanceof BreakerBlockEntity b) {
			b.tripByRelay(why);
		} else if (e instanceof SwitchgearSectionBlockEntity s) {
			s.tripByRelay(why);
		} else if (e instanceof GenBreakerBlockEntity g && g.generator() != null) {
			g.generator().trip("by " + why);
		}
		CircuitManager.markDirty(level);
		return true;
	}

	/** Closes again, after the lockout is reset, a breaker the relay opened; a generator is reset to start and synchronize. */
	public static void close(Level level, BlockPos at) {
		BlockEntity e = level.getBlockEntity(at);
		if (e instanceof BreakerBlockEntity b) {
			b.closeByRelay();
		} else if (e instanceof SwitchgearSectionBlockEntity s) {
			s.closeByRelay();
		} else if (e instanceof GenBreakerBlockEntity g && g.generator() != null) {
			g.generator().reset();
		}
		CircuitManager.markDirty(level);
	}

	/** True if anything in {@code leads} has protective grounds on it. */
	public static boolean grounded(Level level, Collection<BlockPos> leads) {
		for (BlockPos at : leads) {
			if (level.getBlockEntity(at) instanceof Groundable g && g.grounded()) {
				return true;
			}
		}
		return false;
	}
}
