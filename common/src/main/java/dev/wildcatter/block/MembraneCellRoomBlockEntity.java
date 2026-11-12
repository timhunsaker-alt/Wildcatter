package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.ChlorAlkaliPlant;
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
 * The membrane cell room (docs/multiblocks/chlor-alkali-plant.md): 52 bipolar membrane elements in two electrolysers in series, 15 kA,
 * 161 V DC, 2.4 MW, a DC load on its terminals (east face, z 8: + at y 2 over - at y 0) or on a rectifier touching it. Treated brine
 * comes from the brine treatment it stands against; wet chlorine, hydrogen and 32 % caustic go out of the south face of its back cells
 * (x 0, 2 and 4). It stops (WET CHLORINE) when its chlorine line reaches no chlorine train, and when its caustic has nowhere to go;
 * hydrogen with nowhere to go is vented, as at the small cell. On brine that holds calcium its elements' voltage creeps up.
 */
public class MembraneCellRoomBlockEntity extends PlantUnitBlockEntity implements dev.wildcatter.circuit.CircuitPart, Electrolysis.Part, ChemPartBlock.SneakAction {
	private final dev.wildcatter.circuit.DcCell cells = new dev.wildcatter.circuit.DcCell(Tuning.CAP_ELEMENTS, Tuning.CA_CELL_BACK_EMF, Tuning.CAP_ELEMENT_VOLTS,
			Tuning.CAP_ELEMENT_KA * 1000);
	private final Electrolysis dc = new Electrolysis(cells, 5, 8, Direction.EAST);
	private double brineSalt;    // kg of NaCl in the treated brine it holds
	private double brineWater;   // litres of water with it
	private double calciumSeconds; // seconds run on brine that held calcium since the membranes were last replaced
	private boolean calcium;     // the last brine it was given held calcium (no CO2 at the treatment)
	private double making;       // kg/h of chlorine, the last interval
	private double cl2Total;

