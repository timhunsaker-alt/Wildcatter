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
public class ConversionStageBlockEntity extends PlantUnitBlockEntity implements UraniumStage {
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

	public static void tick(Level level, BlockPos pos, BlockState state, ConversionStageBlockEntity s) {
		if (s.plcHold(level)) {
			return;
		}
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		boolean f = s.fluorinator();
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
			if (waiting < 1e-6) {
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
		return Component.literal(status + String.format("; %.1f kg of U held; %,.0f kg %s", uranium, counted, fluorinator() ? "of ash in its bed" : "of water and HF to its scrubber"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("uranium", uranium);
		output.putDouble("counted", counted);
		output.putDouble("aduShare", aduShare);
		output.putDouble("taken", taken);
		output.putDouble("through", through);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		uranium = input.getDoubleOr("uranium", 0);
		counted = input.getDoubleOr("counted", 0);
		aduShare = input.getDoubleOr("aduShare", 0);
		taken = input.getDoubleOr("taken", 0);
		through = input.getDoubleOr("through", 0);
	}
}
