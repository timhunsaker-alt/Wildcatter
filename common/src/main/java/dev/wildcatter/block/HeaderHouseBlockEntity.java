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
		h.load.setDemand(h.producers * Tuning.ISR_PUMP_WATTS);
		double strength = h.load.undervoltage() ? 0 : h.load.delivered();
		for (IsrWellheadBlockEntity well : wells) {
			well.power(level, strength);
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
		if (!injectors.isEmpty()) {
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
				h.bleed) + co2Note;
		h.setChanged();
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
		output.putDouble("co2Used", co2Used);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		producers = input.getIntOr("producers", 0);
		bleed = input.getDoubleOr("bleed", 0);
		co2Used = input.getDoubleOr("co2Used", 0);
	}
}
