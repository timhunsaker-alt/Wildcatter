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
 * One part of the gas turbine plant at true size (docs/multiblocks/gas-turbine.md, combined-cycle.md): the turbine package
 * (controller), its inlet house, generator, generator breaker and exhaust stack; the HRSG, the 5 MW steam turbine, its generator, the
 * air-cooled condenser and the feed pump; and the 13.8 kV load bank. They work by touching.
 */
public class TurbinePartBlock extends BigPartBlock {
	public enum Part {
		GAS_TURBINE("gas_turbine", Footprint.box(4, 4, 10, 0, 0)),
		GT_INLET_HOUSE("gt_inlet_house", Footprint.box(4, 6, 4, 0, 0)),
		GT_GENERATOR("gt_generator", Footprint.box(4, 4, 6, 0, 0)),
		GEN_BREAKER("gen_breaker", Footprint.box(2, 3, 2, 0, 0)),
		GT_EXHAUST_STACK("gt_exhaust_stack", Footprint.box(3, 12, 3, 0, 0)),
		HRSG("hrsg", Footprint.box(5, 12, 12, 0, 0)),
		STEAM_TURBINE_5("steam_turbine_5", Footprint.box(3, 3, 6, 0, 0)),
		ST_GENERATOR("st_generator", Footprint.box(3, 3, 4, 0, 0)),
		AIR_COOLED_CONDENSER("air_cooled_condenser", Footprint.box(6, 8, 6, 0, 0)),
		HRSG_FEED_PUMP("hrsg_feed_pump", Footprint.box(2, 2, 4, 0, 0)),
		LOAD_BANK("load_bank", Footprint.box(3, 3, 6, 0, 0)),
		AERO_TURBINE("aero_turbine", Footprint.box(5, 5, 14, 0, 0)),
		AERO_INLET_HOUSE("aero_inlet_house", Footprint.box(6, 8, 5, 0, 0)),
		AERO_GENERATOR("aero_generator", Footprint.box(4, 4, 7, 0, 0)),
		AERO_EXHAUST_STACK("aero_exhaust_stack", Footprint.box(4, 14, 4, 0, 0)),
		GEN_BREAKER_3000("gen_breaker_3000", Footprint.box(2, 3, 2, 0, 0)),
		TG_GENERATOR_60("tg_generator_60", Footprint.box(4, 4, 8, 0, 0));

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

	public TurbinePartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case GAS_TURBINE, AERO_TURBINE -> new GasTurbineBlockEntity(pos, state);
			case GT_GENERATOR, ST_GENERATOR, AERO_GENERATOR, TG_GENERATOR_60 -> new GeneratorPartBlockEntity(pos, state);
			case GEN_BREAKER, GEN_BREAKER_3000 -> new GenBreakerBlockEntity(pos, state);
			case HRSG -> new HrsgBlockEntity(pos, state);
			case STEAM_TURBINE_5 -> new SteamCycleBlockEntity(pos, state);
			case AIR_COOLED_CONDENSER, HRSG_FEED_PUMP -> new UnitMotorBlockEntity(pos, state);
			case LOAD_BANK -> new LoadBankBlockEntity(pos, state);
			case GT_INLET_HOUSE, GT_EXHAUST_STACK, AERO_INLET_HOUSE, AERO_EXHAUST_STACK -> new TurbinePassiveBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case GAS_TURBINE, AERO_TURBINE -> createTickerHelper(type, WildcatterBlocks.GAS_TURBINE_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case STEAM_TURBINE_5 -> createTickerHelper(type, WildcatterBlocks.STEAM_CYCLE_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case AIR_COOLED_CONDENSER, HRSG_FEED_PUMP -> createTickerHelper(type, WildcatterBlocks.UNIT_MOTOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				default -> null;
			};
		}
		return switch (part) {
			case GAS_TURBINE, AERO_TURBINE -> createTickerHelper(type, WildcatterBlocks.GAS_TURBINE_ENTITY, GasTurbineBlockEntity::tick);
			case GT_GENERATOR, ST_GENERATOR, AERO_GENERATOR, TG_GENERATOR_60 -> createTickerHelper(type, WildcatterBlocks.GENERATOR_PART_ENTITY, GeneratorPartBlockEntity::tick);
			case HRSG -> createTickerHelper(type, WildcatterBlocks.HRSG_ENTITY, HrsgBlockEntity::tick);
			case STEAM_TURBINE_5 -> createTickerHelper(type, WildcatterBlocks.STEAM_CYCLE_ENTITY, SteamCycleBlockEntity::tick);
			case AIR_COOLED_CONDENSER, HRSG_FEED_PUMP -> createTickerHelper(type, WildcatterBlocks.UNIT_MOTOR_ENTITY, UnitMotorBlockEntity::tick);
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
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity entity = level.getBlockEntity(main(pos, state));
		if (entity instanceof GasTurbineBlockEntity gt) {
			player.sendOverlayMessage(player.isShiftKeyDown() ? gt.inspect() : gt.pushButton());
		} else if (entity instanceof LoadBankBlockEntity bank) {
			player.sendOverlayMessage(bank.step(!player.isShiftKeyDown()));
		} else if (entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive())) {
			return InteractionResult.SUCCESS;
		} else if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			player.sendOverlayMessage(machine.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
