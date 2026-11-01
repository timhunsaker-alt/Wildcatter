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
 * One part of the uranium mill at true size (docs/multiblocks/uranium-mill.md): the jaw crusher and SAG mill, the leach tank, the
 * CCD thickener and the SX circuit on steel pipe; the precipitation tank, yellowcake dryer and drum filler, a unit by touching.
 */
public class MillPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		JAW_CRUSHER("jaw_crusher", Footprint.box(3, 4, 4, 0, 0)),
		SAG_MILL("sag_mill", Footprint.box(7, 7, 5, 0, 0)),
		LEACH_TANK("leach_tank", Footprint.box(8, 9, 8, 0, 0)),
		CCD_THICKENER("ccd_thickener", Footprint.box(12, 4, 12, 0, 0)),
		SX_CIRCUIT("sx_circuit", Footprint.box(6, 3, 16, 0, 0)),
		PRECIPITATION_TANK("precipitation_tank", Footprint.box(4, 5, 4, 0, 0)),
		YELLOWCAKE_DRYER("yellowcake_dryer", Footprint.box(3, 8, 3, 0, 0)),
		DRUM_FILLER("drum_filler", Footprint.box(2, 4, 2, 0, 0));

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

	public MillPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case JAW_CRUSHER -> new JawCrusherBlockEntity(pos, state);
			case SAG_MILL -> new SagMillBlockEntity(pos, state);
			case LEACH_TANK -> new LeachTankBlockEntity(pos, state);
			case CCD_THICKENER -> new CcdThickenerBlockEntity(pos, state);
			case SX_CIRCUIT -> new SxCircuitBlockEntity(pos, state);
			case PRECIPITATION_TANK -> new PrecipitationTankBlockEntity(pos, state);
			case YELLOWCAKE_DRYER -> new YellowcakeDryerBlockEntity(pos, state);
			case DRUM_FILLER -> new DrumFillerBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case JAW_CRUSHER -> createTickerHelper(type, WildcatterBlocks.JAW_CRUSHER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case SAG_MILL -> createTickerHelper(type, WildcatterBlocks.SAG_MILL_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case LEACH_TANK -> createTickerHelper(type, WildcatterBlocks.LEACH_TANK_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case CCD_THICKENER -> createTickerHelper(type, WildcatterBlocks.CCD_THICKENER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case SX_CIRCUIT -> createTickerHelper(type, WildcatterBlocks.SX_CIRCUIT_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case PRECIPITATION_TANK -> createTickerHelper(type, WildcatterBlocks.PRECIPITATION_TANK_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case DRUM_FILLER -> createTickerHelper(type, WildcatterBlocks.DRUM_FILLER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case YELLOWCAKE_DRYER -> null;
			};
		}
		return switch (part) {
			case JAW_CRUSHER -> createTickerHelper(type, WildcatterBlocks.JAW_CRUSHER_ENTITY, UnitMotorBlockEntity::tick);
			case SAG_MILL -> createTickerHelper(type, WildcatterBlocks.SAG_MILL_ENTITY, UnitMotorBlockEntity::tick);
			case LEACH_TANK -> createTickerHelper(type, WildcatterBlocks.LEACH_TANK_ENTITY, UnitMotorBlockEntity::tick);
			case CCD_THICKENER -> createTickerHelper(type, WildcatterBlocks.CCD_THICKENER_ENTITY, UnitMotorBlockEntity::tick);
			case SX_CIRCUIT -> createTickerHelper(type, WildcatterBlocks.SX_CIRCUIT_ENTITY, UnitMotorBlockEntity::tick);
			case PRECIPITATION_TANK -> createTickerHelper(type, WildcatterBlocks.PRECIPITATION_TANK_ENTITY, UnitMotorBlockEntity::tick);
			case YELLOWCAKE_DRYER -> createTickerHelper(type, WildcatterBlocks.YELLOWCAKE_DRYER_ENTITY, YellowcakeDryerBlockEntity::tick);
			case DRUM_FILLER -> createTickerHelper(type, WildcatterBlocks.DRUM_FILLER_ENTITY, UnitMotorBlockEntity::tick);
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
		if (entity instanceof DrumFillerBlockEntity filler && filler.drums() > 0) {
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
