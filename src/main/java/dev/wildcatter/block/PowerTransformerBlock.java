package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The substation's power transformer and its controller (docs/multiblocks/substation.md): 138 kV delta to 12.47 kV grounded wye
 * (IEC 132 / 11 kV), 20 MVA, 8 %Z. Its front faces the 138 kV side. It is energized only while the whole lineup stands.
 */
public class PowerTransformerBlock extends TransformerBlock {
	public PowerTransformerBlock(Properties properties, double ratedVa, double percentZ) {
		super(properties, Voltage.DISTRIBUTION, Voltage.HIGH, ratedVa, percentZ, true, false, true);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PowerTransformerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, WildcatterBlocks.POWER_TRANSFORMER_ENTITY, PowerTransformerBlockEntity::tick);
	}
}
