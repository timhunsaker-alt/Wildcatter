package dev.wildcatter.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.fluid.AmmoniaPlant;
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
 * The ammonium nitrate neutraliser (docs/multiblocks/ammonia-plant.md): ammonia and 60 % nitric acid, both on the east face of its
 * bottom cells, react to an 85 % AN solution at about 150 C, NH3 + HNO3 -> NH4NO3. Its own heat of reaction boils the acid's extra
 * water off as steam, so it needs no power or fuel. The solution goes out of the south face of its bottom cells.
 */
public class AnNeutraliserBlockEntity extends PlantUnitBlockEntity {
	private double making;      // kg/h of AN (as 100 %), the last interval
	private double steamKg;     // water boiled off, all told

	public AnNeutraliserBlockEntity(BlockPos pos, BlockState state) {
		super(WildcatterBlocks.AN_NEUTRALISER_ENTITY, pos, state);
	}

	private List<Lines.Face> inlets() {
		return faces(at -> at[1] == 0, Direction.EAST);
	}

	@Override
	protected @Nullable Product nozzle(int[] at, Direction side) {
		return at[2] == 2 && at[1] == 0 && side == Direction.SOUTH ? Product.AN_SOLUTION : null;
	}

	@Override
	protected double hold(Product product) {
		return product == Product.AN_SOLUTION ? AmmoniaPlant.anSolutionKgh() / 3600 * Tuning.PLANT_HOLD_SECONDS * 4 : 0;
	}

	/** kg/h of AN (as 100 %) over the last interval. */
	public double making() {
		return making;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, AnNeutraliserBlockEntity n) {
		if ((level.getGameTime() + pos.hashCode()) % Tuning.WELL_INTERVAL != 0) {
			return;
		}
		n.making = 0;
		if (n.plcHold(level)) {
			n.setChanged();
			return;
		}
		double acidHno3 = Lines.available(level, n.inlets(), Product.NITRIC_ACID, Pull.MACHINE, n) * Tuning.NITRIC_KG_PER_L * Tuning.NITRIC_STRENGTH;
		double ammoniaKg = Lines.available(level, n.inlets(), Product.AMMONIA, Pull.MACHINE, n) * Tuning.AMMONIA_KG_PER_L;
		double solutionPerHno3 = AmmoniaPlant.anPerHno3() / Tuning.AN_STRENGTH;
		double hno3 = Math.min(AmmoniaPlant.hno3Kgh() / 3600 * seconds(), n.room(Product.AN_SOLUTION) / solutionPerHno3);
		if (acidHno3 < 1e-9) {
			n.status = "NO ACID: pipe nitric acid to its east side";
		} else if (ammoniaKg < 1e-9) {
			n.status = "NO AMMONIA: pipe ammonia to its east side";
		} else if (hno3 <= 1e-9) {
			n.status = "idle: FULL, its solution has nowhere to go (pipe its south side to a blender or a stock tank)";
		} else {
			hno3 = Math.min(Math.min(hno3, acidHno3), ammoniaKg / AmmoniaPlant.ammoniaPerHno3());
			hno3 = Lines.pull(level, n.inlets(), Product.NITRIC_ACID, hno3 / Tuning.NITRIC_STRENGTH / Tuning.NITRIC_KG_PER_L, Pull.MACHINE, n)
					* Tuning.NITRIC_KG_PER_L * Tuning.NITRIC_STRENGTH;
			hno3 = Math.min(hno3, Lines.pull(level, n.inlets(), Product.AMMONIA, hno3 * AmmoniaPlant.ammoniaPerHno3() / Tuning.AMMONIA_KG_PER_L, Pull.MACHINE, n)
					* Tuning.AMMONIA_KG_PER_L / AmmoniaPlant.ammoniaPerHno3());
			n.add(Product.AN_SOLUTION, hno3 * solutionPerHno3);
			n.steamKg += hno3 * AmmoniaPlant.evaporatedPerHno3();
			n.making = hno3 * AmmoniaPlant.anPerHno3() / seconds() * 3600;
			n.status = String.format("neutralising at about 150 C: %.1f kg/h of ammonium nitrate as %.1f kg/h of %.0f %% solution, %.1f kg/h of water boiled off",
					n.making, n.making / Tuning.AN_STRENGTH, Tuning.AN_STRENGTH * 100, hno3 * AmmoniaPlant.evaporatedPerHno3() / seconds() * 3600);
		}
		n.setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("AN neutraliser: " + status + String.format("; solution held %.2f kg; %.1f kg of steam off its vent", held(Product.AN_SOLUTION), steamKg));
	}

	// ---- PLC points: RUN, RUNNING, a 4-20 mA rate ---------------------------------------------------------------------------------
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
		return RatePoints.ma(making, AmmoniaPlant.anKgh());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("steam", steamKg);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		steamKg = input.getDoubleOr("steam", 0);
	}
}
