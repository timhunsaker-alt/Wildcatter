package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One part of the ferroalloy plant at true size (docs/multiblocks/ferroalloys.md): the 10 MVA furnace transformer, the submerged arc
 * furnace against it, the 3 Mvar capacitor bank, the moly roaster and the aluminothermic vessel. Sneak and right-click: the transformer
 * steps its tap, the furnace changes its recipe, the bank opens or closes its breaker, the vessel fires its batch.
 */
public class FerroalloyPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		FURNACE_TRANSFORMER("furnace_transformer", 4, 5, 5),
		SUBMERGED_ARC_FURNACE("submerged_arc_furnace", 7, 8, 7),
		CAPACITOR_BANK("capacitor_bank", 2, 3, 4),
		MOLY_ROASTER("moly_roaster", 3, 9, 3),
		ALUMINOTHERMIC_VESSEL("aluminothermic_vessel", 3, 3, 3);

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

	public FerroalloyPartBlock(Properties properties, Part part) {
		super(properties, Footprint.box(part.w, part.h, part.l, 0, 0), part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case FURNACE_TRANSFORMER -> new FurnaceTransformerBlockEntity(pos, state);
			case SUBMERGED_ARC_FURNACE -> new SubmergedArcFurnaceBlockEntity(pos, state);
			case CAPACITOR_BANK -> new CapacitorBankBlockEntity(pos, state);
			case MOLY_ROASTER -> new MolyRoasterBlockEntity(pos, state);
			case ALUMINOTHERMIC_VESSEL -> new AluminothermicVesselBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case MOLY_ROASTER -> createTickerHelper(type, WildcatterBlocks.MOLY_ROASTER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case ALUMINOTHERMIC_VESSEL -> createTickerHelper(type, WildcatterBlocks.ALUMINOTHERMIC_VESSEL_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				default -> null;
			};
		}
		return switch (part) {
			case FURNACE_TRANSFORMER, CAPACITOR_BANK -> null;
			case SUBMERGED_ARC_FURNACE -> createTickerHelper(type, WildcatterBlocks.SUBMERGED_ARC_FURNACE_ENTITY, SubmergedArcFurnaceBlockEntity::tick);
			case MOLY_ROASTER -> createTickerHelper(type, WildcatterBlocks.MOLY_ROASTER_ENTITY, UnitMotorBlockEntity::tick);
			case ALUMINOTHERMIC_VESSEL -> createTickerHelper(type, WildcatterBlocks.ALUMINOTHERMIC_VESSEL_ENTITY, UnitMotorBlockEntity::tick);
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		if (read != null) {
			return read;
		}
		// charge clicked into its bin, as a hopper on top would put it
		if (level.getBlockEntity(main(pos, state)) instanceof HopperFed fed) {
			int[] slots = fed.getSlotsForFace(Direction.UP);
			for (int slot : slots) {
				if (!fed.canPlaceItemThroughFace(slot, stack, Direction.UP)) {
					continue;
				}
				if (level.isClientSide()) {
					return InteractionResult.SUCCESS;
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
					player.sendOverlayMessage(Component.literal("Loaded: " + fed.slots().get(slot).getCount() + " " + fed.slots().get(slot).getHoverName().getString() + " in its bin"));
				}
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity entity = level.getBlockEntity(main(pos, state));
		if (player.isShiftKeyDown()) {
			if (entity instanceof FurnaceTransformerBlockEntity transformer) {
				player.sendOverlayMessage(Component.literal(transformer.nextTap()));
				return InteractionResult.SUCCESS;
			}
			if (entity instanceof SubmergedArcFurnaceBlockEntity furnace) {
				player.sendOverlayMessage(Component.literal(furnace.nextRecipe()));
				return InteractionResult.SUCCESS;
			}
			if (entity instanceof CapacitorBankBlockEntity bank) {
				player.sendOverlayMessage(Component.literal(bank.toggle()));
				return InteractionResult.SUCCESS;
			}
			if (entity instanceof AluminothermicVesselBlockEntity vessel) {
				player.sendOverlayMessage(Component.literal(vessel.fire(level)));
				return InteractionResult.SUCCESS;
			}
		}
		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** A hopper on top of the part works its main cell's bins: the furnace's charge and paste, the vessel's batch. */
	@Override
	public WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
