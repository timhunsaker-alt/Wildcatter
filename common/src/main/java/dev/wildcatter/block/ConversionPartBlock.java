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
 * One part of the conversion plant at true size (docs/multiblocks/conversion.md): the reduction reactor, hydrofluorinator,
 * fluorinator, cold traps, distillation columns and cylinder filler in a line by touching; the steam reformer, the HF kiln and its
 * column, and the rectifier against the fluorine cell house, on steel pipe.
 */
public class ConversionPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		REDUCTION_REACTOR("reduction_reactor", Footprint.box(3, 10, 3, 0, 0)),
		HYDROFLUORINATOR("hydrofluorinator", Footprint.box(3, 10, 6, 0, 0)),
		FLUORINATOR("fluorinator", Footprint.box(3, 10, 3, 0, 0)),
		COLD_TRAPS("cold_traps", Footprint.box(3, 4, 6, 0, 0)),
		DISTILLATION_COLUMNS("distillation_columns", Footprint.box(3, 12, 3, 0, 0)),
		CYLINDER_FILLER("cylinder_filler", Footprint.box(3, 3, 6, 0, 0)),
		STEAM_REFORMER("steam_reformer", Footprint.box(3, 8, 4, 0, 0)),
		HF_KILN("hf_kiln", Footprint.box(3, 4, 10, 0, 0)),
		HF_COLUMN("hf_column", Footprint.box(2, 10, 2, 0, 0)),
		RECTIFIER("rectifier", Footprint.box(3, 4, 4, 0, 0)),
		FLUORINE_CELL_HOUSE("fluorine_cell_house", Footprint.box(6, 4, 12, 0, 0));

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

	public ConversionPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case REDUCTION_REACTOR -> new ReductionReactorBlockEntity(pos, state);
			case HYDROFLUORINATOR, FLUORINATOR -> new ConversionStageBlockEntity(WildcatterBlocks.CONVERSION_STAGE_ENTITY, pos, state);
			case COLD_TRAPS, DISTILLATION_COLUMNS -> new Uf6StageBlockEntity(WildcatterBlocks.UF6_STAGE_ENTITY, pos, state);
			case CYLINDER_FILLER -> new CylinderFillerBlockEntity(pos, state);
			case STEAM_REFORMER -> new SteamReformerBlockEntity(pos, state);
			case HF_KILN -> new HfKilnBlockEntity(pos, state);
			case HF_COLUMN -> new HfColumnBlockEntity(pos, state);
			case RECTIFIER -> new RectifierBlockEntity(pos, state);
			case FLUORINE_CELL_HOUSE -> new FluorineCellHouseBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case COLD_TRAPS, DISTILLATION_COLUMNS -> createTickerHelper(type, WildcatterBlocks.UF6_STAGE_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case CYLINDER_FILLER -> createTickerHelper(type, WildcatterBlocks.CYLINDER_FILLER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case HF_KILN -> createTickerHelper(type, WildcatterBlocks.HF_KILN_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				default -> null;
			};
		}
		return switch (part) {
			case REDUCTION_REACTOR -> createTickerHelper(type, WildcatterBlocks.REDUCTION_REACTOR_ENTITY, ReductionReactorBlockEntity::tick);
			case HYDROFLUORINATOR, FLUORINATOR -> createTickerHelper(type, WildcatterBlocks.CONVERSION_STAGE_ENTITY, ConversionStageBlockEntity::tick);
			case COLD_TRAPS, DISTILLATION_COLUMNS -> createTickerHelper(type, WildcatterBlocks.UF6_STAGE_ENTITY, UnitMotorBlockEntity::tick);
			case CYLINDER_FILLER -> createTickerHelper(type, WildcatterBlocks.CYLINDER_FILLER_ENTITY, UnitMotorBlockEntity::tick);
			case STEAM_REFORMER -> createTickerHelper(type, WildcatterBlocks.STEAM_REFORMER_ENTITY, SteamReformerBlockEntity::tick);
			case HF_KILN -> createTickerHelper(type, WildcatterBlocks.HF_KILN_ENTITY, UnitMotorBlockEntity::tick);
			case HF_COLUMN -> createTickerHelper(type, WildcatterBlocks.HF_COLUMN_ENTITY, HfColumnBlockEntity::tick);
			case RECTIFIER -> null;
			case FLUORINE_CELL_HOUSE -> createTickerHelper(type, WildcatterBlocks.FLUORINE_CELL_HOUSE_ENTITY, FluorineCellHouseBlockEntity::tick);
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
		if (entity instanceof CylinderFillerBlockEntity filler && filler.full() > 0) {
			filler.giveAll(player);
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

	/** A hopper on top of the part, or under it, works its main cell's bins. */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
