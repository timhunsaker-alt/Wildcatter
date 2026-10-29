package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.EnergyBuffer;
import dev.wildcatter.energy.EnergyNode;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.safety.Bondable;
import dev.wildcatter.safety.Grounding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The first machine: burns a little power to give light. */
public class LampBlockEntity extends BlockEntity implements EnergyNode, Inspectable, Bondable {
	private final EnergyBuffer energy = new EnergyBuffer(Tuning.LAMP_BUFFER, Tuning.LAMP_BUFFER, 0);

	public LampBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LAMP_ENTITY, pos, state);
	}

	@Override
	public EnergyBuffer energy() {
		return energy;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, LampBlockEntity lamp) {
		// It needs two cables joined to it: one to bring the power and one to take it back. One wire is just a dead end.
		boolean wired = CableBlock.cablesAround(level, pos, Voltage.LOW) >= Tuning.DEVICE_MIN_CABLES;
		boolean lit = wired && lamp.energy.consume(Tuning.LAMP_USE) == Tuning.LAMP_USE;
		if (state.getValue(LampBlock.LIT) != lit) {
			level.setBlock(pos, state.setValue(LampBlock.LIT, lit), 3);
		}
	}

	public boolean isLive() {
		return energy.stored() > 0;
	}

	@Override
	public Component inspect() {
		return Component.literal("Electric lamp (110 V): " + energy.stored() + "/" + energy.capacity() + " RF, uses " + Tuning.LAMP_USE
				+ " RF/t; needs " + Tuning.DEVICE_MIN_CABLES + " cables; " + (level != null && Grounding.isGrounded(level, worldPosition) ? "grounded" : "UNGROUNDED, touching it can shock you"));
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
