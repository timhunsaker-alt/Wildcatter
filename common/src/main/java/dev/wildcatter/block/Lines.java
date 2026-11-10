package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Flow;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Run;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Runs of steel pipe and what they carry (docs/multiblocks/lease.md): each joined run carries the one product its sources put on it,
 * or is BLOCKED where two meet. Machines pull from the runs on their ports: process outlets first, then tanks. The product math is
 * plain Java in {@code dev.wildcatter.fluid}; this finds the pipes and the machines on them.
 */
public final class Lines {
	private Lines() {
	}

	/** One face of a machine's cell; {@code side} points from the cell to where a pipe would be. */
	public record Face(BlockPos cell, Direction side) {
		public BlockPos pipe() {
			return cell.relative(side);
		}
	}

	/** A source on a run: a node and the face of its cell the run touches. */
	public record Port(FluidNode node, BlockPos cell, Direction side) {
		@Nullable Product offers() {
			return node.offers(cell, side);
		}
	}

	/**
	 * One joined run of steel pipe (or of 600 mm slurry pipe), the machines on it, and what it carries. A slurry run is rated
	 * ({@link #capacityPerTick}): {@code key} and {@code tick} let the pulls of one game tick share its rating.
	 */
	public record Line(Set<BlockPos> pipes, List<Port> ports, Run run, double opening, boolean slurry, long key, long tick) {
		public Line(Set<BlockPos> pipes, List<Port> ports, Run run) {
			this(pipes, ports, run, 1.0);
		}

		public Line(Set<BlockPos> pipes, List<Port> ports, Run run, double opening) {
			this(pipes, ports, run, opening, false, 0, 0);
		}

		/** True if a block of this kind touches the run (a disposal wellhead, which keeps no state). */
		public boolean touches(Level level, Class<? extends Block> kind) {
			for (BlockPos pipe : pipes) {
				for (Direction side : Direction.values()) {
					if (kind.isInstance(level.getBlockState(pipe.relative(side)).getBlock())) {
						return true;
					}
				}
			}
			return false;
		}
	}

	static boolean isSteelPipe(Level level, BlockPos pos) {
		return isRun(level, pos, PipeBlock.Kind.GAS);
	}

	/** A conducting pipe of this kind at {@code pos}. */
	static boolean isRun(Level level, BlockPos pos, PipeBlock.Kind kind) {
		return level.getBlockState(pos).getBlock() instanceof PipeBlock pipe && pipe.kind() == kind && PipeBlock.conducts(level, pos);
	}

	/** The kind of process run at {@code pos}: steel pipe (GAS) or slurry pipe, or null for neither. */
	static PipeBlock.@Nullable Kind runKind(Level level, BlockPos pos) {
		return isRun(level, pos, PipeBlock.Kind.GAS) ? PipeBlock.Kind.GAS : isRun(level, pos, PipeBlock.Kind.SLURRY) ? PipeBlock.Kind.SLURRY : null;
	}

	// ---- the slurry pipe's rating (Tim, 2026-11-10, Tailings p7 #27) ------------------------------------------------------------------
	/** Per rated run: the game tick and what has gone through it in that tick. */
	private static final java.util.Map<Long, double[]> USED = new java.util.HashMap<>();

	/**
	 * What a run passes in a tick of {@code product}, in its own unit: steel pipe is unrated; the 600 mm slurry pipe passes
	 * {@link Tuning#SLURRY_PIPE_M3_S} m3/s, counted for slurries at {@link Tuning#SLURRY_PIPE_KG_M3}, and no gas.
	 */
	public static double capacityPerTick(Line line, Product product) {
		if (!line.slurry()) {
			return Double.POSITIVE_INFINITY;
		}
		if (product.gas()) {
			return 0;
		}
		return Tuning.SLURRY_PIPE_M3_S * ("kg".equals(product.unit()) ? Tuning.SLURRY_PIPE_KG_M3 : 1000.0) / 20.0;
	}

	/** What is left of a run's rating in this tick. */
	public static double room(Line line, Product product) {
		double cap = capacityPerTick(line, product);
		if (Double.isInfinite(cap)) {
			return cap;
		}
		double[] used = USED.get(line.key());
		return Math.max(0, cap - (used != null && used[0] == line.tick() ? used[1] : 0));
	}

	/** Counts {@code amount} through a rated run in this tick. */
	static void use(Line line, double amount) {
		if (!line.slurry() || amount <= 0) {
			return;
		}
		if (USED.size() > 4096) {
			USED.values().removeIf(u -> u[0] < line.tick());
		}
		double[] used = USED.computeIfAbsent(line.key(), k -> new double[] {line.tick(), 0});
		if (used[0] != line.tick()) {
			used[0] = line.tick();
			used[1] = 0;
		}
		used[1] += amount;
	}

	/** Every face of these cells that looks out of them (where a pipe could join). */
	public static List<Face> faces(Collection<BlockPos> cells) {
		List<Face> out = new ArrayList<>();
		for (BlockPos cell : cells) {
			for (Direction side : Direction.values()) {
				if (!cells.contains(cell.relative(side))) {
					out.add(new Face(cell, side));
				}
			}
		}
		return out;
	}

