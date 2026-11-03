package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The coke guide, quench car and wet quench tower (docs/multiblocks/coke-ovens.md) on a coke oven battery's east side: quench pump
 * 55 kW at 480 V, on water off a pump (0.45 L evaporated a kg of coke). Metallurgical coke goes out in 112.5 kg pieces to a hopper
 * under it, or to an empty hand.
 */
public class CokeQuenchTowerBlockEntity extends FillerBlockEntity implements WaterUser {
	private double coke;    // kg quenched, toward a piece
	private double water;   // litres
	private int pieces;

	public CokeQuenchTowerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COKE_QUENCH_TOWER_ENTITY, pos, state);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	@Override
	public double waterLevel() {
		return water / Tuning.CARBON_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.CARBON_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	/** kg of coke it can still take: room toward its pieces, and water to quench it. */
	public double room() {
		return Math.max(0, Math.min(Tuning.CARBON_HOLD_KG - coke, water / Tuning.QUENCH_WATER_PER_KG));
	}

	public double water() {
		return water;
	}

	public void quench(double kg) {
		coke += kg;
		water = Math.max(0, water - kg * Tuning.QUENCH_WATER_PER_KG);
		setChanged();
	}

	@Override
	protected void work(Level level) {
		while (coke >= Tuning.MET_COKE_KG && putOut(new ItemStack(WildcatterItems.MET_COKE))) {
			coke -= Tuning.MET_COKE_KG;
			pieces++;
		}
	}

	@Override
	public Component inspect() {
		String state = water < 1 ? "NO WATER: pump quench water to it" : coke >= Tuning.CARBON_HOLD_KG - Tuning.MET_COKE_KG ? "FULL: take the coke (empty hand, or a hopper under it)"
				: "quenching";
		return Component.literal(String.format("Coke quench tower: %s; %d pieces of coke made, %d waiting; water %.0f/%.0f L; ", state, pieces, outCount(), water,
				Tuning.CARBON_WATER_HOLD) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("coke", coke);
		output.putDouble("water", water);
		output.putInt("pieces", pieces);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		coke = input.getDoubleOr("coke", 0);
		water = input.getDoubleOr("water", 0);
		pieces = input.getIntOr("pieces", 0);
	}
}
