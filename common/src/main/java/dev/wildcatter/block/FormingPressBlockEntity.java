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
 * The forming press (docs/multiblocks/anode-plant.md): a vibro-compactor, 110 kW at 480 V. It presses a 1,275 kg green anode each
 * time the paste plant it touches holds that much paste; the anodes go out to a hopper under it, or to an empty hand.
 */
public class FormingPressBlockEntity extends FillerBlockEntity {
	private int pressed;
	private String unit = "starting";

	public FormingPressBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.FORMING_PRESS_ENTITY, pos, state);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	@Override
	protected void work(Level level) {
		PastePlantBlockEntity plant = Touching.find(level, worldPosition, PastePlantBlockEntity.class, p -> true);
		if (plant == null) {
			unit = "NOT BUILT: set it against the paste plant";
			return;
		}
		ItemStack anode = new ItemStack(WildcatterItems.GREEN_ANODE);
		if (plant.paste() < Tuning.GREEN_ANODE_KG) {
			unit = String.format("NO PASTE: %.0f/%.0f kg of paste for an anode", plant.paste(), Tuning.GREEN_ANODE_KG);
			return;
		}
		if (!roomOut(anode)) {
			unit = "FULL: take the anodes (empty hand, or a hopper under it)";
			return;
		}
		if (drive(level) <= 0) {
			unit = "starting: " + describe();
			return;
		}
		plant.takePaste(Tuning.GREEN_ANODE_KG);
		putOut(anode);
		pressed++;
		unit = "pressed a green anode";
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Forming press (vibro-compactor): %s; %d anodes pressed, %d waiting; ", unit, pressed, outCount()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("pressed", pressed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		pressed = input.getIntOr("pressed", 0);
	}
}
