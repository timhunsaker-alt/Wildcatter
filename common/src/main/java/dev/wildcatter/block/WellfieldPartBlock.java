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
 * A surface part of an ISR wellfield at true size (docs/multiblocks/isr-wellfield.md): the header house with the oxygen generator
 * against it, and the IX columns, on steel pipe.
 */
public class WellfieldPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		HEADER_HOUSE("header_house", Footprint.box(4, 4, 10, 0, 0)),
		OXYGEN_GENERATOR("oxygen_generator", Footprint.box(3, 3, 6, 0, 0)),
		IX_COLUMNS("ix_columns", Footprint.box(8, 6, 4, 0, 0));

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

	public WellfieldPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case HEADER_HOUSE -> new HeaderHouseBlockEntity(pos, state);
			case OXYGEN_GENERATOR -> new OxygenGeneratorBlockEntity(pos, state);
			case IX_COLUMNS -> new IxColumnsBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return part == Part.OXYGEN_GENERATOR ? createTickerHelper(type, WildcatterBlocks.OXYGEN_GENERATOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e)) : null;
		}
		return switch (part) {
			case HEADER_HOUSE -> createTickerHelper(type, WildcatterBlocks.HEADER_HOUSE_ENTITY, HeaderHouseBlockEntity::tick);
			case OXYGEN_GENERATOR -> createTickerHelper(type, WildcatterBlocks.OXYGEN_GENERATOR_ENTITY, UnitMotorBlockEntity::tick);
			case IX_COLUMNS -> createTickerHelper(type, WildcatterBlocks.IX_COLUMNS_ENTITY, IxColumnsBlockEntity::tick);
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
