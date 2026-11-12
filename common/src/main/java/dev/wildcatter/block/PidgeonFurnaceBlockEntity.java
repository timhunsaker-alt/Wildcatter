package dev.wildcatter.block;

import dev.wildcatter.TimeScale;
import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Zirconium;
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
 * The Pidgeon furnace (docs/multiblocks/zirconium-extensions.md item 4, 17c2): a gas-fired furnace holding a bank of alloy-steel retorts
 * at about 1,200 C under vacuum, with their pumps and condensers, 4 x 4 x 10. Dolime (the lime kiln's, on dolomite), ferrosilicon 75
 * (the submerged arc furnace's) and fluorspar (3 % of the briquette) by a hopper on top; gas off the pipe at the east face of its bottom
 * cells, 180 MJ a kg of Mg, about 0.5 MW. 10 kg of magnesium an hour condenses as crowns at the retorts' cold ends; at the end of each
 * 12-hour cycle (through {@link TimeScale}) the crowns are drawn and cast into 22.7 kg ingots, out to a hopper under it. Its vacuum
 * pumps: 30 kW on the industrial supply, box west. Dicalcium silicate slag, about 6 t a t of Mg, is counted.
 */
public class PidgeonFurnaceBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final int DOLIME = 0, FESI = 1, FLUORSPAR = 2, OUT = 3;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(4, ItemStack.EMPTY);
	private double dolime, fesi, fluorspar;   // kg on hand
	private double crowns;    // kg of magnesium condensed this cycle
	private double cycle;     // s into the cycle
	private double slag;      // t
	private int cycles;
	private String unit = "starting";

	public PidgeonFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.PIDGEON_FURNACE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private static boolean fluorspar(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.FLUORITE)) || stack.is(WildcatterItems.SYNTHETIC_FLUORSPAR);
	}

	private boolean takes(int slot, ItemStack stack) {
		return switch (slot) {
			case DOLIME -> stack.is(WildcatterItems.DOLIME_BAG);
			case FESI -> stack.is(WildcatterItems.FERROSILICON_BAG);
			case FLUORSPAR -> fluorspar(stack);
			default -> false;
		};
	}

	@Override
	public boolean takes(ItemStack stack) {
		return takes(DOLIME, stack) || takes(FESI, stack) || takes(FLUORSPAR, stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {DOLIME, FESI, FLUORSPAR} : side == Direction.DOWN ? new int[] {OUT} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takes(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takes(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUT && side == Direction.DOWN;
	}

	private List<Lines.Face> gas() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	public int ingots() {
		return slots.get(OUT).getCount();
	}

	public double crowns() {
		return crowns;
	}

	/** For the game test: the cycle all but done. */
	public void finishCycleForTest() {
		cycle = TimeScale.scaled(Tuning.PIDGEON_CYCLE_S) - 1;
		setChanged();
	}

	/** For the game test: magnesium condensed as if from a cycle's run. */
	public void addCrownsForTest(double kg) {
		crowns += kg;
		setChanged();
	}

	/** Opens a bag or a block of a feed into its kg on hand while it holds less than {@code want}. */
	private void open(double want) {
		if (dolime < want * Zirconium.dolimePerMg() && !slots.get(DOLIME).isEmpty()) {
			slots.get(DOLIME).shrink(1);
			dolime += Tuning.BULK_BAG_KG;
		}
		if (fesi < want * Tuning.PIDGEON_FESI_PER_MG && !slots.get(FESI).isEmpty()) {
			slots.get(FESI).shrink(1);
			fesi += Tuning.BULK_BAG_KG;
		}
		if (fluorspar < want * Zirconium.fluorsparPerMg() && !slots.get(FLUORSPAR).isEmpty()) {
			ItemStack f = slots.get(FLUORSPAR);
			fluorspar += f.is(WildcatterItems.SYNTHETIC_FLUORSPAR) ? Tuning.BULK_BAG_KG : Ore.FLUORITE.kgPerBlock();
			f.shrink(1);
		}
	}

	/** At the cycle's end: the crowns drawn and cast into ingots; the part of an ingot left over waits for the next cycle. */
	private void draw() {
		int n = (int) Math.floor(crowns / Tuning.MG_INGOT_KG + 1e-9);
		ItemStack out = slots.get(OUT);
		int room = out.isEmpty() ? 64 : out.getMaxStackSize() - out.getCount();
		n = Math.min(n, room);
		if (n > 0) {
			if (out.isEmpty()) {
				slots.set(OUT, new ItemStack(WildcatterItems.MAGNESIUM_INGOT, n));
			} else {
				out.grow(n);
			}
			crowns -= n * Tuning.MG_INGOT_KG;
		}
		cycle = 0;
		cycles++;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		double hold = TimeScale.scaled(Tuning.PIDGEON_CYCLE_S);
		if (cycle >= hold - 1e-6) {
			draw();
		}
		double want = Tuning.PIDGEON_MG_KGS * seconds();
		open(want);
		String missing = dolime < want * Zirconium.dolimePerMg() ? "NO DOLIME: put bags of dolime (a lime kiln on dolomite) in its hopper"
				: fesi < want * Tuning.PIDGEON_FESI_PER_MG ? "NO FERROSILICON: put bags of ferrosilicon 75 in its hopper"
				: fluorspar < want * Zirconium.fluorsparPerMg() ? "NO FLUORSPAR: put fluorspar in its hopper" : null;
		if (missing != null) {
			unit = missing;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting the vacuum pumps: " + describe();
			return;
		}
		double need = want * strength * Tuning.PIDGEON_GAS_J_PER_KG;
		double got = GasFiring.fire(level, gas(), need, this);
		double kg = want * strength * Math.min(1, got / need);
		if (kg <= 1e-9) {
			unit = "NO GAS: pipe gas or LPG to the east face of its bottom cells";
			return;
		}
		dolime -= kg * Zirconium.dolimePerMg();
		fesi -= kg * Tuning.PIDGEON_FESI_PER_MG;
		fluorspar -= kg * Zirconium.fluorsparPerMg();
		crowns += kg;
		slag += kg * Tuning.PIDGEON_SLAG_PER_MG / 1000;
		cycle += seconds();
		unit = String.format("reducing dolime with ferrosilicon at 1,200 C under vacuum: %.1f kg/h of magnesium, %.0f kW of gas; crowns %.1f kg, cycle %.0f %% of its %.0f h%s",
				kg / seconds() * 3600, got / seconds() / 1000, crowns, cycle / hold * 100, Tuning.PIDGEON_CYCLE_S / 3600, TimeScale.note(Tuning.PIDGEON_CYCLE_S));
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Pidgeon furnace (vacuum pumps %.0f kW, %s): %s; dolime %,.0f kg, ferrosilicon %,.0f kg, fluorspar %,.0f kg on hand; %d ingots for a hopper; %d cycles, %.1f t of slag; ",
				Tuning.PIDGEON_PUMP_WATTS / 1000, Voltage.INDUSTRIAL.label(), unit, dolime, fesi, fluorspar, ingots(), cycles, slag) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("dolime", dolime);
		output.putDouble("fesi", fesi);
		output.putDouble("fluorspar", fluorspar);
		output.putDouble("crowns", crowns);
		output.putDouble("cycle", cycle);
		output.putDouble("slag", slag);
		output.putInt("cycles", cycles);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		dolime = input.getDoubleOr("dolime", 0);
		fesi = input.getDoubleOr("fesi", 0);
		fluorspar = input.getDoubleOr("fluorspar", 0);
		crowns = input.getDoubleOr("crowns", 0);
		cycle = input.getDoubleOr("cycle", 0);
		slag = input.getDoubleOr("slag", 0);
		cycles = input.getIntOr("cycles", 0);
	}
}
