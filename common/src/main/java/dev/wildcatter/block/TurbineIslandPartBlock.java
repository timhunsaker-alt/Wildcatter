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
import org.jspecify.annotations.Nullable;

/**
 * One part of the turbine island at true size (docs/multiblocks/turbine-island.md): the valve chest, HP turbine, moisture separator
 * reheater and LP turbine, the condenser and its condensate pumps, the deaerator, feed pumps and HP heaters, and the circulating
 * water pumps at the intake. They work by touching; the HP turbine is the controller.
 */
public class TurbineIslandPartBlock extends BigPartBlock {
	public enum Part {
		TURBINE_VALVE_CHEST("turbine_valve_chest", Footprint.box(3, 4, 3, 0, 0)),
		HP_TURBINE("hp_turbine", Footprint.box(4, 4, 6, 0, 0)),
		MOISTURE_SEPARATOR_REHEATER("moisture_separator_reheater", Footprint.box(3, 4, 10, 0, 0)),
		LP_TURBINE("lp_turbine", Footprint.box(6, 5, 8, 0, 0)),
		MAIN_CONDENSER("main_condenser", Footprint.box(6, 5, 8, 0, 0)),
		CONDENSATE_PUMP("condensate_pump", Footprint.box(2, 3, 2, 0, 0)),
		DEAERATOR("deaerator", Footprint.box(3, 5, 8, 0, 0)),
		MAIN_FEED_PUMP("main_feed_pump", Footprint.box(2, 2, 5, 0, 0)),
		HP_FEEDWATER_HEATER("hp_feedwater_heater", Footprint.box(2, 3, 8, 0, 0)),
		CIRCULATING_WATER_PUMP("circulating_water_pump", Footprint.box(3, 8, 3, 0, 0));

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

	public TurbineIslandPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case HP_TURBINE -> new HpTurbineBlockEntity(pos, state);
			case MAIN_CONDENSER -> new CondenserBlockEntity(pos, state);
			case DEAERATOR -> new DeaeratorBlockEntity(pos, state);
			case HP_FEEDWATER_HEATER -> new HpHeaterBlockEntity(pos, state);
			case CIRCULATING_WATER_PUMP -> new CirculatingWaterPumpBlockEntity(pos, state);
			case CONDENSATE_PUMP, MAIN_FEED_PUMP -> new UnitMotorBlockEntity(pos, state);
			default -> new BigPartBlockEntity(pos, state);
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		switch (part) {
			case HP_TURBINE -> {
				return createTickerHelper(type, WildcatterBlocks.HP_TURBINE_ENTITY,
						level.isClientSide() ? (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e) : HpTurbineBlockEntity::tick);
			}
			case CONDENSATE_PUMP, MAIN_FEED_PUMP, CIRCULATING_WATER_PUMP -> {
				BlockEntityType<UnitMotorBlockEntity> mine = (BlockEntityType<UnitMotorBlockEntity>) (BlockEntityType<?>) (part == Part.CIRCULATING_WATER_PUMP
						? WildcatterBlocks.CW_PUMP_ENTITY : WildcatterBlocks.UNIT_MOTOR_ENTITY);
				if (level.isClientSide()) {
					return createTickerHelper(type, mine, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				}
				return createTickerHelper(type, mine, UnitMotorBlockEntity::tick);
			}
			default -> {
				return null;
			}
		}
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
		if (entity instanceof HpTurbineBlockEntity turbine && player.isShiftKeyDown()) {
			player.sendOverlayMessage(Component.literal(turbine.resetTrip(level)));
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		} else if (entity instanceof BigPartBlockEntity) {
			player.sendOverlayMessage(Component.literal(switch (part) {
				case TURBINE_VALVE_CHEST -> "Turbine valve chest: main stop and governor valves; steam in on its top";
				case MOISTURE_SEPARATOR_REHEATER -> "Moisture separator reheater: chevron separators and a live-steam reheat bundle";
				case LP_TURBINE -> "LP turbine: double-flow, exhausting into the condenser";
				default -> part.id();
			}));
		}
		return InteractionResult.SUCCESS;
	}
}
