package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.fluid.Conversion;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The hydrofluorinator and the fluorinator (docs/multiblocks/conversion.md): each takes the uranium of the stage touching it before
 * it and reacts it with the reagent piped to the east face of its bottom cells: anhydrous HF (UO2 to UF4) or fluorine (UF4 to UF6).
 */
public class ConversionStageBlockEntity extends PlantUnitBlockEntity implements UraniumStage, HopperFed {
	// 16b1, the fluorinator's bed (docs/multiblocks/conversion-extensions.md): empty drums (0) and fluorspar (1) in on top, ash drums out under it
	private static final int DRUMS = 0, SPAR = 1, OUT = 2, OUTS = 6;
	private final net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> slots = net.minecraft.core.NonNullList.withSize(OUT + OUTS, net.minecraft.world.item.ItemStack.EMPTY);
	private double retire = -1;   // seconds left of a bed retirement, 0 once dumped and waiting for drums or fluorspar; -1 running
	private double spar;          // kg of CaF2 tipped in for a new bed
	private double uranium;
	private double counted;   // kg: the scrubbed off-gas, or the fluorinator's ash
	private double aduShare;  // of the uranium held, the share from 96 % U3O8 drums (16a)

	public ConversionStageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private boolean fluorinator() {
		return getBlockState().getBlock() instanceof dev.wildcatter.multiblock.BigPart part && part.shapeName().equals("fluorinator");
	}

	private List<Lines.Face> reagentFaces() {
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

	@Override
	public double uranium() {
		return uranium;
	}

	@Override
	public double aduShare() {
		return aduShare;
	}

	@Override
	public double takeUranium(double kg) {
		double got = Math.min(kg, uranium);
		uranium -= got;
		setChanged();
		return got;
	}

	// ---- 16b1: the fluorinator's ash bed --------------------------------------------------------------------------------------
	@Override
	public net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> slots() {
		return slots;
	}

	@Override
	public boolean takes(net.minecraft.world.item.ItemStack stack) {
		return fluorinator() && (stack.is(dev.wildcatter.WildcatterItems.EMPTY_DRUM) || spar(stack) > 0);
	}

	/** kg of CaF2 in one item of fluorspar: a fluorspar block, or a bag of the KOH scrubber's synthetic fluorspar. */
	public static double spar(net.minecraft.world.item.ItemStack stack) {
		return stack.is(dev.wildcatter.ore.OreBlocks.MINERALS.get(dev.wildcatter.ore.Ore.FLUORITE)) ? dev.wildcatter.ore.Ore.FLUORITE.kgPerBlock()
				: stack.is(dev.wildcatter.WildcatterItems.SYNTHETIC_FLUORSPAR) ? Tuning.BULK_BAG_KG : 0;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {DRUMS, SPAR} : side == Direction.DOWN ? new int[] {2, 3, 4, 5, 6, 7} : new int[0];
	}

	@Override
	public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
		return fluorinator() && (slot == DRUMS ? stack.is(dev.wildcatter.WildcatterItems.EMPTY_DRUM) : slot == SPAR && spar(stack) > 0);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, Direction side) {
		return slot >= OUT && side == Direction.DOWN;
	}

	/** kg of ash in the fluorinator's bed. */
	public double ash() {
		return fluorinator() ? counted : 0;
	}

	public boolean retiring() {
		return retire >= 0;
	}

	public int ashDrums() {
		int n = 0;
		for (int i = OUT; i < OUT + OUTS; i++) {
			n += slots.get(i).getCount();
		}
		return n;
	}

	/** Starts retiring the fluorinator's bed (a sneak-click, or a PLC's RETIRE BED); false if it is no fluorinator or already retiring. */
	public boolean retireBed() {
		if (!fluorinator() || retire >= 0) {
			return false;
		}
		retire = Tuning.BED_RETIRE_SECONDS;
		setChanged();
		return true;
	}

	/** For the game test: ash put straight into the bed, and the ten minutes skipped. */
	public void addAshForTest(double kg) {
		counted += kg;
		setChanged();
	}

	public void skipRetireForTest() {
		if (retire > 0) {
			retire = 0;
		}
	}

	/** For the game test: uranium put straight into the stage. */
	public void addUraniumForTest(double kg) {
		uranium += kg;
		setChanged();
	}

	private boolean bedFull() {
		return dev.wildcatter.Rules.fluorinatorAshBed() && counted >= Tuning.ASH_BED_KG;
	}

