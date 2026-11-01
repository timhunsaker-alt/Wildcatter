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

	/** One joined run of steel pipe, the machines on it, and what it carries. */
	public record Line(Set<BlockPos> pipes, List<Port> ports, Run run) {
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
		return level.getBlockState(pos).getBlock() instanceof PipeBlock pipe && pipe.kind() == PipeBlock.Kind.GAS;
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

	/** The run through the steel pipe at {@code start}. */
	public static Line line(Level level, BlockPos start) {
		Set<BlockPos> pipes = new LinkedHashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		pipes.add(start);
		queue.add(start);
		while (!queue.isEmpty() && pipes.size() < Tuning.PIPE_NETWORK_LIMIT) {
			BlockPos at = queue.poll();
			for (Direction side : Direction.values()) {
				BlockPos next = at.relative(side);
				if (isSteelPipe(level, next) && pipes.add(next)) {
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
		return new Line(pipes, ports, Run.of(offered));
	}

	/** Each distinct run of steel pipe on these faces. */
	public static List<Line> lines(Level level, Collection<Face> faces) {
		List<Line> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		for (Face face : faces) {
			BlockPos pipe = face.pipe();
			if (!seen.contains(pipe) && isSteelPipe(level, pipe)) {
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
		if (!line.run().carries(product)) {
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
		double[] share = Flow.share(max, available);
		double got = 0;
		for (int i = 0; i < share.length; i++) {
			if (share[i] > 0) {
				Port p = sources.get(i);
				got += p.node().take(p.cell(), p.side(), product, share[i], pull);
			}
		}
		return got;
	}

	/** How much of {@code product} the runs on {@code faces} could give {@code pull} now. */
	public static double available(Level level, Collection<Face> faces, Product product, FluidNode.Pull pull, @Nullable Object self) {
		double sum = 0;
		for (Line line : lines(level, faces)) {
			for (Port p : sources(line, product, pull, self)) {
				sum += p.node().available(p.cell(), p.side(), product, pull);
			}
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
