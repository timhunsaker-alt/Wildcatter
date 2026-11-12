package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.FurnaceLoad;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.MeltShop;
import dev.wildcatter.fluid.MeltShop.Charge;
import dev.wildcatter.fluid.MeltShop.Grade;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The alloy melt shop's induction furnace and its controller (docs/multiblocks/nickel-smelter.md, 17b2): a 2 t coreless medium-frequency
 * furnace with its power cabinet, 1.5 MW at MV and PF 0.95 (a constant-power load; its box on the west face of its main cell), about
 * 600 kWh a tonne. Its charge comes by a hopper on top, each to its bin: HC ferrochrome, cathode nickel, HC ferromanganese and
 * ferromolybdenum in bulk bags, iron ingots. When its bins hold a heat of the chosen grade (sneak-click sets it) it melts it, then hands
 * the heat to the AOD converter it touches.
 */
public class InductionFurnaceBlockEntity extends PlantUnitBlockEntity implements CircuitPart, HopperFed, NickelPartBlock.Sneaked {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(Charge.values().length, ItemStack.EMPTY);
	private final double[] stock = new double[Charge.values().length];   // kg opened out of bags and ingots
	private final FurnaceLoad load = new FurnaceLoad(Voltage.MEDIUM, Tuning.INDUCTION_POWER_FACTOR);
	private Grade grade = Grade.S304;
	private @Nullable Grade melting;   // the heat in the coil
	private double energyLeft;         // J to finish melting it
	private boolean molten;            // melted, waiting for the AOD
	private int heats;

	public InductionFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.INDUCTION_FURNACE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private static Item itemOf(int slot) {
		return switch (Charge.values()[slot]) {
			case FERROCHROME -> WildcatterItems.FERROCHROME_BAG;
			case NICKEL -> WildcatterItems.NICKEL_CATHODE_BAG;
			case FERROMANGANESE -> WildcatterItems.FERROMANGANESE_BAG;
			case FERROMOLYBDENUM -> WildcatterItems.FERROMOLYBDENUM_BAG;
			case IRON -> Items.IRON_INGOT;
		};
	}