	/** One interval of a retirement: the bed dumped into drums after ten minutes, then a new bed of fluorspar. True while it lasts. */
	private boolean retiring(double dt) {
		if (retire < 0) {
			return false;
		}
		if (retire > 0) {
			retire = Math.max(0, retire - dt);
			status = String.format("RETIRING THE BED: %.0f minutes to dump %,.0f kg of ash", Math.ceil(retire / 60), counted);
			return true;
		}
		while (counted > 1e-6) {
			int out = -1;
			for (int i = OUT; i < OUT + OUTS && out < 0; i++) {
				if (slots.get(i).isEmpty()) {
					out = i;
				}
			}
			if (slots.get(DRUMS).isEmpty() || out < 0) {
				status = String.format("RETIRING THE BED: %s; %,.0f kg of ash still to drum", out < 0 ? "FULL: take the ash drums (a hopper under it)" : "NO DRUMS: put empty drums in a hopper on top", counted);
				return true;
			}
			slots.get(DRUMS).shrink(1);
			slots.set(out, new net.minecraft.world.item.ItemStack(dev.wildcatter.WildcatterItems.FLUORINATOR_ASH_DRUM));
			counted = Math.max(0, counted - Tuning.ASH_DRUM_KG);
		}
		while (spar < Tuning.BED_FLUORSPAR_KG && spar(slots.get(SPAR)) > 0) {
			spar += spar(slots.get(SPAR));
			slots.get(SPAR).shrink(1);
		}
		if (spar < Tuning.BED_FLUORSPAR_KG) {
			status = String.format("RETIRING THE BED: NO FLUORSPAR for the new bed (%.0f of %.0f kg): fluorspar in a hopper on top", spar, Tuning.BED_FLUORSPAR_KG);
			return true;
		}
		spar -= Tuning.BED_FLUORSPAR_KG;
		retire = -1;
		counted = 0;
		return false;
	}

	@Override
	public java.util.List<dev.wildcatter.control.ControlDevice.Point> points() {
		if (!fluorinator()) {
			return super.points();
		}
		java.util.List<dev.wildcatter.control.ControlDevice.Point> list = new java.util.ArrayList<>(super.points());
		list.add(new dev.wildcatter.control.ControlDevice.Point("RETIRE BED", true));
		list.add(new dev.wildcatter.control.ControlDevice.Point("BED FULL", false));
		list.add(new dev.wildcatter.control.ControlDevice.Point("RETIRING", false));
		return list;
	}

