package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.FurnaceLoad;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Ferroalloys;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The furnace transformer (docs/multiblocks/ferroalloys.md): 10 MVA, water-cooled, its primary on the distribution tier (the terminal box
 * on the west face of its main cell) and a 150 V secondary at full tap, about 38 kA a phase, closed in a delta at the electrodes by
 * flexible cables and bus tubes. Its on-load tap changer has 15 steps; the furnace's power goes as the square of the secondary volts.
 * The submerged arc furnace against it is its load: 8 MW at PF 0.80 at full tap. It meters what it takes, and with capacitor banks
 * against it (on its primary bus) the power factor its incoming feeder sees.
 */
public class FurnaceTransformerBlockEntity extends PlantUnitBlockEntity implements CircuitPart {
	private final FurnaceLoad load = new FurnaceLoad(Voltage.DISTRIBUTION, Tuning.SAF_POWER_FACTOR);
	private int tap = Tuning.FURNACE_TAPS;

	public FurnaceTransformerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FURNACE_TRANSFORMER_ENTITY, pos, state);
	}

	public FurnaceLoad load() {
		return load;
	}

	public int tap() {
		return tap;
	}

	public void setTap(int tap) {
		int t = Math.max(1, Math.min(Tuning.FURNACE_TAPS, tap));
		if (t != this.tap) {
			this.tap = t;
			setChanged();
		}
	}

	/** Sneak and right-click: the next tap, wrapping from the top back to 1. */
	public String nextTap() {
		setTap(tap >= Tuning.FURNACE_TAPS ? 1 : tap + 1);
		return String.format("Furnace transformer: tap %d of %d, %.0f V secondary, %.1f MW at the furnace", tap, Tuning.FURNACE_TAPS, Ferroalloys.secondaryVolts(tap),
				Ferroalloys.tapWatts(tap) / 1e6);
	}

	/** The furnace against its east face (its secondary side). */
	public @Nullable SubmergedArcFurnaceBlockEntity furnace() {
		return level == null ? null : Touching.find(level, worldPosition, SubmergedArcFurnaceBlockEntity.class, f -> true);
	}

	/** The capacitor banks against it, on its primary bus. */
	public List<CapacitorBankBlockEntity> banks() {
		if (level == null) {
			return List.of();
		}
		return Touching.parts(level, worldPosition).stream().filter(e -> e instanceof CapacitorBankBlockEntity).map(e -> (CapacitorBankBlockEntity) e).toList();
	}

	/** Watts and vars its incoming feeder carries: the furnace's, less what the banks on its bus give. */
	public double feederWatts() {
		double w = load.watts();
		for (CapacitorBankBlockEntity bank : banks()) {
			w += bank.capacitor().watts();
		}
		return w;
	}

	public double feederVars() {
		double q = load.vars();
		for (CapacitorBankBlockEntity bank : banks()) {
			q -= bank.capacitor().vars();
		}
		return q;
	}

	public double feederPowerFactor() {
		return Ferroalloys.powerFactor(feederWatts(), feederVars());
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	public List<Point> points() {
		return List.of();
	}

	/** What a transmitter on it reads: the furnace's MW, its primary volts and amps, and the power factor at its feeder. */
	@Override
	public double measure(Quantity quantity) {
		return switch (quantity) {
			case WATTS -> load.watts();
			case VOLTS -> load.volts();
			case AMPS -> load.amps();
			case POWER_FACTOR -> feederPowerFactor();
			default -> Double.NaN;
		};
	}

	private Direction terminalFace() {
		return LeasePartBlock.world(getBlockState(), Direction.WEST);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == terminalFace() && voltage == Voltage.DISTRIBUTION && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		SubmergedArcFurnaceBlockEntity furnace = furnace();
		load.setDemand(furnace == null ? 0 : furnace.wantedWatts(Ferroalloys.tapWatts(tap)));
		load.build(circuit, worldPosition, List.of(terminalFace()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		load.apply(circuit, ticks);
		setChanged();
	}

	/** The furnace's side of the readout: MW, Mvar, PF and the electrode current. */
	public String metering() {
		double w = load.watts();
		double q = load.vars();
		String feeder = "";
		List<CapacitorBankBlockEntity> banks = banks();
		if (!banks.isEmpty()) {
			double given = banks.stream().mapToDouble(b -> b.capacitor().vars()).sum();
			feeder = String.format("; with %.1f Mvar of capacitors on its bus its feeder carries %.1f MW, %.1f Mvar: PF %.2f", given / 1e6, feederWatts() / 1e6,
					feederVars() / 1e6, feederPowerFactor());
		}
		return String.format("%.2f MW, %.2f Mvar, PF %.2f, %.1f kA in each electrode%s", w / 1e6, q / 1e6, Ferroalloys.powerFactor(w, q),
				Ferroalloys.electrodeAmps(w, tap) / 1000, feeder);
	}

	@Override
	public Component inspect() {
		String state = load.undervoltage() ? load.describe() : load.watts() > 1000 ? metering() + (load.ramping() ? ", the electrodes going down" : "")
				: furnace() == null ? "NO FURNACE: set a submerged arc furnace against its east face" : "idle: the furnace is not drawing";
		return Component.literal(String.format("Furnace transformer (%.0f MVA, %s to %.0f V): tap %d of %d (%.0f V secondary, %.1f MW at the furnace); primary %s; ",
				Tuning.FURNACE_TRANSFORMER_VA / 1e6, Voltage.DISTRIBUTION.label(), Tuning.FURNACE_SECONDARY_VOLTS, tap, Tuning.FURNACE_TAPS, Ferroalloys.secondaryVolts(tap),
				Ferroalloys.tapWatts(tap) / 1e6, load.describe()) + state + "; sneak and right-click steps the tap");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("tap", tap);
		load.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tap = Math.max(1, Math.min(Tuning.FURNACE_TAPS, input.getIntOr("tap", Tuning.FURNACE_TAPS)));
		load.load(input);
	}
}
