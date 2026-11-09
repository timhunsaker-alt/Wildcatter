package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * One part of a sales gas meter station at true size (docs/multiblocks/sales-gas.md): the meter building (the controller), the filter
 * separator (receipt stations), the ultrasonic meter run, the regulator skid (delivery stations) and the pipeline tap. Each is placed
 * on its own; they work by touching. Sneak and right-click on the building sets RECEIPT or DELIVERY.
 */
public class SalesGasPartBlock extends BigPartBlock {
	public enum Part {
		METER_BUILDING("meter_building", Footprint.box(3, 3, 4, 0, 0)),
		FILTER_SEPARATOR("filter_separator", Footprint.box(2, 4, 2, 0, 0)),
		ULTRASONIC_METER_RUN("ultrasonic_meter_run", Footprint.box(1, 2, 8, 0, 0)),
		REGULATOR_SKID("regulator_skid", Footprint.box(2, 2, 5, 0, 0)),
		PIPELINE_TAP("pipeline_tap", Footprint.box(1, 2, 1, 0, 0));

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

	public SalesGasPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return part == Part.METER_BUILDING ? new MeterBuildingBlockEntity(pos, state) : new SalesGasUnitBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || part != Part.METER_BUILDING) {
			return null;
		}
		return createTickerHelper(type, WildcatterBlocks.METER_BUILDING_ENTITY, UnitMotorBlockEntity::tick);
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
		if (entity instanceof MeterBuildingBlockEntity building && player.isShiftKeyDown()) {
			player.sendOverlayMessage(Component.literal("Meter station set to " + building.toggleMode()));
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
