package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.EnergyBuffer;
import dev.wildcatter.energy.EnergyNet;
import dev.wildcatter.energy.EnergyNode;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One piece of cable. Which voltage it carries (and how much) comes from the block it belongs to. */
public class CableBlockEntity extends BlockEntity implements EnergyNode, Inspectable {
	private final Voltage voltage;
	private final EnergyBuffer energy;
	private int lastSent;

	public CableBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CABLE_ENTITY, pos, state);
		this.voltage = ((CableBlock) state.getBlock()).voltage();
		this.energy = voltage == Voltage.LOW
				? new EnergyBuffer(Tuning.CABLE_BUFFER, Tuning.CABLE_THROUGHPUT, Tuning.CABLE_THROUGHPUT)
				: new EnergyBuffer(Tuning.MEDIUM_CABLE_BUFFER, Tuning.MEDIUM_CABLE_THROUGHPUT, Tuning.MEDIUM_CABLE_THROUGHPUT);
	}

	@Override
	public EnergyBuffer energy() {
		return energy;
	}

	@Override
	public Voltage voltageAt(Direction side) {
		return voltage;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CableBlockEntity cable) {
		cable.lastSent = cable.energy.stored() > 0 ? EnergyNet.push(level, pos, cable.energy, cable.voltage) : 0;
		if (cable.lastSent > 0) {
			cable.setChanged();
		}
	}

	@Override
	public Component inspect() {
		return Component.literal((voltage == Voltage.LOW ? "Copper cable" : "Medium-voltage line") + " (" + voltage.label() + "): "
				+ energy.stored() + "/" + energy.capacity() + " RF, passing on " + lastSent + " RF/t");
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
