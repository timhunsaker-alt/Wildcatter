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
 * One part of the plant's fuel handling at true size (docs/multiblocks/fuel-handling.md): the refuelling machine against the vessel,
 * the fuel transfer system, and the spent fuel pool's controller with its pool in front of it.
 */
public class FuelHandlingPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		REFUELLING_MACHINE("refuelling_machine", Footprint.box(4, 4, 4, 0, 0)),
		FUEL_TRANSFER_SYSTEM("fuel_transfer_system", Footprint.box(2, 2, 6, 0, 0)),
		SPENT_FUEL_POOL("spent_fuel_pool", Footprint.box(4, 3, 4, 0, 0));

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

	public FuelHandlingPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case REFUELLING_MACHINE -> new RefuellingMachineBlockEntity(pos, state);
			case SPENT_FUEL_POOL -> new SpentFuelPoolBlockEntity(pos, state);
			default -> new BigPartBlockEntity(pos, state);
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (part == Part.FUEL_TRANSFER_SYSTEM) {
			return null;
		}
		BlockEntityType<UnitMotorBlockEntity> mine = (BlockEntityType<UnitMotorBlockEntity>) (BlockEntityType<?>) (part == Part.REFUELLING_MACHINE
				? WildcatterBlocks.REFUELLING_MACHINE_ENTITY : WildcatterBlocks.SPENT_FUEL_POOL_ENTITY);
		if (level.isClientSide()) {
			return createTickerHelper(type, mine, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
		}
		return createTickerHelper(type, mine, UnitMotorBlockEntity::tick);
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
		if (entity instanceof RefuellingMachineBlockEntity machine && player.isShiftKeyDown()) {
			player.sendOverlayMessage(Component.literal(machine.nextMode(level)));
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof SpentFuelPoolBlockEntity pool && player.isShiftKeyDown()) {
			player.sendOverlayMessage(Component.literal(pool.toggleCaskMode()));
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		} else if (entity instanceof BigPartBlockEntity) {
			player.sendOverlayMessage(Component.literal("Fuel transfer system: the upender, carriage and transfer tube from the refuelling machine to the pool"));
		}
		return InteractionResult.SUCCESS;
	}

	/** A hopper on top of the refuelling machine works its new-fuel bin. */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
