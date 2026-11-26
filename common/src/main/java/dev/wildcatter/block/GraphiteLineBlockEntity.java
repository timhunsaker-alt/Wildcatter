package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
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
import org.jspecify.annotations.Nullable;

/**
 * The graphite line (docs/multiblocks/grid-batteries.md, 22a): graphite schist by a hopper on top, crushed, floated to a 95 % C flake
 * concentrate and rounded in spheroidising mills, 400 kg/h of spherical graphite (proposed) to a bagging station against it. The ore's
 * graphite is its game grade (80 %), 85 % recovered (proposed); the rest goes as tails, counted. 160 kW at the world's industrial voltage, the
 * box on the west face of its main cell.
 */
public class GraphiteLineBlockEntity extends UnitMotorBlockEntity implements HopperFed, Bagged {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double graphite;    // kg of graphite in the ore on the belt
	private double spherical;   // kg ready for bags
	private double tails;       // t, counted
	private double making;
	private String unit = "starting";

	public GraphiteLineBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.GRAPHITE_LINE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.GRAPHITE));
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

	public void testAdd(double kg) {
		spherical += kg;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double max = Tuning.GRAPHITE_LINE_KGH * dev.wildcatter.TimeScale.production() / 3600 * seconds();
		if (graphite < max / Tuning.GRAPHITE_FLAKE_RECOVERY && fed() > 0) {
			useOne();
			graphite += Ore.GRAPHITE.blockKg() * Ore.GRAPHITE.grade;
			tails += Ore.GRAPHITE.blockKg() * (1 - Ore.GRAPHITE.grade * Tuning.GRAPHITE_FLAKE_RECOVERY) / 1000;
		}
		if (graphite <= 1e-6) {
			unit = "NO ORE: graphite schist in a hopper on top";
			return;
		}
		double kg = Math.min(max, graphite * Tuning.GRAPHITE_FLAKE_RECOVERY);
		kg = Math.min(kg, 2 * Tuning.BULK_BAG_KG - spherical);
		if (kg <= 1e-7) {
			unit = "FULL: set a bagging station against it";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = drive(level);
		if (strength <= 0) {
			unit = "starting: " + describe();
			return;
		}
		kg *= strength;
		graphite -= kg / Tuning.GRAPHITE_FLAKE_RECOVERY;
		spherical += kg;
		making = kg / seconds() * 3600;
		unit = String.format("milling: %.0f kg/h of spherical graphite (95 %% C)", making);
	}

	@Override
	public Item bagItem() {
		return WildcatterItems.SPHERICAL_GRAPHITE_BAG;
	}

	@Override
	public double bagReady() {
		return spherical;
	}

	@Override
	public double takeForBag(double kg) {
		double got = Math.min(kg, spherical);
		spherical -= got;
		setChanged();
		return got;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Graphite line (%.0f kW, %s): ", Tuning.GRAPHITE_LINE_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; graphite on the belt %.0f kg, %d blocks of schist; %,.0f kg ready for bags; tails %.1f t; ", graphite, fed(), spherical, tails) + describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("graphite", graphite);
		output.putDouble("spherical", spherical);
		output.putDouble("tails", tails);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		graphite = input.getDoubleOr("graphite", 0);
		spherical = input.getDoubleOr("spherical", 0);
		tails = input.getDoubleOr("tails", 0);
	}
}
