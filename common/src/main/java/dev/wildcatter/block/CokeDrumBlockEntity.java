package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A coke drum (docs/multiblocks/delayed-coker.md), 3 m across and 15 m tall, with its bottom unheading hydraulics, 15 kW at 480 V. A
 * coker has two on its coke pit: the heater fills one while the pit cuts the other out.
 */
public class CokeDrumBlockEntity extends UnitMotorBlockEntity {
	private double coke;
	private boolean filling;

	public CokeDrumBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COKE_DRUM_ENTITY, pos, state);
	}

	public double coke() {
		return coke;
	}

	public double room() {
		return Math.max(0, Tuning.COKE_DRUM_MAX_KG - coke);
	}

	public void addCoke(double kg) {
		coke += kg;
		setChanged();
	}

	public double takeCoke(double kg) {
		double got = Math.min(kg, coke);
		coke -= got;
		setChanged();
		return got;
	}

	public boolean filling() {
		return filling;
	}

	/** The heater says which drum it is filling. */
	void setFilling(boolean value) {
		if (filling != value) {
			filling = value;
			setChanged();
		}
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Coke drum: %s; %,.0f kg of coke in it; ", filling ? "FILLING" : coke > 0.5 ? "steamed and quenched, for cutting" : "empty, ready to fill", coke)
				+ describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("coke", coke);
		output.putBoolean("filling", filling);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		coke = input.getDoubleOr("coke", 0);
		filling = input.getBooleanOr("filling", false);
	}
}
