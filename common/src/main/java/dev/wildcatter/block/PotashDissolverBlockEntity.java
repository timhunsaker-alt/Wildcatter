package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Phosphate;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.ore.Ore;
import dev.wildcatter.ore.OreBlocks;
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
 * The potash dissolver (docs/multiblocks/fertilizer.md), the potash plant's controller: crushed sylvinite dissolved in hot recycle
 * brine near 100 C on process steam (the south face of its bottom cells); the halite stays undissolved and goes out of the west face of its main cell as
 * tails slurry with a halite assay, for a lined tailings cell. Potash ore by a hopper on its top, make-up water off a pump. It runs
 * the vacuum crystallizer against it, and the crystallizer the potash dryer against that: 5 t/h of MOP. 55 kW on the world's
 * industrial voltage, the box on the west face of its main cell.
 */
public class PotashDissolverBlockEntity extends UnitMotorBlockEntity implements HopperFed, WaterUser, Assayed {
	/** The assay key of its salt tails: a tails mineral, not an item (halite with the clays, kept apart from Tailings' halite). */
	public static final String SALT = "wildcatter:salt_tails";
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double ore;      // kg of sylvinite in the dissolver
	private double water;
	private double making;   // kg/h of MOP, the last interval
	private String unit = "starting";

	public PotashDissolverBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.POTASH_DISSOLVER_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(OreBlocks.MINERALS.get(Ore.SYLVITE));
	}

	private List<Lines.Face> steamIn() {
		return faces(at -> at[2] == 3 && at[1] == 0, Direction.SOUTH);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[0] == 0 && at[1] == 0 && side == Direction.WEST && at[2] > 0 ? Product.TAILS_SLURRY : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.TAILS_SLURRY ? Phosphate.mopKgs() * Phosphate.saltPerMop() / Tuning.CELL_SLURRY_SOLIDS * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	@Override
	public SlurryAssay assayOf(Product product) {
		return product == Product.TAILS_SLURRY ? SlurryAssay.of(Map.of(SALT, Tuning.CELL_SLURRY_SOLIDS)) : SlurryAssay.EMPTY;
	}

	@Override
	public double waterLevel() {
		return water / Tuning.PHOSPHATE_WATER_HOLD;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, Tuning.PHOSPHATE_WATER_HOLD - water));
		water += fits;
		setChanged();
		return fits;
	}

	/** kg/h of MOP over the last interval: 5,000 at the rating. */
	public double making() {
		return making;
	}

	@Override
	protected void work(Level level) {
		making = 0;
		VacuumCrystallizerBlockEntity crystallizer = Touching.find(level, worldPosition, VacuumCrystallizerBlockEntity.class, c -> true);
		if (crystallizer == null) {
			unit = "NO CRYSTALLIZER: set the vacuum crystallizer against it";
			return;
		}
		PotashDryerBlockEntity dryer = Touching.find(level, crystallizer.getBlockPos(), PotashDryerBlockEntity.class, d -> true);
		if (dryer == null) {
			unit = "NO DRYER: set the potash dryer against the crystallizer";
			crystallizer.unit = "NO DRYER: set the potash dryer against it";
			return;
		}
		double max = Phosphate.mopKgs() * seconds();
		if (ore < max * Phosphate.orePerMop() && fed() > 0) {
			useOne();
			ore += Ore.SYLVITE.blockKg();
		}
		if (ore < 1e-6) {
			unit = "NO POTASH: put a hopper of potash ore on its top";
			return;
		}
		if (water < 1e-6) {
			unit = "NO WATER: pump make-up water to it";
			return;
		}
		double steam = Lines.available(level, steamIn(), Product.PROCESS_STEAM, Pull.MACHINE, this);
		if (steam < 1e-6) {
			unit = "NO STEAM: pipe process steam from a package boiler to its south side";
			return;
		}
		double ejector = Lines.available(level, crystallizer.steamIn(), Product.PROCESS_STEAM, Pull.MACHINE, this);
		if (ejector < 1e-6) {
			unit = "HELD: the crystallizer has no steam for its ejectors";
			crystallizer.unit = "NO STEAM: pipe process steam to its south side for the ejectors";
			return;
		}
		double kg = Math.min(max, ore / Phosphate.orePerMop());
		kg = Math.min(kg, water / Phosphate.potashWaterPerMop());
		kg = Math.min(kg, steam / Tuning.POTASH_DISSOLVER_STEAM);
		kg = Math.min(kg, ejector / Tuning.POTASH_EJECTOR_STEAM);
		kg = Math.min(kg, dryer.room());
		if (room(Product.TAILS_SLURRY) < Math.min(kg, max / 4) * Phosphate.saltPerMop() / Tuning.CELL_SLURRY_SOLIDS) {
			unit = "NO SALT STACK: pipe its west side to a lined tailings cell's decant tower (full, breached or missing)";
			return;
		}
		kg = Math.min(kg, room(Product.TAILS_SLURRY) * Tuning.CELL_SLURRY_SOLIDS / Phosphate.saltPerMop());
		if (kg <= 1e-7) {
			unit = "FULL: set a bagging station against the dryer";
			return;
		}
		if (plcHold(level)) {
			unit = status;
			return;
		}
		double strength = Math.min(drive(level), Math.min(crystallizer.drive(level), dryer.drive(level)));
		if (strength <= 0) {
			unit = "starting: " + describe() + "; crystallizer " + crystallizer.motor().describe() + "; dryer " + dryer.motor().describe();
			return;
		}
		kg *= strength;
		double need = kg * Phosphate.dryerJPerKg();
		double got = GasFiring.fire(level, dryer.gasIn(), need, this);
		kg *= Math.min(1, got / need);
		if (kg <= 1e-7) {
			unit = "HELD: the dryer has no fuel";
			dryer.unit = "NO FUEL: pipe gas or LPG to its south side";
			return;
		}
		ore = Math.max(0, ore - kg * Phosphate.orePerMop());
		water = Math.max(0, water - kg * Phosphate.potashWaterPerMop());
		Lines.pull(level, steamIn(), Product.PROCESS_STEAM, kg * Tuning.POTASH_DISSOLVER_STEAM, Pull.MACHINE, this);
		Lines.pull(level, crystallizer.steamIn(), Product.PROCESS_STEAM, kg * Tuning.POTASH_EJECTOR_STEAM, Pull.MACHINE, this);
		add(Product.TAILS_SLURRY, kg * Phosphate.saltPerMop() / Tuning.CELL_SLURRY_SOLIDS);
		dryer.addMop(kg);
		making = kg / seconds() * 3600;
		unit = String.format("dissolving %.2f kg/s of potash ore near 100 C: %,.0f kg/h of MOP, %.2f kg/s of salt to the stack", kg * Phosphate.orePerMop() / seconds(), making,
				kg * Phosphate.saltPerMop() / seconds());
		crystallizer.unit = String.format("crystallizing KCl under vacuum: %.2f kg/s, the ejectors on %.2f kg/s of steam", kg * Tuning.MOP_KCL / seconds(),
				kg * Tuning.POTASH_EJECTOR_STEAM / seconds());
		dryer.unit = String.format("drying and compacting %,.0f kg/h of MOP (0-0-60) on %.0f kW of gas", making, got / seconds() / 1000);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Potash dissolver (%.0f kW, %s): ", Tuning.POTASH_DISSOLVER_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; ore %,.0f kg and %d blocks; water %.0f/%.0f L; salt tails held %.0f kg; ", ore, fed(), water, Tuning.PHOSPHATE_WATER_HOLD, held(Product.TAILS_SLURRY))
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
		return RatePoints.ma(making, Phosphate.mopKgs() * 3600);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("ore", ore);
		output.putDouble("water", water);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		ore = input.getDoubleOr("ore", 0);
		water = input.getDoubleOr("water", 0);
	}
}
