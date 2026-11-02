package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.item.PowerMeterItem;
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
 * The small drilling rig at true size (docs/multiblocks/well-site.md): a skid-mounted top-head-drive rig with a 9 m mast and a
 * 40 hp (30 kW) 480 V three-phase power unit, 3 x 10 x 6. Its main cell is over the hole at the mast end; wire its terminal box,
 * low on the back of the power unit, and it sinks a cased hole straight down. If it finds a field it rigs down and leaves the
 * wellhead behind.
 */
public class DrillingRigBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = cells();

	public DrillingRigBlock(Properties properties) {
		super(properties, FOOTPRINT, "drilling_rig");
	}

	private static Footprint cells() {
		List<Footprint.Cell> cells = new ArrayList<>();
		cells.add(new Footprint.Cell(0, 0, 0));
		for (int z = 0; z < 6; z++) {   // the skid
			for (int x = -1; x <= 1; x++) {
				if (x != 0 || z != 0) {
					cells.add(new Footprint.Cell(x, 0, z));
				}
			}
		}
		for (int y = 1; y < 10; y++) {   // the mast over the hole
			cells.add(new Footprint.Cell(0, y, 0));
		}
		for (int z = 1; z <= 3; z++) {   // the pipe rack
			cells.add(new Footprint.Cell(-1, 1, z));
		}
		cells.add(new Footprint.Cell(1, 1, 1));   // the driller's console
		for (int z = 4; z <= 5; z++) {   // the power unit
			for (int x = -1; x <= 1; x++) {
				cells.add(new Footprint.Cell(x, 1, z));
			}
		}
		return Footprint.of(cells.toArray(Footprint.Cell[]::new));
	}

	/** The power unit's terminal box: the bottom cell at the back. */
	public static BlockPos terminalBox(BlockPos main, BlockState state) {
		return main.relative(state.getValue(FACING).getOpposite(), 5);
	}

	/** An older world's one-block rig grows over its hole where it has room. */
	@Override
	public boolean growsOnLoad() {
		return true;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new DrillingRigBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, WildcatterBlocks.DRILLING_RIG_ENTITY, level.isClientSide() ? (l, p, st, be) -> dev.wildcatter.kinetic.RunningSound.clientTick(be) : DrillingRigBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, pos, player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof DrillingRigBlockEntity rig) {
			if (player.isShiftKeyDown()) {
				player.sendOverlayMessage(rig.swapLeads());
				dev.wildcatter.circuit.CircuitManager.markDirty(level);
			} else if (!Shock.touch((ServerLevel) level, pos, player, Voltage.INDUSTRIAL, rig.isLive())) {
				player.sendOverlayMessage(rig.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