	@Override
	public boolean takes(ItemStack stack) {
		for (int i = 0; i < slots.size(); i++) {
			if (stack.is(itemOf(i))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1, 2, 3, 4} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot >= 0 && slot < slots.size() && stack.is(itemOf(slot));
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	private double have(Charge c) {
		return stock[c.ordinal()] + slots.get(c.ordinal()).getCount() * c.itemKg();
	}

	private void use(Charge c, double kg) {
		int i = c.ordinal();
		while (stock[i] < kg - 1e-9 && !slots.get(i).isEmpty()) {
			slots.get(i).shrink(1);
			stock[i] += c.itemKg();
		}
		stock[i] = Math.max(0, stock[i] - kg);
	}

	public Grade grade() {
		return grade;
	}

	public void setGrade(Grade g) {
		grade = g;
		setChanged();
	}

	public boolean molten() {
		return molten;
	}

	public boolean melting() {
		return melting != null && !molten;
	}

	public FurnaceLoad load() {
		return load;
	}

	/** Sneak and right-click: the next grade, for the next heat. */
	@Override
	public String sneakClick() {
		grade = grade.next();
		setChanged();
		return "Induction furnace: the next heat is " + grade.label + " (" + grade.chemistry + ")";
	}

	/** For the game test: the melt's energy given, so the next interval finishes it. */
	public void testFinishMelt() {
		if (melting != null && !molten) {
			energyLeft = 1;
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, InductionFurnaceBlockEntity f) {
		if (level.isClientSide() || (level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		f.work(level);
		f.setChanged();
	}

	private void work(Level level) {
		double dt = seconds();
		AodConverterBlockEntity aod = Touching.find(level, worldPosition, AodConverterBlockEntity.class, a -> true);
		if (molten) {
			if (aod != null && aod.receive(melting, Tuning.INDUCTION_HEAT_KG, melting.heatCarbon())) {
				molten = false;
				melting = null;
				heats++;
				status = "tapped the heat into the AOD";
			} else {
				status = aod == null ? "MOLTEN: set the AOD converter against it to take the heat" : "MOLTEN: the AOD is still blowing its last heat";
			}
			return;
		}
		if (melting == null) {
			if (plcHold(level)) {
				return;
			}
			for (Charge c : Charge.values()) {
				if (have(c) < grade.heatKg(c) - 1e-6) {
					status = String.format("NO CHARGE: a heat of %s needs %.0f kg of %s, has %.0f (a hopper on its top)", grade.label, grade.heatKg(c), c.label, have(c));
					return;
				}
			}
			for (Charge c : Charge.values()) {
				use(c, grade.heatKg(c));
			}
			melting = grade;
			energyLeft = MeltShop.heatJoules();
		}
		double watts = load.watts();
		energyLeft -= Math.max(0, watts) * dt;
		if (energyLeft <= 0) {
			molten = true;
			energyLeft = 0;
			status = "MELTED: " + melting.label + ", tapping into the AOD";
			return;
		}
		status = watts < 1000 ? (load.undervoltage() ? "NO POWER: " + load.describe() : "starting: waiting for power")
				: String.format("melting %s: %.2f MW, %.0f %% melted, %.0f min left", melting.label, watts / 1e6, (1 - energyLeft / MeltShop.heatJoules()) * 100,
						energyLeft / watts / 60);
	}

	// ---- the grid -----------------------------------------------------------------------------------------------------------------------
	private Direction boxFace() {
		return LeasePartBlock.world(getBlockState(), Direction.WEST);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == boxFace() && voltage == Voltage.MEDIUM && role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		load.setDemand(melting != null && !molten ? Tuning.INDUCTION_WATTS : 0);
		load.build(circuit, worldPosition, List.of(boxFace()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		load.apply(circuit, ticks);
		setChanged();
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	@Override
	public double measure(Quantity quantity) {
		return switch (quantity) {
			case WATTS -> load.watts();
			case VOLTS -> load.volts();
			case AMPS -> load.amps();
			default -> Double.NaN;
		};
	}

	@Override
	public Component inspect() {
		StringBuilder bins = new StringBuilder();
		for (Charge c : Charge.values()) {
			bins.append(String.format(", %s %.0f kg", c.label, have(c)));
		}
		return Component.literal(String.format("Induction furnace (2 t, %.1f MW, %s): next heat %s; ", Tuning.INDUCTION_WATTS / 1e6, Voltage.MEDIUM.label(), grade.label) + status
				+ "; bins" + bins + String.format("; %d heats; %s; sneak and right-click sets the grade", heats, load.describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		for (Charge c : Charge.values()) {
			output.putDouble("stock_" + c.name(), stock[c.ordinal()]);
		}
		output.putString("grade", grade.name());
		output.putString("melting", melting == null ? "" : melting.name());
		output.putDouble("energyLeft", energyLeft);
		output.putBoolean("molten", molten);
		output.putInt("heats", heats);
		load.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		for (Charge c : Charge.values()) {
			stock[c.ordinal()] = input.getDoubleOr("stock_" + c.name(), 0);
		}
		grade = parse(input.getStringOr("grade", "S304"), Grade.S304);
		String m = input.getStringOr("melting", "");
		melting = m.isEmpty() ? null : parse(m, null);
		energyLeft = input.getDoubleOr("energyLeft", 0);
		molten = input.getBooleanOr("molten", false) && melting != null;
		heats = input.getIntOr("heats", 0);
		load.load(input);
	}

	static @Nullable Grade parse(String name, @Nullable Grade fallback) {
		for (Grade g : Grade.values()) {
			if (g.name().equals(name)) {
				return g;
			}
		}
		return fallback;
	}
}
