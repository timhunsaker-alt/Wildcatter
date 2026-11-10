package dev.wildcatter.neoforge.gametest;

import dev.wildcatter.block.Assayed;
import dev.wildcatter.block.FluidNode;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A stand-in gas source for the scenes (docs/biogas.md, "How another mod supplies it"): a block of this test mod, not of Wildcatter,
 * whose block entity implements {@link FluidNode} and offers one product on every face at a set rate. It is exactly what another mod
 * (Farmings' digester) does to put biogas on Wildcatter's steel pipe; the sales scene sets it to pipeline gas or an off-spec gas.
 */
final class TestGasSource {
	private TestGasSource() {
	}

	static BlockEntityType<SourceEntity> TYPE;

	static final class SourceBlock extends Block implements EntityBlock {
		SourceBlock(Properties properties) {
			super(properties);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new SourceEntity(pos, state);
		}
	}

	static final class SourceEntity extends BlockEntity implements FluidNode, dev.wildcatter.block.Mixed, Assayed {
		Product product = Product.BIOGAS;
		/** What a liquor it offers carries (Phase 16's scenes: strip liquor's U3O8 in kg a litre, in share 0). */
		final double[] mix = new double[5];
		/** What a kg of its slurry carries, for a concentrator slurry (the moly roaster's scene); empty for a gas. */
		SlurryAssay assay = SlurryAssay.EMPTY;

		@Override
		public double[] mixOf(Product wanted) {
			return mix;
		}
		/** m3 a second it makes; it holds a second's worth. */
		double rate = 1.0;
		/** m3 taken from it, all told. */
		double taken;
		private double buffer;
		private long last = -1;

		SourceEntity(BlockPos pos, BlockState state) {
			super(TYPE, pos, state);
		}

		private void refill() {
			long now = level == null ? 0 : level.getGameTime();
			if (last < 0) {
				buffer = rate;
			} else if (now > last) {
				buffer = Math.min(rate, buffer + rate * (now - last) / 20.0);
			}
			last = now;
		}

		@Override
		public SlurryAssay assayOf(Product wanted) {
			return wanted == product ? assay : SlurryAssay.EMPTY;
		}

		@Override
		public @Nullable Product offers(BlockPos cell, Direction side) {
			return product;
		}

		@Override
		public double available(BlockPos cell, Direction side, Product wanted, Pull pull) {
			if (wanted != product) {
				return 0;
			}
			refill();
			return buffer;
		}

		@Override
		public double take(BlockPos cell, Direction side, Product wanted, double max, Pull pull) {
			if (wanted != product) {
				return 0;
			}
			refill();
			double got = Math.max(0, Math.min(max, buffer));
			buffer -= got;
			taken += got;
			return got;
		}
	}
}
