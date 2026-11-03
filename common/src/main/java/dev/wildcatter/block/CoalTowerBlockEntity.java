package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The coal tower (docs/multiblocks/coke-ovens.md): the blending and charging bunker and its conveyor, 15 kW at 480 V. Coal (150 kg)
 * or coal blocks (1,350 kg) by a hopper on top, up to 10 t; it feeds the battery it touches, which runs it.
 */
public class CoalTowerBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double coal;

	public CoalTowerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COAL_TOWER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(Items.COAL) || stack.is(Items.COAL_BLOCK);
	}

	public double coal() {
		return coal;
	}

	public void takeCoal(double kg) {
		coal = Math.max(0, coal - kg);
		setChanged();
	}

	@Override
	protected void work(Level level) {
		ItemStack next = slots.get(0);
		double kg = next.is(Items.COAL_BLOCK) ? Tuning.COAL_KG * 9 : Tuning.COAL_KG;
		while (!next.isEmpty() && coal + kg <= Tuning.COAL_TOWER_HOLD_KG) {
			useOne();
			coal += kg;
		}
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Coal tower: %,.0f kg of coal in its bunker, %d more in its hopper slot; ", coal, fed()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("coal", coal);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		coal = input.getDoubleOr("coal", 0);
	}
}
