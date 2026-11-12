package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.DcSupply;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 12.47 kV rectifier, {@code rectifier_mv} (docs/multiblocks/chlor-alkali-plant.md, conversion-extensions.md 16b2): a 12-pulse
 * thyristor rectifier transformer on the world's distribution voltage, 3.2 MVA, up to 16 kA and 500 V DC, PF 0.95, 97 %. As the 4.16 kV
 * rectifier otherwise: its box on the west face of its main cell, + over - on its east face at z 2, a touching cell's rating as its
 * setpoint. It drives the full-size membrane cell room (161 V, 15 kA) or four fluorine cell houses in series (432 V, 6 kA).
 */
public class RectifierMvBlockEntity extends RectifierBlockEntity {
	public RectifierMvBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.RECTIFIER_MV_ENTITY, pos, state, new DcSupply(Voltage.DISTRIBUTION, Tuning.RECTIFIER_MV_MAX_DC_VOLTS, Tuning.RECTIFIER_MV_DC_OHMS,
				Tuning.RECTIFIER_MV_RAMP_VOLTS_PER_S, Tuning.RECTIFIER_EFFICIENCY), Voltage.DISTRIBUTION);
	}

	@Override
	public String deviceName() {
		return "Rectifier (" + Voltage.DISTRIBUTION.label() + ")";
	}

	@Override
	protected String label() {
		return String.format("Rectifier transformer (12-pulse thyristor, %s in, 3.2 MVA, up to 16 kA and %.0f V DC)", Voltage.DISTRIBUTION.label(), Tuning.RECTIFIER_MV_MAX_DC_VOLTS);
	}
}
