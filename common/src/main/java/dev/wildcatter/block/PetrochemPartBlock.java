package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
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
 * One part of the cracker and polyethylene train at true size (docs/multiblocks/cracker.md): the cracking furnace, the quench tower,
 * the cracked gas compressor and the cold box on steel pipe; the polyethylene reactor and its pelletizer, a unit by touching.
 */
public class PetrochemPartBlock extends BigPartBlock {
	public enum Part {
		CRACKING_FURNACE("cracking_furnace", Footprint.box(4, 14, 6, 0, 0)),
		QUENCH_TOWER("quench_tower", Footprint.box(2, 12, 2, 0, 0)),
		CRACKED_GAS_COMPRESSOR("cracked_gas_compressor", Footprint.box(4, 4, 8, 0, 0)),
		COLD_BOX("cold_box", Footprint.box(3, 10, 3, 0, 0)),
		POLYETHYLENE_REACTOR("polyethylene_reactor", Footprint.box(3, 16, 3, 0, 0)),
		PELLETIZER("pelletizer", Footprint.box(2, 3, 6, 0, 0));

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

	public PetrochemPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case CRACKING_FURNACE -> new CrackingFurnaceBlockEntity(pos, state);
			case QUENCH_TOWER -> new QuenchTowerBlockEntity(pos, state);
			case CRACKED_GAS_COMPRESSOR -> new GasCompressorBlockEntity(pos, state);
			case COLD_BOX -> new ColdBoxBlockEntity(pos, state);
			case POLYETHYLENE_REACTOR -> new PolyethyleneReactorBlockEntity(pos, state);
			case PELLETIZER -> new PelletizerBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case CRACKED_GAS_COMPRESSOR -> createTickerHelper(type, WildcatterBlocks.GAS_COMPRESSOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case POLYETHYLENE_REACTOR -> createTickerHelper(type, WildcatterBlocks.PE_REACTOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case PELLETIZER -> createTickerHelper(type, WildcatterBlocks.PELLETIZER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				default -> null;
			};
		}
		return switch (part) {
			case CRACKING_FURNACE -> createTickerHelper(type, WildcatterBlocks.CRACKING_FURNACE_ENTITY, CrackingFurnaceBlockEntity::tick);
			case QUENCH_TOWER -> createTickerHelper(type, WildcatterBlocks.QUENCH_TOWER_ENTITY, QuenchTowerBlockEntity::tick);
			case CRACKED_GAS_COMPRESSOR -> createTickerHelper(type, WildcatterBlocks.GAS_COMPRESSOR_ENTITY, GasCompressorBlockEntity::tick);
			case COLD_BOX -> createTickerHelper(type, WildcatterBlocks.COLD_BOX_ENTITY, ColdBoxBlockEntity::tick);
			case POLYETHYLENE_REACTOR -> createTickerHelper(type, WildcatterBlocks.PE_REACTOR_ENTITY, UnitMotorBlockEntity::tick);
			case PELLETIZER -> createTickerHelper(type, WildcatterBlocks.PELLETIZER_ENTITY, UnitMotorBlockEntity::tick);
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
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity entity = level.getBlockEntity(main(pos, state));
		if (entity instanceof PelletizerBlockEntity pelletizer && pelletizer.sheets() > 0) {
			pelletizer.giveAll(player);
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive())
					|| entity instanceof GasCompressorBlockEntity c && Shock.touch((ServerLevel) level, pos, player, c.motor().voltage(), c.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
