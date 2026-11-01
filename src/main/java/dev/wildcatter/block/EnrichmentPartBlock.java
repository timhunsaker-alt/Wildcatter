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
 * One part of the centrifuge cascade at true size (docs/multiblocks/enrichment.md): the centrifuges, their frequency converter, the
 * feed station and the product and tails take-offs, all on one header of steel pipe. The centrifuge is the one part a player sets
 * many of: a cascade's output is its count of machines (ROADMAP, Phase 7).
 */
public class EnrichmentPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		CENTRIFUGE("centrifuge", Footprint.box(1, 6, 1, 0, 0)),
		FREQUENCY_CONVERTER("frequency_converter", Footprint.box(3, 3, 6, 0, 0)),
		FEED_STATION("feed_station", Footprint.box(3, 3, 5, 0, 0)),
		PRODUCT_TAKEOFF("product_takeoff", Footprint.box(3, 3, 4, 0, 0)),
		TAILS_TAKEOFF("tails_takeoff", Footprint.box(3, 3, 5, 0, 0));

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

	public EnrichmentPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case CENTRIFUGE -> new CentrifugeBlockEntity(pos, state);
			case FREQUENCY_CONVERTER -> new FrequencyConverterBlockEntity(pos, state);
			case FEED_STATION -> new FeedStationBlockEntity(pos, state);
			case PRODUCT_TAKEOFF, TAILS_TAKEOFF -> new TakeoffBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case FEED_STATION -> createTickerHelper(type, WildcatterBlocks.FEED_STATION_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case PRODUCT_TAKEOFF, TAILS_TAKEOFF -> createTickerHelper(type, WildcatterBlocks.TAKEOFF_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				default -> null;
			};
		}
		return switch (part) {
			case CENTRIFUGE -> null;
			case FREQUENCY_CONVERTER -> createTickerHelper(type, WildcatterBlocks.FREQUENCY_CONVERTER_ENTITY, FrequencyConverterBlockEntity::tick);
			case FEED_STATION -> createTickerHelper(type, WildcatterBlocks.FEED_STATION_ENTITY, UnitMotorBlockEntity::tick);
			case PRODUCT_TAKEOFF, TAILS_TAKEOFF -> createTickerHelper(type, WildcatterBlocks.TAKEOFF_ENTITY, UnitMotorBlockEntity::tick);
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
		if (entity instanceof TakeoffBlockEntity t && t.fullCount() > 0) {
			t.giveAll(player);
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof FeedStationBlockEntity f && f.emptied() > 0) {
			f.giveAll(player);
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
