package dev.wildcatter.block;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A gas turbine part with no logic of its own (the inlet house, the exhaust stack): a click reads the turbine it touches. */
public class TurbinePassiveBlockEntity extends BlockEntity implements Inspectable, BigPartEntity {
	private boolean grown;

	public TurbinePassiveBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TURBINE_PASSIVE_ENTITY, pos, state);
	}

	@Override
	public Component inspect() {
		GasTurbineBlockEntity gt = level == null ? null : Touching.find(level, worldPosition, GasTurbineBlockEntity.class, g -> true);
		String name = getBlockState().is(WildcatterBlocks.GT_INLET_HOUSE) ? "Inlet house (filters and silencer)" : "Exhaust stack (simple cycle)";
		return Component.literal(name + ": " + (gt == null ? "NOT AGAINST A GAS TURBINE" : gt.inspect().getString()));
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", grown);
	}
}
