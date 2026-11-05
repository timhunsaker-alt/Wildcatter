package dev.wildcatter.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Run;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.fluid.SlurryHold;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** A pull of slurry off one run, with stand-in source nodes. */
class SlurryPullTest {
	private static final String GALENA = "wildcatter:galena";

	/** A source that puts one slurry on its pipe from a {@link SlurryHold}. */
	private static final class Source implements FluidNode, Assayed {
		final SlurryHold hold = new SlurryHold();
		final Product product;

		Source(Product product, double kg, double galena) {
			this.product = product;
			hold.offer(product, kg, SlurryAssay.of(Map.of(GALENA, galena)));
		}

		@Override
		public Product offers(BlockPos cell, Direction side) {
			return product;
		}

		@Override
		public double available(BlockPos cell, Direction side, Product p, Pull pull) {
			return hold.held(p);
		}

		@Override
		public double take(BlockPos cell, Direction side, Product p, double max, Pull pull) {
			return hold.give(p, max);
		}

		@Override
		public SlurryAssay assayOf(Product p) {
			return hold.assayOf(p);
		}
	}

	private static Lines.Line run(FluidNode... nodes) {
		List<Lines.Port> ports = new java.util.ArrayList<>();
		for (int i = 0; i < nodes.length; i++) {
			ports.add(new Lines.Port(nodes[i], new BlockPos(i, 0, 0), Direction.UP));
		}
		return new Lines.Line(Set.of(BlockPos.ZERO), ports, Run.of(List.of(Product.FINES_SLURRY)));
	}

	@Test
	void aPullCarriesTheAssayOfItsSource() {
		Source a = new Source(Product.FINES_SLURRY, 1000, 0.04);
		Slurry.Taken got = Slurry.take(run(a), Product.FINES_SLURRY, 250, null);
		assertEquals(250, got.kg(), 1e-9);
		assertEquals(0.04, got.assay().get(GALENA), 1e-12);
		assertEquals(750, a.hold.held(Product.FINES_SLURRY), 1e-9);
	}

	@Test
	void twoSourcesBlendByTheKgEachGave() {
		Source rich = new Source(Product.FINES_SLURRY, 300, 0.04);
		Source lean = new Source(Product.FINES_SLURRY, 100, 0.00);
		assertEquals("galena 4.00 %", Slurry.describe(run(lean, rich)), "the meter reads the first source that holds minerals");
		Slurry.Taken got = Slurry.take(run(rich, lean), Product.FINES_SLURRY, 400, null);
		assertEquals(400, got.kg(), 1e-9);
		assertEquals(0.03, got.assay().get(GALENA), 1e-12);
	}

	@Test
	void aPullerNeverDrawsFromItself() {
		Source self = new Source(Product.FINES_SLURRY, 100, 0.04);
		assertEquals(0, Slurry.take(run(self), Product.FINES_SLURRY, 50, self).kg());
		assertNull(Slurry.describe(new Lines.Line(Set.of(), List.of(), Run.EMPTY)));
	}
}
