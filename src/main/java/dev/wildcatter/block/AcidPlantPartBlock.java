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
 * One part of the sulfuric acid plant at true size (docs/multiblocks/acid-plant.md): the sulfur melter, drying tower, main blower,
 * sulfur furnace and waste heat boiler that make burner gas; the converter and its two absorbers that make the acid.
 */public class AcidPlantPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		SULFUR_MELTER("sulfur_melter", Footprint.box(4, 3, 8, 0, 0)),
		DRYING_TOWER("drying_tower", Footprint.box(3, 12, 3, 0, 0)),
		MAIN_BLOWER("main_blower", Footprint.box(3, 3, 5, 0, 0)),
		SULFUR_FURNACE("sulfur_furnace", Footprint.box(3, 3, 10, 0, 0)),
		WASTE_HEAT_BOILER("waste_heat_boiler", Footprint.box(3, 4, 8, 0, 0)),
		CONVERTER("converter", Footprint.box(5, 15, 5, 0, 0)),
		INTERPASS_ABSORBER("interpass_absorber", Footprint.box(3, 12, 3, 0, 0)),
		FINAL_ABSORBER("final_absorber", Footprint.box(3, 20, 3, 0, 0));

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

	public AcidPlantPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case SULFUR_MELTER -> new SulfurMelterBlockEntity(pos, state);
			case DRYING_TOWER, INTERPASS_ABSORBER -> new PassivePartBlockEntity(pos, state);
			case MAIN_BLOWER -> new UnitMotorBlockEntity(pos, state);
			case SULFUR_FURNACE -> new SulfurFurnaceBlockEntity(pos, state);
			case WASTE_HEAT_BOILER -> new WasteHeatBoilerBlockEntity(pos, state);
			case CONVERTER -> new ConverterBlockEntity(pos, state);
			case FINAL_ABSORBER -> new FinalAbsorberBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return part == Part.MAIN_BLOWER ? createTickerHelper(type, WildcatterBlocks.UNIT_MOTOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e)) : null;
		}
		return switch (part) {
			case SULFUR_MELTER -> createTickerHelper(type, WildcatterBlocks.SULFUR_MELTER_ENTITY, SulfurMelterBlockEntity::tick);
			case DRYING_TOWER, INTERPASS_ABSORBER, WASTE_HEAT_BOILER, FINAL_ABSORBER -> null;
			case MAIN_BLOWER -> createTickerHelper(type, WildcatterBlocks.UNIT_MOTOR_ENTITY, UnitMotorBlockEntity::tick);
			case SULFUR_FURNACE -> createTickerHelper(type, WildcatterBlocks.SULFUR_FURNACE_ENTITY, SulfurFurnaceBlockEntity::tick);
			case CONVERTER -> createTickerHelper(type, WildcatterBlocks.CONVERTER_ENTITY, ConverterBlockEntity::tick);
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

		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive())
					|| entity instanceof GasCompressorBlockEntity c && Shock.touch((ServerLevel) level, pos, player, c.motor().voltage(), c.isLive());
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
