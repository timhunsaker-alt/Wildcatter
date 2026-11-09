package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.AmmoniaPlant;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The syngas compressor (docs/multiblocks/ammonia-plant.md): a reciprocating machine taking hydrogen and nitrogen, three to one, off
 * the runs on the east face of its bottom cells, to the synthesis loop's 150 bar. The ammonia converter touching it takes the gas.
 * 40 kW at the industrial voltage; its terminal box on the west face of its main cell.
 */
public class SyngasCompressorBlockEntity extends UnitMotorBlockEntity {
	private double syngas;   // Nm3 of H2 and N2, 3:1, at loop pressure
	private double making;   // Nm3/h, the last interval
	private String unit = "starting";

	public SyngasCompressorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.SYNGAS_COMPRESSOR_ENTITY, pos, state);
	}

	private List<Lines.Face> inlets() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	/** Nm3/h of syngas at the loop's full rate: one reformer's hydrogen and its nitrogen. */
	public static double ratedNm3h() {
		return AmmoniaPlant.ammoniaNm3h() * (AmmoniaPlant.H2_PER_NH3 + AmmoniaPlant.N2_PER_NH3);
	}

	private static double holdM3() {
		return ratedNm3h() / 3600 * Tuning.PLANT_HOLD_SECONDS * 4;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	/** The converter touching it takes up to {@code m3} of syngas; returns what it got. */
	public double takeSyngas(double m3) {
		double got = Math.min(m3, syngas);
		syngas -= got;
		setChanged();
		return got;
	}

	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double want = Math.min(ratedNm3h() / 3600 * seconds(), holdM3() - syngas);
		double h2 = Lines.available(level, inlets(), Product.HYDROGEN, Pull.MACHINE, this);
		double n2 = Lines.available(level, inlets(), Product.NITROGEN, Pull.MACHINE, this);
		if (h2 < 1e-7) {
			unit = "NO HYDROGEN: pipe a steam reformer's hydrogen to its east side";
			return;
		}
		if (n2 < 1e-7) {
			unit = "NO NITROGEN: pipe a nitrogen generator to its east side";
			return;
		}
		if (want <= 1e-7) {
			unit = "idle: FULL, no converter is taking its gas";
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
		double sets = Math.min(Math.min(want / 4, h2 / 3), n2) * strength;   // a set: 3 Nm3 of H2 and 1 of N2
		sets = Math.min(Lines.pull(level, inlets(), Product.HYDROGEN, sets * 3, Pull.MACHINE, this) / 3, sets);
		sets = Math.min(Lines.pull(level, inlets(), Product.NITROGEN, sets, Pull.MACHINE, this), sets);
		syngas += sets * 4;
		making = sets * 4 / seconds() * 3600;
		unit = String.format("compressing %.1f Nm3/h of syngas (H2:N2 3:1) to %.0f bar", making, Tuning.SYNGAS_LOOP_BAR);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Syngas compressor (%.0f kW, %s): ", Tuning.SYNGAS_COMPRESSOR_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; %.2f Nm3 held; ", syngas) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("syngas", syngas);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		syngas = input.getDoubleOr("syngas", 0);
	}
}
