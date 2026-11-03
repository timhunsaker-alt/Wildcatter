package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The rotary coke cooler (docs/multiblocks/delayed-coker.md): drive 30 kW at 480 V. It takes the calcining kiln's coke, which runs
 * it, and a bagging station set against it fills 1 t bags of calcined coke.
 */
public class CokeCoolerBlockEntity extends UnitMotorBlockEntity implements Bagged {
	private double coke;

	public CokeCoolerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COKE_COOLER_ENTITY, pos, state);
	}

	public double room() {
		return Math.max(0, Tuning.CARBON_HOLD_KG - coke);
	}

	/** Calcined coke from the kiln, or for the game test as if from it. */
	public void addCoke(double kg) {
		coke += kg;
		setChanged();
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.CALCINED_COKE_BAG;
	}

	@Override
	public double bagReady() {
		return coke;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, coke);
		coke -= got;
		setChanged();
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Coke cooler (rotary): %,.0f kg of calcined coke held%s; ", coke, coke >= Tuning.CARBON_HOLD_KG - 1 ? " (FULL: set a bagging station against it)" : "")
				+ describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("coke", coke);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		coke = input.getDoubleOr("coke", 0);
	}
}
