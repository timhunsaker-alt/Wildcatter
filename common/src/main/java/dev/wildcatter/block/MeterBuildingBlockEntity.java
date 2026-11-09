package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.Run;
import dev.wildcatter.fluid.SalesGas;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A sales gas meter station's meter building and controller (docs/multiblocks/sales-gas.md): a prefabricated shelter with the flow
 * computer (AGA 9 ultrasonic flow, AGA 8 compressibility), a gas chromatograph reading the heating value, and the RTU with its battery.
 * It runs on 2 kW at the world's residential voltage, its service box on the west face of its main cell.
 * <ul>
 * <li>RECEIPT: it takes up to 5 m3/s of pipeline gas through the filter separator, books it on the dimension's line less 2 % fuel, and
 * refuses gas off spec (rich, wet or biogas) as a real pipeline shuts in a receipt point.</li>
 * <li>DELIVERY: it puts pipeline gas out of the regulator skid, up to its rating, while the line's account holds energy for it.</li>
 * </ul>
 * Without power it runs on the RTU's battery for 8 game hours, then reads METER FAILED and shuts in: a station cannot sell unmetered gas.
 */
public class MeterBuildingBlockEntity extends UnitMotorBlockEntity {
	public enum Mode {
		RECEIPT, DELIVERY
	}

	private Mode mode = Mode.RECEIPT;
	private double battery = Tuning.METER_BATTERY_TICKS;   // ticks of RTU battery left
	private String station = "starting";
	private boolean flowing;
	private boolean shutIn = true;
	private double flow;      // m3/s, the last interval
	private double heating = Tuning.GAS_JOULES_PER_M3;   // J/m3, the chromatograph's last reading
	private long day = -1;
	private double dayM3;
	private double lifeM3;

