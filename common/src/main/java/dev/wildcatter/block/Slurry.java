package dev.wildcatter.block;

import dev.wildcatter.fluid.Flow;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Concentrator slurry on steel pipe, with its assay. The pipe holds nothing: each source node holds its slurry and its assay
 * ({@link Assayed}, usually through a {@link dev.wildcatter.fluid.SlurryHold}), and a pull blends the assays of the sources it drew
 * from by the kg each gave.
 */
public final class Slurry {
	private Slurry() {
	}

	/** What a pull brought: the kg and what they carry. */
	public record Taken(double kg, SlurryAssay assay) {
		public static final Taken NONE = new Taken(0, SlurryAssay.EMPTY);
	}

	/** Takes up to {@code maxKg} of {@code product} from the runs on {@code faces}, never from {@code self}. */
	public static Taken take(Level level, Collection<Lines.Face> faces, Product product, double maxKg, @Nullable Object self) {
		Taken got = Taken.NONE;
		for (Lines.Line line : Lines.lines(level, faces)) {
			if (got.kg() >= maxKg) {
				break;
			}
			got = add(got, take(line, product, maxKg - got.kg(), self));
		}
		return got;
	}

	/** Takes up to {@code maxKg} of {@code product} from one run, shared among its sources as {@link Lines#pull} shares it. */
	public static Taken take(Lines.Line line, Product product, double maxKg, @Nullable Object self) {
		List<Lines.Port> sources = Lines.sources(line, product, FluidNode.Pull.MACHINE, self);
		double[] available = new double[sources.size()];
		for (int i = 0; i < available.length; i++) {
			Lines.Port p = sources.get(i);
			available[i] = p.node().available(p.cell(), p.side(), product, FluidNode.Pull.MACHINE);
		}
		double[] share = Flow.share(Math.min(maxKg, Lines.room(line, product)), available);
		Taken got = Taken.NONE;
		for (int i = 0; i < share.length; i++) {
			if (share[i] > 0) {
				Lines.Port p = sources.get(i);
				SlurryAssay assay = p.node() instanceof Assayed a ? a.assayOf(product) : SlurryAssay.EMPTY;
				double kg = p.node().take(p.cell(), p.side(), product, share[i], FluidNode.Pull.MACHINE);
				got = add(got, new Taken(kg, assay));
			}
		}
		Lines.use(line, got.kg());
		return got;
	}

	/** How many kg of {@code product} the runs on {@code faces} could give now. */
	public static double available(Level level, Collection<Lines.Face> faces, Product product, @Nullable Object self) {
		return Lines.available(level, faces, product, FluidNode.Pull.MACHINE, self);
	}

	/** For the Power Meter: the assay of the first source holding a slurry on this run, or null. */
	public static @Nullable String describe(Lines.Line line) {
		Product product = line.run().product();
		if (product == null) {
			return null;
		}
		for (Lines.Port p : line.ports()) {
			if (p.node() instanceof Assayed a && p.offers() == product && !a.assayOf(product).isEmpty()) {
				return a.assayOf(product).describe();
			}
		}
		return null;
	}

	private static Taken add(Taken a, Taken b) {
		return b.kg() <= 0 ? a : new Taken(a.kg() + b.kg(), SlurryAssay.blend(a.assay(), a.kg(), b.assay(), b.kg()));
	}
}
