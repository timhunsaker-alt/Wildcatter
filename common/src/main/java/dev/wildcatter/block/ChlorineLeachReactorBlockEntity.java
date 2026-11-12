package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Nickel;
import dev.wildcatter.fluid.Product;
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
 * The nickel refinery's chlorine leach reactor and its controller (docs/multiblocks/nickel-smelter.md): Bessemer matte granules leached
 * with chlorine at about 110 C, 30 kW at LV (box west). The matte comes by touching the Peirce-Smith converter or in 1 t bags by a hopper
 * on top; 0.5 t an hour. Its chlorine is the tankhouse's anode chlorine, which comes back by touching, with make-up (and the first fill)
 * off the pipe on the north face of its bottom cells (its east face is the tankhouse's, as the spec's layout has it). The nickel chloride liquor goes on to the purification circuit it touches. With the
 * tankhouse gone or without DC its chlorine stops coming back, and it stops: NO CHLORINE.
 */
public class ChlorineLeachReactorBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double matteNi;      // kg of Ni in matte opened into its feed
	private double liquorNi;     // kg of Ni in the leach liquor
	private double returned;     // kg of anode chlorine come back from the tankhouse
	private double making;       // kg/h of Ni leached, the last interval
	private double madeUp;       // kg of chlorine off the pipe, all told
	private String unit = "starting";

	public ChlorineLeachReactorBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.CHLORINE_LEACH_REACTOR_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.BESSEMER_MATTE_BAG);
	}

	private List<Lines.Face> chlorine() {
		return faces(at -> at[1] == 0 && at[2] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public double making() {
		return making;
	}

	public double liquorNi() {
		return liquorNi;
	}

	/** Purification takes up to {@code kg} of Ni as liquor; returns what it gave. */
	public double takeLiquorNi(double kg) {
		double got = Math.max(0, Math.min(kg, liquorNi));
		liquorNi -= got;
		setChanged();
		return got;
	}

	/** The tankhouse's anode chlorine coming back; it keeps at most an hour's worth. */
	public void returnChlorine(double kg) {
		returned = Math.min(returned + kg, Tuning.LEACH_MATTE_KGH * Tuning.BESSEMER_MATTE_NI * Nickel.leachChlorinePerNi());
		setChanged();
	}

	private @Nullable NickelTankhouseBlockEntity tankhouse(Level level) {
		NickelTankhouseBlockEntity t = Touching.find(level, worldPosition, NickelTankhouseBlockEntity.class, x -> true);
		if (t == null) {
			NiPurificationBlockEntity p = Touching.find(level, worldPosition, NiPurificationBlockEntity.class, x -> true);
			t = p == null ? null : Touching.find(level, p.getBlockPos(), NickelTankhouseBlockEntity.class, x -> true);
		}
		return t;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double dt = seconds();
		double want = Tuning.LEACH_MATTE_KGH * Tuning.BESSEMER_MATTE_NI / 3600 * dt;   // kg of Ni an interval
		if (matteNi < want) {
			PeirceSmithConverterBlockEntity converter = Touching.find(level, worldPosition, PeirceSmithConverterBlockEntity.class, c -> true);
			if (converter != null) {
				matteNi += converter.takeBessemerNi(want * 4 - matteNi);
			}
		}
		if (matteNi < want && !slots.get(0).isEmpty()) {
			slots.get(0).shrink(1);
			matteNi += Tuning.BULK_BAG_KG * Tuning.BESSEMER_MATTE_NI;
		}
		NiPurificationBlockEntity purification = Touching.find(level, worldPosition, NiPurificationBlockEntity.class, p -> true);
		NickelTankhouseBlockEntity tankhouse = tankhouse(level);
		if (purification == null || tankhouse == null) {
			unit = "NOT BUILT: " + (purification == null ? "NO PURIFICATION: set the purification circuit against it" : "NO TANKHOUSE: set the nickel tankhouse against it");
			return;
		}
		if (matteNi <= 1e-6) {
			unit = "NO FEED: set it against the Peirce-Smith converter, or put bags of Bessemer matte in a hopper on its top";
			return;
		}
		if (!tankhouse.givingChlorine()) {
			unit = "NO CHLORINE: the tankhouse's chlorine is not coming back (" + tankhouse.shortStatus() + ")";
			return;
		}
		if (liquorNi >= Tuning.LEACH_LIQUOR_HOLD_KG_NI) {
			unit = "FULL: the purification circuit is not taking its liquor";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double ni = Math.min(want, matteNi);
		double cl = ni * Nickel.leachChlorinePerNi();
		double fromPipe = Math.max(0, cl - returned);
		double pipe = fromPipe > 0 ? Lines.available(level, chlorine(), Product.CHLORINE, Pull.MACHINE, this) : 0;
		double have = Math.min(cl, returned + pipe);
		if (have < cl * 0.01) {
			unit = "NO CHLORINE: pipe chlorine to the north side of its bottom cells (its first fill and make-up)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the agitator: " + describe();
			return;
		}
		double share = have / cl * strength;
		ni *= share;
		cl *= share;
		double fromReturned = Math.min(returned, cl);
		returned -= fromReturned;
		if (cl - fromReturned > 0) {
			madeUp += Lines.pull(level, chlorine(), Product.CHLORINE, cl - fromReturned, Pull.MACHINE, this);
		}
		matteNi -= ni;
		liquorNi += ni;
		making = ni / dt * 3600;
		unit = String.format("leaching Bessemer matte at 110 C: %.0f kg/h of Ni into the liquor, %.0f kg/h of chlorine (%.0f %% back from the tankhouse)",
				making, cl / dt * 3600, cl > 0 ? fromReturned / cl * 100 : 0);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Chlorine leach reactor (%.0f kW, %s): ", Tuning.LEACH_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; matte %.0f kg of Ni in its feed, %d bags; liquor %.0f kg of Ni; anode chlorine %.0f kg back; %.0f kg made up off the pipe; ", matteNi,
						slots.get(0).getCount(), liquorNi, returned, madeUp)
				+ describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, making > 0, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(making, Tuning.LEACH_MATTE_KGH * Tuning.BESSEMER_MATTE_NI);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("matteNi", matteNi);
		output.putDouble("liquorNi", liquorNi);
		output.putDouble("returned", returned);
		output.putDouble("madeUp", madeUp);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		matteNi = input.getDoubleOr("matteNi", 0);
		liquorNi = input.getDoubleOr("liquorNi", 0);
		returned = input.getDoubleOr("returned", 0);
		madeUp = input.getDoubleOr("madeUp", 0);
	}
}
