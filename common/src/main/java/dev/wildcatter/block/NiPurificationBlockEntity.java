package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The refinery's purification circuit (docs/multiblocks/nickel-smelter.md): copper cementation, cobalt by an amine solvent extraction, iron
 * oxidised and precipitated; 40 kW at LV (box north). Its SX needs a 5,000 L first charge of kerosene off the pipe on the east face of its
 * bottom cells (NO KEROSENE until then). It takes the leach reactor's liquor, sends 95 % of its nickel as pure liquor to the tankhouse,
 * casts its copper into copper ingots for a hopper under it, and counts its cobalt (as cobalt carbonate) and the nickel in its residues.
 */
public class NiPurificationBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private static final double COCO3_PER_CO = 118.94 / Nickel.CO;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double kerosene;     // L charged into its SX
	private double pureNi;       // kg of Ni in the pure liquor for the tankhouse
	private double copper;       // kg cemented, waiting to be cast
	private double cobaltCarbonate;   // kg, counted
	private double residueNi;    // kg, counted
	private double copperIngots;
	private double making;       // kg/h of Ni purified
	private String unit = "starting";

	public NiPurificationBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.NI_PURIFICATION_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[] {0} : new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	/** Its box on the north face of its main cell: its west side stands against the leach reactor. */
	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	private List<Lines.Face> keroseneInlet() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public double pureNi() {
		return pureNi;
	}

	public double making() {
		return making;
	}

	/** The tankhouse takes up to {@code kg} of Ni as pure liquor; returns what it gave. */
	public double takePureNi(double kg) {
		double got = Math.max(0, Math.min(kg, pureNi));
		pureNi -= got;
		setChanged();
		return got;
	}

	/** For the game test: its SX charged with kerosene. */
	public void testCharge() {
		kerosene = Tuning.PURIFICATION_KEROSENE_L;
		setChanged();
	}

	@Override
	protected void work(Level level) {
		making = 0;
		double dt = seconds();
		if (kerosene < Tuning.PURIFICATION_KEROSENE_L) {
			kerosene += Lines.pull(level, keroseneInlet(), Product.KEROSENE, Tuning.PURIFICATION_KEROSENE_L - kerosene, Pull.MACHINE, this);
			if (kerosene < Tuning.PURIFICATION_KEROSENE_L) {
				unit = String.format("NO KEROSENE: pipe kerosene to the east side of its bottom cells, its SX's first charge (%.0f of %.0f L)", kerosene,
						Tuning.PURIFICATION_KEROSENE_L);
				return;
			}
		}
		ChlorineLeachReactorBlockEntity leach = Touching.find(level, worldPosition, ChlorineLeachReactorBlockEntity.class, x -> true);
		if (leach == null) {
			unit = "NOT BUILT: set it against the chlorine leach reactor";
			return;
		}
		if (leach.liquorNi() <= 1e-6) {
			unit = "waiting for liquor from the leach reactor";
			return;
		}
		if (pureNi >= Tuning.LEACH_LIQUOR_HOLD_KG_NI) {
			unit = "FULL: the tankhouse is not taking its liquor";
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
		double ni = leach.takeLiquorNi(Tuning.LEACH_MATTE_KGH * Tuning.BESSEMER_MATTE_NI * 1.2 / 3600 * dt * strength);
		pureNi += ni * Tuning.NI_RECOVERY;
		residueNi += ni * (1 - Tuning.NI_RECOVERY);
		copper += ni * Nickel.copperPerNi();
		cobaltCarbonate += ni * Nickel.cobaltPerNi() * COCO3_PER_CO;
		while (copper >= Tuning.COPPER_INGOT_KG && (slots.get(0).isEmpty() || slots.get(0).getCount() < 64)) {
			if (slots.get(0).isEmpty()) {
				slots.set(0, new ItemStack(Items.COPPER_INGOT));
			} else {
				slots.get(0).grow(1);
			}
			copper -= Tuning.COPPER_INGOT_KG;
			copperIngots++;
		}
		making = ni / dt * 3600;
		unit = String.format("purifying: %.0f kg/h of Ni to the tankhouse; copper cemented %.0f kg/h, cobalt %.1f kg/h", making * Tuning.NI_RECOVERY,
				making * Nickel.copperPerNi(), making * Nickel.cobaltPerNi());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Purification (Cu cementation, Co by SX, Fe removal; %.0f kW, %s): ", Tuning.PURIFICATION_WATTS / 1000,
				Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; kerosene %.0f L; pure liquor %.0f kg of Ni; copper %.0f kg to cast, %.0f ingots cast; %.0f kg of cobalt carbonate; %.0f kg of Ni to residues; ",
						kerosene, pureNi, copper, copperIngots, cobaltCarbonate, residueNi)
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
		output.putDouble("kerosene", kerosene);
		output.putDouble("pureNi", pureNi);
		output.putDouble("copper", copper);
		output.putDouble("cobaltCarbonate", cobaltCarbonate);
		output.putDouble("residueNi", residueNi);
		output.putDouble("copperIngots", copperIngots);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		kerosene = input.getDoubleOr("kerosene", 0);
		pureNi = input.getDoubleOr("pureNi", 0);
		copper = input.getDoubleOr("copper", 0);
		cobaltCarbonate = input.getDoubleOr("cobaltCarbonate", 0);
		residueNi = input.getDoubleOr("residueNi", 0);
		copperIngots = input.getDoubleOr("copperIngots", 0);
	}
}
