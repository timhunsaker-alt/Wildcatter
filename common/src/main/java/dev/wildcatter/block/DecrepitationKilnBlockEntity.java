package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The decrepitation kiln (docs/multiblocks/lithium.md): a direct-fired rotary kiln at 1,050 C with its cooler, turning alpha-spodumene into
 * the reactive beta form. Spodumene by a hopper on top; gas or LPG on the east face of its back cells (z 8-11, past the roast kiln), about
 * 1 GJ a tonne. It hands beta-spodumene to the acid roast kiln it touches. Drive and fans 30 kW at the world's industrial voltage, the box
 * on the west face of its main cell. The lithium leach runs it.
 */
public class DecrepitationKilnBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	double ore;     // kg of spodumene opened
	String unit = "idle: the leach is not running it";

	public DecrepitationKilnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DECREPITATION_KILN_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.SPODUMENE));
	}

	List<Lines.Face> gasIn() {
		return faces(at -> at[0] == 2 && at[2] >= 8 && at[1] == 0, Direction.EAST);
	}

	/** Opens spodumene from its bin until it holds {@code kg}. */
	void open(double kg) {
		while (ore < kg && fed() > 0) {
			useOne();
			ore += Ore.SPODUMENE.blockKg();
		}
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Decrepitation kiln (%.0f kW, %s): ", Tuning.DECREPITATION_KILN_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; spodumene %,.0f kg and %d blocks; ", ore, fed()) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("ore", ore);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		ore = input.getDoubleOr("ore", 0);
	}
}
