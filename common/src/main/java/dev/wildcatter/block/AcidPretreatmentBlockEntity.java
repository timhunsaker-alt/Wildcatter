package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Phosphate;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The acid pretreatment (docs/multiblocks/uranium-from-phosphate.md), the uranium recovery's controller: a clarifier, a cooler to
 * about 40 C and an air oxidiser that make the filter acid's uranium U(VI) and take out its solids and humic matter. Filter acid in on
 * the north face of its front cells; it runs the DEPA-TOPO circuit against its east side, whose treated acid goes on to the evaporator.
 * With no circuit, or one that cannot take the acid, the acid passes straight through and out of its own south face, so the fertilizer
 * plant runs on. 45 kW on the world's industrial voltage, the box on the west face of its main cell.
 */
public class AcidPretreatmentBlockEntity extends UnitMotorBlockEntity {
	private double making;   // L/s of acid treated, the last interval
	private String unit = "starting";

	public AcidPretreatmentBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.ACID_PRETREATMENT_ENTITY, pos, state);
	}

	private List<Lines.Face> acidIn() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 5 && at[1] == 0 && side == Direction.SOUTH ? Product.FILTER_ACID : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.FILTER_ACID ? Phosphate.filterAcidLps() * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	/** L/s of acid through the circuit over the last interval. */
	public double making() {
		return making;
	}

	/** Passes acid straight through to its own south face; returns the readout. */
	private String bypass(Level level, double litres, String why) {
		double l = Math.min(litres, room(Product.FILTER_ACID));
		if (l <= 1e-7) {
			return why + "; its bypass is FULL (pipe its south side to the evaporator)";
		}
		add(Product.FILTER_ACID, Lines.pull(level, acidIn(), Product.FILTER_ACID, l, Pull.MACHINE, this));
		return why + String.format("; passing %.2f L/s of acid straight through to its south side", l / seconds());
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double acid = Math.min(Phosphate.filterAcidLps() * seconds(), Lines.available(level, acidIn(), Product.FILTER_ACID, Pull.MACHINE, this));
		if (acid < 1e-7) {
			unit = "NO ACID: pipe filter acid to its north side";
			return;
		}
		DepaTopoCircuitBlockEntity circuit = Touching.find(level, worldPosition, DepaTopoCircuitBlockEntity.class, c -> true);
		if (circuit == null) {
			unit = bypass(level, acid, "NO CIRCUIT: set the DEPA-TOPO circuit against its east side");
			return;
		}
		String why = circuit.prepare(level, acid);
		if (why != null) {
			unit = bypass(level, acid, "circuit " + why);
			return;
		}
		if (plcHold(level)) {
			unit = bypass(level, acid, status);
			return;
		}
		double strength = Math.min(drive(level), circuit.drive(level));
		if (strength <= 0) {
			unit = bypass(level, acid, "starting: " + describe() + "; circuit " + circuit.motor().describe());
			return;
		}
		double l = Lines.pull(level, acidIn(), Product.FILTER_ACID, acid * strength, Pull.MACHINE, this);
		circuit.treat(level, l);
		making = l / seconds();
		unit = String.format("clarifying, cooling to 40 C and oxidising %.2f L/s of filter acid at %.2f g of U a litre to the circuit", making, Phosphate.uraniumGPerL());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Acid pretreatment (%.0f kW, %s): ", Tuning.ACID_PRETREATMENT_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit + "; "
				+ describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, making > 0, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(making, Phosphate.filterAcidLps());
	}
}
