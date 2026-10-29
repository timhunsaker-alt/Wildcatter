package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.EnergyBuffer;
import dev.wildcatter.energy.EnergyNode;
import dev.wildcatter.energy.Inspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The first machine: burns a little power to give light. */
public class LampBlockEntity extends BlockEntity implements EnergyNode, Inspectable {
	private final EnergyBuffer energy = new EnergyBuffer(Tuning.LAMP_BUFFER, Tuning.LAMP_BUFFER, 0);

	public LampBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LAMP_ENTITY, pos, state);
	}

	@Override
	public EnergyBuffer energy() {
		return energy;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, LampBlockEntity lamp) {
		boolean lit = lamp.energy.consume(Tuning.LAMP_USE) == Tuning.LAMP_USE;
		if (state.getValue(LampBlock.LIT) != lit) {
			level.setBlock(pos, state.setValue(LampBlock.LIT, lit), 3);
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("Electric lamp (110 V): " + energy.stored() + "/" + energy.capacity() + " RF, uses " + Tuning.LAMP_USE + " RF/t");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("energy", energy.stored());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.set(input.getIntOr("energy", 0));
	}
}
