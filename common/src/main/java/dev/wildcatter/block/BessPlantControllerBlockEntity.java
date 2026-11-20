package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.control.ControlNet;
import dev.wildcatter.control.Remote;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A battery plant's controller (docs/multiblocks/grid-batteries.md, 22b): the control house with the power plant controller, SCADA, its
 * station battery and HMI. It finds up to four PCSs by control cable (each on its own run, as the PLC network walks) and runs them as one:
 * the mode, the plant's setpoint shared by rating, peak shaving shared, frequency response, and the black start permit. Its PLC points are
 * the plant's (the sums of its blocks).
 */
public class BessPlantControllerBlockEntity extends BlockEntity implements Inspectable, BigPartEntity, ControlDevice {
	private final Remote remote = new Remote(8);
	private BessInverterBlockEntity.Preset preset = BessInverterBlockEntity.Preset.STOPPED;
	private double pSet;
	private double peakLimit;
	private boolean frequencyResponse = true;
	private final List<BlockPos> blocks = new ArrayList<>();
	private boolean grown;

	public BessPlantControllerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.BESS_PLANT_CONTROLLER_ENTITY, pos, state);
	}

	public List<BessPcsBlockEntity> pcs(Level level) {
		List<BessPcsBlockEntity> out = new ArrayList<>();
		for (BlockPos at : blocks) {
			if (level.getBlockEntity(at) instanceof BessPcsBlockEntity pcs) {
				out.add(pcs);
			}
		}
		return out;
	}

	public BessInverterBlockEntity.Preset preset() {
		return preset;
	}

	public void setPreset(BessInverterBlockEntity.Preset value) {
		preset = value;
		pSet = switch (value) {
			case CHARGE -> -rated();
			case DISCHARGE -> rated();
			default -> 0;
		};
		if (value == BessInverterBlockEntity.Preset.PEAK_SHAVE && peakLimit <= 0) {
			peakLimit = rated();
		}
		setChanged();
	}

	public void setSetpoint(double watts) {
		pSet = watts;
	}

	public void setFrequencyResponse(boolean on) {
		frequencyResponse = on;
	}

	public void setPeakLimit(double watts) {
		peakLimit = watts;
	}

	private double rated() {
		return level == null ? 0 : pcs(level).size() * Tuning.PCS_WATTS;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BessPlantControllerBlockEntity plant) {
		long now = level.getGameTime();
		if (now % 20 == 0) {
			plant.find(level);
		}
		if (now % 5 == 0) {
			plant.run(level, now);
		}
	}

	private void find(Level level) {
		blocks.clear();
		ControlNet.Found found = ControlNet.walk(level, BigParts.cellsAt(level, worldPosition));
		for (BlockPos at : found.devices()) {
			if (level.getBlockEntity(at) instanceof BessPcsBlockEntity && blocks.size() < Tuning.PLANT_MAX_BLOCKS) {
				blocks.add(at.immutable());
			}
		}
	}

	private void run(Level level, long now) {
		if (remote.auto()) {
			BessInverterBlockEntity.Preset want;
			if (!remote.on(BessInverterBlockEntity.RUN, now)) {
				want = BessInverterBlockEntity.Preset.STOPPED;
			} else if (remote.on(BessInverterBlockEntity.FORMING, now)) {
				want = BessInverterBlockEntity.Preset.FORMING;
			} else if (remote.on(BessInverterBlockEntity.PEAK_SHAVE, now)) {
				want = BessInverterBlockEntity.Preset.PEAK_SHAVE;
			} else {
				want = BessInverterBlockEntity.Preset.IDLE;
			}
			preset = want;
			if (want == BessInverterBlockEntity.Preset.IDLE) {
				pSet = (remote.fraction(BessInverterBlockEntity.P_SET, now, 0.5) - 0.5) * 2 * rated();
			}
			peakLimit = remote.fraction(BessInverterBlockEntity.PEAK_LIMIT, now, rated() > 0 ? peakLimit / (4 * rated()) : 0) * 4 * rated();
			frequencyResponse = remote.on(BessInverterBlockEntity.FREQ_RESPONSE, now) || !remote.fresh(BessInverterBlockEntity.FREQ_RESPONSE, now);
		}
		List<BessPcsBlockEntity> all = pcs(level);
		boolean permit = !remote.auto() || remote.on(BessInverterBlockEntity.BLACK_START, now);
		for (BessPcsBlockEntity p : all) {
			p.runFromPlant(now, preset == BessInverterBlockEntity.Preset.FORMING && !permit && !p.breakerClosed() ? BessInverterBlockEntity.Preset.STOPPED : preset,
					pSet / all.size(), peakLimit, frequencyResponse, 1.0 / all.size());
		}
	}

	// ---- PLC points: the plant's, the same list as a PCS's ---------------------------------------------------------------------------
	@Override
	public String deviceName() {
		return "Battery plant controller";
	}

	@Override
	public List<Point> points() {
		return BessInverterBlockEntity.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		if (level == null) {
			return false;
		}
		if (point == 23) {
			return remote.auto();
		}
		if (point == 14) {
			return preset != BessInverterBlockEntity.Preset.STOPPED;
		}
		for (BessPcsBlockEntity p : pcs(level)) {
			if (p.readPoint(point)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public double readMa(int point) {
		if (level == null) {
			return 4;
		}
		List<BessPcsBlockEntity> all = pcs(level);
		if (all.isEmpty()) {
			return 4;
		}
		double sum = 0;
		for (BessPcsBlockEntity p : all) {
			sum += p.readMa(point);
		}
		return sum / all.size();   // each PCS's loop is on its own rating, so the plant's is their mean
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		boolean rose = remote.command(point, on, gameTime);
		if (point == BessInverterBlockEntity.RUN && rose && level != null) {
			for (BessPcsBlockEntity p : pcs(level)) {
				if (!p.tripped().isEmpty()) {
					p.reset();
				}
			}
		}
	}

	@Override
	public void commandMa(int point, double milliamps, long gameTime) {
		remote.commandMa(point, milliamps, gameTime);
	}

	@Override
	public java.util.Map<BlockPos, String> hoaPositions() {
		return java.util.Map.of(worldPosition, remote.hoa().name());
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	public Component sneakClick() {
		if (remote.auto()) {
			return Component.literal("Battery plant controller: in AUTO, run by its PLC");
		}
		setPreset(preset.next());
		return Component.literal("Battery plant controller: " + preset.label);
	}

	@Override
	public Component inspect() {
		if (level == null) {
			return Component.empty();
		}
		List<BessPcsBlockEntity> all = pcs(level);
		StringBuilder b = new StringBuilder(String.format("Battery plant controller: %s%s; %d block%s by control cable", preset.label, remote.auto() ? " (AUTO)" : "",
				all.size(), all.size() == 1 ? "" : "s"));
		double watts = 0;
		double kwh = 0;
		for (BessPcsBlockEntity p : all) {
			watts += p.gridWatts();
			kwh += p.energyKwh(level);
		}
		b.append(String.format("; %s %.2f MW, %,.0f kWh stored", watts >= 0 ? "delivering" : "charging", Math.abs(watts) / 1e6, kwh));
		if (all.isEmpty()) {
			b.append(" (run a control cable from it to each PCS)");
		}
		return Component.literal(b.toString());
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		remote.save(output);
		output.putBoolean("grown", grown);
		output.putString("preset", preset.name());
		output.putDouble("pSet", pSet);
		output.putDouble("peakLimit", peakLimit);
		output.putBoolean("freqResponse", frequencyResponse);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		remote.load(input);
		grown = input.getBooleanOr("grown", grown);
		try {
			preset = BessInverterBlockEntity.Preset.valueOf(input.getStringOr("preset", "STOPPED"));
		} catch (IllegalArgumentException e) {
			preset = BessInverterBlockEntity.Preset.STOPPED;
		}
		pSet = input.getDoubleOr("pSet", 0);
		peakLimit = input.getDoubleOr("peakLimit", 0);
		frequencyResponse = input.getBooleanOr("freqResponse", true);
	}
}
