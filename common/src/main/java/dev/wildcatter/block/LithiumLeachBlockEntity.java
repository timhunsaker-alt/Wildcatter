package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Lithium;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import java.util.List;
import java.util.Map;
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
 * The lithium leach (docs/multiblocks/lithium.md), the lithium plant's controller: water leach of the roasted ore, lime neutralisation
 * (quicklime bags by a hopper on top, 20 kg a tonne of ore), a belt filter, and calcium removal with carbonate made in line from caustic
 * and CO2 (liquid CO2 on the south face of its back cell, x 3). The aluminosilicate residue goes out of the south face of its back cells
 * (x 1) as tails slurry, for a lined tailings cell. It runs the acid roast kiln against its west side, the decrepitation kiln against that,
 * and the LiOH crystallizer against its east side: 0.5 t of LiOH.H2O an hour. 37 kW at the world's industrial voltage, the box on the north
 * face of its main cell.
 */
public class LithiumLeachBlockEntity extends UnitMotorBlockEntity implements HopperFed, Assayed {
	/** The assay key of its residue: a tails mineral, not an item. */
	public static final String RESIDUE = "wildcatter:spodumene_residue";
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double lime;     // kg of quicklime opened
	private double making;   // kg/h of LiOH.H2O, the last interval
	private String unit = "starting";

