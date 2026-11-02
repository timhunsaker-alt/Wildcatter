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
 * One part of the fuel plant at true size (docs/multiblocks/fuel.md): the line from the UF6 autoclave to the assembly fixture, each
 * against the next; and the helium recovery unit that stands at the gas plant (gas-plant.md) and fills the welder's cylinders.
 */
public class FuelPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		UF6_AUTOCLAVE("uf6_autoclave", Footprint.box(3, 3, 4, 0, 0)),
		IDR_KILN("idr_kiln", Footprint.box(3, 4, 10, 0, 0)),
		GRANULATOR("granulator", Footprint.box(3, 4, 4, 0, 0)),
		PELLET_PRESS("pellet_press", Footprint.box(3, 3, 4, 0, 0)),
		SINTERING_FURNACE("sintering_furnace", Footprint.box(3, 3, 12, 0, 0)),
		PELLET_GRINDER("pellet_grinder", Footprint.box(3, 3, 4, 0, 0)),
		ROD_LOADER("rod_loader", Footprint.box(3, 3, 10, 0, 0)),
		ROD_WELDER("rod_welder", Footprint.box(3, 3, 6, 0, 0)),
		ASSEMBLY_FIXTURE("assembly_fixture", Footprint.box(3, 6, 10, 0, 0)),
		HELIUM_RECOVERY_UNIT("helium_recovery_unit", Footprint.box(3, 6, 4, 0, 0));

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

	public FuelPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case ROD_WELDER -> new RodWelderBlockEntity(pos, state);
			case ASSEMBLY_FIXTURE -> new AssemblyFixtureBlockEntity(pos, state);
			case HELIUM_RECOVERY_UNIT -> new HeliumRecoveryBlockEntity(pos, state);
			default -> new FuelLineBlockEntity(pos, state);
		};
	}

	private BlockEntityType<? extends UnitMotorBlockEntity> type() {
		return switch (part) {
			case ROD_WELDER -> WildcatterBlocks.ROD_WELDER_ENTITY;
			case ASSEMBLY_FIXTURE -> WildcatterBlocks.ASSEMBLY_FIXTURE_ENTITY;
			case HELIUM_RECOVERY_UNIT -> WildcatterBlocks.HELIUM_RECOVERY_ENTITY;
			default -> WildcatterBlocks.FUEL_LINE_ENTITY;
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		BlockEntityType<UnitMotorBlockEntity> mine = (BlockEntityType<UnitMotorBlockEntity>) type();
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
		if (entity instanceof AssemblyFixtureBlockEntity fixture && fixture.finished() > 0) {
			fixture.giveAll(player);
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