	@Override
	public boolean readPoint(int point) {
		return point == 5 ? fluorinator() && counted >= Tuning.ASH_BED_KG : point == 6 ? retire >= 0 : super.readPoint(point);
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 4) {
			if (on) {
				retireBed();
			}
			return;
		}
		super.commandPoint(point, on, gameTime);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ConversionStageBlockEntity s) {
		if (s.plcHold(level)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		boolean f = s.fluorinator();
		if (f && s.retiring(seconds())) {
			s.status = "Fluorinator: " + s.status;
			s.setChanged();
			return;
		}
		if (f && s.bedFull()) {
			s.status = String.format("Fluorinator: BED FULL (%,.0f kg of ash): retire the bed (sneak-click with an empty hand, or a PLC's RETIRE BED)", s.counted);
			s.setChanged();
			return;
		}
		Class<? extends UraniumStage> before = f ? ConversionStageBlockEntity.class : ReductionReactorBlockEntity.class;
		UraniumStage up = Touching.find(level, pos, before, u -> !f || u instanceof ConversionStageBlockEntity c && !c.fluorinator());
		Product reagent = f ? Product.FLUORINE : Product.ANHYDROUS_HF;
		double share = up == null ? 0 : up.aduShare();
		// kg of F2, or litres of HF, a kg of U: U3O8 drums bring no magnesia to take HF as MgF2 (16a)
		double perU = f ? Tuning.CONV_F2_PER_U : hfPerU(share) / Tuning.AHF_KG_PER_L;
		String name = f ? "Fluorinator" : "Hydrofluorinator";
		if (up == null) {
			s.status = "NOT BUILT: set it against the " + (f ? "HYDROFLUORINATOR" : "REDUCTION REACTOR");
		} else if (up.uranium() < 1e-6) {
			s.status = "idle: no " + (f ? "UF4" : "UO2") + " in the stage before it";
		} else {
			double waiting = Lines.available(level, s.reagentFaces(), reagent, Pull.MACHINE, s);
			double room = Conversion.uraniumKgs() * Tuning.PLANT_HOLD_SECONDS - s.uranium;
			double kg = Math.min(Math.min(Conversion.uraniumKgs() * 1.2 * seconds(), up.uranium()), Math.min(room, waiting / perU));
			KohScrubberBlockEntity koh = f ? null : Touching.find(level, pos, KohScrubberBlockEntity.class, k -> true);
			if (koh != null && !koh.ready(kg * Tuning.HF_EXCESS_PER_U)) {
				s.status = "KOH SCRUBBER SPENT: its off-gas has nowhere to go (lime to the KOH scrubber, or caustic potash for its charge)";
			} else if (waiting < 1e-6) {
				s.status = f ? "NO FLUORINE: pipe fluorine from the cell house to its east side" : "NO HF: pipe anhydrous HF to its east side";
			} else if (kg <= 1e-7) {
				s.status = "idle: the stage after it is full";
			} else {
				Lines.pull(level, s.reagentFaces(), reagent, kg * perU, Pull.MACHINE, s);
				double got = up.takeUranium(kg);
				s.aduShare = UraniumStage.blend(s.uranium, s.aduShare, got, share);
				s.uranium += got;
				s.taken += kg * perU;
				s.through += kg;
				// the fluorinator keeps the feed's impurities as ash; the hydrofluorinator scrubs its water and excess HF
				s.counted += f ? kg * ashPerU(share) : kg * (hfPerU(share) - 0.336 + 0.151);
				if (koh != null) {
					koh.scrub(kg * Tuning.HF_EXCESS_PER_U);
				}
				s.status = String.format("%s %.3f kg/s of U with %.3f %s/s", f ? "fluorinating" : "hydrofluorinating", kg / seconds(), kg * perU / seconds(), reagent.unit());
			}
		}
		s.status = name + ": " + s.status;
		s.setChanged();
	}

	/** kg of impurities a kg of U in the yellowcake, left in the fluorinator's bed as fluorides. */
	static double ashPerU() {
		double u = Tuning.DRUM_NET_KG * Tuning.YELLOWCAKE_U3O8 * dev.wildcatter.ore.Ore.U_IN_U3O8;
		return Tuning.DRUM_NET_KG * (1 - Tuning.YELLOWCAKE_U3O8) / u;
	}

	/** The same for 96 % U3O8 drums: the 4 % that is not U3O8 (16a, proposed). */
	static double aduAshPerU() {
		double u = Tuning.DRUM_NET_KG * Tuning.ADU_U3O8 * dev.wildcatter.ore.Ore.U_IN_U3O8;
		return Tuning.DRUM_NET_KG * (1 - Tuning.ADU_U3O8) / u;
	}

	/** Ash a kg of U for a feed with this share from U3O8 drums. */
	public static double ashPerU(double adu) {
		return ashPerU() * (1 - adu) + aduAshPerU() * adu;
	}

	/** kg of HF a kg of U the hydrofluorinator takes for a feed with this share from U3O8 drums. */
	public static double hfPerU(double adu) {
		return Tuning.CONV_HF_PER_U - Tuning.CONV_HF_MGF2_PER_U * adu;
	}

	private double taken;     // the reagent taken, in its unit, and the kg of U through, since placed (the game test reads them)
	private double through;

	public double reagentTaken() {
		return taken;
	}

	public double uraniumThrough() {
		return through;
	}

	@Override
	public Component inspect() {
		String bed = fluorinator() ? String.format(" (%.0f %% of a full bed); %d ash drums out, %d empty drums waiting", counted / Tuning.ASH_BED_KG * 100, ashDrums(), slots.get(DRUMS).getCount()) : "";
		return Component.literal(status + String.format("; %.1f kg of U held; %,.0f kg %s", uranium, counted, fluorinator() ? "of ash in its bed" : "of water and HF to its scrubber") + bed);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("uranium", uranium);
		output.putDouble("counted", counted);
		output.putDouble("aduShare", aduShare);
		output.putDouble("taken", taken);
		output.putDouble("through", through);
		net.minecraft.world.ContainerHelper.saveAllItems(output, slots);
		output.putDouble("retire", retire);
		output.putDouble("spar", spar);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		uranium = input.getDoubleOr("uranium", 0);
		counted = input.getDoubleOr("counted", 0);
		aduShare = input.getDoubleOr("aduShare", 0);
		taken = input.getDoubleOr("taken", 0);
		through = input.getDoubleOr("through", 0);
		slots.clear();
		net.minecraft.world.ContainerHelper.loadAllItems(input, slots);
		retire = input.getDoubleOr("retire", -1);
		spar = input.getDoubleOr("spar", 0);
	}
}
