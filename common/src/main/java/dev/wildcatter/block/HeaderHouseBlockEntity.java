package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DriveLoad;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import java.util.ArrayList;
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
 * An ISR header house (docs/multiblocks/isr-wellfield.md): the manifolds for up to 25 producers, the drives for their submersible
 * pumps (a variable 480 V load, 5.6 kW a running pump; its box on the north face of its main cell) and the oxygen injection on the
 * injection header, from an oxygen generator against it.
 * - Producers' pipe on the west face of its bottom cells; it runs their pumps.
 * - Pregnant lixiviant out to the IX columns: the south face of its back cells, the west two.
 * - Barren lixiviant back from them: the south face of its back cells, the east two.
 * - Barren lixiviant with oxygen out to the injectors on the pipe at the east face of its bottom cells, less 1 % bleed: its
 *   injection manifold meters each its share, up to 1.24 L/s.
 * - 16d1 (docs/multiblocks/isr-operations.md): a CO2 storage tank against it doses the injection with CO2 at 0.5 g/L, raising the head
 *   grade by up to 30 %; NO CO2 only when such a tank is there and empty. The bleed goes out of the south face of its back cells, one
 *   up (the bottom row there is the IX lines), as
 *   wellfield bleed, for an injection pump and a disposal well; what nothing takes is only counted, as before.
 */
public class HeaderHouseBlockEntity extends PlantUnitBlockEntity implements CircuitPart {
	private final DriveLoad load = new DriveLoad(Voltage.INDUSTRIAL);
	private int producers;
	private double bleed;   // m3 of bleed held back so far
	private double co2Share;   // 16d1: of the full dose, the last interval
	private double co2Used;    // kg
	// 16d2 (isr-operations.md items 3, 4): the monitor ring, the lixiviant front, restoration
	public static final int MINE = 0, SWEEP = 1, RO = 2, STABILITY = 3, RESTORED = 4;
	private static final String[] MODES = {"MINE", "SWEEP", "RO", "STABILITY", "RESTORED"};
	private int mode;
	private double front;          // blocks the lixiviant has spread beyond the patterns
	private double excursion;      // s the front has been out at a monitor well
	private double swept, treated; // m3
	private double stability;      // s without an excursion
	private double bleedForTest = -1;
	private boolean ringOk = true, stopped;
	private String ringNote = "";

