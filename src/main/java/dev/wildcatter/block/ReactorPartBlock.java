package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
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
 * One part of the nuclear island at true size (docs/multiblocks/reactor.md): the vessel and its CRDM head, the console, the two
 * steam generators and their reactor coolant pumps, the pressurizer, the CVCS skid and the aux feed pump. They work by touching.
 */
public class ReactorPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		REACTOR_VESSEL("reactor_vessel", Footprint.box(3, 10, 3, 0, 0)),
		CRDM_HEAD("crdm_head", Footprint.box(3, 3, 3, 0, 0)),
		REACTOR_CONSOLE("reactor_console", Footprint.box(3, 2, 1, 0, 0)),
		STEAM_GENERATOR("steam_generator", Footprint.box(3, 12, 3, 0, 0)),
		REACTOR_COOLANT_PUMP("reactor_coolant_pump", Footprint.box(2, 5, 2, 0, 0)),
		PRESSURIZER("pressurizer", Footprint.box(2, 8, 2, 0, 0)),
		CVCS_SKID("cvcs_skid", Footprint.box(3, 3, 6, 0, 0)),
		AUX_FEED_PUMP("aux_feed_pump", Footprint.box(2, 2, 4, 0, 0));

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

	public ReactorPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case REACTOR_VESSEL -> new ReactorVesselBlockEntity(pos, state);
			case REACTOR_CONSOLE -> new ReactorConsoleBlockEntity(pos, state);
			case STEAM_GENERATOR -> new SteamGeneratorBlockEntity(pos, state);
			case REACTOR_COOLANT_PUMP -> new ReactorCoolantPumpBlockEntity(pos, state);
			case CVCS_SKID -> new CvcsBlockEntity(pos, state);
			case AUX_FEED_PUMP -> new AuxFeedPumpBlockEntity(pos, state);
			default -> new UnitMotorBlockEntity(pos, state);
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		switch (part) {
			case REACTOR_VESSEL -> {
				return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.REACTOR_VESSEL_ENTITY, ReactorVesselBlockEntity::tick);
			}
			case REACTOR_CONSOLE, STEAM_GENERATOR -> {
				return null;
			}
			default -> {
				BlockEntityType<UnitMotorBlockEntity> mine = (BlockEntityType<UnitMotorBlockEntity>) (BlockEntityType<?>) switch (part) {
					case REACTOR_COOLANT_PUMP -> WildcatterBlocks.REACTOR_COOLANT_PUMP_ENTITY;
					case CVCS_SKID -> WildcatterBlocks.CVCS_ENTITY;
					case AUX_FEED_PUMP -> WildcatterBlocks.AUX_FEED_PUMP_ENTITY;
					default -> WildcatterBlocks.UNIT_MOTOR_ENTITY;
				};
				if (level.isClientSide()) {
					return createTickerHelper(type, mine, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				}
				return createTickerHelper(type, mine, UnitMotorBlockEntity::tick);
			}
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		if (read != null) {
			return read;
		}
		if (level.getBlockEntity(main(pos, state)) instanceof ReactorVesselBlockEntity vessel
				&& (stack.is(WildcatterBlocks.FUEL_ASSEMBLY.asItem()) || stack.is(WildcatterItems.RCCA))) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.literal(vessel.load(stack)));
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity entity = level.getBlockEntity(main(pos, state));
		if (player.isShiftKeyDown()) {
			String said = switch (entity) {
				case ReactorVesselBlockEntity vessel -> vessel.unload(player);
				case ReactorConsoleBlockEntity console -> console.nextSetpoint(level);
				case CvcsBlockEntity cvcs -> cvcs.nextTarget(level);
				case ReactorCoolantPumpBlockEntity pump -> pump.toggle();
				default -> null;
			};
			if (said != null) {
				player.sendOverlayMessage(Component.literal(said));
				return InteractionResult.SUCCESS;
			}
		}
		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** A hopper on top of the CVCS works its boric acid bin. */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
