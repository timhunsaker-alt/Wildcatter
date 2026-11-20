package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Frasch water heater (docs/multiblocks/oilfield-follow-ons.md, 23d): a pressurised hot water plant. Water from a water pump,
 * softened and heated to 165 C at 2.8 MPa by gas burners (pipeline gas or LPG on the east face of its bottom cells, 3.4 MW at a well's
 * full rate), sent down a Frasch wellhead's outer string by its charge pump (75 kW at industrial LV). The wellhead finds it by steel
 * pipe and asks it for water each interval.
 */
public class FraschWaterHeaterBlockEntity extends UnitMotorBlockEntity implements WaterUser {
	private double water;
	private double temp = Tuning.FRASCH_COLD_C;
	private double sent;
	private String unit = "idle: no Frasch wellhead asking for water";

	public FraschWaterHeaterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FRASCH_WATER_HEATER_ENTITY, pos, state);
	}

	private List<Lines.Face> burners() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	public double waterLevel() {
		return water / Tuning.FRASCH_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.FRASCH_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	public double temp() {
		return temp;
	}

	/** The wellhead wants {@code kg} of hot water this interval; returns what went down, at 150 C or hotter. */
	public double supply(Level level, double kg) {
		double strength = drive(level);
		if (strength <= 0) {
			unit = "NO PUMP: " + describe();
			temp = Math.max(Tuning.FRASCH_COLD_C, temp - 1);
			return 0;
		}
		double flow = Math.min(kg * strength, water);
		if (flow <= 1e-9) {
			unit = "NO WATER: pump water to it";
			return 0;
		}
		double need = flow * Tuning.FRASCH_WATER_J_PER_KG;
		double fired = GasFiring.fire(level, burners(), need, this);
		double share = Math.min(1, fired / need);
		temp = Tuning.FRASCH_COLD_C + (Tuning.FRASCH_HOT_C - Tuning.FRASCH_COLD_C) * share;
		water -= flow;
		sent += flow;
		setChanged();
		if (temp < Tuning.FRASCH_MIN_C) {
			unit = String.format("NO FUEL: the water only reaches %.0f C (pipe gas to the east side of its bottom)", temp);
			return 0;
		}
		unit = String.format("heating %.1f m3/h of water to %.0f C at 2.8 MPa on %.1f MW of gas", flow / seconds() * 3.6, temp, fired / seconds() / 1e6);
		return flow;
	}

	@Override
	protected void work(Level level) {
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Frasch water heater (165 C, 2.8 MPa; charge pump %.0f kW): %s; water %.0f of %.0f L, %.0f m3 sent down; ", Tuning.FRASCH_CHARGE_PUMP_WATTS / 1000,
				unit, water, Tuning.FRASCH_WATER_HOLD, sent / 1000) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("water", water);
		output.putDouble("temp", temp);
		output.putDouble("sent", sent);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		water = input.getDoubleOr("water", 0);
		temp = input.getDoubleOr("temp", Tuning.FRASCH_COLD_C);
		sent = input.getDoubleOr("sent", 0);
	}
}
