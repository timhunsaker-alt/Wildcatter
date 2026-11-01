package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Conversion;
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
 * The fluorine cell house (docs/multiblocks/conversion.md): 12 KF·2HF cells in series, 9 V and 6 kA each, from the rectifier
 * touching it. Anhydrous HF in on the north face of its front cells; fluorine out of the south face of its back cells, 95 % of
 * Faraday's 0.709 kg a kAh. Its cooling tower's make-up water comes off a pump; the cathode hydrogen goes up its vent.
 */
public class FluorineCellHouseBlockEntity extends PlantUnitBlockEntity implements WaterUser {
	private double water;
	private double share;

	public FluorineCellHouseBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FLUORINE_CELL_HOUSE_ENTITY, pos, state);
	}

	private List<Lines.Face> hf() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 11 && at[1] == 0 && side == Direction.SOUTH ? Product.FLUORINE : null;
	}

	@Override
	protected double hold(Product product) {
		return Conversion.fluorineKgs() * Tuning.PLANT_HOLD_SECONDS * 4;
	}

	@Override
	public double waterLevel() {
		return water / 400.0;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, 400.0 - water));
		water += fits;
		setChanged();
		return fits;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FluorineCellHouseBlockEntity h) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		RectifierBlockEntity rectifier = Touching.find(level, pos, RectifierBlockEntity.class, r -> true);
		double waiting = Lines.available(level, h.hf(), Product.ANHYDROUS_HF, Pull.MACHINE, h) * Tuning.AHF_KG_PER_L;
		boolean work = rectifier != null && waiting > 1e-6 && h.water > 0.01 && h.room(Product.FLUORINE) > 1e-6;
		h.share = rectifier == null ? 0 : rectifier.supply(work ? Conversion.dcWatts() : 0, Conversion.dcVolts());
		if (rectifier == null) {
			h.status = "NO DC: set a RECTIFIER against it";
		} else if (waiting <= 1e-6) {
			h.status = "NO HF: pipe anhydrous HF to the north face of its front";
		} else if (h.water <= 0.01) {
			h.status = "NO COOLING WATER: pump water to it";
		} else if (!work) {
			h.status = "idle: its fluorine has nowhere to go (pipe its back end to a fluorinator)";
		} else {
			double f2 = Math.min(Math.min(Conversion.fluorineKgs() * seconds() * h.share, h.room(Product.FLUORINE)), waiting / Tuning.F2_HF_PER_F2);
			Lines.pull(level, h.hf(), Product.ANHYDROUS_HF, f2 * Tuning.F2_HF_PER_F2 / Tuning.AHF_KG_PER_L, Pull.MACHINE, h);
			h.water = Math.max(0, h.water - Tuning.F2_COOLING_LPS * seconds() * h.share);
			h.add(Product.FLUORINE, f2);
			h.status = f2 > 1e-7 ? String.format("%d cells at %.1f V and %.2f kA: %.1f kg/h of fluorine", Tuning.F2_CELLS, Tuning.F2_CELL_VOLTS,
					Tuning.F2_CELL_KA * h.share, f2 / seconds() * 3600) : "starting: waiting for DC from the rectifier";
		}
		h.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Fluorine cell house (12 KF·2HF cells in series): " + status + String.format("; cooling water %.0f/400 L", water));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("water", water);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getDoubleOr("water", 0);
	}
}