	/** The machine node standing at {@code pos}: a big part's main cell, a pumpjack through its wellhead. */
	static @Nullable FluidNode node(Level level, BlockPos pos) {
		Block block = level.getBlockState(pos).getBlock();
		BlockEntity entity;
		if (block instanceof OilWellheadBlock) {   // a pumpjack's stream comes out of its wellhead
			entity = OilWellheadBlock.pumpjack(level, pos);
		} else {
			entity = dev.wildcatter.multiblock.BigParts.entity(level, pos);
		}
		return entity instanceof FluidNode node ? node : null;
	}

	/** The run through the steel pipe (or the slurry pipe) at {@code start}. */
	public static Line line(Level level, BlockPos start) {
		PipeBlock.Kind kind = runKind(level, start);
		if (kind == null) {
			kind = PipeBlock.Kind.GAS;
		}
		Set<BlockPos> pipes = new LinkedHashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		pipes.add(start);
		queue.add(start);
		long key = start.asLong();
		while (!queue.isEmpty() && pipes.size() < Tuning.PIPE_NETWORK_LIMIT) {
			BlockPos at = queue.poll();
			key = Math.min(key, at.asLong());
			for (Direction side : Direction.values()) {
				BlockPos next = at.relative(side);
				if (isRun(level, next, kind) && pipes.add(next)) {
					queue.add(next);
				}
			}
		}
		List<Port> ports = new ArrayList<>();
		List<Product> offered = new ArrayList<>();
		for (BlockPos pipe : pipes) {
			for (Direction side : Direction.values()) {
				BlockPos cell = pipe.relative(side);
				if (pipes.contains(cell)) {
					continue;
				}
				FluidNode node = node(level, cell);
				if (node != null) {
					Port port = new Port(node, cell, side.getOpposite());
					ports.add(port);
					offered.add(port.offers());
				}
			}
		}
		boolean slurry = kind == PipeBlock.Kind.SLURRY;
		return new Line(pipes, ports, Run.of(offered), Plumbing.throttle(level, pipes), slurry, slurry ? key ^ level.dimension().hashCode() : 0, level.getGameTime());
	}

	/** Each distinct run of steel pipe on these faces. */
	public static List<Line> lines(Level level, Collection<Face> faces) {
		List<Line> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		for (Face face : faces) {
			BlockPos pipe = face.pipe();
			if (!seen.contains(pipe) && runKind(level, pipe) != null) {
				Line line = line(level, pipe);
				seen.addAll(line.pipes());
				out.add(line);
			}
		}
		return out;
	}

	/** The sources of {@code product} on a run that {@code pull} may draw from, process outlets before tanks, never {@code self}. */
	static List<Port> sources(Line line, Product product, FluidNode.Pull pull, @Nullable Object self) {
		List<Port> outlets = new ArrayList<>(), tanks = new ArrayList<>();
		if (!line.run().carries(product) || line.slurry() && product.gas()) {
			return outlets;
		}
		for (Port port : line.ports()) {
			if (port.node() != self && port.offers() == product) {
				(port.node().tank() ? tanks : outlets).add(port);
			}
		}
		if (pull != FluidNode.Pull.TANK) {
			outlets.addAll(tanks);
		}
		return outlets;
	}

	/** Takes up to {@code max} of {@code product} from the runs on {@code faces}; returns how much came. */
	public static double pull(Level level, Collection<Face> faces, Product product, double max, FluidNode.Pull pull, @Nullable Object self) {
		double got = 0;
		for (Line line : lines(level, faces)) {
			if (got >= max) {
				break;
			}
			got += pull(line, product, max - got, pull, self);
		}
		return got;
	}

	/** Takes up to {@code max} of {@code product} from one run. */
	public static double pull(Line line, Product product, double max, FluidNode.Pull pull, @Nullable Object self) {
		List<Port> sources = sources(line, product, pull, self);
		double[] available = new double[sources.size()];
		for (int i = 0; i < available.length; i++) {
			Port p = sources.get(i);
			available[i] = p.node().available(p.cell(), p.side(), product, pull);
		}
		double[] share = Flow.share(Math.min(max * line.opening(), room(line, product)), available);
		double got = 0;
		for (int i = 0; i < share.length; i++) {
			if (share[i] > 0) {
				Port p = sources.get(i);
				got += p.node().take(p.cell(), p.side(), product, share[i], pull);
			}
		}
		use(line, got);
		return got;
	}

	/** How much of {@code product} the runs on {@code faces} could give {@code pull} now. */
	public static double available(Level level, Collection<Face> faces, Product product, FluidNode.Pull pull, @Nullable Object self) {
		double sum = 0;
		for (Line line : lines(level, faces)) {
			double here = 0;
			for (Port p : sources(line, product, pull, self)) {
				here += p.node().available(p.cell(), p.side(), product, pull);
			}
			sum += Math.min(here, room(line, product));
		}
		return sum;
	}

	/** What the runs on {@code faces} carry, for a machine's readout: the first that is not {@code wanted}, or null if all are. */
	public static @Nullable Run wrong(Level level, Collection<Face> faces, Product wanted) {
		for (Line line : lines(level, faces)) {
			if (!line.run().carries(wanted) && (line.run().product() != null || line.run().blocked())) {
				return line.run();
			}
		}
		return null;
	}
}
