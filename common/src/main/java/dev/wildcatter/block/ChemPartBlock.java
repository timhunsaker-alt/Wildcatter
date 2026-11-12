package dev.wildcatter.block;

import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import java.util.function.BiFunction;
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
 * One part of Phase 20 at true size (docs/multiblocks/chlor-alkali-plant.md, titanium-pigment.md, lithium.md): the chlor-alkali plant,
 * the titanium pigment plant and the lithium hydroxide plant. Joined by steel pipe, or by touching where the spec says so.
 */
public class ChemPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		// 20a, the chlor-alkali plant
		BRINE_SATURATOR("brine_saturator", Footprint.box(4, 5, 4, 0, 0), BrineSaturatorBlockEntity::new),
		BRINE_TREATMENT("brine_treatment", Footprint.box(4, 6, 8, 0, 0), BrineTreatmentBlockEntity::new),
		MEMBRANE_CELL_ROOM("membrane_cell_room", Footprint.box(6, 4, 16, 0, 0), MembraneCellRoomBlockEntity::new),
		CHLORINE_TRAIN("chlorine_train", Footprint.box(4, 6, 10, 0, 0), ChlorineTrainBlockEntity::new),
		CAUSTIC_EVAPORATOR("caustic_evaporator", Footprint.box(3, 10, 3, 0, 0), CausticEvaporatorBlockEntity::new),
		HCL_SYNTHESIS_UNIT("hcl_synthesis_unit", Footprint.box(2, 8, 2, 0, 0), HclSynthesisBlockEntity::new);

		private final String id;
		private final Footprint footprint;
		private final BiFunction<BlockPos, BlockState, BlockEntity> make;

		Part(String id, Footprint footprint, BiFunction<BlockPos, BlockState, BlockEntity> make) {
			this.id = id;
			this.footprint = footprint;
			this.make = make;
		}

		public String id() {
			return id;
		}
	}

	private final Part part;

	public ChemPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return part.make.apply(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return (l, p, s, e) -> {
				if (e instanceof UnitMotorBlockEntity motor) {
					dev.wildcatter.kinetic.RunningSound.clientTick(motor);
				}
			};
		}
		return (l, p, s, e) -> {
			if (e instanceof UnitMotorBlockEntity motor) {
				UnitMotorBlockEntity.tick(l, p, s, motor);
			} else if (e instanceof MembraneCellRoomBlockEntity room) {
				MembraneCellRoomBlockEntity.tick(l, p, s, room);
			} else if (e instanceof ProcessPartBlockEntity process) {
				ProcessPartBlockEntity.tick(l, p, s, process);
			}
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		if (read != null) {
			return read;
		}
		// what a hopper on top would put in, clicked in by hand
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
		if (player.isShiftKeyDown() && entity instanceof SneakAction action) {
			player.sendOverlayMessage(Component.literal(action.sneakClick()));
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

	/** A part a sneak-click works (the cell room's membranes, a crystallizer's switch); returns what to tell the player. */
	public interface SneakAction {
		String sneakClick();
	}
}
