package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One part of the gas plant at true size (docs/multiblocks/gas-plant.md): the gas compressor, the TEG dehydrator, the NGL chiller,
 * its refrigeration compressor, the stabilizer and the LPG bullet. Each stands on its own, joined to the others by steel pipe on its
 * nozzles; the refrigeration compressor cools the chiller its skid touches.
 */
public class GasPlantPartBlock extends BigPartBlock {
	public enum Part {
		GAS_COMPRESSOR("gas_compressor", Footprint.box(4, 4, 9, 0, 0)),
		TEG_DEHYDRATOR("teg_dehydrator", Footprint.box(3, 8, 5, 0, 0)),
		NGL_CHILLER("ngl_chiller", Footprint.box(3, 3, 7, 0, 0)),
		REFRIGERATION_COMPRESSOR("refrigeration_compressor", Footprint.box(3, 3, 6, 0, 0)),
		STABILIZER("stabilizer", Footprint.box(2, 12, 2, 0, 0)),
		LPG_BULLET("lpg_bullet", Footprint.box(3, 3, 12, 0, 0));

		private final String id;
		private final Footprint footprint;

		Part(String id, Footprint footprint) {
			this.id = id;
			this.footprint = footprint;
		}

		public String id() {
			return id;
		}
	}

	private final Part part;

	public GasPlantPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case GAS_COMPRESSOR, REFRIGERATION_COMPRESSOR -> new GasCompressorBlockEntity(pos, state);
			case TEG_DEHYDRATOR -> new DehydratorBlockEntity(pos, state);
			case NGL_CHILLER -> new ChillerBlockEntity(pos, state);
			case STABILIZER -> new StabilizerBlockEntity(pos, state);
			case LPG_BULLET -> new StorageTankBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return part == Part.GAS_COMPRESSOR || part == Part.REFRIGERATION_COMPRESSOR
					? createTickerHelper(type, WildcatterBlocks.GAS_COMPRESSOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e)) : null;
		}
		return switch (part) {
			case GAS_COMPRESSOR, REFRIGERATION_COMPRESSOR -> createTickerHelper(type, WildcatterBlocks.GAS_COMPRESSOR_ENTITY, GasCompressorBlockEntity::tick);
			case TEG_DEHYDRATOR -> createTickerHelper(type, WildcatterBlocks.DEHYDRATOR_ENTITY, DehydratorBlockEntity::tick);
			case NGL_CHILLER -> createTickerHelper(type, WildcatterBlocks.CHILLER_ENTITY, ChillerBlockEntity::tick);
			case STABILIZER -> createTickerHelper(type, WildcatterBlocks.STABILIZER_ENTITY, StabilizerBlockEntity::tick);
			case LPG_BULLET -> createTickerHelper(type, WildcatterBlocks.STORAGE_TANK_ENTITY, StorageTankBlockEntity::tick);
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = machine instanceof GasCompressorBlockEntity motor
					&& Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
