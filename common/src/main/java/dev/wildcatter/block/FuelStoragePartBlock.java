package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 19b's parts at true size (docs/multiblocks/reactor-extensions.md): the cask loading station against the spent fuel pool's
 * controller, the horizontal concrete storage module (unbreakable while it holds a canister), the new-fuel inspection stand and the
 * personnel contamination monitor.
 */
public class FuelStoragePartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		CASK_LOADING_STATION("cask_loading_station", Footprint.box(3, 4, 4, 0, 0)),
		STORAGE_MODULE("storage_module", Footprint.box(4, 6, 6, 0, 0)),
		NEW_FUEL_INSPECTION_STAND("new_fuel_inspection_stand", Footprint.box(2, 6, 2, 0, 0)),
		PERSONNEL_MONITOR("personnel_monitor", Footprint.box(1, 3, 1, 0, 0));

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

	public FuelStoragePartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case CASK_LOADING_STATION -> new CaskLoadingStationBlockEntity(pos, state);
			case STORAGE_MODULE -> new StorageModuleBlockEntity(pos, state);
			case NEW_FUEL_INSPECTION_STAND -> new InspectionStandBlockEntity(pos, state);
			case PERSONNEL_MONITOR -> new PersonnelMonitorBlockEntity(pos, state);
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (part == Part.STORAGE_MODULE) {
			return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.STORAGE_MODULE_ENTITY, StorageModuleBlockEntity::tick);
		}
		BlockEntityType<UnitMotorBlockEntity> mine = (BlockEntityType<UnitMotorBlockEntity>) (BlockEntityType<?>) switch (part) {
			case CASK_LOADING_STATION -> WildcatterBlocks.CASK_STATION_ENTITY;
			case NEW_FUEL_INSPECTION_STAND -> WildcatterBlocks.INSPECTION_STAND_ENTITY;
			default -> WildcatterBlocks.PERSONNEL_MONITOR_ENTITY;
		};
		if (level.isClientSide()) {
			return createTickerHelper(type, mine, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
		}
		return createTickerHelper(type, mine, UnitMotorBlockEntity::tick);
	}

	/** A loaded storage module cannot be broken, as well casing cannot. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (part == Part.STORAGE_MODULE && level.getBlockEntity(main(pos, state)) instanceof StorageModuleBlockEntity module && !module.records().isEmpty()) {
			return 0;
		}
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.is(WildcatterItems.POWER_METER) && player.isShiftKeyDown() && level.getBlockEntity(main(pos, state)) instanceof CaskLoadingStationBlockEntity station) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.literal(station.link(level)));
			}
			return InteractionResult.SUCCESS;
		}
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity entity = level.getBlockEntity(main(pos, state));
		if (entity instanceof PersonnelMonitorBlockEntity monitor && player instanceof ServerPlayer sp) {
			player.sendOverlayMessage(Component.literal(monitor.wash(sp)));
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

	/** A hopper on top of the station works its canister bin; on the stand, its assembly bin (and one under it takes them out). */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
