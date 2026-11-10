package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The potash dryer (docs/multiblocks/fertilizer.md): a fluid-bed dryer and compaction against the vacuum crystallizer, run by the
 * potash dissolver. Its air heater burns gas or LPG at the south face of its bottom cells, 0.6 MW at the rating; 75 kW of fans and the
 * compactor on the world's industrial voltage, the box on the north face of its main cell. MOP (0-0-60) to a bagging station against it,
 * in 1 t bags.
 */
public class PotashDryerBlockEntity extends UnitMotorBlockEntity implements Bagged {
	private double mop;   // kg ready for bags
	String unit = "idle: no crystals from the crystallizer";

	public PotashDryerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.POTASH_DRYER_ENTITY, pos, state);
	}

	List<Lines.Face> gasIn() {
		return faces(at -> at[2] == 2 && at[1] == 0, Direction.SOUTH);
	}

	/** Its box is on the north face of its main cell: the part it stands against covers its west side. */
	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	/** kg of MOP it can still take before its bins are full: two bags. */
	double room() {
		return Math.max(0, 2 * Tuning.BULK_BAG_KG - mop);
	}

	void addMop(double kg) {
		mop += kg;
		setChanged();
	}

	/** For the game test: MOP as if dried. */
	public void testAddMop(double kg) {
		addMop(kg);
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.MOP_BAG;
	}

	@Override
	public double bagReady() {
		return mop;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, mop);
		mop -= got;
		setChanged();
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Potash dryer (fans and compactor %.0f kW, %s): ", Tuning.POTASH_DRYER_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; %,.0f kg of MOP ready for bags; ", mop) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("mop", mop);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		mop = input.getDoubleOr("mop", 0);
	}
}
