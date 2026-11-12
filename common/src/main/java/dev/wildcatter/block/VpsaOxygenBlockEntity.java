package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.TitaniumPigment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The VPSA oxygen unit (docs/multiblocks/titanium-pigment.md): a vacuum-swing adsorption unit, 100 Nm3/h of 93 % oxygen on 35 kW at the
 * world's industrial voltage (box on the west face of its main cell). Oxygen out of the south face of its back cells, for the TiCl4
 * oxidiser (or another user); it runs while its oxygen is drawn.
 */
public class VpsaOxygenBlockEntity extends UnitMotorBlockEntity {
	private double making;   // Nm3/h, the last interval
	private String unit = "idle: its oxygen is not drawn";

	public VpsaOxygenBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.VPSA_OXYGEN_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 5 && at[1] == 0 && side == Direction.SOUTH ? Product.OXYGEN : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.OXYGEN ? TitaniumPigment.vpsaM3s() * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double room = room(Product.OXYGEN);
		if (room <= 1e-6) {
			unit = "idle: its oxygen is not drawn (pipe its south side to the oxidiser)";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double m3 = Math.min(room, TitaniumPigment.vpsaM3s() * seconds() * strength);
		add(Product.OXYGEN, m3);
		making = m3 / seconds() * 3600;
		unit = String.format("making %.0f Nm3/h of 93 %% oxygen", making);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("VPSA oxygen unit (%.0f Nm3/h, %.0f kW, %s): ", Tuning.VPSA_O2_NM3H, Tuning.VPSA_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ "; " + describe());
	}
}
