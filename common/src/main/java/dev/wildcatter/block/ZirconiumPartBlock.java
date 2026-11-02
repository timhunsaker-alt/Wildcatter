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
 * One part of the zirconium plant at true size (docs/multiblocks/zirconium.md): the line from the mineral separator to the vacuum
 * anneal furnace, each against the next; the magnesium cell against the Kroll reactor with the magnesia chlorinator; the chlor-alkali
 * cell. The cells take their DC from the conversion plant's rectifier.
 */
public class ZirconiumPartBlock extends BigPartBlock implements net.minecraft.world.WorldlyContainerHolder {
	public enum Part {
		MINERAL_SEPARATOR("mineral_separator", Footprint.box(3, 4, 6, 0, 0)),
		CHLORINATOR("chlorinator", Footprint.box(3, 10, 3, 0, 0)),
		HAFNIUM_COLUMN("hafnium_column", Footprint.box(3, 12, 3, 0, 0)),
		KROLL_REACTOR("kroll_reactor", Footprint.box(3, 6, 3, 0, 0)),
		VACUUM_DISTILLATION("vacuum_distillation", Footprint.box(3, 6, 3, 0, 0)),
		VAR_FURNACE("var_furnace", Footprint.box(3, 8, 3, 0, 0)),
		EXTRUSION_PRESS("extrusion_press", Footprint.box(3, 4, 8, 0, 0)),
		PILGER_MILL("pilger_mill", Footprint.box(3, 3, 10, 0, 0)),
		VACUUM_ANNEAL_FURNACE("vacuum_anneal_furnace", Footprint.box(3, 3, 8, 0, 0)),
		MAGNESIUM_CELL("magnesium_cell", Footprint.box(3, 3, 6, 0, 0)),
		MAGNESIA_CHLORINATOR("magnesia_chlorinator", Footprint.box(3, 6, 3, 0, 0)),
		CHLOR_ALKALI_CELL("chlor_alkali_cell", Footprint.box(3, 3, 6, 0, 0));

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

	public ZirconiumPartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case MINERAL_SEPARATOR, HAFNIUM_COLUMN, VACUUM_DISTILLATION, VAR_FURNACE, EXTRUSION_PRESS, VACUUM_ANNEAL_FURNACE -> new ZrLineBlockEntity(pos, state);
			case CHLORINATOR -> new ChlorinatorBlockEntity(pos, state);
			case KROLL_REACTOR -> new KrollReactorBlockEntity(pos, state);
			case PILGER_MILL -> new PilgerMillBlockEntity(pos, state);
			case MAGNESIUM_CELL -> new MagnesiumCellBlockEntity(pos, state);
			case MAGNESIA_CHLORINATOR -> new MagnesiaChlorinatorBlockEntity(pos, state);
			case CHLOR_ALKALI_CELL -> new ChlorAlkaliCellBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return switch (part) {
				case MINERAL_SEPARATOR, HAFNIUM_COLUMN, VACUUM_DISTILLATION, VAR_FURNACE, EXTRUSION_PRESS, VACUUM_ANNEAL_FURNACE ->
						createTickerHelper(type, WildcatterBlocks.ZR_LINE_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case CHLORINATOR -> createTickerHelper(type, WildcatterBlocks.CHLORINATOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case KROLL_REACTOR -> createTickerHelper(type, WildcatterBlocks.KROLL_REACTOR_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				case PILGER_MILL -> createTickerHelper(type, WildcatterBlocks.PILGER_MILL_ENTITY, (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e));
				default -> null;
			};
		}
		return switch (part) {
			case MINERAL_SEPARATOR, HAFNIUM_COLUMN, VACUUM_DISTILLATION, VAR_FURNACE, EXTRUSION_PRESS, VACUUM_ANNEAL_FURNACE ->
					createTickerHelper(type, WildcatterBlocks.ZR_LINE_ENTITY, UnitMotorBlockEntity::tick);
			case CHLORINATOR -> createTickerHelper(type, WildcatterBlocks.CHLORINATOR_ENTITY, UnitMotorBlockEntity::tick);
			case KROLL_REACTOR -> createTickerHelper(type, WildcatterBlocks.KROLL_REACTOR_ENTITY, UnitMotorBlockEntity::tick);
			case PILGER_MILL -> createTickerHelper(type, WildcatterBlocks.PILGER_MILL_ENTITY, UnitMotorBlockEntity::tick);
			case MAGNESIUM_CELL -> createTickerHelper(type, WildcatterBlocks.MAGNESIUM_CELL_ENTITY, MagnesiumCellBlockEntity::tick);
			case MAGNESIA_CHLORINATOR -> createTickerHelper(type, WildcatterBlocks.MAGNESIA_CHLORINATOR_ENTITY, MagnesiaChlorinatorBlockEntity::tick);
			case CHLOR_ALKALI_CELL -> createTickerHelper(type, WildcatterBlocks.CHLOR_ALKALI_CELL_ENTITY, ChlorAlkaliCellBlockEntity::tick);
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
		if (entity instanceof PilgerMillBlockEntity mill && player.isShiftKeyDown()) {
			player.sendOverlayMessage(Component.literal(mill.changeDies()));
			return InteractionResult.SUCCESS;
		}
		if (entity instanceof ZrLineBlockEntity furnace && furnace.finished() > 0) {
			furnace.giveAll(player);
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
