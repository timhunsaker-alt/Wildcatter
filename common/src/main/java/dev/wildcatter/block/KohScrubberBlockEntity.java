package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
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
 * The KOH scrubber (docs/multiblocks/conversion-extensions.md, 16b1): a packed scrubber, its potassium hydroxide recirculation tank,
 * and a lime reactor and filter that regenerate the liquor. It stands against the hydrofluorinator and takes its excess HF
 * (0.094 kg a kg of U): KOH + HF -> KF + H2O, then 2 KF + Ca(OH)2 -> CaF2 + 2 KOH. Its first charge of 2,000 kg of KOH comes as
 * caustic potash on the pipe at the north face of its front cells; quicklime bags by a hopper on top; the CaF2 cake goes out to a hopper
 * under it as bags of synthetic fluorspar, which the HF kiln and a fluorinator's new bed take as fluorspar. Without lime the KOH is
 * spent in about 28 hours: it reads SPENT and the hydrofluorinator stops. Its pumps: 15 kW on the industrial supply, box on the west.
 */
public class KohScrubberBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int OUT = 4;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1 + OUT, ItemStack.EMPTY);
	private double koh;       // kg of KOH active in the liquor
	private double spent;     // kg of KOH turned to KF, waiting for lime
	private double lime;      // kg of CaO tipped in
	private double caf2;      // kg of CaF2 cake not yet bagged
	private double scrubbedHf;
	private long running = -1000;   // when its pumps last ran
	private String unit = "starting";

	public KohScrubberBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.KOH_SCRUBBER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.QUICKLIME_BAG);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0} : side == Direction.DOWN ? new int[] {1, 2, 3, 4} : new int[0];
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot > 0 && side == Direction.DOWN;
	}

	private List<Lines.Face> potash() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public double koh() {
		return koh;
	}

	public double caf2() {
		return caf2;
	}

	/** True if its pumps are running and its liquor holds the KOH for {@code hf} kg of HF. */
	public boolean ready(double hf) {
		return level != null && level.getGameTime() - running < 40 && koh >= hf * Tuning.KOH_PER_HF;
	}

	/** Takes {@code hf} kg of the hydrofluorinator's excess HF into its liquor. */
	public void scrub(double hf) {
		double used = Math.min(koh, hf * Tuning.KOH_PER_HF);
		koh -= used;
		spent += used;
		scrubbedHf += hf;
		setChanged();
	}

	/** For the game test: its liquor and cake set straight. */
	public void setForTest(double kohKg, double spentKg, double caf2Kg, double limeKg) {
		koh = kohKg;
		spent = spentKg;
		caf2 = caf2Kg;
		lime = limeKg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		// the first charge: caustic potash off its pipe, until it holds 2,000 kg of KOH
		double room = Tuning.KOH_CHARGE_KG - koh - spent;
		if (room > 1e-6) {
			double solution = Lines.pull(level, potash(), Product.CAUSTIC_POTASH, room / Tuning.CAUSTIC_STRENGTH, Pull.MACHINE, this);
			koh += solution * Tuning.CAUSTIC_STRENGTH;
		}
		if (koh + spent < 1e-6) {
			unit = "NO CHARGE: pipe caustic potash to the north face of its front (a chlor-alkali cell run on potash)";
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the pumps: " + describe();
			return;
		}
		running = level.getGameTime();
		// the lime reactor regenerates the spent liquor
		if (spent > 1e-6 && lime < spent * Tuning.LIME_PER_HF / Tuning.KOH_PER_HF && fed() > 0) {
			useOne();
			lime += Tuning.BULK_BAG_KG;
		}
		double regen = Math.min(Math.min(spent, Tuning.KOH_REGEN_KGS * seconds() * strength), lime * Tuning.KOH_PER_HF / Tuning.LIME_PER_HF);
		if (regen > 0) {
			koh += regen;
			spent -= regen;
			lime -= regen / Tuning.KOH_PER_HF * Tuning.LIME_PER_HF;
			caf2 += regen / Tuning.KOH_PER_HF * Tuning.CAF2_PER_HF;
		}
		while (caf2 >= Tuning.BULK_BAG_KG) {
			int out = -1;
			for (int i = 1; i <= OUT && out < 0; i++) {
				ItemStack s = slots.get(i);
				if (s.isEmpty() || s.is(WildcatterItems.SYNTHETIC_FLUORSPAR) && s.getCount() < s.getMaxStackSize()) {
					out = i;
				}
			}
			if (out < 0) {
				break;
			}
			if (slots.get(out).isEmpty()) {
				slots.set(out, new ItemStack(WildcatterItems.SYNTHETIC_FLUORSPAR));
			} else {
				slots.get(out).grow(1);
			}
			caf2 -= Tuning.BULK_BAG_KG;
		}
		double share = koh / Math.max(1, koh + spent);
		if (koh < 1) {
			unit = "SPENT: its KOH is all KF; put quicklime bags in a hopper on top to regenerate it";
		} else if (spent > 1e-6 && lime < 1e-6) {
			unit = String.format("scrubbing; NO LIME: %.0f %% of its KOH left (quicklime bags in a hopper on top)", share * 100);
		} else {
			unit = String.format("scrubbing; %.0f %% of its KOH active%s", share * 100, regen > 0 ? String.format(", regenerating %.1f kg/h with lime", regen / seconds() * 3600) : "");
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("KOH scrubber (" + Math.round(Tuning.KOH_SCRUBBER_WATTS / 1000) + " kW, " + Voltage.INDUSTRIAL.label() + "): " + unit
				+ String.format("; KOH %,.0f kg, spent %,.0f kg; lime %,.0f kg and %d bags; CaF2 %,.0f kg to bag; %,.0f kg of HF scrubbed; ", koh, spent, lime, fed(), caf2, scrubbedHf)
				+ describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("koh", koh);
		output.putDouble("spent", spent);
		output.putDouble("lime", lime);
		output.putDouble("caf2", caf2);
		output.putDouble("scrubbedHf", scrubbedHf);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		koh = input.getDoubleOr("koh", 0);
		spent = input.getDoubleOr("spent", 0);
		lime = input.getDoubleOr("lime", 0);
		caf2 = input.getDoubleOr("caf2", 0);
		scrubbedHf = input.getDoubleOr("scrubbedHf", 0);
	}
}
