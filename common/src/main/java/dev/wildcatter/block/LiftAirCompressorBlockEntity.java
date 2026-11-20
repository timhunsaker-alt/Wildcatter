package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The lift air compressor (docs/multiblocks/oilfield-follow-ons.md, 23d): a rotary screw compressor, 110 kW at industrial LV, about 3 Nm3
 * of air a minute down a Frasch wellhead's inner string to froth the molten sulfur up the middle. The wellhead finds it by steel pipe.
 */
public class LiftAirCompressorBlockEntity extends UnitMotorBlockEntity {
	private String unit = "idle: no Frasch wellhead asking for air";

	public LiftAirCompressorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LIFT_AIR_COMPRESSOR_ENTITY, pos, state);
	}

	/** The wellhead wants its lift air this interval; returns how hard the compressor runs, 0 to 1. */
	public double air(Level level) {
		double strength = drive(level);
		unit = strength > 0 ? String.format("compressing %.1f Nm3/min of lift air", 3.0 * strength) : "starting: " + describe();
		return strength;
	}

	@Override
	protected void work(Level level) {
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Lift air compressor (rotary screw, %.0f kW): %s; ", Tuning.LIFT_AIR_WATTS / 1000, unit) + describe());
	}
}
