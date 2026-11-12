package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.WildcatterItems;
import dev.wildcatter.fluid.Ferroalloys;
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
 * The nickel smelter's electric furnace and its controller (docs/multiblocks/nickel-smelter.md): a rectangular six-in-line furnace with
 * Soderberg electrodes, 1 MW from a furnace transformer set against it at its regulated electrode current (the transformer's lowest tap
 * gives more: the furnace holds its own 1 MW). It smelts the calcine of the fluid-bed roaster it touches at 500 kWh a tonne into
 * furnace matte (25 % Ni) for the Peirce-Smith converter it touches; slag is counted (the converter's slag comes back to it, counted).
 * Electrode paste (green anodes) by a hopper on top. Its transformer gone, it holds its bath and freezes after 4 hours.
 */
public class NickelFurnaceBlockEntity extends PlantUnitBlockEntity implements HopperFed, TransformerFurnace {
	private final NonNullList<ItemStack> slots = NonNullList.withSize(1, ItemStack.EMPTY);
	private double matteNi;          // kg of Ni in the furnace matte in its hearth
	private double paste = Tuning.NI_FURNACE_ELECTRODE_PASTE_KG;
	private double unpowered;
	private boolean everRan;
	private boolean frozen;
	private boolean want;
	private double watts;
	private double making;           // kg/h of calcine smelted, the last interval
	private double smelted;          // t of calcine, all told
	private double slag;             // t
	private double converterSlag;    // t of converter slag returned

	public NickelFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.NICKEL_ELECTRIC_FURNACE_ENTITY, pos, state);
	}

	@Override
	public NonNullList<ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(ItemStack stack) {
		return stack.is(WildcatterItems.GREEN_ANODE);
	}

	public @Nullable FurnaceTransformerBlockEntity transformer() {
		return level == null ? null : Touching.find(level, worldPosition, FurnaceTransformerBlockEntity.class, t -> true);
	}

	public @Nullable FluidBedRoasterBlockEntity roaster() {
		return level == null ? null : Touching.find(level, worldPosition, FluidBedRoasterBlockEntity.class, t -> true);
	}

	public @Nullable PeirceSmithConverterBlockEntity converter() {
		return level == null ? null : Touching.find(level, worldPosition, PeirceSmithConverterBlockEntity.class, t -> true);
	}

	/** What it asks of its transformer: its own 1 MW (less if the tap gives less), half with its electrodes short. */
	@Override
	public double wantedWatts(double tapWatts) {
		if (!want || frozen) {
			return 0;
		}
		return Math.min(tapWatts, Tuning.NI_FURNACE_WATTS) * (paste <= 0 ? Tuning.SAF_SHORT_POWER : 1.0);
	}

	public double watts() {
		return watts;
	}

	public double making() {
		return making;
	}

	/** kg of furnace matte in its hearth. */
	public double matte() {
		return matteNi * Nickel.furnaceMattePerNi();
	}

	public double matteNi() {
		return matteNi;
	}

	/** The converter takes up to {@code kg} of Ni as furnace matte; returns what it gave. */
	public double takeMatteNi(double kg) {
		double got = Math.max(0, Math.min(kg, matteNi));
		matteNi -= got;
		setChanged();
		return got;
	}

	/** The converter's slag back to the furnace, counted. */
	public void returnSlag(double tonnes) {
		converterSlag += tonnes;
		setChanged();
	}

	public boolean frozen() {
		return frozen;
	}

	/** For the game test: {@code seconds} without power, as if time had passed. */
	public void testUnpowered(double seconds) {
		unpowered += seconds;
		setChanged();
	}

	/** For the game test: {@code kg} of Ni put in its hearth as furnace matte. */
	public void testAddMatteNi(double kg) {
		matteNi += kg;
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, NickelFurnaceBlockEntity f) {
		if (level.isClientSide() || (level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		f.work(level);
		f.setChanged();
	}

	private void work(Level level) {
		double dt = seconds();
		making = 0;
		FurnaceTransformerBlockEntity tx = transformer();
		watts = tx == null ? 0 : tx.load().watts();
		if (watts > Tuning.NI_FURNACE_WATTS * 0.02) {
			everRan = true;
			unpowered = 0;
		} else if (everRan && (tx == null || tx.load().undervoltage())) {
			unpowered += dt;   // no power to hold its bath: idle for want of calcine it is kept hot
		}
		if (everRan && unpowered >= Tuning.SAF_FREEZE_SECONDS) {
			frozen = true;
		}
		if (frozen) {
			want = false;
			status = "FROZEN: its bath froze without power; dig it out (break it and place it again)";
			return;
		}
		if (tx == null) {
			want = false;
			status = everRan ? String.format("NOT BUILT: NO TRANSFORMER: holding its bath; it freezes in %.1f h", Math.max(0, Tuning.SAF_FREEZE_SECONDS - unpowered) / 3600)
					: "NOT BUILT: NO TRANSFORMER: set a furnace transformer against it";
			return;
		}
		FluidBedRoasterBlockEntity roaster = roaster();
		if (roaster == null) {
			want = false;
			status = "NOT BUILT: NO ROASTER: set the fluid-bed roaster against it";
			return;
		}
		if (plcHold(level)) {
			want = false;
			return;
		}
		if (matte() >= Tuning.NI_FURNACE_MATTE_HOLD_KG) {
			want = false;
			status = converter() == null ? "FULL: set the Peirce-Smith converter against it to take its matte" : "FULL: the converter is not taking its matte";
			return;
		}
		double need = Nickel.furnaceKgh(Math.min(Ferroalloys.tapWatts(tx.tap()), Tuning.NI_FURNACE_WATTS)) / 3600 * dt;
		if (roaster.calcine() < need * 0.5) {
			want = false;
			status = "NO FEED: waiting for calcine from the roaster";
			return;
		}
		want = true;
		double kg = Math.max(0, watts) * dt / Nickel.furnaceJoulesPerKg();
		if (kg > 0) {
			double[] got = new double[1];
			double ni = roaster.takeCalcine(kg, got);
			matteNi += ni;
			smelted += got[0] / 1000;
			slag += got[0] / 1000 * Tuning.NI_FURNACE_SLAG_PER_T;
			paste = Math.max(0, paste - got[0] / 1000 * Tuning.NI_FURNACE_PASTE_KG_PER_T);
			making = got[0] / dt * 3600;
		}
		while (paste <= Tuning.NI_FURNACE_ELECTRODE_PASTE_KG - Tuning.GREEN_ANODE_KG && !slots.get(0).isEmpty()) {
			slots.get(0).shrink(1);
			paste += Tuning.GREEN_ANODE_KG;
		}
		String electrodes = paste <= 0 ? "; ELECTRODES SHORT: put green anodes (Soderberg paste) in a hopper on its top" : "";
		String conv = converter() == null ? "; no converter against it: its matte waits in the hearth" : "";
		status = watts < 1000 ? "starting: waiting for power from its transformer" + electrodes
				: String.format("smelting calcine: %.2f t/h at %.2f MW; furnace matte %.1f t in the hearth", making / 1000, watts / 1e6, matte() / 1000) + electrodes + conv;
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
			case LEVEL -> matte() / Tuning.NI_FURNACE_MATTE_HOLD_KG * 100;
			case WATTS -> watts;
			default -> Double.NaN;
		};
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
		return RatePoints.ma(making, Nickel.furnaceKgh(Tuning.NI_FURNACE_WATTS));
	}

	@Override
	public Component inspect() {
		FurnaceTransformerBlockEntity tx = transformer();
		String power = tx == null ? "" : String.format("; transformer tap %d, %s", tx.tap(), tx.metering());
		return Component.literal("Nickel electric furnace (six-in-line, 1 MW): " + status + power
				+ String.format("; paste %.0f kg in the electrodes, %d green anodes; %.1f t of calcine smelted, %.1f t of slag, %.1f t of converter slag back",
						paste, slots.get(0).getCount(), smelted, slag, converterSlag));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putDouble("matteNi", matteNi);
		output.putDouble("paste", paste);
		output.putDouble("unpowered", unpowered);
		output.putBoolean("everRan", everRan);
		output.putBoolean("frozen", frozen);
		output.putDouble("smelted", smelted);
		output.putDouble("slag", slag);
		output.putDouble("converterSlag", converterSlag);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		matteNi = input.getDoubleOr("matteNi", 0);
		paste = input.getDoubleOr("paste", Tuning.NI_FURNACE_ELECTRODE_PASTE_KG);
		unpowered = input.getDoubleOr("unpowered", 0);
		everRan = input.getBooleanOr("everRan", false);
		frozen = input.getBooleanOr("frozen", false);
		smelted = input.getDoubleOr("smelted", 0);
		slag = input.getDoubleOr("slag", 0);
		converterSlag = input.getDoubleOr("converterSlag", 0);
	}
}
