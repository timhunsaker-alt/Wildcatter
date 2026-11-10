package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The drilling mud plant's two mills (docs/multiblocks/drilling-mud.md):
 * <ul>
 * <li>the barite mill: a roller mill with its classifier and baghouse grinding barite to API 13A (97 % under 75 µm), 150 kW at 480 V,
 * 5 t/h. The game's 90 % ore makes the 4.1 grade, not the 4.2, for its gangue;</li>
 * <li>the bentonite dryer: a rotary dryer and roller mill taking bentonite to about 10 % moisture and 200 mesh, 3 t/h; its burner
 * fires pipeline gas or LPG at the east face of its bottom cells (1 MW), its mill and fan 75 kW at 480 V.</li>
 * </ul>
 * The mineral goes in by a hopper on top, a block's worth at a time; the ground product goes to a bagging station set against it.
 */
public class MudMillBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double feed;     // kg of the mineral in the mill's bin
	private double ground;   // kg ground and ready for bags
	private double made;     // kg ground in all
	private String unit = "starting";

	public MudMillBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.MUD_MILL_ENTITY, pos, state);
	}

	public boolean dryer() {
		return kind().equals("bentonite_dryer");
	}

	private Ore ore() {
		return dryer() ? Ore.BENTONITE : Ore.BARITE;
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(ore()));
	}

	@Override
	public Item bagItem() {
		return dryer() ? WildcatterItems.BENTONITE_BAG : WildcatterItems.BARITE_BAG;
	}

	@Override
	public double bagReady() {
		return ground;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, ground);
		ground -= got;
		setChanged();
		return got;
	}

	/** For the game test: product as if ground. */
	public void testGround(double kg) {
		ground += kg;
		setChanged();
	}

	public double ground() {
		return ground;
	}

	public double made() {
		return made;
	}

	private List<Lines.Face> burner() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected void work(Level level) {
		double rate = dryer() ? Tuning.BENTONITE_DRYER_KGS : Tuning.BARITE_MILL_KGS;
		double max = rate * seconds();
		if (feed < max && fed() > 0) {
			useOne();
			feed += ore().kgPerBlock();
		}
		String name = dryer() ? "BENTONITE" : "BARITE";
		if (feed < 0.01) {
			unit = "NO " + name + ": put a hopper of " + name.toLowerCase() + " on it";
			return;
		}
		max = Math.min(Math.min(max, feed), 2 * Tuning.BULK_BAG_KG - ground);
		if (max <= 1e-7) {
			unit = "FULL: set a bagging station against it";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		double kg = max * strength;
		double gas = 0;
		if (dryer()) {
			double need = kg * Tuning.BENTONITE_DRYER_J_PER_KG;
			gas = GasFiring.fire(level, burner(), need, this);
			kg *= Math.min(1, gas / need);
			if (kg <= 1e-7) {
				unit = "NO FUEL: pipe gas or LPG to its east side";
				return;
			}
		}
		feed -= kg;
		ground += kg;
		made += kg;
		unit = dryer() ? String.format("drying and milling at %.1f t/h, %.0f kW of gas", kg / seconds() * 3.6, gas / seconds() / 1000)
				: String.format("grinding to API 13A 4.1 at %.1f t/h", kg / seconds() * 3.6);
	}

	@Override
	public Component inspect() {
		String what = dryer() ? "Bentonite dryer (rotary, " + Math.round(Tuning.BENTONITE_DRYER_KGS * 3.6) + " t/h)" : "Barite mill (roller, " + Math.round(Tuning.BARITE_MILL_KGS * 3.6) + " t/h)";
		return Component.literal(String.format("%s: %s; %,.0f kg in its bin and %d blocks; %,.0f kg ready for bags, %.1f t made; ", what, unit, feed, fed(), ground, made / 1000) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("feed", feed);
		output.putDouble("ground", ground);
		output.putDouble("made", made);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		feed = input.getDoubleOr("feed", 0);
		ground = input.getDoubleOr("ground", 0);
		made = input.getDoubleOr("made", 0);
	}
}
