package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import java.util.List;
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

/**
 * The coke calcining kiln (docs/multiblocks/delayed-coker.md): a rotary kiln 2.5 m across and 30 m long at about 1,300 C; drive
 * 75 kW at 480 V. Green coke by a hopper on top loses 22 % as moisture and volatiles; it burns them and 1.5 MJ a kg of calcined
 * coke of gas, coker gas or LPG at the east face of its bottom cells. The calcined coke goes to the cooler it touches.
 */
public class CokeKilnBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double green;   // kg of green coke tipped in
	private double made;    // t of calcined coke
	private String unit = "starting";

	public CokeKilnBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.COKE_KILN_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.GREEN_COKE);
	}

	private List<Lines.Face> east() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	/** kg of calcined coke a second at the coker's rate. */
	static double rate() {
		return Tuning.COKER_RESIDUE_KGS * Tuning.COKER_COKE_YIELD * Tuning.COKE_CALCINE_YIELD;
	}

	@Override
	protected void work(Level level) {
		double max = rate() * Tuning.CARBON_CATCH_UP * seconds();
		if (green < max / Tuning.COKE_CALCINE_YIELD && fed() > 0) {
			useOne();
			green += Tuning.GREEN_COKE_KG;
		}
		CokeCoolerBlockEntity cooler = Touching.find(level, worldPosition, CokeCoolerBlockEntity.class, c -> true);
		if (cooler == null) {
			unit = "NOT BUILT: set it against the coke cooler";
			return;
		}
		if (green < 0.01) {
			unit = "NO COKE: put a hopper of green coke on it";
			return;
		}
		max = Math.min(Math.min(max, green * Tuning.COKE_CALCINE_YIELD), cooler.room());
		if (max <= 1e-7) {
			unit = "FULL: set a bagging station against the cooler";
			return;
		}
		double strength = Math.min(drive(level), cooler.drive(level));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; cooler " + cooler.motor().describe();
			return;
		}
		double kg = max * strength;
		double need = kg * Tuning.COKE_KILN_J_PER_KG;
		double got = GasFiring.fire(level, east(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "NO FUEL: pipe gas, coker gas or LPG to its east side";
			return;
		}
		green = Math.max(0, green - kg / Tuning.COKE_CALCINE_YIELD);
		cooler.addCoke(kg);
		made += kg / 1000;
		unit = String.format("calcining at 1,300 C: %.0f kg/h of calcined coke, %.0f kW of gas", kg / seconds() * 3600, got / seconds() / 1000);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Coke kiln (rotary): %s; green coke %,.0f kg and %d pieces; %.1f t calcined; ", unit, green, fed(), made) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("green", green);
		output.putDouble("made", made);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		green = input.getDoubleOr("green", 0);
		made = input.getDoubleOr("made", 0);
	}
}
