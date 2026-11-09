package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.item.TappingCrucibleItem;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import java.util.ArrayList;
import java.util.List;
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
 * One part of the potline at true size (docs/multiblocks/potline.md): the 138 kV rectiformers, the 180 kA prebake pots, the fume
 * treatment centre and its main fans, and the cast house's ingot casting machine.
 */
public class PotlinePartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		RECTIFORMER("rectiformer", 8, 7, 6),
		POT("pot", 6, 6, 12),
		FUME_TREATMENT_CENTRE("fume_treatment_centre", 8, 12, 16),
		FTC_FAN("ftc_fan", 4, 5, 6),
		CASTING_MACHINE("casting_machine", 3, 3, 12);

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

	public PotlinePartBlock(Properties properties, Part part) {
		super(properties, Footprint.box(part.w, part.h, part.l, 0, 0), part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case RECTIFORMER -> new RectiformerBlockEntity(pos, state);
			case POT -> new PotBlockEntity(pos, state);
			case FUME_TREATMENT_CENTRE -> new FumeTreatmentCentreBlockEntity(pos, state);
			case FTC_FAN -> new UnitMotorBlockEntity(pos, state);
			case CASTING_MACHINE -> new CastingMachineBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case FTC_FAN -> createTickerHelper(type, WildcatterBlocks.UNIT_MOTOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case CASTING_MACHINE -> createTickerHelper(type, WildcatterBlocks.CASTING_MACHINE_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				default -> null;
			};
		}
		return switch (part) {
			case RECTIFORMER -> null;
			case POT -> createTickerHelper(type, WildcatterBlocks.POT_ENTITY, PotBlockEntity::tick);
			case FUME_TREATMENT_CENTRE -> createTickerHelper(type, WildcatterBlocks.FUME_TREATMENT_CENTRE_ENTITY, FumeTreatmentCentreBlockEntity::tick);
			case FTC_FAN -> createTickerHelper(type, WildcatterBlocks.UNIT_MOTOR_ENTITY, UnitMotorBlockEntity::tick);
			case CASTING_MACHINE -> createTickerHelper(type, WildcatterBlocks.CASTING_MACHINE_ENTITY, UnitMotorBlockEntity::tick);
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		if (read != null) {
			return read;
		}
		if (stack.getItem() instanceof TappingCrucibleItem) {
			if (!level.isClientSide()) {
				BlockEntity entity = level.getBlockEntity(main(pos, state));
				if (entity instanceof PotBlockEntity pot) {
					player.sendOverlayMessage(pot.tap(stack));
				} else if (entity instanceof CastingMachineBlockEntity caster) {
					player.sendOverlayMessage(caster.pour(stack));
				}
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
		if (entity instanceof RectiformerBlockEntity unit) {
			if (player.isShiftKeyDown()) {
				unit.nextSetpoint();
			} else {
				unit.reset();
			}
		}
		if (entity instanceof FillerBlockEntity filler && filler.outCount() > 0) {
			filler.giveAll(player);
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof FumeTreatmentCentreBlockEntity ftc && ftc.outCount() > 0) {
			ftc.giveAll(player);
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof PotBlockEntity pot && player.isShiftKeyDown()) {
			player.sendOverlayMessage(pot.toggleCutOut());
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof PotBlockEntity pot && pot.outCount() > 0) {
			pot.giveAll(player);
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

	/** A frozen pot leaves its spent pot lining, not a pot. */
	@Override
	protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		BlockEntity be = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
		if (be instanceof PotBlockEntity pot && pot.frozen()) {
			List<ItemStack> out = new ArrayList<>();
			for (ItemStack drop : drops) {
				out.add(drop.is(asItem()) ? new ItemStack(WildcatterItems.SPENT_POT_LINING) : drop);
			}
			return out;
		}
		return drops;
	}

	/** A hopper on top of the part, or under it, works its main cell's bins. */
	@Override
	public net.minecraft.world.WorldlyContainer getContainer(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof HopperFed fed ? fed : null;
	}
}
