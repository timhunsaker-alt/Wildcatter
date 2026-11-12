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
 * The LiOH crystallizer (docs/multiblocks/lithium.md): causticisation of the leach's lithium sulfate with caustic soda (on the east face of
 * its back cell), a chilling crystallizer for Glauber's salt, and an evaporative crystallizer for LiOH.H2O on process steam (the south
 * face of its back cells, about 3 t a tonne). 110 kW at the world's industrial voltage, the box on the north face of its main cell. Bagged
 * twice: a bagging station against its north face fills sodium sulfate, one against any other face lithium hydroxide. The leach runs it.
 */
public class LiohCrystallizerBlockEntity extends UnitMotorBlockEntity implements Bagged {
	double lioh;     // kg of LiOH.H2O ready for bags
	double na2so4;   // kg of sodium sulfate ready for bags
	String unit = "idle: no lithium sulfate from the leach";

	public LiohCrystallizerBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LIOH_CRYSTALLIZER_ENTITY, pos, state);
	}

	List<Lines.Face> causticIn() {
		return faces(at -> at[0] == 2 && at[2] == 3 && at[1] == 0, Direction.EAST);
	}

	List<Lines.Face> steamIn() {
		return faces(at -> at[2] == 3 && at[1] == 0, Direction.SOUTH);
	}

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

	/** kg of LiOH.H2O it can still make before one of its two products fills two bags. */
	double room() {
		double sulfatePerLioh = dev.wildcatter.fluid.Lithium.na2so4PerLi2o() / dev.wildcatter.fluid.Lithium.liohPerLi2o();
		return Math.max(0, Math.min(2 * Tuning.BULK_BAG_KG - lioh, (2 * Tuning.BULK_BAG_KG - na2so4) / sulfatePerLioh));
	}

	/** For the game test: hydroxide and sulfate as if crystallized. */
	public void testAdd(double liohKg, double na2so4Kg) {
		lioh += liohKg;
		na2so4 += na2so4Kg;
		setChanged();
	}

	/** True if the station at {@code station} stands against its north face: that one bags the sodium sulfate. */
	private boolean sulfateStation(BlockPos station) {
		if (level == null) {
			return false;
		}
		for (int x = 0; x < 3; x++) {
			BlockPos cell = LeasePartBlock.at(worldPosition, getBlockState(), x, 0, -1);
			if (dev.wildcatter.multiblock.BigParts.entity(level, cell) instanceof net.minecraft.world.level.block.entity.BlockEntity e && e.getBlockPos().equals(station)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.LIOH_BAG;
	}

	@Override
	public double bagReady() {
		return lioh;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, lioh);
		lioh -= got;
		setChanged();
		return got;
	}

	@Override
	public Item bagItem(BlockPos station) {
		return sulfateStation(station) ? WildcatterItems.SODIUM_SULFATE_BAG : WildcatterItems.LIOH_BAG;
	}

	@Override
	public double bagReady(BlockPos station) {
		return sulfateStation(station) ? na2so4 : lioh;
	}

	@Override
	public double takeForBag(BlockPos station, double kg) {
		if (!sulfateStation(station)) {
			return takeForBag(kg);
		}
		double got = Math.min(kg, na2so4);
		na2so4 -= got;
		setChanged();
		return got;
	}

	/** Takes LiOH.H2O for the carbonator: up to {@code kg}. */
	public double takeLioh(double kg) {
		return takeForBag(kg);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("LiOH crystallizer (%.0f kW, %s): ", Tuning.LIOH_CRYSTALLIZER_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; %,.0f kg of LiOH.H2O and %,.0f kg of sodium sulfate ready for bags; ", lioh, na2so4) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("lioh", lioh);
		output.putDouble("na2so4", na2so4);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lioh = input.getDoubleOr("lioh", 0);
		na2so4 = input.getDoubleOr("na2so4", 0);
	}
}
