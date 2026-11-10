package dev.wildcatter.block;

import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.multiblock.Footprint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A resin trailer (docs/multiblocks/isr-operations.md, 16d1): 10 m3 of ion-exchange resin in a trailer, set down 2 x 3 x 6 against a
 * satellite's IX columns to be filled with loaded resin, then against the central plant's columns to be eluted. Carried as one item, its
 * loading with it.
 */
public class ResinTrailerBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(2, 3, 6, 0, 0);

	public ResinTrailerBlock(Properties properties) {
		super(properties, FOOTPRINT, "resin_trailer");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new ResinTrailerBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.getBlockEntity(main(pos, state)) instanceof ResinTrailerBlockEntity trailer) {
			trailer.setU3o8(stack.getOrDefault(WildcatterComponents.RESIN_U3O8, 0.0));
		}
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		List<ItemStack> out = new ArrayList<>(super.getDrops(state, params));
		BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (entity instanceof ResinTrailerBlockEntity trailer && trailer.u3o8() > 1e-6) {
			for (ItemStack stack : out) {
				if (stack.is(asItem())) {
					stack.set(WildcatterComponents.RESIN_U3O8, trailer.u3o8());
				}
			}
		}
		return out;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof ResinTrailerBlockEntity trailer) {
			player.sendOverlayMessage(Component.literal(trailer.describe()));
		}
		return InteractionResult.SUCCESS;
	}
}
