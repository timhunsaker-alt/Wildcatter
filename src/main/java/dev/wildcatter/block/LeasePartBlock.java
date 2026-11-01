package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.safety.Shock;
import dev.wildcatter.energy.Voltage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * One part of a lease at true size (docs/multiblocks/lease.md): the separator, a stock or water tank, the flare stack or the
 * injection pump. Each stands on its own and is joined to the others by steel pipe on its nozzles.
 */
public class LeasePartBlock extends BigPartBlock {
	public enum Part {
		SEPARATOR("separator", Footprint.box(2, 2, 4, 0, 0)),
		STOCK_TANK("stock_tank", Footprint.box(4, 6, 4, 0, 0)),
		WATER_TANK("water_tank", Footprint.box(4, 6, 4, 0, 0)),
		FLARE_STACK("flare_stack", Footprint.box(1, 10, 1, 0, 0)),
		INJECTION_PUMP("injection_pump", Footprint.box(2, 1, 6, 0, 0));

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

	public LeasePartBlock(Properties properties, Part part) {
		super(properties, part.footprint, part.id);
		this.part = part;
	}

	public Part part() {
		return part;
	}

	// ---- the part's own frame: drawn facing north, x east, z south from its main cell ----------------------------------------
	/** Where {@code cell} sits in the part's own frame, as {x, y, z}. */
	static int[] local(BlockPos main, BlockState state, BlockPos cell) {
		int dx = cell.getX() - main.getX(), dz = cell.getZ() - main.getZ();
		for (int i = 0; i < BigParts.turns(state); i++) {   // undo the part's quarter turns
			int t = dx;
			dx = dz;
			dz = -t;
		}
		return new int[] {dx, cell.getY() - main.getY(), dz};
	}

	/** The cell at {@code x, y, z} in the part's own frame. */
	static BlockPos at(BlockPos main, BlockState state, int x, int y, int z) {
		int dx = x, dz = z;
		for (int i = 0; i < BigParts.turns(state); i++) {
			int t = dx;
			dx = -dz;
			dz = t;
		}
		return main.offset(dx, y, dz);
	}

	/** A side written in the part's own frame, turned the way the part faces. */
	static Direction world(BlockState state, Direction local) {
		Direction out = local;
		for (int i = 0; local.getAxis().isHorizontal() && i < BigParts.turns(state); i++) {
			out = out.getClockWise();
		}
		return out;
	}

	/** The faces of the part's cells that match {@code where} (its own frame), with the side {@code localSide} turned to the world. */
	static List<Lines.Face> faces(BlockPos main, BlockState state, Predicate<int[]> where, Direction localSide) {
		List<Lines.Face> out = new ArrayList<>();
		Direction side = world(state, localSide);
		for (BlockPos cell : BigParts.cells(main, state)) {
			if (where.test(local(main, state, cell))) {
				out.add(new Lines.Face(cell, side));
			}
		}
		return out;
	}

	/** True if {@code cell}'s {@code side} is one of these faces. */
	static boolean on(List<Lines.Face> faces, BlockPos cell, Direction side) {
		return faces.contains(new Lines.Face(cell, side));
	}

	// ---- block entities, clicks -----------------------------------------------------------------------------------------------
	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (part) {
			case SEPARATOR -> new SeparatorBlockEntity(pos, state);
			case STOCK_TANK, WATER_TANK -> new StorageTankBlockEntity(pos, state);
			case FLARE_STACK -> new FlareStackBlockEntity(pos, state);
			case INJECTION_PUMP -> new InjectionPumpBlockEntity(pos, state);
		};
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return switch (part) {
			case SEPARATOR -> level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.SEPARATOR_ENTITY, SeparatorBlockEntity::tick);
			case STOCK_TANK, WATER_TANK -> level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.STORAGE_TANK_ENTITY, StorageTankBlockEntity::tick);
			case FLARE_STACK -> createTickerHelper(type, WildcatterBlocks.FLARE_STACK_ENTITY, level.isClientSide() ? FlareStackBlockEntity::clientTick : FlareStackBlockEntity::tick);
			case INJECTION_PUMP -> createTickerHelper(type, WildcatterBlocks.INJECTION_PUMP_ENTITY,
					level.isClientSide() ? (l, p, s, e) -> dev.wildcatter.kinetic.RunningSound.clientTick(e) : InjectionPumpBlockEntity::tick);
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		BlockPos main = main(pos, state);
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main, player);
		if (read != null) {
			return read;
		}
		if (level.getBlockEntity(main) instanceof StorageTankBlockEntity tank && tank.bucket(stack, player, hand)) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof dev.wildcatter.energy.Inspectable machine) {
			boolean shocked = machine instanceof InjectionPumpBlockEntity pump && Shock.touch((ServerLevel) level, pos, player, Voltage.INDUSTRIAL, pump.isLive());
			if (!shocked) {
				player.sendOverlayMessage(machine.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
