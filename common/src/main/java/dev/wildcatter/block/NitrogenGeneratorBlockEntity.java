package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A PSA nitrogen generator on carbon molecular sieve (docs/multiblocks/ammonia-plant.md): its air compressor and two sieve beds make
 * 10 Nm3/h of 99.9 % nitrogen from air, about 0.3 kWh a Nm3 (3 kW at the industrial voltage). Nitrogen out of the south face of its
 * back cells; its terminal box on the west face of its main cell.
 */
public class NitrogenGeneratorBlockEntity extends UnitMotorBlockEntity {
	private String unit = "starting";
	private double making;   // Nm3/h, the last interval

	public NitrogenGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.NITROGEN_GENERATOR_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 2 && at[1] == 0 && side == Direction.SOUTH ? Product.NITROGEN : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.NITROGEN ? Tuning.N2_GENERATOR_NM3H / 3600 * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	/** Nm3/h of nitrogen it made over the last interval. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double room = room(Product.NITROGEN);
		if (room <= 1e-7) {
			unit = "idle: FULL, its nitrogen has nowhere to go (pipe its back end to a syngas compressor)";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "NO AIR: its air compressor is not running";
			return;
		}
		double m3 = Math.min(room, Tuning.N2_GENERATOR_NM3H / 3600 * seconds() * strength);
		add(Product.NITROGEN, m3);
		making = m3 / seconds() * 3600;
		unit = String.format("making %.1f Nm3/h of 99.9 %% nitrogen", making);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Nitrogen generator (PSA, %.0f Nm3/h, %.0f kW, %s): ", Tuning.N2_GENERATOR_NM3H, Tuning.N2_GENERATOR_WATTS / 1000, Voltage.INDUSTRIAL.label())
				+ unit + String.format("; nitrogen held %.2f Nm3; ", held(Product.NITROGEN)) + describe());
	}
}
