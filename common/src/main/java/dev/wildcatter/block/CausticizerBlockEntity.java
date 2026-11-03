package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The causticizer (docs/multiblocks/bayer-refinery.md): a lime slaker and causticization tank, Na2CO3 + Ca(OH)2 -> 2 NaOH + CaCO3,
 * slaker and agitator 15 kW at 480 V. Quicklime bags by a hopper on top; it doses the evaporator it touches, which runs it.
 */
public class CausticizerBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double lime;   // kg of CaO tipped in

	public CausticizerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CAUSTICIZER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.QUICKLIME_BAG);
	}

	/** kg of quicklime it can give, tipping a bag in when it runs low. */
	public double lime() {
		if (lime < 1 && fed() > 0) {
			useOne();
			lime += Tuning.BULK_BAG_KG;
			setChanged();
		}
		return lime;
	}

	public void takeLime(double kg) {
		lime = Math.max(0, lime - kg);
		setChanged();
	}

	@Override
	protected void work(Level level) {
		lime();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Causticizer: %,.0f kg of quicklime and %d bags; ", lime, fed()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("lime", lime);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		lime = input.getDoubleOr("lime", 0);
	}
}
