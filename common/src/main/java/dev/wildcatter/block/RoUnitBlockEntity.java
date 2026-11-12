package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The reverse osmosis unit (docs/multiblocks/isr-operations.md item 4, 16d2): cartridge filters, high-pressure pumps and spiral-wound
 * membranes, 3 x 3 x 8, 160 kW on the industrial supply (box west). Against a header house in RO, it treats up to 30 L/s of the producers'
 * water at 75 % recovery: the permeate goes back to the injectors, the brine out with the bleed.
 */
public class RoUnitBlockEntity extends UnitMotorBlockEntity {
	private double treated;   // m3
	private String unit = "idle: set it against a header house in RO";

	public RoUnitBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RO_UNIT_ENTITY, pos, state);
	}

	/** The header house asks it to treat up to {@code litres}; returns the litres it takes this interval. */
	public double treat(Level level, double litres) {
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the high-pressure pumps: " + describe();
			return 0;
		}
		double l = Math.min(litres, Tuning.RO_LPS * seconds() * strength);
		treated += l / 1000;
		unit = String.format("treating %.1f L/s: %.1f L/s of permeate to the injectors, %.1f L/s of brine to the bleed", l / seconds(), l * Tuning.RO_RECOVERY / seconds(),
				l * (1 - Tuning.RO_RECOVERY) / seconds());
		return l;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("RO unit (%.0f kW, %s): %s; %,.0f m3 treated; ", Tuning.RO_WATTS / 1000, Voltage.INDUSTRIAL.label(), unit, treated) + describe());
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("treated", treated);
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		treated = input.getDoubleOr("treated", 0);
	}
}
