package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * 19a's parts at true size (docs/multiblocks/reactor-extensions.md): the containment's equipment hatch and personnel airlock, the
 * passive hydrogen recombiners, the containment monitor (the building's controller) and the pressurizer relief tank.
 */
public class ContainmentPartBlock extends BigPartBlock {
	public enum Part {
		EQUIPMENT_HATCH("equipment_hatch", Footprint.box(4, 4, 1, 0, 0)),
		PERSONNEL_AIRLOCK("personnel_airlock", Footprint.box(2, 3, 3, 0, 0)),
		HYDROGEN_RECOMBINER("hydrogen_recombiner", Footprint.box(1, 2, 1, 0, 0)),
		CONTAINMENT_MONITOR("containment_monitor", Footprint.box(1, 2, 1, 0, 0)),
		PRESSURIZER_RELIEF_TANK("pressurizer_relief_tank", Footprint.box(2, 2, 6, 0, 0));

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

	public ContainmentPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case CONTAINMENT_MONITOR -> new ContainmentMonitorBlockEntity(pos, state);
			case PRESSURIZER_RELIEF_TANK -> new ReliefTankBlockEntity(pos, state);
			default -> new ContainmentDoorBlockEntity(pos, state);
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return part == Part.CONTAINMENT_MONITOR && !level.isClientSide()
				? createTickerHelper(type, WildcatterBlocks.CONTAINMENT_MONITOR_ENTITY, ContainmentMonitorBlockEntity::tick) : null;
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
		String said = switch (entity) {
			case ContainmentDoorBlockEntity door when part == Part.PERSONNEL_AIRLOCK && !player.isShiftKeyDown() -> door.passThrough(level, player);
			case ContainmentDoorBlockEntity door when part == Part.EQUIPMENT_HATCH && player.isShiftKeyDown() -> door.toggle();
			case ContainmentMonitorBlockEntity monitor when player.isShiftKeyDown() -> monitor.refill(level);
			case ReliefTankBlockEntity tank when player.isShiftKeyDown() -> tank.sneakClick();
			case dev.wildcatter.energy.Inspectable machine -> machine.inspect().getString();
			case null, default -> null;
		};
		if (said != null) {
			player.sendOverlayMessage(Component.literal(said));
		}
		return InteractionResult.SUCCESS;
	}
}
