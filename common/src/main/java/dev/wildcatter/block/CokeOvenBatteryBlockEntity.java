package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A battery of 13 by-product slot ovens (docs/multiblocks/coke-ovens.md), 15 t charges coked for 18 h; larry car 30 kW at 480 V.
 * It cokes coal from the coal tower it touches while the pusher machine and quench tower it touches run: 0.75 kg of coke a kg of
 * coal to the quench tower, and 0.40 m3 of raw gas (with its tar) out of the south face of its back cells to the by-product plant,
 * which sends 40 % back to underfire it.
 */
public class CokeOvenBatteryBlockEntity extends UnitMotorBlockEntity {
	private double coked;   // t of coal
	private String unit = "starting";

	public CokeOvenBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COKE_OVEN_BATTERY_ENTITY, pos, state);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return side == Direction.SOUTH && at[2] == 15 && at[1] == 0 ? Product.RAW_COKE_OVEN_GAS : null;
	}

	@Override
	protected double hold(Product product) {
		return Tuning.COKE_OVEN_COAL_KGS * Tuning.CARBON_CATCH_UP * Tuning.COKE_OVEN_GAS_PER_KG * Tuning.PLANT_HOLD_SECONDS;
	}

	@Override
	protected void work(Level level) {
		CoalTowerBlockEntity tower = Touching.find(level, worldPosition, CoalTowerBlockEntity.class, t -> true);
		UnitMotorBlockEntity pusher = Touching.find(level, worldPosition, UnitMotorBlockEntity.class, u -> u.kind().equals("pusher_machine"));
		CokeQuenchTowerBlockEntity quench = Touching.find(level, worldPosition, CokeQuenchTowerBlockEntity.class, q -> true);
		if (tower == null || pusher == null || quench == null) {
			unit = "NOT BUILT: set it against the " + (tower == null ? "coal tower" : pusher == null ? "pusher machine" : "quench tower");
			return;
		}
		if (tower.coal() < 0.01) {
			unit = "NO COAL: put a hopper of coal or coal blocks on the coal tower";
			return;
		}
		if (quench.water() < 1) {
			unit = "NO WATER: pump quench water to the quench tower";
			return;
		}
		double max = Math.min(Tuning.COKE_OVEN_COAL_KGS * Tuning.CARBON_CATCH_UP * seconds(), tower.coal());
		max = Math.min(max, quench.room() / Tuning.COKE_OVEN_COKE_YIELD);
		if (max <= 1e-7) {
			unit = "FULL: take the coke from the quench tower";
			return;
		}
		max = Math.min(max, room(Product.RAW_COKE_OVEN_GAS) / Tuning.COKE_OVEN_GAS_PER_KG);
		if (max <= 1e-7) {
			unit = "idle: its gas has nowhere to go (pipe its back end to a by-product plant)";
			return;
		}
		double strength = Math.min(Math.min(drive(level), pusher.drive(level)), Math.min(quench.drive(level), tower.drive(level)));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; pusher " + pusher.motor().describe() + "; quench " + quench.motor().describe() + "; coal tower " + tower.motor().describe();
			return;
		}
		double kg = max * strength;
		tower.takeCoal(kg);
		quench.quench(kg * Tuning.COKE_OVEN_COKE_YIELD);
		add(Product.RAW_COKE_OVEN_GAS, kg * Tuning.COKE_OVEN_GAS_PER_KG);
		coked += kg / 1000;
		unit = String.format("coking at 1,100 C: %.2f kg/s of coal, %.2f kg/s of coke, %.2f m3/s of raw gas", kg / seconds(), kg * Tuning.COKE_OVEN_COKE_YIELD / seconds(),
				kg * Tuning.COKE_OVEN_GAS_PER_KG / seconds());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Coke oven battery (13 slot ovens): %s; %.1f t of coal coked", unit, coked) + heldReadout() + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("coked", coked);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		coked = input.getDoubleOr("coked", 0);
	}
}
