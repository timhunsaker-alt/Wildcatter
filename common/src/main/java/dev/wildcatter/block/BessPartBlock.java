package dev.wildcatter.block;

import dev.wildcatter.WildcatterComponents;
import dev.wildcatter.energy.BessCells;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One part of a battery energy storage system at true size (docs/multiblocks/grid-batteries.md, 22b): the 20 ft battery container, the
 * PCS skid, the plant controller's control house and the commercial cabinet. The MV skid is a transformer ({@link PadmountBlock#mvSkid}).
 * Right-click reads it; sneak and right-click steps a PCS's, cabinet's or plant's mode (or resets a trip). A container or cabinet keeps its
 * state of health and cycles in its item when broken; its charge is lost (they ship discharged).
 */
public class BessPartBlock extends BigPartBlock {
	public enum Part {
		BESS_CONTAINER("bess_container", Footprint.box(3, 3, 6, 0, 0)),
		BESS_PCS("bess_pcs", Footprint.box(3, 3, 3, 0, 0)),
		BESS_PLANT_CONTROLLER("bess_plant_controller", Footprint.box(3, 4, 3, 0, 0)),
		BESS_CABINET("bess_cabinet", Footprint.box(2, 3, 2, 0, 0));

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

	public BessPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case BESS_CONTAINER -> new BessContainerBlockEntity(pos, state);
			case BESS_PCS -> new BessPcsBlockEntity(pos, state);
			case BESS_PLANT_CONTROLLER -> new BessPlantControllerBlockEntity(pos, state);
			case BESS_CABINET -> new BessCabinetBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return (l, p, s, e) -> {
			if (e instanceof BessInverterBlockEntity inv) {
				BessInverterBlockEntity.tick(l, p, s, inv);
			} else if (e instanceof BessContainerBlockEntity box) {
				BessContainerBlockEntity.tick(l, p, s, box);
			} else if (e instanceof BessPlantControllerBlockEntity plant) {
				BessPlantControllerBlockEntity.tick(l, p, s, plant);
			}
		};
	}

	/**
	 * 22c: under {@code bessThermalRunaway} an explosion's blast reaching a battery container sets it off (a vessel burst, TNT); its blocks are
	 * spared, for it burns out to scrap. With the rule off it is blown up as any block.
	 */
	@Override
	protected void onExplosionHit(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.world.level.Explosion explosion,
			java.util.function.BiConsumer<ItemStack, BlockPos> drops) {
		if (part == Part.BESS_CONTAINER && dev.wildcatter.Rules.on(dev.wildcatter.Rules.Rule.BESS_THERMAL_RUNAWAY)
				&& explosion.getBlockInteraction() != net.minecraft.world.level.Explosion.BlockInteraction.TRIGGER_BLOCK
				&& explosion.getBlockInteraction() != net.minecraft.world.level.Explosion.BlockInteraction.KEEP
				&& level.getBlockEntity(main(pos, state)) instanceof BessContainerBlockEntity box) {
			box.blasted(level);
			return;
		}
		super.onExplosionHit(state, level, pos, explosion, drops);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.getBlockEntity(main(pos, state)) instanceof BessModules box) {
			box.setModules(stack.getOrDefault(WildcatterComponents.BESS_MODULES, 0));
			box.cells().set(0, stack.getOrDefault(WildcatterComponents.BESS_SOH, 1.0), stack.getOrDefault(WildcatterComponents.BESS_CYCLES, 0.0),
					dev.wildcatter.Tuning.CELL_AMBIENT_C);
		}
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		List<ItemStack> out = new ArrayList<>(super.getDrops(state, params));
		if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BessModules box && box.modules() > 0) {
			BessCells cells = box.cells();
			for (ItemStack stack : out) {
				if (stack.is(asItem())) {
					stack.set(WildcatterComponents.BESS_MODULES, box.modules());
					stack.set(WildcatterComponents.BESS_SOH, cells.soh());
					stack.set(WildcatterComponents.BESS_CYCLES, cells.cycles());
				}
			}
		}
		return out;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		if (read != null) {
			return read;
		}
		// 22b: LFP modules load an emptier container or cabinet; a full, worn one takes a whole new set (augmentation) and gives the worn ones back
		if (stack.is(dev.wildcatter.WildcatterItems.LFP_MODULE) && level.getBlockEntity(main(pos, state)) instanceof BessModules box
				&& !(box instanceof BessContainerBlockEntity c && c.burning())) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			net.minecraft.network.chat.Component said;
			if (box.modules() < box.maxModules()) {
				int took = box.load(stack.getCount());
				stack.consume(took, player);
				said = net.minecraft.network.chat.Component.literal(String.format("Loaded %d modules: %d of %d", took, box.modules(), box.maxModules()));
			} else if (box.cells().soh() < 0.99999) {
				int need = box.maxModules();
				if (stack.getCount() < need && !player.getAbilities().instabuild) {
					said = net.minecraft.network.chat.Component.literal("Renewing it takes " + need + " LFP modules in one hand");
				} else {
					stack.consume(need, player);
					box.cells().renew();
					player.getInventory().placeItemBackInInventory(new ItemStack(dev.wildcatter.WildcatterItems.SPENT_LFP_MODULE, need), net.minecraft.util.Prediction.SERVER_ONLY);
					said = net.minecraft.network.chat.Component.literal("Renewed: " + need + " new modules, SOH 100 %; the worn ones are in your inventory");
				}
			} else {
				said = net.minecraft.network.chat.Component.literal("Full, and its modules are new");
			}
			if (box instanceof BlockEntity be) {
				be.setChanged();
			}
			player.sendOverlayMessage(said);
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
		if (player.isShiftKeyDown()) {
			if (entity instanceof BessInverterBlockEntity inv) {
				player.sendOverlayMessage(inv.sneakClick());
				return InteractionResult.SUCCESS;
			}
			if (entity instanceof BessPlantControllerBlockEntity plant) {
				player.sendOverlayMessage(plant.sneakClick());
				return InteractionResult.SUCCESS;
			}
		}
		if (entity instanceof dev.wildcatter.energy.Inspectable machine) {
			player.sendOverlayMessage(machine.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