	public MembraneCellRoomBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MEMBRANE_CELL_ROOM_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[2] != 15 || at[1] != 0 || side != Direction.SOUTH) {
			return null;
		}
		return switch (at[0]) {
			case 0 -> Product.WET_CHLORINE;
			case 2 -> Product.HYDROGEN;
			case 4 -> Product.CAUSTIC_SODA;
			default -> null;
		};
	}

	private List<Lines.Face> chlorineOut() {
		return faces(at -> at[2] == 15 && at[1] == 0 && at[0] == 0, Direction.SOUTH);
	}

	@Override
	protected double hold(Product product) {
		double rate = ChlorAlkaliPlant.cl2Kgs() * Tuning.PLANT_HOLD_SECONDS * 4;
		return switch (product) {
			case WET_CHLORINE -> rate;
			case CAUSTIC_SODA -> rate * ChlorAlkaliPlant.caustic32PerCl2();
			case HYDROGEN -> rate * ChlorAlkaliPlant.hydrogenM3PerCl2();
			default -> 0;
		};
	}

	/** kg/h of chlorine over the last interval: about 990 at the rating. */
	public double making() {
		return making;
	}

	/** Room for treated brine, in kg of chlorine's worth: the treatment fills it. */
	double brineWanted() {
		double want = ChlorAlkaliPlant.cl2Kgs() * seconds() * 3;
		return Math.max(0, Math.min(want - brineSalt / ChlorAlkaliPlant.saltPerCl2(), want - brineWater / Tuning.CA_WATER_PER_CL2));
	}

	void giveBrine(double salt, double water, boolean holdsCalcium) {
		brineSalt += salt;
		brineWater += water;
		calcium = holdsCalcium;
		setChanged();
	}

	/** The creep in each element's voltage from calcium in its brine. */
	public double creep() {
		return ChlorAlkaliPlant.creep(calciumSeconds);
	}

	/** For the game test: as if it had run this long on calcium-bearing brine. */
	public void testCalcium(double seconds) {
		calciumSeconds = seconds;
		setChanged();
	}

	@Override
	public String sneakClick() {
		calciumSeconds = 0;
		setChanged();
		return "Membrane cell room: membranes replaced, its elements back to " + Tuning.CAP_ELEMENT_VOLTS + " V at full current";
	}

	private boolean trainOnLine(Level level) {
		for (Lines.Line line : Lines.lines(level, chlorineOut())) {
			if (line.ports().stream().anyMatch(p -> p.node() instanceof ChlorineTrainBlockEntity)) {
				return true;
			}
		}
		return false;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MembraneCellRoomBlockEntity r) {
		if (r.plcHold(level)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		r.making = 0;
		double emf = Tuning.CA_CELL_BACK_EMF + r.creep();
		if (Math.abs(r.cells.backEmf() - emf) > 1e-4) {
			r.cells.setBackEmf(emf);
			dev.wildcatter.circuit.CircuitManager.markDirty(level);
		}
		boolean train = r.trainOnLine(level);
		boolean work = r.brineSalt > 1e-6 && r.brineWater > 1e-6 && train && r.room(Product.WET_CHLORINE) > 1e-6 && r.room(Product.CAUSTIC_SODA) > 1e-6;
		r.dc.setDrawing(level, work);
		double share = r.dc.share();
		if (r.brineSalt <= 1e-6) {
			r.status = "NO BRINE: set it against a brine treatment, with a saturator fed salt";
		} else if (r.brineWater <= 1e-6) {
			r.status = "NO WATER: pump make-up water to the saturator";
		} else if (!train) {
			r.status = "WET CHLORINE: pipe its chlorine (the south face of its back end, west) to a chlorine train; nothing else takes it wet";
		} else if (r.room(Product.CAUSTIC_SODA) <= 1e-6 || r.room(Product.WET_CHLORINE) <= 1e-6) {
			r.status = "FULL: its " + (r.room(Product.CAUSTIC_SODA) <= 1e-6 ? "caustic" : "chlorine") + " has nowhere to go";
		} else if (r.dc.amps() < 1) {
			r.status = "NO DC: set a 12.47 kV RECTIFIER against it, or run DC bus bars to its terminals (east face)";
		} else {
			double cl2 = ChlorAlkaliPlant.cl2Kgs() * seconds() * share;
			cl2 = Math.min(cl2, Math.min(r.brineSalt / ChlorAlkaliPlant.saltPerCl2(), r.brineWater / Tuning.CA_WATER_PER_CL2));
			cl2 = Math.min(cl2, Math.min(r.room(Product.WET_CHLORINE), r.room(Product.CAUSTIC_SODA) / ChlorAlkaliPlant.caustic32PerCl2()));
			r.brineSalt = Math.max(0, r.brineSalt - cl2 * ChlorAlkaliPlant.saltPerCl2());
			r.brineWater = Math.max(0, r.brineWater - cl2 * Tuning.CA_WATER_PER_CL2);
			r.add(Product.WET_CHLORINE, cl2);
			r.add(Product.CAUSTIC_SODA, cl2 * ChlorAlkaliPlant.caustic32PerCl2());
			r.add(Product.HYDROGEN, Math.min(cl2 * ChlorAlkaliPlant.hydrogenM3PerCl2(), r.room(Product.HYDROGEN)));   // the rest is vented
			if (r.calcium) {
				r.calciumSeconds += seconds();
			}
			r.cl2Total += cl2;
			r.making = cl2 / seconds() * 3600;
			r.status = String.format("%d elements at %.2f V and %.2f kA: %,.0f kg/h of chlorine, %.0f kg/h of NaOH, %.0f Nm3/h of hydrogen%s", Tuning.CAP_ELEMENTS,
					r.dc.volts() / Tuning.CAP_ELEMENTS, r.dc.amps() / 1000, r.making, r.making * Tuning.NAOH_PER_CL2, r.making * ChlorAlkaliPlant.hydrogenM3PerCl2(),
					r.calcium ? "; CALCIUM IN THE BRINE (no CO2 at the treatment): the elements' voltage is creeping" : "");
		}
		r.setChanged();
	}

	@Override
	public Electrolysis electrolysis() {
		return dc;
	}

	@Override
	public boolean hasPort(Direction face, dev.wildcatter.circuit.Conductor role, dev.wildcatter.energy.Voltage voltage) {
		return false;
	}

	@Override
	public void build(dev.wildcatter.circuit.Circuit circuit) {
		dc.build(circuit, this);
	}

	@Override
	public void apply(dev.wildcatter.circuit.Circuit circuit, int ticks) {
		dc.apply(circuit);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Membrane cell room (%d elements, %.0f kA): %s; treated brine %,.0f kg of NaCl in %,.0f L; creep %.2f V an element "
				+ "(sneak-click to replace the membranes); %,.0f kg of chlorine made", Tuning.CAP_ELEMENTS, Tuning.CAP_ELEMENT_KA, status, brineSalt, brineWater, creep(), cl2Total));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("brineSalt", brineSalt);
		output.putDouble("brineWater", brineWater);
		output.putDouble("calciumSeconds", calciumSeconds);
		output.putDouble("cl2Total", cl2Total);
		output.putBoolean("calcium", calcium);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		brineSalt = input.getDoubleOr("brineSalt", 0);
		brineWater = input.getDoubleOr("brineWater", 0);
		calciumSeconds = input.getDoubleOr("calciumSeconds", 0);
		cl2Total = input.getDoubleOr("cl2Total", 0);
		calcium = input.getBooleanOr("calcium", false);
	}
}
