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
 * One part of Phase 18 at true size (docs/multiblocks/fertilizer.md): the phosphoric acid plant (mill, reactor with its pan filter,
 * evaporator, granulation plant) and the potash plant (dissolver, crystallizer, dryer). Joined by steel pipe, or by touching where the spec says so.
 */
public class PhosphatePartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		PHOSPHATE_MILL("phosphate_mill", Footprint.box(3, 4, 6, 0, 0)),
		PHOSPHORIC_REACTOR("phosphoric_reactor", Footprint.box(8, 6, 8, 0, 0)),
		TILTING_PAN_FILTER("tilting_pan_filter", Footprint.box(6, 4, 10, 0, 0)),
		ACID_EVAPORATOR("acid_evaporator", Footprint.box(3, 10, 3, 0, 0)),
		GRANULATION_PLANT("granulation_plant", Footprint.box(4, 5, 12, 0, 0)),
		POTASH_DISSOLVER("potash_dissolver", Footprint.box(4, 5, 4, 0, 0)),
		VACUUM_CRYSTALLIZER("vacuum_crystallizer", Footprint.box(3, 10, 3, 0, 0)),
		POTASH_DRYER("potash_dryer", Footprint.box(8, 4, 3, 0, 0));

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

	public PhosphatePartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case PHOSPHATE_MILL -> new PhosphateMillBlockEntity(pos, state);
			case PHOSPHORIC_REACTOR -> new PhosphoricReactorBlockEntity(pos, state);
			case TILTING_PAN_FILTER -> new PanFilterBlockEntity(pos, state);
			case ACID_EVAPORATOR -> new AcidEvaporatorBlockEntity(pos, state);
			case GRANULATION_PLANT -> new GranulationPlantBlockEntity(pos, state);
			case POTASH_DISSOLVER -> new PotashDissolverBlockEntity(pos, state);
			case VACUUM_CRYSTALLIZER -> new VacuumCrystallizerBlockEntity(pos, state);
			case POTASH_DRYER -> new PotashDryerBlockEntity(pos, state);
		};
	}

	private BlockEntityType<? extends UnitMotorBlockEntity> type() {
		return switch (part) {
			case PHOSPHATE_MILL -> WildcatterBlocks.PHOSPHATE_MILL_ENTITY;
			case PHOSPHORIC_REACTOR -> WildcatterBlocks.PHOSPHORIC_REACTOR_ENTITY;
			case TILTING_PAN_FILTER -> WildcatterBlocks.TILTING_PAN_FILTER_ENTITY;
			case ACID_EVAPORATOR -> WildcatterBlocks.ACID_EVAPORATOR_ENTITY;
			case GRANULATION_PLANT -> WildcatterBlocks.GRANULATION_PLANT_ENTITY;
			case POTASH_DISSOLVER -> WildcatterBlocks.POTASH_DISSOLVER_ENTITY;
			case VACUUM_CRYSTALLIZER -> WildcatterBlocks.VACUUM_CRYSTALLIZER_ENTITY;
			case POTASH_DRYER -> WildcatterBlocks.POTASH_DRYER_ENTITY;
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (type != type()) {
			return null;
		}
		if (level.isClientSide()) {
			return (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick((UnitMotorBlockEntity) e);
		}
		return (l, p, s, e) -> UnitMotorBlockEntity.tick(l, p, s, (UnitMotorBlockEntity) e);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		if (read != null) {
			return read;
		}
		// what a hopper on top would put in, clicked in by hand: rock, potash
		if (level.getBlockEntity(main(pos, state)) instanceof HopperFed fed && fed.takes(stack)) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			for (int slot = 0; slot < fed.slots().size(); slot++) {
				if (!fed.canPlaceItem(slot, stack)) {
					continue;
				}
				ItemStack held = fed.slots().get(slot);
				if (held.isEmpty() || (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize())) {
					if (held.isEmpty()) {
						fed.slots().set(slot, stack.copyWithCount(1));
					} else {
						held.grow(1);
					}
					stack.consume(1, player);
					fed.setChanged();
					player.sendOverlayMessage(Component.literal("Loaded: " + fed.slots().get(slot).getCount() + " in its bin"));
				}
				break;
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
		if (player.isShiftKeyDown() && entity instanceof GranulationPlantBlockEntity granulator) {
			player.sendOverlayMessage(Component.literal(granulator.switchProduct()));
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

	/** A hopper on top of the part works its main cell's bin. */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
