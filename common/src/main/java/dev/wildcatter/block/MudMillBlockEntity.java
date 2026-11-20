package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Product;
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
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private double feed;     // kg of the mineral in the mill's bin
	/** 23a: the second recipe: the barite mill grinding calcite or limestone to LCM, the dryer making the OBM package with quicklime and ammonia. */
	private boolean second;
	private double lime;     // kg of quicklime in the dryer, for the OBM package
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
		return stack.is(OreBlocks.MINERALS.get(ore())) || calcite(stack);
	}

	/** The barite mill's second recipe (23a): calcite, the caprock's or the hills', or limestone, ground to sized calcium carbonate. */
	private boolean calcite(ItemStack stack) {
		return !dryer() && (stack.is(net.minecraft.world.item.Items.CALCITE) || stack.is(OreBlocks.MINERALS.get(Ore.LIMESTONE)));
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? (dryer() ? new int[] {0, 1} : new int[] {0}) : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 ? takes(stack) : slot == 1 && dryer() && stack.is(WildcatterItems.QUICKLIME_BAG);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @org.jspecify.annotations.Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	@Override
	public Item bagItem() {
		if (second) {
			return dryer() ? WildcatterItems.OBM_PACKAGE : WildcatterItems.LCM_BAG;
		}
		return dryer() ? WildcatterItems.BENTONITE_BAG : WildcatterItems.BARITE_BAG;
	}

	/** The dryer's ammonia, on the north face of its bottom cells (the OBM package's amine stand-in). */
	private List<Lines.Face> ammonia() {
		return faces(at -> at[1] == 0 && at[2] == 0, Direction.NORTH);
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
			ItemStack next = slots.get(0);
			boolean alt = calcite(next);
			if (feed < 0.01 && ground < 0.01) {
				second = alt;   // the mill changes over only when empty
			}
			if (alt == second || dryer()) {
				useOne();
				feed += alt ? Ore.LIMESTONE.kgPerBlock() : ore().kgPerBlock();
			}
		}
		if (dryer()) {
			if (lime < Tuning.BULK_BAG_KG * Tuning.OBM_LIME && !slots.get(1).isEmpty()) {
				slots.get(1).shrink(1);
				lime += Tuning.BULK_BAG_KG;
				setChanged();
			}
			boolean obm = lime > 1 && Lines.available(level, ammonia(), Product.AMMONIA, Pull.MACHINE, this) > 0.01;
			if (feed < 0.01 && ground < 0.01) {
				second = obm;
			}
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
		if (dryer() && second) {
			// 23a: a tonne of package from 0.65 t of bentonite, 0.30 t of quicklime and 50 kg of ammonia (proposed stand-in)
			double product = Math.min(kg / Tuning.OBM_BENTONITE, lime / Tuning.OBM_LIME);
			double nh3 = Lines.pull(level, ammonia(), Product.AMMONIA, product * Tuning.OBM_AMMONIA / Tuning.AMMONIA_KG_PER_L, Pull.MACHINE, this);
			product = Math.min(product, nh3 * Tuning.AMMONIA_KG_PER_L / Tuning.OBM_AMMONIA);
			feed -= product * Tuning.OBM_BENTONITE;
			lime -= product * Tuning.OBM_LIME;
			ground += product;
			made += product;
			unit = String.format("making the OBM package at %.1f t/h, %.0f kW of gas", product / seconds() * 3.6, gas / seconds() / 1000);
			return;
		}
		feed -= kg;
		ground += kg;
		made += kg;
		unit = dryer() ? String.format("drying and milling at %.1f t/h, %.0f kW of gas", kg / seconds() * 3.6, gas / seconds() / 1000)
				: second ? String.format("grinding calcium carbonate to LCM at %.1f t/h", kg / seconds() * 3.6) : String.format("grinding to API 13A 4.1 at %.1f t/h", kg / seconds() * 3.6);
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
		output.putBoolean("second", second);
		output.putDouble("lime", lime);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		feed = input.getDoubleOr("feed", 0);
		ground = input.getDoubleOr("ground", 0);
		made = input.getDoubleOr("made", 0);
		second = input.getBooleanOr("second", false);
		lime = input.getDoubleOr("lime", 0);
	}
}
