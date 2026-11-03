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
 * One part of the anode carbon plants at true size (docs/multiblocks/delayed-coker.md, coke-ovens.md, anode-plant.md): the delayed
 * coker and its calciner, the coke ovens and tar still, and the anode plant from the paste plant to the rodding shop.
 */
public class CarbonPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		COKER_FRACTIONATOR("coker_fractionator", 3, 18, 3),
		COKER_HEATER("coker_heater", 4, 10, 6),
		COKE_DRUM("coke_drum", 3, 16, 3),
		COKE_PIT("coke_pit", 6, 4, 8),
		COKE_KILN("coke_kiln", 3, 3, 30),
		COKE_COOLER("coke_cooler", 2, 3, 12),
		COAL_TOWER("coal_tower", 4, 12, 4),
		COKE_OVEN_BATTERY("coke_oven_battery", 10, 8, 16),
		PUSHER_MACHINE("pusher_machine", 4, 6, 16),
		COKE_QUENCH_TOWER("coke_quench_tower", 5, 20, 5),
		BYPRODUCT_PLANT("byproduct_plant", 5, 10, 8),
		TAR_STILL("tar_still", 3, 12, 3),
		PASTE_PLANT("paste_plant", 5, 10, 6),
		FORMING_PRESS("forming_press", 4, 5, 4),
		BAKING_FURNACE("baking_furnace", 10, 5, 24),
		RODDING_SHOP("rodding_shop", 4, 5, 12);

		private final String id;
		public final int w, h, l;

		Part(String id, int w, int h, int l) {
			this.id = id;
			this.w = w;
			this.h = h;
			this.l = l;
		}

		public String id() {
			return id;
		}
	}

	private final Part part;

	public CarbonPartBlock(Properties properties, Part part) {
		super(properties, Footprint.box(part.w, part.h, part.l, 0, 0), part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case COKER_FRACTIONATOR -> new CokerFractionatorBlockEntity(pos, state);
			case COKER_HEATER -> new CokerHeaterBlockEntity(pos, state);
			case COKE_DRUM -> new CokeDrumBlockEntity(pos, state);
			case COKE_PIT -> new CokePitBlockEntity(pos, state);
			case COKE_KILN -> new CokeKilnBlockEntity(pos, state);
			case COKE_COOLER -> new CokeCoolerBlockEntity(pos, state);
			case COAL_TOWER -> new CoalTowerBlockEntity(pos, state);
			case COKE_OVEN_BATTERY -> new CokeOvenBatteryBlockEntity(pos, state);
			case PUSHER_MACHINE -> new UnitMotorBlockEntity(pos, state);
			case COKE_QUENCH_TOWER -> new CokeQuenchTowerBlockEntity(pos, state);
			case BYPRODUCT_PLANT -> new ByproductPlantBlockEntity(pos, state);
			case TAR_STILL -> new TarStillBlockEntity(pos, state);
			case PASTE_PLANT -> new PastePlantBlockEntity(pos, state);
			case FORMING_PRESS -> new FormingPressBlockEntity(pos, state);
			case BAKING_FURNACE -> new BakingFurnaceBlockEntity(pos, state);
			case RODDING_SHOP -> new RoddingShopBlockEntity(pos, state);
		};
	}

	private BlockEntityType<? extends UnitMotorBlockEntity> entityType() {
		return switch (part) {
			case COKER_FRACTIONATOR -> WildcatterBlocks.COKER_FRACTIONATOR_ENTITY;
			case COKER_HEATER -> WildcatterBlocks.COKER_HEATER_ENTITY;
			case COKE_DRUM -> WildcatterBlocks.COKE_DRUM_ENTITY;
			case COKE_PIT -> WildcatterBlocks.COKE_PIT_ENTITY;
			case COKE_KILN -> WildcatterBlocks.COKE_KILN_ENTITY;
			case COKE_COOLER -> WildcatterBlocks.COKE_COOLER_ENTITY;
			case COAL_TOWER -> WildcatterBlocks.COAL_TOWER_ENTITY;
			case COKE_OVEN_BATTERY -> WildcatterBlocks.COKE_OVEN_BATTERY_ENTITY;
			case PUSHER_MACHINE -> WildcatterBlocks.UNIT_MOTOR_ENTITY;
			case COKE_QUENCH_TOWER -> WildcatterBlocks.COKE_QUENCH_TOWER_ENTITY;
			case BYPRODUCT_PLANT -> WildcatterBlocks.BYPRODUCT_PLANT_ENTITY;
			case TAR_STILL -> WildcatterBlocks.TAR_STILL_ENTITY;
			case PASTE_PLANT -> WildcatterBlocks.PASTE_PLANT_ENTITY;
			case FORMING_PRESS -> WildcatterBlocks.FORMING_PRESS_ENTITY;
			case BAKING_FURNACE -> WildcatterBlocks.BAKING_FURNACE_ENTITY;
			case RODDING_SHOP -> WildcatterBlocks.RODDING_SHOP_ENTITY;
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		BlockEntityType<UnitMotorBlockEntity> mine = (BlockEntityType<UnitMotorBlockEntity>) entityType();
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
		if (entity instanceof FillerBlockEntity filler && filler.outCount() > 0) {
			filler.giveAll(player);
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

	/** A hopper on top of the part, or under it, works its main cell's bins. */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
