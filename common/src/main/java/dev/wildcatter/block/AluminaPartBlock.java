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

/**
 * One part of the Bayer refinery or its helpers at true size (docs/multiblocks/bayer-refinery.md, alumina-plant.md): the line from
 * the ball mill to the calciner, the evaporator and causticizer that close the liquor loop, the package boiler, the lime kiln, the
 * AlF3 reactor, the bagging station and the chlorine filler.
 */
public class AluminaPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		BALL_MILL("ball_mill", 4, 4, 8),
		DIGESTER("digester", 5, 8, 5),
		FLASH_TANKS("flash_tanks", 3, 8, 6),
		SETTLER("settler", 10, 4, 10),
		MUD_WASHER("mud_washer", 10, 4, 10),
		RESIDUE_FILTER_PRESS("residue_filter_press", 4, 4, 8),
		POLISHING_FILTER("polishing_filter", 3, 5, 6),
		PRECIPITATOR("precipitator", 12, 18, 12),
		HYDRATE_FILTER("hydrate_filter", 4, 4, 10),
		CALCINER("calciner", 6, 20, 6),
		EVAPORATOR("evaporator", 4, 12, 8),
		CAUSTICIZER("causticizer", 3, 5, 3),
		PACKAGE_BOILER("package_boiler", 4, 5, 9),
		LIME_KILN("lime_kiln", 3, 14, 3),
		ALF3_REACTOR("alf3_reactor", 3, 10, 3),
		BAGGING_STATION("bagging_station", 2, 4, 2),
		CHLORINE_FILLER("chlorine_filler", 3, 4, 3);

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

		public static Part of(String id) {
			for (Part p : values()) {
				if (p.id.equals(id)) {
					return p;
				}
			}
			return null;
		}
	}

	private final Part part;

	public AluminaPartBlock(Properties properties, Part part) {
		super(properties, Footprint.box(part.w, part.h, part.l, 0, 0), part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case BALL_MILL, DIGESTER, FLASH_TANKS, SETTLER, MUD_WASHER, POLISHING_FILTER, PRECIPITATOR, EVAPORATOR, PACKAGE_BOILER -> new BayerStageBlockEntity(pos, state);
			case RESIDUE_FILTER_PRESS -> new ResidueFilterPressBlockEntity(pos, state);
			case HYDRATE_FILTER -> new HydrateFilterBlockEntity(pos, state);
			case CALCINER -> new CalcinerBlockEntity(pos, state);
			case CAUSTICIZER -> new CausticizerBlockEntity(pos, state);
			case LIME_KILN -> new LimeKilnBlockEntity(pos, state);
			case ALF3_REACTOR -> new Alf3ReactorBlockEntity(pos, state);
			case BAGGING_STATION -> new BaggingStationBlockEntity(pos, state);
			case CHLORINE_FILLER -> new ChlorineFillerBlockEntity(pos, state);
		};
	}

	private BlockEntityType<? extends UnitMotorBlockEntity> entityType() {
		return switch (part) {
			case RESIDUE_FILTER_PRESS -> WildcatterBlocks.RESIDUE_FILTER_PRESS_ENTITY;
			case HYDRATE_FILTER -> WildcatterBlocks.HYDRATE_FILTER_ENTITY;
			case CALCINER -> WildcatterBlocks.CALCINER_ENTITY;
			case CAUSTICIZER -> WildcatterBlocks.CAUSTICIZER_ENTITY;
			case LIME_KILN -> WildcatterBlocks.LIME_KILN_ENTITY;
			case ALF3_REACTOR -> WildcatterBlocks.ALF3_REACTOR_ENTITY;
			case BAGGING_STATION -> WildcatterBlocks.BAGGING_STATION_ENTITY;
			case CHLORINE_FILLER -> WildcatterBlocks.CHLORINE_FILLER_ENTITY;
			default -> WildcatterBlocks.BAYER_STAGE_ENTITY;
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
		if (entity instanceof HydrateFilterBlockEntity filter && player.isShiftKeyDown()) {
			player.sendOverlayMessage(Component.literal(filter.stepShare()));
			return InteractionResult.SUCCESS;
		}
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
