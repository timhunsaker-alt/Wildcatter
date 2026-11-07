package dev.wildcatter.control;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.control.Measurable.Quantity;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigParts;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A two-wire transmitter: reads one quantity of the machine it is clamped to and gives 4-20 mA over its range. Past the range it saturates at
 * 3.8 and 20.5 mA; with nothing to read it fails low at 3.5 mA, as NAMUR NE 43 sets out (docs/multiblocks/plc.md).
 */
public class TransmitterBlockEntity extends BlockEntity implements ControlDevice, Inspectable {
	private static final double FAILED = 3.5;
	private Quantity quantity = Quantity.LEVEL;
	private double low = 0;
	private double high = 100;
	/** A flow's running total, for a counter module: one pulse a litre. */
	private double total;

	public TransmitterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.TRANSMITTER_ENTITY, pos, state);
	}

	private Measurable target() {
		if (level == null) {
			return null;
		}
		BlockPos behind = worldPosition.relative(getBlockState().getValue(TransmitterBlock.FACING).getOpposite());
		return BigParts.entity(level, behind) instanceof Measurable measurable ? measurable : null;
	}

	/** What it reads now; NaN if it reads nothing. */
	public double reading() {
		Measurable target = target();
		return target == null ? Double.NaN : target.measure(quantity);
	}

	public Quantity quantity() {
		return quantity;
	}

	public void setQuantity(Quantity next) {
		quantity = next;
		setChanged();
	}

	/** Steps to the next quantity the machine behind has. */
	public void nextQuantity() {
		Measurable target = target();
		Quantity[] all = Quantity.values();
		for (int i = 1; i <= all.length; i++) {
			Quantity next = all[(quantity.ordinal() + i) % all.length];
			if (target == null || !Double.isNaN(target.measure(next))) {
				quantity = next;
				setChanged();
				return;
			}
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TransmitterBlockEntity transmitter) {
		if (transmitter.quantity == Quantity.FLOW) {
			double now = transmitter.reading();
			if (!Double.isNaN(now)) {
				transmitter.total += now / 20.0;
			}
		}
	}

	@Override
	public String deviceName() {
		return quantity.letters + " transmitter (" + quantity.name().toLowerCase(Locale.ROOT).replace('_', ' ') + ")";
	}

	@Override
	public List<Point> points() {
		return List.of(new Point("PV 4-20 mA", false, Signal.MA), new Point("PULSE", false, Signal.PULSE), new Point("SWITCH, OVER MID-RANGE", false));
	}

	@Override
	public boolean readPoint(int point) {
		double value = reading();
		return point == 2 && !Double.isNaN(value) && value > (low + high) / 2;   // a level, pressure, flow or zero-speed switch: a contact at the range's middle
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
	}

	@Override
	public double readMa(int point) {
		double value = reading();
		if (Double.isNaN(value) || high == low) {
			return FAILED;
		}
		return Math.max(3.8, Math.min(20.5, 4 + 16 * (value - low) / (high - low)));
	}

	@Override
	public long readPulses(int point) {
		return (long) total;
	}

	@Override
	public double readTenths(int point) {
		return quantity == Quantity.TEMPERATURE ? reading() * 10 : Double.NaN;
	}

	@Override
	public String setRange(double lowEnd, double highEnd) {
		if (lowEnd == highEnd) {
			return "The range's two ends must differ";
		}
		low = lowEnd;
		high = highEnd;
		setChanged();
		return null;
	}

	@Override
	public Component inspect() {
		double value = reading();
		return Component.literal(deviceName() + ": " + (Double.isNaN(value) ? "NOTHING TO READ behind it, failed low at 3.5 mA" : String.format(Locale.ROOT, "%.2f %s, %.2f mA", value, quantity.unit, readMa(0)))
				+ String.format(Locale.ROOT, "; range %.2f to %.2f %s", low, high, quantity.unit));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("quantity", quantity.name());
		output.putDouble("low", low);
		output.putDouble("high", high);
		output.putDouble("total", total);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		quantity = Quantity.valueOf(input.getStringOr("quantity", Quantity.LEVEL.name()));
		low = input.getDoubleOr("low", 0);
		high = input.getDoubleOr("high", 100);
		total = input.getDoubleOr("total", 0);
	}
}
