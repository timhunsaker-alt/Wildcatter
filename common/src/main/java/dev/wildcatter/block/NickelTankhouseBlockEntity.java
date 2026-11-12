package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.circuit.DcCell;
import dev.wildcatter.fluid.Nickel;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The nickel tankhouse (docs/multiblocks/nickel-smelter.md, dc-bus.md): 18 chloride electrowinning cells in series under titanium anode
 * hoods, 12.3 kA at 3.5 V a cell, 63 V: a DC load on its terminals (+ over - on its south end, x 1) or on the rectifier it touches, which
 * takes its rated current. Its nickel follows the current (Faraday, 1.095 g an ampere-hour, 95 % current efficiency): about 230 kg an
 * hour at full current, cathode nickel for the bagging station set against it. The anode chlorine goes back to the leach reactor it
 * touches.
 */
public class NickelTankhouseBlockEntity extends PlantUnitBlockEntity implements dev.wildcatter.circuit.CircuitPart, Electrolysis.Part, Bagged {
	private final Electrolysis dc = new Electrolysis(new DcCell(Tuning.NI_CELLS, Tuning.NI_CELL_BACK_EMF, Tuning.NI_CELL_VOLTS, Tuning.NI_CELL_KA * 1000), 1, 13,
			Direction.SOUTH);
	private double liquorNi;     // kg of Ni in the cells' catholyte
	private double cathode;      // kg of cathode nickel waiting for bags
	private double won;          // t, all told
	private double making;       // kg/h
	private boolean dcOk = true; // whether, the last time it had liquor and drew, DC came
	private boolean drawing;
	private int noDc;

	public NickelTankhouseBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.NICKEL_TANKHOUSE_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public double cathode() {
		return cathode;
	}

	public double making() {
		return making;
	}

	public double amps() {
		return dc.amps();
	}

	public double volts() {
		return dc.volts();
	}

	/** Whether its anode chlorine is coming back to the leach: false once it has liquor and no DC through it. */
	public boolean givingChlorine() {
		return dcOk;
	}

	public String shortStatus() {
		return dcOk ? "running" : "NO DC";
	}

	/** For the game test: pure liquor carrying {@code kg} of Ni put in its cells. */
	public void testAddLiquor(double kg) {
		liquorNi += kg;
		setChanged();
	}

	/** For the game test: cathode nickel brought to {@code kg}, as if won. */
	public void testFillCathode(double kg) {
		cathode = kg;
		setChanged();
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.NICKEL_CATHODE_BAG;
	}

	@Override
	public double bagReady() {
		return cathode;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, cathode);
		cathode -= got;
		setChanged();
		return got;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, NickelTankhouseBlockEntity t) {
		if (level.isClientSide() || (level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		t.work(level);
		t.setChanged();
	}

	private void work(Level level) {
		double dt = seconds();
		making = 0;
		NiPurificationBlockEntity purification = Touching.find(level, worldPosition, NiPurificationBlockEntity.class, p -> true);
		ChlorineLeachReactorBlockEntity leach = Touching.find(level, worldPosition, ChlorineLeachReactorBlockEntity.class, p -> true);
		if (purification != null && liquorNi < Tuning.LEACH_LIQUOR_HOLD_KG_NI * 0.25) {
			liquorNi += purification.takePureNi(Tuning.LEACH_LIQUOR_HOLD_KG_NI * 0.25 - liquorNi);
		}
		boolean held = plcHold(level);
		boolean work = !held && liquorNi > 1e-6 && cathode < Tuning.NI_CATHODE_HOLD_KG && leach != null;
		dc.setDrawing(level, work);
		if (drawing && work) {   // five seconds drawing with no current through it: NO DC, and the leach's chlorine stops coming back
			noDc = dc.amps() >= 1 ? 0 : noDc + 1;
			dcOk = noDc < 25;
		}
		drawing = work;
		if (held) {
			return;
		}
		if (purification == null || leach == null) {
			status = "NOT BUILT: " + (leach == null ? "set it against the chlorine leach reactor (its anode chlorine goes back there)" : "set it against the purification circuit");
		} else if (liquorNi <= 1e-6) {
			status = "waiting for pure liquor from the purification circuit";
		} else if (cathode >= Tuning.NI_CATHODE_HOLD_KG) {
			status = "FULL: set a bagging station against it with empty bulk bags";
		} else if (dc.amps() < 1) {
			status = "NO DC: set a RECTIFIER against its south end, or run DC bus bars to its terminals (south end)";
		} else {
			double kg = Math.min(Nickel.cathodeKgs(dc.amps()) * dt, liquorNi);
			liquorNi -= kg;
			cathode += kg;
			won += kg / 1000;
			making = kg / dt * 3600;
			leach.returnChlorine(kg * Nickel.anodeChlorinePerNi());
			status = String.format("%.1f kA at %.1f V (%d cells): %.0f kg/h of cathode nickel, %.0f kg/h of chlorine back to the leach", dc.amps() / 1000,
					dc.volts(), Tuning.NI_CELLS, making, making * Nickel.anodeChlorinePerNi());
		}
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
	public double measure(Quantity quantity) {
		return switch (quantity) {
			case AMPS -> dc.amps();
			case VOLTS -> dc.volts();
			case LEVEL -> cathode / Tuning.NI_CATHODE_HOLD_KG * 100;
			default -> Double.NaN;
		};
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Nickel tankhouse (%d chloride cells, %.1f kA, %.0f V rated): %s; liquor %.0f kg of Ni in the cells; cathode %.0f kg for bags; %.2f t won",
				Tuning.NI_CELLS, Tuning.NI_CELL_KA, Nickel.tankhouseVolts(), status, liquorNi, cathode, won));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("liquorNi", liquorNi);
		output.putDouble("cathode", cathode);
		output.putDouble("won", won);
		output.putBoolean("dcOk", dcOk);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		liquorNi = input.getDoubleOr("liquorNi", 0);
		cathode = input.getDoubleOr("cathode", 0);
		won = input.getDoubleOr("won", 0);
		dcOk = input.getBooleanOr("dcOk", true);
	}
}
