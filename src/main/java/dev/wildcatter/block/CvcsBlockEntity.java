package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The chemical and volume control system's skid (docs/multiblocks/reactor.md), against the vessel: its charging pump (15 kW, 480 V)
 * fills the RCS and, once it is full, borates or dilutes it toward its target at 0.5 kg/s of letdown and charging. Boric acid bags by
 * a hopper on top keep its tank; makeup water comes off a pump. The vessel works out the boron; this holds the stock.
 */
public class CvcsBlockEntity extends UnitMotorBlockEntity implements HopperFed, WaterUser {
	private static final double WATER_HOLD = 2000.0;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double acid;    // kg of boric acid in its tank
	private double water;   // L of makeup water

	public CvcsBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CVCS_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.BORIC_ACID);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takes(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public double waterLevel() {
		return water / WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	public double boricAcid() {
		while (acid <= Tuning.CVCS_BORIC_ACID_KG - Tuning.BORIC_ACID_BAG_KG && fed() > 0) {
			useOne();
			acid += Tuning.BORIC_ACID_BAG_KG;
		}
		return acid;
	}

	public double water() {
		return water;
	}

	/** What the vessel used this step: boric acid (negative when the letdown's is recovered) and makeup water. */
	public void use(double acidKg, double waterKg) {
		acid = Math.max(0, Math.min(Tuning.CVCS_BORIC_ACID_KG, acid - acidKg));
		water = Math.max(0, water - waterKg);
		setChanged();
	}

	public void setAcidForTest(double kg) {
		acid = kg;
		setChanged();
	}

	private @Nullable ReactorVesselBlockEntity vessel(Level level) {
		return Touching.find(level, worldPosition, ReactorVesselBlockEntity.class, v -> true);
	}

	public String nextTarget(Level level) {
		ReactorVesselBlockEntity vessel = vessel(level);
		if (vessel == null) {
			return "CVCS: NOT BUILT: set it against the REACTOR VESSEL";
		}
		return String.format("CVCS: boron target %.0f ppm (the RCS holds %.0f ppm)", vessel.reactor().nextTarget(), vessel.reactor().ppm());
	}

	@Override
	public Component inspect() {
		ReactorVesselBlockEntity vessel = level == null ? null : vessel(level);
		String state = vessel == null ? "NOT BUILT: set it against the REACTOR VESSEL"
				: String.format("%s; RCS %.0f ppm, target %.0f ppm", vessel.reactor().cvcsState(), vessel.reactor().ppm(), vessel.reactor().targetPpm());
		return Component.literal(String.format("CVCS: %s; boric acid tank %.0f kg (%d bags waiting), makeup water %.0f L; %s", state, acid, fed(), water, describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("acid", acid);
		output.putDouble("water", water);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		acid = input.getDoubleOr("acid", 0);
		water = input.getDoubleOr("water", 0);
	}
}
