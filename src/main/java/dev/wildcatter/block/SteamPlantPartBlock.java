package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
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
 * One part of the 1 MW steam plant at true size (docs/multiblocks/steam-plant.md): burner, boiler, superheater, turbine,
 * condenser or feed pump, each filling its real size. The turbine generator finds them and runs the plant. The burner takes
 * coal, charcoal or coal blocks (by hand on any of its cells, or a hopper into its main cell) or pipeline gas on any cell; the
 * feed pump is a 480 V three-phase motor, its terminal box on the motor's south side, that needs power before the plant can run.
 */
public class SteamPlantPartBlock extends BigPartBlock {
	public enum Part {
		// sizes along the plant's x, high, along z; every main cell is the part's north-west bottom corner
		BURNER("boiler_burner", Footprint.box(1, 3, 3, 0, 0)),
		DRUM("boiler_drum", Footprint.box(4, 4, 3, 0, 0)),
		SUPERHEATER("superheater", Footprint.box(1, 4, 3, 0, 0)),
		TURBINE("turbine_casing", Footprint.box(2, 2, 2, 0, 0)),
		CONDENSER("condenser", Footprint.box(3, 2, 2, 0, 0)),
		FEED_PUMP("feed_pump", Footprint.box(2, 1, 1, 0, 0));

		private final String id;
		private final Footprint footprint;

		Part(String id, Footprint footprint) {
			this.id = id;
			this.footprint = footprint;
		}
	}

	/** For older code that names the facing through this class. */
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING = BigPartBlock.FACING;

	private final Part part;

	public SteamPlantPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected PartLayout layout() {
		return SteamPlantLayout.layout();
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new SteamPlantPartBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? createTickerHelper(type, WildcatterBlocks.STEAM_PLANT_PART_ENTITY, SteamPlantPartBlockEntity::clientTick) : null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		BlockPos main = main(pos, state);
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main, player);
		if (read != null) {
			return read;
		}
		if (part == Part.BURNER && SteamPlantPartBlockEntity.joulesOf(stack) > 0 && level.getBlockEntity(main) instanceof SteamPlantPartBlockEntity burner) {
			if (!level.isClientSide()) {
				ItemStack rest = burner.insertFuel(stack.copy());
				if (!player.getAbilities().instabuild) {
					player.setItemInHand(hand, rest);
				}
				player.sendOverlayMessage(burner.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof SteamPlantPartBlockEntity p) {
			player.sendOverlayMessage(p.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
