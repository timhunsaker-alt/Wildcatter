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
	/** kg of coal coking in the ovens, with the world rule realResidenceTimes (18 h); empty without it. */
	private final dev.wildcatter.fluid.Holdup ovens = new dev.wildcatter.fluid.Holdup(1800);
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

	private double pushedKg;   // kg of coke pushed out of the hold, all told

	/** kg of coke pushed from the rule's hold, all told. */
	public double pushedFromHold() {
		return pushedKg;
	}

	/** kg of coal in the ovens now (the rule's hold). */
	public double inOvens() {
		return ovens.inProcess();
	}

	/** Game test hook: as if {@code seconds} of coking had passed. */
	public void ageForTest(double seconds) {
		ovens.age(seconds);
		setChanged();
	}

	@Override
	protected void work(Level level) {
		if (dev.wildcatter.Rules.realResidenceTimes() || !ovens.isEmpty()) {
			held(level);
			return;
		}
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

	/**
	 * With the world rule realResidenceTimes (plant-crane-gaps #11): each charge cokes for its real 18 h before it is pushed to the
	 * quench tower; the battery charges until its ovens hold 18 h of coal, so at steady state the rate is the same. Its gas comes off as
	 * it cokes, as before. With the rule turned off again, what is in the ovens is pushed as the quench tower takes it.
	 */
	private void held(Level level) {
		boolean hold = dev.wildcatter.Rules.realResidenceTimes();
		double now = level.getGameTime() / 20.0;
		CoalTowerBlockEntity tower = Touching.find(level, worldPosition, CoalTowerBlockEntity.class, t -> true);
		UnitMotorBlockEntity pusher = Touching.find(level, worldPosition, UnitMotorBlockEntity.class, u -> u.kind().equals("pusher_machine"));
		CokeQuenchTowerBlockEntity quench = Touching.find(level, worldPosition, CokeQuenchTowerBlockEntity.class, q -> true);
		if (tower == null || pusher == null || quench == null) {
			unit = "NOT BUILT: set it against the " + (tower == null ? "coal tower" : pusher == null ? "pusher machine" : "quench tower");
			return;
		}
		if (quench.water() < 1) {
			unit = "NO WATER: pump quench water to the quench tower";
			return;
		}
		double strength = Math.min(Math.min(drive(level), pusher.drive(level)), Math.min(quench.drive(level), tower.drive(level)));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; pusher " + pusher.motor().describe() + "; quench " + quench.motor().describe() + "; coal tower " + tower.motor().describe();
			return;
		}
		// push what has coked its 18 h (all of it, with the rule off) as the quench tower takes it
		double room = quench.room() / Tuning.COKE_OVEN_COKE_YIELD;
		double pushed = hold ? ovens.take(now, room) : ovens.takeAny(room);
		quench.quench(pushed * Tuning.COKE_OVEN_COKE_YIELD);
		pushedKg += pushed * Tuning.COKE_OVEN_COKE_YIELD;
		double capacity = hold ? Tuning.COKE_OVEN_COAL_KGS * Tuning.COKING_HOLD_S : 0;
		double max = Math.min(Tuning.COKE_OVEN_COAL_KGS * Tuning.CARBON_CATCH_UP * seconds(), Math.min(tower.coal(), capacity - ovens.inProcess()));
		max = Math.min(max, room(Product.RAW_COKE_OVEN_GAS) / Tuning.COKE_OVEN_GAS_PER_KG);
		double kg = Math.max(0, max) * strength;
		if (kg > 1e-7) {
			tower.takeCoal(kg);
			ovens.add(now, Tuning.COKING_HOLD_S, kg);
			add(Product.RAW_COKE_OVEN_GAS, kg * Tuning.COKE_OVEN_GAS_PER_KG);
			coked += kg / 1000;
		}
		double next = ovens.untilNext(now);
		unit = String.format("coking at 1,100 C (18 h a charge): %.1f t of coal in the ovens, %.2f kg/s charged, %.2f kg/s of coke pushed", ovens.inProcess() / 1000,
				kg / seconds(), pushed * Tuning.COKE_OVEN_COKE_YIELD / seconds())
				+ (next > 0 ? String.format("; the next push in %.1f h", next / 3600) : "")
				+ (tower.coal() < 0.01 && kg <= 1e-7 ? "; NO COAL on the coal tower" : "");
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Coke oven battery (13 slot ovens): %s; %.1f t of coal coked", unit, coked) + heldReadout() + "; " + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("coked", coked);
		output.putString("ovens", ovens.save());
		output.putDouble("pushedKg", pushedKg);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		coked = input.getDoubleOr("coked", 0);
		ovens.load(input.getStringOr("ovens", ""));
		pushedKg = input.getDoubleOr("pushedKg", 0);
	}
}
