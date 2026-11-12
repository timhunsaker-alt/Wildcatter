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
 * One part of the nickel smelter, refinery and alloy melt shop at true size (docs/multiblocks/nickel-smelter.md): the fluid-bed roaster,
 * the six-in-line electric furnace (the smelter's controller, on a furnace transformer), the Peirce-Smith converter, the chlorine leach
 * reactor (the refinery's controller), the purification circuit and the nickel tankhouse.
 */
public class NickelPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		FLUID_BED_ROASTER("fluid_bed_roaster", 4, 10, 4),
		NICKEL_ELECTRIC_FURNACE("nickel_electric_furnace", 6, 6, 12),
		PEIRCE_SMITH_CONVERTER("peirce_smith_converter", 5, 5, 10),
		CHLORINE_LEACH_REACTOR("chlorine_leach_reactor", 3, 8, 3),
		NI_PURIFICATION("ni_purification", 4, 4, 10),
		NICKEL_TANKHOUSE("nickel_tankhouse", 6, 4, 14),
		INDUCTION_FURNACE("induction_furnace", 3, 3, 3),
		AOD_CONVERTER("aod_converter", 3, 6, 3),
		INGOT_CASTER("ingot_caster", 2, 3, 6);

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

	public NickelPartBlock(Properties properties, Part part) {
		super(properties, Footprint.box(part.w, part.h, part.l, 0, 0), part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case FLUID_BED_ROASTER -> new FluidBedRoasterBlockEntity(pos, state);
			case NICKEL_ELECTRIC_FURNACE -> new NickelFurnaceBlockEntity(pos, state);
			case PEIRCE_SMITH_CONVERTER -> new PeirceSmithConverterBlockEntity(pos, state);
			case CHLORINE_LEACH_REACTOR -> new ChlorineLeachReactorBlockEntity(pos, state);
			case NI_PURIFICATION -> new NiPurificationBlockEntity(pos, state);
			case NICKEL_TANKHOUSE -> new NickelTankhouseBlockEntity(pos, state);
			case INDUCTION_FURNACE -> new InductionFurnaceBlockEntity(pos, state);
			case AOD_CONVERTER -> new AodConverterBlockEntity(pos, state);
			case INGOT_CASTER -> new IngotCasterBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case FLUID_BED_ROASTER -> createTickerHelper(type, WildcatterBlocks.FLUID_BED_ROASTER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case PEIRCE_SMITH_CONVERTER -> createTickerHelper(type, WildcatterBlocks.PEIRCE_SMITH_CONVERTER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case AOD_CONVERTER -> createTickerHelper(type, WildcatterBlocks.AOD_CONVERTER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				default -> null;
			};
		}
		return switch (part) {
			case FLUID_BED_ROASTER -> createTickerHelper(type, WildcatterBlocks.FLUID_BED_ROASTER_ENTITY, UnitMotorBlockEntity::tick);
			case NICKEL_ELECTRIC_FURNACE -> createTickerHelper(type, WildcatterBlocks.NICKEL_ELECTRIC_FURNACE_ENTITY, NickelFurnaceBlockEntity::tick);
			case PEIRCE_SMITH_CONVERTER -> createTickerHelper(type, WildcatterBlocks.PEIRCE_SMITH_CONVERTER_ENTITY, UnitMotorBlockEntity::tick);
			case CHLORINE_LEACH_REACTOR -> createTickerHelper(type, WildcatterBlocks.CHLORINE_LEACH_REACTOR_ENTITY, UnitMotorBlockEntity::tick);
			case NI_PURIFICATION -> createTickerHelper(type, WildcatterBlocks.NI_PURIFICATION_ENTITY, UnitMotorBlockEntity::tick);
			case NICKEL_TANKHOUSE -> createTickerHelper(type, WildcatterBlocks.NICKEL_TANKHOUSE_ENTITY, NickelTankhouseBlockEntity::tick);
			case INDUCTION_FURNACE -> createTickerHelper(type, WildcatterBlocks.INDUCTION_FURNACE_ENTITY, InductionFurnaceBlockEntity::tick);
			case AOD_CONVERTER -> createTickerHelper(type, WildcatterBlocks.AOD_CONVERTER_ENTITY, UnitMotorBlockEntity::tick);
			case INGOT_CASTER -> createTickerHelper(type, WildcatterBlocks.INGOT_CASTER_ENTITY, IngotCasterBlockEntity::tick);
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
		if (player.isShiftKeyDown() && entity instanceof Sneaked sneaked) {
			player.sendOverlayMessage(Component.literal(sneaked.sneakClick()));
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

	/** A hopper on top of (or under) the part works its main cell's bins: the roaster's ore, the furnace's paste, the leach's matte bags. */
	/** A part whose sneak and right-click does something of its own (the melt shop's grade). */
	public interface Sneaked {
		String sneakClick();
	}

	@Override
	public WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
