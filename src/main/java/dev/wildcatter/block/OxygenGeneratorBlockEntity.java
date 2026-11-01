package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A PSA oxygen generator (50 Nm3/h, 25 kW at 480 V) against a header house (docs/multiblocks/isr-wellfield.md): its air compressor
 * and molecular-sieve beds make the oxygen the header house doses into the injection lixiviant.
 */
public class OxygenGeneratorBlockEntity extends UnitMotorBlockEntity {
	private double making;   // kg/s, the last interval

	public OxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.OXYGEN_GENERATOR_ENTITY, pos, state);
	}

	/** The header house wants {@code kg} of oxygen this interval; returns the share of it the skid makes, 0 to 1. */
	public double supply(Level level, double kg) {
		double capacity = Tuning.O2_GENERATOR_NM3H * Tuning.O2_KG_PER_NM3 / 3600 * seconds() * drive(level);
		double made = Math.min(kg, capacity);
		making = made / seconds();
		return kg > 0 ? made / kg : 0;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Oxygen generator (PSA, %.0f Nm3/h): making %.1f kg/h of oxygen; ", Tuning.O2_GENERATOR_NM3H, making * 3600) + describe());
	}
}
