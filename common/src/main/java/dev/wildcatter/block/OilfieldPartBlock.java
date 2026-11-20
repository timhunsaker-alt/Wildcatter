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
 * One part of Phase 23 at true size (docs/multiblocks/oilfield-follow-ons.md): the calcium chloride unit for oil mud, the choke manifold,
 * its remote panel and the mud-gas separator, the gas storage plant and the Frasch sulfur plant. Joined by steel pipe, or by touching
 * where the spec says so.
 */
public class OilfieldPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		// 23a: oil mud
		CALCIUM_CHLORIDE_UNIT("calcium_chloride_unit", Footprint.box(2, 4, 3, 0, 0), CalciumChlorideUnitBlockEntity::new),
		// 23b: the choke manifold, its remote panel and the mud-gas separator
		CHOKE_MANIFOLD("choke_manifold", Footprint.box(2, 2, 4, 0, 0), ChokeManifoldBlockEntity::new),
		REMOTE_CHOKE_PANEL("remote_choke_panel", Footprint.box(1, 2, 1, 0, 0), ChokePanelBlockEntity::new),
		MUD_GAS_SEPARATOR("mud_gas_separator", Footprint.box(1, 6, 1, 0, 0), MudGasSeparatorBlockEntity::new),
		// 23c: gas storage
		STORAGE_COMPRESSOR("storage_compressor", Footprint.box(3, 3, 6, 0, 0), StorageCompressorBlockEntity::new),
		GLYCOL_DEHYDRATOR("glycol_dehydrator", Footprint.box(2, 6, 3, 0, 0), GlycolDehydratorBlockEntity::new),
		// 23d: Frasch sulfur
		FRASCH_WATER_HEATER("frasch_water_heater", Footprint.box(4, 6, 8, 0, 0), FraschWaterHeaterBlockEntity::new),
		LIFT_AIR_COMPRESSOR("lift_air_compressor", Footprint.box(2, 3, 4, 0, 0), LiftAirCompressorBlockEntity::new),
		SULFUR_VAT("sulfur_vat", Footprint.box(6, 2, 6, 0, 0), SulfurVatBlockEntity::new);

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

	public OilfieldPartBlock(Properties properties, Part part) {
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
			} else if (e instanceof Ticking ticking) {
				ticking.serverTick();
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
		if (entity instanceof ChokePanelBlockEntity panel && !player.isShiftKeyDown() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
			panel.open(sp);   // the kill is worked on its screen
			return InteractionResult.SUCCESS;
		}
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

	/** A part a sneak-click works; returns what to tell the player. */
	public interface SneakAction {
		String sneakClick();
	}

	/** A part with its own server tick (the choke panel, the separator). */
	public interface Ticking {
		void serverTick();
	}
}
