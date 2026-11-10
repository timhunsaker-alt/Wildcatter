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
 * One part of the drilling mud plant or the rig's mixing hopper at true size (docs/multiblocks/drilling-mud.md): the barite mill and
 * the bentonite dryer, each filling bulk bags through a bagging station set against it, and the jet mixing hopper set against a land
 * rig's mud tanks. Each is placed on its own and works by touching; solids go in by a hopper on top.
 */
public class MudPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		BARITE_MILL("barite_mill", 3, 6, 4),
		BENTONITE_DRYER("bentonite_dryer", 3, 4, 8),
		MUD_HOPPER("mud_hopper", 2, 3, 3),
		BOP_STACK("bop_stack", 2, 4, 2),
		BOP_ACCUMULATOR("bop_accumulator", 2, 2, 4);

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

	public MudPartBlock(Properties properties, Part part) {
		super(properties, Footprint.box(part.w, part.h, part.l, 0, 0), part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case BARITE_MILL, BENTONITE_DRYER -> new MudMillBlockEntity(pos, state);
			case MUD_HOPPER -> new MudHopperBlockEntity(pos, state);
			case BOP_STACK -> new BopStackBlockEntity(pos, state);
			case BOP_ACCUMULATOR -> new BopAccumulatorBlockEntity(pos, state);
		};
	}

	private BlockEntityType<? extends UnitMotorBlockEntity> entityType() {
		return part == Part.MUD_HOPPER ? WildcatterBlocks.MUD_HOPPER_ENTITY : part == Part.BOP_ACCUMULATOR ? WildcatterBlocks.BOP_ACCUMULATOR_ENTITY : WildcatterBlocks.MUD_MILL_ENTITY;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (part == Part.BOP_STACK) {
			return null;   // the rig works it
		}
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
		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** The BOP stack snaps into the BOP space under a land rig's rotary table, on the wellhead cell, facing the rig's way. */
	@Override
	public net.minecraft.world.item.context.BlockPlaceContext snap(net.minecraft.world.item.context.BlockPlaceContext context) {
		BlockPos slot = part == Part.BOP_STACK ? bopSlot(context.getLevel(), context.getClickedPos()) : null;
		return slot == null || slot.equals(context.getClickedPos()) ? context
				: net.minecraft.world.item.context.BlockPlaceContext.at(context, slot, net.minecraft.core.Direction.UP);
	}

	@Override
	public BlockState placementState(net.minecraft.world.item.context.BlockPlaceContext context) {
		if (part == Part.BOP_STACK) {
			BlockPos origin = LandRig.substructureNear(context.getLevel(), context.getClickedPos());
			if (origin != null) {
				BlockState rig = context.getLevel().getBlockState(origin);
				return defaultBlockState().setValue(FACING, rig.getValue(FACING));
			}
		}
		return super.placementState(context);
	}

	/** Where a BOP stack clicked at {@code pos} goes: the cell over the hole of the substructure near it, or null. */
	static BlockPos bopSlot(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
		BlockPos origin = LandRig.substructureNear(level, pos);
		if (origin == null) {
			return null;
		}
		return LandRig.hole(origin, level.getBlockState(origin).getValue(FACING)).above();
	}

	/** A hopper on top of the part works its main cell's bin. */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
