package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.MeltShop;
import dev.wildcatter.fluid.MeltShop.Grade;
import dev.wildcatter.fluid.Product;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The argon-oxygen decarburisation vessel (docs/multiblocks/nickel-smelter.md, 17b2), blown here with oxygen and nitrogen (argon for the
 * low-nitrogen finish is left out): 30 kW at LV (box west). It takes the induction furnace's heat and blows oxygen from the PSA oxygen
 * generators set against it (isr-wellfield.md) with a third as much nitrogen off the pipe on the east face of its bottom cells (the
 * ammonia plant's nitrogen generator), through its side tuyeres, until the HC ferrochrome's carbon is under 0.08 %. The finished heat
 * waits for the ingot caster it touches.
 */
public class AodConverterBlockEntity extends UnitMotorBlockEntity {
	private @Nullable Grade heat;
	private double kg;
	private double carbon;       // kg of carbon in the bath
	private boolean finished;
	private double o2;           // Nm3 blown into this heat
	private double n2;
	private double o2Rate;       // Nm3/h, the last interval
	private double n2Rate;
	private int heats;
	private String unit = "starting";

	public AodConverterBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.AOD_CONVERTER_ENTITY, pos, state);
	}

	private List<Lines.Face> nitrogen() {
		return faces(at -> at[1] == 0 && at[0] == 2, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return null;
	}

	@Override
	protected double hold(Product product) {
		return 0;
	}

	public boolean empty() {
		return heat == null;
	}

	public boolean finished() {
		return finished;
	}

	public double carbonShare() {
		return kg > 0 ? carbon / kg : 0;
	}

	public double o2Rate() {
		return o2Rate;
	}

	public double n2Rate() {
		return n2Rate;
	}

	/** The induction furnace taps a heat into it; false while it still holds one. */
	public boolean receive(Grade grade, double heatKg, double carbonKg) {
		if (heat != null) {
			return false;
		}
		heat = grade;
		kg = heatKg;
		carbon = carbonKg;
		finished = false;
		o2 = 0;
		n2 = 0;
		setChanged();
		return true;
	}

	/** The caster takes the finished heat: its grade, or null if there is none. */
	public @Nullable Grade pour() {
		if (heat == null || !finished) {
			return null;
		}
		Grade g = heat;
		heat = null;
		finished = false;
		kg = 0;
		carbon = 0;
		heats++;
		setChanged();
		return g;
	}

	/** For the game test: the blow's carbon taken down to its last interval. */
	public void testNearlyDone() {
		if (heat != null && !finished) {
			carbon = kg * Tuning.AOD_FINAL_CARBON + 0.01;
		}
	}

	@Override
	protected void work(Level level) {
		o2Rate = 0;
		n2Rate = 0;
		double dt = seconds();
		if (heat == null) {
			unit = Touching.find(level, worldPosition, InductionFurnaceBlockEntity.class, f -> true) == null
					? "NOT BUILT: set it against the induction furnace" : "waiting for a heat from the induction furnace";
			return;
		}
		if (finished) {
			unit = Touching.find(level, worldPosition, IngotCasterBlockEntity.class, c -> true) == null
					? String.format("FINISHED: %s at %.3f %% C; set the ingot caster against it", heat.label, carbonShare() * 100)
					: String.format("FINISHED: %s at %.3f %% C, pouring", heat.label, carbonShare() * 100);
			return;
		}
		double target = kg * Tuning.AOD_FINAL_CARBON;
		if (carbon <= target + 1e-4) {   // within a tenth of a gram: done
			finished = true;
			return;
		}
		List<OxygenGeneratorBlockEntity> generators = Touching.parts(level, worldPosition).stream().filter(e -> e instanceof OxygenGeneratorBlockEntity)
				.map(e -> (OxygenGeneratorBlockEntity) e).toList();
		if (generators.isEmpty()) {
			unit = "NO OXYGEN: set a PSA oxygen generator against it";
			return;
		}
		double wantO2 = Math.min(Tuning.AOD_MAX_O2_NM3H / 3600 * dt, (carbon - target) / MeltShop.carbonPerO2());
		double nAvailable = Lines.available(level, nitrogen(), Product.NITROGEN, Pull.MACHINE, this);
		if (nAvailable < wantO2 * Tuning.AOD_N2_PER_O2 * 0.05) {
			unit = "NO NITROGEN: pipe nitrogen to the east side of its bottom cells (a nitrogen generator)";
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
		wantO2 = Math.min(wantO2 * strength, nAvailable / Tuning.AOD_N2_PER_O2);
		double gotO2 = 0;
		for (OxygenGeneratorBlockEntity g : generators) {
			if (gotO2 >= wantO2) {
				break;
			}
			double kgWanted = (wantO2 - gotO2) * Tuning.O2_KG_PER_NM3;
			gotO2 += g.supply(level, kgWanted) * kgWanted / Tuning.O2_KG_PER_NM3;
		}
		if (gotO2 <= 1e-9) {
			unit = "NO OXYGEN: its oxygen generator is not making (power?)";
			return;
		}
		double gotN2 = Lines.pull(level, nitrogen(), Product.NITROGEN, gotO2 * Tuning.AOD_N2_PER_O2, Pull.MACHINE, this);
		carbon = Math.max(target, carbon - gotO2 * MeltShop.carbonPerO2());
		o2 += gotO2;
		n2 += gotN2;
		o2Rate = gotO2 / dt * 3600;
		n2Rate = gotN2 / dt * 3600;
		unit = String.format("BLOWING %s: %.0f Nm3/h of oxygen and %.0f of nitrogen through the tuyeres; carbon %.2f %%, to %.2f %%", heat.label, o2Rate, n2Rate,
				carbonShare() * 100, Tuning.AOD_FINAL_CARBON * 100);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("AOD converter (%.0f kW, %s): ", Tuning.AOD_WATTS / 1000, Voltage.INDUSTRIAL.label()) + unit
				+ String.format("; this heat %.0f Nm3 of oxygen and %.0f of nitrogen blown; %d heats; ", o2, n2, heats) + describe());
	}

	@Override
	public List<Point> points() {
		return RatePoints.POINTS;
	}

	@Override
	public boolean readPoint(int point) {
		return RatePoints.read(point, o2Rate > 0, unitRemote.auto());
	}

	@Override
	public double readMa(int point) {
		return RatePoints.ma(o2Rate, Tuning.AOD_MAX_O2_NM3H);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("heat", heat == null ? "" : heat.name());
		output.putDouble("kg", kg);
		output.putDouble("carbon", carbon);
		output.putBoolean("finished", finished);
		output.putDouble("o2", o2);
		output.putDouble("n2", n2);
		output.putInt("heats", heats);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String h = input.getStringOr("heat", "");
		heat = h.isEmpty() ? null : InductionFurnaceBlockEntity.parse(h, null);
		kg = input.getDoubleOr("kg", 0);
		carbon = input.getDoubleOr("carbon", 0);
		finished = input.getBooleanOr("finished", false);
		o2 = input.getDoubleOr("o2", 0);
		n2 = input.getDoubleOr("n2", 0);
		heats = input.getIntOr("heats", 0);
	}
}