	public LithiumLeachBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.LITHIUM_LEACH_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.QUICKLIME_BAG);
	}

	private List<Lines.Face> co2In() {
		return faces(at -> at[0] == 3 && at[2] == 5 && at[1] == 0, Direction.SOUTH);
	}

	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[0] == 1 && at[2] == 5 && at[1] == 0 && side == Direction.SOUTH ? Product.TAILS_SLURRY : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.TAILS_SLURRY ? Lithium.oreKgs() * Lithium.residuePerOre() / Tuning.CELL_SLURRY_SOLIDS * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	@Override
	public SlurryAssay assayOf(Product product) {
		return product == Product.TAILS_SLURRY ? SlurryAssay.of(Map.of(RESIDUE, Tuning.CELL_SLURRY_SOLIDS)) : SlurryAssay.EMPTY;
	}

	/** kg/h of LiOH.H2O over the last interval: 500 at the rating. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		AcidRoastKilnBlockEntity roast = Touching.find(level, worldPosition, AcidRoastKilnBlockEntity.class, r -> true);
		DecrepitationKilnBlockEntity kiln = roast == null ? null : Touching.find(level, roast.getBlockPos(), DecrepitationKilnBlockEntity.class, k -> true);
		LiohCrystallizerBlockEntity crystallizer = Touching.find(level, worldPosition, LiohCrystallizerBlockEntity.class, c -> true);
		if (roast == null || kiln == null || crystallizer == null) {
			unit = "NOT BUILT: set " + (roast == null ? "the ACID ROAST KILN against its west side" : kiln == null ? "the DECREPITATION KILN against the roast kiln"
					: "the LiOH CRYSTALLIZER against its east side");
			return;
		}
		double s = seconds();
		double ore = Lithium.oreKgs() * s;
		kiln.open(ore * 2);
		if (lime < ore * Tuning.LEACH_LIME_PER_T_ORE / 1000 && fed() > 0) {
			useOne();
			lime += Tuning.BULK_BAG_KG;
		}
		double li2oPerOre = Tuning.SPODUMENE_ORE_LI2O * Tuning.LITHIUM_RECOVERY;
		double acid = Lines.available(level, roast.acidIn(), Product.SULFURIC_ACID, Pull.MACHINE, this);
		double co2 = Lines.available(level, co2In(), Product.CO2, Pull.MACHINE, this);
		double caustic = Lines.available(level, crystallizer.causticIn(), Product.CAUSTIC_SODA, Pull.MACHINE, this);
		double steam = Lines.available(level, crystallizer.steamIn(), Product.PROCESS_STEAM, Pull.MACHINE, this);
		double causticPerOre = li2oPerOre * Lithium.naohPerLi2o() / Tuning.CAUSTIC_STRENGTH;
		double steamPerOre = li2oPerOre * Lithium.liohPerLi2o() * Tuning.LIOH_STEAM_PER_T;
		double tailsPerOre = Lithium.residuePerOre() / Tuning.CELL_SLURRY_SOLIDS;
		String lack = kiln.ore <= 1e-6 ? "NO ORE: spodumene in a hopper on the decrepitation kiln"
				: acid <= 1e-6 ? "NO ACID: pipe sulfuric acid to the east face of the roast kiln's back cell"
				: lime <= 1e-6 ? "NO LIME: quicklime bags in a hopper on the leach"
				: co2 <= 1e-6 ? "NO CO2: pipe liquid CO2 to the south face of the leach's back (east)"
				: caustic <= 1e-6 ? "NO CAUSTIC: pipe 32 % caustic soda to the east face of the crystallizer's back cell"
				: steam <= 1e-6 ? "NO STEAM: pipe process steam to the south face of the crystallizer"
				: room(Product.TAILS_SLURRY) < Math.min(ore, kiln.ore) * tailsPerOre / 4 ? "NO TAILS CELL: pipe the leach's residue (south face, west) to a lined tailings cell"
				: crystallizer.room() <= 1e-6 ? "FULL: set bagging stations against the crystallizer" : "";
		if (!lack.isEmpty()) {
			unit = lack;
			crystallizer.unit = kiln.unit = roast.unit = "held: " + lack;
			return;
		}
		double kg = Math.min(ore, kiln.ore);
		kg = Math.min(kg, acid * Tuning.ACID_KG_PER_L / Lithium.acidPerOre());
		kg = Math.min(kg, lime / (Tuning.LEACH_LIME_PER_T_ORE / 1000));
		kg = Math.min(kg, co2 / (Tuning.LEACH_CO2_PER_T_ORE / 1000));
		kg = Math.min(kg, Math.min(caustic / causticPerOre, steam / steamPerOre));
		kg = Math.min(kg, room(Product.TAILS_SLURRY) / tailsPerOre);
		kg = Math.min(kg, crystallizer.room() / (li2oPerOre * Lithium.liohPerLi2o()));
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = Math.min(Math.min(drive(level), roast.drive(level)), Math.min(kiln.drive(level), crystallizer.drive(level)));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; roast " + roast.motor().describe() + "; kiln " + kiln.motor().describe() + "; crystallizer " + crystallizer.motor().describe();
			return;
		}
		kg *= strength;
		double need = kg * Tuning.DECREPITATION_J_PER_KG;
		double fired = GasFiring.fire(level, kiln.gasIn(), need, kiln);
		double roastNeed = Tuning.ROAST_GAS_WATTS * s * kg / ore;
		double roastFired = GasFiring.fire(level, roast.gasIn(), roastNeed, roast);
		kg *= Math.min(1, Math.min(fired / need, roastFired / Math.max(1e-9, roastNeed)));
		if (kg <= 1e-7) {
			unit = "NO GAS: pipe gas or LPG to the decrepitation kiln (east face of its back) and the roast kiln (south face of its back)";
			return;
		}
		kiln.ore -= kg;
		Lines.pull(level, roast.acidIn(), Product.SULFURIC_ACID, kg * Lithium.acidPerOre() / Tuning.ACID_KG_PER_L, Pull.MACHINE, this);
		lime = Math.max(0, lime - kg * Tuning.LEACH_LIME_PER_T_ORE / 1000);
		Lines.pull(level, co2In(), Product.CO2, kg * Tuning.LEACH_CO2_PER_T_ORE / 1000, Pull.MACHINE, this);
		Lines.pull(level, crystallizer.causticIn(), Product.CAUSTIC_SODA, kg * causticPerOre, Pull.MACHINE, this);
		Lines.pull(level, crystallizer.steamIn(), Product.PROCESS_STEAM, kg * steamPerOre, Pull.MACHINE, this);
		add(Product.TAILS_SLURRY, kg * tailsPerOre);
		double li2o = kg * li2oPerOre;
		crystallizer.lioh += li2o * Lithium.liohPerLi2o();
		crystallizer.na2so4 += li2o * Lithium.na2so4PerLi2o();
		making = li2o * Lithium.liohPerLi2o() / s * 3600;
		kiln.unit = String.format("decrepitating %,.0f kg/h of spodumene at 1,050 C on %.2f MW of gas", kg / s * 3600, fired / s / 1e6);
		roast.unit = String.format("roasting with %.0f kg/h of sulfuric acid at 250 C", kg * Lithium.acidPerOre() / s * 3600);
		crystallizer.unit = String.format("causticising on %,.0f kg/h of 32 %% caustic: %.0f kg/h of LiOH.H2O and %.0f kg/h of sodium sulfate", kg * causticPerOre / s * 3600,
				making, li2o * Lithium.na2so4PerLi2o() / s * 3600);
		unit = String.format("leaching %,.0f kg/h of roasted ore: %.0f kg/h of LiOH.H2O; %,.0f kg/h of residue to the tailings", kg / s * 3600, making,
				kg * Lithium.residuePerOre() / s * 3600);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Lithium leach (%.0f kW, %s): ", Tuning.LITHIUM_LEACH_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; lime %.0f kg and %d bags; residue held %.0f kg; ", lime, fed(), held(Product.TAILS_SLURRY)) + describe());
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
		return RatePoints.ma(making, Lithium.liohKgs() * 3600);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("lime", lime);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		lime = input.getDoubleOr("lime", 0);
	}
}
