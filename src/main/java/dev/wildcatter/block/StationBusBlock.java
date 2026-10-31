package dev.wildcatter.block;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A substation's station bus at true size (docs/multiblocks/substation.md): aluminium tube on post insulators on a galvanized
 * structure, the 138 kV bus (2 x 6 x 9) or the 12.47 kV bus (1 x 4 x 3). It carries the phases, neutral and ground from its west
 * terminal to its east one (for a yard facing north), both on the bottom cell of the yard's centre line.
 */
public class StationBusBlock extends BigPartBlock {
	private final Voltage voltage;
	private final int length;
	private final int width;

	/** {@code length} along the line, {@code height} up, {@code width} across it. */
	public StationBusBlock(Properties properties, Voltage voltage, int length, int height, int width, String shapeName) {
		super(properties, Footprint.box(length, height, width, 0, 0), shapeName);
		this.voltage = voltage;
		this.length = length;
		this.width = width;
	}

	public Voltage voltage() {
		return voltage;
	}

	/** Its length along the line, in metres: the conductors' resistance follows it. */
	public int length() {
		return length;
	}

	@Override
	public BlockPos terminalCell(BlockPos main, BlockState state, Direction face) {
		return BigParts.lineTerminal(main, state, face, length, width);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new StationBusBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof StationBusBlockEntity bus) {
			player.sendOverlayMessage(bus.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
