package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Phosphate;
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
 * The DEPA-TOPO circuit (docs/multiblocks/uranium-from-phosphate.md), Oak Ridge's process on the filter acid: mixer-settlers in which
 * 0.5 M DEPA and 0.125 M TOPO in kerosene extract the acid's U(VI), and a reductive strip with iron(II)-bearing acid takes it back
 * out. The pretreatment against its west side runs it. Its first charge is 5,000 L of organic: kerosene by pipe on the north face of
 * its front cells and extractant drums by a hopper on top (which also takes the strip's iron ingots); it reads FILLING until charged,
 * then makes up what the acid carries off. Treated acid out of the south face of its back cells, for the evaporator. 60 kW on the
 * world's industrial voltage, the box on the north face of its main cell.
 *
 * <p>The second cycle (Tim, 2026-11-10, answer 22): the first cycle's strip is re-extracted and stripped with ammonium carbonate, made in
 * line from liquid ammonia (L) and liquid CO2 (kg, 16d1's liquefier) piped to the east face of its east end's front three cells (0.4 kg of
 * NH3 and 0.5 kg of CO2 a kg of U). The AUC goes by touching to 16a's uranium calciner against its east end, which calcines it as it
 * does ADU (U3O8 at 96 %) for the drum filler against the calciner. Without a calciner the strip holds its uranium, up to a drum's
 * worth; FULL then means the calciner is backed up or missing, and the acid goes round the circuit.
 */
public class DepaTopoCircuitBlockEntity extends UnitMotorBlockEntity implements HopperFed {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
	private double organic;      // L of organic in the settlers
	private double kerosene;     // L of the first charge's kerosene in
	private int drums;           // extractant drums of the first charge in
	private double extractant;   // kg of make-up extractant opened and not yet used
	private double iron;         // kg of iron opened for the strip
	private double uranium;      // kg of U held in the first cycle's strip
	private double stripped;     // kg of U sent on as AUC by the second cycle
	private String second = "second cycle: no calciner against its east end";
	private double making;       // kg/h of U taken, the last interval
	private long treated = -1000; // when it last took acid
	private String unit = "FILLING: needs its first charge";

	public DepaTopoCircuitBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.DEPA_TOPO_CIRCUIT_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	private static boolean takesIn(int slot, ItemStack stack) {
		return slot == 0 ? stack.is(WildcatterItems.EXTRACTANT_DRUM) : slot == 1 && stack.is(Items.IRON_INGOT);
	}

	@Override
	public boolean takes(ItemStack stack) {
		return takesIn(0, stack) || takesIn(1, stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 1} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return takesIn(slot, stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && takesIn(slot, stack);
	}

	/** The ammonium carbonate strip's ammonia and CO2: the east face of its east end's front cells (the calciner stands at the back ones). */
	private List<Lines.Face> stripIn() {
		return faces(at -> at[0] == 15 && at[1] == 0 && at[2] <= 2, Direction.EAST);
	}

	/**
	 * The second cycle, asked by the calciner against its east end for up to {@code u3o8} kg of U3O8 as AUC: re-extracts that much of
	 * the strip's uranium and strips it with ammonium carbonate from the ammonia and CO2 on its east face. Returns kg of U3O8.
	 */
	public double takeAuc(Level level, double u3o8) {
		double u = Math.min(uranium, u3o8 * Tuning.U_PER_U3O8);
		if (u <= 1e-9) {
			second = "second cycle: the strip is empty";
			return 0;
		}
		double nh3L = Lines.available(level, stripIn(), Product.AMMONIA, Pull.MACHINE, this);
		double co2 = Lines.available(level, stripIn(), Product.CO2, Pull.MACHINE, this);
		u = Math.min(u, Math.min(nh3L * Tuning.AMMONIA_KG_PER_L / Tuning.AUC_NH3_PER_U, co2 / Tuning.AUC_CO2_PER_U));
		if (u <= 1e-9) {
			second = nh3L * Tuning.AMMONIA_KG_PER_L < 1e-6 ? "second cycle: NO AMMONIA: pipe liquid ammonia to the east face of its east end"
					: "second cycle: NO CO2: pipe liquid CO2 from a CO2 liquefier or its storage tank to the east face of its east end";
			return 0;
		}
		Lines.pull(level, stripIn(), Product.AMMONIA, u * Tuning.AUC_NH3_PER_U / Tuning.AMMONIA_KG_PER_L, Pull.MACHINE, this);
		Lines.pull(level, stripIn(), Product.CO2, u * Tuning.AUC_CO2_PER_U, Pull.MACHINE, this);
		uranium -= u;
		stripped += u;
		second = String.format("second cycle: stripping with ammonium carbonate, AUC to the calciner (%.1f kg of U sent)", stripped);
		setChanged();
		return u / Tuning.U_PER_U3O8;
	}

	/** U3O8 the calciner took as AUC and could not calcine (no gas): back into the strip. */
	public void returnAuc(double u3o8) {
		uranium += u3o8 * Tuning.U_PER_U3O8;
		stripped -= u3o8 * Tuning.U_PER_U3O8;
		setChanged();
	}

	/** kg of U sent on as AUC. */
	public double stripped() {
		return stripped;
	}

	/** The second cycle's state, for its readout. */
	public String secondCycle() {
		return second;
	}

	/** Told by a calciner that it is not taking AUC (none against it is told by the absence of calls). */
	public void calcinerIdle(String why) {
		if (!second.contains("NO AMMONIA") && !second.contains("NO CO2")) {
			second = "second cycle: " + why;
		}
	}

	private List<Lines.Face> keroseneIn() {
		return faces(at -> at[2] == 0 && at[1] == 0, Direction.NORTH);
	}

	/** Its box is on the north face of its main cell: the part it stands against covers its west side. */
	@Override
	protected Direction boxSide() {
		return Direction.NORTH;
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 5 && at[1] == 0 && side == Direction.SOUTH ? Product.TREATED_ACID : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.TREATED_ACID ? Phosphate.filterAcidLps() * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	/** kg of U held in the first cycle's strip. */
	public double uranium() {
		return uranium;
	}

	/** kg/h of U taken over the last interval: about 2.8 at the rating. */
	public double making() {
		return level != null && level.getGameTime() - treated < 40 ? making : 0;
	}

	/** True once it holds its first charge. */
	public boolean charged() {
		return organic >= Tuning.DEPA_TOPO_CHARGE_L - 1e-6;
	}

	/** Game test hook: the strip's opened iron used up. */
	public void testDrainIron() {
		iron = 0;
		setChanged();
	}

	/** Game test hook: the strip as if it had run {@code kg} of U. */
	public void testAddUranium(double kg) {
		uranium += kg;
		setChanged();
	}

	/** The first charge: kerosene off its pipe and drums out of its bin, as fast as they come. */
	private void fill(Level level) {
		double want = Phosphate.chargeKeroseneL() - kerosene;
		if (want > 0) {
			kerosene += Lines.pull(level, keroseneIn(), Product.KEROSENE, Math.min(want, 200), Pull.MACHINE, this);
		}
		while (drums < Phosphate.chargeDrums() && !slots.get(0).isEmpty()) {
			slots.get(0).shrink(1);
			drums++;
		}
		organic = kerosene + drums * Tuning.EXTRACTANT_DRUM_KG;
		setChanged();
	}

	/**
	 * Called by the pretreatment each interval before it sends {@code litres} of acid: fills the first charge, opens iron and extractant
	 * as needed; returns null if it can take the acid, else why not.
	 */
	@Nullable String prepare(Level level, double litres) {
		if (!charged()) {
			fill(level);
			if (!charged()) {
				return unit = String.format("FILLING (first charge): %,.0f of %,.0f L of organic (kerosene %,.0f of %,.0f L on its north side, extractant drums %d of %d in its bin)",
						organic, Tuning.DEPA_TOPO_CHARGE_L, kerosene, Phosphate.chargeKeroseneL(), drums, Phosphate.chargeDrums());
			}
		}
		if (uranium >= Tuning.DEPA_TOPO_U_HOLD_KG) {
			return unit = String.format("FULL: its strip holds %.0f kg of U (%s)", uranium, second);
		}
		double u = litres * Phosphate.uraniumGPerL() / 1000 * Tuning.DEPA_TOPO_RECOVERY;
		if (iron < u * Tuning.IRON_PER_URANIUM && !slots.get(1).isEmpty()) {
			slots.get(1).shrink(1);
			iron += Tuning.IRON_INGOT_KG;
			setChanged();
		}
		if (iron < u * Tuning.IRON_PER_URANIUM) {
			return unit = "NO IRON: put iron ingots in a hopper on its top for the reductive strip";
		}
		double loss = litres / 1000 * Tuning.ORGANIC_LOSS_L_PER_M3;
		double extract = loss * Tuning.DEPA_TOPO_EXTRACTANT;
		if (extractant < extract && !slots.get(0).isEmpty()) {
			slots.get(0).shrink(1);
			extractant += Tuning.EXTRACTANT_DRUM_KG;
			setChanged();
		}
		if (extractant < extract) {
			return unit = "NO EXTRACTANT: put an extractant drum in a hopper on its top for the make-up";
		}
		if (room(Product.TREATED_ACID) < litres) {
			return unit = "idle: FULL, its treated acid has nowhere to go (pipe its south side to the evaporator)";
		}
		return null;
	}

	/** Takes the uranium out of {@code litres} of pretreated acid and sends the acid on. */
	void treat(Level level, double litres) {
		double u = litres * Phosphate.uraniumGPerL() / 1000 * Tuning.DEPA_TOPO_RECOVERY;
		double loss = litres / 1000 * Tuning.ORGANIC_LOSS_L_PER_M3;
		extractant -= loss * Tuning.DEPA_TOPO_EXTRACTANT;
		Lines.pull(level, keroseneIn(), Product.KEROSENE, loss * (1 - Tuning.DEPA_TOPO_EXTRACTANT), Pull.MACHINE, this);   // what does not come is lost from the settlers
		iron -= u * Tuning.IRON_PER_URANIUM;
		uranium += u;
		add(Product.TREATED_ACID, litres);
		making = u / seconds() * 3600;
		treated = level.getGameTime();
		unit = String.format("extracting: %.2f g of U a litre in, %.0f %% recovered, %.2f kg/h of U into the strip", Phosphate.uraniumGPerL(), Tuning.DEPA_TOPO_RECOVERY * 100, making);
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("DEPA-TOPO circuit (mixer-settlers %.0f kW, %s): ", Tuning.DEPA_TOPO_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; organic %,.0f L; extractant %.1f kg open and %d drums; iron %.1f kg and %d ingots; U in the strip %.1f of %.0f kg; %s; treated acid held %.0f L; ",
						organic, extractant, slots.get(0).getCount(), iron, slots.get(1).getCount(), uranium, Tuning.DEPA_TOPO_U_HOLD_KG, second, held(Product.TREATED_ACID))
				+ describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, making() > 0, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(making(), Phosphate.uraniumKgh());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("organic", organic);
		output.putDouble("kerosene", kerosene);
		output.putInt("drums", drums);
		output.putDouble("extractant", extractant);
		output.putDouble("iron", iron);
		output.putDouble("uranium", uranium);
		output.putDouble("stripped", stripped);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		organic = input.getDoubleOr("organic", 0);
		kerosene = input.getDoubleOr("kerosene", 0);
		drums = input.getIntOr("drums", 0);
		extractant = input.getDoubleOr("extractant", 0);
		iron = input.getDoubleOr("iron", 0);
		uranium = input.getDoubleOr("uranium", 0);
		stripped = input.getDoubleOr("stripped", 0);
	}
}
