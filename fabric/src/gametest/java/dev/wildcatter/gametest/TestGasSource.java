package dev.wildcatter.gametest;

import dev.wildcatter.block.FluidNode;
import dev.wildcatter.fluid.Product;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The client scenes' stand-in gas source (docs/biogas.md, "How another mod supplies it"), as the NeoForge test mod's: a block of this
 * test mod whose block entity implements Wildcatter's {@link FluidNode} and offers one product at a set rate. It is what another mod's
 * digester does to put biogas on Wildcatter's steel pipe. Registered by this test mod's main entrypoint.
 */
public final class TestGasSource implements ModInitializer {
	public static final Identifier ID = Identifier.fromNamespaceAndPath("wildcatter_gametest", "gas_source");
	static Block BLOCK;
	static BlockEntityType<SourceEntity> TYPE;

	@Override
	public void onInitialize() {
		BLOCK = Registry.register(BuiltInRegistries.BLOCK, ID, new SourceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, ID))));
		TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ID, new BlockEntityType<>(SourceEntity::new, java.util.Set.of(BLOCK)));
	}

	static final class SourceBlock extends Block implements EntityBlock {
		SourceBlock(Properties properties) {
			super(properties);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new SourceEntity(pos, state);
		}
	}

	static final class SourceEntity extends BlockEntity implements FluidNode {
		Product product = Product.BIOGAS;
		double rate = 1.0;
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