	public MeterBuildingBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.METER_BUILDING_ENTITY, pos, state);
	}

	public Mode mode() {
		return mode;
	}

	/** Sneak and right-click: RECEIPT or DELIVERY. */
	public Mode toggleMode() {
		mode = mode == Mode.RECEIPT ? Mode.DELIVERY : Mode.RECEIPT;
		setChanged();
		return mode;
	}

	/** Game test hook. */
	public void setMode(Mode value) {
		mode = value;
		setChanged();
	}

	/** Game test hook: the battery's charge in ticks, to skip the 8 hours it lasts. */
	public void setBatteryForTest(double ticks) {
		battery = ticks;
		setChanged();
	}

	public boolean flowing() {
		return flowing;
	}

	public boolean shutIn() {
		return shutIn;
	}

	/** m3/s through the meter, the last interval. */
	public double flow() {
		return flow;
	}

	/** m3 through the meter since it was built: the run ticket's lifetime total. */
	public double lifetimeM3() {
		return lifeM3;
	}

	public String station() {
		return station;
	}

	// ---- the residential service: single-phase, so its neutral lands too -------------------------------------------------------
	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == LeasePartBlock.world(getBlockState(), Direction.WEST) && voltage == motor().voltage();
	}

	// ---- PLC points (docs/multiblocks/plc.md, as the other process plants) ---------------------------------------------------------
	@Override
	public List<Point> points() {
		return List.of(new Point("RUN", true), new Point("RUNNING", false), new Point("SHUT IN", false), new Point("AUTO", false),
				new Point("FLOW 4-20 mA, 0-5 m3/s", false, Signal.MA), new Point("HEATING VALUE 4-20 mA, 30-45 MJ/m3", false, Signal.MA));
	}

	@Override
	public boolean readPoint(int point) {
		return switch (point) {
			case 1 -> flowing;
			case 2 -> shutIn;
			case 3 -> unitRemote.auto();
			default -> false;
		};
	}

	@Override
	public double readMa(int point) {
		double share = switch (point) {
			case 4 -> flow / Tuning.SALES_STATION_M3S;
			case 5 -> (heating / 1e6 - 30) / 15;
			default -> 0;
		};
		return 4 + 16 * Math.max(0, Math.min(1.03, share));
	}

	@Override
	public double measure(Quantity quantity) {
		return quantity == Quantity.FLOW ? flow * 1000 : super.measure(quantity);   // the transmitters' unit is L/s
	}

	// ---- each interval ---------------------------------------------------------------------------------------------------------------
	private SalesGasUnitBlockEntity touching(Level level, BlockPos at, String kind) {
		return Touching.find(level, at, SalesGasUnitBlockEntity.class, u -> u.kind().equals(kind));
	}

	@Override
	protected void work(Level level) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		long today = level.getGameTime() / 24_000;   // a game day of running
		if (today != day) {
			day = today;
			dayM3 = 0;
		}
		boolean powered = power(level) > 0;
		battery = powered ? Math.min(Tuning.METER_BATTERY_TICKS, battery + Tuning.WELL_INTERVAL) : Math.max(0, battery - Tuning.WELL_INTERVAL);
		flowing = false;
		shutIn = true;
		flow = 0;
		long now = level.getGameTime();
		SalesGasUnitBlockEntity run = touching(level, worldPosition, "ultrasonic_meter_run");
		SalesGasUnitBlockEntity tap = run == null ? null : touching(level, run.getBlockPos(), "pipeline_tap");
		SalesGasUnitBlockEntity filter = run == null ? null : touching(level, run.getBlockPos(), "filter_separator");
		SalesGasUnitBlockEntity regulator = run == null ? null : touching(level, run.getBlockPos(), "regulator_skid");
		SalesGasLine line = SalesGasLine.get(server);
		if (run == null || tap == null || (mode == Mode.RECEIPT ? filter == null : regulator == null)) {
			station = "NOT BUILT: " + (run == null ? "NO METER RUN (set one against the building)" : tap == null ? "NO TAP (set a pipeline tap against the meter run)"
					: mode == Mode.RECEIPT ? "NO FILTER (a receipt station needs a filter separator against the meter run)"
					: "NO REGULATOR (a delivery station needs a regulator skid against the meter run)");
		} else if (battery <= 0) {
			station = "METER FAILED: no power and the RTU's battery is flat, so the station is shut in (it cannot sell unmetered gas)";
		} else if (!unitRemote.hand() && !unitRemote.on(0, now)) {
			station = unitRemote.auto() ? "SHUT IN: its PLC is not giving it RUN" : "SHUT IN: selector in OFF";
		} else if (mode == Mode.RECEIPT) {
			receive(level, line, filter);
		} else {
			deliver(line, regulator);
		}
		if (!powered && battery > 0 && !station.startsWith("NOT BUILT")) {
			station += String.format("; NO POWER: on the RTU's battery, %.1f h left", battery / 1000.0);
		}
		String valve = "block valve " + (shutIn ? "CLOSED" : "OPEN") + (flowing ? String.format(", %.2f m3/s", flow) : "");
		if (tap != null) {
			tap.read(valve, now);
		}
		if (run != null) {
			run.read(flowing ? String.format("%.2f m3/s (%.1f MMscf/d)", flow, flow * 86_400 / Tuning.M3_PER_SCF / 1e6) : "no flow", now);
		}
		if (filter != null && mode == Mode.RECEIPT) {
			filter.read(flowing ? "passing the receipt's gas" : "no flow", now);
		}
		if (regulator != null && mode == Mode.DELIVERY) {
			regulator.read(flowing ? String.format("letting down %.2f m3/s, its line heater burning %.1f %%", flow, Tuning.SALES_LINE_HEATER_SHARE * 100) : "no flow", now);
		}
	}

	private void receive(Level level, SalesGasLine line, SalesGasUnitBlockEntity filter) {
		List<Lines.Face> inlet = filter.inlet();
		Run wrong = Lines.wrong(level, inlet, Product.PIPELINE_GAS);
		if (wrong != null) {
			SalesGas.Spec spec = wrong.blocked() ? SalesGas.Spec.NONE : SalesGas.check(wrong.product());
			heating = spec == SalesGas.Spec.CO2 ? Tuning.BIOGAS_JOULES_PER_M3 : heating;
			station = "RECEIPT: " + (wrong.blocked() ? "its inlet is " + wrong.describe() : spec.reading()) + "; shut in, nothing booked";
			return;
		}
		double m3 = Lines.pull(level, inlet, Product.PIPELINE_GAS, SalesGas.rating(seconds()), Pull.MACHINE, filter);
		heating = Tuning.GAS_JOULES_PER_M3;
		if (m3 <= 0) {
			shutIn = false;
			station = "RECEIPT: open, no gas at the filter separator's inlet (its north side)";
			return;
		}
		line.receive(m3);
		filter.caught(m3 * Tuning.FILTER_CATCH_L_PER_M3);
		book(m3);
		station = String.format("RECEIPT: %.2f m3/s of pipeline gas on spec, %.0f MJ/m3 (%.0f Btu/scf), %.1f MW", flow, heating / 1e6,
				SalesGas.btuPerScf(heating), flow * heating / 1e6);
	}

	private void deliver(SalesGasLine line, SalesGasUnitBlockEntity regulator) {
		double m3 = SalesGas.deliverable(line.balance(), Math.min(regulator.roomFor(Product.PIPELINE_GAS), SalesGas.rating(seconds())));
		heating = Tuning.GAS_JOULES_PER_M3;
		if (m3 > 0) {
			line.deliver(m3);
			regulator.give(Product.PIPELINE_GAS, m3);
			book(m3);
		}
		if (line.balance() <= 1e-9 && m3 <= 0) {
			station = "DELIVERY: NO GAS ON THE LINE (the line never delivers more than was received, less fuel); shut in";
			return;
		}
		shutIn = false;
		station = flowing ? String.format("DELIVERY: %.3f m3/s of pipeline gas out of the regulator skid, %.2f MW", flow, flow * heating / 1e6)
				: "DELIVERY: open, nothing taking gas from the regulator skid";
	}

	private void book(double m3) {
		flow = m3 / seconds();
		flowing = true;
		shutIn = false;
		dayM3 += m3;
		lifeM3 += m3;
		setChanged();
	}

	@Override
	public Component inspect() {
		double dayGj = SalesGas.gj(dayM3), lifeGj = SalesGas.gj(lifeM3);
		String balance = level instanceof ServerLevel server ? String.format("; the line's account %.1f GJ (%.0f GJ kept as fuel)",
				SalesGasLine.get(server).balance(), SalesGasLine.get(server).retainedGj()) : "";
		return Component.literal(String.format("Meter station, %s (%.0f kW, %s): ", mode, Tuning.METER_BUILDING_WATTS / 1000, Voltage.LOW.label()) + station
				+ String.format("; run ticket: today %,.0f m3, %.1f GJ, %.1f MMBtu; all told %,.0f m3, %.1f GJ, %.1f MMBtu; heating value %.1f MJ/m3",
						dayM3, dayGj, SalesGas.mmbtu(dayGj), lifeM3, lifeGj, SalesGas.mmbtu(lifeGj), heating / 1e6)
				+ balance + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("mode", mode.name());
		output.putDouble("battery", battery);
		output.putLong("day", day);
		output.putDouble("dayM3", dayM3);
		output.putDouble("lifeM3", lifeM3);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		mode = "DELIVERY".equals(input.getStringOr("mode", "RECEIPT")) ? Mode.DELIVERY : Mode.RECEIPT;
		battery = input.getDoubleOr("battery", Tuning.METER_BATTERY_TICKS);
		day = input.getLongOr("day", -1);
		dayM3 = input.getDoubleOr("dayM3", 0);
		lifeM3 = input.getDoubleOr("lifeM3", 0);
	}
}
