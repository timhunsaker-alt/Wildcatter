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
 * One part of a crude unit at true size (docs/multiblocks/crude-units.md): the early skid topping unit (column, heater, pump and
 * air-cooler skid) or the late crude unit (column, heater, desalter, charge pump, side strippers, overhead condenser). The parts form
 * a unit by touching; the column runs it.
 */
public class RefineryPartBlock extends BigPartBlock {
	public enum Part {
		TOPPING_COLUMN("topping_column", Footprint.box(2, 12, 2, 0, 0)),
		TOPPING_HEATER("topping_heater", Footprint.box(2, 6, 2, 0, 0)),
		TOPPING_SKID("topping_skid", Footprint.box(3, 3, 4, 0, 0)),
		ATMOSPHERIC_COLUMN("atmospheric_column", Footprint.box(3, 30, 3, 0, 0)),
		CRUDE_HEATER("crude_heater", Footprint.box(6, 14, 8, 0, 0)),
		DESALTER("desalter", Footprint.box(3, 4, 10, 0, 0)),
		CRUDE_CHARGE_PUMP("crude_charge_pump", Footprint.box(2, 2, 5, 0, 0)),
		SIDE_STRIPPERS("side_strippers", Footprint.box(2, 10, 1, 0, 0)),
		OVERHEAD_CONDENSER("overhead_condenser", Footprint.box(4, 4, 8, 0, 0));

		private final String id;
		private final Footprint footprint;

		Part(String id, Footprint footprint) {
			this.id = id;
			this.footprint = footprint;
		}

		public String id() {
			return id;
		}

		boolean motor() {
			return this == TOPPING_SKID || this == DESALTER || this == CRUDE_CHARGE_PUMP || this == OVERHEAD_CONDENSER;
		}
	}

	private final Part part;

	public RefineryPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case TOPPING_COLUMN -> new ToppingColumnBlockEntity(pos, state);
			case ATMOSPHERIC_COLUMN -> new CrudeColumnBlockEntity(pos, state);
			case TOPPING_HEATER, CRUDE_HEATER -> new FiredHeaterBlockEntity(pos, state);
			case SIDE_STRIPPERS -> new StripperBlockEntity(pos, state);
			case TOPPING_SKID, DESALTER, CRUDE_CHARGE_PUMP, OVERHEAD_CONDENSER -> new UnitMotorBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return part.motor() ? createTickerHelper(type, WildcatterBlocks.UNIT_MOTOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e)) : null;
		}
		return switch (part) {
			case TOPPING_COLUMN -> createTickerHelper(type, WildcatterBlocks.TOPPING_COLUMN_ENTITY, ToppingColumnBlockEntity::tick);
			case ATMOSPHERIC_COLUMN -> createTickerHelper(type, WildcatterBlocks.CRUDE_COLUMN_ENTITY, CrudeColumnBlockEntity::tick);
			case TOPPING_SKID, DESALTER, CRUDE_CHARGE_PUMP, OVERHEAD_CONDENSER -> createTickerHelper(type, WildcatterBlocks.UNIT_MOTOR_ENTITY, UnitMotorBlockEntity::tick);
			default -> null;
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
			boolean shocked = machine instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