	public HeaderHouseBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.HEADER_HOUSE_ENTITY, pos, state);
	}

	private List<Lines.Face> wells() {
		return faces(at -> at[1] == 0, Direction.WEST);
	}

	private List<Lines.Face> injection() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	private List<Lines.Face> bleedFaces() {
		return faces(at -> at[2] == 9 && at[1] == 1, Direction.SOUTH);
	}

	private List<Lines.Face> fromIx() {
		return faces(at -> at[2] == 9 && at[1] == 0 && at[0] >= 2, Direction.SOUTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (side == Direction.SOUTH && at[2] == 9 && at[1] == 1) {
			return Product.WELLFIELD_BLEED;   // 16d1: the bleed, one up on its back, to an injection pump and a disposal well
		}
		if (at[1] != 0) {
			return null;
		}
		return side == Direction.SOUTH && at[2] == 9 && at[0] <= 1 ? Product.PREGNANT_LIXIVIANT : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.WELLFIELD_BLEED ? Tuning.HEADER_HOUSE_PRODUCERS * Tuning.ISR_PRODUCER_LPS * Tuning.ISR_BLEED * 60
				: Tuning.HEADER_HOUSE_PRODUCERS * Tuning.ISR_PRODUCER_LPS * Tuning.PLANT_HOLD_SECONDS;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, HeaderHouseBlockEntity h) {
		if (h.plcHold(level)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		// the producers on its well pipe: their pumps run off its drives
		List<IsrWellheadBlockEntity> wells = new ArrayList<>();
		for (Lines.Line line : Lines.lines(level, h.wells())) {
			for (Lines.Port port : line.ports()) {
				if (port.node() instanceof IsrWellheadBlockEntity well && well.producer() && !wells.contains(well) && wells.size() < Tuning.HEADER_HOUSE_PRODUCERS) {
					wells.add(well);
				}
			}
		}
		h.producers = wells.size();
		List<MonitorWellheadBlockEntity> monitors = new ArrayList<>();
		for (Lines.Line line : Lines.lines(level, h.wells())) {
			for (Lines.Port port : line.ports()) {
				if (port.node() instanceof MonitorWellheadBlockEntity m && !monitors.contains(m)) {
					monitors.add(m);
				}
			}
		}
		h.load.setDemand(h.producers * Tuning.ISR_PUMP_WATTS + monitors.size() * Tuning.MONITOR_PUMP_WATTS);
		double strength = h.load.undervoltage() ? 0 : h.load.delivered();
		boolean pumping = h.mode <= RO;
		for (IsrWellheadBlockEntity well : wells) {
			well.power(level, pumping ? strength : 0);
		}
		List<IsrWellheadBlockEntity> injectorsAll = new ArrayList<>();
		for (Lines.Line line : Lines.lines(level, h.injection())) {
			for (BlockPos pipe : line.pipes()) {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					if (level.getBlockEntity(pipe.relative(side)) instanceof IsrWellheadBlockEntity well && !well.producer() && !injectorsAll.contains(well)) {
						injectorsAll.add(well);
					}
				}
			}
		}
		h.ring(level, wells, injectorsAll, monitors);
		if (h.mode != MINE) {
			h.restore(level, injectorsAll);
			h.setChanged();
			return;
		}
		// pregnant lixiviant from the wells, on to the IX columns
		double[] in = new double[5];
		double max = h.room(Product.PREGNANT_LIXIVIANT);
		double got = max > 0.001 ? h.pullMixed(level, h.wells(), Product.PREGNANT_LIXIVIANT, max, in) : 0;
		if (got > 0) {
			h.addMixed(Product.PREGNANT_LIXIVIANT, got, in);
		}
		// barren lixiviant back from the IX columns: oxygen added, the bleed held back, out to the injectors
		double back = h.room(Product.BARREN_LIXIVIANT) / (1 - Tuning.ISR_BLEED);
		double[] barren = new double[5];
		double returned = back > 0.001 ? h.pullMixed(level, h.fromIx(), Product.BARREN_LIXIVIANT, back, barren) : 0;
		OxygenGeneratorBlockEntity oxygen = Touching.find(level, pos, OxygenGeneratorBlockEntity.class, o -> true);
		double share = returned > 0 && oxygen != null ? oxygen.supply(level, returned * Tuning.O2_DOSE_KG_PER_L) : 0;
		Co2StorageTankBlockEntity tank = Touching.find(level, pos, Co2StorageTankBlockEntity.class, t -> true);
		double co2 = 0;
		if (returned > 0) {
			h.bleed += returned * Tuning.ISR_BLEED / 1000;
			if (!Lines.lines(level, h.bleedFaces()).isEmpty()) {
				h.add(Product.WELLFIELD_BLEED, returned * Tuning.ISR_BLEED);   // piped: for the injection pump; what has nowhere to go is only counted
			}
			h.addMixed(Product.BARREN_LIXIVIANT, returned * (1 - Tuning.ISR_BLEED), new double[] {barren[0], share, 0, 0, 0});
		}
		// the injection manifold: a metered share for each injector on its east pipe, up to each one's rate
		List<IsrWellheadBlockEntity> injectors = new ArrayList<>();
		for (Lines.Line line : Lines.lines(level, h.injection())) {
			for (BlockPos pipe : line.pipes()) {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					if (level.getBlockEntity(pipe.relative(side)) instanceof IsrWellheadBlockEntity well && !well.producer() && !injectors.contains(well)) {
						injectors.add(well);
					}
				}
			}
		}
		double injected = 0;
		boolean held = dev.wildcatter.Rules.isrExcursions() && (!h.ringOk || h.stopped);
		if (!injectors.isEmpty() && !held) {
			double each = Math.min(Tuning.ISR_INJECTOR_LPS * seconds(), h.held(Product.BARREN_LIXIVIANT) / injectors.size());
			double oxygenated = h.mixOf(Product.BARREN_LIXIVIANT)[1];
			// 16d1: CO2 from a storage tank against it, 0.5 g/L of what goes to the injectors, dosed into the injection header
			if (tank != null && each > 0) {
				double want = each * injectors.size() * Tuning.CO2_DOSE_KG_PER_L;
				double got2 = tank.take(want);
				h.co2Used += got2;
				co2 = got2 / want;
			}
			for (IsrWellheadBlockEntity well : injectors) {
				well.inject(level, each, oxygenated, co2, seconds());
			}
			injected = each * injectors.size();
			h.held.put(Product.BARREN_LIXIVIANT, h.held(Product.BARREN_LIXIVIANT) - injected);
		}
		h.co2Share = co2;
		String co2Note = tank == null ? "" : injected <= 0 ? "" : tank.co2() < 1e-3 && co2 < 0.01 ? "; NO CO2: its CO2 tank is empty"
				: String.format("; CO2 %.2f g/L (%.0f %% of the dose)", co2 * Tuning.CO2_DOSE_KG_PER_L * 1000, co2 * 100);
		h.status = String.format("%d producers, %d injectors (%.0f kW of pumps, %.0f %% delivered); %.1f L/s in from the wells, %.1f L/s back to the injectors%s; bleed %.1f m3",
				h.producers, injectors.size(), h.producers * Tuning.ISR_PUMP_WATTS / 1000, strength * 100, got / seconds(), returned * (1 - Tuning.ISR_BLEED) / seconds(),
				returned <= 0 ? "" : oxygen == null ? ", NO OXYGEN: set an oxygen generator against it" : share < 0.99 ? String.format(", only %.0f %% oxygenated", share * 100) : " with oxygen",
				h.bleed) + co2Note + h.ringNote;
		if (held) {
			h.status = (!h.ringOk ? "NO MONITOR RING: it will not inject; " : "EXCURSION: injection stopped until the front is pulled back; ") + h.status;
		} else if (h.ringNote.contains("EXCURSION")) {
			h.status = "EXCURSION: " + h.status;
		}
		h.setChanged();
	}

	// ---- 16d2: the monitor ring, the front, restoration ---------------------------------------------------------------------------
	private double bleedShare(Level level) {
		if (bleedForTest >= 0) {
			return bleedForTest;
		}
		boolean piped = !Lines.lines(level, bleedFaces()).isEmpty();
		// a disposal well that stops backs the bleed up: none is held back
		return piped && held(Product.WELLFIELD_BLEED) >= hold(Product.WELLFIELD_BLEED) - 1e-6 ? 0 : Tuning.ISR_BLEED;
	}

	/** The ring of monitor wells round the patterns, the front's spread and the excursion it makes (the rule isrExcursions). */
	private void ring(Level level, List<IsrWellheadBlockEntity> producers, List<IsrWellheadBlockEntity> injectors, List<MonitorWellheadBlockEntity> monitors) {
		List<BlockPos> pattern = new ArrayList<>();
		producers.forEach(w -> pattern.add(w.getBlockPos()));
		injectors.forEach(w -> pattern.add(w.getBlockPos()));
		long now = level.getGameTime();
		if (pattern.isEmpty()) {
			ringNote = "";
			for (MonitorWellheadBlockEntity m : monitors) {
				m.read(now, "no patterns on its header house yet");
			}
			return;
		}
		double cx = 0, cz = 0;
		for (BlockPos p : pattern) {
			cx += p.getX() + 0.5;
			cz += p.getZ() + 0.5;
		}
		cx /= pattern.size();
		cz /= pattern.size();
		int valid = 0;
		double meanR = 0, nearest = Double.MAX_VALUE;
		List<double[]> distances = new ArrayList<>();
		for (MonitorWellheadBlockEntity m : monitors) {
			BlockPos mp = m.getBlockPos();
			double d = Double.MAX_VALUE;
			for (BlockPos p : pattern) {
				d = Math.min(d, Math.sqrt(Math.pow(mp.getX() - p.getX(), 2) + Math.pow(mp.getZ() - p.getZ(), 2)));
			}
			distances.add(new double[] {d});
			if (d >= Tuning.MONITOR_MIN_BLOCKS && d <= Tuning.MONITOR_MAX_BLOCKS) {
				valid++;
				meanR += Math.sqrt(Math.pow(mp.getX() + 0.5 - cx, 2) + Math.pow(mp.getZ() + 0.5 - cz, 2));
				nearest = Math.min(nearest, d);
			}
		}
		meanR = valid > 0 ? meanR / valid : 0;
		int needed = Math.max(Tuning.MONITOR_MIN_WELLS, (int) Math.ceil(2 * Math.PI * meanR / Tuning.MONITOR_SPACING - 1e-9));
		ringOk = valid >= needed;
		// the front: it spreads under 0.5 % bleed, a block a game hour for each 0.1 % short; pumping without injecting pulls it back
		double hours = Tuning.WELL_INTERVAL / 1000.0;
		boolean rule = dev.wildcatter.Rules.isrExcursions();
		if (rule && mode == MINE && !producers.isEmpty() && !injectors.isEmpty() && !stopped) {
			front = Math.max(0, front + (Tuning.FRONT_HOLD_BLEED - bleedShare(level)) / Tuning.FRONT_STEP_BLEED * Tuning.FRONT_BLOCKS_PER_HOUR * hours);
		} else if (rule && (mode == SWEEP || mode == RO || stopped)) {
			front = Math.max(0, front - (Tuning.ISR_BLEED - Tuning.FRONT_HOLD_BLEED) / Tuning.FRONT_STEP_BLEED * Tuning.FRONT_BLOCKS_PER_HOUR * hours);
		}
		boolean out = rule && valid > 0 && front >= nearest;
		excursion = out ? excursion + Tuning.WELL_INTERVAL / 20.0 : 0;
		stopped = out && excursion >= Tuning.EXCURSION_DAYS * 24000 / 20.0;
		if (out) {
			stability = 0;
		}
		for (int i = 0; i < monitors.size(); i++) {
			double d = distances.get(i)[0];
			boolean inRing = d >= Tuning.MONITOR_MIN_BLOCKS && d <= Tuning.MONITOR_MAX_BLOCKS;
			monitors.get(i).read(now, !inRing ? String.format("%.0f blocks from the nearest pattern: OUTSIDE THE RING (%d-%d blocks)", d, Tuning.MONITOR_MIN_BLOCKS, Tuning.MONITOR_MAX_BLOCKS)
					: out && d <= front ? "EXCURSION: chloride, conductivity and alkalinity over their upper control limits"
					: String.format("%.0f blocks from the nearest pattern; chloride, conductivity and alkalinity within their limits", d));
		}
		ringNote = !rule && monitors.isEmpty() ? "" : String.format("; monitor ring %d of %d wells%s; front %.1f blocks out%s", valid, needed, ringOk ? "" : " (NO MONITOR RING)", front,
				valid > 0 ? String.format(", nearest monitor %.0f", nearest) : "") + (out ? String.format("; EXCURSION for %.1f game days", excursion * 20 / 24000) : "");
	}

	/** Pore volume under its patterns, m3: a tenth of a 20 x 20 x 4 block pattern at 0.3 porosity a producer. */
	public double poreVolume() {
		return Math.max(1, producers) * Tuning.PORE_M3_PER_PRODUCER;
	}

	/** The restoration modes: SWEEP and RO pump, STABILITY waits two game days without an excursion, RESTORED is done. */
	private void restore(Level level, List<IsrWellheadBlockEntity> injectors) {
		String note = ringNote;
		switch (mode) {
			case SWEEP -> {
				double got = pullMixed(level, wells(), Product.PREGNANT_LIXIVIANT, Tuning.HEADER_HOUSE_PRODUCERS * Tuning.ISR_PRODUCER_LPS * seconds(), new double[5]);
				if (!Lines.lines(level, bleedFaces()).isEmpty()) {
					add(Product.WELLFIELD_BLEED, Math.min(got, room(Product.WELLFIELD_BLEED)));
				}
				swept += got / 1000;
				if (swept >= poreVolume() - 1e-6) {
					mode = RO;
				}
				status = String.format("SWEEP: pumping the producers to disposal, injectors off: %.1f of %.0f m3 (one pore volume)", swept, poreVolume()) + note;
			}
			case RO -> {
				RoUnitBlockEntity ro = Touching.find(level, worldPosition, RoUnitBlockEntity.class, r -> true);
				if (ro == null) {
					status = "RO: NO RO UNIT: set an RO UNIT against it" + note;
					return;
				}
				double cap = ro.treat(level, Tuning.RO_LPS * seconds());
				double got = cap > 0 ? pullMixed(level, wells(), Product.PREGNANT_LIXIVIANT, cap, new double[5]) : 0;
				double permeate = got * Tuning.RO_RECOVERY;
				if (!injectors.isEmpty()) {
					for (IsrWellheadBlockEntity well : injectors) {
						well.inject(level, permeate / injectors.size(), 0, 0, seconds());
					}
				}
				if (!Lines.lines(level, bleedFaces()).isEmpty()) {
					add(Product.WELLFIELD_BLEED, Math.min(got - permeate, room(Product.WELLFIELD_BLEED)));
				}
				treated += got / 1000;
				if (treated >= Tuning.RO_PORE_VOLUMES * poreVolume() - 1e-6) {
					mode = STABILITY;
				}
				status = String.format("RO: %.1f of %.0f m3 through the RO unit (two pore volumes), permeate to the injectors", treated, Tuning.RO_PORE_VOLUMES * poreVolume()) + note;
			}
			case STABILITY -> {
				stability += Tuning.WELL_INTERVAL / 20.0;
				double days = stability * 20 / 24000;
				if (days >= Tuning.STABILITY_DAYS - 1e-9) {
					mode = RESTORED;
				}
				status = String.format("STABILITY: monitoring, %.2f of %.0f game days without an excursion", days, Tuning.STABILITY_DAYS) + note;
			}
			default -> status = "RESTORED: the wellfield's groundwater is back to its baseline; nothing pumps" + note;
		}
	}

	/** A sneak-click: MINE to SWEEP (the restoration then runs on by itself); otherwise its progress. */
	public String nextMode() {
		if (mode == MINE) {
			mode = SWEEP;
			swept = treated = stability = 0;
			setChanged();
			return "Header house: SWEEP: the producers pump to disposal and the injectors stop; RO and STABILITY follow";
		}
		if (mode == RESTORED) {
			mode = MINE;
			setChanged();
			return "Header house: back to MINE";
		}
		return "Header house: " + MODES[mode] + " (restoration runs on by itself)";
	}

	public int mode() {
		return mode;
	}

	public double front() {
		return front;
	}

	public boolean ringOk() {
		return ringOk;
	}

	public boolean stoppedByExcursion() {
		return stopped;
	}

	/** For the game test. */
	public void setBleedForTest(double share) {
		bleedForTest = share;
	}

	public void setFrontForTest(double blocks) {
		front = blocks;
	}

	public void setExcursionForTest(double seconds) {
		excursion = seconds;
	}

	public void setRestorationForTest(double sweptM3, double treatedM3, double stabilitySeconds) {
		swept = sweptM3;
		treated = treatedM3;
		stability = stabilitySeconds;
	}

	// ---- its 480 V supply: the drives ----------------------------------------------------------------------------------------
	private Direction boxFace() {
		return LeasePartBlock.world(getBlockState(), Direction.NORTH);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == boxFace() && voltage == Voltage.INDUSTRIAL && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		load.setDemand(producers * Tuning.ISR_PUMP_WATTS);
		load.build(circuit, worldPosition, List.of(boxFace()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		load.apply(circuit, ticks);
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Header house: " + status + "; drives " + load.describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("producers", producers);
		output.putDouble("bleed", bleed);
		output.putInt("mode", mode);
		output.putDouble("front", front);
		output.putDouble("excursion", excursion);
		output.putDouble("swept", swept);
		output.putDouble("treated", treated);
		output.putDouble("stability", stability);
		output.putDouble("co2Used", co2Used);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		producers = input.getIntOr("producers", 0);
		bleed = input.getDoubleOr("bleed", 0);
		mode = input.getIntOr("mode", 0);
		front = input.getDoubleOr("front", 0);
		excursion = input.getDoubleOr("excursion", 0);
		swept = input.getDoubleOr("swept", 0);
		treated = input.getDoubleOr("treated", 0);
		stability = input.getDoubleOr("stability", 0);
		co2Used = input.getDoubleOr("co2Used", 0);
	}
}
