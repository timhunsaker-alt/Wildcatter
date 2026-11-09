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
 * One part of the ammonia plant at true size (docs/multiblocks/ammonia-plant.md): the nitrogen generator, the syngas compressor with
 * the ammonia converter against it, the nitric acid unit, the AN neutraliser and the emulsion blender, on steel pipe to each other,
 * to the existing steam reformer and to Wildcatter's tanks.
 */
public class AmmoniaPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		NITROGEN_GENERATOR("nitrogen_generator", Footprint.box(2, 3, 3, 0, 0)),
		SYNGAS_COMPRESSOR("syngas_compressor", Footprint.box(3, 3, 5, 0, 0)),
		AMMONIA_CONVERTER("ammonia_converter", Footprint.box(3, 10, 3, 0, 0)),
		NITRIC_ACID_UNIT("nitric_acid_unit", Footprint.box(4, 12, 4, 0, 0)),
		AN_NEUTRALISER("an_neutraliser", Footprint.box(3, 5, 3, 0, 0)),
		EMULSION_BLENDER("emulsion_blender", Footprint.box(3, 3, 4, 0, 0));

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

	public AmmoniaPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case NITROGEN_GENERATOR -> new NitrogenGeneratorBlockEntity(pos, state);
			case SYNGAS_COMPRESSOR -> new SyngasCompressorBlockEntity(pos, state);
			case AMMONIA_CONVERTER -> new AmmoniaConverterBlockEntity(pos, state);
			case NITRIC_ACID_UNIT -> new NitricAcidUnitBlockEntity(pos, state);
			case AN_NEUTRALISER -> new AnNeutraliserBlockEntity(pos, state);
			case EMULSION_BLENDER -> new EmulsionBlenderBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case NITROGEN_GENERATOR -> createTickerHelper(type, WildcatterBlocks.NITROGEN_GENERATOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case SYNGAS_COMPRESSOR -> createTickerHelper(type, WildcatterBlocks.SYNGAS_COMPRESSOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case AMMONIA_CONVERTER -> createTickerHelper(type, WildcatterBlocks.AMMONIA_CONVERTER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case NITRIC_ACID_UNIT -> createTickerHelper(type, WildcatterBlocks.NITRIC_ACID_UNIT_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case EMULSION_BLENDER -> createTickerHelper(type, WildcatterBlocks.EMULSION_BLENDER_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case AN_NEUTRALISER -> null;
			};
		}
		return switch (part) {
			case NITROGEN_GENERATOR -> createTickerHelper(type, WildcatterBlocks.NITROGEN_GENERATOR_ENTITY, UnitMotorBlockEntity::tick);
			case SYNGAS_COMPRESSOR -> createTickerHelper(type, WildcatterBlocks.SYNGAS_COMPRESSOR_ENTITY, UnitMotorBlockEntity::tick);
			case AMMONIA_CONVERTER -> createTickerHelper(type, WildcatterBlocks.AMMONIA_CONVERTER_ENTITY, UnitMotorBlockEntity::tick);
			case NITRIC_ACID_UNIT -> createTickerHelper(type, WildcatterBlocks.NITRIC_ACID_UNIT_ENTITY, UnitMotorBlockEntity::tick);
			case AN_NEUTRALISER -> createTickerHelper(type, WildcatterBlocks.AN_NEUTRALISER_ENTITY, AnNeutraliserBlockEntity::tick);
			case EMULSION_BLENDER -> createTickerHelper(type, WildcatterBlocks.EMULSION_BLENDER_ENTITY, UnitMotorBlockEntity::tick);
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		if (read != null) {
			return read;
		}
		// a gauze pack or an emulsifier pail clicked in, as a hopper on top would put it
		if (level.getBlockEntity(main(pos, state)) instanceof HopperFed fed && fed.takes(stack)) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			ItemStack held = fed.slots().get(0);
			if (held.isEmpty() || (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize())) {
				if (held.isEmpty()) {
					fed.slots().set(0, stack.copyWithCount(1));
				} else {
					held.grow(1);
				}
				stack.consume(1, player);
				fed.setChanged();
				player.sendOverlayMessage(Component.literal("Loaded: " + fed.slots().get(0).getCount() + " in its bin"));
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
		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = entity instanceof UnitMotorBlockEntity motor && Shock.touch((ServerLevel) level, pos, player, motor.motor().voltage(), motor.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** A hopper on top of the part works its main cell's bin: gauze packs on the nitric acid unit, emulsifier pails on the blender. */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
