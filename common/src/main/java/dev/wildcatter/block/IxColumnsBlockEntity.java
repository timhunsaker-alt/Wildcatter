package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A lead-lag pair of ion-exchange columns (docs/multiblocks/isr-wellfield.md): the resin takes 95 % of the uranium out of the
 * pregnant lixiviant (north face of its front cells), which goes back barren to the header house (south face of its back cells, the
 * east half). The loaded resin is eluted in salt brine (rock salt by a hopper on top) and the eluate acidified with sulfuric acid
 * (east face of its bottom cells) to break the carbonate complex: eluate at 15 g/L of U3O8 out of the south face of its back
 * cells, the west half, for a precipitation tank.
 */
public class IxColumnsBlockEntity extends PlantUnitBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double resin;   // kg of U3O8 loaded on the resin
	private double salt;

	public IxColumnsBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.IX_COLUMNS_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.HALITE));
	}

	private List<Lines.Face> inlet() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	private List<Lines.Face> acid() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		if (at[2] != 3 || at[1] != 0 || side != Direction.SOUTH) {
			return null;
		}
		return at[0] >= 4 ? Product.BARREN_LIXIVIANT : Product.ELUATE;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.ELUATE ? 200 : Tuning.IX_MAX_LPS * Tuning.PLANT_HOLD_SECONDS;
	}

	/** kg of NaCl to make up per litre of eluate, as the mill's SX strip. */
	private static double saltPerLitre() {
		return Tuning.SX_NACL_G_PER_L / 1000 * Tuning.SX_NACL_MAKEUP;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, IxColumnsBlockEntity x) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		double capacity = Tuning.IX_RESIN_L * Tuning.IX_RESIN_KG_PER_L;
		// loading: pregnant lixiviant through the resin, barren back to the header house
		double max = Math.min(Tuning.IX_MAX_LPS * seconds(), x.room(Product.BARREN_LIXIVIANT));
		double[] in = new double[5];
		double got = max > 0.001 ? x.pullMixed(level, x.inlet(), Product.PREGNANT_LIXIVIANT, max, in) : 0;
		double u = got * in[0];
		double captured = Math.min(u * Tuning.IX_CAPTURE, Math.max(0, capacity - x.resin));
		x.resin += captured;
		if (got > 0) {
			x.addMixed(Product.BARREN_LIXIVIANT, got, new double[] {(u - captured) / got, 0, 0, 0, 0});
		}
		// elution: brine through the loaded resin, the eluate acidified
		if (x.salt < 1 && x.fed() > 0) {
			x.useOne();
			x.salt += Ore.HALITE.kgPerBlock();
		}
		double eluate = Math.min(x.resin / (Tuning.IX_ELUATE_G_PER_L / 1000), x.room(Product.ELUATE));
		eluate = Math.min(eluate, x.salt / saltPerLitre());
		double acidPerLitre = Tuning.IX_ELUATE_ACID_G_PER_L / 1000 / Tuning.ACID_KG_PER_L;   // litres of 98.5 % acid a litre
		double acidWaiting = Lines.available(level, x.acid(), Product.SULFURIC_ACID, Pull.MACHINE, x);
		eluate = Math.min(eluate, acidWaiting / acidPerLitre);
		String elution;
		if (x.resin < 1e-9) {
			elution = "resin unloaded";
		} else if (x.salt < 0.001) {
			elution = "NO SALT: put a hopper of rock salt on it for the elution brine";
		} else if (acidWaiting < 0.001) {
			elution = "NO ACID: pipe sulfuric acid to its east side to break the carbonate complex";
		} else if (eluate > 1e-6) {
			Lines.pull(level, x.acid(), Product.SULFURIC_ACID, eluate * acidPerLitre, Pull.MACHINE, x);
			x.salt -= eluate * saltPerLitre();
			x.resin -= eluate * Tuning.IX_ELUATE_G_PER_L / 1000;
			x.addMixed(Product.ELUATE, eluate, new double[] {Tuning.IX_ELUATE_G_PER_L / 1000, 0, 0, 0, 0});
			elution = String.format("eluting %.2f L/s of eluate at %.0f g/L U3O8", eluate / seconds(), Tuning.IX_ELUATE_G_PER_L);
		} else {
			elution = "eluate waiting (pipe its south-west face to a precipitation tank)";
		}
		x.status = String.format("loading %.1f L/s of pregnant lixiviant at %.0f mg/L: %.2f g/s of U3O8 on the resin (%.0f kg, %.0f %% loaded); %s", got / seconds(),
				got > 0 ? u / got * 1e6 : 0, captured / seconds() * 1000, x.resin, x.resin / capacity * 100, elution);
		x.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("IX columns (lead and lag): " + status + String.format("; salt %,.0f kg and %d blocks", salt, fed()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("resin", resin);
		output.putDouble("salt", salt);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		resin = input.getDoubleOr("resin", 0);
		salt = input.getDoubleOr("salt", 0);
	}
}
